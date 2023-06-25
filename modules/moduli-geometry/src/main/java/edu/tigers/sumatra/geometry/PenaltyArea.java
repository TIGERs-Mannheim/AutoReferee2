/*
 * Copyright (c) 2009 - 2022, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.geometry;

import edu.tigers.sumatra.drawable.DrawableRectangle;
import edu.tigers.sumatra.drawable.IDrawableShape;
import edu.tigers.sumatra.math.IBoundedPath;
import edu.tigers.sumatra.math.SumatraMath;
import edu.tigers.sumatra.math.line.ILineSegment;
import edu.tigers.sumatra.math.line.Lines;
import edu.tigers.sumatra.math.rectangle.IRectangle;
import edu.tigers.sumatra.math.rectangle.Rectangle;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


/**
 * Class representing a rectangular penalty area
 */
public class PenaltyArea implements IPenaltyArea
{
	private final double length;
	private final double depth;

	private final Rectangle rectangle;
	private final IVector2 goalCenter;


	/**
	 * Creates a PenaltyArea
	 *
	 * @param goalCenter
	 * @param length
	 * @param depth
	 */
	public PenaltyArea(final IVector2 goalCenter, final double depth, final double length)
	{
		this.length = length;
		this.depth = depth;
		this.goalCenter = goalCenter;

		double centerOffset = Math.signum(goalCenter.x()) * depth / -2.;
		IVector2 center = Vector2.fromX(goalCenter.x() + centerOffset);
		rectangle = Rectangle.fromCenter(center, depth, length);
	}


	@Override
	public IPenaltyArea withMargin(final double margin)
	{
		double newDepth = Math.max(0, this.depth + margin);
		double newLength = Math.max(0, this.length + margin * 2);
		return new PenaltyArea(getGoalCenter(), newDepth, newLength);
	}


	private List<ILineSegment> getEdges()
	{
		List<ILineSegment> edges = new ArrayList<>();
		double lowerX = getGoalCenter().x();
		double upperX = lowerX - Math.signum(getGoalCenter().x()) * getDepth();
		double negY = -getLength() / 2;
		double posY = getLength() / 2;
		IVector2 p1 = Vector2.fromXY(lowerX, negY);
		IVector2 p2 = Vector2.fromXY(upperX, negY);
		IVector2 p3 = Vector2.fromXY(upperX, posY);
		IVector2 p4 = Vector2.fromXY(lowerX, posY);
		edges.add(Lines.segmentFromPoints(p1, p2));
		edges.add(Lines.segmentFromPoints(p2, p3));
		edges.add(Lines.segmentFromPoints(p3, p4));
		return edges;
	}


	@Override
	public List<IBoundedPath> getPerimeterPath()
	{
		return getEdges().stream().map(IBoundedPath.class::cast).toList();
	}


	@Override
	public double getPerimeterLength()
	{
		return length + 2 * depth;
	}


	@Override
	@SuppressWarnings("squid:S1244") // equality check intended
	public IVector2 projectPointOnToPenaltyAreaBorder(final IVector2 point)
	{
		if (point.x() * Math.signum(getGoalCenter().x()) >= Math.abs(getGoalCenter().x()))
		{
			if (point.y() == 0.0)
			{
				return getGoalCenter().addNew(Vector2.fromX(getDepth()));
			}
			return getGoalCenter().addNew(Vector2.fromY(Math.signum(point.y()) * getLength() / 2));
		}
		return point.nearestToOpt(intersectPerimeterPath(Lines.lineFromPoints(point, getGoalCenter())))
				.orElseGet(() -> getGoalCenter().addNew(Vector2.fromX(getDepth())));
	}


	@Override
	public boolean isPointInShape(final IVector2 point)
	{
		return getRectangle().isPointInShape(point);
	}


	@Override
	public boolean isPointInShapeOrBehind(final IVector2 point)
	{
		return isBehindPenaltyArea(point) || isPointInShape(point);
	}


	@Override
	public IVector2 nearestPointInside(final IVector2 point)
	{
		return getRectangle().nearestPointInside(point);
	}


	@Override
	public IVector2 nearestPointOutside(final IVector2 point)
	{
		if (isPointInShapeOrBehind(point))
		{
			return point.nearestTo(
					getEdges().stream()
							.map(e -> e.closestPointOnPath(point))
							.toList());
		}
		return point;
	}


	@Override
	public boolean isBehindPenaltyArea(final IVector2 point)
	{
		return (Math.abs(point.x()) > Math.abs(getGoalCenter().x()))
				&& ((int) Math.signum(point.x()) == (int) Math.signum(getGoalCenter().x()))
				&& Math.abs(point.y()) < getLength() / 2;
	}


	@Override
	public IVector2 getGoalCenter()
	{
		return goalCenter;
	}


	@Override
	public Rectangle getRectangle()
	{
		return rectangle;
	}


	@Override
	public IVector2 getNegCorner()
	{
		var corner = goalCenter.x() > 0 ? IRectangle.ECorner.BOTTOM_LEFT : IRectangle.ECorner.BOTTOM_RIGHT;
		return getRectangle().getCorner(corner);
	}


	@Override
	public IVector2 getPosCorner()
	{
		var corner = goalCenter.x() > 0 ? IRectangle.ECorner.TOP_LEFT : IRectangle.ECorner.TOP_RIGHT;
		return getRectangle().getCorner(corner);
	}


	private double getLength()
	{
		return getRectangle().yExtent();
	}


	private double getDepth()
	{
		return getRectangle().xExtent();
	}


	@Override
	public List<IDrawableShape> getDrawableShapes()
	{
		List<IDrawableShape> shapes = new ArrayList<>(1);
		shapes.add(new DrawableRectangle(getRectangle(), Color.white));
		return shapes;
	}


	@Override
	public double distanceTo(final IVector2 point)
	{
		return nearestPointInside(point).distanceTo(point);
	}


	@Override
	public double distanceToNearestPointOutside(final IVector2 pos)
	{
		return nearestPointOutside(pos).distanceTo(pos);
	}


	@Override
	public double intersectionArea(final IVector2 from, final IVector2 to)
	{
		if (isBehindPenaltyArea(from) || isBehindPenaltyArea(to))
		{
			return depth * length * 1e-6;
		}

		ILineSegment line = Lines.segmentFromPoints(
				rectangle.nearestPointOutside(from), rectangle.nearestPointOutside(to));
		List<IVector2> intersections = rectangle.intersectPerimeterPath(line);
		if (intersections.size() != 2)
		{
			return 0;
		}
		IVector2 p1 = intersections.get(0);
		IVector2 p2 = intersections.get(1);

		if (SumatraMath.isEqual(p1.x(), goalCenter.x()) || SumatraMath.isEqual(p2.x(), goalCenter.x()))
		{
			return depth * length * 1e-6;
		}

		double frontX = goalCenter.x() - Math.signum(goalCenter.x()) * depth;
		double dp1 = Math.abs(frontX - p1.x());
		double dp2 = Math.abs(frontX - p2.x());
		double dMin = Math.min(dp1, dp2);
		double a = Math.max(dp1, dp2) - dMin;
		double l = Math.abs(p1.y() - p2.y());
		return (l * dMin + l * a * 0.5) * 1e-6;
	}
}
