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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.TestProblem1;
import fr.cnes.sirius.patrius.math.ode.TestProblem3;
import fr.cnes.sirius.patrius.math.ode.TestProblem4;
import fr.cnes.sirius.patrius.math.ode.TestProblem5;
import fr.cnes.sirius.patrius.math.ode.TestProblemHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DormandPrince853IntegratorTest {

    @Test
    public void testMissedEndEvent()
                                    throws DimensionMismatchException, NumberIsTooSmallException,
                                    MaxCountExceededException, NoBracketingException {
        final double t0 = 1878250320.0000029;
        final double tEvent = 1878250379.9999986;
        final double[] k = { 1.0e-4, 1.0e-5, 1.0e-6 };
        final FirstOrderDifferentialEquations ode = new FirstOrderDifferentialEquations(){

            @Override
            public int getDimension() {
                return k.length;
            }

            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
                for (int i = 0; i < y.length; ++i) {
                    yDot[i] = k[i] * y[i];
                }
            }
        };

        final DormandPrince853Integrator integrator = new DormandPrince853Integrator(0.0, 100.0,
            1.0e-10, 1.0e-10);

        final double[] y0 = new double[k.length];
        for (int i = 0; i < y0.length; ++i) {
            y0[i] = i + 1;
        }
        final double[] y = new double[k.length];

        integrator.setInitialStepSize(60.0);
        double finalT = integrator.integrate(ode, t0, y0, tEvent, y);
        Assert.assertEquals(tEvent, finalT, 5.0e-6);
        for (int i = 0; i < y.length; ++i) {
            Assert.assertEquals(y0[i] * MathLib.exp(k[i] * (finalT - t0)), y[i], 1.0e-9);
        }

        integrator.setInitialStepSize(60.0);
        integrator.addEventHandler(new EventHandler(){

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return t - tEvent;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                Assert.assertEquals(tEvent, t, 5.0e-6);
                return Action.CONTINUE;
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }
        }, Double.POSITIVE_INFINITY, 1.0e-20, 100);
        finalT = integrator.integrate(ode, t0, y0, tEvent + 120, y);
        Assert.assertEquals(tEvent + 120, finalT, 5.0e-6);
        for (int i = 0; i < y.length; ++i) {
            Assert.assertEquals(y0[i] * MathLib.exp(k[i] * (finalT - t0)), y[i], 1.0e-9);
        }

    }

    @Test(expected = DimensionMismatchException.class)
    public void testDimensionCheck()
                                    throws DimensionMismatchException, NumberIsTooSmallException,
                                    MaxCountExceededException, NoBracketingException {
        final TestProblem1 pb = new TestProblem1();
        final DormandPrince853Integrator integrator = new DormandPrince853Integrator(0.0, 1.0,
            1.0e-10, 1.0e-10);
        integrator.integrate(pb,
            0.0, new double[pb.getDimension() + 10],
            1.0, new double[pb.getDimension() + 10]);
        Assert.fail("an exception should have been thrown");
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testNullIntervalCheck()
                                       throws DimensionMismatchException, NumberIsTooSmallException,
                                       MaxCountExceededException, NoBracketingException {
        final TestProblem1 pb = new TestProblem1();
        final DormandPrince853Integrator integrator = new DormandPrince853Integrator(0.0, 1.0,
            1.0e-10, 1.0e-10);
        integrator.integrate(pb,
            0.0, new double[pb.getDimension()],
            0.0, new double[pb.getDimension()]);
        Assert.fail("an exception should have been thrown");
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testMinStep()
                             throws DimensionMismatchException, NumberIsTooSmallException,
                             MaxCountExceededException, NoBracketingException {

        final TestProblem1 pb = new TestProblem1();
        final double minStep = 0.1 * (pb.getFinalTime() - pb.getInitialTime());
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double[] vecAbsoluteTolerance = { 1.0e-15, 1.0e-16 };
        final double[] vecRelativeTolerance = { 1.0e-15, 1.0e-16 };

        final FirstOrderIntegrator integ = new DormandPrince853Integrator(minStep, maxStep,
            vecAbsoluteTolerance,
            vecRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
        Assert.fail("an exception should have been thrown");

    }

    @Test
    public void testIncreasingTolerance()
                                         throws DimensionMismatchException, NumberIsTooSmallException,
                                         MaxCountExceededException, NoBracketingException {

        int previousCalls = Integer.MAX_VALUE;
        final AdaptiveStepsizeIntegrator integ =
            new DormandPrince853Integrator(1, 1E10,
                Double.NaN, Double.NaN);
        for (int i = -12; i < -2; ++i) {
            final TestProblem1 pb = new TestProblem1();
            final double minStep = 0;
            final double maxStep = pb.getFinalTime() - pb.getInitialTime();
            final double scalAbsoluteTolerance = MathLib.pow(10.0, i);
            final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;
            integ.setStepSizeControl(minStep, maxStep, scalAbsoluteTolerance,
                scalRelativeTolerance, true);

            final TestProblemHandler handler = new TestProblemHandler(pb, integ);
            integ.addStepHandler(handler);
            integ.integrate(pb,
                pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);

            // the 1.3 factor is only valid for this test
            // and has been obtained from trial and error
            // there is no general relation between local and global errors
            Assert.assertTrue(handler.getMaximalValueError() < (1.3 * scalAbsoluteTolerance));
            Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);

            final int calls = pb.getCalls();
            Assert.assertEquals(integ.getEvaluations(), calls);
            Assert.assertTrue(calls <= previousCalls);
            previousCalls = calls;

        }

    }

    @Test
    public void testTooLargeFirstStep()
                                       throws DimensionMismatchException, NumberIsTooSmallException,
                                       MaxCountExceededException, NoBracketingException {

        final AdaptiveStepsizeIntegrator integ =
            new DormandPrince853Integrator(1, 1E10, Double.NaN, Double.NaN);
        final double start = 0.0;
        final double end = 0.001;
        final FirstOrderDifferentialEquations equations = new FirstOrderDifferentialEquations(){

            @Override
            public int getDimension() {
                return 1;
            }

            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
                Assert.assertTrue(t >= MathLib.nextAfter(start, Double.NEGATIVE_INFINITY));
                Assert.assertTrue(t <= MathLib.nextAfter(end, Double.POSITIVE_INFINITY));
                yDot[0] = -100.0 * y[0];
            }

        };

        integ.setStepSizeControl(0, 1.0, 1.0e-6, 1.0e-8, true);
        integ.integrate(equations, start, new double[] { 1.0 }, end, new double[1]);

    }

    @Test
    public void testBackward()
                              throws DimensionMismatchException, NumberIsTooSmallException,
                              MaxCountExceededException, NoBracketingException {

        final TestProblem5 pb = new TestProblem5();
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

        final FirstOrderIntegrator integ = new DormandPrince853Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 1.1e-7);
        Assert.assertTrue(handler.getMaximalValueError() < 1.1e-7);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("Dormand-Prince 8 (5, 3)", integ.getName());
    }

    @Test
    public void testEvents()
                            throws DimensionMismatchException, NumberIsTooSmallException,
                            MaxCountExceededException, NoBracketingException {

        final TestProblem4 pb = new TestProblem4();
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-9;
        final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

        final FirstOrderIntegrator integ = new DormandPrince853Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        final EventHandler[] functions = pb.getEventsHandlers();
        final double convergence = 1.0e-8 * maxStep;
        for (final EventHandler function : functions) {
            integ.addEventHandler(function, Double.POSITIVE_INFINITY, convergence, 1000);
        }
        Assert.assertEquals(functions.length, integ.getEventHandlers().size());
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertEquals(0, handler.getMaximalValueError(), 2.1e-7);
        Assert.assertEquals(0, handler.getMaximalTimeError(), convergence);
        Assert.assertEquals(12.0, handler.getLastTime(), convergence);
        integ.clearEventHandlers();
        Assert.assertEquals(0, integ.getEventHandlers().size());

    }

    @Test
    public void testKepler()
                            throws DimensionMismatchException, NumberIsTooSmallException,
                            MaxCountExceededException, NoBracketingException {

        final TestProblem3 pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = scalAbsoluteTolerance;

        final FirstOrderIntegrator integ = new DormandPrince853Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        integ.addStepHandler(new KeplerHandler(pb));
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertEquals(integ.getEvaluations(), pb.getCalls());
        Assert.assertTrue(pb.getCalls() < 3300);

    }

    @Test
    public void testVariableSteps()
                                   throws DimensionMismatchException, NumberIsTooSmallException,
                                   MaxCountExceededException, NoBracketingException {

        final TestProblem3 pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = scalAbsoluteTolerance;

        final FirstOrderIntegrator integ = new DormandPrince853Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        integ.addStepHandler(new VariableHandler());
        final double stopTime = integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
        Assert.assertEquals(pb.getFinalTime(), stopTime, 1.0e-10);
        Assert.assertEquals("Dormand-Prince 8 (5, 3)", integ.getName());
    }

    @Test
    public void testUnstableDerivative()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {
        final StepProblem stepProblem = new StepProblem(0.0, 1.0, 2.0);
        final FirstOrderIntegrator integ =
            new DormandPrince853Integrator(0.1, 10, 1.0e-12, 0.0);
        integ.addEventHandler(stepProblem, 1.0, 1.0e-12, 1000);
        final double[] y = { Double.NaN };
        integ.integrate(stepProblem, 0.0, new double[] { 0.0 }, 10.0, y);
        Assert.assertEquals(8.0, y[0], 1.0e-12);
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link DormandPrince853Integrator#DormandPrince853Integrator(double, double, double, double)}
     * @testedMethod {@link DormandPrince853Integrator#DormandPrince853Integrator(double, double[], double[], double)}
     * @testedMethod {@link DormandPrince853Integrator#DormandPrince853Integrator(double, double, double, double, boolean)}
     * @testedMethod {@link DormandPrince853Integrator#DormandPrince853Integrator(double, double[], double[], double, boolean)}
     * @testedMethod {@link EmbeddedRungeKuttaIntegrator#integrate(ExpandableStatefulODE, double)}
     * @testedMethod {@link AdaptiveStepsizeIntegrator#filterStep(double, boolean, boolean)}
     * 
     * @description Test a NumberIsTooSmallException exception is expected during the integration
     *              process only when the default constructor is used or the acceptSmall boolean
     *              value is set to false.
     * 
     * @input parameters
     * 
     * @output integrator
     * 
     * @throws PatriusException
     * 
     * @testPassCriteria expected NumberIsTooSmallException Exception in specified cases
     * 
     * @see none
     * 
     * @comments none
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void FT2099() throws PatriusException {

        // Integrator initialization
        final double minStep = 1.0;
        final double maxStep = 10.;
        final double absTol = 1.e-6;
        final double relTol = 0.;
        final double[] absTolVec = { 1.e-6 };
        final double[] relTolVec = { 0. };

        final FirstOrderIntegrator integrator1 = new DormandPrince853Integrator(minStep, maxStep,
            absTol, relTol);
        final FirstOrderIntegrator integrator2 = new DormandPrince853Integrator(minStep, maxStep,
            absTol, relTol, false);
        final FirstOrderIntegrator integrator3 = new DormandPrince853Integrator(minStep, maxStep,
            absTol, relTol, true);

        final FirstOrderIntegrator integrator4 = new DormandPrince853Integrator(minStep, maxStep,
            absTolVec, relTolVec);
        final FirstOrderIntegrator integrator5 = new DormandPrince853Integrator(minStep, maxStep,
            absTolVec, relTolVec, false);
        final FirstOrderIntegrator integrator6 = new DormandPrince853Integrator(minStep, maxStep,
            absTolVec, relTolVec, true);

        // Check - expectException : true if exception is expected
        this.checkIntegration(integrator1, true);
        this.checkIntegration(integrator2, true);
        this.checkIntegration(integrator3, false);

        this.checkIntegration(integrator4, true);
        this.checkIntegration(integrator5, true);
        this.checkIntegration(integrator6, false);

    }

    /**
     * Check integration is performed as expected (exception or not) depending on whether integrator allow small steps
     * or not.
     * 
     * @param integrator integrator to test
     * @param expectException true if exception is expected
     */
    private void checkIntegration(final FirstOrderIntegrator integrator, final boolean expectException) {

        try {
            // ODE with step
            final FirstOrderDifferentialEquations ode = new FirstOrderDifferentialEquations(){
                @Override
                public int getDimension() {
                    return 1;
                }

                @Override
                public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
                    if (t < 5) {
                        yDot[0] = 1;
                    } else {
                        yDot[0] = -1;
                    }
                }
            };

            // Integration
            final double[] y0 = { 0. };
            final double[] y = { 0. };
            integrator.integrate(ode, 0, y0, 100, y);
            // Exception is not expected
            Assert.assertFalse(expectException);
        } catch (final NumberIsTooSmallException e) {
            // Exception is expected
            Assert.assertTrue(expectException);
        }
    }

    private static class KeplerHandler implements StepHandler {
        public KeplerHandler(final TestProblem3 pb) {
            this.pb = pb;
        }

        @Override
        public void init(final double t0, final double[] y0, final double t) {
            this.nbSteps = 0;
            this.maxError = 0;
        }

        @Override
        public void
                handleStep(final StepInterpolator interpolator, final boolean isLast)
                                                                                     throws MaxCountExceededException {

            ++this.nbSteps;
            for (int a = 1; a < 10; ++a) {

                final double prev = interpolator.getPreviousTime();
                final double curr = interpolator.getCurrentTime();
                final double interp = ((10 - a) * prev + a * curr) / 10;
                interpolator.setInterpolatedTime(interp);

                final double[] interpolatedY = interpolator.getInterpolatedState();
                final double[] theoreticalY = this.pb.computeTheoreticalState(interpolator.getInterpolatedTime());
                final double dx = interpolatedY[0] - theoreticalY[0];
                final double dy = interpolatedY[1] - theoreticalY[1];
                final double error = dx * dx + dy * dy;
                if (error > this.maxError) {
                    this.maxError = error;
                }
            }
            if (isLast) {
                Assert.assertTrue(this.maxError < 2.4e-10);
                Assert.assertTrue(this.nbSteps < 150);
            }
        }

        private int nbSteps;
        private double maxError;
        private final TestProblem3 pb;
    }

    private static class VariableHandler implements StepHandler {
        public VariableHandler() {
            this.firstTime = true;
            this.minStep = 0;
            this.maxStep = 0;
        }

        @Override
        public void init(final double t0, final double[] y0, final double t) {
            this.firstTime = true;
            this.minStep = 0;
            this.maxStep = 0;
        }

        @Override
        public void handleStep(final StepInterpolator interpolator,
                               final boolean isLast) {

            final double step = MathLib.abs(interpolator.getCurrentTime()
                - interpolator.getPreviousTime());
            if (this.firstTime) {
                this.minStep = MathLib.abs(step);
                this.maxStep = this.minStep;
                this.firstTime = false;
            } else {
                if (step < this.minStep) {
                    this.minStep = step;
                }
                if (step > this.maxStep) {
                    this.maxStep = step;
                }
            }

            if (isLast) {
                Assert.assertTrue(this.minStep < (1.0 / 100.0));
                Assert.assertTrue(this.maxStep > (1.0 / 2.0));
            }
        }

        private boolean firstTime = true;
        private double minStep = 0;
        private double maxStep = 0;
    }

}
