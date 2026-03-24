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
 * @author Ivar Meikas
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil.LineSegment;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.mm.pictograms.PictogramsFactory;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.ICreateService;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;

public class AnchorUtil {

	public static final int CONNECTION_POINT_SIZE = 4;

	private static final IPeService peService = Graphiti.getPeService();
	private static final IGaService gaService = Graphiti.getGaService();
	private static final ICreateService createService = Graphiti.getCreateService();

	/**
	 * This Connection Anchor Adapter listens for changes in the number of
	 * Connections on the given Anchor and deletes the AnchorContainer shape (an
	 * instance of ConnectionDecorator) when the incoming and outgoing
	 * connection count is zero.
	 */
	private static class ConnectionAnchorAdapter extends AdapterImpl {
		Anchor anchor;
		private boolean deleting = false;
		
		public static ConnectionAnchorAdapter adapt(Anchor anchor) {
			return new ConnectionAnchorAdapter(anchor);
		}
		
		private ConnectionAnchorAdapter(Anchor anchor) {
			this.anchor = anchor;
			anchor.eAdapters().add(this);
			setTarget(anchor);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.emf.common.notify.Adapter#notifyChanged(org.eclipse.emf.common.notify.Notification)
		 */
		@Override
		public void notifyChanged(Notification notification) {
			// if the connection is no longer connected to this shape
			// then delete the Connection Point
			if (!deleting) {
				if (notification.getNotifier() == anchor) {
					// something changed in the anchor
					int et = notification.getEventType();
					if (et==Notification.REMOVE) {
						if (anchor.getIncomingConnections().isEmpty() && anchor.getOutgoingConnections().isEmpty()) {
							// the anchor has no connections, so we need to delete it
							deleting = true;
							if (anchor.eContainer()!=null)
								Graphiti.getPeService().deletePictogramElement(anchor.getParent());
						}
					}
				}
			}
		}
	}
	
	public static FixPointAnchor createAnchor(AnchorContainer ac, AnchorSite site) {
		if (ac instanceof Shape) {
			ILocation loc = peService.getLocationRelativeToDiagram((Shape)ac);
			IDimension size = GraphicsUtil.calculateSize((Shape)ac);
			int x = 0;
			int y = 0;
			switch (site) {
			case BOTTOM:
				x = loc.getX() + size.getWidth()/2;
				y = loc.getY() + size.getHeight();
				break;
			case LEFT:
				x = loc.getX();
				y = loc.getY() + size.getHeight()/2;
				break;
			case RIGHT:
				x = loc.getX() + size.getWidth();
				y = loc.getY() + size.getHeight()/2;
				break;
			case TOP:
				x = loc.getX() + size.getWidth()/2;
				y = loc.getY();
				break;
			case CENTER:
				x = loc.getX() + size.getWidth()/2;
				y = loc.getY() + size.getHeight()/2;
				break;
			default:
				break;
			}
			return createAnchor(ac, x, y);
		}
		return null;
	}
	
	public static FixPointAnchor createAnchor(AnchorContainer ac, int x, int y, AnchorType at) {
		return createAnchor(ac, gaService.createPoint(x, y), at);
	}
	
	public static FixPointAnchor createAnchor(AnchorContainer ac, Point p, AnchorType at) {
		FixPointAnchor anchor = createBoundaryAnchor(ac, p);
		AnchorType.setType(anchor, at);
		return anchor;
	}
	
	public static FixPointAnchor createAnchor(AnchorContainer ac, int x, int y) {
		return createAnchor(ac, gaService.createPoint(x, y));
	}
	
	public static FixPointAnchor createAnchor(AnchorContainer ac, Point p) {
		if (isConnectionAnchorContainer(ac)) {
			return (FixPointAnchor)ac.getAnchors().get(0);
		}
		else if (ac instanceof Connection) {
			return createConnectionAnchor((Connection)ac);
		}
		FixPointAnchor anchor = createBoundaryAnchor(ac, p);
		AnchorType at = AnchorType.getType(ac);
		AnchorType.setType(anchor, at);
		return anchor;
	}
	
