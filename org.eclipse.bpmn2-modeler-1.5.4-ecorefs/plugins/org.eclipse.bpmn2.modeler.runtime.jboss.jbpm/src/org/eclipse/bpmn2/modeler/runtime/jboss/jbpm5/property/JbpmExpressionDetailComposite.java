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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property;

import org.eclipse.bpmn2.Assignment;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.property.data.ExpressionDetailComposite;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Composite;

/**
 * See https://bugzilla.redhat.com/show_bug.cgi?id=1144961
 */
public class JbpmExpressionDetailComposite extends ExpressionDetailComposite {

	/**
	 * @param parent
	 * @param style
	 */
	public JbpmExpressionDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * @param section
	 */
	public JbpmExpressionDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	@Override
	public AbstractPropertiesProvider getPropertiesProvider(final EObject object) {
		if (propertiesProvider==null) {
			propertiesProvider = new AbstractPropertiesProvider(object) {
				String[] properties1 = new String[] {
						"body", //$NON-NLS-1$
				};
				String[] properties2 = new String[] {
						"language", //$NON-NLS-1$
						"body", //$NON-NLS-1$
				};
				
				@Override
				public String[] getProperties() {
					EObject container = ModelUtil.getContainer(object);
					if (container instanceof Assignment) {
						return properties1;
					}
					return properties2; 
				}
			};
		}
		return propertiesProvider;
	}
}
