/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.utils.AnchorSite;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.Shape;

/**
 * The Class ConnectionRoute.
 */
public class ConnectionRoute implements Comparable<ConnectionRoute>, Comparator<ConnectionRoute> {

	/**
	 * Records a collision of a line segment with a shape.
	 */
	class Collision {

		/** The shape. */
		Shape shape;
		/** The line segment start point. */
		Point start;
		/** The line segment end point. */
		Point end;

		/**
		 * Instantiates a new collision.
		 *
		 * @param shape the collision shape
		 * @param start the line segment start point
		 * @param end the line segment end point
		 */
		public Collision(Shape shape, Point start, Point end) {
			this.shape = shape;
			this.start = start;
			this.end = end;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			Object o = BusinessObjectUtil.getFirstBaseElement(shape);
			return ModelUtil.getTextValue(o);
		}
	}

	/**
	 * Records the crossing of a line segment with an existing connection.
	 */
	class Crossing {

		/** The connection. */
		Connection connection;
		/** The line segment start point. */
		Point start;
		/** The line segment end point. */
		Point end;

		/**
		 * Instantiates a new crossing.
		 *
		 * @param connection the crossed connection
		 * @param start the line segment start point
		 * @param end the line segment end point
		 */
		public Crossing(Connection connection, Point start, Point end) {
			this.connection = connection;
			this.start = start;
			this.end = end;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			Object o = BusinessObjectUtil.getFirstBaseElement(connection);
			return ModelUtil.getTextValue(o);
		}
	}

	/** The router. */
	private DefaultConnectionRouter router;
	/** The route id. */
	private int id;
	private List<Point> points = new ArrayList<Point>();
	private List<Point> special = new ArrayList<Point>();

	/** The list of shape collisions. */
	private List<Collision> collisions = new ArrayList<Collision>();

	/** The list of connection crossings. */
	private List<Crossing> crossings = new ArrayList<Crossing>();

	/** The source shape of the route being calculated. */
	private Shape source;

	/** The Source Anchor Location for this Route */
	private AnchorSite sourceAnchorSite;
	private Point sourceAnchorLocation;

	/** The target shape of the route being calculated. */
	private Shape target;

	/** The Target Anchor Location for this Route */
	private AnchorSite targetAnchorSite;
	private Point targetAnchorLocation;

	private boolean valid = true;
	private int rank = 0;

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Instantiates a new connection route.
	 *
	 * @param router the router
	 * @param id the id
	 * @param source the source
	 * @param target the target
	 */
	public ConnectionRoute(DefaultConnectionRouter router, int id, Shape source, Shape target) {
		this.router = router;
		this.setId(id);
		this.source = source;
		this.target = target;
	}

	/**
	 * Apply.
	 *
	 * @param ffc the ffc
	 */
	public void apply(FreeFormConnection ffc) {
		apply(ffc, null, null);
	}

