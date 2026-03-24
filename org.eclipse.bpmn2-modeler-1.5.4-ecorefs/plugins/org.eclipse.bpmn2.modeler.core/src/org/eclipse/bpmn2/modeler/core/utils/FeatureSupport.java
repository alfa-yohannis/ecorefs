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
package org.eclipse.bpmn2.modeler.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpmn2.AdHocSubProcess;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.CallChoreography;
import org.eclipse.bpmn2.CallableElement;
import org.eclipse.bpmn2.CategoryValue;
import org.eclipse.bpmn2.Choreography;
import org.eclipse.bpmn2.ChoreographyActivity;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.CorrelationPropertyRetrievalExpression;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Group;
import org.eclipse.bpmn2.ImplicitThrowEvent;
import org.eclipse.bpmn2.IntermediateCatchEvent;
import org.eclipse.bpmn2.IntermediateThrowEvent;
import org.eclipse.bpmn2.Lane;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.Operation;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.ReceiveTask;
import org.eclipse.bpmn2.SendTask;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.SubChoreography;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.Transaction;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.di.ParticipantBandKind;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.AbstractConnectionRouter;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.MultiUpdateFeature;
import org.eclipse.bpmn2.modeler.core.features.choreography.ChoreographyUtil;
import org.eclipse.bpmn2.modeler.core.features.label.UpdateLabelFeature;
import org.eclipse.bpmn2.modeler.core.model.ModelHandler;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle.LabelPosition;
import org.eclipse.dd.dc.Bounds;
import org.eclipse.dd.di.DiagramElement;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.ITargetContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.context.impl.ResizeShapeContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.mm.MmFactory;
import org.eclipse.graphiti.mm.Property;
import org.eclipse.graphiti.mm.PropertyContainer;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeService;

/**
 * This is a hodgepodge of static methods used by various Graphiti Features.
 */
public class FeatureSupport {

	public static boolean isValidFlowElementTarget(ITargetContext context) {
		boolean intoDiagram = context.getTargetContainer() instanceof Diagram;
		boolean intoLane = isTargetLane(context) && isTargetLaneOnTop(context);
		boolean intoParticipant = isTargetParticipant(context);
		boolean intoFlowElementContainer = isTargetFlowElementsContainer(context);
		boolean intoGroup = isTargetGroup(context);
		return (intoDiagram || intoLane || intoParticipant || intoFlowElementContainer) && !intoGroup;
	}
	
	public static boolean isValidArtifactTarget(ITargetContext context) {
		boolean intoParticipant = isTargetParticipant(context);
		Participant participant = null;
		if (intoParticipant) {
			participant = FeatureSupport.getTargetParticipant(context);
			if (FeatureSupport.hasBpmnDiagram(participant)) {
				return false;
			}
		}
		boolean intoDiagram = context.getTargetContainer() instanceof Diagram;
		if (intoDiagram) {
			Diagram diagram = Graphiti.getPeService().getDiagramForShape(context.getTargetContainer());
			if (FeatureSupport.isParticipantReference(diagram, participant)) {
				return false;
			}
		}
		boolean intoLane = isTargetLane(context) && isTargetLaneOnTop(context);
		boolean intoSubProcess = isTargetSubProcess(context);
		boolean intoSubChoreography = isTargetSubChoreography(context);
		boolean intoGroup = isTargetGroup(context);
		return (intoDiagram || intoLane || intoParticipant || intoSubProcess || intoSubChoreography) && !intoGroup;
	}

