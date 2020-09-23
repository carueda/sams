package sigoper;

/**
 * Thrown when two or more signatures are not defined on the same
 * abscissas.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class DomainException extends OperationException {
	/**
	 * Creates a DomainException.
	 *
	 * @param msg The message for this exception.
	 */
	public DomainException(String msg) {
		super(msg);
	}
}
