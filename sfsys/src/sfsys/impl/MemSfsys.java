package sfsys.impl;

import sfsys.*;

import java.util.*;
import java.io.*;

/**
 * An implementation of a filesystem that keeps all information
 * in memory.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class MemSfsys implements ISfsys {
	final IDirectory root;

	private MemSfsys() {
		root = new NDirectory("");
	}

    private MemSfsys(IDirectory root) {
		this.root = root;
    }

	public String getInfo() {
		return "MemSfsys";
	}
	
	public IDirectory getRoot() {
		return root;
	}

	public static ISfsys createSfsys() {
		return new MemSfsys();
	}

    public static ISfsys createSfsys(String filename) throws Exception {
		ISfsys fs = createSfsys();
		Reader r = new InputStreamReader(new FileInputStream(filename));
		DecodeConstructor.fillDirectory(fs.getRoot(), r);
		r.close();
		return fs;
    }

	private String encode() {
		IVisitor v = new EncodeVisitor();
		StringBuffer sb = new StringBuffer();
		for ( Iterator iter = root.getChildren().iterator(); iter.hasNext(); )
			((INode) iter.next()).accept(v, sb);
		return sb.toString();
	}

    public void save(String filename) throws java.io.IOException {
		String enc = encode();
		PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
		pw.println(enc);
		pw.close();
    }

	abstract class Node implements INode {
		String name;
		IDirectory parent;
		
		Node(String name){
			this.name = name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
	
		public String getName() {
			return name;
		}
	
		public void setParent(IDirectory parent) {
			this.parent = parent;
		}
	
		public IDirectory getParent() {
			return parent;
		}
		
		public String getPath() {
			if ( parent == null )
				return name;
			else
				return parent.getPath() + name;
		}
	}	
	
	class NDirectory extends Node implements IDirectory {
		private List children;
		
		NDirectory(String name) {
			super(name);
			children = new ArrayList();
		}
	
		public Object accept(IVisitor v, Object obj) {
			return v.visit(this, obj);
		}
		
		private INode add(INode node) {
			children.add(node);
			((Node) node).setParent(this);
			return node;
		}

		public IDirectory createDirectory(String name) {
			return (IDirectory) add(new NDirectory(name));
		}

		public IFile createFile(String name) {
			return (IFile) add(new NFile(name));
		}
		
		public ILink createLink(String name, String path) {
			return (ILink) add(new NLink(name, path));
		}
	
		public List getChildren() {
			return children;
		}

		public String toString() {
			return getName() + "/";
		}
		
		public String getPath() {
			return super.getPath()+ "/";
		}
		
		public INode getNode(String name) {
			for ( Iterator iter = children.iterator(); iter.hasNext(); ) {
				INode node = (INode) iter.next();
				if ( name.equals(node.getName()) )
					return node;
			}
			return null;
		}
		
		public INode findNode(String path) {
			IDirectory from = this;
			if ( path.startsWith("/") )
				from = getRoot();
			
			path = path.replaceAll("^/+", "");
			path = path.replaceAll("/+$", "") + "/";
			String[] apath = path.split("/+");
			IDirectory dir = from;
			INode node = dir;
			for ( int i = 0; i < apath.length; i++ ) {
				node = dir.getNode(apath[i]);
				if ( node == null )
					return null;
				
				if ( i < apath.length - 1 ) {
					if ( !(node instanceof IDirectory) )
						return null;
					dir = (IDirectory) node;
				}
			}
			return node;
		}
			
	}	
	
	class NFile extends Node implements IFile {
		Object obj;
		
		NFile(String name) {
			super(name);
		}
		
		public Object accept(IVisitor v, Object obj) {
			return v.visit(this, obj);
		}
		
		public void setObject(Object obj) {
			this.obj = obj;
		}
	
		public Object getObject() {
			return obj;
		}
		
		public String toString() {
			return getName();
		}
	}

	class NLink extends Node implements ILink {
		String path;
		
		NLink(String name, String path) {
			super(name);
			this.path = path;
		}
		
		public Object accept(IVisitor v, Object obj) {
			return v.visit(this, obj);
		}
		
		public String getRefPath() {
			return path;
		}
		
		public void setRefPath(String path) {
			this.path = path;
		}
		
		public String toString() {
			return getName() + " -> " + getRefPath();
		}
	}	


	static class EncodeVisitor implements IVisitor {
		public Object visit(IFile n, Object obj) {
			StringBuffer sb = (StringBuffer) obj;
			sb.append("\'" +n.getName()+ "'F");
			return obj;
		}
		
		public Object visit(ILink n, Object obj) {
			StringBuffer sb = (StringBuffer) obj;
			sb.append("\'" +n.getName()+ "'L\'" +n.getRefPath()+ "'");
			return obj;
		}
		
		public Object visit(IDirectory n, Object obj) {
			StringBuffer sb = (StringBuffer) obj;
			sb.append("\'" +n.getName()+ "'{");
			for ( Iterator iter = n.getChildren().iterator(); iter.hasNext(); )
				((INode) iter.next()).accept(this, obj);
			sb.append("}");
			return obj;
		}
	}


	static class DecodeConstructor {
		static char lookahead;
		static Reader r;
		static int col;

		public static void fillDirectory(IDirectory dir, Reader rr)
		throws SyntaxException, Exception {
			r = rr;
			col = 1;
			lookahead = next();
			fill(dir);
		}

		static void fill(IDirectory dir)
		throws SyntaxException, Exception {
			if ( lookahead == '\'' )
				while ( lookahead != '}' && lookahead != 0 )
					node(dir);
		}

		static void node(IDirectory parent)
		throws SyntaxException, Exception {
			String name = name();
			if ( lookahead == '{' ) { 
				accept('{');
				fill(parent.createDirectory(name));
				accept('}');
			}
			else if ( lookahead == 'F' ) {
				accept('F');
				parent.createFile(name);
			}
			else if ( lookahead == 'L' ) {
				accept('L');
				String ref = name();
				parent.createLink(name, ref);
			}
			else
				throw error("Expecting one of { , F, L");
		}

		static String name() throws SyntaxException, Exception {
			accept('\'');
			StringBuffer sb = new StringBuffer();
			while ( lookahead != '\'' && lookahead > 0 ) {
				sb.append(lookahead);
				lookahead = next();
			}
			accept('\'');
			return sb.toString();
		}
		
		static void accept(char ch) throws SyntaxException, Exception {
			if ( lookahead != ch )
				throw error("Expecting " +ch+ " instead of " +lookahead);
			lookahead = next();
		}

	    static char next() throws Exception {
			int n = r.read();
			if ( n > ' ' ) {
				col++;
				return (char) n;
			}
			return 0;
	    }
		
	    static Exception error(String m) {
			Exception exc = new SyntaxException(col+ ": " +m);
			exc.printStackTrace();
			return exc;
		}
		
		static class SyntaxException extends Exception {
			SyntaxException(String m) {
				super(m);
			}
		}
	}
}


