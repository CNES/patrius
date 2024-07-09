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
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.BarrierFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LinearMultivariateRealFunction;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Basic Phase I Method (implemented as a Barried Method).
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 579"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * 
 * @since 4.6
 */
public class BasicPhaseIBM {

    /** Barrier method for the original problem */
    private final BarrierMethod originalProblem;
    /** Original dimension */
    private final int originalDim;
    /** Dimension */
    private final int dim;

    /**
     * Constructor used to set the barrier method and the dimension
     * @param originalProb the barrier method
     */
    public BasicPhaseIBM(final BarrierMethod originalProb) {
        this.originalProblem = originalProb;
        this.originalDim = originalProb.getDim();
        this.dim = originalProb.getDim() + 1;// variable Y=(X, s)
    }
    
    /**
     * Find a feasible initial point
     * @return originalSolution
     * @throws PatriusException if an error occurs
     */
    public double[] findFeasibleInitialPoint() throws PatriusException {

        final OptimizationRequest or = new OptimizationRequest();

        // objective function: s
        final RealVector c = new ArrayRealVector(dim);
        c.setEntry(dim - 1, 1.);
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c.toArray(), 0);
        or.setF0(objectiveFunction);
        or.setToleranceFeas(originalProblem.getToleranceFeas());
        or.setTolerance(originalProblem.getTolerance());
        or.setCheckKKTSolutionAccuracy(originalProblem.isCheckKKTSolutionAccuracy());

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
            if (originalRPriX0.getNorm() > originalProblem.getToleranceFeas()) {
                // The initial point for Basic Phase I Method must be equalities-feasible
                throw new PatriusException(PatriusMessages.INITIAL_POINT_NOT_FEASIBLE);
            }
        }

        final double s = originalProblem.getBarrierFunction().calculatePhase1InitialFeasiblePoint(x0.toArray(),
                originalProblem.getToleranceFeas());

        // lucky strike?
        if (s < 0) {
            // the given notFeasible starting point is already feasible
            return x0.toArray();
        }

        RealVector initialPoint = new ArrayRealVector(1, s);
        initialPoint = x0.append(initialPoint);
        or.setInitialPoint(initialPoint.toArray());

        // optimization
        final BarrierFunction bfPh1 = originalProblem.getBarrierFunction().createPhase1BarrierFunction();
        final BarrierMethod opt = new PhaseIBarrierMethod(bfPh1);
        opt.setOptimizationRequest(or);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            // Failed to find an initial feasible point
            throw new PatriusException(PatriusMessages.INITIAL_POINT_FAILED);
        }
        final OptimizationResponse response = opt.getOptimizationResponse();
        final double[] originalSolution = new double[originalDim];
        System.arraycopy(response.getSolution(), 0, originalSolution, 0, originalDim);
        if (Double.isNaN(originalProblem.getBarrierFunction().value(originalSolution))) {
            // Infeasible problem
            throw new PatriusException(PatriusMessages.INFEASIBLE_PROBLEM);
        }

        return originalSolution;
    }

    /**
     * Just looking for one out of all the possible solutions.
     * @param a matrix
     * @param b vector
     * @return RealVector with the possible solutions
     * @throws PatriusException if an error occurs 
     * @see "Convex Optimization, C.5 p. 681".
     */
    private RealVector findOneRoot(final RealMatrix a,
            final RealVector b) throws PatriusException {
        return originalProblem.findEqFeasiblePoint(a, b);
    }

    /**
     * Class Phase I barrier method
     */
    private class PhaseIBarrierMethod extends BarrierMethod {
        
        /**
         * Constructor used to set the barrier function
         * @param barrierF barrier function
         */
        public PhaseIBarrierMethod(final BarrierFunction barrierF) {
            super(barrierF);
        }

        /**
         * Check the custom exit conditions
         */
        @Override
        protected boolean checkCustomExitConditions(final RealVector y) {
            final RealVector x = y.getSubVector(0, getDim() - 1);

            // equalities
            RealVector originalRPriX = new ArrayRealVector(0);
            if (getA() != null) {
                originalRPriX = AlgebraUtils.zMult(originalProblem.getA(), x, originalProblem.getB(), -1.);
            }
            final boolean b2 = originalRPriX.getNorm() < originalProblem.getToleranceFeas();

            // inequalities
            final boolean b1 = !Double.isNaN(originalProblem.getBarrierFunction().value(x.toArray()))
                    || y.getEntry(y.getDimension() - 1) < 0;

            return b1 && b2;
        }
    }
}
