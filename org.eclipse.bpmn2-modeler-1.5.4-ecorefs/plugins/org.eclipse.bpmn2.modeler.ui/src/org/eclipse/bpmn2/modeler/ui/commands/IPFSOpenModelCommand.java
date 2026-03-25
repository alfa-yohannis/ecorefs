package org.eclipse.bpmn2.modeler.ui.commands;

import java.io.File;

import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

// EcoreFS begin: command that downloads a BPMN model from IPFS/IPNS and opens it locally
public class IPFSOpenModelCommand extends AbstractHandler {

	public static final String ID = "org.eclipse.bpmn2.modeler.command.openFromIpfs"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String apiUrl = IPFSWorkspaceResourceSupport.resolvePreferences().getIpfsApiUrl();
		String initialReference = IPFSWorkspaceResourceSupport.resolvePreferences().getIpfsDefaultLoadReference();
		InputDialog dialog = new InputDialog(HandlerUtil.getActiveShell(event), Messages.IPFS_Open_Title,
				Messages.IPFS_Open_Message, initialReference, new IInputValidator() {
					@Override
					public String isValid(String newText) {
						if (newText == null || newText.trim().isEmpty()) {
							return Messages.IPFS_Open_Invalid_Empty;
						}
						if (!IPFSModelTransfer.isSupportedReference(newText)) {
							return Messages.IPFS_Open_Invalid_Format;
						}
						return null;
					}
				});

		if (dialog.open() != Window.OK) {
			return null;
		}

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			throw new ExecutionException("No active workbench window is available."); //$NON-NLS-1$
		}

		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			throw new ExecutionException("No active workbench page is available."); //$NON-NLS-1$
		}

		try {
			// EcoreFS begin: persist downloaded models into the active workspace folder when one is available
			org.eclipse.core.resources.IContainer targetContainer = IPFSWorkspaceResourceSupport.resolveTargetContainer(event);
			if (targetContainer != null) {
				org.eclipse.core.resources.IFile targetFile = IPFSWorkspaceResourceSupport.downloadToWorkspaceFile(
						dialog.getValue(), apiUrl, targetContainer);
				IDE.openEditor(page, targetFile);
				return null;
			}

			File tempFile = IPFSModelTransfer.downloadToTempModel(dialog.getValue(), apiUrl);
			IDE.openEditorOnFileStore(page, EFS.getStore(tempFile.toURI()));
			// EcoreFS end: persist downloaded models into the active workspace folder when one is available
			return null;
		} catch (Exception e) {
			Activator.logError(e);
			MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Open_Error_Title, e.getMessage());
			throw new ExecutionException("Could not open BPMN2 model from IPFS.", e); //$NON-NLS-1$
		}
	}
}
// EcoreFS end: command that downloads a BPMN model from IPFS/IPNS and opens it locally
