package samsgui.dbgui;

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
		new ImportFilesFromDirectory().go(dbgui);
	}

	static class ImportFilesFromDirectory {	
		StringBuffer task_message = new StringBuffer();
		boolean task_isDone;
		Timer timer;
		
		void go(final DbGui dbgui) {
			JFrame frame = dbgui.getFrame();
			final ISamsDb db = dbgui.getDatabase();
			if ( db == null )
				return;
			
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
			final JList l_filetypes = new JList(getFileTypes());
			l_filetypes.setSelectedValue(getFileTypes()[0], true);
			l_filetypes.setBorder(SamsGui.createTitledBorder("Only import files with type"));
			final JLabel status = new JLabel();
			status.setFont(status.getFont().deriveFont(Font.ITALIC));
			final JProgressBar progressBar = new JProgressBar(0, 1000);
			progressBar.setValue(0);
			progressBar.setStringPainted(true); //get space for the string
			progressBar.setString("");          //but don't paint it
			progressBar.setEnabled(false);
			final JTextArea taskOutput = new JTextArea(5, 30);
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
						taskOutput.append(s + "\n");
						taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
					}
						
					if ( task_isDone ) {
						Toolkit.getDefaultToolkit().beep();
						timer.stop();
					}
				}
			});
			
			Object[] array = {
				p_file,
				cb_recurse,
				l_filetypes,
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
					String finaltryfiletype = (String) l_filetypes.getSelectedValue();
					if ( finaltryfiletype != null && finaltryfiletype.equals(guess_type) )
						finaltryfiletype = null;
					
					final String tryfiletype = finaltryfiletype;
					
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
									dirname, recurse, tryfiletype, new SamsDbManager.ImportDirectoryListener() {
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
								
								task_isDone = true;
								task_message.append("\nDone. " +successful+ " files successfully imported");
								btnAccept.setText("Close");
								btnAccept.setEnabled(true);
							}
							catch(Exception ex) {
								task_message.append("\nError: " +ex.getMessage());
								task_isDone = true;
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

	/**
	 * Gets the signatures contained in an ASCII file.
	 * <ul>
	 *	<li> The file is scanned one line at a time.
	 *	<li> Separators for tokens are: simple spaces, commas, and/or tabs.
	 *	<li> A line is "recognized" if it starts with at least two floating point values;
	 *       otherwise, the line is ignored.
	 *	<li> Recognized lines in the same file can contain different number of columns.
	 *	<li> Let val_0, val_1, ..., val_n be the scanned, consecutive floating point values
	 *       found in the line.
	 *	<li> for 1 &lt;= i &lt;= n, signature (i-1)-th is added the point (val_0, val_i),
	 *       that is, val_0 will be its abscissa and val_i its ordinate.
	 * </ul>
	 *
	 * @param filename  Name of the file to read.
	 * @return The list of signatures in the same order as they appear in the file.
	 */
	public static List getSignaturesFromAsciiFile(String filename) throws Exception {
		String SEPARATORS = " ,\t";
		BufferedReader stream = null;
		int lineno = 0;
		String line;
		List sigs = new ArrayList();
		try {
			stream = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			while ( (line = stream.readLine()) != null ) {
				lineno++;
				StringTokenizer st = new StringTokenizer(line, SEPARATORS);
				int columns = st.countTokens();
				if ( columns <= 1 )
					continue; // just ignore this line
				
				double x;
				try {			
					x = Double.parseDouble(st.nextToken());
				}
				catch (NumberFormatException ex) {
					continue; // just ignore this line
				}
				
				// scan next columns-1 columns:
				for ( int ii = 0; ii < columns - 1; ii++ ) {
					try {			
						double y = Double.parseDouble(st.nextToken());
						
						// successfuly gotten y-value for ii-th signature
						// check if a new signature must be created:
						if ( ii == sigs.size() )
							sigs.add(new Signature());
						
						Signature sig = (Signature) sigs.get(ii);
						sig.addDatapoint(x, y);
					}
					catch (NumberFormatException ex) {
						break; // stop scanning current line
					}
				}
			}
			return sigs;
		}
		catch ( IOException ex ) {
			throw new Exception(
				"At line " +lineno+ ":\n" +
				ex.getClass().getName()+ " : " +ex.getMessage()
			);
		}
		finally{
			if ( stream != null )
				try{ stream.close(); }catch ( Exception ex ){}
		}
	}

	/** Interacts with the user to import files from a given ASCII file. */
	public static void importSignaturesFromAsciiFile(DbGui dbgui) {
		new ImportSignaturesFromAsciiFile().go(dbgui);
	}
	
	static class ImportSignaturesFromAsciiFile {	
		StringBuffer task_message = new StringBuffer();
		boolean task_isDone;
		Timer timer;
		
		void go(final DbGui dbgui) {
			JFrame frame = dbgui.getFrame();
			final ISamsDb db = dbgui.getDatabase();
			if ( db == null )
				return;
			
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
						taskOutput.append(s + "\n");
						taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
					}
						
					if ( task_isDone ) {
						Toolkit.getDefaultToolkit().beep();
						timer.stop();
					}
				}
			});
			
			Object[] array = {
				p_file,
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
							try {
								int ii = 1; // to make names
								List sigs = getSignaturesFromAsciiFile(filename);
								// +2 : see a), b) below
								progressBar.setMaximum(sigs.size() +2);
								progressBar.setIndeterminate(false);
								progressBar.setString(null); //display % string
								
								for ( Iterator it = sigs.iterator(); it.hasNext(); ) {
									Signature sig = (Signature) it.next();
									String path = "imported" + "/" +basefilename+ "_" +ii;
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
								
								task_isDone = true;
								task_message.append("\nDone. " +ii+ " signatures successfully imported");
								btnAccept.setText("Close");
								btnAccept.setEnabled(true);
							}
							catch(Exception ex) {
								task_message.append("\nError: " +ex.getMessage());
								task_isDone = true;
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
}
