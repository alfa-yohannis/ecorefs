package org.eclipse.bpmn2.modeler.core.features;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

/**
 * This feature class creates a "reference" to an existing BPMN business object.
 * The new PictogramElement is added to the current Graphiti Diagram, and a new
 * DI element (i.e. a BPMNShape or BPMNEdge) is created and added to the
 * BPMNDiagram that is being rendered by the Diagram.
 * 
 * This is used, for example, to create a reference to an existing Pool in a
 * diagram that has been "pushed down". {@see PushdownFeature}
 */
public class CreateShapeReferenceFeature<T extends BaseElement> extends AbstractBpmn2CreateFeature<T> {

	T referencedBusinessObject;
	BPMNShape referencedBpmnShape;
	
	public CreateShapeReferenceFeature(IFeatureProvider fp, BPMNShape referencedBpmnShape, T businessObject) {
	    this(fp);
	    this.referencedBpmnShape = referencedBpmnShape;
	    this.referencedBusinessObject = businessObject;
    }
	
	private CreateShapeReferenceFeature(IFeatureProvider fp) {
	    super(fp);
    }

	@Override
    public boolean canCreate(ICreateContext context) {
		return true;
    }

	@Override
	public Object[] create(ICreateContext context) {
		context.putProperty(GraphitiConstants.BUSINESS_OBJECT, referencedBusinessObject);
		context.putProperty(GraphitiConstants.COPIED_BPMN_DI_ELEMENT, referencedBpmnShape);
		PictogramElement pe = addGraphicalRepresentation(context, referencedBusinessObject);
		context.putProperty(GraphitiConstants.PICTOGRAM_ELEMENT, pe);
		return new Object[] { pe };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CreateFeature#getBusinessObjectClass()
	 */
	public EClass getBusinessObjectClass() {
		return referencedBusinessObject.eClass();
	}
}