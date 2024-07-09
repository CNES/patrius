/**
 *
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
 * @history 14/02/2012
 * 
 * HISTORY
* VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.validation.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.stat.StatUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.AbstractBoundedPVProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvLagrange;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.PVCoordinatesPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DihedralFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.precomputed.Ephemeris;
import fr.cnes.sirius.patrius.propagation.precomputed.IntegratedEphemeris;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;
import fr.cnes.sirius.patrius.validation.events.EventsLogger.LoggedEvent;

/**
 * Validation test about event detection accuracy with ephemeris based propagators.
 * 
 * @author cardosop
 * 
 * @version $Id: EphemerisEventDetection.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class EphemerisEventDetection {

    /** String constant. */
    private static final String UNXP2000_405 = "unxp2000.405";
    /** String constant. */
    private static final String NUMBER_OF_DETECTED_EVENTS = "number of detected events :";
    /** String constant. */
    private static final String MS2 = "ms";
    /** String constant. */
    private static final String S = "s:";
    /** String constant. */
    private static final String MN2 = "mn:";
    /** String constant. */
    private static final String H2 = "h:";
    /** String constant. */
    private static final String STATION_VISIBILITY_3 = "station visibility 3";
    /** String constant. */
    private static final String STATION_VISIBILITY_2 = "station visibility 2";
    /** String constant. */
    private static final String STATION_VISIBILITY_1 = "station visibility 1";
    /** String constant. */
    private static final String STR_PENUMBRA = "penumbra";
    /** String constant. */
    private static final String STR_ECLIPSE = "eclipse";
    /** String constant. */
    private static final String NODES_PASSAGES = "nodes passages";
    /** String constant. */
    private static final String APOGEE_PERIGEE_PASSAGES = "apogee perigee passages";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Ephemeris event detection
         * 
         * @featureDescription
         * 
         * @coveredRequirements
         * 
         */
        EPHEMERIS_EVENT
    }

    /**
     * Enum for propagator types.
     * 
     */
    private enum BoundedPropagatorType {
        /** . */
        EPHEMERIS,
        /** . */
        LAGRANGE_EPHEMERIS_8,
        /** . */
        LAGRANGE_EPHEMERIS_10,
        /** . */
        LAGRANGE_EPHEMERIS_12
    }

    /** Numerical propagator. */
    private static NumericalPropagator propagator;
    /** InitialState. */
    private static SpacecraftState initialState;
    /** Attitude provider. */
    private static AttitudeProvider attProvider;
    /** Propagation duration. */
    private static int days = 30;
    /** Propagation interval. */
    private static AbsoluteDateInterval interval;
    /** Apogee/perigee passages detector. */
    private static EventDetector apogeePergieePassages;
    /** Nodes passages detector. */
    private static EventDetector nodesPassages;
    /** Eclipse detector. */
    private static EventDetector eclipse;
    /** Penumbra detector. */
    private static EventDetector penumbra;
    /** Station visibility. */
    private static EventDetector stationVisi35;
    /** Station visibility. */
    private static EventDetector stationVisi30;
    /** Station visibility. */
    private static EventDetector stationVisi20;

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#EPHEMERIS_EVENT}
     * 
     * @testedMethod
     * 
     * @description test the event detection accuracy with ephemeris based propagators
     * 
     * @input
     * 
     * @output none
     * 
     * @testPassCriteria
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testEphemerisEvent() throws PatriusException {

        // Duration between interpolation steps
        final int[] steps = { 10, 60, 300, 600, 1200 };
        // final int[] steps = { 1200 };

        propagator.setEphemerisMode();
        System.out.println("fin setEphemerisMode()");

        // tic
        final long start = System.currentTimeMillis();
        propagator.propagate(interval.getLowerData(), interval.getUpperData());
        // toc
        final long duree = System.currentTimeMillis() - start;
        System.out.println("fin propagate() - " + (duree / 1000.));

        final BoundedPropagator boundedPropagator = propagator.getGeneratedEphemeris();
        System.out.println("fin getGeneratedEphemeris()");

        AbsoluteDateInterval propInterval = interval;

        for (final int step : steps) {
            // Building states
            final List<SpacecraftState> states = new ArrayList<SpacecraftState>();
            // [(int) (interval.getDuration() / step
            // + 1)];

            final AbsoluteDate minDate = boundedPropagator.getMinDate();
            for (int i = 0; i < (int) (interval.getDuration() / step + 1); i++) {
                states.add(boundedPropagator.propagate(minDate.shiftedBy(i * step)));
            }
            System.out.println("fin states - step interval " + step);
            System.out.println();

            final List<EventsLogger.LoggedEvent> eventsRef = compareWithRef(propagator, interval);

            System.out.println("Ephemeris with step = " + step);
            System.out.println();
            this.compareWithRef(step, states, propInterval, BoundedPropagatorType.EPHEMERIS, eventsRef);

            // Lagrange ephemeris
            System.out.println("LagrangeEphemeris with order 8 and step = " + step);
            System.out.println();
            propInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, interval.getLowerData().shiftedBy(
                3 * step), interval.getUpperData().shiftedBy(-3 * step), IntervalEndpointType.CLOSED);
            this.compareWithRef(step, states, propInterval, BoundedPropagatorType.LAGRANGE_EPHEMERIS_8, eventsRef);

            System.out.println("LagrangeEphemeris with order 10 and step = " + step);
            System.out.println();
            propInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, interval.getLowerData().shiftedBy(
                4 * step), interval.getUpperData().shiftedBy(-4 * step), IntervalEndpointType.CLOSED);
            this.compareWithRef(step, states, propInterval, BoundedPropagatorType.LAGRANGE_EPHEMERIS_10, eventsRef);

            System.out.println("LagrangeEphemeris with order 12 and step = " + step);
            System.out.println();
            propInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, interval.getLowerData().shiftedBy(
                5 * step), interval.getUpperData().shiftedBy(-5 * step), IntervalEndpointType.CLOSED);
            this.compareWithRef(step, states, propInterval, BoundedPropagatorType.LAGRANGE_EPHEMERIS_12, eventsRef);

        }

        System.out.println("done!");
    }

    /**
     * Get the logged events of an event detection performed on an object Ephemeris.
     * 
     * @param step
     *        step between the ephemeris given to Ephemeris
     * @param states
     *        results of a propagator set to ephemeris mode
     * @param propInterval
     *        propagation interval
     * @param type
     *        bounded propagator type
     * @param eventsRef
     *        reference list of events
     * @throws PatriusException
     *         should not happen
     */
    private void compareWithRef(final int step, final List<SpacecraftState> states,
                                final AbsoluteDateInterval propInterval, final BoundedPropagatorType type,
                                final List<EventsLogger.LoggedEvent> eventsRef) throws PatriusException {

        final AbsoluteDateInterval safeInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, propInterval
            .getLowerData().shiftedBy(0.101), propInterval.getUpperData().shiftedBy(-0.101),
            IntervalEndpointType.CLOSED);

        // System.out.println("SIZE :" + eventsRef.size());

        BoundedPropagator ephemeris = null;

        final EventsLogger logEph = new EventsLogger();

        // Compare only on the most restricted interval (will be Lagrange 12)
        final EphemerisPvLagrange restrictedEph = new EphemerisPvLagrange(
            states.toArray(new SpacecraftState[states.size()]), 12, null);
        final AbsoluteDate minDate = restrictedEph.getMinDate();
        final AbsoluteDate maxDate = restrictedEph.getMaxDate();

        if (type == BoundedPropagatorType.EPHEMERIS) {
            ephemeris = new Ephemeris(states, 2);
        } else if (type == BoundedPropagatorType.LAGRANGE_EPHEMERIS_8) {
            final EphemerisPvLagrange eph = new EphemerisPvLagrange(states.toArray(new SpacecraftState[states.size()]),
                8, null);
            ;
            final PVCoordinatesPropagator pvp = new PVCoordinatesPropagator(eph, eph.getMinDate(), states.get(0).getMu(),
                states.get(0).getFrame());
            ephemeris = new BoundedPropagatorWrapper(pvp, eph);
        } else if (type == BoundedPropagatorType.LAGRANGE_EPHEMERIS_10) {
            final EphemerisPvLagrange eph = new EphemerisPvLagrange(states.toArray(new SpacecraftState[states.size()]),
                10, null);
            ;
            final PVCoordinatesPropagator pvp = new PVCoordinatesPropagator(eph, eph.getMinDate(), states.get(0).getMu(),
                states.get(0).getFrame());
            ephemeris = new BoundedPropagatorWrapper(pvp, eph);
        } else if (type == BoundedPropagatorType.LAGRANGE_EPHEMERIS_12) {
            final EphemerisPvLagrange eph = new EphemerisPvLagrange(states.toArray(new SpacecraftState[states.size()]),
                12, null);
            ;
            final PVCoordinatesPropagator pvp = new PVCoordinatesPropagator(eph, eph.getMinDate(), states.get(0).getMu(),
                states.get(0).getFrame());
            ephemeris = new BoundedPropagatorWrapper(pvp, eph);
        }

        ephemeris.setAttitudeProvider(attProvider);

        ephemeris.clearEventsDetectors();
        ephemeris.addEventDetector(logEph.monitorDetector(apogeePergieePassages, APOGEE_PERIGEE_PASSAGES));
        ephemeris.addEventDetector(logEph.monitorDetector(nodesPassages, NODES_PASSAGES));
        ephemeris.addEventDetector(logEph.monitorDetector(eclipse, STR_ECLIPSE));
        ephemeris.addEventDetector(logEph.monitorDetector(penumbra, STR_PENUMBRA));
        ephemeris.addEventDetector(logEph.monitorDetector(stationVisi35, STATION_VISIBILITY_1));
        ephemeris.addEventDetector(logEph.monitorDetector(stationVisi30, STATION_VISIBILITY_2));
        ephemeris.addEventDetector(logEph.monitorDetector(stationVisi20, STATION_VISIBILITY_3));

        // tic
        final long start = System.currentTimeMillis();

        ephemeris.propagate(safeInterval.getLowerData(), safeInterval.getUpperData());

        // toc
        final long duree = System.currentTimeMillis() - start;

        // affichage
        final long h = duree / 3600000;
        final long mn = (duree % 3600000) / 60000;
        final long sec = (duree % 60000) / 1000;
        final long ms = (duree % 1000);
        System.out.println("running time ephemeris = " + h + H2 + mn + MN2 + sec + S + ms + MS2);

        compareEvents(eventsRef, logEph.getLoggedEvents(), minDate, maxDate);
    }

    /**
     * Computes the logged events of an event detection performed on an object IntegratedEphemeris.
     * 
     * @param propag
     *        propagator in ephemeris mode which provides the integrated ephemeris
     * @param propInterval
     *        propagation interval
     * @return also, returns a list of events (reference events) for the other reference tests.
     * @throws PropagationException
     *         should not happen
     */
    private static List<EventsLogger.LoggedEvent>
            compareWithRef(final NumericalPropagator propag,
                           final AbsoluteDateInterval propInterval)
                                                                   throws PropagationException {

        final EventsLogger log = new EventsLogger();
        propag.clearEventsDetectors();
        propag.resetInitialState(initialState);
        propag.addEventDetector(log.monitorDetector(apogeePergieePassages, APOGEE_PERIGEE_PASSAGES));
        propag.addEventDetector(log.monitorDetector(nodesPassages, NODES_PASSAGES));
        propag.addEventDetector(log.monitorDetector(eclipse, STR_ECLIPSE));
        propag.addEventDetector(log.monitorDetector(penumbra, STR_PENUMBRA));
        propag.addEventDetector(log.monitorDetector(stationVisi35, STATION_VISIBILITY_1));
        propag.addEventDetector(log.monitorDetector(stationVisi30, STATION_VISIBILITY_2));
        propag.addEventDetector(log.monitorDetector(stationVisi20, STATION_VISIBILITY_3));

        // tic
        long start = System.currentTimeMillis();

        propag.propagate(propInterval.getLowerData(), propInterval.getUpperData());

        // toc
        long duree = System.currentTimeMillis() - start;

        // affichage
        long h = duree / 3600000;
        long mn = (duree % 3600000) / 60000;
        long sec = (duree % 60000) / 1000;
        long ms = (duree % 1000);

        System.out.println("running time propagator = " + h + H2 + mn + MN2 + sec + S + ms + MS2);

        final List<EventsLogger.LoggedEvent> eventsRef = log.getLoggedEvents();
        System.out.println("SIZE :" + eventsRef.size());

        final AbsoluteDateInterval safeInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, propInterval
            .getLowerData().shiftedBy(0.001), propInterval.getUpperData().shiftedBy(-0.001),
            IntervalEndpointType.CLOSED);

        final IntegratedEphemeris integratedEphemeris = (IntegratedEphemeris) propag.getGeneratedEphemeris();

        final EventsLogger logEph = new EventsLogger();
        integratedEphemeris.clearEventsDetectors();
        integratedEphemeris.addEventDetector(logEph.monitorDetector(apogeePergieePassages,
            APOGEE_PERIGEE_PASSAGES));
        integratedEphemeris.addEventDetector(logEph.monitorDetector(nodesPassages, NODES_PASSAGES));
        integratedEphemeris.addEventDetector(logEph.monitorDetector(eclipse, STR_ECLIPSE));
        integratedEphemeris.addEventDetector(logEph.monitorDetector(penumbra, STR_PENUMBRA));
        integratedEphemeris.addEventDetector(logEph.monitorDetector(stationVisi35, STATION_VISIBILITY_1));
        integratedEphemeris.addEventDetector(logEph.monitorDetector(stationVisi30, STATION_VISIBILITY_2));
        integratedEphemeris.addEventDetector(logEph.monitorDetector(stationVisi20, STATION_VISIBILITY_3));

        // tic
        start = System.currentTimeMillis();

        // interval shifted for event detection
        integratedEphemeris.propagate(safeInterval.getLowerData(), safeInterval.getUpperData());

        // toc
        duree = System.currentTimeMillis() - start;
        // affichage
        h = duree / 3600000;
        mn = (duree % 3600000) / 60000;
        sec = (duree % 60000) / 1000;
        ms = (duree % 1000);
        System.out.println("running time integratedEphemeris = " + h + H2 + mn + MN2 + sec + S + ms + MS2);

        compareEvents(eventsRef, logEph.getLoggedEvents(), null, null);

        return eventsRef;
    }

    /**
     * Compares logged events with respect to their dates.
     * 
     * @param eventsRef
     *        reference events list
     * @param eventsRes
     *        obtained events list
     * @param minDate
     *        restriction interval date
     * @param maxDate
     *        restriction interval date
     */
    private static void compareEvents(final List<EventsLogger.LoggedEvent> eventsRef,
                                      final List<EventsLogger.LoggedEvent> eventsRes,
                                      final AbsoluteDate minDate, final AbsoluteDate maxDate) {

        final List<EventsLogger.LoggedEvent> rstEventsRef;
        final List<EventsLogger.LoggedEvent> rstEventsRes;
        if (minDate != null && maxDate != null) {
            rstEventsRef = restrict(eventsRef, minDate, maxDate);
            System.out.println("RESTRICTED number of reference events :" + rstEventsRef.size());
            rstEventsRes = restrict(eventsRes, minDate, maxDate);
        } else {
            rstEventsRef = eventsRef;
            rstEventsRes = eventsRes;
        }

        if (rstEventsRef.size() == rstEventsRes.size()) {
            double meanGap;
            double maxGap;
            final int s = rstEventsRef.size();
            final double[] gaps = new double[s];
            for (int i = 0; i < s; i++) {
                gaps[i] = MathLib.abs(rstEventsRes.get(i).getState().getDate()
                    .durationFrom(rstEventsRef.get(i).getState().getDate()));
            }
            meanGap = StatUtils.mean(gaps);
            maxGap = StatUtils.max(gaps);
            System.out.println(NUMBER_OF_DETECTED_EVENTS + rstEventsRes.size());
            System.out.println("mean gap : " + meanGap + " " + "max gap : " + maxGap);
        } else {
            System.out.println(NUMBER_OF_DETECTED_EVENTS + rstEventsRes.size());
            System.out.println("the number of detected events is different !");
        }
        System.out.println();
    }

    /**
     * Event list restiction on dates.
     * 
     * @param lstOfEvts
     *        list of events
     * @param minDate
     *        restriction interval date
     * @param maxDate
     *        restriction interval date
     * @return list of events restricted to minDate,maxDate
     */
    private static List<EventsLogger.LoggedEvent> restrict(final List<EventsLogger.LoggedEvent> lstOfEvts,
                                                           final AbsoluteDate minDate, final AbsoluteDate maxDate) {
        // Restrict eventsRef on the minDate,maxDate interval
        int firstGood = -1;
        int lastGood = -1;
        int idx = 0;
        while (firstGood == -1) {
            final LoggedEvent curEvt = lstOfEvts.get(idx);
            if (curEvt.getState().getDate().compareTo(minDate) >= 0) {
                firstGood = idx;
            } else {
                idx++;
            }
        }
        idx = lstOfEvts.size() - 1;
        while (lastGood == -1) {
            final LoggedEvent curEvt = lstOfEvts.get(idx);
            if (curEvt.getState().getDate().compareTo(maxDate) <= 0) {
                lastGood = idx;
            } else {
                idx--;
            }
        }

        final List<EventsLogger.LoggedEvent> rstEventsRef = lstOfEvts.subList(firstGood, lastGood + 1);

        return rstEventsRef;
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Before
    public void setUp() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2003");

        // integrator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(0.1, 500, absTOL, relTOL);

        // propagator
        propagator = new NumericalPropagator(integrator);

        // initial orbit
        final AbsoluteDate date = new AbsoluteDate(2005, 1, 2, TimeScalesFactory.getTAI());
        final Frame frame = FramesFactory.getGCRF();
        final double re = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.EGM96_EARTH_MU;
        final double a = 7200000;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.001, MathLib.toRadians(40), MathLib.toRadians(10),
            MathLib.toRadians(15), MathLib.toRadians(20), PositionAngle.MEAN, frame, date, mu);

        interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date, date.shiftedBy(days * 24 * 3600),
            IntervalEndpointType.CLOSED);

        initialState = new SpacecraftState(initialOrbit);
        propagator.resetInitialState(initialState);

        // events
        // apogee perigee passages
        apogeePergieePassages = new ApsideDetector(initialOrbit, 2){
            private static final long serialVersionUID = 7149796307062112194L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // nodes passages
        nodesPassages = new NodeDetector(initialOrbit, initialOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING){
            private static final long serialVersionUID = 1528780196650676150L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader(UNXP2000_405,
            JPLEphemeridesLoader.EphemerisType.SUN);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader(UNXP2000_405,
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader(UNXP2000_405,
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        final CelestialBody sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);

        final JPLEphemeridesLoader loaderEarth = new JPLEphemeridesLoader(UNXP2000_405,
            JPLEphemeridesLoader.EphemerisType.EARTH);

        final CelestialBody earth = loaderEarth.loadCelestialBody(CelestialBodyFactory.EARTH);

        // eclipse
        eclipse = new EclipseDetector(sun, Constants.SUN_RADIUS, earth, re, 0, 300, 0.001){
            private static final long serialVersionUID = -2984027140864819559L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        // penumbra
        penumbra = new EclipseDetector(sun, Constants.SUN_RADIUS, earth, re, 1, 300, 0.001){
            private static final long serialVersionUID = 5098112473308858265L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        final int maxCheck = 120;

        final GeodeticPoint point1 = new GeodeticPoint(MathLib.toRadians(40), MathLib.toRadians(300), 0);
        final GeodeticPoint point2 = new GeodeticPoint(MathLib.toRadians(-30), MathLib.toRadians(250), 0);
        final GeodeticPoint point3 = new GeodeticPoint(MathLib.toRadians(-12), MathLib.toRadians(30), 0);

        final OneAxisEllipsoid earthBody = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
            FramesFactory.getITRF());

        final PVCoordinatesProvider station1 = new TopocentricFrame(earthBody, point1, "station 1");
        final PVCoordinatesProvider station2 = new TopocentricFrame(earthBody, point2, "station 2");
        final PVCoordinatesProvider station3 = new TopocentricFrame(earthBody, point3, "station 3");

        // station visibility
        stationVisi35 = new CircularFieldOfViewDetector(station1, Vector3D.PLUS_I, MathLib.toRadians(35), maxCheck){
            private static final long serialVersionUID = -54150076610577203L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        stationVisi30 = new CircularFieldOfViewDetector(station2, Vector3D.PLUS_I, MathLib.toRadians(30), maxCheck){
            private static final long serialVersionUID = -7242813421915186858L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };
        final Vector3D center = Vector3D.MINUS_I;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = Vector3D.PLUS_J;
        stationVisi20 = new DihedralFieldOfViewDetector(station3, center, axis1, MathLib.toRadians(20), axis2,
            MathLib.toRadians(50), maxCheck){
            private static final long serialVersionUID = 1278789570580110865L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.CONTINUE;
            }
        };

        attProvider = new NadirPointing(earthBody);
        propagator.setAttitudeProvider(attProvider);
    }

    /**
     * Wrapper for propagator and bounded ephemeris to build a BoundedPropagator.
     */
    private class BoundedPropagatorWrapper implements BoundedPropagator {

        /** Underlying propagator. */
        private final Propagator propagator;

        /** Underlying bounded ephemeris. */
        private final AbstractBoundedPVProvider boundedEphemeris;

        public BoundedPropagatorWrapper(final Propagator propagator, final AbstractBoundedPVProvider boundedEphemeris) {
            this.propagator = propagator;
            this.boundedEphemeris = boundedEphemeris;
        }

        @Override
        public int getMode() {
            return this.propagator.getMode();
        }

        @Override
        public void setSlaveMode() {
            this.propagator.setSlaveMode();
        }

        @Override
        public void setMasterMode(final double h, final PatriusFixedStepHandler handler) {
            this.propagator.setMasterMode(h, handler);
        }

        @Override
        public void setMasterMode(final PatriusStepHandler handler) {
            this.propagator.setMasterMode(handler);
        }

        @Override
        public void setEphemerisMode() {
            this.propagator.setEphemerisMode();
        }

        @Override
        public void setOrbitFrame(final Frame frame) throws PatriusException {
            this.propagator.setOrbitFrame(frame);
        }

        @Override
        public BoundedPropagator getGeneratedEphemeris() throws IllegalStateException {
            return this.propagator.getGeneratedEphemeris();
        }

        @Override
        public SpacecraftState getInitialState() throws PatriusException {
            return this.propagator.getInitialState();
        }

        @Override
        public void resetInitialState(final SpacecraftState state) throws PropagationException {
            this.propagator.resetInitialState(state);
        }

        @Override
        public void addEventDetector(final EventDetector detector) {
            this.propagator.addEventDetector(detector);
        }

        @Override
        public Collection<EventDetector> getEventsDetectors() {
            return this.propagator.getEventsDetectors();
        }

        @Override
        public void clearEventsDetectors() {
            this.propagator.clearEventsDetectors();
        }

        @Override
        public AttitudeProvider getAttitudeProvider() {
            return this.propagator.getAttitudeProvider();
        }

        @Override
        public AttitudeProvider getAttitudeProviderForces() {
            return this.propagator.getAttitudeProviderForces();
        }

        @Override
        public AttitudeProvider getAttitudeProviderEvents() {
            return this.propagator.getAttitudeProviderEvents();
        }

        @Override
        public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
            this.propagator.setAttitudeProvider(attitudeProvider);
        }

        @Override
        public void setAttitudeProviderForces(final AttitudeProvider attitudeProviderForces) {
            this.propagator.setAttitudeProviderForces(attitudeProviderForces);
        }

        @Override
        public void setAttitudeProviderEvents(final AttitudeProvider attitudeProviderEvents) {
            this.propagator.setAttitudeProviderEvents(attitudeProviderEvents);
        }

        @Override
        public Frame getFrame() {
            return this.propagator.getFrame();
        }

        @Override
        public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
            return this.propagator.propagate(target);
        }

        @Override
        public SpacecraftState
                propagate(final AbsoluteDate start, final AbsoluteDate target) throws PropagationException {
            return this.propagator.propagate(start, target);
        }

        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return this.propagator.getPVCoordinates(date, frame);
        }

        @Override
        public AbsoluteDate getMinDate() {
            return this.boundedEphemeris.getMinDate();
        }

        @Override
        public AbsoluteDate getMaxDate() {
            return this.boundedEphemeris.getMaxDate();
        }

        @Override
        public SpacecraftState getSpacecraftState(AbsoluteDate date) throws PropagationException {
            return this.propagate(date);
        }

    }
}
