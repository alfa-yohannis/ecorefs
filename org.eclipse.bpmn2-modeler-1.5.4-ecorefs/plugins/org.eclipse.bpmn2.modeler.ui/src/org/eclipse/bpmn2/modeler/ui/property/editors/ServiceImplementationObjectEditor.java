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

package org.eclipse.bpmn2.modeler.ui.property.editors;

import java.util.Hashtable;

import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.ServiceTaskPropertiesAdapter;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 *
 */
public class ServiceImplementationObjectEditor extends NameAndURIObjectEditor {

	/**
	 * @param parent
	 * @param object
	 * @param feature
	 */
	public ServiceImplementationObjectEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object, feature);
		setPreferenceKey(Bpmn2Preferences.PREF_SERVICE_IMPLEMENTATIONS);
	}

	/**
	 * @param parent
	 * @param object
	 * @param feature
	 * @param featureEType
	 */
	public ServiceImplementationObjectEditor(AbstractDetailComposite parent, EObject object,
			EStructuralFeature feature, EClass featureEType) {
		super(parent, object, feature, featureEType);
		setPreferenceKey(Bpmn2Preferences.PREF_SERVICE_IMPLEMENTATIONS);
	}

	@Override
	protected Hashtable<String,Object> getChoiceOfValues(EObject object, EStructuralFeature feature) {
		return ServiceTaskPropertiesAdapter.getChoiceOfValues(object);
	}
}
