package org.eclipse.bpmn2.modeler.core.features;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractCreateFlowFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Connection;

/**
 * This feature class creates a "reference" to an existing BPMN business object.
 * The new PictogramElement is added to the current Graphiti Diagram, and a new
 * DI element (i.e. a BPMNShape or BPMNEdge) is created and added to the
 * BPMNDiagram that is being rendered by the Diagram.
 * 
 * This is used, for example, to create a reference to an existing Message Flow in a
 * diagram that has been "pushed down". {@see PushdownFeature}
 */
public class CreateConnectionReferenceFeature extends AbstractCreateConnectionFeature {

	ICreateConnectionFeature createFeature = null;
	BaseElement referencedBusinessObject;
	BPMNEdge referencedBpmnEdge;
	
	public CreateConnectionReferenceFeature(IFeatureProvider fp, BPMNEdge referencedBpmnEdge, BaseElement businessObject) {
	    super(fp,null,null);
	    this.referencedBpmnEdge = referencedBpmnEdge;
	    this.referencedBusinessObject = businessObject;
    }

	@Override
    public boolean canCreate(ICreateConnectionContext context) {
		createFeature = AbstractCreateFlowFeature.getCreateFeature(getFeatureProvider(), context, referencedBusinessObject);
		return createFeature!=null && createFeature.canCreate(context);
    }

	@Override
	public Connection create(ICreateConnectionContext context) {
		Connection connection = null;
		if (canCreate(context)) {
			context.putProperty(GraphitiConstants.BUSINESS_OBJECT, referencedBusinessObject);
			context.putProperty(GraphitiConstants.COPIED_BPMN_DI_ELEMENT, referencedBpmnEdge);
			connection = createFeature.create(context);
			context.putProperty(GraphitiConstants.PICTOGRAM_ELEMENT, connection);
		}
		return connection;
	}

	@Override
	public boolean canStartConnection(ICreateConnectionContext context) {
		return true;
	}
}