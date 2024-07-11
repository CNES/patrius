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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.EigenDecomposition;
import fr.cnes.sirius.patrius.math.linear.NonPositiveDefiniteMatrixException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.SingularMatrixException;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;
import fr.cnes.sirius.patrius.math.random.Well19937c;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Implementation of the multivariate normal (Gaussian) distribution.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Multivariate_normal_distribution"> Multivariate normal distribution
 *      (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/MultivariateNormalDistribution.html"> Multivariate normal distribution
 *      (MathWorld)</a>
 * 
 * @version $Id: MultivariateNormalDistribution.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.1
 */
public class MultivariateNormalDistribution
    extends AbstractMultivariateRealDistribution {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Vector of means. */
    private final double[] means;
    /** Covariance matrix. */
    private final RealMatrix covarianceMatrix;
    /** The matrix inverse of the covariance matrix. */
    private final RealMatrix covarianceMatrixInverse;
    /** The determinant of the covariance matrix. */
    private final double covarianceMatrixDeterminant;
    /** Matrix used in computation of samples. */
    private final RealMatrix samplingMatrix;

    /**
     * Creates a multivariate normal distribution with the given mean vector and
     * covariance matrix. <br/>
     * The number of dimensions is equal to the length of the mean vector
     * and to the number of rows and columns of the covariance matrix.
     * It is frequently written as "p" in formulae.
     * 
     * @param meansIn
     *        Vector of means.
     * @param covariances
     *        Covariance matrix.
     * @throws DimensionMismatchException
     *         if the arrays length are
     *         inconsistent.
     * @throws SingularMatrixException
     *         if the eigenvalue decomposition cannot
     *         be performed on the provided covariance matrix.
     * @throws NonPositiveDefiniteMatrixException
     *         if any of the eigenvalues is
     *         negative.
     */
    public MultivariateNormalDistribution(final double[] meansIn,
        final double[][] covariances) {
        this(new Well19937c(), meansIn, covariances);
    }

    /**
     * Creates a multivariate normal distribution with the given mean vector and
     * covariance matrix. <br/>
     * The number of dimensions is equal to the length of the mean vector
     * and to the number of rows and columns of the covariance matrix.
     * It is frequently written as "p" in formulae.
     * 
     * @param rng
     *        Random Number Generator.
     * @param meansIn
     *        Vector of means.
     * @param covariances
     *        Covariance matrix.
     * @throws DimensionMismatchException
     *         if the arrays length are
     *         inconsistent.
     * @throws SingularMatrixException
     *         if the eigenvalue decomposition cannot
     *         be performed on the provided covariance matrix.
     * @throws NonPositiveDefiniteMatrixException
     *         if any of the eigenvalues is
     *         negative.
     */
    public MultivariateNormalDistribution(final RandomGenerator rng,
        final double[] meansIn,
        final double[][] covariances) {
        super(rng, meansIn.length);

        final int dim = meansIn.length;

        if (covariances.length != dim) {
            throw new DimensionMismatchException(covariances.length, dim);
        }

        for (int i = 0; i < dim; i++) {
            if (dim != covariances[i].length) {
                throw new DimensionMismatchException(covariances[i].length, dim);
            }
        }

        this.means = MathArrays.copyOf(meansIn);

        this.covarianceMatrix = new Array2DRowRealMatrix(covariances);

        // Covariance matrix eigen decomposition.
        final EigenDecomposition covMatDec = new EigenDecomposition(this.covarianceMatrix);

        // Compute and store the inverse.
        this.covarianceMatrixInverse = covMatDec.getSolver().getInverse();
        // Compute and store the determinant.
        this.covarianceMatrixDeterminant = covMatDec.getDeterminant();

        // Eigenvalues of the covariance matrix.
        final double[] covMatEigenvalues = covMatDec.getRealEigenvalues();

        for (int i = 0; i < covMatEigenvalues.length; i++) {
            if (covMatEigenvalues[i] < 0) {
                throw new NonPositiveDefiniteMatrixException(covMatEigenvalues[i], i, 0);
            }
        }

        // Matrix where each column is an eigenvector of the covariance matrix.
        final Array2DRowRealMatrix covMatEigenvectors = new Array2DRowRealMatrix(dim, dim);
        for (int v = 0; v < dim; v++) {
            final double[] evec = covMatDec.getEigenvector(v).toArray();
            covMatEigenvectors.setColumn(v, evec);
        }

        final RealMatrix tmpMatrix = covMatEigenvectors.transpose();

        // Scale each eigenvector by the square root of its eigenvalue.
        for (int row = 0; row < dim; row++) {
            final double factor = MathLib.sqrt(covMatEigenvalues[row]);
            for (int col = 0; col < dim; col++) {
                tmpMatrix.multiplyEntry(row, col, factor);
            }
        }

        this.samplingMatrix = covMatEigenvectors.multiply(tmpMatrix);
    }

    /**
     * Gets the mean vector.
     * 
     * @return the mean vector.
     */
    public double[] getMeans() {
        return MathArrays.copyOf(this.means);
    }

    /**
     * Gets the covariance matrix.
     * 
     * @return the covariance matrix.
     */
    public RealMatrix getCovariances() {
        return this.covarianceMatrix.copy();
    }

    /** {@inheritDoc} */
    @Override
    public double density(final double[] vals) {
        final int dim = this.getDimension();
        if (vals.length != dim) {
            throw new DimensionMismatchException(vals.length, dim);
        }

        return MathLib.pow(2 * FastMath.PI, -dim / 2) *
            MathLib.pow(this.covarianceMatrixDeterminant, -HALF) *
            this.getExponentTerm(vals);
    }

    /**
     * Gets the square root of each element on the diagonal of the covariance
     * matrix.
     * 
     * @return the standard deviations.
     */
    public double[] getStandardDeviations() {
        final int dim = this.getDimension();
        final double[] std = new double[dim];
        for (int i = 0; i < dim; i++) {
            std[i] = MathLib.sqrt(this.covarianceMatrix.getEntry(i, i));
        }
        return std;
    }

    /** {@inheritDoc} */
    @Override
    public double[] sample() {
        // Get sample dimension
        final int dim = this.getDimension();
        // Initialize normal values
        final double[] normalVals = new double[dim];

        for (int i = 0; i < dim; i++) {
            normalVals[i] = this.random.nextGaussian();
        }

        // Initialize final values
        final double[] vals = this.samplingMatrix.operate(normalVals);

        for (int i = 0; i < dim; i++) {
            vals[i] += this.means[i];
        }

        return vals;
    }

    /**
     * Computes the term used in the exponent (see definition of the distribution).
     * 
     * @param values
     *        Values at which to compute density.
     * @return the multiplication factor of density calculations.
     */
    private double getExponentTerm(final double[] values) {
        // Initialize centered array
        final double[] centered = new double[values.length];
        for (int i = 0; i < centered.length; i++) {
            centered[i] = values[i] - this.getMeans()[i];
        }
        // Pre-multiply matrix inverse of covariance matrix
        final double[] preMultiplied = this.covarianceMatrixInverse.preMultiply(centered);
        double sum = 0;
        for (int i = 0; i < preMultiplied.length; i++) {
            sum += preMultiplied[i] * centered[i];
        }
        // return multiplication factor of density calculations
        return MathLib.exp(-HALF * sum);
    }
}
