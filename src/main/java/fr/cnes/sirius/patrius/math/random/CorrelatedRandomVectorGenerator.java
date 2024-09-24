/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION::DM:291:11/03/2015: add uniformly correlated random vector generation
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.random;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RectangularCholeskyDecomposition;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * A {@link RandomVectorGenerator} that generates vectors with with
 * correlated components.
 * <p>
 * Random vectors with correlated components are built by combining the uncorrelated components of another random vector
 * in such a way that the resulting correlations are the ones specified by a positive definite covariance matrix.
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
 * @version $Id: CorrelatedRandomVectorGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */

public class CorrelatedRandomVectorGenerator
    implements RandomVectorGenerator {
    /** Mean vector. */
    private final double[] mean;
    /** Underlying generator. */
    private final NormalizedRandomGenerator generator;
    /** Storage for the normalized vector. */
    private final double[] normalized;
    /** Root of the covariance matrix. */
    private final RealMatrix root;

    /**
     * Builds a correlated random vector generator from its mean
     * vector and covariance matrix.
     * 
     * @param meanIn
     *        Expected mean values for all components.
     * @param covariance
     *        Covariance matrix.
     * @param small
     *        Diagonal elements threshold under which column are
     *        considered to be dependent on previous ones and are discarded
     * @param generatorIn
     *        underlying generator for uncorrelated normalized
     *        components.
     * @throws fr.cnes.sirius.patrius.math.linear.NonPositiveDefiniteMatrixException
     *         if the covariance matrix is not strictly positive definite.
     * @throws DimensionMismatchException
     *         if the mean and covariance
     *         arrays dimensions do not match.
     */
    public CorrelatedRandomVectorGenerator(final double[] meanIn,
        final RealMatrix covariance, final double small,
        final NormalizedRandomGenerator generatorIn) {
        final int order = covariance.getRowDimension();
        if (meanIn.length != order) {
            throw new DimensionMismatchException(meanIn.length, order);
        }
        this.mean = meanIn.clone();

        // Check the covariance matrix is symmetric
        MatrixUtils.checkSymmetric(covariance, Precision.EPSILON);

        final RectangularCholeskyDecomposition decomposition =
            new RectangularCholeskyDecomposition(covariance, small);
        this.root = decomposition.getRootMatrix();

        this.generator = generatorIn;
        this.normalized = new double[decomposition.getRank()];
    }

    /**
     * Builds a null mean random correlated vector generator from its
     * covariance matrix.
     * 
     * @param covariance
     *        Covariance matrix.
     * @param small
     *        Diagonal elements threshold under which column are
     *        considered to be dependent on previous ones and are discarded.
     * @param generatorIn
     *        Underlying generator for uncorrelated normalized
     *        components.
     * @throws fr.cnes.sirius.patrius.math.linear.NonPositiveDefiniteMatrixException
     *         if the covariance matrix is not strictly positive definite.
     */
    public CorrelatedRandomVectorGenerator(final RealMatrix covariance, final double small,
        final NormalizedRandomGenerator generatorIn) {
        final int order = covariance.getRowDimension();
        this.mean = new double[order];
        for (int i = 0; i < order; ++i) {
            this.mean[i] = 0;
        }

        // Check the covariance matrix is symmetric
        MatrixUtils.checkSymmetric(covariance, Precision.EPSILON);

        final RectangularCholeskyDecomposition decomposition =
            new RectangularCholeskyDecomposition(covariance, small);
        this.root = decomposition.getRootMatrix();

        this.generator = generatorIn;
        this.normalized = new double[decomposition.getRank()];
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
        return this.normalized.length;
    }

    /**
     * Get the root of the covariance matrix.
     * The root is the rectangular matrix <code>B</code> such that
     * the covariance matrix is equal to <code>B.B<sup>T</sup></code>
     * 
     * @return root of the square matrix
     * @see #getRank()
     */
    public RealMatrix getRootMatrix() {
        return this.root;
    }

    /**
     * Generate a correlated random vector.
     * 
     * @return a random vector as an array of double. The returned array
     *         is created at each call, the caller can do what it wants with it.
     */
    @Override
    public double[] nextVector() {

        // generate uncorrelated vector
        for (int i = 0; i < this.normalized.length; ++i) {
            this.normalized[i] = this.generator.nextNormalizedDouble();
        }

        // initialize correlated vector
        final double[] correlated = new double[this.mean.length];
        // compute correlated vector
        for (int i = 0; i < correlated.length; ++i) {
            correlated[i] = this.mean[i];
            for (int j = 0; j < this.root.getColumnDimension(); ++j) {
                correlated[i] += this.root.getEntry(i, j) * this.normalized[j];
            }
        }

        // return computed vector
        return correlated;

    }

}
