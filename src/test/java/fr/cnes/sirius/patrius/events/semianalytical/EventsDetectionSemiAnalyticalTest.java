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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events.semianalytical;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.events.semianalytical.Detector1.Dependance;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;

/**
 * Main class for the semi analytical tests.
 * 
 * @version $Id$
 * 
 * @since 2.3
 */
public class EventsDetectionSemiAnalyticalTest {

    /** Integrator. */
    private FirstOrderIntegrator integrator;

    /** Convergence parameter. */
    private double convergence;

    /** True if detector 1 is added before detector 2. */
    private final boolean ordered = true;

    /**
     * 12 Test cases with a classical RK integrator and a convergence parameter of 1e-12
     * for StelaAbstractPropagator
     */
    @Test
    public void semiAnalyticalMainSmallConvergenceClassicalRK() {

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());
        
        this.integrator = new ClassicalRungeKuttaIntegrator(1.);
        this.convergence = 1E-12;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<>();

        list.add(new UC1().getTestResult());
        list.add(new UC2().getTestResult());
        // list.add(new UC3().getTestResult());
        list.add(new UC4().getTestResult());
        // list.add(new UC5().getTestResult());
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
     * 12 Test cases with a classical RK integrator and a convergence parameter of 1e-1
     * for StelaAbstractPropagator
     */
    @Test
    public void semiAnalyticalMainBigConvergenceClassicalRK() {

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        this.integrator = new ClassicalRungeKuttaIntegrator(1.);
        this.convergence = 1E-1;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<>();

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
            // System.out.println("UC" + (i + 1) + " : " + list.get(i));
        }
        Assert.assertTrue(result);
    }

    /**
     * 12 Test cases with a RK6 integrator and a convergence parameter of 1e-1
     * for StelaAbstractPropagator
     */
    @Test
    public void semiAnalyticalMainBigConvergenceRK6() {

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        this.integrator = new RungeKutta6Integrator(1.);
        this.convergence = 1e-1;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<>();

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
     * 12 Test cases with a RK6 integrator and a convergence parameter of 1e-12
     * for StelaAbstractPropagator
     */
    @Test
    public void semiAnalyticalMainSmallConvergenceRK6() {

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        this.integrator = new RungeKutta6Integrator(1.);
        this.convergence = 1e-12;

        // ==================== Play all test cases ===============

        final List<Boolean> list = new ArrayList<>();

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

        /** constructor */
        public UC1() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.CONTINUE, Dependance.NONE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (continue) at t1.
     */
    private class UC2 extends UC {

        /** constructor */
        public UC2() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, false);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, cancels E2), E2 (continue) at t1.
     */
    private class UC3 extends UC {

        /** constructor */
        public UC3() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CANCEL,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (continue) at t1.
     */
    private class UC4 extends UC {

        /** constructor */
        public UC4() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1.shiftedBy(10.), Action.CONTINUE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, delays E2), E2 (continue) at t1'.
     */
    private class UC5 extends UC {

        /** constructor */
        public UC5() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.DELAY,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1.shiftedBy(0.2),
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (continue, nothing), E2 (continue) at t2.
     */
    private class UC6 extends UC {

        /** constructor */
        public UC6() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.CONTINUE, Dependance.NONE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (continue) at t2.
     */
    private class UC7 extends UC {

        /** constructor */
        public UC7() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, cancels E2), E2 (continue) at t2.
     */
    private class UC8 extends UC {

        /** constructor */
        public UC8() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CANCEL,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (continue) at t2.
     */
    private class UC9 extends UC {

        /** constructor */
        public UC9() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2.shiftedBy(10.), Action.CONTINUE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2.shiftedBy(-0.1),
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, delays E2) E2 (continue) at t2'.
     */
    private class UC10 extends UC {

        /** constructor */
        public UC10() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.DELAY,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t2, Action.CONTINUE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t2, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t2.shiftedBy(0.2),
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t2,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, nothing), E2 (reset_state) at t1.
     */
    private class UC11 extends UC {

        /** constructor */
        public UC11() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.NONE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1, Action.RESET_STATE, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }

    /**
     * E1 (reset_state, creates E2), E2 (reset_state) at t1.
     */
    private class UC12 extends UC {

        /** constructor */
        public UC12() {
            super(EventsDetectionSemiAnalyticalTest.this.integrator,
                EventsDetectionSemiAnalyticalTest.this.convergence, EventsDetectionSemiAnalyticalTest.this.ordered);
        }

        @Override
        public Detector1 getDetector1() {
            return new Detector1(this.t1, Action.RESET_STATE, Dependance.CREATE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector2 getDetector2() {
            return new Detector2(this.t1.shiftedBy(10.), Action.RESET_STATE,
                EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public Detector3 getDetector3() {
            return new Detector3(this.t1, EventsDetectionSemiAnalyticalTest.this.convergence);
        }

        @Override
        public List<Event> getExpectedEventList() {
            final List<Event> list = new ArrayList<>();
            list.add(new Event(Detector1.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new ResetState(Detector2.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            list.add(new Event(Detector3.class.getSimpleName(), this.t1,
                EventsDetectionSemiAnalyticalTest.this.convergence));
            return list;
        }

        @Override
        public List<Event> getExpectedEventListRetropolation() {
            // Not tested
            return null;
        }
    }
}
