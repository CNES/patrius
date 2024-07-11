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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Class transforming any matrix to bi-diagonal shape.
 * <p>
 * Any m &times; n matrix A can be written as the product of three matrices: A = U &times; B &times; V<sup>T</sup> with
 * U an m &times; m orthogonal matrix, B an m &times; n bi-diagonal matrix (lower diagonal if m &lt; n, upper diagonal
 * otherwise), and V an n &times; n orthogonal matrix.
 * </p>
 * <p>
 * Transformation to bi-diagonal shape is often not a goal by itself, but it is an intermediate step in more general
 * decomposition algorithms like {@link SingularValueDecomposition Singular Value Decomposition}. This class is
 * therefore intended for internal use by the library and is not public. As a consequence of this explicitly limited
 * scope, many methods directly returns references to internal arrays, not copies.
 * </p>
 * 
 * @version $Id: BiDiagonalTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.NullAssignment")
class BiDiagonalTransformer {

    /** Householder vectors. */
    private final double[][] householderVectors;

    /** Main diagonal. */
    private final double[] main;

    /** Secondary diagonal. */
    private final double[] secondary;

    /** Cached value of U. */
    private RealMatrix cachedU;

    /** Cached value of B. */
    private RealMatrix cachedB;

    /** Cached value of V. */
    private RealMatrix cachedV;

    /**
     * Build the transformation to bi-diagonal shape of a matrix.
     * 
     * @param matrix
     *        the matrix to transform.
     */
    public BiDiagonalTransformer(final RealMatrix matrix) {

        final int m = matrix.getRowDimension();
        final int n = matrix.getColumnDimension();
        final int p = MathLib.min(m, n);
        this.householderVectors = matrix.getData();
        this.main = new double[p];
        this.secondary = new double[p - 1];
        this.cachedU = null;
        this.cachedB = null;
        this.cachedV = null;

        // transform matrix
        if (m >= n) {
            this.transformToUpperBiDiagonal();
        } else {
            this.transformToLowerBiDiagonal();
        }

    }

    /**
     * Returns the matrix U of the transform.
     * <p>
     * U is an orthogonal matrix, i.e. its transpose is also its inverse.
     * </p>
     * 
     * @return the U matrix
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public RealMatrix getU() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (this.cachedU == null) {

            final int m = this.householderVectors.length;
            final int n = this.householderVectors[0].length;
            final int p = this.main.length;
            final int diagOffset = (m >= n) ? 0 : 1;
            final double[] diagonal = (m >= n) ? this.main : this.secondary;
            final double[][] ua = new double[m][m];

            // fill up the part of the matrix not affected by Householder transforms
            for (int k = m - 1; k >= p; --k) {
                ua[k][k] = 1;
            }

            // build up first part of the matrix by applying Householder transforms
            for (int k = p - 1; k >= diagOffset; --k) {
                final double[] hK = this.householderVectors[k];
                ua[k][k] = 1;
                if (hK[k - diagOffset] != 0.0) {
                    for (int j = k; j < m; ++j) {
                        double alpha = 0;
                        for (int i = k; i < m; ++i) {
                            alpha -= ua[i][j] * this.householderVectors[i][k - diagOffset];
                        }
                        alpha /= diagonal[k - diagOffset] * hK[k - diagOffset];

                        for (int i = k; i < m; ++i) {
                            ua[i][j] += -alpha * this.householderVectors[i][k - diagOffset];
                        }
                    }
                }
            }
            if (diagOffset > 0) {
                ua[0][0] = 1;
            }
            this.cachedU = MatrixUtils.createRealMatrix(ua, false);
        }

        // return the cached matrix
        return this.cachedU;

    }

    /**
     * Returns the bi-diagonal matrix B of the transform.
     * 
     * @return the B matrix
     */
    public RealMatrix getB() {

        if (this.cachedB == null) {

            final int m = this.householderVectors.length;
            final int n = this.householderVectors[0].length;
            final double[][] ba = new double[m][n];
            for (int i = 0; i < this.main.length; ++i) {
                ba[i][i] = this.main[i];
                if (m < n) {
                    if (i > 0) {
                        ba[i][i - 1] = this.secondary[i - 1];
                    }
                } else {
                    if (i < this.main.length - 1) {
                        ba[i][i + 1] = this.secondary[i];
                    }
                }
            }
            this.cachedB = MatrixUtils.createRealMatrix(ba, false);
        }

        // return the cached matrix
        return this.cachedB;

    }

