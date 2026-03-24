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

package org.eclipse.bpmn2.modeler.core.validation;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.validation.model.ConstraintStatus;

/**
 *
 */
public class StatusList extends ArrayList<IStatus> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -940953174548443328L;

	@Override
	public boolean contains(Object o) {
		if (super.contains(o))
			return true;
		if (o instanceof IStatus) {
			IStatus status = (IStatus) o;
			for (IStatus s : this) {
				if (s instanceof ConstraintStatus && status instanceof ConstraintStatus) {
					ConstraintStatus cs = (ConstraintStatus) s;
					ConstraintStatus cStatus = (ConstraintStatus) status;
					if (cs.getTarget()==cStatus.getTarget() &&
							cs.getResultLocus().equals(cStatus.getResultLocus()) &&
							cs.getConstraint().equals(cStatus.getConstraint()) &&
							cs.getSeverity() == cStatus.getSeverity() &&
							cs.getMessage().equals(cStatus.getMessage())) {
						return true;
					}
				}
				if (s.getSeverity() == status.getSeverity() &&
						s.getMessage().equals(status.getMessage()) ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean add(IStatus e) {
		if (!contains(e))
			return super.add(e);
		return false;
	}

}
