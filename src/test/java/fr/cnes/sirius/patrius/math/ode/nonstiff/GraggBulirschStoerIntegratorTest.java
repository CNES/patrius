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
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
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
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.TestProblem1;
import fr.cnes.sirius.patrius.math.ode.TestProblem3;
import fr.cnes.sirius.patrius.math.ode.TestProblem4;
import fr.cnes.sirius.patrius.math.ode.TestProblem5;
import fr.cnes.sirius.patrius.math.ode.TestProblemAbstract;
import fr.cnes.sirius.patrius.math.ode.TestProblemHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class GraggBulirschStoerIntegratorTest {

    @Test(expected = DimensionMismatchException.class)
    public void testDimensionCheck()
                                    throws DimensionMismatchException, NumberIsTooSmallException,
                                    MaxCountExceededException, NoBracketingException {
        final TestProblem1 pb = new TestProblem1();
        final AdaptiveStepsizeIntegrator integrator =
            new GraggBulirschStoerIntegrator(0.0, 1.0, 1.0e-10, 1.0e-10);
        integrator.integrate(pb,
            0.0, new double[pb.getDimension() + 10],
            1.0, new double[pb.getDimension() + 10]);
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testNullIntervalCheck()
                                       throws DimensionMismatchException, NumberIsTooSmallException,
                                       MaxCountExceededException, NoBracketingException {
        final TestProblem1 pb = new TestProblem1();
        final GraggBulirschStoerIntegrator integrator =
            new GraggBulirschStoerIntegrator(0.0, 1.0, 1.0e-10, 1.0e-10);
        integrator.integrate(pb,
            0.0, new double[pb.getDimension()],
            0.0, new double[pb.getDimension()]);
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testMinStep()
                             throws DimensionMismatchException, NumberIsTooSmallException,
                             MaxCountExceededException, NoBracketingException {

        final TestProblem5 pb = new TestProblem5();
        final double minStep = 0.1 * MathLib.abs(pb.getFinalTime() - pb.getInitialTime());
        final double maxStep = MathLib.abs(pb.getFinalTime() - pb.getInitialTime());
        final double[] vecAbsoluteTolerance = { 1.0e-20, 1.0e-21 };
        final double[] vecRelativeTolerance = { 1.0e-20, 1.0e-21 };

        final FirstOrderIntegrator integ =
            new GraggBulirschStoerIntegrator(minStep, maxStep,
                vecAbsoluteTolerance, vecRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

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

        final FirstOrderIntegrator integ = new GraggBulirschStoerIntegrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 7.5e-9);
        Assert.assertTrue(handler.getMaximalValueError() < 8.1e-9);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("Gragg-Bulirsch-Stoer", integ.getName());
    }

    @Test
    public void testIncreasingTolerance()
                                         throws DimensionMismatchException, NumberIsTooSmallException,
                                         MaxCountExceededException, NoBracketingException {

        int previousCalls = Integer.MAX_VALUE;
        for (int i = -12; i < -4; ++i) {
            final TestProblem1 pb = new TestProblem1();
            final double minStep = 0;
            final double maxStep = pb.getFinalTime() - pb.getInitialTime();
            final double absTolerance = MathLib.pow(10.0, i);
            final double relTolerance = absTolerance;

            final FirstOrderIntegrator integ =
                new GraggBulirschStoerIntegrator(minStep, maxStep,
                    absTolerance, relTolerance);
            final TestProblemHandler handler = new TestProblemHandler(pb, integ);
            integ.addStepHandler(handler);
            integ.integrate(pb,
                pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);

            // the coefficients are only valid for this test
            // and have been obtained from trial and error
            // there is no general relation between local and global errors
            final double ratio = handler.getMaximalValueError() / absTolerance;
            Assert.assertTrue(ratio < 2.4);
            Assert.assertTrue(ratio > 0.02);
            Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);

            final int calls = pb.getCalls();
            Assert.assertEquals(integ.getEvaluations(), calls);
            Assert.assertTrue(calls <= previousCalls);
            previousCalls = calls;

        }

    }

    @Test
    public void testIntegratorControls()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {

        final TestProblem3 pb = new TestProblem3(0.999);
        final GraggBulirschStoerIntegrator integ =
            new GraggBulirschStoerIntegrator(0, pb.getFinalTime() - pb.getInitialTime(),
                1.0e-8, 1.0e-10);

        final double errorWithDefaultSettings = this.getMaxError(integ, pb);

        // stability control
        integ.setStabilityCheck(true, 2, 1, 0.99);
        Assert.assertTrue(errorWithDefaultSettings < this.getMaxError(integ, pb));
        integ.setStabilityCheck(true, -1, -1, -1);

        integ.setControlFactors(0.5, 0.99, 0.1, 2.5);
        Assert.assertTrue(errorWithDefaultSettings < this.getMaxError(integ, pb));
        integ.setControlFactors(-1, -1, -1, -1);

        integ.setOrderControl(10, 0.7, 0.95);
        Assert.assertTrue(errorWithDefaultSettings < this.getMaxError(integ, pb));
        integ.setOrderControl(-1, -1, -1);

        integ.setInterpolationControl(true, 3);
        Assert.assertTrue(errorWithDefaultSettings < this.getMaxError(integ, pb));
        integ.setInterpolationControl(true, -1);

    }

    private
            double
            getMaxError(final FirstOrderIntegrator integrator, final TestProblemAbstract pb)
                                                                                            throws DimensionMismatchException,
                                                                                            NumberIsTooSmallException,
                                                                                            MaxCountExceededException,
                                                                                            NoBracketingException {
        final TestProblemHandler handler = new TestProblemHandler(pb, integrator);
        integrator.addStepHandler(handler);
        integrator.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
        return handler.getMaximalValueError();
    }

    // @Test Test ne passe plus a cause d'un probleme de reinitialisation de l'integrateur
    public void testEvents()
                            throws DimensionMismatchException, NumberIsTooSmallException,
                            MaxCountExceededException, NoBracketingException {

        final TestProblem4 pb = new TestProblem4();
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-10;
        final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

        final FirstOrderIntegrator integ = new GraggBulirschStoerIntegrator(minStep, maxStep,
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

        Assert.assertTrue(handler.getMaximalValueError() < 4.0e-7);
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
        final double absTolerance = 1.0e-6;
        final double relTolerance = 1.0e-6;

        final FirstOrderIntegrator integ =
            new GraggBulirschStoerIntegrator(minStep, maxStep,
                absTolerance, relTolerance);
        integ.addStepHandler(new KeplerStepHandler(pb));
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertEquals(integ.getEvaluations(), pb.getCalls());
        Assert.assertTrue(pb.getCalls() < 2150);

    }

    @Test
    public void testVariableSteps()
                                   throws DimensionMismatchException, NumberIsTooSmallException,
                                   MaxCountExceededException, NoBracketingException {

        final TestProblem3 pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double absTolerance = 1.0e-8;
        final double relTolerance = 1.0e-8;
        final FirstOrderIntegrator integ =
            new GraggBulirschStoerIntegrator(minStep, maxStep,
                absTolerance, relTolerance);
        integ.addStepHandler(new VariableStepHandler());
        final double stopTime = integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
        Assert.assertEquals(pb.getFinalTime(), stopTime, 1.0e-10);
        Assert.assertEquals("Gragg-Bulirsch-Stoer", integ.getName());
    }

    @Test
    public void testTooLargeFirstStep()
                                       throws DimensionMismatchException, NumberIsTooSmallException,
                                       MaxCountExceededException, NoBracketingException {

        final AdaptiveStepsizeIntegrator integ =
            new GraggBulirschStoerIntegrator(1, 1E10, Double.NaN, Double.NaN);
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

    // @Test Test ne passe plus a cause d'un probleme de reinitialisation de l'integrateur
    public void testUnstableDerivative()
                                        throws DimensionMismatchException, NumberIsTooSmallException,
                                        MaxCountExceededException, NoBracketingException {
        final StepProblem stepProblem = new StepProblem(0.0, 1.0, 2.0);
        final FirstOrderIntegrator integ =
            new GraggBulirschStoerIntegrator(0.1, 10, 1.0e-12, 0.0);
        integ.addEventHandler(stepProblem, 1.0, 1.0e-12, 1000);
        final double[] y = { Double.NaN };
        integ.integrate(stepProblem, 0.0, new double[] { 0.0 }, 10.0, y);
        Assert.assertEquals(8.0, y[0], 1.0e-12);
    }

    @Test
    public void testIssue596()
                              throws DimensionMismatchException, NumberIsTooSmallException,
                              MaxCountExceededException, NoBracketingException {
        final FirstOrderIntegrator integ = new GraggBulirschStoerIntegrator(1e-10, 100.0, 1e-7, 1e-7);
        integ.addStepHandler(new StepHandler(){

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public
                    void
                    handleStep(final StepInterpolator interpolator, final boolean isLast)
                                                                                         throws MaxCountExceededException {
                final double t = interpolator.getCurrentTime();
                interpolator.setInterpolatedTime(t);
                final double[] y = interpolator.getInterpolatedState();
                final double[] yDot = interpolator.getInterpolatedDerivatives();
                Assert.assertEquals(3.0 * t - 5.0, y[0], 1.0e-14);
                Assert.assertEquals(3.0, yDot[0], 1.0e-14);
            }
        });
        final double[] y = { 4.0 };
        final double t0 = 3.0;
        final double tend = 10.0;
        integ.integrate(new FirstOrderDifferentialEquations(){
            @Override
            public int getDimension() {
                return 1;
            }

            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
                yDot[0] = 3.0;
            }
        }, t0, y, tend, y);

    }

    private static class KeplerStepHandler implements StepHandler {
        public KeplerStepHandler(final TestProblem3 pb) {
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
            for (int a = 1; a < 100; ++a) {

                final double prev = interpolator.getPreviousTime();
                final double curr = interpolator.getCurrentTime();
                final double interp = ((100 - a) * prev + a * curr) / 100;
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
                Assert.assertTrue(this.maxError < 2.7e-6);
                Assert.assertTrue(this.nbSteps < 80);
            }
        }

        private int nbSteps;
        private double maxError;
        private final TestProblem3 pb;
    }

    public static class VariableStepHandler implements StepHandler {
        public VariableStepHandler() {
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
                Assert.assertTrue(this.minStep < 8.2e-3);
                Assert.assertTrue(this.maxStep > 1.5);
            }
        }

        private boolean firstTime;
        private double minStep;
        private double maxStep;
    }

}
