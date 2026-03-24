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
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.features.activity.subprocess;

import org.eclipse.bpmn2.modeler.core.features.activity.LayoutActivityFeature;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;

public class LayoutExpandableActivityFeature extends LayoutActivityFeature {

	public LayoutExpandableActivityFeature(IFeatureProvider fp) {
		super(fp);
	}
	
	@Override
	public boolean layout(ILayoutContext context) {
		ContainerShape containerShape = (ContainerShape) context.getPictogramElement();
		try {
			boolean setChildrenVisible = FeatureSupport.isElementExpanded(containerShape);
			FeatureSupport.setContainerChildrenVisible(getFeatureProvider(), containerShape, setChildrenVisible);
		} catch (Exception e) {
			// It's OK, I've played a programmer before...
			// e.printStackTrace();
		}
		
		return super.layout(context);
	}
}
