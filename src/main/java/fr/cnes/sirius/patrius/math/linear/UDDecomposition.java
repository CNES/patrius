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
 * @history creation 10/08/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

/**
 * An interface to classes that implement an algorithm to calculate the UD-decomposition of a real matrix.
 * <p>
 * The UD-decomposition of matrix A is a set of three matrices: U, D and U<sup>t</sup> such that A =
 * U&times;D&times;U<sup>t</sup>. U is a upper triangular matrix and D is an diagonal matrix.
 * </p>
 * 
 * <p>
 * - The matrix A must be a symmetric matrix and positive definite
 * </p>
 * See DV_MATHS_270.
 * 
 * @author Denis Claude
 * 
 * @version $Id: UDDecomposition.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public interface UDDecomposition {
    /**
     * Returns the matrix U of the decomposition.
     * <p>
     * U is an upper-triangular matrix
     * </p>
     * 
     * @return the U matrix
     */
    RealMatrix getU();

    /**
     * Returns the transpose of the matrix U of the decomposition.
     * <p>
     * U<sup>T</sup> is an lower-triangular matrix
     * </p>
     * 
     * @return the transpose of the matrix U of the decomposition
     */
    RealMatrix getUT();

    /**
     * Returns the matrix D of the decomposition.
     * <p>
     * D is an diagonal matrix
     * </p>
     * 
     * @return the D matrix
     */
    RealMatrix getD();

    /**
     * Return the determinant of the matrix
     * 
     * @return determinant of the matrix
     */
    double getDeterminant();

    /**
     * Get a solver of the linear equation A &times; X = B for matrices A.
     * 
     * @return a solver
     */
    DecompositionSolver getSolver();
}
