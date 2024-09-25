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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fraction;

import java.text.NumberFormat;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.math.util.FastMath;

public class FractionFormatTest {

    FractionFormat properFormat = null;
    FractionFormat improperFormat = null;

    protected Locale getLocale() {
        return Locale.getDefault();
    }

    @Before
    public void setUp() {
        this.properFormat = FractionFormat.getProperInstance(this.getLocale());
        this.improperFormat = FractionFormat.getImproperInstance(this.getLocale());
    }

    @Test
    public void testFormat() {
        final Fraction c = new Fraction(1, 2);
        final String expected = "1 / 2";

        String actual = this.properFormat.format(c);
        Assert.assertEquals(expected, actual);

        actual = this.improperFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFormatNegative() {
        final Fraction c = new Fraction(-1, 2);
        final String expected = "-1 / 2";

        String actual = this.properFormat.format(c);
        Assert.assertEquals(expected, actual);

        actual = this.improperFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFormatZero() {
        final Fraction c = new Fraction(0, 1);
        final String expected = "0 / 1";

        String actual = this.properFormat.format(c);
        Assert.assertEquals(expected, actual);

        actual = this.improperFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFormatImproper() {
        final Fraction c = new Fraction(5, 3);

        String actual = this.properFormat.format(c);
        Assert.assertEquals("1 2 / 3", actual);

        actual = this.improperFormat.format(c);
        Assert.assertEquals("5 / 3", actual);
    }

    @Test
    public void testFormatImproperNegative() {
        final Fraction c = new Fraction(-5, 3);

        String actual = this.properFormat.format(c);
        Assert.assertEquals("-1 2 / 3", actual);

        actual = this.improperFormat.format(c);
        Assert.assertEquals("-5 / 3", actual);
    }

    @Test
    public void testParse() {
        final String source = "1 / 2";

        try {
            Fraction c = this.properFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(1, c.getNumerator());
            Assert.assertEquals(2, c.getDenominator());

            c = this.improperFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(1, c.getNumerator());
            Assert.assertEquals(2, c.getDenominator());
        } catch (final MathParseException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void testParseInteger() {
        final String source = "10";
        {
            final Fraction c = this.properFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(10, c.getNumerator());
            Assert.assertEquals(1, c.getDenominator());
        }
        {
            final Fraction c = this.improperFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(10, c.getNumerator());
            Assert.assertEquals(1, c.getDenominator());
        }
    }

    @Test
    public void testParseOne1() {
        final String source = "1 / 1";
        final Fraction c = this.properFormat.parse(source);
        Assert.assertNotNull(c);
        Assert.assertEquals(1, c.getNumerator());
        Assert.assertEquals(1, c.getDenominator());
    }

    @Test
    public void testParseOne2() {
        final String source = "10 / 10";
        final Fraction c = this.properFormat.parse(source);
        Assert.assertNotNull(c);
        Assert.assertEquals(1, c.getNumerator());
        Assert.assertEquals(1, c.getDenominator());
    }

    @Test
    public void testParseZero1() {
        final String source = "0 / 1";
        final Fraction c = this.properFormat.parse(source);
        Assert.assertNotNull(c);
        Assert.assertEquals(0, c.getNumerator());
        Assert.assertEquals(1, c.getDenominator());
    }

    @Test
    public void testParseZero2() {
        final String source = "-0 / 1";
        final Fraction c = this.properFormat.parse(source);
        Assert.assertNotNull(c);
        Assert.assertEquals(0, c.getNumerator());
        Assert.assertEquals(1, c.getDenominator());
        // This test shows that the sign is not preserved.
        Assert.assertEquals(Double.POSITIVE_INFINITY, 1d / c.doubleValue(), 0);
    }

    @Test
    public void testParseInvalid() {
        final String source = "a";
        final String msg = "should not be able to parse '10 / a'.";
        try {
            this.properFormat.parse(source);
            Assert.fail(msg);
        } catch (final MathParseException ex) {
            // success
        }
        try {
            this.improperFormat.parse(source);
            Assert.fail(msg);
        } catch (final MathParseException ex) {
            // success
        }
    }

    @Test
    public void testParseInvalidDenominator() {
        final String source = "10 / a";
        final String msg = "should not be able to parse '10 / a'.";
        try {
            this.properFormat.parse(source);
            Assert.fail(msg);
        } catch (final MathParseException ex) {
            // success
        }
        try {
            this.improperFormat.parse(source);
            Assert.fail(msg);
        } catch (final MathParseException ex) {
            // success
        }
    }

    @Test
    public void testParseNegative() {

        {
            String source = "-1 / 2";
            Fraction c = this.properFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(-1, c.getNumerator());
            Assert.assertEquals(2, c.getDenominator());

            c = this.improperFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(-1, c.getNumerator());
            Assert.assertEquals(2, c.getDenominator());

            source = "1 / -2";
            c = this.properFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(-1, c.getNumerator());
            Assert.assertEquals(2, c.getDenominator());

            c = this.improperFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(-1, c.getNumerator());
            Assert.assertEquals(2, c.getDenominator());
        }
    }

    @Test
    public void testParseProper() {
        final String source = "1 2 / 3";

        {
            final Fraction c = this.properFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(5, c.getNumerator());
            Assert.assertEquals(3, c.getDenominator());
        }

        try {
            this.improperFormat.parse(source);
            Assert.fail("invalid improper fraction.");
        } catch (final MathParseException ex) {
            // success
        }
    }

    @Test
    public void testParseProperNegative() {
        final String source = "-1 2 / 3";
        {
            final Fraction c = this.properFormat.parse(source);
            Assert.assertNotNull(c);
            Assert.assertEquals(-5, c.getNumerator());
            Assert.assertEquals(3, c.getDenominator());
        }

        try {
            this.improperFormat.parse(source);
            Assert.fail("invalid improper fraction.");
        } catch (final MathParseException ex) {
            // success
        }
    }

    @Test
    public void testParseProperInvalidMinus() {
        String source = "2 -2 / 3";
        try {
            this.properFormat.parse(source);
            Assert.fail("invalid minus in improper fraction.");
        } catch (final MathParseException ex) {
            // expected
        }
        source = "2 2 / -3";
        try {
            this.properFormat.parse(source);
            Assert.fail("invalid minus in improper fraction.");
        } catch (final MathParseException ex) {
            // expected
        }
    }

    @Test
    public void testNumeratorFormat() {
        NumberFormat old = this.properFormat.getNumeratorFormat();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setParseIntegerOnly(true);
        this.properFormat.setNumeratorFormat(nf);
        Assert.assertEquals(nf, this.properFormat.getNumeratorFormat());
        this.properFormat.setNumeratorFormat(old);

        old = this.improperFormat.getNumeratorFormat();
        nf = NumberFormat.getInstance();
        nf.setParseIntegerOnly(true);
        this.improperFormat.setNumeratorFormat(nf);
        Assert.assertEquals(nf, this.improperFormat.getNumeratorFormat());
        this.improperFormat.setNumeratorFormat(old);
    }

    @Test
    public void testDenominatorFormat() {
        NumberFormat old = this.properFormat.getDenominatorFormat();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setParseIntegerOnly(true);
        this.properFormat.setDenominatorFormat(nf);
        Assert.assertEquals(nf, this.properFormat.getDenominatorFormat());
        this.properFormat.setDenominatorFormat(old);

        old = this.improperFormat.getDenominatorFormat();
        nf = NumberFormat.getInstance();
        nf.setParseIntegerOnly(true);
        this.improperFormat.setDenominatorFormat(nf);
        Assert.assertEquals(nf, this.improperFormat.getDenominatorFormat());
        this.improperFormat.setDenominatorFormat(old);
    }

    @Test
    public void testWholeFormat() {
        final ProperFractionFormat format = (ProperFractionFormat) this.properFormat;

        final NumberFormat old = format.getWholeFormat();
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setParseIntegerOnly(true);
        format.setWholeFormat(nf);
        Assert.assertEquals(nf, format.getWholeFormat());
        format.setWholeFormat(old);
    }

    @Test
    public void testLongFormat() {
        Assert.assertEquals("10 / 1", this.improperFormat.format(10l));
    }

    @Test
    public void testDoubleFormat() {
        Assert.assertEquals("355 / 113", this.improperFormat.format(FastMath.PI));
    }
}
