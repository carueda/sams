package samsgui;

import fileutils.Files;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.io.*;

/** 
 * Main SAMS GUI controller.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Controller {
	public static void newDatabase() {
		String dirname = null;
		while ( dirname == null ) {
			dirname = Dialogs.selectDirectory("Specify a NEW directory");
			if ( dirname == null )
				return;
			if ( !dirname.toLowerCase().endsWith(".samsdb") )
				dirname += ".samsdb";
			File dir = new File(dirname);
			if ( dir.exists() ) {
				SamsGui.message(
					dirname+ ": This file or directory already exists\n" +
					"Please, choose a different location/name for the new database"
				);
				dirname = null;
			}
		}
		
		try {
			SamsGui.create(dirname);
		}
		catch(Exception ex) {
			SamsGui.message("Error: " +ex.getMessage());
		}
	}
	
	public static void openDatabase() {
		String dirname = Dialogs.selectDirectory("Select the directory");
		if ( dirname == null )
			return;
		try {
			SamsGui.open(dirname);
		}
		catch(Exception ex) {
			SamsGui.message("Error: " +ex.getMessage());
		}
	}
	
	public static void closeDatabase() {
		SamsGui.close();
	}
	
	public static void saveDatabase() {
		try {
			SamsGui.save();
		}
		catch(Exception ex) {
			SamsGui.message("Error: " +ex.getMessage());
		}
	}
	
	public static void deleteDatabase() {
		try {
			SamsGui.delete();
		}
		catch(Exception ex) {
			SamsGui.message("Error: " +ex.getMessage());
		}
	}
	
	public static void editMetadataDefinition() {
		try {
			SamsGui.editMetadataDefinition();
		}
		catch(Exception ex) {
			SamsGui.message("Error: " +ex.getMessage());
		}
	}
	
	public static void quit() {
		java.awt.Toolkit.getDefaultToolkit().beep();
		if ( SamsGui.confirm("Really quit SAMS?") )
			SamsGui.quit();
	}

	/** Dialog utilities. */
	static class Dialogs {
		private static String currentDirectoryPath = Prefs.get(Prefs.IMPORT_DIR);
	
		private static void managePreference() {
			Prefs.set(Prefs.IMPORT_DIR, currentDirectoryPath);
		}
	
		public static String selectDirectory(String title) {
			managePreference();
			JFileChooser chooser = new JFileChooser(currentDirectoryPath);
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int retval = chooser.showDialog(SamsGui.getFocusedFrame(), null);
			if ( retval == JFileChooser.APPROVE_OPTION ) {
				File theFile = chooser.getSelectedFile();
				if ( theFile != null ) {
					currentDirectoryPath = theFile.getParent();
					managePreference();
					String sel = chooser.getSelectedFile().getAbsolutePath();
					return sel;
				}
			}
			return null;
		}
	
		static public String selectSaveFile(String title, int mode, Files.FileFilter ff) {
			managePreference();
			JFileChooser chooser = new JFileChooser(currentDirectoryPath);
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(mode);
			chooser.setFileFilter(ff);
			int retval = chooser.showSaveDialog(SamsGui.getFocusedFrame());
			if ( retval == JFileChooser.APPROVE_OPTION ) {
				File theFile = chooser.getSelectedFile();
				if ( theFile != null ) {
					String sel = chooser.getSelectedFile().getAbsolutePath();
					return sel;
				}
			}
			return null;
		}
		
		static public String selectFile(String title, int mode, Files.FileFilter ff) {
			managePreference();
			JFileChooser chooser = new JFileChooser(currentDirectoryPath);
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(mode);
			chooser.setFileFilter(ff);
			int retval = chooser.showDialog(SamsGui.getFocusedFrame(), null);
			if ( retval == JFileChooser.APPROVE_OPTION ) {
				File theFile = chooser.getSelectedFile();
				if ( theFile != null ) {
					String sel = chooser.getSelectedFile().getAbsolutePath();
					return sel;
				}
			}
			return null;
		}
	}
}

