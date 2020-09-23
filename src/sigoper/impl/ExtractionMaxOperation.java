package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.*;

/**
 * Extracts a specific point from various signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class ExtractionMaxOperation implements IMultiSignatureOperation
{
	static final int FROM = 0;
	static final int TO = 1;
	static String[] par_names =        { "from", "to", };
	static String[] par_descriptions = { "From", "To", };
	static Object[] par_values =       { "",	"", };
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);
	
	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Extract max";
	}

	public String getDescription()
	{
		return "Extracts the maximum value from various signatures in a given range";
	}

	/**
	 * Extracts the maximum value from various signatures in a given range
	 * The length of the resulting signature is equal to the
	 * number of signatures given.
	 *
	 * <p>
	 * Each resulting datapoint is given the correponding signature
	 * client object (that could be null).
	 *
	 * <p>
	 * If no sigs are given, it returns an empty signature.
	 *
	 * @param sigs The signatures to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature[] sigs)
	throws OperationException
	{
		double from, to;
		try
		{
			from = Double.parseDouble(((String) par_values[FROM]).trim());
			to   = Double.parseDouble(((String) par_values[TO]).trim());
			if ( from > to )
			{
				throw new Exception("Invalid range");
			}
		}
		catch(Exception ex)
		{
			throw new OperationException("Invalid parameters: " +ex.getMessage());
		}
		
		Signature sig = new Signature();
		if ( sigs == null || sigs.length == 0 )
		{
			return sig;
		}
		
		for ( int i = 0; i < sigs.length; i++ )
		{
			int max_index = OpUtil.findMaxIndex(sigs[i], 0, from, to);
			Signature.Datapoint dp = sigs[i].getDatapoint(max_index);
			double y = dp.y;
			sig.addDatapoint(i + 1, y, sigs[i].getUserObject());
		}
			
		return sig;
	}
}

