package envifile;

/**
 * Thrown when a file being read doesn't follow an expected format. 
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public class InvalidEnviFormatException extends Exception {
	/**
	 * Creates an InvalidEnviFormatException.
	 *
	 * @param msg A message for the exception.
	 */
	public InvalidEnviFormatException(String msg) {
		super(msg);
	}
}
