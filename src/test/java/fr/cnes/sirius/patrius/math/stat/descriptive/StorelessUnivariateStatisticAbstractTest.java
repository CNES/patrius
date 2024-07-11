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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.stat.descriptive.moment.SecondMoment;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test cases for {@link StorelessUnivariateStatistic} classes.
 * 
 * @version $Id: StorelessUnivariateStatisticAbstractTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public abstract class StorelessUnivariateStatisticAbstractTest
    extends UnivariateStatisticAbstractTest {

    /** Small sample arrays */
    protected double[][] smallSamples = { {}, { 1 }, { 1, 2 }, { 1, 2, 3 }, { 1, 2, 3, 4 } };

    /** Return a new instance of the statistic */
    @Override
    public abstract UnivariateStatistic getUnivariateStatistic();

    /** Expected value for the testArray defined in UnivariateStatisticAbstractTest */
    @Override
    public abstract double expectedValue();

    /**
     * Verifies that increment() and incrementAll work properly.
     */
    @Test
    public void testIncrementation() {

        final StorelessUnivariateStatistic statistic =
            (StorelessUnivariateStatistic) this.getUnivariateStatistic();

        // Add testArray one value at a time and check result
        for (final double element : this.testArray) {
            statistic.increment(element);
        }

        Assert.assertEquals(this.expectedValue(), statistic.getResult(), this.getTolerance());
        Assert.assertEquals(this.testArray.length, statistic.getN());

        statistic.clear();

        // Add testArray all at once and check again
        statistic.incrementAll(this.testArray);
        Assert.assertEquals(this.expectedValue(), statistic.getResult(), this.getTolerance());
        Assert.assertEquals(this.testArray.length, statistic.getN());

        statistic.clear();

        // Cleared
        this.checkClearValue(statistic);
        Assert.assertEquals(0, statistic.getN());

    }

    protected void checkClearValue(final StorelessUnivariateStatistic statistic) {
        Assert.assertTrue(Double.isNaN(statistic.getResult()));
    }

    @Test
    public void testSerialization() {

        StorelessUnivariateStatistic statistic =
            (StorelessUnivariateStatistic) this.getUnivariateStatistic();

        TestUtils.checkSerializedEquality(statistic);

        statistic.clear();

        for (int i = 0; i < this.testArray.length; i++) {
            statistic.increment(this.testArray[i]);
            if (i % 5 == 0) {
                statistic = (StorelessUnivariateStatistic) TestUtils.serializeAndRecover(statistic);
            }
        }

        TestUtils.checkSerializedEquality(statistic);

        Assert.assertEquals(this.expectedValue(), statistic.getResult(), this.getTolerance());

        statistic.clear();

        this.checkClearValue(statistic);

    }

    @Test
    public void testEqualsAndHashCode() {
        final StorelessUnivariateStatistic statistic =
            (StorelessUnivariateStatistic) this.getUnivariateStatistic();
        StorelessUnivariateStatistic statistic2 = null;

        Assert.assertTrue("non-null, compared to null", !statistic.equals(statistic2));
        Assert.assertTrue("reflexive, non-null", statistic.equals(statistic));

        final int emptyHash = statistic.hashCode();
        statistic2 = (StorelessUnivariateStatistic) this.getUnivariateStatistic();
        Assert.assertTrue("empty stats should be equal", statistic.equals(statistic2));
        Assert.assertEquals("empty stats should have the same hashcode",
            emptyHash, statistic2.hashCode());

        statistic.increment(1d);
        Assert.assertTrue("reflexive, non-empty", statistic.equals(statistic));
        Assert.assertTrue("non-empty, compared to empty", !statistic.equals(statistic2));
        Assert.assertTrue("non-empty, compared to empty", !statistic2.equals(statistic));
        Assert.assertTrue("non-empty stat should have different hashcode from empty stat",
            statistic.hashCode() != emptyHash);

        statistic2.increment(1d);
        Assert.assertTrue("stats with same data should be equal", statistic.equals(statistic2));
        Assert.assertEquals("stats with same data should have the same hashcode",
            statistic.hashCode(), statistic2.hashCode());

        statistic.increment(Double.POSITIVE_INFINITY);
        Assert.assertTrue("stats with different n's should not be equal", !statistic2.equals(statistic));
        Assert.assertTrue("stats with different n's should have different hashcodes",
            statistic.hashCode() != statistic2.hashCode());

        statistic2.increment(Double.POSITIVE_INFINITY);
        Assert.assertTrue("stats with same data should be equal", statistic.equals(statistic2));
        Assert.assertEquals("stats with same data should have the same hashcode",
            statistic.hashCode(), statistic2.hashCode());

        statistic.clear();
        statistic2.clear();
        Assert.assertTrue("cleared stats should be equal", statistic.equals(statistic2));
        Assert.assertEquals("cleared stats should have thashcode of empty stat",
            emptyHash, statistic2.hashCode());
        Assert.assertEquals("cleared stats should have thashcode of empty stat",
            emptyHash, statistic.hashCode());

    }

    @Test
    public void testMomentSmallSamples() {
        final UnivariateStatistic stat = this.getUnivariateStatistic();
        if (stat instanceof SecondMoment) {
            final SecondMoment moment = (SecondMoment) this.getUnivariateStatistic();
            Assert.assertTrue(Double.isNaN(moment.getResult()));
            moment.increment(1d);
            Assert.assertEquals(0d, moment.getResult(), 0);
        }
    }

    /**
     * Make sure that evaluate(double[]) and inrementAll(double[]),
     * getResult() give same results.
     */
    @Test
    public void testConsistency() {
        final StorelessUnivariateStatistic stat = (StorelessUnivariateStatistic) this.getUnivariateStatistic();
        stat.incrementAll(this.testArray);
        Assert.assertEquals(stat.getResult(), stat.evaluate(this.testArray), this.getTolerance());
        for (final double[] smallSample : this.smallSamples) {
            stat.clear();
            for (int j = 0; j < smallSample.length; j++) {
                stat.increment(smallSample[j]);
            }
            TestUtils.assertEquals(stat.getResult(), stat.evaluate(smallSample), this.getTolerance());
        }
    }

    /**
     * Verifies that copied statistics remain equal to originals when
     * incremented the same way.
     * 
     */
    @Test
    public void testCopyConsistency() {

        final StorelessUnivariateStatistic master =
            (StorelessUnivariateStatistic) this.getUnivariateStatistic();

        StorelessUnivariateStatistic replica = null;

        // Randomly select a portion of testArray to load first
        final long index = MathLib.round((MathLib.random()) * this.testArray.length);

        // Put first half in master and copy master to replica
        master.incrementAll(this.testArray, 0, (int) index);
        replica = master.copy();

        // Check same
        Assert.assertTrue(replica.equals(master));
        Assert.assertTrue(master.equals(replica));

        // Now add second part to both and check again
        master.incrementAll(this.testArray,
            (int) index, (int) (this.testArray.length - index));
        replica.incrementAll(this.testArray,
            (int) index, (int) (this.testArray.length - index));
        Assert.assertTrue(replica.equals(master));
        Assert.assertTrue(master.equals(replica));
    }

    @Test
    public void testSerial() {
        final StorelessUnivariateStatistic s =
            (StorelessUnivariateStatistic) this.getUnivariateStatistic();
        Assert.assertEquals(s, TestUtils.serializeAndRecover(s));
    }
}
