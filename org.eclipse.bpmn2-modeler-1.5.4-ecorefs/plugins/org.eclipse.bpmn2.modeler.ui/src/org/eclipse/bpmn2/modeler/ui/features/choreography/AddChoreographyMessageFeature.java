/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat, Inc and others.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * Horus Software GmbH	- Bug 497543
 *
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.ui.features.choreography;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Choreography;
import org.eclipse.bpmn2.ChoreographyTask;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.features.choreography.ChoreographyUtil;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.model.ModelHandler;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.internal.util.ui.PopupMenu;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * @author Bob Brodt
 *
 */
public class AddChoreographyMessageFeature extends AbstractCustomFeature {

	protected boolean changesDone = false;

	private static ILabelProvider labelProvider = new ILabelProvider() {

		public void removeListener(ILabelProviderListener listener) {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void dispose() {

		}

		public void addListener(ILabelProviderListener listener) {

		}

		public String getText(Object element) {
			return ChoreographyUtil.getMessageName((Message) element);
		}

		public Image getImage(Object element) {
			return null;
		}

	};

	/**
	 * @param fp
	 */
	public AddChoreographyMessageFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public String getName() {
		return Messages.AddChoreographyMessageFeature_Name;
	}

	@Override
	public String getDescription() {
		return Messages.AddChoreographyMessageFeature_Description;
	}

	@Override
	public String getImageId() {
		return ImageProvider.IMG_16_ADD_MESSAGE;
	}

	@Override
	public boolean isAvailable(IContext context) {
		return true;
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement[] pes = context.getPictogramElements();
		if (pes != null && pes.length == 1) {
			PictogramElement pe = pes[0];
			Object bo = getBusinessObjectForPictogramElement(pe);
			if (pe instanceof ContainerShape && bo instanceof Participant) {
				Participant participant = (Participant) bo;

				Object parent = getBusinessObjectForPictogramElement(((ContainerShape) pe).getContainer());
				if (parent instanceof ChoreographyTask) {

					ChoreographyTask choreographyTask = (ChoreographyTask) parent;

					// Check if choreography task already associated with
					// MessageFlow with this Participant as the source
					if (choreographyTask.getParticipantRefs().size() == 2) {
						for (MessageFlow mf : choreographyTask.getMessageFlowRef()) {
							if (mf.getSourceRef() != null && mf.getSourceRef().equals(participant)) {
								return false;
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.graphiti.features.custom.ICustomFeature#execute(org.eclipse.graphiti.features.context.ICustomContext)
	 */
	@Override
	public void execute(ICustomContext context) {
		PictogramElement pe = context.getPictogramElements()[0];
		ContainerShape participantShape = (ContainerShape) pe;
		ContainerShape choreographyTaskShape  = (ContainerShape) participantShape.eContainer();
		Participant participant = (Participant) getBusinessObjectForPictogramElement(participantShape);
		ChoreographyTask choreographyTask = (ChoreographyTask) getBusinessObjectForPictogramElement(choreographyTaskShape);
		Definitions definitions = ModelUtil.getDefinitions(choreographyTask);
		ModelHandler mh = ModelHandler.getInstance(choreographyTask);

		Message message = Bpmn2ModelerFactory.createObject(participant.eResource(), Message.class);
		String oldName = message.getName();
		message.setName(Messages.AddChoreographyMessageFeature_New);
		message.setId(null);

		List<Message> messageList = new ArrayList<Message>();
		messageList.add(message);
		messageList.addAll( ModelUtil.getAllRootElements(definitions, Message.class) );

		Message result = message;
		if (messageList.size() > 1) {
			PopupMenu popupMenu = new PopupMenu(messageList, labelProvider);
			changesDone = popupMenu.show(Display.getCurrent().getActiveShell());
			if (changesDone) {
				result = (Message) popupMenu.getResult();
			} else {
				EcoreUtil.delete(message);
				message = null;
			}
		} else
			changesDone = true;

		if (changesDone) {
			if (result == message) {
				// the new one
				definitions.getRootElements().add(message);
				message.setId(null);
				ModelUtil.setID(message);
				message.setName(oldName);
			} else {
				// and existing one
				
				EcoreUtil.delete(message);
				message = result;
			}

			// get the other Participant to which this Participant will be sending the Message
			// Note that we have already checked (in canExecute()) that this Choreography Task
			// has only two Participants.
			Participant otherParticipant;
			if (choreographyTask.getParticipantRefs().get(0)==participant)
				otherParticipant = choreographyTask.getParticipantRefs().get(1);
			else
				otherParticipant = choreographyTask.getParticipantRefs().get(0);
			
			MessageFlow messageFlow = mh.createMessageFlow(participant, otherParticipant);
			messageFlow.setName(ModelUtil.toCanonicalString(messageFlow.getId()));

			Choreography choreography = (Choreography) choreographyTask.eContainer();
			choreography.getMessageFlows().add(messageFlow);

			messageFlow.setMessageRef(message);
			choreographyTask.getMessageFlowRef().add(messageFlow);

			BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(participantShape, BPMNShape.class);
			bpmnShape.setIsMessageVisible(true);
			
			UpdateContext updateContext = new UpdateContext(choreographyTaskShape);
			getFeatureProvider().updateIfPossible(updateContext);
		}
	}

	@Override
	public boolean hasDoneChanges() {
		return changesDone;
	}
}
