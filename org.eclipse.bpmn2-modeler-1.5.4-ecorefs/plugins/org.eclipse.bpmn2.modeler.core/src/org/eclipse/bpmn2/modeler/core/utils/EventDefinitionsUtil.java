/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
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

package org.eclipse.bpmn2.modeler.core.utils;

import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.Error;
import org.eclipse.bpmn2.ErrorEventDefinition;
import org.eclipse.bpmn2.Escalation;
import org.eclipse.bpmn2.EscalationEventDefinition;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.InputSet;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.OutputSet;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.Signal;
import org.eclipse.bpmn2.SignalEventDefinition;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.emf.ecore.resource.Resource;

/**
 *
 */
public class EventDefinitionsUtil {

	private EventDefinitionsUtil() {
	}

	public static Tuple<ItemAwareElement, DataAssociation> getIOParameter(Event event, EventDefinition eventDefinition) {
		Resource resource = event.eResource();
		ItemAwareElement element = null;
		DataAssociation association = null;
		BaseElement ioSet = null;
		List<EventDefinition> eventDefinitions = null;
		List<ItemAwareElement> parameters = null;
		List<DataAssociation> associations = null;
		ThrowEvent throwEvent = null;
		CatchEvent catchEvent = null;
		boolean isInput = false;
		if (event instanceof ThrowEvent) {
			throwEvent = (ThrowEvent)event;
			eventDefinitions = throwEvent.getEventDefinitions();
			parameters = (List)throwEvent.getDataInputs();
			associations = (List)throwEvent.getDataInputAssociation();
			ioSet = throwEvent.getInputSet();
			isInput = true;
		}
		else {
			catchEvent = (CatchEvent)event;
			eventDefinitions = catchEvent.getEventDefinitions();
			parameters = (List)catchEvent.getDataOutputs();
			associations = (List)catchEvent.getDataOutputAssociation();
			ioSet = catchEvent.getOutputSet();
		}
		
		int index = -1;
		for (EventDefinition ed : eventDefinitions) {
			element = null;
			association = null;
			if (hasItemDefinition(ed)) {
				ItemDefinition itemDefinition = getItemDefinition(ed);
				++index;
				if (parameters.size()<=index) {
					String name = ed.getId().replace("EventDefinition", ""); //$NON-NLS-1$ //$NON-NLS-2$
					if (isInput) {
						element = Bpmn2ModelerFactory.createObject(resource, DataInput.class);
						((DataInput)element).setName(name+"_Input"); //$NON-NLS-1$
					}
					else {
						element = Bpmn2ModelerFactory.createObject(resource, DataOutput.class);
						((DataOutput)element).setName(name+"_Output"); //$NON-NLS-1$
					}
					if (itemDefinition!=null) {
						element.setItemSubjectRef(itemDefinition);
					}
					parameters.add(element);
				}
				else {
					element = parameters.get(index);
				}
				if (isInput) {
					for (DataAssociation a : associations) {
						if (a.getTargetRef() == element) {
							association = a;
							break;
						}
					}
					if (association==null) {
						association = Bpmn2ModelerFactory.createObject(resource, DataInputAssociation.class);
						association.setTargetRef(element);
						associations.add(association);
					}
				}
				else {
					for (DataAssociation a : associations) {
						if (a.getSourceRef().contains(element)) {
							association = a;
							break;
						}
					}
					if (association==null) {
						association = Bpmn2ModelerFactory.createObject(resource, DataOutputAssociation.class);
						if (element!=null)
							association.getSourceRef().add(element);
						associations.add(association);
					}
				}
				if (ioSet==null) {
					if (isInput) {
						ioSet = (BaseElement) Bpmn2ModelerFactory.createObject(resource, InputSet.class);
						throwEvent.setInputSet((InputSet)ioSet);
					}
					else {
						ioSet = (BaseElement) Bpmn2ModelerFactory.createObject(resource, OutputSet.class);
						catchEvent.setOutputSet((OutputSet)ioSet);
					}
				}
				if (isInput) {
					if (!((InputSet)ioSet).getDataInputRefs().contains(element)) {
						((InputSet)ioSet).getDataInputRefs().add((DataInput)element);
					}
				}
				else {
					if (!((OutputSet)ioSet).getDataOutputRefs().contains(element)) {
						((OutputSet)ioSet).getDataOutputRefs().add((DataOutput)element);
					}
				}
			}
			if (ed==eventDefinition)
				break;
		}
		return new Tuple(element,association);
	}

