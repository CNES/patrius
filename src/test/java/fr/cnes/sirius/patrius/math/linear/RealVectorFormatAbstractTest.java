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
package fr.cnes.sirius.patrius.math.linear;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathParseException;

public abstract class RealVectorFormatAbstractTest {

    RealVectorFormat realVectorFormat = null;
    RealVectorFormat realVectorFormatSquare = null;

    protected abstract Locale getLocale();

    protected abstract char getDecimalCharacter();

    public RealVectorFormatAbstractTest() {
        this.realVectorFormat = RealVectorFormat.getInstance(this.getLocale());
        final NumberFormat nf = NumberFormat.getInstance(this.getLocale());
        nf.setMaximumFractionDigits(2);
        this.realVectorFormatSquare = new RealVectorFormat("[", "]", " : ", nf);
    }

    @Test
    public void testSimpleNoDecimals() {
        final ArrayRealVector c = new ArrayRealVector(new double[] { 1, 1, 1 });
        final String expected = "{1; 1; 1}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimals() {
        final ArrayRealVector c = new ArrayRealVector(new double[] { 1.23, 1.43, 1.63 });
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "23; 1" + this.getDecimalCharacter() +
                "43; 1" + this.getDecimalCharacter() +
                "63}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimalsTrunc() {
        final ArrayRealVector c =
            new ArrayRealVector(new double[] { 1.232323232323, 1.43434343434343, 1.633333333333 });
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "2323232323; 1" + this.getDecimalCharacter() +
                "4343434343; 1" + this.getDecimalCharacter() +
                "6333333333}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeX() {
        final ArrayRealVector c = new ArrayRealVector(new double[] { -1.232323232323, 1.43, 1.63 });
        final String expected =
            "{-1" + this.getDecimalCharacter() +
                "2323232323; 1" + this.getDecimalCharacter() +
                "43; 1" + this.getDecimalCharacter() +
                "63}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeY() {
        final ArrayRealVector c = new ArrayRealVector(new double[] { 1.23, -1.434343434343, 1.63 });
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "23; -1" + this.getDecimalCharacter() +
                "4343434343; 1" + this.getDecimalCharacter() +
                "63}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeZ() {
        final ArrayRealVector c = new ArrayRealVector(new double[] { 1.23, 1.43, -1.633333333333 });
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "23; 1" + this.getDecimalCharacter() +
                "43; -1" + this.getDecimalCharacter() +
                "6333333333}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNonDefaultSetting() {
        final ArrayRealVector c = new ArrayRealVector(new double[] { 1, 1, 1 });
        final String expected = "[1 : 1 : 1]";
        final String actual = this.realVectorFormatSquare.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDefaultFormatRealVectorImpl() {
        final Locale defaultLocal = Locale.getDefault();
        Locale.setDefault(this.getLocale());

        final ArrayRealVector c =
            new ArrayRealVector(new double[] { 232.22222222222, -342.3333333333, 432.44444444444 });
        final String expected =
            "{232" + this.getDecimalCharacter() +
                "2222222222; -342" + this.getDecimalCharacter() +
                "3333333333; 432" + this.getDecimalCharacter() +
                "4444444444}";
        final String actual = (new RealVectorFormat()).format(c);
        Assert.assertEquals(expected, actual);

        Locale.setDefault(defaultLocal);
    }

    @Test
    public void testNan() {
        final ArrayRealVector c = new ArrayRealVector(new double[] { Double.NaN, Double.NaN, Double.NaN });
        final String expected = "{(NaN); (NaN); (NaN)}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPositiveInfinity() {
        final ArrayRealVector c = new ArrayRealVector(new double[] {
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
        });
        final String expected = "{(Infinity); (Infinity); (Infinity)}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void tesNegativeInfinity() {
        final ArrayRealVector c = new ArrayRealVector(new double[] {
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        });
        final String expected = "{(-Infinity); (-Infinity); (-Infinity)}";
        final String actual = this.realVectorFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleNoDecimals() {
        final String source = "{1; 1; 1}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 1, 1, 1 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseIgnoredWhitespace() {
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 1, 1, 1 });
        final ParsePosition pos1 = new ParsePosition(0);
        final String source1 = "{1;1;1}";
        Assert.assertEquals(expected, this.realVectorFormat.parse(source1, pos1));
        Assert.assertEquals(source1.length(), pos1.getIndex());
        final ParsePosition pos2 = new ParsePosition(0);
        final String source2 = " { 1 ; 1 ; 1 } ";
        Assert.assertEquals(expected, this.realVectorFormat.parse(source2, pos2));
        Assert.assertEquals(source2.length() - 1, pos2.getIndex());
    }

