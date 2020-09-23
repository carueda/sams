package sigoper;

import sig.Signature;

/**
 * A Signature-&gt;Signature operation, that is,
 * an operation on a single signature resulting in a single signature.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public interface ISingleSignatureOperation extends IOperation {
	/**
	 * Makes an operation on a signature.
	 *
	 * @param sig The signature to be operated.
	 * @return    The resulting signature.
	 * @throws OperationException If the operation cannot be completed.
	 */
	public Signature operate(Signature sig)
	throws OperationException;

}