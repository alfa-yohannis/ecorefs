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

package org.eclipse.bpmn2.modeler.core.validation.validators;

import org.eclipse.bpmn2.Error;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class ErrorValidator extends AbstractBpmn2ElementValidator<Error> {

	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public ErrorValidator(IValidationContext ctx) {
		super(ctx);
	}

	/**
	 * Construct a BPMN2 Element Validator with the given Validator as the parent.
	 * The parent is responsible for collecting all of the validation Status objects
	 * and reporting them back to the Validation Constraint.
	 *
	 * @param parent a parent Validator class
	 */
	@SuppressWarnings("rawtypes")
	public ErrorValidator(AbstractBpmn2ElementValidator parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(Error object) {
		if (ProcessValidator.isContainingProcessExecutable(object)) {
			// Only report problems with this object one time.
			// This same error should not be reported when validating
			// other objects that references this object. 
			if (this.parent==null) {
				if (isEmpty(object.getErrorCode()))
					addMissingFeatureStatus(object,"errorCode",Status.ERROR); //$NON-NLS-1$
				
				ItemDefinition itemDefinition = object.getStructureRef();
				if (itemDefinition!=null) {
					new ItemDefinitionValidator(this).validate(itemDefinition);
				}
				else
					addMissingFeatureStatus(object,"structureRef",Status.ERROR); //$NON-NLS-1$
			}
		}
		return getResult();
	}

}

