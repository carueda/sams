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
	public static ISfsys createDir(String dirname, String fileExt, boolean hideFileExt) throws Exception {
		return sfsys.impl.DirSfsys.createSfsys(dirname, fileExt, hideFileExt);
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
