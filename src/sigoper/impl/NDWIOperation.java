package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.*;

/**
 * Computes the NDWI from various signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class NDWIOperation implements IMultiSignatureOperation
{
	public IOperation.IParameterInfo getParameterInfo()
	{
		return null;
	}
	
	public String getName()
	{
		return "NDWI";
	}

	public String getDescription()
	{
		return "Computes the NDWI";
	}

	/**
	 * Computes the NDWI from various signatures.
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
		Signature sig = new Signature();
		if ( sigs == null || sigs.length == 0 )
		{
			return sig;
		}
		
		for ( int i = 0; i < sigs.length; i++ )
		{
			double y = ndwi(sigs[i]);
			sig.addDatapoint(i + 1, y, sigs[i].getUserObject());
		}
			
		return sig;
	}

	/**
	 * Computes the NDWI of a signature.
	 *
	 * @param sig The signature to be operated.
	 * @return     The resulting value.
	 */
	static double ndwi(Signature sig)
	throws OperationException
	{
		int size = sig.getSize();
		double at860nm = OpUtil.valueAt(sig, 860.);
		double at1240nm = OpUtil.valueAt(sig, 1240.);
		double den = (at860nm + at1240nm);
		return den != 0 ? ((at860nm - at1240nm) / den) : 0.0;
	}
}

