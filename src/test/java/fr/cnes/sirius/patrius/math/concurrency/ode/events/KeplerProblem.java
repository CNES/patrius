/**
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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.concurrency.ode.events;

import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Kepler problem class test.
 */
public class KeplerProblem implements FirstOrderDifferentialEquations {

    /** Earth gravitational constant. */
    private static double mu = 0.39860043770442E+15;

    /**
     * Complete constructor
     * 
     * @since 1.1
     */
    public KeplerProblem() {

    }

    /**
     * @description give the dimension of the problem
     * @return dimension of the problem
     * @sorg.apache.commons.math.th3.ode.FirstOrderDifferentialEquations#getDimension()
     */
    @Override
    public final int getDimension() {
        return 6;
    }

    /**
     * @description Get the current time derivative of the state vector.
     * @param t
     *        current value of the independent <I>time</I> variable
     * @param y
     *        array containing the current value of the state vector
     * @param yDot
     *        placeholder array where to put the time derivative of the state vector
     * @org.apache.commons.math.ath3.ode.FirstOrderDifferentialEquations#computeDerivatives(double, double[], double[])
     */
    @Override
    public final void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        yDot[0] = y[3];
        yDot[1] = y[4];
        yDot[2] = y[5];
        yDot[3] = -mu * y[0] / MathLib.pow(y[0] * y[0] + y[1] * y[1] + y[2] * y[2], 1.5);
        yDot[4] = -mu * y[1] / MathLib.pow(y[0] * y[0] + y[1] * y[1] + y[2] * y[2], 1.5);
        yDot[5] = -mu * y[2] / MathLib.pow(y[0] * y[0] + y[1] * y[1] + y[2] * y[2], 1.5);

    }

}
