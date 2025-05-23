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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3111:10/05/2022:[PATRIUS] Suite FA 2999 Corrections mineures de la classe EigenDecomposition 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2999:15/11/2021:[PATRIUS] Anomalies classe EigenDecomposition 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2247:27/05/2020:Modifications dans EigenDecomposition 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop NestedBlockDepth check
//CHECKSTYLE: stop CommentRatio check
//Reason: model - Commons-Math code

/**
 * Calculates the eigen decomposition of a real matrix.
 * <p>
 * The eigen decomposition of matrix A is a set of two matrices: V and D such that 
 * A = V &times; D &times; V<sup>-1</sup>. Where A, V and D are all m &times; m matrices.
 * </p>
 * <p>
 * This class is similar in spirit to the <code>EigenvalueDecomposition</code> class from the <a
 * href="http://math.nist.gov/javanumerics/jama/">JAMA</a> library, with the following changes:
 * </p>
 * <ul>
 * <li>a {@link #getVT() getVt} method has been added,</li>
 * <li>two {@link #getRealEigenvalue(int) getRealEigenvalue} and {@link #getImagEigenvalue(int)
 * getImagEigenvalue} methods to pick up a single eigenvalue have been added,</li>
 * <li>a {@link #getEigenvector(int) getEigenvector} method to pick up a single eigenvector has been added,</li>
 * <li>a {@link #getDeterminant() getDeterminant} method has been added.</li>
 * <li>a {@link #getSolver() getSolver} method has been added.</li>
 * </ul>
 * <p>
 * As of 3.1, this class supports general real matrices (both symmetric and non-symmetric):
 * </p>
 * <p>
 * If A is symmetric, then A = V &times; D &times; V<sup>T</sup> where the eigenvalue matrix D is 
 * a diagonal of real values and the eigenvector matrix V is orthogonal 
 * (i.e. V &times; V<sup>T</sup> = V<sup>T</sup> &times; V = Id).
 * </p>
 * <p>
 * If A is not symmetric, then the eigenvalue matrix D is block diagonal with the real eigenvalues in 1-by-1 blocks and
 * any complex eigenvalues, lambda + i*mu, in 2-by-2 blocks:
 * 
 * <pre>
 *    [lambda, mu    ]
 *    [   -mu, lambda]
 * </pre>
 * 
 * The columns of V represent the eigenvectors in the sense that A*V = V*D. The matrix V may be badly conditioned, 
 * or even singular, so the validity of the equation A = V &times; D &times; V<sup>-1</sup> depends upon 
 * the condition of V.
 * </p>
 * <p>
 * This implementation is based on the paper by A. Drubrulle, R.S. Martin and J.H. Wilkinson "The Implicit QL Algorithm"
 * in Wilksinson and Reinsch (1971) Handbook for automatic computation, vol. 2, Linear algebra, Springer-Verlag,
 * New-York
 * </p>
 * @see <a href="http://mathworld.wolfram.com/EigenDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/Eigendecomposition_of_a_matrix">Wikipedia</a>
 * @since 2.0 (changed to concrete class in 3.0)
 */
@SuppressWarnings("PMD.NullAssignment")
public class EigenDecomposition implements Decomposition {

    /** Internally used epsilon criteria. */
    private static final double EPSILON = 1e-12;
    /** Maximum number of iterations accepted in the implicit QL transformation */
    private static final byte MAX_ITER = 30;
    /**
     * Default factor value to compute relativeSymmetryThreshold
     * in constructor EigenDecomposition(RealMatrix)
     */
    private static final int THRESHOLD_FACTOR = 10;
    /** Threshold above which off-diagonal elements are considered too different and matrix not symmetric. */
    private final double relativeSymmetryThreshold;
    /** Main diagonal of the tridiagonal matrix. */
    private double[] main;
    /** Secondary diagonal of the tridiagonal matrix. */
    private double[] secondary;
    /**
     * Transformer to tridiagonal (may be null if matrix is already
     * tridiagonal).
     */
    private TriDiagonalTransformer transformer;
    /** Real part of the realEigenvalues. */
    private double[] realEigenValues;
    /** Imaginary part of the realEigenvalues. */
    private double[] imagEigenValues;
    /** Eigenvectors. */
    private ArrayRealVector[] eigenVectors;
    /** Whether the matrix is symmetric. */
    private boolean symmetric;

