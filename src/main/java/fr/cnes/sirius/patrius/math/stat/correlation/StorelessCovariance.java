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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.correlation;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathUnsupportedOperationException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;

/**
 * Covariance implementation that does not require input data to be
 * stored in memory. The size of the covariance matrix is specified in the
 * constructor. Specific elements of the matrix are incrementally updated with
 * calls to incrementRow() or increment Covariance().
 * 
 * <p>
 * This class is based on a paper written by Philippe P&eacute;bay: <a
 * href="http://prod.sandia.gov/techlib/access-control.cgi/2008/086212.pdf"> Formulas for Robust, One-Pass Parallel
 * Computation of Covariances and Arbitrary-Order Statistical Moments</a>, 2008, Technical Report SAND2008-6212, Sandia
 * National Laboratories.
 * </p>
 * 
 * <p>
 * Note: the underlying covariance matrix is symmetric, thus only the upper triangular part of the matrix is stored and
 * updated each increment.
 * </p>
 * 
 * @version $Id: StorelessCovariance.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class StorelessCovariance extends Covariance {

    /** the square covariance matrix (upper triangular part) */
    private final StorelessBivariateCovariance[] covMatrix;

    /** dimension of the square covariance matrix */
    private final int dimension;

    /**
     * Create a bias corrected covariance matrix with a given dimension.
     * 
     * @param dim
     *        the dimension of the square covariance matrix
     */
    public StorelessCovariance(final int dim) {
        this(dim, true);
    }

    /**
     * Create a covariance matrix with a given number of rows and columns and the
     * indicated bias correction.
     * 
     * @param dim
     *        the dimension of the covariance matrix
     * @param biasCorrected
     *        if <code>true</code> the covariance estimate is corrected
     *        for bias, i.e. n-1 in the denominator, otherwise there is no bias correction,
     *        i.e. n in the denominator.
     */
    public StorelessCovariance(final int dim, final boolean biasCorrected) {
        super();
        this.dimension = dim;
        this.covMatrix = new StorelessBivariateCovariance[this.dimension * (this.dimension + 1) / 2];
        this.initializeMatrix(biasCorrected);
    }

    /**
     * Initialize the internal two-dimensional array of {@link StorelessBivariateCovariance} instances.
     * 
     * @param biasCorrected
     *        if the covariance estimate shall be corrected for bias
     */
    private void initializeMatrix(final boolean biasCorrected) {
        for (int i = 0; i < this.dimension; i++) {
            for (int j = 0; j < this.dimension; j++) {
                this.setElement(i, j, new StorelessBivariateCovariance(biasCorrected));
            }
        }
    }

    /**
     * Returns the index (i, j) translated into the one-dimensional
     * array used to store the upper triangular part of the symmetric
     * covariance matrix.
     * 
     * @param i
     *        the row index
     * @param j
     *        the column index
     * @return the corresponding index in the matrix array
     */
    private int indexOf(final int i, final int j) {
        return j < i ? i * (i + 1) / 2 + j : j * (j + 1) / 2 + i;
    }

    /**
     * Gets the element at index (i, j) from the covariance matrix
     * 
     * @param i
     *        the row index
     * @param j
     *        the column index
     * @return the {@link StorelessBivariateCovariance} element at the given index
     */
    private StorelessBivariateCovariance getElement(final int i, final int j) {
        return this.covMatrix[this.indexOf(i, j)];
    }

    /**
     * Sets the covariance element at index (i, j) in the covariance matrix
     * 
     * @param i
     *        the row index
     * @param j
     *        the column index
     * @param cov
     *        the {@link StorelessBivariateCovariance} element to be set
     */
    private void setElement(final int i, final int j,
                            final StorelessBivariateCovariance cov) {
        this.covMatrix[this.indexOf(i, j)] = cov;
    }

    /**
     * Get the covariance for an individual element of the covariance matrix.
     * 
     * @param xIndex
     *        row index in the covariance matrix
     * @param yIndex
     *        column index in the covariance matrix
     * @return the covariance of the given element
     * @throws NumberIsTooSmallException
     *         if the number of observations
     *         in the cell is &lt; 2
     */
    public double getCovariance(final int xIndex,
                                final int yIndex) {

        return this.getElement(xIndex, yIndex).getResult();

    }

    /**
     * Increment the covariance matrix with one row of data.
     * 
     * @param data
     *        array representing one row of data.
     * @throws DimensionMismatchException
     *         if the length of <code>rowData</code> does not match with the covariance matrix
     */
    public void increment(final double[] data) {

        final int length = data.length;
        if (length != this.dimension) {
            throw new DimensionMismatchException(length, this.dimension);
        }

        // only update the upper triangular part of the covariance matrix
        // as only these parts are actually stored
        for (int i = 0; i < length; i++) {
            for (int j = i; j < length; j++) {
                this.getElement(i, j).increment(data[i], data[j]);
            }
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @throws NumberIsTooSmallException
     *         if the number of observations
     *         in a cell is &lt; 2
     */
    @Override
    public RealMatrix getCovarianceMatrix() {
        return MatrixUtils.createRealMatrix(this.getData(), false);
    }

    /**
     * Return the covariance matrix as two-dimensional array.
     * 
     * @return a two-dimensional double array of covariance values
     * @throws NumberIsTooSmallException
     *         if the number of observations
     *         for a cell is &lt; 2
     */
    public double[][] getData() {
        final double[][] data = new double[this.dimension][this.dimension];
        for (int i = 0; i < this.dimension; i++) {
            for (int j = 0; j < this.dimension; j++) {
                data[i][j] = this.getElement(i, j).getResult();
            }
        }
        return data;
    }

    /**
     * This {@link Covariance} method is not supported by a {@link StorelessCovariance},
     * since the number of bivariate observations does not have to be the same for different
     * pairs of covariates - i.e., N as defined in {@link Covariance#getN()} is undefined.
     * 
     * @return nothing as this implementation always throws a {@link MathUnsupportedOperationException}
     * @throws MathUnsupportedOperationException
     *         in all cases
     */
    @Override
    public int getN() {
        throw new MathUnsupportedOperationException();
    }
}
