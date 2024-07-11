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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;

/**
 * Test cases for the {@link Frequency} class.
 * 
 * @version $Id: FrequencyTest.java 18108 2017-10-04 06:45:27Z bignon $
 */

public final class FrequencyTest {
    private final long oneL = 1;
    private final long twoL = 2;
    private final long threeL = 3;
    private final int oneI = 1;
    private final int twoI = 2;
    private final int threeI = 3;
    private final double tolerance = 10E-15;
    private Frequency f = null;

    @Before
    public void setUp() {
        this.f = new Frequency();
    }

    /** test freq counts */
    @Test
    public void testCounts() {
        Assert.assertEquals("total count", 0, this.f.getSumFreq());
        this.f.addValue(this.oneL);
        this.f.addValue(this.twoL);
        this.f.addValue(1);
        this.f.addValue(this.oneI);
        Assert.assertEquals("one frequency count", 3, this.f.getCount(1));
        Assert.assertEquals("two frequency count", 1, this.f.getCount(2));
        Assert.assertEquals("three frequency count", 0, this.f.getCount(3));
        Assert.assertEquals("total count", 4, this.f.getSumFreq());
        Assert.assertEquals("zero cumulative frequency", 0, this.f.getCumFreq(0));
        Assert.assertEquals("one cumulative frequency", 3, this.f.getCumFreq(1));
        Assert.assertEquals("two cumulative frequency", 4, this.f.getCumFreq(2));
        Assert.assertEquals("Integer argument cum freq", 4, this.f.getCumFreq(Integer.valueOf(2)));
        Assert.assertEquals("five cumulative frequency", 4, this.f.getCumFreq(5));
        Assert.assertEquals("foo cumulative frequency", 0, this.f.getCumFreq("foo"));

        this.f.clear();
        Assert.assertEquals("total count", 0, this.f.getSumFreq());

        // userguide examples -------------------------------------------------------------------
        this.f.addValue("one");
        this.f.addValue("One");
        this.f.addValue("oNe");
        this.f.addValue("Z");
        Assert.assertEquals("one cumulative frequency", 1, this.f.getCount("one"));
        Assert.assertEquals("Z cumulative pct", 0.5, this.f.getCumPct("Z"), this.tolerance);
        Assert.assertEquals("z cumulative pct", 1.0, this.f.getCumPct("z"), this.tolerance);
        Assert.assertEquals("Ot cumulative pct", 0.25, this.f.getCumPct("Ot"), this.tolerance);
        this.f.clear();

        this.f = null;
        Frequency f = new Frequency();
        f.addValue(1);
        f.addValue(Integer.valueOf(1));
        f.addValue(Long.valueOf(1));
        f.addValue(2);
        f.addValue(Integer.valueOf(-1));
        Assert.assertEquals("1 count", 3, f.getCount(1));
        Assert.assertEquals("1 count", 3, f.getCount(Integer.valueOf(1)));
        Assert.assertEquals("0 cum pct", 0.2, f.getCumPct(0), this.tolerance);
        Assert.assertEquals("1 pct", 0.6, f.getPct(Integer.valueOf(1)), this.tolerance);
        Assert.assertEquals("-2 cum pct", 0, f.getCumPct(-2), this.tolerance);
        Assert.assertEquals("10 cum pct", 1, f.getCumPct(10), this.tolerance);

        f = null;
        f = new Frequency(String.CASE_INSENSITIVE_ORDER);
        f.addValue("one");
        f.addValue("One");
        f.addValue("oNe");
        f.addValue("Z");
        Assert.assertEquals("one count", 3, f.getCount("one"));
        Assert.assertEquals("Z cumulative pct -- case insensitive", 1, f.getCumPct("Z"), this.tolerance);
        Assert.assertEquals("z cumulative pct -- case insensitive", 1, f.getCumPct("z"), this.tolerance);

        f = null;
        f = new Frequency();
        Assert.assertEquals(0L, f.getCount('a'));
        Assert.assertEquals(0L, f.getCumFreq('b'));
        TestUtils.assertEquals(Double.NaN, f.getPct('a'), 0.0);
        TestUtils.assertEquals(Double.NaN, f.getCumPct('b'), 0.0);
        f.addValue('a');
        f.addValue('b');
        f.addValue('c');
        f.addValue('d');
        Assert.assertEquals(1L, f.getCount('a'));
        Assert.assertEquals(2L, f.getCumFreq('b'));
        Assert.assertEquals(0.25, f.getPct('a'), 0.0);
        Assert.assertEquals(0.5, f.getCumPct('b'), 0.0);
        Assert.assertEquals(1.0, f.getCumPct('e'), 0.0);
    }

