package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Collection;
import java.util.Iterator;

/**
 * Computes the standard deviation (sqrt of the sample variance) of various signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class StandardDeviationOperation implements IMultiSignatureOperation
{
	static final int MISSING_VALUE_INDEX = 0;
	static String[] par_names =        { "missing_value", };
	static String[] par_descriptions = { "Missing value indicator", };
	static Object[] par_values =       { "0", };
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);

	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Standard Deviation";
	}

	public String getDescription()
	{
		return "Computes the standard deviation (sqrt of sample variance)";
	}

	/**
	 * Computes the standard deviation (sqrt of the sample variance) of 
	 * various signatures.
	 * The length of the resulting signature is equal to the
	 * shortest signature given.
	 *
	 * <p>Special cases:
	 * <ul>
	 *  <li>If no sigs are given, it returns an empty signature.
	 *  <li>If only one signature is given, this is returned.
	 * </ul>
	 *
	 * <p>PRE: The given signatures are defined at the same points.
	 *
	 * @param sigs The signatures to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature[] sigs)
	throws OperationException
	{
		double missing_value = Double.NaN;
		String mv = ((String) par_values[MISSING_VALUE_INDEX]).trim();
		if ( mv.length() > 0 )
		{
			try
			{
				missing_value = Double.parseDouble(mv);
			}
			catch(Exception ex)
			{
				throw new OperationException("Invalid parameter: " +ex.getMessage());
			}
		}

		Signature sig = new Signature();

		if ( sigs == null || sigs.length == 0 )
		{
			return sig;
		}

		if ( sigs.length == 1 )
		{
			return sigs[0];
		}

		// take the min size and check domains:
		int size = OpUtil.minSize(sigs);
		OpUtil.checkDomains(sigs, size);
		
		IndexIterator ii = new IndexIterator(sigs, 0, missing_value);

		for ( int i = 0; i < size; i++ )
		{
			double sum = 0.0;  
			int count = 0;     // denominator
			
			// read from the first signature just to take the abscissa for i:
			Signature.Datapoint dp = sigs[0].getDatapoint(i);
			double x = dp.x;

			ii.reset(i);
			while ( ii.hasNext() )
			{
				double y = ii.next();				
				sum += y;
				count++;
			}

			if ( count > 0 )
			{
				double avg = sum / count;

				// compute sample variance:
				double var = 0;
				ii.reset(i);
				while ( ii.hasNext() )
				{
					double y = ii.next();				
					double h = y - avg;
					var += h * h;
				}

				double sd = Math.sqrt(var / (sigs.length - 1));
				sig.addDatapoint(x, sd);
			}
			else
			{
				// See AverageOperation for an explanation.
				sig.addDatapoint(x, missing_value);
			}
		}

		return sig;
	}

}
