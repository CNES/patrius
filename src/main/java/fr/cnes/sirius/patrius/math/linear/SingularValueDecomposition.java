/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
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
package fr.cnes.sirius.patrius.math.linear;

import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Calculates the compact Singular Value Decomposition of a matrix.
 * <p>
 * The Singular Value Decomposition of matrix A is a set of three matrices: U, &Sigma; and V such that A = U &times;
 * &Sigma; &times; V<sup>T</sup>. Let A be a m &times; n matrix, then U is a m &times; p orthogonal matrix, &Sigma; is a
 * p &times; p diagonal matrix with positive or null elements, V is a p &times; n orthogonal matrix (hence V<sup>T</sup>
 * is also orthogonal) where p=min(m,n).
 * </p>
 * <p>
 * This class is similar to the class with similar name from the <a
 * href="http://math.nist.gov/javanumerics/jama/">JAMA</a> library, with the following changes:
 * </p>
 * <ul>
 * <li>the {@code norm2} method which has been renamed as {@link #getNorm()
 * getNorm},</li>
 * <li>the {@code cond} method which has been renamed as {@link #getConditionNumber() getConditionNumber},</li>
 * <li>the {@code rank} method which has been renamed as {@link #getRank()
 * getRank},</li>
 * <li>a {@link #getUT() getUT} method has been added,</li>
 * <li>a {@link #getVT() getVT} method has been added,</li>
 * <li>a {@link #getSolver() getSolver} method has been added,</li>
 * <li>a {@link #getCovariance(double) getCovariance} method has been added.</li>
 * </ul>
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @see <a href="http://mathworld.wolfram.com/SingularValueDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/Singular_value_decomposition">Wikipedia</a>
 * @version $Id: SingularValueDecomposition.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0 (changed to concrete class in 3.0)
 */
public class SingularValueDecomposition implements Decomposition {
    /** Relative threshold for small singular values. */
    private static final double EPS = 0x1.0p-52;
    /** Absolute threshold for small singular values. */
    private static final double TINY = 0x1.0p-966;
    /** Computed singular values. */
    private double[] singularValues;
    /** max(row dimension, column dimension). */
    private int m;
    /** min(row dimension, column dimension). */
    private int n;
    /** Indicator for transposed matrix. */
    private boolean transposed;
    /** Cached value of U matrix. */
    private RealMatrix cachedU;
    /** Cached value of V matrix. */
    private RealMatrix cachedV;
    /** Tolerance value for small singular values, calculated once we have populated "singularValues". */
    private double tol;

    /**
     * Simple constructor.
     * 
     * <p>
     * The decomposition is directly computed on the input matrix
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     */
    public SingularValueDecomposition(final RealMatrix matrix) {
        svdDecompose(matrix);
    }

    /**
     * Calculates the compact Singular Value Decomposition of the given matrix.
     * 
     * @param matrix
     *        The matrix to decompose.
     **/
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop AvoidNestedBlocks check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Commons-Math code kept as such
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private void svdDecompose(final RealMatrix matrix) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume CommentRatio check
        final double[][] a;

        // "m" is always the largest dimension.
        if (matrix.getRowDimension() < matrix.getColumnDimension()) {
            this.transposed = true;
            a = matrix.transpose().getData(false);
            this.m = matrix.getColumnDimension();
            this.n = matrix.getRowDimension();
        } else {
            this.transposed = false;
            a = matrix.getData(true);
            this.m = matrix.getRowDimension();
            this.n = matrix.getColumnDimension();
        }

        this.singularValues = new double[this.n];
        final double[][] u = new double[this.m][this.n];
        final double[][] v = new double[this.n][this.n];
        final double[] e = new double[this.n];
        final double[] work = new double[this.m];
        // Reduce A to bidiagonal form, storing the diagonal elements
        // in s and the super-diagonal elements in e.
        final int nct = MathLib.min(this.m - 1, this.n);
        final int nrt = MathLib.max(0, this.n - 2);
        for (int k = 0; k < MathLib.max(nct, nrt); k++) {
            if (k < nct) {
                // Compute the transformation for the k-th column and
                // place the k-th diagonal in s[k].
                // Compute 2-norm of k-th column without under/overflow.
                this.singularValues[k] = 0;
                for (int i = k; i < this.m; i++) {
                    this.singularValues[k] = MathLib.hypot(this.singularValues[k], a[i][k]);
                }
                if (this.singularValues[k] != 0) {
                    if (a[k][k] < 0) {
                        this.singularValues[k] = -this.singularValues[k];
                    }
                    for (int i = k; i < this.m; i++) {
                        a[i][k] /= this.singularValues[k];
                    }
                    a[k][k] += 1;
                }
                this.singularValues[k] = -this.singularValues[k];
            }
            for (int j = k + 1; j < this.n; j++) {
                if (k < nct &&
                        this.singularValues[k] != 0) {
                    // Apply the transformation.
                    double t = 0;
                    for (int i = k; i < this.m; i++) {
                        t += a[i][k] * a[i][j];
                    }
                    t = -t / a[k][k];
                    for (int i = k; i < this.m; i++) {
                        a[i][j] += t * a[i][k];
                    }
                }
                // Place the k-th row of A into e for the
                // subsequent calculation of the row transformation.
                e[j] = a[k][j];
            }
            if (k < nct) {
                // Place the transformation in U for subsequent back
                // multiplication.
                for (int i = k; i < this.m; i++) {
                    u[i][k] = a[i][k];
                }
            }
            if (k < nrt) {
                // Compute the k-th row transformation and place the
                // k-th super-diagonal in e[k].
                // Compute 2-norm without under/overflow.
                e[k] = 0;
                for (int i = k + 1; i < this.n; i++) {
                    e[k] = MathLib.hypot(e[k], e[i]);
                }
                if (e[k] != 0) {
                    if (e[k + 1] < 0) {
                        e[k] = -e[k];
                    }
                    for (int i = k + 1; i < this.n; i++) {
                        e[i] /= e[k];
                    }
                    e[k + 1] += 1;
                }
                e[k] = -e[k];
                if (k + 1 < this.m &&
                        e[k] != 0) {
                    // Apply the transformation.
                    for (int i = k + 1; i < this.m; i++) {
                        work[i] = 0;
                    }
                    for (int j = k + 1; j < this.n; j++) {
                        for (int i = k + 1; i < this.m; i++) {
                            work[i] += e[j] * a[i][j];
                        }
                    }
                    for (int j = k + 1; j < this.n; j++) {
                        final double t = -e[j] / e[k + 1];
                        for (int i = k + 1; i < this.m; i++) {
                            a[i][j] += t * work[i];
                        }
                    }
                }

                // Place the transformation in V for subsequent
                // back multiplication.
                for (int i = k + 1; i < this.n; i++) {
                    v[i][k] = e[i];
                }
            }
        }
        // Set up the final bidiagonal matrix or order p.
        int p = this.n;
        if (nct < this.n) {
            this.singularValues[nct] = a[nct][nct];
        }
        if (this.m < p) {
            this.singularValues[p - 1] = 0;
        }
        if (nrt + 1 < p) {
            e[nrt] = a[nrt][p - 1];
        }
        e[p - 1] = 0;

        // Generate U.
        for (int j = nct; j < this.n; j++) {
            for (int i = 0; i < this.m; i++) {
                u[i][j] = 0;
            }
            u[j][j] = 1;
        }
        for (int k = nct - 1; k >= 0; k--) {
            if (this.singularValues[k] == 0) {
                for (int i = 0; i < this.m; i++) {
                    u[i][k] = 0;
                }
                u[k][k] = 1;
            } else {
                for (int j = k + 1; j < this.n; j++) {
                    double t = 0;
                    for (int i = k; i < this.m; i++) {
                        t += u[i][k] * u[i][j];
                    }
                    t = -t / u[k][k];
                    for (int i = k; i < this.m; i++) {
                        u[i][j] += t * u[i][k];
                    }
                }
                for (int i = k; i < this.m; i++) {
                    u[i][k] = -u[i][k];
                }
                u[k][k] = 1 + u[k][k];
                for (int i = 0; i < k - 1; i++) {
                    u[i][k] = 0;
                }
            }
        }

        // Generate V.
        for (int k = this.n - 1; k >= 0; k--) {
            if (k < nrt &&
                    e[k] != 0) {
                for (int j = k + 1; j < this.n; j++) {
                    double t = 0;
                    for (int i = k + 1; i < this.n; i++) {
                        t += v[i][k] * v[i][j];
                    }
                    t = -t / v[k + 1][k];
                    for (int i = k + 1; i < this.n; i++) {
                        v[i][j] += t * v[i][k];
                    }
                }
            }
            for (int i = 0; i < this.n; i++) {
                v[i][k] = 0;
            }
            v[k][k] = 1;
        }

        // Main iteration loop for the singular values.
        final int pp = p - 1;
        while (p > 0) {
            int k;
            final int kase;
            // Here is where a test for too many iterations would go.
            // This section of the program inspects for
            // negligible elements in the s and e arrays. On
            // completion the variables kase and k are set as follows.
            // kase = 1 if s(p) and e[k-1] are negligible and k<p
            // kase = 2 if s(k) is negligible and k<p
            // kase = 3 if e[k-1] is negligible, k<p, and
            // s(k), ..., s(p) are not negligible (qr step).
            // kase = 4 if e(p-1) is negligible (convergence).
            for (k = p - 2; k >= 0; k--) {
                final double threshold = TINY + EPS * (MathLib.abs(this.singularValues[k]) +
                        MathLib.abs(this.singularValues[k + 1]));
                // the following condition is written this way in order
                // to break out of the loop when NaN occurs, writing it
                // as "if (FastMath.abs(e[k]) <= threshold)" would loop
                // indefinitely in case of NaNs because comparison on NaNs
                // always return false, regardless of what is checked
                // see issue MATH-947
                if (!(MathLib.abs(e[k]) > threshold)) {
                    e[k] = 0;
                    break;
                }
            }

            if (k == p - 2) {
                kase = 4;
            } else {
                int ks;
                for (ks = p - 1; ks >= k; ks--) {
                    if (ks == k) {
                        break;
                    }
                    final double t = (ks != p ? MathLib.abs(e[ks]) : 0) +
                            (ks != k + 1 ? MathLib.abs(e[ks - 1]) : 0);
                    if (MathLib.abs(this.singularValues[ks]) <= TINY + EPS * t) {
                        this.singularValues[ks] = 0;
                        break;
                    }
                }
                if (ks == k) {
                    kase = 3;
                } else if (ks == p - 1) {
                    kase = 1;
                } else {
                    kase = 2;
                    k = ks;
                }
            }
            k++;
            // Perform the task indicated by kase.
            switch (kase) {
            // Deflate negligible s(p).
                case 1: {
                    double f = e[p - 2];
                    e[p - 2] = 0;
                    for (int j = p - 2; j >= k; j--) {
                        double t = MathLib.hypot(this.singularValues[j], f);
                        final double cs = this.singularValues[j] / t;
                        final double sn = f / t;
                        this.singularValues[j] = t;
                        if (j != k) {
                            f = -sn * e[j - 1];
                            e[j - 1] = cs * e[j - 1];
                        }

                        for (int i = 0; i < this.n; i++) {
                            t = cs * v[i][j] + sn * v[i][p - 1];
                            v[i][p - 1] = -sn * v[i][j] + cs * v[i][p - 1];
                            v[i][j] = t;
                        }
                    }
                }
                    break;
                // Split at negligible s(k).
                case 2: {
                    double f = e[k - 1];
                    e[k - 1] = 0;
                    for (int j = k; j < p; j++) {
                        double t = MathLib.hypot(this.singularValues[j], f);
                        final double cs = this.singularValues[j] / t;
                        final double sn = f / t;
                        this.singularValues[j] = t;
                        f = -sn * e[j];
                        e[j] = cs * e[j];

                        for (int i = 0; i < this.m; i++) {
                            t = cs * u[i][j] + sn * u[i][k - 1];
                            u[i][k - 1] = -sn * u[i][j] + cs * u[i][k - 1];
                            u[i][j] = t;
                        }
                    }
                }
                    break;
                // Perform one qr step.
                case 3: {
                    // Calculate the shift.
                    final double maxPm1Pm2 = MathLib.max(MathLib.abs(this.singularValues[p - 1]),
                        MathLib.abs(this.singularValues[p - 2]));
                    final double scale = MathLib.max(MathLib.max(MathLib.max(maxPm1Pm2,
                        MathLib.abs(e[p - 2])),
                        MathLib.abs(this.singularValues[k])),
                        MathLib.abs(e[k]));
                    final double sp = this.singularValues[p - 1] / scale;
                    final double spm1 = this.singularValues[p - 2] / scale;
                    final double epm1 = e[p - 2] / scale;
                    final double sk = this.singularValues[k] / scale;
                    final double ek = e[k] / scale;
                    final double b = ((spm1 + sp) * (spm1 - sp) + epm1 * epm1) / 2.0;
                    final double c = (sp * epm1) * (sp * epm1);
                    double shift = 0;
                    if (b != 0 ||
                            c != 0) {
                        shift = MathLib.sqrt(b * b + c);
                        if (b < 0) {
                            shift = -shift;
                        }
                        shift = c / (b + shift);
                    }
                    double f = (sk + sp) * (sk - sp) + shift;
                    double g = sk * ek;
                    // Chase zeros.
                    for (int j = k; j < p - 1; j++) {
                        double t = MathLib.hypot(f, g);
                        double cs = f / t;
                        double sn = g / t;
                        if (j != k) {
                            e[j - 1] = t;
                        }
                        f = cs * this.singularValues[j] + sn * e[j];
                        e[j] = cs * e[j] - sn * this.singularValues[j];
                        g = sn * this.singularValues[j + 1];
                        this.singularValues[j + 1] = cs * this.singularValues[j + 1];

                        for (int i = 0; i < this.n; i++) {
                            t = cs * v[i][j] + sn * v[i][j + 1];
                            v[i][j + 1] = -sn * v[i][j] + cs * v[i][j + 1];
                            v[i][j] = t;
                        }
                        t = MathLib.hypot(f, g);
                        cs = f / t;
                        sn = g / t;
                        this.singularValues[j] = t;
                        f = cs * e[j] + sn * this.singularValues[j + 1];
                        this.singularValues[j + 1] = -sn * e[j] + cs * this.singularValues[j + 1];
                        g = sn * e[j + 1];
                        e[j + 1] = cs * e[j + 1];
                        if (j < this.m - 1) {
                            for (int i = 0; i < this.m; i++) {
                                t = cs * u[i][j] + sn * u[i][j + 1];
                                u[i][j + 1] = -sn * u[i][j] + cs * u[i][j + 1];
                                u[i][j] = t;
                            }
                        }
                    }
                    e[p - 2] = f;
                }
                    break;
                // Convergence.
                default:
                    // Make the singular values positive.
                    if (this.singularValues[k] <= 0) {
                        this.singularValues[k] = this.singularValues[k] < 0 ? -this.singularValues[k] : 0;

                        for (int i = 0; i <= pp; i++) {
                            v[i][k] = -v[i][k];
                        }
                    }
                    // Order the singular values.
                    while (k < pp) {
                        if (this.singularValues[k] >= this.singularValues[k + 1]) {
                            break;
                        }
                        double t = this.singularValues[k];
                        this.singularValues[k] = this.singularValues[k + 1];
                        this.singularValues[k + 1] = t;
                        if (k < this.n - 1) {
                            for (int i = 0; i < this.n; i++) {
                                t = v[i][k + 1];
                                v[i][k + 1] = v[i][k];
                                v[i][k] = t;
                            }
                        }
                        if (k < this.m - 1) {
                            for (int i = 0; i < this.m; i++) {
                                t = u[i][k + 1];
                                u[i][k + 1] = u[i][k];
                                u[i][k] = t;
                            }
                        }
                        k++;
                    }
                    p--;
                    break;
            }
        }

        // Set the small value tolerance used to calculate rank and pseudo-inverse
        this.tol = MathLib.max(this.m * this.singularValues[0] * EPS,
            MathLib.sqrt(Precision.SAFE_MIN));

        if (this.transposed) {
            this.cachedU = MatrixUtils.createRealMatrix(v, false);
            this.cachedV = MatrixUtils.createRealMatrix(u, false);
        } else {
            this.cachedU = MatrixUtils.createRealMatrix(u, false);
            this.cachedV = MatrixUtils.createRealMatrix(v, false);
        }
    }

    // CHECKSTYLE: resume AvoidNestedBlocks check

    /**
     * Returns the matrix U of the decomposition.
     * <p>
     * U is an orthogonal matrix, i.e. its transpose is also its inverse.
     * </p>
     * 
     * @return the U matrix
     * @see #getUT()
     */
    public RealMatrix getU() {
        return this.cachedU;

    }

    /**
     * Returns the transpose of the matrix U of the decomposition.
     * <p>
     * U is an orthogonal matrix, i.e. its transpose is also its inverse.
     * </p>
     * 
     * @return the transpose of the U matrix
     * @see #getU()
     */
    public RealMatrix getUT() {
        /** Cached value of transposed U matrix. */
        return this.cachedU.transpose();
    }

    /**
     * Returns the diagonal matrix &Sigma; of the decomposition.
     * <p>
     * &Sigma; is a diagonal matrix. The singular values are provided in non-increasing order, for compatibility with
     * Jama.
     * </p>
     * 
     * @return the &Sigma; matrix
     */
    public RealMatrix getS() {
        // cache the matrix for subsequent calls
        return MatrixUtils.createRealDiagonalMatrix(this.singularValues);
    }

    /**
     * Returns the diagonal elements of the matrix &Sigma; of the decomposition.
     * <p>
     * The singular values are provided in non-increasing order, for compatibility with Jama.
     * </p>
     * 
     * @return the diagonal elements of the &Sigma; matrix
     */
    public double[] getSingularValues() {
        return this.singularValues.clone();
    }

    /**
     * Returns the matrix V of the decomposition.
     * <p>
     * V is an orthogonal matrix, i.e. its transpose is also its inverse.
     * </p>
     * 
     * @return the V matrix (or null if decomposed matrix is singular)
     * @see #getVT()
     */
    public RealMatrix getV() {
        return this.cachedV;
    }

    /**
     * Returns the transpose of the matrix V of the decomposition.
     * <p>
     * V is an orthogonal matrix, i.e. its transpose is also its inverse.
     * </p>
     * 
     * @return the V matrix
     * @see #getV()
     */
    public RealMatrix getVT() {
        return cachedV.transpose();
    }

    /**
     * Returns the n &times; n covariance matrix.
     * <p>
     * The covariance matrix is V &times; J &times; V<sup>T</sup> where J is the diagonal matrix of the inverse of the
     * squares of the singular values.
     * </p>
     * 
     * @param minSingularValue
     *        value below which singular values are ignored
     *        (a 0 or negative value implies all singular value will be used)
     * @return covariance matrix
     * @exception IllegalArgumentException
     *            if minSingularValue is larger than
     *            the largest singular value, meaning all singular values are ignored
     */
    public RealMatrix getCovariance(final double minSingularValue) {
        // get the number of singular values to consider
        final int p = this.singularValues.length;
        int dimension = 0;
        while (dimension < p &&
                this.singularValues[dimension] >= minSingularValue) {
            ++dimension;
        }

        if (dimension == 0) {
            throw new NumberIsTooLargeException(PatriusMessages.TOO_LARGE_CUTOFF_SINGULAR_VALUE,
                minSingularValue, this.singularValues[0], true);
        }

        final double[][] data = new double[dimension][p];
        this.getVT().walkInOptimizedOrder(new DefaultRealMatrixPreservingVisitor(){
            /** {@inheritDoc} */
            @Override
            public void visit(final int row, final int column,
                    final double value) {
                data[row][column] = value / SingularValueDecomposition.this.singularValues[row];
            }
        }, 0, dimension - 1, 0, p - 1);

        final RealMatrix jv = new Array2DRowRealMatrix(data, false);
        return jv.transpose().multiply(jv);
    }

    /**
     * Returns the L<sub>2</sub> norm of the matrix.
     * <p>
     * The L<sub>2</sub> norm is max(|A &times; u|<sub>2</sub> / |u|<sub>2</sub>), where |.|<sub>2</sub> denotes the
     * vectorial 2-norm (i.e. the traditional euclidian norm).
     * </p>
     * 
     * @return norm
     */
    public double getNorm() {
        return this.singularValues[0];
    }

    /**
     * Return the condition number of the matrix.
     * 
     * @return condition number of the matrix
     */
    public double getConditionNumber() {
        return this.singularValues[0] / this.singularValues[this.n - 1];
    }

    /**
     * Computes the inverse of the condition number.
     * In cases of rank deficiency, the {@link #getConditionNumber() condition
     * number} will become undefined.
     * 
     * @return the inverse of the condition number.
     */
    public double getInverseConditionNumber() {
        return this.singularValues[this.n - 1] / this.singularValues[0];
    }

    /**
     * Return the effective numerical matrix rank.
     * <p>
     * The effective numerical rank is the number of non-negligible singular values. The threshold used to identify
     * non-negligible terms is max(m,n) &times; ulp(s<sub>1</sub>) where ulp(s<sub>1</sub>) is the least significant bit
     * of the largest singular value.
     * </p>
     * 
     * @return effective numerical matrix rank
     */
    public int getRank() {
        int r = 0;
        for (final double singularValue : this.singularValues) {
            if (singularValue > this.tol) {
                r++;
            }
        }
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public DecompositionSolver getSolver() {
        if (this.singularValues != null && this.cachedU != null && this.cachedV != null) {
            return new Solver(this.singularValues, this.getUT(), this.getV(), this.getRank() == this.m, this.tol);
        } else {
            return null;
        }
    }
    
    /**
     * Builder for decomposition.
     * 
     * @return decomposition
     */
    public static Function<RealMatrix, Decomposition> decompositionBuilder () {
        return (realMatrix) -> new SingularValueDecomposition(realMatrix);
    }

    /** Specialized solver. */
    private static final class Solver implements DecompositionSolver {
        /** Pseudo-inverse of the initial matrix. */
        private final RealMatrix pseudoInverse;
        /** Singularity indicator. */
        private final boolean nonSingular;

        /**
         * Build a solver from decomposed matrix.
         * 
         * @param singularValues
         *        Singular values.
         * @param uT
         *        U<sup>T</sup> matrix of the decomposition.
         * @param v
         *        V matrix of the decomposition.
         * @param nonSingularIn
         *        Singularity indicator.
         * @param tolIn
         *        tolerance for singular values
         */
        private Solver(final double[] singularValues, final RealMatrix uT,
                       final RealMatrix v, final boolean nonSingularIn, final double tolIn) {
            final double[][] suT = uT.getData(false);
            for (int i = 0; i < singularValues.length; ++i) {
                final double a;
                if (singularValues[i] > tolIn) {
                    a = 1 / singularValues[i];
                } else {
                    a = 0;
                }
                final double[] suTi = suT[i];
                for (int j = 0; j < suTi.length; ++j) {
                    suTi[j] *= a;
                }
            }
            this.pseudoInverse = v.multiply(new Array2DRowRealMatrix(suT, false));
            this.nonSingular = nonSingularIn;
        }

        /**
         * Solve the linear equation A &times; X = B in least square sense.
         * <p>
         * The m&times;n matrix A may not be square, the solution X is such that ||A &times; X - B|| is minimal.
         * </p>
         * 
         * @param b
         *        Right-hand side of the equation A &times; X = B
         * @return a vector X that minimizes the two norm of A &times; X - B
         * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
         *         if the matrices dimensions do not match.
         */
        @Override
        public RealVector solve(final RealVector b) {
            return this.pseudoInverse.operate(b);
        }

        /**
         * Solve the linear equation A &times; X = B in least square sense.
         * <p>
         * The m&times;n matrix A may not be square, the solution X is such that ||A &times; X - B|| is minimal.
         * </p>
         * 
         * @param b
         *        Right-hand side of the equation A &times; X = B
         * @return a matrix X that minimizes the two norm of A &times; X - B
         * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
         *         if the matrices dimensions do not match.
         */
        @Override
        public RealMatrix solve(final RealMatrix b) {
            return this.pseudoInverse.multiply(b);
        }

        /**
         * Check if the decomposed matrix is non-singular.
         * 
         * @return {@code true} if the decomposed matrix is non-singular.
         */
        @Override
        public boolean isNonSingular() {
            return this.nonSingular;
        }

        /**
         * Get the pseudo-inverse of the decomposed matrix.
         * 
         * @return the inverse matrix.
         */
        @Override
        public RealMatrix getInverse() {
            return this.pseudoInverse;
        }

    }
}
