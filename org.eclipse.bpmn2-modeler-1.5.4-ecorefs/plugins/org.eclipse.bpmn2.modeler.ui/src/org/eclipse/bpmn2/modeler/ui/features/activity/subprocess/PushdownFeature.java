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
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.ui.features.activity.subprocess;

import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.Lane;
import org.eclipse.bpmn2.LaneSet;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.choreography.ChoreographyUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.dd.di.DiagramElement;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.osgi.util.NLS;

/**
 * @author Bob Brodt
 *
 */
public class PushdownFeature extends AbstractPushPullFeature {

	
	/**
	 * @param fp
	 */
	public PushdownFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	@Override
	public String getName() {
	    return Messages.PushdownFeature_Name;
	}
	
	@Override
	public String getDescription() {
		if (description==null)
			description = Messages.PushdownFeature_Description;
		return description;
	}

	@Override
	public String getImageId() {
		return ImageProvider.IMG_16_PUSHDOWN;
	}

	@Override
	public boolean isAvailable(IContext context) {
		if (context instanceof ICustomContext) {
			PictogramElement[] pes = ((ICustomContext)context).getPictogramElements();
			if (pes != null && pes.length == 1) {
				PictogramElement pe = pes[0];
				if (!ChoreographyUtil.isChoreographyParticipantBand(pe)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			PictogramElement pe = pes[0];
			if (ChoreographyUtil.isChoreographyParticipantBand(pe))
				return false;
			Object bo = getBusinessObjectForPictogramElement(pe);
			description = NLS.bind(Messages.PushdownFeature_Description_1,ModelUtil.getLabel(bo));
			
			if (bo instanceof Participant) {
				if (FeatureSupport.isParticipantReference(getDiagram(), (Participant)bo))
					return false;
				bo = ((Participant)bo).getProcessRef();
			}
			if (bo instanceof FlowElementsContainer) {
				return DIUtils.findBPMNDiagram((BaseElement)bo) == null;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.custom.ICustomFeature#execute(org.eclipse.graphiti.features.context.ICustomContext)
	 */
	@Override
	public void execute(ICustomContext context) {
		// we already know there's one and only one PE element in canExecute() and that it's
		// a ContainerShape for a Flow Elements Container object (e.g. a Pool or SubProcess)
		containerShape = (ContainerShape)context.getPictogramElements()[0];
		
		// get the BPMN Diagram that this container lives in
		oldBpmnDiagram = DIUtils.findBPMNDiagram(containerShape);

		// get the BPMNShape for the container
		bpmnShape = null;
		Object bo = getBusinessObjectForPictogramElement(containerShape);
		if (bo instanceof Participant) {
			bpmnShape = DIUtils.findBPMNShape(oldBpmnDiagram, (Participant)bo);
			// the business object for a Pool is its referenced Process
			businessObject = ((Participant)bo).getProcessRef();
		}
		else if (bo instanceof FlowElementsContainer) {
			businessObject = (FlowElementsContainer)bo;
			bpmnShape = DIUtils.findBPMNShape(oldBpmnDiagram, businessObject);
		}
		
		oldDiagram = DIUtils.findDiagram(getDiagramBehavior(), oldBpmnDiagram);

		// create the new BPMNDiagram and its Graphiti Diagram
        newBpmnDiagram = DIUtils.createBPMNDiagram(businessObject);
		newDiagram = DIUtils.getOrCreateDiagram(getDiagramBehavior(), newBpmnDiagram);
		
		// copy the BPMN DI elements into the new BPMNDiagram
		collectDiagramElements(businessObject, oldBpmnDiagram);

		// collect all Shapes and Connections from old Diagram
		collectShapes(containerShape);
		collectConnections(containerShape);

		// calculate a bounding rectangle which contains the source container's descendants...
		boundingRectangle = calculateBoundingRectangle(containerShape, childShapes);
		// ...and move shapes from container into new Diagram
		moveShapes(oldDiagram, newDiagram, boundingRectangle.x, boundingRectangle.y);
		// update bounding rectangle after the move
		boundingRectangle = GraphicsUtil.getBoundingRectangle(childShapes);
		
		// move Connections from old Diagram to new Diagram
		moveConnections(containerShape, newDiagram);
		moveDiagramElements(oldBpmnDiagram, newBpmnDiagram);

		// move associated Graphiti data structures
		moveGraphitiData(oldDiagram, newDiagram);

		// collapse the SubProcess if applicable
		if (FeatureSupport.isExpandableElement(businessObject)) {
			bpmnShape.setIsExpanded(true);
			CollapseFlowNodeFeature collapseFeature = new CollapseFlowNodeFeature(getFeatureProvider());
			collapseFeature.execute(context);
		}

		// let the feature provider know there's a new diagram now
		getFeatureProvider().getDiagramTypeProvider().resourceReloaded(newDiagram);
		
//		GraphicsUtil.debug = true;
		FeatureSupport.updateConnections(getFeatureProvider(), internalConnections, true);
//		GraphicsUtil.debug = false;
	}
	
	@Override
	protected void collectDiagramElements(FlowElementsContainer businessObject, BPMNDiagram source) {
		for (LaneSet ls : businessObject.getLaneSets()) {
			for (Lane l : ls.getLanes()) {
				DiagramElement de = DIUtils.findDiagramElement(source, l);
				if (de!=null)
					diagramElements.add(de);
			}
		}
		// super
		for (FlowElement fe : businessObject.getFlowElements()) {
			DiagramElement de = DIUtils.findDiagramElement(source, fe);
			if (de!=null)
				diagramElements.add(de);
			if (fe instanceof FlowElementsContainer) {
				collectDiagramElements((FlowElementsContainer)fe, source);
			}
		}
	}

	@Override
	protected void collectShapes(ContainerShape source) {
		for (Shape s : source.getChildren()) {
			if (s instanceof ContainerShape) {
				childShapes.add(s);
				Shape l = FeatureSupport.getLabelShape(s);
				if (l!=null)
					childShapes.add(l);
			}
		}
	}
	
	@Override
	protected void moveGraphitiData(Diagram source, Diagram target) {
		for (Shape s : childShapes) {
			// PictogramLinks is not a containment list, so each link needs to be
			// explicitly removed from the old Diagram and added to the new one.
			if (s.getLink()!=null) {
				source.getPictogramLinks().remove(s.getLink());
				target.getPictogramLinks().add(s.getLink());
			}
		}
		for (Connection c : internalConnections) {
			if (c.getLink()!=null) {
				source.getPictogramLinks().remove(c.getLink());
				target.getPictogramLinks().add(c.getLink());
			}
		}
		// Colors, Fonts and Styles on the other hand, are containment lists so we
		// need to make copies of these.
	}

	@Override
	protected Point getChildOffset(ContainerShape targetContainerShape) {
		return GraphicsUtil.createPoint(50, 50);
	}
	
	@Override
	protected Rectangle calculateBoundingRectangle(ContainerShape containerShape, List<Shape> childShapes) {
		// calculate bounding rectangle for all children shapes
		Point offset = getChildOffset(containerShape);
		Rectangle rect = GraphicsUtil.getBoundingRectangle(childShapes);
		rect.x -= offset.getX();
		rect.y -= offset.getY();
		return rect;
	}
}
