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

package org.eclipse.bpmn2.modeler.ui.property.events;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.LinkEventDefinition;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Bob Brodt
 *
 */
public class LinkEventDefinitionDetailComposite extends DefaultDetailComposite {
	
	/**
	 * @param parent
	 * @param style
	 */
	public LinkEventDefinitionDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * @param section
	 */
	public LinkEventDefinitionDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	@Override
	public void cleanBindings() {
		super.cleanBindings();
	}

	@Override
	public void createBindings(EObject be) {
		final LinkEventDefinition eventDefinition = (LinkEventDefinition)be;
		if (eventDefinition.eContainer() instanceof Event) {
			Event event = (Event) eventDefinition.eContainer();
			this.bindAttribute(getAttributesParent(), eventDefinition, Bpmn2Package.eINSTANCE.getLinkEventDefinition_Name());
			if (event instanceof CatchEvent) {
				bindReference(eventDefinition, Bpmn2Package.eINSTANCE.getLinkEventDefinition_Source());
			}
			else {
				bindReference(eventDefinition, Bpmn2Package.eINSTANCE.getLinkEventDefinition_Target());
			}
		}
	}
}
