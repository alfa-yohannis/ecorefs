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

import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.utils.AnchorSite;
import org.eclipse.bpmn2.modeler.core.utils.AnchorType;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BoundaryEventPositionHelper;
import org.eclipse.bpmn2.modeler.core.utils.BoundaryEventPositionHelper.PositionOnLine;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

/**
 * Router for connections that can have user-settable bendpoints.
 */
public class BendpointConnectionRouter extends DefaultConnectionRouter {

	/** The connection, must be a {@code FreeFormConnection}. */
	protected FreeFormConnection ffc;
	/** The moved or added bendpoint (if any). */
	protected Point movedBendpoint;
	/** The removed bendpoint. */
	protected Point removedBendpoint;
	/** The list of old connection cuts (including the end cuts) for determining if a route has changed */
	protected Point oldPoints[];

	/** The list of allowable source shape edges which can serve as sites (top, left, bottom or right) */
	AnchorSite sourceAnchorSites[];

	/** The list of allowable target shape edges which can serve as sites */
	AnchorSite targetAnchorSites[];
	
	/**
	 * Instantiates a new bendpoint connection router.
	 *
	 * @param fp the Feature Provider
	 */
	public BendpointConnectionRouter(IFeatureProvider fp) {
		super(fp);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.IConnectionRouter#canRoute(org.eclipse.graphiti.mm.pictograms.Connection)
	 */
	@Override
	public boolean canRoute(Connection connection) {
		return super.canRoute(connection) && connection instanceof FreeFormConnection;
	}

	@Override
	public boolean routingNeeded(Connection connection) {
		if (Graphiti.getPeService().getProperty(connection, RoutingNet.CONNECTION)!=null) {
			return false;
		}
		return super.routingNeeded(connection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.DefaultConnectionRouter#initialize(org.eclipse.graphiti.mm.pictograms.Connection)
	 */
	@Override
	protected void initialize(Connection connection) {
		// check if initialization needed?
		if (oldPoints==null) {
			super.initialize(connection);
			ffc = (FreeFormConnection)connection;
	
			movedBendpoint = getMovedBendpoint(ffc);
			if (movedBendpoint==null)
				movedBendpoint = getAddedBendpoint(ffc);
			removedBendpoint = getRemovedBendpoint(ffc);
	
			findAllShapes();
			if (movedBendpoint!=null) {
				for (ContainerShape shape : allShapes) {
					if (GraphicsUtil.contains(shape, movedBendpoint)) {
						movedBendpoint = null;
						break;
					}
				}
			}
	
			/**
			 * Save the connection's start/end anchors, and their locations as well as
			 * the bendpoints. This is used to compare against the new ConnectionRoute
			 */
			oldPoints = new Point[ffc.getBendpoints().size() + 2];
			int i = 0;
			oldPoints[i++] = GraphicsUtil.createPoint(ffc.getStart());
			for (Point p : ffc.getBendpoints()) {
				oldPoints[i++] = GraphicsUtil.createPoint(p);
			}
			oldPoints[i++] = GraphicsUtil.createPoint(ffc.getEnd());
			calculateAllowedAnchorSites();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.DefaultConnectionRouter#route(org.eclipse.graphiti.mm.pictograms.Connection)
	 */
	@Override
	public boolean route(Connection connection) {
		initialize(connection);
		
		boolean changed = false;
		if (connection instanceof FreeFormConnection) {
			ConnectionRoute route = calculateRoute();
			if (route!=null) {
				changed = isRouteChanged(route);
				applyRoute(route);
			}
			dispose();
		}
		
		return changed;
	}

	/**
	 * 
	 */
	protected void calculateAllowedAnchorSites() {
		
		EObject bo = BusinessObjectUtil.getBusinessObjectForPictogramElement(source);
		if (bo instanceof BoundaryEvent) {
			sourceAnchorSites = calculateBoundaryEventAnchorSites(source);
		}
		bo = BusinessObjectUtil.getBusinessObjectForPictogramElement(target);
		if (bo instanceof BoundaryEvent) {
			targetAnchorSites = calculateBoundaryEventAnchorSites(target);
		}
		if (AnchorType.getType(source) == AnchorType.CONNECTION) {
			sourceAnchorSites = new AnchorSite[1];
			sourceAnchorSites[0] = AnchorSite.CENTER;
		}
		if (AnchorType.getType(target) == AnchorType.CONNECTION) {
			targetAnchorSites = new AnchorSite[1];
			targetAnchorSites[0] = AnchorSite.CENTER;
		}
		
		ILocation sPos = Graphiti.getPeService().getLocationRelativeToDiagram(source);
		IDimension sSize = GraphicsUtil.calculateSize(source);
		ILocation tPos = Graphiti.getPeService().getLocationRelativeToDiagram(target);
		IDimension tSize = GraphicsUtil.calculateSize(target);
		
		if (movedBendpoint!=null) {
			Point sc = GraphicsUtil.getShapeCenter(source);
			Point tc = GraphicsUtil.getShapeCenter(target);
			double ds = GraphicsUtil.getLength(movedBendpoint, sc);
			double dt = GraphicsUtil.getLength(movedBendpoint, tc);
			boolean expandSourceSites = ds > 2*dt;
			boolean expandTargetSites = dt > 2*ds;
			if (sourceAnchorSites==null) {
				if (movedBendpoint.getX() < sPos.getX()) {
					// bendpoint is left of shape
					if (movedBendpoint.getY() < sPos.getY()) {
						// bendpoint is above shape
						sourceAnchorSites = new AnchorSite[2];
						sourceAnchorSites[0] = AnchorSite.LEFT;
						sourceAnchorSites[1] = AnchorSite.TOP;
					}
					else if (sPos.getY()+sSize.getHeight() < movedBendpoint.getY()) {
						// bendpoint is below shape
						sourceAnchorSites = new AnchorSite[2];
						sourceAnchorSites[0] = AnchorSite.LEFT;
						sourceAnchorSites[1] = AnchorSite.BOTTOM;
					}
					else {
						// bendpoint is directly left of shape
						if (expandSourceSites) {
							sourceAnchorSites = new AnchorSite[3];
							sourceAnchorSites[0] = AnchorSite.LEFT;
							sourceAnchorSites[1] = AnchorSite.TOP;
							sourceAnchorSites[2] = AnchorSite.BOTTOM;
						}
						else {
							sourceAnchorSites = new AnchorSite[1];
							sourceAnchorSites[0] = AnchorSite.LEFT;
						}
					}
				}
				else if (sPos.getX()+sSize.getWidth() < movedBendpoint.getX()) {
					// bendpoint is right of shape
					if (movedBendpoint.getY() < sPos.getY()) {
						// bendpoint is above shape
						sourceAnchorSites = new AnchorSite[2];
						sourceAnchorSites[0] = AnchorSite.RIGHT;
						sourceAnchorSites[1] = AnchorSite.TOP;
					}
					else if (sPos.getY()+sSize.getHeight() < movedBendpoint.getY()) {
						// bendpoint is below shape
						sourceAnchorSites = new AnchorSite[2];
						sourceAnchorSites[0] = AnchorSite.RIGHT;
						sourceAnchorSites[1] = AnchorSite.BOTTOM;
					}
					else {
						// bendpoint is directly right of shape
						if (expandSourceSites) {
							sourceAnchorSites = new AnchorSite[3];
							sourceAnchorSites[0] = AnchorSite.RIGHT;
							sourceAnchorSites[1] = AnchorSite.TOP;
							sourceAnchorSites[2] = AnchorSite.BOTTOM;
						}
						else {
							sourceAnchorSites = new AnchorSite[1];
							sourceAnchorSites[0] = AnchorSite.RIGHT;
						}
					}
				}
				else {
					// bendpoint is directly above or below shape
					if (movedBendpoint.getY() < sPos.getY()) {
						// bendpoint is above shape
						if (expandSourceSites) {
							sourceAnchorSites = new AnchorSite[3];
							sourceAnchorSites[0] = AnchorSite.TOP;
							sourceAnchorSites[1] = AnchorSite.LEFT;
							sourceAnchorSites[2] = AnchorSite.RIGHT;
						}
						else {
							sourceAnchorSites = new AnchorSite[1];
							sourceAnchorSites[0] = AnchorSite.TOP;
						}
					}
					else if (sPos.getY()+sSize.getHeight() < movedBendpoint.getY()) {
						// bendpoint is below shape
						if (expandSourceSites) {
							sourceAnchorSites = new AnchorSite[3];
							sourceAnchorSites[0] = AnchorSite.BOTTOM;
							sourceAnchorSites[1] = AnchorSite.LEFT;
							sourceAnchorSites[2] = AnchorSite.RIGHT;
						}
						else {
							sourceAnchorSites = new AnchorSite[1];
							sourceAnchorSites[0] = AnchorSite.BOTTOM;
						}
					}
					else {
						// bendpoint is inside shape
						sourceAnchorSites = new AnchorSite[0];
					}
				}
			}
			if (targetAnchorSites==null) {
				if (movedBendpoint.getX() < tPos.getX()) {
					// bendpoint is left of shape
					if (movedBendpoint.getY() < tPos.getY()) {
						// bendpoint is above shape
						targetAnchorSites = new AnchorSite[2];
						targetAnchorSites[0] = AnchorSite.LEFT;
						targetAnchorSites[1] = AnchorSite.TOP;
					}
					else if (tPos.getY()+tSize.getHeight() < movedBendpoint.getY()) {
						// bendpoint is below shape
						targetAnchorSites = new AnchorSite[2];
						targetAnchorSites[0] = AnchorSite.LEFT;
						targetAnchorSites[1] = AnchorSite.BOTTOM;
					}
					else {
						// bendpoint is directly left of shape
						if (expandTargetSites) {
							targetAnchorSites = new AnchorSite[3];
							targetAnchorSites[0] = AnchorSite.LEFT;
							targetAnchorSites[1] = AnchorSite.TOP;
							targetAnchorSites[2] = AnchorSite.BOTTOM;
						}
						else {
							targetAnchorSites = new AnchorSite[1];
							targetAnchorSites[0] = AnchorSite.LEFT;
						}
					}
				}
				else if (tPos.getX()+tSize.getWidth() < movedBendpoint.getX()) {
					// bendpoint is right of shape
					if (movedBendpoint.getY() < tPos.getY()) {
						// bendpoint is above shape
						targetAnchorSites = new AnchorSite[2];
						targetAnchorSites[0] = AnchorSite.RIGHT;
						targetAnchorSites[1] = AnchorSite.TOP;
					}
					else if (tPos.getY()+tSize.getHeight() < movedBendpoint.getY()) {
						// bendpoint is below shape
						targetAnchorSites = new AnchorSite[2];
						targetAnchorSites[0] = AnchorSite.RIGHT;
						targetAnchorSites[1] = AnchorSite.BOTTOM;
					}
					else {
						// bendpoint is directly right of shape
						if (expandTargetSites) {
							targetAnchorSites = new AnchorSite[3];
							targetAnchorSites[0] = AnchorSite.RIGHT;
							targetAnchorSites[1] = AnchorSite.TOP;
							targetAnchorSites[2] = AnchorSite.BOTTOM;
						}
						else {
							targetAnchorSites = new AnchorSite[1];
							targetAnchorSites[0] = AnchorSite.RIGHT;
						}
					}
				}
				else {
					// bendpoint is directly above or below shape
					if (movedBendpoint.getY() < tPos.getY()) {
						// bendpoint is above shape
						if (expandTargetSites) {
							targetAnchorSites = new AnchorSite[3];
							targetAnchorSites[0] = AnchorSite.TOP;
							targetAnchorSites[1] = AnchorSite.LEFT;
							targetAnchorSites[2] = AnchorSite.RIGHT;
						}
						else {
							targetAnchorSites = new AnchorSite[1];
							targetAnchorSites[0] = AnchorSite.TOP;
						}
					}
					else if (tPos.getY()+tSize.getHeight() < movedBendpoint.getY()) {
						// bendpoint is below shape
						if (expandTargetSites) {
							targetAnchorSites = new AnchorSite[3];
							targetAnchorSites[0] = AnchorSite.BOTTOM;
							targetAnchorSites[1] = AnchorSite.LEFT;
							targetAnchorSites[2] = AnchorSite.RIGHT;
						}
						else {
							targetAnchorSites = new AnchorSite[1];
							targetAnchorSites[0] = AnchorSite.BOTTOM;
						}
					}
					else {
						// bendpoint is inside shape
						targetAnchorSites = new AnchorSite[0];
					}
				}
			}
			return;
		}

		// find relative locations
		if (sPos.getX()+sSize.getWidth() < tPos.getX()) {
			// source shape is to left of target
			if (sPos.getY()+sSize.getHeight() < tPos.getY()) {
				// source shape is to left and above target:
				// omit the two opposite sides of both source and target
				if (sourceAnchorSites==null) {
					sourceAnchorSites = new AnchorSite[2];
					sourceAnchorSites[0] = AnchorSite.RIGHT;
					sourceAnchorSites[1] = AnchorSite.BOTTOM;
				}
				if (targetAnchorSites==null) {
					targetAnchorSites = new AnchorSite[2];
					targetAnchorSites[0] = AnchorSite.LEFT;
					targetAnchorSites[1] = AnchorSite.TOP;
				}
			}
			else if(sPos.getY() > tPos.getY()+tSize.getHeight()) {
				// source shape is to left and below target
				if (sourceAnchorSites==null) {
					sourceAnchorSites = new AnchorSite[2];
					sourceAnchorSites[0] = AnchorSite.RIGHT;
					sourceAnchorSites[1] = AnchorSite.TOP;
				}
				if (targetAnchorSites==null) {
					targetAnchorSites = new AnchorSite[2];
					targetAnchorSites[0] = AnchorSite.LEFT;
					targetAnchorSites[1] = AnchorSite.BOTTOM;
				}
			}
			else {
				if (sourceAnchorSites==null) {
					sourceAnchorSites = new AnchorSite[3];
					sourceAnchorSites[0] = AnchorSite.RIGHT;
					sourceAnchorSites[1] = AnchorSite.TOP;
					sourceAnchorSites[2] = AnchorSite.BOTTOM;
				}
				if (targetAnchorSites==null) {
					targetAnchorSites = new AnchorSite[3];
					targetAnchorSites[0] = AnchorSite.LEFT;
					targetAnchorSites[1] = AnchorSite.TOP;
					targetAnchorSites[2] = AnchorSite.BOTTOM;
				}
			}
		}
		else if (sPos.getX() > tPos.getX()+tSize.getWidth()) {
			// source shape is to right of target
			if (sPos.getY()+sSize.getHeight() < tPos.getY()) {
				// source shape is to right and above target
				if (sourceAnchorSites==null) {
					sourceAnchorSites = new AnchorSite[2];
					sourceAnchorSites[0] = AnchorSite.LEFT;
					sourceAnchorSites[1] = AnchorSite.BOTTOM;
				}
				if (targetAnchorSites==null) {
					targetAnchorSites = new AnchorSite[2];
					targetAnchorSites[0] = AnchorSite.RIGHT;
					targetAnchorSites[1] = AnchorSite.TOP;
				}
			}
			else if(sPos.getY() > tPos.getY()+tSize.getHeight()) {
				// source shape is to right and below target
				if (sourceAnchorSites==null) {
					sourceAnchorSites = new AnchorSite[2];
					sourceAnchorSites[0] = AnchorSite.LEFT;
					sourceAnchorSites[1] = AnchorSite.TOP;
				}
				if (targetAnchorSites==null) {
					targetAnchorSites = new AnchorSite[2];
					targetAnchorSites[0] = AnchorSite.RIGHT;
					targetAnchorSites[1] = AnchorSite.BOTTOM;
				}
			}
			else {
				if (sourceAnchorSites==null) {
					sourceAnchorSites = new AnchorSite[3];
					sourceAnchorSites[0] = AnchorSite.LEFT;
					sourceAnchorSites[1] = AnchorSite.TOP;
					sourceAnchorSites[2] = AnchorSite.BOTTOM;
				}
				if (targetAnchorSites==null) {
					targetAnchorSites = new AnchorSite[3];
					targetAnchorSites[0] = AnchorSite.RIGHT;
					targetAnchorSites[1] = AnchorSite.TOP;
					targetAnchorSites[2] = AnchorSite.BOTTOM;
				}
			}
		}
		else if (sPos.getY()+sSize.getHeight() < tPos.getY()) {
			// source shape is above target
			if (sourceAnchorSites==null) {
				sourceAnchorSites = new AnchorSite[3];
				sourceAnchorSites[0] = AnchorSite.LEFT;
				sourceAnchorSites[1] = AnchorSite.RIGHT;
				sourceAnchorSites[2] = AnchorSite.BOTTOM;
			}
			if (targetAnchorSites==null) {
				targetAnchorSites = new AnchorSite[3];
				targetAnchorSites[0] = AnchorSite.LEFT;
				targetAnchorSites[1] = AnchorSite.RIGHT;
				targetAnchorSites[2] = AnchorSite.TOP;
			}
		}
		else if(sPos.getY() > tPos.getY()+tSize.getHeight()) {
			// source shape is below target
			if (sourceAnchorSites==null) {
				sourceAnchorSites = new AnchorSite[3];
				sourceAnchorSites[0] = AnchorSite.LEFT;
				sourceAnchorSites[1] = AnchorSite.RIGHT;
				sourceAnchorSites[2] = AnchorSite.TOP;
			}
			if (targetAnchorSites==null) {
				targetAnchorSites = new AnchorSite[3];
				targetAnchorSites[0] = AnchorSite.LEFT;
				targetAnchorSites[1] = AnchorSite.RIGHT;
				targetAnchorSites[2] = AnchorSite.BOTTOM;
			}
		}
		else {
			// source and target overlap
			if (sourceAnchorSites==null) {
				sourceAnchorSites = new AnchorSite[4];
				sourceAnchorSites[0] = AnchorSite.LEFT;
				sourceAnchorSites[1] = AnchorSite.RIGHT;
				sourceAnchorSites[2] = AnchorSite.TOP;
				sourceAnchorSites[3] = AnchorSite.BOTTOM;
			}
			if (targetAnchorSites==null) {
				targetAnchorSites = new AnchorSite[4];
				targetAnchorSites[0] = AnchorSite.LEFT;
				targetAnchorSites[1] = AnchorSite.RIGHT;
				targetAnchorSites[2] = AnchorSite.TOP;
				targetAnchorSites[3] = AnchorSite.BOTTOM;
			}
		}
	}

	protected AnchorSite[] calculateBoundaryEventAnchorSites(Shape shape) {
		AnchorSite sites[];
		PositionOnLine pol = BoundaryEventPositionHelper.getPositionOnLineProperty(shape);
		switch (pol.getLocationType()) {
		case BOTTOM:
			sites = new AnchorSite[1];
			sites[0] = AnchorSite.BOTTOM;
			break;
		case BOTTOM_LEFT:
			sites = new AnchorSite[2];
			sites[0] = AnchorSite.BOTTOM;
			sites[1] = AnchorSite.LEFT;
			break;
		case BOTTOM_RIGHT:
			sites = new AnchorSite[2];
			sites[0] = AnchorSite.BOTTOM;
			sites[1] = AnchorSite.RIGHT;
			break;
		case LEFT:
			sites = new AnchorSite[1];
			sites[0] = AnchorSite.LEFT;
			break;
		case RIGHT:
			sites = new AnchorSite[1];
			sites[0] = AnchorSite.RIGHT;
			break;
		case TOP:
			sites = new AnchorSite[1];
			sites[0] = AnchorSite.TOP;
			break;
		case TOP_LEFT:
			sites = new AnchorSite[2];
			sites[0] = AnchorSite.TOP;
			sites[1] = AnchorSite.LEFT;
			break;
		case TOP_RIGHT:
			sites = new AnchorSite[2];
			sites[0] = AnchorSite.TOP;
			sites[1] = AnchorSite.RIGHT;
			break;
		default:
			sites = new AnchorSite[4];
			sites[0] = AnchorSite.TOP;
			sites[1] = AnchorSite.LEFT;
			sites[2] = AnchorSite.BOTTOM;
			sites[3] = AnchorSite.RIGHT;
			break;
		}
		return sites;
	}

	protected boolean shouldCalculate(AnchorSite sourceSite, AnchorSite targetSite) {
		for (int i=0; i<sourceAnchorSites.length; ++i) {
			if (sourceSite == sourceAnchorSites[i]) {
				for (int j=0; j<targetAnchorSites.length; ++j) {
					if (targetSite == targetAnchorSites[j]) {
						return true;
					}
				}				
			}
		}
		return false;
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

		ConnectionRoute route = new ConnectionRoute(this, 1, source, target);
		
		Point start = GraphicsUtil.createPoint(sourceAnchor);
		Point end = GraphicsUtil.createPoint(targetAnchor);

		route.add(start);
		for (int i=1; i<oldPoints.length -1; ++i) {
			route.add(oldPoints[i]);
		}
		route.add(end);
		
		return route;
	}
	
	/**
	 * Route connections whose source and target are the same. This only
	 * reroutes the connection if there are currently no bendpoints in the
	 * connection - we don't want to reroute a connection that may have already
	 * been manually rerouted.
	 *
	 * @return true if the router has done any work
	 */
	protected ConnectionRoute calculateSelfConnectionRoute() {
		if (!isSelfConnection())
			return null;
		
		if (movedBendpoint==null) {
			if (ffc.getStart() != ffc.getEnd() && ffc.getBendpoints().size()>0) {
				// this connection starts and ends at the same node but it has different
				// anchor cuts and at least one bendpoint, which makes it likely that
				// this connection was already routed previously and the self-connection
				// is how the user wants it. But, check if the user wants to force routing.
				if (!forceRouting(ffc))
					return null;
			}
		}
		
		ILocation loc = peService.getLocationRelativeToDiagram(target);
		IDimension size = GraphicsUtil.calculateSize(target);
		Point p;
		
		p = Graphiti.getCreateService().createPoint(loc.getX()+size.getWidth()/2, loc.getY());
		FixPointAnchor topAnchor = (FixPointAnchor) ffc.getEnd();
		AnchorUtil.moveAnchor(topAnchor, p);

		p = Graphiti.getCreateService().createPoint(loc.getX()+size.getWidth(), loc.getY()+size.getHeight()/2);
		FixPointAnchor rightAnchor = (FixPointAnchor) ffc.getStart();
		AnchorUtil.moveAnchor(rightAnchor, p);

		// create the bendpoints that loop the connection around the top-right corner of the figure
		Point right = GraphicsUtil.createPoint(rightAnchor); // the point to the right of the node
		right.setX(right.getX() + 20);
		
		Point top = GraphicsUtil.createPoint(topAnchor); // point above the node
		top.setY(top.getY() - 20);

		Point corner = Graphiti.getCreateService().createPoint(right.getX(), top.getY()); // point above the top-right corner 
		
		// adjust these cuts to the moved or added bendpoint if possible
		p = movedBendpoint;
		if (p!=null) {
			int x = p.getX();
			int y = p.getY();
			if (x > loc.getX() + size.getWidth() + 2) {
				right.setX(x);
				corner.setX(x);
			}
			if (y < loc.getY() - 2) {
				top.setY(y);
				corner.setY(y);
			}
		}

		// and add them to the new Route
		ConnectionRoute route = new ConnectionRoute(this,1,source,target);
		route.add(GraphicsUtil.createPoint(rightAnchor));
		route.add(right);
		route.add(corner);
		route.add(top);
		route.add(GraphicsUtil.createPoint(topAnchor));

		return route;
	}
	
	/**
	 * Compare the connection's original start/end locations and all of its
	 * bendpoints with the newly calculated route.
	 *
	 * @param route the route
	 * @return true if the connection is different from the newly calculated
	 *         route
	 */
	protected boolean isRouteChanged(ConnectionRoute route) {
		if (route==null || route.size()==0)
			return false;
		if (oldPoints.length!=route.size()) {
			return true;
		}
		for (int i=0; i<oldPoints.length; ++i) {
			Point p1 = oldPoints[i];
			Point p2 = route.get(i);
			if (!GraphicsUtil.pointsEqual(p1, p2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set the connection's new start/end point anchors and the newly calculated
	 * bendpoints.
	 *
	 * @param route the route
	 */
	protected void applyRoute(ConnectionRoute route) {
		route.apply(ffc, sourceAnchor, targetAnchor);
		DIUtils.updateDIEdge(ffc);
	}

	protected AnchorSite getNextAnchorSite(AnchorSite site) {
		switch (site) {
		case BOTTOM:
			return AnchorSite.LEFT;
		case LEFT:
			return AnchorSite.RIGHT;
		case RIGHT:
			return AnchorSite.TOP;
		case TOP:
			return AnchorSite.BOTTOM;
		case CENTER:
			return AnchorSite.CENTER;
		}
		return site;
	}

	/**
	 * Set a property in the given FreeFormConnection that represents the index
	 * of an existing bendpoint that has been moved by the user. This bendpoint
	 * is taken into consideration in the new routing calculations.
	 * 
	 * @param connection - FreeFormConnection to check
	 * @param index - index of a bendpoint. If this value is out of range, the
	 *            property will be remmoved from the connection
	 */
	public static void setMovedBendpoint(Connection connection, int index) {
		setInterestingBendpoint(connection, "moved.", index); //$NON-NLS-1$
	}

	/**
	 * Sets the added bendpoint.
	 *
	 * @param connection the connection
	 * @param index the index
	 */
	public static void setAddedBendpoint(Connection connection, int index) {
		setInterestingBendpoint(connection, "added.", index); //$NON-NLS-1$
	}

	/**
	 * Sets the removed bendpoint.
	 *
	 * @param connection the connection
	 * @param index the index
	 */
	public static void setRemovedBendpoint(Connection connection, Point p) {
		AbstractConnectionRouter.setRoutingInfo(connection, "removed."+ROUTING_INFO_BENDPOINT, p.getX() + "." + p.getY()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Sets the fixed bendpoint.
	 *
	 * @param connection the connection
	 * @param index the index
	 */
	public static void setFixedBendpoint(Connection connection, int index) {
		setInterestingBendpoint(connection, "fixed."+index+".", index); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static void setOldBendpointLocation(Connection connection, Point p) {
		AbstractConnectionRouter.setRoutingInfo(connection, "oldloc."+ROUTING_INFO_BENDPOINT, p.getX() + "." + p.getY()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Sets the interesting bendpoint.
	 *
	 * @param connection the connection
	 * @param type the type
	 * @param index the index
	 */
	protected static void setInterestingBendpoint(Connection connection, String type, int index) {
		if (connection instanceof FreeFormConnection) {
			int size = ((FreeFormConnection)connection).getBendpoints().size();
			if (index>=0 && size>0) {
				if (index>=size)
					index = size-1;
				AbstractConnectionRouter.setRoutingInfoInt(connection, type+ROUTING_INFO_BENDPOINT, index);
			}
			else
				AbstractConnectionRouter.removeRoutingInfo(connection, type+ROUTING_INFO_BENDPOINT);
		}
	}

	/**
	 * Return the "moved bendpoint" property that was previously set in the
	 * FreeFormConnection by setMovedBendpoint()
	 * 
	 * @param connection - FreeFormConnection to check
	 * @return a Graphiti Point in Diagram-relative coordinates, or null if the
	 *         property is not set
	 */
	public static Point getMovedBendpoint(Connection connection) {
		return getInterestingBendpoint(connection, "moved."); //$NON-NLS-1$
	}
	
	/**
	 * Gets the added bendpoint.
	 *
	 * @param connection the connection
	 * @return the added bendpoint
	 */
	public static Point getAddedBendpoint(Connection connection) {
		return getInterestingBendpoint(connection, "added."); //$NON-NLS-1$
	}
	
	/**
	 * Gets the removed bendpoint.
	 *
	 * @param connection the connection
	 * @return the removed bendpoint
	 */
	public static Point getRemovedBendpoint(Connection connection) {
		String value = AbstractConnectionRouter.getRoutingInfo(connection, "removed."+ROUTING_INFO_BENDPOINT); //$NON-NLS-1$
		if (value!=null && !value.isEmpty()) {
			String b[] = value.split("\\."); //$NON-NLS-1$
			int x = Integer.parseInt(b[0]);
			int y = Integer.parseInt(b[1]);
			return GraphicsUtil.createPoint(x, y);
		}
		return null;
	}
	
	/**
	 * Gets the fixed bendpoint.
	 *
	 * @param connection the connection
	 * @param index the index
	 * @return the fixed bendpoint
	 */
	public static Point getFixedBendpoint(Connection connection, int index) {
		return getInterestingBendpoint(connection, "fixed."+index+"."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static Point getOldBendpointLocation(Connection connection) {
		String value = AbstractConnectionRouter.getRoutingInfo(connection, "oldloc."+ROUTING_INFO_BENDPOINT); //$NON-NLS-1$
		if (value!=null && !value.isEmpty()) {
			String b[] = value.split("\\."); //$NON-NLS-1$
			int x = Integer.parseInt(b[0]);
			int y = Integer.parseInt(b[1]);
			return GraphicsUtil.createPoint(x, y);
		}
		return null;
	}

	/**
	 * Gets the interesting bendpoint.
	 *
	 * @param connection the connection
	 * @param type the type
	 * @return the interesting bendpoint
	 */
	protected static Point getInterestingBendpoint(Connection connection, String type) {
		try {
			int index = AbstractConnectionRouter.getRoutingInfoInt(connection, type+ROUTING_INFO_BENDPOINT);
			return ((FreeFormConnection)connection).getBendpoints().get(index);
		}
		catch (Exception e) {
		}
		return null;
	}
}