    /**
     * Calculates the eigen decomposition of the given real matrix.
     * <p>
     * Supports decomposition of a general matrix since 3.1.
     * 
     * @param matrix
     *        Matrix to decompose.
     * @throws MaxCountExceededException
     *         if the algorithm fails to converge.
     * @throws MathArithmeticException
     *         if the decomposition of a general matrix
     *         results in a matrix with zero norm
     * @since 3.1
     */
    public EigenDecomposition(final RealMatrix matrix) {
        this(matrix, THRESHOLD_FACTOR * matrix.getRowDimension() * matrix.getColumnDimension() * Precision.EPSILON);
    }

    /**
     * Calculates the eigen decomposition of the given real matrix.
     * <p>
     * Supports decomposition of a general matrix since 3.1.
     * 
     * @param matrix
     *        Matrix to decompose.
     * @param relativeSymmetryThreshold
     *        Relative tolerance when checking whether the RealMatrix matrix is symmetric.
     * @throws MaxCountExceededException
     *         if the algorithm fails to converge.
     * @throws MathArithmeticException
     *         if the decomposition of a general matrix
     *         results in a matrix with zero norm
     * @since 4.5
     */
    public EigenDecomposition(final RealMatrix matrix,
            final double relativeSymmetryThreshold) {
        this.relativeSymmetryThreshold = relativeSymmetryThreshold;
        eigenDecompose(matrix);
    }

    /**
     * Calculates the eigen decomposition of the symmetric tridiagonal
     * matrix. The Householder matrix is assumed to be the identity matrix.
     *
     * @param main Main diagonal of the symmetric tridiagonal form.
     * @param secondary Secondary of the tridiagonal form.
     * @throws MaxCountExceededException if the algorithm fails to converge.
     * @since 3.1
     */
    public EigenDecomposition(final double[] main,
            final double[] secondary) {
        this.relativeSymmetryThreshold = 0.;
        this.symmetric = true;
        this.main = main.clone();
        this.secondary = secondary.clone();
        this.transformer = null;
        final int size = main.length;
        final double[][] z = new double[size][size];
        for (int i = 0; i < size; i++) {
            z[i][i] = 1.0;
        }
        this.findEigenVectors(z);
    }

