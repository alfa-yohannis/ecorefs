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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.adapters;

import java.util.Hashtable;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.modeler.core.adapters.ObjectDescriptor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.JavaVariableNameObjectEditor;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.Messages;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.util.JbpmModelUtil;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.ItemDefinitionRefFeatureDescriptor;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.PropertyPropertiesAdapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Bob Brodt
 *
 */
public class JbpmPropertyPropertiesAdapter extends PropertyPropertiesAdapter {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public JbpmPropertyPropertiesAdapter(AdapterFactory adapterFactory, Property object) {
		super(adapterFactory, object);

    	EStructuralFeature feature = Bpmn2Package.eINSTANCE.getItemAwareElement_ItemSubjectRef();
    	setProperty(feature, UI_CAN_CREATE_NEW, Boolean.TRUE);
    	setProperty(feature, UI_CAN_EDIT, Boolean.TRUE);
		setProperty(feature, UI_IS_MULTI_CHOICE, Boolean.TRUE);
		
    	setFeatureDescriptor(feature,
			new ItemDefinitionRefFeatureDescriptor<Property>(this,object,feature) {
				
	    		@Override
	    		protected void internalSet(Property property, EStructuralFeature feature, Object value, int index) {
					property.setItemSubjectRef(JbpmModelUtil.getDataType(property, value));
				}
				
				@Override
				public Hashtable<String, Object> getChoiceOfValues() {
					return JbpmModelUtil.getChoiceOfValues(object);
				}
				
				@Override
				public boolean isMultiLine() {
					return true;
				}
			}
    	);

		feature = Bpmn2Package.eINSTANCE.getProperty_Name();
		setProperty(feature, UI_OBJECT_EDITOR_CLASS, JavaVariableNameObjectEditor.class);
		
		setObjectDescriptor(new ObjectDescriptor<Property>(this,object) {
			
			@Override
			public String getLabel() {
				return Messages.JbpmDataItemsDetailComposite_LocalVariablesTitle;
			}
		});

	}
}
