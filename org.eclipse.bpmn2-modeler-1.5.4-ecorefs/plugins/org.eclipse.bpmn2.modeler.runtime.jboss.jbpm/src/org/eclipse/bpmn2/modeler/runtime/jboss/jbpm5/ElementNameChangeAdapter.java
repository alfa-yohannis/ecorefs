package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.Lane;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

public class ElementNameChangeAdapter implements Adapter {
	
	public static ElementNameChangeAdapter adapt(EObject object) {
		if (appliesTo(object)) {
			for (Adapter a : ((EObject)object).eAdapters()) {
				if (a instanceof ProcessVariableNameChangeAdapter) {
					return (ElementNameChangeAdapter)a;
				}
			}
			ElementNameChangeAdapter a = new ElementNameChangeAdapter();
			object.eAdapters().add(a);
			return a;
		}
		return null;
	}
	
	public static boolean appliesTo(EObject object) {
		// Curiously, Gateways do not have an "elementname" Meta Data element.
		return (object instanceof FlowNode && !(object instanceof Gateway))
				|| object instanceof Lane;
	}

	@Override
	public void notifyChanged(Notification notification) {
		if (notification.getNotifier() instanceof BaseElement) {
			BaseElement element = (BaseElement)notification.getNotifier();
            if (notification.getEventType()==Notification.SET) {
            	Object f = notification.getFeature();
            	if (f instanceof EAttribute && "name".equals(((EAttribute)f).getName())) { //$NON-NLS-1$
            		// The notifier is an Activity and its name has changed
            		// Update the MetaData element named "elementname" to
            		// reflect the new Activity name.
            		String value = notification.getNewStringValue();
            		if (value==null || value.isEmpty())
            			value = "";
            		MetaDataTypeAdapter.setMetaData(element, "elementname", value); //$NON-NLS-1$
            	}
            }
		}
	}
	
	@Override
	public Notifier getTarget() {
		return null;
	}

	@Override
	public void setTarget(Notifier newTarget) {
		if (newTarget instanceof BaseElement) {
			BaseElement element = (BaseElement)newTarget;
			EStructuralFeature feature = element.eClass().getEStructuralFeature("name"); //$NON-NLS-1$
			if (feature!=null) {
				Object oldValue = ""; //$NON-NLS-1$
				Object newValue = element.eGet(feature);
				Notification notification = new ENotificationImpl((InternalEObject)element,
						Notification.SET, feature,
						oldValue, newValue);
				notifyChanged(notification);
			}
		}
	}

	@Override
	public boolean isAdapterForType(Object type) {
		return false;
	}

}