	public static List<FixPointAnchor> getAnchors(AnchorContainer ac, AnchorSite site) {
		List<FixPointAnchor> result = new ArrayList<FixPointAnchor>();
		for (Anchor anchor : ac.getAnchors()) {
			if (FeatureSupport.getPropertyValue(anchor, GraphitiConstants.ANCHOR_TYPE)!=null &&
				AnchorSite.getSite((FixPointAnchor)anchor)==site)
				result.add((FixPointAnchor)anchor);
		}
		return result;
	}
	
	public static List<FixPointAnchor> getAnchors(AnchorContainer ac) {
		List<FixPointAnchor> result = new ArrayList<FixPointAnchor>();
		if (ac instanceof Connection) {
			Connection connection = (Connection) ac;
			for (ConnectionDecorator cd : connection.getConnectionDecorators()) {
				if (isConnectionAnchorContainer(cd)) {
					for (Anchor a : cd.getAnchors())
						result.add((FixPointAnchor)a);
				}
			}
		}
		else if (ac != null) {
			for (Anchor anchor : ac.getAnchors()) {
				if (FeatureSupport.getPropertyValue(anchor, GraphitiConstants.ANCHOR_TYPE)!=null)
					result.add((FixPointAnchor)anchor);
			}
		}
		return result;
	}
	
	public static int countAnchors(AnchorContainer ac, AnchorSite site) {
		int count = 0;
		for (Anchor anchor : ac.getAnchors()) {
			if (FeatureSupport.getPropertyValue(anchor, GraphitiConstants.ANCHOR_TYPE)!=null &&
				AnchorSite.getSite((FixPointAnchor)anchor)==site)
				++count;
		}
		return count;
	}

	public static void moveAnchor(FixPointAnchor anchor, int x, int y) {
		moveAnchor(anchor, GraphicsUtil.createPoint(x,y));
	}
	
	public static void moveAnchor(FixPointAnchor anchor, Point p) {
		AnchorContainer parent = anchor.getParent();
		if (isConnectionAnchorContainer(parent)) {
			// these can't be moved
			return;
		}
		if (parent instanceof Shape) {
			p = GraphicsUtil.createPoint(p); // make a copy
			ILocation loc = peService.getLocationRelativeToDiagram((Shape)parent);
			AnchorType at = AnchorType.getType(anchor);
			LineSegment edge;
			if (at==AnchorType.POOL)
				edge = GraphicsUtil.findNearestOrthogonalEdge((Shape)parent, p);
			else
				edge = GraphicsUtil.findNearestEdge((Shape)parent, p);
			AnchorSite site;
			if (edge.isHorizontal()) {
				int y = edge.getStart().getY();
				if (y==loc.getY())
					site = AnchorSite.TOP;
				else
					site = AnchorSite.BOTTOM;
				p.setY(y - loc.getY());
				int x = p.getX();
//				if (FeatureSupport.isParticipant(parent)) {
					if (x < edge.getStart().getX())
						x = edge.getStart().getX();
					if (x > edge.getEnd().getX())
						x = edge.getEnd().getX();
//				}
//				else
//					x = edge.getMiddle().getX();

				p.setX(x - loc.getX());
			}
			else {
				int x = edge.getStart().getX();
				if (x==loc.getX())
					site = AnchorSite.LEFT;
				else
					site = AnchorSite.RIGHT;
				p.setX(x - loc.getX());
				int y = p.getY();
//				if (FeatureSupport.isParticipant(parent)) {
					if (y < edge.getStart().getY())
						y = edge.getStart().getY();
					if (y > edge.getEnd().getY())
						y = edge.getEnd().getY();
//				}
//				else
//					x = edge.getMiddle().getY();
				p.setY(y - loc.getY());
			}
			AnchorSite.setSite(anchor, site);
		}
		anchor.setLocation(p);
	}

