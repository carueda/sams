package sfsys;

import java.io.*;

/**
 * Demostration program.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class Main
{
	static ISfsys mount(String filename) throws Exception {
		if ( filename == null )
			return Sfsys.createMem();
		else
			return Sfsys.createDir(filename);
	}
	
	public static void main(String[] args) throws Exception {
		ISfsys fs;
		
		if ( args.length > 0 && args[0].equals("demo") ) {
			fs = mount(null);
			fs.getRoot().createDirectory("dir1");
			fs.getRoot().createFile("aFile");
			fs.getRoot().createDirectory("dir2");
		}
		else {
			fs = args.length == 0 ? mount(null) : mount(args[0]);
		}
		
		new Shell(fs.getRoot(), new InputStreamReader(System.in), new PrintWriter(System.out)) {
			public boolean process(String[] toks) throws Exception {
				if ( super.process(toks) )
					return true;

				if ( toks[0].equals("mount") ) {
					if ( toks.length == 1 ) {
						pw.println("mount: expected argument");
						return true;
					}
					setDirectory(mount(toks[1]).getRoot());
					info();
					return true;
				}
				else if ( toks[0].equals("newfs") ) {
					setDirectory(mount(null).getRoot());
					info();
					return true;
				}
				return false;
			}
			protected void handleException(Exception ex) {
				pw.println("Exception: " +ex.getMessage());
				ex.printStackTrace();
			}
		}.getRunnable().run();
	}
}
