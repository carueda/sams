package sfsys;

/**
 * Constructor of ISfsys objects.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class Sfsys {
	/** Creates a directory-based fs with root in that directory. */
	public static ISfsys createDir(String dirname) throws Exception {
		return sfsys.impl.DirSfsys.createSfsys(dirname);
	}

	/** Creates a directory-based fs with root in that directory. */
	public static ISfsys createDir(String dirname, String file_ext) throws Exception {
		return sfsys.impl.DirSfsys.createSfsys(dirname, file_ext);
	}

	/** Creates an empty memory-based ISfsys. */
	public static ISfsys createMem() throws Exception {
		return sfsys.impl.MemSfsys.createSfsys();
	}
	
	/** Creates a memory-based ISfsys loaded from a file. */
	public static ISfsys createMem(String filename) throws Exception {
		return sfsys.impl.MemSfsys.createSfsys(filename);
	}
	
	private Sfsys() {}
}
