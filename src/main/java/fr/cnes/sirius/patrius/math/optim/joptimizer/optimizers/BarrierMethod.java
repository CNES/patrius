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
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.BarrierFunction;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Barrier method.
 * 
 * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 568"
 * @author alberto trivellato (alberto.trivellato@gmail.com)
* HISTORY
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
 * 
 * @since 4.6
 */
public class BarrierMethod extends OptimizationRequestHandler {

    /** Barrier function initialized to null */
    private final BarrierFunction barrierFunction;

    /**
     * Constructor used to set the barrier function
     * @param barrierF barrier function
     */
    public BarrierMethod(final BarrierFunction barrierF) {
        super();
        this.barrierFunction = barrierF;
    }

    /** {@inheritDoc} */
    //CHECKSTYLE: stop CyclomaticComplexity check
    //CHECKSTYLE: stop MethodLength check
    //CHECKSTYLE: stop CommentRatio check
    // Reason: complex JOptimizer code kept as such
    @Override
    public int optimize() throws PatriusException {
        //CHECKSTYLE: resume CommentRatio check
        //CHECKSTYLE: resume MethodLength check
        //CHECKSTYLE: resume CyclomaticComplexity check

        // Check if an initial point is provided
        RealVector x0 = getInitialPoint();
        if (x0 == null) {
            final RealVector x0NF = getNotFeasibleInitialPoint();
            if (x0NF != null) {
                final double rPriX0NFNorm = (rPri(x0NF)).getNorm();
                if (rPriX0NFNorm <= getToleranceFeas()
                        && !Double.isNaN(this.barrierFunction.value(x0NF.toArray()))) {
                    x0 = x0NF;
                }
            }
            // If initial point either not feasible initial point are provided,
            // find one
            if (x0 == null) {
                final BasicPhaseIBM bf1 = new BasicPhaseIBM(this);
                x0 = new ArrayRealVector(bf1.findFeasibleInitialPoint());
            }
        }

        // check X0 feasibility
        final double rPriX0Norm = (rPri(x0)).getNorm();
        if (Double.isNaN(this.barrierFunction.value(x0.toArray()))
                || rPriX0Norm > getToleranceFeas()) {
            // initial point must be strictly feasible
            // Exception
            throw new PatriusException(PatriusMessages.INITIAL_POINT_NOT_FEASIBLE);
        }

        // Temporary variables
        RealVector vecX = x0;
        final int dim = vecX.getDimension();
        double t = 1d;
        int outerIteration = 0;
        // Init response
        final OptimizationResponse response = new OptimizationResponse();
        // Iterations until convergence
        while (true) {
            outerIteration++;

            // Stopping criterion: quit if gap < tolerance.
            final double gap = this.barrierFunction.getDualityGap(t);
            if (gap <= getTolerance()) {
                break;
            }
            // custom exit condition
            if (checkCustomExitConditions(vecX)) {
                response.setReturnCode(OptimizationResponse.SUCCESS);
                break;
            }

            // Centering step: compute x*(t) by minimizing tf0 + phi (the barrier function), subject
            // to Ax = b, starting
            // at x.
            final double tIter = t;
            final ConvexMultivariateRealFunction newObjectiveFunction = new ConvexMultivariateRealFunction() {

                /** {@inheritDoc} */
                @Override
                public double value(final double[] val) {
                    final RealVector x = new ArrayRealVector(val);
                    final double phi = barrierFunction.value(val);
                    return tIter * getF0(x) + phi;
                }

               /** {@inheritDoc} */
                @Override
                public double[] gradient(final double[] val) {
                    final RealVector x = new ArrayRealVector(val);
                    final RealVector phiGrad = new ArrayRealVector(barrierFunction.gradient(val));
                    return getGradF0(x).mapMultiply(tIter).add(phiGrad).toArray();
                }

                /** {@inheritDoc} */
                @Override
                public double[][] hessian(final double[] val) {
                    final RealVector x = new ArrayRealVector(val);
                    final RealMatrix hessF0X = getHessF0(x);
                    final double[][] hessX = barrierFunction.hessian(val);
                    final RealMatrix phiHess = new BlockRealMatrix(hessX);
                    return hessF0X.scalarMultiply(tIter).add(phiHess).getData(false);
                }

                /** {@inheritDoc} */
                @Override
                public int getDim() {
                    return dim;
                }
            };

            // NB: cannot use the same request object for the inner step
            final OptimizationRequest or = new OptimizationRequest();
            // Set the data for the new request
            if (getA() != null) {
                or.setA(getA().getData());
            } else {
                or.setA((double[][]) null);
            }
            or.setAlpha(getAlpha());
            if (getB() != null) {
                or.setB(getB().toArray());
            } else {
                or.setB((double[]) null);
            }
            or.setBeta(getBeta());
            or.setCheckKKTSolutionAccuracy(isCheckKKTSolutionAccuracy());
            or.setCheckProgressConditions(isCheckProgressConditions());
            or.setF0(newObjectiveFunction);
            or.setInitialPoint(vecX.toArray());
            or.setMaxIteration(getMaxIteration());
            or.setMu(getMu());
            or.setTolerance(getToleranceInnerStep());
            or.setToleranceKKT(getToleranceKKT());

            final BarrierNewtonLEConstrainedFSP opt = new BarrierNewtonLEConstrainedFSP(true, this);
            opt.setOptimizationRequest(or);
            // If optimization fail --> stop
            if (opt.optimize() == OptimizationResponse.FAILED) {
                response.setReturnCode(OptimizationResponse.FAILED);
                break;
            }
            final OptimizationResponse newtonResponse = opt.getOptimizationResponse();

            // Update. x := x*(t).
            vecX = new ArrayRealVector(newtonResponse.getSolution());

            // Increase t: t := mu*t.
            t = getMu() * t;

            // iteration limit condition
            if (outerIteration == getMaxIteration()) {
                response.setReturnCode(OptimizationResponse.FAILED);
                break;
            }
        }

        response.setSolution(vecX.toArray());
        setOptimizationResponse(response);
        return response.getReturnCode(); // return 0, 1 or 2
    }

    /**
     * Use the barrier function instead.
     */
    @Override
    protected RealVector getFi(final RealVector x) {
        throw new UnsupportedOperationException();
    }

    /**
     * Use the barrier function instead.
     */
    @Override
    protected RealMatrix getGradFi(final RealVector x) {
        throw new UnsupportedOperationException();
    }

    /**
     * Use the barrier function instead.
     */
    @Override
    public RealMatrix[] getHessFi(final RealVector x) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the barrier function
     * @return barrier function
     */
    protected BarrierFunction getBarrierFunction() {
        return this.barrierFunction;
    }

    /** Class linear equality constrained newton barrier, with feasible starting point */
    private static class BarrierNewtonLEConstrainedFSP extends NewtonLEConstrainedFSP {
        /** Barrier method initialized to null */
        private final BarrierMethod father;

        /**
         * Constructor
         * @param activateChain true/false
         * @param f barrier function
         */
        public BarrierNewtonLEConstrainedFSP(final boolean activateChain, final BarrierMethod f) {
            super(activateChain);
            this.father = f;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean checkCustomExitConditions(final RealVector y) {
            return father.checkCustomExitConditions(y);
        }
    }
}
