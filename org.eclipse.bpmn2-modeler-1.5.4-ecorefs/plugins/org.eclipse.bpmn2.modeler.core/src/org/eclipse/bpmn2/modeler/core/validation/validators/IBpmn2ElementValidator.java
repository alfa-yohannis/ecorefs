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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EClass;

/**
 * Interface that defines a BPMN2 Element Validator.
 */
public interface IBpmn2ElementValidator<T> {

	/**
	 * Perform the object validation.
	 * 
	 * @param object the BPMN2 element to be validated
	 * @return the results of the validation.
	 */
	IStatus validate(T object);
	
	/**
	 * Check if the validator for a specific super type of the given object should also
	 * be invoked.
	 * 
	 * @param eClass the BPMN2 element EClass which must be a super type of the given object
	 * @param object the BPMN2 element instance object
	 * @return
	 */
	boolean checkSuperType(EClass eClass, T object);
	
	/**
	 * Returns true if this validator can handle Live validations.
	 * 
	 * @return true if Live validations are handled, false if this validator
	 *         should only be used in Batch mode.
	 */
	boolean doLiveValidation();
	
	/**
	 * Returns the result of the validation after validate() has been called.
	 * Note that IStatus may be a multi-Status object.
	 * 
	 * @return the IStatus of the validation result. If no validation errors were
	 * detected, this will be an IStatus object with severity Status.OK
	 */
	IStatus getResult();
}
