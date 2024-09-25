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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import org.junit.Assert;
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
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagation;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link LightAberrationTransformation} class.
 *
 * @author bonitt
 *
 * @since 4.13
 */
public class LightAberrationTransformationTest {

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedMethod {@link LightAberrationTransformation#applyTo(Vector3D, Vector3D, SignalDirection)}
     * @testedMethod {@link LightAberrationTransformation#computeAberrationAngle(Vector3D, Vector3D)}
     * 
     * @description Instantiation of a direction described a central body (the central body's center is the target
     *              point), and getting the direction vector taking into account the stellar aberration. The
     *              {@link PVCoordinatesProvider} of the observer is built from a {@link KeplerianOrbit}. The validation
     *              concerns not only the final direction which goes from the observer to the corrected target position,
     *              but also the delta of the angle which lies between the observer-target direction and the observer's
     *              velocity vector.
     *              <p>
     *              Note: this test is reused from the previous class StellarAberrationCorrectionTest replaced by this
     *              one and adjusted for the {@link LightAberrationTransformation} computation features.<br>
     *              Once the values have been compared between the two class, non regression values are then used in
     *              this test.
     *              </p>
     * 
     * @output Vector3D (direction) and double (delta of angle)
     */
    @Test
    public void testApplyTo() throws PatriusException {

        // Time tolerance for convergence
        final double eps = 1e-14;
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        // Reference frame for testing : earth centered frame
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame icrf = FramesFactory.getICRF();

        // Earth direction
        final EarthCenterDirection earthDirection = new EarthCenterDirection();
        // Target object
        final PVCoordinatesProvider target = earthDirection.getTargetPvProvider();

        // -------- the observer object with a keplerian motion (circular) --------
        final KeplerianOrbit observer = new KeplerianOrbit(1E7, 0., 0., 0., 0., 0., PositionAngle.MEAN, gcrf,
            t0, Constants.GRIM5C1_EARTH_MU);

        final Transform t = icrf.getTransformTo(gcrf, t0);

        // Direction towards the target
        Vector3D dirVector;
        Vector3D dirVectorInIcrf;

        // Expected and actual direction of signal propagation
        Vector3D expected;
        Vector3D actual;

        // Inertial velocity of source in ICRF frame
        Vector3D sourceVelocityInIcrf;
        // Delta theta
        double deltaTheta;

        // ABERRATION CORRECTION: STELLAR

        // Direction vector computed in GCRF

        // From target to observer, fixed date is signal reception date, stellar aberration correction
        actual = earthDirection.getVector(observer, SignalDirection.FROM_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // Apply the stellar aberration correction
        dirVector = earthDirection.getVector(observer, t0, gcrf);
        expected = LightAberrationTransformation.applyTo(dirVector, observer.getPVCoordinates(t0, gcrf).getVelocity(),
            SignalDirection.FROM_TARGET);
        // Check direction vector
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // Inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = observer.getPVCoordinates(t0, icrf).getVelocity();
        // Direction in ICRF frame
        dirVectorInIcrf = t.transformVector(dirVector);
        // Compute delta theta
        deltaTheta = LightAberrationTransformation.computeAberrationAngle(dirVectorInIcrf, sourceVelocityInIcrf);
        // Check delta theta
        Assert.assertEquals(-8.440247313779015E-6, deltaTheta, this.comparisonEpsilon); // Non regression value

        // From observer to target, fixed date is signal reception date, stellar aberration correction
        actual = earthDirection.getVector(observer, SignalDirection.TOWARD_TARGET, AberrationCorrection.STELLAR,
            t0, FixedDate.RECEPTION, gcrf, eps);
        // Apply the stellar aberration correction
        dirVector = earthDirection.getVector(observer, t0, gcrf);
        expected = LightAberrationTransformation.applyTo(dirVector, target.getPVCoordinates(t0, gcrf).getVelocity(),
            SignalDirection.TOWARD_TARGET);
        // Check direction vector
        checkVector3DEquality(expected, actual, this.comparisonEpsilon, this.comparisonEpsilon);

        // Inertial velocity of source in ICRF frame
        sourceVelocityInIcrf = target.getPVCoordinates(t0, icrf).getVelocity();
        // Direction in ICRF frame
        dirVectorInIcrf = icrf.getTransformTo(gcrf, t0).transformVector(dirVector);
        // Compute delta theta
        deltaTheta = LightAberrationTransformation.computeAberrationAngle(dirVectorInIcrf, sourceVelocityInIcrf);
        // Check delta theta
        Assert.assertEquals(-1.828547498655686E-5, deltaTheta, this.comparisonEpsilon); // Non regression value
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @testedMethod {@link LightAberrationTransformation#LightAberrationTransformation(VacuumSignalPropagation, SignalDirection)}
     * @testedMethod {@link LightAberrationTransformation#LightAberrationTransformation(Vector3D, Vector3D, SignalDirection)}
     * @testedMethod {@link LightAberrationTransformation#getAberrationAngle()}
     * @testedMethod {@link LightAberrationTransformation#getTransformedToTargetDirection()}
     * @testedMethod {@link LightAberrationTransformation#getSignalDirection()}
     * 
     * @description Evaluate the constructors and getters.
     */
    @Test
    public void testConstructor() throws PatriusException {

        // Time tolerance for convergence
        final double eps = 1e-14;
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        // Reference frame for testing : earth centered frame
        final Frame gcrf = FramesFactory.getGCRF();

        // Earth direction
        final EarthCenterDirection earthDirection = new EarthCenterDirection();
        // Target object
        final PVCoordinatesProvider target = earthDirection.getTargetPvProvider();

        // -------- the observer object with a keplerian motion (circular) --------
        final KeplerianOrbit observer = new KeplerianOrbit(1E7, 0., 0., 0., 0., 0., PositionAngle.MEAN, gcrf,
            t0, Constants.GRIM5C1_EARTH_MU);

        // Direction towards the target
        Vector3D dirVector;

        // Expected and actual direction of signal propagation
        Vector3D expected;

        LightAberrationTransformation lightAberrationTransformation;

        // Build a signal propagation
        final VacuumSignalPropagationModel model =
            new VacuumSignalPropagationModel(gcrf, eps, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);
        final VacuumSignalPropagation signalPropagation =
            model.computeSignalPropagation(target, observer, t0, FixedDate.RECEPTION);

        // #1: FROM_TARGET
        lightAberrationTransformation =
            new LightAberrationTransformation(signalPropagation, SignalDirection.FROM_TARGET);

        dirVector = earthDirection.getVector(observer, t0, gcrf);
        expected = LightAberrationTransformation.applyTo(dirVector, observer.getPVCoordinates(t0,
            gcrf).getVelocity(), SignalDirection.FROM_TARGET);

        Assert.assertEquals(-2.1059506253529037E-5, lightAberrationTransformation.getAberrationAngle(),
            this.comparisonEpsilon);
        Assert.assertEquals(expected, lightAberrationTransformation.getTransformedToTargetDirection());
        Assert.assertEquals(SignalDirection.FROM_TARGET, lightAberrationTransformation.getSignalDirection());

        // #1: TOWARD_TARGET
        lightAberrationTransformation =
            new LightAberrationTransformation(signalPropagation, SignalDirection.TOWARD_TARGET);

        expected = dirVector.negate();

        Assert.assertEquals(0., lightAberrationTransformation.getAberrationAngle(), this.comparisonEpsilon);
        Assert.assertEquals(expected, lightAberrationTransformation.getTransformedToTargetDirection());
        Assert.assertEquals(SignalDirection.TOWARD_TARGET, lightAberrationTransformation.getSignalDirection());
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

    /** Set up. */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-dataCNES-2003");
    }
}