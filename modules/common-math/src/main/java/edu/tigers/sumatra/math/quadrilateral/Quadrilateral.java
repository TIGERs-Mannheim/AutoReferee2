/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.math.quadrilateral;

import com.sleepycat.persist.model.Persistent;
import edu.tigers.sumatra.math.line.ILine;
import edu.tigers.sumatra.math.line.Line;
import edu.tigers.sumatra.math.line.v2.ILineSegment;
import edu.tigers.sumatra.math.line.v2.Lines;
import edu.tigers.sumatra.math.triangle.ITriangle;
import edu.tigers.sumatra.math.triangle.Triangle;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.VectorAngleComparator;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Implementation of a quadrilateral ("Viereck") defined by 4 corners and backed by 2 triangles.
 */
@Persistent
@EqualsAndHashCode
@ToString
public final class Quadrilateral implements IQuadrilateral
{
	@NonNull
	private final List<IVector2> corners;


	@SuppressWarnings("unused")
	private Quadrilateral()
	{
		this.corners = new ArrayList<>();
	}


	private Quadrilateral(final List<IVector2> corners)
	{
		// make sure we use an array list, which is persistable
		this.corners = new ArrayList<>(corners);
	}


	/**
	 * Create a quadrilateral by its 4 corners
	 *
	 * @param points 4 points that build the the quadrilateral
	 * @return a new quadrilateral
	 */
	public static IQuadrilateral fromCorners(final List<IVector2> points)
	{
		Validate.isTrue(points.size() == 4, "Exactly 4 corners required!");
		IVector2 center = points.stream()
				.reduce(IVector2::addNew)
				.orElseThrow(IllegalStateException::new)
				.multiplyNew(1.0 / 4.0);
		List<IVector2> sortedCorners = points.stream()
				.map(v -> v.subtractNew(center))
				.sorted(new VectorAngleComparator())
				.map(v -> v.addNew(center))
				.collect(Collectors.toList());
		return new Quadrilateral(sortedCorners);
	}


	/**
	 * Create a quadrilateral by its 4 corners
	 *
	 * @param a corner a
	 * @param b corner b
	 * @param c corner c
	 * @param d corner d
	 * @return a new quadrilateral
	 */
	public static IQuadrilateral fromCorners(@NonNull final IVector2 a, @NonNull final IVector2 b,
			@NonNull final IVector2 c, @NonNull final IVector2 d)
	{
		List<IVector2> corners = new ArrayList<>(4);
		corners.add(a);
		corners.add(b);
		corners.add(c);
		corners.add(d);
		return fromCorners(corners);
	}


	/**
	 * Create a isosceles trapezoid (gleichschenkliges Trapez)
	 *
	 * @param p1 first point (center of the lower edge)
	 * @param w1 width of the lower edge
	 * @param p2 second point (center of the upper edge)
	 * @param w2 width of the upper edge
	 * @return a new quadrilateral
	 */
	public static IQuadrilateral isoscelesTrapezoid(@NonNull IVector2 p1, double w1, @NonNull IVector2 p2, double w2)
	{
		IVector2 normal = p2.subtractNew(p1).getNormalVector().normalize();
		return fromCorners(
				p1.addNew(normal.multiplyNew(w1 / 2)),
				p1.addNew(normal.multiplyNew(-w1 / 2)),
				p2.addNew(normal.multiplyNew(w2 / 2)),
				p2.addNew(normal.multiplyNew(-w2 / 2))
		);
	}


	@Override
	public List<IVector2> getCorners()
	{
		return Collections.unmodifiableList(corners);
	}


	@Override
	public boolean isPointInShape(final IVector2 point)
	{
		return getTriangles().stream().anyMatch(t -> t.isPointInShape(point));
	}


	@Override
	public boolean isPointInShape(final IVector2 point, final double margin)
	{
		return getTriangles().stream().anyMatch(t -> t.isPointInShape(point, margin));
	}


	@Override
	public List<ITriangle> getTriangles()
	{
		List<ITriangle> triangles = new ArrayList<>(2);
		triangles.add(Triangle.fromCorners(corners.get(0), corners.get(1), corners.get(2)));
		triangles.add(Triangle.fromCorners(corners.get(3), corners.get(0), corners.get(2)));
		return triangles;
	}


	@Override
	public List<ILine> getEdges()
	{
		List<ILine> edges = new ArrayList<>(4);
		for (int i = 0; i < 4; i++)
		{
			edges.add(Line.fromPoints(corners.get(i), corners.get((i + 1) % 4)));
		}
		return edges;
	}


	@Override
	public List<IVector2> lineIntersections(final ILine line)
	{
		IVector2 a = corners.get(0);
		IVector2 b = corners.get(1);
		IVector2 c = corners.get(2);
		IVector2 d = corners.get(3);
		ILineSegment ab = Lines.segmentFromPoints(a, b);
		ILineSegment bc = Lines.segmentFromPoints(b, c);
		ILineSegment cd = Lines.segmentFromPoints(c, d);
		ILineSegment da = Lines.segmentFromPoints(d, a);
		ILineSegment[] lines = new ILineSegment[] { ab, bc, cd, da };
		List<IVector2> intersections = new ArrayList<>();
		for (ILineSegment segment : lines)
		{
			Optional<IVector2> intersection = segment.intersectLine(Lines.lineFromLegacyLine(line));
			intersection.ifPresent(intersections::add);
		}
		return intersections;
	}
}
