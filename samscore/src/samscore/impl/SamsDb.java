package samscore.impl;

import samscore.ISamsDb;
import samscore.ISamsDb.IMetadataDef.IAttributeDef;
import sig.Signature;
import sfsys.ISfsys;
import sfsys.ISfsys.*;
import sfsys.Sfsys;

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
	private static final String computedDirName = "computed";

	private File baseDir;
	private File sigsDir;

	/** Mapping:s attrName->AttributeDef */
	private Map attrDefMap;
	
	/** List of attribute definitions, AttributeDef */
	private List attrDefList;

	/** The spectrum elements. The mapping is path->Spectrum */
	private Map spectrums;

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
			
			new File(sigsDir, "imported").mkdirs();
			new File(sigsDir, "computed").mkdirs();
			
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
	
	public IDirectory getGroupingUnderLocation(String subpath) throws Exception {
		INode inode = getGroupingLocation().getNode(subpath);
		if ( inode instanceof IDirectory )
			return (IDirectory) inode;
		else
			return null;
	}
	
	public ISfsys getGroupingLocation() throws Exception {
		return Sfsys.create(sigsDir.getPath());
	}
	
	public ISfsys getGroupingBy(String[] attrNames) throws Exception {
		ISfsys fs = null;
		if ( attrNames.length == 1 && attrNames[0].equals("location") )
			fs = getGroupingLocation();
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
		if ( orderBy == null || orderBy.trim().length() == 0 )
			orderBy = "location,name";
 
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

		return result.iterator();
	}
	
	public ISpectrum getSpectrum(String path) throws Exception {
		path = normalizePath(path);
		return (ISpectrum) spectrums.get(path);
	}
	
	public ISpectrum addSpectrum(String path, Signature sig) throws Exception {
		path = normalizePath(path);
		ISpectrum spectrum = (ISpectrum) spectrums.get(path);
		if ( spectrum == null ) {
			spectrum = new Spectrum(this, path);
			spectrums.put(path, spectrum);
		}
		setSignature(path, sig);
		return spectrum;
	}
	
	public void deleteSpectrum(String path) throws Exception {
		path = normalizePath(path);
		// remove entry:
		ISpectrum spectrum = (ISpectrum) spectrums.get(path);
		if ( spectrum != null )
			spectrums.remove(path);
		// remove signature:
		File file = new File(sigsDir, path);
		if ( file.exists() )
			file.delete();
	}
	
	public Signature getSignature(String path) throws Exception {
		path = normalizePath(path);
		File file = new File(sigsDir, path);
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
		path = normalizePath(path);
		File file = new File(sigsDir, path);
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
					stream.print("   # " +p.obj);
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
		
		/** convenience no-arg constructor for clone() */
		Spectrum() { }
		
		/** creates a copy but with path/location/name updated according to new_path. */
		Spectrum clone(String new_path) {
			Spectrum clone = new Spectrum();
			clone.attrValues = new HashMap();
			for ( Iterator iter = attrValues.keySet().iterator(); iter.hasNext(); ) {
				String name = (String) iter.next();
				String value = (String) attrValues.get(name);
				clone.attrValues.put(name, value);
			}
			clone.db = db;
			clone.path = new_path;
			int index = clone.path.lastIndexOf("/") + 1;
			String location = clone.path.substring(0, index);
			String name = clone.path.substring(index);
			clone.attrValues.put("location", location);
			clone.attrValues.put("name", name);
			return clone;
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

	/** Normalizes the path to a regular file. See test_normalizePath for examples. */
	static String normalizePath(String path) {
		assert path.trim().length() > 0;
		path = path.replace('\\', '/');
		path = path.replace(':', '/');
		assert !path.endsWith("/");
		path = path.replaceAll("//+", "/");
		if ( !path.endsWith(".txt") )
			path += ".txt";
		if ( !path.startsWith("/") )
			path = "/" +path;
		return path;
	}
	static void test_normalizePath() {
		System.out.println("test_normalizePath()");
		assert normalizePath("abc").equals("/abc.txt") ;
		assert normalizePath("//abc//def").equals("/abc/def.txt") ;
		assert normalizePath("abc\\def").equals("/abc/def.txt") ;
		assert normalizePath("abc:def").equals("/abc/def.txt") ;
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
			public boolean elementFinished(int index, String path) { return false; } 
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
				if ( obs.elementFinished(i+1, path) ) {
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
				String name = e.spectrum.getString("name");
				String new_path = normalizePath(target_location+ "/" +name);
				
				Spectrum modified = (Spectrum) spectrums.get(new_path);
				if ( modified == null ) {
					modified = e.spectrum.clone(new_path);
					spectrums.put(new_path, modified);
				}
				modified.setSignature(e.signature);
				processed++;
				if ( obs.elementFinished(i+1, new_path) )
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
				if ( obs.elementFinished(i+1, path) )
					break;  // but go to endTask
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
				if ( obs.elementFinished(i+1, path) )
					break;  // but go to endTask
			}
			obs.endTask(processed);
		}
	}
}
