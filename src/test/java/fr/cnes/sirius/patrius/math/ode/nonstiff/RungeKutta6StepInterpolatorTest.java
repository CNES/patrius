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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.math.util.FastMath;

/**
 * @description test class for RungeKutta6StepInterpolator
 * 
 * @author Cedric Dental
 * 
 * @version $Id: RungeKutta6StepInterpolatorTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.3
 * 
 */
public class RungeKutta6StepInterpolatorTest {

    private static int ji = 0;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela Interpolator
         * 
         * @featureDescription Adding 6th order Runge-Kutta Step Interpolator
         * 
         * @coveredRequirements TODO
         */
        INTERRK6

    }

    private static class CircleDiffEq implements FirstOrderDifferentialEquations {

        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            // Circle equation
            yDot[0] = -y[1];
            yDot[1] = y[0];
        }

        @Override
        public int getDimension() {
            return 2;
        }
    }

    private static class CircleStepHandler implements StepHandler {

        @Override
        public void handleStep(final StepInterpolator interpolator, final boolean isLast) {

            final double[][] RESULTS = { { 0.1963495408493621, 0.9807852803484617, 0.19509032945638108 },
                { 0.1963495408493621, 0.9807852803484617, 0.19509032945638108 } };
            final double[][] RESULTS_THETA = { { 0.9807852803484617, 0.19509032945638108 },
                { 0.9807852803484617, 0.19509032945638108 } };
            final double startT = interpolator.getPreviousTime();
            // final double[] endState = interpolator.getInterpolatedState();
            final double endT = interpolator.getCurrentTime();
            ji++;
            if (ji == 1) {
                for (int i = 0; i < 2; i++) {

                    Assert.assertEquals(RESULTS[i][0], interpolator.getInterpolatedTime(), 1e-15);
                    Assert.assertEquals(RESULTS[i][1], interpolator.getInterpolatedState()[0], 1e-15);
                    Assert.assertEquals(RESULTS[i][2], interpolator.getInterpolatedState()[1], 1e-15);

                    final double theta = 1.5;
                    final double oneMinusThetaH = 1 - theta;
                    ((RungeKutta6StepInterpolator) interpolator).computeInterpolatedStateAndDerivatives(theta,
                        oneMinusThetaH);

                    Assert.assertEquals(RESULTS_THETA[i][0], interpolator.getInterpolatedState()[0], 1e-15);
                    Assert.assertEquals(RESULTS_THETA[i][1], interpolator.getInterpolatedState()[1], 1e-15);
                }

            }
            // System.out.println("STEP "+endT+" "+endState[0]+" "+endState[1]);
        }

        /** {@inheritDoc} */

        @Override
        public void init(final double t0, final double[] y0, final double t) {
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERRK6}
     * 
     * @testedMethod {@link RungeKutta6StepInterpolator#RungeKutta6StepInterpolator()}
     * 
     * @description Test on the Linear Step Interpolator.
     * 
     * @input circleEq, circleSH, integrator, interpolator
     * 
     * @output Interpolated Time and State
     * 
     * @testPassCriteria Results according to Stela RK6 Linear Step Interpolator
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testInterpolator()
    {

        // definition of the integrator
        final double integrationStep = FastMath.PI / 16.;
        final RungeKutta6Integrator integrator = new RungeKutta6Integrator(integrationStep);
        final FirstOrderDifferentialEquations circleEq = new CircleDiffEq();

        // StepHandler
        final StepHandler circleSH = new CircleStepHandler();
        integrator.addStepHandler(circleSH);

        // Inputs
        final double startTime = 0.;
        final double[] initialState = { 1., 0 };
        final double endTime = 2 * FastMath.PI;
        final double[] finalState = new double[2];

        integrator.integrate(circleEq, startTime, initialState, endTime, finalState);

    }

}
