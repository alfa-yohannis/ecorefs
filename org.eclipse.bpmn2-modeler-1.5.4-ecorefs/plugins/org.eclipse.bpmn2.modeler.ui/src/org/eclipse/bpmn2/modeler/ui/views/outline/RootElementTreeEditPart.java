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
package org.eclipse.bpmn2.modeler.ui.views.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.modeler.ui.util.PropertyUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.swt.graphics.Image;

public class RootElementTreeEditPart extends AbstractGraphicsTreeEditPart {
	
	public RootElementTreeEditPart(DiagramTreeEditPart dep, RootElement graphicsAlgorithm) {
		super(dep, graphicsAlgorithm);
	}

	public RootElement getRootElement() {
		return (RootElement) getModel();
	}

	// ======================= overwriteable behaviour ========================

	/**
	 * Creates the EditPolicies of this EditPart. Subclasses often overwrite
	 * this method to change the behaviour of the editpart.
	 */
	@Override
	protected void createEditPolicies() {
	}
	
	@Override
	protected Image getImage() {
		EObject o = (EObject)getModel();
		return PropertyUtil.getImage(o);
	}

	@Override
	protected List<Object> getModelChildren() {
		List<Object> retList = new ArrayList<Object>();
		RootElement elem = getRootElement();
		if (elem != null && elem.eResource() != null) {
			if (elem instanceof FlowElementsContainer) {
				FlowElementsContainer container = (FlowElementsContainer)elem;
				retList.addAll(FlowElementTreeEditPart.getFlowElementsContainerChildren(container));
				return retList;
			}
			if (elem instanceof Collaboration) {
				Collaboration collaboration = (Collaboration)elem;
				retList.addAll(collaboration.getParticipants());
				retList.addAll(collaboration.getConversations());
				retList.addAll(collaboration.getConversationLinks());
				retList.addAll(collaboration.getMessageFlows());
				retList.addAll(collaboration.getArtifacts());
			}
		}
		return retList;
	}
	
	protected void reorderChild(EditPart editpart, int index) {
		removeChildVisual(editpart);
		List children = getChildren();
		children.remove(editpart);
		if (index>=children.size())
			index = children.size() - 1;
		children.add(index, editpart);
		addChildVisual(editpart, index);
	}

}