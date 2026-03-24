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

import java.util.Hashtable;
import java.util.List;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.IntermediateCatchEvent;
import org.eclipse.bpmn2.IntermediateThrowEvent;
import org.eclipse.bpmn2.LinkEventDefinition;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.adapters.ObjectDescriptor;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * @author Bob Brodt
 *
 */
public class LinkEventDefinitionPropertiesAdapter extends EventDefinitionPropertiesAdapter<LinkEventDefinition> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public LinkEventDefinitionPropertiesAdapter(AdapterFactory adapterFactory, LinkEventDefinition object) {
		super(adapterFactory, object);
		
    	setProperty(Bpmn2Package.eINSTANCE.getLinkEventDefinition_Source(), UI_CAN_CREATE_NEW, Boolean.FALSE);
    	setProperty(Bpmn2Package.eINSTANCE.getLinkEventDefinition_Source(), UI_CAN_EDIT, Boolean.FALSE);
    	setProperty(Bpmn2Package.eINSTANCE.getLinkEventDefinition_Target(), UI_CAN_SET_NULL, Boolean.TRUE);
    	setProperty(Bpmn2Package.eINSTANCE.getLinkEventDefinition_Target(), UI_CAN_CREATE_NEW, Boolean.FALSE);
    	setProperty(Bpmn2Package.eINSTANCE.getLinkEventDefinition_Target(), UI_CAN_EDIT, Boolean.FALSE);
    	setProperty(Bpmn2Package.eINSTANCE.getLinkEventDefinition_Target(), UI_IS_MULTI_CHOICE, Boolean.TRUE);

    	EStructuralFeature feature = Bpmn2Package.eINSTANCE.getLinkEventDefinition_Target();
		setFeatureDescriptor(feature, new FeatureDescriptor<LinkEventDefinition>(this, object, feature) {

			@Override
			public String getTextValue() {
				return getLinkName(object);
			}

			@Override
			public Hashtable<String, Object> getChoiceOfValues() {
				// add all ItemDefinitions
				Hashtable<String, Object> choices = new Hashtable<String, Object>();
				String s;
				Definitions defs = ModelUtil.getDefinitions(object);
				Event thisEvent = getEvent(object);
				List<LinkEventDefinition> links = (List) ModelUtil.getAllReachableObjects(defs,
						Bpmn2Package.eINSTANCE.getLinkEventDefinition());
				for (LinkEventDefinition link : links) {
					if (link != object) {
						Event thatEvent = getEvent(link);
						if (	(thisEvent instanceof IntermediateCatchEvent && thatEvent instanceof IntermediateThrowEvent) ||
								(thatEvent instanceof IntermediateCatchEvent && thisEvent instanceof IntermediateThrowEvent)) {
							ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(link);
							s = adapter.getFeatureDescriptor(Bpmn2Package.eINSTANCE.getLinkEventDefinition_Source())
									.getTextValue();
							choices.put(s, link);
						}
					}
				}
				return choices;
			}
		});

		feature = Bpmn2Package.eINSTANCE.getLinkEventDefinition_Source();
		setFeatureDescriptor(feature, new FeatureDescriptor<LinkEventDefinition>(this, object, feature) {

			@Override
			public String getTextValue() {
				return getLinkName(object);
			}
		});
		
		setObjectDescriptor(new ObjectDescriptor<LinkEventDefinition>(this, object) {
			public String getTextValue() {
				return getLinkName(object);
			}
		});
	}

	private static String getLinkName(LinkEventDefinition link) {
		String eventName = ModelUtil.getCanonicalName(getEvent(link));
		String linkName = link.getName();
		if (linkName==null || linkName.isEmpty())
			linkName = link.getId();
		return eventName + "/" + linkName; //$NON-NLS-1$
	}
	
	private static Event getEvent(LinkEventDefinition link) {
		if (link.eContainer() instanceof Event) {
			Event event = (Event) link.eContainer();
			if (event instanceof IntermediateCatchEvent || event instanceof IntermediateThrowEvent) {
				return event;
			}
		}
		return null;
	}

}
