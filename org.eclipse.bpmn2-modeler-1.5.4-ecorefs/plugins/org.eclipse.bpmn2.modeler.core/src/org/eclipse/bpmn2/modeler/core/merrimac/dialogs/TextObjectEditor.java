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

import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.IConstants;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * @author Bob Brodt
 *
 */
public class TextObjectEditor extends ObjectEditor {

	protected Text text;
	protected boolean multiLine = false;
	protected boolean testMultiLine = true;
	protected Button expandButton;

	/**
	 * @param parent
	 * @param object
	 * @param feature
	 */
	public TextObjectEditor(AbstractDetailComposite parent, EObject object, EStructuralFeature feature) {
		super(parent, object, feature);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.ui.property.editors.ObjectEditor#createControl(org.eclipse.swt.widgets.Composite, java.lang.String)
	 */
	@Override
	protected Control createControl(final Composite composite, final String label, int style) {
		createLabel(composite,label);

		// verify if multiline mode was requested...
		if (testMultiLine && super.isMultiLineText()) {
			multiLine = true;		
		}

		// change style to multiline...
		if (multiLine) {
			style |= SWT.MULTI | SWT.V_SCROLL;
		}
		
		text = getToolkit().createText(composite, "", style | SWT.BORDER); //$NON-NLS-1$

		GridData data;
		if (multiLine) {
			expandButton = getToolkit().createButton(composite, null, SWT.PUSH);
			expandButton.setImage( Activator.getDefault().getImage(IConstants.ICON_EXPAND_20));
			expandButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Dialog dialog = createTextInputDialog();
					showTextInputDialog(dialog);
				}
			});
			data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
			data.horizontalIndent = 0;
			data.verticalIndent = 0;
			expandButton.setLayoutData(data);

			data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			data.heightHint = 100;
		}
		else {
			data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		}
		text.setLayoutData(data);

		int textLimit = multiLine ? Text.LIMIT : Bpmn2Preferences.getInstance(object).getTextLimit();
		text.setTextLimit(textLimit);

		text.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN && multiLine)
					e.doit = false;
			}

		});
		setText(getText());

		text.addModifyListener( new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isWidgetUpdating)
					setValue(text.getText());
			}
		});

		// ask the object if this feature is read-only
		Object result = getExtendedProperty(ExtendedPropertiesAdapter.UI_CAN_EDIT);
		if (result instanceof Boolean)
			setEditable((Boolean)result);

		return text;
	}

	protected Dialog createTextInputDialog() {
		String initialValue = ExtendedPropertiesProvider.getTextValue(object,feature);
		String title = this.getLabel() == null ? null : this.getLabel().getText();
		if (title==null || title.isEmpty()) {
			title = ExtendedPropertiesProvider.getLabel(object)
					+ " " + ExtendedPropertiesProvider.getLabel(object, feature);
		}
		MultiLineStyledTextInputDialog dialog = new MultiLineStyledTextInputDialog(
				this.getControl().getShell(),
				title,
				null,
				initialValue,
				null);
		return dialog;
	}
	
	protected int showTextInputDialog(Dialog dialog) {
		int result = dialog.open();
		if (result==Window.OK){
			if (dialog instanceof StyledTextInputDialog)
			setValue(((StyledTextInputDialog)dialog).getValue());
		}
		return result;
	}
	
	public void setMultiLine(boolean multiLine) {
		testMultiLine = false;
		this.multiLine = multiLine;

	}

	@Override
	public void setObject(EObject object) {
		super.setObject(object);
		updateText();
	}

	@Override
	public void setObject(EObject object, EStructuralFeature feature) {
		super.setObject(object, feature);
		updateText();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor#setValue(java.lang.Object)
	 */
	@Override
	public boolean setValue(Object result) {

		if (multiLine && result instanceof String) {
			result = fromPlatformString((String) result);
		}
		if (super.setValue(result)) {
			updateText();
			return true;
		}
		// revert the change on error
		text.setText(getText());
		return false;
	}

	/**
	 * Update the text field widget after its underlying value has changed.
	 */
	protected void updateText() {
		try {
			isWidgetUpdating = true;
			if (!text.isDisposed()) {
				String oldText = text.getText();
				String newText = getText();
				if (newText != null && !newText.equals(oldText)) {
					int pos = text.getCaretPosition();
					setText(newText);
					text.setSelection(pos, pos);
				}
			}
		}
		finally {
			isWidgetUpdating = false;
		}
	}

	/**
	 * Set the text field with the given value
	 *
	 * @param value - new value for the text field
	 */
	protected void setText(String value) {
		if (value==null)
			value = ""; //$NON-NLS-1$
		if (!value.equals(text.getText()))
				text.setText(value);
	}

	/**
	 * Returns the string representation of the given value used for
	 * display in the text field. The default implementation correctly
	 * handles structureRef values (proxy URIs from a DynamicEObject)
	 * and provides reasonable behavior for EObject values.
	 *
	 * @return string representation of the EObject feature's value.
	 */
	protected String getText() {
		Object value = getBusinessObjectDelegate().getTextValue(object, feature);
		
		if (multiLine && value instanceof String)
			value = toPlatformString((String) value);

		if (value == null)
			return canSetNull() ? null : ""; //$NON-NLS-1$

		return value.toString();
	}

	@Override
	public void notifyChanged(Notification notification) {
		if (notification.getEventType() == -1) {
			super.notifyChanged(notification);
		}
		else if (object == notification.getNotifier()) {
			if (notification.getFeature() instanceof EStructuralFeature) {
				EStructuralFeature f = (EStructuralFeature)notification.getFeature();
				if (f!=null && (f.getName().equals(feature.getName()) ||
						f.getName().equals("mixed")) ) { // handle the case of FormalExpression.body //$NON-NLS-1$
					updateText();
					super.notifyChanged(notification);
				}
			}
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		text.setVisible(visible);
		GridData data = (GridData)text.getLayoutData();
		data.exclude = !visible;
		text.getParent().redraw();
		if (expandButton!=null && !expandButton.isDisposed()) {
			expandButton.setVisible(visible);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (text!=null && !text.isDisposed()) {
			text.dispose();
			text = null;
		}
		if (expandButton!=null && !expandButton.isDisposed()) {
			expandButton.dispose();
			expandButton = null;
		}
	}

	@Override
	public Control getControl() {
		return text;
	}

	@Override
	public Object getValue() {
		return getText();
	}
	
	/**
	 * Convert line delimiters in a string from platform dependent values to a
	 * single "\n" character. This works on all platforms, because SWT Text
	 * widgets universally understand "\n" to be the line delimiter.
	 * 
	 * @param s
	 *            string to convert
	 * @return converted string with platform-dependent line delimiters
	 */
	public static String fromPlatformString(String s) {
		if (s==null)
			return null;
		return s.replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$
	}
	
	/**
	 * Convert "\n" characters in a string back to platform-dependent values:
	 * "\r\n" for windows, "\n" for Linux, "\r" for Mac OS X
	 * 
	 * @param s
	 * @return
	 */
	public static String toPlatformString(String s) {
		if (s==null)
			return null;
		return s.replaceAll("\n", Text.DELIMITER); //$NON-NLS-1$
	}
}
