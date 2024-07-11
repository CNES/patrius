/**
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
/* Copyright 2002-2015 CS Systèmes d'Information
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.FieldVector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937a;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolationFunctionBuilder;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TimeStampedPVCoordinatesTest {

    @Test
    public void testPVOnlyConstructor() {
        // setup
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D p = new Vector3D(1, 2, 3);
        final Vector3D v = new Vector3D(4, 5, 6);

        // action
        final TimeStampedPVCoordinates actual = new TimeStampedPVCoordinates(date, p, v);

        // verify
        Assert.assertEquals(date, actual.getDate());
        Assert.assertEquals(1, actual.getPosition().getX(), 0);
        Assert.assertEquals(2, actual.getPosition().getY(), 0);
        Assert.assertEquals(3, actual.getPosition().getZ(), 0);
        Assert.assertEquals(4, actual.getVelocity().getX(), 0);
        Assert.assertEquals(5, actual.getVelocity().getY(), 0);
        Assert.assertEquals(6, actual.getVelocity().getZ(), 0);
        Assert.assertNull(actual.getAcceleration());
    }

    @Test
    public void testPVCoordinatesCopyConstructor() {
        // setup
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));

        // action
        final TimeStampedPVCoordinates actual = new TimeStampedPVCoordinates(date, pv);

        // verify
        Assert.assertEquals(date, actual.getDate());
        Assert.assertEquals(1, actual.getPosition().getX(), 0);
        Assert.assertEquals(2, actual.getPosition().getY(), 0);
        Assert.assertEquals(3, actual.getPosition().getZ(), 0);
        Assert.assertEquals(4, actual.getVelocity().getX(), 0);
        Assert.assertEquals(5, actual.getVelocity().getY(), 0);
        Assert.assertEquals(6, actual.getVelocity().getZ(), 0);
        Assert.assertNull(actual.getAcceleration());
    }

    @Test
    public void testLinearConstructors() {
        final TimeStampedPVCoordinates pv1 = new TimeStampedPVCoordinates(AbsoluteDate.CCSDS_EPOCH,
            new Vector3D(1, 0.1, 10),
            new Vector3D(-1, -0.1, -10),
            new Vector3D(10, -1.0, -100));
        final TimeStampedPVCoordinates pv2 = new TimeStampedPVCoordinates(AbsoluteDate.CCSDS_EPOCH,
            new Vector3D(2, 0.2, 20),
            new Vector3D(-2, -0.2, -20),
            new Vector3D(20, -2.0, -200));
        final TimeStampedPVCoordinates pv3 = new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH,
            new Vector3D(3, 0.3, 30),
            new Vector3D(-3, -0.3, -30),
            new Vector3D(30, -3.0, -300));
        final TimeStampedPVCoordinates pv4 = new TimeStampedPVCoordinates(AbsoluteDate.JULIAN_EPOCH,
            new Vector3D(4, 0.4, 40),
            new Vector3D(-4, -0.4, -40),
            new Vector3D(40, -4.0, -400));
        checkPV(pv4, new TimeStampedPVCoordinates(AbsoluteDate.JULIAN_EPOCH, 4, pv1), 1.0e-15);
        checkPV(pv2, new TimeStampedPVCoordinates(AbsoluteDate.CCSDS_EPOCH, pv1, pv3), 1.0e-15);
        checkPV(pv3, new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH, 1, pv1, 1, pv2), 1.0e-15);
        checkPV(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, 2, pv4),
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv3),
            1.0e-15);
        checkPV(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv3),
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, 3, pv1, 1, pv2, 1, pv4),
            1.0e-15);
        checkPV(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, 5, pv4),
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, 4, pv1, 3, pv2, 2, pv3, 1, pv4),
            1.0e-15);
    }

    @Test
    public void testToDerivativeStructureVector2() throws PatriusException {
        final FieldVector3D<DerivativeStructure> fv =
            new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH,
                new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, -1.0, -100)).toDerivativeStructureVector(2);
        Assert.assertEquals(1, fv.getX().getFreeParameters());
        Assert.assertEquals(2, fv.getX().getOrder());
        Assert.assertEquals(1.0, fv.getX().getReal(), 1.0e-10);
        Assert.assertEquals(0.1, fv.getY().getReal(), 1.0e-10);
        Assert.assertEquals(10.0, fv.getZ().getReal(), 1.0e-10);
        Assert.assertEquals(-1.0, fv.getX().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(-0.1, fv.getY().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(-10.0, fv.getZ().getPartialDerivative(1), 1.0e-15);
        Assert.assertEquals(10.0, fv.getX().getPartialDerivative(2), 1.0e-15);
        Assert.assertEquals(-1.0, fv.getY().getPartialDerivative(2), 1.0e-15);
        Assert.assertEquals(-100.0, fv.getZ().getPartialDerivative(2), 1.0e-15);
        checkPV(new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH,
            new Vector3D(1, 0.1, 10),
            new Vector3D(-1, -0.1, -10),
            new Vector3D(10, -1.0, -100)),
            new TimeStampedPVCoordinates(AbsoluteDate.GALILEO_EPOCH, fv), 1.0e-15);

        for (double dt = 0; dt < 10; dt += 0.125) {
            final Vector3D p = new PVCoordinates(new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, -1.0, -100)).shiftedBy(dt).getPosition();
            Assert.assertEquals(p.getX(), fv.getX().taylor(dt), 1.0e-14);
            Assert.assertEquals(p.getY(), fv.getY().taylor(dt), 1.0e-14);
            Assert.assertEquals(p.getZ(), fv.getZ().taylor(dt), 1.0e-14);
        }
    }

    @Test
    public void testShift() {
        final Vector3D p1 = new Vector3D(1, 0.1, 10);
        final Vector3D v1 = new Vector3D(-1, -0.1, -10);
        final Vector3D a1 = new Vector3D(10, 1.0, 100);
        final Vector3D p2 = new Vector3D(7, 0.7, 70);
        final Vector3D v2 = new Vector3D(-11, -1.1, -110);
        final Vector3D a2 = new Vector3D(10, 1.0, 100);
        checkPV(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, p2, v2, a2),
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(1.0), p1, v1, a1).shiftedBy(-1.0), 1.0e-15);
        Assert.assertEquals(
            0.0,
            PVCoordinates.estimateVelocity(p1, p2, -1.0).subtract(new Vector3D(-6, -0.6, -60)).getNorm(),
            1.0e-15);
    }

    @Test
    public void testToString() {
        Utils.setDataRoot("regular-data");
        final TimeStampedPVCoordinates pv1 =
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10),
                new Vector3D(10, 1.0, 100));
        Assert.assertEquals("{2000-01-01T11:59:27.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0), A(10.0, 1.0, 100.0)}",
            pv1.toString());

        final TimeStampedPVCoordinates pv2 =
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                new Vector3D(1, 0.1, 10),
                new Vector3D(-1, -0.1, -10));
        Assert.assertEquals("{2000-01-01T11:59:27.816, P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0)}", pv2.toString());
    }

    @Test
    public void testInterpolatePolynomialPVA() {
        final Random random = new Random(0xfe3945fcb8bf47cel);
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            final PolynomialFunction px = randomPolynomial(5, random);
            final PolynomialFunction py = randomPolynomial(5, random);
            final PolynomialFunction pz = randomPolynomial(5, random);
            final PolynomialFunction pxDot = px.polynomialDerivative();
            final PolynomialFunction pyDot = py.polynomialDerivative();
            final PolynomialFunction pzDot = pz.polynomialDerivative();
            final PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            final PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            final PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
            for (final double dt : new double[] { 0.0, 0.5, 1.0 }) {
                final Vector3D position = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                final Vector3D velocity = new Vector3D(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt));
                final Vector3D acceleration = new Vector3D(pxDotDot.value(dt), pyDotDot.value(dt), pzDotDot.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity, acceleration));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                final TimeStampedPVCoordinates interpolated =
                    TimeStampedPVCoordinates.interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_PVA,
                        sample);
                final Vector3D p = interpolated.getPosition();
                final Vector3D v = interpolated.getVelocity();
                final Vector3D a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt), p.getX(), 4.0e-16 * p.getNorm());
                Assert.assertEquals(py.value(dt), p.getY(), 4.0e-16 * p.getNorm());
                Assert.assertEquals(pz.value(dt), p.getZ(), 4.0e-16 * p.getNorm());
                Assert.assertEquals(pxDot.value(dt), v.getX(), 9.0e-16 * v.getNorm());
                Assert.assertEquals(pyDot.value(dt), v.getY(), 9.0e-16 * v.getNorm());
                Assert.assertEquals(pzDot.value(dt), v.getZ(), 9.0e-16 * v.getNorm());
                Assert.assertEquals(pxDotDot.value(dt), a.getX(), 9.0e-15 * a.getNorm());
                Assert.assertEquals(pyDotDot.value(dt), a.getY(), 9.0e-15 * a.getNorm());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ(), 9.0e-15 * a.getNorm());
            }
        }
    }

    @Test
    public void testInterpolatePolynomialPV() {
        final Random random = new Random(0xae7771c9933407bdl);
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            final PolynomialFunction px = randomPolynomial(5, random);
            final PolynomialFunction py = randomPolynomial(5, random);
            final PolynomialFunction pz = randomPolynomial(5, random);
            final PolynomialFunction pxDot = px.polynomialDerivative();
            final PolynomialFunction pyDot = py.polynomialDerivative();
            final PolynomialFunction pzDot = pz.polynomialDerivative();
            final PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            final PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            final PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
            for (final double dt : new double[] { 0.0, 0.5, 1.0 }) {
                final Vector3D position = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                final Vector3D velocity = new Vector3D(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity, Vector3D.ZERO));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                final TimeStampedPVCoordinates interpolated =
                    TimeStampedPVCoordinates.interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_PV,
                        sample);
                final Vector3D p = interpolated.getPosition();
                final Vector3D v = interpolated.getVelocity();
                final Vector3D a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt), p.getX(), 4.0e-16 * p.getNorm());
                Assert.assertEquals(py.value(dt), p.getY(), 4.0e-16 * p.getNorm());
                Assert.assertEquals(pz.value(dt), p.getZ(), 4.0e-16 * p.getNorm());
                Assert.assertEquals(pxDot.value(dt), v.getX(), 9.0e-16 * v.getNorm());
                Assert.assertEquals(pyDot.value(dt), v.getY(), 9.0e-16 * v.getNorm());
                Assert.assertEquals(pzDot.value(dt), v.getZ(), 9.0e-16 * v.getNorm());
                Assert.assertEquals(pxDotDot.value(dt), a.getX(), 1.0e-14 * a.getNorm());
                Assert.assertEquals(pyDotDot.value(dt), a.getY(), 1.0e-14 * a.getNorm());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ(), 1.0e-14 * a.getNorm());
            }
        }
    }

    @Test
    public void testInterpolatePolynomialPositionOnly() {
        final Random random = new Random(0x88740a12e4299003l);
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            final PolynomialFunction px = randomPolynomial(5, random);
            final PolynomialFunction py = randomPolynomial(5, random);
            final PolynomialFunction pz = randomPolynomial(5, random);
            final PolynomialFunction pxDot = px.polynomialDerivative();
            final PolynomialFunction pyDot = py.polynomialDerivative();
            final PolynomialFunction pzDot = pz.polynomialDerivative();
            final PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            final PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            final PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
            for (final double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                final Vector3D position = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, Vector3D.ZERO, Vector3D.ZERO));
            }

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                final TimeStampedPVCoordinates interpolated =
                    TimeStampedPVCoordinates
                        .interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_P, sample);
                final Vector3D p = interpolated.getPosition();
                final Vector3D v = interpolated.getVelocity();
                final Vector3D a = interpolated.getAcceleration();
                Assert.assertEquals(px.value(dt), p.getX(), 5.0e-16 * p.getNorm());
                Assert.assertEquals(py.value(dt), p.getY(), 5.0e-16 * p.getNorm());
                Assert.assertEquals(pz.value(dt), p.getZ(), 5.0e-16 * p.getNorm());
                Assert.assertEquals(pxDot.value(dt), v.getX(), 7.0e-15 * v.getNorm());
                Assert.assertEquals(pyDot.value(dt), v.getY(), 7.0e-15 * v.getNorm());
                Assert.assertEquals(pzDot.value(dt), v.getZ(), 7.0e-15 * v.getNorm());
                Assert.assertEquals(pxDotDot.value(dt), a.getX(), 2.0e-13 * a.getNorm());
                Assert.assertEquals(pyDotDot.value(dt), a.getY(), 2.0e-13 * a.getNorm());
                Assert.assertEquals(pzDotDot.value(dt), a.getZ(), 2.0e-13 * a.getNorm());
            }
        }
    }

    @Test
    public void testInterpolateNonPolynomial() {
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
        for (final double dt : new double[] { 0.0, 0.5, 1.0 }) {
            final Vector3D position = new Vector3D(MathLib.cos(dt), MathLib.sin(dt), 0.0);
            final Vector3D velocity = new Vector3D(-MathLib.sin(dt), MathLib.cos(dt), 0.0);
            final Vector3D acceleration = new Vector3D(-MathLib.cos(dt), -MathLib.sin(dt), 0.0);
            sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity, acceleration));
        }

        for (double dt = 0; dt < 1.0; dt += 0.01) {
            final TimeStampedPVCoordinates interpolated =
                TimeStampedPVCoordinates.interpolate(t0.shiftedBy(dt), CartesianDerivativesFilter.USE_PVA, sample);
            final Vector3D p = interpolated.getPosition();
            final Vector3D v = interpolated.getVelocity();
            final Vector3D a = interpolated.getAcceleration();
            Assert.assertEquals(MathLib.cos(dt), p.getX(), 3e-10 * p.getNorm());
            Assert.assertEquals(MathLib.sin(dt), p.getY(), 3e-10 * p.getNorm());
            Assert.assertEquals(0, p.getZ(), 3e-10 * p.getNorm());
            Assert.assertEquals(-MathLib.sin(dt), v.getX(), 3e-9 * v.getNorm());
            Assert.assertEquals(MathLib.cos(dt), v.getY(), 3e-9 * v.getNorm());
            Assert.assertEquals(0, v.getZ(), 3e-9 * v.getNorm());
            Assert.assertEquals(-MathLib.cos(dt), a.getX(), 4e-8 * a.getNorm());
            Assert.assertEquals(-MathLib.sin(dt), a.getY(), 4e-8 * a.getNorm());
            Assert.assertEquals(0, a.getZ(), 4e-8 * a.getNorm());
        }
    }

    /** Interpolation function with {@link CartesianDerivativesFilter#USE_P P} filter with acceleration computation. */
    private static final TimeStampedInterpolationFunctionBuilder<TimeStampedPVCoordinates,
            TimeStampedPVCoordinates> INTERP_FUNC_BUILDER_P_TRUE =
                (samples, indexInf, indexSup) -> TimeStampedPVCoordinates
                    .buildInterpolationFunction(samples, indexInf, indexSup, CartesianDerivativesFilter.USE_P, true);

    /** Interpolation function with {@link CartesianDerivativesFilter#USE_PV PV} filter with acceleration computation. */
    private static final TimeStampedInterpolationFunctionBuilder<TimeStampedPVCoordinates,
            TimeStampedPVCoordinates> INTERP_FUNC_BUILDER_PV_TRUE = (samples, indexInf, indexSup) -> TimeStampedPVCoordinates
                .buildInterpolationFunction(samples, indexInf, indexSup, CartesianDerivativesFilter.USE_PV, true);

    /** Interpolation function with {@link CartesianDerivativesFilter#USE_PVA PVA} filter with acceleration computation. */
    private static final TimeStampedInterpolationFunctionBuilder<TimeStampedPVCoordinates,
            TimeStampedPVCoordinates> INTERP_FUNC_BUILDER_PVA_TRUE = (samples, indexInf, indexSup) -> TimeStampedPVCoordinates
                .buildInterpolationFunction(samples, indexInf, indexSup, CartesianDerivativesFilter.USE_PVA, true);

    /** Interpolation function with {@link CartesianDerivativesFilter#USE_P P} filter without acceleration computation. */
    private static final TimeStampedInterpolationFunctionBuilder<TimeStampedPVCoordinates,
            TimeStampedPVCoordinates> INTERP_FUNC_BUILDER_P_FALSE =
                (samples, indexInf, indexSup) -> TimeStampedPVCoordinates
                    .buildInterpolationFunction(samples, indexInf, indexSup, CartesianDerivativesFilter.USE_P, false);

    /** Interpolation function with {@link CartesianDerivativesFilter#USE_PV PV} filter without acceleration computation. */
    private static final TimeStampedInterpolationFunctionBuilder<TimeStampedPVCoordinates,
            TimeStampedPVCoordinates> INTERP_FUNC_BUILDER_PV_FALSE = (samples, indexInf, indexSup) -> TimeStampedPVCoordinates
                .buildInterpolationFunction(samples, indexInf, indexSup, CartesianDerivativesFilter.USE_PV, false);

    /** Interpolation function with {@link CartesianDerivativesFilter#USE_PVA PVA} filter without acceleration computation. */
    private static final TimeStampedInterpolationFunctionBuilder<TimeStampedPVCoordinates,
            TimeStampedPVCoordinates> INTERP_FUNC_BUILDER_PVA_FALSE = (samples, indexInf, indexSup) -> TimeStampedPVCoordinates
                .buildInterpolationFunction(samples, indexInf, indexSup, CartesianDerivativesFilter.USE_PVA, false);

    /**
     * @description Evaluate the interpolation function behavior.
     *
     * @testedMethod {@link TimeStampedPVCoordinates#buildInterpolationFunction(TimeStampedPVCoordinates[], int, int,
     *               CartesianDerivativesFilter, boolean)}
     *
     * @testPassCriteria The interpolation function is build as expected and generate the expected data.
     */
    @Test
    public void testInterpolationFunction() {

        // Initialize some samples for the interpolation
        final int size = 4;
        final AbsoluteDate refDate = AbsoluteDate.J2000_EPOCH;

        final TimeStampedPVCoordinates[] samples = new TimeStampedPVCoordinates[size];
        for (int i = 0; i < size; i++) {
            final AbsoluteDate date = refDate.shiftedBy(60. * i);
            final Vector3D pos = new Vector3D(7179992.82 + i * 10.2, 2276.519 - i * 10.1, -14178.396 + i * 10.8);
            final Vector3D vel = new Vector3D(7.450848 - i * 0.7, -1181.198684 + i * 0.2, 7356.62864 + i * 0.66);
            final Vector3D acc = new Vector3D(i * 0.65, -1.184 + i * 0.12, 7.64 + i * 0.06);
            samples[i] = new TimeStampedPVCoordinates(date, pos, vel, acc);
        }

        final double duration = samples[samples.length - 1].getDate().durationFrom(samples[0].getDate());
        final int subSize = 10;
        final double step = duration / (size * subSize);

        final boolean[] computeAccelerationArray = new boolean[] { false, true };

        // Loop over each boolean case
        for (final boolean computeAcceleration : computeAccelerationArray) {

            // Loop on each filter to evaluate them all
            for (final CartesianDerivativesFilter filter : CartesianDerivativesFilter.values()) {

                // Build the interpolation function depending on the selecting filter
                final Function<AbsoluteDate, ? extends TimeStampedPVCoordinates> interpolationFct;
                switch (filter) {
                    case USE_P:
                        if (computeAcceleration) {
                            interpolationFct = INTERP_FUNC_BUILDER_P_TRUE.buildInterpolationFunction(samples, 0, size);
                        } else {
                            interpolationFct = INTERP_FUNC_BUILDER_P_FALSE.buildInterpolationFunction(samples, 0, size);
                        }
                        break;
                    case USE_PV:
                        if (computeAcceleration) {
                            interpolationFct = INTERP_FUNC_BUILDER_PV_TRUE.buildInterpolationFunction(samples, 0, size);
                        } else {
                            interpolationFct = INTERP_FUNC_BUILDER_PV_FALSE.buildInterpolationFunction(samples, 0, size);
                        }
                        break;
                    case USE_PVA:
                        if (computeAcceleration) {
                            interpolationFct = INTERP_FUNC_BUILDER_PVA_TRUE.buildInterpolationFunction(samples, 0, size);
                        } else {
                            interpolationFct = INTERP_FUNC_BUILDER_PVA_FALSE.buildInterpolationFunction(samples, 0, size);
                        }
                        break;
                    default:
                        throw new EnumConstantNotPresentException(CartesianDerivativesFilter.class, "filter");
                }

                // Build the expected interpolator on the samples according to the filter
                final HermiteInterpolator interpolator = new HermiteInterpolator();

                for (int i = 0; i < size; i++) {
                    final AbsoluteDate date = refDate.shiftedBy(60. * i);

                    final Vector3D position = samples[i].getPosition();
                    final Vector3D velocity = samples[i].getVelocity();
                    final Vector3D acceleration = samples[i].getAcceleration();
                    final double[] value;
                    final double[] derivativeValue;
                    final double[] doubleDerivative;

                    switch (filter) {
                        case USE_P:
                            value = new double[] { position.getX(), position.getY(), position.getZ() };
                            interpolator.addSamplePoint(date.durationFrom(refDate), value);
                            break;
                        case USE_PV:
                            value = new double[] { position.getX(), position.getY(), position.getZ() };
                            derivativeValue = new double[] { velocity.getX(), velocity.getY(), velocity.getZ() };
                            interpolator.addSamplePoint(date.durationFrom(refDate), value, derivativeValue);
                            break;
                        case USE_PVA:

                            value = new double[] { position.getX(), position.getY(), position.getZ() };
                            derivativeValue = new double[] { velocity.getX(), velocity.getY(), velocity.getZ() };
                            doubleDerivative =
                                new double[] { acceleration.getX(), acceleration.getY(), acceleration.getZ() };
                            interpolator.addSamplePoint(date.durationFrom(refDate), value, derivativeValue, doubleDerivative);
                            break;
                        default:
                            throw new EnumConstantNotPresentException(CartesianDerivativesFilter.class, "filter");
                    }
                }

                // Evaluate the interpolation with sub-samples
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j <= subSize; j++) {
                        final AbsoluteDate date = refDate.shiftedBy((i * subSize + j) * step);

                        // Use the interpolation function
                        final TimeStampedPVCoordinates timeStampedPV = interpolationFct.apply(date);

                        // Compute the expected TimeStampedPVCoordinates
                        final double durationBis = date.durationFrom(refDate);
                        final double[][] valueAndDerivative = interpolator.valueAndDerivative(durationBis, computeAcceleration);
                        final Vector3D p = new Vector3D(valueAndDerivative[0]);
                        final Vector3D v = new Vector3D(valueAndDerivative[1]);

                        final TimeStampedPVCoordinates expectedTimeStampedPV;
                        if (computeAcceleration) {
                            final Vector3D a = new Vector3D(valueAndDerivative[2]);
                            expectedTimeStampedPV = new TimeStampedPVCoordinates(date, p, v, a);
                        } else {
                            expectedTimeStampedPV = new TimeStampedPVCoordinates(date, p, v);
                        }

                        // Compare the values
                        Assert.assertEquals(expectedTimeStampedPV, timeStampedPV);
                    }
                }
            }
        }
    }

    /**
     * @description Evaluate the interpolation function exception case.
     *
     * @testedMethod {@link TimeStampedPVCoordinates#buildInterpolationFunction(TimeStampedPVCoordinates[], int, int,
     *               CartesianDerivativesFilter, boolean)}
     *
     * @testPassCriteria The interpolation function throw the expected exception.
     */
    @Test
    public void testInterpolationFunctionException() {
        // Initialize some samples without acceleration
        final int size = 4;
        final AbsoluteDate refDate = AbsoluteDate.J2000_EPOCH;

        final TimeStampedPVCoordinates[] samples = new TimeStampedPVCoordinates[size];
        for (int i = 0; i < size; i++) {
            final AbsoluteDate date = refDate.shiftedBy(60. * i);
            final Vector3D pos = new Vector3D(7179992.82 + i * 10.2, 2276.519 - i * 10.1, -14178.396 + i * 10.8);
            final Vector3D vel = new Vector3D(7.450848 - i * 0.7, -1181.198684 + i * 0.2, 7356.62864 + i * 0.66);
            samples[i] = new TimeStampedPVCoordinates(date, pos, vel);
        }

        // Try to build an interpolation function which requires an acceleration component in its samples
        try {
            INTERP_FUNC_BUILDER_PVA_TRUE.buildInterpolationFunction(samples, 0, size);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testSerialization() {

        // Random test
        final RandomGenerator generator = new Well19937a(9562140688462201l);
        final AbsoluteDate ad = new AbsoluteDate(new DateComponents(2008, 3, 9), new TimeComponents(8, 12, 13.816),
            TimeScalesFactory.getTAI());
        for (int i = 0; i < 1000; ++i) {
            final TimeStampedPVCoordinates pvC1 = randomTimeStampedPVCoordinates(generator, 1.0, 1.0, 1.0, ad);
            final TimeStampedPVCoordinates pvC2 = TestUtils.serializeAndRecover(pvC1);
            testEquals(pvC2, pvC1, true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link TimeStampedPVCoordinates#equals(Object)}
     * @testedMethod {@link TimeStampedPVCoordinates#hashCode()}
     * 
     * @description test equals() and hashcode() method for different case:
     *              - params1 vs params1
     *              - params1 vs params2 with same attributes
     *              - params 1 vs params3 with different attributes
     * 
     * @input ReentryParameters
     * 
     * @output boolean and int
     * 
     * @testPassCriteria output is as expected
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public void testEquals() {

        // Initialization
        final TimeStampedPVCoordinates params1 =
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K));
        final TimeStampedPVCoordinates params2 =
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_K));
        final TimeStampedPVCoordinates params3 =
            new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH, new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_J));
        final PVCoordinates params4 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_J);

        // Check
        Assert.assertTrue(params1.equals(params1));
        Assert.assertTrue(params1.equals(params2));
        Assert.assertFalse(params1.equals(params3));
        Assert.assertFalse(params3.equals(params4));

        Assert.assertTrue(params1.hashCode() == params2.hashCode());
        Assert.assertFalse(params1.hashCode() == params3.hashCode());
        Assert.assertFalse(params3.hashCode() == params4.hashCode());
    }

    private static void testEquals(final TimeStampedPVCoordinates pvC1, final TimeStampedPVCoordinates pvC2,
                                   final boolean expectedResult) {
        Assert.assertEquals(pvC1.getPosition().equals(pvC2.getPosition()), expectedResult);
        Assert.assertEquals(pvC1.getVelocity().equals(pvC2.getVelocity()), expectedResult);
        Assert.assertEquals(pvC1.getAcceleration().equals(pvC2.getAcceleration()), expectedResult);
        Assert.assertEquals(pvC1.getDate().equals(pvC2.getDate()), expectedResult);
    }

    private static Vector3D randomVector(final RandomGenerator random, final double norm) {
        final double n = random.nextDouble() * norm;
        final double x = random.nextDouble();
        final double y = random.nextDouble();
        final double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private static TimeStampedPVCoordinates randomTimeStampedPVCoordinates(final RandomGenerator random,
                                                                           final double norm0, final double norm1,
                                                                           final double norm2, final AbsoluteDate ad) {
        final Vector3D p0 = randomVector(random, norm0);
        final Vector3D p1 = randomVector(random, norm1);
        final Vector3D p2 = randomVector(random, norm2);
        return new TimeStampedPVCoordinates(ad, p0, p1, p2);
    }

    private static PolynomialFunction randomPolynomial(final int degree, final Random random) {
        final double[] coeff = new double[1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
    }

    private static void checkPV(final TimeStampedPVCoordinates expected, final TimeStampedPVCoordinates real,
                                final double epsilon) {
        Assert.assertEquals(expected.getDate(), real.getDate());
        Assert.assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        Assert.assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        Assert.assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        Assert.assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        Assert.assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        Assert.assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getX(), real.getAcceleration().getX(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getY(), real.getAcceleration().getY(), epsilon);
        Assert.assertEquals(expected.getAcceleration().getZ(), real.getAcceleration().getZ(), epsilon);
    }
}
