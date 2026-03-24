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
import org.eclipse.bpmn2.DataInput;
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
public class JbpmServiceTaskDetailComposite extends JbpmTaskDetailComposite {

	public final static String INPUT_NAME = "Parameter"; //$NON-NLS-1$
	public final static String OUTPUT_NAME = "Result"; //$NON-NLS-1$
	
	public JbpmServiceTaskDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	public JbpmServiceTaskDetailComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	@Override
	protected DataInput getDefaultDataInput(Activity activity) {
		InputOutputSpecification ioSpec = activity.getIoSpecification();
		if (ioSpec!=null) {
			for (DataInput din : ioSpec.getDataInputs()) {
				if (INPUT_NAME.equals(din.getName()))
					return din;
			}
		}
		return null;
	}
	
	@Override
	protected DataInput createDefaultDataInput(Activity activity) {
		DataInput input = super.createDefaultDataInput(activity);
		input.setName(INPUT_NAME);
		return input;
	}
	
	@Override
	protected DataOutput getDefaultDataOutput(Activity activity) {
		InputOutputSpecification ioSpec = activity.getIoSpecification();
		if (ioSpec!=null) {
			for (DataOutput dout : ioSpec.getDataOutputs()) {
				if (OUTPUT_NAME.equals(dout.getName()))
					return dout;
			}
		}
		return null;
	}
	
	@Override
	protected DataOutput createDefaultDataOutput(Activity activity) {
		DataOutput output = super.createDefaultDataOutput(activity);
		output.setName(OUTPUT_NAME);
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
