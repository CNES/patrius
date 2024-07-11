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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.util.Iterator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Unit tests for {@link CombinedPhenomenaDetector}.<br>
 * </p>
 * 
 * @author Julie Anton
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class CombinedPhenomenaDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the combined phenomena detector
         * 
         * @featureDescription Validate the combined phenomena detector
         * 
         * @coveredRequirements DV-EVT_100
         */
        VALIDATE_COMBINED_PHENOMENA_DETECTOR
    }

    /**
     * An orbit.
     */
    private static Orbit orbit;

    /**
     * A propagator.
     */
    private static NumericalPropagator propagator;

    /**
     * The propagation interval.
     */
    private static AbsoluteDateInterval interval;

    /**
     * A total eclipse detector.
     */
    private static EventDetector umbra;

    /**
     * A partial eclipse detector.
     */
    private static EventDetector penumbra;

    /**
     * A logger for eclipse events.
     */
    private static EventsLogger logger;;

    /**
     * @throws PatriusException when using the propagator
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_COMBINED_PHENOMENA_DETECTOR}
     * 
     * @testedMethod {@link CombinedPhenomenaDetector#CombinedPhenomenaDetector(EventDetector, boolean, EventDetector, boolean, boolean)}
     * @testedMethod {@link CombinedPhenomenaDetector#g(SpacecraftState)}
     * @testedMethod {@link CombinedPhenomenaDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests the combination of two phenomena (satellite in total eclipse and satellite
     *              in penumbra) when detecting events during propagation; the boolean operator used
     *              for the phenomena combination is AND, therefore the test checks if the combined
     *              events detector triggers an event when satellite enters umbra (umbra AND
     *              penumbra = umbra).
     * 
     * @input two {@link EclipseDetector}, two {@link EventsLogger}
     * 
     * @output a list of {@link LoggedEvent} generated during propagation
     * 
     * @testPassCriteria the {@link LoggedEvent} are successfully created and they coincide with the {@link LoggedEvent}
     *                   for umbra.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testANDCombination() throws PatriusException {

        // Create the combined event detector using the AND operator
        final CombinedPhenomenaDetector detectorAND = new CombinedPhenomenaDetector(umbra, false,
            penumbra, false, true);

        // New logger for the events combination
        final EventsLogger loggerAND = new EventsLogger();

        // Propagate the same orbit with the combined events detector:
        propagator.addEventDetector(loggerAND.monitorDetector(detectorAND));
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.propagate(interval.getLowerData(), interval.getUpperData());

        // Check if the combined events and the umbra events coincide (satellite should be in umbra
        // AND in penumbra for a combined event to be triggered: that means satellite should be in
        // umbra)
        final Iterator<LoggedEvent> events = logger.getLoggedEvents().iterator();
        final Iterator<LoggedEvent> eventsAND = loggerAND.getLoggedEvents().iterator();
        while (events.hasNext()) {
            final LoggedEvent event = events.next();
            if (((EclipseDetector) event.getEventDetector()).isTotalEclipse()) {
                // If the logged event corresponds to a total eclipse event, check if its date is
                // the same
                // date of the combined event.
                final SpacecraftState expectedState = event.getState();
                final LoggedEvent eventAND = eventsAND.next();
                final SpacecraftState actualState = eventAND.getState();
                Assert.assertEquals(expectedState.getDate(), actualState.getDate());
                // If the logged event corresponds to a total eclipse event, check if its
                // increasing/decreasing
                // status is the same status of the combines event.
                Assert.assertEquals(!event.isIncreasing(), eventAND.isIncreasing());
            }
        }
    }

    /**
     * @throws PatriusException when using the propagator
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_COMBINED_PHENOMENA_DETECTOR}
     * 
     * @testedMethod {@link CombinedPhenomenaDetector#CombinedPhenomenaDetector(EventDetector, boolean, EventDetector, boolean, boolean)}
     * @testedMethod {@link CombinedPhenomenaDetector#g(SpacecraftState)}
     * @testedMethod {@link CombinedPhenomenaDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests the combination of two phenomena (satellite in total eclipse and satellite
     *              in penumbra) when detecting events during propagation; the boolean operator used
     *              for the phenomena combination is OR, therefore the test checks if the combined
     *              events detector triggers an event when satellite enters penumbra (umbra OR
     *              penumbra = penumbra).
     * 
     * @input two {@link EclipseDetector}, two {@link EventsLogger}
     * 
     * @output a list of {@link LoggedEvent} generated during propagation
     * 
     * @testPassCriteria the {@link LoggedEvent} are successfully created and they coincide with the {@link LoggedEvent}
     *                   for penumbra.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testORCombination() throws PatriusException {

        // Create the combined event detector using the OR operator
        final CombinedPhenomenaDetector detectorOR = new CombinedPhenomenaDetector(umbra, false,
            penumbra, false, false);

        // New logger for the events combination
        final EventsLogger loggerOR = new EventsLogger();

        // Propagate the same orbit with the combined events detector:
        propagator.addEventDetector(loggerOR.monitorDetector(detectorOR));
        propagator.resetInitialState(new SpacecraftState(orbit));
        propagator.propagate(interval.getLowerData(), interval.getUpperData());

        // Check if the combined events and the penumbra events coincide (satellite should be in
        // umbra
        // OR in penumbra for a combined event to be triggered: that means satellite should be in
        // penumbra)
        final Iterator<LoggedEvent> events = logger.getLoggedEvents().iterator();
        final Iterator<LoggedEvent> eventsOR = loggerOR.getLoggedEvents().iterator();
        while (events.hasNext()) {
            final LoggedEvent event = events.next();
            if (!((EclipseDetector) event.getEventDetector()).isTotalEclipse()) {
                // If the logged event corresponds to a partial eclipse event, check if its date is
                // the same
                // date of the combined event.
                final SpacecraftState expectedState = event.getState();
                final LoggedEvent eventOR = eventsOR.next();
                final SpacecraftState actualState = eventOR.getState();
                Assert.assertEquals(expectedState.getDate(), actualState.getDate());
                // If the logged event corresponds to a partial eclipse event, check if its
                // increasing/decreasing
                // status is the same status of the combines event.
                Assert.assertEquals(!event.isIncreasing(), eventOR.isIncreasing());
            }
        }
    }

    /**
     * @throws PatriusException when using the propagator
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_COMBINED_PHENOMENA_DETECTOR}
     * 
     * @testedMethod {@link CombinedPhenomenaDetector#CombinedPhenomenaDetector(EventDetector, boolean, EventDetector, boolean, boolean)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output a {@link CombinedPhenomenaDetector}
     * 
     * @testPassCriteria the {@link CombinedPhenomenaDetector} is successfully created
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testConstructor() throws PatriusException {

        // Create the combined event detector using the OR operator
        final CombinedPhenomenaDetector detector = new CombinedPhenomenaDetector(umbra, false,
            penumbra, false, false, Action.CONTINUE);
        // test getter
        Assert.assertEquals(umbra, detector.getDetector1());
        Assert.assertEquals(penumbra, detector.getDetector2());

        // Copy
        final CombinedPhenomenaDetector detectorCopy = (CombinedPhenomenaDetector) detector.copy();
        Assert.assertEquals(detector.getMaxCheckInterval(), detectorCopy.getMaxCheckInterval(), 0);
    }

    /**
     * Setup for all the tests in the class. Provides two {@link EclipseDetector}, a {@link NumericalPropagator} and an
     * {@link Orbit}.
     * 
     * @throws PatriusException should not happen here
     */
    @Before
    public void setUp() throws PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");
        final AbsoluteDate date = new AbsoluteDate(2000, 1, 10, TimeScalesFactory.getTT());

        // Set up the orbit
        final double re = 6378136;
        final double a = re + 1000000;
        orbit = new KeplerianOrbit(a, 0, 0.2, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getGCRF(),
            date, Constants.EGM96_EARTH_MU);

        // Set up the propagator
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setEphemerisMode();
        propagator.resetInitialState(new SpacecraftState(orbit));

        // Create a total eclipse detector that does not stop the propagation
        final JPLCelestialBodyLoader loaderSun = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SUN);
        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER,
            loaderSSB);

        final CelestialBody sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);
        final JPLCelestialBodyLoader loaderEarth = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH);
        final CelestialBody earth = loaderEarth.loadCelestialBody(CelestialBodyFactory.EARTH);

        umbra = new EclipseDetector(sun, Constants.SUN_RADIUS, earth,
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 0, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD){
            private static final long serialVersionUID = -5744694215413136122L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                return Action.CONTINUE;
            }
        };

        // Create a partial eclipse detector that does not stop the propagation
        penumbra = new EclipseDetector(sun, Constants.SUN_RADIUS, earth,
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 1, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD){
            private static final long serialVersionUID = -5744694215413136122L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                return Action.CONTINUE;
            }
        };

        // Orbital period
        final double t = MathUtils.TWO_PI
            * MathLib.sqrt(MathLib.pow(orbit.getA(), 3) / Constants.EGM96_EARTH_MU);

        // Set the propagation interval
        interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date,
            date.shiftedBy(2 * t), IntervalEndpointType.CLOSED);

        logger = new EventsLogger();
        // Propagate using penumbra and umbra detectors
        propagator.addEventDetector(logger.monitorDetector(umbra));
        propagator.addEventDetector(logger.monitorDetector(penumbra));
        propagator.propagate(interval.getLowerData(), interval.getUpperData());
        propagator.clearEventsDetectors();
    }
}
