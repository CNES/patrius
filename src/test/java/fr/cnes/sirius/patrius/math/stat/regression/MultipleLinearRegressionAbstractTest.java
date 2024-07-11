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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.regression;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;

public abstract class MultipleLinearRegressionAbstractTest {

    protected AbstractMultipleLinearRegression regression;

    @Before
    public void setUp() {
        this.regression = this.createRegression();
    }

    protected abstract AbstractMultipleLinearRegression createRegression();

    protected abstract int getNumberOfRegressors();

    protected abstract int getSampleSize();

    @Test
    public void canEstimateRegressionParameters() {
        final double[] beta = this.regression.estimateRegressionParameters();
        Assert.assertEquals(this.getNumberOfRegressors(), beta.length);
    }

    @Test
    public void canEstimateResiduals() {
        final double[] e = this.regression.estimateResiduals();
        Assert.assertEquals(this.getSampleSize(), e.length);
    }

    @Test
    public void canEstimateRegressionParametersVariance() {
        final double[][] variance = this.regression.estimateRegressionParametersVariance();
        Assert.assertEquals(this.getNumberOfRegressors(), variance.length);
    }

    @Test
    public void canEstimateRegressandVariance() {
        if (this.getSampleSize() > this.getNumberOfRegressors()) {
            final double variance = this.regression.estimateRegressandVariance();
            Assert.assertTrue(variance > 0.0);
        }
    }

    /**
     * Verifies that newSampleData methods consistently insert unitary columns
     * in design matrix. Confirms the fix for MATH-411.
     */
    @Test
    public void testNewSample() {
        final double[] design = new double[] {
            1, 19, 22, 33,
            2, 20, 30, 40,
            3, 25, 35, 45,
            4, 27, 37, 47
        };
        final double[] y = new double[] { 1, 2, 3, 4 };
        final double[][] x = new double[][] {
            { 19, 22, 33 },
            { 20, 30, 40 },
            { 25, 35, 45 },
            { 27, 37, 47 }
        };
        final AbstractMultipleLinearRegression regression = this.createRegression();
        regression.newSampleData(design, 4, 3);
        RealMatrix flatX = regression.getX().copy();
        RealVector flatY = regression.getY().copy();
        regression.newXSampleData(x);
        regression.newYSampleData(y);
        Assert.assertEquals(flatX, regression.getX());
        Assert.assertEquals(flatY, regression.getY());

        // No intercept
        regression.setNoIntercept(true);
        regression.newSampleData(design, 4, 3);
        flatX = regression.getX().copy();
        flatY = regression.getY().copy();
        regression.newXSampleData(x);
        regression.newYSampleData(y);
        Assert.assertEquals(flatX, regression.getX());
        Assert.assertEquals(flatY, regression.getY());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewSampleNullData() {
        final double[] data = null;
        this.createRegression().newSampleData(data, 2, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewSampleInvalidData() {
        final double[] data = new double[] { 1, 2, 3, 4 };
        this.createRegression().newSampleData(data, 2, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewSampleInsufficientData() {
        final double[] data = new double[] { 1, 2, 3, 4 };
        this.createRegression().newSampleData(data, 1, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXSampleDataNull() {
        this.createRegression().newXSampleData(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testYSampleDataNull() {
        this.createRegression().newYSampleData(null);
    }

}
