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

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.ComplexGateway;
import org.eclipse.bpmn2.EventBasedGateway;
import org.eclipse.bpmn2.ExclusiveGateway;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.GatewayDirection;
import org.eclipse.bpmn2.InclusiveGateway;
import org.eclipse.bpmn2.ParallelGateway;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.validation.IValidationContext;

/**
 * This class is responsible to validate the gateway direction 
 * for the different gateway types. The Gateway type relates to 
 * the number of incoming and outgoing sequence flows. 
 * <ul>
 * <li>Converging gateways MUST have multiple incoming, and zero or one outgoing connection</li>
 * <li>Diverging gateways MUST have zero or one incoming, and multiple outgoing connections</li>
 * <li>Mixed gateways MUST have multiple incoming, and multiple outgoing connections</li>
 * <li>Unspecified gateways MUST have either multiple incoming, or multiple outgoing connections</li>
 * </ul>
 * 
 * Issues: #550406
 * 
 */
public class GatewayValidator extends AbstractBpmn2ElementValidator<Gateway> {

	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public GatewayValidator(IValidationContext ctx) {
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
	public GatewayValidator(AbstractBpmn2ElementValidator parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(Gateway object) {
		GatewayDirection direction = object.getGatewayDirection();
		int incoming = object.getIncoming().size();
		int outgoing = object.getOutgoing().size();
		if (direction == GatewayDirection.CONVERGING) {
			// Converging gateways MUST have multiple incoming, and zero or one outgoing connection
			if (incoming<1) {
				ctx.addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Converging_Multiple_Incoming);
			}
			if (outgoing>1) {
				ctx.addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Converging_One_Outgoing);
			}
		}
		else if (direction == GatewayDirection.DIVERGING) {
			// Diverging gateways MUST have zero or one incoming, and multiple outgoing connections
			if (incoming>1) {
				ctx.addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Diverging_One_Incoming);
			}
			if (outgoing<1) {
				ctx.addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Diverging_Multiple_Outgoing);
			}
		}
		else if (direction == GatewayDirection.MIXED) {
			// Mixed gateways MUST have multiple incoming, and multiple outgoing connections
			if (incoming<1) {
				ctx.addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Mixed_Multiple_Incoming);
			}
			if (outgoing<1) {
				ctx.addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Mixed_Multiple_Outgoing);
			}
		}
		else {
			// Unspecified gateways MUST have either multiple incoming, or multiple outgoing connections
			if (outgoing<1 && incoming<1) {
				ctx.addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Unspecified_Multiple_Incoming_Outgoing);
			}
		}
		
		// validate direction type (skip for mixed gateway type)
		if (direction != GatewayDirection.MIXED) {
			if (object instanceof ExclusiveGateway) {
				if (direction != GatewayDirection.DIVERGING
						&& direction != GatewayDirection.CONVERGING) {
					addStatus(object, "gatewayDirection", //$NON-NLS-1$
							Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Exclusive_Converging_Diverging);
				}
			}
			if (object instanceof EventBasedGateway) {
				if (direction != GatewayDirection.DIVERGING) {
					addStatus(object,"gatewayDirection", //$NON-NLS-1$
							Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Event_Diverging);
				}
			}
			if (object instanceof ParallelGateway) {
				if (direction != GatewayDirection.DIVERGING
						&& direction != GatewayDirection.CONVERGING) {
					addStatus(object,"gatewayDirection", //$NON-NLS-1$
							Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Parallel_Converging_Diverging);
				}
			}
			if (object instanceof InclusiveGateway) {
				if (direction != GatewayDirection.DIVERGING
						&& direction != GatewayDirection.CONVERGING) {
					addStatus(object,"gatewayDirection", //$NON-NLS-1$
							Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Inclusive_Converging_Diverging);
				}
			}
			if (object instanceof ComplexGateway) {
				if (direction != GatewayDirection.DIVERGING
						&& direction != GatewayDirection.CONVERGING) {
					addStatus(object,"gatewayDirection", //$NON-NLS-1$
							Status.ERROR, org.eclipse.bpmn2.modeler.core.validation.validators.Messages.GatewayValidator_Complex_Converging_Diverging);
				}
			}
		}
		return getResult();
	}

}

