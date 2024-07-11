/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.regression;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.NonSquareMatrixException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Abstract base class for implementations of MultipleLinearRegression.
 * 
 * @version $Id: AbstractMultipleLinearRegression.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public abstract class AbstractMultipleLinearRegression implements
    MultipleLinearRegression {

    /** X sample data. */
    private RealMatrix xMatrix;

    /** Y sample data. */
    private RealVector yVector;

    /** Whether or not the regression model includes an intercept. True means no intercept. */
    private boolean noIntercept = false;

    /**
     * @return the X sample data.
     */
    protected RealMatrix getX() {
        return this.xMatrix;
    }

    /**
     * @return the Y sample data.
     */
    protected RealVector getY() {
        return this.yVector;
    }

    /**
     * @return true if the model has no intercept term; false otherwise
     * @since 2.2
     */
    public boolean isNoIntercept() {
        return this.noIntercept;
    }

    /**
     * @param noInterceptIn
     *        true means the model is to be estimated without an intercept term
     * @since 2.2
     */
    public void setNoIntercept(final boolean noInterceptIn) {
        this.noIntercept = noInterceptIn;
    }

    /**
     * <p>
     * Loads model x and y sample data from a flat input array, overriding any previous sample.
     * </p>
     * <p>
     * Assumes that rows are concatenated with y values first in each row. For example, an input <code>data</code> array
     * containing the sequence of values (1, 2, 3, 4, 5, 6, 7, 8, 9) with <code>nobs = 3</code> and
     * <code>nvars = 2</code> creates a regression dataset with two independent variables, as below:
     * 
     * <pre>
     *   y   x[0]  x[1]
     *   --------------
     *   1     2     3
     *   4     5     6
     *   7     8     9
     * </pre>
     * 
     * </p>
     * <p>
     * Note that there is no need to add an initial unitary column (column of 1's) when specifying a model including an
     * intercept term. If {@link #isNoIntercept()} is <code>true</code>, the X matrix will be created without an initial
     * column of "1"s; otherwise this column will be added.
     * </p>
     * <p>
     * Throws IllegalArgumentException if any of the following preconditions fail:
     * <ul>
     * <li><code>data</code> cannot be null</li>
     * <li><code>data.length = nobs * (nvars + 1)</li>
     * <li><code>nobs > nvars</code></li>
     * </ul>
     * </p>
     * 
     * @param data
     *        input data array
     * @param nobs
     *        number of observations (rows)
     * @param nvars
     *        number of independent variables (columns, not counting y)
     * @throws NullArgumentException
     *         if the data array is null
     * @throws DimensionMismatchException
     *         if the length of the data array is not equal
     *         to <code>nobs * (nvars + 1)</code>
     * @throws NumberIsTooSmallException
     *         if <code>nobs</code> is smaller than <code>nvars</code>
     */
    public void newSampleData(final double[] data, final int nobs, final int nvars) {
        if (data == null) {
            throw new NullArgumentException();
        }
        if (data.length != nobs * (nvars + 1)) {
            throw new DimensionMismatchException(data.length, nobs * (nvars + 1));
        }
        if (nobs <= nvars) {
            throw new NumberIsTooSmallException(nobs, nvars, false);
        }
        final double[] y = new double[nobs];
        final int cols = this.noIntercept ? nvars : nvars + 1;
        final double[][] x = new double[nobs][cols];
        int pointer = 0;
        for (int i = 0; i < nobs; i++) {
            y[i] = data[pointer++];
            if (!this.noIntercept) {
                x[i][0] = 1.0d;
            }
            for (int j = this.noIntercept ? 0 : 1; j < cols; j++) {
                x[i][j] = data[pointer++];
            }
        }
        this.xMatrix = new Array2DRowRealMatrix(x);
        this.yVector = new ArrayRealVector(y);
    }

    /**
     * Loads new y sample data, overriding any previous data.
     * 
     * @param y
     *        the array representing the y sample
     * @throws NullArgumentException
     *         if y is null
     * @throws NoDataException
     *         if y is empty
     */
    protected void newYSampleData(final double[] y) {
        if (y == null) {
            throw new NullArgumentException();
        }
        if (y.length == 0) {
            throw new NoDataException();
        }
        this.yVector = new ArrayRealVector(y);
    }

    /**
     * <p>
     * Loads new x sample data, overriding any previous data.
     * </p>
     * The input <code>x</code> array should have one row for each sample
     * observation, with columns corresponding to independent variables.
     * For example, if
     * 
     * <pre>
     * <code> x = new double[][] {{1, 2}, {3, 4}, {5, 6}} </code>
     * </pre>
     * 
     * then <code>setXSampleData(x) </code> results in a model with two independent
     * variables and 3 observations:
     * 
     * <pre>
     *   x[0]  x[1]
     *   ----------
     *     1    2
     *     3    4
     *     5    6
     * </pre>
     * 
     * </p>
     * <p>
     * Note that there is no need to add an initial unitary column (column of 1's) when specifying a model including an
     * intercept term.
     * </p>
     * 
     * @param x
     *        the rectangular array representing the x sample
     * @throws NullArgumentException
     *         if x is null
     * @throws NoDataException
     *         if x is empty
     * @throws DimensionMismatchException
     *         if x is not rectangular
     */
    protected void newXSampleData(final double[][] x) {
        if (x == null) {
            throw new NullArgumentException();
        }
        if (x.length == 0) {
            throw new NoDataException();
        }
        if (this.noIntercept) {
            this.xMatrix = new Array2DRowRealMatrix(x, true);
        } else {
            // Augment design matrix with initial unitary column
            final int nVars = x[0].length;
            final double[][] xAug = new double[x.length][nVars + 1];
            for (int i = 0; i < x.length; i++) {
                if (x[i].length != nVars) {
                    throw new DimensionMismatchException(x[i].length, nVars);
                }
                xAug[i][0] = 1.0d;
                System.arraycopy(x[i], 0, xAug[i], 1, nVars);
            }
            this.xMatrix = new Array2DRowRealMatrix(xAug, false);
        }
    }

    /**
     * Validates sample data. Checks that
     * <ul>
     * <li>Neither x nor y is null or empty;</li>
     * <li>The length (i.e. number of rows) of x equals the length of y</li>
     * <li>x has at least one more row than it has columns (i.e. there is sufficient data to estimate regression
     * coefficients for each of the columns in x plus an intercept.</li>
     * </ul>
     * 
     * @param x
     *        the [n,k] array representing the x data
     * @param y
     *        the [n,1] array representing the y data
     * @throws NullArgumentException
     *         if {@code x} or {@code y} is null
     * @throws DimensionMismatchException
     *         if {@code x} and {@code y} do not
     *         have the same length
     * @throws NoDataException
     *         if {@code x} or {@code y} are zero-length
     * @throws MathIllegalArgumentException
     *         if the number of rows of {@code x} is not larger than the number of columns + 1
     */
    protected void validateSampleData(final double[][] x, final double[] y) {
        if ((x == null) || (y == null)) {
            throw new NullArgumentException();
        }
        if (x.length != y.length) {
            throw new DimensionMismatchException(y.length, x.length);
        }
        if (x.length == 0) {
            // Must be no y data either
            throw new NoDataException();
        }
        if (x[0].length + 1 > x.length) {
            throw new MathIllegalArgumentException(
                PatriusMessages.NOT_ENOUGH_DATA_FOR_NUMBER_OF_PREDICTORS,
                x.length, x[0].length);
        }
    }

    /**
     * Validates that the x data and covariance matrix have the same
     * number of rows and that the covariance matrix is square.
     * 
     * @param x
     *        the [n,k] array representing the x sample
     * @param covariance
     *        the [n,n] array representing the covariance matrix
     * @throws DimensionMismatchException
     *         if the number of rows in x is not equal
     *         to the number of rows in covariance
     * @throws NonSquareMatrixException
     *         if the covariance matrix is not square
     */
    protected void validateCovarianceData(final double[][] x, final double[][] covariance) {
        if (x.length != covariance.length) {
            throw new DimensionMismatchException(x.length, covariance.length);
        }
        if (covariance.length > 0 && covariance.length != covariance[0].length) {
            throw new NonSquareMatrixException(covariance.length, covariance[0].length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateRegressionParameters() {
        final RealVector b = this.calculateBeta();
        return b.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateResiduals() {
        final RealVector b = this.calculateBeta();
        final RealVector e = this.yVector.subtract(this.xMatrix.operate(b));
        return e.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[][] estimateRegressionParametersVariance() {
        return this.calculateBetaVariance().getData(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] estimateRegressionParametersStandardErrors() {
        final double[][] betaVariance = this.estimateRegressionParametersVariance();
        final double sigma = this.calculateErrorVariance();
        final int length = betaVariance[0].length;
        final double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            result[i] = MathLib.sqrt(sigma * betaVariance[i][i]);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double estimateRegressandVariance() {
        return this.calculateYVariance();
    }

    /**
     * Estimates the variance of the error.
     * 
     * @return estimate of the error variance
     * @since 2.2
     */
    public double estimateErrorVariance() {
        return this.calculateErrorVariance();

    }

    /**
     * Estimates the standard error of the regression.
     * 
     * @return regression standard error
     * @since 2.2
     */
    public double estimateRegressionStandardError() {
        return Math.sqrt(this.estimateErrorVariance());
    }

    /**
     * Calculates the beta of multiple linear regression in matrix notation.
     * 
     * @return beta
     */
    protected abstract RealVector calculateBeta();

    /**
     * Calculates the beta variance of multiple linear regression in matrix
     * notation.
     * 
     * @return beta variance
     */
    protected abstract RealMatrix calculateBetaVariance();

    /**
     * Calculates the variance of the y values.
     * 
     * @return Y variance
     */
    protected double calculateYVariance() {
        return new Variance().evaluate(this.yVector.toArray());
    }

    /**
     * <p>
     * Calculates the variance of the error term.
     * </p>
     * Uses the formula
     * 
     * <pre>
     * var(u) = u &middot; u / (n - k)
     * </pre>
     * 
     * where n and k are the row and column dimensions of the design
     * matrix X.
     * 
     * @return error variance estimate
     * @since 2.2
     */
    protected double calculateErrorVariance() {
        final RealVector residuals = this.calculateResiduals();
        return residuals.dotProduct(residuals) /
            (this.xMatrix.getRowDimension() - this.xMatrix.getColumnDimension());
    }

    /**
     * Calculates the residuals of multiple linear regression in matrix
     * notation.
     * 
     * <pre>
     * u = y - X * b
     * </pre>
     * 
     * @return The residuals [n,1] matrix
     */
    protected RealVector calculateResiduals() {
        final RealVector b = this.calculateBeta();
        return this.yVector.subtract(this.xMatrix.operate(b));
    }

    // CHECKSTYLE: resume CommentRatio check
}
