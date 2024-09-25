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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.stat.StatUtils;
import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;

/**
 * The class <code>StableRandomGeneratorTest</code> contains tests for the class {@link StableRandomGenerator}
 * 
 * @version $Revision: 18108 $
 */
public class StableRandomGeneratorTest {

    private final RandomGenerator rg = new Well19937c(100);
    private final static int sampleSize = 10000;

    /**
     * Run the double nextDouble() method test Due to leptokurtic property the
     * acceptance range is widened.
     */
    @Test
    public void testNextDouble() {
        final StableRandomGenerator generator = new StableRandomGenerator(this.rg, 1.3,
            0.1);
        final double[] sample = new double[2 * sampleSize];
        for (int i = 0; i < sample.length; ++i) {
            sample[i] = generator.nextNormalizedDouble();
        }
        Assert.assertEquals(0.0, StatUtils.mean(sample), 0.3);
    }

    /**
     * If alpha = 2, than it must be Gaussian distribution
     */
    @Test
    public void testGaussianCase() {
        final StableRandomGenerator generator = new StableRandomGenerator(this.rg, 2d, 0.0);

        final double[] sample = new double[sampleSize];
        for (int i = 0; i < sample.length; ++i) {
            sample[i] = generator.nextNormalizedDouble();
        }
        Assert.assertEquals(0.0, StatUtils.mean(sample), 0.02);
        Assert.assertEquals(1.0, StatUtils.variance(sample), 0.02);
    }

    /**
     * If alpha = 1, than it must be Cauchy distribution
     */
    @Test
    public void testCauchyCase() {
        final StableRandomGenerator generator = new StableRandomGenerator(this.rg, 1d, 0.0);
        final DescriptiveStatistics summary = new DescriptiveStatistics();

        for (int i = 0; i < sampleSize; ++i) {
            final double sample = generator.nextNormalizedDouble();
            summary.addValue(sample);
        }

        // Standard Cauchy distribution should have zero median and mode
        final double median = summary.getPercentile(50);
        Assert.assertEquals(0.0, median, 0.2);
    }

    /**
     * Input parameter range tests
     */
    @Test
    public void testAlphaRangeBelowZero() {
        try {
            new StableRandomGenerator(this.rg,
                -1.0, 0.0);
            Assert.fail("Expected OutOfRangeException");
        } catch (final OutOfRangeException e) {
            Assert.assertEquals(-1.0, e.getArgument());
        }
    }

    @Test
    public void testAlphaRangeAboveTwo() {
        try {
            new StableRandomGenerator(this.rg,
                3.0, 0.0);
            Assert.fail("Expected OutOfRangeException");
        } catch (final OutOfRangeException e) {
            Assert.assertEquals(3.0, e.getArgument());
        }
    }

    @Test
    public void testBetaRangeBelowMinusOne() {
        try {
            new StableRandomGenerator(this.rg,
                1.0, -2.0);
            Assert.fail("Expected OutOfRangeException");
        } catch (final OutOfRangeException e) {
            Assert.assertEquals(-2.0, e.getArgument());
        }
    }

    @Test
    public void testBetaRangeAboveOne() {
        try {
            new StableRandomGenerator(this.rg,
                1.0, 2.0);
            Assert.fail("Expected OutOfRangeException");
        } catch (final OutOfRangeException e) {
            Assert.assertEquals(2.0, e.getArgument());
        }
    }
}