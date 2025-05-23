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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.regression;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Results of a Multiple Linear Regression model fit.
 * 
 * @version $Id: RegressionResults.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class RegressionResults implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 5193976046222677405L;
    /** INDEX of Sum of Squared Errors */
    private static final int SSE_IDX = 0;
    /** INDEX of Sum of Squares of Model */
    private static final int SST_IDX = 1;
    /** INDEX of R-Squared of regression */
    private static final int RSQ_IDX = 2;
    /** INDEX of Mean Squared Error */
    private static final int MSE_IDX = 3;
    /** INDEX of Adjusted R Squared */
    private static final int ADJRSQ_IDX = 4;
        /** regression slope parameters */
    private final double[] parameters;
    /** variance covariance matrix of parameters */
    private final double[][] varCovData;
    /** boolean flag for variance covariance matrix in symm compressed storage */
    private final boolean isSymmetricVCD;
    /** number of observations on which results are based */
    private final long nobs;
    /** boolean flag indicator of whether a constant was included */
    private final boolean containsConstant;
    /** array storing global results, SSE, MSE, RSQ, adjRSQ */
    private final double[] globalFitInfo;

    /**
     * Constructor for Regression Results.
     * 
     * @param parametersIn
     *        a double array with the regression slope estimates
     * @param varcov
     *        the variance covariance matrix, stored either in a square matrix
     *        or as a compressed
     * @param isSymmetricCompressed
     *        a flag which denotes that the variance covariance
     *        matrix is in symmetric compressed format
     * @param nobsIn
     *        the number of observations of the regression estimation
     * @param rankIn
     *        the number of independent variables in the regression
     * @param sumy
     *        the sum of the independent variable
     * @param sumysq
     *        the sum of the squared independent variable
     * @param sse
     *        sum of squared errors
     * @param containsConstantIn
     *        true model has constant, false model does not have constant
     * @param copyData
     *        if true a deep copy of all input data is made, if false only references
     *        are copied and the RegressionResults become mutable
     */
    public RegressionResults(
        final double[] parametersIn, final double[][] varcov,
        final boolean isSymmetricCompressed,
        final long nobsIn, final int rankIn,
        final double sumy, final double sumysq, final double sse,
        final boolean containsConstantIn,
        final boolean copyData) {
        if (copyData) {
            this.parameters = MathArrays.copyOf(parametersIn);
            this.varCovData = new double[varcov.length][];
            for (int i = 0; i < varcov.length; i++) {
                this.varCovData[i] = MathArrays.copyOf(varcov[i]);
            }
        } else {
            this.parameters = parametersIn;
            this.varCovData = varcov;
        }
        this.isSymmetricVCD = isSymmetricCompressed;
        this.nobs = nobsIn;
        this.containsConstant = containsConstantIn;
        this.globalFitInfo = new double[5];
        Arrays.fill(this.globalFitInfo, Double.NaN);

        if (rankIn > 0) {
            this.globalFitInfo[SST_IDX] = containsConstantIn ?
                (sumysq - sumy * sumy / nobsIn) : sumysq;
        }

        this.globalFitInfo[SSE_IDX] = sse;
        this.globalFitInfo[MSE_IDX] = this.globalFitInfo[SSE_IDX] /
            (nobsIn - rankIn);
        this.globalFitInfo[RSQ_IDX] = 1.0 -
            this.globalFitInfo[SSE_IDX] /
            this.globalFitInfo[SST_IDX];

        if (containsConstantIn) {
            this.globalFitInfo[ADJRSQ_IDX] = 1.0 - (sse * (nobsIn - 1.0)) /
                (this.globalFitInfo[SST_IDX] * (nobsIn - rankIn));
        } else {
            this.globalFitInfo[ADJRSQ_IDX] = 1.0 -
                (1.0 - this.globalFitInfo[RSQ_IDX]) *
                ((double) nobsIn / ((double) (nobsIn - rankIn)));
        }
    }

    /**
     * <p>
     * Returns the parameter estimate for the regressor at the given index.
     * </p>
     * 
     * <p>
     * A redundant regressor will have its redundancy flag set, as well as a parameters estimated equal to
     * {@code Double.NaN}
     * </p>
     * 
     * @param index
     *        Index.
     * @return the parameters estimated for regressor at index.
     * @throws OutOfRangeException
     *         if {@code index} is not in the interval {@code [0, number of parameters)}.
     */
    public double getParameterEstimate(final int index) {
        if (this.parameters == null) {
            return Double.NaN;
        }
        if (index < 0 || index >= this.parameters.length) {
            throw new OutOfRangeException(index, 0, this.parameters.length - 1);
        }
        return this.parameters[index];
    }

    /**
     * <p>
     * Returns a copy of the regression parameters estimates.
     * </p>
     * 
     * <p>
     * The parameter estimates are returned in the natural order of the data.
     * </p>
     * 
     * <p>
     * A redundant regressor will have its redundancy flag set, as will a parameter estimate equal to {@code Double.NaN}
     * .
     * </p>
     * 
     * @return array of parameter estimates, null if no estimation occurred
     */
    public double[] getParameterEstimates() {
        if (this.parameters == null) {
            return null;
        }
        return MathArrays.copyOf(this.parameters);
    }

    /**
     * Returns the <a href="http://www.xycoon.com/standerrorb(1).htm">standard
     * error of the parameter estimate at index</a>,
     * usually denoted s(b<sub>index</sub>).
     * 
     * @param index
     *        Index.
     * @return the standard errors associated with parameters estimated at index.
     * @throws OutOfRangeException
     *         if {@code index} is not in the interval {@code [0, number of parameters)}.
     */
    public double getStdErrorOfEstimate(final int index) {
        if (this.parameters == null) {
            // Parameters are null, the standard error is not a number
            return Double.NaN;
        }
        if (index < 0 || index >= this.parameters.length) {
            // Exception, index is not in [0, number of parameters]
            throw new OutOfRangeException(index, 0, this.parameters.length - 1);
        }
        final double var = this.getVcvElement(index, index);
        final double res;
        if (!Double.isNaN(var) && var > Double.MIN_VALUE) {
            res = MathLib.sqrt(var);
        } else {
            // The element from the variance-covariance matrix is NaN or too small, error is NaN
            res = Double.NaN;
        }
        return res;
    }

    /**
     * <p>
     * Returns the <a href="http://www.xycoon.com/standerrorb(1).htm">standard error of the parameter estimates</a>,
     * usually denoted s(b<sub>i</sub>).
     * </p>
     * 
     * <p>
     * If there are problems with an ill conditioned design matrix then the regressor which is redundant will be
     * assigned <code>Double.NaN</code>.
     * </p>
     * 
     * @return an array standard errors associated with parameters estimates,
     *         null if no estimation occurred
     */
    public double[] getStdErrorOfEstimates() {
        if (this.parameters == null) {
            // Parameters are null, return a null error array
            return null;
        }
        // Initialize standard error array
        final double[] se = new double[this.parameters.length];
        for (int i = 0; i < this.parameters.length; i++) {
            // Fill the array for each parameter using the variance-covariance matrix
            final double var = this.getVcvElement(i, i);
            if (!Double.isNaN(var) && var > Double.MIN_VALUE) {
                se[i] = MathLib.sqrt(var);
                continue;
            }
            se[i] = Double.NaN;
        }
        return se;
    }

    /**
     * <p>
     * Returns the covariance between regression parameters i and j.
     * </p>
     * 
     * <p>
     * If there are problems with an ill conditioned design matrix then the covariance which involves redundant columns
     * will be assigned {@code Double.NaN}.
     * </p>
     * 
     * @param i
     *        {@code i}th regression parameter.
     * @param j
     *        {@code j}th regression parameter.
     * @return the covariance of the parameter estimates.
     * @throws OutOfRangeException
     *         if {@code i} or {@code j} is not in the
     *         interval {@code [0, number of parameters)}.
     */
    public double getCovarianceOfParameters(final int i, final int j) {
        if (this.parameters == null) {
            return Double.NaN;
        }
        if (i < 0 || i >= this.parameters.length) {
            throw new OutOfRangeException(i, 0, this.parameters.length - 1);
        }
        if (j < 0 || j >= this.parameters.length) {
            throw new OutOfRangeException(j, 0, this.parameters.length - 1);
        }
        return this.getVcvElement(i, j);
    }

    /**
     * <p>
     * Returns the number of parameters estimated in the model.
     * </p>
     * 
     * <p>
     * This is the maximum number of regressors, some techniques may drop redundant parameters
     * </p>
     * 
     * @return number of regressors, -1 if not estimated
     */
    public int getNumberOfParameters() {
        if (this.parameters == null) {
            return -1;
        }
        return this.parameters.length;
    }

    /**
     * Returns the number of observations added to the regression model.
     * 
     * @return Number of observations, -1 if an error condition prevents estimation
     */
    public long getN() {
        return this.nobs;
    }

    /**
     * <p>
     * Returns the sum of squared deviations of the y values about their mean.
     * </p>
     * 
     * <p>
     * This is defined as SSTO <a href="http://www.xycoon.com/SumOfSquares.htm">here</a>.
     * </p>
     * 
     * <p>
     * If {@code n < 2}, this returns {@code Double.NaN}.
     * </p>
     * 
     * @return sum of squared deviations of y values
     */
    public double getTotalSumSquares() {
        return this.globalFitInfo[SST_IDX];
    }

    /**
     * <p>
     * Returns the sum of squared deviations of the predicted y values about their mean (which equals the mean of y).
     * </p>
     * 
     * <p>
     * This is usually abbreviated SSR or SSM. It is defined as SSM <a
     * href="http://www.xycoon.com/SumOfSquares.htm">here</a>
     * </p>
     * 
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least two observations (with at least two different x values) must have been added before invoking this
     * method. If this method is invoked before a model can be estimated, <code>Double.NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @return sum of squared deviations of predicted y values
     */
    public double getRegressionSumSquares() {
        return this.globalFitInfo[SST_IDX] - this.globalFitInfo[SSE_IDX];
    }

    /**
     * <p>
     * Returns the <a href="http://www.xycoon.com/SumOfSquares.htm"> sum of squared errors</a> (SSE) associated with the
     * regression model.
     * </p>
     * 
     * <p>
     * The return value is constrained to be non-negative - i.e., if due to rounding errors the computational formula
     * returns a negative result, 0 is returned.
     * </p>
     * 
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>numberOfParameters data pairs must have been added before invoking this method. If this method is invoked
     * before a model can be estimated, <code>Double,NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @return sum of squared errors associated with the regression model
     */
    public double getErrorSumSquares() {
        return this.globalFitInfo[SSE_IDX];
    }

    /**
     * <p>
     * Returns the sum of squared errors divided by the degrees of freedom, usually abbreviated MSE.
     * </p>
     * 
     * <p>
     * If there are fewer than <strong>numberOfParameters + 1</strong> data pairs in the model, or if there is no
     * variation in <code>x</code>, this returns <code>Double.NaN</code>.
     * </p>
     * 
     * @return sum of squared deviations of y values
     */
    public double getMeanSquareError() {
        return this.globalFitInfo[MSE_IDX];
    }

    /**
     * <p>
     * Returns the <a href="http://www.xycoon.com/coefficient1.htm"> coefficient of multiple determination</a>, usually
     * denoted r-square.
     * </p>
     * 
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least numberOfParameters observations (with at least numberOfParameters different x values) must have been
     * added before invoking this method. If this method is invoked before a model can be estimated, {@code Double,NaN}
     * is returned.</li>
     * </ul>
     * </p>
     * 
     * @return r-square, a double in the interval [0, 1]
     */
    public double getRSquared() {
        return this.globalFitInfo[RSQ_IDX];
    }

    /**
     * <p>
     * Returns the adjusted R-squared statistic, defined by the formula
     * 
     * <pre>
     * R<sup>2</sup><sub>adj</sub> = 1 - [SSR (n - 1)] / [SSTO (n - p)]
     * </pre>
     * 
     * where SSR is the sum of squared residuals}, SSTO is the total sum of squares}, n is the number of observations
     * and p is the number of parameters estimated (including the intercept).
     * </p>
     * 
     * <p>
     * If the regression is estimated without an intercept term, what is returned is
     * 
     * <pre>
     * <code> 1 - (1 - {@link #getRSquared()} ) * (n / (n - p)) </code>
     * </pre>
     * 
     * </p>
     * 
     * @return adjusted R-Squared statistic
     */
    public double getAdjustedRSquared() {
        return this.globalFitInfo[ADJRSQ_IDX];
    }

    /**
     * Returns true if the regression model has been computed including an intercept.
     * In this case, the coefficient of the intercept is the first element of the {@link #getParameterEstimates()
     * parameter estimates}.
     * 
     * @return true if the model has an intercept term
     */
    public boolean hasIntercept() {
        return this.containsConstant;
    }

    /**
     * Gets the i-jth element of the variance-covariance matrix.
     * 
     * @param i
     *        first variable index
     * @param j
     *        second variable index
     * @return the requested variance-covariance matrix entry
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private double getVcvElement(final int i, final int j) {
        // CHECKSTYLE: resume ReturnCount check
        if (this.isSymmetricVCD) {
            if (this.varCovData.length > 1) {
                // could be stored in upper or lower triangular
                if (i == j) {
                    return this.varCovData[i][i];
                } else if (i >= this.varCovData[j].length) {
                    return this.varCovData[i][j];
                } else {
                    return this.varCovData[j][i];
                }
            } else {
                // could be in single array
                if (i > j) {
                    return this.varCovData[0][(i + 1) * i / 2 + j];
                } else {
                    return this.varCovData[0][(j + 1) * j / 2 + i];
                }
            }
        } else {
            return this.varCovData[i][j];
        }
    }
}
