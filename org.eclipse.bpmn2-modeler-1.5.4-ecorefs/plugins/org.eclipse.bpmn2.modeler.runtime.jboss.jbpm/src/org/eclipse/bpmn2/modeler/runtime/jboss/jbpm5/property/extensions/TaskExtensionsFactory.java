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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.extensions;

import org.eclipse.bpmn2.modeler.core.model.ModelDecorator;
import org.eclipse.bpmn2.modeler.core.model.ModelDecoratorAdapter;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.DroolsPackage;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/*
 * Notifications:
 * [from:from1|tousers:user1,user2|togroups:group1,group2|replyTo:reply1|subject:subject1|body:body1<br />Hello World<br />Some more text]@[exp1]^[from:from3|tousers:user5|togroups:group5|replyTo:reply3|subject:subject3|body:body3]@[exp3]
 * Reassignments:
 * [users:user1,user2|groups:group1,group2]@[exp1]^[users:user5|groups:group5]@[exp3]
 */

public class TaskExtensionsFactory {
	
	public final static String TASKREASSIGNMENT_CLASS_NAME = "TaskReassignment"; //$NON-NLS-1$
	public final static String TASKREASSIGNMENT_TYPE_FEATURE_NAME = "type"; //$NON-NLS-1$
	public final static String TASKREASSIGNMENT_USERS_FEATURE_NAME = "users"; //$NON-NLS-1$
	public final static String TASKREASSIGNMENT_GROUPS_FEATURE_NAME = "groups"; //$NON-NLS-1$
	public final static String TASKREASSIGNMENT_EXPIRES_AT_FEATURE_NAME = "expiresAt"; //$NON-NLS-1$

	public final static String TASKNOTIFICATION_CLASS_NAME = "TaskNotification"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_TYPE_FEATURE_NAME = "type"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_FROM_FEATURE_NAME = "from"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_TO_USERS_FEATURE_NAME = "tousers"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_TO_GROUPS_FEATURE_NAME = "togroups"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_REPLY_TO_FEATURE_NAME = "replyTo"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_SUBJECT_FEATURE_NAME = "subject"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_BODY_FEATURE_NAME = "body"; //$NON-NLS-1$
	public final static String TASKNOTIFICATION_EXPIRES_AT_FEATURE_NAME = "expiresAt"; //$NON-NLS-1$
	
	ModelDecorator modelDecorator = ModelDecoratorAdapter.getModelDecorator(DroolsPackage.eINSTANCE);
	
	public static TaskExtensionsFactory eINSTANCE = new TaskExtensionsFactory();
	
	private TaskExtensionsFactory() {
		modelDecorator.createEClass(TASKREASSIGNMENT_CLASS_NAME, TaskReassignment.class);
		modelDecorator.createEAttribute(TASKREASSIGNMENT_TYPE_FEATURE_NAME,
				"ReassignmentType:EEnum", TASKREASSIGNMENT_CLASS_NAME, //$NON-NLS-1$
				null, new Enumerator[] {
					ReassignmentType.NOT_STARTED_REASSIGN,
					ReassignmentType.NOT_COMPLETED_REASSIGN
				}
		);
		modelDecorator.createEReference(TASKREASSIGNMENT_USERS_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKREASSIGNMENT_CLASS_NAME, true, true);
		modelDecorator.createEReference(TASKREASSIGNMENT_GROUPS_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKREASSIGNMENT_CLASS_NAME, true, true);
		modelDecorator.createEAttribute(TASKREASSIGNMENT_EXPIRES_AT_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKREASSIGNMENT_CLASS_NAME, ""); //$NON-NLS-1$

	
		modelDecorator.createEClass(TASKNOTIFICATION_CLASS_NAME, TaskNotification.class);
		modelDecorator.createEAttribute(TASKNOTIFICATION_TYPE_FEATURE_NAME,
				"NotificationType:EEnum", TASKNOTIFICATION_CLASS_NAME, //$NON-NLS-1$
				null, new Enumerator[] {
					NotificationType.NOT_STARTED_NOTIFY,
					NotificationType.NOT_COMPLETED_NOTIFY
				}
		);
		modelDecorator.createEAttribute(TASKNOTIFICATION_FROM_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKNOTIFICATION_CLASS_NAME, ""); //$NON-NLS-1$
		modelDecorator.createEReference(TASKNOTIFICATION_TO_USERS_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKNOTIFICATION_CLASS_NAME, true, true);
		modelDecorator.createEReference(TASKNOTIFICATION_TO_GROUPS_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKNOTIFICATION_CLASS_NAME, true, true);
		modelDecorator.createEAttribute(TASKNOTIFICATION_REPLY_TO_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKNOTIFICATION_CLASS_NAME, ""); //$NON-NLS-1$
		modelDecorator.createEAttribute(TASKNOTIFICATION_SUBJECT_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKNOTIFICATION_CLASS_NAME, ""); //$NON-NLS-1$
		modelDecorator.createEAttribute(TASKNOTIFICATION_BODY_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKNOTIFICATION_CLASS_NAME, ""); //$NON-NLS-1$
		modelDecorator.createEAttribute(TASKNOTIFICATION_EXPIRES_AT_FEATURE_NAME, "EString", //$NON-NLS-1$
				TASKNOTIFICATION_CLASS_NAME, ""); //$NON-NLS-1$
	}
	
