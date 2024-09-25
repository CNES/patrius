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
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
import fr.cnes.sirius.patrius.math.ode.TestProblemAbstract;
import fr.cnes.sirius.patrius.math.ode.TestProblemHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DormandPrince54IntegratorTest {

    @Test(expected = DimensionMismatchException.class)
    public void testDimensionCheck()
                                    throws DimensionMismatchException, NumberIsTooSmallException,
                                    MaxCountExceededException, NoBracketingException {
        final TestProblem1 pb = new TestProblem1();
        final DormandPrince54Integrator integrator = new DormandPrince54Integrator(0.0, 1.0,
            1.0e-10, 1.0e-10);
        integrator.integrate(pb,
            0.0, new double[pb.getDimension() + 10],
            1.0, new double[pb.getDimension() + 10]);
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

        final FirstOrderIntegrator integ = new DormandPrince54Integrator(minStep, maxStep,
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
    public void testSmallLastStep()
                                   throws DimensionMismatchException, NumberIsTooSmallException,
                                   MaxCountExceededException, NoBracketingException {

        final TestProblemAbstract pb = new TestProblem5();
        final double minStep = 1.25;
        final double maxStep = MathLib.abs(pb.getFinalTime() - pb.getInitialTime());
        final double scalAbsoluteTolerance = 6.0e-4;
        final double scalRelativeTolerance = 6.0e-4;

        final AdaptiveStepsizeIntegrator integ =
            new DormandPrince54Integrator(minStep, maxStep,
                scalAbsoluteTolerance,
                scalRelativeTolerance);

        final DP54SmallLastHandler handler = new DP54SmallLastHandler(minStep);
        integ.addStepHandler(handler);
        integ.setInitialStepSize(1.7);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
        Assert.assertTrue(handler.wasLastSeen());
        Assert.assertEquals("Dormand-Prince 5(4)", integ.getName());

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

        final FirstOrderIntegrator integ = new DormandPrince54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 2.0e-7);
        Assert.assertTrue(handler.getMaximalValueError() < 2.0e-7);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("Dormand-Prince 5(4)", integ.getName());
    }

    private static class DP54SmallLastHandler implements StepHandler {

        public DP54SmallLastHandler(final double minStep) {
            this.lastSeen = false;
            this.minStep = minStep;
        }

        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }

        @Override
        public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
            if (isLast) {
                this.lastSeen = true;
                final double h = interpolator.getCurrentTime() - interpolator.getPreviousTime();
                Assert.assertTrue(MathLib.abs(h) < this.minStep);
            }
        }

        public boolean wasLastSeen() {
            return this.lastSeen;
        }

        private boolean lastSeen;
        private final double minStep;

    }

    @Test
    public void testIncreasingTolerance()
                                         throws DimensionMismatchException, NumberIsTooSmallException,
                                         MaxCountExceededException, NoBracketingException {

        int previousCalls = Integer.MAX_VALUE;
        for (int i = -12; i < -2; ++i) {
            final TestProblem1 pb = new TestProblem1();
            final double minStep = 0;
            final double maxStep = pb.getFinalTime() - pb.getInitialTime();
            final double scalAbsoluteTolerance = MathLib.pow(10.0, i);
            final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

            final EmbeddedRungeKuttaIntegrator integ =
                new DormandPrince54Integrator(minStep, maxStep,
                    scalAbsoluteTolerance, scalRelativeTolerance);
            final TestProblemHandler handler = new TestProblemHandler(pb, integ);
            integ.setSafety(0.8);
            integ.setMaxGrowth(5.0);
            integ.setMinReduction(0.3);
            integ.addStepHandler(handler);
            integ.integrate(pb,
                pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);
            Assert.assertEquals(0.8, integ.getSafety(), 1.0e-12);
            Assert.assertEquals(5.0, integ.getMaxGrowth(), 1.0e-12);
            Assert.assertEquals(0.3, integ.getMinReduction(), 1.0e-12);

            // the 0.7 factor is only valid for this test
            // and has been obtained from trial and error
            // there is no general relation between local and global errors
            Assert.assertTrue(handler.getMaximalValueError() < (0.7 * scalAbsoluteTolerance));
            Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);

            final int calls = pb.getCalls();
            Assert.assertEquals(integ.getEvaluations(), calls);
            Assert.assertTrue(calls <= previousCalls);
            previousCalls = calls;

        }

    }

    @Test
    public void testEvents()
                            throws DimensionMismatchException, NumberIsTooSmallException,
                            MaxCountExceededException, NoBracketingException {

        final TestProblem4 pb = new TestProblem4();
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

        final FirstOrderIntegrator integ = new DormandPrince54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        final EventHandler[] functions = pb.getEventsHandlers();
        final double convergence = 1.0e-8 * maxStep;
        for (final EventHandler function : functions) {
            integ.addEventHandler(function,
                Double.POSITIVE_INFINITY, convergence, 1000);
        }
        Assert.assertEquals(functions.length, integ.getEventHandlers().size());
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getMaximalValueError() < 5.0e-6);
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

        final FirstOrderIntegrator integ = new DormandPrince54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        integ.addStepHandler(new KeplerHandler(pb));
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertEquals(integ.getEvaluations(), pb.getCalls());
        Assert.assertTrue(pb.getCalls() < 2800);

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

        final FirstOrderIntegrator integ = new DormandPrince54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        integ.addStepHandler(new VariableHandler());
        final double stopTime = integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
        Assert.assertEquals(pb.getFinalTime(), stopTime, 1.0e-10);
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link DormandPrince54Integrator#DormandPrince54Integrator(double, double, double, double)}
     * @testedMethod {@link DormandPrince54Integrator#DormandPrince54Integrator(double, double[], double[], double)}
     * @testedMethod {@link DormandPrince54Integrator#DormandPrince54Integrator(double, double, double, double, boolean)}
     * @testedMethod {@link DormandPrince54Integrator#DormandPrince54Integrator(double, double[], double[], double, boolean)}
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

        final FirstOrderIntegrator integrator1 = new DormandPrince54Integrator(minStep, maxStep,
            absTol, relTol);
        final FirstOrderIntegrator integrator2 = new DormandPrince54Integrator(minStep, maxStep,
            absTol, relTol, false);
        final FirstOrderIntegrator integrator3 = new DormandPrince54Integrator(minStep, maxStep,
            absTol, relTol, true);

        final FirstOrderIntegrator integrator4 = new DormandPrince54Integrator(minStep, maxStep,
            absTolVec, relTolVec);
        final FirstOrderIntegrator integrator5 = new DormandPrince54Integrator(minStep, maxStep,
            absTolVec, relTolVec, false);
        final FirstOrderIntegrator integrator6 = new DormandPrince54Integrator(minStep, maxStep,
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
                Assert.assertTrue(this.maxError < 7.0e-10);
                Assert.assertTrue(this.nbSteps < 400);
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
                Assert.assertTrue(this.minStep < (1.0 / 450.0));
                Assert.assertTrue(this.maxStep > (1.0 / 4.2));
            }
        }

        private boolean firstTime;
        private double minStep;
        private double maxStep;
    }

}
