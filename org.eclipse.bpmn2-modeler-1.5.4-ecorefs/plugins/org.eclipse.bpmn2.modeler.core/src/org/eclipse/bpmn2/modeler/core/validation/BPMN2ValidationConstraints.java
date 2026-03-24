/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 * All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *******************************************************************************/
package org.eclipse.bpmn2.modeler.core.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator;
import org.eclipse.bpmn2.modeler.core.validation.validators.IBpmn2ElementValidator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.AbstractModelConstraint;
import org.eclipse.emf.validation.EMFEventType;
import org.eclipse.emf.validation.IValidationContext;
import org.eclipse.emf.validation.model.ConstraintStatus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * Implements a Model Constraint validation object for BPMN2 elements. This Constraint
 * is invoked for every object in a BPMN2 resource. The strategy is to search for an
 * object validator to handle each specific BPMN2 element type. If none is found, move
 * up the object's super type hierarchy until a validator can be found. That validator
 * is used as the "base type validator" and controls whether we will continue to move
 * up the type hierarchy for additional validations. 
 */
public class BPMN2ValidationConstraints extends AbstractModelConstraint {

	public final static String CATEGORY_ID = "org.eclipse.bpmn2.modeler.core.validation.category.override"; //$NON-NLS-1$

	protected StatusList result = new StatusList();
	
	public BPMN2ValidationConstraints() {
	}

	@Override
	public IStatus validate(IValidationContext ctx) {
		initialize();
		
		EObject object = ctx.getTarget();
		if (doValidation(ctx, object)) {
			
			// Find a validator class to handle this object type:
			// We want to start with the instance EClass of the object
			// first, then work our way up the super class hierarchy
			// all the way to BaseElement. Unfortunately, EClass#getEAllSuperTypes()
			// returns the class hierarchy in reverse order, from BaseElement down
			// to more specialized types, so we'll have to make a copy and reverse
			// the list.
			List<EClass> types = new ArrayList<EClass>();
			types.addAll(object.eClass().getEAllSuperTypes());
			types.add(object.eClass());
			Collections.reverse(types);
			IBpmn2ElementValidator baseValidator = null;
			IBpmn2ElementValidator validator = null;
			for (EClass eClass : types) {
				if (baseValidator!=null)
					validator = getValidator(baseValidator, eClass);
				else
					validator = getValidator(ctx, eClass);
				
				if (validator!=null) {
					if (baseValidator==null) {
						baseValidator = validator;
						if (isLiveValidation(ctx) && !validator.doLiveValidation())
							continue;
						addStatus(validator.validate(object));
					}
					else if (baseValidator.checkSuperType(eClass, object)) {
						// The subclass validator wants to invoke the validator
						// for this super class.
						if (isLiveValidation(ctx) && !validator.doLiveValidation())
							continue;
						addStatus(validator.validate(object));
					}
				}
			}
		}
		return getResult(ctx);
	}

	/**
	 * Initialize this Model Constraint object. Since the EMF Validation Framework
	 * only constructs a single instance of each Constraing object, we need to make sure
	 * it is properly initialized each time it is invoked. In this case, we need to clear
	 * out the "results" list every time.
	 * 
	 * Subclasses should override this method for additional initialization.
	 */
	protected void initialize() {
		result.clear();
	}
	
	/**
	 * Determine if the given object should be validated. Subclasses may override this
	 * method to test for additional conditions that enable/disable object validation.
	 * 
	 * This base implementation searches for a Model Constraint for the given object
	 * type that may be defined in the current Target Runtime, and returns "false" if
	 * one is found. This means that the Target Runtime needs to override the default
	 * validations performed on the given object. 
	 * 
	 * @param ctx the Validation Context
	 * @param object the object to be validated
	 * @return true if the object needs to be validated, false if not.
	 */
	protected boolean doValidation(IValidationContext ctx, EObject object) {
		Bpmn2Preferences prefs = Bpmn2Preferences.getInstance(object);
		if (prefs.getDoCoreValidation()) {
			if (!deferValidationToTargetRuntime(ctx, object))
				return true;
		}
		return false;
	}
	
	/**
	 * Search for a BPMN2 Element Validator for the given object type.
	 * 
	 * @param ctx the Validation Context
	 * @param eClass the EClass (type) of the BPMN2 element
	 * @return a Validator if one has been defined, or null
	 */
	protected IBpmn2ElementValidator<?> getValidator(IValidationContext ctx, EClass eClass) {
		return AbstractBpmn2ElementValidator.getValidator(ctx, eClass.getInstanceClass());
	}
	
