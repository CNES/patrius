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
 * @history creation 21/01/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:592:07/04/2016: Javadoc improvement
 * VERSION::DM:684:27/03/2018:add 2nd order RK6 interpolator
 * VERSION::FA:1774:22/10/2018: Javadoc correction
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

/**
 * A 6th order Runge-Kutta Integrators
 * <p>
 * Implementation of a sixth order Runge-Kutta integrator for STELA.
 * <p>
 * Butcher array :
 * 
 * <pre>
 *     0  |     0        0        0        0        0        0        0
 *    1/3 |    1/3       0        0        0        0        0        0
 *    2/3 |     0       2/3       0        0        0        0        0
 *    1/3 |    1/12     1/3     -1/12      0        0        0        0
 *    5/6 |   25/48   -55/24    35/48    15/8       0        0        0
 *    1/6 |    3/20   -11/24    -1/8      1/2      1/10      0        0
 *     1  | -261/260   33/13    43/156 -118/39    32/195   80/39      0
 *        |----------------------------------------------------------------
 *        |   13/200     0      11/40    11/40     4/25     4/25    13/200
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * <b>Warning:</b> This interpolator currently performs a 2nd order interpolation issued from article <i>Dense output
 * for strong stability preserving Runge–Kutta methods, D. Ketcheson, 2016</i>. <br/>
 * Accuracy is however below 1m for standard timestep.
 * </p>
 * 
 * @see RungeKuttaIntegrator
 * 
 * @concurrency not thread-safe
 * 
 * @author Cedric Dental
 * 
 * @version 1.3
 * 
 * @since 1.3
 * 
 */
// CHECKSTYLE: stop MagicNumber check
public class RungeKutta6Integrator extends RungeKuttaIntegrator {

    /** Time steps Butcher array. */
    private static final double[] STATIC_C = { 1. / 3., 2. / 3., 1. / 3., 5. / 6., 1. / 6., 1.0 };

    /** Internal weights Butcher array. */
    private static final double[][] STATIC_A = { { 1. / 3. }, { 0., 2. / 3. }, { 1. / 12., 1. / 3., -1. / 12. },
        { 25. / 48., -55. / 24., 35. / 48., 15. / 8. }, { 3. / 20., -11. / 24., -1. / 8., 1. / 2., 1. / 10. },
        { -261. / 260., 33. / 13., 43. / 156., -118. / 39., 32. / 195., 80. / 39. } };

    /** Propagation weights Butcher array. */
    private static final double[] STATIC_B = { 13. / 200., 0., 11. / 40., 11. / 40., 4. / 25., 4. / 25., 13. / 200. };

    /**
     * Simple constructor.
     * Build a sixth-order Runge-Kutta integrator with the given
     * step.
     * 
     * @param step
     *        integration step
     */
    public RungeKutta6Integrator(final double step) {
        super("Runge-Kutta 6", STATIC_C, STATIC_A, STATIC_B,
            new RungeKutta6StepInterpolator(STATIC_B), step);
    }

    // CHECKSTYLE: resume MagicNumber check
}
