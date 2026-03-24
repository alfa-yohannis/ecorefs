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

import org.eclipse.bpmn2.Assignment;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

/*
 *
 */
@SuppressWarnings("serial")
public class TaskReassignmentList extends BasicEList<EObject> implements Adapter {

	protected Task task;
	
	public TaskReassignmentList() {
		super();
	}

	public void add(String type, String string) {
		TaskReassignment tr = TaskExtensionsFactory.eINSTANCE.createTaskReassignment();
		tr.setType(ReassignmentType.get(type));
		add(tr);
		String tail = tr.fromString(string);
		while (tail!=null) {
			tr = TaskExtensionsFactory.eINSTANCE.createTaskReassignment();
			tr.setType(ReassignmentType.get(type));
			add(tr);
			tail = tr.fromString(tail);
		}

	}

	@Override
	public boolean add(EObject object) {
		Assert.isTrue(object instanceof TaskReassignment);
		object.eAdapters().add(this);
		((TaskReassignment)object).setTask(task);
		return super.add(object);
	}

	public boolean replace(int index, String type, String string) {
		if (index>=0) {
			if (index>=size()) {
				add(type, string);
				return true;
			}
		}
		TaskReassignment tr = get(index);
		String oldString = tr.toString();
		if (!tr.getType().getLiteral().equals(type) || !oldString.equals(string)) {
			tr.setType(ReassignmentType.get(type));
			tr.fromString(string);
			return true;
		}
		return false;
	}
	
	@Override
	public EObject remove(int index) {
		EObject object = super.remove(index);
		object.eAdapters().remove(this);
		return object;
	}

	@Override
	public TaskReassignment get(int index) {
		return (TaskReassignment)super.get(index);
	}
    
	public void setTask(Task task) {
		Assert.isTrue(task!=null);
		InputOutputSpecification iospec = task.getIoSpecification();
		Assert.isTrue(iospec!=null && iospec.getInputSets().size()>0);
		
		this.task = task;
	}
	