	public static void moveAnchor(FixPointAnchor anchor, FixPointAnchor refAnchor) {
		AnchorContainer parent = anchor.getParent();
		if (isConnectionAnchorContainer(parent)) {
			// these can't be moved
			return;
		}

		AnchorContainer refParent = refAnchor.getParent();
		AnchorSite refSite = AnchorSite.getSite(refAnchor);
		
		Point p = GraphicsUtil.createPoint(refAnchor);
		
		if (parent instanceof Shape && refParent instanceof Shape && refSite!=null) {
			LineSegment refEdge = GraphicsUtil.getEdges((Shape)refParent)[refSite.ordinal()];
			ILocation loc = peService.getLocationRelativeToDiagram((Shape)parent);
			LineSegment edge = GraphicsUtil.findNearestEdge((Shape)parent, refEdge.getStart(), refEdge.getEnd());
			AnchorSite site;
			if (edge.isHorizontal()) {
				int y = edge.getStart().getY();
				if (y==loc.getY())
					site = AnchorSite.TOP;
				else
					site = AnchorSite.BOTTOM;
				p.setY(y - loc.getY());
				int x = p.getX();
				if (x < edge.getStart().getX())
					x = edge.getStart().getX();
				if (x > edge.getEnd().getX())
					x = edge.getEnd().getX();
				p.setX(x - loc.getX());
			}
			else {
				int x = edge.getStart().getX();
				if (x==loc.getX())
					site = AnchorSite.LEFT;
				else
					site = AnchorSite.RIGHT;
				p.setX(x - loc.getX());
				int y = p.getY();
				if (y < edge.getStart().getY())
					y = edge.getStart().getY();
				if (y > edge.getEnd().getY())
					y = edge.getEnd().getY();
				p.setY(y - loc.getY());
			}
			AnchorSite.setSite(anchor, site);
			anchor.setLocation(p);
		}
		else
			moveAnchor(anchor, p);
	}
	
	public static List<Connection> getConnections(AnchorContainer ac, AnchorSite site) {
		List<Connection> connections = new ArrayList<Connection>();
		for (Anchor a : ac.getAnchors()) {
			if (FeatureSupport.getPropertyValue(a, GraphitiConstants.ANCHOR_TYPE)!=null)
				if (site==AnchorSite.getSite((FixPointAnchor)a)) {
					connections.addAll(a.getIncomingConnections());
					connections.addAll(a.getOutgoingConnections());
				}
		}
		return connections;
	}

	public static void adjustAnchors(AnchorContainer ac) {
		if (ac instanceof ConnectionDecorator) {
			for (Anchor a : ac.getAnchors()) {
				String property = FeatureSupport.getPropertyValue(a, GraphitiConstants.ANCHOR_TYPE);
				if ( property!=null &&
						a.getIncomingConnections().isEmpty() &&
						a.getOutgoingConnections().isEmpty()) {
					peService.deletePictogramElement(ac);
					return;
				}
			}
		}
		else if (ac!=null){
			deleteUnusedAnchors(ac);
			relocateAnchors(ac);
		}
	}

	public static AnchorContainer getAnchorContainer(Anchor anchor) {
		AnchorContainer ac = anchor.getParent();
		return ac;
	}
	
