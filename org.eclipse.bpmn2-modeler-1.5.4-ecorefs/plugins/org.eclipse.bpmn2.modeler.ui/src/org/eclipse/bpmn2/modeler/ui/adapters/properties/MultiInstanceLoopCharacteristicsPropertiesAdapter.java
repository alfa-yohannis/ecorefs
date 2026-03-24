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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataObject;
import org.eclipse.bpmn2.DataObjectReference;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.InputOutputSpecification;
import org.eclipse.bpmn2.InputSet;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.OutputSet;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.MultiInstanceLoopCharacteristicsPropertiesAdapter.LoopDataCollectionFeatureDescriptor;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * @author Bob Brodt
 *
 */
public class MultiInstanceLoopCharacteristicsPropertiesAdapter extends ExtendedPropertiesAdapter<MultiInstanceLoopCharacteristics> {

	protected final static EStructuralFeature LOOP_DATA_INPUT_REF = Bpmn2Package.eINSTANCE.getMultiInstanceLoopCharacteristics_LoopDataInputRef();
	protected final static EStructuralFeature INPUT_DATA_ITEM = Bpmn2Package.eINSTANCE.getMultiInstanceLoopCharacteristics_InputDataItem();
	protected final static EStructuralFeature LOOP_DATA_OUTPUT_REF = Bpmn2Package.eINSTANCE.getMultiInstanceLoopCharacteristics_LoopDataOutputRef();
	protected final static EStructuralFeature OUTPUT_DATA_ITEM = Bpmn2Package.eINSTANCE.getMultiInstanceLoopCharacteristics_OutputDataItem();
	
	/**
	 * @param adapterFactory
	 * @param object
	 */
	public MultiInstanceLoopCharacteristicsPropertiesAdapter(AdapterFactory adapterFactory, MultiInstanceLoopCharacteristics object) {
		super(adapterFactory, object);

		setProperty(LOOP_DATA_INPUT_REF, UI_IS_MULTI_CHOICE, Boolean.TRUE);
		setProperty(LOOP_DATA_INPUT_REF, UI_CAN_EDIT, Boolean.TRUE);
		setProperty(LOOP_DATA_INPUT_REF, UI_CAN_CREATE_NEW, Boolean.TRUE);

		setProperty(LOOP_DATA_OUTPUT_REF, UI_IS_MULTI_CHOICE, Boolean.TRUE);
		setProperty(LOOP_DATA_OUTPUT_REF, UI_CAN_EDIT, Boolean.TRUE);
		setProperty(LOOP_DATA_OUTPUT_REF, UI_CAN_CREATE_NEW, Boolean.TRUE);

		setFeatureDescriptor(LOOP_DATA_INPUT_REF, new LoopDataInputCollectionFeatureDescriptor(this, object));
		setFeatureDescriptor(INPUT_DATA_ITEM, new LoopInputDataItemFeatureDescriptor(this, object));

		setFeatureDescriptor(LOOP_DATA_OUTPUT_REF, new LoopDataOutputCollectionFeatureDescriptor(this, object));
		setFeatureDescriptor(OUTPUT_DATA_ITEM, new LoopOutputDataItemFeatureDescriptor(this, object));
	}

	/**
	 * This is a base class for LoopDataInputCollectionFeatureDescriptor and LoopDataOutputCollectionFeatureDescriptor.
	 * It calculates the Loop Input/Output collections and Input/Output parameters that are in scope for the 
	 * Activity that owns this Multi-Instance Loop Characteristics object. From the BPMN2 spec:
	 * 
	 * Loop Data Input/Output (the collection):
	 *    For Tasks it is a reference to a Data Input/Output which is part of the Activity’s InputOutputSpecification.
	 *    For Sub-Processes it is a reference to a collection-valued Data Object in the context that is visible to the
	 *    Sub-Processes.
	 *    
	 * Input/Output Parameter (the instance parameter):
	 *   This Data Input/Output can be the source/target of DataInput/OutputAssociation to a data input/output of the
	 *   Activity’s InputOutputSpecification.
	 *   The type of this Data Input/Output MUST be the scalar of the type defined for the loopDataInput/Output.
	 */
	protected class LoopDataCollectionFeatureDescriptor extends FeatureDescriptor<MultiInstanceLoopCharacteristics> {

