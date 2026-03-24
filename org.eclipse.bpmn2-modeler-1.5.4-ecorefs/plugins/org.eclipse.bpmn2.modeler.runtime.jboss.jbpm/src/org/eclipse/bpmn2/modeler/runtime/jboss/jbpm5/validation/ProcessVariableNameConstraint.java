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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.validators.ProcessVariableNameValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.AbstractModelConstraint;
import org.eclipse.emf.validation.IValidationContext;

public class ProcessVariableNameConstraint extends AbstractModelConstraint {

	@Override
	public IStatus validate(IValidationContext ctx) {
		EObject object = ctx.getTarget();
		if (object instanceof BaseElement) {
			return new ProcessVariableNameValidator(ctx).validate((BaseElement) object);
		}
		return ctx.createSuccessStatus();
	}

	public boolean doLiveValidation() {
		return true;
	}
}
