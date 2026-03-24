/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.adapters;

import java.util.Hashtable;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.DataAssociationPropertiesAdapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;

public class JbpmDataAssociationPropertiesAdapter extends
		DataAssociationPropertiesAdapter {

	public JbpmDataAssociationPropertiesAdapter(AdapterFactory adapterFactory,
			DataAssociation object) {
		super(adapterFactory, object);

    	EStructuralFeature ref;
    	
    	ref = Bpmn2Package.eINSTANCE.getDataAssociation_SourceRef();
    	setFeatureDescriptor(ref, new JbpmSourceTargetFeatureDescriptor(this,object,ref) {
    		
    	});

		ref = Bpmn2Package.eINSTANCE.getDataAssociation_TargetRef();
    	setFeatureDescriptor(ref, new JbpmSourceTargetFeatureDescriptor(this,object,ref) {
    		
    	});
	}

	public class JbpmSourceTargetFeatureDescriptor extends SourceTargetFeatureDescriptor {

		public JbpmSourceTargetFeatureDescriptor(ExtendedPropertiesAdapter<DataAssociation> owner,
				DataAssociation object, EStructuralFeature feature) {
			super(owner, object, feature);
		}

		@Override
		public Hashtable<String, Object> getChoiceOfValues() {
			Hashtable<String, Object> choices = super.getChoiceOfValues();
			
			// GlobalType extensions can not be used for DataAssociations (as of jBPM 6)
			// but maybe this will be supported in the future.
			
//			EObject container = ModelUtil.getContainer(object);
//			Definitions definitions = ModelUtil.getDefinitions(container);
//			for (RootElement re : definitions.getRootElements()) {
//				if (re instanceof Process) {
//					Process process = (Process)re;
//					for (GlobalType g : ModelUtil.getAllExtensionAttributeValues(process, GlobalType.class)) {
//						choices.put(g.getIdentifier(), g);
//					}
//				}
//			}
			return choices;
		}
	}
	
}
