/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Interval;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.IntervalsSet;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Region.Location;

/**
 * This class represents a subset of a {@link Line}.
 * 
 * @version $Id: SubLine.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class SubLine {

    /** Underlying line. */
    private final Line line;

    /** Remaining region of the hyperplane. */
    private final IntervalsSet remainingRegion;

    /**
     * Simple constructor.
     * 
     * @param lineIn
     *        underlying line
     * @param remainingRegionIn
     *        remaining region of the line
     */
    public SubLine(final Line lineIn, final IntervalsSet remainingRegionIn) {
        this.line = lineIn;
        this.remainingRegion = remainingRegionIn;
    }

    /**
     * Create a sub-line from two endpoints.
     * 
     * @param start
     *        start point
     * @param end
     *        end point
     * @exception MathIllegalArgumentException
     *            if the points are equal
     */
    public SubLine(final Vector3D start, final Vector3D end) {
        this(new Line(start, end), buildIntervalSet(start, end));
    }

    /**
     * Create a sub-line from a segment.
     * 
     * @param segment
     *        single segment forming the sub-line
     * @exception MathIllegalArgumentException
     *            if the segment endpoints are equal
     */
    public SubLine(final Segment segment) {
        this(segment.getLine(), buildIntervalSet(segment.getStart(), segment.getEnd()));
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

        final List<Interval> list = this.remainingRegion.asList();
        final List<Segment> segments = new ArrayList<Segment>();

        for (final Interval interval : list) {
            final Vector3D start = this.line.toSpace(new Vector1D(interval.getInf()));
            final Vector3D end = this.line.toSpace(new Vector1D(interval.getSup()));
            segments.add(new Segment(start, end, this.line));
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
    public Vector3D intersection(final SubLine subLine, final boolean includeEndPoints) {

        // compute the intersection on infinite line
        final Vector3D v1D = this.line.intersection(subLine.line);

        // check location of point with respect to first sub-line
        final Location loc1 = this.remainingRegion.checkPoint(this.line.toSubSpace(v1D));

        // check location of point with respect to second sub-line
        final Location loc2 = subLine.remainingRegion.checkPoint(subLine.line.toSubSpace(v1D));

        if (includeEndPoints) {
            return ((loc1 != Location.OUTSIDE) && (loc2 != Location.OUTSIDE)) ? v1D : null;
        } else {
            return ((loc1 == Location.INSIDE) && (loc2 == Location.INSIDE)) ? v1D : null;
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
     * @exception MathIllegalArgumentException
     *            if the points are equal
     */
    private static IntervalsSet buildIntervalSet(final Vector3D start, final Vector3D end) {
        final Line line = new Line(start, end);
        return new IntervalsSet(line.toSubSpace(start).getX(),
            line.toSubSpace(end).getX());
    }

}
