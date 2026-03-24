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

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.ListCompositeColumnProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.TableColumn;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.TextObjectEditor;
import org.eclipse.bpmn2.modeler.core.model.ModelDecorator;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.DroolsFactory;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.DroolsPackage;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.ExternalProcess;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.MetaDataType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.MetaValueType;
import org.eclipse.bpmn2.modeler.ui.property.DescriptionPropertySection;
import org.eclipse.bpmn2.modeler.ui.property.ExtensionValueListComposite;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * This is an empty tab section which simply exists to hide the "Basic" tab
 * defined the editor UI plugin.
 * 
 * @author Bob Brodt
 *
 */
public class JbpmDescriptionPropertySection extends DescriptionPropertySection {

	MetaDataTable metaDataTable = null;

	class MetaDataTable extends ExtensionValueListComposite {
		BaseElement be;
		
		public MetaDataTable(Composite parent, BaseElement be) {
			super(parent, DEFAULT_STYLE);
			this.be = be;
		}

		@Override
		protected EObject addListItem(EObject object, EStructuralFeature feature) {
			// generate a unique global variable name
			String base = "elementname"; //$NON-NLS-1$
			int suffix = 1;
			String name = base + suffix;
			for (;;) {
				boolean found = false;
				for (Object md : ModelDecorator.getAllExtensionAttributeValues(object, MetaDataType.class)) {
					if (name.equals(((MetaDataType) md).getName())) {
						found = true;
						break;
					}
				}
				if (!found)
					break;
				name = base + ++suffix;
			}

			MetaDataType newData = DroolsFactory.eINSTANCE.createMetaDataType();
			MetaValueType newValue = DroolsFactory.eINSTANCE.createMetaValueType();
			newValue.setValue("");
			newData.setName(name);
			newData.setMetaValue(newValue);

			// This editor lifecycle event is normally triggered by the Bpmn2ModelerFactory
			// when an object is created. We need to send this event from here to make sure
			// a {@see ProcessVariableNameChangeAdapter} is attached to the object.
			TargetRuntime rt = TargetRuntime.getRuntime(object);
			LifecycleEvent.notify(EventType.BUSINESSOBJECT_CREATED, newData, rt);

			addExtensionValue(newData);
			return newData;
		}
		
		protected int createColumnProvider(EObject theobject, EStructuralFeature thefeature) {
			if (columnProvider==null) {
				getColumnProvider(theobject,thefeature);
			}
			return columnProvider.getColumns().size();
		}

		@Override
		public ListCompositeColumnProvider getColumnProvider(EObject object, EStructuralFeature feature) {
			columnProvider = super.getColumnProvider(object, feature);
			columnProvider.addRaw(new TableColumn(object, object.eClass().getEStructuralFeature("id")) { //$NON-NLS-1$
				public String getText(Object element) {
					String text = null;
					if (element instanceof MetaDataType) {
						MetaDataType metaData = (MetaDataType) element;
						MetaValueType metaValue = metaData.getMetaValue();
						if (metaValue!=null)
							text = metaValue.getValue();
					}
					return text==null ? "" : text;
				}

				@Override
				public String getHeaderText() {
					return "Value";
				}

				@Override
				public CellEditor createCellEditor(Composite parent) {
					// need to override this to avoid any problems
					return null;
				}

			}).setEditable(true);
			return columnProvider;
		}

		@Override
		public AbstractDetailComposite createDetailComposite(Class eClass, final Composite parent, int style) {
			AbstractDetailComposite detailComposite = new DefaultDetailComposite(parent, style) {

				@Override
				public void createBindings(EObject be) {
					super.createBindings(be);
					if (be instanceof MetaDataType) {
						MetaDataType metaData = (MetaDataType) be;
						MetaValueType metaValue = metaData.getMetaValue();
						ObjectEditor editor = new TextObjectEditor(this,metaValue,DroolsPackage.eINSTANCE.getMetaValueType_Value());
						editor.createControl(this.getAttributesParent(),"Value");
					}
				}
				
			};
			return detailComposite;

		}
	}

	@Override
	protected AbstractDetailComposite createSectionRoot() {
		return new JbpmDescriptionPropertyComposite(this);
	}

	@Override
	public AbstractDetailComposite createSectionRoot(Composite parent, int style) {
		return new JbpmDescriptionPropertyComposite(parent, style);
	}

	public JbpmDescriptionPropertySection() {
		super();
	}

	public class JbpmDescriptionPropertyComposite extends DescriptionDetailComposite {

		public JbpmDescriptionPropertyComposite(AbstractBpmn2PropertySection section) {
			super(section);
		}

		public JbpmDescriptionPropertyComposite(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		public void cleanBindings() {
			super.cleanBindings();
			metaDataTable = null;
		}

		@Override
		public void createBindings(final EObject be) {
			// for BPMNDiagram objects, pick out the Process and render the
			// Process attributes
			Process process = null;
			if (be instanceof Participant) {
				process = ((Participant) be).getProcessRef();
			} else if (be instanceof BPMNDiagram) {
				BaseElement bpmnElement = ((BPMNDiagram) be).getPlane().getBpmnElement();
				if (bpmnElement instanceof Process)
					process = (Process) bpmnElement;
			} else if (be instanceof ExternalProcess) {
				// TODO: hook into jBPM guvnor instance to fetch additional info
				// maybe?
				bindAttribute(be, "name"); //$NON-NLS-1$
				return;
			}

			if (process == null) {
				// display the default Description tab
				super.createBindings(be);
			} else {
				// create our own for Process
				bindDescription(be);

				bindAttribute(process, "name"); //$NON-NLS-1$
				// bindAttribute(process, "id");
				// bindAttribute(process, "anyAttribute");
				// bindAttribute(process, "isExecutable");
				bindList(process, "documentation"); //$NON-NLS-1$
				// bindAttribute(process, "adHoc");
				// bindList(process, "properties"); // this has moved to
				// JbpmDataItemsDetailComposite
				// bindList(process, "laneSets"); // don't need this
			}
			if (be instanceof BaseElement) {
				Bpmn2Preferences preferences = (Bpmn2Preferences) getDiagramEditor().getAdapter(Bpmn2Preferences.class);
				if (preferences.getShowAdvancedPropertiesTab()) {
					bindMetaDataTable((BaseElement) be);
				}
			}
		}

		@Override
		protected void bindAppearance(EObject be) {
			// TODO: support the color/appearance extensions defined by jBPM Web Designer
		}

		protected void bindMetaDataTable(final BaseElement be) {
			metaDataTable = new MetaDataTable(this, be);
			metaDataTable.bindList(be, DroolsPackage.eINSTANCE.getDocumentRoot_MetaData());
			metaDataTable.setTitle(Messages.JbpmDescriptionPropertySection_MetaData_Section_Title);

		}
	}
}
