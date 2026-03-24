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

package org.eclipse.bpmn2.modeler.ui.adapters.properties;

import java.util.Hashtable;
import java.util.Map.Entry;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.ResourceAssignmentExpression;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.adapters.InsertionAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ObjectDescriptor;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.ExpressionLanguageDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;

/**
 * @author Bob Brodt
 *
 */
public class FormalExpressionPropertiesAdapter extends ExtendedPropertiesAdapter<FormalExpression> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public FormalExpressionPropertiesAdapter(AdapterFactory adapterFactory, FormalExpression object) {
		super(adapterFactory, object);

    	final EStructuralFeature body = Bpmn2Package.eINSTANCE.getFormalExpression_Body();
    	setFeatureDescriptor(body,
			new FeatureDescriptor<FormalExpression>(this,object,body) {
    		
    			@Override
    	   		protected void internalSet(FormalExpression formalExpression, EStructuralFeature feature, Object value, int index) {
    				String body = value==null ? null : value.toString();
    				InsertionAdapter.executeIfNeeded(formalExpression);
    				Object b = FormalExpressionPropertiesAdapter.this.getProperty(feature, "CDATA"); //$NON-NLS-1$
    				if (b !=null)
    					setBodyCDATA(formalExpression, body);
    				else
    				formalExpression.setBody(body);
    			}
    			
	    		@Override
	    		public String getTextValue() {
					String body = ModelUtil.getExpressionBody(object);
					if (body==null)
						return ""; //$NON-NLS-1$
					return body;
	    		}
	    		
				@Override
				public String getLabel() {
					if (object.eContainer() instanceof SequenceFlow)
						return Messages.FormalExpressionPropertiesAdapter_Condition;
					if (object.eContainer() instanceof ResourceAssignmentExpression)
						return Messages.FormalExpressionPropertiesAdapter_Actor;
					return Messages.FormalExpressionPropertiesAdapter_Script;
				}

				@Override
				public boolean isMultiLine() {
					// formal expression body is always a multiline text field
					return true;
				}
				
				private void setBodyCDATA(FormalExpression formalExpression, String body) {
					formalExpression.getMixed().clear();
					FeatureMap.Entry cdata = FeatureMapUtil.createCDATAEntry(body);
					formalExpression.getMixed().add(cdata);

				}
			}
    	);
    	
    	final EStructuralFeature language = Bpmn2Package.eINSTANCE.getFormalExpression_Language();
		setProperty(language, UI_IS_MULTI_CHOICE, Boolean.TRUE);
		setProperty(language, UI_CAN_SET_NULL, Boolean.TRUE);
    	setFeatureDescriptor(language,
    		new FeatureDescriptor<FormalExpression>(this,object,language) {
    		
				@Override
				public String getLabel() {
					if (object.eContainer() instanceof SequenceFlow)
						return Messages.FormalExpressionPropertiesAdapter_Condition_Language;
					return Messages.FormalExpressionPropertiesAdapter_Script_Language;
				}
	
				@Override
				public Hashtable<String, Object> getChoiceOfValues() {
					return FormalExpressionPropertiesAdapter.getChoiceOfValues(object);
				}
				
			}
    	);
		
		EStructuralFeature feature = Bpmn2Package.eINSTANCE.getFormalExpression_EvaluatesToTypeRef();
		setProperty(feature, UI_IS_MULTI_CHOICE, Boolean.TRUE);
    	setFeatureDescriptor(feature, new ItemDefinitionRefFeatureDescriptor<FormalExpression>(this, object, feature));

		setObjectDescriptor(new ObjectDescriptor<FormalExpression>(this,object) {
			@Override
			public String getTextValue() {
				return getFeatureDescriptor(body).getTextValue();
			}

			@Override
			public String getLabel() {
				if (object.eContainer() instanceof SequenceFlow)
					return Messages.FormalExpressionPropertiesAdapter_Condition;
				if (object.eContainer() instanceof ResourceAssignmentExpression)
					return Messages.FormalExpressionPropertiesAdapter_Actor;
				return Messages.FormalExpressionPropertiesAdapter_Script;
			}
		});
	}
	
	public static Hashtable<String, Object> getChoiceOfValues(EObject object) {
		Hashtable<String,Object> choices = new Hashtable<String, Object>();
		TargetRuntime rt = TargetRuntime.getRuntime(object);
		for (ExpressionLanguageDescriptor el : rt.getExpressionLanguageDescriptors()) {
			choices.put(el.getName(), ModelUtil.createStringWrapper(el.getUri()));
		}
		
		Bpmn2Preferences prefs = Bpmn2Preferences.getInstance(object);
		for (Entry<String, String> entry : prefs.getNameAndURIs(Bpmn2Preferences.PREF_EXPRESSION_LANGUAGE).entrySet()) {
			if (!choices.containsKey(entry.getKey())) {
				choices.put(entry.getKey(), ModelUtil.createStringWrapper(entry.getValue()));
			}
		}
		return choices;
	}
}
