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
package fr.cnes.sirius.patrius.math.transform;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * JUnit Test for HadamardTransformerTest
 * 
 * @see fr.cnes.sirius.patrius.math.transform.FastHadamardTransformer
 */
public final class FastHadamardTransformerTest {

    /**
     * Test of transformer for the a 8-point FHT (means n=8)
     */
    @Test
    public void test8Points() {
        this.checkAllTransforms(new int[] { 1, 4, -2, 3, 0, 1, 4, -1 },
            new int[] { 10, -4, 2, -4, 2, -12, 6, 8 });
    }

    /**
     * Test of transformer for the a 4-points FHT (means n=4)
     */
    @Test
    public void test4Points() {
        this.checkAllTransforms(new int[] { 1, 2, 3, 4 },
            new int[] { 10, -2, -4, 0 });
    }

    /**
     * Test the inverse transform of an integer vector is not always an integer vector
     */
    @Test
    public void testNoIntInverse() {
        final FastHadamardTransformer transformer = new FastHadamardTransformer();
        final double[] x = transformer.transform(new double[] { 0, 1, 0, 1 }, TransformType.INVERSE);
        Assert.assertEquals(0.5, x[0], 0);
        Assert.assertEquals(-0.5, x[1], 0);
        Assert.assertEquals(0.0, x[2], 0);
        Assert.assertEquals(0.0, x[3], 0);
    }

    /**
     * Test of transformer for wrong number of points
     */
    @Test
    public void test3Points() {
        try {
            new FastHadamardTransformer().transform(new double[3], TransformType.FORWARD);
            Assert.fail("an exception should have been thrown");
        } catch (final IllegalArgumentException iae) {
            // expected
        }
    }

    private void checkAllTransforms(final int[] x, final int[] y) {
        this.checkDoubleTransform(x, y);
        this.checkInverseDoubleTransform(x, y);
        this.checkIntTransform(x, y);
    }

    private void checkDoubleTransform(final int[] x, final int[] y) {
        // Initiate the transformer
        final FastHadamardTransformer transformer = new FastHadamardTransformer();

        // check double transform
        final double[] dX = new double[x.length];
        for (int i = 0; i < dX.length; ++i) {
            dX[i] = x[i];
        }
        final double dResult[] = transformer.transform(dX, TransformType.FORWARD);
        for (int i = 0; i < dResult.length; i++) {
            // compare computed results to precomputed results
            Assert.assertTrue(Precision.equals(y[i], dResult[i], 1));
        }
    }

    private void checkIntTransform(final int[] x, final int[] y) {
        // Initiate the transformer
        final FastHadamardTransformer transformer = new FastHadamardTransformer();

        // check integer transform
        final int iResult[] = transformer.transform(x);
        for (int i = 0; i < iResult.length; i++) {
            // compare computed results to precomputed results
            Assert.assertEquals(y[i], iResult[i]);
        }

    }

    private void checkInverseDoubleTransform(final int[] x, final int[] y) {
        // Initiate the transformer
        final FastHadamardTransformer transformer = new FastHadamardTransformer();

        // check double transform
        final double[] dY = new double[y.length];
        for (int i = 0; i < dY.length; ++i) {
            dY[i] = y[i];
        }
        final double dResult[] = transformer.transform(dY, TransformType.INVERSE);
        for (int i = 0; i < dResult.length; i++) {
            // compare computed results to precomputed results
            Assert.assertTrue(Precision.equals(x[i], dResult[i], 1));
        }

    }

}
