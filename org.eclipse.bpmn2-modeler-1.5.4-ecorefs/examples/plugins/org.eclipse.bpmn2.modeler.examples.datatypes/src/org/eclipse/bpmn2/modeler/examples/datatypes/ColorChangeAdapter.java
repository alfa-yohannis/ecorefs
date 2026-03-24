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

package org.eclipse.bpmn2.modeler.examples.datatypes;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.modeler.core.model.ModelDecorator;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.impl.InternalTransactionalEditingDomain;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.util.IColorConstant;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 *
 */
public class ColorChangeAdapter extends AdapterImpl {
	
	public static void adapt(ContainerShape containerShape) {
		EObject be = BusinessObjectUtil.getBusinessObjectForPictogramElement(containerShape);
		if (be!=null) {
			for (Adapter a : be.eAdapters()) {
				if (a instanceof ColorChangeAdapter)
					return;
			}
			ColorChangeAdapter adapter = new ColorChangeAdapter();
			adapter.setTarget(containerShape);
			be.eAdapters().add(adapter);
			adapter.setTarget(containerShape);
			adapter.updateColor();
		}
	}
	
	@Override
	public void notifyChanged(Notification msg) {
		EStructuralFeature feature = (msg.getFeature() instanceof EStructuralFeature ? (EStructuralFeature)msg.getFeature() : null);
		if (msg.getEventType()==Notification.SET && feature!=null && feature.getName().contains("color")) {
			updateColor();
		}
	}
	
	private void updateColor() {
		final ContainerShape containerShape = (ContainerShape) getTarget();
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(containerShape);
		RGB foreground = null;
		RGB background = null;
		EStructuralFeature feature = ModelDecorator.getAnyAttribute(be, "bgcolor");
		if (feature!=null) {
			Object value = be.eGet(feature);
			if (value instanceof RGB) {
				background = (RGB) value;
			}
		}
		feature = ModelDecorator.getAnyAttribute(be, "fgcolor");
		if (feature!=null) {
			Object value = be.eGet(feature);
			if (value instanceof RGB) {
				foreground = (RGB) value;
			}
		}
		GraphicsAlgorithm ga = null;
		if (containerShape.getChildren().size()>0) {
			Shape shape = containerShape.getChildren().get(0);
			ga = shape.getGraphicsAlgorithm();
		}
		else
			ga = containerShape.getGraphicsAlgorithm();
		
		Bpmn2Preferences pref = Bpmn2Preferences.getInstance(be);
		ShapeStyle ss = new ShapeStyle( pref.getShapeStyle(be) );
		if (background!=null) {
			IColorConstant cc = ShapeStyle.RGBToColor(background);
			ss.setShapeBackground(cc);
			ss.setShapePrimarySelectedColor(ShapeStyle.darker(cc));
			ss.setShapeSecondarySelectedColor(ShapeStyle.lighter(cc));
		}
		if (foreground!=null) {
			IColorConstant cc = ShapeStyle.RGBToColor(foreground);
			ss.setShapeForeground(cc);
		}
		StyleUtil.applyStyle(ga, be, ss);
	}
}
