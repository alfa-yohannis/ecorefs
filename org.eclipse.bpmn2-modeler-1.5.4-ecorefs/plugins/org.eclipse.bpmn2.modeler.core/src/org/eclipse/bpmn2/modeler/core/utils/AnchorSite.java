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

package org.eclipse.bpmn2.modeler.core.utils;

import org.eclipse.bpmn2.modeler.core.features.GraphitiConstants;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil.LineSegment;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;

public enum AnchorSite {
	/** These ordinals MUST have the same meanings as {@see GraphicsUtil#getEdges(Shape)} */
	TOP("anchor.top"), BOTTOM("anchor.bottom"), LEFT("anchor.left"), RIGHT("anchor.right"), CENTER("anchor.center"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private final String key;

	private AnchorSite(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public static AnchorSite getSite(String key) {
		for (AnchorSite l : values()) {
			if (l.getKey().equals(key)) {
				return l;
			}
		}
		
		return CENTER;
//		throw new IllegalArgumentException("Cannot determine Anchor Site "+key);
	}
	
	public static AnchorSite getSite(FixPointAnchor anchor) {
		return getSite(FeatureSupport.getPropertyValue(anchor, GraphitiConstants.ANCHOR_LOCATION));
	}
	
	public static void setSite(FixPointAnchor anchor, AnchorSite site) {
		FeatureSupport.setPropertyValue(anchor, GraphitiConstants.ANCHOR_LOCATION, site.getKey());
	}
	
	/**
	 * Return the AnchorSite value of the shape's edge that is nearest
	 * to the given point.
	 * 
	 * @param shape
	 * @param p
	 * @return
	 */
	public static AnchorSite getNearestEdge(Shape shape, Point p) {
		LineSegment edge = GraphicsUtil.findNearestEdge(shape, p);
		ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(shape);
		AnchorSite site;
		if (edge.isHorizontal()) {
			int y = edge.getStart().getY();
			if (y==loc.getY())
				site = TOP;
			else
				site = BOTTOM;
		}
		else {
			int x = edge.getStart().getX();
			if (x==loc.getX())
				site = LEFT;
			else
				site = RIGHT;
		}
		return site;
	}
	
	/**
	 * Return the AnchorSite value of the shape's edge that is nearest
	 * to the given points.
	 * 
	 * @param shape
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static AnchorSite getNearestEdge(Shape shape, Point p1, Point p2) {
		ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(shape);
		if (shape instanceof ConnectionDecorator) {
			Point pm = GraphicsUtil.getMidpoint(p1, p2);
			Connection c = ((ConnectionDecorator)shape).getConnection();
			LineSegment lineSegment = null;
			Point start = GraphicsUtil.createPoint(c.getStart());
			Point end = GraphicsUtil.createPoint(c.getEnd());
			if (c instanceof FreeFormConnection) {
				double minDistance = Double.MAX_VALUE;
				double d;
				LineSegment ls;
				FreeFormConnection ffc = (FreeFormConnection) c;
				Point p0 = start;
				for (Point p : ffc.getBendpoints()) {
					ls = new LineSegment(p0,p);
					d = ls.getDistance(pm);
					if (d<minDistance) {
						minDistance = d;
						lineSegment = ls;
					}
					p0 = p;
				}
				ls = new LineSegment(p0,end);
				d = ls.getDistance(pm);
				if (d<minDistance) {
					minDistance = d;
					lineSegment = ls;
				}
			}
			if (lineSegment==null) {
				lineSegment = new LineSegment(start,end);
			}
			if (lineSegment.isVertical()) {
				if (pm.getX() > lineSegment.getMiddle().getX()) {
					return RIGHT;
				}
				else {
					return LEFT;
				}
			}
			else if (lineSegment.isHorizontal()) {
				if (pm.getY() > lineSegment.getMiddle().getY()) {
					return BOTTOM;
				}
				else {
					return TOP;
				}
			}
			int dx =  Math.abs(pm.getX() - lineSegment.getMiddle().getX());
			int dy =  Math.abs(pm.getY() - lineSegment.getMiddle().getY());
			if (dx<dy) {
				if (pm.getY() > lineSegment.getMiddle().getY()) {
					return BOTTOM;
				}
				else {
					return TOP;
				}
			}
			else {
				if (pm.getX() > lineSegment.getMiddle().getX()) {
					return RIGHT;
				}
				else {
					return LEFT;
				}
			}
		}
		LineSegment edge = GraphicsUtil.findNearestEdge(shape, p1, p2);
		AnchorSite site;
		if (edge.isHorizontal()) {
			int y = edge.getStart().getY();
			if (y==loc.getY())
				site = TOP;
			else
				site = BOTTOM;
		}
		else {
			int x = edge.getStart().getX();
			if (x==loc.getX())
				site = LEFT;
			else
				site = RIGHT;
		}
		return site;
	}
}