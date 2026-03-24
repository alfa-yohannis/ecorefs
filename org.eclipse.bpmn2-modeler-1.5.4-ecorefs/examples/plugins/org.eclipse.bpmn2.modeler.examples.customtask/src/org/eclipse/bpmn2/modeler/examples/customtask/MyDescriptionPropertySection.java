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

package org.eclipse.bpmn2.modeler.examples.customtask;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.ExtensionAttributeValue;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.TextObjectEditor;
import org.eclipse.bpmn2.modeler.examples.customtask.MyModel.MetaData;
import org.eclipse.bpmn2.modeler.examples.customtask.MyModel.MyModelFactory;
import org.eclipse.bpmn2.modeler.examples.customtask.MyModel.MyModelPackage;
import org.eclipse.bpmn2.modeler.ui.property.DescriptionPropertySection;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 *
 */
public class MyDescriptionPropertySection extends DescriptionPropertySection {

	final static EClass METADATA_CLASS = MyModelPackage.eINSTANCE.getMetaData();
	final static EStructuralFeature METADATA_FEATURE = MyModelPackage.eINSTANCE.getDocumentRoot_MetaData();
	final static EStructuralFeature METADATA_NAME = MyModelPackage.eINSTANCE.getMetaData_Name();
	final static EStructuralFeature METADATA_VALUE = MyModelPackage.eINSTANCE.getMetaData_Value();
	
	public MyDescriptionPropertySection() {
		super();
	}

	@Override
	protected AbstractDetailComposite createSectionRoot() {
		return new MyDescriptionPropertyComposite(this);
	}

	@Override
	public AbstractDetailComposite createSectionRoot(Composite parent, int style) {
		// TODO Auto-generated method stub
		return super.createSectionRoot(parent, style);
	}

	class MyDescriptionPropertyComposite extends DescriptionDetailComposite {

		public MyDescriptionPropertyComposite(AbstractBpmn2PropertySection section) {
			super(section);
		}

		public MyDescriptionPropertyComposite(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		public void createBindings(final EObject be) {
			super.createBindings(be);
			// in our example, all BaseElements may have a MetaData extension element
			if (be instanceof BaseElement)
				bindMetaData((BaseElement)be);
		}

		protected void bindMetaData(final BaseElement be) {
			if (isModelObjectEnabled(METADATA_CLASS)) {
				// create a new Property Tab section with a twistie
				Composite section = createSectionComposite(this, "Metadata");
				// get the MetaData object from this BaseElement's extension elements
				MetaData metaData = (MetaData) findExtensionAttributeValue(be, METADATA_FEATURE);
				if (metaData==null) {
					// the BaseElement does not have one
					// create a button to add a new one
					Button button = toolkit.createButton(section, "Add Metadata", SWT.PUSH);
					// since the Property Tab composite has 3 columns, we need to add some fillers
					toolkit.createLabel(section, ""); //$NON-NLS-1$
					toolkit.createLabel(section, ""); //$NON-NLS-1$
					button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							// create the new MetaData and insert it into the
							// BaseElement's extension elements container
							// Note that this has to be done inside a transaction
							TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
							domain.getCommandStack().execute(new RecordingCommand(domain) {
								@Override
								protected void doExecute() {
									MetaData metaData = MyModelFactory.eINSTANCE.createMetaData();
									metaData.setValue("");
									addExtensionAttributeValue(be, METADATA_FEATURE, metaData);
									setBusinessObject(be);
								}
							});
						}
					});
				}
				else {
					// create text editors for the MetaData name and value
					TextObjectEditor nameEditor = new TextObjectEditor(this, metaData, METADATA_NAME);
					TextObjectEditor valueEditor = new TextObjectEditor(this, metaData, METADATA_VALUE);
					valueEditor.setMultiLine(true);
					nameEditor.createControl(section, "Name");
					valueEditor.createControl(section, "Value");

					// create a button (with fillers) to remove this MetaData
					toolkit.createLabel(section, ""); //$NON-NLS-1$
					Button button = toolkit.createButton(section, "Remove Metadata", SWT.PUSH);
					toolkit.createLabel(section, ""); //$NON-NLS-1$
					button.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							// remove the MetaData object from this BaseElement
							TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
							domain.getCommandStack().execute(new RecordingCommand(domain) {
								@Override
								protected void doExecute() {
									removeExtensionAttributeValue(be, METADATA_FEATURE);
									setBusinessObject(be);
								}
							});
						}
					});
				}
			}
		}
		
		/**
		 * Find the first entry in this BaseElement's extension elements container
		 * that matches the given structural feature.
		 * 
		 * @param be a BaseElement
		 * @param feature the structural feature to search for
		 * @return the value of the extension element
		 */
		Object findExtensionAttributeValue(BaseElement be, EStructuralFeature feature) {
			for (ExtensionAttributeValue eav : be.getExtensionValues()) {
				for (FeatureMap.Entry entry : eav.getValue()) {
					if (entry.getEStructuralFeature() == feature) {
						return entry.getValue();
					}
				}
			}
			return null;
		}

		/**
		 * Add a new extension element to the given BaseElement.
		 * 
		 * @param be a BaseElement
		 * @param feature the structural feature
		 */
		void addExtensionAttributeValue(BaseElement be, EStructuralFeature feature, Object value) {
			ExtensionAttributeValue eav = null;
			if (be.getExtensionValues().size()>0) {
				// reuse the <bpmn2:extensionElements> container if this BaseElement already has one
				eav = be.getExtensionValues().get(0);
			}
			else {
				// otherwise create a new one
				eav = Bpmn2Factory.eINSTANCE.createExtensionAttributeValue();
				be.getExtensionValues().add(eav);
			}
			eav.getValue().add(feature, value);
		}
		
		/**
		 * Remove an extension element from the given BaseElement that matches the
		 * given structural feature.
		 * 
		 * @param be a BaseElement
		 * @param feature the structural feature
		 */
		void removeExtensionAttributeValue(BaseElement be, EStructuralFeature feature) {
			for (ExtensionAttributeValue eav : be.getExtensionValues()) {
				for (FeatureMap.Entry entry : eav.getValue()) {
					if (entry.getEStructuralFeature() == feature) {
						be.getExtensionValues().remove(eav);
						return;
					}
				}
			}
		}
	}
}