	/**
	 * Apply.
	 *
	 * @param ffc the ffc
	 * @param sourceAnchor the source anchor
	 * @param targetAnchor the target anchor
	 */
	public void apply(FreeFormConnection ffc, FixPointAnchor sourceAnchor, FixPointAnchor targetAnchor) {

		// set connection's source and target anchors
		Point p = get(0);
		if (sourceAnchor == null) {
			sourceAnchor = AnchorUtil.createAnchor(source, p);
			ffc.setStart(sourceAnchor);
		} else {
			AnchorUtil.moveAnchor(sourceAnchor, p);
			if (sourceAnchorSite != null) {
				AnchorUtil.moveAnchor(sourceAnchor, sourceAnchorLocation);
				AnchorSite.setSite(sourceAnchor, sourceAnchorSite);
				AnchorUtil.adjustAnchors(source);
			}
		}

		p = get(size() - 1);
		if (targetAnchor == null) {
			// NOTE: a route with only a starting point indicates that it could
			// not be calculated.
			// In this case, make the connection a straight line from source to
			// target.
			targetAnchor = AnchorUtil.createAnchor(target, p);
			ffc.setEnd(targetAnchor);
		} else {
			AnchorUtil.moveAnchor(targetAnchor, p);
			if (targetAnchorSite != null) {
				AnchorUtil.moveAnchor(targetAnchor, targetAnchorLocation);
				AnchorSite.setSite(targetAnchor, targetAnchorSite);
				AnchorUtil.adjustAnchors(target);
			}
		}

		// add the bendpoints
		ffc.getBendpoints().clear();
		for (int i = 1; i < this.size() - 1; ++i) {
			ffc.getBendpoints().add(this.get(i));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String text;
		int size = getPoints().size();
		Point p0 = size == 0 ? null : getPoints().get(0);
		Point p1 = size == 0 ? null : getPoints().get(size - 1);
		String start = p0 == null ? "null" : p0.getX() + "," + p0.getY(); //$NON-NLS-1$ //$NON-NLS-2$
		String end = p1 == null ? "null" : p1.getX() + "," + p1.getY(); //$NON-NLS-1$ //$NON-NLS-2$

		text = String.format("%3d",getId()) + (valid ? " :" : "X:") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				" rank=" + rank + //$NON-NLS-1$
				" length=" + getLength() + //$NON-NLS-1$
				" points=" + getPoints().size() + //$NON-NLS-1$ //$NON-NLS-2$
				" source=" + sourceAnchorSite + " " + start + //$NON-NLS-1$  //$NON-NLS-2$
				" target=" + targetAnchorSite + " " + end; //$NON-NLS-1$ //$NON-NLS-2$
		if (collisions.size() > 0) {
			text += " collisions="; //$NON-NLS-1$
			Iterator<Collision> iter = collisions.iterator();
			while (iter.hasNext()) {
				Collision c = iter.next();
				text += "'" + c.toString() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
				if (iter.hasNext())
					text += ", "; //$NON-NLS-1$
			}
		}
		if (crossings.size() > 0) {
			text += " crossings="; //$NON-NLS-1$
			Iterator<Crossing> iter = crossings.iterator();
			while (iter.hasNext()) {
				Crossing c = iter.next();
				text += "'" + c.toString() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
				if (iter.hasNext())
					text += ", "; //$NON-NLS-1$
			}
		}
		return text;
	}

	public void setSourceAnchor(FixPointAnchor sourceAnchor) {
		this.sourceAnchorSite = AnchorSite.getSite(sourceAnchor);
		this.sourceAnchorLocation = GraphicsUtil.createPoint(sourceAnchor);

	}

	public void setTargetAnchor(FixPointAnchor targetAnchor) {
		this.targetAnchorSite = AnchorSite.getSite(targetAnchor);
		this.targetAnchorLocation = GraphicsUtil.createPoint(targetAnchor);
	}

	public AnchorSite getSourceAnchorSite() {
		return sourceAnchorSite;
	}

	public AnchorSite getTargetAnchorSite() {
		return targetAnchorSite;
	}

	/**
	 * Adds a new point to the route. If the point is already in the route, the
	 * route is marked as invalid and the new point is not added.
	 *
	 * @param newPoint the new point
	 * @return true, if successful
	 */
	public boolean add(Point newPoint) {
		for (Point p : getPoints()) {
			if (GraphicsUtil.pointsEqual(newPoint, p)) {
				setValid(false);
				return false;
			}
		}
		getPoints().add(GraphicsUtil.createPoint(newPoint));
		return true;
	}
	
	public boolean add(int index, Point newPoint) {
		for (Point p : getPoints()) {
			if (GraphicsUtil.pointsEqual(newPoint, p)) {
				setValid(false);
				return false;
			}
		}
		getPoints().add(index, GraphicsUtil.createPoint(newPoint));
		return true;
	}

	public boolean addAll(List<Point> list) {
		for (Point p : list) {
			add(p);
		}
		return isValid();
	}

	public boolean contains(Point newPoint) {
		for (Point p : getPoints()) {
			if (GraphicsUtil.pointsEqual(newPoint, p)) {
				return true;
			}
		}
		return false;
	}

	public void addSpecial(Point p) {
		if (p != null)
			special.add(p);
	}

	public boolean isSpecial(Point p) {
		return special.contains(p);
	}

	/**
	 * Gets the point at the given index.
	 *
	 * @param index the index
	 * @return the point
	 */
	public Point get(int index) {
		return getPoints().get(index);
	}

	/**
	 * Returns the number of points in the route.
	 *
	 * @return the route size
	 */
	public int size() {
		return getPoints().size();
	}

	/**
	 * Adds collision information to the route.
	 *
	 * @param shape the shape
	 * @param start the start
	 * @param end the end
	 */
	public void addCollision(Shape shape, Point start, Point end) {
		if (shape != null) {
			for (Collision c : collisions) {
				if (c.shape == shape)
					return;
			}
			collisions.add(new Collision(shape, start, end));
		}
	}

	/**
	 * Returns a list of shapes that are intersected by the current route.
	 * 
	 * @return a list of shape collisions.
	 */
	public List<Collision> getCollisions() {
		return collisions;
	}

	/**
	 * Adds line crossing information to the route.
	 *
	 * @param connection the connection being intersected
	 * @param start the start
	 * @param end the end
	 */
	public void addCrossing(Connection connection, Point start, Point end) {
		crossings.add(new Crossing(connection, start, end));
	}

	/**
	 * Returns a list of connection lines that the current route intersects.
	 * 
	 * @return a list of connections
	 */
	public List<Crossing> getCrossings() {
		return crossings;
	}

	/**
	 * Sets a flag indicating if the route is valid.
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Checks if is valid.
	 *
	 * @return true, if is valid
	 */
	public boolean isValid() {
		if (valid)
			return getLength() < Integer.MAX_VALUE;
		return false;
	}

	/**
	 * Gets the length of the route calculated as the sum of the lengths of all
	 * line segments formed by the routing points.
	 *
	 * @return the length
	 */
	public int getLength() {
		int length = 0;
		if (getPoints().size() > 1) {
			Point p1 = getPoints().get(0);
			for (int i = 1; i < getPoints().size(); ++i) {
				Point p2 = getPoints().get(i);
				// if (isHorizontal(p1,p2) || isVertical(p1,p2))
				length += (int) GraphicsUtil.getLength(p1, p2);
				// else
				// return Integer.MAX_VALUE;
				p1 = p2;
			}
		} else {
			// this route could not be calculated
			return Integer.MAX_VALUE;
		}
		return length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ConnectionRoute arg0) {
		return compare(this, arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(ConnectionRoute o1, ConnectionRoute o2) {
		int i = 0;
		int v1 = o1.isValid() ? 1 : 0;
		int v2 = o2.isValid() ? 1 : 0;
		i = v2 - v1;
		if (i == 0) {
			i = o1.collisions.size() - o2.collisions.size();
			if (i == 0) {
				i = o1.crossings.size() - o2.crossings.size();
				if (i == 0) {
					i = o1.getRank() - o2.getRank();
					if (i == 0) {
						i = o1.getPoints().size() - o2.getPoints().size();
						if (i == 0) {
							i = o1.getLength() - o2.getLength();
							if (i == 0) {
								i = o1.getPoints().size() - o2.getPoints().size();
							}
						}
					}
				}
				// else {
				// // pick the shorter route
				// float dl = Math.abs(o1.getLength() - o2.getLength());
				// float sl = (o1.getLength() + o2.getLength()) / 2;
				// dl = dl/sl;
				// if ( dl > 0.5)
				// return o1.getLength() - o2.getLength();
				// }
			}
		}
//		else if (o1.isValid() != o2.isValid()) {
//			// pick the shorter route
//			float dl = Math.abs(o1.getLength() - o2.getLength());
//			float sl = (o1.getLength() + o2.getLength()) / 2;
//			dl = dl / sl;
//			if (dl > 0.5)
//				return o1.getLength() - o2.getLength();
//		}
		return i;
	}

	private boolean removeUnusedPoints() {
		boolean changed = false;

		Point p1 = getPoints().get(0);
		for (int i = 1; i < getPoints().size() - 1; ++i) {
			Point p2 = getPoints().get(i);
			if (!isSpecial(p2) && i + 1 < getPoints().size()) {
				boolean remove = false;
				if (GraphicsUtil.pointsEqual(p1, p2)) {
					remove = true;
				} else {
					// remove unnecessary bendpoints: two consecutive
					// horizontal or vertical line segments
					Point p3 = getPoints().get(i + 1);
					int x1 = p1.getX();
					int x2 = p2.getX();
					int x3 = p3.getX();
					int y1 = p1.getY();
					int y2 = p2.getY();
					int y3 = p3.getY();
					if (	
							(
								GraphicsUtil.isVertical(p1, p2) &&
								GraphicsUtil.isVertical(p2, p3) && (
									(y1 < y2 && y2 < y3) ||
									(y1 > y2 && y2 > y3)
								)
								)
							|| (
								GraphicsUtil.isHorizontal(p1, p2) &&
								GraphicsUtil.isHorizontal(p2, p3) && (
									(x1 < x2 && x2 < x3) ||
									(x1 > x2 && x2 > x3)
								)
							)
						) {
						if (router.getCollision(p1, p3) == null)
							remove = true;
					}
				}
				if (remove) {
					getPoints().remove(i);
					// look at these set of points again
					--i;
					changed = true;
				}
			}
			p1 = p2;
		}
		return changed;
	}

	private boolean removeUnusedSegments() {
		boolean changed = false;

		// remove unnecessary "U" shapes
		Point p1 = getPoints().get(1);
		for (int i=2; i<getPoints().size()-2; ++i) {
			Point p2 = getPoints().get(i);
			if (!isSpecial(p2) && i+2 < getPoints().size()) {
				Point p3 = getPoints().get(i+1);
				if (!isSpecial(p3)) {
					Point p4 = getPoints().get(i+2);
					if (GraphicsUtil.isHorizontal(p1,p2) && GraphicsUtil.isVertical(p2,p3) && GraphicsUtil.isHorizontal(p3,p4)) {
						if (Direction.get(p1,p2) != Direction.get(p3,p4)) {
							// this forms a horizontal "U" shape
							Point p = GraphicsUtil.createPoint(p1.getX(), p3.getY());
							if (router.getCollision(p1,p)==null) {
								getPoints().set(i+1, p);
								getPoints().remove(p2);
								getPoints().remove(p3);
								--i;
								changed = true;
							}
						}
						else {
							// this forms an "S" shape
							Point p = GraphicsUtil.createPoint(p1.getX(), p3.getY());
							if (router.getCollision(p1,p)==null && router.getCollision(p, p3)==null) {
								getPoints().set(i, p);
								--i;
								changed = true;
							}							
						}
					}
					else if (GraphicsUtil.isVertical(p1,p2) && GraphicsUtil.isHorizontal(p2,p3) && GraphicsUtil.isVertical(p3,p4)) {
						if (Direction.get(p1,p2) != Direction.get(p3,p4)) {
							// this forms a horizontal "U" shape
							Point p = GraphicsUtil.createPoint(p3.getX(), p1.getY());
							if (router.getCollision(p1,p)==null) {
								getPoints().set(i+1, p);
								getPoints().remove(p2);
								getPoints().remove(p3);
								--i;
								changed = true;
							}
						}
						else {
							// this forms an "S" shape
							Point p = GraphicsUtil.createPoint(p3.getX(), p1.getY());
							if (router.getCollision(p1,p)==null && router.getCollision(p, p3)==null) {
								getPoints().set(i, p);
								--i;
								changed = true;
							}							
						}
					}
				}
			}
			p1 = p2;
		}

		/*
		 * Remove "T" shapes. These may be created by "overshoot" of the
		 * departure and approach route segments calculated by the Manhattan
		 * router.
		 */
		p1 = getPoints().get(0);
		for (int i = 1; i < getPoints().size() - 1; ++i) {
			Point p2 = getPoints().get(i);
			if (i + 1 < getPoints().size()) {
				Point p3 = getPoints().get(i + 1);
				if (p1.getX() == p2.getX() && p2.getX() == p3.getX()) {
					if ((p2.getY() < p1.getY() && p2.getY() < p3.getY())
							|| (p2.getY() > p1.getY() && p2.getY() > p3.getY())) {
						getPoints().remove(p2);
						--i;
						changed = true;
					}
				} else if (p1.getY() == p2.getY() && p2.getY() == p3.getY()) {
					if ((p2.getX() < p1.getX() && p2.getX() < p3.getX())
							|| (p2.getX() > p1.getX() && p2.getX() > p3.getX())) {
						getPoints().remove(p2);
						--i;
						changed = true;
					}
				}
			}
			p1 = p2;
		}
		return changed;
	}

	/**
	 * Optimize the routing points by removing unnecessary and overlapping line
	 * segments in the route.
	 *
	 * @return true, if successful
	 */
	public boolean optimize() {
		boolean changed = false;
		changed = removeUnusedPoints();
		if (removeUnusedSegments())
		{
			// this may cause some unused points to be left over
			removeUnusedPoints();
			changed = true;
		}
		return changed;
	}

	/**
	 * Gets the ranking of this route, used during comparison to other routes.
	 * This is an indication of the "quality" or desirability of the route when
	 * compared to other routes; the higher the rank, the less desirable is the
	 * route.
	 *
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * Sets the rank.
	 *
	 * @param rank the new rank
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}

	/**
	 * Gets the points.
	 *
	 * @return the points
	 */
	public List<Point> getPoints() {
		return points;
	}

	/**
	 * Sets the points.
	 *
	 * @param points the new points
	 */
	public void setPoints(List<Point> points) {
		this.points = points;
	}
}
