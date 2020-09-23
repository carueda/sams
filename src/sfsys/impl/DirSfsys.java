package sfsys.impl;

import sfsys.*;

import java.util.*;
import java.io.*;

/**
 * An implementation of a filesystem that reflects the structure
 * of an underlying physical directory.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class DirSfsys implements ISfsys {
	File basedir;
	String fileExt;
	boolean hideFileExt;
	INode root;
	NodeMan nodeMan;
	
	private DirSfsys(String dirname, String fileExt, boolean hideFileExt) throws Exception {
		basedir = new File(dirname).getCanonicalFile();
		if ( !basedir.isDirectory() )
			throw new Exception("Not a directory");
		
		this.fileExt = fileExt;
		this.hideFileExt = hideFileExt;
		nodeMan = new NodeMan();
		root = nodeMan.getDirectory("/");
	}

	public String getInfo() {
		return "DirSfsys: Base directory: " +basedir.getPath();
	}
	
	public INode getRoot() {
		return root;
	}

    public static ISfsys createSfsys(String dirname) throws Exception {
		return new DirSfsys(dirname, null, false);
    }

    public static ISfsys createSfsys(String dirname, String fileExt, boolean hideFileExt) throws Exception {
		return new DirSfsys(dirname, fileExt, hideFileExt);
    }

    public void save(String filename) throws java.io.IOException {
		throw new UnsupportedOperationException();
	}
	
	/** Main node manager. */
	class NodeMan {
		Map dirs = new HashMap();
		Map files = new HashMap();
		
		INode getDirectory(String path) {
			path = normalizePath(path);
			INode dir = (INode) dirs.get(path);
			if ( dir == null ) {
				dir = new NDirectory(path);
				dirs.put(path, dir);
			}
			return dir;
		}

		INode getFile(String path) {
			path = normalizePath(path);
			INode file = (INode) files.get(path);
			if ( file == null ) {
				file = new NFile(path);
				files.put(path, file);
			}
			return file;
		}

		abstract class Node implements INode {
			private String path;
			
			Node(String path) {
				if ( path == null || !path.startsWith("/") )
					throw new IllegalArgumentException();
				this.path = path;
			}
			
			public ISfsys getSfsys() {
				return DirSfsys.this;
			}
			
			public Object accept(IVisitor v, Object obj) {
				return v.visit(this, obj);
			}
			
			public String getName() {
				return path.substring(path.lastIndexOf('/') + 1);
			}
		
			public String toString() {
				return getName();
			}
		
			public INode getParent() {
				String parent_path = path.substring(0, path.lastIndexOf('/'));
				if ( parent_path.length() == 0 )
					return null;
				
				return getDirectory(parent_path);
			}
			
			public String getPath() {
				return path;
			}
			
			public boolean isDirectory() {
				return false;
			}
			
			public boolean isFile() {
				return false;
			}
			
			public boolean isLink() {
				return false;
			}
			
			File getAbsoluteFile() {
				return new File(basedir, getPath());
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
			NDirectory(String path) {
				super(path);
			}
		
			public boolean isDirectory() {
				return true;
			}
			
			public INode createDirectory(String dirname) {
				String path = getPath()+ "/" +dirname;
				File subdir = new File(basedir, path);
				if ( subdir.mkdir() )
					return getDirectory(path);
				return null;
			}
			
			public INode createFile(String filename) {
				String path = getPath()+ "/" +filename;
				File subfile = new File(basedir, path);
				try {
					if ( !subfile.exists() )
						new FileOutputStream(subfile).close();
					return getFile(path);
				}
				catch(Exception ex) {
					return null;
				}
			}
			
			public List getChildren() {
				File absfile = getAbsoluteFile();
				List children = new ArrayList();
				File[] a = absfile.listFiles();
				if ( a == null ) {
					System.err.println("unable to get list for " +absfile);
					return children;
				}
				for ( int i = 0; i < a.length; i++ ) {
					File f = a[i];
					String subpath = getPath()+ "/" +f.getName();
					if ( f.isDirectory() ) {
						INode dir = getDirectory(subpath);
						children.add(dir);
					}
					else if ( f.isFile() ) {
						if ( fileExt == null || f.getName().endsWith(fileExt) ) {
							if ( hideFileExt )
								subpath = subpath.substring(0, subpath.length() - fileExt.length());
							INode file = getFile(subpath);
							children.add(file);
						}
					}
				}
				Collections.sort(children, new Comparator() {
					public int compare(Object o1, Object o2){
						INode n1 = (INode) o1;
						INode n2 = (INode) o2;
						if ( n1.isFile() ^ n2.isFile() )
							return n1.isFile() ? 1 : -1;
						else
							return n1.getName().compareTo(n2.getName());
					}
				});
				return children;
			}
	
			public String toString() {
				return getName() + "/";
			}
			
			
			public INode getChild(String name) {
				for ( Iterator iter = getChildren().iterator(); iter.hasNext(); ) {
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
				
				path = path.replaceAll("^/+", "").replaceAll("/+$", "");
				path = from.getPath()+ "/" +path;
				path = path.replaceAll("/+", "/");
				File file = new File(basedir, path);
				if ( !file.exists() )
					return null;
				
				try {
					file = file.getCanonicalFile();
					if ( file.getPath().length() < basedir.getPath().length() )
						return null;
				}
				catch(IOException ex) {
					return null;   // shouldn't happen
				}
				
				path = "/" +file.getPath().substring(basedir.getPath().length());
					
				if ( file.isDirectory() )
					return getDirectory(path);
				else if ( file.isFile() )
					return getFile(path);
				else
					return null;
			}
		}	
		
		class NFile extends Node {
			Object obj;
			
			NFile(String path) {
				super(path);
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


