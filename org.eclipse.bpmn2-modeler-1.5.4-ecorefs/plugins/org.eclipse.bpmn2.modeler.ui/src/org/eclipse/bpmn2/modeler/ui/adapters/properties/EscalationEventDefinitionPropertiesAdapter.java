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
import org.eclipse.bpmn2.Escalation;
import org.eclipse.bpmn2.EscalationEventDefinition;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Bob Brodt
 *
 */
public class EscalationEventDefinitionPropertiesAdapter extends EventDefinitionPropertiesAdapter<EscalationEventDefinition> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public EscalationEventDefinitionPropertiesAdapter(AdapterFactory adapterFactory, EscalationEventDefinition object) {
		super(adapterFactory, object);
		
    	setProperty(Bpmn2Package.eINSTANCE.getEscalationEventDefinition_EscalationRef(), UI_CAN_CREATE_NEW, Boolean.TRUE);
    	setProperty(Bpmn2Package.eINSTANCE.getEscalationEventDefinition_EscalationRef(), UI_CAN_EDIT, Boolean.TRUE);
    	setProperty(Bpmn2Package.eINSTANCE.getEscalationEventDefinition_EscalationRef(), UI_IS_MULTI_CHOICE, Boolean.TRUE);

		EStructuralFeature ref = Bpmn2Package.eINSTANCE.getEscalationEventDefinition_EscalationRef();
		setFeatureDescriptor(ref, new FeatureDescriptor<EscalationEventDefinition>(this,object,ref) {

			@Override
			protected void internalSet(EscalationEventDefinition object, EStructuralFeature feature, Object value, int index) {
				super.internalSet(object, feature, value, index);
				Escalation escalation = object.getEscalationRef();
				if (escalation!=null) {
					ItemDefinition itemDefinition = escalation.getStructureRef();
					// propagate the structureRef of this Escalation to Activity DataInputs and DataOutputs
					// but only if it is define (not null)
					if (itemDefinition!=null)
						ExtendedPropertiesProvider.setValue(escalation, Bpmn2Package.eINSTANCE.getEscalation_StructureRef(), itemDefinition);
				}
			}

		});
	}

}
