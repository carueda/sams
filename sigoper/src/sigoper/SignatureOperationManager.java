package sigoper;

import sigoper.impl.*;
import sig.Signature;

import java.util.*;
import java.io.*;

/**
 * This class gets the operations on signatures.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public final class SignatureOperationManager {
	/** Directory with scripted operations. */
	private static String scripted_dirname;
	
	/** Operation names. */
	private static List operNames;

	/** Mapping oper-code-name -> IOperation */
	private static Map codOpers;
	
	private static List multi_opers;
	private static List single_opers;
	private static List binary_opers;
	
	
	/**
	 * Initializes the signature operation manager.
	 *
	 * Also loads the scripted operations found in a given directory.
	 */
	public static void init(String _scripted_dirname) {
		scripted_dirname = _scripted_dirname;
		
		// Initialization with "compiled" operators:
		multi_opers = new ArrayList();
		multi_opers.add(new AverageOperation());
		multi_opers.add(new StandardDeviationOperation());
		multi_opers.add(new SumOperation());
		multi_opers.add(new MaximumOperation());
		multi_opers.add(new MinimumOperation());
		multi_opers.add(new AbsortionFeatureAnalysisOperation());
		multi_opers.add(new NDWIOperation());
		multi_opers.add(new SimpleExtractionOperation());
		multi_opers.add(new ExtractionMaxOperation());
		
		single_opers = new ArrayList();
		single_opers.add(new ScaleOperation());
		single_opers.add(new CropOperation());
		single_opers.add(new SavitzkyGolayOperation());
		single_opers.add(new DerivativeOperation());
		single_opers.add(new CorrectionOperation());
		single_opers.add(new MCTOperation());
		single_opers.add(new ChangeAbscissaUnitsOperation());
		
		binary_opers = new ArrayList();
		binary_opers.add(new SimpleRatioOperation());
		binary_opers.add(new SubtractionOperation());
		binary_opers.add(new NormalizeOperation());
		binary_opers.add(new FWHMSamplingOperation());
		binary_opers.add(new Reflectance2RadianceOperation());
			
		_loadScriptedOperations();
		_createMap();
	}
	
	private static void _createMap() {
		codOpers = new HashMap();
		operNames = new ArrayList();

		List[] lists = { multi_opers, single_opers, binary_opers };
		for ( int i = 0; i < lists.length; i++ ) {
			if ( i > 0 )
				operNames.add(null);  // separator
			
			List opers = lists[i];
			for ( Iterator it = opers.iterator(); it.hasNext(); ) {
				IOperation oper = (IOperation) it.next();
				String name = oper.getName();
				codOpers.put(name, oper);
				operNames.add(name);
			}
		}
	}

	private static void _loadScriptedOperations() {
		if ( scripted_dirname == null )
			return;
		
		File scripted_dir = new File(scripted_dirname);
		if ( !scripted_dir.isDirectory() )
			return;
		
		File[] list = scripted_dir.listFiles();
		if ( list == null )
			return;
		
		for ( int i = 0; i < list.length; i++ ) {
			File file = list[i];
			String scriptname = file.getName();
			try {
				if ( scriptname.endsWith(".m.bsh") )
					multi_opers.add(new BshMultiOperation(file.getAbsolutePath()));
				else if ( scriptname.endsWith(".s.bsh") )
					single_opers.add(new BshSingleOperation(file.getAbsolutePath()));
				else if ( scriptname.endsWith(".b.bsh") )
					binary_opers.add(new BshBinaryOperation(file.getAbsolutePath()));
			}
			catch(Exception ex) {
				System.out.println(ex.getMessage());
			}
		}
	}
	
	/** Reloads all scripted operations under directory given in
	  * initialization.
	  */
	public static void reloadScriptedOperations() {
		//
		// strategy: recreate the lists; keep the non-scripted 
		// operations; _loadScriptedOperations, and _createMap().
		//
		
		List old_multi_opers = multi_opers;
		List old_single_opers = single_opers;
		List old_binary_opers = binary_opers;
		
		multi_opers = new ArrayList();
		single_opers = new ArrayList();
		binary_opers = new ArrayList();
		
		List[] old_lists = { old_multi_opers, old_single_opers, old_binary_opers };
		List[] new_lists = { multi_opers, single_opers, binary_opers };
		
		for ( int i = 0; i < old_lists.length; i++ ) {
			List old_opers = old_lists[i];
			List new_opers = new_lists[i];
			for ( Iterator it = old_opers.iterator(); it.hasNext(); ) {
				IOperation old_oper = (IOperation) it.next();
				if ( !(old_oper instanceof BshOperation) )
					new_opers.add(old_oper); // keep it
			}
		}
		
		_loadScriptedOperations();
		_createMap();
	}
	
	/**
	 * Gets the set of operation names.
	 *
	 * @return the set of operation names.
	 */
	public static List getOperationNames() {
		return operNames;
	}

	/**
	 * Gets an operation.
	 *
	 * @param name The operation name. See getOperationNames().
	 *
	 * @return The operation object.
	 */
	public static IOperation getSignatureOperation(String name) {
		IOperation a = (IOperation) codOpers.get(name);
		if ( a == null ) {
			System.out.println("Warning: Operation not found: " +name);
		}
		return a;
	}

	// non-instanceable
	private SignatureOperationManager() {}

	
	static abstract class BshOperation implements IOperation {
		String name;
		String description;
		IOperation.IParameterInfo parInfo;
		
		bsh.Interpreter bsh;
		
		BshOperation(String scriptname) throws Exception {
			bsh = new bsh.Interpreter();
			bsh.eval(
				"import sig.Signature;\n"+
				"import sigoper.*;\n"+
				"import sigoper.impl.*;\n"
			);
			// initialization with default values and convenience stuff:
			bsh.set("name", scriptname);
			bsh.set("description", "No description available");
			bsh.set("__parInfo", new HashMap());
			Map __parIndex = new HashMap();
			bsh.set("__parIndex", __parIndex);
			
			bsh.eval(
				"addParameterInfo(name, description, value) {\n"+
				"	entry = new Object[]{description, value};\n"+
				"	__parInfo.put(name, entry);\n"+
				"}\n"+
				"\n"+
				"getDoubleParameter(parname) {\n"+
				"	int index = ((Integer) __parIndex.get(parname)).intValue();\n"+
				"	Double.parseDouble(((String) __par_values[index]).trim());\n"+ 
				"}\n"+
				"\n"+
				"getIntegerParameter(parname) {\n"+
				"	int index = ((Integer) __parIndex.get(parname)).intValue();\n"+
				"	Integer.parseInt(((String) __par_values[index]).trim());\n"+ 
				"}\n"
			);
			
			// now read the script:
			bsh.source(scriptname);
				
			// take definitions:
			name = (String) bsh.eval("this.name");
			description = (String) bsh.eval("this.description");

			parInfo = null;
			Map __parInfo = (Map) bsh.eval("this.__parInfo");
			if ( __parInfo != null && !__parInfo.isEmpty() ) {
				int size = __parInfo.size();
				String[] par_names = new String[size]; 
				String[] par_descriptions = new String[size]; 
				Object[] par_values  = new Object[size];
				int i = 0;
				for ( Iterator it = __parInfo.keySet().iterator(); it.hasNext(); i++ ) {
					String name = (String) it.next();
					Object[] entry = (Object[]) __parInfo.get(name);
					par_names[i] = name;
					par_descriptions[i] = (String) entry[0];
					par_values[i] = (Object) entry[1];
					
					__parIndex.put(name, new Integer(i));
				}
				parInfo = new ParInfo(par_names, par_descriptions, par_values);
				bsh.set("__par_values", par_values);
			}
			
		}
		
		public IOperation.IParameterInfo getParameterInfo() {
			return parInfo;
		}
		
		public String getName() {
			return name;
		}
	
		public String getDescription() {
			return description;
		}
	
	}
	
	static class BshSingleOperation extends BshOperation implements ISingleSignatureOperation {
		BshSingleOperation(String scriptname)
		throws Exception {
			super(scriptname);
		}
		
		public Signature operate(Signature sig) throws OperationException {
			try {
				bsh.set("sig", sig);
				return (Signature) bsh.eval("this.operate(sig)");
			}
			catch(bsh.EvalError ex) {
				ex.printStackTrace();
				throw new OperationException(
					"Error executing scripted operation:\n"+
					"  " +ex.getMessage()
				);
			}
		}
	}

	static class BshMultiOperation extends BshOperation implements IMultiSignatureOperation {
		BshMultiOperation(String scriptname) throws Exception {
			super(scriptname);
		}
		
		public Signature operate(Signature[] sigs) throws OperationException {
			try {
				bsh.set("sigs", sigs);
				return (Signature) bsh.eval("this.operate(sigs)");
			}
			catch(bsh.EvalError ex) {
				ex.printStackTrace();
				throw new OperationException(
					"Error executing scripted operation:\n"+
					"  " +ex.getMessage()
				);
			}
		}
	}

	static class BshBinaryOperation extends BshOperation implements IBinarySignatureOperation {
		BshBinaryOperation(String scriptname) throws Exception {
			super(scriptname);
		}
		
		public Signature operate(Signature sig0, Signature sig1) throws OperationException {
			try {
				bsh.set("sig0", sig0);
				bsh.set("sig1", sig1);
				return (Signature) bsh.eval("this.operate(sig0, sig1)");
			}
			catch(bsh.EvalError ex) {
				ex.printStackTrace();
				throw new OperationException(
					"Error executing scripted operation:\n"+
					"  " +ex.getMessage()
				);
			}
		}
	}
}