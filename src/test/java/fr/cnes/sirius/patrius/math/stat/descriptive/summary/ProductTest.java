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
package fr.cnes.sirius.patrius.math.stat.descriptive.summary;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: ProductTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ProductTest extends StorelessUnivariateStatisticAbstractTest {

    protected Product stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new Product();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTolerance() {
        return 10E8; // sic -- big absolute error due to only 15 digits of accuracy in double
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        return this.product;
    }

    /** Expected value for the testArray defined in UnivariateStatisticAbstractTest */
    public double expectedWeightedValue() {
        return this.weightedProduct;
    }

    @Test
    public void testSpecialValues() {
        final Product product = new Product();
        Assert.assertEquals(1, product.getResult(), 0);
        product.increment(1);
        Assert.assertEquals(1, product.getResult(), 0);
        product.increment(Double.POSITIVE_INFINITY);
        Assert.assertEquals(Double.POSITIVE_INFINITY, product.getResult(), 0);
        product.increment(Double.NEGATIVE_INFINITY);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, product.getResult(), 0);
        product.increment(Double.NaN);
        Assert.assertTrue(Double.isNaN(product.getResult()));
        product.increment(1);
        Assert.assertTrue(Double.isNaN(product.getResult()));
    }

    @Test
    public void testWeightedProduct() {
        final Product product = new Product();
        Assert.assertEquals(this.expectedWeightedValue(),
            product.evaluate(this.testArray, this.testWeightsArray, 0, this.testArray.length), this.getTolerance());
        Assert.assertEquals(this.expectedValue(),
            product.evaluate(this.testArray, this.unitWeightsArray, 0, this.testArray.length),
            this.getTolerance());
    }

    @Override
    protected void checkClearValue(final StorelessUnivariateStatistic statistic) {
        Assert.assertEquals(1, statistic.getResult(), 0);
    }

}