    @Test
    public void testParseSimpleWithDecimals() {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "23; 1" + this.getDecimalCharacter() +
                "43; 1" + this.getDecimalCharacter() +
                "63}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 1.23, 1.43, 1.63 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleWithDecimalsTrunc() {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "2323; 1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 1.2323, 1.4343, 1.6333 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeX() {
        final String source =
            "{-1" + this.getDecimalCharacter() +
                "2323; 1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { -1.2323, 1.4343, 1.6333 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeY() {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "2323; -1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 1.2323, -1.4343, 1.6333 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeZ() {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "2323; 1" + this.getDecimalCharacter() +
                "4343; -1" + this.getDecimalCharacter() +
                "6333}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 1.2323, 1.4343, -1.6333 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeAll() {
        final String source =
            "{-1" + this.getDecimalCharacter() +
                "2323; -1" + this.getDecimalCharacter() +
                "4343; -1" + this.getDecimalCharacter() +
                "6333}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { -1.2323, -1.4343, -1.6333 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseZeroX() {
        final String source =
            "{0" + this.getDecimalCharacter() +
                "0; -1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 0.0, -1.4343, 1.6333 });
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNonDefaultSetting() {
        final String source =
            "[1" + this.getDecimalCharacter() +
                "2323 : 1" + this.getDecimalCharacter() +
                "4343 : 1" + this.getDecimalCharacter() +
                "6333]";
        final ArrayRealVector expected = new ArrayRealVector(new double[] { 1.2323, 1.4343, 1.6333 });
        final ArrayRealVector actual = this.realVectorFormatSquare.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNan() {
        final String source = "{(NaN); (NaN); (NaN)}";
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(new ArrayRealVector(new double[] { Double.NaN, Double.NaN, Double.NaN }), actual);
    }

    @Test
    public void testParsePositiveInfinity() {
        final String source = "{(Infinity); (Infinity); (Infinity)}";
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(new ArrayRealVector(new double[] {
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
        }), actual);
    }

    @Test
    public void testParseNegativeInfinity() {
        final String source = "{(-Infinity); (-Infinity); (-Infinity)}";
        final ArrayRealVector actual = this.realVectorFormat.parse(source);
        Assert.assertEquals(new ArrayRealVector(new double[] {
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        }), actual);
    }

    @Test
    public void testParseNoComponents() {
        try {
            this.realVectorFormat.parse("{ }");
            Assert.fail("Expecting MathParseException");
        } catch (final MathParseException pe) {
            // expected behavior
        }
    }

    @Test
    public void testParseManyComponents() {
        final ArrayRealVector parsed = this.realVectorFormat.parse("{0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0}");
        Assert.assertEquals(24, parsed.getDimension());
    }

    @Test
    public void testConstructorSingleFormat() {
        final NumberFormat nf = NumberFormat.getInstance();
        final RealVectorFormat cf = new RealVectorFormat(nf);
        Assert.assertNotNull(cf);
        Assert.assertEquals(nf, cf.getFormat());
    }

    @Test
    public void testForgottenPrefix() {
        final ParsePosition pos = new ParsePosition(0);
        final String source = "1; 1; 1}";
        Assert.assertNull("Should not parse <" + source + ">", new RealVectorFormat().parse(source, pos));
        Assert.assertEquals(0, pos.getErrorIndex());
    }

    @Test
    public void testForgottenSeparator() {
        final ParsePosition pos = new ParsePosition(0);
        final String source = "{1; 1 1}";
        Assert.assertNull("Should not parse <" + source + ">", new RealVectorFormat().parse(source, pos));
        Assert.assertEquals(6, pos.getErrorIndex());
    }

    @Test
    public void testForgottenSuffix() {
        final ParsePosition pos = new ParsePosition(0);
        final String source = "{1; 1; 1 ";
        Assert.assertNull("Should not parse <" + source + ">", new RealVectorFormat().parse(source, pos));
        Assert.assertEquals(8, pos.getErrorIndex());
    }
}
