package samsgui.dbgui;

import samsgui.SamsGui;
import samsgui.BaseDialog;
import samsgui.Prefs;

import samscore.ISamsDb;
import samscore.ISamsDb.*;
import samscore.Sams;
import sig.Signature;
import sigoper.*;
import sfsys.ISfsys;
import sfsys.ISfsys.*;

import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Font;
import java.util.*;

/**
 * GUI for a SAMS database.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class DbGui extends JPanel {
	JFrame parentFrame;  // accesible to Plot
	private ISamsDb db;
	private Tree tree;
	private Plot plot;
	private Table table;

	private JSplitPane splitPane1;
	private JSplitPane splitPane2;
	
	private JLabel loc_label;
	
	private StatusBar statusBar;
	/** the reference for reference-based operations. */
	private String referenceSID;
	
	/** The popup menu for spectrum. */
	private JPopupMenu popupSpectrum;

	/** Popup menus for empty selections. */
	private JPopupMenu popupSpectrumNoSelection;
	private JPopupMenu popupGroupNoSelection;

		
	public DbGui(JFrame parentFrame, ISamsDb db) throws Exception {
		super(new BorderLayout());
		this.db = db;
		this.parentFrame = parentFrame;
		splitPane1 = getJSplitPane1();
		splitPane2 = getJSplitPane2();

		tree = new Tree(this);
		table = new Table() {
			protected ISpectrum doRenaming(ISpectrum s, String new_name_value) {
				try {
					return _doRenaming(s, new_name_value);
				}
				catch(Exception ex) {
					SamsGui.message("Error: " +ex.getMessage());
					return null;
				}
			}
		};

		splitPane2.add(table);
		splitPane2.add(createPlotPanel());
		splitPane1.add(tree);
		splitPane1.add(splitPane2);
		add(splitPane1, BorderLayout.CENTER);
		tree.setMinimumSize(new Dimension(200, 130));
		table.setMinimumSize(new Dimension(80, 130));

		tree.getJTree().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent me) {
				JTree jtree = tree.getJTree();
				int selRow = jtree.getRowForLocation(me.getX(), me.getY());
				if ( selRow != -1 ) {
					TreePath treePath = jtree.getPathForLocation(me.getX(), me.getY());
					click(treePath, me);
				}
			}
		});

		tree.getJTree().addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e){
				treeSelectionChanged(e);
			}
		});
		
		add(statusBar = new StatusBar(),  BorderLayout.SOUTH);
		
		setDatabase(db);
	}
	
	private ISpectrum _doRenaming(ISpectrum s, String new_name_value) throws Exception {
		assert new_name_value.length() > 0;
		String oldpath = s.getPath();
		String newpath = db.renameSpectrum(oldpath, s.getLocation() + new_name_value); 
		if ( newpath == null )
			return null; // OK, there was no necessary change
		s = db.getSpectrum(newpath);
		tree.removeNode(oldpath);
		tree.insertNode(newpath, true);
		clearPlot();
		return s;
	}
	
	/** notifies */
	public void metadataUpdated() {
		table.updateMetadata();
	}

	/** notifies */
	public void refreshTable() {
		table.updateData();
	}

	public ISamsDb getDatabase() {
		return db;
	}

	public void saveDatabase() {
		if ( db == null )
			return;
		try {
			db.save();
		}
		catch(Exception ex) {
			SamsGui.message(db.getInfo()+ "\n\nCould not save database: " +ex.getMessage());
		}
	}
	
	public void setDatabase(ISamsDb db) throws Exception {
		this.db = db;
		table.setDatabase(db);
		tree.setInfo();
		table.revalidate();
		plot.reset();
		plot.repaint();
	}
	
	public Tree getTree() {
		return tree;
	}
	
	public void showLegendsWindow() {
		plot.showLegendsWindow();
	}
	
	public void clearPlot() {
		plot.clearSignatures();
		plot.repaint();
	}
	
	public void formatPlot() {
		plot.showPlotFormatter();
	}
		
	public void plotSelectedSignatures(boolean only) {
		if ( db == null )
			return;
		Collection sids = new ArrayList();
		List selectedSpectra = tree.getSelectedSpectraNodes();
		if ( selectedSpectra != null ) {
			for ( Iterator it = selectedSpectra.iterator(); it.hasNext(); ) {
				Tree.MyNode n = (Tree.MyNode) it.next();
				sids.add(n.getStringPath());
			}
		}
		plotSignatures(sids, only);
		plot.repaint();
	}
	public void plotSignatures(Collection paths, boolean only) {
		if ( db == null )
			return;
		
		if ( only )
			plot.clearSignatures();

		try {
			for ( Iterator it = paths.iterator(); it.hasNext(); ) {
				String path = (String) it.next();
				Signature sig = db.getSignature(path);
				String legend = path;
				plot.addSignature(sig, legend);
			}
		}
		catch ( Exception ex ) {
			SamsGui.message("Error: " +ex.getMessage());
		}
	}
	
	public void printPlot() {
		try {
			//plot.printPtolemyVersion();
			
			// alternative way -- under testing
			plot.print(); 
		}
		catch (Exception ex) {
			SamsGui.message("Printing failed:\n" + ex.toString());
		}
		parentFrame.toFront();
	}
	public JFrame getFrame() {
		return parentFrame;
	}
	
	public void display() {
		if ( !parentFrame.isShowing() )
			parentFrame.setVisible(true);
		parentFrame.toFront();
	}
	
	public void close() {
		try {
			setDatabase(null);
		}
		catch (Exception ex) {
			// ignore `cause shouldn't happen: database is null
		}
	}
	
	private JSplitPane getJSplitPane1() {
		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		sp.setDividerSize(8);
		sp.setAutoscrolls(false);
		sp.setContinuousLayout(false);
		sp.setDividerLocation(.5);
		sp.setOneTouchExpandable(true);
		return sp;
	}
	
	public JSplitPane getJSplitPane2() {
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setDividerSize(8);
		sp.setAutoscrolls(false);
		sp.setContinuousLayout(false);
		sp.setDividerLocation(.5);
		sp.setOneTouchExpandable(true);
		return sp;
	}

	JPanel createPlotPanel() {
		JPanel p = new JPanel(new BorderLayout());
		plot = new Plot(this);
		p.add(plot, "Center");
		JToolBar tb = new JToolBar();
		p.add(tb, "North");
		tb.setBorderPainted(true);
		tb.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		JLabel label = new JLabel("Plot");
		label.setForeground(Color.gray);
		tb.add(label);
		
		JButton[] buttons = plot.getButtons();
		for ( int i = 0; i < buttons.length; i++ )
			tb.add(buttons[i]);
		
		//tb.add(Actions.getAction("range-plot"));
		//tb.add(Actions.getAction("export-plot"));
		//tb.add(Actions.getAction("print-plot"));   FOR INTERNAL TESTING

		tb.addSeparator();
		loc_label = new JLabel("x: y:");
		loc_label.setForeground(Color.gray);
		tb.add(loc_label);

		return p;
	}
	
	// accesible to Plot
	void updateLocation(double x, double y) {
		loc_label.setText("x=" +x+ "  y=" +y);
	}
	
	protected void click(TreePath treePath, MouseEvent me) {
		JTree jtree = tree.getJTree();
		if ( !jtree.isPathSelected(treePath) && (me.getModifiers() & MouseEvent.BUTTON3_MASK) != 0 ) {
			if ( me.isControlDown() )
				jtree.addSelectionPath(treePath);
			else
				jtree.setSelectionPath(treePath);
		}
		Tree.MyNode node = (Tree.MyNode) treePath.getLastPathComponent();
		if ( node.isGroup() )
			clickGroup(node, me);
		else if ( node.isSpectrum() )
			clickSpectrum(node, me);
	}
	
	public void clickSpectrum(Tree.MyNode n, MouseEvent me) {
		if ( (me.getModifiers() & MouseEvent.BUTTON3_MASK) != 0 ) {
			JPopupMenu popup = getPopupMenuSpectrum();
			Component c = (Component) me.getSource();
			popup.show(c, me.getX()+5, me.getY());
			return;
		}
		String path = n.getStringPath();
		Signature sig = null;
		try {
			sig = db.getSignature(path);
			String legend = path;
			if ( me.isControlDown() )
				plot.toggleSignature(sig, legend);
			else
				plot.setSignature(sig, legend);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			SamsGui.message("Error: " +ex.getMessage());
		}
		plot.repaint();
	}

	public void clickGroup(Tree.MyNode n, MouseEvent me) {
		if ( (me.getModifiers() & MouseEvent.BUTTON3_MASK) != 0 ) {
			JPopupMenu popup = getPopupMenuGroup();
			Component c = (Component) me.getSource();
			popup.show(c, me.getX()+5, me.getY());
			return;
		}
	}

	JPopupMenu getPopupMenuSpectrum() {
		List selectedSpectra = tree.getSelectedSpectraPaths();
		if ( selectedSpectra == null ) {
			// no selection of spectra elements: show corresponding popup:
			if ( popupSpectrumNoSelection == null ) {
				popupSpectrumNoSelection = new JPopupMenu();
				popupSpectrumNoSelection.add(new JLabel(" No signatures selected ", JLabel.RIGHT));
				popupSpectrumNoSelection.addSeparator();
			}
			return popupSpectrumNoSelection;
		}

		String title;
		if (  selectedSpectra.size() == 1 )
			title = "Selected: " +((String) selectedSpectra.get(0));
		else
			title = "Multiple selection: " +selectedSpectra.size()+ " signatures";
	
		popupSpectrum = new JPopupMenu();
		JLabel label = new JLabel(title, JLabel.LEFT);
		label.setIcon(tree.getLeafIcon());
		label.setForeground(Color.gray);
		popupSpectrum.add(label);
		popupSpectrum.addSeparator();
		popupSpectrum.add(createComputeMenu());
		popupSpectrum.addSeparator();
		List list = Actions.getSelectedSpectraActions(selectedSpectra);
		for ( Iterator it = list.iterator(); it.hasNext(); ) {
			Action action = (Action) it.next();
			if ( action == null )
				popupSpectrum.addSeparator();
			else
				popupSpectrum.add(action);
		}
		return popupSpectrum;
	}
	
	JPopupMenu getPopupMenuGroup() {
		List selectedGroups = tree.getSelectedGroupPaths();
		if ( selectedGroups == null ) {
			// no selection of group elements: show corresponding popup:
			if ( popupGroupNoSelection == null ) {
				popupGroupNoSelection = new JPopupMenu();
				popupGroupNoSelection.add(new JLabel(" No groups selected ", JLabel.RIGHT));
				popupGroupNoSelection.addSeparator();
			}
			return popupGroupNoSelection;
		}
		String title;
		if (  selectedGroups.size() == 1 )
			title = "Selected: " +((String) selectedGroups.get(0));
		else
			title = "Multiple selection: " +selectedGroups.size()+ " groups";
	
		JLabel label = new JLabel(title, tree.getOpenIcon(), JLabel.LEFT);
		label.setForeground(Color.gray);
		JPopupMenu popupGroup = new JPopupMenu();
		popupGroup.add(label);
		popupGroup.addSeparator();

		List list = Actions.getGroupActions(selectedGroups);
		JMenu menu;
		for ( Iterator it = list.iterator(); it.hasNext(); ) {
			Action action = (Action) it.next();
			if ( action == null )
				popupGroup.addSeparator();
			else
				popupGroup.add(action);
		}
		return popupGroup;
	}
	
	public void compute(String opername) throws Exception {
		Signature reference_sig = null;
		IOperation sigOper = SignatureOperationManager.getSignatureOperation(opername);
		if ( sigOper instanceof IBinarySignatureOperation ) {
			if ( referenceSID == null ) {
				SamsGui.message(
					"This is a reference-based operation.\n" +
					"Please, set a reference signature first."
				);
				return;
			}
			reference_sig = db.getSignature(referenceSID);
		}
		List selectedSpectra = tree.getSelectedSpectraPaths();
		if ( selectedSpectra != null )
			new Compute(this, sigOper, selectedSpectra, reference_sig);
	}

	public void export(String format) throws Exception {
		if ( db == null )
			return;
		List selectedSpectra = tree.getSelectedSpectraPaths();
		List selectedGroups = tree.getSelectedGroupPaths();
		if ( selectedSpectra == null && selectedGroups == null ) {
			SamsGui.message("Please select the signatures to be exported.");
			return;
		}
		StringBuffer info = null;
		if ( selectedSpectra != null && selectedGroups != null ) {
			info = new StringBuffer("<html>");
			info.append("<b>Note:</b> " +
				"The current selection includes both groups and specific spectra.<br>\n" +
				"Only selected spectra will be exported.<br>\n"
			);
		}
		
		// list of all paths to be exported:
		List paths = new ArrayList();
		
		if ( selectedSpectra != null ) {
			// only selected spectra
			for ( Iterator iter = selectedSpectra.iterator(); iter.hasNext(); )
				paths.add(iter.next());
		}
		else { // selectedGroups != null
			// include members from subgroups?
			if ( SamsGui.confirm(
				selectedGroups.size()+ " group(s) selected.\n" +
				"Direct spectra members will be included.\n" +
				"Do you want to include all members from subgroups as well?"
				)
			) {
				// Yes, include members from subgroups.
				// easy strategy: for each spectrum check if it "belongs" to any of
				// the selected groups or its subgroups:
				for ( Iterator iter = db.getAllPaths(); iter.hasNext(); ) {
					String path = (String) iter.next();
					ISpectrum s = db.getSpectrum(path);
					boolean include = false;
					for ( Iterator iterg = selectedGroups.iterator(); iterg.hasNext(); ) {
						String group_path = (String) iterg.next();
						if ( path.startsWith(group_path+ "/") ) {
							include = true;
							break;
						}
					}
						
					if ( include && !paths.contains(path) )
						paths.add(path);
				}
			}
			else {
				// No, only direct signature members:
				for ( Iterator iterg = selectedGroups.iterator(); iterg.hasNext(); ) {
					String group_path = (String) iterg.next();
					IDirectory dir = db.getGroupingUnderLocation(group_path);
					List children = dir.getChildren();
					for ( Iterator iter = children.iterator(); iter.hasNext(); ) {
						INode inode = (INode) iter.next();
						if ( inode instanceof IFile ) {
							String path = inode.getPath();
							if ( !paths.contains(path) )
								paths.add(path);
						}
					}
				}
			}
		}
		
		// test:
		//for ( Iterator iter = paths.iterator(); iter.hasNext(); )
		//	System.out.println("[" +iter.next()+ "]");

		if ( paths.size() == 0 )
			SamsGui.message("No signatures were specified");
		else
			new Exporter(this, paths, format, info);
	}
	
    protected void treeSelectionChanged(TreeSelectionEvent e){
		updateStatus();
	}

	/** Creates a menu bar for this. */
	public JMenuBar createMenuBar() {
		JMenuBar mb = new JMenuBar();
		JMenu m;
		JMenu submenu;
		
		/////////////////////////////////////////////////////////////
		// Database menu
		m = new JMenu("Database");
		mb.add(m);
		m.setMnemonic(KeyEvent.VK_D);
		m.add(Actions.getAction("new-database"));
		m.add(Actions.getAction("open-database"));
		m.add(Actions.getAction("save-database"));
		m.add(Actions.getAction("close-database"));
		m.add(Actions.getAction("delete-database"));

		m.addSeparator();
		
		m.add(Actions.getAction("edit-spectrum-structure"));
		
		submenu = new JMenu("Import signatures from");
		m.add(submenu);
		submenu.setMnemonic(KeyEvent.VK_I);
		submenu.add(Actions.getAction("import-files-database"));
		submenu.add(Actions.getAction("import-envi-signatures"));
		submenu.add(Actions.getAction("import-signatures-from-ascii"));
		submenu.add(Actions.getAction("import-system-clipboard"));
		
		submenu = new JMenu("New grouping by...");
		m.add(submenu);
		submenu.setMnemonic(KeyEvent.VK_G);
		submenu.add(Actions.getAction("new-grouping-by-attribute"));
		submenu.add(Actions.getAction("new-grouping-filename"));

		m.addSeparator();
		m.add(Actions.getAction("quit"));

		/////////////////////////////////////////////////////////////
		// "Selected" menu
		m = new JMenu("Selected");
		mb.add(m);
		m.setMnemonic(KeyEvent.VK_S);
		m.add(createComputeMenu());
		m.addSeparator();
		for ( Iterator it = Actions.getSelectedSpectraActions(null).iterator(); it.hasNext(); ) {
			Action action = (Action) it.next();
			if ( action == null )
				m.addSeparator();
			else
				m.add(action);
		}
		
		
		/////////////////////////////////////////////////////////////
		// Plot menu
		m = new JMenu("Plot");
		mb.add(m);
		m.setMnemonic(KeyEvent.VK_P);
		m.add(Actions.getAction("clear-plot"));

		submenu = new JMenu("Range");
		m.add(submenu);
		submenu.setMnemonic(KeyEvent.VK_R);
		for ( Iterator it = getPlotRangeActions().iterator(); it.hasNext(); ) {
			Action action = (Action) it.next();
			if ( action != null )
				submenu.add(new JMenuItem(action));
			else
				submenu.addSeparator();
		}
		submenu = new JMenu("Export");
		submenu.setMnemonic(KeyEvent.VK_X);
		m.add(submenu);
		for ( Iterator it = getPlotExportActions().iterator(); it.hasNext(); ) {
			Action action = (Action) it.next();
			if ( action != null )
				submenu.add(new JMenuItem(action));
			else
				submenu.addSeparator();
		}
		
		m.add(Actions.getAction("format-plot"));
		m.add(Actions.getAction("print-plot"));
		m.add(Actions.getAction("plot-window-legends"));

		m.add(new JCheckBoxMenuItem(
			new AbstractAction("Antialiased") {
				public void actionPerformed(ActionEvent e) {
					JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
					plot.setAntiAliased(cbmi.getState());
					plot.repaint();
				}
			}
		));
		
		/////////////////////////////////////////////////////////////
		// Help menu
		m = new JMenu("Help");
		m.setMnemonic(KeyEvent.VK_H);
		mb.add(m);
		m.add(Actions.getAction("about"));

		//updateMenus();

		return mb;
	}

	/** Gets the menu for "compute" options.*/
	JMenu createComputeMenu() {
		JMenu computeMenu = new JMenu("Compute");
		computeMenu.setMnemonic(KeyEvent.VK_O);
		for ( Iterator it = Actions.getComputeActions(null).iterator(); it.hasNext(); ) {
			Action action = (Action) it.next();
			if ( action == null )
				computeMenu.addSeparator();
			else
				computeMenu.add(action);
		}
		return computeMenu;
	}

	/** @return A list (Action) containing the actions. */
	public List getPlotRangeActions() {
		List plot_range_actions = new ArrayList();
		Action action;
		action = new AbstractAction("Full scale") {
			public void actionPerformed(ActionEvent e) {
				plot.fillPlot();
			}
		};
		action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_F));
		plot_range_actions.add(action);
		
		action = new AbstractAction("Visible [400:700]") {
			public void actionPerformed(ActionEvent e) {
				plot.zoomXRange(400, 700);
			}
		};
		action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		plot_range_actions.add(action);

		action = new AbstractAction("NDVI [500:900]") {
			public void actionPerformed(ActionEvent e) {
				plot.zoomXRange(500, 900);
			}
		};
		action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		plot_range_actions.add(action);

		action = new AbstractAction("Chlorophyll [550:680]") {
			public void actionPerformed(ActionEvent e) {
				plot.zoomXRange(550, 680);
			}
		};
		action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		plot_range_actions.add(action);

		plot_range_actions.add(null);
		
		action = new AbstractAction("Zoom current X-range") {
			public void actionPerformed(ActionEvent e) {
				plot.zoomXRange();
			}
		};
		action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
		plot_range_actions.add(action);

		action = new AbstractAction("Zoom current Y-range") {
			public void actionPerformed(ActionEvent e) {
				plot.zoomYRange();
			}
		};
		action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_Y));
		plot_range_actions.add(action);

		return plot_range_actions;
	}

	/** @return A list (Action) containing the actions. */
	public List getPlotExportActions() {
		List plot_export_actions = new ArrayList();
		Action action;
		
		action = new AbstractAction("Encapsulated Postscript") {
			public void actionPerformed(ActionEvent e) {
				plot.exportToEPS();
			}
		};
		action.putValue(Action.SHORT_DESCRIPTION, "Exports to Encapsulated Postscript format");
		action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
		plot_export_actions.add(action);

		return plot_export_actions;
	}

	/** Called by Tree. */
    public void focusedNodeChanged() {
		updateStatus();
	}
	
	public void updateStatus() {
		String signature_selection; 
		List selectedSpectra = tree.getSelectedSpectraPaths();
		if ( selectedSpectra == null )
			signature_selection = "None";
		else if ( selectedSpectra.size() == 1 )
			signature_selection = (String) selectedSpectra.get(0);
		else
			signature_selection = selectedSpectra.size()+ " signatures";
		
		String group_selection; 
		List selectedGroups = tree.getSelectedGroupPaths();
		if ( selectedGroups == null )
			group_selection = "None";
		else if (  selectedGroups.size() == 1 )
			group_selection = (String) selectedGroups.get(0);
		else
			group_selection = selectedGroups.size()+ " groups selected";

		String focused_element = "None";		
		Tree.MyNode focusedNode = tree.getFocusedNode();
		if ( focusedNode != null )
			focused_element = focusedNode.getStringPath();
		
		String reference_signature = "None";
		if ( referenceSID != null )
			reference_signature = referenceSID;

		String clipboard_contents = "None";
		if ( db != null ) {
			int size = db.getClipboard().size();
			if ( size > 0 )
			clipboard_contents = size+ " signatures";
		}

		statusBar.updateStatusInfo(new String[] {
				signature_selection,
				reference_signature,
				focused_element,
				group_selection,
				clipboard_contents,
			}
		);
	}

	/** Sets the focused signature as the reference for reference-based operations. */
	public void setAsReference() {
		Tree.MyNode focusedNode = tree.getFocusedNode();
		if ( focusedNode == null || !focusedNode.isSpectrum() ) {
			SamsGui.message("Please, first focus the signature to be taken as the reference");
			return;
		}
		referenceSID = focusedNode.getStringPath();
		updateStatus();
	}

	public void viewData() {
		if ( db == null )
			return;
		List selectedSpectra = tree.getSelectedSpectraPaths();
		if (  selectedSpectra == null || selectedSpectra.size() != 1 )
			return;
		final String path = (String) selectedSpectra.get(0);
		SignatureTable sigTable;
		try {
			final Signature sig = db.getSignature(path);
			sigTable = new SignatureTable(sig) {
				protected boolean save() {
					try {
						db.setSignature(path, sig);
						return true;
					}
					catch(Exception ex) {
						SamsGui.message("Error: " +ex.getMessage());
						return false;
					}			
				}
			};
		}
		catch(Exception ex) {
			SamsGui.message("Error: " +ex.getMessage());
			return;
		}			
		
		final JDialog frame = new JDialog(getFrame(), path, false);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent _) {
				if ( frame.isShowing() )
					Prefs.updateRectangle(Prefs.VIEW_RECT, frame); 
				frame.dispose();
			}
		});
		frame.getContentPane().add(sigTable);
		Rectangle r = Prefs.getRectangle(Prefs.VIEW_RECT);
		frame.setLocation(r.x, r.y);
		frame.setSize(r.width, r.height);
		frame.setVisible(true);
	}

	public void rename() throws Exception {
		if ( db == null )
			return;
		List selectedSpectra = tree.getSelectedSpectraPaths();
		if (  selectedSpectra == null || selectedSpectra.size() != 1 )
			return;
		String path = (String) selectedSpectra.get(0);
		final ISpectrum s = db.getSpectrum(path);
		if ( s == null )
			throw new Error(path+ ": spectrum not found!!");
		
		JTextField f_oldname = new JTextField(s.getName());
		f_oldname.setEditable(false);
		final JTextField f_newname = new JTextField(12);
		final JLabel status = new JLabel();
		status.setFont(status.getFont().deriveFont(Font.ITALIC));
		
		f_oldname.setBorder(SamsGui.createTitledBorder("Current name"));
		f_newname.setBorder(SamsGui.createTitledBorder("New name"));
		
		Object[] array = {
			f_oldname,
			f_newname,
			status
		};
		
		String diag_title = "Rename signature";
		final BaseDialog form = new BaseDialog(getFrame(), diag_title, array) {
			public boolean dataOk() {
				String msg = null;
				String newname = f_newname.getText().trim();
				if ( newname.length() == 0 )
					msg = "Specify the new name";
				if ( msg == null ) {
					status.setForeground(Color.gray);
					status.setText("OK");
				}
				else {
					status.setForeground(Color.red);
					status.setText(msg);
				}
				return msg == null;
			}
		};
		form.activate();
		form.pack();
		form.setLocationRelativeTo(getFrame());
		form.setVisible(true);
		if ( form.accepted() ) {
			String newname = f_newname.getText().trim();
			try {
				if ( _doRenaming(s, newname) != null )
					table.updateData();
			}
			catch(Exception ex) {
				SamsGui.message("Error: " +ex.getMessage());
				return;
			}
		}
	}
}
