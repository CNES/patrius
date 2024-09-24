/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.11:DM:DM-3254:22/05/2023:[PATRIUS] AngularCoordinates - Distinction entre acceleration rotationelle nulle et ZERO
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.sampling.FixedStepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepNormalizer;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well1024a;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AngularCoordinatesTest {

    @Test
    public void testZeroRate() {
        final AngularCoordinates AngularCoordinates =
            new AngularCoordinates(new Rotation(false, 0.48, 0.64, 0.36, 0.48), Vector3D.ZERO);
        Assert.assertEquals(Vector3D.ZERO, AngularCoordinates.getRotationRate());
        final double dt = 10.0;
        final AngularCoordinates shifted = AngularCoordinates.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationRate());
        Assert.assertEquals(AngularCoordinates.getRotation(), shifted.getRotation());
    }

    @Test
    public void testShift() {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final AngularCoordinates AngularCoordinates =
            new AngularCoordinates(Rotation.IDENTITY,
                new Vector3D(rate, Vector3D.PLUS_K));
        Assert.assertEquals(rate, AngularCoordinates.getRotationRate().getNorm(), 1.0e-10);
        final double dt = 10.0;
        final double alpha = rate * dt;
        final AngularCoordinates shifted = AngularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(AngularCoordinates.getRotation(), shifted.getRotation()), 1.0e-10);

        final Vector3D xSat = shifted.getRotation().applyTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new Vector3D(MathLib.cos(alpha), MathLib.sin(alpha), 0)).getNorm(),
            1.0e-10);
        final Vector3D ySat = shifted.getRotation().applyTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new Vector3D(-MathLib.sin(alpha), MathLib.cos(alpha), 0)).getNorm(),
            1.0e-10);
        final Vector3D zSat = shifted.getRotation().applyTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-10);

    }

    @Test
    public void testSpin() {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final AngularCoordinates angularCoordinates =
            new AngularCoordinates(new Rotation(false, 0.48, 0.64, 0.36, 0.48),
                new Vector3D(rate, Vector3D.PLUS_K));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm(), 1.0e-10);
        final double dt = 10.0;
        final AngularCoordinates shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(rate * dt, Rotation.distance(angularCoordinates.getRotation(), shifted.getRotation()),
            1.0e-10);

        final Vector3D shiftedX = shifted.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D shiftedY = shifted.getRotation().applyTo(Vector3D.PLUS_J);
        final Vector3D shiftedZ = shifted.getRotation().applyTo(Vector3D.PLUS_K);
        final Vector3D originalX = angularCoordinates.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D originalY = angularCoordinates.getRotation().applyTo(Vector3D.PLUS_J);
        final Vector3D originalZ = angularCoordinates.getRotation().applyTo(Vector3D.PLUS_K);
        Assert.assertEquals(MathLib.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assert.assertEquals(MathLib.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assert.assertEquals(-MathLib.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assert.assertEquals(MathLib.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assert.assertEquals(1.0, Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        final Vector3D forward =
            AngularCoordinates.estimateRate(angularCoordinates.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(angularCoordinates.getRotationRate()).getNorm(), 1.0e-10);

        final Vector3D reversed = AngularCoordinates
            .estimateRate(shifted.getRotation(), angularCoordinates.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(angularCoordinates.getRotationRate()).getNorm(), 1.0e-10);

    }

    @Test
    public void testAddOffset() {
        final Random random = new Random(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            final Rotation r = randomRotation(random);
            final Vector3D o = randomVector(random, 1.0e-3);

            // Case with non-null acceleration (and true computeSpinDerivatives)
            Vector3D a = randomVector(random, 1.0e-3);
            AngularCoordinates ac = new AngularCoordinates(r, o, a);
            AngularCoordinates sum = ac.addOffset(ac, true);
            Assert.assertNotNull(sum.getRotation());
            Assert.assertNotNull(sum.getRotationRate());
            // Check that the acceleration is not null
            Assert.assertNotNull(sum.getRotationAcceleration());

            // Case with null acceleration (and true computeSpinDerivatives)
            a = null;
            ac = new AngularCoordinates(r, o, a);
            sum = ac.addOffset(ac, true);
            Assert.assertNotNull(sum.getRotation());
            Assert.assertNotNull(sum.getRotationRate());
            // Check that the acceleration is null
            Assert.assertNull(sum.getRotationAcceleration());
        }
    }

    @Test
    public void testReverseOffset() {
        final Random random = new Random(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            final Rotation r = randomRotation(random);
            final Vector3D o = randomVector(random, 1.0e-3);
            final Vector3D a = randomVector(random, 1.0e-3);
            final AngularCoordinates ac = new AngularCoordinates(r, o, a);
            final AngularCoordinates sum = ac.addOffset(ac.revert(true), true);
            Assert.assertEquals(0.0, sum.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationAcceleration().getNorm(), 1.0e-15);

            // Same computation with subtractOffset
            final AngularCoordinates diff = ac.subtractOffset(ac, true);
            Assert.assertEquals(0.0, sum.getRotationAcceleration().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, diff.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, diff.getRotationRate().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, diff.getRotationAcceleration().getNorm(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        final AngularCoordinates ac1 =
            new AngularCoordinates(new Rotation(false, 0.48, 0.64, 0.36, 0.48), Vector3D.ZERO);
        final AngularCoordinates ac2 =
            new AngularCoordinates(new Rotation(false, 0.36, -0.48, 0.48, 0.64), Vector3D.ZERO);

        final AngularCoordinates add12 = ac1.addOffset(ac2);
        final AngularCoordinates add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, Rotation.distance(add12.getRotation(), add21.getRotation()), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        final Random random = new Random(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            final Rotation r1 = randomRotation(random);
            final Vector3D o1 = randomVector(random, 1.0e-2);
            final Vector3D oDot1 = randomVector(random, 1.0e-2);
            final AngularCoordinates ac1 = new AngularCoordinates(r1, o1, oDot1);
            final Rotation r2 = randomRotation(random);
            final Vector3D o2 = randomVector(random, 1.0e-2);
            final Vector3D oDot2 = randomVector(random, 1.0e-2);
            final AngularCoordinates ac2 = new AngularCoordinates(r2, o2, oDot2);

            final AngularCoordinates roundTripSA = ac1.subtractOffset(ac2, true).addOffset(ac2, true);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripSA.getRotation()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()), 2.0e-17);
            Assert.assertEquals(0.0,
                Vector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()), 2.0e-17);

            final AngularCoordinates roundTripAS = ac1.addOffset(ac2, true).subtractOffset(ac2, true);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripAS.getRotation()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()), 2.0e-17);
            Assert.assertEquals(0.0,
                Vector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()), 2.0e-17);

        }
    }

    @Test
    public void testRodriguesSymmetry() {

        // check the two-way conversion result in identity
        final RandomGenerator random = new Well1024a(0xb1e615aaa8236b52l);
        for (int i = 0; i < 1000; ++i) {
            final Rotation rotation = randomRotation(random);
            final Vector3D rotationRate = randomVector(random, 0.01);
            final Vector3D rotationAcceleration = randomVector(random, 0.01);
            final AngularCoordinates ac = new AngularCoordinates(rotation, rotationRate, rotationAcceleration);
            final AngularCoordinates rebuilt = AngularCoordinates.createFromModifiedRodrigues(
                ac.getModifiedRodrigues(1.0, true), true);
            Assert.assertEquals(0.0, Rotation.distance(rotation, rebuilt.getRotation()), 1.0e-14);
            Assert.assertEquals(0.0, Vector3D.distance(rotationRate, rebuilt.getRotationRate()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(rotationAcceleration, rebuilt.getRotationAcceleration()),
                1.0e-15);
        }

    }

    @Test
    public void testRodriguesSpecialCases() {

        // identity
        final double[][] identity = new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO)
            .getModifiedRodrigues(1.0);
        for (final double[] row : identity) {
            for (final double element : row) {
                Assert.assertEquals(0.0, element, Precision.SAFE_MIN);
            }
        }
        final AngularCoordinates acId = AngularCoordinates.createFromModifiedRodrigues(identity);
        Assert.assertEquals(0.0, acId.getRotation().getAngle(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, acId.getRotationRate().getNorm(), Precision.SAFE_MIN);

        // PI angle rotation (which is singular for non-modified Rodrigues vector)
        final RandomGenerator random = new Well1024a(0x2158523e6accb859l);
        for (int i = 0; i < 100; ++i) {
            final Vector3D axis = randomVector(random, 1.0);
            final AngularCoordinates original = new AngularCoordinates(new Rotation(axis, FastMath.PI),
                Vector3D.ZERO, Vector3D.ZERO);
            final AngularCoordinates rebuilt = AngularCoordinates.createFromModifiedRodrigues(original
                .getModifiedRodrigues(1.0));
            Assert.assertEquals(FastMath.PI, rebuilt.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, MathLib.sin(Vector3D.angle(axis, rebuilt.getRotation().getAxis())), 1.0e-15);
            Assert.assertEquals(0.0, rebuilt.getRotationRate().getNorm(), 1.0e-16);
        }

    }

    @Test
    public void testShiftWithoutAcceleration() {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final AngularCoordinates ac =
            new AngularCoordinates(Rotation.IDENTITY,
                new Vector3D(rate, Vector3D.PLUS_K),
                Vector3D.ZERO);
        Assert.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        final double dt = 10.0;
        final double alpha = rate * dt;
        final AngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-15);

        final Vector3D xSat = shifted.getRotation().applyTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new Vector3D(MathLib.cos(alpha), MathLib.sin(alpha), 0)).getNorm(),
            1.0e-15);
        final Vector3D ySat = shifted.getRotation().applyTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new Vector3D(-MathLib.sin(alpha), MathLib.cos(alpha), 0)).getNorm(),
            1.0e-15);
        final Vector3D zSat = shifted.getRotation().applyTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-15);

    }

    @Test
    public void testShiftWithAcceleration() {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final double acc = 0.001;
        final double dt = 1.0;
        final int n = 2000;
        final AngularCoordinates quadratic =
            new AngularCoordinates(Rotation.IDENTITY,
                new Vector3D(rate, Vector3D.PLUS_K),
                new Vector3D(acc, Vector3D.PLUS_J));
        final AngularCoordinates linear =
            new AngularCoordinates(quadratic.getRotation(), quadratic.getRotationRate(), Vector3D.ZERO);

        final FirstOrderDifferentialEquations ode = new FirstOrderDifferentialEquations(){
            @Override
            public int getDimension() {
                return 4;
            }

            @Override
            public void computeDerivatives(final double t, final double[] q, final double[] qDot) {
                final double omegaX = quadratic.getRotationRate().getX() + t
                    * quadratic.getRotationAcceleration().getX();
                final double omegaY = quadratic.getRotationRate().getY() + t
                    * quadratic.getRotationAcceleration().getY();
                final double omegaZ = quadratic.getRotationRate().getZ() + t
                    * quadratic.getRotationAcceleration().getZ();
                qDot[0] = 0.5 * MathArrays.linearCombination(q[1], omegaX, q[2], omegaY, -q[3], omegaZ);
                qDot[1] = 0.5 * MathArrays.linearCombination(q[0], omegaX, q[3], omegaY, -q[2], omegaZ);
                qDot[2] = 0.5 * MathArrays.linearCombination(-q[3], omegaX, q[0], omegaY, q[1], omegaZ);
                qDot[3] = 0.5 * MathArrays.linearCombination(q[2], omegaX, -q[1], omegaY, q[0], omegaZ);
            }
        };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(1.0e-6, 1.0, 1.0e-12, 1.0e-12);
        integrator.addStepHandler(new StepNormalizer(dt / n, new FixedStepHandler(){
            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public void handleStep(final double t, final double[] y, final double[] yDot, final boolean isLast) {
                final Rotation reference = new Rotation(true, y[0], y[1], y[2], y[3]);

                // the error in shiftedBy taking acceleration into account is cubic
                final double expectedCubicError = 1.4544e-6 * t * t * t;
                Assert.assertEquals(expectedCubicError,
                    Rotation.distance(reference, quadratic.shiftedBy(t, true).getRotation()),
                    0.0001 * expectedCubicError);

                // the error in shiftedBy not taking acceleration into account is quadratic
                final double expectedQuadraticError = 5.0e-4 * t * t;
                Assert.assertEquals(expectedQuadraticError,
                    Rotation.distance(reference, linear.shiftedBy(t, true).getRotation()),
                    0.00001 * expectedQuadraticError);

            }
        }));

        final double[] y = new double[] {
            quadratic.getRotation().getQuaternion().getQ0(),
            quadratic.getRotation().getQuaternion().getQ1(),
            quadratic.getRotation().getQuaternion().getQ2(),
            quadratic.getRotation().getQuaternion().getQ3()
        };
        integrator.integrate(ode, 0, y, dt, y);

    }

    @Test
    public void testInverseCrossProducts() {
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_J);
        checkInverse(Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
        checkInverse(Vector3D.ZERO, Vector3D.ZERO, Vector3D.PLUS_J);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_K, Vector3D.PLUS_J);
        checkInverse(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.ZERO);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_I);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, new Vector3D(1, 0, -1).normalize());
        checkInverse(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.ZERO, Vector3D.PLUS_J, Vector3D.ZERO);
    }

    @Test
    public void testInverseCrossProductsFailures() {
        checkInverseFailure(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.PLUS_J, Vector3D.PLUS_I,
                Vector3D.PLUS_K);
        checkInverseFailure(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO,
                Vector3D.PLUS_K);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.ZERO, Vector3D.MINUS_I,
                Vector3D.PLUS_K);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.ZERO, Vector3D.PLUS_J,
                Vector3D.PLUS_J);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_J,
                Vector3D.ZERO);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.ZERO,
                Vector3D.PLUS_J);
    }

    @Test
    public void testRandomInverseCrossProducts() {
        final RandomGenerator generator = new Well1024a(0x52b29d8f6ac2d64bl);
        for (int i = 0; i < 10000; ++i) {
            final Vector3D omega = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            final Vector3D v1 = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            final Vector3D v2 = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            checkInverse(omega, v1, v2);
        }
    }

    @Test
    public void testRandomPVCoordinates() throws PatriusException {

        final RandomGenerator generator = new Well1024a(0x49eb5b92d1f94b89l);

        for (int i = 0; i < 100; ++i) {

            final Rotation r = randomRotation(generator);
            final Vector3D omega = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            final Vector3D omegaDot = randomVector(generator, 0.1 * generator.nextDouble() + 0.01);

            final AngularCoordinates ref = new AngularCoordinates(r, omega, omegaDot);
            final AngularCoordinates inv = ref.revert(true);

            for (int j = 0; j < 100; ++j) {

                final PVCoordinates v1 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
                final PVCoordinates v2 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
                final PVCoordinates u1 = inv.applyTo(v1);
                final PVCoordinates u2 = inv.applyTo(v2);

                final AngularCoordinates rebuilt = new AngularCoordinates(u1, u2, v1, v2, 1.0e-9, true);

                Assert.assertEquals(0.0, Rotation.distance(r, rebuilt.getRotation()), 2.0e-14);

                Assert.assertEquals(0.0, Vector3D.distance(omega,
                    rebuilt.getRotationRate()),
                    2.0e-12 * omega.getNorm());

                Assert.assertEquals(0.0, Vector3D.distance(omegaDot,
                    rebuilt.getRotationAcceleration()),
                    4.0e-10 * omegaDot.getNorm());
            }
        }
    }

    @Test
    public void testRandomPVCoordinateseNoAcc() throws PatriusException {

        final RandomGenerator generator = new Well1024a(0x49eb5b92d1f94b89l);

        final Rotation r = randomRotation(generator);
        final Vector3D omega = randomVector(generator, 10 * generator.nextDouble() + 1.0);
        final Vector3D omegaDot = randomVector(generator, 0.1 * generator.nextDouble() + 0.01);

        final AngularCoordinates ref = new AngularCoordinates(r, omega, omegaDot);
        final AngularCoordinates inv = ref.revert(true);

        final PVCoordinates v1 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
        final PVCoordinates v2 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
        final PVCoordinates u1 = inv.applyTo(v1);
        final PVCoordinates u2 = inv.applyTo(v2);

        final AngularCoordinates ar = new AngularCoordinates(u1, u2, v1, v2, 1.0e-9);

        Assert.assertEquals(0.0, Rotation.distance(r, ar.getRotation()), 2.0e-14);

        Assert.assertEquals(0.0, Vector3D.distance(omega,
            ar.getRotationRate()),
            2.0e-12 * omega.getNorm());
        Assert.assertNull(ar.getRotationAcceleration());
    }

    // Mathematical validation test
    @Test
    public void testCircularAccelerated() throws PatriusException {

        // Initial conditions : rotation = 0 deg, spin = 1 deg / s, dot spin = 1 deg / s^2
        final double initRot = 0;
        final Vector3D initOmega = new Vector3D(MathLib.toRadians(1), Vector3D.PLUS_J);
        final Vector3D initOmegaDot = new Vector3D(MathLib.toRadians(1), Vector3D.PLUS_J);

        // Expected rotation, spin and dotspin function of time
        final double t = 5;
        final double expectedRot = initRot + initOmega.getNorm() * t + initOmegaDot.getNorm() * MathLib.pow(t, 2) / 2;
        final Vector3D expectedOmega = initOmega.add(initOmegaDot.scalarMultiply(t));
        final Vector3D expectedOmegaDot = initOmegaDot;

        // Initial PVCoordinates couple to rotate are X and Z axis to a fixed radius
        final double R = 1.0;

        // X
        final Vector3D posInitX = new Vector3D(R, 0, 0);
        final PVCoordinates pvCoordInitX = new PVCoordinates(posInitX, Vector3D.ZERO, Vector3D.ZERO);

        // Z
        final Vector3D posInitZ = new Vector3D(0, 0, R);
        final PVCoordinates pvCoordInitZ = new PVCoordinates(posInitZ, Vector3D.ZERO, Vector3D.ZERO);

        // Final PVCoordinates couple after rotation
        // X computed with position (R cos (teta), 0, R sin(teta)), velocity and acceleration with analytical
        // derivatives
        final Vector3D posFinalX = new Vector3D(R * MathLib.cos(expectedRot), 0, R * MathLib.sin(expectedRot));
        final Vector3D velFinalX = new Vector3D(-R * expectedOmega.getNorm() * MathLib.sin(expectedRot), 0,
            R * expectedOmega.getNorm() * MathLib.cos(expectedRot));
        final Vector3D accFinalX = new Vector3D(-R
            * (MathLib.pow(expectedOmega.getNorm(), 2) * MathLib.cos(expectedRot) +
            initOmegaDot.getNorm() * MathLib.sin(expectedRot)), 0, R * (initOmegaDot.getNorm() *
            MathLib.cos(expectedRot) - MathLib.pow(expectedOmega.getNorm(), 2) * MathLib.sin(expectedRot)));
        final PVCoordinates pvCoordFinalX = new PVCoordinates(posFinalX, velFinalX, accFinalX);

        // Z computed with position (- R sin (teta), 0, R cos(teta)), velocity and acceleration with analytical
        // derivatives
        final Vector3D posFinalZ = new Vector3D(-R * MathLib.sin(expectedRot), 0, R * MathLib.cos(expectedRot));
        final Vector3D velFinalZ = new Vector3D(-R * expectedOmega.getNorm() * MathLib.cos(expectedRot), 0,
            -R * expectedOmega.getNorm() * MathLib.sin(expectedRot));
        final Vector3D accFinalZ = new Vector3D(-R * (initOmegaDot.getNorm() * MathLib.cos(expectedRot) -
            MathLib.pow(expectedOmega.getNorm(), 2) * MathLib.sin(expectedRot)), 0, -R * (initOmegaDot.getNorm() *
            MathLib.sin(expectedRot) + MathLib.pow(expectedOmega.getNorm(), 2) * MathLib.cos(expectedRot)));
        final PVCoordinates pvCoordFinalZ = new PVCoordinates(posFinalZ, velFinalZ, accFinalZ);

        // Compute the angular coordinates to validate
        final AngularCoordinates ac = new AngularCoordinates(pvCoordInitX, pvCoordInitZ, pvCoordFinalX, pvCoordFinalZ,
            1e-9, true);

        final Rotation rot = ac.getRotation();
        final Vector3D rotRate = ac.getRotationRate();
        final Vector3D accRate = ac.getRotationAcceleration();

        Assert.assertEquals(expectedRot, rot.getAngle(), 1E-15);
        Assert.assertEquals(0, Vector3D.distance(expectedOmega, rotRate), 1E-15 * expectedOmega.getNorm());
        Assert.assertEquals(0, Vector3D.distance(expectedOmegaDot, accRate), 1E-15 * initOmegaDot.getNorm());

    }

    @Test
    public void testCoverage() {
        final AngularCoordinates ar = new AngularCoordinates();
        Assert.assertEquals(Rotation.IDENTITY, ar.getRotation());
        Assert.assertEquals(Vector3D.ZERO, ar.getRotationRate());
        Assert.assertEquals(Vector3D.ZERO, ar.getRotationAcceleration());
    }

    @Test(expected = PatriusException.class)
    public void testAngularCoordException1() throws PatriusException {
        final PVCoordinates u1 = new PVCoordinates();
        final PVCoordinates u2 = new PVCoordinates();
        final PVCoordinates v1 = new PVCoordinates();
        final PVCoordinates v2 = new PVCoordinates();
        new AngularCoordinates(u1, u2, v1, v2, 1.0E-9, true);
    }

    @Test(expected = PatriusException.class)
    public void testAngularCoordException2() throws PatriusException {

        final RandomGenerator generator = new Well1024a(0x52b29d8f6ac2d64bl);
        final PVCoordinates u1 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
        final PVCoordinates u2 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
        final PVCoordinates v1 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
        final PVCoordinates v2 = randomPVCoordinates(generator, 1000, 1.0, 0.001);

        new AngularCoordinates(u1, u2, v1, v2, 1.0E-14, true);
    }

    @Test
    public void testSerialization() {

        final RandomGenerator generator = new Well1024a(0x49eb5b92d1f94b89l);

        for (int i = 0; i < 500; ++i) {

            final Rotation r = randomRotation(generator);
            final Vector3D omega = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            final Vector3D omegaDot = randomVector(generator, 0.1 * generator.nextDouble() + 0.01);

            final AngularCoordinates c = new AngularCoordinates(r, omega, omegaDot);

            final AngularCoordinates c2 = TestUtils.serializeAndRecover(c);
            assertEqualsAngularCoordinates(c, c2);
        }
    }

    public static void assertEqualsAngularCoordinates(final AngularCoordinates c1, final AngularCoordinates c2) {
        Assert.assertTrue(c1.getRotation().isEqualTo(c2.getRotation()));
        Assert.assertEquals(c1.getRotationRate(), c2.getRotationRate());
        Assert.assertEquals(c1.getRotationAcceleration(), c2.getRotationAcceleration());
    }

    private static PVCoordinates randomPVCoordinates(final RandomGenerator random,
                                              final double norm0, final double norm1, final double norm2) {
        final Vector3D p0 = randomVector(random, norm0);
        final Vector3D p1 = randomVector(random, norm1);
        final Vector3D p2 = randomVector(random, norm2);
        return new PVCoordinates(p0, p1, p2);
    }

    private static void checkInverse(final Vector3D omega, final Vector3D v1, final Vector3D v2) {
        checkInverse(omega,
            v1, Vector3D.crossProduct(omega, v1),
            v2, Vector3D.crossProduct(omega, v2));
    }

    private static void checkInverseFailure(final Vector3D omega, final Vector3D v1,
            final Vector3D c1, final Vector3D v2,
                                     final Vector3D c2) {
        try {
            checkInverse(omega, v1, c1, v2, c2);
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalArgumentException miae) {
            // expected
        }
    }

    private static void checkInverse(final Vector3D omega, final Vector3D v1, final Vector3D c1,
            final Vector3D v2,
                              final Vector3D c2)
                                                throws MathIllegalArgumentException {
        final Vector3D rebuilt = Vector3D.inverseCrossProducts(v1, c1, v2, c2, 1.0e-9);
        Assert.assertEquals(0.0, Vector3D.distance(omega, rebuilt), 5.0e-12 * omega.getNorm());
    }

    private static Vector3D randomVector(final Random random, final double norm) {
        final double n = random.nextDouble() * norm;
        final double x = random.nextDouble();
        final double y = random.nextDouble();
        final double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private static Rotation randomRotation(final Random random) {
        final double q0 = random.nextDouble() * 2 - 1;
        final double q1 = random.nextDouble() * 2 - 1;
        final double q2 = random.nextDouble() * 2 - 1;
        final double q3 = random.nextDouble() * 2 - 1;
        final double q = MathLib.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(false, q0 / q, q1 / q, q2 / q, q3 / q);
    }

    private static Rotation randomRotation(final RandomGenerator random) {
        final double q0 = random.nextDouble() * 2 - 1;
        final double q1 = random.nextDouble() * 2 - 1;
        final double q2 = random.nextDouble() * 2 - 1;
        final double q3 = random.nextDouble() * 2 - 1;
        final double q = MathLib.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(false, q0 / q, q1 / q, q2 / q, q3 / q);
    }

    private static Vector3D randomVector(final RandomGenerator random, final double norm) {
        final double n = random.nextDouble() * norm;
        final double x = random.nextDouble();
        final double y = random.nextDouble();
        final double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }
}
