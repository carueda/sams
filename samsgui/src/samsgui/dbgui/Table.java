package samsgui.dbgui;

import samsgui.SamsGui;
import samscore.ISamsDb;
import samscore.ISamsDb.*;
import samscore.ISamsDb.IMetadataDef.IAttributeDef;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.util.*;

/**
 * Spectra table.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public abstract class Table extends JPanel {
	ISamsDb db;
	List /*ISpectrum*/ spectrums; // for TableModel
	TableModel tableModel;
	JTable jtable;
	ICondition condition = null;
	String orderBy;
	ControlPanel controlPanel;

	/** Creates the Spectra panel. */
	public Table() {
		super(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Spectra table"));
		add(controlPanel = new ControlPanel(), BorderLayout.NORTH);
		add(new JLabel("No database"), BorderLayout.CENTER);
	}

	/** called to rename a row. */
	protected abstract ISpectrum doRenaming(ISpectrum s, String new_name_value);

	/** Update the meta data. */
	public void updateMetadata() {
		if ( tableModel != null )
			tableModel.updateMetadata();
	}

	/** refreshes the contents of this table. */
	public void updateData() {
		if ( tableModel != null )
			tableModel.updateData();
	}
	
	/** Updates the database being rendered. */
	public void setDatabase(ISamsDb db) {
		removeAll();
		this.db = db;
		if ( db == null ) {
			add(new JLabel("No database"), BorderLayout.CENTER);
			tableModel = null;
			return;
		}
		try {
			tableModel = new TableModel(db);
		}
		catch ( Exception ex ) {
			ex.printStackTrace();
			add(new JLabel(ex.getMessage()), BorderLayout.CENTER);
			return;
		}

		jtable = new JTable(tableModel);
		jtable.setPreferredScrollableViewportSize(new Dimension(400, 200));
		jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jtable.setRowSelectionAllowed(true);
		add(new JScrollPane(jtable), BorderLayout.CENTER);
		add(controlPanel, BorderLayout.NORTH);
		
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

	private class TableModel extends AbstractTableModel {
		ISamsDb db;
		IMetadataDef metadata;
		IAttributeDef[] attributes;
	
		TableModel(ISamsDb db) throws Exception {
			this.db = db;
			metadata = db.getMetadata();
			_updateMetadata();
			_updateData();
		}
	
		private void _updateMetadata() throws Exception {
			metadata = db.getMetadata();
			List defs = metadata.getDefinitions();
			attributes = (IAttributeDef[]) defs.toArray(new IAttributeDef[defs.size()]);
		}
		
		private void _updateData() throws Exception {
			spectrums = _selectRows();
		}
		
		/**  Updates the meta data. */
		void updateMetadata() {
			try {
				_updateMetadata();
				fireTableStructureChanged();
			}
			catch ( Exception ex ) {
			}
		}
		
		/**  Updates the data. */
		void updateData() {
			try {
				_updateData();
				fireTableDataChanged();
			}
			catch ( Exception ex ) {
			}
		}
	
		public int getColumnCount() {
			return 2 + attributes.length;
		}
	
		public String getColumnName(int col) {
			return col == 0 ? "Location" : 
			       col == 1 ? "Name"     : attributes[col - 2].getName()
			;
		}
	
		public int getRowCount() {
			return spectrums.size();
		}
	
		public Object getValueAt(int row, int col) {
			ISpectrum s = (ISpectrum) spectrums.get(row);
			if ( col == 0 )
				return s.getLocation();
			else if ( col == 1 )
				return s.getName();
			else 
				return s.getString(attributes[col - 2].getName());
		}
	
		public boolean isCellEditable(int row, int col) {
			if ( col == 0 )
				return false;
			return col == 1 || attributes[col - 2].isEditable();
		}
	
		public void setValueAt(Object val, int row, int col) {
			String newvalue = ((String) val).trim();
			ISpectrum s = (ISpectrum) spectrums.get(row);
			assert col > 0 ;
			if ( col == 1 ) {
				String oldvalue = s.getName();
				if ( !oldvalue.equals(newvalue) ) {
					s = doRenaming(s, newvalue);
					if ( s != null )
						spectrums.set(row, s);
				}
			}
			else { 
				String colname = attributes[col - 2].getName();
				String oldvalue = s.getString(colname);
				if ( !oldvalue.equals(newvalue) ) {
					s.setString(colname, newvalue);
					try {
						s.save();
					}
					catch(Exception ex) {
						SamsGui.message("Error trying to save new value: " +ex.getMessage());
						s.setString(colname, oldvalue);
					}
				}
			}
		}
	}

	public void sort(String orderBy) throws Exception {
		spectrums = _selectRows(condition, orderBy);
		this.orderBy = orderBy;
		if ( tableModel != null )
			tableModel.fireTableDataChanged();
	}
	
	public void filter(String cond_txt) throws Exception {
		ICondition condition = db.createCondition(cond_txt);
		spectrums = _selectRows(condition, orderBy);
		this.condition = condition;
		if ( tableModel != null )
			tableModel.fireTableDataChanged();
	}
	
	private List _selectRows() throws Exception {
		return _selectRows(condition, orderBy);
	}
	
	private List _selectRows(ICondition condition, String orderBy) throws Exception {
		List tmp = new ArrayList();
		for ( Iterator it = db.selectSpectrums(condition, orderBy); it.hasNext(); ) { 
			ISpectrum s = (ISpectrum) it.next();
			tmp.add(s);
		}
		return tmp;
	}

	private class ControlPanel extends JPanel {
		JPanel controls;
		JTextField sort_tf;
		JTextField filter_tf;
		
		ControlPanel() {
			super(new FlowLayout(FlowLayout.LEFT));
			add(new JLabel("Sort field"));
			add(sort_tf = new JTextField(8));
			add(new JLabel("Filter condition"));
			add(filter_tf = new JTextField(8));
			
			sort_tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						sort(sort_tf.getText());
					}
					catch(Exception ex) {
						System.out.println("Table.ControlPanel.sort: " +ex.getMessage());
					}
				}
			});
			filter_tf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						filter(filter_tf.getText());
					}
					catch(Exception ex) {
						System.out.println("Table.ControlPanel.filter: " +ex.getMessage());
					}
				}
			});
		}
	}
}
