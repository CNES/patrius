/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Euclidean1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Interval;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.IntervalsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.OrientedPoint;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.AbstractSubHyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.BSPTree;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region.Location;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Side;
import fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class represents a sub-hyperplane for {@link Line}.
 * 
 * @version $Id: SubLine.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class SubLine extends AbstractSubHyperplane<Euclidean2D, Euclidean1D> {

    /** Threshold. */
    private static final double THRESHOLD = 1.0e-10;

    /**
     * Simple constructor.
     * 
     * @param hyperplane
     *        underlying hyperplane
     * @param remainingRegion
     *        remaining region of the hyperplane
     */
    public SubLine(final Hyperplane<Euclidean2D> hyperplane,
        final Region<Euclidean1D> remainingRegion) {
        super(hyperplane, remainingRegion);
    }

    /**
     * Create a sub-line from two endpoints.
     * 
     * @param start
     *        start point
     * @param end
     *        end point
     */
    public SubLine(final Vector2D start, final Vector2D end) {
        super(new Line(start, end), buildIntervalSet(start, end));
    }

    /**
     * Create a sub-line from a segment.
     * 
     * @param segment
     *        single segment forming the sub-line
     */
    public SubLine(final Segment segment) {
        super(segment.getLine(), buildIntervalSet(segment.getStart(), segment.getEnd()));
    }

    /**
     * Get the endpoints of the sub-line.
     * <p>
     * A subline may be any arbitrary number of disjoints segments, so the endpoints are provided as a list of endpoint
     * pairs. Each element of the list represents one segment, and each segment contains a start point at index 0 and an
     * end point at index 1. If the sub-line is unbounded in the negative infinity direction, the start point of the
     * first segment will have infinite coordinates. If the sub-line is unbounded in the positive infinity direction,
     * the end point of the last segment will have infinite coordinates. So a sub-line covering the whole line will
     * contain just one row and both elements of this row will have infinite coordinates. If the sub-line is empty, the
     * returned list will contain 0 segments.
     * </p>
     * 
     * @return list of segments endpoints
     */
    public List<Segment> getSegments() {

        final Line line = (Line) this.getHyperplane();
        final List<Interval> list = ((IntervalsSet) this.getRemainingRegion()).asList();
        final List<Segment> segments = new ArrayList<Segment>();

        for (final Interval interval : list) {
            final Vector2D start = line.toSpace(new Vector1D(interval.getInf()));
            final Vector2D end = line.toSpace(new Vector1D(interval.getSup()));
            segments.add(new Segment(start, end, line));
        }

        return segments;

    }

    /**
     * Get the intersection of the instance and another sub-line.
     * <p>
     * This method is related to the {@link Line#intersection(Line)
     * intersection} method in the {@link Line Line} class, but in addition to compute the point along infinite lines,
     * it also checks the point lies on both sub-line ranges.
     * </p>
     * 
     * @param subLine
     *        other sub-line which may intersect instance
     * @param includeEndPoints
     *        if true, endpoints are considered to belong to
     *        instance (i.e. they are closed sets) and may be returned, otherwise endpoints
     *        are considered to not belong to instance (i.e. they are open sets) and intersection
     *        occurring on endpoints lead to null being returned
     * @return the intersection point if there is one, null if the sub-lines don't intersect
     */
    public Vector2D intersection(final SubLine subLine, final boolean includeEndPoints) {

        // retrieve the underlying lines
        final Line line1 = (Line) this.getHyperplane();
        final Line line2 = (Line) subLine.getHyperplane();

        // compute the intersection on infinite line
        final Vector2D v2D = line1.intersection(line2);

        // check location of point with respect to first sub-line
        final Location loc1 = this.getRemainingRegion().checkPoint(line1.toSubSpace(v2D));

        // check location of point with respect to second sub-line
        final Location loc2 = subLine.getRemainingRegion().checkPoint(line2.toSubSpace(v2D));

        if (includeEndPoints) {
            return ((loc1 != Location.OUTSIDE) && (loc2 != Location.OUTSIDE)) ? v2D : null;
        } else {
            return ((loc1 == Location.INSIDE) && (loc2 == Location.INSIDE)) ? v2D : null;
        }

    }

    /**
     * Build an interval set from two points.
     * 
     * @param start
     *        start point
     * @param end
     *        end point
     * @return an interval set
     */
    private static IntervalsSet buildIntervalSet(final Vector2D start, final Vector2D end) {
        final Line line = new Line(start, end);
        return new IntervalsSet(line.toSubSpace(start).getX(),
            line.toSubSpace(end).getX());
    }

    /** {@inheritDoc} */
    @Override
    // CHECKSTYLE: stop IllegalType check
    // Reason: Commons-Math code kept as such
            protected
            AbstractSubHyperplane<Euclidean2D, Euclidean1D> buildNew(final Hyperplane<Euclidean2D> hyperplane,
                                                                     final Region<Euclidean1D> remainingRegion) {
        // CHECKSTYLE: resume IllegalType check
        return new SubLine(hyperplane, remainingRegion);
    }

    /** {@inheritDoc} */
    @Override
    public Side side(final Hyperplane<Euclidean2D> hyperplane) {

        final Line thisLine = (Line) this.getHyperplane();
        final Line otherLine = (Line) hyperplane;
        final Vector2D crossing = thisLine.intersection(otherLine);

        if (crossing == null) {
            // the lines are parallel,
            final double global = otherLine.getOffset(thisLine);
            return (global < -THRESHOLD) ? Side.MINUS : ((global > THRESHOLD) ? Side.PLUS : Side.HYPER);
        }

        // the lines do intersect
        final boolean direct = MathLib.sin(thisLine.getAngle() - otherLine.getAngle()) < 0;
        final Vector1D x = thisLine.toSubSpace(crossing);
        return this.getRemainingRegion().side(new OrientedPoint(x, direct));

    }

    /** {@inheritDoc} */
    @Override
    public SplitSubHyperplane<Euclidean2D> split(final Hyperplane<Euclidean2D> hyperplane) {

        final Line thisLine = (Line) this.getHyperplane();
        final Line otherLine = (Line) hyperplane;
        final Vector2D crossing = thisLine.intersection(otherLine);

        if (crossing == null) {
            // the lines are parallel
            final double global = otherLine.getOffset(thisLine);
            return (global < -THRESHOLD) ?
                new SplitSubHyperplane<Euclidean2D>(null, this) :
                new SplitSubHyperplane<Euclidean2D>(this, null);
        }

        // the lines do intersect
        final boolean direct = MathLib.sin(thisLine.getAngle() - otherLine.getAngle()) < 0;
        final Vector1D x = thisLine.toSubSpace(crossing);
        final SubHyperplane<Euclidean1D> subPlus = new OrientedPoint(x, !direct).wholeHyperplane();
        final SubHyperplane<Euclidean1D> subMinus = new OrientedPoint(x, direct).wholeHyperplane();

        final BSPTree<Euclidean1D> splitTree = this.getRemainingRegion().getTree(false).split(subMinus);
        final BSPTree<Euclidean1D> plusTree = this.getRemainingRegion().isEmpty(splitTree.getPlus()) ?
            new BSPTree<Euclidean1D>(Boolean.FALSE) :
            new BSPTree<Euclidean1D>(subPlus, new BSPTree<Euclidean1D>(Boolean.FALSE),
                splitTree.getPlus(), null);
        final BSPTree<Euclidean1D> minusTree = this.getRemainingRegion().isEmpty(splitTree.getMinus()) ?
            new BSPTree<Euclidean1D>(Boolean.FALSE) :
            new BSPTree<Euclidean1D>(subMinus, new BSPTree<Euclidean1D>(Boolean.FALSE),
                splitTree.getMinus(), null);

        // Return result
        //
        return new SplitSubHyperplane<Euclidean2D>(new SubLine(thisLine.copySelf(), new IntervalsSet(plusTree)),
            new SubLine(thisLine.copySelf(), new IntervalsSet(minusTree)));

    }

}
