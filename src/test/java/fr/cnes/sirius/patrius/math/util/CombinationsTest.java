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

import java.util.Comparator;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;

/**
 * Tests for the {@link Combinations} class.
 * 
 */
public class CombinationsTest {
    @Test
    public void testAccessor1() {
        final int n = 5;
        final int k = 3;
        Assert.assertEquals(n, new Combinations(n, k).getN());
    }

    @Test
    public void testAccessor2() {
        final int n = 5;
        final int k = 3;
        Assert.assertEquals(k, new Combinations(n, k).getK());
    }

    @Test
    public void testLexicographicIterator() {
        this.checkLexicographicIterator(new Combinations(5, 3));
        this.checkLexicographicIterator(new Combinations(6, 4));
        this.checkLexicographicIterator(new Combinations(8, 2));
        this.checkLexicographicIterator(new Combinations(6, 1));
        this.checkLexicographicIterator(new Combinations(3, 3));
        this.checkLexicographicIterator(new Combinations(1, 1));
        this.checkLexicographicIterator(new Combinations(1, 0));
        this.checkLexicographicIterator(new Combinations(0, 0));
        this.checkLexicographicIterator(new Combinations(4, 2));
        this.checkLexicographicIterator(new Combinations(123, 2));
    }

    @Test(expected = DimensionMismatchException.class)
    public void testLexicographicComparatorWrongIterate1() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = new Combinations(n, k).comparator();
        comp.compare(new int[] { 1 }, new int[] { 0, 1, 2 });
    }

    @Test(expected = DimensionMismatchException.class)
    public void testLexicographicComparatorWrongIterate2() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = new Combinations(n, k).comparator();
        comp.compare(new int[] { 0, 1, 2 }, new int[] { 0, 1, 2, 3 });
    }

    @Test(expected = OutOfRangeException.class)
    public void testLexicographicComparatorWrongIterate3() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = new Combinations(n, k).comparator();
        comp.compare(new int[] { 1, 2, 5 }, new int[] { 0, 1, 2 });
    }

    @Test(expected = OutOfRangeException.class)
    public void testLexicographicComparatorWrongIterate4() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = new Combinations(n, k).comparator();
        comp.compare(new int[] { 1, 2, 4 }, new int[] { -1, 1, 2 });
    }

    @Test
    public void testLexicographicComparator() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = new Combinations(n, k).comparator();
        Assert.assertEquals(1, comp.compare(new int[] { 1, 2, 4 },
            new int[] { 1, 2, 3 }));
        Assert.assertEquals(-1, comp.compare(new int[] { 0, 1, 4 },
            new int[] { 0, 2, 4 }));
        Assert.assertEquals(0, comp.compare(new int[] { 1, 3, 4 },
            new int[] { 1, 3, 4 }));
    }

    /**
     * Check that iterates can be passed unsorted.
     */
    @Test
    public void testLexicographicComparatorUnsorted() {
        final int n = 5;
        final int k = 3;
        final Comparator<int[]> comp = new Combinations(n, k).comparator();
        Assert.assertEquals(1, comp.compare(new int[] { 1, 4, 2 },
            new int[] { 1, 3, 2 }));
        Assert.assertEquals(-1, comp.compare(new int[] { 0, 4, 1 },
            new int[] { 0, 4, 2 }));
        Assert.assertEquals(0, comp.compare(new int[] { 1, 4, 3 },
            new int[] { 1, 3, 4 }));
    }

    @Test
    public void testEmptyCombination() {
        final Iterator<int[]> iter = new Combinations(12345, 0).iterator();
        Assert.assertTrue(iter.hasNext());
        final int[] c = iter.next();
        Assert.assertEquals(0, c.length);
        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void testFullSetCombination() {
        final int n = 67;
        final Iterator<int[]> iter = new Combinations(n, n).iterator();
        Assert.assertTrue(iter.hasNext());
        final int[] c = iter.next();
        Assert.assertEquals(n, c.length);

        for (int i = 0; i < n; i++) {
            Assert.assertEquals(i, c[i]);
        }

        Assert.assertFalse(iter.hasNext());
    }

    /**
     * Verifies that the iterator generates a lexicographically
     * increasing sequence of b(n,k) arrays, each having length k
     * and each array itself increasing.
     * 
     * @param c
     *        Combinations.
     */
    private void checkLexicographicIterator(final Combinations c) {
        final Comparator<int[]> comp = c.comparator();
        final int n = c.getN();
        final int k = c.getK();

        int[] lastIterate = null;

        long numIterates = 0;
        for (final int[] iterate : c) {
            Assert.assertEquals(k, iterate.length);

            // Check that the sequence of iterates is ordered.
            if (lastIterate != null) {
                Assert.assertTrue(comp.compare(iterate, lastIterate) == 1);
            }

            // Check that each iterate is ordered.
            for (int i = 1; i < iterate.length; i++) {
                Assert.assertTrue(iterate[i] > iterate[i - 1]);
            }

            lastIterate = iterate;
            ++numIterates;
        }

        // Check the number of iterates.
        Assert.assertEquals(CombinatoricsUtils.binomialCoefficient(n, k),
            numIterates);
    }

    @Test
    public void testCombinationsIteratorFail() {
        try {
            new Combinations(4, 5).iterator();
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }

        try {
            new Combinations(-1, -2).iterator();
            Assert.fail("expecting MathIllegalArgumentException");
        } catch (final MathIllegalArgumentException ex) {
            // ignored
        }
    }
}
