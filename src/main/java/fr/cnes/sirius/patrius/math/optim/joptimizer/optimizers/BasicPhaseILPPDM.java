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
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.solvers.UpperDiagonalHKKTSolver;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Basic Phase I Method form LP problems (implemented as a Primal-Dual Method).
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 579"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class BasicPhaseILPPDM {

    /** Scalar */
    private static final double SCALAR = 1.1;
    /** LPPrimal Dual Method original problem  */
    private final LPPrimalDualMethod originalProblem;
    /** Original dimension */
    private final int originalDim;
    /** Dimension  */
    private final int dim;

    /**
     * Constructor
     * 
     * @param originalP LPPrimal Dual Method problem
     */
    public BasicPhaseILPPDM(final LPPrimalDualMethod originalP) {
        super();
        this.originalProblem = originalP;
        this.originalDim = originalP.getDim();
        this.dim = originalP.getDim() + 1;// variable Y=(X, s)
    }

    /**
     * Find a feasible initial point
     * 
     * @return vector with the point
     * @throws PatriusException if an error occurs
     */
    public RealVector findFeasibleInitialPoint() throws PatriusException {

        final LPOptimizationRequest or = new LPOptimizationRequest();

        // objective function: s
        final RealVector c = new ArrayRealVector(dim);
        c.setEntry(dim - 1, 1.);
        or.setC(c);
        or.setToleranceFeas(originalProblem.getToleranceFeas());
        or.setTolerance(originalProblem.getTolerance());

        // Equality constraints: add a final zeroes column
        final RealMatrix matAEorig = originalProblem.getA();
        final RealVector vecBEorig = originalProblem.getB();
        if (matAEorig != null) {
            final RealMatrix zeroCols = new BlockRealMatrix(matAEorig.getRowDimension(), 1);
            final RealMatrix[][] parts = new RealMatrix[][] { { matAEorig, zeroCols } };
            final RealMatrix matAE = AlgebraUtils.composeMatrix(parts);
            final RealVector vecBE = vecBEorig;
            or.setA(matAE);
            or.setB(vecBE);
        }

        // initial point
        RealVector x0 = originalProblem.getNotFeasibleInitialPoint();
        if (x0 == null) {
            if (matAEorig != null) {
                x0 = findOneRoot(matAEorig, vecBEorig);
            } else {
                x0 = new ArrayRealVector(originalProblem.getDim(), 1. / originalProblem.getDim());
            }
        }

        // check primal norm
        if (matAEorig != null) {
            final RealVector originalRPriX0 = AlgebraUtils.zMult(matAEorig, x0, vecBEorig, -1);
            final double norm = originalRPriX0.getNorm();
            if (norm > originalProblem.getToleranceFeas()) {
                //The initial point for Basic Phase I Method must be equalities-feasible
                throw new PatriusException(PatriusMessages.INITIAL_POINT_NOT_FEASIBLE); 
            }
        }

        final RealVector originalFiX0 = originalProblem.getFi(x0);

        // lucky strike?
        int maxIneqIndex = originalFiX0.getMaxIndex();
        if (originalFiX0.getEntry(maxIneqIndex) + originalProblem.getTolerance() < 0) {
            // the given notFeasible starting point is in fact already feasible
            return x0;
        }

        RealVector initialPoint = new ArrayRealVector(1, MathLib.sqrt(originalProblem.getToleranceFeas()));
        initialPoint = x0.append(initialPoint);
        double s0 = initialPoint.getEntry(dim - 1);
        for (int i = 0; i < originalFiX0.getDimension(); i++) {
            s0 = MathLib.max(s0, originalFiX0.getEntry(i) * SCALAR);
        }
        initialPoint.setEntry(dim - 1, s0);
        or.setInitialPoint(initialPoint.toArray());

        // optimization
        final LPPrimalDualMethod opt = new PhaseILPPrimalDualMethod(2 * s0);// the bounds of this problem
        // are not used
        opt.setKKTSolver(new UpperDiagonalHKKTSolver(originalDim));
        opt.setOptimizationRequest(or);
        if (opt.optimizePresolvedStandardLP() == OptimizationResponse.FAILED) {
            // failed to find an initial feasible point
            throw new PatriusException(PatriusMessages.INITIAL_POINT_FAILED); 
        }
        final LPOptimizationResponse response = opt.getLPOptimizationResponse();
        final RealVector sol = new ArrayRealVector(response.getSolution());
        final RealVector ret = sol.getSubVector(0, originalDim);
        final RealVector ineq = originalProblem.getFi(ret);
        maxIneqIndex = ineq.getMaxIndex();
        if (ineq.getEntry(maxIneqIndex) >= 0) {
            // infeasible problem
            throw new PatriusException(PatriusMessages.INFEASIBLE_PROBLEM); 
        }

        return ret;
    }

    /**
     * Just looking for one out of all the possible solutions.
     * @param a matrix
     * @param b vector
     * @return feasible point
     * @throws PatriusException if an error occurs
     * @see "Convex Optimization, C.5 p. 681".
     */
    private RealVector findOneRoot(final RealMatrix a,
            final RealVector b) throws PatriusException {
        return originalProblem.findEqFeasiblePoint(a, b);
    }

    /**
     * Class Phase I LPPrimalDualMethod
     */
    private class PhaseILPPrimalDualMethod extends LPPrimalDualMethod {

        /** Maximum s value */
        private final double maxSValue;

        /**
         * We need this constructor because this class is used to solve a problem
         * that is not strictly standard (the introduction of the variable s)
         * transforms the original bounds in classical inequalities constraints.
         * This is a little forcing but we can go on anyway.
         * @param maxS max s value
         */
        PhaseILPPrimalDualMethod(final double maxS) {
            super();
            this.maxSValue = maxS;
            this.dim = originalProblem.getDim() + 1;
            this.meq = originalProblem.getMeq();
            this.mieq = originalProblem.getMieq() + 1;// the variable s has an upper bound
        }

        /**
         * Inequality functions values at X.
         * This is (-x+lb-s) for all original bounded lb
         * and (x-ub-s) for all original bounded ub
         * ans (s - maxSValue)
         */
        @Override
        protected RealVector getFi(final RealVector vecXs) {
            final double[] ret = new double[originalProblem.getMieq() + 1];
            final double s = vecXs.getEntry(getDim() - 1);
            // loop on the original bounded bounds
            for (int i = 0; i < originalProblem.getDim(); i++) {
                ret[i] = -vecXs.getEntry(i) - s + originalProblem.getLb().getEntry(i);// -x -s +lb < 0
                ret[originalProblem.getDim() + i] = vecXs.getEntry(i) - 
                        s - originalProblem.getUb().getEntry(i);// x -s -ub
                                                                                                             // < 0
            }
            ret[originalProblem.getMieq()] = s - this.maxSValue;

            return new ArrayRealVector(ret);
        }

        /** {@inheritDoc} */
        @Override
        protected RealVector rDual(final RealVector gradF0X,
                final RealVector l,
                final RealVector v) {

            // take into account the inequalities given by the original bounds
            // part 1
            RealVector m1 = new ArrayRealVector(getDim());
            for (int i = 0; i < originalProblem.getDim(); i++) {
                double m = 0;
                m += -l.getEntry(i);
                m += l.getEntry(originalProblem.getDim() + i);
                m1.setEntry(i, m);
            }

            // part 2
            // take into account the last column of -1 terms
            double d = 0d;
            for (int i = 0; i < originalProblem.getMieq(); i++) {
                d += l.getEntry(i);
            }
            m1.setEntry(getDim() - 1, m1.getEntry(getDim() - 1) - d);

            // take into account gradF0X
            m1 = m1.add(gradF0X);

            // take into account the upper bound on s: s < maxSValue
            final double m = l.getEntry(l.getDimension() - 1);
            m1.setEntry(getDim() - 1, m1.getEntry(getDim() - 1) + m);

            if (getMeq() == 0) {
                return m1;
            }
            return AlgebraUtils.zMultTranspose(getA(), v, m1, 1.);
        }

        /** {@inheritDoc} */
        @Override
        protected RealVector gradSum(final double t,
                final RealVector fiX) {
            // creates the vector to return
            final RealVector gradSum = new ArrayRealVector(getDim());
            double ddim = 0;
            // calculates the second term of the first row 
            for (int i = 0; i < originalProblem.getDim(); i++) {
                double di = 0;
                final double fiXL = fiX.getEntry(i);
                di += 1. / (t * fiXL);
                ddim += 1. / (t * fiXL);
                final double fiXU = fiX.getEntry(originalProblem.getDim() + i);
                di += -1. / (t * fiXU);
                ddim += 1. / (t * fiXU);
                gradSum.setEntry(i, di);
            }
            final double fsU = fiX.getEntry(fiX.getDimension() - 1);
            ddim += -1. / (t * fsU);// given by s < maxSValue
            gradSum.setEntry(getDim() - 1, ddim);

            return gradSum; //vector with second term of the first row 
        }

        /**
         * This is the third addendum of (11.56).
         * Returns only the subdiagonal elements in a real matrix.
         * Remember that the inequality functions are f[i] - s,
         * (i.e. (-x+lb-s) for all original bounded lb and (x-ub-s) for all original bounded ub)
         * plus the upper bound for s
         */
        @Override
        protected RealMatrix gradLSum(final RealVector l,
                final RealVector fiX) {
            // creates the matrix to return
            final BlockRealMatrix gradLSum = new BlockRealMatrix(getDim(), getDim());
            double ddimdim = 0;
            // counts for the original bound terms
            for (int i = 0; i < originalProblem.getDim(); i++) {
                double dii = 0;
                double didim = 0;
                final double fiXL = fiX.getEntry(i);
                final double lI = l.getEntry(i);
                final double lIFiXL = -lI / fiXL;
                dii += lIFiXL;
                didim += lIFiXL;
                ddimdim += lIFiXL;
                final double lDimI = l.getEntry(originalProblem.getDim() + i);
                final double fiXU = fiX.getEntry(originalProblem.getDim() + i);
                final double lDimIFiXU = -lDimI / fiXU;
                dii += lDimIFiXU;
                didim -= lDimIFiXU;
                ddimdim += lDimIFiXU;
                gradLSum.setEntry(i, i, dii);
                gradLSum.setEntry(getDim() - 1, i, didim);
            }

            // counts for the new upper bound on s
            final double lDimS = l.getEntry(l.getDimension() - 1);
            final double fsU = fiX.getEntry(fiX.getDimension() - 1);
            final double lDimSFsU = -lDimS / fsU;
            // sum
            ddimdim += lDimSFsU;

            gradLSum.setEntry(getDim() - 1, getDim() - 1, ddimdim); // set the subdiagonal elements

            return gradLSum; //returns the subdiagonal elements
        }

        /** {@inheritDoc} */
        @Override
        protected RealVector gradFiStepX(final RealVector stepX) {

            final RealVector ret = new ArrayRealVector(getMieq());
            for (int i = 0; i < originalProblem.getDim(); i++) {
                ret.setEntry(i, -stepX.getEntry(i) - stepX.getEntry(getDim() - 1));
                ret.setEntry(originalProblem.getDim() + i, stepX.getEntry(i) - stepX.getEntry(getDim() - 1));
            }
            // counts for the new upper bound on s
            ret.setEntry(getMieq() - 1, stepX.getEntry(getDim() - 1));

            return ret;
        }

        /**
         * Check custom exit conditions
         */
        @Override
        protected boolean checkCustomExitConditions(final RealVector xS) {
            // Inequality
            final RealVector x = xS.getSubVector(0, getDim() - 1);
            final RealVector ineqX = originalProblem.getFi(x);
            final int ineqMaxIndex = ineqX.getMaxIndex();

            final boolean isInternal = (ineqX.getEntry(ineqMaxIndex) + getTolerance() < 0)
                    || xS.getEntry(xS.getDimension() - 1) < 0;
            if (!isInternal) {
                // Immediate return
                return false;
            }

            RealVector originalRPriX = new ArrayRealVector(0);
            if (getA() != null) {
                originalRPriX = AlgebraUtils.zMult(originalProblem.getA(), x, originalProblem.getB(), -1);
            }
            final boolean isPrimalFeas = originalRPriX.getNorm() < originalProblem.getToleranceFeas();

            // Return result
            return isInternal && isPrimalFeas;
        }

        /**
         * Return the lower bounds for the problem.
         * It is always null because only the bounds of the original problem are used.
         */
        @Override
        protected RealVector getLb() {
            return null;
        }

        /**
         * Return the lower bounds for the problem.
         * It is always null because only the bounds of the original problem are used.
         */
        @Override
        protected RealVector getUb() {
            return null;
        }
    }
}
