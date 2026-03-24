/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014, 2015 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Flavio Donzé
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.runtime;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * this adapter basically only holds the current target runtime for an notifier (resource, eobject, eclass),<br/>
 * which allows access to the current runtime through the notifier 
 * 
 * @author Flavio Donzé
 */
public class TargetRuntimeAdapter extends AdapterImpl implements ITargetRuntimeProvider {
	/** current target runtime of the adapter */
	private TargetRuntime targetRuntime = null;
	
	/**
	 * add a {@link TargetRuntimeAdapter} to the passed resource adapters,<br/>
	 * if there is an adapter with a different target runtime registered,<br/>
	 * remove it and add a new one with the current runtime
	 * 
	 * @param resource
	 * @param targetRuntime
	 */
	public static void adapt(Resource resource, TargetRuntime targetRuntime) {
		for (Adapter a : resource.eAdapters()) {
			if (a instanceof TargetRuntimeAdapter) {
				if (((TargetRuntimeAdapter) a).getTargetRuntime().equals(targetRuntime)) {
					return;
				} else {
					resource.eAdapters().remove(a);
					break;
				}
			}
		}
		TargetRuntimeAdapter adapter = new TargetRuntimeAdapter(targetRuntime);
		resource.eAdapters().add(adapter);
	}
	
	/**
 	 * add a {@link TargetRuntimeAdapter} to the passed eobject adapters,<br/>
	 * if there is an adapter with a different target runtime registered,<br/>
	 * remove it and add a new one with the current runtime
	 * 
	 * @param object
	 * @param targetRuntime
	 */
	public static void adapt(EObject object, TargetRuntime targetRuntime) {
		for (Adapter a : object.eAdapters()) {
			if (a instanceof TargetRuntimeAdapter) {
				if (((TargetRuntimeAdapter) a).getTargetRuntime().equals(targetRuntime)) {
					return;
				} else {
					object.eAdapters().remove(a);
					break;
				}
			}
		}

		// check if the adapter of the resource can be used
		Resource resource = object.eResource();
		if (resource != null) {
			for (Adapter a : object.eAdapters()) {
				if (a instanceof TargetRuntimeAdapter) {
					if (((TargetRuntimeAdapter) a).getTargetRuntime().equals(targetRuntime)) {
						object.eAdapters().add(a);
						return;
					} else {
						throw new IllegalStateException("not allowed to have differnt runtime on the object and it's resource"); //$NON-NLS-1$
					}
				}
			}
		}
		
		TargetRuntimeAdapter adapter = new TargetRuntimeAdapter(targetRuntime);
		object.eAdapters().add(adapter);	
	}
	
	/**
	 * if there is a runtime adapter registered, remove it from the notifier
	 * 
	 * @param notifier e.g. resource, eobject or eclass
	 */
	public static void remove(Notifier notifier) {
		for (Adapter a : notifier.eAdapters()) {
			if (a instanceof TargetRuntimeAdapter) {
				notifier.eAdapters().remove(a);
				return;
			}
		}
	}

	/**
	 * returns the target runtime registered on the resource
	 * 
	 * @param notifier e.g. resource, eobject or eclass
	 * @return null in case there is no {@link TargetRuntimeAdapter}
	 */
	public static TargetRuntime getTargetRuntime(Notifier notifier) {
		for (Adapter a : notifier.eAdapters()) {
			if (a instanceof TargetRuntimeAdapter)
				return ((TargetRuntimeAdapter) a).getTargetRuntime();
		}
		return null;
	}
	
	/**
	 * constructor
	 * 
	 * @param targetRuntime
	 */
	public TargetRuntimeAdapter(TargetRuntime targetRuntime) {
		this.targetRuntime = targetRuntime;
	}

	/**
	 * returns the current target runtime of this adapter
	 * 
	 * @return
	 */
	public TargetRuntime getTargetRuntime() {
		return targetRuntime;
	}

	@Override
	public void setTargetRuntime(TargetRuntime rt) {
		this.targetRuntime = rt;
	}
}
