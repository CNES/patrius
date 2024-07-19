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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.optim.joptimizer.optimizers;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.LinearMultivariateRealFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Basic Phase I Method (implemented as a Primal-Dual Method).
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 579"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public class BasicPhaseIPDM {
    
    /** Scalar */
    private static final double SCALAR = -0.5;
    /** Primal dual method original problem */
    private final PrimalDualMethod originalProblem;
    /** Original dimension */
    private final int originalDim;
    /** Dimension */
    private final int dim;

    /**
     * Constructor
     * 
     * @param originalP original problem 
     */
    public BasicPhaseIPDM(final PrimalDualMethod originalP) {
        this.originalProblem = originalP;
        this.originalDim = originalP.getDim();
        this.dim = originalP.getDim() + 1;// variable Y=(X, s)
    }

    /**
     * Find a feasible initial point
     * 
     * @return feasible initial point 
     * @throws PatriusException if an error occurs
     */
    //CHECKSTYLE: stop MethodLength check
    // Reason: complex JOptimizer code kept as such
    public RealVector findFeasibleInitialPoint() throws PatriusException {
        //CHECKSTYLE: resume MethodLength check

        final OptimizationRequest or = new OptimizationRequest();

        // objective function: s
        final RealVector c = new ArrayRealVector(this.dim);
        c.setEntry(this.dim - 1, 1.);
        final LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction(c.toArray(), 0);
        or.setF0(objectiveFunction);
        or.setToleranceFeas(this.originalProblem.getToleranceFeas());
        or.setTolerance(this.originalProblem.getTolerance());

        // Inequality constraints: fi(X)-s
        final ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[this.originalProblem
                .getMieq()];
        for (int i = 0; i < inequalities.length; i++) {

            final ConvexMultivariateRealFunction originalFi = this.originalProblem.getFi()[i];

            final ConvexMultivariateRealFunction fi = new ConvexMultivariateRealFunction() {

                /**
                 * Evaluation of the function at point X
                 */
                @Override
                public double value(final double[] val) {
                    final RealVector y = new ArrayRealVector(val);
                    final RealVector x = y.getSubVector(0, BasicPhaseIPDM.this.originalDim);
                    return originalFi.value(x.toArray()) - y.getEntry(BasicPhaseIPDM.this.dim - 1);
                }

                /**
                 * Function gradient at point X.
                 */
                @Override
                public double[] gradient(final double[] val) {
                    final RealVector y = new ArrayRealVector(val);
                    final RealVector x = y.getSubVector(0, BasicPhaseIPDM.this.originalDim);
                    final RealVector origGrad = new ArrayRealVector(originalFi.gradient(x.toArray()));
                    RealVector ret = new ArrayRealVector(1, -1);
                    ret = origGrad.append(ret);
                    return ret.toArray();
                }

                /**
                 * Function hessian at point X
                 */
                @Override
                public double[][] hessian(final double[] val) {
                    final RealVector y = new ArrayRealVector(val);
                    // get a part of the vector
                    final RealVector x = y.getSubVector(0, BasicPhaseIPDM.this.originalDim);
                    final double[][] originalFiHess = originalFi.hessian(x.toArray());
                    // if original hessian values are 0, returns array of 0
                    if (originalFiHess == null) {
                        return null;
                    }
                    final RealMatrix origHess;
                    origHess = new BlockRealMatrix(originalFi.hessian(x.toArray()));
                    final RealMatrix[][] parts =
                        new RealMatrix[][] { { origHess, null }, { null, new BlockRealMatrix(1, 1) } };
                    // return a matrix composed by the original hessian values and zeros
                    return AlgebraUtils.composeMatrix(parts).getData(false);
                }

                /**
                 * Get dimension
                 */
                @Override
                public int getDim() {
                    return BasicPhaseIPDM.this.dim;
                }
            };
            inequalities[i] = fi;
        }
        or.setFi(inequalities);

        // Equality constraints: add a final zeroes column
        final RealMatrix matAEorig = this.originalProblem.getA();
        final RealVector vecBEorig = this.originalProblem.getB();
        if (matAEorig != null) {
            final RealMatrix zeroCols = new BlockRealMatrix(matAEorig.getRowDimension(), 1);
            final RealMatrix[][] parts = new RealMatrix[][] { { matAEorig, zeroCols } };
            final RealMatrix matAE = AlgebraUtils.composeMatrix(parts);
            final RealVector vecBE = vecBEorig;
            or.setA(matAE);
            or.setB(vecBE);
        }

        // initial point
        RealVector x0 = this.originalProblem.getNotFeasibleInitialPoint();
        if (x0 == null) {
            if (matAEorig != null) {
                x0 = findOneRoot(matAEorig, vecBEorig);
            } else {
                x0 = new ArrayRealVector(this.originalProblem.getDim(), 1. / this.originalProblem.getDim());
            }
        }

        // check primal norm
        if (matAEorig != null) {
            final RealVector originalRPriX0 = AlgebraUtils.zMult(matAEorig, x0, vecBEorig, -1);
            final double norm = originalRPriX0.getNorm();
            if (norm > this.originalProblem.getToleranceFeas()) {
                // The initial point for Basic Phase I Method must be equalities-feasible
                throw new PatriusException(PatriusMessages.INITIAL_POINT_NOT_FEASIBLE); 
            }
        }

        final RealVector originalFiX0 = this.originalProblem.getFi(x0);

        // lucky strike?
        int maxIneqIndex = originalFiX0.getMaxIndex();
        if (originalFiX0.getEntry(maxIneqIndex) + this.originalProblem.getTolerance() < 0) {
            // the given notFeasible starting point is in fact already feasible
            return x0;
        }

        RealVector initialPoint = new ArrayRealVector(1, MathLib.sqrt(this.originalProblem.getToleranceFeas()));
        initialPoint = x0.append(initialPoint);
        for (int i = 0; i < originalFiX0.getDimension(); i++) {
            initialPoint.setEntry(
                this.dim - 1,
                MathLib.max(initialPoint.getEntry(this.dim - 1),
                    originalFiX0.getEntry(i) * MathLib.pow(this.originalProblem.getToleranceFeas(), SCALAR)));
        }
        or.setInitialPoint(initialPoint.toArray());

        // optimization
        final PrimalDualMethod opt = new PhaseIPrimalDualMethod();
        opt.setOptimizationRequest(or);
        if (opt.optimize() == OptimizationResponse.FAILED) {
            // Failed to find an initial feasible point
            throw new PatriusException(PatriusMessages.INITIAL_POINT_FAILED); 
        }
        final OptimizationResponse response = opt.getOptimizationResponse();
        final RealVector sol = new ArrayRealVector(response.getSolution());
        final RealVector ret = sol.getSubVector(0, this.originalDim);
        final RealVector ineq = this.originalProblem.getFi(ret); // inequalities
        maxIneqIndex = ineq.getMaxIndex(); // max inequality index

        if (ineq.getEntry(maxIneqIndex) >= 0) {
            // Infeasible problem
            throw new PatriusException(PatriusMessages.INFEASIBLE_PROBLEM); 
        }

        return ret;  //returns the feasible initial point
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
        return this.originalProblem.findEqFeasiblePoint(a, b);
    }

    /**
     * Class Phase I primal dual method
     */
    private class PhaseIPrimalDualMethod extends PrimalDualMethod {
        /**
         * Check custom exit conditions
         */
        @Override
        protected boolean checkCustomExitConditions(final RealVector y) {
            final RealVector x = y.getSubVector(0, getDim() - 1);
            // get the inequalities vector
            final RealVector ineqX = BasicPhaseIPDM.this.originalProblem.getFi(x);
            final int ineqMaxIndex = ineqX.getMaxIndex(); // max inequality value index
            // first check
            final boolean isInternal = (ineqX.getEntry(ineqMaxIndex) + getTolerance() < 0)
                    || y.getEntry(y.getDimension() - 1) < 0;
            if (!isInternal) {
                return false;
            }

            RealVector originalRPriX = new ArrayRealVector(0);
            if (getA() != null) {
                originalRPriX = AlgebraUtils.zMult(BasicPhaseIPDM.this.originalProblem.getA(), x,
                    BasicPhaseIPDM.this.originalProblem.getB(), -1);
            }
            // second check
            final boolean isPrimalFeas = originalRPriX.getNorm() < BasicPhaseIPDM.this.originalProblem
                .getToleranceFeas();

            return isInternal && isPrimalFeas;
        }
    }
}
