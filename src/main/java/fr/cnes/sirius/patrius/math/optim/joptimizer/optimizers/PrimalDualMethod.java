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
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.AbstractKKTSolver;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.BasicKKTSolver;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Primal-dual interior-point method.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 609"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * @since 4.6
 */
public class PrimalDualMethod extends OptimizationRequestHandler {

    /** ktt solver */
    private AbstractKKTSolver kktSolver;

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

        RealVector x0 = getInitialPoint();
        if (x0 == null) {
            final RealVector x0NF = getNotFeasibleInitialPoint();
            if (x0NF != null) {
                final double rPriX0NFNorm = (rPri(x0NF)).getNorm();
                final RealVector fiX0NF = getFi(x0NF);
                final int maxIndex = fiX0NF.getMaxIndex();
                final double maxValue = fiX0NF.getEntry(maxIndex);
                if (maxValue < 0 && rPriX0NFNorm <= getToleranceFeas()) {
                    x0 = x0NF; // the provided not-feasible starting point is already feasible
                }
            }
            if (x0 == null) {
                final BasicPhaseIPDM bf1 = new BasicPhaseIPDM(this);
                x0 = bf1.findFeasibleInitialPoint();
            }
        }

        // check X0 feasibility
        final RealVector fiX0 = getFi(x0);
        final int maxIndex = fiX0.getMaxIndex();
        final double maxValue = fiX0.getEntry(maxIndex);
        if (maxValue >= 0) {
            // Initial point must be strictly feasible
            throw new PatriusException(PatriusMessages.INITIAL_POINT_NOT_FEASIBLE);
        }

        final double rPriX0Norm = (rPri(x0)).getNorm();
        if (rPriX0Norm > getToleranceFeas()) {
            // Initial point must be strictly feasible
            throw new PatriusException(PatriusMessages.INITIAL_POINT_NOT_FEASIBLE);
        }

        final RealVector v0;
        if (getA() != null) {
            v0 = new ArrayRealVector(getA().getRowDimension());
        } else {
            v0 = new ArrayRealVector(0);
        }

        RealVector l0 = getInitialLagrangian();
        if (l0 != null) {
            for (int j = 0; j < l0.getDimension(); j++) {
                // must be >0
                if (l0.getEntry(j) <= 0) {
                    throw new IllegalArgumentException("initial lagrangian must be strictly > 0");
                }
            }
        } else {
            l0 = new ArrayRealVector(getMieq(), MathLib.min(1, (double) getDim() / getMieq()));// must
                                                                                            // be >0
        }

        RealVector x = x0;
        RealVector v = v0;
        RealVector l = l0;

        double previousRPriXNorm = Double.NaN;
        double previousRDualXLVNorm = Double.NaN;
        double previousSurrDG = Double.NaN;

        final OptimizationResponse response = new OptimizationResponse();
        int iteration = 0;
        while (true) {

            iteration++;
            // iteration limit condition
            if (iteration == getMaxIteration() + 1) {
                response.setReturnCode(OptimizationResponse.FAILED);
                break; // Max iterations limit reached
            }

            // determine functions evaluations
            final RealVector gradF0X = getGradF0(x);
            final RealVector fiX = getFi(x);
            final RealMatrix gradFiX = getGradFi(x);

            // custom exit condition
            if (checkCustomExitConditions(x)) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break; // optimization success, stop
            }

            final double surrDG = getSurrogateDualityGap(fiX, l);

            // determine residuals
            final RealVector rPriX = rPri(x);
            final RealVector rDualXLV = rDual(gradFiX, gradF0X, l, v);
            final double rPriXNorm = rPriX.getNorm();
            final double rDualXLVNorm = rDualXLV.getNorm();

