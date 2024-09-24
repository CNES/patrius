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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.VectorialCovariance;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.VectorialMean;

public class UncorrelatedRandomVectorGeneratorTest {
    private final double[] mean;
    private final double[] standardDeviation;
    private final UncorrelatedRandomVectorGenerator generator;

    public UncorrelatedRandomVectorGeneratorTest() {
        this.mean = new double[] { 0.0, 1.0, -3.0, 2.3 };
        this.standardDeviation = new double[] { 1.0, 2.0, 10.0, 0.1 };
        final RandomGenerator rg = new JDKRandomGenerator();
        rg.setSeed(17399225432l);
        this.generator =
            new UncorrelatedRandomVectorGenerator(this.mean, this.standardDeviation,
                new GaussianRandomGenerator(rg));
    }

    @Test
    public void testMeanAndCorrelation() {

        final VectorialMean meanStat = new VectorialMean(this.mean.length);
        final VectorialCovariance covStat = new VectorialCovariance(this.mean.length, true);
        for (int i = 0; i < 10000; ++i) {
            final double[] v = this.generator.nextVector();
            meanStat.increment(v);
            covStat.increment(v);
        }

        final double[] estimatedMean = meanStat.getResult();
        double scale;
        final RealMatrix estimatedCorrelation = covStat.getResult();
        for (int i = 0; i < estimatedMean.length; ++i) {
            Assert.assertEquals(this.mean[i], estimatedMean[i], 0.07);
            for (int j = 0; j < i; ++j) {
                scale = this.standardDeviation[i] * this.standardDeviation[j];
                Assert.assertEquals(0, estimatedCorrelation.getEntry(i, j) / scale, 0.03);
            }
            scale = this.standardDeviation[i] * this.standardDeviation[i];
            Assert.assertEquals(1, estimatedCorrelation.getEntry(i, i) / scale, 0.025);
        }
    }
}
