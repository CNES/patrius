/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.solver.BaseSecantSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.PegasusSolver;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;

/**
 * Tests for overlapping state events. Also tests an event function that does
 * not converge to zero, but does have values of opposite sign around its root.
 */
public class OverlappingEventsTest implements FirstOrderDifferentialEquations {

    /** Expected event times for first event. */
    private static final double[] EVENT_TIMES1 = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0,
        7.0, 8.0, 9.0 };

    /** Expected event times for second event. */
    private static final double[] EVENT_TIMES2 = { 0.5, 1.0, 1.5, 2.0, 2.5, 3.0,
        3.5, 4.0, 4.5, 5.0, 5.5, 6.0,
        6.5, 7.0, 7.5, 8.0, 8.5, 9.0,
        9.5 };

    /**
     * Test for events that occur at the exact same time, but due to numerical
     * calculations occur very close together instead. Uses event type 0. See
     * {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler#g(double, double[])
     * EventHandler.g(double, double[])}.
     */
    @Test
    public void testOverlappingEvents0()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {
        this.test(0);
    }

    /**
     * Test for events that occur at the exact same time, but due to numerical
     * calculations occur very close together instead. Uses event type 1. See
     * {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler#g(double, double[])
     * EventHandler.g(double, double[])}.
     */
    @Test
    public void testOverlappingEvents1()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {
        this.test(1);
    }

    /**
     * Test for events that occur at the exact same time, but due to numerical
     * calculations occur very close together instead.
     * 
     * @param eventType
     *        the type of events to use. See
     *        {@link fr.cnes.sirius.patrius.math.ode.events.EventHandler#g(double, double[])
     *        EventHandler.g(double, double[])}.
     */
    public void test(final int eventType)
                                         throws DimensionMismatchException, NumberIsTooSmallException,
                                         MaxCountExceededException, NoBracketingException {
        final double e = 1e-15;
        final FirstOrderIntegrator integrator = new DormandPrince853Integrator(e, 100.0, 1e-7, 1e-7);
        final BaseSecantSolver rootSolver = new PegasusSolver(e, e);
        final EventHandler evt1 = new Event(0, eventType);
        final EventHandler evt2 = new Event(1, eventType);
        integrator.addEventHandler(evt1, 0.1, e, 999, rootSolver);
        integrator.addEventHandler(evt2, 0.1, e, 999, rootSolver);
        double t = 0.0;
        final double tEnd = 10.0;
        final double[] y = { 0.0, 0.0 };
        final List<Double> events1 = new ArrayList<Double>();
        final List<Double> events2 = new ArrayList<Double>();
        while (t < tEnd) {
            t = integrator.integrate(this, t, y, tEnd, y);
            // System.out.println("t=" + t + ",\t\ty=[" + y[0] + "," + y[1] + "]");

            if (y[0] >= 1.0) {
                y[0] = 0.0;
                events1.add(t);
                // System.out.println("Event 1 @ t=" + t);
            }
            if (y[1] >= 1.0) {
                y[1] = 0.0;
                events2.add(t);
                // System.out.println("Event 2 @ t=" + t);
            }
        }
        Assert.assertEquals(EVENT_TIMES1.length, events1.size());
        Assert.assertEquals(EVENT_TIMES2.length, events2.size());
        for (int i = 0; i < EVENT_TIMES1.length; i++) {
            Assert.assertEquals(EVENT_TIMES1[i], events1.get(i), 1e-7);
        }
        for (int i = 0; i < EVENT_TIMES2.length; i++) {
            Assert.assertEquals(EVENT_TIMES2[i], events2.get(i), 1e-7);
        }
        // System.out.println();
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        yDot[0] = 1.0;
        yDot[1] = 2.0;
    }

    /** State events for this unit test. */
    private class Event implements EventHandler {
        /** The index of the continuous variable to use. */
        private final int idx;

        /** The event type to use. See {@link #g}. */
        private final int eventType;

        /**
         * Constructor for the {@link Event} class.
         * 
         * @param idx
         *        the index of the continuous variable to use
         * @param eventType
         *        the type of event to use. See {@link #g}
         */
        public Event(final int idx, final int eventType) {
            this.idx = idx;
            this.eventType = eventType;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }

        /** {@inheritDoc} */
        @Override
        public double g(final double t, final double[] y) {
            return (this.eventType == 0) ? y[this.idx] >= 1.0 ? 1.0 : -1.0
                : y[this.idx] - 1.0;
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
            return Action.STOP;
        }

        /** {@inheritDoc} */
        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void resetState(final double t, final double[] y) {
            // Never called.
        }

        /** {@inheritDoc} */
        @Override
        public int getSlopeSelection() {
            return 2;
        }
    }
}
