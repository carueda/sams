package sigoper;

import sig.Signature;

/**
 * A Signature*-&gt;Signature operation, that is,
 * an operation on multiple signatures resulting in a single signature.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public interface IMultiSignatureOperation extends IOperation {
	/**
	 * Makes an operation on multiple signatures.
	 *
	 * Each resulting datapoint may be given the corresponding signature
	 * client object (if any), as a means to "identify" each
	 * resulting datapoint from the user's point of view.
	 *
	 * @param sigs The signatures to be operated.
	 * @return     The resulting signature.
	 * @throws OperationException If the operation cannot be completed.
	 */
	public Signature operate(Signature[] sigs)
	throws OperationException;
}