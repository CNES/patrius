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
 * 
 * @history creation 30/11/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans l'interface ITargetDirection
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitudeTest;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.AberrationCorrection;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.SignalDirection;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the directions described by its target point, represented by a PVCoordinatesProvider.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: GenericTargetDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class GenericTargetDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Geneneric target direction
         * 
         * @featureDescription direction described by its target point,
         *                     represented by a PVCoordinatesProvider.
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170, DV-GEOMETRIE_190
         */
        GENERIC_ORIGIN_TARGET_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-dataCNES-2003");
        Report.printClassHeader(TabulatedAttitudeTest.class.getSimpleName(), "Generic target direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_ORIGIN_TARGET_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by its
     *              target point, and getting of the vector expressed
     *              in a frame, at a date, giving the origin point.
     * 
     * @input the origin and target created as basic PVCoordinatesProvider
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one between the origin and the target
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        // origin and target creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
        final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
        final Vector3D targetPos = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D targetVel = new Vector3D(-8.657, 657.5764, 567.1596);

        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        final PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);

        final BasicPVCoordinatesProvider originIn = new BasicPVCoordinatesProvider(originPV, frame);
        final BasicPVCoordinatesProvider targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        // Direction creation
        final GenericTargetDirection direction = new GenericTargetDirection(targetIn);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D expected = targetPos.subtract(originPos);

        try {
            final Vector3D result = direction.getVector(originIn, date, frame);

            Assert.assertEquals(expected.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getZ(), result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", expected, result);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_ORIGIN_TARGET_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getTargetPVCoordinates(AbsoluteDate, Frame)}
     * 
     * 
     * @description Instantiation of a direction described by its
     *              target point, and getting of the target
     *              PV coordinates expressed in a frame, at a date.
     * 
     * @input the target created as basic PVCoordinatesProvider
     * 
     * @output PVCoordinates
     * 
     * @testPassCriteria the returned coordinates are identical to the ones used to
     *                   create the direction. The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetTarget() {
        // origin and target creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D targetPos = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D targetVel = new Vector3D(-8.657, 657.5764, 567.1596);

        final PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);

        final BasicPVCoordinatesProvider targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        // Direction creation
        final GenericTargetDirection direction = new GenericTargetDirection(targetIn);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        try {
            final PVCoordinates outTarget = direction.getTargetPVCoordinates(date, frame);
            final Vector3D resultPos = outTarget.getPosition();
            final Vector3D resultVel = outTarget.getVelocity();

            Assert.assertEquals(targetPos.getX(), resultPos.getX(), this.comparisonEpsilon);
            Assert.assertEquals(targetPos.getY(), resultPos.getY(), this.comparisonEpsilon);
            Assert.assertEquals(targetPos.getZ(), resultPos.getZ(), this.comparisonEpsilon);

            Assert.assertEquals(targetVel.getX(), resultVel.getX(), this.comparisonEpsilon);
            Assert.assertEquals(targetVel.getY(), resultVel.getY(), this.comparisonEpsilon);
            Assert.assertEquals(targetVel.getZ(), resultVel.getZ(), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_ORIGIN_TARGET_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by its
     *              target point, and getting of the line containing a given origin and
     *              target point.
     * 
     * @input the origin and target created as basic PVCoordinatesProvider
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains both points. An exception is returned if
     *                   the points are identical. The 1.0e-14 epsilon is the simple double comparison epsilon, used
     *                   because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetLine() {
        // origin and target creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
        final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
        Vector3D targetPos = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D targetVel = new Vector3D(-8.657, 657.5764, 567.1596);

        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);

        final BasicPVCoordinatesProvider originIn = new BasicPVCoordinatesProvider(originPV, frame);
        BasicPVCoordinatesProvider targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        // Direction creation
        GenericTargetDirection direction = new GenericTargetDirection(targetIn);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        try {
            // line creation
            final Line line = direction.getLine(originIn, date, frame);
            // test of the points
            Assert.assertTrue(line.contains(originPos));
            Assert.assertTrue(line.contains(targetPos));

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // test with identical origin and target points
        targetPos = originPos;
        targetPV = new PVCoordinates(targetPos, targetVel);
        targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        direction = new GenericTargetDirection(targetIn);

        try {
            direction.getLine(originIn, date, frame);

            // an exception must be thrown
            Assert.fail();

        } catch (final PatriusException e) {
            // expected !
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getVector(PVCoordinatesProvider, SignalDirection, AbsoluteDate, FixedDate, Frame, double)}
     * 
     * @description Instantiation of a direction described by a target stationnary point in an arbitrary frame defined
     *              by a {@link BasicPVCoordinatesProvider}. The origin object is described by a {@link KeplerianOrbit}
     *              .The output result is the direction vector connecting both objects taking into account the delay of
     *              signal propagation. The input frame is frozen in the ICRF frame, it leads to a difference between
     *              the expected signal propagation duration and the actual one (absolute error of 1E-6 second).
     * 
     * @input the origin is a spacecraft created as {@link KeplerianOrbit}
     * 
     * @output Vector3D
     */
    @Test
    public void testGetVectorDelay() throws PatriusException {

        // time tolerance for signal propagation algorithm
        final double eps = 1E-14;

        // an arbitrary date for orbit definition
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        // arbitrary fixed date
        final AbsoluteDate fixedDate = t0.shiftedBy(3600);

        // frames used for testing
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame icrf = FramesFactory.getICRF();
        final Frame frozen = gcrf.getFrozenFrame(icrf, fixedDate, "");
        final Transform frozenToGcrf = frozen.getTransformTo(gcrf, fixedDate);

        // target object
        final BasicPVCoordinatesProvider target = new BasicPVCoordinatesProvider(PVCoordinates.ZERO, gcrf);

        // target direction
        final GenericTargetDirection toTarget = new GenericTargetDirection(target);

        // the origin object orbiting around the target
        final double a = 1E7;
        final KeplerianOrbit origin = new KeplerianOrbit(a, 0, 0, 0, 0, 0, PositionAngle.TRUE, gcrf, t0,
            Constants.EGM96_EARTH_MU);
        // delay of signal propagation between objects, slightly different
        // from the one computed by the method
        final double expectedDt = MathLib.divide(a, Constants.SPEED_OF_LIGHT);
        System.out.println(expectedDt);
        System.out.println();
        // local orbital frame
        final Frame lof = new LocalOrbitalFrame(gcrf, LOFType.QSW, origin, "orbital frame");
        final Frame frozenLof = lof.getFrozenFrame(icrf, fixedDate, "");
        final Transform frozenToLof = frozenLof.getTransformTo(lof, fixedDate);

        Vector3D actual;
        Vector3D expected;
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

        PVCoordinates pvOrigin;
        PVCoordinates pvTarget;
        // true propagation duration
        double dt;

        // ABERRATION CORRECTION: NONE

        // direction vector computed in GCRF

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = target.getPVCoordinates(t0, gcrf);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // with expected signal propagation duration
        dt = expectedDt;
        pvTarget = target.getPVCoordinates(t0, gcrf);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = target.getPVCoordinates(t0, gcrf);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // direction vector computed in local orbital frame

        // from target to origin, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, 1E-1, 1E-4);
        // with the true signal propagation duration
        dt = 0.03335812068567534;
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // from origin to target, fixed date is signal reception date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // ABERRATION CORRECTION: LIGHT-TIME

        // direction vector computed in GCRF

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // with true signal propagation duration
        dt = 0.03335469884296893;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozen);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(dt), frozen);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = frozenToGcrf.transformVector(expected);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // with expected signal propagation duration
        dt = expectedDt;
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(dt), frozen);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = frozenToGcrf.transformVector(expected);
        checkVector3DEquality(expected, actual, 1E-1, 1E-4);

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.RECEPTION, gcrf, eps);
        // with expected signal propagation duration
        dt = 0.033358120624931265;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozen);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(-dt), frozen);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = frozenToGcrf.transformVector(expected);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // direction vector computed in local orbital frame

        // from target to origin, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.EMISSION, lof, eps);
        // with expected signal propagation duration
        dt = expectedDt;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate, frozenLof);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = frozenToLof.transformVector(expected);
        checkVector3DEquality(expected, actual, 1E-1, 1E-4);
        // with the true signal propagation duration
        dt = 0.03335812068567534;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate, frozenLof);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = frozenToLof.transformVector(expected);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // from origin to target, fixed date is signal reception date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.RECEPTION, lof, eps);
        // with the true signal propagation duration
        dt = 0.03335469890377918;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate, frozenLof);
        // expected direction vector in frozen frame
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = frozenToLof.transformVector(expected);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // ABERRATION CORRECTION: STELLAR

        // direction vector computed in GCRF

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.EMISSION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = target.getPVCoordinates(t0, gcrf);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = StellarAberrationCorrection.applyInverseTo(origin, expectedPreStellarCorrection, gcrf, t0);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // with expected signal propagation duration
        dt = expectedDt;
        pvTarget = target.getPVCoordinates(t0, gcrf);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = StellarAberrationCorrection.applyInverseTo(origin, expectedPreStellarCorrection, gcrf, t0);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = origin.getPVCoordinates(t0, icrf).getVelocity();
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

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.RECEPTION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = target.getPVCoordinates(t0, gcrf);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        expected = StellarAberrationCorrection.applyTo(origin, expectedPreStellarCorrection.negate(), gcrf, t0);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = origin.getPVCoordinates(t0, icrf).getVelocity();
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

        // direction vector computed in local orbital frame

        // from target to origin, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.EMISSION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        expected = StellarAberrationCorrection.applyInverseTo(target, expectedPreStellarCorrection, lof, t0).negate();
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // with the true signal propagation duration
        dt = 0.03335812068567534;
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        expected = StellarAberrationCorrection.applyInverseTo(target, expectedPreStellarCorrection, lof, t0).negate();
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = target.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in LOF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(lof, t0).transformVector(sourceVelocityInIcrf);
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

        // from origin to target, fixed date is signal reception date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.RECEPTION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = StellarAberrationCorrection.applyTo(target, expectedPreStellarCorrection.negate(), lof, t0).negate();
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = target.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in LOF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(lof, t0).transformVector(sourceVelocityInIcrf);
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

        // ABERRATION CORRECTION: ALL

        // direction vector computed in GCRF

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // with expected signal propagation duration
        dt = expectedDt;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozen);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(dt), frozen);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expectedPreStellarCorrection = frozenToGcrf.transformVector(expectedPreStellarCorrection);
        expected = StellarAberrationCorrection.applyInverseTo(origin, expectedPreStellarCorrection, gcrf, fixedDate);
        checkVector3DEquality(expected, actual, 6E-2, 1.0004);
        // with true signal propagation duration
        dt = 0.03335469884296893;
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(dt), frozen);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expectedPreStellarCorrection = frozenToGcrf.transformVector(expectedPreStellarCorrection);
        expected = StellarAberrationCorrection.applyInverseTo(origin, expectedPreStellarCorrection, gcrf, fixedDate);
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = origin.getPVCoordinates(t0, icrf).getVelocity();
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
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 4E-5);

        // from origin to target, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.RECEPTION, gcrf, eps);
        // with expected signal propagation duration
        dt = 0.033358120624931265;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozen);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(-dt), frozen);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        expectedPreStellarCorrection = frozenToGcrf.transformVector(expectedPreStellarCorrection);
        expected = StellarAberrationCorrection.applyTo(origin, expectedPreStellarCorrection.negate(), gcrf, fixedDate);
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
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 4E-5);

        // direction vector computed in local orbital frame

        // from target to origin, fixed date is signal emission date
        actual = toTarget.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.EMISSION, lof, eps);
        // with expected signal propagation duration
        dt = expectedDt;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate, frozenLof);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        expectedPreStellarCorrection = frozenToLof.transformVector(expectedPreStellarCorrection);
        expected = StellarAberrationCorrection.applyInverseTo(target, expectedPreStellarCorrection, lof,
            fixedDate).negate();
        checkVector3DEquality(expected, actual, 1E-1, 1);
        // with the true signal propagation duration
        dt = 0.03335812068567534;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenLof);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        expectedPreStellarCorrection = frozenToLof.transformVector(expectedPreStellarCorrection);
        expected = StellarAberrationCorrection.applyInverseTo(target, expectedPreStellarCorrection, lof,
            fixedDate).negate();
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = target.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in LOF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(lof, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);

        // from origin to target, fixed date is signal reception date
        actual = toTarget.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.RECEPTION, lof, eps);
        // with the true signal propagation duration
        dt = 0.03335469890377918;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate, frozenLof);
        // expected direction vector in frozen frame
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expectedPreStellarCorrection = frozenToLof.transformVector(expectedPreStellarCorrection);
        expected = StellarAberrationCorrection.applyTo(target, expectedPreStellarCorrection.negate(), lof,
            fixedDate).negate();
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = origin.getPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in LOF frame
        sourceVelocityProjInFrame = icrf.getTransformTo(lof, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 3E-7);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getLine(PVCoordinatesProvider, SignalDirection, AbsoluteDate, FixedDate, Frame, double)}
     * 
     * @description Instantiation of a direction described by a target stationnary point in an arbitrary frame defined
     *              by a {@link BasicPVCoordinatesProvider}. The origin object is described by a {@link KeplerianOrbit}.
     *              The output result is the {@link Line} joining both objects positions taking into account the delay
     *              of signal propagation.
     * 
     * @input the origin is a spacecraft created as {@link KeplerianOrbit}
     * 
     * @output Vector3D
     */
    @Test
    public void testGetLineDelay() throws PatriusException {

        // Earth parameters
        final double radius = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.EGM96_EARTH_MU;

        // Time tolerance for convergence
        final double eps = 1E-14;

        // an arbitrary date for orbit definition
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        // arbitrary fixed date for signal emission/reception
        final AbsoluteDate fixedDate = t0.shiftedBy(10);

        // ICRF Frame
        final Frame icrf = FramesFactory.getICRF();
        // GCRF Frame
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame frozenGcrf = gcrf.getFrozenFrame(icrf, fixedDate, " ");
        final Transform frozenToGcrf = frozenGcrf.getTransformTo(gcrf, fixedDate);

        // spherical coordinates of ground station
        final double lat = MathLib.toRadians(25.);
        final double lon = MathLib.toRadians(28.);
        final double[] latSinCos = MathLib.sinAndCos(lat);
        final double[] longSinCos = MathLib.sinAndCos(lon);
        // cartesian coordinates
        final Vector3D posInGcrf = new Vector3D(radius * latSinCos[1] * longSinCos[1], radius * latSinCos[1]
                * longSinCos[0], radius * latSinCos[0]);
        // local frame
        final Transform t = new Transform(t0, posInGcrf);
        final Frame stationFrame = new Frame(gcrf, t, "station local frame", true);
        final Frame frozenStationFrame = stationFrame.getFrozenFrame(icrf, fixedDate, " ");
        final Transform frozenToStation = frozenStationFrame.getTransformTo(stationFrame, fixedDate);

        // target object is a basic pv provider
        final BasicPVCoordinatesProvider target = new BasicPVCoordinatesProvider(new PVCoordinates(posInGcrf,
            Vector3D.ZERO), gcrf);

        // the target direction
        final GenericTargetDirection toTarget = new GenericTargetDirection(target);

        // the origin object
        final KeplerianOrbit origin = new KeplerianOrbit(radius + 850E3, 0.0, 0, 0., 0., 0., PositionAngle.MEAN, gcrf,
            t0, mu);
        // local orbital frame
        final Frame lof = new LocalOrbitalFrame(gcrf, LOFType.QSW, origin, "orbital frame");
        final Frame frozenLof = lof.getFrozenFrame(icrf, fixedDate, " ");
        final Transform frozenToLof = frozenLof.getTransformTo(lof, fixedDate);

        Line actual;
        Vector3D direction;

        // signal propagation duration
        double dt;

        PVCoordinates pvOrigin;
        PVCoordinates pvTarget;

        // ABERRATION CORRECTION: NONE

        // from origin to target, fixed date is signal emission date
        // computed in frame attached to the ground station
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, stationFrame, eps);
        pvOrigin = origin.getPVCoordinates(t0, stationFrame);
        pvTarget = target.getPVCoordinates(t0, stationFrame);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 5E-6);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < 5E-6);

        // from origin to target, fixed date is signal reception date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        direction = pvOrigin.getPosition().subtract(pvTarget.getPosition());
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 7E-7);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < 8E-7);

        // from target to origin, fixed date is signal emission date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 8E-7);

        // from target to origin, fixed date is signal reception date
        // computed in GCRF
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, gcrf, eps);
        pvTarget = target.getPVCoordinates(t0, gcrf);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        direction = pvOrigin.getPosition().subtract(pvTarget.getPosition());
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < 4E-10);

        // ABERRATION CORRECTION: LIGHT-TIME

        // from origin to target, fixed date is signal emission date
        // computed in frame attached to the ground station
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.EMISSION, stationFrame, eps);
        // expected signal propagation delay
        dt = MathLib.divide(toTarget.getVector(origin, fixedDate, gcrf).getNorm(), Constants.SPEED_OF_LIGHT);
        // the direction vector with delay is the same as without delay if computed in gcrf frame
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenStationFrame);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(dt), frozenStationFrame);
        direction = frozenToStation.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        // the equality is not strict due to delta between expected propagation duration
        // and the duration computed by the model
        checkVector3DEquality(direction.normalize(), actual.getDirection(), 3E-9, 6E-9);
        Assert.assertTrue(actual.distance(frozenToStation.transformPosition(pvOrigin.getPosition())) < 1E-9);

        // from origin to target, fixed date is signal reception date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.RECEPTION, lof, eps);
        // true signal propagation duration
        dt = 0.014442019927144223;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate, frozenLof);
        direction = frozenToLof.transformVector(pvOrigin.getPosition().subtract(pvTarget.getPosition()));
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(frozenToLof.transformPosition(pvOrigin.getPosition())) < 2E-10);
        Assert.assertTrue(actual.distance(frozenToLof.transformPosition(pvTarget.getPosition())) < 2E-9);

        // from target to origin, fixed date is signal emission date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.RECEPTION, lof, eps);
        // true signal propagation duration
        dt = 0.014440818596972216;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenLof);
        direction = frozenToLof.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < this.comparisonEpsilon);

        // from target to origin, fixed date is signal reception date
        // computed in GCRF
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // true signal propagation duration
        dt = 0.014440598718237885;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenGcrf);
        pvTarget = target.getPVCoordinates(fixedDate, frozenGcrf);
        direction = frozenToGcrf.transformVector(pvOrigin.getPosition().subtract(pvTarget.getPosition()));
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvTarget.getPosition())) < 3E-10);

        // ABERRATION CORRECTION: STELLAR

        // from origin to target, fixed date is signal emission date
        // computed in frame attached to the ground station
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.EMISSION, stationFrame, eps);
        pvOrigin = origin.getPVCoordinates(t0, stationFrame);
        pvTarget = target.getPVCoordinates(t0, stationFrame);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        direction = StellarAberrationCorrection.applyInverseTo(origin, direction, stationFrame, t0);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 5E-6);

        // from origin to target, fixed date is signal reception date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.RECEPTION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        direction = pvOrigin.getPosition().subtract(pvTarget.getPosition());
        direction = StellarAberrationCorrection.applyInverseTo(origin, direction, lof, t0);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), 2E-4, 4E-4);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < 8E-7);

        // from target to origin, fixed date is signal emission date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.RECEPTION, lof, eps);
        pvOrigin = origin.getPVCoordinates(t0, lof);
        pvTarget = target.getPVCoordinates(t0, lof);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        direction = StellarAberrationCorrection.applyTo(target, direction, lof, t0);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), 2E-4, 4E-4);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 8E-7);

        // from target to origin, fixed date is signal reception date
        // computed in GCRF
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.EMISSION, gcrf, eps);
        pvTarget = target.getPVCoordinates(t0, gcrf);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        direction = pvOrigin.getPosition().subtract(pvTarget.getPosition()).negate();
        direction = StellarAberrationCorrection.applyTo(target, direction, gcrf, t0).negate();
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < 3E-10);

        // ABERRATION CORRECTION: ALL

        // from origin to target, fixed date is signal emission date
        // computed in frame attached to the ground station
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.EMISSION, stationFrame, eps);
        // expected signal propagation delay
        dt = MathLib.divide(toTarget.getVector(origin, fixedDate, gcrf).getNorm(), Constants.SPEED_OF_LIGHT);
        // the direction vector with delay is the same as without delay if computed in gcrf frame
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenStationFrame);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(dt), frozenStationFrame);
        direction = frozenToStation.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        direction = StellarAberrationCorrection.applyInverseTo(origin, direction, stationFrame, fixedDate);
        // the equality is not strict due to delta between expected propagation duration
        // and the duration computed by the model
        checkVector3DEquality(direction.normalize(), actual.getDirection(), 3E-9, 6E-9);
        Assert.assertTrue(actual.distance(frozenToStation.transformPosition(pvOrigin.getPosition())) < 2E-10);

        // from origin to target, fixed date is signal reception date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.RECEPTION, lof, eps);
        // true signal propagation duration
        dt = 0.014442019927144223;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate, frozenLof);
        direction = frozenToLof.transformVector(pvOrigin.getPosition().subtract(pvTarget.getPosition()));
        direction = StellarAberrationCorrection.applyInverseTo(origin, direction, lof, fixedDate);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), 2E-4, 4E-4);
        Assert.assertTrue(actual.distance(frozenToLof.transformPosition(pvTarget.getPosition())) < 2E-9);

        // from target to origin, fixed date is signal emission date
        // computed in LOCAL ORBITAL FRAME
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.RECEPTION, lof, eps);
        // true signal propagation duration
        dt = 0.014440818596972216;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenLof);
        pvTarget = target.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenLof);
        direction = frozenToLof.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate());
        direction = StellarAberrationCorrection.applyTo(target, direction, lof, fixedDate).negate();
        checkVector3DEquality(direction.normalize(), actual.getDirection(), 2E-4, 4E-4);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < this.comparisonEpsilon);

        // from target to origin, fixed date is signal reception date
        // computed in GCRF
        actual = toTarget.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // true signal propagation duration
        dt = 0.014440598718237885;
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenGcrf);
        pvTarget = target.getPVCoordinates(fixedDate, frozenGcrf);
        direction = frozenToGcrf.transformVector(pvOrigin.getPosition().subtract(pvTarget.getPosition()).negate());
        direction = StellarAberrationCorrection.applyTo(target, direction, gcrf, fixedDate).negate();
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvTarget.getPosition())) < 3E-10);
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
     * Test all cases of getVector method (math result).
     */
    @Test
    public void testGetVectorAllCases() throws PatriusException {
        // Results are not round because of GCRF / ICRF relative movement during the signal propagation

        // No aberration correction
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, FixedDate.EMISSION, new Vector3D(1.0,
            0.0, 0.0));
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, FixedDate.RECEPTION, new Vector3D(1.0,
            0.0, 0.0));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.NONE, FixedDate.EMISSION, new Vector3D(1.0,
            0.0, 0.0));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.NONE, FixedDate.RECEPTION, new Vector3D(1.0,
            0.0, 0.0));
        // Stellar aberration correction
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, FixedDate.EMISSION, new Vector3D(
            0.9999999998328042, 1.6777453297209673E-5, 7.273848681420285E-6));
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, FixedDate.RECEPTION, new Vector3D(
            1.0, 0.0, 0.0));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, FixedDate.EMISSION, new Vector3D(1.0,
            0.0, 0.0));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, FixedDate.RECEPTION, new Vector3D(
            0.9999999998328042, -1.6777453297209673E-5, -7.273848681420285E-6));
        // Light-time aberration correction
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, FixedDate.EMISSION, new Vector3D(
            -3.335640863497846E-9, 1.0, 0.0));
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, FixedDate.RECEPTION,
            new Vector3D(0.999999999832806, -1.67773606560119E-5, -7.273808575969392E-6));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, FixedDate.EMISSION, new Vector3D(
            0.9999999998328022, 1.6777545958801177E-5, 7.273888795725275E-6));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, FixedDate.RECEPTION, new Vector3D(
            0.9949874371066201, -0.0999999999999995, 0.0));
        // All aberration corrections
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, FixedDate.EMISSION, new Vector3D(
            9.934855497244164E-5, 0.999999995038478, 7.273848681420279E-6));
        testDirection(SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, FixedDate.RECEPTION, new Vector3D(
            0.999999999832806, -1.677736071197515E-5, -7.273808600232205E-6));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.ALL, FixedDate.EMISSION, new Vector3D(
            0.9999999998328022, 1.6777545902837307E-5, 7.273888771462194E-6));
        testDirection(SignalDirection.FROM_TARGET, AberrationCorrection.ALL, FixedDate.RECEPTION, new Vector3D(
            0.9949847738731045, -0.10002649502896005, -7.273848681420281E-6));
    }

    /**
     * Test getVector method.
     * 
     * @param signalDirection signal direction
     * @param aberrationCorrection aberration correction
     * @param fixedDate fixed date type
     * @param expected expected result
     */
    private void testDirection(final SignalDirection signalDirection,
                               final AberrationCorrection aberrationCorrection,
                               final FixedDate fixedDate,
                               final Vector3D expected) throws PatriusException {
        final AbsoluteDate originDate = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final double transfertDuration = 10.;
        final double deltaxyz = transfertDuration * Constants.SPEED_OF_LIGHT;
        final PVCoordinatesProvider origin = new ConstantPVCoordinatesProvider(Vector3D.ZERO, frame);
        // Target of the form (a, b, 0.)
        final PVCoordinatesProvider target = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) throws PatriusException {
                final double dt = date.durationFrom(originDate);
                Vector3D position;
                if (date.durationFrom(originDate) < 0) {
                    // Specific case: in this case, regular case would lead to faster than light travel leading to no
                    // convergence issues of light time signal propagation calculation
                    // Target is voluntarily slowed down
                    position = new Vector3D(deltaxyz, dt * Constants.SPEED_OF_LIGHT / 10., 0.);
                } else {
                    // Regular case
                    position = new Vector3D(deltaxyz * (1 - dt / transfertDuration), deltaxyz * dt / transfertDuration,
                        0.);
                }
                // Velocity only to not have a null velocity
                return new PVCoordinates(position, Vector3D.PLUS_I);
            }

            @Override
            public Frame getNativeFrame(AbsoluteDate date, Frame frame) throws PatriusException {
                return null;
            }
        };
        final GenericTargetDirection direction = new GenericTargetDirection(target);
        final Vector3D dir = direction.getVector(origin, signalDirection, aberrationCorrection, originDate, fixedDate,
            frame,
            1E-12);
        System.out.println(signalDirection + " " + aberrationCorrection + " " + fixedDate + " " + dir.normalize());
        Assert.assertEquals(0., dir.normalize().subtract(expected).getNorm(), 0.);
    }
}
