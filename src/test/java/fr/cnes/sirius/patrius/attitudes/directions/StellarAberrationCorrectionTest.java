/**
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
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans l'interface ITargetDirection
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.AberrationCorrection;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.SignalDirection;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for stellar aberration correction.
 *              </p>
 *
 * @since 4.10
 * 
 */
public class StellarAberrationCorrectionTest {

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link StellarAberrationCorrection#applyTo(PVCoordinatesProvider, Vector3D, Frame, AbsoluteDate)}
     * 
     * @description Instantiation of a direction described a central body (the central body's center is the target
     *              point), and getting the direction vector taking into account the stellar aberration. The
     *              {@link PVCoordinatesProvider} of the observer is built from a {@link KeplerianOrbit}. The validation
     *              concerns not only the final direction which goes from the observer to the corrected target position,
     *              but also the delta of the angle which lies between the observer-target direction and the observer's
     *              velocity vector.
     * 
     * @output Vector3D (direction) and double (delta of angle)
     */
    @Test
    public void testApplyTo() throws PatriusException {

        // Time tolerance for convergence
        final double eps = 1E-14;

        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        // reference frame for testing : earth centered frame
        final Frame gcrf = FramesFactory.getGCRF();

        final Frame icrf = FramesFactory.getICRF();
        final Frame frozenGcrfT0 = gcrf.getFrozenFrame(icrf, t0, "inertialFrozen");
        final Transform frozenToGcrfT0 = frozenGcrfT0.getTransformTo(gcrf, t0);

        // earth direction
        final EarthCenterDirection earthDirection = new EarthCenterDirection();
        // target object
        final PVCoordinatesProvider target = earthDirection.getTargetPvProvider();

        // -------- the observer object with a keplerian motion (circular) --------
        final KeplerianOrbit observer = new KeplerianOrbit(1E7, 0., 0., 0., 0., 0., PositionAngle.MEAN, gcrf,
            t0, Constants.GRIM5C1_EARTH_MU);

        // PV coordinates of objects
        PVCoordinates pvObserver;
        PVCoordinates pvTarget;

        // Expected and actual direction of signal propagation
        Vector3D expected;
        Vector3D actual;
        // Expected direction of signal propagation before the application of the stellar aberration correction (this
        // expected pre-stellar correction direction is used to compute the angle with respect to the stellar-corrected
        // direction)
        Vector3D expectedPreStellarCorrection;

        // Approximated expected beta factor (sourceVelocity/c)
        final double approxExpectedBetaFactor = 1E-4;
        // inertial velocity of source in ICRF frame
        Vector3D sourceVelocityInIcrf;
        // source velocity projected in the given frame
        Vector3D sourceVelocityProjInFrame;
        // actual beta factor
        double actualBetaFactor;
        // expected delta theta
        double expectedDeltaTheta;
        // actual theta
        double actualTheta;
        // actual delta theta
        double actualDeltaTheta;

        // ABERRATION CORRECTION: STELLAR

        // direction vector computed in GCRF

        // from target to observer, fixed date is signal reception date, stellar aberration correction
        actual = earthDirection.getVector(observer, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // observer position at fixed (reception) date in frozen frame
        pvObserver = observer.getPVCoordinates(t0, frozenGcrfT0);
        // target position at emission date in frozen frame
        pvTarget = earthDirection.getTargetPVCoordinates(t0, frozenGcrfT0);
        // transform the direction into GCRF frame at fixed date
        expectedPreStellarCorrection = frozenToGcrfT0.transformVector(pvTarget.getPosition().subtract(
            pvObserver.getPosition()).negate());
        // apply the stellar aberration correction
        expected = StellarAberrationCorrection.applyTo(observer, expectedPreStellarCorrection.negate(), gcrf, t0);
        // check direction vector
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = observer.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in GCRF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(gcrf, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);
        // expected delta theta
        expectedDeltaTheta = Vector3D.angle(expectedPreStellarCorrection, expected.negate());
        // actual theta
        actualTheta = Vector3D.angle(expectedPreStellarCorrection, sourceVelocityProjInFrame);
        // actual delta theta
        actualDeltaTheta = MathLib.asin(MathLib.sin(actualTheta) * actualBetaFactor);
        // check delta theta
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, this.comparisonEpsilon);

        // from observer to target, fixed date is signal reception date, stellar aberration correction
        actual = earthDirection.getVector(observer, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // observer position at emission date in frozen frame
        pvObserver = observer.getPVCoordinates(t0, frozenGcrfT0);
        // target position at fixed (reception) date in frozen frame
        pvTarget = earthDirection.getTargetPVCoordinates(t0, frozenGcrfT0);
        // transform the direction into GCRF frame at fixed date
        expectedPreStellarCorrection = frozenToGcrfT0.transformVector(pvTarget.getPosition().subtract(
            pvObserver.getPosition()));
        // apply the stellar aberration correction
        expected = StellarAberrationCorrection.applyTo(target, expectedPreStellarCorrection.negate(), gcrf, t0)
            .negate();
        // check direction vector
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = target.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in GCRF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(gcrf, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);
        // expected delta theta
        expectedDeltaTheta = Vector3D.angle(expectedPreStellarCorrection, expected);
        // actual theta
        actualTheta = Vector3D.angle(expectedPreStellarCorrection, sourceVelocityProjInFrame);
        // actual delta theta
        actualDeltaTheta = MathLib.asin(MathLib.sin(actualTheta) * actualBetaFactor);
        // check delta theta
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, this.comparisonEpsilon);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link StellarAberrationCorrection#applyInverseTo(PVCoordinatesProvider, Vector3D, Frame, AbsoluteDate)}
     * 
     * @description Instantiation of a direction described a central body (the central body's center is the target
     *              point), and getting the direction vector taking into account the stellar aberration. The
     *              {@link PVCoordinatesProvider} of the transmitter is built from a {@link KeplerianOrbit}. The
     *              validation concerns not only the final direction which goes from the transmitter to the corrected
     *              target position, but also the delta of the angle which lies between the transmitter-target direction
     *              and the transmitter's velocity vector.
     * 
     * @output Vector3D (direction) and double (delta of angle)
     */
    @Test
    public void testApplyInverseTo() throws PatriusException {

        // Time tolerance for convergence
        final double eps = 1E-14;

        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        // reference frame for testing : earth centered frame
        final Frame gcrf = FramesFactory.getGCRF();

        final Frame icrf = FramesFactory.getICRF();
        final Frame frozenGcrfT0 = gcrf.getFrozenFrame(icrf, t0, "inertialFrozen");
        final Transform frozenToGcrfT0 = frozenGcrfT0.getTransformTo(gcrf, t0);

        // earth direction
        final EarthCenterDirection earthDirection = new EarthCenterDirection();
        // target object
        final PVCoordinatesProvider target = earthDirection.getTargetPvProvider();

        // -------- the transmitter object with a keplerian motion (circular) --------
        final KeplerianOrbit transmitter = new KeplerianOrbit(1E7, 0., 0., 0., 0., 0., PositionAngle.MEAN, gcrf,
            t0, Constants.GRIM5C1_EARTH_MU);

        // PV coordinates of objects
        PVCoordinates pvTransmitter;
        PVCoordinates pvTarget;

        // Expected and actual direction of signal propagation
        Vector3D expected;
        Vector3D actual;
        // Expected direction of signal propagation before the application of the stellar aberration correction (this
        // expected pre-stellar correction direction is used to compute the angle with respect to the stellar-corrected
        // direction)
        Vector3D expectedPreStellarCorrection;

        // Approximated expected beta factor (sourceVelocity/c)
        final double approxExpectedBetaFactor = 1E-4;
        // inertial velocity of source in ICRF frame
        Vector3D sourceVelocityInIcrf;
        // source velocity projected in the given frame
        Vector3D sourceVelocityProjInFrame;
        // actual beta factor
        double actualBetaFactor;
        // expected delta theta
        double expectedDeltaTheta;
        // actual theta
        double actualTheta;
        // actual delta theta
        double actualDeltaTheta;

        // ABERRATION CORRECTION: STELLAR

        // direction vector computed in GCRF

        // from target to transmitter, fixed date is signal emission date, stellar aberration correction
        actual = earthDirection.getVector(transmitter, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.EMISSION, gcrf, eps);
        // transmitter position at reception date in frozen frame
        pvTransmitter = transmitter.getPVCoordinates(t0, frozenGcrfT0);
        // target position at fixed (emission) date in frozen frame
        pvTarget = earthDirection.getTargetPVCoordinates(t0, frozenGcrfT0);
        // transform the direction into GCRF frame at fixed date
        expectedPreStellarCorrection = frozenToGcrfT0.transformVector(pvTarget.getPosition().subtract(
            pvTransmitter.getPosition()).negate());
        // apply the stellar aberration correction
        expected = StellarAberrationCorrection.applyInverseTo(target, expectedPreStellarCorrection, gcrf, t0).negate();
        // check direction vector
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = target.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in GCRF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(gcrf, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);
        // expected delta theta
        expectedDeltaTheta = Vector3D.angle(expectedPreStellarCorrection, expected.negate());
        // actual theta
        actualTheta = Vector3D.angle(expectedPreStellarCorrection, sourceVelocityProjInFrame);
        // actual delta theta
        actualDeltaTheta = MathLib.asin(MathLib.sin(actualTheta) * actualBetaFactor);
        // check delta theta
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, this.comparisonEpsilon);

        // from transmitter to target, fixed date is signal emission date, stellar aberration correction
        actual = earthDirection.getVector(transmitter, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.EMISSION, gcrf, eps);
        // transmitter position at fixed (emission) date in frozen frame
        pvTransmitter = transmitter.getPVCoordinates(t0, frozenGcrfT0);
        // target position at reception date in frozen frame
        pvTarget = earthDirection.getTargetPVCoordinates(t0, frozenGcrfT0);
        // transform the direction into GCRF frame at fixed date
        expectedPreStellarCorrection = frozenToGcrfT0.transformVector(pvTarget.getPosition().subtract(
            pvTransmitter.getPosition()));
        // apply the stellar aberration correction
        expected = StellarAberrationCorrection.applyInverseTo(transmitter, expectedPreStellarCorrection, gcrf, t0);
        // check direction vector
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = transmitter.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in GCRF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(gcrf, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 3E-7);
        // expected delta theta
        expectedDeltaTheta = Vector3D.angle(expectedPreStellarCorrection, expected);
        // actual theta
        actualTheta = Vector3D.angle(expectedPreStellarCorrection, sourceVelocityProjInFrame);
        // actual delta theta
        actualDeltaTheta = MathLib.asin(MathLib.sin(actualTheta) * actualBetaFactor);
        // check delta theta
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, this.comparisonEpsilon);
    }

    /**
     * Check the equality between two {@link Vector3D}.
     * 
     * @param expected
     *        the expected value
     * @param actual
     *        the actual value
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    private static void checkVector3DEquality(final Vector3D expected, final Vector3D actual, final double absTol,
                                              final double relTol) {
        // absolute difference vector
        final Vector3D delta = actual.subtract(expected);

        // check absolute difference
        Assert.assertEquals(0, MathLib.abs(delta.getX()), absTol);
        Assert.assertEquals(0, MathLib.abs(delta.getY()), absTol);
        Assert.assertEquals(0, MathLib.abs(delta.getZ()), absTol);
        // check relative difference
        if (actual.getX() != 0) {
            Assert.assertEquals(0, MathLib.abs(delta.getX()) / actual.getX(), relTol);
        }
        if (actual.getY() != 0) {
            Assert.assertEquals(0, MathLib.abs(delta.getY()) / actual.getY(), relTol);
        }
        if (actual.getZ() != 0) {
            Assert.assertEquals(0, MathLib.abs(delta.getZ()) / actual.getZ(), relTol);
        }
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
    }

}
