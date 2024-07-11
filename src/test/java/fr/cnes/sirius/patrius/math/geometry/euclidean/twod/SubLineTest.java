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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Euclidean1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.IntervalsSet;
import fr.cnes.sirius.patrius.math.geometry.partitioning.RegionFactory;

public class SubLineTest {

    @Test
    public void testEndPoints() {
        final Vector2D p1 = new Vector2D(-1, -7);
        final Vector2D p2 = new Vector2D(7, -1);
        final Segment segment = new Segment(p1, p2, new Line(p1, p2));
        final SubLine sub = new SubLine(segment);
        final List<Segment> segments = sub.getSegments();
        Assert.assertEquals(1, segments.size());
        Assert.assertEquals(0.0, new Vector2D(-1, -7).distance(segments.get(0).getStart()), 1.0e-10);
        Assert.assertEquals(0.0, new Vector2D(7, -1).distance(segments.get(0).getEnd()), 1.0e-10);
    }

    @Test
    public void testNoEndPoints() {
        final SubLine wholeLine = new Line(new Vector2D(-1, 7), new Vector2D(7, 1)).wholeHyperplane();
        final List<Segment> segments = wholeLine.getSegments();
        Assert.assertEquals(1, segments.size());
        Assert.assertTrue(Double.isInfinite(segments.get(0).getStart().getX()) &&
            segments.get(0).getStart().getX() < 0);
        Assert.assertTrue(Double.isInfinite(segments.get(0).getStart().getY()) &&
            segments.get(0).getStart().getY() > 0);
        Assert.assertTrue(Double.isInfinite(segments.get(0).getEnd().getX()) &&
            segments.get(0).getEnd().getX() > 0);
        Assert.assertTrue(Double.isInfinite(segments.get(0).getEnd().getY()) &&
            segments.get(0).getEnd().getY() < 0);
    }

    @Test
    public void testNoSegments() {
        final SubLine empty = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1)),
            new RegionFactory<Euclidean1D>().getComplement(new IntervalsSet()));
        final List<Segment> segments = empty.getSegments();
        Assert.assertEquals(0, segments.size());
    }

    @Test
    public void testSeveralSegments() {
        final SubLine twoSubs = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1)),
            new RegionFactory<Euclidean1D>().union(new IntervalsSet(1, 2),
                new IntervalsSet(3, 4)));
        final List<Segment> segments = twoSubs.getSegments();
        Assert.assertEquals(2, segments.size());
    }

    @Test
    public void testHalfInfiniteNeg() {
        final SubLine empty = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1)),
            new IntervalsSet(Double.NEGATIVE_INFINITY, 0.0));
        final List<Segment> segments = empty.getSegments();
        Assert.assertEquals(1, segments.size());
        Assert.assertTrue(Double.isInfinite(segments.get(0).getStart().getX()) &&
            segments.get(0).getStart().getX() < 0);
        Assert.assertTrue(Double.isInfinite(segments.get(0).getStart().getY()) &&
            segments.get(0).getStart().getY() < 0);
        Assert.assertEquals(0.0, new Vector2D(3, -4).distance(segments.get(0).getEnd()), 1.0e-10);
    }

    @Test
    public void testHalfInfinitePos() {
        final SubLine empty = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1)),
            new IntervalsSet(0.0, Double.POSITIVE_INFINITY));
        final List<Segment> segments = empty.getSegments();
        Assert.assertEquals(1, segments.size());
        Assert.assertEquals(0.0, new Vector2D(3, -4).distance(segments.get(0).getStart()), 1.0e-10);
        Assert.assertTrue(Double.isInfinite(segments.get(0).getEnd().getX()) &&
            segments.get(0).getEnd().getX() > 0);
        Assert.assertTrue(Double.isInfinite(segments.get(0).getEnd().getY()) &&
            segments.get(0).getEnd().getY() > 0);
    }

    @Test
    public void testIntersectionInsideInside() {
        final SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(3, 1));
        final SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 2));
        Assert.assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, true)), 1.0e-12);
        Assert.assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, false)), 1.0e-12);
    }

    @Test
    public void testIntersectionInsideBoundary() {
        final SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(3, 1));
        final SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 1));
        Assert.assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, true)), 1.0e-12);
        Assert.assertNull(sub1.intersection(sub2, false));
    }

    @Test
    public void testIntersectionInsideOutside() {
        final SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(3, 1));
        final SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 0.5));
        Assert.assertNull(sub1.intersection(sub2, true));
        Assert.assertNull(sub1.intersection(sub2, false));
    }

    @Test
    public void testIntersectionBoundaryBoundary() {
        final SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(2, 1));
        final SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 1));
        Assert.assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, true)), 1.0e-12);
        Assert.assertNull(sub1.intersection(sub2, false));
    }

    @Test
    public void testIntersectionBoundaryOutside() {
        final SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(2, 1));
        final SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 0.5));
        Assert.assertNull(sub1.intersection(sub2, true));
        Assert.assertNull(sub1.intersection(sub2, false));
    }

    @Test
    public void testIntersectionOutsideOutside() {
        final SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(1.5, 1));
        final SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 0.5));
        Assert.assertNull(sub1.intersection(sub2, true));
        Assert.assertNull(sub1.intersection(sub2, false));
    }

}
