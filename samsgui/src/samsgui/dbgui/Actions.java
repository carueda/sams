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
	 *
	 * @param selectedSpectra    The list of selected signatures.
	 *
	 * @return A list (Action) containing the actions.
	 */
	public static List getSelectedSpectraActions(List selectedSpectra) {
		List list = new ArrayList();

		if ( selectedSpectra != null && selectedSpectra.size() == 1 ) {
			list.add(getAction("view-data"));
			list.add(getAction("view-source"));
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
		list.add(getAction("delete"));
		list.add(null);
		//list.add(new GroupExportAction("envi"));
		//list.add(new GroupExportAction("envi-sl"));
		//list.add(new GroupExportAction("ascii"));
		
		return list;
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
		actions.put("import-files-database", new BaseAction("Directory",
			"Import signatures from a directory", KeyEvent.VK_D) {
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
		actions.put("import-system-clipboard", new BaseAction("System clipboard",
			"Import from the system clipboard", KeyEvent.VK_S) {
				public void run() {
					Controller.importSystemClipboard();
				}
			}
		);

		actions.put("new-grouping-by-attribute", new BaseAction("Attribute value",
			"Creates a grouping according to attribute values", KeyEvent.VK_A)
		);
		actions.put("new-grouping-filename", new BaseAction("Filename",
			"Creates a grouping according to file names", KeyEvent.VK_F)
		);
		actions.put("delete-grouping", new BaseAction("Delete group",
			"Deletes this group")
		);
		
		actions.put("new-group", new BaseAction("New subgroup",
			"Lets you create a subgroup")
		);

		actions.put("about", new BaseAction("About SAMS..."));
		
		actions.put("copy", new BaseAction("Copy",
			"Copies the selected signatures to an internal clipboard", KeyEvent.VK_C, "control INSERT")
		);
		actions.put("cut", new BaseAction("Cut",
			"Cuts the selected signatures to an internal clipboard", KeyEvent.VK_U, "shift DELETE")
		);
		actions.put("paste-ref", new BaseAction("Paste reference",
			"Pastes references (shortcuts) to the signatures from the internal clipboard", KeyEvent.VK_R, "shift INSERT")
		);
		actions.put("delete", new BaseAction("Delete",
			"Deletes the list of signatures", KeyEvent.VK_D, "DELETE")
		);
		actions.put("rename", new BaseAction("Rename", "Renames a signature"));
		actions.put("select", new BaseAction("Select", "Selectes some signatures")); 
		
		actions.put("view-source", new BaseAction("View source", "View source of a signature"));
		actions.put("view-data", new BaseAction("View data", "View data of a signature"));
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
		
		actions.put("range-plot", new BaseAction("Range", "Changes the displayed X-range"));
		actions.put("export-plot", new BaseAction("Export", "Exporting options"));
		actions.put("analysis", new BaseAction("Analysis", "Operations on spectra"));
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
	
	/** Base class for compute action objects */
	static class ComputeAction extends BaseAction {
		ComputeAction(String opername) {
			super(opername, SignatureOperationManager.getSignatureOperation(opername).getDescription());
		}
	}
	
	public static List getComputeActions(List list) {
		if ( list == null )
			list = new ArrayList();
		List on = Sams.getOperationNames();
		for ( Iterator it = on.iterator(); it.hasNext(); ) {
			final String opername = (String) it.next();
			if ( opername == null )
				list.add(null);
			else {
				list.add(new ComputeAction(opername) {
					public void run() {
						Controller.compute(opername);
					}
				});
			}
		}
		return list;
	}

}