    /** test pcts */
    @Test
    public void testPcts() {
        this.f.addValue(this.oneL);
        this.f.addValue(this.twoL);
        this.f.addValue(this.oneI);
        this.f.addValue(this.twoI);
        this.f.addValue(this.threeL);
        this.f.addValue(this.threeL);
        this.f.addValue(3);
        this.f.addValue(this.threeI);
        Assert.assertEquals("one pct", 0.25, this.f.getPct(1), this.tolerance);
        Assert.assertEquals("two pct", 0.25, this.f.getPct(Long.valueOf(2)), this.tolerance);
        Assert.assertEquals("three pct", 0.5, this.f.getPct(this.threeL), this.tolerance);
        Assert.assertEquals("five pct", 0, this.f.getPct(5), this.tolerance);
        Assert.assertEquals("foo pct", 0, this.f.getPct("foo"), this.tolerance);
        Assert.assertEquals("one cum pct", 0.25, this.f.getCumPct(1), this.tolerance);
        Assert.assertEquals("two cum pct", 0.50, this.f.getCumPct(Long.valueOf(2)), this.tolerance);
        Assert.assertEquals("Integer argument", 0.50, this.f.getCumPct(Integer.valueOf(2)), this.tolerance);
        Assert.assertEquals("three cum pct", 1.0, this.f.getCumPct(this.threeL), this.tolerance);
        Assert.assertEquals("five cum pct", 1.0, this.f.getCumPct(5), this.tolerance);
        Assert.assertEquals("zero cum pct", 0.0, this.f.getCumPct(0), this.tolerance);
        Assert.assertEquals("foo cum pct", 0, this.f.getCumPct("foo"), this.tolerance);
    }

