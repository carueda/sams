package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * Gets a derivative of a signature.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class DerivativeOperation implements ISingleSignatureOperation
{
	static final int FILTER = 0;
	static final String NO_FILTER = "(Don't smooth)";
	
	static String[] par_names =        { "filter-type", };
	static String[] par_descriptions = { "Smooth first using filter", };
	static String[] filternames = new String[SavitzkyGolayFilter.filters.length + 1];
	static
	{
		for ( int i = 0; i < filternames.length - 1; i++ )
		{
			filternames[i] = SavitzkyGolayFilter.filters[i].toString();
		}
		filternames[filternames.length - 1] = NO_FILTER;
	}
	static Object[] par_values =       { filternames, };
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
		return "Derivative";
	}

	public String getDescription()
	{
		return "Gets the first derivative of a signature";
	}

	/**
	 * Gets a derivative of a signature.
	 *
	 * @param sig  The signature to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature sig)
	throws OperationException
	{
		SavitzkyGolayFilter filter = null;
		if ( ! filtername.equals(NO_FILTER) )
		{
			for ( int i = 0; i < filternames.length; i++ )
			{
				if ( filternames[i].equals(filtername) )
				{
					filter = SavitzkyGolayFilter.filters[i];
					break;
				}
			}
		}
		
		if ( filter != null )
		{
			sig = filter.operate(sig, 0, 9999999);
		}
		
		
		int size = sig.getSize();
		Signature new_sig = new Signature();
		for ( int i = 0; i < size - 1; i++ )
		{
			Signature.Datapoint p = sig.getDatapoint(i);
			Signature.Datapoint q = sig.getDatapoint(i + 1);
			double d = (q.y - p.y) / (q.x - p.x);
			new_sig.addDatapoint(p.x, d);
		}
		Signature.Datapoint p = sig.getDatapoint(size - 1);
		new_sig.addDatapoint(p.x, 0.0);

		return new_sig;
	}
}
