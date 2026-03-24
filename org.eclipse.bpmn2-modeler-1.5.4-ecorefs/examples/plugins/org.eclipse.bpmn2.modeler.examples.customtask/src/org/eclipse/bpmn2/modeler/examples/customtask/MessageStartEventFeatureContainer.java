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

package org.eclipse.bpmn2.modeler.examples.customtask;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.modeler.core.features.CustomShapeFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.ShowPropertiesFeature;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.features.event.StartEventFeatureContainer.CreateStartEventFeature;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;

/**
 *
 */
public class MessageStartEventFeatureContainer extends CustomShapeFeatureContainer {

	@Override
	public ICreateFeature getCreateFeature(IFeatureProvider fp) {
		return new MessageStartEventCreateFeature(fp);
	}

	@Override
	public String getId(EObject object) {
		if (object instanceof StartEvent) {
			return "org.eclipse.bpmn2.modeler.examples.customtask.messageStartEvent";
		}

		return null;
	}

	public class MessageStartEventCreateFeature extends CreateStartEventFeature {

		public MessageStartEventCreateFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public StartEvent createBusinessObject(ICreateContext context) {
			StartEvent startEvent = super.createBusinessObject(context);
			EClass eClass = Bpmn2Package.eINSTANCE.getMessageEventDefinition();
			MessageEventDefinition messageEventDefinition = (MessageEventDefinition) Bpmn2ModelerFactory.eINSTANCE.create(eClass);
			ModelUtil.setID(messageEventDefinition);
			startEvent.getEventDefinitions().add(messageEventDefinition);
			return startEvent;
		}
		
	}

	@Override
	public ICustomFeature[] getCustomFeatures(IFeatureProvider fp) {
		return new ICustomFeature[0];
	}
}
