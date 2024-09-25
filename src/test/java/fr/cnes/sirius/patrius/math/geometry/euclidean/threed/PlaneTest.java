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
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.Precision;

public class PlaneTest {

    @Test
    public void testConstructors() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
        final Vector3D orig = new Vector3D(2.5, 4.6, 2.0);
        final Vector3D orig2 = new Vector3D(2.5, 4.6, 5.2);
        final Vector3D v1 = new Vector3D(3, 1, 0);
        final Vector3D v2 = new Vector3D(0, 2, 0);
        final Vector3D p1 = orig.add(v1);
        final Vector3D p2 = orig.add(v2);
        final Vector3D z = new Vector3D(0, 0, 1);

        // build test with three points compared to one point and two vectors
        Plane plane1 = new Plane(orig, v1, v2, false);
        final Plane plane2 = new Plane(orig, p1, p2);
        Assert.assertTrue(plane1.sameOrientationAs(plane2));
        Assert.assertEquals(Vector3D.dotProduct(z, plane1.getU()), 0.0, eps);

        // test of the isFrame boolean : the origin is known
        plane1 = new Plane(orig, v1, v2, true);
        Assert.assertTrue(orig.equals(plane1.getOrigin()));
        Assert.assertTrue(v1.equals(plane1.getU()));
        Assert.assertTrue(v2.equals(plane1.getV()));
        Assert.assertTrue(plane1.sameOrientationAs(plane2));

        final Plane plane3 = new Plane(orig2, v1, v2, false);
        final double offset = plane3.getOffset(plane1);
        Assert.assertEquals(offset, -3.2, eps);

        // test with parallel frame vectors
        try {
            plane1 = new Plane(orig, v1, v1, true);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected
        }

        // build test with a line and a vector
        final Line inLine = new Line(orig, orig.add(v1));
        plane1 = new Plane(inLine, v2);
        Assert.assertTrue(plane1.sameOrientationAs(plane2));
        Assert.assertEquals(Vector3D.dotProduct(z, plane1.getU()), 0.0, eps);

        // test with a vector parallel to the line
        try {
            plane1 = new Plane(inLine, v1);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected
        }

        // build test with a line and a point of space
        plane1 = new Plane(p2, inLine);
        Assert.assertTrue(plane1.sameOrientationAs(plane2));
        Assert.assertEquals(Vector3D.dotProduct(z, plane1.getU()), 0.0, eps);

