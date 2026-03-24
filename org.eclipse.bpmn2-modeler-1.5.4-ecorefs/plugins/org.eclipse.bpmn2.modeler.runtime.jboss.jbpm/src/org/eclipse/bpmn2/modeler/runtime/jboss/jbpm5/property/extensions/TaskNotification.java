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

public class TaskNotification extends AnyTypeImpl implements IEditingDomainProvider {
	
	Task task;
	NotificationType type = NotificationType.NOT_STARTED_NOTIFY;
	String from = ""; //$NON-NLS-1$
	EList<String> toUsers = new BasicEList<String>();
	EList<String> toGroups = new BasicEList<String>();
	String replyTo = ""; //$NON-NLS-1$
	String subject = ""; //$NON-NLS-1$
	String body = ""; //$NON-NLS-1$
	String expiresAt = ""; //$NON-NLS-1$

	static EStructuralFeature typeFeature;
	static EStructuralFeature fromFeature;
	static EStructuralFeature toUsersFeature;
	static EStructuralFeature toGroupsFeature;
	static EStructuralFeature replyToFeature;
	static EStructuralFeature subjectFeature;
	static EStructuralFeature bodyFeature;
	static EStructuralFeature expiresAtFeature;

	public TaskNotification() {
		super();
		// cache these:
		typeFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_Type();
		fromFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_From();
		toUsersFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_ToUsers();
		toGroupsFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_ToGroups();
		replyToFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_ReplyTo();
		subjectFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_Subject();
		bodyFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_Body();
		expiresAtFeature = TaskExtensionsFactory.eINSTANCE.getTaskNotification_ExpiresAt();
	}

	@Override
	public EClass eClass() {
		return TaskExtensionsFactory.eINSTANCE.getTaskNotification();
	}

	@Override
	public Object eGet(EStructuralFeature eFeature) {
		if (eFeature == typeFeature)
			return getType();
		if (eFeature == fromFeature)
			return getFrom();
		if (eFeature == toUsersFeature)
			return getToUsers();
		if (eFeature == toGroupsFeature)
			return getToGroups();
		if (eFeature == replyToFeature)
			return getReplyTo();
		if (eFeature == subjectFeature)
			return getSubject();
		if (eFeature == bodyFeature)
			return getBody();
		if (eFeature == expiresAtFeature)
			return getExpiresAt();
		return super.eGet(eFeature);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void eSet(EStructuralFeature eFeature, Object newValue) {
		if (eFeature == typeFeature) {
			setType((NotificationType) newValue);
		}
		else if (eFeature == fromFeature) {
			setFrom((String)newValue);
		}
		else if (eFeature == toUsersFeature) {
			getToUsers().clear();
			getToUsers().addAll((Collection<? extends String>) newValue);
		}
		else if (eFeature == toGroupsFeature) {
			getToGroups().clear();
			getToGroups().addAll((Collection<? extends String>) newValue);
		}
		else if (eFeature == replyToFeature) {
			setReplyTo((String)newValue);
		}
		else if (eFeature == subjectFeature) {
			setSubject((String)newValue);
		}
		else if (eFeature == bodyFeature) {
			setBody((String)newValue);
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
	
	public NotificationType getType() {
		return type;
	}

	public void setType(NotificationType type) {
		NotificationType oldType = this.type;
		this.type = type;
        if (eNotificationRequired() && oldType!=type)
            eNotify(new ENotificationImpl(this, Notification.SET, typeFeature.getFeatureID(),
                    oldType, type));
	}
	
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		String oldFrom = this.from;
		this.from = from;
        if (eNotificationRequired() && oldFrom!=from)
            eNotify(new ENotificationImpl(this, Notification.SET, fromFeature.getFeatureID(),
                    oldFrom, from));
	}

	public EList<String> getToUsers() {
		return toUsers;
	}
	
	public String getToUsersAsString() {
		String text = ""; //$NON-NLS-1$
		for (int i=0; i<getToUsers().size(); ++i) {
			text += getToUsers().get(i);
			if (i+1<getToUsers().size())
				text += ","; //$NON-NLS-1$
		}
		return text;

	}

	public void setToUsers(String users) {
		EList<String> oldUsers = new BasicEList<String>();
		oldUsers.addAll(this.toUsers);
		this.toUsers.clear();
		if (users!=null && !users.isEmpty()) {
			for (String s : users.split(",")) { //$NON-NLS-1$
				s = s.trim();
				if (!s.isEmpty())
					this.toUsers.add(s);
			}
		}
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, toUsersFeature.getFeatureID(),
                    oldUsers, this.toUsers));
	}
	
	public EList<String> getToGroups() {
		return toGroups;
	}
	
	public String getToGroupsAsString() {
		String text = ""; //$NON-NLS-1$
		for (int i=0; i<getToGroups().size(); ++i) {
			text += getToGroups().get(i);
			if (i+1<getToGroups().size())
				text += ","; //$NON-NLS-1$
		}
		return text;

	}

