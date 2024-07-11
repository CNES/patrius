/**
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * HISTORY
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.functions;


/**
 * Interface for the barrier function used by a given barrier optimization method.
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.2"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 *
 * @since 4.6
 */
public interface BarrierFunction extends TwiceDifferentiableMultivariateRealFunction {

    /**
     * Calculates the duality gap for a barrier method build with this barrier function.
     * @param t value
     * @return duality gap
     */
    public double getDualityGap(final double t);

    /**
     * Create the barrier function for the basic Phase I method.
     * @return phase 1 barrier function
     * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.4.1"
     */
    public BarrierFunction createPhase1BarrierFunction();

    /**
     * Calculates the initial value for the additional variable s in basic Phase I method.
     * @param originalNotFeasiblePoint initial point (not-feasible)
     * @param tolerance tolerance
     * @return phase 1 initial feasible point
     * @see "S.Boyd and L.Vandenberghe, Convex Optimization, 11.4.1"
     */
    public double calculatePhase1InitialFeasiblePoint(final double[] originalNotFeasiblePoint,
            final double tolerance);
}
