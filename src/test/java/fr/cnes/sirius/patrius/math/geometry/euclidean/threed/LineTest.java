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
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class LineTest {

    @Test
    public void testCreateLines() {
        // Infinite lines
        final Vector3D p1 = new Vector3D(1, 2, 3);
        final Vector3D dir = new Vector3D(1, 1, 1).normalize();
        final Line l1 = Line.createLine(p1, dir);
        final Line l2 = new Line(p1, new Vector3D(2, 3, 4));

        Assert.assertEquals(l1.getOrigin().getX(), l2.getOrigin().getX(), 1E-14);
        Assert.assertEquals(l1.getOrigin().getY(), l2.getOrigin().getY(), 1E-14);
        Assert.assertEquals(l1.getOrigin().getZ(), l2.getOrigin().getZ(), 1E-14);
        Assert.assertEquals(l1.getDirection().getX(), l2.getDirection().getX(), 1E-14);
        Assert.assertEquals(l1.getDirection().getY(), l2.getDirection().getY(), 1E-14);
        Assert.assertEquals(l1.getDirection().getZ(), l2.getDirection().getZ(), 1E-14);
        Assert.assertEquals(l1.getMinAbscissa(), l2.getMinAbscissa(), 1E-14);

        // Semi-finite lines
        final Line l3 = Line.createLine(p1, dir, p1);
        final Line l4 = new Line(p1, new Vector3D(2, 3, 4), p1);

        Assert.assertEquals(l3.getOrigin().getX(), l4.getOrigin().getX(), 1E-14);
        Assert.assertEquals(l3.getOrigin().getY(), l4.getOrigin().getY(), 1E-14);
        Assert.assertEquals(l3.getOrigin().getZ(), l4.getOrigin().getZ(), 1E-14);
        Assert.assertEquals(l3.getDirection().getX(), l4.getDirection().getX(), 1E-14);
        Assert.assertEquals(l3.getDirection().getY(), l4.getDirection().getY(), 1E-14);
        Assert.assertEquals(l3.getDirection().getZ(), l4.getDirection().getZ(), 1E-14);
        Assert.assertEquals(l3.getMinAbscissa(), l4.getMinAbscissa(), 1E-14);

        // Test zero norm case
        try {
            Line.createLine(p1, Vector3D.ZERO);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertEquals(PatriusMessages.ZERO_NORM.getSourceString(), e.getMessage());
        }
    }

    @Test
    public void testContains() throws MathIllegalArgumentException, MathArithmeticException {
        final Vector3D p1 = new Vector3D(0, 0, 1);
        final Line l = new Line(p1, new Vector3D(0, 0, 2));
        Assert.assertTrue(l.contains(p1));
        Assert.assertTrue(l.contains(new Vector3D(1.0, p1, 0.3, l.getDirection())));
        final Vector3D u = l.getDirection().orthogonal();
        final Vector3D v = Vector3D.crossProduct(l.getDirection(), u);
        for (double alpha = 0; alpha < 2 * FastMath.PI; alpha += 0.3) {
            Assert.assertFalse(l.contains(p1.add(new Vector3D(MathLib.cos(alpha), u,
                MathLib.sin(alpha), v))));
        }
    }

    @Test
    public void testSimilar() throws MathIllegalArgumentException, MathArithmeticException {
        final Vector3D p1 = new Vector3D(1.2, 3.4, -5.8);
        final Vector3D p2 = new Vector3D(3.4, -5.8, 1.2);
        final Vector3D p3 = p1.add(10, p2.subtract(p1));
        final Line lA = new Line(p1, p2);
        final Line lB = lA.revert();
        final Line lC = new Line(p2, p1);
        final Line lD = new Line(p1, p2, p1);
        final Line lE = new Line(p1, p3, p1);
        final Line lF = new Line(p1, p2, p2);

        Assert.assertTrue(lA.isSimilarTo(lA));
        Assert.assertTrue(lA.isSimilarTo(lB));
        Assert.assertTrue(lA.isSimilarTo(lC));
        Assert.assertTrue(lB.isSimilarTo(lC));
        Assert.assertFalse(lA.isSimilarTo(lD));
        Assert.assertFalse(lA.isSimilarTo(new Line(p1, lA.getDirection().orthogonal())));
        Assert.assertTrue(lD.isSimilarTo(lE));
        Assert.assertFalse(lD.isSimilarTo(lF));
        Assert.assertFalse(lD.isSimilarTo(lD.revert()));
    }

    @Test
    public void testRevert() throws MathIllegalArgumentException, MathArithmeticException {
        final Vector3D p1 = new Vector3D(1., 1., 1.);
        final Vector3D p2 = new Vector3D(2., 2., 2.);
        final Vector3D pMinAbscissa = new Vector3D(1.2, 1.2, 1.2);
        final Line lA = new Line(p1, p2, pMinAbscissa);
        final Line lB = lA.revert();
        Assert.assertEquals(lB.getDirection().getX(), -lA.getDirection().getX(), 1E-14);
        Assert.assertEquals(lB.getDirection().getY(), -lA.getDirection().getY(), 1E-14);
        Assert.assertEquals(lB.getDirection().getZ(), -lA.getDirection().getZ(), 1E-14);
        Assert.assertEquals(lB.getOrigin().getX(), lA.getOrigin().getX(), 1E-14);
        Assert.assertEquals(lB.getOrigin().getY(), lA.getOrigin().getY(), 1E-14);
        Assert.assertEquals(lB.getOrigin().getZ(), lA.getOrigin().getZ(), 1E-14);
        Assert.assertEquals(lB.pointAt(lB.getMinAbscissa()).getX(), lA.pointAt(lA.getMinAbscissa()).getX(), 1E-14);
        Assert.assertEquals(lB.pointAt(lB.getMinAbscissa()).getY(), lA.pointAt(lA.getMinAbscissa()).getY(), 1E-14);
        Assert.assertEquals(lB.pointAt(lB.getMinAbscissa()).getZ(), lA.pointAt(lA.getMinAbscissa()).getZ(), 1E-14);
        Assert.assertEquals(lB.getMinAbscissa(), -lA.getMinAbscissa(), 1E-14);
    }

    @Test
    public void testPointDistance() throws MathIllegalArgumentException {
        final Line l = new Line(new Vector3D(0, 1, 1), new Vector3D(0, 2, 2));
        Assert.assertEquals(MathLib.sqrt(3.0 / 2.0), l.distance(new Vector3D(1, 0, 1)), 1.0e-10);
        Assert.assertEquals(0, l.distance(new Vector3D(0, -4, -4)), 1.0e-10);

        // Test with semi-finite line
        final Line l2 = new Line(new Vector3D(0, 1, 1), new Vector3D(0, 2, 2), new Vector3D(0, 1, 1));
        Assert.assertEquals(MathLib.sqrt(3.0 / 2.0), l2.distance(new Vector3D(1, 1, 2)), 1.0e-10);
        Assert.assertEquals(5 * MathLib.sqrt(2), l2.distance(new Vector3D(0, -4, -4)), 1.0e-10);
        Assert.assertEquals(MathLib.sqrt(14), l2.distance(new Vector3D(-1, -1, -2)), 1.0e-10);
    }

    @Test
    public void testLineDistance() throws MathIllegalArgumentException {
        final Line l = new Line(new Vector3D(0, 1, 1), new Vector3D(0, 2, 2));
        Assert.assertEquals(1.0,
            l.distance(new Line(new Vector3D(1, 0, 1), new Vector3D(1, 0, 2))),
            1.0e-10);
        Assert.assertEquals(0.5,
            l.distance(new Line(new Vector3D(-0.5, 0, 0), new Vector3D(-0.5, -1, -1))),
            1.0e-10);
        Assert.assertEquals(0.0,
            l.distance(l),
            1.0e-10);
        Assert.assertEquals(0.0,
            l.distance(new Line(new Vector3D(0, -4, -4), new Vector3D(0, -5, -5))),
            1.0e-10);
        Assert.assertEquals(0.0,
            l.distance(new Line(new Vector3D(0, -4, -4), new Vector3D(0, -3, -4))),
            1.0e-10);
        Assert.assertEquals(0.0,
            l.distance(new Line(new Vector3D(0, -4, -4), new Vector3D(1, -4, -4))),
            1.0e-10);
        Assert.assertEquals(MathLib.sqrt(8),
            l.distance(new Line(new Vector3D(0, -4, 0), new Vector3D(1, -4, 0))),
            1.0e-10);

        final Line l2 = new Line(new Vector3D(5, 4, 0), new Vector3D(5, 4, 1), new Vector3D(8, 4, 7));
        Assert.assertEquals(new Vector3D(5, 4, 7).distance(new Vector3D(0, 5.5, 5.5)), l.distance(l2), 1.0e-10);

    }

    @Test
    public void testClosest() throws MathIllegalArgumentException {
        final Line l = new Line(new Vector3D(0, 1, 1), new Vector3D(0, 2, 2));
        Assert.assertEquals(0.0,
            l.closestPoint(new Line(new Vector3D(1, 0, 1), new Vector3D(1, 0, 2))).distance(new Vector3D(0, 0, 0)),
            1.0e-10);
        Assert.assertEquals(
            0.5,
            l.closestPoint(new Line(new Vector3D(-0.5, 0, 0), new Vector3D(-0.5, -1, -1))).distance(
                new Vector3D(-0.5, 0, 0)),
            1.0e-10);
        Assert.assertEquals(0.0,
            l.closestPoint(l).distance(new Vector3D(0, 0, 0)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            l.closestPoint(new Line(new Vector3D(0, -4, -4), new Vector3D(0, -5, -5))).distance(
                new Vector3D(0, 0, 0)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            l.closestPoint(new Line(new Vector3D(0, -4, -4), new Vector3D(0, -3, -4))).distance(
                new Vector3D(0, -4, -4)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            l.closestPoint(new Line(new Vector3D(0, -4, -4), new Vector3D(1, -4, -4))).distance(
                new Vector3D(0, -4, -4)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            l.closestPoint(new Line(new Vector3D(1, -4, -4), new Vector3D(1, -5, -5))).distance(
                new Vector3D(0, 0, 0)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            l.closestPoint(new Line(new Vector3D(0, -4, 0), new Vector3D(1, -4, 0))).distance(
                new Vector3D(0, -2, -2)),
            1.0e-10);

        // Test again with semi-finite line
        final Line l2 = new Line(new Vector3D(0, 1, 1), new Vector3D(0, 2, 2), new Vector3D(-4, 3, 3));
        Assert.assertEquals(0.0, l2.closestPoint(new Line(new Vector3D(-2, 1, 1), new Vector3D(-2, 5, 5))).distance(
            new Vector3D(0, 3, 3)), 1.0e-10);
        Assert.assertEquals(0.0, l2.closestPoint(new Line(Vector3D.ZERO, Vector3D.PLUS_I)).distance(
            new Vector3D(0, 3, 3)), 1.0e-10);
        
        final Line infinite = new Line(new Vector3D(1, 0, 0), new Vector3D(0, 1, 0));
        final Line semiFinite = new Line(new Vector3D(1.1, 0, 0), new Vector3D(1.1, 1, 0), new Vector3D(1.1, 1.1, 0));
        Assert.assertEquals(0.0, infinite.closestPoint(semiFinite).distance(new Vector3D(0.5, 0.5, 0)), 1.0e-10);

        /**
         * Test again with method closestPointTo
         */
        Vector3D[] points = new Vector3D[2];

        // Two infinite lines
        final Line parallelInfinite = new Line(new Vector3D(-2, 1, 1), new Vector3D(-2, 5, 5));
        points = l.closestPointTo(parallelInfinite);
        Assert.assertEquals(0.0, points[0].distance(parallelInfinite.getOrigin()), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(l.getOrigin()), 1.0e-10);

        final Line otherDirInfinite = new Line(new Vector3D(5, 4, 0), new Vector3D(5, 4, 1));
        points = l.closestPointTo(otherDirInfinite);
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(5, 4, 4)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 4, 4)), 1.0e-10);

        // One infinite line and one semi-finite line
        final Line parallelSemiFinite = new Line(new Vector3D(-2, 1, 1), new Vector3D(-2, 5, 5), new Vector3D(-4, 7, 7));
        points = l.closestPointTo(parallelSemiFinite);
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(-2, 7, 7)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 7, 7)), 1.0e-10);

        final Line otherDirSemiFinite = new Line(new Vector3D(5, 4, 0), new Vector3D(5, 4, 1), new Vector3D(8, 4, 7));
        points = l.closestPointTo(otherDirSemiFinite);
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(5, 4, 7)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 5.5, 5.5)), 1.0e-10);

        // Two semi-finite lines
        points = l2.closestPointTo(parallelSemiFinite);
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(-2, 7, 7)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 7, 7)), 1.0e-10);

        points = l2.revert().closestPointTo(parallelSemiFinite);
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(-2, 7, 7)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 3, 3)), 1.0e-10);

        points = l2.closestPointTo(otherDirSemiFinite);
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(5, 4, 7)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 5.5, 5.5)), 1.0e-10);

        points = l2.closestPointTo(otherDirSemiFinite.revert());
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(5, 4, 4)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 4, 4)), 1.0e-10);

        points = l2.revert().closestPointTo(otherDirSemiFinite);
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(5, 4, 7)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 3, 3)), 1.0e-10);

        points = l2.revert().closestPointTo(otherDirSemiFinite.revert());
        Assert.assertEquals(0.0, points[0].distance(new Vector3D(5, 4, 3)), 1.0e-10);
        Assert.assertEquals(0.0, points[1].distance(new Vector3D(0, 3, 3)), 1.0e-10);
    }

    @Test
    public void testLineIntersections() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
        Vector3D p1 = new Vector3D(0, 1, 1);
        Vector3D p2 = new Vector3D(0, 2, 2);
        final Line line1 = new Line(p1, p2);
        final Vector3D pMinAbscissaSmall = new Vector3D(7.0, 7.0, 7.0);
        final Vector3D pMinAbscissaBig = new Vector3D(9.0, 9.0, 9.0);
        final Line line1minAbscissaSmall = new Line(p1, p2, pMinAbscissaSmall);
        final Line line1minAbscissaBig = new Line(p1, p2, pMinAbscissaBig);

        // test with a line intersecting not line1
        p1 = new Vector3D(1, 1, 1);
        p2 = new Vector3D(8, 3, 2);
        Line line2 = new Line(p1, p2);
        Vector3D[] intersections = line1.getIntersectionPoints(line2);
        Assert.assertEquals(0, intersections.length);

        // tests with a line intersecting line1
        p1 = new Vector3D(-8, 0, 0);
        p2 = new Vector3D(-7, 1, 1);
        line2 = new Line(p1, p2);
        intersections = line1.getIntersectionPoints(line2);
        Assert.assertEquals(1, intersections.length);

        Vector3D intersect1 = intersections[0];

        Assert.assertEquals(0.0, intersect1.getX(), eps);
        Assert.assertEquals(8.0, intersect1.getY(), eps);
        Assert.assertEquals(8.0, intersect1.getZ(), eps);

        // tests with a line intersecting line1
        p1 = new Vector3D(-8, 0, 0);
        p2 = new Vector3D(-7, 1, 1);
        intersections = line1.getIntersectionPoints(line2);
        Assert.assertEquals(1, intersections.length);

        intersect1 = intersections[0];

        Assert.assertEquals(0.0, intersect1.getX(), eps);
        Assert.assertEquals(8.0, intersect1.getY(), eps);
        Assert.assertEquals(8.0, intersect1.getZ(), eps);

        // tests with a line intersecting line1minAbscissaSmall on the right side of the line
        p1 = new Vector3D(-8, 0, 0);
        p2 = new Vector3D(-7, 1, 1);
        intersections = line1minAbscissaSmall.getIntersectionPoints(line2);
        Assert.assertEquals(1, intersections.length);

        intersect1 = intersections[0];

        Assert.assertEquals(0.0, intersect1.getX(), eps);
        Assert.assertEquals(8.0, intersect1.getY(), eps);
        Assert.assertEquals(8.0, intersect1.getZ(), eps);

        // tests with a line intersecting line1minAbscissaBig on the wrong side of the line
        intersections = line1minAbscissaBig.getIntersectionPoints(line2);

        Assert.assertEquals(0, intersections.length);
        Assert.assertEquals(0.81, line1minAbscissaBig.distance(line2), 0.1);

        // test with a line identical to line1
        p1 = new Vector3D(0, 1, 1);
        p2 = new Vector3D(0, 2, 2);
        line2 = new Line(p1, p2);
        intersections = line1.getIntersectionPoints(line2);

        Assert.assertEquals(1, intersections.length);

        intersect1 = intersections[0];
        Assert.assertEquals(0.0, intersect1.getX(), eps);
        Assert.assertEquals(0.0, intersect1.getY(), eps);
        Assert.assertEquals(0.0, intersect1.getZ(), eps);

        // test with a line parallel to line1, but different
        p1 = new Vector3D(0, 4, 1);
        p2 = new Vector3D(0, 5, 2);
        line2 = new Line(p1, p2);
        line2 = new Line(line2);
        intersections = line1.getIntersectionPoints(line2);
        Assert.assertEquals(0, intersections.length);

        // test with wrong line
        try {
            line2 = new Line(p1, p1);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            Assert.assertEquals(PatriusMessages.ZERO_NORM.getSourceString(), e.getMessage());
        }

        // Tests where lines are identical regardless abscissas
        final Line line3 = new Line(new Vector3D(0, 3, 3), new Vector3D(0, 7, 7), new Vector3D(5, 5, 5));
        final Line line4 = new Line(new Vector3D(0, 2, 2), new Vector3D(0, 1, 1), new Vector3D(0, 3, 3));
        Assert.assertEquals(0.0, line1.getIntersectionPoints(line3)[0].distance(new Vector3D(0, 5, 5)), 1.0e-10);
        Assert.assertEquals(0.0, line3.getIntersectionPoints(line1)[0].distance(new Vector3D(0, 5, 5)), 1.0e-10);
        Assert.assertEquals(0, line3.getIntersectionPoints(line4).length);
    }

    @Test
    public void testIntersection() throws MathIllegalArgumentException {
        final Line line1 = new Line(new Vector3D(0, 1, 1), new Vector3D(0, 2, 2));
        Assert.assertNull(line1.intersection(new Line(new Vector3D(1, 0, 1), new Vector3D(1, 0, 2))));
        Assert.assertNull(line1.intersection(new Line(new Vector3D(-0.5, 0, 0), new Vector3D(-0.5, -1, -1))));
        Assert.assertEquals(0.0,
            line1.intersection(line1).distance(new Vector3D(0, 0, 0)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            line1.intersection(new Line(new Vector3D(0, -4, -4), new Vector3D(0, -5, -5))).distance(
                new Vector3D(0, 0, 0)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            line1.intersection(new Line(new Vector3D(0, -4, -4), new Vector3D(0, -3, -4))).distance(
                new Vector3D(0, -4, -4)),
            1.0e-10);
        Assert.assertEquals(
            0.0,
            line1.intersection(new Line(new Vector3D(0, -4, -4), new Vector3D(1, -4, -4))).distance(
                new Vector3D(0, -4, -4)),
            1.0e-10);
        Assert.assertNull(line1.intersection(new Line(new Vector3D(0, -4, 0), new Vector3D(1, -4, 0))));

        // tests with a line intersecting line1minAbscissaSmall on the right side of the line
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
        Vector3D p1 = new Vector3D(0, 1, 1);
        Vector3D p2 = new Vector3D(0, 2, 2);
        final Vector3D pMinAbscissaSmall = new Vector3D(7.0, 7.0, 7.0);
        final Vector3D pMinAbscissaBig = new Vector3D(9.0, 9.0, 9.0);
        final Line line1minAbscissaSmall = new Line(p1, p2, pMinAbscissaSmall);
        final Line line1minAbscissaBig = new Line(p1, p2, pMinAbscissaBig);
        p1 = new Vector3D(-8, 0, 0);
        p2 = new Vector3D(-7, 1, 1);
        final Line line2 = new Line(p1, p2);
        Vector3D intersections = line1minAbscissaSmall.intersection(line2);

        Assert.assertNotNull(intersections);

        Assert.assertEquals(0.0, intersections.getX(), eps);
        Assert.assertEquals(8.0, intersections.getY(), eps);
        Assert.assertEquals(8.0, intersections.getZ(), eps);

        // tests with a line intersecting line1minAbscissaBig on the wrong side of the line
        intersections = line1minAbscissaBig.intersection(line2);

        Assert.assertNull(intersections);
    }

    @Test
    public void testAbscissas() {
        /**
         * Test min abscissas
         */
        // Create the points to be used for the creation of the lines
        final Vector3D p1 = new Vector3D(0, 1, 1);
        final Vector3D p2 = new Vector3D(0, 2, 2);

        // Case without the point of minimum abscissa specified
        final Line l = new Line(p1, p2);
        Assert.assertNotNull(l);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, l.getMinAbscissa(), 0.);

        // Case with the point of minimum abscissa specified
        final Line l2 = new Line(p1, p2, new Vector3D(0, 0, 0));
        Assert.assertNotNull(l2);
        Assert.assertEquals(0., l2.getMinAbscissa(), 0.);

        // Case with a line not having the point of minimum abscissa specified
        final Line l3 = new Line(l);
        Assert.assertNotNull(l3);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, l3.getMinAbscissa(), 0.);

        // Case with a line having the point of minimum abscissa specified
        final Line l4 = new Line(l2);
        Assert.assertNotNull(l4);
        Assert.assertEquals(0., l4.getMinAbscissa(), 0.);

        // Case with a reset
        l4.reset(p1, p2);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, l4.getMinAbscissa(), 0.);

        /**
         * Test abscissas getter
         */
        Assert.assertEquals(0, l.getAbscissa(new Vector3D(0, 0, 0)), 0.);
        Assert.assertEquals(0, l.getAbscissa(new Vector3D(5, 0, 0)), 0.);
        Assert.assertEquals(0, l3.getAbscissa(new Vector3D(0, 0, 0)), 0.);
        Assert.assertEquals(0, l3.getAbscissa(new Vector3D(5, 0, 0)), 0.);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, l3.getAbscissa(new Vector3D(Double.NaN, 0, 0)), 0.);
    }

    @Test
    public void testWholeLine() {

        final Line line = new Line(new Vector3D(0, 1, 1), new Vector3D(0, 2, 2), new Vector3D(5, 7, 9));
        final SubLine subLine = line.wholeLine();
        final List<Segment> list = subLine.getSegments();

        Assert.assertEquals(1, list.size());
        Assert.assertEquals(Double.NaN, list.get(0).getStart().getX(), 0.);
        Assert.assertEquals(Double.NaN, list.get(0).getEnd().getX(), 0.);
    }

    @Test
    public void testPointOfMinAbscissa() {

        final Line line = new Line(new Vector3D(1, 0, 0), new Vector3D(2, 0, 0), new Vector3D(5, 7, 9));
        final Vector3D[] points = new Vector3D[] { new Vector3D(0, 1, 1), new Vector3D(2, 4, 3), new Vector3D(5, 7, 9),
            new Vector3D(-3, 10, 30), new Vector3D(-2, 5, 5), new Vector3D(-3, 0, 0) };

        Assert.assertEquals(0.0, new Vector3D(-3, 10, 30).distance(line.pointOfMinAbscissa(points)), 0.);
    }
}
