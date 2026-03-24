package org.eclipse.bpmn2.modeler.core.runtime;

public interface ITargetRuntimeProvider {
	TargetRuntime getTargetRuntime();
	void setTargetRuntime(TargetRuntime rt);
}
