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

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.random.UnitSphereRandomVectorGenerator;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

public class FieldRotationDSTest {

    @Test
    public void testIdentity() {

        FieldRotation<DerivativeStructure> r = this.createRotation(1, 0, 0, 0, false);
        this.checkVector(r.applyTo(this.createVector(1, 0, 0)), this.createVector(1, 0, 0));
        this.checkVector(r.applyTo(this.createVector(0, 1, 0)), this.createVector(0, 1, 0));
        this.checkVector(r.applyTo(this.createVector(0, 0, 1)), this.createVector(0, 0, 1));
        this.checkAngle(r.getAngle(), 0);

        r = this.createRotation(-1, 0, 0, 0, false);
        this.checkVector(r.applyTo(this.createVector(1, 0, 0)), this.createVector(1, 0, 0));
        this.checkVector(r.applyTo(this.createVector(0, 1, 0)), this.createVector(0, 1, 0));
        this.checkVector(r.applyTo(this.createVector(0, 0, 1)), this.createVector(0, 0, 1));
        this.checkAngle(r.getAngle(), 0);

        r = this.createRotation(42, 0, 0, 0, true);
        this.checkVector(r.applyTo(this.createVector(1, 0, 0)), this.createVector(1, 0, 0));
        this.checkVector(r.applyTo(this.createVector(0, 1, 0)), this.createVector(0, 1, 0));
        this.checkVector(r.applyTo(this.createVector(0, 0, 1)), this.createVector(0, 0, 1));
        this.checkAngle(r.getAngle(), 0);

    }

