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
 * @history created 27/02/12
 *
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
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
import fr.cnes.sirius.patrius.math.util.MathLib;
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
 * Unit tests for {@link DistanceDetector}.
 * 
 * @author cardosop
 * 
 * @version $Id: DistanceDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class DistanceDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the distance detector
         * 
         * @featureDescription Validate the distance detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_DISTANCE_DETECTOR
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
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#DistanceDetector (org.orekit.utils.PVCoordinatesProvider, int) }
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : reference body, distance
     * 
     * @output a {@link DistanceDetector}
     * 
     * @testPassCriteria the {@link DistanceDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testDistanceDetectorCtor() {
        final DistanceDetector detector =
            new DistanceDetector(theSun, 12345.);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#DistanceDetector (org.orekit.utils.PVCoordinatesProvider, int, double, double)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : reference body, distance, max check, threshold
     * 
     * @output a {@link DistanceDetector}
     * 
     * @testPassCriteria the {@link DistanceDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testDistanceDetectorCtor2() {
        final DistanceDetector detector =
            new DistanceDetector(theSun, 54321., 10, 0.1);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#DistanceDetector (org.orekit.utils.PVCoordinatesProvider, int, double, double, Action)}
     * 
     * @description constructor test
     * 
     * @input constructor parameters : reference body, distance, max check, threshold and STOP.Action
     * 
     * @output a {@link DistanceDetector}
     * 
     * @testPassCriteria the {@link DistanceDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testDistanceDetectorCtor3() {
        final DistanceDetector detector =
            new DistanceDetector(theSun, 54321., 10, 0.1, Action.STOP);
        final DistanceDetector detector2 = (DistanceDetector) detector.copy();
        // Test getter
        Assert.assertEquals(54321., detector2.getDistance());
        Assert.assertEquals(theSun, detector2.getBody());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#DistanceDetector (org.orekit.utils.PVCoordinatesProvider, int) }
     * 
     * @description error constructor test
     * 
     * @input constructor parameters : reference body, negative distance
     * 
     * @output none
     * 
     * @testPassCriteria the {@link DistanceDetector} cannot be created (IllegalArgumentException)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testDistanceDetectorCtorError() {
        // Illegal integer as parameter
        final DistanceDetector detector =
            new DistanceDetector(theSun, -1.);
        // We should never reach the next line...
        Assert.fail(detector.toString() + " exists but should not...");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link DistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values for true and false as second parameters
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
        final DistanceDetector detector =
            new DistanceDetector(theSun, 2121.);
        // Distance is decreasing
        Action rez = detector.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez);
        // Distance is increasing
        rez = detector.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#g(SpacecraftState)}
     * 
     * @description tests {@link DistanceDetector#g(SpacecraftState)}
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
        final DistanceDetector detector = new DistanceDetector(theSun, 333.);
        final double expectedDot = 1.4818489552866605E11;
        Assert.assertEquals(expectedDot, detector.g(tISSSpState), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#g(SpacecraftState)}
     * @testedMethod {@link DistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description Propagation test 01 : detecting the distance from the sun
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
        final double propagShift = 100000.;
        // Note : result changed for PATRIUS 4.2
        final double expectedShift = 1481.1756959904353;
        final double distance = 1.4818E11;

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(tISSOrbit);

        // Detector for the given distance to the sun
        final DistanceDetector detector = new DistanceDetector(theSun, distance);
        propagator.addEventDetector(detector);

        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(propagShift));

        Assert.assertEquals(expectedShift, curState.getDate().durationFrom(iniDate), Utils.epsilonTest);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_DISTANCE_DETECTOR}
     * 
     * @testedMethod {@link DistanceDetector#g(SpacecraftState)}
     * @testedMethod {@link DistanceDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description detecting distance between two coplanar circular orbit
     * 
     * @input propagator, two {@link EquinoctialOrbit}
     * 
     * @output resulting SpacecraftState from the propagation
     * 
     * @testPassCriteria the angle between the position of satellite 1 and satellite 2 should be
     *                   equal to PI/2 when the distance event is detected (distance = sqrt(a1*a1 + a2*a2))
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
        final double propagShift = 100000.;

        final double mu = CelestialBodyFactory.getEarth().getGM();
        // first equatorial orbit :
        final double a1 = 8000000;
        final Orbit orbit1 = new EquinoctialOrbit(a1, 0, 0, 0, 0, FastMath.PI, PositionAngle.MEAN,
            FramesFactory.getGCRF(), iniDate, mu);
        // second equatorial orbit :
        final double a2 = 8200000;
        final Orbit orbit2 = new EquinoctialOrbit(a2, 0, 0, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), iniDate, mu);

        // propagator:
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit1);
        // detects the following distance between satellite 1 and satellite 2:
        final double distance = MathLib.sqrt(a1 * a1 + a2 * a2);
        final DistanceDetector detector = new DistanceDetector(orbit2, distance);
        propagator.addEventDetector(detector);

        // the propagation should stop when the distance is reached:
        final SpacecraftState curState = propagator.propagate(iniDate.shiftedBy(propagShift));

        // position of the satellites when event is detected:
        final Vector3D position1 = orbit1.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();
        final Vector3D position2 = orbit2.getPVCoordinates(curState.getDate(), FramesFactory.getGCRF()).getPosition();

        // computes the angle between the two satellites:
        final double angle = Vector3D.angle(position1, position2);
        // verifies that this angle is = PI:
        Assert.assertEquals(FastMath.PI / 2, angle, Utils.epsilonTest);
    }
}
