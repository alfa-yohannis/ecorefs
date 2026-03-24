/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 * All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.adapters.properties;

import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ObjectDescriptor;
import org.eclipse.emf.common.notify.AdapterFactory;

public class EventDefinitionPropertiesAdapter<T extends EventDefinition> extends ExtendedPropertiesAdapter<T> {

	public EventDefinitionPropertiesAdapter(AdapterFactory adapterFactory, T object) {
		super(adapterFactory, object);
		
		setObjectDescriptor(new ObjectDescriptor<T>(this,object));
	}
}
