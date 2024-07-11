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
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;
import fr.cnes.sirius.patrius.math.stat.descriptive.SummaryStatistics;

/**
 * Certified data test cases.
 * 
 * @version $Id: CertifiedDataTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class CertifiedDataTest {

    protected double mean = Double.NaN;

    protected double std = Double.NaN;

    /**
     * Test SummaryStatistics - implementations that do not store the data
     * and use single pass algorithms to compute statistics
     */
    @Test
    public void testSummaryStatistics() throws Exception {
        final SummaryStatistics u = new SummaryStatistics();
        this.loadStats("data/PiDigits.txt", u);
        Assert.assertEquals("PiDigits: std", this.std, u.getStandardDeviation(), 1E-13);
        Assert.assertEquals("PiDigits: mean", this.mean, u.getMean(), 1E-13);

        this.loadStats("data/Mavro.txt", u);
        Assert.assertEquals("Mavro: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("Mavro: mean", this.mean, u.getMean(), 1E-14);

        this.loadStats("data/Michelso.txt", u);
        Assert.assertEquals("Michelso: std", this.std, u.getStandardDeviation(), 1E-13);
        Assert.assertEquals("Michelso: mean", this.mean, u.getMean(), 1E-13);

        this.loadStats("data/NumAcc1.txt", u);
        Assert.assertEquals("NumAcc1: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("NumAcc1: mean", this.mean, u.getMean(), 1E-14);

        this.loadStats("data/NumAcc2.txt", u);
        Assert.assertEquals("NumAcc2: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("NumAcc2: mean", this.mean, u.getMean(), 1E-14);
    }

    /**
     * Test DescriptiveStatistics - implementations that store full array of
     * values and execute multi-pass algorithms
     */
    @Test
    public void testDescriptiveStatistics() throws Exception {

        final DescriptiveStatistics u = new DescriptiveStatistics();

        this.loadStats("data/PiDigits.txt", u);
        Assert.assertEquals("PiDigits: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("PiDigits: mean", this.mean, u.getMean(), 1E-14);

        this.loadStats("data/Mavro.txt", u);
        Assert.assertEquals("Mavro: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("Mavro: mean", this.mean, u.getMean(), 1E-14);

        this.loadStats("data/Michelso.txt", u);
        Assert.assertEquals("Michelso: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("Michelso: mean", this.mean, u.getMean(), 1E-14);

        this.loadStats("data/NumAcc1.txt", u);
        Assert.assertEquals("NumAcc1: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("NumAcc1: mean", this.mean, u.getMean(), 1E-14);

        this.loadStats("data/NumAcc2.txt", u);
        Assert.assertEquals("NumAcc2: std", this.std, u.getStandardDeviation(), 1E-14);
        Assert.assertEquals("NumAcc2: mean", this.mean, u.getMean(), 1E-14);
    }

    /**
     * loads a DescriptiveStatistics off of a test file
     * 
     * @param file
     * @param statistical
     *        summary
     */
    private void loadStats(final String resource, final Object u) throws Exception {

        DescriptiveStatistics d = null;
        SummaryStatistics s = null;
        if (u instanceof DescriptiveStatistics) {
            d = (DescriptiveStatistics) u;
            d.clear();
        } else {
            s = (SummaryStatistics) u;
            s.clear();
        }
        this.mean = Double.NaN;
        this.std = Double.NaN;

        final InputStream resourceAsStream = CertifiedDataTest.class.getResourceAsStream(resource);
        Assert.assertNotNull("Could not find resource " + resource, resourceAsStream);
        final BufferedReader in =
            new BufferedReader(
                new InputStreamReader(
                    resourceAsStream));

        String line = null;

        for (int j = 0; j < 60; j++) {
            line = in.readLine();
            if (j == 40) {
                this.mean =
                    Double.parseDouble(
                        line.substring(line.lastIndexOf(":") + 1).trim());
            }
            if (j == 41) {
                this.std =
                    Double.parseDouble(
                        line.substring(line.lastIndexOf(":") + 1).trim());
            }
        }

        line = in.readLine();

        while (line != null) {
            if (d != null) {
                d.addValue(Double.parseDouble(line.trim()));
            } else {
                s.addValue(Double.parseDouble(line.trim()));
            }
            line = in.readLine();
        }

        resourceAsStream.close();
        in.close();
    }
}
