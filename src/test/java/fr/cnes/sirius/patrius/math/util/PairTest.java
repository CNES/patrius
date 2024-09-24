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
 * VERSION:4.11:DM:DM-3286:22/05/2023:[PATRIUS] Ajout d'une methode toString() a la classe Pair
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link Pair}.
 */
public class PairTest {

    @Test
    public void testAccessor() {
        final Pair<Integer, Double> p = new Pair<>(new Integer(1), new Double(2));
        Assert.assertEquals(new Integer(1), p.getKey());
        Assert.assertEquals(new Double(2), p.getValue(), Math.ulp(1d));
    }

    @Test
    public void testAccessor2() {
        final Pair<Integer, Double> p = new Pair<>(new Integer(1), new Double(2));

        // Check that both APIs refer to the same data.

        Assert.assertTrue(p.getFirst() == p.getKey());
        Assert.assertTrue(p.getSecond() == p.getValue());
    }

    @Test
    public void testEquals() {
        Pair<Integer, Double> p1 = new Pair<>(null, null);
        Assert.assertFalse(p1.equals(null));

        Pair<Integer, Double> p2 = new Pair<>(null, null);
        Assert.assertTrue(p1.equals(p2));

        final Pair<Integer, Double> p2bis = new Pair<>(p2);
        Assert.assertTrue(p2bis.equals(p2));
        Assert.assertFalse(p2bis.equals(1));
        Assert.assertEquals(0, p2bis.hashCode(), 0);

        p1 = new Pair<>(new Integer(1), new Double(2));
        Assert.assertFalse(p1.equals(p2));

        p2 = new Pair<>(new Integer(1), new Double(2));
        Assert.assertTrue(p1.equals(p2));

        final Pair<Integer, Float> p3 = new Pair<>(new Integer(1), new Float(2));
        Assert.assertFalse(p1.equals(p3));
    }

    @Test
    public void testHashCode() {
        final MyInteger m1 = new MyInteger(1);
        final MyInteger m2 = new MyInteger(1);

        final Pair<MyInteger, MyInteger> p1 = new Pair<>(m1, m1);
        final Pair<MyInteger, MyInteger> p2 = new Pair<>(m2, m2);
        // Same contents, same hash code.
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        // Different contents, different hash codes.
        m2.set(2);
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
    }
    
    @Test
    public void testToString() {
        // Test with native integer and Double
        Pair<Integer, Double> p1 = new Pair<>(new Integer(1), new Double(2.6));
        Assert.assertTrue(p1.toString().equalsIgnoreCase("{1, 2.6}"));

        // Test with created class with a toString method
        final MyInteger m1 = new MyInteger(3);
        final MyInteger m2 = new MyInteger(4);
        final Pair<MyInteger, MyInteger> p2 = new Pair<>(m1, m2);
        Assert.assertTrue(p2.toString().equals("{3, 4}"));
    }

    /**
     * A mutable integer.
     */
    private static class MyInteger {
        private int i;

        public MyInteger(final int i) {
            this.i = i;
        }

        public void set(final int i) {
            this.i = i;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof MyInteger)) {
                return false;
            }
            return this.i == ((MyInteger) o).i;
        }

        @Override
        public int hashCode() {
            return this.i;
        }
        
        @Override
        public String toString() {
            return "" + this.i + "";
        }
    }
}
