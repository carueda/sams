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
	private static final Color bg_grouping = new Color(240,240,240);
	private static final Color bg_group = new Color(222,222,255);
	private static final Color bg_spectrum = new Color(210,255,255);

	// base structure:
	protected final MyNode rootNode = new MyNode().reset("^");
	protected final MyNode locationNode = new MyNode().reset("location:", false, true, true);
	protected MyNode importedNode;
	protected MyNode computedNode;
	
	protected DbGui dbgui;
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

		// setup tree model:		
        treeModel = new DefaultTreeModel(rootNode);
		treeModel.insertNodeInto(locationNode, rootNode, rootNode.getChildCount());
		locationNode.setParent(rootNode);
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
	
	public void resetInfo() {
		locationNode.removeAllChildren();
		ISamsDb db = dbgui.getDatabase();
		if ( db != null ) {
			try {
				createGroupNode(locationNode, db.getGroupingUnderLocation("/"));
			}
			catch(Exception ex) {
				ex.printStackTrace();  // shouldn't happen
			}
		}
		treeModel.reload();
	}		
		
	public void updateReadOnlyGroupingBy(String[] attrNames) {
		StringBuffer sb = new StringBuffer("");
		for ( int i = 0; i < attrNames.length; i++ )
			sb.append(attrNames[i].toLowerCase()+ ":");
		String gby_name = sb.toString();
		if ( gby_name.equals("location:") || gby_name.equals("location:name:") ) {
			// maybe do something here later.
			return;
		}
		// find if node already exists:
		MyNode gby_node = null;
		for ( int i = 0; i < rootNode.getChildCount(); i++ ) {
			MyNode node = (MyNode) rootNode.getChildAt(i);
			if ( node.getName().equals(gby_name) ) {
				gby_node = node;
				break;
			}
		}
		if ( gby_node == null ) {   // not found; create:
			gby_node = new MyNode().reset(gby_name);
			treeModel.insertNodeInto(gby_node, rootNode, rootNode.getChildCount());
			gby_node.setParent(rootNode);
		}
		else {
			gby_node.removeAllChildren();
			treeModel.reload();
		}
		
		ISamsDb db = dbgui.getDatabase();
		if ( db != null ) {
			try {
				createGroupNode(gby_node, db.getGroupingBy(attrNames));
			}
			catch(Exception ex) {
				ex.printStackTrace();  // shouldn't happen
			}
		}
		if ( gby_node.getChildCount() > 0 ) {
			// scroll to first child:
			MyNode childNode = (MyNode) gby_node.getChildAt(0);
			jtree.scrollPathToVisible(new TreePath(childNode.getPath()));
		}
	}		
		
	public List getLocationGroups() {
		List loc_groups = new ArrayList();
		loc_groups.add("/");
		loc_groups.add("/computed");
		loc_groups.add("/imported");
		loc_groups.add("/OTHER");
		return loc_groups;
	}
	
	public MyNode findLocationNode(String path, boolean isSpectrum) {
		if ( path.length() == 0 || path.equals("/") )
			return isSpectrum ? null : locationNode;
		
		MyNode parent = locationNode;
		String[] parts = path.split("/");
		for ( int i = 0; i < parts.length; i++ ) {
			String part = parts[i];
			if ( part.length() == 0 )
				continue;
			parent = findChildNode(parent, part, i == parts.length-1 && isSpectrum);
			if ( parent == null )
				return null;
			if ( i == parts.length-1 )  // found;
				return parent;
		}
		return null;
	}
	
	private MyNode findChildNode(MyNode parent, String name, boolean isSpectrum) {
		for ( int i = 0; i < parent.getChildCount(); i++ ) {
			MyNode n = (MyNode) parent.getChildAt(i);
			if ( name.equals(n.getName()) ) {
				if ( isSpectrum && n.isSpectrum()
				||  !isSpectrum && n.isGroup() )
					return n;
			}
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
		MyNode parent = findLocationNode(parent_path, false);
		if ( parent == null )
			return null;

		String name = path.substring(path.lastIndexOf('/') + 1);
		addChild(parent, name, isSpectrum, true);
		return parent_path;
	}
		
	/** Removes a removable node. */
	public void removeNode(String path, boolean isSpectrum) {
		MyNode node = findLocationNode(path, isSpectrum);
		if ( node == null )
			return;
		if ( node == locationNode ) {
			jtree.scrollPathToVisible(new TreePath(locationNode.getPath()));
			return;
		}
		MyNode parent = (MyNode) node.getParent();
		if ( parent == null )
			return;
		treeModel.removeNodeFromParent(node);
		//treeModel.reload();
		jtree.scrollPathToVisible(new TreePath(parent.getPath()));
	}
	
	public MyNode getImportedNode() {
		return importedNode;
	}
	
	public MyNode getComputedNode() {
		return computedNode;
	}
	
	private void createGroupNode(MyNode parent, ISfsys.INode group) {
		for ( Iterator iter = group.getChildren().iterator(); iter.hasNext(); ) {
			ISfsys.INode node = (ISfsys.INode) iter.next();
			if ( node.isFile() ) {
				MyNode mynode = new MyNode().reset(node.getName(), true);
				parent.add(mynode);
			}
			else if ( node.isDirectory() ) {
				// recurse creating subgroup nodes:
				MyNode child = new MyNode().reset(node.getName(), false);
				createGroupNode(child, node);
				parent.add(child);
				
				String subpath = node.getPath();
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
	
	public List getSelectedGroups() {
		return _getSelectedNodes(false, true);
	}
	
	// TO BE ELIMINATED !!!!!!!
	public List getSelectedSpectraPaths() {
		return _getSelectedNodes(true, false);
	}
	
	// TO BE ELIMINATED !!!!!!!
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
				if ( (spectra && n.isSpectrum())  ||  (!spectra && n.isGroup()) ) {
					if ( nodes )
						list.add(n);
					else {
						String loc_path = n.getLocationPath();
						if ( loc_path.trim().length() > 0 )
							list.add(loc_path);
					}
				}
			}
		}
		return list;
	}
	
	/** @return true iff current selection is only under "location:" branch. */
	public boolean selectionOnlyUnderLocation() {
		TreePath[] paths = jtree.getSelectionPaths(); 
		if ( paths != null ) {
			for ( int i = 0; i < paths.length; i++ ) {
				TreePath tree_path = paths[i];
				Object[] node_path = tree_path.getPath();
				if ( node_path.length >= 2 && !((MyNode)node_path[1]).getName().equals("location:") )
					return false;
			}
		}
		return true;
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
            parent = locationNode;
        MyNode childNode = findChildNode(parent, child_name, isSpectrum);
		if ( childNode == null ) {
			childNode = new MyNode().reset(child_name, isSpectrum);
	        treeModel.insertNodeInto(childNode, parent, isSpectrum ? parent.getChildCount() : 0);
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
			
			boolean set_bold_font = false;
			if ( n.isGroup() ) {
				setIcon(openIcon);
				setBackgroundSelectionColor(bg_group);
				set_bold_font = getText().equals("location:");
			}
			else if ( n.isSpectrum() )  {
				setBackgroundSelectionColor(bg_spectrum);
			}
			else {
				setBackgroundSelectionColor(bg_grouping);
				set_bold_font = true;
			}
			set_bold_font = set_bold_font || hasFocus;
	
			if ( normalFont == null ) {
				normalFont = getFont();
				if ( normalFont != null )
					boldFont = normalFont.deriveFont(Font.BOLD);
			}
			
			if ( normalFont != null && boldFont != null )
				setFont(set_bold_font ? boldFont : normalFont);

			return this;
		}
	}

	public static class MyNode extends DefaultMutableTreeNode {
		// Starts with "/" when this node refers to a read-only subgrouping leaf; so
		// this is exactly the value returned by getLocationPath.
		// otherwise, this name is just the last part of a path that starts from
		// the "location:" subgrouping.
		String name; 
		
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
		
		/** tells if this node is under the "location:" grouping. */
		public boolean underLocationGrouping() {
			TreeNode[] node_path = getPath();
			return node_path.length >= 2 && !((MyNode)node_path[1]).getName().equals("location:");
		}
		
		/** gets the real path under "location:" of the group or signature referenced by this node.
		 * Note that this.underLocationGrouping() can be false. */
		public String getLocationPath() {
			if ( name.startsWith("/") ) {
				// this is the case when the name is precisely the path
				return name;
			}
			else {
				if ( name.equals("location:") ) {
					// the location root:
					return "/";
				}
				StringBuffer sb = new StringBuffer();
				TreeNode[] t = getPath();
				if ( t.length >= 2 && ((MyNode) t[1]).getName().equals("location:") ) {
					// from 2 to omit root and "location:" nodes:
					for ( int i = 2; i < t.length; i++ ) {
						MyNode mynode = (MyNode) t[i];
						sb.append("/" +mynode.getName());
					}
				}
				
				return sb.toString();
			}
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isGroup() {
			return !isSpectrum; //isGroup;
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
