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
 * @history created 15/11/11
 *
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
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
 * Unit tests for {@link NodeDetector}.<br>
 * Note : unit test written for code coverage.
 * 
 * @author cardosop
 * 
 * @version $Id: NodeDetectorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class NodeDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the node detector
         * 
         * @featureDescription Validate the node detector
         * 
         * @coveredRequirements DV-EVT_120, DV-EVT_50
         */
        VALIDATE_NODE_DETECTOR
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
        final AbsoluteDate date = new AbsoluteDate("2011-11-09T12:00:00Z",
            TimeScalesFactory.getTT());

        final double mu = CelestialBodyFactory.getEarth().getGM();

        final Vector3D issPos = new Vector3D(ix, iy, iz);
        final Vector3D issVit = new Vector3D(ivx, ivy, ivz);
        final PVCoordinates pvCoordinates = new PVCoordinates(issPos, issVit);
        tISSOrbit = new CartesianOrbit(pvCoordinates,
            FramesFactory.getEME2000(), date, mu);

        tISSSpState = new SpacecraftState(tISSOrbit);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#NodeDetector(Orbit, Frame, int)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : Orbit, Frame
     * 
     * @output an {@link NodeDetector}
     * 
     * @testPassCriteria the {@link NodeDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testNodeDetectorOrbitFrame() {
        final NodeDetector detector =
            new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
        // test the throwing of an exception when a constructor parameter is not supported:
        boolean asExpected = false;
        try {
            new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), 9);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            asExpected = true;
        }
        Assert.assertTrue(asExpected);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#NodeDetector(Orbit, Frame, int, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : threshold, Orbit, Frame
     * 
     * @output an {@link NodeDetector}
     * 
     * @testPassCriteria the {@link NodeDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testNodeDetectorDoubleOrbitFrame() {
        final NodeDetector detector =
            new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING, 0.12345);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#NodeDetector(Frame, int, double, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters : max check, threshold, Frame
     * 
     * @output an {@link NodeDetector}
     * 
     * @testPassCriteria the {@link NodeDetector} is successfully created
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testNodeDetectorDoubleDoubleFrame() {
        final NodeDetector detector =
            new NodeDetector(tISSOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING, 300, 1e-05);
        // The constructor did not crash...
        Assert.assertNotNull(detector);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#getFrame()}
     * 
     * @description tests {@link NodeDetector#getFrame()}
     * 
     * @input constructor parameters
     * 
     * @output getFrame output
     * 
     * @testPassCriteria getFrame returns the expected frame
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen here
     */
    @Test
    public final void testGetFrame() throws PatriusException {
        final Frame expectedFrame = FramesFactory.getEME2000();
        final NodeDetector detector =
            new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING);
        Assert.assertEquals(expectedFrame, detector.getFrame());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link NodeDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor and eventOccured parameters
     * 
     * @output eventOccured outputs
     * 
     * @testPassCriteria eventOccured returns the expected values for true and false as second parameters
     *                   for true and false as third parameters
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
        final NodeDetector detector =
            new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING);
        final NodeDetector detector2 = (NodeDetector) detector.copy();
        Action rez = detector2.eventOccurred(tISSSpState, true, true);
        Assert.assertEquals(Action.STOP, rez);
        rez = detector2.eventOccurred(tISSSpState, false, true);
        Assert.assertEquals(Action.STOP, rez);
        rez = detector2.eventOccurred(tISSSpState, true, false);
        Assert.assertEquals(Action.STOP, rez);
        rez = detector2.eventOccurred(tISSSpState, false, false);
        Assert.assertEquals(Action.STOP, rez);

        Assert.assertEquals(Action.STOP, detector2.getActionAtEntry());
        Assert.assertEquals(Action.STOP, detector2.getActionAtExit());
        Assert.assertEquals(false, detector2.isRemoveAtEntry());
        Assert.assertEquals(false, detector2.isRemoveAtExit());

        final NodeDetector detector3 =
            (NodeDetector) new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), NodeDetector.ASCENDING).copy();
        Assert.assertEquals(NodeDetector.ASCENDING, detector3.getSlopeSelection());
        final NodeDetector detector4 =
            (NodeDetector) new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), NodeDetector.DESCENDING).copy();
        Assert.assertEquals(NodeDetector.DESCENDING, detector4.getSlopeSelection());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#g(SpacecraftState)}
     * 
     * @description tests {@link NodeDetector#g(SpacecraftState)}
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
        final Frame targetFrame = FramesFactory.getEME2000();
        final NodeDetector detector =
            new NodeDetector(tISSOrbit, tISSOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING);
        // We basically reimplement g...
        final CartesianOrbit expectedOrbit = tISSOrbit;
        final SpacecraftState expectedState = new SpacecraftState(expectedOrbit);
        Assert.assertEquals(expectedState.getPVCoordinates(
            targetFrame).getPosition().getZ(), detector.g(tISSSpState), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#NodeDetector(Frame, int, double, double, Action)}
     * @testedMethod {@link NodeDetector#g(SpacecraftState)}
     * 
     * @description tests the numerical propagation and retro-propagation with 3 node detectors: an ascending
     *              node detector, a descending node detector, a ascending/descending nodes detector.
     * 
     * @input three node detectors and one numerical propagator
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
        final double maxCheck = tISSOrbit.getKeplerianPeriod() / 3;
        final double threshold = 1.0e-13 * tISSOrbit.getKeplerianPeriod();
        final NodeDetector ascending = new NodeDetector(FramesFactory.getEME2000(), NodeDetector.ASCENDING, maxCheck,
            threshold, Action.CONTINUE);
        final NodeDetector descending = new NodeDetector(FramesFactory.getEME2000(), NodeDetector.DESCENDING, maxCheck,
            threshold, Action.CONTINUE);
        final NodeDetector ascendingdescending = new NodeDetector(FramesFactory.getEME2000(),
            NodeDetector.ASCENDING_DESCENDING, maxCheck, threshold, Action.CONTINUE);

        // propagator:
        final double period = tISSOrbit.getKeplerianPeriod();
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(tISSSpState);
        // logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(ascending));
        propagator.addEventDetector(logger.monitorDetector(descending));
        propagator.addEventDetector(logger.monitorDetector(ascendingdescending));

        /*
         * PROPAGATION
         */
        propagator.propagate(tISSSpState.getDate().shiftedBy(period));
        Assert.assertEquals(4, logger.getLoggedEvents().size());
        final LoggedEvent event0 = logger.getLoggedEvents().get(0);
        final LoggedEvent event1 = logger.getLoggedEvents().get(1);
        final LoggedEvent event2 = logger.getLoggedEvents().get(2);
        final LoggedEvent event3 = logger.getLoggedEvents().get(3);
        Assert.assertEquals(event0.getState().getDate(), event1.getState().getDate());
        Assert.assertEquals(event2.getState().getDate(), event3.getState().getDate());
        Assert.assertTrue(event0.isIncreasing());
        // event0,1 = ascending, event2,3 = descending:
        Assert.assertFalse(1 == event0.getEventDetector().getSlopeSelection());
        Assert.assertFalse(1 == event1.getEventDetector().getSlopeSelection());
        Assert.assertFalse(0 == event2.getEventDetector().getSlopeSelection());
        Assert.assertFalse(0 == event3.getEventDetector().getSlopeSelection());
        Assert.assertTrue(event0.getState().getPVCoordinates().getVelocity().getZ() > 0);
        Assert.assertTrue(event2.getState().getPVCoordinates().getVelocity().getZ() < 0);

        /*
         * RETRO PROPAGATION
         */
        propagator.propagate(tISSSpState.getDate());
        Assert.assertEquals(8, logger.getLoggedEvents().size());
        final LoggedEvent event4 = logger.getLoggedEvents().get(4);
        final LoggedEvent event5 = logger.getLoggedEvents().get(5);
        final LoggedEvent event6 = logger.getLoggedEvents().get(6);
        final LoggedEvent event7 = logger.getLoggedEvents().get(7);
        Assert.assertEquals(event4.getState().getDate(), event5.getState().getDate());
        Assert.assertEquals(event6.getState().getDate(), event7.getState().getDate());
        Assert.assertTrue(event4.isIncreasing());
        // event0,1 = ascending, event2,3 = descending:
        Assert.assertFalse(0 == event4.getEventDetector().getSlopeSelection());
        Assert.assertFalse(0 == event5.getEventDetector().getSlopeSelection());
        Assert.assertFalse(1 == event6.getEventDetector().getSlopeSelection());
        Assert.assertFalse(1 == event7.getEventDetector().getSlopeSelection());
        Assert.assertTrue(event4.getState().getPVCoordinates().getVelocity().getZ() < 0);
        Assert.assertTrue(event6.getState().getPVCoordinates().getVelocity().getZ() > 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_NODE_DETECTOR}
     * 
     * @testedMethod {@link NodeDetector#NodeDetector(Frame, int, double, double, Action)}
     * @testedMethod {@link NodeDetector#g(SpacecraftState)}
     * 
     * @description tests the numerical propagation and retro-propagation with 3 node detectors: an ascending
     *              node detector, a descending node detector, a ascending/descending nodes detector.
     * 
     * @input three node detectors and one numerical propagator
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
    public final void testPropagation() throws PatriusException {
        final double maxCheck = tISSOrbit.getKeplerianPeriod() / 3;
        final double threshold = 1.0e-13 * tISSOrbit.getKeplerianPeriod();
        final NodeDetector ascending = new NodeDetector(FramesFactory.getEME2000(), NodeDetector.ASCENDING, maxCheck,
            threshold, Action.CONTINUE);
        final NodeDetector descending = new NodeDetector(FramesFactory.getEME2000(), NodeDetector.DESCENDING, maxCheck,
            threshold, Action.CONTINUE);
        final MyNodeDetector ascendingdescending = new MyNodeDetector(FramesFactory.getEME2000(), maxCheck, threshold,
            Action.RESET_STATE, Action.CONTINUE, false, true);

        // propagator:
        final double period = tISSOrbit.getKeplerianPeriod();
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        final Propagator propagator = new NumericalPropagator(integrator);
        propagator.resetInitialState(tISSSpState);
        // logger:
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(ascending));
        propagator.addEventDetector(logger.monitorDetector(descending));
        propagator.addEventDetector(logger.monitorDetector(ascendingdescending));

        /*
         * PROPAGATION
         */
        propagator.propagate(tISSSpState.getDate().shiftedBy(2 * period));
        Assert.assertEquals(6, logger.getLoggedEvents().size());
        final LoggedEvent event0 = logger.getLoggedEvents().get(0);
        Assert.assertTrue(event0.isIncreasing());
        // event0,1 = ascending, event2,3 = descending:
        Assert.assertEquals(true, ascendingdescending.isReset());
        Assert.assertEquals(2, ascendingdescending.getCount());
    }

    /**
     * Custom node detector.
     * This detector enable to know if the state is reset.
     * This detector count the number of event detection.
     */
    class MyNodeDetector extends NodeDetector {

        private boolean isReset;
        private int count;

        public MyNodeDetector(final Frame frame, final double maxCheck,
            final double threshold, final Action ascendingNode, final Action descendingNode,
            final boolean removeAscending, final boolean removeDescending) {
            super(frame, maxCheck, threshold, ascendingNode, descendingNode, removeAscending, removeDescending);
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
