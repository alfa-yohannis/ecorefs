package org.eclipse.bpmn2.modeler.ui.features.activity.subprocess;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.Lane;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.dd.di.DiagramElement;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

public abstract class AbstractPushPullFeature extends AbstractCustomFeature {

	public final static int MARGIN = 20;

	protected String description;
	protected ContainerShape containerShape;
	protected FlowElementsContainer businessObject;
	protected Diagram oldDiagram;
	protected Diagram newDiagram;
	protected BPMNDiagram oldBpmnDiagram;
	protected BPMNDiagram newBpmnDiagram;
	protected BPMNShape bpmnShape;
	protected Rectangle boundingRectangle = null;
	protected List<DiagramElement> diagramElements = new ArrayList<DiagramElement>();
	protected List<Shape> childShapes = new ArrayList<Shape>();
	protected List<Connection> internalConnections = new ArrayList<Connection>();
	protected Hashtable<Connection, SourceTarget> externalConnections = new Hashtable<Connection, SourceTarget>();
	
	public static class SourceTarget {
		public AnchorContainer localShape;
		public AnchorContainer source;
		public AnchorContainer target;
		
		public SourceTarget(AnchorContainer source, AnchorContainer target, AnchorContainer localShape) {
			this.source = source;
			this.target = target;
			this.localShape = localShape;
		}
	}

	public AbstractPushPullFeature(IFeatureProvider fp) {
		super(fp);
	}

	protected abstract void collectDiagramElements(FlowElementsContainer businessObject, BPMNDiagram source);
	protected abstract void collectShapes(ContainerShape source);
	protected abstract void moveGraphitiData(Diagram source, Diagram target);
	protected abstract Rectangle calculateBoundingRectangle(ContainerShape containerShape, List<Shape> childShapes);

	protected void moveDiagramElements(BPMNDiagram source, BPMNDiagram target) {
		for (DiagramElement sourceDe : diagramElements) {
			BaseElement be = null;
			if (sourceDe instanceof BPMNEdge) {
				be = ((BPMNEdge)sourceDe).getBpmnElement();
			}
			else if (sourceDe instanceof BPMNShape) {
				be = ((BPMNShape)sourceDe).getBpmnElement();
			}
			
			boolean moveIt = true;
			// is there an element on the target diagram that has the same business object?
			for (DiagramElement targetDe : target.getPlane().getPlaneElement()) {
				if (sourceDe instanceof BPMNEdge && targetDe instanceof BPMNEdge) {
					if (be == ((BPMNEdge)targetDe).getBpmnElement()) {
						moveIt = false;
					}
				}
				else if (sourceDe instanceof BPMNShape && targetDe instanceof BPMNShape) {
					if (be == ((BPMNShape)targetDe).getBpmnElement()) {
						moveIt = false;
					}
				}
			}
			if (moveIt)
				target.getPlane().getPlaneElement().add(sourceDe);
		}
	}

	protected void moveShapes(ContainerShape source, ContainerShape target, int xOffset, int yOffset) {
		for (Shape s : childShapes) {
			if (s instanceof ContainerShape && FeatureSupport.hasBPMNShape((ContainerShape)s)) {
				ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(s);
				int x = loc.getX() - xOffset;
				int y = loc.getY() - yOffset;
				MoveShapeContext moveContext = new MoveShapeContext(s);
				moveContext.setSourceContainer(source);
				moveContext.setTargetContainer(target);
				moveContext.setLocation(x, y);
				
				IMoveShapeFeature moveFeature = getFeatureProvider().getMoveShapeFeature(moveContext);
				moveFeature.moveShape(moveContext);
				layoutIfNecessary((ContainerShape)s);
			}
			else {
				target.getChildren().add(s);
			}
		}
	}
	
	protected boolean isReferenceShape(Diagram diagram, PictogramElement pe) {
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(pe);
		if (be instanceof Participant && FeatureSupport.isParticipantReference(diagram, (Participant)be))
			return true;
		return false;
	}
	
	protected AnchorContainer getExternalContainer(ContainerShape source, AnchorContainer ac) {
		// the external container can only be a Participant or a Diagram
		EObject parent = ac;
		while (parent!=null && !(parent instanceof Diagram)) {
			if (parent==source) {
				// the shape is a descendant of the container being pushed down
				// so it is considered not external to the new diagram.
				return null;
			}
			if (parent instanceof ContainerShape) {
				BaseElement be = BusinessObjectUtil.getFirstBaseElement((PictogramElement)parent);
				if (be instanceof Participant) // || be instanceof FlowElementsContainer)
					return (AnchorContainer) parent;
//				else if (be instanceof Artifact && parent.eContainer() instanceof ContainerShape) 
//					return (AnchorContainer) parent.eContainer();
			}
			parent = parent.eContainer();
		}
		if (parent instanceof Diagram) {
//			if (parent==Graphiti.getPeService().getDiagramForPictogramElement(source)) {
//				// the shape is on the diagram being pulled up so it
//				// is considered not external to the old diagram.
//				return null;
//			}
			return (AnchorContainer) parent;
		}
		return null;
	}

