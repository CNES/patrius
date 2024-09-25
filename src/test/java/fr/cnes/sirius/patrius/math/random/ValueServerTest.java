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

import java.net.URL;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.cnes.sirius.patrius.math.RetryRunner;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.ZeroException;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;

/**
 * Test cases for the ValueServer class.
 * 
 * @version $Id: ValueServerTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

@RunWith(RetryRunner.class)
public final class ValueServerTest {

    private final ValueServer vs = new ValueServer(new Well19937c(100));

    @Before
    public void setUp() {
        this.vs.setMode(ValueServer.DIGEST_MODE);
        final URL url = this.getClass().getResource("testData.txt");
        this.vs.setValuesFileURL(url);
    }

    /**
     * Generate 1000 random values and make sure they look OK.<br>
     * Note that there is a non-zero (but very small) probability that
     * these tests will fail even if the code is working as designed.
     */
    @Test
    public void testNextDigest() throws Exception {
        double next = 0.0;
        final double tolerance = 0.1;
        this.vs.computeDistribution();
        Assert.assertTrue("empirical distribution property",
            this.vs.getEmpiricalDistribution() != null);
        SummaryStatistics stats = new SummaryStatistics();
        for (int i = 1; i < 1000; i++) {
            next = this.vs.getNext();
            stats.addValue(next);
        }
        Assert.assertEquals("mean", 5.069831575018909, stats.getMean(), tolerance);
        Assert.assertEquals("std dev", 1.0173699343977738, stats.getStandardDeviation(),
            tolerance);

        this.vs.computeDistribution(500);
        stats = new SummaryStatistics();
        for (int i = 1; i < 1000; i++) {
            next = this.vs.getNext();
            stats.addValue(next);
        }
        Assert.assertEquals("mean", 5.069831575018909, stats.getMean(), tolerance);
        Assert.assertEquals("std dev", 1.0173699343977738, stats.getStandardDeviation(),
            tolerance);
    }

    /**
     * Verify that when provided with fixed seeds, stochastic modes
     * generate fixed sequences. Verifies the fix for MATH-654.
     */
    @Test
    public void testFixedSeed() throws Exception {
        final ValueServer valueServer = new ValueServer();
        final URL url = this.getClass().getResource("testData.txt");
        valueServer.setValuesFileURL(url);
        valueServer.computeDistribution();
        this.checkFixedSeed(valueServer, ValueServer.DIGEST_MODE);
        this.checkFixedSeed(valueServer, ValueServer.EXPONENTIAL_MODE);
        this.checkFixedSeed(valueServer, ValueServer.GAUSSIAN_MODE);
        this.checkFixedSeed(valueServer, ValueServer.UNIFORM_MODE);
    }

    /**
     * Do the check for {@link #testFixedSeed()}
     * 
     * @param mode
     *        ValueServer mode
     */
    private void checkFixedSeed(final ValueServer valueServer, final int mode) throws Exception {
        valueServer.reSeed(1000);
        valueServer.setMode(mode);
        final double[][] values = new double[2][100];
        for (int i = 0; i < 100; i++) {
            values[0][i] = valueServer.getNext();
        }
        valueServer.reSeed(1000);
        for (int i = 0; i < 100; i++) {
            values[1][i] = valueServer.getNext();
        }
        Assert.assertTrue(Arrays.equals(values[0], values[1]));
    }

    /**
     * Make sure exception thrown if digest getNext is attempted
     * before loading empiricalDistribution.
     */
    @Test
    public void testNextDigestFail() throws Exception {
        try {
            this.vs.getNext();
            Assert.fail("Expecting IllegalStateException");
        } catch (final IllegalStateException ex) {
        }
    }

    @Test
    public void testEmptyReplayFile() throws Exception {
        try {
            final URL url = this.getClass().getResource("emptyFile.txt");
            this.vs.setMode(ValueServer.REPLAY_MODE);
            this.vs.setValuesFileURL(url);
            this.vs.getNext();
            Assert.fail("an exception should have been thrown");
        } catch (final MathIllegalStateException mise) {
            // expected behavior
        }
    }

    @Test
    public void testEmptyDigestFile() throws Exception {
        try {
            final URL url = this.getClass().getResource("emptyFile.txt");
            this.vs.setMode(ValueServer.DIGEST_MODE);
            this.vs.setValuesFileURL(url);
            this.vs.computeDistribution();
            Assert.fail("an exception should have been thrown");
        } catch (final ZeroException ze) {
            // expected behavior
        }
    }

    /**
     * Test ValueServer REPLAY_MODE using values in testData file.<br>
     * Check that the values 1,2,1001,1002 match data file values 1 and 2.
     * the sample data file.
     */
    @Test
    public void testReplay() throws Exception {
        final double firstDataValue = 4.038625496201205;
        final double secondDataValue = 3.6485326248346936;
        final double tolerance = 10E-15;
        double compareValue = 0.0d;
        this.vs.setMode(ValueServer.REPLAY_MODE);
        this.vs.resetReplayFile();
        compareValue = this.vs.getNext();
        Assert.assertEquals(compareValue, firstDataValue, tolerance);
        compareValue = this.vs.getNext();
        Assert.assertEquals(compareValue, secondDataValue, tolerance);
        for (int i = 3; i < 1001; i++) {
            compareValue = this.vs.getNext();
        }
        compareValue = this.vs.getNext();
        Assert.assertEquals(compareValue, firstDataValue, tolerance);
        compareValue = this.vs.getNext();
        Assert.assertEquals(compareValue, secondDataValue, tolerance);
        this.vs.closeReplayFile();
        // make sure no NPE
        this.vs.closeReplayFile();
    }

    /**
     * Test other ValueServer modes
     */
    @Test
    public void testModes() throws Exception {
        this.vs.setMode(ValueServer.CONSTANT_MODE);
        this.vs.setMu(0);
        Assert.assertEquals("constant mode test", this.vs.getMu(), this.vs.getNext(), Double.MIN_VALUE);
        this.vs.setMode(ValueServer.UNIFORM_MODE);
        this.vs.setMu(2);
        double val = this.vs.getNext();
        Assert.assertTrue(val > 0 && val < 4);
        this.vs.setSigma(1);
        this.vs.setMode(ValueServer.GAUSSIAN_MODE);
        val = this.vs.getNext();
        Assert.assertTrue("gaussian value close enough to mean",
            val < this.vs.getMu() + 100 * this.vs.getSigma());
        this.vs.setMode(ValueServer.EXPONENTIAL_MODE);
        val = this.vs.getNext();
        Assert.assertTrue(val > 0);
        try {
            this.vs.setMode(1000);
            this.vs.getNext();
            Assert.fail("bad mode, expecting IllegalStateException");
        } catch (final IllegalStateException ex) {
            // ignored
        }
    }

    /**
     * Test fill
     */
    @Test
    public void testFill() throws Exception {
        this.vs.setMode(ValueServer.CONSTANT_MODE);
        this.vs.setMu(2);
        final double[] val = new double[5];
        this.vs.fill(val);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals("fill test in place", 2, val[i], Double.MIN_VALUE);
        }
        final double v2[] = this.vs.fill(3);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals("fill test in place", 2, v2[i], Double.MIN_VALUE);
        }
    }

    /**
     * Test getters to make Clover happy
     */
    @Test
    public void testProperties() throws Exception {
        this.vs.setMode(ValueServer.CONSTANT_MODE);
        Assert.assertEquals("mode test", ValueServer.CONSTANT_MODE, this.vs.getMode());
        this.vs.setValuesFileURL("http://www.apache.org");
        final URL url = this.vs.getValuesFileURL();
        Assert.assertEquals("valuesFileURL test", "http://www.apache.org", url.toString());
    }

}
