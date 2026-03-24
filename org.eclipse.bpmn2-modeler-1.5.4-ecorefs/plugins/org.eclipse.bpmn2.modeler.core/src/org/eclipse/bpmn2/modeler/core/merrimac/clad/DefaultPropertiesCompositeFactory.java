package org.eclipse.bpmn2.modeler.core.merrimac.clad;

import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.swt.widgets.Composite;

public class DefaultPropertiesCompositeFactory implements IPropertiesCompositeFactory {

	@Override
	public AbstractDetailComposite createDetailComposite(Class eClass, AbstractBpmn2PropertySection section,
			TargetRuntime targetRuntime) {
		return null;
	}

	@Override
	public AbstractDetailComposite createDetailComposite(Class eClass, Composite parent, TargetRuntime targetRuntime, int style) {
		return null;
	}

	@Override
	public AbstractListComposite createListComposite(Class eClass, AbstractBpmn2PropertySection section,
			TargetRuntime targetRuntime) {
		return null;
	}

	@Override
	public AbstractListComposite createListComposite(Class eClass, Composite parent, TargetRuntime targetRuntime,
			int style) {
		return null;
	}

	@Override
	public AbstractDialogComposite createDialogComposite(EClass eClass, Composite parent, TargetRuntime targetRuntime,
			int style) {
		return null;
	}

}