	protected IBpmn2ElementValidator<?> getValidator(IBpmn2ElementValidator<?> parent, EClass eClass) {
		return AbstractBpmn2ElementValidator.getValidator(parent, eClass.getInstanceClass());
	}
	
	/**
	 * Add the given IStatus to our results.
	 * 
	 * @param status
	 */
	protected void addStatus(IStatus status) {
		result.add(status);
	}
	
	/**
	 * Return the validation result. If no validation errors were found, this will be
	 * a single IStatus object with severity "OK".
	 * 
	 * @param ctx the Validation Context
	 * @return an IStatus object
	 */
	protected IStatus getResult(IValidationContext ctx) {
		if (result.isEmpty())
			return ctx.createSuccessStatus();
		if (result.size()==1)
			return result.get(0);
		return ConstraintStatus.createMultiStatus(ctx, result);
	}
	
	/**
	 * Check if the Validation Context indicates a Live validation.
	 * 
	 * @param ctx the Validation Context
	 * @return true if the Validation Context has a non-null EMF Event type,
	 *         indicating this is a Live validation.
	 */
	protected boolean isLiveValidation(IValidationContext ctx) {
		return ctx.getEventType() != EMFEventType.NULL;
	}

	/**
	 * Check if the Target Runtime defined for the given object's Resource wants
	 * to override the default validation for the given object.
	 * 
	 * To define a validation override, an extension plugin needs to define a Validation
	 * Constraint with category="org.eclipse.bpmn2.modeler.core.validation.category.override"
	 * for example:
	 * 
	 * <pre>
	 * <extension point="org.eclipse.emf.validation.constraintProviders">
	 *     <!-- These constraints override the ones in BPMN2 Modeler Core Validation -->
	 *     <constraintProvider cache="true">
	 *         <package namespaceUri="http://www.omg.org/spec/BPMN/20100524/MODEL-XMI" />
	 *         <constraints categories="org.eclipse.bpmn2.modeler.core.validation.category.override">
	 *             <constraint
	 *                 lang="Java"
	 *                 class="org.my.plugin.validation.ProcessConstraint"
	 *                 severity="ERROR"
	 *                 mode="Batch"
	 *                 name="My Plugin Process Constraint"
	 *                 id="org.my.plugin.validation.Process"
	 *                 statusCode="1">
	 *                 <target class="Process" />
	 *             </constraint>
	 *         </constraints>
	 *     </constraintProvider>
	 * </extension>
	 * </pre>
	 * 
	 * This will cause the default validation to be skipped, and the Validation
	 * Framework will invoke the "org.my.plugin.validation.ProcessConstraint"
	 * Model Constraint instead, whenever a Process object needs to be validated.
	 * 
	 * @param ctx
	 * @param object
	 * @return
	 */
	private boolean deferValidationToTargetRuntime(IValidationContext ctx, EObject object) {
		TargetRuntime rt = TargetRuntime.getRuntime(object);
		if (rt != TargetRuntime.getDefaultRuntime()) {
			ClassLoader cl = rt.getRuntimeExtension().getClass().getClassLoader();
			Bundle b = ((BundleReference)cl).getBundle();
			String pluginId = b.getSymbolicName();
			IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.emf.validation.constraintProviders"); //$NON-NLS-1$
			for (IConfigurationElement e : elements) {
				if (pluginId.equals(e.getDeclaringExtension().getContributor().getName())) {
					if ("constraintProvider".equals(e.getName())) { //$NON-NLS-1$
						for (IConfigurationElement e1 : e.getChildren("constraints")) { //$NON-NLS-1$
							String categories = e1.getAttribute("categories"); //$NON-NLS-1$
							if (categories!=null && categories.contains(CATEGORY_ID)) {
								for (IConfigurationElement e2 : e1.getChildren("constraint")) { //$NON-NLS-1$
									for (IConfigurationElement e3 : e2.getChildren("target")) { //$NON-NLS-1$
										String className = e3.getAttribute("class"); //$NON-NLS-1$
										int i = className.indexOf(':');
										if (i>0) {
											className = className.substring(0,i);
										}
										if (object.eClass().getName().equals(className)) {
											String mode = isLiveValidation(ctx) ? "Live" : "Batch"; //$NON-NLS-1$ //$NON-NLS-2$
											String m = e2.getAttribute("mode"); //$NON-NLS-1$
											if (m==null)
												m = "Batch"; //$NON-NLS-1$
											if (mode.equals(m))
												return true;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
}
