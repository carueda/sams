package samscore;

import sig.Signature;
import sfsys.ISfsys;
import sfsys.ISfsys.INode;

import java.util.List;
import java.util.Iterator;
import java.util.Properties;

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
	public Iterator getAllPaths();
	
	/** Creates a condition specification to be used by selectSpectrums. */
	public ICondition createCondition(String text) throws Exception;

	/** Creates a ordering specification to be used by selectSpectrums
	 * @param text A list of expressions separated by ':'.
	 *		Example: "location ; name.substring(0,5)"
	 */
	public IOrder createOrder(String text) throws Exception;

	/** Select elements that satisfy a condition and order.
	 * @param condition Condition for desired elements.
	 *		null is semantically equivalent to true.
	 * @param OrderBy Order for selected elements. 
	 *		null or empty is semantically equivalent to "order by location, then name." 
	 */
	public Iterator selectSpectrums(ICondition condition, IOrder orderBy) throws Exception ;
	
	/** Adds an element 
	 * Returns normalized path.
	 */
	public String addSpectrum(String path, Signature sig) throws Exception;

	/** Removes an element including its signature file. */
	public void deleteSpectrum(String path) throws Exception;
	
	/** Renames an element. 
	 * Returns the normalized new path; null if not change was necessary at all.
	 */
	public String renameSpectrum(String oldPath, String newPath) throws Exception;

	/** Gets a signature 
	 * @throw  Exception if signature not found or cannot be read in.
	 */
	public Signature getSignature(String path) throws Exception;
	
	/** Sets a signature */
	public void setSignature(String path, Signature sig) throws Exception;
	
	/** gets a sub-grouping by getLocation(). */
	public INode getGroupingUnderLocation(String path) throws Exception;

	/** gets a grouping structure according to attribute values. */
	public INode getGroupingBy(String[] attrNames) throws Exception;
	
	/**
	 * Gets the general properties associated to this database. 
	 * A client may use this to store its own properties related to
	 * this database; Internal property names start with "samscore";
	 * a client should use a different prefix for its names.
	 * A call to save() always stores the current state of this properties.
	 */
	public Properties getInfoProperties();
	
	/** Represents an element in this database. */
	public interface ISpectrum {
		/** Gets the path of this element, which is equal to getLocation + getname() */
		public String getPath();
		
		/** gets the location. Always ends with "/". */
		public String getLocation();
		
		/** gets the name. */
		public String getName();
	
		/** Gets the value of an attribute.
		 * @param attrName "location" -> getLocation()
		 *                 "name" -> getName()
		 */
		public String getString(String attrName);
	
		/** Sets the value of an string attribute. 
		 * Use db.renameSpectrum(*) to rename an element.
		 * @throw IllegalArgumentException if attrName is "location" or "name".
		 */
		public void setString(String attrName, String attrValue);
		
		/** Saves this spectrum. */
		public void save() throws Exception;
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
		}
	}

	/** Represents a condition to select elements. */
	public interface ICondition {
		/** Gets a string representation of this condition. */
		public String toString();	
	}
	
	/** Represents an order to select elements. */
	public interface IOrder {
		/** gets the grouping structure reflecting this order specification. */
		public INode getGroupingBy() throws Exception;
		
		/** Gets a string representation of this order specification. */
		public String toString();	
	}
	
	/** Returns the associated clipboard. */
	public IClipboard getClipboard();
	
	/** Provides clipboard-like tasks on this database. */
	public interface IClipboard {
		/** Copies a list of spectra into this clipboard. */
		public void copy(List paths) throws Exception;
		
		/** Pastes the spectra in this clipboard into the given location. */
		public void paste(String target_location) throws Exception ;
		
		/** Cuts a list of spectra into this clipboard. */
		public void cut(List paths) throws Exception;
		
		/** Deletes the list of given spectra. 
		 * This operation that doesn't change the current contents. */
		public void delete(List paths) throws Exception;
		
		/** Deletes the list of given groups (recursively). 
		 * This operation that doesn't change the current contents. */
		public void deleteGroups(List paths) throws Exception;
		
		/** Gets the number of elements in this clipboard. */
		public int size();
		
		/** Sets the task observer. */
		public void setObserver(IObserver obs);
		
		/** Interface for clipboard task observers. */
		public interface IObserver {
			/** called with the number of elements to be processed in a task. */
			public void startTask(int total);
			
			/** called when an element has just been processed. Note: index in [1..total]. 
			 * @return true to stop the task. */
			public boolean elementFinished(int index, String path, boolean isSpectrum);

			/** called with the number of elements successfully processed in a task. */
			public void endTask(int processed);
		}
	}
}
