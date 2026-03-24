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
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.features.participant;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.LaneSet;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.BaseElementFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.CreateConnectionReferenceFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.MultiUpdateFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.LayoutContainerFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.UpdateContainerLabelFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.participant.AddParticipantFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.participant.CreateParticipantReferenceFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.participant.DirectEditParticipantFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.participant.ResizeParticipantFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.participant.UpdateParticipantFeature;
import org.eclipse.bpmn2.modeler.core.features.containers.participant.UpdateParticipantMultiplicityFeature;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.ui.features.activity.subprocess.PullupFeature;
import org.eclipse.bpmn2.modeler.ui.features.activity.subprocess.PushdownFeature;
import org.eclipse.bpmn2.modeler.ui.features.activity.subprocess.AbstractPushPullFeature.SourceTarget;
import org.eclipse.bpmn2.modeler.ui.features.choreography.AddChoreographyMessageFeature;
import org.eclipse.bpmn2.modeler.ui.features.choreography.BlackboxFeature;
import org.eclipse.bpmn2.modeler.ui.features.choreography.RemoveChoreographyMessageFeature;
import org.eclipse.bpmn2.modeler.ui.features.choreography.RemoveChoreographyParticipantFeature;
import org.eclipse.bpmn2.modeler.ui.features.choreography.ShowDiagramPageFeature;
import org.eclipse.bpmn2.modeler.ui.features.choreography.WhiteboxFeature;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.IRemoveFeature;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.context.IMoveContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.IResizeContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.context.impl.ReconnectionContext;
import org.eclipse.graphiti.features.context.impl.ResizeShapeContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

public class ParticipantFeatureContainer extends BaseElementFeatureContainer {

	@Override
	public Object getApplyObject(IContext context) {
		if (
				context instanceof IUpdateContext ||
				context instanceof ILayoutContext ||
				context instanceof IMoveContext ||
				context instanceof IResizeContext
				) {
			PictogramElement pe = ((IPictogramElementContext)context).getPictogramElement();
			if (FeatureSupport.isLabelShape(pe))
				pe = (PictogramElement) pe.eContainer();
			if (FeatureSupport.isChoreographyParticipantBand(pe))
				return null;
		}
		Object o = super.getApplyObject(context);
		
		return o;
	}

	@Override
	public boolean canApplyTo(Object o) {
		return super.canApplyTo(o) && o instanceof Participant;
	}

	@Override
	public ICreateFeature getCreateFeature(IFeatureProvider fp) {
		return new CreateParticipantFeature(fp);
	}

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return new AddParticipantFeature(fp);
	}

	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		MultiUpdateFeature multiUpdate = new MultiUpdateFeature(fp);
		multiUpdate.addFeature(new UpdateParticipantFeature(fp));
		multiUpdate.addFeature(new UpdateParticipantMultiplicityFeature(fp));
