/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.examples.dynamic;

import org.eclipse.bpmn2.modeler.ui.diagram.Bpmn2ToolBehaviorProvider;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;

public class SampleToolProvider extends Bpmn2ToolBehaviorProvider {

	public SampleToolProvider(IDiagramTypeProvider diagramTypeProvider) {
		super(diagramTypeProvider);
	}

	@Override
	public Object getToolTip(GraphicsAlgorithm ga) {
		return ga.eClass().getName();
	}

}
