package samscore;

import samscore.impl.SamsDbFactory;
import sig.Signature;
import sigoper.SignatureOperationManager;
import specfile.*;
import envifile.EnviFileManager;
import envifile.InvalidEnviFormatException;

import java.util.*;
import java.io.*;

/**
 * Point of access to SAMS core.
 * Also offers a number of general utilities.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Sams {
	
	/** Opens a database. */
	public static String getVersion() {
		String version = "3.2";   // pending
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
	
	/**
	 * Gets the signatures contained in an ASCII file.
	 * <ul>
	 *	<li> The file is scanned one line at a time.
	 *	<li> Separators for tokens are: simple spaces, commas, and/or tabs.
	 *	<li> A line is "recognized" if it starts with at least two floating point values;
	 *       otherwise, the line is ignored.
	 *	<li> Recognized lines in the same file can contain different number of columns.
	 *	<li> Let val_0, val_1, ..., val_n be the scanned, consecutive floating point values
	 *       found in the line.
	 *	<li> for 1 &lt;= i &lt;= n, signature (i-1)-th is added the point (val_0, val_i),
	 *       that is, val_0 will be its abscissa and val_i its ordinate.
	 * </ul>
	 *
	 * @param filename  Name of the file to read.
	 * @return The list of signatures in the same order as they appear in the file.
	 */
	public static List getSignaturesFromAsciiFile(String filename) throws Exception {
		String SEPARATORS = " ,\t";
		BufferedReader stream = null;
		int lineno = 0;
		String line;
		List sigs = new ArrayList();
		try {
			stream = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			while ( (line = stream.readLine()) != null ) {
				lineno++;
				StringTokenizer st = new StringTokenizer(line, SEPARATORS);
				int columns = st.countTokens();
				if ( columns <= 1 )
					continue; // just ignore this line
				
				double x;
				try {			
					x = Double.parseDouble(st.nextToken());
				}
				catch (NumberFormatException ex) {
					continue; // just ignore this line
				}
				
				// scan next columns-1 columns:
				for ( int ii = 0; ii < columns - 1; ii++ ) {
					try {			
						double y = Double.parseDouble(st.nextToken());
						
						// successfuly gotten y-value for ii-th signature
						// check if a new signature must be created:
						if ( ii == sigs.size() )
							sigs.add(new Signature());
						
						Signature sig = (Signature) sigs.get(ii);
						sig.addDatapoint(x, y);
					}
					catch (NumberFormatException ex) {
						break; // stop scanning current line
					}
				}
			}
			return sigs;
		}
		catch ( IOException ex ) {
			throw new Exception(
				"At line " +lineno+ ":\n" +
				ex.getClass().getName()+ " : " +ex.getMessage()
			);
		}
		finally{
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}

	/**
	 * Gets a signature for a specific (line,pixel) from an Envi Standard file.
	 *
	 * @param filename The Envi file name.
	 * @param line Line of the desired location.
	 * @param pixel Column of the desired location.
	 * @return The signature.
	 *
	 * @throws FileNotFoundException
	 * @throws InvalidEnviFormatException
	 * @throws IOException
	 */
	public static Signature getSignatureFromEnviFile(String filename, int line, int pixel)
	throws FileNotFoundException, InvalidEnviFormatException, IOException {
		return EnviFileManager.getSignature(filename, line, pixel);
	}
	
	private Sams() {}
}
