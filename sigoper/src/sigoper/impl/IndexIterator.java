package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.*;

/**
 * Offers an iterator on valid values for a specific index from a number
 * of signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class IndexIterator
{
	Signature[] sigs; 
	int index;
	double missing_value;
	int kk;
	double next;
	
	/**
	 * Creates an interator.
	 */
	public IndexIterator(Signature[] sigs, int index, double missing_value)
	{
		this.sigs = sigs;
		this.missing_value = missing_value;
		reset(index);
	}
	
	/**
	 * Resets the index for this iterator.
	 */
	public IndexIterator reset(int index)
	{
		this.index = index;
		this.kk = 0;
		prepare_next();
		return this;
	}

	public boolean hasNext()
	{
		return !Double.isNaN(next);
	}
	
	public double next()
	{
		double ret_next = next;
		prepare_next();
		return ret_next;
	}
	
	private void prepare_next()
	{
		next = Double.NaN;
		for ( ; kk < sigs.length; kk++ )
		{
			Signature.Datapoint	dp = sigs[kk].getDatapoint(index);
			if ( Double.isNaN(dp.y) 
			||   !Double.isNaN(missing_value) && Math.abs(missing_value - dp.y) < OpUtil.EPS )
			{
				continue;
			}
			next = dp.y;   // and see below for kk
			break;
		}
		
		kk++;  // in case next took a valid value
	}

}
