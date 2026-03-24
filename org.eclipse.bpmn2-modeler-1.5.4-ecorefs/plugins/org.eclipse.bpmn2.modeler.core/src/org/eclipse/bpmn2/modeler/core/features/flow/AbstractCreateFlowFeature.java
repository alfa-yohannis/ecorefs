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
 * @author Ivar Meikas
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features.flow;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Association;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.ChoreographyTask;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Conversation;
import org.eclipse.bpmn2.ConversationLink;
import org.eclipse.bpmn2.ConversationNode;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.SubChoreography;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CreateConnectionFeature;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.ChopboxAnchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;

public abstract class AbstractCreateFlowFeature<
		CONNECTION extends BaseElement,
		SOURCE extends EObject,
		TARGET extends EObject>
	extends AbstractBpmn2CreateConnectionFeature<CONNECTION, SOURCE, TARGET> {

	public AbstractCreateFlowFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canCreate(ICreateConnectionContext context) {
		SOURCE source = getSourceBo(context);
		TARGET target = getTargetBo(context);
		if (source!=null && target!=null) {
			// Make sure only one connection of each type is created for the same
			// source and target objects, i.e. you can't have two SequenceFlows
			// with the same source and target objects.
			AnchorContainer sourceContainer = null;
			AnchorContainer targetContainer = null;
			if (context.getSourceAnchor()!=null)
				sourceContainer = context.getSourceAnchor().getParent();
			else if (context.getSourcePictogramElement() instanceof FreeFormConnection) {
				return true;
			}
			if (context.getTargetAnchor()!=null) {
				targetContainer = context.getTargetAnchor().getParent();
			}
			else if (context.getTargetPictogramElement() instanceof FreeFormConnection) {
				return true;
			}
			if (!canCreateConnection(sourceContainer, targetContainer, getBusinessObjectClass(), null))
				return false;
			return true;
		}
		return false;
	}

	@Override
	public Connection create(ICreateConnectionContext context) {
		Connection connection = null;
		CONNECTION businessObject = createBusinessObject(context);
		if (businessObject!=null) {
			AnchorContainer source = (AnchorContainer) context.getSourcePictogramElement();
			// If source shape is a Label, replace it with its owner.
			if (FeatureSupport.isLabelShape(source))
				source = (AnchorContainer) FeatureSupport.getLabelOwner(source);
			AnchorContainer target = (AnchorContainer) context.getTargetPictogramElement();
			// Same for target shape
			if (FeatureSupport.isLabelShape(target))
				target = (AnchorContainer) FeatureSupport.getLabelOwner(target);
			Anchor sourceAnchor = context.getSourceAnchor();
			Anchor targetAnchor = context.getTargetAnchor();
			ILocation sourceLoc = context.getSourceLocation();
			ILocation targetLoc = context.getTargetLocation();
			Point p;
			// need to create a source anchor?
			if (sourceAnchor==null || sourceAnchor instanceof ChopboxAnchor) {
				if (sourceLoc!=null) {
					// use the source location if available
					p = GraphicsUtil.createPoint(sourceLoc);
				}
				else if (targetLoc!=null) {
					// or the target location
					p = GraphicsUtil.createPoint(targetLoc);
				}
				else if (targetAnchor!=null) {
					// the target anchor as a reference point
					p = GraphicsUtil.createPoint(targetAnchor);
				}
				else {
					// fallback is to use the center point of the
					// target PE as the reference point
					p = GraphicsUtil.getShapeCenter(target);
				}
				sourceAnchor = AnchorUtil.createAnchor(source, p);	
			}
			
			// same thing for target anchor
			if (targetAnchor==null || targetAnchor instanceof ChopboxAnchor) {
				if (targetLoc!=null) {
					p = GraphicsUtil.createPoint(targetLoc);
				}
				else if (sourceLoc!=null) {
					p = GraphicsUtil.createPoint(sourceLoc);
				}
				else if (sourceAnchor!=null) {
					// the target anchor as a reference point
					p = GraphicsUtil.createPoint(sourceAnchor);
				}
				else {
					p = GraphicsUtil.getShapeCenter(source);
				}
				targetAnchor = AnchorUtil.createAnchor(target, p);	
			}
			
			((CreateConnectionContext)context).setSourceAnchor(sourceAnchor);
			((CreateConnectionContext)context).setTargetAnchor(targetAnchor);
			AddConnectionContext addContext = createAddConnectionContext(context, businessObject);
			addContext.setNewObject(businessObject);

			connection = (Connection) getFeatureProvider().addIfPossible(addContext);
	
			FeatureSupport.updateConnection(getFeatureProvider(), connection);
	
			changesDone = true;
		}
		else
			changesDone = false;
		
		return connection;
	}
	
	@Override
	public CONNECTION createBusinessObject(ICreateConnectionContext context) {
		CONNECTION businessObject = super.createBusinessObject(context);
		SOURCE source = getSourceBo(context);
		TARGET target = getTargetBo(context);
		addConnectionToContainer(businessObject, source, target);

		return businessObject;
	}

	protected abstract String getStencilImageId();

	@Override
	public String getCreateImageId() {
		return getStencilImageId();
	}

	@Override
	public String getCreateLargeImageId() {
		return getStencilImageId();
	}
	
	/**
	 * Add a connection object to its correct container, depending on the type of connection.
	 * The container is determined by the connection's source object, which may be either a
	 * FlowNode or InteractionNode depending on the type of connection.
	 * 
	 * The container is determined as follows, based on connection type:
	 *   SequenceFlow - added to the list of FlowElements in a FlowElementsContainer,
	 *                  e.g. Process, SubProcess, etc.
	 *   Association - added to the list of Artifacts in a FlowElementsContainer
	 *   MessageFlow - added to the list of MessageFlows in a Collaboration
	 *   ConversationLink - added to the list of ConversationLinks in a Collaboration
	 *   
	 * This method does NOT handle DataAssociations which, technically appear like connections,
	 * but are really input/output mappings of an Activity's I/O Parameters. These are handled
	 * as a special case in
	 * @link org.eclipse.bpmn2.modeler.ui.features.flow.DataAssociationFeatureContainer#createBusinessObject()
	 * 
	 * Note that this method recursively walks up the source object's containment hierarchy
	 * as it tries to find the correct container for the connection.
	 * 
	 * @param connection - the connection to be added to a container element
	 * @param source - the source object of the connection
	 * @param target - the target object of the connection
	 * @return true if the connection was added to a container
	 */
	private boolean addConnectionToContainer(CONNECTION connection, SOURCE source, TARGET target) {
		EObject sourceContainer = source.eContainer();
		EObject targetContainer = target.eContainer();
		if (connection instanceof SequenceFlow) {
			if (source instanceof Participant) {
				if (((Participant)source).getProcessRef()!=null) {
					((Participant)source).getProcessRef().getFlowElements().add((SequenceFlow)connection);
					return true;
				}
			}
			else if (sourceContainer instanceof FlowElementsContainer && sourceContainer == targetContainer) {
				((FlowElementsContainer)sourceContainer).getFlowElements().add((SequenceFlow)connection);
				return true;
			}
			else if (source instanceof FlowElementsContainer) {
				((FlowElementsContainer)source).getFlowElements().add((SequenceFlow)connection);
				return true;
			}
		}
		else if (connection instanceof Association) {
			if (addAssociationToContainer(connection, source))
				return true;
			if (addAssociationToContainer(connection, target))
				return true;
		}
		else if (connection instanceof MessageFlow) {
			if (source instanceof Activity) {
				EObject container = source.eContainer();
				while (container!=null) {
					if (container instanceof Process) {
						source = (SOURCE) container;
						break;
					}
					container = container.eContainer();
				}
			}
			if (source instanceof Process) {
				// find the Collaboration that owns this Process
				Definitions definitions = ModelUtil.getDefinitions(source);
				for (Collaboration c : ModelUtil.getAllRootElements(definitions, Collaboration.class)) {
					for (Participant p : c.getParticipants()) {
						if (p.getProcessRef() == source) {
							source = (SOURCE)c;
							break;
						}
					}
				}
			}
			if (source instanceof Collaboration) {
				if (!((Collaboration)source).getMessageFlows().contains(connection))
					((Collaboration)source).getMessageFlows().add((MessageFlow)connection);
				return true;
			}
			else if (source instanceof ChoreographyTask) {
				if (!((ChoreographyTask)source).getMessageFlowRef().contains(connection))
					((ChoreographyTask)source).getMessageFlowRef().add((MessageFlow)connection);
			}
			else if (source instanceof ConversationNode) {
				if (!((ConversationNode)source).getMessageFlowRefs().contains(connection))
					((ConversationNode)source).getMessageFlowRefs().add((MessageFlow)connection);
			}
		}
		else if (connection instanceof ConversationLink) {
			if (source instanceof Process) {
				// find the Collaboration that owns this Process
				Definitions definitions = ModelUtil.getDefinitions(source);
				for (Collaboration c : ModelUtil.getAllRootElements(definitions, Collaboration.class)) {
					for (Participant p : c.getParticipants()) {
						if (p.getProcessRef() == source) {
							source = (SOURCE)c;
							break;
						}
					}
				}
			}
			if (source instanceof Collaboration) {
				((Collaboration)source).getConversationLinks().add((ConversationLink)connection);
				return true;
			}
			if (source instanceof Conversation && target instanceof Participant) {
				((Conversation)source).getParticipantRefs().add((Participant)target);
			}
			if (target instanceof Conversation && source instanceof Participant) {
				((Conversation)target).getParticipantRefs().add((Participant)source);
			}
		}

		if (source.eContainer() instanceof EObject) {
			return addConnectionToContainer(connection, (SOURCE)source.eContainer(), (TARGET)target.eContainer());
		}
		
		return false;
	}
	
	private boolean addAssociationToContainer(CONNECTION connection, EObject endpoint) {
		EObject container = endpoint.eContainer();
		if (endpoint instanceof Process) {
			((Process)endpoint).getArtifacts().add((Association)connection);
			return true;
		}
		else if (endpoint instanceof SubProcess) {
			while (container!=null) {
				if (container instanceof Process) {
					((Process)container).getArtifacts().add((Association)connection);
					return true;
				}
				if (container instanceof Collaboration) {
					((Collaboration)container).getArtifacts().add((Association)connection);
					return true;
				}
				container = container.eContainer();
			}
			((SubProcess)endpoint).getArtifacts().add((Association)connection);
			return true;
		}
		else if (endpoint instanceof SubChoreography) {
			while (container!=null) {
				if (container instanceof Process) {
					((Process)container).getArtifacts().add((Association)connection);
					return true;
				}
				if (container instanceof Collaboration) {
					((Collaboration)container).getArtifacts().add((Association)connection);
					return true;
				}
				container = container.eContainer();
			}
			((SubChoreography)endpoint).getArtifacts().add((Association)connection);
			return true;
		}
		else if (endpoint instanceof Collaboration) {
			((Collaboration)endpoint).getArtifacts().add((Association)connection);
			return true;
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	public static ICreateConnectionFeature getCreateFeature(IFeatureProvider fp, ICreateConnectionContext context, Object businessObject) {
		for (ICreateConnectionFeature cf : fp.getCreateConnectionFeatures()) {
			if (cf instanceof AbstractCreateFlowFeature) {
				AbstractCreateFlowFeature acf = (AbstractCreateFlowFeature) cf;
				if (acf.getBusinessObjectClass().isInstance(businessObject)) {
					return acf;
				}
			}
		}
		return null;
	}
}