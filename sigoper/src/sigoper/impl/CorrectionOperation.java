package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Iterator;

/**
 * CorrectionOperation.
Hugh writes:<br>
 * <pre>
... Due to a weakness of one of the 
spectrometers, under certain conditions an error is introduced, apparently 
randomly, into most of the spectra generated within a certain range of 
reflectance intensity.  Specifically, the spectra have a artificial sharp 
jump from the 975nm wavelength response to the 976nm.  The location of the 
jump is consistent among spectra, but the value is not.  I have attached a 
few spectra as examples.  Could you create an algorithm in SAMS which would:

determine the correction factor needed to match the 976 to the 975 value and
apply that correction factor to 976 and all subsequent spectral values

i.e. correction=(value of 976nm - value of 975nm)
      x_new = x_old - correction for all x_old > 975

Because of the location of the error and it's inconsistent nature, several 
of our key analysis techniques are currently useless to us and we can't 
progress on our projects without a "fix". Particularly I can't get good 
results from the NDWI analysis, and Shawn can't do a continuum removal 
around the 980nm water absorption dip.

If you wanted to make this a more generally applicable feature, you could 
allow the future user to define the location of the break point and wether 
they wanted the correction applied to the forward half or the back half of 
the spectra.

 * </pre>
 *
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class CorrectionOperation implements ISingleSignatureOperation {
	static final int BREAKX = 0;
	static final int ADDITIVE = 1;
	static final int FORWARD = 2;
	
	static String[] par_names =        { "breakx", "additive", "forward",};
	static String[] par_descriptions = { "Break point", "Additive correction?", "Apply to the forward?", };
	static Object[] par_values =       { "975", Boolean.FALSE, Boolean.TRUE, };
	static boolean additive = false;
	static boolean forward = true;
	static ParInfo parInfo = new ParInfo(par_names, par_descriptions, par_values) {
		public void setValue(int i, Object value) {
			if ( i == FORWARD )
				forward = ((Boolean) value).booleanValue();
			else if ( i == ADDITIVE )
				additive = ((Boolean) value).booleanValue();
			super.setValue(i, value);
		}
	};
	
	public IOperation.IParameterInfo getParameterInfo() {
		return parInfo;
	}
	
	public String getName() {
		return "Jump correction";
	}

	public String getDescription() {
		return "Jump correction";
	}

	/**
	 * Jump correction.
	 *
	 * @param sig  The signature to be operated.
	 * @return     The resulting signature.
	 */
	public Signature operate(Signature sig)
	throws OperationException {
	/*
		Algorithm description

		Let o, p, q, and r be the four consecutive points of interest, that is,
		with the break point lying between p and q. From points o and p we get
		the first tangent t1, and from points q and r, the tangent t2.
		The average tangent t = (t1 + t2)/2 is used to estimate the value
		at point q according to the following equation:
		
		          qqy - p.y
			t =  -----------
			      q.x - p.x
				  
		Solving for qqy (the estimated value) we have:
		
                   (t1 + t2)(q.x - p.x)
            qqy =  --------------------  + p.y
                            2
				  
		So the correction will be:
			correction = q.y - qqy;
	*/
		double breakx;
		try {
			breakx = Double.parseDouble(((String) par_values[BREAKX]).trim());
		}
		catch(Exception ex) {
			throw new OperationException("Invalid parameters: " +ex.getMessage());
		}
System.out.println("breakx=" +breakx);
System.out.println("forward=" +forward);
System.out.println("additive=" +additive);

		int size = sig.getSize();
		int indexx = OpUtil.indexAt(sig, breakx);

		if ( indexx - 1 < 0 || indexx + 1 >= size )
			throw new OperationException("Can't get enough points");
		
		Signature.Datapoint o, p, q, r;
		
		o = sig.getDatapoint(indexx - 1);
		p = sig.getDatapoint(indexx);
		q = sig.getDatapoint(indexx + 1);
		r = sig.getDatapoint(indexx + 2);
		
		double t1 = (p.y - o.y) / (p.x - o.x);
		double t2 = (r.y - q.y) / (r.x - q.x);
		
		double qqy = (t1 + t2) * (q.x - p.x) / 2  +  p.y;
		
		double correction;
		if ( additive )
			correction = q.y - qqy;
		else {
			if ( OpUtil.equalValues(q.y, 0.0) )
				throw new OperationException("Correction would be infinite. (" +q.y+ ")");
			
			correction = qqy / q.y;
		}
		
		Signature new_sig = new Signature();

		int fromindex, toindex;
		
		if ( forward ) {
			fromindex = indexx + 1;
			toindex = size - 1;
		}
		else {
			fromindex = 0;
			toindex = indexx;
			correction = -correction;
		}
		
		for ( int i = 0; i < size; i++ ) {
			p = sig.getDatapoint(i);
			double y = p.y;
			if ( fromindex <= i && i <= toindex ) {
				if ( additive )
					y -= correction;
				else
					y *= correction;
			}
			
			new_sig.addDatapoint(p.x, y);
		}

		return new_sig;
	}
}
