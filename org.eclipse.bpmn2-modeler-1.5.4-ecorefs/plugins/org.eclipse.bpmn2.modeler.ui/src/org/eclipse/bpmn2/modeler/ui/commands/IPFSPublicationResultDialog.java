package org.eclipse.bpmn2.modeler.ui.commands;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

// EcoreFS begin: custom result dialog keeps long IPFS/IPNS addresses visible and easy to copy
public class IPFSPublicationResultDialog extends TitleAreaDialog {

	private static final int COPY_ALL_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;

	private final String dialogTitle;
	private final String dialogMessage;
	private final String detailsText;

	public IPFSPublicationResultDialog(Shell parentShell, String dialogTitle, String dialogMessage, String detailsText) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.dialogMessage = dialogMessage;
		this.detailsText = detailsText;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(dialogTitle);
		setMessage(dialogMessage);

		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(1, false));

		Label detailsLabel = new Label(container, SWT.NONE);
		detailsLabel.setText(Messages.IPFS_Publish_Result_Details_Label);

		Text details = new Text(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		details.setEditable(false);
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridData gridData = (GridData) details.getLayoutData();
		gridData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH) + 280;
		gridData.heightHint = 220;
		details.setText(detailsText);
		details.setSelection(0, details.getText().length());

		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, COPY_ALL_BUTTON_ID, Messages.IPFS_Publish_Result_Copy_Button, false);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == COPY_ALL_BUTTON_ID) {
			Clipboard clipboard = new Clipboard(getShell().getDisplay());
			try {
				clipboard.setContents(new Object[] { detailsText }, new Transfer[] { TextTransfer.getInstance() });
			} finally {
				clipboard.dispose();
			}
			return;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}
// EcoreFS end: custom result dialog keeps long IPFS/IPNS addresses visible and easy to copy
