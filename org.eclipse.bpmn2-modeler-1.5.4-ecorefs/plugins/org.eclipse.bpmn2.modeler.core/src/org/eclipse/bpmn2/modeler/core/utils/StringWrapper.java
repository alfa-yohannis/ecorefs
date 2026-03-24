package org.eclipse.bpmn2.modeler.core.utils;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xml.type.AnyType;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;

public class StringWrapper extends DynamicEObjectImpl {
	private static EClass eClass = null;
	private static EPackage ePackage = null;

	public StringWrapper() {
		super();
		eSetClass(eClass());
	}

	public StringWrapper(String value) {
		this();
		this.eSet(eClass.getEStructuralFeature("value"), value); //$NON-NLS-1$
	}

	@Override
	public EClass eClass() {
		if (eClass==null) {
			ePackage = EcoreFactory.eINSTANCE.createEPackage();
			eClass = EcoreFactory.eINSTANCE.createEClass();
			ePackage.getEClassifiers().add(eClass);
			
			eClass.setName("StringWrapper"); //$NON-NLS-1$
			eClass.getESuperTypes().add(XMLTypePackage.eINSTANCE.getAnyType());
			ExtendedMetaData.INSTANCE.setName(eClass, ""); //$NON-NLS-1$
			eClass.setInstanceClass(AnyType.class);

			EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
			eAttribute.setName("value"); //$NON-NLS-1$
			eAttribute.setChangeable(true);
			eAttribute.setUnsettable(true);
			eAttribute.setEType(EcorePackage.eINSTANCE.getEClassifier("EString")); //$NON-NLS-1$
			eClass.getEStructuralFeatures().add(eAttribute);

//			ExtendedMetaData.INSTANCE.setNamespace(eAttribute, ePackage.getNsURI());
			ExtendedMetaData.INSTANCE.setFeatureKind(eAttribute, ExtendedMetaData.ATTRIBUTE_FEATURE);
			ExtendedMetaData.INSTANCE.setName(eAttribute, "value"); //$NON-NLS-1$
		}
		return eClass;
	}
	
	// prevent owners from trying to resolve this thing - it's just a string!
	public boolean eIsProxy() {
		return false;
	}

	@Override
	public boolean equals(Object that) {
		String thisValue = this.toString();
		if (that==null) {
			return thisValue==null || thisValue.isEmpty();
		}
		String thatValue = that.toString();
		if (thisValue==null) {
			return thatValue==null;
		}
		return thisValue.equals(thatValue);
	}
	
	@Override
	public String toString() {
		EStructuralFeature feature = this.eClass().getEStructuralFeature("value"); //$NON-NLS-1$
		if (feature!=null) {
			return (String)eGet(feature);
		}
		return null;
	}

}
