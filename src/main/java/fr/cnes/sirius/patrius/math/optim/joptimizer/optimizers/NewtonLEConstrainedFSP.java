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
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AbstractKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.BasicKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Linear equality constrained newton optimizer, with feasible starting point.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 521"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * 
 * @since 4.6
 */
public class NewtonLEConstrainedFSP extends OptimizationRequestHandler {

    /** Maximum number of iterations */
    private static final double MAX_ITERATIONS = 250;
    
    /** ktt solver */
    private AbstractKKTSolver kktSolver;

    /**
     * Constructor
     * @param activateChain true/false
     */
    public NewtonLEConstrainedFSP(final boolean activateChain) {
        super();
        if (activateChain) {
            this.successor = new NewtonLEConstrainedISP(true);
        }
    }

    /**
     * Constructor
     */
    public NewtonLEConstrainedFSP() {
        this(false);
    }

    /**
     * Optimizer
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    //CHECKSTYLE: stop MethodLength check
    // Reason: complex JOptimizer code kept as such
    @Override
    public int optimize() throws PatriusException {
        //CHECKSTYLE: resume CyclomaticComplexity check
        //CHECKSTYLE: resume MethodLength check
        
        // checking responsibility
        if (getFi() != null) {
            // forward to the chain
            return forwardOptimizationRequest();
        }

        // initial point must be feasible (i.e., satisfy x in domF and Ax = b).
        final RealVector x0 = getInitialPoint();
        final double rPriX0Norm;
        if (x0 != null) {
            rPriX0Norm = rPri(x0).getNorm();
        }else {
            rPriX0Norm = 0d;
        }
        if (x0 == null || rPriX0Norm > getTolerance()) {
            // infeasible starting point, forward to the chain
            return forwardOptimizationRequest();
        }
        RealVector x = x0;
        double f0X;
        // double previousF0X = Double.NaN;
        double previousLambda = Double.NaN;
        final OptimizationResponse response = new OptimizationResponse();
        int iteration = 0;
        while (true) {
            iteration++;
            f0X = getF0(x);

            // custom exit condition
            if (checkCustomExitConditions(x)) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break; // optimization success, stop
            }

            final RealVector gradX = getGradF0(x);  // get gradient
            final RealMatrix hessX = getHessF0(x);  // get hessian

            final double gradXNorm = gradX.getNorm();
            // exit condition: check norm < epsilon
            if (gradXNorm < Utils.getDoubleMachineEpsilon()) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break; // optimization success, stop
            }

            // Newton step and decrement
            if (this.kktSolver == null) {
                this.kktSolver = new BasicKKTSolver();
            }
            if (isCheckKKTSolutionAccuracy()) {
                kktSolver.setCheckKKTSolutionAccuracy(isCheckKKTSolutionAccuracy());
                kktSolver.setToleranceKKT(getToleranceKKT());
            }
            kktSolver.setHMatrix(hessX);
            kktSolver.setGVector(gradX);
            if (getA() != null) {
                kktSolver.setAMatrix(getA());
            }
            final RealVector[] sol = kktSolver.solve(); //Returns two vectors solutions of the KKT system
            final RealVector step = sol[0];

            // exit condition: check the Newton decrement
            final double lambda = MathLib.sqrt(step.dotProduct(hessX.operate(step)));
            if (lambda / 2. <= getTolerance()) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break; // optimization succes, stop
            }

            // iteration limit condition
            if (iteration == getMaxIteration()) {
                response.setReturnCode(OptimizationResponse.FAILED);
                break; // Max iterations limit reached
            }

            // progress conditions
            if (isCheckProgressConditions()) {
                if (!Double.isNaN(previousLambda) && previousLambda <= lambda) {
                    response.setReturnCode(OptimizationResponse.FAILED);
                    break;// No progress achieved, exit iterations loop without desired accuracy
                }
            }
            previousLambda = lambda;

            // backtracking line search
            double s = 1d;
            RealVector x1 = null;
            int cnt = 0;
            while (cnt < MAX_ITERATIONS) {
                cnt++;
                // @TODO: can we use simplification 9.7.1 ??

                // x + s*step
                x1 = AlgebraUtils.add(x, step, s);

                if (isInDomainF0(x1)) {
                    final double condSX = getF0(x1);
                    // NB: this will also check !Double.isNaN(getF0(X1))
                    final double condDX = f0X + getAlpha() * s * gradX.dotProduct(step);
                    if (condSX <= condDX) {
                        break;
                    }
                }
                s = getBeta() * s;
            }

            // update
            x = x1;
        }

        response.setSolution(x.toArray());
        setOptimizationResponse(response);
        return response.getReturnCode();  // return success or fail
    }

    /**
     * Set the ktt solver
     * @param kktSol ktt solver
     */
    public void setKKTSolver(final AbstractKKTSolver kktSol) {
        this.kktSolver = kktSol;
    }
}
