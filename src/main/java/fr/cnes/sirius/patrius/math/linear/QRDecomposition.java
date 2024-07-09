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

import java.util.Arrays;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Calculates the QR-decomposition of a matrix.
 * <p>
 * The QR-decomposition of a matrix A consists of two matrices Q and R that satisfy: A = QR, Q is orthogonal
 * (Q<sup>T</sup>Q = I), and R is upper triangular. If A is m&times;n, Q is m&times;m and R m&times;n.
 * </p>
 * <p>
 * This class compute the decomposition using Householder reflectors.
 * </p>
 * <p>
 * For efficiency purposes, the decomposition in packed form is transposed. This allows inner loop to iterate inside
 * rows, which is much more cache-efficient in Java.
 * </p>
 * <p>
 * This class is based on the class with similar name from the <a
 * href="http://math.nist.gov/javanumerics/jama/">JAMA</a> library, with the following changes:
 * </p>
 * <ul>
 * <li>a {@link #getQT() getQT} method has been added,</li>
 * <li>the {@code solve} and {@code isFullRank} methods have been replaced by a {@link #getSolver() getSolver} method
 * and the equivalent methods provided by the returned {@link DecompositionSolver}.</li>
 * </ul>
 * 
 * <p>This class is up-to-date with commons-math 3.6.1.</p>
 * 
 * @see <a href="http://mathworld.wolfram.com/QRDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/QR_decomposition">Wikipedia</a>
 * 
 * @version $Id: QRDecomposition.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2 (changed to concrete class in 3.0)
 */
@SuppressWarnings("PMD.NullAssignment")
public class QRDecomposition implements Decomposition {
    /**
     * A packed TRANSPOSED representation of the QR decomposition.
     * <p>
     * The elements BELOW the diagonal are the elements of the UPPER triangular matrix R, and the rows ABOVE the
     * diagonal are the Householder reflector vectors from which an explicit form of Q can be recomputed if desired.
     * </p>
     */
    private double[][] qrt;
    /** The diagonal elements of R. */
    private double[] rDiag;
    /** Cached value of Q. */
    private RealMatrix cachedQ;
    /** Cached value of QT. */
    private RealMatrix cachedQT;
    /** Cached value of R. */
    private RealMatrix cachedR;
    /** Cached value of H. */
    private RealMatrix cachedH;
    /** Singularity threshold. */
    private final double threshold;

    /**
     * Simple constructor.
     * The singularity threshold defaults to zero.
     * 
     * <p>
     * The decomposition is directly computed on the input matrix
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     * @see #QRDecomposition(double)
     */
    public QRDecomposition(final RealMatrix matrix) {
        this(matrix, 0d);
    }

    /**
     * Constructor used to set the singularity threshold.
     * 
     * <p>
     * The decomposition is directly computed on the input matrix
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     * @param thresholdIn
     *        Singularity threshold.
     */
    public QRDecomposition(final RealMatrix matrix, final double thresholdIn) {
        this.threshold = thresholdIn;
        qrDecompose(matrix);
    }

    /**
     * <p>
     * Calculates the QR-decomposition of the given matrix.
     * </p>
     * <p>
     * The QR decomposition of a matrix A is calculated using Householder reflectors by repeating the following
     * operations to each minor A(minor,minor) of A.
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     **/
    private void qrDecompose(final RealMatrix matrix) {
        final int m = matrix.getRowDimension();
        final int n = matrix.getColumnDimension();
        this.qrt = matrix.transpose().getData(false);
        this.rDiag = new double[MathLib.min(m, n)];
        this.cachedQ = null;
        this.cachedQT = null;
        this.cachedR = null;
        this.cachedH = null;

        for (int minor = 0; minor < MathLib.min(qrt.length, qrt[0].length); minor++) {
            performHouseholderReflection(minor, qrt);
        }
    }

    /**
     * Perform Householder reflection for a minor A(minor, minor) of A.
     * 
     * @param minor minor index
     * @param matrix transposed matrix
     */
    protected void performHouseholderReflection(final int minor, final double[][] matrix) {

        final double[] qrtMinor = matrix[minor];

        /*
         * Let x be the first column of the minor, and a^2 = |x|^2.
         * x will be in the positions qr[minor][minor] through qr[m][minor].
         * The first column of the transformed minor will be (a,0,0,..)'
         * The sign of a is chosen to be opposite to the sign of the first
         * component of x. Let's find a:
         */
        double xNormSqr = 0;
        for (int row = minor; row < qrtMinor.length; row++) {
            final double c = qrtMinor[row];
            xNormSqr += c * c;
        }
        final double a = (qrtMinor[minor] > 0) ? -MathLib.sqrt(xNormSqr) : MathLib.sqrt(xNormSqr);
        this.rDiag[minor] = a;

        if (a != 0.0) {

            /*
             * Calculate the normalized reflection vector v and transform
             * the first column. We know the norm of v beforehand: v = x-ae
             * so |v|^2 = <x-ae,x-ae> = <x,x>-2a<x,e>+a^2<e,e> =
             * a^2+a^2-2a<x,e> = 2a*(a - <x,e>).
             * Here <x, e> is now qr[minor][minor].
             * v = x-ae is stored in the column at qr:
             */
            qrtMinor[minor] -= a; // now |v|^2 = -2a*(qr[minor][minor])

            /*
             * Transform the rest of the columns of the minor:
             * They will be transformed by the matrix H = I-2vv'/|v|^2.
             * If x is a column vector of the minor, then
             * Hx = (I-2vv'/|v|^2)x = x-2vv'x/|v|^2 = x - 2<x,v>/|v|^2 v.
             * Therefore the transformation is easily calculated by
             * subtracting the column vector (2<x,v>/|v|^2)v from x.
             * Let 2<x,v>/|v|^2 = alpha. From above we have
             * |v|^2 = -2a*(qr[minor][minor]), so
             * alpha = -<x,v>/(a*qr[minor][minor])
             */
            for (int col = minor + 1; col < matrix.length; col++) {
                final double[] qrtCol = matrix[col];
                double alpha = 0;
                for (int row = minor; row < qrtCol.length; row++) {
                    alpha -= qrtCol[row] * qrtMinor[row];
                }
                alpha /= a * qrtMinor[minor];

                // Subtract the column vector alpha*v from x.
                for (int row = minor; row < qrtCol.length; row++) {
                    qrtCol[row] -= alpha * qrtMinor[row];
                }
            }
        }
    }

    /**
     * Returns the matrix R of the decomposition. By default, this method returns the full form (n&nbsp;&times;&nbsp;m)
     * of the matrix R.
     * <p>
     * R is an upper-triangular matrix
     * </p>
     * 
     * @return the R matrix
     */
    public RealMatrix getR() {
        return getR(false);
    }

    /**
     * Returns the matrix R of the decomposition in its compact form (n&nbsp;&times;&nbsp;n) or in its full form
     * (m&nbsp;&times;&nbsp;n).
     * <p>
     * R is an upper-triangular matrix
     * </p>
     * 
     * @param compactForm if {@code true} R dimensions will be n&nbsp;&times;&nbsp;n, else R dimensions will be
     *        m&nbsp;&times;&nbsp;n
     * @return the R matrix
     */
    public RealMatrix getR(final boolean compactForm) {

        // R is supposed to be m x n
        final int n = this.qrt.length;
        final int m = this.qrt[0].length;

        final double[][] ra;
        if (compactForm) {
            ra = new double[MathLib.min(m, n)][n];
        } else {
            ra = new double[m][n];
        }
        // copy the diagonal from rDiag and the upper triangle of qr
        for (int row = MathLib.min(m, n) - 1; row >= 0; row--) {
            ra[row][row] = this.rDiag[row];
            for (int col = row + 1; col < n; col++) {
                ra[row][col] = this.qrt[col][row];
            }
        }
        this.cachedR = MatrixUtils.createRealMatrix(ra, false);


        // return the cached matrix
        return this.cachedR;
    }

    /**
     * Returns the matrix Q of the decomposition.
     * <p>
     * Q is an orthogonal matrix
     * </p>
     * 
     * @return the Q matrix
     */
    public RealMatrix getQ() {
        this.cachedQ = this.getQT().transpose();
        return this.cachedQ;
    }

    /**
     * Returns the transpose of the matrix Q of the decomposition.
     * <p>
     * Q is an orthogonal matrix
     * </p>
     * 
     * @return the transpose of the Q matrix, Q<sup>T</sup>
     */
    public RealMatrix getQT() {
        // QT is supposed to be m x m
        final int n = this.qrt.length;
        final int m = this.qrt[0].length;
        final double[][] qta = new double[m][m];

        /*
         * Q = Q1 Q2 ... Q_m, so Q is formed by first constructing Q_m and then
         * applying the Householder transformations Q_(m-1),Q_(m-2),...,Q1 in
         * succession to the result
         */
        for (int minor = m - 1; minor >= MathLib.min(m, n); minor--) {
            qta[minor][minor] = 1.0d;
        }

        for (int minor = MathLib.min(m, n) - 1; minor >= 0; minor--) {
            final double[] qrtMinor = this.qrt[minor];
            qta[minor][minor] = 1.0d;
            if (qrtMinor[minor] != 0.0) {
                for (int col = minor; col < m; col++) {
                    double alpha = 0;
                    for (int row = minor; row < m; row++) {
                        alpha -= qta[col][row] * qrtMinor[row];
                    }
                    alpha /= this.rDiag[minor] * qrtMinor[minor];

                    for (int row = minor; row < m; row++) {
                        qta[col][row] += -alpha * qrtMinor[row];
                    }
                }
            }
        }
        this.cachedQT = MatrixUtils.createRealMatrix(qta, false);

        return this.cachedQT;
    }
    
    /**
     * Returns the Householder reflector vectors.
     * <p>
     * H is a lower trapezoidal matrix whose columns represent each successive Householder reflector vector. This matrix
     * is used to compute Q.
     * </p>
     * 
     * @return a matrix containing the Householder reflector vectors
     */
    public RealMatrix getH() {
        final int n = this.qrt.length;
        final int m = this.qrt[0].length;
        final double[][] ha = new double[m][n];
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < MathLib.min(i + 1, n); ++j) {
                ha[i][j] = this.qrt[j][i] / -this.rDiag[j];
            }
        }
        this.cachedH = MatrixUtils.createRealMatrix(ha, false);

        return this.cachedH;
    }

    /**
     * {@inheritDoc}
     * Get a solver for finding the A &times; X = B solution in least square sense.
     * <p>
     * Least Square sense means a solver can be computed for an overdetermined system,
     * (i.e. a system with more equations than unknowns, which corresponds to a tall A
     * matrix with more rows than columns). In any case, if the matrix is singular
     * within the tolerance set at {@link QRDecomposition#QRDecomposition(RealMatrix,
     * double) construction}, an error will be triggered when
     * the {@link DecompositionSolver#solve(RealVector) solve} method will be called.
     * </p>
     * @return a solver
     */
    @Override
    public DecompositionSolver getSolver() {
        return new Solver(this.qrt, this.rDiag, this.threshold);
    }
    
    /**
     * Builder for decomposition. 
     * 
     * @param thresholdIn
     *        Singularity threshold.
     * @return decomposition
     */
    public static Function<RealMatrix, Decomposition> decompositionBuilder (final double thresholdIn) {
        return (realMatrix) -> new QRDecomposition(realMatrix, thresholdIn);
    }

    /** Specialized solver. */
    private static final class Solver implements DecompositionSolver {
        /**
         * A packed TRANSPOSED representation of the QR decomposition.
         * <p>
         * The elements BELOW the diagonal are the elements of the UPPER triangular matrix R, and the rows ABOVE the
         * diagonal are the Householder reflector vectors from which an explicit form of Q can be recomputed if desired.
         * </p>
         */
        private final double[][] qrt;
        /** The diagonal elements of R. */
        private final double[] rDiag;
        /** Singularity threshold. */
        private final double threshold;

        /**
         * Build a solver from decomposed matrix.
         * 
         * @param qrtIn
         *        Packed TRANSPOSED representation of the QR decomposition.
         * @param rDiagIn
         *        Diagonal elements of R.
         * @param thresholdIn
         *        Singularity threshold.
         */
        private Solver(final double[][] qrtIn, final double[] rDiagIn, final double thresholdIn) {
            this.qrt = qrtIn;
            this.rDiag = rDiagIn;
            this.threshold = thresholdIn;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isNonSingular() {
            for (final double diag : this.rDiag) {
                if (MathLib.abs(diag) <= this.threshold) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public RealVector solve(final RealVector b) {
            // Sanity checks
            final int m = this.qrt[0].length;
            if (b.getDimension() != m) {
                throw new DimensionMismatchException(b.getDimension(), m);
            }
            if (!this.isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int n = this.qrt.length;

            final double[] x = new double[n];
            final double[] y = b.toArray();

            // apply Householder transforms to solve Q.y = b
            for (int minor = 0; minor < MathLib.min(m, n); minor++) {

                final double[] qrtMinor = this.qrt[minor];
                double dotProduct = 0;
                for (int row = minor; row < m; row++) {
                    dotProduct += y[row] * qrtMinor[row];
                }
                dotProduct /= this.rDiag[minor] * qrtMinor[minor];

                for (int row = minor; row < m; row++) {
                    y[row] += dotProduct * qrtMinor[row];
                }
            }

            // solve triangular system R.x = y
            for (int row = this.rDiag.length - 1; row >= 0; --row) {
                y[row] /= this.rDiag[row];
                final double yRow = y[row];
                final double[] qrtRow = this.qrt[row];
                x[row] = yRow;
                for (int i = 0; i < row; i++) {
                    y[i] -= yRow * qrtRow[i];
                }
            }

            // Return result
            return new ArrayRealVector(x, false);
        }

        /** {@inheritDoc} */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Commons-Math code kept as such
        @Override
        @SuppressWarnings("PMD.AvoidArrayLoops")
        public RealMatrix solve(final RealMatrix b) {
            // CHECKSTYLE: resume CyclomaticComplexity check

            // Sanity checks
            final int m = this.qrt[0].length;
            if (b.getRowDimension() != m) {
                throw new DimensionMismatchException(b.getRowDimension(), m);
            }
            if (!this.isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int n = this.qrt.length;

            final int columns = b.getColumnDimension();
            final int blockSize = BlockRealMatrix.BLOCK_SIZE;
            final int cBlocks = (columns + blockSize - 1) / blockSize;
            final double[][] xBlocks = BlockRealMatrix.createBlocksLayout(n, columns);
            final double[][] y = new double[b.getRowDimension()][blockSize];
            final double[] alpha = new double[blockSize];

            for (int kBlock = 0; kBlock < cBlocks; ++kBlock) {
                final int kStart = kBlock * blockSize;
                final int kEnd = MathLib.min(kStart + blockSize, columns);
                final int kWidth = kEnd - kStart;

                // get the right hand side vector
                b.copySubMatrix(0, m - 1, kStart, kEnd - 1, y);

                // apply Householder transforms to solve Q.y = b
                for (int minor = 0; minor < MathLib.min(m, n); minor++) {
                    final double[] qrtMinor = this.qrt[minor];
                    final double factor = 1.0 / (this.rDiag[minor] * qrtMinor[minor]);

                    Arrays.fill(alpha, 0, kWidth, 0.0);
                    for (int row = minor; row < m; ++row) {
                        final double d = qrtMinor[row];
                        final double[] yRow = y[row];
                        for (int k = 0; k < kWidth; ++k) {
                            alpha[k] += d * yRow[k];
                        }
                    }
                    for (int k = 0; k < kWidth; ++k) {
                        alpha[k] *= factor;
                    }

                    for (int row = minor; row < m; ++row) {
                        final double d = qrtMinor[row];
                        final double[] yRow = y[row];
                        for (int k = 0; k < kWidth; ++k) {
                            yRow[k] += alpha[k] * d;
                        }
                    }
                }

                // solve triangular system R.x = y
                for (int j = this.rDiag.length - 1; j >= 0; --j) {
                    final int jBlock = j / blockSize;
                    final int jStart = jBlock * blockSize;
                    final double factor = 1.0 / this.rDiag[j];
                    final double[] yJ = y[j];
                    final double[] xBlock = xBlocks[jBlock * cBlocks + kBlock];
                    int index = (j - jStart) * kWidth;
                    for (int k = 0; k < kWidth; ++k) {
                        yJ[k] *= factor;
                        xBlock[index++] = yJ[k];
                    }

                    final double[] qrtJ = this.qrt[j];
                    for (int i = 0; i < j; ++i) {
                        final double rIJ = qrtJ[i];
                        final double[] yI = y[i];
                        for (int k = 0; k < kWidth; ++k) {
                            yI[k] -= yJ[k] * rIJ;
                        }
                    }
                }
            }

            // Return result
            return new BlockRealMatrix(n, columns, xBlocks, false);
        }

        /**
         * {@inheritDoc}
         * 
         * @throws SingularMatrixException if the decomposed matrix is singular.
         **/
        @Override
        public RealMatrix getInverse() {
            return this.solve(MatrixUtils.createRealIdentityMatrix(this.qrt[0].length, true));

        }
        
    }

    // CHECKSTYLE: resume CommentRatio check
}
