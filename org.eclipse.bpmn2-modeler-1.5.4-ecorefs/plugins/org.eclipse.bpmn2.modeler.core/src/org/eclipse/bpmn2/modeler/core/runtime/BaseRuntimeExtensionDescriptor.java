/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.runtime;

import java.util.Collection;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

/**
 * The abstract base class for Target Runtime Extension Descriptor classes.
 * This class provides methods for adding and removing instances of its subclasses to the
 * correct list in the TargetRuntime instance - this is done with java reflection in setRuntime()
 * and dispose() respectively.
 * 
 * All subclasses MUST conform as follows:
 * 
 * - define a static String field named EXTENSION_NAME which must be the same as the
 * - implement the method getExtensionName() which MUST return EXTENSION_NAME (unfortunately
 *   java does not allow class fields to be overridden the same as methods) 
 *   org.eclipse.bpmn2.modeler.runtime extension point element that it supports.
 * - define a public constructor that accepts and IConfigurationElement (this comes from
 *   the extension plugin's configuration, i.e. plugin.xml)
 * - optionally override setRuntime() to perform additional class initialization
 * - optionally override dispose() to perform additional cleanup
 * 
 * Extension Descriptor classes 
 */
public abstract class BaseRuntimeExtensionDescriptor implements IRuntimeExtensionDescriptor {

	protected TargetRuntime targetRuntime;
	protected IFile configFile;
	protected long configFileTimestamp;
	protected final IConfigurationElement configurationElement;
	protected String id;

	public static <T extends BaseRuntimeExtensionDescriptor> T getDescriptor(EObject object, Class type) {
		ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(object);
		if (adapter!=null) {
			return (T)adapter.getProperty(type.getName());
		}
		return null;
	}
	
	public BaseRuntimeExtensionDescriptor() {
		configurationElement = null;
	}
	
	public BaseRuntimeExtensionDescriptor(IConfigurationElement e) {
		configurationElement = e;
		id = e.getAttribute("id"); //$NON-NLS-1$
	}

	public String getId() {
		return id;
	}

	public void dispose() {
		Collection<IRuntimeExtensionDescriptor> list = targetRuntime.getRuntimeExtensionDescriptors(getExtensionName());
		list.remove(this);
	}

	public BaseRuntimeExtensionDescriptor(TargetRuntime rt) {
		targetRuntime = rt;
		configurationElement = rt.configurationElement;
	}
	
	public IFile getConfigFile() {
		return configFile;
	}

	public void setConfigFile(IFile configFile) {
		this.configFile = configFile;
		if (configFile!=null)
			configFileTimestamp = configFile.getLocalTimeStamp();
		else
			configFileTimestamp = 0;
	}
	
	public long getConfigFileTimestamp() {
		return configFileTimestamp;
	}
	
	public TargetRuntime getRuntime() {
		return targetRuntime;
	}

	public void setRuntime(TargetRuntime targetRuntime) {
		this.targetRuntime = targetRuntime;
		Collection<IRuntimeExtensionDescriptor> list = targetRuntime.getRuntimeExtensionDescriptors(getExtensionName());
		list.add(this);
	}
	
	public EPackage getEPackage() {
		if (targetRuntime.getModelDescriptor()!=null)
			return targetRuntime.getModelDescriptor().getEPackage();
		return Bpmn2Package.eINSTANCE;
	}
}