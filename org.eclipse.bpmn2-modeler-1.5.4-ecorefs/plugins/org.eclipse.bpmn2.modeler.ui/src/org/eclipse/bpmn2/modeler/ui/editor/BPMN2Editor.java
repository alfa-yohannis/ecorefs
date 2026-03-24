/******************************************************************************* 
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 *  All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.editor;

import java.io.File;
import java.util.Arrays;

import org.eclipse.bpmn2.modeler.core.builder.BPMN2Builder;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditingDialog;
import org.eclipse.bpmn2.modeler.core.model.ModelHandlerLocator;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.utils.ErrorUtils;
import org.eclipse.bpmn2.modeler.core.utils.FileUtils;
import org.eclipse.bpmn2.modeler.core.utils.MarkerUtils;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.validation.BPMN2ProjectValidator;
import org.eclipse.bpmn2.modeler.core.validation.BPMN2ValidationStatusLoader;
import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.Bpmn2DiagramEditorInput;
import org.eclipse.bpmn2.modeler.ui.util.PropertyUtil;
import org.eclipse.bpmn2.util.Bpmn2ResourceImpl;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

public class BPMN2Editor extends DefaultBPMN2Editor implements IGotoMarker {
	public static final String EDITOR_ID = "org.eclipse.bpmn2.modeler.ui.bpmn2editor"; //$NON-NLS-1$

	private static BPMN2Editor activeEditor;

	protected BPMN2MultiPageEditor multipageEditor;
	private IPartListener2 selectionListener;
	private IWorkbenchListener workbenchListener;
    private IResourceChangeListener markerChangeListener;
	private boolean workbenchShutdown = false;

	public BPMN2Editor(BPMN2MultiPageEditor mpe) {
		multipageEditor = mpe;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (input instanceof IFileEditorInput) {
			BPMN2Builder.INSTANCE.loadExtensions( ((IFileEditorInput)input).getFile().getProject() );
		}

		// This needs to happen very early because setActiveEditor will try to
		// determine the TargetRuntime from the EditorInput.
		setActiveEditor(this);

		if (this.getDiagramBehavior()==null) {
			super.init(site, input);
			// add a listener so we get notified if the workbench is shutting down.
			// in this case we don't want to delete the temp file!
			addWorkbenchListener();
			addSelectionListener();
			addMarkerChangeListener();
		}
		else if (input instanceof Bpmn2DiagramEditorInput) {
			bpmnDiagram = ((Bpmn2DiagramEditorInput)input).getBpmnDiagram();
			if (bpmnDiagram!=null) {
				setBpmnDiagram(bpmnDiagram);
			}
		}
	}

	@Override
	protected EditorInputHelper getInputHelper() {
		return new ExtendedEditorInputHelper();
	}

    protected void loadMarkers() {
    	if (getModelFile()!=null) {
	        // read in the markers
	        BPMN2ValidationStatusLoader vsl = new BPMN2ValidationStatusLoader(this);

	        try {
	            vsl.load(Arrays.asList(getModelFile().findMarkers(null, true, IResource.DEPTH_ZERO)));
	        } catch (CoreException e) {
	            Activator.logStatus(e.getStatus());
	        }
    	}
    }

	@Override
	protected DiagramBehavior createDiagramBehavior() {
		DiagramBehavior diagramBehavior = new BPMN2EditorDiagramBehavior(this);
		return diagramBehavior;
	}

	public BPMN2MultiPageEditor getMultipageEditor() {
		return multipageEditor;
	}

	public static BPMN2Editor getActiveEditor() {
		return activeEditor;
	}

	protected void setActiveEditor(BPMN2Editor editor) {
		activeEditor = editor;
		if (activeEditor!=null) {
			Bpmn2Preferences.setActiveProject(activeEditor.getProject());
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class required) {
		if (required == StructuredTextEditor.class) {
			// ugly hack to disable selection in Property Viewer while source viewer is active
			if (multipageEditor.getActiveEditor() == multipageEditor.getSourceViewer())
				return multipageEditor.getSourceViewer();
		}
		return super.getAdapter(required);
	}

    @Override
    public void gotoMarker(IMarker marker) {
    	ResourceSet rs = getEditingDomain().getResourceSet();
        EObject target = MarkerUtils.getTargetObject(rs, marker);
        if (target == null) {
            return;
        }
    	IFeatureProvider fp = getDiagramTypeProvider().getFeatureProvider();
        PictogramElement pe = MarkerUtils.getContainerShape(fp, marker);

        if (pe!=null)
        	selectPictogramElements(new PictogramElement[] {pe});
        if (pe == null || PropertyUtil.getPropertySheetView() == null) {
			ObjectEditingDialog dialog = new ObjectEditingDialog(this, target);
			ObjectEditingDialog.openWithTransaction(dialog);
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    	super.doSave(monitor);

		Resource resource = getResourceSet().getResource(modelUri, false);
		BPMN2ProjectValidator.validateOnSave(resource, monitor);
    }

    @Override
	public void doSaveAs() {
		IFile oldFile = getModelFile();
		SaveAsDialog saveAsDialog = new SaveAsDialog(getSite().getShell());
		saveAsDialog.setOriginalFile(oldFile);
		saveAsDialog.create();
		if (saveAsDialog.open() == SaveAsDialog.CANCEL) {
			return;
		}
		IPath newFilePath = saveAsDialog.getResult();
		if (newFilePath == null){
			return;
		}

        IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(newFilePath);
        IWorkbenchPage page = getSite().getPage();
        try {
        	// if new file exists, close its editor (if open) and delete the existing file
            if (newFile.exists()) {
    			IEditorPart editorPart = ResourceUtil.findEditor(page, newFile);
    			if (editorPart!=null)
	    			page.closeEditor(editorPart, false);
        		newFile.delete(true, null);
            }
            // make a copy
			oldFile.copy(newFilePath, true, null);
		} catch (CoreException e) {
			showErrorDialogWithLogging(e);
			return;
		}

        // change the Resource URI and save it to the new file
		URI newURI = URI.createPlatformResourceURI(newFile.getFullPath().toString(), true);
    	handleResourceMoved(bpmnResource,newURI);
    	doSave(null);
	}

	public void closeEditor() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				boolean closed = getSite().getPage().closeEditor(BPMN2Editor.this, false);
				if (!closed){
					// If close editor fails, try again with explicit editorpart
					// of the old file
					IFile oldFile = ResourcesPlugin.getWorkspace().getRoot().getFile(getModelPath());
					IEditorPart editorPart = ResourceUtil.findEditor(getSite().getPage(), oldFile);
					closed = getSite().getPage().closeEditor(editorPart, false);
				}
			}
		});
	}


	// Show error dialog and log the error
	private void showErrorDialogWithLogging(Exception e) {
		Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		ErrorUtils.showErrorWithLogging(status);
	}
	
	private void addWorkbenchListener() {
		if (workbenchListener==null) {
			workbenchListener = new IWorkbenchListener() {
				@Override
				public boolean preShutdown(IWorkbench workbench, boolean forced) {
					workbenchShutdown = true;
					return true;
				}

				@Override
				public void postShutdown(IWorkbench workbench) {
				}

			};
			PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);
		}
	}

	private void removeWorkbenchListener() {
		if (workbenchListener!=null) {
			PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener);
			workbenchListener = null;
		}
	}
	
	private void addSelectionListener() {
		if (selectionListener == null) {
			IWorkbenchPage page = getSite().getPage();
			selectionListener = new IPartListener2() {
				public void partActivated(IWorkbenchPartReference partRef) {
					IWorkbenchPart part = partRef.getPart(false);
					if (part instanceof BPMN2MultiPageEditor) {
						BPMN2MultiPageEditor mpe = (BPMN2MultiPageEditor)part;
						setActiveEditor(mpe.getDesignEditor());
					}
				}

				@Override
				public void partBroughtToTop(IWorkbenchPartReference partRef) {
					IWorkbenchPart part = partRef.getPart(false);
					if (part instanceof BPMN2MultiPageEditor) {
						BPMN2MultiPageEditor mpe = (BPMN2MultiPageEditor)part;
						setActiveEditor(mpe.getDesignEditor());
					}
				}

				@Override
				public void partClosed(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partDeactivated(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partOpened(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partHidden(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partVisible(IWorkbenchPartReference partRef) {
				}

				@Override
				public void partInputChanged(IWorkbenchPartReference partRef) {
				}
			};
			page.addPartListener(selectionListener);
		}
	}

	private void removeSelectionListener() {
		if (selectionListener!=null) {
			getSite().getPage().removePartListener(selectionListener);
			selectionListener = null;
		}
	}

	private void addMarkerChangeListener() {
		if (getModelFile()!=null) {
			if (markerChangeListener==null) {
				markerChangeListener = new BPMN2MarkerChangeListener(this);
		        getModelFile().getWorkspace().addResourceChangeListener(markerChangeListener, IResourceChangeEvent.POST_BUILD);
			}
		}
	}
	
	private void removeMarkerChangeListener() {
		if (markerChangeListener!=null) {
			getModelFile().getWorkspace().removeResourceChangeListener(markerChangeListener);
			markerChangeListener = null;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	// WorkspaceSynchronizer handlers called from delegate
	////////////////////////////////////////////////////////////////////////////////
	
	public boolean handleResourceChanged(Resource resource) {
		if (resource==bpmnResource) {
			URI newURI = resource.getURI();
			URI modelUri = getModelUri();
			Bpmn2Preferences preferences = getPreferences();
			if (!modelUri.equals(newURI)) {
				ModelHandlerLocator.remove(modelUri);
				modelUri = newURI;
				if (preferences!=null) {
					preferences.removePreferenceChangeListener(this);
					preferences.dispose();
					preferences = null;
				}
				targetRuntime = null;
				modelHandler = ModelHandlerLocator.createModelHandler(modelUri, (Bpmn2ResourceImpl)resource);
				ModelHandlerLocator.put(diagramUri, modelHandler);
				
		    	Bpmn2DiagramEditorInput input = (Bpmn2DiagramEditorInput)getEditorInput();
		    	input.updateUri(newURI);
		    	multipageEditor.setInput(input);
			}
		}
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (getEditorInput()!=null) {
					updateDirtyState();
			    	refreshTitle();
				}
			}
		});
		return true;
	}

	public boolean handleResourceDeleted(Resource resource) {
		closeEditor();
		return true;
	}

	public boolean handleResourceMoved(Resource resource, URI newURI) {
		URI oldURI = resource.getURI();
		// The XML loader uses a lazy reference loading: references to internal objects
		// are initialized as proxies until first accessed (with eGet()).
		// Before we change the URI, make sure all references are resolved
		// otherwise the proxy URI (of unresolved references) will still be the old one.
		TreeIterator<EObject> iter = resource.getAllContents();
		while (iter.hasNext()) {
			EObject o = iter.next();
			for (EReference r : o.eClass().getEAllReferences()) {
				// the eGet() will handle proxy resolving
				o.eGet(r);
			}
		}
		resource.setURI(newURI);
		
		if (resource == bpmnResource) {
			ModelHandlerLocator.remove(modelUri);
			modelUri = newURI;
			if (preferences!=null) {
				preferences.removePreferenceChangeListener(this);
				preferences.dispose();
				preferences = null;
			}
			targetRuntime = null;
			modelHandler = ModelHandlerLocator.createModelHandler(modelUri, (Bpmn2ResourceImpl)resource);
			ModelHandlerLocator.put(diagramUri, modelHandler);
			
	    	Bpmn2DiagramEditorInput input = (Bpmn2DiagramEditorInput)getEditorInput();
	    	input.updateUri(newURI);
	    	multipageEditor.setInput(input);
	    	
	    	handleResourceChanged(resource);
		}
		else if (diagramUri.equals(oldURI)) {
			ModelHandlerLocator.remove(diagramUri);
			diagramUri = newURI;
			ModelHandlerLocator.put(diagramUri, modelHandler);
		}

		return true;
	}

	@Override
	public void dispose() {
		// clear ID mapping tables if no more instances of editor are active
		int instances = 0;
		IWorkbenchPage[] pages = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages();
		for (IWorkbenchPage p : pages) {
			IEditorReference[] refs = p.findEditors(null, EDITOR_ID, IWorkbenchPage.MATCH_ID);
			for (IEditorReference r : refs) {
				if (r.getEditor(false) instanceof BPMN2MultiPageEditor) {
					if (((BPMN2MultiPageEditor)r.getEditor(false)).designEditor != this)
						++instances;
				}
			}
		}

		BPMN2Editor otherEditor = findOpenEditor(this, getEditorInput());
		if (otherEditor==null && diagramUri != null) {
			// we can delete the Graphiti Diagram file if there are no other
			// editor windows open for this BPMN2 file.
			File diagramFile = new File(diagramUri.toFileString());
			if (diagramFile.exists()) {
				try {
					diagramFile.delete();
				}
				catch (Exception e) {
				}
			}
		}
		if (modelHandler != null) {
			// TODO have to clear IDs in BasicBPMN2Editor?
			ModelUtil.clearIDs(modelHandler.getResource(), instances==0);
		}


		removeSelectionListener();
		if (instances==0)
			setActiveEditor(null);

		super.dispose();

		// get rid of temp files and folders, but NOT if the workbench is being shut down.
		// when the workbench is restarted, we need to have those temp files around!
		if (modelUri != null && !workbenchShutdown) {
			if (FileUtils.isTempFile(modelUri)) {
				FileUtils.deleteTempFile(modelUri);
			}
		}

		removeWorkbenchListener();
		removeMarkerChangeListener();
	}

	public static BPMN2Editor findOpenEditor(IEditorPart newEditor, IEditorInput newInput) {
		if (newEditor!=null && newInput!=null) {
			IWorkbenchPage[] pages = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages();
			for (IWorkbenchPage page : pages) {
				IEditorReference[] otherEditors = page.findEditors(newInput, null, IWorkbenchPage.MATCH_INPUT);
				for (IEditorReference ref : otherEditors) {
					IEditorPart part = ref.getEditor(true);
					if (part instanceof BPMN2MultiPageEditor) {
						BPMN2Editor otherEditor = ((BPMN2MultiPageEditor)part).getDesignEditor();
						if (otherEditor!=newEditor) {
							return otherEditor;
						}
					}
					else if (part instanceof BPMN2Editor) {
						BPMN2Editor otherEditor = (BPMN2Editor)part;
						if (otherEditor!=newEditor) {
							return otherEditor;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * input helper class for the BPMN2Editor, handles IDE specific functionality
	 *
	 * @author Flavio Donzé
	 */
	public class ExtendedEditorInputHelper extends EditorInputHelper {
		/** current open BPMNEditor with the same editor input */
		private BPMN2Editor otherEditor;

		@Override
		public void preSetInput(IEditorInput input, DefaultBPMN2Editor editor) {
			input = recreateInput(input, editor);

			// Check if this is a New Editor Window for an already open editor
			otherEditor = BPMN2Editor.findOpenEditor(editor,input);

			ResourceSet resourceSet = initializeResourceSet(input, editor);

			// Now create the BPMN2 model resource, or reuse the one from the already open editor.
			if (otherEditor==null) {
				editor.bpmnResource = createBPMN2Resource(editor, resourceSet);
			}
			else {
				editor.bpmnResource = otherEditor.bpmnResource;
			}
		}

		@Override
		public void postSetInput(IEditorInput input, DefaultBPMN2Editor editor) {
			super.postSetInput(input, editor);

			// Load error markers
			((BPMN2Editor) editor).loadMarkers();
		}

		@Override
		protected void importDiagram(IEditorInput input, DefaultBPMN2Editor editor) {
			if (otherEditor==null) {
				super.importDiagram(input, editor);
			}
		}
	}
}
