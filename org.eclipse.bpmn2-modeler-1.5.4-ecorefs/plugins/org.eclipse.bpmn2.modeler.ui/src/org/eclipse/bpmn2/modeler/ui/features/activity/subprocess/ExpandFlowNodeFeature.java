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

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.bpmn2.modeler.ui.features.choreography.ShowDiagramPageFeature;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.features.context.impl.ResizeShapeContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public class ExpandFlowNodeFeature extends ShowDiagramPageFeature {

	private final static String NAME = Messages.ExpandFlowNodeFeature_Name;
	private final static String DESCRIPTION = Messages.ExpandFlowNodeFeature_Description;
	
	private String name = NAME;
	private String description = DESCRIPTION;
	private boolean hasDoneChanges = false;
	
	public ExpandFlowNodeFeature(IFeatureProvider fp) {
	    super(fp);
    }
	
	@Override
	public String getName() {
	    return name;
	}
	
	@Override
	public String getDescription() {
	    return description;
	}

	@Override
	public String getImageId() {
		return ImageProvider.IMG_16_EXPAND;
	}

	@Override
	public boolean isAvailable(IContext context) {
		return true;
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		if (super.canExecute(context)) {
			name = super.getName();
			description = super.getDescription();
			return true;
		}
		else {
			name = NAME;
			description = DESCRIPTION;
		}
		
		boolean ret = false;
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			BaseElement be = BusinessObjectUtil.getFirstBaseElement(pes[0]);
			return !FeatureSupport.isElementExpanded(be);
		}
		return ret;
	}

	@Override
	public void execute(ICustomContext context) {
		if (super.canExecute(context)) {
			super.execute(context);
			return;
		}
		
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			PictogramElement pe0 = pes[0];
			Object bo = getBusinessObjectForPictogramElement(pe0);
			if (pe0 instanceof ContainerShape && bo instanceof FlowNode) {
				ContainerShape containerShape = (ContainerShape)pe0;
				FlowNode flowNode = (FlowNode)bo;
				try {
					BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(containerShape);
					BPMNShape bpmnShape = DIUtils.findBPMNShape(bpmnDiagram, flowNode);

					GraphicsAlgorithm ga = containerShape.getGraphicsAlgorithm();
					FeatureSupport.updateCollapsedSize(containerShape);
					
					// SubProcess is collapsed - resize to previously
					// expanded size or minimum size such that all children
					// are visible
					// NOTE: children tasks will be set visible in LayoutExpandableActivityFeature

					IDimension newSize = FeatureSupport.getExpandedSize(containerShape);
					int newWidth = newSize.getWidth();
					int newHeight = newSize.getHeight();
					int oldWidth = ga.getWidth();
					int oldHeight = ga.getHeight();

					ResizeShapeContext resizeContext = new ResizeShapeContext(containerShape);
					resizeContext.setWidth(newWidth);
					resizeContext.setHeight(newHeight);
					// don't set new position yet - this messes up the ResizeExpandableActivityFeature's
					// calculation of where to position child elements relative to previous parent's
					// location.
					int newX = ga.getX() + oldWidth/2 - newWidth/2;
					int newY = ga.getY() + oldHeight/2 - newHeight/2;
					resizeContext.setX(newX);
					resizeContext.setY(newY);
					resizeContext.setWidth(newWidth);
					resizeContext.setHeight(newHeight);

					// The layout feature gets called immediately after resize,
					// so we need to let it know that this expandable activity is now expanded
					// Note that we can't simply set the BPMNShape.isExpanded attribute here
					// because the resize feature needs to know the current state
					FeatureSupport.setElementExpanded(containerShape, true);
					
					IResizeShapeFeature resizeFeature = getFeatureProvider().getResizeShapeFeature(resizeContext);
					resizeFeature.resizeShape(resizeContext);

					bpmnShape.setIsExpanded(true);

					// Now set new position
					newWidth = resizeContext.getWidth();
					newHeight = resizeContext.getHeight();
					if (newX!=ga.getX() || newY!=ga.getY()) {
						MoveShapeContext moveContext = new MoveShapeContext(containerShape);
						moveContext.setDeltaX(oldWidth/2 - newWidth/2);
						moveContext.setDeltaY(oldHeight/2 - newHeight/2);
						moveContext.setSourceContainer(containerShape.getContainer());
						moveContext.setTargetContainer(containerShape.getContainer());
						moveContext.setLocation(newX, newY);
						IMoveShapeFeature moveFeature = getFeatureProvider().getMoveShapeFeature(moveContext);
						moveFeature.moveShape(moveContext);
					}

					UpdateContext updateContext = new UpdateContext(containerShape);
					updateContext.putProperty(GraphitiConstants.FORCE_UPDATE_ALL, Boolean.TRUE);
					IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
					updateFeature.update(updateContext);
					
					LayoutContext layoutContext = new LayoutContext(containerShape);
					getFeatureProvider().layoutIfPossible(layoutContext);
					
					List<ContainerShape> children = new ArrayList<ContainerShape>();
					for (PictogramElement pe : containerShape.getChildren()) {
						if (pe instanceof ContainerShape && FeatureSupport.hasBPMNShape(pe)) {
							children.add((ContainerShape)pe);
						}
					}
					for (ContainerShape s : children) {
						layoutContext = new LayoutContext(s);
						getFeatureProvider().layoutIfPossible(layoutContext);
					}
					
					// layout the incoming and outgoing connections
					FeatureSupport.updateConnections(getFeatureProvider(), FeatureSupport.getConnections(containerShape), true);
					
					hasDoneChanges = true;
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean hasDoneChanges() {
		return hasDoneChanges;
	}
	
}