/**
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
 */
/* Copyright 2002-2015 CS Systèmes d'Information
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
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
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class TimeStampedAngularCoordinatesTest {

    @Test
    public void testZeroRate() throws PatriusException {
        final TimeStampedAngularCoordinates ac =
            new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                new Rotation(false, 0.48, 0.64, 0.36, 0.48),
                Vector3D.ZERO, Vector3D.ZERO);
        Assert.assertEquals(Vector3D.ZERO, ac.getRotationRate());
        final double dt = 10.0;
        final TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationAcceleration());
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationRate());
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-15);
    }

    @Test
    public void testTwoPairs() throws PatriusException, java.io.IOException {
        final RandomGenerator random = new Well1024a(0x976ad943966c9f00l);

        for (int i = 0; i < 20; ++i) {

            final Rotation r = this.randomRotation(random);
            final Vector3D o = this.randomVector(random, 1.0e-2);
            final Vector3D a = this.randomVector(random, 1.0e-2);
            final TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o,
                    a);

            final PVCoordinates u1 = this.randomPVCoordinates(random, 1000, 1.0, 0.001);
            final PVCoordinates u2 = this.randomPVCoordinates(random, 1000, 1.0, 0.001);
            final TimeStampedPVCoordinates tu1 = new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, u1);
            final PVCoordinates v1 = reference.applyTo(u1);
            final PVCoordinates v2 = reference.applyTo(u2);
            final TimeStampedPVCoordinates tv1 = reference.applyTo(tu1);
            Assert.assertEquals(0, v1.getPosition().distance(tv1.getPosition()), 1e-15);
            Assert.assertEquals(0, v1.getAngularVelocity().distance(tv1.getAngularVelocity()), 1e-15);
            Assert.assertEquals(0, v1.getAcceleration().distance(tv1.getAcceleration()), 1e-15);
            final TimeStampedAngularCoordinates tac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, u1, u2, v1, v2, 1.0e-9, true);
            final AngularCoordinates ac =
                new AngularCoordinates(u1, u2, v1, v2, 1.0e-9, true);

            Assert.assertEquals(0,
                Vector3D.distance(v1.getPosition().normalize(), tac.applyTo(u1).getPosition().normalize()), 1.0e-14);
            Assert.assertEquals(0,
                Vector3D.distance(v1.getVelocity().normalize(), tac.applyTo(u1).getVelocity().normalize()), 1.0e-14);
            Assert.assertEquals(0,
                Vector3D.distance(v1.getAcceleration().normalize(), tac.applyTo(u1).getAcceleration().normalize()),
                1.0e-14);
            Assert.assertEquals(0,
                Vector3D.distance(v2.getPosition().normalize(), tac.applyTo(u2).getPosition().normalize()), 1.0e-14);
            Assert.assertEquals(0,
                Vector3D.distance(v2.getVelocity().normalize(), tac.applyTo(u2).getVelocity().normalize()), 1.0e-14);
            Assert.assertEquals(0,
                Vector3D.distance(v2.getAcceleration().normalize(), tac.applyTo(u2).getAcceleration().normalize()),
                1.0e-14);

            // check that AngularCoordinates and TimeStampedAngularCoordinates are equals
            Assert.assertTrue(ac.getRotation().isEqualTo(tac.getRotation()));
            Assert.assertEquals(0, Vector3D.distance(ac.getRotationRate(), tac.getRotationRate()), 0.);
            Assert.assertEquals(0, Vector3D.distance(ac.getRotationAcceleration(), tac.getRotationAcceleration()), 0.);

        }
    }

    @Test
    public void testShift() throws PatriusException {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final TimeStampedAngularCoordinates ac =
            new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                Rotation.IDENTITY,
                new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assert.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        final double dt = 10.0;
        final double alpha = rate * dt;
        final TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-10);

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
    public void testSpin() throws PatriusException {
        final double rate = 2 * FastMath.PI / (12 * 60);
        final TimeStampedAngularCoordinates ac =
            new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                new Rotation(false, 0.48, 0.64, 0.36, 0.48),
                new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assert.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        final double dt = 10.0;
        final TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(rate * dt, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-10);

        final Vector3D shiftedX = shifted.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D shiftedY = shifted.getRotation().applyTo(Vector3D.PLUS_J);
        final Vector3D shiftedZ = shifted.getRotation().applyTo(Vector3D.PLUS_K);
        final Vector3D originalX = ac.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D originalY = ac.getRotation().applyTo(Vector3D.PLUS_J);
        final Vector3D originalZ = ac.getRotation().applyTo(Vector3D.PLUS_K);
        Assert.assertEquals(MathLib.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assert.assertEquals(MathLib.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assert.assertEquals(-MathLib.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assert.assertEquals(MathLib.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assert.assertEquals(0.0, Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assert.assertEquals(1.0, Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        final Vector3D forward = AngularCoordinates.estimateRate(ac.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(ac.getRotationRate()).getNorm(), 1.0e-10);

        final Vector3D reversed = AngularCoordinates.estimateRate(shifted.getRotation(), ac.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(ac.getRotationRate()).getNorm(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        final RandomGenerator random = new Well1024a(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            final Rotation r = this.randomRotation(random);
            final Vector3D o = this.randomVector(random, 1.0e-3);
            final Vector3D a = this.randomVector(random, 1.0e-3);
            final TimeStampedAngularCoordinates ac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, a);
            final TimeStampedAngularCoordinates sum = ac.addOffset(ac.revert(true), true);
            Assert.assertEquals(0.0, sum.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationAcceleration().getNorm(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        final TimeStampedAngularCoordinates ac1 =
            new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                new Rotation(false, 0.48, 0.64, 0.36, 0.48), Vector3D.ZERO, Vector3D.ZERO);
        final TimeStampedAngularCoordinates ac2 =
            new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                new Rotation(false, 0.36, -0.48, 0.48, 0.64), Vector3D.ZERO, Vector3D.ZERO);

        final TimeStampedAngularCoordinates add12 = ac1.addOffset(ac2);
        final TimeStampedAngularCoordinates add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, Rotation.distance(add12.getRotation(), add21.getRotation()), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        final RandomGenerator random = new Well1024a(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            final Rotation r1 = this.randomRotation(random);
            final Vector3D o1 = this.randomVector(random, 1.0e-2);
            final Vector3D a1 = this.randomVector(random, 1.0e-2);
            final TimeStampedAngularCoordinates ac1 =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r1, o1, a1);
            final Rotation r2 = this.randomRotation(random);
            final Vector3D o2 = this.randomVector(random, 1.0e-2);
            final Vector3D a2 = this.randomVector(random, 1.0e-2);

            final TimeStampedAngularCoordinates ac2 =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r2, o2, a2);
            final TimeStampedAngularCoordinates roundTripSA = ac1.subtractOffset(ac2, true).addOffset(ac2, true);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripSA.getRotation()), 4.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()), 2.0e-17);
            Vector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration());
            Assert.assertEquals(0.0,
                Vector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()), 1.0e-16);

            final TimeStampedAngularCoordinates roundTripAS = ac1.addOffset(ac2, true).subtractOffset(ac2, true);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripAS.getRotation()), 6.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()), 2.0e-17);
            Assert.assertEquals(0.0,
                Vector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()), 2.0e-16);
        }
    }

    @Test
    public void testInterpolationAroundPI() throws PatriusException {

        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();

        // add angular coordinates at t0: 179.999 degrees rotation along X axis
        final AbsoluteDate t0 = new AbsoluteDate("2012-01-01T00:00:00.000", TimeScalesFactory.getTAI());
        final TimeStampedAngularCoordinates ac0 = new TimeStampedAngularCoordinates(t0,
            new Rotation(Vector3D.PLUS_I, MathLib.toRadians(179.999)),
            new Vector3D(MathLib.toRadians(0), 0, 0),
            Vector3D.ZERO);
        sample.add(ac0);

        // add angular coordinates at t1: -179.999 degrees rotation (= 180.001 degrees) along X axis
        final AbsoluteDate t1 = new AbsoluteDate("2012-01-01T00:00:02.000", TimeScalesFactory.getTAI());
        final TimeStampedAngularCoordinates ac1 = new TimeStampedAngularCoordinates(t1,
            new Rotation(Vector3D.PLUS_I, MathLib.toRadians(-179.999)),
            new Vector3D(MathLib.toRadians(0), 0, 0),
            Vector3D.ZERO);
        sample.add(ac1);

        // get interpolated angular coordinates at mid time between t0 and t1
        final AbsoluteDate t = new AbsoluteDate("2012-01-01T00:00:01.000", TimeScalesFactory.getTAI());
        final TimeStampedAngularCoordinates interpolated =
            TimeStampedAngularCoordinates.interpolate(t, AngularDerivativesFilter.USE_R, sample);

        Assert.assertEquals(MathLib.toRadians(180), interpolated.getRotation().getAngle(), 1.0e-12);

    }

    @Test
    public void testInterpolationWithoutAcceleration() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        final double alpha0 = 0.5 * FastMath.PI;
        final double omega = 0.05 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
            new TimeStampedAngularCoordinates(date,
                new Rotation(Vector3D.PLUS_K, alpha0),
                new Vector3D(omega, Vector3D.MINUS_K),
                Vector3D.ZERO);
        final double[] errors = this.interpolationErrors(reference, 1.0, false);
        Assert.assertEquals(0.0, errors[0], 1.0e-15);
        Assert.assertEquals(0.0, errors[1], 3.0e-15);
        Assert.assertEquals(0.0, errors[2], 3.0e-14);
    }

    @Test
    public void testInterpolationWithAcceleration() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        final double alpha0 = 0.5 * FastMath.PI;
        final double omega = 0.05 * FastMath.PI;
        final double eta = 0.005 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
            new TimeStampedAngularCoordinates(date,
                new Rotation(Vector3D.PLUS_K, alpha0),
                new Vector3D(omega, Vector3D.MINUS_K),
                new Vector3D(eta, Vector3D.PLUS_J));
        final double[] errors = this.interpolationErrors(reference, 1.0, true);
        Assert.assertEquals(0.0, errors[0], 3.0e-5);
        Assert.assertEquals(0.0, errors[1], 2.0e-4);
        Assert.assertEquals(0.0, errors[2], 4.6e-3);
    }

    private double[] interpolationErrors(final TimeStampedAngularCoordinates reference, final double dt,
                                         final boolean setSpinDerivatives)
                                                                          throws PatriusException {

        final FirstOrderDifferentialEquations ode = new FirstOrderDifferentialEquations(){
            @Override
            public int getDimension() {
                return 4;
            }

            @Override
            public void computeDerivatives(final double t, final double[] q, final double[] qDot) {
                final double omegaX = reference.getRotationRate().getX() + t
                    * reference.getRotationAcceleration().getX();
                final double omegaY = reference.getRotationRate().getY() + t
                    * reference.getRotationAcceleration().getY();
                final double omegaZ = reference.getRotationRate().getZ() + t
                    * reference.getRotationAcceleration().getZ();
                qDot[0] = 0.5 * MathArrays.linearCombination(-q[1], omegaX, -q[2], omegaY, -q[3], omegaZ);
                qDot[1] = 0.5 * MathArrays.linearCombination(q[0], omegaX, -q[3], omegaY, q[2], omegaZ);
                qDot[2] = 0.5 * MathArrays.linearCombination(q[3], omegaX, q[0], omegaY, -q[1], omegaZ);
                qDot[3] = 0.5 * MathArrays.linearCombination(-q[2], omegaX, q[1], omegaY, q[0], omegaZ);
            }
        };
        final List<TimeStampedAngularCoordinates> complete = new ArrayList<TimeStampedAngularCoordinates>();
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(1.0e-6, 1.0, 1.0e-12, 1.0e-12);
        integrator.addStepHandler(new StepNormalizer(dt / 2000, new FixedStepHandler(){
            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public void handleStep(final double t, final double[] y, final double[] yDot, final boolean isLast) {
                complete.add(new TimeStampedAngularCoordinates(reference.getDate().shiftedBy(t),
                    new Rotation(true, y[0], y[1], y[2], y[3]),
                    new Vector3D(1, reference.getRotationRate(),
                        t, reference.getRotationAcceleration()),
                    reference.getRotationAcceleration()));
            }
        }));

        final double[] y = new double[] {
            reference.getRotation().getQuaternion().getQ0(),
            reference.getRotation().getQuaternion().getQ1(),
            reference.getRotation().getQuaternion().getQ2(),
            reference.getRotation().getQuaternion().getQ3()
        };
        integrator.integrate(ode, 0, y, dt, y);

        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        sample.add(complete.get(0));
        sample.add(complete.get(complete.size() / 2));
        sample.add(complete.get(complete.size() - 1));

        double maxRotationError = 0;
        double maxRateError = 0;
        double maxAccelerationError = 0;
        for (final TimeStampedAngularCoordinates acRef : complete) {
            final TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates.interpolate(acRef.getDate(), AngularDerivativesFilter.USE_RRA,
                    sample, setSpinDerivatives);
            final double rotationError = Rotation.distance(acRef.getRotation(), interpolated.getRotation());
            final double rateError = Vector3D.distance(acRef.getRotationRate(), interpolated.getRotationRate());
            final double accelerationError = setSpinDerivatives ? Vector3D.distance(acRef.getRotationAcceleration(),
                interpolated.getRotationAcceleration()) : 0.;
            maxRotationError = MathLib.max(maxRotationError, rotationError);
            maxRateError = MathLib.max(maxRateError, rateError);
            maxAccelerationError = MathLib.max(maxAccelerationError, accelerationError);
        }

        return new double[] {
            maxRotationError, maxRateError, maxAccelerationError
        };

    }

    @Test
    public void testInterpolationNeedOffsetWrongRate() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        final double omega = 2.0 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
            new TimeStampedAngularCoordinates(date,
                Rotation.IDENTITY,
                new Vector3D(omega, Vector3D.MINUS_K),
                Vector3D.ZERO);

        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (final double dt : new double[] { 0.0, 0.25, 0.5, 0.75, 1.0 }) {
            final TimeStampedAngularCoordinates shifted = reference.shiftedBy(dt);
            sample.add(new TimeStampedAngularCoordinates(shifted.getDate(),
                shifted.getRotation(),
                Vector3D.ZERO, Vector3D.ZERO));
        }

        for (final TimeStampedAngularCoordinates s : sample) {
            final TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates.interpolate(s.getDate(), AngularDerivativesFilter.USE_RR, sample);
            final Rotation r = interpolated.getRotation();
            final Vector3D rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, Rotation.distance(s.getRotation(), r), 2.0e-14);
            Assert.assertEquals(0.0, Vector3D.distance(s.getRotationRate(), rate), 2.0e-13);
        }

    }

    @Test
    public void testInterpolationRotationOnly() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        final double alpha0 = 0.5 * FastMath.PI;
        final double omega = 0.5 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
            new TimeStampedAngularCoordinates(date,
                new Rotation(Vector3D.PLUS_K, alpha0),
                new Vector3D(omega, Vector3D.MINUS_K),
                Vector3D.ZERO);

        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (final double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
            final Rotation r = reference.shiftedBy(dt).getRotation();
            sample.add(new TimeStampedAngularCoordinates(date.shiftedBy(dt), r, Vector3D.ZERO, Vector3D.ZERO));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            final TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates.interpolate(date.shiftedBy(dt), AngularDerivativesFilter.USE_R,
                    sample);
            final Rotation r = interpolated.getRotation();
            final Vector3D rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, Rotation.distance(reference.shiftedBy(dt).getRotation(), r), 3.0e-4);
            Assert.assertEquals(0.0, Vector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate), 1.0e-2);
        }

    }

    @Test
    public void testInterpolationTooSmallSample() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        final double alpha0 = 0.5 * FastMath.PI;
        final double omega = 0.5 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
            new TimeStampedAngularCoordinates(date,
                new Rotation(Vector3D.PLUS_K, alpha0),
                new Vector3D(omega, Vector3D.MINUS_K),
                Vector3D.ZERO);

        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        final Rotation r = reference.shiftedBy(0.2).getRotation();
        sample.add(new TimeStampedAngularCoordinates(date.shiftedBy(0.2), r, Vector3D.ZERO, Vector3D.ZERO));

        try {
            TimeStampedAngularCoordinates.interpolate(date.shiftedBy(0.3), AngularDerivativesFilter.USE_R, sample);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException oe) {
            Assert.assertEquals(PatriusMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION, oe.getSpecifier());
            Assert.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testInterpolationGTODIssue() throws PatriusException {
        final AbsoluteDate t0 = new AbsoluteDate("2004-04-06T19:59:28.000", TimeScalesFactory.getTAI());
        final double[][] params = new double[][] {
            { 0.0, -0.3802356750911964, -0.9248896320037013, 7.292115030462892e-5 },
            { 4.0, 0.1345716955788532, -0.990903859488413, 7.292115033301528e-5 },
            { 8.0, -0.613127541102373, 0.7899839354960061, 7.292115037371062e-5 }
        };
        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (final double[] row : params) {
            final AbsoluteDate t = t0.shiftedBy(row[0] * 3600.0);
            final Rotation r = new Rotation(false, row[1], 0.0, 0.0, row[2]);
            final Vector3D o = new Vector3D(row[3], Vector3D.PLUS_K);
            sample.add(new TimeStampedAngularCoordinates(t, r, o, Vector3D.ZERO));
        }
        for (double dt = 0; dt < 29000; dt += 120) {
            final TimeStampedAngularCoordinates shifted = sample.get(0).shiftedBy(dt);
            final TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates
                    .interpolate(t0.shiftedBy(dt), AngularDerivativesFilter.USE_RR, sample);
            Assert.assertEquals(0.0,
                Rotation.distance(shifted.getRotation(), interpolated.getRotation()),
                1.3e-7);
            Assert.assertEquals(0.0,
                Vector3D.distance(shifted.getRotationRate(), interpolated.getRotationRate()),
                1.0e-11);
        }

    }

    @Test
    public void testSerialization() {

        // random test
        final AbsoluteDate[] dates =
        { AbsoluteDate.J2000_EPOCH, AbsoluteDate.CCSDS_EPOCH, AbsoluteDate.FIFTIES_EPOCH_UTC,
            new AbsoluteDate("2004-04-06T19:59:28.000", TimeScalesFactory.getTAI()),
            new AbsoluteDate("2012-01-01T00:00:01.000", TimeScalesFactory.getTAI()) };
        final RandomGenerator generator = new Well1024a(0x49eb5b92d1f94b89l);

        for (final AbsoluteDate date : dates) {

            final Rotation r = this.randomRotation(generator);
            final Vector3D omega = this.randomVector(generator, 10 * generator.nextDouble() + 1.0);
            final Vector3D omegaDot = this.randomVector(generator, 0.1 * generator.nextDouble() + 0.01);

            final TimeStampedAngularCoordinates c = new TimeStampedAngularCoordinates(date, r, omega, omegaDot);

            final TimeStampedAngularCoordinates c2 = TestUtils.serializeAndRecover(c);
            assertEqualsTimeStampedAngularCoordinates(c, c2);
        }

    }

    public static void assertEqualsTimeStampedAngularCoordinates(final TimeStampedAngularCoordinates c1,
                                                                 final TimeStampedAngularCoordinates c2) {
        AngularCoordinatesTest.assertEqualsAngularCoordinates(c1, c2);
        Assert.assertEquals(c1.getDate(), c2.getDate());
    }

    private Vector3D randomVector(final RandomGenerator random, final double norm) {
        final double n = random.nextDouble() * norm;
        final double x = 2 * random.nextDouble() - 1;
        final double y = 2 * random.nextDouble() - 1;
        final double z = 2 * random.nextDouble() - 1;
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private PVCoordinates randomPVCoordinates(final RandomGenerator random,
                                              final double norm0, final double norm1, final double norm2) {
        final Vector3D p0 = this.randomVector(random, norm0);
        final Vector3D p1 = this.randomVector(random, norm1);
        final Vector3D p2 = this.randomVector(random, norm2);
        return new PVCoordinates(p0, p1, p2);
    }

    private Rotation randomRotation(final RandomGenerator random) {
        final double q0 = random.nextDouble() * 2 - 1;
        final double q1 = random.nextDouble() * 2 - 1;
        final double q2 = random.nextDouble() * 2 - 1;
        final double q3 = random.nextDouble() * 2 - 1;
        final double q = MathLib.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(false, q0 / q, q1 / q, q2 / q, q3 / q);
    }

}