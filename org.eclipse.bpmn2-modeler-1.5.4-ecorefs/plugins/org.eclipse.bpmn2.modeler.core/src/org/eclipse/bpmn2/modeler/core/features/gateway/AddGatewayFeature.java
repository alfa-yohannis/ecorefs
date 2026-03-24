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
package org.eclipse.bpmn2.modeler.core.features.gateway;

import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2AddFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.label.AddShapeLabelFeature;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences.BPMNDIAttributeDefault;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.ShapeDecoratorUtil;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.mm.algorithms.Polygon;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

public abstract class AddGatewayFeature<T extends Gateway>
	extends AbstractBpmn2AddFeature<T> {

	public AddGatewayFeature(IFeatureProvider fp) {
		super(fp);
	}

	public IAddFeature getAddLabelFeature(IFeatureProvider fp) {
		return new AddShapeLabelFeature(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		return FeatureSupport.isValidFlowElementTarget(context);
	}

	@Override
	public PictogramElement add(IAddContext context) {
		T businessObject = getBusinessObject(context);

		int width = this.getWidth(context);
		int height = this.getHeight(context);
		// for backward compatibility with older files that included
		// the label height in the figure height
		if (width!=height) {
			width = height = Math.min(width, height);
		}

		int oldX = context.getX();
		int oldY = context.getY();
		adjustLocation(context, width, height);
		int x = context.getX();
		int y = context.getY();
		
		// Create a container for the gateway-symbol
		final ContainerShape containerShape = peService.createContainerShape(context.getTargetContainer(), true);
		final Rectangle gatewayRect = gaService.createInvisibleRectangle(containerShape);
		gaService.setLocationAndSize(gatewayRect, x, y, width, height);

		Shape gatewayShape = peService.createShape(containerShape, false);
		Polygon gatewayPolygon = ShapeDecoratorUtil.createGateway(gatewayShape, width, height);
		StyleUtil.applyStyle(gatewayPolygon, businessObject);
		gaService.setLocationAndSize(gatewayPolygon, 0, 0, width, height);

		boolean isImport = context.getProperty(GraphitiConstants.IMPORT_PROPERTY) != null;
		BPMNShape newBpmnShape = createDIShape(containerShape, businessObject, !isImport);
		if (preferences.getIsMarkerVisible()==BPMNDIAttributeDefault.ALWAYS_TRUE ||
				preferences.getIsMarkerVisible()==BPMNDIAttributeDefault.DEFAULT_TRUE) {
			newBpmnShape.setIsMarkerVisible(true);
		}
		else if (preferences.getIsMarkerVisible()==BPMNDIAttributeDefault.ALWAYS_FALSE) {
			newBpmnShape.setIsMarkerVisible(false);
		}
		else {
			BPMNShape oldBpmnShape = (BPMNShape)context.getProperty(GraphitiConstants.COPIED_BPMN_DI_ELEMENT);
			if (oldBpmnShape!=null) {
				newBpmnShape.setIsMarkerVisible( oldBpmnShape.isIsMarkerVisible() );
			}
		}
		
		// hook for subclasses to inject extra code
		decorateShape(context, containerShape, businessObject);
		peService.createChopboxAnchor(containerShape);
		
		((AddContext)context).setX(oldX);
		((AddContext)context).setY(oldY);
		splitConnection(context, containerShape);
		
		return containerShape;
	}
}