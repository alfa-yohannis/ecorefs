/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
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

package org.eclipse.bpmn2.modeler.core.features.containers;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.DefaultResizeBPMNShapeFeature;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;

/**
 * Abstract base class for the Resize Feature for Lanes and Pools. This class
 * handles Label and Connection updating after the Pool/Lane hierarchy has been
 * resized, and also creates a list of non-Lane descendant shapes (i.e. Tasks,
 * Events, Gateways, etc.) contained in the root-level Pool or Lane.
 * <p>
 * The Pool/Lane Resize Feature specializations must implement {@link #resizeHeight()}
 * and {@link #resizeWidth()}
 */
public abstract class AbstractResizeContainerFeature extends DefaultResizeBPMNShapeFeature {

	protected ContainerShape rootContainer;
	protected List<PictogramElement> descendants = new ArrayList<PictogramElement>();
	protected boolean isHorizontal;
	protected Stack<Point> containerPos = new Stack<Point>();

	/**
	 * @param fp the Feature Provider
	 */
	public AbstractResizeContainerFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	abstract protected void resizeHeight(IResizeShapeContext context);
	abstract protected void resizeWidth(IResizeShapeContext context);
	
	@Override
	protected void preResizeShape(IResizeShapeContext context) {
		ContainerShape containerShape = (ContainerShape) context.getShape();

		if (rootContainer==null) {
			super.preResizeShape(context);

			rootContainer = FeatureSupport.getRootContainer(containerShape);
			isHorizontal = FeatureSupport.isHorizontal(rootContainer);
			descendants = FeatureSupport.getPoolAndLaneDescendants(rootContainer);
			
		}
		
		GraphicsAlgorithm laneGa = containerShape.getGraphicsAlgorithm();
		Point p = Graphiti.getCreateService().createPoint(laneGa.getX(),laneGa.getY());
		containerPos.push(p);
	}
	
	/**
	 * Adjust location of children so that a resize up or left
	 * leaves them in the same location relative to the diagram.
	 * This allows the user to create (or remove) space between
	 * the Lane's edge and the contained activities.
	 */
	@Override
	protected void postResizeShape(IResizeShapeContext context) {
		ContainerShape containerShape = (ContainerShape) context.getShape();
		
		Point pos = containerPos.pop();
		if (context.getDirection()==IResizeShapeContext.DIRECTION_NORTH ||
				context.getDirection()==IResizeShapeContext.DIRECTION_SOUTH ||
				context.getDirection()==IResizeShapeContext.DIRECTION_WEST ||
				context.getDirection()==IResizeShapeContext.DIRECTION_NORTH_WEST ||
				context.getDirection()==IResizeShapeContext.DIRECTION_NORTH_EAST ||
				context.getDirection()==IResizeShapeContext.DIRECTION_SOUTH_WEST) {
			int deltaX = pos.getX() - context.getX();
			int deltaY = pos.getY() - context.getY();
			// we'll need to use this as the offset for MOVABLE Labels
			Point offset = Graphiti.getCreateService().createPoint(deltaX, deltaY);
			
			List<ContainerShape> movedShapes = new ArrayList<ContainerShape>();
			for (PictogramElement pe : descendants) {
				if (containerShape.getChildren().contains(pe)) {
					GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
					Graphiti.getLayoutService().setLocation(ga, ga.getX() + deltaX, ga.getY() + deltaY);
					FeatureSupport.updateLabel(getFeatureProvider(), pe, offset);
					if (pe instanceof ContainerShape)
						movedShapes.add((ContainerShape)pe);
				}
			}
			
			FeatureSupport.updateConnections(getFeatureProvider(), movedShapes);
		}
		
		DIUtils.updateDIShape(containerShape);
		if (rootContainer!=containerShape) {
			DIUtils.updateDIShape(rootContainer);
		}

		FeatureSupport.updateLabel(getFeatureProvider(), rootContainer, null);

		super.postResizeShape(context);
	}
	
}
