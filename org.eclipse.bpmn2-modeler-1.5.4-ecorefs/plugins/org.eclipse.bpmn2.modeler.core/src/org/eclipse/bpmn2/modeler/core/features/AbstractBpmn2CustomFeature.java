package org.eclipse.bpmn2.modeler.core.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.ui.editor.DiagramEditor;

public abstract class AbstractBpmn2CustomFeature extends AbstractCustomFeature {

	public AbstractBpmn2CustomFeature(IFeatureProvider fp) {
		super(fp);
		// TODO Auto-generated constructor stub
	}
	
	protected DiagramEditor getDiagramEditor() {
		return (DiagramEditor)getFeatureProvider().getDiagramTypeProvider().getDiagramBehavior().getDiagramContainer();
	}
}
