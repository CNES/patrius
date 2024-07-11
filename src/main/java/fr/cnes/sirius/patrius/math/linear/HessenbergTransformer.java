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
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Class transforming a general real matrix to Hessenberg form.
 * <p>
 * A m &times; m matrix A can be written as the product of three matrices: A = P &times; H &times; P<sup>T</sup> with P
 * an orthogonal matrix and H a Hessenberg matrix. Both P and H are m &times; m matrices.
 * </p>
 * <p>
 * Transformation to Hessenberg form is often not a goal by itself, but it is an intermediate step in more general
 * decomposition algorithms like {@link EigenDecomposition eigen decomposition}. This class is therefore intended for
 * internal use by the library and is not public. As a consequence of this explicitly limited scope, many methods
 * directly returns references to internal arrays, not copies.
 * </p>
 * <p>
 * This class is based on the method orthes in class EigenvalueDecomposition from the <a
 * href="http://math.nist.gov/javanumerics/jama/">JAMA</a> library.
 * </p>
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @see <a href="http://mathworld.wolfram.com/HessenbergDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/Householder_transformation">Householder Transformations</a>
 * @version $Id: HessenbergTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
@SuppressWarnings("PMD.NullAssignment")
class HessenbergTransformer {
    /** Householder vectors. */
    private final double[][] householderVectors;
    /** Temporary storage vector. */
    private final double[] ort;
    /** Cached value of P. */
    private RealMatrix cachedP;
    /** Cached value of Pt. */
    private RealMatrix cachedPt;
    /** Cached value of H. */
    private RealMatrix cachedH;

    /**
     * Build the transformation to Hessenberg form of a general matrix.
     * 
     * @param matrix
     *        matrix to transform
     * @throws NonSquareMatrixException
     *         if the matrix is not square
     */
    public HessenbergTransformer(final RealMatrix matrix) {
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(),
                matrix.getColumnDimension());
        }

        final int m = matrix.getRowDimension();
        this.householderVectors = matrix.getData();
        this.ort = new double[m];
        this.cachedP = null;
        this.cachedPt = null;
        this.cachedH = null;

        // transform matrix
        this.transform();
    }

    /**
     * Returns the matrix P of the transform.
     * <p>
     * P is an orthogonal matrix, i.e. its inverse is also its transpose.
     * </p>
     * 
     * @return the P matrix
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public RealMatrix getP() {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (this.cachedP == null) {
            // Compute P
            final int n = this.householderVectors.length;
            final int high = n - 1;
            final double[][] pa = new double[n][n];

            // Initialize diagonal 2 dimension array
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    pa[i][j] = (i == j) ? 1 : 0;
                }
            }

            // Loop on all rows of pa
            for (int m = high - 1; m >= 1; m--) {
                if (this.householderVectors[m][m - 1] != 0.0) {
                    for (int i = m + 1; i <= high; i++) {
                        this.ort[i] = this.householderVectors[i][m - 1];
                    }

                    // Loop on all elements of the current row to update the value in pa
                    for (int j = m; j <= high; j++) {
                        double g = 0.0;

                        for (int i = m; i <= high; i++) {
                            g += this.ort[i] * pa[i][j];
                        }

                        // Double division avoids possible underflow
                        g = (g / this.ort[m]) / this.householderVectors[m][m - 1];

                        for (int i = m; i <= high; i++) {
                            pa[i][j] += g * this.ort[i];
                        }
                    }
                }
            }

            // Convert to matrix
            this.cachedP = MatrixUtils.createRealMatrix(pa, false);
        }
        // Return result
        //
        return this.cachedP;
    }

    /**
     * Returns the transpose of the matrix P of the transform.
     * <p>
     * P is an orthogonal matrix, i.e. its inverse is also its transpose.
     * </p>
     * 
     * @return the transpose of the P matrix
     */
    public RealMatrix getPT() {
        if (this.cachedPt == null) {
            this.cachedPt = this.getP().transpose();
        }

        // return the cached matrix
        return this.cachedPt;
    }

    /**
     * Returns the Hessenberg matrix H of the transform.
     * 
     * @return the H matrix
     */
    public RealMatrix getH() {
        if (this.cachedH == null) {
            final int m = this.householderVectors.length;
            final double[][] h = new double[m][m];
            for (int i = 0; i < m; ++i) {
                if (i > 0) {
                    // copy the entry of the lower sub-diagonal
                    h[i][i - 1] = this.householderVectors[i][i - 1];
                }

                // copy upper triangular part of the matrix
                for (int j = i; j < m; ++j) {
                    h[i][j] = this.householderVectors[i][j];
                }
            }
            this.cachedH = MatrixUtils.createRealMatrix(h, false);
        }

        // return the cached matrix
        return this.cachedH;
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
     * Transform original matrix to Hessenberg form.
     * <p>
     * Transformation is done using Householder transforms.
     * </p>
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void transform() {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        final int n = this.householderVectors.length;
        final int high = n - 1;

        for (int m = 1; m <= high - 1; m++) {
            // Scale column.
            double scale = 0;
            for (int i = m; i <= high; i++) {
                scale += MathLib.abs(this.householderVectors[i][m - 1]);
            }

            // If the scale is not zero
            if (!Precision.equals(scale, 0)) {
                // Compute Householder transformation.
                double h = 0;
                for (int i = high; i >= m; i--) {
                    this.ort[i] = this.householderVectors[i][m - 1] / scale;
                    h += this.ort[i] * this.ort[i];
                }
                final double g = (this.ort[m] > 0) ? -MathLib.sqrt(h) : MathLib.sqrt(h);

                h -= this.ort[m] * g;
                this.ort[m] -= g;

                // Apply Householder similarity transformation
                // H = (I - u*u' / h) * H * (I - u*u' / h)

                // Loop on all columns of householderVectors
                for (int j = m; j < n; j++) {
                    double f = 0;
                    for (int i = high; i >= m; i--) {
                        f += this.ort[i] * this.householderVectors[i][j];
                    }
                    f /= h;
                    // Loop on all element of current column to update householderVectors
                    for (int i = m; i <= high; i++) {
                        this.householderVectors[i][j] -= f * this.ort[i];
                    }
                }

                // Loop on all rows of householderVectors
                for (int i = 0; i <= high; i++) {
                    double f = 0;
                    for (int j = high; j >= m; j--) {
                        f += this.ort[j] * this.householderVectors[i][j];
                    }
                    f /= h;
                    // Loop on all elements of current row to update householderVectors²
                    for (int j = m; j <= high; j++) {
                        this.householderVectors[i][j] -= f * this.ort[j];
                    }
                }

                // Finalization
                this.ort[m] = scale * this.ort[m];
                this.householderVectors[m][m - 1] = scale * g;
            }
        }
    }
}
