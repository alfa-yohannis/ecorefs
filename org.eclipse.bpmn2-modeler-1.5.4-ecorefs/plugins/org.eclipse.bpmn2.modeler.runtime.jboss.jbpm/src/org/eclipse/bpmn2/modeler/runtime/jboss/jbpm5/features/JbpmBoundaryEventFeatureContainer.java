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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.features;

import org.eclipse.bpmn2.ScriptTask;
import org.eclipse.bpmn2.modeler.ui.features.event.AddBoundaryEventFeature;
import org.eclipse.bpmn2.modeler.ui.features.event.BoundaryEventFeatureContainer;
import org.eclipse.bpmn2.modeler.ui.features.event.CreateBoundaryEventFeature;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICreateContext;

public class JbpmBoundaryEventFeatureContainer extends BoundaryEventFeatureContainer {

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return new AddBoundaryEventFeature(fp){

			@Override
			public boolean canAdd(IAddContext context) {
				if (super.canAdd(context)) {
					Object bo = getBusinessObjectForPictogramElement(context.getTargetContainer());
					return !(bo instanceof ScriptTask);
				}
				return false;
			}
			
		};
	}

	@Override
	public ICreateFeature getCreateFeature(IFeatureProvider fp) {
		return new CreateBoundaryEventFeature(fp) {

			@Override
			public boolean canCreate(ICreateContext context) {
				if (super.canCreate(context)) {
					Object bo = getBusinessObjectForPictogramElement(context.getTargetContainer());
					return !(bo instanceof ScriptTask);
				}
				return false;
			}
		};
	}
}
