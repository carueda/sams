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
 *	<li> Previous returned ISfsys (or INode) objects are not automatically updated
 *		after a modification (say, addSpectrum); you have to retrieve
 *		a new ISfsys object (e.g getGroupingBy).	 
 * </ul>
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
class SamsDb implements ISamsDb {
	private static final String G_IMPORTED = "imported";
	private static final String G_COMPUTED = "computed";
	
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

	private static final String infoName = "info.sams";
	private static final String sigsDirName = "sigs";
	private static final String computedDirName = G_COMPUTED;

	private File baseDir;
	private File sigsDir;

	/** Mapping:s attrName->AttributeDef */
	private Map attrDefMap;
	
	/** List of attribute definitions, AttributeDef */
	private List attrDefList;

	private IMetadataDef mddef;
	
	/** my associated clipboard. */
	private Clipboard clipboard;
	

	private SamsDb(File baseDir, boolean create) throws Exception {
		this.baseDir = baseDir;
		sigsDir = new File(baseDir, sigsDirName);
		mddef = new MetadataDef();
		
		if ( create ) {
			if ( !sigsDir.mkdirs() )
				throw new Exception(baseDir+ ": Cannot create directory structure");
			
			new File(sigsDir, G_IMPORTED).mkdirs();
			new File(sigsDir, G_COMPUTED).mkdirs();
			
			attrDefList = new ArrayList();	// basic metadata definition: 
			attrDefList.add(new AttributeDef("status", "good"));
			save();
		}
		else {
			load();
		}

		_updateAttrDefMap();
		clipboard = new Clipboard();
	}

	private void _updateAttrDefMap() {
		attrDefMap = new HashMap();
		for ( Iterator iter = attrDefList.iterator(); iter.hasNext(); ) {
			IAttributeDef def = (IAttributeDef) iter.next();
			attrDefMap.put(def.getName(), def);
		}
	}
	
