/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.4:FA:FA-2137:04/10/2019:FA mineure Patrius V4.3
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.stat.regression;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.distribution.TDistribution;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NoDataException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Estimates an ordinary least squares regression model
 * with one independent variable.
 * <p>
 * <code> y = intercept + slope * x  </code>
 * </p>
 * <p>
 * Standard errors for <code>intercept</code> and <code>slope</code> are available as well as ANOVA, r-square and
 * Pearson's r statistics.
 * </p>
 * <p>
 * Observations (x,y pairs) can be added to the model one at a time or they can be provided in a 2-dimensional array.
 * The observations are not stored in memory, so there is no limit to the number of observations that can be added to
 * the model.
 * </p>
 * <p>
 * <strong>Usage Notes</strong>:
 * <ul>
 * <li>When there are fewer than two observations in the model, or when there is no variation in the x values (i.e. all
 * x values are the same) all statistics return <code>NaN</code>. At least two observations with different x coordinates
 * are required to estimate a bivariate regression model.</li>
 * <li>Getters for the statistics always compute values based on the current set of observations -- i.e., you can get
 * statistics, then add more data and get updated statistics without using a new instance. There is no "compute" method
 * that updates all statistics. Each of the getters performs the necessary computations to return the requested
 * statistic.</li>
 * <li>The intercept term may be suppressed by passing {@code false} to the {@link #SimpleRegression(boolean)}
 * constructor. When the {@code hasIntercept} property is false, the model is estimated without a constant term and
 * {@link #getIntercept()} returns {@code 0}.</li>
 * </ul>
 * </p>
 * 
 * @version $Id: SimpleRegression.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SimpleRegression implements Serializable, UpdatingMultipleLinearRegression {

    /** Default slope confidence interval. */
    private static final double DEFAULT_SLOPE_CONFIDENCE_INTERVAL = 0.05d;

     /** Serializable UID. */
    private static final long serialVersionUID = -3004689053607543335L;
    
    /** 10 */
    private static final int TEN = 10;

    /** sum of x values */
    private double sumX = 0d;

    /** total variation in x (sum of squared deviations from xbar) */
    private double sumXX = 0d;

    /** sum of y values */
    private double sumY = 0d;

    /** total variation in y (sum of squared deviations from ybar) */
    private double sumYY = 0d;

    /** sum of products */
    private double sumXY = 0d;

    /** number of observations */
    private long n = 0;

    /** mean of accumulated x values, used in updating formulas */
    private double xbar = 0;

    /** mean of accumulated y values, used in updating formulas */
    private double ybar = 0;

    /** include an intercept or not */
    private final boolean hasInterceptFlag;

    // ---------------------Public methods--------------------------------------

    /**
     * Create an empty SimpleRegression instance
     */
    public SimpleRegression() {
        this(true);
    }

    /**
     * Create a SimpleRegression instance, specifying whether or not to estimate
     * an intercept.
     * 
     * <p>
     * Use {@code false} to estimate a model with no intercept. When the {@code hasIntercept} property is false, the
     * model is estimated without a constant term and {@link #getIntercept()} returns {@code 0}.
     * </p>
     * 
     * @param includeIntercept
     *        whether or not to include an intercept term in
     *        the regression model
     */
    public SimpleRegression(final boolean includeIntercept) {
        super();
        this.hasInterceptFlag = includeIntercept;
    }

    /**
     * Adds the observation (x,y) to the regression data set.
     * <p>
     * Uses updating formulas for means and sums of squares defined in "Algorithms for Computing the Sample Variance:
     * Analysis and Recommendations", Chan, T.F., Golub, G.H., and LeVeque, R.J. 1983, American Statistician, vol. 37,
     * pp. 242-247, referenced in Weisberg, S. "Applied Linear Regression". 2nd Ed. 1985.
     * </p>
     * 
     * 
     * @param x
     *        independent variable value
     * @param y
     *        dependent variable value
     */
    public void addData(final double x, final double y) {
        if (this.n == 0) {
            this.xbar = x;
            this.ybar = y;
        } else {
            if (this.hasInterceptFlag) {
                final double fact1 = 1.0 + this.n;
                final double fact2 = this.n / (1.0 + this.n);
                final double dx = x - this.xbar;
                final double dy = y - this.ybar;
                this.sumXX += dx * dx * fact2;
                this.sumYY += dy * dy * fact2;
                this.sumXY += dx * dy * fact2;
                this.xbar += dx / fact1;
                this.ybar += dy / fact1;
            }
        }
        if (!this.hasInterceptFlag) {
            this.sumXX += x * x;
            this.sumYY += y * y;
            this.sumXY += x * y;
        }
        this.sumX += x;
        this.sumY += y;
        this.n++;
    }

    /**
     * Removes the observation (x,y) from the regression data set.
     * <p>
     * Mirrors the addData method. This method permits the use of SimpleRegression instances in streaming mode where the
     * regression is applied to a sliding "window" of observations, however the caller is responsible for maintaining
     * the set of observations in the window.
     * </p>
     * 
     * The method has no effect if there are no points of data (i.e. n=0)
     * 
     * @param x
     *        independent variable value
     * @param y
     *        dependent variable value
     */
    public void removeData(final double x, final double y) {
        if (this.n > 0) {
            if (this.hasInterceptFlag) {
                final double fact1 = this.n - 1.0;
                final double fact2 = this.n / (this.n - 1.0);
                final double dx = x - this.xbar;
                final double dy = y - this.ybar;
                this.sumXX -= dx * dx * fact2;
                this.sumYY -= dy * dy * fact2;
                this.sumXY -= dx * dy * fact2;
                this.xbar -= dx / fact1;
                this.ybar -= dy / fact1;
            } else {
                final double fact1 = this.n - 1.0;
                this.sumXX -= x * x;
                this.sumYY -= y * y;
                this.sumXY -= x * y;
                this.xbar -= x / fact1;
                this.ybar -= y / fact1;
            }
            this.sumX -= x;
            this.sumY -= y;
            this.n--;
        }
    }

    /**
     * Adds the observations represented by the elements in <code>data</code>.
     * <p>
     * <code>(data[0][0],data[0][1])</code> will be the first observation, then <code>(data[1][0],data[1][1])</code>,
     * etc.
     * </p>
     * <p>
     * This method does not replace data that has already been added. The observations represented by <code>data</code>
     * are added to the existing dataset.
     * </p>
     * <p>
     * To replace all data, use <code>clear()</code> before adding the new data.
     * </p>
     * 
     * @param data
     *        array of observations to be added
     * @throws ModelSpecificationException
     *         if the length of {@code data[i]} is not
     *         greater than or equal to 2
     */
    public void addData(final double[][] data) {
        for (final double[] element : data) {
            if (element.length < 2) {
                throw new ModelSpecificationException(PatriusMessages.INVALID_REGRESSION_OBSERVATION,
                    element.length, 2);
            }
            this.addData(element[0], element[1]);
        }
    }

    /**
     * Adds one observation to the regression model.
     * 
     * @param x
     *        the independent variables which form the design matrix
     * @param y
     *        the dependent or response variable
     * @throws ModelSpecificationException
     *         if the length of {@code x} does not equal
     *         the number of independent variables in the model
     */
    @Override
    public void addObservation(final double[] x, final double y) {
        if (x == null || x.length == 0) {
            throw new ModelSpecificationException(PatriusMessages.INVALID_REGRESSION_OBSERVATION, x == null ?
                0 : x.length, 1);
        }
        this.addData(x[0], y);
    }

    /**
     * Adds a series of observations to the regression model. The lengths of
     * x and y must be the same and x must be rectangular.
     * 
     * @param x
     *        a series of observations on the independent variables
     * @param y
     *        a series of observations on the dependent variable
     *        The length of x and y must be the same
     * @throws ModelSpecificationException
     *         if {@code x} is not rectangular, does not match
     *         the length of {@code y} or does not contain sufficient data to estimate the model
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public void addObservations(final double[][] x, final double[] y) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if ((x == null) || (y == null) || (x.length != y.length)) {
            throw new ModelSpecificationException(
                PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE,
                (x == null) ? 0 : x.length,
                (y == null) ? 0 : y.length);
        }
        boolean obsOk = true;
        for (final double[] element : x) {
            if (element == null || element.length == 0) {
                obsOk = false;
            }
        }
        if (!obsOk) {
            throw new ModelSpecificationException(
                PatriusMessages.NOT_ENOUGH_DATA_FOR_NUMBER_OF_PREDICTORS,
                0, 1);
        }
        for (int i = 0; i < x.length; i++) {
            this.addData(x[i][0], y[i]);
        }
    }

    /**
     * Removes observations represented by the elements in <code>data</code>.
     * <p>
     * If the array is larger than the current n, only the first n elements are processed. This method permits the use
     * of SimpleRegression instances in streaming mode where the regression is applied to a sliding "window" of
     * observations, however the caller is responsible for maintaining the set of observations in the window.
     * </p>
     * <p>
     * To remove all data, use <code>clear()</code>.
     * </p>
     * 
     * @param data
     *        array of observations to be removed
     */
    public void removeData(final double[][] data) {
        for (int i = 0; i < data.length && this.n > 0; i++) {
            this.removeData(data[i][0], data[i][1]);
        }
    }

    /**
     * Clears all data from the model.
     */
    @Override
    public void clear() {
        this.sumX = 0d;
        this.sumXX = 0d;
        this.sumY = 0d;
        this.sumYY = 0d;
        this.sumXY = 0d;
        this.n = 0;
    }

    /**
     * Returns the number of observations that have been added to the model.
     * 
     * @return n number of observations that have been added.
     */
    @Override
    public long getN() {
        return this.n;
    }

    /**
     * Returns the "predicted" <code>y</code> value associated with the
     * supplied <code>x</code> value, based on the data that has been
     * added to the model when this method is activated.
     * <p>
     * <code> predict(x) = intercept + slope * x </code>
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least two observations (with at least two different x values) must have been added before invoking this
     * method. If this method is invoked before a model can be estimated, <code>Double,NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @param x
     *        input <code>x</code> value
     * @return predicted <code>y</code> value
     */
    public double predict(final double x) {
        final double b1 = this.getSlope();
        if (this.hasInterceptFlag) {
            return this.getIntercept(b1) + b1 * x;
        }
        return b1 * x;
    }

    /**
     * Returns the intercept of the estimated regression line, if {@link #hasIntercept()} is true; otherwise 0.
     * <p>
     * The least squares estimate of the intercept is computed using the <a
     * href="http://www.xycoon.com/estimation4.htm">normal equations</a>. The intercept is sometimes denoted b0.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least two observations (with at least two different x values) must have been added before invoking this
     * method. If this method is invoked before a model can be estimated, <code>Double,NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @return the intercept of the regression line if the model includes an
     *         intercept; 0 otherwise
     * @see #SimpleRegression(boolean)
     */
    public double getIntercept() {
        return this.hasInterceptFlag ? this.getIntercept(this.getSlope()) : 0.0;
    }

    /**
     * Returns true if the model includes an intercept term.
     * 
     * @return true if the regression includes an intercept; false otherwise
     * @see #SimpleRegression(boolean)
     */
    @Override
    public boolean hasIntercept() {
        return this.hasInterceptFlag;
    }

    /**
     * Returns the slope of the estimated regression line.
     * <p>
     * The least squares estimate of the slope is computed using the <a
     * href="http://www.xycoon.com/estimation4.htm">normal equations</a>. The slope is sometimes denoted b1.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least two observations (with at least two different x values) must have been added before invoking this
     * method. If this method is invoked before a model can be estimated, <code>Double.NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @return the slope of the regression line
     */
    public double getSlope() {
        final double res;
        if (this.n < 2) {
            // not enough data
            res = Double.NaN;
        } else if (MathLib.abs(this.sumXX) < TEN * Double.MIN_VALUE) {
            // not enough variation in x
            res = Double.NaN;
        } else {
            res = this.sumXY / this.sumXX;
        }
        return res;
    }

    /**
     * Returns the <a href="http://www.xycoon.com/SumOfSquares.htm">
     * sum of squared errors</a> (SSE) associated with the regression
     * model.
     * <p>
     * The sum is computed using the computational formula
     * </p>
     * <p>
     * <code>SSE = SYY - (SXY * SXY / SXX)</code>
     * </p>
     * <p>
     * where <code>SYY</code> is the sum of the squared deviations of the y values about their mean, <code>SXX</code> is
     * similarly defined and <code>SXY</code> is the sum of the products of x and y mean deviations.
     * </p>
     * <p>
     * The sums are accumulated using the updating algorithm referenced in {@link #addData}.
     * </p>
     * <p>
     * The return value is constrained to be non-negative - i.e., if due to rounding errors the computational formula
     * returns a negative result, 0 is returned.
     * </p>
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least two observations (with at least two different x values) must have been added before invoking this
     * method. If this method is invoked before a model can be estimated, <code>Double,NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @return sum of squared errors associated with the regression model
     */
    public double getSumSquaredErrors() {
        if(this.sumXX != 0. && !Double.isNaN(this.sumXX) && !Double.isNaN(this.sumXY) && !Double.isNaN(this.sumYY)) {
            return MathLib.max(0d, this.sumYY - this.sumXY * this.sumXY / this.sumXX);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Returns the sum of squared deviations of the y values about their mean.
     * <p>
     * This is defined as SSTO <a href="http://www.xycoon.com/SumOfSquares.htm">here</a>.
     * </p>
     * <p>
     * If <code>n < 2</code>, this returns <code>Double.NaN</code>.
     * </p>
     * 
     * @return sum of squared deviations of y values
     */
    public double getTotalSumSquares() {
        if (this.n < 2) {
            return Double.NaN;
        }
        return this.sumYY;
    }

    /**
     * Returns the sum of squared deviations of the x values about their mean.
     * 
     * If <code>n < 2</code>, this returns <code>Double.NaN</code>.</p>
     * 
     * @return sum of squared deviations of x values
     */
    public double getXSumSquares() {
        if (this.n < 2) {
            return Double.NaN;
        }
        return this.sumXX;
    }

    /**
     * Returns the sum of crossproducts, x<sub>i</sub>*y<sub>i</sub>.
     * 
     * @return sum of cross products
     */
    public double getSumOfCrossProducts() {
        return this.sumXY;
    }

    /**
     * Returns the sum of squared deviations of the predicted y values about
     * their mean (which equals the mean of y).
     * <p>
     * This is usually abbreviated SSR or SSM. It is defined as SSM <a
     * href="http://www.xycoon.com/SumOfSquares.htm">here</a>
     * </p>
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
        return this.getRegressionSumSquares(this.getSlope());
    }

    /**
     * Returns the sum of squared errors divided by the degrees of freedom,
     * usually abbreviated MSE.
     * <p>
     * If there are fewer than <strong>three</strong> data pairs in the model, or if there is no variation in
     * <code>x</code>, this returns <code>Double.NaN</code>.
     * </p>
     * 
     * @return sum of squared deviations of y values
     */
    public double getMeanSquareError() {
        if (this.n < 3) {
            return Double.NaN;
        }
        return this.hasInterceptFlag ? (this.getSumSquaredErrors() / (this.n - 2))
            : (this.getSumSquaredErrors() / (this.n - 1));
    }

    /**
     * Returns <a href="http://mathworld.wolfram.com/CorrelationCoefficient.html">
     * Pearson's product moment correlation coefficient</a>,
     * usually denoted r.
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least two observations (with at least two different x values) must have been added before invoking this
     * method. If this method is invoked before a model can be estimated, <code>Double,NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @return Pearson's r
     */
    public double getR() {
        double res;
        final double b1 = this.getSlope();
        final double rSquare = this.getRSquare();
        if (rSquare >= 0. && !Double.isNaN(rSquare)) {
            res = MathLib.sqrt(rSquare);
            if (b1 < 0) {
                res = -res;
            }
        } else {
            res = Double.NaN;
        }
        return res;
    }

    /**
     * Returns the <a href="http://www.xycoon.com/coefficient1.htm">
     * coefficient of determination</a>,
     * usually denoted r-square.
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>At least two observations (with at least two different x values) must have been added before invoking this
     * method. If this method is invoked before a model can be estimated, <code>Double,NaN</code> is returned.</li>
     * </ul>
     * </p>
     * 
     * @return r-square
     */
    public double getRSquare() {
        final double ssto = this.getTotalSumSquares();
        return (ssto - this.getSumSquaredErrors()) / ssto;
    }

    /**
     * Returns the <a href="http://www.xycoon.com/standarderrorb0.htm">
     * standard error of the intercept estimate</a>,
     * usually denoted s(b0).
     * <p>
     * If there are fewer that <strong>three</strong> observations in the model, or if there is no variation in x, this
     * returns <code>Double.NaN</code>.
     * </p>
     * Additionally, a <code>Double.NaN</code> is
     * returned when the intercept is constrained to be zero
     * 
     * @return standard error associated with intercept estimate
     */
    public double getInterceptStdErr() {
        final double res;
        if (this.hasInterceptFlag) {
            final double resSquare = this.getMeanSquareError() * ((1d / this.n) + (this.xbar * this.xbar) / this.sumXX);
            if (resSquare >= 0. && !Double.isNaN(resSquare)) {
                res = MathLib.sqrt(resSquare);
            } else {
                res = Double.NaN;
            }
        } else {
            res = Double.NaN;
        }
        return res;
    }

    /**
     * Returns the <a href="http://www.xycoon.com/standerrorb(1).htm">standard
     * error of the slope estimate</a>,
     * usually denoted s(b1).
     * <p>
     * If there are fewer that <strong>three</strong> data pairs in the model, or if there is no variation in x, this
     * returns <code>Double.NaN</code>.
     * </p>
     * 
     * @return standard error associated with slope estimate
     */
    public double getSlopeStdErr() {
        final double res;
        final double resSquare = this.getMeanSquareError() / this.sumXX;
        if (resSquare >= 0. && !Double.isNaN(resSquare)) {
            res = MathLib.sqrt(resSquare);
        } else {
            res = Double.NaN;
        }
        return res;
    }

    /**
     * Returns the half-width of a 95% confidence interval for the slope
     * estimate.
     * <p>
     * The 95% confidence interval is
     * </p>
     * <p>
     * <code>(getSlope() - getSlopeConfidenceInterval(),
     * getSlope() + getSlopeConfidenceInterval())</code>
     * </p>
     * <p>
     * If there are fewer that <strong>three</strong> observations in the model, or if there is no variation in x, this
     * returns <code>Double.NaN</code>.
     * </p>
     * <p>
     * <strong>Usage Note</strong>:<br>
     * The validity of this statistic depends on the assumption that the observations included in the model are drawn
     * from a <a href="http://mathworld.wolfram.com/BivariateNormalDistribution.html"> Bivariate Normal
     * Distribution</a>.
     * </p>
     * 
     * @return half-width of 95% confidence interval for the slope estimate
     * @throws OutOfRangeException
     *         if the confidence interval can not be computed.
     */
    public double getSlopeConfidenceInterval() {
        return this.getSlopeConfidenceInterval(DEFAULT_SLOPE_CONFIDENCE_INTERVAL);
    }

    /**
     * Returns the half-width of a (100-100*alpha)% confidence interval for
     * the slope estimate.
     * <p>
     * The (100-100*alpha)% confidence interval is
     * </p>
     * <p>
     * <code>(getSlope() - getSlopeConfidenceInterval(),
     * getSlope() + getSlopeConfidenceInterval())</code>
     * </p>
     * <p>
     * To request, for example, a 99% confidence interval, use <code>alpha = .01</code>
     * </p>
     * <p>
     * <strong>Usage Note</strong>:<br>
     * The validity of this statistic depends on the assumption that the observations included in the model are drawn
     * from a <a href="http://mathworld.wolfram.com/BivariateNormalDistribution.html"> Bivariate Normal
     * Distribution</a>.
     * </p>
     * <p>
     * <strong> Preconditions:</strong>
     * <ul>
     * <li>If there are fewer that <strong>three</strong> observations in the model, or if there is no variation in x,
     * this returns <code>Double.NaN</code>.</li>
     * <li><code>(0 < alpha < 1)</code>; otherwise an <code>OutOfRangeException</code> is thrown.</li>
     * </ul>
     * </p>
     * 
     * @param alpha
     *        the desired significance level
     * @return half-width of 95% confidence interval for the slope estimate
     * @throws OutOfRangeException
     *         if the confidence interval can not be computed.
     */
    public double getSlopeConfidenceInterval(final double alpha) {
        if (this.n < 3) {
            return Double.NaN;
        }
        if (alpha >= 1 || alpha <= 0) {
            throw new OutOfRangeException(PatriusMessages.SIGNIFICANCE_LEVEL,
                alpha, 0, 1);
        }
        // No advertised NotStrictlyPositiveException here - will return NaN above
        final TDistribution distribution = new TDistribution(this.n - 2);
        return this.getSlopeStdErr() *
            distribution.inverseCumulativeProbability(1d - alpha / 2d);
    }

    /**
     * Returns the significance level of the slope (equiv) correlation.
     * <p>
     * Specifically, the returned value is the smallest <code>alpha</code> such that the slope confidence interval with
     * significance level equal to <code>alpha</code> does not include <code>0</code>. On regression output, this is
     * often denoted <code>Prob(|t| > 0)</code>
     * </p>
     * <p>
     * <strong>Usage Note</strong>:<br>
     * The validity of this statistic depends on the assumption that the observations included in the model are drawn
     * from a <a href="http://mathworld.wolfram.com/BivariateNormalDistribution.html"> Bivariate Normal
     * Distribution</a>.
     * </p>
     * <p>
     * If there are fewer that <strong>three</strong> observations in the model, or if there is no variation in x, this
     * returns <code>Double.NaN</code>.
     * </p>
     * 
     * @return significance level for slope/correlation
     * @throws fr.cnes.sirius.patrius.math.exception.MaxCountExceededException
     *         if the significance level can not be computed.
     */
    public double getSignificance() {
        if (this.n < 3) {
            return Double.NaN;
        }
        // No advertised NotStrictlyPositiveException here - will return NaN above
        final TDistribution distribution = new TDistribution(this.n - 2);
        return 2d * (1.0 - distribution.cumulativeProbability(
            MathLib.abs(this.getSlope()) / this.getSlopeStdErr()));
    }

    // ---------------------Private methods-----------------------------------

    /**
     * Returns the intercept of the estimated regression line, given the slope.
     * <p>
     * Will return <code>NaN</code> if slope is <code>NaN</code>.
     * </p>
     * 
     * @param slope
     *        current slope
     * @return the intercept of the regression line
     */
    private double getIntercept(final double slope) {
        if (this.hasInterceptFlag) {
            return (this.sumY - slope * this.sumX) / this.n;
        }
        return 0.0;
    }

    /**
     * Computes SSR from b1.
     * 
     * @param slope
     *        regression slope estimate
     * @return sum of squared deviations of predicted y values
     */
    private double getRegressionSumSquares(final double slope) {
        return slope * slope * this.sumXX;
    }

    /**
     * Performs a regression on data present in buffers and outputs a RegressionResults object.
     * 
     * <p>
     * If there are fewer than 3 observations in the model and {@code hasIntercept} is true a {@code NoDataException} is
     * thrown. If there is no intercept term, the model must contain at least 2 observations.
     * </p>
     * 
     * @return RegressionResults acts as a container of regression output
     * @throws ModelSpecificationException
     *         if the model is not correctly specified
     * @throws NoDataException
     *         if there is not sufficient data in the model to
     *         estimate the regression parameters
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public RegressionResults regress() {
        // CHECKSTYLE: resume ReturnCount check
        if (this.hasInterceptFlag) {
            if (this.n < 3) {
                throw new NoDataException(PatriusMessages.NOT_ENOUGH_DATA_REGRESSION);
            }
            if (MathLib.abs(this.sumXX) > Precision.SAFE_MIN) {
                final double[] params = new double[] { this.getIntercept(), this.getSlope() };
                final double mse = this.getMeanSquareError();
                final double syy = this.sumYY + this.sumY * this.sumY / this.n;
                final double[] vcv = new double[] {
                    mse * (this.xbar * this.xbar / this.sumXX + 1.0 / this.n),
                    -this.xbar * mse / this.sumXX,
                    mse / this.sumXX };
                return new RegressionResults(
                    params, new double[][] { vcv }, true, this.n, 2,
                    this.sumY, syy, this.getSumSquaredErrors(), true, false);
            } else {
                final double[] params = new double[] { this.sumY / this.n, Double.NaN };
                // final double mse = getMeanSquareError();
                final double[] vcv = new double[] {
                    this.ybar / (this.n - 1.0),
                    Double.NaN,
                    Double.NaN };
                return new RegressionResults(
                    params, new double[][] { vcv }, true, this.n, 1,
                    this.sumY, this.sumYY, this.getSumSquaredErrors(), true, false);
            }
        } else {
            if (this.n < 2) {
                throw new NoDataException(PatriusMessages.NOT_ENOUGH_DATA_REGRESSION);
            }
            if (Double.isNaN(this.sumXX)) {
                final double[] vcv = new double[] { Double.NaN };
                final double[] params = new double[] { Double.NaN };
                return new RegressionResults(
                    params, new double[][] { vcv }, true, this.n, 1,
                    Double.NaN, Double.NaN, Double.NaN, false, false);
            } else {
                final double[] vcv = new double[] { this.getMeanSquareError() / this.sumXX };
                final double[] params = new double[] { this.sumXY / this.sumXX };
                return new RegressionResults(
                    params, new double[][] { vcv }, true, this.n, 1,
                    this.sumY, this.sumYY, this.getSumSquaredErrors(), false, false);
            }
        }
    }

    /**
     * Performs a regression on data present in buffers including only regressors
     * indexed in variablesToInclude and outputs a RegressionResults object
     * 
     * @param variablesToInclude
     *        an array of indices of regressors to include
     * @return RegressionResults acts as a container of regression output
     * @throws MathIllegalArgumentException
     *         if the variablesToInclude array is null or zero length
     * @throws OutOfRangeException
     *         if a requested variable is not present in model
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @Override
    public RegressionResults regress(final int[] variablesToInclude) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume ReturnCount check
        if (variablesToInclude == null || variablesToInclude.length == 0) {
            throw new MathIllegalArgumentException(PatriusMessages.ARRAY_ZERO_LENGTH_OR_NULL_NOT_ALLOWED);
        }
        if (variablesToInclude.length > 2 || (variablesToInclude.length > 1 && !this.hasInterceptFlag)) {
            throw new ModelSpecificationException(
                PatriusMessages.ARRAY_SIZE_EXCEEDS_MAX_VARIABLES,
                (variablesToInclude.length > 1 && !this.hasInterceptFlag) ? 1 : 2);
        }

        if (this.hasInterceptFlag) {
            if (variablesToInclude.length == 2) {
                if (variablesToInclude[0] == 1) {
                    throw new ModelSpecificationException(PatriusMessages.NOT_INCREASING_SEQUENCE);
                } else if (variablesToInclude[0] != 0) {
                    throw new OutOfRangeException(variablesToInclude[0], 0, 1);
                }
                if (variablesToInclude[1] != 1) {
                    throw new OutOfRangeException(variablesToInclude[0], 0, 1);
                }
                return this.regress();
            } else {
                if (variablesToInclude[0] != 1 && variablesToInclude[0] != 0) {
                    throw new OutOfRangeException(variablesToInclude[0], 0, 1);
                }
                final double mean = this.sumY * this.sumY / this.n;
                final double syy = this.sumYY + mean;
                if (variablesToInclude[0] == 0) {
                    // just the mean
                    final double[] vcv = new double[] { this.sumYY / (((this.n - 1) * this.n)) };
                    final double[] params = new double[] { this.ybar };
                    return new RegressionResults(
                        params, new double[][] { vcv }, true, this.n, 1,
                        this.sumY, syy + mean, this.sumYY, true, false);

                } else if (variablesToInclude[0] == 1) {
                    // final double _syy = sumYY + sumY * sumY / ((double) n);
                    final double sxx = this.sumXX + this.sumX * this.sumX / this.n;
                    final double sxy = this.sumXY + this.sumX * this.sumY / this.n;
                    final double sse = MathLib.max(0d, syy - sxy * sxy / sxx);
                    final double mse = sse / ((this.n - 1));
                    if (Double.isNaN(sxx)) {
                        final double[] vcv = new double[] { Double.NaN };
                        final double[] params = new double[] { Double.NaN };
                        return new RegressionResults(
                            params, new double[][] { vcv }, true, this.n, 1,
                            Double.NaN, Double.NaN, Double.NaN, false, false);
                    } else {
                        final double[] vcv = new double[] { mse / sxx };
                        final double[] params = new double[] { sxy / sxx };
                        return new RegressionResults(
                            params, new double[][] { vcv }, true, this.n, 1,
                            this.sumY, syy, sse, false, false);
                    }
                }
            }
        } else {
            if (variablesToInclude[0] != 0) {
                throw new OutOfRangeException(variablesToInclude[0], 0, 0);
            }
            return this.regress();
        }

        return null;
    }

    // CHECKSTYLE: resume CommentRatio check
}
