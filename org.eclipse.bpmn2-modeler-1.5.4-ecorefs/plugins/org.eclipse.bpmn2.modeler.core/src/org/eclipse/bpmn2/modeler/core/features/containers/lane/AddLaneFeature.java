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
 * @author Ivar Meikas
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features.containers.lane;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Lane;
import org.eclipse.bpmn2.LaneSet;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2AddFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.label.AddShapeLabelFeature;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.dd.dc.Bounds;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ITargetContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.mm.algorithms.AbstractText;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

public class AddLaneFeature extends AbstractBpmn2AddFeature<Lane> {
	
	public AddLaneFeature(IFeatureProvider fp) {
		super(fp);
	}

	public IAddFeature getAddLabelFeature(IFeatureProvider fp) {
		return new AddShapeLabelFeature(fp) {
			
			@Override
			protected AbstractText createText(Shape labelShape, String labelText) {
				// need to override the default MultiText created by super
				// because the Graphiti layout algorithm doesn't work as
				// expected when text angle is -90
				return gaService.createText(labelShape, labelText);
			}
	
			@Override
			public void applyStyle(AbstractText text, BaseElement be) {
				super.applyStyle(text, be);
				text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
				text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
			}
		};
	}
	
	@Override
	public boolean canAdd(IAddContext context) {
		// NOTE: This is slightly different from FeatureSupport.isValidFlowElementTarget()
		// because a Lane can be added to a Lane that is not a top-level Lane. This is not
		// the case for Activities, Events and Gateways.
		boolean intoDiagram = context.getTargetContainer() instanceof Diagram;
		boolean intoLane = FeatureSupport.isTargetLane(context);
		boolean intoParticipant = FeatureSupport.isTargetParticipant(context);
		boolean intoFlowElementContainer = FeatureSupport.isTargetFlowElementsContainer(context);
		boolean intoGroup = FeatureSupport.isTargetGroup(context);
		return (intoDiagram || intoLane || intoParticipant || intoFlowElementContainer) && !intoGroup;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		Lane businessObject = getBusinessObject(context);
 
		ContainerShape containerShape = peService.createContainerShape(context.getTargetContainer(), true);
		Rectangle rect = gaService.createRectangle(containerShape);
		StyleUtil.applyStyle(rect, businessObject);
		
		boolean isImport = context.getProperty(GraphitiConstants.IMPORT_PROPERTY) != null;
		boolean isCopy = context.getProperty(GraphitiConstants.COPIED_BPMN_DI_ELEMENT) != null;

		int width = this.getWidth(context);
		int height = this.getHeight(context);
		
		gaService.setLocationAndSize(rect, context.getX(), context.getY(), width, height);
		BPMNShape bpmnShape = createDIShape(containerShape, businessObject, !(isImport || isCopy));
		
		if (FeatureSupport.isTargetLane(context)) {
			Lane targetLane = FeatureSupport.getTargetLane(context);
			if (!isImport) {
				BPMNShape laneShape = findDIShape(targetLane);
				if (laneShape!=null)
					bpmnShape.setIsHorizontal(laneShape.isIsHorizontal());
			}
			businessObject.getFlowNodeRefs().addAll(targetLane.getFlowNodeRefs());
			targetLane.getFlowNodeRefs().clear();
		}
		
		if (FeatureSupport.isTargetParticipant(context)) {
			Participant targetParticipant = FeatureSupport.getTargetParticipant(context);
			Process targetProcess = targetParticipant.getProcessRef();
			if (!isImport) {
				BPMNShape participantShape = findDIShape(targetParticipant);
				if (participantShape!=null)
					bpmnShape.setIsHorizontal(participantShape.isIsHorizontal());
			}
			
			if (getNumberOfLanes(context) == 1) { // this is the first lane of the participant, move flow nodes
				moveFlowNodes(targetProcess, businessObject);
			}
		}

		boolean horz = bpmnShape.isIsHorizontal();
		FeatureSupport.setHorizontal(containerShape, horz);
		
		if (FeatureSupport.isTargetLane(context) || FeatureSupport.isTargetParticipant(context)) {
			for (Shape s : getFlowNodeShapes(context, businessObject)) {
				GraphicsUtil.sendToFront(s);
				s.setContainer(containerShape);
				
				for (EObject linkedObj : s.getLink().getBusinessObjects()) {
					if(linkedObj instanceof FlowNode) {
						businessObject.getFlowNodeRefs().add((FlowNode) linkedObj);
					}
				}
			}
			containerShape.setContainer(context.getTargetContainer());
		}

		// hook for subclasses to inject extra code
		((AddContext)context).setWidth(width);
		((AddContext)context).setHeight(height);
		decorateShape(context, containerShape, businessObject);

		peService.createChopboxAnchor(containerShape);

		return containerShape;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2AddFeature#decorateShape(org.eclipse.graphiti.features.context.IAddContext, org.eclipse.graphiti.mm.pictograms.ContainerShape, org.eclipse.bpmn2.BaseElement)
	 */
	protected void decorateShape(IAddContext context, ContainerShape containerShape, Lane businessObject) {
	}
	
	private void moveFlowNodes(Process targetProcess, Lane lane) {
		for (FlowElement element : targetProcess.getFlowElements()) {
			if (element instanceof FlowNode) {
				lane.getFlowNodeRefs().add((FlowNode) element);
			}
		}
	}

	private List<Shape> getFlowNodeShapes(IAddContext context, Lane lane) {
		List<FlowNode> nodes = lane.getFlowNodeRefs();
		List<Shape> shapes = new ArrayList<Shape>();
		for (Shape s : context.getTargetContainer().getChildren()) {
			Object bo = getBusinessObjectForPictogramElement(s);
			if (bo != null && nodes.contains(bo)) {
				shapes.add(s);
			}
		}
		return shapes;
	}

	private int getNumberOfLanes(ITargetContext context) {
		ContainerShape targetContainer = context.getTargetContainer();
		Object bo = getBusinessObjectForPictogramElement(targetContainer);
		if (bo instanceof Lane) {
			Lane lane = (Lane) bo;
			return lane.getChildLaneSet().getLanes().size();
		} else if (bo instanceof Participant) {
			List<LaneSet> laneSets = ((Participant) bo).getProcessRef().getLaneSets();
			if (laneSets.size() > 0) {
				return laneSets.get(0).getLanes().size();
			}
			return laneSets.size();
		} else if (bo instanceof SubProcess) {
			List<LaneSet> laneSets = ((SubProcess) bo).getLaneSets();
			if (laneSets.size() > 0) {
				return laneSets.get(0).getLanes().size();
			}
			return laneSets.size();
		}
		return 0;
	}
	
	private Bounds getPreviousBounds(IAddContext context) {
		EObject bo = (EObject) getBusinessObjectForPictogramElement(context.getTargetContainer());
		if (bo instanceof Participant) {
			List<LaneSet> laneSets = ((Participant) bo).getProcessRef().getLaneSets();
			List<Lane> lanes = null;
			
			if (laneSets.size() > 0 && laneSets.get(0).getLanes().size() > 1) {
				lanes = laneSets.get(0).getLanes();
				Lane lane = lanes.get(lanes.size() - 2); // get the lane created before, current lane is already included
				BPMNShape laneShape = findDIShape(lane);
				Bounds bounds = laneShape.getBounds();
				return bounds;
			}
		}
		return null;
	}
	
	
//	protected int getHeight(IAddContext context) {
//		int h = super.getHeight(context);
//		int w = super.getWidth(context);
//		if (!isHorizontal(context)) {
//			return Math.max(w, h);
//		}
//		return Math.min(w, h);
//	}
//	
//	/**
//	 * Gets the width of the new Pool based on User Preferences for size orientation.
//	 *
//	 * @param context the AddContext for the new shape
//	 * @return the width
//	 */
//	protected int getWidth(IAddContext context) {
//		int h = super.getHeight(context);
//		int w = super.getWidth(context);
//		if (!isHorizontal(context)) {
//			return Math.min(w, h);
//		}
//		return Math.max(w, h);
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2AddFeature#getBusinessObjectType()
	 */
	@Override
	public Class getBusinessObjectType() {
		return Lane.class;
	}
}