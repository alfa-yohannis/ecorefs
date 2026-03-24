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
package org.eclipse.bpmn2.modeler.core.validation;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.core.validation.messages"; //$NON-NLS-1$
	public static String BPMN2ProjectValidator_No_BPMN2_Project_Nature;
	public static String BPMN2ProjectValidator_No_WST_Project_Builder;
	public static String BPMN2ProjectValidator_Dont_Ask_Again;
	public static String BPMN2ProjectValidator_Invalid_File;
	public static String BPMN2ProjectValidator_Title;
	public static String ValidationStatusAdapter_Multiple_Problems_Found;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
