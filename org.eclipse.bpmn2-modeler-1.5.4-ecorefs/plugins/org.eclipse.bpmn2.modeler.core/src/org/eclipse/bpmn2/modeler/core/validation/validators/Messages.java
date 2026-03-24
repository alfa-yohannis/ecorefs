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

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.core.validation.validators.messages"; //$NON-NLS-1$
	public static String AbstractBpmn2ElementValidator_Object_Missing_Connections;
	public static String AbstractBpmn2ElementValidator_Object_Missing_Feature;
	public static String AssociationValidator_No_Source;
	public static String AssociationValidator_No_Target;
	public static String BaseElementValidator_Duplicate_IDs;
	public static String BaseElementValidator_ID_Is_Empty;
	public static String BaseElementValidator_ID_Is_Invalid;
	public static String ChoreographyActivityValidator_Need_Two_Participants;
	public static String ChoreographyActivityValidator_No_Initiating_Participant;
	public static String ChoreographyTaskValidator_Source_Not_Participant;
	public static String ChoreographyTaskValidator_Target_Not_Participant;
	public static String DataAssociationValidator_Input_Uninitialized;
	public static String DataAssociationValidator_Output_Uninitialized;
	public static String DefinitionsValidator_No_TargetNamespace;
	public static String EventDefinitionValidator_CalledActivity;
	public static String EventDefinitionValidator_Condition;
	public static String EventDefinitionValidator_Error;
	public static String EventDefinitionValidator_Escalation;
	public static String EventDefinitionValidator_Message;
	public static String EventDefinitionValidator_No_Source_DataItem;
	public static String EventDefinitionValidator_No_Target_DataItem;
	public static String EventDefinitionValidator_Signal;
	public static String EventDefinitionValidator_Timer;
	public static String ExpressionValidator_Empty;
	public static String FlowNodeValidator_EventSubProcess_Has_Incoming;
	public static String FlowNodeValidator_EventSubProcess_Has_Outgoing;
	public static String FlowNodeValidator_Only_One_Incoming_Allowed;
	public static String FlowNodeValidator_Only_One_Outgoing_Allowed;
	public static String GatewayValidator_Complex_Converging_Diverging;
	public static String GatewayValidator_Converging_Multiple_Incoming;
	public static String GatewayValidator_Converging_One_Outgoing;
	public static String GatewayValidator_Diverging_Multiple_Outgoing;
	public static String GatewayValidator_Diverging_One_Incoming;
	public static String GatewayValidator_Event_Diverging;
	public static String GatewayValidator_Exclusive_Converging_Diverging;
	public static String GatewayValidator_Inclusive_Converging_Diverging;
	public static String GatewayValidator_Mixed_Multiple_Incoming;
	public static String GatewayValidator_Mixed_Multiple_Outgoing;
	public static String GatewayValidator_Parallel_Converging_Diverging;
	public static String GatewayValidator_Unspecified_Multiple_Incoming_Outgoing;
	public static String MultiInstanceLoopCharacteristicsValidator_Both_Condition_And_Inputs;
	public static String MultiInstanceLoopCharacteristicsValidator_No_Condition_Or_Inputs;
	public static String MultiInstanceLoopCharacteristicsValidator_No_Input_Instance_Parameter;
	public static String MultiInstanceLoopCharacteristicsValidator_No_Output_Instance_Parameter;
	public static String ProcessValidator_No_EndEvent;
	public static String ProcessValidator_No_Name;
	public static String ProcessValidator_No_StartEvent;
	public static String ProcessValidator_Multiple_UntriggeredStartEvents;
	public static String SequenceFlowValidator_SequenceFlow_No_Source;
	public static String SequenceFlowValidator_SequenceFlow_No_Target;
	public static String StandardLoopCharacteristicsValidator_No_Loop_Condition;
	public static String SubProcessValidator_EventSubProcess_Multiple_Start;
	public static String SubProcessValidator_EventSubProcess_No_Event;
	public static String SubProcessValidator_EventSubProcess_No_Start;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
