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
package org.eclipse.bpmn2.modeler.ui.features.event;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.ui.features.event.messages"; //$NON-NLS-1$
	public static String FollowLinkFeature_Create_Link_Description;
	public static String FollowLinkFeature_name;
	public static String FollowLinkFeature_Show_Source_Links_Description;
	public static String FollowLinkFeature_Show_Target_Link_Description;
	public static String AppendEventFeature_Description;
	public static String AppendEventFeature_Name;
	public static String StartEventFeatureContainer_Is_Interrupting_Changed;
	public static String UpdateBoundaryEventFeature_Description_Changed;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
