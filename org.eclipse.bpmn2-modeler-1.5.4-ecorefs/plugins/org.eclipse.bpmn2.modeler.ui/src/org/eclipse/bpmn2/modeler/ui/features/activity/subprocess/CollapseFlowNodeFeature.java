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

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CustomFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.impl.ResizeShapeContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public class CollapseFlowNodeFeature extends AbstractBpmn2CustomFeature {

	private boolean hasDoneChanges = false;

	public CollapseFlowNodeFeature(IFeatureProvider fp) {
		super(fp);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Collapse"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {

		return "Collapse the Activity and hide contents"; //$NON-NLS-1$
	}

	@Override
	public String getImageId() {
		return ImageProvider.IMG_16_COLLAPSE;
	}

	@Override
	public boolean isAvailable(IContext context) {
		return true;
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			BaseElement be = BusinessObjectUtil.getFirstBaseElement(pes[0]);
			return FeatureSupport.isElementExpanded(be);
		}
		return false;
	}

	@Override
	public void execute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			PictogramElement pe0 = pes[0];
			Object bo = getBusinessObjectForPictogramElement(pe0);
			if (pe0 instanceof ContainerShape && bo instanceof FlowNode) {
				ContainerShape containerShape = (ContainerShape)pe0;
				FlowNode flowNode = (FlowNode)bo;
				try {
					BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(pe0);
					BPMNShape bpmnShape = DIUtils.findBPMNShape(bpmnDiagram, flowNode);
					if (bpmnShape.isIsExpanded()) {
						GraphicsAlgorithm ga = containerShape.getGraphicsAlgorithm();
						FeatureSupport.updateExpandedSize(containerShape);
						
						// SubProcess is expanded - resize to either previously
						// collapsed size or standard Task size
						// NOTE: children tasks will be set not-visible in LayoutExpandableActivityFeature
						
						IDimension newSize = FeatureSupport.getCollapsedSize(containerShape);
						int newWidth = newSize.getWidth();
						int newHeight = newSize.getHeight();
						int oldWidth = ga.getWidth();
						int oldHeight = ga.getHeight();

						ResizeShapeContext resizeContext = new ResizeShapeContext(containerShape);
						resizeContext.setX(ga.getX() + oldWidth/2 - newWidth/2);
						resizeContext.setY(ga.getY() + oldHeight/2 - newHeight/2);
						resizeContext.setWidth(newWidth);
						resizeContext.setHeight(newHeight);

						FeatureSupport.setElementExpanded(containerShape, false);
						
						IResizeShapeFeature resizeFeature = getFeatureProvider().getResizeShapeFeature(resizeContext);
						resizeFeature.resizeShape(resizeContext);
						
						bpmnShape.setIsExpanded(false);

						UpdateContext updateContext = new UpdateContext(containerShape);
						updateContext.putProperty(GraphitiConstants.FORCE_UPDATE_ALL, Boolean.TRUE);
						IUpdateFeature updateFeature = getFeatureProvider().getUpdateFeature(updateContext);
						updateFeature.update(updateContext);
						
						// layout the incoming and outgoing connections
						FeatureSupport.updateConnections(getFeatureProvider(), FeatureSupport.getConnections(containerShape), true);

						getDiagramEditor().selectPictogramElements(new PictogramElement[] {});
					}
					
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
