package samsgui.dbgui;

import samsgui.SamsGui;
import samsgui.BaseDialog;

import samscore.ISamsDb;
import samscore.ISamsDb.ISpectrum;
import samscore.Sams;
import sfsys.ISfsys;
import sfsys.ISfsys.*;
import sig.Signature;
import sigoper.*;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;


/** Makes a computation. */
public class Compute {
	static final int ONE_SECOND = 1000;
	static final String lbl_in_place = "In place";
	static final String lbl_new_signature = "New signature";
	static final String lbl_name_suffix = "Suffix for new name";
	static final String lbl_ignore_suffix = "Ignore original suffix";

	static String ignore_suffix_value = "";   // to remember
	
	private DbGui dbgui;
	private IOperation sigOper;
	private IOperation.IParameterInfo parInfo;
	private List selectedSpectra;
	private Signature reference_sig;
	private DefaultMutableTreeNode computedNode;
	
	
	public Compute(DbGui dbgui, IOperation sigOper, List selectedSpectra, Signature reference_sig) {
		this.dbgui = dbgui;
		this.sigOper = sigOper;
		this.selectedSpectra = selectedSpectra;
		this.reference_sig = reference_sig;
		computedNode = dbgui.getTree().getComputedNode();
		if ( sigOper instanceof IMultiSignatureOperation )
			new MultiForm((IMultiSignatureOperation) sigOper).go();
		else
			new SingleForm(sigOper).go();
	}

    static void doUpdate(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }

	void addParInfoComponents(List array) {
		if ( parInfo == null )
			return;

		int numpars = parInfo.getNumParameters();
		for ( int i = 0; i < numpars; i++ ) {
			String pname = parInfo.getName(i);
			String pdesc = parInfo.getDescription(i);
			Object pval = parInfo.getValue(i);
			if ( pval instanceof String ) {
				String str = (String) pval;
				JTextField cmp = new JTextField(str);
				cmp.setBorder(SamsGui.createTitledBorder(pdesc));
				array.add(cmp);
				//form.addLine(pname, pdesc, str);
			}
			else if ( pval instanceof String[] ) {
				String[] values = (String[]) pval; 
				JList cmp = new JList(values);
				cmp.setSelectedValue(values[0], true);
				cmp.setBorder(SamsGui.createTitledBorder(pdesc));
				array.add(cmp);
				//form.addChoice(pname, pdesc, values, values[0]);
			}
			else if ( pval instanceof Boolean ) {
				boolean defaultValue = ((Boolean) pval).booleanValue();
				JCheckBox cmp = new JCheckBox(pdesc);
				array.add(cmp);
				//form.addCheckBox(pname, pdesc, defaultValue);
			}
		}
	}

	class MultiForm {
		IMultiSignatureOperation sigOper;
		
		StringBuffer task_message = new StringBuffer();
		boolean task_isDone;
		Timer timer;
		JProgressBar progressBar;
		JTextArea taskOutput;
		
		MultiForm(IMultiSignatureOperation sigOper) {
			this.sigOper = sigOper;
			parInfo = sigOper.getParameterInfo();
		}
		
		void go() {
			JFrame frame = dbgui.getFrame();
			final ISamsDb db = dbgui.getDatabase();
			if ( db == null )
				return;
			
			final JTextField f_resultname = new JTextField(20);
			f_resultname.setBorder(SamsGui.createTitledBorder("Computed signature name"));
			final JLabel status = new JLabel();
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
			
			List array = new ArrayList();
			array.add(f_resultname);
			addParInfoComponents(array);
			array.add(status);
			array.add(progressBar);
			array.add(new JScrollPane(taskOutput));
			
			String diag_title = sigOper.getName();
			final BaseDialog form = new BaseDialog(frame, diag_title, array.toArray()) {
				public boolean dataOk() {
					String msg = null;
					String resultname = f_resultname.getText();
					if ( resultname.trim().length() == 0 )
						msg = "Please specify a name for resulting signature";
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
				
				public void notifyUpdate() {
					if ( !timer.isRunning() )
						super.notifyUpdate();
				}
				
				int successful;
				
				public boolean preAccept() {
					if ( task_isDone )
						return true;
					
					if ( !dataOk() )
						return false;
					
					final String resultname = f_resultname.getText();
					
					// do computation:
					Thread thread = new Thread(new Runnable() {
						public void run() {
							doUpdate(new Runnable() {
								public void run() {
									f_resultname.setEditable(false);
									
									btnAccept.setEnabled(false);
									btnCancel.setEnabled(false);
									progressBar.setEnabled(true);
									taskOutput.setEnabled(true);
								}
							});
	
							progressBar.setMaximum(selectedSpectra.size() +2);
							progressBar.setIndeterminate(false);
							progressBar.setString(null); //display % string

							try {	
								Signature[] sigs = getSignatures();
								
								// a)
								task_message.append("\nComputing...");
								progressBar.setValue(progressBar.getMaximum() -2);
								Signature sig = sigOper.operate(sigs);
								String path = "/computed/" +resultname;
								
								// b)
								task_message.append("\nAdding result [" +path+ "]");
								progressBar.setValue(progressBar.getMaximum() -1);
								ISpectrum s = db.addSpectrum(path, sig);
								IFile f = (IFile) db.getGroupingLocation().getRoot().findNode(s.getPath());
								dbgui.getTree().addObject(computedNode, f, true);

								progressBar.setValue(progressBar.getMaximum());
								
								task_message.append("\nDone.");
							}
							catch(RuntimeException ex) {
								task_message.append("\nRuntimeException!: " +ex.getMessage());
								ex.printStackTrace();
							}
							catch(Exception ex) {
								task_message.append("\nError: " +ex.getMessage());
							}
							finally {
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
		
		Signature[] getSignatures() throws Exception {
			Signature[] sigs = new Signature[selectedSpectra.size()];
			for ( int i = 0; i < selectedSpectra.size(); i++ ) {
				IFile s = (IFile) selectedSpectra.get(i);
				String path = s.getPath();
				task_message.append("processing " +path+ "\n");
				progressBar.setValue(i+1);
				Signature sig = dbgui.getDatabase().getSignature(path);
				if ( sig.getUserObject() == null )
					sig.setUserObject(s.getName());
				sigs[i] = sig;
			}
			return sigs;
		}
		
	}
	
	class SingleForm {
		IOperation sigOper;
		SingleForm(IOperation sigOper) {
			this.sigOper = sigOper;
		}
		
		void go() {
		}
	}
	
}
