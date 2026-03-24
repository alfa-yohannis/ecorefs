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

import java.util.List;
import java.util.Map;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.ParticipantMultiplicity;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.adapters.ObjectDescriptor;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * @author Bob Brodt
 *
 */
public class ParticipantPropertiesAdapter extends ExtendedPropertiesAdapter<Participant> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public ParticipantPropertiesAdapter(AdapterFactory adapterFactory, Participant object) {
		super(adapterFactory, object);
		

		EStructuralFeature ref = Bpmn2Package.eINSTANCE.getParticipant_ProcessRef();
    	setProperty(ref, UI_CAN_CREATE_NEW, Boolean.FALSE);

    	setFeatureDescriptor(ref, new RootElementRefFeatureDescriptor<Participant>(this,object,ref));

		setObjectDescriptor(new ObjectDescriptor<Participant>(this,object) {
			
			@Override
			public Participant createObject(Resource resource, EClass eclass, Map<String, Object> args) {
				Participant participant = super.createObject(resource, eclass, args);
				
				Definitions definitions = null;
				if (resource!=null)
					definitions = ModelUtil.getDefinitions(resource);
				else {
					definitions = ModelUtil.getDefinitions(participant);
				}

		        // add the Participant to the first Choreography or Collaboration we find.
				if (definitions!=null) {
					BPMNDiagram bpmnDiagram = null;
					List<Collaboration> collaborations = ModelUtil.getAllRootElements(definitions, Collaboration.class);
					for (Collaboration c : collaborations) {
						BPMNDiagram bd = DIUtils.findBPMNDiagram(c);
						if (bd!=null) {
							c.getParticipants().add(participant);
							break;
						}
					}
			        if (participant.eContainer()==null) {
			        	bpmnDiagram = BPMN2Editor.getActiveEditor().getBpmnDiagram();
			        	BaseElement be = null;
			        	if (bpmnDiagram!=null)
			        		be = bpmnDiagram.getPlane().getBpmnElement();
			        	// no Collaboration element found - create one
			        	Collaboration collaboration = Bpmn2ModelerFactory.create(resource, Collaboration.class);
			        	definitions.getRootElements().add(collaboration);
			        	collaboration.getParticipants().add(participant);
			        	if (be instanceof Process) {
			        		// convert the existing Process diagram to a Collaboration
			        		// by creating a new Participant for the default Process.
			        		Process process = (Process) be;
			        		Participant defaultParticipant = Bpmn2ModelerFactory.create(resource, Participant.class);
			        		defaultParticipant.setName(process.getName() +  " Pool");
			        		defaultParticipant.setProcessRef(process);
			        		collaboration.getParticipants().add(defaultParticipant);
			        	}
			        	if (bpmnDiagram!=null)
			        		bpmnDiagram.getPlane().setBpmnElement(collaboration);
			        }
				}
		        
				return participant;
			}
			
		});
		
		ref = Bpmn2Package.eINSTANCE.getParticipant_ParticipantMultiplicity();
		setProperty(ref, UI_CAN_EDIT_INLINE, Boolean.FALSE);
		setProperty(ref, UI_CAN_CREATE_NEW, Boolean.TRUE);
		setProperty(ref, UI_CAN_EDIT, Boolean.TRUE);
		setProperty(ref, UI_IS_MULTI_CHOICE, Boolean.FALSE);
    	setFeatureDescriptor(ref, new FeatureDescriptor<Participant>(this,object,ref) {

			@Override
			public String getLabel() {
				return Messages.ParticipantPropertiesAdapter_Multiplicity;
			}

			@Override
			public String getTextValue() {
				 ParticipantMultiplicity pm = object.getParticipantMultiplicity();
				 if (pm!=null) {
					 return pm.getMinimum() + ".." + pm.getMaximum(); //$NON-NLS-1$
				 }
				 return ""; //$NON-NLS-1$
			}

    	});

	}
}
