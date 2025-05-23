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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;


/**
 * Interface handling decomposition algorithms that can solve A &times; X = B.
 * <p>
 * Decomposition algorithms decompose an A matrix has a product of several specific matrices from which they can solve A
 * &times; X = B in least squares sense: they find X such that ||A &times; X - B|| is minimal.
 * </p>
 * <p>
 * Some solvers like {@link LUDecomposition} can only find the solution for square matrices and when the solution is an
 * exact linear solution, i.e. when ||A &times; X - B|| is exactly 0. Other solvers can also find solutions with
 * non-square matrix A and with non-null minimal norm. If an exact linear solution exists it is also the minimal norm
 * solution.
 * </p>
 * 
 * @version $Id: DecompositionSolver.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public interface DecompositionSolver {

    /**
     * Solve the linear equation A &times; X = B for matrices A.
     * <p>
     * The A matrix is implicit, it is provided by the underlying decomposition algorithm.
     * </p>
     * 
     * @param b
     *        right-hand side of the equation A &times; X = B
     * @return a vector X that minimizes the two norm of A &times; X - B
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if the matrices dimensions do not match.
     * @throws SingularMatrixException
     *         if the decomposed matrix is singular.
     */
    RealVector solve(final RealVector b);

    /**
     * Solve the linear equation A &times; X = B for matrices A.
     * <p>
     * The A matrix is implicit, it is provided by the underlying decomposition algorithm.
     * </p>
     * 
     * @param b
     *        right-hand side of the equation A &times; X = B
     * @return a matrix X that minimizes the two norm of A &times; X - B
     * @throws fr.cnes.sirius.patrius.math.exception.DimensionMismatchException
     *         if the matrices dimensions do not match.
     * @throws SingularMatrixException
     *         if the decomposed matrix is singular.
     */
    RealMatrix solve(final RealMatrix b);

    /**
     * Check if the decomposed matrix is non-singular.
     * 
     * @return true if the decomposed matrix is non-singular.
     */
    boolean isNonSingular();
    
    /**
     * Get the inverse (or pseudo-inverse) of the decomposed matrix.
     * 
     * @return inverse matrix
     * @throws SingularMatrixException
     *         if the decomposed matrix is singular.
     */
    RealMatrix getInverse();
    
}
