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
package org.eclipse.bpmn2.modeler.ui.features.choreography;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.ChoreographyActivity;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.SubChoreography;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.features.MultiUpdateFeature;
import org.eclipse.bpmn2.modeler.core.utils.ShapeDecoratorUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;

public class SubChoreographyFeatureContainer extends AbstractChoreographyFeatureContainer {

	@Override
	public boolean canApplyTo(Object o) {
		return super.canApplyTo(o) &&
				(o instanceof SubChoreography || o instanceof Participant);
	}

	@Override
	public ICreateFeature getCreateFeature(IFeatureProvider fp) {
		return new CreateSubChoreographyFeature(fp);
	}

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return new AddSubChoreographyFeature(fp);
	}

	@Override
	public MultiUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		MultiUpdateFeature multiUpdate = super.getUpdateFeature(fp);
		multiUpdate.addFeature(new UpdateChoreographyMarkerFeature(fp) {
			@Override
			protected void doUpdate(ChoreographyActivity element, ContainerShape markerContainer) {
				super.doUpdate(element, markerContainer);
				// SubChoreography always has an EXPAND marker
				ShapeDecoratorUtil.showActivityMarker(markerContainer, GraphitiConstants.ACTIVITY_MARKER_EXPAND);
			}
		});
		
		return multiUpdate;
	}

	@Override
	public ICustomFeature[] getCustomFeatures(IFeatureProvider fp) {
		return super.getCustomFeatures(fp);
	
// FIXME: a SubChoreography should allow pushdown to a new Choreography top-level diagram
//		ICustomFeature[] superFeatures = super.getCustomFeatures(fp);
//		ICustomFeature[] thisFeatures = new ICustomFeature[2 + superFeatures.length];
//		thisFeatures[0] = new PushdownFeature(fp);
//		thisFeatures[1] = new PullupFeature(fp);
//		for (int i=0; i<superFeatures.length; ++i)
//			thisFeatures[2+i] = superFeatures[i];
//		return thisFeatures;
	}

	public static class CreateSubChoreographyFeature extends AbstractCreateChoreographyActivityFeature<SubChoreography> {

		public CreateSubChoreographyFeature(IFeatureProvider fp) {
			super(fp);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.bpmn2.modeler.core.features.AbstractCreateFlowElementFeature#getStencilImageId()
		 */
		@Override
		public String getStencilImageId() {
			return ImageProvider.IMG_16_SUB_CHOREOGRAPHY;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.bpmn2.modeler.core.features.AbstractCreateFlowElementFeature#getFlowElementClass()
		 */
		@Override
		public EClass getBusinessObjectClass() {
			return Bpmn2Package.eINSTANCE.getSubChoreography();
		}
	}
}