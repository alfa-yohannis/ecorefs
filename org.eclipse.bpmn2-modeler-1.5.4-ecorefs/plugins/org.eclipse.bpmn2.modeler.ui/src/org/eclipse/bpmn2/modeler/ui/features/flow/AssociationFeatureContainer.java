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
package org.eclipse.bpmn2.modeler.ui.features.flow;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Association;
import org.eclipse.bpmn2.AssociationDirection;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2UpdateFeature;
import org.eclipse.bpmn2.modeler.core.features.BaseElementConnectionFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.DefaultDeleteBPMNShapeFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractAddFlowFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractCreateFlowFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractReconnectFlowFeature;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IReconnectionContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.DeleteContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;

public class AssociationFeatureContainer extends BaseElementConnectionFeatureContainer {

	// the property used to store the current Association's direction;
	// the value can be one of the AssociationDirection enumerations (a null
	// or empty string is the same as "None")
	public static final String ASSOCIATION_DIRECTION = "association.direction"; //$NON-NLS-1$
	public static final String ARROWHEAD_DECORATOR = "arrowhead.decorator"; //$NON-NLS-1$
	
	@Override
	public boolean canApplyTo(Object o) {
		return super.canApplyTo(o) && o instanceof Association;
	}

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return new AddAssociationFeature(fp);
	}

	@Override
	public ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider fp) {
		return new CreateAssociationFeature(fp);
	}

	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		return new UpdateAssociationFeature(fp);
	}
	
	@Override
	public IReconnectionFeature getReconnectionFeature(IFeatureProvider fp) {
		return new ReconnectAssociationFeature(fp);
	}
	
	@Override
	public IDeleteFeature getDeleteFeature(IFeatureProvider fp) {
		return new DeleteAssociationFeature(fp);
	}
	
	private static void setAssociationDirection(Connection connection, Association businessObject) {
		IPeService peService = Graphiti.getPeService();
		IGaService gaService = Graphiti.getGaService();
		String newDirection = businessObject.getAssociationDirection().toString();
		if (newDirection==null || newDirection.isEmpty())
			newDirection = AssociationDirection.NONE.toString();
		String oldDirection = FeatureSupport.getPropertyValue(connection, ASSOCIATION_DIRECTION);
		if (oldDirection==null || oldDirection.isEmpty())
			oldDirection = AssociationDirection.NONE.toString();

		if (!oldDirection.equals(newDirection)) {
			ConnectionDecorator sourceDecorator = null;
			ConnectionDecorator targetDecorator = null;
			for (ConnectionDecorator d : connection.getConnectionDecorators()) {
				String s = FeatureSupport.getPropertyValue(d, ARROWHEAD_DECORATOR);
				if (s!=null) {
					if (s.equals("source")) //$NON-NLS-1$
						sourceDecorator = d;
					else if (s.equals("target")) //$NON-NLS-1$
						targetDecorator = d;
				}
			}
			
			boolean needSource = false;
			boolean needTarget = false;
			if (newDirection.equals(AssociationDirection.ONE.toString())) {
				needTarget = true;
			}
			else if (newDirection.equals(AssociationDirection.BOTH.toString())) {
				needSource = needTarget = true;
			}
			
			final int w = 7;
			final int l = 13;
			if (needSource) {
				if (sourceDecorator==null) {
					sourceDecorator = peService.createConnectionDecorator(connection, false, 0.0, true);
					Polyline arrowhead = gaService.createPolyline(sourceDecorator, new int[] { -l, w, 0, 0, -l, -w });
					StyleUtil.applyStyle(arrowhead, businessObject);
					FeatureSupport.setPropertyValue(sourceDecorator, ARROWHEAD_DECORATOR, "source"); //$NON-NLS-1$
				}
			}
			else {
				if (sourceDecorator!=null)
					connection.getConnectionDecorators().remove(sourceDecorator);				
			}
			if (needTarget) {
				if (targetDecorator==null) {
					targetDecorator = peService.createConnectionDecorator(connection, false, 1.0, true);
					Polyline arrowhead = gaService.createPolyline(targetDecorator, new int[] { -l, w, 0, 0, -l, -w });
					StyleUtil.applyStyle(arrowhead, businessObject);
					FeatureSupport.setPropertyValue(targetDecorator, ARROWHEAD_DECORATOR, "target"); //$NON-NLS-1$
				}
			}
			else {
				if (targetDecorator!=null)
					connection.getConnectionDecorators().remove(targetDecorator);				
			}
		
			// update the property value in the Connection PictogramElement
			FeatureSupport.setPropertyValue(connection, ASSOCIATION_DIRECTION, newDirection);
		}

	}
	
	private static void updateCompensationHandlers(IFeatureProvider fp, Association association,
			BaseElement oldSource, BaseElement oldTarget, 
			BaseElement newSource, BaseElement newTarget) {
		Diagram diagram = fp.getDiagramTypeProvider().getDiagram();
		boolean oldTargetHasOtherCompensationEvents = false;
		for (Connection c : diagram.getConnections()) {
			Association a = BusinessObjectUtil.getFirstElementOfType(c, Association.class);
			if (a!=null && a.getAssociationDirection()==AssociationDirection.ONE &&
				a!=association && a.getTargetRef()==oldTarget) {
				oldTargetHasOtherCompensationEvents = true;
				break;
			}
		}
		Association oldAssociation = null;
		for (Connection c : diagram.getConnections()) {
			Association a = BusinessObjectUtil.getFirstElementOfType(c, Association.class);
			if (a!=null && a.getAssociationDirection()==AssociationDirection.ONE &&
				a!=association && a.getSourceRef()==newSource) {
				oldAssociation = a;
				break;
			}
		}
	
		if (association.getAssociationDirection()==AssociationDirection.ONE) {
			if (oldSource instanceof BoundaryEvent && oldTarget instanceof Activity) {
				// set the Activity reference of the old Boundary Event null
				CompensateEventDefinition ced = getCompensateEvent(oldSource);
				if (ced!=null) {
					ced.setActivityRef(null);
					if (!oldTargetHasOtherCompensationEvents) {
						((Activity)oldTarget).setIsForCompensation(false);
						PictogramElement pe = fp.getPictogramElementForBusinessObject(oldTarget);
						IUpdateContext uc = new UpdateContext(pe);
						IUpdateFeature uf = fp.getUpdateFeature(uc);
						uf.update(uc);
					}
				}
			}
			if (newSource instanceof BoundaryEvent && newTarget instanceof Activity) {
				// Set the Activity reference of the new Boundary Event
				CompensateEventDefinition ced = getCompensateEvent(newSource);
				if (ced!=null) {
					oldTarget = ced.getActivityRef();
					ced.setActivityRef((Activity)newTarget);
					((Activity)newTarget).setIsForCompensation(true);
					PictogramElement pe = fp.getPictogramElementForBusinessObject(newTarget);
					IUpdateContext uc = new UpdateContext(pe);
					IUpdateFeature uf = fp.getUpdateFeature(uc);
					uf.update(uc);
					
					if ((oldSource!=newSource || oldTarget!=newTarget) && oldAssociation!=null) {
						pe = fp.getPictogramElementForBusinessObject(oldAssociation);
						IDeleteContext dc = new DeleteContext(pe);
						IDeleteFeature df = fp.getDeleteFeature(dc);
						df.delete(dc);
					}
				}
			}
		}
	}
	
	private static CompensateEventDefinition getCompensateEvent(BaseElement source) {
		if (source instanceof BoundaryEvent) {
			// find a Compensate Event Definition in the BoundaryEvent
			for (EventDefinition ed : ((BoundaryEvent)source).getEventDefinitions()) {
				if (ed instanceof CompensateEventDefinition) {
					return (CompensateEventDefinition) ed;
				}
			}
		}
		return null;
	}

	public class AddAssociationFeature extends AbstractAddFlowFeature<Association> {
		public AddAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		protected Polyline createConnectionLine(Connection connection) {
			Polyline connectionLine = super.createConnectionLine(connection);
			connectionLine.setLineWidth(2);
			connectionLine.setLineStyle(LineStyle.DOT);
			return connectionLine;
		}

		@Override
		protected void decorateConnection(IAddConnectionContext context, Connection connection, Association businessObject) {
			setAssociationDirection(connection, businessObject);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2AddFeature#getBusinessObjectType()
		 */
		@Override
		public Class getBusinessObjectType() {
			return Association.class;
		}
	}


	public class CreateAssociationFeature extends AbstractCreateFlowFeature<Association, BaseElement, BaseElement> {

		public CreateAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean isAvailable(IContext context) {
			if (!isModelObjectEnabled(Bpmn2Package.eINSTANCE.getAssociation()))
				return false;
			if (context instanceof ICreateConnectionContext) {
				BaseElement source = getSourceBo((ICreateConnectionContext) context);
				if (source instanceof Activity || source instanceof Gateway || source instanceof Event)
					return false;
			}
			return super.isAvailable(context);
		}

		@Override
		public boolean canCreate(ICreateConnectionContext context) {
			if (!super.canCreate(context))
				return false;
			
			BaseElement source = getSourceBo(context);
			BaseElement target = getTargetBo(context);
			if (source!=null && target!=null) {
				if (source instanceof BoundaryEvent && target instanceof Activity)
					return true;
				
				if (source instanceof BaseElement || target instanceof BaseElement)
					return true;
			}			
			return false;
		}

		@Override
		public Connection create(ICreateConnectionContext context) {
			Connection connection = super.create(context);
			Association association = getBusinessObject(context);
			if (association.getSourceRef() instanceof BoundaryEvent && association.getTargetRef() instanceof Activity) {
				association.setAssociationDirection(AssociationDirection.ONE);
			}
			return connection;
		}

		@Override
		protected String getStencilImageId() {
			return ImageProvider.IMG_16_ASSOCIATION;
		}

		@Override
		protected Class<BaseElement> getSourceClass() {
			return BaseElement.class;
		}

		@Override
		protected Class<BaseElement> getTargetClass() {
			return BaseElement.class;
		}

		@Override
		protected BaseElement getSourceBo(ICreateConnectionContext context) {
			Anchor anchor = getSourceAnchor(context);
			if (anchor != null && anchor.getParent() instanceof Shape) {
				Shape shape = (Shape) anchor.getParent();
				return BusinessObjectUtil.getFirstElementOfType(shape, getTargetClass());
			}
			else if (context.getSourcePictogramElement() instanceof Connection) {
				Connection connection = (Connection) context.getSourcePictogramElement();
				return BusinessObjectUtil.getFirstBaseElement(connection);
			}
			return null;
		}

		@Override
		protected BaseElement getTargetBo(ICreateConnectionContext context) {
			Anchor anchor = getTargetAnchor(context);
			if (anchor != null && anchor.getParent() instanceof Shape) {
				Shape shape = (Shape) anchor.getParent();
				return BusinessObjectUtil.getFirstElementOfType(shape, getTargetClass());
			}
			else if (context.getTargetPictogramElement() instanceof Connection) {
				Connection connection = (Connection) context.getTargetPictogramElement();
				return BusinessObjectUtil.getFirstBaseElement(connection);
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CreateConnectionFeature#getBusinessObjectClass()
		 */
		@Override
		public EClass getBusinessObjectClass() {
			return Bpmn2Package.eINSTANCE.getAssociation();
		}
	}
	
	public static class UpdateAssociationFeature extends AbstractBpmn2UpdateFeature {

		public UpdateAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean canUpdate(IUpdateContext context) {
			if (context.getPictogramElement() instanceof Connection) {
				return BusinessObjectUtil.getFirstElementOfType(
						context.getPictogramElement(), Association.class) != null;
			}
			return false;
		}

		@Override
		public IReason updateNeeded(IUpdateContext context) {
			if (canUpdate(context)) {
				Connection connection = (Connection) context.getPictogramElement();
				Association businessObject = BusinessObjectUtil.getFirstElementOfType(context.getPictogramElement(),
						Association.class);
				String newDirection = businessObject.getAssociationDirection().toString();
				if (newDirection==null || newDirection.isEmpty())
					newDirection = AssociationDirection.NONE.toString();
				String oldDirection = FeatureSupport.getPropertyValue(connection, ASSOCIATION_DIRECTION);
				if (oldDirection==null || oldDirection.isEmpty())
					oldDirection = AssociationDirection.NONE.toString();
	
				if (!oldDirection.equals(newDirection)) {
					return Reason.createTrueReason(Messages.AssociationFeatureContainer_Direction_Changed);
				}
			}
			return Reason.createFalseReason();
		}

		@Override
		public boolean update(IUpdateContext context) {
			Connection connection = (Connection) context.getPictogramElement();
			Association association = BusinessObjectUtil.getFirstElementOfType(context.getPictogramElement(),
					Association.class);
			setAssociationDirection(connection, association);
			BaseElement source = association.getSourceRef();
			BaseElement target = association.getTargetRef();
			updateCompensationHandlers(getFeatureProvider(), association, source, target, source, target);
			
			return true;
		}
	}
	public static class ReconnectAssociationFeature extends AbstractReconnectFlowFeature {

		BaseElement oldSource;
		BaseElement oldTarget;
		
		public ReconnectAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean canReconnect(IReconnectionContext context) {
			BaseElement targetElement = BusinessObjectUtil.getFirstElementOfType(context.getTargetPictogramElement(), BaseElement.class);
			if (targetElement instanceof Association)
				return false;
			PictogramElement targetPictogramElement = context.getTargetPictogramElement();
			if (targetPictogramElement instanceof FreeFormConnection) {
				// can't reconnect to the same Connection
				AnchorContainer ac = context.getConnection().getEnd().getParent();
				if (BusinessObjectUtil.getBusinessObjectForPictogramElement(ac) == targetElement)
					return false;
				return true;
			}
			return super.canReconnect(context);
		}

		@Override
		protected Class<? extends EObject> getTargetClass() {
			return BaseElement.class;
		}

		@Override
		protected Class<? extends EObject> getSourceClass() {
			return BaseElement.class;
		}

		@Override
		public void preReconnect(IReconnectionContext context) {
			AnchorContainer ac;
			ac = context.getConnection().getStart().getParent();
			oldSource = BusinessObjectUtil.getFirstBaseElement(ac);
			ac = context.getConnection().getEnd().getParent();
			oldTarget = BusinessObjectUtil.getFirstBaseElement(ac);

			super.preReconnect(context);
		}

		@Override
		public void postReconnect(IReconnectionContext context) {
			AnchorUtil.adjustAnchors(context.getOldAnchor().getParent());
			AnchorContainer ac;
			ac = context.getConnection().getStart().getParent();
			BaseElement newSource = BusinessObjectUtil.getFirstBaseElement(ac);
			ac = context.getConnection().getEnd().getParent();
			BaseElement newTarget = BusinessObjectUtil.getFirstBaseElement(ac);
			Association association = BusinessObjectUtil.getFirstElementOfType(context.getConnection(), Association.class);
			updateCompensationHandlers(getFeatureProvider(), association, oldSource, oldTarget, newSource, newTarget);
				
			super.postReconnect(context);
		}
	} 

	public static class DeleteAssociationFeature extends DefaultDeleteBPMNShapeFeature {

		/**
		 * @param fp
		 */
		public DeleteAssociationFeature(IFeatureProvider fp) {
			super(fp);
		}
		
		@Override
		public boolean canDelete(IDeleteContext context) {
			// don't delete if it's not an Association Connection
			if (!(context.getPictogramElement() instanceof Connection))
				return false;
			return super.canDelete(context);
		}
		
		@Override
		public void delete(IDeleteContext context) {
			// we already know this is a Connection because of canDelete()
			Connection connection = (Connection) context.getPictogramElement();
			AnchorContainer ac;
			ac = connection.getStart().getParent();
			BaseElement oldSource = BusinessObjectUtil.getFirstBaseElement(ac);
			ac = connection.getEnd().getParent();
			BaseElement oldTarget = BusinessObjectUtil.getFirstBaseElement(ac);
			Association association = BusinessObjectUtil.getFirstElementOfType(connection, Association.class);
			updateCompensationHandlers(getFeatureProvider(), association, oldSource, oldTarget, null, null);
			
			super.delete(context);
		}
	}
}