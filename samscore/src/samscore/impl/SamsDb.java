package samscore.impl;

import samscore.ISamsDb;
import samscore.ISamsDb.IMetadataDef.IAttributeDef;
import sig.Signature;
import sfsys.ISfsys;
import sfsys.ISfsys.*;
import sfsys.Sfsys;
import fileutils.Files;

import java.io.*;
import java.util.*;

/** 
 * A SAMS database implementation.
 * Notes:
 * <ul>
 *	<li> Never use a path ending with '/'
 *	<li> Previous returned ISfsys.INode objects are not automatically updated
 *		after a modification (say, addSpectrum); you have to retrieve them
 *		again (e.g getGroupingBy) NOTE: TO BE REVISED	 
 * </ul>
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
class SamsDb implements ISamsDb {
	private static final String PROP_PREFIX = "samscore";
	private static final String G_IMPORTED = "imported";
	private static final String G_COMPUTED = "computed";
	
	private static final String infoFilename = "info.sams";
	private static final String locationDirName = "location";

	/** Each signature file is given this path suffix. */
	static final String SIG_SUFFIX = ".sig";
	
	/** Each metadata spectrum file is given this path suffix. */
	static final String MD_SUFFIX = ".md";
	
	static ISamsDb open(String dirname) throws Exception {
		File baseDir = new File(dirname).getCanonicalFile();
		if ( !baseDir.isDirectory() )
			throw new Exception(baseDir+ ": Not a directory");
		return new SamsDb(baseDir, false);
	}
	
	static ISamsDb create(String dirname) throws Exception {
		File baseDir = new File(dirname).getCanonicalFile();
		if ( baseDir.isDirectory() )
			throw new Exception(baseDir+ ": Directory exists");
		return new SamsDb(baseDir, true);
	}

	private File baseDir;
	private File locationDir;

	private Properties infoProps;
	
	/** Mapping:s attrName->AttributeDef */
	private Map attrDefMap;
	
	/** List of attribute definitions, AttributeDef */
	private List attrDefList;

	private IMetadataDef mddef;
	
	/** my associated clipboard. */
	private Clipboard clipboard;
	

	private SamsDb(File baseDir, boolean create) throws Exception {
		this.baseDir = baseDir;
		locationDir = new File(baseDir, locationDirName);
		infoProps = new Properties();
		attrDefList = new ArrayList();	// basic metadata definition: 
		mddef = new MetadataDef();
		
		if ( create ) {
			if ( !locationDir.mkdirs() )
				throw new Exception(baseDir+ ": Cannot create directory structure");
			
			new File(locationDir, G_IMPORTED).mkdirs();
			new File(locationDir, G_COMPUTED).mkdirs();
			
			attrDefList.add(new AttributeDef("status", "good"));
			save();
		}
		else {
			load();
		}

		_updateAttrDefMap();
		clipboard = new Clipboard();
	}
	
	public Properties getInfoProperties() {
		return infoProps;
	}

	private void _updateAttrDefMap() {
		attrDefMap = new HashMap();
		for ( Iterator iter = attrDefList.iterator(); iter.hasNext(); ) {
			IAttributeDef def = (IAttributeDef) iter.next();
			attrDefMap.put(def.getName(), def);
		}
	}
	
	private void _setAttrDefPropsFromList() {
		int i = 0;
		for ( ; i < attrDefList.size(); i++ ) {
			IAttributeDef attr = (IAttributeDef) attrDefList.get(i);
			String prefix = PROP_PREFIX+ ".attrdef" +i;
			infoProps.setProperty(prefix+ ".name", attr.getName());
			infoProps.setProperty(prefix+ ".defvalue", attr.getDefaultValue());
		}
		// remove possible more definitions:
		for ( ; ; i++ ) {
			String prefix = PROP_PREFIX+ ".attrdef" +i;
			String name = infoProps.getProperty(prefix+ ".name");
			if ( name == null )
				break;
			infoProps.remove(prefix+ ".name");
			infoProps.remove(prefix+ ".defvalue");
		}
	}

	private void _getAttrDefPropsToList() {
		int i = 0;
		for ( ; ; i++ ) {
			String prefix = PROP_PREFIX+ ".attrdef" +i;
			String name = infoProps.getProperty(prefix+ ".name");
			if ( name == null ) 
				break;
			String defvalue = infoProps.getProperty(prefix+ ".defvalue");
			attrDefList.add(new AttributeDef(name, defvalue));
		}
	}

	public void save() throws Exception {
		_setAttrDefPropsFromList();
		File infoFile = new File(baseDir, infoFilename);
		_storeProperties(infoFile, infoProps, "# SAMS Database properties. DO NOT EDIT!"); 
	}

	private void load() throws Exception {
		File infoFile = new File(baseDir, infoFilename);
		_loadProperties(infoFile, infoProps);
		_getAttrDefPropsToList();
	}

	public String getInfo() {
		return baseDir.getPath();
	}
	
	/** gets the grouping by getLocation()". */
	private INode _getGroupingLocation() throws Exception {
		return Sfsys.createDir(locationDir.getPath(), SIG_SUFFIX, true).getRoot();
	}
	
	public INode getGroupingUnderLocation(String subpath) throws Exception {
		INode node = _getGroupingLocation().findNode(subpath);
		return node.isDirectory() ? node : null;
	}
	
	public INode getGroupingBy(String[] attrNames) throws Exception {
		INode dir = null;
		if ( attrNames.length == 1 && attrNames[0].equals("location") )
			dir = _getGroupingLocation();
		else
			dir = _makeGroupingBy(attrNames);
		
		return dir;
	}

	private INode _makeGroupingBy(String[] attrNames) throws Exception {
		ISfsys fs = Sfsys.createMem();
		for ( Iterator it = getAllPaths(); it.hasNext(); ) {
			String path = (String) it.next();
			ISpectrum s = getSpectrum(path);
			ISfsys.INode base = fs.getRoot();
			for ( int i = 0; i < attrNames.length; i++ ) {
				String attrName = attrNames[i];
				String attrVal;
				// assign attrVal depending on type:
				if ( true )  // true: only string is supported now
					attrVal = "'" +s.getString(attrName)+ "'";
				//else other types... PENDING FEATURE
				
				ISfsys.INode val_dir = base.getChild(attrVal);
				if ( val_dir == null )
					val_dir = base.createDirectory(attrVal);
				base = val_dir;
			}
			base.createFile(path);
		}
		return fs.getRoot();
	}
	
	public IMetadataDef getMetadata() {
		return mddef;
	}
	
	public Iterator getAllPaths() {
		List paths = new ArrayList();
		try {
			ISfsys fs = Sfsys.createDir(locationDir.getPath(), SIG_SUFFIX, true);
			_populatePaths(paths, fs.getRoot());
		}
		catch(Exception ex) {
			throw new Error("Unexpected exception: " +ex.getMessage());
		}
		return paths.iterator();
	}
	
	private void _populatePaths(List paths, INode dir) {
		List children = dir.getChildren();
		for ( Iterator iter = children.iterator(); iter.hasNext(); ) {
			INode inode = (INode) iter.next();
			if ( inode.isFile() )
				paths.add(inode.getPath());
			else if ( inode.isDirectory() )
				_populatePaths(paths, inode);
		}
	}
	
	/** Checks attrName corresponds to a defined attribute, including the 
	 * special implicit attributes "location" and "name". */
	private boolean isDefinedAttributeName(String attrName) {
		return attrName.equals("location") || attrName.equals("name")
		    || mddef.get(attrName) != null
		;
	}
		
	public ICondition createCondition(String text) throws Exception {
		return new Condition(text);
	}

	public IOrder createOrder(String text) throws Exception {
		return new Order(text);
	}

	public Iterator selectSpectrums(ICondition condition, IOrder orderBy) throws Exception {
		// first, filter:
		List result = new ArrayList();
		for ( Iterator it = getAllPaths(); it.hasNext(); ) {
			String path = (String) it.next();
			ISpectrum s = getSpectrum(path);
			if ( condition == null || ((Condition) condition).accepts(s) )
				result.add(s);
		}
 
 		// second, order:
		if ( orderBy != null )
			Collections.sort(result, (Comparator) orderBy);

		return result.iterator();
	}
	
	public ISpectrum getSpectrum(String path) throws Exception {
		return new Spectrum(path);
	}
	
	public String addSpectrum(String path, Signature sig) throws Exception {
		setSignature(path, sig);
		return _normalizePath(path);
	}
	
	public void deleteSpectrum(String path) throws Exception {
		path = _normalizePath(path);
		String[] exts = { SIG_SUFFIX, MD_SUFFIX };
		for ( int i = 0; i < exts.length; i++ ) {
			String ext = exts[i];
			File file = new File(locationDir, path + ext);
			if ( file.exists() )
				file.delete();
		}
	}
	
	public Signature getSignature(String path) throws Exception {
		File file = new File(locationDir, normalizePathSignature(path));
		if ( !file.exists() )
			throw new Exception(path+ ": Signature not found");

		BufferedReader stream = null;
		try {
			stream = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			Signature sig = new Signature();
			String line;
			while ( (line = stream.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(line, " ,\t");
				try {
					double x = Double.parseDouble(st.nextToken());
					double y = Double.parseDouble(st.nextToken());
					String info = null;
					try {
						info = st.nextToken();
					}
					catch ( NoSuchElementException ex ) {
						// ignore
					}
					sig.addDatapoint(x, y, info);
				}
				catch ( NoSuchElementException ex ) {
					// ignore
				}
			}
			
			return sig;
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}

	public void setSignature(String path, Signature sig) throws Exception {
		File file = new File(locationDir, normalizePathSignature(path));
		File parent = file.getParentFile();
		if ( !parent.exists() && !parent.mkdirs() )
			throw new Exception("Cannot make directory for: " +file.getAbsolutePath());
		PrintWriter stream = null;
		try {
			stream = new PrintWriter(new FileOutputStream(file));
			int size = sig.getSize();
			for ( int i = 0; i < size; i++ ) {
				Signature.Datapoint p = sig.getDatapoint(i);
				stream.print(p.x+ " , " +p.y);
				if ( p.obj != null )
					stream.print(" , " +p.obj);
				stream.println();
			}
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}
	
	private static void _storeProperties(File file, Properties props, String header)
	throws Exception {
		assert file.getParentFile().exists() ;
		OutputStream stream = null;
		try {
			stream = new BufferedOutputStream(new FileOutputStream(file));
			props.store(stream, header);
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}
	
	private static boolean _loadProperties(File file, Properties props)
	throws Exception {
		assert file.getParentFile().exists() ;
		if ( !file.exists() )
			return false;  // OK, nothing new to load
		InputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(file));
			props.load(stream);
			return true;
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}
	
	private void _storeSpectrumMetadata(String path, Properties attrValues)
	throws Exception {
		File file = new File(locationDir, normalizePathMetadata(path));
		if ( attrValues.size() == 0 ) {
			// no values to store.
			if ( file.exists() )
				file.delete();
			return;
		}
		_storeProperties(file, attrValues, "#Signature metadata");
	}
	
	private boolean _loadSpectrumMetadata(String path, Properties attrValues)
	throws Exception {
		File file = new File(locationDir, normalizePathMetadata(path));
		return _loadProperties(file, attrValues); 
	}
	
	public String renameSpectrum(String oldPath, String newPath) throws Exception {
		oldPath = _normalizePath(oldPath);
		newPath = _normalizePath(newPath);
		if ( oldPath.equals(newPath) )
			return null;   // no renaming neccesary.
		
		String[] exts = { SIG_SUFFIX, MD_SUFFIX };
		for ( int i = 0; i < exts.length; i++ ) {
			String ext = exts[i];
			File oldfile = new File(locationDir, oldPath + ext);
			if ( oldfile.exists() ) {
				File newfile = new File(locationDir, newPath + ext);
				if ( !oldfile.renameTo(newfile) )
					throw new Exception("Cannot rename signature: " +oldfile+ " -> " +newfile);
			}
		}
		return newPath;
	}
	
	final class Spectrum implements ISpectrum {
		String location;
		String name;
		
		/** Attribute name->value mapping. */
		Properties attrValues;
		
		/** creates an element */
		Spectrum(String path) throws Exception {
			int index = path.lastIndexOf("/") + 1;
			location = path.substring(0, index);
			name = path.substring(index);
			attrValues = new Properties();
			_loadSpectrumMetadata(path, attrValues);
		}
		
		public String getLocation() {
			return location;
		}
		public String getName() {
			return name;
		}
		public String getPath() {
			return location + name;
		}
	
		public String getString(String attrName) {
			assert !attrName.equals("path") ;
			if ( attrName.equals("location") )
				return location;
			if ( attrName.equals("name") )
				return name;
			
			String val = attrValues.getProperty(attrName);
			if ( val == null ) {
				IMetadataDef.IAttributeDef attr = mddef.get(attrName);
				if ( attr != null )
					val = attr.getDefaultValue();
			}
			return val;
		}
	
		public void setString(String attrName, String attrValue) {
			assert !attrName.equals("path") ;
			if ( attrName.equals("location") || attrName.equals("name") )
				throw new IllegalArgumentException(attrName);
			
			attrValues.setProperty(attrName, attrValue);
		}
		
		public void save() throws Exception {
			_storeSpectrumMetadata(getPath(), attrValues);
		}
		
		void remove(String attrName) {
			attrValues.remove(attrName);
		}
	}

	class MetadataDef implements IMetadataDef {		
		public IAttributeDef get(String attrName) {
			return (IAttributeDef) attrDefMap.get(attrName);
		}
		
		public int getNumDefinitions() {
			return attrDefList.size();
		}
		
		public List getDefinitions() {
			return attrDefList;
		}
		
		public IAttributeDef add(String attrName, String defaultValue) {
			IAttributeDef attribute = get(attrName);
			if ( attribute != null )
				throw new RuntimeException("Attribute '" +attrName+ "' already exists");

			attribute = new AttributeDef(attrName, defaultValue);
			attrDefMap.put(attrName, attribute);
			attrDefList.add(attribute);

			return attribute;
		}
		
		public void delete(String attrName) {
			IAttributeDef attribute = get(attrName);
			if ( attribute == null )
				throw new RuntimeException("Attribute '" +attrName+ "' does not exist");
			
			attrDefMap.remove(attrName);
			attrDefList.remove(attribute);
			
			// update all spectrum elements:
			Properties attrValues = new Properties();
			for ( Iterator it = getAllPaths(); it.hasNext(); ) {
				String path = (String) it.next();
				attrValues.clear();
				try {
					if ( _loadSpectrumMetadata(path, attrValues) ) {
						if ( attrValues.getProperty(attrName) != null ) {
							attrValues.remove(attrName);
							_storeSpectrumMetadata(path, attrValues);
						}
					}
				}
				catch(Exception ex) {
					System.out.println("Should not happen!  Continuing anyway...");
				}
			}
		}
	}
		
	static class AttributeDef implements IMetadataDef.IAttributeDef {
		String name;
		String defaultValue;
		
		AttributeDef(String name, String defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}
		
		public String getName() {
			return name;
		}
		
		public String getDefaultValue() {
			return defaultValue;
		}
	}

	/** Normalizes the path to a signature file. */
	private static String normalizePathSignature(String path) {
		return _normalizePath(path) + SIG_SUFFIX;
	}
	/** Normalizes the path to a spectrum metadata file. */
	private static String normalizePathMetadata(String path) {
		return _normalizePath(path) +  MD_SUFFIX;
	}
	/** Normalizes a path such that not to end with special suffixes. */
	private static String _normalizePath(String path) {
		assert path.trim().length() > 0;
		path = path.replace('\\', '/');
		path = path.replace(':', '/');
		assert !path.endsWith("/");
		path = path.replaceAll("//+", "/");
		path = path.replaceAll("\\.+$", "");
		String[] ignores = { SIG_SUFFIX, MD_SUFFIX };
		for ( int i = 0; i < ignores.length; i++ ) {
			String ignore = ignores[i];
			if ( path.toLowerCase().endsWith(ignore) ) {
				path = path.substring(0, path.length() - ignore.length());
				break;
			}
		}
		if ( !path.startsWith("/") )
			path = "/" +path;
		return path;
	}
	private static void test_normalizePath() {
		System.out.println("test_normalizePath()");
		assert normalizePathSignature("abc").equals("/abc.ss") ;
		assert normalizePathSignature("//abc//def").equals("/abc/def.ss") ;
		assert normalizePathSignature("abc\\def").equals("/abc/def.ss") ;
		assert normalizePathMetadata("abc:def").equals("/abc/def.md") ;
		assert normalizePathSignature("abc/def.SS").equals("/abc/def.ss") ;
		assert normalizePathMetadata("abc/def.ss").equals("/abc/def.md") ;
	}
	public static void main(String[]_) {
		test_normalizePath();
	}
	
	/** An evaluator of expression */ 
	class Evaluator {
		// fake values should be "big" enough to minimize the chance for
		// RuntimeExceptions during semantic checking (eg StringIndexOutOfBoundsException)
		private final String fake_big_value = new String(new char[1024]).intern();
		
		private bsh.Interpreter bsh;
		private String src;
		private Class returnType;
		
		Evaluator() throws Exception {
			bsh = new bsh.Interpreter();
		}
		
		String getSource() {
			return src;
		}
		
		Class getReturnType() {
			return returnType;
		}
		
		/** Unconditionally sets the source. */
		Evaluator setValidSource(String source) throws Exception {
			src = source;
			return this;
		}
			
		/** Also performs syntax and semantic checking */
		Evaluator setSource(String source) throws Exception {
			returnType = null;
			// change every single quote for double quote (char is not considered): 
			src = source.replace('\'', '"');
			// to distinguish strings:  (escaped quotes are not processed yet)
			String[] parts = (" " +src+ " ").split("\"");
			boolean in_string = false;
			for ( int j = 0; j < parts.length; j++, in_string = !in_string ) {
				if ( !in_string ) {
					// Add underscore to every atribute name (avoid conflict with Java reserved words):
					for ( Iterator iter = attrDefList.iterator(); iter.hasNext(); ) {
						IAttributeDef def = (IAttributeDef) iter.next();
						String name = def.getName();
						
						//System.out.println("<[" +parts[j]+ "]");
						
						// PENDING to make a clever replacement
						parts[j] = parts[j].replaceAll("(" +name+ ")", "$1_");
						//parts[j] = parts[j].replaceAll("\\b(" +name+ ")\\s*([^(])", "$1_$2");
						
						//System.out.println(">[" +parts[j]+ "]");
					}
				}
			}
			// restores quotes:
			src = parts[0];
			for ( int i = 1; i < parts.length; i++ )
				src += "\"" +parts[i];
			
			// PENDING 
			//	replace '==' -> .equals(..)
			// Why beanshell evaluates "abc" == "abc" as false ??
			
			// check syntax:
			try {
				new bsh.Parser(new StringReader(src)).Expression();
			}
			catch(bsh.ParseException ex) {
				System.err.println(ex.getMessage());
				throw new Exception("Syntax error", ex);
			}
			catch(bsh.TokenMgrError ex) {
				System.err.println(ex.getMessage());
				throw new Exception("Syntax error", ex);
			}
			
			// check semantics:
			try {
				bindFakeValues();
			}
			catch(bsh.EvalError ex) {
				throw new Exception("bind error: " +ex.getMessage());
			}
			try {
				// evaluate with those fake values:
				Object obj = bsh.eval(src);
				if ( obj != null ) 
					returnType = obj.getClass();
			}
			catch(bsh.EvalError ex) {
				throw new Exception("evaluation error: " +ex.getMessage());
			}
			return this; // OK
		}
		
		Evaluator bindFakeValues() throws Exception {
			try {
				bsh.set("name", fake_big_value);
				bsh.set("location", fake_big_value);
				for ( Iterator iter = attrDefList.iterator(); iter.hasNext(); ) {
					IAttributeDef def = (IAttributeDef) iter.next();
					String name = def.getName();
					bsh.set(name+ "_", fake_big_value);
				}
				return this;
			}
			catch(bsh.EvalError ex) {
				throw new Exception("bind error: " +ex.getMessage());
			}
		}
		
		Evaluator bind(ISpectrum s) throws Exception {
			try {
				bsh.set("name", s.getName().intern());
				bsh.set("location", s.getLocation().intern());
				for ( Iterator iter = attrDefList.iterator(); iter.hasNext(); ) {
					IAttributeDef def = (IAttributeDef) iter.next();
					String name = def.getName();
					bsh.set(name+ "_", s.getString(name).intern());
				}
				return this;
			}
			catch(bsh.EvalError ex) {
				throw new Exception("bind error: " +ex.getMessage());
			}
		}
		
		Object eval() throws Exception {
			try {
				return bsh.eval(src);
			}
			catch(bsh.EvalError ex) {
				throw new Exception("Eval error: " +ex.getMessage());
			}
		}
	}

	class Condition implements ICondition {
		Evaluator evaluator;

		Condition(String cond_text) throws Exception {
			cond_text = cond_text.trim();
			if ( cond_text.length() > 0 ) {
				evaluator = new Evaluator();
				Class type = evaluator.setSource(cond_text).getReturnType();
				if ( Boolean.class != type )
					throw new Exception("Expression is not a valid condition");
			}
		}

		public String toString() {
			return evaluator == null ? "true" : evaluator.toString();
		}
		
		/** Determines if a spectrum satisfies this condition. */
		boolean accepts(ISpectrum s) throws Exception {
			if ( evaluator == null )
				return true;
			Boolean bool = null;
			try {
				Object obj = evaluator.bind(s).eval();
				if ( !(obj instanceof Boolean) )
					throw new Exception("Expression is not boolean");
				bool = (Boolean) obj;
			}
			catch(Exception ex) {
				System.err.println(ex.getMessage());
			}
			return bool != null && bool.booleanValue();
		}
	}

	/** Implementation of IOrder. */
	class Order implements IOrder, Comparator {
		String src;
		Evaluator evaluator1, evaluator2;
		String[] orderByExpressions;  // the list of expressions

		Order(String ord_text) throws Exception {
			ord_text = ord_text.trim();
			if ( ord_text.length() > 0 ) {
				// simple expression split by using String.split:
				orderByExpressions = ord_text.replaceAll("(:|\\s)+$", "").split(":");

				// src only used for toString:
				src = "";
				for ( int i = 0; i < orderByExpressions.length; i++ )
					src += orderByExpressions[i].trim()+ " : ";

				evaluator1 = new Evaluator();
				for ( int i = 0; i < orderByExpressions.length; i++ ) {
					Class type = evaluator1.setSource(orderByExpressions[i]).getReturnType();
					if ( type == null )
						throw new Exception(evaluator1.getSource()+ ": invalid expression");
					if ( String.class != type )
						throw new Exception(evaluator1.getSource()+ ": result type must be a string (" +type+ ")");
					orderByExpressions[i] = evaluator1.getSource();
				}
				evaluator2 = new Evaluator();
			}
		}

		public String toString() {
			return src;
		}
		
		public int compare(Object o1, Object o2) {
			if ( orderByExpressions != null ) {
				ISpectrum s1 = (ISpectrum) o1;
				ISpectrum s2 = (ISpectrum) o2;
				try {
					evaluator1.bind(s1);
					evaluator2.bind(s2);
					for ( int i = 0; i < orderByExpressions.length; i++ ) {
						String str1 = (String) evaluator1.setValidSource(orderByExpressions[i]).eval();
						String str2 = (String) evaluator2.setValidSource(orderByExpressions[i]).eval();
						int c = str1.compareTo(str2);
						if ( c != 0 )
							return c;
					}
				}
				catch(Exception ex) {
					System.err.println("Order.compare error:" +ex.getMessage());
				}
			}
			return 0;
		}
		
		public INode getGroupingBy() throws Exception {
			ISfsys fs = Sfsys.createMem();
			for ( Iterator it = getAllPaths(); it.hasNext(); ) {
				String path = (String) it.next();
				ISpectrum s = getSpectrum(path);
				evaluator1.bind(s);
				ISfsys.INode base = fs.getRoot();
				for ( int i = 0; i < orderByExpressions.length; i++ ) {
					String str = (String) evaluator1.setValidSource(orderByExpressions[i]).eval();
					String attrVal;
					// assign attrVal depending on type:
					if ( true )  // true: only string is supported now
						attrVal = "'" +str+ "'";
					//else other types... PENDING FEATURE
					
					ISfsys.INode val_dir = base.getChild(attrVal);
					if ( val_dir == null )
						val_dir = base.createDirectory(attrVal);
					base = val_dir;
				}
				base.createFile(path);
			}
			return fs.getRoot();
		}
	
	}

	public IClipboard getClipboard() {
		return clipboard;
	}
	
	/** clipboard element */
	static class ClipboardElement {
		Spectrum spectrum;
		Signature signature;
		ClipboardElement(Spectrum spectrum, Signature signature) {
			assert spectrum != null;
			assert signature != null;
			this.spectrum = spectrum;
			this.signature = signature;
		}
	}
	
	/** the clipboard */
	class Clipboard implements IClipboard {
		// the elements in the clipboard
		List elements = new ArrayList();
		
		// a default observer
		final IObserver null_obs = new IObserver() {
			public void startTask(int total) {}
			public boolean elementFinished(int index, String path, boolean isSpectrum) { return false; } 
			public void endTask(int processed) { }
		};
		IObserver obs = null_obs;
		
		public int size() {
			return elements.size();
		}
		
		public void setObserver(IObserver obs) {
			this.obs = obs == null ? null_obs : obs;
		}
		
		public void copy(List paths) throws Exception {
			if ( paths == null || paths.size() == 0 )
				return;
			obs.startTask(paths.size());
			int processed = 0;
			List new_elements = new ArrayList();
			for ( int i = 0; i < paths.size(); i++ ) {
				String path = (String) paths.get(i);
				Spectrum spec = (Spectrum) getSpectrum(path);
				Signature sig = getSignature(path);
				if ( spec == null )
					System.out.println(path+ ": Not found!!!");
				new_elements.add(new ClipboardElement(spec, sig));
				processed++;
				if ( obs.elementFinished(i+1, path, true) ) {
					obs.endTask(0);  // no element was actually copied.
					return;
				}
			}
			if ( new_elements.size() > 0 )
				elements = new_elements;
			obs.endTask(processed);
		}
		
		public void paste(String target_location) throws Exception {
			if ( elements.size() == 0 )
				return;
			obs.startTask(elements.size());
			int processed = 0;
			for ( int i = 0; i < elements.size(); i++ ) {
				ClipboardElement e = (ClipboardElement) elements.get(i);
				String name = e.spectrum.getName();
				String new_path = _normalizePath(target_location+ "/" +name);
				
				setSignature(new_path, e.signature);
				_storeSpectrumMetadata(new_path, e.spectrum.attrValues);
				
				processed++;
				if ( obs.elementFinished(i+1, new_path, true) )
					break;  // but go to endTask
			}
			obs.endTask(processed);
		}
		
		public void cut(List paths) throws Exception {
			if ( paths == null || paths.size() == 0 )
				return;
			obs.startTask(paths.size());
			int processed = 0;
			elements.clear();
			for ( int i = 0; i < paths.size(); i++ ) {
				String path = (String) paths.get(i);
				Spectrum spec = (Spectrum) getSpectrum(path);
				Signature sig = getSignature(path);
				elements.add(new ClipboardElement(spec, sig));
				deleteSpectrum(path);
				processed++;
				if ( obs.elementFinished(i+1, path, true) )
					break;  // but go to endTask
			}
			obs.endTask(processed);
		}
		
		public void deleteGroups(List groupPaths) throws Exception {
			List paths = new ArrayList();
			for ( Iterator iter = getAllPaths(); iter.hasNext(); ) {
				String path = (String) iter.next();
				boolean include = false;
				for ( Iterator iterg = groupPaths.iterator(); iterg.hasNext(); ) {
					String group_path = (String) iterg.next();
					if ( group_path.equals("/") || path.startsWith(group_path+ "/") ) {
						include = true;
						break;
					}
				}
				if ( include && !paths.contains(path) )
					paths.add(path);
			}
			obs.startTask(paths.size() + groupPaths.size());
			int processed = 0;
			int i = 0;
			boolean canceled = false;
			// delete spectra paths first:
			for ( ; i < paths.size(); i++ ) {
				String path = (String) paths.get(i);
				deleteSpectrum(path);
				processed++;
				if ( obs.elementFinished(i+1, path, true) ) {
					canceled = true;
					break;
				}
			}
			if ( !canceled ) {
				// now delete directories except those that we want to keep:
				for ( Iterator iterg = groupPaths.iterator(); iterg.hasNext(); ) {
					String group_path = (String) iterg.next();
					if ( !group_path.equals("/") ) {
						File dir = new File(locationDir, group_path);
						if ( !dir.getName().equals(G_IMPORTED)
						&&   !dir.getName().equals(G_COMPUTED) ) {
							if ( dir.exists() )
								dir.delete();  // should be empty
							processed++;
							if ( obs.elementFinished(i+1, group_path, false) )
								break;
						}
					}
					i++;
				}
			}
			obs.endTask(processed);
		}
		
		public void delete(List paths) throws Exception {
			if ( paths == null || paths.size() == 0 )
				return;
			obs.startTask(paths.size());
			int processed = 0;
			for ( int i = 0; i < paths.size(); i++ ) {
				String path = (String) paths.get(i);
				deleteSpectrum(path);
				processed++;
				if ( obs.elementFinished(i+1, path, true) )
					break;  // but go to endTask
			}
			obs.endTask(processed);
		}
	}
}
