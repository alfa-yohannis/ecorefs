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

import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractListComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultListComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.ListCompositeColumnProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.ListCompositeContentProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.TableColumn;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.TextAndButtonObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.TextObjectEditor;
import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;

public class TaskNotificationsListComposite extends DefaultListComposite {

	protected TaskNotificationList mylist;
	protected boolean updateList;
	protected Task task;

	class NotificationTypeTableColumn extends TableColumn {
		public NotificationTypeTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_Type());
			setHeaderText(Messages.TaskNotificationsListComposite_Type_Header);
			setEditable(true);
		}
	}

	class NotificationFromTableColumn extends TableColumn {
		public NotificationFromTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_From());
			setHeaderText(Messages.TaskNotificationsListComposite_From_Header);
			setEditable(true);
		}
	}

	class NotificationUsersTableColumn extends TableColumn {
		public NotificationUsersTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_ToUsers());
			setHeaderText(Messages.TaskNotificationsListComposite_ToUsers_Header);
			setEditable(false);
		}

		@Override
		public String getText(Object element) {
			return ((TaskNotification)element).getToUsersAsString();
		}
	}

	class NotificationGroupsTableColumn extends TableColumn {
		public NotificationGroupsTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_ToGroups());
			setHeaderText(Messages.TaskNotificationsListComposite_ToGroups_Header);
			setEditable(false);
		}

		@Override
		public String getText(Object element) {
			return ((TaskNotification)element).getToGroupsAsString();
		}
	}

	class NotificationReplyToTableColumn extends TableColumn {
		public NotificationReplyToTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_ReplyTo());
			setHeaderText(Messages.TaskNotificationsListComposite_ReplyTo_Header);
			setEditable(true);
		}
	}

	class NotificationSubjectTableColumn extends TableColumn {
		public NotificationSubjectTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_Subject());
			setHeaderText(Messages.TaskNotificationsListComposite_Subject_Header);
			setEditable(true);
		}
	}

	class NotificationBodyTableColumn extends TableColumn {
		public NotificationBodyTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_Body());
			setHeaderText(Messages.TaskNotificationsListComposite_Body_Header);
			setEditable(true);
		}
	}

	class NotificationExpiresAtTableColumn extends TableColumn {
		public NotificationExpiresAtTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskNotification_ExpiresAt());
			setHeaderText(Messages.TaskNotificationsListComposite_ExpiresAt_Header);
			setEditable(true);
		}
	}

	public TaskNotificationsListComposite(Composite parent, final Task task) {
		super(parent, AbstractListComposite.DEFAULT_STYLE);
		this.task = task;
	}

	@Override
	public EClass getListItemClass(EObject object, EStructuralFeature feature) {
		return TaskExtensionsFactory.eINSTANCE.getTaskNotification();
	}

	@Override
	protected int createColumnProvider(EObject object, EStructuralFeature feature) {
		if (columnProvider==null) {
			columnProvider = new ListCompositeColumnProvider(this);
			columnProvider.addRaw( new NotificationTypeTableColumn(object) );
			columnProvider.addRaw( new NotificationFromTableColumn(object) );
			columnProvider.addRaw( new NotificationUsersTableColumn(object) );
			columnProvider.addRaw( new NotificationGroupsTableColumn(object) );
			columnProvider.addRaw( new NotificationReplyToTableColumn(object) );
			columnProvider.addRaw( new NotificationSubjectTableColumn(object) );
			columnProvider.addRaw( new NotificationBodyTableColumn(object) );
			columnProvider.addRaw( new NotificationExpiresAtTableColumn(object) );
		}
		return columnProvider.getColumns().size();
	}

	@Override
	public ListCompositeContentProvider getContentProvider(EObject object, EStructuralFeature feature, EList<EObject>list) {
		if (contentProvider==null) {
			contentProvider = new ListCompositeContentProvider(this, object, feature, list) {

				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					if (oldInput!=newInput || updateList) {
						
						getItemList();
						Task task = (Task)object;
						
						// rebuild the list based on current task data inputs
						TaskNotificationList newlist = new TaskNotificationList();
						newlist.setTask(task);
						for (DataInputAssociation dia : task.getDataInputAssociations()) {
							DataInput di = (DataInput)dia.getTargetRef();
							if (di!=null) {
								String type = di.getName();
								if (NotificationType.get(type) != null) {
									FormalExpression exp = (FormalExpression)dia.getAssignment().get(0).getFrom();
									newlist.add(type, exp.getBody());
								}
							}
						}
						
						// compare it with the new list
						for (int index = 0; index<newlist.size(); ++index) {
							TaskNotification tn = newlist.get(index);
							mylist.replace(index, tn.getType().getLiteral(), tn.toString());
						}
						object = listComposite.getBusinessObject();
						updateList = false;
					}
				}

				@Override
				public Object[] getElements(Object inputElement) {
					return getItemList().toArray();
				}
			};
		}
		return contentProvider;
	}

	public AbstractDetailComposite createDetailComposite(Class eClass, Composite parent, int style) {
		AbstractDetailComposite composite = new DefaultDetailComposite(parent, style) {
			
			protected Composite bindFeature(EObject be, EStructuralFeature feature, EClass eItemClass) {
				final TaskNotification tn = (TaskNotification) be;
				if (TaskExtensionsFactory.TASKNOTIFICATION_TO_USERS_FEATURE_NAME.equals(feature.getName())) {
					TextObjectEditor editor = new TextAndButtonObjectEditor(this, be, feature) {

						@Override
						protected void buttonClicked(int buttonId) {
							IInputValidator validator = new IInputValidator() {

								@Override
								public String isValid(String newText) {
									if (newText == null || newText.isEmpty())
										return null;
									for (String s : newText.split(",")) { //$NON-NLS-1$
										s = s.trim();
										if (!s.isEmpty() && !SyntaxCheckerUtils.isNCName(s)) {
											return NLS.bind(Messages.TaskNotificationsListComposite_UserName_Error, s);
										}
									}
									return null;
								}

							};

							String initialValue = tn.getToUsersAsString();
							InputDialog dialog = new InputDialog(getShell(),
									Messages.TaskNotificationsListComposite_EditUsers_Title,
									Messages.TaskNotificationsListComposite_EditUsers_Message,
									initialValue,
									validator);
							if (dialog.open() == Window.OK) {
								setValue(dialog.getValue());
							}
						}

						@Override
						public Object getValue() {
							return tn.getToUsersAsString();
						}

						@Override
						protected String getText() {
							return tn.getToUsersAsString();
						}

						@Override
						public boolean setValue(final Object result) {
							TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
							domain.getCommandStack().execute(new RecordingCommand(domain) {
								@Override
								protected void doExecute() {
									tn.setToUsers((String) result);
								}
							});
							updateText();
							return true;
						}
					};
					editor.createControl(this, Messages.TaskNotificationsListComposite_ToUsers_Label);
					editor.setEditable(false);
					return null;
				}
				if (TaskExtensionsFactory.TASKNOTIFICATION_TO_GROUPS_FEATURE_NAME.equals(feature.getName())) {
					TextObjectEditor editor = new TextAndButtonObjectEditor(this, be, feature) {

						@Override
						protected void buttonClicked(int buttonId) {
							IInputValidator validator = new IInputValidator() {

								@Override
								public String isValid(String newText) {
									if (newText == null || newText.isEmpty())
										return null;
									for (String s : newText.split(",")) { //$NON-NLS-1$
										s = s.trim();
										if (!s.isEmpty() && !SyntaxCheckerUtils.isNCName(s)) {
											return NLS.bind(Messages.TaskNotificationsListComposite_GroupName_Error, s);
										}
									}
									return null;
								}

							};

							String initialValue = tn.getToGroupsAsString();
							InputDialog dialog = new InputDialog(getShell(),
									Messages.TaskNotificationsListComposite_EditGroups_Title,
									Messages.TaskNotificationsListComposite_EditGroups_Message,
									initialValue,
									validator);
							if (dialog.open() == Window.OK) {
								setValue(dialog.getValue());
							}
						}

						@Override
						public Object getValue() {
							return tn.getToGroupsAsString();
						}

						@Override
						protected String getText() {
							return tn.getToGroupsAsString();
						}

						@Override
						public boolean setValue(final Object result) {
							TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
							domain.getCommandStack().execute(new RecordingCommand(domain) {
								@Override
								protected void doExecute() {
									tn.setToGroups((String) result);
								}
							});
							updateText();
							return true;
						}
					};
					editor.createControl(this, Messages.TaskNotificationsListComposite_ToGroups_Label);
					editor.setEditable(false);
					return null;
				}
				if (TaskExtensionsFactory.TASKNOTIFICATION_BODY_FEATURE_NAME.equals(feature.getName())) {
					TextObjectEditor editor = new TextObjectEditor(this, be, feature) {
						@Override
						protected boolean isMultiLineText() {
							return true;
						}
					};
					editor.setMultiLine(true);
					editor.createControl(this, Messages.TaskNotificationsListComposite_Body_Label);
					return null;
				}
				return super.bindFeature(be, feature, eItemClass);
			}

			@Override
			protected boolean isModelObjectEnabled(EClass eclass, EStructuralFeature feature) {
				return true;
			}

		};
		return composite;
	}
	
	protected EList<EObject> getItemList() {
		if (mylist==null) {
			mylist = new TaskNotificationList();
			mylist.setTask(task);
		}
		return mylist;
	}
	
	protected EObject addListItem(EObject object, EStructuralFeature feature) {
		TaskNotification newItem = TaskExtensionsFactory.eINSTANCE.createTaskNotification();
		getItemList().add(newItem);
		mylist.updateTask();
		return newItem;
	}

	@Override
	protected Object removeListItem(EObject object, EStructuralFeature feature, int index) {
		updateList = true;
		Object result = super.removeListItem(object, feature, index);
		mylist.updateTask();
		return result;
	}

	@Override
	protected Object moveListItemUp(EObject object, EStructuralFeature feature, int index) {
		updateList = true;
		Object result = super.moveListItemUp(object, feature, index);
		mylist.updateTask();
		return result;
	}

	@Override
	protected Object moveListItemDown(EObject object, EStructuralFeature feature, int index) {
		updateList = true;
		Object result = super.moveListItemDown(object, feature, index);
		mylist.updateTask();
		return result;
	}
}