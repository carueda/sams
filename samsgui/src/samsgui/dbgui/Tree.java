package samsgui.dbgui;

import samscore.ISamsDb;
import sfsys.ISfsys;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.event.*;
import java.awt.Toolkit;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.font.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;

/** 
 * Tree display.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Tree extends JPanel {
	private static final Color bg_group = new Color(222,222,255);
	private static final Color bg_spectrum = new Color(210,255,255);

	protected DbGui dbgui;
	protected final MyNode rootNode = new MyNode().reset("^");
	protected MyNode importedNode;
	protected MyNode computedNode;
    protected DefaultTreeModel treeModel;
    protected JTree jtree;

	protected DefaultTreeCellRenderer tcr;
	protected MyNode focusedNode = null;	
	
	public Tree(DbGui dbgui) {
		super(new BorderLayout());
		this.dbgui = dbgui;
		
		JLabel label = new JLabel("Right-click for options", JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(Font.ITALIC));
		label.setForeground(Color.gray);
		add(label, BorderLayout.NORTH);
		
        treeModel = new DefaultTreeModel(rootNode);
        jtree = new JTree(treeModel);

		// To avoid keyboard navigation when "Alt" is pressed. 
		jtree.setUI(new javax.swing.plaf.basic.BasicTreeUI() {
			protected KeyListener createKeyListener() {
				return new KeyHandler() {
					public void keyTyped(KeyEvent e) {
						if ( (e.getModifiers() & KeyEvent.ALT_MASK) == KeyEvent.ALT_MASK )
							e.consume();
						else
							super.keyTyped(e);
					}
				};
			}
		});

		jtree.setRootVisible(false);
        jtree.setShowsRootHandles(true);
		jtree.putClientProperty("JTree.lineStyle", "Angled");
		
        add(new JScrollPane(jtree));
		
		// cell renderer:
		jtree.setCellRenderer(tcr = new MyRenderer());
	}

	public JTree getJTree() {
		return jtree;
	}
	
	public Icon getLeafIcon(){
		return tcr.getLeafIcon();
	}
	
	public Icon getOpenIcon() {
		return tcr.getOpenIcon();
	}
	
	public void setInfo() {
		rootNode.removeAllChildren();
		ISamsDb db = dbgui.getDatabase();
		if ( db != null ) {
			try {
				createGroupNode(rootNode, db.getGroupingUnderLocation("/"));
			}
			catch(Exception ex) {
			}
		}
		treeModel.reload();
	}		
		
	MyNode findNode(String path) {
		MyNode parent = rootNode;
		String[] parts = path.split("/");
		for ( int i = 0; i < parts.length; i++ ) {
			String part = parts[i];
			if ( part.length() == 0 )
				continue;
			parent = findChildNode(parent, part);
			if ( parent == null )
				return null;
			if ( i == parts.length - 1 )  // found;
				return parent;
		}
		return null;
	}
	
	MyNode findChildNode(MyNode parent, String name) {
		for ( int i = 0; i < parent.getChildCount(); i++ ) {
			MyNode n = (MyNode) parent.getChildAt(i);
			if ( name.equals(n.getName()) )
				return n;
		}
		return null;
	}
	
	/** Returns path to affected parent */
	public String insertNode(String path, boolean isSpectrum) {
		assert !path.endsWith("/");
		
		ISamsDb db = dbgui.getDatabase();
		if ( db == null )
			return null;

		String parent_path = path.substring(0, path.lastIndexOf('/'));
		MyNode parent = findNode(parent_path);
		if ( parent == null )
			return null;

		String name = path.substring(path.lastIndexOf('/') + 1);
		addChild(parent, name, isSpectrum, true);
		return parent_path;
	}
		
	/** Returns path to affected parent */
	public String removeNode(String path) {
		MyNode node = findNode(path);
		if ( node == null ) {
			return null;
		}
		MyNode parent = (MyNode) node.getParent();
		if ( parent == null ) {
			return null;
		}
		treeModel.removeNodeFromParent(node);
		//treeModel.reload();
		jtree.scrollPathToVisible(new TreePath(parent.getPath()));
		return parent.getStringPath();
	}
	
	public MyNode getImportedNode() {
		return importedNode;
	}
	
	public MyNode getComputedNode() {
		return computedNode;
	}
	
	private void createGroupNode(MyNode parent, ISfsys.IDirectory group) {
		for ( Iterator iter = group.getChildren().iterator(); iter.hasNext(); ) {
			ISfsys.INode node = (ISfsys.INode) iter.next();
			if ( node instanceof ISfsys.IFile ) {
				MyNode mynode = new MyNode().reset(node.getName(), true);
				parent.add(mynode);
			}
			else if ( node instanceof ISfsys.IDirectory ) {
				// recurse creating subgroup nodes:
				ISfsys.IDirectory subgroup = (ISfsys.IDirectory) node;
				MyNode child = new MyNode().reset(subgroup.getName(), false);
				createGroupNode(child, subgroup);
				parent.add(child);
				
				String subpath = subgroup.getPath();
				if ( subpath.equals("/computed") )
					computedNode = child;
				else if ( subpath.equals("/imported") )
					importedNode = child;
			}
			else
				throw new RuntimeException("Unexpected object type");
		}
	}
	
	public List getSelectedSpectraNodes() {
		return _getSelectedNodes(true, true);
	}
	public List getSelectedSpectraPaths() {
		return _getSelectedNodes(true, false);
	}
	public List getSelectedGroupNodes() {
		return _getSelectedNodes(false, true);
	}
	public List getSelectedGroupPaths() {
		return _getSelectedNodes(false, false);
	}
	
	/**
	 * @param spectra true for spectra; false for groups (aka directories).
	 * @param nodes true for nodes; false for paths
	 * @return The list of elements, NEVER null.
	 */
	private List _getSelectedNodes(boolean spectra, boolean nodes) {
		List list = new ArrayList();
		TreePath[] paths = jtree.getSelectionPaths(); 
		if ( paths != null ) {
			for ( int i = 0; i < paths.length; i++ ) {
				TreePath tree_path = paths[i];
				MyNode n = (MyNode) tree_path.getLastPathComponent();
				if ( spectra && n.isSpectrum()  ||  !spectra && n.isGroup() ) {
					if ( nodes )
						list.add(n);
					else {
						if ( n.getStringPath().trim().length() == 0 ) {
							System.out.println(n._toString());
							assert false;
						}
						list.add(n.getStringPath());
					}
				}
			}
		}
		return list;
	}
	
    public MyNode getFocusedNode() {
		return focusedNode;
	}

    protected void updateFocusedNode(MyNode node, int row) {
		if ( focusedNode != node ) {
			focusedNode = node;
			dbgui.focusedNodeChanged();
		}
	}

	/** adds a child if necessary. */
    public MyNode addChild(
		MyNode parent,
		String child_name,
		boolean isSpectrum,
		boolean shouldBeVisible
	) {
        if ( parent == null ) 
            parent = rootNode;
        MyNode childNode = findChildNode(parent, child_name);
		if ( childNode == null ) {
			childNode = new MyNode().reset(child_name, isSpectrum);
	        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
			childNode.setParent(parent);
		}
        if ( shouldBeVisible ) 
            jtree.scrollPathToVisible(new TreePath(childNode.getPath()));
        return childNode;
    }

	class MyRenderer extends DefaultTreeCellRenderer  {
		Font normalFont = null;
		Font boldFont = null;
		
		/**
		 * Returns a wider preferred size value in an attempt to make room for 
		 * the bold font. <b>This is not guaranteed.</b>
		 */
		public Dimension getPreferredSize() {
			Dimension dim = super.getPreferredSize();
			if ( dim != null ) {
				// 16: arbitrarely chosen.
				int new_width = 16 + (int) dim.width;
				dim = new Dimension(new_width, dim.height);
			}
			return dim;
		}

		public Component getTreeCellRendererComponent(
			JTree jtree,
			Object value,
			boolean sel,
			boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus
		) {
			if ( !(value instanceof MyNode) ) {
				super.getTreeCellRendererComponent(
					jtree, value, sel,
					expanded, leaf, row,
					hasFocus
				);
				return this;
			}

			MyNode n = (MyNode) value;
			
			if ( hasFocus )
				updateFocusedNode(n, row);

			// do default behaviour:
			super.getTreeCellRendererComponent(
				jtree, value, sel,
				expanded, leaf, row,
				hasFocus
			);
			
			if ( n.isGroup() ) {
				setIcon(openIcon);
				setBackgroundSelectionColor(bg_group);
				//setToolTipText("....");
			}
			else if ( n.isSpectrum() )  {
				setBackgroundSelectionColor(bg_spectrum);
				//setToolTipText(null); //no tool tip
			}
	
			if ( normalFont == null ) {
				normalFont = getFont();
				if ( normalFont != null )
					boldFont = normalFont.deriveFont(Font.BOLD);
			}
			
			if ( normalFont != null && boldFont != null )
				setFont(hasFocus ? boldFont : normalFont);

			return this;
		}
	}

	public static class MyNode extends DefaultMutableTreeNode {
		String name; // always the simple name -- no slashes at all
		boolean isSpectrum;
		boolean isGroup;
		
		MyNode() {
			super();
		}
		
		MyNode reset(String name) {
			return reset(name, false, false, true);
		}
		
		MyNode reset(String name, boolean isSpectrum) {
			return reset(name, isSpectrum, !isSpectrum, !isSpectrum);
		}
		
		private MyNode reset(String name, boolean isSpectrum, boolean isGroup, boolean allowsChildren) {
			assert name.trim().length() > 0;
			this.name = name;
			this.isSpectrum = isSpectrum;
			this.isGroup = isGroup;
			setAllowsChildren(allowsChildren);
			return this;
		}
		
		public String getStringPath() {
			TreeNode[] t = getPath();
			assert this == t[t.length - 1];
			StringBuffer sb = new StringBuffer();
			// from 1 to omit root node:
			for ( int i = 1; i < t.length; i++ ) {
				MyNode mynode = (MyNode) t[i];
				sb.append("/" +mynode.getName());
			}
			return sb.toString();
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isGroup() {
			return isGroup;
		}
		
		public boolean isSpectrum() {
			return isSpectrum;
		}
		
		public String toString() {
			return name;
		}
		
		String _toString() {
			return "name=[" +name+ "]\n"
				+  "isSpectrum=[" +isSpectrum+ "]\n"
				+  "isGroup=[" +isGroup+ "]\n"
				+  "allowsChildren=[" +getAllowsChildren()+ "]"
			;
		}
	}
}
