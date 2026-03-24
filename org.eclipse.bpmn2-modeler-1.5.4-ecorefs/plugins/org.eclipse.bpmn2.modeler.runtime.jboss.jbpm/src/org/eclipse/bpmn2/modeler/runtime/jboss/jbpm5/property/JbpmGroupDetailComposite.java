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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Category;
import org.eclipse.bpmn2.CategoryValue;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Group;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.InsertionAdapter;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.TextObjectEditor;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.swt.widgets.Composite;

public class JbpmGroupDetailComposite extends DefaultDetailComposite {

	public JbpmGroupDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	public JbpmGroupDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void createBindings(EObject be) {
		getAttributesParent();

		Group group = (Group) be;
		Resource resource = ExtendedPropertiesAdapter.getResource(group);
		Category category = null;
		CategoryValue categoryValue = group.getCategoryValueRef();
		if (categoryValue==null) {
			Definitions definitions = ModelUtil.getDefinitions(group);
			for (Category c : ModelUtil.getAllRootElements(definitions, Category.class)) {
				if ("default".equals(c.getName())) {
					category = c;
					break;
				}
			}
			categoryValue = (CategoryValue) Bpmn2ModelerFactory.create(resource, CategoryValue.class);
			if (category==null) {
				category = Bpmn2ModelerFactory.create(resource, Category.class);
				category.setName("default");
				category.getCategoryValue().add(categoryValue);
				InsertionAdapter.add(definitions, Bpmn2Package.eINSTANCE.getDefinitions_RootElements(), category);
			}
			else {
				InsertionAdapter.add(category, Bpmn2Package.eINSTANCE.getCategory_CategoryValue(), categoryValue);
			}
			InsertionAdapter.add(group, Bpmn2Package.eINSTANCE.getGroup_CategoryValueRef(), categoryValue);
		}
		else {
			category = (Category) categoryValue.eContainer();
			// TODO: should we make sure the Category name is "default"? Does it matter?
		}
		
		ObjectEditor editor = new TextObjectEditor(this,categoryValue,Bpmn2Package.eINSTANCE.getCategoryValue_Value());
		editor.createControl(this,"Name");
	}
}
