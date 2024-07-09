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
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

/**
 * Optimization process output: stores the solution as well as an exit code.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * 
 * @since 4.6
 */
public class OptimizationResponse {

    /** Succes variable */
    public static final int SUCCESS = 0;
    /** Warn variable */
    public static final int WARN = 1;
    /** Failed variable */
    public static final int FAILED = 2;

    /**
     * The optimization return code. In the case of WARN, you are given a result
     * by the optimizer but you must manually check if this is appropriate for
     * you (i.e. you have to manually check if constraints are satisfied within
     * an acceptable tolerance). It can happen, for example, when the algorithm
     * exceeds the available number of iterations.
     */
    private int returnCode;

    /** solution */
    private double[] solution;

    /** Lagrangian multipliers. */
    private double[] multiplicators;

    /**
     * Set the return code (succes, warn or failed)
     * 
     * @param code return code
     */
    public void setReturnCode(final int code) {
        this.returnCode = code;
    }

    /**
     * Get the return code
     * 
     * @return 0, 1 or 2
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * Set the solution
     * 
     * @param sol solution
     */
    public void setSolution(final double[] sol) {
        this.solution = sol.clone();
    }

    /**
     * Get the solution
     * 
     * @return solution
     */
    public double[] getSolution() {
        return solution.clone();
    }

    /**
     * Set the Lagrangian multipliers.
     * 
     * @param multiplicators Lagrangian multipliers
     */
    public void setMultiplicators(final double[] multiplicators) {
        this.multiplicators = multiplicators.clone();
    }

    /**
     * Returns the Lagrangian multipliers.
     * @return the Lagrangian multipliers
     */
    public double[] getMultiplicators() {
        return multiplicators.clone();
    }
}
