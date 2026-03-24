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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.ChoreographyActivity;
import org.eclipse.bpmn2.ChoreographyTask;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.di.ParticipantBandKind;
import org.eclipse.bpmn2.modeler.core.features.AbstractUpdateBaseElementFeature;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.choreography.ChoreographyUtil;
import org.eclipse.bpmn2.modeler.core.features.choreography.Messages;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.AnchorSite;
import org.eclipse.bpmn2.modeler.core.utils.AnchorType;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.ShapeDecoratorUtil;
import org.eclipse.bpmn2.modeler.core.utils.ShapeDecoratorUtil.Envelope;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.bpmn2.modeler.core.utils.Tuple;
import org.eclipse.dd.dc.Bounds;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.PropertyContainer;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.ui.services.GraphitiUi;
import org.eclipse.graphiti.util.IColorConstant;

public class UpdateChoreographyMessageLinkFeature extends AbstractUpdateBaseElementFeature<BaseElement> {

	private static IGaService gaService = Graphiti.getGaService();
	private static IPeService peService = Graphiti.getPeService();

	public UpdateChoreographyMessageLinkFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canUpdate(IUpdateContext context) {
		boolean result = false;
		if (super.canUpdate(context)) {
			result = ChoreographyUtil.getChoreographyActivityShape(context.getPictogramElement()) != null;
		}
		return result;
	}

	@Override
	public IReason updateNeeded(IUpdateContext context) {
		IReason reason = super.updateNeeded(context);
		if (reason.toBoolean()) {
			return reason;
		}

		// check all of the Participant Bands to see if any MessageLinks need to be updated
		ContainerShape choreographyActivityShape = ChoreographyUtil.getChoreographyActivityShape(context.getPictogramElement());
		ChoreographyActivity choreographyActivity = BusinessObjectUtil.getFirstElementOfType(choreographyActivityShape, ChoreographyActivity.class);
		String shapeIds = ChoreographyUtil.getParticipantRefIds(choreographyActivityShape);
		String activityIds = ChoreographyUtil.getParticipantRefIds(choreographyActivity);
		if (!shapeIds.equals(activityIds))
			reason = Reason.createTrueReason(Messages.UpdateChoreographyMessageLinkFeature_Participants_Changed);
		else {
			List<ContainerShape> bandShapes = FeatureSupport.getParticipantBandContainerShapes(choreographyActivityShape);
			for (ContainerShape bandShape : bandShapes) {
				BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(bandShape, BPMNShape.class);
				boolean visible = new Boolean(FeatureSupport.getPropertyValue(bandShape, ChoreographyUtil.MESSAGE_VISIBLE));
				if (bpmnShape.isIsMessageVisible() != visible) {
					reason = Reason.createTrueReason(Messages.UpdateChoreographyMessageLinkFeature_Message_Link_Visible_Changed);
					break;
				}
			}
		}

		return reason;
	}

