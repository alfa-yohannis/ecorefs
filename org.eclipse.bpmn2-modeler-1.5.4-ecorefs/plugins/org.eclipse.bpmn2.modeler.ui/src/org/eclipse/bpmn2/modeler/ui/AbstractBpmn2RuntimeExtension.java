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
package org.eclipse.bpmn2.modeler.ui;

import java.util.Enumeration;

import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XNIException;
import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Assignment;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.CallChoreography;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.Category;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.ConditionalEventDefinition;
import org.eclipse.bpmn2.CorrelationPropertyRetrievalExpression;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataObject;
import org.eclipse.bpmn2.DataObjectReference;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataStore;
import org.eclipse.bpmn2.DataStoreReference;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.ErrorEventDefinition;
import org.eclipse.bpmn2.EscalationEventDefinition;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.Expression;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.GlobalBusinessRuleTask;
import org.eclipse.bpmn2.GlobalManualTask;
import org.eclipse.bpmn2.GlobalScriptTask;
import org.eclipse.bpmn2.GlobalTask;
import org.eclipse.bpmn2.GlobalUserTask;
import org.eclipse.bpmn2.HumanPerformer;
import org.eclipse.bpmn2.Import;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.Interface;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.LinkEventDefinition;
import org.eclipse.bpmn2.ManualTask;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.MessageFlow;
import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.Operation;
import org.eclipse.bpmn2.Performer;
import org.eclipse.bpmn2.PotentialOwner;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.ResourceAssignmentExpression;
import org.eclipse.bpmn2.ResourceParameterBinding;
import org.eclipse.bpmn2.ResourceRole;
import org.eclipse.bpmn2.ScriptTask;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.SignalEventDefinition;
import org.eclipse.bpmn2.StandardLoopCharacteristics;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.TextAnnotation;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.TimerEventDefinition;
import org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDialogComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultListComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.PropertiesCompositeFactory;
import org.eclipse.bpmn2.modeler.core.preferences.ModelEnablements;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.modeler.ui.diagram.Bpmn2FeatureMap;
import org.eclipse.bpmn2.modeler.ui.property.StyleChangeAdapter;
import org.eclipse.bpmn2.modeler.ui.property.artifact.CategoryDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.artifact.TextAnnotationDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.connectors.MessageFlowDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.connectors.SequenceFlowDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.DataAssignmentDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.DataObjectPropertySection.DataObjectDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.DataObjectReferencePropertySection.DataObjectReferenceDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.DataStorePropertySection.DataStoreDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.DataStoreReferencePropertySection.DataStoreReferenceDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.ExpressionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.InterfaceDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.ItemAwareElementDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.MessageDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.MessageListComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.OperationDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.ResourceAssignmentExpressionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.ResourceParameterBindingDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.data.ResourceRoleDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.CorrelationPropertyREListComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.DefinitionsPropertyComposite.ImportDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.ItemDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.ItemDefinitionListComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.ProcessDiagramDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.PropertyListComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.ResourceRoleListComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.BoundaryEventDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.CatchEventDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.CommonEventDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.CommonEventPropertySection.EventDefinitionDialogComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.ConditionalEventDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.EndEventDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.LinkEventDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.StartEventDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.ThrowEventDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.TimerEventDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.gateways.GatewayDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.ActivityDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.ActivityInputDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.ActivityOutputDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.DataAssociationDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.IoParametersDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.ManualTaskDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.MultiInstanceLoopCharacteristicsDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.ScriptTaskDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.StandardLoopCharacteristicsDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.TaskDetailComposite;
import org.eclipse.bpmn2.modeler.ui.util.PropertyUtil;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.ui.IEditorInput;
import org.xml.sax.InputSource;


public abstract class AbstractBpmn2RuntimeExtension implements IBpmn2RuntimeExtension {

	public AbstractBpmn2RuntimeExtension() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension#isContentForRuntime(org.eclipse.ui.IEditorInput)
	 */
	@Override
	public boolean isContentForRuntime(IEditorInput input) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension#getTargetNamespace(org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType)
	 */
	@Override
	public abstract String getTargetNamespace(Bpmn2DiagramType diagramType);

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension#notify(org.eclipse.bpmn2.modeler.core.LifecycleEvent)
	 */

