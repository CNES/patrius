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
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;


/**
 * Convex Optimizer.
 * 
 * The algorithm selection is implemented as a Chain of Responsibility pattern,
 * and this class is the client of the chain.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization"
 * @author <a href="mailto:alberto.trivellato@gmail.com">alberto trivellato</a>
 * 
 * @since 4.6
 */
public final class JOptimizer {
    
    /** Default max number of iterations */
    public static final int DEFAULT_MAX_ITERATION = 500;
    /** Default feasibility tolerance */
    public static final double DEFAULT_FEASIBILITY_TOLERANCE = 1.E-6;
    /** Default tolerance */
    public static final double DEFAULT_TOLERANCE = 1.E-5;
    /** Default tolerance for inner step */
    public static final double DEFAULT_TOLERANCE_INNER_STEP = 1.E-5;
    /** Default ktt tolerance */
    public static final double DEFAULT_KKT_TOLERANCE = 1.E-9;
    /** Default alpha */
    public static final double DEFAULT_ALPHA = 0.055;
    /** Default beta */
    public static final double DEFAULT_BETA = 0.55;
    /** Default mu */
    public static final double DEFAULT_MU = 10;
    /** Barrier method string*/
    public static final String BARRIER_METHOD = "BARRIER_METHOD";
    /** Primal dual method string */
    public static final String PRIMAL_DUAL_METHOD = "PRIMAL_DUAL_METHOD";
    /** Default interior point method string */
    public static final String DEFAULT_INTERIOR_POINT_METHOD = PRIMAL_DUAL_METHOD;
    
    /** Optimization request */
    private OptimizationRequest request = null;
    /** Optimization response */
    private OptimizationResponse response = null;

    /**
     * Convex Optimizer method
     * 
     * @return retCode
     * @throws PatriusException if an error occurs
     **/
    public int optimize() throws PatriusException {
        // start with the first step in the chain.
        final OptimizationRequestHandler handler = new NewtonUnconstrained(true);
        handler.setOptimizationRequest(request);
        final int retCode = handler.optimize();
        this.response = handler.getOptimizationResponse();
        return retCode;
    }
    
    /**
     * Set the optimization request
     * 
     * @param or optimization request
     **/
    public void setOptimizationRequest(final OptimizationRequest or) {
        this.request = or;
    }
    
    /**
     * Get the optimization request
     * 
     * @return optimization request
     **/
    public OptimizationResponse getOptimizationResponse() {
        return response;
    }
}
