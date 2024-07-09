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
 * VERSION::FA:306:14/11/2014: coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test cases for the {@link ArrayRealVector} class.
 * 
 * @version $Id: ArrayRealVectorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ArrayRealVectorTest extends RealVectorAbstractTest {

    @Override
    public RealVector create(final double[] data) {
        return new ArrayRealVector(data, true);
    }

    @Test
    public void testConstructors() {
        final double[] vec1 = { 1d, 2d, 3d };
        final double[] vec3 = { 7d, 8d, 9d };
        final double[] vec4 = { 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d };
        final Double[] dvec1 = { 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d };

        final ArrayRealVector v0 = new ArrayRealVector();
        Assert.assertEquals("testData len", 0, v0.getDimension());

        final ArrayRealVector v1 = new ArrayRealVector(7);
        Assert.assertEquals("testData len", 7, v1.getDimension());
        Assert.assertEquals("testData is 0.0 ", 0.0, v1.getEntry(6), 0);

        final ArrayRealVector v2 = new ArrayRealVector(5, 1.23);
        Assert.assertEquals("testData len", 5, v2.getDimension());
        Assert.assertEquals("testData is 1.23 ", 1.23, v2.getEntry(4), 0);

        final ArrayRealVector v3 = new ArrayRealVector(vec1);
        Assert.assertEquals("testData len", 3, v3.getDimension());
        Assert.assertEquals("testData is 2.0 ", 2.0, v3.getEntry(1), 0);

        final ArrayRealVector v3_bis = new ArrayRealVector(vec1, true);
        Assert.assertEquals("testData len", 3, v3_bis.getDimension());
        Assert.assertEquals("testData is 2.0 ", 2.0, v3_bis.getEntry(1), 0);
        Assert.assertNotSame(v3_bis.getDataRef(), vec1);
        Assert.assertNotSame(v3_bis.toArray(), vec1);

        final ArrayRealVector v3_ter = new ArrayRealVector(vec1, false);
        Assert.assertEquals("testData len", 3, v3_ter.getDimension());
        Assert.assertEquals("testData is 2.0 ", 2.0, v3_ter.getEntry(1), 0);
        Assert.assertSame(v3_ter.getDataRef(), vec1);
        Assert.assertNotSame(v3_ter.toArray(), vec1);

        final ArrayRealVector v4 = new ArrayRealVector(vec4, 3, 2);
        Assert.assertEquals("testData len", 2, v4.getDimension());
        Assert.assertEquals("testData is 4.0 ", 4.0, v4.getEntry(0), 0);
        try {
            new ArrayRealVector(vec4, 8, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }

        final RealVector v5_i = new ArrayRealVector(dvec1);
        Assert.assertEquals("testData len", 9, v5_i.getDimension());
        Assert.assertEquals("testData is 9.0 ", 9.0, v5_i.getEntry(8), 0);

        final ArrayRealVector v5 = new ArrayRealVector(dvec1);
        Assert.assertEquals("testData len", 9, v5.getDimension());
        Assert.assertEquals("testData is 9.0 ", 9.0, v5.getEntry(8), 0);

        final ArrayRealVector v6 = new ArrayRealVector(dvec1, 3, 2);
        Assert.assertEquals("testData len", 2, v6.getDimension());
        Assert.assertEquals("testData is 4.0 ", 4.0, v6.getEntry(0), 0);
        try {
            new ArrayRealVector(dvec1, 8, 3);
            Assert.fail("MathIllegalArgumentException expected");
        } catch (final MathIllegalArgumentException ex) {
            // expected behavior
        }

        final ArrayRealVector v7 = new ArrayRealVector(v1);
        Assert.assertEquals("testData len", 7, v7.getDimension());
        Assert.assertEquals("testData is 0.0 ", 0.0, v7.getEntry(6), 0);

        final RealVectorTestImpl v7_i = new RealVectorTestImpl(vec1);

        final ArrayRealVector v7_2 = new ArrayRealVector(v7_i);
        Assert.assertEquals("testData len", 3, v7_2.getDimension());
        Assert.assertEquals("testData is 0.0 ", 2.0d, v7_2.getEntry(1), 0);

        final ArrayRealVector v8 = new ArrayRealVector(v1, true);
        Assert.assertEquals("testData len", 7, v8.getDimension());
        Assert.assertEquals("testData is 0.0 ", 0.0, v8.getEntry(6), 0);
        Assert.assertNotSame("testData not same object ", v1.getDataRef(), v8.getDataRef());

        final ArrayRealVector v8_2 = new ArrayRealVector(v1, false);
        Assert.assertEquals("testData len", 7, v8_2.getDimension());
        Assert.assertEquals("testData is 0.0 ", 0.0, v8_2.getEntry(6), 0);
        Assert.assertEquals("testData same object ", v1.getDataRef(), v8_2.getDataRef());

        final ArrayRealVector v9 = new ArrayRealVector(v1, v3);
        Assert.assertEquals("testData len", 10, v9.getDimension());
        Assert.assertEquals("testData is 1.0 ", 1.0, v9.getEntry(7), 0);

        final ArrayRealVector v10 = new ArrayRealVector(v2, new RealVectorTestImpl(vec3));
        Assert.assertEquals("testData len", 8, v10.getDimension());
        Assert.assertEquals("testData is 1.23 ", 1.23, v10.getEntry(4), 0);
        Assert.assertEquals("testData is 7.0 ", 7.0, v10.getEntry(5), 0);

        final ArrayRealVector v11 = new ArrayRealVector(new RealVectorTestImpl(vec3), v2);
        Assert.assertEquals("testData len", 8, v11.getDimension());
        Assert.assertEquals("testData is 9.0 ", 9.0, v11.getEntry(2), 0);
        Assert.assertEquals("testData is 1.23 ", 1.23, v11.getEntry(3), 0);

        final ArrayRealVector v12 = new ArrayRealVector(v2, vec3);
        Assert.assertEquals("testData len", 8, v12.getDimension());
        Assert.assertEquals("testData is 1.23 ", 1.23, v12.getEntry(4), 0);
        Assert.assertEquals("testData is 7.0 ", 7.0, v12.getEntry(5), 0);

        final ArrayRealVector v13 = new ArrayRealVector(vec3, v2);
        Assert.assertEquals("testData len", 8, v13.getDimension());
        Assert.assertEquals("testData is 9.0 ", 9.0, v13.getEntry(2), 0);
        Assert.assertEquals("testData is 1.23 ", 1.23, v13.getEntry(3), 0);

        final ArrayRealVector v14 = new ArrayRealVector(vec3, vec4);
        Assert.assertEquals("testData len", 12, v14.getDimension());
        Assert.assertEquals("testData is 9.0 ", 9.0, v14.getEntry(2), 0);
        Assert.assertEquals("testData is 1.0 ", 1.0, v14.getEntry(3), 0);

        final double d[] = null;
        final Double dd[] = null;
        final RealVector v = null;
        try {
            new ArrayRealVector(d, true);
            Assert.fail("NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        try {
            new ArrayRealVector(d, 0, 1);
            Assert.fail("NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        try {
            new ArrayRealVector(dd, 0, 1);
            Assert.fail("NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

        try {
            new ArrayRealVector(v);
            Assert.fail("NullArgumentException expected");
        } catch (final NullArgumentException ex) {
            // expected behavior
        }

    }

    @Test
    public void testGetDataRef() {
        final double[] data = { 1d, 2d, 3d, 4d };
        final ArrayRealVector v = new ArrayRealVector(data);
        v.getDataRef()[0] = 0d;
        Assert.assertEquals("", 0d, v.getEntry(0), 0);
    }

    @Test
    public void testPredicates() {

        Assert.assertEquals(this.create(new double[] { Double.NaN, 1, 2 }).hashCode(),
            this.create(new double[] { 0, Double.NaN, 2 }).hashCode());

        Assert.assertTrue(this.create(new double[] { Double.NaN, 1, 2 }).hashCode() !=
            this.create(new double[] { 0, 1, 2 }).hashCode());
    }

    @Test
    public void testZeroVectors() {
        Assert.assertEquals(0, new ArrayRealVector(new double[0]).getDimension());
        Assert.assertEquals(0, new ArrayRealVector(new double[0], true).getDimension());
        Assert.assertEquals(0, new ArrayRealVector(new double[0], false).getDimension());
    }

    /**
     * For coverage purposes, to get in the catch bloc of method
     * setSubVector. It has to be a RealVector but not an ArrayRealVector
     */
    @Test(expected = OutOfRangeException.class)
    public void testExceptionSetSubVector() {

        final ArrayRealVector vec = new ArrayRealVector(3, 14);
        final double[] datav = { 2, 2, 2 };
        final RealVectorTestImpl v = new RealVectorTestImpl(datav);
        final int index = 0;
        vec.setSubVector(index, v);
        final double[] result = vec.getDataRef();
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(datav[i], result[i], Precision.DOUBLE_COMPARISON_EPSILON);
        }

        final ArrayRealVector vec2 = new ArrayRealVector(3, 14);
        final double[] datav2 = { 2, 2, 2, 2, 2 };
        final RealVectorTestImpl v2 = new RealVectorTestImpl(datav2);
        final int index2 = 2;
        vec2.setSubVector(index2, v2);

    }

    /**
     * Evaluate the {@link ArrayRealVector#ebeMultiply(RealVector) ebeMultiply(RealVector)} method.
     */
    @Test
    public void testEbeMultiply() {

        final ArrayRealVector vec = new ArrayRealVector(new double[] { 3., 14. });
        final ArrayRealVector v = new ArrayRealVector(new double[] { 3., 2. });

        ArrayRealVector expected;
        ArrayRealVector result;

        result = vec.ebeMultiply(v);
        expected = new ArrayRealVector(new double[] { 9., 28. });

        for (int i = 0; i < vec.getDimension(); i++) {
            Assert.assertEquals(expected.getEntry(i), result.getEntry(i), Precision.DOUBLE_COMPARISON_EPSILON);
        }

        try {
            vec.ebeMultiply(new ArrayRealVector(new double[] { 9., 28., 3. }));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Generic type
        result = vec.ebeMultiply(RealVector.unmodifiableRealVector(v));

        for (int i = 0; i < vec.getDimension(); i++) {
            Assert.assertEquals(expected.getEntry(i), result.getEntry(i), Precision.DOUBLE_COMPARISON_EPSILON);
        }

        try {
            vec.ebeMultiply(new ArrayRealVector(new double[] { 9., 28., 3. }));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Evaluate the {@link ArrayRealVector#ebeDivide(RealVector) ebeDivide(RealVector)} method.
     */
    @Test
    public void testEbeDivide() {

        final ArrayRealVector vec = new ArrayRealVector(new double[] { 9., 28. });
        final ArrayRealVector v = new ArrayRealVector(new double[] { 3., 7. });

        ArrayRealVector expected;
        ArrayRealVector result;

        result = vec.ebeDivide(v);
        expected = new ArrayRealVector(new double[] { 3., 4. });

        for (int i = 0; i < vec.getDimension(); i++) {
            Assert.assertEquals(expected.getEntry(i), result.getEntry(i), Precision.DOUBLE_COMPARISON_EPSILON);
        }

        try {
            vec.ebeMultiply(new ArrayRealVector(new double[] { 3., 4., 2. }));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Generic type
        result = vec.ebeDivide(RealVector.unmodifiableRealVector(v));

        for (int i = 0; i < vec.getDimension(); i++) {
            Assert.assertEquals(expected.getEntry(i), result.getEntry(i), Precision.DOUBLE_COMPARISON_EPSILON);
        }

        try {
            vec.ebeMultiply(new ArrayRealVector(new double[] { 9., 28., 3. }));
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

}
