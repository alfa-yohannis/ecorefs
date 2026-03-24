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

import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.GatewayDirection;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.validation.IValidationContext;

public class GatewayValidator extends org.eclipse.bpmn2.modeler.core.validation.validators.GatewayValidator {

	/**
	 * @param ctx
	 */
	public GatewayValidator(IValidationContext ctx) {
		super(ctx);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(Gateway object) {
		super.validate(object);
		if (getResult().isOK()) {
			// jBPM only supports converging & diverging gateways
			GatewayDirection direction = object.getGatewayDirection();
			if (direction != GatewayDirection.DIVERGING
					&& direction != GatewayDirection.CONVERGING) {
				addStatus(object, "gatewayDirection", //$NON-NLS-1$
						Status.ERROR, Messages.GatewayValidator_Invalid_GatewayDirection);
			}
		}
		
		return getResult();
	}
}