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
package org.eclipse.bpmn2.modeler.ui;

import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.emf.common.util.URI;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.FileEditorInput;

public final class Bpmn2DiagramEditorInput extends DiagramEditorInput {

	/**
	 * The memento key for the stored {@link URI} BPMN2 file string
	 */
	public static final String KEY_MODEL_URI = "org.eclipse.bpmn2.modeler.uri"; //$NON-NLS-1$

	private Bpmn2DiagramType initialDiagramType = Bpmn2DiagramType.NONE;
	private String targetNamespace;
	private BPMNDiagram bpmnDiagram;
	private URI modelUri;
	
	public Bpmn2DiagramEditorInput(URI modelUri, URI diagramUri, String providerId) {
		super(diagramUri, providerId);
		this.modelUri = modelUri;
	}
	
	public Bpmn2DiagramType getInitialDiagramType() {
		return initialDiagramType;
	}

	public void setInitialDiagramType(Bpmn2DiagramType initialDiagramType) {
		this.initialDiagramType = initialDiagramType;
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public URI  getModelUri() {
		return modelUri;
	}
	
	public String getToolTipText() {
		return modelUri.toPlatformString(true);
	}
	
	public String getName() {
		return URI.decode(modelUri.trimFileExtension().lastSegment());
	}
	
	/**
	 * Determine the Target Runtime for this IEditorInput. This method is only
	 * used during creation when the model file has not yet been populated, but
	 * we already know the targetNamespace and the initial diagram type to be
	 * constructed. This will only happen immediately after the New File wizard
	 * is finished.
	 * 
	 * @return the Target Runtime that owns the targetNamespace defined in this
	 *         IEditorInput, or null if the targetNamespace is empty or no
	 *         Target Runtime owns the targetNamespace.
	 */
	public TargetRuntime getRuntime() {
		return TargetRuntime.getRuntime(targetNamespace, initialDiagramType);
	}
	
	public void updateUri(URI diagramFileUri) {
		if (diagramFileUri.isPlatformResource()) {
			modelUri = diagramFileUri;
		}
		else
			super.updateUri(diagramFileUri);
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean superEquals = super.equals(obj);
		if (superEquals) {
			return true;
		}

		// Eclipse makes FileEditorInputs for files to be opened. Here we check if the file is actually the same
		// as the DiagramEditorInput uses. This is for preventing opening new editors for the same file.
		if (obj instanceof FileEditorInput) {

			String path = ((FileEditorInput) obj).getFile().getFullPath().toString();
			URI platformUri = URI.createPlatformResourceURI(path, true);
			if (platformUri.equals(modelUri))
				return true;
		}
		return false;
	}

	public BPMNDiagram getBpmnDiagram() {
		return bpmnDiagram;
	}

	public void setBpmnDiagram(BPMNDiagram bpmnDiagram) {
		this.bpmnDiagram = bpmnDiagram;
	}
	
	@Override
	public String getFactoryId() {
		return Bpmn2DiagramEditorInputFactory.class.getName();
	}

	public void saveState(IMemento memento) {
		super.saveState(memento);
		
		// Do not store anything for deleted objects
		boolean exists = exists();
		if (!exists) {
			return;
		}
		// Store object name, URI and diagram type provider ID
		memento.putString(KEY_MODEL_URI, this.modelUri.toString());
	}
}