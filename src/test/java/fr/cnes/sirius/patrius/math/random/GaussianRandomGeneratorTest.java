/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
package fr.cnes.sirius.patrius.math.random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.StatUtils;

public class GaussianRandomGeneratorTest {

    @Test
    public void testMeanAndStandardDeviation() {
        final RandomGenerator rg = new JDKRandomGenerator();
        rg.setSeed(17399225432l);
        final GaussianRandomGenerator generator = new GaussianRandomGenerator(rg);
        final double[] sample = new double[10000];
        for (int i = 0; i < sample.length; ++i) {
            sample[i] = generator.nextNormalizedDouble();
        }
        Assert.assertEquals(0.0, StatUtils.mean(sample), 0.012);
        Assert.assertEquals(1.0, StatUtils.variance(sample), 0.01);
    }

}
