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
package org.eclipse.bpmn2.modeler.ui.features.choreography;

import java.util.List;

import org.eclipse.bpmn2.ChoreographyActivity;
import org.eclipse.bpmn2.ChoreographyTask;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.features.DefaultDeleteBPMNShapeFeature;
import org.eclipse.bpmn2.modeler.core.features.DirectEditBaseElementFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.MultiUpdateFeature;
import org.eclipse.bpmn2.modeler.core.features.PropertyBasedFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.choreography.ChoreographyUtil;
import org.eclipse.bpmn2.modeler.core.features.label.UpdateLabelFeature;
import org.eclipse.bpmn2.modeler.core.utils.AnchorSite;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.Tuple;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IMoveConnectionDecoratorFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IMoveConnectionDecoratorContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.AbstractMoveShapeFeature;
import org.eclipse.graphiti.features.impl.DefaultMoveConnectionDecoratorFeature;
import org.eclipse.graphiti.features.impl.DefaultResizeShapeFeature;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;

public class ChoreographyMessageLinkFeatureContainer extends PropertyBasedFeatureContainer {

	@Override
	protected String getPropertyKey() {
		return GraphitiConstants.MESSAGE_LINK;
	}

	@Override
	protected boolean canApplyToProperty(String value) {
		return new Boolean(value);
	}

	@Override
	public boolean canApplyTo(Object o) {
		if (super.canApplyTo(o))
			return true;
		if (o instanceof Connection &&
				ChoreographyUtil.isChoreographyMessageLink((Connection)o))
			return true;
		return false;
	}

	@Override
	public ICreateFeature getCreateFeature(IFeatureProvider fp) {
		return null;
	}
	
	@Override
	public ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		MultiUpdateFeature multiUpdate = new MultiUpdateFeature(fp);
		multiUpdate.addFeature(new UpdateLabelFeature(fp));
		multiUpdate.addFeature(new UpdateChoreographyMessageFlowFeature(fp));
		return multiUpdate;
	}

	@Override
	public IDirectEditingFeature getDirectEditingFeature(IFeatureProvider fp) {
		return new DirectEditBaseElementFeature(fp);
	}

	@Override
	public ILayoutFeature getLayoutFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public IMoveShapeFeature getMoveFeature(IFeatureProvider fp) {
		return new AbstractMoveShapeFeature(fp) {

			@Override
			public void moveShape(IMoveShapeContext context) {
			}

			@Override
			public boolean canMoveShape(IMoveShapeContext context) {
				return false;
			}
		};
	}

	@Override
	public IMoveConnectionDecoratorFeature getMoveConnectionDecoratorFeature(IFeatureProvider fp) {
		return new DefaultMoveConnectionDecoratorFeature(fp) {

			@Override
			public boolean canMoveConnectionDecorator(IMoveConnectionDecoratorContext context) {
				return false;
			}

			@Override
			public void moveConnectionDecorator(IMoveConnectionDecoratorContext context) {
			}
			
		};
	}

	@Override
	public IResizeShapeFeature getResizeFeature(IFeatureProvider fp) {
		return new DefaultResizeShapeFeature(fp) {
			@Override
			public boolean canResizeShape(IResizeShapeContext context) {
				return false;
			}
		};
	}

	@Override
	public IDeleteFeature getDeleteFeature(IFeatureProvider fp) {
		return new DefaultDeleteBPMNShapeFeature(fp) {

			@Override
			public boolean canDelete(IDeleteContext context) {
				return context.getPictogramElement() instanceof ContainerShape;
			}

			@Override
			public void delete(IDeleteContext context) {
				ContainerShape envelope = (ContainerShape) context.getPictogramElement();
				for (FixPointAnchor anchor : AnchorUtil.getAnchors(envelope)) 
					modifyAffectedBands(anchor);
				super.delete(context);
			}

			private void modifyAffectedBands(FixPointAnchor anchor) {

				for (Connection connection : anchor.getIncomingConnections()) {

					AnchorContainer choreographyActivityShape = connection.getStart().getParent();

					if (!(choreographyActivityShape instanceof ContainerShape)) {
						continue;
					}

					ChoreographyActivity choreographyActivity = BusinessObjectUtil.getFirstElementOfType(choreographyActivityShape, ChoreographyActivity.class);
					if (choreographyActivity==null) {
						continue;
					}
					MessageFlow messageFlow = (MessageFlow)BusinessObjectUtil.getBusinessObjectForPictogramElement(connection);

					List<ContainerShape> bands = FeatureSupport.getParticipantBandContainerShapes((ContainerShape) choreographyActivityShape);

					Tuple<List<ContainerShape>, List<ContainerShape>> topAndBottomBands = FeatureSupport.getTopAndBottomBands(bands);

					List<ContainerShape> affectedBands = AnchorSite.getSite(anchor) == AnchorSite.BOTTOM ?
							topAndBottomBands.getFirst() :
							topAndBottomBands.getSecond();

					for (ContainerShape band : affectedBands) {
						BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(band, BPMNShape.class);
						bpmnShape.setIsMessageVisible(false);
					}

					ChoreographyUtil.removeChoreographyMessageLink(connection);
					
					if (choreographyActivity instanceof ChoreographyTask) {
						ChoreographyTask ct = (ChoreographyTask) choreographyActivity;
						ct.getMessageFlowRef().remove(messageFlow);
						UpdateContext updateContext = new UpdateContext(choreographyActivityShape);
						getFeatureProvider().updateIfPossible(updateContext);
					}
					break;
				}
			}
		};
	}

	@Override
	public IReconnectionFeature getReconnectionFeature(IFeatureProvider fp) {
		// TODO Auto-generated method stub
		return null;
	}
}