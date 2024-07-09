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

import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Bivariate Covariance implementation that does not require input data to be
 * stored in memory.
 * 
 * <p>
 * This class is based on a paper written by Philippe P&eacute;bay: <a
 * href="http://prod.sandia.gov/techlib/access-control.cgi/2008/086212.pdf"> Formulas for Robust, One-Pass Parallel
 * Computation of Covariances and Arbitrary-Order Statistical Moments</a>, 2008, Technical Report SAND2008-6212, Sandia
 * National Laboratories. It computes the covariance for a pair of variables. Use {@link StorelessCovariance} to
 * estimate an entire covariance matrix.
 * </p>
 * 
 * <p>
 * Note: This class is package private as it is only used internally in the {@link StorelessCovariance} class.
 * </p>
 * 
 * @version $Id: StorelessBivariateCovariance.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
class StorelessBivariateCovariance {

    /** the mean of variable x */
    private double meanX;

    /** the mean of variable y */
    private double meanY;

    /** number of observations */
    private double n;

    /** the running covariance estimate */
    private double covarianceNumerator;

    /** flag for bias correction */
    private final boolean biasCorrected;

    /**
     * Create an empty {@link StorelessBivariateCovariance} instance with
     * bias correction.
     */
    public StorelessBivariateCovariance() {
        this(true);
    }

    /**
     * Create an empty {@link StorelessBivariateCovariance} instance.
     * 
     * @param biasCorrection
     *        if <code>true</code> the covariance estimate is corrected
     *        for bias, i.e. n-1 in the denominator, otherwise there is no bias correction,
     *        i.e. n in the denominator.
     */
    public StorelessBivariateCovariance(final boolean biasCorrection) {
        this.meanX = 0.0;
        this.meanY = 0.0;
        this.n = 0;
        this.covarianceNumerator = 0.0;
        this.biasCorrected = biasCorrection;
    }

    /**
     * Update the covariance estimation with a pair of variables (x, y).
     * 
     * @param x
     *        the x value
     * @param y
     *        the y value
     */
    public void increment(final double x, final double y) {
        this.n++;
        final double deltaX = x - this.meanX;
        final double deltaY = y - this.meanY;
        this.meanX += deltaX / this.n;
        this.meanY += deltaY / this.n;
        this.covarianceNumerator += ((this.n - 1.0) / this.n) * deltaX * deltaY;
    }

    /**
     * Returns the number of observations.
     * 
     * @return number of observations
     */
    public double getN() {
        return this.n;
    }

    /**
     * Return the current covariance estimate.
     * 
     * @return the current covariance
     * @throws NumberIsTooSmallException
     *         if the number of observations
     *         is &lt; 2
     */
    public double getResult() {
        if (this.n < 2) {
            throw new NumberIsTooSmallException(PatriusMessages.INSUFFICIENT_DIMENSION,
                this.n, 2, true);
        }
        if (this.biasCorrected) {
            return this.covarianceNumerator / (this.n - 1d);
        } else {
            return this.covarianceNumerator / this.n;
        }
    }
}