		public LoopDataCollectionFeatureDescriptor(
				ExtendedPropertiesAdapter<MultiInstanceLoopCharacteristics> owner,
				MultiInstanceLoopCharacteristics object, EStructuralFeature feature) {
			super(owner, object, feature);
			Assert.isTrue(feature==LOOP_DATA_INPUT_REF || feature==LOOP_DATA_OUTPUT_REF);
		}

		@Override
		public EObject createFeature(Resource resource, EClass eclass) {
			EObject value = super.createFeature(resource, eclass);
			// if the new object is the collection reference, we need to attach it to the
			// activity's InputOutputSpecification.
			Activity container = (Activity)ModelUtil.getContainer(object);
			EStructuralFeature f = container.eClass().getEStructuralFeature("ioSpecification"); //$NON-NLS-1$
			if (f!=null) {
				InputOutputSpecification ioSpecification = (InputOutputSpecification)container.eGet(f);
				if (ioSpecification==null) {
					ioSpecification = Bpmn2ModelerFactory.createFeature(object.eResource(), container, f, InputOutputSpecification.class);
				}
				if (value instanceof DataInput)
					ioSpecification.getDataInputs().add((DataInput)value);
				else
					ioSpecification.getDataOutputs().add((DataOutput)value);
			}
			return value;
		}
		
		@Override
		public Hashtable<String, Object> getChoiceOfValues() {
			Hashtable<String, Object> choices = new Hashtable<String, Object>();
			
			Activity container = (Activity)ModelUtil.getContainer(object);
			List values = new ArrayList<EObject>();

			EStructuralFeature f = container.eClass().getEStructuralFeature("ioSpecification"); //$NON-NLS-1$
			if (f!=null) {
				InputOutputSpecification ioSpecification = (InputOutputSpecification)container.eGet(f);
				if (ioSpecification!=null) {
					if (feature == LOOP_DATA_INPUT_REF)
						values.addAll(ioSpecification.getDataInputs());
					else
						values.addAll(ioSpecification.getDataOutputs());
				}
				values.remove(object.getLoopDataInputRef());
				values.remove(object.getLoopDataOutputRef());
			}

//			if (container instanceof SubProcess)
			{
				// Collect all DataObjects from Process and SubProcess ancestors
				// DataObjects are FlowElements, so we will have to weed those out from other FlowElements.
				List<EObject> flowElements = ModelUtil.collectAncestorObjects(object, "flowElements", new Class[] {Process.class, SubProcess.class}); //$NON-NLS-1$
				for (EObject fe : flowElements) {
					if (fe instanceof DataObjectReference) {
						fe = ((DataObjectReference)fe).getDataObjectRef();
					}
					if (!(fe instanceof DataObject)) {
						fe = null;
					}
					if (fe!=null && !values.contains(fe))
						values.add(fe);
				}
				// Collect all Properties from Process and SubProcess ancestors
				EObject parent = container.eContainer();
				while (parent!=null) {
					if (parent instanceof Process) {
						values.addAll(((Process)parent).getProperties());
					}
					if (parent instanceof SubProcess) {
						values.addAll(((SubProcess)parent).getProperties());
					}
					parent = parent.eContainer();
				}
			}
			
			if (values!=null) {
				for (Object p : values) {
					choices.put( getChoiceString(p), p);
				}
			}
			super.setChoiceOfValues(choices);
			return super.getChoiceOfValues();
		}
		
		@Override
		public String getTextValue() {
			Object value = getValue();
			if (value!=null) {
				return super.getChoiceString(value);
			}					
			return "";
		}
	}

