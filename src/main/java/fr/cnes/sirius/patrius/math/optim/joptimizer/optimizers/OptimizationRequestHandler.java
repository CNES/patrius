/**
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
* HISTORY
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
* END-HISTORY
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

import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.linear.SingularValueDecomposition;
import fr.cnes.sirius.patrius.math.optim.joptimizer.algebra.AlgebraUtils;
import fr.cnes.sirius.patrius.math.optim.joptimizer.functions.ConvexMultivariateRealFunction;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Generic class for optimization process.
 * 
 * @since 4.6
 */
public class OptimizationRequestHandler {

    /** Scalar */
    public static final double SCALAR = 0.99;
    /** Maximum number of iterations */
    public static final double MAX_ITERATIONS = 500;
    /** optimization request handler */
    protected OptimizationRequestHandler successor = null;
    /** optimization request */
    protected OptimizationRequest request;
    /** optimization response */
    protected OptimizationResponse response;
    /** dimension */
    protected int dim = -1;
    /** number of equalities */
    protected int meq = -1;
    /** number of inequalities */
    protected int mieq = -1;
    /** Transposed matrix A */
    private RealMatrix aT = null;

    /**
     * Protected constructor
     */
    protected OptimizationRequestHandler() {
        super();
    }

    /**
     * Set the optimization request
     * @param req request
     */
    public void setOptimizationRequest(final OptimizationRequest req) {
        this.request = req;
    }

    /**
     * Get the optimization request
     * @return request
     */
    protected OptimizationRequest getOptimizationRequest() {
        return this.request;
    }

    /**
     * Set the optimization response
     * @param res response
     */
    protected void setOptimizationResponse(final OptimizationResponse res) {
        this.response = res;
    }

    /**
     * Get the optimization response
     * @return response
     */
    public OptimizationResponse getOptimizationResponse() {
        return this.response;
    }

    /**
     * Optimizer
     * @return retCode
     * @throws PatriusException if an error occurs
     */
    public int optimize() throws PatriusException {
        return forwardOptimizationRequest();
    }

    /**
     * Forward optimization request
     * @return retCode
     * @throws PatriusException if an error occurs
     */
    protected int forwardOptimizationRequest() throws PatriusException {
        if (successor != null) {
            successor.setOptimizationRequest(request);
            final int retCode = successor.optimize();
            this.response = successor.getOptimizationResponse();
            return retCode;
        }
        throw new PatriusException(PatriusMessages.FAILED_PROBLEM);
    }

    /**
     * Get the number of variables.
     * @return dimension
     */
    protected final int getDim() {
        if (dim < 0) {
            dim = this.request.getF0().getDim();
        }
        return dim;
    }

    /**
     * Get the number of equalities.
     * @return number
     */
    protected final int getMeq() {
        if (meq < 0) {
            if (this.request.getA() == null) {
                meq = 0;
            } else {
                meq = this.request.getA().getRowDimension();
            }
        }
        return meq;
    }

    /**
     * Get the number of inequalities.
     * @return number
     */
    protected final int getMieq() {
        if (mieq < 0) {
            mieq = getFi().length;
        }
        return mieq;
    }

    /**
     * Get the initial point
     * @return point
     */
    protected RealVector getInitialPoint() {
        return request.getInitialPoint();
    }

    /**
     * Get the not feasible initial point
     * @return point
     */
    protected RealVector getNotFeasibleInitialPoint() {
        return request.getNotFeasibleInitialPoint();
    }

    /**
     * Get the initial Lagrangian
     * @return Lagrangian
     */
    protected RealVector getInitialLagrangian() {
        return request.getInitialLagrangian();
    }

    /**
     * Get the equalities constraints matrix
     * @return matrix
     */
    protected final RealMatrix getA() {
        return request.getA();
    }

    /**
     * Get the equalities constraints matrix transposed
     * @return transposed matrix
     */
    protected final RealMatrix getAT() {
        if (aT == null && getA() != null) {
            aT = getA().transpose();

        }
        return aT;
    }

    /**
     * Get the equalities constraints vector
     * @return vector
     */
    protected final RealVector getB() {
        return request.getB();
    }

    /**
     * Get the maximum number of iteration in the search algorithm
     * @return max number
     */
    protected final int getMaxIteration() {
        return request.getMaxIteration();
    }

    /**
     * Get the tolerance for the minimum value
     * @return tolerance
     */
    protected final double getTolerance() {
        return request.getTolerance();
    }

