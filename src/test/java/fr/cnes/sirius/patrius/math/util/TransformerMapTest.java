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

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;

/**
 * @version $Id: TransformerMapTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class TransformerMapTest {
    /**
     *
     */
    @Test
    public void testPutTransformer() {
        final NumberTransformer expected = new DefaultTransformer();

        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertEquals(expected, map.getTransformer(TransformerMapTest.class));
    }

    /**
     *
     */
    @Test
    public void testContainsClass() {
        final NumberTransformer expected = new DefaultTransformer();
        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertTrue(map.containsClass(TransformerMapTest.class));
    }

    /**
     *
     */
    @Test
    public void testContainsTransformer() {
        final NumberTransformer expected = new DefaultTransformer();
        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertTrue(map.containsTransformer(expected));
    }

    /**
     *
     */
    @Test
    public void testRemoveTransformer() {
        final NumberTransformer expected = new DefaultTransformer();

        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertTrue(map.containsClass(TransformerMapTest.class));
        Assert.assertTrue(map.containsTransformer(expected));
        map.removeTransformer(TransformerMapTest.class);
        Assert.assertFalse(map.containsClass(TransformerMapTest.class));
        Assert.assertFalse(map.containsTransformer(expected));
    }

    /**
     *
     */
    @Test
    public void testClear() {
        final NumberTransformer expected = new DefaultTransformer();

        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertTrue(map.containsClass(TransformerMapTest.class));
        map.clear();
        Assert.assertFalse(map.containsClass(TransformerMapTest.class));
    }

    /**
     *
     */
    @Test
    public void testClasses() {
        final NumberTransformer expected = new DefaultTransformer();
        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertTrue(map.classes().contains(TransformerMapTest.class));
    }

    /**
     *
     */
    @Test
    public void testTransformers() {
        final NumberTransformer expected = new DefaultTransformer();
        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertTrue(map.transformers().contains(expected));
    }

    @Test
    public void testSerial() {
        final NumberTransformer expected = new DefaultTransformer();
        final TransformerMap map = new TransformerMap();
        map.putTransformer(TransformerMapTest.class, expected);
        Assert.assertEquals(map, TestUtils.serializeAndRecover(map));
    }

    @Test
    public void testEquals() {
        final TransformerMap map = new TransformerMap();
        Assert.assertTrue(map.equals(map));
        final NumberTransformer d = map.getTransformer(double.class);
        Assert.assertFalse(map.equals(d));
        Assert.assertNotNull(map.hashCode());

    }
}
