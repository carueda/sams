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
		catch(Exception ex) {
			SamsGui.message("Error: " +ex.getMessage());
		}
	}
	
	public static void openDatabase() {
		String dirname = Dialogs.selectDatabaseDirectory("Select the directory");
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
	
	public static void importFiles() {
		SamsGui.importFiles();
	}
	
	public static void importEnvi() {
	}
	
	public static void importAscii() {
		SamsGui.importAscii();
	}
	
	public static void importSystemClipboard() {
	}
	
	public static void quit() {
		java.awt.Toolkit.getDefaultToolkit().beep();
		if ( SamsGui.confirm("Really quit SAMS?") )
			SamsGui.quit();
	}

	/** Dialog utilities. */
	public static class Dialogs {
		public static String selectDatabaseDirectory(String title) {
			String basedir = "";
			File file = new File(Prefs.get(Prefs.RECENT));
			if ( file.exists() && file.getParent() != null )
				basedir = file.getParent();
			File dir = selectDirectory(title, basedir);
			return dir == null ? null : dir.getAbsolutePath(); 
		}
		
		public static String selectImportDirectory(String title) {
			String basedir = Prefs.get(Prefs.IMPORT_DIR); 
			File file = selectDirectory(title, basedir);
			if ( file != null )
				Prefs.set(Prefs.IMPORT_DIR, file.getParent());
			return file.getAbsolutePath();
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
			if ( file != null )
				Prefs.set(Prefs.IMPORT_DIR, file.getParent());
			return file.getAbsolutePath();
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
	
		public static String selectExportFile(String title, Files.FileFilter ff) {
			String basedir = Prefs.get(Prefs.EXPORT_DIR); 
			File file = selectSaveFile(title, ff, basedir);
			if ( file != null )
				Prefs.set(Prefs.EXPORT_DIR, file.getParent());
			return file.getAbsolutePath();
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
}

