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

import java.util.List;

import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.modeler.core.model.ModelDecorator;
import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator;
import org.eclipse.bpmn2.modeler.core.validation.validators.BaseElementValidator;
import org.eclipse.bpmn2.modeler.core.validation.validators.FlowElementsContainerValidator;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.Messages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.validation.IValidationContext;

public class ProcessValidator extends AbstractBpmn2ElementValidator<Process> {

	/**
	 * @param ctx
	 */
	public ProcessValidator(IValidationContext ctx) {
		super(ctx);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(Process object) {
		new org.eclipse.bpmn2.modeler.core.validation.validators.ProcessValidator(this).validate(object);

		EStructuralFeature feature;
		feature = ModelDecorator.getAnyAttribute(object, "packageName"); //$NON-NLS-1$
		String name = null;
		if (feature!=null) {
			name = (String) object.eGet(feature);
		}
		if (name==null || name.isEmpty()) {
			// See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=482967
			// package name is not required
//			addStatus(object, "packageName", Status.ERROR, Messages.ProcessConstraint_No_Package_Name, object.getName(), object.getId()); //$NON-NLS-1$
		}
		else if (!SyntaxCheckerUtils.isJavaPackageName(name)) {
			addStatus(object, "packageName", Status.ERROR, Messages.ProcessValidator_Invalid_PackageName, name); //$NON-NLS-1$
		}

		name = object.getName();
		if (name==null || name.isEmpty()) {
			addStatus(object, "name", Status.ERROR, Messages.ProcessConstraint_No_Process_Name, object.getId()); //$NON-NLS-1$
		}
		
		feature = ModelDecorator.getAnyAttribute(object, "adHoc"); //$NON-NLS-1$
		if (feature!=null) {
			Boolean adHoc = (Boolean) object.eGet(feature);
			if (!adHoc.booleanValue()) {
				// This is not an ad-hoc process:
				// need to make sure all nodes are connected,
				// same as core BPMN2
				new FlowElementsContainerValidator(this).validate(object);
			}
		}
		else {
			// Default value for missing "adHoc" attribute is "false"
			new FlowElementsContainerValidator(this).validate(object);
		}
		
		new BaseElementValidator(this).validate(object);
		
		return getResult();
	}
}