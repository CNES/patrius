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
package fr.cnes.sirius.patrius.math.util;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.fraction.FractionConversionException;
import fr.cnes.sirius.patrius.math.fraction.FractionField;

public class OpenIntToFieldTest {

    private final Map<Integer, Fraction> javaMap = new HashMap<Integer, Fraction>();
    private final FractionField field = FractionField.getInstance();

    @Before
    public void setUp() throws FractionConversionException {
        this.javaMap.put(50, new Fraction(100.0));
        this.javaMap.put(75, new Fraction(75.0));
        this.javaMap.put(25, new Fraction(500.0));
        this.javaMap.put(Integer.MAX_VALUE, new Fraction(Integer.MAX_VALUE));
        this.javaMap.put(0, new Fraction(-1.0));
        this.javaMap.put(1, new Fraction(0.0));
        this.javaMap.put(33, new Fraction(-0.1));
        this.javaMap.put(23234234, new Fraction(-242343.0));
        this.javaMap.put(23321, new Fraction(Integer.MIN_VALUE));
        this.javaMap.put(-4444, new Fraction(332.0));
        this.javaMap.put(-1, new Fraction(-2323.0));
        this.javaMap.put(Integer.MIN_VALUE, new Fraction(44.0));

        /* Add a few more to cause the table to rehash */
        this.javaMap.putAll(this.generate());

    }

    private Map<Integer, Fraction> generate() {
        final Map<Integer, Fraction> map = new HashMap<Integer, Fraction>();
        final Random r = new Random();
        double dd = 0;
        for (int i = 0; i < 2000; ++i) {
            dd = r.nextDouble();
        }
        try {
            map.put(r.nextInt(), new Fraction(dd));
        } catch (final FractionConversionException e) {
            throw new IllegalStateException("Invalid :" + dd, e);
        }
        return map;
    }

    private OpenIntToFieldHashMap<Fraction> createFromJavaMap(final Field<Fraction> field) {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(field);
        for (final Map.Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            map.put(mapEntry.getKey(), mapEntry.getValue());
        }
        return map;
    }

    @Test
    public void testPutAndGetWith0ExpectedSize() {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(this.field, 0);
        this.assertPutAndGet(map);
    }

    @Test
    public void testPutAndGetWithExpectedSize() {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(this.field, 500);
        this.assertPutAndGet(map);
    }

    @Test
    public void testPutAndGet() {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(this.field);
        this.assertPutAndGet(map);
    }

    private void assertPutAndGet(final OpenIntToFieldHashMap<Fraction> map) {
        this.assertPutAndGet(map, 0, new HashSet<Integer>());
    }

    private void assertPutAndGet(final OpenIntToFieldHashMap<Fraction> map, int mapSize,
                                 final Set<Integer> keysInMap) {
        Assert.assertEquals(mapSize, map.size());
        for (final Map.Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            map.put(mapEntry.getKey(), mapEntry.getValue());
            if (!keysInMap.contains(mapEntry.getKey())) {
                ++mapSize;
            }
            Assert.assertEquals(mapSize, map.size());
            Assert.assertEquals(mapEntry.getValue(), map.get(mapEntry.getKey()));
        }
    }

    @Test
    public void testPutAbsentOnExisting() {
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        int size = this.javaMap.size();
        for (final Map.Entry<Integer, Fraction> mapEntry : this.generateAbsent().entrySet()) {
            map.put(mapEntry.getKey(), mapEntry.getValue());
            Assert.assertEquals(++size, map.size());
            Assert.assertEquals(mapEntry.getValue(), map.get(mapEntry.getKey()));
        }
    }

    @Test
    public void testPutOnExisting() {
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        for (final Map.Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            map.put(mapEntry.getKey(), mapEntry.getValue());
            Assert.assertEquals(this.javaMap.size(), map.size());
            Assert.assertEquals(mapEntry.getValue(), map.get(mapEntry.getKey()));
        }
    }

    @Test
    public void testGetAbsent() {
        final Map<Integer, Fraction> generated = this.generateAbsent();
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);

