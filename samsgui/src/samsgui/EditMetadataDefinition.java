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
class EditMetadataDefinition {
	JDialog frame;
	ISamsDb db;
	TableModel tableModel;
	JTable table;
	int selectedRow;
	
	EditMetadataDefinition(JFrame parentFrame, ISamsDb db) {
		this.db = db;
		frame = new JDialog(parentFrame, "Spectrum metadata structure", true);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createTitledBorder("Attribute definitions"));
		frame.setContentPane(cp);
	}
	
	void edit() throws Exception {
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
		b = new JButton("Add attribute");
		b.setMnemonic(KeyEvent.VK_A);
		b.setActionCommand("add-attribute");
		b.addActionListener(lis);
		buttons.add(b);
		b = new JButton("Delete attribute");
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
		frame.setLocation(300, 300);
		frame.setVisible(true);
	}
		
		
	class BListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				String cmd = e.getActionCommand();
				if ( cmd.equals("add-attribute") )
					add_attribute();
				else if ( cmd.equals("delete-attribute") )
					tableModel.delete_attribute();
				else // "close"
					EditMetadataDefinition.this.frame.dispose();
			}
			catch ( Exception ex ) {
				SamsGui.message("Error: " +ex.getMessage());
			}
		}

		JDialog frame;
		//Form form;

		private void accept_attribute() {
			frame.dispose();
			/*String name = form.stringValue("atr-name");
			String type = form.stringValue("atr-type");
			String defaultValue = form.stringValue("atr-defaultValue");
			if ( DBUtil.addSpectrumAttribute(db, name, type, defaultValue) ) {
				int lastRow = table.getRowCount() -1;
				tableModel.fireTableRowsInserted(lastRow, lastRow);
			}*/
		}
		private void cancel_attribute() {
			frame.dispose();
		}
		
		private void add_attribute() {
			/*form = createNewAttributeForm();
			form.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent ev) {
					if ( ev.getKeyChar() == KeyEvent.VK_ENTER )
						accept_attribute();
					else if ( ev.getKeyChar() == KeyEvent.VK_ESCAPE )
						cancel_attribute();
				}
			});			
			frame = new JDialog(EditSpectrumStructureAction.this.frame, "Spectrum structure", true);
			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

			frame.getContentPane().add(form);

			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
			frame.getContentPane().add(buttons, "South");
			JButton b;
			ActionListener lis = new BListener();
			b = new JButton("Add");
			b.setMnemonic(KeyEvent.VK_ENTER);
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					accept_attribute();
				}
			});
			buttons.add(b);
			b = new JButton("Cancel");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					cancel_attribute();
				}
			});
			buttons.add(b);
			frame.pack();
			frame.setLocation(300, 300);
			frame.setVisible(true);*/
		}
	}
	
	class TableModel extends AbstractTableModel {
		final String[] colnames = { "Name", "Default" };
		IMetadataDef metadata;

		TableModel() throws Exception {
			metadata = db.getMetadata();
		}

		public int getColumnCount() {
			return colnames.length;
		}

		public String getColumnName(int columnIndex) {
			return colnames[columnIndex];
		}

		public int getRowCount() {
			return metadata.getNumDefinitions();
		}

		public Object getValueAt(int row, int col) {
			try {
				IAttributeDef attribute = (IAttributeDef) metadata.getDefinitions().get(row);
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
			/*if ( selectedRow < 0 )
				return;

			java.awt.Toolkit.getDefaultToolkit().beep();

			IAttribute[] attributes = metadata.getSpectrumAttributes();
			IAttribute attribute = attributes[selectedRow];
			String name = attribute.getName();
			if ( name.equals("SID") ) {
				JOptionPane.showMessageDialog(
					gui.getMainFrame(),
					name+ ": This attribute cannot be deleted",
					"Information",
					JOptionPane.INFORMATION_MESSAGE
				);
				return;
			}

			int sel = JOptionPane.showConfirmDialog(
				gui.getMainFrame(),
				name+ ": Are you sure you want to delete this attribute?",
				"Warning",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
			);
			if ( sel != 0 )
			{
				return;
			}

			if ( DBUtil.deleteSpectrumAttribute(db, name) )
			{
				tableModel.fireTableRowsDeleted(selectedRow, selectedRow);
			}*/
		}
	}

	/*private static Form createNewAttributeForm() {
		Form form = new Form();
		form.setBorder(BorderFactory.createTitledBorder("New attribute"));
		
		form.addLine("atr-name", "Name", "");
		form.addChoice("atr-type", "Type", 
			AttributeTypes.TYPES,
			AttributeTypes.TYPES[0] 
		);
		form.setTextWidth(3);
		form.addLine("atr-defaultValue", "Default value", "");

		return form;
	}*/
}	
