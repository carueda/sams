package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.*;

/**
 * Computes the MCT of various signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class MCTOperation implements ISingleSignatureOperation
{
	static final int MAX_FROM = 0;
	static final int MAX_TO = 1;
	static final int MIN_FROM = 2;
	static final int MIN_TO = 3;

	static String[] par_names = { 
		"max-from", 
		"max-to", 
		"min-from", 
		"min-to", 
	};
	static String[] par_descriptions = { 
		"Find maximum from", 
		"Find maximum to",
		"Find minimum from", 
		"Find minimum to",
	};
	static Object[] par_values = { 
		"540",
		"565",
		"660",
		"685",
	};
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);
	

	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "MCT";
	}

	public String getDescription()
	{
		return "Multiple Continuum Transformation";
	}

	/**
	 * Computes the MCT of a signature.
	 *
	 * @param sig  The signature to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature sig)
	throws OperationException
	{
		double max_from, max_to;
		double min_from, min_to;
		
		try
		{
			max_from = Double.parseDouble((String) par_values[MAX_FROM]);
			max_to = Double.parseDouble((String) par_values[MAX_TO]);
			min_from = Double.parseDouble((String) par_values[MIN_FROM]);
			min_to = Double.parseDouble((String) par_values[MIN_TO]);
		}
		catch(Exception ex)
		{
			throw new OperationException("Invalid parameter: " +ex.getMessage());
		}
		
		return mct(sig, max_from, max_to, min_from, min_to);
	}

	/**
	 * Computes the MCT of a signature.
	 *
	 * @param sig The signature to be operated.
	 * @return     The resulting value.
	 */
	static Signature mct(
		Signature sig,
		double max_from, double max_to,
		double min_from, double min_to
	)
	throws OperationException
	{
		int size = sig.getSize();

		// find interested indexes:

		int from_index = OpUtil.findMaxIndex(sig, 0, max_from, max_to);
		int to_index   = OpUtil.findMinIndex(sig, from_index, min_from, min_to);
		
		Signature.Datapoint p = sig.getDatapoint(from_index);
		Signature.Datapoint q = sig.getDatapoint(to_index);

		Signature new_sig = new Signature();
		new_sig.addDatapoint(p.x, 0.0);
		for ( int i = from_index + 1; i < to_index ; i++ )
		{
			Signature.Datapoint r = sig.getDatapoint(i);
			double y = OpUtil.interpolate(p.x, p.y, q.x, q.y, r.x);
			
			double val = r.y - y;
			new_sig.addDatapoint(r.x, val);
		}
		new_sig.addDatapoint(q.x, 0.0);

		return new_sig;
	}
	
}
