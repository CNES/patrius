/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.DiagonalMatrix;
import fr.cnes.sirius.patrius.math.linear.EigenDecomposition;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.JacobianMultivariateVectorOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Base class for implementing least-squares optimizers.
 * It provides methods for error estimation.
 * 
 * @version $Id: AbstractLeastSquaresOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public abstract class AbstractLeastSquaresOptimizer
    extends JacobianMultivariateVectorOptimizer {
    /** Square-root of the weight matrix. */
    private RealMatrix weightMatrixSqrt;
    /** Cost value (square root of the sum of the residuals). */
    private double cost;

    /**
     * @param checker
     *        Convergence checker.
     */
    protected AbstractLeastSquaresOptimizer(final ConvergenceChecker<PointVectorValuePair> checker) {
        super(checker);
    }

    /**
     * Computes the weighted Jacobian matrix.
     * 
     * @param params
     *        Model parameters at which to compute the Jacobian.
     * @return the weighted Jacobian: W<sup>1/2</sup> J.
     * @throws DimensionMismatchException
     *         if the Jacobian dimension does not
     *         match problem dimension.
     */
    protected RealMatrix computeWeightedJacobian(final double[] params) {
        return this.weightMatrixSqrt.multiply(MatrixUtils.createRealMatrix(this.computeJacobian(params), false));
    }

    /**
     * Computes the cost.
     * 
     * @param residuals
     *        Residuals.
     * @return the cost.
     * @see #computeResiduals(double[])
     */
    protected double computeCost(final double[] residuals) {
        final ArrayRealVector r = new ArrayRealVector(residuals);
        return MathLib.sqrt(r.dotProduct(this.getWeight().operate(r)));
    }

    /**
     * Gets the root-mean-square (RMS) value.
     * 
     * The RMS the root of the arithmetic mean of the square of all weighted
     * residuals.
     * This is related to the criterion that is minimized by the optimizer
     * as follows: If <em>c</em> if the criterion, and <em>n</em> is the
     * number of measurements, then the RMS is <em>sqrt (c/n)</em>.
     * 
     * @return the RMS value.
     */
    public double getRMS() {
        return MathLib.sqrt(this.getChiSquare() / this.getTargetSize());
    }

    /**
     * Get a Chi-Square-like value assuming the N residuals follow N
     * distinct normal distributions centered on 0 and whose variances are
     * the reciprocal of the weights.
     * 
     * @return chi-square value
     */
    public double getChiSquare() {
        return this.cost * this.cost;
    }

    /**
     * Gets the square-root of the weight matrix.
     * 
     * @return the square-root of the weight matrix.
     */
    public RealMatrix getWeightSquareRoot() {
        return this.weightMatrixSqrt.copy();
    }

    /**
     * Sets the cost.
     * 
     * @param costIn
     *        Cost value.
     */
    protected void setCost(final double costIn) {
        this.cost = costIn;
    }

    /**
     * Get the covariance matrix of the optimized parameters. <br/>
     * Note that this operation involves the inversion of the <code>J<sup>T</sup>J</code> matrix, where {@code J} is the
     * Jacobian matrix.
     * The {@code threshold} parameter is a way for the caller to specify
     * that the result of this computation should be considered meaningless,
     * and thus trigger an exception.
     * 
     * @param params
     *        Model parameters.
     * @param threshold
     *        Singularity threshold.
     * @return the covariance matrix.
     * @throws fr.cnes.sirius.patrius.math.linear.SingularMatrixException
     *         if the covariance matrix cannot be computed (singular problem).
     */
    public double[][] computeCovariances(final double[] params,
                                         final double threshold) {
        // Set up the Jacobian.
        final RealMatrix j = this.computeWeightedJacobian(params);

        // Compute transpose(J)J.
        final RealMatrix jTj = j.transpose().multiply(j);

        // Compute the covariances matrix.
        final DecompositionSolver solver = new QRDecomposition(jTj, threshold).getSolver();
        return solver.getInverse().getData(false);
    }

    /**
     * Computes an estimate of the standard deviation of the parameters. The
     * returned values are the square root of the diagonal coefficients of the
     * covariance matrix, {@code sd(a[i]) ~= sqrt(C[i][i])}, where {@code a[i]} is the optimized value of the {@code i}
     * -th parameter, and {@code C} is
     * the covariance matrix.
     * 
     * @param params
     *        Model parameters.
     * @param covarianceSingularityThreshold
     *        Singularity threshold (see {@link #computeCovariances(double[],double) computeCovariances}).
     * @return an estimate of the standard deviation of the optimized parameters
     * @throws fr.cnes.sirius.patrius.math.linear.SingularMatrixException
     *         if the covariance matrix cannot be computed.
     */
    public double[] computeSigma(final double[] params,
                                 final double covarianceSingularityThreshold) {
        final int nC = params.length;
        final double[] sig = new double[nC];
        final double[][] cov = this.computeCovariances(params, covarianceSingularityThreshold);
        for (int i = 0; i < nC; ++i) {
            sig[i] = MathLib.sqrt(cov[i][i]);
        }
        return sig;
    }

    /**
     * {@inheritDoc}
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.MaxEval}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.InitialGuess}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.SimpleBounds}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Target}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunction}</li>
     *        <li>{@link fr.cnes.sirius.patrius.math.optim.nonlinear.vector.ModelFunctionJacobian}</li>
     *        </ul>
     * @return {@inheritDoc}
     * @throws TooManyEvaluationsException
     *         if the maximal number of
     *         evaluations is exceeded.
     * @throws DimensionMismatchException
     *         if the initial guess, target, and weight
     *         arguments have inconsistent dimensions.
     */
    @Override
    public PointVectorValuePair optimize(final OptimizationData... optData) {
        // Retrieve settings.
        this.parseOptimizationData(optData);
        // Set up base class and perform computation.
        return super.optimize(optData);
    }

    /**
     * Computes the residuals.
     * The residual is the difference between the observed (target)
     * values and the model (objective function) value.
     * There is one residual for each element of the vector-valued
     * function.
     * 
     * @param objectiveValue
     *        Value of the the objective function. This is
     *        the value returned from a call to {@link #computeObjectiveValue(double[]) computeObjectiveValue} (whose
     *        array argument contains the model parameters).
     * @return the residuals.
     * @throws DimensionMismatchException
     *         if {@code params} has a wrong
     *         length.
     */
    protected double[] computeResiduals(final double[] objectiveValue) {
        final double[] target = this.getTarget();
        if (objectiveValue.length != target.length) {
            throw new DimensionMismatchException(target.length,
                objectiveValue.length);
        }

        final double[] residuals = new double[target.length];
        for (int i = 0; i < target.length; i++) {
            residuals[i] = target[i] - objectiveValue[i];
        }

        return residuals;
    }

    /**
     * Scans the list of (required and optional) optimization data that
     * characterize the problem.
     * If the weight matrix is specified, the {@link #weightMatrixSqrt} field is recomputed.
     * 
     * @param optData
     *        Optimization data. The following data will be looked for:
     *        <ul>
     *        <li>{@link Weight}</li>
     *        </ul>
     */
    private void parseOptimizationData(final OptimizationData... optData) {
        // The existing values (as set by the previous call) are reused if
        // not provided in the argument list.
        for (final OptimizationData data : optData) {
            if (data instanceof Weight) {
                this.weightMatrixSqrt = this.squareRoot(((Weight) data).getWeight());
                // If more data must be parsed, this statement _must_ be
                // changed to "continue".
                break;
            }
        }
    }

    /**
     * Computes the square-root of the weight matrix.
     * 
     * @param m
     *        Symmetric, positive-definite (weight) matrix.
     * @return the square-root of the weight matrix.
     */
    private RealMatrix squareRoot(final RealMatrix m) {
        if (m instanceof DiagonalMatrix) {
            // initialize dim with the row count of m
            final int dim = m.getRowDimension();
            // new instance of a diagonal matrix
            final RealMatrix sqrtM = new DiagonalMatrix(dim);
            // evaluate each entry of the matrix
            for (int i = 0; i < dim; i++) {
                sqrtM.setEntry(i, i, MathLib.sqrt(m.getEntry(i, i)));
            }
            return sqrtM;
        } else {
            final EigenDecomposition dec = new EigenDecomposition(m);
            return dec.getSquareRoot();
        }
    }
}
