/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author bfitzpat
 *
 */
public class WorkItemDefinitionImpl implements WorkItemDefinition {
	
	@Override
	public String toString() {
		return "WorkItemDefinitionImpl [widName=" + widName //$NON-NLS-1$
				+ ", widDisplayName=" + widDisplayName  //$NON-NLS-1$
				+ ", widCategory=" + widCategory //$NON-NLS-1$
				+ ", widIcon=" + widIcon //$NON-NLS-1$
				+ ", widCustomEditor=" + widCustomEditor //$NON-NLS-1$
				+ ", widEclipseCustomEditor=" + widEclipseCustomEditor //$NON-NLS-1$
				+ ", widParameters=" + widParameters //$NON-NLS-1$
				+ ", widResults=" + widResults //$NON-NLS-1$
				+ "]"; //$NON-NLS-1$
	}
	
	private List<String> imports = new ArrayList<String>();
	private String widName;
	private String widDisplayName;
	private String widDescription;
	private String widCategory;
	private String widIcon;
	private String widCustomEditor;
	private String widEclipseCustomEditor;
	private LinkedHashMap<String, Parameter> widParameters;
	private LinkedHashMap<String, Parameter> widResults;
	private File file;

	@Override
	public String getName() {
		return this.widName;
	}

	@Override
	public String getDisplayName() {
		return this.widDisplayName;
	}

	@Override
	public String getDescription() {
		return widDescription;
	}

	@Override
	public String getCategory() {
		return widCategory;
	}

	@Override
	public String getIcon() {
		return this.widIcon;
	}

	@Override
	public LinkedHashMap<String, Parameter> getParameters() {
		if (this.widParameters == null) 
			this.widParameters = new LinkedHashMap<String, Parameter>();
		return this.widParameters;
	}
	
	@Override
	public File getDefinitionFile() {
		return file;
	}
	
	public void setDefinitionFile(File file) {
		this.file = file;
	}

	@Override
	public void setName(String name) {
		this.widName = name;
	}

	@Override
	public void setDispalyName(String displayName) {
		this.widDisplayName = displayName;
	}

	@Override
	public void setDescription(String description) {
		this.widDescription = description;
	}

	@Override
	public void setCategory(String category) {
		this.widCategory = category;
	}

	@Override
	public void setIcon(String iconPath) {
		this.widIcon = iconPath;
	}

	@Override
	public String getCustomEditor() {
		return this.widCustomEditor;
	}

	@Override
	public void setCustomEditor(String editor) {
		if (editor!=null && !editor.isEmpty())
			this.widCustomEditor = editor;
	}

	@Override
	public String getEclipseCustomEditor() {
		return this.widEclipseCustomEditor;
	}

	@Override
	public void setEclipseCustomEditor(String editor) {
		this.widEclipseCustomEditor = editor;
	}

	@Override
	public LinkedHashMap<String, Parameter> getResults() {
		if (this.widResults == null) 
			this.widResults = new LinkedHashMap<String, Parameter>();
		return this.widResults;
	}

	@Override
	public List<String> getImports()
	{
		return imports;
	}
	
	@Override
	public void addImport(String fullyQualifiedClassName)
	{
		imports.add(fullyQualifiedClassName);
	}

	/**
	 * Find the fully-qualified name for the given class name.
	 */
	@Override
	public String findImport(String className)
	{
		for (String i : imports) {
			if ( i.endsWith("."+className))
				return i;
		}
		return null;
	}
}
