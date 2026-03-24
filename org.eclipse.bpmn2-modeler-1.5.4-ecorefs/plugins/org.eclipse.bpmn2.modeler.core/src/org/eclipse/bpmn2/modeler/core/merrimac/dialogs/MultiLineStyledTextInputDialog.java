package org.eclipse.bpmn2.modeler.core.merrimac.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class MultiLineStyledTextInputDialog extends StyledTextInputDialog {
	
	public MultiLineStyledTextInputDialog(Shell parentShell, String title, String message, String initialValue,
			IInputValidator validator) {
		super(parentShell, title, message, initialValue, validator);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected StyledText createText(Composite composite) {
		StyledText text = new StyledText(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		data.heightHint = 5 * text.getLineHeight();
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		text.setLayoutData(data);
		return text;
	}
}