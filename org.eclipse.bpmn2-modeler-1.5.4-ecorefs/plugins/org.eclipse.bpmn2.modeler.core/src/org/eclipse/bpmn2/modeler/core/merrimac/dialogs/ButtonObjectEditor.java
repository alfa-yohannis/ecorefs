package org.eclipse.bpmn2.modeler.core.merrimac.dialogs;

import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.IConstants;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ButtonObjectEditor extends ObjectEditor {

	protected Button defaultButton;

	// TODO: add user settable icon and button click handler
	public ButtonObjectEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object, feature);
	}

	@Override
	protected Control createControl(Composite composite, String label, int style) {
		createLabel(composite,label);
		defaultButton = getToolkit().createButton(composite, null, SWT.PUSH);
		defaultButton.setImage( Activator.getDefault().getImage(IConstants.ICON_EDIT_20));
		defaultButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
		
		defaultButton.addSelectionListener(new SelectionAdapter() {

			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent e) {
				buttonClicked();
			}
		});
		
		return defaultButton;
	}

	protected void buttonClicked() {
		
	}
	
	@Override
	public Object getValue() {
		return defaultButton.getData();
	}

}
