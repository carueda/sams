package samscore;

import samscore.impl.SamsDbFactory;
import sig.Signature;
import sigoper.SignatureOperationManager;
import specfile.*;

import java.util.*;
import java.io.File;

/**
 * Point of access to SAMS core.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Sams {
	
	/** Opens a database. */
	public static String getVersion() {
		String version = "2X";   // pending
		return version;
	}
	
	/** Opens a database. */
	public static void init(String operdir) throws Exception {
		SignatureOperationManager.init(operdir);
	}
	
	/** Opens a database. */
	public static ISamsDb open(String dirname) throws Exception {
		return SamsDbFactory.open(dirname);
	}

	/** Creates a database. */
	public static ISamsDb create(String dirname) throws Exception {
		return SamsDbFactory.create(dirname);
	}
	
	/** Reads an external signature file. */
	public static ISpectrumFile readSignatureFile(String filename, String filetype) throws Exception {
		return SpectrumFileManager.openSpectrumFile(filename, filetype);
	}

	/** Gets the list of available signature operations. */
	public static List getOperationNames() {
		return SignatureOperationManager.getOperationNames();
	}
	
	private Sams() {}
}
