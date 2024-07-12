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
 * VERSION::FA:306:12/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

import java.awt.geom.AffineTransform;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Euclidean1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Transform;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class LineTest {

    /**
     * For coverage purpose.
     * Tests the method revertSelf() with an angle superior to PI.
     */
    @Test
    public void testRevertSelf() {
        final Line l = new Line(new Vector2D(1, 1), new Vector2D(1, 1));
        final double angle = 3 * FastMath.PI / 2.;
        l.setAngle(angle);
        l.revertSelf();
        final double expectedAngle = angle - FastMath.PI;
        Assert.assertEquals(expectedAngle, l.getAngle(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * For coverage purpose.
     * Tests the exception case if constructor of private class LineTransform.
     * The matrix associated with the affine transformation must be non invertible,
     * thus [1 0;0 0] is a correct candidate.
     * Expected behavior : PatriusMessages.NON_INVERTIBLE_TRANSFORM
     */
    @Test(expected = MathIllegalArgumentException.class)
    public void testLineTransformNonInvertibleTransform() {

        final AffineTransform transform = new AffineTransform(1, 0, 0, 0, 1, 1);
        Line.getTransform(transform);
    }

    @Test
    public void testLine() {
        final Line l = new Line(new Vector2D(1, 1), new Vector2D(1, 1));
        Assert.assertEquals(1., l.getOriginOffset(), 1.0e-10);
        Assert.assertEquals(0., l.getAngle(), 1.0e-10);

        final Line l1 = new Line(new Vector2D(1, 2), new Vector2D(2, 3));
        final Line l2 = l1.copySelf();
        l1.revertSelf();
        double expectedAngle = l2.getAngle() + FastMath.PI;
        Assert.assertEquals(expectedAngle, l1.getAngle(), 1.0e-10);
        Assert.assertEquals(-l2.getOriginOffset(), l1.getOriginOffset(), 1.0e-10);

        final Line l3 = new Line(new Vector2D(0, 0), new Vector2D(1, 1));
        Assert.assertEquals(-MathLib.sqrt(2.) / 2., l1.getOffset(l3), 1.0e-10);
        Assert.assertEquals(-MathLib.sqrt(2.) / 2., l1.getOffset(new Vector2D(2, 2)), 1.0e-10);

        final Line l4 = new Line(new Vector2D(2, 0), new Vector2D(2, 3));
        l1.translateToPoint(new Vector2D(0, 3));
        Assert.assertEquals(-2., l4.getOriginOffset(), 1.0e-10);

        expectedAngle = l1.getAngle() + FastMath.PI / 2.;
        l1.setAngle(expectedAngle);
        l1.setOriginOffset(2.);
        Assert.assertEquals(expectedAngle, l1.getAngle(), 1.0e-10);
        Assert.assertEquals(2., l1.getOriginOffset(), 1.0e-10);
    }

    @Test
    public void testContains() {
        final Line l = new Line(new Vector2D(0, 1), new Vector2D(1, 2));
        Assert.assertTrue(l.contains(new Vector2D(0, 1)));
        Assert.assertTrue(l.contains(new Vector2D(1, 2)));
        Assert.assertTrue(l.contains(new Vector2D(7, 8)));
        Assert.assertTrue(!l.contains(new Vector2D(8, 7)));
    }

    @Test
    public void testAbscissa() {
        final Line l = new Line(new Vector2D(2, 1), new Vector2D(-2, -2));
        Assert.assertEquals(0.0,
            (l.toSubSpace(new Vector2D(-3, 4))).getX(),
            1.0e-10);
        Assert.assertEquals(0.0,
            (l.toSubSpace(new Vector2D(3, -4))).getX(),
            1.0e-10);
        Assert.assertEquals(-5.0,
            (l.toSubSpace(new Vector2D(7, -1))).getX(),
            1.0e-10);
        Assert.assertEquals(5.0,
            (l.toSubSpace(new Vector2D(-1, -7))).getX(),
            1.0e-10);
    }

    @Test
    public void testOffset() {
        final Line l = new Line(new Vector2D(2, 1), new Vector2D(-2, -2));
        Assert.assertEquals(-5.0, l.getOffset(new Vector2D(5, -3)), 1.0e-10);
        Assert.assertEquals(+5.0, l.getOffset(new Vector2D(-5, 2)), 1.0e-10);
    }

    @Test
    public void testDistance() {
        final Line l = new Line(new Vector2D(2, 1), new Vector2D(-2, -2));
        Assert.assertEquals(+5.0, l.distance(new Vector2D(5, -3)), 1.0e-10);
        Assert.assertEquals(+5.0, l.distance(new Vector2D(-5, 2)), 1.0e-10);
    }

    @Test
    public void testPointAt() {
        final Line l = new Line(new Vector2D(2, 1), new Vector2D(-2, -2));
        for (double a = -2.0; a < 2.0; a += 0.2) {
            final Vector1D pA = new Vector1D(a);
            Vector2D point = l.toSpace(pA);
            Assert.assertEquals(a, (l.toSubSpace(point)).getX(), 1.0e-10);
            Assert.assertEquals(0.0, l.getOffset(point), 1.0e-10);
            for (double o = -2.0; o < 2.0; o += 0.2) {
                point = l.getPointAt(pA, o);
                Assert.assertEquals(a, (l.toSubSpace(point)).getX(), 1.0e-10);
                Assert.assertEquals(o, l.getOffset(point), 1.0e-10);
            }
        }
    }

    @Test
    public void testOriginOffset() {
        final Line l1 = new Line(new Vector2D(0, 1), new Vector2D(1, 2));
        Assert.assertEquals(MathLib.sqrt(0.5), l1.getOriginOffset(), 1.0e-10);
        final Line l2 = new Line(new Vector2D(1, 2), new Vector2D(0, 1));
        Assert.assertEquals(-MathLib.sqrt(0.5), l2.getOriginOffset(), 1.0e-10);
    }

    @Test
    public void testParallel() {
        final Line l1 = new Line(new Vector2D(0, 1), new Vector2D(1, 2));
        final Line l2 = new Line(new Vector2D(2, 2), new Vector2D(3, 3));
        Assert.assertTrue(l1.isParallelTo(l2));
        final Line l3 = new Line(new Vector2D(1, 0), new Vector2D(0.5, -0.5));
        Assert.assertTrue(l1.isParallelTo(l3));
        final Line l4 = new Line(new Vector2D(1, 0), new Vector2D(0.5, -0.51));
        Assert.assertTrue(!l1.isParallelTo(l4));
    }

    @Test
    public void testTransform() throws MathIllegalArgumentException {

        final Line l1 = new Line(new Vector2D(1.0, 1.0), new Vector2D(4.0, 1.0));
        final Transform<Euclidean2D, Euclidean1D> t1 =
            Line.getTransform(new AffineTransform(0.0, 0.5, -1.0, 0.0, 1.0, 1.5));
        Assert.assertEquals(0.5 * FastMath.PI,
            ((Line) t1.apply(l1)).getAngle(),
            1.0e-10);

        final Line l2 = new Line(new Vector2D(0.0, 0.0), new Vector2D(1.0, 1.0));
        final Transform<Euclidean2D, Euclidean1D> t2 =
            Line.getTransform(new AffineTransform(0.0, 0.5, -1.0, 0.0, 1.0, 1.5));
        Assert.assertEquals(MathLib.atan2(1.0, -2.0),
            ((Line) t2.apply(l2)).getAngle(),
            1.0e-10);

    }

    @Test
    public void testIntersection() {
        final Line l1 = new Line(new Vector2D(0, 1), new Vector2D(1, 2));
        final Line l2 = new Line(new Vector2D(-1, 2), new Vector2D(2, 1));
        final Vector2D p = l1.intersection(l2);
        Assert.assertEquals(0.5, p.getX(), 1.0e-10);
        Assert.assertEquals(1.5, p.getY(), 1.0e-10);
    }

}
