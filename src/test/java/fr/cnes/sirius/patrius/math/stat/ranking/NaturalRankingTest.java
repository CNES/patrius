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
package fr.cnes.sirius.patrius.math.stat.ranking;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NotANumberException;
import fr.cnes.sirius.patrius.math.random.JDKRandomGenerator;
import fr.cnes.sirius.patrius.math.random.RandomGenerator;

/**
 * Test cases for NaturalRanking class
 * 
 * @since 2.0
 * @version $Id: NaturalRankingTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class NaturalRankingTest {

    private final double[] exampleData = { 20, 17, 30, 42.3, 17, 50,
        Double.NaN, Double.NEGATIVE_INFINITY, 17 };
    private final double[] tiesFirst = { 0, 0, 2, 1, 4 };
    private final double[] tiesLast = { 4, 4, 1, 0 };
    private final double[] multipleNaNs = { 0, 1, Double.NaN, Double.NaN };
    private final double[] multipleTies = { 3, 2, 5, 5, 6, 6, 1 };
    private final double[] allSame = { 0, 0, 0, 0 };

    @Test
    public void testDefault() { // Ties averaged, NaNs failed
        final NaturalRanking ranking = new NaturalRanking();
        double[] ranks;

        try {
            ranks = ranking.rank(this.exampleData);
            Assert.fail("expected NotANumberException due to NaNStrategy.FAILED");
        } catch (final NotANumberException e) {
            // expected
        }

        ranks = ranking.rank(this.tiesFirst);
        double[] correctRanks = new double[] { 1.5, 1.5, 4, 3, 5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesLast);
        correctRanks = new double[] { 3.5, 3.5, 2, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);

        try {
            ranks = ranking.rank(this.multipleNaNs);
            Assert.fail("expected NotANumberException due to NaNStrategy.FAILED");
        } catch (final NotANumberException e) {
            // expected
        }

        ranks = ranking.rank(this.multipleTies);
        correctRanks = new double[] { 3, 2, 4.5, 4.5, 6.5, 6.5, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.allSame);
        correctRanks = new double[] { 2.5, 2.5, 2.5, 2.5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
    }

    @Test
    public void testNaNsMaximalTiesMinimum() {
        final NaturalRanking ranking = new NaturalRanking(NaNStrategy.MAXIMAL, TiesStrategy.MINIMUM);
        double[] ranks = ranking.rank(this.exampleData);
        double[] correctRanks = { 5, 2, 6, 7, 2, 8, 9, 1, 2 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesFirst);
        correctRanks = new double[] { 1, 1, 4, 3, 5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesLast);
        correctRanks = new double[] { 3, 3, 2, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleNaNs);
        correctRanks = new double[] { 1, 2, 3, 3 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleTies);
        correctRanks = new double[] { 3, 2, 4, 4, 6, 6, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.allSame);
        correctRanks = new double[] { 1, 1, 1, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
    }

    @Test
    public void testNaNsRemovedTiesSequential() {
        final NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED,
            TiesStrategy.SEQUENTIAL);
        double[] ranks = ranking.rank(this.exampleData);
        double[] correctRanks = { 5, 2, 6, 7, 3, 8, 1, 4 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesFirst);
        correctRanks = new double[] { 1, 2, 4, 3, 5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesLast);
        correctRanks = new double[] { 3, 4, 2, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleNaNs);
        correctRanks = new double[] { 1, 2 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleTies);
        correctRanks = new double[] { 3, 2, 4, 5, 6, 7, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.allSame);
        correctRanks = new double[] { 1, 2, 3, 4 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
    }

    @Test
    public void testNaNsMinimalTiesMaximum() {
        final NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL,
            TiesStrategy.MAXIMUM);
        double[] ranks = ranking.rank(this.exampleData);
        double[] correctRanks = { 6, 5, 7, 8, 5, 9, 2, 2, 5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesFirst);
        correctRanks = new double[] { 2, 2, 4, 3, 5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesLast);
        correctRanks = new double[] { 4, 4, 2, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleNaNs);
        correctRanks = new double[] { 3, 4, 2, 2 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleTies);
        correctRanks = new double[] { 3, 2, 5, 5, 7, 7, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.allSame);
        correctRanks = new double[] { 4, 4, 4, 4 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
    }

    @Test
    public void testNaNsMinimalTiesAverage() {
        final NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL);
        double[] ranks = ranking.rank(this.exampleData);
        double[] correctRanks = { 6, 4, 7, 8, 4, 9, 1.5, 1.5, 4 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesFirst);
        correctRanks = new double[] { 1.5, 1.5, 4, 3, 5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesLast);
        correctRanks = new double[] { 3.5, 3.5, 2, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleNaNs);
        correctRanks = new double[] { 3, 4, 1.5, 1.5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleTies);
        correctRanks = new double[] { 3, 2, 4.5, 4.5, 6.5, 6.5, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.allSame);
        correctRanks = new double[] { 2.5, 2.5, 2.5, 2.5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
    }

    @Test
    public void testNaNsFixedTiesRandom() {
        final RandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(1000);
        final NaturalRanking ranking = new NaturalRanking(NaNStrategy.FIXED,
            randomGenerator);
        double[] ranks = ranking.rank(this.exampleData);
        double[] correctRanks = { 5, 4, 6, 7, 3, 8, Double.NaN, 1, 4 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesFirst);
        correctRanks = new double[] { 1, 1, 4, 3, 5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.tiesLast);
        correctRanks = new double[] { 3, 4, 2, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleNaNs);
        correctRanks = new double[] { 1, 2, Double.NaN, Double.NaN };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.multipleTies);
        correctRanks = new double[] { 3, 2, 5, 5, 7, 6, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranks = ranking.rank(this.allSame);
        correctRanks = new double[] { 1, 3, 4, 4 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
    }

    @Test
    public void testNaNsAndInfs() {
        final double[] data = { 0, Double.POSITIVE_INFINITY, Double.NaN,
            Double.NEGATIVE_INFINITY };
        NaturalRanking ranking = new NaturalRanking(NaNStrategy.MAXIMAL);
        double[] ranks = ranking.rank(data);
        double[] correctRanks = new double[] { 2, 3.5, 3.5, 1 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
        ranking = new NaturalRanking(NaNStrategy.MINIMAL);
        ranks = ranking.rank(data);
        correctRanks = new double[] { 3, 4, 1.5, 1.5 };
        TestUtils.assertEquals(correctRanks, ranks, 0d);
    }

    @Test(expected = NotANumberException.class)
    public void testNaNsFailed() {
        final double[] data = { 0, Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY };
        final NaturalRanking ranking = new NaturalRanking(NaNStrategy.FAILED);
        ranking.rank(data);
    }

    @Test
    public void testNoNaNsFailed() {
        final double[] data = { 1, 2, 3, 4 };
        final NaturalRanking ranking = new NaturalRanking(NaNStrategy.FAILED);
        final double[] ranks = ranking.rank(data);
        TestUtils.assertEquals(data, ranks, 0d);
    }

}
