package sigoper;

/**
 * The base interface for operations.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public interface IOperation{
	/**
	 * Returns the name of this operation.
	 *
	 * @return  The name of this operation.
	 */
	public String getName();

	/**
	 * Returns a short description of this operation.
	 *
	 * @return  A short description of this operation.
	 */
	public String getDescription();
	
	/**
	 * Returns info about parameters associated to this operation. 
	 * null if this operation doesn't require any parameters.
	 *
	 * @return  An object to describe the parameters of this operation, or null
	 *          if not applicable.
	 */
	public IParameterInfo getParameterInfo();

	
	/**
	 * Parameter info for an operation.
	 * @version 05/30/02
	 */
	public interface IParameterInfo 
	{
		/**
		 * Returns the number of parameters associated to an operation.
		 * If this number is <code>N</code>, then the parameters will be identified
		 * from 0 to <code>N-1</code>.
		 *
		 * @return  The number of parameters.
		 */
		public int getNumParameters();
	
		/**
		 * Returns the name of a parameter.
		 *
		 * @param par The parameter id.
		 *
		 * @return  The name.
		 */
		public String getName(int par);

		/**
		 * Returns a short description for a parameter.
		 *
		 * @param par The parameter id.
		 *
		 * @return  A short description.
		 */
		public String getDescription(int par);

		/**
		 * Returns the value for a parameter.
		 *
		 * @param par The parameter id.
		 *
		 * @return  the value of this parameter.
		 */
		public Object getValue(int par);
		
		/**
		 * Sets a value for a parameter.
		 *
		 * @param par The parameter id.
		 * @param value Value for the parameter.
		 */
		public void setValue(int par, Object value);
		
	}
}