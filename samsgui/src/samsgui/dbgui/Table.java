package samsgui.dbgui;

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
import java.util.*;

/**
 * Spectra table.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Table extends JPanel {
	ISamsDb db;
	List /*ISpectrum*/ spectrums; // for TableModel
	TableModel tableModel;
	JTable table;
	ControlPanel controlPanel;

	/** Creates the Spectra panel. */
	public Table() {
		super(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Spectra table"));
		add(controlPanel = new ControlPanel(), BorderLayout.NORTH);
		add(new JLabel("No database"), BorderLayout.CENTER);
	}

	/** Update the meta data. */
	public void updateMetadata() {
		if ( tableModel != null )
			tableModel.updateMetadata();
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
			add(new JLabel(ex.getMessage()), BorderLayout.CENTER);
			return;
		}

		table = new JTable(tableModel);
		table.setPreferredScrollableViewportSize(new Dimension(400, 200));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSelectionAllowed(true);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(controlPanel, BorderLayout.NORTH);
		
		tableModel.addTableModelListener(
			new TableModelListener() {
				public void tableChanged(TableModelEvent e) {
					int row = e.getFirstRow();
					int column = e.getColumn();
					String columnName = tableModel.getColumnName(column);
					Object data = tableModel.getValueAt(row, column);

					// Do something with the data...
					System.out.println(data);
				}
			}
		);
	}

	private class TableModel extends AbstractTableModel {
		ISamsDb db;
		IMetadataDef metadata;
		IAttributeDef[] attributes;
	
		TableModel(ISamsDb db)
		throws Exception
		{
			this.db = db;
			metadata = db.getMetadata();
			_updateMetadata();
	
			spectrums = controlPanel.selectRows();
			for ( Iterator it = db.getSpectrumIterator(); it.hasNext(); ) {
				ISpectrum s = (ISpectrum) it.next();
				spectrums.add(s);
			}
		}
	
		private void _updateMetadata() throws Exception {
			metadata = db.getMetadata();
			List defs = metadata.getDefinitions();
			attributes = (IAttributeDef[]) defs.toArray(new IAttributeDef[defs.size()]);
		}
		
		/**  Update the meta data. */
		public void updateMetadata() {
			try {
				_updateMetadata();
				fireTableStructureChanged();
			}
			catch ( Exception ex ) {
			}
		}
	
		public int getColumnCount() {
			return attributes.length;
		}
	
		public String getColumnName(int columnIndex) {
			try {
				return attributes[columnIndex].getName();
			}
			catch (ArrayIndexOutOfBoundsException ex) {
				// Java bug?
				return "Java bug: "+"ArrayIndexOutOfBoundsException: col=" +columnIndex;
			}
		}
	
		public int getRowCount() {
			return spectrums.size();
		}
	
		public Object getValueAt(int row, int col) {
			try {
				String cn = attributes[col].getName();
				String val = ((ISpectrum) spectrums.get(row)).getString(cn);
				return val;
			}
			catch (ArrayIndexOutOfBoundsException ex) {
				// Java bug?
				return "Java bug: " +"ArrayIndexOutOfBoundsException: row,col=" +row+ "," +col;
			}
		}
	
		public boolean isCellEditable(int row, int col) {
			return attributes[col].isEditable();
		}
	
		public void setValueAt(Object val, int row, int col) {
			ISpectrum s = (ISpectrum) spectrums.get(row);
			s.setString(attributes[col].getName(), (String) val);
		}
	}

	private class ControlPanel extends JPanel {
		ICondition condition = null;
		String orderBy = "location,name";
		
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
		
		void sort(String orderBy) throws Exception {
			spectrums = _selectRows(condition, orderBy);
			this.orderBy = orderBy;
			tableModel.fireTableDataChanged();
		}
		
		void filter(String cond_txt) throws Exception {
			ICondition condition = db.createCondition(cond_txt);
			spectrums = _selectRows(condition, orderBy);
			this.condition = condition;
			tableModel.fireTableDataChanged();
		}
		
		List selectRows() throws Exception {
			return _selectRows(condition, orderBy);
		}
		
		private List _selectRows(ICondition condition, String orderBy) throws Exception {
			List tmp = new ArrayList();
			for ( Iterator it = db.select(condition, orderBy); it.hasNext(); ) 
				tmp.add(it.next());
			return tmp;
		}
		
	
	}
}
