package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.*;

/**
 * AbsortionFeatureAnalysis on various signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class AbsortionFeatureAnalysisOperation implements IMultiSignatureOperation
{
	static final int MAX_1_FROM = 0;
	static final int MAX_1_TO = 1;
	static final int MAX_2_FROM = 2;
	static final int MAX_2_TO = 3;

	static String[] par_names = { 
		"max-1-from", 
		"max-1-to", 
		"max-2-from", 
		"max-2-to", 
	};
	static String[] par_descriptions = { 
		"First maximum from", 
		"First maximum to",
		"Second maximum from", 
		"Second maximum to",
	};
	static Object[] par_values = { 
		"1050",
		"1130",
		"1250",
		"1310",
	};
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values);
	

	public IOperation.IParameterInfo getParameterInfo()
	{
		return parInfo;
	}
	
	public String getName()
	{
		return "Absortion feature analysis";
	}

	public String getDescription()
	{
		return "Absortion feature analysis";
	}

	/**
	 * Computes the absortion feature analysis on various signatures.
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
		double max_1_from, max_1_to;
		double max_2_from, max_2_to;
		
		try
		{
			max_1_from = Double.parseDouble((String) par_values[MAX_1_FROM]);
			max_1_to = Double.parseDouble((String) par_values[MAX_1_TO]);
			max_2_from = Double.parseDouble((String) par_values[MAX_2_FROM]);
			max_2_to = Double.parseDouble((String) par_values[MAX_2_TO]);
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
			double cr = compute(sigs[i], max_1_from, max_1_to, max_2_from, max_2_to);
			sig.addDatapoint(i + 1, cr, sigs[i].getUserObject());
		}

		return sig;
	}

	/**
	 * Makes the computation on a single signature.
	 *
	 * @param sig The signature to be operated.
	 * @return     The resulting value.
	 */
	static double compute(
		Signature sig,
		double max_1_from, double max_1_to,
		double max_2_from, double max_2_to
	)
	throws OperationException
	{
		int size = sig.getSize();

		// find interested indexes:

		int from_index = OpUtil.findMaxIndex(sig, 0, max_1_from, max_1_to);
		int to_index =   OpUtil.findMaxIndex(sig, from_index, max_2_from, max_2_to);
		
//System.out.println(from_index+ " -> " +to_index);
//System.out.println(sig.getDatapoint(from_index).x+ " -> " +sig.getDatapoint(to_index).x);

		// compute area under function
		double area = 0;
		for ( int i = from_index; i < to_index - 1; i++ )
		{
			Signature.Datapoint dp0 = sig.getDatapoint(i);
			Signature.Datapoint dp1 = sig.getDatapoint(i + 1);
			
			double w = dp1.x - dp0.x;
			
			area += w * ( dp0.y + dp1.y ) / 2; 
		}

//System.out.println("area = " +area);		
		// compute total area:
		Signature.Datapoint dp0 = sig.getDatapoint(from_index);
		Signature.Datapoint dp1 = sig.getDatapoint(to_index - 1);
		double w = dp1.x - dp0.x;
		double total_area = w * ( dp0.y + dp1.y ) / 2; 
//System.out.println("total_area = " +total_area);

		// final computation:
		double cr = 1.0 - area / total_area;
//System.out.println("cr = " +cr);
		return cr;
	}
	
}
