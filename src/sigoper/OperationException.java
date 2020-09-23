package sigoper;

import sig.Signature;

/**
 * Thrown when a problem arises during the computation of an operation.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class OperationException extends Exception {
	/**
	 * Creates an OperationException.
	 *
	 * @param msg The message for this exception.
	 */
	public OperationException(String msg) {
		super(msg);
	}
}
