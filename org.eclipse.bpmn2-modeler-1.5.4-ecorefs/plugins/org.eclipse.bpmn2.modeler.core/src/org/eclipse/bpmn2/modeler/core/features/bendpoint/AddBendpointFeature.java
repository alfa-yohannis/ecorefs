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
package org.eclipse.bpmn2.modeler.core.features.bendpoint;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.BendpointConnectionRouter;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.dd.dc.DcFactory;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddBendpointContext;
import org.eclipse.graphiti.features.impl.DefaultAddBendpointFeature;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;

public class AddBendpointFeature extends DefaultAddBendpointFeature {

	public AddBendpointFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAddBendpoint(IAddBendpointContext context) {
		try {
			FreeFormConnection connection = context.getConnection();
			BaseElement element = (BaseElement) BusinessObjectUtil.getFirstElementOfType(connection, BaseElement.class);
			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(connection);
			BPMNEdge edge = DIUtils.findBPMNEdge(bpmnDiagram, element);
			return edge!=null;
		} catch (Exception e) {
			Activator.logError(e);
		}
		return false;
	}

	@Override
	public void addBendpoint(IAddBendpointContext context) {
		FreeFormConnection connection = context.getConnection();
		int index = context.getBendpointIndex();
		Point bp1;
		if (index<=0)
			bp1 = GraphicsUtil.createPoint(connection.getStart());
		else
			bp1 = connection.getBendpoints().get(index-1);
		Point bp2;
		if (index>=connection.getBendpoints().size())
			bp2 = GraphicsUtil.createPoint(connection.getEnd());
		else
			bp2 = connection.getBendpoints().get(index);
		Point m = GraphicsUtil.getMidpoint(bp1, bp2);
		BendpointConnectionRouter.setOldBendpointLocation(connection, m);
		
		super.addBendpoint(context);
		
		try {
			BaseElement element = (BaseElement) BusinessObjectUtil.getFirstElementOfType(connection, BaseElement.class);
			org.eclipse.dd.dc.Point p = DcFactory.eINSTANCE.createPoint();
			p.setX(context.getX());
			p.setY(context.getY());

			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(connection);
			BPMNEdge edge = DIUtils.findBPMNEdge(bpmnDiagram, element);
			edge.getWaypoint().add(index+1, p);
			BendpointConnectionRouter.setAddedBendpoint(connection, index);
			FeatureSupport.updateConnection(getFeatureProvider(), connection);
			
		} catch (Exception e) {
			Activator.logError(e);
		}
	}

}