package samsgui.dbgui;

import samsgui.SamsGui;
import samsgui.Controller;
import samsgui.BaseDialog;

import samscore.ISamsDb;
import samscore.ISamsDb.ISpectrum;
import samscore.Sams;
import samscore.SamsDbManager;
import samscore.SamsDbManager.ExportListener;
import sig.Signature;
import sigoper.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.io.*;


/**
 * Interacts with the user to export data.
 * @author Carlos A. Rueda
 * @version $Id$
 */ 
public class Exporter {
	static final int ONE_SECOND = 1000;
	private DbGui dbgui;
	private List paths;
	String format;
	private StringBuffer info;
	
	public Exporter(DbGui dbgui, List paths, String format, StringBuffer info) {
		this.dbgui = dbgui;
		this.paths = paths;
		this.format = format;
		this.info = info;
		new ExportPaths().go();
	}

	class ExportPaths {
		StringBuffer task_message = new StringBuffer();
		boolean task_isDone;
		Timer timer;

		void go() {
			JFrame frame = dbgui.getFrame();
			final ISamsDb db = dbgui.getDatabase();
			assert db != null;
			
			JPanel p_file = new JPanel(new FlowLayout(FlowLayout.LEFT));
			final JTextField f_file = new JTextField(32);
			p_file.setBorder(SamsGui.createTitledBorder("Destination file"));
			p_file.add(f_file);
			final JButton b_choose = new JButton("Choose");
			p_file.add(b_choose);
			b_choose.setMnemonic(KeyEvent.VK_C);
			b_choose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					String filename = Controller.Dialogs.selectExportFile("Select a file to export");
					if ( filename != null )
						f_file.setText(filename);
				}
			});
			final JRadioButton r_header_no = new JRadioButton("No header");
			final JRadioButton r_header_name = new JRadioButton("Name");
			final JRadioButton r_header_loc_name = new JRadioButton("Location/Name");
			ButtonGroup group_header = new ButtonGroup();
			group_header.add(r_header_no);
			group_header.add(r_header_name);
			group_header.add(r_header_loc_name);
			
			final JLabel status = new JLabel();
			status.setFont(status.getFont().deriveFont(Font.ITALIC));
			final JProgressBar progressBar = new JProgressBar(0, 1000);
			progressBar.setValue(0);
			progressBar.setStringPainted(true); //get space for the string
			progressBar.setString("");          //but don't paint it
			progressBar.setEnabled(false);
			final JTextArea taskOutput = new JTextArea(5, 30);
			taskOutput.setBackground(null);
			taskOutput.setMargin(new Insets(5,5,5,5));
			taskOutput.setEditable(false);
			taskOutput.setEnabled(false);
			
			
			// timer.
			timer = new Timer(ONE_SECOND, new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					String s = task_message.toString();
					task_message.setLength(0);
					if ( s.length() > 0 ) {
						String last = s.substring(s.lastIndexOf('\n') + 1); 
						taskOutput.append(s);
						taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
					}
						
					if ( task_isDone ) {
						Toolkit.getDefaultToolkit().beep();
						timer.stop();
					}
				}
			});
			
			List array = new ArrayList();
			if ( info != null ) {
				JLabel l_info = new JLabel(info.toString());
				l_info.setForeground(Color.gray);
				array.add(l_info);
			}
			array.add(new JLabel("Number of signatures: " +paths.size()));
			array.add(p_file);
			array.add(status);
			array.add(progressBar);
			array.add(new JScrollPane(taskOutput));
			
			String diag_title = "Export spectra files";
			final BaseDialog form = new BaseDialog(frame, diag_title, array.toArray()) {
				public boolean dataOk() {
					String msg = null;
					String filename = f_file.getText();
					if ( filename.trim().length() == 0 )
						msg = "Please specify a file";
					else if ( new File(filename.trim()).isDirectory() )
						msg = "Not a valid file (it's a directory)";
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
				
				public void notifyUpdate(String comp_name, Object value) {
					if ( !timer.isRunning() )
						super.notifyUpdate(comp_name, value);
				}
				
				public boolean preAccept() {
					if ( task_isDone )
						return true;
					
					if ( !dataOk() )
						return false;
					
					final String filename = f_file.getText();
					
					// do exportation:
					Thread thread = new Thread(new Runnable() {
						public void run() {
							Controller.doUpdate(new Runnable() {
								public void run() {
									b_choose.setEnabled(false);
									f_file.setEditable(false);
									
									btnAccept.setEnabled(false);
									btnCancel.setEnabled(false);
									progressBar.setEnabled(true);
									taskOutput.setEnabled(true);
								}
							});
	
							PrintWriter writer = new PrintWriter(System.out, true);
							SamsDbManager dbman = new SamsDbManager(db, writer);
							try {
								if ( format.equals("ascii") ) {
									dbman.exportAscii(paths, filename, new ExportListener() {	
										public void exporting(final int file_number, final String relative_filename) {
											task_message.append("(" +file_number+ ") " +relative_filename+ "\n");
											progressBar.setValue(file_number);
										}
									});
								}
								else if ( format.equals("envi") ) {
									dbman.exportEnvi(paths, filename, new ExportListener() {	
										public void exporting(final int file_number, final String relative_filename) {
											task_message.append("(" +file_number+ ") " +relative_filename+ "\n");
											progressBar.setValue(file_number);
										}
									});
								}
								else if ( format.equals("envi-sl") ) {
									dbman.exportEnviLibrary(paths, filename, new ExportListener() {	
										public void exporting(final int file_number, final String relative_filename) {
											task_message.append("(" +file_number+ ") " +relative_filename+ "\n");
											progressBar.setValue(file_number);
										}
									});
								}
								else
									throw new Error("Unexpected format: " +format);
								
								progressBar.setValue(progressBar.getMaximum());
								
								task_message.append("\nDone");
							}
							catch(Exception ex) {
								task_message.append("\nError: " +ex.getMessage());
							}
							task_isDone = true;
							btnAccept.setText("Close");
							btnAccept.setEnabled(true);
							btnCancel.setEnabled(true);
						}
					});
					
					progressBar.setMaximum(paths.size() + 1);
					progressBar.setIndeterminate(false);
					progressBar.setString(null); //display % string
					task_isDone = false;
					thread.start();
					timer.start();
					return false;
				}
			};
			form.activate();
			form.pack();
			form.setLocationRelativeTo(frame);
			form.setVisible(true);
		}
	}
	
	class ExportSelectedGroups {
		void go() {
			// PENDING
		}
	}
}