    /** test adding incomparable values */
    @Test
    public void testAdd() {
        final char aChar = 'a';
        final char bChar = 'b';
        final String aString = "a";
        this.f.addValue(aChar);
        this.f.addValue(bChar);
        try {
            this.f.addValue(aString);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
        try {
            this.f.addValue(2);
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
        Assert.assertEquals("a pct", 0.5, this.f.getPct(aChar), this.tolerance);
        Assert.assertEquals("b cum pct", 1.0, this.f.getCumPct(bChar), this.tolerance);
        Assert.assertEquals("a string pct", 0.0, this.f.getPct(aString), this.tolerance);
        Assert.assertEquals("a string cum pct", 0.0, this.f.getCumPct(aString), this.tolerance);

        this.f = new Frequency();
        this.f.addValue("One");
        try {
            this.f.addValue(new Integer("One"));
            Assert.fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }

    /** test empty table */
    @Test
    public void testEmptyTable() {
        Assert.assertEquals("freq sum, empty table", 0, this.f.getSumFreq());
        Assert.assertEquals("count, empty table", 0, this.f.getCount(0));
        Assert.assertEquals("count, empty table", 0, this.f.getCount(Integer.valueOf(0)));
        Assert.assertEquals("cum freq, empty table", 0, this.f.getCumFreq(0));
        Assert.assertEquals("cum freq, empty table", 0, this.f.getCumFreq("x"));
        Assert.assertTrue("pct, empty table", Double.isNaN(this.f.getPct(0)));
        Assert.assertTrue("pct, empty table", Double.isNaN(this.f.getPct(Integer.valueOf(0))));
        Assert.assertTrue("cum pct, empty table", Double.isNaN(this.f.getCumPct(0)));
        Assert.assertTrue("cum pct, empty table", Double.isNaN(this.f.getCumPct(Integer.valueOf(0))));
    }

    /**
     * Tests toString()
     */
    @Test
    public void testToString() throws Exception {
        this.f.addValue(this.oneL);
        this.f.addValue(this.twoL);
        this.f.addValue(this.oneI);
        this.f.addValue(this.twoI);

        final String s = this.f.toString();
        // System.out.println(s);
        Assert.assertNotNull(s);
        final BufferedReader reader = new BufferedReader(new StringReader(s));
        String line = reader.readLine(); // header line
        Assert.assertNotNull(line);

        line = reader.readLine(); // one's or two's line
        Assert.assertNotNull(line);

        line = reader.readLine(); // one's or two's line
        Assert.assertNotNull(line);

        line = reader.readLine(); // no more elements
        Assert.assertNull(line);
    }

    /**
     * Tests equals()
     */
    @Test
    public void testEquals() {
        Assert.assertEquals(31, this.f.hashCode(), this.tolerance);
        Assert.assertTrue("equals", this.f.equals(this.f));
        Assert.assertFalse("equals", this.f.equals(this.oneI));
        final Frequency f2 = new Frequency();
        f2.addValue(this.twoI);
        Assert.assertFalse("equals", this.f.equals(f2));

    }

    @Test
    public void testIntegerValues() {
        Comparable<?> obj1 = null;
        obj1 = Integer.valueOf(1);
        final Integer int1 = Integer.valueOf(1);
        this.f.addValue(obj1);
        this.f.addValue(int1);
        this.f.addValue(2);
        this.f.addValue(Long.valueOf(2));
        Assert.assertEquals("Integer 1 count", 2, this.f.getCount(1));
        Assert.assertEquals("Integer 1 count", 2, this.f.getCount(Integer.valueOf(1)));
        Assert.assertEquals("Integer 1 count", 2, this.f.getCount(Long.valueOf(1)));
        Assert.assertEquals("Integer 1 cumPct", 0.5, this.f.getCumPct(1), this.tolerance);
        Assert.assertEquals("Integer 1 cumPct", 0.5, this.f.getCumPct(Long.valueOf(1)), this.tolerance);
        Assert.assertEquals("Integer 1 cumPct", 0.5, this.f.getCumPct(Integer.valueOf(1)), this.tolerance);
        final Iterator<?> it = this.f.valuesIterator();
        while (it.hasNext()) {
            Assert.assertTrue(it.next() instanceof Long);
        }
    }

    @Test
    public void testSerial() {
        this.f.addValue(this.oneL);
        this.f.addValue(this.twoL);
        this.f.addValue(this.oneI);
        this.f.addValue(this.twoI);
        Assert.assertEquals(this.f, TestUtils.serializeAndRecover(this.f));
    }

    @Test
    public void testGetUniqueCount() {
        Assert.assertEquals(0, this.f.getUniqueCount());
        this.f.addValue(this.oneL);
        Assert.assertEquals(1, this.f.getUniqueCount());
        this.f.addValue(this.oneL);
        Assert.assertEquals(1, this.f.getUniqueCount());
        this.f.addValue(this.twoI);
        Assert.assertEquals(2, this.f.getUniqueCount());
    }

    @Test
    public void testIncrement() {
        Assert.assertEquals(0, this.f.getUniqueCount());
        this.f.incrementValue(this.oneL, 1);
        Assert.assertEquals(1, this.f.getCount(this.oneL));

        this.f.incrementValue(this.oneL, 4);
        Assert.assertEquals(5, this.f.getCount(this.oneL));

        this.f.incrementValue(this.oneL, -5);
        Assert.assertEquals(0, this.f.getCount(this.oneL));
    }

    @Test
    public void testMerge() {
        Assert.assertEquals(0, this.f.getUniqueCount());
        this.f.addValue(this.oneL);
        this.f.addValue(this.twoL);
        this.f.addValue(this.oneI);
        this.f.addValue(this.twoI);

        Assert.assertEquals(2, this.f.getUniqueCount());
        Assert.assertEquals(2, this.f.getCount(this.oneI));
        Assert.assertEquals(2, this.f.getCount(this.twoI));

        final Frequency g = new Frequency();
        g.addValue(this.oneL);
        g.addValue(this.threeL);
        g.addValue(this.threeI);

        Assert.assertEquals(2, g.getUniqueCount());
        Assert.assertEquals(1, g.getCount(this.oneI));
        Assert.assertEquals(2, g.getCount(this.threeI));

        this.f.merge(g);

        Assert.assertEquals(3, this.f.getUniqueCount());
        Assert.assertEquals(3, this.f.getCount(this.oneI));
        Assert.assertEquals(2, this.f.getCount(this.twoI));
        Assert.assertEquals(2, this.f.getCount(this.threeI));
    }

    @Test
    public void testMergeCollection() {
        Assert.assertEquals(0, this.f.getUniqueCount());
        this.f.addValue(this.oneL);

        Assert.assertEquals(1, this.f.getUniqueCount());
        Assert.assertEquals(1, this.f.getCount(this.oneI));
        Assert.assertEquals(0, this.f.getCount(this.twoI));

        final Frequency g = new Frequency();
        g.addValue(this.twoL);

        final Frequency h = new Frequency();
        h.addValue(this.threeL);

        final List<Frequency> coll = new ArrayList<Frequency>();
        coll.add(g);
        coll.add(h);
        this.f.merge(coll);

        Assert.assertEquals(3, this.f.getUniqueCount());
        Assert.assertEquals(1, this.f.getCount(this.oneI));
        Assert.assertEquals(1, this.f.getCount(this.twoI));
        Assert.assertEquals(1, this.f.getCount(this.threeI));
    }
}