	protected class LoopDataInputCollectionFeatureDescriptor extends LoopDataCollectionFeatureDescriptor {
		
		public LoopDataInputCollectionFeatureDescriptor(
				ExtendedPropertiesAdapter<MultiInstanceLoopCharacteristics> owner,
				MultiInstanceLoopCharacteristics object) {
			super(owner, object, LOOP_DATA_INPUT_REF);
		}
		
		@Override
		protected void internalSet(MultiInstanceLoopCharacteristics object, EStructuralFeature feature, Object value, int index) {
			ItemAwareElement element = null;
			if (value instanceof ItemAwareElement)
				element = (ItemAwareElement) value;
			
			if (object.eContainer() instanceof Activity) {
				value = setInputCollection(object, (Activity) object.eContainer(), element);
			}
			super.internalSet(object, feature, value, index);
		}

		@Override
		public Object getValue() {
			ItemAwareElement din = object.getLoopDataInputRef();
			if (din==null)
				return null;
			// this is the LoopDataInputRef in the MI Loop Characteristics
			if (object.eContainer() instanceof SubProcess) {
				// get the name from the DataInput itself
				return super.getValue();
			}
			else if (object.eContainer() instanceof Task) {
				// get the name from the Task's mapped DataInput
				Task task = (Task) object.eContainer();
				for (DataInputAssociation dia : task.getDataInputAssociations()) {
					if (din == dia.getTargetRef() && dia.getSourceRef().size()>0 && dia.getSourceRef().get(0) instanceof ItemAwareElement) {
						return dia.getSourceRef().get(0);
					}
				}
			}				
			return null;
		}
	}
	
	protected class LoopDataOutputCollectionFeatureDescriptor extends LoopDataCollectionFeatureDescriptor {
		
		public LoopDataOutputCollectionFeatureDescriptor(
				ExtendedPropertiesAdapter<MultiInstanceLoopCharacteristics> owner,
				MultiInstanceLoopCharacteristics object) {
			super(owner, object, LOOP_DATA_OUTPUT_REF);
		}
		
		@Override
		protected void internalSet(MultiInstanceLoopCharacteristics object, EStructuralFeature feature, Object value, int index) {
			ItemAwareElement element = null;
			if (value instanceof ItemAwareElement)
				element = (ItemAwareElement) value;
			
			if (object.eContainer() instanceof Activity) {
				value = setOutputCollection(object, (Activity) object.eContainer(), element);
			}
			super.internalSet(object, feature, value, index);
		}

		@Override
		public Object getValue() {
			ItemAwareElement dout = object.getLoopDataOutputRef();
			if (dout==null)
				return null;
			// this is the DataInput in the MI Loop Characteristics
			if (object.eContainer() instanceof SubProcess) {
				// get the name from the DataInput itself
				return super.getValue();
			}
			else if (object.eContainer() instanceof Task) {
				// get the name from the Task's mapped DataInput
				Task task = (Task) object.eContainer();
				for (DataOutputAssociation doa : task.getDataOutputAssociations()) {
					if (doa.getSourceRef().contains(dout) && doa.getTargetRef() instanceof ItemAwareElement) {
						return doa.getTargetRef();
					}
				}
			}				
			return null;
		}
	}

	protected class LoopDataItemFeatureDescriptor extends FeatureDescriptor<MultiInstanceLoopCharacteristics> {

		public LoopDataItemFeatureDescriptor(ExtendedPropertiesAdapter<MultiInstanceLoopCharacteristics> owner,
				MultiInstanceLoopCharacteristics object, EStructuralFeature feature) {
			super(owner, object, feature);
			Assert.isTrue(feature==INPUT_DATA_ITEM || feature==OUTPUT_DATA_ITEM);
		}

