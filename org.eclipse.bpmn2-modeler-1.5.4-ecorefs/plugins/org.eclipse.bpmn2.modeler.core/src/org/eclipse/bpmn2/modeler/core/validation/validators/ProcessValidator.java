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

package org.eclipse.bpmn2.modeler.core.validation.validators;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.IValidationContext;

/**
 *
 */
public class ProcessValidator extends AbstractBpmn2ElementValidator<Process> {

	/**
	 * @param ctx
	 */
	public ProcessValidator(IValidationContext ctx) {
		super(ctx);
	}

	/**
	 * @param other
	 */
	@SuppressWarnings("rawtypes")
	public ProcessValidator(AbstractBpmn2ElementValidator other) {
		super(other);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(Process process) {
		boolean foundStartEvent = false;
		List<StartEvent> untriggeredStartEvents = new ArrayList<StartEvent>();
		boolean foundEndEvent = false;
		List<FlowElement> flowElements = process.getFlowElements();
		for (FlowElement fe : flowElements) {
			if (fe instanceof StartEvent) {
				foundStartEvent = true;
				if (((StartEvent)fe).getEventDefinitions().size() == 0)
					untriggeredStartEvents.add((StartEvent)fe);
			}
			if (fe instanceof EndEvent) {
				foundEndEvent = true;
			}
		}
		if (!foundStartEvent) {
			addStatus(process, Status.WARNING, Messages.ProcessValidator_No_StartEvent);
		}
		if (untriggeredStartEvents.size()>1) {
			addStatus(process,
					untriggeredStartEvents.toArray(new StartEvent[untriggeredStartEvents.size()]),
					Status.ERROR, Messages.ProcessValidator_Multiple_UntriggeredStartEvents);
		}
		if (!foundEndEvent) {
			addStatus(process, Status.WARNING, Messages.ProcessValidator_No_EndEvent);
		}
		
		if (isEmpty(process.getName())) {
			addStatus(process, "name", Status.WARNING, Messages.ProcessValidator_No_Name, process.getId()); //$NON-NLS-1$
		}
		
		return getResult();
	}
	
	@Override
	public boolean checkSuperType(EClass eClass, Process object) {
		if ("FlowElementsContainer".equals(eClass.getName())) //$NON-NLS-1$
			return true;
		if ("BaseElement".equals(eClass.getName())) //$NON-NLS-1$
			return true;
		return false;
	}
	
	public static boolean isContainingProcessExecutable(EObject object) {
		Process process = null;
		if (object instanceof Process)
			process = (Process) object;
		else if (object instanceof RootElement) {
			Definitions definitions = ModelUtil.getDefinitions(object);
			if (definitions!=null) {
				// return the first Process element found
				for (RootElement re : definitions.getRootElements()) {
					if (re instanceof Process) {
						process = (Process) re;
						break;
					}
				}
			}
		}
		else
			process = (Process) ModelUtil.findNearestAncestor(object, new Class[] { Process.class });
		return process!=null && process.isIsExecutable();
	}
}
