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

package org.eclipse.bpmn2.modeler.ui.adapters.properties;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.Signal;
import org.eclipse.bpmn2.SignalEventDefinition;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Bob Brodt
 *
 */
public class SignalEventDefinitionPropertiesAdapter extends EventDefinitionPropertiesAdapter<SignalEventDefinition> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public SignalEventDefinitionPropertiesAdapter(AdapterFactory adapterFactory, SignalEventDefinition object) {
		super(adapterFactory, object);
    	
		EStructuralFeature ref = Bpmn2Package.eINSTANCE.getSignalEventDefinition_SignalRef();
    	setProperty(ref, UI_IS_MULTI_CHOICE, Boolean.TRUE);

    	setFeatureDescriptor(ref, new FeatureDescriptor<SignalEventDefinition>(this,object,ref) {

			@Override
			protected void internalSet(SignalEventDefinition object, EStructuralFeature feature, Object value, int index) {
				super.internalSet(object, feature, value, index);
				Signal Signal = object.getSignalRef();
				if (Signal!=null) {
					ItemDefinition itemDefinition = Signal.getStructureRef();
					ExtendedPropertiesProvider.setValue(Signal, Bpmn2Package.eINSTANCE.getSignal_StructureRef(), itemDefinition);
				}
			}

		});
	}

}
