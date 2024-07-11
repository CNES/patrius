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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.StrictlyConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AbstractKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.BasicKKTSolver;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 487"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class NewtonUnconstrained extends OptimizationRequestHandler {

    /** Maximum number of iterations */
    private static final double MAX_ITERATIONS = 25;

    /**
     * Constructor
     * @param activateChain true/false
     */
    public NewtonUnconstrained(final boolean activateChain) {
        super();
        if (activateChain) {
            this.successor = new NewtonLEConstrainedFSP(true);
        }
    }

    /**
     * Constructor
     */
    public NewtonUnconstrained() {
        this(false);
    }

    /**
     * Optimizer
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: complex JOptimizer code kept as such
    @Override
    public int optimize() throws PatriusException {
        //CHECKSTYLE: resume CyclomaticComplexity check

        // checking responsibility
        if (getA() != null || getFi() != null) {
            // forward to the chain
            return forwardOptimizationRequest();
        }
        if (!(getF0() instanceof StrictlyConvexMultivariateRealFunction)) {
            throw new PatriusException(PatriusMessages.UNSOLVABLE_PROBLEM);
        }

        RealVector x0 = getInitialPoint();
        if (x0 == null) {
            x0 = new ArrayRealVector(getDim());
        }

        RealVector x = x0;
        double previousLambda = Double.NaN;
        final OptimizationResponse response = new OptimizationResponse();
        int iteration = 0;
        while (true) {
            iteration++;

            // custom exit condition
            if (checkCustomExitConditions(x)) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break;
            }

            final RealVector gradX = getGradF0(x);
            final RealMatrix hessX = getHessF0(x);

            // Newton step and decrement
            final RealVector step = calculateNewtonStep(hessX, gradX);

            // Newton decrement
            final double lambda = MathLib.sqrt(-gradX.dotProduct(step));
            if (lambda / 2. <= getTolerance()) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break;
            }

            // iteration limit condition
            if (iteration == getMaxIteration()) {
                response.setReturnCode(OptimizationResponse.FAILED);
                break;
            }

            // progress conditions
            if (isCheckProgressConditions()) {
                if (!Double.isNaN(previousLambda) && previousLambda <= lambda) {
                    response.setReturnCode(OptimizationResponse.FAILED);
                    break;
                }
            }
            previousLambda = lambda;

            // backtracking line search
            double s = 1d;
            RealVector x1 = null;
            final double f0X = getF0(x);
            int cnt = 0;
            while (cnt < MAX_ITERATIONS) {
                cnt++;
                // X1 = X.copy().assign(step.copy().assign(Mult.mult(s)), Functions.plus);// x +
                // t*step
                x1 = AlgebraUtils.add(x, step, s);
                final double condSX = getF0(x1);
                // NB: this will also check !Double.isNaN(getF0(X1))
                final double condDX = f0X + getAlpha() * s * gradX.dotProduct(step);
                if (condSX <= condDX) {
                    break;
                }
                s = getBeta() * s;
            }

            // update
            x = x1;
        }

        response.setSolution(x.toArray());
        setOptimizationResponse(response);
        return response.getReturnCode();
    }

    /**
     * y
     * NB: the matrix hessX is square
     * Hess.step = -Grad
     * 
     * @param hessX hessian matrix X
     * @param gradX gradient of X
     * @return step
     * @throws PatriusException if an error occurs
     */
    private RealVector calculateNewtonStep(final RealMatrix hessX,
            final RealVector gradX) throws PatriusException {
        final AbstractKKTSolver kktSolver = new BasicKKTSolver();
        if (isCheckKKTSolutionAccuracy()) {
            kktSolver.setCheckKKTSolutionAccuracy(isCheckKKTSolutionAccuracy());
            kktSolver.setToleranceKKT(getToleranceKKT());
        }
        kktSolver.setHMatrix(hessX);
        kktSolver.setGVector(gradX);
        final RealVector[] sol = kktSolver.solve();
        return sol[0];
    }
}
