/**
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.CholeskyDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.joptimizer.util.Utils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Cholesky L.L[T] factorization and inverse for symmetric and positive matrix:
 * 
 * Q = L.L[T], L lower-triangular
 * 
 * Just the subdiagonal elements of Q are used.
 * 
 * <p>
 * The main difference with {@link CholeskyDecomposition} is that this implementation contains a
 * rescaler {@link MatrixRescaler} in order to cope with badly conditioned matrices.
 * </p>
 * 
 * @author <a href="mailto:alberto.trivellato@gmail.com">alberto trivellato</a>
 * HISTORY
* VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 * 
 * @since 4.6
 */
public final class CholeskyFactorization {

    /** Dimension */
    private int dim;
    /** Matrix Q */
    private RealMatrix q;
    /** Rescaler */
    private MatrixRescaler rescaler = null;
    /** The rescaling factor */
    private RealVector u;
    /** L data */
    private double[][] lData;
    /** Matrix L */
    private RealMatrix l;
    /** Transposed L matrix */
    private RealMatrix lT;

    /**
     * Constructor defining matrix Q, and null rescaler (to avoid rescaling)
     * 
     * @param matQ matrix Q
     * @throws PatriusException if an error occurs
     */
    public CholeskyFactorization(final RealMatrix matQ) throws PatriusException {
        this(matQ, null);
    }

    /**
     * Constructor defining matrix Q and rescaler
     * 
     * @param matQ the matrix to factorize
     * @param res a matrix rescaling implementation for balancing the factorization
     * @throws PatriusException if an error occurs
     */
    public CholeskyFactorization(final RealMatrix matQ,
            final MatrixRescaler res) throws PatriusException {
        this.dim = matQ.getRowDimension();
        this.q = matQ;
        this.rescaler = res;
    }

    /**
     * Calculates the Cholesky factorization with false checkSymmetry
     * 
     * @throws PatriusException if an error occurs
     */
    public void factorize() throws PatriusException {
        factorize(false);
    }

    /**
     * Cholesky factorization L of psd matrix, Q = L.LT.
     * Construction of the matrix L.
     * 
     * @param checkSymmetry check matrix symmetry?
     * @throws PatriusException if an error occurs
     */
    public void factorize(final boolean checkSymmetry) throws PatriusException {
        if (checkSymmetry && !q.isSquare()) {
            // Exception
            throw new PatriusException(PatriusMessages.NOT_SYMMETRIC_MATRIX);
        }

        if (this.rescaler != null) {
            // scaling the Q matrix, we have:
            // Q1 = U.Q.U[T] = U.L.L[T].U[T] = (U.L).(U.L)[T]
            // and because U is diagonal it preserves the triangular form of U.L, so
            // Q1 = U.Q.U[T] = L1.L1[T] is the new Cholesky decomposition
            final RealVector uV = rescaler.getMatrixScalingFactorsSymm(q);
            this.u = uV;
            this.q = AlgebraUtils.diagonalMatrixMult(uV, q, uV);
        }
        final double threshold = Utils.getDoubleMachineEpsilon();
        this.lData = new double[dim][];

        for (int i = 0; i < dim; i++) {
            lData[i] = new double[i + 1];
            final double[] lDataI = lData[i];
            // j < i
            for (int j = 0; j < i; j++) {
                final double[] lDataJ = lData[j];
                double sum = 0.0;
                for (int k = 0; k < j; k++) {
                    sum += lDataI[k] * lDataJ[k];
                }
                lDataI[j] = 1.0 / lDataJ[j] * (q.getEntry(i, j) - sum);
            }
            // j==i
            double sum = 0.0;
            for (int k = 0; k < i; k++) {
                sum += MathLib.pow(lDataI[k], 2);
            }
            final double d = q.getEntry(i, i) - sum;
            if (!(d > threshold)) {
                // Exception
                throw new PatriusException(PatriusMessages.NOT_POSITIVE_DEFINITE_MATRIX);
            }
            lDataI[i] = MathLib.sqrt(d);
        }
    }

    /**
     * 
     * Compute the inverse of the matrix.
     * @return the inverse matrix Q
     */
    public RealMatrix getInverse() {

        // QInv = LTInv * LInv, but for symmetry (QInv=QInvT)
        // QInv = LInvT * LTInvT = LInvT * LInv, so
        // LInvT = LTInv, and we calculate
        // QInv = LInvT * LInv

        final double[][] lTData = getLT().getData();
        final int dimension = lTData.length;

        // LTInv calculation (it will be x)
        // NB: LInv is lower-triangular
        final double[][] x = new double[dimension][dimension];
        for (int j = 0; j < dimension; j++) {
            final double[] xJ = x[j];
            xJ[j] = 1.;
            final double[] lTJ = lTData[j];
            final double lTJJ = lTJ[j];
            for (int k = 0; k < j + 1; ++k) {
                xJ[k] /= lTJJ;
            }
            for (int i = j + 1; i < dimension; i++) {
                final double[] xI = x[i];
                final double lTJI = lTJ[i];
                if (Double.compare(lTJI, 0.) != 0) {
                    for (int k = 0; k < j + 1; ++k) {
                        xI[k] -= xJ[k] * lTJI;
                    }
                }
            }
        }

        // transposition (L is upper-triangular)
        final double[][] lInvTData = new double[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            final double[] lInvTDatai = lInvTData[i];
            for (int j = i; j < dimension; j++) {
                lInvTDatai[j] = x[j][i];
            }
        }

        // QInv
        // NB: LInvT is upper-triangular, so LInvT[i][j]=0 if i>j
        final RealMatrix qInvData = new BlockRealMatrix(dimension, dimension);
        for (int row = 0; row < dimension; row++) {
            final double[] lInvTDataRow = lInvTData[row];
            final RealVector qInvDataRow = qInvData.getRowVector(row);
            for (int col = row; col < dimension; col++) {// symmetry of QInv
                final double[] lInvTDataCol = lInvTData[col];
                double sum = 0;
                for (int i = col; i < dimension; i++) {// upper triangular
                    sum += lInvTDataRow[i] * lInvTDataCol[i];
                }
                qInvDataRow.setEntry(col, sum);
                qInvData.setEntry(row, col, sum);
                qInvData.setEntry(col, row, sum);// symmetry of QInv
            }
        }

        return qInvData;
    }

