/*******************************************************************************
 * Copyright (c) 2011 - 2015 Red Hat, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.di.DIImport;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.Bpmn2TabbedPropertySheetPage;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerResourceImpl;
import org.eclipse.bpmn2.modeler.core.model.ModelHandler;
import org.eclipse.bpmn2.modeler.core.model.ModelHandlerLocator;
import org.eclipse.bpmn2.modeler.core.model.ProxyURIConverterImplExtension;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.preferences.ModelEnablements;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntimeAdapter;
import org.eclipse.bpmn2.modeler.core.runtime.ToolPaletteDescriptor;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.DiagramEditorAdapter;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.bpmn2.modeler.help.IHelpContexts;
import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.Bpmn2DiagramEditorInput;
import org.eclipse.bpmn2.modeler.ui.diagram.Bpmn2ToolBehaviorProvider;
import org.eclipse.bpmn2.modeler.ui.property.PropertyTabDescriptorProvider;
import org.eclipse.bpmn2.modeler.ui.views.outline.BPMN2EditorOutlinePage;
import org.eclipse.bpmn2.modeler.ui.views.outline.BPMN2EditorSelectionSynchronizer;
import org.eclipse.bpmn2.modeler.ui.wizards.BPMN2DiagramCreator;
import org.eclipse.bpmn2.modeler.ui.wizards.FileService;
import org.eclipse.bpmn2.util.Bpmn2ResourceImpl;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.transaction.NotificationFilter;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.TransactionalEditingDomain.Lifecycle;
import org.eclipse.emf.transaction.impl.TransactionalEditingDomainImpl;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.graphiti.ui.internal.editor.GFPaletteRoot;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptorProvider;

@SuppressWarnings("restriction")
public class DefaultBPMN2Editor extends DiagramEditor implements IPreferenceChangeListener {

	static {
		TargetRuntime.createTargetRuntimes();
	}

	public static final String CONTRIBUTOR_ID = "org.eclipse.bpmn2.modeler.ui.PropertyContributor"; //$NON-NLS-1$

	protected ModelHandler modelHandler;
	protected URI modelUri;
	protected URI diagramUri;
	private boolean editable = true;

	protected BPMNDiagram bpmnDiagram;
	protected Bpmn2ResourceImpl bpmnResource;

	// We need this to find BPMN2 Editors that are already open for this file
	// Used when opening a New Editor window for an already open editor.
	private IEditorInput currentInput;
	private static ITabDescriptorProvider tabDescriptorProvider;
	private BPMN2EditingDomainListener editingDomainListener;
	private boolean selectionChanging;

	protected Bpmn2Preferences preferences;
	protected TargetRuntime targetRuntime;
	private ModelEnablements modelEnablements;
	private boolean importInProgress;
	private BPMN2EditorSelectionSynchronizer synchronizer;

	protected DiagramEditorAdapter editorAdapter;
	protected IPropertySheetPage propertySheetPage;
	protected IContentOutlinePage outlinePage;

	protected boolean saveInProgress = false;
	private static NotificationFilter filterNone = new NotificationFilter.Custom() {
		@Override
		public boolean matches(Notification notification) {
			return false;
		}
	};

	@Override
	public IEditorInput getEditorInput() {
		return currentInput;
	}

	protected DiagramEditorAdapter getEditorAdapter() {
		return editorAdapter;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		currentInput = input;

		if (this.getDiagramBehavior()==null) {
			super.init(site, input);
		}
		else if (input instanceof Bpmn2DiagramEditorInput) {
			bpmnDiagram = ((Bpmn2DiagramEditorInput)input).getBpmnDiagram();
			if (bpmnDiagram!=null) {
				setBpmnDiagram(bpmnDiagram);
			}
		}
	}

	/**
	 * Beware, creates a new input and changes this editor!
	 */
	protected Bpmn2DiagramEditorInput createNewDiagramEditorInput(IEditorInput input, Bpmn2DiagramType diagramType, String targetNamespace)
			throws PartInitException {

		modelUri = FileService.getInputUri(input);
		if (modelUri==null)
			throw new PartInitException(Messages.BPMN2Editor_Cannot_Create_Editor_Input);
		input = BPMN2DiagramCreator.createDiagram(input, modelUri, diagramType,targetNamespace,this);
		diagramUri = ((Bpmn2DiagramEditorInput)input).getUri();

		return (Bpmn2DiagramEditorInput)input;
	}

	/**
	 * Bypasses Graphiti's Persistency Behavior code and save only the BPMN2 model resource.
	 * This is only used after a successful Import if the BPMN2 model was changed in any way,
	 * e.g. missing DI elements were added.
	 */
	private void saveModelFile() {
		try {
			bpmnResource.save(null);
			((BasicCommandStack) getEditingDomain().getCommandStack()).saveIsDone();
			updateDirtyState();
		}
		catch (IOException e) {
			Activator.logError(e);
		}
	}

	protected EditorInputHelper getInputHelper() {
		return new EditorInputHelper();
	}

	@Override
	protected void setInput(IEditorInput input) {
		EditorInputHelper inputHelper = getInputHelper();
		inputHelper.preSetInput(input, this);
		super.setInput(input);
		inputHelper.postSetInput(input, this);
	}

	@Override
	protected DiagramEditorInput convertToDiagramEditorInput(IEditorInput input) throws PartInitException {
		IEditorInput newInput = createNewDiagramEditorInput(input, Bpmn2DiagramType.NONE, ""); //$NON-NLS-1$
		if (newInput==null)
			newInput = super.convertToDiagramEditorInput(input);
		return (DiagramEditorInput) newInput;
	}

	protected void importDiagram() {
		try {
			importInProgress = true;
			// make sure this guy is active, otherwise it's not selectable
			Diagram diagram = getDiagramTypeProvider().getDiagram();
			IFeatureProvider featureProvider = getDiagramTypeProvider().getFeatureProvider();
			diagram.setActive(true);
			Bpmn2DiagramEditorInput input = (Bpmn2DiagramEditorInput) getEditorInput();
			Bpmn2DiagramType diagramType = input.getInitialDiagramType();
			String targetNamespace = input.getTargetNamespace();

			if (diagramType != Bpmn2DiagramType.NONE) {
				bpmnDiagram = modelHandler.createDiagramType(diagramType, targetNamespace);
				featureProvider.link(diagram, bpmnDiagram);
				// If the bpmn file was missing DI elements, they would have been added by the importer
				// so save the file now in case it was changed.
				saveModelFile();
			} else if (targetNamespace != null && !targetNamespace.isEmpty()) {
				modelHandler.getDefinitions().setTargetNamespace(targetNamespace);
			}

			DIImport di = new DIImport(this, getPreferences());
			di.setModelHandler(modelHandler);

			di.generateFromDI();
		}
		finally {
			importInProgress = false;
		}
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isEditable() {
	    return editable;
	}

	@Override
	public boolean isDirty() {
		if (!editable)
			return false;
		if (getEditorInput()==null)
			return false;
		return super.isDirty();
	}

	@Override
	protected DiagramBehavior createDiagramBehavior() {
		DiagramBehavior diagramBehavior = new DefaultBPMN2EditorDiagramBehavior(this);
		return diagramBehavior;
	}

	public Bpmn2Preferences getPreferences() {
		if (preferences==null) {
			loadPreferences(getProject());
		}
		return preferences;
	}

	private void loadPreferences(IProject project) {
		preferences = Bpmn2Preferences.getInstance(project);
		preferences.addPreferenceChangeListener(this);
	}

	/**
	 * ID for tabbed property sheets.
	 *
	 * @return the contributor id
	 */
	@Override
	public String getContributorId() {
		return CONTRIBUTOR_ID;
	}

	public TargetRuntime getTargetRuntime() {
		if (targetRuntime==null) {
			targetRuntime = getTargetRuntime(getEditorInput());
		}
		return targetRuntime;
	}

	public ModelEnablements getModelEnablements() {
		String profileName = getPreferences().getDefaultToolProfile(getTargetRuntime());
		if (modelEnablements!=null) {
			if (!modelEnablements.getId().equals(profileName)) {
				modelEnablements = null;
			}

		}
		if (modelEnablements==null) {
			modelEnablements = getPreferences().getModelEnablements(getTargetRuntime(), profileName);
			if (modelEnablements.size()==0) {
				// This Target Runtime doesn't define a Tool Profile
				// so we'll use the one for Default Runtime
				profileName = getPreferences().getDefaultToolProfile(TargetRuntime.getDefaultRuntime());
				ModelEnablements defaultEnablements = getPreferences().getModelEnablements(TargetRuntime.getDefaultRuntime(), profileName);
				modelEnablements.copy(defaultEnablements);
			}
		}
		return modelEnablements;
	}

	protected TargetRuntime getTargetRuntime(IEditorInput input) {
		if (targetRuntime==null && input!=null) {
			 // If the project has not been configured for a specific runtime through the "BPMN2"
			 // project properties page (i.e. the target is "None") then allow the runtime extension
			 // plug-ins an opportunity to identify the given process file contents as their own.
			 // If none of the plug-ins respond with "yes, this file is targeted for my runtime",
			 // then use the "None" as the extension. This will configure the BPMN2 Modeler with
			 // generic property sheets and other default behavior.
			if (input instanceof Bpmn2DiagramEditorInput)
				targetRuntime = ((Bpmn2DiagramEditorInput)input).getRuntime();
			if (targetRuntime==null)
				targetRuntime = TargetRuntime.getRuntime(input);
		}
		return targetRuntime;
	}

	public void updatePalette() {
		GFPaletteRoot pr = (GFPaletteRoot)getPaletteRoot();
		if (pr!=null) {
			// force a reload of this
			modelEnablements = null;
			pr.updatePaletteEntries();
			Bpmn2ToolBehaviorProvider toolBehaviorProvider =
					(Bpmn2ToolBehaviorProvider)getDiagramTypeProvider().
					getCurrentToolBehaviorProvider();
			toolBehaviorProvider.createPaletteProfilesGroup(this, pr);
		}
	}


	@Override
	public void refreshTitle() {
		if (getEditorInput()!=null) {
			String name = getEditorInput().getName();
			setPartName(URI.decode(name));
		}
	}

	public BPMN2EditingDomainListener getEditingDomainListener() {
		if (editingDomainListener==null) {
			TransactionalEditingDomainImpl editingDomain = (TransactionalEditingDomainImpl)getEditingDomain();
			if (editingDomain==null) {
				return null;
			}
			editingDomainListener = new BPMN2EditingDomainListener(this);

			Lifecycle domainLifeCycle = editingDomain.getAdapter(Lifecycle.class);
			domainLifeCycle.addTransactionalEditingDomainListener(editingDomainListener);
		}
		return editingDomainListener;
	}

	public BasicDiagnostic getDiagnostics() {
		return getEditingDomainListener().getDiagnostics();
	}

	@Override
	protected SelectionSynchronizer getSelectionSynchronizer() {
		if (synchronizer == null)
			synchronizer = new BPMN2EditorSelectionSynchronizer();
		return synchronizer;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class required) {
		if (required==DiagramEditor.class) {
			return this;
		}
		if (required==ITabDescriptorProvider.class) {
			if (tabDescriptorProvider==null) {
				tabDescriptorProvider = new PropertyTabDescriptorProvider();
//				IWorkbenchPage page = getEditorSite().getPage();
//				String viewID = "org.eclipse.ui.views.PropertySheet"; //$NON-NLS-1$
//				try {
//					page.showView(viewID, null, IWorkbenchPage.VIEW_CREATE);
//					page.showView(viewID, null,  IWorkbenchPage.VIEW_ACTIVATE);
//				}
//				catch (Exception e) {}
			}
			return tabDescriptorProvider;
		}
		if (required==TargetRuntime.class)
			return getTargetRuntime();
		if (required==Bpmn2Preferences.class)
			return getPreferences();
		if (required == IPropertySheetPage.class) {
			if (propertySheetPage==null) {
				propertySheetPage = new Bpmn2TabbedPropertySheetPage(this);
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!propertySheetPage.getControl().isDisposed()) {
							propertySheetPage.getControl().addDisposeListener(new DisposeListener() {
								@Override
								public void widgetDisposed(DisposeEvent e) {
									propertySheetPage = null;
								}
							});
						}
					}
				});
			}
			return propertySheetPage;
		}
		if (required == SelectionSynchronizer.class) {
			return getSelectionSynchronizer();
		}
		if (required == IContentOutlinePage.class) {
			if (getDiagramTypeProvider() != null) {
				if (outlinePage==null) {
					outlinePage = new BPMN2EditorOutlinePage(this);
				}
				return outlinePage;
			}
		}
		if (required == ModelEnablements.class) {
			return getModelEnablements();
		}
		if (required == ToolPaletteDescriptor.class) {
			String profileName = getPreferences().getDefaultToolProfile(getTargetRuntime());
			return getTargetRuntime().getToolPalette(profileName);
		}
		if (required == NotificationFilter.class) {
			if (saveInProgress)
				return filterNone;
			else
				return null;
		}
		if (required==GraphicalViewer.class) {
			return getGraphicalViewer();
		}
		if (required==Resource.class) {
			return getResource();
		}

		return super.getAdapter(required);
	}

	@Override
	public void dispose() {
		if (targetRuntime != null) {
			targetRuntime.notify(new LifecycleEvent(EventType.EDITOR_SHUTDOWN, this, targetRuntime));
		}
		if (modelHandler!=null) {
			modelHandler.dispose();
		}
		if (preferences != null) {
			preferences.removePreferenceChangeListener(this);
		}

		// cancel the Property Sheet Page job
		if (propertySheetPage!=null)
			propertySheetPage.selectionChanged(this, null);

		// get rid of cached Property Tab Descriptors
		if (tabDescriptorProvider instanceof PropertyTabDescriptorProvider)
			((PropertyTabDescriptorProvider)tabDescriptorProvider).disposeTabDescriptors(bpmnResource);

		if (getResourceSet() != null) {
			getResourceSet().eAdapters().remove(getEditorAdapter());
		}

		super.dispose();

		if (modelUri != null) {
			ModelHandlerLocator.remove(modelUri);
		}

		if (preferences != null) {
			preferences.dispose();
		}

		currentInput = null;
	}

	public IPath getModelPath() {
		if (getModelFile()!=null)
			return getModelFile().getFullPath();
		return null;
	}

	public IProject getProject() {
		if (getModelFile()!=null)
			return getModelFile().getProject();
		return null;
	}

	public IFile getModelFile() {
		if (modelUri!=null) {
			String uriString = modelUri.trimFragment().toPlatformString(true);
			if (uriString!=null) {
				IPath fullPath = new Path(uriString);
				return ResourcesPlugin.getWorkspace().getRoot().getFile(fullPath);
			}
		}
		return null;
	}

	public URI getModelUri() {
		return modelUri;
	}

	public URI getDiagramUri() {
		return diagramUri;
	}

	public ModelHandler getModelHandler() {
		return modelHandler;
	}

	public Resource getResource() {
		return bpmnResource;
	}

	public ResourceSet getResourceSet() {
		TransactionalEditingDomain editingDomain = getEditingDomain();
		return editingDomain != null ? editingDomain.getResourceSet() : null;
	}

	public void refresh() {
		if (!importInProgress)
			getDiagramBehavior().getRefreshBehavior().refresh();
	}

	@Override
	public void createPartControl(Composite parent) {
		if (getGraphicalViewer()==null) {
			super.createPartControl(parent);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContexts.TOC);
		}
	}

	public BPMNDiagram getBpmnDiagram() {
		if (bpmnDiagram==null) {
			Definitions definitions = ModelUtil.getDefinitions(bpmnResource);
			if (definitions!=null && definitions.getDiagrams().size()>0)
				bpmnDiagram = definitions.getDiagrams().get(0);
		}

//		if (bpmnDiagram!=null) {
//			GraphicalViewer viewer = getGraphicalViewer();
//			mapDiagramToViewer.put(bpmnDiagram, viewer);
//		}
		return bpmnDiagram;
	}

	public void setBpmnDiagram(final BPMNDiagram bpmnDiagram) {
		// create a new Graphiti Diagram if needed
		Diagram diagram = DIUtils.getOrCreateDiagram(getDiagramBehavior(), bpmnDiagram);

		// clear current selection to avoid confusing the GraphicalViewer
		selectPictogramElements(new PictogramElement[] {});

		// Tell the DTP about the new Diagram
		getDiagramTypeProvider().resourceReloaded(diagram);
		getDiagramBehavior().getRefreshBehavior().initRefresh();
		setPictogramElementsForSelection(null);
		// set Diagram as contents for the graphical viewer and refresh
		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setContents(diagram);
		EditPart ep = viewer.getRootEditPart().getContents();
		if (ep instanceof AbstractGraphicalEditPart) {
			IFigure fig = ((AbstractGraphicalEditPart)ep).getFigure();
			fig.setBorder(new MarginBorder(0,0,50,50));
		}

		ConnectionLayerClippingStrategy.applyTo(viewer);

		getDiagramBehavior().refreshContent();

		// remember this for later
		this.bpmnDiagram = bpmnDiagram;
		modelEnablements = null;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		final PictogramElement selections[] = getSelectedPictogramElements();
//		long start = System.currentTimeMillis();
		try {
			saveInProgress = true;
//			System.out.print("Saving...");
			super.doSave(monitor);
		}
		finally {
			saveInProgress = false;
		}
//		System.out.println("done in "+(System.currentTimeMillis()-start)+" ms");
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				selectPictogramElements(selections);
			}
		});
	}

	@Override
	public boolean isSaveAsAllowed() {
		return getModelFile()!=null;
	}


	////////////////////////////////////////////////////////////////////////////////
	// Other handlers
	////////////////////////////////////////////////////////////////////////////////

	@Override
	public synchronized void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!selectionChanging && selection instanceof IStructuredSelection) {
			try {
				selectionChanging = true;
				// Filter out label shapes as these are controlled by their ContainerShape owners
				// For example a "Select All" key action would select Labels as well.
				Iterator iter = ((IStructuredSelection)selection).iterator();
				List<Object> filteredObjects = new ArrayList<Object>();
				List<PictogramElement> pictogramElements = new ArrayList<PictogramElement>();
				while (iter.hasNext()) {
					boolean addIt = true;
					Object o = iter.next();
					if (o instanceof EditPart) {
						Object model = ((EditPart)o).getModel();
						if (model instanceof PictogramElement) {
							if (FeatureSupport.isLabelShape((PictogramElement)model)) {
								addIt = false;
							}
							else {
								pictogramElements.add((PictogramElement)model);
							}
						}
					}
					if (addIt)
						filteredObjects.add(o);
				}
				IStructuredSelection filteredSelection = new StructuredSelection(filteredObjects);
		
				// Graphiti understands multipage editors
				// but apparently GEF doesn't
				super.selectionChanged(part,filteredSelection); // Graphiti's DiagramEditorInternal
				super.selectPictogramElements(pictogramElements.toArray(new PictogramElement[pictogramElements.size()]));
				
				updateActions(getSelectionActions()); // usually done in GEF's GraphicalEditor
			}
			finally {
				selectionChanging = false;
			}
		}
		// if the selected element is obscured by another shape
		// send it to the top of the z-stack.
		// FIXME: this should be done in the figure's UpdateFeature or LayoutFeature, not here.
