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

package org.eclipse.bpmn2.modeler.core.validation.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.validation.StatusList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.validation.EMFEventType;
import org.eclipse.emf.validation.IValidationContext;
import org.eclipse.emf.validation.model.ConstraintStatus;

/**
 *
 */
public abstract class AbstractBpmn2ElementValidator<T extends EObject> implements IBpmn2ElementValidator<T> {

	protected AbstractBpmn2ElementValidator<?> parent;
	protected IValidationContext ctx;
	protected StatusList result = new StatusList();

	public AbstractBpmn2ElementValidator(IValidationContext ctx) {
		this.ctx = ctx;
	}

	public AbstractBpmn2ElementValidator(AbstractBpmn2ElementValidator<?> other) {
		this.parent = other.getParent();
		this.ctx = parent.ctx;
		this.result = parent.result;
	}

	/**
	 * Factory method for creating a validator object. The names of the Java
	 * classes that implement a validator must be in the form <BPMN2 type
	 * name>Validator and must be declared public. This factory method searches
	 * for such classes in the same package as this
	 * AbstractBpmn2ElementValidator base class. If such a class is found, a new
	 * instance is constructed and returned; otherwise null is returned.
	 * 
	 * @param ctx the IValidationContext used to construct the validator class
	 * @param c the BPMN2 element type. If this is an implementation class and
	 *            not an interface, the trailing "Impl" is truncated to form the
	 *            type name.
	 * @return a validator class if found, otherwise null.
	 */
	public static IBpmn2ElementValidator<?> getValidator(IValidationContext ctx, Class<?> c) {
		String className = AbstractBpmn2ElementValidator.class.getPackage().getName() + "." + c.getSimpleName(); //$NON-NLS-1$
		if (className.endsWith("Impl")) { //$NON-NLS-1$
			className = className.replaceFirst("Impl$", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		className += "Validator"; //$NON-NLS-1$
		try {
			Class<?> validatorClass = AbstractBpmn2ElementValidator.class.getClassLoader().loadClass(className);
			if (validatorClass != null) {
				return (AbstractBpmn2ElementValidator<?>) validatorClass.getConstructor(IValidationContext.class)
						.newInstance(ctx);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public static IBpmn2ElementValidator<?> getValidator(IBpmn2ElementValidator<?> parent, Class<?> c) {
		String className = AbstractBpmn2ElementValidator.class.getPackage().getName() + "." + c.getSimpleName(); //$NON-NLS-1$
		if (className.endsWith("Impl")) { //$NON-NLS-1$
			className = className.replaceFirst("Impl$", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		className += "Validator"; //$NON-NLS-1$
		try {
			Class<?> validatorClass = AbstractBpmn2ElementValidator.class.getClassLoader().loadClass(className);
			if (validatorClass != null) {
				return (AbstractBpmn2ElementValidator<?>) validatorClass.getConstructor(AbstractBpmn2ElementValidator.class)
						.newInstance(parent);
			}
		} catch (Exception e) {
		}
		return null;
	}

	protected AbstractBpmn2ElementValidator() {
	}

	protected void addStatus(EObject object, EObject[] resultLocus, int severity, String messagePattern, Object... messageArguments) {
		IStatus status = ConstraintStatus.createStatus(ctx, object, Arrays.asList(resultLocus), severity, 0, messagePattern, messageArguments);
		addStatus(status);
	}
	
	protected void addStatus(EObject object, int severity, String messagePattern, Object... messageArguments) {
		IStatus status = ConstraintStatus.createStatus(ctx, object, null, severity, 0, messagePattern, messageArguments);
		addStatus(status);
	}

	protected void addStatus(EObject object, String featureName, int severity, String messagePattern, Object... messageArguments) {
		List<EObject> resultLocus = null;
		resultLocus = new ArrayList<EObject>();
		EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
		if (feature != null) {
			resultLocus.add(feature);
		}
		IStatus status = ConstraintStatus.createStatus(ctx, object, resultLocus, severity, 0, messagePattern, messageArguments);
		addStatus(status);
	}

	protected void addStatus(EObject object, String featureName, EObject[] resultLocus, int severity, String messagePattern, Object... messageArguments) {
		List<EObject> rsltLocus = null;
		rsltLocus = new ArrayList<EObject>();
		rsltLocus.addAll(Arrays.asList(resultLocus));
		EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
		if (feature != null) {
			rsltLocus.add(feature);
		}
		IStatus status = ConstraintStatus.createStatus(ctx, object, rsltLocus, severity, 0, messagePattern, messageArguments);
		addStatus(status);
	}

	protected void addMissingFeatureStatus(EObject object, String featureName, int severity) {
		EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
		// change error message slightly for connections
		String message;
		if (feature!=null && feature.getEType() == Bpmn2Package.eINSTANCE.getSequenceFlow())
			message = Messages.AbstractBpmn2ElementValidator_Object_Missing_Connections;
		else
			message = Messages.AbstractBpmn2ElementValidator_Object_Missing_Feature;
		if (feature!=null)
			featureName = getLabel(object, feature);
		addStatus(object, featureName, severity, message,
				getLabel(object),
				getName(object),
				featureName);
	}

	protected void addMissingFeatureStatus(EObject object, String featureName, EObject[] resultLocus, int severity) {
		EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
		// change error message slightly for connections
		String message;
		if (feature!=null && feature.getEType() == Bpmn2Package.eINSTANCE.getSequenceFlow())
			message = Messages.AbstractBpmn2ElementValidator_Object_Missing_Connections;
		else
			message = Messages.AbstractBpmn2ElementValidator_Object_Missing_Feature;
		if (feature!=null)
			featureName = getLabel(object, feature);
		addStatus(object, featureName, resultLocus, severity, message,
				getLabel(object),
				getName(object),
				featureName);
	}

	protected void addStatus(IStatus status) {
		if (status.getSeverity()!=Status.OK)
			result.add(status);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.IBpmn2ElementValidator#getResult()
	 */
	public IStatus getResult() {
		if (result.isEmpty())
			return ctx.createSuccessStatus();
		if (result.size() == 1)
			return result.get(0);
		return ConstraintStatus.createMultiStatus(ctx, result);
	}

	protected AbstractBpmn2ElementValidator<?> getParent() {
		if (parent == null)
			return this;
		return parent;
	}

	protected String getLabel(EObject object) {
		return ExtendedPropertiesProvider.getLabel(object);
	}

	protected String getLabel(EObject object, EStructuralFeature feature) {
		return ExtendedPropertiesProvider.getLabel(object, feature);
	}

	protected String getName(EObject object) {
		return ExtendedPropertiesProvider.getTextValue(object);
	}

	/**
	 * Convenience method for testing if a given object is null or empty.
	 * The meaning of "empty" varies depending on the object type.
	 * 
	 * @param object the object to be tested
	 * @return true if the object is null or empty
	 */
	@SuppressWarnings("rawtypes")
	protected static boolean isEmpty(Object object) {
		if (object == null)
			return true;
		else if (object instanceof String) {
			String str = (String) object;
			return str == null || str.isEmpty();
		} else if (object instanceof List) {
			return ((List<?>) object).isEmpty();
		} else if (ModelUtil.isStringWrapper(object)) {
			String w = ModelUtil.getStringWrapperValue(object);
			if (w == null || w.isEmpty())
				return true;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.IBpmn2ElementValidator#validate(java.lang.Object)
	 */
	public abstract IStatus validate(T object);

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.IBpmn2ElementValidator#checkSuperType(org.eclipse.emf.ecore.EClass, java.lang.Object)
	 */
	public boolean checkSuperType(EClass eClass, T object) {
		return false;
	}
	
	/**
	 * Check if the Validation Context indicates a Live validation.
	 * 
	 * @param ctx the Validation Context
	 * @return true if the Validation Context has a non-null EMF Event type,
	 *         indicating this is a Live validation.
	 */
	protected boolean isLiveValidation() {
		return ctx.getEventType() != EMFEventType.NULL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.IBpmn2ElementValidator#doLiveValidation()
	 */
	public boolean doLiveValidation() {
		return false;
	}
}
