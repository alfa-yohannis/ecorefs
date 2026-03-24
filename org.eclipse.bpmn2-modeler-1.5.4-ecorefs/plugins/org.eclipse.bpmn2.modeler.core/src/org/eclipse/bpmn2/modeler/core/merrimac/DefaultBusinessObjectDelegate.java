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

package org.eclipse.bpmn2.modeler.core.merrimac;

import java.util.Hashtable;
import java.util.List;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.util.Bpmn2Resource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;

/**
 * Default implementation for accessing Business Objects in the merrimac UI package.
 */
public class DefaultBusinessObjectDelegate implements IBusinessObjectDelegate {

	TransactionalEditingDomain editingDomain;
	Resource resource;
	
	public DefaultBusinessObjectDelegate(TransactionalEditingDomain editingDomain) {
		Assert.isNotNull(editingDomain);
		this.editingDomain = editingDomain;
	}

	@Override
	public EObject createObject(EClass eClass) {
		return Bpmn2ModelerFactory.createObject(getResource(), eClass);
	}
	
	@Override
	public EObject createFeature(EObject object, EStructuralFeature feature, EClass featureClass) {
		return Bpmn2ModelerFactory.createFeature(getResource(), object, feature, featureClass);
	}
	
	@Override
	public EObject createFeature(EObject object, EStructuralFeature feature, Class featureClass) {
		return Bpmn2ModelerFactory.createFeature(getResource(), object, feature, featureClass);
	}

	@Override
	public <T extends EObject> T createObject(Class clazz) {
		EClass eClass = (EClass) Bpmn2Package.eINSTANCE.getEClassifier(clazz.getSimpleName());
		if (eClass!=null) {
			return (T) createObject(eClass);
		}
		return null;
	}

	@Override
	public String getTextValue(EObject object) {
		return ExtendedPropertiesProvider.getTextValue(object);
	}

	@Override
	public boolean setTextValue(EObject object, String value) {
		return ExtendedPropertiesProvider.setTextValue(object, value);
	}

	@Override
	public String getLabel(EObject object) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.getLabel(getResource(), (EClass)object);
		return ExtendedPropertiesProvider.getLabel(object);
	}

	@Override
	public EStructuralFeature getFeature(EObject object, String name) {
		EStructuralFeature feature = object.eClass().getEStructuralFeature(name);
		if (feature==null) {
			// maybe it's an extension feature?
			ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(object);
			if (adapter!=null) {
				feature = adapter.getFeature(name);
			}
		}
		return feature;
	}

	@Override
	public String getLabel(EObject object, EStructuralFeature feature) {
//		if (feature.getEType().getEPackage() != Bpmn2Package.eINSTANCE)
		return ExtendedPropertiesProvider.getLabel(object, feature);
//		return ExtendedPropertiesProvider.getLabel(feature);
	}
	
	@Override
	public Object getValue(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.getValue(object, feature);
	}
	
	@Override
	public List<Object> getValueList(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.getValueList(getResource(), (EClass)object, feature);
		return ExtendedPropertiesProvider.getValueList(object, feature);
	}

	@Override
	public boolean setValue(EObject object, EStructuralFeature feature, Object value) {
		return ExtendedPropertiesProvider.setValue(object, feature, value);
	}

	@Override
	public Object getValue(EObject object, EStructuralFeature feature, int index) {
		return ExtendedPropertiesProvider.getValue(object, feature, index);
	}

	@Override
	public boolean setValue(EObject object, EStructuralFeature feature, Object value, int index) {
		return ExtendedPropertiesProvider.setValue(object, feature, value, index);
	}

	@Override
	public String getTextValue(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.getTextValue(object, feature);
	}

	@Override
	public boolean setTextValue(EObject object, EStructuralFeature feature, String value) {
		return ExtendedPropertiesProvider.setTextValue(object, value);
	}

	@Override
	public boolean isList(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.isList(object,feature);
	}

	@Override
	public boolean isAttribute(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.isAttribute(object,feature);
	}

	@Override
	public boolean isReference(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.isReference(object,feature);
	}

	@Override
	public boolean isMultiLineText(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.isMultiLineText(getResource(), (EClass)object,feature);
		return ExtendedPropertiesProvider.isMultiLineText(object,feature);
	}

	@Override
	public boolean canEdit(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.canEdit(getResource(), (EClass)object,feature);
		return ExtendedPropertiesProvider.canEdit(object,feature);
	}

	@Override
	public boolean canCreateNew(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.canCreateNew(getResource(), (EClass)object,feature);
		return ExtendedPropertiesProvider.canCreateNew(object,feature);
	}

	@Override
	public boolean canEditInline(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.canEditInline(getResource(), (EClass)object,feature);
		return ExtendedPropertiesProvider.canEditInline(object,feature);
	}

	@Override
	public boolean canSetNull(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.canSetNull(getResource(), (EClass)object,feature);
		return ExtendedPropertiesProvider.canSetNull(object,feature);
	}

	@Override
	public boolean isContainmentFeature(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.isContainmentFeature(object,feature);
	}

	@Override
	public boolean isMultiChoice(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.isMultiChoice(getResource(), (EClass)object, feature);
		return ExtendedPropertiesProvider.isMultiChoice(object, feature);
	}

	@Override
	public Hashtable<String, Object> getChoiceOfValues(EObject object, EStructuralFeature feature) {
		if (object instanceof EClass)
			return ExtendedPropertiesProvider.getChoiceOfValues(getResource(), (EClass)object, feature);
		return ExtendedPropertiesProvider.getChoiceOfValues(object, feature);
	}

	@Override
	public EClassifier getEType(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.getEType(object, feature);
	}

	private Resource getResource() {
		if (resource==null) {
			for (Resource r : editingDomain.getResourceSet().getResources()) {
				if (r instanceof Bpmn2Resource) {
					resource = r;
					break;
				}
			}
			
		}
		return resource;
	}
}
