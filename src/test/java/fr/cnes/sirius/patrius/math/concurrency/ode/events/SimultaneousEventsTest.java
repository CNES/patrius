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
 * @history created 23/04/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.concurrency.ode.events;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;

/**
 * This test illustrates the fact that event handling
 * in the commons math integrator can now detect two events
 * occuring at the same time.
 * 
 * @author cardosop
 * 
 * @version $Id: SimultaneousEventsTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.2
 * 
 */
public class SimultaneousEventsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Simultaneous events test
         * 
         * @featureDescription Test simultaneous events
         * 
         * @coveredRequirements None
         */
        SIMULTANEOUS_EVENTS
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIMULTANEOUS_EVENTS}
     * 
     * @testedMethod {@link AbstractIntegrator#integrate(FirstOrderDifferentialEquations, double, double[], double, double[])}
     * 
     * @description proves that the simultaneous events are detected
     * 
     * @input an equation
     * 
     * @output number of detected events
     * 
     * @testPassCriteria 2 events are detected
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testSimultaneousEvents() {

        final FirstOrderDifferentialEquations equation = new FirstOrderDifferentialEquations(){

            @Override
            public int getDimension() {
                return 1;
            }

            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
                yDot[0] = 1.0;
            }
        };

        final DormandPrince853Integrator integrator = new DormandPrince853Integrator(0.001, 1000, 1.0e-14, 1.0e-14);
        integrator.addEventHandler(new TimeEvent(11), 0.1, 1.0e-9, 1000);
        integrator.addEventHandler(new TimeEvent(11), 0.1, 1.0e-9, 1000);
        integrator.setInitialStepSize(3.0);

        final double target = 30.0;
        final double[] y = new double[1];
        final double tEnd = integrator.integrate(equation, 0.0, y, target, y);
        System.out.println("Events counter : " + TimeEvent.getEventsCounter());
        // Exact duration --> epsilon 0
        Assert.assertEquals(target, tEnd, 0.);
        Assert.assertEquals(2, TimeEvent.getEventsCounter());

    }

    /**
     * Simple "Time" event.
     * 
     */
    private static class TimeEvent implements EventHandler {

        /** TBC. */
        private static volatile int eventsCounter = 0;
        /** TBC. */
        private final double tEvent;

        /**
         * TBC.
         * 
         * @param timeEvent
         *        TBC.
         */
        public TimeEvent(final double timeEvent) {
            this.tEvent = timeEvent;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }

        /** {@inheritDoc} */
        @Override
        public double g(final double t, final double[] y) {
            return t - this.tEvent;
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
            eventsCounter++;
            System.out.println("TimeEvent " + this.hashCode() + " : event at t = " + t);
            return Action.CONTINUE;
        }

        /** {@inheritDoc} */
        @Override
        public void resetState(final double t, final double[] y) {

        }

        /**
         * Get the number of times an event occurred - all instances.
         * 
         * @return the number
         */
        public static int getEventsCounter() {
            return eventsCounter;
        }

        /** {@inheritDoc} */
        @Override
        public int getSlopeSelection() {
            return 2;
        }
    }
}
