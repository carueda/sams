package sigoper.impl;

import sigoper.*;
import sig.Signature;

import java.util.Collection;
import java.util.Iterator;

/**
 * A basic parameter info implementation based on arrays.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class ParInfo implements IOperation.IParameterInfo
{
	String[] par_names; 
	String[] par_descriptions; 
	Object[] par_values; 

	public ParInfo(
		String[] par_names, 
		String[] par_descriptions, 
		Object[] par_values
	)
	{
		this.par_names = par_names;
		this.par_descriptions = par_descriptions;
		this.par_values = par_values;
	}
	
	public int getNumParameters()
	{
		return par_names.length;
	}
	public String getName(int i)
	{
		return par_names[i];
	}
	public String getDescription(int i)
	{
		return par_descriptions[i];
	}
	public Object getValue(int i)
	{
		return par_values[i];
	}
	public void setValue(int i, Object value)
	{
		par_values[i] = value;
	}
}

