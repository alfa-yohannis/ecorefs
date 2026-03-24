/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 * All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *******************************************************************************/
package org.eclipse.bpmn2.modeler.core.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class BPMN2Nature implements IProjectNature {

	/** BPMN2 Project Nature ID **/
	public static final String NATURE_ID = "org.eclipse.bpmn2.modeler.core.bpmn2Nature"; //$NON-NLS-1$
	/** WST Validation Builder ID **/
	public static final String WST_VALIDATION_BUILDER_ID = "org.eclipse.wst.validation.validationbuilder"; //$NON-NLS-1$

	private IProject project;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		if (!project.hasNature(BPMN2Nature.NATURE_ID)) {
			// Add the nature
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = BPMN2Nature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
		configureBuilder(BPMN2Builder.BUILDER_ID);
		// We need the WST validation builder as well
		configureBuilder(WST_VALIDATION_BUILDER_ID);
	}

	public static boolean hasBuilder(IProject project, String builderId) {
		try {
			IProjectDescription desc = project.getDescription();
			ICommand[] commands = desc.getBuildSpec();
	
			for (int i = 0; i < commands.length; ++i) {
				if (commands[i].getBuilderName().equals(builderId)) {
					return true;
				}
			}
		}
		catch (CoreException e) {
		}
		return false;
	}
	
	private void configureBuilder(String builderId) throws CoreException {
		if (!hasBuilder(project, builderId)) {
			IProjectDescription desc = project.getDescription();
			ICommand[] commands = desc.getBuildSpec();
			ICommand[] newCommands = new ICommand[commands.length + 1];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			ICommand command = desc.newCommand();
			command.setBuilderName(builderId);
			newCommands[newCommands.length - 1] = command;
			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
			project.refreshLocal(IProject.DEPTH_INFINITE, null);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		if (project.hasNature(BPMN2Nature.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			for (int i = 0; i < natures.length; ++i) {
				if (BPMN2Nature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i,
							natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					return;
				}
			}
		}
		deconfigureBuilder(BPMN2Builder.BUILDER_ID);
		// Don't remove WST validation builder.
		// Validation is enabled/disabled by the Project or Global
		// User Preferences UI.
	}

	private void deconfigureBuilder(String builderId) throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderId)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i,
						commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);			
				return;
			}
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}
	
	public static void setBPMN2Nature(IProject project, boolean enable) {
		try {
			BPMN2Nature nature = new BPMN2Nature();
			nature.setProject(project);
			if (enable)
				nature.configure();
			else
				nature.deconfigure();
		} catch (CoreException e) {
		}
	}
}
