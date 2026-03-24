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

import org.eclipse.bpmn2.AdHocSubProcess;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class SubProcessValidator extends AbstractBpmn2ElementValidator<SubProcess> {

	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public SubProcessValidator(IValidationContext ctx) {
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
	public SubProcessValidator(AbstractBpmn2ElementValidator parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(SubProcess object) {
		if (object.isTriggeredByEvent()) {
			StartEvent start = null;
			for (FlowElement fe : object.getFlowElements()) {
				if (fe instanceof StartEvent) {
					if (start!=null) {
						addStatus(object,Status.ERROR,Messages.SubProcessValidator_EventSubProcess_Multiple_Start);
					}
					start = (StartEvent) fe;
				}
			}
			if (start!=null) {
				if (start.getEventDefinitions().size()==0) {
					addStatus(object,Status.ERROR,Messages.SubProcessValidator_EventSubProcess_No_Event);
				}
			}
			else {
				addStatus(object,Status.ERROR,Messages.SubProcessValidator_EventSubProcess_No_Start);
			}
		}
		return getResult();
	}
	
	@Override
	public boolean checkSuperType(EClass eClass, SubProcess object) {
		if ("FlowElementsContainer".equals(eClass.getName())) { //$NON-NLS-1$
			if (!(object instanceof AdHocSubProcess))
				return true;
		}
		return false;
	}

}