	public static boolean hasItemDefinition(EventDefinition eventDefinition) {
		return (eventDefinition instanceof ErrorEventDefinition ||
			eventDefinition instanceof EscalationEventDefinition ||
			eventDefinition instanceof SignalEventDefinition ||
			eventDefinition instanceof MessageEventDefinition);
	}

	public static ItemDefinition getItemDefinition(EventDefinition eventDefinition) {
		ItemDefinition itemDefinition = null;
		if (eventDefinition instanceof ErrorEventDefinition) {
			Error payloadContainer = ((ErrorEventDefinition)eventDefinition).getErrorRef();
			itemDefinition = payloadContainer==null ? null : payloadContainer.getStructureRef();
		}
		if (eventDefinition instanceof EscalationEventDefinition) {
			Escalation payloadContainer = ((EscalationEventDefinition)eventDefinition).getEscalationRef();
			itemDefinition = payloadContainer==null ? null : payloadContainer.getStructureRef();
		}
		if (eventDefinition instanceof SignalEventDefinition) {
			Signal payloadContainer = ((SignalEventDefinition)eventDefinition).getSignalRef();
			itemDefinition = payloadContainer==null ? null : payloadContainer.getStructureRef();
		}
		if (eventDefinition instanceof MessageEventDefinition) {
			Message payloadContainer = ((MessageEventDefinition)eventDefinition).getMessageRef();
			itemDefinition = payloadContainer==null ? null : payloadContainer.getItemRef();
		}
		return itemDefinition;
	}

	public static void setItemDefinition(EventDefinition eventDefinition, ItemDefinition itemDefinition) {
		if (eventDefinition instanceof ErrorEventDefinition) {
			Error payloadContainer = ((ErrorEventDefinition)eventDefinition).getErrorRef();
			payloadContainer.setStructureRef(itemDefinition);
		}
		if (eventDefinition instanceof EscalationEventDefinition) {
			Escalation payloadContainer = ((EscalationEventDefinition)eventDefinition).getEscalationRef();
			payloadContainer.setStructureRef(itemDefinition);
		}
		if (eventDefinition instanceof SignalEventDefinition) {
			Signal payloadContainer = ((SignalEventDefinition)eventDefinition).getSignalRef();
			payloadContainer.setStructureRef(itemDefinition);
		}
		if (eventDefinition instanceof MessageEventDefinition) {
			Message payloadContainer = ((MessageEventDefinition)eventDefinition).getMessageRef();
			payloadContainer.setItemRef(itemDefinition);
		}
	}

	public static EventDefinition getEventDefinition(ItemAwareElement element) {
		Event event = null;
		List<EventDefinition> eventDefinitions = null;
		if (element.eContainer() instanceof CatchEvent) {
			event = (Event) element.eContainer();
			eventDefinitions = ((CatchEvent)event).getEventDefinitions();
		}
		else if (element.eContainer() instanceof ThrowEvent) {
			event = (Event) element.eContainer();
			eventDefinitions = ((ThrowEvent)event).getEventDefinitions();
		}
		if (eventDefinitions!=null) {
			for (EventDefinition ed : eventDefinitions) {
				Tuple<ItemAwareElement, DataAssociation> param = getIOParameter(event, ed);
				if (param.getFirst()==element) {
					return ed;
				}
			}
		}
		return null;
	}

	public static RootElement getEventDefinitionTarget(EventDefinition eventDefinition) {
		if (eventDefinition instanceof ErrorEventDefinition) {
			return ((ErrorEventDefinition)eventDefinition).getErrorRef();
		}
		if (eventDefinition instanceof EscalationEventDefinition) {
			return ((EscalationEventDefinition)eventDefinition).getEscalationRef();
		}
		if (eventDefinition instanceof SignalEventDefinition) {
			return ((SignalEventDefinition)eventDefinition).getSignalRef();
		}
		if (eventDefinition instanceof MessageEventDefinition) {
			return ((MessageEventDefinition)eventDefinition).getMessageRef();
		}
		return null;
	}

}
