package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.editor;

import java.util.Hashtable;

public class Work {
	WorkItemEditor wie;
	Object work;
	Hashtable<String, ParameterDefinition> parameterDefinitions = new Hashtable<String, ParameterDefinition>();

	public Work(WorkItemEditor wie) {
		this.wie = wie;
		work = wie.drools.newObject("WorkImpl");
		setName(wie.wid.getDisplayName());
	}
	
	public Work(WorkItemEditor wie, Object work) {
		this.wie = wie;
		this.work = work;
	}
	
	public Object getObject() {
		return work;
	}
	
    public void setName(String name) {
		wie.drools.invoke(work, "setName", name);
    }
    
    public void setParameter(String name, Object value) {
    	wie.drools.invokeWithTypes(work, "setParameter", String.class, name, Object.class, value);
    }
    
    public Object getParameter(String name) {
    	return wie.drools.invokeWithTypes(work, "getParameter", String.class, name);
    }
    
    public void addParameterDefinition(ParameterDefinition parameterDefinition) {
		wie.drools.invokeWithTypes(work, "addParameterDefinition", wie.drools.loadClass("ParameterDefinition"), parameterDefinition.getObject());
		parameterDefinitions.put(parameterDefinition.getName(), parameterDefinition);
    }
    
    public ParameterDefinition getParameterDefinition(String name) {
    	return parameterDefinitions.get(name);
    }
}
