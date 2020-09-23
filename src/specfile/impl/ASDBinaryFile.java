package specfile.impl;

import specfile.*;
import sig.Signature;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Represents a spectrum file in ASD binary format.
 * Currently it only reads float data format.
 * Files are assumed to be in little-endian format. 
 * @author Carlos Rueda
 * @version $Id$
 */
public class ASDBinaryFile implements ISpectrumFile {
	private static final int FLOAT_FORMAT = 0;
	private static final int INTEGER_FORMAT = 1;
	private static final int DOUBLE_FORMAT = 2;
	

	
	ASDBinaryFileHeader header;
	Signature sig;

	/**
	 * Creates a ASDBinaryFile object.
	 * The associated signature is sorted.
	 *
	 * @param filename ASD file name.
	 */
	public ASDBinaryFile(String filename)
	throws FileNotFoundException, InvalidSpectrumFormatException, IOException
	{
		sig = new Signature();
		RandomAccessFile file = new RandomAccessFile(filename, "r");
		try {
			header = new ASDBinaryFileHeader(file);
			if ( !header.company_name.equals("ASD") ) {
				throw new InvalidSpectrumFormatException(
					new File(filename).getName()+
					": is not an ASD binary file"
				);
			}
			//System.out.println(header);		
			//if ( header.data_type != 1 ) { // REF_TYPE -- reflectance
			//	throw new InvalidSpectrumFormatException(
			//		new File(filename).getName()+
			//		": data_type is not reflectance"
			//	);
			//}
			
			file.seek(484);
	
			double wl = header.ch1_wavel;
			switch ( header.data_format ) {
				case FLOAT_FORMAT: 
					for ( int i = 0; i < header.channels; i++ ) {
						sig.addDatapoint(wl, ASDBinaryFileHeader.readFloatLittleEndian(file));
						wl += header.wavel_step;
					}
					break;
				case INTEGER_FORMAT:
				case DOUBLE_FORMAT: 
					throw new InvalidSpectrumFormatException(
						new File(filename).getName()+
						": INTEGER and DOUBLE data formats not implemented yet. Only FLOAT"
					);
				default:
					throw new InvalidSpectrumFormatException(
						new File(filename).getName()+
						": " +header.data_format+ " : Unknown data format"
					);
			}
		}
		finally {
			file.close();
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
		return "ASDb";
	}
	
	public String toString() {
		return header.toString();
	}
}

