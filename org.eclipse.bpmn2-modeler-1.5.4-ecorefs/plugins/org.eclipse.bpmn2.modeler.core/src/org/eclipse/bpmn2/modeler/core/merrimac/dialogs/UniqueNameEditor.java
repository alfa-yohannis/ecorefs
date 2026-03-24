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

import java.util.Iterator;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;

/**
 * This class implements an object name string popup editor. An input dialog is
 * used to prompt the user to enter a new name for an existing EObject. A
 * validator ensures that the name is unique within a given list of like
 * EObjects.
 * 
 * Subclasses must provide:
 * <ol>
 * <li>a list of EObjects whose names should not be duplicated</li>
 * <li>a validator for the name string, {@code validateName(String)}. This
 * validator need not be concerned with checking for duplicates, as this is
 * already done by this class, but it must return an error message string
 * indicating the error or null if the name is valid.</li>
 * </ol>
 */
abstract public class UniqueNameEditor<T extends EObject> extends TextAndButtonObjectEditor {

	/**
	 * @param parent
	 * @param object
	 */
	public UniqueNameEditor(AbstractDetailComposite parent, T object, EStructuralFeature feature) {
		super(parent, object, feature);
	}
	
	protected abstract List<T> getEObjectList();
	
	protected String validateName(String name) {
		return null;
	}
	
	protected boolean isNullAllowed() {
		return false;
	}
	
	@Override
	protected void buttonClicked(int buttonId) {
		// Default button was clicked: open a text editor and allow editing of object ID
		String text = getText();
		
		IInputValidator validator = new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText==null || newText.isEmpty()) {
					if (isNullAllowed())
						return null;
					return Messages.UniqueNameEditor_Name_is_Null;
				}
				String message = validateName(newText);
				if (message!=null)
					return message;
				
				// check for ID collisions with other objects
				Iterator<T> iter = getEObjectList().iterator();
				while (iter.hasNext()) {
					EObject o = iter.next();
					if (o!=object) {
						if (o.eGet(feature) instanceof String) {
							String name = (String)o.eGet(feature);
							if (newText.equals(name)) {
								return NLS.bind(Messages.UniqueNameEditor_Duplicate_Name, newText);
							}
						}
					}
				}
				return null;
			}
		};

		InputDialog dialog = new InputDialog(
				parent.getShell(),
				Messages.UniqueNameEditor_Edit_Name,
				NLS.bind(Messages.UniqueNameEditor_Enter_New_Name_for, getObjectName(object)),
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