    /**
     * Get the tolerance for the constraints satisfaction
     * @return tolerance
     */
    protected final double getToleranceFeas() {
        return request.getToleranceFeas();
    }

    /**
     * Get the tolerance for inner iterations in the barrier-method.
     * @return tolerance
     */
    protected final double getToleranceInnerStep() {
        return request.getToleranceInnerStep();
    }

    /**
     * Get the calibration parameter for line search
     * @return alpha
     */
    protected final double getAlpha() {
        return request.getAlpha();
    }

    /**
     * Get the calibration parameter for line search
     * @return beta
     */
    protected final double getBeta() {
        return request.getBeta();
    }

    /**
     * Get the calibration parameter for line search
     * @return mu
     */
    protected final double getMu() {
        return request.getMu();
    }

    /**
     * If true, a progress in the relevant algorithm norms
     * is required during iterations, otherwise the iteration
     * will be exited with a warning
     * @return true/false
     */
    protected final boolean isCheckProgressConditions() {
        return request.isCheckProgressConditions();
    }

    /**
     * Check the accuracy of the solution of KKT system during
     * iterations. If true, every inversion of the system must
     * have an accuracy that satisfy the given toleranceKKT
     * @return true/false
     */
    protected final boolean isCheckKKTSolutionAccuracy() {
        return request.isCheckKKTSolutionAccuracy();
    }

    /**
     * Get the acceptable tolerance for KKT system resolution
     * @return tolerance
     */
    protected final double getToleranceKKT() {
        return request.getToleranceKKT();
    }

    /**
     * Get the chosen interior point method.
     * @return point
     */
    protected final String getInteriorPointMethod() {
        return request.getInteriorPointMethod();
    }

    /**
     * Objective function.
     * @return objective funtion
     */
    protected final ConvexMultivariateRealFunction getF0() {
        return request.getF0();
    }

    /**
     * Objective function domain.
     * @param x vector X
     * @return true/false
     */
    protected boolean isInDomainF0(final RealVector x) {
        final double f0X = request.getF0().value(x.toArray());
        return !Double.isInfinite(f0X) && !Double.isNaN(f0X);
    }

    /**
     * Objective function value at X.
     * @param x value X
     * @return objective function
     */
    protected double getF0(final RealVector x) {
        return request.getF0().value(x.toArray());
    }

    /**
     * Objective function gradient at X.
     * @param x gradient X
     * @return objective function
     */
    protected RealVector getGradF0(final RealVector x) {
        return new ArrayRealVector(request.getF0().gradient(x.toArray()));
    }

    /**
     * Objective function hessian at X.
     * @param x hessian X
     * @return objective function
     */
    protected RealMatrix getHessF0(final RealVector x) {
        final double[][] hess = request.getF0().hessian(x.toArray());
        if (hess == null) {
            return new BlockRealMatrix(x.getDimension(), x.getDimension());
        } else {
            return new BlockRealMatrix(hess);
        }
    }

    /**
     * Inequality functions.
     * @return inequalities constraints array
     */
    protected ConvexMultivariateRealFunction[] getFi() {
        return request.getFi();
    }

    /**
     * Inequality functions values at X.
     * @param vecX values X
     * @return inequality function
     */
    protected RealVector getFi(final RealVector vecX) {
        // inequalities constraints array
        final ConvexMultivariateRealFunction[] fis = request.getFi();
        if (fis == null) {
            return null;
        }
        final double[] ret = new double[fis.length];
        final double[] x = vecX.toArray();
        for (int i = 0; i < fis.length; i++) {
            final ConvexMultivariateRealFunction fi = fis[i];
            // inequalities values
            final double fix = fi.value(x);
            ret[i] = fix;
        }
        // RealVector with the inequalities values
        return new ArrayRealVector(ret);
    }

    /**
     * Inequality functions gradients values at X.
     * @param vecX gradients X
     * @return inequality function
     */
    protected RealMatrix getGradFi(final RealVector vecX) {
        final RealMatrix ret = new BlockRealMatrix(request.getFi().length, vecX.getDimension());
        final double[] x = vecX.toArray();
        for (int i = 0; i < request.getFi().length; i++) {
            ret.setRow(i, request.getFi()[i].gradient(x));
        }
        return ret;
    }