//		multiUpdate.addFeature(new UpdateChoreographyMessageLinkFeature(fp));
		multiUpdate.addFeature(new UpdateContainerLabelFeature(fp));
		return multiUpdate;
	}

	@Override
	public IDirectEditingFeature getDirectEditingFeature(IFeatureProvider fp) {
		return new DirectEditParticipantFeature(fp);
	}

	@Override
	public ILayoutFeature getLayoutFeature(IFeatureProvider fp) {
		return new LayoutContainerFeature(fp);
	}

	@Override
	public IMoveShapeFeature getMoveFeature(IFeatureProvider fp) {
		return new MoveParticipantFeature(fp);
	}

	@Override
	public IResizeShapeFeature getResizeFeature(IFeatureProvider fp) {
		return new ResizeParticipantFeature(fp);
	}

	@Override
	public IDeleteFeature getDeleteFeature(IFeatureProvider fp) {
		return new DeleteParticipantFeature(fp);
	}

	@Override
	public IRemoveFeature getRemoveFeature(IFeatureProvider fp) {
		return new RemoveChoreographyParticipantFeature(fp);
	}

	@Override
	public ICustomFeature[] getCustomFeatures(IFeatureProvider fp) {
		ICustomFeature[] superFeatures = super.getCustomFeatures(fp);
		ICustomFeature[] thisFeatures = new ICustomFeature[8 + superFeatures.length];
		thisFeatures[0] = new ShowDiagramPageFeature(fp);
		int i;
		for (i=0; i<superFeatures.length; ++i)
			thisFeatures[i+1] = superFeatures[i];
		thisFeatures[++i] = new AddChoreographyMessageFeature(fp);
		thisFeatures[++i] = new RemoveChoreographyMessageFeature(fp);
		thisFeatures[++i] = new RotatePoolFeature(fp);
		thisFeatures[++i] = new WhiteboxFeature(fp);
		thisFeatures[++i] = new BlackboxFeature(fp);
		thisFeatures[++i] = new ParticipantPushdownFeature(fp);
		thisFeatures[++i] = new ParticipantPullupFeature(fp);
		return thisFeatures;
	}
	
	public static class ParticipantPushdownFeature extends PushdownFeature {

		public ParticipantPushdownFeature(IFeatureProvider fp) {
			super(fp);
		}
		
		@Override
		public void execute(ICustomContext context) {

			// Push the contents of this Pool into a new diagram.
			super.execute(context);

			updateParticipant(getFeatureProvider(), containerShape);
		}
		
		@Override
		protected void moveConnections(ContainerShape source, ContainerShape target) {
			// disconnect external connections from the shapes that will be
			// pushed to the new diagram, and reconnect it to the soon to be
			// empty Pool shape on the original diagram.
			ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(containerShape);
			for (Entry<Connection, SourceTarget> e : externalConnections.entrySet()) {
				Connection c = e.getKey();
				SourceTarget st = e.getValue();
				if (st.localShape==c.getStart().getParent()) {
					// reconnect source to the Pool on the original diagram
					Anchor anchor = AnchorUtil.createAnchor(containerShape, GraphicsUtil.getShapeCenter(containerShape));
					ReconnectionContext rc = new ReconnectionContext(c, c.getStart(), anchor, loc);
					rc.setReconnectType(ReconnectionContext.RECONNECT_SOURCE);
					rc.setTargetPictogramElement(containerShape);
					IReconnectionFeature rf = getFeatureProvider().getReconnectionFeature(rc);
					rf.reconnect(rc);
				}
				else if (st.localShape==c.getEnd().getParent()) {
					// reconnect target
					Anchor anchor = AnchorUtil.createAnchor(containerShape, GraphicsUtil.getShapeCenter(containerShape));
					ReconnectionContext rc = new ReconnectionContext(c, c.getEnd(), anchor, loc);
					rc.setReconnectType(ReconnectionContext.RECONNECT_TARGET);
					rc.setTargetPictogramElement(containerShape);
					IReconnectionFeature rf = getFeatureProvider().getReconnectionFeature(rc);
					rf.reconnect(rc);
				}
				FeatureSupport.updateConnection(getFeatureProvider(), c, true);
			}

			super.moveConnections(source, target);
			
			// Create dummy Pools and Data Inputs/Outputs and reconnect all external connections
			boolean isHorz = bpmnShape.isIsHorizontal();
			int nextX = 0;
			int nextY = 0;
			if (isHorz) {
				nextX = boundingRectangle.x;
				nextY = boundingRectangle.y + boundingRectangle.height + 4*MARGIN;
			}
			else {
				nextX = boundingRectangle.x + boundingRectangle.width + 4*MARGIN;
				nextY = boundingRectangle.y;
			}
			
			Hashtable<AnchorContainer, AnchorContainer> externalContainers = new Hashtable<AnchorContainer, AnchorContainer>();
			
			for (Entry<Connection, SourceTarget> e : externalConnections.entrySet()) {
				Connection externalConnection = e.getKey();
				// so far we can only handle MessageFlows here
				if (!(BusinessObjectUtil.getFirstBaseElement(externalConnection) instanceof MessageFlow))
					continue;
				
				SourceTarget st = e.getValue();
				AnchorContainer externalContainer = null;
				AnchorContainer localShape = st.localShape;
				if (st.localShape==st.source) {
					externalContainer = st.target;
				}
				else if (st.localShape==st.target) {
					externalContainer = st.source;
				}
				AnchorContainer localContainer = externalContainers.get(externalContainer);
				if (localContainer==null) {
					// the local Pool or DataInput/Output does not exist yet, create it
					Participant participant = null;
					BPMNShape diagramElement = null;
					BaseElement be = BusinessObjectUtil.getFirstBaseElement(externalContainer);
					if (be instanceof Participant) {
						diagramElement = BusinessObjectUtil.getFirstElementOfType(externalContainer, BPMNShape.class);
						if (diagramElement!=null)
							participant = (Participant) be;
					}
					else {
						// external container must be a Diagram
						Diagram diagram = Graphiti.getPeService().getDiagramForPictogramElement(externalContainer);
						localContainer = externalContainers.get(diagram);
						if (localContainer==null) {
							BPMNDiagram bpmnDiagram = BusinessObjectUtil.getFirstElementOfType(diagram, BPMNDiagram.class);
							be = bpmnDiagram.getPlane().getBpmnElement();
							if (be instanceof Collaboration) {
								for (Participant p : ((Collaboration)be).getParticipants()) {
									if (!FeatureSupport.hasBpmnDiagram(p)) {
										// this is the "default pool" - it has no BPMNDiagram of its own
										// but rather shares the Collaboration BPMNDiagram
										participant = p;
										//diagramElement = bpmnDiagram;
										break;
									}
								}
							}
						}
					}
					if (participant!=null) {
						CustomContext customContext = new CustomContext(new PictogramElement[] {newDiagram});
						customContext.setX(nextX);
						customContext.setY(nextY);
						ICustomFeature f = new CreateParticipantReferenceFeature(getFeatureProvider(),diagramElement, participant);
						f.execute(customContext);
						Object o = customContext.getProperty(GraphitiConstants.PICTOGRAM_ELEMENT);
						if (o instanceof AnchorContainer) {
							localContainer = (AnchorContainer) o;
							// keep track of this in case more than one
							// connection is attached to this container
							externalContainers.put(externalContainer, localContainer);
							IDimension size = GraphicsUtil.calculateSize(localContainer);
							if (isHorz) {
								nextX += size.getWidth() + 2*MARGIN;
							}
							else {
								nextY += size.getHeight() + 2*MARGIN;
							}
						}

					}
				}
				
				if (localContainer!=null) {
					Connection localConnection = null;
					BaseElement be = BusinessObjectUtil.getFirstBaseElement(externalConnection);
					BPMNEdge bpmnEdge = DIUtils.findBPMNEdge(oldBpmnDiagram, be);
					CreateConnectionReferenceFeature ccf = new CreateConnectionReferenceFeature(
							getFeatureProvider(), bpmnEdge, be);
					AnchorContainer sourceShape = null;
					AnchorContainer targetShape = null;
					if (st.localShape==st.source) {
						// outgoing connection: connect the source to the local Shape
						// and target to the reference Pool shape.
						sourceShape = localShape;
						targetShape = localContainer;
						
					}					
					else {
						sourceShape = localContainer;
						targetShape = localShape;
					}					

					if (sourceShape!=null && targetShape!=null) {
						Anchor sourceAnchor = AnchorUtil.createAnchor(sourceShape, GraphicsUtil.getShapeCenter(sourceShape));
						Anchor targetAnchor = AnchorUtil.createAnchor(targetShape, GraphicsUtil.getShapeCenter(targetShape));
						CreateConnectionContext ccc = new CreateConnectionContext();
						ccc.setSourcePictogramElement(sourceShape);
						ccc.setTargetPictogramElement(targetShape);
						ccc.setSourceAnchor(sourceAnchor);
						ccc.setTargetAnchor(targetAnchor);

						localConnection = ccf.create(ccc);
						if (localConnection!=null) {
							internalConnections.add(localConnection);
						}
					}
				}
			}
		}
	}
	
	public static class ParticipantPullupFeature extends PullupFeature {

		public ParticipantPullupFeature(IFeatureProvider fp) {
			super(fp);
		}
		
		@Override
		public void execute(ICustomContext context) {

			super.execute(context);
			
			int laneCount = 0;
			for (LaneSet ls : businessObject.getLaneSets()) {
				laneCount += ls.getLanes().size();
			}
			if (laneCount==0) {
				// the Pool has no Lanes, only individual flow elements so
				// we need to resize the Pool to the bounding rectangle
				// of all the flow elements.
				IDimension dim = GraphicsUtil.calculateSize(containerShape);
				boolean resizeNeeded = false;
				if (dim.getWidth()<boundingRectangle.width) {
					resizeNeeded = true;
					dim.setWidth(boundingRectangle.width);
				}
				if (dim.getHeight()<boundingRectangle.height) {
					resizeNeeded = true;
					dim.setHeight(boundingRectangle.height);
				}
				if (resizeNeeded) {
					ResizeShapeContext resizeContext = new ResizeShapeContext(containerShape);
					resizeContext.setHeight(dim.getHeight());
					resizeContext.setWidth(dim.getWidth());
					ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(containerShape);
					resizeContext.setLocation(loc.getX(), loc.getY());
	
					IResizeShapeFeature resizeFeature = getFeatureProvider().getResizeShapeFeature(resizeContext);
					resizeFeature.resizeShape(resizeContext);
				}
			}
			
			updateParticipant(getFeatureProvider(), containerShape);
		}

		@Override
		protected Point getChildOffset(ContainerShape targetContainerShape) {
			boolean horz = FeatureSupport.isHorizontal(targetContainerShape);
			if (horz)
				return GraphicsUtil.createPoint(AddParticipantFeature.TITLE_HEIGHT, 0);
			return GraphicsUtil.createPoint(0, AddParticipantFeature.TITLE_HEIGHT);
		}

		@Override
		protected void collectShapes(ContainerShape source) {
			List <PictogramElement> deleted = new ArrayList<PictogramElement>();
			childShapes.addAll(source.getChildren());
			for (Shape s : childShapes) {
				if (isReferenceShape(oldDiagram, s)) {
					deleted.add(s);
					deleted.add(FeatureSupport.getLabelShape(s));
					continue;
				}
			}
			childShapes.removeAll(deleted);
		}
		
		@Override
		protected void moveConnections(ContainerShape source, ContainerShape target) {

			super.moveConnections(source, target);
			
			// handle reconnection of external connections
			ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(containerShape);
			for (Entry<Connection, SourceTarget> e : externalConnections.entrySet()) {
				Connection oldConnection = e.getKey();
				Connection newConnection = null;
				SourceTarget st = e.getValue();
				// search for a connection on the new diagram with the same
				// business object as the old connection
				BaseElement connectionBusinessObject = BusinessObjectUtil.getFirstBaseElement(oldConnection);
				newConnection = findReferencedConnection(oldConnection, newDiagram);

				AnchorContainer sourceShape = null;
				AnchorContainer targetShape = null;
				if (newConnection==null) {
					// this external connection on the old diagram does not
					// exist on the new diagram, so create it
					if (st.localShape==oldConnection.getStart().getParent()) {
						sourceShape = st.localShape;
						// find the target shape on the new diagram using the business object
						// of the target shape from the old diagram (a "reference" shape)
						targetShape = findReferencedShape(st.target, newDiagram);
					}
					else if (st.localShape==oldConnection.getEnd().getParent()) {
						sourceShape = findReferencedShape(st.source, newDiagram);
						targetShape = st.localShape;
					}
					
					if (sourceShape!=null && targetShape!=null) {
						Anchor sourceAnchor = AnchorUtil.createAnchor(sourceShape, GraphicsUtil.getShapeCenter(sourceShape));
						Anchor targetAnchor = AnchorUtil.createAnchor(targetShape, GraphicsUtil.getShapeCenter(targetShape));
						CreateConnectionContext ccc = new CreateConnectionContext();
						ccc.setSourcePictogramElement(sourceShape);
						ccc.setTargetPictogramElement(targetShape);
						ccc.setSourceAnchor(sourceAnchor);
						ccc.setTargetAnchor(targetAnchor);

						BPMNEdge bpmnEdge = DIUtils.findBPMNEdge(oldBpmnDiagram, connectionBusinessObject);
						CreateConnectionReferenceFeature ccf = new CreateConnectionReferenceFeature(
								getFeatureProvider(), bpmnEdge, connectionBusinessObject);
						newConnection = ccf.create(ccc);
					}
				}
				else {
					// the external connection on the old diagram has an
					// analog on the new diagram. Reconnect the one on the new diagram. 
					if (st.localShape==oldConnection.getStart().getParent()) {
						// reconnect source to the shape on the old diagram
						sourceShape = st.localShape;
						Anchor anchor = AnchorUtil.createAnchor(sourceShape, GraphicsUtil.getShapeCenter(sourceShape));
						ReconnectionContext rc = new ReconnectionContext(newConnection, oldConnection.getStart(), anchor, loc);
						rc.setReconnectType(ReconnectionContext.RECONNECT_SOURCE);
						rc.setTargetPictogramElement(sourceShape);
						IReconnectionFeature rf = getFeatureProvider().getReconnectionFeature(rc);
						rf.reconnect(rc);
					}
					else if (st.localShape==oldConnection.getEnd().getParent()) {
						// reconnect target
						targetShape = st.localShape;
						Anchor anchor = AnchorUtil.createAnchor(targetShape, GraphicsUtil.getShapeCenter(targetShape));
						ReconnectionContext rc = new ReconnectionContext(newConnection, oldConnection.getEnd(), anchor, loc);
						rc.setReconnectType(ReconnectionContext.RECONNECT_TARGET);
						rc.setTargetPictogramElement(targetShape);
						IReconnectionFeature rf = getFeatureProvider().getReconnectionFeature(rc);
						rf.reconnect(rc);
					}
				}
				if (newConnection!=null)
					FeatureSupport.updateConnection(getFeatureProvider(), newConnection, true);
			}
			
		}
	}
	
	protected static void updateParticipant(IFeatureProvider fp, ContainerShape containerShape) {
		UpdateContext updateContext = new UpdateContext(containerShape);
		updateContext.putProperty(GraphitiConstants.FORCE_UPDATE_ALL, Boolean.TRUE);
		IUpdateFeature updateFeature = fp.getUpdateFeature(updateContext);
		updateFeature.update(updateContext);
		
		LayoutContext layoutContext = new LayoutContext(containerShape);
		ILayoutFeature layoutFeature = fp.getLayoutFeature(layoutContext);
		layoutFeature.layout(layoutContext);
	}
}