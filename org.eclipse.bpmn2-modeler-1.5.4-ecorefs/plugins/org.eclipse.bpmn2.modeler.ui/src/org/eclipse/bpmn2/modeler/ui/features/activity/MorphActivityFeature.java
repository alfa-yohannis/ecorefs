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

package org.eclipse.bpmn2.modeler.ui.features.activity;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.InputSet;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.OutputSet;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.ResourceRole;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.bpmn2.modeler.ui.features.AbstractMorphNodeFeature;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.graphiti.features.IFeatureProvider;

/**
 * @author Bob Brodt
 *
 */
public class MorphActivityFeature extends AbstractMorphNodeFeature<Activity> {

	/**
	 * @param fp
	 */
	public MorphActivityFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public String getName() {
		return Messages.MorphActivityFeature_Name;
	}

	@Override
	public String getDescription() {
		return Messages.MorphActivityFeature_Description;
	}

	@Override
	public String getImageId() {
		return ImageProvider.IMG_16_MORPH;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.ui.features.AbstractAppendNodeNodeFeature#getBusinessObjectClass()
	 */
	@Override
	public EClass getBusinessObjectClass() {
		return Bpmn2Package.eINSTANCE.getActivity();
	}

	@Override
	public void copyBusinessObject(Activity oldObject, Activity newObject) {
		newObject.setCompletionQuantity( oldObject.getCompletionQuantity() );
		newObject.setDefault( oldObject.getDefault() );
		// I/O Parameters need special attention:
		// if the new object defines its own I/O Parameters, we will
		// need to merge these with the ones from the old object.
		// The exception is that SubProcesses have no DataInputs/Outputs
		// so the old ones (if any) will not be merged into the new SubProcess
		// object.
		if (newObject instanceof SubProcess) {
			newObject.setIoSpecification(null);
			newObject.getDataInputAssociations().clear();
			newObject.getDataOutputAssociations().clear();
		}
		else {
			InputOutputSpecification oldIoSpec = oldObject.getIoSpecification();
			List<DataInput> inputsToRemove = new ArrayList<DataInput>();
			List<DataOutput> outputsToRemove = new ArrayList<DataOutput>();

			if (oldIoSpec!=null) {
				InputOutputSpecification newIoSpec = newObject.getIoSpecification();

				if (newIoSpec!=null) {
					// merge DataInputs
					for (int i=0; i<oldIoSpec.getDataInputs().size(); ++i) {
						DataInput odi = oldIoSpec.getDataInputs().get(i);
						boolean addIt = true;
						for (int j=0; j<newIoSpec.getDataInputs().size(); ++j) {
							DataInput ndi = newIoSpec.getDataInputs().get(j);
							String oldName = new String(odi.getName());
							String newName = new String(ndi.getName());
							if (oldName.equals(newName)) {
								inputsToRemove.add(odi);
								addIt = false;
								break;
							}
						}
						if (addIt)
							newIoSpec.getDataInputs().add(odi);
					}
					oldIoSpec.getDataInputs().removeAll(inputsToRemove);
					
					// merge DataOutputs
					for (int i=0; i<oldIoSpec.getDataOutputs().size(); ++i) {
						DataOutput odo = oldIoSpec.getDataOutputs().get(i);
						boolean addIt = true;
						for (int j=0; j<newIoSpec.getDataOutputs().size(); ++j) {
							DataOutput ndo = newIoSpec.getDataOutputs().get(j);
							String oldName = new String(odo.getName());
							String newName = new String(ndo.getName());
							if (oldName.equals(newName)) {
								outputsToRemove.add(odo);
								addIt = false;
								break;
							}
						}
						if (addIt)
							newIoSpec.getDataOutputs().add(odo);
					}
					oldIoSpec.getDataOutputs().removeAll(outputsToRemove);

					if (oldIoSpec.getInputSets().size()>0) {
						// merge InputSets
						for (int i=0; i<oldIoSpec.getInputSets().size(); ++i) {
							InputSet ois = oldIoSpec.getInputSets().get(i);
							ois.getDataInputRefs().removeAll(inputsToRemove);
							if (ois.getDataInputRefs().size()>0) {
								InputSet nis = null;
								if (newIoSpec.getInputSets().size()>i) {
									nis = newIoSpec.getInputSets().get(i);
									nis.getDataInputRefs().addAll(ois.getDataInputRefs());
								}
								else
									newIoSpec.getInputSets().add(ois);
							}
						}
					}
					if (oldIoSpec.getOutputSets().size()>0) {
						// merge OutputSets
						for (int i=0; i<oldIoSpec.getOutputSets().size(); ++i) {
							OutputSet oos = oldIoSpec.getOutputSets().get(i);
							oos.getDataOutputRefs().removeAll(outputsToRemove);
							if (oos.getDataOutputRefs().size()>0) {
								OutputSet nos = null;
								if (newIoSpec.getOutputSets().size()>i) {
									nos = newIoSpec.getOutputSets().get(i);
									nos.getDataOutputRefs().addAll(oos.getDataOutputRefs());
								}
								else
									newIoSpec.getOutputSets().add(oos);
							}
						}
					}
				}
				else {
					newObject.setIoSpecification(oldIoSpec);
				}
			}
			
			// merge DataInputAssociations
			List<DataInputAssociation> inputAssociations = new ArrayList<DataInputAssociation>();
			inputAssociations.addAll(oldObject.getDataInputAssociations());
			for (DataInputAssociation dia : inputAssociations) {
				if (!inputsToRemove.contains(dia.getTargetRef()))
					newObject.getDataInputAssociations().add(dia);
			}
			// merge DataOutputAssociations
			List<DataOutputAssociation> outputAssociations = new ArrayList<DataOutputAssociation>();
			outputAssociations.addAll(oldObject.getDataOutputAssociations());
			for (DataOutputAssociation doa : outputAssociations) {
				boolean addIt = true;
				for (ItemAwareElement e : doa.getSourceRef()) {
					if (outputsToRemove.contains(e)) {
						addIt = false;
						break;
					}
				}
				if (addIt)
					newObject.getDataOutputAssociations().add(doa);
			}
		}
		newObject.setIsForCompensation( oldObject.isIsForCompensation() );
		String defaultName = ModelUtil.toCanonicalString(oldObject.getId());
		if (!defaultName.equals(oldObject.getName()))
			newObject.setName( oldObject.getName() );
		newObject.setStartQuantity( oldObject.getStartQuantity() );
		newObject.getBoundaryEventRefs().addAll(oldObject.getBoundaryEventRefs());

		// merge Properties
		for (Property p : oldObject.getProperties()) {
			if (!newObject.getProperties().contains(p))
				newObject.getProperties().add(p);
		}
		// merge Resources
		for (ResourceRole rr : oldObject.getResources()) {
			if (!newObject.getResources().contains(rr))
				newObject.getResources().add(rr);
		}
	}
}
