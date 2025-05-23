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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.distribution;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.special.Gamma;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for WeibullDistribution.
 * Extends ContinuousDistributionAbstractTest. See class javadoc for
 * ContinuousDistributionAbstractTest for details.
 * 
 * @version $Id: WeibullDistributionTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class WeibullDistributionTest extends RealDistributionAbstractTest {

    // -------------- Implementations for abstract methods -----------------------

    /** Creates the default continuous distribution instance to use in tests. */
    @Override
    public WeibullDistribution makeDistribution() {
        return new WeibullDistribution(1.2, 2.1);
    }

    /** Creates the default cumulative probability distribution test input values */
    @Override
    public double[] makeCumulativeTestPoints() {
        // quantiles computed using R version 2.9.2
        return new double[] { 0.00664355180993, 0.0454328283309, 0.0981162737374, 0.176713524579, 0.321946865392,
            10.5115496887, 7.4976304671, 6.23205600701, 5.23968436955, 4.2079028257 };
    }

    /** Creates the default cumulative probability density test expected values */
    @Override
    public double[] makeCumulativeTestValues() {
        return new double[] { 0.001, 0.01, 0.025, 0.05, 0.1, 0.999, 0.990, 0.975, 0.950, 0.900 };
    }

    /** Creates the default probability density test expected values */
    @Override
    public double[] makeDensityTestValues() {
        return new double[] { 0.180535929306, 0.262801138133, 0.301905425199, 0.330899152971,
            0.353441418887, 0.000788590320203, 0.00737060094841, 0.0177576041516, 0.0343043442574, 0.065664589369 };
    }

    // ---------------------------- Additional test cases -------------------------

    @Test
    public void testInverseCumulativeProbabilityExtremes() {
        this.setInverseCumulativeTestPoints(new double[] { 0.0, 1.0 });
        this.setInverseCumulativeTestValues(new double[] { 0.0, Double.POSITIVE_INFINITY });
        this.verifyInverseCumulativeProbabilities();
    }

    @Test
    public void testAlpha() {
        WeibullDistribution dist = new WeibullDistribution(1, 2);
        Assert.assertEquals(1, dist.getShape(), 0);
        try {
            dist = new WeibullDistribution(0, 2);
            Assert.fail("NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException e) {
            // Expected.
        }
    }

    @Test
    public void testBeta() {
        WeibullDistribution dist = new WeibullDistribution(1, 2);
        Assert.assertEquals(2, dist.getScale(), 0);
        try {
            dist = new WeibullDistribution(1, 0);
            Assert.fail("NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException e) {
            // Expected.
        }
    }

    @Test
    public void testMoments() {
        final double tol = 1e-9;
        WeibullDistribution dist;

        dist = new WeibullDistribution(2.5, 3.5);
        // In R: 3.5*gamma(1+(1/2.5)) (or emperically: mean(rweibull(10000, 2.5, 3.5)))
        Assert.assertEquals(dist.getNumericalMean(), 3.5 * MathLib.exp(Gamma.logGamma(1 + (1 / 2.5))), tol);
        Assert.assertEquals(dist.getNumericalVariance(), (3.5 * 3.5) *
            MathLib.exp(Gamma.logGamma(1 + (2 / 2.5))) -
            (dist.getNumericalMean() * dist.getNumericalMean()), tol);

        dist = new WeibullDistribution(10.4, 2.222);
        Assert.assertEquals(dist.getNumericalMean(), 2.222 * MathLib.exp(Gamma.logGamma(1 + (1 / 10.4))), tol);
        Assert.assertEquals(dist.getNumericalVariance(), (2.222 * 2.222) *
            MathLib.exp(Gamma.logGamma(1 + (2 / 10.4))) -
            (dist.getNumericalMean() * dist.getNumericalMean()), tol);
    }
}
