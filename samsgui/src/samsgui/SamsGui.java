package samsgui;

import samsgui.dbgui.*;

import samscore.Sams;
import samscore.ISamsDb;
import samscore.ISamsDb.*;
import fileutils.Files;
import sfsys.ISfsys;
import sfsys.ISfsys.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import java.io.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.util.List;

/** 
 * Main Sams GUI component.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */ 
public class SamsGui {
	
	private static final String NO_DB_NAME = "<No database>";
	
	static DbGui focusedDbGui;
	/** Mapping from filename -> DbGui */
	static Map openDbGuis;
	
	public static void init(String operdirname) throws Exception {
		Sams.init(operdirname);
		openDbGuis = new HashMap();
		_initSystemMonitoring();
		_setLookAndFeel();
	}
		
	public static void openRecent() throws Exception {
		String filename = Prefs.get(Prefs.RECENT);
		if ( filename.length() == 0 || filename.equals(NO_DB_NAME) ||  !new File(filename).exists() )
			filename = null;
		open(filename);
	}
	
	public static void open(String filename) throws Exception {
		filename = _getCanonicalPath(filename);
		DbGui dbgui = (DbGui) openDbGuis.get(filename);
		if ( dbgui != null ) { // already open
			dbgui.display();
			focusedDbGui = dbgui;
			return;
		}
		ISamsDb db = null;
		DbFrame frame;
		if ( NO_DB_NAME.equals(filename) ) {
			frame = new DbFrame(_nextRectangle());
			_dispatch(frame, db);
		}
		else {
			db = Sams.open(filename);
			// try to use NO_DB_NAME dbgui, if open:
			dbgui = (DbGui) openDbGuis.get(NO_DB_NAME);
			if ( dbgui != null ) {
				frame = (DbFrame) dbgui.getFrame();
				dbgui.setDatabase(db);
				openDbGuis.remove(NO_DB_NAME);
				focusedDbGui = dbgui;
				focusedDbGui.display();
			}
			else {
				frame = new DbFrame(_nextRectangle());
				_dispatch(frame, db);
			}
		}
		frame.filename = filename;
		frame.setTitle("SAMS - " +frame.filename);
		openDbGuis.put(filename, focusedDbGui);
	}

	public static void create(String filename) throws Exception {
		filename = _getCanonicalPath(filename);
		if ( openDbGuis.get(filename) != null ) { // already open
			message(filename+ "\nThis database already exists and is currently open");
			return;
		}
		ISamsDb db = Sams.create(filename);
		DbFrame frame;
		// try to use NO_DB_NAME dbgui, if open:
		DbGui dbgui = (DbGui) openDbGuis.get(NO_DB_NAME);
		if ( dbgui != null ) {
			frame = (DbFrame) dbgui.getFrame();
			dbgui.setDatabase(db);
			openDbGuis.remove(NO_DB_NAME);
			focusedDbGui = dbgui;
			focusedDbGui.display();
		}
		else {
			Rectangle rect = _nextRectangle();
			frame = new DbFrame(rect);
			_dispatch(frame, db);
		}
		frame.filename = filename;
		frame.setTitle("SAMS - " +frame.filename);
		openDbGuis.put(filename, focusedDbGui);
	}

	private static void _dispatch(DbFrame frame, ISamsDb db) throws Exception {
		focusedDbGui = new DbGui(frame, db);
		frame.dbgui = focusedDbGui;
		frame.setJMenuBar(focusedDbGui.createMenuBar());
		frame.getContentPane().add(focusedDbGui);
		focusedDbGui.display();
	}
	
	/** Updates prefs related to recent open database. */
	private static void updateRecentPrefs() {
		if ( focusedDbGui == null )
			return;
		DbFrame frame = (DbFrame) focusedDbGui.getFrame();
		if ( frame.filename == null || NO_DB_NAME.equals(frame.filename) )
			return;
		Prefs.set(Prefs.RECENT, frame.filename);
		Point loc = frame.getLocationOnScreen();
		if ( loc != null )
			Prefs.setRectangle(Prefs.MAIN_RECT, new Rectangle(loc, frame.getSize()));
	}
	
	/** Closes all open databases and quits the application. */
	public static void quit() {
		updateRecentPrefs();
		// save and close every database:
		for ( Iterator iter = openDbGuis.values().iterator(); iter.hasNext(); ) {
			DbGui dbgui = (DbGui) iter.next();
			JFrame frame = dbgui.getFrame();
			dbgui.saveDatabase();
			frame.dispose();
		}
		System.exit(0);
	}
	
	/** Saves all open databases. */
	public static void saveAll() {
		for ( Iterator iter = openDbGuis.values().iterator(); iter.hasNext(); ) {
			DbGui dbgui = (DbGui) iter.next();
			dbgui.saveDatabase();
		}
	}
	
