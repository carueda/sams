package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * Savitzky-Golay smoothing.
 *
 * Current limitations:
 * <ul>
 *	<li> Only some hard-coded filters are available. See SavitzkyGolayFilter.
 *	<li> Only one filter can be applied.
 * </ul>
 *
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class SavitzkyGolayOperation implements ISingleSignatureOperation
{
	static final int FILTER = 0;
	static final int FROM = 1;
	static final int TO = 2;
	
	static String[] par_names =        { "filter-type", "from", "to", };
	static String[] par_descriptions = { "Filter type", "From", "To", };
	static String[] filternames = new String[SavitzkyGolayFilter.filters.length];
	static
	{
		for ( int i = 0; i < filternames.length; i++ )
		{
			filternames[i] = SavitzkyGolayFilter.filters[i].toString();
		}
	}
	static Object[] par_values =       { filternames, "0", "9999", };
	static String filtername = null;
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values)
	{
		public void setValue(int i, Object value)
		{
			if ( i == FILTER )
				filtername = (String) value;
			else
				super.setValue(i, value);
		}
	};
	
	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Smooth";
	}

	public String getDescription()
	{
		return "Savitzky-Golay filtering";
	}

	/**
	 * Savitzky-Golay smoothing of a signature.
	 *
	 * @param sig  The signature to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature sig)
	throws OperationException
	{
		SavitzkyGolayFilter filter = null;
		for ( int i = 0; i < filternames.length; i++ )
		{
			if ( filternames[i].equals(filtername) )
			{
				filter = SavitzkyGolayFilter.filters[i];
				break;
			}
		}
		if ( filter == null )
			throw new RuntimeException("Shouldn't happen!");
		
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

		return filter.operate(sig, from, to);
	}
}
