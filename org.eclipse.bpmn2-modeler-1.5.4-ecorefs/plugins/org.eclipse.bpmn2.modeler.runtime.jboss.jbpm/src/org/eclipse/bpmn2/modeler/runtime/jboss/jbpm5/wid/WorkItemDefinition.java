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
import java.util.LinkedHashMap;
import java.util.List;

/**
 * WorkItemDefinition simply captures the data we need from the *.wid/*.conf file
 * @author bfitzpat
 *
 */
public interface WorkItemDefinition {

	public static class Parameter {
		public String type;
		public Object value;
	};

	public String getName();
	public void setName ( String name );
	public String getDisplayName();
	public String getDescription();
	public String getCategory();
	public String getCustomEditor();
	public String getEclipseCustomEditor();
	public void setCustomEditor ( String editor );
	public void setEclipseCustomEditor ( String editor );
	public void setDispalyName ( String displayName );
	public void setDescription ( String description );
	public void setCategory ( String category );
	public String getIcon();
	public void setIcon ( String iconPath );
	public LinkedHashMap<String, Parameter> getParameters();
	public LinkedHashMap<String, Parameter> getResults();
	public File getDefinitionFile();
	
	public List<String> getImports();
	public void addImport(String fullyQualifiedClassName);
	public String findImport(String className);
}
