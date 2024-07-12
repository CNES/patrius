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
 * VERSION::FA:306:14/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.Serializable;
import java.lang.reflect.Array;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.Field;
import fr.cnes.sirius.patrius.math.FieldElement;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.fraction.Fraction;
import fr.cnes.sirius.patrius.math.fraction.FractionField;

/**
 * Test cases for the {@link ArrayFieldVector} class.
 * 
 * @version $Id: ArrayFieldVectorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ArrayFieldVectorTest {

    //
    protected Fraction[][] ma1 = { { new Fraction(1), new Fraction(2), new Fraction(3) },
            { new Fraction(4), new Fraction(5), new Fraction(6) },
            { new Fraction(7), new Fraction(8), new Fraction(9) } };
    protected Fraction[] vec1 = { new Fraction(1), new Fraction(2), new Fraction(3) };
    protected Fraction[] vec2 = { new Fraction(4), new Fraction(5), new Fraction(6) };
    protected Fraction[] vec3 = { new Fraction(7), new Fraction(8), new Fraction(9) };
    protected Fraction[] vec4 = { new Fraction(1), new Fraction(2), new Fraction(3),
            new Fraction(4), new Fraction(5), new Fraction(6), new Fraction(7), new Fraction(8),
            new Fraction(9) };
    protected Fraction[] vec_null = { new Fraction(0), new Fraction(0), new Fraction(0) };
    protected Fraction[] dvec1 = { new Fraction(1), new Fraction(2), new Fraction(3),
            new Fraction(4), new Fraction(5), new Fraction(6), new Fraction(7), new Fraction(8),
            new Fraction(9) };
    protected Fraction[][] mat1 = { { new Fraction(1), new Fraction(2), new Fraction(3) },
            { new Fraction(4), new Fraction(5), new Fraction(6) },
            { new Fraction(7), new Fraction(8), new Fraction(9) } };

    // Testclass to test the FieldVector<Fraction> interface
    // only with enough content to support the test
    public static class FieldVectorTestImpl<T extends FieldElement<T>> implements FieldVector<T>,
            Serializable {

        private static final long serialVersionUID = 3970959016014158539L;

        private final Field<T> field;

        /** Entries of the vector. */
        protected T[] data;

        /**
         * Build an array of elements.
         * 
         * @param length
         *        size of the array to build
         * @return a new array
         */
        @SuppressWarnings("unchecked")
        // field is of type T
                private
                T[] buildArray(final int length) {
            return (T[]) Array.newInstance(this.field.getRuntimeClass(), length);
        }

        public FieldVectorTestImpl(final T[] d) {
            this.field = d[0].getField();
            this.data = d.clone();
        }

        @Override
        public Field<T> getField() {
            return this.field;
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not supported, unneeded for test purposes");
        }

        @Override
        public FieldVector<T> copy() {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> add(final FieldVector<T> v) {
            throw this.unsupported();
        }

        public FieldVector<T> add(final T[] v) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> subtract(final FieldVector<T> v) {
            throw this.unsupported();
        }

        public FieldVector<T> subtract(final T[] v) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapAdd(final T d) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapAddToSelf(final T d) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapSubtract(final T d) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapSubtractToSelf(final T d) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapMultiply(final T d) {
            final T[] out = this.buildArray(this.data.length);
            for (int i = 0; i < this.data.length; i++) {
                out[i] = this.data[i].multiply(d);
            }
            return new FieldVectorTestImpl<>(out);
        }

        @Override
        public FieldVector<T> mapMultiplyToSelf(final T d) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapDivide(final T d) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapDivideToSelf(final T d) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapInv() {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> mapInvToSelf() {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> ebeMultiply(final FieldVector<T> v) {
            throw this.unsupported();
        }

        public FieldVector<T> ebeMultiply(final T[] v) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> ebeDivide(final FieldVector<T> v) {
            throw this.unsupported();
        }

        public FieldVector<T> ebeDivide(final T[] v) {
            throw this.unsupported();
        }

        public T[] getData() {
            return this.data.clone();
        }

        @Override
        public T dotProduct(final FieldVector<T> v) {
            T dot = this.field.getZero();
            for (int i = 0; i < this.data.length; i++) {
                dot = dot.add(this.data[i].multiply(v.getEntry(i)));
            }
            return dot;
        }

        public T dotProduct(final T[] v) {
            T dot = this.field.getZero();
            for (int i = 0; i < this.data.length; i++) {
                dot = dot.add(this.data[i].multiply(v[i]));
            }
            return dot;
        }

        @Override
        public FieldVector<T> projection(final FieldVector<T> v) {
            throw this.unsupported();
        }

        public FieldVector<T> projection(final T[] v) {
            throw this.unsupported();
        }

        @Override
        public FieldMatrix<T> outerProduct(final FieldVector<T> v) {
            throw this.unsupported();
        }

        public FieldMatrix<T> outerProduct(final T[] v) {
            throw this.unsupported();
        }

        @Override
        public T getEntry(final int index) {
            return this.data[index];
        }

        @Override
        public int getDimension() {
            return this.data.length;
        }

        @Override
        public FieldVector<T> append(final FieldVector<T> v) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> append(final T d) {
            throw this.unsupported();
        }

        public FieldVector<T> append(final T[] a) {
            throw this.unsupported();
        }

        @Override
        public FieldVector<T> getSubVector(final int index, final int n) {
            throw this.unsupported();
        }

        @Override
        public void setEntry(final int index, final T value) {
            throw this.unsupported();
        }

        @Override
        public void setSubVector(final int index, final FieldVector<T> v) {
            throw this.unsupported();
        }

        public void setSubVector(final int index, final T[] v) {
            throw this.unsupported();
        }

        @Override
        public void set(final T value) {
            throw this.unsupported();
        }

        @Override
        public T[] toArray() {
            return this.data;
        }

    }

    @Test
    public void testConstructors() {

        final ArrayFieldVector<Fraction> v0 = new ArrayFieldVector<>(FractionField.getInstance());
        Assert.assertEquals(0, v0.getDimension());

        final ArrayFieldVector<Fraction> v1 = new ArrayFieldVector<>(FractionField.getInstance(), 7);
        Assert.assertEquals(7, v1.getDimension());
        Assert.assertEquals(new Fraction(0), v1.getEntry(6));

        final ArrayFieldVector<Fraction> v2 = new ArrayFieldVector<>(5, new Fraction(123, 100));
        Assert.assertEquals(5, v2.getDimension());
        Assert.assertEquals(new Fraction(123, 100), v2.getEntry(4));

        final ArrayFieldVector<Fraction> v3 = new ArrayFieldVector<>(FractionField.getInstance(),
                this.vec1);
        Assert.assertEquals(3, v3.getDimension());
        Assert.assertEquals(new Fraction(2), v3.getEntry(1));

        final ArrayFieldVector<Fraction> v4 = new ArrayFieldVector<>(FractionField.getInstance(),
                this.vec4, 3, 2);
        Assert.assertEquals(2, v4.getDimension());
        Assert.assertEquals(new Fraction(4), v4.getEntry(0));
        try {
            new ArrayFieldVector<>(this.vec4, 8, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }

        final FieldVector<Fraction> v5_i = new ArrayFieldVector<>(this.dvec1);
        Assert.assertEquals(9, v5_i.getDimension());
        Assert.assertEquals(new Fraction(9), v5_i.getEntry(8));

        final ArrayFieldVector<Fraction> v5 = new ArrayFieldVector<>(this.dvec1);
        Assert.assertEquals(9, v5.getDimension());
        Assert.assertEquals(new Fraction(9), v5.getEntry(8));

        final ArrayFieldVector<Fraction> v6 = new ArrayFieldVector<>(this.dvec1, 3, 2);
        Assert.assertEquals(2, v6.getDimension());
        Assert.assertEquals(new Fraction(4), v6.getEntry(0));
        try {
            new ArrayFieldVector<>(this.dvec1, 8, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }

        final ArrayFieldVector<Fraction> v7 = new ArrayFieldVector<>(v1);
        Assert.assertEquals(7, v7.getDimension());
        Assert.assertEquals(new Fraction(0), v7.getEntry(6));

        final FieldVectorTestImpl<Fraction> v7_i = new FieldVectorTestImpl<>(this.vec1);

        final ArrayFieldVector<Fraction> v7_2 = new ArrayFieldVector<>(v7_i);
        Assert.assertEquals(3, v7_2.getDimension());
        Assert.assertEquals(new Fraction(2), v7_2.getEntry(1));

        final ArrayFieldVector<Fraction> v8 = new ArrayFieldVector<>(v1, true);
        Assert.assertEquals(7, v8.getDimension());
        Assert.assertEquals(new Fraction(0), v8.getEntry(6));
        Assert.assertNotSame("testData not same object ", v1.getDataRef(), v8.getDataRef());

        final ArrayFieldVector<Fraction> v8_2 = new ArrayFieldVector<>(v1, false);
        Assert.assertEquals(7, v8_2.getDimension());
        Assert.assertEquals(new Fraction(0), v8_2.getEntry(6));
        Assert.assertArrayEquals(v1.getDataRef(), v8_2.getDataRef());

        final ArrayFieldVector<Fraction> v9 = new ArrayFieldVector<>(v1, v3);
        Assert.assertEquals(10, v9.getDimension());
        Assert.assertEquals(new Fraction(1), v9.getEntry(7));

    }

    @Test
    public void testDataInOut() {

        final ArrayFieldVector<Fraction> v1 = new ArrayFieldVector<>(this.vec1);
        final ArrayFieldVector<Fraction> v2 = new ArrayFieldVector<>(this.vec2);
        final ArrayFieldVector<Fraction> v4 = new ArrayFieldVector<>(this.vec4);
        final FieldVectorTestImpl<Fraction> v2_t = new FieldVectorTestImpl<>(this.vec2);

        final FieldVector<Fraction> v_append_1 = v1.append(v2);
        Assert.assertEquals(6, v_append_1.getDimension());
        Assert.assertEquals(new Fraction(4), v_append_1.getEntry(3));

        final FieldVector<Fraction> v_append_2 = v1.append(new Fraction(2));
        Assert.assertEquals(4, v_append_2.getDimension());
        Assert.assertEquals(new Fraction(2), v_append_2.getEntry(3));

        final FieldVector<Fraction> v_append_4 = v1.append(v2_t);
        Assert.assertEquals(6, v_append_4.getDimension());
        Assert.assertEquals(new Fraction(4), v_append_4.getEntry(3));

        final FieldVector<Fraction> v_copy = v1.copy();
        Assert.assertEquals(3, v_copy.getDimension());
        Assert.assertNotSame("testData not same object ", v1.getDataRef(), v_copy.toArray());

        final Fraction[] a_frac = v1.toArray();
        Assert.assertEquals(3, a_frac.length);
        Assert.assertNotSame("testData not same object ", v1.getDataRef(), a_frac);

        // ArrayFieldVector<Fraction> vout4 = (ArrayFieldVector<Fraction>) v1.clone();
        // Assert.assertEquals(3, vout4.getDimension());
        // Assert.assertEquals(v1.getDataRef(), vout4.getDataRef());

        final FieldVector<Fraction> vout5 = v4.getSubVector(3, 3);
        Assert.assertEquals(3, vout5.getDimension());
        Assert.assertEquals(new Fraction(5), vout5.getEntry(1));
        try {
            v4.getSubVector(3, 7);
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected behavior
        }

        final ArrayFieldVector<Fraction> v_set1 = (ArrayFieldVector<Fraction>) v1.copy();
        v_set1.setEntry(1, new Fraction(11));
        Assert.assertEquals(new Fraction(11), v_set1.getEntry(1));
        try {
            v_set1.setEntry(3, new Fraction(11));
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected behavior
        }

        final ArrayFieldVector<Fraction> v_set2 = (ArrayFieldVector<Fraction>) v4.copy();
        v_set2.set(3, v1);
        Assert.assertEquals(new Fraction(1), v_set2.getEntry(3));
        Assert.assertEquals(new Fraction(7), v_set2.getEntry(6));
        try {
            v_set2.set(7, v1);
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected behavior
        }

        final ArrayFieldVector<Fraction> v_set3 = (ArrayFieldVector<Fraction>) v1.copy();
        v_set3.set(new Fraction(13));
        Assert.assertEquals(new Fraction(13), v_set3.getEntry(2));

        try {
            v_set3.getEntry(23);
            Assert.fail("ArrayIndexOutOfBoundsException expected");
        } catch (final ArrayIndexOutOfBoundsException ex) {
            // expected behavior
        }

        final ArrayFieldVector<Fraction> v_set4 = (ArrayFieldVector<Fraction>) v4.copy();
        v_set4.setSubVector(3, v2_t);
        Assert.assertEquals(new Fraction(4), v_set4.getEntry(3));
        Assert.assertEquals(new Fraction(7), v_set4.getEntry(6));
        try {
            v_set4.setSubVector(7, v2_t);
            Assert.fail("OutOfRangeException expected");
        } catch (final OutOfRangeException ex) {
            // expected behavior
        }

        final ArrayFieldVector<Fraction> vout10 = (ArrayFieldVector<Fraction>) v1.copy();
        final ArrayFieldVector<Fraction> vout10_2 = (ArrayFieldVector<Fraction>) v1.copy();
        Assert.assertEquals(vout10, vout10_2);
        vout10_2.setEntry(0, new Fraction(11, 10));
        Assert.assertNotSame(vout10, vout10_2);

    }

    @Test
    public void testMapFunctions() {
        final ArrayFieldVector<Fraction> v1 = new ArrayFieldVector<>(this.vec1);

        // octave = v1 .+ 2.0
        final FieldVector<Fraction> v_mapAdd = v1.mapAdd(new Fraction(2));
        final Fraction[] result_mapAdd = { new Fraction(3), new Fraction(4), new Fraction(5) };
        this.checkArray("compare vectors", result_mapAdd, v_mapAdd.toArray());

        // octave = v1 .+ 2.0
        final FieldVector<Fraction> v_mapAddToSelf = v1.copy();
        v_mapAddToSelf.mapAddToSelf(new Fraction(2));
        final Fraction[] result_mapAddToSelf = { new Fraction(3), new Fraction(4), new Fraction(5) };
        this.checkArray("compare vectors", result_mapAddToSelf, v_mapAddToSelf.toArray());

        // octave = v1 .- 2.0
        final FieldVector<Fraction> v_mapSubtract = v1.mapSubtract(new Fraction(2));
        final Fraction[] result_mapSubtract = { new Fraction(-1), new Fraction(0), new Fraction(1) };
        this.checkArray("compare vectors", result_mapSubtract, v_mapSubtract.toArray());

        // octave = v1 .- 2.0
        final FieldVector<Fraction> v_mapSubtractToSelf = v1.copy();
        v_mapSubtractToSelf.mapSubtractToSelf(new Fraction(2));
        final Fraction[] result_mapSubtractToSelf = { new Fraction(-1), new Fraction(0),
                new Fraction(1) };
        this.checkArray("compare vectors", result_mapSubtractToSelf, v_mapSubtractToSelf.toArray());

        // octave = v1 .* 2.0
        final FieldVector<Fraction> v_mapMultiply = v1.mapMultiply(new Fraction(2));
        final Fraction[] result_mapMultiply = { new Fraction(2), new Fraction(4), new Fraction(6) };
        this.checkArray("compare vectors", result_mapMultiply, v_mapMultiply.toArray());

        // octave = v1 .* 2.0
        final FieldVector<Fraction> v_mapMultiplyToSelf = v1.copy();
        v_mapMultiplyToSelf.mapMultiplyToSelf(new Fraction(2));
        final Fraction[] result_mapMultiplyToSelf = { new Fraction(2), new Fraction(4),
                new Fraction(6) };
        this.checkArray("compare vectors", result_mapMultiplyToSelf, v_mapMultiplyToSelf.toArray());

        // octave = v1 ./ 2.0
        final FieldVector<Fraction> v_mapDivide = v1.mapDivide(new Fraction(2));
        final Fraction[] result_mapDivide = { new Fraction(1, 2), new Fraction(1),
                new Fraction(3, 2) };
        this.checkArray("compare vectors", result_mapDivide, v_mapDivide.toArray());

        // octave = v1 ./ 2.0
        final FieldVector<Fraction> v_mapDivideToSelf = v1.copy();
        v_mapDivideToSelf.mapDivideToSelf(new Fraction(2));
        final Fraction[] result_mapDivideToSelf = { new Fraction(1, 2), new Fraction(1),
                new Fraction(3, 2) };
        this.checkArray("compare vectors", result_mapDivideToSelf, v_mapDivideToSelf.toArray());

        // octave = v1 .^-1
        final FieldVector<Fraction> v_mapInv = v1.mapInv();
        final Fraction[] result_mapInv = { new Fraction(1), new Fraction(1, 2), new Fraction(1, 3) };
        this.checkArray("compare vectors", result_mapInv, v_mapInv.toArray());

        // octave = v1 .^-1
        final FieldVector<Fraction> v_mapInvToSelf = v1.copy();
        v_mapInvToSelf.mapInvToSelf();
        final Fraction[] result_mapInvToSelf = { new Fraction(1), new Fraction(1, 2),
                new Fraction(1, 3) };
        this.checkArray("compare vectors", result_mapInvToSelf, v_mapInvToSelf.toArray());

    }

    @Test
    public void testBasicFunctions() {
        final ArrayFieldVector<Fraction> v1 = new ArrayFieldVector<>(this.vec1);
        final ArrayFieldVector<Fraction> v2 = new ArrayFieldVector<>(this.vec2);
        new ArrayFieldVector<>(this.vec_null);

        final FieldVectorTestImpl<Fraction> v2_t = new FieldVectorTestImpl<>(this.vec2);

        // octave = v1 + v2
        final ArrayFieldVector<Fraction> v_add = v1.add(v2);
        final Fraction[] result_add = { new Fraction(5), new Fraction(7), new Fraction(9) };
        this.checkArray("compare vect", v_add.getData(), result_add);

        final FieldVectorTestImpl<Fraction> vt2 = new FieldVectorTestImpl<>(this.vec2);
        final FieldVector<Fraction> v_add_i = v1.add(vt2);
        final Fraction[] result_add_i = { new Fraction(5), new Fraction(7), new Fraction(9) };
        this.checkArray("compare vect", v_add_i.toArray(), result_add_i);

        // octave = v1 - v2
        final ArrayFieldVector<Fraction> v_subtract = v1.subtract(v2);
        final Fraction[] result_subtract = { new Fraction(-3), new Fraction(-3), new Fraction(-3) };
        this.checkArray("compare vect", v_subtract.getData(), result_subtract);

        final FieldVector<Fraction> v_subtract_i = v1.subtract(vt2);
        final Fraction[] result_subtract_i = { new Fraction(-3), new Fraction(-3), new Fraction(-3) };
        this.checkArray("compare vect", v_subtract_i.toArray(), result_subtract_i);

        // octave v1 .* v2
        final ArrayFieldVector<Fraction> v_ebeMultiply = v1.ebeMultiply(v2);
        final Fraction[] result_ebeMultiply = { new Fraction(4), new Fraction(10), new Fraction(18) };
        this.checkArray("compare vect", v_ebeMultiply.getData(), result_ebeMultiply);

        final FieldVector<Fraction> v_ebeMultiply_2 = v1.ebeMultiply(v2_t);
        final Fraction[] result_ebeMultiply_2 = { new Fraction(4), new Fraction(10),
                new Fraction(18) };
        this.checkArray("compare vect", v_ebeMultiply_2.toArray(), result_ebeMultiply_2);

        // octave v1 ./ v2
        final ArrayFieldVector<Fraction> v_ebeDivide = v1.ebeDivide(v2);
        final Fraction[] result_ebeDivide = { new Fraction(1, 4), new Fraction(2, 5),
                new Fraction(1, 2) };
        this.checkArray("compare vect", v_ebeDivide.getData(), result_ebeDivide);

        final FieldVector<Fraction> v_ebeDivide_2 = v1.ebeDivide(v2_t);
        final Fraction[] result_ebeDivide_2 = { new Fraction(1, 4), new Fraction(2, 5),
                new Fraction(1, 2) };
        this.checkArray("compare vect", v_ebeDivide_2.toArray(), result_ebeDivide_2);

        // octave dot(v1,v2)
        final Fraction dot = v1.dotProduct(v2);
        Assert.assertEquals("compare val ", new Fraction(32), dot);

        // octave dot(v1,v2_t)
        final Fraction dot_2 = v1.dotProduct(v2_t);
        Assert.assertEquals("compare val ", new Fraction(32), dot_2);

        final FieldMatrix<Fraction> m_outerProduct = v1.outerProduct(v2);
        Assert.assertEquals("compare val ", new Fraction(4), m_outerProduct.getEntry(0, 0));

        final FieldMatrix<Fraction> m_outerProduct_2 = v1.outerProduct(v2_t);
        Assert.assertEquals("compare val ", new Fraction(4), m_outerProduct_2.getEntry(0, 0));

        final ArrayFieldVector<Fraction> v_projection = v1.projection(v2);
        final Fraction[] result_projection = { new Fraction(128, 77), new Fraction(160, 77),
                new Fraction(192, 77) };
        this.checkArray("compare vect", v_projection.getData(), result_projection);

        final FieldVector<Fraction> v_projection_2 = v1.projection(v2_t);
        final Fraction[] result_projection_2 = { new Fraction(128, 77), new Fraction(160, 77),
                new Fraction(192, 77) };
        this.checkArray("compare vect", v_projection_2.toArray(), result_projection_2);
    }

    @Test
    public void testMisc() {
        final ArrayFieldVector<Fraction> v1 = new ArrayFieldVector<>(this.vec1);
        final ArrayFieldVector<Fraction> v4 = new ArrayFieldVector<>(this.vec4);
        final FieldVector<Fraction> v4_2 = new ArrayFieldVector<>(this.vec4);

        final String out1 = v1.toString();
        Assert.assertTrue("some output ", out1.length() != 0);
        /*
         * Fraction[] dout1 = v1.copyOut();
         * Assert.assertEquals(3, dout1.length);
         * assertNotSame("testData not same object ", v1.getDataRef(), dout1);
         */
        try {
            v1.checkVectorDimensions(2);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }

        try {
            v1.checkVectorDimensions(v4);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }

        try {
            v1.checkVectorDimensions(v4_2);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }
    }

    @Test
    public void testSerial() {
        final ArrayFieldVector<Fraction> v = new ArrayFieldVector<>(this.vec1);
        Assert.assertEquals(v, TestUtils.serializeAndRecover(v));
    }

    @Test
    public void testZeroVectors() {

        // when the field is not specified, array cannot be empty
        try {
            new ArrayFieldVector<>(new Fraction[0]);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }
        try {
            new ArrayFieldVector<>(new Fraction[0], true);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }
        try {
            new ArrayFieldVector<>(new Fraction[0], false);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }

        // when the field is specified, array can be empty
        Assert.assertEquals(0,
                new ArrayFieldVector<>(FractionField.getInstance(), new Fraction[0]).getDimension());
        Assert.assertEquals(0, new ArrayFieldVector<>(FractionField.getInstance(), new Fraction[0],
                true).getDimension());
        Assert.assertEquals(0, new ArrayFieldVector<>(FractionField.getInstance(), new Fraction[0],
                false).getDimension());
    }

    @Test
    public void testOuterProduct() {
        final ArrayFieldVector<Fraction> u = new ArrayFieldVector<>(FractionField.getInstance(),
                new Fraction[] { new Fraction(1), new Fraction(2), new Fraction(-3) });
        final ArrayFieldVector<Fraction> v = new ArrayFieldVector<>(FractionField.getInstance(),
                new Fraction[] { new Fraction(4), new Fraction(-2) });

        final FieldMatrix<Fraction> uv = u.outerProduct(v);

        final double tol = Math.ulp(1d);
        Assert.assertEquals(new Fraction(4).doubleValue(), uv.getEntry(0, 0).doubleValue(), tol);
        Assert.assertEquals(new Fraction(-2).doubleValue(), uv.getEntry(0, 1).doubleValue(), tol);
        Assert.assertEquals(new Fraction(8).doubleValue(), uv.getEntry(1, 0).doubleValue(), tol);
        Assert.assertEquals(new Fraction(-4).doubleValue(), uv.getEntry(1, 1).doubleValue(), tol);
        Assert.assertEquals(new Fraction(-12).doubleValue(), uv.getEntry(2, 0).doubleValue(), tol);
        Assert.assertEquals(new Fraction(6).doubleValue(), uv.getEntry(2, 1).doubleValue(), tol);
    }

    /**
     * For coverage purposes, checks the behavior of constructors when d==null.
     * NullArgumentException expected.
     * In the test below, there are surrounded with try catch because this test has to be done
     * for constructor number 4 to 13.
     */
    @Test
    public void testConstructorsdnull() {

        // creating all kinds of arguments required
        // tests the if (d == null)
        final Fraction[] d = null;
        final Field<Fraction> field = FractionField.getInstance();
        final boolean copyArray = true;
        final FieldVector<Fraction> v = null;
        final ArrayFieldVector<Fraction> v2 = null;

        // 4th constructor : ArrayFieldVector(T[] d)
        try {
            new ArrayFieldVector<>(d);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 5th constructor : ArrayFieldVector(Field<T> field, T[] d)
        try {
            new ArrayFieldVector<>(field, d);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 6th constructor : ArrayFieldVector(T[] d, boolean copyArray)
        try {
            new ArrayFieldVector<>(d, copyArray);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 7th constructor : ArrayFieldVector(Field<T> field, T[] d, boolean copyArray)
        try {
            new ArrayFieldVector<>(field, d, copyArray);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 8th constructor : ArrayFieldVector(T[] d, int pos, int size)
        try {
            new ArrayFieldVector<>(d, 1, 1);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 9th constructor : ArrayFieldVector(Field<T> field, T[] d, int pos, int size)
        try {
            new ArrayFieldVector<>(field, d, 1, 1);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 10th constructor : ArrayFieldVector(FieldVector<T> v)
        try {
            new ArrayFieldVector<>(v);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 11th constructor : ArrayFieldVector(ArrayFieldVector<T> v)
        try {
            new ArrayFieldVector<>(v2);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 12th constructor : ArrayFieldVector(ArrayFieldVector<T> v, boolean deep)
        try {
            new ArrayFieldVector<>(v2, copyArray);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        // 13th constructor : ArrayFieldVector(ArrayFieldVector<T> v1, ArrayFieldVector<T> v2)
        try {
            new ArrayFieldVector<>(v2, v2);
            Assert.fail();
        } catch (final NullArgumentException ex) {
            // expected behavior
        }
    }

    /**
     * For coverage purposes, checks the gestion of NumberIsTooLargeException in
     * the 9th constructor ArrayFieldVector(Field<T> field, T[] d, int pos, int size).
     */
    @Test(expected = NumberIsTooLargeException.class)
    public void testNumberIsTooLargeException9thConstructor() {
        // 9th constructor : if (d.length < pos + size) {
        final Fraction[] d2 = new Fraction[1];
        d2[0] = new Fraction(14);
        final Field<Fraction> field = FractionField.getInstance();
        new ArrayFieldVector<>(field, d2, 1, 1);
    }

    /** verifies that two vectors are equals */
    protected void checkArray(final String msg, final Fraction[] m, final Fraction[] n) {
        if (m.length != n.length) {
            Assert.fail("vectors have different lengths");
        }
        for (int i = 0; i < m.length; i++) {
            Assert.assertEquals(msg + " " + i + " elements differ", m[i], n[i]);
        }
    }
}
