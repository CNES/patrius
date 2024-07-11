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

import fr.cnes.sirius.patrius.math.linear.EigenDecomposition;

/**
 * Function 1/2 * x.P.x + q.x + r,
 * P symmetric and positive semi-definite
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class PSDQuadraticMultivariateRealFunction extends QuadraticMultivariateRealFunction
        implements ConvexMultivariateRealFunction {

    /**
     * Constructor
     * 
     * @param pMatrix matrix P
     * @param qVector vector Q
     * @param r value r
     */
    public PSDQuadraticMultivariateRealFunction(final double[][] pMatrix,
            final double[] qVector,
            final double r) {
        this(pMatrix, qVector, r, false);
    }

    /**
     * Constructor
     * 
     * @param pMatrix matrix P
     * @param qVector vector Q
     * @param r value r
     * @param checkPSD check the PSD?
     */
    public PSDQuadraticMultivariateRealFunction(final double[][] pMatrix,
            final double[] qVector,
            final double r,
            final boolean checkPSD) {
        super(pMatrix, qVector, r);
        if (checkPSD) {
            final EigenDecomposition eDecomp = new EigenDecomposition(p);
            final double[] realEigenvalues = eDecomp.getRealEigenvalues();
            for (int i = 0; i < realEigenvalues.length; i++) {
                if (realEigenvalues[i] < 0) {
                    throw new IllegalArgumentException("Not positive semi-definite matrix");
                }
            }
        }
    }
}
