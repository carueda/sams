package sig;

import java.util.*;

/** 
 * A signature is a collection of datapoints.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public final class Signature {
	private List datapoints;
	private Object obj;

	/** Creates an empty signature.*/
	public Signature(Object obj) {
		this.obj = obj;
		datapoints = new ArrayList();
	}

	/** Creates an empty signature.*/
	public Signature() {
		this(null);
	}
	
	/** Gets the client object of this signature. */
	public Object getUserObject() {
		return obj;
	}

	/** Sets the client object of this signature. */
	public void setUserObject(Object obj) {
		this.obj = obj;
	}

	/** Adds a datapoint.*/
	public Datapoint addDatapoint(double x, double y, Object obj) {
		Datapoint p = new Datapoint(x, y, obj);
		datapoints.add(p);
		return p;
	}
	/** Adds a datapoint.*/
	public Datapoint addDatapoint(double x, double y) {
		return addDatapoint(x, y, null);
	}

	/** Gets the number of datapoints in this signature. */
	public int getSize() {
		return datapoints.size();
	}

	/** Gets a datapoint in this signature. */
	public Datapoint getDatapoint(int index) {
		return (Datapoint) datapoints.get(index);
	}

	/** A point in a signature.*/
	public static final class Datapoint implements Comparable {
		/** The abscissa value. */
		public double x;
	
		/** The ordinate value.*/
		public double y;
	
		/** A client object. */
		public Object obj;

		/** Creates a point. */
		public Datapoint(double x, double y, Object obj) {
			this.x = x;
			this.y = y;
			this.obj = obj;
		}
		
		/** Creates a point. */
		public Datapoint(double x, double y) {
			this(x, y, null);
		}
		
		public int compareTo(Object o) {
			Datapoint other = (Datapoint) o;
			return new Double(this.x).compareTo(new Double(other.x));
		}
	}

	/** Sorts points such that abscissas are in ascending order.
	 */
	public void sort() {
		Collections.sort(datapoints);
	}

	/** Gets a complete clone of this signature. */
	public Object clone() {
		Signature sig = new Signature();
		for (int i = 0; i < getSize(); i++) {
			Datapoint dp = getDatapoint(i);
			sig.addDatapoint(dp.x, dp.y);
		}
		return sig;
	}
}
