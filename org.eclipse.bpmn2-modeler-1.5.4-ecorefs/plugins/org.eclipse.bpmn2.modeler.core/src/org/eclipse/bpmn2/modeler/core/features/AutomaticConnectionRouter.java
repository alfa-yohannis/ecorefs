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

import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;

/**
 * Router for connections that can have user-settable bendpoints. The route is
 * calculated such that it is the most direct line between source and target,
 * but avoids collisions by navigating around shapes.
 */
public class AutomaticConnectionRouter extends BendpointConnectionRouter {

	/** The minimum distance between a bendpoint and a shape when rerouting to avoid collisions. */
	protected static final int margin = 10;
	
	/**
	 * Instantiates a new bendpoint connection router.
	 *
	 * @param fp the Feature Provider
	 */
	public AutomaticConnectionRouter(IFeatureProvider fp) {
		super(fp);
	}
	
	/**
	 * Calculate route.
	 *
	 * @return the connection route
	 */
	protected ConnectionRoute calculateRoute() {
		if (isSelfConnection()) {
			return calculateSelfConnectionRoute();
		}
		
		GraphicsUtil.debug = false;
		
		Point start = null;
		Point end = null;
		Point middle = null;
		if (movedBendpoint!=null) {
			middle = movedBendpoint;
		}
		
		// relocate the source and target anchors for closest proximity to their
		// opposite shapes' centers or the nearest bendpoint
		int length = oldPoints.length;
		Point ref;
		if (length>2)
			ref = oldPoints[1];
		else
			ref = GraphicsUtil.getShapeCenter(target);
		AnchorUtil.moveAnchor(sourceAnchor, ref);
		AnchorUtil.adjustAnchors(source);
		if (length>2)
			ref = oldPoints[length-2];
		else
			ref = GraphicsUtil.getShapeCenter(source);
		AnchorUtil.moveAnchor(targetAnchor, ref);
		AnchorUtil.adjustAnchors(target);

		ConnectionRoute route = new ConnectionRoute(this, 1, source,target);

		start = GraphicsUtil.createPoint(sourceAnchor);
		end = GraphicsUtil.createPoint(targetAnchor);
		route.setSourceAnchor(sourceAnchor);
		route.setTargetAnchor(targetAnchor);
		
		calculateRoute(route, start,middle,end);
		
		return route;
	}
	
	protected ConnectionRoute calculateRoute(ConnectionRoute route, Point start, Point middle, Point end) {
		if (middle!=null) {
			calculateRoute(route, start, middle);
			calculateRoute(route, middle, end);
		}
		else {
			calculateRoute(route, start, end);
		}
		route.add(end);
		
		return route;
	}
	
	protected ConnectionRoute calculateRoute(ConnectionRoute route, Point start, Point end) {
		
		route.add(start);
		
		Point p1 = start;
		Point p2 = end;
		while (true) {
			ContainerShape shape = getCollision(p1,p2);
			if (shape==null || shape==target || shape==source) {
				break;
			}
			// navigate around this shape
			DetourPoints detour = new DetourPoints(shape, margin);
			for (Point d : detour.calculateDetour(p1, p2)) {
				if (!route.add(d))
					return route;
			}
			p1 = route.get(route.size() - 1);
		}
		
		return route;
	}

	protected void optimize(ConnectionRoute route) {
		route.addSpecial(movedBendpoint);
		route.optimize();
	}
}
