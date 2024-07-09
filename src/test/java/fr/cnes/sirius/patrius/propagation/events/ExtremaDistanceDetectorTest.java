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
 * @history created 27/02/12
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link ExtremaDistanceDetector}.
 * 
 * @author cardosop
 * 
 * @version $Id: ExtremaDistanceDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ExtremaDistanceDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the extrema distance detector
         * 
         * @featureDescription Validate the extrema distance detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_EXTREMA_DISTANCE_DETECTOR
    }

    /**
     * A Cartesian orbit used for the tests.
     */
    private static CartesianOrbit tISSOrbit;

    /**
     * A SpacecraftState used for the tests.
     */
    private static SpacecraftState tISSSpState;

    /** The Sun. */
    private static CelestialBody theSun;

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
        Utils.setDataRoot("regular-dataCNES-2003/de406-ephemerides");
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
        theSun = CelestialBodyFactory.getSun();

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), iniDate, mu);
        tISSSpState = new SpacecraftState(tISSOrbit);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#ExtremaDistanceDetector (org.orekit.utils.PVCoordinatesProvider, int) }
     * @testedMethod {@link ExtremaDistanceDetector#getBody() }
     * 
     * @description simple constructor test + test getter
     * 
     * @input constructor parameters : reference body, mode
     * 
     * @output an {@link ExtremaDistanceDetector}
     * 
     * @testPassCriteria the {@link ExtremaDistanceDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testDistanceDetectorCtor() {
        final ExtremaDistanceDetector detector =
            new ExtremaDistanceDetector(theSun, ExtremaDistanceDetector.MAX);
        final ExtremaDistanceDetector detector2 = (ExtremaDistanceDetector) detector.copy();
        // The constructor did not crash...
        Assert.assertNotNull(detector);
        // test getter
        Assert.assertEquals(theSun.hashCode(), detector2.getBody().hashCode());
        // test the throwing of an exception when a constructor parameter is not supported:
        boolean asExpected = false;
        try {
            new ExtremaDistanceDetector(theSun, 100);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#ExtremaDistanceDetector (org.orekit.utils.PVCoordinatesProvider, int, double, double)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : reference body, mode, max check, threshold
     * 
     * @output an {@link ExtremaDistanceDetector}
     * 
     * @testPassCriteria the {@link ExtremaDistanceDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testDistanceDetectorCtor2() {
        final ExtremaDistanceDetector detector =
            new ExtremaDistanceDetector(theSun, ExtremaDistanceDetector.MIN, 10, 0.1);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#ExtremaDistanceDetector (org.orekit.utils.PVCoordinatesProvider, int) }
     * 
     * @description error constructor test
     * 
     * @input constructor parameters : reference body, mode
     * 
     * @output none
     * 
     * @testPassCriteria the {@link ExtremaDistanceDetector} cannot be created (IllegalArgumentException)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testDistanceDetectorCtorError() {
        // Illegal integer as parameter
        final ExtremaDistanceDetector detector =
            new ExtremaDistanceDetector(theSun, Integer.MAX_VALUE);
        // We should never reach the next line...
        Assert.fail(detector.toString() + " exists but should not...");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link ExtremaDistanceDetector#ExtremaDistanceDetector(org.orekit.utils.PVCoordinatesProvider, double, double, Action, Action)}
     * @testedMethod {@link ExtremaDistanceDetector#ExtremaDistanceDetector(org.orekit.utils.PVCoordinatesProvider, int, double, double, Action)}
     * 
     * @description tests {@link ExtremaDistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
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
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testEventOccurred() throws PatriusException {
        final ExtremaDistanceDetector detector =
            new ExtremaDistanceDetector(theSun, ExtremaDistanceDetector.MAX);
        // Distance is decreasing and integration direction is forward
        Action rez = detector.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is increasing and integration direction is forward
        rez = detector.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is decreasing and integration direction is backward
        rez = detector.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is increasing and integration direction is backward
        rez = detector.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.STOP, rez);

        final ExtremaDistanceDetector detectorMIN_MAX =
            new ExtremaDistanceDetector(theSun, AbstractDetector.DEFAULT_MAXCHECK,
                AbstractDetector.DEFAULT_THRESHOLD, Action.CONTINUE, Action.CONTINUE);
        // Distance is decreasing and integration direction is forward
        rez = detectorMIN_MAX.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.CONTINUE, rez);
        // Distance is increasing and integration direction is forward
        rez = detectorMIN_MAX.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.CONTINUE, rez);
        // Distance is decreasing and integration direction is backward
        rez = detectorMIN_MAX.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.CONTINUE, rez);
        // Distance is increasing and integration direction is backward
        rez = detectorMIN_MAX.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.CONTINUE, rez);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#g(SpacecraftState)}
     * 
     * @description tests {@link ExtremaDistanceDetector#g(SpacecraftState)}
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g returns the expected value
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testG() throws PatriusException {
        final ExtremaDistanceDetector detector = new ExtremaDistanceDetector(theSun, ExtremaDistanceDetector.MAX);
        // Note : result changed for PATRIUS 4.2
        final double expectedDot = -3.45099753511631E14;
        Assert.assertEquals(expectedDot, detector.g(tISSSpState), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#g(SpacecraftState)}
     * @testedMethod {@link ExtremaDistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Propagation test 01 : detecting max distance from the sun
     * 
     * @input propagator, DistanceDetector input parameters
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria resulting SpacecraftState at the expected date
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testPropagation01() throws PatriusException {
        final double propagShift = 10000.;
        // Note : result changed for PATRIUS 4.2
        final double expectedShift = 4869.047601269994;

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(tISSOrbit);

        // Detector for maximum distance to the sun
        final ExtremaDistanceDetector detector = new ExtremaDistanceDetector(theSun, ExtremaDistanceDetector.MAX, 600,
            1e-12);
        propagator.addEventDetector(detector);

        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(propagShift));

        // System.out.println(iniDate.toString());
        // System.out.println(curState.getDate().toString());
        Assert.assertEquals(expectedShift, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#g(SpacecraftState)}
     * @testedMethod {@link ExtremaDistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Propagation test 01 : detecting min distance from the sun
     * 
     * @input propagator, DistanceDetector input parameters
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria resulting SpacecraftState at the expected date
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testPropagation02() throws PatriusException {
        final double propagShift = 10000.;
        // Note : result changed for PATRIUS 4.2
        final double expectedShift = 2327.393721024023;

        // Propagator
        KeplerianPropagator propagator = new KeplerianPropagator(tISSOrbit);

        // Detector for minimum distance to the sun
        final ExtremaDistanceDetector detector = new ExtremaDistanceDetector(theSun, ExtremaDistanceDetector.MIN);
        propagator.addEventDetector(detector);

        SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(propagShift));

        // System.out.println(iniDate.toString());
        // System.out.println(curState.getDate().toString());

        Assert.assertEquals(expectedShift, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);

        /*
         * Same test with MIN_MAX detector
         */
        final ExtremaDistanceDetector detectorMINMAX = new ExtremaDistanceDetector(theSun,
            ExtremaDistanceDetector.MIN_MAX);
        propagator = new KeplerianPropagator(tISSOrbit);
        propagator.addEventDetector(detectorMINMAX);
        curState = propagator.propagate(iniDate.shiftedBy(propagShift));
        Assert.assertEquals(expectedShift, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EXTREMA_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link ExtremaDistanceDetector#g(SpacecraftState)}
     * @testedMethod {@link ExtremaDistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description detecting maximal and minimal distance between two coplanar circular orbit
     * 
     * @input propagator, two {@link EquinoctialOrbit}
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria the angle between the position of satellite 1 and satellite 2 should be
     *                   equal to PI when the maximum distance event is detected, and it should be equal to zero when
     *                   the minimal distance is detected.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testTwoEquatorialOrbits() throws PatriusException {
        final double propagShift = 1000000.;

        final double mu = CelestialBodyFactory.getEarth().getGM();
        // first equatorial orbit :
        final Orbit orbit1 = new EquinoctialOrbit(8000000, 0, 0, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), iniDate, mu);
        // second equatorial orbit :
        final Orbit orbit2 = new EquinoctialOrbit(8200000, 0, 0, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), iniDate, mu);

        // propagator:
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit1);
        // detects the maximum distance between satellite 1 and satellite 2:
        final ExtremaDistanceDetector detector1 = new ExtremaDistanceDetector(orbit2, ExtremaDistanceDetector.MAX);
        propagator.addEventDetector(detector1);

        // the propagation should stop when the maximum distance is reached:
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(propagShift));

        // position of the satellites when event is detected:
        Vector3D position1 = orbit1.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();
        Vector3D position2 = orbit2.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();

        // computes the angle between the two satellites:
        double angle = Vector3D.angle(position1, position2);
        // verifies that this angle is = PI:
        Assert.assertEquals(FastMath.PI, angle, Utils.epsilonTest);

        // propagator:
        propagator.clearEventsDetectors();
        // detects the minimum distance between satellite 1 and satellite 2:
        final ExtremaDistanceDetector detector2 = new ExtremaDistanceDetector(orbit2, ExtremaDistanceDetector.MIN);
        propagator.addEventDetector(detector2);

        // the propagation should stop when the minimum distance is reached:
        final SpacecraftState curState2 = propagator.propagate(iniDate.shiftedBy(propagShift));

        // position of the satellites when event is detected:
        position1 = orbit1.getPVCoordinates(curState2.getDate(), FramesFactory.getGCRF()).getPosition();
        position2 = orbit2.getPVCoordinates(curState2.getDate(), FramesFactory.getGCRF()).getPosition();

        // computes the angle between the two satellites:
        angle = Vector3D.angle(position1, position2);
        // verifies that this angle is = 0:
        Assert.assertEquals(0, angle, Utils.epsilonTest);
    }
}
