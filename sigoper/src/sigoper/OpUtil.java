package sigoper;

import sig.Signature;

import java.util.*;

/**
 * Operation utilities.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public final class OpUtil {
	/**
	 * Used to compare values for equality. 
	 * 1.0e-5 is chosen rather arbitrarily.
	 */
	public static final double EPS = 1.0e-5;
	
	/**
	 * Determines if two abscissa values x1 and x2 are "equal", that is,
	 * if abs(a-b) &lt;= EPS.
	 *
	 * @return true iff the given values are equal. 
	 */
	public static boolean equalAbscissas(double x1, double x2) {
		return Math.abs(x1 - x2) <= EPS;
	}

	/**
	 * Determines if two values x1 and x2 are "equal", that is,
	 * if abs(a-b) &lt;= EPS.
	 *
	 * @return true iff the given values are equal. 
	 */
	public static boolean equalValues(double x1, double x2) {
		return Math.abs(x1 - x2) <= EPS;
	}

	/**
	 * Checks if two signatures are defined on the same abscissas.
	 * <p>
	 * equalAbscissas is used to check equality.
	 * <p>
	 * The checked indexes will be from <code>0</code> to 
	 * <code>MIN(sig1.getSize(), sig2.getSize(), max_index)</code>.
	 *
	 * @param sig1        Signature
	 * @param sig2        Signature
	 * @param max_index   Max index to check.
	 *
	 * @throws DomainException If the signatures are not defined on the same abscissas. 
	 */
	public static void checkDomain(Signature sig1, Signature sig2, int max_index ) throws DomainException {
		Signature.Datapoint dp1, dp2;
		max_index = Math.min(sig1.getSize(), max_index);
		max_index = Math.min(sig2.getSize(), max_index);

		for ( int i = 0; i < max_index; i++ ) {
			dp1 = sig1.getDatapoint(i);
			dp2 = sig2.getDatapoint(i);
			if ( !equalAbscissas(dp1.x, dp2.x) ) {
				throw new DomainException(
					"Different abscissa values at index " +i+ ":\n"+ 
					"    " +dp1.x+ "  vs.  " +dp2.x+ "\n"
				);
			}
		}
	}
	
	/**
	 * Checks definition on the same abscissas.
	 * <p>
	 * equalAbscissas is used to check equality.
	 * <p>
	 * The checked indexes will be from <code>0</code> to 
	 * <code>MIN(sig1.getSize(), sig2.getSize(), max_index)</code>.
	 *
	 * @param sigs        The signatures
	 * @param max_index   Max index to check in each signature.
	 *
	 * @throws DomainException If the signatures are not defined on the same abscissas. 
	 */
	public static void checkDomains(Signature[] sigs, int max_index) throws DomainException {
		// check against first signature:
		for ( int k = 1; k < sigs.length; k++ ) {
			try {
				OpUtil.checkDomain(sigs[0], sigs[k], max_index);
			}
			catch(DomainException ex) {
				throw ex;
			}
		}
	}
	
	/**
	 * Gets the minimum size from a list of signatures.
	 *
	 * @param sigs        The signatures
	 *
	 * @return the minimum size from a list of signatures.
	 *         Integer.MAX_VALUE if the array is empty.
	 */
	public static int minSize(Signature[] sigs) {
		int size = Integer.MAX_VALUE;
		for ( int k = 0; k < sigs.length; k++ ) {
			int ss = sigs[k].getSize();
			if ( size > ss )
				size = ss;
		}
		return size;
	}

	/**
	 * Gets the maximum size from a list of signatures.
	 *
	 * @param sigs        The signatures
	 *
	 * @return the maximum size from a list of signatures.
	 *         Integer.MIN_VALUE if the array is empty.
	 */
	public static int maxSize(Signature[] sigs) {
		int size = Integer.MIN_VALUE;
		for ( int k = 0; k < sigs.length; k++ ) {
			int ss = sigs[k].getSize();
			if ( size < ss )
				size = ss;
		}
		return size;
	}

	/**
	 * Gets the minimum Y value from a signature.
	 *
	 * @param sig        The signature
	 *
	 * @return the minimum Y value.
	 *         Double.POSITIVE_INFINITY if the signature is empty.
	 */
	public static double minY(Signature sig) {
		double min = Double.POSITIVE_INFINITY;
		int size = sig.getSize();
		for ( int k = 0; k < size; k++ ) {
			Signature.Datapoint dp = sig.getDatapoint(k);
			if ( min > dp.y )
				min = dp.y;
		}
		return min;
	}

	/**
	 * Gets the maximum Y value from a signature.
	 *
	 * @param sig        The signature
	 *
	 * @return the maximum Y value.
	 *         Double.NEGATIVE_INFINITY if the signature is empty.
	 */
	public static double maxY(Signature sig) {
		double max = Double.NEGATIVE_INFINITY;
		int size = sig.getSize();
		for ( int k = 0; k < size; k++ ) {
			Signature.Datapoint dp = sig.getDatapoint(k);
			if ( max < dp.y )
				max = dp.y;
		}
		return max;
	}

	/**
	 * Gets the value of a signature at a given abscissa.
	 *
	 * @return The resulting value. This could be the result of a linear 
	 *         interpolation between the nearest available values.
	 *
	 * @throws OperationException If signature is undefined at the given abscissa.
	 */
	public static double valueAt(Signature sig, double at) throws OperationException {
		int size = sig.getSize();
		Signature.Datapoint p = null;
		Signature.Datapoint q = null;
		
		// search for index of at:
		int index;
		for ( index = 0; index < size; index++ ) {
			p = sig.getDatapoint(index);
			if ( at <= p.x )
				break;
		}
		if ( index == size )
			throw new OperationException("signature undefined at " +at);
		
		if ( equalAbscissas(at, p.x)       // close enough, or ...
		||   index == size - 1  )          // no next to interpolate
			return p.y;
		
		// make interpolation with next point
		q = sig.getDatapoint(index + 1);
		return interpolate(p.x, p.y, q.x, q.y, at);
	}
	
	/**
	 * Gets the first index i such that sig[i].x >= at.
	 *
	 * @return The index.
	 *
	 * @throws OperationException If signature is undefined at the given abscissa.
	 */
	public static int indexAt(Signature sig, double at) throws OperationException {
		int size = sig.getSize();
		Signature.Datapoint p = null;
		
		// search for index of at:
		int index;
		for ( index = 0; index < size; index++ ) {
			p = sig.getDatapoint(index);
			if ( p.x >= at )
				return index;
		}
		throw new OperationException("signature undefined at " +at);
	}
	
	/**
	 * Returns  yp2 - (((yp2-yp1)*(xp2-xc))/(xp2-xp1))
	 */
	public static double interpolate(double xp1, double yp1, double xp2, double yp2, double xc) {
		return yp2 - (((yp2-yp1)*(xp2-xc))/(xp2-xp1));
	}
	
	/**
	 * Finds the "maximum" index in sig in [from,to].
	 */
	public static int findMaxIndex(Signature sig, int from_index, double from, double to)
	throws OperationException {
		int size = sig.getSize();
		double max_val = 0.0;  // just to avoid "uninitialized variable"
		int max_index = -1;
		
		// get first value in [from,to]
		for ( int i = from_index; i < size; i++ ) {
			Signature.Datapoint dp = sig.getDatapoint(i);
			double x = dp.x;
			if ( from <= x ) {
				max_index = i;
				max_val = dp.y;
				break;
			}
		}
		if ( max_index < 0 )
			throw new OperationException("signature undefined on " +from);

		// select maximum
		for ( int i = max_index + 1; i < size; i++ ) {
			Signature.Datapoint dp = sig.getDatapoint(i);
			double x = dp.x;
			if ( x > to)
				break;
			double y = dp.y;
			if ( max_val < y ) {
				max_val = y;
				max_index = i;
			}
		}
		
		return max_index;
	}

	/**
	 * Finds the "minimum" index in sig in [from,to].
	 */
	public static int findMinIndex(Signature sig, int from_index, double from, double to)
	throws OperationException {
		int size = sig.getSize();
		double min_val = 0.0;  // just to avoid "uninitialized variable"
		int min_index = -1;
		
		// get first value in [from,to]
		for ( int i = from_index; i < size; i++ ) {
			Signature.Datapoint dp = sig.getDatapoint(i);
			double x = dp.x;
			if ( from <= x ) {
				min_index = i;
				min_val = dp.y;
				break;
			}
		}
		if ( min_index < 0 )
			throw new OperationException("signature undefined on " +from);

		// select minimum
		for ( int i = min_index + 1; i < size; i++ ) {
			Signature.Datapoint dp = sig.getDatapoint(i);
			double x = dp.x;
			if ( x > to)
				break;
			double y = dp.y;
			if ( min_val > y ) {
				min_val = y;
				min_index = i;
			}
		}
		
		return min_index;
	}

	// Non-instanceable	
	private OpUtil() {}
}
