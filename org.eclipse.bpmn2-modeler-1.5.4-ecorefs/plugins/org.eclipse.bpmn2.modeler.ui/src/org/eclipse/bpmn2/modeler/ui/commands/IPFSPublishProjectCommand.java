package org.eclipse.bpmn2.modeler.ui.commands;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer;
import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer.PublicationResult;
import org.eclipse.bpmn2.modeler.ui.util.IPFSProjectManifest;
import org.eclipse.bpmn2.modeler.ui.util.IPFSProjectManifest.ModelEntry;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

// EcoreFS begin: command that publishes all BPMN models in the active project or folder through a manifest
public class IPFSPublishProjectCommand extends AbstractHandler {

	public static final String ID = "org.eclipse.bpmn2.modeler.command.publishProjectToIpfs"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		File manifestFile = null;
		try {
			IContainer container = IPFSWorkspaceResourceSupport.resolveTargetContainer(event);
			if (container == null || !container.isAccessible()) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Publish_Error_Title,
						Messages.IPFS_Project_Publish_No_Target);
				return null;
			}

			Bpmn2Preferences preferences = IPFSWorkspaceResourceSupport.resolvePreferences();
			if (Bpmn2Preferences.PREF_IPFS_PUBLISH_MODE_IPNS.equals(preferences.getIpfsPublishMode())
					&& preferences.getIpfsDefaultIpnsKey().isEmpty()) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Publish_Error_Title,
						Messages.IPFS_Publish_Missing_IPNS_Key);
				return null;
			}

			PlatformUI.getWorkbench().saveAllEditors(false);

			List<IFile> modelFiles = IPFSWorkspaceResourceSupport.collectBpmnFiles(container);
			if (modelFiles.isEmpty()) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Publish_Error_Title,
						Messages.IPFS_Project_Publish_No_Models);
				return null;
			}

			IFile selectedRoot = IPFSWorkspaceResourceSupport.resolveSelectedBpmnFile(event, container);
			if (selectedRoot == null) {
				selectedRoot = modelFiles.get(0);
			}

			List<ModelEntry> entries = new ArrayList<ModelEntry>();
			StringBuilder details = new StringBuilder();
			details.append(NLS.bind(Messages.IPFS_Project_Publish_Details_Header,
					new Object[] { container.getFullPath().toPortableString(), Integer.valueOf(modelFiles.size()),
							IPFSWorkspaceResourceSupport.toRelativePath(container, selectedRoot) }));
			details.append("\n\n"); //$NON-NLS-1$

			for (IFile modelFile : modelFiles) {
				if (modelFile.getLocation() == null) {
					continue;
				}
				String relativePath = IPFSWorkspaceResourceSupport.toRelativePath(container, modelFile);
				PublicationResult publication = IPFSModelTransfer.publish(modelFile.getLocation().toFile(),
						preferences.getIpfsApiUrl());
				entries.add(new ModelEntry(relativePath, publication.getCid()));
				details.append(relativePath).append(" -> ").append(publication.getCid()).append('\n'); //$NON-NLS-1$
			}

			if (entries.isEmpty()) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Publish_Error_Title,
						Messages.IPFS_Project_Publish_No_Models);
				return null;
			}

			IPFSProjectManifest manifest = new IPFSProjectManifest(container.getName(),
					IPFSWorkspaceResourceSupport.toRelativePath(container, selectedRoot), entries);
			manifestFile = File.createTempFile("bpmn-project-manifest", ".json"); //$NON-NLS-1$ //$NON-NLS-2$
			Files.writeString(manifestFile.toPath(), manifest.toJson(), StandardCharsets.UTF_8);

			PublicationResult manifestPublication = IPFSModelTransfer.publish(manifestFile, preferences.getIpfsApiUrl(),
					preferences.getIpfsPublishMode(), preferences.getIpfsDefaultIpnsKey());
			new IPFSPublicationResultDialog(HandlerUtil.getActiveShell(event),
					Messages.IPFS_Project_Publish_Success_Title, Messages.IPFS_Project_Publish_Result_Message,
					buildPublicationDetails(manifestPublication, details.toString())).open();
			return null;
		} catch (Exception e) {
			Activator.logError(e);
			MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.IPFS_Project_Publish_Error_Title,
					e.getMessage());
			throw new ExecutionException("Could not publish the BPMN project to IPFS.", e); //$NON-NLS-1$
		} finally {
			if (manifestFile != null && manifestFile.exists()) {
				manifestFile.delete();
			}
		}
	}

	private String buildPublicationDetails(PublicationResult manifestPublication, String modelDetails) {
		StringBuilder details = new StringBuilder();
		details.append(NLS.bind(Messages.IPFS_Project_Publish_Manifest_Details,
				new Object[] { manifestPublication.getCid(), manifestPublication.getIpfsUri(), manifestPublication.getIpfsPath(),
						manifestPublication.getGatewayUrl() }));
		if (manifestPublication.hasIpnsPublication()) {
			details.append('\n').append('\n');
			details.append(NLS.bind(Messages.IPFS_Project_Publish_Manifest_Details_With_IPNS,
					new Object[] { manifestPublication.getIpnsName(), manifestPublication.getIpnsUri(),
							manifestPublication.getIpnsPath(), manifestPublication.getIpnsGatewayUrl() }));
		}
		details.append('\n').append('\n').append(modelDetails);
		return details.toString();
	}
}
// EcoreFS end: command that publishes all BPMN models in the active project or folder through a manifest
