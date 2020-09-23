package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * Simple ratio operation.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class SimpleRatioOperation implements IBinarySignatureOperation
{
	public IOperation.IParameterInfo getParameterInfo()
	{
		return null;
	}
	
	public String getName()
	{
		return "Simple ratio";
	}

	public String getDescription()
	{
		return "Gets the simple ratio between two signatures";
	}

	/**
	 * Computes <code>sig0 / sig1</code>.
	 */
	public Signature operate(Signature sig0, Signature sig1)
	throws OperationException
	{
		int size0 = sig0.getSize();
		int size1 = sig1.getSize();
		int size = Math.min(size0, size1);
		Signature new_sig = new Signature();
		for ( int i = 0; i < size; i++ )
		{
			Signature.Datapoint dp0 = sig0.getDatapoint(i);
			Signature.Datapoint dp1 = sig1.getDatapoint(i);
			if ( ! OpUtil.equalAbscissas(dp0.x, dp1.x) )
			{
				throw new OperationException("Different abscissas found! At index: " +i+ "\n" +
					"   " +dp0.x+ " vs. " +dp1.x
				);
			}
			double res = dp0.y / dp1.y;
			new_sig.addDatapoint(dp0.x, res);
		}

		return new_sig;
	}
}
