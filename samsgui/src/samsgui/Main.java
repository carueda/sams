package samsgui;

/** 
 * Main program.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Main {
	static void usage(String msg){
		if ( msg != null ) {
			System.err.println(msg+ " Try `samsgui -help'");
			System.exit(1);
		}
		else {
			System.out.println(
				"USAGE:\n"+
				"      samsgui -help\n"+
				"      samsgui [-opers dir]\n"
			);
			System.exit(0);
		}
	}
	
	public static void main(String[] args) throws Exception {
		String operdirname = null; 
		int arg = 0;
		for (; arg < args.length && args[arg].startsWith("-"); arg++ ) {
			if ( args[arg].equals("-opers") )
				operdirname = args[++arg]; 
			else if ( args[arg].equals("-help") )
				usage(null); 
			else
				usage(args[arg]+ ": unknown option.");
		}
		SamsGui.init(operdirname);
	}
}
