package samsgui.dbgui;

import samsgui.SamsGui;

import samscore.ISamsDb;
import samscore.Sams;
import sfsys.ISfsys;
import sfsys.ISfsys.*;
import sig.Signature;

import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.*;

/**
 * GUI for a SAMS database.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class DbGui extends JPanel {

	JFrame parentFrame;  // accesible to Plot
	private ISamsDb db;
	private ISfsys fs;
	private Tree tree;
	private Plot plot;
	private Table table;

	private JSplitPane splitPane1;
	private JSplitPane splitPane2;
	
	private JLabel loc_label;
	
		
	public DbGui(JFrame parentFrame, ISamsDb db) throws Exception {
		super(new BorderLayout());
		this.db = db;
		this.parentFrame = parentFrame;
		splitPane1 = getJSplitPane1();
		splitPane2 = getJSplitPane2();

		tree = new Tree();
		table = new Table();

		splitPane2.add(table);
		splitPane2.add(createPlotPanel());
		splitPane1.add(tree);
		splitPane1.add(splitPane2);
		add(splitPane1, BorderLayout.CENTER);
		tree.setMinimumSize(new Dimension(200, 130));
		table.setMinimumSize(new Dimension(80, 130));

		tree.getJTree().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getJTree().getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getJTree().getPathForLocation(e.getX(), e.getY());
				if ( selRow != -1 ) {
					DefaultMutableTreeNode n = (DefaultMutableTreeNode) selPath.getLastPathComponent();
					click(n, e);
				}
			}
		});

		tree.getJTree().addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e){
				treeSelectionChanged(e);
			}
		});
		setDatabase(db);
	}
	
	/** notifies */
	public void metadataUpdated() {
		table.updateMetadata();
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
		fs = null;
		if ( db != null )
			fs = db.getGroupingBy(new String[] {"location"});
		tree.setInfo(fs);
		table.revalidate();
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
	
	protected void click(DefaultMutableTreeNode n, MouseEvent e) {
		Object obj = n.getUserObject();
		if ( obj instanceof IDirectory )
			clickGroup(n, e);
		else if ( obj instanceof IFile )
			clickSpectrum(n, e);
	}
	
	public void clickSpectrum(DefaultMutableTreeNode n, MouseEvent e) {
		IFile file = (IFile) n.getUserObject();
		String path = file.getPath();
		System.out.println("clickSpectrum: " +path);
		Signature sig = null;
		try {
			sig = db.getSignature(path);
			String legend = path;
			if ( e.isControlDown() )
				plot.toggleSignature(sig, legend);
			else
				plot.setSignature(sig, legend);
		}
		catch(Exception ex) {
			plot.setTitle("Error: " +ex.getMessage());
		}
		plot.repaint();
	}

	public void clickGroup(DefaultMutableTreeNode n, MouseEvent e) {
		System.out.println("clickGroup");
	}

    protected void treeSelectionChanged(TreeSelectionEvent e){
		System.out.println("treeSelectionChanged");
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
		for ( Iterator it = getComputeActions(null).iterator(); it.hasNext(); ) {
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

	public static List getComputeActions(List list) {
		if ( list == null )
			list = new ArrayList();
		/*List on = SignatureOperationManager.getOperationNames();
		for ( Iterator it = on.iterator(); it.hasNext(); ) {
			String opername = (String) it.next();
			// null checked as a separator indicator:
			list.add(opername == null ? null : new ComputeAction(opername));
		}*/
		return list;
	}
}
