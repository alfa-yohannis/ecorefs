/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.messages"; //$NON-NLS-1$
	public static String CallActivityConstraint_Process_ID_Not_Found;
	public static String CallActivityConstraint_Not_A_Process_ID;
	public static String CallActivityConstraint_Not_A_Process_Variable;
	public static String CallActivityConstraint_Independent_And_WaitForCompletion_False;
	public static String ProcessConstraint_No_Package_Name;
	public static String ProcessConstraint_No_Process_Name;
	public static String ProcessValidator_Invalid_PackageName;
	public static String ProcessValidator_No_End;
	public static String ProcessValidator_No_Name;
	public static String ProcessValidator_No_Start;
	public static String ProcessVariableNameConstraint_Duplicate_ID;
	public static String ProcessVariableNameValidator_ID_Empty;
	public static String ProcessVariableNameValidator_ID_Invalid;
	public static String UserTaskConstraint_No_Form;
	public static String UserTaskConstraint_No_Name;
	public static String InterfaceValidator_Implementation_Invalid;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
