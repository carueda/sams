package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.*;

/**
 * Extracts a specific point from various signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class SimpleExtractionOperation implements IMultiSignatureOperation
{
	static final int DESIRED_POINT = 0;
	static String[] par_names = { 
		"desired_point", 
	};
	static String[] par_descriptions = { 
		"Desired wavelength", 
	};
	static Object[] par_values = { 
		"",
	};
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);
	
	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Simple extraction";
	}

	public String getDescription()
	{
		return "Extracts a specific point from various signatures";
	}

	/**
	 * Extracts a specific point from various signatures.
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
		double desired_point;
		
		try
		{
			desired_point = Double.parseDouble((String) par_values[DESIRED_POINT]);
		}
		catch(Exception ex)
		{
			throw new OperationException("Invalid parameter: " +ex.getMessage());
		}
		
		Signature sig = new Signature();
		if ( sigs == null || sigs.length == 0 )
		{
			return sig;
		}
		
		for ( int i = 0; i < sigs.length; i++ )
		{
			double y = OpUtil.valueAt(sigs[i], desired_point);
			sig.addDatapoint(i + 1, y, sigs[i].getUserObject());
		}
			
		return sig;
	}
}