	public static Hashtable<AnchorSite, List<FixPointAnchor>> countAnchors(AnchorContainer ac) {
		Hashtable<AnchorSite, List<FixPointAnchor>> result = new Hashtable<AnchorSite, List<FixPointAnchor>>(); 
		List<FixPointAnchor> topAnchors = null;
		List<FixPointAnchor> bottomAnchors = null;
		List<FixPointAnchor> leftAnchors = null;
		List<FixPointAnchor> rightAnchors = null;
		for (Anchor a : ac.getAnchors()) {
			AnchorType at = AnchorType.getType(a);
			if (at == AnchorType.ACTIVITY || at == AnchorType.MESSAGELINK) {
				// Count all of the Activity Anchors 
				FixPointAnchor anchor = (FixPointAnchor) a;
				AnchorSite site = AnchorSite.getSite(anchor);
				switch (site) {
				case BOTTOM:
					if (bottomAnchors==null) {
						bottomAnchors = new ArrayList<FixPointAnchor>();
						result.put(site, bottomAnchors);
					}
					bottomAnchors.add(anchor);
					break;
				case CENTER:
					break;
				case LEFT:
					if (leftAnchors==null) {
						leftAnchors = new ArrayList<FixPointAnchor>();
						result.put(site, leftAnchors);
					}
					leftAnchors.add(anchor);
					break;
				case RIGHT:
					if (rightAnchors==null) {
						rightAnchors = new ArrayList<FixPointAnchor>();
						result.put(site, rightAnchors);
					}
					rightAnchors.add(anchor);
					break;
				case TOP:
					if (topAnchors==null) {
						topAnchors = new ArrayList<FixPointAnchor>();
						result.put(site, topAnchors);
					}
					topAnchors.add(anchor);
					break;
				default:
					break;
				
				}
			}
		}
		return result;
	}
	
	public static Map<Anchor,Point> saveAnchorLocations(AnchorContainer ac) {
		Map<Anchor, Point> points = new Hashtable<Anchor,Point>();
		for (Anchor a : ac.getAnchors()) {
			if (a instanceof FixPointAnchor) {
				Point p = GraphicsUtil.createPoint(((FixPointAnchor) a).getLocation());
				points.put(a, p);
			}
		}
		return points;
	}
	
