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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.correlation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;

/**
 * Test cases for Spearman's rank correlation
 * 
 * @since 2.0
 * @version $Id: SpearmansRankCorrelationTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class SpearmansRankCorrelationTest extends PearsonsCorrelationTest {

    /**
     * Test Longley dataset against R.
     */
    @Override
    @Test
    public void testLongly() {
        final RealMatrix matrix = this.createRealMatrix(this.longleyData, 16, 7);
        final SpearmansCorrelation corrInstance = new SpearmansCorrelation(matrix);
        final RealMatrix correlationMatrix = corrInstance.getCorrelationMatrix();
        final double[] rData = new double[] {
            1, 0.982352941176471, 0.985294117647059, 0.564705882352941, 0.2264705882352941, 0.976470588235294,
            0.976470588235294, 0.982352941176471, 1, 0.997058823529412, 0.664705882352941, 0.2205882352941176,
            0.997058823529412, 0.997058823529412, 0.985294117647059, 0.997058823529412, 1, 0.638235294117647,
            0.2235294117647059, 0.9941176470588236, 0.9941176470588236, 0.564705882352941, 0.664705882352941,
            0.638235294117647, 1, -0.3411764705882353, 0.685294117647059, 0.685294117647059, 0.2264705882352941,
            0.2205882352941176, 0.2235294117647059, -0.3411764705882353, 1, 0.2264705882352941, 0.2264705882352941,
            0.976470588235294, 0.997058823529412, 0.9941176470588236, 0.685294117647059, 0.2264705882352941, 1, 1,
            0.976470588235294, 0.997058823529412, 0.9941176470588236, 0.685294117647059, 0.2264705882352941, 1, 1
        };
        TestUtils.assertEquals("Spearman's correlation matrix", this.createRealMatrix(rData, 7, 7), correlationMatrix,
            10E-15);
    }

    /**
     * Test R swiss fertility dataset.
     */
    @Test
    public void testSwiss() {
        final RealMatrix matrix = this.createRealMatrix(this.swissData, 47, 5);
        final SpearmansCorrelation corrInstance = new SpearmansCorrelation(matrix);
        final RealMatrix correlationMatrix = corrInstance.getCorrelationMatrix();
        final double[] rData = new double[] {
            1, 0.2426642769364176, -0.660902996352354, -0.443257690360988, 0.4136455623012432,
            0.2426642769364176, 1, -0.598859938748963, -0.650463814145816, 0.2886878090882852,
            -0.660902996352354, -0.598859938748963, 1, 0.674603831406147, -0.4750575257171745,
            -0.443257690360988, -0.650463814145816, 0.674603831406147, 1, -0.1444163088302244,
            0.4136455623012432, 0.2886878090882852, -0.4750575257171745, -0.1444163088302244, 1
        };
        TestUtils.assertEquals("Spearman's correlation matrix", this.createRealMatrix(rData, 5, 5), correlationMatrix,
            10E-15);
    }

    /**
     * Constant column
     */
    @Override
    @Test
    public void testConstant() {
        final double[] noVariance = new double[] { 1, 1, 1, 1 };
        final double[] values = new double[] { 1, 2, 3, 4 };
        Assert.assertTrue(Double.isNaN(new SpearmansCorrelation().correlation(noVariance, values)));
    }

    /**
     * Insufficient data
     */
    @Override
    @Test
    public void testInsufficientData() {
        final double[] one = new double[] { 1 };
        final double[] two = new double[] { 2 };
        try {
            new SpearmansCorrelation().correlation(one, two);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        final RealMatrix matrix = new BlockRealMatrix(new double[][] { { 0 }, { 1 } });
        try {
            new SpearmansCorrelation(matrix);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
    }

    @Override
    @Test
    public void testConsistency() {
        final RealMatrix matrix = this.createRealMatrix(this.longleyData, 16, 7);
        final SpearmansCorrelation corrInstance = new SpearmansCorrelation(matrix);
        final double[][] data = matrix.getData(false);
        final double[] x = matrix.getColumn(0);
        final double[] y = matrix.getColumn(1);
        Assert.assertEquals(new SpearmansCorrelation().correlation(x, y),
            corrInstance.getCorrelationMatrix().getEntry(0, 1), Double.MIN_VALUE);
        TestUtils.assertEquals("Correlation matrix", corrInstance.getCorrelationMatrix(),
            new SpearmansCorrelation().computeCorrelationMatrix(data), Double.MIN_VALUE);
    }

    // Not relevant here
    @Override
    @Test
    public void testStdErrorConsistency() {
    }

    @Override
    @Test
    public void testCovarianceConsistency() {
    }

}
