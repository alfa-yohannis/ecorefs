/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.merrimac.clad;

import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.swt.widgets.Composite;

public interface IPropertiesCompositeFactory {
	AbstractDetailComposite createDetailComposite(Class eClass, AbstractBpmn2PropertySection section, TargetRuntime targetRuntime);
	AbstractDetailComposite createDetailComposite(Class eClass, Composite parent, TargetRuntime targetRuntime, int style);

	AbstractListComposite createListComposite(Class eClass, AbstractBpmn2PropertySection section, TargetRuntime targetRuntime);
	AbstractListComposite createListComposite(Class eClass, Composite parent, TargetRuntime targetRuntime, int style);
	
	AbstractDialogComposite createDialogComposite(EClass eClass, Composite parent, TargetRuntime targetRuntime, int style);
	
}