	public void save() throws Exception {
		File infoFile = new File(baseDir, infoName);
		ObjectOutputStream stream = null;
		try {
			stream = new ObjectOutputStream(new FileOutputStream(infoFile));
			stream.writeObject(attrDefList);
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}

	private void load() throws Exception {
		File infoFile = new File(baseDir, infoName);
		ObjectInputStream stream = null;
		try {
			stream = new ObjectInputStream(new FileInputStream(infoFile));
			attrDefList = (List) stream.readObject();
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}

	public String getInfo() {
		return baseDir.getPath();
	}
	
	public INode getGroupingUnderLocation(String subpath) throws Exception {
		INode inode = getGroupingLocation().getNode(subpath);
		if ( inode.isDirectory() )
			return inode;
		else
			return null;
	}
	
	public ISfsys getGroupingLocation() throws Exception {
		return Sfsys.createDir(sigsDir.getPath(), SIG_SUFFIX, true);
	}
	
	public ISfsys getGroupingBy(String[] attrNames) throws Exception {
		ISfsys fs = null;
		if ( attrNames.length == 1 && attrNames[0].equals("location") )
			fs = getGroupingLocation();
		else
			fs = _makeGroupingBy(attrNames);
		
		return fs;
	}

	private ISfsys _makeGroupingBy(String[] attrNames) throws Exception {
		ISfsys fs = Sfsys.createMem();

		for ( Iterator it = getAllPaths(); it.hasNext(); ) {
			String path = (String) it.next();
			ISpectrum s = getSpectrum(path);
			 
			ISfsys.INode base = fs.getRoot();
			for ( int i = 0; i < attrNames.length; i++ ) {
				String attrName = attrNames[i];
				String attrVal = s.getString(attrName);
				ISfsys.INode val_dir = base.getNode(attrVal);
				if ( val_dir == null )
					val_dir = base.createDirectory(attrVal);
				base = val_dir;
			}
			base.createFile(path);
		}
		return fs;
	}
	
	public IMetadataDef getMetadata() {
		return mddef;
	}
	
	public Iterator getAllPaths() {
		List paths = new ArrayList();
		try {
			ISfsys fs = Sfsys.createDir(sigsDir.getPath(), SIG_SUFFIX, true);
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
	
	public ICondition createCondition(String text) throws Exception {
		return new Condition(text);
	}

	public Iterator selectSpectrums(ICondition condition, String orderBy) throws Exception {
		List result = new ArrayList();
		for ( Iterator it = getAllPaths(); it.hasNext(); ) {
			String path = (String) it.next();
			ISpectrum s = getSpectrum(path);
			if ( condition == null || ((Condition) condition).accepts(s) )
				result.add(s);
		}
		if ( orderBy == null || orderBy.trim().length() == 0 )
			orderBy = "location,name";
 
		final String[] orderByAttrNames = orderBy.split("(,|\\s)+");
		Comparator comparator = new Comparator() {
			public int compare(Object o1, Object o2){
				ISpectrum s1 = (ISpectrum) o1;
				ISpectrum s2 = (ISpectrum) o2;
				for ( int i = 0; i < orderByAttrNames.length; i++ ) {
					int c = s1.getString(orderByAttrNames[i]).compareTo(s2.getString(orderByAttrNames[i]));
					if ( c != 0 )
						return c;
				}
				return 0;
			}
		};
		Collections.sort(result, comparator);

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
			File file = new File(sigsDir, path + ext);
			if ( file.exists() )
				file.delete();
		}
	}
	
	public Signature getSignature(String path) throws Exception {
		File file = new File(sigsDir, normalizePathSignature(path));
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
			sig.sort();
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
		File file = new File(sigsDir, normalizePathSignature(path));
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
	
	private void _storeSpectrumMetadata(String path, Properties attrValues)
	throws Exception {
		File file = new File(sigsDir, normalizePathMetadata(path));
		assert file.getParentFile().exists() ;
		if ( attrValues.size() == 0 ) {
			// no values to store.
			if ( file.exists() )
				file.delete();
			return;
		}
		OutputStream stream = null;
		try {
			stream = new BufferedOutputStream(new FileOutputStream(file));
			attrValues.store(stream, "#Signature metadata");
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}
	
	private boolean _loadSpectrumMetadata(String path, Properties attrValues)
	throws Exception {
		File file = new File(sigsDir, normalizePathMetadata(path));
		assert file.getParentFile().exists() ;
		if ( !file.exists() )
			return false;  // OK
		InputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(file));
			attrValues.load(stream);
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
	
	public String renameSpectrum(String oldPath, String newPath) throws Exception {
		oldPath = _normalizePath(oldPath);
		newPath = _normalizePath(newPath);
		if ( oldPath.equals(newPath) )
			return null;   // no renaming neccesary.
		
		String[] exts = { SIG_SUFFIX, MD_SUFFIX };
		for ( int i = 0; i < exts.length; i++ ) {
			String ext = exts[i];
			File oldfile = new File(sigsDir, oldPath + ext);
			if ( oldfile.exists() ) {
				File newfile = new File(sigsDir, newPath + ext);
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
		
	static class AttributeDef implements IMetadataDef.IAttributeDef, Serializable {
		String name;
		String defaultValue;
		boolean editable;
		
		AttributeDef(String name, String defaultValue) {
			this(name, defaultValue, true);
		}
		
		AttributeDef(String name, String defaultValue, boolean editable) {
			this.name = name;
			this.defaultValue = defaultValue;
			this.editable = editable;
		}
		
		public String getName() {
			return name;
		}
		
		public String getDefaultValue() {
			return defaultValue;
		}
		
		public boolean isEditable() {
			return editable;
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

	class Condition implements ICondition {
		String text;
		String attrName;
		String attrValue;

		Condition(String cond_text) throws Exception {
			text = cond_text.trim();
			if ( text.length() == 0 )
				text = null;
			if ( text != null ) {
				StringTokenizer st = new StringTokenizer(text);
				try {
					attrName = st.nextToken();
					String eq = st.nextToken();
					if ( !eq.equals("=") )
						throw new Exception("Syntax error");
					attrValue = st.nextToken();
				}
				catch ( NoSuchElementException ex ) {
					throw new Exception("Syntax error");
				}
	
				// check attrName is valid:
				IAttributeDef atr = mddef.get(attrName);
				if ( atr == null )
					throw new Exception(attrName+ ": Not a valid attribute");
			}
		}

		public String toString() {
			return text != null ? text : "<empty>";
		}

		/** Determines if a spectrum satisfies this query. */
		boolean accepts(ISpectrum s) {
			if ( text == null )
				return true;
			String val = s.getString(attrName);
			return val != null && val.equals(attrValue);
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
					if ( path.startsWith(group_path+ "/") ) {
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
				// now delete directories:
				for ( Iterator iterg = groupPaths.iterator(); iterg.hasNext(); ) {
					String group_path = (String) iterg.next();
					File dir = new File(sigsDir, group_path);
					if ( !dir.getName().equals(G_IMPORTED)
					&&   !dir.getName().equals(G_COMPUTED) ) {
						if ( dir.exists() )
							dir.delete();  // should be empty
						processed++;
						if ( obs.elementFinished(i+1, group_path, false) )
							break;
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
