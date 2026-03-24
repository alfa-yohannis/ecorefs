package org.eclipse.bpmn2.modeler.core.merrimac.dialogs;

import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class StyledTextInputDialog extends Dialog {
    /**
     * The title of the dialog.
     */
    private String title;

    /**
     * The message to display, or <code>null</code> if none.
     */
    private String message;

    /**
     * The input value; the empty string by default.
     */
    private String value = "";//$NON-NLS-1$

    /**
     * The input validator, or <code>null</code> if none.
     */
    private IInputValidator validator;

    /**
     * OK button widget.
     */
    private Button okButton;

    /**
     * Input text widget.
     */
    private StyledText text;

    /**
     * Error message label widget.
     */
    private Text errorMessageText;
    
    /**
     * Error message string.
     */
    private String errorMessage;
    
	/**
	 * Preference Store where size and location of this dialog are kept.
	 * When the dialog is resized, this info is updated; when the dialog
	 * is reopened for this same object type and feature, its size and
	 * location are restored from these values.
	 */
	protected IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

    /**
     * Creates an input dialog with OK and Cancel buttons. Note that the dialog
     * will have no visual representation (no widgets) until it is told to open.
     * <p>
     * Note that the <code>open</code> method blocks for input dialogs.
     * </p>
     * 
     * @param parentShell
     *            the parent shell, or <code>null</code> to create a top-level
     *            shell
     * @param dialogTitle
     *            the dialog title, or <code>null</code> if none
     * @param dialogMessage
     *            the dialog message, or <code>null</code> if none
     * @param initialValue
     *            the initial input value, or <code>null</code> if none
     *            (equivalent to the empty string)
     * @param validator
     *            an input validator, or <code>null</code> if none
     */
    public StyledTextInputDialog(Shell parentShell, String dialogTitle,
            String dialogMessage, String initialValue, IInputValidator validator) {
        super(parentShell);
        this.title = dialogTitle;
        message = dialogMessage;
        if (initialValue == null) {
			value = "";//$NON-NLS-1$
		} else {
			value = initialValue;
		}
        this.validator = validator;
    }

    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            value = text.getText();
        } else {
            value = null;
        }
        super.buttonPressed(buttonId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null) {
			shell.setText(title);
		}
    }
    

	/**
	 * The Preference Store key used to access this dialog's position and size.
	 * Implementations should override this to provide their own key string
	 * based on the EObject and feature being edited.
	 * 
	 * @return
	 */
	protected String getPreferenceKey() {
		return title==null ? null : title.replaceAll(" ", "_");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
	 */
	@Override
	protected void initializeBounds() {

		final String key = getPreferenceKey();
		if (key!=null && !key.isEmpty()) {
			Point p = getShell().getSize();
			int width = preferenceStore.getInt("dialog."+key+".width"); //$NON-NLS-1$ //$NON-NLS-2$
			if (width==0)
				width = p.x;
			int height = preferenceStore.getInt("dialog."+key+".height"); //$NON-NLS-1$ //$NON-NLS-2$
			if (height==0)
				height = p.y;
			getShell().setSize(width,height);
			
			p = getShell().getLocation();
			int x = preferenceStore.getInt("dialog."+key+".x"); //$NON-NLS-1$ //$NON-NLS-2$
			if (x==0)
				x = p.x;
			int y = preferenceStore.getInt("dialog."+key+".y"); //$NON-NLS-1$ //$NON-NLS-2$
			if (y==0)
				y = p.y;
			getShell().setLocation(x,y);
	
			getShell().addControlListener(new ControlListener() {
				public void controlMoved(ControlEvent e)
				{
					Point p = getShell().getLocation();
					preferenceStore.setValue("dialog."+key+".x", p.x); //$NON-NLS-1$ //$NON-NLS-2$
					preferenceStore.setValue("dialog."+key+".y", p.y); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				public void controlResized(ControlEvent e)
				{
					Point p = getShell().getSize();
					preferenceStore.setValue("dialog."+key+".width", p.x); //$NON-NLS-1$ //$NON-NLS-2$
					preferenceStore.setValue("dialog."+key+".height", p.y); //$NON-NLS-1$ //$NON-NLS-2$
				}
		
			});
		}
		else {
			super.initializeBounds();
		}
	}

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        okButton = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        //do this here because setting the text will set enablement on the OK
        // button
        text.setFocus();
        if (value != null) {
            text.setText(value);
            text.selectAll();
        }
    }

    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        // create composite
        Composite composite = (Composite) super.createDialogArea(parent);
        // create message
        if (message != null) {
            Label label = new Label(composite, SWT.WRAP);
            label.setText(message);
            GridData data = new GridData(GridData.GRAB_HORIZONTAL
                    /* | GridData.GRAB_VERTICAL */ | GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.VERTICAL_ALIGN_CENTER);
            data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            label.setLayoutData(data);
            label.setFont(parent.getFont());
        }
        text = createText(composite);
        text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });
        errorMessageText = new Text(composite, SWT.READ_ONLY);
        errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL));
        errorMessageText.setBackground(errorMessageText.getDisplay()
                .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        // Set the error message text
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=66292
        setErrorMessage(errorMessage);

        applyDialogFont(composite);
        return composite;
    }

    /**
     * Creates the text widget for the user's input.
     * 
     * @return the text widget
     */
    protected StyledText createText(Composite composite) {
    	StyledText text = new StyledText(composite, SWT.SINGLE | SWT.BORDER);
      text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
              | GridData.HORIZONTAL_ALIGN_FILL));
      return text;
    }

    /**
     * Returns the error message label.
     * 
     * @return the error message label
     * @deprecated use setErrorMessage(String) instead
     */
    @Deprecated
    protected Label getErrorMessageLabel() {
        return null;
    }

    /**
     * Returns the OK button.
     * 
     * @return the OK button
     */
    protected Button getOkButton() {
        return okButton;
    }

    /**
     * Returns the text area.
     * 
     * @return the text area
     */
    protected StyledText getText() {
        return text;
    }

    /**
     * Returns the validator.
     * 
     * @return the validator
     */
    protected IInputValidator getValidator() {
        return validator;
    }

    /**
     * Returns the string typed into this input dialog.
     * 
     * @return the input string
     */
    public String getValue() {
        return value;
    }

    /**
     * Validates the input.
     * <p>
     * The default implementation of this framework method delegates the request
     * to the supplied input validator object; if it finds the input invalid,
     * the error message is displayed in the dialog's message line. This hook
     * method is called whenever the text changes in the input field.
     * </p>
     */
    protected void validateInput() {
        String errorMessage = null;
        if (validator != null) {
            errorMessage = validator.isValid(text.getText());
        }
        // Bug 16256: important not to treat "" (blank error) the same as null
        // (no error)
        setErrorMessage(errorMessage);
    }

    /**
     * Sets or clears the error message.
     * If not <code>null</code>, the OK button is disabled.
     * 
     * @param errorMessage
     *            the error message, or <code>null</code> to clear
     */
    public void setErrorMessage(String errorMessage) {
    	this.errorMessage = errorMessage;
    	if (errorMessageText != null && !errorMessageText.isDisposed()) {
    		errorMessageText.setText(errorMessage == null ? "" : errorMessage); //$NON-NLS-1$
    		errorMessageText.getParent().update();
    		// Access the OK button by id, in case clients have overridden button creation.
    		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
    		Control button = getButton(IDialogConstants.OK_ID);
    		if (button != null) {
    			button.setEnabled(errorMessage == null);
    		}
    	}
    }
}