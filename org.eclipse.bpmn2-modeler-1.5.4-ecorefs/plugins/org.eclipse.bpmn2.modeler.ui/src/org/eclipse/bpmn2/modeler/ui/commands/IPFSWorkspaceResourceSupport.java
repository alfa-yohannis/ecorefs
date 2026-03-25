package org.eclipse.bpmn2.modeler.ui.commands;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.bpmn2.modeler.ui.util.IPFSModelTransfer;

// EcoreFS begin: shared workspace helpers keep the single-file and project-level IPFS commands consistent
final class IPFSWorkspaceResourceSupport {

	private IPFSWorkspaceResourceSupport() {
	}

	static Bpmn2Preferences resolvePreferences() {
		BPMN2Editor editor = BPMN2Editor.getActiveEditor();
		if (editor != null && editor.getProject() != null) {
			return Bpmn2Preferences.getInstance(editor.getProject());
		}

		IProject project = Bpmn2Preferences.getActiveProject();
		if (project != null) {
			return Bpmn2Preferences.getInstance(project);
		}
		return Bpmn2Preferences.getInstance();
	}

	static IContainer resolveTargetContainer(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object firstElement = ((IStructuredSelection) selection).getFirstElement();
			IResource selectedResource = adaptToResource(firstElement);
			if (selectedResource instanceof IContainer && selectedResource.isAccessible()) {
				return (IContainer) selectedResource;
			}
			if (selectedResource instanceof IFile && selectedResource.getParent() != null
					&& selectedResource.getParent().isAccessible()) {
				return selectedResource.getParent();
			}
		}

		BPMN2Editor editor = BPMN2Editor.getActiveEditor();
		if (editor != null) {
			IFile modelFile = editor.getModelFile();
			if (modelFile != null && modelFile.getParent() != null && modelFile.getParent().isAccessible()) {
				return modelFile.getParent();
			}
			if (editor.getProject() != null && editor.getProject().isAccessible()) {
				return editor.getProject();
			}
		}

