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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.util.messages"; //$NON-NLS-1$
	public static String JbpmImportDialog_Create_Process_Variables_Label;
	public static String JbpmInterfaceImportDialog_Available_Methods;
	public static String JbpmInterfaceImportDialog_Select_All;
	public static String JbpmInterfaceImportDialog_Select_None;
	public static String JbpmModelUtil_Duplicate_Import_Message;
	public static String JbpmModelUtil_Duplicate_Import_Title;
	public static String JbpmModelUtil_No_Process_Message;
	public static String JbpmModelUtil_No_Process_Title;
	public static String JbpmModelUtil_Scenario_Name;
	public static String JbpmModelUtil_Simulation;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
