package envifile;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;
import java.io.DataOutputStream;

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
	
	/** returns a mnemonic name of this type */
	public abstract String toString();
	
	
	private static class BYTE extends EnviDataType {
		public String toString() { return "Byte"; }
		public int code() { return 1; }
		public int size() { return 1; }
		
		public void write(double val, DataOutputStream dos)
		throws IOException {
			int v = (byte) Math.round(val);
			dos.writeByte(v);
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
	}
	private static class FLOAT64 extends EnviDataType {
		public String toString() { return "Float64"; }
		public int code() { return 5; }
		public int size() { return 8; }

		public void write(double val, DataOutputStream dos)
		throws IOException {
			dos.writeDouble(val);
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
	}
}

