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
	IDirectory root;
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
	
	public IDirectory getRoot() {
		return root;
	}

	public INode getNode(String path) {
		return root.findNode(path);
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
		
		IDirectory getDirectory(String path) {
			path = normalizePath(path);
			NDirectory dir = (NDirectory) dirs.get(path);
			if ( dir == null ) {
				dir = new NDirectory(path);
				dirs.put(path, dir);
			}
			return dir;
		}

		IFile getFile(String path) {
			path = normalizePath(path);
			NFile file = (NFile) files.get(path);
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
			
			public String getName() {
				return path.substring(path.lastIndexOf('/') + 1);
			}
		
			public String toString() {
				return getName();
			}
		
			public IDirectory getParent() {
				String parent_path = path.substring(0, path.lastIndexOf('/'));
				if ( parent_path.length() == 0 )
					return null;
				
				return getDirectory(parent_path);
			}
			
			public String getPath() {
				return path;
			}
			
			File getAbsoluteFile() {
				return new File(basedir, getPath());
			}
		}	
		
		class NDirectory extends Node implements IDirectory {
			NDirectory(String path) {
				super(path);
			}
		
			public IDirectory createDirectory(String dirname) {
				String path = getPath()+ "/" +dirname;
				File subdir = new File(basedir, path);
				if ( subdir.mkdir() )
					return getDirectory(path);
				return null;
			}
			
			public IFile createFile(String filename) {
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
			
			public ILink createLink(String name, String path) {
				throw new UnsupportedOperationException();
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
						IDirectory dir = getDirectory(subpath);
						children.add(dir);
					}
					else if ( f.isFile() ) {
						if ( fileExt == null || f.getName().endsWith(fileExt) ) {
							if ( hideFileExt )
								subpath = subpath.substring(0, subpath.length() - fileExt.length());
							IFile file = getFile(subpath);
							children.add(file);
						}
					}
				}
				Collections.sort(children, new Comparator() {
					public int compare(Object o1, Object o2){
						INode n1 = (INode) o1;
						INode n2 = (INode) o2;
						if ( n1 instanceof IFile ^ n2 instanceof IFile )
							return n1 instanceof IFile ? 1 : -1;
						else
							return n1.getName().compareTo(n2.getName());
					}
				});
				return children;
			}
	
			public String toString() {
				return getName() + "/";
			}
			
			
			public INode getNode(String name) {
				for ( Iterator iter = getChildren().iterator(); iter.hasNext(); ) {
					INode node = (INode) iter.next();
					if ( name.equals(node.getName()) )
						return node;
				}
				return null;
			}
	
			public INode findNode(String path) {
				path = normalizePath(path);
				IDirectory from = this;
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
				
			public Object accept(IVisitor v, Object obj) {
				return v.visit(this, obj);
			}
		}	
		
		class NFile extends Node implements IFile {
			Object obj;
			
			NFile(String path) {
				super(path);
			}
			
			public void setObject(Object obj) {
				this.obj = obj;
			}
		
			public Object getObject() {
				return obj;
			}
			
			public Object accept(IVisitor v, Object obj) {
				return v.visit(this, obj);
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


