/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.validators;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.IValidationContext;


/**
 *
 */
public class DefinitionsValidator extends org.eclipse.bpmn2.modeler.core.validation.validators.DefinitionsValidator {


	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public DefinitionsValidator(IValidationContext ctx) {
		super(ctx);
	}

	/**
	 * Construct a BPMN2 Element Validator with the given Validator as the parent.
	 * The parent is responsible for collecting all of the validation Status objects
	 * and reporting them back to the Validation Constraint.
	 *
	 * @param parent a parent Validator class
	 */
	public DefinitionsValidator(AbstractBpmn2ElementValidator<?> other) {
		super(other);
	}

	@Override
	public IStatus validate(Definitions object) {
		// See https://issues.jboss.org/browse/JBPM-4860
		// TargetNamespace should not be required if we already know this is a jBPM process file
//		if (object.getTargetNamespace()==null || object.getTargetNamespace().isEmpty()) {
//			addStatus(object, Status.ERROR, Messages.DefinitionsValidator_No_TargetNamespace);
//		}
		if (isLiveValidation()) {
			TreeIterator<EObject> iter = object.eAllContents();
			while (iter.hasNext()) {
				EObject o = iter.next();
				if (o instanceof BaseElement) {
					addStatus(new ProcessVariableNameValidator(this).validate((BaseElement)o));
				}
			}
		}
		return getResult();
	}
}
