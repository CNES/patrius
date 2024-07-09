/**
 * 
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
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link ExtremaLatitudeDetector}.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: ExtremaLatitudeDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ExtremaLatitudeDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the extrema latitude detector
         * 
         * @featureDescription Validate the extrema latitude detector
         * 
         * @coveredRequirements DV-EVT_120
         */
        VALIDATE_EXTREMA_LATITUDE_DETECTOR
    }

    /**
     * A Cartesian orbit used for the tests.
     */
    private static CartesianOrbit tISSOrbit;

    /**
     * A SpacecraftState used for the tests.
     */
    private static SpacecraftState tISSSpState;

    /** Initial propagator date. */
    private static AbsoluteDate iniDate;

    /**
     * Setup for all unit tests in the class.
     * Provides an {@link Orbit}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

        // Some orbit data for the tests
        // (Real ISS data!)
        final double ix = 2156444.05;
        final double iy = 3611777.68;
        final double iz = -5316875.46;
        final double ivx = -6579.446110;
        final double ivy = 3916.478783;
        final double ivz = 8.876119;

        iniDate = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());
        final double mu = CelestialBodyFactory.getEarth().getGM();

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), iniDate, mu);
        tISSSpState = new SpacecraftState(tISSOrbit);
    }

    /**
     * @throws PatriusException
     *         frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLatitudeDetector#ExtremaLatitudeDetector(Integer, Frame) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : extrema mode
     * 
     * @output an {@link ExtremaLatitudeDetector}
     * 
     * @testPassCriteria the {@link ExtremaLatitudeDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testLatitudeDetectorCtor1() throws PatriusException {
        final ExtremaLatitudeDetector detector =
            new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MAX, FramesFactory.getGCRF());
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException
     *         frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLatitudeDetector#ExtremaLatitudeDetector(int, Frame, double, double)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : extrema mode, max check, threshold
     * 
     * @output an {@link ExtremaLatitudeDetector}
     * 
     * @testPassCriteria the {@link ExtremaLatitudeDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testLatitudeDetectorCtor2() throws PatriusException {
        final ExtremaLatitudeDetector detector =
            new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MIN, FramesFactory.getGCRF(), 10, 0.1);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
        // test the throwing of an exception when a constructor parameter is not supported:
        boolean asExpected = false;
        try {
            new ExtremaLatitudeDetector(8, FramesFactory.getGCRF(), 3, 10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);
    }

    /**
     * @throws PatriusException
     *         frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLatitudeDetector#ExtremaLatitudeDetector(Integer, Frame, Action) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : extrema mode StopAction
     * 
     * @output an {@link ExtremaLatitudeDetector}
     * 
     * @testPassCriteria the {@link ExtremaLatitudeDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testLatitudeDetectorCtor3() throws PatriusException {
        final ExtremaLatitudeDetector detector =
            new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MAX, FramesFactory.getGCRF(), 10, 0.1, Action.STOP);
        final ExtremaLatitudeDetector detector2 = (ExtremaLatitudeDetector) detector.copy();
        // Test getter
        Assert.assertEquals(FramesFactory.getGCRF(), detector2.getBodyFrame());
    }

    /**
     * @throws PatriusException
     *         frame exception
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLatitudeDetector#ExtremaLatitudeDetector(Integer, Frame) }
     * 
     * @description error constructor test: the input integer representing the mode is not a valid
     *              value (0 and 1 are the only valid values).
     * 
     * @input constructor parameters : extrema mode
     * 
     * @output none
     * 
     * @testPassCriteria the {@link ExtremaLatitudeDetector} cannot be created (IllegalArgumentException)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testLatitudeDetectorCtorError() throws PatriusException {
        // Illegal integer as parameter
        final ExtremaLatitudeDetector detector =
            new ExtremaLatitudeDetector(Integer.MAX_VALUE, FramesFactory.getGCRF());
        // We should never reach the next line...
        Assert.fail(detector.toString() + " exists but should not...");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLatitudeDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link ExtremaLatitudeDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values for true and false as second parameters
     *                   and for true and false as third parameters
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEventOccurred() throws PatriusException {
        final ExtremaLatitudeDetector detector =
            new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MAX, FramesFactory.getGCRF());
        // Latitude is decreasing and the integration direction is forward
        Action rez = detector.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez);
        // Latitude is increasing and the integration direction is forward
        rez = detector.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez);
        // Latitude is decreasing and the integration direction is backward
        rez = detector.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.STOP, rez);
        // Latitude is increasing and the integration direction is backward
        rez = detector.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLatitudeDetector#g(SpacecraftState)}
     * 
     * @description tests {@link ExtremaLatitudeDetector#g(SpacecraftState)} propagating an elliptical
     *              orbit during one period: the first part of the test associates a maximal latitude detector to the
     *              propagator and verifies that the propagation stops when the satellite is at perigee, the second part
     *              of the test associates a minimal latitude detector and verifies that the propagation stops when
     *              the satellite is at apogee.
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when a maximal or a minimal latitude is detected.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEllipticOrbit1() throws PatriusException {
        // set up the elliptic orbit :
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Orbit orbit = new KeplerianOrbit(9000000, 0.2, MathLib.toRadians(40), MathLib.toRadians(90),
            0, MathLib.toRadians(-10), PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        // detects the maximal latitude:
        final ExtremaLatitudeDetector detectorMax = new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MAX,
            FramesFactory.getGCRF(), 600, 1e-12);
        propagator.addEventDetector(detectorMax);
        SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        // check the maximum is when the satellite is at perigee:
        Assert.assertEquals(FastMath.PI / 2, curState.getLv(), Utils.epsilonTest);
        Assert.assertEquals(9000000 * (1 - 0.2), curState.getPVCoordinates().getPosition().getNorm(), 1E-9);

        propagator.clearEventsDetectors();
        // detects the minimal latitude:
        final ExtremaLatitudeDetector detectorMin = new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MIN,
            FramesFactory.getGCRF(), 600, 1e-12);
        propagator.addEventDetector(detectorMin);
        curState = propagator.propagate(iniDate.shiftedBy(period));
        // check the minimum is when the satellite is at apogee:
        Assert.assertEquals(FastMath.PI * 3 / 2, curState.getLv(), Utils.epsilonTest);
        Assert
            .assertEquals(9000000 * (1 + 0.2), curState.getPVCoordinates().getPosition().getNorm(), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_LATITUDE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaLatitudeDetector#g(SpacecraftState)}
     * 
     * @description tests {@link ExtremaLatitudeDetector#g(SpacecraftState)} propagating an elliptical
     *              orbit with perigee argument = O during one period: the first part of the test associates a maximal
     *              latitude detector to the propagator and verifies that the propagation stops when the satellite
     *              eccentric anomaly is equal to PI/2, the second part of the test associates a minimal latitude
     *              detector and verifies that the propagation stops when the satellite eccentric anomaly is 3/2PI.
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g is zero when a maximal or a minimal latitude is detected.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEllipticOrbit2() throws PatriusException {
        // set up the elliptic orbit :
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Orbit orbit = new KeplerianOrbit(10000000, 0.15, MathLib.toRadians(30), 0,
            0, MathLib.toRadians(-10), PositionAngle.TRUE, FramesFactory.getGCRF(), iniDate, mu);
        final double period = orbit.getKeplerianPeriod();
        final Propagator propagator = new KeplerianPropagator(orbit);

        // detects the maximal latitude:
        final ExtremaLatitudeDetector detectorMax = new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MAX,
            FramesFactory.getGCRF());
        propagator.addEventDetector(detectorMax);
        SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(period));
        // check the maximum is when the eccentric anomaly of the satellite is PI/2
        Assert.assertEquals(FastMath.PI / 2, curState.getLE(), Utils.epsilonTest);

        propagator.clearEventsDetectors();
        // detects the minimal latitude:
        final ExtremaLatitudeDetector detectorMin = new ExtremaLatitudeDetector(ExtremaLatitudeDetector.MIN,
            FramesFactory.getGCRF());
        propagator.addEventDetector(detectorMin);
        curState = propagator.propagate(iniDate.shiftedBy(period));
        // check the minimum is when the eccentric anomaly of the satellite is 3/2PI
        Assert.assertEquals(FastMath.PI * 3 / 2, curState.getLE(), 1E-9);
    }
}