	public void setToGroups(String groups) {
		EList<String> oldGroups = new BasicEList<String>();
		oldGroups.addAll(this.toGroups);
		this.toGroups.clear();
		if (groups!=null && !groups.isEmpty()) {
			for (String s : groups.split(",")) { //$NON-NLS-1$
				s = s.trim();
				if (!s.isEmpty())
					this.toGroups.add(s);
			}
		}
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, toGroupsFeature.getFeatureID(),
                    oldGroups, this.toGroups));
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		String oldReplyTo = this.replyTo;
		this.replyTo = replyTo;
        if (eNotificationRequired() && oldReplyTo!=replyTo)
            eNotify(new ENotificationImpl(this, Notification.SET, replyToFeature.getFeatureID(),
                    oldReplyTo, replyTo));
	}
	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		String oldSubject = this.subject;
		this.subject = subject;
        if (eNotificationRequired() && oldSubject!=subject)
            eNotify(new ENotificationImpl(this, Notification.SET, subjectFeature.getFeatureID(),
                    oldSubject, subject));
	}
	
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		String oldBody = this.body;
		this.body = body;
        if (eNotificationRequired() && oldBody!=body)
            eNotify(new ENotificationImpl(this, Notification.SET, bodyFeature.getFeatureID(),
                    oldBody, body));
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
		String result = "[from:" + from; //$NON-NLS-1$
		result += "|tousers:"; //$NON-NLS-1$
		for (int i = 0; i < toUsers.size(); ++i) {
			result += toUsers.get(i);
			if (i + 1 < toUsers.size())
				result += ","; //$NON-NLS-1$
		}
		result += "|togroups:"; //$NON-NLS-1$
		for (int i = 0; i < toGroups.size(); ++i) {
			result += toGroups.get(i);
			if (i + 1 < toGroups.size())
				result += ","; //$NON-NLS-1$
		}
		result += "|replyTo:" + replyTo; //$NON-NLS-1$
		result += "|subject:" + subject; //$NON-NLS-1$
		result += "|body:" + body.replaceAll("\r","").replaceAll("\n", "<br />"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		result += "]@[" + expiresAt + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		return result;
	}

	/*
	 * [from:from1|tousers:user1,user2|togroups:group1,group2|replyTo:reply1|subject:Message Topic|body:First line ending with newline<br />Last line of body.]@[exp1]
	 */
	public String fromString(String string) {
		String tail = null;
		from = ""; //$NON-NLS-1$
		toUsers.clear();
		toGroups.clear();
		replyTo = ""; //$NON-NLS-1$
		subject = ""; //$NON-NLS-1$
		body = ""; //$NON-NLS-1$
		expiresAt = ""; //$NON-NLS-1$
		EStructuralFeature currentFeature = null;
		// append a newline to given string - this is used to collect
		// the tail of the string if a "^" delimiter is found.
		ExtendedStringTokenizer st = new ExtendedStringTokenizer(string + "\n", "[:,|]@^", true); //$NON-NLS-1$ //$NON-NLS-2$
		while (st.hasMoreTokens()) {
			String t = st.nextToken();
			if ("|]".contains(t)) { //$NON-NLS-1$
				currentFeature = null;
				continue;
			}
			else if ("^".equals(t)) { //$NON-NLS-1$
				tail = st.nextToken("\n"); //$NON-NLS-1$
				break;
			}
			else if (currentFeature!=null) {
				// we're currently parsing a feature
				if (currentFeature==fromFeature) {
					from += t.trim();
				}
				else if (currentFeature==toUsersFeature) {
					if (!",".equals(t)) //$NON-NLS-1$
						getToUsers().add(t.trim());
				}
				else if (currentFeature==toGroupsFeature) {
					if (!",".equals(t)) //$NON-NLS-1$
						getToGroups().add(t.trim());
				}
				else if (currentFeature==replyToFeature) {
					replyTo += t;
				}
				else if (currentFeature==subjectFeature) {
					subject += t;
				}
				else if (currentFeature==bodyFeature) {
					body += t;
					while (st.hasMoreTokens()) {
						String t1 = st.nextToken();
						if ("]".equals(t1)) { //$NON-NLS-1$
							String t2 = st.nextToken();
							if ("@".equals(t2)) { //$NON-NLS-1$
								String t3 = st.nextToken();
								if ("[".equals(t3)) { //$NON-NLS-1$
									// body is complete, expect expiresAt feature next
									currentFeature = expiresAtFeature;
									break;
								}
								else {
									// add tokens to body
									st.pushToken(t3);
									body += t1;
									body += t2;
								}
							}
							else {
								st.pushToken(t2);
								// add tokens to body;
								body += t1;
							}
						}
						else
							body += t1;
					}
				}
				else if (currentFeature==expiresAtFeature) {
					expiresAt += t;
				}
			}
			else if ("from".equals(t)) { //$NON-NLS-1$
				currentFeature = fromFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
			else if ("tousers".equals(t)) { //$NON-NLS-1$
				currentFeature = toUsersFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
			else if ("togroups".equals(t)) { //$NON-NLS-1$
				currentFeature = toGroupsFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
			else if ("replyTo".equals(t)) { //$NON-NLS-1$
				currentFeature = replyToFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
			else if ("subject".equals(t)) { //$NON-NLS-1$
				currentFeature = subjectFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
			else if ("body".equals(t)) { //$NON-NLS-1$
				currentFeature = bodyFeature;
				Assert.isTrue(":".equals(st.nextToken())); //$NON-NLS-1$
			}
		}
		
		// post processing: convert HTML break tags (<br/>) to newlines in the message body
		body = body.replaceAll("<br[ \t]*/>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		
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
