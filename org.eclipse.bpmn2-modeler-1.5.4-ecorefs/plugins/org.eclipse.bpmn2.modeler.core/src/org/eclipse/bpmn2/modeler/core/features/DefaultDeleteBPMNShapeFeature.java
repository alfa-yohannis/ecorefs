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
package org.eclipse.bpmn2.modeler.core.features;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.impl.DeleteContext;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.features.DefaultDeleteFeature;

// TODO: Auto-generated Javadoc
/**
 * Default Graphiti {@code DeleteFeature} class for Shapes.
 * <p>
 */
public class DefaultDeleteBPMNShapeFeature extends DefaultDeleteFeature {

	/**
	 * Instantiates a new default delete bpmn shape feature.
	 *
	 * @param fp the fp
	 */
	public DefaultDeleteBPMNShapeFeature(IFeatureProvider fp) {
		super(fp);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.features.DefaultDeleteFeature#getUserDecision(org.eclipse.graphiti.features.context.IDeleteContext)
	 */
	@Override
	protected boolean getUserDecision(IDeleteContext context) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.features.DefaultDeleteFeature#canDelete(org.eclipse.graphiti.features.context.IDeleteContext)
	 */
	public boolean canDelete(IDeleteContext context) {
		// don't delete the Diagram!
		if (context.getPictogramElement() instanceof Diagram)
			return false;
		TargetRuntime rt = TargetRuntime.getRuntime(context.getPictogramElement());
		LifecycleEvent event = new LifecycleEvent(EventType.PICTOGRAMELEMENT_CAN_DELETE,
				getFeatureProvider(), context, context.getPictogramElement(), rt);
		LifecycleEvent.notify(event);
		return event.doit;
	}
	
	public void delete(IDeleteContext context) {
		PictogramElement pe = context.getPictogramElement();
		List<AnchorContainer> shapes = new ArrayList<AnchorContainer>();
		
		List<Connection> connections = new ArrayList<Connection>();
		if (pe instanceof Connection) {
			// The PE being deleted is a Connection:
			// if it has other connections connected to it,
			// delete those as well.
			for (Anchor a : AnchorUtil.getAnchors((Connection)pe)) {
				connections.addAll(a.getIncomingConnections());
				connections.addAll(a.getOutgoingConnections());
			}
			for  (Connection c : connections) {
				DeleteContext dc = new DeleteContext(c);
				IDeleteFeature f = getFeatureProvider().getDeleteFeature(dc);
				f.delete(dc);
			}
			
			Connection connection = (Connection) pe;
			AnchorContainer ac;
			ac = connection.getStart()==null ? null : connection.getStart().getParent();
			if (ac!=null)
				shapes.add(ac);
			ac = connection.getEnd()==null ? null : connection.getEnd().getParent();
			if (ac!=null)
				shapes.add(ac);
		}
		
		super.delete(context);

		// If the deleted PE was a Connection, update all of the remaining
		// Connections for the source and target shapes
//		connections.clear();
//		for (AnchorContainer shape : shapes) {
//			for (Anchor a : shape.getAnchors()) {
//				connections.addAll(a.getIncomingConnections());
//				connections.addAll(a.getOutgoingConnections());
//			}
//		}
//		for (Connection connection : connections)
//			FeatureSupport.updateConnection(getFeatureProvider(), connection);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.features.DefaultDeleteFeature#deleteBusinessObject(java.lang.Object)
	 */
	@Override
	protected void deleteBusinessObject(Object bo) {
		EStructuralFeature reference = ((EObject)bo).eClass().getEStructuralFeature("categoryValueRef"); //$NON-NLS-1$
		if (reference!=null) {
			Object v = ((EObject)bo).eGet(reference);
			if (v instanceof EList) {
				((EList)v).clear();
			}
		}

		List<PictogramElement> pictElements = Graphiti.getLinkService().getPictogramElements(getDiagram(), (EObject) bo);
		for (Iterator<PictogramElement> iterator = pictElements.iterator(); iterator.hasNext();) {
			PictogramElement pe = iterator.next();
			deletePeEnvironment(pe);
			Graphiti.getPeService().deletePictogramElement(pe);
		}
		TargetRuntime targetRuntime = TargetRuntime.getRuntime(getDiagramBehavior());
		LifecycleEvent.notify(new LifecycleEvent(EventType.BUSINESSOBJECT_DELETED, bo, targetRuntime));

		super.deleteBusinessObject(bo);
	}
	
	/**
	 * Delete pe environment.
	 *
	 * @param pictogramElement the pictogram element
	 */
	protected void deletePeEnvironment(PictogramElement pictogramElement){
		if (pictogramElement instanceof ContainerShape) {
			ContainerShape containerShape = (ContainerShape) pictogramElement;
			List<Connection> connections = new ArrayList<Connection>();
			for (Anchor anchor : containerShape.getAnchors()) {
				connections.addAll(anchor.getIncomingConnections());
				connections.addAll(anchor.getOutgoingConnections());
			}
			deleteConnections(getFeatureProvider(), connections);
			deleteContainer(getFeatureProvider(), containerShape);
		}
	}
	
	/**
	 * Delete container.
	 *
	 * @param fp the fp
	 * @param containerShape the container shape
	 */
	protected void deleteContainer(IFeatureProvider fp, ContainerShape containerShape) {
		TargetRuntime targetRuntime = TargetRuntime.getRuntime(getDiagramBehavior());

		Object[] children = containerShape.getChildren().toArray();
		for (Object shape : children) {
			if (shape instanceof ContainerShape) {
				DeleteContext context = new DeleteContext((PictogramElement) shape);
				LifecycleEvent.notify(new LifecycleEvent(EventType.PICTOGRAMELEMENT_DELETED, fp, context, shape, targetRuntime));

				fp.getDeleteFeature(context).delete(context);
			}
		}

		LifecycleEvent.notify(new LifecycleEvent(EventType.PICTOGRAMELEMENT_DELETED, fp, null, containerShape, targetRuntime));
	}

	/**
	 * Delete connections.
	 *
	 * @param fp the fp
	 * @param connections the connections
	 */
	protected void deleteConnections(IFeatureProvider fp, List<Connection> connections) {
		List<Connection> allConnections = new ArrayList<Connection>();
		allConnections.addAll(connections);
		for (Connection connection : allConnections) {
			IDeleteContext context = new DeleteContext(connection);

			TargetRuntime targetRuntime = TargetRuntime.getRuntime(getDiagramBehavior());
			LifecycleEvent.notify(new LifecycleEvent(EventType.PICTOGRAMELEMENT_DELETED, fp, context, connection, targetRuntime));
			fp.getDeleteFeature(context).delete(context);
		}
	}
	
}
