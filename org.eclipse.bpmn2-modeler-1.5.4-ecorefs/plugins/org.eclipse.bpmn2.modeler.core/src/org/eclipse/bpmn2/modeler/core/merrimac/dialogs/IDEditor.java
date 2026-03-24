/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
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

package org.eclipse.bpmn2.modeler.core.merrimac.dialogs;

import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;

/**
 * This class implements an object ID popup editor. An input dialog is used to
 * allow editing of object IDs, and a validator ensures that the ID is unique
 * within the entire document.
 */
public class IDEditor extends TextAndButtonObjectEditor {

	Definitions definitions = null;
	
	/**
	 * @param parent
	 * @param object
	 */
	public IDEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object, feature);
		definitions = ModelUtil.getDefinitions(object);
	}
	
	@Override
	protected void buttonClicked(int buttonId) {
		// Default button was clicked: open a text editor and allow editing of object ID
		String text = getText();
		
		IInputValidator validator = new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText==null || newText.isEmpty()) {
					return Messages.IDEditor_ID_is_Null;
				}
				if (!SyntaxCheckerUtils.isQName(newText)) {
					return Messages.IDEditor_ID_is_Invalid;
				}
				// check for ID collisions with other objects
				TreeIterator<EObject> iter = definitions.eAllContents();
				while (iter.hasNext()) {
					EObject o = iter.next();
					if (o!=object) {
						EStructuralFeature f = o.eClass().getEStructuralFeature("id");
						if (f!=null && o.eGet(f) instanceof String) {
							String id = (String)o.eGet(f);
							if (newText.equals(id)) {
								return NLS.bind(Messages.IDEditor_Duplicate_ID, getObjectName(o));
							}
						}
					}
				}
				return null;
			}
		};

		InputDialog dialog = new InputDialog(
				parent.getShell(),
				Messages.IDEditor_Edit_ID,
				NLS.bind(Messages.IDEditor_Enter_New_ID_for, getObjectName(object)),
				text,
				validator);
		
		if (dialog.open()==Window.OK){
			setValue(dialog.getValue());
		}
	}
	
	private String getObjectName(EObject o) {
		return ModelUtil.getLabel(o) + " \"" + ModelUtil.getDisplayName(o) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
