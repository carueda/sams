package specfile.impl;

import specfile.*;
import sig.Signature;

import java.io.*;

/**
 * Represents a spectrum file in "only reflectance" format.
 *
 * This is an ad hoc format in which each line in the file is
 * expected to have a reflectance value. The wavelengths for the
 * values will be 350, 351, 352, ...
 *
 * THIS IS A PRELIMINARY VERSION: Wavelength values should be
 * specified.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public class OnlyReflectanceFile implements ISpectrumFile {
	Signature sig;

	/**
	 * Creates a OnlyReflectanceFile object.
	 * The associated signature is sorted.
	 *
	 * @param filename The file name.
	 */
	public OnlyReflectanceFile(String filename)
	throws FileNotFoundException, InvalidSpectrumFormatException, IOException {
		sig = new Signature();
		BufferedReader br = new BufferedReader(
			new InputStreamReader(
				new FileInputStream(filename)
			)
		);

		// PENDING: fix these magic numbers:

		double first = 350;
		double step = 1;

		String line;
		while ( (line = br.readLine()) != null ) {
			line = line.trim();

			try {
				double y = Double.parseDouble(line);
				sig.addDatapoint(first, y);

				first += step;
			}
			catch ( Exception ex ) {
				// ignore any parse error.
			}
		}
		br.close();

		// subjetive: if the number of points is not large enough,
		// then this is not a valid file:
		if ( sig.getSize() < 3 ) {
			throw new InvalidSpectrumFormatException(
				new File(filename).getName()+
				": is not an \"only reflectance\" file"
			);
		}
	}

	/**
	 * Gets the signature.
	 *
	 * @return The signature.
	 */
	public Signature getSignature() {
		return sig;
	}

	/**
	 * Gets the format name.
	 *
	 * @return The format name.
	 */
	public String getFormatName() {
		return "Only-Reflectance";
	}
}