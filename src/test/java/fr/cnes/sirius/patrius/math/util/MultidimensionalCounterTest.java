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
package fr.cnes.sirius.patrius.math.util;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 *
 */
public class MultidimensionalCounterTest {
    @Test
    public void testPreconditions() {
        MultidimensionalCounter c;

        try {
            c = new MultidimensionalCounter(0, 1);
            Assert.fail("NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException e) {
            // Expected.
        }
        try {
            c = new MultidimensionalCounter(2, 0);
            Assert.fail("NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException e) {
            // Expected.
        }
        try {
            c = new MultidimensionalCounter(-1, 1);
            Assert.fail("NotStrictlyPositiveException expected");
        } catch (final NotStrictlyPositiveException e) {
            // Expected.
        }

        c = new MultidimensionalCounter(2, 3);
        try {
            c.getCount(1, 1, 1);
            Assert.fail("DimensionMismatchException expected");
        } catch (final DimensionMismatchException e) {
            // Expected.
        }
        try {
            c.getCount(3, 1);
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException e) {
            // Expected.
        }
        try {
            c.getCount(0, -1);
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException e) {
            // Expected.
        }
        try {
            c.getCounts(-1);
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException e) {
            // Expected.
        }
        try {
            c.getCounts(6);
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException e) {
            // Expected.
        }
    }

    @Test
    public void testIteratorPreconditions() {
        final MultidimensionalCounter.Iterator iter = (new MultidimensionalCounter(2, 3)).iterator();
        try {
            iter.getCount(-1);
            Assert.fail("IndexOutOfBoundsException expected");
        } catch (final IndexOutOfBoundsException e) {
            // Expected.
        }
        try {
            iter.getCount(2);
            Assert.fail("IndexOutOfBoundsException expected");
        } catch (final IndexOutOfBoundsException e) {
            // Expected.
        }
    }

    @Test
    public void testMulti2UniConversion() {
        final MultidimensionalCounter c = new MultidimensionalCounter(2, 4, 5);
        Assert.assertEquals(c.getCount(1, 2, 3), 33);
    }

    @Test
    public void testAccessors() {
        final int[] originalSize = new int[] { 2, 6, 5 };
        final MultidimensionalCounter c = new MultidimensionalCounter(originalSize);
        final int nDim = c.getDimension();
        Assert.assertEquals(nDim, originalSize.length);

        final int[] size = c.getSizes();
        for (int i = 0; i < nDim; i++) {
            Assert.assertEquals(originalSize[i], size[i]);
        }
    }

    @Test
    public void testIterationConsistency() {
        final MultidimensionalCounter c = new MultidimensionalCounter(2, 3, 4);
        final int[][] expected = new int[][] {
            { 0, 0, 0 },
            { 0, 0, 1 },
            { 0, 0, 2 },
            { 0, 0, 3 },
            { 0, 1, 0 },
            { 0, 1, 1 },
            { 0, 1, 2 },
            { 0, 1, 3 },
            { 0, 2, 0 },
            { 0, 2, 1 },
            { 0, 2, 2 },
            { 0, 2, 3 },
            { 1, 0, 0 },
            { 1, 0, 1 },
            { 1, 0, 2 },
            { 1, 0, 3 },
            { 1, 1, 0 },
            { 1, 1, 1 },
            { 1, 1, 2 },
            { 1, 1, 3 },
            { 1, 2, 0 },
            { 1, 2, 1 },
            { 1, 2, 2 },
            { 1, 2, 3 }
        };

        final int totalSize = c.getSize();
        final int nDim = c.getDimension();
        final MultidimensionalCounter.Iterator iter = c.iterator();
        for (int i = 0; i < totalSize; i++) {
            if (!iter.hasNext()) {
                Assert.fail("Too short");
            }
            final int uniDimIndex = iter.next();
            Assert.assertEquals("Wrong iteration at " + i, i, uniDimIndex);

            for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                Assert.assertEquals("Wrong multidimensional index for [" + i + "][" + dimIndex + "]",
                    expected[i][dimIndex], iter.getCount(dimIndex));
            }

            Assert.assertEquals("Wrong unidimensional index for [" + i + "]",
                c.getCount(expected[i]), uniDimIndex);

            final int[] indices = c.getCounts(uniDimIndex);
            for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                Assert.assertEquals("Wrong multidimensional index for [" + i + "][" + dimIndex + "]",
                    expected[i][dimIndex], indices[dimIndex]);
            }
        }

        if (iter.hasNext()) {
            Assert.fail("Too long");
        }
    }
}
