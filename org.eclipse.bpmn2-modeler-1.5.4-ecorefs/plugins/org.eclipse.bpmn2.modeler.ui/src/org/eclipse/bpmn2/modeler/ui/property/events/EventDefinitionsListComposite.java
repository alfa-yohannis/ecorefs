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
package org.eclipse.bpmn2.modeler.ui.property.events;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.CancelEventDefinition;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.ConditionalEventDefinition;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.ErrorEventDefinition;
import org.eclipse.bpmn2.EscalationEventDefinition;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.Expression;
import org.eclipse.bpmn2.IntermediateCatchEvent;
import org.eclipse.bpmn2.IntermediateThrowEvent;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.LinkEventDefinition;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.SignalEventDefinition;
import org.eclipse.bpmn2.TerminateEventDefinition;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.TimerEventDefinition;
import org.eclipse.bpmn2.Transaction;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultListComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.ListCompositeColumnProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.PropertiesCompositeFactory;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.TableColumn;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ModelSubclassSelectionDialog;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.EventDefinitionsUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.Tuple;
import org.eclipse.bpmn2.modeler.ui.property.tasks.DataAssociationDetailComposite;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class EventDefinitionsListComposite extends DefaultListComposite {
	
	protected Event event;
	
	public EventDefinitionsListComposite(Composite parent, Event event) {
		super(parent, DEFAULT_STYLE);
		this.event = event;
	}
	
	protected int createColumnProvider(EObject theobject, EStructuralFeature thefeature) {
		if (columnProvider==null) {
			getColumnProvider(theobject,thefeature);
		}
		return columnProvider.getColumns().size();
	}
	
	@Override
	protected EObject addListItem(final EObject object, EStructuralFeature feature) {
		EObject newItem = super.addListItem(object, feature);
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=417207
		// the Cancel Activity checkbox should always be TRUE
		// if the Boundary Event contains a Error Event Definition,
		// and should be hidden when it contains a Compensate Event Definition.
		if (object instanceof BoundaryEvent) {
			BoundaryEvent be = (BoundaryEvent) object;
			if (newItem instanceof ErrorEventDefinition) {
				be.setCancelActivity(true);
				((AbstractDetailComposite)getParent()).refresh();
			}
			else if (newItem instanceof CompensateEventDefinition) {
				be.setCancelActivity(false);
				((AbstractDetailComposite)getParent()).refresh();
			}
		}
		if (EventDefinitionsUtil.hasItemDefinition((EventDefinition)newItem)) {
			// create a new DataInput or DataOutput
			Tuple<ItemAwareElement,DataAssociation> param = EventDefinitionsUtil.getIOParameter((Event)object, (EventDefinition)newItem);
			param.getFirst().setId(null);
			ModelUtil.setID(param.getFirst(), object.eResource());
		}
		
//		Diagram diagram = getDiagramEditor().getDiagramTypeProvider().getDiagram();
//		IFeatureProvider fp = getDiagramEditor().getDiagramTypeProvider().getFeatureProvider();
//		PictogramElement pe = Graphiti.getLinkService().getPictogramElements(diagram, object).get(0);
//		AddContext context = new AddContext();
//		context.setTargetContainer((ContainerShape) pe);
//		context.setNewObject(newItem);
//		fp.addIfPossible(context);

//		Display.getDefault().asyncExec(new Runnable() {
//
//			@Override
//			public void run() {
//				if (getPropertySection()!=null)
//					getPropertySection().getSectionRoot().setBusinessObject(object);
//			}
//			
//		});
		return newItem;
	}

	@Override
	protected Object removeListItem(final EObject object, EStructuralFeature feature, int index) {
		Object oldItem = getListItem(object,feature,index);
		if (EventDefinitionsUtil.hasItemDefinition((EventDefinition)oldItem)) {
			// remove this DataInput or DataOutput
			Tuple<ItemAwareElement,DataAssociation> param = EventDefinitionsUtil.getIOParameter((Event)object, (EventDefinition)oldItem);
			EcoreUtil.delete(param.getFirst());
			EcoreUtil.delete(param.getSecond());
		}
		Object newItem = super.removeListItem(object, feature, index);
		if (object instanceof BoundaryEvent) {
			BoundaryEvent be = (BoundaryEvent) object;
			if (oldItem instanceof ErrorEventDefinition) {
				be.setCancelActivity(true);
				((AbstractDetailComposite)getParent()).refresh();
			}
			else if (oldItem instanceof CompensateEventDefinition) {
				be.setCancelActivity(false);
				((AbstractDetailComposite)getParent()).refresh();
			}
		}
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (getPropertySection()!=null)
					getPropertySection().getSectionRoot().setBusinessObject(object);
			}
			
		});
		return newItem;
	}

	@Override
	protected Object moveListItemUp(EObject object, EStructuralFeature feature, int index) {
		Object result = super.moveListItemUp(object, feature, index);
		Object oldItem = getListItem(object,feature,index);
		Tuple<ItemAwareElement,DataAssociation> param = EventDefinitionsUtil.getIOParameter((Event)object, (EventDefinition)oldItem);
		if (object instanceof ThrowEvent) {
			// ThrowEvent input
			ThrowEvent te = (ThrowEvent) object;
			int i = te.getDataInputs().indexOf(param.getFirst());
			if (i>0) {
				te.getDataInputs().remove(i);
				te.getDataInputs().add(i-1, (DataInput) param.getFirst());
			}
		}
		else {
			// CatchEvent output
			CatchEvent te = (CatchEvent) object;
			int i = te.getDataOutputs().indexOf(param.getFirst());
			if (i>0) {
				te.getDataOutputs().remove(i);
				te.getDataOutputs().add(i-1, (DataOutput) param.getFirst());
			}
		}
		return result;
	}

	@Override
	protected Object moveListItemDown(EObject object, EStructuralFeature feature, int index) {
		Object result = super.moveListItemDown(object, feature, index);
		Object oldItem = getListItem(object,feature,index);
		Tuple<ItemAwareElement,DataAssociation> param = EventDefinitionsUtil.getIOParameter((Event)object, (EventDefinition)oldItem);
		if (object instanceof ThrowEvent) {
			// ThrowEvent input
			ThrowEvent te = (ThrowEvent) object;
			int i = te.getDataInputs().indexOf(param.getFirst());
			if (i>=0) {
				te.getDataInputs().remove(i);
				te.getDataInputs().add(i+1, (DataInput) param.getFirst());
			}
		}
		else {
			// CatchEvent output
			CatchEvent te = (CatchEvent) object;
			int i = te.getDataOutputs().indexOf(param.getFirst());
			if (i>=0) {
				te.getDataOutputs().remove(i);
				te.getDataOutputs().add(i+1, (DataOutput) param.getFirst());
			}
		}
		return result;
	}

	@Override
	public ListCompositeColumnProvider getColumnProvider(EObject object, EStructuralFeature feature) {
		columnProvider = super.getColumnProvider(object,feature);
		columnProvider.remove(0); // get rid of the ID column
		columnProvider.add(
				new TableColumn(object,feature) {
					public String getText(Object element) {
						EObject o = (EObject)element;
						return o.eClass().getName().replace("EventDefinition", ""); //$NON-NLS-1$ //$NON-NLS-2$
					}

					@Override
					public String getHeaderText() {
						return Messages.EventDefinitionsListComposite_Event_Type_Header;
					}
					
					@Override
					public CellEditor createCellEditor (Composite parent) {
						// need to override this to avoid any problems
						return super.createCellEditor(parent);
					}
				}
			).setEditable(false);
		columnProvider.addRaw(
				new TableColumn(object,object.eClass().getEStructuralFeature("id")) { //$NON-NLS-1$
					public String getText(Object element) {
						if (element instanceof CancelEventDefinition) {
							CancelEventDefinition cancel = (CancelEventDefinition)element;
							EObject txn = cancel.eContainer();
							while (!(txn instanceof Transaction) && txn.eContainer()!=null) {
								txn = txn.eContainer();
							}
							return getTextValue(txn);
						}
						if (element instanceof CompensateEventDefinition) {
							if (((CompensateEventDefinition)element).getActivityRef()!=null)
								return ((CompensateEventDefinition)element).getActivityRef().getName();
						}
						if (element instanceof ConditionalEventDefinition) {
							if (((ConditionalEventDefinition)element).getCondition()!=null)
								return getTextValue(((ConditionalEventDefinition)element).getCondition());
						}
						if (element instanceof ErrorEventDefinition) {
							if (((ErrorEventDefinition)element).getErrorRef()!=null)
								return ((ErrorEventDefinition)element).getErrorRef().getName();
						}
						if (element instanceof EscalationEventDefinition) {
							if (((EscalationEventDefinition)element).getEscalationRef()!=null)
								return ((EscalationEventDefinition)element).getEscalationRef().getName();
						}
						if (element instanceof LinkEventDefinition) {
							String text = ""; //$NON-NLS-1$
							LinkEventDefinition link = (LinkEventDefinition)element;
							Event event = (Event) link.eContainer();
							if (event instanceof CatchEvent) {
								int size = link.getSource().size();
								for (int i=0; i<size; ++i) {
									LinkEventDefinition source = link.getSource().get(i);
									text += getTextValue(source.eContainer());
									if (i<size-1)
										text += ", "; //$NON-NLS-1$
								}
								if (!text.isEmpty()) {
									text += " -> "; //$NON-NLS-1$
									text += getTextValue(link.eContainer());
									return text;
								}
							}
							else if (event instanceof ThrowEvent) {
								if (link.getTarget()!=null) {
									text += getTextValue(link.eContainer());
									text += " -> "; //$NON-NLS-1$
									LinkEventDefinition target = link.getTarget();
									text += getTextValue(target.eContainer());
									return text;
								}
							}
						}
						if (element instanceof MessageEventDefinition) {
							if (((MessageEventDefinition)element).getMessageRef()!=null)
								return ((MessageEventDefinition)element).getMessageRef().getName();
						}
						if (element instanceof SignalEventDefinition) {
							if (((SignalEventDefinition)element).getSignalRef()!=null)
								return ((SignalEventDefinition)element).getSignalRef().getName();
						}
						if (element instanceof TerminateEventDefinition) {
						}
						if (element instanceof TimerEventDefinition) {
							Expression exp = ((TimerEventDefinition)element).getTimeDate();
							if (exp!=null)
								return Messages.TimerEventDefinitionDetailComposite_Time_Date + ": " + //$NON-NLS-1$
										getTextValue(exp);
							exp = ((TimerEventDefinition)element).getTimeCycle();
							if (exp!=null)
								return Messages.TimerEventDefinitionDetailComposite_Interval + ": " + //$NON-NLS-1$
									getTextValue(exp);
							exp = ((TimerEventDefinition)element).getTimeDuration();
							if (exp!=null)
								return Messages.TimerEventDefinitionDetailComposite_Duration + ": " + //$NON-NLS-1$
									getTextValue(exp);
						}
						
						// fallback: if the object has an ExtendedPropertiesAdapter, use the text value
						// provided by that adapter.
						ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(element);
						if (adapter!=null) {
							String textValue = adapter.getObjectDescriptor().getTextValue();
							if (textValue!=null)
								return textValue;
						}
								
						return Messages.EventDefinitionsListComposite_None_Label;
					}

					@Override
					public String getHeaderText() {
						return Messages.EventDefinitionsListComposite_Event_ID_Header;
					}
					
					@Override
					public CellEditor createCellEditor (Composite parent) {
						// need to override this to avoid any problems
						return super.createCellEditor(parent);
					}
					
					private String getTextValue(EObject object) {
						return ExtendedPropertiesProvider.getTextValue(object);
					}
					
				}
			).setEditable(false);
		return columnProvider;
	}

	protected List<EClass> getAllowedEventDefinitions(Event event, Object parentContainer) {
		return FeatureSupport.getAllowedEventDefinitions(event, parentContainer);
	}

	@Override
	public EClass getListItemClassToAdd(EClass listItemClass) {
		EClass eclass = null;
		ModelSubclassSelectionDialog dialog = new ModelSubclassSelectionDialog(getDiagramEditor(), businessObject, feature) {
			@Override
			protected void filterList(List<EClass> items) {
				List<EClass> filteredItems = new ArrayList<EClass>();
				List<EClass> allowedItems = getAllowedEventDefinitions(event, null);
				for (EClass eclass : items) {
					if (allowedItems.contains(eclass)) {
						boolean skip = false;
						if (eclass.getInstanceClass() == LinkEventDefinition.class) {
							// only allow one Link Event Definition
							if (businessObject instanceof IntermediateCatchEvent) {
								for (EventDefinition ed : ((IntermediateCatchEvent) businessObject).getEventDefinitions()) {
									if (ed instanceof LinkEventDefinition)
										skip = true;
								}
							}
							else if (businessObject instanceof IntermediateThrowEvent) {
								for (EventDefinition ed : ((IntermediateThrowEvent) businessObject).getEventDefinitions()) {
									if (ed instanceof LinkEventDefinition)
										skip = true;
								}
							}
						}
						if (!skip)
							filteredItems.add(eclass);
					}
				}
				items.clear();
				items.addAll(filteredItems);
			}
		};
		
		if (dialog.open()==Window.OK){
			eclass = (EClass)dialog.getResult()[0];
		}
		return eclass;
	}
	
	public AbstractDetailComposite createDetailComposite(Class eClass, Composite parent, int style) {
		TargetRuntime rt = TargetRuntime.getRuntime(event);
		AbstractDetailComposite detailComposite = PropertiesCompositeFactory.INSTANCE.createDetailComposite(eClass, parent, rt, style);
		if (detailComposite!=null)
			return detailComposite;
		
		if (eClass==TimerEventDefinition.class) {
			return new TimerEventDefinitionDetailComposite(parent, style);
		}
		if (eClass==ConditionalEventDefinition.class){
			return new ConditionalEventDefinitionDetailComposite(parent, style);
		}
		return new EventDefinitionsDetailComposite(parent, (Event)getBusinessObject());
	}
	
	public class EventDefinitionsDetailComposite extends DefaultDetailComposite {

		protected Event event;
		protected EventDefinition eventDefinition;
		protected DataAssociationDetailComposite dataAssociationComposite;

		public EventDefinitionsDetailComposite(Composite parent, Event event) {
			super(parent, SWT.NONE);
			this.event = event;
		}

		@Override
		protected void cleanBindings() {
			super.cleanBindings();
			dataAssociationComposite = null;
		}

		@Override
		public void createBindings(EObject be) {
			super.createBindings(be);
			
			eventDefinition = (EventDefinition) be;
			
			if (dataAssociationComposite==null) {
				dataAssociationComposite = new DataAssociationDetailComposite(getAttributesParent(), SWT.NONE);
			}
			
			if (EventDefinitionsUtil.hasItemDefinition(eventDefinition)) {
				dataAssociationComposite.setVisible(true);
				if (event instanceof ThrowEvent)
					dataAssociationComposite.setShowToGroup(false);
				else
					dataAssociationComposite.setShowFromGroup(false);
				
				// determine the correct I/O Parameter (DataInput or DataOutput) for this Event Definition
				Tuple<ItemAwareElement,DataAssociation> param = null;
				try {
					param = EventDefinitionsUtil.getIOParameter(event, eventDefinition);
				}
				catch (IllegalStateException e) {
					// The model is corrupt because it is missing one or more required BPMN2 model elements.
					// Create a transaction to add the missing elements.
					final Object result[] = new Object[1];
					TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
					domain.getCommandStack().execute(new RecordingCommand(domain) {
						@Override
						protected void doExecute() {
							result[0] = EventDefinitionsUtil.getIOParameter(event, eventDefinition);
						}
					});
					param = (Tuple<ItemAwareElement, DataAssociation>) result[0];
				}
				if (param!=null) {
					dataAssociationComposite.setBusinessObject(param.getFirst());
					String type = eventDefinition.eClass().getName().replace("EventDefinition", ""); //$NON-NLS-1$ //$NON-NLS-2$
					if (event instanceof ThrowEvent) {
						dataAssociationComposite.getFromGroup().setText(
							NLS.bind(
								Messages.EventDefinitionsListComposite_Map_Outgoing,
								type
							)
						);
					}
					else {
						dataAssociationComposite.getToGroup().setText(
							NLS.bind(
								Messages.EventDefinitionsListComposite_Map_Incoming,
								type
							)
						);
					}
				}
			}
			else {
				dataAssociationComposite.setVisible(false);
			}
		}
		
		public DataAssociationDetailComposite getDataAssociationComposite() {
			return dataAssociationComposite;
		}
	}
}