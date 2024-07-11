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
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.joptimizer.algebra;

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.DefaultRealMatrixChangingVisitor;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Calculates the matrix rescaling factors so that the 1-norm of each row and each column of the
 * scaled matrix asymptotically converges to one.
 * 
 * @author alberto trivellato (alberto.trivellato@gmail.com)
 * @see Daniel Ruiz, "A scaling algorithm to equilibrate both rows and columns norms in matrices"
 * @see Philip A. Knight, Daniel Ruiz, Bora Ucar
 *      "A Symmetry Preserving Algorithm for Matrix Scaling"
 *      
 * @since 4.6
 */
public final class Matrix1NornRescaler implements MatrixRescaler {

    /** Epsilon */
    private static final double EPSILON = 1.e-3;
    /** Epsilon value */
    private double eps = EPSILON;

    /**
     * Constructor
     */
    public Matrix1NornRescaler() {
        //This constructor is intentionally empty. 
    }

    /**
     * Constructor 
     * @param epsilon epsilon value
     */
    public Matrix1NornRescaler(final double epsilon) {
        this.eps = epsilon;
    }

    /**
     * Scaling factors for not singular matrices.
     * @see Daniel Ruiz,
     *      "A scaling algorithm to equilibrate both rows and columns norms in matrices"
     * @see Philip A. Knight, Daniel Ruiz, Bora Ucar
     *      "A Symmetry Preserving Algorithm for Matrix Scaling"
     */
    @Override
    public RealVector[] getMatrixScalingFactors(final RealMatrix a) {
        final int r = a.getRowDimension();
        final int c = a.getColumnDimension();
        final RealVector d1 = new ArrayRealVector(r, 1);
        final RealVector d2 = new ArrayRealVector(c, 1);
        RealMatrix aK = a.copy();
        final RealVector dR = new ArrayRealVector(r, 1);
        final RealVector dC = new ArrayRealVector(c, 1);
        final RealVector dRInv = new ArrayRealVector(r);
        final RealVector dCInv = new ArrayRealVector(c);
        final int maxIteration = 50;
        for (int k = 0; k <= maxIteration; k++) {
            double normR = -Double.MAX_VALUE;
            double normC = -Double.MAX_VALUE;
            for (int i = 0; i < r; i++) {
                final double dri = (aK.getRowVector(i)).getLInfNorm();
                dR.setEntry(i, MathLib.sqrt(dri));  // set with sqrt(dri)
                dRInv.setEntry(i, 1. / MathLib.sqrt(dri));  // // set with 1/sqrt(dri)
                normR = MathLib.max(normR, MathLib.abs(1 - dri));
            }
            for (int j = 0; j < c; j++) {
                final double dci = (aK.getColumnVector(j)).getLInfNorm();
                dC.setEntry(j, MathLib.sqrt(dci));
                dCInv.setEntry(j, 1. / MathLib.sqrt(dci));
                normC = MathLib.max(normC, MathLib.abs(1 - dci));
            }

            if (normR < eps && normC < eps) {
                break;  // stop when the norm of R and C is smaller than epsilon
            }

            // D1 = ALG.mult(D1, DRInv);
            for (int i = 0; i < r; i++) {
                final double prevD1I = d1.getEntry(i);
                final double newD1I = prevD1I * dRInv.getEntry(i);  // new D1i = previous D1i * DRInversedi
                d1.setEntry(i, newD1I);
            }
            // D2 = ALG.mult(D2, DCInv);
            for (int j = 0; j < c; j++) {
                final double prevD2J = d2.getEntry(j);
                final double newD2J = prevD2J * dCInv.getEntry(j); // new D1j = previous D1j * DRInversedj
                d2.setEntry(j, newD2J);
            }

            // AK = ALG.mult(DRInv, ALG.mult(AK, DCInv));
            aK = AlgebraUtils.diagonalMatrixMult(dRInv, aK, dCInv); // dRInv.aK.dCInv
        }

        return new RealVector[] { d1, d2 }; // scaling factors
    }

