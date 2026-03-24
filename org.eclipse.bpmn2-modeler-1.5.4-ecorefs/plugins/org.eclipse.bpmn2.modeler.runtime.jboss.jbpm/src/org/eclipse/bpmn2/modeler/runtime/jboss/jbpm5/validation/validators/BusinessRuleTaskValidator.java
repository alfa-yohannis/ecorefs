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

import java.util.Iterator;

import org.eclipse.bpmn2.BusinessRuleTask;
import org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.validation.IValidationContext;

public class BusinessRuleTaskValidator extends AbstractBpmn2ElementValidator<BusinessRuleTask> {

	/**
	 * @param ctx
	 */
	public BusinessRuleTaskValidator(IValidationContext ctx) {
		super(ctx);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(BusinessRuleTask object) {
		
		Iterator<FeatureMap.Entry> iter = object.getAnyAttribute().iterator();
		boolean hasRuleflowGroup = false;
		while (iter.hasNext()) {
			FeatureMap.Entry entry = iter.next();
			if (entry.getEStructuralFeature().getName().equals("ruleFlowGroup")) { //$NON-NLS-1$
				String ruleflowGroup = (String) entry.getValue();
				if (ruleflowGroup!=null && !ruleflowGroup.isEmpty()) {
					hasRuleflowGroup = true;
					break;
				}
			}
		}
		if (!hasRuleflowGroup) {
			addMissingFeatureStatus(object,"ruleFlowGroup",Status.ERROR); //$NON-NLS-1$
		}
		return getResult();
	}	
}