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
package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;

/**
 * Interface for Matrix rescalers.
 * Calculate the row and column scaling matrices R and T relative to a given
 * matrix A (scaled A = R.A.T).
 * They may be used, for instance, to scale the matrix prior to solving a
 * corresponding set of linear equations.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public interface MatrixRescaler {

    /**
     * Calculates the R and T scaling factors (matrices) for a generic matrix A so that A'(=scaled
     * A) = R.A.T
     * @param a matrix A
     * @return array with R,T
     */
    RealVector[] getMatrixScalingFactors(final RealMatrix a);

    /**
     * Calculates the R and T scaling factors (matrices) for a symmetric matrix A so that A'(=scaled
     * A) = R.A.T
     * @param a matrix A
     * @return array with R,T
     */
    RealVector getMatrixScalingFactorsSymm(final RealMatrix a);

    /**
     * Check if the scaling algorithm returned proper results.
     * @param aOriginal the ORIGINAL (before scaling) matrix
     * @param u the return of the scaling algorithm
     * @param v the return of the scaling algorithm
     * @return true/false
     */
    boolean checkScaling(final RealMatrix aOriginal,
            final RealVector u,
            final RealVector v);
}