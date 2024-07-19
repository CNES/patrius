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
 * VERSION::FA:210:10/03/2014:Corrected slerp method to return quickest path
 * VERSION::FA:306:12/11/2014: coverage
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:342:05/03/2015:No exceptions thrown for singular Euler or Cardan angles: testSingularities() replaced
 * VERSION::FA:422:29/04/2015:Exception thrown if quaternion norm is 0
 * VERSION::DM:524:10/03/2016:Serialization test
 * VERSION::FA:608:29/07/2016:NaN produced by slerp() method
 * VERSION::FA:765:02/01/2017:Bad angles retrieved by getAngles()
 * VERSION::FA:1474:15/03/2018: Unnecessary computation in the constructor
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class RotationTest {

    /**
     * Test FA-2398: rotation with norm of rotation axis < Precision.EPSILON should not return an exception 
     */
    @Test
    public void testFA2398() {
        try {
            final Rotation rotation = Rotation.IDENTITY;
            final Vector3D rate = new Vector3D(MathLib.nextAfter(Precision.EPSILON, 0), 0, 0);
            final AngularCoordinates angularCoordinates = new AngularCoordinates(rotation, rate);
            final AngularCoordinates actual = angularCoordinates.shiftedBy(0);
            // Expected
            Assert.assertEquals(0., Rotation.distance(actual.getRotation(), Rotation.IDENTITY), 0.);
        } catch (final MathIllegalArgumentException e) {
            // Not supposed to happen
            Assert.fail();
        }
    }

    /**
     * Corrects a computation error in slerp whereby the computed path was not the shortest.
     * 
     * Initial rotation :
     * ------------------
     * Rotation{-0.12885996016041334,0.0,0.0,-0.991662800889222}
     * {0; 0; -1}
     * 165.19256249987413
     * 
     * Final rotation :
     * ----------------
     * Rotation{-9.462505025822089E-4,0.0,0.0,0.9999995523048929}
     * {0; 0; 1}
     * 179.89156766349828
     * 
     * h value :
     * ---------
     * h = 0.2
     * 
     * faulty slerp :
     * --------------
     * Rotation{-0.6679901404810177,0.0,0.0,-0.7441701231708718}
     * {0; 0; -1}
     * 96.17573646719948
     * q0 = -0.6679901404810177
     * q1 = 0.0
     * q2 = 0.0
     * q3 = -0.7441701231708718
     * 
     * corrected slerp :
     * -----------------
     * Rotation{-0.10300315212753876,0.0,0.0,-0.9946810296028528}
     * {0; 0; -1}
     * 168.17573646719973
     * q0 = -0.10300315212753876
     * q1 = 0.0
     * q2 = 0.0
     * q3 = -0.9946810296028528
     * 
     * @throws InterruptedException
     */
    @Test
    public void testShortestPath() throws InterruptedException {

        // initial rotation
        final Quaternion quat1 = new Quaternion(-0.12994332352173027, new double[] { 0, 0, -1 });
        final Rotation r1 = new Rotation(false, quat1.normalize());

        // final rotation
        final Quaternion quat2 = new Quaternion(-9.462509262141186E-4, new double[] { 0, 0, 1 });
        final Rotation r2 = new Rotation(false, quat2.normalize());

        // slerp
        final double h = .2;
        final Rotation r = Rotation.slerp(r1, r2, h);

        // expected result :
        final double sign = r.getQuaternion().getQ0() > 0 ? 1 : -1;
        final double q0 = 0.10300315212753876 * sign;
        final double q1 = 0.0 * sign;
        final double q2 = 0.0 * sign;
        final double q3 = 0.9946810296028528 * sign;

        // asserts
        final double eps = 1e-14;
        Assert.assertEquals(q0, r.getQuaternion().getQ0(), eps);
        Assert.assertEquals(q1, r.getQuaternion().getQ1(), eps);
        Assert.assertEquals(q2, r.getQuaternion().getQ2(), eps);
        Assert.assertEquals(q3, r.getQuaternion().getQ3(), eps);
    }

    @Test
    public void testIdentity() {

        Rotation r = Rotation.IDENTITY;
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_K);
        checkAngle(r.getAngle(), 0);

        r = new Rotation(false, -1, 0, 0, 0);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_K);
        checkAngle(r.getAngle(), 0);

        r = new Rotation(true, new double[] { 42, 0, 0, 0 });
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_K);
        checkAngle(r.getAngle(), 0);
    }

    @Test
    public void testAxisAngle() throws MathIllegalArgumentException {

        Rotation r = new Rotation(new Vector3D(10, 10, 10), 2 * FastMath.PI / 3);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_J);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_K);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_I);
        final double s = 1 / MathLib.sqrt(3);
        checkVector(r.getAxis(), new Vector3D(s, s, s));
        checkAngle(r.getAngle(), 2 * FastMath.PI / 3);

        try {
            final Rotation rot = new Rotation(new Vector3D(0, 0, 0), 2 * FastMath.PI / 3);
            Assert.assertEquals(Rotation.distance(rot, Rotation.IDENTITY), 0, 0);
        } catch (final MathIllegalArgumentException e) {
            Assert.fail("an exception should not have been thrown");
        }

        r = new Rotation(Vector3D.PLUS_K, 1.5 * FastMath.PI);
        checkVector(r.getAxis(), new Vector3D(0, 0, -1));
        checkAngle(r.getAngle(), 0.5 * FastMath.PI);

        r = new Rotation(Vector3D.PLUS_J, FastMath.PI);
        checkVector(r.getAxis(), Vector3D.PLUS_J);
        checkAngle(r.getAngle(), FastMath.PI);

        checkVector(Rotation.IDENTITY.getAxis(), Vector3D.PLUS_I);

        r = new Rotation(Vector3D.MINUS_J, -FastMath.PI / 3);
        checkVector(r.getAxis(), Vector3D.PLUS_J);
        checkAngle(r.getAngle(), FastMath.PI / 3);

        r = new Rotation(Vector3D.PLUS_J, 2 * FastMath.PI + FastMath.PI / 3);
        checkVector(r.getAxis(), Vector3D.PLUS_J);
        checkAngle(r.getAngle(), FastMath.PI / 3);

        r = new Rotation(Vector3D.PLUS_J, 0);
        checkVector(r.getAxis(), Vector3D.PLUS_I);
        checkAngle(r.getAngle(), 0);
    }

    @Test
    public void testRevert() {
        final Rotation r = new Rotation(true, 0.001, 0.36, 0.48, 0.8);
        final Rotation reverted = r.revert();
        checkRotation(reverted, -0.001, 0.36, 0.48, 0.8);
        checkRotation(r.applyTo(reverted), 1, 0, 0, 0);
        checkRotation(reverted.applyTo(r), 1, 0, 0, 0);
        Assert.assertEquals(r.getAngle(), reverted.getAngle(), 1.0e-12);
        Assert.assertEquals(-1, Vector3D.dotProduct(r.getAxis(), reverted.getAxis()), 1.0e-12);
    }

    @Test
    public void testVectorOnePair() throws MathArithmeticException {

        final Vector3D u = new Vector3D(3, 2, 1);
        final Vector3D v = new Vector3D(-4, 2, 2);
        final Rotation r = new Rotation(u, v);
        checkVector(r.applyTo(u.scalarMultiply(v.getNorm())), v.scalarMultiply(u.getNorm()));

        checkAngle(new Rotation(u, u.negate()).getAngle(), FastMath.PI);

        try {
            new Rotation(u, Vector3D.ZERO);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        checkRotation(new Rotation(u, u), 1, 0, 0, 0);
    }

    @Test
    public void testVectorTwoPairs() throws MathArithmeticException {

        final Vector3D u1 = new Vector3D(3, 0, 0);
        final Vector3D u2 = new Vector3D(0, 5, 0);
        final Vector3D v1 = new Vector3D(0, 0, 2);
        final Vector3D v2 = new Vector3D(-2, 0, 2);
        Rotation r = new Rotation(u1, u2, v1, v2);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_K);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.MINUS_I);

        r = new Rotation(u1, u2, u1.negate(), u2.negate());
        final Vector3D axis = r.getAxis();
        if (Vector3D.dotProduct(axis, Vector3D.PLUS_K) > 0) {
            checkVector(axis, Vector3D.PLUS_K);
        } else {
            checkVector(axis, Vector3D.MINUS_K);
        }
        checkAngle(r.getAngle(), FastMath.PI);

        final double sqrt = MathLib.sqrt(2) / 2;
        r = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_J,
            new Vector3D(0.5, 0.5, sqrt),
            new Vector3D(0.5, 0.5, -sqrt));
        checkRotation(r, sqrt, -0.5, -0.5, 0);

        r = new Rotation(u1, u2, u1, Vector3D.crossProduct(u1, u2));
        checkRotation(r, sqrt, sqrt, 0, 0);

        checkRotation(new Rotation(u1, u2, u1, u2), 1, 0, 0, 0);

        try {
            new Rotation(u1, u2, Vector3D.ZERO, v2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        try {
            new Rotation(Vector3D.ZERO, u2, v1, v2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        try {
            new Rotation(u1, Vector3D.ZERO, v1, v2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

        try {
            new Rotation(u1, u2, v1, Vector3D.ZERO);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }
    }

    @Test
    public void testMatrix()
                            throws NotARotationMatrixException {

        try {
            new Rotation(new double[][] { { 0.0, 1.0, 0.0 }, { 1.0, 0.0, 0.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.0, 1.0 }, { 1.0, 0.0, 0.0 },
                { 0.0, 0.0, 1.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.0, 1.0, 0.0 }, { 1.0, 0.0 },
                { 0.0, 0.0, 1.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.0, 1.0, 0.0 }, { 1.0, 0.0, 0.0 },
                { 0.0, 0.0 } }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] { { 0.445888, 0.797184, -0.407040 }, { 0.821760, -0.184320, 0.539200 },
                { -0.354816, 0.574912, 0.737280 }
            }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            new Rotation(new double[][] {
                { 0.4, 0.8, -0.4 },
                { -0.4, 0.6, 0.7 },
                { 0.8, -0.2, 0.5 }
            }, 1.0e-15);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        checkRotation(new Rotation(new double[][] {
            { 0.445888, 0.797184, -0.407040 },
            { -0.354816, 0.574912, 0.737280 },
            { 0.821760, -0.184320, 0.539200 }
        }, 1.0e-10),
            0.8, -0.288, -0.384, -0.36);

        checkRotation(new Rotation(new double[][] {
            { 0.539200, 0.737280, 0.407040 },
            { 0.184320, -0.574912, 0.797184 },
            { 0.821760, -0.354816, -0.445888 }
        }, 1.0e-10),
            0.36, -0.8, -0.288, -0.384);

        checkRotation(new Rotation(new double[][] {
            { -0.445888, 0.797184, -0.407040 },
            { 0.354816, 0.574912, 0.737280 },
            { 0.821760, 0.184320, -0.539200 }
        }, 1.0e-10),
            0.384, -0.36, -0.8, -0.288);

        checkRotation(new Rotation(new double[][] {
            { -0.539200, 0.737280, 0.407040 },
            { -0.184320, -0.574912, 0.797184 },
            { 0.821760, 0.354816, 0.445888 }
        }, 1.0e-10),
            0.288, -0.384, -0.36, -0.8);

        final double[][] m1 = { { 0.0, 1.0, 0.0 },
            { 0.0, 0.0, 1.0 },
            { 1.0, 0.0, 0.0 } };
        Rotation r = new Rotation(m1, 1.0e-7);
        checkVector(r.applyTo(Vector3D.PLUS_I), Vector3D.PLUS_K);
        checkVector(r.applyTo(Vector3D.PLUS_J), Vector3D.PLUS_I);
        checkVector(r.applyTo(Vector3D.PLUS_K), Vector3D.PLUS_J);

        final double[][] m2 = { { 0.83203, -0.55012, -0.07139 },
            { 0.48293, 0.78164, -0.39474 },
            { 0.27296, 0.29396, 0.91602 } };
        r = new Rotation(m2, 1.0e-12);

        final double[][] m3 = r.getMatrix();
        final double d00 = m2[0][0] - m3[0][0];
        final double d01 = m2[0][1] - m3[0][1];
        final double d02 = m2[0][2] - m3[0][2];
        final double d10 = m2[1][0] - m3[1][0];
        final double d11 = m2[1][1] - m3[1][1];
        final double d12 = m2[1][2] - m3[1][2];
        final double d20 = m2[2][0] - m3[2][0];
        final double d21 = m2[2][1] - m3[2][1];
        final double d22 = m2[2][2] - m3[2][2];

        Assert.assertTrue(MathLib.abs(d00) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d01) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d02) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d10) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d11) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d12) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d20) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d21) < 6.0e-6);
        Assert.assertTrue(MathLib.abs(d22) < 6.0e-6);

        Assert.assertTrue(MathLib.abs(d00) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d01) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d02) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d10) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d11) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d12) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d20) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d21) > 4.0e-7);
        Assert.assertTrue(MathLib.abs(d22) > 4.0e-7);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                final double m3tm3 = m3[i][0] * m3[j][0]
                    + m3[i][1] * m3[j][1]
                    + m3[i][2] * m3[j][2];
                if (i == j) {
                    Assert.assertTrue(MathLib.abs(m3tm3 - 1.0) < 1.0e-10);
                } else {
                    Assert.assertTrue(MathLib.abs(m3tm3) < 1.0e-10);
                }
            }
        }

        checkVector(r.applyTo(Vector3D.PLUS_I),
            new Vector3D(m3[0][0], m3[1][0], m3[2][0]));
        checkVector(r.applyTo(Vector3D.PLUS_J),
            new Vector3D(m3[0][1], m3[1][1], m3[2][1]));
        checkVector(r.applyTo(Vector3D.PLUS_K),
            new Vector3D(m3[0][2], m3[1][2], m3[2][2]));

        final double[][] m4 = { { 1.0, 0.0, 0.0 },
            { 0.0, -1.0, 0.0 },
            { 0.0, 0.0, -1.0 } };
        r = new Rotation(m4, 1.0e-7);
        checkAngle(r.getAngle(), FastMath.PI);

        try {
            final double[][] m5 = { { 0.0, 0.0, 1.0 },
                { 0.0, 1.0, 0.0 },
                { 1.0, 0.0, 0.0 } };
            r = new Rotation(m5, 1.0e-7);
            Assert.fail("got " + r + ", should have caught an exception");
        } catch (final NotARotationMatrixException e) {
            // expected
        }
    }

    @Test
    public void testAngles()
    {

        final RotationOrder[] CardanOrders = {
            RotationOrder.XYZ, RotationOrder.XZY, RotationOrder.YXZ,
            RotationOrder.YZX, RotationOrder.ZXY, RotationOrder.ZYX
        };

        for (final RotationOrder cardanOrder : CardanOrders) {
            for (double alpha1 = 0.1; alpha1 < 6.2; alpha1 += 0.3) {
                for (double alpha2 = -1.55; alpha2 < 1.55; alpha2 += 0.3) {
                    for (double alpha3 = 0.1; alpha3 < 6.2; alpha3 += 0.3) {
                        final Rotation r = new Rotation(cardanOrder, alpha1, alpha2, alpha3);
                        final double[] angles = r.getAngles(cardanOrder);
                        checkAngle(angles[0], alpha1);
                        checkAngle(angles[1], alpha2);
                        checkAngle(angles[2], alpha3);
                    }
                }
            }
        }

        final RotationOrder[] EulerOrders = {
            RotationOrder.XYX, RotationOrder.XZX, RotationOrder.YXY,
            RotationOrder.YZY, RotationOrder.ZXZ, RotationOrder.ZYZ
        };

        for (final RotationOrder eulerOrder : EulerOrders) {
            for (double alpha1 = 0.1; alpha1 < 6.2; alpha1 += 0.3) {
                for (double alpha2 = 0.05; alpha2 < 3.1; alpha2 += 0.3) {
                    for (double alpha3 = 0.1; alpha3 < 6.2; alpha3 += 0.3) {
                        final Rotation r = new Rotation(eulerOrder,
                            alpha1, alpha2, alpha3);
                        final double[] angles = r.getAngles(eulerOrder);
                        checkAngle(angles[0], alpha1);
                        checkAngle(angles[1], alpha2);
                        checkAngle(angles[2], alpha3);
                    }
                }
            }
        }
    }

    /**
     * Check that no exception is thrown when the angles are singular
     * This test is meant to work only if no exceptions are thrown
     */
    @Test
    public void testSingularities() {

        final Map<RotationOrder, Rotation> m = new LinkedHashMap<>();
        final double PI = FastMath.PI;
        final double PIS2 = 0.5 * FastMath.PI;

        // Rotation corresponding to singularities in Euler or Cardan Angles
        m.put(RotationOrder.XYZ, new Rotation(Vector3D.PLUS_J, PIS2));
        m.put(RotationOrder.XYX, new Rotation(Vector3D.PLUS_J, PI));
        m.put(RotationOrder.ZYZ, new Rotation(Vector3D.PLUS_J, PI));
        m.put(RotationOrder.ZYX, new Rotation(Vector3D.PLUS_J, PIS2));
        m.put(RotationOrder.YXZ, new Rotation(Vector3D.PLUS_I, PIS2));
        m.put(RotationOrder.YXY, new Rotation(Vector3D.PLUS_I, PI));
        m.put(RotationOrder.ZXZ, new Rotation(Vector3D.PLUS_I, PI));
        m.put(RotationOrder.ZXY, new Rotation(Vector3D.PLUS_I, PIS2));
        m.put(RotationOrder.XZY, new Rotation(Vector3D.PLUS_K, PIS2));
        m.put(RotationOrder.YZY, new Rotation(Vector3D.PLUS_K, PI));
        m.put(RotationOrder.XZX, new Rotation(Vector3D.PLUS_K, PI));
        m.put(RotationOrder.YZX, new Rotation(Vector3D.PLUS_K, PIS2));

        double[] angles;
        Rotation r;

        for (final Entry<RotationOrder, Rotation> entry : m.entrySet()) {

            // Transformation Rotation ==> angles
            angles = entry.getValue().getAngles(entry.getKey());
            // Verification of inverse transformation (angles ==> Rotation)
            r = new Rotation(entry.getKey(), angles[0], angles[1], angles[2]);

            // Check differences
            Assert.assertEquals(entry.getValue().applyInverseTo(r).getAngle(), 0, 1e-30);

            // Transformation of inverse rotation Rotation ==> angles
            angles = entry.getValue().revert().getAngles(entry.getKey());
            // Verification of inverse transformation (angles ==> Rotation)
            r = new Rotation(entry.getKey(), angles[0], angles[1], angles[2]);

            // Check differences
            Assert.assertEquals(entry.getValue().revert().applyInverseTo(r).getAngle(), 0, 1e-30);
        }
    }

    /**
     * FT 765 : check that angle for mono-axis rotation are well retrieved by Euler sequences.
     * Used rotations are 1) XYX sequence with (X, Y, Z) = (5.0, 0.0, 0.0) rad,
     * 2) YXY sequence with (X, Y, Z) = (5.0, 0.0, 0.0) rad,
     * 3) ZXZ sequence with (X, Y, Z) = (5.0, 0.0, 0.0) rad,
     * 
     * The following sequences are given to Rotation.getAngles(RotationOrder) :
     * 1) XYZ, XYX, XZX
     * 2) YXZ, YXY, YZY
     * 3) ZXY, ZXZ, ZYZ
     * the set of expected Euler angle for each case is (5.0, 0.0, 0.0).
     */
    @Test
    public void testBugRotation() {

        // Build the rotation : XYX sequence with X angle = 5 rad
        final Rotation rotationXYX = new Rotation(RotationOrder.XYX, MathLib.toRadians(5.0), 0.0, 0.0);
        final Vector3D anglesXYZ = new Vector3D(rotationXYX.getAngles(RotationOrder.XYZ));
        final Vector3D anglesXYX = new Vector3D(rotationXYX.getAngles(RotationOrder.XYX));
        final Vector3D anglesXZX = new Vector3D(rotationXYX.getAngles(RotationOrder.XZX));

        // Perform comparisons
        checkVector(anglesXYZ, anglesXYX);
        checkVector(anglesXYZ, anglesXZX);

        // YXY sequence with Y angle = 5 rad
        final Rotation rotationYXY = new Rotation(RotationOrder.YXY, MathLib.toRadians(5.0), 0.0, 0.0);
        final Vector3D anglesYXZ = new Vector3D(rotationYXY.getAngles(RotationOrder.YXZ));
        final Vector3D anglesYXY = new Vector3D(rotationYXY.getAngles(RotationOrder.YXY));
        final Vector3D anglesYZY = new Vector3D(rotationYXY.getAngles(RotationOrder.YZY));

        // Perform comparisons
        checkVector(anglesYXZ, anglesYXY);
        checkVector(anglesYXZ, anglesYZY);

        // ZXZ sequence with Z angle = 5 rad
        final Rotation rotationZXZ = new Rotation(RotationOrder.ZXZ, MathLib.toRadians(5.0), 0.0, 0.0);
        final Vector3D anglesZXY = new Vector3D(rotationZXZ.getAngles(RotationOrder.ZXY));
        final Vector3D anglesZXZ = new Vector3D(rotationZXZ.getAngles(RotationOrder.ZXZ));
        final Vector3D anglesZYZ = new Vector3D(rotationZXZ.getAngles(RotationOrder.ZXZ));

        // Perform comparisons
        checkVector(anglesZXY, anglesZXZ);
        checkVector(anglesZXY, anglesZYZ);
    }

    @Test
    public void testQuaternion() throws MathIllegalArgumentException {

        Rotation r1 = new Rotation(new Vector3D(2, -3, 5), 1.7);
        final double n = 23.5;
        final Rotation r2 = new Rotation(true, n * r1.getQi()[0],
            n * r1.getQi()[1], n * r1.getQi()[2],
            n * r1.getQi()[3]);
        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    checkVector(r2.applyTo(u), r1.applyTo(u));
                }
            }
        }

        r1 = new Rotation(false, 0.288, 0.384, 0.36, 0.8);
        checkRotation(r1, r1.getQi()[0], r1.getQi()[1], r1.getQi()[2], r1.getQi()[3]);

        r1 = new Rotation(false, new double[] { 0.288, 0.384, 0.36, 0.8 });
        checkRotation(r1, r1.getQi()[0], r1.getQi()[1], r1.getQi()[2], r1.getQi()[3]);
    }

    /**
     * For coverage purpose.
     * Goes through all loops of public static method slerp(Rotation, Rotation, double)
     * 
     * @throws MathIllegalArgumentException
     *         bcs Rotation
     */
    @Test
    public void testSlerp() throws MathIllegalArgumentException {

        double h = 0.5;
        final Rotation r0 = new Rotation(new Vector3D(2, -3, 5), 1.7);
        Rotation result = Rotation.slerp(r0, r0, h);
        Assert.assertEquals(r0, result);

        h = 0;
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, 0);
        result = Rotation.slerp(r0, r1, h);
        Assert.assertEquals(r0, result);

        h = 1;
        result = Rotation.slerp(r0, r1, h);
        Assert.assertEquals(r1, result);
    }

    @Test
    public void testCompose() throws MathIllegalArgumentException {

        final Rotation r1 = new Rotation(new Vector3D(2, -3, 5), 1.7);
        final Rotation r2 = new Rotation(new Vector3D(-1, 3, 2), 0.3);
        final Rotation r3 = r2.applyTo(r1);

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    checkVector(r2.applyTo(r1.applyTo(u)), r3.applyTo(u));
                }
            }
        }

    }

    @Test
    public void testComposeInverse() throws MathIllegalArgumentException {

        final Rotation r1 = new Rotation(new Vector3D(2, -3, 5), 1.7);
        final Rotation r2 = new Rotation(new Vector3D(-1, 3, 2), 0.3);
        final Rotation r3 = r2.applyInverseTo(r1);

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    checkVector(r2.applyInverseTo(r1.applyTo(u)), r3.applyTo(u));
                }
            }
        }
    }

    @Test
    public void testArray() throws MathIllegalArgumentException {

        final Rotation r = new Rotation(new Vector3D(2, -3, 5), 1.7);

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    final Vector3D v = r.applyTo(u);
                    final double[] inOut = new double[] { x, y, z };
                    r.applyTo(inOut, inOut);
                    Assert.assertEquals(v.getX(), inOut[0], 1.0e-10);
                    Assert.assertEquals(v.getY(), inOut[1], 1.0e-10);
                    Assert.assertEquals(v.getZ(), inOut[2], 1.0e-10);
                    r.applyInverseTo(inOut, inOut);
                    Assert.assertEquals(u.getX(), inOut[0], 1.0e-10);
                    Assert.assertEquals(u.getY(), inOut[1], 1.0e-10);
                    Assert.assertEquals(u.getZ(), inOut[2], 1.0e-10);
                }
            }
        }
    }

    @Test
    public void testApplyInverseTo() throws MathIllegalArgumentException {

        Rotation r = new Rotation(new Vector3D(2, -3, 5), 1.7);
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final Vector3D u = new Vector3D(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));

                checkVector(u, r.applyInverseTo(r.applyTo(u)));
                checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }

        r = Rotation.IDENTITY;
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final Vector3D u = new Vector3D(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));
                checkVector(u, r.applyInverseTo(r.applyTo(u)));
                checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }

        r = new Rotation(Vector3D.PLUS_K, FastMath.PI);
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final Vector3D u = new Vector3D(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));
                checkVector(u, r.applyInverseTo(r.applyTo(u)));
                checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }
    }

    @Test
    public void testIssue639() throws MathArithmeticException {
        final Vector3D u1 = new Vector3D(-1321008684645961.0 / 268435456.0,
            -5774608829631843.0 / 268435456.0,
            -3822921525525679.0 / 4294967296.0);
        final Vector3D u2 = new Vector3D(-5712344449280879.0 / 2097152.0,
            -2275058564560979.0 / 1048576.0,
            4423475992255071.0 / 65536.0);
        final Rotation rot = new Rotation(u1, u2, Vector3D.PLUS_I, Vector3D.PLUS_K);
        Assert.assertEquals(0.6228370359608200639829222, rot.getQi()[0], 1.0e-15);
        Assert.assertEquals(-0.0257707621456498790029987, rot.getQi()[1], 1.0e-15);
        Assert.assertEquals(0.0000000002503012255839931, rot.getQi()[2], 1.0e-15);
        Assert.assertEquals(0.7819270390861109450724902, rot.getQi()[3], 1.0e-15);
    }

    @Test
    public void testIssue801() throws MathArithmeticException {
        final Vector3D u1 = new Vector3D(0.9999988431610581, -0.0015210774290851095, 0.0);
        final Vector3D u2 = new Vector3D(0.0, 0.0, 1.0);

        final Vector3D v1 = new Vector3D(0.9999999999999999, 0.0, 0.0);
        final Vector3D v2 = new Vector3D(0.0, 0.0, -1.0);

        final Rotation quat = new Rotation(u1, u2, v1, v2);
        final double q2 = quat.getQi()[0] * quat.getQi()[0] +
            quat.getQi()[1] * quat.getQi()[1] +
            quat.getQi()[2] * quat.getQi()[2] +
            quat.getQi()[3] * quat.getQi()[3];
        Assert.assertEquals(1.0, q2, 1.0e-14);
        Assert.assertEquals(0.0, Vector3D.angle(v1, quat.applyTo(u1)), 1.0e-14);
        Assert.assertEquals(0.0, Vector3D.angle(v2, quat.applyTo(u2)), 1.0e-14);
    }

    @Test(expected = MathIllegalArgumentException.class)
    public void testNullQuaternion() throws MathIllegalArgumentException {
        new Rotation(true, 0, 0, 0, 0);
    }

    @Test
    public void testSerialization() {
        // Random test
        final Well1024a random = new Well1024a(3218326325478000321l);
        for (int i = 0; i < 1000; ++i) {
            final double x = 1000 * random.nextDouble();
            final double y = 1000 * random.nextDouble();
            final double z = 1000 * random.nextDouble();
            final double a = 1000 * random.nextDouble();
            final Vector3D vector3D = new Vector3D(x, y, z);
            final Rotation rotation = new Rotation(vector3D, a);
            final Rotation rotation2 = TestUtils.serializeAndRecover(rotation);
            rotation.isEqualTo(rotation2);
        }

        // Test constants
        final Rotation rotation2 = TestUtils.serializeAndRecover(Rotation.IDENTITY);
        Rotation.IDENTITY.isEqualTo(rotation2);

        // Test RotationOrder
        final Rotation rotation3 = new Rotation(RotationOrder.XYZ, 0.1, 0.2, 0.3);
        final Rotation rotation4 = TestUtils.serializeAndRecover(rotation3);
        rotation3.isEqualTo(rotation4);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link Rotation# slerp(Rotation, Rotation, double)}
     * 
     * @description This test covers the limit case of the slerp method in which
     *              the inputs rotations are closely equals, so that the cosinus angle between them is
     *              computed at 1 or greater than 1. This led to NaN values in both cases
     *              on the output quaternion components, but this has been corrected by adding conditions on
     *              the angle computed.
     * 
     * @input couples of rotations
     * 
     * @output output rotation computed by the slerp
     * @testPassCriteria output quaternion must have no components at NaN,
     *                   and must be strictly equal to the first one in the input pair.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void slerpLimitTest() {

        // First couple of rotations
        final double h = 0.7317629752757284;
        final Rotation rprev = new Rotation(false, 0.548071845455124, 0.8357613368813491, 4.5879426845679634E-4,
            0.03346684182193046);
        final Rotation rnext = new Rotation(false, 0.5480718454118304, 0.8357613367116979, 4.5880896674289977E-4,
            0.03346684656609817);
        final Rotation res = Rotation.slerp(rprev, rnext, h);

        // Ensure output quaternion has no components at NaN
        // and that rotation is strictly equal the rprev
        Assert.assertFalse(Double.isNaN(res.getQuaternion().getQ0()));
        Assert.assertFalse(Double.isNaN(res.getQuaternion().getQ1()));
        Assert.assertFalse(Double.isNaN(res.getQuaternion().getQ2()));
        Assert.assertFalse(Double.isNaN(res.getQuaternion().getQ3()));
        Assert.assertTrue(rprev.isEqualTo(res));

        // Second couple of rotations
        final Rotation rprev2 = new Rotation(true, 0.5480718454118304, 0.8357613367116979, 4.5880896674289977E-4,
            0.03346684656609817);
        final Rotation rnext2 = new Rotation(true, 0.5480718453683652, 0.8357613365420483, 4.588236607441529E-4,
            0.033466851313084196);
        final Rotation res2 = Rotation.slerp(rprev2, rnext2, h);
        Assert.assertFalse(Double.isNaN(res2.getQuaternion().getQ0()));
        Assert.assertFalse(Double.isNaN(res2.getQuaternion().getQ1()));
        Assert.assertFalse(Double.isNaN(res2.getQuaternion().getQ2()));
        Assert.assertFalse(Double.isNaN(res2.getQuaternion().getQ3()));
        Assert.assertTrue(rprev2.isEqualTo(res2));
    }

    private static void checkVector(final Vector3D v1, final Vector3D v2) {
        Assert.assertTrue(v1.subtract(v2).getNorm() < 1.0e-10);
    }

    private static void checkAngle(final double a1, final double a2) {
        Assert.assertEquals(a1, MathUtils.normalizeAngle(a2, a1), 1.0e-10);
    }

    private static void checkRotation(final Rotation r, final double q0, final double q1,
            final double q2, final double q3) {
        Assert.assertEquals(0, Rotation.distance(r, new Rotation(false, q0, q1, q2, q3)),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }
}
