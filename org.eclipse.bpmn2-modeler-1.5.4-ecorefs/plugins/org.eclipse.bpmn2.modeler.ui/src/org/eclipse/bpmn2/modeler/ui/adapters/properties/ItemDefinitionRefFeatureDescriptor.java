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

package org.eclipse.bpmn2.modeler.ui.adapters.properties;

import java.util.Hashtable;
import java.util.Stack;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.ItemKind;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.utils.EventDefinitionsUtil;
import org.eclipse.bpmn2.modeler.core.utils.ImportUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.Tuple;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * @author Bob Brodt
 *
 */
public class ItemDefinitionRefFeatureDescriptor<T extends BaseElement> extends FeatureDescriptor<T> {

	protected ImportUtil importer = new ImportUtil();
	
	/**
	 * @param adapterFactory
	 * @param object
	 * @param feature
	 */
	public ItemDefinitionRefFeatureDescriptor(ExtendedPropertiesAdapter<T> owner, T object, EStructuralFeature feature) {
		super(owner, object, feature);
		// I found a couple of instances where this class was used for references that were NOT
		// RootElements - just check to make sure here...
		Assert.isTrue( RootElement.class.isAssignableFrom(feature.getEType().getInstanceClass()) );
	}
	
	@Override
	public String getLabel() {
		return ItemDefinitionPropertiesAdapter.getLabel();
	}

	@Override
	public String getTextValue() {
		ItemDefinition itemDefinition = (ItemDefinition) object.eGet(feature);
		return ItemDefinitionPropertiesAdapter.getDisplayName(itemDefinition);
	}
	
	@Override
	public EObject createFeature(Resource resource, EClass eClass) {
		ItemDefinition itemDefinition = ItemDefinitionPropertiesAdapter.createItemDefinition(object.eResource());
		return itemDefinition;
	}

	@Override
	public Object getValue() {
		ItemDefinition itemDefinition = (ItemDefinition) object.eGet(feature);
		return ItemDefinitionPropertiesAdapter.getStructureRef(itemDefinition);
	}

	@Override
	protected void internalSet(T object, EStructuralFeature feature, Object value, int index) {
		Definitions definitions = ModelUtil.getDefinitions(object);
		if (value instanceof String) {
			value = importer.createItemDefinition(definitions, null, (String)value, ItemKind.INFORMATION);
		}
		
		if (value==null || value instanceof ItemDefinition) {
			ItemDefinition itemDefinition = (ItemDefinition) value;

			super.internalSet(object, feature, itemDefinition, index);
			
			// if there are any DataInputAssociations or DataOutputAssociations that map to this object
			// then change their ItemDefinitions to match.
			if (definitions!=null) {
				// We use a stack to track the ItemAwareElements that were changed as
				// a result of this object's change.
				if (object instanceof ItemAwareElement) {
					changeReferences((ItemAwareElement)object, itemDefinition);
				}
				else if (object instanceof RootElement) {
					// find all references to this root element:
					// for event definitions, correlate the input/output association with the event definition
					// that references this object, and change the ItemDefinition of that data input/output.
					changeReferences((RootElement)object, itemDefinition);
				}
			}
		}
	}
	
	protected void changeReferences(RootElement object, ItemDefinition itemDefinition) {
		for (EObject ed : ModelUtil.getAllReachableObjects(object, Bpmn2Package.eINSTANCE.getEventDefinition())) {
			if (EventDefinitionsUtil.getEventDefinitionTarget((EventDefinition)ed) == object) {
				Tuple<ItemAwareElement, DataAssociation> param =
						EventDefinitionsUtil.getIOParameter((Event)ed.eContainer(), (EventDefinition)ed);
				changeReferences(param.getFirst(), itemDefinition);
			}
		}
	}
	
	protected void changeReferences(ItemAwareElement object, ItemDefinition itemDefinition) {
		Definitions definitions = ModelUtil.getDefinitions(object);
		Stack<ItemAwareElement> changedObjects = new Stack<ItemAwareElement>();
		changedObjects.push((ItemAwareElement)object);
		while (!changedObjects.isEmpty()) {
			ItemAwareElement element = changedObjects.pop();
			if (element.eContainer() instanceof Event) {
				EventDefinition ed = EventDefinitionsUtil.getEventDefinition(element);
				if (ed!=null) {
					ItemDefinition id = EventDefinitionsUtil.getItemDefinition(ed);
					if (id!=itemDefinition) {
						EventDefinitionsUtil.setItemDefinition(ed,itemDefinition);
						RootElement re = EventDefinitionsUtil.getEventDefinitionTarget(ed);
						changeReferences(re,itemDefinition);
					}
				}
			}
			
			if (element.getItemSubjectRef()!=itemDefinition) {
				element.setItemSubjectRef(itemDefinition);
			}
			
			TreeIterator<EObject> iter = definitions.eAllContents();
			while (iter.hasNext()) {
				EObject o = iter.next();
				if (o instanceof DataInputAssociation) {
					DataInputAssociation da = (DataInputAssociation) o;
					if (da.getSourceRef().contains(element)) {
						if (da.getTargetRef()!=null) {
							if (da.getTargetRef().getItemSubjectRef()!=itemDefinition) {
								da.getTargetRef().setItemSubjectRef(itemDefinition);
								changedObjects.push(da.getTargetRef());
							}
						}
					}
					else if (da.getTargetRef()==element) {
						for (ItemAwareElement e : da.getSourceRef()) {
							if (e.getItemSubjectRef()!=itemDefinition) {
								e.setItemSubjectRef(itemDefinition);
								changedObjects.push(e);
							}
						}
					}
				}
				else if (o instanceof DataOutputAssociation) {
					DataOutputAssociation da = (DataOutputAssociation) o;
					if (da.getSourceRef().contains(element)) {
						if (da.getTargetRef()!=null) {
							if (da.getTargetRef().getItemSubjectRef()!=itemDefinition) {
								da.getTargetRef().setItemSubjectRef(itemDefinition);
								changedObjects.push(da.getTargetRef());
							}
						}
					}
					else if (da.getTargetRef()==element) {
						for (ItemAwareElement e : da.getSourceRef()) {
							if (e.getItemSubjectRef()!=itemDefinition) {
								e.setItemSubjectRef(itemDefinition);
								changedObjects.push(e);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public Hashtable<String, Object> getChoiceOfValues() {
		return ItemDefinitionPropertiesAdapter.getChoiceOfValues(object);
	}
}