	@Override
	public boolean update(IUpdateContext context) {

		if (!canUpdate(context))
			return false;
		
		ContainerShape choreographyTaskShape = ChoreographyUtil.getChoreographyActivityShape(context.getPictogramElement());

		IFeatureProvider fp = getFeatureProvider();
		List<MessageFlow> messageFlows = new ArrayList<MessageFlow>();
		ChoreographyTask choreography = BusinessObjectUtil.getFirstElementOfType(choreographyTaskShape,
				ChoreographyTask.class);
		if (choreography != null) {
			messageFlows.addAll(choreography.getMessageFlowRef());
		}

		List<ContainerShape> bandShapes = FeatureSupport.getParticipantBandContainerShapes(choreographyTaskShape);
		Tuple<List<ContainerShape>, List<ContainerShape>> topAndBottom = FeatureSupport.getTopAndBottomBands(bandShapes);
		List<ContainerShape> shapesWithVisibleMessages = new ArrayList<ContainerShape>();

		FixPointAnchor topAnchor = null;
		FixPointAnchor bottomAnchor = null;
		Connection topConnection = null;
		Connection bottomConnection = null;

		for (Connection connection : AnchorUtil.getConnections(choreographyTaskShape, AnchorSite.TOP)) {
			AnchorContainer container = connection.getEnd().getParent();
			String property = FeatureSupport.getPropertyValue((PropertyContainer) container, GraphitiConstants.MESSAGE_LINK);
			if (Boolean.parseBoolean(property)) {
				topConnection = connection;
				topAnchor = (FixPointAnchor) connection.getEnd();
				break;
			}
		}
		if (topAnchor==null)
			topAnchor = AnchorUtil.createAnchor(choreographyTaskShape, AnchorSite.TOP);
		// this prevents any of the routers from moving this anchor
		AnchorType.setType(topAnchor, AnchorType.MESSAGELINK);

		for (Connection connection : AnchorUtil.getConnections(choreographyTaskShape, AnchorSite.BOTTOM)) {
			AnchorContainer container = connection.getEnd().getParent();
			String property = FeatureSupport.getPropertyValue((PropertyContainer) container, GraphitiConstants.MESSAGE_LINK);
			if (Boolean.parseBoolean(property)) {
				bottomConnection = connection;
				bottomAnchor = (FixPointAnchor) connection.getEnd();
				break;
			}
		}
		if (bottomAnchor==null)
			bottomAnchor = AnchorUtil.createAnchor(choreographyTaskShape, AnchorSite.BOTTOM);
		// this prevents any of the routers from moving this anchor
		AnchorType.setType(bottomAnchor, AnchorType.MESSAGELINK);

		for (ContainerShape bandShape : bandShapes) {
			BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(bandShape, BPMNShape.class);
			if (bpmnShape.isIsMessageVisible()) {
				shapesWithVisibleMessages.add(bandShape);
			}
		}

		boolean shouldDrawTopMessage = !Collections.disjoint(topAndBottom.getFirst(), shapesWithVisibleMessages);
		boolean shouldDrawBottomMessage = !Collections.disjoint(topAndBottom.getSecond(), shapesWithVisibleMessages);
		ContainerShape envelope;
		
		String topMessageName = null;
		String bottomMessageName = null;
		Message topMessage = null;
		Message bottomMessage = null;

		if (shouldDrawTopMessage) {
			topMessage = getMessage(messageFlows, topAndBottom.getFirst(), false);
			topMessageName = getMessageName(messageFlows, topAndBottom.getFirst());
		}
		if (topMessageName == null) {
			topMessageName = new String();
		}

		if (shouldDrawBottomMessage) {
			bottomMessage = getMessage(messageFlows, topAndBottom.getSecond(), false);
			bottomMessageName = getMessageName(messageFlows, topAndBottom.getSecond());
		}
		if (bottomMessageName == null) {
			bottomMessageName = new String();
		}

		BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(choreographyTaskShape, BPMNShape.class);
		Bounds bounds = bpmnShape.getBounds();
		int x = (int) ((bounds.getX() + bounds.getWidth() / 2) - (ChoreographyUtil.ENV_W / 2));

		MessageFlow flow = getMessageFlow(messageFlows, topAndBottom.getFirst());
		envelope = null;
		if (topConnection==null && shouldDrawTopMessage && flow!=null) {
			int y = (int) (bounds.getY() - ChoreographyUtil.ENVELOPE_HEIGHT_MODIFIER - ChoreographyUtil.ENV_H);
			envelope = drawMessageLink(topMessage, topMessageName, topAnchor, x, y, isFilled(topAndBottom.getFirst()));
			FeatureSupport.setPropertyValue(envelope, ChoreographyUtil.MESSAGE_NAME, topMessageName);
		}
		else if (topConnection!=null && !shouldDrawTopMessage) {
			envelope = (ContainerShape) topConnection.getEnd().getParent();
			peService.deletePictogramElement(topConnection);
			peService.deletePictogramElement(envelope);
			envelope = null;
		}
		else if (topConnection!=null && shouldDrawTopMessage && flow!=null) {
			envelope = (ContainerShape) topConnection.getEnd().getParent();
			setMessageLabel(topMessageName, envelope);
		}
		if (envelope!=null) {
			// link up the message flow
			linkMessageFlow(fp, flow, envelope);
		}

		envelope = null;
		flow = getMessageFlow(messageFlows, topAndBottom.getSecond());
		if (bottomConnection==null && shouldDrawBottomMessage && flow!=null) {
			int y = (int) (bounds.getY() + bounds.getHeight() + ChoreographyUtil.ENVELOPE_HEIGHT_MODIFIER);
			envelope = drawMessageLink(bottomMessage, bottomMessageName, bottomAnchor, x, y, isFilled(topAndBottom.getSecond()));
			FeatureSupport.setPropertyValue(envelope, ChoreographyUtil.MESSAGE_NAME, bottomMessageName);
		}
		else if (bottomConnection!=null && !shouldDrawBottomMessage) {
			envelope = (ContainerShape) bottomConnection.getEnd().getParent();
			peService.deletePictogramElement(bottomConnection);
			peService.deletePictogramElement(envelope);
			envelope = null;
		}
		else if (bottomConnection!=null && shouldDrawBottomMessage && flow!=null) {
			envelope = (ContainerShape) bottomConnection.getEnd().getParent();
			setMessageLabel(bottomMessageName, envelope);
		}
		
		if (envelope!=null) {
			// link up the message flow
			linkMessageFlow(fp, flow, envelope);
		}

		for (ContainerShape bandShape : bandShapes) {
			bpmnShape = BusinessObjectUtil.getFirstElementOfType(bandShape, BPMNShape.class);
			FeatureSupport.setPropertyValue(bandShape, ChoreographyUtil.MESSAGE_VISIBLE,
					Boolean.toString(bpmnShape.isIsMessageVisible()));
		}

		AnchorUtil.adjustAnchors(choreographyTaskShape);
		return true;
	}

