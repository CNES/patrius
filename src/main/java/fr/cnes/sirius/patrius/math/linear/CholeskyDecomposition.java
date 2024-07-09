/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
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

import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code kept as such

/**
 * Calculates the Cholesky decomposition of a matrix.
 * <p>
 * The Cholesky decomposition of a real symmetric positive-definite matrix A consists of a lower triangular matrix L
 * with same size such that: A = LL<sup>T</sup>. In a sense, this is the square root of A.
 * </p>
 * <p>
 * This class is based on the class with similar name from the <a
 * href="http://math.nist.gov/javanumerics/jama/">JAMA</a> library, with the following changes:
 * </p>
 * <ul>
 * <li>a {@link #getLT() getLT} method has been added,</li>
 * <li>the {@code isspd} method has been removed, since the constructor of this class throws a
 * {@link NonPositiveDefiniteMatrixException} when a matrix cannot be decomposed,</li>
 * <li>a {@link #getDeterminant() getDeterminant} method has been added,</li>
 * <li>the {@code solve} method has been replaced by a {@link #getSolver()
 * getSolver} method and the equivalent method provided by the returned {@link DecompositionSolver}.</li>
 * </ul>
 * 
 * @see <a href="http://mathworld.wolfram.com/CholeskyDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/Cholesky_decomposition">Wikipedia</a>
 * @version $Id: CholeskyDecomposition.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0 (changed to concrete class in 3.0)
 */
@SuppressWarnings("PMD.NullAssignment")
public class CholeskyDecomposition implements Decomposition {
    /**
     * Default threshold above which off-diagonal elements are considered too different
     * and matrix not symmetric.
     */
    public static final double DEFAULT_RELATIVE_SYMMETRY_THRESHOLD = 1.0e-15;
    /**
     * Default threshold below which diagonal elements are considered null
     * and matrix not positive definite.
     */
    public static final double DEFAULT_ABSOLUTE_POSITIVITY_THRESHOLD = 1.0e-10;
    /** Row-oriented storage for L<sup>T</sup> matrix data. */
    private double[][] lTData;
    /** Cached value of L. */
    private RealMatrix cachedL;
    /** Cached value of LT. */
    private RealMatrix cachedLT;
    /** Threshold above which off-diagonal elements are considered too different and matrix not symmetric. */
    private final double relativeSymmetryThreshold;
    /** Threshold below which diagonal elements are considered null and matrix not positive definite. */
    private final double absolutePositivityThreshold;

    /**
     * Calling this constructor is equivalent to call {@link #CholeskyDecomposition(RealMatrix, double, double)} with
     * the thresholds set to the default values {@link #DEFAULT_RELATIVE_SYMMETRY_THRESHOLD} and
     * {@link #DEFAULT_ABSOLUTE_POSITIVITY_THRESHOLD}
     * 
     * <p>
     * The decomposition is directly computed on the input matrix.
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     * @throws NonSquareMatrixException
     *         if the matrix is not square.
     * @throws NonSymmetricMatrixException
     *         if the matrix is not symmetric.
     * @throws NonPositiveDefiniteMatrixException
     *         if the matrix is not
     *         strictly positive definite.
     * @see #DEFAULT_RELATIVE_SYMMETRY_THRESHOLD
     * @see #DEFAULT_ABSOLUTE_POSITIVITY_THRESHOLD
     * @see #CholeskyDecomposition(double, double)
     */
    public CholeskyDecomposition(final RealMatrix matrix) {
        this(matrix, DEFAULT_RELATIVE_SYMMETRY_THRESHOLD, DEFAULT_ABSOLUTE_POSITIVITY_THRESHOLD);
    }

