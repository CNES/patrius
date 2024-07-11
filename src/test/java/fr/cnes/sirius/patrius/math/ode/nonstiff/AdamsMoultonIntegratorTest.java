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
 * VERSION::FA:306:21/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.TestProblem1;
import fr.cnes.sirius.patrius.math.ode.TestProblem5;
import fr.cnes.sirius.patrius.math.ode.TestProblem6;
import fr.cnes.sirius.patrius.math.ode.TestProblemHandler;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AdamsMoultonIntegratorTest {

    /**
     * For coverage purposes, tests the if (nSteps < 2) in constructor of MultistepIntegrator
     */
    @Test(expected = NumberIsTooSmallException.class)
    public void testNStepsConstructor() {
        new AdamsMoultonIntegrator(1, 0.0, 1.0, 1.0e-10, 1.0e-10);
    }

    @Test(expected = DimensionMismatchException.class)
    public void dimensionCheck() {
        final TestProblem1 pb = new TestProblem1();
        final FirstOrderIntegrator integ =
            new AdamsMoultonIntegrator(2, 0.0, 1.0, 1.0e-10, 1.0e-10);
        integ.integrate(pb,
            0.0, new double[pb.getDimension() + 10],
            1.0, new double[pb.getDimension() + 10]);
    }

    @Test(expected = NumberIsTooSmallException.class)
    public void testMinStep() {

        final TestProblem1 pb = new TestProblem1();
        final double minStep = 0.1 * (pb.getFinalTime() - pb.getInitialTime());
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double[] vecAbsoluteTolerance = { 1.0e-15, 1.0e-16 };
        final double[] vecRelativeTolerance = { 1.0e-15, 1.0e-16 };

        final FirstOrderIntegrator integ = new AdamsMoultonIntegrator(4, minStep, maxStep,
            vecAbsoluteTolerance,
            vecRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

    }

    @Test
    public void testIncreasingTolerance() {

        int previousCalls = Integer.MAX_VALUE;
        for (int i = -12; i < -2; ++i) {
            final TestProblem1 pb = new TestProblem1();
            final double minStep = 0;
            final double maxStep = pb.getFinalTime() - pb.getInitialTime();
            final double scalAbsoluteTolerance = MathLib.pow(10.0, i);
            final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

            final FirstOrderIntegrator integ = new AdamsMoultonIntegrator(4, minStep, maxStep,
                scalAbsoluteTolerance,
                scalRelativeTolerance);
            final TestProblemHandler handler = new TestProblemHandler(pb, integ);
            integ.addStepHandler(handler);
            integ.integrate(pb,
                pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);

            // the 0.5 and 11.0 factors are only valid for this test
            // and has been obtained from trial and error
            // there is no general relation between local and global errors
            Assert.assertTrue(handler.getMaximalValueError() > (0.5 * scalAbsoluteTolerance));
            Assert.assertTrue(handler.getMaximalValueError() < (11.0 * scalAbsoluteTolerance));
            Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-16);

            final int calls = pb.getCalls();
            Assert.assertEquals(integ.getEvaluations(), calls);
            Assert.assertTrue(calls <= previousCalls);
            previousCalls = calls;

        }

    }

    @Test(expected = MaxCountExceededException.class)
    public void exceedMaxEvaluations() {

        final TestProblem1 pb = new TestProblem1();
        final double range = pb.getFinalTime() - pb.getInitialTime();

        final AdamsMoultonIntegrator integ = new AdamsMoultonIntegrator(2, 0, range, 1.0e-12, 1.0e-12);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.setMaxEvaluations(650);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

    }

    @Test
    public void backward() {

        final TestProblem5 pb = new TestProblem5();
        final double range = MathLib.abs(pb.getFinalTime() - pb.getInitialTime());

        final FirstOrderIntegrator integ = new AdamsMoultonIntegrator(4, 0, range, 1.0e-12, 1.0e-12);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 1.0e-9);
        Assert.assertTrue(handler.getMaximalValueError() < 1.0e-9);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-16);
        Assert.assertEquals("Adams-Moulton", integ.getName());
    }

    @Test
    public void polynomial() {
        final TestProblem6 pb = new TestProblem6();
        final double range = MathLib.abs(pb.getFinalTime() - pb.getInitialTime());

        for (int nSteps = 2; nSteps < 8; ++nSteps) {
            final AdamsMoultonIntegrator integ =
                new AdamsMoultonIntegrator(nSteps, 1.0e-6 * range, 0.1 * range, 1.0e-5, 1.0e-5);
            final TestProblemHandler handler = new TestProblemHandler(pb, integ);
            integ.addStepHandler(handler);
            integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);
            if (nSteps < 4) {
                Assert.assertTrue(handler.getMaximalValueError() > 7.0e-04);
            } else {
                Assert.assertTrue(handler.getMaximalValueError() < 3.0e-13);
            }
        }
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link AdamsMoultonIntegrator#AdamsMoultonIntegrator(int, double, double, double, double)}
     * @testedMethod {@link AdamsMoultonIntegrator#AdamsMoultonIntegrator(int, double, double[], double[], double)}
     * @testedMethod {@link AdamsMoultonIntegrator#AdamsMoultonIntegrator(int, double, double, double, double, boolean)}
     * @testedMethod {@link AdamsMoultonIntegrator#AdamsMoultonIntegrator(int, double, double[], double[], double, boolean)}
     * @testedMethod {@link AdamsMoultonIntegrator#integrate(ExpandableStatefulODE, double)}
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
        final int nSteps = 2;
        final double minStep = 1.0;
        final double maxStep = 10.;
        final double absTol = 1.e-6;
        final double relTol = 0.;
        final double[] absTolVec = { 1.e-6 };
        final double[] relTolVec = { 0. };

        final FirstOrderIntegrator integrator1 = new AdamsMoultonIntegrator(nSteps, minStep, maxStep,
            absTol, relTol);
        final FirstOrderIntegrator integrator2 = new AdamsMoultonIntegrator(nSteps, minStep, maxStep,
            absTol, relTol, false);
        final FirstOrderIntegrator integrator3 = new AdamsMoultonIntegrator(nSteps, minStep, maxStep,
            absTol, relTol, true);

        final FirstOrderIntegrator integrator4 = new AdamsMoultonIntegrator(nSteps, minStep, maxStep,
            absTolVec, relTolVec);
        final FirstOrderIntegrator integrator5 = new AdamsMoultonIntegrator(nSteps, minStep, maxStep,
            absTolVec, relTolVec, false);
        final FirstOrderIntegrator integrator6 = new AdamsMoultonIntegrator(nSteps, minStep, maxStep,
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
}
