/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history created 13/12/2011
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Add shouldBeRemoved method
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.cnesmerge.ode.events;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.exception.MathInternalError;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventState;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DummyStepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;

/**
 * Unit tests for EventState.<br>
 * IMPORTANT : to be merged into commons-math eventually.
 * 
 * @author cardosop
 * 
 * @version $Id: EventStateTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.1
 * 
 */
public class EventStateTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the EventState class
         * 
         * @featureDescription Validation of the EventState class
         * 
         * @coveredRequirements DV-INTEG_70, DV-INTEG_140, DV-EVT_61, DV-EVT_62, DV-EVT_63
         */
        VALIDATION_EVENTSTATE
    }

    /**
     * Error string.
     */
    private static final String CVG_EXMSG = "ConvergenceException on call to evaluateStep()";

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#getMaxCheckInterval()}
     * @testedMethod {@link EventState#getConvergence()}
     * @testedMethod {@link EventState#getMaxIterationCount()}
     * @testedMethod {@link EventState#getEventHandler()}
     * 
     * @description unit test for simple getters in EventState
     * 
     * @input constructor input values and objects
     * 
     * @output getters' output
     * 
     * @testPassCriteria the getters' output match the constructor's input
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testSimpleGetters() {
        final EventHandler bogusEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return 1.;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
                // does nothing
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                // TODO Auto-generated method stub
                return false;
            }
        };
        final double maxChkInt = 20.;
        final double tolerance = 0.1;
        final int maxIter = 10;
        final EventState es = new EventState(bogusEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        Assert.assertEquals(maxChkInt, es.getMaxCheckInterval(), 0.);
        Assert.assertEquals(tolerance, es.getConvergence(), 0.);
        Assert.assertEquals(maxIter, es.getMaxIterationCount(), 0.);
        Assert.assertEquals(bogusEventHandler, es.getEventHandler());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#reinitializeBegin(StepInterpolator)}
     * 
     * @description unit test for the reinitializeBegin method
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria successful calls to the reinitializeBegin method (since calling reinitializeBegin does not
     *                   directly modify the output of the getters, there are no values to check on calls of
     *                   reinitializeBegin only.)
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testReinitializeBegin() {
        final double maxChkInt = 20.;
        final double tolerance = 0.1;
        final int maxIter = 10;
        final EventHandler bogusEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return 1.;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
                // does nothing
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                // TODO Auto-generated method stub
                return false;
            }
        };
        // 1) Simplest case : forward interpolator, no special case
        final StepInterpolator interpolator1 =
            new DummyStepInterpolator(new double[0], new double[0], true);
        final EventState es1 = new EventState(bogusEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        es1.reinitializeBegin(interpolator1);

        // 2) Backward interpolator
        final StepInterpolator interpolator2 =
            new DummyStepInterpolator(new double[0], new double[0], false);
        final EventState es2 = new EventState(bogusEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        es2.reinitializeBegin(interpolator2);

        // 3) Forward interpolator, special case : 0 at end of ignore zone
        final EventHandler onToleranceEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                // Zeroes on the end of the startup ignore zone
                return (t - tolerance);
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
                // does nothing
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                // TODO Auto-generated method stub
                return false;
            }
        };
        final DummyStepInterpolator interpolator3 =
            new DummyStepInterpolator(new double[0], new double[0], true);
        interpolator3.setSoftPreviousTime(0.);
        final EventState es3 = new EventState(onToleranceEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        es3.reinitializeBegin(interpolator3);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#evaluateStep(StepInterpolator)}
     * @testedMethod {@link EventState#getEventTime()}
     * @testedMethod {@link EventState#stepAccepted(double, double[])}
     * 
     * @description unit test for the evaluateStep method, mostly for code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria return values of evaluateStep as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    @Ignore
    public final void testEvaluateStep() {
        double maxChkInt = 20.;
        final double tolerance = 0.1;
        final int maxIter = 10;
        final EventHandler bogusEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return 1.;
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
                // does nothing
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                // TODO Auto-generated method stub
                return false;
            }
        };
        final EventHandler onToleranceEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                // Zeroes on the end of the startup ignore zone
                return (t - tolerance);
            }

            @Override
            public Action eventOccurred(final double t, final double[] y, final boolean increasing,
                                        final boolean forward) {
                return Action.CONTINUE;
            }

            @Override
            public void init(final double t0, final double[] y0, final double t) {
                // does nothing
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }

            @Override
            public boolean shouldBeRemoved() {
                // TODO Auto-generated method stub
                return false;
            }
        };

        // 1) Simple case : forward interpolator, no event found
        final DummyStepInterpolator interpolator1 =
            new DummyStepInterpolator(new double[0], new double[0], true);
        final EventState es1 = new EventState(bogusEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        try {
            final boolean rez = es1.evaluateStep(interpolator1);
            // No event found
            Assert.assertFalse(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }

        // 2) Backward interpolator, event found
        final DummyStepInterpolator interpolator2 =
            new DummyStepInterpolator(new double[0], new double[0], false);
        final EventState es2 = new EventState(onToleranceEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        try {
            interpolator2.setSoftPreviousTime(2.);
            interpolator2.setSoftCurrentTime(-2.);
            es2.reinitializeBegin(interpolator2);
            final boolean rez = es2.evaluateStep(interpolator2);
            // Event found
            Assert.assertTrue(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }

        // 3) Special case : step too small, no event found
        final DummyStepInterpolator interpolator3 =
            new DummyStepInterpolator(new double[0], new double[0], true);
        interpolator3.setSoftPreviousTime(-tolerance);
        final EventState es3 = new EventState(bogusEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        // Event state initialization
        es3.reinitializeBegin(interpolator3);

        interpolator3.setSoftCurrentTime(0.01);
        // Next step
        try {
            final boolean rez = es3.evaluateStep(interpolator3);
            // Step too small for anything to be found
            Assert.assertFalse(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }

        // 4) Error case : EventException in the eventHandler;
        // DOES NOT EXIST ANY MORE

        // 5) Special case : process the same event twice
        maxChkInt = 1.;
        final DummyStepInterpolator interpolator5 =
            new DummyStepInterpolator(new double[0], new double[0], true);
        interpolator5.setSoftPreviousTime(-tolerance);
        final EventState es5 = new EventState(onToleranceEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        // Event state initialization
        es5.reinitializeBegin(interpolator5);

        interpolator5.setSoftCurrentTime(1.);
        try {
            final boolean rez = es5.evaluateStep(interpolator5);
            Assert.assertTrue(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }
        // Accept the step
        es5.stepAccepted(es5.getEventTime(), interpolator5.getInterpolatedState());

        // Do it again, so that the same event is processed twice
        // at some point.
        interpolator5.setSoftPreviousTime(-tolerance);
        // Event state initialization
        es5.reinitializeBegin(interpolator5);

        interpolator5.setSoftCurrentTime(1.);
        try {
            final boolean rez = es5.evaluateStep(interpolator5);
            // Event already found before
            Assert.assertFalse(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }

        // 6) Special case : process the same event twice
        // with a rigged solver
        final RiggedBrentSolver riggedSolver = new RiggedBrentSolver(tolerance);
        final DummyStepInterpolator interpolator6 =
            new DummyStepInterpolator(new double[0], new double[0], true);
        interpolator6.setSoftPreviousTime(-tolerance);
        final EventState es6 = new EventState(onToleranceEventHandler, maxChkInt,
            tolerance, maxIter,
            riggedSolver);
        // Event state initialization
        es6.reinitializeBegin(interpolator6);

        interpolator6.setSoftCurrentTime(1.);
        try {
            // Rig the solver with an unrealistic root
            riggedSolver.rig(100.);
            final boolean rez = es6.evaluateStep(interpolator6);
            Assert.assertTrue(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }
        // Accept the step (with a rigged root)
        es6.stepAccepted(es6.getEventTime(), interpolator6.getInterpolatedState());

        // Do it again, so that the same event is processed twice
        // at some point.
        interpolator6.setSoftPreviousTime(-tolerance);
        // Event state initialization
        es6.reinitializeBegin(interpolator6);

        interpolator6.setSoftCurrentTime(1.);
        try {
            final boolean rez = es6.evaluateStep(interpolator6);
            // Event already found before (even if rigged...)
            Assert.assertFalse(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }

        // 7) Error case : process with a rigged solver
        // causing a MathUserException
        // DOES NOT EXIST ANY MORE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#evaluateStep(StepInterpolator)}
     * 
     * @description Unit test that covers a rare error case - only useful for code coverage.
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria a MathInternalError that should never happen happens anyway
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test(expected = MathInternalError.class)
    @Ignore
    public final void testEvaluateStepMathError() {
        final double maxChkInt = 0.1;
        final double tolerance = 0.05;
        final int maxIter = 10;
        // Special error case
        final EventHandler onToleranceEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                // Zeroes on the end of the startup ignore zone
                final double precub = (t - tolerance);
                return (precub);
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
            public void init(final double t0, final double[] y0, final double t) {
                // does nothing
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }
        };
        final DummyStepInterpolator interpol =
            new DummyStepInterpolator(new double[0], new double[0], true);
        interpol.setSoftPreviousTime(-tolerance);
        final EventState es = new EventState(onToleranceEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        // Event state initialization
        es.reinitializeBegin(interpol);

        interpol.setSoftCurrentTime(1.);
        try {
            es.evaluateStep(interpol);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }
        // Accept the step
        es.stepAccepted(es.getEventTime(), interpol.getInterpolatedState());
        // Do it again, so that the same event is processed twice
        // at some point.
        interpol.setSoftPreviousTime(-tolerance);
        // Event state initialization
        es.reinitializeBegin(interpol);
        interpol.setSoftCurrentTime(1.);
        try {
            es.evaluateStep(interpol);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_EVENTSTATE}
     * 
     * @testedMethod {@link EventState#reset(double, double[])}
     * 
     * @description unit test for the reset method, mostly for code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria return values of reset as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public final void testReset() {
        final double maxChkInt = 20.;
        final double tolerance = 0.1;
        final int maxIter = 10;
        final EventHandler simpleEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
            }

            @Override
            public double g(final double t, final double[] y) {
                return (t - tolerance);
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
            public void init(final double t0, final double[] y0, final double t) {
                // does nothing
            }

            @Override
            public int getSlopeSelection() {
                return 2;
            }
        };

        // Use a simple case to call reset
        final DummyStepInterpolator interpolator1 =
            new DummyStepInterpolator(new double[0], new double[0], true);
        final EventState es1 = new EventState(simpleEventHandler, maxChkInt,
            tolerance, maxIter,
            new BrentSolver(tolerance));
        // First call to reset (nothing to reset at this point)
        boolean rez;
        rez = es1.reset(0., new double[0]);
        Assert.assertFalse(rez);

        try {
            interpolator1.setSoftPreviousTime(-2.);
            interpolator1.setSoftCurrentTime(2.);
            es1.reinitializeBegin(interpolator1);
            rez = es1.evaluateStep(interpolator1);
            // Event found
            Assert.assertTrue(rez);
            // Accept the event
            es1.stepAccepted(es1.getEventTime(), interpolator1.getInterpolatedState());
            // Reset the event handler
            rez = es1.reset(es1.getEventTime(), new double[0]);
            // No derivatives to reset
            Assert.assertFalse(rez);
        } catch (final ConvergenceException e) {
            Assert.fail(CVG_EXMSG);
        }
    }

    /**
     * Implements a modified BrentSolver whose solver result can be modified at will.<br>
     * This acts in practice as a mock object.
     */
    private final class RiggedBrentSolver extends BrentSolver {

        /** True when rigged mode on. */
        private boolean rigged = false;
        /** Rigged solution value. */
        private double riggedRoot;

        /**
         * Constructor.
         * 
         * @param tol
         *        tolerance
         **/
        public RiggedBrentSolver(final double tol) {
            super(tol);
        }

        /**
         * Rig method.
         * 
         * @param rRoot
         *        the rigged solution
         */
        public void rig(final double rRoot) {
            this.riggedRoot = rRoot;
            this.rigged = true;
        }

        @Override
        public double solve(final int maxEval, final UnivariateFunction f, final double min, final double max) {
            double rez;
            if (this.rigged) {
                rez = this.riggedRoot;
            } else {
                rez = this.solve(maxEval, f, min, max, min + 0.5 * (max - min));
            }
            return rez;
        }
    }

}
