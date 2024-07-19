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
package fr.cnes.sirius.patrius.math.stat.correlation;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Mean;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.Variance;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computes covariances for pairs of arrays or columns of a matrix.
 * 
 * <p>
 * The constructors that take <code>RealMatrix</code> or <code>double[][]</code> arguments generate covariance matrices.
 * The columns of the input matrices are assumed to represent variable values.
 * </p>
 * 
 * <p>
 * The constructor argument <code>biasCorrected</code> determines whether or not computed covariances are
 * bias-corrected.
 * </p>
 * 
 * <p>
 * Unbiased covariances are given by the formula
 * </p>
 * <code>cov(X, Y) = &Sigma;[(x<sub>i</sub> - E(X))(y<sub>i</sub> - E(Y))] / (n - 1)</code> where <code>E(X)</code> is
 * the mean of <code>X</code> and <code>E(Y)</code> is the mean of the <code>Y</code> values.
 * 
 * <p>
 * Non-bias-corrected estimates use <code>n</code> in place of <code>n - 1</code>
 * 
 * @version $Id: Covariance.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class Covariance {

    /** covariance matrix */
    private final RealMatrix covarianceMatrix;

    /**
     * Create an empty covariance matrix.
     */
    /** Number of observations (length of covariate vectors) */
    private final int n;

    /**
     * Create a Covariance with no data
     */
    public Covariance() {
        super();
        this.covarianceMatrix = null;
        this.n = 0;
    }

    /**
     * Create a Covariance matrix from a rectangular array
     * whose columns represent covariates.
     * 
     * <p>
     * The <code>biasCorrected</code> parameter determines whether or not covariance estimates are bias-corrected.
     * </p>
     * 
     * <p>
     * The input array must be rectangular with at least two columns and two rows.
     * </p>
     * 
     * @param data
     *        rectangular array with columns representing covariates
     * @param biasCorrected
     *        true means covariances are bias-corrected
     * @throws MathIllegalArgumentException
     *         if the input data array is not
     *         rectangular with at least two rows and two columns.
     */
    public Covariance(final double[][] data, final boolean biasCorrected) {
        this(new BlockRealMatrix(data), biasCorrected);
    }

    /**
     * Create a Covariance matrix from a rectangular array
     * whose columns represent covariates.
     * 
     * <p>
     * The input array must be rectangular with at least two columns and two rows
     * </p>
     * 
     * @param data
     *        rectangular array with columns representing covariates
     * @throws MathIllegalArgumentException
     *         if the input data array is not
     *         rectangular with at least two rows and two columns.
     */
    public Covariance(final double[][] data) {
        this(data, true);
    }

    /**
     * Create a covariance matrix from a matrix whose columns
     * represent covariates.
     * 
     * <p>
     * The <code>biasCorrected</code> parameter determines whether or not covariance estimates are bias-corrected.
     * </p>
     * 
     * <p>
     * The matrix must have at least two columns and two rows
     * </p>
     * 
     * @param matrix
     *        matrix with columns representing covariates
     * @param biasCorrected
     *        true means covariances are bias-corrected
     * @throws MathIllegalArgumentException
     *         if the input matrix does not have
     *         at least two rows and two columns
     */
    public Covariance(final RealMatrix matrix, final boolean biasCorrected) {
        this.checkSufficientData(matrix);
        this.n = matrix.getRowDimension();
        this.covarianceMatrix = this.computeCovarianceMatrix(matrix, biasCorrected);
    }

    /**
     * Create a covariance matrix from a matrix whose columns
     * represent covariates.
     * 
     * <p>
     * The matrix must have at least two columns and two rows
     * </p>
     * 
     * @param matrix
     *        matrix with columns representing covariates
     * @throws MathIllegalArgumentException
     *         if the input matrix does not have
     *         at least two rows and two columns
     */
    public Covariance(final RealMatrix matrix) {
        this(matrix, true);
    }

    /**
     * Returns the covariance matrix
     * 
     * @return covariance matrix
     */
    public RealMatrix getCovarianceMatrix() {
        return this.covarianceMatrix;
    }

    /**
     * Returns the number of observations (length of covariate vectors)
     * 
     * @return number of observations
     */
    public int getN() {
        return this.n;
    }

    /**
     * Compute a covariance matrix from a matrix whose columns represent
     * covariates.
     * 
     * @param matrix
     *        input matrix (must have at least two columns and two rows)
     * @param biasCorrected
     *        determines whether or not covariance estimates are bias-corrected
     * @return covariance matrix
     * @throws MathIllegalArgumentException
     *         if the matrix does not contain sufficient data
     */
    protected RealMatrix computeCovarianceMatrix(final RealMatrix matrix, final boolean biasCorrected) {
        // get column dimension
        final int dimension = matrix.getColumnDimension();
        // initialize variance and output matrix
        final Variance variance = new Variance(biasCorrected);
        final RealMatrix outMatrix = new BlockRealMatrix(dimension, dimension);
        for (int i = 0; i < dimension; i++) {
            // compute covariance values for the column
            for (int j = 0; j < i; j++) {
                final double cov = this.covariance(matrix.getColumn(i), matrix.getColumn(j), biasCorrected);
                outMatrix.setEntry(i, j, cov);
                outMatrix.setEntry(j, i, cov);
            }
            // set diagonal values from variance
            outMatrix.setEntry(i, i, variance.evaluate(matrix.getColumn(i)));
        }
        return outMatrix;
    }

    /**
     * Create a covariance matrix from a matrix whose columns represent
     * covariates. Covariances are computed using the bias-corrected formula.
     * 
     * @param matrix
     *        input matrix (must have at least two columns and two rows)
     * @return covariance matrix
     * @throws MathIllegalArgumentException
     *         if matrix does not contain sufficient data
     * @see #Covariance
     */
    protected RealMatrix computeCovarianceMatrix(final RealMatrix matrix) {
        return this.computeCovarianceMatrix(matrix, true);
    }

    /**
     * Compute a covariance matrix from a rectangular array whose columns represent
     * covariates.
     * 
     * @param data
     *        input array (must have at least two columns and two rows)
     * @param biasCorrected
     *        determines whether or not covariance estimates are bias-corrected
     * @return covariance matrix
     * @throws MathIllegalArgumentException
     *         if the data array does not contain sufficient
     *         data
     */
    protected RealMatrix computeCovarianceMatrix(final double[][] data, final boolean biasCorrected) {
        return this.computeCovarianceMatrix(new BlockRealMatrix(data), biasCorrected);
    }

    /**
     * Create a covariance matrix from a rectangular array whose columns represent
     * covariates. Covariances are computed using the bias-corrected formula.
     * 
     * @param data
     *        input array (must have at least two columns and two rows)
     * @return covariance matrix
     * @throws MathIllegalArgumentException
     *         if the data array does not contain sufficient data
     * @see #Covariance
     */
    protected RealMatrix computeCovarianceMatrix(final double[][] data) {
        return this.computeCovarianceMatrix(data, true);
    }

    /**
     * Computes the covariance between the two arrays.
     * 
     * <p>
     * Array lengths must match and the common length must be at least 2.
     * </p>
     * 
     * @param xArray
     *        first data array
     * @param yArray
     *        second data array
     * @param biasCorrected
     *        if true, returned value will be bias-corrected
     * @return returns the covariance for the two arrays
     * @throws MathIllegalArgumentException
     *         if the arrays lengths do not match or
     *         there is insufficient data
     */
    public double covariance(final double[] xArray, final double[] yArray, final boolean biasCorrected) {
        // Initialization
        final Mean mean = new Mean();
        double result = 0d;
        final int length = xArray.length;
        if (length != yArray.length) {
            // Exception
            throw new MathIllegalArgumentException(
                PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE, length, yArray.length);
        } else if (length < 2) {
            // Exception
            throw new MathIllegalArgumentException(
                PatriusMessages.INSUFFICIENT_OBSERVED_POINTS_IN_SAMPLE, length, 2);
        } else {
            // General case
            final double xMean = mean.evaluate(xArray);
            final double yMean = mean.evaluate(yArray);
            for (int i = 0; i < length; i++) {
                final double xDev = xArray[i] - xMean;
                final double yDev = yArray[i] - yMean;
                result += (xDev * yDev - result) / (i + 1);
            }
        }
        return biasCorrected ? result * ((double) length / (double) (length - 1)) : result;
    }

    /**
     * Computes the covariance between the two arrays, using the bias-corrected
     * formula.
     * 
     * <p>
     * Array lengths must match and the common length must be at least 2.
     * </p>
     * 
     * @param xArray
     *        first data array
     * @param yArray
     *        second data array
     * @return returns the covariance for the two arrays
     * @throws MathIllegalArgumentException
     *         if the arrays lengths do not match or
     *         there is insufficient data
     */
    public double covariance(final double[] xArray, final double[] yArray) {
        return this.covariance(xArray, yArray, true);
    }

    /**
     * Throws MathIllegalArgumentException if the matrix does not have at least
     * two columns and two rows.
     * 
     * @param matrix
     *        matrix to check
     * @throws MathIllegalArgumentException
     *         if the matrix does not contain sufficient data
     *         to compute covariance
     */
    private void checkSufficientData(final RealMatrix matrix) {
        final int nRows = matrix.getRowDimension();
        final int nCols = matrix.getColumnDimension();
        if (nRows < 2 || nCols < 2) {
            throw new MathIllegalArgumentException(
                PatriusMessages.INSUFFICIENT_ROWS_AND_COLUMNS,
                nRows, nCols);
        }
    }
}