            // exit condition
            if (rPriXNorm <= getToleranceFeas() && rDualXLVNorm <= getToleranceFeas()
                    && surrDG <= getTolerance()) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break; // optimization success, stop
            }

            // progress conditions
            if (isCheckProgressConditions()) {
                if (!Double.isNaN(previousRPriXNorm) && !Double.isNaN(previousRDualXLVNorm)
                        && !Double.isNaN(previousSurrDG)) {
                    if ((previousRPriXNorm <= rPriXNorm && rPriXNorm >= getToleranceFeas())
                            || (previousRDualXLVNorm <= rDualXLVNorm && rDualXLVNorm >= getToleranceFeas())) {
                        response.setReturnCode(OptimizationResponse.FAILED);
                        break; // No progress achieved, exit iterations loop without desired
                               // accuracy
                    }
                }
                previousRPriXNorm = rPriXNorm;
                previousRDualXLVNorm = rDualXLVNorm;
                previousSurrDG = surrDG;
            }

            // compute primal-dual search direction
            // a) prepare 11.55 system
            RealMatrix hessSum = getHessF0(x);
            final RealMatrix[] hessFiX = getHessFi(x);
            for (int j = 0; j < getMieq(); j++) {
                if (hessFiX[j] != null) {
                    hessSum = AlgebraUtils.add(hessSum, hessFiX[j], l.getEntry(j));
                }
            }

            RealMatrix gradSum2 = new BlockRealMatrix(getDim(), getDim());
            for (int j = 0; j < getMieq(); j++) {
                final double c = -l.getEntry(j) / fiX.getEntry(j);
                final RealVector g = gradFiX.getRowVector(j);
                // SeqBlas.seqBlas.dger(c, g, g, GradSum);
                // compute GradSum+c*g*g'
                gradSum2 = gradSum2.add(g.mapMultiply(c).outerProduct(g));
            }

            final RealMatrix hpd = hessSum.add(gradSum2);

            RealVector gradSum = new ArrayRealVector(getDim());
            // determine t
            final double t = getMu() * getMieq() / surrDG;
            for (int j = 0; j < getMieq(); j++) {
                gradSum = AlgebraUtils.add(gradSum, gradFiX.getRowVector(j),
                        1. / (-t * fiX.getEntry(j)));
            }
            RealVector g = null;
            if (getAT() == null) {
                g = gradF0X.add(gradSum);
            } else {
                g = gradF0X.add(gradSum.add(getAT().operate(v)));
            }

            // b) solving 11.55 system
            if (this.kktSolver == null) {
                this.kktSolver = new BasicKKTSolver();
            }
            if (isCheckKKTSolutionAccuracy()) {
                kktSolver.setCheckKKTSolutionAccuracy(true);
                kktSolver.setToleranceKKT(getToleranceKKT());
            }
            kktSolver.setHMatrix(hpd);
            kktSolver.setGVector(g);
            if (getA() != null) {
                kktSolver.setAMatrix(getA());
                kktSolver.setHVector(rPriX);
            }
            final RealVector[] sol = kktSolver.solve(); // Returns two vectors solutions of the KKT
                                                        // system
            final RealVector stepX = sol[0];
            final RealVector stepV;
            if (sol[1] != null) {
                stepV = sol[1];
            } else {
                stepV = new ArrayRealVector(0);
            }

            // c) solving for L
            RealVector stepL = null;
            final RealVector rCentXLt = rCent(fiX, l, t);
            final RealVector a2 = rCentXLt.copy().ebeDivide(fiX);
            final RealVector b2 = gradFiX.operate(stepX).ebeMultiply(l);
            final RealVector c2 = b2.ebeDivide(fiX);
            stepL = AlgebraUtils.add(a2, c2, -1);

            // line search and update
            // a) sMax computation
            double sMax = Double.MAX_VALUE;
            for (int j = 0; j < getMieq(); j++) {
                if (stepL.getEntry(j) < 0) {
                    sMax = MathLib.min(-l.getEntry(j) / stepL.getEntry(j), sMax);
                }
            }
            sMax = MathLib.min(1, sMax);
            double s = SCALAR * sMax;
            // b) backtracking with f
            RealVector x1 = new ArrayRealVector(x.getDimension());
            int cnt = 0;
            boolean areAllNegative = true;
            while (cnt < MAX_ITERATIONS) {
                cnt++;
                // X1 = X + s*stepX
                x1 = stepX.copy().mapMultiply(s).add(x);
                final RealVector ineqValueX1 = getFi(x1);
                areAllNegative = true;
                for (int j = 0; areAllNegative && j < getMieq(); j++) {
                    areAllNegative = (Double.compare(ineqValueX1.getEntry(j), 0.) < 0);
                }
                if (areAllNegative) {
                    break;
                }
                s = getBeta() * s;
            }

            if (!areAllNegative) {
                // exited from the feasible region
                // Optimization failed: impossible to remain within the feasible region
                throw new PatriusException(PatriusMessages.OPTIMIZATION_FAILED);
            }

            // c) backtracking with norm
            double previousNormRX1L1V1t = Double.NaN;
            final double rCentXLtNorm = rCentXLt.getNorm();
            final double normRXLVt = MathLib.sqrt(MathLib.pow(rPriXNorm, 2) + MathLib.pow(rCentXLtNorm, 2)
                    + MathLib.pow(rDualXLVNorm, 2));
            RealVector l1 = new ArrayRealVector(l.getDimension());
            RealVector v1 = new ArrayRealVector(v.getDimension());
            RealVector fiX1 = null;
            RealVector gradF0X1 = null;
            RealMatrix gradFiX1 = null;
            RealVector rPriX1 = null;
            RealVector rCentX1L1t;
            cnt = 0;
            while (cnt < MAX_ITERATIONS) {
                cnt++;
                x1 = AlgebraUtils.add(x, stepX, s);
                l1 = AlgebraUtils.add(l, stepL, s);
                v1 = AlgebraUtils.add(v, stepV, s);

                if (isInDomainF0(x1)) {
                    fiX1 = getFi(x1);
                    gradF0X1 = getGradF0(x1);
                    gradFiX1 = getGradFi(x1);

                    rPriX1 = rPri(x1);
                    rCentX1L1t = rCent(fiX1, l1, t);
                    final RealVector rDualX1L1V1 = rDual(gradFiX1, gradF0X1, l1, v1);
                    final double normRX1L1V1t = rPriX1.getNorm() + rCentX1L1t.getNorm()
                            + rDualX1L1V1.getNorm();
                    if (normRX1L1V1t <= (1 - getAlpha() * s) * normRXLVt) { // exit condition
                        break;
                    }

                    if (!Double.isNaN(previousNormRX1L1V1t)) {
                        if (previousNormRX1L1V1t <= normRX1L1V1t) { // exit condition
                            // No progress achieved in backtracking with norm
                            break;
                        }
                    }
                    previousNormRX1L1V1t = normRX1L1V1t;
                }

                s = getBeta() * s;
            }

            // update
            x = x1;
            v = v1;
            l = l1;
        }

        response.setSolution(x.toArray());
        response.setMultiplicators(l.toArray());
        setOptimizationResponse(response);
        return response.getReturnCode(); // return 0,1 or 2 (success, warn or failed)
    }

    /**
     * Get surrogate duality gap.
     * 
     * @param fiX vector
     * @param l vector
     * @return duality gap
     * @see "Convex Optimization, 11.59"
     */
    private double getSurrogateDualityGap(final RealVector fiX,
            final RealVector l) {
        return -fiX.dotProduct(l);
    }

    /**
     * 
     * @param gradFiX matrix
     * @param gradF0X vector
     * @param l vector
     * @param v vector
     * @return solution
     * @see "Convex Optimization, p. 610"
     */
    private RealVector rDual(final RealMatrix gradFiX,
            final RealVector gradF0X,
            final RealVector l,
            final RealVector v) {
        if (getA() == null) {
            return AlgebraUtils.zMultTranspose(gradFiX, l, gradF0X, 1.);
        }
        return AlgebraUtils.zMultTranspose(getA(), v,
                AlgebraUtils.zMultTranspose(gradFiX, l, gradF0X, 1.), 1.);
    }

    /**
     * 
     * @param fiX vector
     * @param l vector
     * @param t value
     * @return solution
     * @see "Convex Optimization, p. 610"
     */
    private RealVector rCent(final RealVector fiX,
            final RealVector l,
            final double t) {
        final RealVector ret = new ArrayRealVector(l.getDimension());
        for (int i = 0; i < ret.getDimension(); i++) {
            ret.setEntry(i, -l.getEntry(i) * fiX.getEntry(i) - 1. / t);
        }
        return ret;
    }
}
