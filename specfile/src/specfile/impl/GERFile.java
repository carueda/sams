package specfile.impl;

import specfile.*;
import sig.Signature;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Represents a spectrum file in GER format.
 *
 * The first line is expected to be one of the following:
 * <p>
 * <pre>///GER SIGNATUR FILE///</pre>
 * <p>
 * <pre>///GER ASCII FILE///</pre>
 * <p>
 *
 * Subsequent lines are parsed for 3 or 4 numerical values.
 * Lines that cannot be parsed to at least 3 values, are simply ignored.
 * The first value is always taken as a wavelength.
 * If a fourth value, say w, is parsed successfully, then w/100 is taken as the reflectance value.
 * Otherwise, the reflectance will be y/z where y and z are the second and third values.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public class GERFile implements ISpectrumFile {
	Signature sig;

	/**
	 * Creates a GERFile object.
	 * The associated signature is sorted.
	 *
	 * @param filename GER file name.
	 */
	public GERFile(String filename)
	throws FileNotFoundException, InvalidSpectrumFormatException, IOException {
		sig = new Signature();
		BufferedReader br = new BufferedReader(
			new InputStreamReader(
				new FileInputStream(filename)
			)
		);

		// check that is "///GER SIGNATUR FILE///"
		String line = br.readLine();
		if ( line == null 
		||      !line.trim().equals("///GER SIGNATUR FILE///")    // DIFFERENCES ????
		     && !line.trim().equals("///GER ASCII FILE///")       // PENDING
		) {
			throw new InvalidSpectrumFormatException(
				new File(filename).getName()+
				": is not a GER file"
			);
		}

		while ( (line = br.readLine()) != null ) {
			try {
				StringTokenizer st = new StringTokenizer(line, " ,\t");
				double x = Double.parseDouble(st.nextToken());
				double y = Double.parseDouble(st.nextToken());
				double z = Double.parseDouble(st.nextToken());
				
				// try a fourth column (2002-05-30)
				try {
					double w = Double.parseDouble(st.nextToken());
					// take this a the reflectance value divided by 100:
					sig.addDatapoint(x, w / 100.0);
					continue;
				}
				catch(Exception ex) {
					// ignore: just work with 2nd and 3rd values...
				}
				
				y = (z != 0.0) ? y/z : 0.0;
				sig.addDatapoint(x, y);
			}
			catch ( Exception ex ) {
				// ignore any parse error.
			}
		}
		br.close();
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
		return "GER";
	}
}