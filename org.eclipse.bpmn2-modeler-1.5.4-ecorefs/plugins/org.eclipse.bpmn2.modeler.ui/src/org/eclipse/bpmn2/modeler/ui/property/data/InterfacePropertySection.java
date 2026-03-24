/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 * All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.property.data;


import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Interface;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultPropertySection;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;

public class InterfacePropertySection extends DefaultPropertySection {

	@Override
	protected AbstractDetailComposite createSectionRoot() {
		return new InterfaceDetailComposite(this);
	}

	@Override
	public AbstractDetailComposite createSectionRoot(Composite parent, int style) {
		return new InterfaceDetailComposite(parent, style);
	}

	public InterfacePropertySection() {
		super();
	}
	
	@Override
	public boolean appliesTo(IWorkbenchPart part, ISelection selection) {
		if (super.appliesTo(part, selection)) {
			if (isModelObjectEnabled(Bpmn2Package.eINSTANCE.getInterface())) {
				EObject bo = getBusinessObjectForSelection(selection);
				return bo!=null;
			}
		}
		return false;
	}
	
	@Override
	public EObject getBusinessObjectForSelection(ISelection selection) {
		EObject bo = super.getBusinessObjectForSelection(selection);
		if (bo instanceof Interface) {
			return bo;
		}
		
		return null;
	}
}
