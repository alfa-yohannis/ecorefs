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
package org.eclipse.bpmn2.modeler.core.validation;

import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.runtime.ToolPaletteDescriptor;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

public class ResourcePropertyTester extends PropertyTester {

	public ResourcePropertyTester() {
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof EObject) {
			EObject object = (EObject) receiver;
			// Get the TargetRuntime from this EObject.
			// If the EObject's nsURI has been registered by any
			// BPMN2 Modeler extension plugin, skip validation.
			Resource resource = object.eResource();
			if (resource!=null) {
				TargetRuntime rt = TargetRuntime.getRuntime(resource);
				if (rt!=null) {
					if ("targetRuntimeId".equals(property)) { //$NON-NLS-1$
						return rt.getId().equals(expectedValue);
					}
					else if ("toolPaletteProfile".equals(property)) { //$NON-NLS-1$
						ToolPaletteDescriptor tpd = rt.getToolPalette(object);
						if (tpd != null) {
							for (String profileId : tpd.getProfileIds()) {
								if (profileId.equals(expectedValue))
									return true;
							}
							if (expectedValue instanceof String)
								return ((String)expectedValue).isEmpty();
						}
					}
					if ("doCoreValidation".equals(property)) { //$NON-NLS-1$
						Bpmn2Preferences prefs = Bpmn2Preferences.getInstance(object);
						if (prefs!=null) {
							String value = Boolean.toString( prefs.getDoCoreValidation() );
							expectedValue = expectedValue.toString();
							return value.equals(expectedValue);
						}
						return false;
					}
				}
			}
		}
		return false;
	}

}
