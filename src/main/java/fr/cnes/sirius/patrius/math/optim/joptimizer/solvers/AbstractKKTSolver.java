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
package fr.cnes.sirius.patrius.math.optim.joptimizer.solvers;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Solves the KKT system:
 * 
 * H.v + [A]T.w = -g, <br>
 * A.v = -h, <br>
 * 
 * (H is square and symmetric)
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 542"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 *
 * @since 4.6
 */
public abstract class AbstractKKTSolver {

    /** Default scalar */
    public static final double DEFAULT_SCALAR = 1.e-6;
    /** Matrix H */
    protected RealMatrix matH;
    /** Matrix A */
    protected RealMatrix matA;
    /** Transpose A matrix */
    protected RealMatrix matAT;
    /** Vector g */
    protected RealVector g;
    /** Vector h */
    protected RealVector h;
    /** KTT tolerance */
    protected double toleranceKKT = Utils.getDoubleMachineEpsilon();
    /** Check if the KTT accuracy is preserved*/
    protected boolean checkKKTSolutionAcc;
    /** Default scalar */
    protected double defaultScalar = DEFAULT_SCALAR;

    /**
     * Returns two vectors v and w solutions of the KKT system.
     * @return v and w
     * @throws PatriusException if an error occurs
     */
    public abstract RealVector[] solve() throws PatriusException;

    /**
     * Set the H matrix
     * 
     * @param hMatrix matrix H
     **/
    public void setHMatrix(final RealMatrix hMatrix) {
        this.matH = hMatrix;
    }
    
    /**
     * Set the A matrix
     * 
     * @param aMatrix matrix A
     **/
    public void setAMatrix(final RealMatrix aMatrix) {
        this.matA = aMatrix;
        this.matAT = matA.transpose();
    }

    /**
     * Set the g vector
     * 
     * @param gVector vector G
     **/
    public void setGVector(final RealVector gVector) {
        this.g = gVector;
    }
    
    /**
     * Set the h vector
     * 
     * @param hVector vector H
     **/
    public void setHVector(final RealVector hVector) {
        this.h = hVector;
    }

    /**
     * Acceptable tolerance for system resolution.
     * 
     * @param tolerance KTT tolerance
     */
    public void setToleranceKKT(final double tolerance) {
        this.toleranceKKT = tolerance;
    }
    
    /**
     * Set if the KTT solution accuracy is preserved or not
     * 
     * @param b true or false
     **/
    public void setCheckKKTSolutionAccuracy(final boolean b) {
        this.checkKKTSolutionAcc = b;
    }
    
    /**
     * Solve the augmented KTT system
     * 
     * @return vector with the solution
     * @throws PatriusException if an error occurs
     **/
    protected RealVector[] solveAugmentedKKT() throws PatriusException {
        if (matA == null) {
            throw new IllegalStateException("Matrix A cannot be null");
        }
        final AbstractKKTSolver kktSolver = new AugmentedKKTSolver();
        kktSolver.setCheckKKTSolutionAccuracy(false);// if the caller has true, then it will make
                                                     // the check, otherwise
                                                     // no check at all
        kktSolver.setHMatrix(matH);
        kktSolver.setAMatrix(matA);
        kktSolver.setGVector(g);
        kktSolver.setHVector(h);
        return kktSolver.solve();
    }

    /**
     * Check the solution of the system
     * 
     * KKT.x = b
     * 
     * against the scaled residual
     * 
     * beta < gamma,
     * 
     * where gamma is a parameter chosen by the user and beta is
     * the scaled residual,
     * 
     * beta = ||KKT.x-b||_oo/( ||KKT||_oo . ||x||_oo + ||b||_oo ),
     * with ||x||_oo = max(||x[i]||)
     * 
     * @param v vector V
     * @param w vector W
     * @return true/false
     */
    protected boolean checkKKTSolutionAccuracy(final RealVector v,
            final RealVector w) {
        RealMatrix kkt = null;
        RealVector x = null;
        RealVector b = null;

        if (this.matA != null) {
            if (h != null) {
                // H.v + [A]T.w = -g
                // A.v = -h
                final RealMatrix[][] parts = { { this.matH, this.matAT }, { this.matA, null } };
                // compose the KKT matrix from H, AT and A
                kkt = AlgebraUtils.composeMatrix(parts);
                x = v.append(w);
                b = g.append(h).mapMultiply(-1);
            } else {
                // H.v + [A]T.w = -g
                final RealMatrix[][] parts = { { this.matH, this.matAT } };
                // compose the KKT matrix from H, and AT
                kkt = AlgebraUtils.composeMatrix(parts);
                x = v.append(w);
                b = g.mapMultiply(-1);
            }
        } else {
            // H.v = -g
            kkt = this.matH;
            x = v;
            b = g.mapMultiply(-1);
        }

        // checking residual
        final double scaledResidual = Utils.calculateScaledResidual(kkt, x, b);
        return scaledResidual < toleranceKKT;
    }
}
