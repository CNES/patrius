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

import fr.cnes.sirius.patrius.math.distribution.TDistribution;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.stat.regression.SimpleRegression;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computes Pearson's product-moment correlation coefficients for pairs of arrays
 * or columns of a matrix.
 * 
 * <p>
 * The constructors that take <code>RealMatrix</code> or <code>double[][]</code> arguments generate correlation
 * matrices. The columns of the input matrices are assumed to represent variable values. Correlations are given by the
 * formula
 * </p>
 * <code>cor(X, Y) = &Sigma;[(x<sub>i</sub> - E(X))(y<sub>i</sub> - E(Y))] / [(n - 1)s(X)s(Y)]</code> where
 * <code>E(X)</code> is the mean of <code>X</code>, <code>E(Y)</code> is the mean of the <code>Y</code> values and s(X),
 * s(Y) are standard deviations.
 * 
 * @version $Id: PearsonsCorrelation.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings({"PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
public class PearsonsCorrelation {

    /** correlation matrix */
    private final RealMatrix correlationMatrix;

    /** number of observations */
    private final int nObs;

    /**
     * Create a PearsonsCorrelation instance without data
     */
    public PearsonsCorrelation() {
        super();
        this.correlationMatrix = null;
        this.nObs = 0;
    }

    /**
     * Create a PearsonsCorrelation from a rectangular array
     * whose columns represent values of variables to be correlated.
     * 
     * @param data
     *        rectangular array with columns representing variables
     * @throws IllegalArgumentException
     *         if the input data array is not
     *         rectangular with at least two rows and two columns.
     */
    public PearsonsCorrelation(final double[][] data) {
        this(new BlockRealMatrix(data));
    }

    /**
     * Create a PearsonsCorrelation from a RealMatrix whose columns
     * represent variables to be correlated.
     * 
     * @param matrix
     *        matrix with columns representing variables to correlate
     */
    public PearsonsCorrelation(final RealMatrix matrix) {
        checkSufficientData(matrix);
        this.nObs = matrix.getRowDimension();
        this.correlationMatrix = this.computeCorrelationMatrix(matrix);
    }

    /**
     * Create a PearsonsCorrelation from a {@link Covariance}. The correlation
     * matrix is computed by scaling the Covariance's covariance matrix.
     * The Covariance instance must have been created from a data matrix with
     * columns representing variable values.
     * 
     * @param covariance
     *        Covariance instance
     */
    public PearsonsCorrelation(final Covariance covariance) {
        final RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
        if (covarianceMatrix == null) {
            throw new NullArgumentException(PatriusMessages.COVARIANCE_MATRIX);
        }
        this.nObs = covariance.getN();
        this.correlationMatrix = this.covarianceToCorrelation(covarianceMatrix);
    }

    /**
     * Create a PearsonsCorrelation from a covariance matrix. The correlation
     * matrix is computed by scaling the covariance matrix.
     * 
     * @param covarianceMatrix
     *        covariance matrix
     * @param numberOfObservations
     *        the number of observations in the dataset used to compute
     *        the covariance matrix
     */
    public PearsonsCorrelation(final RealMatrix covarianceMatrix, final int numberOfObservations) {
        this.nObs = numberOfObservations;
        this.correlationMatrix = this.covarianceToCorrelation(covarianceMatrix);

    }

    /**
     * Returns the correlation matrix
     * 
     * @return correlation matrix
     */
    public RealMatrix getCorrelationMatrix() {
        return this.correlationMatrix;
    }

    /**
     * Returns a matrix of standard errors associated with the estimates
     * in the correlation matrix.<br/>
     * <code>getCorrelationStandardErrors().getEntry(i,j)</code> is the standard
     * error associated with <code>getCorrelationMatrix.getEntry(i,j)</code>
     * <p>
     * The formula used to compute the standard error is <br/>
     * <code>SE<sub>r</sub> = ((1 - r<sup>2</sup>) / (n - 2))<sup>1/2</sup></code> where <code>r</code> is the estimated
     * correlation coefficient and <code>n</code> is the number of observations in the source dataset.
     * </p>
     * 
     * @return matrix of correlation standard errors
     */
    public RealMatrix getCorrelationStandardErrors() {
        // get column dimension of correlation matrix
        final int nVars = this.correlationMatrix.getColumnDimension();
        // initialize output column array
        final double[][] out = new double[nVars][nVars];
        for (int i = 0; i < nVars; i++) {
            for (int j = 0; j < nVars; j++) {
                // compute standard error for each entry of the correlation matrix
                final double r = this.correlationMatrix.getEntry(i, j);
                out[i][j] = MathLib.sqrt((1 - r * r) / (this.nObs - 2));
            }
        }
        return new BlockRealMatrix(out);
    }

    /**
     * Returns a matrix of p-values associated with the (two-sided) null
     * hypothesis that the corresponding correlation coefficient is zero.
     * <p>
     * <code>getCorrelationPValues().getEntry(i,j)</code> is the probability that a random variable distributed as
     * <code>t<sub>n-2</sub></code> takes a value with absolute value greater than or equal to <br>
     * <code>|r|((n - 2) / (1 - r<sup>2</sup>))<sup>1/2</sup></code>
     * </p>
     * <p>
     * The values in the matrix are sometimes referred to as the <i>significance</i> of the corresponding correlation
     * coefficients.
     * </p>
     * 
     * @return matrix of p-values
     * @throws fr.cnes.sirius.patrius.math.exception.MaxCountExceededException
     *         if an error occurs estimating probabilities
     */
    public RealMatrix getCorrelationPValues() {
        // create t distribution
        final TDistribution tDistribution = new TDistribution(this.nObs - 2);
        final int nVars = this.correlationMatrix.getColumnDimension();
        final double[][] out = new double[nVars][nVars];
        for (int i = 0; i < nVars; i++) {
            for (int j = 0; j < nVars; j++) {
                if (i == j) {
                    // zeros on the diagonal
                    out[i][j] = 0d;
                } else {
                    // compute p-value
                    final double r = this.correlationMatrix.getEntry(i, j);
                    final double t = MathLib.abs(r * MathLib.sqrt((this.nObs - 2) / (1 - r * r)));
                    out[i][j] = 2 * tDistribution.cumulativeProbability(-t);
                }
            }
        }
        // create and return real matrix from computed output data
        return new BlockRealMatrix(out);
    }

    /**
     * Computes the correlation matrix for the columns of the
     * input matrix.
     * 
     * @param matrix
     *        matrix with columns representing variables to correlate
     * @return correlation matrix
     */
    public RealMatrix computeCorrelationMatrix(final RealMatrix matrix) {
        // get column dimension
        final int nVars = matrix.getColumnDimension();
        // initialize output matrix
        final RealMatrix outMatrix = new BlockRealMatrix(nVars, nVars);
        for (int i = 0; i < nVars; i++) {
            for (int j = 0; j < i; j++) {
                // compute correlation for the columns of the input matrix
                final double corr = this.correlation(matrix.getColumn(i), matrix.getColumn(j));
                outMatrix.setEntry(i, j, corr);
                outMatrix.setEntry(j, i, corr);
            }
            // set values for the diagonal
            outMatrix.setEntry(i, i, 1d);
        }
        return outMatrix;
    }

    /**
     * Computes the correlation matrix for the columns of the
     * input rectangular array. The colums of the array represent values
     * of variables to be correlated.
     * 
     * @param data
     *        matrix with columns representing variables to correlate
     * @return correlation matrix
     */
    public RealMatrix computeCorrelationMatrix(final double[][] data) {
        return this.computeCorrelationMatrix(new BlockRealMatrix(data));
    }

    /**
     * Computes the Pearson's product-moment correlation coefficient between the two arrays.
     * 
     * </p>Throws IllegalArgumentException if the arrays do not have the same length
     * or their common length is less than 2</p>
     * 
     * @param xArray
     *        first data array
     * @param yArray
     *        second data array
     * @return Returns Pearson's correlation coefficient for the two arrays
     * @throws DimensionMismatchException
     *         if the arrays lengths do not match
     * @throws MathIllegalArgumentException
     *         if there is insufficient data
     */
    public double correlation(final double[] xArray, final double[] yArray) {
        final SimpleRegression regression = new SimpleRegression();
        if (xArray.length != yArray.length) {
            // Exception
            throw new DimensionMismatchException(xArray.length, yArray.length);
        } else if (xArray.length < 2) {
            // Exception
            throw new MathIllegalArgumentException(PatriusMessages.INSUFFICIENT_DIMENSION,
                xArray.length, 2);
        } else {
            // complete the regression dataset
            for (int i = 0; i < xArray.length; i++) {
                regression.addData(xArray[i], yArray[i]);
            }
            return regression.getR();
        }
    }

    /**
     * Derives a correlation matrix from a covariance matrix.
     * 
     * <p>
     * Uses the formula <br/>
     * <code>r(X,Y) = cov(X,Y)/s(X)s(Y)</code> where <code>r(&middot,&middot;)</code> is the correlation coefficient and
     * <code>s(&middot;)</code> means standard deviation.
     * </p>
     * 
     * @param covarianceMatrix
     *        the covariance matrix
     * @return correlation matrix
     */
    public RealMatrix covarianceToCorrelation(final RealMatrix covarianceMatrix) {
        // get column dimension
        final int nVars = covarianceMatrix.getColumnDimension();
        // initialize output matrix
        final RealMatrix outMatrix = new BlockRealMatrix(nVars, nVars);
        for (int i = 0; i < nVars; i++) {
            final double sigma = MathLib.sqrt(covarianceMatrix.getEntry(i, i));
            // set diagonal values
            outMatrix.setEntry(i, i, 1d);
            // compute correlation values from covariance matrix
            for (int j = 0; j < i; j++) {
                final double entry = covarianceMatrix.getEntry(i, j) /
                    (sigma * MathLib.sqrt(covarianceMatrix.getEntry(j, j)));
                outMatrix.setEntry(i, j, entry);
                outMatrix.setEntry(j, i, entry);
            }
        }
        return outMatrix;
    }

    /**
     * Throws IllegalArgumentException of the matrix does not have at least
     * two columns and two rows
     * 
     * @param matrix
     *        matrix to check for sufficiency
     * @throws MathIllegalArgumentException
     *         if there is insufficient data
     */
    private static void checkSufficientData(final RealMatrix matrix) {
        final int nRows = matrix.getRowDimension();
        final int nCols = matrix.getColumnDimension();
        if (nRows < 2 || nCols < 2) {
            throw new MathIllegalArgumentException(PatriusMessages.INSUFFICIENT_ROWS_AND_COLUMNS,
                nRows, nCols);
        }
    }
}
