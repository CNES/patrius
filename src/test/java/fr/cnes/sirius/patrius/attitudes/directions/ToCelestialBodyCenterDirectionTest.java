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
 * @history creation 06/12/2011
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans l'interface ITargetDirection
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.AberrationCorrection;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.SignalDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.LinearTwoPointsPVProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the directions described by a celestial body : the celestial body's center is the target
 *              point.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ToCelestialBodyCenterDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class ToCelestialBodyCenterDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Direction "to the center of a celestial body"
         * 
         * @featureDescription Direction from a given origin point described by a PVCoordinatesProvider
         *                     to the center of a celestial body (CelestialBody)
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170, DV-GEOMETRIE_190, DV-ATT_380
         */
        TO_CELECTIAL_BODY_CENTER_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ToCelestialBodyCenterDirectionTest.class.getSimpleName(),
            "To celestial body center direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by a celetial body
     *              (the celestial body's center is the target point),
     *              and getting of the vector associated to a given origin expressed in a frame, at a date.
     * 
     * @input the origin created as basic PVCoordinatesProvider, the celestial body
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one from the origin to the celestial body's center,
     *                   when expressed in the wanted frame. The 1.0e-14 epsilon is the simple double comparison
     *                   epsilon, used because
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

        try {
            // frames creation
            // creation of the earth as CelestialBody
            final CelestialBody earth = CelestialBodyFactory.getEarth();
            final Frame earthFrame = earth.getInertialFrame(IAUPoleModelType.CONSTANT);

            // another frame...
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outFram");

            // origin creation from the earth frame
            final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
            final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
            final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
            final BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            final ToCelestialBodyCenterDirection direction = new ToCelestialBodyCenterDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            final Vector3D result = direction.getVector(inOrigin, date, outputFrame);

            // expected position
            final Vector3D expectedPos = outTransform.transformVector(originPos).scalarMultiply(-1);

            Assert.assertEquals(expectedPos.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getZ(), result.getZ(), this.comparisonEpsilon);
            
            Report.printToReport("Direction", expectedPos, result);
            
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getTargetPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by
     *              a celetial body (the celestial body's center is the target point),
     *              and getting of the target (center of the body) PV coordinates
     *              expressed in a frame, at a date.
     * 
     * @input the origin created as basic PVCoordinatesProvider, the celestial body
     * 
     * @output PVCoordinates
     * 
     * @testPassCriteria the returned target coordinates are equal to zero when transformed in the
     *                   celestial body's local frame. The 1.0e-14 epsilon is the simple double comparison epsilon, used
     *                   because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetTarget() {

        try {
            // frames creation
            // creation of the earth as CelestialBody
            final CelestialBody earth = CelestialBodyFactory.getEarth();
            final Frame earthFrame = earth.getInertialFrame(IAUPoleModelType.CONSTANT);

            // another one
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outFrame");

            // direction creation
            final ToCelestialBodyCenterDirection direction = new ToCelestialBodyCenterDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            final PVCoordinates outOrigin = direction.getTargetPVCoordinates(date, outputFrame);
            final Vector3D resultPos = outOrigin.getPosition();
            final Vector3D resultVel = outOrigin.getVelocity();

            // expected coordinates
            final Vector3D expectedPos = outTransform.transformPosition(Vector3D.ZERO);
            final Vector3D expectedVel = outTransform.transformVector(Vector3D.ZERO);

            Assert.assertEquals(expectedPos.getX(), resultPos.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getY(), resultPos.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expectedPos.getZ(), resultPos.getZ(), this.comparisonEpsilon);

            Assert.assertEquals(expectedVel.getX(), resultVel.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expectedVel.getY(), resultVel.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expectedVel.getZ(), resultVel.getZ(), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described
     *              a celetial body (the celestial body's center is the target point),
     *              and getting of the line containing a given origin and the associateds vector.
     * 
     * @input the origin created as basic PVCoordinatesProvider, the celestial body
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the origin and the (0,0,0) point of the
     *                   celestial body's local frame.
     *                   An exception is returned if those points are identical.
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetLine() {

        try {
            // frames creation
            // creation of the earth as CelestialBody
            final CelestialBody earth = CelestialBodyFactory.getEarth();
            final Frame earthFrame = earth.getInertialFrame(IAUPoleModelType.TRUE);

            // another one
            final Vector3D translationVect = new Vector3D(10.0, 10.0, 10.0);
            final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, translationVect);
            final Frame outputFrame = new Frame(earthFrame, outTransform, "outputFrame");

            // origin creation from the earth frame
            Vector3D originPos = new Vector3D(1.0, 0.0, 0.0);
            final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
            PVCoordinates originPV = new PVCoordinates(originPos, originVel);
            BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            ToCelestialBodyCenterDirection direction = new ToCelestialBodyCenterDirection(earth);

            // test
            // the date has no meaning here
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            // line creation
            final Line line = direction.getLine(inOrigin, date, outputFrame);
            // expected points
            final Vector3D expectedCenter = outTransform.transformPosition(originPos);
            final Vector3D expectedOrigin = outTransform.transformPosition(Vector3D.ZERO);
            // test of the points
            Assert.assertTrue(line.contains(expectedCenter));
            Assert.assertTrue(line.contains(expectedOrigin));

            // test with the origin equal to (0,0,0)
            originPos = Vector3D.ZERO;
            originPV = new PVCoordinates(originPos, originVel);
            inOrigin = new BasicPVCoordinatesProvider(originPV, earthFrame);

            // direction creation
            direction = new ToCelestialBodyCenterDirection(earth);

            try {
                direction.getLine(inOrigin, date, outputFrame);

                // An exception must be thrown !
                Assert.fail();

            } catch (final PatriusException e) {
                // expected
            }

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }
    
    /**
     * @throws PatriusException 
     * @testType UT
     * 
     * @testedFeature {@link features#TO_CELECTIAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getVector(PVCoordinatesProvider, SignalDirection, AbsoluteDate, FixedDate, Frame, double)}
     * 
     * @description Instantiation of a direction described by
     *              a celestial body set as the target point (the earth is the celestial body). The origin object is described by
     *              a {@link LinearTwoPointsPVProvider}. The output result is the direction vector connecting both objects taking into account 
     *              the delay of signal propagation. 
     * 
     * @input the origin is a spacecraft created as {@link LinearTwoPointsPVProvider} 
     * 
     * @output PVCoordinates
     */
    @Test
    public void testGetVectorDelay() throws PatriusException{
        // Time tolerance for convergence
        final double eps = 1E-14;
        
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        // arbitrary fixed date
        final AbsoluteDate fixedDate = t0.shiftedBy(10);
        
        // the target celestial body
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final Frame targetFrame = earth.getInertialFrame(IAUPoleModelType.TRUE);
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame icrf = FramesFactory.getICRF();
        
        final Frame frozenGcrf = targetFrame.getFrozenFrame(FramesFactory.getICRF(), fixedDate, "inertialFrozen");
        final Transform frozenToGcrf = frozenGcrf.getTransformTo(gcrf, fixedDate);
        
        final KeplerianOrbit origin = new KeplerianOrbit(1E7, 0, 0, 0, 0, 0, PositionAngle.TRUE, gcrf, t0,
            Constants.GRIM5C1_EARTH_MU);
        
        // target direction 
        final ToCelestialBodyCenterDirection toEarth = new ToCelestialBodyCenterDirection(earth);
        // target object
        final PVCoordinatesProvider target = toEarth.getTargetPvProvider();

        // expected signal propagation delay between objects 
        final double dt = MathLib.divide(origin.getA(), Constants.SPEED_OF_LIGHT);

        // true signal propagation delay
        double trueDelay;
        
        // PV coordinates of objects
        PVCoordinates pvOrigin;
        PVCoordinates pvTarget;
        
        // Expected and actual direction of signal propagation
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

        // ABERRATION CORRECTION: NONE

        // from origin to target, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, gcrf, eps);
        // position of origin object at emission date
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        // position of target at reception date in frozen frame
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        // transform the direction into gcrf frame at fixed date
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // from origin to target, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, gcrf, eps);
        // target position at fixed date
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        // origin position at emission date
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        // transform the direction vector in gcrf frame at fixed date
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // from target to origin, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, gcrf, eps);
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        // transform the direction vector in gcrf frame at fixed date
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // from target to origin, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        // transform the direction vector in gcrf frame at fixed date
        expected = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // ABERRATION CORRECTION: LIGHT-TIME

        // from origin to target, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.03335972734336223;
        // position of origin object at emission date
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        // position of target at reception date in frozen frame
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate.shiftedBy(trueDelay), frozenGcrf);
        // transform the direction into gcrf frame at fixed date
        expected = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        
        // from origin to target, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.RECEPTION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.03335972733206782;
        // target position at fixed date
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate, frozenGcrf);
        // origin position at emission date
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-trueDelay), frozenGcrf);
        // transform the direction vector in gcrf frame at fixed date
        expected = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);
        
        // from target to origin, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.0333530923552573;
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate, frozenGcrf);
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(trueDelay), frozenGcrf);
        // transform the direction vector in gcrf frame at fixed date
        expected = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // same case with expected signal propagation duration 
        // the tolerance for comparison is higher
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenGcrf);
        expected = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        checkVector3DEquality(expected, actual, 1E-1, 1E-4);
        
        // from target to origin, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, fixedDate,
            FixedDate.RECEPTION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.03335309236659434;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate.shiftedBy(-trueDelay), frozenGcrf);
        // transform the direction vector in gcrf frame at fixed date
        expected = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(pvOrigin.getPosition()));
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // ABERRATION CORRECTION: STELLAR

        // from origin to target, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.EMISSION, gcrf, eps);
        // position of origin object at emission date
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        // position of target at reception date in frozen frame
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        // transform the direction into gcrf frame at fixed date
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

        // from origin to target, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.RECEPTION, gcrf, eps);
        // target position at fixed date
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        // origin position at emission date
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        // transform the direction vector in gcrf frame at fixed date
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = StellarAberrationCorrection.applyTo(target, expectedPreStellarCorrection.negate(), gcrf, t0)
            .negate();
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

        // from target to origin, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.EMISSION, gcrf, eps);
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        // transform the direction vector in gcrf frame at fixed date
        expectedPreStellarCorrection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        expected = StellarAberrationCorrection.applyTo(target, expectedPreStellarCorrection, gcrf, t0);
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

        // from target to origin, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR, t0,
            FixedDate.RECEPTION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = toEarth.getTargetPVCoordinates(t0, gcrf);
        // transform the direction vector in gcrf frame at fixed date
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

        // ABERRATION CORRECTION: ALL

        // from origin to target, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.03335972734336223;
        // position of origin object at emission date
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        // position of target at reception date in frozen frame
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate.shiftedBy(trueDelay), frozenGcrf);
        // transform the direction into gcrf frame at fixed date
        expectedPreStellarCorrection = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(
            pvOrigin.getPosition()));
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
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 2E-10);

        // from origin to target, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.RECEPTION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.03335972733206782;
        // target position at fixed date
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate, frozenGcrf);
        // origin position at emission date
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-trueDelay), frozenGcrf);
        // transform the direction vector in gcrf frame at fixed date
        expectedPreStellarCorrection = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(
            pvOrigin.getPosition()));
        expected = StellarAberrationCorrection.applyTo(target, expectedPreStellarCorrection.negate(), gcrf, fixedDate)
            .negate();
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
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 3E-10);

        // from target to origin, fixed date is signal emission date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.EMISSION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.0333530923552573;
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate, frozenGcrf);
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(trueDelay), frozenGcrf);
        // transform the direction vector in gcrf frame at fixed date
        expectedPreStellarCorrection = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(
            pvOrigin.getPosition()).negate());
        expected = StellarAberrationCorrection.applyInverseTo(target, expectedPreStellarCorrection, gcrf, fixedDate)
            .negate();
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
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 3E-10);

        // same case with expected signal propagation duration
        // the tolerance for comparison is higher
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(dt), frozenGcrf);
        expectedPreStellarCorrection = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(
            pvOrigin.getPosition()));
        expected = StellarAberrationCorrection.applyTo(target, expectedPreStellarCorrection, gcrf, fixedDate);
        // low precision since the expected signal propagation duration is used
        checkVector3DEquality(expected, actual, 1E-1, 1);

        // from target to origin, fixed date is signal reception date
        actual = toEarth.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL, fixedDate,
            FixedDate.RECEPTION, gcrf, eps);
        // true signal propagation duration
        trueDelay = 0.03335309236659434;
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        pvTarget = toEarth.getTargetPVCoordinates(fixedDate.shiftedBy(-trueDelay), frozenGcrf);
        // transform the direction vector in gcrf frame at fixed date
        expectedPreStellarCorrection = frozenToGcrf.transformVector(pvTarget.getPosition().subtract(
            pvOrigin.getPosition()).negate());
        expected = StellarAberrationCorrection.applyTo(origin, expectedPreStellarCorrection.negate(), gcrf, fixedDate);
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
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 3E-10);
    }
    
    /**
     * @throws PatriusException
     * @throws
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link ToCelestialBodyCenterDirection#getLine(PVCoordinatesProvider, SignalDirection, AbsoluteDate, FixedDate, Frame, double)}
     * 
     * @description Instantiation of a direction described
     *              a central body (the central body's center is the target point),
     *              and getting the line joining both objects taking into account the delay of the signal propagation.
     *              The {@link PVCoordinatesProvider} of the origin is built from a {@link KeplerianOrbit}.
     * 
     * @output Line
     */
    @Test
    public void testGetLineDelayKeplerianMotion() throws PatriusException{
        // Time tolerance for convergence
        final double eps = 1E-14;
        
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        // arbitrary fixed date for signal emission/reception
        final AbsoluteDate fixedDate = t0.shiftedBy(10);
        
        // the target celestial body
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final Frame bodyFrame = earth.getInertialFrame(IAUPoleModelType.TRUE);
        // reference frame for testing : earth centered frame
        final Frame gcrf = FramesFactory.getGCRF();
        // frame computation
        final Frame frozenGcrf = bodyFrame.getFrozenFrame(FramesFactory.getICRF(), fixedDate, "inertialFrozen");
        final Transform frozenToGcrf = frozenGcrf.getTransformTo(gcrf, fixedDate);
        
        // earth direction
        final ToCelestialBodyCenterDirection earthDirection = new ToCelestialBodyCenterDirection(earth);

        // -------- the origin object with a keplerian motion (circular)
        final KeplerianOrbit origin = new KeplerianOrbit(1E7, 0., 0., 0., 0., 0., PositionAngle.MEAN, gcrf, t0, Constants.GRIM5C1_EARTH_MU);

        // signal propagation duration is not time dependent (circular orbit)
        final double dt = MathLib.divide(origin.getA(), Constants.SPEED_OF_LIGHT);
 
        Line actual;
        Vector3D direction;
        
        PVCoordinates pvOrigin;
        PVCoordinates pvTarget;

        // ABERRATION CORRECTION: NONE

        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.EMISSION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = earthDirection.getTargetPVCoordinates(t0, gcrf);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 3E-13);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < 3E-13);

        // from origin to target, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < this.comparisonEpsilon);

        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.EMISSION, gcrf, eps);
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < this.comparisonEpsilon);

        // from target to origin, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.NONE,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 3E-13);
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < 3E-13);

        // ABERRATION CORRECTION: LIGHT-TIME

        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        pvTarget = earthDirection.getTargetPVCoordinates(fixedDate.shiftedBy(dt), frozenGcrf);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon, this.comparisonEpsilon);
        // low precision since the expected signal propagation duration is used
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvOrigin.getPosition())) < 3E-9);
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvTarget.getPosition())) < 1E-1);
        
        // from origin to target, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenGcrf);
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon, this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));
        // low precision since the expected signal propagation duration is used
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvOrigin.getPosition())) < 1E-2);
        
        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(+dt), frozenGcrf);
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon, this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));
        // low precision since the expected signal propagation duration is used
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvOrigin.getPosition())) < 1E-2);
        
        // from target to origin, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        pvTarget = earthDirection.getTargetPVCoordinates(fixedDate.shiftedBy(-dt), frozenGcrf);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon, this.comparisonEpsilon);
        // low precision since the expected signal propagation duration is used
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvOrigin.getPosition())) < 1E-2);
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvTarget.getPosition())) < 1E-1);

        // ABERRATION CORRECTION: STELLAR

        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.EMISSION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(t0, gcrf);
        pvTarget = earthDirection.getTargetPVCoordinates(t0, gcrf);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 2E-9);

        // from origin to target, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < this.comparisonEpsilon);

        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.EMISSION, gcrf, eps);
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));
        Assert.assertTrue(actual.distance(pvTarget.getPosition()) < this.comparisonEpsilon);

        // from target to origin, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.distance(pvOrigin.getPosition()) < 2E-9);

        // ABERRATION CORRECTION: ALL

        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        pvTarget = earthDirection.getTargetPVCoordinates(fixedDate.shiftedBy(dt), frozenGcrf);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        // low precision since the expected signal propagation duration is used
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvOrigin.getPosition())) < 6E-10);

        // from origin to target, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.TOWARD_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(-dt), frozenGcrf);
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));

        // from origin to target, fixed date is signal emission date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.EMISSION, gcrf, eps);
        pvOrigin = origin.getPVCoordinates(fixedDate.shiftedBy(+dt), frozenGcrf);
        checkVector3DEquality(direction.normalize().negate(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        Assert.assertTrue(actual.contains(Vector3D.ZERO));

        // from target to origin, fixed date is signal reception date
        actual = earthDirection.getLine(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        direction = earthDirection.getVector(origin, SignalDirection.FROM_TARGET, AberrationCorrection.ALL,
            fixedDate, FixedDate.RECEPTION, gcrf, eps);
        // position/velocity of objects
        pvOrigin = origin.getPVCoordinates(fixedDate, frozenGcrf);
        pvTarget = earthDirection.getTargetPVCoordinates(fixedDate.shiftedBy(-dt), frozenGcrf);
        checkVector3DEquality(direction.normalize(), actual.getDirection(), this.comparisonEpsilon,
            this.comparisonEpsilon);
        // low precision since the expected signal propagation duration is used
        Assert.assertTrue(actual.distance(frozenToGcrf.transformPosition(pvOrigin.getPosition())) < 2E-9);
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
        Assert.assertEquals(0,MathLib.abs(delta.getY()), absTol);
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
