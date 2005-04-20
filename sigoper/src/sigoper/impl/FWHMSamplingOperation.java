package sigoper.impl;

import sigoper.*;
import sig.Signature;


/**
 * FWHMSamplingOperation.
 *
 * See http://mathworld.wolfram.com/GaussianFunction.html
 *
 * @author C. Rueda, P. Zarco, Y. Cheng
 * @version $Id$ 
 */
public class FWHMSamplingOperation implements IBinarySignatureOperation {
	static double kte = Math.sqrt(8*Math.log(2));
	static ParInfo parInfo = null; // none
	
	public IOperation.IParameterInfo getParameterInfo() {
		return parInfo;
	}
	
	public String getName() {
		return "FWHM based resampling";
	}

	public String getDescription() {
		return 
			"<html>"+
			"A reference-based operation to resample selected signatures.<br>"+
			"The reference signature should contain <b>wavelength,FWHM</b> columns."
		;
	}

	/** Performs the FWHM based resampling operation.
	  *
	  * @param sig - A selected signature.
	  *
	  * @param ref - The FWHM signature.
	  */
	public Signature operate(Signature sig, Signature ref)
	throws OperationException {
		// create new signature:
		Signature new_sig = new Signature();
	
		// get sizes of operands:
		int ref_size = ref.getSize();
		int sig_size = sig.getSize();
		
		for ( int i = 0; i < ref_size; i++ ) {
			Signature.Datapoint rp = ref.getDatapoint(i);
			// wavelength at which we want resample sig:
			double w = rp.x;
			
			double fwhm = rp.y;
			double sigma = fwhm / kte;
			
			// as suggested by P.Z.
			double winsize = 2*fwhm;
			double winsize2 = fwhm;   // == winsize/2
	
			// just to check that at least the signature is defined at w
			// (we don't need the returned index)
			OpUtil.indexAt(sig, w);
			
			// interval of interest:
			double at_inf = w - winsize2;		
			double at_sup = w + winsize2;
	
			// corresponding indices:
			int inf_idx = OpUtil.rightMostIndexAt(sig, at_inf);
			int sup_idx = OpUtil.leftMostIndexAt(sig, at_sup);
	
			// the following adjustments are OK since we already know
			// the signature is at least defined at the center abscissa:
			if ( inf_idx < 0 )
				inf_idx = 0;
			if ( sup_idx >= sig_size )
				sup_idx = sig_size -1;
			
			// make weighting:
			double sum = 0.0;
			double ker_sum = 0.0;
			for ( int j = inf_idx; j <= sup_idx; j++ ) {
				Signature.Datapoint sp = sig.getDatapoint(j);
				
				double spxw2 = (sp.x-w)*(sp.x-w);
				double sigma2 = sigma*sigma;
				
				double weight = Math.exp(-spxw2 / (2*sigma2));
				ker_sum += weight;
				sum += weight * sp.y;
			}
			double y = sum / ker_sum;
			new_sig.addDatapoint(w, y);
		}
	
		return new_sig;
	}
}
