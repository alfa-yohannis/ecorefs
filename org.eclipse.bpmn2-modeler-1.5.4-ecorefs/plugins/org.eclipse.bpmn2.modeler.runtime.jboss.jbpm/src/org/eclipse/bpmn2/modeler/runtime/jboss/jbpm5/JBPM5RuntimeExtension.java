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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.Expression;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.Interface;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.ManualTask;
import org.eclipse.bpmn2.Message;
import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.Operation;
import org.eclipse.bpmn2.ReceiveTask;
import org.eclipse.bpmn2.ScriptTask;
import org.eclipse.bpmn2.SendTask;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.PropertiesCompositeFactory;
import org.eclipse.bpmn2.modeler.core.runtime.CustomTaskDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.CustomTaskImageProvider;
import org.eclipse.bpmn2.modeler.core.runtime.ModelExtensionDescriptor.Property;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.features.JbpmCustomTaskFeatureContainer;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.GlobalType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.drools.ImportType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmActivityDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmCommonEventDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmDataAssociationDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmDataItemsPropertySection.GlobalTypeDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmDefinitionsPropertySection.JbpmMessageDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmDefinitionsPropertySection.JbpmMessageListComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmExpressionDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmGatewayDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmImportTypeDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmInterfaceDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmItemDefinitionDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmItemDefinitionListComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmManualTaskDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmMultiInstanceDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmOperationDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmReceiveTaskDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmScriptTaskDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmSendTaskDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmSequenceFlowDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.JbpmTaskDetailComposite;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.WIDLoader;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.WorkItemDefinition;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.WorkItemDefinition.Parameter;
import org.eclipse.bpmn2.modeler.ui.AbstractBpmn2RuntimeExtension.RootElementParser;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.bpmn2.modeler.ui.wizards.FileService;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.xml.sax.InputSource;

public class JBPM5RuntimeExtension implements IBpmn2RuntimeExtension {
	
	public final static String JBPM5_RUNTIME_ID = "org.jboss.runtime.jbpm5"; //$NON-NLS-1$
	public final static String DROOLS_NAMESPACE = "http://www.jboss.org/drools"; //$NON-NLS-1$

	private List<WorkItemDefinition> workItemDefinitions;
	
	/* (non-Javadoc)
	 * Check if the given input file is a drools-generated (jBPM) process file.
	 * 
	 * @see org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension#isContentForRuntime(org.eclipse.core.resources.IFile)
	 */
	@Override
	public boolean isContentForRuntime(IEditorInput input) {
		InputSource source = new InputSource( FileService.getInputContents(input) );
		RootElementParser parser = new RootElementParser(DROOLS_NAMESPACE);
		parser.parse(source);
		return parser.getResult();
	}

	@Override
	public String getTargetNamespace(Bpmn2DiagramType diagramType){
		return DROOLS_NAMESPACE;
	}

	public List<WorkItemDefinition> getWorkItemDefinitions() {
		if (workItemDefinitions==null)
			workItemDefinitions = new ArrayList<WorkItemDefinition>();
		return workItemDefinitions;
	}

	public WorkItemDefinition getWorkItemDefinition(String taskName) {
		if (taskName!=null && !taskName.isEmpty()) {
			List<WorkItemDefinition> wids = getWorkItemDefinitions();
			for (WorkItemDefinition wid : wids) {
				if (taskName.equals(wid.getName())) {
					return wid;
				}
			}
		}
		return null;
	}
	
