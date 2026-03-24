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
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.property.data;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ComboObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.swt.widgets.Composite;

public class ItemAwareElementDetailComposite extends DefaultDetailComposite {

	private AbstractPropertiesProvider propertiesProvider;

	public ItemAwareElementDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * @param section
	 */
	public ItemAwareElementDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	@Override
	public AbstractPropertiesProvider getPropertiesProvider(EObject object) {
		if (propertiesProvider==null) {
			propertiesProvider = new AbstractPropertiesProvider(object) {
				String[] properties = new String[] {
						"id", //$NON-NLS-1$
						"name", //$NON-NLS-1$
						"isCollection", //$NON-NLS-1$
						"itemSubjectRef", //$NON-NLS-1$
						"dataState", //$NON-NLS-1$
				};
				
				@Override
				public String[] getProperties() {
					return properties; 
				}
			};
		}
		return propertiesProvider;
	}
	
	@Override
	protected void bindReference(Composite parent, EObject object, EReference reference) {
		if (reference.getName().equals("itemSubjectRef")) { //$NON-NLS-1$
			if (isModelObjectEnabled(object.eClass(), reference)) {
				if (parent==null)
					parent = getAttributesParent();
				
				String displayName = getBusinessObjectDelegate().getLabel(object, reference);
				ObjectEditor editor = new ComboObjectEditor(this,object,reference, Bpmn2Package.eINSTANCE.getItemDefinition());
				editor.createControl(parent,displayName);
			}
		}
		else
			super.bindReference(parent, object, reference);
	}
}
