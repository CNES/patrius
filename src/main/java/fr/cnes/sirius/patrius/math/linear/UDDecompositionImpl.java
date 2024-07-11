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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Calculates the UD decomposition of a matrix.
 * <p>
 * The UD-decomposition of matrix A is a set of three matrices: U, D and U<sup>t</sup> such that A =
 * U&times;D&times;U<sup>t</sup>. U is a upper triangular matrix and D is an diagonal matrix.
 * </p>
 * 
 * See Flavien Mercier (DCT/SB/OR)
 * 
 * @concurrency immutable
 * 
 * @author Denis Claude, Julie Anton
 * 
 * @version $Id: UDDecompositionImpl.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class UDDecompositionImpl implements UDDecomposition, Decomposition {

    /**
     * Default threshold above which off-diagonal elements are considered too different and matrix not symmetric.
     */
    public static final double DEFAULT_RELATIVE_SYMMETRY_THRESHOLD = 1.0e-15;
    /**
     * Default threshold below which diagonal elements are considered null and matrix not positive definite.
     */
    public static final double DEFAULT_ABSOLUTE_POSITIVITY_THRESHOLD = 0.0;

    /** Row-oriented storage for matrix data. */
    private double[][] data;
    /** Cached value of U. */
    private RealMatrix cachedU;
    /** Cached value of D. */
    private RealMatrix cachedD;
    /** Threshold above which off-diagonal elements are considered too different and matrix not symmetric. */
    private final double relativeSymmetryThreshold;
    /** Threshold below which diagonal elements are considered null and matrix not positive definite. */
    private final double absolutePositivityThreshold;

    /**
     * Calling this constructor is equivalent to call {@link #UDDecompositionImpl(RealMatrix, double, double)} with the
     * thresholds set to the default values {@link #DEFAULT_RELATIVE_SYMMETRY_THRESHOLD} and
     * {@link #DEFAULT_ABSOLUTE_POSITIVITY_THRESHOLD}.
     * 
     * <p>
     * The decomposition is directly computed on the input matrix.
     * </p>
     * 
     * @param matrix
     *        The matrix to decompose.
     * @see #UDDecompositionImpl(RealMatrix, double, double)
     * @see #DEFAULT_RELATIVE_SYMMETRY_THRESHOLD
     * @see #DEFAULT_ABSOLUTE_POSITIVITY_THRESHOLD
     * @since 1.0
     */
    public UDDecompositionImpl(final RealMatrix matrix) {
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
     *        threshold above which off-diagonal elements are considered too different and matrix not symmetric
     * @param absolutePositivityThreshold
     *        threshold below which diagonal elements are considered null and matrix not positive definite
     * @since 1.0
     */
    public UDDecompositionImpl(final RealMatrix matrix, final double relativeSymmetryThreshold,
                               final double absolutePositivityThreshold) {
        this.relativeSymmetryThreshold = relativeSymmetryThreshold;
        this.absolutePositivityThreshold = absolutePositivityThreshold;
        udDecompose(matrix);
    }

    /**
     * Calculates the UD-decomposition of the given matrix.
     * 
     * @param realMatrix
     *        The matrix to decompose.
     */
    private void udDecompose(final RealMatrix realMatrix) {
        // Sanity checks
        final Array2DRowRealMatrix matrix = (Array2DRowRealMatrix) realMatrix;

        // tests if the matrix is square
        if (!matrix.isSquare()) {
            throw new NonSquareMatrixException(matrix.getRowDimension(), matrix.getColumnDimension());
        }
        // tests if the matrix is symmetric
        if (!matrix.isSymmetric(relativeSymmetryThreshold)) {
            throw new NonSymmetricMatrixException(matrix.getRowDimension(), matrix.getColumnDimension(),
                relativeSymmetryThreshold);
        }

        double sigma = 0.0;
        final int order = matrix.getRowDimension();

        // Data initialization
        this.data = matrix.getData();
        this.cachedU = MatrixUtils.createRealMatrix(order, order);
        this.cachedD = MatrixUtils.createRealMatrix(order, order);

        // Decomposition routine
        for (int j = order - 1; j > -1; --j) {

            for (int i = j; i > -1; --i) {
                sigma = this.data[i][j];

                for (int k = j + 1; k < order; ++k) {
                    sigma -= this.cachedU.getEntry(i, k) * this.cachedD.getEntry(k, k) * this.cachedU.getEntry(j, k);
                }

                // Diagonal terms
                if (i == j) {
                    // tests if the matrix is positive definite
                    if (sigma < absolutePositivityThreshold) {
                        throw new NonPositiveDefiniteMatrixException(sigma, j, absolutePositivityThreshold);
                    }
                    this.cachedD.setEntry(j, j, sigma);
                    this.cachedU.setEntry(j, j, 1.0);
                } else {
                    // Non-diagonal terms
                    this.cachedU.setEntry(i, j, MathLib.divide(sigma, this.cachedD.getEntry(j, j)));
                }
            }
        }
    }

    /**
     * Get the U matrix
     * 
     * @precondition
     * 
     * @return RealMatrix
     * 
     * @since 1.0
     */
    @Override
    public RealMatrix getU() {
        return this.cachedU.copy();
    }

    /**
     * get the D matrix
     * 
     * @precondition
     * 
     * @return RealMatrix
     * 
     * @since 1.0
     */
    @Override
    public RealMatrix getD() {
        return this.cachedD.copy();
    }

    /**
     * get the UT matrix
     * 
     * @precondition
     * 
     * @return RealMatrix
     * 
     * @since 1.0
     */
    @Override
    public RealMatrix getUT() {
        return this.cachedU.transpose();
    }

    /**
     * get the determinant
     * 
     * @return double
     * 
     * @since 1.0
     */
    @Override
    public double getDeterminant() {
        double determinant = 1.0;
        for (int i = 0; i < this.data.length; ++i) {
            final double ii = this.cachedU.getEntry(i, i);
            determinant *= ii * ii * this.cachedD.getEntry(i, i);
        }
        return determinant;
    }

    /**
     * @description gives the solver based on the UD decomposition
     * @return the UD solver
     * @see UDDecomposition#getSolver()
     */
    @Override
    public DecompositionSolver getSolver() {
        return new Solver(this.cachedU, this.cachedD);
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
    public static Function<RealMatrix, Decomposition> decompositionBuilder (final double relativeSymmetryThreshold,
            final double absolutePositivityThreshold) {
        return (realMatrix) -> new UDDecompositionImpl(realMatrix, 
                relativeSymmetryThreshold, absolutePositivityThreshold);
    }

    /** Specialized solver. */
    private static final class Solver implements DecompositionSolver {
        /** U matrix data. */
        private final double[][] uData;
        /** D matrix data. */
        private final double[][] dData;

        /**
         * Build a solver from decomposed matrix.
         * 
         * @param matrixU
         *        U matrix
         * @param matrixD
         *        D matrix
         */
        private Solver(final RealMatrix matrixU, final RealMatrix matrixD) {
            this.uData = matrixU.getData(false);
            this.dData = matrixD.getData(false);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isNonSingular() {
            // if we get this far, the matrix was positive definite, hence non-singular
            return true;
        }

        /**
         * @param b
         *        double[]
         * @return double[]
         */
        public double[] solve(final double[] b) {
            final int m = this.uData.length;
            if (b.length != m) {
                throw new DimensionMismatchException(b.length, m);
            }

            final double[] x = b.clone();

            this.solveVector(m, x);

            return x;
        }

        /**
         * Solve the linear equation A &times; X = B.
         * <p>
         * The A matrix is implicit here (UD decomposition)
         * </p>
         * 
         * @param m
         *        length of U matrix
         * @param x
         *        vector X
         */
        private void solveVector(final int m, final double[] x) {

            // Solve UZ = b
            for (int j = m - 1; j >= 0; j--) {
                // divide x values by corresponding diagonal value in U matrix
                x[j] /= this.uData[j][j];
                final double xJ = x[j];
                for (int i = j - 1; i >= 0; i--) {
                    x[i] -= xJ * this.uData[i][j];
                }
            }

            // Solve DY=Z
            for (int j = 0; j < m; j++) {
                // divide x values by corresponding diagonal value in D matrix
                x[j] /= this.dData[j][j];
            }

            // Solve UTX = Y
            for (int j = 0; j < m; j++) {
                // divide x values by corresponding diagonal value in U matrix
                x[j] /= this.uData[j][j];
                final double xJ = x[j];
                for (int i = j + 1; i < m; i++) {
                    x[i] -= xJ * this.uData[j][i];
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.PreserveStackTrace")
        public RealVector solve(final RealVector b) {
            try {
                return this.solve((ArrayRealVector) b);
            } catch (final ClassCastException cce) {

                final int m = this.uData.length;
                if (b.getDimension() != m) {
                    throw new DimensionMismatchException(b.getDimension(), m);
                }

                final double[] x = b.toArray();

                this.solveVector(m, x);
                return new ArrayRealVector(x, false);
            }
        }

        /**
         * Solve the linear equation A &times; X = B.
         * <p>
         * The A matrix is implicit here.
         * 
         * @param b
         *        right-hand side of the equation A &times; X = B
         * @return a vector X such that A &times; X = B
         * @throws DimensionMismatchException
         *         if the matrices dimensions do not match.
         * @throws SingularMatrixException
         *         if the decomposed matrix is singular.
         */
        public ArrayRealVector solve(final ArrayRealVector b) {
            return new ArrayRealVector(this.solve(b.getDataRef()), false);
        }

        /**
         * Solve the linear equation A &times; X = B for matrices A.
         * <p>
         * The A matrix is implicit, it is provided by the underlying decomposition algorithm.
         * </p>
         * 
         * @param b
         *        right-hand side of the equation A &times; X = B
         * @param reuseB
         *        if true, the b array will be reused and returned, instead of being copied
         * @return a matrix X that minimises the two norm of A &times; X - B
         *         if the matrices dimensions do not match.
         * @throws SingularMatrixException
         *         if the decomposed matrix is singular.
         */
        private double[][] solve(final double[][] b, final boolean reuseB) {
            final int m = this.uData.length;
            if (b.length != m) {
                // the B and U dimensions do not match:
                throw new DimensionMismatchException(b.length, m);
            }

            final int nColB = b[0].length;
            final double[][] x;
            if (reuseB) {
                x = b;
            } else {
                x = new double[b.length][nColB];
                for (int i = 0; i < b.length; ++i) {
                    // copy the b matrix in x:
                    System.arraycopy(b[i], 0, x[i], 0, nColB);
                }
            }
            // solve the linear equation:
            this.solveMatrix(x, m, nColB);

            return x;
        }

        /**
         * Solve the linear equation A &times; X = B for matrices.
         * <p>
         * The A matrix is implicit here (UD decomposition)
         * </p>
         * 
         * @param m
         *        length of U matrix
         * @param x
         *        vector X
         * @param nColB
         *        length of b[0]
         */
        private void solveMatrix(final double[][] x, final int m, final int nColB) {

            // Solve UZ = b
            for (int j = m - 1; j >= 0; j--) {
                // Loop on all rows of x
                final double[] xJ = x[j];
                for (int k = 0; k < nColB; ++k) {
                    // Loop on all elements of current row and divide by corresponding
                    // diagonal element in uData
                    xJ[k] /= this.uData[j][j];
                }
                for (int i = j - 1; i >= 0; i--) {
                    final double[] xI = x[i];
                    for (int k = 0; k < nColB; ++k) {
                        xI[k] -= xJ[k] * this.uData[i][j];
                    }
                }
            }

            // Solve DY=Z
            this.solveDYEqualZ(x, m, nColB);

            // Solve UTX = Y
            this.solveUTXEqualY(x, m, nColB);
        }

        /**
         * Solves DY=Z
         * 
         * @param m
         *        length of U matrix
         * @param x
         *        vector X
         * @param nColB
         *        length of b[0]
         */
        private void solveDYEqualZ(final double[][] x, final int m, final int nColB) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < nColB; ++k) {
                    x[j][k] /= this.dData[j][j];
                }
            }
        }

        /**
         * Solves UTX = Y
         * 
         * @param m
         *        length of U matrix
         * @param x
         *        vector X
         * @param nColB
         *        length of b[0]
         */
        private void solveUTXEqualY(final double[][] x, final int m, final int nColB) {
            // Loop on all rows of x
            for (int j = 0; j < m; j++) {
                final double[] xJ = x[j];
                for (int k = 0; k < nColB; ++k) {
                    // Loop on all elements of current row and divide by corresponding
                    // diagonal element in uData
                    xJ[k] /= this.uData[j][j];
                }
                // Loop on all rows after the current one
                for (int i = j + 1; i < m; i++) {
                    final double[] xI = x[i];
                    for (int k = 0; k < nColB; ++k) {
                        xI[k] -= xJ[k] * this.uData[j][i];
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public RealMatrix solve(final RealMatrix b) {
            return new Array2DRowRealMatrix(this.solve(b.getData(), true), false);
        }

        /** {@inheritDoc} */
        @Override
        public RealMatrix getInverse() {
            return this.solve(MatrixUtils.createRealIdentityMatrix(this.uData.length, true));
        }

    }
    
}
