package fr.cnes.sirius.patrius.events.utils;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.MultiEventDetector;
import fr.cnes.sirius.patrius.events.detectors.LocalTimeAngleDetector;
import fr.cnes.sirius.patrius.events.detectors.LongitudeDetector;
import fr.cnes.sirius.patrius.events.postprocessing.MultiCodedEventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.MultiGenericCodingEventDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * HISTORY
 * VERSION:4.13.3:FA:FA-283:27/03/2024:[PATRIUS] Methode filterEvent() non-wrappe dans OneSatEventDetectorWrapper
 * END-HISTORY
 */
 
public class OneSatEventDetectorWrapperTest {

    /** String "satID" */
    private static final String SAT_ID = "satID";

    /** A start date */
    private static AbsoluteDate date0;

    /** An end date */
    private static AbsoluteDate dateF;

    /** A spacecraft state */
    private static SpacecraftState state;

    /** Orbit */
    private static Orbit orbit;

    /** Orbit period */
    private static double period;

    /** A propagator */
    private static MultiNumericalPropagator propagator;

    /**
     * Setup for unit tests in the class.
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Before
    public void setUp() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");
        // Set up the dates
        date0 = new AbsoluteDate("2000-01-01T12:00:00Z", TimeScalesFactory.getTT());
        final double sma = 7e6;
        orbit = new KeplerianOrbit(sma, 0.001, 0.2, 0.3, 0, 0.2, PositionAngle.MEAN, FramesFactory.getGCRF(), date0,
            Constants.EGM96_EARTH_MU);
        period = FastMath.PI * 2 * MathLib.sqrt(MathLib.pow(sma, 3) / Constants.EIGEN5C_EARTH_MU);
        dateF = date0.shiftedBy(period);
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
    public void tearDown() {
        date0 = null;
        dateF = null;
        propagator = null;
        orbit = null;
    }

    /**
     * Tests the use of the override method {@link OneSatEventDetectorWrapper#filterEvent(Map, boolean, boolean)} for
     * the specific {@link LongitudeDetector}.
     * <p>
     * Test shows that the event is filtered (true is returned).
     * 
     * @throws PatriusException if problems when adding initial state, propagating or filtering the event
     */
    @Test
    public void testFilterEventLongDetector() throws PatriusException {

        // Create a longitude detector which stops the propagation when the target longitude is reached by the
        // spacecraft
        final double targetLongitude = MathLib.toRadians(30.);
        final LongitudeDetector longDetector = new LongitudeDetector(targetLongitude, orbit.getFrame());
        final OneSatEventDetectorWrapper oneSatEventDetWrap = new OneSatEventDetectorWrapper(longDetector, SAT_ID);
        // Set up the coding detector:
        final MultiGenericCodingEventDetector multiCodEventDet =
            new MultiGenericCodingEventDetector(oneSatEventDetWrap, "Increasing longitude", "Decreasing longitude",
                true, "Longitude");
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(multiCodEventDet);
        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(state, SAT_ID);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get(SAT_ID).getMu())), SAT_ID);
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        final Map<String, SpacecraftState> map = new HashMap<String, SpacecraftState>();
        map.put(SAT_ID, state);

        // Verify that the event is filtered
        Assert.assertTrue(oneSatEventDetWrap.filterEvent(map, false, true));

    }

    /**
     * Tests the use of the override method {@link OneSatEventDetectorWrapper#filterEvent(Map, boolean, boolean)} for
     * the specific {@link LocalTimeAngleDetector}.
     * <p>
     * Test shows that the event is filtered (true is returned).
     * 
     * @throws PatriusException if problems when creating the detector, adding initial state, propagating or filtering
     *         the event
     */
    @Test
    public void testFilterEventLocalTimeDetector() throws PatriusException {

        final LocalTimeAngleDetector locTimeAngleDetector = new LocalTimeAngleDetector(0.);
        final OneSatEventDetectorWrapper oneSatEventDetWrap =
            new OneSatEventDetectorWrapper(locTimeAngleDetector, SAT_ID);
        // Set up the coding detector:
        final MultiGenericCodingEventDetector multiCodEventDet =
            new MultiGenericCodingEventDetector(oneSatEventDetWrap, "Increasing longitude", "Decreasing longitude",
                true, "Longitude");
        final MultiCodedEventsLogger logger = new MultiCodedEventsLogger();
        // Check the logged events list is empty:
        Assert.assertTrue(logger.getLoggedCodedEventSet().isEmpty());
        final MultiEventDetector d = logger.monitorDetector(multiCodEventDet);

        // Set up the propagator with the node detector:
        propagator.addEventDetector(d);
        propagator.setEphemerisMode();
        propagator.addInitialState(state, SAT_ID);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(propagator.getInitialStates()
            .get(SAT_ID).getMu())), SAT_ID);
        // Propagate during two orbital periods:
        propagator.propagate(date0, dateF);

        final Map<String, SpacecraftState> map = new HashMap<String, SpacecraftState>();
        map.put(SAT_ID, state);

        // Verify that the event is filtered
        Assert.assertTrue(oneSatEventDetWrap.filterEvent(map, false, true));

    }

    /**
     * Test designed to verify that the g function launched by the wrapper is corresponding to the associated one of the
     * detector.
     * 
     * @throws PatriusException if problems when calling the g() functions for detectors
     */
    @Test
    public void testDetGWrapper() throws PatriusException {
        // Create a longitude detector which stops the propagation when the target longitude is reached by the
        // spacecraft
        final double targetLongitude = MathLib.toRadians(30.);
        final LongitudeDetector longDetector = new LongitudeDetector(targetLongitude, orbit.getFrame());
        final OneSatEventDetectorWrapper oneSatEventDetWrap = new OneSatEventDetectorWrapper(longDetector, SAT_ID);
        Assert.assertEquals(longDetector.g(state), oneSatEventDetWrap.g(state), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test verifying the getter for the satellite ID given at the construction of the
     * {@link OneSatEventDetectorWrapper} object.
     * 
     * @throws PatriusException if problems when creating the local time detector
     */
    @Test
    public void testGetter() throws PatriusException {
        final LocalTimeAngleDetector locTimeAngleDetector = new LocalTimeAngleDetector(0.);
        final OneSatEventDetectorWrapper oneSatEventDetWrap =
            new OneSatEventDetectorWrapper(locTimeAngleDetector, SAT_ID);
        final String satIdExp = SAT_ID;
        Assert.assertEquals("Satellite ID is not equal for OneSatEventDetectorWrapper", oneSatEventDetWrap.getID(),
            satIdExp);
    }

    /**
     * Tests the method {@link OneSatEventDetectorWrapper#resetStates(Map)} by providing two different spacecraft states
     * with associated satellite IDs.
     * 
     * @throws PatriusException if problems when calling the reset states method
     */
    @Test
    public void testResetStates() throws PatriusException {

        // Initialise the local time detector and the associated wrapper
        final LocalTimeAngleDetector locTimeAngleDetector = new LocalTimeAngleDetector(0.);
        final OneSatEventDetectorWrapper oneSatEventDetWrap =
            new OneSatEventDetectorWrapper(locTimeAngleDetector, SAT_ID);

        // Define two IDs and associated spacecraft states
        final String satId1 = "satId1";
        final String satId2 = "satId2";
        final SpacecraftState state1 = state.shiftedBy(60.);
        final SpacecraftState state2 = state1.shiftedBy(60.);

        // Add IDs and states to the Map
        final Map<String, SpacecraftState> states = new HashMap<String, SpacecraftState>();
        states.put(satId1, state1);
        states.put(satId2, state2);

        // Call the reset states method and verify that returned map contains IDs and spacecraft states
        final Map<String, SpacecraftState> resetStates = oneSatEventDetWrap.resetStates(states);
        Assert.assertTrue(resetStates.containsKey(satId1));
        Assert.assertTrue(resetStates.containsKey(satId2));
        Assert.assertTrue(resetStates.containsValue(state1));
        Assert.assertTrue(resetStates.containsValue(state2));

    }

}
