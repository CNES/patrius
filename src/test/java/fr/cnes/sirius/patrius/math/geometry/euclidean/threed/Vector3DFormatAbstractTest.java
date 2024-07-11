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
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathParseException;
import fr.cnes.sirius.patrius.math.geometry.VectorFormat;

public abstract class Vector3DFormatAbstractTest {

    Vector3DFormat vector3DFormat = null;
    Vector3DFormat vector3DFormatSquare = null;

    protected abstract Locale getLocale();

    protected abstract char getDecimalCharacter();

    protected Vector3DFormatAbstractTest() {
        this.vector3DFormat = Vector3DFormat.getInstance(this.getLocale());
        final NumberFormat nf = NumberFormat.getInstance(this.getLocale());
        nf.setMaximumFractionDigits(2);
        this.vector3DFormatSquare = new Vector3DFormat("[", "]", " : ", nf);
    }

    @Test
    public void testSimpleNoDecimals() {
        final Vector3D c = new Vector3D(1, 1, 1);
        final String expected = "{1; 1; 1}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimals() {
        final Vector3D c = new Vector3D(1.23, 1.43, 1.63);
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "23; 1" + this.getDecimalCharacter() +
                "43; 1" + this.getDecimalCharacter() +
                "63}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleWithDecimalsTrunc() {
        final Vector3D c = new Vector3D(1.232323232323, 1.434343434343, 1.633333333333);
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "2323232323; 1" + this.getDecimalCharacter() +
                "4343434343; 1" + this.getDecimalCharacter() +
                "6333333333}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeX() {
        final Vector3D c = new Vector3D(-1.232323232323, 1.43, 1.63);
        final String expected =
            "{-1" + this.getDecimalCharacter() +
                "2323232323; 1" + this.getDecimalCharacter() +
                "43; 1" + this.getDecimalCharacter() +
                "63}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeY() {
        final Vector3D c = new Vector3D(1.23, -1.434343434343, 1.63);
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "23; -1" + this.getDecimalCharacter() +
                "4343434343; 1" + this.getDecimalCharacter() +
                "63}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNegativeZ() {
        final Vector3D c = new Vector3D(1.23, 1.43, -1.633333333333);
        final String expected =
            "{1" + this.getDecimalCharacter() +
                "23; 1" + this.getDecimalCharacter() +
                "43; -1" + this.getDecimalCharacter() +
                "6333333333}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNonDefaultSetting() {
        final Vector3D c = new Vector3D(1, 1, 1);
        final String expected = "[1 : 1 : 1]";
        final String actual = this.vector3DFormatSquare.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDefaultFormatVector3D() {
        final Locale defaultLocal = Locale.getDefault();
        Locale.setDefault(this.getLocale());

        final Vector3D c = new Vector3D(232.22222222222, -342.3333333333, 432.44444444444);
        final String expected =
            "{232" + this.getDecimalCharacter() +
                "2222222222; -342" + this.getDecimalCharacter() +
                "3333333333; 432" + this.getDecimalCharacter() +
                "4444444444}";
        final String actual = (new Vector3DFormat()).format(c);
        Assert.assertEquals(expected, actual);

        Locale.setDefault(defaultLocal);
    }

    @Test
    public void testNan() {
        final Vector3D c = Vector3D.NaN;
        final String expected = "{(NaN); (NaN); (NaN)}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPositiveInfinity() {
        final Vector3D c = Vector3D.POSITIVE_INFINITY;
        final String expected = "{(Infinity); (Infinity); (Infinity)}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void tesNegativeInfinity() {
        final Vector3D c = Vector3D.NEGATIVE_INFINITY;
        final String expected = "{(-Infinity); (-Infinity); (-Infinity)}";
        final String actual = this.vector3DFormat.format(c);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleNoDecimals() throws MathParseException {
        final String source = "{1; 1; 1}";
        final Vector3D expected = new Vector3D(1, 1, 1);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseIgnoredWhitespace() {
        final Vector3D expected = new Vector3D(1, 1, 1);
        final ParsePosition pos1 = new ParsePosition(0);
        final String source1 = "{1;1;1}";
        Assert.assertEquals(expected, this.vector3DFormat.parse(source1, pos1));
        Assert.assertEquals(source1.length(), pos1.getIndex());
        final ParsePosition pos2 = new ParsePosition(0);
        final String source2 = " { 1 ; 1 ; 1 } ";
        Assert.assertEquals(expected, this.vector3DFormat.parse(source2, pos2));
        Assert.assertEquals(source2.length() - 1, pos2.getIndex());
    }

    @Test
    public void testParseSimpleWithDecimals() throws MathParseException {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "23; 1" + this.getDecimalCharacter() +
                "43; 1" + this.getDecimalCharacter() +
                "63}";
        final Vector3D expected = new Vector3D(1.23, 1.43, 1.63);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseSimpleWithDecimalsTrunc() throws MathParseException {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "2323; 1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final Vector3D expected = new Vector3D(1.2323, 1.4343, 1.6333);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeX() throws MathParseException {
        final String source =
            "{-1" + this.getDecimalCharacter() +
                "2323; 1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final Vector3D expected = new Vector3D(-1.2323, 1.4343, 1.6333);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeY() throws MathParseException {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "2323; -1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final Vector3D expected = new Vector3D(1.2323, -1.4343, 1.6333);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeZ() throws MathParseException {
        final String source =
            "{1" + this.getDecimalCharacter() +
                "2323; 1" + this.getDecimalCharacter() +
                "4343; -1" + this.getDecimalCharacter() +
                "6333}";
        final Vector3D expected = new Vector3D(1.2323, 1.4343, -1.6333);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeAll() throws MathParseException {
        final String source =
            "{-1" + this.getDecimalCharacter() +
                "2323; -1" + this.getDecimalCharacter() +
                "4343; -1" + this.getDecimalCharacter() +
                "6333}";
        final Vector3D expected = new Vector3D(-1.2323, -1.4343, -1.6333);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseZeroX() throws MathParseException {
        final String source =
            "{0" + this.getDecimalCharacter() +
                "0; -1" + this.getDecimalCharacter() +
                "4343; 1" + this.getDecimalCharacter() +
                "6333}";
        final Vector3D expected = new Vector3D(0.0, -1.4343, 1.6333);
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNonDefaultSetting() throws MathParseException {
        final String source =
            "[1" + this.getDecimalCharacter() +
                "2323 : 1" + this.getDecimalCharacter() +
                "4343 : 1" + this.getDecimalCharacter() +
                "6333]";
        final Vector3D expected = new Vector3D(1.2323, 1.4343, 1.6333);
        final Vector3D actual = this.vector3DFormatSquare.parse(source);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseNan() throws MathParseException {
        final String source = "{(NaN); (NaN); (NaN)}";
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(Vector3D.NaN, actual);
    }

    @Test
    public void testParsePositiveInfinity() throws MathParseException {
        final String source = "{(Infinity); (Infinity); (Infinity)}";
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(Vector3D.POSITIVE_INFINITY, actual);
    }

    @Test
    public void testParseNegativeInfinity() throws MathParseException {
        final String source = "{(-Infinity); (-Infinity); (-Infinity)}";
        final Vector3D actual = this.vector3DFormat.parse(source);
        Assert.assertEquals(Vector3D.NEGATIVE_INFINITY, actual);
    }

    @Test
    public void testParseInvalid() {
        // Invalid string
        final String source =
            "[1" + this.getDecimalCharacter() +
                "2323 : 1" + this.getDecimalCharacter() +
                "4343 : " + "meuh]";
        try {
            this.vector3DFormatSquare.parse(source);
            Assert.fail("expected MathParseException");
        } catch (final MathParseException ex) {
        }
    }

    @Test
    public void testConstructorSingleFormat() {
        final NumberFormat nf = NumberFormat.getInstance();
        final Vector3DFormat cf = new Vector3DFormat(nf);
        Assert.assertNotNull(cf);
        Assert.assertEquals(nf, cf.getFormat());
    }

    @Test
    public void testForgottenPrefix() {
        final ParsePosition pos = new ParsePosition(0);
        Assert.assertNull(new Vector3DFormat().parse("1; 1; 1}", pos));
        Assert.assertEquals(0, pos.getErrorIndex());
    }

    @Test
    public void testForgottenSeparator() {
        final ParsePosition pos = new ParsePosition(0);
        Assert.assertNull(new Vector3DFormat().parse("{1; 1 1}", pos));
        Assert.assertEquals(6, pos.getErrorIndex());
    }

    @Test
    public void testForgottenSuffix() {
        final ParsePosition pos = new ParsePosition(0);
        Assert.assertNull(new Vector3DFormat().parse("{1; 1; 1 ", pos));
        Assert.assertEquals(8, pos.getErrorIndex());
    }

    @Test
    public void testCoverage() {
        // Several tests for code coverage
        final Locale[] nfLocales = NumberFormat.getAvailableLocales();
        final Locale[] v3dLocales = VectorFormat.getAvailableLocales();
        Assert.assertArrayEquals(nfLocales, v3dLocales);

        final String pre = "GA";
        final String suf = "BU";
        final String sep = "ZO";
        final Vector3DFormat bogusFormat = new Vector3DFormat(pre, suf, sep);
        Assert.assertEquals(pre, bogusFormat.getPrefix());
        Assert.assertEquals(suf, bogusFormat.getSuffix());
        Assert.assertEquals(sep, bogusFormat.getSeparator());
    }

}
