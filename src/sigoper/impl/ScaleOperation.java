package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * Scales a signature.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class ScaleOperation implements ISingleSignatureOperation
{
	static final int SCALE = 0;
	static String[] par_names =        { "scale", };
	static String[] par_descriptions = { "Scale", };
	static Object[] par_values =       { "1",};
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);
	
	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Scale";
	}

	public String getDescription()
	{
		return "Scales a signature";
	}

	/**
	 * Scales a signature.
	 *
	 * @param sig  The signature to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature sig)
	throws OperationException
	{
		double scale;
		try
		{
			scale = Double.parseDouble(((String) par_values[SCALE]).trim());
		}
		catch(Exception ex)
		{
			throw new OperationException("Invalid value for scale: " +ex.getMessage());
		}

		int size = sig.getSize();
		Signature new_sig = new Signature();
		for ( int i = 0; i < size; i++ )
		{
			Signature.Datapoint dp = sig.getDatapoint(i);
			new_sig.addDatapoint(dp.x, dp.y * scale);
		}

		return new_sig;
	}
}