    @Test
    public void testBadConstruction() throws MathIllegalArgumentException {
        try {
            this.createRotation(0, 0, 0, 0, false);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAxisAngle() throws MathIllegalArgumentException {

        FieldRotation<DerivativeStructure> r = new FieldRotation<>(this.createAxis(10, 10, 10),
            this.createAngle(2 * FastMath.PI / 3));
        this.checkVector(r.applyTo(this.createVector(1, 0, 0)), this.createVector(0, 1, 0));
        this.checkVector(r.applyTo(this.createVector(0, 1, 0)), this.createVector(0, 0, 1));
        this.checkVector(r.applyTo(this.createVector(0, 0, 1)), this.createVector(1, 0, 0));
        final double s = 1 / MathLib.sqrt(3);
        this.checkVector(r.getAxis(), this.createVector(s, s, s));
        this.checkAngle(r.getAngle(), 2 * FastMath.PI / 3);

        try {
            new FieldRotation<>(this.createAxis(0, 0, 0), this.createAngle(2 * FastMath.PI / 3));
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException e) {
            // expected
        }

        r = new FieldRotation<>(this.createAxis(0, 0, 1), this.createAngle(1.5 * FastMath.PI));
        this.checkVector(r.getAxis(), this.createVector(0, 0, -1));
        this.checkAngle(r.getAngle(), 0.5 * FastMath.PI);

        r = new FieldRotation<>(this.createAxis(0, 1, 0), this.createAngle(FastMath.PI));
        this.checkVector(r.getAxis(), this.createVector(0, 1, 0));
        this.checkAngle(r.getAngle(), FastMath.PI);
        final double angle = this.createRotation(-0.009, 0, 0, 0.989, false).getAngle().getReal();
        final double expected = 2 * MathLib.acos(0.009);
        Assert.assertEquals(angle, expected, 1.0e-15);

        this.checkVector(this.createRotation(1, 0, 0, 0, false).getAxis(), this.createVector(1, 0, 0));
    }

    @Test
    public void testRevert() {
        final double a = 0.001;
        final double b = 0.36;
        final double c = 0.48;
        final double d = 0.8;
        final FieldRotation<DerivativeStructure> r = this.createRotation(a, b, c, d, true);
        final double a2 = a * a;
        final double b2 = b * b;
        final double c2 = c * c;
        final double d2 = d * d;
        final double den = (a2 + b2 + c2 + d2) * MathLib.sqrt(a2 + b2 + c2 + d2);
        Assert.assertEquals((b2 + c2 + d2) / den, r.getQ0().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(-a * b / den, r.getQ0().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(-a * c / den, r.getQ0().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(-a * d / den, r.getQ0().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(-b * a / den, r.getQ1().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals((a2 + c2 + d2) / den, r.getQ1().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(-b * c / den, r.getQ1().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(-b * d / den, r.getQ1().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(-c * a / den, r.getQ2().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(-c * b / den, r.getQ2().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals((a2 + b2 + d2) / den, r.getQ2().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(-c * d / den, r.getQ2().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(-d * a / den, r.getQ3().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(-d * b / den, r.getQ3().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(-d * c / den, r.getQ3().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals((a2 + b2 + c2) / den, r.getQ3().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        final FieldRotation<DerivativeStructure> reverted = r.revert();
        final FieldRotation<DerivativeStructure> rrT = r.applyTo(reverted);
        this.checkRotationDS(rrT, 1, 0, 0, 0);
        Assert.assertEquals(0, rrT.getQ0().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ0().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ0().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ0().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ1().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ1().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ1().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ1().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ2().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ2().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ2().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ2().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ3().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ3().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ3().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rrT.getQ3().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        final FieldRotation<DerivativeStructure> rTr = reverted.applyTo(r);
        this.checkRotationDS(rTr, 1, 0, 0, 0);
        Assert.assertEquals(0, rTr.getQ0().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ0().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ0().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ0().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ1().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ1().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ1().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ1().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ2().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ2().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ2().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ2().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ3().getPartialDerivative(1, 0, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ3().getPartialDerivative(0, 1, 0, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ3().getPartialDerivative(0, 0, 1, 0), 1.0e-15);
        Assert.assertEquals(0, rTr.getQ3().getPartialDerivative(0, 0, 0, 1), 1.0e-15);
        Assert.assertEquals(r.getAngle().getReal(), reverted.getAngle().getReal(), 1.0e-15);
        Assert.assertEquals(-1, FieldVector3D.dotProduct(r.getAxis(), reverted.getAxis()).getReal(), 1.0e-15);
    }

    @Test
    public void testVectorOnePair() throws MathArithmeticException {

        final FieldVector3D<DerivativeStructure> u = this.createVector(3, 2, 1);
        final FieldVector3D<DerivativeStructure> v = this.createVector(-4, 2, 2);
        final FieldRotation<DerivativeStructure> r = new FieldRotation<>(u, v);
        r.applyTo(u.scalarMultiply(v.getNorm()));
        this.checkVector(r.applyTo(u.scalarMultiply(v.getNorm())), v.scalarMultiply(u.getNorm()));

        this.checkAngle(new FieldRotation<>(u, u.negate()).getAngle(), FastMath.PI);

        try {
            new FieldRotation<>(u, this.createVector(0, 0, 0));
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

    }

    @Test
    public void testVectorTwoPairs() throws MathArithmeticException {

        final FieldVector3D<DerivativeStructure> u1 = this.createVector(3, 0, 0);
        final FieldVector3D<DerivativeStructure> u2 = this.createVector(0, 5, 0);
        final FieldVector3D<DerivativeStructure> v1 = this.createVector(0, 0, 2);
        final FieldVector3D<DerivativeStructure> v2 = this.createVector(-2, 0, 2);
        FieldRotation<DerivativeStructure> r = new FieldRotation<>(u1, u2, v1, v2);
        this.checkVector(r.applyTo(this.createVector(1, 0, 0)), this.createVector(0, 0, 1));
        this.checkVector(r.applyTo(this.createVector(0, 1, 0)), this.createVector(-1, 0, 0));

        r = new FieldRotation<>(u1, u2, u1.negate(), u2.negate());
        final FieldVector3D<DerivativeStructure> axis = r.getAxis();
        if (FieldVector3D.dotProduct(axis, this.createVector(0, 0, 1)).getReal() > 0) {
            this.checkVector(axis, this.createVector(0, 0, 1));
        } else {
            this.checkVector(axis, this.createVector(0, 0, -1));
        }
        this.checkAngle(r.getAngle(), FastMath.PI);

        final double sqrt = MathLib.sqrt(2) / 2;
        r = new FieldRotation<>(this.createVector(1, 0, 0), this.createVector(0, 1, 0),
            this.createVector(0.5, 0.5, sqrt),
            this.createVector(0.5, 0.5, -sqrt));
        this.checkRotationDS(r, sqrt, -0.5, -0.5, 0);

        r = new FieldRotation<>(u1, u2, u1, FieldVector3D.crossProduct(u1, u2));
        this.checkRotationDS(r, sqrt, sqrt, 0, 0);

        this.checkRotationDS(new FieldRotation<>(u1, u2, u1, u2), 1, 0, 0, 0);

        try {
            new FieldRotation<>(u1, u2, this.createVector(0, 0, 0), v2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathArithmeticException e) {
            // expected behavior
        }

    }

    @Test
    public void testMatrix()
                            throws NotARotationMatrixException {

        try {
            this.createRotation(new double[][] {
                { 0.0, 1.0, 0.0 },
                { 1.0, 0.0, 0.0 }
            }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            this.createRotation(new double[][] {
                { 0.445888, 0.797184, -0.407040 },
                { 0.821760, -0.184320, 0.539200 },
                { -0.354816, 0.574912, 0.737280 }
            }, 1.0e-7);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        try {
            this.createRotation(new double[][] {
                { 0.4, 0.8, -0.4 },
                { -0.4, 0.6, 0.7 },
                { 0.8, -0.2, 0.5 }
            }, 1.0e-15);
            Assert.fail("Expecting NotARotationMatrixException");
        } catch (final NotARotationMatrixException nrme) {
            // expected behavior
        }

        this.checkRotationDS(this.createRotation(new double[][] {
            { 0.445888, 0.797184, -0.407040 },
            { -0.354816, 0.574912, 0.737280 },
            { 0.821760, -0.184320, 0.539200 }
        }, 1.0e-10),
            0.8, -0.288, -0.384, -0.36);

        this.checkRotationDS(this.createRotation(new double[][] {
            { 0.539200, 0.737280, 0.407040 },
            { 0.184320, -0.574912, 0.797184 },
            { 0.821760, -0.354816, -0.445888 }
        }, 1.0e-10),
            0.36, -0.8, -0.288, -0.384);

        this.checkRotationDS(this.createRotation(new double[][] {
            { -0.445888, 0.797184, -0.407040 },
            { 0.354816, 0.574912, 0.737280 },
            { 0.821760, 0.184320, -0.539200 }
        }, 1.0e-10),
            0.384, -0.36, -0.8, -0.288);

        this.checkRotationDS(this.createRotation(new double[][] {
            { -0.539200, 0.737280, 0.407040 },
            { -0.184320, -0.574912, 0.797184 },
            { 0.821760, 0.354816, 0.445888 }
        }, 1.0e-10),
            0.288, -0.384, -0.36, -0.8);

        final double[][] m1 = { { 0.0, 1.0, 0.0 },
            { 0.0, 0.0, 1.0 },
            { 1.0, 0.0, 0.0 } };
        FieldRotation<DerivativeStructure> r = this.createRotation(m1, 1.0e-7);
        this.checkVector(r.applyTo(this.createVector(1, 0, 0)), this.createVector(0, 0, 1));
        this.checkVector(r.applyTo(this.createVector(0, 1, 0)), this.createVector(1, 0, 0));
        this.checkVector(r.applyTo(this.createVector(0, 0, 1)), this.createVector(0, 1, 0));

        final double[][] m2 = { { 0.83203, -0.55012, -0.07139 },
            { 0.48293, 0.78164, -0.39474 },
            { 0.27296, 0.29396, 0.91602 } };
        r = this.createRotation(m2, 1.0e-12);

        final DerivativeStructure[][] m3 = r.getMatrix();
        final double d00 = m2[0][0] - m3[0][0].getReal();
        final double d01 = m2[0][1] - m3[0][1].getReal();
        final double d02 = m2[0][2] - m3[0][2].getReal();
        final double d10 = m2[1][0] - m3[1][0].getReal();
        final double d11 = m2[1][1] - m3[1][1].getReal();
        final double d12 = m2[1][2] - m3[1][2].getReal();
        final double d20 = m2[2][0] - m3[2][0].getReal();
        final double d21 = m2[2][1] - m3[2][1].getReal();
        final double d22 = m2[2][2] - m3[2][2].getReal();

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
                final double m3tm3 = m3[i][0].getReal() * m3[j][0].getReal() +
                    m3[i][1].getReal() * m3[j][1].getReal() +
                    m3[i][2].getReal() * m3[j][2].getReal();
                if (i == j) {
                    Assert.assertTrue(MathLib.abs(m3tm3 - 1.0) < 1.0e-10);
                } else {
                    Assert.assertTrue(MathLib.abs(m3tm3) < 1.0e-10);
                }
            }
        }

        this.checkVector(r.applyTo(this.createVector(1, 0, 0)),
            new FieldVector3D<>(m3[0][0], m3[1][0], m3[2][0]));
        this.checkVector(r.applyTo(this.createVector(0, 1, 0)),
            new FieldVector3D<>(m3[0][1], m3[1][1], m3[2][1]));
        this.checkVector(r.applyTo(this.createVector(0, 0, 1)),
            new FieldVector3D<>(m3[0][2], m3[1][2], m3[2][2]));

        final double[][] m4 = { { 1.0, 0.0, 0.0 },
            { 0.0, -1.0, 0.0 },
            { 0.0, 0.0, -1.0 } };
        r = this.createRotation(m4, 1.0e-7);
        this.checkAngle(r.getAngle(), FastMath.PI);

        try {
            final double[][] m5 = { { 0.0, 0.0, 1.0 },
                { 0.0, 1.0, 0.0 },
                { 1.0, 0.0, 0.0 } };
            r = this.createRotation(m5, 1.0e-7);
            Assert.fail("got " + r + ", should have caught an exception");
        } catch (final NotARotationMatrixException e) {
            // expected
        }

    }

    @Test
    public void testAngles() {

        final RotationOrder[] CardanOrders = {
            RotationOrder.XYZ, RotationOrder.XZY, RotationOrder.YXZ,
            RotationOrder.YZX, RotationOrder.ZXY, RotationOrder.ZYX
        };

        for (final RotationOrder cardanOrder : CardanOrders) {
            for (double alpha1 = 0.1; alpha1 < 6.2; alpha1 += 0.3) {
                for (double alpha2 = -1.55; alpha2 < 1.55; alpha2 += 0.3) {
                    for (double alpha3 = 0.1; alpha3 < 6.2; alpha3 += 0.3) {
                        final FieldRotation<DerivativeStructure> r =
                            new FieldRotation<>(cardanOrder,
                                new DerivativeStructure(3, 1, 0, alpha1),
                                new DerivativeStructure(3, 1, 1, alpha2),
                                new DerivativeStructure(3, 1, 2, alpha3));
                        final DerivativeStructure[] angles = r.getAngles(cardanOrder);
                        this.checkAngle(angles[0], alpha1);
                        this.checkAngle(angles[1], alpha2);
                        this.checkAngle(angles[2], alpha3);
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
                        final FieldRotation<DerivativeStructure> r = new FieldRotation<>(eulerOrder,
                            new DerivativeStructure(3, 1, 0, alpha1),
                            new DerivativeStructure(3, 1, 1, alpha2),
                            new DerivativeStructure(3, 1, 2, alpha3));
                        final DerivativeStructure[] angles = r.getAngles(eulerOrder);
                        this.checkAngle(angles[0], alpha1);
                        this.checkAngle(angles[1], alpha2);
                        this.checkAngle(angles[2], alpha3);
                    }
                }
            }
        }

    }

    @Test
    public void testSingularities() {

        final RotationOrder[] CardanOrders = {
            RotationOrder.XYZ, RotationOrder.XZY, RotationOrder.YXZ,
            RotationOrder.YZX, RotationOrder.ZXY, RotationOrder.ZYX
        };

        final double[] singularCardanAngle = { FastMath.PI / 2, -FastMath.PI / 2 };
        for (final RotationOrder cardanOrder : CardanOrders) {
            for (final double element : singularCardanAngle) {
                new FieldRotation<>(cardanOrder,
                    new DerivativeStructure(3, 1, 0, 0.1),
                    new DerivativeStructure(3, 1, 1, element),
                    new DerivativeStructure(3, 1, 2, 0.3));
            }
        }

        final RotationOrder[] EulerOrders = {
            RotationOrder.XYX, RotationOrder.XZX, RotationOrder.YXY,
            RotationOrder.YZY, RotationOrder.ZXZ, RotationOrder.ZYZ
        };

        final double[] singularEulerAngle = { 0, FastMath.PI };
        for (final RotationOrder eulerOrder : EulerOrders) {
            for (final double element : singularEulerAngle) {
                new FieldRotation<>(eulerOrder,
                    new DerivativeStructure(3, 1, 0, 0.1),
                    new DerivativeStructure(3, 1, 1, element),
                    new DerivativeStructure(3, 1, 2, 0.3));
            }
        }

    }

    @Test
    public void testQuaternion() throws MathIllegalArgumentException {

        FieldRotation<DerivativeStructure> r1 = new FieldRotation<>(this.createVector(2, -3, 5),
            this.createAngle(1.7));
        final double n = 23.5;
        final FieldRotation<DerivativeStructure> r2 = new FieldRotation<>(r1.getQ0().multiply(n), r1
            .getQ1().multiply(n),
            r1.getQ2().multiply(n), r1.getQ3().multiply(n),
            true);
        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final FieldVector3D<DerivativeStructure> u = this.createVector(x, y, z);
                    this.checkVector(r2.applyTo(u), r1.applyTo(u));
                }
            }
        }

        r1 = this.createRotation(0.288, 0.384, 0.36, 0.8, false);
        this.checkRotationDS(r1,
            -r1.getQ0().getReal(), -r1.getQ1().getReal(),
            -r1.getQ2().getReal(), -r1.getQ3().getReal());
        Assert.assertEquals(0.288, r1.toRotation().getQuaternion().getQ0(), 1.0e-15);
        Assert.assertEquals(0.384, r1.toRotation().getQuaternion().getQ1(), 1.0e-15);
        Assert.assertEquals(0.36, r1.toRotation().getQuaternion().getQ2(), 1.0e-15);
        Assert.assertEquals(0.8, r1.toRotation().getQuaternion().getQ3(), 1.0e-15);

    }

    @Test
    public void testCompose() throws MathIllegalArgumentException {

        final FieldRotation<DerivativeStructure> r1 =
            new FieldRotation<>(this.createVector(2, -3, 5),
                this.createAngle(1.7));
        final FieldRotation<DerivativeStructure> r2 =
            new FieldRotation<>(this.createVector(-1, 3, 2),
                this.createAngle(0.3));
        final FieldRotation<DerivativeStructure> r3 = r2.applyTo(r1);
        final FieldRotation<DerivativeStructure> r3Double = r2.applyTo(new Rotation(false,
            r1.getQ0().getReal(),
            r1.getQ1().getReal(),
            r1.getQ2().getReal(),
            r1.getQ3().getReal()));

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final FieldVector3D<DerivativeStructure> u = this.createVector(x, y, z);
                    this.checkVector(r2.applyTo(r1.applyTo(u)), r3.applyTo(u));
                    this.checkVector(r2.applyTo(r1.applyTo(u)), r3Double.applyTo(u));
                }
            }
        }
    }

    @Test
    public void testComposeInverse() throws MathIllegalArgumentException {

        final FieldRotation<DerivativeStructure> r1 =
            new FieldRotation<>(this.createVector(2, -3, 5),
                this.createAngle(1.7));
        final FieldRotation<DerivativeStructure> r2 =
            new FieldRotation<>(this.createVector(-1, 3, 2),
                this.createAngle(0.3));
        final FieldRotation<DerivativeStructure> r3 = r2.applyInverseTo(r1);
        final FieldRotation<DerivativeStructure> r3Double = r2.applyInverseTo(new Rotation(false,
            r1.getQ0().getReal(),
            r1.getQ1().getReal(),
            r1.getQ2().getReal(),
            r1.getQ3().getReal()));

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final FieldVector3D<DerivativeStructure> u = this.createVector(x, y, z);
                    this.checkVector(r2.applyInverseTo(r1.applyTo(u)), r3.applyTo(u));
                    this.checkVector(r2.applyInverseTo(r1.applyTo(u)), r3Double.applyTo(u));
                }
            }
        }

    }

    @Test
    public void testDoubleVectors() throws MathIllegalArgumentException {

        final Well1024a random = new Well1024a(0x180b41cfeeffaf67l);
        final UnitSphereRandomVectorGenerator g = new UnitSphereRandomVectorGenerator(3, random);
        for (int i = 0; i < 10; ++i) {
            final double[] unit = g.nextVector();
            final FieldRotation<DerivativeStructure> r =
                new FieldRotation<>(this.createVector(unit[0],
                    unit[1], unit[2]),
                    this.createAngle(random.nextDouble()));

            for (double x = -0.9; x < 0.9; x += 0.2) {
                for (double y = -0.9; y < 0.9; y += 0.2) {
                    for (double z = -0.9; z < 0.9; z += 0.2) {
                        final FieldVector3D<DerivativeStructure> uds = this.createVector(x, y, z);
                        final FieldVector3D<DerivativeStructure> ruds = r.applyTo(uds);
                        final FieldVector3D<DerivativeStructure> rIuds = r.applyInverseTo(uds);
                        final Vector3D u = new Vector3D(x, y, z);
                        final FieldVector3D<DerivativeStructure> ru = r.applyTo(u);
                        final FieldVector3D<DerivativeStructure> rIu = r.applyInverseTo(u);
                        final DerivativeStructure[] ruArray = new DerivativeStructure[3];
                        r.applyTo(new double[] { x, y, z }, ruArray);
                        final DerivativeStructure[] rIuArray = new DerivativeStructure[3];
                        r.applyInverseTo(new double[] { x, y, z }, rIuArray);
                        this.checkVector(ruds, ru);
                        this.checkVector(ruds, new FieldVector3D<>(ruArray));
                        this.checkVector(rIuds, rIu);
                        this.checkVector(rIuds, new FieldVector3D<>(rIuArray));
                    }
                }
            }
        }

    }

    @Test
    public void testDoubleRotations() throws MathIllegalArgumentException {

        final Well1024a random = new Well1024a(0x180b41cfeeffaf67l);
        final UnitSphereRandomVectorGenerator g = new UnitSphereRandomVectorGenerator(3, random);
        for (int i = 0; i < 10; ++i) {
            final double[] unit1 = g.nextVector();
            final Rotation r1 = new Rotation(new Vector3D(unit1[0], unit1[1], unit1[2]),
                random.nextDouble());
            final FieldRotation<DerivativeStructure> r1Prime = new FieldRotation<>(
                new DerivativeStructure(4, 1, 0, r1.getQuaternion().getQ0()),
                new DerivativeStructure(4, 1, 1, r1.getQuaternion().getQ1()),
                new DerivativeStructure(4, 1, 2, r1.getQuaternion().getQ2()),
                new DerivativeStructure(4, 1, 3, r1.getQuaternion().getQ3()),
                false);
            final double[] unit2 = g.nextVector();
            final FieldRotation<DerivativeStructure> r2 =
                new FieldRotation<>(this.createVector(unit2[0],
                    unit2[1], unit2[2]),
                    this.createAngle(random.nextDouble()));

            final FieldRotation<DerivativeStructure> rA = FieldRotation.applyTo(r1, r2);
            final FieldRotation<DerivativeStructure> rB = r1Prime.applyTo(r2);
            final FieldRotation<DerivativeStructure> rC = FieldRotation.applyInverseTo(r1, r2);
            final FieldRotation<DerivativeStructure> rD = r1Prime.applyInverseTo(r2);

            for (double x = -0.9; x < 0.9; x += 0.2) {
                for (double y = -0.9; y < 0.9; y += 0.2) {
                    for (double z = -0.9; z < 0.9; z += 0.2) {

                        final FieldVector3D<DerivativeStructure> uds = this.createVector(x, y, z);
                        this.checkVector(r1Prime.applyTo(uds), FieldRotation.applyTo(r1, uds));
                        this.checkVector(r1Prime.applyInverseTo(uds), FieldRotation.applyInverseTo(r1, uds));
                        this.checkVector(rA.applyTo(uds), rB.applyTo(uds));
                        this.checkVector(rA.applyInverseTo(uds), rB.applyInverseTo(uds));
                        this.checkVector(rC.applyTo(uds), rD.applyTo(uds));
                        this.checkVector(rC.applyInverseTo(uds), rD.applyInverseTo(uds));

                    }
                }
            }
        }

    }

    @Test
    public void testDerivatives() {

        final double eps = 5.0e-16;
        final double kx = 2;
        final double ky = -3;
        final double kz = 5;
        final double n2 = kx * kx + ky * ky + kz * kz;
        final double n = MathLib.sqrt(n2);
        final double theta = 1.7;
        final double cosTheta = MathLib.cos(theta);
        final double sinTheta = MathLib.sin(theta);
        final FieldRotation<DerivativeStructure> r =
            new FieldRotation<>(this.createAxis(kx, ky, kz),
                this.createAngle(theta));
        final Vector3D a = new Vector3D(kx / n, ky / n, kz / n);

        // Jacobian of the normalized rotation axis a with respect to the Cartesian vector k
        final RealMatrix dadk = MatrixUtils.createRealMatrix(new double[][] {
            { (ky * ky + kz * kz) / (n * n2), -kx * ky / (n * n2), -kx * kz / (n * n2) },
            { -kx * ky / (n * n2), (kx * kx + kz * kz) / (n * n2), -ky * kz / (n * n2) },
            { -kx * kz / (n * n2), -ky * kz / (n * n2), (kx * kx + ky * ky) / (n * n2) }
        });

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final Vector3D u = new Vector3D(x, y, z);
                    final FieldVector3D<DerivativeStructure> v = r.applyTo(this.createVector(x, y, z));

                    // explicit formula for rotation of vector u around axis a with angle theta
                    final double dot = Vector3D.dotProduct(u, a);
                    final Vector3D cross = Vector3D.crossProduct(a, u);
                    final double c1 = 1 - cosTheta;
                    final double c2 = c1 * dot;
                    final Vector3D rt = new Vector3D(cosTheta, u, c2, a, sinTheta, cross);
                    Assert.assertEquals(rt.getX(), v.getX().getReal(), eps);
                    Assert.assertEquals(rt.getY(), v.getY().getReal(), eps);
                    Assert.assertEquals(rt.getZ(), v.getZ().getReal(), eps);

                    // Jacobian of the image v = r(u) with respect to rotation axis a
                    // (analytical differentiation of the explicit formula)
                    final RealMatrix dvda = MatrixUtils.createRealMatrix(new double[][] {
                        { c1 * x * a.getX() + c2, c1 * y * a.getX() + sinTheta * z,
                            c1 * z * a.getX() - sinTheta * y },
                        { c1 * x * a.getY() - sinTheta * z, c1 * y * a.getY() + c2,
                            c1 * z * a.getY() + sinTheta * x },
                        { c1 * x * a.getZ() + sinTheta * y, c1 * y * a.getZ() - sinTheta * x,
                            c1 * z * a.getZ() + c2 }
                    });

                    // compose Jacobians
                    final RealMatrix dvdk = dvda.multiply(dadk);

                    // derivatives with respect to un-normalized axis
                    Assert.assertEquals(dvdk.getEntry(0, 0), v.getX().getPartialDerivative(1, 0, 0, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(0, 1), v.getX().getPartialDerivative(0, 1, 0, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(0, 2), v.getX().getPartialDerivative(0, 0, 1, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(1, 0), v.getY().getPartialDerivative(1, 0, 0, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(1, 1), v.getY().getPartialDerivative(0, 1, 0, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(1, 2), v.getY().getPartialDerivative(0, 0, 1, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(2, 0), v.getZ().getPartialDerivative(1, 0, 0, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(2, 1), v.getZ().getPartialDerivative(0, 1, 0, 0), eps);
                    Assert.assertEquals(dvdk.getEntry(2, 2), v.getZ().getPartialDerivative(0, 0, 1, 0), eps);

                    // derivative with respect to rotation angle
                    // (analytical differentiation of the explicit formula)
                    final Vector3D dvdTheta =
                        new Vector3D(-sinTheta, u, sinTheta * dot, a, cosTheta, cross);
                    Assert.assertEquals(dvdTheta.getX(), v.getX().getPartialDerivative(0, 0, 0, 1), eps);
                    Assert.assertEquals(dvdTheta.getY(), v.getY().getPartialDerivative(0, 0, 0, 1), eps);
                    Assert.assertEquals(dvdTheta.getZ(), v.getZ().getPartialDerivative(0, 0, 0, 1), eps);

                }
            }
        }
    }

    @Test
    public void testArray() throws MathIllegalArgumentException {

        final FieldRotation<DerivativeStructure> r = new FieldRotation<>(this.createAxis(2, -3, 5),
            this.createAngle(1.7));

        for (double x = -0.9; x < 0.9; x += 0.2) {
            for (double y = -0.9; y < 0.9; y += 0.2) {
                for (double z = -0.9; z < 0.9; z += 0.2) {
                    final FieldVector3D<DerivativeStructure> u = this.createVector(x, y, z);
                    final FieldVector3D<DerivativeStructure> v = r.applyTo(u);
                    final DerivativeStructure[] out = new DerivativeStructure[3];
                    r.applyTo(new DerivativeStructure[] { u.getX(), u.getY(), u.getZ() }, out);
                    Assert.assertEquals(v.getX().getReal(), out[0].getReal(), 1.0e-10);
                    Assert.assertEquals(v.getY().getReal(), out[1].getReal(), 1.0e-10);
                    Assert.assertEquals(v.getZ().getReal(), out[2].getReal(), 1.0e-10);
                    r.applyInverseTo(out, out);
                    Assert.assertEquals(u.getX().getReal(), out[0].getReal(), 1.0e-10);
                    Assert.assertEquals(u.getY().getReal(), out[1].getReal(), 1.0e-10);
                    Assert.assertEquals(u.getZ().getReal(), out[2].getReal(), 1.0e-10);
                }
            }
        }

    }

    @Test
    public void testApplyInverseTo() throws MathIllegalArgumentException {

        final DerivativeStructure[] in = new DerivativeStructure[3];
        final DerivativeStructure[] out = new DerivativeStructure[3];
        final DerivativeStructure[] rebuilt = new DerivativeStructure[3];
        FieldRotation<DerivativeStructure> r = new FieldRotation<>(this.createVector(2, -3, 5),
            this.createAngle(1.7));
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final FieldVector3D<DerivativeStructure> u = this.createVector(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));
                r.applyInverseTo(r.applyTo(u));
                this.checkVector(u, r.applyInverseTo(r.applyTo(u)));
                this.checkVector(u, r.applyTo(r.applyInverseTo(u)));
                in[0] = u.getX();
                in[1] = u.getY();
                in[2] = u.getZ();
                r.applyTo(in, out);
                r.applyInverseTo(out, rebuilt);
                Assert.assertEquals(in[0].getReal(), rebuilt[0].getReal(), 1.0e-12);
                Assert.assertEquals(in[1].getReal(), rebuilt[1].getReal(), 1.0e-12);
                Assert.assertEquals(in[2].getReal(), rebuilt[2].getReal(), 1.0e-12);
            }
        }

        r = this.createRotation(1, 0, 0, 0, false);
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final FieldVector3D<DerivativeStructure> u = this.createVector(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));
                this.checkVector(u, r.applyInverseTo(r.applyTo(u)));
                this.checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }

        r = new FieldRotation<>(this.createVector(0, 0, 1), this.createAngle(FastMath.PI));
        for (double lambda = 0; lambda < 6.2; lambda += 0.2) {
            for (double phi = -1.55; phi < 1.55; phi += 0.2) {
                final FieldVector3D<DerivativeStructure> u = this.createVector(MathLib.cos(lambda) * MathLib.cos(phi),
                    MathLib.sin(lambda) * MathLib.cos(phi),
                    MathLib.sin(phi));
                this.checkVector(u, r.applyInverseTo(r.applyTo(u)));
                this.checkVector(u, r.applyTo(r.applyInverseTo(u)));
            }
        }

    }

    @Test
    public void testIssue639() throws MathArithmeticException {
        final FieldVector3D<DerivativeStructure> u1 = this.createVector(-1321008684645961.0 / 268435456.0,
            -5774608829631843.0 / 268435456.0,
            -3822921525525679.0 / 4294967296.0);
        final FieldVector3D<DerivativeStructure> u2 = this.createVector(-5712344449280879.0 / 2097152.0,
            -2275058564560979.0 / 1048576.0,
            4423475992255071.0 / 65536.0);
        final FieldRotation<DerivativeStructure> rot =
            new FieldRotation<>(u1, u2, this.createVector(1, 0, 0),
                this.createVector(0, 0, 1));
        Assert.assertEquals(0.6228370359608200639829222, rot.getQ0().getReal(), 1.0e-15);
        Assert.assertEquals(-0.0257707621456498790029987, rot.getQ1().getReal(), 1.0e-15);
        Assert.assertEquals(0.0000000002503012255839931, rot.getQ2().getReal(), 1.0e-15);
        Assert.assertEquals(0.7819270390861109450724902, rot.getQ3().getReal(), 1.0e-15);
    }

    @Test
    public void testIssue801() throws MathArithmeticException {
        final FieldVector3D<DerivativeStructure> u1 =
            this.createVector(0.9999988431610581, -0.0015210774290851095, 0.0);
        final FieldVector3D<DerivativeStructure> u2 = this.createVector(0.0, 0.0, 1.0);

        final FieldVector3D<DerivativeStructure> v1 = this.createVector(0.9999999999999999, 0.0, 0.0);
        final FieldVector3D<DerivativeStructure> v2 = this.createVector(0.0, 0.0, -1.0);

        final FieldRotation<DerivativeStructure> quat = new FieldRotation<>(u1, u2, v1, v2);
        final double q2 = quat.getQ0().getReal() * quat.getQ0().getReal() +
            quat.getQ1().getReal() * quat.getQ1().getReal() +
            quat.getQ2().getReal() * quat.getQ2().getReal() +
            quat.getQ3().getReal() * quat.getQ3().getReal();
        Assert.assertEquals(1.0, q2, 1.0e-14);
        Assert.assertEquals(0.0, FieldVector3D.angle(v1, quat.applyTo(u1)).getReal(), 1.0e-14);
        Assert.assertEquals(0.0, FieldVector3D.angle(v2, quat.applyTo(u2)).getReal(), 1.0e-14);

    }

    // Coverage test
    @Test
    public void testCovGetAngle() throws MathArithmeticException {
        final FieldVector3D<DerivativeStructure> u1 =
            this.createVector(0.9999988431610581, -0.0015210774290851095, 0.0);
        final FieldVector3D<DerivativeStructure> u2 = this.createVector(0.0, 0.0, 1.0);

        final FieldVector3D<DerivativeStructure> v1 = this.createVector(0.9999999999999999, 0.0, 0.0);
        final FieldVector3D<DerivativeStructure> v2 = this.createVector(0.0, 0.0, -1.0);

        final FieldRotation<DerivativeStructure> quat = new FieldRotation<>(u1, u2, v1, v2);
        final double q2 = quat.getQ0().getReal() * quat.getQ0().getReal() +
            quat.getQ1().getReal() * quat.getQ1().getReal() +
            quat.getQ2().getReal() * quat.getQ2().getReal() +
            quat.getQ3().getReal() * quat.getQ3().getReal();
        Assert.assertEquals(1.0, q2, 1.0e-14);
        Assert.assertEquals(0.0, FieldVector3D.angle(v1, quat.applyTo(u1)).getReal(), 1.0e-14);
        Assert.assertEquals(0.0, FieldVector3D.angle(v2, quat.applyTo(u2)).getReal(), 1.0e-14);

    }

    private void checkAngle(final DerivativeStructure a1, final double a2) {
        Assert.assertEquals(a1.getReal(), MathUtils.normalizeAngle(a2, a1.getReal()), 1.0e-10);
    }

    private void checkRotationDS(final FieldRotation<DerivativeStructure> r, final double q0, final double q1,
                                 final double q2, final double q3) {
        final FieldRotation<DerivativeStructure> rPrime = this.createRotation(q0, q1, q2, q3, false);
        Assert.assertEquals(0, FieldRotation.distance(r, rPrime).getReal(), 1.0e-12);
    }

    private FieldRotation<DerivativeStructure> createRotation(final double q0, final double q1, final double q2,
                                                              final double q3,
                                                              final boolean needsNormalization) {
        return new FieldRotation<>(new DerivativeStructure(4, 1, 0, q0),
            new DerivativeStructure(4, 1, 1, q1),
            new DerivativeStructure(4, 1, 2, q2),
            new DerivativeStructure(4, 1, 3, q3),
            needsNormalization);
    }

    private FieldRotation<DerivativeStructure> createRotation(final double[][] m, final double threshold) {
        final DerivativeStructure[][] mds = new DerivativeStructure[m.length][m[0].length];
        int index = 0;
        for (int i = 0; i < m.length; ++i) {
            for (int j = 0; j < m[i].length; ++j) {
                mds[i][j] = new DerivativeStructure(4, 1, index, m[i][j]);
                index = (index + 1) % 4;
            }
        }
        return new FieldRotation<>(mds, threshold);
    }

    private FieldVector3D<DerivativeStructure> createVector(final double x, final double y, final double z) {
        return new FieldVector3D<>(new DerivativeStructure(4, 1, x),
            new DerivativeStructure(4, 1, y),
            new DerivativeStructure(4, 1, z));
    }

    private FieldVector3D<DerivativeStructure> createAxis(final double x, final double y, final double z) {
        return new FieldVector3D<>(new DerivativeStructure(4, 1, 0, x),
            new DerivativeStructure(4, 1, 1, y),
            new DerivativeStructure(4, 1, 2, z));
    }

    private DerivativeStructure createAngle(final double alpha) {
        return new DerivativeStructure(4, 1, 3, alpha);
    }

    private void checkVector(final FieldVector3D<DerivativeStructure> u, final FieldVector3D<DerivativeStructure> v) {
        Assert.assertEquals(u.getX().getReal(), v.getX().getReal(), 1.0e-12);
        Assert.assertEquals(u.getY().getReal(), v.getY().getReal(), 1.0e-12);
        Assert.assertEquals(u.getZ().getReal(), v.getZ().getReal(), 1.0e-12);
    }

}
