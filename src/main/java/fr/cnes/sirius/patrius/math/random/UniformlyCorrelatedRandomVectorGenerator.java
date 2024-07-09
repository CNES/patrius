/**
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
 * 
 *
 * @history creation 11/03/2015
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:291:11/03/2015: add uniformly correlated random vector generation
 * VERSION::FA:467:03/11/2015: suppression of clone() for the attribute mean in first constructor
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.random;

import java.util.Locale;

import fr.cnes.sirius.patrius.math.distribution.NormalDistribution;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.EigenDecomposition;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.NonSquareMatrixException;
import fr.cnes.sirius.patrius.math.linear.NonSymmetricMatrixException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RectangularCholeskyDecomposition;
import fr.cnes.sirius.patrius.math.stat.StatUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * A {@link RandomVectorGenerator} that generates vectors with with
 * correlated components.
 * <p>
 * Random vectors with correlated components are built by combining the uncorrelated components of another random vector
 * in such a way that the resulting correlations are the ones specified by a positive definite correlation matrix.
 * </p>
 * <p>
 * The main use for correlated random vector generation is for Monte-Carlo simulation of physical problems with several
 * variables, for example to generate error vectors to be added to a nominal vector. A particularly interesting case is
 * when the generated vector should be drawn from a <a
 * href="http://en.wikipedia.org/wiki/Multivariate_normal_distribution"> Multivariate Normal Distribution</a>. The
 * approach using a Cholesky decomposition is quite usual in this case. However, it can be extended to other cases as
 * long as the underlying random generator provides {@link NormalizedRandomGenerator normalized values} like
 * {@link GaussianRandomGenerator} or {@link UniformRandomGenerator}.
 * </p>
 * <p>
 * Sometimes, the covariance matrix for a given simulation is not strictly positive definite. This means that the
 * correlations are not all independent from each other. In this case, however, the non strictly positive elements found
 * during the Cholesky decomposition of the covariance matrix should not be negative either, they should be null.
 * Another non-conventional extension handling this case is used here. Rather than computing
 * <code>C = U<sup>T</sup>.U</code> where <code>C</code> is the covariance matrix and <code>U</code> is an
 * upper-triangular matrix, we compute <code>C = B.B<sup>T</sup></code> where <code>B</code> is a rectangular matrix
 * having more rows than columns. The number of columns of <code>B</code> is the rank of the covariance matrix, and it
 * is the dimension of the uncorrelated random vector that is needed to compute the component of the correlated vector.
 * This class handles this situation automatically.
 * </p>
 * 
 * @version $Id: UniformlyCorrelatedRandomVectorGenerator.java 17583 2017-05-10 13:05:10Z bignon $
 * @concurrency not thread-safe
 * @since 3.0
 */

public class UniformlyCorrelatedRandomVectorGenerator implements RandomVectorGenerator {

    /** Gaussian to uniform const. */
    private static final double GAUSS_TO_UNIFORM = MathLib.sqrt(12);

    /** Mean vector. */
    private final double[] mean;

    /** Underlying generator. */
    private final NormalizedRandomGenerator generator;

    /** Standard deviation vector. */
    private final double[] standardDeviation;

    /** Root of the covariance matrix. */
    private final RealMatrix root;

    /**
     * Builds a correlated random vector generator from its mean vector and covariance matrix.
     * 
     * @param aMean
     *        Expected mean values for all components.
     * @param covariance
     *        Covariance matrix.
     * @param small
     *        Diagonal elements threshold under which column are
     *        considered to be dependent on previous ones and are discarded
     * @param aGenerator
     *        underlying generator for uncorrelated normalized components.
     * @throws DimensionMismatchException
     *         thrown if mean vector is not of the same dimension as covariance matrix
     * @throws NonSquareMatrixException
     *         if the covariance matrix is not square
     * @throws NonSymmetricMatrixException
     *         if the covariance matrix is not symmetric
     */
    public UniformlyCorrelatedRandomVectorGenerator(final double[] aMean, final RealMatrix covariance,
        final double small, final NormalizedRandomGenerator aGenerator) {

        final int order = covariance.getRowDimension();
        if (aMean.length != order) {
            throw new DimensionMismatchException(aMean.length, order);
        }
        this.mean = aMean;
        this.generator = aGenerator;

        // Check the covariance matrix is symmetric
        MatrixUtils.checkSymmetric(covariance, Precision.EPSILON);

        // Compute standard deviation vector and root matrix
        this.standardDeviation = this.computeStandardDeviation(covariance);
        final RealMatrix correlationMatrix = this.computeCorrelationMatrix(covariance);
        final RectangularCholeskyDecomposition decomposition =
            new RectangularCholeskyDecomposition(correlationMatrix, small);
        this.root = decomposition.getRootMatrix();
    }

    /**
     * Builds a null mean random correlated vector generator from its covariance matrix.
     * 
     * @param covariance
     *        Covariance matrix.
     * @param small
     *        Diagonal elements threshold under which column are
     *        considered to be dependent on previous ones and are discarded.
     * @param aGenerator
     *        Underlying generator for uncorrelated normalized components.
     */
    public UniformlyCorrelatedRandomVectorGenerator(final RealMatrix covariance, final double small,
        final NormalizedRandomGenerator aGenerator) {
        this(new double[covariance.getRowDimension()], covariance, small, aGenerator);
    }

