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
package org.eclipse.bpmn2.modeler.core.features.containers.participant;

import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.AbstractUpdateBaseElementFeature;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;

public class UpdateParticipantFeature extends AbstractUpdateBaseElementFeature<Participant> {

	public UpdateParticipantFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public IReason updateNeeded(IUpdateContext context) {
		return super.updateNeeded(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.func.IUpdate#update(org.eclipse.graphiti.features.context.IUpdateContext)
	 */
	@Override
	public boolean update(IUpdateContext context) {
		PictogramElement pe = context.getPictogramElement();

		if (pe instanceof ContainerShape) {
			ContainerShape containerShape = (ContainerShape) pe;
			DIUtils.updateDIShape(containerShape);
			
			Diagram diagram = Graphiti.getPeService().getDiagramForShape(containerShape);
			Participant businessObject = BusinessObjectUtil.getFirstElementOfType(containerShape, Participant.class);
			GraphicsAlgorithm rect = containerShape.getGraphicsAlgorithm();
			boolean isPushed = FeatureSupport.hasBpmnDiagram(businessObject);
			boolean isReference = FeatureSupport.isParticipantReference(diagram, businessObject);
			// order matters here
			if (isReference) {
				rect.setLineWidth(3);
				rect.setLineStyle(LineStyle.DASH);
				rect.setTransparency(0.4);
			}
			else if (isPushed) {
				rect.setLineWidth(3);
				rect.setLineStyle(LineStyle.SOLID);
				rect.setTransparency(0.0);
			}
			else {
				rect.setLineWidth(1);
				rect.setLineStyle(LineStyle.SOLID);
				rect.setTransparency(0.0);
			}
			return true;
		}
		return false;
	}
}