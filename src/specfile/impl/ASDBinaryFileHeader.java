package specfile.impl;

import specfile.*;

import java.io.*;


/**
 * Binary ASD header info.
 * @author Carlos Rueda
 * @version $Id$
 */
public class ASDBinaryFileHeader {
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
	public ASDBinaryFileHeader(RandomAccessFile file) throws IOException {
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
}

