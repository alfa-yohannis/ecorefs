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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Association;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * @author Bob Brodt
 *
 */
public class CompensateEventDefinitionPropertiesAdapter extends EventDefinitionPropertiesAdapter<CompensateEventDefinition> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public CompensateEventDefinitionPropertiesAdapter(AdapterFactory adapterFactory, CompensateEventDefinition object) {
		super(adapterFactory, object);
		
		EStructuralFeature feature = Bpmn2Package.eINSTANCE.getCompensateEventDefinition_ActivityRef();
    	setProperty(feature, UI_CAN_CREATE_NEW, Boolean.FALSE);
    	setProperty(feature, UI_CAN_EDIT, Boolean.FALSE);
    	setProperty(feature,UI_IS_MULTI_CHOICE, Boolean.TRUE);
    	
    	setFeatureDescriptor(feature, new FeatureDescriptor<CompensateEventDefinition>(this,object,feature) {

    		@Override
    		public Hashtable<String, Object> getChoiceOfValues() {
    			Hashtable<String, Object> choices = super.getChoiceOfValues();
				Event event = (Event)object.eContainer();
				if (event instanceof BoundaryEvent && ((BoundaryEvent)event).getAttachedToRef()!=null) {
					EObject eventContainer = event.eContainer();
					Activity attachedToActivity = ((BoundaryEvent)event).getAttachedToRef();
					List<String> removed = new ArrayList<String>();
					for (Entry<String, Object> e : choices.entrySet()) {
						EObject o = (EObject) e.getValue();
						if (o==attachedToActivity) {
							// can't create an association between the Boundary Event
							// and the Activity to which it is attached.
							removed.add(e.getKey());
						}
						else {
							// can't create an association to an Activity that is outside
							// the Boundary Event's container Activity.
							if (o.eContainer() != eventContainer) {
								removed.add(e.getKey());
							}
						}
					}
					for (String key : removed) {
						choices.remove(key);
					}
				}
    			
    			return choices;
    		}
    		
			@Override
			protected void internalSet(CompensateEventDefinition object, EStructuralFeature feature, Object value, int index) {
				Event event = (Event)object.eContainer();
				Activity oldActivity = object.getActivityRef();
				Activity newActivity = null;
				if (value instanceof Activity) {
					newActivity = (Activity) value;
				}
				if (newActivity==oldActivity)
					return;
				
				super.internalSet(object, feature, newActivity, index);

				if (event instanceof BoundaryEvent) {
					Resource resource = object.eResource();
					Association association = null;
					if (oldActivity!=null) {
						// there may have been an existing Association between this
						// Event and the previously referenced Activity - try to find it
						for (EObject o : ModelUtil.getAllReachableObjects(object, Bpmn2Package.eINSTANCE.getAssociation())) {
							if (o instanceof Association) {
								Association a = (Association) o;
								BaseElement source = a.getSourceRef();
								BaseElement target = a.getTargetRef();
								if ( source==event && target==oldActivity) {
									association = a;
									break;
								}
							}
						}
					}
	
					if (newActivity==null) {
						// The newly selected Activity referenced by the Compensation Event
						// is null so delete the Association if one exists
						if (association!=null) {
							ExtendedPropertiesProvider.setValue(association, Bpmn2Package.eINSTANCE.getAssociation_SourceRef(), null);
						}
					}
					else {
						// create or update the Association
						if (association==null) {
							// create a new Association:
							// Note that this is only for the purpose of getting an ExtendedPropertiesAdapter
							// for the Association and having it create the Association Connection.
							association = (Association) Bpmn2ModelerFactory.createObject(resource, Bpmn2Package.eINSTANCE.getAssociation());
						}
						// and set the Event as the source reference
						ExtendedPropertiesProvider.setValue(association, Bpmn2Package.eINSTANCE.getAssociation_SourceRef(), event);
						// and the new Activity as the target
						ExtendedPropertiesProvider.setValue(association, Bpmn2Package.eINSTANCE.getAssociation_TargetRef(), newActivity);
					}
				}
			}

    	}); 
	}

}