	@Override
	public void notify(LifecycleEvent event) {
//		System.out.println(event.eventType+ ": " + event.target);
		TargetRuntime targetRuntime = event.targetRuntime;
		if (event.eventType.equals(EventType.EDITOR_INITIALIZED)) {
			if (event.target instanceof IAdaptable) {
				ModelEnablements me = (ModelEnablements) ((IAdaptable)event.target).getAdapter(ModelEnablements.class);
				me.setEnabled("BaseElement", "style", true); //$NON-NLS-1$ //$NON-NLS-2$
				me.setEnabled("ShapeStyle", true); //$NON-NLS-1$
			}
			PropertiesCompositeFactory.register(EObject.class, DefaultDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(EObject.class, DefaultListComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(EObject.class, DefaultDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Message.class, MessageDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Message.class, MessageListComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(MessageFlow.class, MessageFlowDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Property.class, ItemAwareElementDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(CallActivity.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(GlobalTask.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(GlobalBusinessRuleTask.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(GlobalManualTask.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(GlobalScriptTask.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(GlobalUserTask.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Import.class, ImportDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Category.class, CategoryDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(TextAnnotation.class, TextAnnotationDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(SequenceFlow.class, SequenceFlowDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(DataObject.class, DataObjectDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(DataObjectReference.class, DataObjectDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Assignment.class, DataAssignmentDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Expression.class, ExpressionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(FormalExpression.class, ExpressionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ResourceAssignmentExpression.class, ResourceAssignmentExpressionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ResourceParameterBinding.class, ResourceParameterBindingDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(PotentialOwner.class, ResourceRoleDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(HumanPerformer.class, ResourceRoleDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Performer.class, ResourceRoleDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(DataObjectReference.class, DataObjectReferenceDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(DataStore.class, DataStoreDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(DataStoreReference.class, DataStoreReferenceDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Interface.class, InterfaceDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Operation.class, OperationDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ItemDefinition.class, ItemDefinitionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ItemDefinition.class, ItemDefinitionListComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(CorrelationPropertyRetrievalExpression.class, CorrelationPropertyREListComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Property.class, PropertyListComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ResourceRole.class, ResourceRoleListComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Event.class, CommonEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(StartEvent.class, StartEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(EndEvent.class, EndEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(CatchEvent.class, CatchEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ThrowEvent.class, ThrowEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(BoundaryEvent.class, BoundaryEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(TimerEventDefinition.class, TimerEventDefinitionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(LinkEventDefinition.class, LinkEventDefinitionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ConditionalEventDefinition.class, ConditionalEventDefinitionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(CompensateEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ConditionalEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ErrorEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(EscalationEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(LinkEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(MessageEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(SignalEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(TimerEventDefinition.class, EventDefinitionDialogComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Process.class, ProcessDiagramDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(EndEvent.class, EndEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(StartEvent.class, StartEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ThrowEvent.class, ThrowEventDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(StandardLoopCharacteristics.class, StandardLoopCharacteristicsDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(MultiInstanceLoopCharacteristics.class, MultiInstanceLoopCharacteristicsDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Gateway.class, GatewayDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Activity.class, ActivityInputDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(InputOutputSpecification.class, ActivityInputDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Activity.class, ActivityOutputDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(CallChoreography.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(InputOutputSpecification.class, IoParametersDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(DataInput.class, DataAssociationDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(DataOutput.class, DataAssociationDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ManualTask.class, ManualTaskDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ScriptTask.class, ScriptTaskDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(SubProcess.class, ActivityDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Task.class, TaskDetailComposite.class, targetRuntime);
		}
		else if (event.eventType.equals(EventType.PICTOGRAMELEMENT_ADDED)) {
			StyleChangeAdapter.adapt((PictogramElement) event.target);
//		}
//		else if (event.eventType.equals(EventType.BUSINESSOBJECT_CREATED) && event.target instanceof BaseElement) {
			
			BaseElement object = BusinessObjectUtil.getFirstBaseElement((PictogramElement) event.target); //(BaseElement) event.target;
			EClass ec = object.eClass();
			if (Bpmn2FeatureMap.ALL_FIGURES.contains(ec.getInstanceClass()) && ShapeStyle.hasStyle(object))
				ShapeStyle.createStyleObject(object);
		}
		else if (event.eventType.equals(EventType.COMMAND_UNDO) || event.eventType.equals(EventType.COMMAND_REDO)) {
			AbstractBpmn2PropertySection section = PropertyUtil.getCurrentPropertySection();
			if (section!=null) {
				// in general we want to update the current property section
				// to reflect the old/new values of an undo/redo operation.
				// However, iIf the business object being rendered by this
				// property section has been deleted, the framework will throw
				// an NPE - this is safe to ignore
				try {
					section.redraw();
				}
				catch (Exception e) {
				}
			}
		}
	}

	/**
	 * A simple XML parser that checks if the root element of an xml document contains any
	 * namespace definitions matching the given namespace URI.
	 * 
	 * @author bbrodt
	 */
	public static class RootElementParser extends SAXParser {
		private String namespace;
		private boolean result = false;
		
		/**
		 * @param namespace - the namespace URI to scan for.
		 */
		public RootElementParser(String namespace) {
			this.namespace = namespace;
		}
		
		public boolean getResult() {
			return result;
		}
		
		public void parse(InputSource source) {
			result = false;
			try {
				super.parse(source);
			} catch (AcceptedException e) {
				result = true;
			} catch (Exception e) {
			}
		}
		
		@Override
		public void startElement(QName qName, XMLAttributes attributes, Augmentations augmentations)
				throws XNIException {

			super.startElement(qName, attributes, augmentations);

			// search the "definitions" for a namespace that matches the required namespace
			if ("definitions".equals(qName.localpart)) { //$NON-NLS-1$
				String namespace = attributes.getValue("targetNamespace"); //$NON-NLS-1$
				if (this.namespace.equals(namespace))
					throw new AcceptedException(qName.localpart);
				Enumeration<?> e = fNamespaceContext.getAllPrefixes();
				while (e.hasMoreElements()) {
					String prefix = (String)e.nextElement();
					namespace = fNamespaceContext.getURI(prefix);
					if (this.namespace.equals(namespace))
						throw new AcceptedException(qName.localpart);
				}
				throw new RejectedException();
			} else {
				throw new RejectedException();
			}
		}
	}

	public static class AcceptedException extends RuntimeException {
		public String acceptedRootElement;

		public AcceptedException(String acceptedRootElement) {
			this.acceptedRootElement = acceptedRootElement;
		}

		private static final long serialVersionUID = 1L;
	}

	public static class RejectedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
