/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
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

import java.util.Arrays;

import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * This class is a concrete implementation of the {@link UpdatingMultipleLinearRegression} interface.
 * 
 * <p>
 * The algorithm is described in:
 * 
 * <pre>
 * Algorithm AS 274: Least Squares Routines to Supplement Those of Gentleman
 * Author(s): Alan J. Miller
 * Source: Journal of the Royal Statistical Society.
 * Series C (Applied Statistics), Vol. 41, No. 2
 * (1992), pp. 458-478
 * Published by: Blackwell Publishing for the Royal Statistical Society
 * Stable URL: http://www.jstor.org/stable/2347583
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * This method for multiple regression forms the solution to the OLS problem by updating the QR decomposition as
 * described by Gentleman.
 * </p>
 * 
 * @version $Id: MillerUpdatingRegression.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class MillerUpdatingRegression implements UpdatingMultipleLinearRegression {

    /** number of variables in regression */
    private final int nvars;
    /** diagonals of cross products matrix */
    private final double[] d;
    /** the elements of the R`Y */
    private final double[] rhs;
    /** the off diagonal portion of the R matrix */
    private final double[] r;
    /** the tolerance for each of the variables */
    private final double[] tol;
    /** residual sum of squares for all nested regressions */
    private final double[] rss;
    /** order of the regressors */
    private final int[] vorder;
    /** scratch space for tolerance calc */
    private final double[] workTolset;
    /** number of observations entered */
    private long nobs = 0;
    /** sum of squared errors of largest regression */
    private double sserr = 0.0;
    /** has rss been called? */
    private boolean rssSet = false;
    /** has the tolerance setting method been called */
    private boolean tolSetFlag = false;
    /** flags for variables with linear dependency problems */
    private final boolean[] lindep;
    /** singular x values */
    private final double[] xSing;
    /** workspace for singularity method */
    private final double[] workSing;
    /** summation of Y variable */
    private double sumy = 0.0;
    /** summation of squared Y values */
    private double sumsqy = 0.0;
    /** boolean flag whether a regression constant is added */
    private final boolean hasInterceptFlag;
    /** zero tolerance */
    private final double epsilon;

    /**
     * This is the augmented constructor for the MillerUpdatingRegression class.
     * 
     * @param numberOfVariables
     *        number of regressors to expect, not including constant
     * @param includeConstant
     *        include a constant automatically
     * @param errorTolerance
     *        zero tolerance, how machine zero is determined
     * @throws ModelSpecificationException
     *         if {@code numberOfVariables is less than 1}
     */
    public MillerUpdatingRegression(final int numberOfVariables,
        final boolean includeConstant, final double errorTolerance) {
        if (numberOfVariables < 1) {
            throw new ModelSpecificationException(PatriusMessages.NO_REGRESSORS);
        }
        if (includeConstant) {
            this.nvars = numberOfVariables + 1;
        } else {
            this.nvars = numberOfVariables;
        }
        this.hasInterceptFlag = includeConstant;
        this.nobs = 0;
        this.d = new double[this.nvars];
        this.rhs = new double[this.nvars];
        this.r = new double[this.nvars * (this.nvars - 1) / 2];
        this.tol = new double[this.nvars];
        this.rss = new double[this.nvars];
        this.vorder = new int[this.nvars];
        this.xSing = new double[this.nvars];
        this.workSing = new double[this.nvars];
        this.workTolset = new double[this.nvars];
        this.lindep = new boolean[this.nvars];
        for (int i = 0; i < this.nvars; i++) {
            this.vorder[i] = i;
        }
        if (errorTolerance > 0) {
            this.epsilon = errorTolerance;
        } else {
            this.epsilon = -errorTolerance;
        }
    }

    /**
     * Primary constructor for the MillerUpdatingRegression.
     * 
     * @param numberOfVariables
     *        maximum number of potential regressors
     * @param includeConstant
     *        include a constant automatically
     * @throws ModelSpecificationException
     *         if {@code numberOfVariables is less than 1}
     */
    public MillerUpdatingRegression(final int numberOfVariables, final boolean includeConstant) {
        this(numberOfVariables, includeConstant, Precision.EPSILON);
    }

    /**
     * A getter method which determines whether a constant is included.
     * 
     * @return true regression has an intercept, false no intercept
     */
    @Override
    public boolean hasIntercept() {
        return this.hasInterceptFlag;
    }

    /**
     * Gets the number of observations added to the regression model.
     * 
     * @return number of observations
     */
    @Override
    public long getN() {
        return this.nobs;
    }

    /**
     * Adds an observation to the regression model.
     * 
     * @param x
     *        the array with regressor values
     * @param y
     *        the value of dependent variable given these regressors
     * @exception ModelSpecificationException
     *            if the length of {@code x} does not equal
     *            the number of independent variables in the model
     */
    @Override
    public void addObservation(final double[] x, final double y) {

        if ((!this.hasInterceptFlag && x.length != this.nvars) ||
            (this.hasInterceptFlag && x.length + 1 != this.nvars)) {
            throw new ModelSpecificationException(PatriusMessages.INVALID_REGRESSION_OBSERVATION,
                x.length, this.nvars);
        }
        if (this.hasInterceptFlag) {
            final double[] tmp = new double[x.length + 1];
            System.arraycopy(x, 0, tmp, 1, x.length);
            tmp[0] = 1.0;
            this.include(tmp, 1.0, y);
        } else {
            this.include(MathArrays.copyOf(x, x.length), 1.0, y);
        }
        ++this.nobs;

    }

    /**
     * Adds multiple observations to the model.
     * 
     * @param x
     *        observations on the regressors
     * @param y
     *        observations on the regressand
     * @throws ModelSpecificationException
     *         if {@code x} is not rectangular, does not match
     *         the length of {@code y} or does not contain sufficient data to estimate the model
     */
    @Override
    public void addObservations(final double[][] x, final double[] y) {
        if ((x == null) || (y == null) || (x.length != y.length)) {
            throw new ModelSpecificationException(
                PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE,
                (x == null) ? 0 : x.length,
                (y == null) ? 0 : y.length);
        }
        if (x.length == 0) {
            // Must be no y data either
            throw new ModelSpecificationException(
                PatriusMessages.NO_DATA);
        }
        if (x[0].length + 1 > x.length) {
            throw new ModelSpecificationException(
                PatriusMessages.NOT_ENOUGH_DATA_FOR_NUMBER_OF_PREDICTORS,
                x.length, x[0].length);
        }
        for (int i = 0; i < x.length; i++) {
            this.addObservation(x[i], y[i]);
        }
    }

    /**
     * The include method is where the QR decomposition occurs. This statement forms all
     * intermediate data which will be used for all derivative measures.
     * According to the miller paper, note that in the original implementation the x vector
     * is overwritten. In this implementation, the include method is passed a copy of the
     * original data vector so that there is no contamination of the data. Additionally,
     * this method differs slightly from Gentleman's method, in that the assumption is
     * of dense design matrices, there is some advantage in using the original gentleman algorithm
     * on sparse matrices.
     * 
     * @param x
     *        observations on the regressors
     * @param wi
     *        weight of the this observation (-1,1)
     * @param yi
     *        observation on the regressand
     */
    private void include(final double[] x, final double wi, final double yi) {
        int nextr = 0;
        double w = wi;
        double y = yi;
        double xi;
        double di;
        double wxi;
        double dpi;
        double xk;
        double w2;
        this.rssSet = false;
        this.sumy = this.smartAdd(yi, this.sumy);
        this.sumsqy = this.smartAdd(this.sumsqy, yi * yi);
        for (int i = 0; i < x.length; i++) {
            if (w == 0.0) {
                return;
            }
            xi = x[i];

            if (xi == 0.0) {
                nextr += this.nvars - i - 1;
                continue;
            }
            di = this.d[i];
            wxi = w * xi;
            w2 = w;
            if (di == 0.0) {
                dpi = wxi * xi;
                w = 0.0;
            } else {
                dpi = this.smartAdd(di, wxi * xi);
                final double tmp = wxi * xi / di;
                if (MathLib.abs(tmp) > Precision.EPSILON) {
                    w = (di * w) / dpi;
                }
            }
            this.d[i] = dpi;
            for (int k = i + 1; k < this.nvars; k++) {
                xk = x[k];
                x[k] = this.smartAdd(xk, -xi * this.r[nextr]);
                if (di == 0.0) {
                    this.r[nextr] = xk / xi;
                } else {
                    this.r[nextr] = this.smartAdd(di * this.r[nextr], (w2 * xi) * xk) / dpi;
                }
                ++nextr;
            }
            xk = y;
            y = this.smartAdd(xk, -xi * this.rhs[i]);
            if (di == 0.0) {
                this.rhs[i] = xk / xi;
            } else {
                this.rhs[i] = this.smartAdd(di * this.rhs[i], wxi * xk) / dpi;
            }
        }
        this.sserr = this.smartAdd(this.sserr, w * y * y);
    }

    /**
     * Adds to number a and b such that the contamination due to
     * numerical smallness of one addend does not corrupt the sum.
     * 
     * @param a
     *        - an addend
     * @param b
     *        - an addend
     * @return the sum of the a and b
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private double smartAdd(final double a, final double b) {
        // CHECKSTYLE: resume ReturnCount check
        final double a2 = MathLib.abs(a);
        final double b2 = MathLib.abs(b);
        if (a2 > b2) {
            final double eps = a2 * Precision.EPSILON;
            if (b2 > eps) {
                return a + b;
            }
            return a;
        } else {
            final double eps = b2 * Precision.EPSILON;
            if (a2 > eps) {
                return a + b;
            }
            return b;
        }
    }

    /**
     * As the name suggests, clear wipes the internals and reorders everything in the
     * canonical order.
     */
    @Override
    public void clear() {
        Arrays.fill(this.d, 0.0);
        Arrays.fill(this.rhs, 0.0);
        Arrays.fill(this.r, 0.0);
        Arrays.fill(this.tol, 0.0);
        Arrays.fill(this.rss, 0.0);
        Arrays.fill(this.workTolset, 0.0);
        Arrays.fill(this.workSing, 0.0);
        Arrays.fill(this.xSing, 0.0);
        Arrays.fill(this.lindep, false);
        for (int i = 0; i < this.nvars; i++) {
            this.vorder[i] = i;
        }
        this.nobs = 0;
        this.sserr = 0.0;
        this.sumy = 0.0;
        this.sumsqy = 0.0;
        this.rssSet = false;
        this.tolSetFlag = false;
    }

    /**
     * This sets up tolerances for singularity testing.
     */
    private void tolset() {
        int pos;
        double total;
        final double eps = this.epsilon;
        for (int i = 0; i < this.nvars; i++) {
            this.workTolset[i] = Math.sqrt(this.d[i]);
        }
        this.tol[0] = eps * this.workTolset[0];
        for (int col = 1; col < this.nvars; col++) {
            pos = col - 1;
            total = this.workTolset[col];
            for (int row = 0; row < col; row++) {
                total += Math.abs(this.r[pos]) * this.workTolset[row];
                pos += this.nvars - row - 2;
            }
            this.tol[col] = eps * total;
        }
        this.tolSetFlag = true;
    }

    /**
     * The regcf method conducts the linear regression and extracts the
     * parameter vector. Notice that the algorithm can do subset regression
     * with no alteration.
     * 
     * @param nreq
     *        how many of the regressors to include (either in canonical
     *        order, or in the current reordered state)
     * @return an array with the estimated slope coefficients
     * @throws ModelSpecificationException
     *         if {@code nreq} is less than 1
     *         or greater than the number of independent variables
     */
    private double[] regcf(final int nreq) {
        if (nreq < 1) {
            throw new ModelSpecificationException(PatriusMessages.NO_REGRESSORS);
        }
        if (nreq > this.nvars) {
            throw new ModelSpecificationException(
                PatriusMessages.TOO_MANY_REGRESSORS, nreq, this.nvars);
        }
        if (!this.tolSetFlag) {
            this.tolset();
        }
        int nextr;
        final double[] ret = new double[nreq];
        boolean rankProblem = false;
        for (int i = nreq - 1; i > -1; i--) {
            if (Math.sqrt(this.d[i]) < this.tol[i]) {
                ret[i] = 0.0;
                this.d[i] = 0.0;
                rankProblem = true;
            } else {
                ret[i] = this.rhs[i];
                nextr = i * (this.nvars + this.nvars - i - 1) / 2;
                for (int j = i + 1; j < nreq; j++) {
                    ret[i] = this.smartAdd(ret[i], -this.r[nextr] * ret[j]);
                    ++nextr;
                }
            }
        }
        if (rankProblem) {
            for (int i = 0; i < nreq; i++) {
                if (this.lindep[i]) {
                    ret[i] = Double.NaN;
                }
            }
        }
        return ret;
    }

    /**
     * The method which checks for singularities and then eliminates the offending
     * columns.
     */
    private void singcheck() {
        int pos;
        for (int i = 0; i < this.nvars; i++) {
            this.workSing[i] = Math.sqrt(this.d[i]);
        }
        for (int col = 0; col < this.nvars; col++) {
            // Set elements within R to zero if they are less than tol(col) in
            // absolute value after being scaled by the square root of their row
            // multiplier
            final double temp = this.tol[col];
            pos = col - 1;
            for (int row = 0; row < col - 1; row++) {
                if (Math.abs(this.r[pos]) * this.workSing[row] < temp) {
                    this.r[pos] = 0.0;
                }
                pos += this.nvars - row - 2;
            }
            // If diagonal element is near zero, set it to zero, set appropriate
            // element of LINDEP, and use INCLUD to augment the projections in
            // the lower rows of the orthogonalization.
            this.lindep[col] = false;
            if (this.workSing[col] < temp) {
                this.lindep[col] = true;
                if (col < this.nvars - 1) {
                    Arrays.fill(this.xSing, 0.0);
                    int pi2 = col * (this.nvars + this.nvars - col - 1) / 2;
                    for (int xi2 = col + 1; xi2 < this.nvars; xi2++, pi2++) {
                        this.xSing[xi2] = this.r[pi2];
                        this.r[pi2] = 0.0;
                    }
                    final double y = this.rhs[col];
                    final double weight = this.d[col];
                    this.d[col] = 0.0;
                    this.rhs[col] = 0.0;
                    this.include(this.xSing, weight, y);
                } else {
                    this.sserr += this.d[col] * this.rhs[col] * this.rhs[col];
                }
            }
        }
    }

    /**
     * Calculates the sum of squared errors for the full regression
     * and all subsets in the following manner:
     * 
     * <pre>
     * rss[] ={
     * ResidualSumOfSquares_allNvars,
     * ResidualSumOfSquares_FirstNvars-1,
     * ResidualSumOfSquares_FirstNvars-2,
     * ..., ResidualSumOfSquares_FirstVariable}
     * </pre>
     */
    private void sse() {
        double total = this.sserr;
        this.rss[this.nvars - 1] = this.sserr;
        for (int i = this.nvars - 1; i > 0; i--) {
            total += this.d[i] * this.rhs[i] * this.rhs[i];
            this.rss[i - 1] = total;
        }
        this.rssSet = true;
    }

    /**
     * Calculates the cov matrix assuming only the first nreq variables are
     * included in the calculation. The returned array contains a symmetric
     * matrix stored in lower triangular form. The matrix will have
     * ( nreq + 1 ) * nreq / 2 elements. For illustration
     * 
     * <pre>
     * cov =
     * {
     *  cov_00,
     *  cov_10, cov_11,
     *  cov_20, cov_21, cov22,
     *  ...
     * }
     * </pre>
     * 
     * @param nreq
     *        how many of the regressors to include (either in canonical
     *        order, or in the current reordered state)
     * @return an array with the variance covariance of the included
     *         regressors in lower triangular form
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private double[] cov(final int nreq) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (this.nobs <= nreq) {
            return null;
        }
        double rnk = 0.0;
        for (int i = 0; i < nreq; i++) {
            if (!this.lindep[i]) {
                rnk += 1.0;
            }
        }
        final double var = this.rss[nreq - 1] / (this.nobs - rnk);
        final double[] rinv = new double[nreq * (nreq - 1) / 2];
        this.inverse(rinv, nreq);
        final double[] covmat = new double[nreq * (nreq + 1) / 2];
        Arrays.fill(covmat, Double.NaN);
        int pos2;
        int pos1;
        int start = 0;
        double total = 0;
        for (int row = 0; row < nreq; row++) {
            pos2 = start;
            if (!this.lindep[row]) {
                for (int col = row; col < nreq; col++) {
                    if (this.lindep[col]) {
                        pos2 += nreq - col - 1;
                    } else {
                        pos1 = start + col - row;
                        if (row == col) {
                            total = 1.0 / this.d[col];
                        } else {
                            total = rinv[pos1 - 1] / this.d[col];
                        }
                        for (int k = col + 1; k < nreq; k++) {
                            if (!this.lindep[k]) {
                                total += rinv[pos1] * rinv[pos2] / this.d[k];
                            }
                            ++pos1;
                            ++pos2;
                        }
                        covmat[(col + 1) * col / 2 + row] = total * var;
                    }
                }
            }
            start += nreq - row - 1;
        }
        return covmat;
    }

    /**
     * This internal method calculates the inverse of the upper-triangular portion
     * of the R matrix.
     * 
     * @param rinv
     *        the storage for the inverse of r
     * @param nreq
     *        how many of the regressors to include (either in canonical
     *        order, or in the current reordered state)
     */
    private void inverse(final double[] rinv, final int nreq) {
        int pos = nreq * (nreq - 1) / 2 - 1;
        int pos1 = -1;
        int pos2 = -1;
        double total = 0.0;
        Arrays.fill(rinv, Double.NaN);
        for (int row = nreq - 1; row > 0; --row) {
            if (this.lindep[row]) {
                pos -= nreq - row;
            } else {
                final int start = (row - 1) * (this.nvars + this.nvars - row) / 2;
                for (int col = nreq; col > row; --col) {
                    pos1 = start;
                    pos2 = pos;
                    total = 0.0;
                    for (int k = row; k < col - 1; k++) {
                        pos2 += nreq - k - 1;
                        if (!this.lindep[k]) {
                            total += -this.r[pos1] * rinv[pos2];
                        }
                        ++pos1;
                    }
                    rinv[pos] = total - this.r[pos1];
                    --pos;
                }
            }
        }
    }

    /**
     * In the original algorithm only the partial correlations of the regressors
     * is returned to the user. In this implementation, we have
     * 
     * <pre>
     * corr =
     * {
     *   corrxx - lower triangular
     *   corrxy - bottom row of the matrix
     * }
     * Replaces subroutines PCORR and COR of:
     * ALGORITHM AS274  APPL. STATIST. (1992) VOL.41, NO. 2
     * </pre>
     * 
     * <p>
     * Calculate partial correlations after the variables in rows 1, 2, ..., IN have been forced into the regression. If
     * IN = 1, and the first row of R represents a constant in the model, then the usual simple correlations are
     * returned.
     * </p>
     * 
     * <p>
     * If IN = 0, the value returned in array CORMAT for the correlation of variables Xi & Xj is:
     * 
     * <pre>
     * sum(Xi.Xj) / Sqrt(sum(Xi &circ; 2).sum(Xj &circ; 2))
     * </pre>
     * 
     * </p>
     * 
     * <p>
     * On return, array CORMAT contains the upper triangle of the matrix of partial correlations stored by rows,
     * excluding the 1's on the diagonal. e.g. if IN = 2, the consecutive elements returned are: (3,4) (3,5) ...
     * (3,ncol), (4,5) (4,6) ... (4,ncol), etc. Array YCORR stores the partial correlations with the Y-variable starting
     * with YCORR(IN+1) = partial correlation with the variable in position (IN+1).
     * </p>
     * 
     * @param size
     *        how many of the regressors to include (either in canonical
     *        order, or in the current reordered state)
     * @return an array with the partial correlations of the remainder of
     *         regressors with each other and the regressand, in lower triangular form
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public double[] getPartialCorrelations(final int size) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (size < -1 || size >= this.nvars) {
            return null;
        }
        final int rmsOff = -size;
        final double[] rms = new double[this.nvars - size];
        final int offXX = (this.nvars - size) * (this.nvars - size - 1) / 2;
        final int nvm = this.nvars - 1;
        final int basePos = this.r.length - (nvm - size) * (nvm - size + 1) / 2;
        if (this.d[size] > 0.0) {
            rms[size + rmsOff] = 1.0 / Math.sqrt(this.d[size]);
        }
        int pos;
        int pos1;
        int pos2;
        for (int col = size + 1; col < this.nvars; col++) {
            pos = basePos + col - 1 - size;
            double sumxx = this.d[col];
            for (int row = size; row < col; row++) {
                sumxx += this.d[row] * this.r[pos] * this.r[pos];
                pos += this.nvars - row - 2;
            }
            if (sumxx > 0.0) {
                rms[col + rmsOff] = 1.0 / Math.sqrt(sumxx);
            } else {
                rms[col + rmsOff] = 0.0;
            }
        }
        double sumyy = this.sserr;
        for (int row = size; row < this.nvars; row++) {
            sumyy += this.d[row] * this.rhs[row] * this.rhs[row];
        }
        if (sumyy > 0.0) {
            sumyy = 1.0 / Math.sqrt(sumyy);
        }
        pos = 0;
        final int wrkOff = -(size + 1);
        final double[] work = new double[this.nvars - size - 1];
        final double[] output = new double[(this.nvars - size + 1) * (this.nvars - size) / 2];
        for (int col1 = size; col1 < this.nvars; col1++) {
            double sumxy = 0.0;
            Arrays.fill(work, 0.0);
            pos1 = basePos + col1 - size - 1;
            for (int row = size; row < col1; row++) {
                pos2 = pos1 + 1;
                for (int col2 = col1 + 1; col2 < this.nvars; col2++) {
                    work[col2 + wrkOff] += this.d[row] * this.r[pos1] * this.r[pos2];
                    pos2++;
                }
                sumxy += this.d[row] * this.r[pos1] * this.rhs[row];
                pos1 += this.nvars - row - 2;
            }
            pos2 = pos1 + 1;
            for (int col2 = col1 + 1; col2 < this.nvars; col2++) {
                work[col2 + wrkOff] += this.d[col1] * this.r[pos2];
                ++pos2;
                output[(col2 - 1 - size) * (col2 - size) / 2 + col1 - size] =
                    work[col2 + wrkOff] * rms[col1 + rmsOff] * rms[col2 + rmsOff];
                ++pos;
            }
            sumxy += this.d[col1] * this.rhs[col1];
            output[col1 + rmsOff + offXX] = sumxy * rms[col1 + rmsOff] * sumyy;
        }

        return output;
    }

    /**
     * ALGORITHM AS274 APPL. STATIST. (1992) VOL.41, NO. 2.
     * Move variable from position FROM to position TO in an
     * orthogonal reduction produced by AS75.1.
     * 
     * @param from
     *        initial position
     * @param to
     *        destination
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void vmove(final int from, final int to) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (from == to) {
            // Special case
            return;
        }
        double d1;
        double d2;
        double x;
        double d1new;
        double d2new;
        double cbar;
        double sbar;
        double y;
        final int first;
        final int inc;
        int m1;
        int m2;
        int mp1;
        int pos;
        boolean bSkipTo40 = false;
        if (!this.rssSet) {
            this.sse();
        }
        int count = 0;
        if (from < to) {
            first = from;
            inc = 1;
            count = to - from;
        } else {
            first = from - 1;
            inc = -1;
            count = from - to;
        }

        int m = first;
        int idx = 0;
        while (idx < count) {
            m1 = m * (this.nvars + this.nvars - m - 1) / 2;
            m2 = m1 + this.nvars - m - 1;
            mp1 = m + 1;

            d1 = this.d[m];
            d2 = this.d[mp1];
            // Special cases.
            if (d1 > this.epsilon || d2 > this.epsilon) {
                x = this.r[m1];
                if (Math.abs(x) * Math.sqrt(d1) < this.tol[mp1]) {
                    x = 0.0;
                }
                if (d1 < this.epsilon || Math.abs(x) < this.epsilon) {
                    this.d[m] = d2;
                    this.d[mp1] = d1;
                    this.r[m1] = 0.0;
                    for (int col = m + 2; col < this.nvars; col++) {
                        ++m1;
                        x = this.r[m1];
                        this.r[m1] = this.r[m2];
                        this.r[m2] = x;
                        ++m2;
                    }
                    x = this.rhs[m];
                    this.rhs[m] = this.rhs[mp1];
                    this.rhs[mp1] = x;
                    bSkipTo40 = true;
                    // break;
                } else if (d2 < this.epsilon) {
                    this.d[m] = d1 * x * x;
                    this.r[m1] = 1.0 / x;
                    for (int i = m1 + 1; i < m1 + this.nvars - m - 1; i++) {
                        this.r[i] /= x;
                    }
                    this.rhs[m] = this.rhs[m] / x;
                    bSkipTo40 = true;
                    // break;
                }
                if (!bSkipTo40) {
                    d1new = d2 + d1 * x * x;
                    cbar = d2 / d1new;
                    sbar = x * d1 / d1new;
                    d2new = d1 * cbar;
                    this.d[m] = d1new;
                    this.d[mp1] = d2new;
                    this.r[m1] = sbar;
                    for (int col = m + 2; col < this.nvars; col++) {
                        ++m1;
                        y = this.r[m1];
                        this.r[m1] = cbar * this.r[m2] + sbar * y;
                        this.r[m2] = y - x * this.r[m2];
                        ++m2;
                    }
                    y = this.rhs[m];
                    this.rhs[m] = cbar * this.rhs[mp1] + sbar * y;
                    this.rhs[mp1] = y - x * this.rhs[mp1];
                }
            }
            if (m > 0) {
                pos = m;
                for (int row = 0; row < m; row++) {
                    x = this.r[pos];
                    this.r[pos] = this.r[pos - 1];
                    this.r[pos - 1] = x;
                    pos += this.nvars - row - 2;
                }
            }
            // Adjust variable order (VORDER), the tolerances (TOL) and
            // the vector of residual sums of squares (RSS).
            m1 = this.vorder[m];
            this.vorder[m] = this.vorder[mp1];
            this.vorder[mp1] = m1;
            x = this.tol[m];
            this.tol[m] = this.tol[mp1];
            this.tol[mp1] = x;
            this.rss[m] = this.rss[mp1] + this.d[mp1] * this.rhs[mp1] * this.rhs[mp1];

            m += inc;
            ++idx;
        }
    }

    /**
     * ALGORITHM AS274 APPL. STATIST. (1992) VOL.41, NO. 2
     * 
     * <p>
     * Re-order the variables in an orthogonal reduction produced by AS75.1 so that the N variables in LIST start at
     * position POS1, though will not necessarily be in the same order as in LIST. Any variables in VORDER before
     * position POS1 are not moved. Auxiliary routine called: VMOVE.
     * </p>
     * 
     * <p>
     * This internal method reorders the regressors.
     * </p>
     * 
     * @param list
     *        the regressors to move
     * @param pos1
     *        where the list will be placed
     * @return -1 error, 0 everything ok
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    private int reorderRegressors(final int[] list, final int pos1) {
        // CHECKSTYLE: resume ReturnCount check
        if (list.length < 1 || list.length > this.nvars + 1 - pos1) {
            // Error
            return -1;
        }
        // Initialization
        int next;
        int i;
        int l;
        next = pos1;
        i = pos1;
        // Loop to reorder the list
        while (i < this.nvars) {
            l = this.vorder[i];
            for (final int element : list) {
                if (l == element && i > next) {
                    this.vmove(i, next);
                    ++next;
                    if (next >= list.length + pos1) {
                        return 0;
                    } else {
                        break;
                    }
                }
            }
            ++i;
        }
        // Return result
        return 0;
    }

    /**
     * Gets the diagonal of the Hat matrix also known as the leverage matrix.
     * 
     * @param rowData
     *        returns the diagonal of the hat matrix for this observation
     * @return the diagonal element of the hatmatrix
     */
    public double getDiagonalOfHatMatrix(final double[] rowData) {
        if (rowData.length > this.nvars) {
            return Double.NaN;
        }

        final double[] wk = new double[this.nvars];
        int pos;
        double total;

        final double[] xrow;
        if (this.hasInterceptFlag) {
            xrow = new double[rowData.length + 1];
            xrow[0] = 1.0;
            System.arraycopy(rowData, 0, xrow, 1, rowData.length);
        } else {
            xrow = rowData;
        }
        double hii = 0.0;
        for (int col = 0; col < xrow.length; col++) {
            if (Math.sqrt(this.d[col]) < this.tol[col]) {
                wk[col] = 0.0;
            } else {
                pos = col - 1;
                total = xrow[col];
                for (int row = 0; row < col; row++) {
                    total = this.smartAdd(total, -wk[row] * this.r[pos]);
                    pos += this.nvars - row - 2;
                }
                wk[col] = total;
                hii = this.smartAdd(hii, (total * total) / this.d[col]);
            }
        }
        return hii;
    }

    /**
     * Gets the order of the regressors, useful if some type of reordering
     * has been called. Calling regress with int[]{} args will trigger
     * a reordering.
     * 
     * @return int[] with the current order of the regressors
     */
    public int[] getOrderOfRegressors() {
        return MathArrays.copyOf(this.vorder);
    }

    /**
     * Conducts a regression on the data in the model, using all regressors.
     * 
     * @return RegressionResults the structure holding all regression results
     * @exception ModelSpecificationException
     *            - thrown if number of observations is
     *            less than the number of variables
     */
    @Override
    public RegressionResults regress() {
        return this.regress(this.nvars);
    }

    /**
     * Conducts a regression on the data in the model, using a subset of regressors.
     * 
     * @param numberOfRegressors
     *        many of the regressors to include (either in canonical
     *        order, or in the current reordered state)
     * @return RegressionResults the structure holding all regression results
     * @exception ModelSpecificationException
     *            - thrown if number of observations is
     *            less than the number of variables or number of regressors requested
     *            is greater than the regressors in the model
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public RegressionResults regress(final int numberOfRegressors) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (this.nobs <= numberOfRegressors) {
            throw new ModelSpecificationException(
                PatriusMessages.NOT_ENOUGH_DATA_FOR_NUMBER_OF_PREDICTORS,
                this.nobs, numberOfRegressors);
        }
        if (numberOfRegressors > this.nvars) {
            throw new ModelSpecificationException(
                PatriusMessages.TOO_MANY_REGRESSORS, numberOfRegressors, this.nvars);
        }

        this.tolset();
        this.singcheck();

        final double[] beta = this.regcf(numberOfRegressors);

        this.sse();

        final double[] cov = this.cov(numberOfRegressors);

        int rnk = 0;
        for (int i = 0; i < this.lindep.length; i++) {
            if (!this.lindep[i]) {
                ++rnk;
            }
        }

        boolean needsReorder = false;
        for (int i = 0; i < numberOfRegressors; i++) {
            if (this.vorder[i] != i) {
                needsReorder = true;
                break;
            }
        }
        if (needsReorder) {
            final double[] betaNew = new double[beta.length];
            final double[] covNew = new double[cov.length];

            final int[] newIndices = new int[beta.length];
            for (int i = 0; i < this.nvars; i++) {
                for (int j = 0; j < numberOfRegressors; j++) {
                    if (this.vorder[j] == i) {
                        betaNew[i] = beta[j];
                        newIndices[i] = j;
                    }
                }
            }

            int idx1 = 0;
            int idx2;
            int i2;
            int j2;
            for (int i = 0; i < beta.length; i++) {
                i2 = newIndices[i];
                for (int j = 0; j <= i; j++, idx1++) {
                    j2 = newIndices[j];
                    if (i2 > j2) {
                        idx2 = i2 * (i2 + 1) / 2 + j2;
                    } else {
                        idx2 = j2 * (j2 + 1) / 2 + i2;
                    }
                    covNew[idx1] = cov[idx2];
                }
            }
            return new RegressionResults(
                betaNew, new double[][] { covNew }, true, this.nobs, rnk,
                this.sumy, this.sumsqy, this.sserr, this.hasInterceptFlag, false);
        } else {
            return new RegressionResults(
                beta, new double[][] { cov }, true, this.nobs, rnk,
                this.sumy, this.sumsqy, this.sserr, this.hasInterceptFlag, false);
        }
    }

    /**
     * Conducts a regression on the data in the model, using regressors in array
     * Calling this method will change the internal order of the regressors
     * and care is required in interpreting the hatmatrix.
     * 
     * @param variablesToInclude
     *        array of variables to include in regression
     * @return RegressionResults the structure holding all regression results
     * @exception ModelSpecificationException
     *            - thrown if number of observations is
     *            less than the number of variables, the number of regressors requested
     *            is greater than the regressors in the model or a regressor index in
     *            regressor array does not exist
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Commons-Math code kept as such
    @Override
    public RegressionResults regress(final int[] variablesToInclude) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (variablesToInclude.length > this.nvars) {
            throw new ModelSpecificationException(
                PatriusMessages.TOO_MANY_REGRESSORS, variablesToInclude.length, this.nvars);
        }
        if (this.nobs <= this.nvars) {
            throw new ModelSpecificationException(
                PatriusMessages.NOT_ENOUGH_DATA_FOR_NUMBER_OF_PREDICTORS,
                this.nobs, this.nvars);
        }
        Arrays.sort(variablesToInclude);
        int iExclude = 0;
        for (int i = 0; i < variablesToInclude.length; i++) {
            if (i >= this.nvars) {
                throw new ModelSpecificationException(
                    PatriusMessages.INDEX_LARGER_THAN_MAX, i, this.nvars);
            }
            if (i > 0 && variablesToInclude[i] == variablesToInclude[i - 1]) {
                variablesToInclude[i] = -1;
                ++iExclude;
            }
        }
        final int[] series;
        if (iExclude > 0) {
            int j = 0;
            series = new int[variablesToInclude.length - iExclude];
            for (final int element : variablesToInclude) {
                if (element > -1) {
                    series[j] = element;
                    ++j;
                }
            }
        } else {
            series = variablesToInclude;
        }

        this.reorderRegressors(series, 0);
        this.tolset();
        this.singcheck();

        final double[] beta = this.regcf(series.length);

        this.sse();

        final double[] cov = this.cov(series.length);

        int rnk = 0;
        for (int i = 0; i < this.lindep.length; i++) {
            if (!this.lindep[i]) {
                ++rnk;
            }
        }

        boolean needsReorder = false;
        for (int i = 0; i < this.nvars; i++) {
            if (this.vorder[i] != series[i]) {
                needsReorder = true;
                break;
            }
        }
        if (needsReorder) {
            final double[] betaNew = new double[beta.length];
            final int[] newIndices = new int[beta.length];
            for (int i = 0; i < series.length; i++) {
                for (int j = 0; j < this.vorder.length; j++) {
                    if (this.vorder[j] == series[i]) {
                        betaNew[i] = beta[j];
                        newIndices[i] = j;
                    }
                }
            }
            final double[] covNew = new double[cov.length];
            int idx1 = 0;
            int idx2;
            int i2;
            int j2;
            for (int i = 0; i < beta.length; i++) {
                i2 = newIndices[i];
                for (int j = 0; j <= i; j++, idx1++) {
                    j2 = newIndices[j];
                    if (i2 > j2) {
                        idx2 = i2 * (i2 + 1) / 2 + j2;
                    } else {
                        idx2 = j2 * (j2 + 1) / 2 + i2;
                    }
                    covNew[idx1] = cov[idx2];
                }
            }
            return new RegressionResults(
                betaNew, new double[][] { covNew }, true, this.nobs, rnk,
                this.sumy, this.sumsqy, this.sserr, this.hasInterceptFlag, false);
        } else {
            return new RegressionResults(
                beta, new double[][] { cov }, true, this.nobs, rnk,
                this.sumy, this.sumsqy, this.sserr, this.hasInterceptFlag, false);
        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
