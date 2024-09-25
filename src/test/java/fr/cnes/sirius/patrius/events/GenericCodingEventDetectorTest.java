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
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3146:10/05/2022:[PATRIUS] Ajout d'une methode static logEventsOverTimeInterval
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Add exception to SpacececraftState.getAttitude()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.SortedSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AbstractAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEvent;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsLogger.LoggedCodedEvent;
import fr.cnes.sirius.patrius.events.postprocessing.CodingEventDetector;
import fr.cnes.sirius.patrius.events.postprocessing.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link GenericCodingEventDetector}.<br>
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class GenericCodingEventDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the generic coding event detector
         * 
         * @featureDescription Validate the generic coding event detector
         * 
         * @coveredRequirements DV-EVT_10, DV-EVT_60, DV-TRAJ_190
         */
        VALIDATE_GENERIC_CODING_EVENT_DETECTOR
    }

    /**
     * A visibility detector.
     */
    private static CircularFieldOfViewDetector visi;

    /**
     * A spacecraft state.
     */
    private static SpacecraftState state;

    /**
     * A date.
     */
    private static AbsoluteDate date;

    /**
     * Setup for all unit tests in the class. Provides two strings, a {@link AbsoluteDate} and a
     * boolean.
     * 
     * @throws PatriusException should not happen here
     */
    @Before
    public void setUp() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Set up the visibility event:
        // Circular field of view detector
        final CelestialBodyFrame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid body = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frame);
        final EllipsoidPoint coord = new EllipsoidPoint(body, body.getLLHCoordinatesSystem(), MathLib.toRadians(5),
            MathLib.toRadians(120), 0, "");
        final TopocentricFrame station = new TopocentricFrame(coord, "Station1");
        final Vector3D center = Vector3D.PLUS_I;
        final double aperture = MathLib.toRadians(35);
        visi = new CircularFieldOfViewDetector(station, center, aperture, 0.1);

        // Set up the spacecraft state:
        date = new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getTT());
        final Orbit orbit = new KeplerianOrbit(1e07, 0.1, 0.1, 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit,
            orbit.getDate(), orbit.getFrame());
        state = new SpacecraftState(orbit, attitude);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#GenericCodingEventDetector(EventDetector, String, String, boolean, String)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : {@link EventDetector}, code "increasing", code "decreasing",
     *        boolean, code "phenomenon"
     * 
     * @output an {@link GenericCodingEventDetector}
     * 
     * @testPassCriteria the {@link GenericCodingEventDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGenericCodingEventPhen() {
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", true, "Visibility");
        // Check the constructor did not crash:
        Assert.assertNotNull(detector);

        // Copy
        final GenericCodingEventDetector detectorCopy = (GenericCodingEventDetector) detector
            .copy();
        Assert.assertEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#GenericCodingEventDetector(EventDetector, String, String)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : {@link EventDetector}, code "increasing", code "decreasing"
     * 
     * @output an {@link GenericCodingEventDetector}
     * 
     * @testPassCriteria the {@link GenericCodingEventDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGenericCodingEventNoPhen() {

        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT");
        // Check the constructor did not crash:
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#GenericCodingEventDetector(EventDetector, String, String, boolean, String, double, int)}
     * @testedMethod {@link GenericCodingEventDetector#GenericCodingEventDetector(EventDetector, String, String, double, int)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output some {@link GenericCodingEventDetector}
     * 
     * @testPassCriteria the {@link GenericCodingEventDetector} are successfully created
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testGenericCodingEventDelayOcc() {
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector1 = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", 0, 0);
        // Check the constructor did not crash:
        Assert.assertNotNull(detector1);
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector2 = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", 756.32, 0);
        // Check the constructor did not crash:
        Assert.assertNotNull(detector2);
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector3 = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", true, "VISI", 0, 0);
        // Check the constructor did not crash:
        Assert.assertNotNull(detector3);
    }

    /**
     * @throws PatriusException from the wrapped event detector.
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * @testedMethod {@link GenericCodingEventDetector#g(SpacecraftState)}
     * @testedMethod {@link GenericCodingEventDetector#resetState(SpacecraftState)}
     * @testedMethod {@link GenericCodingEventDetector#getThreshold()}
     * @testedMethod {@link GenericCodingEventDetector#getMaxCheckInterval()}
     * @testedMethod {@link GenericCodingEventDetector#getMaxIterationCount()}
     * 
     * @description tests all the overridden methods of {@link EventDetector} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link GenericCodingEventDetector} constructor parameters, and a {@link SpacecraftState}
     * 
     * @output a {@link GenericCodingEventDetector}
     * 
     * @testPassCriteria all the methods return the expected values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testOverriddenMethods() throws PatriusException {
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT");

        // Test eventOccurred method
        Action actual = detector.eventOccurred(state, true, true);
        Assert.assertEquals(Action.CONTINUE, actual);
        actual = detector.eventOccurred(state, false, true);
        Assert.assertEquals(Action.STOP, actual);
        actual = detector.eventOccurred(state, true, false);
        Assert.assertEquals(Action.CONTINUE, actual);
        actual = detector.eventOccurred(state, false, false);
        Assert.assertEquals(Action.STOP, actual);

        // Test g method
        final double expectedG = visi.g(state);
        Assert.assertEquals(expectedG, detector.g(state), 0.);

        // Test resetState method
        final AbsoluteDate date0 = new AbsoluteDate(2000, 1, 15, TimeScalesFactory.getTT());
        final Orbit orbit0 = new KeplerianOrbit(1e07, 0.1, 0.1, 0, 0, FastMath.PI, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date0, Constants.EGM96_EARTH_MU);
        final SpacecraftState state0 = new SpacecraftState(orbit0);
        final SpacecraftState expectedState = visi.resetState(state0);
        Assert.assertEquals(expectedState, detector.resetState(state0));

        // Test getThreshold
        final double expectedThreshold = visi.getThreshold();
        Assert.assertEquals(expectedThreshold, detector.getThreshold(), 0.);

        // Test getMaxCheckInterval
        final double maxCheck = visi.getMaxCheckInterval();
        Assert.assertEquals(maxCheck, detector.getMaxCheckInterval(), 0.);

        // Test getMaxIterationCount
        final double maxIteration = visi.getMaxIterationCount();
        Assert.assertEquals(maxIteration, detector.getMaxIterationCount(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#buildCodedEvent(SpacecraftState, boolean)}
     * 
     * @description tests {@link GenericCodingEventDetector#buildCodedEvent(SpacecraftState, boolean)} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link GenericCodingEventDetector} and {@link CodedEvent} constructor parameters
     * 
     * @output two {@link CodedEvent} (one when g is increasing and one when is decreasing)
     * 
     * @testPassCriteria the method returns the expected CodedEvent when g is increasing and when g
     *                   is decreasing
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testBuildCodedEvent() {
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", true, "Visibility");
        // Expected CodedEvent when increasing
        final CodedEvent expected1 = new CodedEvent("ENTER", "generic event", date, true);
        // Expected CodedEvent when decreasing
        final CodedEvent expected2 = new CodedEvent("EXIT", "generic event", date, false);

        Assert.assertEquals(expected1, detector.buildCodedEvent(state, true));
        Assert.assertEquals(expected2, detector.buildCodedEvent(state, false));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#positiveSignMeansActive()}
     * 
     * @description tests {@link GenericCodingEventDetector#positiveSignMeansActive()} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link GenericCodingEventDetector} constructor parameters
     * 
     * @output the flag increasingIsStart
     * 
     * @testPassCriteria the method returns the expected boolean
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testPositiveSignMeansActive() {
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", false, "Visibility");

        Assert.assertEquals(false, detector.positiveSignMeansActive());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#getPhenomenonCode()}
     * 
     * @description tests {@link GenericCodingEventDetector#getPhenomenonCode()} using the
     *              {@link CircularFieldOfViewDetector}
     * 
     * @input {@link GenericCodingEventDetector} constructor parameters
     * 
     * @output the phenomenon code, null when the {@link GenericCodingEventDetector} does not
     *         support phenomena
     * 
     * @testPassCriteria the method returns the expected code (or null)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetPhenomenonCode() {
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector1 = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", true, "Visibility");
        final GenericCodingEventDetector detector2 = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT");
        Assert.assertEquals("Visibility", detector1.getPhenomenonCode());
        Assert.assertNull(detector2.getPhenomenonCode());
    }

    /**
     * @throws PatriusException from the wrapped event detector.
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_GENERIC_CODING_EVENT_DETECTOR}
     * 
     * @testedMethod {@link GenericCodingEventDetector#isStateActive(SpacecraftState)}
     * 
     * @description tests {@link GenericCodingEventDetector#isStateActive(SpacecraftState)}
     * 
     * @input {@link GenericCodingEventDetector} constructor parameters
     * 
     * @output if the state is active or not
     * 
     * @testPassCriteria the method returns the expected state
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testIsStateActive() throws PatriusException {
        // The CodingEventDetector is created:
        final GenericCodingEventDetector detector1 = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", true, "Visibility");
        Assert.assertEquals(false, detector1.isStateActive(state));
    }

    /**
     * Test the log of the events over the time interval of a propagation.
     * 
     * @throws PatriusException if spacecraft state cannot be propagated
     * 
     * @testedMethod {@link GenericCodingEventDetector#logEventsOverTimeInterval (CodedEventsLogger, Propagator, CodingEventDetector, AbsoluteDateInterval)}
     */
    @Test
    public void testLogEventsOverTimeInterval() throws PatriusException {
        // Coded events logger
        final CodedEventsLogger eventsLogger = new CodedEventsLogger();
        // Propagator
        final FirstOrderIntegrator integ = new RungeKutta6Integrator(5);
        final NumericalPropagator prop = new NumericalPropagator(integ, state.getFrame(), OrbitType.KEPLERIAN,
            PositionAngle.TRUE);
        prop.setInitialState(state);
        prop.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(state.getMu())));
        final AbstractAttitudeLaw law = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        prop.setAttitudeProviderEvents(law);
        prop.setAttitudeProviderForces(law);
        // Coding event detector
        final GenericCodingEventDetector eventDetector = new GenericCodingEventDetector(visi, "ENTER",
            "EXIT", true, "Visibility");
        // Date interval
        final AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(date, Constants.JULIAN_DAY);

        // Check that the event set is empty
        final SortedSet<LoggedCodedEvent> eventSet = eventsLogger.getLoggedCodedEventSet();
        Assert.assertTrue(eventSet.isEmpty());

        // Log events over time interval
        GenericCodingEventDetector.logEventsOverTimeInterval(eventsLogger, prop, eventDetector, dateInterval);

        // Check that the event set is not empty anymore, but it now contains 2 elements
        Assert.assertEquals(2, eventSet.size());

        // Check that the event detector has been cleared
        Assert.assertTrue(prop.getEventsDetectors().isEmpty());
    }
}
