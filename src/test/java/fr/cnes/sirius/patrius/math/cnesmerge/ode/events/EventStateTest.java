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
 * @history created 13/12/2011
 *
 * HISTORY
 * VERSION:4.13:FA:FA-79:08/12/2023:[PATRIUS] Probleme dans la fonction g de LocalTimeAngleDetector
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Add shouldBeRemoved method
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.cnesmerge.ode.events;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.BrentSolver;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.events.EventState;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DummyStepInterpolator;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

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
    public void testSimpleGetters() {
        final EventHandler bogusEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
                // nothing to do
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
    public void testReinitializeBegin() {
        final double maxChkInt = 20.;
        final double tolerance = 0.1;
        final int maxIter = 10;
        final EventHandler bogusEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
                // nothing to do
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
                // nothing to do
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
    public void testReset() {
        final double maxChkInt = 20.;
        final double tolerance = 0.1;
        final int maxIter = 10;
        final EventHandler simpleEventHandler = new EventHandler(){
            @Override
            public void resetState(final double t, final double[] y) {
                // nothing to do
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

            /** {@inheritDoc} */
            @Override
            public boolean filterEvent(final double t,
                    final double[] y,
                    final boolean increasing,
                    final boolean forward) {
                return false;
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

        /** Serializable UID. */
        private static final long serialVersionUID = -252317700981290335L;
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
