package specfile.impl;

import specfile.*;
import sig.Signature;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Represents a spectrum file in ASD format.
 * @author Carlos Rueda
 * @version $Id$
 */
public class ASDFile implements ISpectrumFile {
	Signature sig;

	/**
	 * Creates a ASDFile object.
	 * The associated signature is sorted.
	 *
	 * @param filename ASD file name.
	 */
	public ASDFile(String filename)
	throws FileNotFoundException, InvalidSpectrumFormatException, IOException {
		sig = new Signature();
		BufferedReader br = new BufferedReader(
			new InputStreamReader(
				new FileInputStream(filename)
			)
		);

		// check that is a ASD file:
		// PENDING:  How to recognize an ASD file from its first lines?

		String line;
		while ( (line = br.readLine()) != null ) {
			try {
				StringTokenizer st = new StringTokenizer(line, " ,\t");
				double x = Double.parseDouble(st.nextToken());
				double y = Double.parseDouble(st.nextToken());
				sig.addDatapoint(x, y);
			}
			catch ( Exception ex ) {
				// ignore any parse error.
			}
		}
		br.close();

		// subjetive: if the number of points is not large enough,
		// then this is not an ASD file:
		if ( sig.getSize() < 3 ) {
			throw new InvalidSpectrumFormatException(
				new File(filename).getName()+
				": is not an ASD file"
			);
		}
		sig.sort();
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
		return "ASD";
	}
}