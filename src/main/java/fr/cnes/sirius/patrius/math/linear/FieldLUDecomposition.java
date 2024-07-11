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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.lang.reflect.Array;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

/**
 * Calculates the LUP-decomposition of a square matrix.
 * <p>
 * The LUP-decomposition of a matrix A consists of three matrices L, U and P that satisfy: PA = LU, L is lower
 * triangular, and U is upper triangular and P is a permutation matrix. All matrices are m&times;m.
 * </p>
 * <p>
 * Since {@link FieldElement field elements} do not provide an ordering operator, the permutation matrix is computed
 * here only in order to avoid a zero pivot element, no attempt is done to get the largest pivot element.
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
 * @param <T>
 *        the type of the field elements
 * @see <a href="http://mathworld.wolfram.com/LUDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/LU_decomposition">Wikipedia</a>
 * @version $Id: FieldLUDecomposition.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0 (changed to concrete class in 3.0)
 */
@SuppressWarnings("PMD.NullAssignment")
public class FieldLUDecomposition<T extends FieldElement<T>> {

    /** Field to which the elements belong. */
    private final Field<T> field;

    /** Entries of LU decomposition. */
    private final T[][] lu;

    /** Pivot permutation associated with LU decomposition. */
    private final int[] pivot;

    /** Parity of the permutation associated with the LU decomposition. */
    @SuppressWarnings("PMD.ImmutableField")
    private boolean even;

    /** Singularity indicator. */
    @SuppressWarnings("PMD.ImmutableField")
    private boolean singular;

    /** Cached value of L. */
    private FieldMatrix<T> cachedL;

    /** Cached value of U. */
    private FieldMatrix<T> cachedU;

    /** Cached value of P. */
    private FieldMatrix<T> cachedP;

    /**
     * Calculates the LU-decomposition of the given matrix.
     * 
     * @param matrix
     *        The matrix to decompose.
     * @throws NonSquareMatrixException
     *         if matrix is not square
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    public FieldLUDecomposition(final FieldMatrix<T> matrix) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(),
                matrix.getColumnDimension());
        }

        final int m = matrix.getColumnDimension();
        this.field = matrix.getField();
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

            T sum = this.field.getZero();

            // upper
            for (int row = 0; row < col; row++) {
                final T[] luRow = this.lu[row];
                sum = luRow[col];
                for (int i = 0; i < row; i++) {
                    sum = sum.subtract(luRow[i].multiply(this.lu[i][col]));
                }
                luRow[col] = sum;
            }

            // lower
            // permutation row
            int nonZero = col;
            for (int row = col; row < m; row++) {
                final T[] luRow = this.lu[row];
                sum = luRow[col];
                for (int i = 0; i < col; i++) {
                    sum = sum.subtract(luRow[i].multiply(this.lu[i][col]));
                }
                luRow[col] = sum;

                if (this.lu[nonZero][col].equals(this.field.getZero())) {
                    // try to select a better permutation choice
                    ++nonZero;
                }
            }

            // Singularity check
            if (nonZero >= m) {
                this.singular = true;
                return;
            }

            // Pivot if necessary
            if (nonZero != col) {
                T tmp = this.field.getZero();
                for (int i = 0; i < m; i++) {
                    tmp = this.lu[nonZero][i];
                    this.lu[nonZero][i] = this.lu[col][i];
                    this.lu[col][i] = tmp;
                }
                final int temp = this.pivot[nonZero];
                this.pivot[nonZero] = this.pivot[col];
                this.pivot[col] = temp;
                this.even = !this.even;
            }

            // Divide the lower elements by the "winning" diagonal elt.
            final T luDiag = this.lu[col][col];
            for (int row = col + 1; row < m; row++) {
                final T[] luRow = this.lu[row];
                luRow[col] = luRow[col].divide(luDiag);
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
    public FieldMatrix<T> getL() {
        // Check that the cached value of L is not already set and that the matrix is 
        // not singular
        if ((this.cachedL == null) && !this.singular) {
            final int m = this.pivot.length;
            this.cachedL = new Array2DRowFieldMatrix<T>(this.field, m, m);
            // Loop on all entries of LU decomposition
            for (int i = 0; i < m; ++i) {
                final T[] luI = this.lu[i];
                // Loop on all elements of current entry of LU decomposition to compute
                // entries of cached value of L
                for (int j = 0; j < i; ++j) {
                    this.cachedL.setEntry(i, j, luI[j]);
                }
                this.cachedL.setEntry(i, i, this.field.getOne());
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
    public FieldMatrix<T> getU() {
        // Check that the decomposed matrix is not singular
        if ((this.cachedU == null) && !this.singular) {
            // Get pivot length and initialize U matrix
            final int m = this.pivot.length;
            this.cachedU = new Array2DRowFieldMatrix<T>(this.field, m, m);
            // Compute each values of U matrix
            for (int i = 0; i < m; ++i) {
                final T[] luI = this.lu[i];
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
    public FieldMatrix<T> getP() {
        if ((this.cachedP == null) && !this.singular) {
            final int m = this.pivot.length;
            this.cachedP = new Array2DRowFieldMatrix<T>(this.field, m, m);
            for (int i = 0; i < m; ++i) {
                this.cachedP.setEntry(i, this.pivot[i], this.field.getOne());
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
     * Return the determinant of the matrix.
     * 
     * @return determinant of the matrix
     */
    public T getDeterminant() {
        if (this.singular) {
            return this.field.getZero();
        } else {
            final int m = this.pivot.length;
            T determinant = this.even ? this.field.getOne() : this.field.getZero().subtract(this.field.getOne());
            for (int i = 0; i < m; i++) {
                determinant = determinant.multiply(this.lu[i][i]);
            }
            return determinant;
        }
    }

