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

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;

public class VectorialCovarianceTest {
    private final double[][] points;

    public VectorialCovarianceTest() {
        this.points = new double[][] {
            { 1.2, 2.3, 4.5 },
            { -0.7, 2.3, 5.0 },
            { 3.1, 0.0, -3.1 },
            { 6.0, 1.2, 4.2 },
            { -0.7, 2.3, 5.0 }
        };
    }

    @Test
    public void testMismatch() {
        try {
            new VectorialCovariance(8, true).increment(new double[5]);
            Assert.fail("an exception should have been thrown");
        } catch (final DimensionMismatchException dme) {
            Assert.assertEquals(5, dme.getArgument());
            Assert.assertEquals(8, dme.getDimension());
        }
    }

    @Test
    public void testSimplistic() {
        final VectorialCovariance stat = new VectorialCovariance(2, true);
        stat.increment(new double[] { -1.0, 1.0 });
        stat.increment(new double[] { 1.0, -1.0 });
        final RealMatrix c = stat.getResult();
        Assert.assertEquals(2.0, c.getEntry(0, 0), 1.0e-12);
        Assert.assertEquals(-2.0, c.getEntry(1, 0), 1.0e-12);
        Assert.assertEquals(2.0, c.getEntry(1, 1), 1.0e-12);
    }

    @Test
    public void testBasicStats() {

        final VectorialCovariance stat = new VectorialCovariance(this.points[0].length, true);
        for (final double[] point : this.points) {
            stat.increment(point);
        }

        Assert.assertEquals(this.points.length, stat.getN());

        final RealMatrix c = stat.getResult();
        final double[][] refC = new double[][] {
            { 8.0470, -1.9195, -3.4445 },
            { -1.9195, 1.0470, 3.2795 },
            { -3.4445, 3.2795, 12.2070 }
        };

        for (int i = 0; i < c.getRowDimension(); ++i) {
            for (int j = 0; j <= i; ++j) {
                Assert.assertEquals(refC[i][j], c.getEntry(i, j), 1.0e-12);
            }
        }

    }

    @Test
    public void testSerial() {
        final VectorialCovariance stat = new VectorialCovariance(this.points[0].length, true);
        Assert.assertEquals(stat, TestUtils.serializeAndRecover(stat));
    }

    @Test
    public void testHashCode() {
        final VectorialCovariance stat = new VectorialCovariance(this.points[0].length, true);
        Assert.assertEquals(1780436368, stat.hashCode());
    }

    @Test
    public void testEquals() {
        final VectorialCovariance stat1 = new VectorialCovariance(this.points[0].length, true);
        Assert.assertTrue(stat1.equals(stat1));
        Assert.assertFalse(stat1.equals(new Mean()));
        VectorialCovariance stat2 = new VectorialCovariance(this.points[0].length, false);
        Assert.assertFalse(stat1.equals(stat2));
        stat2 = new VectorialCovariance(10, true);
        Assert.assertFalse(stat1.equals(stat2));
        stat2 = new VectorialCovariance(this.points[0].length, true);
        stat2.increment(new double[] { 1, 2, 3 });
        Assert.assertFalse(stat1.equals(stat2));
        stat2 = new VectorialCovariance(this.points[0].length, true);
        stat1.increment(new double[] { 0, 2, 3 });
        stat2.increment(new double[] { 1, 2, 3 });
        Assert.assertFalse(stat1.equals(stat2));
    }
}
