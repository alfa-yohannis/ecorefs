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

public class TaskReassignmentsListComposite extends DefaultListComposite {

	protected TaskReassignmentList mylist;
	protected boolean updateList;
	protected Task task;
	
	class ReassignmentTypeTableColumn extends TableColumn {
		public ReassignmentTypeTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskReassignment_Type());
			setHeaderText(Messages.TaskReassignmentsListComposite_Type_Header);
			setEditable(true);
		}

		@Override
		public String getText(Object element) {
			return ((TaskReassignment)element).getType().getLiteral();
		}
	}

	class ReassignmentExpiresAtTableColumn extends TableColumn {
		public ReassignmentExpiresAtTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskReassignment_ExpiresAt());
			setHeaderText(Messages.TaskReassignmentsListComposite_ExpiresAt_Header);
			setEditable(true);
		}

		@Override
		public String getText(Object element) {
			return ((TaskReassignment)element).getExpiresAt();
		}
	}

	class ReassignmentUsersTableColumn extends TableColumn {
		public ReassignmentUsersTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskReassignment_Users());
			setHeaderText(Messages.TaskReassignmentsListComposite_Users_Header);
			setEditable(false);
		}

		@Override
		public String getText(Object element) {
			return ((TaskReassignment)element).getUsersAsString();
		}
	}

	class ReassignmentGroupsTableColumn extends TableColumn {
		public ReassignmentGroupsTableColumn(EObject object) {
			super(object, TaskExtensionsFactory.eINSTANCE.getTaskReassignment_Groups());
			setHeaderText(Messages.TaskReassignmentsListComposite_Groups_Header);
			setEditable(false);
		}

		@Override
		public String getText(Object element) {
			return ((TaskReassignment)element).getGroupsAsString();
		}
	}

	public TaskReassignmentsListComposite(Composite parent, final Task task) {
		super(parent, AbstractListComposite.DEFAULT_STYLE);
		this.task = task;
	}

	@Override
	public EClass getListItemClass(EObject object, EStructuralFeature feature) {
		return TaskExtensionsFactory.eINSTANCE.getTaskReassignment();
	}

	@Override
	protected int createColumnProvider(EObject object, EStructuralFeature feature) {
		if (columnProvider==null) {
			columnProvider = new ListCompositeColumnProvider(this);
			columnProvider.addRaw( new ReassignmentTypeTableColumn(object));
			columnProvider.addRaw( new ReassignmentUsersTableColumn(object));
			columnProvider.addRaw( new ReassignmentGroupsTableColumn(object));
			columnProvider.addRaw( new ReassignmentExpiresAtTableColumn(object));
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
						TaskReassignmentList newlist = new TaskReassignmentList();
						newlist.setTask(task);
						for (DataInputAssociation dia : task.getDataInputAssociations()) {
							DataInput di = (DataInput)dia.getTargetRef();
							if (di!=null) {
								String type = di.getName();
								if (ReassignmentType.get(type) != null) {
									FormalExpression exp = (FormalExpression)dia.getAssignment().get(0).getFrom();
									newlist.add(type, exp.getBody());
								}
							}
						}
						
						// compare it with the new list
						for (int index = 0; index<newlist.size(); ++index) {
							TaskReassignment tr = newlist.get(index);
							mylist.replace(index, tr.getType().getLiteral(), tr.toString());
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
				final TaskReassignment tr = (TaskReassignment) be;
				if ("users".equals(feature.getName())) { //$NON-NLS-1$
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
											return NLS.bind(Messages.TaskReassignmentsListComposite_InvalidUserName, s);
										}
									}
									return null;
								}

							};

							String initialValue = tr.getUsersAsString();
							InputDialog dialog = new InputDialog(getShell(),
									Messages.TaskReassignmentsListComposite_EditUsers_Title,
									Messages.TaskReassignmentsListComposite_EditUsers_Message,
									initialValue,
									validator);
							if (dialog.open() == Window.OK) {
								setValue(dialog.getValue());
							}
						}

						@Override
						public Object getValue() {
							return tr.getUsersAsString();
						}

						@Override
						protected String getText() {
							return tr.getUsersAsString();
						}

						@Override
						public boolean setValue(final Object result) {
							TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
							domain.getCommandStack().execute(new RecordingCommand(domain) {
								@Override
								protected void doExecute() {
									tr.setUsers((String) result);
								}
							});
							updateText();
							return true;
						}
					};
					editor.createControl(this, Messages.TaskReassignmentsListComposite_Users_Label);
					editor.setEditable(false);
					return null;
				}
				if ("groups".equals(feature.getName())) { //$NON-NLS-1$
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
											return NLS.bind(Messages.TaskReassignmentsListComposite_InvalidGroupName, s);
										}
									}
									return null;
								}

							};

							String initialValue = tr.getGroupsAsString();
							InputDialog dialog = new InputDialog(getShell(),
									Messages.TaskReassignmentsListComposite_EditGroups_Title,
									Messages.TaskReassignmentsListComposite_EditGroups_Message,
									initialValue,
									validator);
							if (dialog.open() == Window.OK) {
								setValue(dialog.getValue());
							}
						}

						@Override
						public Object getValue() {
							return tr.getGroupsAsString();
						}

						@Override
						protected String getText() {
							return tr.getGroupsAsString();
						}

						@Override
						public boolean setValue(final Object result) {
							TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
							domain.getCommandStack().execute(new RecordingCommand(domain) {
								@Override
								protected void doExecute() {
									tr.setGroups((String) result);
								}
							});
							updateText();
							return true;
						}
					};
					editor.createControl(this, Messages.TaskReassignmentsListComposite_Groups_Label);
					editor.setEditable(false);
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
			mylist = new TaskReassignmentList();
			mylist.setTask(task);
		}
		return mylist;
	}
	
	protected EObject addListItem(EObject object, EStructuralFeature feature) {
		TaskReassignment newItem = TaskExtensionsFactory.eINSTANCE.createTaskReassignment();
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