	private void linkMessageFlow(IFeatureProvider fp, MessageFlow flow,ContainerShape envelope) {
		for (Anchor a : envelope.getAnchors()) {
			for (Connection c : a.getIncomingConnections()) {
				fp.link(c, flow);
			}
			for (Connection c : a.getOutgoingConnections()) {
				fp.link(c, flow);
			}
		}
	}
	
	private boolean isFilled(List<ContainerShape> bands) {
		boolean filled = true;
		for (ContainerShape band : bands) {
			BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(band, BPMNShape.class);
			if (!bpmnShape.isIsMessageVisible()) {
				continue;
			}
			ParticipantBandKind bandKind = bpmnShape.getParticipantBandKind();
			if (bandKind == ParticipantBandKind.TOP_INITIATING || bandKind == ParticipantBandKind.BOTTOM_INITIATING
					|| bandKind == ParticipantBandKind.MIDDLE_INITIATING) {
				filled = false;
				break;
			}
		}
		return filled;
	}

	private void setMessageLabel(String label, PictogramElement message) {
		ContainerShape containerShape = (ContainerShape) message;
		Iterator<Shape> iterator = peService.getAllContainedShapes(containerShape).iterator();
		while (iterator.hasNext()) {
			Shape shape = iterator.next();
			if (shape.getGraphicsAlgorithm() instanceof Text) {
				Text text = (Text) shape.getGraphicsAlgorithm();
				text.setValue(label);
				IDimension size = GraphitiUi.getUiLayoutService().calculateTextSize(label, text.getFont());
				gaService.setSize(containerShape.getGraphicsAlgorithm(), ChoreographyUtil.ENV_W + size.getWidth() + 3, ChoreographyUtil.ENV_H);
				gaService.setSize(text, size.getWidth(), size.getHeight());
				FeatureSupport.setPropertyValue(containerShape, ChoreographyUtil.MESSAGE_NAME, label);
				break;
			}
		}
	}

	private String getMessageName(List<MessageFlow> messageFlows, List<ContainerShape> bands) {
		for (ContainerShape band : bands) {
			Participant participant = BusinessObjectUtil.getFirstElementOfType(band, Participant.class);
			BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(band, BPMNShape.class);
			if (bpmnShape.isIsMessageVisible()) {
				for (MessageFlow flow : messageFlows) {
					if (flow.getSourceRef().equals(participant)) {
						return ChoreographyUtil.getMessageFlowName(flow);
					}
				}
			}
		}
		return null;
	}

	private MessageFlow getMessageFlow(List<MessageFlow> messageFlows, List<ContainerShape> bands) {
		for (ContainerShape band : bands) {
			Participant participant = BusinessObjectUtil.getFirstElementOfType(band, Participant.class);
			BPMNShape bpmnShape = BusinessObjectUtil.getFirstElementOfType(band, BPMNShape.class);
			if (bpmnShape.isIsMessageVisible()) {
				for (MessageFlow flow : messageFlows) {
					if (flow.getSourceRef().equals(participant)) {
						return flow;
					}
				}
			}
		}
		return null;
	}

