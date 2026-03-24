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

package org.eclipse.bpmn2.modeler.ui.features.callactivity;

import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.GlobalTask;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.bpmn2.modeler.ui.features.callactivity.CallActivityFeatureContainer.CreateCallActivityFeature;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractCreateCallGlobalTaskFeature<T extends GlobalTask> extends CreateCallActivityFeature {

	/**
	 * @param fp
	 */
	public AbstractCreateCallGlobalTaskFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public CallActivity createBusinessObject(ICreateContext context) {
		CallActivity callActivity = super.createBusinessObject(context);
		Definitions definitions = ModelUtil.getDefinitions(callActivity );
		T task = (T) Bpmn2ModelerFactory.create(definitions.eResource(), getFeatureClass());
		definitions.getRootElements().add(task);
		ModelUtil.setID(task);
		task.setName( ModelUtil.toCanonicalString(task.getId()));
		callActivity.setCalledElementRef(task);
		callActivity.setName(NLS.bind(Messages.AbstractCreateCallGlobalTaskFeature_Name_Label, task.getName()));
		
		return callActivity;
	}

	@Override
	public String getStencilImageId() {
		return ImageProvider.IMG_16_CALL_ACTIVITY;
	}

	public abstract EClass getFeatureClass();
}