/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.examples.customtask;

import org.eclipse.bpmn2.modeler.core.features.CustomShapeFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.IShapeFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.event.definitions.AbstractCreateEventDefinitionFeature;
import org.eclipse.bpmn2.modeler.core.features.event.definitions.AbstractEventDefinitionFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.event.definitions.DecorationAlgorithm;
import org.eclipse.bpmn2.modeler.core.runtime.CustomTaskImageProvider;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.examples.customtask.MyModel.MyEventDefinition;
import org.eclipse.bpmn2.modeler.examples.customtask.MyModel.MyModelPackage;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.mm.algorithms.Image;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

public class MyEventDefinitionFeatureContainer extends CustomShapeFeatureContainer {

	// this value must match what's in the plugin.xml
	private final static String CUSTOM_TASK_ID = "org.eclipse.bpmn2.modeler.examples.customtask.eventDefinition1";

	public MyEventDefinitionFeatureContainer() {
	}

	@Override
	public String getId(EObject object) {
		// This is where we inspect the object to determine what its custom task ID should be.
		// In this case, the object will be a MyEventDefinition instance.
		if (object instanceof MyEventDefinition)
			return CUSTOM_TASK_ID;
			
		return null;
	}
	
	@Override
	public Object getApplyObject(IContext context) {
		if (context instanceof IPictogramElementContext) {
			PictogramElement pe = ((IPictogramElementContext)context).getPictogramElement();
			EObject bo = BusinessObjectUtil.getBusinessObjectForPictogramElement(pe);
			return bo instanceof MyEventDefinition ? bo : null;
		}
		return null;
	}
	
	protected IShapeFeatureContainer createFeatureContainer(IFeatureProvider fp) {
		return new AbstractEventDefinitionFeatureContainer() {

			@Override
			public ICreateFeature getCreateFeature(IFeatureProvider fp) {
				return new CreateMyEventDefinitionFeature(fp);
			}

			@Override
			public IAddFeature getAddFeature(IFeatureProvider fp) {
				return new AddEventDefinitionFeature(fp) {

					@Override
					public boolean canAdd(IAddContext context) {
						return true;
					}
				};
			}

			@Override
			protected Shape drawForStart(DecorationAlgorithm algorithm, ContainerShape shape) {
				return draw(algorithm,shape);
			}

			@Override
			protected Shape drawForEnd(DecorationAlgorithm algorithm, ContainerShape shape) {
				return draw(algorithm,shape);
			}

			@Override
			protected Shape drawForThrow(DecorationAlgorithm algorithm, ContainerShape shape) {
				return draw(algorithm,shape);
			}

			@Override
			protected Shape drawForCatch(DecorationAlgorithm algorithm, ContainerShape shape) {
				return draw(algorithm,shape);
			}

			@Override
			protected Shape drawForBoundary(DecorationAlgorithm algorithm, ContainerShape shape) {
				return draw(algorithm,shape);
			}

			private Shape draw(DecorationAlgorithm algorithm, ContainerShape shape) {
				Shape iconShape = Graphiti.getPeService().createShape(shape, false);
				Image img = CustomTaskImageProvider.createImage(customTaskDescriptor, iconShape, customTaskDescriptor.getIcon(), 30, 30);
				Graphiti.getGaService().setLocationAndSize(img, 3, 3, 30, 30);
				return iconShape;
			}
			
		};
	}
	
	public class CreateMyEventDefinitionFeature extends AbstractCreateEventDefinitionFeature<MyEventDefinition> {

		public CreateMyEventDefinitionFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean canCreate(ICreateContext context) {
			return true;
		}
		
		@Override
		public EClass getBusinessObjectClass() {
			return MyModelPackage.eINSTANCE.getMyEventDefinition();
		}

		@Override
		protected String getStencilImageId() {
			return null;
		}
		
	}
}
