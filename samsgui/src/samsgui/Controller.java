package samsgui;

import samsgui.dbgui.*;
import samsgui.dbgui.Tree.MyNode;

import samscore.ISamsDb;
import samscore.ISamsDb.*;
import sfsys.ISfsys;
import sfsys.ISfsys.*;
import fileutils.Files;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.io.*;
import java.util.List;
import java.util.*;

/** 
 * Main SAMS GUI controller.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Controller {
	public static void newDatabase() {
		String dirname = null;
		while ( dirname == null ) {
			dirname = Dialogs.selectDatabaseDirectory("Specify a NEW directory");
			if ( dirname == null )
				return;
			if ( !dirname.toLowerCase().endsWith(".samsdb") )
				dirname += ".samsdb";
			File dir = new File(dirname);
			if ( dir.exists() ) {
				SamsGui.message(
					dirname+ ": This file or directory already exists.\n" +
					"Please, choose a different location/name for the new database"
				);
				dirname = null;
			}
		}
		
		try {
			SamsGui.create(dirname);
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void openDatabase() {
		String dirname = Dialogs.selectDatabaseDirectory("Select directory containing the SAMS database");
		if ( dirname == null )
			return;
		try {
			SamsGui.open(dirname);
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void closeDatabase() {
		try {
			SamsGui.close();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void saveDatabase() {
		try {
			SamsGui.save();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void deleteDatabase() {
		try {
			SamsGui.delete();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void editMetadataDefinition() {
		try {
			SamsGui.editMetadataDefinition();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void importFilesFromDirectory() {
		SamsGui.importFilesFromDirectory();
	}
	
	public static void importFiles() {
		SamsGui.importFiles();
	}
	
	public static void importEnvi() {
		SamsGui.importEnvi();
	}
	
	public static void importAscii() {
		SamsGui.importAscii();
	}
	
	public static void quit() {
		if ( SamsGui.confirm("Really quit SAMS?") )
			SamsGui.quit();
	}

	public static void clearPlot() {
		SamsGui.clearPlot();
	}
	
	public static void printPlot() {
		SamsGui.printPlot();
	}
	public static void formatPlot() {
		SamsGui.formatPlot();
	}
	public static void plotSelectedSignatures(boolean b) {
		SamsGui.plotSelectedSignatures(b);
	}
	
	public static void showLegendsWindow() {
		SamsGui.showLegendsWindow();
	}
	
	public static void setAsReference() {
		SamsGui.setAsReference();
	}
	
	public static void reloadOperations() {
		SamsGui.reloadOperations();
	}
	
	public static void compute(String opername) {
		try {
			SamsGui.compute(opername);
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void export(String format) {
		try {
			SamsGui.export(format);
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	private static void _setEnabledClipboardActions(boolean b) {
		for ( Iterator iter = Actions.getClipboardActions().iterator(); iter.hasNext(); ) {
			((Action) iter.next()).setEnabled(b);
		}
	}
	
	static class ClipboardObserver implements IClipboard.IObserver {
		DbGui dbgui;
		Object message;
		boolean refreshTable;
		boolean insertIntoTree;
		boolean removeFromTree;
		int total;
		Component parent;
		ProgressMonitor pm;
		
		ClipboardObserver(DbGui dbgui, Object message, boolean refreshTable, boolean insertIntoTree, boolean removeFromTree) {
			this.dbgui = dbgui;
			this.message = message;
			this.refreshTable = refreshTable;
			this.insertIntoTree = insertIntoTree;
			this.removeFromTree = removeFromTree;
		}
		
		public void startTask(int total) {
			this.total = total;
			pm = new ProgressMonitor(dbgui.getTree(), message, null, 1, total);
			pm.setMillisToDecideToPopup(0);
			pm.setMillisToPopup(0);
		}
		
		public boolean elementFinished(int index, String path, boolean isSpectrum) {
			pm.setProgress(index);
			if ( insertIntoTree )
				dbgui.getTree().insertNode(path, isSpectrum);
			if ( removeFromTree )
				dbgui.getTree().removeNode(path, isSpectrum);
			return pm.isCanceled();
		}
		
		public void endTask(int processed) { 
			pm.close();
			if ( refreshTable )
				dbgui.refreshTable();
			dbgui.updateStatus();
		}
	}
	
	public static void copy() {
		DbGui dbgui = SamsGui.getFocusedDbGui();
		if ( dbgui == null )
			return;
		final ISamsDb db = dbgui.getDatabase();
		if ( db == null )
			return;
		final List paths = dbgui.getTree().getSelectedSpectraPaths();
		if ( paths.size() == 0 ) {
			SamsGui.message("No selected spectra to copy");
			return;
		}
		try {
			ClipboardObserver obs = new ClipboardObserver(dbgui, "Copying...", false, false, false);
			db.getClipboard().setObserver(obs);
			Thread thread = new Thread(new Runnable() {
				public void run() {
					_setEnabledClipboardActions(false);
					try {
						db.getClipboard().copy(paths);
					}
					catch(Throwable ex) {
						handleThrowable(ex);
					}
					finally {
						_setEnabledClipboardActions(true);
					}
				}
			});
			thread.start();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	public static void paste() {
		final DbGui dbgui = SamsGui.getFocusedDbGui();
		if ( dbgui == null )
			return;
		final ISamsDb db = dbgui.getDatabase();
		if ( db == null )
			return;
		List paths = dbgui.getTree().getSelectedGroups();
		if ( paths.size() != 1 ) {
			SamsGui.message("One group under the location grouping must be selected to paste signatures onto it");
			return;
		}
		MyNode target_node = (MyNode) paths.get(0);
		if ( !target_node.underGrouping("location:") ) {
			SamsGui.message("Only the location grouping can be used for pasting");
			return;
		}
		final String target_path = target_node.getLocationPath();
		try {
			db.getClipboard().setObserver(new ClipboardObserver(dbgui, "Pasting to " +target_path+ "...", true, true, false));
			Thread thread = new Thread(new Runnable() {
				public void run() {
					_setEnabledClipboardActions(false);
					try {
						db.getClipboard().paste(target_path);
					}
					catch(Throwable ex) {
						handleThrowable(ex);
					}
					finally {
						_setEnabledClipboardActions(true);
					}
				}
			});
			thread.start();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}

	public static void cut() {
		DbGui dbgui = SamsGui.getFocusedDbGui();
		if ( dbgui == null )
			return;
		final ISamsDb db = dbgui.getDatabase();
		if ( db == null )
			return;
		final List paths = dbgui.getTree().getSelectedSpectraPaths();
		if ( paths.size() == 0 ) {
			SamsGui.message("No selected spectra to cut");
			return;
		}

		if ( !dbgui.getTree().selectionOnlyUnderLocation() ) {
			SamsGui.message("Please use the location grouping to select the elements to be cut.");
			return;
		}
		
		try {
			ClipboardObserver obs = new ClipboardObserver(dbgui, "Cutting...", true, false, true);
			db.getClipboard().setObserver(obs);
			Thread thread = new Thread(new Runnable() {
				public void run() {
					_setEnabledClipboardActions(false);
					try {
						db.getClipboard().cut(paths);
					}
					catch(Throwable ex) {
						handleThrowable(ex);
					}
					finally {
						_setEnabledClipboardActions(true);
					}
				}
			});
			thread.start();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}

	public static void delete() {
		DbGui dbgui = SamsGui.getFocusedDbGui();
		if ( dbgui == null )
			return;
		final ISamsDb db = dbgui.getDatabase();
		if ( db == null )
			return;
		
		boolean collect_isSpectra;
		List collect_paths;
		String confirm_msg;
		
		List selectedSpectraPaths = dbgui.getTree().getSelectedSpectraPaths();
		List selectedGroups = dbgui.getTree().getSelectedGroups();
		if ( selectedGroups.size() == 0 ) {
			if ( selectedSpectraPaths.size() == 0 ) {
				SamsGui.message("No selected spectra to delete");
				return;
			}
			// Spectra selection.
			collect_isSpectra = true;
			collect_paths = selectedSpectraPaths;
			confirm_msg = "Delete " +
				(collect_paths.size()==1 ? (String)collect_paths.get(0) : collect_paths.size()+" selected elements")+ "?"; 
		}
		else { // selectedGroups.size() > 0
			if ( selectedSpectraPaths.size() > 0 ) {
				SamsGui.message("Both groups and spectra are selected.");
				return;
			}
			// Group selection.
			collect_paths = new ArrayList();
			// check ther are only deletable groups (not under "location:"
			for ( Iterator iter = selectedGroups.iterator(); iter.hasNext(); ) {
				MyNode grp_node = (MyNode) iter.next();
				if ( !grp_node.underGrouping("location:") ) {
					// check if it is a grouping root:
					if ( grp_node.isGroupingNode() ) {
						dbgui.removeGrouping(grp_node);
						continue;
					}
					SamsGui.message("Only groups under location grouping can be deleted.");
					return;
				}
				collect_paths.add(grp_node.getLocationPath());
			}
			collect_isSpectra = false;
			confirm_msg = "Delete " +
				(collect_paths.size()==1 ? (String)collect_paths.get(0) : collect_paths.size()+" selected groups")+ "?\n"+
				"All members and subgroups will also be deleted.";
		}
		
		if ( !dbgui.getTree().selectionOnlyUnderLocation() ) {
			SamsGui.message("Please use the location grouping to select the elements to be deleted.");
			return;
		}
		
		if ( collect_paths.size() == 0 )
			return;
		
		if ( !SamsGui.confirm(confirm_msg) )
			return;
		
		final boolean isSpectra = collect_isSpectra;
		final List paths = collect_paths;

		try {
			ClipboardObserver obs = new ClipboardObserver(dbgui, "Deleting...", true, false, true);
			db.getClipboard().setObserver(obs);
			Thread thread = new Thread(new Runnable() {
				public void run() {
					_setEnabledClipboardActions(false);
					try {
						if ( isSpectra )
							db.getClipboard().delete(paths);
						else
							db.getClipboard().deleteGroups(paths);
					}
					catch(Throwable ex) {
						handleThrowable(ex);
					}
					finally {
						_setEnabledClipboardActions(true);
					}
				}
			});
			thread.start();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	/** View the data of current selected signature. */
	public static void viewData() {
		try {
			SamsGui.viewData();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	/** Renames the current selected signature. */
	public static void rename() {
		try {
			SamsGui.rename();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	/** (Re)creates a grouping. */
	public static void createGroupingBy() {
		try {
			SamsGui.createGroupingBy();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	/** Shows the "About" message. */
	public static void about() {
		SamsGui.showAboutMessage();
	}
	
	/** Creates a subgroup under current selected group. */
	public static void createGroup() {
		try {
			SamsGui.createGroup();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
	/** Refreshes structure of the main grouping associated to current selected group. */
	public static void refreshGrouping() {
		try {
			SamsGui.refreshGrouping();
		}
		catch(Throwable ex) {
			handleThrowable(ex);
		}
	}
	
    public static void doUpdate(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        }
        catch(Exception e) {
            System.err.println(e);
        }
    }
	
	/** Dialog utilities. */
	public static class Dialogs {
		public static String selectDatabaseDirectory(String title) {
			String basedir = "";
			File file = new File(Prefs.get(Prefs.RECENT));
			if ( file.getParent() != null )
				basedir = file.getParent();
			File dir = selectDirectory(title, basedir);
			return dir == null ? null : dir.getAbsolutePath(); 
		}
		
		public static File[] selectImportFiles(String title) {
			String basedir = Prefs.get(Prefs.IMPORT_DIR);
			File[] files = null;
			JFileChooser chooser = new JFileChooser(basedir);
			chooser.setDialogTitle(title);
			chooser.setMultiSelectionEnabled(true);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int retval = chooser.showDialog(SamsGui.getFocusedFrame(), null);
			if ( retval == JFileChooser.APPROVE_OPTION ) {
				files = chooser.getSelectedFiles();
				if ( files.length > 0 )
					Prefs.set(Prefs.IMPORT_DIR, files[0].getParent());
			}
			return files;
		}
		
		public static String selectImportDirectory(String title) {
			String basedir = Prefs.get(Prefs.IMPORT_DIR); 
			File file = selectDirectory(title, basedir);
			if ( file != null ) {
				Prefs.set(Prefs.IMPORT_DIR, file.getParent());
				return file.getAbsolutePath();
			}
			return null;
		}
			
		public static File selectDirectory(String title, String basedir) {
			File file = null;
			JFileChooser chooser = new JFileChooser(basedir);
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int retval = chooser.showDialog(SamsGui.getFocusedFrame(), null);
			if ( retval == JFileChooser.APPROVE_OPTION )
				file = chooser.getSelectedFile();
			return file;
		}
	
		public static String selectImportFile(String title) {
			String basedir = Prefs.get(Prefs.IMPORT_DIR); 
			File file = selectFile(title, basedir);
			if ( file != null ) {
				Prefs.set(Prefs.IMPORT_DIR, file.getParent());
				return file.getAbsolutePath();
			}
			return null;
		}
		
		public static File selectFile(String title, String basedir) {
			File file = null;
			JFileChooser chooser = new JFileChooser(basedir);
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int retval = chooser.showDialog(SamsGui.getFocusedFrame(), null);
			if ( retval == JFileChooser.APPROVE_OPTION )
				file = chooser.getSelectedFile();
			return file;
		}
	
		public static String selectExportFile(String title) {
			return selectExportFile(title, null);
		}
		
		public static String selectExportFile(String title, Files.FileFilter ff) {
			String basedir = Prefs.get(Prefs.EXPORT_DIR); 
			File file = selectSaveFile(title, ff, basedir);
			if ( file != null ) {
				Prefs.set(Prefs.EXPORT_DIR, file.getParent());
				return file.getAbsolutePath();
			}
			return null;
		}
			
		public static File selectSaveFile(String title, Files.FileFilter ff, String basedir) {
			File file = null;
			JFileChooser chooser = new JFileChooser(basedir);
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(ff);
			int retval = chooser.showSaveDialog(SamsGui.getFocusedFrame());
			if ( retval == JFileChooser.APPROVE_OPTION )
				file = chooser.getSelectedFile();
			return file;
		}
	}
	
	public static void handleThrowable(Throwable t) {
		if ( t instanceof RuntimeException || t instanceof Error ) {
			t.printStackTrace();
			SamsGui.message("Internal error: " +t.getMessage());
		}
		else
			SamsGui.message("Error: " +t.getMessage());
	}
	
}

