package sigoper;

import sig.Signature;

/**
 * A SignatureXSignature-&gt;Signature operation.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public interface IBinarySignatureOperation extends IOperation
{
	/**
	 * Performs the operation.
	 *
	 * @param sig0 One signature.
	 * @param sig1 The other signature.
	 * @return     The resulting signature.
	 * @throws     OperationException If the operation cannot be completed.
	 */
	public Signature operate(Signature sig0, Signature sig1)
	throws OperationException;

}