	public void updateTask() {
		/*
		 * Here we need to examine the current list of DataInputAssociations on the
		 * Task and, if there is already a DataInput named NotStartedReassign or
		 * NotCompletedReassign, append a new expression. Otherwise create a new
		 * DataInput and DataInputAssociation.
		 * 
		 * The XML looks like this:
		 * 
		 * <bpmn2:ioSpecification id="IOSpec_1">
         *   <bpmn2:dataInput id="DataInput_1" name="NotCompletedReassign"/>
         *   <bpmn2:dataInput id="DataInput_2" name="NotStartedReassign"/>
      	 * </bpmn2:ioSpecification>
		 *
		 * <bpmn2:dataInputAssociation id="DataInputAssociation_1">
		 *   <bpmn2:targetRef>DataInput_1</bpmn2:targetRef>
		 *   <bpmn2:assignment id="Assignment_1">
	 	 *     <bpmn2:from xsi:type="bpmn2:tFormalExpression"><![CDATA[[users:user2|groups:group2]@[exp2]^[users:user1,user2,user3|groups:group1,group2,group3]@[exp4]]]></bpmn2:from>
		 *     <bpmn2:to xsi:type="bpmn2:tFormalExpression">DataInput_1</bpmn2:to>
		 *   </bpmn2:assignment>
		 * </bpmn2:dataInputAssociation>
		 * 
		 * <bpmn2:dataInputAssociation id="DataInputAssociation_2">
		 *   <bpmn2:targetRef>DataInput_2</bpmn2:targetRef>
		 *   <bpmn2:assignment id="Assignment_2">
	 	 *     <bpmn2:from xsi:type="bpmn2:tFormalExpression"><![CDATA[[users:user1|groups:group1]@[exp1]^[users:user1,user2,user3|groups:group1,group2,group3]@[exp4]]]></bpmn2:from>
		 *     <bpmn2:to xsi:type="bpmn2:tFormalExpression">DataInput_2</bpmn2:to>
		 *   </bpmn2:assignment>
		 * </bpmn2:dataInputAssociation>
		 */
		Resource resource = task.eResource();
		InputOutputSpecification iospec = task.getIoSpecification();
		DataInputAssociation notStarted = null;
		DataInputAssociation notCompleted = null;
		DataInput input;
		Assignment assignment;
		FormalExpression expression;
		String body;
		for (DataInputAssociation dia : task.getDataInputAssociations()) {
			if (dia.getTargetRef() instanceof DataInput) {
				input = (DataInput) dia.getTargetRef();
				if (ReassignmentType.NOT_STARTED_REASSIGN.getLiteral().equals(input.getName())) {
					notStarted = dia;
				}
				else if (ReassignmentType.NOT_COMPLETED_REASSIGN.getLiteral().equals(input.getName())) {
					notCompleted = dia;
				}
			}
		}
		
		body = toString(ReassignmentType.NOT_STARTED_REASSIGN);
		if (body.isEmpty()) {
			if (notStarted!=null) {
				// need to remove the NotCompletedReassign data input and association
				iospec.getDataInputs().remove(notStarted.getTargetRef());
				iospec.getInputSets().get(0).getDataInputRefs().remove(notStarted.getTargetRef());
				task.getDataInputAssociations().remove(notStarted);
			}
		}
		else {
			if (notStarted==null) {
				// create the NotStartedReassign data input and association
				input = (DataInput) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getDataInput());
				input.setName(ReassignmentType.NOT_STARTED_REASSIGN.getLiteral());
				iospec.getDataInputs().add(input);
				iospec.getInputSets().get(0).getDataInputRefs().add(input);
				notStarted = (DataInputAssociation) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getDataInputAssociation());
				notStarted.setTargetRef(input);
				assignment = (Assignment) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getAssignment());
				expression = (FormalExpression) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getFormalExpression());
				// make sure this DataInput has an ID
				ModelUtil.setID(input);
				expression.setBody(input.getId());
				assignment.setTo(expression);
				notStarted.getAssignment().add(assignment);
				task.getDataInputAssociations().add(notStarted);

				ModelUtil.setID(notStarted);
				ModelUtil.setID(assignment);
				ModelUtil.setID(expression);
			}
			
			assignment = (Assignment) notStarted.getAssignment().get(0);
			expression = (FormalExpression)assignment.getFrom();
			if (expression==null) {
				expression = (FormalExpression) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getFormalExpression());
				assignment.setFrom(expression);
				ModelUtil.setID(expression);
			}
			expression.setBody(body);
		}		
		
		body = toString(ReassignmentType.NOT_COMPLETED_REASSIGN);
		if (body.isEmpty()) {
			if (notCompleted!=null) {
				// need to remove the NotCompletedReassign data input and association
				iospec.getDataInputs().remove(notCompleted.getTargetRef());
				iospec.getInputSets().get(0).getDataInputRefs().remove(notCompleted.getTargetRef());
				task.getDataInputAssociations().remove(notCompleted);
			}
		}
		else {
			if (notCompleted==null) {
				// create the NotStartedReassign data input and association
				input = (DataInput) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getDataInput());
				input.setName(ReassignmentType.NOT_COMPLETED_REASSIGN.getLiteral());
				iospec.getDataInputs().add(input);
				iospec.getInputSets().get(0).getDataInputRefs().add(input);
				notCompleted = (DataInputAssociation) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getDataInputAssociation());
				notCompleted.setTargetRef(input);
				assignment = (Assignment) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getAssignment());
				expression = (FormalExpression) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getFormalExpression());
				// make sure this DataInput has an ID
				ModelUtil.setID(input);
				expression.setBody(input.getId());
				assignment.setTo(expression);
				notCompleted.getAssignment().add(assignment);
				task.getDataInputAssociations().add(notCompleted);

				ModelUtil.setID(notCompleted);
				ModelUtil.setID(assignment);
				ModelUtil.setID(expression);
			}
			
			assignment = (Assignment) notCompleted.getAssignment().get(0);
			expression = (FormalExpression)assignment.getFrom();
			if (expression==null) {
				expression = (FormalExpression) Bpmn2ModelerFactory.create(resource, Bpmn2Package.eINSTANCE.getFormalExpression());
				assignment.setFrom(expression);
				ModelUtil.setID(expression);
			}
			expression.setBody(body);
		}
	}
	
	public String toString(ReassignmentType type) {
		String result = ""; //$NON-NLS-1$
		for (int i=0; i<size(); ++i) {
			TaskReassignment tr = get(i);
			if (tr.getType().equals(type)) {
				if (!result.isEmpty())
					result += "^"; //$NON-NLS-1$
				result += tr.toString();
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.emf.common.notify.Adapter#getTarget()
	 */
	@Override
	public Notifier getTarget() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.emf.common.notify.Adapter#setTarget(org.eclipse.emf.common.notify.Notifier)
	 */
	@Override
	public void setTarget(Notifier newTarget) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.emf.common.notify.Adapter#isAdapterForType(java.lang.Object)
	 */
	@Override
	public boolean isAdapterForType(Object type) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.emf.common.notify.Adapter#notifyChanged(org.eclipse.emf.common.notify.Notification)
	 */
	@Override
	public void notifyChanged(Notification notification) {
		updateTask();
	}
}