	protected boolean isDescendant(AnchorContainer ac, PictogramElement shape) {
		// if this is a Lane, the real container should be its enclosing Diagram
		// or Pool shape.
		while (FeatureSupport.isLane(ac)) {
			if (ac.eContainer() instanceof AnchorContainer) {
				ac = (AnchorContainer)ac.eContainer();
			}
		}
		if (shape instanceof ConnectionDecorator) {
			shape = ((ConnectionDecorator) shape).getConnection();
		}
		if (shape instanceof Connection) {
			Connection c = (Connection) shape;
			return isDescendant(ac, c.getStart().getParent()) &&
					isDescendant(ac, c.getEnd().getParent());
		}
		Diagram diagram = Graphiti.getPeService().getDiagramForPictogramElement(ac);
		while (shape!=null) {
			if (isReferenceShape(diagram,shape))
				return false;
			if (shape==ac)
				return true;
			if (shape.eContainer() instanceof ContainerShape)
				shape = (ContainerShape) shape.eContainer();
			else
				break;
		}
		return shape==ac;
	}
	
	protected boolean isExternalConnection(AnchorContainer ac, Connection c) {
		return !isDescendant(ac, c.getStart().getParent()) ||
				!isDescendant(ac, c.getEnd().getParent());
	}

	// "External" is from the point of view of the diagram containing the internals
	// of a pushed container: any connection to/from a shape that is a reference
	// to a business object on another diagram is considered "external".
	protected void collectExternalConnections(ContainerShape source, Connection c) {
		if (!externalConnections.containsKey(c)) {
			AnchorContainer ac;
			AnchorContainer startShape = null;
			AnchorContainer endShape = null;
			AnchorContainer startContainer = null;
			AnchorContainer endContainer = null;
	
			startShape = c.getStart().getParent();
			endShape = c.getEnd().getParent();
			if (isDescendant(source, startShape)) {
				if (isDescendant(source, endShape))
					return;
				endContainer = getExternalContainer(source, endShape);
				externalConnections.put(c, new SourceTarget(startShape,endContainer,startShape));
			}
			else if (isDescendant(source, endShape)) {
				if (isDescendant(source, startShape))
					return;
				startContainer = getExternalContainer(source, startShape);
				externalConnections.put(c, new SourceTarget(startContainer,endShape,endShape));
			}
		}
	}

	protected void collectConnections(ContainerShape source) {
		// Follow all external connections of ancestor shapes, keeping track
		// of the external shape that is connected to the local shape.
		for (Shape s : source.getChildren()) {
			if (s instanceof ContainerShape) {
				for ( Connection c : FeatureSupport.getConnections(s)) {
					if (isExternalConnection(source, c)) {
						collectExternalConnections(source, c);
					}
					else {
						if (!internalConnections.contains(c))
							internalConnections.add(c);
					}
					for (Connection c2 : FeatureSupport.getConnections(c)) {
						if (isExternalConnection(source, c2)) {
							collectExternalConnections(source, c2);
						}
						else {
							if (!internalConnections.contains(c2))
								internalConnections.add(c2);
						}
					}
				}
				
				BaseElement be = BusinessObjectUtil.getFirstBaseElement(s);
				if (be instanceof FlowElementsContainer) {
					collectConnections((ContainerShape)s);
				}
				else if (be instanceof Lane) {
					collectConnections((ContainerShape)s);
				}
			}
		}
	}

	protected void moveConnections(ContainerShape source, ContainerShape target) {
		Diagram targetDiagram = Graphiti.getPeService().getDiagramForShape(target);
		targetDiagram.getConnections().addAll(internalConnections);
	}
	
	protected ContainerShape findReferencedShape(AnchorContainer refShape, Diagram diagram) {
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(refShape);
		for (Object o : Graphiti.getPeService().getLinkedPictogramElements(new EObject[] {be}, diagram)) {
			if (o instanceof ContainerShape && FeatureSupport.hasBPMNShape((ContainerShape)o)) {
				// this must be the one
				return (ContainerShape)o;
			}
		}
		return null;
	}
	
	protected Connection findReferencedConnection(Connection refConnection, Diagram diagram) {
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(refConnection);
		for (Connection c : diagram.getConnections()) {
			if (BusinessObjectUtil.getFirstBaseElement(c)==be) {
				return c;
			}
		}
		return null;
	}
	
	protected Point getChildOffset(ContainerShape targetContainerShape) {
		return GraphicsUtil.createPoint(0, 0);
	}

	protected void layoutIfNecessary(ContainerShape shape) {
		FlowElementsContainer fec = BusinessObjectUtil.getFirstElementOfType(shape, FlowElementsContainer.class);
		if (fec!=null) {
			BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(shape);
			if (bpmnDiagram!=null) {
				BPMNShape bpmnShape = DIUtils.findBPMNShape(bpmnDiagram, fec);
				if (bpmnShape!=null) {
					LayoutContext layoutContext = new LayoutContext(shape);
					ILayoutFeature layoutFeature = getFeatureProvider().getLayoutFeature(layoutContext);
					layoutFeature.layout(layoutContext);
				}
			}
		}
	}
}
