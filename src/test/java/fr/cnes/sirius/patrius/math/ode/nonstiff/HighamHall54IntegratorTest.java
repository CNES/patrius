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
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
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
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.ode.ExpandableStatefulODE;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.TestProblem1;
import fr.cnes.sirius.patrius.math.ode.TestProblem3;
import fr.cnes.sirius.patrius.math.ode.TestProblem4;
import fr.cnes.sirius.patrius.math.ode.TestProblem5;
import fr.cnes.sirius.patrius.math.ode.TestProblemHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class HighamHall54IntegratorTest {

    @Test
    public void testWrongDerivative()
                                     throws DimensionMismatchException, NumberIsTooSmallException,
                                     MaxCountExceededException, NoBracketingException {
        final HighamHall54Integrator integrator =
            new HighamHall54Integrator(0.0, 1.0, 1.0e-10, 1.0e-10);
        final FirstOrderDifferentialEquations equations =
            new FirstOrderDifferentialEquations(){
                @Override
                public void computeDerivatives(final double t, final double[] y, final double[] dot) {
                    if (t < -0.5) {
                        throw new LocalException();
                    } else {
                        throw new RuntimeException("oops");
                    }
                }

                @Override
                public int getDimension() {
                    return 1;
                }
            };

        try {
            integrator.integrate(equations, -1.0, new double[1], 0.0, new double[1]);
            Assert.fail("an exception should have been thrown");
        } catch (final LocalException de) {
            // expected behavior
        }

        try {
            integrator.integrate(equations, 0.0, new double[1], 1.0, new double[1]);
            Assert.fail("an exception should have been thrown");
        } catch (final RuntimeException de) {
            // expected behavior
        }

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

        final FirstOrderIntegrator integ = new HighamHall54Integrator(minStep, maxStep,
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
        for (int i = -12; i < -2; ++i) {
            final TestProblem1 pb = new TestProblem1();
            final double minStep = 0;
            final double maxStep = pb.getFinalTime() - pb.getInitialTime();
            final double scalAbsoluteTolerance = MathLib.pow(10.0, i);
            final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

            final FirstOrderIntegrator integ = new HighamHall54Integrator(minStep, maxStep,
                scalAbsoluteTolerance,
                scalRelativeTolerance);
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
    public void testBackward()
                              throws DimensionMismatchException, NumberIsTooSmallException,
                              MaxCountExceededException, NoBracketingException {

        final TestProblem5 pb = new TestProblem5();
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

        final FirstOrderIntegrator integ = new HighamHall54Integrator(minStep, maxStep,
            scalAbsoluteTolerance,
            scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 5.0e-7);
        Assert.assertTrue(handler.getMaximalValueError() < 5.0e-7);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("Higham-Hall 5(4)", integ.getName());
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

        final FirstOrderIntegrator integ = new HighamHall54Integrator(minStep, maxStep,
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

        Assert.assertTrue(handler.getMaximalValueError() < 1.0e-7);
        Assert.assertEquals(0, handler.getMaximalTimeError(), convergence);
        Assert.assertEquals(12.0, handler.getLastTime(), convergence);
        integ.clearEventHandlers();
        Assert.assertEquals(0, integ.getEventHandlers().size());

    }

    @Test(expected = LocalException.class)
    public void testEventsErrors()
                                  throws DimensionMismatchException, NumberIsTooSmallException,
                                  MaxCountExceededException, NoBracketingException {

        final TestProblem1 pb = new TestProblem1();
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

        final FirstOrderIntegrator integ =
            new HighamHall54Integrator(minStep, maxStep,
                scalAbsoluteTolerance, scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);

        integ.addEventHandler(new EventHandler(){
            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public double g(final double t, final double[] y) {
                final double middle = (pb.getInitialTime() + pb.getFinalTime()) / 2;
                final double offset = t - middle;
                if (offset > 0) {
                    throw new LocalException();
                }
                return offset;
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public boolean filterEvent(final double t,
                    final double[] y,
                    final boolean increasing,
                    final boolean forward) {
                return false;
            }
        }, Double.POSITIVE_INFINITY, 1.0e-8 * maxStep, 1000);

        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

    }

    private static class LocalException extends RuntimeException {
        private static final long serialVersionUID = 3041292643919807960L;
    }

    @Test
    public void testEventsNoConvergence()
                                         throws DimensionMismatchException, NumberIsTooSmallException,
                                         MaxCountExceededException, NoBracketingException {

        final TestProblem1 pb = new TestProblem1();
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double scalAbsoluteTolerance = 1.0e-8;
        final double scalRelativeTolerance = 0.01 * scalAbsoluteTolerance;

        final FirstOrderIntegrator integ =
            new HighamHall54Integrator(minStep, maxStep,
                scalAbsoluteTolerance, scalRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);

        integ.addEventHandler(new EventHandler(){
            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public double g(final double t, final double[] y) {
                final double middle = (pb.getInitialTime() + pb.getFinalTime()) / 2;
                final double offset = t - middle;
                return (offset > 0) ? (offset + 0.5) : (offset - 0.5);
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            /** {@inheritDoc} */
            @Override
            public boolean filterEvent(final double t,
                    final double[] y,
                    final boolean increasing,
                    final boolean forward) {
                return false;
            }
        }, Double.POSITIVE_INFINITY, 1.0e-8 * maxStep, 3);

        try {
            integ.integrate(pb,
                pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);
            Assert.fail("an exception should have been thrown");
        } catch (final TooManyEvaluationsException tmee) {
            // Expected.
        }

    }

    @Test
    public void testSanityChecks()
                                  throws DimensionMismatchException, NumberIsTooSmallException,
                                  MaxCountExceededException, NoBracketingException {
        final TestProblem3 pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();

        try {
            final FirstOrderIntegrator integ =
                new HighamHall54Integrator(minStep, maxStep, new double[4], new double[4]);
            integ.integrate(pb, pb.getInitialTime(), new double[6],
                pb.getFinalTime(), new double[pb.getDimension()]);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException ie) {
            // expected behavior
        }

        try {
            final FirstOrderIntegrator integ =
                new HighamHall54Integrator(minStep, maxStep, new double[4], new double[4]);
            integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[6]);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException ie) {
            // expected behavior
        }

        try {
            final FirstOrderIntegrator integ =
                new HighamHall54Integrator(minStep, maxStep, new double[2], new double[4]);
            integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException ie) {
            // expected behavior
        }

        try {
            final FirstOrderIntegrator integ =
                new HighamHall54Integrator(minStep, maxStep, new double[4], new double[2]);
            integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
                pb.getFinalTime(), new double[pb.getDimension()]);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException ie) {
            // expected behavior
        }

        try {
            final FirstOrderIntegrator integ =
                new HighamHall54Integrator(minStep, maxStep, new double[4], new double[4]);
            integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
                pb.getInitialTime(), new double[pb.getDimension()]);
            Assert.fail("an exception should have been thrown");
        } catch (final NumberIsTooSmallException ie) {
            // expected behavior
        }

    }

    @Test
    public void testKepler()
                            throws DimensionMismatchException, NumberIsTooSmallException,
                            MaxCountExceededException, NoBracketingException {

        final TestProblem3 pb = new TestProblem3(0.9);
        final double minStep = 0;
        final double maxStep = pb.getFinalTime() - pb.getInitialTime();
        final double[] vecAbsoluteTolerance = { 1.0e-8, 1.0e-8, 1.0e-10, 1.0e-10 };
        final double[] vecRelativeTolerance = { 1.0e-10, 1.0e-10, 1.0e-8, 1.0e-8 };

        final FirstOrderIntegrator integ = new HighamHall54Integrator(minStep, maxStep,
            vecAbsoluteTolerance,
            vecRelativeTolerance);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
        Assert.assertEquals(0.0, handler.getMaximalValueError(), 1.5e-4);
        Assert.assertEquals("Higham-Hall 5(4)", integ.getName());
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link HighamHall54Integrator#HighamHall54Integrator(double, double, double, double)}
     * @testedMethod {@link HighamHall54Integrator#HighamHall54Integrator(double, double[], double[], double)}
     * @testedMethod {@link HighamHall54Integrator#HighamHall54Integrator(double, double, double, double, boolean)}
     * @testedMethod {@link HighamHall54Integrator#HighamHall54Integrator(double, double[], double[], double, boolean)}
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

        final FirstOrderIntegrator integrator1 = new HighamHall54Integrator(minStep, maxStep,
            absTol, relTol);
        final FirstOrderIntegrator integrator2 = new HighamHall54Integrator(minStep, maxStep,
            absTol, relTol, false);
        final FirstOrderIntegrator integrator3 = new HighamHall54Integrator(minStep, maxStep,
            absTol, relTol, true);

        final FirstOrderIntegrator integrator4 = new HighamHall54Integrator(minStep, maxStep,
            absTolVec, relTolVec);
        final FirstOrderIntegrator integrator5 = new HighamHall54Integrator(minStep, maxStep,
            absTolVec, relTolVec, false);
        final FirstOrderIntegrator integrator6 = new HighamHall54Integrator(minStep, maxStep,
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
