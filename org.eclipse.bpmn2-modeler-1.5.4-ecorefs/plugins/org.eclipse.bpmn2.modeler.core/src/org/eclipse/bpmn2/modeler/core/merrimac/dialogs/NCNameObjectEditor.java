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

package org.eclipse.bpmn2.modeler.core.merrimac.dialogs;

import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Bob Brodt
 *
 */
public class NCNameObjectEditor extends TextObjectEditor {

	/**
	 * @param parent
	 * @param object
	 * @param feature
	 */
	public NCNameObjectEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object, feature);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.ui.property.editors.ObjectEditor#createControl(org.eclipse.swt.widgets.Composite, java.lang.String)
	 */
	@Override
	protected Control createControl(Composite composite, String label, int style) {
		createLabel(composite,label);

		text = getToolkit().createText(composite, ""); //$NON-NLS-1$
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text.setTextLimit(Bpmn2Preferences.getInstance(object).getTextLimit());
		text.addVerifyListener(new VerifyListener() {

			/**
			 * taken from
			 * http://dev.eclipse.org/viewcvs/viewvc.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets
			 * /Snippet19.java?view=co
			 */
			@Override
			public void verifyText(VerifyEvent e) {
				if (Character.isISOControl(e.character)) {
					if (e.text==null || e.text.isEmpty())
						return;
				}
				String s = getValue() + e.text;
				e.doit = SyntaxCheckerUtils.isNCName(s);
				if (!e.doit) {
					if (SyntaxCheckerUtils.getInvalidChar()=='.') {
						// Allow a dot to appear at end of NC name.
						// if the user does not provide a valid name at the
						// end, it will be caught during batch validation
						e.doit = true;
					}
					else
						showErrorMessage(NLS.bind(Messages.NCNameObjectEditor_Invalid_Character, e.text));
				}
			}
		});

		updateText();

		IObservableValue<String> textObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe((Control) text);
		textObserveTextObserveWidget.addValueChangeListener(new IValueChangeListener<String>() {
			
			@Override
			public void handleValueChange(ValueChangeEvent<? extends String> event) {
				String s = text.getText();
				if (!getValue().equals(s)) {
					setValue(s);
				}
			}
		});


		return text;
	}

	protected String getText() {
		Object value = getBusinessObjectDelegate().getValue(object, feature);
		return value==null ? "" : value.toString(); //$NON-NLS-1$
	}
}
