/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
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

package org.eclipse.bpmn2.modeler.ui.features.event;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.IntermediateCatchEvent;
import org.eclipse.bpmn2.IntermediateThrowEvent;
import org.eclipse.bpmn2.LinkEventDefinition;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CustomFeature;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.graphiti.ui.internal.util.ui.PopupMenu;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 *
 */
public class FollowLinkFeature extends AbstractBpmn2CustomFeature {

	ICustomContext context;
	private static ILabelProvider labelProvider = new ILabelProvider() {

		public void removeListener(ILabelProviderListener listener) {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void dispose() {
		}

		public void addListener(ILabelProviderListener listener) {
		}

		public String getText(Object object) {
			ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(object);
			return adapter.getObjectDescriptor().getTextValue();
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}
	};
	
	/**
	 * @param fp
	 */
	public FollowLinkFeature(IFeatureProvider fp) {
		super(fp);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		PictogramElement pe = context.getPictogramElements()[0];
		Event event = BusinessObjectUtil.getFirstElementOfType(pe, Event.class);
		if (event instanceof IntermediateCatchEvent) {
			for (EventDefinition ed : ((IntermediateCatchEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					if (!((LinkEventDefinition) ed).getSource().isEmpty()) {
						this.context = context;
						return true;
					}
				}
			}
		}
		else if (event instanceof IntermediateThrowEvent) {
			for (EventDefinition ed : ((IntermediateThrowEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					if (((LinkEventDefinition) ed).getTarget()!=null) {
						this.context = context;
						return true;
					}
				}
			}
		}
		this.context = null;
		return false;
	}

	@Override
	public boolean isAvailable(IContext context) {
		if (context instanceof ICustomContext) {
			return canExecute((ICustomContext)context);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.custom.ICustomFeature#execute(org.eclipse.graphiti.features.context.ICustomContext)
	 */
	@Override
	public void execute(ICustomContext context) {
		DiagramEditor editor = getDiagramEditor();
		PictogramElement pe = context.getPictogramElements()[0];
		Event event = BusinessObjectUtil.getFirstElementOfType(pe, Event.class);
		LinkEventDefinition target = null;
		if (event instanceof IntermediateCatchEvent) {
			for (EventDefinition ed : ((IntermediateCatchEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					LinkEventDefinition link = (LinkEventDefinition) ed;
					List<LinkEventDefinition> sources = link.getSource();
					if (!sources.isEmpty()) {
						if (sources.size()==1) {
							target = sources.get(0);
						}
						else {
							PopupMenu popupMenu = new PopupMenu(sources, labelProvider);
							if (popupMenu.show(Display.getCurrent().getActiveShell())) {
								target = (LinkEventDefinition) popupMenu.getResult();
							}
						}
					}
				}
			}
		}
		else if (event instanceof IntermediateThrowEvent) {
			for (EventDefinition ed : ((IntermediateThrowEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					LinkEventDefinition link = (LinkEventDefinition) ed;
					target = link.getTarget();
				}
			}
		}
		if (target!=null) {
			Diagram diagram = DIUtils.getDiagram((Event)target.eContainer());
			List<PictogramElement> pes = Graphiti.getLinkService().getPictogramElements(diagram, target.eContainer());
			IStructuredSelection selection = new StructuredSelection(pes);
			editor.selectionChanged(editor, selection);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.AbstractFeature#hasDoneChanges()
	 */
	@Override
	public boolean hasDoneChanges() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.custom.AbstractCustomFeature#getImageId()
	 */
	@Override
	public String getImageId() {
		return ImageProvider.IMG_16_FOLLOW_LINK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.AbstractFeature#getName()
	 */
	@Override
	public String getName() {
		return Messages.FollowLinkFeature_name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.custom.AbstractCustomFeature#getDescription()
	 */
	@Override
	public String getDescription() {
		if (context!=null) {
			PictogramElement pe = context.getPictogramElements()[0];
			Event event = BusinessObjectUtil.getFirstElementOfType(pe, Event.class);
			if (getLinks(event).size()==0) {
				return Messages.FollowLinkFeature_Create_Link_Description;
			}
			else if (event instanceof IntermediateCatchEvent) {
				return Messages.FollowLinkFeature_Show_Source_Links_Description;
			}
			else {
				return Messages.FollowLinkFeature_Show_Target_Link_Description;
			}
		}
		return null;
	}
	
	private LinkEventDefinition getLinkEventDefinition(Event event) {
		if (event instanceof IntermediateCatchEvent) {
			for (EventDefinition ed : ((IntermediateCatchEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					return (LinkEventDefinition) ed;
				}
			}
		}
		else if (event instanceof IntermediateThrowEvent) {
			for (EventDefinition ed : ((IntermediateThrowEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					return (LinkEventDefinition) ed;
				}
			}
		}
		return null;
	}
	
	private List<LinkEventDefinition> getLinks(Event event) {
		List<LinkEventDefinition> list = new ArrayList<LinkEventDefinition>();
		if (event instanceof IntermediateCatchEvent) {
			for (EventDefinition ed : ((IntermediateCatchEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					LinkEventDefinition link = (LinkEventDefinition) ed;
					if (!link.getSource().isEmpty())
						list.addAll(link.getSource());
				}
			}
		}
		else if (event instanceof IntermediateThrowEvent) {
			for (EventDefinition ed : ((IntermediateThrowEvent) event).getEventDefinitions()) {
				if (ed instanceof LinkEventDefinition) {
					LinkEventDefinition link = (LinkEventDefinition) ed;
					if (link.getTarget()!=null)
						list.add(link.getTarget());
				}
			}
		}
		return list;
	}
}
