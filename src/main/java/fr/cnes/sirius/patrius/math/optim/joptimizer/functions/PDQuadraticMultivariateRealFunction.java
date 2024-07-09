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
package fr.cnes.sirius.patrius.math.optim.joptimizer.functions;

import fr.cnes.sirius.patrius.math.linear.CholeskyDecomposition;
import fr.cnes.sirius.patrius.math.linear.NonPositiveDefiniteMatrixException;

/**
 * Function 1/2 * x.P.x + q.x + r,
 * P symmetric and positive definite
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * 
 * @since 4.6
 */
public class PDQuadraticMultivariateRealFunction extends PSDQuadraticMultivariateRealFunction
        implements StrictlyConvexMultivariateRealFunction {

    /**
     * Constructor
     * 
     * @param pMatrix matrix
     * @param qVector vector
     * @param r value
     */
    public PDQuadraticMultivariateRealFunction(final double[][] pMatrix,
            final double[] qVector,
            final double r) {
        this(pMatrix, qVector, r, false);
    }

    /**
     * Constructor
     * 
     * @param pMatrix matrix
     * @param qVector vector
     * @param r value
     * @param checkPD check the PD?
     */
    public PDQuadraticMultivariateRealFunction(final double[][] pMatrix,
            final double[] qVector,
            final double r,
            final boolean checkPD) {
        super(pMatrix, qVector, r, false);
        if (checkPD) {
            try {
                new CholeskyDecomposition(p);
            } catch (NonPositiveDefiniteMatrixException e) {
                throw new IllegalArgumentException("P not symmetric positive definite", e);
            }
        }
    }
}