	public static JFrame getFocusedFrame() {
		return focusedDbGui != null ? focusedDbGui.getFrame() : null;
	}
	
	/** Closes the focused database. */
	public static void close() throws Exception {
		if ( focusedDbGui == null )
			return;
		save();
		DbFrame frame = (DbFrame) focusedDbGui.getFrame();
		String filename = frame.filename;
		if ( NO_DB_NAME.equals(filename) ) {
			if ( numOpen() <= 1 )
				System.exit(0);
			else {
				focusedDbGui = null;
				openDbGuis.remove(filename);
				frame.dispose();
				return;
			}
		}
		else {
			focusedDbGui.close();
			openDbGuis.remove(filename);
			if ( numOpen() == 0 ) {  // was last window?
				openDbGuis.put(NO_DB_NAME, focusedDbGui);
				frame.filename = NO_DB_NAME;
				frame.setTitle("SAMS - " +frame.filename);
			}
			else {
				focusedDbGui = null;
				frame.dispose();
			}
		}
	}
	
	private static void _close() {
		try {
			close();
		}
		catch(Exception ex) {
			message("Error: " +ex.getMessage());
		}
	}
	
	/** Deletes the focused database. */
	public static void delete() throws Exception {
		if ( focusedDbGui == null )
			return;
		JFrame frame = focusedDbGui.getFrame();
		String filename = ((DbFrame) frame).filename;
		if ( NO_DB_NAME.equals(filename) )
			return;

		java.awt.Toolkit.getDefaultToolkit().beep();
		Object[] options = { "Yes, I am", "Cancel" };
		int sel = javax.swing.JOptionPane.showOptionDialog(
			frame,
			"\n"
			+filename+ "\n"
			+"You are about to delete this database.\n"
			+"\n"
			+"Are you sure you want to proceed?\n"
			+"\n",
			"*** WARNING ***",
			javax.swing.JOptionPane.DEFAULT_OPTION,
			javax.swing.JOptionPane.WARNING_MESSAGE,
			null,
			options, options[1]
		);
		if ( sel != 0 )
			return;

		Files.deleteDirectory(filename); // PENDING to generalize to dir or file
		if ( new File(filename).exists() )
			message(filename+ "\nSAMS could not delete this directory.\nPlease, delete it manually.");
		openDbGuis.remove(filename);
		if ( numOpen() > 0 ) {
			frame.dispose();
			focusedDbGui = null;
		}
	}
	
	public static DbGui getFocusedDbGui() { 
		return focusedDbGui;
	}
	
	/** Saves the focused database. */
	public static void save() throws Exception {
		if ( focusedDbGui == null )
			return;
		DbFrame frame = (DbFrame) focusedDbGui.getFrame();
		String filename = frame.filename;
		if ( NO_DB_NAME.equals(filename) )
			return;
		focusedDbGui.getDatabase().save();
		message(focusedDbGui.getDatabase().getInfo()+ "\nDatabase saved.");
	}
	
	/** Number of open databases. */
	private static int numOpen() {
		return openDbGuis.size();
	}
	
	private static void _focusedDbGuiChanged(DbGui newFocusedDbGui){
		if ( newFocusedDbGui != null ) {
			focusedDbGui = newFocusedDbGui;
			updateRecentPrefs();
		}
	}

	public static void editMetadataDefinition() throws Exception {
		if ( focusedDbGui == null )
			return;
		DbFrame dbframe = (DbFrame) focusedDbGui.getFrame();
		String filename = dbframe.filename;
		if ( NO_DB_NAME.equals(filename) )
			return;
		
		new EditMetadataDefinition(dbframe, focusedDbGui.getDatabase()) {
			protected String dataOk4new(String attr_name, String attr_defval) {
				if ( attr_name.trim().length() == 0 )
					return "Missing name";
				ISamsDb db = focusedDbGui.getDatabase();
				if ( db.getMetadata().get(attr_name) != null )
					return "Duplicate name";
				return null;
			}
			protected boolean addNew(String attr_name, String attr_defval) {
				ISamsDb db = focusedDbGui.getDatabase();
				db.getMetadata().add(attr_name, attr_defval);
				focusedDbGui.metadataUpdated();
				return true;
			}
			protected boolean delete(String attr_name) {
				if ( attr_name.equals("location") || attr_name.equals("name") ) {
					SamsGui.message(frame, attr_name+ ": This attribute cannot be deleted");
					return false;
				}
				if ( SamsGui.confirm(frame, attr_name+ ": Are you sure you want to delete this attribute?") ) {
					ISamsDb db = focusedDbGui.getDatabase();
					db.getMetadata().delete(attr_name);
					focusedDbGui.metadataUpdated();
					return true;
				}
				return false;
			}
		};
	}
	
