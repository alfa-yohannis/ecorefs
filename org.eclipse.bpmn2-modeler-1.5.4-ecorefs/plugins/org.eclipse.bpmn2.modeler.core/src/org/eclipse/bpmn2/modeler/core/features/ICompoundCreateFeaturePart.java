package org.eclipse.bpmn2.modeler.core.features;

import java.util.List;

import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public interface ICompoundCreateFeaturePart {

	ICompoundCreateFeaturePart getParent();
	String getToolName();
	IContext getToolContext();
	List<PictogramElement> getPictogramElements();
	void addPictogramElement(PictogramElement pe);
}