//		final List<ContainerShape> moved = new ArrayList<ContainerShape>();
//		for (PictogramElement pe : getSelectedPictogramElements()) {
//			if (pe instanceof ContainerShape && !(pe instanceof Diagram)) {
//				final ContainerShape shape = (ContainerShape)pe;
//				ContainerShape container = shape.getContainer();
//				// make sure this shape has not been deleted
//				if (container==null)
//					continue;
//				int size = container.getChildren().size();
//				if (size>1) {
//					// don't send Choreography Participant bands, Pools or Lanes to front
//					// they're already there...
//					BaseElement baseElement = BusinessObjectUtil.getFirstBaseElement(shape);
//					if (baseElement instanceof Participant || baseElement instanceof Lane)
//						continue;
//					boolean obscured = false;
//					int index = container.getChildren().indexOf(shape);
//					for (int i=index+1; i<container.getChildren().size(); ++i) {
//						PictogramElement sibling = container.getChildren().get(i);
//						if (sibling instanceof ContainerShape &&
//								!FeatureSupport.isLabelShape((ContainerShape)sibling)) {
//							if (GraphicsUtil.intersects(shape, (ContainerShape)sibling)) {
//								boolean siblingIsBoundaryEvent = false;
//								if (baseElement instanceof Activity) {
//									BaseElement be = BusinessObjectUtil.getFirstBaseElement(sibling);
//									for (BoundaryEvent boundaryEvent : ((Activity)baseElement).getBoundaryEventRefs()) {
//										if (be==boundaryEvent) {
//											siblingIsBoundaryEvent = true;
//											break;
//										}
//									}
//								}
//								if (!siblingIsBoundaryEvent) {
//									obscured = true;
//								}
//							}
//						}
//					}
//					// if the selected shape is an Activity, it may have Boundary Event shapes
//					// attached to it - these will have to be moved to the top so they're
//					// not obscured by the Activity.
//					if (baseElement instanceof Activity) {
//						for (BoundaryEvent be : ((Activity)baseElement).getBoundaryEventRefs()) {
//							for (PictogramElement child : container.getChildren()) {
//								if (child instanceof ContainerShape && BusinessObjectUtil.getFirstBaseElement(child) == be) {
//									index = container.getChildren().indexOf(child);
//									for (int i=index+1; i<container.getChildren().size(); ++i) {
//										PictogramElement sibling = container.getChildren().get(i);
//										if (sibling!=shape && sibling instanceof ContainerShape) {
//											if (GraphicsUtil.intersects((ContainerShape)child, (ContainerShape)sibling)) {
//												obscured = true;
//												moved.add((ContainerShape)child);
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//					if (obscured) {
//						moved.add(0,shape);
//					}
//				}
//			}
//		}
//		if (!moved.isEmpty()) {
//			Display.getDefault().asyncExec(new Runnable() {
//				@Override
//				public void run() {
//					getEditingDomain().getCommandStack().execute(new RecordingCommand(getEditingDomain()) {
//						@Override
//						protected void doExecute() {
//							for (ContainerShape child : moved) {
//								GraphicsUtil.sendToFront(child);
//							}
//						}
//					});
//				}
//			});
//		}
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		getPreferences().reload();

		if (event.getKey().contains("/"+Bpmn2Preferences.PREF_MODEL_ENABLEMENT+"/")) //$NON-NLS-1$ //$NON-NLS-2$
			modelEnablements = null;

		if (event.getKey().contains(Bpmn2Preferences.PREF_SHOW_ADVANCED_PROPERTIES) ||
				event.getKey().contains("/"+Bpmn2Preferences.PREF_TOOL_PROFILE+"/")) { //$NON-NLS-1$ //$NON-NLS-2$
			// get rid of cached Property Tab Descriptors
			if (tabDescriptorProvider instanceof PropertyTabDescriptorProvider)
				((PropertyTabDescriptorProvider)tabDescriptorProvider).disposeTabDescriptors(bpmnResource);
		}

		if (event.getKey().contains("/"+Bpmn2Preferences.PREF_SHAPE_STYLE+"/")) { //$NON-NLS-1$ //$NON-NLS-2$
			int i = event.getKey().lastIndexOf('/');
			if (i<=0)
				return;
			// Get the object type whose ShapeStyle has changed (e.g. "Task")
			// and change it if possible. This needs to run in a transaction.
			final String name = event.getKey().substring(i+1);
			getEditingDomain().getCommandStack().execute(new RecordingCommand(getEditingDomain()) {
				@Override
				protected void doExecute() {
					IFeatureProvider fp = DefaultBPMN2Editor.this.getDiagramTypeProvider().getFeatureProvider();
					// Collect all PictogramElements and their corresponding GraphicsAlgorithms
					// to which the ShapeStyle change applies.
					Resource resource = getDiagramTypeProvider().getDiagram().eResource();
					for (PictogramElement pe : ModelUtil.getAllObjectsOfType(resource, PictogramElement.class)) {
						BaseElement be = BusinessObjectUtil.getFirstElementOfType(pe, BaseElement.class);
						// The Business Object class name must match the ShapeStyle type
						if (be!=null && be.eClass().getName().equals(name)) {
							// find this PE's GraphicsAlgorithrms that has the
							// PREF_SHAPE_STYLE property set - this is the GA to
							// which the ShapeStyle applies.
							GraphicsAlgorithm ga = StyleUtil.getShapeStyleContainer(pe);
							// If the ShapeStyle for this BaseElement has already
							// been changed by the user, do not reset it.
							String style = ShapeStyle.encode(ShapeStyle.getShapeStyle(be));
							if (style.equals(event.getNewValue())) {
								StyleUtil.applyStyle(ga, be);
								if (pe instanceof Shape && FeatureSupport.isLabelShape(pe)) {
									UpdateContext context = new UpdateContext(pe);
									IUpdateFeature feature = fp.getUpdateFeature(context);
									if (feature!=null && feature.canUpdate(context)) {
										feature.update(context);
									}
								}
							}
						}
					}
				}
			});
		}

		if (event.getKey().contains(ShapeStyle.Category.GRID.toString())) { //$NON-NLS-1$
			getEditingDomain().getCommandStack().execute(new RecordingCommand(getEditingDomain()) {
				@Override
				protected void doExecute() {
					ShapeStyle ss = getPreferences().getShapeStyle(ShapeStyle.Category.GRID);
					Diagram diagram = getDiagramTypeProvider().getDiagram();
					diagram.setGridUnit(ss.getDefaultWidth());
					diagram.setVerticalGridUnit(ss.getDefaultHeight());
					diagram.setSnapToGrid(ss.getSnapToGrid());
					GraphicsAlgorithm ga = diagram.getGraphicsAlgorithm();
					IGaService gaService = Graphiti.getGaService();
					ga.setForeground(gaService.manageColor(diagram, ss.getShapeForeground()));
					refresh();
					getGraphicalControl().redraw();
					getGraphicalControl().getDisplay().update();
				}
			});
		}

		if (event.getKey().contains(ShapeStyle.Category.CANVAS.toString())) { //$NON-NLS-1$
			getEditingDomain().getCommandStack().execute(new RecordingCommand(getEditingDomain()) {
				@Override
				protected void doExecute() {
					ShapeStyle ss = getPreferences().getShapeStyle(ShapeStyle.Category.CANVAS);
					Diagram diagram = getDiagramTypeProvider().getDiagram();
					GraphicsAlgorithm ga = diagram.getGraphicsAlgorithm();
					IGaService gaService = Graphiti.getGaService();
					ga.setBackground(gaService.manageColor(diagram, ss.getShapeBackground()));
					refresh();
					getGraphicalControl().redraw();
				}
			});
		}
	}

	public static IEditorPart openEditor(URI modelURI) {
		IEditorPart part = null;
		try {
			Bpmn2DiagramEditorInput input = BPMN2DiagramCreator.createDiagram(modelURI, Bpmn2DiagramType.NONE, ""); //$NON-NLS-1$
			part = BPMN2DiagramCreator.openEditor(input);
		}
		catch (Exception e) {
			Activator.logError(e);
		}

		return part;
	}

	@Override
	public void configureGraphicalViewer() {
	    super.configureGraphicalViewer();
	    // add zooming action with "CTRL + Mouse Wheel"
	    GraphicalViewer viewer = getGraphicalViewer();
	    viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
	}

	/**
	 * helper class used to handle setInput() of the editor, because of it's complexity split it up in own class,
	 * which may be overridden and customized
	 *
	 * @author Flavio Donz
	 *
	 */
	public class EditorInputHelper {

		/**
		 * method called in editor.setInput() previous to super.setInput()
		 *
		 * @param input
		 * @param editor
		 */
		public void preSetInput(IEditorInput input, DefaultBPMN2Editor editor) {
			input = recreateInput(input, editor);

			ResourceSet resourceSet = initializeResourceSet(input, editor);
			editor.bpmnResource = createBPMN2Resource(editor, resourceSet);
		}

		protected IEditorInput recreateInput(IEditorInput input, DefaultBPMN2Editor editor) {
			try {
				if (input instanceof Bpmn2DiagramEditorInput) {
					Bpmn2DiagramType diagramType = Bpmn2DiagramType.NONE;
					String targetNamespace = null;
					diagramType = ((Bpmn2DiagramEditorInput)input).getInitialDiagramType();
					targetNamespace = ((Bpmn2DiagramEditorInput)input).getTargetNamespace();
					input = editor.createNewDiagramEditorInput(input, diagramType, targetNamespace);
				}
				editor.currentInput = input;
			}
			catch (Exception e) {
				Activator.logError(e);
			}
			return input;
		}

		protected ResourceSet initializeResourceSet(IEditorInput input, DefaultBPMN2Editor editor) {
			// Determine which Target Runtime to use for this input and initialize the ResourceSet
			TargetRuntime targetRuntime = editor.getTargetRuntime(input);
			targetRuntime.notify(new LifecycleEvent(EventType.EDITOR_STARTUP,editor, targetRuntime));

			ResourceSet resourceSet = editor.getEditingDomain().getResourceSet();
			resourceSet.setURIConverter(new ProxyURIConverterImplExtension(editor.modelUri));
			resourceSet.eAdapters().add(editor.editorAdapter = new DiagramEditorAdapter(editor));

			// Tell the TargetRuntime about the ResourceSet. This allows the TargetRuntime to provide its
			// own ResourceFactory if needed.
			targetRuntime.registerExtensionResourceFactory(resourceSet);
			return resourceSet;
		}

		protected Bpmn2ResourceImpl createBPMN2Resource(DefaultBPMN2Editor editor, ResourceSet resourceSet) {
			Bpmn2ResourceImpl resource = (Bpmn2ResourceImpl) resourceSet.createResource(editor.modelUri, Bpmn2ModelerResourceImpl.BPMN2_CONTENT_TYPE_ID);
			// make sure resource has target runtime adapter
			TargetRuntimeAdapter.adapt(resource, getTargetRuntime());
			return resource;
		}

		/**
		 * method called in editor.setInput() after super.setInput()
		 *
		 * @param input
		 * @param editor
		 */
		public void postSetInput(IEditorInput input, DefaultBPMN2Editor editor) {
			// Hook a transaction exception handler so we can get diagnostics about EMF validation errors.
			editor.getEditingDomainListener();

			// This does the actual loading of the resource.
			// TODO: move the loading code to BPMN2PersistencyBehavior where it belongs,
			// and get rid of ModelHandler and ModelHandlerLocator
			editor.modelHandler = ModelHandlerLocator.createModelHandler(editor.modelUri, editor.bpmnResource);
			ModelHandlerLocator.put(editor.diagramUri, editor.modelHandler);

			TargetRuntime rt = editor.getTargetRuntime();
			rt.notify(new LifecycleEvent(EventType.EDITOR_INITIALIZED, editor, rt));

			importDiagram(input, editor);
		}

		protected void importDiagram(IEditorInput input, final DefaultBPMN2Editor editor) {
			try {
				editor.getPreferences().setDoCoreValidation(false);
				// Import the BPMNDI model that creates the Graphiti shapes, connections, etc.
				BasicCommandStack commandStack = (BasicCommandStack) editor.getEditingDomain().getCommandStack();
				commandStack.execute(new RecordingCommand(editor.getEditingDomain()) {
					@Override
					protected void doExecute() {
						editor.importDiagram();
					}
				});

				Definitions definitions = ModelUtil.getDefinitions(editor.bpmnResource);
				if (definitions!=null) {
					// we'll need this in case doSaveAs()
					((Bpmn2DiagramEditorInput)input).setTargetNamespace(definitions.getTargetNamespace());
					((Bpmn2DiagramEditorInput)input).setInitialDiagramType(ModelUtil.getDiagramType(editor));
				}
				// Reset the save point and initialize the undo stack
				commandStack.saveIsDone();
				commandStack.flush();
			}
			finally {
				editor.getPreferences().setDoCoreValidation(true);
			}
		}
	}
}
