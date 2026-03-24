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

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.Group;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;

public enum AnchorType {
	ACTIVITY("activity"), POOL("pool"), GATEWAY("gateway"), MESSAGELINK("messagelink"), CONNECTION("connection"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	
	private final String key;
	
	private AnchorType(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public static AnchorType getType(String key) {
		for (AnchorType t : values()) {
			if (t.getKey().equals(key)) {
				return t;
			}
		}
		return null;
	}
	
	public static AnchorType getType(AnchorContainer ac) {
		if (ac instanceof ConnectionDecorator)
			return CONNECTION;
		
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(ac);
		String property = FeatureSupport.getPropertyValue(ac, GraphitiConstants.MESSAGE_LINK);
		if (Boolean.parseBoolean(property)) {
			return MESSAGELINK;
		}
		else if (be instanceof Participant) {
			return POOL;
		}
		else if (be instanceof Group) {
			return POOL;
		}
		else if (be instanceof Gateway || be instanceof Event) {
			return GATEWAY;
		}
		else if (be instanceof SequenceFlow) {
			return CONNECTION;
		}
		else if (be != null) {
			return ACTIVITY;
		}
		throw new IllegalArgumentException("Cannot determine Anchor Type for business object "+be); //$NON-NLS-1$
	}
	
	public static AnchorType getType(Anchor anchor) {
		return getType(FeatureSupport.getPropertyValue(anchor, GraphitiConstants.ANCHOR_TYPE));
	}
	
	public static void setType(Anchor anchor, AnchorType at) {
		FeatureSupport.setPropertyValue(anchor, GraphitiConstants.ANCHOR_TYPE, at.getKey());
	}
}