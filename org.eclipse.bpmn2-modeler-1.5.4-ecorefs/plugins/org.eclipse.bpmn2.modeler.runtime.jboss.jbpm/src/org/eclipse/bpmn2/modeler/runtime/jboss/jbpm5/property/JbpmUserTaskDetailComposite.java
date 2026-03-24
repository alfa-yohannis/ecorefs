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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property;


import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.extensions.TaskNotificationsListComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.extensions.TaskReassignmentsListComposite;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Bob Brodt
 *
 */
public class JbpmUserTaskDetailComposite extends JbpmTaskDetailComposite {

	final static String NOT_STARTED_NOTIFY = "NotStartedNotify"; //$NON-NLS-1$
	final static String NOT_COMPLETED_NOTIFY = "NotCompletedNotify"; //$NON-NLS-1$

	public JbpmUserTaskDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	public JbpmUserTaskDetailComposite(Composite parent, int style) {
		super(parent, style);
	}


	@Override
	public void createBindings(EObject be) {
		super.createBindings(be);

		TaskNotificationsListComposite notificationsList = new TaskNotificationsListComposite(this, (Task)be);
		notificationsList.bindList(be, PACKAGE.getActivity_DataInputAssociations());
		notificationsList.setTitle("Notifications"); //$NON-NLS-1$

		TaskReassignmentsListComposite reassignmentsList = new TaskReassignmentsListComposite(this, (Task)be);
		reassignmentsList.bindList(be, PACKAGE.getActivity_DataInputAssociations());
		reassignmentsList.setTitle("Reassignments"); //$NON-NLS-1$
	}
}