		@Override
		public Hashtable<String, Object> getChoiceOfValues() {
			Hashtable<String, Object> choices = new Hashtable<String, Object>();
			EObject container = ModelUtil.getContainer(object);
			// get the Property instances (a.k.a. "local variables") of the containing Process or SubProcess
			if (container instanceof Activity) {
				Activity activity = (Activity) container;
				InputOutputSpecification iospec = activity.getIoSpecification();
				if (iospec!=null) {
					if (feature==INPUT_DATA_ITEM) {
						for (ItemAwareElement e : iospec.getDataInputs()) {
							if (object.getLoopDataInputRef()!=e)
								choices.put(getChoiceString(e), e);
						}
					}
					else {
						for (ItemAwareElement e : iospec.getDataOutputs()) {
							if (object.getLoopDataOutputRef()!=e)
								choices.put(getChoiceString(e), e);
						}
					}
				}
			}
			return choices;
		}
		
		@Override
		public String getTextValue() {
			Object value = getValue();
			if (value!=null) {
				return super.getChoiceString(value);
			}					
			return "";
		}
	}

	protected class LoopInputDataItemFeatureDescriptor extends LoopDataItemFeatureDescriptor {

		public LoopInputDataItemFeatureDescriptor(ExtendedPropertiesAdapter<MultiInstanceLoopCharacteristics> owner,
				MultiInstanceLoopCharacteristics object) {
			super(owner, object, INPUT_DATA_ITEM);
		}

		@Override
		protected void internalSet(MultiInstanceLoopCharacteristics object, EStructuralFeature feature, Object value, int index) {
			ItemAwareElement element = null;
			if (value instanceof ItemAwareElement)
				element = (ItemAwareElement) value;
			
			if (object.eContainer() instanceof SubProcess) {
				value = setSubProcessInputItem(object, (SubProcess) object.eContainer(), element);
			}
			else if (object.eContainer() instanceof Task) {
				value = setTaskInputItem(object, (Task) object.eContainer(), element);
			}				
			super.internalSet(object, feature, value, index);
		}

		@Override
		public Object getValue() {
			DataInput din = object.getInputDataItem();
			if (din==null)
				return null;
			// this is the DataInput in the MI Loop Characteristics
			if (object.eContainer() instanceof SubProcess) {
				// get the name from the DataInput itself
				return din;
			}
			else if (object.eContainer() instanceof Task) {
				// get the name from the Task's mapped DataInput
				Task task = (Task) object.eContainer();
				for (DataInputAssociation dia : task.getDataInputAssociations()) {
					if (dia.getSourceRef().contains(din) && dia.getTargetRef() instanceof DataInput) {
						return dia.getTargetRef();
					}
				}
			}				
			return null;
		}
	}

	protected class LoopOutputDataItemFeatureDescriptor extends LoopDataItemFeatureDescriptor {

		public LoopOutputDataItemFeatureDescriptor(ExtendedPropertiesAdapter<MultiInstanceLoopCharacteristics> owner,
				MultiInstanceLoopCharacteristics object) {
			super(owner, object, OUTPUT_DATA_ITEM);
		}

		@Override
		protected void internalSet(MultiInstanceLoopCharacteristics object, EStructuralFeature feature, Object value, int index) {
			ItemAwareElement element = null;
			if (value instanceof ItemAwareElement)
				element = (ItemAwareElement) value;
			
			if (object.eContainer() instanceof SubProcess) {
				value = setSubProcessOutputItem(object, (SubProcess) object.eContainer(), element);
			}
			else if (object.eContainer() instanceof Task) {
				value = setTaskOutputItem(object, (Task) object.eContainer(), element);
			}				
			super.internalSet(object, feature, value, index);
		}

