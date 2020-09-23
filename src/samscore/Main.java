package samscore;

import java.io.*;

/**
 * Demostration program.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class Main {
	static SamsDbManager dbman = null;
	static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	static PrintWriter pw = new PrintWriter(System.out, true);
	
	static Interpreter interpreter = null;
	
	
	public static void main(String[] args) throws Exception {
		String operdirname = args.length == 1 ? args[0] : null; 
		Sams.init(operdirname);
		run();
	}

	static String getInfo() {
		return dbman == null ? "no database" : dbman.getDatabase().getInfo();
	}
	
	static boolean process(String[] toks) throws Exception {
		if ( toks[0].equals("open") || toks[0].equals("create") ) {
			ISamsDb db = toks[0].equals("open") ? Sams.open(toks[1]) : Sams.create(toks[1]);
			dbman = new SamsDbManager(db, pw);
			if ( interpreter == null )
				interpreter = new Interpreter(dbman, br, pw);
			else
				dbman.setDatabase(db);
			return true;
		}
		else if ( dbman == null ) {
			pw.println("Only 'open', 'create'  available when no dtabase");
			return true;
		}
		else
			return interpreter.process(toks);
	}
	
	static void handleException(Exception ex) {
		pw.println("Exception: " +ex.getMessage());
		ex.printStackTrace();
	}

	static void run() {
		String line;
		while ( true ) {
			pw.println("[" +getInfo()+ "]");
			pw.print(" $ ");
			pw.flush();
			try {
				if ( (line = br.readLine()) == null )
					break;
				if ( line.trim().length() == 0 )
					continue;
				String[] toks = line.split("\\s+");
				if  ( !process(toks) )
					pw.println(toks[0]+ ": unrecognized command");
			}
			catch (Exception ex) {
				handleException(ex);
			}
		}
	}
}
