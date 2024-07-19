/**
 * Copyright 2019-2020 CNES
 * Copyright 2011-2014 JOptimizer
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Algebraic utility operations.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * 
 * @since 4.6
 */
public final class AlgebraUtils {

    /** Message. */
    private static final String WRONG_MAT_DIM = "wrong matrices dimensions";
    /** Message. */
    private static final String WRONG_VECT_DIM = "wrong vectors dimensions";
    /** Message. */
    private static final String FOUND = ", found: ";

    /**
     * Private constructor.
     */
    private AlgebraUtils() {
    }

    /**
     * Matrix-vector multiplication with diagonal matrix.
     * 
     * @param diagonalM
     *        diagonal matrix M, in the form of a vector of its diagonal
     *        elements
     * @param vector
     *        vector
     * @return M.x
     */
    public static final RealVector diagonalMatrixMult(final RealVector diagonalM,
            final RealVector vector) {
        final int n = diagonalM.getDimension();
        final RealVector ret = new ArrayRealVector(n);
        for (int i = 0; i < n; i++) {
            ret.setEntry(i, diagonalM.getEntry(i) * vector.getEntry(i));
        }
        return ret;
    }

    /**
     * Return diagonalU.A with diagonalU diagonal.
     * 
     * @param diagonalU
     *        matrix U, in the form of a vector of its diagonal elements
     * @param a
     *        matrix A
     * @return U.A
     */
    public static final RealMatrix diagonalMatrixMult(final RealVector diagonalU,
            final RealMatrix a) {
        final int r = diagonalU.getDimension();
        final int c = a.getColumnDimension();
        final RealMatrix ret = new BlockRealMatrix(r, c);
        // loop into each element for multiplication
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                // set U.A
                ret.setEntry(i, j, a.getEntry(i, j) * diagonalU.getEntry(i));
            }
        }
        return ret; // diagonal multiplication results
    }

    /**
     * Return A.diagonalU with diagonalU diagonal.
     * 
     * @param a
     *        matrix A
     * @param diagonalU
     *        matrix U, in the form of a vector of its diagonal elements
     * @return U.A
     */
    public static final RealMatrix diagonalMatrixMult(final RealMatrix a,
            final RealVector diagonalU) {
        final int r = diagonalU.getDimension();
        final int c = a.getColumnDimension();
        final RealMatrix ret = new BlockRealMatrix(r, c);
        // loop into each element for multiplication
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                // set U.A
                ret.setEntry(i, j, a.getEntry(i, j) * diagonalU.getEntry(j));
            }
        }

        return ret; // diagonal multiplication results
    }

    /**
     * Return diagonalU.A.diagonalV with diagonalU and diagonalV diagonal.
     * 
     * @param diagonalU
     *        diagonal matrix U, in the form of a vector of its diagonal
     *        elements
     * @param a
     *        matrix A
     * @param diagonalV
     *        diagonal matrix V, in the form of a vector of its diagonal
     *        elements
     * @return U.A.V
     */
    public static final RealMatrix diagonalMatrixMult(final RealVector diagonalU,
            final RealMatrix a,
            final RealVector diagonalV) {
        final int r = a.getRowDimension();
        final int c = a.getColumnDimension();
        final RealMatrix ret = new BlockRealMatrix(r, c);
        // loop into each element for multiplication
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                // set U.A.V
                ret.setEntry(i, j, a.getEntry(i, j) * diagonalU.getEntry(i) * diagonalV.getEntry(j));
            }
        }

        return ret; // diagonal multiplication results
    }

    /**
     * Return the sub-diagonal result of the multiplication. If A is sparse,
     * returns a sparse matrix (even if, generally speaking, the multiplication
     * of two sparse matrices is not sparse) because the result is at least 50%
     * (aside the diagonal elements) sparse.
     * 
     * @param a
     *        matrix
     * @param b
     *        matrix
     * @return sub-diagonal result of the multiplication
     */
    public static RealMatrix subdiagonalMultiply(final RealMatrix a,
            final RealMatrix b) {
        final int r = a.getRowDimension();
        final int c = b.getColumnDimension();
        if (r != c) {
            // dimension mismatch
            throw new IllegalArgumentException("The result must be square");
        }

        final RealMatrix ret = new BlockRealMatrix(r, c);
        final int rc = a.getColumnDimension();
        // loop into each element for multiplication
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < i + 1; j++) {
                double s = 0;
                for (int k = 0; k < rc; k++) {
                    // sub-diagonal multiplication
                    s += a.getEntry(i, k) * b.getEntry(k, j);
                }
                ret.setEntry(i, j, s);
            }
        }

        return ret; // return sub-diagonal result of the multiplication
    }

    /**
     * Returns v = A.a + beta*b. Useful in avoiding the need of the copy() in
     * the colt api.
     * 
     * @param matA
     *        matrix A
     * @param a
     *        vector
     * @param b
     *        vector
     * @param beta
     *        double
     * @return v
     */
    public static final RealVector zMult(final RealMatrix matA,
            final RealVector a,
            final RealVector b,
            final double beta) {

        if (matA.getColumnDimension() != a.getDimension()) {
            // dimensions mismatch
            throw new IllegalArgumentException(
                    "Wrong matrix dimensions. Number of columns must be " + a.getDimension()
                            + FOUND + matA.getColumnDimension());
        }

        if (matA.getRowDimension() != b.getDimension()) {
            // dimensions mismatch
            throw new IllegalArgumentException("Wrong matrix dimensions. Number of rows must be "
                    + b.getDimension() + FOUND + matA.getRowDimension());
        }

        final RealVector ret = new ArrayRealVector(matA.getRowDimension());

        for (int i = 0; i < matA.getRowDimension(); i++) {
            // beta*b.
            double vi = beta * b.getEntry(i);
            for (int j = 0; j < matA.getColumnDimension(); j++) {
                // A.a + (beta*b)
                vi += matA.getEntry(i, j) * a.getEntry(j);
            }
            ret.setEntry(i, vi);
        }

        return ret;
    }

    /**
     * Returns v = A[T].a + beta*b. Useful in avoiding the need of the copy() in
     * the colt api.
     * 
     * @param matA
     *        matrix A
     * @param a
     *        vector
     * @param b
     *        vector
     * @param beta
     *        constant
     * @return v
     */
    public static final RealVector zMultTranspose(final RealMatrix matA,
            final RealVector a,
            final RealVector b,
            final double beta) {
        if (matA.getRowDimension() != a.getDimension()
                || matA.getColumnDimension() != b.getDimension()) {
            // dimensions mismatch
            throw new IllegalArgumentException(WRONG_MAT_DIM);
        }
        final RealVector ret = new ArrayRealVector(matA.getColumnDimension());

        for (int i = 0; i < matA.getColumnDimension(); i++) {
            // beta*b
            double vi = beta * b.getEntry(i);
            for (int j = 0; j < matA.getRowDimension(); j++) {
                // A[T].a + (beta*b)
                vi += matA.getEntry(j, i) * a.getEntry(j);
            }
            ret.setEntry(i, vi);
        }

        return ret;
    }

    /**
     * Returns C = A + beta * B (linear combination). Useful in avoiding the need of the copy() in
     * the
     * colt api.
     * 
     * @param a
     *        matrix
     * @param b
     *        matrix
     * @param beta
     *        constant
     * @return C
     */
    public static final RealMatrix add(final RealMatrix a,
            final RealMatrix b,
            final double beta) {
        if (a.getRowDimension() != b.getRowDimension()
                || a.getColumnDimension() != b.getColumnDimension()) {
            // Check dimensions
            throw new IllegalArgumentException(WRONG_MAT_DIM);
        }
        // Operation in one pass
        final RealMatrix ret = new BlockRealMatrix(a.getRowDimension(), a.getColumnDimension());
        for (int i = 0; i < ret.getRowDimension(); i++) {
            for (int j = 0; j < ret.getColumnDimension(); j++) {
                ret.setEntry(i, j, a.getEntry(i, j) + beta * b.getEntry(i, j));
            }
        }
        // Return matrix
        return ret;
    }

    /**
     * Returns v = v1 + c * v2 (linear combination). Useful in avoiding the need of the copy() in
     * the
     * colt api.
     * 
     * @param v1
     *        vector
     * @param v2
     *        vector
     * @param c
     *        constant
     * @return v
     */
    public static final RealVector add(final RealVector v1,
            final RealVector v2,
            final double c) {
        if (v1.getDimension() != v2.getDimension()) {
            throw new IllegalArgumentException(WRONG_VECT_DIM);
        }
        final RealVector ret = new ArrayRealVector(v1.getDimension());
        for (int i = 0; i < ret.getDimension(); i++) {
            ret.setEntry(i, v1.getEntry(i) + c * v2.getEntry(i));
        }

        return ret;
    }

    /**
     * Return a new array with all the occurrences of oldValue replaced by
     * newValue.
     * 
     * @param v
     *        vector
     * @param oldValue
     *        value to be replaced
     * @param newValue
     *        value to replace with
     * @return new array
     */
    public static final RealVector replaceValues(final RealVector v,
            final double oldValue,
            final double newValue) {
        // If the vector is null, directly return null
        if (v == null) {
            return null;
        }
        final RealVector ret = new ArrayRealVector(v.getDimension());
        for (int i = 0; i < v.getDimension(); i++) {
            final double vi = v.getEntry(i);
            if (Double.compare(oldValue, vi) != 0) {
                // no substitution
                ret.setEntry(i, vi);
            } else {
                // Substitution
                ret.setEntry(i, newValue);
            }
        }
        return ret;
    }

    /**
     * Returns a lower and an upper bound for the condition number <br>
     * kp(A) = Norm[A, p] / Norm[A^-1, p] <br>
     * where <br>
     * Norm[A, p] = sup ( Norm[A.x, p]/Norm[x, p] , x !=0 ) <br>
     * for a matrix and <br>
     * Norm[x, 1] := Sum[MathLib.abs(x[i]), i] <br>
     * Norm[x, 2] := MathLib.sqrt(Sum[MathLib.pow(x[i], 2), i]) <br>
     * Norm[x, 00] := Max[MathLib.abs(x[i]), i] <br>
     * for a vector.
     * 
     * @param a
     *        matrix you want the condition number of
     * @param p
     *        norm order (2 or Integer.MAX_VALUE)
     * @return an array with the two bounds (lower and upper bound)
     * 
     * See Ravindra S. Gajulapalli, Leon S. Lasdon
     *      "Scaling Sparse Matrices for Optimization Algorithms"
     */
    public static double[] getConditionNumberRange(final RealMatrix a,
            final int p) {
        double infLimit = Double.NEGATIVE_INFINITY;
        final List<Double> columnNormsList = new ArrayList<>();
        switch (p) {
            case 2:
                for (int j = 0; j < a.getColumnDimension(); j++) {
                    columnNormsList.add(a.getColumnVector(j).getL1Norm());
                }
                Collections.sort(columnNormsList);
                // kp >= Norm[Ai, p]/Norm[Aj, p],
                // for each i, j = 0,1,...,n, Ak columns of A
                infLimit = columnNormsList.get(columnNormsList.size() - 1) / columnNormsList.get(0);
                break;

            case Integer.MAX_VALUE:
                final double normAInf = a.getNorm();
                for (int j = 0; j < a.getColumnDimension(); j++) {
                    columnNormsList.add(a.getColumnVector(j).getLInfNorm());
                }
                Collections.sort(columnNormsList);
                // k1 >= Norm[A, +oo]/min{ Norm[Aj, +oo],
                // for each j = 0,1,...,n }, Ak columns of A
                infLimit = normAInf / columnNormsList.get(0);
                break;

            default:
                // Error: p norm order != (2 or Integer.MAX_VALUE)
                throw new IllegalArgumentException("p must be 2 or Integer.MAX_VALUE");
        }
        return new double[] { infLimit, Double.POSITIVE_INFINITY };
    }

    /**
     * Given a symm matrix S that stores just its subdiagonal elements,
     * reconstructs the full symmetric matrix.
     * 
     * @param s
     *        matrix
     * @return full symmetric matrix
     */
    public static final RealMatrix fillSubdiagonalSymmetricMatrix(final RealMatrix s) {

        if (s.getRowDimension() != s.getColumnDimension()) {
            // dimension mismatch
            throw new IllegalArgumentException("Not square matrix");
        }
        /** full symmetric matrix */
        final RealMatrix sFull = new BlockRealMatrix(s.getRowDimension(), s.getRowDimension());

        for (int i = 0; i < s.getRowDimension(); i++) {
            for (int j = 0; j < i + 1; j++) {
                final double sij = s.getEntry(i, j);
                sFull.setEntry(i, j, sij);
                sFull.setEntry(j, i, sij);
            }
        }

        return sFull; // reconstructed full symmetric matrix
    }

    /**
     * Constructs a new diagonal matrix whose diagonal elements are the elements
     * of <tt>vector</tt>. Cells values are copied. The new matrix is not a
     * view.
     * 
     * @param vector
     *        with the diagonal values
     * @return a new matrix.
     */
    public static RealMatrix diagonal(final RealVector vector) {
        final int size = vector.getDimension();
        final RealMatrix diag = new BlockRealMatrix(size, size);
        for (int i = size - 1; i >= 0; i--) {
            diag.setEntry(i, i, vector.getEntry(i));
        }
        return diag;
    }

    /**
     * Return a vector with random values
     * 
     * @param dim
     *        dimension of the vector
     * @param min
     *        minimum value
     * @param max
     *        maximum value
     * @param seed
     *        of the random number generator
     * @return vector with random values
     */
    public static RealVector randomValuesVector(final int dim,
            final double min,
            final double max,
            final Long seed) {
        Random random = new Random();
        if (seed != null) {
            random = new Random(seed);
        }
        final double[] v = new double[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = min + random.nextDouble() * (max - min);
        }
        return new ArrayRealVector(v);
    }

    /**
     * Constructs a block matrix made from the given parts. All matrices of a
     * given column within <tt>parts</tt> must have the same number of columns.
     * All matrices of a given row within <tt>parts</tt> must have the same
     * number of rows. Otherwise an <tt>IllegalArgumentException</tt> is thrown.
     * Note that <tt>null</tt>s within <tt>parts[row,col]</tt> are an exception
     * to this rule: they are ignored. Cells are copied.
     * 
     * From https://github.com/kzn/colt/blob/master/src/cern/colt/matrix/
     * DoubleFactory2D.java Extracted 17/11/2020
     * @param parts matrices
     * @return build matrix.
     **/
    //CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: complex JOptimizer code kept as such
    public static RealMatrix composeMatrix(final RealMatrix[][] parts) {
        //CHECKSTYLE: resume CyclomaticComplexity check
        // checks if the given array is rectangular
        checkRectangularShape(parts);
        final int rows = parts.length; // number of rows
        int columns = 0;
        if (parts.length > 0) {
            columns = parts[0].length; // number of columns
        }

        // determine maximum column width of each column
        final int[] maxWidths = new int[columns];
        for (int column = columns - 1; column >= 0; column--) {
            int maxWidth = 0;
            for (int row = rows - 1; row >= 0; row--) {
                final RealMatrix part = parts[row][column];
                if (part != null) {
                    final int width = part.getColumnDimension();
                    // check that the different parts has the same number of columns
                    if (maxWidth > 0 && width > 0 && width != maxWidth) {
                        // error, different number of columns
                        throw new IllegalArgumentException("Different number of columns.");
                    }
                    maxWidth = MathLib.max(maxWidth, width);
                }
            }
            maxWidths[column] = maxWidth;
        }

        // determine row height of each row
        final int[] maxHeights = new int[rows];
        for (int row = rows - 1; row >= 0; row--) {
            int maxHeight = 0;
            for (int column = columns - 1; column >= 0; column--) {
                final RealMatrix part = parts[row][column];
                if (part != null) {
                    final int height = part.getRowDimension();
                    // check that the different parts has the same number of rows
                    if (maxHeight > 0 && height > 0 && height != maxHeight) {
                        // error, different number of rows
                        throw new IllegalArgumentException("Different number of rows.");
                    }
                    maxHeight = MathLib.max(maxHeight, height);
                }
            }
            maxHeights[row] = maxHeight;
        }

        // shape of result
        int resultRows = 0;
        for (int row = rows - 1; row >= 0; row--) {
            resultRows += maxHeights[row];
        }
        int resultCols = 0;
        for (int column = columns - 1; column >= 0; column--) {
            resultCols += maxWidths[column];
        }
        // create the final matrix
        final RealMatrix matrix = new BlockRealMatrix(resultRows, resultCols);

        // copy
        int r = 0;
        for (int row = 0; row < rows; row++) {
            int c = 0;
            for (int column = 0; column < columns; column++) {
                final RealMatrix part = parts[row][column];
                if (part != null) {
                    matrix.setSubMatrix(part.getData(false), r, c);

                }
                c += maxWidths[column];
            }
            r += maxHeights[row];
        }

        return matrix;
    }

    /**
     * Checks whether the given array is rectangular, that is, whether all rows
     * have the same number of columns.
     * @param array array
     * @throws IllegalArgumentException
     *         if the array is not rectangular.
     */
    public static void checkRectangularShape(final RealMatrix[][] array) {
        int columns = -1;
        for (int row = array.length - 1; row >= 0; row--) {
            if (array[row] != null) {
                if (columns == -1) {
                    columns = array[row].length;
                }
                if (array[row].length != columns) {
                    throw new IllegalArgumentException(
                            "All rows of array must have same number of columns.");
                }
            }
        }
    }
}
