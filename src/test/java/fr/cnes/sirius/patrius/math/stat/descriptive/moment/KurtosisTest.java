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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:26/11/2014: coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: KurtosisTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class KurtosisTest extends StorelessUnivariateStatisticAbstractTest {

    protected Kurtosis stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new Kurtosis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        // Coverage of the Kurtosis(Kurtosis) constructor:
        this.stat = new Kurtosis();
        new Kurtosis(this.stat);
        return this.kurt;
    }

    /**
     * Make sure Double.NaN is returned iff n < 4
     * 
     */
    @Test
    public void testNaN() {
        final Kurtosis kurt = new Kurtosis();
        Assert.assertTrue(Double.isNaN(kurt.getResult()));
        kurt.increment(1d);
        Assert.assertTrue(Double.isNaN(kurt.getResult()));
        kurt.increment(1d);
        Assert.assertTrue(Double.isNaN(kurt.getResult()));
        kurt.increment(1d);
        Assert.assertTrue(Double.isNaN(kurt.getResult()));
        kurt.increment(1d);
        Assert.assertFalse(Double.isNaN(kurt.getResult()));
    }

}