		@Override
		public Object getValue() {
			DataOutput dout = object.getOutputDataItem();
			if (dout==null)
				return null;
			// this is the DataOutput in the MI Loop Characteristics
			if (object.eContainer() instanceof SubProcess) {
				// get the name from the DataOutput itself
				return dout;
			}
			else if (object.eContainer() instanceof Task) {
				// get the name from the Task's mapped DataOutput
				Task task = (Task) object.eContainer();
				for (DataOutputAssociation dia : task.getDataOutputAssociations()) {
					if (dia.getTargetRef()==dout && dia.getSourceRef().size()>0 && dia.getSourceRef().get(0) instanceof DataOutput) {
						return dia.getSourceRef().get(0);
					}
				}
			}				
			return null;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//
	////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Set or clear the Loop Data Input Reference feature of the given
	 * MultiInstancLoopCharacteristics object.
	 * 
	 * This also manages the Activity's Data Inputs and Input Sets in the
	 * IOSpecificaiont as well as the Activity's DataInputAssociations.
	 * 
	 * @param milc
	 *            the MultiInstancLoopCharacteristics object
	 * @param subprocess
	 *            the affected Activity
	 * @param element
	 *            the new value for the Loop Data Input
	 * @see MultiInstanceLoopCharacteristics#setLoopDataOutputRef(ItemAwareElement)
	 */
	private ItemAwareElement setInputCollection(MultiInstanceLoopCharacteristics milc, Activity subprocess, ItemAwareElement element) {
		InputOutputSpecification ioSpec = subprocess.getIoSpecification();
		Resource resource = getResource();
		if (element!=null) {
			DataInput input = null;
			InputSet inputSet = null;
			DataInputAssociation inputAssociation = null;
			if (ioSpec==null) {
				ioSpec = Bpmn2ModelerFactory.create(resource, InputOutputSpecification.class);
				subprocess.setIoSpecification(ioSpec);
			}
			else {
				for (DataInput din : ioSpec.getDataInputs()) {
					if (din == milc.getLoopDataInputRef()) {
						input = din;
						break;
					}
				}
			}
			if (input == null)
				input = Bpmn2ModelerFactory.create(resource, DataInput.class);
			input.setName(element.getId());
			input.setItemSubjectRef(element.getItemSubjectRef());
			input.setIsCollection(true);
			if (!ioSpec.getDataInputs().contains(input))
				ioSpec.getDataInputs().add(input);
			
			for (InputSet is : ioSpec.getInputSets()) {
				if (is.getDataInputRefs().contains(input)) {
					inputSet = is;
					break;
				}
			}
			if (inputSet == null) {
				if (ioSpec.getInputSets().size()==0) {
					inputSet = Bpmn2ModelerFactory.create(resource, InputSet.class);
					ioSpec.getInputSets().add(inputSet);
				}
				else
					inputSet = ioSpec.getInputSets().get(0);
			}
			if (!inputSet.getDataInputRefs().contains(input))
				inputSet.getDataInputRefs().add(input);

			for (DataInputAssociation dia : subprocess.getDataInputAssociations()) {
				if (dia.getTargetRef()==input) {
					inputAssociation = dia;
					break;
				}
			}
			if (inputAssociation == null) {
				inputAssociation = Bpmn2ModelerFactory.create(resource, DataInputAssociation.class);
				subprocess.getDataInputAssociations().add(inputAssociation);
			}
			
			inputAssociation.setTargetRef(input);
			inputAssociation.getSourceRef().clear();
			inputAssociation.getSourceRef().add(element);
			
			element = input;
		}
		else {
			ItemAwareElement input = milc.getLoopDataInputRef();
			if (ioSpec!=null) {
				if (input!=null) {
					for (DataInput din : ioSpec.getDataInputs()) {
						if (din == input) {
							ioSpec.getDataInputs().remove(din);
							if (ioSpec.getInputSets().size()>0) {
								ioSpec.getInputSets().get(0).getDataInputRefs().remove(din);
								if (ioSpec.getInputSets().get(0).getDataInputRefs().size()==0)
									ioSpec.getInputSets().remove(0);
							}
							break;
						}
					}
					int i = 0;
					for (DataInputAssociation dia : subprocess.getDataInputAssociations()) {
						if (dia.getTargetRef() == input) {
							subprocess.getDataInputAssociations().remove(i);
							break;
						}
						++i;
					}
				}
				if (ioSpec.getDataInputs().size()==0 && ioSpec.getDataOutputs().size()==0) {
					subprocess.setIoSpecification(null);
				}
			}
		}
		
		return element;
	}
	
	/**
	 * Set or clear the Loop Data Output Reference feature of the given
	 * MultiInstancLoopCharacteristics object.
	 * 
	 * This also manages the Activity's Data Outputs and Output Sets in the
	 * IOSpecificaiont as well as the Activity's DataOutputAssociations.
	 * 
	 * @param milc
	 *            the MultiInstancLoopCharacteristics object
	 * @param activity
	 *            the affected Activity
	 * @param element
	 *            the new value for the Loop Data Output
	 * @see MultiInstanceLoopCharacteristics#setLoopDataOutputRef(ItemAwareElement)
	 */
	private ItemAwareElement setOutputCollection(MultiInstanceLoopCharacteristics milc, Activity activity, ItemAwareElement element) {
		InputOutputSpecification ioSpec = activity.getIoSpecification();
		Resource resource = getResource();
		if (element!=null) {
			DataOutput output = null;
			OutputSet outputSet = null;
			DataOutputAssociation outputAssociation = null;
			if (ioSpec==null) {
				ioSpec = Bpmn2ModelerFactory.create(resource, InputOutputSpecification.class);
				activity.setIoSpecification(ioSpec);
			}
			else {
				for (DataOutput dout : ioSpec.getDataOutputs()) {
					if (dout == milc.getLoopDataOutputRef()) {
						output = dout;
						break;
					}
				}
			}
			if (output == null)
				output = Bpmn2ModelerFactory.create(resource, DataOutput.class);
			output.setName(element.getId());
			output.setItemSubjectRef(element.getItemSubjectRef());
			output.setIsCollection(true);
			if (!ioSpec.getDataOutputs().contains(output))
				ioSpec.getDataOutputs().add(output);
			
			for (OutputSet os : ioSpec.getOutputSets()) {
				if (os.getDataOutputRefs().contains(output)) {
					outputSet = os;
					break;
				}
			}
			if (outputSet == null) {
				if (ioSpec.getOutputSets().size()==0) {
					outputSet = Bpmn2ModelerFactory.create(resource, OutputSet.class);
					ioSpec.getOutputSets().add(outputSet);
				}
				else
					outputSet = ioSpec.getOutputSets().get(0);
			}
			if (!outputSet.getDataOutputRefs().contains(output))
				outputSet.getDataOutputRefs().add(output);

			for (DataOutputAssociation doa : activity.getDataOutputAssociations()) {
				if (doa.getSourceRef().size()==1 && doa.getSourceRef().get(0)==output) {
					outputAssociation = doa;
					break;
				}
			}
			if (outputAssociation == null) {
				outputAssociation = Bpmn2ModelerFactory.create(resource, DataOutputAssociation.class);
				activity.getDataOutputAssociations().add(outputAssociation);
			}
			
			outputAssociation.getSourceRef().clear();
			outputAssociation.getSourceRef().add(output);
			outputAssociation.setTargetRef(element);
			
			element = output;
		}
		else {
			ItemAwareElement output = milc.getLoopDataOutputRef();
			if (ioSpec!=null) {
				if (output!=null) {
					for (DataOutput dout : ioSpec.getDataOutputs()) {
						if (dout == output) {
							ioSpec.getDataOutputs().remove(dout);
							if (ioSpec.getOutputSets().size()>0) {
								ioSpec.getOutputSets().get(0).getDataOutputRefs().remove(dout);
								if (ioSpec.getOutputSets().get(0).getDataOutputRefs().size()==0)
									ioSpec.getOutputSets().remove(0);
							}
							break;
						}
					}
					int i = 0;
					for (DataOutputAssociation doa : activity.getDataOutputAssociations()) {
						if (doa.getSourceRef().size()>0 && doa.getSourceRef().get(0) == output) {
							activity.getDataOutputAssociations().remove(i);
							break;
						}
						++i;
					}
				}
				if (ioSpec.getDataInputs().size()==0 && ioSpec.getDataOutputs().size()==0) {
					activity.setIoSpecification(null);
				}
			}

		}
		return element;
	}
	
	private ItemAwareElement setSubProcessInputItem(MultiInstanceLoopCharacteristics milc, SubProcess subprocess, ItemAwareElement element) {
		return element;
	}

	private ItemAwareElement setSubProcessOutputItem(MultiInstanceLoopCharacteristics milc, SubProcess subprocess, ItemAwareElement element) {
		return element;
	}

	private ItemAwareElement setTaskInputItem(MultiInstanceLoopCharacteristics milc, Task task, ItemAwareElement element) {
		Resource resource = task.eResource();
		// find the old DataInputAssociation for the current MI Input Data Item
		// and delete it because it will be replaced by a new one.
		DataInput oldDin = milc.getInputDataItem();
		if (oldDin!=null) {
			for (DataInputAssociation dia : task.getDataInputAssociations()) {
				if (dia.getSourceRef().contains(oldDin)) {
					task.getDataInputAssociations().remove(dia);
					break;
				}
			}
		}		
		
		// find the DataInputAssociation for this DataInput, if it exists
		DataInputAssociation dataInputAssociation = null;
		for (DataInputAssociation dia : task.getDataInputAssociations()) {
			if (dia.getTargetRef() == element) {
				dataInputAssociation = dia;
				break;
			}
		}
		if (dataInputAssociation==null) {
			// not found? create one!
			dataInputAssociation = Bpmn2ModelerFactory.create(resource, DataInputAssociation.class);
			task.getDataInputAssociations().add(dataInputAssociation);
			dataInputAssociation.setTargetRef(element);
		}
		// create a new DataInput for the MI loop input item
		// and map it to the given element
		DataInput din = Bpmn2ModelerFactory.create(resource, DataInput.class);
		din.setName(((DataInput)element).getName());
		din.setItemSubjectRef(element.getItemSubjectRef());
		dataInputAssociation.getSourceRef().clear();
		dataInputAssociation.getSourceRef().add(din);
		return din;
	}

	private ItemAwareElement setTaskOutputItem(MultiInstanceLoopCharacteristics milc, Task task, ItemAwareElement element) {
		Resource resource = task.eResource();
		// find the old DataOutputAssociation for the current MI Output Data Item
		// and delete it because it will be replaced by a new one.
		DataOutput oldDout = milc.getOutputDataItem();
		if (oldDout!=null) {
			for (DataOutputAssociation doa : task.getDataOutputAssociations()) {
				if (doa.getTargetRef() == oldDout) {
					task.getDataOutputAssociations().remove(doa);
					break;
				}
			}
		}		
		
		// find the DataOutputAssociation for this DataOutput, if it exists
		DataOutputAssociation dataOutputAssociation = null;
		for (DataOutputAssociation doa : task.getDataOutputAssociations()) {
			if (doa.getSourceRef().contains(element)) {
				dataOutputAssociation = doa;
				break;
			}
		}
		if (dataOutputAssociation==null) {
			// not found? create one!
			dataOutputAssociation = Bpmn2ModelerFactory.create(resource, DataOutputAssociation.class);
			task.getDataOutputAssociations().add(dataOutputAssociation);
			dataOutputAssociation.getSourceRef().clear();
			dataOutputAssociation.getSourceRef().add(element);
		}
		// create a new DataOutput for the MI loop input item
		// and map it to the given element
		DataOutput dout = Bpmn2ModelerFactory.create(resource, DataOutput.class);
		dout.setName(((DataOutput)element).getName());
		dout.setItemSubjectRef(element.getItemSubjectRef());
		dataOutputAssociation.setTargetRef(dout);
		return dout;
	}
}
