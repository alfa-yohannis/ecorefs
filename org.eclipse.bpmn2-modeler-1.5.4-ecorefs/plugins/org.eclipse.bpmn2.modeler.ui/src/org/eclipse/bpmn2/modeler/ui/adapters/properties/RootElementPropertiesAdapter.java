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

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ObjectDescriptor;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.model.RootElementComparator;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * @author Bob Brodt
 *
 */
public class RootElementPropertiesAdapter<T extends RootElement> extends ExtendedPropertiesAdapter<T> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public RootElementPropertiesAdapter(AdapterFactory adapterFactory, T object) {
		super(adapterFactory, object);
		
		// create a Root Element Reference feature descriptor for every feature that is a reference to a RootElement
		// the RootElementRefFeatureDescriptor is used to construct RootElement objects via its createFeature()
		// method, and inserts those objects into the document root.
		EList<EStructuralFeature>list = object.eClass().getEAllStructuralFeatures();
		for (EStructuralFeature ref : list) {
			EClassifier type = ref.getEType();
			if (type instanceof EClass) {
				EList<EClass> supertypes = ((EClass)type).getEAllSuperTypes();
				for (EClass st : supertypes) {
					if (st == Bpmn2Package.eINSTANCE.getRootElement()) {
						setFeatureDescriptor(ref, new RootElementRefFeatureDescriptor<T>(this,object,ref));
					}
				}
			}
		}
		
		setObjectDescriptor(new RootElementObjectDescriptor<T>(this, object));
	}
	
	@SuppressWarnings("hiding")
	public class RootElementObjectDescriptor<T extends RootElement> extends ObjectDescriptor<T> {

		public RootElementObjectDescriptor(ExtendedPropertiesAdapter<T> owner, T object) {
			super(owner, object);
		}
		
		@Override
		public T createObject(Resource resource, EClass eclass, Map<String, Object> args) {
			T rootElement = super.createObject(resource, eclass, args);
			
			Definitions definitions = null;
			if (resource!=null)
				definitions = ModelUtil.getDefinitions(resource);
			else
				definitions = ModelUtil.getDefinitions(rootElement);
			if (definitions!=null) {
				try {
					definitions.getRootElements().add(rootElement);
					if (rootElement instanceof Collaboration) {
						// The new object is a Collaboration: all Participants must
						// be contained in the Collaboration, and all Processes
						// must be referenced by Participants.
			        	Collaboration collaboration = (Collaboration) rootElement;
			        	
			        	// Find the Default Process if there is one.
			        	// This will be a Process that has a BPMNDiagram
			        	List<Process> processes = ModelUtil.getAllRootElements(definitions, Process.class);
			        	for (Process process : processes) {
			        		BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(process);
			        		if (bpmnDiagram!=null) {
				        		// create a Participant for the Default Process so we can add it to the Collaboration
								Participant defaultParticipant = Bpmn2ModelerFactory.create(resource, Participant.class);
								defaultParticipant.setProcessRef(process);
								defaultParticipant.setName(process.getName() +  " Pool"); //$NON-NLS-1$
					        	collaboration.getParticipants().add(defaultParticipant);
								bpmnDiagram.getPlane().setBpmnElement(collaboration);
			        			break;
			        		}
			        	}
					}
					// If we are creating a process than 
					// see issue #516607 
					else if (rootElement instanceof Process  ) {
						// The new object is a Process: if this is a Collaboration diagram
						// we have to lookup for the Collaboration definition.
						// If available, then we set the DefinitionalCollaborationRef for the new Process!
						Process process = (Process) rootElement;
						// lookup for collaboration definition containers...
						List<Collaboration> collaborations = ModelUtil.getAllRootElements(definitions, Collaboration.class);
						for (Collaboration collaboration : collaborations) {
							// take the first one
							process.setDefinitionalCollaborationRef(collaboration);
							break; 
							// skip the next block - we don't want another participant... (see issue #516607)
							/*
							BPMNDiagram bpmnDiagram = DIUtils.findBPMNDiagram(collaboration);
							if (bpmnDiagram!=null) {
								bpmnDiagram.getPlane().setBpmnElement(process);
								Participant defaultParticipant = null;
								// does this Collaboration have a default Participant?
								// i.e. a Participant that does not have a Pool shape.
								for (Participant participant : collaboration.getParticipants()) {
									if (DIUtils.findBPMNShape(participant)==null && participant.getProcessRef()==null) {
										defaultParticipant = participant;
										break;
									}
								}
								if (defaultParticipant==null)
									defaultParticipant = Bpmn2ModelerFactory.create(resource, Participant.class);
								defaultParticipant.setProcessRef(process);
								defaultParticipant.setName(process.getName() +  " Pool"); //$NON-NLS-1$
					        	collaboration.getParticipants().add(defaultParticipant);
			        			break;
							}
							*/
						}
					}
					
					ECollections.sort((EList<RootElement>)definitions.getRootElements(), new RootElementComparator());
				}
				catch (IllegalStateException e) {
					try {
						// well this is odd...
						// even though we did not have an open write transaction,
						// the getRootElements().add() does not get rolled back.
						definitions.getRootElements().remove(rootElement);
					}
					catch (Exception e2) {
						throw e;
					}
				}
			}
			return rootElement;
		}
	}

}