        // test with a point belonging to the line
        try {
            plane1 = new Plane(p1, inLine);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected
        }
    }

    @Test
    public void testCopyResetRevert() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
        final Vector3D orig = new Vector3D(0, 0, 0);
        final Vector3D x = new Vector3D(1, 0, 0);
        final Vector3D z = new Vector3D(0, 0, 1);
        final Plane plane1 = new Plane(orig, z);

        final Plane plane2 = plane1.copySelf();
        Assert.assertTrue(orig.equals(plane2.getOrigin()));
        Assert.assertTrue(plane1.sameOrientationAs(plane2));
        Assert.assertEquals(Vector3D.dotProduct(z, plane2.getU()), 0.0, eps);

        plane1.reset(z, x);
        Assert.assertTrue(orig.equals(plane1.getOrigin()));
        Assert.assertTrue(!plane1.sameOrientationAs(plane2));
        Assert.assertEquals(Vector3D.dotProduct(x, plane1.getU()), 0.0, eps);

        plane2.reset(plane1);
        Assert.assertTrue(orig.equals(plane2.getOrigin()));
        Assert.assertTrue(plane1.sameOrientationAs(plane2));
        Assert.assertEquals(Vector3D.dotProduct(x, plane2.getU()), 0.0, eps);

        Plane plane3 = new Plane(plane1);
        Assert.assertTrue(orig.equals(plane3.getOrigin()));
        Assert.assertTrue(plane1.sameOrientationAs(plane3));
        Assert.assertTrue(plane1.isSimilarTo(plane3));
        Assert.assertEquals(Vector3D.dotProduct(x, plane3.getU()), 0.0, eps);

        plane3.revertSelf();
        Assert.assertTrue(orig.equals(plane3.getOrigin()));
        Assert.assertTrue(!plane1.sameOrientationAs(plane3));
        plane3 = plane3.translate(x);
        Assert.assertTrue(!plane1.isSimilarTo(plane3));
        Assert.assertEquals(Vector3D.dotProduct(x, plane3.getU()), 0.0, eps);
        Assert.assertEquals(Vector3D.dotProduct(x, plane3.getV()), 0.0, eps);
    }

    @Test
    public void testToSubSpace() {
        final Vector3D orig = new Vector3D(0, 0, 0);
        final Vector3D x = new Vector3D(1, 0, 0);
        final Vector3D y = new Vector3D(0, 1, 0);
        final Vector3D p = new Vector3D(1, 1, 1);
        final Plane plane1 = new Plane(orig, x, y, true);
        final Vector2D ref = new Vector2D(1, 1);

        final Vector2D res = plane1.toSubSpace(p);
        Assert.assertTrue(ref.equals(res));
    }

    @Test
    public void testToSpace() {
        final Vector3D orig = new Vector3D(1, 1, 2);
        final Vector3D x = new Vector3D(1, 0, 0);
        final Vector3D y = new Vector3D(0, 1, 0);
        final Vector2D p = new Vector2D(2, 5);
        final Plane plane1 = new Plane(orig, x, y, true);
        final Vector3D ref1 = new Vector3D(2, 5, 2);
        final Vector3D ref2 = new Vector3D(2, 5, 4);

        final Vector3D res1 = plane1.toSpace(p);
        Assert.assertTrue(ref1.equals(res1));
        final Vector3D res2 = plane1.getPointAt(p, 2);
        Assert.assertTrue(ref2.equals(res2));
    }

    @Test
    public void testContains() throws MathArithmeticException {
        final Plane p = new Plane(new Vector3D(0, 0, 1), new Vector3D(0, 0, 1));
        Assert.assertTrue(p.contains(new Vector3D(0, 0, 1)));
        Assert.assertTrue(p.contains(new Vector3D(17, -32, 1)));
        Assert.assertTrue(!p.contains(new Vector3D(17, -32, 1.001)));
    }

    @Test
    public void testOffset() throws MathArithmeticException {
        final Vector3D p1 = new Vector3D(1, 1, 1);
        final Plane p = new Plane(p1, new Vector3D(0.2, 0, 0));
        Assert.assertEquals(-5.0, p.getOffset(new Vector3D(-4, 0, 0)), 1.0e-10);
        Assert.assertEquals(+5.0, p.getOffset(new Vector3D(6, 10, -12)), 1.0e-10);
        Assert.assertEquals(0.3,
            p.getOffset(new Vector3D(1.0, p1, 0.3, p.getNormal())),
            1.0e-10);
        Assert.assertEquals(-0.3,
            p.getOffset(new Vector3D(1.0, p1, -0.3, p.getNormal())),
            1.0e-10);
    }

    @Test
    public void testPoint() throws MathArithmeticException {
        final Plane p = new Plane(new Vector3D(2, -3, 1), new Vector3D(1, 4, 9));
        Assert.assertTrue(p.contains(p.getOrigin()));
    }

    @Test
    public void testThreePoints() throws MathArithmeticException {
        final Vector3D p1 = new Vector3D(1.2, 3.4, -5.8);
        final Vector3D p2 = new Vector3D(3.4, -5.8, 1.2);
        final Vector3D p3 = new Vector3D(-2.0, 4.3, 0.7);
        final Plane p = new Plane(p1, p2, p3);
        Assert.assertTrue(p.contains(p1));
        Assert.assertTrue(p.contains(p2));
        Assert.assertTrue(p.contains(p3));
    }

    @Test
    public void testRotate() throws MathArithmeticException, MathIllegalArgumentException {
        final Vector3D p1 = new Vector3D(1.2, 3.4, -5.8);
        final Vector3D p2 = new Vector3D(3.4, -5.8, 1.2);
        final Vector3D p3 = new Vector3D(-2.0, 4.3, 0.7);
        Plane p = new Plane(p1, p2, p3);
        final Vector3D oldNormal = p.getNormal();

        p = p.rotate(p2, new Rotation(p2.subtract(p1), 1.7));
        Assert.assertTrue(p.contains(p1));
        Assert.assertTrue(p.contains(p2));
        Assert.assertTrue(!p.contains(p3));

        p = p.rotate(p2, new Rotation(oldNormal, 0.1));
        Assert.assertTrue(!p.contains(p1));
        Assert.assertTrue(p.contains(p2));
        Assert.assertTrue(!p.contains(p3));

        p = p.rotate(p1, new Rotation(oldNormal, 0.1));
        Assert.assertTrue(!p.contains(p1));
        Assert.assertTrue(!p.contains(p2));
        Assert.assertTrue(!p.contains(p3));

    }

    @Test
    public void testTranslate() throws MathArithmeticException {
        final Vector3D p1 = new Vector3D(1.2, 3.4, -5.8);
        final Vector3D p2 = new Vector3D(3.4, -5.8, 1.2);
        final Vector3D p3 = new Vector3D(-2.0, 4.3, 0.7);
        Plane p = new Plane(p1, p2, p3);

        p = p.translate(new Vector3D(2.0, p.getU(), -1.5, p.getV()));
        Assert.assertTrue(p.contains(p1));
        Assert.assertTrue(p.contains(p2));
        Assert.assertTrue(p.contains(p3));

        p = p.translate(new Vector3D(-1.2, p.getNormal()));
        Assert.assertTrue(!p.contains(p1));
        Assert.assertTrue(!p.contains(p2));
        Assert.assertTrue(!p.contains(p3));

        p = p.translate(new Vector3D(+1.2, p.getNormal()));
        Assert.assertTrue(p.contains(p1));
        Assert.assertTrue(p.contains(p2));
        Assert.assertTrue(p.contains(p3));

    }

    @Test
    public void testIntersection() throws MathArithmeticException, MathIllegalArgumentException {
        final Plane p = new Plane(new Vector3D(1, 2, 3), new Vector3D(-4, 1, -5));
        final Line l = new Line(new Vector3D(0.2, -3.5, 0.7), new Vector3D(1.2, -2.5, -0.3));
        final Vector3D point = p.intersection(l);
        Assert.assertTrue(p.contains(point));
        Assert.assertTrue(l.contains(point));
        Assert.assertNull(p.intersection(new Line(new Vector3D(10, 10, 10),
            new Vector3D(10, 10, 10).add(p.getNormal().orthogonal()))));
    }

    @Test
    public void testIntersection2() throws MathArithmeticException {
        final Vector3D p1 = new Vector3D(1.2, 3.4, -5.8);
        final Vector3D p2 = new Vector3D(3.4, -5.8, 1.2);
        final Plane pA = new Plane(p1, p2, new Vector3D(-2.0, 4.3, 0.7));
        final Plane pB = new Plane(p1, new Vector3D(11.4, -3.8, 5.1), p2);
        final Line l = pA.intersection(pB);
        Assert.assertTrue(l.contains(p1));
        Assert.assertTrue(l.contains(p2));
        Assert.assertNull(pA.intersection(pA));
    }

    @Test
    public void testIntersection3() throws MathArithmeticException {
        final Vector3D reference = new Vector3D(1.2, 3.4, -5.8);
        final Plane p1 = new Plane(reference, new Vector3D(1, 3, 3));
        final Plane p2 = new Plane(reference, new Vector3D(-2, 4, 0));
        final Plane p3 = new Plane(reference, new Vector3D(7, 0, -4));
        final Vector3D p = Plane.intersection(p1, p2, p3);
        Assert.assertEquals(reference.getX(), p.getX(), 1.0e-10);
        Assert.assertEquals(reference.getY(), p.getY(), 1.0e-10);
        Assert.assertEquals(reference.getZ(), p.getZ(), 1.0e-10);
    }

    @Test
    public void testIntersection4() {
        final Vector3D v1 = new Vector3D(1.2, 3.4, -5.8);
        final Vector3D v2 = new Vector3D(3.4, -5.8, 1.2);
        final Plane p1 = new Plane(v1, new Vector3D(1, 3, 3));
        final Plane p2 = new Plane(v2, new Vector3D(1, 3, 3));
        final Plane p3 = new Plane(v2, new Vector3D(7, 0, -4));
        final Vector3D res = Plane.intersection(p1, p2, p3);
        Assert.assertNull(res);
    }

    @Test
    public void testSimilar() throws MathArithmeticException {
        final Vector3D p1 = new Vector3D(1.2, 3.4, -5.8);
        final Vector3D p2 = new Vector3D(3.4, -5.8, 1.2);
        final Vector3D p3 = new Vector3D(-2.0, 4.3, 0.7);
        final Plane pA = new Plane(p1, p2, p3);
        final Plane pB = new Plane(p1, new Vector3D(11.4, -3.8, 5.1), p2);
        Assert.assertTrue(!pA.isSimilarTo(pB));
        Assert.assertTrue(pA.isSimilarTo(pA));
        Assert.assertTrue(pA.isSimilarTo(new Plane(p1, p3, p2)));
        final Vector3D shift = new Vector3D(0.3, pA.getNormal());
        Assert.assertTrue(!pA.isSimilarTo(new Plane(p1.add(shift),
            p3.add(shift),
            p2.add(shift))));
    }

    @Test
    public void testDistanceToPoint() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
        final Vector3D x = new Vector3D(1, 0, 0);
        final Vector3D y = new Vector3D(0, 1, 0);
        final Vector3D z = new Vector3D(0, 0, 1);
        final Vector3D pointIn = new Vector3D(1 / 3.0, new Vector3D(1, 1, 1));

        // test with a point belonging to the plane
        final Plane plane = new Plane(x, y, z);
        double res = plane.distanceTo(pointIn);
        Assert.assertEquals(res, 0.0, eps);

        // test with a point out of the plane
        res = plane.distanceTo(new Vector3D(1, 1, 1));
        final double ref = Math.sqrt(3.0) - 1.0 / Math.sqrt(3.0);
        Assert.assertEquals(res, ref, eps);
    }

    @Test
    public void testDistanceToLine() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
        final Vector3D x = new Vector3D(1, 0, 0);
        final Vector3D y = new Vector3D(0, 1, 0);
        final Vector3D z = new Vector3D(0, 0, 1);
        final Plane plane = new Plane(x, y, z);

        // test with a line parallel to the plane
        final Vector3D orig = new Vector3D(1, -1, 3);
        Vector3D dir = new Vector3D(0, -1, 1);
        final Line line1 = new Line(orig, orig.add(dir));
        final double res1 = plane.distanceTo(line1);
        final double ref1 = Math.sqrt(3.0) - 1.0 / Math.sqrt(3.0);
        Assert.assertEquals(res1, ref1, eps);

        // test with a line intersecting the plane
        dir = new Vector3D(0, -2, 1);
        final Line line2 = new Line(orig, orig.add(dir));
        final double res2 = plane.distanceTo(line2);
        Assert.assertEquals(res2, 0.0, eps);
    }

    @Test
    public void testIntersects() {
        final Vector3D x = new Vector3D(1, 0, 0);
        final Vector3D y = new Vector3D(0, 1, 0);
        final Vector3D z = new Vector3D(0, 0, 1);
        final Plane plane = new Plane(x, y, z);

        // test with a line parallel to the plane
        final Vector3D orig = new Vector3D(1, -1, 3);
        Vector3D dir = new Vector3D(0, -1, 1);
        final Line line1 = new Line(orig, orig.add(dir));
        boolean inter = plane.intersects(line1);
        Assert.assertTrue(!inter);

        // test with a line intersecting the plane
        dir = new Vector3D(0, -2, 1);
        final Line line2 = new Line(orig, orig.add(dir));
        inter = plane.intersects(line2);
        Assert.assertTrue(inter);

    }

    @Test
    public void testIntersectionsLine() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;
        final Vector3D x = new Vector3D(1, 0, 0);
        final Vector3D y = new Vector3D(0, 1, 0);
        final Vector3D z = new Vector3D(0, 0, 1);
        final Plane plane = new Plane(x, y, z);

        // test with a line parallel to the plane
        final Vector3D orig = new Vector3D(1, -1, 3);
        Vector3D dir = new Vector3D(0, -1, 1);
        final Line line1 = new Line(orig, orig.add(dir));
        Vector3D[] res = plane.getIntersectionPoints(line1);
        Assert.assertEquals(0, res.length);

        // test with a line intersecting the plane
        dir = new Vector3D(0, -2, 1);
        final Line line2 = new Line(orig, orig.add(dir));
        res = plane.getIntersectionPoints(line2);

        Assert.assertEquals(1, res[0].getX(), eps);
        Assert.assertEquals(-5, res[0].getY(), eps);
        Assert.assertEquals(5, res[0].getZ(), eps);
    }

    @Test
    public void testClosestPointToLine() {
        final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

        final Vector3D x = new Vector3D(1, 0, 1);
        final Vector3D y = new Vector3D(0, 1, 1);
        final Vector3D z = new Vector3D(0, 0, 1);
        final Plane plane = new Plane(x, y, z);

        // test with a line parallel to the plane
        final Vector3D orig = new Vector3D(0, 0, 3);
        Vector3D dir = new Vector3D(0, -1, 0);
        final Line line = new Line(orig, orig.add(dir));
        Vector3D[] res = plane.closestPointTo(line);

        Assert.assertEquals(0, res[0].getX(), eps);
        Assert.assertEquals(0, res[0].getY(), eps);
        Assert.assertEquals(3, res[0].getZ(), eps);

        Assert.assertEquals(0, res[1].getX(), eps);
        Assert.assertEquals(0, res[1].getY(), eps);
        Assert.assertEquals(1, res[1].getZ(), eps);

        // test with a line intersecting the plane
        dir = new Vector3D(1, 1, -1);
        final Line line2 = new Line(orig, orig.add(dir));
        res = plane.closestPointTo(line2);

        Assert.assertEquals(2, res[0].getX(), eps);
        Assert.assertEquals(2, res[0].getY(), eps);
        Assert.assertEquals(1, res[0].getZ(), eps);

        Assert.assertEquals(2, res[1].getX(), eps);
        Assert.assertEquals(2, res[1].getY(), eps);
        Assert.assertEquals(1, res[1].getZ(), eps);
    }

}