    /**
     * Get a solver for finding the A &times; X = B solution in exact linear sense.
     * 
     * @return a solver
     */
    public FieldDecompositionSolver<T> getSolver() {
        return new Solver<T>(this.field, this.lu, this.pivot, this.singular);
    }

    /**
     * Specialized solver.
     * 
     * @param <T> element
     */
    private static final class Solver<T extends FieldElement<T>> implements FieldDecompositionSolver<T> {

        /** Field to which the elements belong. */
        private final Field<T> field;

        /** Entries of LU decomposition. */
        private final T[][] lu;

        /** Pivot permutation associated with LU decomposition. */
        private final int[] pivot;

        /** Singularity indicator. */
        private final boolean singular;

        /**
         * Build a solver from decomposed matrix.
         * 
         * @param fieldIn
         *        field to which the matrix elements belong
         * @param luIn
         *        entries of LU decomposition
         * @param pivotIn
         *        pivot permutation associated with LU decomposition
         * @param singularIn
         *        singularity indicator
         */
        private Solver(final Field<T> fieldIn, final T[][] luIn,
            final int[] pivotIn, final boolean singularIn) {
            this.field = fieldIn;
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
        @SuppressWarnings("PMD.PreserveStackTrace")
        public FieldVector<T> solve(final FieldVector<T> b) {
            try {
                // Attempt to linear equation with cast as ArrayFieldVector
                return this.solve((ArrayFieldVector<T>) b);
            } catch (final ClassCastException cce) {

                final int m = this.pivot.length;
                if (b.getDimension() != m) {
                    // Exception
                    throw new DimensionMismatchException(b.getDimension(), m);
                }
                if (this.singular) {
                    // Exception
                    throw new SingularMatrixException();
                }

                @SuppressWarnings("unchecked")
                // field is of type T
                final T[] bp = (T[]) Array.newInstance(this.field.getRuntimeClass(), m);

                // Apply permutations to b
                for (int row = 0; row < m; row++) {
                    bp[row] = b.getEntry(this.pivot[row]);
                }

                // Solve LY = b
                for (int col = 0; col < m; col++) {
                    final T bpCol = bp[col];
                    for (int i = col + 1; i < m; i++) {
                        bp[i] = bp[i].subtract(bpCol.multiply(this.lu[i][col]));
                    }
                }

                // Solve UX = Y
                for (int col = m - 1; col >= 0; col--) {
                    bp[col] = bp[col].divide(this.lu[col][col]);
                    final T bpCol = bp[col];
                    for (int i = 0; i < col; i++) {
                        bp[i] = bp[i].subtract(bpCol.multiply(this.lu[i][col]));
                    }
                }

                // Return a new ArrayFieldVector with computed results
                return new ArrayFieldVector<T>(this.field, bp, false);

            }
        }

        /**
         * Solve the linear equation A &times; X = B.
         * <p>
         * The A matrix is implicit here. It is
         * </p>
         * 
         * @param b
         *        right-hand side of the equation A &times; X = B
         * @return a vector X such that A &times; X = B
         * @throws DimensionMismatchException
         *         if the matrices dimensions do not match.
         * @throws SingularMatrixException
         *         if the decomposed matrix is singular.
         */
        public ArrayFieldVector<T> solve(final ArrayFieldVector<T> b) {
            // Get pivot length and b dimension
            final int m = this.pivot.length;
            final int length = b.getDimension();
            if (length != m) {
                // Exception on dimensions
                throw new DimensionMismatchException(length, m);
            }
            if (this.singular) {
                // Exception
                throw new SingularMatrixException();
            }

            @SuppressWarnings("unchecked")
            // field is of type T
            final T[] bp = (T[]) Array.newInstance(this.field.getRuntimeClass(),
                m);

            // Apply permutations to b
            for (int row = 0; row < m; row++) {
                bp[row] = b.getEntry(this.pivot[row]);
            }

            // Solve LY = b
            for (int col = 0; col < m; col++) {
                final T bpCol = bp[col];
                for (int i = col + 1; i < m; i++) {
                    bp[i] = bp[i].subtract(bpCol.multiply(this.lu[i][col]));
                }
            }

            // Solve UX = Y
            for (int col = m - 1; col >= 0; col--) {
                bp[col] = bp[col].divide(this.lu[col][col]);
                final T bpCol = bp[col];
                for (int i = 0; i < col; i++) {
                    bp[i] = bp[i].subtract(bpCol.multiply(this.lu[i][col]));
                }
            }

            // Return result
            return new ArrayFieldVector<T>(bp, false);
        }

        /** {@inheritDoc} */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Commons-Math code kept as such
        @Override
        public FieldMatrix<T> solve(final FieldMatrix<T> b) {
            // CHECKSTYLE: resume CyclomaticComplexity check
            // Check pivot length against b row dimension
            final int m = this.pivot.length;
            if (b.getRowDimension() != m) {
                // Dimensions mismatch
                throw new DimensionMismatchException(b.getRowDimension(), m);
            }
            // Check matrix singularity
            if (this.singular) {
                // Matrix is singular
                throw new SingularMatrixException();
            }

            // Get b column dimension
            final int nColB = b.getColumnDimension();

            // Apply permutations to b
            @SuppressWarnings("unchecked")
            // field is of type T
            final T[][] bp = (T[][]) Array.newInstance(this.field.getRuntimeClass(), new int[] { m, nColB });
            for (int row = 0; row < m; row++) {
                final T[] bpRow = bp[row];
                final int pRow = this.pivot[row];
                for (int col = 0; col < nColB; col++) {
                    bpRow[col] = b.getEntry(pRow, col);
                }
            }

            // Solve LY = b
            for (int col = 0; col < m; col++) {
                final T[] bpCol = bp[col];
                for (int i = col + 1; i < m; i++) {
                    final T[] bpI = bp[i];
                    final T luICol = this.lu[i][col];
                    for (int j = 0; j < nColB; j++) {
                        bpI[j] = bpI[j].subtract(bpCol[j].multiply(luICol));
                    }
                }
            }

            // Solve UX = Y
            for (int col = m - 1; col >= 0; col--) {
                final T[] bpCol = bp[col];
                final T luDiag = this.lu[col][col];
                // Loop on all columns
                for (int j = 0; j < nColB; j++) {
                    bpCol[j] = bpCol[j].divide(luDiag);
                }
                for (int i = 0; i < col; i++) {
                    final T[] bpI = bp[i];
                    final T luICol = this.lu[i][col];
                    for (int j = 0; j < nColB; j++) {
                        bpI[j] = bpI[j].subtract(bpCol[j].multiply(luICol));
                    }
                }
            }

            // Return result
            //
            return new Array2DRowFieldMatrix<T>(this.field, bp, false);

        }

        /** {@inheritDoc} */
        @Override
        public FieldMatrix<T> getInverse() {
            final int m = this.pivot.length;
            final T one = this.field.getOne();
            final FieldMatrix<T> identity = new Array2DRowFieldMatrix<T>(this.field, m, m);
            for (int i = 0; i < m; ++i) {
                identity.setEntry(i, i, one);
            }
            return this.solve(identity);
        }
    }
}
