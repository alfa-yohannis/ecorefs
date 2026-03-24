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

import org.eclipse.bpmn2.Assignment;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.UserTask;
import org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.IDiagramProfile;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.Messages;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.validation.ServletUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.validation.IValidationContext;

public class UserTaskValidator extends AbstractBpmn2ElementValidator<UserTask> {

	// TODO: Link the ServletUtil to guvnor db somehow so that we can look up
	// external Processes and Packages and whatnot. 
	private IDiagramProfile profile;
	private String uuid = "uuid"; //$NON-NLS-1$

	/**
	 * @param ctx
	 */
	public UserTaskValidator(IValidationContext ctx) {
		super(ctx);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.validation.validators.AbstractBpmn2ElementValidator#validate(org.eclipse.bpmn2.BaseElement)
	 */
	@Override
	public IStatus validate(UserTask object) {
		String taskName = null;
		if (object.getIoSpecification()!=null) {
			for (DataInput di : object.getIoSpecification().getDataInputs()) {
				if ("TaskName".equalsIgnoreCase(di.getName())) { //$NON-NLS-1$
					for (DataInputAssociation dia : object.getDataInputAssociations()) {
						if (dia.getTargetRef() == di) {
							if (dia.getAssignment().size()!=0) {
								Assignment a = dia.getAssignment().get(0);
								if (a.getFrom() instanceof FormalExpression) {
									String body = ((FormalExpression)a.getFrom()).getBody();
									if (body!=null && !body.isEmpty()) {
										taskName = body;
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		if (taskName==null) {
			addStatus(object, Status.ERROR, Messages.UserTaskConstraint_No_Name);
		}
		else {
			// TODO:
			if (taskName != null) {
				String[] packageAssetInfo = ServletUtil.findPackageAndAssetInfo(uuid, profile);
				String taskFormName = taskName + "-taskform"; //$NON-NLS-1$
				if (!ServletUtil.assetExistsInGuvnor(packageAssetInfo[0], taskFormName, profile)) {
					addStatus(object, Status.ERROR, Messages.UserTaskConstraint_No_Form);
				}
			}
		}

		// simulation validation
		// TODO: fix this
		/*
		if (ut.getExtensionValues() != null && ut.getExtensionValues().size() > 0) {
			boolean foundStaffAvailability = false;
			for (ExtensionAttributeValue extattrval : ut.getExtensionValues()) {
				Bpmn2FeatureMap extensionElements = extattrval.getValue();
				if (extensionElements!=null) {
					@SuppressWarnings("unchecked")
					List<MetaDataType> metaDataTypeExtensions = (List<MetaDataType>) extensionElements.get(
							DroolsPackage.Literals.DOCUMENT_ROOT__METADATA, true);
					if (metaDataTypeExtensions != null && metaDataTypeExtensions.size() > 0) {
						MetaDataType metaType = metaDataTypeExtensions.get(0);
						for (Object metaValueObj : metaType.getMetaValue()) {
							MetaValueType entry = (MetaValueType) metaValueObj;
							if (entry.getName() != null && entry.getName().equals("staffavailability")) {
								Float f = new Float(entry.getValue());
								if (f.floatValue() < 0) {
									ctx.createFailureStatus("User Task Simulation Parameter \"Staff Availability\" must be positive");
								}
								foundStaffAvailability = true;
							}
						}
					}
					if (!foundStaffAvailability) {
						return ctx.createFailureStatus("User Task Simulation Parameter \"Staff Availability\" is not defined");
					}
				}
			}
		}
		*/
		return getResult();
	}
}