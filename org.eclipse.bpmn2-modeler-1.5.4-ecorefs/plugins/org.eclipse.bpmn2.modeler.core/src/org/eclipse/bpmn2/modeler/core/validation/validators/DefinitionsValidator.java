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

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Import;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class DefinitionsValidator extends AbstractBpmn2ElementValidator<Definitions> {

	public DefinitionsValidator(AbstractBpmn2ElementValidator<?> other) {
		super(other);
	}

	public DefinitionsValidator(IValidationContext ctx) {
		super(ctx);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(Definitions object) {
		// See https://issues.jboss.org/browse/JBPM-4860
		if (object.getTargetNamespace()==null || object.getTargetNamespace().isEmpty()) {
			addStatus(object, Status.WARNING, Messages.DefinitionsValidator_No_TargetNamespace);
		}
		for (Import elem : object.getImports()) {
			if (isEmpty(elem.getLocation())) {
				addMissingFeatureStatus(elem,"location",Status.ERROR); //$NON-NLS-1$
			}
			if (isEmpty(elem.getNamespace())) {
				addMissingFeatureStatus(elem,"namespace",Status.ERROR); //$NON-NLS-1$
			}
			if (isEmpty(elem.getImportType())) {
				addMissingFeatureStatus(elem,"importType",Status.ERROR); //$NON-NLS-1$
			}
		}
		if (isLiveValidation()) {
			TreeIterator<EObject> iter = object.eAllContents();
			while (iter.hasNext()) {
				EObject o = iter.next();
				if (o instanceof BaseElement) {
					addStatus(new BaseElementValidator(this).validate((BaseElement)o));
				}
			}
		}
		return getResult();
	}

	public boolean doLiveValidation() {
		return true;
	}
}
