package envifile;

import sig.Signature;

import java.io.*;

/**
 * An EnviFileManager.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public final class EnviFileManager {
	/**
	 * Gets a signature for a specific (line,pixel) from a file.
	 * The resulting signature is sorted.
	 *
	 * @param filename The Envi file name.
	 * @param type     Assumed file type. Current recognized types:
	 *                        "Standard"
	 *                 If null, an automatic recognition is attempted.
	 * @param line Line of the desired location.
	 * @param pixel Column of the desired location.
	 *
	 * @return The signature.
	 *
	 * @throws FileNotFoundException
	 * @throws InvalidEnviFormatException
	 * @throws IOException
	 */
	public static Signature getSignature(
		String filename,
		String type,
		int line,
		int pixel
	) throws FileNotFoundException, InvalidEnviFormatException, IOException {
		EnviStandardFile.Header header = EnviStandardFile.readHeader(filename);
		
		// check that the given (line,pixel) is valid:
		if ( header.x_start <= pixel && pixel < header.x_start + header.samples
		&&   header.y_start <= line  && line  < header.y_start + header.lines ) {
			// OK
		}
		else {
			throw new InvalidEnviFormatException(
				"(" +line+ "," +pixel+ ") invalid"
			);
		}
		
		if ( header.data_type != 4 )
			throw new InvalidEnviFormatException("Expected data type = 4");
		if ( ! header.interleave.equals("bip") )
			throw new InvalidEnviFormatException("Sorry: Only interleave 'bip' is supported now");

		// translate point according to (x_start, y_start):
		line -= header.y_start;
		pixel -= header.x_start;
		
		// open the binary file:			
		DataInputStream dis = new DataInputStream(new FileInputStream(filename));

		try {
			// skip to point (line,pixel):
			int skip = (line * header.samples + pixel) * header.bands * 4;
			dis.skipBytes(skip);
			
			// read the signature;
			Signature sig = new Signature();
			for ( int k = 0; k < header.bands; k++ )
			{
				float data = dis.readFloat();
				sig.addDatapoint(header.wavelengths[k], data);
			}
			
			sig.sort();
			return sig;
		}
		finally {
			dis.close();
		}
	}

	// Non-instanciable
	private EnviFileManager() {}

}