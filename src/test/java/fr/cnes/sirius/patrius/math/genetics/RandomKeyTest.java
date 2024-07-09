/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.genetics;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class RandomKeyTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor1() {
        new DummyRandomKey(new Double[] { 0.2, 0.3, 1.2 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor2() {
        new DummyRandomKey(new Double[] { 0.2, 0.3, -0.2 });
    }

    @Test
    public void testIsSame() {
        final DummyRandomKey drk1 = new DummyRandomKey(new Double[] { 0.4, 0.1, 0.5, 0.8, 0.2 });
        final DummyRandomKey drk2 = new DummyRandomKey(new Double[] { 0.4, 0.1, 0.5, 0.8, 0.2 });
        final DummyRandomKey drk3 = new DummyRandomKey(new Double[] { 0.4, 0.15, 0.5, 0.8, 0.2 });
        final DummyRandomKey drk4 = new DummyRandomKey(new Double[] { 0.4, 0.25, 0.5, 0.8, 0.2 });
        final DummyRandomKey drk5 = new DummyRandomKey(new Double[] { 0.4, 0.25, 0.5, 0.8, 0.2, 0.5 });

        Assert.assertTrue(drk1.isSame(drk2));
        Assert.assertTrue(drk2.isSame(drk3));
        Assert.assertFalse(drk3.isSame(drk4));
        Assert.assertFalse(drk4.isSame(drk5));
    }

    @Test
    public void testDecode() {
        final DummyRandomKey drk = new DummyRandomKey(new Double[] { 0.4, 0.1, 0.5, 0.8, 0.2 });
        final List<String> decoded = drk.decode(Arrays.asList(new String[] { "a", "b", "c", "d", "e" }));

        Assert.assertEquals("b", decoded.get(0));
        Assert.assertEquals("e", decoded.get(1));
        Assert.assertEquals("a", decoded.get(2));
        Assert.assertEquals("c", decoded.get(3));
        Assert.assertEquals("d", decoded.get(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRepresentation() {
        new DummyRandomKey(new Double[] { 0.1, 0.1, 2d, 0.8, 0.2 });
    }

    @Test
    public void testRandomPermutation() {
        // never generate an invalid one
        for (int i = 0; i < 10; i++) {
            final DummyRandomKey drk = new DummyRandomKey(RandomKey.randomPermutation(20));
            Assert.assertNotNull(drk);
        }
    }

    @Test
    public void testIdentityPermutation() {
        final DummyRandomKey drk = new DummyRandomKey(RandomKey.identityPermutation(5));
        final List<String> decoded = drk.decode(Arrays.asList(new String[] { "a", "b", "c", "d", "e" }));

        Assert.assertEquals("a", decoded.get(0));
        Assert.assertEquals("b", decoded.get(1));
        Assert.assertEquals("c", decoded.get(2));
        Assert.assertEquals("d", decoded.get(3));
        Assert.assertEquals("e", decoded.get(4));
    }

    @Test
    public void testComparatorPermutation() {
        final List<String> data = Arrays.asList(new String[] { "x", "b", "c", "z", "b" });

        List<Double> permutation = RandomKey.comparatorPermutation(data, new Comparator<String>(){
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        });
        Double[] permArr = new Double[data.size()];
        permArr = permutation.toArray(permArr);
        Assert.assertArrayEquals(new Double[] { 0.6, 0.0, 0.4, 0.8, 0.2 }, permArr);
        List<String> decodedData = new DummyRandomKey(permutation).decode(data);
        Assert.assertEquals("b", decodedData.get(0));
        Assert.assertEquals("b", decodedData.get(1));
        Assert.assertEquals("c", decodedData.get(2));
        Assert.assertEquals("x", decodedData.get(3));
        Assert.assertEquals("z", decodedData.get(4));

        permutation = RandomKey.comparatorPermutation(data, new Comparator<String>(){
            @Override
            public int compare(final String o1, final String o2) {
                return o2.compareTo(o1);
            }
        });
        permArr = new Double[data.size()];
        permArr = permutation.toArray(permArr);
        Assert.assertArrayEquals(new Double[] { 0.2, 0.6, 0.4, 0.0, 0.8 }, permArr);
        decodedData = new DummyRandomKey(permutation).decode(data);
        Assert.assertEquals("z", decodedData.get(0));
        Assert.assertEquals("x", decodedData.get(1));
        Assert.assertEquals("c", decodedData.get(2));
        Assert.assertEquals("b", decodedData.get(3));
        Assert.assertEquals("b", decodedData.get(4));
    }

    @Test
    public void testInducedPermutation() {
        final List<String> origData = Arrays.asList(new String[] { "a", "b", "c", "d", "d" });
        final List<String> permutedData = Arrays.asList(new String[] { "d", "b", "c", "a", "d" });

        final DummyRandomKey drk = new DummyRandomKey(RandomKey.inducedPermutation(origData, permutedData));
        final List<String> decoded = drk.decode(origData);

        Assert.assertEquals("d", decoded.get(0));
        Assert.assertEquals("b", decoded.get(1));
        Assert.assertEquals("c", decoded.get(2));
        Assert.assertEquals("a", decoded.get(3));
        Assert.assertEquals("d", decoded.get(4));

        try {
            RandomKey.inducedPermutation(
                Arrays.asList(new String[] { "a", "b", "c", "d", "d" }),
                Arrays.asList(new String[] { "a", "b", "c", "d" })
                );
            Assert.fail("Uncaught exception");
        } catch (final IllegalArgumentException e) {
            // no-op
        }
        try {
            RandomKey.inducedPermutation(
                Arrays.asList(new String[] { "a", "b", "c", "d", "d" }),
                Arrays.asList(new String[] { "a", "b", "c", "d", "f" })
                );
            Assert.fail("Uncaught exception");
        } catch (final IllegalArgumentException e) {
            // no-op
        }
    }

    @Test
    public void testEqualRepr() {
        final DummyRandomKey drk = new DummyRandomKey(new Double[] { 0.2, 0.2, 0.5 });
        final List<String> decodedData = drk.decode(Arrays.asList(new String[] { "a", "b", "c" }));
        Assert.assertEquals("a", decodedData.get(0));
        Assert.assertEquals("b", decodedData.get(1));
        Assert.assertEquals("c", decodedData.get(2));
    }

}
