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
 * @author Ivar Meikas
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features.artifact;

import org.eclipse.bpmn2.TextAnnotation;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2DirectEditingFeature;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

public class DirectEditTextAnnotationFeature extends AbstractBpmn2DirectEditingFeature {

	public DirectEditTextAnnotationFeature(IFeatureProvider fp) {
	    super(fp);
    }

	@Override
    public String getInitialValue(IDirectEditingContext context) {
		PictogramElement pe = context.getPictogramElement();
		TextAnnotation annotation = (TextAnnotation) getBusinessObjectForPictogramElement(pe);
		String text = annotation.getText();
		return text==null ? "" : text; //$NON-NLS-1$
	}

	@Override
    public void setValue(String value, IDirectEditingContext context) {
		PictogramElement pe = context.getPictogramElement();
		TextAnnotation annotation = (TextAnnotation) getBusinessObjectForPictogramElement(pe);
		annotation.setText(value);
		updatePictogramElement(((Shape) pe).getContainer());
    }
	
	@Override
	public boolean canDirectEdit(IDirectEditingContext context) {
		return BusinessObjectUtil.getBusinessObject(context, TextAnnotation.class) != null;
	}
}
