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
	
	/** Gets the root directory. */
	public INode getRoot();
	
	/** Optional operation */
    public void save(String filename) throws java.io.IOException;

	/** Defines each node in the filesystem. */
	public interface INode {
		public String getName();
		public INode getParent();
		public String getPath();
		public Object accept(IVisitor v, Object obj);
		public boolean isDirectory();
		public boolean isFile();
		public boolean isLink();
		public ISfsys getSfsys();
		
		// when isDirectory:
		public INode createDirectory(String name);
		public INode createFile(String name);
		
		public java.util.List getChildren();
		
		/** gets a child */
		public INode getChild(String name);
		
		/** Finds a node given its path. */
		public INode findNode(String path);
		
		/** Optional operation */
		public INode createLink(String name, String path);

		// when isFile:
		public void setObject(Object obj);
		public Object getObject();

		// when isLink:
		public String getRefPath();
		public void setRefPath(String path);
	}
	
	/** Interface for traversing the filesystem. */
	public interface IVisitor {
		public Object visit(INode n, Object obj);
	}
}

