package samsgui.dbgui;

import sfsys.ISfsys;
import sfsys.ISfsys.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.font.*;
import java.awt.Color;
import java.awt.event.*;
import java.util.Iterator;

/** 
 * Tree display.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Tree extends JPanel {
	ISfsys fs;
	protected DefaultMutableTreeNode rootNode;
    protected DefaultTreeModel treeModel;
    protected JTree tree;
	
	public Tree() {
		super(new BorderLayout());
		fs = null;
		
		JLabel label = new JLabel("Right-click for options", JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(Font.ITALIC));
		label.setForeground(Color.gray);
		add(label, BorderLayout.NORTH);
		
        rootNode = new DefaultMutableTreeNode("Root Node");
        treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);

		// To avoid keyboard navigation when "Alt" is pressed. 
		tree.setUI(new javax.swing.plaf.basic.BasicTreeUI() {
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

		tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
		tree.putClientProperty("JTree.lineStyle", "Angled");
		
        add(new JScrollPane(tree));
	}

	public JTree getJTree() {
		return tree;
	}
	
	public void setInfo(ISfsys	fs) {
		rootNode.removeAllChildren();
		this.fs = fs;
		if ( fs != null ) {
			IDirectory group = fs.getRoot();
			rootNode.setUserObject(group);
			createGroupNode(rootNode, group);
		}
		else
			rootNode.setUserObject("no database");
		treeModel.reload();
	}
	
	private void createGroupNode(DefaultMutableTreeNode parent, IDirectory group) {
		for ( Iterator iter = group.getChildren().iterator(); iter.hasNext(); ) {
			ISfsys.INode node = (ISfsys.INode) iter.next();
			if ( node instanceof ISfsys.IFile ) {
				parent.add(new DefaultMutableTreeNode(node, false));
			}
			else if ( node instanceof ISfsys.IDirectory ) {
				// recurse creating subgroup nodes:
				ISfsys.IDirectory subgroup = (ISfsys.IDirectory) node;
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(subgroup, true);
				createGroupNode(child, subgroup);
				parent.add(child);
			}
			else
				throw new RuntimeException("Unexpected object type");
		}
	}
}