	public static boolean isChoreographyParticipantBand(PictogramElement element) {
		if (element!=null) {
			EObject container = element.eContainer();
			if (container instanceof PictogramElement) {
				PictogramElement containerElem = (PictogramElement) container;
				Object bo = Graphiti.getLinkService().getBusinessObjectForLinkedPictogramElement(containerElem);
				if (bo instanceof ChoreographyActivity) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isValidDataTarget(ITargetContext context) {
		boolean intoParticipant = isTargetParticipant(context);
		Participant participant = null;
		if (intoParticipant) {
			participant = FeatureSupport.getTargetParticipant(context);
			if (FeatureSupport.hasBpmnDiagram(participant)) {
				return false;
			}
		}
		boolean intoDiagram = context.getTargetContainer() instanceof Diagram;
		if (intoDiagram) {
			Diagram diagram = Graphiti.getPeService().getDiagramForShape(context.getTargetContainer());
			if (FeatureSupport.isParticipantReference(diagram, participant)) {
				return false;
			}
		}
		Object containerBO = BusinessObjectUtil.getBusinessObjectForPictogramElement( context.getTargetContainer() );
		boolean intoSubProcess = containerBO instanceof FlowElementsContainer;
		if (intoSubProcess || intoDiagram || intoParticipant)
			return true;
		if (isTargetLane(context) && isTargetLaneOnTop(context))
			return true;
		return false;
	}
	
	public static boolean isTargetSubProcess(ITargetContext context) {
		return BusinessObjectUtil.containsElementOfType(context.getTargetContainer(), SubProcess.class);
	}
	
	public static boolean isTargetSubChoreography(ITargetContext context) {
		return BusinessObjectUtil.containsElementOfType(context.getTargetContainer(), SubChoreography.class);
	}

	public static boolean isTargetLane(ITargetContext context) {
		return isLane(context.getTargetContainer());
	}

	public static boolean isLane(PictogramElement element) {
		return element instanceof ContainerShape &&
				BusinessObjectUtil.containsElementOfType(element, Lane.class);
	}

	public static Lane getTargetLane(ITargetContext context) {
		PictogramElement element = context.getTargetContainer();
		return BusinessObjectUtil.getFirstElementOfType(element, Lane.class);
	}

	public static boolean isTargetGroup(ITargetContext context) {
		Group group = BusinessObjectUtil.getFirstElementOfType(context.getTargetContainer(), Group.class);
		return group != null;
	}
	
	public static boolean isTargetParticipant(ITargetContext context) {
		return isParticipant(context.getTargetContainer());
	}

	public static boolean isParticipant(PictogramElement element) {
		return element instanceof ContainerShape &&
				BusinessObjectUtil.containsElementOfType(element, Participant.class) &&
				!isChoreographyParticipantBand(element);
	}

	public static Participant getTargetParticipant(ITargetContext context) {
		PictogramElement element = context.getTargetContainer();
		return BusinessObjectUtil.getFirstElementOfType(element, Participant.class);
	}

	public static SubProcess getTargetSubProcess(ITargetContext context) {
		PictogramElement element = context.getTargetContainer();
		return BusinessObjectUtil.getFirstElementOfType(element, SubProcess.class);
	}

	public static boolean isTargetFlowElementsContainer(ITargetContext context) {
		return BusinessObjectUtil.containsElementOfType(context.getTargetContainer(),
				FlowElementsContainer.class);
	}

	public static FlowElementsContainer getTargetFlowElementsContainer(ITargetContext context) {
		PictogramElement element = context.getTargetContainer();
		return BusinessObjectUtil.getFirstElementOfType(element, FlowElementsContainer.class);
	}
	
	public static boolean isLaneOnTop(Lane lane) {
		return lane.getChildLaneSet() == null || lane.getChildLaneSet().getLanes().isEmpty();
	}

	public static boolean isTargetLaneOnTop(ITargetContext context) {
		Lane lane = BusinessObjectUtil.getFirstElementOfType(context.getTargetContainer(), Lane.class);
		return lane.getChildLaneSet() == null || lane.getChildLaneSet().getLanes().isEmpty();
	}
	
	public static EObject getTargetObject(ITargetContext context) {
		ContainerShape targetContainer = context.getTargetContainer();
		EObject targetObject = BusinessObjectUtil.getBusinessObjectForPictogramElement(targetContainer);
		if (targetObject instanceof BPMNDiagram) {
			targetObject = ((BPMNDiagram)targetObject).getPlane().getBpmnElement();
		}
		if (targetObject instanceof Lane) {
			while (targetObject!=null) {
				targetObject = targetObject.eContainer();
				if (targetObject instanceof FlowElementsContainer)
					break;
			}
		}
		if (targetObject instanceof Collaboration && !(targetObject instanceof Choreography)) {
			Collaboration collaboration = (Collaboration)targetObject;
			for (Participant p : collaboration.getParticipants()) {
				if (p.getProcessRef()!=null) {
					targetObject = p.getProcessRef();
					break;
				}
			}

		}
		if (targetObject instanceof Participant) {
			targetObject = ((Participant)targetObject).getProcessRef();
		}
		return targetObject;
	}

	public static boolean isHorizontal(ContainerShape container) {
		EObject parent = container.eContainer();
		if (parent instanceof PictogramElement) {
			// participant bands are always "vertical" so that
			// the label is drawn horizontally by the LayoutFeature
			if (BusinessObjectUtil.getFirstElementOfType((PictogramElement)parent, ChoreographyActivity.class) != null)
				return false;
		}

		String v = getPropertyValue(container, GraphitiConstants.IS_HORIZONTAL_PROPERTY);
		if (v==null) {
			BPMNShape bpmnShape = DIUtils.findBPMNShape(BusinessObjectUtil.getFirstBaseElement(container));
			if (bpmnShape!=null)
				return bpmnShape.isIsHorizontal();
			return Bpmn2Preferences.getInstance(container).isHorizontalDefault();
		}
		return Boolean.parseBoolean(v);
	}
	
	public static void setHorizontal(ContainerShape container, boolean isHorizontal) {
		setPropertyValue(container, GraphitiConstants.IS_HORIZONTAL_PROPERTY, Boolean.toString(isHorizontal));
		BPMNShape bs = BusinessObjectUtil.getFirstElementOfType(container, BPMNShape.class);
		if (bs!=null)
			bs.setIsHorizontal(isHorizontal);
	}
	
	public static boolean isHorizontal(IContext context) {
		Object v = context.getProperty(GraphitiConstants.IS_HORIZONTAL_PROPERTY);
		if (v==null) {
			Bpmn2Preferences preferences = Bpmn2Preferences.getInstance();
			return preferences.isHorizontalDefault();
		}
		return (Boolean)v;
	}
	
	public static void setHorizontal(IContext context, boolean isHorizontal) {
		context.putProperty(GraphitiConstants.IS_HORIZONTAL_PROPERTY, isHorizontal);
	}
	
	/**
	 * Checks if the given PictogramElement is a ContainerShape that is linked
	 * to both a BaseElement and a BPMNShape object. In other words, the PE
	 * represents an visual for a BPMN2 node element such as a Task, Gateway,
	 * Event, etc.
	 * 
	 * @param pe the PictogramElement
	 * @return true if the PE is a BPMN2 node element.
	 */
	public static boolean isBpmnShape(PictogramElement pe) {
		return pe instanceof ContainerShape &&
				BusinessObjectUtil.getFirstBaseElement(pe) != null &&
				BusinessObjectUtil.getFirstElementOfType(pe, BPMNShape.class) !=null;
	}
	
	public static List<PictogramElement> getContainerChildren(ContainerShape container) {
		List<PictogramElement> list = new ArrayList<PictogramElement>();
		for (PictogramElement pe : container.getChildren()) {
			if (ShapeDecoratorUtil.isActivityBorder(pe))
				continue;
			if (ShapeDecoratorUtil.isValidationDecorator(pe))
				continue;
			if (isLabelShape(pe))
				continue;
			list.add(pe);
		}
		return list;
	}

	public static List<PictogramElement> getContainerDecorators(ContainerShape container) {
		List<PictogramElement> list = new ArrayList<PictogramElement>();
		for (PictogramElement pe : container.getChildren()) {
			if (ShapeDecoratorUtil.isActivityBorder(pe))
				list.add(pe);
		}
		return list;
	}
	
	public static void setContainerChildrenVisible(IFeatureProvider fp, ContainerShape container, boolean visible) {
		List<PictogramElement> list = new ArrayList<PictogramElement>();
		list.addAll(container.getChildren());
		for (PictogramElement pe : list) {
			if (ShapeDecoratorUtil.isActivityBorder(pe))
				continue;
			
			if (ShapeDecoratorUtil.isEventSubProcessDecorator(pe)) {
				pe.setVisible(!visible);
			}
			else
				pe.setVisible(visible);

			if (visible)
				updateLabel(fp, pe, null);
			if (pe instanceof AnchorContainer) {
				AnchorContainer ac = (AnchorContainer)pe;
				for (Anchor a : ac.getAnchors()) {
					for (Connection c : a.getOutgoingConnections()) {
						c.setVisible(visible);
						if (visible)
							updateLabel(fp, c, null);
						for (ConnectionDecorator decorator : c.getConnectionDecorators()) {
							decorator.setVisible(visible);
						}
					}
				}
			}
		}
	}

	public static ContainerShape getRootContainer(ContainerShape container) {
		ContainerShape parent = container.getContainer();
		EObject bo = BusinessObjectUtil.getFirstElementOfType(parent, BaseElement.class);
		if (bo != null && (bo instanceof Lane || bo instanceof Participant)) {
			return getRootContainer(parent);
		}
		return container;
	}

	public static Participant getTargetParticipant(ITargetContext context, ModelHandler handler) {
		if (context.getTargetContainer() instanceof Diagram) {
			return handler.getInternalParticipant();
		}

		Object bo = BusinessObjectUtil.getFirstElementOfType(context.getTargetContainer(), BaseElement.class);

		if (bo instanceof Participant) {
			return (Participant) bo;
		}

		return handler.getParticipant(bo);
	}

	public static Shape getShape(ContainerShape container, String property, String expectedValue) {
		IPeService peService = Graphiti.getPeService();
		Iterator<Shape> iterator = peService.getAllContainedShapes(container).iterator();
		while (iterator.hasNext()) {
			Shape shape = iterator.next();
			String value = getPropertyValue(shape, property);
			if (value != null && value.equals(expectedValue)) {
				return shape;
			}
		}
		return null;
	}

	public static ContainerShape getFirstLaneInContainer(ContainerShape root) {
		List<PictogramElement> laneShapes = BusinessObjectUtil.getChildElementsOfType(root, Lane.class);
		if (!laneShapes.isEmpty()) {
			Iterator<PictogramElement> iterator = laneShapes.iterator();
			PictogramElement result = iterator.next();
			if (result instanceof ContainerShape) {
				GraphicsAlgorithm ga = result.getGraphicsAlgorithm();
				if (isHorizontal(root)) {
					while (iterator.hasNext()) {
						PictogramElement currentShape = iterator.next();
						if (currentShape instanceof ContainerShape) {
							if (currentShape.getGraphicsAlgorithm().getY() < ga.getY()) {
								result = currentShape;
							}
						}
					}
				} else {
					while (iterator.hasNext()) {
						PictogramElement currentShape = iterator.next();
						if (currentShape instanceof ContainerShape) {
							if (currentShape.getGraphicsAlgorithm().getX() < ga.getX()) {
								result = currentShape;
							}
						}
					}				
				}
				return (ContainerShape) result;
			}
		}
		return root;
	}
	
	public static ContainerShape getLastLaneInContainer(ContainerShape root) {
		List<PictogramElement> laneShapes = BusinessObjectUtil.getChildElementsOfType(root, Lane.class);
		if (!laneShapes.isEmpty()) {
			Iterator<PictogramElement> iterator = laneShapes.iterator();
			PictogramElement result = iterator.next();
			if (result instanceof ContainerShape) {
				GraphicsAlgorithm ga = result.getGraphicsAlgorithm();
				if (isHorizontal(root)) {
					while (iterator.hasNext()) {
						PictogramElement currentShape = iterator.next();
						if (currentShape instanceof ContainerShape) {
							if (currentShape.getGraphicsAlgorithm().getY() > ga.getY()) {
								result = currentShape;
							}
						}
					}
				} else {
					while (iterator.hasNext()) {
						PictogramElement currentShape = iterator.next();
						if (currentShape instanceof ContainerShape) {
							if (currentShape.getGraphicsAlgorithm().getX() > ga.getX()) {
								result = currentShape;
							}
						}
					}				
				}
				return (ContainerShape) result;
			}
		}
		return root;
	}
	
	public static List<PictogramElement> getPoolOrLaneChildren(ContainerShape containerShape) {
		List<PictogramElement> children = new ArrayList<PictogramElement>();
		for (PictogramElement pe : containerShape.getChildren()) {
			BaseElement be = BusinessObjectUtil.getFirstElementOfType(pe, BaseElement.class);
			if (pe instanceof ContainerShape && !isLane(pe) && be!=null)
				children.add(pe);
		}
		return children;
	}

	/**
	 * Returns a list of all Shapes and any Connections attached to them, that
	 * are children or descendants of the given Lane or Pool container. Only
	 * Shapes that are NOT Lanes are returned.
	 * 
	 * @param containerShape the Lane or Pool container shape.
	 */
	public static List<PictogramElement> getPoolAndLaneDescendants(ContainerShape containerShape) {
		List<PictogramElement> children = new ArrayList<PictogramElement>();
		collectChildren(containerShape, children, false);
		return children;
	}

	/**
	 * Collects all Shapes and any Connections attached to them, that are
	 * children or descendants of the given Lane or Pool container. Only Shapes
	 * that are NOT Lanes are collected.
	 * 
	 * @param containerShape the current Pool or Lane shape. This method is
	 *            recursive and is initially invoked for the root container.
	 * @param descendants the list of descendant Shapes and attached Connections
	 * @param includeLanes if true, includes all Lane shapes in the results
	 *            list
	 */
	public static void collectChildren(ContainerShape containerShape, List<PictogramElement> descendants, boolean includeLanes) {
		for (PictogramElement pe : containerShape.getChildren()) {
			if (pe instanceof ContainerShape) {
				if (isLane(pe)) {
					if (includeLanes)
						descendants.add(pe);
					collectChildren((ContainerShape) pe, descendants, includeLanes);
				}
				else {
					if (isBpmnShape(pe)) {
						descendants.add(pe);
						for (Anchor a :((ContainerShape) pe).getAnchors()) {
							for (Connection c : a.getIncomingConnections()) {
								if (c instanceof FreeFormConnection && !descendants.contains(c)) {
									descendants.add(c);
								}
							}
							for (Connection c : a.getOutgoingConnections()) {
								if (c instanceof FreeFormConnection && !descendants.contains(c)) {
									descendants.add(c);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Check if the given BaseElement has 
	 * @param baseElement
	 * @return
	 */
	public static boolean hasBpmnDiagram(BaseElement baseElement) {
		BaseElement process = null;
		if (baseElement instanceof Participant) {
			process = ((Participant)baseElement).getProcessRef();
		}
		else if (baseElement instanceof CallActivity) {
			CallableElement ce = ((CallActivity)baseElement).getCalledElementRef();
			if (ce instanceof Process)
				process = (Process)ce;
		}
		
		if (process!=null) {
			baseElement = process;
		}
		
		try {
			Definitions definitions = ModelUtil.getDefinitions(baseElement);
			for (BPMNDiagram d : definitions.getDiagrams()) {
				if (d.getPlane().getBpmnElement() == baseElement)
					return true;
			}
		}
		catch (Exception e){
		}
		
		return false;
	}
	
	public static boolean isParticipantReference(Diagram diagram, Participant participant) {
		if (participant!=null) {
			// the shape is a Pool, find its containing Graphiti Diagram
			// and from that, get the BPMNDiagram
			BPMNDiagram bpmnDiagram = (BPMNDiagram) BusinessObjectUtil.getBusinessObjectForPictogramElement(diagram);
			if (bpmnDiagram==null)
				return false;
			// if the BPMNDiagram that contains this Participant is NOT a
			// Collaboration (the only type that can contain a Participant) then
			// this must be a Participant reference
			BaseElement be = bpmnDiagram.getPlane().getBpmnElement();
			if (!(be instanceof Collaboration))
				return true;
		}
		return false;
	}

	public static List<ContainerShape> findGroupedShapes(ContainerShape groupShape) {
		Diagram diagram = null;
		EObject parent = groupShape.eContainer();
		while (parent!=null) {
			if (parent instanceof Diagram) {
				diagram = (Diagram)parent;
				break;
			}
			parent = parent.eContainer();
		}
	
		// find all shapes that are inside this Group
		// these will be moved along with the Group
		List<ContainerShape> list = new ArrayList<ContainerShape>();
		if (diagram!=null && isGroupShape(groupShape)) {
			TreeIterator<EObject> iter = diagram.eAllContents();
			while (iter.hasNext()) {
				EObject child = iter.next();
				if (child instanceof ContainerShape
						&& child!=groupShape
						&& !list.contains(child)
						&& !isLabelShape((ContainerShape)child)) {
					ContainerShape shape = (ContainerShape)child;
					if (isGroupShape(shape)) {
						if (GraphicsUtil.contains(groupShape, shape)) {
							if (!list.contains(shape)) {
								list.add(shape);
							}
						}
					}
					else if (GraphicsUtil.contains(groupShape, shape)) {
						if (!list.contains(shape)) {
							list.add(shape);
						}
						// find this shape's parent ContainerShape if it has one
						while (!(shape.getContainer() instanceof Diagram)) {
							shape = shape.getContainer();
						}
						if (!list.contains(shape) && shape!=groupShape) {
							list.add(shape);
						}
					}
				}
			}
		}
		return list;
	}

	public static boolean isGroupShape(PictogramElement shape) {
		return shape instanceof ContainerShape && BusinessObjectUtil.getFirstBaseElement(shape) instanceof Group;
	}

	public static List<EObject> findMessageReferences(Diagram diagram, Message message) {
		List<EObject> result = new ArrayList<EObject>();
		Definitions definitions = ModelUtil.getDefinitions(message);
		TreeIterator<EObject> iter = definitions.eAllContents();
		while (iter.hasNext()) {
			EObject o = iter.next();
			if (o instanceof MessageFlow) {
				if (((MessageFlow)o).getMessageRef() == message) {
					result.add(o);
				}
			}
			if (o instanceof MessageEventDefinition) {
				if (((MessageEventDefinition)o).getMessageRef() == message) {
					result.add(o);
				}
			}
			if (o instanceof Operation) {
				if (((Operation)o).getInMessageRef() == message ||
						((Operation)o).getOutMessageRef() == message) {
					result.add(o);
				}
			}
			if (o instanceof ReceiveTask) {
				if (((ReceiveTask)o).getMessageRef() == message) {
					result.add(o);
				}
			}
			if (o instanceof SendTask) {
				if (((SendTask)o).getMessageRef() == message) {
					result.add(o);
				}
			}
			if (o instanceof CorrelationPropertyRetrievalExpression) {
				if (((CorrelationPropertyRetrievalExpression)o).getMessageRef() == message) {
					result.add(o);
				}
			}
		}

		if (diagram!=null) {
			iter = diagram.eResource().getAllContents();
			while (iter.hasNext()) {
				EObject o = iter.next();
				if (o instanceof ContainerShape && !isLabelShape((ContainerShape)o)) {
					if (BusinessObjectUtil.getFirstBaseElement((ContainerShape)o) == message)
						result.add(o);
				}
			}
		}
		return result;
	}
	
	public static List<EClass> getAllowedEventDefinitions(Event event, Object parentContainer) {
		BaseElement eventOwner = null;
		if (parentContainer instanceof BaseElement)
			eventOwner = (BaseElement) parentContainer;
		
		if (event instanceof BoundaryEvent && ((BoundaryEvent)event).getAttachedToRef()!=null) {
			eventOwner = ((BoundaryEvent)event).getAttachedToRef();
		}
		else {
			EObject parent = event.eContainer();
			while (parent!=null) {
				if (parent instanceof FlowElementsContainer ) {
					eventOwner = (BaseElement)parent;
					break;
				}
				parent = parent.eContainer();
			}
		}
		
		List<EClass> allowedItems = new ArrayList<EClass>();
		if (event instanceof BoundaryEvent) {
			if (eventOwner instanceof Transaction) {
//				if (((BoundaryEvent)event).isCancelActivity())
					allowedItems.add(Bpmn2Package.eINSTANCE.getCancelEventDefinition());
			}
//			if (((BoundaryEvent)event).isCancelActivity())
				allowedItems.add(Bpmn2Package.eINSTANCE.getCompensateEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getConditionalEventDefinition());
//			if (((BoundaryEvent)event).isCancelActivity())
				allowedItems.add(Bpmn2Package.eINSTANCE.getErrorEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getEscalationEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getMessageEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getSignalEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getTimerEventDefinition());
		}
		else if (event instanceof IntermediateCatchEvent) {
			allowedItems.add(Bpmn2Package.eINSTANCE.getConditionalEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getLinkEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getMessageEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getSignalEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getTimerEventDefinition());
		}
		else if (event instanceof StartEvent) {
			if (eventOwner instanceof SubProcess) {
//				if (((StartEvent)event).isIsInterrupting()) {
					allowedItems.add(Bpmn2Package.eINSTANCE.getCompensateEventDefinition());
					allowedItems.add(Bpmn2Package.eINSTANCE.getErrorEventDefinition());
//				}
				allowedItems.add(Bpmn2Package.eINSTANCE.getEscalationEventDefinition());
			}
			allowedItems.add(Bpmn2Package.eINSTANCE.getConditionalEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getMessageEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getSignalEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getTimerEventDefinition());
		}
		else if (event instanceof EndEvent) {
			if (eventOwner instanceof Transaction)
				allowedItems.add(Bpmn2Package.eINSTANCE.getCancelEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getCompensateEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getErrorEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getEscalationEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getMessageEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getSignalEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getTerminateEventDefinition());
		}
		else if (event instanceof ImplicitThrowEvent) {
			allowedItems.add(Bpmn2Package.eINSTANCE.getCompensateEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getEscalationEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getLinkEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getMessageEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getSignalEventDefinition());
		}
		else if (event instanceof IntermediateThrowEvent) {
			allowedItems.add(Bpmn2Package.eINSTANCE.getCompensateEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getEscalationEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getLinkEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getMessageEventDefinition());
			allowedItems.add(Bpmn2Package.eINSTANCE.getSignalEventDefinition());
		}
		return allowedItems;
	}

	public static boolean updateConnection(IFeatureProvider fp, Connection connection) {
		boolean layoutChanged = false;

		LayoutContext layoutContext = new LayoutContext(connection);
		ILayoutFeature layoutFeature = fp.getLayoutFeature(layoutContext);
		if (layoutFeature!=null) {
			layoutFeature.layout(layoutContext);
			layoutChanged = layoutFeature.hasDoneChanges();
		}
		
		boolean updateChanged = false;
		UpdateContext updateContext = new UpdateContext(connection);
		IUpdateFeature updateFeature = fp.getUpdateFeature(updateContext);
		if (updateFeature!=null && updateFeature.updateNeeded(updateContext).toBoolean()) {
			updateFeature.update(updateContext);
			updateChanged = updateFeature.hasDoneChanges();
		}

		if (layoutChanged)
			updateLabel(fp, connection, null);
		
		// also update any Connections that are connected to this Connection
		
		for (Shape shape : connection.getConnectionDecorators()) {
			updateConnections(fp, shape);
		}
		return layoutChanged || updateChanged;
	}
	
	public static List<Connection> getConnections(AnchorContainer ac) {
		List<Connection> connections = new ArrayList<Connection>();
		if (ac instanceof Connection) {
			Connection c = (Connection)ac;
			for (ConnectionDecorator cd : c.getConnectionDecorators()) {
				for (Anchor a : cd.getAnchors()) {
					connections.addAll(a.getIncomingConnections());
					connections.addAll(a.getOutgoingConnections());
				}
			}
		}
		else {
			for (Anchor a : ac.getAnchors()) {
				connections.addAll(a.getIncomingConnections());
				connections.addAll(a.getOutgoingConnections());
			}
		}
		return connections;
	}
	
	public static List<Connection> getConnectionsRecursive(AnchorContainer ac) {
		List<Connection> connections = new ArrayList<Connection>();
		TreeIterator<EObject> iter = ac.eAllContents();
		while (iter.hasNext()) {
			EObject o = iter.next();
			if (o instanceof AnchorContainer) {
				for (Anchor a : ((AnchorContainer)o).getAnchors()) {
					connections.addAll(a.getIncomingConnections());
					connections.addAll(a.getOutgoingConnections());
				}
			}
		}
		return connections;
	}

	public static void updateConnections(IFeatureProvider fp, List<Connection> connections, boolean force) {
		if (force) {
			for (Connection c : connections) {
				if (c instanceof FreeFormConnection) {
					FreeFormConnection ffc = (FreeFormConnection)c;
					ffc.getBendpoints().clear();
				}
			}
		}
		for (Connection c : connections) {
			updateConnection(fp,c,force);
		}
	}

	public static boolean updateConnection(IFeatureProvider fp, Connection connection, boolean force) {
		AbstractConnectionRouter.setForceRouting(connection, force);
		if (force) {
			if (connection instanceof FreeFormConnection) {
				FreeFormConnection ffc = (FreeFormConnection)connection;
				ffc.getBendpoints().clear();
			}
		}
		return updateConnection(fp,connection);
	}

	public static void updateConnections(IFeatureProvider fp, AnchorContainer ac, List<Connection> alreadyUpdated) {
		for (int ai=0; ai<ac.getAnchors().size(); ++ai) {
			Anchor a = ac.getAnchors().get(ai);
			for (int ci=0; ci<a.getIncomingConnections().size(); ++ci) {
				Connection c = a.getIncomingConnections().get(ci);
				if (c instanceof FreeFormConnection) {
					if (!alreadyUpdated.contains(c)) {
						updateConnection(fp, c, true);
						alreadyUpdated.add(c);
					}
				}
			}
		}
		
		for (int ai=0; ai<ac.getAnchors().size(); ++ai) {
			Anchor a = ac.getAnchors().get(ai);
			for (int ci=0; ci<a.getOutgoingConnections().size(); ++ci) {
				Connection c = a.getOutgoingConnections().get(ci);
				if (c instanceof FreeFormConnection) {
					if (!alreadyUpdated.contains(c)) {
						updateConnection(fp, c, true);
						alreadyUpdated.add(c);
					}
				}
			}
		}
	}

	public static void updateConnections(IFeatureProvider fp, AnchorContainer ac) {
		List<Connection> alreadyUpdated = new ArrayList<Connection>();
		updateConnections(fp, ac, alreadyUpdated);
	}
	
	public static void updateConnections(IFeatureProvider fp, List<? extends AnchorContainer> acs) {
		List<Connection> alreadyUpdated = new ArrayList<Connection>();
		for (AnchorContainer ac : acs)
			updateConnections(fp, ac, alreadyUpdated);
	}
	
	public static void updateCategoryValues(IFeatureProvider fp, List<ContainerShape> shapes) {
		// Update CategoryValues for SequenceFlows also
		List<Connection> connections = new ArrayList<Connection>();
		for (ContainerShape cs : shapes) {
			updateCategoryValues(fp, cs);

			for (Anchor a : cs.getAnchors()) {
				for (Connection c : a.getIncomingConnections()) {
					if (!connections.contains(c))
						connections.add(c);
				}
				for (Connection c : a.getOutgoingConnections()) {
					if (!connections.contains(c))
						connections.add(c);
				}
			}
		}
		for (Connection c : connections) {
			updateCategoryValues(fp, c);
		}
	}
	
	public static void updateCategoryValues(IFeatureProvider fp, PictogramElement pe) {
		
		Resource resource = ExtendedPropertiesAdapter.getResource(pe);
		if (Bpmn2Preferences.getInstance(resource).getPropagateGroupCategories()) {
			// only do this if User Preference is enabled: assign the Group's CategoryValue
			// to the FlowElement represented by the given PictogramElement
			Diagram diagram = fp.getDiagramTypeProvider().getDiagram();
			FlowElement flowElement = BusinessObjectUtil.getFirstElementOfType(pe, FlowElement.class);
			if (flowElement==null)
				return;
			// remove any previous Category Values from this FlowElement
			flowElement.getCategoryValueRef().clear();
			
			// find all Groups in this Resource and check if it contains the given FlowElement
			if (pe instanceof ContainerShape) {
				for (Group group : ModelUtil.getAllObjectsOfType(resource, Group.class)) {
					CategoryValue cv = group.getCategoryValueRef();
					if (cv==null)
						continue;
					
					for (PictogramElement groupShape : Graphiti.getLinkService().getPictogramElements(diagram, group)) {
						if (groupShape instanceof ContainerShape) {
							for (ContainerShape flowElementShape : findGroupedShapes((ContainerShape) groupShape)) {
								FlowElement fe = BusinessObjectUtil.getFirstElementOfType(flowElementShape, FlowElement.class);
								if (fe==flowElement) {
									fe.getCategoryValueRef().add(cv);
									break;
								}
							}
						}
					}
				}
			}
			else if (pe instanceof Connection && flowElement instanceof SequenceFlow) {
				SequenceFlow sf = (SequenceFlow) flowElement;
				FlowNode source = sf.getSourceRef();
				FlowNode target = sf.getTargetRef();
				
				sf.getCategoryValueRef().clear();
				sf.getCategoryValueRef().addAll(source.getCategoryValueRef());
				sf.getCategoryValueRef().addAll(target.getCategoryValueRef());
			}
		}
	}

	public static void setToolTip(GraphicsAlgorithm ga, String text) {
		if (ga!=null) {
			Property prop = MmFactory.eINSTANCE.createProperty();
			prop.setKey(GraphitiConstants.TOOLTIP_PROPERTY);
			prop.setValue(text);
			ga.getProperties().add(prop);
		}
	}
	
	public static String getToolTip(GraphicsAlgorithm ga) {
		if (ga!=null) {
			for (Property prop : ga.getProperties()) {
				if (GraphitiConstants.TOOLTIP_PROPERTY.equals(prop.getKey()))
					return prop.getValue();
			}
		}
		return null;
	}

	public static boolean hasBPMNShape(PictogramElement pe) {
		return BusinessObjectUtil.getFirstElementOfType(pe, BPMNShape.class) != null;
	}

	public static boolean hasBPMNEdge(PictogramElement pe) {
		return BusinessObjectUtil.getFirstElementOfType(pe, BPMNEdge.class) != null;
	}

	public static boolean hasBPMNElement(PictogramElement pe) {
		return hasBPMNShape(pe) || hasBPMNEdge(pe);
	}
	
	/*
	 * Label support methods
	 */
	public static PictogramElement getPictogramElement(IContext context) {
		if (context instanceof IPictogramElementContext) {
			return ((IPictogramElementContext) context).getPictogramElement();
		}
		else if (context instanceof ICustomContext) {
			PictogramElement[] pes = ((ICustomContext) context).getPictogramElements();
			if (pes.length==1)
				return pes[0];
		}
		return null;
	}
	
	/**
	 * Checks the given PictogramElement to see if it is a Label shape. Label
	 * shapes are identified by the LABEL_SHAPE property equal to "true".
	 * 
	 * @param shape the shape to test
	 * @return true if the shape is a Label
	 */
	public static boolean isLabelShape(PictogramElement shape) {
		if (shape==null)
			return false;
		return getPropertyValue(shape, GraphitiConstants.LABEL_SHAPE) != null;
	}

	public static boolean isHidden(PictogramElement pe) {
		if (getPropertyValue(pe, GraphitiConstants.IS_HIDDEN)!=null)
			return true;
		return false;
	}

	public static boolean setHidden(PictogramElement pe, boolean hidden) {
		if (hidden) {
			pe.eSetDeliver(false);
			setPropertyValue(pe, GraphitiConstants.IS_HIDDEN, Boolean.TRUE.toString());
			pe.setVisible(false);
			pe.eSetDeliver(true);
		}
		else {
			pe.eSetDeliver(false);
			Graphiti.getPeService().removeProperty(pe, GraphitiConstants.IS_HIDDEN);
			pe.setVisible(true);
			pe.eSetDeliver(true);
		}
		return false;
	}
	
	/**
	 * Gets the owner {@link PictogramElement} of a Label from a given IContext
	 * object. Label shapes are added by Feature Containers using a
	 * {@link org.eclipse.bpmn2.modeler.core.features.MultiAddFeature} - the
	 * first Add Feature in the Multi Add creates the graphical element that
	 * represents the shape or connection, and a subsequent Add Feature
	 * contributes the Label.
	 * <p>
	 * A Label and its owning PictogramElement are ultimately linked to each
	 * other by way of the PictogramElement link list; the owner has a link to
	 * the ContainerShape of the Label and the Label has a link to the owning
	 * shape or connection.
	 * 
	 * @param context a Graphiti context object
	 * @return the PictogramElement that owns a Label if it has one, or null.
	 */
	public static PictogramElement getLabelOwner(IContext context) {
		List<PictogramElement> pes = (List<PictogramElement>) context.getProperty(GraphitiConstants.PICTOGRAM_ELEMENTS);
		if (pes!=null && pes.size()>0) {
			return getLabelOwner(pes.get( pes.size()-1 )); 
		}
		PictogramElement pe = (PictogramElement) context.getProperty(GraphitiConstants.PICTOGRAM_ELEMENT);
		if (pe!=null)
			return pe;
		if (context instanceof IPictogramElementContext)
			return getLabelOwner(((IPictogramElementContext)context).getPictogramElement());
		return null;
	}

	/**
	 * Gets the owner {@link PictogramElement} of a Label from a given
	 * PictogramElement. The given PE may already be the owner of the Label, in
	 * which case the given value is returned. See also
	 * {@link FeatureSupport#getLabelOwner(IContext)}
	 * 
	 * @param pe a PictogramElement that represents the Label shape. This may be
	 *            either the Label shape or its owner.
	 * @return the PictogramElement that is the owner of a Label shape.
	 */
	public static PictogramElement getLabelOwner(PictogramElement pe) {
		DiagramElement de = BusinessObjectUtil.getFirstElementOfType(pe, DiagramElement.class);
		if (de!=null)
			return pe;
		ContainerShape cs = BusinessObjectUtil.getFirstElementOfType(pe, ContainerShape.class);
		de = BusinessObjectUtil.getFirstElementOfType(cs, DiagramElement.class);
		if (de!=null)
			return cs;
		
		// Messages attached to MessageFlows do not have a BPMNShape element, their
		// visibility is controlled by a setting on the BPMNEdge for the MessageFlow
		Message msg = BusinessObjectUtil.getFirstElementOfType(cs, Message.class);
		if (msg!=null)
			return cs;
		// If this is the ContainerShape of the MessageFlow Message, then it is the label owner.
		if (pe instanceof ContainerShape) {
			Shape labelShape = BusinessObjectUtil.getFirstElementOfType(pe, Shape.class);
			if (isLabelShape(labelShape))
				return pe;
		}

		Connection c = BusinessObjectUtil.getFirstElementOfType(pe, Connection.class);
		de = BusinessObjectUtil.getFirstElementOfType(c, DiagramElement.class);
		if (de!=null)
			return c;
		return null;
	}

	/**
	 * Gets the owner {@link PictogramElement} of a Label from a given
	 * Text {@link GraphicsAlgorithm}.
	 * 
	 * @param text a GraphicsAlgorithm that contains the Label text.
	 * @return the PictogramElement that is the owner of the Label shape.
	 */
	public static PictogramElement getLabelOwner(GraphicsAlgorithm text) {
		PictogramElement pe = text.getPictogramElement();
		if (isLabelShape(pe))
			return getLabelOwner(pe);
		return null;
	}

	/**
	 * Gets the Label shape for the given PictogramElement. This method is the opposite of
	 * {@link FeatureSupport#getLabelOwner(PictogramElement)} .
	 * 
	 * @param pe a PictogramElement that represents the owner of the Label. This may be
	 *            either the owner of the Label, or the Label shape itself.
	 * @return the PictogramElement that represents the Label.
	 */
	public static Shape getLabelShape(PictogramElement pe) {
		pe = getLabelOwner(pe);
		if (pe instanceof Connection) {
			for (ConnectionDecorator d : ((Connection)pe).getConnectionDecorators()) {
				if (isLabelShape(d))
					return d;
			}
		}
		Shape cs = BusinessObjectUtil.getFirstElementOfType(pe, Shape.class);
		if (isLabelShape(cs) ) {
			return cs;
		}
		return null;
	}

	/**
	 * Updates the contents of, and relocates a Label according to User
	 * Preferences for the linked BPMN2 object.
	 * 
	 * @param fp the Feature Provider
	 * @param pe the PictogramElement that may be either the Label shape or its
	 *            owner.
	 * @param offset an optional X/Y offset if the owning shape was moved. This is
	 *            used to relocate Labels that are "movable", that is they can
	 *            be manually positioned independently of their owners.
	 */
	public static void updateLabel(IFeatureProvider fp, PictogramElement pe, Point offset) {
		if (isLabelShape(pe))
			pe = getLabelOwner(pe);
		if (pe!=null) {
			UpdateContext context = new UpdateContext(pe);
			// Offset is only used if the label is MOVABLE - we need to keep the label's
			// relative position to its owning shape the same.
			context.putProperty(GraphitiConstants.LABEL_OFFSET, offset);
			IUpdateFeature feature = fp.getUpdateFeature(context);
			if (feature instanceof MultiUpdateFeature) {
				MultiUpdateFeature mf = (MultiUpdateFeature) feature;
				for (IUpdateFeature uf : mf.getFeatures())
					if (uf instanceof UpdateLabelFeature) {
						uf.update(context);
					}
			}
			else if (feature!=null) {
				feature.update(context);
			}
		}
	}
	
	/**
	 * Get the position of the label relative to its owning figure for the given
	 * BaseElement as defined in the User Preferences.
	 * 
	 * @param element the BaseElement that is represented by the graphical figure.
	 * @return a ShapeStyle LabelPosition relative location indicator. 
	 */
	public static LabelPosition getLabelPosition(PictogramElement pe) {
		BaseElement element = BusinessObjectUtil.getFirstBaseElement(pe);
		if (element!=null) {
			ShapeStyle ss = ShapeStyle.getShapeStyle(element);
			return ss.getLabelPosition();
		}
		return LabelPosition.BOTTOM;
	}
	
	/*
	 * Choreography support methods
	 */

	public static Tuple<List<ContainerShape>, List<ContainerShape>> getTopAndBottomBands(List<ContainerShape> bandShapes) {
		List<ContainerShape> top = new ArrayList<ContainerShape>();
		List<ContainerShape> bottom = new ArrayList<ContainerShape>();
	
		if (bandShapes.size() == 1) {
			BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(bandShapes.get(0), BPMNShape.class);
			ParticipantBandKind bandKind = bpmnShape.getParticipantBandKind();
			if (bandKind == ParticipantBandKind.TOP_INITIATING || bandKind == ParticipantBandKind.TOP_NON_INITIATING) {
				top.add(bandShapes.get(0));
			} else if (bandKind == ParticipantBandKind.BOTTOM_INITIATING
					|| bandKind == ParticipantBandKind.BOTTOM_NON_INITIATING) {
				bottom.add(bandShapes.get(0));
			} else {
				top.add(bandShapes.get(0));
			}
		} else {
			Collections.sort(bandShapes, new Comparator<ContainerShape>() {
				@Override
				public int compare(ContainerShape c1, ContainerShape c2) {
					BPMNShape bpmnShape1 = BusinessObjectUtil.getFirstElementOfType(c1, BPMNShape.class);
					Bounds bounds1 = bpmnShape1.getBounds();
					BPMNShape bpmnShape2 = BusinessObjectUtil.getFirstElementOfType(c2, BPMNShape.class);
					Bounds bounds2 = bpmnShape2.getBounds();
					return new Float(bounds1.getY()).compareTo(new Float(bounds2.getY()));
				}
			});
			int n = bandShapes.size();
			int divider = n / 2;
			top.addAll(bandShapes.subList(0, divider));
			bottom.addAll(bandShapes.subList(divider, n));
		}
		return new Tuple<List<ContainerShape>, List<ContainerShape>>(top, bottom);
	}

	public static List<ContainerShape> getParticipantBandContainerShapes(ContainerShape containerShape) {
		IPeService peService = Graphiti.getPeService();
		List<ContainerShape> bandShapes = new ArrayList<ContainerShape>();
		Collection<Shape> shapes = peService.getAllContainedShapes(containerShape);
		for (Shape s : shapes) {
			String property = getPropertyValue(s, ChoreographyUtil.PARTICIPANT_BAND);
			if (new Boolean(property)) {
				bandShapes.add((ContainerShape) s);
			}
		}
		return bandShapes;
	}
	
	public static boolean isElementExpanded(PictogramElement pe) {
		String property = getPropertyValue(pe,GraphitiConstants.IS_EXPANDED);
		return Boolean.parseBoolean(property);
	}

	public static void setElementExpanded(PictogramElement pe, boolean isExpanded) {
		if (isExpanded)
			setPropertyValue(pe, GraphitiConstants.IS_EXPANDED, Boolean.TRUE.toString());
		else {
			setPropertyValue(pe, GraphitiConstants.IS_EXPANDED, null);
		}
	}

	public static void setElementExpanded(BaseElement be, boolean isExpanded) {
		if (isExpandableElement(be)) {
			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(be);
			// otherwise check the "isExpanded" state of the BPMNShape element.
			BPMNShape bpmnShape = DIUtils.findBPMNShape(be);
			if (bpmnShape!=null) {
				if (bpmnDiagram!=null) {
					// this container has its contents on another
					// diagram page, so always set it as "collapse"
					isExpanded = false;
				}
				bpmnShape.setIsExpanded(isExpanded);
			}
		}
	}
	
	public static boolean isElementExpanded(BaseElement be) {
		if (isExpandableElement(be)) {
			// if the BaseElement has its own BPMNDiagram page it should be considered
			// to be collapsed and should be represented as such.
			// TODO: this condition should be removed once we implement Link events as
			// "off page" connectors.
			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(be);
			// otherwise check the "isExpanded" state of the BPMNShape element.
			BPMNShape bpmnShape = DIUtils.findBPMNShape(be);
			if (bpmnShape!=null && bpmnShape.isIsExpanded() && bpmnDiagram==null)
				return true;
		}
		return false;
	}
	
	public static boolean isExpandableElement(PictogramElement pe) {
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(pe);
		return isExpandableElement(be);
	}
	
	public static boolean isExpandableElement(BaseElement be) {
		return be instanceof SubProcess
				|| be instanceof AdHocSubProcess
				|| be instanceof Transaction
				|| be instanceof CallActivity
				|| be instanceof CallChoreography;
	}
	
	public static void updateExpandedSize(ContainerShape shape) {
		IDimension size = GraphicsUtil.calculateSize(shape);
		setExpandedSize(shape, size);
	}
	
	public static void setExpandedSize(ContainerShape shape, IDimension size) {
		setExpandedSize(shape, size.getWidth(), size.getHeight());
	}
	
	public static void setExpandedSize(ContainerShape shape, int width, int height) {
		setPropertyValue(shape, GraphitiConstants.EXPANDED_SIZE, width+","+height); //$NON-NLS-1$
	}
	
	public static IDimension getExpandedSize(ContainerShape shape) {
		IDimension size = GraphicsUtil.calculateSize(shape);
		String property = getPropertyValue(shape, GraphitiConstants.EXPANDED_SIZE);
		if (property==null) {
			ResizeShapeContext resizeContext = new ResizeShapeContext(shape);
			ExpandableActivitySizeCalculator calculator = new ExpandableActivitySizeCalculator(resizeContext);
			size.setWidth(calculator.getWidth());
			size.setHeight(calculator.getHeight());
		}
		else {
			int index = property.indexOf(',');
			int w = Integer.parseInt(property.substring(0,index));
			int h = Integer.parseInt(property.substring(index+1));
			size.setWidth(w);
			size.setHeight(h);
		}
		return size;
	}
	
	public static void updateCollapsedSize(ContainerShape shape) {
		IDimension size = GraphicsUtil.calculateSize(shape);
		setCollapsedSize(shape, size);
	}
	
	public static void setCollapsedSize(ContainerShape shape, IDimension size) {
		setCollapsedSize(shape, size.getWidth(), size.getHeight());
	}
	
	public static void setCollapsedSize(ContainerShape shape, int width, int height) {
		setPropertyValue(shape, GraphitiConstants.COLLAPSED_SIZE, width+","+height); //$NON-NLS-1$
	}
	
	public static IDimension getCollapsedSize(ContainerShape shape) {
		IDimension size = GraphicsUtil.calculateSize(shape);
		String property = getPropertyValue(shape, GraphitiConstants.COLLAPSED_SIZE);
		if (property==null) {
			Bpmn2Preferences preferences = Bpmn2Preferences.getInstance(shape);
			ShapeStyle ss = preferences.getShapeStyle("TASK");
			size.setWidth(ss.getDefaultWidth());
			size.setHeight(ss.getDefaultHeight());
		}
		else {
			int index = property.indexOf(',');
			int w = Integer.parseInt(property.substring(0,index));
			int h = Integer.parseInt(property.substring(index+1));
			size.setWidth(w);
			size.setHeight(h);
		}
		return size;
	}
	
	public static String getPropertyValue(PropertyContainer propertyContainer, String key) {
		return Graphiti.getPeService().getPropertyValue(propertyContainer, key);
	}
	
	public static void setPropertyValue(PropertyContainer propertyContainer, String key, String value) {
		while (Graphiti.getPeService().getPropertyValue(propertyContainer, key)!=null)
			Graphiti.getPeService().removeProperty(propertyContainer, key);
		if (value!=null)
			Graphiti.getPeService().setPropertyValue(propertyContainer, key, value);
	}
	
	public static class ExpandableActivitySizeCalculator {
		
		public static int MARGIN = 20;
		public int deltaX;
		public int deltaY;
		int deltaWidth;
		int deltaHeight;
		int minWidth;
		int minHeight;
		ContainerShape containerShape;
		ResizeShapeContext context;
		ShapeStyle ss;
		
		public ExpandableActivitySizeCalculator(ResizeShapeContext context) {
			this.context = context;
			setShape((ContainerShape) context.getPictogramElement());
		}
		
		private void setShape(ContainerShape containerShape) {
			this.containerShape = containerShape;
			Bpmn2Preferences preferences = Bpmn2Preferences.getInstance(containerShape.eResource());
			ss = preferences.getShapeStyle(BusinessObjectUtil.getFirstBaseElement(containerShape));
			calculate();
		}
		
		private ILocation getLocationRelativeToContainer(ContainerShape parent, ContainerShape child) {
			ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(child);
			ContainerShape container = parent.getContainer();
			if (container instanceof ContainerShape && !(container instanceof Diagram)) {
				ILocation containerLoc = Graphiti.getPeService().getLocationRelativeToDiagram(container);
				loc.setX(loc.getX() - containerLoc.getX());
				loc.setY(loc.getY() - containerLoc.getY());
			}
			return loc;
		}
		
		private void calculate() {
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int minChildX = Integer.MAX_VALUE;
			int minChildY = Integer.MAX_VALUE;
			minWidth = 0;
			minHeight = 0;
			List<PictogramElement> containerChildren = getContainerChildren(containerShape);
			GraphicsAlgorithm ga;
	
			for (PictogramElement pe : containerChildren) {
				ga = pe.getGraphicsAlgorithm();
				if (ga!=null) {
					if (ga.getX() < minChildX)
						minChildX = ga.getX();
					if (ga.getY() < minChildY)
						minChildY = ga.getY();
					ILocation loc = getLocationRelativeToContainer(containerShape, (ContainerShape)pe);
					int x = loc.getX();
					int y = loc.getY();
					if (x < minX)
						minX = x;
					if (y < minY)
						minY = y;
				}
			}
			
			for (PictogramElement pe : containerChildren) {
				ga = pe.getGraphicsAlgorithm();
				if (ga!=null) {
					ILocation loc = getLocationRelativeToContainer(containerShape, (ContainerShape)pe);
					int w = loc.getX() - minX + ga.getWidth();
					int h = loc.getY() - minY + ga.getHeight();
					if (w > minWidth)
						minWidth = w;
					if (h > minHeight)
						minHeight = h;
				}
			}
			
			if (minWidth<=0)
				minWidth = ss.getDefaultWidth();
			if (minHeight<=0)
				minHeight = ss.getDefaultHeight();
			
			minX -= MARGIN;
			minY -= MARGIN;
			minWidth += 2*MARGIN;
			minHeight += 2*MARGIN;
			
			ga = containerShape.getGraphicsAlgorithm();
	
			if (context.getX()>minX) {
				int dx0 = context.getX() - ga.getX();
				context.setX(minX);
				int dx1 = context.getX() - ga.getX();
				context.setWidth(context.getWidth() - (dx1 - dx0));
			}
			if (context.getY()>minY) {
				int dy0 = context.getY() - ga.getY();
				context.setY(minY);
				int dy1 = context.getY() - ga.getY();
				context.setHeight(context.getHeight() - (dy1 - dy0));
			}
			
			if (context.getX() != ga.getX())
				deltaX = context.getX() - ga.getX();
			else if (context.getWidth()<minWidth + minChildX - MARGIN)
				context.setWidth(minWidth + minChildX - MARGIN);
			if (context.getY() != ga.getY())
				deltaY = context.getY() - ga.getY();
			else if (context.getHeight()<minHeight + minChildY - MARGIN)
				context.setHeight(minHeight + minChildY - MARGIN);
		}
		
		public int getWidth() {
			return minWidth;
		}
		
		public int getHeight() {
			return minHeight;
		}
	}


	/**
	 * Find all shapes in the diagram which can cause a collision. 
	 *
	 * @return the list
	 */
	public static List<ContainerShape> findAllShapes(ContainerShape container, ContainerShape[] ignoreList) {
		List<ContainerShape> allShapes = new ArrayList<ContainerShape>();
		
		allShapes = new ArrayList<ContainerShape>();
		
		// first find all ancestors of source and  target, and their siblings
		List<ContainerShape> ancestors = new ArrayList<ContainerShape>();
		List<ContainerShape> shapes = new ArrayList<ContainerShape>();
		
		TreeIterator<EObject> iter = container.eAllContents();
		while (iter.hasNext()) {
			EObject o = iter.next();
			if (o instanceof ContainerShape && !(o instanceof Diagram)) {
				// this is a potential collision shape
				ContainerShape shape = (ContainerShape)o;
				BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(shape, BPMNShape.class);
				if (bpmnShape==null) {
					// this shape does not have a visual,
					// so no collision is possible
					continue;
				}
				if (FeatureSupport.isGroupShape(shape) || FeatureSupport.isLabelShape(shape) ) {
					// ignore Groups and Labels
					continue;
				}

				boolean ignore = false;
				for (ContainerShape ignoreShape : ignoreList) {
					if (shape==ignoreShape) {
						ignore = true;
					}
					else {
						EObject ancestor = ignoreShape.eContainer();
						while (ancestor!=null) {
							if (shape==ancestor) {
								ancestors.add((ContainerShape)ancestor);
								ignore = true;
								break;
							}
							ancestor = ancestor.eContainer();
						}
						ancestor = shape.eContainer();
						for (ContainerShape is : ignoreList) {
							if (ancestor==is) {
								ignore = true;
								break;
							}
							ancestor = ancestor.eContainer();
						}
					}
				}
				
				if (!ignore && !shapes.contains(shape)) {
					shapes.add(shape);
				}
			}
		}
		
		for (ContainerShape shape : shapes) {
			// this is a potential collision shape
			if (!ancestors.contains(shape)) {
				boolean ignore = false;
				// only Lanes can be siblings to other Lanes within a container
				// which may be either another Lane or a Pool.
				BaseElement be = BusinessObjectUtil.getFirstBaseElement(shape);
				if (be instanceof Lane) {
					EObject ancestor = shape.eContainer();
					while (!(ancestor instanceof Diagram)) {
						if (ancestors.contains(ancestor)) {
							ignore = true;
							break;
						}
						ancestor = ancestor.eContainer();
					}
				}
				// Yet another hack for dealing with imported files.
				// Here we have a poorly laid out diagram such that
				// a Pool overlaps another Pool almost completely.
				// This is an indication that the tool used to create
				// the file is rendering Pools on separate pages, even
				// though there is only a single BPMNDiagram.
				// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=463205
				// for an example of this.
				if (be instanceof Lane || be instanceof Participant) {
					for (ContainerShape ancestor : ancestors) {
						if (GraphicsUtil.intersects(shape, ancestor)) {
							ignore = true;
							break;
						}
					}
				}				
				// check if any ancestors overlaps this Lane or Pool
				// this is a special case
				
				if (!ignore) {
					allShapes.add(shape);
				}
			}
		}

		GraphicsUtil.dump("All Shapes: ", allShapes); //$NON-NLS-1$
		return allShapes;
	}
}
