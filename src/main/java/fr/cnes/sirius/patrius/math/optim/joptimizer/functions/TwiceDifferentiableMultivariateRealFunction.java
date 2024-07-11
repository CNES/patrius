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
 * Interface for multi-variate functions that are twice differentiable., i.e. for which a gradient and a hessian can be
 * provided.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public interface TwiceDifferentiableMultivariateRealFunction {

    /**
     * Evaluation of the function at point X.
     * 
     * @param x point
     * @return evaluation
     */
    double value(double[] x);

    /**
     * Function gradient at point X.
     * 
     * @param x point
     * @return gradient
     */
    double[] gradient(double[] x);

    /**
     * Function hessian at point X.
     * @param x point
     * @return hessian
     */
    double[][] hessian(double[] x);

    /**
     * Get dimension of the function argument.
     * @return dimension
     */
    int getDim();
}
