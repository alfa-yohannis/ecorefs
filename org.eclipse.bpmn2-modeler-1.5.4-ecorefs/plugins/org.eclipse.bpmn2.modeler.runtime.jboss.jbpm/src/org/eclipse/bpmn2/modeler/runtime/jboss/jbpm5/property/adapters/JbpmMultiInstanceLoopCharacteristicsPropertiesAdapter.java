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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.JavaVariableNameObjectEditor;
import org.eclipse.bpmn2.modeler.core.model.ModelDecorator;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.GlobalType;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.MultiInstanceLoopCharacteristicsPropertiesAdapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EObject;

public class JbpmMultiInstanceLoopCharacteristicsPropertiesAdapter extends MultiInstanceLoopCharacteristicsPropertiesAdapter {

	public JbpmMultiInstanceLoopCharacteristicsPropertiesAdapter(AdapterFactory adapterFactory, MultiInstanceLoopCharacteristics object) {
		super(adapterFactory, object);

		setProperty(INPUT_DATA_ITEM, UI_OBJECT_EDITOR_CLASS, JavaVariableNameObjectEditor.class);
		setProperty(OUTPUT_DATA_ITEM, UI_OBJECT_EDITOR_CLASS, JavaVariableNameObjectEditor.class);

		setFeatureDescriptor(LOOP_DATA_INPUT_REF, new LoopDataInputCollectionFeatureDescriptor(this, object) {
			@Override
			public Hashtable<String, Object> getChoiceOfValues() {
				Hashtable<String, Object> choices = super.getChoiceOfValues();
				for (GlobalType g : getAllGlobals(object)) {
					ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(g);
					choices.put(adapter.getObjectDescriptor().getTextValue(), g);
				}
				return choices;
			}
		});
		
		setFeatureDescriptor(LOOP_DATA_OUTPUT_REF, new LoopDataOutputCollectionFeatureDescriptor(this, object) {
			@Override
			public Hashtable<String, Object> getChoiceOfValues() {
				Hashtable<String, Object> choices = super.getChoiceOfValues();
				for (GlobalType g : getAllGlobals(object)) {
					ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(g);
					choices.put(adapter.getObjectDescriptor().getTextValue(), g);
				}
				return choices;
			}
		});
	}
	
	private List<GlobalType> getAllGlobals(EObject object) {
		List<GlobalType> results = new ArrayList<GlobalType>();
		// navigate up to the Process from the given EObject
		Process process = null;
		while (object!=null) {
			if (object instanceof Process) {
				process = (Process) object;
				break;
			}
			object = object.eContainer();
		}
		if (process!=null) {
			for (Object g : ModelDecorator.getAllExtensionAttributeValues(process, GlobalType.class)) {
				results.add((GlobalType)g);
			}
		}
		return results;
	}
}
