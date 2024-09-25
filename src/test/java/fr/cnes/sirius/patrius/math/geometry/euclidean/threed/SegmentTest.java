/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
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
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;

public class SegmentTest {
    
    @Test
    public void testConstructorsAndGetters() {
        // Constructor without line
        final Vector3D start = new Vector3D(1, 1, 1);
        final Vector3D end = new Vector3D(-1, -1, -1);
        Segment segment = new Segment(start, end);
        final Line line = new Line(start, end);
        Assert.assertEquals(start, segment.getStart());
        Assert.assertEquals(end, segment.getEnd());
        Assert.assertEquals(line.getOrigin(), segment.getLine().getOrigin());
        Assert.assertEquals(line.getDirection(), segment.getLine().getDirection());
        Assert.assertEquals(line.getMinAbscissa(), segment.getLine().getMinAbscissa(),
            Precision.DOUBLE_COMPARISON_EPSILON);

        // Constructor with line
        segment = new Segment(start, end, line);
        Assert.assertEquals(start, segment.getStart());
        Assert.assertEquals(end, segment.getEnd());
        Assert.assertEquals(line.getOrigin(), segment.getLine().getOrigin());
        Assert.assertEquals(line.getDirection(), segment.getLine().getDirection());
        Assert.assertEquals(line.getMinAbscissa(), segment.getLine().getMinAbscissa(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }
    
    @Test
    public void testIsIntersectingSegments() {
        // Case 1: without intersecting segments

        // Define the array of points
        final Vector3D[] points = new Vector3D[4];
        points[0] = new Vector3D(1.0, 1.0, 1.0);
        points[1] = new Vector3D(-1.0, 1.0, 1.0);
        points[2] = new Vector3D(-1.0, -1.0, 1.0);
        points[3] = new Vector3D(1.0, -1.0, 1.0);

        // Create the array of segments
        final Segment[] segments = new Segment[4];
        segments[0] = new Segment(points[0], points[1]);
        segments[1] = new Segment(points[1], points[2]);
        segments[2] = new Segment(points[2], points[3]);
        segments[3] = new Segment(points[3], points[0]);

        // Check that the segments do not intersect
        Assert.assertEquals(false, Segment.isIntersectingSegments(segments));

        // Case 2: with intersecting segments

        // Define the array of points
        points[0] = new Vector3D(-0.001, 0.001, 1);
        points[1] = new Vector3D(-0.001, -0.001, 1);
        points[2] = new Vector3D(0.001, 0.001, 1);
        points[3] = new Vector3D(0.001, -0.001, 1);

        // Create the array of segments
        segments[0] = new Segment(points[0], points[1]);
        segments[1] = new Segment(points[1], points[2]);
        segments[2] = new Segment(points[2], points[3]);
        segments[3] = new Segment(points[3], points[0]);

        // Check that the segments intersect
        Assert.assertEquals(true, Segment.isIntersectingSegments(segments));
    }

}
