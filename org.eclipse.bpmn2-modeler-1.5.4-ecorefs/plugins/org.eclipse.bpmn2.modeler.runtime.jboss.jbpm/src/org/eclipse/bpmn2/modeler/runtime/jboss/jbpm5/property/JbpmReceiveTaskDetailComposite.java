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

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.Operation;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.ui.property.tasks.DataAssociationDetailComposite.MapType;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Bob Brodt
 *
 */
public class JbpmReceiveTaskDetailComposite extends JbpmTaskDetailComposite {

	public final static String MESSAGE_NAME = "Message"; //$NON-NLS-1$
//	public final static String MESSAGEID_NAME = "MessageId"; //$NON-NLS-1$

	/**
	 * @param section
	 */
	public JbpmReceiveTaskDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	public JbpmReceiveTaskDetailComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	@Override
	protected DataOutput getDefaultDataOutput(Activity activity) {
		InputOutputSpecification ioSpec = activity.getIoSpecification();
		if (ioSpec!=null) {
			for (DataOutput dout : ioSpec.getDataOutputs()) {
				if (MESSAGE_NAME.equals(dout.getName()))
					return dout;
			}
		}
		return null;
	}
	
	@Override
	protected DataOutput createDefaultDataOutput(Activity activity) {
		DataOutput output = super.createDefaultDataOutput(activity);
		output.setName(MESSAGE_NAME);
		return output;
	}
	
	@Override
	protected void createMessageAssociations(Composite container, Activity activity,
			EReference operationRef, Operation operation,
			EReference messageRef, Message message) {

		super.createMessageAssociations(container, activity,
				operationRef, operation,
				messageRef, message);
		
		outputComposite.setAllowedMapTypes(MapType.Property.getValue());
		inputComposite.setAllowedMapTypes(MapType.Property.getValue() | MapType.SingleAssignment.getValue());
	}
}
