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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property;


import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.ImportType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.util.JbpmModelUtil;
import org.eclipse.bpmn2.modeler.ui.property.editors.ItemDefinitionStructureEditor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jdt.core.IType;

public class JbpmItemDefinitionStructureEditor extends ItemDefinitionStructureEditor {

	public JbpmItemDefinitionStructureEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object);
	}

	@Override
	protected boolean canAdd() {
		return true;
	}
	
	@Override
	protected void buttonClicked(int buttonId) {
		if (buttonId==ID_ADD_BUTTON) {
		    IType type = JbpmModelUtil.showImportDialog(object);
			ImportType imp = JbpmModelUtil.addImport(type, object);
			if (imp!=null)
				setText(imp.getName());
		}
		else
			super.buttonClicked(buttonId);
	}
}
