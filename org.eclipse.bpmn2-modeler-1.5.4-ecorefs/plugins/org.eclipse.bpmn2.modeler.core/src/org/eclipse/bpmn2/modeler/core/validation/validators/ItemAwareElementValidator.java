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

import java.util.List;

import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class ItemAwareElementValidator extends AbstractBpmn2ElementValidator<ItemAwareElement> {

	/**
	 * Construct a BPMN2 Element Validator from a Validation Context.
	 *
	 * @param ctx
	 */
	public ItemAwareElementValidator(IValidationContext ctx) {
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
	public ItemAwareElementValidator(AbstractBpmn2ElementValidator parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(ItemAwareElement object) {
		if (ProcessValidator.isContainingProcessExecutable(object)) {
			if (isEmpty(object.getItemSubjectRef())) {
				EObject container = object.eContainer();
				if (container instanceof InputOutputSpecification) {
					container = container.eContainer();
				}
				EStructuralFeature doaFeature = container.eClass().getEStructuralFeature("dataOutputAssociation"); //$NON-NLS-1$
				EStructuralFeature diaFeature = container.eClass().getEStructuralFeature("dataInputAssociation"); //$NON-NLS-1$
				
				if (object instanceof DataOutput) {
					// if there's a DataOutputAssociation that references
					// this object, use the data type of the target of the
					// association
					if (doaFeature!=null) {
						for (DataOutputAssociation doa : (List<DataOutputAssociation>) container.eGet(doaFeature)) {
							if (doa.getSourceRef().contains(object)) {
								ItemAwareElement target = doa.getTargetRef();
								if (target!=null) {
									return getResult();
								}
							}
						}
					}
				}
				else if (object instanceof DataInput) {
					// same as above for DataInputs
					if (diaFeature!=null) {
						for (DataInputAssociation dia : (List<DataInputAssociation>) container.eGet(diaFeature)) {
							if (dia.getTargetRef() == object) {
								for (ItemAwareElement source : dia.getSourceRef()) {
									if (source!=null) {
										return getResult();
									}
								}
							}
						}
					}
				}
				
				addMissingFeatureStatus(object,"itemSubjectRef",new EObject[] {container},Status.ERROR); //$NON-NLS-1$
			}
		}
		return getResult();
	}

}

