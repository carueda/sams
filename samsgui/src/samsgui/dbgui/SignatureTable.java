package samsgui.dbgui;

import samscore.ISamsDb;
import sig.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.*;

/**
 * Table to show/edit the contents of a signature.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public abstract class SignatureTable extends JPanel {
	protected Signature sig;
	protected TableModel tableModel;
	protected JTable jtable;
	protected JButton buttonSave;
	protected ControlPanel controlPanel;

	/** Creates a signature table. */
	public SignatureTable(Signature sig) throws Exception {
		super(new BorderLayout());
		this.sig = sig;
		tableModel = new TableModel();
		jtable = new JTable(tableModel);
		jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jtable.setRowSelectionAllowed(true);
		setBorder(BorderFactory.createTitledBorder("Signature data"));
		add(new JScrollPane(jtable), BorderLayout.CENTER);
		add(controlPanel = new ControlPanel(), BorderLayout.SOUTH);
		
		tableModel.addTableModelListener(
			new TableModelListener() {
				public void tableChanged(TableModelEvent e) {
					int row = e.getFirstRow();
					int column = e.getColumn();
					if ( row >= 0 && column >= 0 ) {
						String columnName = tableModel.getColumnName(column);
						Object data = tableModel.getValueAt(row, column);
						// Do something with the data... PENDING
						System.out.println(data);
					}
				}
			}
		);
	}

	// return true to indicate successful operation. */
	abstract protected boolean save();
	
	protected void modified() {
		buttonSave.setEnabled(true);
	}

	// return true to indicate successful operation. */
	protected void _save() {
		buttonSave.setEnabled(!save());
	}
	
	private class TableModel extends AbstractTableModel {
		String[] colnames = { "", "x", "y", "Info" };
	
		public int getColumnCount() {
			return colnames.length;
		}
	
		public String getColumnName(int columnIndex) {
			return colnames[columnIndex];
		}
	
		public int getRowCount() {
			return sig.getSize();
		}
	
		public Object getValueAt(int row, int col) {
			Signature.Datapoint dp = sig.getDatapoint(row);
			String val = "??";
			if ( col == 0 )
				val = String.valueOf(row+1);
			else if ( col == 1 )
				val = String.valueOf(dp.x);
			else if ( col == 2 )
				val = String.valueOf(dp.y);
			else if ( col == 3 ) {
				val = dp.obj == null ? "" : ((String) dp.obj);
			}
			return val;
		}
	
		public boolean isCellEditable(int row, int col) {
			return true;
		}
	
		public void setValueAt(Object val, int row, int col) {
			Signature.Datapoint dp = sig.getDatapoint(row);
			if ( 1 <= col && col <= 2 ) {
				try {
					double d = Double.parseDouble((String) val);
					if ( col == 1 )
						dp.x = d;
					else if ( col == 2 )
						dp.y = d;
				}
				catch(NumberFormatException ex) {
					return; // ignore
				}
			}
			else if ( col == 3 ) {
				String info = ((String) val).trim();
				if ( info.length() == 0 )
					info = null;
				dp.obj = info;
			}
			modified();
		}
	}
	
	private class ControlPanel extends JPanel {
		ControlPanel() {
			super(new FlowLayout(FlowLayout.LEFT));
			add(buttonSave = new JButton("Save"));
			buttonSave.setEnabled(false);
			buttonSave.setActionCommand("save");
			buttonSave.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String cmd = e.getActionCommand();
					if ( cmd.equals("save") )
						_save();
				}
			});
		}
	}
}
