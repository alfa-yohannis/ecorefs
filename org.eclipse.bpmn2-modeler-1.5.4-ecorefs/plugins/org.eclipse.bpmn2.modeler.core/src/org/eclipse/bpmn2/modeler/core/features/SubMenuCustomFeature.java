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

package org.eclipse.bpmn2.modeler.core.features;

import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;

public class SubMenuCustomFeature extends AbstractCustomFeature {

	AbstractCustomFeature customFeature;
	ICreateFeature feature;
	/**
	 * @param fp
	 */
	public SubMenuCustomFeature(AbstractCustomFeature customFeature, ICreateFeature feature) {
		super(customFeature.getFeatureProvider());
		this.customFeature = customFeature;
		this.feature = feature;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.custom.ICustomFeature#execute(org.eclipse.graphiti.features.context.ICustomContext)
	 */
	@Override
	public void execute(ICustomContext context) {
		context.putProperty(GraphitiConstants.CREATE_FEATURE, feature); //$NON-NLS-1$
		customFeature.execute(context);
	}

	public boolean canExecute(ICustomContext context) {
		return customFeature.canExecute(context);
	}

	@Override
	public String getDescription() {
		return feature.getDescription();
	}

	@Override
	public String getName() {
		return customFeature.getName() + "/" + feature.getName(); //$NON-NLS-1$
	}
}