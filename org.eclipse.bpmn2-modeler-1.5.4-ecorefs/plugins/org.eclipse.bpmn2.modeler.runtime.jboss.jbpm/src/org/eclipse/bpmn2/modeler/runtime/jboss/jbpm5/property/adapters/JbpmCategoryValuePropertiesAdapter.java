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

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CategoryValue;
import org.eclipse.bpmn2.Group;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.adapters.ObjectDescriptor;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.CategoryValuePropertiesAdapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

public class JbpmCategoryValuePropertiesAdapter extends CategoryValuePropertiesAdapter {

	public JbpmCategoryValuePropertiesAdapter(AdapterFactory adapterFactory, CategoryValue object) {
		super(adapterFactory, object);

		EStructuralFeature ref = Bpmn2Package.eINSTANCE.getCategoryValue_Value();
//		setProperty(ref, UI_CAN_CREATE_NEW, Boolean.TRUE);
//		setProperty(ref, UI_IS_MULTI_CHOICE, Boolean.TRUE);
		setProperty(ref, UI_CAN_SET_NULL, Boolean.TRUE);
		// this is a read-only list
		setProperty(Bpmn2Package.eINSTANCE.getCategoryValue_CategorizedFlowElements(), UI_CAN_EDIT, Boolean.FALSE);
		
    	setFeatureDescriptor(ref,
			new FeatureDescriptor<CategoryValue>(this, object,ref) {
				@Override
				public String getTextValue() {
					return getDisplayName(object);
				}

				@Override
				public Object getValue() {
					return super.getValue();
				}

				@Override
		   		protected void internalSet(CategoryValue categoryValue, EStructuralFeature feature, Object value, int index) {
					if (value instanceof String) {
						int i = ((String) value).indexOf(":"); //$NON-NLS-1$
						if (i>=0)
							value = ((String) value).substring(i+1);
					}
					super.internalSet(categoryValue, feature, value, index);
					updateGroups(categoryValue);
				}
				
			}
    	
    	);
    	
		setObjectDescriptor(new ObjectDescriptor<CategoryValue>(this, object) {
			@Override
			public String getTextValue() {
				return getDisplayName(object);
			}
		});
	}
	
	protected static String getDisplayName(CategoryValue categoryValue) {
		return categoryValue.getValue();
	}
}