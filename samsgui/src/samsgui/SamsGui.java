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
	// Names of resources loaded via a classloader:
	private static final String INFO_PROPS_FILENAME = "samsgui/info.properties";
	private static final String LICENSE_FILENAME = "samsgui/copyright.txt";
	private static final String SPLASH_FILENAME = "samsgui/img/splash.png";
	
	private static final String NO_DB_NAME = "<No database>";
	
	static Info info = null;

	static DbGui focusedDbGui;
	
	/** Mapping from filename -> DbGui */
	static Map openDbGuis;
	
	public static void init(String operdirname) throws Exception {
		info = new Info();
		if ( operdirname == null ) {
			operdirname = info.getSAMSDirectory()+ "/opers";
			if ( !new File(operdirname).isDirectory() ) {
				System.err.println(operdirname+ ": inexistent directory");
				operdirname = null;
			}
		}
		nextFrame = new DbFrame();
		Splash splash = Splash.showSplash(nextFrame);
		Sams.init(operdirname);
		openDbGuis = new HashMap();
		_initSystemMonitoring();
		_setLookAndFeel();
		String filename = Prefs.get(Prefs.RECENT);
		if ( filename.length() == 0 || filename.equals(NO_DB_NAME) ||  !new File(filename).exists() )
			filename = null;
		if ( filename != null )
			splash.status("Opening recent database '" +new File(filename).getName()+ "'...");
		open(filename);
		splash.status(null);
		ToolTipManager ttman = ToolTipManager.sharedInstance();
		ttman.setDismissDelay(60*1000);
	}

	public static Info getInfo() {
		return info;
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
			frame = _getNextFrame();
			frame.init(_nextRectangle());
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
				frame = _getNextFrame();
				frame.init(_nextRectangle());
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
			frame = _getNextFrame();
			frame.init(rect);
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
		Prefs.updateRectangle(Prefs.MAIN_RECT, frame);
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
		DbFrame frame = (DbFrame) focusedDbGui.getFrame();
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
				if ( !attr_name.matches("\\w+") )
					return "Invalid name";
				if ( attr_name.equalsIgnoreCase("location") || attr_name.equalsIgnoreCase("name") )
					return "Reserved name";
				ISamsDb db = focusedDbGui.getDatabase();
				if ( db.getMetadata().get(attr_name) != null )
					return "Duplicate name";
				return null;
			}
			protected boolean addNew(String attr_name, String attr_defval) {
				ISamsDb db = focusedDbGui.getDatabase();
				db.getMetadata().add(attr_name, attr_defval);
				focusedDbGui.metadataAttributeAdded(attr_name);
				return true;
			}
			protected boolean delete(String attr_name) {
				if ( SamsGui.confirm(frame, attr_name+ ": Are you sure you want to delete this attribute?") ) {
					ISamsDb db = focusedDbGui.getDatabase();
					db.getMetadata().delete(attr_name);
					focusedDbGui.metadataAttributeDeleted(attr_name);
					return true;
				}
				return false;
			}
		};
	}
	
	public static void importFilesFromDirectory() {
		if ( focusedDbGui == null )
			return;
		DbFrame dbframe = (DbFrame) focusedDbGui.getFrame();
		if ( !NO_DB_NAME.equals(dbframe.filename) )
			Importer.importFilesFromDirectory(focusedDbGui);
	}
	
	public static void importFiles() {
		if ( focusedDbGui == null )
			return;
		DbFrame dbframe = (DbFrame) focusedDbGui.getFrame();
		String filename = dbframe.filename;
		if ( !NO_DB_NAME.equals(dbframe.filename) )
			Importer.importFiles(focusedDbGui);
	}
	
	public static void importAscii() {
		if ( focusedDbGui == null )
			return;
		DbFrame dbframe = (DbFrame) focusedDbGui.getFrame();
		if ( !NO_DB_NAME.equals(dbframe.filename) )
			Importer.importSignaturesFromAsciiFile(focusedDbGui);
	}
	
	public static void importEnvi() {
		if ( focusedDbGui == null )
			return;
		DbFrame dbframe = (DbFrame) focusedDbGui.getFrame();
		if ( !NO_DB_NAME.equals(dbframe.filename) )
			Importer.importSignaturesFromEnviFile(focusedDbGui);
	}
	
	private static DbFrame nextFrame = null;
	private static DbFrame _getNextFrame() {
		DbFrame ret = nextFrame;
		nextFrame = new DbFrame();
		return ret;
	}
	
	/** instances must be created via _getNextFrame() */
	private static class DbFrame extends JFrame {
		String filename;
		DbGui dbgui;
		JMenu windowMenu;
	
		/** Constructor with no initialization. */
		DbFrame() { 
			super();
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

	/** Shows the "About" message. */
	public static void showAboutMessage() {
		String about = info.getAboutMessage();
		String license = info.getLicense();
		String msg = about+ "\n" +license;
		JFrame parent = focusedDbGui == null ? null : focusedDbGui.getFrame();
        final JDialog window = new JDialog(parent, "About", true); 
		JTabbedPane tabs = new JTabbedPane();
		window.getContentPane().add(tabs, BorderLayout.CENTER);
		JButton ok = new JButton("OK");
		window.getContentPane().add(ok, BorderLayout.SOUTH);
		JLabel about_l = new JLabel(about, info.getIcon(SPLASH_FILENAME), JLabel.CENTER);
		about_l.setVerticalTextPosition(JLabel.BOTTOM);
		about_l.setHorizontalTextPosition(JLabel.CENTER);
		tabs.addTab("About", about_l);
		JTextArea ta = new JTextArea(license);
		ta.setEditable(false);
		ta.setFont(new Font("monospaced", 0, 12));
		tabs.addTab("Copyright", new JScrollPane(ta));
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				window.setVisible(false);
			}
		});
		window.pack();
		if ( parent != null )
			window.setLocationRelativeTo(parent);
		ok.requestFocus();
		window.setVisible(true);
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

	public static void reloadOperations() {
		if ( focusedDbGui != null )
			focusedDbGui.reloadOperations();
	}

	public static void compute(String opername) throws Exception {
		if ( focusedDbGui != null )
			focusedDbGui.compute(opername);
	}

	public static void export(String format) throws Exception {
		if ( focusedDbGui != null )
			focusedDbGui.export(format);
	}

	public static void viewData() {
		if ( focusedDbGui != null )
			focusedDbGui.viewData();
	}
	
	public static void rename() throws Exception {
		if ( focusedDbGui != null )
			focusedDbGui.rename();
	}

	public static void createGroup() throws Exception {
		if ( focusedDbGui != null )
			focusedDbGui.createGroup();
	}

	public static void refreshGrouping() throws Exception {
		if ( focusedDbGui != null )
			focusedDbGui.refreshGrouping();
	}

	/** (Re)creates a grouping. */
	public static void createGroupingBy() throws Exception {
		if ( focusedDbGui != null )
			focusedDbGui.createGroupingBy();
	}

	/** Splash Window. */
	static class Splash extends JWindow {
		private JLabel status_label;
		
		/**
		 * Writes a status message. If the argument is null, then the
		 * splash window will be closed in at most one second approx.
		 */
		public void status(String status_text) {
			if ( status_text != null )
				status_label.setText(status_text);
			else
				new Thread(getWaitRunner(1000)).start();
		}
		 
		/** Creates a splash window. */
		private Splash(String text, Frame f) {
			super(f);
			final Color bg_color = null;//new Color(210,255,255);
			setContentPane(new JPanel(new BorderLayout()));
			((JPanel) getContentPane()).setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			getContentPane().setBackground(bg_color);
			ImageIcon ii = info.getIcon(SPLASH_FILENAME);
			MouseListener ml = new MouseAdapter()  {
				public void mousePressed(MouseEvent e) {
					setVisible(false);
					dispose();
				}
			};
			addMouseListener(ml);
			
			JLabel label = new JLabel(ii, JLabel.CENTER);
			label.setIconTextGap(100);
			getContentPane().add(label, BorderLayout.CENTER);
	
			JPanel fields = new JPanel(new GridLayout(2, 1)); 
			getContentPane().add(fields, BorderLayout.SOUTH);
			
			JLabel text_label = new JLabel(text);
			text_label.setFont(text_label.getFont().deriveFont(Font.BOLD));
			text_label.setHorizontalAlignment(JLabel.CENTER);
			text_label.setBackground(bg_color);
			text_label.setOpaque(true);
			text_label.addMouseListener(ml);
			fields.add(text_label);
			
			status_label = new JLabel("Starting...", JLabel.CENTER);
			status_label.setFont(status_label.getFont().deriveFont(10f));
			status_label.setHorizontalAlignment(JLabel.CENTER);
			status_label.setBackground(bg_color);
			status_label.setOpaque(true);
			status_label.addMouseListener(ml);
			fields.add(status_label);
	
			pack();
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension labelSize = label.getPreferredSize();
			int locy = (screenSize.height - labelSize.height)/2 - 150;
			if ( locy < 0 )
				locy = 0;
			setLocation( (screenSize.width - labelSize.width)/2, locy);
			Runnable waitRunner = getWaitRunner(30*1000); 
			setVisible(true);
			new Thread(waitRunner, "SplashThread").start();
		}
	
		private Runnable getWaitRunner(final int pause) {
			final Runnable closerRunner = new Runnable() {
				public void run() {
					setVisible(false);
					dispose();
				}
			};
			Runnable waitRunner = new Runnable() {
				public void run() {
					try {
						Thread.sleep(pause);
						SwingUtilities.invokeAndWait(closerRunner);
					}
					catch(Exception e) {
					}
				}
			};
			return waitRunner;
		}
		
		/** Displays the splash window. */
		static Splash showSplash(JFrame frame) {
			return new Splash("Version " +info.getVersion()+ " (" +info.getBuild()+ ")", frame);
		}
	}

	/** Application properties. */
	static class Info {
		// default values
		String name = "SAMS";
		String version = "v";
		String build = "b";
		
		String licenseText = null;  // License text (loaded on demand):
		
		/** Loads the properties. */
		Info()  {
			Properties props = new Properties(); 
			InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(INFO_PROPS_FILENAME);
			if ( is == null ) {
				System.err.println(
					"!!!!!! Resource " +INFO_PROPS_FILENAME+ " not found.\n" +
					"!!!!!! SAMS has not been properly compiled.\n"+
					"!!!!!! Continuing with default values."
				);
			}
			else {
				try {
					props.load(is);
					is.close();
				}
				catch(IOException ex) { 
					// ignore. 
				}
				name    = props.getProperty("sams.name");
				version = props.getProperty("sams.version");
				build   = props.getProperty("sams.build");
			}
		}

		public ImageIcon getIcon(String filename) {
			ImageIcon icon = null;
			java.net.URL url = ClassLoader.getSystemClassLoader().getResource(filename);
			if ( url != null )
				icon = new ImageIcon(url);
			return icon;
		}
	
		public String getVersion() {
			return version;
		}
		
		public String getBuild() {
			return build;
		}
		
		public String getSAMSDirectory() {
			return System.getProperty("sams.dir");
		}
		
		public String getLicense() {
			if ( licenseText == null ) {
				InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(LICENSE_FILENAME);
				StringBuffer sb = new StringBuffer(); 
				if ( is == null ) {
					sb.append(
						"!!!!!! Resource " +LICENSE_FILENAME+ " not found.\n" +
						"!!!!!! SAMS has not been properly compiled.\n"
					);
				}
				else {
					try {
						BufferedReader br = new BufferedReader(new InputStreamReader(is));
						String line;
						while ( (line = br.readLine()) != null )
							sb.append(line+ "\n");
					}
					catch(IOException ex) {
						sb.append(ex.getClass().getName()+ " : " +ex.getMessage()+ "\n");
					}
				}
				licenseText = sb.toString();
			}
			return licenseText;
		}
		
		public String getAboutMessage() {
			return
				"<html>\n" + "<div align='center'>\n"+
				"<br>\n"+
				"<b>SAMS - Spectral Analysis and Management System</b><br>\n"+
				"Version " +getVersion()+ " (Build " +getBuild()+ ")<br>\n"+
				"<br>\n"+
				"<code>http://www.cstars.ucdavis.edu/software/sams/</code><br>\n"+
				"<br>\n"+
				"Center for Spatial Technologies and Remote Sensing<br>\n"+
				"Department of Land, Air, and Water Resources<br>\n"+
				"University of California, Davis<br>\n"+
				"<br>\n"+
				"<br>\n"+
				"Please read the copyright notice.\n"+
				"</div>\n" + "</html>\n"
			;
		}
	}
}
