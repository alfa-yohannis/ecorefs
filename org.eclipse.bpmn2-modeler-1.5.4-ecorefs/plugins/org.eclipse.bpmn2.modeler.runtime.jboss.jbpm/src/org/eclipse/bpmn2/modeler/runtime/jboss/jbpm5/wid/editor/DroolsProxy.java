package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.editor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Hashtable;

import org.eclipse.bpmn2.modeler.core.utils.JavaProjectClassLoader;

public class DroolsProxy {
	private JavaProjectClassLoader classLoader;
	private Hashtable<String, Class> droolsClasses = new Hashtable<String, Class>();

	public DroolsProxy() {
		this.classLoader = null;
	}
	
	public DroolsProxy(JavaProjectClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public Class loadClass(String className) {
		String fqn = className;
		if (!className.contains(".")) {
			// just the class name - no package name
			if ("Work".equals(className)) {
				fqn = "org.drools.core.process.core.Work";
			}
			else if ("WorkEditor".equals(className)) {
				fqn = "org.drools.core.process.core.WorkEditor";
			}
			else if ("WorkDefinition".equals(className)) {
				fqn = "org.drools.core.process.core.WorkDefinition";
			}
			else if ("WorkEditor".equals(className)) {
				fqn = "org.drools.core.process.core.WorkEditor";
			}
			else if ("ParameterDefinition".equals(className)) {
				fqn = "org.drools.core.process.core.ParameterDefinition";
			}
			else if ("ParameterDefinitionImpl".equals(className)) {
				fqn = "org.drools.core.process.core.impl.ParameterDefinitionImpl";
			}
			else if ("WorkDefinitionImpl".equals(className)) {
				fqn = "org.drools.core.process.core.impl.WorkDefinitionImpl";
			}
			else if ("WorkImpl".equals(className)) {
				fqn = "org.drools.core.process.core.impl.WorkImpl";
			}
			else if ("DataType".equals(className)) {
				fqn = "org.drools.core.process.core.datatype.DataType";
			}
		}
		Class droolsClass = droolsClasses.get(fqn);
		if (droolsClass==null) {
			droolsClass = classLoader.loadClass(fqn);
			if (droolsClass!=null)
				droolsClasses.put(fqn, droolsClass);
		}
		return droolsClass;
	}
	
	public Object newObject(String className, Object...args) {
		Class droolsClass = loadClass(className);
		return newObject(droolsClass, args);
	}
	
	public Object newObject(Class droolsClass, Object...args) {
		try {
			Class types[] = new Class[args.length];
			for (int i=0; i<args.length; ++i) {
				types[i] = args[i].getClass();
			}
			Constructor ctor = droolsClass.getConstructor(types);
			return ctor.newInstance(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Object newParameterDefinition(String name, Object value) {
		try {
			Class types[] = new Class[2];
			types[0] = String.class;
			types[1] = loadClass("DataType");
			Class parameterDefinitionClass = loadClass("ParameterDefinitionImpl");
			Constructor ctor = parameterDefinitionClass.getConstructor(types);
			return ctor.newInstance(name, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Object invoke(Object droolsObject, String methodName, Object...args) {
		try {
			Class types[] = new Class[args.length];
			for (int i=0; i<args.length; ++i) {
				types[i] = args[i].getClass();
			}
			Class c = droolsObject.getClass();
			Method m = c.getMethod(methodName, types);
			return m.invoke(droolsObject, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Object invokeWithTypes(Object droolsObject, String methodName, Object...args) {
		try {
			Class types[] = new Class[args.length/2];
			Object values[] = new Object[args.length/2];
			for (int i=0; i<args.length/2; ++i) {
				types[i] = (Class)args[i*2];
			}
			for (int i=0; i<args.length/2; ++i) {
				values[i] = args[i*2+1];
			}
			Class c = droolsObject.getClass();
			Method m = c.getMethod(methodName, types);
			return m.invoke(droolsObject, values);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
