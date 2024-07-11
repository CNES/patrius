/**
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
 * 
 * @history created 24/01/13
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Math adapter class.
 * 
 * @concurrency unconditionally thread-safe
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public final class JavaMathAdapter {

    static {
        // Bogus instance for code coverage only
        final JavaMathAdapter bogus = new JavaMathAdapter();
        bogus.hashCode();
    }

    /**
     * Hidden constructor.
     */
    private JavaMathAdapter() {
    }

    /**
     * Computes "x" modulo "mod".
     * 
     * @param x
     *        value to modulate
     * @param mod
     *        modulo (for instance &pi;)
     * @return "x" modulo "mod"
     */
    public static double mod(final double x, final double mod) {
        return (((x % mod) + mod) % mod);
    }

    /**
     * Compute the Binomial Coefficient, "a choose b", the number of b-element subsets that can be selected from an
     * a-element set. This formula can be used with negative a values.
     * 
     * @param a
     *        the size of the set
     * @param b
     *        the size of the subsets
     * @return a choose b
     */
    public static double binomialCoefficientGeneric(final int a, final int b) {

        // Initialization
        double res = 0;
        if (b < 0) {
            res = 0;
        } else if (b == 0) {
            res = 1;
        } else {
            // Generic case: k > 0
            res = 1;
            for (int i = 1; i <= b; i++) {
                res *= (double) (a - b + i) / i;
            }
        }

        // Return result
        return res;
    }

    /**
     * Multiply an automatically-generated-3-dimensional matrix with a vector.<br >
     * Automatically generated 3D matrices have their rows and wideness inverted. This method corrects it.
     * 
     * @param mat
     *        the 3-dimensional matrix
     * @param vect
     *        the vector
     * @return product, a 2-dimensional matrix
     * @throws PatriusException
     *         thrown if matrix dimension mismatch
     */
    public static double[][] threeDMatrixVectorMultiply(final double[][][] mat,
                                                        final double[] vect) throws PatriusException {
        // initializations
        final int col = mat[0].length;

        if (col != vect.length) {
            // Dimension mismatch
            throw new PatriusException(PatriusMessages.INVALID_ARRAY_LENGTH);
        }

        final int row = mat.length;
        final int wide = mat[0][0].length;

        // initialize the resulting 2-dimensional matrix
        final double[][] res = new double[wide][row];

        // Compute multiplication
        double sum;
        for (int k = 0; k < wide; k++) {
            for (int i = 0; i < row; i++) {
                sum = 0;
                for (int j = 0; j < col; j++) {
                    sum += mat[i][j][k] * vect[j];
                }
                res[k][i] = sum;
            }
        }

        // Return result
        return res;
    }

    /**
     * Invert a vector.
     * 
     * @param v
     *        vector
     * @return -v
     */
    public static double[] negate(final double[] v) {

        final double[] res = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            res[i] = -v[i];
        }

        return res;
    }

    /**
     * Add 2 matrices.
     * 
     * @param m1
     *        first matrix
     * @param m2
     *        second matrix
     * @return sum of the two matrices
     * @throws PatriusException
     *         thrown if matrix dimension mismatch
     */
    public static double[][] matrixAdd(final double[][] m1, final double[][] m2) throws PatriusException {
        if (m1[0].length != m2[0].length) {
            // Dimension mismatch
            throw new PatriusException(PatriusMessages.INVALID_ARRAY_LENGTH);
        }
        if (m1.length != m2.length) {
            // Dimension mismatch
            throw new PatriusException(PatriusMessages.INVALID_ARRAY_LENGTH);
        }

        // Addition
        final double[][] res = new double[m1.length][m1[0].length];
        for (int i = 0; i < m1.length; i++) {
            for (int j = 0; j < m1[0].length; j++) {
                res[i][j] = m1[i][j] + m2[i][j];
            }
        }

        // Return result
        return res;
    }

    /**
     * Copy a vector into a matrix, column per column.
     * 
     * @param vector
     *        vector to be copied
     * @param matrix
     *        matrix where to put the data
     */
    public static void vectorToMatrix(final double[] vector, final double[][] matrix) {
        // Initialization
        final int col = matrix[0].length;
        final int row = matrix.length;

        if (row * col < vector.length) {
            // Dimension mismatch
            throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE, vector.length, row * col);
        }

        // Computation
        // Argument is directly initialized
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                matrix[i][j] = vector[i + j * row];
            }
        }
    }

    /**
     * Copy a matrix into a vector, column per column.
     * 
     * @param matrix
     *        matrix from which the data is read
     * @param vector
     *        vector in which the data is put
     * @param offset
     *        offset after which the data will be put in the vector thrown if dimensions mismatch
     */
    public static void matrixToVector(final double[][] matrix, final double[] vector, final int offset) {
        final int col = matrix[0].length;
        final int row = matrix.length;

        if (vector.length < row * col + offset) {
            // Dimension mismatch
            throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE, vector.length, row * col
                + offset);
        }

        for (int i = 0; i < row * col; i++) {
            vector[offset + i] = matrix[i % row][i / row];
        }
    }

    /**
     * Multiply 2 matrix.
     * 
     * @param m1
     *        first Matrix
     * @param m2
     *        second matrix
     * @return the products
     */
    public static double[][] matrixMultiply(final double[][] m1, final double[][] m2) {

        // Check
        if (m1[0].length != m2.length) {
            // Dimension mismatch
            throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE, m1[0].length, m2.length);
        }

        // Homemade routine to save computation time
        
        // Initialization
        final double[][] res = new double[m1.length][m2[0].length];
        // Computation
        double sum;
        for (int i = 0; i < m1.length; i++) {
            for (int j = 0; j < m2[0].length; j++) {
                sum = 0;
                for (int k = 0; k < m2.length; k++) {
                    sum += m1[i][k] * m2[k][j];
                }
                res[i][j] = sum;
            }
        }
        // Return result
        return res;
    }

    /**
     * Transpose a matrix.
     * 
     * @param m
     *        the matrix.
     * @return Matrix transposed.
     */
    public static double[][] matrixTranspose(final double[][] m) {

        // Homemade routine to save computation time
        final double[][] res = new double[m[0].length][m.length];

        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                res[j][i] = m[i][j];
            }
        }

        return res;
    }

    /**
     * Multiply matrix with a vector.
     * 
     * @param m
     *        the matrix
     * @param v
     *        the vector
     * @return Products with matrix and vector
     */
    public static double[] matrixVectorMultiply(final double[][] m, final double[] v) {

        // Check
        if (m[0].length != v.length) {
            // Dimension mismatch
            throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE, m[0].length, v.length);
        }

        // Homemade routine to save computation time
        final double[] res = new double[m.length];
        
        // Initialization
        double sum;
        // Computation
        for (int i = 0; i < m.length; i++) {
            sum = 0;
            for (int k = 0; k < v.length; k++) {
                sum += m[i][k] * v[k];
            }
            res[i] = sum;
        }

        // Return result
        return res;
    }

    /**
     * Return coef * matrix.
     * 
     * @param coef
     *        a coefficent
     * @param matrix
     *        a matrix
     * @return a matrix resulting from the operation coef * matrix.
     */
    public static double[][] scalarMultiply(final double coef, final double[][] matrix) {
        final double[][] res = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                res[i][j] = coef * matrix[i][j];
            }

        }
        return res;
    }
}