		IProject project = Bpmn2Preferences.getActiveProject();
		if (project != null && project.isAccessible()) {
			return project;
		}
		return null;
	}

	static IFile resolveSelectedBpmnFile(ExecutionEvent event, IContainer container) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object firstElement = ((IStructuredSelection) selection).getFirstElement();
			IResource selectedResource = adaptToResource(firstElement);
			if (selectedResource instanceof IFile && isBpmnFile((IFile) selectedResource)
					&& container != null && container.getFullPath().isPrefixOf(selectedResource.getFullPath())) {
				return (IFile) selectedResource;
			}
		}

		BPMN2Editor editor = BPMN2Editor.getActiveEditor();
		if (editor != null) {
			IFile modelFile = editor.getModelFile();
			if (modelFile != null && isBpmnFile(modelFile) && container != null
					&& container.getFullPath().isPrefixOf(modelFile.getFullPath())) {
				return modelFile;
			}
		}
		return null;
	}

	static IFile downloadToWorkspaceFile(String reference, String apiUrl, IContainer container) throws Exception {
		File downloadedFile = IPFSModelTransfer.downloadToTempModel(reference, apiUrl);
		try {
			IFile targetFile = createUniqueTargetFile(container, deriveWorkspaceFileBaseName(reference), "bpmn"); //$NON-NLS-1$
			try (InputStream input = new FileInputStream(downloadedFile)) {
				if (targetFile.exists()) {
					targetFile.setContents(input, IResource.FORCE, new NullProgressMonitor());
				} else {
					targetFile.create(input, IResource.FORCE, new NullProgressMonitor());
				}
			}
			targetFile.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			return targetFile;
		} finally {
			if (downloadedFile.exists()) {
				downloadedFile.delete();
			}
		}
	}

	static IFile writeWorkspaceFile(IContainer container, String relativePath, byte[] contents) throws Exception {
		IPath path = new Path(relativePath);
		if (path.segmentCount() > 1) {
			ensureFolder(container, path.removeLastSegments(1));
		}

		IFile file = container.getFile(path);
		try (InputStream input = new ByteArrayInputStream(contents)) {
			if (file.exists()) {
				file.setContents(input, IResource.FORCE, new NullProgressMonitor());
			} else {
				file.create(input, IResource.FORCE, new NullProgressMonitor());
			}
		}
		file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
		return file;
	}

	static IFolder createUniqueFolder(IContainer parent, String baseName) throws Exception {
		String normalized = normalizeFolderBaseName(baseName);
		IFolder folder = parent.getFolder(new Path(normalized));
		int suffix = 1;
		while (folder.exists()) {
			folder = parent.getFolder(new Path(normalized + "-" + suffix)); //$NON-NLS-1$
			suffix++;
		}
		folder.create(true, true, new NullProgressMonitor());
		return folder;
	}

	static List<IFile> collectBpmnFiles(IContainer root) throws Exception {
		final List<IFile> files = new ArrayList<IFile>();
		root.accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) {
				if (resource instanceof IFile && isBpmnFile((IFile) resource)) {
					files.add((IFile) resource);
				}
				return true;
			}
		});
		Collections.sort(files, new Comparator<IFile>() {
			@Override
			public int compare(IFile left, IFile right) {
				return left.getFullPath().toPortableString().compareTo(right.getFullPath().toPortableString());
			}
		});
		return files;
	}

	static String toRelativePath(IContainer root, IFile file) {
		return file.getFullPath().makeRelativeTo(root.getFullPath()).toPortableString();
	}

	private static IResource adaptToResource(Object element) {
		if (element instanceof IResource) {
			return (IResource) element;
		}
		if (element instanceof IAdaptable) {
			return (IResource) ((IAdaptable) element).getAdapter(IResource.class);
		}
		return null;
	}

	private static boolean isBpmnFile(IFile file) {
		String extension = file.getFileExtension();
		return "bpmn".equalsIgnoreCase(extension) || "bpmn2".equalsIgnoreCase(extension); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static IFolder ensureFolder(IContainer container, IPath relativeFolder) throws Exception {
		IContainer current = container;
		for (String segment : relativeFolder.segments()) {
			IFolder folder = current.getFolder(new Path(segment));
			if (!folder.exists()) {
				folder.create(true, true, new NullProgressMonitor());
			}
			current = folder;
		}
		return (IFolder) current;
	}

	private static IFile createUniqueTargetFile(IContainer container, String baseName, String extension) {
		IFile file = container.getFile(new Path(baseName + "." + extension)); //$NON-NLS-1$
		int suffix = 1;
		while (file.exists()) {
			file = container.getFile(new Path(baseName + "-" + suffix + "." + extension)); //$NON-NLS-1$ //$NON-NLS-2$
			suffix++;
		}
		return file;
	}

	private static String deriveWorkspaceFileBaseName(String reference) {
		String baseName = reference == null ? "ipfs-model" : reference.trim(); //$NON-NLS-1$
		baseName = baseName.replace('\\', '/');
		int lastSlash = baseName.lastIndexOf('/');
		if (lastSlash >= 0 && lastSlash + 1 < baseName.length()) {
			baseName = baseName.substring(lastSlash + 1);
		}
		baseName = baseName.replaceAll("^ipfs://", ""); //$NON-NLS-1$ //$NON-NLS-2$
		baseName = baseName.replaceAll("^ipns://", ""); //$NON-NLS-1$ //$NON-NLS-2$
		baseName = baseName.replaceAll("\\.bpmn2?$", ""); //$NON-NLS-1$ //$NON-NLS-2$
		baseName = baseName.replaceAll("^[^a-zA-Z0-9]+", "ipfs-"); //$NON-NLS-1$ //$NON-NLS-2$
		baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		if (baseName.isEmpty()) {
			baseName = "ipfs-model"; //$NON-NLS-1$
		}
		if (baseName.length() > 80) {
			baseName = baseName.substring(0, 80);
		}
		return baseName;
	}

	private static String normalizeFolderBaseName(String baseName) {
		String normalized = baseName == null ? "ipfs-project" : baseName.trim(); //$NON-NLS-1$
		normalized = normalized.replaceAll("\\.bpmn2?$", ""); //$NON-NLS-1$ //$NON-NLS-2$
		normalized = normalized.replaceAll("^[^a-zA-Z0-9]+", "ipfs-project-"); //$NON-NLS-1$ //$NON-NLS-2$
		normalized = normalized.replaceAll("[^a-zA-Z0-9._-]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		if (normalized.isEmpty()) {
			normalized = "ipfs-project"; //$NON-NLS-1$
		}
		if (normalized.length() > 80) {
			normalized = normalized.substring(0, 80);
		}
		return normalized;
	}
}
// EcoreFS end: shared workspace helpers keep the single-file and project-level IPFS commands consistent
