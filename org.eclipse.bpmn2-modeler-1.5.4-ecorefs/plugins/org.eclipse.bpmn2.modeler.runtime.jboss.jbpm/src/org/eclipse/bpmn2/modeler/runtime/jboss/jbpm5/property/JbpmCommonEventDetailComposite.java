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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property;

import java.util.List;

import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.ConditionalEventDefinition;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.LinkEventDefinition;
import org.eclipse.bpmn2.ScriptTask;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.TimerEventDefinition;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractListComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor;
import org.eclipse.bpmn2.modeler.core.utils.EventDefinitionsUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.ui.property.editors.ExpressionLanguageObjectEditor;
import org.eclipse.bpmn2.modeler.ui.property.events.CommonEventDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.ConditionalEventDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.EventDefinitionsListComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.LinkEventDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.events.TimerEventDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.tasks.DataAssociationDetailComposite.MapType;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Bob Brodt
 *
 */
public class JbpmCommonEventDetailComposite extends CommonEventDetailComposite {

	/**
	 * @param parent
	 * @param style
	 */
	public JbpmCommonEventDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * @param section
	 */
	public JbpmCommonEventDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	@Override
	protected AbstractListComposite bindList(final EObject object, EStructuralFeature feature, EClass listItemClass) {
		if (isModelObjectEnabled(object.eClass(), feature)) {
			if ("eventDefinitions".equals(feature.getName())) { //$NON-NLS-1$
				eventsTable = new EventDefinitionsListComposite(this, (Event)object) {

					@Override
					protected List<EClass> getAllowedEventDefinitions(Event event, Object parentContainer) {
						List<EClass> list = FeatureSupport.getAllowedEventDefinitions(event, parentContainer);
						if (event instanceof BoundaryEvent) {
							if (((BoundaryEvent)event).getAttachedToRef() instanceof ScriptTask) {
								// See https://bugzilla.redhat.com/show_bug.cgi?id=1141619
								// remove timer event because it's not allowed on a ScriptTask
								list.remove(Bpmn2Package.eINSTANCE.getTimerEventDefinition());
							}
						}
						return list;
					}

					@Override
					public AbstractDetailComposite createDetailComposite(Class eClass, Composite parent, int style) {
						if (eClass==TimerEventDefinition.class) {
							return new JbpmTimerEventDefinitionDetailComposite(parent, style);
						}
						if (eClass==ConditionalEventDefinition.class){
							return new ConditionalEventDefinitionDetailComposite(parent, style);
						}
						if (eClass==LinkEventDefinition.class){
							return new LinkEventDefinitionDetailComposite(parent, style);
						}
						EventDefinitionsDetailComposite details = new EventDefinitionsDetailComposite(parent, (Event)getBusinessObject()) {
							@Override
							public void createBindings(EObject be) {
								super.createBindings(be);
								if (object instanceof CatchEvent)
									getDataAssociationComposite().setAllowedMapTypes(MapType.Property.getValue());
								else
									getDataAssociationComposite().setAllowedMapTypes(MapType.Property.getValue() | MapType.SingleAssignment.getValue());
							}
						};
						return details;
					}
					
					@Override
					protected EObject addListItem(EObject object, EStructuralFeature feature) {
						List<EventDefinition> eventDefinitions = null;
						if (event instanceof ThrowEvent)
							eventDefinitions = ((ThrowEvent)event).getEventDefinitions();
						else if  (event instanceof CatchEvent)
							eventDefinitions = ((CatchEvent)event).getEventDefinitions();
							
						if (eventDefinitions.size()>0) {
							MessageDialog.openError(getShell(), Messages.JbpmCommonEventDetailComposite_Error_Title,
								Messages.JbpmCommonEventDetailComposite_Error_Message
							);
							return null;
						}
						EObject result = super.addListItem(object, feature);
						if (EventDefinitionsUtil.hasItemDefinition((EventDefinition)result)) {
							if (event instanceof ThrowEvent) {
								DataInput input = ((ThrowEvent)event).getDataInputs().get(0);
								input.setName("event"); //$NON-NLS-1$
							}
							else if  (event instanceof CatchEvent) {
								DataOutput output = ((CatchEvent)event).getDataOutputs().get(0);
								output.setName("event"); //$NON-NLS-1$
							}
						}
						return result;
					}
				};
				eventsTable.bindList(object, feature);
				eventsTable.setTitle(Messages.JbpmCommonEventDetailComposite_Title);
				return eventsTable;
			}
			else if ("dataInputs".equals(feature.getName()) || "dataOutputs".equals(feature.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
				// only show Input/Output list if the event definition requires it
				List<EventDefinition> eventDefinitions = null;
				if (object instanceof ThrowEvent)
					eventDefinitions = ((ThrowEvent)object).getEventDefinitions();
				else if  (object instanceof CatchEvent)
					eventDefinitions = ((CatchEvent)object).getEventDefinitions();
				if (eventDefinitions.size()>0) {
					if (EventDefinitionsUtil.hasItemDefinition(eventDefinitions.get(0))) {
						super.bindList(object, feature, listItemClass);
					}
				}
			}
			else
				return super.bindList(object, feature, listItemClass);
		}
		return null;
	}
	
	public static class JbpmTimerEventDefinitionDetailComposite extends TimerEventDefinitionDetailComposite {

		protected ObjectEditor scriptLanguageEditor;

		public JbpmTimerEventDefinitionDetailComposite(Composite parent, int style) {
			super(parent, style);
			// TODO Auto-generated constructor stub
		}

		/**
		 * @param section
		 */
		public JbpmTimerEventDefinitionDetailComposite(AbstractBpmn2PropertySection section) {
			super(section);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void createBindings(EObject be) {
			super.createBindings(be);
			scriptLanguageEditor = new ExpressionLanguageObjectEditor(this, expression, PACKAGE.getFormalExpression_Language());
			scriptLanguageEditor.createControl(getAttributesParent(), Messages.JbpmCommonEventDetailComposite_TimerScriptLanguage);
		}
	}
}