    /**
     * Constructor used to set the relative & absolute thresholds.
     * 
     * <p>
     * The decomposition is directly computed on the input matrix.
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     * @param relativeSymmetryThreshold
     *        threshold above which off-diagonal
     *        elements are considered too different and matrix not symmetric
     * @param absolutePositivityThreshold
     *        threshold below which diagonal
     *        elements are considered null and matrix not positive definite
     * @throws NonSquareMatrixException
     *         if the matrix is not square.
     * @throws NonSymmetricMatrixException
     *         if the matrix is not symmetric.
     * @throws NonPositiveDefiniteMatrixException
     *         if the matrix is not
     *         strictly positive definite.
     */
    public CholeskyDecomposition(final RealMatrix matrix, final double relativeSymmetryThreshold,
                                 final double absolutePositivityThreshold) {
        this.relativeSymmetryThreshold = relativeSymmetryThreshold;
        this.absolutePositivityThreshold = absolutePositivityThreshold;
        choleskyDecompose(matrix);
    }

    /**
     * Calculates the Cholesky decomposition of the given matrix.
     * 
     * @param matrix
     *        The matrix to decompose.
     **/
    private void choleskyDecompose(final RealMatrix matrix) {
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(),
                matrix.getColumnDimension());
        }

        this.cachedL = null;
        this.cachedLT = null;
        final int order = matrix.getRowDimension();

        // DiagonalMatrix optimization - ltData is built will each sqrt root value of the diagonal matrix
        if (matrix instanceof DiagonalMatrix) {

            final double[][] lTDataTemp = new double[order][order];
            for (int i = 0; i < order; i++) {
                final double entry = matrix.getEntry(i, i);
                if (entry <= absolutePositivityThreshold) {
                    throw new NonPositiveDefiniteMatrixException(entry, i, absolutePositivityThreshold);
                }
                lTDataTemp[i][i] = MathLib.sqrt(entry);
            }
            this.lTData = lTDataTemp;

        } else {

            this.lTData = matrix.getData();

            // check the matrix before transformation
            for (int i = 0; i < order; ++i) {
                final double[] lI = this.lTData[i];

                // check off-diagonal elements (and reset them to 0)
                for (int j = i + 1; j < order; ++j) {
                    final double[] lJ = this.lTData[j];
                    final double lIJ = lI[j];
                    final double lJI = lJ[i];
                    final double maxDelta =
                        relativeSymmetryThreshold * MathLib.max(MathLib.abs(lIJ), MathLib.abs(lJI));
                    if (MathLib.abs(lIJ - lJI) > maxDelta) {
                        throw new NonSymmetricMatrixException(i, j, relativeSymmetryThreshold);
                    }
                    lJ[i] = 0;
                }
            }

            // transform the matrix
            for (int i = 0; i < order; ++i) {

                final double[] ltI = this.lTData[i];

                // check diagonal element
                if (ltI[i] <= absolutePositivityThreshold) {
                    throw new NonPositiveDefiniteMatrixException(ltI[i], i, absolutePositivityThreshold);
                }

                ltI[i] = MathLib.sqrt(ltI[i]);
                final double inverse = 1.0 / ltI[i];

                for (int q = order - 1; q > i; --q) {
                    ltI[q] *= inverse;
                    final double[] ltQ = this.lTData[q];
                    for (int p = q; p < order; ++p) {
                        ltQ[p] -= ltI[q] * ltI[p];
                    }
                }
            }
        }
    }

    /**
     * Returns the matrix L of the decomposition.
     * <p>
     * L is an lower-triangular matrix
     * </p>
     * 
     * @return the L matrix
     */
    public RealMatrix getL() {
        this.cachedL = this.getLT().transpose();
        return this.cachedL;
    }

    /**
     * Returns the transpose of the matrix L of the decomposition.
     * <p>
     * L<sup>T</sup> is an upper-triangular matrix
     * </p>
     * 
     * @return the transpose of the matrix L of the decomposition
     */
    public RealMatrix getLT() {
        this.cachedLT = MatrixUtils.createRealMatrix(this.lTData);
        return this.cachedLT;
    }

    /**
     * Return the determinant of the matrix
     * 
     * @return determinant of the matrix
     */
    public double getDeterminant() {
        double determinant = 1.0;
        for (int i = 0; i < this.lTData.length; ++i) {
            final double lTii = this.lTData[i][i];
            determinant *= lTii * lTii;
        }
        return determinant;
    }

    /** {@inheritDoc} */
    @Override
    public DecompositionSolver getSolver() {
        return new Solver(this.lTData);
    }
    
    /**
     * Builder for decomposition.
     * 
     * @param relativeSymmetryThreshold
     *        threshold above which off-diagonal elements are considered too different and matrix not symmetric
     * @param absolutePositivityThreshold
     *        threshold below which diagonal elements are considered null and matrix not positive definite
     * @return decomposition
     */
    public static Function<RealMatrix, Decomposition> decompositionBuilder(final double relativeSymmetryThreshold, 
            final double absolutePositivityThreshold) {
        return (realMatrix) -> new CholeskyDecomposition(realMatrix, relativeSymmetryThreshold, 
                absolutePositivityThreshold);
    }

    /** Specialized solver. */
    private static final class Solver implements DecompositionSolver {

        /** Row-oriented storage for L<sup>T</sup> matrix data. */
        private final double[][] lTData;

        /**
         * Build a solver from decomposed matrix.
         * 
         * @param lTDataIn
         *        row-oriented storage for L<sup>T</sup> matrix data
         */
        private Solver(final double[][] lTDataIn) {
            this.lTData = lTDataIn;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isNonSingular() {
            // if we get this far, the matrix was positive definite, hence non-singular
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public RealVector solve(final RealVector b) {
            final int m = this.lTData.length;
            if (b.getDimension() != m) {
                // Exception
                throw new DimensionMismatchException(b.getDimension(), m);
            }

            final double[] x = b.toArray();

            // Solve LY = b
            for (int j = 0; j < m; j++) {
                final double[] lJ = this.lTData[j];
                x[j] /= lJ[j];
                final double xJ = x[j];
                for (int i = j + 1; i < m; i++) {
                    x[i] -= xJ * lJ[i];
                }
            }

            // Solve LTX = Y
            for (int j = m - 1; j >= 0; j--) {
                x[j] /= this.lTData[j][j];
                final double xJ = x[j];
                for (int i = 0; i < j; i++) {
                    x[i] -= xJ * this.lTData[i][j];
                }
            }

            // Return result
            return new ArrayRealVector(x, false);
        }

        /** {@inheritDoc} */
        @Override
        public RealMatrix solve(final RealMatrix b) {

            // Check
            final int m = this.lTData.length;
            if (b.getRowDimension() != m) {
                // Exception
                throw new DimensionMismatchException(b.getRowDimension(), m);
            }

            final int nColB = b.getColumnDimension();
            final double[][] x = b.getData();

            // Solve LY = b
            for (int j = 0; j < m; j++) {
                final double[] lJ = this.lTData[j];
                final double lJJ = lJ[j];
                final double[] xJ = x[j];
                for (int k = 0; k < nColB; ++k) {
                    xJ[k] /= lJJ;
                }
                for (int i = j + 1; i < m; i++) {
                    final double[] xI = x[i];
                    final double lJI = lJ[i];
                    for (int k = 0; k < nColB; ++k) {
                        xI[k] -= xJ[k] * lJI;
                    }
                }
            }

            // Solve LTX = Y
            for (int j = m - 1; j >= 0; j--) {
                final double lJJ = this.lTData[j][j];
                final double[] xJ = x[j];
                for (int k = 0; k < nColB; ++k) {
                    xJ[k] /= lJJ;
                }
                for (int i = 0; i < j; i++) {
                    final double[] xI = x[i];
                    final double lIJ = this.lTData[i][j];
                    for (int k = 0; k < nColB; ++k) {
                        xI[k] -= xJ[k] * lIJ;
                    }
                }
            }

            // Return result
            return new Array2DRowRealMatrix(x, false);
        }
        
        /** {@inheritDoc} */
        @Override
        public RealMatrix getInverse() {
            return this.solve(MatrixUtils.createRealIdentityMatrix(this.lTData.length, true));
        }

    }

    // CHECKSTYLE: resume CommentRatio check
}
