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
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.ConditionalEventDefinition;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.Error;
import org.eclipse.bpmn2.ErrorEventDefinition;
import org.eclipse.bpmn2.Escalation;
import org.eclipse.bpmn2.EscalationEventDefinition;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.Signal;
import org.eclipse.bpmn2.SignalEventDefinition;
import org.eclipse.bpmn2.TimerEventDefinition;
import org.eclipse.bpmn2.modeler.core.utils.EventDefinitionsUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.Tuple;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class EventDefinitionValidator extends AbstractBpmn2ElementValidator<EventDefinition> {

	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public EventDefinitionValidator(IValidationContext ctx) {
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
	public EventDefinitionValidator(AbstractBpmn2ElementValidator parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(EventDefinition ed) {
		BaseElement edContainer = (BaseElement) ed.eContainer();
		if (ProcessValidator.isContainingProcessExecutable(edContainer)) {
			if (ed instanceof TimerEventDefinition) {
				TimerEventDefinition ted = (TimerEventDefinition) ed;
				if (ted.getTimeDate() == null && ted.getTimeDuration() == null && ted.getTimeCycle() == null) {
					addMissingFeatureStatus(edContainer,Messages.EventDefinitionValidator_Timer, Status.ERROR);
				}
			} else if (ed instanceof SignalEventDefinition) {
				Signal signal = ((SignalEventDefinition) ed).getSignalRef();
				if (signal==null)
					addMissingFeatureStatus(edContainer,Messages.EventDefinitionValidator_Signal, Status.ERROR);
				else
					new SignalValidator(this).validate(signal);
			} else if (ed instanceof ErrorEventDefinition) {
				Error error = ((ErrorEventDefinition) ed).getErrorRef();
				if (error==null)
					addMissingFeatureStatus(edContainer,Messages.EventDefinitionValidator_Error, Status.ERROR);
				else
					new ErrorValidator(this).validate(error);
			} else if (ed instanceof ConditionalEventDefinition) {
				FormalExpression expression = (FormalExpression) ((ConditionalEventDefinition) ed).getCondition();
				if (expression==null || isEmpty(expression.getBody()))
					addMissingFeatureStatus(edContainer,Messages.EventDefinitionValidator_Condition, Status.ERROR);
				else
					new ExpressionValidator(this).validate(expression);
			} else if (ed instanceof EscalationEventDefinition) {
				Escalation escalation = ((EscalationEventDefinition) ed).getEscalationRef();
				if (escalation==null)
					addMissingFeatureStatus(edContainer,Messages.EventDefinitionValidator_Escalation, Status.ERROR);
				else
					new EscalationValidator(this).validate(escalation);
			} else if (ed instanceof MessageEventDefinition) {
				Message message = ((MessageEventDefinition) ed).getMessageRef();
				if (message==null)
					addMissingFeatureStatus(edContainer,Messages.EventDefinitionValidator_Message, Status.ERROR);
				else
					new MessageValidator(this).validate(message);
			} else if (ed instanceof CompensateEventDefinition) {
				Activity activity = ((CompensateEventDefinition) ed).getActivityRef();
				if (activity==null)
					addMissingFeatureStatus(edContainer,Messages.EventDefinitionValidator_CalledActivity, Status.ERROR);
				else
					new ActivityValidator(this).validate(activity);
			}
		
			if (edContainer instanceof Event && EventDefinitionsUtil.hasItemDefinition(ed)) {
				// get Data Association and make sure both source and target are defined
				Tuple<ItemAwareElement,DataAssociation> param = EventDefinitionsUtil.getIOParameter((Event)edContainer, ed);
				DataAssociation da = param.getSecond();
				if (da instanceof DataInputAssociation) {
					if (((DataInputAssociation)da).getSourceRef().size()==0) {
						addStatus(edContainer,Status.ERROR,Messages.EventDefinitionValidator_No_Source_DataItem,
								ModelUtil.getLabel(ed),
								ModelUtil.getLabel(edContainer));
					}
				}
				else if (da instanceof DataOutputAssociation) {
					if (((DataOutputAssociation)da).getTargetRef()==null) {
						addStatus(edContainer,Status.ERROR,Messages.EventDefinitionValidator_No_Target_DataItem,
								ModelUtil.getLabel(ed));
					}
				}
			}
		}
		
		return getResult();
	}

}

