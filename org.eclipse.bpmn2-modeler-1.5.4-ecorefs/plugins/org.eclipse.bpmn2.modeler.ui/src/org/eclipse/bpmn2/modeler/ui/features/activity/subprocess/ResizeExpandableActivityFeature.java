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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.DefaultResizeBPMNShapeFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil.LineSegment;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.features.context.impl.ResizeShapeContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;

public class ResizeExpandableActivityFeature extends DefaultResizeBPMNShapeFeature {
	
	public ResizeExpandableActivityFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	@Override
	public void resizeShape(IResizeShapeContext context) {

		ResizeShapeContext resizeShapeContext = (ResizeShapeContext)context;

		ContainerShape containerShape = (ContainerShape) context.getPictogramElement();
		
		// save center point of original size
		Point containerShapeCenter = GraphicsUtil.getShapeCenter(containerShape);

		Activity activity = BusinessObjectUtil.getFirstElementOfType(containerShape, Activity.class);
		
		List<AnchorContainer> movedChildren = new ArrayList<AnchorContainer>();
		List<PictogramElement> containerChildren = FeatureSupport.getContainerChildren(containerShape);
		try {
			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(containerShape);
			BPMNShape bpmnShape = DIUtils.findBPMNShape(bpmnDiagram, activity);
			if (bpmnShape.isIsExpanded()) { // this is the current expanded state

				// Activity is expanded

				// This resize feature will be called from two places:
				// 1. a normal resize operation done by the user
				// 2. the ExpandFlowNodeFeature and CollapseFlowNodeFeature
				// make sure this shape really is collapsed before we update
				// the "collapsed size" property in the shape.
				if (FeatureSupport.isElementExpanded(containerShape)) {
					FeatureSupport.ExpandableActivitySizeCalculator sizeCalc = new FeatureSupport.ExpandableActivitySizeCalculator(resizeShapeContext);
					int deltaX = sizeCalc.deltaX;
					int deltaY = sizeCalc.deltaY;
					
					if (deltaX != 0) {
						for (PictogramElement pe : containerChildren) {
							GraphicsAlgorithm childGa = pe.getGraphicsAlgorithm();
							if (childGa!=null) {
								int x = childGa.getX() - deltaX;
								childGa.setX(x);
								if (pe instanceof ContainerShape)
									movedChildren.add((ContainerShape)pe);
							}
						}
					}
					
					if (deltaY != 0) {
						for (PictogramElement pe : containerChildren) {
							GraphicsAlgorithm childGa = pe.getGraphicsAlgorithm();
							if (childGa!=null) {
								int y = childGa.getY() - deltaY;
								childGa.setY(y);
								if (!movedChildren.contains(pe) && pe instanceof ContainerShape)
									movedChildren.add((ContainerShape)pe);
							}
						}
					}
				}
			
				super.resizeShape(context);

				// This resize feature will be called from two places:
				// 1. a normal resize operation done by the user
				// 2. the ExpandFlowNodeFeature and CollapseFlowNodeFeature
				// make sure this shape really is collapsed before we update
				// the "collapsed size" property in the shape.
				if (FeatureSupport.isElementExpanded(containerShape))
					FeatureSupport.updateExpandedSize(containerShape);
			}
			else {
				
				// Activity is collapsed

				for (PictogramElement pe : FeatureSupport.getContainerDecorators(containerShape)) {
					GraphicsAlgorithm childGa = pe.getGraphicsAlgorithm();
					if (childGa!=null) {
						childGa.setWidth(context.getWidth());
						childGa.setHeight(context.getHeight());
					}
				}
				
				super.resizeShape(context);
				
				if (!FeatureSupport.isElementExpanded(containerShape))
					FeatureSupport.updateCollapsedSize(containerShape);
			}

			List<ContainerShape> alreadyMoved = new ArrayList<ContainerShape>();
			moveNeighborShapes(containerShape, containerShapeCenter, alreadyMoved);
			
		} catch (Exception e) {
			Activator.logError(e);
		}
		
		FeatureSupport.updateConnections(getFeatureProvider(), movedChildren);
	}
	
	private void moveNeighborShapes(ContainerShape movedShape, Point movedShapeCenter, List<ContainerShape> alreadyMoved) {
		
		List<ContainerShape> movedShapes = new ArrayList<ContainerShape>();
		
		// we may have to move some shapes that are nearby to avoid overlapping
		LineSegment[] containerEdges = GraphicsUtil.getEdges(movedShape);
		ContainerShape movedShapeContainer = movedShape.getContainer();
		List<ContainerShape> neighborShapes = FeatureSupport.findAllShapes(movedShapeContainer, new ContainerShape[] { movedShape });
		for (ContainerShape s : neighborShapes) {
			if (GraphicsUtil.intersects(movedShape, s, 10) && s.getContainer()==movedShapeContainer) {
				// we'll have to move this guy
				GraphicsAlgorithm sGA = s.getGraphicsAlgorithm();
				int x0 = sGA.getX();
				int y0 = sGA.getY();
				int x1 = x0;
				int y1 = y0;
				Point p1 = GraphicsUtil.getShapeCenter(s);
				int dx = p1.getX() - movedShapeCenter.getX();
				int dy = p1.getY() - movedShapeCenter.getY();
				if (Math.abs(dx) > Math.abs(dy)) {
					// move this shape horizontally
					if (dx<0) {
						// left
						// right edge of this shape should be to the left
						// of containerShape, plus a little space between
						x1 = containerEdges[2].getStart().getX() - sGA.getWidth() - 20;
					}
					else {
						// right
						x1 = containerEdges[3].getStart().getX()  + 20;
					}
				}
				else {
					// move it vertically
					if (dy<0) {
						// up
						// bottom edge of this shape should be above
						// containerShape, plus a little space between
						y1 = containerEdges[0].getStart().getY() - sGA.getHeight() - 20;
					}
					else {
						// down
						y1 = containerEdges[1].getStart().getY() + 20;
					}
				}
				
				// set up for move
				movedShapes.add(s);
				MoveShapeContext moveContext = new MoveShapeContext(s);
				moveContext.setDeltaX(x1-x0);
				moveContext.setDeltaY(y1-y0);
				moveContext.setSourceContainer(movedShapeContainer);
				moveContext.setTargetContainer(movedShapeContainer);
				moveContext.setLocation(x1, y1);
				moveContext.putProperty(GraphitiConstants.ACTIVITY_MOVE_PROPERTY, true);

				IMoveShapeFeature moveFeature = getFeatureProvider().getMoveShapeFeature(moveContext);
				if (moveFeature.canMoveShape(moveContext)) {
					moveFeature.moveShape(moveContext);
				}
			}
		}
		
		for (ContainerShape shape : movedShapes) {
			if (!alreadyMoved.contains(shape)) {
				alreadyMoved.add(shape);
				moveNeighborShapes(shape, movedShapeCenter, alreadyMoved);
				FeatureSupport.updateConnections(getFeatureProvider(), shape);
			}
		}
	}
}
