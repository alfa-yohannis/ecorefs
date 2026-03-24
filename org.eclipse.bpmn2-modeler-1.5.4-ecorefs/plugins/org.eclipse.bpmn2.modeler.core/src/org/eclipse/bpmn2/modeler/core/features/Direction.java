package org.eclipse.bpmn2.modeler.core.features;

import org.eclipse.bpmn2.modeler.core.utils.AnchorSite;
import org.eclipse.graphiti.mm.algorithms.styles.Point;

/**
 * The connection routing directions.
 */
public enum Direction {
	NONE, UP, DOWN, LEFT, RIGHT;

	/**
	 * Determine the direction of the vector defined by the points [start,end].
	 * If the vector is diagonal, return the direction of the longest horizontal
	 * or vertical component.
	 *  
	 * @param start
	 * @param end
	 * @return
	 */
	public static Direction get(Point start, Point end) {
		return get(start,end,false);
	}
	
	public static Direction get(Point start, Point end, boolean exact) {
		int dx = start.getX() - end.getX();
		int dy = start.getY() - end.getY();
		if (dy == 0) {
			if (dx > 0)
				return LEFT;
			if (dx < 0)
				return RIGHT;
		} else if (dx == 0) {
			if (dy > 0)
				return UP;
			if (dy < 0)
				return DOWN;
		} if (!exact) {
			if (dy > 0) {
				if (dx > 0) {
					if (Math.abs(dx)>Math.abs(dy))
						return LEFT;
					return UP;
				}
				if (dx < 0) {
					if (Math.abs(dx)>Math.abs(dy))
						return RIGHT;
					return UP;
				}
			} else if (dy < 0) {
				if (dx > 0) {
					if (Math.abs(dx)>Math.abs(dy))
						return LEFT;
					return DOWN;
					
				}
				if (dx < 0) {
					if (Math.abs(dx)>Math.abs(dy))
						return RIGHT;
					return DOWN;
				}
			}
		}
		return Direction.NONE;
	}

	/**
	 * Calculate a new direction for the vector defined by the points
	 * [start,end]. If the vector is diagonal, return the old direction
	 * value if possible. That is, if the old direction is the same as one
	 * of the horizontal or vertical components of the vector.
	 * 
	 * @param oldDirection an arbitrary Direction
	 * @param start origin of the vector
	 * @param end endpoint of the vector
	 * @return a new Direction
	 */
	public static Direction get(Direction oldDirection, Point start, Point end) {
		int dx = start.getX() - end.getX();
		int dy = start.getY() - end.getY();
		if (dy == 0) {
			if (dx > 0)
				return LEFT;
			if (dx < 0)
				return RIGHT;
		} else if (dx == 0) {
			if (dy > 0)
				return UP;
			if (dy < 0)
				return DOWN;
		} else if (dy > 0) {
			if (dx > 0) {
				if (oldDirection==LEFT || oldDirection==UP)
					return oldDirection;
				if (Math.abs(dx)>Math.abs(dy))
					return LEFT;
				return UP;
			}
			if (dx < 0) {
				if (oldDirection==RIGHT || oldDirection==UP)
					return oldDirection;
				if (Math.abs(dx)>Math.abs(dy))
					return RIGHT;
				return UP;
			}
		} else if (dy < 0) {
			if (dx > 0) {
				if (oldDirection==LEFT || oldDirection==DOWN)
					return oldDirection;
				if (Math.abs(dx)>Math.abs(dy))
					return LEFT;
				return DOWN;
				
			}
			if (dx < 0) {
				if (oldDirection==RIGHT || oldDirection==DOWN)
					return oldDirection;
				if (Math.abs(dx)>Math.abs(dy))
					return RIGHT;
				return DOWN;
			}
		}
		return Direction.NONE;
	}

	/**
	 * Translate an AnchorSite to a Direction.
	 * 
	 * @param site the AnchorSite
	 * @return the Direction that corresponds to the AnchorSite
	 */
	public static Direction get(AnchorSite site) {
		switch (site) {
		case TOP:
			return UP;
		case BOTTOM:
			return DOWN;
		case LEFT:
			return LEFT;
		case RIGHT:
			return RIGHT;
		}
		return NONE;
	}

	/**
	 * Reverse the given direction by 180 degrees.
	 * 
	 * @param direction the original direction
	 * @return the original direction transposed 180 degrees
	 */
	public static Direction reverse(Direction direction) {
		switch (direction) {
		case DOWN:
			return UP;
		case UP:
			return DOWN;
		case RIGHT:
			return LEFT;
		case LEFT:
			return RIGHT;
		}
		return NONE;
	}
	
	public boolean isHorizontal() {
		return this == LEFT || this == RIGHT;
	}
	
	public boolean isVertical() {
		return this == UP || this == DOWN;
	}
	
	public boolean parallel(Direction that) {
		return
				(this.isHorizontal() && that.isHorizontal()) ||
				(this.isVertical() && that.isVertical());
	}
}