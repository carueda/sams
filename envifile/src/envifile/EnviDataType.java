package envifile;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;

/**
 * Some of the Envi data types.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public abstract class EnviDataType {
	public static final EnviDataType BYTE    = new BYTE();
	public static final EnviDataType INT16   = new INT16();
	public static final EnviDataType UINT16  = new UINT16();
	public static final EnviDataType INT32   = new INT32();
	public static final EnviDataType UINT32  = new UINT32();
	public static final EnviDataType FLOAT32 = new FLOAT32();
	public static final EnviDataType FLOAT64 = new FLOAT64();
	
	/** The list of the available types in this class */
	public static final List list;
	
	static {
		list = new ArrayList();
		list.add(BYTE);
		list.add(INT16);
		list.add(UINT16);
		list.add(INT32);
		list.add(UINT32);
		list.add(FLOAT32);
		list.add(FLOAT64);
	}
	
	/** returns the type corresponding to a given code, or
	  * null if the code is not supported.
	  */
	public static EnviDataType get(int code) {
		for ( Iterator it = list.iterator(); it.hasNext(); ) {
			EnviDataType type = (EnviDataType) it.next();
			if ( type.code() == code )
				return type;
		}
		return null;
	}
	
	protected int byte_order = 1;
	
	/** returns the numeric Envi code of this type */
	public abstract int code();
	
	/** returns the size in bytes of this type */
	public abstract int size();
	
	/** writes a value to an output stream
	  * @param val The value in double format. Except for
	  *            the FLOAT64 type, conversion will be
	  *            done as follows: 
	  * <pre>
	  *              [type] newval = ([type]) Math.round(val);
	  *              dos.write[type](newval;
	  * </pre>
	  * @param dos The output to write the (possibly converted) value.
	  */
	public abstract void write(double val, DataOutputStream dos)
	throws IOException;
	
	/** reads a value from an input stream.
	  * @param dis The input stream.
	  */
	public abstract double read(DataInputStream dis)
	throws IOException;
	
	/** returns a mnemonic name of this type */
	public abstract String toString();
	
	/** sets the byte order.
	  * @param byte_order 0=little endian; 1=big endian
	  */
	public void setByteOrder(int byte_order) {
		this.byte_order = byte_order;
	}
	
	protected short _readInt16(DataInputStream dis)
	throws IOException {
		if ( byte_order == 1 ) {
			return dis.readShort();
		}
		else {
			int c0 = dis.readUnsignedByte();
			int c1 = dis.readUnsignedByte();
			return (short) ( (c1 << 8) | c0 );
		}
	}
	
	protected int _readUInt16(DataInputStream dis)
	throws IOException {
		if ( byte_order == 1 ) {
			return dis.readUnsignedShort();
		}
		else {
			int c0 = dis.readUnsignedByte();
			int c1 = dis.readUnsignedByte();
			return (c1 << 8) | c0 ;
		}
	}
	
	protected int _readInt32(DataInputStream dis)
	throws IOException {
		if ( byte_order == 1 ) {
			return dis.readInt();
		}
		else {
			int c0 = dis.readUnsignedByte();
			int c1 = dis.readUnsignedByte();
			int c2 = dis.readUnsignedByte();
			int c3 = dis.readUnsignedByte();
			return (c3 << 24) | (c2 << 16) | (c1 << 8) | c0 ;
		}
	}
	
	protected long _readUInt32(DataInputStream dis)
	throws IOException {
		return 0xFFFFFFFFL & _readInt32(dis);
	}
	
	protected long _readInt64(DataInputStream dis)
	throws IOException {
		if ( byte_order == 1 ) {
			return dis.readLong();
		}
		else {
			long c0 = _readUInt32(dis);
			long c1 = _readUInt32(dis);
			return (c1 << 32) |  c0 ;
		}
	}
	
	protected float readFloat(DataInputStream dis)
	throws IOException {
		if ( byte_order == 1 ) {
			return dis.readFloat();
		}
		else {
			return Float.intBitsToFloat(_readInt32(dis));
		}
	}

	protected double readDouble(DataInputStream dis)
	throws IOException {
		if ( byte_order == 1 ) {
			return dis.readDouble();
		}
		else {
			return Double.longBitsToDouble(_readInt64(dis));
		}
	}
	
	///////////////// the types ///////////////////////////

	private static class BYTE extends EnviDataType {
		public String toString() { return "Byte"; }
		public int code() { return 1; }
		public int size() { return 1; }
		
		public void write(double val, DataOutputStream dos)
		throws IOException {
			int v = (byte) Math.round(val);
			dos.writeByte(v);
		}
		
		public double read(DataInputStream dis)
		throws IOException {
			return (double) dis.readUnsignedByte();
		}
	}
	private static class INT16 extends EnviDataType {
		public String toString() { return "Int16"; }
		public int code() { return 2; }
		public int size() { return 2; }

		public void write(double val, DataOutputStream dos)
		throws IOException {
			int v = (short) Math.round(val);
			dos.writeShort(v);
		}
		
		public double read(DataInputStream dis)
		throws IOException {
			return (double) _readInt16(dis);
		}
	}
	private static class INT32 extends EnviDataType {
		public String toString() { return "Int32"; }
		public int code() { return 3; }
		public int size() { return 4; }

		public void write(double val, DataOutputStream dos)
		throws IOException {
			int v = (int) Math.round(val);
			dos.writeInt(v);
		}
		
		public double read(DataInputStream dis)
		throws IOException {
			return (double) _readInt32(dis);
		}
	}
	private static class FLOAT32 extends EnviDataType {
		public String toString() { return "Float32"; }
		public int code() { return 4; }
		public int size() { return 4; }

		public void write(double val, DataOutputStream dos)
		throws IOException {
			float v = (float) val;
			dos.writeFloat(v);
		}
		
		public double read(DataInputStream dis)
		throws IOException {
			return (double) readFloat(dis);
		}
	}
	private static class FLOAT64 extends EnviDataType {
		public String toString() { return "Float64"; }
		public int code() { return 5; }
		public int size() { return 8; }

		public void write(double val, DataOutputStream dos)
		throws IOException {
			dos.writeDouble(val);
		}
		
		public double read(DataInputStream dis)
		throws IOException {
			return (double) readDouble(dis);
		}
	}
	private static class UINT16 extends EnviDataType {
		public String toString() { return "UInt16"; }
		public int code() { return 12; }
		public int size() { return 2; }

		public void write(double val, DataOutputStream dos)
		throws IOException {
			int v = (short) Math.round(val);
			dos.writeShort(v);
		}
		
		public double read(DataInputStream dis)
		throws IOException {
			return (double) _readUInt16(dis);
		}
	}
	private static class UINT32 extends EnviDataType {
		public String toString() { return "UInt32"; }
		public int code() { return 13; }
		public int size() { return 4; }

		public void write(double val, DataOutputStream dos)
		throws IOException {
			int v = (int) Math.round(val);
			dos.writeInt(v);
		}
		
		public double read(DataInputStream dis)
		throws IOException {
			return (double) _readUInt32(dis);
		}
	}
	
	/** a test program. See createbinaryfile.cc */
	public static void main(String[] args) throws Exception {
		String filename = "binary.data";
		System.out.println("reading " +filename);
		DataInputStream dis = new DataInputStream(
			new java.io.FileInputStream(filename)
		);
		
		// first, read the byte order in the file:
		int byte_order = dis.readByte();
		System.out.println("byte order = " +byte_order);

		int data_type;
		while ( dis.available() > 0 && (data_type = dis.readByte()) > 0 ) {
			EnviDataType type = EnviDataType.get(data_type);
			type.setByteOrder(byte_order);
			System.out.println(type.read(dis)+ " " +type);
		}
		dis.close();
	}
}

