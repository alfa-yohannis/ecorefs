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
import org.eclipse.dd.dc.Point;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IMoveBendpointContext;
import org.eclipse.graphiti.features.impl.DefaultMoveBendpointFeature;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;

public class MoveBendpointFeature extends DefaultMoveBendpointFeature {

	public MoveBendpointFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean moveBendpoint(IMoveBendpointContext context) {
		boolean moved = super.moveBendpoint(context);
		
		try {
			FreeFormConnection connection = context.getConnection();
			int index = context.getBendpointIndex();

			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(connection);
			BaseElement element = (BaseElement) BusinessObjectUtil.getFirstElementOfType(connection, BaseElement.class);
			BPMNEdge edge = DIUtils.findBPMNEdge(bpmnDiagram, element);
			if (edge!=null) {
				BendpointConnectionRouter.setOldBendpointLocation(connection, context.getBendpoint());

				Point p = edge.getWaypoint().get(index+1);
				p.setX(context.getX());
				p.setY(context.getY());
				BendpointConnectionRouter.setMovedBendpoint(connection, index);
				FeatureSupport.updateConnection(getFeatureProvider(), connection);
			}
			
		} catch (Exception e) {
			Activator.logError(e);
		}
		return moved;
	}
}