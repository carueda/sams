package specfile.impl;

import specfile.*;
import sig.Signature;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Represents a spectrum file in ASD binary format.
 * Currently it only reads: reflectance data type, and float data format.
 * Files are assumed to be in little-endian format. 
 * @author Carlos Rueda
 * @version $Id$
 */
public class ASDBinaryFile implements ISpectrumFile {
	private static final int FLOAT_FORMAT = 0;
	private static final int INTEGER_FORMAT = 1;
	private static final int DOUBLE_FORMAT = 2;
	
	/**
	 * Helper to read a 4-byte float number assuming that the less
	 * significant byte comes first.
	 */
	static float readFloatLittleEndian(RandomAccessFile file) throws IOException {
		int a = file.readUnsignedByte();
		int b = file.readUnsignedByte();
		int c = file.readUnsignedByte();
		int d = file.readUnsignedByte();
		int bits = (d << 24) | (c << 16) | (b << 8) | a ;
		return Float.intBitsToFloat(bits);
	}
 
	/**
	 * Helper to read a 2-byte short number assuming that the less 
	 * significant byte comes first.
	 */
	static int readUnsignedShortLittleEndian(RandomAccessFile file) throws IOException {
		int a = file.readUnsignedByte();
		int b = file.readUnsignedByte();
		return (int) (b << 8) | a ;
	}


	/**
	 * Binary ASD header info.
	 */
	public static class Header {
		public String company_name;
		public int data_type;
		public float ch1_wavel;
		public float wavel_step;
		public int data_format;
		public int channels;
		public int instrument;

		/**
		 * Creates a binary ASD header by reading from the given file.
		 */
		public Header(RandomAccessFile file) throws IOException {
			byte[] buffer = new byte[16];
			file.read(buffer, 0, 3);
			company_name = new String(buffer, 0, 3);
			file.seek(186);
			data_type = file.readUnsignedByte();
			file.seek(191);
			ch1_wavel = readFloatLittleEndian(file);
			wavel_step = readFloatLittleEndian(file);
			data_format = file.readUnsignedByte();
			file.seek(204);
			channels = readUnsignedShortLittleEndian(file);
			file.seek(431);
			instrument = file.readUnsignedByte();
		}
		
		public String toString() {
			return
				"company name = " +company_name+ "\n"+
				"data_type    = " +data_type+ "\n"+
				"ch1_wavel    = " +ch1_wavel+ "\n"+
				"wavel_step   = " +wavel_step+ "\n"+
				"data_format  = " +data_format+ "\n"+
				"channels     = " +channels+ "\n"+
				"instrument   = " +instrument+ "\n"
			;
		}
	}
	
	Header header;
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
			header = new Header(file);
			if ( !header.company_name.equals("ASD") ) {
				throw new InvalidSpectrumFormatException(
					new File(filename).getName()+
					": is not an ASD binary file"
				);
			}
			//System.out.println(header);		
			if ( header.data_type != 1 ) { // REF_TYPE -- reflectance
				throw new InvalidSpectrumFormatException(
					new File(filename).getName()+
					": data_type is not reflectance"
				);
			}
			
			file.seek(484);
	
			double wl = header.ch1_wavel;
			switch ( header.data_format ) {
				case FLOAT_FORMAT: 
					for ( int i = 0; i < header.channels; i++ ) {
						sig.addDatapoint(wl, readFloatLittleEndian(file));
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