    /**
     * Solver for system AX = b.
     * 
     * @param b vector
     * @return result X
     */
    public RealVector solve(final RealVector b) {
        if (b.getDimension() != dim) {
            // wrong dimension of vector b
            throw new PatriusRuntimeException(PatriusMessages.DIMENSIONS_MISMATCH, null);
        }

        // with scaling, we must solve U.Q.U.z = U.b, after that we have x = U.z
        RealVector bCopy = b.copy();
        if (this.rescaler != null) {
            // Scaling
            bCopy = AlgebraUtils.diagonalMatrixMult(this.u, b);
        }

        // Solve L.y = b
        final double[] y = new double[dim];
        for (int i = 0; i < dim; i++) {
            final double[] lI = lData[i];
            double sum = 0;
            for (int j = 0; j < i; j++) {
                sum += lI[j] * y[j];
            }
            y[i] = (bCopy.getEntry(i) - sum) / lI[i];
        }

        // Solve L[T].x = y
        final RealVector x = new ArrayRealVector(dim);
        for (int i = dim - 1; i > -1; i--) {
            double sum = 0;
            for (int j = dim - 1; j > i; j--) {
                sum += lData[j][i] * x.getEntry(j);
            }
            x.setEntry(i, (y[i] - sum) / lData[i][i]);
        }

        if (this.rescaler != null) {
            // Scaling case
            return AlgebraUtils.diagonalMatrixMult(this.u, x);
        } else {
            // Regular case
            // No scaling
            return x;
        }
    }

    /**
     * Solver for system AX = b.
     * 
     * @param b matrix
     * @return result X
     */
    public RealMatrix solve(final RealMatrix b) {
        // check matrix dimensions
        if (b.getRowDimension() != dim) {
            // wrong dimension of vector b
            throw new PatriusRuntimeException(PatriusMessages.DIMENSIONS_MISMATCH, null);
        }

        // with scaling, we must solve U.Q.U.z = U.b, after that we have x = U.z
        RealMatrix bCopy = b.copy(); // copy of b, not possible no change an argument
        if (this.rescaler != null) {
            // B = U.B;
            bCopy = AlgebraUtils.diagonalMatrixMult(this.u, b);
        }

        final int nOfColumns = b.getColumnDimension();

        // Solve LY = B
        // the same as L.Yc = Bc for each column Yc e Bc
        final double[][] y = new double[dim][nOfColumns];
        for (int i = 0; i < dim; i++) {
            final double[] lI = lData[i];
            final double[] sum = new double[nOfColumns];
            for (int j = 0; j < i; j++) {
                final double lIJ = lI[j];
                final double[] yJ = y[j];
                for (int col = 0; col < nOfColumns; col++) {
                    sum[col] += lIJ * yJ[col];
                }
            }
            final double[] yI = y[i];
            final RealVector bI = bCopy.getRowVector(i);
            final double lII = lI[i];
            for (int col = 0; col < nOfColumns; col++) {
                yI[col] = (bI.getEntry(col) - sum[col]) / lII;
            }
        }

        // Solve L[T].X = Y
        // the same as L[T].Xc = Yc for each column
        final RealMatrix x = new BlockRealMatrix(dim, nOfColumns);
        for (int i = dim - 1; i > -1; i--) {
            final double[] sum = new double[nOfColumns];
            for (int j = dim - 1; j > i; j--) {
                final double[] lJ = lData[j];
                final RealVector xJ = x.getRowVector(j);
                for (int col = 0; col < nOfColumns; col++) {
                    sum[col] += lJ[i] * xJ.getEntry(col);
                }
            }
            final RealVector xI = x.getRowVector(i);
            final double[] yI = y[i];
            final double lII = lData[i][i];
            for (int col = 0; col < nOfColumns; col++) {
                xI.setEntry(col, (yI[col] - sum[col]) / lII);
                x.setEntry(i, col, (yI[col] - sum[col]) / lII);
            }
        }

        // Check if rescaler is used
        if (this.rescaler != null) {
            // return U.X
            return AlgebraUtils.diagonalMatrixMult(this.u, x);
        } else {
            return x;
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
        //
        if (this.l == null) {
            final double[][] myL = new double[dim][dim];
            for (int i = 0; i < dim; i++) {
                final double[] lDataI = lData[i];
                final double[] myLI = myL[i];
                // copy the L data into the new array
                System.arraycopy(lDataI, 0, myLI, 0, i + 1);
            }
            if (this.rescaler != null) {
                // Q = UInv.Q1.UInv[T] = UInv.L1.L1[T].UInv[T] = (UInv.L1).(UInv.L1)[T]
                // so
                // L = UInv.L1
                final RealVector uInv = new ArrayRealVector(dim);
                for (int i = 0; i < dim; i++) {
                    uInv.setEntry(i, 1. / u.getEntry(i));
                }
                // U-1.myL
                this.l = AlgebraUtils.diagonalMatrixMult(uInv, new BlockRealMatrix(myL));
            } else {
                this.l = new BlockRealMatrix(myL);
            }
        }
        return this.l;
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
        if (this.lT == null) {
            this.lT = getL().transpose();
        }
        return this.lT;
    }
}
