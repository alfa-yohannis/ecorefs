/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.property.editors;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ComboObjectEditor;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NameAndURIObjectEditor extends ComboObjectEditor {

	protected String preferenceKey;
	
	public NameAndURIObjectEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object, feature);
	}

	public NameAndURIObjectEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature,
			EClass featureEType) {
		super(parent, object, feature, featureEType);
	}
	
	public void setPreferenceKey(String key) {
		this.preferenceKey = key;
	}
	
	protected boolean canEdit() {
		if (comboViewer==null)
			return true;
		
		ISelection selection = comboViewer.getSelection();
		if (selection instanceof StructuredSelection) {
			String firstElement = (String) ((StructuredSelection) selection).getFirstElement();
			if ((firstElement != null && firstElement.isEmpty())) {
				// nothing to edit
				firstElement = null;
			}
			if (firstElement != null && comboViewer.getData(firstElement) instanceof EObject) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean canCreateNew() {
		return true;
	}

	@Override
	public boolean setValue(Object result) {
		if (ModelUtil.isStringWrapper(result)) {
			result = ModelUtil.getStringWrapperValue(result);
		}
		return super.setValue(result);
	}
	
	public Object getValue() {
		Object value = object.eGet(feature);
		if (value==null)
			return ""; //$NON-NLS-1$
		Hashtable<String,Object> choices = getChoiceOfValues(object, feature);
		for (Entry<String, Object> entry : choices.entrySet()) {
			if (entry.getValue().equals(value))
				return entry.getKey();
		}
		return value.toString();
	}
	
	protected EObject createObject() throws Exception {
		Hashtable<String,Object> choices = getChoiceOfValues(object, feature);
		NameAndURIDialog dialog = new NameAndURIDialog(
				getDiagramEditor().getEditorSite().getShell(), 
				NLS.bind(Messages.NameAndURIObjectEditor_Create_New_Title, ModelUtil.toCanonicalString(preferenceKey)), 
				choices, null, null);
		if ( dialog.open() == Window.OK) {
			Bpmn2Preferences prefs = (Bpmn2Preferences) getDiagramEditor().getAdapter(Bpmn2Preferences.class);
			String newURI = dialog.getURI();
			String newName = dialog.getName();
			prefs.addNameAndURI(preferenceKey, newName, newURI);
			return ModelUtil.createStringWrapper( newURI );
		}
		throw new OperationCanceledException(Messages.NameAndURIObjectEditor_Dialog_Cancelled);
	}
	
	protected EObject editObject(EObject value) throws Exception {
		Hashtable<String,Object> choices = getChoiceOfValues(object, feature);
		final String oldURI = ModelUtil.getStringWrapperTextValue(value);
		final String oldName = (String)getValue();
		NameAndURIDialog dialog = new NameAndURIDialog(
				getDiagramEditor().getEditorSite().getShell(), 
				NLS.bind(Messages.NameAndURIObjectEditor_Edit_Title, ModelUtil.toCanonicalString(preferenceKey)), 
				choices, oldName, oldURI);
		if ( dialog.open() == Window.OK) {
			final String newURI = dialog.getURI();
			final String newName = dialog.getName();
			if (!newURI.equals(oldURI)) {
				final Definitions definitions = ModelUtil.getDefinitions(object);
				if (definitions!=null) {
					TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
					domain.getCommandStack().execute(new RecordingCommand(domain) {
						@Override
						protected void doExecute() {
							String featureName = feature.getName();
							TreeIterator<EObject> iter = definitions.eAllContents();
							while (iter.hasNext()) {
								EObject o = iter.next();
								EStructuralFeature f = o.eClass().getEStructuralFeature(featureName);
								if (f!=null) {
									String uri = (String)o.eGet(f);
									if (oldURI.equals(uri)) {
										o.eSet(f, newURI);
									}
								}
							}
						}
					});
				}
	
				Bpmn2Preferences prefs = (Bpmn2Preferences) getDiagramEditor().getAdapter(Bpmn2Preferences.class);
				prefs.removeNameAndURI(preferenceKey, oldName);
				prefs.addNameAndURI(preferenceKey, newName, newURI);
				return ModelUtil.createStringWrapper(newURI);
			}
		}
		throw new OperationCanceledException(Messages.NameAndURIObjectEditor_Dialog_Cancelled);
	}
	
//	protected Hashtable<String,Object> getChoiceOfValues(EObject object, EStructuralFeature feature) {
//		ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(object);
//		if (adapter)
//		return ServiceTaskPropertiesAdapter.getChoiceOfValues(object);
//	}
	
	public class NameAndURIDialog extends InputDialog {
		
		private String nameString;
		private Text nameText;
		
		public NameAndURIDialog(Shell shell, String title, final Map<String,Object> choices, final String nameString, final String uriString) {
			super(
					shell,
					title,
					Messages.NameAndURIObjectEditor_URI_Label,
					uriString,
					new IInputValidator() {

						@Override
						public String isValid(String newText) {
							if (newText==null || newText.isEmpty())
								return Messages.NameAndURIObjectEditor_Invalid_Empty;
							return null;
						}
					}
				);
			this.nameString = nameString;
		}
		
	    @Override
		protected Control createDialogArea(Composite parent) {
	        Composite composite = (Composite) createMyComposite(parent);
	        // create prompt
            Label label = new Label(composite, SWT.WRAP);
            label.setText(Messages.NameAndURIObjectEditor_Name_Label);
            GridData data = new GridData(GridData.GRAB_HORIZONTAL
                    | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.VERTICAL_ALIGN_CENTER);
            data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            label.setLayoutData(data);
            label.setFont(parent.getFont());

            // create the Name input nameText
	        nameText = new Text(composite, getInputTextStyle());
	        nameText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
	                | GridData.HORIZONTAL_ALIGN_FILL));
	        nameText.addModifyListener(new ModifyListener() {
	            public void modifyText(ModifyEvent e) {
	                validateInput();
	            }
	        });
	        return super.createDialogArea(parent);
	    }
	    
		private Composite createMyComposite(Composite parent) {
			// create a composite with standard margins and spacing
			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
			layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
			composite.setLayout(layout);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			applyDialogFont(composite);
			return composite;
		}
		
		@Override
		protected void validateInput() {
            String errorMessage = getValidator().isValid(nameText.getText());
            if (errorMessage==null)
            	super.validateInput();
            else
            	setErrorMessage(errorMessage);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
	        nameText.setFocus();
	        if (nameString != null) {
	        	nameText.setText(nameString);
	            nameText.selectAll();
	        }
	        validateInput();
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.OK_ID) {
				nameString = nameText.getText();
			} else {
				nameString = null;
			}
			super.buttonPressed(buttonId);
		}
		
		public String getName() {
			return nameString;
		}
		
		public String getURI() {
			return super.getValue();
		}
	}
}
