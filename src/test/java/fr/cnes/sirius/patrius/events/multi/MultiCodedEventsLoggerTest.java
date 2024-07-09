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
 * @history created 18/03/2015
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.multi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsList;
import fr.cnes.sirius.patrius.events.CodingEventDetector;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.events.PhenomenaList;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.events.multi.MultiCodedEventsLogger.MultiLoggedCodedEvent;
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
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.OneSatEventDetectorWrapper;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class {@link MultiCodedEventsLogger}
 * </p>
 * <p>
 * This test class is copied from {@link fr.cnes.fr.cnes.sirius.patrius.propagation.events.EventsLoggerTest}
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class MultiCodedEventsLoggerTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the multi coded events logger
         * 
         * @featureDescription Validate the multi coded events logger
         * 
         */
        VALIDATE_MULTI_CODED_EVENTS_LOGGER
    }

    /**
     * String "State1".
     */
    private static final String STATE1 = "state1";

    /**
     * String "Undefined Event".
     */
    private static String undefinedEvent;

    /**
     * A node detector.
     */
    private static MultiEventDetector node;

    /**
     * An eclipse detector.
     */
    private static MultiEventDetector eclipse;

    /**
     * A station visibility detector.
     */
    private static MultiEventDetector stationVisi;

    /**
     * An apside detector.
     */
    private static MultiEventDetector apside;

    /**
     * A date detector.
     */
    private static MultiEventDetector date;

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
    private static MultiGenericCodingEventDetector nodeDet;

    /**
     * A coding detector for eclipses.
     */
    private static MultiGenericCodingEventDetector eclipseDet;

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
    private static MultiNumericalPropagator propagator;

    /**
     * Setup for unit tests in the class. Provides two {@link CodedEvent}.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Before
    public final void setUp() throws PatriusException {
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
        final NodeDetector nodeDetector = new NodeDetector(orbit, FramesFactory.getEME2000(),
            NodeDetector.ASCENDING_DESCENDING){

            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        node = new OneSatEventDetectorWrapper(nodeDetector, STATE1);

        // Set up the coding detector:
        nodeDet = new MultiGenericCodingEventDetector(node, "Ascending node", "Descending node", true, "Nodes");

        // Eclipse detector that does not stop the propagation
        final double sunRadius = 696000000.;
        final double earthRadius = 6400000.;
        final EclipseDetector eclipseDetector = new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
            CelestialBodyFactory.getEarth(), earthRadius, 0,
            60., 1.e-3){
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        eclipse = new OneSatEventDetectorWrapper(eclipseDetector, STATE1);
        // Set up the coding detector:
        eclipseDet = new MultiGenericCodingEventDetector(eclipse, "Exit", "Enter", false, "Eclipses");

        final int maxCheck = 300;

        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(60), MathLib.toRadians(5), 0);

        final OneAxisEllipsoid earthBody = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF());

        final PVCoordinatesProvider station = new TopocentricFrame(earthBody, point, "station");

        // Station visibility detector
        final CircularFieldOfViewDetector stationVisiDetector = new CircularFieldOfViewDetector(station,
            Vector3D.PLUS_I, MathLib.toRadians(35), maxCheck){
            private static final long serialVersionUID = -54150076610577203L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        stationVisi = new OneSatEventDetectorWrapper(stationVisiDetector, STATE1);

        // Apogee - perigee passages detector
        final ApsideDetector apsideDetector = new ApsideDetector(orbit, 2){
            private static final long serialVersionUID = 7149796307062112194L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        apside = new OneSatEventDetectorWrapper(apsideDetector, STATE1);

        // Date detector : after one period
        final DateDetector dateDetector = new DateDetector(date0.shiftedBy(period)){
            private static final long serialVersionUID = -1598569861618988777L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        date = new OneSatEventDetectorWrapper(dateDetector, STATE1);

        // Set up the spacecraft state:
        state = new SpacecraftState(orbit);

        // Set up the propagator
        final FirstOrderIntegrator rk = new ClassicalRungeKuttaIntegrator(1);
        propagator = new MultiNumericalPropagator(rk);
    }

    /**
     * TearDown for unit tests in the class.
     */
    @After
    public final void tearDown() {
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
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#MultiCodedEventsLogger()}
     * 
     * @description simple constructor test
     * 
     * @input no inputs
     * 
     * @output a {@link MultiCodedEventsLogger}
     * 
     * @testPassCriteria the {@link MultiCodedEventsLogger} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testMultiCodedEventsLogger() {
        // The CodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the constructor did not crash:
        Assert.assertNotNull(logger);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#getCodedEventsList()}
     * 
     * @description tests {@link MultiCodedEventsLogger#getCodedEventsList()}
     * 
     * @input a {@link MultiNumericalPropagator} and a {@link NodeDetector}
     * 
     * @output a {@link CodedEventsList}
     * 
     * @testPassCriteria the {@link CodedEvent} generated during the propagation are the expected {@link CodedEvent}
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testGetCodedEventsList() throws PatriusException {
        // The CodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
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
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#buildCodedEventListMap()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getMultiDetector()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getCodedEvent()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getDate()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#compareTo(MultiLoggedCodedEvent)}
     * @testedMethod {@link MultiCodedEventsLogger#monitorDetector(CodingEventDetector)}
     * @testedMethod {@link MultiCodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link MultiCodedEventsLogger#buildCodedEventListMap()} using a list of MultiLoggedCodedEvents
     *              created
     *              during an orbit propagation. The detected events are nodes crossings.
     * 
     * @input a {@link MultiNumericalPropagator} and a {@link NodeDetector}
     * 
     * @output a map of {@link CodedEventsList}
     * 
     * @testPassCriteria the map of {@link CodedEventsList} should contain the expected CodedEvents, that represent the
     *                   ascending and descending node crossing; the number of CodedEvents depends on the number of
     *                   orbits (4 node crossing during two orbital periods)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testBuildCodedEventListMap() throws PatriusException {
        // The CodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        // Get the map of coded events lists:
        final Map<MultiCodingEventDetector, CodedEventsList> map = logger.buildCodedEventListMap();
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
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#monitorDetector(MultiCodingEventDetector)}
     * 
     * @description covers {@link MultiCodedEventsLogger.CodingWrapper#resetState(SpacecraftState, boolean)}
     * 
     * @input a {@link NumericalPropagator} and a {@link NodeDetector}
     * 
     * @output {@link MultiLoggedCodedEvent}
     * 
     * @testPassCriteria this test performs a numerical propagation and covers the resetState function; it just checks
     *                   if at least one event has been detected.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testEventOccurred() throws PatriusException {
        // Node detector that does not stop the propagation
        final NodeDetector nodeDetector = new NodeDetector(orbit, FramesFactory.getEME2000(),
            NodeDetector.ASCENDING_DESCENDING){

            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.RESET_STATE;
            }
        };
        node = new OneSatEventDetectorWrapper(nodeDetector, STATE1);
        // Set up the coding detector:
        nodeDet = new MultiGenericCodingEventDetector(node, "Ascending node", "Descending node", true, "Nodes");

        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);
        Assert.assertFalse(logger.getLoggedCodedEventSet().isEmpty());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getState()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#isIncreasing()}
     * 
     * @description covers {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getState()} and
     *              {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#isIncreasing()}<br>
     * 
     * @input a {@link NumericalPropagator} and a {@link NodeDetector}
     * 
     * @output {@link MultiLoggedCodedEvent}
     * 
     * @testPassCriteria this test performs a numerical propagation and covers getState and isIncreasing functions; it
     *                   checks that the state and the flag isIncreasing for a descending node event have the expected
     *                   values.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testLoggedCodedEvents() throws PatriusException {
        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);
        // Check the getState function on the first event (descending node : anomaly = pi):
        Assert
            .assertEquals(FastMath.PI, logger.getLoggedCodedEventSet().first().getStates().get(STATE1).getLM(), 0.001);
        // Check the isIncreasing function on the first event (descending node : decreasing):
        Assert.assertEquals(false, logger.getLoggedCodedEventSet().first().isIncreasing());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#MultiLoggedCodedEvent(MultiCodingEventDetector, CodedEvent, SpacecraftState, boolean)}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getMultiDetector()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getCodedEvent()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#getDate()}
     * @testedMethod {@link MultiCodedEventsLogger.MultiLoggedCodedEvent#compareTo(MultiLoggedCodedEvent)}
     * @testedMethod {@link MultiCodedEventsLogger#processEvent(MultiCodingEventDetector, SpacecraftState, boolean)}
     * @testedMethod {@link MultiCodedEventsLogger#monitorDetector(MultiCodingEventDetector)}
     * @testedMethod {@link MultiCodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link MultiCodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     *              using a
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
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testBuildPhenomenaListMap1() throws PatriusException {
        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        // Get the map of coded events lists:

        // Set the definition interval != null
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.OPEN, date0, dateF,
            IntervalEndpointType.OPEN);
        final Map<MultiCodingEventDetector, PhenomenaList> map = logger.buildPhenomenaListMap(interval, null);
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
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     * @testedMethod {@link MultiCodedEventsLogger#monitorDetector(MultiCodingEventDetector)}
     * @testedMethod {@link MultiCodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link MultiCodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     *              using a
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
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testBuildPhenomenaListMap2() throws PatriusException {
        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(nodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
        // Propagate during two orbital periods:
        propagator.propagate(date0, date0.shiftedBy(1000));

        // Set the definition interval == null
        final Map<String, SpacecraftState> states = new HashMap<String, SpacecraftState>();
        states.put(STATE1, state);
        final Map<MultiCodingEventDetector, PhenomenaList> map = logger.buildPhenomenaListMap(null, states);
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
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     * @testedMethod {@link MultiCodedEventsLogger#monitorDetector(MultiCodingEventDetector)}
     * @testedMethod {@link MultiCodedEventsLogger#getLoggedCodedEventSet()}
     * 
     * @description tests {@link MultiCodedEventsLogger#buildPhenomenaListMap(AbsoluteDateInterval, SpacecraftState)}
     *              using a
     *              list of MultiLoggedCodedEvents created during an orbit propagation. In this test no events are
     *              detected
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
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testBuildPhenomenaListMap3() throws PatriusException {
        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(eclipseDet);

        // Set up the propagator with the eclipse detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
        // Propagate during two orbital periods:
        propagator.propagate(date0, date0.shiftedBy(10));

        // Set the definition interval == null
        final Map<String, SpacecraftState> states = new HashMap<String, SpacecraftState>();
        states.put(STATE1, state);
        final Map<MultiCodingEventDetector, PhenomenaList> map = logger.buildPhenomenaListMap(null, states);
        final PhenomenaList eclipseList = map.get(eclipseDet);

        // There should be 0 phenomena during two orbital periods (phenomenon is not active)
        Assert.assertEquals(0, eclipseList.getList().size());
    }

    /**
     * @throws PatriusException
     *         propagation exceptions
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#processEvent(MultiCodingEventDetector, SpacecraftState, boolean)}
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
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testDelayedEvents() throws PatriusException {
        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Set up a generic node detector:
        final MultiGenericCodingEventDetector genericNodeDet = new MultiGenericCodingEventDetector(node,
            "ASCENDING NODE",
            "DESCENDING NODE", true, "Nodes");
        // Set up the coding detector with a delay of 10 s:
        final MultiGenericCodingEventDetector delayedNodeDet = new MultiGenericCodingEventDetector(node,
            "ASCENDING NODE",
            "DESCENDING NODE", true, "Nodes", 0.125, 0);
        final MultiEventDetector g = logger.monitorDetector(genericNodeDet);
        final MultiEventDetector d = logger.monitorDetector(delayedNodeDet);
        // Set up the propagator with the nodes detector:
        propagator.addEventDetector(g);
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
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
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#processEvent(MultiCodingEventDetector, SpacecraftState, boolean)}
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
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testOccurrenceEvents() throws PatriusException {
        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();

        // Set up the coding detector looking for the third occurrence
        final MultiGenericCodingEventDetector genericNodeDet = new MultiGenericCodingEventDetector(node,
            "ASCENDING NODE",
            "DESCENDING NODE");
        final MultiGenericCodingEventDetector occurrenceNodeDet = new MultiGenericCodingEventDetector(node,
            "ASCENDING NODE",
            "DESCENDING NODE", true, "Nodes", 0, 3);
        final MultiEventDetector generic = logger.monitorDetector(genericNodeDet);
        final MultiEventDetector occurrence = logger.monitorDetector(occurrenceNodeDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(generic);
        propagator.addEventDetector(occurrence);
        propagator.setEphemerisMode();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);
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
     * @testedFeature {@link features#VALIDATE_MULTI_CODED_EVENTS_LOGGER}
     * 
     * @testedMethod {@link MultiCodedEventsLogger#processEvent(MultiCodingEventDetector, SpacecraftState, boolean)};
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
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testOccurrenceEventsAndDelayedEvents() throws PatriusException {
        // Creation of the EventsLogger
        final MultiEventsLogger log = new MultiEventsLogger();
        propagator.addInitialState(new SpacecraftState(orbit), STATE1);

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY), STATE1);
        propagator.addEventDetector(log.monitorDetector(stationVisi));
        propagator.addEventDetector(log.monitorDetector(eclipse));
        propagator.addEventDetector(log.monitorDetector(apside));
        propagator.addEventDetector(log.monitorDetector(date));

        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        propagator.clearEventsDetectors();

        log.getLoggedEvents().size();

        // The MultiCodedEventsLogger is created:
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();

        final MultiGenericCodingEventDetector genericNodeDet = new MultiGenericCodingEventDetector(node,
            "Generic Ascending node",
            "Generic Descending node");
        final MultiEventDetector gNode = logger.monitorDetector(genericNodeDet);

        final MultiGenericCodingEventDetector delayedNodeDet = new MultiGenericCodingEventDetector(node,
            "Ascending node",
            "Descending node", 10., 3);
        final MultiEventDetector dNode = logger.monitorDetector(delayedNodeDet);

        propagator.addEventDetector(gNode);
        propagator.addEventDetector(dNode);

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
