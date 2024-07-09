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
 * @history created 15/11/11
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 * 
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link ApsideDetector}.<br>
 * Note : unit test written for code coverage.
 * 
 * @author cardosop
 * 
 * @version $Id: ApsideDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ApsideDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Apside detector
         * 
         * @featureDescription Validate the Apside detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_APSIDE_DETECTOR
    }

    /**
     * A Cartesian orbit used for the tests.
     */
    private static CartesianOrbit tISSOrbit;

    /**
     * A SpacecraftState used for the tests.
     */
    private static SpacecraftState tISSSpState;

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
        final AbsoluteDate date = new AbsoluteDate("2011-11-09T12:00:00Z", TimeScalesFactory.getTT());

        final double mu = CelestialBodyFactory.getEarth().getGM();

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        tISSSpState = new SpacecraftState(tISSOrbit);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#ApsideDetector(Orbit, int)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : Orbit
     * 
     * @output an {@link ApsideDetector}
     * 
     * @testPassCriteria the {@link ApsideDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testApsideDetectorOrbit() {
        final ApsideDetector detector =
            new ApsideDetector(tISSOrbit, 0);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
        // test the throwing of an exception when a constructor parameter is not supported:
        boolean asExpected = false;
        try {
            new ApsideDetector(tISSOrbit, 7);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#ApsideDetector(Orbit, int, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : threshold, Orbit
     * 
     * @output an {@link ApsideDetector}
     * 
     * @testPassCriteria the {@link ApsideDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testApsideDetectorDoubleOrbit() {
        final ApsideDetector detector =
            new ApsideDetector(tISSOrbit, 1, 0.12345);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#ApsideDetector(int, double, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : max check, threshold
     * 
     * @output an {@link ApsideDetector}
     * 
     * @testPassCriteria the {@link ApsideDetector} is successfully created
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testApsideDetectorNoOrbit() {
        final ApsideDetector detector =
            new ApsideDetector(2, 300, 1e-05);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link ApsideDetector#eventOccurred(SpacecraftState, boolean, boolean)}
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
        final ApsideDetector detector =
            new ApsideDetector(tISSOrbit, 2, 0.12345);
        final ApsideDetector detector2 = (ApsideDetector) detector.copy();
        Action rez = detector2.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez);
        rez = detector2.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez);

        Assert.assertEquals(Action.STOP, detector2.getActionAtApogee());
        Assert.assertEquals(Action.STOP, detector2.getActionAtPerigee());
        Assert.assertEquals(false, detector2.removeAtApogee());
        Assert.assertEquals(false, detector2.removeAtPerigee());

        final ApsideDetector detector3 = (ApsideDetector) new ApsideDetector(tISSOrbit, 0).copy();
        Assert.assertEquals(0, detector3.getSlopeSelection());
        final ApsideDetector detector4 = (ApsideDetector) new ApsideDetector(tISSOrbit, 1).copy();
        Assert.assertEquals(1, detector4.getSlopeSelection());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#g(SpacecraftState)}
     * 
     * @description tests {@link ApsideDetector#g(SpacecraftState)}
     * 
     * @input constructor and g parameters
     * 
     * @output g output
     * 
     * @testPassCriteria g returns the expected value
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testG() throws PatriusException {
        final ApsideDetector detector = new ApsideDetector(tISSOrbit, 2);
        // We basically reimplement g...
        final PVCoordinates pvBody = tISSSpState.getPVCoordinates();

        final double expectedDot = Vector3D.dotProduct(pvBody.getPosition(),
            pvBody.getVelocity());
        Assert.assertEquals(expectedDot, detector.g(tISSSpState), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#ApsideDetector(int, double, double, Action)}
     * @testedMethod {@link ApsideDetector#ApsideDetector(double, double, Action, Action)}
     * @testedMethod {@link ApsideDetector#g(SpacecraftState)}
     * 
     * @description tests the numerical propagation with 3 apside detectors: a perigee
     *              detector, an apogee detector, a perigee/apogee detector with different actions in case of apogee or
     *              perigee
     *              detection. In case of propagation, the first detected event is perigee. When occurred, the
     *              propagation should
     *              continue. And the second detected event is apogee. When occurred, the propagation should stop
     * 
     * @input three apside detectors and one numerical propagator
     * 
     * @output the events logged during the propagation
     * 
     * @testPassCriteria the detected events are the expected one. The propagation was stopped at apogee detection.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testPropagationStop() throws PatriusException {
        final ApsideDetector perigee = new ApsideDetector(ApsideDetector.PERIGEE, tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        final ApsideDetector apogee = new ApsideDetector(ApsideDetector.APOGEE, tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        final ApsideDetector perigeeapogee = new ApsideDetector(tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.STOP, Action.CONTINUE);
        // propagator:
        final double period = tISSOrbit.getKeplerianPeriod();
        // no mass: 6 tolerance values for the 6 states
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(tISSSpState);
        // logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(perigee));
        propagator.addEventDetector(logger.monitorDetector(apogee));
        propagator.addEventDetector(logger.monitorDetector(perigeeapogee));
        final SpacecraftState endState = propagator.propagate(tISSSpState.getDate().shiftedBy(period));
        Assert.assertEquals(4, logger.getLoggedEvents().size());
        final LoggedEvent event0 = logger.getLoggedEvents().get(0);
        final LoggedEvent event1 = logger.getLoggedEvents().get(1);
        final LoggedEvent event2 = logger.getLoggedEvents().get(2);
        final LoggedEvent event3 = logger.getLoggedEvents().get(3);
        Assert.assertEquals(event0.getState().getDate(), event1.getState().getDate());
        Assert.assertEquals(event2.getState().getDate(), event3.getState().getDate());
        if (event0.isIncreasing()) {
            // event0,1 = Perigee, event2,3 = Apogee:
            Assert.assertFalse(1 == event0.getEventDetector().getSlopeSelection());
            Assert.assertFalse(1 == event1.getEventDetector().getSlopeSelection());
            Assert.assertFalse(0 == event2.getEventDetector().getSlopeSelection());
            Assert.assertFalse(0 == event3.getEventDetector().getSlopeSelection());
            Assert.assertTrue(event0.getState().getPVCoordinates().getPosition().getNorm()
                < event2.getState().getPVCoordinates().getPosition().getNorm());
        } else {
            // event2,3 = Perigee, event0,1 = Apogee:
            Assert.assertFalse(0 == event0.getEventDetector().getSlopeSelection());
            Assert.assertFalse(0 == event1.getEventDetector().getSlopeSelection());
            Assert.assertFalse(1 == event2.getEventDetector().getSlopeSelection());
            Assert.assertFalse(1 == event3.getEventDetector().getSlopeSelection());
            Assert.assertTrue(event2.getState().getPVCoordinates().getPosition().getNorm()
                < event0.getState().getPVCoordinates().getPosition().getNorm());
        }
        Assert.assertEquals(event3.getState().getDate(), endState.getDate());
        Assert.assertFalse(endState.getDate().equals(tISSSpState.getDate().shiftedBy(period)));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#ApsideDetector(int, double, double, Action)}
     * @testedMethod {@link ApsideDetector#g(SpacecraftState)}
     * 
     * @description tests the numerical propagation and retro-propagtion with 3 apside detectors: a perigee
     *              detector, an apogee detector, a perigee/apogee detector.
     * 
     * @input three apside detectors and one numerical propagator
     * 
     * @output the events logged during the propagation
     * 
     * @testPassCriteria the detected events are the expected one. The detection happen in the expected order.
     *                   The same events are detected during propagation and retro-propagation.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testRetroPropagation() throws PatriusException {
        final ApsideDetector perigee = new ApsideDetector(ApsideDetector.PERIGEE, tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        final ApsideDetector apogee = new ApsideDetector(ApsideDetector.APOGEE, tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        final ApsideDetector perigeeapogee = new ApsideDetector(ApsideDetector.PERIGEE_APOGEE,
            tISSOrbit.getKeplerianPeriod() / 3, 1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        // propagator:
        final double period = tISSOrbit.getKeplerianPeriod();
        // no mass: 6 tolerance values for the 6 states
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(tISSSpState);
        // logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(perigee));
        propagator.addEventDetector(logger.monitorDetector(apogee));
        propagator.addEventDetector(logger.monitorDetector(perigeeapogee));

        propagator.propagate(tISSSpState.getDate().shiftedBy(period));
        Assert.assertEquals(4, logger.getLoggedEvents().size());
        final LoggedEvent event0 = logger.getLoggedEvents().get(0);
        final LoggedEvent event1 = logger.getLoggedEvents().get(1);
        final LoggedEvent event2 = logger.getLoggedEvents().get(2);
        final LoggedEvent event3 = logger.getLoggedEvents().get(3);
        Assert.assertEquals(event0.getState().getDate(), event1.getState().getDate());
        Assert.assertEquals(event2.getState().getDate(), event3.getState().getDate());
        Assert.assertEquals(true, event0.isIncreasing());
        Assert.assertEquals(false, event2.isIncreasing());

        // event0,1 = Perigee, event2,3 = Apogee:
        Assert.assertFalse(1 == event0.getEventDetector().getSlopeSelection());
        Assert.assertFalse(1 == event1.getEventDetector().getSlopeSelection());
        Assert.assertFalse(0 == event2.getEventDetector().getSlopeSelection());
        Assert.assertFalse(0 == event3.getEventDetector().getSlopeSelection());
        Assert.assertTrue(event0.getState().getPVCoordinates().getPosition().getNorm()
            < event2.getState().getPVCoordinates().getPosition().getNorm());

        propagator.propagate(tISSSpState.getDate());
        Assert.assertEquals(8, logger.getLoggedEvents().size());
        final LoggedEvent event4 = logger.getLoggedEvents().get(4);
        final LoggedEvent event5 = logger.getLoggedEvents().get(5);
        final LoggedEvent event6 = logger.getLoggedEvents().get(6);
        final LoggedEvent event7 = logger.getLoggedEvents().get(7);
        Assert.assertEquals(event4.getState().getDate(), event5.getState().getDate());
        Assert.assertEquals(event6.getState().getDate(), event7.getState().getDate());
        Assert.assertEquals(true, event4.isIncreasing());
        Assert.assertEquals(false, event6.isIncreasing());
        // event4,5 = Apogee, event6,7 = Perigee:
        Assert.assertFalse(0 == event4.getEventDetector().getSlopeSelection());
        Assert.assertFalse(0 == event5.getEventDetector().getSlopeSelection());
        Assert.assertFalse(1 == event6.getEventDetector().getSlopeSelection());
        Assert.assertFalse(1 == event7.getEventDetector().getSlopeSelection());
        Assert.assertTrue(event4.getState().getPVCoordinates().getPosition().getNorm()
            > event6.getState().getPVCoordinates().getPosition().getNorm());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_APSIDE_DETECTOR}
     * 
     * @testedMethod {@link ApsideDetector#ApsideDetector(int, double, double, Action)}
     * @testedMethod {@link ApsideDetector#g(SpacecraftState)}
     * 
     * @description tests the numerical propagation with 3 apside detectors: a perigee
     *              detector, an apogee detector, a perigee/apogee detector with different actions in case of apogee or
     *              perigee
     *              detection. In case of propagation, the first detected event is perigee. When occurred, the detector
     *              should
     *              be removed. And the second detected event is apogee. When occurred, the state should be reset
     * 
     * @input three apside detectors and one numerical propagator
     * 
     * @output the events logged during the propagation
     * 
     * @testPassCriteria the detected events are the expected one. The apogee_perigee detector should be removed after
     *                   apogee detection.
     *                   The state should be reset after perigee detection.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testPropagationRemoveDetectorResetState() throws PatriusException {
        final ApsideDetector perigee = new ApsideDetector(ApsideDetector.PERIGEE, tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        final ApsideDetector apogee = new ApsideDetector(ApsideDetector.APOGEE, tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        MyApsideDetector perigeeapogee = new MyApsideDetector(ApsideDetector.PERIGEE_APOGEE,
            tISSOrbit.getKeplerianPeriod() / 3, 1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE);
        // propagator:
        final double period = tISSOrbit.getKeplerianPeriod();
        // no mass: 6 tolerance values for the 6 states
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(tISSSpState);
        // logger:
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(perigee));
        propagator.addEventDetector(logger.monitorDetector(apogee));
        propagator.addEventDetector(logger.monitorDetector(perigeeapogee));
        propagator.propagate(tISSSpState.getDate().shiftedBy(2 * period));
        Assert.assertEquals(8, logger.getLoggedEvents().size());
        LoggedEvent event0 = logger.getLoggedEvents().get(0);
        if (event0.isIncreasing()) {
            // event0,1 = Perigee, event2,3 = Apogee:
            Assert.assertEquals(false, perigeeapogee.isReset());
            Assert.assertEquals(4, perigeeapogee.getCount());
        } else {
            // event2,3 = Perigee, event0,1 = Apogee:
            Assert.assertEquals(false, perigeeapogee.isReset());
            Assert.assertEquals(4, perigeeapogee.getCount());
        }

        // Create a new propagation
        // ApogeePerigee detector with Action.REMOVE_DETECTOR when apogee event occurred
        perigeeapogee = new MyApsideDetector(tISSOrbit.getKeplerianPeriod() / 3,
            1.0e-13 * tISSOrbit.getKeplerianPeriod(), Action.CONTINUE, Action.RESET_STATE, true, false);
        propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(tISSSpState);
        // logger:
        logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(perigee));
        propagator.addEventDetector(logger.monitorDetector(apogee));
        propagator.addEventDetector(logger.monitorDetector(perigeeapogee));
        propagator.propagate(tISSSpState.getDate().shiftedBy(2 * period));
        Assert.assertEquals(6, logger.getLoggedEvents().size());
        event0 = logger.getLoggedEvents().get(0);
        if (event0.isIncreasing()) {
            // event0,1 = Perigee, event2,3 = Apogee:
            Assert.assertEquals(true, perigeeapogee.isReset());
            Assert.assertEquals(2, perigeeapogee.getCount());
        } else {
            // event2,3 = Perigee, event0,1 = Apogee:
            Assert.assertEquals(true, perigeeapogee.isReset());
            Assert.assertEquals(1, perigeeapogee.getCount());
        }
    }

    /**
     * Custom apside detector.
     * This detector enable to know if the state was reset.
     * This detector count the number of event detection.
     */
    class MyApsideDetector extends ApsideDetector {

        private boolean isReset;
        private int count;

        public MyApsideDetector(final double maxCheck, final double threshold, final Action action_apogee,
            final Action action_perigee, final boolean remove_apogee, final boolean remove_perigee) {
            super(maxCheck, threshold, action_apogee, action_perigee, remove_apogee, remove_perigee);
            this.isReset = false;
            this.count = 0;
        }

        public MyApsideDetector(final int slopeSelection, final double maxCheck, final double threshold,
            final Action action) {
            super(slopeSelection, maxCheck, threshold, action);
            this.isReset = false;
            this.count = 0;
        }

        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
            this.isReset = true;
            return oldState;
        }

        public boolean isReset() {
            return this.isReset;
        }

        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            this.count++;
            return super.eventOccurred(s, increasing, forward);
        }

        public int getCount() {
            return this.count;
        }
    }
}
