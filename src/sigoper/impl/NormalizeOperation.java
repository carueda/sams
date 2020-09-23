package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * Adjustment operation.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class NormalizeOperation implements IBinarySignatureOperation
{
	static final int DESIRED_POINT = 0;
	static String[] par_names = { 
		"desired_point", 
	};
	static String[] par_descriptions = { 
		"Base wavelength", 
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
		return "Normalize";
	}

	public String getDescription()
	{
		return "Normalizes according to the reference signature at a given wavelength";
	}

	/**
	 * Computes scale*sig0, where scale = sig1(x) / sig0(x) and x is the
	 * base wavelength.
	 */
	public Signature operate(Signature sig0, Signature sig1)
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

		double num = OpUtil.valueAt(sig1, desired_point);
		double den = OpUtil.valueAt(sig0, desired_point);
		double scale = den != 0.0 ? num/den : 1.0;

		int size = sig0.getSize();
		Signature new_sig = new Signature();
		for ( int i = 0; i < size; i++ )
		{
			Signature.Datapoint dp = sig0.getDatapoint(i);
			new_sig.addDatapoint(dp.x, dp.y * scale);
		}
		
		return new_sig;
	}
}
