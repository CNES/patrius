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
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
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

public abstract class RealMatrixFormatAbstractTest {

    RealMatrixFormat realMatrixFormat = null;
    RealMatrixFormat realMatrixFormatOctave = null;

    protected abstract Locale getLocale();

    protected abstract char getDecimalCharacter();

    public RealMatrixFormatAbstractTest() {
        this.realMatrixFormat = RealMatrixFormat.getInstance(this.getLocale());
        final NumberFormat nf = NumberFormat.getInstance(this.getLocale());
        nf.setMaximumFractionDigits(2);
        this.realMatrixFormatOctave = new RealMatrixFormat("[", "]", "", "", "; ", ", ", nf);
    }

    @Test
    public void testSimpleNoDecimals() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { 1, 1, 1 }, { 1, 1, 1 } });
        final String expected = "{{1,1,1},{1,1,1}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimals() {
        final RealMatrix m =
            MatrixUtils.createRealMatrix(new double[][] { { 1.23, 1.43, 1.63 }, { 2.46, 2.46, 2.66 } });
        final String expected =
            "{{1" + this.getDecimalCharacter() +
                "23,1" + this.getDecimalCharacter() +
                "43,1" + this.getDecimalCharacter() +
                "63},{2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "66}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimalsTrunc() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { 1.232323232323, 1.43, 1.63 },
            { 2.46, 2.46, 2.666666666666 } });
        final String expected =
            "{{1" + this.getDecimalCharacter() +
                "2323232323,1" + this.getDecimalCharacter() +
                "43,1" + this.getDecimalCharacter() +
                "63},{2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "6666666667}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeComponent() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { -1.232323232323, 1.43, 1.63 },
            { 2.46, 2.46, 2.66 } });
        final String expected =
            "{{-1" + this.getDecimalCharacter() +
                "2323232323,1" + this.getDecimalCharacter() +
                "43,1" + this.getDecimalCharacter() +
                "63},{2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "66}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeComponent2() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { 1.23, -1.434343434343, 1.63 },
            { 2.46, 2.46, 2.66 } });
        final String expected =
            "{{1" + this.getDecimalCharacter() +
                "23,-1" + this.getDecimalCharacter() +
                "4343434343,1" + this.getDecimalCharacter() +
                "63},{2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "66}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeSecondRow() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { 1.23, 1.43, 1.63 },
            { -2.66666666666, 2.46, 2.66 } });
        final String expected =
            "{{1" + this.getDecimalCharacter() +
                "23,1" + this.getDecimalCharacter() +
                "43,1" + this.getDecimalCharacter() +
                "63},{-2" + this.getDecimalCharacter() +
                "6666666667,2" + this.getDecimalCharacter() +
                "46,2" + this.getDecimalCharacter() +
                "66}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNonDefaultSetting() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { 1, 1, 1 }, { 1, 1, 1 } });
        final String expected = "[1, 1, 1; 1, 1, 1]";
        final String actual = this.realMatrixFormatOctave.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDefaultFormat() {
        final Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(this.getLocale());

        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { 232.2222222222, -342.33333333333,
            432.44444444444 } });
        final String expected =
            "{{232" + this.getDecimalCharacter() +
                "2222222222,-342" + this.getDecimalCharacter() +
                "3333333333,432" + this.getDecimalCharacter() +
                "4444444444}}";
        final String actual = (new RealMatrixFormat()).format(m);
        Assert.assertEquals(expected, actual);

        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testNan() {
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { Double.NaN, Double.NaN, Double.NaN } });
        final String expected = "{{(NaN),(NaN),(NaN)}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPositiveInfinity() {
        final RealMatrix m = MatrixUtils.createRealMatrix(
            new double[][] { { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY } });
        final String expected = "{{(Infinity),(Infinity),(Infinity)}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void tesNegativeInfinity() {
        final RealMatrix m = MatrixUtils.createRealMatrix(
            new double[][] { { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY } });
        final String expected = "{{(-Infinity),(-Infinity),(-Infinity)}}";
        final String actual = this.realMatrixFormat.format(m);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleNoDecimals() {
        final String source = "{{1, 1, 1}, {1, 1, 1}}";
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { 1, 1, 1 }, { 1, 1, 1 } });
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseIgnoredWhitespace() {
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { 1, 1, 1 }, { 1, 1, 1 } });
        final ParsePosition pos1 = new ParsePosition(0);
        final String source1 = "{{1,1,1},{1,1,1}}";
        Assert.assertEquals(expected, this.realMatrixFormat.parse(source1, pos1));
        Assert.assertEquals(source1.length(), pos1.getIndex());
        final ParsePosition pos2 = new ParsePosition(0);
        final String source2 = " { { 1 , 1 , 1 } , { 1 , 1 , 1 } } ";
        Assert.assertEquals(expected, this.realMatrixFormat.parse(source2, pos2));
        Assert.assertEquals(source2.length() - 1, pos2.getIndex());
    }

    @Test
    public void testParseSimpleWithDecimals() {
        final String source =
            "{{1" + this.getDecimalCharacter() +
                "23,1" + this.getDecimalCharacter() +
                "43,1" + this.getDecimalCharacter() +
                "63}}";
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { 1.23, 1.43, 1.63 } });
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleWithDecimalsTrunc() {
        final String source =
            "{{1" + this.getDecimalCharacter() +
                "2323,1" + this.getDecimalCharacter() +
                "4343,1" + this.getDecimalCharacter() +
                "6333}}";
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { 1.2323, 1.4343, 1.6333 } });
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeComponent() {
        final String source =
            "{{-1" + this.getDecimalCharacter() +
                "2323,1" + this.getDecimalCharacter() +
                "4343,1" + this.getDecimalCharacter() +
                "6333}}";
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { -1.2323, 1.4343, 1.6333 } });
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeAll() {
        final String source =
            "{{-1" + this.getDecimalCharacter() +
                "2323,-1" + this.getDecimalCharacter() +
                "4343,-1" + this.getDecimalCharacter() +
                "6333}}";
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { -1.2323, -1.4343, -1.6333 } });
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseZeroComponent() {
        final String source =
            "{{0" + this.getDecimalCharacter() +
                "0,-1" + this.getDecimalCharacter() +
                "4343,1" + this.getDecimalCharacter() +
                "6333}}";
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { 0.0, -1.4343, 1.6333 } });
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNonDefaultSetting() {
        final String source =
            "[1" + this.getDecimalCharacter() +
                "2323, 1" + this.getDecimalCharacter() +
                "4343, 1" + this.getDecimalCharacter() +
                "6333]";
        final RealMatrix expected = MatrixUtils.createRealMatrix(new double[][] { { 1.2323, 1.4343, 1.6333 } });
        final RealMatrix actual = this.realMatrixFormatOctave.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNan() {
        final String source = "{{(NaN), (NaN), (NaN)}}";
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        final RealMatrix expected =
            MatrixUtils.createRealMatrix(new double[][] { { Double.NaN, Double.NaN, Double.NaN } });
        for (int i = 0; i < expected.getRowDimension(); i++) {
            for (int j = 0; j < expected.getColumnDimension(); j++) {
                Assert.assertTrue(Double.isNaN(actual.getEntry(i, j)));
            }
        }
    }

    @Test
    public void testParsePositiveInfinity() {
        final String source = "{{(Infinity), (Infinity), (Infinity)}}";
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        final RealMatrix expected = MatrixUtils.createRealMatrix(
            new double[][] { { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY } });
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeInfinity() {
        final String source = "{{(-Infinity), (-Infinity), (-Infinity)}}";
        final RealMatrix actual = this.realMatrixFormat.parse(source);
        final RealMatrix expected = MatrixUtils.createRealMatrix(
            new double[][] { { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY } });
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNoComponents() {
        try {
            this.realMatrixFormat.parse("{{ }}");
            Assert.fail("Expecting MathParseException");
        } catch (final MathParseException pe) {
            // expected behavior
        }
    }

    @Test
    public void testParseManyComponents() {
        final RealMatrix parsed = this.realMatrixFormat.parse("{{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}}");
        Assert.assertEquals(24, parsed.getColumnDimension());
    }

    @Test
    public void testConstructorSingleFormat() {
        final NumberFormat nf = NumberFormat.getInstance();
        final RealMatrixFormat mf = new RealMatrixFormat(nf);
        Assert.assertNotNull(mf);
        Assert.assertEquals(nf, mf.getFormat());
    }

    @Test
    public void testForgottenPrefix() {
        final ParsePosition pos = new ParsePosition(0);
        final String source = "1; 1; 1]";
        Assert.assertNull("Should not parse <" + source + ">", this.realMatrixFormat.parse(source, pos));
        Assert.assertEquals(0, pos.getErrorIndex());
    }

    @Test
    public void testForgottenSeparator() {
        final ParsePosition pos = new ParsePosition(0);
        final String source = "{{1, 1 1}}";
        Assert.assertNull("Should not parse <" + source + ">", this.realMatrixFormat.parse(source, pos));
        Assert.assertEquals(7, pos.getErrorIndex());
    }

    @Test
    public void testForgottenSuffix() {
        final ParsePosition pos = new ParsePosition(0);
        final String source = "{{1, 1, 1 ";
        Assert.assertNull("Should not parse <" + source + ">", this.realMatrixFormat.parse(source, pos));
        Assert.assertEquals(9, pos.getErrorIndex());
    }

    @Test
    public void testGetters() {
    	// Initialization
        final RealMatrixFormat formatter =  new RealMatrixFormat("[", "]", "", "", "; ", ", ");
        
        // Check getters
        Assert.assertEquals("[", formatter.getPrefix());
        Assert.assertEquals("]", formatter.getSuffix());
        Assert.assertEquals("", formatter.getRowPrefix());
        Assert.assertEquals("", formatter.getRowSuffix());
        Assert.assertEquals("; ", formatter.getRowSeparator());
        Assert.assertEquals(", ", formatter.getColumnSeparator());
        Assert.assertNotNull(RealMatrixFormat.getInstance());
        Assert.assertEquals(160, RealMatrixFormat.getAvailableLocales().length);
    }

    @Test
    public void testSummaryMode() {
    	// Initialization
        final RealMatrixFormat formatter =  new RealMatrixFormat("[", "]", "", "", "; ", ", ", "", 1);
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] { { 1, 1, 1 }, { 1, 1, 1 }, { 1, 1, 1 } });
        // Check
        final String expected = "[, ..., ; ; , ..., ]\nRows number : 3	 Columns number : 3";
        final String actual = formatter.format(m);
        Assert.assertEquals(expected, actual);
    }
}
