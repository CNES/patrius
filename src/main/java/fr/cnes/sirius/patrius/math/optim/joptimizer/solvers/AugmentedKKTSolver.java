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
package fr.cnes.sirius.patrius.math.optim.joptimizer.solvers;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.DefaultRealMatrixChangingVisitor;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.CholeskyFactorization;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.Matrix1NornRescaler;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.MatrixRescaler;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Solves the KKT system
 * 
 * H.v + [A]T.w = -g, <br>
 * A.v = -h
 * 
 * with singular H. The KKT matrix is nonsingular if and only if H + ATQA > 0
 * for some Q > 0, 0, in which case, H + ATQA > 0 for all Q > 0. This class uses
 * the diagonal matrix Q = s.Id with scalar s > 0 to try finding the solution.
 * NOTE: matrix A can not be null for this solver
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 547"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 *
 * @since 4.6
 */
public class AugmentedKKTSolver extends AbstractKKTSolver {
    
    /** Augmentation constant */
    private double s = DEFAULT_SCALAR;

    /**
     * Returns the two vectors v and w.
     */
    @Override
    public RealVector[] solve() throws PatriusException {

        if (matA == null) {
            throw new IllegalStateException("Matrix A cannot be null");
        }
   
        // augmentation
        final RealMatrix matHAugm = AlgebraUtils.subdiagonalMultiply(matAT, matA);// H + ATQA
        matHAugm.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
            /** {@inheritDoc} */
            @Override
            public double visit(final int i,
                    final int j,
                    final double matHAugmij) {
                if (matHAugmij != 0) { 
                    return Double.valueOf(s * matHAugmij);
                } else {
                    return matHAugmij;
                }
            }
        });

        matH.walkInOptimizedOrder(new DefaultRealMatrixChangingVisitor() {
            /** {@inheritDoc} */
            @Override
            public double visit(final int i,
                    final int j,
                    final double matHij) {
                if (matHij != 0) {
                    if (i + 1 > j) {
                        // the sub-diagonal elements
                        matHAugm.setEntry(i, j, matHij + matHAugm.getEntry(i, j));
                    }
                }
                return matHij;
            }
        });

        RealVector gAugm = null;// g + ATQh
        if (h != null) {
            final RealVector matATQh = matAT.operate(AlgebraUtils.diagonalMatrixMult(
                    new ArrayRealVector(matA.getRowDimension(), 1), h));
            final RealVector gATQh = AlgebraUtils.add(g, matATQh, defaultScalar);
            gAugm = gATQh;
        } else {
            gAugm = g.copy();
        }

        // solving the augmented system
        final CholeskyFactorization matHFact = new CholeskyFactorization(matHAugm, 
                (MatrixRescaler) new Matrix1NornRescaler());
        matHFact.factorize();
        
        // Solving KKT system via elimination
        final RealVector matHInvg = matHFact.solve(gAugm);
        final RealMatrix matHInvAT = matHFact.solve(matAT);
        final RealMatrix matMenoSLower = AlgebraUtils.subdiagonalMultiply(matA, matHInvAT);
        final RealVector matAHInvg = matA.operate(matHInvg);
        
        final CholeskyFactorization matMSFact = new CholeskyFactorization(matMenoSLower, 
                (MatrixRescaler) new Matrix1NornRescaler());
        matMSFact.factorize();
        RealVector w = null;// dim equals rank of A
        if (h == null) {
            w = matMSFact.solve(matAHInvg.mapMultiply(-1));
        } else {
            w = matMSFact.solve(AlgebraUtils.add(h, matAHInvg, -1));
        }
        
        // v = -(HInvg + HInvAT.w)
        final RealVector v = matHInvg.add(matHInvAT.operate(w)).mapMultiply(-1); // dim equals cols of A

        // solution checking
        if (this.checkKKTSolutionAcc && !this.checkKKTSolutionAccuracy(v, w)) {
            throw new PatriusException(PatriusMessages.KKT_SOLUTION_FAILED);
        }

        final RealVector[] ret = new ArrayRealVector[2];
        ret[0] = v;
        ret[1] = w;
        return ret;
    }
    /**
     * Set a value to s
     * @param constant value to assign to the variable s
     */
    public void setS(final double constant) {
        this.s = constant;
    }
}
