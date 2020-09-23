package samsgui;

import java.io.File;

/**
 * Main program.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class Main {
	static void usage(String msg){
		if ( msg != null ) {
			System.err.println(msg+ ".  Try `-help'");
			System.exit(1);
		}
		else {
			System.out.println(
				"USAGE:\n"+
				"   sams <directory>         SAMS work directory\n" +
				"   sams -verbose            Show some setup messages\n" +
				"   sams -help               This help message\n"
			);
			System.exit(0);
		}
	}
	
	public static void main(String[] args) throws Exception {
		String samsDirectory = null;
		boolean verbose = false;
		for (int arg = 0; arg < args.length; arg++ ) {
			if ( args[arg].equals("-help") ) {
				usage(null);
			}
			else if ( args[arg].equals("-verbose") ) {
				verbose = true;
			}
			else if ( args[arg].startsWith("-") ) {
				usage(args[arg]+ ": unknown option");
			}
			else if (samsDirectory == null) {
				samsDirectory = args[arg];
			}
			else {
				usage(args[arg]+ ": unexpected argument");
			}
		}
		if (samsDirectory == null) {
			usage("error: missing required argument indicating SAMS work directory");
			return;
		}
		if (!new File(samsDirectory).isDirectory()) {
			usage("error: '" + samsDirectory + "' is not a directory. " +
					"Please indicate an existing directory to be used by SAMS");
			return;
		}

		SamsGui.init(samsDirectory, verbose);
	}
}
