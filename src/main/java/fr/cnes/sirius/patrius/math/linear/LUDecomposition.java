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

import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Calculates the LUP-decomposition of a square matrix.
 * <p>
 * The LUP-decomposition of a matrix A consists of three matrices L, U and P that satisfy: P&times;A = L&times;U. L is
 * lower triangular (with unit diagonal terms), U is upper triangular and P is a permutation matrix. All matrices are
 * m&times;m.
 * </p>
 * <p>
 * As shown by the presence of the P matrix, this decomposition is implemented using partial pivoting.
 * </p>
 * <p>
 * This class is based on the class with similar name from the <a
 * href="http://math.nist.gov/javanumerics/jama/">JAMA</a> library.
 * </p>
 * <ul>
 * <li>a {@link #getP() getP} method has been added,</li>
 * <li>the {@code det} method has been renamed as {@link #getDeterminant()
 * getDeterminant},</li>
 * <li>the {@code getDoublePivot} method has been removed (but the int based {@link #getPivot() getPivot} method has
 * been kept),</li>
 * <li>the {@code solve} and {@code isNonSingular} methods have been replaced by a {@link #getSolver() getSolver} method
 * and the equivalent methods provided by the returned {@link DecompositionSolver}.</li>
 * </ul>
 * 
 * @see <a href="http://mathworld.wolfram.com/LUDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/LU_decomposition">Wikipedia</a>
 * @version $Id: LUDecomposition.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0 (changed to concrete class in 3.0)
 */
@SuppressWarnings("PMD.NullAssignment")
public class LUDecomposition implements Decomposition {
    /** Default bound to determine effective singularity in LU decomposition. */
    private static final double DEFAULT_TOO_SMALL = 1e-11;
    /** Entries of LU decomposition. */
    private double[][] lu;
    /** Pivot permutation associated with LU decomposition. */
    private int[] pivot;
    /** Parity of the permutation associated with the LU decomposition. */
    private boolean even;
    /** Singularity indicator. */
    private boolean singular;
    /** Cached value of L. */
    private RealMatrix cachedL;
    /** Cached value of U. */
    private RealMatrix cachedU;
    /** Cached value of P. */
    private RealMatrix cachedP;
    /** threshold (based on partial row norm) under which a matrix is considered singular. */
    private final double singularityThreshold;

    /**
     * Simple constructor.
     * The singularity threshold defaults to 1e-11.
     * 
     * <p>
     * The decomposition is directly computed on the input matrix
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     * @throws NonSquareMatrixException
     *         if matrix is not square.
     * @see #LUDecomposition(RealMatrix, double)
     */
    public LUDecomposition(final RealMatrix matrix) {
        this(matrix, DEFAULT_TOO_SMALL);
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
     * @param singularityThreshold
     *        threshold (based on partial row norm) under which a matrix is considered singular
     * @throws NonSquareMatrixException
     *         if matrix is not square
     */
    public LUDecomposition(final RealMatrix matrix, final double singularityThreshold) {
        this.singularityThreshold = singularityThreshold;
        luDecompose(matrix);
    }

    /**
     * Calculates the LU-decomposition of the given matrix.
     * 
     * @param matrix
     *        The matrix to decompose.
     **/
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void luDecompose(final RealMatrix matrix) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(),
                matrix.getColumnDimension());
        }

        final int m = matrix.getColumnDimension();
        this.lu = matrix.getData();
        this.pivot = new int[m];
        this.cachedL = null;
        this.cachedU = null;
        this.cachedP = null;

        // Initialize permutation array and parity
        for (int row = 0; row < m; row++) {
            this.pivot[row] = row;
        }
        this.even = true;
        this.singular = false;

        // Loop over columns
        for (int col = 0; col < m; col++) {

            // upper
            for (int row = 0; row < col; row++) {
                final double[] luRow = this.lu[row];
                double sum = luRow[col];
                for (int i = 0; i < row; i++) {
                    sum -= luRow[i] * this.lu[i][col];
                }
                luRow[col] = sum;
            }

            // lower
            // permutation row
            int max = col;
            double largest = Double.NEGATIVE_INFINITY;
            for (int row = col; row < m; row++) {
                final double[] luRow = this.lu[row];
                double sum = luRow[col];
                for (int i = 0; i < col; i++) {
                    sum -= luRow[i] * this.lu[i][col];
                }
                luRow[col] = sum;

                // maintain best permutation choice
                if (MathLib.abs(sum) > largest) {
                    largest = MathLib.abs(sum);
                    max = row;
                }
            }

            // Singularity check
            if (MathLib.abs(this.lu[max][col]) < singularityThreshold) {
                this.singular = true;
                return;
            }

            // Pivot if necessary
            if (max != col) {
                double tmp = 0;
                final double[] luMax = this.lu[max];
                final double[] luCol = this.lu[col];
                for (int i = 0; i < m; i++) {
                    tmp = luMax[i];
                    luMax[i] = luCol[i];
                    luCol[i] = tmp;
                }
                final int temp = this.pivot[max];
                this.pivot[max] = this.pivot[col];
                this.pivot[col] = temp;
                this.even = !this.even;
            }

            // Divide the lower elements by the "winning" diagonal elt.
            final double luDiag = this.lu[col][col];
            for (int row = col + 1; row < m; row++) {
                this.lu[row][col] /= luDiag;
            }
        }
    }

    /**
     * Returns the matrix L of the decomposition.
     * <p>
     * L is a lower-triangular matrix
     * </p>
     * 
     * @return the L matrix (or null if decomposed matrix is singular)
     */
    public RealMatrix getL() {
        if (!this.singular) {
            final int m = this.pivot.length;
            this.cachedL = MatrixUtils.createRealMatrix(m, m);
            for (int i = 0; i < m; ++i) {
                final double[] luI = this.lu[i];
                for (int j = 0; j < i; ++j) {
                    this.cachedL.setEntry(i, j, luI[j]);
                }
                this.cachedL.setEntry(i, i, 1.0);
            }
        }

        return this.cachedL;
    }

    /**
     * Returns the matrix U of the decomposition.
     * <p>
     * U is an upper-triangular matrix
     * </p>
     * 
     * @return the U matrix (or null if decomposed matrix is singular)
     */
    public RealMatrix getU() {
        if (!this.singular) {
            final int m = this.pivot.length;
            this.cachedU = MatrixUtils.createRealMatrix(m, m);
            for (int i = 0; i < m; ++i) {
                final double[] luI = this.lu[i];
                for (int j = i; j < m; ++j) {
                    this.cachedU.setEntry(i, j, luI[j]);
                }
            }
        }

        return this.cachedU;
    }

    /**
     * Returns the P rows permutation matrix.
     * <p>
     * P is a sparse matrix with exactly one element set to 1.0 in each row and each column, all other elements being
     * set to 0.0.
     * </p>
     * <p>
     * The positions of the 1 elements are given by the {@link #getPivot()
     * pivot permutation vector}.
     * </p>
     * 
     * @return the P rows permutation matrix (or null if decomposed matrix is singular)
     * @see #getPivot()
     */
    public RealMatrix getP() {
        if (!this.singular) {
            final int m = this.pivot.length;
            this.cachedP = MatrixUtils.createRealMatrix(m, m);
            for (int i = 0; i < m; ++i) {
                this.cachedP.setEntry(i, this.pivot[i], 1.0);
            }
        }

        return this.cachedP;
    }

    /**
     * Returns the pivot permutation vector.
     * 
     * @return the pivot permutation vector
     * @see #getP()
     */
    public int[] getPivot() {
        return this.pivot.clone();
    }

    /**
     * Return the determinant of the matrix
     * 
     * @return determinant of the matrix
     */
    public double getDeterminant() {
        if (this.singular) {
            return 0;
        } else {
            final int m = this.pivot.length;
            double determinant = this.even ? 1 : -1;
            for (int i = 0; i < m; i++) {
                determinant *= this.lu[i][i];
            }
            return determinant;
        }
    }

    /** {@inheritDoc} */
    @Override
    public DecompositionSolver getSolver() {
        return new Solver(this.lu, this.pivot, this.singular);
    }

    /**
     * Builder for decomposition. 
     * 
     * @param singularityThreshold
     *        Singularity threshold
     * @return decomposition
     */
    public static Function<RealMatrix, Decomposition> decompositionBuilder (final double singularityThreshold) {
        return (realMatrix) -> new LUDecomposition(realMatrix, singularityThreshold);
    }
    
    /** Specialized solver. */
    private static final class Solver implements DecompositionSolver {

        /** Entries of LU decomposition. */
        private final double[][] lu;

        /** Pivot permutation associated with LU decomposition. */
        private final int[] pivot;

        /** Singularity indicator. */
        private final boolean singular;

        /**
         * Build a solver from decomposed matrix.
         * 
         * @param luIn
         *        entries of LU decomposition
         * @param pivotIn
         *        pivot permutation associated with LU decomposition
         * @param singularIn
         *        singularity indicator
         */
        private Solver(final double[][] luIn, final int[] pivotIn, final boolean singularIn) {
            this.lu = luIn;
            this.pivot = pivotIn;
            this.singular = singularIn;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isNonSingular() {
            return !this.singular;
        }

        /** {@inheritDoc} */
        @Override
        public RealVector solve(final RealVector b) {
            final int m = this.pivot.length;
            if (b.getDimension() != m) {
                throw new DimensionMismatchException(b.getDimension(), m);
            }
            if (this.singular) {
                throw new SingularMatrixException();
            }

            final double[] bp = new double[m];

            // Apply permutations to b
            for (int row = 0; row < m; row++) {
                bp[row] = b.getEntry(this.pivot[row]);
            }

            // Solve LY = b
            for (int col = 0; col < m; col++) {
                final double bpCol = bp[col];
                for (int i = col + 1; i < m; i++) {
                    bp[i] -= bpCol * this.lu[i][col];
                }
            }

            // Solve UX = Y
            for (int col = m - 1; col >= 0; col--) {
                bp[col] /= this.lu[col][col];
                final double bpCol = bp[col];
                for (int i = 0; i < col; i++) {
                    bp[i] -= bpCol * this.lu[i][col];
                }
            }

            return new ArrayRealVector(bp, false);
        }

        /** {@inheritDoc} */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Commons-Math code kept as such
        @Override
        public RealMatrix solve(final RealMatrix b) {
            // CHECKSTYLE: resume CyclomaticComplexity check

            final int m = this.pivot.length;
            if (b.getRowDimension() != m) {
                throw new DimensionMismatchException(b.getRowDimension(), m);
            }
            if (this.singular) {
                throw new SingularMatrixException();
            }

            final int nColB = b.getColumnDimension();

            // Apply permutations to b
            final double[][] bp = new double[m][nColB];
            for (int row = 0; row < m; row++) {
                final double[] bpRow = bp[row];
                final int pRow = this.pivot[row];
                for (int col = 0; col < nColB; col++) {
                    bpRow[col] = b.getEntry(pRow, col);
                }
            }

            // Solve LY = b
            for (int col = 0; col < m; col++) {
                final double[] bpCol = bp[col];
                for (int i = col + 1; i < m; i++) {
                    final double[] bpI = bp[i];
                    final double luICol = this.lu[i][col];
                    for (int j = 0; j < nColB; j++) {
                        bpI[j] -= bpCol[j] * luICol;
                    }
                }
            }

            // Solve UX = Y
            for (int col = m - 1; col >= 0; col--) {
                final double[] bpCol = bp[col];
                final double luDiag = this.lu[col][col];
                for (int j = 0; j < nColB; j++) {
                    bpCol[j] /= luDiag;
                }
                for (int i = 0; i < col; i++) {
                    final double[] bpI = bp[i];
                    final double luICol = this.lu[i][col];
                    for (int j = 0; j < nColB; j++) {
                        bpI[j] -= bpCol[j] * luICol;
                    }
                }
            }

            return new Array2DRowRealMatrix(bp, false);
        }

        /** {@inheritDoc} */
        @Override
        public RealMatrix getInverse() {
            return this.solve(MatrixUtils.createRealIdentityMatrix(this.pivot.length, true));
        }

    }

    // CHECKSTYLE: resume CommentRatio check
}