	private Message getMessage(List<MessageFlow> messageFlows, List<ContainerShape> bands, boolean create) {
		MessageFlow flow = getMessageFlow(messageFlows, bands);
		if (flow!=null) {
			if (flow.getMessageRef()==null && create) {
				Message msg = Bpmn2ModelerFactory.createObject(flow.eResource(), Message.class);
				msg.setName(Messages.ChoreographyUtil_Undefined_Message);
				ModelUtil.getDefinitions(flow).getRootElements().add(msg);
				flow.setMessageRef(msg);
				ModelUtil.setID(msg);
			}
			return flow.getMessageRef();
		}
		return null;
	}

	private ContainerShape drawMessageLink(Message message, String name, FixPointAnchor choreographyTaskAnchor, int x, int y, boolean filled) {
		Diagram diagram = peService.getDiagramForAnchor(choreographyTaskAnchor);

		FreeFormConnection connection = peService.createFreeFormConnection(diagram);
		Polyline connectionLine = gaService.createPolyline(connection);
		connectionLine.setForeground(gaService.manageColor(diagram, StyleUtil.CLASS_FOREGROUND));
		connectionLine.setLineStyle(LineStyle.DOT);
		connectionLine.setLineWidth(2);
		FeatureSupport.setPropertyValue(connection, GraphitiConstants.MESSAGE_LINK, Boolean.toString(true));

		ContainerShape envelope = peService.createContainerShape(diagram, true);
		Rectangle invisibleRectangle = gaService.createInvisibleRectangle(envelope);
		gaService.setLocation(invisibleRectangle, x, y);
		gaService.setSize(invisibleRectangle, ChoreographyUtil.ENV_W, ChoreographyUtil.ENV_H);
		getFeatureProvider().link(envelope, message);
		FeatureSupport.setPropertyValue(envelope, GraphitiConstants.MESSAGE_LINK, Boolean.toString(true));

		Shape envelopeShape = peService.createShape(envelope, false);
		Envelope envelopeGa = ShapeDecoratorUtil.createEnvelope(envelopeShape, 0, 0, ChoreographyUtil.ENV_W, ChoreographyUtil.ENV_H);
		IColorConstant color = filled ? IColorConstant.LIGHT_GRAY : IColorConstant.WHITE;
		envelopeGa.rect.setFilled(true);
		envelopeGa.rect.setBackground(gaService.manageColor(diagram, color));
		envelopeGa.rect.setForeground(gaService.manageColor(diagram, StyleUtil.CLASS_FOREGROUND));
		envelopeGa.line.setForeground(gaService.manageColor(diagram, StyleUtil.CLASS_FOREGROUND));

		Shape textShape = peService.createShape(envelope, false);
		Text text = gaService.createDefaultText(diagram, textShape);
		IDimension size = GraphitiUi.getUiLayoutService().calculateTextSize(name, text.getFont());
		gaService.setLocationAndSize(text, ChoreographyUtil.ENV_W + 3, 3, size.getWidth(), size.getHeight());
		text.setValue(name);

		gaService.setSize(invisibleRectangle, ChoreographyUtil.ENV_W + size.getWidth() + 3, ChoreographyUtil.ENV_H);

		FixPointAnchor envelopeAnchor;
		if (AnchorSite.getSite(choreographyTaskAnchor) == AnchorSite.TOP) {
			envelopeAnchor = AnchorUtil.createAnchor(envelope, AnchorSite.BOTTOM);
			AnchorUtil.moveAnchor(envelopeAnchor, x + ChoreographyUtil.ENV_W/2, y + ChoreographyUtil.ENV_H);
		} else {
			envelopeAnchor = AnchorUtil.createAnchor(envelope, AnchorSite.TOP);
			AnchorUtil.moveAnchor(envelopeAnchor, x + ChoreographyUtil.ENV_W/2, y);
		}
		
		connection.setStart(choreographyTaskAnchor);
		connection.setEnd(envelopeAnchor);

		return envelope;
	}

}