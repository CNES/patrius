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
package fr.cnes.sirius.patrius.math.optim.joptimizer.solvers;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.CholeskyFactorization;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.Matrix1NornRescaler;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * H.v + [A]T.w = -g, <br>
 * A.v = -h
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 542"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * 
 * @since 4.6
 */
public final class BasicKKTSolver extends AbstractKKTSolver {

    /**
     * Returns the two vectors v and w.
     */
    @Override
    public RealVector[] solve() throws PatriusException {
        final CholeskyFactorization matHFact = new CholeskyFactorization(matH, new Matrix1NornRescaler());
        boolean isHReducible = true;
        try{
            matHFact.factorize();
        }catch(IllegalArgumentException e){
            isHReducible = false;
        }catch(PatriusException e){
            isHReducible = false;
        }
        final RealVector v;// dim equals cols of A
        final RealVector w;// dim equals rank of A
        

        if (isHReducible) {
            // Solving KKT system via elimination
            final RealVector matHInvg = matHFact.solve(g);
            if (matA != null) {
                final RealMatrix matHInvAT = matHFact.solve(matAT);
                final RealMatrix matMenoSLower = AlgebraUtils.subdiagonalMultiply(matA, matHInvAT);
                final RealVector matAHInvg = matA.operate(matHInvg);

                final CholeskyFactorization matMSFact = new CholeskyFactorization(matMenoSLower, 
                        new Matrix1NornRescaler());
                matMSFact.factorize();
                if (h == null) {
                    w = matMSFact.solve(matAHInvg.mapMultiply(-1));
                } else {
                    w = matMSFact.solve(AlgebraUtils.add(h, matAHInvg, -1));
                }

                v = matHInvg.add(matHInvAT.operate(w)).mapMultiply(-1);
            } else {
                w = null;
                v = matHInvg.mapMultiply(-1);
            }
        } else {
            // H is singular
            // Solving the full KKT system
            if (matA != null) {
                final RealVector[] fullSol = this.solveAugmentedKKT();
                v = fullSol[0];
                w = fullSol[1];
            } else {
                // KKT solution failed
                throw new PatriusException(PatriusMessages.KKT_SOLUTION_FAILED); 
            }
        }
        
        // solution checking
        if (this.checkKKTSolutionAcc && !this.checkKKTSolutionAccuracy(v, w)) {
            // KKT solution failed
            throw new PatriusException(PatriusMessages.KKT_SOLUTION_FAILED); 
        }
        
        final RealVector[] ret = new RealVector[2];
        ret[0] = v;
        ret[1] = w;
        return ret;
    }
}