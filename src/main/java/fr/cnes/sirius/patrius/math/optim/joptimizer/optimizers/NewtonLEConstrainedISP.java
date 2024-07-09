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

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.BarrierFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LogarithmicBarrier;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AbstractKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.BasicKKTSolver;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Linear equality constrained newton optimizer, with infeasible starting point.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 521"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * @since 4.6
 */
public class NewtonLEConstrainedISP extends OptimizationRequestHandler {

    /** ktt solver */
    private AbstractKKTSolver kktSolver;

    /**
     * Constructor
     * @param activateChain true/false
     */
    public NewtonLEConstrainedISP(final boolean activateChain) {
        super();
        if (activateChain) {
            this.successor = new PrimalDualMethod();
        }
    }

    /**
     * Constructor
     */
    public NewtonLEConstrainedISP() {
        this(false);
    }

    /**
     * Optimizer
     */
    //CHECKSTYLE: stop CyclomaticComplexity check
    //CHECKSTYLE: stop MethodLength check
    // Reason: complex JOptimizer code kept as such
    @Override
    public int optimize() throws PatriusException, PatriusRuntimeException {
        //CHECKSTYLE: resume CyclomaticComplexity check
        //CHECKSTYLE: resume MethodLength check

        // checking responsibility
        if (getFi() != null) {
            // forward to the chain
            return forwardOptimizationRequest();
        }

        RealVector x0 = getInitialPoint();
        if (x0 == null) {
            if (getA() != null) {
                x0 = findEqFeasiblePoint(getA(), getB());
                // Switch to the linear equality feasible starting point Newton algorithm
                final NewtonLEConstrainedFSP opt = new NewtonLEConstrainedFSP();
                final OptimizationRequest req = getOptimizationRequest();
                req.setInitialPoint(x0.toArray());
                opt.setOptimizationRequest(req);
                final int retcode = opt.optimize();
                final OptimizationResponse resp = opt.getOptimizationResponse();
                setOptimizationResponse(resp);
                return retcode;
            } else {
                x0 = new ArrayRealVector(getDim());
            }
        }
        final RealVector v0;
        if (getA() != null) {
            v0 = new ArrayRealVector(getA().getRowDimension());
        } else {
            v0 = new ArrayRealVector(0);
        }

        RealVector x = x0;
        RealVector v = v0;
        RealVector gradX = null;
        RealMatrix hessX = null;
        RealVector rDualXV = null;
        RealVector rPriX = null;
        double previousRPriXNorm = Double.NaN;
        double previousRXVNorm = Double.NaN;
        final OptimizationResponse response = new OptimizationResponse();
        int iteration = 0;
        while (true) {
            iteration++;

            // custom exit condition
            if (checkCustomExitConditions(x)) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break; // exit conditions passed --> success
            }

            gradX = getGradF0(x);
            hessX = getHessF0(x);
            rDualXV = rDual(x, v, gradX);
            rPriX = rPri(x);

            // exit condition
            final double rPriXNorm = rPriX.getNorm();
            final double rDualXVNorm = rDualXV.getNorm();
            final double rXVNorm = MathLib.sqrt(MathLib.pow(rPriXNorm, 2) + MathLib.pow(rDualXVNorm, 2));
            if (rPriXNorm <= getTolerance() && rXVNorm <= getTolerance()) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break; // if norms < tolerance --> success
            }

            // Newton step and decrement
            if (this.kktSolver == null) {
                // this.kktSolver = new BasicKKTSolver();
                this.kktSolver = new BasicKKTSolver();
            }
            if (isCheckKKTSolutionAccuracy()) {
                kktSolver.setCheckKKTSolutionAccuracy(isCheckKKTSolutionAccuracy());
                kktSolver.setToleranceKKT(getToleranceKKT());
            }
            kktSolver.setHMatrix(hessX);
            kktSolver.setGVector(rDualXV);
            if (getA() != null) {
                kktSolver.setAMatrix(getA());
                kktSolver.setHVector(rPriX);
            }
            final RealVector[] sol = kktSolver.solve(); // Returns two vectors solutions of the KKT
                                                        // system
            final RealVector stepV;
            if (sol[1] != null) {
                stepV = sol[1];
            } else {
                stepV = new ArrayRealVector(0);
            }

