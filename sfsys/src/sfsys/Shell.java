package sfsys;

import sfsys.ISfsys.*;

import java.util.*;
import java.io.*;

/**
 * To run different commands on a ISfsys.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class Shell {
	INode curr;
	BufferedReader br;
	PrintWriter pw;
	Run run;
	
	public Shell(INode curr, Reader r, Writer w) {
		setDirectory(curr);
		if ( r instanceof BufferedReader )
			br = (BufferedReader) r;
		else
			br = new BufferedReader(r);
		if ( w instanceof PrintWriter )
			pw = (PrintWriter) w;
		else
			pw = new PrintWriter(w, true);
		
		run = null;
	}

	public void setDirectory(INode curr) {
		this.curr = curr;
	}
	
	public Runnable getRunnable() {
		if ( run == null )
			run = new Run();
		
		return run;
	}
	
	public void info()  { 
		pw.println(curr.getSfsys().getInfo()); 
	}
	
	public void pwd() { 
		pw.println(curr.getPath()); 
	}
	
	public void mkdir(String name) {
		curr.createDirectory(name);
		pw.println(name);
	}
	
	public void mkfile(String name) {
		curr.createFile(name);
		pw.println(name);
	}
	
	public void mklink(String name, String path) {
		INode refnode = curr.findNode(path);
		if ( refnode == null )
			pw.println(path+ ": No such file or directory");
		else {
			try {
				curr.createLink(name, path);
				pw.println(name);
			}
			catch(UnsupportedOperationException ex) {
				pw.println("unsupported operation on this filesystem");
			}
		}
	}
	
	public void save(String filename) throws java.io.IOException {
		try {
			curr.getSfsys().save(filename);
			pw.println("saved");
		}
		catch(UnsupportedOperationException ex) {
			pw.println("unsupported operation on this filesystem");
		}
	}

	public void tree() {
		curr.accept(
			new IVisitor() {
				int indent = 0;
				public Object visit(INode n, Object obj) {
					if ( n.isFile() )
						pw.println(indent() + n.getName());
					else if ( n.isLink() )
						pw.println(indent() + n.getName() + " -> " +n.getRefPath());
					else {
						indent++;
						for ( Iterator iter = n.getChildren().iterator(); iter.hasNext(); )
							((INode) iter.next()).accept(this, obj);
						indent--;
					}
					return obj;
				}
				StringBuffer indent() {
					StringBuffer sb = new StringBuffer();
					for ( int i = 0; i < indent; i++ )
						sb.append("    ");
					return sb;
				}
			}, 
			null
		);
	}
	
	private void ls(INode dir, boolean header) {
		if ( header )
			pw.println(dir.getPath()+ "/");
		for (Iterator iter = dir.getChildren().iterator(); iter.hasNext(); )
			pw.println("    " +iter.next());
	}
	
	public void ls(String[] toks) {
		if ( toks.length == 1 )
			ls(curr, false);
		else
			for ( int i = 1; i < toks.length; i++ ) {
				String path = toks[i];
				INode node = curr.findNode(path);
				if ( node.isDirectory() )
					ls(node, true);
				else if ( node.isFile() )
					pw.println(node.getPath());
				else
					pw.println(path+ ": Not a directory");
			}
	}
	
	public void cd(String path) {
		INode node = curr.findNode(path);
		while ( node.isLink() ) {
		    String ref = node.getRefPath();
		    node = curr.findNode(ref);
		}

		if ( node.isDirectory() )
			curr = node;
		else
			pw.println(path+ ": Not a directory");
	}

	protected void handleException(Exception ex) {
		pw.println("Exception: " +ex.getMessage());
	}

	public boolean process(String[] toks) throws Exception {
		if ( toks[0].equals("cd") )
			cd(toks[1]);
		else if ( toks[0].equals("ls") )
			ls(toks);
		else if ( toks[0].equals("pwd") )
			pwd();
		else if ( toks[0].equals("tree") )
			tree();
		else if ( toks[0].equals("mkdir") )
			mkdir(toks[1]);
		else if ( toks[0].equals("mkfile") )
			mkfile(toks[1]);
		else if ( toks[0].equals("mklink") )
			mklink(toks[1], toks[2]);
		else if ( toks[0].equals("save") )
			save(toks[1]);
		else if ( toks[0].equals("info") )
			info();
		else
			return false;
		
		return true;
	}

	private class Run implements Runnable {
		public void run() {
			String line;
			while ( true ) {
				pw.print("[" +curr.getPath()+ "] $ ");
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
}

