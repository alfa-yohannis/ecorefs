package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.editor;

import java.util.HashMap;
import java.util.Map;

public class WorkDefinition {
	WorkItemEditor wie;
	Object workDefinition;
    private Map<String, ParameterDefinition> parameters = new HashMap<String, ParameterDefinition>();
    private Map<String, ParameterDefinition> results = new HashMap<String, ParameterDefinition>();

	public WorkDefinition(WorkItemEditor wie) {
		this.wie = wie;
		workDefinition = wie.drools.newObject("WorkDefinitionImpl");
		setName(wie.task.getName());
	}

	public Object getObject() {
		return workDefinition;
	}
	
	public void setName(String name) {
        wie.drools.invoke(workDefinition, "setName", name);
    }
	
	public void addParameter(ParameterDefinition parameterDefinition) {
		wie.drools.invokeWithTypes(workDefinition, "addParameter", wie.drools.loadClass("ParameterDefinition"), parameterDefinition.getObject());
		parameters.put(parameterDefinition.getName(), parameterDefinition);
	}

	public void addResult(ParameterDefinition result) {
		wie.drools.invokeWithTypes(workDefinition, "addResult", wie.drools.loadClass("ParameterDefinition"), result.getObject());
        results.put(result.getName(), result);
    }

	public ParameterDefinition getParameter(String name) {
		return parameters.get(name);
	}

	public ParameterDefinition getResult(String name) {
		return results.get(name);
	}
}
