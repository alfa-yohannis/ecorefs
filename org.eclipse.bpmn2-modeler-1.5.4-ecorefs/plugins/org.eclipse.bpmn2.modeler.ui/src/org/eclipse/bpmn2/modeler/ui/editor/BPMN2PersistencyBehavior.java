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
package org.eclipse.bpmn2.modeler.ui.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.validation.BPMN2ProjectValidator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.IThreadListener;
import org.eclipse.swt.widgets.Display;

public class BPMN2PersistencyBehavior extends DefaultPersistencyBehavior {

	DefaultBPMN2Editor editor;

	public BPMN2PersistencyBehavior(DiagramBehavior diagramBehavior) {
		super(diagramBehavior);
		editor = (DefaultBPMN2Editor)diagramBehavior.getDiagramContainer();
	}
	
    @Override
    public Diagram loadDiagram(URI diagramUri) {
    	Diagram diagram = super.loadDiagram(diagramUri);
//    	Resource resource = editor.getResource();
//		List<Tuple<EObject,EObject>> dups = ModelUtil.findDuplicateIds(resource);
//		if (dups.size()>0) {
//			FixDuplicateIdsDialog dlg = new FixDuplicateIdsDialog(dups);
//			dlg.open();
//		}
    	return diagram;
    }
    
    @Override
	public void saveDiagram(IProgressMonitor monitor) {
    	Resource resource = editor.getResource();
    	// this is no longer needed since BaseElementValidator does the job
    	// during Live validation (below)
//		List<Tuple<EObject,EObject>> dups = ModelUtil.findDuplicateIds(resource);
//		if (dups.size()>0) {
//			FixDuplicateIdsDialog dlg = new FixDuplicateIdsDialog(dups);
//			dlg.open();
//		}
		
		// Perform a Live validation first: if there are any ERRORs, the model should be
		// considered to be corrupt (because of such things as invalid IDs, duplicate IDs, etc.)
		// and saving it in its current state MAY render the file unreadable.
		IStatus status = BPMN2ProjectValidator.validateLive(ModelUtil.getDefinitions(resource));
		if (status.getSeverity() >= Status.ERROR) {
			String statusList = ""; //$NON-NLS-1$
			for (IStatus s : collectStatus(status)) {
				statusList += "  " + s.getMessage() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			MessageDialog.openError(editor.getEditorSite().getShell(), Messages.BPMN2PersistencyBehavior_Cannot_Save_Title,
				Messages.BPMN2PersistencyBehavior_Cannot_Save_Message+
				statusList);
			monitor.setCanceled(true);
			return;
		}

    	super.saveDiagram(monitor);
    }
    
    List<IStatus> collectStatus(IStatus status) {
    	if (status.getChildren().length==0)
    		return Collections.singletonList(status);

    	List<IStatus> result = new ArrayList<IStatus>();
    	for (IStatus child : status.getChildren()) {
    		for (IStatus s : collectStatus(child)) {
    			if (!result.contains(s))
    				result.add(s);
    		}
    	}
    	return result;
    }
    
	protected IRunnableWithProgress createOperation(final Set<Resource> savedResources,
			final Map<Resource, Map<?, ?>> saveOptions) {
		// Do the work within an operation because this is a long running
		// activity that modifies the workbench.
		final IRunnableWithProgress operation = new SaveOperation(saveOptions, savedResources);
		return operation;
	}

	/**
	 * The workspace operation used to do the actual save.
	 */
	protected final class SaveOperation implements IRunnableWithProgress, IThreadListener {
		private final Map<Resource, Map<?, ?>> saveOptions;
		private final Set<Resource> savedResources;

		private SaveOperation(Map<Resource, Map<?, ?>> saveOptions, Set<Resource> savedResources) {
			this.saveOptions = saveOptions;
			this.savedResources = savedResources;
		}

		// This is the method that gets invoked when the operation runs.
		public void run(final IProgressMonitor monitor) {
			// Save the resources to the file system.
			try {
				savedResources.addAll(save(diagramBehavior.getEditingDomain(), saveOptions, monitor));
			} catch (final WrappedException e) {
				String emsg = e.getMessage().replaceAll("\tat .*", "").replaceFirst(".*Exception: ","").trim(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				final String msg = emsg.replace("\r\n\r\n", "").replace("\n\n", "");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.BPMN2PersistencyBehavior_Cannot_Save_Title, msg);
						monitor.setCanceled(true);
					}
				});
				throw e;
			}
		}

		/*
		 * Transfer the rule from the calling thread to the callee. This should
		 * be invoked before executing the callee and after the callee has
		 * executed, thus transferring the rule back to the calling thread. See
		 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=391046
		 */
		@Override
		public void threadChange(Thread thread) {
			ISchedulingRule rule = Job.getJobManager().currentRule();
			if (rule != null) {
				Job.getJobManager().transferRule(rule, thread);
			}
		}
	}

}
