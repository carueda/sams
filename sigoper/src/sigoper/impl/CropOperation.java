package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * Crops a signature.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class CropOperation implements ISingleSignatureOperation
{
	static final int FROM = 0;
	static final int TO = 1;
	static String[] par_names =        { "from", "to", };
	static String[] par_descriptions = { "Remove all before wavelength", "Remove all after wavelength", };
	static Object[] par_values =       { "", "", };
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);
	
	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Crop";
	}

	public String getDescription()
	{
		return "Crops a signature";
	}

	/**
	 * Crops a signature.
	 *
	 * @param sig  The signature to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature sig)
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

		int size = sig.getSize();
		Signature new_sig = new Signature();
		for ( int i = 0; i < size; i++ )
		{
			Signature.Datapoint dp = sig.getDatapoint(i);
			if ( from <= dp.x && dp.x <= to )
			{
				new_sig.addDatapoint(dp.x, dp.y);
			}
		}

		return new_sig;
	}
}
