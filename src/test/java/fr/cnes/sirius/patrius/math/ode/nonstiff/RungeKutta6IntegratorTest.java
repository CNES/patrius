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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:684:27/03/2018:add 2nd order RK6 interpolator
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler.Action;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * @description test class for RungeKutta6Integrator
 * 
 * @author Cedric Dental
 * 
 * @version $Id: RungeKutta6IntegratorTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.3
 * 
 */
public class RungeKutta6IntegratorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela Integrator
         * 
         * @featureDescription Adding 6th order Runge-Kutta Integrator
         * 
         * @coveredRequirements
         */
        RK6

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input ode, integrator
     * 
     * @output Missed End Event
     * 
     * @testPassCriteria Results according to CommonsMath
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMissedEndEvent() {
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

        final RungeKutta6Integrator integrator = new RungeKutta6Integrator(60.0);

        final double[] y0 = new double[k.length];
        for (int i = 0; i < y0.length; ++i) {
            y0[i] = i + 1;
        }
        final double[] y = new double[k.length];

        double finalT = integrator.integrate(ode, t0, y0, tEvent, y);
        Assert.assertEquals(tEvent, finalT, 5.0e-6);
        for (int i = 0; i < y.length; ++i) {
            Assert.assertEquals(y0[i] * MathLib.exp(k[i] * (finalT - t0)), y[i], 1.0e-9);
        }

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
            public boolean shouldBeRemoved() {
                return false;
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
        }, Double.POSITIVE_INFINITY, 1.0e-20, 100);
        finalT = integrator.integrate(ode, t0, y0, tEvent + 120, y);
        Assert.assertEquals(tEvent + 120, finalT, 5.0e-6);
        for (int i = 0; i < y.length; ++i) {
            Assert.assertEquals(y0[i] * MathLib.exp(k[i] * (finalT - t0)), y[i], 1.0e-9);
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input problem, integrator
     * 
     * @output exception
     * 
     * @testPassCriteria Results according to CommonsMath
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSanityChecks() {
        try {
            final TestProblem1 pb = new TestProblem1();
            new RungeKutta6Integrator(0.01).integrate(pb,
                0.0, new double[pb.getDimension() + 10],
                1.0, new double[pb.getDimension()]);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException ie) {
        }
        try {
            final TestProblem1 pb = new TestProblem1();
            new RungeKutta6Integrator(0.01).integrate(pb,
                0.0, new double[pb.getDimension()],
                1.0, new double[pb.getDimension() + 10]);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException ie) {
        }
        try {
            final TestProblem1 pb = new TestProblem1();
            new RungeKutta6Integrator(0.01).integrate(pb,
                0.0, new double[pb.getDimension()],
                0.0, new double[pb.getDimension()]);
            Assert.fail("an exception should have been thrown");
        } catch (final NumberIsTooSmallException ie) {
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input problem, integrator
     * 
     * @output Time of integration according to Stela RK6 integrator
     * 
     * @testPassCriteria Results according to CommonsMath
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testDecreasingSteps()
    {

        final TestProblemAbstract[] problems = TestProblemFactory.getProblems();
        for (final TestProblemAbstract problem : problems) {

            double previousValueError = Double.NaN;
            double previousTimeError = Double.NaN;
            for (int i = 4; i < 10; ++i) {

                final TestProblemAbstract pb = problem.copy();
                final double step = (pb.getFinalTime() - pb.getInitialTime()) * MathLib.pow(2.0, -i);

                final FirstOrderIntegrator integ = new RungeKutta6Integrator(step);
                final TestProblemHandler handler = new TestProblemHandler(pb, integ);
                integ.addStepHandler(handler);
                final EventHandler[] functions = pb.getEventsHandlers();
                for (final EventHandler function : functions) {
                    integ.addEventHandler(function,
                        Double.POSITIVE_INFINITY, 1.0e-6 * step, 1000);
                }
                Assert.assertEquals(functions.length, integ.getEventHandlers().size());
                final double stopTime = integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
                    pb.getFinalTime(), new double[pb.getDimension()]);
                if (functions.length == 0) {
                    Assert.assertEquals(pb.getFinalTime(), stopTime, 1.0e-10);
                }

                final double error = handler.getMaximalValueError();
                if (i > 4) {
                    Assert.assertTrue(error < 1.01 * MathLib.abs(previousValueError));
                }
                previousValueError = error;

                final double timeError = handler.getMaximalTimeError();
                if (i > 4) {
                    Assert.assertTrue(timeError <= MathLib.abs(previousTimeError));
                }
                previousTimeError = timeError;

                integ.clearEventHandlers();
                Assert.assertEquals(0, integ.getEventHandlers().size());
            }

        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input problem, integrator
     * 
     * @output Interpolation errors
     * 
     * @testPassCriteria Results according to CommonsMath and Stela RK6
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testSmallStep()
    {

        final TestProblem1 pb = new TestProblem1();
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.001;

        final FirstOrderIntegrator integ = new RungeKutta6Integrator(step);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 2.0e-16);
        Assert.assertTrue(handler.getMaximalValueError() < 2.0e-6);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("Runge-Kutta 6", integ.getName());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input problem, integrator
     * 
     * @output Interpolation errors
     * 
     * @testPassCriteria Results according to CommonsMath and Stela RK6
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testBigStep()
    {

        final TestProblem1 pb = new TestProblem1();
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.2;

        final FirstOrderIntegrator integ = new RungeKutta6Integrator(step);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() > 2.5e-5);
        Assert.assertTrue(handler.getMaximalValueError() > 0.009);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input problem, integrator
     * 
     * @output Interpolation errors
     * 
     * @testPassCriteria Results according to CommonsMath and Stela RK6
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testBackward()
    {

        final TestProblem5 pb = new TestProblem5();
        final double step = MathLib.abs(pb.getFinalTime() - pb.getInitialTime()) * 0.001;

        final FirstOrderIntegrator integ = new RungeKutta6Integrator(step);
        final TestProblemHandler handler = new TestProblemHandler(pb, integ);
        integ.addStepHandler(handler);
        integ.integrate(pb, pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);

        Assert.assertTrue(handler.getLastError() < 5.0e-10);
        Assert.assertTrue(handler.getMaximalValueError() < 1.1e-4);
        Assert.assertEquals(0, handler.getMaximalTimeError(), 1.0e-12);
        Assert.assertEquals("Runge-Kutta 6", integ.getName());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input problem, integrator
     * 
     * @output Interpolation errors
     * 
     * @testPassCriteria Results according to CommonsMath and Stela RK6
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testKepler()
    {

        final TestProblem3 pb = new TestProblem3(0.9);
        final double step = (pb.getFinalTime() - pb.getInitialTime()) * 0.0003;

        final FirstOrderIntegrator integ = new RungeKutta6Integrator(step);
        integ.addStepHandler(new KeplerHandler(pb));
        integ.integrate(pb,
            pb.getInitialTime(), pb.getInitialState(),
            pb.getFinalTime(), new double[pb.getDimension()]);
    }

    private static class KeplerHandler implements StepHandler {
        public KeplerHandler(final TestProblem3 pb) {
            this.pb = pb;
            this.maxError = 0;
        }

        @Override
        public void init(final double t0, final double[] y0, final double t) {
            this.maxError = 0;
        }

        @Override
        public void handleStep(final StepInterpolator interpolator, final boolean isLast) {

            final double[] interpolatedY = interpolator.getInterpolatedState();
            final double[] theoreticalY = this.pb.computeTheoreticalState(interpolator.getCurrentTime());
            final double dx = interpolatedY[0] - theoreticalY[0];
            final double dy = interpolatedY[1] - theoreticalY[1];
            final double error = dx * dx + dy * dy;
            if (error > this.maxError) {
                this.maxError = error;
            }
            if (isLast) {
                // even with more than 1000 evaluations per period,
                // RK6 is not able to integrate such an eccentric
                // orbit with a good accuracy
                Assert.assertTrue(this.maxError > 4e-8);
            }
        }

        private double maxError = 0;
        private final TestProblem3 pb;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link RungeKutta6Integrator#RungeKutta6Integrator(double)}
     * 
     * @description CommonsMath Generic test for Runge-Kutta Integrators, results are computed with Stela RK6
     *              integrator.
     * 
     * @input problem, integrator
     * 
     * @output Interpolation errors
     * 
     * @testPassCriteria Results according to CommonsMath
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testStepSize()
    {
        final double step = 1.23456;
        final FirstOrderIntegrator integ = new RungeKutta6Integrator(step);
        integ.addStepHandler(new StepHandler(){
            @Override
            public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
                if (!isLast) {
                    Assert.assertEquals(step,
                        interpolator.getCurrentTime() - interpolator.getPreviousTime(),
                        1.0e-12);
                }
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }
        });
        integ.integrate(new FirstOrderDifferentialEquations(){
            @Override
            public void computeDerivatives(final double t, final double[] y, final double[] dot) {
                dot[0] = 1.0;
            }

            @Override
            public int getDimension() {
                return 1;
            }
        }, 0.0, new double[] { 0.0 }, 5.0, new double[1]);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RK6}
     * 
     * @testedMethod {@link AbstractIntegrator}
     * 
     * @description coverage test
     * 
     * @input an EventHandler, an integrator
     * 
     * @output
     * 
     * @testPassCriteria Verify that the returned Action.REMOVE_DETECTOR is taken into account.
     * 
     * @comments Test for coverage purpose only. See propagator tests.
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public final void testRemoveDetector() {
        final double t0 = 1878250320.0000029;

        /**
         * Custom detector.
         */
        class Detector implements EventHandler {
            private int count = 0;
            private final Action action;
            private boolean shouldBeRemoved = false;

            public Detector(final Action act, final boolean remove) {
                this.action = act;
                this.shouldBeRemoved = remove;
            }

            public int getCount() {
                return this.count;
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return MathLib.sin(t);
            }

            @Override
            public boolean shouldBeRemoved() {
                return this.shouldBeRemoved;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                this.count++;
                return this.action;
            }

            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }
        }

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

        final RungeKutta6Integrator integrator1 = new RungeKutta6Integrator(60.0);
        final Detector detector1 = new Detector(Action.CONTINUE, false);
        integrator1.addEventHandler(detector1, Double.POSITIVE_INFINITY, 1.0e-20, 100);
        final double[] y0 = new double[k.length];
        for (int i = 0; i < y0.length; ++i) {
            y0[i] = i + 1;
        }
        final double[] y = new double[k.length];

        integrator1.integrate(ode, t0, y0, t0 + 300, y);
        Assert.assertNotSame(1, detector1.getCount());

        final RungeKutta6Integrator integrator2 = new RungeKutta6Integrator(60.0);
        final Detector detector2 = new Detector(Action.CONTINUE, true);
        integrator2.addEventHandler(detector2, Double.POSITIVE_INFINITY, 1.0e-20, 100);

        integrator2.integrate(ode, t0, y0, t0 + 300, y);
        Assert.assertEquals(1, detector2.getCount());
    }

}
