package samsgui.dbgui;

import samsgui.dbgui.Tree.MyNode;
import samsgui.*;

import samscore.*;
import samscore.SamsDbManager.DirectoryImporter;
import specfile.SpectrumFileManager;
import sig.Signature;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.text.DecimalFormat;


/**
 * Importer services.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Importer {
	static final int ONE_SECOND = 1000;
	static final String guess_type = "-Any recognized type-";

	/** list of recognized file types augmented with "best guess" */
	private static String[] getFileTypes() {
		String[] realtypes = SpectrumFileManager.getFileTypes();
		String[] filetypes = new String[realtypes.length + 1];
		System.arraycopy(realtypes, 0, filetypes, 1, realtypes.length);
		filetypes[0] = guess_type;
		return filetypes;
	}
	
	/** Interacts with the user to import files from a given directory. */
	public static void importFilesFromDirectory(DbGui dbgui) {
		if ( dbgui.getDatabase() != null )
			new ImportFilesFromDirectory(dbgui).go();
	}

	static class ImportFilesFromDirectory extends BaseImport {
		ImportFilesFromDirectory(DbGui dbgui) {
			super(dbgui);
		}
		
		void go() {
			JPanel p_file = new JPanel(new FlowLayout(FlowLayout.LEFT));
			final JTextField f_file = new JTextField(32);
			p_file.setBorder(SamsGui.createTitledBorder("Directory"));
			p_file.add(f_file);
			final JButton b_choose = new JButton("Choose");
			p_file.add(b_choose);
			b_choose.setMnemonic(KeyEvent.VK_C);
			b_choose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					String dirname = Controller.Dialogs.selectImportDirectory(
						"Select a base directory to import files from"
					);
					if ( dirname != null && new File(dirname).isDirectory() )
						f_file.setText(dirname);
				}
			});
			final JCheckBox cb_recurse = new JCheckBox("Search subdirectories?");
			final JComboBox l_filetypes = new JComboBox(getFileTypes());
			l_filetypes.setSelectedItem(getFileTypes()[0]);
			l_filetypes.setBorder(SamsGui.createTitledBorder("Only import files with type"));
			
			Object[] array = {
				p_file,
				cb_recurse,
				l_filetypes,
				cb_targetGroup,
				status,
				progressBar,
				new JScrollPane(taskOutput),
			};
			
			String diag_title = "Import spectra files";
			final BaseDialog form = new BaseDialog(frame, diag_title, array) {
				public boolean dataOk() {
					String msg = null;
					String dirname = f_file.getText();
					if ( dirname.trim().length() == 0 )
						msg = "Please specify a directory";
					else if ( !new File(dirname.trim()).isDirectory() )
						msg = "Not a valid directory";
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
				
				int successful;
				
				public boolean preAccept() {
					if ( task_isDone )
						return true;
					
					if ( !dataOk() )
						return false;
					
					final String dirname = f_file.getText();
					final boolean recurse = cb_recurse.isSelected();
					String finaltryfiletype = (String) l_filetypes.getSelectedItem();
					if ( finaltryfiletype != null && finaltryfiletype.equals(guess_type) )
						finaltryfiletype = null;
					
					final String tryfiletype = finaltryfiletype;
					final String grp_loc = (String) cb_targetGroup.getSelectedItem();
					
					// do importation:
					Thread thread = new Thread(new Runnable() {
						public void run() {
							Controller.doUpdate(new Runnable() {
								public void run() {
									b_choose.setEnabled(false);
									f_file.setEditable(false);
									cb_recurse.setEnabled(false);
									l_filetypes.setEnabled(false);
									
									btnAccept.setEnabled(false);
									btnCancel.setEnabled(false);
									progressBar.setEnabled(true);
									taskOutput.setEnabled(true);
								}
							});
	
							PrintWriter writer = new PrintWriter(System.out, true);
							SamsDbManager dbman = new SamsDbManager(db, writer);
							try {
								successful = 0;
								DirectoryImporter importer = dbman.createDirectoryImporter(
									dirname, recurse, tryfiletype, grp_loc, 
									new SamsDbManager.ImportDirectoryListener() {
									public void importing(int file_number, String relative_filename, String filetype) {
										task_message.append("\n" +file_number+ " - " +relative_filename+ ": ");
										if ( filetype != null ) {
											task_message.append("recognized as '" +filetype+ "' type.");
											successful++;
										}
										else if ( tryfiletype != null )
											task_message.append("Not recognized as '" +tryfiletype+ "' type.");
										else
											task_message.append("Not recognized.");
										progressBar.setValue(file_number);
									}
								});
								
								progressBar.setMaximum(importer.getEstimatedFiles() +2);
								progressBar.setIndeterminate(false);
								progressBar.setString(null); //display % string
								
								importer.importFiles();
								
								// a)
								task_message.append("\nUpdating database...");
								progressBar.setValue(progressBar.getMaximum() -2);
								db.save();
								
								// b)
								task_message.append("\nUpdating display...");
								progressBar.setValue(progressBar.getMaximum() -1);
								dbgui.setDatabase(db);

								progressBar.setValue(progressBar.getMaximum());
							}
							catch(Exception ex) {
								progressBar.setString("Error: see below");
								task_message.append("\nError: " +ex.getMessage());
							}
							finally {
								task_message.append("\nDone. " +successful+ " files imported");
								task_isDone = true;
								btnAccept.setText("Close");
								btnAccept.setEnabled(true);
							}
						}
					});
					
					progressBar.setString("Starting...");
					progressBar.setIndeterminate(true);
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

	/** Interacts with the user to import files from a given directory. */
	public static void importFiles(DbGui dbgui) {
		if ( dbgui.getDatabase() != null )
			new ImportFiles(dbgui).go();
	}

	static class ImportFiles extends BaseImport {	
		File[] selectedFiles = new File[0];
		ImportFiles(DbGui dbgui) {
			super(dbgui);
		}
		
		void go() {
			JPanel p_file = new JPanel(new FlowLayout(FlowLayout.LEFT));
			final JTextField f_files = new JTextField(0+ " file(s) selected");
			f_files.setEditable(false);
			p_file.setBorder(SamsGui.createTitledBorder("Selected files"));
			p_file.add(f_files);
			final JButton b_choose = new JButton("Choose");
			p_file.add(b_choose);
			b_choose.setMnemonic(KeyEvent.VK_C);
			b_choose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					File[] files = Controller.Dialogs.selectImportFiles(
						"Select the files you want to import"
					);
					if ( files != null ) {
						selectedFiles = files;
						f_files.setText(selectedFiles.length+ " file(s) selected");
					}
				}
			});
			final JComboBox l_filetypes = new JComboBox(getFileTypes());
			l_filetypes.setSelectedItem(getFileTypes()[0]);
			l_filetypes.setBorder(SamsGui.createTitledBorder("Only import files with type"));
			
			Object[] array = {
				p_file,
				l_filetypes,
				cb_targetGroup,
				status,
				progressBar,
				new JScrollPane(taskOutput),
			};
			
			String diag_title = "Import spectra files";
			final BaseDialog form = new BaseDialog(frame, diag_title, array) {
				public boolean dataOk() {
					String msg = null;
					if ( selectedFiles.length == 0 )
						msg = "Please specify the files to import";
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
				
				int successful;
				
				public boolean preAccept() {
					if ( task_isDone )
						return true;
					
					if ( !dataOk() )
						return false;
					
					String finaltryfiletype = (String) l_filetypes.getSelectedItem();
					if ( finaltryfiletype != null && finaltryfiletype.equals(guess_type) )
						finaltryfiletype = null;
					
					final String tryfiletype = finaltryfiletype;
					final String grp_loc = (String) cb_targetGroup.getSelectedItem();
					final MyNode grp_node = dbgui.getTree().findLocationNode(grp_loc, false);
					
					// do importation:
					Thread thread = new Thread(new Runnable() {
						public void run() {
							Controller.doUpdate(new Runnable() {
								public void run() {
									b_choose.setEnabled(false);
									l_filetypes.setEnabled(false);
									
									btnAccept.setEnabled(false);
									btnCancel.setEnabled(false);
									progressBar.setEnabled(true);
									taskOutput.setEnabled(true);
								}
							});
	
							PrintWriter writer = new PrintWriter(System.out, true);
							SamsDbManager dbman = new SamsDbManager(db, writer);
							progressBar.setMaximum(selectedFiles.length);
							progressBar.setIndeterminate(false);
							progressBar.setString(null); //display % string
							
							int successful = 0;
							for ( int i = 0; i < selectedFiles.length; i++ ) {
								File file = selectedFiles[i];
								String filename = file.getAbsolutePath();
								task_message.append("\nimporting " +file.getName());
								try {
									String path = dbman.importFile(filename, tryfiletype, grp_loc);
									successful++;
									dbgui.getTree().addChild(grp_node, file.getName(), true, false);
								}
								catch(Exception ex) {
									task_message.append("\nError: " +ex.getMessage());
								}
								finally {
									progressBar.setValue(i+1);
								}
							}
							dbgui.getTree().scrollToVisible(grp_node);
							task_message.append("\nDone. " +successful+ " signatures imported");
							task_isDone = true;
							btnAccept.setText("Close");
							btnAccept.setEnabled(true);
						}
					});
					
					progressBar.setString("Starting...");
					progressBar.setIndeterminate(true);
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

	/** Interacts with the user to import files from a given ASCII file. */
	public static void importSignaturesFromAsciiFile(DbGui dbgui) {
		if ( dbgui.getDatabase() != null )
			new ImportSignaturesFromAsciiFile(dbgui).go();
	}
	
	static class ImportSignaturesFromAsciiFile extends BaseImport {	
		ImportSignaturesFromAsciiFile(DbGui dbgui) {
			super(dbgui);
		}
		
		void go() {
			JPanel p_file = new JPanel(new FlowLayout(FlowLayout.LEFT));
			final JTextField f_file = new JTextField(32);
			p_file.setBorder(SamsGui.createTitledBorder("Ascii file"));
			p_file.add(f_file);
			final JButton b_choose = new JButton("Choose");
			p_file.add(b_choose);
			b_choose.setMnemonic(KeyEvent.VK_C);
			b_choose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					String filename = Controller.Dialogs.selectImportFile(
						"Select the file"
					);
					if ( filename != null && new File(filename).isFile() )
						f_file.setText(filename);
				}
			});

			// format pattern for suffix in new names:
			JPanel p_suffix = new JPanel(new FlowLayout(FlowLayout.LEFT));
			final JTextField f_suffix = new JTextField(8);
			p_suffix.setBorder(SamsGui.createTitledBorder("To create names of new signatures"));
			p_suffix.add(new JLabel("Pattern for suffix:"));
			f_suffix.setText("_0000");
			p_suffix.add(f_suffix);
			
			
			Object[] array = {
				p_file,
				p_suffix,
				cb_targetGroup,
				status,
				progressBar,
				new JScrollPane(taskOutput),
			};
			
			String diag_title = "Import spectra from Ascii file";
			final BaseDialog form = new BaseDialog(frame, diag_title, array) {
				public boolean dataOk() {
					String msg = null;
					String filename = f_file.getText();
					if ( filename.trim().length() == 0 )
						msg = "Please specify a file";
					else if ( !new File(filename.trim()).isFile() )
						msg = "Not a valid file";
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
				
				int successful;
				
				public boolean preAccept() {
					if ( task_isDone )
						return true;
					
					if ( !dataOk() )
						return false;
					
					final String filename = f_file.getText();
					final String grp_loc = (String) cb_targetGroup.getSelectedItem();
					final MyNode grp_node = dbgui.getTree().findLocationNode(grp_loc, false);
					
					// do importation:
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
							String basefilename = new File(filename).getName();
							int ii = 1; // to make names
							DecimalFormat decform = new DecimalFormat(f_suffix.getText());
							try {
								List sigs = Sams.getSignaturesFromAsciiFile(filename);
								// +2 : see a), b) below
								progressBar.setMaximum(sigs.size() +2);
								progressBar.setIndeterminate(false);
								progressBar.setString(null); //display % string
								
								for ( Iterator it = sigs.iterator(); it.hasNext(); ) {
									Signature sig = (Signature) it.next();
									String path = grp_loc+ "/" +basefilename+ decform.format(ii);
									db.addSpectrum(path, sig);
									task_message.append("\nimporting " +path);
									progressBar.setValue(ii++);
								}
								
								// a)
								task_message.append("\nUpdating database...");
								progressBar.setValue(progressBar.getMaximum() -2);
								db.save();
								
								// b)
								task_message.append("\nUpdating display...");
								progressBar.setValue(progressBar.getMaximum() -1);
								dbgui.setDatabase(db);
								
								progressBar.setValue(progressBar.getMaximum());
							}
							catch(Exception ex) {
								progressBar.setString("Error: see below");
								task_message.append("\nError: " +ex.getMessage());
							}
							finally {
								task_message.append("\nDone. " +(ii-1)+ " signatures imported");
								task_isDone = true;
								btnAccept.setText("Close");
								btnAccept.setEnabled(true);
							}
						}
					});
					
					progressBar.setString("Starting...");
					progressBar.setIndeterminate(true);
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
	
	/** Interacts with the user to import signature from a given Envi file. */
	public static void importSignaturesFromEnviFile(DbGui dbgui) {
		if ( dbgui.getDatabase() != null )
			new ImportSignaturesFromEnviFile(dbgui).go();
	}
	
	static class ImportSignaturesFromEnviFile extends BaseImport {	
		ImportSignaturesFromEnviFile(DbGui dbgui) {
			super(dbgui);
		}
		
		void go() {
			JPanel p_file = new JPanel(new FlowLayout(FlowLayout.LEFT));
			final JTextField f_file = new JTextField(32);
			p_file.setBorder(SamsGui.createTitledBorder("Envi file"));
			p_file.add(f_file);
			final JButton b_choose = new JButton("Choose");
			p_file.add(b_choose);
			b_choose.setMnemonic(KeyEvent.VK_C);
			b_choose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					String filename = Controller.Dialogs.selectImportFile(
						"Select the file"
					);
					if ( filename != null && new File(filename).isFile() )
						f_file.setText(filename);
				}
			});
			JPanel p_location = new JPanel(new GridLayout(1,2));
			final JTextField tf_line = new JTextField(4);
			tf_line.setBorder(SamsGui.createTitledBorder("Line"));
			final JTextField tf_pixel = new JTextField(4);
			tf_pixel.setBorder(SamsGui.createTitledBorder("Pixel"));
			p_location.add(tf_line);
			p_location.add(tf_pixel);
			
			Object[] array = {
				p_file,
				p_location,
				cb_targetGroup,
				status,
				progressBar,
				new JScrollPane(taskOutput),
			};
			
			String diag_title = "Import spectrum from Envi file";
			final BaseDialog form = new BaseDialog(frame, diag_title, array) {
				public boolean dataOk() {
					String msg = null;
					String filename = f_file.getText();
					String s_line = tf_line.getText();
					String s_pixel = tf_pixel.getText();
					if ( filename.trim().length() == 0 )
						msg = "Please specify a file";
					else if ( !new File(filename.trim()).isFile() )
						msg = "Not a valid file";
					else if ( !s_line.matches("\\d+") )
						msg = "Please specify a valid line number";
					else if ( !s_pixel.matches("\\d+") )
						msg = "Please specify a valid pixel number";
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
				
				int successful;
				
				public boolean preAccept() {
					if ( task_isDone )
						return true;
					
					if ( !dataOk() )
						return false;
					
					final String filename = f_file.getText();
					final int line = Integer.parseInt(tf_line.getText());
					final int pixel = Integer.parseInt(tf_pixel.getText());
					final String grp_loc = (String) cb_targetGroup.getSelectedItem();
					final MyNode grp_node = dbgui.getTree().findLocationNode(grp_loc, false);
					
					// do importation:
					Thread thread = new Thread(new Runnable() {
						public void run() {
							Controller.doUpdate(new Runnable() {
								public void run() {
									b_choose.setEnabled(false);
									f_file.setEditable(false);
									tf_line.setEditable(false);
									tf_pixel.setEditable(false);
									
									btnAccept.setEnabled(false);
									btnCancel.setEnabled(false);
									progressBar.setEnabled(true);
									taskOutput.setEnabled(true);
								}
							});
	
							String basefilename = new File(filename).getName();
							try {
								Signature sig = Sams.getSignatureFromEnviFile(filename, line, pixel);
								progressBar.setString("reading");
								String name = basefilename+ "_" +line+ "_" +pixel;
								String path = grp_loc+ "/" +name;
								task_message.append("\nimporting " +path);
								db.addSpectrum(path, sig);
								MyNode child = dbgui.getTree().addChild(grp_node, name, true, false);
								dbgui.getTree().scrollToVisible(child);
								progressBar.setString("done");
							}
							catch(Exception ex) {
								progressBar.setString("Error: see below");
								task_message.append("\nError: " +ex.getMessage());
							}
							finally {
								progressBar.setIndeterminate(false);
								task_message.append("\nDone.");
								task_isDone = true;
								btnAccept.setText("Close");
								btnAccept.setEnabled(true);
							}
						}
					});
					
					progressBar.setString("Starting...");
					progressBar.setIndeterminate(true);
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
	
	static abstract class BaseImport {
		DbGui dbgui;
		JFrame frame;
		ISamsDb db;
		StringBuffer task_message = new StringBuffer();
		boolean task_isDone;
		Timer timer;
		JComboBox cb_targetGroup;
		JLabel status;
		JProgressBar progressBar;
		JTextArea taskOutput;
		
		BaseImport(DbGui dbgui) {
			this.dbgui = dbgui;
			frame = dbgui.getFrame();
			db = dbgui.getDatabase();
			List loc_groups = dbgui.getTree().getLocationGroups();
			cb_targetGroup = new JComboBox(loc_groups.toArray());
			cb_targetGroup.setSelectedItem("/imported"); // if it exists
			cb_targetGroup.setBorder(SamsGui.createTitledBorder("Put new signature(s) under location"));

			status = new JLabel();
			status.setFont(status.getFont().deriveFont(Font.ITALIC));
			progressBar = new JProgressBar(0, 1000);
			progressBar.setValue(0);
			progressBar.setStringPainted(true); //get space for the string
			progressBar.setString("");          //but don't paint it
			progressBar.setEnabled(false);
			taskOutput = new JTextArea(5, 30);
			taskOutput.setMargin(new Insets(5,5,5,5));
			taskOutput.setEditable(false);
			taskOutput.setEnabled(false);
			timer = new Timer(ONE_SECOND, new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					String s = task_message.toString();
					task_message.setLength(0);
					if ( s.length() > 0 ) {
						String last = s.substring(s.lastIndexOf('\n') + 1); 
						taskOutput.append(s + "\n");
						taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
					}
					if ( task_isDone ) {
						Toolkit.getDefaultToolkit().beep();
						timer.stop();
					}
				}
			});
		}
	}
		
}
