package org.eclipse.bpmn2.modeler.ui.commands;

import java.nio.charset.StandardCharsets;

import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer;
import org.eclipse.bpmn2.modeler.ui.util.IPFSProjectManifest;
import org.eclipse.bpmn2.modeler.ui.util.IPFSProjectManifest.ModelEntry;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

// EcoreFS begin: command that restores a published multi-file BPMN project manifest into the workspace
public class IPFSOpenProjectCommand extends AbstractHandler {

	public static final String ID = "org.eclipse.bpmn2.modeler.command.openProjectFromIpfs"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		InputDialog dialog = new InputDialog(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Open_Title,
				Messages.IPFS_Project_Open_Message, "", new IInputValidator() { //$NON-NLS-1$
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

		try {
			IContainer targetContainer = IPFSWorkspaceResourceSupport.resolveTargetContainer(event);
			if (targetContainer == null || !targetContainer.isAccessible()) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Open_Error_Title,
						Messages.IPFS_Project_Open_No_Target);
				return null;
			}

			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null || window.getActivePage() == null) {
				throw new ExecutionException("No active workbench page is available."); //$NON-NLS-1$
			}
			IWorkbenchPage page = window.getActivePage();

			String apiUrl = IPFSWorkspaceResourceSupport.resolvePreferences().getIpfsApiUrl();
			String manifestJson = new String(IPFSModelTransfer.downloadBytes(dialog.getValue(), apiUrl),
					StandardCharsets.UTF_8);
			IPFSProjectManifest manifest = IPFSProjectManifest.fromJson(manifestJson);

			IFolder importFolder = IPFSWorkspaceResourceSupport.createUniqueFolder(targetContainer,
					deriveImportFolderName(manifest));
			IFile rootFile = null;
			for (ModelEntry model : manifest.getModels()) {
				byte[] bytes = IPFSModelTransfer.downloadBytes(model.getCid(), apiUrl);
				IFile file = IPFSWorkspaceResourceSupport.writeWorkspaceFile(importFolder, model.getPath(), bytes);
				if (manifest.getRootModel().equals(model.getPath())) {
					rootFile = file;
				}
			}

			if (rootFile == null) {
				rootFile = importFolder.getFile(new Path(manifest.getRootModel()));
			}
			if (!rootFile.exists()) {
				throw new ExecutionException("Root model file was not found after import: " //$NON-NLS-1$
						+ manifest.getRootModel());
			}
			IDE.openEditor(page, rootFile);
			return null;
		} catch (Exception e) {
			Activator.logError(e);
			MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Open_Error_Title,
					e.getMessage());
			throw new ExecutionException("Could not open the BPMN project from IPFS.", e); //$NON-NLS-1$
		}
	}

	private String deriveImportFolderName(IPFSProjectManifest manifest) {
		String base = manifest.getProjectName();
		if (base == null || base.trim().isEmpty()) {
			base = manifest.getRootModel();
		}
		return "ipfs-project-" + base; //$NON-NLS-1$
	}
}
// EcoreFS end: command that restores a published multi-file BPMN project manifest into the workspace
