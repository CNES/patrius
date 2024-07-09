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
package fr.cnes.sirius.patrius.math.complex;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.FastMath;

public abstract class ComplexFormatAbstractTest {

    ComplexFormat complexFormat = null;
    ComplexFormat complexFormatJ = null;

    protected abstract Locale getLocale();

    protected abstract char getDecimalCharacter();

    protected ComplexFormatAbstractTest() {
        this.complexFormat = ComplexFormat.getInstance(this.getLocale());
        this.complexFormatJ = ComplexFormat.getInstance("j", this.getLocale());
    }

    @Test
    public void testSimpleNoDecimals() {
        final Complex c = new Complex(1, 2);
        final String expected = "1 + 2i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTrimOneImaginary() {
        final ComplexFormat fmt = ComplexFormat.getInstance(this.getLocale());
        fmt.getImaginaryFormat().setMaximumFractionDigits(1);

        Complex c = new Complex(1, 1.04);
        String expected = "1 + i";
        String actual = fmt.format(c);
        Assert.assertEquals(expected, actual);

        c = new Complex(1, 1.09);
        expected = "1 + 1" + this.getDecimalCharacter() + "1i";
        actual = fmt.format(c);
        Assert.assertEquals(expected, actual);

        c = new Complex(1, -1.09);
        expected = "1 - 1" + this.getDecimalCharacter() + "1i";
        actual = fmt.format(c);
        Assert.assertEquals(expected, actual);

        c = new Complex(1, -1.04);
        expected = "1 - i";
        actual = fmt.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimals() {
        final Complex c = new Complex(1.23, 1.43);
        final String expected = "1" + this.getDecimalCharacter() + "23 + 1" + this.getDecimalCharacter() + "43i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimalsTrunc() {
        final Complex c = new Complex(1.232323232323, 1.434343434343);
        final String expected =
            "1" + this.getDecimalCharacter() + "2323232323 + 1" + this.getDecimalCharacter() + "4343434343i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeReal() {
        final Complex c = new Complex(-1.232323232323, 1.43);
        final String expected =
            "-1" + this.getDecimalCharacter() + "2323232323 + 1" + this.getDecimalCharacter() + "43i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeImaginary() {
        final Complex c = new Complex(1.23, -1.434343434343);
        final String expected =
            "1" + this.getDecimalCharacter() + "23 - 1" + this.getDecimalCharacter() + "4343434343i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeBoth() {
        final Complex c = new Complex(-1.232323232323, -1.434343434343);
        final String expected =
            "-1" + this.getDecimalCharacter() + "2323232323 - 1" + this.getDecimalCharacter() + "4343434343i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testZeroReal() {
        final Complex c = new Complex(0.0, -1.434343434343);
        final String expected = "0 - 1" + this.getDecimalCharacter() + "4343434343i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testZeroImaginary() {
        final Complex c = new Complex(30.23333333333, 0);
        final String expected = "30" + this.getDecimalCharacter() + "2333333333";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDifferentImaginaryChar() {
        final Complex c = new Complex(1, 1);
        final String expected = "1 + j";
        final String actual = this.complexFormatJ.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDefaultFormatComplex() {
        final Locale defaultLocal = Locale.getDefault();
        Locale.setDefault(this.getLocale());

        final Complex c = new Complex(232.22222222222, -342.3333333333);
        final String expected =
            "232" + this.getDecimalCharacter() + "2222222222 - 342" + this.getDecimalCharacter() + "3333333333i";
        final String actual = (new ComplexFormat()).format(c);
        Assert.assertEquals(expected, actual);

        Locale.setDefault(defaultLocal);
    }

    @Test
    public void testNan() {
        final Complex c = new Complex(Double.NaN, Double.NaN);
        final String expected = "(NaN) + (NaN)i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPositiveInfinity() {
        final Complex c = new Complex(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        final String expected = "(Infinity) + (Infinity)i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeInfinity() {
        final Complex c = new Complex(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        final String expected = "(-Infinity) - (Infinity)i";
        final String actual = this.complexFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleNoDecimals() {
        final String source = "1 + 1i";
        final Complex expected = new Complex(1, 1);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleWithDecimals() {
        final String source = "1" + this.getDecimalCharacter() + "23 + 1" + this.getDecimalCharacter() + "43i";
        final Complex expected = new Complex(1.23, 1.43);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleWithDecimalsTrunc() {
        final String source =
            "1" + this.getDecimalCharacter() + "232323232323 + 1" + this.getDecimalCharacter() + "434343434343i";
        final Complex expected = new Complex(1.232323232323, 1.434343434343);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeReal() {
        final String source =
            "-1" + this.getDecimalCharacter() + "232323232323 + 1" + this.getDecimalCharacter() + "4343i";
        final Complex expected = new Complex(-1.232323232323, 1.4343);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeImaginary() {
        final String source =
            "1" + this.getDecimalCharacter() + "2323 - 1" + this.getDecimalCharacter() + "434343434343i";
        final Complex expected = new Complex(1.2323, -1.434343434343);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeBoth() {
        final String source =
            "-1" + this.getDecimalCharacter() + "232323232323 - 1" + this.getDecimalCharacter() + "434343434343i";
        final Complex expected = new Complex(-1.232323232323, -1.434343434343);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseZeroReal() {
        final String source = "0" + this.getDecimalCharacter() + "0 - 1" + this.getDecimalCharacter() + "4343i";
        final Complex expected = new Complex(0.0, -1.4343);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseZeroImaginary() {
        final String source = "-1" + this.getDecimalCharacter() + "2323";
        final Complex expected = new Complex(-1.2323, 0);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseDifferentImaginaryChar() {
        final String source = "-1" + this.getDecimalCharacter() + "2323 - 1" + this.getDecimalCharacter() + "4343j";
        final Complex expected = new Complex(-1.2323, -1.4343);
        final Complex actual = this.complexFormatJ.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNan() {
        final String source = "(NaN) + (NaN)i";
        final Complex expected = new Complex(Double.NaN, Double.NaN);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParsePositiveInfinity() {
        final String source = "(Infinity) + (Infinity)i";
        final Complex expected = new Complex(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPaseNegativeInfinity() {
        final String source = "(-Infinity) - (Infinity)i";
        final Complex expected = new Complex(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        final Complex actual = this.complexFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testConstructorSingleFormat() {
        final NumberFormat nf = NumberFormat.getInstance();
        final ComplexFormat cf = new ComplexFormat(nf);
        Assert.assertNotNull(cf);
        Assert.assertEquals(nf, cf.getRealFormat());
    }

    @Test
    public void testGetImaginaryFormat() {
        final NumberFormat nf = NumberFormat.getInstance();
        final ComplexFormat cf = new ComplexFormat(nf);
        Assert.assertSame(nf, cf.getImaginaryFormat());
    }

    @Test
    public void testGetRealFormat() {
        final NumberFormat nf = NumberFormat.getInstance();
        final ComplexFormat cf = new ComplexFormat(nf);
        Assert.assertSame(nf, cf.getRealFormat());
    }

    @Test
    public void testFormatNumber() {
        final ComplexFormat cf = ComplexFormat.getInstance(this.getLocale());
        final Double pi = Double.valueOf(FastMath.PI);
        final String text = cf.format(pi);
        Assert.assertEquals("3" + this.getDecimalCharacter() + "1415926536", text);
    }

    @Test
    public void testForgottenImaginaryCharacter() {
        final ParsePosition pos = new ParsePosition(0);
        Assert.assertNull(new ComplexFormat().parse("1 + 1", pos));
        Assert.assertEquals(5, pos.getErrorIndex());
    }
}