	/*
	 * Task Reassignment
	 */
	public EClass getTaskReassignment() {
		return modelDecorator.getEClass(TASKREASSIGNMENT_CLASS_NAME);
	}
	
	public EStructuralFeature getTaskReassignment_Type() {
		return getTaskReassignment().getEStructuralFeature(TASKREASSIGNMENT_TYPE_FEATURE_NAME);
	}
	
	public EStructuralFeature getTaskReassignment_ExpiresAt() {
		return getTaskReassignment().getEStructuralFeature("expiresAt"); //$NON-NLS-1$
	}
	
	public EStructuralFeature getTaskReassignment_Users() {
		return getTaskReassignment().getEStructuralFeature(TASKREASSIGNMENT_USERS_FEATURE_NAME);
	}
	
	public EStructuralFeature getTaskReassignment_Groups() {
		return getTaskReassignment().getEStructuralFeature(TASKREASSIGNMENT_GROUPS_FEATURE_NAME);
	}
	
	public TaskReassignment createTaskReassignment() {
		EClass eClass = getTaskReassignment();
		EObject object = eClass.getEPackage().getEFactoryInstance().create(eClass);
		return (TaskReassignment)object;
	}
	
	/*
	 * Task Notification
	 */
	public EClass getTaskNotification() {
		return modelDecorator.getEClass(TASKNOTIFICATION_CLASS_NAME);
	}
	
	public EStructuralFeature getTaskNotification_Type() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_TYPE_FEATURE_NAME);
	}

	public EStructuralFeature getTaskNotification_From() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_FROM_FEATURE_NAME);
	}

	public EStructuralFeature getTaskNotification_ToUsers() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_TO_USERS_FEATURE_NAME);
	}

	public EStructuralFeature getTaskNotification_ToGroups() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_TO_GROUPS_FEATURE_NAME);
	}

	public EStructuralFeature getTaskNotification_ReplyTo() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_REPLY_TO_FEATURE_NAME);
	}

	public EStructuralFeature getTaskNotification_Subject() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_SUBJECT_FEATURE_NAME);
	}

	public EStructuralFeature getTaskNotification_Body() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_BODY_FEATURE_NAME);
	}

	public EStructuralFeature getTaskNotification_ExpiresAt() {
		return getTaskNotification().getEStructuralFeature(TASKNOTIFICATION_EXPIRES_AT_FEATURE_NAME);
	}
	
	public TaskNotification createTaskNotification() {
		EClass eClass = getTaskNotification();
		EObject object = eClass.getEPackage().getEFactoryInstance().create(eClass);
		return (TaskNotification)object;
	}
}