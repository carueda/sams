package sigoper.impl;

import sigoper.*;
import sig.Signature;


/**
 * Reflectance2RadianceOperation
 * @author C. Rueda, P. Zarco, Y. Cheng
 * @version $Id$ 
 */
public class Reflectance2RadianceOperation implements IBinarySignatureOperation {
	static ParInfo parInfo = null; // none
	
	public IOperation.IParameterInfo getParameterInfo() {
		return parInfo;
	}
	
	public String getName() {
		return "Reflectance to radiance";
	}

	public String getDescription() {
		return "Reflectance to radiance calculation using total irradiance spectra";
	}

	/** Performs the Reflectance to radiance operation.
	  *
	  * @param refl - The reflectance signature
	  *
	  * @param irrad - The irradiance signature.
	  */
	public Signature operate(Signature refl_sig, Signature irrad_sig)
	throws OperationException {
		// create the resulting radiance signature:	
		Signature new_rad = new Signature();
		
		// We will use OpUtil.averagedValueAt; for the required
		// winsize, we take just the distance between two
		// consecutive samples in refl signature:
		double x0 = refl_sig.getDatapoint(0).x;
		double x1 = refl_sig.getDatapoint(1).x;
		double winsize = x1 - x0;
		
		// for each datapoint in reflectance:
		int refl_size = refl_sig.getSize();
		for ( int i = 0; i < refl_size; i++ ) {
			// get the i-th datapoint: 
			Signature.Datapoint refl_point = refl_sig.getDatapoint(i);
	
			// get the irradiance value by averaging in the window
			// centered at the reflectance wavelength:
			double irrad_val = OpUtil.averagedValueAt(irrad_sig, refl_point.x, winsize);
			
			// apply conversion:
			double rad = refl_point.y * irrad_val / Math.PI;
			
			// and add the new point to resulting radiance:
			new_rad.addDatapoint(refl_point.x, rad);
		}
	
		// done. Return the created radiance signature:
		return new_rad;
	}
}
