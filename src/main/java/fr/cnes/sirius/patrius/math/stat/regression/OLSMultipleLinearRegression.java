/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
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
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.math.stat.regression;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.LUDecomposition;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.stat.StatUtils;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.SecondMoment;

/**
 * <p>
 * Implements ordinary least squares (OLS) to estimate the parameters of a multiple linear regression model.
 * </p>
 * 
 * <p>
 * The regression coefficients, <code>b</code>, satisfy the normal equations:
 * 
 * <pre>
 * <code> X<sup>T</sup> X b = X<sup>T</sup> y </code>
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * To solve the normal equations, this implementation uses QR decomposition of the <code>X</code> matrix. (See
 * {@link QRDecomposition} for details on the decomposition algorithm.) The <code>X</code> matrix, also known as the
 * <i>design matrix,</i> has rows corresponding to sample observations and columns corresponding to independent
 * variables. When the model is estimated using an intercept term (i.e. when {@link #isNoIntercept() isNoIntercept} is
 * false as it is by default), the <code>X</code> matrix includes an initial column identically equal to 1. We solve the
 * normal equations as follows:
 * 
 * <pre>
 * <code> X<sup>T</sup>X b = X<sup>T</sup> y
 * (QR)<sup>T</sup> (QR) b = (QR)<sup>T</sup>y
 * R<sup>T</sup> (Q<sup>T</sup>Q) R b = R<sup>T</sup> Q<sup>T</sup> y
 * R<sup>T</sup> R b = R<sup>T</sup> Q<sup>T</sup> y
 * (R<sup>T</sup>)<sup>-1</sup> R<sup>T</sup> R b = (R<sup>T</sup>)<sup>-1</sup> R<sup>T</sup> Q<sup>T</sup> y
 * R b = Q<sup>T</sup> y </code>
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * Given <code>Q</code> and <code>R</code>, the last equation is solved by back-substitution.
 * </p>
 * 
 * @version $Id: OLSMultipleLinearRegression.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class OLSMultipleLinearRegression extends AbstractMultipleLinearRegression {

    /** Cached QR decomposition of X matrix */
    private QRDecomposition qr = null;

    /**
     * Loads model x and y sample data, overriding any previous sample.
     * 
     * Computes and caches QR decomposition of the X matrix.
     * 
     * @param y
     *        the [n,1] array representing the y sample
     * @param x
     *        the [n,k] array representing the x sample
     * @throws MathIllegalArgumentException
     *         if the x and y array data are not
     *         compatible for the regression
     */
    public void newSampleData(final double[] y, final double[][] x) {
        this.validateSampleData(x, y);
        this.newYSampleData(y);
        this.newXSampleData(x);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation computes and caches the QR decomposition of the X matrix.
     * </p>
     */
    @Override
    public void newSampleData(final double[] data, final int nobs, final int nvars) {
        super.newSampleData(data, nobs, nvars);
        this.qr = new QRDecomposition(this.getX());
    }

    /**
     * <p>
     * Compute the "hat" matrix.
     * </p>
     * <p>
     * The hat matrix is defined in terms of the design matrix X by X(X<sup>T</sup>X)<sup>-1</sup>X<sup>T</sup>
     * </p>
     * <p>
     * The implementation here uses the QR decomposition to compute the hat matrix as Q I<sub>p</sub>Q<sup>T</sup> where
     * I<sub>p</sub> is the p-dimensional identity matrix augmented by 0's. This computational formula is from
     * "The Hat Matrix in Regression and ANOVA", David C. Hoaglin and Roy E. Welsch, <i>The American Statistician</i>,
     * Vol. 32, No. 1 (Feb., 1978), pp. 17-22.
     * </p>
     * <p>
     * Data for the model must have been successfully loaded using one of the {@code newSampleData} methods before
     * invoking this method; otherwise a {@code NullPointerException} will be thrown.
     * </p>
     * 
     * @return the hat matrix
     */
    public RealMatrix calculateHat() {
        // Create augmented identity matrix
        final RealMatrix q = this.qr.getQ();
        final int p = this.qr.getR().getColumnDimension();
        final int n = q.getColumnDimension();
        // No try-catch or advertised NotStrictlyPositiveException - NPE above if n < 3
        final Array2DRowRealMatrix augI = new Array2DRowRealMatrix(n, n);
        final double[][] augIData = augI.getDataRef();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j && i < p) {
                    augIData[i][j] = 1d;
                } else {
                    augIData[i][j] = 0d;
                }
            }
        }

        // Compute and return Hat matrix
        // No DME advertised - args valid if we get here
        return q.multiply(augI).multiply(q.transpose());
    }

    /**
     * <p>
     * Returns the sum of squared deviations of Y from its mean.
     * </p>
     * 
     * <p>
     * If the model has no intercept term, <code>0</code> is used for the mean of Y - i.e., what is returned is the sum
     * of the squared Y values.
     * </p>
     * 
     * <p>
     * The value returned by this method is the SSTO value used in the {@link #calculateRSquared() R-squared}
     * computation.
     * </p>
     * 
     * @return SSTO - the total sum of squares
     * @throws MathIllegalArgumentException
     *         if the sample has not been set or does
     *         not contain at least 3 observations
     * @see #isNoIntercept()
     * @since 2.2
     */
    public double calculateTotalSumOfSquares() {
        if (this.isNoIntercept()) {
            return StatUtils.sumSq(this.getY().toArray());
        } else {
            return new SecondMoment().evaluate(this.getY().toArray());
        }
    }

    /**
     * Returns the sum of squared residuals.
     * 
     * @return residual sum of squares
     * @since 2.2
     */
    public double calculateResidualSumOfSquares() {
        final RealVector residuals = this.calculateResiduals();
        // No advertised DME, args are valid
        return residuals.dotProduct(residuals);
    }

    /**
     * Returns the R-Squared statistic, defined by the formula
     * 
     * <pre>
     * R<sup>2</sup> = 1 - SSR / SSTO
     * </pre>
     * 
     * where SSR is the {@link #calculateResidualSumOfSquares() sum of squared residuals} and SSTO is the
     * {@link #calculateTotalSumOfSquares() total sum of squares}
     * 
     * @return R-square statistic
     * @throws MathIllegalArgumentException
     *         if the sample has not been set or does
     *         not contain at least 3 observations
     * @since 2.2
     */
    public double calculateRSquared() {
        return 1 - this.calculateResidualSumOfSquares() / this.calculateTotalSumOfSquares();
    }

    /**
     * <p>
     * Returns the adjusted R-squared statistic, defined by the formula
     * 
     * <pre>
     * R<sup>2</sup><sub>adj</sub> = 1 - [SSR (n - 1)] / [SSTO (n - p)]
     * </pre>
     * 
     * where SSR is the {@link #calculateResidualSumOfSquares() sum of squared residuals}, SSTO is the
     * {@link #calculateTotalSumOfSquares() total sum of squares}, n is the number of observations and p is the number
     * of parameters estimated (including the intercept).
     * </p>
     * 
     * <p>
     * If the regression is estimated without an intercept term, what is returned is
     * 
     * <pre>
     * <code> 1 - (1 - {@link #calculateRSquared()}) * (n / (n - p)) </code>
     * </pre>
     * 
     * </p>
     * 
     * @return adjusted R-Squared statistic
     * @throws MathIllegalArgumentException
     *         if the sample has not been set or does
     *         not contain at least 3 observations
     * @see #isNoIntercept()
     * @since 2.2
     */
    public double calculateAdjustedRSquared() {
        final double n = this.getX().getRowDimension();
        if (this.isNoIntercept()) {
            return 1 - (1 - this.calculateRSquared()) * (n / (n - this.getX().getColumnDimension()));
        } else {
            return 1 - (this.calculateResidualSumOfSquares() * (n - 1)) /
                (this.calculateTotalSumOfSquares() * (n - this.getX().getColumnDimension()));
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation computes and caches the QR decomposition of the X matrix once it is successfully loaded.
     * </p>
     */
    @Override
    protected void newXSampleData(final double[][] x) {
        super.newXSampleData(x);
        this.qr = new QRDecomposition(this.getX());
    }

    /**
     * Calculates the regression coefficients using OLS.
     * 
     * <p>
     * Data for the model must have been successfully loaded using one of the {@code newSampleData} methods before
     * invoking this method; otherwise a {@code NullPointerException} will be thrown.
     * </p>
     * 
     * @return beta
     */
    @Override
    protected RealVector calculateBeta() {
        return this.qr.getSolver().solve(this.getY());
    }

    /**
     * <p>
     * Calculates the variance-covariance matrix of the regression parameters.
     * </p>
     * <p>
     * Var(b) = (X<sup>T</sup>X)<sup>-1</sup>
     * </p>
     * <p>
     * Uses QR decomposition to reduce (X<sup>T</sup>X)<sup>-1</sup> to (R<sup>T</sup>R)<sup>-1</sup>, with only the top
     * p rows of R included, where p = the length of the beta vector.
     * </p>
     * 
     * <p>
     * Data for the model must have been successfully loaded using one of the {@code newSampleData} methods before
     * invoking this method; otherwise a {@code NullPointerException} will be thrown.
     * </p>
     * 
     * @return The beta variance-covariance matrix
     */
    @Override
    protected RealMatrix calculateBetaVariance() {
        final int p = this.getX().getColumnDimension();
        final RealMatrix raug = this.qr.getR().getSubMatrix(0, p - 1, 0, p - 1);
        final RealMatrix rinv = new LUDecomposition(raug).getSolver().getInverse();
        return rinv.multiply(rinv.transpose());
    }

}