    /**
     * Compute standard deviation vector given a covariance matrix.
     * 
     * @param covarianceMatrix
     *        covariance matrix
     * @return standard deviation vector
     */
    private double[] computeStandardDeviation(final RealMatrix covarianceMatrix) {

        // Initialization
        final int size = covarianceMatrix.getColumnDimension();
        final double[] stdVector = new double[size];

        // Retrieve all terms
        for (int i = 0; i < size; i++) {

            final double covarianceValue = covarianceMatrix.getEntry(i, i);

            if (covarianceValue < 0) {
                throw new IllegalArgumentException(
                    PatriusMessages.NOT_A_COVARIANCE_MATRIX.getLocalizedString(Locale.getDefault()));
            } else if (covarianceValue == 0) {
                // Set to 0
                stdVector[i] = 0;
            } else {
                // Standard deviation is root of covariance value
                stdVector[i] = MathLib.sqrt(covarianceValue);
            }
        }

        return stdVector;
    }

    /**
     * Compute correlation matrix given a covariance matrix.
     * 
     * @param covarianceMatrix
     *        covariance matrix
     * @return correlation matrix
     */
    private RealMatrix computeCorrelationMatrix(final RealMatrix covarianceMatrix) {

        // Get standard deviation vector inverse
        final double[] standardDeviationInv = new double[this.standardDeviation.length];
        for (int i = 0; i < standardDeviationInv.length; i++) {
            if (this.standardDeviation[i] != 0) {
                standardDeviationInv[i] = MathLib.divide(1., this.standardDeviation[i]);
            }
        }

        final RealMatrix sdInvMatrix = MatrixUtils.createRealDiagonalMatrix(standardDeviationInv);

        // Compute correlation matrix
        final RealMatrix corMatrix = sdInvMatrix.multiply(covarianceMatrix.multiply(sdInvMatrix));

        // Check correlation matrix is definite semi-positive
        final EigenDecomposition eigen = new EigenDecomposition(corMatrix);
        final double minEigenValue = StatUtils.min(eigen.getRealEigenvalues());

        if (minEigenValue < 0) {
            // Correlation matrix is not definite semi-positive (positive eigen values)
            throw new IllegalArgumentException(
                PatriusMessages.NOT_A_COVARIANCE_MATRIX.getLocalizedString(Locale.getDefault()));
        }

        // Initialize final used matrix
        RealMatrix finalMatrix = corMatrix;

        // Apply Spearman correction
        final int corMatrixSize = corMatrix.getColumnDimension();
        final double[][] correctedMatrix = new double[corMatrixSize][corMatrixSize];
        for (int i = 0; i < corMatrixSize; i++) {
            for (int j = 0; j < corMatrixSize; j++) {
                if (i == j) {
                    correctedMatrix[i][j] = corMatrix.getEntry(i, j);
                } else {
                    correctedMatrix[i][j] = 2 * MathLib.sin(corMatrix.getEntry(i, j) * FastMath.PI / 6.);
                }
            }
        }

        // Check corrected correlation matrix is definite semi-positive
        final BlockRealMatrix matrix2 = new BlockRealMatrix(correctedMatrix);
        final EigenDecomposition eigen2 = new EigenDecomposition(matrix2);
        final double minEigenValue2 = StatUtils.min(eigen2.getRealEigenvalues());

        if (minEigenValue2 > 0) {
            // Use corrected matrix if definite positive
            finalMatrix = new BlockRealMatrix(correctedMatrix);
        }

        return finalMatrix;
    }

    /**
     * Get the underlying normalized components generator.
     * 
     * @return underlying uncorrelated components generator
     */
    public NormalizedRandomGenerator getGenerator() {
        return this.generator;
    }

    /**
     * Get the rank of the covariance matrix.
     * The rank is the number of independent rows in the covariance
     * matrix, it is also the number of columns of the root matrix.
     * 
     * @return rank of the square matrix.
     * @see #getRootMatrix()
     */
    public int getRank() {
        return this.root.getColumnDimension();
    }

    /**
     * Get the root of the <b>correlation</b> matrix.
     * The root is the rectangular matrix <code>B</code> such that
     * the <b>correlation</b> matrix is equal to <code>B.B<sup>T</sup></code>
     * 
     * @return root of the <b>correlation</b> matrix
     * @see #getRank()
     */
    public RealMatrix getRootMatrix() {
        return this.root;
    }

    /**
     * Get the standard deviation vector.
     * The standard deviation vector is square root of the covariance diagonal elements.
     * 
     * @return standard deviation vector
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getStandardDeviationVector() {
        return this.standardDeviation;
    }

    /**
     * Generate a correlated random vector.
     * 
     * @return a random vector as an array of double. The returned array
     *         is created at each call, the caller can do what it wants with it.
     */
    @Override
    public double[] nextVector() {

        // Generate uncorrelated vector (size is rank of the root matrix)
        final double[] normalized = new double[this.getRank()];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = this.generator.nextNormalizedDouble();
        }

        // Compute dispersed vector
        final double[] lt = this.root.operate(normalized);

        final double[] z = new double[lt.length];
        // new normal distribution (mean = 0, sigma = 1)
        final NormalDistribution normalDistribution = new NormalDistribution();
        for (int i = 0; i < lt.length; i++) {
            z[i] = GAUSS_TO_UNIFORM * (normalDistribution.cumulativeProbability(lt[i]) - 1. / 2.);
        }

        // Compute correlated vector ( = mean + sigma * z)
        final double[] correlated = new double[this.mean.length];
        for (int i = 0; i < correlated.length; i++) {
            correlated[i] = this.mean[i] + this.standardDeviation[i] * z[i];
        }

        // Return computed correlated vector
        return correlated;
    }

}
