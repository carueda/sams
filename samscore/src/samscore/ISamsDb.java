package samscore;

import sig.Signature;
import sfsys.ISfsys;

import java.util.List;
import java.util.Iterator;

/** 
 * A SAMS database.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public interface ISamsDb {

	/** Gets general info associated. */
	public String getInfo();

	/** save. */
	public void save() throws Exception;

	/** Gets the metadata definition associated. */
	public IMetadataDef getMetadata();
	
	/** Gets an element */
	public ISpectrum getSpectrum(String path) throws Exception;
	
	/** Gets all elements */
	public Iterator getSpectrumIterator();
	
	/** Creates a condition. */
	public ICondition createCondition(String text) throws Exception;

	/** Select elements that satisfy a condition and in a given order.
	 * @param condition can be null. 
	 * @orderBy comma-separated field names. null or empty is equivalent to "location,name". 
	 */
	public Iterator select(ICondition condition, String orderBy) throws Exception ;
	
	/** Adds an element */
	public ISpectrum addSpectrum(String path, Signature sig) throws Exception;

	/** Gets a signature */
	public Signature getSignature(String path) throws Exception;
	
	/** Sets a signature */
	public void setSignature(String path, Signature sig) throws Exception;
	
	/** gets a grouping structure according to attribute values. */
	public ISfsys getGroupingBy(String[] attrNames) throws Exception;
	
	/** gets the grouping by "location". */
	public ISfsys getGroupingLocation() throws Exception;
	
	/** Represents an element in this database. */
	public interface ISpectrum {
		/** Gets the path of this element. */
		public String getPath();
	
		/** Gets the value of an attribute. */
		public String getString(String attrName);
	
		/** Sets the value of an string attribute. */
		public void setString(String attrName, String attrValue);

		/** Gets the associated signature */
		public Signature getSignature() throws Exception;
		
		/** Sets the associated signature */
		public void setSignature(Signature sig) throws Exception;
	
	}

	/** Metadata definition */
	public interface IMetadataDef {
		/** number of definitions */
		public int getNumDefinitions();
		
		/** gets all definitions, IAttributeDef */
		public List getDefinitions();
		
		/** gets an attribute. */
		public IAttributeDef get(String name);
		
		/** adds an attribute. */
		public IAttributeDef add(String name, String defaultValue);
		
		/** deletes an attribute. */
		public void delete(String name);
		
		/** Definition of an attribute. */
		public interface IAttributeDef {
			
			/** Gets the name of this attribute. */
			public String getName();
			
			/** Gets the default value of this attribute. */
			public String getDefaultValue();
			
			/** determines if this attribute is editable. */
			public boolean isEditable();
		}
	}

	/** Represents a condition to select elements. */
	public interface ICondition {
		/** Gets a string representation of this condition. */
		public String toString();	
	}
	
	/** Returns the associated clipboard. */
	public IClipboard getClipboard() throws Exception;
	
	/** Provides clipboard-like actions on this database. */
	public interface IClipboard {
		/** Copies a list of spectra into this clipboard. */
		public void copy(List paths) throws Exception;
		
		/** Cuts a list of spectra into this clipboard. */
		public void cut(List paths) throws Exception;
		
		/** Pastes the spectra in this clipboard into the given location. */
		public void paste(String target_location) throws Exception;
		
		/** Deletes the list of given spectra. */
		public void delete(List paths) throws Exception;
	}
}
