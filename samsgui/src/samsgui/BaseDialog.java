package samsgui;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import javax.swing.BorderFactory;
import javax.swing.border.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;

/** 
 * Base utility for dialogs.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class BaseDialog extends JDialog {
	public static Border normalBorder = BorderFactory.createLineBorder(Color.black);
	public static Border redBorder = BorderFactory.createLineBorder(Color.red);
	
	private Object[] components;
	private boolean componentsEnabled;
    private JOptionPane optionPane;
    private boolean accepted;
    protected JButton btnAccept = new JButton("OK");
    protected JButton btnCancel = new JButton("Cancel");
	
	private void _setListeners(final Object comp) {
		if ( comp instanceof Container ) {
			Container container = (Container) comp;
			Component[] components = container.getComponents();
			for ( int i = 0; i < components.length; i++ )
				_setListeners(components[i]);
		}
		if ( comp instanceof JTextComponent ) {
			((JTextComponent) comp).getDocument().addDocumentListener(new DocumentListener() {
				void common() {
					JTextComponent c = (JTextComponent) comp; 
					notifyUpdate(c.getName(), c.getText()); 
				}
				public void insertUpdate(DocumentEvent e) { common(); }
				public void removeUpdate(DocumentEvent e) { common(); }
				public void changedUpdate(DocumentEvent e) { common(); }
			});
		}
		
		if ( comp instanceof JComboBox ) {
			((JComboBox) comp).addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					JComboBox c = (JComboBox) comp;
					notifyUpdate(c.getName(), c.getSelectedItem());
				}
			});
		}
		if ( comp instanceof JCheckBox ) {
			((JCheckBox) comp).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JCheckBox c = (JCheckBox) comp;
					notifyUpdate(c.getName(), Boolean.valueOf(c.isSelected()));
				}
			});
		}
		if ( comp instanceof JRadioButton ) {
			((JRadioButton) comp).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JRadioButton c = (JRadioButton) comp;
					notifyUpdate(c.getName(), Boolean.valueOf(c.isSelected()));
				}
			});
		}
	}

	private void _setComponentsEnabled(Object comp, boolean enabled) {
		if ( comp instanceof Container ) {
			Container container = (Container) comp;
			Component[] components = container.getComponents();
			for ( int i = 0; i < components.length; i++ )
				_setComponentsEnabled(components[i], enabled);
		}
		if ( comp instanceof JComponent )
			((JComponent) comp).setEnabled(enabled);
	}

    public BaseDialog(Dialog aFrame, String title, Object[] components) {
		super(aFrame, title, true);
		init(components);
	}
	
    public BaseDialog(Frame aFrame, String title, Object[] components) {
		super(aFrame, title, true);
		init(components);
	}
	
    private void init(Object[] components) {
		this.components = components;
		componentsEnabled = true;
        Object[] options = {btnAccept, btnCancel};
		accepted = false;
		btnAccept.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				optionPane.setValue(btnAccept.getText());
			}
		});
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				optionPane.setValue(btnCancel.getText());
			}
		});
		for ( int i = 0; i < components.length; i++ )
			_setListeners(components[i]);
		
        optionPane = new JOptionPane(components, 
                                    JOptionPane.QUESTION_MESSAGE,
                                    JOptionPane.YES_NO_OPTION,
                                    null,
                                    options,
                                    options[0]
		);
        setContentPane(optionPane);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
            }
        });
		
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String prop = e.getPropertyName();
                if (isVisible() 
                && (e.getSource() == optionPane)
                && (prop.equals(JOptionPane.VALUE_PROPERTY) ||
                     prop.equals(JOptionPane.INPUT_VALUE_PROPERTY))
				) {
                    Object value = optionPane.getValue();

                    if (value == JOptionPane.UNINITIALIZED_VALUE)
                        return;  //ignore reset

                    // Reset the JOptionPane's value.
                    // If you don't do this, then if the user
                    // presses the same button next time, no
                    // property change event will be fired.
                    optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
					//System.out.println(value.getClass().getName()+ " : " +value);
                    if (value.equals(btnAccept.getText())) {
						if ( preAccept() ) {
							accepted = true;
							close();
						}
						else
							btnAccept.setEnabled(false);
                    } 
					else
                        close();
                }
            }
        });
    }
	
	public boolean areComponentsEnabled() {
		return componentsEnabled;
	}
	public void setComponentsEnabled(boolean enabled) {
		componentsEnabled = enabled;
		for ( int i = 0; i < components.length; i++ )
			_setComponentsEnabled(components[i], enabled);
	}
	
	public void activate() {
		btnAccept.setEnabled(dataOk());
	}

	/** Any field modification is notified to subclasses via this method. */
    public void notifyUpdate(String comp_name, Object value) {
        btnAccept.setEnabled(dataOk());
    }

	public boolean dataOk() {
		return true;
	}
	
	public boolean preAccept() {
		return dataOk();
	}
	
    public boolean accepted() {
        return accepted;
    }
	
    public void close() {
        setVisible(false);
    }
}