            // iteration limit condition
            if (iteration == getMaxIteration()) {
                response.setReturnCode(OptimizationResponse.FAILED);
                break; // Max iterations limit reached
            }

            // progress conditions
            if (isCheckProgressConditions()) {
                if (!Double.isNaN(previousRPriXNorm) && !Double.isNaN(previousRXVNorm)) {
                    if ((previousRPriXNorm <= rPriXNorm && rPriXNorm >= getTolerance())
                            || (previousRXVNorm <= rXVNorm && rXVNorm >= getTolerance())) {
                        response.setReturnCode(OptimizationResponse.FAILED);
                        break; // No progress achieved, exit iterations loop without desired
                               // accuracy

                    }
                }
            }
            previousRPriXNorm = rPriXNorm;
            previousRXVNorm = rXVNorm;

            // backtracking line search
            double s = 1d;
            RealVector x1 = null;
            RealVector v1 = null;
            RealVector gradX1 = null;
            RealVector rDualX1V1 = null;
            RealVector rPriX1V1 = null;
            double previousNormRX1V1 = Double.NaN;
            final RealVector stepX = sol[0];
            while (true) {
                x1 = AlgebraUtils.add(x, stepX, s);
                v1 = AlgebraUtils.add(v, stepV, s);
                if (isInDomainF0(x1)) {
                    gradX1 = getGradF0(x1);
                    rDualX1V1 = rDual(x1, v1, gradX1);
                    rPriX1V1 = rPri(x1);
                    final double normRX1V1 = rDualX1V1.getNorm() + rPriX1V1.getNorm();
                    if (normRX1V1 <= (1 - getAlpha() * s) * rXVNorm) {
                        break;
                    }

                    if (!Double.isNaN(previousNormRX1V1)) {
                        if (previousNormRX1V1 <= normRX1V1) {
                            // No progress achieved in backtracking with norm
                            break;
                        }
                    }
                    previousNormRX1V1 = normRX1V1;
                }

                s = getBeta() * s;
            }

            // update
            x = x1;
            v = v1;
        }

        response.setSolution(x.toArray());
        setOptimizationResponse(response);
        return response.getReturnCode(); // return , or 0, 1 or 2 (success, warn or fail)
    }

    /**
     * rDual(x,v) := gradF(X)+[A]T*V (p 532)
     * 
     * @param x vector X
     * @param v vector V
     * @param gradX gradient X
     * @return result
     */
    private RealVector rDual(final RealVector x,
            final RealVector v,
            final RealVector gradX) {
        if (getA() == null) {
            return gradX;
        }
        // return getAT().zMult(V, gradX.copy(), 1., 1, false);
        return AlgebraUtils.zMult(getAT(), v, gradX, 1);
    }

    /**
     * Forward optimization request
     */
    @Override
    protected int forwardOptimizationRequest() throws PatriusException {
        if (successor != null) {
            // this mean the chain was activated
            if (JOptimizer.PRIMAL_DUAL_METHOD.equals(getInteriorPointMethod())) {
                this.successor = new PrimalDualMethod();
            } else if (JOptimizer.BARRIER_METHOD.equals(getInteriorPointMethod())) {
                final BarrierFunction bf = new LogarithmicBarrier(getFi(), getDim());
                this.successor = new BarrierMethod(bf);
            }
        }
        return super.forwardOptimizationRequest();
    }

    /**
     * Set the ktt solver
     * @param kktSol solver
     */
    public void setKKTSolver(final AbstractKKTSolver kktSol) {
        this.kktSolver = kktSol;
    }
}
