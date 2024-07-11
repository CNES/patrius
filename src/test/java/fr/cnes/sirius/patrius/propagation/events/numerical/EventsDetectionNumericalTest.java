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
 * VERSION::FA:1868:08/01/2019: handle proper end of integration
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.numerical;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler.Action;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.propagation.events.numerical.Detector1.Dependance;

/**
 * Main.
 * 
 * @version $Id: EventsDetectionNumericalTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class EventsDetectionNumericalTest {

    /** Integrator. */
    private FirstOrderIntegrator integrator;

    /** Convergence parameter. */
    private double convergence;

    /** True if detector 1 is added before detector 2. */
    private final boolean ordered = true;

    /**
     * Main with a classical RK integrator and a small convergence parameter : 1e-12
     */
    @Test
    public void numericalMainClassicalRKSmallConvergenceParameter() {

        this.integrator = new ClassicalRungeKuttaIntegrator(1.);
        this.convergence = 1E-12;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<Boolean>();

        list.add(new UC1().getTestResult());
        list.add(new UC2().getTestResult());
        list.add(new UC3().getTestResult());
        list.add(new UC4().getTestResult());
        list.add(new UC5().getTestResult());
        list.add(new UC6().getTestResult());
        list.add(new UC7().getTestResult());
        list.add(new UC8().getTestResult());
        list.add(new UC9().getTestResult());
        list.add(new UC10().getTestResult());
        // Temporarily commented because of EventState class issues (PATRIUS 4.2 - FA-1868)
        // list.add(new UC11().getTestResult());
        list.add(new UC12().getTestResult());

        boolean result = true;
        for (int i = 0; i < list.size(); i++) {
            result &= list.get(i);
        }
        Assert.assertTrue(result);
    }

    /**
     * Main with a classical RK integrator and a big convergence parameter : 1e-1
     */
    @Test
    public void numericalMainClassicalRKBigConvergenceParameter() {

        this.integrator = new ClassicalRungeKuttaIntegrator(1.);
        this.convergence = 1E-1;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<Boolean>();

        list.add(new UC1().getTestResult());
        list.add(new UC2().getTestResult());
        list.add(new UC3().getTestResult());
        list.add(new UC4().getTestResult());
        list.add(new UC5().getTestResult());
        list.add(new UC6().getTestResult());
        list.add(new UC7().getTestResult());
        list.add(new UC8().getTestResult());
        list.add(new UC9().getTestResult());
        list.add(new UC10().getTestResult());
        list.add(new UC11().getTestResult());
        list.add(new UC12().getTestResult());

        boolean result = true;
        for (int i = 0; i < list.size(); i++) {
            result &= list.get(i);
        }
        Assert.assertTrue(result);
    }

    /**
     * Main with a Dormand Prince integrator and a small convergence parameter : 1e-12
     */
    @Test
    public void numericalMainDP853SmallConvergenceParameter() {

        this.integrator = new DormandPrince853Integrator(0.01, 2., 1E-15, 1E-15);
        this.convergence = 1E-12;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<Boolean>();

        list.add(new UC1().getTestResult());
        list.add(new UC2().getTestResult());
        list.add(new UC3().getTestResult());
        list.add(new UC4().getTestResult());
        list.add(new UC5().getTestResult());
        list.add(new UC6().getTestResult());
        list.add(new UC7().getTestResult());
        list.add(new UC8().getTestResult());
        list.add(new UC9().getTestResult());
        list.add(new UC10().getTestResult());
        list.add(new UC11().getTestResult());
        list.add(new UC12().getTestResult());

        boolean result = true;
        for (int i = 0; i < list.size(); i++) {
            result &= list.get(i);
        }
        Assert.assertTrue(result);
    }

    /**
     * Main with a Dormand Prince integrator and a big convergence parameter : 1e-1
     */
    @Test
    public void numericalMainDP853BigConvergenceParameter() {

        this.integrator = new DormandPrince853Integrator(0.1, 10., 1E-15, 1E-15);
        this.convergence = 1E-1;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<Boolean>();

        list.add(new UC1().getTestResult());
        list.add(new UC2().getTestResult());
        list.add(new UC3().getTestResult());
        list.add(new UC4().getTestResult());
        list.add(new UC5().getTestResult());
        list.add(new UC6().getTestResult());
        list.add(new UC7().getTestResult());
        list.add(new UC8().getTestResult());
        list.add(new UC9().getTestResult());
        list.add(new UC10().getTestResult());
        list.add(new UC11().getTestResult());
        list.add(new UC12().getTestResult());

        boolean result = true;
        for (int i = 0; i < list.size(); i++) {
            result &= list.get(i);
        }
        Assert.assertTrue(result);
    }

    /**
     * E1 (continue, nothing), E2 (continue) at t1.
     */
    private class UC1 extends UC {

        /** Constructor. */
        public UC1() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.CONTINUE, Dependance.NONE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (continue) at t1.
     */
    private class UC2 extends UC {
        /** Constructor. */
        public UC2() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, cancels E2), E2 (continue) at t1.
     */
    private class UC3 extends UC {

        /** Constructor. */
        public UC3() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CANCEL,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1 - 10,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, -4.5,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, -4.5,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (continue) at t1.
     */
    private class UC4 extends UC {

        /** Constructor. */
        public UC4() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1 + 10, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, delays E2), E2 (continue) at t1'.
     */
    private class UC5 extends UC {

        /** Constructor. */
        public UC5() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.DELAY,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1 + 0.2, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1 - 0.2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1 - 0.2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1 + 0.2, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1 - 0.2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (continue, nothing), E2 (continue) at t2.
     */
    private class UC6 extends UC {

        /** Constructor. */
        public UC6() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.CONTINUE, Dependance.NONE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (continue) at t2.
     */
    private class UC7 extends UC {

        /** Constructor. */
        public UC7() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, cancels E2), E2 (continue) at t2.
     */
    private class UC8 extends UC {

        /** Constructor. */
        public UC8() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CANCEL,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2 - 10,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1 - 10,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2 - 10,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (continue) at t2.
     */
    private class UC9 extends UC {

        /** Constructor. */
        public UC9() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2 + 10, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2 - 0.1, this.t2 + 10.1 - 0.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2 - 0.1, this.t2 + 10.1 - 0.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, 25.7,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, delays E2) E2 (continue) at t2'.
     */
    private class UC10 extends UC {

        /** Constructor. */
        public UC10() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.DELAY,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2 + 0.2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2 - 0.2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1 - 0.2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2 + 0.2, this.t2,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, this.t2 - 0.2,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (reset_state) at t1.
     */
    private class UC11 extends UC {

        /** Constructor. */
        public UC11() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.RESET_STATE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (reset_state) at t1.
     */
    private class UC12 extends UC {

        /** Constructor. */
        public UC12() {
            super(EventsDetectionNumericalTest.this.integrator, EventsDetectionNumericalTest.this.convergence,
                EventsDetectionNumericalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1 + 10, Action.RESET_STATE, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionNumericalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, this.t1 + 10.1,
                EventsDetectionNumericalTest.this.convergence));
            return list;
        }
    }
}
