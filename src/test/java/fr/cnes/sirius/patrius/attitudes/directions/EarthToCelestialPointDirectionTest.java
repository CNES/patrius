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
 * @history creation 30/09/2016
 *
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans l'interface ITargetDirection
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.AberrationCorrection;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection.SignalDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
/**
 * @description <p>
 *              Tests for Earth center to a celestial body direction : the central body's center is the target point.
 *              </p>
 * 
 * @author rodriguest
 * 
 * @version $Id: EarthToCelestialBodyCenterDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 3.3
 * 
 */
public class EarthToCelestialPointDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Direction "Earth center to the center of a center body"
         * 
         * @featureDescription Direction from the Earth center described by a PVCoordinatesProvider
         *                     to the center of a central body
         *                     *
         * @coveredRequirements -
         */
        EARTH_CENTER_TO_CENTRAL_BODY_CENTER_DIRECTION
    }

    /** Celestial body : the Sun. */
    private CelestialBody sun;

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon for relative comparison. */
    private final double relComparisonEpsilon = 4e-11;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EARTH_CENTER_TO_CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthToCelestialPointDirection #getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of the direction Earth center => celestial body center
     *              (the central body's center is the target point),
     *              and getting of the vector associated to a given origin expressed in a frame, at a date.
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one from the origin to the central body's center,
     *                   when expressed in the wanted frame. The 1.0e-14 epsilon is the simple double comparison
     *                   epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetVector() throws PatriusException {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        // frame creation : MOD
        final Frame mod = FramesFactory.getMOD(true);

        // Date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Sun in output frame
        final Vector3D expectedDir = this.sun.getPVCoordinates(date, mod).getPosition();

        // direction creation
        final EarthToCelestialPointDirection direction = new EarthToCelestialPointDirection(this.sun);

        // Actual direction
        final Vector3D actualDir = direction.getVector(null, date, mod);

        // Comparisons
        Assert.assertEquals(MathLib.abs(expectedDir.getX() - actualDir.getX()) / expectedDir.getX(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedDir.getY() - actualDir.getY()) / expectedDir.getY(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedDir.getZ() - actualDir.getZ()) / expectedDir.getZ(), 0.,
            this.comparisonEpsilon);
        Report.printToReport("Direction", expectedDir, actualDir);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthToCelestialPointDirection#getTargetPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by
     *              a central body (the central body's center is the target point),
     *              and getting of the target (center of the body) PV coordinates
     *              expressed in a frame, at a date.
     * 
     * @output PVCoordinates
     * 
     * @testPassCriteria the returned target coordinates are the one expected.
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because the computations
     *                   involve
     *                   here no mechanics algorithms.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetTarget() throws PatriusException {

        // frame creation : MOD
        final Frame mod = FramesFactory.getMOD(true);

        // Date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Sun is the target
        final PVCoordinates target = this.sun.getPVCoordinates(date, mod);
        final Vector3D expectedPos = target.getPosition();
        final Vector3D expectedVel = target.getVelocity();

        // direction creation
        final EarthToCelestialPointDirection direction = new EarthToCelestialPointDirection(this.sun);

        // Actual target
        final PVCoordinates actualTarget = direction.getTargetPVCoordinates(date, mod);
        final Vector3D actualPos = actualTarget.getPosition();
        final Vector3D actualVel = actualTarget.getVelocity();

        // Comparisons
        Assert.assertEquals(MathLib.abs(expectedPos.getX() - actualPos.getX()) / expectedPos.getX(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedPos.getY() - actualPos.getY()) / expectedPos.getY(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedPos.getZ() - actualPos.getZ()) / expectedPos.getZ(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedVel.getX() - actualVel.getX()) / expectedVel.getX(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedVel.getY() - actualVel.getY()) / expectedVel.getY(), 0.,
            this.comparisonEpsilon);
        Assert.assertEquals(MathLib.abs(expectedVel.getX() - actualVel.getX()) / expectedVel.getX(), 0.,
            this.comparisonEpsilon);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthToCelestialPointDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described
     *              by the direction Earth center => celestial body center (the central body's center is the target
     *              point),
     *              and getting of the line containing a given origin and the associated vector.
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the celestial body (the Sun) and the (0,0,0) point of the
     *                   gcrf frame (the Earth).
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetLine() throws PatriusException {

        // frame creation
        final Frame mod = FramesFactory.getMOD(false);

        // Date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Transform from GCRF to MOD
        final Transform outTransform = FramesFactory.getGCRF().getTransformTo(mod, date);

        // Sun in output frame
        final Vector3D sunCenter = this.sun.getPVCoordinates(date, mod).getPosition();  
        // direction creation
        final EarthToCelestialPointDirection direction = new EarthToCelestialPointDirection(this.sun);

        // Actual line
        final Line line = direction.getLine(null, date, mod);

        // expected points : Earth and Sun
        final Vector3D earthCenter = outTransform.transformPosition(Vector3D.ZERO);

        // test of the points : they must be on the line ! (since Sun is far away, distance cannot be accurate)
        Assert.assertTrue(line.distance(sunCenter) < 1E-4);
        Assert.assertTrue(line.contains(sunCenter.normalize()));
        Assert.assertTrue(line.contains(earthCenter));
    }
    
    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthToCelestialPointDirection#getVector(PVCoordinatesProvider, SignalDirection, AbsoluteDate, FixedDate, Frame, double)}
     * 
     * @description Instantiation of a direction described by the direction Earth center => celestial body center (the
     *              central body's center is the target point),and getting the relative position vector between both
     *              points taking into account the delay of the signal propagation.
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the computed vector is oriented, as expected, from the Earth center to the celestial body
     *                   center
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testGetVectorWithDelay() throws PatriusException{      
        // time tolerance
        final double eps = 1E-14;
        
        // Arbitrary fixed date for testing purpose
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        
        // earth to sun direction
        final EarthToCelestialPointDirection earthToSun = new EarthToCelestialPointDirection(this.sun);
        final EarthCenterDirection earthDirection = new EarthCenterDirection();
        
        Vector3D actual;
        Vector3D direction;
        double dt;
        
        PVCoordinates pvTarget;
        PVCoordinates pvOrigin;
        
        // computation of direction vector in the frame attached to the sun
        final Frame bodyFrame = this.sun.getICRF();
        final Frame icrf = FramesFactory.getICRF();
        final Frame frozen = bodyFrame.getFrozenFrame(icrf, t0, " ");
        
        // frame transform
        final Transform frozenToBodyFrame = frozen.getTransformTo(bodyFrame, t0);

        // Expected direction of signal propagation before the application of the stellar aberration correction (this
        // expected pre-stellar correction direction is used to compute the angle with respect to the stellar-corrected
        // direction)
        Vector3D preStellarCorrectionDirection;
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

        // signal direction from target to origin, fixed date is signal emission
        actual = earthToSun.getVector(null, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, bodyFrame);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, bodyFrame);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        Assert.assertEquals(direction.getX(), actual.getX(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getY(), actual.getY(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getZ(), actual.getZ(), this.comparisonEpsilon);

        // signal direction from target to origin, fixed date is signal reception
        actual = earthToSun.getVector(null, SignalDirection.FROM_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, bodyFrame);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, bodyFrame);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        Assert.assertEquals(direction.getX(), actual.getX(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getY(), actual.getY(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getZ(), actual.getZ(), this.comparisonEpsilon);

        // signal direction from origin to target, fixed date is signal emission
        actual = earthToSun.getVector(null, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.EMISSION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, bodyFrame);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, bodyFrame);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        Assert.assertEquals(direction.getX(), actual.getX(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getY(), actual.getY(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getZ(), actual.getZ(), this.comparisonEpsilon);

        // signal direction from origin to target, fixed date is signal reception
        actual = earthToSun.getVector(null, SignalDirection.TOWARD_TARGET, AberrationCorrection.NONE, t0,
            FixedDate.RECEPTION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, bodyFrame);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, bodyFrame);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        Assert.assertEquals(direction.getX(), actual.getX(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getY(), actual.getY(), this.comparisonEpsilon);
        Assert.assertEquals(direction.getZ(), actual.getZ(), this.comparisonEpsilon);

        // ABERRATION CORRECTION: LIGHT-TIME

        // signal direction from target to origin, fixed date is signal emission
        actual = earthToSun.getVector(null, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, t0,
            FixedDate.EMISSION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.6851716175076;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0.shiftedBy(dt), frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        direction = frozenToBodyFrame.transformVector(direction);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);
        
        // signal direction from target to origin, fixed date is signal reception
        actual = earthToSun.getVector(null, SignalDirection.FROM_TARGET, AberrationCorrection.LIGHT_TIME, t0,
            FixedDate.RECEPTION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.6851924239382;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0.shiftedBy(-dt), frozen);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        direction = frozenToBodyFrame.transformVector(direction);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);
        
        // signal direction from origin to target, fixed date is signal emission
        actual = earthToSun.getVector(null, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, t0,
            FixedDate.EMISSION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.6852392104504;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0.shiftedBy(+dt), frozen);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        direction = frozenToBodyFrame.transformVector(direction);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);
        
        // signal direction from origin to target, fixed date is signal reception
        actual = earthToSun.getVector(null, SignalDirection.TOWARD_TARGET, AberrationCorrection.LIGHT_TIME, t0,
            FixedDate.RECEPTION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.68526008592033;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0.shiftedBy(-dt), frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        direction = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        direction = frozenToBodyFrame.transformVector(direction);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);

        // ABERRATION CORRECTION: STELLAR

        // signal direction from target to origin, fixed date is signal emission
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.FROM_TARGET,
            AberrationCorrection.STELLAR, t0, FixedDate.EMISSION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection,
            earthToSun.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.TOWARD_TARGET).negate();
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);

        // signal direction from target to origin, fixed date is signal reception
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.FROM_TARGET,
            AberrationCorrection.STELLAR, t0, FixedDate.RECEPTION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection,
            earthDirection.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.FROM_TARGET);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);

        // signal direction from origin to target, fixed date is signal emission
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.TOWARD_TARGET,
            AberrationCorrection.STELLAR, t0, FixedDate.EMISSION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection,
            earthDirection.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.TOWARD_TARGET);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = earthDirection.getTargetPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in body frame
        sourceVelocityProjInFrame = icrf.getTransformTo(bodyFrame, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);
        // expected delta theta
        expectedDeltaTheta = Vector3D.angle(preStellarCorrectionDirection, direction);
        // actual theta
        actualTheta = Vector3D.angle(preStellarCorrectionDirection, sourceVelocityProjInFrame);
        // actual delta theta
        actualDeltaTheta = MathLib.asin(MathLib.sin(actualTheta) * actualBetaFactor);
        // check delta theta
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 1e-7);

        // signal direction from origin to target, fixed date is signal reception
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.TOWARD_TARGET,
            AberrationCorrection.STELLAR, t0, FixedDate.RECEPTION, bodyFrame, eps);
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection,
            earthToSun.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.FROM_TARGET).negate();
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = earthDirection.getTargetPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in body frame
        sourceVelocityProjInFrame = icrf.getTransformTo(bodyFrame, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);

        // ABERRATION CORRECTION: ALL

        // signal direction from target to origin, fixed date is signal emission
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.FROM_TARGET,
            AberrationCorrection.ALL, t0, FixedDate.EMISSION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.6851716175076;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0.shiftedBy(dt), frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection,
            earthToSun.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.FROM_TARGET);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);

        // signal direction from target to origin, fixed date is signal reception
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.FROM_TARGET,
            AberrationCorrection.ALL, t0, FixedDate.RECEPTION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.6851924239382;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0.shiftedBy(-dt), frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection.negate(),
            earthDirection.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.FROM_TARGET);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);

        // signal direction from origin to target, fixed date is signal emission
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.TOWARD_TARGET,
            AberrationCorrection.ALL, t0, FixedDate.EMISSION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.6852392104504;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0, frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0.shiftedBy(+dt), frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition());
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection,
            earthDirection.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.TOWARD_TARGET);
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = earthDirection.getTargetPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in body frame
        sourceVelocityProjInFrame = icrf.getTransformTo(bodyFrame, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);
        // expected delta theta
        expectedDeltaTheta = Vector3D.angle(preStellarCorrectionDirection, direction);
        // actual theta
        actualTheta = Vector3D.angle(preStellarCorrectionDirection, sourceVelocityProjInFrame);
        // actual delta theta
        actualDeltaTheta = MathLib.asin(MathLib.sin(actualTheta) * actualBetaFactor);
        // check delta theta
        Assert.assertEquals(expectedDeltaTheta, actualDeltaTheta, 1e-7);

        // signal direction from origin to target, fixed date is signal reception
        actual = earthToSun.getVector(earthDirection.getTargetPvProvider(), SignalDirection.TOWARD_TARGET,
            AberrationCorrection.ALL, t0, FixedDate.RECEPTION, bodyFrame, eps);
        // true signal propagation duration
        dt = 490.68526008592033;
        pvOrigin = earthDirection.getTargetPVCoordinates(t0.shiftedBy(-dt), frozen);
        pvTarget = earthToSun.getTargetPVCoordinates(t0, frozen);
        preStellarCorrectionDirection = pvTarget.getPosition().subtract(pvOrigin.getPosition()).negate();
        preStellarCorrectionDirection = frozenToBodyFrame.transformVector(preStellarCorrectionDirection);
        direction = LightAberrationTransformation.applyTo(preStellarCorrectionDirection,
            earthToSun.getTargetPvProvider().getPVCoordinates(t0, bodyFrame).getVelocity(),
            SignalDirection.FROM_TARGET).negate();
        Assert.assertEquals(0., (direction.getX() / actual.getX()) / actual.getX(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getY() / actual.getY()) / actual.getY(), this.relComparisonEpsilon);
        Assert.assertEquals(0., (direction.getZ() / actual.getZ()) / actual.getZ(), this.relComparisonEpsilon);
        // inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = earthDirection.getTargetPVCoordinates(t0, icrf).getVelocity();
        // source velocity projected in body frame
        sourceVelocityProjInFrame = icrf.getTransformTo(bodyFrame, t0).transformVector(sourceVelocityInIcrf);
        // actual beta factor
        actualBetaFactor = sourceVelocityProjInFrame.getNorm() / Constants.SPEED_OF_LIGHT;
        // check beta factor
        Assert.assertEquals(approxExpectedBetaFactor, actualBetaFactor, 2E-6);
    }
    
    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#CENTRAL_BODY_CENTER_DIRECTION}
     * 
     * @testedMethod {@link EarthToCelestialPointDirection#getVector(PVCoordinatesProvider, SignalDirection, AbsoluteDate, FixedDate, Frame, double)}
     * 
     * @description Instantiation of a direction described
     *              by the direction Earth center => celestial body center (the central body's center is the target
     *              point), and getting of the line containing a given origin and the associated vector taking into
     *              account the delay of the signal propagation.
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the direction of the computed line must be equal to the normalized direction returned by
     *                   getVector.
     */
    @Test
    public void testGetLineWithDelay() throws PatriusException{
        
        // frame creation 
        final Frame mod = FramesFactory.getMOD(false);
        
        // Arbitrary date for testing purpose
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        
        // earth to sun direction
        final EarthToCelestialPointDirection earthToSun = new EarthToCelestialPointDirection(this.sun);
        // earth direction
        final EarthCenterDirection earthDirection = new EarthCenterDirection();
        
        // tests both signal propagation direction
        for(final SignalDirection signalDirection : SignalDirection.values()){    
            for (final FixedDate fixedDateType : FixedDate.values()) {
                for (final AberrationCorrection aberrationCorrection : AberrationCorrection.values()) {
                   
                    final Line actual = earthToSun.getLine(earthDirection.getTargetPvProvider(), signalDirection,
                        aberrationCorrection, t0, fixedDateType, mod, this.comparisonEpsilon);
                    final Vector3D direction = earthToSun.getVector(earthDirection.getTargetPvProvider(),
                        signalDirection, aberrationCorrection, t0, fixedDateType, mod, this.comparisonEpsilon)
                        .normalize();
                
                // assert the equality between the getVector method and line direction
                Assert.assertEquals(MathLib.abs(actual.getDirection().getX()), MathLib.abs(direction.getX()), this.comparisonEpsilon);
                Assert.assertEquals(MathLib.abs(actual.getDirection().getY()), MathLib.abs(direction.getY()), this.comparisonEpsilon);
                Assert.assertEquals(MathLib.abs(actual.getDirection().getZ()), MathLib.abs(direction.getZ()), this.comparisonEpsilon);
                }
            }
        }
    }
    
    /**
     * Set up before class.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        this.sun = CelestialBodyFactory.getSun();
    }

}
