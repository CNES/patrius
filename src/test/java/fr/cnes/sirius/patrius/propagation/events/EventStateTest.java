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
 * HISTORY
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.14:OPENFD-292:22/08/2024: Implementation de multi-propagateurs mixtes
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.AbstractDetector;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.utils.EventState;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Unit tests for EventState.
 * 
 * @author clauded
 * 
 * @version $Id: EventStateTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class EventStateTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle EventState
         * 
         * @featureDescription the state for one {@link EventDetector event detector} during integration steps.
         * 
         * @coveredRequirements DV-EVT_10
         */
        VALIDATE_EVENTSTATE
    }

    /**
     * mu
     */
    private double mu;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description simple test
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria successfully detection
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testEventState() throws PatriusException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date,
            this.mu);

        final Propagator propagator = new KeplerianPropagator(orbit);
        final DateDetector detector = new DateDetector(date.shiftedBy(10.0), 0.5, 10);
        detector.addEventDate(date.shiftedBy(3));
        SpacecraftState s;
        propagator.addEventDetector(detector);
        s = propagator.propagate(date.shiftedBy(1));
        Assert.assertTrue(detector.g(s) > 0);

        detector.resetState(s);

        final Collection<EventDetector> events = propagator.getEventsDetectors();
        Assert.assertTrue(events.size() == 1);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description simple test that covers a special case, the corner case, when the convergence is reached.
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria successfully detection, no exception raised.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testCornerCaseNoException() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        this.mu = Constants.EGM96_EARTH_MU;
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7500000, 0.001, 0.40, 0, 0, 0, PositionAngle.MEAN, referenceFrame, date,
            this.mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final Propagator propagator = new KeplerianPropagator(orbit);

        propagator.resetInitialState(initialState);

        final EventDetector detector = new EventMock(date, 5, 0.05);
        propagator.addEventDetector(detector);

        propagator.propagate(date, date.shiftedBy(30));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description simple test that covers a case when the event's g() method raises an OrekitException.
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria a PropagationException is raised (it wraps the event detector exception).
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         expected, as a PropagationException subclass.
     */
    @Test(expected = PropagationException.class)
    public void testGException() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        this.mu = Constants.EGM96_EARTH_MU;
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7500000, 0.001, 0.40, 0, 0, 0, PositionAngle.MEAN, referenceFrame, date,
            this.mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final Propagator propagator = new KeplerianPropagator(orbit);

        propagator.resetInitialState(initialState);

        final EventMock detector = new EventMock(date, 5, 0.05);
        propagator.addEventDetector(detector);
        // Makes the event detector fail after some calls
        detector.makeGFail(true, 10);

        propagator.propagate(date, date.shiftedBy(30));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getEventDetector()}
     * @testedMethod {@link EventState#evaluateStep(org.orekit.propagation.sampling.OrekitStepInterpolator)}
     * 
     * @description miscelaneous tests for coverage.
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria none
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testMiscCoverage() throws PatriusException {

        final AbsoluteDate date = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        this.mu = Constants.EGM96_EARTH_MU;
        final Frame referenceFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7500000, 0.001, 0.40, 0, 0, 0, PositionAngle.MEAN, referenceFrame, date,
            this.mu);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        final Propagator propagator = new KeplerianPropagator(orbit);

        propagator.resetInitialState(initialState);

        final EventDetector detector = new DateDetector(initialState.getDate());

        final EventState myEventState = new EventState(detector);

        // Reset called as the detector is set for the initial date exactly (special case) => returned state should be
        // null
        final SpacecraftState nullState = myEventState.reset(initialState);
        Assert.assertNull(nullState);

    }
    
    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATE_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#evaluateStep(MultiPatriusStepInterpolator, String)}
     *               {@link EventState#evaluateStep(PatriusStepInterpolator)
     * 
     * @description miscelaneous tests for coverage
     * 
     * @input constructor parameters
     * 
     * @output an {@link EventState}
     * 
     * @testPassCriteria none
     * 
     * @referenceVersion 4.14
     * 
     * @nonRegressionVersion 4.14
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testOtherSolver() throws PatriusException {

        /**
         * This detector triggers an event when reaching a given date.
         */
        class MyDateDetector extends AbstractDetector {

            private static final long serialVersionUID = 2194486837569694560L;
            /**
             * Date triggering event.
             */
            AbsoluteDate date;

            /**
             * @param date
             * @param stepDuration
             * @param slopeSelectionIn
             * @param maxCheckIn
             * @param thresholdIn
             * @param actionIn
             * @param removeIn
             */
            public MyDateDetector(final AbsoluteDate date, final int slopeSelectionIn,
                                  final double maxCheckIn, final double thresholdIn, final Action actionIn,
                                  final boolean removeIn) {
                super(slopeSelectionIn, maxCheckIn, thresholdIn, actionIn, removeIn);
                this.date = new AbsoluteDate(date.getEpoch(), date.getOffset());
            }

            @Override
            public EventDetector copy() {
                return null;
            }

            @Override
            public double g(SpacecraftState s) throws PatriusException {
                return s.getDate().durationFrom(this.date);
            }

        }

        /**
         * This class permits to access basic propagation method publicly in our private BasicStepInterpolator.
         */
        class MyKeplerianPropagator extends KeplerianPropagator {

            private static final long serialVersionUID = 1L;

            /**
             * Default constructor.
             * 
             * @param initialOrbit
             * @throws PropagationException
             */
            public MyKeplerianPropagator(final Orbit initialOrbit) throws PropagationException {
                super(initialOrbit);
            }

            /**
             * Basic propagation.
             */
            public SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
                return super.basicPropagate(date);
            }

        }

        /**
         * Internal class for local propagation (copied and adapted from {@link AbstractPropagator}).
         */
        class BasicStepInterpolator implements PatriusStepInterpolator {

            /** Serializable UID. */
            private static final long serialVersionUID = 26269718303505539L;

            /** Global previous date. */
            private AbsoluteDate globalPreviousDate;

            /** Global current date. */
            private AbsoluteDate globalCurrentDate;

            /** Global forward propagation indicator. */
            private boolean globalForward;

            /** Soft previous date. */
            private AbsoluteDate softPreviousDate;

            /** Soft current date. */
            private AbsoluteDate softCurrentDate;

            /** Interpolated state. */
            private SpacecraftState interpolatedState;

            /** Forward propagation indicator. */
            private boolean forward;

            /** Propagator. */
            private MyKeplerianPropagator propagator;

            /**
             * Build a new instance from a basic propagator.
             * 
             * @param propagator
             */
            public BasicStepInterpolator(final MyKeplerianPropagator propagator) {
                this.globalPreviousDate = AbsoluteDate.PAST_INFINITY;
                this.globalCurrentDate = AbsoluteDate.PAST_INFINITY;
                this.softPreviousDate = AbsoluteDate.PAST_INFINITY;
                this.softCurrentDate = AbsoluteDate.PAST_INFINITY;
                this.propagator = propagator;
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getCurrentDate() {
                return this.softCurrentDate;
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getInterpolatedDate() {
                return this.interpolatedState.getDate();
            }

            /** {@inheritDoc} */
            @Override
            public SpacecraftState getInterpolatedState() throws PatriusException {
                return this.interpolatedState;
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getPreviousDate() {
                return this.softPreviousDate;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isForward() {
                return this.forward;
            }

            /** {@inheritDoc} */
            @Override
            public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {
                // compute the raw spacecraft state
                this.interpolatedState = this.propagator.basicPropagate(date);
            }

            /**
             * Store the current step date.
             *
             * @param date
             *        current date
             * @exception PropagationException
             *            if the state cannot be propagated at specified date
             */
            public void storeDate(final AbsoluteDate date) throws PropagationException {
                this.globalCurrentDate = date;
                this.softCurrentDate = this.globalCurrentDate;

                if (this.globalCurrentDate.compareTo(this.globalPreviousDate) == 0) {
                    // Current date = previous date: the only way to known propagation direction is to compare global
                    // propagation direction
                    this.forward = this.globalForward;
                } else {
                    this.forward = this.globalCurrentDate.compareTo(this.globalPreviousDate) >= 0;
                }

                this.setInterpolatedDate(this.globalCurrentDate);
            }

        }

        /**
         * Mock multi-step interpolator.
         */
        class MockMultiStepInterpolator implements MultiPatriusStepInterpolator {

            /** Global previous date. */
            private AbsoluteDate globalPreviousDate;

            /** Global current date. */
            private AbsoluteDate globalCurrentDate;

            /** Global forward propagation indicator. */
            private boolean globalForward;

            /** Soft previous date. */
            private AbsoluteDate softPreviousDate;

            /** Soft current date. */
            private AbsoluteDate softCurrentDate;

            /** Interpolated date. */
            private AbsoluteDate interpolatedDate;

            /** Interpolated states. */
            private Map<String, SpacecraftState> interpolatedStates;

            /** Forward propagation indicator. */
            private boolean forward;

            /** Propagators. */
            private Map<String, Propagator> propagators;

            /**
             * Build a new instance from a basic propagator.
             * 
             * @throws PatriusException
             */
            public MockMultiStepInterpolator(final Map<String, Propagator> propagators)
                throws PatriusException {
                this.globalPreviousDate = AbsoluteDate.PAST_INFINITY;
                this.globalCurrentDate = AbsoluteDate.PAST_INFINITY;
                this.softPreviousDate = AbsoluteDate.PAST_INFINITY;
                this.softCurrentDate = AbsoluteDate.PAST_INFINITY;
                this.propagators = new HashMap<>(propagators);
                this.interpolatedStates = new HashMap<>(propagators.size());
                for (final Entry<String, Propagator> entry : this.propagators.entrySet()) {
                    this.interpolatedStates.put(entry.getKey(), entry.getValue().getInitialState());
                }
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getCurrentDate() {
                return this.softCurrentDate;
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getInterpolatedDate() {
                return this.interpolatedDate;
            }

            /** {@inheritDoc} */
            @Override
            public Map<String, SpacecraftState> getInterpolatedStates() throws PatriusException {
                return this.interpolatedStates;
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getPreviousDate() {
                return this.softPreviousDate;
            }

            /** {@inheritDoc} */
            @Override
            public boolean isForward() {
                return this.forward;
            }

            /** {@inheritDoc} */
            @Override
            public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {
                // Compute raw spacecraft states
                this.interpolatedDate = date;
                this.interpolatedStates = basicPropagate(date);

            }

            /**
             * Store the current step date and triggers basic propagation.
             *
             * @param date
             *        current date
             * 
             * @exception PropagationException
             *            if the state cannot be propagated at specified date
             */
            public void storeDate(final AbsoluteDate date) throws PropagationException {
                this.globalCurrentDate = date;
                this.softCurrentDate = this.globalCurrentDate;

                if (this.globalCurrentDate.compareTo(this.globalPreviousDate) == 0) {
                    // Current date = previous date: the only way to known propagation direction is to compare global
                    // propagation direction
                    this.forward = this.globalForward;
                } else {
                    this.forward = this.globalCurrentDate.compareTo(this.globalPreviousDate) >= 0;
                }

                this.setInterpolatedDate(this.globalCurrentDate);
            }

            /**
             * Basic propagation.
             * 
             * @param date
             * @return
             * @throws PropagationException
             */
            private Map<String, SpacecraftState> basicPropagate(final AbsoluteDate date) throws PropagationException {

                final Map<String, SpacecraftState> basicPropagationMap = new HashMap<>();

                try {
                    final Iterator<String> idsIterator = this.interpolatedStates.keySet().iterator();

                    while (idsIterator.hasNext()) {
                        final String satId = idsIterator.next();
                        final Propagator propagator = this.propagators.get(satId);

                        SpacecraftState spacecraftState = propagator.propagate(date);
                        do {
                            /*
                             * Use of a loop here to compensate for very small date shifts error that may occur with
                             * long
                             * propagation durations. A simple call to propagator.getSpacecraftState(date) ends up with
                             * such
                             * lacks of precision sometimes.
                             */
                            spacecraftState = spacecraftState.shiftedBy(date.durationFrom(spacecraftState.getDate()));
                        } while (MathLib.abs(date.durationFrom(spacecraftState.getDate())) > Precision.EPSILON);

                        basicPropagationMap.put(satId, spacecraftState);
                    }

                } catch (final PatriusException exception) {
                    throw new PropagationException(exception);
                }

                return basicPropagationMap;

            }

        }

        final double dt = 5.;
        final AbsoluteDate date0 = new AbsoluteDate(2000, 3, 1, TimeScalesFactory.getTT());
        final AbsoluteDate dateEvent = date0.shiftedBy(dt);
        final AbsoluteDate dateTarget = date0.shiftedBy(2 * dt);

        this.mu = Constants.EGM96_EARTH_MU;
        final Frame gcrf = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7E6, 0., 0., 0., 0., 0., PositionAngle.TRUE, gcrf, date0, this.mu);
        final MyKeplerianPropagator propagator = new MyKeplerianPropagator(orbit);

        final double maxcheck = 1E-3;
        final double threshold = 1E-2;
        final EventDetector detector = new MyDateDetector(dateEvent, 0, maxcheck, threshold, Action.CONTINUE, false);

        final String bracketed = "bracketed";
        final String notBracketed = "not bracketed";
        final EventState eventStateBracketed = new EventState(detector, bracketed);
        final EventState eventStateBrent = new EventState(detector, new BrentSolver(), notBracketed);

        // Initialize event states
        final SpacecraftState state0 = propagator.getInitialState();
        eventStateBracketed.reinitializeBegin(state0);
        eventStateBrent.reinitializeBegin(state0);

        final HashMap<String, Propagator> map0 = new HashMap<>(1);
        final String id0 = "test";
        map0.put(id0, propagator);
        final MockMultiStepInterpolator interpolator = new MockMultiStepInterpolator(map0);

        interpolator.storeDate(dateTarget);
        Assert.assertTrue(eventStateBracketed.evaluateStep(interpolator, id0));
        Assert.assertTrue(eventStateBrent.evaluateStep(interpolator, id0));
        Assert.assertEquals(dateEvent, eventStateBracketed.getEventTime());
        Assert.assertEquals(dateEvent, eventStateBrent.getEventTime());

        interpolator.storeDate(dateTarget);
        eventStateBracketed.storeState(interpolator.getInterpolatedStates().get(id0), true);

        eventStateBracketed.reinitializeBegin(state0);
        eventStateBrent.reinitializeBegin(state0);

        // Set interpolator to a date very close to date0 (diff below threshold)
        final AbsoluteDate dateClose0 = date0.shiftedBy(threshold / 10);
        interpolator.storeDate(dateClose0);

        // Evaluate the step at this step, no event shall occur
        Assert.assertFalse(eventStateBracketed.evaluateStep(interpolator, id0));

        // Redo previous test with a detector that shall trigger an event
        final EventDetector detectorClose0 =
            new MyDateDetector(dateClose0, 0, maxcheck, threshold, Action.CONTINUE, false);
        final EventState eventStateBracketedClose0 = new EventState(detectorClose0, bracketed);
        eventStateBracketedClose0.reinitializeBegin(state0);
        interpolator.storeDate(dateClose0.shiftedBy(threshold / 10));
        Assert.assertTrue(eventStateBracketedClose0.evaluateStep(interpolator, id0));
        // Event is now considered pending, evaluating the step with final state should return true
        Assert.assertTrue(eventStateBracketedClose0.evaluateStep(interpolator.getInterpolatedStates().get(id0)));

        // Cover close date case with a BasicStepIntepolator
        eventStateBracketedClose0.reinitializeBegin(state0);
        final BasicStepInterpolator interpolatorNew = new BasicStepInterpolator(propagator);
        interpolatorNew.storeDate(dateClose0.shiftedBy(threshold / 10));
        Assert.assertTrue(eventStateBracketedClose0.evaluateStep(interpolatorNew));
        
        // Cover lines of evaluateStep(SpacecraftState)
        final EventState eventStateNew = new EventState(detector, bracketed);
        interpolator.storeDate(dateEvent.shiftedBy(- dt / 2));
        Assert.assertFalse(eventStateNew.evaluateStep(interpolator.getInterpolatedStates().get(id0)));
        interpolator.storeDate(dateTarget);
        Assert.assertTrue(eventStateNew.evaluateStep(interpolator.getInterpolatedStates().get(id0)));
        eventStateNew.stepAccepted(interpolator.getInterpolatedStates().get(id0));
        Assert.assertTrue(eventStateNew.evaluateStep(interpolator.getInterpolatedStates().get(id0)));

    }

    /**
     * setup
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.clear();
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        this.mu = Constants.EIGEN5C_EARTH_MU;
    }
}
