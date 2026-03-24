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
package org.eclipse.bpmn2.modeler.ui;

import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;


/**
 * This class is intended to only be used by the generic BPMN2 Modeler editor.
 * It should be considered "final" and, as such, should not be subclassed.
 * 
 * Clients should extend AbstractBpmn2RuntimeExtension instead and provide
 * their own getTargetNamespace() method.
 * 
 * Also, the Target Namespace for and extension plugin must be unique.
 */
public class DefaultBpmn2RuntimeExtension extends AbstractBpmn2RuntimeExtension {

	private static final String targetNamespace = "http://org.eclipse.bpmn2/default"; //$NON-NLS-1$
	
	public DefaultBpmn2RuntimeExtension() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension#getTargetNamespace(org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType)
	 */
	@Override
	public String getTargetNamespace(Bpmn2DiagramType diagramType){
		String type = ""; //$NON-NLS-1$
		if (diagramType != null) {
			switch (diagramType) {
			case PROCESS:
				type = "/process"; //$NON-NLS-1$
				break;
			case COLLABORATION:
				type = "/collaboration"; //$NON-NLS-1$
				break;
			case CHOREOGRAPHY:
				type = "/choreography"; //$NON-NLS-1$
				break;
			default:
				type = ""; //$NON-NLS-1$
				break;
			}
		}
		return targetNamespace + type;
	}
}
