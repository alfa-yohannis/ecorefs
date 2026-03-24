/*******************************************************************************
 * Copyright (c) 2011 - 2015 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.editor;

import java.util.ArrayList;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.editor.DefaultMarkerBehavior;
import org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior;
import org.eclipse.graphiti.ui.editor.DefaultUpdateBehavior;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.graphiti.ui.editor.IDiagramContainerUI;
import org.eclipse.swt.widgets.Control;

public class DefaultBPMN2EditorDiagramBehavior extends DiagramBehavior {

	public DefaultBPMN2EditorDiagramBehavior(DefaultBPMN2Editor bpmn2Editor) {
		super(bpmn2Editor);
		setParentPart(bpmn2Editor);
		initDefaultBehaviors();
	}

	@Override
	protected DefaultUpdateBehavior createUpdateBehavior() {
		return new BPMN2EditorUpdateBehavior(this);
	}

    @Override
    protected DefaultPersistencyBehavior createPersistencyBehavior() {
    	return new BPMN2PersistencyBehavior(this);
    }

	@Override
	protected DefaultMarkerBehavior createMarkerBehavior() {
		return new BPMN2EditorMarkerBehavior(this);
	}

	@Override
	protected PictogramElement[] getPictogramElementsForSelection() {
		// filter out invisible elements when setting selection
		PictogramElement[] pictogramElements = super.getPictogramElementsForSelection();
		if (pictogramElements==null)
			return null;
		ArrayList<PictogramElement> visibleList = new ArrayList<PictogramElement>();
		for (PictogramElement pe : pictogramElements) {
			if (pe.isVisible())
				visibleList.add(pe);
		}
		return visibleList.toArray(new PictogramElement[visibleList.size()]);
	}

	@Override
	protected void selectPictogramElements(PictogramElement[] pictogramElements) {
		
		// Avoid NPE when a final selection comes in from the Outline Viewer AFTER
		// the editor is closed and the workbench is shutting down.
		IDiagramContainerUI diagramContainer = getDiagramContainer();
		if (diagramContainer == null)
			return;
		
		GraphicalViewer gv = diagramContainer.getGraphicalViewer();
		if (gv == null)
			return;
		
		Control control = gv.getControl();
		if (control==null || control.isDisposed())
			return;
		
		super.selectPictogramElements(pictogramElements);
	}
}
