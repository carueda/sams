package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * Abscissa unit conversion.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class ChangeAbscissaUnitsOperation implements ISingleSignatureOperation
{
	static final int CONVERSION_FACTOR = 0;
	static String[] par_names =        { "convfactor", };
	static String[] par_descriptions = { "Conversion factor", };
	static Object[] par_values =       { "0.001",};
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);
	
	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Change wavelength unit";
	}

	public String getDescription()
	{
		return "Change the wavelength units according to a conversion factor";
	}

	/**
	 * Performs the operation.
	 *
	 * @param sig  The signature to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature sig)
	throws OperationException
	{
		double convfactor;
		try
		{
			convfactor = Double.parseDouble(((String) par_values[CONVERSION_FACTOR]).trim());
		}
		catch(Exception ex)
		{
			throw new OperationException("Invalid value for conversion factor: " +ex.getMessage());
		}

		int size = sig.getSize();
		Signature new_sig = new Signature();
		for ( int i = 0; i < size; i++ )
		{
			Signature.Datapoint dp = sig.getDatapoint(i);
			new_sig.addDatapoint(dp.x * convfactor, dp.y);
		}

		return new_sig;
	}
}
