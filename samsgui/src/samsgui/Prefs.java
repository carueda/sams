package samsgui;

import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Point;

import java.util.prefs.*;
import java.util.*;

/** 
 * SAMS system and user preferences.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Prefs {

	/** The name of the last open database. */
	public static final String RECENT = "sams.pref.recent";

	/** Last directory used to import. */
	public static final String IMPORT_DIR = "sams.pref.import.dir";

	/** Last directory used to export. */
	public static final String EXPORT_DIR = "sams.pref.export.dir";

	/** Rectangle for the main window. */
	public static final String MAIN_RECT = "sams.pref.main.rect";
	
	/** Rectangle for the plot formatter. */
	public static final String PLOT_FORMATTER_RECT = "sams.pref.plot.formatter.rect";

	/** Rectangle for "view" window. */
	public static final String VIEW_RECT = "sams.pref.view.rect";

	/** Rectangle for the help window. */
	public static final String HELP_RECT = "sams.pref.help.rect";

	public static final String PREF_LAF = "sams.pref.laf";

	private static Preferences prefs = Preferences.userRoot();
	private static Map default_rects = new HashMap();
	static {
		java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
		Dimension d = tk.getScreenSize();
		Dimension s = new Dimension(800, 700);
		Rectangle rect = new Rectangle(
				(d.width - s.width) / 2, (d.height - s.height) / 2,
				s.width, s.height
		);
		default_rects.put(MAIN_RECT, new Rectangle(
					(d.width - s.width) / 2, (d.height - s.height) / 2,
					s.width, s.height
		));
	}
	
	/** Gets a rectangle preference. */
	public static Rectangle getRectangle(String key) {
		Rectangle r = (Rectangle) default_rects.get(key);
		if ( r == null )
			throw new RuntimeException("undefined rect key: " +key);
		int x = prefs.getInt(key+"_x", r.x);
		int y = prefs.getInt(key+"_y", r.y);
		int width = prefs.getInt(key+"_width", r.width);
		int height = prefs.getInt(key+"_height", r.height);
		return new Rectangle(x, y, width, height);
	}
	
	/** Updates a rectangle preference. */
	public static void setRectangle(String key, Rectangle r) {
		if ( default_rects.get(key) == null )
			throw new RuntimeException("undefined rect key: " +key);
		prefs.putInt(key+"_x", r.x);
		prefs.putInt(key+"_y", r.y);
		prefs.putInt(key+"_width", r.width);
		prefs.putInt(key+"_height", r.height);
	}
	
	/** Sets a preference. */
	public static void set(String key, String value) {
		prefs.put(key, value);
	}

	/** Gets a preference. */
	public static String get(String key) {
		return prefs.get(key, "");
	}

	// --
	private Prefs() {}
}
