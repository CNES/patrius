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
 * @history created 12/09/2014
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 *  VERSION::DM:226:12/09/2014: problem with event detections.
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.analytical;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.analytical.Detector1.Dependance;

/**
 * Main class for the analytical tests.
 * 
 * @version $Id: EventsDetectionAnalyticalTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public class EventsDetectionAnalyticalTest {

    /** Convergence parameter. */
    private double convergence = 1E-1;

    /** True if detector 1 is added before detector 2. */
    private final boolean ordered = true;

    /**
     * 12 * 2 tests for detecting events with a small and huge convergence parameters.
     */
    @Test
    public void analyticalMain() {

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

        // changing the convergence parameter for a small one
        this.convergence = 1E-12;

        final List<Boolean> list2 = new ArrayList<Boolean>();

        list2.add(new UC1().getTestResult());
        list2.add(new UC2().getTestResult());
        list2.add(new UC3().getTestResult());
        list2.add(new UC4().getTestResult());
        list2.add(new UC5().getTestResult());
        list2.add(new UC6().getTestResult());
        list2.add(new UC7().getTestResult());
        list2.add(new UC8().getTestResult());
        list2.add(new UC9().getTestResult());
        list2.add(new UC10().getTestResult());
        list2.add(new UC11().getTestResult());
        list2.add(new UC12().getTestResult());

        boolean result2 = true;
        for (int i = 0; i < list2.size(); i++) {
            result2 &= list2.get(i);
        }
        Assert.assertTrue(result2);

    }

    /**
     * E1 (continue, nothing), E2 (continue) at t1.
     */
    private class UC1 extends UC {

        /** constructor */
        public UC1() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.CONTINUE, Dependance.NONE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (continue) at t1.
     */
    private class UC2 extends UC {

        /** constructor */
        public UC2() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, cancels E2), E2 (continue) at t1.
     */
    private class UC3 extends UC {

        /** constructor */
        public UC3() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CANCEL,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (continue) at t1.
     */
    private class UC4 extends UC {

        /** constructor */
        public UC4() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1.shiftedBy(10.), Action.CONTINUE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, delays E2), E2 (continue) at t1'.
     */
    private class UC5 extends UC {

        /** constructor */
        public UC5() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.DELAY,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1.shiftedBy(0.2),
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1.shiftedBy(0.2),
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (continue, nothing), E2 (continue) at t2.
     */
    private class UC6 extends UC {

        /** constructor */
        public UC6() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.CONTINUE, Dependance.NONE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (continue) at t2.
     */
    private class UC7 extends UC {

        /** constructor */
        public UC7() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, cancels E2), E2 (continue) at t2.
     */
    private class UC8 extends UC {

        /** constructor */
        public UC8() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CANCEL,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (continue) at t2.
     */
    private class UC9 extends UC {

        /** constructor */
        public UC9() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2.shiftedBy(10.), Action.CONTINUE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2.shiftedBy(-0.1),
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2.shiftedBy(0.1),
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, delays E2) E2 (continue) at t2'.
     */
    private class UC10 extends UC {

        /** constructor */
        public UC10() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.DELAY,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2.shiftedBy(0.2),
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2.shiftedBy(0.2),
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (reset_state) at t1.
     */
    private class UC11 extends UC {

        /** constructor */
        public UC11() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.RESET_STATE, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (reset_state) at t1.
     */
    private class UC12 extends UC {

        /** constructor */
        public UC12() {
            super(EventsDetectionAnalyticalTest.this.convergence, EventsDetectionAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1.shiftedBy(10.), Action.RESET_STATE,
                EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            final List<Event> list = new ArrayList<Event>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1, EventsDetectionAnalyticalTest.this.convergence));
            return list;
        }
    }
}