	public static void importFiles() {
		if ( focusedDbGui == null )
			return;
		DbFrame dbframe = (DbFrame) focusedDbGui.getFrame();
		String filename = dbframe.filename;
		if ( NO_DB_NAME.equals(filename) )
			return;
		
		Importer.importFilesFromDirectory(focusedDbGui);
	}
	
	public static void importAscii() {
		if ( focusedDbGui == null )
			return;
		DbFrame dbframe = (DbFrame) focusedDbGui.getFrame();
		String filename = dbframe.filename;
		if ( NO_DB_NAME.equals(filename) )
			return;
		
		Importer.importSignaturesFromAsciiFile(focusedDbGui);
	}
	
	private static class DbFrame extends JFrame {
		String filename;
		DbGui dbgui;
		JMenu windowMenu;
	
		/** Constructor with no initialization. */
		DbFrame() { 
			super();
		}
		
		/** Constructor with initialization. */
		DbFrame(Rectangle rect) {
			this();
			init(rect);
		}
		
		void init(Rectangle rect) {
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent _) {
					_close();
				}
			});
			addWindowFocusListener(new WindowFocusListener() {
				public void windowGainedFocus(WindowEvent ev) {
					_focusedDbGuiChanged(dbgui);
				}
				public void windowLostFocus(WindowEvent _) {}
			});
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			URL url = getClass().getClassLoader().getResource("img/icon.jpg");
			if ( url != null ) 
				setIconImage(new ImageIcon(url).getImage());

			if ( rect != null ) {
				setLocation(rect.x, rect.y);
				setSize(rect.width, rect.height);
			}
		}
	}
	
	
	private static void _setLookAndFeel() {
		try {
			javax.swing.UIManager.setLookAndFeel("com.incors.plaf.kunststoff.KunststoffLookAndFeel");
		}
		catch(Exception e){
			System.out.println("KunststoffLookAndFeel not found: " +e.getMessage());
		}
	}
	
	private static void _initSystemMonitoring() {
		AWTEventListener listener = new AWTEventListener() {
			int keyId = KeyEvent.KEY_PRESSED;
			int keyCode = KeyEvent.VK_Z;
			int keyModifiers = (InputEvent.CTRL_MASK | InputEvent.ALT_MASK);
			public void eventDispatched(AWTEvent event) {
				KeyEvent ke = (KeyEvent)event;
				if ((ke.getID() == keyId) && (ke.getKeyCode() == keyCode) && (ke.getModifiers() == keyModifiers))
					System.out.println("freeMemory=" +Runtime.getRuntime().freeMemory());
			};
		};
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		toolkit.addAWTEventListener(listener, AWTEvent.KEY_EVENT_MASK);
	}		

	public static boolean confirm(String msg) {
		return confirm(getFocusedFrame(), msg);
	}
	public static boolean confirm(Component comp, String msg) {
		int sel = JOptionPane.showConfirmDialog(
			comp,
			msg,
			"Confirm",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE
		);
		return sel == 0;
	}
	
	public static void message(String msg) {
		message(getFocusedFrame(), msg);
	}
	public static void message(Component comp, String msg) {
		JOptionPane.showMessageDialog(
			comp,
			msg,
			"Message",
			JOptionPane.INFORMATION_MESSAGE
		);
	}

	private static String _getCanonicalPath(String filename) {
		if ( filename == null || filename.trim().length() == 0 )
			return NO_DB_NAME;
		File file = new File(filename);
		try {
			return file.getCanonicalPath();
		}
		catch(IOException ex) {
			return file.getPath();
		}
	}

	private static Rectangle _nextRectangle() {
		Rectangle rect;
		if ( focusedDbGui != null ) {
			JFrame base_frame = focusedDbGui.getFrame();
			rect = new Rectangle(base_frame.getLocation(), base_frame.getSize());
			rect.translate(16, 16);
		}
		else
			rect = Prefs.getRectangle(Prefs.MAIN_RECT);
		return rect;
	}

	public static Border createTitledBorder(String title) {
		return BorderFactory.createTitledBorder(title);
	}

	public static void showLegendsWindow() {
		if ( focusedDbGui != null )
			focusedDbGui.showLegendsWindow();
	}
	public static void clearPlot() {
		if ( focusedDbGui != null )
			focusedDbGui.clearPlot();
	}
	public static void printPlot() {
		if ( focusedDbGui != null )
			focusedDbGui.printPlot();
	}
	public static void formatPlot() {
		if ( focusedDbGui != null )
			focusedDbGui.formatPlot();
	}
	public static void plotSelectedSignatures(boolean b) {
		if ( focusedDbGui != null )
			focusedDbGui.plotSelectedSignatures(b);
	}

	public static void setAsReference() {
		if ( focusedDbGui != null )
			focusedDbGui.setAsReference();
	}

	public static void compute(String opername) throws Exception {
		if ( focusedDbGui != null )
			focusedDbGui.compute(opername);
	}

}
