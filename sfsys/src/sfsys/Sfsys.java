package sfsys;

/**
 * Constructor of ISfsys objects.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class Sfsys {
	/**
	 * Creates an ISfsys.
	 * @param filename  
	 *	If null, a memory-based, new fs is created
	 *	If filename is an existing file, a memory-based fs is loaded
	 *      from that file.
	 *	If filename is a directory, a directory-based fs is created
	 *      with root in that directory.
	 */
	public static ISfsys create(String filename) throws Exception {
		if ( filename == null )
			return sfsys.impl.MemSfsys.createSfsys();
		
		java.io.File file = new java.io.File(filename);
		if ( !file.exists() )
			throw new Exception(filename+ ": no such file or directory");
		
		if ( file.isDirectory() )
			return sfsys.impl.DirSfsys.createSfsys(filename);
		else
			return sfsys.impl.MemSfsys.createSfsys(filename);
	}

	private Sfsys() {}
}
