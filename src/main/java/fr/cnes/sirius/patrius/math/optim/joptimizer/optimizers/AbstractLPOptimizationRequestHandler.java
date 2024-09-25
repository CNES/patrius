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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;

/**
 * Class Linear Problem Optimization Request Handler.
 *
 * @since 4.6
 **/
public abstract class AbstractLPOptimizationRequestHandler extends OptimizationRequestHandler {

    /** Message */
    private static final String WITH_CLASS = "Use the matrix formulation with the class ";
    /** Message */
    private static final String THIS_PROB = " for this linear problem";

    /**
     * Set the linear problem optimization request
     * 
     * @param lpReq request
     **/
    public void setLPOptimizationRequest(final LPOptimizationRequest lpReq) {
        this.request = lpReq;
    }

    /**
     * Get the linear problem optimization request
     * 
     * @return request
     **/
    protected LPOptimizationRequest getLPOptimizationRequest() {
        return (LPOptimizationRequest) this.request;
    }

    /**
     * Set the linear problem optimization response
     * 
     * @param lpRes response
     **/
    protected void setLPOptimizationResponse(final LPOptimizationResponse lpRes) {
        this.response = lpRes;
    }

    /**
     * Get the linear problem optimization response
     * 
     * @return response
     **/
    public LPOptimizationResponse getLPOptimizationResponse() {
        return (LPOptimizationResponse) this.response;
    }

    /**
     * Set optimization request 
     * @param request
     */
    @Override
    public void setOptimizationRequest(final OptimizationRequest request) {
        if (request instanceof LPOptimizationRequest) {
            super.setOptimizationRequest(request);
        } else {
            throw new UnsupportedOperationException(WITH_CLASS
                    + LPOptimizationRequest.class.getName() + THIS_PROB);
        }
    }

    /**
     * Set optimization response
     * @param response
     */
    @Override
    protected void setOptimizationResponse(final OptimizationResponse response) {
        if (response instanceof LPOptimizationResponse) {
            super.setOptimizationResponse(response);
        } else {
            throw new UnsupportedOperationException(WITH_CLASS
                    + LPOptimizationRequest.class.getName() + THIS_PROB);
        }
    }

    /**
     * Get the linear objective function
     * 
     * @return linear objective function
     **/
    protected RealVector getC() {
        return getLPOptimizationRequest().getC();
    }

    /**
     * Get the linear inequalities constraints matrix
     * 
     * @return linear inequalities constraints matrix
     **/
    protected RealMatrix getG() {
        return getLPOptimizationRequest().getG();
    }

    /**
     * Get the linear inequalities constraints coefficients
     * 
     * @return linear inequalities constraints coefficients
     **/
    protected RealVector getH() {
        return getLPOptimizationRequest().getH();
    }

    /**
     * Get the lower bounds
     * 
     * @return lower bounds 
     **/
    protected RealVector getLb() {
        return getLPOptimizationRequest().getLb();
    }

    /**
     * Get the upper bounds
     * 
     * @return upper bounds 
     **/
    protected RealVector getUb() {
        return getLPOptimizationRequest().getUb();
    }

    /**
     * Get the Lagrangian lower bounds for linear constraints (A rows)
     * 
     * @return Lagrangian lower bounds for linear constraints 
     **/
    protected RealVector getYlb() {
        return getLPOptimizationRequest().getYlb();
    }

    /**
     * Get the Lagrangian upper bounds for linear constraints (A rows)
     * 
     * @return Lagrangian upper bounds for linear constraints 
     **/
    protected RealVector getYub() {
        return getLPOptimizationRequest().getYub();
    }

    /**
     * Get the Lagrangian upper bounds for linear bounds
     * 
     * @return Lagrangian upper bounds for linear bounds
     **/
    protected RealVector getZlb() {
        return getLPOptimizationRequest().getZlb();
    }

    /**
     * Get the Lagrangian upper bounds for upper bounds
     * 
     * @return Lagrangian upper bounds for upper bounds
     **/
    protected RealVector getZub() {
        return getLPOptimizationRequest().getZub();
    }
}
