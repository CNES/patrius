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
 * @history created 12/09/2014
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * (remove exception throw by SpacecraftState constructor)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.bounds;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Main class for the tests on bounds.
 * 
 * @version $Id: EventsDetectionBoundsTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */

public class EventsDetectionBoundsTest {

    /** t0. */
    private final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

    /** t1. */
    private final AbsoluteDate t1 = this.t0.shiftedBy(500);

    /** t2. */
    private final AbsoluteDate t2 = this.t0.shiftedBy(1000);

    /** Initial orbit. */
    private final Orbit initialOrbit = new KeplerianOrbit(7100000, 0, 0, 0, 0, 0, PositionAngle.MEAN,
        FramesFactory.getEME2000(), this.t0, Constants.EGM96_EARTH_MU);

    /** Convergence parameter. */
    private final double smallConvergence = 1E-12;

    /** Convergence parameter. */
    private final double bigConvergence = 1E-1;

    /**
     * Two parameters for convergence : 1e-12 and 1e-1
     * Numerical propagator.
     * DP 853 integrator
     */
    @Test
    public void testNumericalPropagatorDP853Integrator() {

        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(0.01, 10., 1E-15, 1E-15);
        final Propagator propagator = this.buildNumericalPropagator(integrator);

        // ==================== Play all test cases ===============
        final List<Boolean> list = new ArrayList<Boolean>();

        // convergence parameter 1e-12
        list.add(new UC1(propagator, this.smallConvergence).getTestResult());
        list.add(new UC2(propagator, this.smallConvergence).getTestResult());
        list.add(new UC3(propagator, this.smallConvergence).getTestResult());

        // convergence parameter 1e-1
        list.add(new UC1(propagator, this.bigConvergence).getTestResult());
        list.add(new UC2(propagator, this.bigConvergence).getTestResult());
        list.add(new UC3(propagator, this.bigConvergence).getTestResult());

        boolean result = true;
        for (int i = 0; i < list.size(); i++) {
            result &= list.get(i);
        }
        Assert.assertTrue(result);
    }

    /**
     * Two parameters for convergence : 1e-12 and 1e-1
     * Numerical propagator.
     * Classical RK integrator
     */
    @Test
    public void testNumericalPropagatorClassicalRKIntegrator() {

        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(1.);
        final Propagator propagator = this.buildNumericalPropagator(integrator);

        // ==================== Play all test cases ===============
        final List<Boolean> list = new ArrayList<Boolean>();

        // convergence parameter 1e-12
        list.add(new UC1(propagator, this.smallConvergence).getTestResult());
        list.add(new UC2(propagator, this.smallConvergence).getTestResult());
        list.add(new UC3(propagator, this.smallConvergence).getTestResult());

        // convergence parameter 1e-1
        list.add(new UC1(propagator, this.bigConvergence).getTestResult());
        list.add(new UC2(propagator, this.bigConvergence).getTestResult());
        list.add(new UC3(propagator, this.bigConvergence).getTestResult());

        boolean result = true;
        for (int i = 0; i < list.size(); i++) {
            result &= list.get(i);
        }
        Assert.assertTrue(result);
    }

    /**
     * Two parameters for convergence : 1e-12 and 1e-1
     * Keplerian propagator.
     */
    @Test
    public void testKeplerianPropagator() {

        final Propagator propagator = this.buildKeplerianPropagator();

        // ==================== Play all test cases ===============
        final List<Boolean> list = new ArrayList<Boolean>();

        // convergence parameter 1e-12
        list.add(new UC1(propagator, this.smallConvergence).getTestResult());
        list.add(new UC2(propagator, this.smallConvergence).getTestResult());
        list.add(new UC3(propagator, this.smallConvergence).getTestResult());

        // convergence parameter 1e-1
        list.add(new UC1(propagator, this.bigConvergence).getTestResult());
        list.add(new UC2(propagator, this.bigConvergence).getTestResult());
        list.add(new UC3(propagator, this.bigConvergence).getTestResult());

        boolean result = true;
        for (int i = 0; i < list.size(); i++) {
            result &= list.get(i);
        }
        Assert.assertTrue(result);
    }

    /**
     * Build a Keplerian propagator.
     * 
     * @return Keplerian propagator.
     */
    public Propagator buildKeplerianPropagator() {
        KeplerianPropagator propagatorKep = null;
        try {
            propagatorKep = new KeplerianPropagator(this.initialOrbit);
        } catch (final PropagationException e) {
            e.printStackTrace();
        }
        return propagatorKep;
    }

    /**
     * Build a numerical propagator.
     * 
     * @param integrator
     *        FirstOrderIntegrator
     * @return numerical propagator.
     */
    public Propagator buildNumericalPropagator(final FirstOrderIntegrator integrator) {
        final NumericalPropagator propagatorNum = new NumericalPropagator(integrator);
        propagatorNum.setInitialState(new SpacecraftState(this.initialOrbit));
        return propagatorNum;
    }

    /**
     * Propagation from t0 to t1, event at t1.
     */
    private class UC1 extends UC {

        /** Convergence. */
        private final double convergence;

        /**
         * Constructor.
         * 
         * @param propagator
         *        first order propagator
         * @param convergence2
         *        double
         */
        public UC1(final Propagator propagator, final double convergence2) {
            super(propagator, EventsDetectionBoundsTest.this.t0, EventsDetectionBoundsTest.this.t1,
                EventsDetectionBoundsTest.this.t1, convergence2);
            this.convergence = convergence2;
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(-100),
                this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1, this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(-100),
                this.convergence));
            return list;
        }
    }

    /**
     * Propagation from t1 to t2, event at t1.
     */
    private class UC2 extends UC {

        /** Convergence. */
        private final double convergence;

        /**
         * Constructor.
         * 
         * @param propagator
         *        first order propagator
         * @param convergence2
         *        double
         */
        public UC2(final Propagator propagator, final double convergence2) {
            super(propagator, EventsDetectionBoundsTest.this.t1, EventsDetectionBoundsTest.this.t2,
                EventsDetectionBoundsTest.this.t1, convergence2);
            this.convergence = convergence2;
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1, this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(100),
                this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(100),
                this.convergence));
            return list;
        }
    }

    /**
     * Propagation from t0 to t2, 2 events at t1 with stop action.
     */
    private class UC3 extends UCStop {

        /** Convergence. */
        private final double convergence;

        /**
         * Constructor.
         * 
         * @param propagator
         *        first order propagator
         * @param convergence2
         *        double
         */
        public UC3(final Propagator propagator, final double convergence2) {
            super(propagator, EventsDetectionBoundsTest.this.t0, EventsDetectionBoundsTest.this.t2,
                EventsDetectionBoundsTest.this.t1, convergence2);
            this.convergence = convergence2;
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), EventsDetectionBoundsTest.this.t1, this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(-100),
                this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1, this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(100),
                this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), EventsDetectionBoundsTest.this.t1, this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(100),
                this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1, this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), EventsDetectionBoundsTest.this.t1.shiftedBy(-100),
                this.convergence));
            return list;
        }
    }
}
