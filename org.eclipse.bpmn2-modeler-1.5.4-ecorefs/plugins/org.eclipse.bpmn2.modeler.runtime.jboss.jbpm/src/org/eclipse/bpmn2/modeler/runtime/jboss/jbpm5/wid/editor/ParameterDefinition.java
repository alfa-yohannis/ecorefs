package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.editor;

public class ParameterDefinition {
	WorkItemEditor wie;
	Object parameterDefinition;
	
	public ParameterDefinition(WorkItemEditor wie) {
		this.wie = wie;
		parameterDefinition = wie.drools.newObject("ParameterDefinitionImpl");
	}
	
	public Object getObject() {
		return parameterDefinition;
	}
	
	public String getName() {
		return (String)wie.drools.invoke(parameterDefinition, "getName");
	}
	
	public void setName(String name) {
		wie.drools.invoke(parameterDefinition, "setName", name);
	}
	
	public void setType(String typeName) {
		Object type = wie.drools.newObject(typeName);
		wie.drools.invokeWithTypes(parameterDefinition, "setType", wie.drools.loadClass("DataType"), type);
	}
	
	public Object getType() {
		return wie.drools.invoke(parameterDefinition, "getType");
	}
	
	public Object setStringValue(String value) {
		return wie.drools.invoke(getType(), "readValue", value);
	}
	
	public String getStringValue(Object value) {
		return (String)wie.drools.invokeWithTypes(getType(), "writeValue", Object.class, value);
	}
}
