package samsgui.dbgui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

class StatusBar extends JPanel {
	private static String[] statusLabels = {
		"Signature selection",
		"Reference signature",
		"Focused",
		"Group selection",
	};
	private static String[] statusTooltips = {
		"Shows which signature or how many signatures are currently selected",
		"This signature has a special meaning in reference-based operations",
		"You can make this signature the 'reference' one by typing Alt-Enter",
		"",
	};
	private JTextField[] statusFields;
	
	
	StatusBar() {
		super(new GridLayout(1, statusLabels.length));
		statusFields = new JTextField[statusLabels.length];
		for ( int i = 0; i < statusLabels.length; i++ ) {
			statusFields[i] = new JTextField("None");
			statusFields[i].setToolTipText(statusTooltips[i]);
			statusFields[i].setBorder(BorderFactory.createTitledBorder(statusLabels[i]));
			statusFields[i].setEditable(false);
			add(statusFields[i]);
		}
	}

	void updateStatusInfo(String[] msgs) {
		for ( int i = 0; i < statusFields.length && i < msgs.length; i++ ) {
			if ( !statusFields[i].getText().equals(msgs[i]) )
				statusFields[i].setText(msgs[i]);
		}
	}
}
