package specfile.impl;

import specfile.*;
import sig.Signature;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Represents a spectrum file in CSV format.
 * @author Carlos Rueda
 * @version $Id$
 */
public class CSVFile implements ISpectrumFile {
	Signature sig;

	/**
	 * Creates a CSVFile object.
	 * The associated signature is NOT sorted.
	 *
	 * @param filename CSV file name.
	 */
	public CSVFile(String filename)
	throws FileNotFoundException, InvalidSpectrumFormatException, IOException {
		sig = new Signature();
		BufferedReader br = new BufferedReader(
			new InputStreamReader(
				new FileInputStream(filename)
			)
		);

		// check that is a CSV file:

		String line;
		while ( (line = br.readLine()) != null ) {
			try {
				StringTokenizer st = new StringTokenizer(line, ",");
				double x = Double.parseDouble(st.nextToken().trim());
				double y = Double.parseDouble(st.nextToken().trim());
				sig.addDatapoint(x, y);
			}
			catch ( Exception ex ) {
				// ignore any parse error.
			}
		}
		br.close();

		// at least one point
		if ( sig.getSize() == 0 ) {
			throw new InvalidSpectrumFormatException(
				new File(filename).getName()+
				": not a valid spectrum in CSV format"
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
		return "CSV";
	}
}
