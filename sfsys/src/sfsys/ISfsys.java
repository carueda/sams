package sfsys;

/**
 * A simple filesystem.
 * File separator is always '/'; that means any '\\' and ':' is
 * interpreted (and possibly stored) as '/'.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public interface ISfsys {
	public String getInfo();
	public IDirectory getRoot();
	
	/** Optional operation */
    public void save(String filename) throws java.io.IOException;

	/** Interface for traversing the filesystem. */
	public interface IVisitor {
		public Object visit(IFile n, Object obj);
		public Object visit(ILink n, Object obj);
		public Object visit(IDirectory n, Object obj);
	}

	/** Defines each node in the filesystem. */
	public interface INode {
		public String getName();
		public IDirectory getParent();
		public String getPath();
		public Object accept(IVisitor v, Object obj);
	}

	/** Defines a directory in the filesystem. */
	public interface IDirectory extends INode {
		public IDirectory createDirectory(String name);
		public IFile createFile(String name);

		public INode getNode(String name);
		
		/** Finds a node given its path. */
		public INode findNode(String path);
		
		public java.util.List getChildren();
		
		/** Optional operation */
		public ILink createLink(String name, String path);
	}
	
	/** Defines a file in the filesystem. */
	public interface IFile extends INode {
		public void setObject(Object obj);
		public Object getObject();
	}
	
	/** Defines a symbolic link in the filesystem. */
	public interface ILink extends INode {
		public String getRefPath();
		public void setRefPath(String path);
	}
}

