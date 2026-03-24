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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5;

import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataObject;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.Error;
import org.eclipse.bpmn2.Escalation;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.Signal;
import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.GlobalType;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.validation.model.EvaluationMode;
import org.eclipse.emf.validation.service.IValidator;
import org.eclipse.emf.validation.service.ModelValidationService;

/*
 * jBPM uses certain objects' IDs instead of names because...well, I don't know why...bad decision maybe?
 * Anyway, it means that the names and IDs MUST be kept in sync because our UI does not allow IDs to be
 * edited directly since that would be a VERY BAD THING. IDs MUST be unique, and allowing the user to edit
 * them could violate that constraint, e.g. I should be able to create a org.eclipse.bpmn2.Property named
 * "counter" in every SubProcess in my file, but that would violate the ID constraint.
 * 
 * But, it is what it is...
 */
public class ProcessVariableNameChangeAdapter implements Adapter {

	private ProcessVariableNameChangeAdapter() {
	}
	
	public static ProcessVariableNameChangeAdapter adapt(EObject object) {
		if (appliesTo(object)) {
			for (Adapter a : ((EObject)object).eAdapters()) {
				if (a instanceof ProcessVariableNameChangeAdapter) {
					return (ProcessVariableNameChangeAdapter)a;
				}
			}
			ProcessVariableNameChangeAdapter a = new ProcessVariableNameChangeAdapter();
			object.eAdapters().add(a);
			return a;
		}
		return null;
	}
	
	public static boolean appliesTo(EObject object) {
		return (object instanceof org.eclipse.bpmn2.Property ||
				object instanceof DataObject ||
				// see https://issues.jboss.org/browse/RHBPMS-4302
//				object instanceof Message ||
//				object instanceof Signal ||
//				object instanceof Error ||
//				object instanceof Escalation ||
				object instanceof GlobalType ||
					(
						// DataInput and DataOutput objects are a special case: Only
						// keep the name and ID in sync if the DataInput is being used
						// as the instance parameter for MultiInstanceLoopCharacteristics
						// (or if the object hasn't been added to a container yet).
						(object instanceof DataInput || object instanceof DataOutput) && (
						object.eContainer() instanceof MultiInstanceLoopCharacteristics ||
						object.eContainer()==null)
					)
				);
		
	}
	
	@Override
	public void notifyChanged(Notification notification) {
		if (notification.getNotifier() instanceof EObject) {
			EObject object = (EObject)notification.getNotifier();
            if (notification.getEventType()==Notification.SET && appliesTo(object)) {

				Object f = notification.getFeature();
				if (f instanceof EStructuralFeature) {
					EStructuralFeature feature = (EStructuralFeature)f;
					EStructuralFeature idFeature = object.eClass().getEStructuralFeature("id"); //$NON-NLS-1$
					EStructuralFeature nameFeature = object.eClass().getEStructuralFeature("name"); //$NON-NLS-1$
					EStructuralFeature identifierFeature = object.eClass().getEStructuralFeature("identifier"); //$NON-NLS-1$
					if (identifierFeature!=null)
						nameFeature = identifierFeature;
                    if ("name".equals(feature.getName()) || "identifier".equals(feature.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
                    	// you guys are killing me here!
                    	// if the object is still being created and it hasn't been added to a container
                    	// yet, then don't keep the ID in sync with the name...in other words, if the name
                    	// is being set, don't alter the ID to match the name...yet...
                    	if (object.eContainer()==null)
                    		return;
						Object newValue = notification.getNewValue();
						Object oldValue = notification.getOldValue();
						if (newValue!=null && !newValue.equals(oldValue))
						{
							if (idFeature!=null) {
								if (!SyntaxCheckerUtils.isJavaIdentifier((String)newValue))
									newValue = SyntaxCheckerUtils.toJavaIdentifier((String)newValue);
								boolean deliver = object.eDeliver();
								try {
									if (deliver)
										object.eSetDeliver(false);
									
									// No need to make this ID unique; ID collisions will be detected
									// during validate() and will be reported in the UI.
									object.eSet(nameFeature, newValue);
									object.eSet(idFeature, newValue);
								}
								catch (Exception e) {
									
								}
								finally {
									object.eSetDeliver(deliver);
								}								
								validate(notification);
							}
						}
					}
                    else if ("id".equals(feature.getName())) { //$NON-NLS-1$
						Object newValue = notification.getNewValue();
						Object oldValue = notification.getOldValue();
						if (newValue!=null && !newValue.equals(oldValue)) 
						{
							if (nameFeature!=null) {
								boolean deliver = object.eDeliver();
								try {
									if (deliver)
										object.eSetDeliver(false);
									
									Object uniqueId = makeUniqueId(object,newValue);
									object.eSet(nameFeature, uniqueId);
									object.eSet(idFeature, uniqueId);
								}
								catch (Exception e) {
									
								}
								finally {
									object.eSetDeliver(deliver);
								}								
								validate(notification);
							}
						}
					}
				}
            }
		}
	}

	private Object makeUniqueId(EObject object, Object id) {
		int i = 1;
		Object uniqueId = id;
		EObject dup = null;
		do {
			dup = findDuplicateId(object,uniqueId);
			if (dup!=null) {
				uniqueId = id + "_" + i++; //$NON-NLS-1$
			}
		}
		while (dup!=null);
		return uniqueId;
	}
	
	private EObject findDuplicateId(EObject object, Object id) {
		if (object!=null && id!=null) {
			Resource resource = object.eResource();
			
			TreeIterator<EObject> iter = resource.getAllContents();
			while (iter.hasNext()) {
				EObject o = iter.next();
				if (o!=object) {
					EStructuralFeature f = o.eClass().getEStructuralFeature("id"); //$NON-NLS-1$
					if (f!=null) {
						Object existingId = o.eGet(f);
						if (id.equals(existingId))
							return o;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public Notifier getTarget() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTarget(Notifier newTarget) {
		if (newTarget instanceof EObject) {
			EObject object = (EObject)newTarget;
			EStructuralFeature feature = object.eClass().getEStructuralFeature("name"); //$NON-NLS-1$
			if (feature!=null) {
				Object oldValue = ""; //$NON-NLS-1$
				Object newValue = object.eGet(feature);
				Notification notification = new ENotificationImpl((InternalEObject)object,
						Notification.SET, feature,
						oldValue, newValue);
				notifyChanged(notification);
			}
		}
	}

	@Override
	public boolean isAdapterForType(Object type) {
		// TODO Auto-generated method stub
		return false;
	}

	private void validate(Notification notification) {
		IValidator<Notification> validator = ModelValidationService.getInstance().newValidator(EvaluationMode.LIVE);
		validator.validate(notification);
	}
}
