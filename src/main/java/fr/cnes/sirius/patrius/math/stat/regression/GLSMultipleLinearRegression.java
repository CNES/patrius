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
package fr.cnes.sirius.patrius.math.stat.regression;

import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.LUDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;

/**
 * The GLS implementation of the multiple linear regression.
 * 
 * GLS assumes a general covariance matrix Omega of the error
 * 
 * <pre>
 * u ~ N(0, Omega)
 * </pre>
 * 
 * Estimated by GLS,
 * 
 * <pre>
 * b=(X' Omega^-1 X)^-1X'Omega^-1 y
 * </pre>
 * 
 * whose variance is
 * 
 * <pre>
 * Var(b)=(X' Omega^-1 X)^-1
 * </pre>
 * 
 * @version $Id: GLSMultipleLinearRegression.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class GLSMultipleLinearRegression extends AbstractMultipleLinearRegression {

    /** Covariance matrix. */
    private RealMatrix omega;

    /** Inverse of covariance matrix. */
    private RealMatrix omegaInverse;

    /**
     * Replace sample data, overriding any previous sample.
     * 
     * @param y
     *        y values of the sample
     * @param x
     *        x values of the sample
     * @param covariance
     *        array representing the covariance matrix
     */
    public void newSampleData(final double[] y, final double[][] x, final double[][] covariance) {
        this.validateSampleData(x, y);
        this.newYSampleData(y);
        this.newXSampleData(x);
        this.validateCovarianceData(x, covariance);
        this.newCovarianceData(covariance);
    }

    /**
     * Add the covariance data.
     * 
     * @param omegaIn
     *        the [n,n] array representing the covariance
     */
    protected void newCovarianceData(final double[][] omegaIn) {
        this.omega = new Array2DRowRealMatrix(omegaIn);
        this.omegaInverse = null;
    }

    /**
     * Get the inverse of the covariance.
     * <p>
     * The inverse of the covariance matrix is lazily evaluated and cached.
     * </p>
     * 
     * @return inverse of the covariance
     */
    protected RealMatrix getOmegaInverse() {
        if (this.omegaInverse == null) {
            this.omegaInverse = new LUDecomposition(this.omega).getSolver().getInverse();
        }
        return this.omegaInverse;
    }

    /**
     * Calculates beta by GLS.
     * 
     * <pre>
     *  b=(X' Omega^-1 X)^-1X'Omega^-1 y
     * </pre>
     * 
     * @return beta
     */
    @Override
    protected RealVector calculateBeta() {
        final RealMatrix oi = this.getOmegaInverse();
        final RealMatrix xt = this.getX().transpose();
        final RealMatrix xtoix = xt.multiply(oi).multiply(this.getX());
        final RealMatrix inverse = new LUDecomposition(xtoix).getSolver().getInverse();
        return inverse.multiply(xt).multiply(oi).operate(this.getY());
    }

    /**
     * Calculates the variance on the beta.
     * 
     * <pre>
     *  Var(b)=(X' Omega^-1 X)^-1
     * </pre>
     * 
     * @return The beta variance matrix
     */
    @Override
    protected RealMatrix calculateBetaVariance() {
        final RealMatrix oi = this.getOmegaInverse();
        final RealMatrix xtoix = this.getX().transpose().multiply(oi).multiply(this.getX());
        return new LUDecomposition(xtoix).getSolver().getInverse();
    }

    /**
     * Calculates the estimated variance of the error term using the formula
     * 
     * <pre>
     *  Var(u) = Tr(u' Omega^-1 u)/(n-k)
     * </pre>
     * 
     * where n and k are the row and column dimensions of the design
     * matrix X.
     * 
     * @return error variance
     * @since 2.2
     */
    @Override
    protected double calculateErrorVariance() {
        final RealVector residuals = this.calculateResiduals();
        final double t = residuals.dotProduct(this.getOmegaInverse().operate(residuals));
        return t / (this.getX().getRowDimension() - this.getX().getColumnDimension());
    }
}
