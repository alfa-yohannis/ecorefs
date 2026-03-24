package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;

/*
 * Class: Visits each file in the project to see if it's a *.conf/*.wid
 * and loads the file's contents into a String. If the given IProject is
 * a JavaProject, then all jars in the classpath are searched for *.conf/*.wid
 * files and their contents are loaded.
 * 
 * @author bfitzpat
 *
 */
public class WIDLoader {

	private static final int BUFFER_SIZE = 1024;
	private static final String WID_FOLDER = "/src/main/resources/META-INF";
	private static final String ICONS_FOLDER = "/src/main/resources/icons";
	
	private List<WorkItemDefinition> projectWIDs = new ArrayList<WorkItemDefinition>();
	private HashMap<String, ImageDescriptor> projectIcons = new HashMap<String, ImageDescriptor>();
	private List<WorkItemDefinition> classpathWIDs = new ArrayList<WorkItemDefinition>();
	private HashMap<String, ImageDescriptor> classpathIcons = new HashMap<String, ImageDescriptor>();
	
	private class WIDVisitor implements IResourceVisitor {

		@Override
		public boolean visit (IResource resource) throws CoreException {
			try {
				if (resource.getType() == IResource.FILE) {
					// only check the folder src/main/resources/META-INF for WID files
					if ("wid".equalsIgnoreCase(((IFile)resource).getFileExtension())) { //$NON-NLS-1$
						String folder = resource.getParent().getFullPath().toString();
						IProject project = resource.getProject();
						if ((project.getFullPath().toString() + WID_FOLDER).equals(folder)) {
							getProjectFileWIDs((IFile)resource);
						}
						return true;
					}
				}
				else if (resource.getType() == IResource.PROJECT) {
					// Check if this is a Java Project.
					// If so, check all jars in the classpath for WID files.
					IProject project = (IProject)resource;
					IJavaProject javaProject;
					javaProject = getJavaProject(project);
					if (javaProject!=null) {
						for (IClasspathEntry e : javaProject.getRawClasspath()) {
							if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
								IPath path = e.getPath();
								String name = null;
								if (path.getDevice()!=null)
									name = e.getPath().toOSString();
								else
									name = project.getLocation().removeLastSegments(1).append(e.getPath()).toOSString();
								java.io.File jarFile = new java.io.File(name);
								getJarFileWIDs(jarFile);
							}
						}
					}
					return true;
				}
				else if (resource.getType() == IResource.FOLDER) {
					// skip over "bin" and "target" folders
					String name = resource.getName();
					if ("bin".equals(name) || "target".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
						return false;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (WIDException e2) {
				e2.printStackTrace();
			}
			return true;
		}
	}
	
	public void load(IProject project) throws CoreException {
		WIDVisitor visitor = new WIDVisitor();
		project.accept(visitor, IResource.DEPTH_INFINITE, false);
	}
	
	public List<WorkItemDefinition> getProjectWIDs() {
		return projectWIDs;
	}

	public List<WorkItemDefinition> getClasspathWIDs() {
		return classpathWIDs;
	}
	
	public HashMap<String, ImageDescriptor> getProjectIcons() {
		return projectIcons;
	}

	public HashMap<String, ImageDescriptor> getClasspathIcons() {
		return classpathIcons;
	}
	
	private void getProjectFileWIDs(IFile file) throws CoreException, IOException, WIDException {
		InputStream is = file.getContents();
		String content = inputStreamToString(is,null);
		List<WorkItemDefinition> widMap = WIDParser.parse(content);
		for (WorkItemDefinition wid : widMap) {
			String icon = wid.getIcon();
			if (icon!=null && !icon.isEmpty()) {
				getProjectFileIcon(file, icon);
				((WorkItemDefinitionImpl)wid).setDefinitionFile(file.getFullPath().toFile());
			}
		}
		projectWIDs.addAll(widMap);
	}
	
	private void getProjectFileIcon(IFile file, String icon) throws CoreException, IOException {
		IProject project = file.getProject();
		
		// search for icon file in /src/main/resources/icons
		IFile iconFile = project.getFile(new Path(ICONS_FOLDER + "/" + icon));
		
		InputStream is = null;
		try {
			if (iconFile.exists()) {
				is = iconFile.getContents();
				if (is != null) {
					ImageDescriptor imageDescriptor = ImageDescriptor.createFromImageData(new ImageData(is));
					projectIcons.put(icon, imageDescriptor);
					is.close();
				}
			}
		}
		finally {
			if (is!=null)
				is.close();
		}
	}
	
	private void getJarFileWIDs(java.io.File jarFile) throws IOException, WIDException {
		JarFile jar = new java.util.jar.JarFile(jarFile);
	    InputStream is = null;
		try {
//			System.out.println("Jar: "+jar.getName());
//			for (Entry<Object, Object> e : jar.getManifest().getMainAttributes().entrySet()) {
//				System.out.println("  "+e.getKey() + "=" + e.getValue());
//			}
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
			    JarEntry entry = enumEntries.nextElement();
			    if (!entry.isDirectory()) {
				    String name = entry.getName();
				    if (name.endsWith(".wid")) {
				    	is = jar.getInputStream(entry);
						if (is!=null) {
							String content = inputStreamToString(is,null);
							List<WorkItemDefinition> widMap = WIDParser.parse(content);
							for (WorkItemDefinition wid : widMap) {
								String icon = wid.getIcon();
								getJarFileIcon(jar, icon);
								((WorkItemDefinitionImpl)wid).setDefinitionFile(jarFile.getAbsoluteFile());
							}
							classpathWIDs.addAll(widMap);
							is.close();
						}
				    }
			    }
			}
		}
		finally {
			jar.close();
			if (is!=null)
				is.close();
		}
	}

	private void getJarFileIcon(JarFile jar, String icon) throws IOException {
		ImageDescriptor imageDescriptor = null;
		if (icon!=null && !icon.isEmpty() && !classpathIcons.containsKey(icon)) {
		    InputStream is = null;
			try {
				Enumeration<JarEntry> enumEntries = jar.entries();
				while (enumEntries.hasMoreElements()) {
				    JarEntry entry = enumEntries.nextElement();
				    if (!entry.isDirectory()) {
					    String name = entry.getName();
					    if (icon.equals(name)) {
					    	is = jar.getInputStream(entry);
							if (is!=null) {
								imageDescriptor = ImageDescriptor.createFromImageData(new ImageData(is));
								classpathIcons.put(icon, imageDescriptor);
								is.close();
							}
							break;
					    }
				    }
				}
			}
			finally {
				if (is!=null)
					is.close();
			}
		}
	}
	
	private IJavaProject getJavaProject(IProject project) throws IOException {
        if (project != null) {
            try {
                if (project.getNature("org.eclipse.jdt.core.javanature") != null) {
                    IJavaProject javaProject = JavaCore.create(project);
                    if (javaProject.exists()){
                        return javaProject;
                    }
                }
            } catch (CoreException e) {
            	e.printStackTrace();
            }
        }
        return null;
    }

	public static String inputStreamToString(InputStream inputStream, Charset charset) throws IOException {
		if (charset==null)
			charset = StandardCharsets.UTF_8;
	    StringBuilder builder = new StringBuilder();
	    InputStreamReader reader = new InputStreamReader(inputStream, charset);
	    char[] buffer = new char[BUFFER_SIZE];
	    int length;
	    while ((length = reader.read(buffer)) != -1) {
	        builder.append(buffer, 0, length);
	    }
	    return builder.toString();
	}
	
}