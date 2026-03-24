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
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.ui.adapters.properties;

import org.eclipse.bpmn2.Association;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.features.flow.AssociationFeatureContainer;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.DeleteContext;
import org.eclipse.graphiti.features.context.impl.ReconnectionContext;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.editor.DiagramEditor;

/**
 * @author Bob Brodt
 *
 */
public class AssociationPropertiesAdapter extends ExtendedPropertiesAdapter<Association> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public AssociationPropertiesAdapter(AdapterFactory adapterFactory, Association object) {
		super(adapterFactory, object);

    	EStructuralFeature ref;
    	
    	ref = Bpmn2Package.eINSTANCE.getAssociation_SourceRef();
    	setFeatureDescriptor(ref, new SourceTargetFeatureDescriptor(this,object,ref));
		setProperty(ref, UI_CAN_EDIT_INLINE, Boolean.FALSE);
		setProperty(ref, UI_CAN_EDIT, Boolean.FALSE);
		setProperty(ref, UI_CAN_CREATE_NEW, Boolean.FALSE);
		setProperty(ref, UI_IS_MULTI_CHOICE, Boolean.TRUE);

		ref = Bpmn2Package.eINSTANCE.getAssociation_TargetRef();
    	setFeatureDescriptor(ref, new SourceTargetFeatureDescriptor(this,object,ref));
		setProperty(ref, UI_CAN_EDIT_INLINE, Boolean.FALSE);
		setProperty(ref, UI_CAN_EDIT, Boolean.FALSE);
		setProperty(ref, UI_CAN_CREATE_NEW, Boolean.FALSE);
		setProperty(ref, UI_IS_MULTI_CHOICE, Boolean.TRUE);
	}

	public class SourceTargetFeatureDescriptor extends FeatureDescriptor<Association> {

		public SourceTargetFeatureDescriptor(ExtendedPropertiesAdapter<Association> owner, Association object,
				EStructuralFeature feature) {
			super(owner, object, feature);
		}

		@Override
		public String getLabel() {
			if (feature == Bpmn2Package.eINSTANCE.getAssociation_SourceRef())
				return Messages.AssociationPropertiesAdapter_Source;
			return Messages.AssociationPropertiesAdapter_Target;
		}

		@SuppressWarnings("unchecked")
		@Override
   		protected void internalSet(Association association, EStructuralFeature feature, Object value, int index) {
			BaseElement element = null;
			if (value instanceof BaseElement) {
				element = (BaseElement) value;
			}
			if (feature == Bpmn2Package.eINSTANCE.getAssociation_SourceRef()) {
				association.setSourceRef(element);
			}
			else {
				association.setTargetRef(element);
			}
			updateConnectionIfNeeded(association, element);
		}

		private void updateConnectionIfNeeded(Association association, BaseElement element) {
			DiagramEditor diagramEditor = ModelUtil.getDiagramEditor(association);
			if (diagramEditor==null)
				return;
			
			boolean updateConnection = false;
			IFeatureProvider fp = diagramEditor.getDiagramTypeProvider().getFeatureProvider();
			
			ContainerShape eventShape = DIUtils.getContainerShape(association.getSourceRef());
			ContainerShape activityShape = DIUtils.getContainerShape(association.getTargetRef());
			Connection connection = DIUtils.getConnection(association);

			if (connection!=null) {
				// There's an existing Association connection which needs to
				// either be reconnected or deleted depending on user action.
				if (eventShape!=null && activityShape!=null) {
					// need to reconnect the Association
					ReconnectionContext rc = null;
					Point p = GraphicsUtil.createPoint(connection.getStart());
					Anchor a = AnchorUtil.createAnchor((AnchorContainer) activityShape, p);
					rc = new ReconnectionContext(connection, connection.getStart(), a, null);
					rc.setTargetPictogramElement(activityShape);
					rc.setTargetLocation(Graphiti.getPeService().getLocationRelativeToDiagram(a));
					rc.setReconnectType(ReconnectionContext.RECONNECT_TARGET);
					IReconnectionFeature rf = fp.getReconnectionFeature(rc);
					if (rf.canReconnect(rc)) {
						rf.reconnect(rc);
						updateConnection = true;
					}
				}
				else {
					// need to delete the Association connection
					DeleteContext dc = new DeleteContext(connection);
					connection.getLink().getBusinessObjects().remove(0);
					IDeleteFeature df = fp.getDeleteFeature(dc);
					df.delete(dc);
				}
			}
			else if (eventShape!=null && activityShape!=null) {
				// There is no Association connection yet, but we have source
				// and target shapes so create one.
				Point p = GraphicsUtil.createPoint((AnchorContainer) activityShape);
				Anchor eventAnchor = AnchorUtil.createAnchor(eventShape, p);
				p = GraphicsUtil.createPoint(eventShape);
				Anchor activityAnchor = AnchorUtil.createAnchor((AnchorContainer) activityShape, p);
				CreateConnectionContext ccc = new CreateConnectionContext();
				ccc.setSourcePictogramElement(eventShape);
				ccc.setSourceAnchor(eventAnchor);
				ccc.setTargetPictogramElement(activityShape);
				ccc.setTargetAnchor(activityAnchor);
				ICreateConnectionFeature ccf = new AssociationFeatureContainer().getCreateConnectionFeature(fp);
				connection = ccf.create(ccc);
			}
			if (updateConnection) {
				FeatureSupport.updateConnection(diagramEditor.getDiagramTypeProvider().getFeatureProvider(), connection);
			}
		}
	}
}
