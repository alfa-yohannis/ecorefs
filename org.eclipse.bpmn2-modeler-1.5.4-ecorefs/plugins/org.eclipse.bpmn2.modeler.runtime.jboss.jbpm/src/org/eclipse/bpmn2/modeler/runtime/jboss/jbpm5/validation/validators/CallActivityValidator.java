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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.validators;

import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.modeler.core.model.ModelDecorator;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.ExternalProcess;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.util.JbpmModelUtil;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.IDiagramProfile;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.Messages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.validation.IValidationContext;

public class CallActivityValidator extends AbstractBpmn2ElementValidator<CallActivity> {

	// TODO: Link the ServletUtil to guvnor db somehow so that we can look up
	// external Processes and Packages and whatnot. 
	private IDiagramProfile profile;
	private String uuid = "uuid"; //$NON-NLS-1$

	/**
	 * @param ctx
	 */
	public CallActivityValidator(IValidationContext ctx) {
		super(ctx);
	}

	@Override
	public IStatus validate(CallActivity object) {
		Object ref = object.getCalledElementRef();
		if (ref instanceof ExternalProcess) {
			String calledProcessId = ((ExternalProcess)ref).getId();
//			String[] packageAssetInfo = ServletUtil.findPackageAndAssetInfo(uuid, profile);
//			String packageName = packageAssetInfo[0];
//			List<String> allProcessesInPackage = ServletUtil.getAllProcessesInPackage(packageName, profile);
//			boolean foundCalledElementProcess = false;
//			for (String p : allProcessesInPackage) {
//				String processContent = ServletUtil.getProcessSourceContent(packageName, p, profile);
//				Pattern pattern = Pattern.compile("<\\S*process[\\s\\S]*id=\"" + calledProcessId + "\"", //$NON-NLS-1$ //$NON-NLS-2$
//						Pattern.MULTILINE);
//				Matcher m = pattern.matcher(processContent);
//				if (m.find()) {
//					foundCalledElementProcess = true;
//					break;
//				}
//			}
//			foundCalledElementProcess = true; // TODO: remove this
//			if (!foundCalledElementProcess) {
//				addStatus(object, Status.ERROR, Messages.CallActivityConstraint_Process_ID_Not_Found, calledProcessId);
//			}

			boolean isVariable = false;
			String var = JbpmModelUtil.getVariableReference(calledProcessId);
			if (var!=null) {
				// get the Property instances (a.k.a. "local variables") of the containing Process or SubProcess
				for (EObject p : ModelUtil.collectAncestorObjects(object, "properties", new Class[] {Process.class, SubProcess.class})) {  //$NON-NLS-1$
					String id = ((Property)p).getId();
					if (var.equals(id)) {
						isVariable = true;
						break;
					}
				}
				if (!isVariable)
					addStatus(object, Status.ERROR, Messages.CallActivityConstraint_Not_A_Process_Variable, calledProcessId);
			}
			else if (!JbpmModelUtil.isProcessId(calledProcessId))
				addStatus(object, Status.ERROR, Messages.CallActivityConstraint_Not_A_Process_ID, calledProcessId);
		}
		EStructuralFeature feature;
		feature = ModelDecorator.getAnyAttribute(object, "independent"); //$NON-NLS-1$
		Boolean independent = (Boolean) object.eGet(feature);
		feature = ModelDecorator.getAnyAttribute(object, "waitForCompletion"); //$NON-NLS-1$
		Boolean waitForCompletion = (Boolean) object.eGet(feature);
		if (independent==false && waitForCompletion==false) {
			addStatus(object, Status.ERROR, Messages.CallActivityConstraint_Independent_And_WaitForCompletion_False);
		}
		return getResult();
	}
}