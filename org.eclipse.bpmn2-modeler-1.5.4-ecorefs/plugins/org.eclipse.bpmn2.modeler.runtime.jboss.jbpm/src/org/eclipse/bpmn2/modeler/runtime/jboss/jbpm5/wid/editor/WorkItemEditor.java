package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.editor;

import java.io.InputStream;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Task;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.utils.FileUtils;
import org.eclipse.bpmn2.modeler.core.utils.JavaProjectClassLoader;
import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.WorkItemDefinition;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class WorkItemEditor {
	public WorkItemDefinition wid;
	public Task task;
	public Dialog dialog;
	public WorkDefinition workDefinition;
	public DroolsProxy drools;
	public Work work;
	boolean done = false;
	
	private WorkItemEditor() {
	}
	
	public static WorkItemEditor create(WorkItemDefinition wid, Task task) {
    	WorkItemEditor workItemEditor = new WorkItemEditor();
    	workItemEditor.wid = wid;
    	workItemEditor.task = task;
		final Shell shell = Display.getDefault().getActiveShell();
    	final String editorClassName = wid.getEclipseCustomEditor();
    	if (!SyntaxCheckerUtils.isJavaPackageName(editorClassName)) {
			MessageDialog.openError(shell, "Custom Editor", "Cannot load the Custom Editor because\n"+
					editorClassName+
					"\nis not a valid Java class name.");
			return null;
    	}
    	
		try {
			JavaProjectClassLoader cl = getProjectClassLoader(task);
			workItemEditor.drools = new DroolsProxy(cl);
			Class editorClass = cl.loadClass(editorClassName);
			if (editorClass==null) {
				boolean createIt = MessageDialog.openQuestion(shell, "Custom Editor", "Cannot find the Custom Editor:\n\n  "+
						editorClassName+
						"\n\nin the class path. Would you like to create a sample editor?");
				
				if (createIt) {
					editorClass = createSampleEditor(cl, editorClassName);
					if (editorClass==null)
						throw new ClassNotFoundException();
				}
				else
					return null;
			}
	    	if (editorClassName!=null && !editorClassName.isEmpty()) {
	    		workItemEditor.dialog = (Dialog)editorClass.getConstructor(Shell.class).newInstance(shell);
	    	}
		}
		catch (IllegalArgumentException iae) {
			MessageDialog.openError(shell, "Custom Editor", "Cannot load the Custom Editor:\n\n  "+
					editorClassName+
					"\n\nbecause the project is not a Java project.");
			return null;
		}
		catch (ClassNotFoundException cnfe) {
			MessageDialog.openError(shell, "Custom Editor", "Cannot create the sample editor:\n\n  "+
					editorClassName+
					"\n\nPlease check the Work Item Definition file:\n"+
					wid.getDefinitionFile().getAbsolutePath());
			return null;
		}
		catch (NoSuchMethodException nsme) {
			MessageDialog.openError(shell, "Custom Editor", "Cannot create the Custom Editor:\n\n  "+
					editorClassName+
					"\n\nbecause the class does not have a constructor.");
			return null;
		}
		catch (ClassCastException cce) {
			MessageDialog.openError(shell, "Custom Editor", "Cannot create the Custom Editor:\n\n  "+
					editorClassName+
					"\n\nbecause the class does not implement the WorkEditor interface.");
			return null;
		}
		catch (Exception ex) {
			MessageDialog.openError(shell, "Custom Editor", "Cannot create the Custom Editor:\n\n  "+
					editorClassName+
					"\n\nbecause of an unknown error:\n"+ex.getMessage());
			ex.printStackTrace();
			return null;
		}
		return workItemEditor;
	}

	private static Class createSampleEditor(JavaProjectClassLoader cl, String editorClassName) {
		int i = editorClassName.lastIndexOf(".");
		if (i<=0)
			return null;
		String packageName = editorClassName.substring(0,i);
		String className = editorClassName.substring(i+1);

		IProgressMonitor monitor = null;
		IJavaProject project = cl.getJavaProject();
		String fileName = "resources/SampleWorkItemEditor.java";

		IFolder folder = project.getProject().getFolder("src/main/java");
		IPackageFragmentRoot packageFragmentRoot = project.getPackageFragmentRoot(folder);
		try {
			IPackageFragment packageFragment = packageFragmentRoot.createPackageFragment(packageName, true, monitor);
			ClassLoader rcl = WorkItemEditor.class.getClassLoader();
			InputStream inputstream = rcl.getResourceAsStream(fileName);
			String content = new String(FileUtils.readStream(inputstream));
			// change the class name
			content = content.replace("SampleWorkItemEditor", className);
			// insert the package declaration before the first import
			content = content.replaceFirst("import ", "package "+packageName+";\n\nimport ");
			packageFragment.createCompilationUnit(className + ".java", content, true, monitor);
			project.getProject().build(IncrementalProjectBuilder.FULL_BUILD,monitor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Class editorClass = cl.loadClass(editorClassName);
		return editorClass;
	}
	
	private static JavaProjectClassLoader getProjectClassLoader(BaseElement baseElement) {
		Resource res = ExtendedPropertiesAdapter.getResource(baseElement);
		URI uri = res.getURI();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(uri.segment(1));
		JavaProjectClassLoader cl = new JavaProjectClassLoader(project);
		return cl;
	}
	
	public boolean show() {
		if (!done) {
			done = true;
			Object result = drools.invoke(dialog, "show");
			if (result instanceof Boolean && (Boolean)result) {
				Object workObject = drools.invoke(dialog, "getWork");
				work = new Work(this, workObject);
				return true;
			}
		}
		return false;
	}
	
    public Work getWork() {
    	if (work==null) {
    		work = new Work(this);
			drools.invokeWithTypes(dialog, "setWork", drools.loadClass("Work"), work.getObject());
    	}
    	return work;
    }

    public WorkDefinition getWorkDefinition() {
    	if (workDefinition==null) {
    		workDefinition = new WorkDefinition(this);
    		drools.invokeWithTypes(dialog, "setWorkDefinition", drools.loadClass("WorkDefinition"), workDefinition.getObject());
    	}
    	return workDefinition;
    }

	public ParameterDefinition getParameter(String name) {
		ParameterDefinition pd = getWorkDefinition().getParameter(name);
		if (pd==null) {
			pd = new ParameterDefinition(this);
			pd.setName(name);
			getWorkDefinition().addParameter(pd);
			getWork().addParameterDefinition(pd);
		}
		return pd;
	}

	public ParameterDefinition getResult(String name) {
		ParameterDefinition pd = getWorkDefinition().getResult(name);
		if (pd==null) {
			pd = new ParameterDefinition(this);
			pd.setName(name);
			getWorkDefinition().addResult(pd);
			getWork().addParameterDefinition(pd);
		}
		return pd;
	}
}