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
	IDirectory root;
	NodeMan nodeMan;
	
	private DirSfsys(String dirname) throws Exception {
		basedir = new File(dirname).getCanonicalFile();
		if ( !basedir.isDirectory() )
			throw new Exception("Not a directory");
		
		nodeMan = new NodeMan();
		root = nodeMan.getDirectory("/");
	}

	public String getInfo() {
		return "DirSfsys: Base directory: " +basedir.getPath();
	}
	
	public IDirectory getRoot() {
		return root;
	}

    public static ISfsys createSfsys(String dirname) throws Exception {
		return new DirSfsys(dirname);
    }

    public void save(String filename) throws java.io.IOException {
		throw new UnsupportedOperationException();
	}
	
	/** Main node manager. */
	class NodeMan {
		Map dirs = new HashMap();
		Map files = new HashMap();
		
		IDirectory getDirectory(String path) {
			path = path.replaceAll("/+", "/");
			NDirectory dir = (NDirectory) dirs.get(path);
			if ( dir == null ) {
				dir = new NDirectory(path);
				dirs.put(path, dir);
			}
			return dir;
		}

		IFile getFile(String path) {
			path = path.replaceAll("/+", "/");
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
					String subpath = getPath()+ "/" +a[i].getName();
					if ( a[i].isDirectory() ) {
						IDirectory dir = getDirectory(subpath);
						children.add(dir);
					}
					else if ( a[i].isFile() ) {
						IFile file = getFile(subpath);
						children.add(file);
					}
				}
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
}