    /**
     * Calculates the eigen decomposition of the given real matrix.
     * 
     * @param matrix
     *        The matrix to decompose.
     */
    private void eigenDecompose(final RealMatrix matrix) {

        if (matrix instanceof DiagonalMatrix) {
            // Diagonal matrix optimization
            final double[] dataRef = ((DiagonalMatrix) matrix).getDataRef();
            final int n = dataRef.length;
            // Decreasing values sorting
            final Double[] shortedDataRef = new Double[n];
            for (int i = 0; i < n; i++) {
                shortedDataRef[i] = new Double(dataRef[i]);
            }
            Arrays.sort(shortedDataRef, Collections.reverseOrder());

            final double[] eigenValuesTemp = new double[n];
            final ArrayRealVector[] eigenVectorsTemp = new ArrayRealVector[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (shortedDataRef[i] == dataRef[j]) {
                        final double[] tmp = new double[n];
                        tmp[j] = 1.0;
                        eigenVectorsTemp[i] = new ArrayRealVector(tmp, false);
                    }
                }
                eigenValuesTemp[i] = shortedDataRef[i].doubleValue();
            }

            this.realEigenValues = eigenValuesTemp;
            this.eigenVectors = eigenVectorsTemp;
            this.imagEigenValues = new double[n]; // Vector 0
            this.symmetric = true;
            
        } else {
           
            this.symmetric = matrix.isSymmetric(this.relativeSymmetryThreshold);
            if (this.symmetric) {
                this.transformToTridiagonal(matrix);
                this.findEigenVectors(this.transformer.getQ().getData(true));
            } else {
                final SchurTransformer t = this.transformToSchur(matrix);
                this.findEigenVectorsFromSchur(t);
            }
        }
    }

    /**
     * Gets the matrix V of the decomposition.
     * The columns of V are the eigenvectors of the original matrix.
     * No assumption is made about the orientation of the system axes formed
     * by the columns of V (e.g. in a 3-dimension space, V can form a left-
     * or right-handed system).
     *
     * @return the V matrix.
     */
    public RealMatrix getV() {
        final int m = this.eigenVectors.length;
        // Cached value of V.
        final RealMatrix cachedV = MatrixUtils.createRealMatrix(m, m);
        for (int k = 0; k < m; ++k) {
            cachedV.setColumnVector(k, this.eigenVectors[k]);
        }

        return cachedV;
    }

    /**
     * Gets the block diagonal matrix D of the decomposition.
     * D is a block diagonal matrix.
     * Real eigenvalues are on the diagonal while complex values are on
     * 2x2 blocks { {real +imaginary}, {-imaginary, real} }.
     *
     * @return the D matrix.
     *
     * @see #getRealEigenvalues()
     * @see #getImagEigenvalues()
     */
    public RealMatrix getD() {
        // Cached value of D.
        final RealMatrix cachedD = MatrixUtils.createRealDiagonalMatrix(this.realEigenValues);

        for (int i = 0; i < this.imagEigenValues.length; i++) {
            if (Precision.compareTo(this.imagEigenValues[i], 0.0, EPSILON) > 0) {
                cachedD.setEntry(i, i + 1, this.imagEigenValues[i]);
            } else if (Precision.compareTo(this.imagEigenValues[i], 0.0, EPSILON) < 0) {
                cachedD.setEntry(i, i - 1, this.imagEigenValues[i]);
            }
        }

        return cachedD;
    }

    /**
     * Gets the transpose of the matrix V of the decomposition.
     * The columns of V are the eigenvectors of the original matrix.
     * No assumption is made about the orientation of the system axes formed
     * by the columns of V (e.g. in a 3-dimension space, V can form a left-
     * or right-handed system).
     *
     * @return the transpose of the V matrix.
     */
    public RealMatrix getVT() {
        final int m = this.eigenVectors.length;
        // Cached value of Vt.
        final RealMatrix cachedVt = MatrixUtils.createRealMatrix(m, m);
        for (int k = 0; k < m; ++k) {
            cachedVt.setRowVector(k, this.eigenVectors[k]);
        }

        return cachedVt;
    }

    /**
     * Returns whether the calculated eigen values are complex or real.
     * <p>
     * The method performs a zero check for each element of the {@link #getImagEigenvalues()} array and returns
     * {@code true} if any element is not equal to zero.
     *
     * @return {@code true} if the eigen values are complex, {@code false} otherwise
     * @since 3.1
     */
    public boolean hasComplexEigenvalues() {
        for (int i = 0; i < this.imagEigenValues.length; i++) {
            if (!Precision.equals(this.imagEigenValues[i], 0.0, EPSILON)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a copy of the real parts of the eigenvalues of the original matrix.
     *
     * @return a copy of the real parts of the eigenvalues of the original matrix.
     *
     * @see #getD()
     * @see #getRealEigenvalue(int)
     * @see #getImagEigenvalues()
     */
    public double[] getRealEigenvalues() {
        return this.realEigenValues.clone();
    }

    /**
     * Returns the real part of the i<sup>th</sup> eigenvalue of the original
     * matrix.
     *
     * @param i index of the eigenvalue (counting from 0)
     * @return real part of the i<sup>th</sup> eigenvalue of the original
     *         matrix.
     *
     * @see #getD()
     * @see #getRealEigenvalues()
     * @see #getImagEigenvalue(int)
     */
    public double getRealEigenvalue(final int i) {
        return this.realEigenValues[i];
    }

    /**
     * Gets a copy of the imaginary parts of the eigenvalues of the original
     * matrix.
     *
     * @return a copy of the imaginary parts of the eigenvalues of the original
     *         matrix.
     *
     * @see #getD()
     * @see #getImagEigenvalue(int)
     * @see #getRealEigenvalues()
     */
    public double[] getImagEigenvalues() {
        return this.imagEigenValues.clone();
    }

    /**
     * Gets the imaginary part of the i<sup>th</sup> eigenvalue of the original
     * matrix.
     *
     * @param i Index of the eigenvalue (counting from 0).
     * @return the imaginary part of the i<sup>th</sup> eigenvalue of the original
     *         matrix.
     *
     * @see #getD()
     * @see #getImagEigenvalues()
     * @see #getRealEigenvalue(int)
     */
    public double getImagEigenvalue(final int i) {
        return this.imagEigenValues[i];
    }

    /**
     * Gets a copy of the i<sup>th</sup> eigenvector of the original matrix.
     *
     * @param i Index of the eigenvector (counting from 0).
     * @return a copy of the i<sup>th</sup> eigenvector of the original matrix.
     * @see #getD()
     */
    public RealVector getEigenvector(final int i) {
        return this.eigenVectors[i].copy();
    }

    /**
     * Computes the determinant of the matrix.
     *
     * @return the determinant of the matrix.
     */
    public double getDeterminant() {
        double determinant = 1;
        for (final double lambda : this.realEigenValues) {
            determinant *= lambda;
        }
        return determinant;
    }

    /**
     * Computes the square-root of the matrix.
     * This implementation assumes that the matrix is symmetric and positive
     * definite.
     *
     * @return the square-root of the matrix.
     * @throws MathUnsupportedOperationException if the matrix is not
     *         symmetric or not positive definite.
     * @since 3.1
     */
    public RealMatrix getSquareRoot() {
        
        if (!this.symmetric) {
            throw new MathUnsupportedOperationException();
        }
        
        final double[] sqrtEigenValues = new double[this.realEigenValues.length];
        for (int i = 0; i < this.realEigenValues.length; i++) {
            final double eigen = this.realEigenValues[i];
            if (eigen < 0) {
                throw new MathUnsupportedOperationException();
            }
            sqrtEigenValues[i] = MathLib.sqrt(eigen);
        }
        final RealMatrix sqrtEigen = MatrixUtils.createRealDiagonalMatrix(sqrtEigenValues);
        final RealMatrix v = this.getV();
        final RealMatrix vT = this.getVT();

        return v.multiply(sqrtEigen).multiply(vT);
    }

    /**
     * Gets a solver for finding the A &times; X = B solution in exact
     * linear sense.
     * <p>
     * Since 3.1, eigen decomposition of a general matrix is supported, but the {@link DecompositionSolver} only
     * supports real eigenvalues.
     *
     * @return a solver
     * @throws MathUnsupportedOperationException if the matrix is not symmetric
     */
    @Override
    public DecompositionSolver getSolver() {
        if (!this.symmetric) {
            throw new MathUnsupportedOperationException();
        }
        return new Solver(this.realEigenValues, this.imagEigenValues, this.eigenVectors);
    }
    
    /**
     * Method returning the value of the private global parameter isSymmetric.
     * If the methods returns true, it means that the decomposition method used is Dubrulle et al.(1971)'s.
     * If the methods returns false, it means that the decomposition method used
     * is the matrix transformation to a Shur form.
     * 
     * @return the value of the boolean private global parameter isSymmetric.
     * @since 4.5
     */
    public boolean isSymmetric() {
        return this.symmetric;
    }

    /**
     * Transforms the matrix to tridiagonal form.
     *
     * @param matrix Matrix to transform.
     */
    private void transformToTridiagonal(final RealMatrix matrix) {
        // transform the matrix to tridiagonal
        this.transformer = new TriDiagonalTransformer(matrix);
        this.main = this.transformer.getMainDiagonalRef();
        this.secondary = this.transformer.getSecondaryDiagonalRef();
    }

    /**
     * Find eigenvalues and eigenvectors (Dubrulle et al., 1971)
     *
     * @param householderMatrix Householder matrix of the transformation
     *        to tridiagonal form.
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void findEigenVectors(final double[][] householderMatrix) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // CHECKSTYLE: resume MethodLength check
        final double[][] z = householderMatrix;
        final int n = this.main.length;
        this.realEigenValues = new double[n];
        this.imagEigenValues = new double[n];
        final double[] e = new double[n];
        for (int i = 0; i < n - 1; i++) {
            this.realEigenValues[i] = this.main[i];
            e[i] = this.secondary[i];
        }
        this.realEigenValues[n - 1] = this.main[n - 1];
        e[n - 1] = 0;

        // Determine the largest main and secondary value in absolute term.
        double maxAbsoluteValue = 0;
        for (int i = 0; i < n; i++) {
            if (MathLib.abs(this.realEigenValues[i]) > maxAbsoluteValue) {
                maxAbsoluteValue = MathLib.abs(this.realEigenValues[i]);
            }
            if (MathLib.abs(e[i]) > maxAbsoluteValue) {
                maxAbsoluteValue = MathLib.abs(e[i]);
            }
        }
        // Make null any main and secondary value too small to be significant
        if (maxAbsoluteValue != 0) {
            for (int i = 0; i < n; i++) {
                if (MathLib.abs(this.realEigenValues[i]) <= Precision.EPSILON * maxAbsoluteValue) {
                    this.realEigenValues[i] = 0;
                }
                if (MathLib.abs(e[i]) <= Precision.EPSILON * maxAbsoluteValue) {
                    e[i] = 0;
                }
            }
        }

        for (int j = 0; j < n; j++) {
            int its = 0;
            int m;
            do {
                for (m = j; m < n - 1; m++) {
                    final double delta = MathLib.abs(this.realEigenValues[m])
                            + MathLib.abs(this.realEigenValues[m + 1]);
                    if (MathLib.abs(e[m]) + delta == delta) {
                        break;
                    }
                }
                if (m != j) {
                    if (its == MAX_ITER) {
                        throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, MAX_ITER);
                    }
                    its++;
                    double q = (this.realEigenValues[j + 1] - this.realEigenValues[j]) / (2 * e[j]);
                    double t = MathLib.sqrt(1 + q * q);
                    if (q < 0.0) {
                        q = this.realEigenValues[m] - this.realEigenValues[j] + e[j] / (q - t);
                    } else {
                        q = this.realEigenValues[m] - this.realEigenValues[j] + e[j] / (q + t);
                    }
                    double u = 0.0;
                    double s = 1.0;
                    double c = 1.0;
                    int i;
                    for (i = m - 1; i >= j; i--) {
                        double p = s * e[i];
                        final double h = c * e[i];
                        if (MathLib.abs(p) >= MathLib.abs(q)) {
                            c = q / p;
                            t = MathLib.sqrt(c * c + 1.0);
                            e[i + 1] = p * t;
                            s = 1.0 / t;
                            c *= s;
                        } else {
                            s = p / q;
                            t = MathLib.sqrt(s * s + 1.0);
                            e[i + 1] = q * t;
                            c = 1.0 / t;
                            s *= c;
                        }
                        if (e[i + 1] == 0.0) {
                            this.realEigenValues[i + 1] -= u;
                            e[m] = 0.0;
                            break;
                        }
                        q = this.realEigenValues[i + 1] - u;
                        t = (this.realEigenValues[i] - q) * s + 2.0 * c * h;
                        u = s * t;
                        this.realEigenValues[i + 1] = q + u;
                        q = c * t - h;
                        for (int ia = 0; ia < n; ia++) {
                            p = z[ia][i + 1];
                            z[ia][i + 1] = s * z[ia][i] + c * p;
                            z[ia][i] = c * z[ia][i] - s * p;
                        }
                    }
                    if (t == 0.0 && i >= j) {
                        continue;
                    }
                    this.realEigenValues[j] -= u;
                    e[j] = q;
                    e[m] = 0.0;
                }
            } while (m != j);
        }

        // Sort the eigen values (and vectors) in increase order
        for (int i = 0; i < n; i++) {
            int k = i;
            double p = this.realEigenValues[i];
            for (int j = i + 1; j < n; j++) {
                if (this.realEigenValues[j] > p) {
                    k = j;
                    p = this.realEigenValues[j];
                }
            }
            if (k != i) {
                this.realEigenValues[k] = this.realEigenValues[i];
                this.realEigenValues[i] = p;
                for (int j = 0; j < n; j++) {
                    p = z[j][i];
                    z[j][i] = z[j][k];
                    z[j][k] = p;
                }
            }
        }

        // Determine the largest eigen value in absolute term.
        maxAbsoluteValue = 0;
        for (int i = 0; i < n; i++) {
            if (MathLib.abs(this.realEigenValues[i]) > maxAbsoluteValue) {
                maxAbsoluteValue = MathLib.abs(this.realEigenValues[i]);
            }
        }
        // Make null any eigen value too small to be significant
        if (maxAbsoluteValue != 0.0) {
            for (int i = 0; i < n; i++) {
                if (MathLib.abs(this.realEigenValues[i]) < Precision.EPSILON * maxAbsoluteValue) {
                    this.realEigenValues[i] = 0;
                }
            }
        }
        this.eigenVectors = new ArrayRealVector[n];
        final double[] tmp = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tmp[j] = z[j][i];
            }
            this.eigenVectors[i] = new ArrayRealVector(tmp);
        }
    }
    
    /**
     * Transforms the matrix to Schur form and calculates the eigenvalues.
     *
     * @param matrix Matrix to transform.
     * @return the {@link SchurTransformer Shur transform} for this matrix
     */
    private SchurTransformer transformToSchur(final RealMatrix matrix) {
        final SchurTransformer schurTransform = new SchurTransformer(matrix);
        final double[][] matT = schurTransform.getT().getData(false);

        this.realEigenValues = new double[matT.length];
        this.imagEigenValues = new double[matT.length];

        int i = 0;
        while (i < this.realEigenValues.length) {
            if (i == (this.realEigenValues.length - 1) || Precision.equals(matT[i + 1][i], 0.0, EPSILON)) {
                this.realEigenValues[i] = matT[i][i];
                i++;
            } else {
                final double x = matT[i + 1][i + 1];
                final double p = 0.5 * (matT[i][i] - x);
                final double z = MathLib.sqrt(MathLib.abs(p * p + matT[i + 1][i] * matT[i][i + 1]));
                this.realEigenValues[i] = x + p;
                this.imagEigenValues[i] = z;
                this.realEigenValues[i + 1] = x + p;
                this.imagEigenValues[i + 1] = -z;
                i += 2;
            }
        }
        return schurTransform;
    }

    /**
     * Performs a division of two complex numbers.
     *
     * @param xr real part of the first number
     * @param xi imaginary part of the first number
     * @param yr real part of the second number
     * @param yi imaginary part of the second number
     * @return result of the complex division
     */
    private static Complex cdiv(final double xr, final double xi, final double yr, final double yi) {
        return new Complex(xr, xi).divide(new Complex(yr, yi));
    }

    /**
     * Find eigenvectors from a matrix transformed to Schur form.
     *
     * @param schur the schur transformation of the matrix
     * @throws MathArithmeticException if the Schur form has a norm of zero
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    private void findEigenVectorsFromSchur(final SchurTransformer schur) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check
        final double[][] matrixT = schur.getT().getData(true);

        final int n = matrixT.length;

        // compute matrix norm
        double norm = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = MathLib.max(i - 1, 0); j < n; j++) {
                norm += MathLib.abs(matrixT[i][j]);
            }
        }

        // we can not handle a matrix with zero norm
        if (Precision.equals(norm, 0.0, EPSILON)) {
            throw new MathArithmeticException(PatriusMessages.ZERO_NORM);
        }

        // Backsubstitute to find vectors of upper triangular form

        double r = 0.0;
        double s = 0.0;
        double z = 0.0;

        for (int idx = n - 1; idx >= 0; idx--) {
            final double p = this.realEigenValues[idx];
            double q = this.imagEigenValues[idx];

            if (Precision.equals(q, 0.0)) {
                // Real vector
                int l = idx;
                matrixT[idx][idx] = 1.0;
                for (int i = idx - 1; i >= 0; i--) {
                    final double w = matrixT[i][i] - p;
                    r = 0.0;
                    for (int j = l; j <= idx; j++) {
                        r += matrixT[i][j] * matrixT[j][idx];
                    }
                    if (Precision.compareTo(this.imagEigenValues[i], 0.0, EPSILON) < 0) {
                        z = w;
                        s = r;
                    } else {
                        l = i;
                        if (Precision.equals(this.imagEigenValues[i], 0.0)) {
                            if (w != 0.0) {
                                matrixT[i][idx] = -r / w;
                            } else {
                                matrixT[i][idx] = -r / (Precision.EPSILON * norm);
                            }
                        } else {
                            // Solve real equations
                            final double x = matrixT[i][i + 1];
                            final double y = matrixT[i + 1][i];
                            q = (this.realEigenValues[i] - p) * (this.realEigenValues[i] - p) + this.imagEigenValues[i]
                                    * this.imagEigenValues[i];
                            final double t = (x * s - z * r) / q;
                            matrixT[i][idx] = t;
                            if (MathLib.abs(x) > MathLib.abs(z)) {
                                matrixT[i + 1][idx] = (-r - w * t) / x;
                            } else {
                                matrixT[i + 1][idx] = (-s - y * t) / z;
                            }
                        }

                        // Overflow control
                        final double t = MathLib.abs(matrixT[i][idx]);
                        if ((Precision.EPSILON * t) * t > 1) {
                            for (int j = i; j <= idx; j++) {
                                matrixT[j][idx] /= t;
                            }
                        }
                    }
                }
            } else if (q < 0.0) {
                // Complex vector
                int l = idx - 1;

                // Last vector component imaginary so matrix is triangular
                if (MathLib.abs(matrixT[idx][idx - 1]) > MathLib.abs(matrixT[idx - 1][idx])) {
                    matrixT[idx - 1][idx - 1] = q / matrixT[idx][idx - 1];
                    matrixT[idx - 1][idx] = -(matrixT[idx][idx] - p) / matrixT[idx][idx - 1];
                } else {
                    final Complex result = cdiv(0.0, -matrixT[idx - 1][idx], matrixT[idx - 1][idx - 1] - p, q);
                    matrixT[idx - 1][idx - 1] = result.getReal();
                    matrixT[idx - 1][idx] = result.getImaginary();
                }

                matrixT[idx][idx - 1] = 0.0;
                matrixT[idx][idx] = 1.0;

                for (int i = idx - 2; i >= 0; i--) {
                    double ra = 0.0;
                    double sa = 0.0;
                    for (int j = l; j <= idx; j++) {
                        ra += matrixT[i][j] * matrixT[j][idx - 1];
                        sa += matrixT[i][j] * matrixT[j][idx];
                    }
                    final double w = matrixT[i][i] - p;

                    if (Precision.compareTo(this.imagEigenValues[i], 0.0, EPSILON) < 0) {
                        z = w;
                        r = ra;
                        s = sa;
                    } else {
                        l = i;
                        if (Precision.equals(this.imagEigenValues[i], 0.0)) {
                            final Complex c = cdiv(-ra, -sa, w, q);
                            matrixT[i][idx - 1] = c.getReal();
                            matrixT[i][idx] = c.getImaginary();
                        } else {
                            // Solve complex equations
                            final double x = matrixT[i][i + 1];
                            final double y = matrixT[i + 1][i];
                            double vr = (this.realEigenValues[i] - p) * (this.realEigenValues[i] - p)
                                    + this.imagEigenValues[i]
                                    * this.imagEigenValues[i] - q * q;
                            final double vi = (this.realEigenValues[i] - p) * 2.0 * q;
                            if (Precision.equals(vr, 0.0) && Precision.equals(vi, 0.0)) {
                                vr = Precision.EPSILON
                                        * norm
                                        * (MathLib.abs(w) + MathLib.abs(q) + MathLib.abs(x) + MathLib.abs(y) + MathLib
                                                .abs(z));
                            }
                            final Complex c = cdiv(x * r - z * ra + q * sa, x * s - z * sa - q * ra, vr, vi);
                            matrixT[i][idx - 1] = c.getReal();
                            matrixT[i][idx] = c.getImaginary();

                            if (MathLib.abs(x) > (MathLib.abs(z) + MathLib.abs(q))) {
                                matrixT[i + 1][idx - 1] = (-ra - w * matrixT[i][idx - 1] + q * matrixT[i][idx]) / x;
                                matrixT[i + 1][idx] = (-sa - w * matrixT[i][idx] - q * matrixT[i][idx - 1]) / x;
                            } else {
                                final Complex c2 = cdiv(-r - y * matrixT[i][idx - 1], -s - y * matrixT[i][idx], z, q);
                                matrixT[i + 1][idx - 1] = c2.getReal();
                                matrixT[i + 1][idx] = c2.getImaginary();
                            }
                        }

                        // Overflow control
                        final double t = MathLib.max(MathLib.abs(matrixT[i][idx - 1]), MathLib.abs(matrixT[i][idx]));
                        if ((Precision.EPSILON * t) * t > 1) {
                            for (int j = i; j <= idx; j++) {
                                matrixT[j][idx - 1] /= t;
                                matrixT[j][idx] /= t;
                            }
                        }
                    }
                }
            }
        }

        // Back transformation to get eigenvectors of original matrix
        final double[][] matrixP = schur.getP().getData(true);
        for (int j = n - 1; j >= 0; j--) {
            for (int i = 0; i <= n - 1; i++) {
                z = 0.0;
                for (int k = 0; k <= MathLib.min(j, n - 1); k++) {
                    z += matrixP[i][k] * matrixT[k][j];
                }
                matrixP[i][j] = z;
            }
        }

        this.eigenVectors = new ArrayRealVector[n];
        final double[] tmp = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tmp[j] = matrixP[j][i];
            }
            this.eigenVectors[i] = new ArrayRealVector(tmp);
        }
    }

    /**
     * Builder for decomposition.
     * 
     * @param relativeSymmetryThreshold
     *        Relative tolerance when checking whether the RealMatrix matrix is symmetric.
     * @return decomposition
     */
    public static Function<RealMatrix, Decomposition> decompositionBuilder (final double relativeSymmetryThreshold) {
        return (realMatrix) -> new EigenDecomposition(realMatrix, relativeSymmetryThreshold);
    }

    /** Specialized solver. */
    private static final class Solver implements DecompositionSolver {

        /** Real part of the realEigenvalues. */
        private final double[] realEigenvalues;
        /** Imaginary part of the realEigenvalues. */
        private final double[] imagEigenvalues;
        /** Eigenvectors. */
        private final ArrayRealVector[] eigenvectors;

        /**
         * Builds a solver from decomposed matrix.
         *
         * @param realEigenvaluesIn Real parts of the eigenvalues.
         * @param imagEigenvaluesIn Imaginary parts of the eigenvalues.
         * @param eigenvectorsIn Eigenvectors.
         */
        private Solver(final double[] realEigenvaluesIn,
                final double[] imagEigenvaluesIn,
                final ArrayRealVector[] eigenvectorsIn) {
            this.realEigenvalues = realEigenvaluesIn;
            this.imagEigenvalues = imagEigenvaluesIn;
            this.eigenvectors = eigenvectorsIn;
        }

        /**
         * Solves the linear equation A &times; X = B for symmetric matrices A.
         * <p>
         * This method only finds exact linear solutions, i.e. solutions for which ||A &times; X - B|| is exactly 0.
         * </p>
         *
         * @param b Right-hand side of the equation A &times; X = B.
         * @return a Vector X that minimizes the two norm of A &times; X - B.
         *
         * @throws DimensionMismatchException if the matrices dimensions do not match.
         * @throws SingularMatrixException if the decomposed matrix is singular.
         */
        @Override
        public RealVector solve(final RealVector b) {
            if (!isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int m = this.realEigenvalues.length;
            if (b.getDimension() != m) {
                throw new DimensionMismatchException(b.getDimension(), m);
            }

            final double[] bp = new double[m];
            for (int i = 0; i < m; ++i) {
                final ArrayRealVector v = this.eigenvectors[i];
                final double[] vData = v.getDataRef();
                final double s = v.dotProduct(b) / this.realEigenvalues[i];
                for (int j = 0; j < m; ++j) {
                    bp[j] += s * vData[j];
                }
            }

            return new ArrayRealVector(bp, false);
        }

        /** {@inheritDoc} */
        @Override
        public RealMatrix solve(final RealMatrix b) {

            if (!isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int m = this.realEigenvalues.length;
            if (b.getRowDimension() != m) {
                throw new DimensionMismatchException(b.getRowDimension(), m);
            }

            final int nColB = b.getColumnDimension();
            final double[][] bp = new double[m][nColB];
            final double[] tmpCol = new double[m];
            for (int k = 0; k < nColB; ++k) {
                for (int i = 0; i < m; ++i) {
                    tmpCol[i] = b.getEntry(i, k);
                    bp[i][k] = 0;
                }
                for (int i = 0; i < m; ++i) {
                    final ArrayRealVector v = this.eigenvectors[i];
                    final double[] vData = v.getDataRef();
                    double s = 0;
                    for (int j = 0; j < m; ++j) {
                        s += v.getEntry(j) * tmpCol[j];
                    }
                    s /= this.realEigenvalues[i];
                    for (int j = 0; j < m; ++j) {
                        bp[j][k] += s * vData[j];
                    }
                }
            }

            return new Array2DRowRealMatrix(bp, false);

        }

        /**
         * Checks whether the decomposed matrix is non-singular.
         *
         * @return true if the decomposed matrix is non-singular.
         */
        @Override
        public boolean isNonSingular() {
            double largestEigenvalueNorm = 0.0;
            // Looping over all values (in case they are not sorted in decreasing
            // order of their norm).
            for (int i = 0; i < this.realEigenvalues.length; ++i) {
                largestEigenvalueNorm = MathLib.max(largestEigenvalueNorm, eigenvalueNorm(i));
            }
            // Corner case: zero matrix, all exactly 0 eigenvalues
            if (largestEigenvalueNorm == 0.0) {
                return false;
            }
            for (int i = 0; i < this.realEigenvalues.length; ++i) {
                // Looking for eigenvalues that are 0, where we consider anything much much smaller
                // than the largest eigenvalue to be effectively 0.
                if (Precision.equals(eigenvalueNorm(i) / largestEigenvalueNorm, 0, EPSILON)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * @param i which eigenvalue to find the norm of
         * @return the norm of ith (complex) eigenvalue.
         */
        private double eigenvalueNorm(final int i) {
            final double re = this.realEigenvalues[i];
            final double im = this.imagEigenvalues[i];
            return MathLib.sqrt(re * re + im * im);
        }

        /**
         * Get the inverse of the decomposed matrix.
         *
         * @return the inverse matrix.
         * @throws SingularMatrixException if the decomposed matrix is singular.
         */
        @Override
        public RealMatrix getInverse() {
            if (!isNonSingular()) {
                throw new SingularMatrixException();
            }

            final int m = this.realEigenvalues.length;
            final double[][] invData = new double[m][m];

            for (int i = 0; i < m; ++i) {
                final double[] invI = invData[i];
                for (int j = 0; j < m; ++j) {
                    double invIJ = 0;
                    for (int k = 0; k < m; ++k) {
                        final double[] vK = this.eigenvectors[k].getDataRef();
                        invIJ += vK[i] * vK[j] / this.realEigenvalues[k];
                    }
                    invI[j] = invIJ;
                }
            }
            return MatrixUtils.createRealMatrix(invData);
        }
        
    }
    
    // CHECKSTYLE: resume CommentRatio check
    // CHECKSTYLE: resume NestedBlockDepth check
}