    /**
     * Scaling factors for symmetric (not singular) matrices.
     * Just the subdiagonal elements of the matrix are required.
     * @see Daniel Ruiz,
     *      "A scaling algorithm to equilibrate both rows and columns norms in matrices"
     * @see Philip A. Knight, Daniel Ruiz, Bora Ucar
     *      "A Symmetry Preserving Algorithm for Matrix Scaling"
     */
    @Override
    public RealVector getMatrixScalingFactorsSymm(final RealMatrix a) {
        // Initialization
        final int dim = a.getColumnDimension();
        final RealVector d1 = new ArrayRealVector(dim, 1);
        RealMatrix aK = a.copy();
        final RealMatrix dR = MatrixUtils.createRealIdentityMatrix(dim); // identity matrix
        final RealVector dRInv = new ArrayRealVector(dim);
        final int maxIteration = 50;
        // Iterations until convergence
        for (int k = 0; k <= maxIteration; k++) {
            double normR = -Double.MAX_VALUE;
            for (int i = 0; i < dim; i++) {
                final double dri = getRowInfinityNorm(aK, i); // infinity norm
                if (dri < 0 || Double.isNaN(dri)) {
                    // NaN case
                    dR.setEntry(i, i, Double.NaN);
                    dRInv.setEntry(i, 1. / Double.NaN);
                    normR = Double.NaN;
                } else {
                    dR.setEntry(i, i, MathLib.sqrt(dri));
                    dRInv.setEntry(i, 1. / MathLib.sqrt(dri));
                    normR = MathLib.max(normR, MathLib.abs(1 - dri));
                }
                if (Double.isNaN(normR)) {
                    throw new IllegalArgumentException("matrix is singular"); // error: singular matrix
                }
            }

            if (normR < eps) {
                break;  // stop when the norm of R is smaller than epsilon
            }

            for (int i = 0; i < dim; i++) {
                final double prevD1I = d1.getEntry(i);
                final double newD1I = prevD1I * dRInv.getEntry(i);
                d1.setEntry(i, newD1I);
            }

            aK = AlgebraUtils.diagonalMatrixMult(dRInv, aK, dRInv); // dRInv.aK.dRInv
        }

        return d1; // Scaling factors
    }

    /**
     * Check if the scaling algorithm returned proper results.
     * Note that AOriginal cannot be only subdiagonal filled, because this check
     * is for both symm and bath notsymm matrices.
     * @param aOriginal the ORIGINAL (before scaling) matrix
     * @param u the return of the scaling algorithm
     * @param v the return of the scaling algorithm
     * @param base
     * @return
     */
    @Override
    public boolean checkScaling(final RealMatrix aOriginal,
            final RealVector u,
            final RealVector v) {

        final int c = aOriginal.getColumnDimension();
        final int r = aOriginal.getRowDimension();
        final double[] maxValueHolder = new double[] { -Double.MAX_VALUE };

        final DefaultRealMatrixChangingVisitor myFunct = new DefaultRealMatrixChangingVisitor() {
            /** {@inheritDoc} */
            @Override
            public double visit(final int i,
                    final int j,
                    final double pij) {
                if (pij != 0) {
                    maxValueHolder[0] = MathLib.max(maxValueHolder[0], MathLib.abs(pij));
                }
                return pij;
            }
        };

        final RealMatrix aScaled = AlgebraUtils.diagonalMatrixMult(u, aOriginal, v); // u.aOriginal.v

        // view A row by row
        boolean isOk = true;
        for (int i = 0; isOk && i < r; i++) {
            maxValueHolder[0] = -Double.MAX_VALUE;
            final RealMatrix p = aScaled.getSubMatrix(i, i, 0, c - 1);
            p.walkInOptimizedOrder(myFunct);
            isOk = MathLib.abs(1. - maxValueHolder[0]) < eps;  // Check |1-maxValueHolder[0]| < epsilon
        }
        // view A col by col
        for (int j = 0; isOk && j < c; j++) {
            maxValueHolder[0] = -Double.MAX_VALUE;
            final RealMatrix p = aScaled.getSubMatrix(0, r - 1, j, j);
            p.walkInOptimizedOrder(myFunct);
            isOk = MathLib.abs(1. - maxValueHolder[0]) < eps;
        }
        return isOk; // true if scaling algorithm returned proper results
    }

    /**
     * 
     * @param aSymm symm matrix filled in its subdiagonal elements
     * @param r the index of the row
     * @return infinity norm
     */
    public static double getRowInfinityNorm(final RealMatrix aSymm,
            final int r) {

        final double[] maxValueHolder = new double[] { -Double.MAX_VALUE };

        final DefaultRealMatrixChangingVisitor myFunct = new DefaultRealMatrixChangingVisitor() {
            /** {@inheritDoc} */
            @Override
            public double visit(final int i,
                    final int j,
                    final double pij) {
                if (pij != 0) {
                    // assign max(maxValueHolder[0], |pij|)
                    maxValueHolder[0] = MathLib.max(maxValueHolder[0], MathLib.abs(pij));
                }
                return pij;
            }
        };

        // view A row from starting element to diagonal
        final RealMatrix aR = aSymm.getSubMatrix(r, r, 0, r);
        aR.walkInOptimizedOrder(myFunct);
        // view A col from diagonal to final element
        final RealMatrix aC = aSymm.getSubMatrix(r, r + aSymm.getRowDimension() - r - 1, r, r);
        aC.walkInOptimizedOrder(myFunct);

        return maxValueHolder[0]; // infinity norm
    }
}
