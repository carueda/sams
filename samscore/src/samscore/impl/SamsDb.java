package samscore.impl;

import samscore.ISamsDb;
import samscore.ISamsDb.IMetadataDef.IAttributeDef;
import sig.Signature;
import sfsys.ISfsys;
import sfsys.Sfsys;

import java.io.*;
import java.util.*;

/** 
 * A SAMS database implementation.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
class SamsDb implements ISamsDb {
	
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
	private static final String spectraDirName = "signatures";

	private File baseDir;
	private File spectraDir;
	private ISfsys location_fs;

	/** Mapping:s attrName->AttributeDef */
	private Map attrDefMap;
	
	/** List of attribute definitions, AttributeDef */
	private List attrDefList;

	/** The spectrum elements. The mapping is path->Spectrum */
	private Map spectrums;

	private IMetadataDef mddef;
	
	private IPathNormalizer pathNormalizer; 


	private SamsDb(File baseDir, boolean create) throws Exception {
		this.baseDir = baseDir;
		spectraDir = new File(baseDir, spectraDirName);
		mddef = new MetadataDef();
		
		if ( create ) {
			if ( ! spectraDir.mkdirs() )
				throw new Exception(baseDir+ ": Cannot create directory structure");
			
			attrDefList = new ArrayList();	// basic metadata definition: 
			attrDefList.add(new AttributeDef("location", "", false));
			attrDefList.add(new AttributeDef("name", "", false));
			attrDefList.add(new AttributeDef("status", "good"));
			spectrums = new HashMap();	// empty element table
			save();
		}
		else {
			load();
		}

		_updateAttrDefMap();
		pathNormalizer = new PathNormalizer();
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
			stream.writeObject(spectrums);
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
			spectrums = (Map) stream.readObject();
		}
		catch ( Exception ex ) {
			throw new Exception(ex.getClass().getName()+ " : " +ex.getMessage());
		}
		finally {
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
		
		// update metadata definition for each spectrum
		for ( Iterator it = spectrums.values().iterator(); it.hasNext(); ) {
			Spectrum s = (Spectrum) it.next();
			s.db = this;
		}		
	}

	public String getInfo() {
		return baseDir.getPath();
	}
	
	public ISfsys getGroupingBy(String[] attrNames) throws Exception {
		ISfsys fs = null;
		if ( attrNames.length == 1 && attrNames[0].equals("location") ) {
			if ( location_fs == null )
				location_fs = Sfsys.create(spectraDir.getPath());
			fs = location_fs;
		}
		else
			fs = _makeGroupingBy(attrNames);
		
		return fs;
	}
	
	private IAttributeDef[] _getAttrDefs(String[] attrNames) throws Exception {
		IAttributeDef[] attrs = new IAttributeDef[attrNames.length];
		for ( int i = 0; i < attrNames.length; i++ ) {
			String attrName = attrNames[i];
			attrs[i] = mddef.get(attrName);
			if ( attrs[i] == null ) 
				throw new Exception(attrName+ ": Undefined attribute");
		}
		return attrs;
	}
	
	private ISfsys _makeGroupingBy(String[] attrNames) throws Exception {
		IAttributeDef[] attrs = _getAttrDefs(attrNames);
		ISfsys fs = Sfsys.create(null);

		for ( Iterator it = getSpectrumIterator(); it.hasNext(); ) {
			 ISamsDb.ISpectrum s = (ISamsDb.ISpectrum) it.next();
			 String path = s.getPath();
			 
			 ISfsys.IDirectory base = fs.getRoot();
			 for ( int i = 0; i < attrNames.length; i++ ) {
				 String attrName = attrNames[i];
				 String attrVal = s.getString(attrName);
				 ISfsys.IDirectory val_dir = (ISfsys.IDirectory) base.getNode(attrVal);
				 if ( val_dir == null )
					 val_dir = base.createDirectory(attrVal);
				 base = val_dir;
			 }
			 base.createFile(path);
		}
		
		return fs;
	}
	
	public IPathNormalizer getPathNormalizer() {
		return pathNormalizer;
	}
	
	public IMetadataDef getMetadata() {
		return mddef;
	}
	
	public Iterator getSpectrumIterator() {
		return spectrums.values().iterator();
	}
	
	public ICondition createCondition(String text) throws Exception {
		return new Condition(text);
	}

	public Iterator select(ICondition condition, String orderBy) throws Exception {
		List result = new ArrayList();
		for ( Iterator it = getSpectrumIterator(); it.hasNext(); ) {
			ISpectrum s = (ISpectrum) it.next();
			if ( condition == null || ((Condition) condition).accepts(s) )
				result.add(s);
		}
		if ( orderBy != null && orderBy.length() > 0 ) { 
			final String[] orderByAttrNames = orderBy.split("(,|\\s)+");
			final IAttributeDef[] attrs = _getAttrDefs(orderByAttrNames);
			Comparator comparator = new Comparator() {
				public int compare(Object o1, Object o2){
					ISpectrum s1 = (ISpectrum) o1;
					ISpectrum s2 = (ISpectrum) o2;
					for ( int i = 0; i < attrs.length; i++ ) {
						int c = s1.getString(orderByAttrNames[i]).compareTo(s2.getString(orderByAttrNames[i]));
						if ( c != 0 )
							return c;
					}
					return 0;
				}
			};
			Collections.sort(result, comparator);
		}
		return result.iterator();
	}
	
	public ISpectrum getSpectrum(String path) throws Exception {
		path = getPathNormalizer().normalize(path);
		return (ISpectrum) spectrums.get(path);
	}
	
	public ISpectrum addSpectrum(String path, Signature sig) throws Exception {
		path = getPathNormalizer().normalize(path);
		ISpectrum spectrum = (ISpectrum) spectrums.get(path);
		if ( spectrum == null ) {
			spectrum = new Spectrum(this, path);
			spectrums.put(path, spectrum);
		}
		setSignature(path, sig);
		return spectrum;
	}
	
	public Signature getSignature(String path) throws Exception {
		path = getPathNormalizer().normalize(path);
		File file = new File(spectraDir, path+ ".txt");
		if ( !file.exists() )
			return null;

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
					sig.addDatapoint(x, y);
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
		path = getPathNormalizer().normalize(path);
		File file = new File(spectraDir, path+ ".txt");
		File parent = file.getParentFile();
		if ( !parent.exists() && !parent.mkdirs() )
			throw new Exception("Cannot make directory for : " +file.getAbsolutePath());
		PrintWriter stream = null;
		try {
			stream = new PrintWriter(new FileOutputStream(file));
			int size = sig.getSize();
			for ( int i = 0; i < size; i++ ) {
				Signature.Datapoint p = sig.getDatapoint(i);
				stream.print(p.x+ " , " +p.y);
				//if ( p.obj != null ) {
				//	stream.print("   # " +p.obj);
				//}
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
	
	final static class Spectrum implements ISpectrum, Serializable {
		String path;
		
		/** Attribute name->value mapping. */
		Map attrValues;
		
		/** my database. */
		transient SamsDb db;
		
		/** creates an element */
		Spectrum(SamsDb db, String path) {
			this.db = db;
			this.path = path;
			int index = path.lastIndexOf("/") + 1;
			String location = path.substring(0, index);
			String name = path.substring(index);
			attrValues = new HashMap();
			attrValues.put("location", location);
			attrValues.put("name", name);
		}
		
		public String getPath() {
			return path;
		}
	
		public String getString(String attrName) {
			String val = null;
			if ( attrName.equals("path") )
				val = getPath();
			else {
				val = (String) attrValues.get(attrName);
				if ( val == null ) {
					IMetadataDef.IAttributeDef attr = db.mddef.get(attrName);
					if ( attr != null )
						val = attr.getDefaultValue();
				}
			}
			return val;
		}
	
		public void setString(String attrName, String attrValue) {
			if ( attrName.equals("path") )
				path = attrValue;
			else
				attrValues.put(attrName, attrValue);
		}
		
		public Signature getSignature() throws Exception {
			return db.getSignature(getPath());
		}

		public void setSignature(Signature sig) throws Exception {
			db.setSignature(getPath(), sig);
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
			for ( Iterator it = spectrums.values().iterator(); it.hasNext(); ) {
				Spectrum s = (Spectrum) it.next();
				s.remove(attrName);
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

	/**
		The path normalizer used in this database implementation.
		This path normalizer works as follows:
		<ul>
			<li> Any ignored suffix is deleted. Currently, only ".txt".
			<li> File.separatorChar replaced by '/'
			<li> Prefix "/" added if necessary
			<li> "/+$" deleted
			<li> "//+" replaced by "/"
		</ul>
		Examples:
		<ul>
			<li> "" --> "/"
			<li> "/" --> "/"
			<li> "abc*" --> "/abc*"
			<li> "/abc.txt" --> "/abc"
			<li> "//abc//def" --> "/abc/def"
			<li> "/abc/" --> "/abc"
		</ul>
	*/
	static class PathNormalizer implements IPathNormalizer {
		private String[] ignoredSuffixes = { ".txt" };
		
		public String normalize(String path) {
			for ( int i = 0; i < ignoredSuffixes.length; i++ ) {
				String suffix = ignoredSuffixes[i];
				if ( path.toLowerCase().endsWith(suffix.toLowerCase()) ) {
					path = path.substring(0, path.length() - suffix.length());
					break;
				}
			}
			path = path.replace(File.separatorChar, '/');
			if ( !path.startsWith("/") )
				path = "/" +path;
			if ( !path.equals("/") ) {
				path = path.replaceAll("//+", "/");
				path = path.replaceAll("/$", "");
			}
			return path;
		}
		
		public List getIgnoredSuffixes() {
			return Arrays.asList(ignoredSuffixes);
		}
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
}
