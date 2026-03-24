package org.eclipse.bpmn2.modeler.ui.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer;
import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer.PublicationResult;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;

// EcoreFS begin: command that publishes the current BPMN model file to IPFS
public class IPFSPublishModelCommand extends AbstractHandler {

	public static final String ID = "org.eclipse.bpmn2.modeler.command.publishModelToIpfs"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		BPMN2Editor editor = BPMN2Editor.getActiveEditor();
		if (editor == null) {
			return null;
		}

		File tempExport = null;
		try {
			if (editor.isDirty()) {
				editor.doSave(new NullProgressMonitor());
			}

			ModelSource modelSource = resolveModelSource(editor);
			tempExport = modelSource.tempExport;
			Bpmn2Preferences preferences = Bpmn2Preferences.getInstance(editor.getProject());

			if (Bpmn2Preferences.PREF_IPFS_PUBLISH_MODE_IPNS.equals(preferences.getIpfsPublishMode())
					&& preferences.getIpfsDefaultIpnsKey().isEmpty()) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Publish_Error_Title,
						Messages.IPFS_Publish_Missing_IPNS_Key);
				return null;
			}

			PublicationResult result = IPFSModelTransfer.publish(modelSource.modelFile, preferences.getIpfsApiUrl(),
					preferences.getIpfsPublishMode(), preferences.getIpfsDefaultIpnsKey());
			if (result.hasIpnsPublication()) {
				MessageDialog.openInformation(HandlerUtil.getActiveShell(event), Messages.IPFS_Publish_Success_Title,
						NLS.bind(Messages.IPFS_Publish_Success_Message_With_IPNS,
								new Object[] { result.getCid(), result.getIpfsUri(), result.getIpfsPath(),
										result.getGatewayUrl(), result.getIpnsName(), result.getIpnsUri(),
										result.getIpnsPath(), result.getIpnsGatewayUrl() }));
			}
			else {
				MessageDialog.openInformation(HandlerUtil.getActiveShell(event), Messages.IPFS_Publish_Success_Title,
						NLS.bind(Messages.IPFS_Publish_Success_Message,
								new Object[] { result.getCid(), result.getIpfsUri(), result.getIpfsPath(),
										result.getGatewayUrl() }));
			}
			return null;
		} catch (Exception e) {
			Activator.logError(e);
			MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Publish_Error_Title, e.getMessage());
			throw new ExecutionException("Could not publish BPMN2 model to IPFS.", e); //$NON-NLS-1$
		} finally {
			if (tempExport != null && tempExport.exists()) {
				tempExport.delete();
			}
		}
	}

	private ModelSource resolveModelSource(BPMN2Editor editor) throws IOException {
		IFile workspaceFile = editor.getModelFile();
		if (workspaceFile != null) {
			IPath location = workspaceFile.getLocation();
			if (location != null) {
				return new ModelSource(location.toFile(), null);
			}
		}

		URI modelUri = editor.getModelUri();
		if (modelUri != null && modelUri.isFile()) {
			return new ModelSource(new File(modelUri.toFileString()), null);
		}

		Resource resource = editor.getResource();
		if (resource == null) {
			throw new IOException(Messages.IPFS_Publish_No_Model);
		}

		File tempFile = File.createTempFile("bpmn2-publish", ".bpmn"); //$NON-NLS-1$ //$NON-NLS-2$
		try (OutputStream output = new FileOutputStream(tempFile)) {
			resource.save(output, null);
		}
		return new ModelSource(tempFile, tempFile);
	}

	private static final class ModelSource {
		private final File modelFile;
		private final File tempExport;

		private ModelSource(File modelFile, File tempExport) {
			this.modelFile = modelFile;
			this.tempExport = tempExport;
		}
	}
}
// EcoreFS end: command that publishes the current BPMN model file to IPFS
