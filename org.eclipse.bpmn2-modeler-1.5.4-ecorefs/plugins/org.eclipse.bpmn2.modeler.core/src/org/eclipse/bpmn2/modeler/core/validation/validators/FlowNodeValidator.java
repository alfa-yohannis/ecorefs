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

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.AdHocSubProcess;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class FlowNodeValidator extends AbstractBpmn2ElementValidator<FlowNode> {

	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public FlowNodeValidator(IValidationContext ctx) {
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
	public FlowNodeValidator(AbstractBpmn2ElementValidator parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(FlowNode object) {
		if (object instanceof Gateway)
			return getResult();

		boolean needIncoming = true;
		boolean needOutgoing = true;
		if (object instanceof ThrowEvent)
			needOutgoing = false;
		if (object instanceof CatchEvent)
			needIncoming = false;
		
		if (object instanceof SubProcess) {
			if (((SubProcess) object).isTriggeredByEvent()) {
				// Event SubProcesses may not be connected
				// to the rest of the Process
				if (object.getOutgoing().size()>0) {
					addStatus(object,"outgoing",Status.ERROR,org.eclipse.bpmn2.modeler.core.validation.validators.Messages.FlowNodeValidator_EventSubProcess_Has_Outgoing); //$NON-NLS-1$
				}
				if (object.getIncoming().size()>0) {
					addStatus(object,"incoming",Status.ERROR,org.eclipse.bpmn2.modeler.core.validation.validators.Messages.FlowNodeValidator_EventSubProcess_Has_Incoming); //$NON-NLS-1$
				}
				needIncoming = false;
				needOutgoing = false;
			}
		}
		if (object instanceof Activity) {
			if (((Activity)object).isIsForCompensation()) {
				needIncoming = false;
				needOutgoing = false;
			}
		}
		if (object instanceof BoundaryEvent) {
			for (EventDefinition ed : ((BoundaryEvent)object).getEventDefinitions()) {
				if (ed instanceof CompensateEventDefinition) {
					needOutgoing = false;
					break;
				}
			}

		}
		if (object.eContainer() instanceof AdHocSubProcess) {
			needIncoming = false;
			needOutgoing = false;
		}
		
		if (needOutgoing) {
			if ((object.getOutgoing() == null || object.getOutgoing().size() < 1)) {
				addMissingFeatureStatus(object,"outgoing",Status.ERROR); //$NON-NLS-1$
			}
		}
		if (needIncoming) {
			if ((object.getIncoming() == null || object.getIncoming().size() < 1)) {
				addMissingFeatureStatus(object,"incoming",Status.ERROR); //$NON-NLS-1$
			}
		}
		if (Bpmn2Preferences.getInstance(object).getAllowMultipleConnections() == false) {
			if (object.getIncoming().size()>1)
				addStatus(object,Status.WARNING,org.eclipse.bpmn2.modeler.core.validation.validators.Messages.FlowNodeValidator_Only_One_Incoming_Allowed,object.eClass().getName());
			if (object.getOutgoing().size()>1)
				addStatus(object,Status.WARNING,org.eclipse.bpmn2.modeler.core.validation.validators.Messages.FlowNodeValidator_Only_One_Outgoing_Allowed,object.eClass().getName());
		}
		return getResult();
	}

}

