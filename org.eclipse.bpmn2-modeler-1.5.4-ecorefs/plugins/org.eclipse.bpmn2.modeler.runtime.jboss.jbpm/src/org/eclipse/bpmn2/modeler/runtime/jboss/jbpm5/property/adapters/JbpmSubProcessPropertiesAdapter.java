package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.adapters;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.LoopCharacteristics;
import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.StandardLoopCharacteristics;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.ui.adapters.properties.ActivityPropertiesAdapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;

public class JbpmSubProcessPropertiesAdapter extends ActivityPropertiesAdapter<SubProcess> {

	public JbpmSubProcessPropertiesAdapter(AdapterFactory adapterFactory, SubProcess object) {
		super(adapterFactory, object);

		EStructuralFeature feature = Bpmn2Package.eINSTANCE.getActivity_LoopCharacteristics();
		setFeatureDescriptor(feature,
				new FeatureDescriptor<SubProcess>(this,object,feature) {
					@Override
					protected void internalSet(SubProcess object, EStructuralFeature feature, Object value, int index) {
						if (value instanceof String) {
							if ("MultiInstanceLoopCharacteristics".equals(value)) { //$NON-NLS-1$
								MultiInstanceLoopCharacteristics milc = Bpmn2ModelerFactory.create(getResource(), MultiInstanceLoopCharacteristics.class);
								value = milc;
							}
							else if ("StandardLoopCharacteristics".equals(value)) { //$NON-NLS-1$
								StandardLoopCharacteristics milc = Bpmn2ModelerFactory.create(getResource(), StandardLoopCharacteristics.class);
								value = milc;
							}
						}
						else if (value==null) {
							// Here we need to do some cleanup of ioSpecification and DataInput/OutputAssociations
							// if the Activity currently has MultiInstanceLoopCharacteristics set.
							LoopCharacteristics lc = object.getLoopCharacteristics();
							// currently only MultiInstanceLoopCharacteristics are supported but
							// let's check anyway, just in case...
							if (lc instanceof MultiInstanceLoopCharacteristics) {
								ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(lc);
								adapter.getFeatureDescriptor(Bpmn2Package.eINSTANCE.getMultiInstanceLoopCharacteristics_LoopDataInputRef()).setValue(null);
								adapter.getFeatureDescriptor(Bpmn2Package.eINSTANCE.getMultiInstanceLoopCharacteristics_LoopDataOutputRef()).setValue(null);
							}
						}
						super.internalSet(object, feature, value, index);
					}
				}
			);
	}

}
