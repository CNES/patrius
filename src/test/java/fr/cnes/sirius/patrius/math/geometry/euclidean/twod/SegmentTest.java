/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.8:DM:DM-2979:15/11/2021:[PATRIUS] Ajout d'une methode public getIntersection(Segment) dans la classe Segment 2D 
* VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 *
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class SegmentTest {

    @Test
    public void testDistance() {
        final Vector2D start = new Vector2D(2, 2);
        final Vector2D end = new Vector2D(-2, -2);
        final Segment segment = new Segment(start, end, new Line(start, end));

        // distance to center of segment
        Assert.assertEquals(MathLib.sqrt(2), segment.distance(new Vector2D(1, -1)), 1.0e-10);

        // distance a point on segment
        Assert.assertEquals(MathLib.sin(Math.PI / 4.0), segment.distance(new Vector2D(0, -1)), 1.0e-10);

        // distance to end point
        Assert.assertEquals(MathLib.sqrt(8), segment.distance(new Vector2D(0, 4)), 1.0e-10);

        // distance to start point
        Assert.assertEquals(MathLib.sqrt(8), segment.distance(new Vector2D(0, -4)), 1.0e-10);
    }

    /**
     * Test {@link Segment#getClosestPoint(Vector2D)} method for various configurations.
     */
    @Test
    public void testClosestPoint() {
        final Vector2D start = new Vector2D(2, 2);
        final Vector2D end = new Vector2D(-2, -2);
        final Segment segment = new Segment(start, end, new Line(start, end));

        // distance to center of segment
        Assert.assertEquals(Vector2D.distance(new Vector2D(0, 0), segment.getClosestPoint(new Vector2D(1, -1))), 0, 1.0e-10);

        // distance a point on segment
        Assert.assertEquals(Vector2D.distance(new Vector2D(-0.5, -0.5), segment.getClosestPoint(new Vector2D(0, -1))), 0, 1.0e-10);

        // distance to start point
        Assert.assertEquals(Vector2D.distance(new Vector2D(2, 2), segment.getClosestPoint(new Vector2D(0, 4))), 0, 1.0e-10);

        // distance to end point
        Assert.assertEquals(Vector2D.distance(new Vector2D(-2, -2), segment.getClosestPoint(new Vector2D(0, -4))), 0, 1.0e-10);
    }

    /**
     * Test {@link Segment#getIntersection(Segment)} method for various configurations.
     */
    @Test
    public void testIntersection() {
        // First segment
        final Vector2D start1 = new Vector2D(1, 1);
        final Vector2D end1 = new Vector2D(2, 2);
        final Segment segment1 = new Segment(start1, end1, new Line(start1, end1));

        // No intersection
        final Vector2D start2 = new Vector2D(2, 1);
        final Vector2D end2 = new Vector2D(3, 2);
        final Segment segment2 = new Segment(start2, end2, new Line(start2, end2));
        Assert.assertNull(segment1.getIntersection(segment2));
        
        // Intersection on the line but not on the segment
        final Vector2D start3 = new Vector2D(3, 1);
        final Vector2D end3 = new Vector2D(3, 2);
        final Segment segment3 = new Segment(start3, end3, new Line(start3, end3));
        Assert.assertNull(segment1.getIntersection(segment3));
        
        // Intersection on the segment
        final Vector2D start4 = new Vector2D(1.5, 1);
        final Vector2D end4 = new Vector2D(1.5, 2);
        final Segment segment4 = new Segment(start4, end4, new Line(start4, end4));
        Assert.assertTrue(segment1.getIntersection(segment4).equals(new Vector2D(1.5, 1.5)));
        
        // Intersection on the segment boundaries
        final Vector2D start5 = new Vector2D(2, 1);
        final Vector2D end5 = new Vector2D(2, 2);
        final Segment segment5 = new Segment(start5, end5, new Line(start5, end5));
        Assert.assertTrue(segment1.getIntersection(segment5).equals(new Vector2D(2, 2)));
        
        // Segments on same line
        final Vector2D start6 = new Vector2D(0, 0);
        final Vector2D end6 = new Vector2D(1, 1);
        final Segment segment6 = new Segment(start6, end6, new Line(start6, end6));
        Assert.assertNull(segment1.getIntersection(segment6));
        
        // Segments on same line
        final Vector2D start7 = new Vector2D(0, 0);
        final Vector2D end7 = new Vector2D(-1, -1);
        final Segment segment7 = new Segment(start7, end7, new Line(start7, end7));
        Assert.assertNull(segment1.getIntersection(segment7));
        
        // Same segment
        Assert.assertNull(segment1.getIntersection(segment1));
    }

    /**
     * For coverage purpose : gets through the if condition r<0 or r>1 in method distance
     * meaning if the point is not inside the segment
     */
    @Test
    public void testDistancePointOutsideSegment() {
        final Vector2D start = new Vector2D(0, 0);
        final Vector2D end = new Vector2D(1, 0);
        final Segment segment = new Segment(start, end, new Line(start, end));

        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        // case r>1 : closest distance ? to the end of segment
        Assert.assertEquals(1, segment.distance(new Vector2D(2, 0)), eps);

        // case r<0 : closest distance ? to the beginning of segment
        Assert.assertEquals(1, segment.distance(new Vector2D(-1, 0)), eps);

    }
}
