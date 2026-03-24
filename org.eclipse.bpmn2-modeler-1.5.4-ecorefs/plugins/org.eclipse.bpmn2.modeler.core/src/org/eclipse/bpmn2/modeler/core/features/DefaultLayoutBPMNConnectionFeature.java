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
package org.eclipse.bpmn2.modeler.core.features;

import java.util.Hashtable;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.AbstractLayoutFeature;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;

/**
 * Default Graphiti {@code LayoutFeature} class for Connections.
 * <p>
 * This simply invokes one of the Connection Routers depending on the user
 * preferences for each type of connection: SequenceFlow, MessageFlow,
 * Association and Conversation. See the Bpmn2EditorPreferencePage for details.
 */
public class DefaultLayoutBPMNConnectionFeature extends AbstractLayoutFeature {

	/** True if changes were made by this feature. */
	boolean hasDoneChanges = false;
	Diagram diagram;
	Hashtable<Connection, IConnectionRouter> routers = new Hashtable<Connection, IConnectionRouter>();

	/**
	 * Instantiates a new default layout bpmn connection feature.
	 *
	 * @param fp the fp
	 */
	public DefaultLayoutBPMNConnectionFeature(IFeatureProvider fp) {
		super(fp);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.func.ILayout#canLayout(org.eclipse.graphiti.features.context.ILayoutContext)
	 */
	@Override
	public boolean canLayout(ILayoutContext context) {
		// Check if automatic routing has been disabled by the user.
		if (context.getPictogramElement() instanceof Connection) {
			Connection connection = (Connection) context.getPictogramElement();
			BaseElement be = BusinessObjectUtil.getFirstBaseElement(connection);
			Bpmn2Preferences prefs = Bpmn2Preferences.getInstance(be);
			return prefs.getEnableConnectionRouting();
		}
		return false;
	}

	@Override
	public void execute(IContext context) {
		PictogramElement pe = ((IPictogramElementContext)context).getPictogramElement();
		TargetRuntime rt = TargetRuntime.getRuntime(pe);
		LifecycleEvent.notify(new LifecycleEvent(EventType.PICTOGRAMELEMENT_LAYOUT, getFeatureProvider(), context, pe, rt));
		super.execute(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.AbstractFeature#hasDoneChanges()
	 */
	@Override
	public boolean hasDoneChanges() {
		return hasDoneChanges;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.func.ILayout#layout(org.eclipse.graphiti.features.context.ILayoutContext)
	 */
	@Override
	public boolean layout(ILayoutContext context) {
		if (canLayout(context)) {
			Connection connection = (Connection) context.getPictogramElement();
			if (context.getProperty(GraphitiConstants.INITIAL_UPDATE) == Boolean.TRUE)
				FeatureSupport.setPropertyValue(connection, GraphitiConstants.INITIAL_UPDATE, Boolean.TRUE.toString());

			diagram = Graphiti.getPeService().getDiagramForPictogramElement(connection);
			IConnectionRouter router = getRouter(connection);
			if (router.canRoute(connection) && router.routingNeeded(connection)) {
				hasDoneChanges |= router.route(connection);
				if (router.routingNeeded(connection))
					AbstractConnectionRouter.addRoutingInfo(connection, "failed"); //$NON-NLS-1$
				else
					AbstractConnectionRouter.removeRoutingInfo(connection);
			}

			UpdateContext uc = new UpdateContext(connection);
			getFeatureProvider().updateIfPossible(uc);

			Graphiti.getPeService().removeProperty(connection, GraphitiConstants.INITIAL_UPDATE);
		}
		return hasDoneChanges;
	}
	
	private IConnectionRouter getRouter(Connection connection) {
		if (routers.containsKey(connection))
			return routers.get(connection);
		
		IFeatureProvider fp = getFeatureProvider();
		IConnectionRouter router = DefaultConnectionRouter.getRouter(fp, connection);
		if (router==null)
			router = new BendpointConnectionRouter(fp);
		
		routers.put(connection, router);
		return router;
	}
}
