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

package org.eclipse.bpmn2.modeler.ui.property.data;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Interface;
import org.eclipse.bpmn2.Operation;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.TextObjectEditor;
import org.eclipse.bpmn2.modeler.ui.property.editors.SchemaObjectEditor;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Bob Brodt
 *
 */
public class OperationDetailComposite extends DefaultDetailComposite {

	/**
	 * @param parent
	 * @param style
	 */
	public OperationDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * @param section
	 */
	public OperationDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	@Override
	public AbstractPropertiesProvider getPropertiesProvider(EObject object) {
		if (propertiesProvider==null) {
			propertiesProvider = new AbstractPropertiesProvider(object) {
				String[] properties = new String[] {
						"name", //$NON-NLS-1$
						"implementationRef", //$NON-NLS-1$
						"inMessageRef", //$NON-NLS-1$
						"outMessageRef", //$NON-NLS-1$
						"errorRefs" //$NON-NLS-1$
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
	public void createBindings(EObject be) {
		Object data = getData("container"); //$NON-NLS-1$
		if (data instanceof Activity) {
			// If the container object is an Activity, we want to also display
			// the Interface name and implementation.
			Operation op = (Operation) be;
			Interface iface = (Interface) op.eContainer();
			if (isModelObjectEnabled(iface.eClass())) {
				EAttribute name = PACKAGE.getInterface_Name();
				EReference implementationRef = PACKAGE.getInterface_ImplementationRef();
				Composite parent = getAttributesParent();
				
				String displayName = ExtendedPropertiesProvider.getLabel(iface, name);
				ObjectEditor editor = new TextObjectEditor(this,iface,name);
				editor.createControl(parent,displayName);
	
				displayName = ExtendedPropertiesProvider.getLabel(iface, implementationRef);
				editor = new SchemaObjectEditor(this,iface,implementationRef);
				editor.createControl(parent,displayName);
			}
		}
		
		super.createBindings(be);
	}

	@Override
	protected void bindReference(Composite parent, EObject object, EReference reference) {
		if ("implementationRef".equals(reference.getName()) && //$NON-NLS-1$
				isModelObjectEnabled(object.eClass(), reference)) {
			
			if (parent==null)
				parent = getAttributesParent();
			
			String displayName = ExtendedPropertiesProvider.getLabel(object, reference);
			
			SchemaObjectEditor editor = new SchemaObjectEditor(this,object,reference);
			editor.createControl(parent,displayName);
		}
		else
			super.bindReference(parent, object, reference);
	}
}