	public static void restoreAnchorLocations(AnchorContainer ac, Map<Anchor,Point> points) {
		for (Anchor a : ac.getAnchors()) {
			if (a instanceof FixPointAnchor) {
				Point p = points.get(a);
				if (p!=null) {
					((FixPointAnchor) a).setLocation(p);
				}
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	// Private API
	/////////////////////////////////////////////////////////////////////

	private static FixPointAnchor createBoundaryAnchor(AnchorContainer ac, Point p) {
		FixPointAnchor anchor = peService.createFixPointAnchor(ac);
		moveAnchor(anchor, p);
		gaService.createInvisibleRectangle(anchor);

		return anchor;
	}

	private static FixPointAnchor createConnectionAnchor(Connection connection) {

		Diagram diagram = peService.getDiagramForPictogramElement(connection);
		// First create a Connection Decorator and set its location according to the given Point
		// If the Point is not provided, use the coordinates of the Connection midpoint.
		ConnectionDecorator decorator = createService.createConnectionDecorator(connection, true, 0.5, true);
		FeatureSupport.setPropertyValue(decorator, GraphitiConstants.CONNECTION_POINT, Boolean.TRUE.toString());
		Rectangle rectangle = createService.createRectangle(decorator);
		rectangle.setFilled(true);
		rectangle.setForeground(Graphiti.getGaService().manageColor(diagram, StyleUtil.CLASS_FOREGROUND));
		rectangle.setX(-CONNECTION_POINT_SIZE/2);
		rectangle.setY(-CONNECTION_POINT_SIZE/2);
		rectangle.setWidth(CONNECTION_POINT_SIZE);
		rectangle.setHeight(CONNECTION_POINT_SIZE);

		PictogramLink link = createPictogramLink(decorator);
		BaseElement be = BusinessObjectUtil.getFirstBaseElement(connection);
		link.getBusinessObjects().add(be);
		
		FixPointAnchor anchor = createService.createFixPointAnchor(decorator);
		AnchorSite.setSite(anchor, AnchorSite.CENTER);
		AnchorType.setType(anchor, AnchorType.CONNECTION);
		anchor.setLocation(GraphicsUtil.createPoint(CONNECTION_POINT_SIZE/2, CONNECTION_POINT_SIZE/2));
		FeatureSupport.setPropertyValue(anchor, GraphitiConstants.CONNECTION_POINT, Boolean.TRUE.toString());
		
		// if the anchor doesn't have a GraphicsAlgorithm, GEF will throw a fit
		// so create an invisible rectangle for it
		createService.createInvisibleRectangle(anchor);

		// add an adapter that will delete the Connection Decorator when the last
		// connection on its Anchor is removed
		ConnectionAnchorAdapter.adapt(anchor);
		
		return anchor;
	}

	private static boolean isConnectionAnchorContainer(AnchorContainer ac) {
		return Boolean.TRUE.toString().equals(FeatureSupport.getPropertyValue(ac, GraphitiConstants.CONNECTION_POINT));
	}
	
	/**
	 * @param parent
	 */
	private static void deleteUnusedAnchors(AnchorContainer ac) {
		if (ac!=null) {
			List<Anchor> deleted = new ArrayList<Anchor>();
	
			for (Anchor a : ac.getAnchors()) {
				String property = FeatureSupport.getPropertyValue(a, GraphitiConstants.ANCHOR_TYPE);
				if ( property!=null &&
						a.getIncomingConnections().isEmpty() &&
						a.getOutgoingConnections().isEmpty()) {
					deleted.add(a);
				}
			}
	
			for (Anchor a : deleted) {
				peService.deletePictogramElement(a);
			}
		}
	}
	
	private static void relocateAnchors(AnchorContainer ac) {
		IDimension size = GraphicsUtil.calculateSize(ac);
		relocateAnchors(ac, size.getWidth(), size.getHeight());
	}
	
	private static void relocateAnchors(AnchorContainer ac, int w, int h) {

		if (ac==null || isConnectionAnchorContainer(ac))
			return;

		// Calculated offsets for each Activity Anchor
		int topOffset = 0;
		int bottomOffset = 0;
		int leftOffset = 0;
		int rightOffset = 0;
		// Connections attached to each anchor
		Hashtable<AnchorSite, List<FixPointAnchor>> anchors = countAnchors(ac);
		List<FixPointAnchor> topAnchors = anchors.get(AnchorSite.TOP);
		List<FixPointAnchor> bottomAnchors = anchors.get(AnchorSite.BOTTOM);
		List<FixPointAnchor> leftAnchors = anchors.get(AnchorSite.LEFT);
		List<FixPointAnchor> rightAnchors = anchors.get(AnchorSite.RIGHT);
		// These are the number of Activity Anchors along each
		// edge of the Activity. These anchors are distributed
		// evenly along an edge of the shape
		int topCount = topAnchors==null ? 0 : topAnchors.size();
		int bottomCount = bottomAnchors==null ? 0 : bottomAnchors.size();
		int leftCount = leftAnchors==null ? 0 : leftAnchors.size();
		int rightCount = rightAnchors==null ? 0 : rightAnchors.size();

		for (Anchor a : ac.getAnchors()) {
			AnchorType at = AnchorType.getType(a);
			if (at == AnchorType.ACTIVITY) {
				// adjust the Activity Anchors
				int index; 
				FixPointAnchor anchor = (FixPointAnchor) a;
				AnchorSite site = AnchorSite.getSite(anchor);
				switch (site) {
				case BOTTOM:
					index = calculateIndex(anchor, bottomAnchors);
					if (index>=0)
						bottomOffset = (index+1) * w/(bottomCount+1);
					else
						bottomOffset += w/(bottomCount+1);
					anchor.setLocation(gaService.createPoint(bottomOffset, h));
					break;
				case CENTER:
					break;
				case LEFT:
					index = calculateIndex(anchor, leftAnchors);
					if (index>=0)
						leftOffset = (index+1) * h/(leftCount+1);
					else
						leftOffset += h/(leftCount+1);
					anchor.setLocation(gaService.createPoint(0, leftOffset));
					break;
				case RIGHT:
					index = calculateIndex(anchor, rightAnchors);
					if (index>=0)
						rightOffset = (index+1) * h/(rightCount+1);
					else
						rightOffset += h/(rightCount+1);
					anchor.setLocation(gaService.createPoint(w, rightOffset));
					break;
				case TOP:
					index = calculateIndex(anchor, topAnchors);
					if (index>=0)
						topOffset = (index+1) * w/(topCount+1);
					else
						topOffset += w/(topCount+1);
					anchor.setLocation(gaService.createPoint(topOffset, 0));
					break;
				default:
					break;
				}
			}
			else if (at == AnchorType.GATEWAY || at == AnchorType.MESSAGELINK) {
				// adjust the Gateway and Event Anchors: all of these
				// are attached to the middle of an edge
				FixPointAnchor anchor = (FixPointAnchor) a;
				AnchorSite site = AnchorSite.getSite(anchor);
				switch (site) {
				case BOTTOM:
					anchor.setLocation(gaService.createPoint(w/2, h));
					break;
				case CENTER:
					break;
				case LEFT:
					anchor.setLocation(gaService.createPoint(0, h/2));
					break;
				case RIGHT:
					anchor.setLocation(gaService.createPoint(w, h/2));
					break;
				case TOP:
					anchor.setLocation(gaService.createPoint(w/2, 0));
					break;
				default:
					break;
				}
			}
			else if (at == AnchorType.POOL) {
				// adjust Pool Anchors: these are placed by the user at a specific
				// point on an edge of the Pool. The position of these anchors may
				// be adjusted by the Connection Routers for optimal routing.
				FixPointAnchor anchor = (FixPointAnchor) a;
				Point p = anchor.getLocation();
				if (p.getX() > w)
					p.setX(w);
				if (p.getY() > h)
					p.setY(h);
				AnchorSite site = AnchorSite.getSite(anchor);
				switch (site) {
				case BOTTOM:
					anchor.setLocation(gaService.createPoint(p.getX(), h));
					break;
				case CENTER:
					break;
				case LEFT:
					anchor.setLocation(gaService.createPoint(0, p.getY()));
					break;
				case RIGHT:
					anchor.setLocation(gaService.createPoint(w, p.getY()));
					break;
				case TOP:
					anchor.setLocation(gaService.createPoint(p.getX(), 0));
					break;
				default:
					break;
				
				}
			}
		}
	}

	private static FixPointAnchor getOpposite(FixPointAnchor anchor) {
		Anchor a = null;
		if (anchor.getOutgoingConnections().isEmpty()) {
			if (anchor.getIncomingConnections().isEmpty())
				return null;
			a = anchor.getIncomingConnections().get(0).getStart();
		}
		else
			a = anchor.getOutgoingConnections().get(0).getEnd();
		
		return a instanceof FixPointAnchor ? (FixPointAnchor)a : null;
	}
	
	private static int calculateIndex(FixPointAnchor anchor, List<FixPointAnchor> all) {
		// TODO: fix this: should probably look at the location of the closest bendboint,
		// not the opposite anchor.
//		if (true) return -1;
		TreeMap<Integer, FixPointAnchor> offsets = new TreeMap<Integer, FixPointAnchor>();
		AnchorSite site = AnchorSite.getSite(anchor);
		for (FixPointAnchor a : all) {
			FixPointAnchor a2 = getOpposite(a);
			if (a2!=null) {
				ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(a2);
				if (site==AnchorSite.TOP || site==AnchorSite.BOTTOM) {
					// collect the x coordinates of opposite anchor
					offsets.put(loc.getX(), a);
				}
				else {
					offsets.put(loc.getY(), a);
				}
			}
		}
		int index = 0;
		for (Entry<Integer, FixPointAnchor> entry : offsets.entrySet()) {
			if (entry.getValue() == anchor)
				return index;
			++index;
		}
		
		return -1;
	}

	private static PictogramLink createPictogramLink(PictogramElement pe) {
		PictogramLink ret = null;
		
		Diagram diagram = Graphiti.getPeService().getDiagramForPictogramElement(pe);
		if (diagram != null) {
			// create new link
			ret = PictogramsFactory.eINSTANCE.createPictogramLink();
			ret.setPictogramElement(pe);

			// add new link to diagram
			diagram.getPictogramLinks().add(ret);
		}
		return ret;
	}

}