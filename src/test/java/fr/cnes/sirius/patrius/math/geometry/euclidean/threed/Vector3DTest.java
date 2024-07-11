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
 * VERSION:4.7:DM:DM-2758:18/05/2021:Conversion de coordonnees cartesiennes en Coordonnees spheriques
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:Serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.Space;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

public class Vector3DTest {

    @Test
    public void testConstructors() throws DimensionMismatchException {
        final double r = MathLib.sqrt(2) / 2;
        this.checkVector(new Vector3D(2, new Vector3D(FastMath.PI / 3, -FastMath.PI / 4)),
            r, r * MathLib.sqrt(3), -2 * r);
        this.checkVector(new Vector3D(2, Vector3D.PLUS_I,
            -3, Vector3D.MINUS_K),
            2, 0, 3);
        this.checkVector(new Vector3D(2, Vector3D.PLUS_I,
            5, Vector3D.PLUS_J,
            -3, Vector3D.MINUS_K),
            2, 5, 3);
        this.checkVector(new Vector3D(2, Vector3D.PLUS_I,
            5, Vector3D.PLUS_J,
            5, Vector3D.MINUS_J,
            -3, Vector3D.MINUS_K),
            2, 0, 3);
        this.checkVector(new Vector3D(new double[] { 2, 5, -3 }),
            2, 5, -3);

        // creation from a RealVector test
        final double[] data = { 2.0, 0.0, 3.0 };
        final ArrayRealVector realVector = new ArrayRealVector(data);
        final Vector3D vector3D = new Vector3D(realVector);
        this.checkVector(vector3D, 2, 0, 3);
        final double[] dataRes = vector3D.getRealVector().toArray();
        Assert.assertEquals(data[0], dataRes[0], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(data[1], dataRes[1], Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(data[2], dataRes[2], Precision.DOUBLE_COMPARISON_EPSILON);
        // bad size RealVector
        try {
            final double[] data2 = { 2.0, 0.0, 3.0, 5.0 };
            final ArrayRealVector realVector2 = new ArrayRealVector(data2);
            new Vector3D(realVector2);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
        
        // From spherical coordinates
        final Vector3D expected = new Vector3D(2., 3., 4.);
        final SphericalCoordinates coord = new SphericalCoordinates(expected);
        final Vector3D actual = new Vector3D(coord);
        Assert.assertEquals(0, actual.distance(expected), 0.);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testWrongDimension() throws DimensionMismatchException {
        new Vector3D(new double[] { 2, 5 });
    }

    @Test
    public void testCoordinates() {
        final Vector3D v = new Vector3D(1, 2, 3);
        Assert.assertTrue(MathLib.abs(v.getX() - 1) < 1.0e-12);
        Assert.assertTrue(MathLib.abs(v.getY() - 2) < 1.0e-12);
        Assert.assertTrue(MathLib.abs(v.getZ() - 3) < 1.0e-12);
        final double[] coordinates = v.toArray();
        Assert.assertTrue(MathLib.abs(coordinates[0] - 1) < 1.0e-12);
        Assert.assertTrue(MathLib.abs(coordinates[1] - 2) < 1.0e-12);
        Assert.assertTrue(MathLib.abs(coordinates[2] - 3) < 1.0e-12);
    }

    @Test
    public void testNorm1() {
        Assert.assertEquals(0.0, Vector3D.ZERO.getNorm1(), 0);
        Assert.assertEquals(6.0, new Vector3D(1, -2, 3).getNorm1(), 0);
    }

    @Test
    public void testNorm() {
        Assert.assertEquals(0.0, Vector3D.ZERO.getNorm(), 0);
        Assert.assertEquals(MathLib.sqrt(14), new Vector3D(1, 2, 3).getNorm(), 1.0e-12);
    }

    @Test
    public void testNormInf() {
        Assert.assertEquals(0.0, Vector3D.ZERO.getNormInf(), 0);
        Assert.assertEquals(3.0, new Vector3D(1, -2, 3).getNormInf(), 0);
    }

    @Test
    public void testDistance1() {
        final Vector3D v1 = new Vector3D(1, -2, 3);
        final Vector3D v2 = new Vector3D(-4, 2, 0);
        Assert.assertEquals(0.0, Vector3D.distance1(Vector3D.MINUS_I, Vector3D.MINUS_I), 0);
        Assert.assertEquals(12.0, Vector3D.distance1(v1, v2), 1.0e-12);
        Assert.assertEquals(v1.subtract(v2).getNorm1(), Vector3D.distance1(v1, v2), 1.0e-12);
    }

    @Test
    public void testDistance() {
        final Vector3D v1 = new Vector3D(1, -2, 3);
        final Vector3D v2 = new Vector3D(-4, 2, 0);
        Assert.assertEquals(0.0, Vector3D.distance(Vector3D.MINUS_I, Vector3D.MINUS_I), 0);
        Assert.assertEquals(MathLib.sqrt(50), Vector3D.distance(v1, v2), 1.0e-12);
        Assert.assertEquals(v1.subtract(v2).getNorm(), Vector3D.distance(v1, v2), 1.0e-12);
    }

    @Test
    public void testDistanceSq() {
        final Vector3D v1 = new Vector3D(1, -2, 3);
        final Vector3D v2 = new Vector3D(-4, 2, 0);
        Assert.assertEquals(0.0, Vector3D.distanceSq(Vector3D.MINUS_I, Vector3D.MINUS_I), 0);
        Assert.assertEquals(50.0, Vector3D.distanceSq(v1, v2), 1.0e-12);
        Assert.assertEquals(Vector3D.distance(v1, v2) * Vector3D.distance(v1, v2),
            Vector3D.distanceSq(v1, v2), 1.0e-12);
    }

    @Test
    public void testDistanceInf() {
        final Vector3D v1 = new Vector3D(1, -2, 3);
        final Vector3D v2 = new Vector3D(-4, 2, 0);
        Assert.assertEquals(0.0, Vector3D.distanceInf(Vector3D.MINUS_I, Vector3D.MINUS_I), 0);
        Assert.assertEquals(5.0, Vector3D.distanceInf(v1, v2), 1.0e-12);
        Assert.assertEquals(v1.subtract(v2).getNormInf(), Vector3D.distanceInf(v1, v2), 1.0e-12);
    }

    @Test
    public void testSubtract() {
        Vector3D v1 = new Vector3D(1, 2, 3);
        final Vector3D v2 = new Vector3D(-3, -2, -1);
        v1 = v1.subtract(v2);
        this.checkVector(v1, 4, 4, 4);

        this.checkVector(v2.subtract(v1), -7, -6, -5);
        this.checkVector(v2.subtract(3, v1), -15, -14, -13);
    }

    @Test
    public void testAdd() {
        Vector3D v1 = new Vector3D(1, 2, 3);
        final Vector3D v2 = new Vector3D(-3, -2, -1);
        v1 = v1.add(v2);
        this.checkVector(v1, -2, 0, 2);

        this.checkVector(v2.add(v1), -5, -2, 1);
        this.checkVector(v2.add(3, v1), -9, -2, 5);
    }

    @Test
    public void testScalarProduct() {
        Vector3D v = new Vector3D(1, 2, 3);
        v = v.scalarMultiply(3);
        this.checkVector(v, 3, 6, 9);

        this.checkVector(v.scalarMultiply(0.5), 1.5, 3, 4.5);
    }

    @Test
    public void testVectorialProducts() {
        final Vector3D v1 = new Vector3D(2, 1, -4);
        final Vector3D v2 = new Vector3D(3, 1, -1);

        Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(v1, v2) - 11) < 1.0e-12);

        final Vector3D v3 = Vector3D.crossProduct(v1, v2);
        this.checkVector(v3, 3, -10, -1);

        Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(v1, v3)) < 1.0e-12);
        Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(v2, v3)) < 1.0e-12);
    }

    @Test
    public void testCrossProductCancellation() {
        final Vector3D v1 = new Vector3D(9070467121.0, 4535233560.0, 1);
        final Vector3D v2 = new Vector3D(9070467123.0, 4535233561.0, 1);
        this.checkVector(Vector3D.crossProduct(v1, v2), -1, 2, 1);

        final double scale = MathLib.scalb(1.0, 100);
        final Vector3D big1 = new Vector3D(scale, v1);
        final Vector3D small2 = new Vector3D(1 / scale, v2);
        this.checkVector(Vector3D.crossProduct(big1, small2), -1, 2, 1);

        final Vector3D v3 = new Vector3D(Precision.SAFE_MIN, 0., 0.);
        final Vector3D v4 = new Vector3D(0., Precision.SAFE_MIN, 0.);
        this.checkVector(Vector3D.crossProduct(v3, v4), 0., 0., 0.);
    }

    @Test
    public void testAngular() {
        Assert.assertEquals(0, Vector3D.PLUS_I.getAlpha(), 1.0e-10);
        Assert.assertEquals(0, Vector3D.PLUS_I.getDelta(), 1.0e-10);
        Assert.assertEquals(FastMath.PI / 2, Vector3D.PLUS_J.getAlpha(), 1.0e-10);
        Assert.assertEquals(0, Vector3D.PLUS_J.getDelta(), 1.0e-10);
        Assert.assertEquals(0, Vector3D.PLUS_K.getAlpha(), 1.0e-10);
        Assert.assertEquals(FastMath.PI / 2, Vector3D.PLUS_K.getDelta(), 1.0e-10);

        final Vector3D u = new Vector3D(-1, 1, -1);
        Assert.assertEquals(3 * FastMath.PI / 4, u.getAlpha(), 1.0e-10);
        Assert.assertEquals(-1.0 / MathLib.sqrt(3), MathLib.sin(u.getDelta()), 1.0e-10);
    }

    @Test
    public void testAngularSeparation() throws MathArithmeticException {
        final Vector3D v1 = new Vector3D(2, -1, 4);

        final Vector3D k = v1.normalize();
        final Vector3D i = k.orthogonal();
        final Vector3D v2 = k.scalarMultiply(MathLib.cos(1.2)).add(i.scalarMultiply(MathLib.sin(1.2)));

        Assert.assertTrue(MathLib.abs(Vector3D.angle(v1, v2) - 1.2) < 1.0e-12);
    }

    @Test
    public void testNormalize() throws MathArithmeticException {
        Assert.assertEquals(1.0, new Vector3D(5, -4, 2).normalize().getNorm(), 1.0e-12);
        try {
            Vector3D.ZERO.normalize();
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException ae) {
            // expected behavior
        }
    }

    @Test
    public void testOrthogonal() throws MathArithmeticException {
        final Vector3D v1 = new Vector3D(0.1, 2.5, 1.3);
        Assert.assertEquals(0.0, Vector3D.dotProduct(v1, v1.orthogonal()), 1.0e-12);
        final Vector3D v2 = new Vector3D(2.3, -0.003, 7.6);
        Assert.assertEquals(0.0, Vector3D.dotProduct(v2, v2.orthogonal()), 1.0e-12);
        final Vector3D v3 = new Vector3D(-1.7, 1.4, 0.2);
        Assert.assertEquals(0.0, Vector3D.dotProduct(v3, v3.orthogonal()), 1.0e-12);
        final Vector3D v4 = new Vector3D(MathLib.sqrt(2.), 0., MathLib.sqrt(2.));
        Assert.assertEquals(0.0, Vector3D.dotProduct(v4, v4.orthogonal()), 1.0e-12);
        try {
            new Vector3D(0, 0, 0).orthogonal();
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException ae) {
            // expected behavior
        }
    }

    @Test
    public void testAngle() throws MathArithmeticException {
        Assert.assertEquals(0.22572612855273393616,
            Vector3D.angle(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6)),
            1.0e-12);
        Assert.assertEquals(7.98595620686106654517199e-8,
            Vector3D.angle(new Vector3D(1, 2, 3), new Vector3D(2, 4, 6.000001)),
            1.0e-12);
        Assert.assertEquals(3.14159257373023116985197793156,
            Vector3D.angle(new Vector3D(1, 2, 3), new Vector3D(-2, -4, -6.000001)),
            1.0e-12);
        try {
            Vector3D.angle(Vector3D.ZERO, Vector3D.PLUS_I);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException ae) {
            // expected behavior
        }
    }

    @Test
    public void testGetSpace() {
        final Vector3D v = new Vector3D(1., -1., 1.);
        final Space s = v.getSpace();
        final String expected = "fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Euclidean3D";
        final String actual = s.toString().substring(0, 65);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetZero() {
        final Vector3D v = new Vector3D(1., -1., 1.);
        final Vector3D zero = v.getZero();
        this.checkVector(zero, 0., 0., 0.);
    }

    @Test
    public void testNegate() {
        final Vector3D v = new Vector3D(1., 2., 3.);
        final Vector3D nv = v.negate();
        this.checkVector(nv, -1., -2., -3.);
    }

    @Test
    public void testIsNaN() {
        final Vector3D v1 = new Vector3D(Double.NaN, 1., 1.);
        Assert.assertTrue(v1.isNaN());
        final Vector3D v2 = new Vector3D(1., Double.NaN, 1.);
        Assert.assertTrue(v2.isNaN());
        final Vector3D v3 = new Vector3D(1., 1., Double.NaN);
        Assert.assertTrue(v3.isNaN());
    }

    @Test
    public void testIsInfinite() {
        final Vector3D v1 = new Vector3D(Double.POSITIVE_INFINITY, 1., 1.);
        Assert.assertTrue(v1.isInfinite());
        final Vector3D v2 = new Vector3D(1., Double.POSITIVE_INFINITY, 1.);
        Assert.assertTrue(v2.isInfinite());
        final Vector3D v3 = new Vector3D(1., 1., Double.POSITIVE_INFINITY);
        Assert.assertTrue(v3.isInfinite());
    }

    @Test
    public void testEquals() {
        final Vector3D v1 = new Vector3D(1., 1., 2.);
        final Vector3D v2 = new Vector3D(1., 1., 2.);
        final Vector3D v3 = new Vector3D(-1., 1., Double.NaN);
        final Vector3D v5 = new Vector3D(1., 1., 3.);
        final Vector3D v6 = new Vector3D(1., 3., 2.);
        final Vector3D v7 = new Vector3D(3., 1., 2.);
        final double d[] = { -1., 1., 2. };
        final ArrayRealVector v4 = new ArrayRealVector(d);
        Assert.assertTrue(v1.equals(v1));
        Assert.assertTrue(v1.equals(v2));
        Assert.assertFalse(v1.equals(v3));
        Assert.assertFalse(v1.equals(v4));
        Assert.assertFalse(v1.equals(v5));
        Assert.assertFalse(v1.equals(v6));
        Assert.assertFalse(v1.equals(v7));
    }

    @Test
    public void testToString() {
        final Vector3D v = new Vector3D(1., 2., 3.);
        final String expected = "{1; 2; 3}";
        final String actual = v.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToStringFormat() {
        final Vector3D v = new Vector3D(1., 2., 3.);
        final NumberFormat formatter = new DecimalFormat("000");
        final String expected = "{001; 002; 003}";
        final String actual = v.toString(formatter);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testHashCode() {
        final Vector3D v1 = new Vector3D(1., 2., 3.);
        final Vector3D v2 = new Vector3D(Double.NaN, 2., 3.);
        int expected, actual;
        expected = 1431830528;
        actual = v1.hashCode();
        Assert.assertEquals(expected, actual);
        expected = 642;
        actual = v2.hashCode();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAccurateDotProduct() {
        // the following two vectors are nearly but not exactly orthogonal
        // naive dot product (i.e. computing u1.x * u2.x + u1.y * u2.y + u1.z * u2.z
        // leads to a result of 0.0, instead of the correct -1.855129...
        final Vector3D u1 = new Vector3D(-1321008684645961.0 / 268435456.0,
            -5774608829631843.0 / 268435456.0,
            -7645843051051357.0 / 8589934592.0);
        final Vector3D u2 = new Vector3D(-5712344449280879.0 / 2097152.0,
            -4550117129121957.0 / 2097152.0,
            8846951984510141.0 / 131072.0);
        final double sNaive = u1.getX() * u2.getX() + u1.getY() * u2.getY() + u1.getZ() * u2.getZ();
        final double sAccurate = u1.dotProduct(u2);
        Assert.assertEquals(0.0, sNaive, 1.0e-30);
        Assert.assertEquals(-2088690039198397.0 / 1125899906842624.0, sAccurate, 1.0e-16);
    }

    @Test
    public void testDotProduct() {
        // we compare accurate versus naive dot product implementations
        // on regular vectors (i.e. not extreme cases like in the previous test)
        final Well1024a random = new Well1024a(553267312521321234l);
        for (int i = 0; i < 10000; ++i) {
            final double ux = 10000 * random.nextDouble();
            final double uy = 10000 * random.nextDouble();
            final double uz = 10000 * random.nextDouble();
            final double vx = 10000 * random.nextDouble();
            final double vy = 10000 * random.nextDouble();
            final double vz = 10000 * random.nextDouble();
            final double sNaive = ux * vx + uy * vy + uz * vz;
            final double sAccurate = new Vector3D(ux, uy, uz).dotProduct(new Vector3D(vx, vy, vz));
            Assert.assertEquals(sNaive, sAccurate, 2.5e-16 * sAccurate);
        }
    }

    @Test
    public void testAccurateCrossProduct() {
        // the vectors u1 and u2 are nearly but not exactly anti-parallel
        // (7.31e-16 degrees from 180 degrees) naive cross product (i.e.
        // computing u1.x * u2.x + u1.y * u2.y + u1.z * u2.z
        // leads to a result of [0.0009765, -0.0001220, -0.0039062],
        // instead of the correct [0.0006913, -0.0001254, -0.0007909]
        final Vector3D u1 = new Vector3D(-1321008684645961.0 / 268435456.0,
            -5774608829631843.0 / 268435456.0,
            -7645843051051357.0 / 8589934592.0);
        final Vector3D u2 = new Vector3D(1796571811118507.0 / 2147483648.0,
            7853468008299307.0 / 2147483648.0,
            2599586637357461.0 / 17179869184.0);
        final Vector3D u3 = new Vector3D(12753243807587107.0 / 18446744073709551616.0,
            -2313766922703915.0 / 18446744073709551616.0,
            -227970081415313.0 / 288230376151711744.0);
        final Vector3D cNaive = new Vector3D(u1.getY() * u2.getZ() - u1.getZ() * u2.getY(),
            u1.getZ() * u2.getX() - u1.getX() * u2.getZ(),
            u1.getX() * u2.getY() - u1.getY() * u2.getX());
        final Vector3D cAccurate = u1.crossProduct(u2);
        Assert.assertTrue(u3.distance(cNaive) > 2.9 * u3.getNorm());
        Assert.assertEquals(0.0, u3.distance(cAccurate), 1.0e-30 * cAccurate.getNorm());
    }

    @Test
    public void testCrossProduct() {
        // we compare accurate versus naive cross product implementations
        // on regular vectors (i.e. not extreme cases like in the previous test)
        final Well1024a random = new Well1024a(885362227452043214l);
        for (int i = 0; i < 10000; ++i) {
            final double ux = 10000 * random.nextDouble();
            final double uy = 10000 * random.nextDouble();
            final double uz = 10000 * random.nextDouble();
            final double vx = 10000 * random.nextDouble();
            final double vy = 10000 * random.nextDouble();
            final double vz = 10000 * random.nextDouble();
            final Vector3D cNaive = new Vector3D(uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx);
            final Vector3D cAccurate = new Vector3D(ux, uy, uz).crossProduct(new Vector3D(vx, vy, vz));
            Assert.assertEquals(0.0, cAccurate.distance(cNaive), 6.0e-15 * cAccurate.getNorm());
        }
    }

    @Test
    public void testSerialization() {
        // Random test
        final Well1024a random = new Well1024a(305632124547806521l);
        for (int i = 0; i < 1000; ++i) {
            final double x = 10000 * random.nextDouble();
            final double y = 10000 * random.nextDouble();
            final double z = 10000 * random.nextDouble();
            final Vector3D vector3D = new Vector3D(x, y, z);
            TestUtils.checkSerializedEquality(vector3D);
        }

        // Test constants
        TestUtils.checkSerializedEquality(Vector3D.NaN);
        TestUtils.checkSerializedEquality(Vector3D.ZERO);
        TestUtils.checkSerializedEquality(Vector3D.PLUS_I);
        TestUtils.checkSerializedEquality(Vector3D.MINUS_I);
        TestUtils.checkSerializedEquality(Vector3D.PLUS_J);
        TestUtils.checkSerializedEquality(Vector3D.MINUS_J);
        TestUtils.checkSerializedEquality(Vector3D.PLUS_K);
        TestUtils.checkSerializedEquality(Vector3D.MINUS_K);
        TestUtils.checkSerializedEquality(Vector3D.POSITIVE_INFINITY);
        TestUtils.checkSerializedEquality(Vector3D.NEGATIVE_INFINITY);
    }

    private void checkVector(final Vector3D v, final double x, final double y, final double z) {
        Assert.assertEquals(x, v.getX(), 1.0e-12);
        Assert.assertEquals(y, v.getY(), 1.0e-12);
        Assert.assertEquals(z, v.getZ(), 1.0e-12);
    }
}
