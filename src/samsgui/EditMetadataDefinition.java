package samsgui;

import samscore.Sams;
import samscore.ISamsDb;
import samscore.ISamsDb.IMetadataDef;
import samscore.ISamsDb.IMetadataDef.*;
import fileutils.Files;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import java.io.*;

/** 
 * EditMetadataDefinition
 * @author Carlos A. Rueda
 * @version $Id$ 
 */ 
abstract class EditMetadataDefinition {
	protected JDialog frame;
	protected ISamsDb db;
	protected TableModel tableModel;
	protected JTable table;
	protected int selectedRow;
	
	EditMetadataDefinition(JFrame parentFrame, ISamsDb db) throws Exception {
		this.db = db;
		frame = new JDialog(parentFrame, "Metadata structure", true);
		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(SamsGui.createTitledBorder("Attribute definitions"));
		frame.setContentPane(cp);

		tableModel = new TableModel();
		table = new JTable(tableModel);
		table.setPreferredScrollableViewportSize(new Dimension(300, 100));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		frame.getContentPane().add(new JScrollPane(table));

		ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				//Ignore extra messages.
				if (e.getValueIsAdjusting())
					return;
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				selectedRow = lsm.isSelectionEmpty() ? -1 : lsm.getMinSelectionIndex();
			}
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		frame.getContentPane().add(buttons, "South");
		JButton b;
		ActionListener lis = new BListener();
		b = new JButton("Add new attribute");
		b.setMnemonic(KeyEvent.VK_A);
		b.setActionCommand("add-attribute");
		b.addActionListener(lis);
		buttons.add(b);
		b = new JButton("Delete selected attribute");
		b.setMnemonic(KeyEvent.VK_D);
		b.setActionCommand("delete-attribute");
		b.addActionListener(lis);
		buttons.add(b);
		b = new JButton("Close");
		b.setMnemonic(KeyEvent.VK_C);
		b.setActionCommand("close");
		b.addActionListener(lis);
		buttons.add(b);
		
		frame.pack();
		frame.setLocationRelativeTo(parentFrame);
		frame.setVisible(true);
	}

	protected abstract String dataOk4new(String attr_name, String attr_defval);
	protected abstract boolean addNew(String attr_name, String attr_defval);
	protected abstract boolean delete(String attr_name);
	
	
	class BListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				String cmd = e.getActionCommand();
				if ( cmd.equals("add-attribute") ) {
					add_attribute();
					table.requestFocus();
				}
				else if ( cmd.equals("delete-attribute") ) {
					tableModel.delete_attribute();
					table.requestFocus();
				}
				else // "close"
					EditMetadataDefinition.this.frame.dispose();
			}
			catch ( Exception ex ) {
				SamsGui.message("Error: " +ex.getMessage());
			}
		}

		private void add_attribute() {
			final JTextField f_name = new JTextField(12);
			final JTextField f_defval = new JTextField(12);
			final JLabel status = new JLabel();
			status.setFont(status.getFont().deriveFont(Font.ITALIC));
			
			f_name.setBorder(SamsGui.createTitledBorder("Attribute name"));
			f_defval.setBorder(SamsGui.createTitledBorder("Default value"));
			
			Object[] array = {
				f_name,
				f_defval,
				status
			};
			
			String diag_title = "Add new attribute";
			final BaseDialog form = new BaseDialog(frame, diag_title, array) {
				public boolean dataOk() {
					String attr_name = f_name.getText();
					String attr_defval = f_defval.getText();
					String msg = EditMetadataDefinition.this.dataOk4new(attr_name, attr_defval);
					if ( msg == null ) {
						status.setForeground(Color.gray);
						status.setText("OK");
					}
					else {
						status.setForeground(Color.red);
						status.setText(msg);
					}
					return msg == null;
				}
			};
			form.activate();
			form.pack();
			form.setLocationRelativeTo(frame);
			form.setVisible(true);
			if ( form.accepted() ) {
				String attr_name = f_name.getText().trim();
				String attr_defval = f_defval.getText().trim();
				if ( EditMetadataDefinition.this.addNew(attr_name, attr_defval) ) {
					int lastRow = table.getRowCount() -1;
					tableModel.fireTableRowsInserted(lastRow, lastRow);
				}
			}
		}
	}
	
	class TableModel extends AbstractTableModel {
		final String[] colnames = { "Name", "Default value" };

		public int getColumnCount() {
			return colnames.length;
		}

		public String getColumnName(int columnIndex) {
			return colnames[columnIndex];
		}

		public int getRowCount() {
			return db.getMetadata().getNumDefinitions();
		}

		public Object getValueAt(int row, int col) {
			try {
				IAttributeDef attribute = (IAttributeDef) db.getMetadata().getDefinitions().get(row);
				switch ( col ) {
					case 0:
						return attribute.getName();
					case 1:
						return attribute.getDefaultValue();
				}
				throw new InternalError("col=" +col);
			}
			catch ( Exception ex ) {
				return ex.getMessage();
			}
		}

		private void delete_attribute() throws Exception {
			if ( selectedRow < 0 )
				return;
			java.awt.Toolkit.getDefaultToolkit().beep();
			IAttributeDef attrdef = (IAttributeDef) db.getMetadata().getDefinitions().get(selectedRow);
			String attr_name = attrdef.getName();
			if ( EditMetadataDefinition.this.delete(attr_name) )
				fireTableRowsDeleted(selectedRow, selectedRow);
		}
	}
}	
