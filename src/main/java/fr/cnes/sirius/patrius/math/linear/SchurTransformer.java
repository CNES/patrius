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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code

/**
 * Class transforming a general real matrix to Schur form.
 * <p>
 * A m &times; m matrix A can be written as the product of three matrices: A = P &times; T &times; P<sup>T</sup> with P
 * an orthogonal matrix and T an quasi-triangular matrix. Both P and T are m &times; m matrices.
 * </p>
 * <p>
 * Transformation to Schur form is often not a goal by itself, but it is an intermediate step in more general
 * decomposition algorithms like {@link EigenDecomposition eigen decomposition}. This class is therefore intended for
 * internal use by the library and is not public. As a consequence of this explicitly limited scope, many methods
 * directly returns references to internal arrays, not copies.
 * </p>
 * <p>
 * This class is based on the method hqr2 in class EigenvalueDecomposition from the <a
 * href="http://math.nist.gov/javanumerics/jama/">JAMA</a> library.
 * </p>
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @see <a href="http://mathworld.wolfram.com/SchurDecomposition.html">Schur Decomposition - MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/Schur_decomposition">Schur Decomposition - Wikipedia</a>
 * @see <a href="http://en.wikipedia.org/wiki/Householder_transformation">Householder Transformations</a>
 * @version $Id: SchurTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
@SuppressWarnings("PMD.NullAssignment")
class SchurTransformer {
    /** Maximum allowed iterations for convergence of the transformation. */
    private static final int MAX_ITERATIONS = 100;

    /** P matrix. */
    private final double[][] matrixP;
    /** T matrix. */
    private final double[][] matrixT;
    /** Cached value of P. */
    private RealMatrix cachedP;
    /** Cached value of T. */
    private RealMatrix cachedT;
    /** Cached value of PT. */
    private RealMatrix cachedPt;

    /** Epsilon criteria taken from JAMA code (originally was 2^-52). */
    private final double epsilon = Precision.EPSILON;

    /**
     * Build the transformation to Schur form of a general real matrix.
     * 
     * @param matrix
     *        matrix to transform
     * @throws NonSquareMatrixException
     *         if the matrix is not square
     */
    public SchurTransformer(final RealMatrix matrix) {
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(),
                matrix.getColumnDimension());
        }

        final HessenbergTransformer transformer = new HessenbergTransformer(matrix);
        this.matrixT = transformer.getH().getData();
        this.matrixP = transformer.getP().getData();
        this.cachedT = null;
        this.cachedP = null;
        this.cachedPt = null;

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
    public RealMatrix getP() {
        if (this.cachedP == null) {
            this.cachedP = MatrixUtils.createRealMatrix(this.matrixP);
        }
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
     * Returns the quasi-triangular Schur matrix T of the transform.
     * 
     * @return the T matrix
     */
    public RealMatrix getT() {
        if (this.cachedT == null) {
            this.cachedT = MatrixUtils.createRealMatrix(this.matrixT);
        }

        // return the cached matrix
        return this.cachedT;
    }

    /**
     * Transform original matrix to Schur form.
     * 
     * @throws MaxCountExceededException
     *         if the transformation does not converge
     */
    private void transform() {
        final int n = this.matrixT.length;

        // compute matrix norm
        final double norm = this.getNorm();

        // shift information
        final ShiftInfo shift = new ShiftInfo();

        // Outer loop over eigenvalue index
        int iteration = 0;
        int iu = n - 1;
        while (iu >= 0) {

            // Look for single small sub-diagonal element
            final int il = this.findSmallSubDiagonalElement(iu, norm);

            // Check for convergence
            if (il == iu) {
                // One root found
                this.matrixT[iu][iu] += shift.exShift;
                iu--;
                iteration = 0;
            } else if (il == iu - 1) {
                // Two roots found
                double p = (this.matrixT[iu - 1][iu - 1] - this.matrixT[iu][iu]) / 2.0;
                double q = p * p + this.matrixT[iu][iu - 1] * this.matrixT[iu - 1][iu];
                this.matrixT[iu][iu] += shift.exShift;
                this.matrixT[iu - 1][iu - 1] += shift.exShift;

                if (q >= 0) {
                    double z = MathLib.sqrt(MathLib.abs(q));
                    if (p >= 0) {
                        z = p + z;
                    } else {
                        z = p - z;
                    }
                    final double x = this.matrixT[iu][iu - 1];
                    final double s = MathLib.abs(x) + MathLib.abs(z);
                    p = x / s;
                    q = z / s;
                    final double r = MathLib.sqrt(p * p + q * q);
                    p = p / r;
                    q = q / r;

                    // Row modification
                    for (int j = iu - 1; j < n; j++) {
                        z = this.matrixT[iu - 1][j];
                        this.matrixT[iu - 1][j] = q * z + p * this.matrixT[iu][j];
                        this.matrixT[iu][j] = q * this.matrixT[iu][j] - p * z;
                    }

                    // Column modification
                    for (int i = 0; i <= iu; i++) {
                        z = this.matrixT[i][iu - 1];
                        this.matrixT[i][iu - 1] = q * z + p * this.matrixT[i][iu];
                        this.matrixT[i][iu] = q * this.matrixT[i][iu] - p * z;
                    }

                    // Accumulate transformations
                    for (int i = 0; i <= n - 1; i++) {
                        z = this.matrixP[i][iu - 1];
                        this.matrixP[i][iu - 1] = q * z + p * this.matrixP[i][iu];
                        this.matrixP[i][iu] = q * this.matrixP[i][iu] - p * z;
                    }
                }
                iu -= 2;
                iteration = 0;
            } else {
                // No convergence yet
                this.computeShift(il, iu, iteration, shift);

                // stop transformation after too many iterations
                iteration++;
                if (iteration > MAX_ITERATIONS) {
                    throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED,
                        MAX_ITERATIONS);
                }

                // the initial houseHolder vector for the QR step
                final double[] hVec = new double[3];

                final int im = this.initQRStep(il, iu, shift, hVec);
                this.performDoubleQRStep(il, im, iu, shift, hVec);
            }
        }
    }

    /**
     * Computes the L1 norm of the (quasi-)triangular matrix T.
     * 
     * @return the L1 norm of matrix T
     */
    private double getNorm() {
        double norm = 0.0;
        for (int i = 0; i < this.matrixT.length; i++) {
            // as matrix T is (quasi-)triangular, also take the sub-diagonal element into account
            for (int j = MathLib.max(i - 1, 0); j < this.matrixT.length; j++) {
                norm += MathLib.abs(this.matrixT[i][j]);
            }
        }
        return norm;
    }

    /**
     * Find the first small sub-diagonal element and returns its index.
     * 
     * @param startIdx
     *        the starting index for the search
     * @param norm
     *        the L1 norm of the matrix
     * @return the index of the first small sub-diagonal element
     */
    private int findSmallSubDiagonalElement(final int startIdx, final double norm) {
        int l = startIdx;
        while (l > 0) {
            double s = MathLib.abs(this.matrixT[l - 1][l - 1]) + MathLib.abs(this.matrixT[l][l]);
            if (s == 0.0) {
                s = norm;
            }
            if (MathLib.abs(this.matrixT[l][l - 1]) < this.epsilon * s) {
                break;
            }
            l--;
        }
        return l;
    }

    /**
     * Compute the shift for the current iteration.
     * 
     * @param l
     *        the index of the small sub-diagonal element
     * @param idx
     *        the current eigenvalue index
     * @param iteration
     *        the current iteration
     * @param shift
     *        holder for shift information
     */
    private void computeShift(final int l, final int idx, final int iteration, final ShiftInfo shift) {
        // Form shift
        shift.x = this.matrixT[idx][idx];
        shift.y = 0.0;
        shift.w = 0.0;
        if (l < idx) {
            shift.y = this.matrixT[idx - 1][idx - 1];
            shift.w = this.matrixT[idx][idx - 1] * this.matrixT[idx - 1][idx];
        }

        // Wilkinson's original ad hoc shift
        if (iteration == 10) {
            shift.exShift += shift.x;
            for (int i = 0; i <= idx; i++) {
                this.matrixT[i][i] -= shift.x;
            }
            final double s = MathLib.abs(this.matrixT[idx][idx - 1]) + MathLib.abs(this.matrixT[idx - 1][idx - 2]);
            shift.x = 0.75 * s;
            shift.y = 0.75 * s;
            shift.w = -0.4375 * s * s;
        }

        // MATLAB's new ad hoc shift
        if (iteration == 30) {
            double s = (shift.y - shift.x) / 2.0;
            s = s * s + shift.w;
            if (s > 0.0) {
                s = MathLib.sqrt(s);
                if (shift.y < shift.x) {
                    s = -s;
                }
                s = shift.x - shift.w / ((shift.y - shift.x) / 2.0 + s);
                for (int i = 0; i <= idx; i++) {
                    this.matrixT[i][i] -= s;
                }
                shift.exShift += s;
                shift.x = 0.964;
                shift.y = 0.964;
                shift.w = 0.964;
            }
        }
    }

    /**
     * Initialize the householder vectors for the QR step.
     * 
     * @param il
     *        the index of the small sub-diagonal element
     * @param iu
     *        the current eigenvalue index
     * @param shift
     *        shift information holder
     * @param hVec
     *        the initial houseHolder vector
     * @return the start index for the QR step
     */
    private int initQRStep(final int il, final int iu, final ShiftInfo shift, final double[] hVec) {
        // Look for two consecutive small sub-diagonal elements
        int im = iu - 2;
        while (im >= il) {
            final double z = this.matrixT[im][im];
            final double r = shift.x - z;
            final double s = shift.y - z;
            hVec[0] = (r * s - shift.w) / this.matrixT[im + 1][im] + this.matrixT[im][im + 1];
            hVec[1] = this.matrixT[im + 1][im + 1] - z - r - s;
            hVec[2] = this.matrixT[im + 2][im + 1];

            if (im == il) {
                break;
            }

            final double lhs = MathLib.abs(this.matrixT[im][im - 1]) * (MathLib.abs(hVec[1]) + MathLib.abs(hVec[2]));
            final double rhs = MathLib.abs(hVec[0]) * (MathLib.abs(this.matrixT[im - 1][im - 1]) +
                MathLib.abs(z) +
                MathLib.abs(this.matrixT[im + 1][im + 1]));

            if (lhs < this.epsilon * rhs) {
                break;
            }
            im--;
        }

        return im;
    }

    /**
     * Perform a double QR step involving rows l:idx and columns m:n
     * 
     * @param il
     *        the index of the small sub-diagonal element
     * @param im
     *        the start index for the QR step
     * @param iu
     *        the current eigenvalue index
     * @param shift
     *        shift information holder
     * @param hVec
     *        the initial houseHolder vector
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void performDoubleQRStep(final int il, final int im, final int iu,
                                     final ShiftInfo shift, final double[] hVec) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        final int n = this.matrixT.length;
        double p = hVec[0];
        double q = hVec[1];
        double r = hVec[2];

        for (int k = im; k <= iu - 1; k++) {
            final boolean notlast = k != (iu - 1);
            if (k != im) {
                p = this.matrixT[k][k - 1];
                q = this.matrixT[k + 1][k - 1];
                r = notlast ? this.matrixT[k + 2][k - 1] : 0.0;
                shift.x = MathLib.abs(p) + MathLib.abs(q) + MathLib.abs(r);
                if (Precision.equals(shift.x, 0.0, epsilon)) {
                    continue;
                }
                p /= shift.x;
                q /= shift.x;
                r /= shift.x;
            }
            double s = MathLib.sqrt(p * p + q * q + r * r);
            if (p < 0.0) {
                s = -s;
            }
            if (s != 0.0) {
                if (k != im) {
                    this.matrixT[k][k - 1] = -s * shift.x;
                } else if (il != im) {
                    this.matrixT[k][k - 1] = -this.matrixT[k][k - 1];
                }
                p += s;
                shift.x = p / s;
                shift.y = q / s;
                final double z = r / s;
                q /= p;
                r /= p;

                // Row modification
                for (int j = k; j < n; j++) {
                    p = this.matrixT[k][j] + q * this.matrixT[k + 1][j];
                    if (notlast) {
                        p += r * matrixT[k + 2][j];
                        this.matrixT[k + 2][j] -= p * z;
                    }
                    this.matrixT[k][j] -= p * shift.x;
                    this.matrixT[k + 1][j] -= p * shift.y;
                }

                // Column modification
                for (int i = 0; i <= MathLib.min(iu, k + 3); i++) {
                    p = shift.x * this.matrixT[i][k] + shift.y * this.matrixT[i][k + 1];
                    if (notlast) {
                        p += z * this.matrixT[i][k + 2];
                        this.matrixT[i][k + 2] -= p * r;
                    }
                    this.matrixT[i][k] -= p;
                    this.matrixT[i][k + 1] -= p * q;
                }

                // Accumulate transformations
                final int high = this.matrixT.length - 1;
                for (int i = 0; i <= high; i++) {
                    p = shift.x * this.matrixP[i][k] + shift.y * this.matrixP[i][k + 1];
                    if (notlast) {
                        p += z * this.matrixP[i][k + 2];
                        this.matrixP[i][k + 2] -= p * r;
                    }
                    this.matrixP[i][k] -= p;
                    this.matrixP[i][k + 1] -= p * q;
                }
            } // (s != 0)
        } // k loop

        // clean up pollution due to round-off errors
        for (int i = im + 2; i <= iu; i++) {
            this.matrixT[i][i - 2] = 0.0;
            if (i > im + 2) {
                this.matrixT[i][i - 3] = 0.0;
            }
        }
    }

    /**
     * Internal data structure holding the current shift information.
     * Contains variable names as present in the original JAMA code.
     */
    private static class ShiftInfo {

        /** x shift info */
        private double x;
        /** y shift info */
        private double y;
        /** w shift info */
        private double w;
        /** Indicates an exceptional shift. */
        private double exShift;

    }

    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume MagicNumber check
}
