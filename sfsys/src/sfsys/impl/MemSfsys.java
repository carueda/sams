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
	final INode root;

	private MemSfsys() {
		root = new NDirectory("");
	}

    private MemSfsys(INode root) {
		this.root = root;
    }

	public String getInfo() {
		return "MemSfsys";
	}
	
	public INode getRoot() {
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
		INode parent;
		
		Node(String name){
			this.name = name;
		}
		
		public ISfsys getSfsys() {
			return MemSfsys.this;
		}
		
		public boolean isFile() {
			return false;
		}
		
		public boolean isDirectory() {
			return false;
		}
		
		public boolean isLink() {
			return false;
		}
		
		public Object accept(IVisitor v, Object obj) {
			return v.visit(this, obj);
		}
		
		public void setName(String name) {
			this.name = name;
		}
	
		public String getName() {
			return name;
		}
	
		public void setParent(INode parent) {
			this.parent = parent;
		}
	
		public INode getParent() {
			return parent;
		}
		
		public String getPath() {
			if ( parent == null )
				return name;
			else
				return parent.getPath() + name;
		}
	
		public INode createDirectory(String dirname) {
			throw new UnsupportedOperationException();
		}
		public INode createFile(String filename) {
			throw new UnsupportedOperationException();
		}
		public INode getChild(String name) {
			throw new UnsupportedOperationException();
		}
		public INode findNode(String path) {
			throw new UnsupportedOperationException();
		}
		public List getChildren() {
			throw new UnsupportedOperationException();
		}
		public INode createLink(String name, String path) {
			throw new UnsupportedOperationException();
		}
		public void setObject(Object obj) {
			throw new UnsupportedOperationException();
		}
		public Object getObject() {
			throw new UnsupportedOperationException();
		}
		public String getRefPath() {
			throw new UnsupportedOperationException();
		}
		public void setRefPath(String path) {
			throw new UnsupportedOperationException();
		}
	}	
	
	class NDirectory extends Node {
		private List children;
		
		NDirectory(String name) {
			super(name);
			children = new ArrayList();
		}
	
		public boolean isDirectory() {
			return true;
		}
		
		private INode add(INode node) {
			children.add(node);
			((Node) node).setParent(this);
			return node;
		}

		public INode createDirectory(String name) {
			return add(new NDirectory(name));
		}

		public INode createFile(String name) {
			return add(new NFile(name));
		}
		
		public INode createLink(String name, String path) {
			path = normalizePath(path);
			return add(new NLink(name, path));
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
		
		public INode getChild(String name) {
			for ( Iterator iter = children.iterator(); iter.hasNext(); ) {
				INode node = (INode) iter.next();
				if ( name.equals(node.getName()) )
					return node;
			}
			return null;
		}
		
		public INode findNode(String path) {
			path = normalizePath(path);
			INode from = this;
			if ( path.startsWith("/") )
				from = getRoot();
			
			path = path.replaceAll("^/+", "");
			path = path.replaceAll("/+$", "") + "/";
			String[] apath = path.split("/+");
			INode dir = from;
			INode node = dir;
			for ( int i = 0; i < apath.length; i++ ) {
				node = dir.getChild(apath[i]);
				if ( node == null )
					return null;
				
				if ( i < apath.length - 1 ) {
					if ( !node.isDirectory() )
						return null;
					dir = node;
				}
			}
			return node;
		}
			
	}	
	
	class NFile extends Node {
		Object obj;
		
		NFile(String name) {
			super(name);
		}
		
		public boolean isFile() {
			return true;
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

	class NLink extends Node {
		String path;
		
		NLink(String name, String path) {
			super(name);
			this.path = path;
		}
		
		public boolean isLink() {
			return true;
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
		public Object visit(INode n, Object obj) {
			StringBuffer sb = (StringBuffer) obj;
			if ( n.isFile() )
				sb.append("\'" +n.getName()+ "'F");
			else if ( n.isLink() )
				sb.append("\'" +n.getName()+ "'L\'" +n.getRefPath()+ "'");
			else if ( n.isDirectory() ) {
				sb.append("\'" +n.getName()+ "'{");
				for ( Iterator iter = n.getChildren().iterator(); iter.hasNext(); )
					((INode) iter.next()).accept(this, obj);
				sb.append("}");
			}
			return obj;
		}
	}


	static class DecodeConstructor {
		static char lookahead;
		static Reader r;
		static int col;

		public static void fillDirectory(INode dir, Reader rr)
		throws SyntaxException, Exception {
			r = rr;
			col = 1;
			lookahead = next();
			fill(dir);
		}

		static void fill(INode dir)
		throws SyntaxException, Exception {
			if ( lookahead == '\'' )
				while ( lookahead != '}' && lookahead != 0 )
					node(dir);
		}

		static void node(INode parent)
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
	
	/** Normalizes a path, that is, replaces "\\" and ":" for "/". */
	static String normalizePath(String path) {
		path = path.replace('\\', '/');
		path = path.replace(':', '/');
		path = path.replaceAll("/+", "/");
		return path;
	}
}


