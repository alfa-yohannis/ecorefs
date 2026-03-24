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

import java.util.Collection;

import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.modeler.core.utils.ExtendedStringTokenizer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.xml.type.impl.AnyTypeImpl;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;

public class TaskReassignment extends AnyTypeImpl implements IEditingDomainProvider {
	
	Task task;
	ReassignmentType type = ReassignmentType.NOT_STARTED_REASSIGN;
	EList<String> users = new BasicEList<String>();
	EList<String> groups = new BasicEList<String>();
	String expiresAt = ""; //$NON-NLS-1$

	static EStructuralFeature typeFeature;
	static EStructuralFeature usersFeature;
	static EStructuralFeature groupsFeature;
	static EStructuralFeature expiresAtFeature;

	public TaskReassignment() {
		super();
		// cache these:
		typeFeature = TaskExtensionsFactory.eINSTANCE.getTaskReassignment_Type();
		usersFeature = TaskExtensionsFactory.eINSTANCE.getTaskReassignment_Users();
		groupsFeature = TaskExtensionsFactory.eINSTANCE.getTaskReassignment_Groups();
		expiresAtFeature = TaskExtensionsFactory.eINSTANCE.getTaskReassignment_ExpiresAt();
	}

	@Override
	public EClass eClass() {
		return TaskExtensionsFactory.eINSTANCE.getTaskReassignment();
	}

	@Override
	public Object eGet(EStructuralFeature eFeature) {
		if (eFeature == typeFeature)
			return getType();
		if (eFeature == usersFeature)
			return getUsers();
		if (eFeature == groupsFeature)
			return getGroups();
		if (eFeature == expiresAtFeature)
			return getExpiresAt();
		return super.eGet(eFeature);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void eSet(EStructuralFeature eFeature, Object newValue) {
		if (eFeature == typeFeature) {
			setType((ReassignmentType) newValue);
		}
		else if (eFeature == usersFeature) {
			getUsers().clear();
			getUsers().addAll((Collection<? extends String>) newValue);
		}
		else if (eFeature == groupsFeature) {
			getGroups().clear();
			getGroups().addAll((Collection<? extends String>) newValue);
		}
		else if (eFeature == expiresAtFeature) {
			setExpiresAt((String) newValue);
		}
		else
			super.eSet(eFeature, newValue);
	}

	public void setTask(Task task) {
		this.task = task;
	}
	
	public Task getTask() {
		return task;
	}
	
	public ReassignmentType getType() {
		return type;
	}

	public void setType(ReassignmentType type) {
		ReassignmentType oldType = this.type;
		this.type = type;
        if (eNotificationRequired() && oldType!=type)
            eNotify(new ENotificationImpl(this, Notification.SET, typeFeature.getFeatureID(),
                    oldType, type));
	}

	public EList<String> getUsers() {
		return users;
	}
	
	public String getUsersAsString() {
		String text = ""; //$NON-NLS-1$
		for (int i=0; i<getUsers().size(); ++i) {
			text += getUsers().get(i);
			if (i+1<getUsers().size())
				text += ","; //$NON-NLS-1$
		}
		return text;

	}

	public void setUsers(String users) {
		EList<String> oldUsers = new BasicEList<String>();
		oldUsers.addAll(this.users);
		this.users.clear();
		if (users!=null && !users.isEmpty()) {
			for (String s : users.split(",")) { //$NON-NLS-1$
				s = s.trim();
				if (!s.isEmpty())
					this.users.add(s);
			}
		}
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, usersFeature.getFeatureID(),
                    oldUsers, this.users));
	}
	
	public EList<String> getGroups() {
		return groups;
	}
	
	public String getGroupsAsString() {
		String text = ""; //$NON-NLS-1$
		for (int i=0; i<getGroups().size(); ++i) {
			text += getGroups().get(i);
			if (i+1<getGroups().size())
				text += ","; //$NON-NLS-1$
		}
		return text;

	}

	public void setGroups(String groups) {
		EList<String> oldGroups = new BasicEList<String>();
		oldGroups.addAll(this.groups);
		this.groups.clear();
		if (groups!=null && !groups.isEmpty()) {
			for (String s : groups.split(",")) { //$NON-NLS-1$
				s = s.trim();
				if (!s.isEmpty())
					this.groups.add(s);
			}
		}
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, groupsFeature.getFeatureID(),
                    oldGroups, this.groups));
	}

	public String getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(String expiresAt) {
		String oldExpiresAt = this.expiresAt;
		this.expiresAt = expiresAt;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, expiresAtFeature.getFeatureID(),
            		oldExpiresAt, this.expiresAt));
	}

	@Override
	public String toString() {
		String result = "[users:"; //$NON-NLS-1$
		for (int i = 0; i < users.size(); ++i) {
			result += users.get(i);
			if (i + 1 < users.size())
				result += ","; //$NON-NLS-1$
		}
		result += "|groups:"; //$NON-NLS-1$
		for (int i = 0; i < groups.size(); ++i) {
			result += groups.get(i);
			if (i + 1 < groups.size())
				result += ","; //$NON-NLS-1$
		}
		result += "]@[" + expiresAt + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		return result;
	}

	/*
	 * [users:user1,user2|groups:group1,group2]@[exp1]^[users:user5|groups:group5]@[exp3]
	 */
	public String fromString(String string) {
		String tail = null;
		users.clear();
		groups.clear();
		expiresAt = ""; //$NON-NLS-1$
		EStructuralFeature currentFeature = null;
		// append a newline to given string - this is used to collect
		// the tail of the string if a "^" delimiter is found.
		ExtendedStringTokenizer st = new ExtendedStringTokenizer(string + "\n", "[:,|]@^", true); //$NON-NLS-1$ //$NON-NLS-2$
		while (st.hasMoreTokens()) {
			String t = st.nextToken();
			if ("|".equals(t)) { //$NON-NLS-1$
				currentFeature = null;
				continue;
			}
			else if ("]".equals(t)) { //$NON-NLS-1$
				String t2 = st.nextToken();
				if ("@".equals(t2)) { //$NON-NLS-1$
					String t3 = st.nextToken();
					if ("[".equals(t3)) { //$NON-NLS-1$
						// body is complete, expect expiresAt feature next
						currentFeature = expiresAtFeature;
					}
					else {
						// add tokens to body
						st.pushToken(t3);
						st.pushToken(t2);
					}
				}
				else {
					st.pushToken(t2);
				}
			}
			else if ("^".equals(t)) { //$NON-NLS-1$
				tail = st.nextToken("\n"); //$NON-NLS-1$
				break;
			}
			else if (currentFeature!=null) {
				// we're currently parsing a feature
				if (currentFeature==usersFeature) {
					if (!",".equals(t)) //$NON-NLS-1$
						getUsers().add(t.trim());
				}
				else if (currentFeature==groupsFeature) {
					if (!",".equals(t)) //$NON-NLS-1$
						getGroups().add(t.trim());
				}
				else if (currentFeature==expiresAtFeature) {
					expiresAt += t;
				}
			}
			else if ("users".equals(t)) { //$NON-NLS-1$
				currentFeature = usersFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
			else if ("groups".equals(t)) { //$NON-NLS-1$
				currentFeature = groupsFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
		}
		
		return tail;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.emf.edit.domain.IEditingDomainProvider#getEditingDomain()
	 */
	@Override
	public EditingDomain getEditingDomain() {
		EditingDomain result = AdapterFactoryEditingDomain.getEditingDomainFor(task);
		return result;
	}
}
