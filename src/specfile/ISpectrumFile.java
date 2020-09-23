package specfile;

import sig.Signature;

/**
 * Represents a spectrum file.
 * A spectrum file has an specific file format 
 * and an associated signature.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public interface ISpectrumFile {
	/**
	 * Gets the signature.
	 *
	 * @return The signature.
	 */
	public Signature getSignature();

	/**
	 * Gets the format name.
	 *
	 * @return The format name.
	 */
	public String getFormatName();

}