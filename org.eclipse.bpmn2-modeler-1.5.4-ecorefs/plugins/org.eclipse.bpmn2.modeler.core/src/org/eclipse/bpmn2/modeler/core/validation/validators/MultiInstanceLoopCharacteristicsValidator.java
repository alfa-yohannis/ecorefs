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

import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class MultiInstanceLoopCharacteristicsValidator extends AbstractBpmn2ElementValidator<MultiInstanceLoopCharacteristics> {

	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public MultiInstanceLoopCharacteristicsValidator(IValidationContext ctx) {
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
	public MultiInstanceLoopCharacteristicsValidator(AbstractBpmn2ElementValidator parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(MultiInstanceLoopCharacteristics object) {
		EObject resultLocus[] = new EObject[] {object.eContainer()};
		if (isEmpty(object.getLoopCardinality())) {
			if (isEmpty(object.getLoopDataInputRef())) {
				addStatus(object,resultLocus,Status.ERROR,Messages.MultiInstanceLoopCharacteristicsValidator_No_Condition_Or_Inputs);
			}
			else if (isEmpty(object.getInputDataItem())) {
				addStatus(object,resultLocus,Status.ERROR,Messages.MultiInstanceLoopCharacteristicsValidator_No_Input_Instance_Parameter);
			}
		}
		else {
			if (!isEmpty(object.getLoopDataInputRef())) {
				addStatus(object,resultLocus,Status.ERROR,Messages.MultiInstanceLoopCharacteristicsValidator_Both_Condition_And_Inputs);
			}
		}
		if (!isEmpty(object.getLoopDataOutputRef())) {
			if (isEmpty(object.getOutputDataItem())) {
				addStatus(object,resultLocus,Status.ERROR,Messages.MultiInstanceLoopCharacteristicsValidator_No_Output_Instance_Parameter);
			}
		}
		return getResult();
	}

}

