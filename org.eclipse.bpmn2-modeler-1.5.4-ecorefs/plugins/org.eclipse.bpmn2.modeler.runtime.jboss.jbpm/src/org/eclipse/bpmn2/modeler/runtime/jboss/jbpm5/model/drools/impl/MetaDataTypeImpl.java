/**
 */
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.impl;

import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.DroolsPackage;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.MetaDataType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.MetaValueType;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>MetaData Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.impl.MetaDataTypeImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.impl.MetaDataTypeImpl#getMetaValue <em>Meta Value</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MetaDataTypeImpl extends EObjectImpl implements MetaDataType {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;
	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;
	/**
	 * The cached value of the '{@link #getMetaValue() <em>Meta Value</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMetaValue()
	 * @generated
	 * @ordered
	 */
	protected MetaValueType metaValue;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MetaDataTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return DroolsPackage.Literals.META_DATA_TYPE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, DroolsPackage.META_DATA_TYPE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MetaValueType getMetaValue() {
		return metaValue;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetMetaValue(MetaValueType newMetaValue, NotificationChain msgs) {
		MetaValueType oldMetaValue = metaValue;
		metaValue = newMetaValue;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, DroolsPackage.META_DATA_TYPE__META_VALUE, oldMetaValue, newMetaValue);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMetaValue(MetaValueType newMetaValue) {
		if (newMetaValue != metaValue) {
			NotificationChain msgs = null;
			if (metaValue != null)
				msgs = ((InternalEObject)metaValue).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - DroolsPackage.META_DATA_TYPE__META_VALUE, null, msgs);
			if (newMetaValue != null)
				msgs = ((InternalEObject)newMetaValue).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - DroolsPackage.META_DATA_TYPE__META_VALUE, null, msgs);
			msgs = basicSetMetaValue(newMetaValue, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, DroolsPackage.META_DATA_TYPE__META_VALUE, newMetaValue, newMetaValue));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case DroolsPackage.META_DATA_TYPE__META_VALUE:
				return basicSetMetaValue(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case DroolsPackage.META_DATA_TYPE__NAME:
				return getName();
			case DroolsPackage.META_DATA_TYPE__META_VALUE:
				return getMetaValue();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case DroolsPackage.META_DATA_TYPE__NAME:
				setName((String)newValue);
				return;
			case DroolsPackage.META_DATA_TYPE__META_VALUE:
				setMetaValue((MetaValueType)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case DroolsPackage.META_DATA_TYPE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case DroolsPackage.META_DATA_TYPE__META_VALUE:
				setMetaValue((MetaValueType)null);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case DroolsPackage.META_DATA_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case DroolsPackage.META_DATA_TYPE__META_VALUE:
				return metaValue != null;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

} //MetaDataTypeImpl