	@Override
	public void notify(LifecycleEvent event) {
        TargetRuntime targetRuntime = event.targetRuntime;

		if (event.eventType == EventType.EDITOR_STARTUP) {
			// TODO: if file was opened from a Guvnor Repository view (or git in jBPM 6)
			// we may want to explicitly make the editor read-only
	
			IFile inputFile = ((BPMN2Editor) event.target).getModelFile();
			if (inputFile!=null) {
				IProject project = inputFile.getProject();

				// initialize workItemDefinitions list if necessary
				getWorkItemDefinitions().clear();
				try {
					final WIDLoader loader = new WIDLoader();
					loader.load(project);
					
					if (loader.getClasspathWIDs().size() > 0) {
						workItemDefinitions.addAll(loader.getClasspathWIDs());
						for (Entry<String, ImageDescriptor> e : loader.getClasspathIcons().entrySet()) {
							CustomTaskImageProvider.registerImage(e.getKey(), e.getValue());
						}
					}

					if (loader.getProjectWIDs().size() > 0) {
						workItemDefinitions.addAll(loader.getProjectWIDs());
						for (Entry<String, ImageDescriptor> e : loader.getProjectIcons().entrySet()) {
							CustomTaskImageProvider.registerImage(e.getKey(), e.getValue());
						}
					}

					if (!workItemDefinitions.isEmpty()) {
						List<CustomTaskDescriptor> removed = new ArrayList<CustomTaskDescriptor>();
						for (CustomTaskDescriptor d : targetRuntime.getCustomTaskDescriptors()) {
							if (!d.isPermanent())
								removed.add(d);
						}
						targetRuntime.getCustomTaskDescriptors().removeAll(removed);
					
						java.util.Iterator<WorkItemDefinition> widIterator = workItemDefinitions.iterator();
						while(widIterator.hasNext()) {
							final WorkItemDefinition wid = widIterator.next();
							final CustomTaskDescriptor ctd = convertWIDtoCT(inputFile.getProject(),wid);
							if (ctd != null) {
								if (targetRuntime.customTaskExists(ctd.getId())) {
									Display.getDefault().asyncExec( new Runnable() {
										@Override
										public void run() {
											MessageDialog.openError(Display.getDefault().getActiveShell(),
													Messages.JBPM5RuntimeExtension_Duplicate_Task_Title,
													NLS.bind(
														Messages.JBPM5RuntimeExtension_Duplicate_Task_Message,
														ctd.getId(),
														wid.getDefinitionFile())
												);
										}
									});
								}
								else
									targetRuntime.addCustomTask(ctd);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		else if (event.eventType == EventType.EDITOR_INITIALIZED) {
			// Register all of our Property Tab Detail overrides here. 
			PropertiesCompositeFactory.register(Activity.class, JbpmActivityDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(DataInput.class, JbpmDataAssociationDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(DataOutput.class, JbpmDataAssociationDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(Event.class, JbpmCommonEventDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(Gateway.class, JbpmGatewayDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(GlobalType.class, GlobalTypeDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(ImportType.class, JbpmImportTypeDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(ItemDefinition.class, JbpmItemDefinitionListComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(ManualTask.class, JbpmManualTaskDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(Message.class, JbpmMessageDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(Message.class, JbpmMessageListComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(MultiInstanceLoopCharacteristics.class, JbpmMultiInstanceDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(ReceiveTask.class, JbpmReceiveTaskDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(ScriptTask.class, JbpmScriptTaskDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(SendTask.class, JbpmSendTaskDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(SequenceFlow.class, JbpmSequenceFlowDetailComposite.class, targetRuntime);
	        PropertiesCompositeFactory.register(Task.class, JbpmTaskDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(ItemDefinition.class, JbpmItemDefinitionDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Interface.class, JbpmInterfaceDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Operation.class, JbpmOperationDetailComposite.class, targetRuntime);
			PropertiesCompositeFactory.register(Expression.class, JbpmExpressionDetailComposite.class, targetRuntime);
		}
		else if (event.eventType == EventType.BUSINESSOBJECT_CREATED) {
			EObject object = (EObject) event.target;
			// Add a name change adapter to every one of these objects.
			// See my rant in ProcessVariableNameChangeAdapter...
			if (ProcessVariableNameChangeAdapter.appliesTo(object)) {
				ProcessVariableNameChangeAdapter.adapt(object);
			}
			else if (MetaDataTypeAdapter.appliesTo(object)) {
				MetaDataTypeAdapter.adapt(object);
			}
		}
		else if (event.eventType == EventType.BUSINESSOBJECT_LOADED ||
				event.eventType == EventType.BUSINESSOBJECT_INITIALIZED) {
			EObject object = (EObject) event.target;
			if (object instanceof GlobalType) {
				// The BaseElement feature "id" is not saved, but it MUST be kept in sync with the
				// GlobalType feature "identifier" - this acts like the "name" feature of other
				// ItemAwareElements that treat "name" like an ID.
				// @see ProcessVariableNameChangeAdapter for details of how these are kept in sync.
				((GlobalType) object).setId(((GlobalType) object).getIdentifier());
			}
			else if (ProcessVariableNameChangeAdapter.appliesTo(object)) {
				EStructuralFeature nameFeature = object.eClass().getEStructuralFeature("name"); //$NON-NLS-1$
				String n = (String) object.eGet(nameFeature);
				if (n==null || n.isEmpty()) {
					EStructuralFeature idFeature = object.eClass().getEStructuralFeature("id"); //$NON-NLS-1$
					object.eSet(nameFeature, object.eGet(idFeature));
				}
			}
			else if (ElementNameChangeAdapter.appliesTo(object)) {
				ElementNameChangeAdapter.adapt(object);
			}
		}
	}
	
	/*
	 * Convert a WID to a CustomTaskDescriptor
	 * @param wid
	 * @return
	 */
	private CustomTaskDescriptor convertWIDtoCT(IProject project, WorkItemDefinition wid) {
		if (wid != null) {
			String id = wid.getName();
			String name = wid.getDisplayName();
			CustomTaskDescriptor ct = new CustomTaskDescriptor(id,name);
			ct.setType("Task"); //$NON-NLS-1$
			ct.setDescription(wid.getDescription());
			ct.setCategory(wid.getCategory());
			ct.setFeatureContainer(new JbpmCustomTaskFeatureContainer());
			ct.getFeatureContainer().setCustomTaskDescriptor(ct);
			ct.getFeatureContainer().setId(id);
			
			// process basic properties here
			setBasicProps ( ct, wid);
			
			// process xml properties here - i.e. task variables
			Property ioSpecification = createIOSpecificationSection(ct, wid);
			createDataAssociations(ioSpecification, ct);
			
			return ct;
		}
		return null;
	}
	
	/*
	 * Process the high-level props
	 * @param propName
	 * @param wid
	 * @return
	 */
	private String getWIDPropertyValue ( String propName, WorkItemDefinition wid) {
		if (propName.equalsIgnoreCase("taskname")) { //$NON-NLS-1$
			return wid.getName();
		}
		if (propName.equalsIgnoreCase("displayName")) { //$NON-NLS-1$
			return wid.getDisplayName();
		}
		if (propName.equalsIgnoreCase("icon")) { //$NON-NLS-1$
			return wid.getIcon();
		}
		if (propName.equalsIgnoreCase("customEditor")) { //$NON-NLS-1$
			return wid.getCustomEditor();
		}
		if (propName.equalsIgnoreCase("eclipse:customEditor")) { //$NON-NLS-1$
			return wid.getEclipseCustomEditor();
		}
		return null;
	}
	
	/*
	 * Get the high-level prop from the WID
	 * @param propName
	 * @param wid
	 * @return
	 */
	private Property getPropertyFromWID ( String propName, WorkItemDefinition wid ) {
		String name = propName;
		String value = getWIDPropertyValue(propName, wid);
		String description = null;
		String type = "EString"; //$NON-NLS-1$
		Property prop = new Property(null, name, description);
		prop.type = type;
		if (value == null && propName.equalsIgnoreCase("icon")) { //$NON-NLS-1$
			value = "task.png"; //$NON-NLS-1$
		}
		if (value!=null)
			prop.getValues().add(value);
		return prop;
	}
	
	/*
	 * Create the input and output data associations
	 * @param ioSpecification
	 * @param ct
	 */
	private void createDataAssociations ( Property ioSpecification, CustomTaskDescriptor ct) {
		Object[] values = ioSpecification.getValues().toArray();
		int inputCounter = -1;
		int outputCounter = -1;
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Property) {
				Property prop = (Property) values[i];
				if (prop.name.equals("dataInputs")) { //$NON-NLS-1$
					inputCounter++;
					Property dataInputAssociations = new Property (prop, "dataInputAssociations", null); //$NON-NLS-1$
					Property targetRef = new Property (dataInputAssociations, "targetRef", null); //$NON-NLS-1$
					targetRef.ref = "ioSpecification/dataInputs#" + inputCounter; //$NON-NLS-1$
					dataInputAssociations.getValues().add(targetRef);
					ct.getProperties().add(dataInputAssociations);
				}
				else if (prop.name.equals("dataOutputs")) {
					outputCounter++;
					Property dataOutputAssociations = new Property (prop, "dataOutputAssociations", null);
					Property sourceRef = new Property (prop, "sourceRef", null);
					sourceRef.ref = "ioSpecification/dataOutputs#" + outputCounter;
					dataOutputAssociations.getValues().add(sourceRef);
//					Property targetRef = new Property (prop, "targetRef", null);
//					dataOutputAssociations.getValues().add(targetRef);
					ct.getProperties().add(dataOutputAssociations);
				}

			}
		}
	}
	
	/*
	 * Handle creating the ioSpecification from the WID/CT
	 * @param ct
	 * @param wid
	 */
	private Property createIOSpecificationSection ( CustomTaskDescriptor ct, WorkItemDefinition wid ) {
		Property ioSpecification = new Property (null,"ioSpecification", null); //$NON-NLS-1$
		
		for (Entry<String, Parameter> entry : wid.getParameters().entrySet()) {
			Property dataInputs = new Property(ioSpecification,"dataInputs", null); //$NON-NLS-1$
			Property dataInputsName = new Property(dataInputs,"name", null); //$NON-NLS-1$
			dataInputsName.getValues().add(entry.getKey());
			dataInputs.getValues().add(dataInputsName);
			ioSpecification.getValues().add(dataInputs);
		}

		// this code if enabled will create a default output variable
		
//		if (wid.getResults().isEmpty()) {
//			Property dataOutputs = new Property("dataOutputs", null);
//			Property dataOutputsName = new Property("name", null);
//			dataOutputsName.getValues().add("result");
//			dataOutputs.getValues().add(dataOutputsName);
//			ioSpecification.getValues().add(dataOutputs);
//		} else {
			for (Entry<String, Parameter> entry : wid.getResults().entrySet()) {
				Property dataOutputs = new Property(ioSpecification,"dataOutputs", null); //$NON-NLS-1$
				Property dataOutputsName = new Property(dataOutputs,"name", null); //$NON-NLS-1$
				dataOutputsName.getValues().add(entry.getKey());
				dataOutputs.getValues().add(dataOutputsName);
				ioSpecification.getValues().add(dataOutputs);
			}
//		}

		Object[] values = ioSpecification.getValues().toArray();
		int inputCounter = -1;
		int outputCounter = -1;
		Property inputSets = new Property(ioSpecification,"inputSets", null); //$NON-NLS-1$
		Property outputSets = new Property(ioSpecification,"outputSets", null); //$NON-NLS-1$
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Property) {
				Property prop = (Property) values[i];
				if (prop.name.equals("dataInputs")) { //$NON-NLS-1$
					inputCounter++;
					Property inputSetsRef = new Property (inputSets,"dataInputRefs", null); //$NON-NLS-1$
					inputSetsRef.ref = "ioSpecification/dataInputs#" + inputCounter; //$NON-NLS-1$
					inputSets.getValues().add(inputSetsRef);
				} else 	if (prop.name.equals("dataOutputs")) { //$NON-NLS-1$
					outputCounter++;
					Property outputSetsRef = new Property (outputSets,"dataOutputRefs", null); //$NON-NLS-1$
					outputSetsRef.ref = "ioSpecification/dataOutputs#" + outputCounter; //$NON-NLS-1$
					outputSets.getValues().add(outputSetsRef);
				}
			}
		}
		if (inputSets.getValues().size() > 0) 
			ioSpecification.getValues().add(inputSets);
		if (outputSets.getValues().size() > 0) 
			ioSpecification.getValues().add(outputSets);
		
		ct.getProperties().add(ioSpecification);
		return ioSpecification;
	}
	
	/*
	 * Handle the top-level props
	 * @param ct
	 * @param wid
	 */
	private void setBasicProps ( CustomTaskDescriptor ct, WorkItemDefinition wid) {
		String[] basicProps = new String[] { "taskName", "displayName", "icon" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (int i = 0; i < basicProps.length; i++) {
			Property prop = getPropertyFromWID(basicProps[i], wid);
			if (prop!=null)
				ct.getProperties().add(prop);
		}
	}
}
