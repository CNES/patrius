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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else "nominal" dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::FA:764:06/01/2017:Anomaly on events detection
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.events;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.sampling.AbstractStepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.DummyStepInterpolator;

public class EventStateTest {

    /**
     * FA-2116.
     * 
     * @testType UT
     * 
     * @description test a particular case of event detection with a max check of 100s:
     *              - Detector 1 with events at t=11s, t=13s, t=50s
     *              - Detector 2 with events at t=12s (reset_state)
     *              The behavior is the following:
     *              - Detection of D1 at t = 50s and D2 at t = 12s
     *              - Reset_state is first event
     *              - Detection on interval [0; 12] => D1 at t = 11s
     *              - Reset_state of D2 at t = 12s
     *              - Events at t=13s and t=50s of D1 are not detected
     * 
     * @testPassCriteria behavior is as expected
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public void testFA2116() {

        final double RESET_STATE_DATE = 12;
        final double INTEGRATION_END = 100;

        // Initialization
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(INTEGRATION_END);
        final FirstOrderDifferentialEquations equations = new FirstOrderDifferentialEquations(){
            @Override
            public int getDimension() {
                return 1;
            }

            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
                yDot[0] = 1;
            }
        };

        // Add event detectors
        final EventHandler eventHandler1 = new EventHandler(){

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public int getSlopeSelection() {
                return 0;
            }

            @Override
            public double g(final double t, final double[] y) {
                if (Double.isNaN(t)) {
                    Assert.fail();
                }
                double g = 0;
                if (t < RESET_STATE_DATE - 1) {
                    g = -1;
                } else if (t > RESET_STATE_DATE - 1 && t < RESET_STATE_DATE + 1) {
                    g = 1;
                } else if (t > RESET_STATE_DATE + 1 && t < 50) {
                    g = -1;
                } else {
                    g = 1;
                }
                return g;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                // Action at t = 11s
                Assert.assertEquals(11., t, 0.001);
                return Action.CONTINUE;
            }
        };

        final EventHandler eventHandler2 = new EventHandler(){

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            @Override
            public void resetState(final double t, final double[] y) {
                // Reset state at t = 12s
                Assert.assertEquals(12., t, 0.001);
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public int getSlopeSelection() {
                return 0;
            }

            @Override
            public double g(final double t, final double[] y) {
                return t - RESET_STATE_DATE;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.RESET_STATE;
            }
        };

        integrator.addEventHandler(eventHandler1, INTEGRATION_END, 0.001, 100);
        integrator.addEventHandler(eventHandler2, INTEGRATION_END, 0.001, 100);

        // Integration
        final double[] y0 = { 0. };
        final double[] y = { 0. };
        integrator.integrate(equations, 0, y0, INTEGRATION_END, y);
    }

    // JIRA: MATH-322
    @Test
    public void closeEvents() throws MaxCountExceededException, NoBracketingException {

        final double r1 = 90.0;
        final double r2 = 135.0;
        final double gap = r2 - r1;
        final EventHandler closeEventsGenerator = new EventHandler(){
            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return (t - r1) * (r2 - t);
            }

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }
        };

        final double tolerance = 0.1;
        final EventState es = new EventState(closeEventsGenerator, 1.5 * gap,
            tolerance, 100,
            new BrentSolver(tolerance));

        final AbstractStepInterpolator interpolator =
            new DummyStepInterpolator(new double[0], new double[0], true);
        interpolator.storeTime(r1 - 2.5 * gap);
        interpolator.shift();
        interpolator.storeTime(r1 - 1.5 * gap);
        es.reinitializeBegin(interpolator);

        interpolator.shift();
        interpolator.storeTime(r1 - 0.5 * gap);
        Assert.assertFalse(es.evaluateStep(interpolator));

        interpolator.shift();
        interpolator.storeTime(0.5 * (r1 + r2));
        Assert.assertTrue(es.evaluateStep(interpolator));
        Assert.assertEquals(r1, es.getEventTime(), tolerance);
        es.stepAccepted(es.getEventTime(), new double[0]);

        interpolator.shift();
        interpolator.storeTime(r2 + 0.4 * gap);
        Assert.assertTrue(es.evaluateStep(interpolator));
        Assert.assertEquals(r2, es.getEventTime(), tolerance);

    }

    // Tests the detection of an event in a step using the method evaluateStep,
    // when the event handler is set to detect only events with increasing g function. (slopeSelection = 0)
    @Test
    public void closeEventsSlopeSelectionZero() {

        final double r1 = 90.0;
        final double r2 = 135.0;
        final double gap = r2 - r1;
        final EventHandler closeEventsGenerator = new EventHandler(){
            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return (t - r1) * (r2 - t);
            }

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            // the slope selection parameter is set to zero:
            @Override
            public int getSlopeSelection() {
                return 0;
            }
        };

        final double tolerance = 0.1;
        final EventState es = new EventState(closeEventsGenerator, 1.5 * gap,
            tolerance, 100,
            new BrentSolver(tolerance));

        final AbstractStepInterpolator interpolator =
            new DummyStepInterpolator(new double[0], new double[0], true);
        interpolator.storeTime(r1 - 2.5 * gap);
        interpolator.shift();
        interpolator.storeTime(r1 - 1.5 * gap);
        es.reinitializeBegin(interpolator);

        interpolator.shift();
        interpolator.storeTime(0.5 * (r1 + r2));
        // the event r1 should be detected (g function is increasing in r1)
        Assert.assertTrue(es.evaluateStep(interpolator));
        Assert.assertEquals(r1, es.getEventTime(), tolerance);
        es.stepAccepted(es.getEventTime(), new double[0]);

        interpolator.shift();
        interpolator.storeTime(r2 + 0.4 * gap);
        // the event r2 should not be detected (g function is decreasing in r2)
        Assert.assertFalse(es.evaluateStep(interpolator));
    }

}
