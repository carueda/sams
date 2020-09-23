package samsgui.dbgui;

import  samsgui.Controller;
import  samscore.Sams;
import  sigoper.*;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;

/** 
 * Actions of the SAMS GUI application.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public final class Actions {
	private static List clipboardActions = null;
	
	/**
	 * Gets an specific action.
	 * If the name is not a predefined action, it returns
	 * a new "not-implemented" action object.
	 *
	 * @param name The action name
	 * @return The action object.
	 */
	public static Action getAction(String name) {
		Action a = (Action) actions.get(name);
		if ( a == null )
			a = new BaseAction(name);
		return a;
	}


	/**
	 * Gets the list of actions on selected signatures.
	 * @param selectedSpectra    The list of selected signatures.
	 * @return A list (Action) containing the actions.
	 */
	public static List getSelectedSpectraActions(List selectedSpectra) {
		List list = new ArrayList();
		if ( selectedSpectra != null && selectedSpectra.size() == 1 ) {
			list.add(getAction("view-data"));
			list.add(getAction("rename"));
			list.add(null);
		}
		list.add(getAction("plot-selected-only"));
		list.add(getAction("plot-selected-add"));
		list.add(null);
		list.add(getAction("set-reference"));
		list.add(null);
		list.add(getAction("copy"));
		list.add(getAction("cut"));
		list.add(getAction("paste"));
		list.add(getAction("delete"));
		list.add(null);
		list.add(getAction("export-envi"));
		list.add(getAction("export-envi-sl"));
		list.add(getAction("export-ascii"));
		list.add(null);
		list.add(getAction("reload-opers"));
		return list;
	}

	/**
	 * Gets the list of actions on groups.
	 * @param selectedSpectra    The list of selected signatures.
	 * @return A list (Action) containing the actions.
	 */
	public static List getGroupActions(List selectedGroups) {
		List list = new ArrayList();
		if ( selectedGroups.size() == 1 ) {
			list.add(getAction("new-group"));
			list.add(getAction("paste"));
			list.add(getAction("refresh-grouping"));
			list.add(null);
		}
		list.add(getAction("delete"));
		list.add(null);
		list.add(getAction("export-envi"));
		list.add(getAction("export-envi-sl"));
		list.add(getAction("export-ascii"));
		return list;
	}

	
	/** Gets the list of clipboard actions. */
	public static List getClipboardActions() {
		if ( clipboardActions == null ) {
			clipboardActions = new ArrayList();
			clipboardActions.add(getAction("copy"));
			clipboardActions.add(getAction("cut"));
			clipboardActions.add(getAction("paste"));
			clipboardActions.add(getAction("delete"));
		}
		return clipboardActions;
	}
	
	// mapping String->Action	
	private static Map actions;
	
	// Initialization with the simple actions.
	static {
		actions = new HashMap();

		actions.put("new-database", new BaseAction("New...", 
			"Lets you define a new database", KeyEvent.VK_N, "control N") {
				public void run() {
					Controller.newDatabase();
				}
			}
		);
		actions.put("open-database", new BaseAction("Open...", 
			"Opens a database into SAMS", KeyEvent.VK_O, "control O") {
				public void run() {
					Controller.openDatabase();
				}
			}
		);
		actions.put("save-database", new BaseAction("Save",
			"Saves the state of the current database", KeyEvent.VK_S, "control S") {
				public void run() {
					Controller.saveDatabase();
				}
			}
		);
		actions.put("close-database", new BaseAction("Close",
			"Closes the current database", KeyEvent.VK_C, "control W") {
				public void run() {
					Controller.closeDatabase();
				}
			}
		);
		actions.put("delete-database", new BaseAction("Delete",
			"Deletes the current database") {
				public void run() {
					Controller.deleteDatabase();
				}
			}
		);
		actions.put("quit", new BaseAction("Quit",
			"Quits SAMS", KeyEvent.VK_Q, "control Q") {
				public void run() {
					Controller.quit();
				}
			}
		);
		actions.put("edit-spectrum-structure", new BaseAction("Edit metadata structure",
			"Edits the spectrum metadata structure", KeyEvent.VK_E) {
				public void run() {
					Controller.editMetadataDefinition();
				}
			}
		);
		actions.put("import-directory", new BaseAction("Directory",
			"Import signatures from a directory", KeyEvent.VK_D) {
				public void run() {
					Controller.importFilesFromDirectory();
				}
			}
		);
		actions.put("import-files", new BaseAction("Specific files",
			"Import signatures from selected files", KeyEvent.VK_S) {
				public void run() {
					Controller.importFiles();
				}
			}
		);
		actions.put("import-envi-signatures", new BaseAction("Envi Standard file",
			"Import from an Envi file", KeyEvent.VK_E) {
				public void run() {
					Controller.importEnvi();
				}
			}
		);
		actions.put("import-signatures-from-ascii", new BaseAction("ASCII file",
			"Import from an ASCII file", KeyEvent.VK_A) {
				public void run() {
					Controller.importAscii();
				}
			}
		);
		actions.put("export-envi", new ExportAction("envi", "Export to Envi Standard file"));
		actions.put("export-envi-sl", new ExportAction("envi-sl", "Export to Envi Spectral Library"));
		actions.put("export-ascii", new ExportAction("ascii", "Export to ASCII file"));

		actions.put("new-grouping-by-attribute", new BaseAction("Attribute value",
			"Creates/updates a grouping according to attribute values", KeyEvent.VK_A) {
				public void run() {
					Controller.createGroupingBy();
				}
			}
		);
		
		actions.put("new-group", new BaseAction("New subgroup",
			"Creates a subgroup") {
				public void run() {
					Controller.createGroup();
				}
			}
		);

		actions.put("refresh-grouping", new BaseAction("Refresh grouping",
			"Refreshes structure of the main grouping associated to this element") {
				public void run() {
					Controller.refreshGrouping();
				}
			}
		);

		actions.put("about", new BaseAction("About SAMS...")  {
				public void run() {
					Controller.about();
				}
			}
		);
		
		actions.put("copy", new BaseAction("Copy",
			"Copies selected elements to an internal clipboard", "control INSERT") {
				public void run() {
					Controller.copy();
				}
			}
		);
		actions.put("cut", new BaseAction("Cut",
			"Cuts selected elements to an internal clipboard", "shift DELETE") {
				public void run() {
					Controller.cut();
				}
			}
		);
		actions.put("paste", new BaseAction("Paste",
			"Pastes elements from the internal clipboard into the selected group", "shift INSERT") {
				public void run() {
					Controller.paste();
				}
			}
		);
		actions.put("delete", new BaseAction("Delete",
			"Deletes the selected elements", "DELETE") {
				public void run() {
					Controller.delete();
				}
			}
		);
		actions.put("rename", new BaseAction("Rename", "Renames a signature") {
				public void run() {
					Controller.rename();
				}
			}
		);
		
		actions.put("view-data", new BaseAction("View data", "View contents of a signature") {
				public void run() {
					Controller.viewData();
				}
			}
		);
		actions.put("set-reference", new BaseAction("Set as reference",
			"Sets the focused signature as the reference for reference-based operations", 0, "alt ENTER") {
				public void run() {
					Controller.setAsReference();
				}
			}
		);
		actions.put("print-plot", new BaseAction("Print",
			"Prints the plot", KeyEvent.VK_P, "control P") {
				public void run() {
					Controller.printPlot();
				}
			}
		);
		
		actions.put("format-plot", new BaseAction("Format", "Sets the plot format", KeyEvent.VK_F) {
				public void run() {
					Controller.formatPlot();
				}
			}
		);
		actions.put("plot-selected-only", new BaseAction("Plot",
			"Plots only the selected signatures", KeyEvent.VK_P, "ENTER") {
				public void run() {
					Controller.plotSelectedSignatures(true);
				}
			}
		);
		actions.put("plot-selected-add", new BaseAction("Add to plot",
			"Adds the selected signatures to the plot", KeyEvent.VK_A, "control ENTER") {
				public void run() {
					Controller.plotSelectedSignatures(false);
				}
			}
		);
		actions.put("clear-plot", new BaseAction("Clear", "Clears the plot", KeyEvent.VK_C) {
				public void run() {
					Controller.clearPlot();
				}
			}
		);
		actions.put("plot-window-legends", new BaseAction("Legends window",
			"Shows the plot legends in a floating window", KeyEvent.VK_L) {
				public void run() {
					Controller.showLegendsWindow();
				}
			}
		);
		
		actions.put("reload-opers", new BaseAction("Reload operations",
			"Reloads operations") {
				public void run() {
					Controller.reloadOperations();
				}
			}
		);
	}

	/** Base class for action objects */
	static class BaseAction extends AbstractAction implements Runnable {
		String name;
		BaseAction(String name) {
			super(name);
			this.name = name;
		}
		BaseAction(String name, String short_descr) {
			this(name);
			putValue(SHORT_DESCRIPTION, short_descr);
		}
		BaseAction(String name, String short_descr, int mnemonic) {
			this(name, short_descr);
			putValue(MNEMONIC_KEY, new Integer(mnemonic));
		}
		BaseAction(String name, String short_descr, int mnemonic, String keyStroke) {
			this(name, short_descr, mnemonic);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyStroke));
		}
		BaseAction(String name, String short_descr, String keyStroke) {
			this(name, short_descr);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyStroke));
		}
		
		public void run() {
			JOptionPane.showMessageDialog(
				null,
				name+ ": not implemented",
				"",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
		
		public void actionPerformed(ActionEvent ev) {
			run();
		}
	}
	
	/** Class for compute action objects */
	static class ComputeAction extends BaseAction {
		ComputeAction(String opername) {
			super(opername, SignatureOperationManager.getSignatureOperation(opername).getDescription());
		}
		public void run() {
			Controller.compute(name);
		}
	}
	
	public static List getComputeActions(List list) {
		if ( list == null )
			list = new ArrayList();
		List on = Sams.getOperationNames();
		for ( Iterator it = on.iterator(); it.hasNext(); ) {
			String opername = (String) it.next();
			if ( opername == null )
				list.add(null);
			else
				list.add(new ComputeAction(opername));
		}
		return list;
	}

	/** Class for export action objects */
	static class ExportAction extends BaseAction {
		String format;
		ExportAction(String format, String name) {
			super(name);
			this.format = format;
		}
		public void run() {
			Controller.export(format);
		}
	}
}

