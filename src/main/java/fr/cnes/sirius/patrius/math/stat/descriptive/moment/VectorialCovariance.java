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
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import java.io.Serializable;
import java.util.Arrays;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;

/**
 * Returns the covariance matrix of the available vectors.
 * 
 * @since 1.2
 * @version $Id: VectorialCovariance.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class VectorialCovariance implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = 4118372414238930270L;

    /** Sums for each component. */
    private final double[] sums;

    /** Sums of products for each component. */
    private final double[] productsSums;

    /** Indicator for bias correction. */
    private final boolean isBiasCorrected;

    /** Number of vectors in the sample. */
    private long n;

    /**
     * Constructs a VectorialCovariance.
     * 
     * @param dimension
     *        vectors dimension
     * @param isBiasCorrectedIn
     *        if true, computed the unbiased sample covariance,
     *        otherwise computes the biased population covariance
     */
    public VectorialCovariance(final int dimension, final boolean isBiasCorrectedIn) {
        this.sums = new double[dimension];
        this.productsSums = new double[dimension * (dimension + 1) / 2];
        this.n = 0;
        this.isBiasCorrected = isBiasCorrectedIn;
    }

    /**
     * Add a new vector to the sample.
     * 
     * @param v
     *        vector to add
     * @throws DimensionMismatchException
     *         if the vector does not have the right dimension
     */
    public void increment(final double[] v) {
        if (v.length != this.sums.length) {
            // Exception : the vector does not have the right dimension
            throw new DimensionMismatchException(v.length, this.sums.length);
        }
        int k = 0;
        // loop on the vector elements
        for (int i = 0; i < v.length; ++i) {
            // update sums and product sums with vector elements
            this.sums[i] += v[i];
            for (int j = 0; j <= i; ++j) {
                this.productsSums[k++] += v[i] * v[j];
            }
        }
        // increment number of vectors in the sample
        this.n++;
    }

    /**
     * Get the covariance matrix.
     * 
     * @return covariance matrix
     */
    public RealMatrix getResult() {

        // get matrix dimension
        final int dimension = this.sums.length;
        // initialize matrix
        final RealMatrix result = MatrixUtils.createRealMatrix(dimension, dimension);

        // compute covariance matrix
        if (this.n > 1) {
            final double c = 1.0 / (this.n * (this.isBiasCorrected ? (this.n - 1) : this.n));
            int k = 0;
            for (int i = 0; i < dimension; ++i) {
                for (int j = 0; j <= i; ++j) {
                    final double e = c * (this.n * this.productsSums[k++] - this.sums[i] * this.sums[j]);
                    result.setEntry(i, j, e);
                    result.setEntry(j, i, e);
                }
            }
        }

        // return computed result
        return result;

    }

    /**
     * Get the number of vectors in the sample.
     * 
     * @return number of vectors in the sample
     */
    public long getN() {
        return this.n;
    }

    /**
     * Clears the internal state of the Statistic
     */
    public void clear() {
        this.n = 0;
        Arrays.fill(this.sums, 0.0);
        Arrays.fill(this.productsSums, 0.0);
    }

    // CHECKSTYLE: stop MagicNumber check
    // Reason: model - Commons-Math code

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.isBiasCorrected ? 1231 : 1237);
        result = prime * result + (int) (this.n ^ (this.n >>> 32));
        result = prime * result + Arrays.hashCode(this.productsSums);
        result = prime * result + Arrays.hashCode(this.sums);
        return result;
    }

    // CHECKSTYLE: resume MagicNumber check

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            // Immediate return
            return true;
        }
        if (!(obj instanceof VectorialCovariance)) {
            // Immediate return
            return false;
        }

        // General case
        final VectorialCovariance other = (VectorialCovariance) obj;
        if (this.isBiasCorrected != other.isBiasCorrected) {
            return false;
        }
        if (this.n != other.n) {
            return false;
        }
        if (!Arrays.equals(this.productsSums, other.productsSums)) {
            return false;
        }
        return Arrays.equals(this.sums, other.sums);
    }
}
