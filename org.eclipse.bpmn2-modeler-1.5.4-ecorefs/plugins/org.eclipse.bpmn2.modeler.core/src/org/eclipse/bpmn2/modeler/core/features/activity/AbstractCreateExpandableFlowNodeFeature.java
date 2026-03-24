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
package org.eclipse.bpmn2.modeler.core.features.activity;

import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.AbstractCreateFlowElementFeature;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractCreateExpandableFlowNodeFeature.
 *
 * @param <T> the generic type
 */
public abstract class AbstractCreateExpandableFlowNodeFeature<T extends FlowNode> extends
		AbstractCreateFlowElementFeature<T> {

	/**
	 * Instantiates a new abstract create expandable flow node feature.
	 *
	 * @param fp the Feature Provider
	 */
	public AbstractCreateExpandableFlowNodeFeature(IFeatureProvider fp) {
		super(fp);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.AbstractCreateFlowElementFeature#create(org.eclipse.graphiti.features.context.ICreateContext)
	 */
	@Override
	public Object[] create(ICreateContext context) {
		Object[] elems = super.create(context);
		try {
			
			BPMNShape shape = DIUtils.findBPMNShape((T)elems[0]);
			// if the Activity is expandable, set "isExpanded" to true because
			// this feature will always create an expanded BPMNShape.
			EStructuralFeature feature = ((EObject)shape).eClass().getEStructuralFeature("isExpanded"); //$NON-NLS-1$
			if (feature!=null) {
				shape.eSet(feature, Boolean.TRUE);
				IUpdateContext updateContext = new UpdateContext((PictogramElement) elems[1]);
				getFeatureProvider().updateIfPossible(updateContext);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return elems;
	}

}