    /**
     * Returns the matrix V of the transform.
     * <p>
     * V is an orthogonal matrix, i.e. its transpose is also its inverse.
     * </p>
     * 
     * @return the V matrix
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public RealMatrix getV() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (this.cachedV == null) {

            final int m = this.householderVectors.length;
            final int n = this.householderVectors[0].length;
            final int p = this.main.length;
            final int diagOffset = (m >= n) ? 1 : 0;
            final double[] diagonal = (m >= n) ? this.secondary : this.main;
            final double[][] va = new double[n][n];

            // fill up the part of the matrix not affected by Householder transforms
            for (int k = n - 1; k >= p; --k) {
                va[k][k] = 1;
            }

            // build up first part of the matrix by applying Householder transforms
            for (int k = p - 1; k >= diagOffset; --k) {
                final double[] hK = this.householderVectors[k - diagOffset];
                va[k][k] = 1;
                if (hK[k] != 0.0) {
                    for (int j = k; j < n; ++j) {
                        double beta = 0;
                        for (int i = k; i < n; ++i) {
                            beta -= va[i][j] * hK[i];
                        }
                        beta /= diagonal[k - diagOffset] * hK[k];

                        for (int i = k; i < n; ++i) {
                            va[i][j] += -beta * hK[i];
                        }
                    }
                }
            }
            if (diagOffset > 0) {
                va[0][0] = 1;
            }
            this.cachedV = MatrixUtils.createRealMatrix(va, false);
        }

        // return the cached matrix
        return this.cachedV;

    }

    /**
     * Get the Householder vectors of the transform.
     * <p>
     * Note that since this class is only intended for internal use, it returns directly a reference to its internal
     * arrays, not a copy.
     * </p>
     * 
     * @return the main diagonal elements of the B matrix
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getHouseholderVectorsRef() {
        return this.householderVectors;
    }

    /**
     * Get the main diagonal elements of the matrix B of the transform.
     * <p>
     * Note that since this class is only intended for internal use, it returns directly a reference to its internal
     * arrays, not a copy.
     * </p>
     * 
     * @return the main diagonal elements of the B matrix
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getMainDiagonalRef() {
        return this.main;
    }

    /**
     * Get the secondary diagonal elements of the matrix B of the transform.
     * <p>
     * Note that since this class is only intended for internal use, it returns directly a reference to its internal
     * arrays, not a copy.
     * </p>
     * 
     * @return the secondary diagonal elements of the B matrix
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getSecondaryDiagonalRef() {
        return this.secondary;
    }

    /**
     * Check if the matrix is transformed to upper bi-diagonal.
     * 
     * @return true if the matrix is transformed to upper bi-diagonal
     */
    public boolean isUpperBiDiagonal() {
        return this.householderVectors.length >= this.householderVectors[0].length;
    }

