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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.solvers;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.CholeskyFactorization;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.Matrix1NornRescaler;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.MatrixRescaler;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Solves
 * 
 * H.v + [A]T.w = -g, <br>
 * A.v = -h
 * 
 * for upper diagonal H.
 * H is expected to be diagonal in its upper left corner of dimension diagonalLength.
 * Only the subdiagonal elements are relevant.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 542"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class UpperDiagonalHKKTSolver extends AbstractKKTSolver {

    /** Avoid scaling? */
    private boolean avoidScaling = false;
    /** Diagonal length */
    private int diagonalLength;

    /**
     * Solver
     * @param diagonalLen diagonal length
     */
    public UpperDiagonalHKKTSolver(final int diagonalLen) {
        this(diagonalLen, false);
    }

    /**
     * Solver
     * @param diagonalLen diagonal length
     * @param avoidScal avoid scaling?
     */
    public UpperDiagonalHKKTSolver(final int diagonalLen,
            final boolean avoidScal) {
        super();
        this.diagonalLength = diagonalLen;
        this.avoidScaling = avoidScal;
    }

    /**
     * Returns the two vectors v and w.
     */
    @Override
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: complex JOptimizr code kept as such
    public RealVector[] solve() throws PatriusException {

        RealVector v = null;// dim equals cols of A
        RealVector w = null;// dim equals rank of A
        final MatrixRescaler rescaler;
        if (avoidScaling) {
            rescaler = null;
        } else {
            rescaler = new Matrix1NornRescaler();
        }
        final CholeskyFactorization hFact = new CholeskyFactorization((RealMatrix) matH, rescaler);
        boolean isHReducible = true;
        try {
            hFact.factorize();
        } catch (PatriusException e) {
            isHReducible = false;
        }

        if (isHReducible) {
            // Solving KKT system via elimination
            final RealVector hInvg;
            hInvg = hFact.solve(g);

            if (matA != null) {
                final RealMatrix hInvAT;
                hInvAT = hFact.solve(matAT);

                final RealMatrix menoSLower = AlgebraUtils.subdiagonalMultiply(matA, hInvAT);
                final RealVector aHInvg = matA.operate(hInvg);

                final CholeskyFactorization mSFact = new CholeskyFactorization(menoSLower,
                        new Matrix1NornRescaler());
                try {
                    mSFact.factorize();
                    if (h == null) {
                        w = mSFact.solve(aHInvg.mapMultiply(-1));
                    } else {
                        final RealVector hmAHInvg = AlgebraUtils.add(h, aHInvg, -1);
                        w = mSFact.solve(hmAHInvg);
                    }

                    v = hInvg.add(hInvAT.operate(w)).mapMultiply(-1);
                } catch (final PatriusException e) {
                    // NOTE: it would be more appropriate to try solving the
                    // full KKT, but if
                    // the decomposition
                    // of the Shur complement of H (>0) in KKT fails it is
                    // certainty for a
                    // numerical issue and
                    // the augmented KKT seems to be more able to recover from
                    // this situation
                    final RealVector[] fullSol = this.solveAugmentedKKT();
                    v = fullSol[0];
                    w = fullSol[1];
                }

            } else {
                // A==null
                w = null;
                v = hInvg.mapMultiply(-1);
            }
        } else {
            // H not isReducible, try solving the augmented KKT system
            final RealVector[] fullSol = this.solveAugmentedKKT();
            v = fullSol[0];
            w = fullSol[1];
        }

        // solution checking
        if (this.checkKKTSolutionAcc && !this.checkKKTSolutionAccuracy(v, w)) {
            throw new PatriusException(PatriusMessages.KKT_SOLUTION_FAILED);
        }

        final RealVector[] ret = new RealVector[2];
        ret[0] = v;
        ret[1] = w;
        return ret;
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
     */
    @Override
    protected boolean checkKKTSolutionAccuracy(final RealVector v,
            final RealVector w) {
        // initializes the variables
        RealMatrix kkt = null;
        RealVector x = null;
        RealVector b = null;
        final RealMatrix hFull = AlgebraUtils.fillSubdiagonalSymmetricMatrix(this.matH);

        if (this.matA != null) {
            if (h != null) {
                // H.v + [A]T.w = -g
                // A.v = -h
                final RealMatrix[][] parts = { { hFull, this.matAT }, { this.matA, null } };
                kkt = AlgebraUtils.composeMatrix(parts);

                x = v.append(w);
                b = g.append(h).mapMultiply(-1);
            } else {
                // H.v + [A]T.w = -g
                final RealMatrix[][] parts = { { hFull, this.matAT } };
                kkt = AlgebraUtils.composeMatrix(parts);
                x = v.append(w);
                b = g.mapMultiply(-1);
            }
        } else {
            // H.v = -g
            kkt = hFull;
            x = v;
            b = g.mapMultiply(-1);
        }

        // checking residual
        final double scaledResidual = Utils.calculateScaledResidual(kkt, x, b);
        return scaledResidual < toleranceKKT;
    }

    /**
     * Set diagonal length
     * @param diagonalLength length
     */
    public void setDiagonalLength(final int diagonalLength) {
        this.diagonalLength = diagonalLength;
    }

    /**
     * Get diagonal length
     * @return length
     */
    public int getDiagonalLength() {
        return diagonalLength;
    }
}
