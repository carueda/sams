package specfile;

/**
 * Thrown when a spectra file is being read and its contents doesn't follow 
 * the expected format. 
 * @author Carlos Rueda
 * @version $Id$
 */
public class InvalidSpectrumFormatException extends Exception {
	/**
	 * Creates an InvalidSpectraFormatException.
	 *
	 * @param msg A message for the exception.
	 */
	public InvalidSpectrumFormatException(String msg) {
		super(msg);
	}
	
}