    /**
     * Transform original matrix to upper bi-diagonal form.
     * <p>
     * Transformation is done using alternate Householder transforms on columns and rows.
     * </p>
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void transformToUpperBiDiagonal() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final int m = this.householderVectors.length;
        final int n = this.householderVectors[0].length;
        for (int k = 0; k < n; k++) {

            // zero-out a column
            double xNormSqr = 0;
            for (int i = k; i < m; ++i) {
                final double c = this.householderVectors[i][k];
                xNormSqr += c * c;
            }
            final double[] hK = this.householderVectors[k];
            final double a = (hK[k] > 0) ? -MathLib.sqrt(xNormSqr) : MathLib.sqrt(xNormSqr);
            this.main[k] = a;
            if (a != 0.0) {
                hK[k] -= a;
                for (int j = k + 1; j < n; ++j) {
                    double alpha = 0;
                    for (int i = k; i < m; ++i) {
                        final double[] hI = this.householderVectors[i];
                        alpha -= hI[j] * hI[k];
                    }
                    alpha /= a * this.householderVectors[k][k];
                    for (int i = k; i < m; ++i) {
                        final double[] hI = this.householderVectors[i];
                        hI[j] -= alpha * hI[k];
                    }
                }
            }

            if (k < n - 1) {
                // zero-out a row
                xNormSqr = 0;
                for (int j = k + 1; j < n; ++j) {
                    final double c = hK[j];
                    xNormSqr += c * c;
                }
                final double b = (hK[k + 1] > 0) ? -MathLib.sqrt(xNormSqr) : MathLib.sqrt(xNormSqr);
                this.secondary[k] = b;
                if (b != 0.0) {
                    hK[k + 1] -= b;
                    for (int i = k + 1; i < m; ++i) {
                        final double[] hI = this.householderVectors[i];
                        double beta = 0;
                        for (int j = k + 1; j < n; ++j) {
                            beta -= hI[j] * hK[j];
                        }
                        beta /= b * hK[k + 1];
                        for (int j = k + 1; j < n; ++j) {
                            hI[j] -= beta * hK[j];
                        }
                    }
                }
            }

        }
    }

    /**
     * Transform original matrix to lower bi-diagonal form.
     * <p>
     * Transformation is done using alternate Householder transforms on rows and columns.
     * </p>
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void transformToLowerBiDiagonal() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        final int m = this.householderVectors.length;
        final int n = this.householderVectors[0].length;
        for (int k = 0; k < m; k++) {

            // zero-out a row
            final double[] hK = this.householderVectors[k];
            double xNormSqr = 0;
            for (int j = k; j < n; ++j) {
                final double c = hK[j];
                xNormSqr += c * c;
            }
            final double a = (hK[k] > 0) ? -MathLib.sqrt(xNormSqr) : MathLib.sqrt(xNormSqr);
            this.main[k] = a;
            if (a != 0.0) {
                hK[k] -= a;
                for (int i = k + 1; i < m; ++i) {
                    final double[] hI = this.householderVectors[i];
                    double alpha = 0;
                    for (int j = k; j < n; ++j) {
                        alpha -= hI[j] * hK[j];
                    }
                    alpha /= a * this.householderVectors[k][k];
                    for (int j = k; j < n; ++j) {
                        hI[j] -= alpha * hK[j];
                    }
                }
            }

            if (k < m - 1) {
                // zero-out a column
                final double[] hKp1 = this.householderVectors[k + 1];
                xNormSqr = 0;
                for (int i = k + 1; i < m; ++i) {
                    final double c = this.householderVectors[i][k];
                    xNormSqr += c * c;
                }
                final double b = (hKp1[k] > 0) ? -MathLib.sqrt(xNormSqr) : MathLib.sqrt(xNormSqr);
                this.secondary[k] = b;
                if (b != 0.0) {
                    hKp1[k] -= b;
                    for (int j = k + 1; j < n; ++j) {
                        double beta = 0;
                        for (int i = k + 1; i < m; ++i) {
                            final double[] hI = this.householderVectors[i];
                            beta -= hI[j] * hI[k];
                        }
                        beta /= b * hKp1[k];
                        for (int i = k + 1; i < m; ++i) {
                            final double[] hI = this.householderVectors[i];
                            hI[j] -= beta * hI[k];
                        }
                    }
                }
            }

        }
    }

    // CHECKSTYLE: resume CommentRatio check
}
