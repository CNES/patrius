/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.stat.correlation;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.stat.ranking.NaturalRanking;
import fr.cnes.sirius.patrius.math.stat.ranking.RankingAlgorithm;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Spearman's rank correlation. This implementation performs a rank transformation on the input data and then computes
 * {@link PearsonsCorrelation} on the ranked data.
 * </p>
 * 
 * <p>
 * By default, ranks are computed using {@link NaturalRanking} with default strategies for handling NaNs and ties in the
 * data (NaNs maximal, ties averaged). The ranking algorithm can be set using a constructor argument.
 * </p>
 * 
 * @since 2.0
 * @version $Id: SpearmansCorrelation.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.NullAssignment")
public class SpearmansCorrelation {

    /** Input data */
    private final RealMatrix data;

    /** Ranking algorithm */
    private final RankingAlgorithm rankingAlgorithm;

    /** Rank correlation */
    private final PearsonsCorrelation rankCorrelation;

    /**
     * Create a SpearmansCorrelation without data.
     */
    public SpearmansCorrelation() {
        this(new NaturalRanking());
    }

    /**
     * Create a SpearmansCorrelation with the given ranking algorithm.
     * 
     * @param rankingAlgorithmIn
     *        ranking algorithm
     * @since 3.1
     */
    public SpearmansCorrelation(final RankingAlgorithm rankingAlgorithmIn) {
        this.data = null;
        this.rankingAlgorithm = rankingAlgorithmIn;
        this.rankCorrelation = null;
    }

    /**
     * Create a SpearmansCorrelation from the given data matrix.
     * 
     * @param dataMatrix
     *        matrix of data with columns representing
     *        variables to correlate
     */
    public SpearmansCorrelation(final RealMatrix dataMatrix) {
        this(dataMatrix, new NaturalRanking());
    }

    /**
     * Create a SpearmansCorrelation with the given input data matrix
     * and ranking algorithm.
     * 
     * @param dataMatrix
     *        matrix of data with columns representing
     *        variables to correlate
     * @param rankingAlgorithmIn
     *        ranking algorithm
     */
    public SpearmansCorrelation(final RealMatrix dataMatrix, final RankingAlgorithm rankingAlgorithmIn) {
        this.data = dataMatrix.copy();
        this.rankingAlgorithm = rankingAlgorithmIn;
        this.rankTransform(this.data);
        this.rankCorrelation = new PearsonsCorrelation(this.data);
    }

    /**
     * Calculate the Spearman Rank Correlation Matrix.
     * 
     * @return Spearman Rank Correlation Matrix
     */
    public RealMatrix getCorrelationMatrix() {
        return this.rankCorrelation.getCorrelationMatrix();
    }

    /**
     * Returns a {@link PearsonsCorrelation} instance constructed from the
     * ranked input data. That is, <code>new SpearmansCorrelation(matrix).getRankCorrelation()</code> is equivalent to
     * <code>new PearsonsCorrelation(rankTransform(matrix))</code> where <code>rankTransform(matrix)</code> is the
     * result of applying the
     * configured <code>RankingAlgorithm</code> to each of the columns of <code>matrix.</code>
     * 
     * @return PearsonsCorrelation among ranked column data
     */
    public PearsonsCorrelation getRankCorrelation() {
        return this.rankCorrelation;
    }

    /**
     * Computes the Spearman's rank correlation matrix for the columns of the
     * input matrix.
     * 
     * @param matrix
     *        matrix with columns representing variables to correlate
     * @return correlation matrix
     */
    public RealMatrix computeCorrelationMatrix(final RealMatrix matrix) {
        final RealMatrix matrixCopy = matrix.copy();
        this.rankTransform(matrixCopy);
        return new PearsonsCorrelation().computeCorrelationMatrix(matrixCopy);
    }

    /**
     * Computes the Spearman's rank correlation matrix for the columns of the
     * input rectangular array. The columns of the array represent values
     * of variables to be correlated.
     * 
     * @param matrix
     *        matrix with columns representing variables to correlate
     * @return correlation matrix
     */
    public RealMatrix computeCorrelationMatrix(final double[][] matrix) {
        return this.computeCorrelationMatrix(new BlockRealMatrix(matrix));
    }

    /**
     * Computes the Spearman's rank correlation coefficient between the two arrays.
     * 
     * @param xArray
     *        first data array
     * @param yArray
     *        second data array
     * @return Returns Spearman's rank correlation coefficient for the two arrays
     * @throws DimensionMismatchException
     *         if the arrays lengths do not match
     * @throws MathIllegalArgumentException
     *         if the array length is less than 2
     */
    public double correlation(final double[] xArray, final double[] yArray) {
        if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        } else if (xArray.length < 2) {
            throw new MathIllegalArgumentException(PatriusMessages.INSUFFICIENT_DIMENSION,
                xArray.length, 2);
        } else {
            return new PearsonsCorrelation().correlation(this.rankingAlgorithm.rank(xArray),
                this.rankingAlgorithm.rank(yArray));
        }
    }

    /**
     * Applies rank transform to each of the columns of <code>matrix</code> using the current
     * <code>rankingAlgorithm</code>
     * 
     * @param matrix
     *        matrix to transform
     */
    private void rankTransform(final RealMatrix matrix) {
        for (int i = 0; i < matrix.getColumnDimension(); i++) {
            matrix.setColumn(i, this.rankingAlgorithm.rank(matrix.getColumn(i)));
        }
    }
}