        for (final Map.Entry<Integer, Fraction> mapEntry : generated.entrySet()) {
            Assert.assertTrue(this.field.getZero().equals(map.get(mapEntry.getKey())));
        }
    }

    @Test
    public void testGetFromEmpty() {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(this.field);
        Assert.assertTrue(this.field.getZero().equals(map.get(5)));
        Assert.assertTrue(this.field.getZero().equals(map.get(0)));
        Assert.assertTrue(this.field.getZero().equals(map.get(50)));
    }

    @Test
    public void testRemove() {
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        int mapSize = this.javaMap.size();
        Assert.assertEquals(mapSize, map.size());
        for (final Map.Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            map.remove(mapEntry.getKey());
            Assert.assertEquals(--mapSize, map.size());
            Assert.assertTrue(this.field.getZero().equals(map.get(mapEntry.getKey())));
        }

        /* Ensure that put and get still work correctly after removals */
        this.assertPutAndGet(map);
    }

    /* This time only remove some entries */
    @Test
    public void testRemove2() {
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        int mapSize = this.javaMap.size();
        int count = 0;
        final Set<Integer> keysInMap = new HashSet<Integer>(this.javaMap.keySet());
        for (final Map.Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            keysInMap.remove(mapEntry.getKey());
            map.remove(mapEntry.getKey());
            Assert.assertEquals(--mapSize, map.size());
            Assert.assertTrue(this.field.getZero().equals(map.get(mapEntry.getKey())));
            if (count++ > 5) {
                break;
            }
        }

        /* Ensure that put and get still work correctly after removals */
        this.assertPutAndGet(map, mapSize, keysInMap);
    }

    @Test
    public void testRemoveFromEmpty() {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(this.field);
        Assert.assertTrue(this.field.getZero().equals(map.remove(50)));
    }

    @Test
    public void testRemoveAbsent() {
        final Map<Integer, Fraction> generated = this.generateAbsent();

        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        final int mapSize = map.size();

        for (final Map.Entry<Integer, Fraction> mapEntry : generated.entrySet()) {
            map.remove(mapEntry.getKey());
            Assert.assertEquals(mapSize, map.size());
            Assert.assertTrue(this.field.getZero().equals(map.get(mapEntry.getKey())));
        }
    }

    /**
     * Returns a map with at least 100 elements where each element is absent from javaMap.
     */
    private Map<Integer, Fraction> generateAbsent() {
        final Map<Integer, Fraction> generated = new HashMap<Integer, Fraction>();
        do {
            generated.putAll(this.generate());
            for (final Integer key : this.javaMap.keySet()) {
                generated.remove(key);
            }
        } while (generated.size() < 100);
        return generated;
    }

    @Test
    public void testCopy() {
        final OpenIntToFieldHashMap<Fraction> copy =
            new OpenIntToFieldHashMap<Fraction>(this.createFromJavaMap(this.field));
        Assert.assertEquals(this.javaMap.size(), copy.size());

        for (final Map.Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            Assert.assertEquals(mapEntry.getValue(), copy.get(mapEntry.getKey()));
        }
    }

    @Test
    public void testContainsKey() {
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        for (final Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            Assert.assertTrue(map.containsKey(mapEntry.getKey()));
        }
        for (final Map.Entry<Integer, Fraction> mapEntry : this.generateAbsent().entrySet()) {
            Assert.assertFalse(map.containsKey(mapEntry.getKey()));
        }
        for (final Entry<Integer, Fraction> mapEntry : this.javaMap.entrySet()) {
            final int key = mapEntry.getKey();
            Assert.assertTrue(map.containsKey(key));
            map.remove(key);
            Assert.assertFalse(map.containsKey(key));
        }
    }

    @Test
    public void testIterator() {
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        final OpenIntToFieldHashMap<Fraction>.Iterator iterator = map.iterator();
        for (int i = 0; i < map.size(); ++i) {
            Assert.assertTrue(iterator.hasNext());
            iterator.advance();
            final int key = iterator.key();
            Assert.assertTrue(map.containsKey(key));
            Assert.assertEquals(this.javaMap.get(key), map.get(key));
            Assert.assertEquals(this.javaMap.get(key), iterator.value());
            Assert.assertTrue(this.javaMap.containsKey(key));
        }
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.advance();
            Assert.fail("an exception should have been thrown");
        } catch (final NoSuchElementException nsee) {
            // expected
        }
    }

    @Test
    public void testConcurrentModification() {
        final OpenIntToFieldHashMap<Fraction> map = this.createFromJavaMap(this.field);
        final OpenIntToFieldHashMap<Fraction>.Iterator iterator = map.iterator();
        map.put(3, new Fraction(3));
        try {
            iterator.advance();
            Assert.fail("an exception should have been thrown");
        } catch (final ConcurrentModificationException cme) {
            // expected
        }
    }

    /**
     * Regression test for a bug in findInsertionIndex where the hashing in the second probing
     * loop was inconsistent with the first causing duplicate keys after the right sequence
     * of puts and removes.
     */
    @Test
    public void testPutKeysWithCollisions() {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(this.field);
        final int key1 = -1996012590;
        final Fraction value1 = new Fraction(1);
        map.put(key1, value1);
        final int key2 = 835099822;
        map.put(key2, value1);
        final int key3 = 1008859686;
        map.put(key3, value1);
        Assert.assertEquals(value1, map.get(key3));
        Assert.assertEquals(3, map.size());

        map.remove(key2);
        final Fraction value2 = new Fraction(2);
        map.put(key3, value2);
        Assert.assertEquals(value2, map.get(key3));
        Assert.assertEquals(2, map.size());
    }

    /**
     * Similar to testPutKeysWithCollisions() but exercises the codepaths in a slightly
     * different manner.
     */
    @Test
    public void testPutKeysWithCollision2() {
        final OpenIntToFieldHashMap<Fraction> map = new OpenIntToFieldHashMap<Fraction>(this.field);
        final int key1 = 837989881;
        final Fraction value1 = new Fraction(1);
        map.put(key1, value1);
        final int key2 = 476463321;
        map.put(key2, value1);
        Assert.assertEquals(2, map.size());
        Assert.assertEquals(value1, map.get(key2));

        map.remove(key1);
        final Fraction value2 = new Fraction(2);
        map.put(key2, value2);
        Assert.assertEquals(1, map.size());
        Assert.assertEquals(value2, map.get(key2));
    }

}
