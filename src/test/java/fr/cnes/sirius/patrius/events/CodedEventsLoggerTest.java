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
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.detectors.ApsideDetector;
import fr.cnes.sirius.patrius.events.detectors.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.detectors.EclipseDetector;
import fr.cnes.sirius.patrius.events.detectors.NodeDetector;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEvent;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsList;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.CodingEventDetector;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.events.postprocessing.PhenomenaList;
import fr.cnes.sirius.patrius.events.postprocessing.Phenomenon;
import fr.cnes.sirius.patrius.events.postprocessing.CodedEventsLogger.LoggedCodedEvent;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link CodedEventsLogger}.<br>
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class CodedEventsLoggerTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the coded events logger
         * 
         * @featureDescription Validate the coded events logger
         * 
         * @coveredRequirements DV-TRAJ_190, DV-TRAJ_200, DV-TRAJ_210, DV-EVT_60
         */
        VALIDATE_CODED_EVENTS_LOGGER
    }

    /**
     * String "Undefined Event".
     */
    private static String undefinedEvent;

    /**
     * A node detector.
     */
    private static NodeDetector node;

    /**
     * An eclipse detector.
     */
    private static EclipseDetector eclipse;

    /**
     * A station visibility detector.
     */
    private static CircularFieldOfViewDetector stationVisi;

    /**
     * An apside detector.
     */
    private static ApsideDetector apside;

    /**
     * A date detector.
     */
    private static DateDetector date;

    /**
     * A start date.
     */
    private static AbsoluteDate date0;

    /**
     * An end date.
     */
    private static AbsoluteDate dateF;

    /**
     * A spacecraft state.
     */
    private static SpacecraftState state;

    /**
     * A coding detector for nodes.
     */
    private static GenericCodingEventDetector nodeDet;

    /**
     * A coding detector for eclipses.
     */
    private static GenericCodingEventDetector eclipseDet;

    /**
     * Orbit.
     */
    private static Orbit orbit;

    /**
     * Orbit period.
     */
    private static double period;

    /**
     * A propagator.
     */
    private static NumericalPropagator propagator;

    /**
     * Setup for unit tests in the class. Provides two {@link CodedEvent}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Before
    public void setUp() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");

        undefinedEvent = "UNDEFINED_EVENT";

        // Set up the dates
        date0 = new AbsoluteDate("2000-01-01T12:00:00Z", TimeScalesFactory.getTT());
        period = FastMath.PI * 2 * MathLib.sqrt(MathLib.pow(7e6, 3) / Constants.EIGEN5C_EARTH_MU);

        dateF = date0.shiftedBy(2 * period);
        // date = new AbsoluteDate("2000-01-01T12:00:00Z", TimeScalesFactory.getTT());
        orbit = new KeplerianOrbit(7e6, 0.001, 0.2, 0.3, 0, 0.2, PositionAngle.MEAN, FramesFactory.getGCRF(), date0,
            Constants.EGM96_EARTH_MU);

        // Node detector that does not stop the propagation
        node = new NodeDetector(orbit, FramesFactory.getEME2000(), NodeDetector.ASCENDING_DESCENDING){

            /** Serializable UID. */
            private static final long serialVersionUID = -8525156497993643953L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        // Set up the coding detector:
        nodeDet = new GenericCodingEventDetector(node, "Ascending node", "Descending node", true, "Nodes");

        // Eclipse detector that does not stop the propagation
        final double sunRadius = 696000000.;
        final double earthRadius = 6400000.;
        eclipse = new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius, CelestialBodyFactory.getEarth(),
            earthRadius, 0, 60., 1.e-3){
            /** Serializable UID. */
            private static final long serialVersionUID = -7053949963934620479L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        // Set up the coding detector:
        eclipseDet = new GenericCodingEventDetector(eclipse, "Exit", "Enter", false, "Eclipses");

        final int maxCheck = 300;

        final OneAxisEllipsoid earthBody = new OneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF());

        final EllipsoidPoint point = new EllipsoidPoint(earthBody, earthBody.getLLHCoordinatesSystem(),
            MathLib.toRadians(60), MathLib.toRadians(5), 0, "");

        final PVCoordinatesProvider station = new TopocentricFrame(point, "station");

        // Station visibility detector
        stationVisi = new CircularFieldOfViewDetector(station, Vector3D.PLUS_I, MathLib.toRadians(35), maxCheck){
            private static final long serialVersionUID = -54150076610577203L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        // Apogee - perigee passages detector
        apside = new ApsideDetector(orbit, 2){
            private static final long serialVersionUID = 7149796307062112194L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        // Date detector : after one period
        date = new DateDetector(date0.shiftedBy(period)){
            private static final long serialVersionUID = -1598569861618988777L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.CONTINUE;
            }
        };

        // Set up the spacecraft state:
        state = new SpacecraftState(orbit);

        // Set up the propagator
        final FirstOrderIntegrator rk = new ClassicalRungeKuttaIntegrator(1);
        propagator = new NumericalPropagator(rk);
    }

    /**
     * TearDown for unit tests in the class.
     */
    @After
    public void tearDown() {
        propagator = null;
        orbit = null;
        node = null;
        nodeDet = null;
        eclipse = null;
        eclipseDet = null;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#CodedEventsLogger()}
     * 
     * @description simple constructor test
     * 
     * @input no inputs
     * 
     * @output a {@link CodedEventsLogger}
     * 
     * @testPassCriteria the {@link CodedEventsLogger} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testCodedEventsLogger() {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the constructor did not crash:
        Assert.assertNotNull(logger);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#getCodedEventsList()}
     * 
     * @description tests {@link CodedEventsLogger#getCodedEventsList()}
     * 
     * @input a {@link NumericalPropagator} and a {@link NodeDetector}
     * 
     * @output a {@link CodedEventsList}
     * 
     * @testPassCriteria the {@link CodedEvent} generated during the propagation are the expected {@link CodedEvent}
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetCodedEventsList() throws PatriusException {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final EventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
            .getMu())));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        // Create a list of CodedEvents
        final CodedEventsList list = logger.getCodedEventsList();
        Assert.assertEquals(4, list.getList().size());
        Assert.assertEquals("Descending node", list.getList().get(0).getCode());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#buildCodedEventListMap()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#getDetector()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#getCodedEvent()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#getDate()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#compareTo(LoggedCodedEvent)}
     * @testedMethod {@link CodedEventsLogger#monitorDetector(CodingEventDetector)}
     * @testedMethod {@link CodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link CodedEventsLogger#buildCodedEventListMap()} using a list of LoggedCodedEvents created
     *              during an orbit propagation. The detected events are nodes crossings.
     * 
     * @input a {@link NumericalPropagator} and a {@link NodeDetector}
     * 
     * @output a map of {@link CodedEventsList}
     * 
     * @testPassCriteria the map of {@link CodedEventsList} should contain the expected CodedEvents, that represent the
     *                   ascending and descending node crossing; the number of CodedEvents depends on the number of
     *                   orbits (4 node crossing during two orbital periods)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testBuildCodedEventListMap() throws PatriusException {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final EventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
            .getMu())));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        // Get the map of coded events lists:
        final Map<CodingEventDetector, CodedEventsList> map = logger.buildCodedEventListMap();
        final CodedEventsList nodeList = map.get(nodeDet);

        // There should be 4 events during two orbital periods
        Assert.assertEquals(4, nodeList.getList().size());
        // First event should be a descending node (the initial orbit anomaly is 0.2 rad):
        Assert.assertEquals("Descending node", nodeList.getList().get(0).getCode());
        Assert.assertEquals(false, nodeList.getList().get(0).isStartingEvent());
        // Second event should be an ascending node:
        Assert.assertEquals("Ascending node", nodeList.getList().get(1).getCode());
        Assert.assertEquals(true, nodeList.getList().get(1).isStartingEvent());

        // Check the logged events list is not empty:
        Assert.assertFalse(logger.getLoggedCodedEventSet().isEmpty());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#monitorDetector(CodingEventDetector)}
     * 
     * @description covers {@link CodedEventsLogger.CodingWrapper#resetState(SpacecraftState, boolean)}
     * 
     * @input a {@link NumericalPropagator} and a {@link NodeDetector}
     * 
     * @output {@link LoggedCodedEvent}
     * 
     * @testPassCriteria this test performs a numerical propagation and covers the resetState function; it just checks
     *                   if at least one event has been detected.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testEventOccurred() throws PatriusException {
        // Node detector that does not stop the propagation
        node = new NodeDetector(orbit, FramesFactory.getEME2000(), NodeDetector.ASCENDING_DESCENDING){

            /** Serializable UID. */
            private static final long serialVersionUID = 4003941336786137560L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return Action.RESET_STATE;
            }
        };
        // Set up the coding detector:
        nodeDet = new GenericCodingEventDetector(node, "Ascending node", "Descending node", true, "Nodes");

        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final EventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
            .getMu())));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);
        Assert.assertFalse(logger.getLoggedCodedEventSet().isEmpty());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#getState()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#isIncreasing()}
     * 
     * @description covers {@link CodedEventsLogger.LoggedCodedEvent#getState()} and
     *              {@link CodedEventsLogger.LoggedCodedEvent#isIncreasing()}<br>
     * 
     * @input a {@link NumericalPropagator} and a {@link NodeDetector}
     * 
     * @output {@link LoggedCodedEvent}
     * 
     * @testPassCriteria this test performs a numerical propagation and covers getState and isIncreasing functions; it
     *                   checks that the state and the flag isIncreasing for a descending node event have the expected
     *                   values.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testLoggedCodedEvents() throws PatriusException {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final EventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
            .getMu())));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);
        // Check the getState function on the first event (descending node : anomaly = pi):
        Assert.assertEquals(FastMath.PI, logger.getLoggedCodedEventSet().first().getState().getLM(), 0.001);
        // Check the isIncreasing function on the first event (descending node : decreasing):
        Assert.assertEquals(false, logger.getLoggedCodedEventSet().first().isIncreasing());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#LoggedCodedEvent(CodingEventDetector, CodedEvent, SpacecraftState, boolean)}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#getDetector()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#getCodedEvent()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#getDate()}
     * @testedMethod {@link CodedEventsLogger.LoggedCodedEvent#compareTo(LoggedCodedEvent)}
     * @testedMethod {@link CodedEventsLogger#processEvent(CodingEventDetector, SpacecraftState, boolean)}
     * @testedMethod {@link CodedEventsLogger#monitorDetector(CodingEventDetector)}
     * @testedMethod {@link CodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link CodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)} using a
     *              list of LoggedCodedEvents created during an orbit propagation. The detected events are nodes
     *              crossings and phenomena are time intervals bounded by node crossing events.
     * 
     * @input a {@link NumericalPropagator} and a {@link NodeDetector}
     * 
     * @output a map of {@link PhenomenaList}
     * 
     * @testPassCriteria the map of {@link PhenomenaList} should contain the expected {@link Phenomenon}. In this test
     *                   the phenomenon starts when the satellite crosses the ascending node and ends when the satellite
     *                   crosses descending node: during a two orbits time interval we expect to obtain three phenomena.
     *                   The phenomena should be bounded by node crossing events (ascending node crossing as the
     *                   starting event and descending node crossing as the ending event). The initial phenomenon should
     *                   have an undefined starting event (orbit propagation starts when satellite has already crossed
     *                   the ascending node), and the final event should be bounded by an undefined ending event (orbit
     *                   propagation finishes when the satellite has not crossed yet the descending node).
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testBuildPhenomenaListMap1() throws PatriusException {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final EventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
            .getMu())));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        // Get the map of coded events lists:

        // Set the definition interval != null
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date0, dateF,
            IntervalEndpointType.OPEN);
        final Map<CodingEventDetector, PhenomenaList> map = logger.buildPhenomenaListMap(interval, null);
        final PhenomenaList nodeList = map.get(nodeDet);

        // There should be 3 phenomena during two orbital periods
        Assert.assertEquals(3, nodeList.getList().size());
        // Phenomena should be ascending/descending periods
        // First phenomenon has an undefined starting event
        Assert.assertEquals(undefinedEvent, nodeList.getList().get(0).getStartingEvent().getCode());
        Assert.assertEquals("Descending node", nodeList.getList().get(0).getEndingEvent().getCode());
        Assert.assertEquals("Ascending node", nodeList.getList().get(1).getStartingEvent().getCode());
        Assert.assertEquals("Descending node", nodeList.getList().get(1).getEndingEvent().getCode());
        Assert.assertEquals("Ascending node", nodeList.getList().get(2).getStartingEvent().getCode());
        // Last phenomenon has an undefined ending event
        Assert.assertEquals(undefinedEvent, nodeList.getList().get(2).getEndingEvent().getCode());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     * @testedMethod {@link CodedEventsLogger#monitorDetector(CodingEventDetector)}
     * @testedMethod {@link CodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link CodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)} using a
     *              list of LoggedCodedEvents created during an orbit propagation. In this test no events are detected
     *              during propagation but the state is "active": the logger should be able to create a undefined
     *              phenomenon with default boundaries.
     * 
     * @input a {@link NumericalPropagator}, an {@link Orbit} and a {@link NodeDetector}
     * 
     * @output a map of {@link PhenomenaList}
     * 
     * @testPassCriteria the map of {@link PhenomenaList} should contain the expected {@link Phenomenon}, i.e. a
     *                   phenomenon bounded by undefined events.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testBuildPhenomenaListMap2() throws PatriusException {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final EventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        // Propagate during two orbital periods:
        propagator.propagate(date0, date0.shiftedBy(1000));

        // Set the definition interval == null
        final Map<CodingEventDetector, PhenomenaList> map = logger.buildPhenomenaListMap(null, state);
        final PhenomenaList nodeList = map.get(nodeDet);

        // There should be 1 phenomena during two orbital periods
        Assert.assertEquals(1, nodeList.getList().size());
        // Phenomenon should be bounded by undefined events:
        Assert.assertEquals(undefinedEvent, nodeList.getList().get(0).getStartingEvent().getCode());
        Assert.assertEquals(undefinedEvent, nodeList.getList().get(0).getEndingEvent().getCode());
        // The dates of events should be the default PAST_INFINITY and FUTURE_INFINITY dates:
        Assert.assertEquals(AbsoluteDate.PAST_INFINITY, nodeList.getList().get(0).getStartingEvent().getDate());
        Assert.assertEquals(AbsoluteDate.FUTURE_INFINITY, nodeList.getList().get(0).getEndingEvent().getDate());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     * @testedMethod {@link CodedEventsLogger#monitorDetector(CodingEventDetector)}
     * @testedMethod {@link CodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link CodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)} using a
     *              list of LoggedCodedEvents created during an orbit propagation. In this test no events are detected
     *              during propagation and the state is not "active": the logger should not be able to create a
     *              phenomenon.
     * 
     * @input a {@link NumericalPropagator}, an {@link Orbit} and a {@link EclipseDetector}
     * 
     * @output a map of {@link PhenomenaList}
     * 
     * @testPassCriteria the map of {@link PhenomenaList} should not contain any phenomenon, as the satellite is not in
     *                   eclipse during the propagation.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testBuildPhenomenaListMap3() throws PatriusException {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final EventDetector d = logger.monitorDetector(eclipseDet);

        // Set up the propagator with the eclipse detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        // Propagate during two orbital periods:
        propagator.propagate(date0, date0.shiftedBy(10));

        // Set the definition interval == null
        final Map<CodingEventDetector, PhenomenaList> map = logger.buildPhenomenaListMap(null, state);
        final PhenomenaList eclipseList = map.get(eclipseDet);

        // There should be 0 phenomena during two orbital periods (phenomenon is not active)
        Assert.assertEquals(0, eclipseList.getList().size());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#processEvent(CodingEventDetector, SpacecraftState, boolean)}
     * @testedMethod {@link GenericCodingEventDetector#buildDelayedCodedEvent(SpacecraftState, boolean)}
     * 
     * @description tests the creation of the CodedEvent when a delay is given as a parameter to the
     *              GenericCodingEventDetector constructor.
     * 
     * @input a {@link NumericalPropagator}, an {@link Orbit} and a {@link NodeDetector}
     * 
     * @output a list of {@link CodedEvent} and a map of {@link PhenomenaList}
     * 
     * @testPassCriteria the list of {@link CodedEvent} should contain the standard events and the delayed events; those
     *                   delayed event should have a delayed date with respect to the standard events; the map of
     *                   {@link PhenomenaList} should contain three phenomena (the same of the detection without delay).
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testDelayedEvents() throws PatriusException {
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();
        // Set up a generic node detector:
        final GenericCodingEventDetector genericNodeDet = new GenericCodingEventDetector(node, "ASCENDING NODE",
            "DESCENDING NODE", true, "Nodes");
        // Set up the coding detector with a delay of 10 s:
        final GenericCodingEventDetector delayedNodeDet = new GenericCodingEventDetector(node, "ASCENDING NODE",
            "DESCENDING NODE", true, "Nodes", 0.125, 0);
        final EventDetector g = logger.monitorDetector(genericNodeDet);
        final EventDetector d = logger.monitorDetector(delayedNodeDet);
        // Set up the propagator with the nodes detector:
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.addEventDetector(g);
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        final CodedEventsList list = logger.getCodedEventsList();
        final Set<CodedEvent> listAsc = list.getEvents("ASCENDING NODE", null, null);
        final Set<CodedEvent> listDes = list.getEvents("DESCENDING NODE", null, null);
        final Set<CodedEvent> listAscD = list.getEvents("DELAYED ASCENDING NODE", null, null);
        final Set<CodedEvent> listDesD = list.getEvents("DELAYED DESCENDING NODE", null, null);

        // check the date of the delayed ascending node events is the expected one:
        for (int i = 0; i < listAsc.size(); i++) {
            final AbsoluteDate date1 = ((CodedEvent) listAsc.toArray()[i]).getDate();
            final AbsoluteDate date2 = ((CodedEvent) listAscD.toArray()[i]).getDate();
            Assert.assertEquals(date2, date1.shiftedBy(0.125));
        }
        // check the date of the delayed descending node events is the expected one:
        for (int i = 0; i < listDes.size(); i++) {
            final AbsoluteDate date1 = ((CodedEvent) listDes.toArray()[i]).getDate();
            final AbsoluteDate date2 = ((CodedEvent) listDesD.toArray()[i]).getDate();
            Assert.assertEquals(date2, date1.shiftedBy(0.125));
        }
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#processEvent(CodingEventDetector, SpacecraftState, boolean)}
     * @testedMethod {@link GenericCodingEventDetector#buildOccurrenceCodedEvent(SpacecraftState, boolean)}
     * 
     * @description tests the creation of the CodedEvent when an occurrence number is given as a parameter to the
     *              GenericCodingEventDetector constructor.
     * 
     * @input a {@link NumericalPropagator}, an {@link Orbit} and a {@link NodeDetector}
     * 
     * @output a list of {@link CodedEvent} and a map of {@link PhenomenaList}
     * 
     * @testPassCriteria the list of {@link CodedEvent} should contain the standard events and the third occurrence
     *                   event; this last event should have the same date of the corresponding standard event; the map
     *                   of {@link PhenomenaList} should contain three phenomena (the same of the detection without
     *                   occurrence numbers).
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testOccurrenceEvents() throws PatriusException {
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(state.getMu())));
        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();

        // Set up the coding detector looking for the third occurrence
        final GenericCodingEventDetector genericNodeDet = new GenericCodingEventDetector(node, "ASCENDING NODE",
            "DESCENDING NODE");
        final GenericCodingEventDetector occurrenceNodeDet = new GenericCodingEventDetector(node, "ASCENDING NODE",
            "DESCENDING NODE", true, "Nodes", 0, 3);
        final EventDetector generic = logger.monitorDetector(genericNodeDet);
        final EventDetector occurrence = logger.monitorDetector(occurrenceNodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(generic);
        propagator.addEventDetector(occurrence);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        // Check the logged events list is not empty:
        Assert.assertTrue(!logger.getLoggedCodedEventSet().isEmpty());
        final CodedEventsList list = logger.getCodedEventsList();
        final Set<CodedEvent> listDes = list.getEvents("DESCENDING NODE", null, null);
        final Set<CodedEvent> listAsc = list.getEvents("ASCENDING NODE", null, null);
        final Set<CodedEvent> listDesN3 = list.getEvents("DESCENDING NODE N.3", null, null);
        final Set<CodedEvent> listAscN3 = list.getEvents("ASCENDING NODE N.3", null, null);

        Assert.assertEquals(1, listDesN3.size());
        Assert.assertEquals("DESCENDING NODE N.3", ((CodedEvent) listDesN3.toArray()[0]).getCode());
        Assert.assertEquals(0, listAscN3.size());
        Assert.assertEquals(listDes.size() + listAsc.size() + 1, list.getList().size());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link CodedEventsLogger#processEvent(CodingEventDetector, SpacecraftState, boolean)};
     * 
     * @description tests the creation of several CodedEvent when occurrence number and delay are given, only the delay
     *              is given, neither the delay nor the occurrence are given as parameters of the
     *              GenericCodingEventDetector constructor.
     * 
     * @input a {@link NumericalPropagator}, an {@link Orbit}, a {@link NodeDetector}, an {@link EclipseDetector}, an
     *        {@link ApsideDetector}, a {@link DateDetector} and a {@link CircularFieldOfViewDetector}.
     * 
     * @output a list of {@link CodedEvent}
     * 
     * @testPassCriteria the list of {@link CodedEvent} should contain the standard events, the third delayed occurrence
     *                   event of the eclipse and the nodes passage and the delayed date passage; this list should
     *                   contain 3 events more than the list which ignores the delays and the occurrences.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testOccurrenceEventsAndDelayedEvents() throws PatriusException {
        // Creation of the EventsLogger
        final EventsLogger log = new EventsLogger();

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator.addEventDetector(log.monitorDetector(stationVisi));
        propagator.addEventDetector(log.monitorDetector(eclipse));
        propagator.addEventDetector(log.monitorDetector(apside));
        propagator.addEventDetector(log.monitorDetector(date));

        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialState()
            .getMu())));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        propagator.clearEventsDetectors();

        log.getLoggedEvents().size();

        // The CodedEventsLogger is created:
        final CodedEventsLogger logger = new CodedEventsLogger();

        final GenericCodingEventDetector genericNodeDet = new GenericCodingEventDetector(node,
            "Generic Ascending node",
            "Generic Descending node");
        final EventDetector gNode = logger.monitorDetector(genericNodeDet);

        final GenericCodingEventDetector delayedNodeDet = new GenericCodingEventDetector(node, "Ascending node",
            "Descending node", 10., 3);
        final EventDetector dNode = logger.monitorDetector(delayedNodeDet);

        propagator.addEventDetector(gNode);
        propagator.addEventDetector(dNode);

        propagator.resetInitialState(new SpacecraftState(orbit));
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        // Check the logged events list is not empty:
        Assert.assertTrue(!logger.getLoggedCodedEventSet().isEmpty());

        final CodedEventsList list = logger.getCodedEventsList();
        final Set<CodedEvent> listAsc = list.getEvents("Generic Ascending node", null, null);
        final Set<CodedEvent> listDes = list.getEvents("Generic Descending node", null, null);
        final Set<CodedEvent> listAscN3 = list.getEvents("DELAYED Ascending node N.3", null, null);
        final Set<CodedEvent> listDesN3 = list.getEvents("DELAYED Descending node N.3", null, null);
        Assert.assertEquals(2, listAsc.size());
        Assert.assertEquals(2, listDes.size());
        Assert.assertEquals(1, listAscN3.size() + listDesN3.size());
        // check the date of the delayed event is the expected one:
        Assert.assertEquals(((CodedEvent) listDes.toArray()[1]).getDate().shiftedBy(10.),
            ((CodedEvent) listDesN3.toArray()[0]).getDate());
    }
}
