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
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator;
import org.eclipse.bpmn2.modeler.core.validation.validators.BaseElementValidator;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.ProcessVariableNameChangeAdapter;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.ExternalProcess;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.GlobalType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.util.JbpmModelUtil;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.Messages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.IValidationContext;

public class ProcessVariableNameValidator extends AbstractBpmn2ElementValidator<BaseElement> {

	/**
	 * @param ctx
	 */
	public ProcessVariableNameValidator(IValidationContext ctx) {
		super(ctx);
	}
	
	/**
	 * Construct a BPMN2 Element Validator with the given Validator as the parent.
	 * The parent is responsible for collecting all of the validation Status objects
	 * and reporting them back to the Validation Constraint.
	 *
	 * @param parent a parent Validator class
	 */
	public ProcessVariableNameValidator(AbstractBpmn2ElementValidator<?> other) {
		super(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.
	 * AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(BaseElement object) {
		String id = null;
		String featureName = null;
		if (object instanceof GlobalType) {
			id = ((GlobalType) object).getIdentifier();
			featureName = "identifier"; //$NON-NLS-1$
		}
		else if (object instanceof BaseElement) {
			id = ((BaseElement) object).getId();
			featureName = "id"; //$NON-NLS-1$
		}

		if (isEmpty(id)) {
			IStatus status = new BaseElementValidator(this).validate(object);
		}
		else {
			if (object instanceof Process || object instanceof ExternalProcess) {
				if (!JbpmModelUtil.isProcessId(id)) {
					addStatus(object, featureName, Status.ERROR, Messages.ProcessVariableNameValidator_ID_Invalid, object.eClass().getName(), id);
				}
			}
			else if (ProcessVariableNameChangeAdapter.appliesTo(object)) {
				if (!SyntaxCheckerUtils.isJavaIdentifier(id)) {
					addStatus(object, featureName, Status.ERROR, Messages.ProcessVariableNameValidator_ID_Invalid, object.eClass().getName(), id);
				}
			}
			else {
				if (!SyntaxCheckerUtils.isNCName(id)) {
					addStatus(object, featureName, Status.ERROR, Messages.ProcessVariableNameValidator_ID_Invalid, object.eClass().getName(), id);
				}
			}

			Definitions definitions = ModelUtil.getDefinitions(object);
			TreeIterator<EObject> iter = definitions.eAllContents();
			while (iter.hasNext()) {
				EObject o2 = iter.next();
				if (o2 instanceof BaseElement && object!=o2) {
					String id2;
					if (o2 instanceof GlobalType)
						id2 = ((GlobalType) o2).getIdentifier();
					else
						id2 = ((BaseElement) o2).getId();
					if (id != null && id2 != null) {
						if (id.equals(id2)) {
							addStatus(object, featureName, Status.ERROR,
									Messages.ProcessVariableNameConstraint_Duplicate_ID,
									getLabel(object)
											+ " " + getName(object), //$NON-NLS-1$
									getLabel(o2)
											+ " " + getName(o2)); //$NON-NLS-1$
						}
					}
				}
			}
		}
		return getResult();
	}
}