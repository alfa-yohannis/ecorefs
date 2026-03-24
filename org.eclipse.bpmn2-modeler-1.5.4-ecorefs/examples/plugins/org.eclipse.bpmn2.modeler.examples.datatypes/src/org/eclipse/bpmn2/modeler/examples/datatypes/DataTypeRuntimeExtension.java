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

import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.modeler.ui.AbstractBpmn2RuntimeExtension;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.swt.graphics.RGB;

/**
 *
 */
public class DataTypeRuntimeExtension extends AbstractBpmn2RuntimeExtension {

	public final static String TARGET_NAMESPACE = "http://org.eclipse.bpmn2.modeler.examples.datatype";
	
	@Override
	public void notify(LifecycleEvent event) {
		if (event.eventType.equals(EventType.BUSINESSOBJECT_CREATED)) {
			// As soon as the Business Object is created initialize the foreground
			// and background color attributes to their defaults as set in the
			// User Preferences for this object type.
			EObject be = (EObject) event.target;
			// We need an adapter to do this:
			ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(be);
			if (adapter!=null) {
				// Fetch the colors from User Preferences
				Bpmn2Preferences pref = Bpmn2Preferences.getInstance(be);
				ShapeStyle ss = pref.getShapeStyle(be);
				RGB foreground = ShapeStyle.colorToRGB(ss.getShapeForeground());
				RGB background = ShapeStyle.colorToRGB(ss.getShapeBackground());
				EStructuralFeature feature;
				// does this object have a "bgcolor" extension attribute?
				feature = adapter.getFeature("bgcolor");
				if (feature!=null) {
					// yes, set the default value
					be.eSet(feature,background);
				}
				// does this object have a "fgcolor" extension attribute?
				feature = adapter.getFeature("fgcolor");
				if (feature!=null) {
					// yes, set it
					be.eSet(feature,foreground);
				}
			}
		}
		if (event.eventType.equals(EventType.PICTOGRAMELEMENT_ADDED) && event.target instanceof ContainerShape) {
			ColorChangeAdapter.adapt((ContainerShape) event.target);
		}
	}

	@Override
	public String getTargetNamespace(Bpmn2DiagramType diagramType) {
		return TARGET_NAMESPACE;
	}
}