    /**
     * Inequality functions hessians values at X.
     * @param vecX hessians X
     * @return inequality function
     */
    protected RealMatrix[] getHessFi(final RealVector vecX) {
        // result matrix, dimension of the inequalities constraints array
        final RealMatrix[] ret = new RealMatrix[request.getFi().length];
        final double[] x = vecX.toArray();
        for (int i = 0; i < request.getFi().length; i++) {
            // take the hessian value
            final double[][] hess = request.getFi()[i].hessian(x);
            if (hess == null) {
                ret[i] = null;
            } else {
                ret[i] = new BlockRealMatrix(hess);
            }
        }
        // return a RealMatrix with the hessian inequalities
        return ret;
    }

    /**
     * Overriding this, a subclass can define some extra condition for exiting the iteration loop.
     * @param y vector
     * @return false
     */
    protected boolean checkCustomExitConditions(final RealVector y) {
        return false;
    }

    /**
     * Find a solution of the linear (equalities) system A.x = b.
     * A is a pxn matrix, with rank(A) = p < n.
     * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 682"
     *      NB: we are waiting for Csparsej to fix its qr decomposition issues.
     * @TODO: sign this method with more restrictive class parameters
     * @param aMatrix matrix A
     * @param bVector vector B
     * @return solution
     * @throws PatriusRuntimeException if an error occurs
     */
    protected RealVector findEqFeasiblePoint(final RealMatrix aMatrix,
            final RealVector bVector) throws PatriusRuntimeException {

        final int p = aMatrix.getRowDimension();
        final int m = aMatrix.getColumnDimension();
        if (m <= p) {
            throw new PatriusRuntimeException(PatriusMessages.FAILED_RANK, null);
        }
        return findEqFeasiblePoint2(aMatrix, bVector);
    }

    /**
     * Find a solution of the linear (equalities) system A.x = b.
     * A is a pxn matrix, with rank(A) = p < n.
     * @see "S.Boyd and L.Vandenberghe, Convex Optimization, p. 682"
     * @TODO: sign this method with more restrictive class parameters
     * @param aMatrix matrix A
     * @param bVector vector B
     * @return solution
     * @throws PatriusRuntimeException if an error occurs
     */
    protected RealVector findEqFeasiblePoint2(final RealMatrix aMatrix,
            final RealVector bVector) throws PatriusRuntimeException {

        final int p = aMatrix.getRowDimension();
        final int m = aMatrix.getColumnDimension();
        if (m <= p) {
            // Equalities matrix A must be pxn with rank(A) = p < n
            throw new PatriusRuntimeException(PatriusMessages.FAILED_RANK, null);
        }

        // Transpose matrix A
        final RealMatrix at = new Array2DRowRealMatrix(aMatrix.getColumnDimension(),
                aMatrix.getRowDimension());
        for (int i = 0; i < aMatrix.getRowDimension(); i++) {
            for (int j = 0; j < aMatrix.getColumnDimension(); j++) {
                at.setEntry(j, i, aMatrix.getEntry(i, j));
            }
        }

        final SingularValueDecomposition dFact1 = new SingularValueDecomposition(at);
        final int rankAT = dFact1.getRank();
        if (rankAT != p) {
            // Equalities matrix A must have full rank: rankAT < p
            throw new PatriusRuntimeException(PatriusMessages.FAILED_FULL_RANK, null);
        }

        final QRDecomposition dFact = new QRDecomposition(at);

        // A = QR
        final RealMatrix q1q2 = dFact.getQ();
        final RealMatrix r0 = dFact.getR();
        final RealMatrix q1 = q1q2.getSubMatrix(0, at.getRowDimension() - 1, 0, p - 1);
        final RealMatrix r = r0.getSubMatrix(0, p - 1, 0, p - 1);

        // w = Q1 * Inv([R]T) . b
        double[] w = null;

        // solve R[T].x = b (Inv(R))[T] = Inv(R[T])
        final double[] x = new double[p];
        for (int i = 0; i < p; i++) {
            double sum = 0;
            for (int j = 0; j < i; j++) {
                sum += r.getEntry(j, i) * x[j];
            }
            x[i] = (bVector.getEntry(i) - sum) / r.getEntry(i, i);
        }
        w = q1.operate(x);
        // RealVector with solution
        return new ArrayRealVector(w);
    }

    /**
     * rPri := Ax - b
     * @param x vector
     * @return solution
     */
    protected RealVector rPri(final RealVector x) {
        if (getA() == null) {
            return new ArrayRealVector(0);
        }
        return AlgebraUtils.zMult(getA(), x, getB(), -1);
    }
}
