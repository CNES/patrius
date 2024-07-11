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
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive.moment;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatistic;
import fr.cnes.sirius.patrius.math.stat.descriptive.StorelessUnivariateStatisticAbstractTest;
import fr.cnes.sirius.patrius.math.stat.descriptive.UnivariateStatistic;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for the {@link UnivariateStatistic} class.
 * 
 * @version $Id: GeometricMeanTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class GeometricMeanTest extends StorelessUnivariateStatisticAbstractTest {

    protected GeometricMean stat;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateStatistic getUnivariateStatistic() {
        return new GeometricMean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double expectedValue() {
        return this.geoMean;
    }

    @Test
    public void testSpecialValues() {
        final GeometricMean mean = new GeometricMean();
        // empty
        Assert.assertTrue(Double.isNaN(mean.getResult()));

        // finite data
        mean.increment(1d);
        Assert.assertFalse(Double.isNaN(mean.getResult()));

        // add 0 -- makes log sum blow to minus infinity, should make 0
        mean.increment(0d);
        Assert.assertEquals(0d, mean.getResult(), 0);

        // add positive infinity - note the minus infinity above
        mean.increment(Double.POSITIVE_INFINITY);
        Assert.assertTrue(Double.isNaN(mean.getResult()));

        // clear
        mean.clear();
        Assert.assertTrue(Double.isNaN(mean.getResult()));

        // copy constructor check :
        mean.increment(1);
        mean.increment(2);
        mean.increment(3);
        mean.increment(4);
        final GeometricMean mean2 = new GeometricMean(mean);
        Assert.assertEquals(mean2.getResult(), mean.getResult(), 0);

        // getSumLogImpl check :
        final double exp = MathLib.log(1) + MathLib.log(2) + MathLib.log(3) + MathLib.log(4);
        Assert.assertEquals(mean.getSumLogImpl().getResult(), exp, 0);
        mean.clear();

        // positive infinity by itself
        mean.increment(Double.POSITIVE_INFINITY);
        Assert.assertEquals(Double.POSITIVE_INFINITY, mean.getResult(), 0);

        // negative value -- should make NaN
        mean.increment(-2d);
        Assert.assertTrue(Double.isNaN(mean.getResult()));

        final StorelessUnivariateStatistic stat = new StorelessUnivariateStatistic(){

            @Override
            public
                    double
                    evaluate(final double[] values, final int begin, final int length)
                                                                                      throws MathIllegalArgumentException {
                return 0;
            }

            @Override
            public double evaluate(final double[] values) throws MathIllegalArgumentException {
                return 0;
            }

            @Override
            public
                    void
                    incrementAll(final double[] values, final int start, final int length)
                                                                                          throws MathIllegalArgumentException {
            }

            @Override
            public void incrementAll(final double[] values) throws MathIllegalArgumentException {
            }

            @Override
            public void increment(final double d) {
            }

            @Override
            public double getResult() {
                return 0;
            }

            @Override
            public long getN() {
                return 100;
            }

            @Override
            public StorelessUnivariateStatistic copy() {
                return null;
            }

            @Override
            public void clear() {
            }
        };
        // Coverage of the MathIllegalStateException in the checkEmpty method:
        boolean rez = false;
        try {
            mean.setSumLogImpl(stat);
            Assert.fail();
        } catch (final MathIllegalStateException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

}
