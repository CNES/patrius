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
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.linear;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Abs;
import fr.cnes.sirius.patrius.math.analysis.function.Acos;
import fr.cnes.sirius.patrius.math.analysis.function.Asin;
import fr.cnes.sirius.patrius.math.analysis.function.Atan;
import fr.cnes.sirius.patrius.math.analysis.function.Cbrt;
import fr.cnes.sirius.patrius.math.analysis.function.Ceil;
import fr.cnes.sirius.patrius.math.analysis.function.Cos;
import fr.cnes.sirius.patrius.math.analysis.function.Cosh;
import fr.cnes.sirius.patrius.math.analysis.function.Exp;
import fr.cnes.sirius.patrius.math.analysis.function.Expm1;
import fr.cnes.sirius.patrius.math.analysis.function.Floor;
import fr.cnes.sirius.patrius.math.analysis.function.Inverse;
import fr.cnes.sirius.patrius.math.analysis.function.Log;
import fr.cnes.sirius.patrius.math.analysis.function.Log10;
import fr.cnes.sirius.patrius.math.analysis.function.Log1p;
import fr.cnes.sirius.patrius.math.analysis.function.Power;
import fr.cnes.sirius.patrius.math.analysis.function.Rint;
import fr.cnes.sirius.patrius.math.analysis.function.Signum;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.function.Sinh;
import fr.cnes.sirius.patrius.math.analysis.function.Sqrt;
import fr.cnes.sirius.patrius.math.analysis.function.Tan;
import fr.cnes.sirius.patrius.math.analysis.function.Tanh;
import fr.cnes.sirius.patrius.math.analysis.function.Ulp;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NotPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathArrays;
import fr.cnes.sirius.patrius.math.util.MathLib;

public abstract class RealVectorAbstractTest {

    private enum BinaryOperation {
        ADD, SUB, MUL, DIV
    }

    /**
     * <p>
     * This is an attempt at covering most particular cases of combining two values. Here {@code x} is the value
     * returned by {@link #getPreferredEntryValue()}, while {@code y} and {@code z} are two "normal" values.
     * </p>
     * <ol>
     * <li>
     * Addition: the following cases should be covered
     * <ul>
     * <li>{@code (2 * x) + (-x)}</li>
     * <li>{@code (-x) + 2 * x}</li>
     * <li>{@code x + y}</li>
     * <li>{@code y + x}</li>
     * <li>{@code y + z}</li>
     * <li>{@code y + (x - y)}</li>
     * <li>{@code (y - x) + x}</li>
     * </ul>
     * The values to be considered are: {@code x, y, z, 2 * x, -x, x - y, y - x}.</li>
     * <li>
     * Subtraction: the following cases should be covered
     * <ul>
     * <li>{@code (2 * x) - x}</li>
     * <li>{@code x - y}</li>
     * <li>{@code y - x}</li>
     * <li>{@code y - z}</li>
     * <li>{@code y - (y - x)}</li>
     * <li>{@code (y + x) - y}</li>
     * </ul>
     * The values to be considered are: {@code x, y, z, x + y, y - x}.</li>
     * <li>
     * Multiplication
     * <ul>
     * <li>{@code (x * x) * (1 / x)}</li>
     * <li>{@code (1 / x) * (x * x)}</li>
     * <li>{@code x * y}</li>
     * <li>{@code y * x}</li>
     * <li>{@code y * z}</li>
     * </ul>
     * The values to be considered are: {@code x, y, z, 1 / x, x * x}.</li>
     * <li>
     * Division
     * <ul>
     * <li>{@code (x * x) / x}</li>
     * <li>{@code x / y}</li>
     * <li>{@code y / x}</li>
     * <li>{@code y / z}</li>
     * </ul>
     * The values to be considered are: {@code x, y, z, x * x}.</li>
     * </ol>
     * Also to be considered {@code NaN}, {@code POSITIVE_INFINITY}, {@code NEGATIVE_INFINITY}, {@code +0.0},
     * {@code -0.0}.
     */
    private final double[] values;

    /**
     * Creates a new instance of {@link RealVector}, with specified entries.
     * The returned vector must be of the type currently tested. It should be
     * noted that some tests assume that no references to the specified {@code double[]} are kept in the returned
     * object: if necessary, defensive
     * copy of this array should be made.
     * 
     * @param data
     *        the entries of the vector to be created
     * @return a new {@link RealVector} of the type to be tested
     */
    public abstract RealVector create(double[] data);

    /**
     * Creates a new instance of {@link RealVector}, with specified entries.
     * The type of the returned vector must be different from the type currently
     * tested. It should be noted that some tests assume that no references to
     * the specified {@code double[]} are kept in the returned object: if
     * necessary, defensive copy of this array should be made.
     * 
     * @param data
     *        the entries of the vector to be created
     * @return a new {@link RealVector} of an alien type
     */
    public RealVector createAlien(final double[] data) {
        return new RealVectorTestImpl(data);
    }

    /**
     * Returns a preferred value of the entries, to be tested specifically. Some
     * implementations of {@link RealVector} do
     * not store specific values of entries. In order to ensure that all tests
     * take into account this specific value, some entries of the vectors to be
     * tested are deliberately set to the value returned by the present method.
     * The default implementation returns {@code 0.0}.
     * 
     * @return a value which <em>should</em> be present in all vectors to be
     *         tested
     */
    public double getPreferredEntryValue() {
        return 0.0;
    }

    public RealVectorAbstractTest() {
        /*
         * Make sure that x, y, z are three different values. Also, x is the
         * preferred value (e.g. the value which is not stored in sparse
         * implementations).
         */
        final double x = getPreferredEntryValue();
        final double y = x + 1d;
        final double z = y + 1d;

        this.values =
            new double[] {
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                0d, -0d, x, y, z, 2 * x, -x, 1 / x, x * x, x + y, x - y, y - x
            };
    }

    @Test
    public void testGetDimension() {
        final double x = getPreferredEntryValue();
        final double[] data1 = { x, x, x, x };
        Assert.assertEquals(data1.length, create(data1).getDimension());
        final double y = x + 1;
        final double[] data2 = { y, y, y, y };
        Assert.assertEquals(data2.length, create(data2).getDimension());
    }

    @Test
    public void testGetEntry() {
        final double x = getPreferredEntryValue();
        final double[] data = { x, 1d, 2d, x, x };
        final RealVector v = create(data);
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals("entry " + i, data[i], v.getEntry(i), 0d);
        }
    }

    @Test(expected = OutOfRangeException.class)
    public void testGetEntryInvalidIndex1() {
        create(new double[4]).getEntry(-1);
    }

    @Test(expected = OutOfRangeException.class)
    public void testGetEntryInvalidIndex2() {
        create(new double[4]).getEntry(4);
    }

    @Test
    public void testSetEntry() {
        final double x = getPreferredEntryValue();
        final double[] data = { x, 1d, 2d, x, x };
        final double[] expected = MathArrays.copyOf(data);
        final RealVector actual = create(data);

        /*
         * Try setting to any value.
         */
        for (int i = 0; i < data.length; i++) {
            final double oldValue = data[i];
            final double newValue = oldValue + 1d;
            expected[i] = newValue;
            actual.setEntry(i, newValue);
            TestUtils.assertEquals("while setting entry #" + i, expected,
                actual, 0d);
            expected[i] = oldValue;
            actual.setEntry(i, oldValue);
        }

        /*
         * Try setting to the preferred value.
         */
        for (int i = 0; i < data.length; i++) {
            final double oldValue = data[i];
            final double newValue = x;
            expected[i] = newValue;
            actual.setEntry(i, newValue);
            TestUtils.assertEquals("while setting entry #" + i, expected,
                actual, 0d);
            expected[i] = oldValue;
            actual.setEntry(i, oldValue);
        }
    }

    @Test(expected = OutOfRangeException.class)
    public void testSetEntryInvalidIndex1() {
        create(new double[4]).setEntry(-1, getPreferredEntryValue());
    }

    @Test(expected = OutOfRangeException.class)
    public void testSetEntryInvalidIndex2() {
        create(new double[4]).setEntry(4, getPreferredEntryValue());
    }

    @Test
    public void testAddToEntry() {
        final double x = getPreferredEntryValue();
        final double[] data1 = { x, 1d, 2d, x, x };
        final double[] expected = MathArrays.copyOf(data1);
        final RealVector actual = create(data1);

        /*
         * Try adding any value.
         */
        double increment = 1d;
        for (int i = 0; i < data1.length; i++) {
            final double oldValue = data1[i];
            expected[i] += increment;
            actual.addToEntry(i, increment);
            TestUtils.assertEquals("while incrementing entry #" + i, expected,
                actual, 0d);
            expected[i] = oldValue;
            actual.setEntry(i, oldValue);
        }

        /*
         * Try incrementing so that result is equal to preferred value.
         */
        for (int i = 0; i < data1.length; i++) {
            final double oldValue = data1[i];
            increment = x - oldValue;
            expected[i] = x;
            actual.addToEntry(i, increment);
            TestUtils.assertEquals("while incrementing entry #" + i, expected,
                actual, 0d);
            expected[i] = oldValue;
            actual.setEntry(i, oldValue);
        }
    }

    @Test(expected = OutOfRangeException.class)
    public void testAddToEntryInvalidIndex1() {
        create(new double[3]).addToEntry(-1, getPreferredEntryValue());
    }

    @Test(expected = OutOfRangeException.class)
    public void testAddToEntryInvalidIndex2() {
        create(new double[3]).addToEntry(4, getPreferredEntryValue());
    }

    private static void doTestAppendVector(final String message, final RealVector v1,
                                    final RealVector v2, final double delta) {

        final int n1 = v1.getDimension();
        final int n2 = v2.getDimension();
        final RealVector v = v1.append(v2);
        Assert.assertEquals(message, n1 + n2, v.getDimension());
        for (int i = 0; i < n1; i++) {
            final String msg = message + ", entry #" + i;
            Assert.assertEquals(msg, v1.getEntry(i), v.getEntry(i), delta);
        }
        for (int i = 0; i < n2; i++) {
            final String msg = message + ", entry #" + (n1 + i);
            Assert.assertEquals(msg, v2.getEntry(i), v.getEntry(n1 + i), delta);
        }
    }

    @Test
    public void testAppendVector() {
        final double x = getPreferredEntryValue();
        final double[] data1 = { x, 1d, 2d, x, x };
        final double[] data2 = { x, x, 3d, x, 4d, x };

        doTestAppendVector("same type", create(data1), create(data2), 0d);
        doTestAppendVector("mixed types", create(data1), createAlien(data2), 0d);
    }

    private static void doTestAppendScalar(final String message, final RealVector v,
                                    final double d, final double delta) {

        final int n = v.getDimension();
        final RealVector w = v.append(d);
        Assert.assertEquals(message, n + 1, w.getDimension());
        for (int i = 0; i < n; i++) {
            final String msg = message + ", entry #" + i;
            Assert.assertEquals(msg, v.getEntry(i), w.getEntry(i), delta);
        }
        final String msg = message + ", entry #" + n;
        Assert.assertEquals(msg, d, w.getEntry(n), delta);
    }

    @Test
    public void testAppendScalar() {
        final double x = getPreferredEntryValue();
        final double[] data = new double[] { x, 1d, 2d, x, x };

        doTestAppendScalar("", create(data), 1d, 0d);
        doTestAppendScalar("", create(data), x, 0d);
    }

    @Test
    public void testGetSubVector() {
        final double x = getPreferredEntryValue();
        final double[] data = { x, x, x, 1d, x, 2d, x, x, 3d, x, x, x, 4d, x, x, x };
        final int index = 1;
        final int n = data.length - 5;
        final RealVector actual = create(data).getSubVector(index, n);
        final double[] expected = new double[n];
        System.arraycopy(data, index, expected, 0, n);
        TestUtils.assertEquals("", expected, actual, 0d);
    }

    @Test(expected = OutOfRangeException.class)
    public void testGetSubVectorInvalidIndex1() {
        final int n = 10;
        create(new double[n]).getSubVector(-1, 2);
    }

    @Test(expected = OutOfRangeException.class)
    public void testGetSubVectorInvalidIndex2() {
        final int n = 10;
        create(new double[n]).getSubVector(n, 2);
    }

    @Test(expected = OutOfRangeException.class)
    public void testGetSubVectorInvalidIndex3() {
        final int n = 10;
        create(new double[n]).getSubVector(0, n + 1);
    }

    @Test(expected = NotPositiveException.class)
    public void testGetSubVectorInvalidIndex4() {
        final int n = 10;
        create(new double[n]).getSubVector(3, -2);
    }

    @Test
    public void testSetSubVectorSameType() {
        final double x = getPreferredEntryValue();
        final double[] expected = { x, x, x, 1d, x, 2d, x, x, 3d, x, x, x, 4d, x, x, x };
        final double[] sub = { 5d, x, 6d, 7d, 8d };
        final RealVector actual = create(expected);
        final int index = 2;
        actual.setSubVector(index, create(sub));

        for (int i = 0; i < sub.length; i++) {
            expected[index + i] = sub[i];
        }
        TestUtils.assertEquals("", expected, actual, 0d);
    }

    @Test
    public void testSetSubVectorMixedType() {
        final double x = getPreferredEntryValue();
        final double[] expected = { x, x, x, 1d, x, 2d, x, x, 3d, x, x, x, 4d, x, x, x };
        final double[] sub = { 5d, x, 6d, 7d, 8d };
        final RealVector actual = create(expected);
        final int index = 2;
        actual.setSubVector(index, createAlien(sub));

        for (int i = 0; i < sub.length; i++) {
            expected[index + i] = sub[i];
        }
        TestUtils.assertEquals("", expected, actual, 0d);
    }

    @Test(expected = OutOfRangeException.class)
    public void testSetSubVectorInvalidIndex1() {
        create(new double[10]).setSubVector(-1, create(new double[2]));
    }

    @Test(expected = OutOfRangeException.class)
    public void testSetSubVectorInvalidIndex2() {
        create(new double[10]).setSubVector(10, create(new double[2]));
    }

    @Test(expected = OutOfRangeException.class)
    public void testSetSubVectorInvalidIndex3() {
        create(new double[10]).setSubVector(9, create(new double[2]));
    }

    @Test
    public void testIsNaN() {
        final RealVector v = create(new double[] { 0, 1, 2 });

        Assert.assertFalse(v.isNaN());
        v.setEntry(1, Double.NaN);
        Assert.assertTrue(v.isNaN());
    }

    @Test
    public void testIsInfinite() {
        final RealVector v = create(new double[] { 0, 1, 2 });

        Assert.assertFalse(v.isInfinite());
        v.setEntry(0, Double.POSITIVE_INFINITY);
        Assert.assertTrue(v.isInfinite());
        v.setEntry(1, Double.NaN);
        Assert.assertFalse(v.isInfinite());
    }

    private void doTestEbeBinaryOperation(final BinaryOperation op, final boolean mixed) {
        final double[] data1 = new double[this.values.length * this.values.length];
        final double[] data2 = new double[this.values.length * this.values.length];
        int k = 0;
        for (final double value : this.values) {
            for (final double value2 : this.values) {
                data1[k] = value;
                data2[k] = value2;
                ++k;
            }
        }
        final RealVector v1 = create(data1);
        final RealVector v2 = mixed ? createAlien(data2) : create(data2);
        final RealVector actual;
        switch (op) {
            case ADD:
                actual = v1.add(v2);
                break;
            case SUB:
                actual = v1.subtract(v2);
                break;
            default:
                throw new AssertionError("unexpected value");
        }
        final double[] expected = new double[data1.length];
        for (int i = 0; i < expected.length; i++) {
            switch (op) {
                case ADD:
                    expected[i] = data1[i] + data2[i];
                    break;
                case SUB:
                    expected[i] = data1[i] - data2[i];
                    break;
                case MUL:
                    expected[i] = data1[i] * data2[i];
                    break;
                case DIV:
                    expected[i] = data1[i] / data2[i];
                    break;
                default:
                    throw new AssertionError("unexpected value");
            }
        }
        for (int i = 0; i < expected.length; i++) {
            final String msg = "entry #" + i + ", left = " + data1[i] + ", right = " + data2[i];
            Assert.assertEquals(msg, expected[i], actual.getEntry(i), 0.0);
        }
    }

    private void doTestEbeBinaryOperationDimensionMismatch(final BinaryOperation op) {
        final int n = 10;
        switch (op) {
            case ADD:
                create(new double[n]).add(create(new double[n + 1]));
                break;
            case SUB:
                create(new double[n]).subtract(create(new double[n + 1]));
                break;
            default:
                throw new AssertionError("unexpected value");
        }
    }

    @Test
    public void testAddSameType() {
        doTestEbeBinaryOperation(BinaryOperation.ADD, false);
    }

    @Test
    public void testAddMixedTypes() {
        doTestEbeBinaryOperation(BinaryOperation.ADD, true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testAddDimensionMismatch() {
        doTestEbeBinaryOperationDimensionMismatch(BinaryOperation.ADD);
    }

    @Test
    public void testSubtractSameType() {
        doTestEbeBinaryOperation(BinaryOperation.SUB, false);
    }

    @Test
    public void testSubtractMixedTypes() {
        doTestEbeBinaryOperation(BinaryOperation.SUB, true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testSubtractDimensionMismatch() {
        doTestEbeBinaryOperationDimensionMismatch(BinaryOperation.SUB);
    }

    @Ignore("ebeMultiply(RealVector) is known to be faulty (MATH-803) and is deprecated.")
    @Test
    public void testEbeMultiplySameType() {
        doTestEbeBinaryOperation(BinaryOperation.MUL, false);
    }

    @Ignore("ebeMultiply(RealVector) is known to be faulty (MATH-803) and is deprecated.")
    @Test
    public void testEbeMultiplyMixedTypes() {
        doTestEbeBinaryOperation(BinaryOperation.MUL, true);
    }

    @Ignore("ebeMultiply(RealVector) is known to be faulty (MATH-803) and is deprecated.")
    @Test(expected = DimensionMismatchException.class)
    public void testEbeMultiplyDimensionMismatch() {
        doTestEbeBinaryOperationDimensionMismatch(BinaryOperation.MUL);
    }

    @Ignore("ebeDivide(RealVector) is known to be faulty (MATH-803) and is deprecated.")
    @Test
    public void testEbeDivideSameType() {
        doTestEbeBinaryOperation(BinaryOperation.DIV, false);
    }

    @Ignore("ebeDivide(RealVector) is known to be faulty (MATH-803) and is deprecated.")
    @Test
    public void testEbeDivideMixedTypes() {
        doTestEbeBinaryOperation(BinaryOperation.DIV, true);
    }

    @Ignore("ebeDivide(RealVector) is known to be faulty (MATH-803) and is deprecated.")
    @Test(expected = DimensionMismatchException.class)
    public void testEbeDivideDimensionMismatch() {
        doTestEbeBinaryOperationDimensionMismatch(BinaryOperation.DIV);
    }

    private void doTestGetDistance(final boolean mixed) {
        final double x = getPreferredEntryValue();
        final double[] data1 = new double[] { x, x, 1d, x, 2d, x, x, 3d, x };
        final double[] data2 = new double[] { 4d, x, x, 5d, 6d, 7d, x, x, 8d };
        final RealVector v1 = create(data1);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(data2);
        } else {
            v2 = create(data2);
        }
        final double actual = v1.getDistance(v2);
        double expected = 0d;
        for (int i = 0; i < data1.length; i++) {
            final double delta = data2[i] - data1[i];
            expected += delta * delta;
        }
        expected = MathLib.sqrt(expected);
        Assert.assertEquals("", expected, actual, 0d);
    }

    @Test
    public void testGetDistanceSameType() {
        doTestGetDistance(false);
    }

    @Test
    public void testGetDistanceMixedTypes() {
        doTestGetDistance(true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testGetDistanceDimensionMismatch() {
        create(new double[4]).getDistance(createAlien(new double[5]));
    }

    @Test
    public void testGetNorm() {
        final double x = getPreferredEntryValue();
        final double[] data = new double[] { x, x, 1d, x, 2d, x, x, 3d, x };
        final RealVector v = create(data);
        final double actual = v.getNorm();
        double expected = 0d;
        for (final double element : data) {
            expected += element * element;
        }
        expected = MathLib.sqrt(expected);
        Assert.assertEquals("", expected, actual, 0d);
        
        Assert.assertEquals(6, new ArrayRealVector(new double[] { 1, 2, -3}).getL1Norm(), 0);
        Assert.assertEquals(3, new ArrayRealVector(new double[] { 1, 2, -3}).getLInfNorm(), 0);
    }

    private void doTestGetL1Distance(final boolean mixed) {
        final double x = getPreferredEntryValue();
        final double[] data1 = new double[] { x, x, 1d, x, 2d, x, x, 3d, x };
        final double[] data2 = new double[] { 4d, x, x, 5d, 6d, 7d, x, x, 8d };
        final RealVector v1 = create(data1);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(data2);
        } else {
            v2 = create(data2);
        }
        final double actual = v1.getL1Distance(v2);
        double expected = 0d;
        for (int i = 0; i < data1.length; i++) {
            final double delta = data2[i] - data1[i];
            expected += MathLib.abs(delta);
        }
        Assert.assertEquals("", expected, actual, 0d);
    }

    @Test
    public void testGetL1DistanceSameType() {
        doTestGetL1Distance(false);
    }

    @Test
    public void testGetL1DistanceMixedTypes() {
        doTestGetL1Distance(true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testGetL1DistanceDimensionMismatch() {
        create(new double[4]).getL1Distance(createAlien(new double[5]));
    }

    @Test
    public void testGetL1Norm() {
        final double x = getPreferredEntryValue();
        final double[] data = new double[] { x, x, 1d, x, 2d, x, x, 3d, x };
        final RealVector v = create(data);
        final double actual = v.getL1Norm();
        double expected = 0d;
        for (final double element : data) {
            expected += MathLib.abs(element);
        }
        Assert.assertEquals("", expected, actual, 0d);

    }

    private void doTestGetLInfDistance(final boolean mixed) {
        final double x = getPreferredEntryValue();
        final double[] data1 = new double[] { x, x, 1d, x, 2d, x, x, 3d, x };
        final double[] data2 = new double[] { 4d, x, x, 5d, 6d, 7d, x, x, 8d };
        final RealVector v1 = create(data1);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(data2);
        } else {
            v2 = create(data2);
        }
        final double actual = v1.getLInfDistance(v2);
        double expected = 0d;
        for (int i = 0; i < data1.length; i++) {
            final double delta = data2[i] - data1[i];
            expected = MathLib.max(expected, MathLib.abs(delta));
        }
        Assert.assertEquals("", expected, actual, 0d);
    }

    @Test
    public void testGetLInfDistanceSameType() {
        doTestGetLInfDistance(false);
    }

    @Test
    public void testGetLInfDistanceMixedTypes() {
        doTestGetLInfDistance(true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testGetLInfDistanceDimensionMismatch() {
        create(new double[4]).getLInfDistance(createAlien(new double[5]));
    }

    @Test
    public void testGetLInfNorm() {
        final double x = getPreferredEntryValue();
        final double[] data = new double[] { x, x, 1d, x, 2d, x, x, 3d, x };
        final RealVector v = create(data);
        final double actual = v.getLInfNorm();
        double expected = 0d;
        for (final double element : data) {
            expected = MathLib.max(expected, MathLib.abs(element));
        }
        Assert.assertEquals("", expected, actual, 0d);

    }

    private void doTestMapBinaryOperation(final BinaryOperation op, final boolean inPlace) {
        final double[] expected = new double[this.values.length];
        for (final double d : this.values) {
            for (int j = 0; j < expected.length; j++) {
                switch (op) {
                    case ADD:
                        expected[j] = this.values[j] + d;
                        break;
                    case SUB:
                        expected[j] = this.values[j] - d;
                        break;
                    case MUL:
                        expected[j] = this.values[j] * d;
                        break;
                    case DIV:
                        expected[j] = this.values[j] / d;
                        break;
                    default:
                        throw new AssertionError("unexpected value");
                }
            }
            final RealVector v = create(this.values);
            final RealVector actual;
            if (inPlace) {
                switch (op) {
                    case ADD:
                        actual = v.mapAddToSelf(d);
                        break;
                    case SUB:
                        actual = v.mapSubtractToSelf(d);
                        break;
                    case MUL:
                        actual = v.mapMultiplyToSelf(d);
                        break;
                    case DIV:
                        actual = v.mapDivideToSelf(d);
                        break;
                    default:
                        throw new AssertionError("unexpected value");
                }
            } else {
                switch (op) {
                    case ADD:
                        actual = v.mapAdd(d);
                        break;
                    case SUB:
                        actual = v.mapSubtract(d);
                        break;
                    case MUL:
                        actual = v.mapMultiply(d);
                        break;
                    case DIV:
                        actual = v.mapDivide(d);
                        break;
                    default:
                        throw new AssertionError("unexpected value");
                }
            }
            TestUtils.assertEquals(Double.toString(d), expected, actual, 0d);
        }
    }

    @Test
    public void testMapAdd() {
        doTestMapBinaryOperation(BinaryOperation.ADD, false);
    }

    @Test
    public void testMapAddToSelf() {
        doTestMapBinaryOperation(BinaryOperation.ADD, true);
    }

    @Test
    public void testMapSubtract() {
        doTestMapBinaryOperation(BinaryOperation.SUB, false);
    }

    @Test
    public void testMapSubtractToSelf() {
        doTestMapBinaryOperation(BinaryOperation.SUB, true);
    }

    @Test
    public void testMapMultiply() {
        doTestMapBinaryOperation(BinaryOperation.MUL, false);
    }

    @Test
    public void testMapMultiplyToSelf() {
        doTestMapBinaryOperation(BinaryOperation.MUL, true);
    }

    @Test
    public void testMapDivide() {
        doTestMapBinaryOperation(BinaryOperation.DIV, false);
    }

    @Test
    public void testMapDivideToSelf() {
        doTestMapBinaryOperation(BinaryOperation.DIV, true);
    }

    private void doTestMapFunction(final UnivariateFunction f,
                                   final boolean inPlace) {
        final double[] data = new double[this.values.length + 6];
        System.arraycopy(this.values, 0, data, 0, this.values.length);
        data[this.values.length + 0] = 0.5 * FastMath.PI;
        data[this.values.length + 1] = -0.5 * FastMath.PI;
        data[this.values.length + 2] = FastMath.E;
        data[this.values.length + 3] = -FastMath.E;
        data[this.values.length + 4] = 1.0;
        data[this.values.length + 5] = -1.0;
        final double[] expected = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            try {
                expected[i] = f.value(data[i]);
            } catch (final ArithmeticException e) {
                // FT-670: throw Arithmetic exception instead of returning NaN
                expected[i] = Double.NaN;
            }
        }
        final RealVector v = create(data);
        RealVector actual;
        if (inPlace) {
            try {
                actual = v.mapToSelf(f);
                Assert.assertSame(v, actual);
            } catch (final ArithmeticException e) {
                // FT-670: Arithmetic exception thrown instead of returning NaN, do not test this case
                return;
            }
        } else {
            try {
                actual = v.map(f);
            } catch (final ArithmeticException e) {
                // FT-670: Arithmetic exception thrown instead of returning NaN, do not test this case
                return;
            }
        }
        TestUtils.assertEquals(f.getClass().getSimpleName(), expected, actual, 1E-16);
    }

    private static UnivariateFunction[] createFunctions() {
        return new UnivariateFunction[] {
            new Power(2.0), new Exp(), new Expm1(), new Log(), new Log10(),
            new Log1p(), new Cosh(), new Sinh(), new Tanh(), new Cos(),
            new Sin(), new Tan(), new Acos(), new Asin(), new Atan(),
            new Inverse(), new Abs(), new Sqrt(), new Cbrt(), new Ceil(),
            new Floor(), new Rint(), new Signum(), new Ulp()
        };
    }

    @Test
    public void testMap() {
        final UnivariateFunction[] functions = createFunctions();
        for (final UnivariateFunction f : functions) {
            doTestMapFunction(f, false);
        }
    }

    @Test
    public void testMapToSelf() {
        final UnivariateFunction[] functions = createFunctions();
        for (final UnivariateFunction f : functions) {
            doTestMapFunction(f, true);
        }
    }

    private void doTestOuterProduct(final boolean mixed) {
        final double[] dataU = this.values;
        final RealVector u = create(dataU);
        final double[] dataV = new double[this.values.length + 3];
        System.arraycopy(this.values, 0, dataV, 0, this.values.length);
        dataV[this.values.length] = 1d;
        dataV[this.values.length] = -2d;
        dataV[this.values.length] = 3d;
        final RealVector v;
        if (mixed) {
            v = createAlien(dataV);
        } else {
            v = create(dataV);
        }
        final RealMatrix uv = u.outerProduct(v);
        Assert.assertEquals("number of rows", dataU.length, uv
            .getRowDimension());
        Assert.assertEquals("number of columns", dataV.length, uv
            .getColumnDimension());
        for (int i = 0; i < dataU.length; i++) {
            for (int j = 0; j < dataV.length; j++) {
                final double expected = dataU[i] * dataV[j];
                final double actual = uv.getEntry(i, j);
                Assert.assertEquals(dataU[i] + " * " + dataV[j], expected, actual, 0d);
            }
        }
    }

    @Test
    public void testOuterProductSameType() {
        doTestOuterProduct(false);
    }

    @Test
    public void testOuterProductMixedTypes() {
        doTestOuterProduct(true);
    }

    private void doTestProjection(final boolean mixed) {
        final double x = getPreferredEntryValue();
        final double[] data1 = {
            x, 1d, x, x, 2d, x, x, x, 3d, x, x, x, x
        };
        final double[] data2 = {
            5d, -6d, 7d, x, x, -8d, -9d, 10d, 11d, x, 12d, 13d, -15d
        };
        double dotProduct = 0d;
        double norm2 = 0d;
        for (int i = 0; i < data1.length; i++) {
            dotProduct += data1[i] * data2[i];
            norm2 += data2[i] * data2[i];
        }
        final double s = dotProduct / norm2;
        final double[] expected = new double[data1.length];
        for (int i = 0; i < data2.length; i++) {
            expected[i] = s * data2[i];
        }
        final RealVector v1 = create(data1);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(data2);
        } else {
            v2 = create(data2);
        }
        final RealVector actual = v1.projection(v2);
        TestUtils.assertEquals("", expected, actual, 0d);
    }

    @Test
    public void testProjectionSameType() {
        doTestProjection(false);
    }

    @Test
    public void testProjectionMixedTypes() {
        doTestProjection(true);
    }

    @Test(expected = MathArithmeticException.class)
    public void testProjectionNullVector() {
        create(new double[4]).projection(create(new double[4]));
    }

    @Test(expected = DimensionMismatchException.class)
    public void testProjectionDimensionMismatch() {
        final RealVector v1 = create(new double[4]);
        final RealVector v2 = create(new double[5]);
        v2.set(1.0);
        v1.projection(v2);
    }

    @Test
    public void testSet() {
        for (final double expected : this.values) {
            final RealVector v = create(this.values);
            v.set(expected);
            for (int j = 0; j < this.values.length; j++) {
                Assert.assertEquals("entry #" + j, expected, v.getEntry(j), 0);
            }
        }
    }

    @Test
    public void testToArray() {
        final double[] data = create(this.values).toArray();
        Assert.assertNotSame(this.values, data);
        for (int i = 0; i < this.values.length; i++) {
            Assert.assertEquals("entry #" + i, this.values[i], data[i], 0);
        }
    }

    private void doTestUnitVector(final boolean inPlace) {
        final double x = getPreferredEntryValue();
        final double[] data = {
            x, 1d, x, x, 2d, x, x, x, 3d, x, x, x, x
        };
        double norm = 0d;
        for (final double element : data) {
            norm += element * element;
        }
        norm = MathLib.sqrt(norm);
        final double[] expected = new double[data.length];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = data[i] / norm;
        }
        final RealVector v = create(data);
        final RealVector actual;
        if (inPlace) {
            v.unitize();
            actual = v;
        } else {
            actual = v.unitVector();
            Assert.assertNotSame(v, actual);
        }
        TestUtils.assertEquals("", expected, actual, 0d);
    }

    @Test
    public void testUnitVector() {
        doTestUnitVector(false);
    }

    @Test
    public void testUnitize() {
        doTestUnitVector(true);
    }

    private void doTestUnitVectorNullVector(final boolean inPlace) {
        final double[] data = {
            0d, 0d, 0d, 0d, 0d
        };
        if (inPlace) {
            create(data).unitize();
        } else {
            create(data).unitVector();
        }
    }

    @Test(expected = ArithmeticException.class)
    public void testUnitVectorNullVector() {
        doTestUnitVectorNullVector(false);
    }

    @Test(expected = ArithmeticException.class)
    public void testUnitizeNullVector() {
        doTestUnitVectorNullVector(true);
    }

    @Test
    public void testIterator() {
        final RealVector v = create(this.values);
        final Iterator<RealVector.Entry> it = v.iterator();
        for (int i = 0; i < this.values.length; i++) {
            Assert.assertTrue("entry #" + i, it.hasNext());
            final RealVector.Entry e = it.next();
            Assert.assertEquals("", i, e.getIndex());
            Assert.assertEquals("", this.values[i], e.getValue(), 0d);
            try {
                it.remove();
                Assert.fail("UnsupportedOperationException should have been thrown");
            } catch (final UnsupportedOperationException exc) {
                // Expected behavior
            }
        }
        Assert.assertFalse(it.hasNext());
        try {
            it.next();
            Assert.fail("NoSuchElementException should have been thrown");
        } catch (final NoSuchElementException e) {
            // Expected behavior
        }
    }

    private void doTestCombine(final boolean inPlace, final boolean mixed) {
        final int n = this.values.length * this.values.length;
        final double[] data1 = new double[n];
        final double[] data2 = new double[n];
        for (int i = 0; i < this.values.length; i++) {
            for (int j = 0; j < this.values.length; j++) {
                final int index = this.values.length * i + j;
                data1[index] = this.values[i];
                data2[index] = this.values[j];
            }
        }
        final RealVector v1 = create(data1);
        final RealVector v2 = mixed ? createAlien(data2) : create(data2);
        final double[] expected = new double[n];
        for (final double a1 : this.values) {
            for (final double a2 : this.values) {
                for (int k = 0; k < n; k++) {
                    expected[k] = a1 * data1[k] + a2 * data2[k];
                }
                final RealVector actual;
                if (inPlace) {
                    final RealVector v1bis = v1.copy();
                    actual = v1bis.combineToSelf(a1, a2, v2);
                    Assert.assertSame(v1bis, actual);
                } else {
                    actual = v1.combine(a1, a2, v2);
                }
                TestUtils.assertEquals("a1 = " + a1 + ", a2 = " + a2, expected,
                    actual, 0.);
            }
        }
    }

    private void doTestCombineDimensionMismatch(final boolean inPlace, final boolean mixed) {
        final RealVector v1 = create(new double[10]);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(new double[15]);
        } else {
            v2 = create(new double[15]);
        }
        if (inPlace) {
            v1.combineToSelf(1.0, 1.0, v2);
        } else {
            v1.combine(1.0, 1.0, v2);
        }
    }

    @Test
    public void testCombineSameType() {
        doTestCombine(false, false);
    }

    @Test
    public void testCombineMixedTypes() {
        doTestCombine(false, true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testCombineDimensionMismatchSameType() {
        doTestCombineDimensionMismatch(false, false);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testCombineDimensionMismatchMixedTypes() {
        doTestCombineDimensionMismatch(false, true);
    }

    @Test
    public void testCombineToSelfSameType() {
        doTestCombine(true, false);
    }

    @Test
    public void testCombineToSelfMixedTypes() {
        doTestCombine(true, true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testCombineToSelfDimensionMismatchSameType() {
        doTestCombineDimensionMismatch(true, false);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testCombineToSelfDimensionMismatchMixedTypes() {
        doTestCombineDimensionMismatch(true, true);
    }

    @Test
    public void testCopy() {
        final RealVector v = create(this.values);
        final RealVector w = v.copy();
        Assert.assertNotSame(v, w);
        TestUtils.assertEquals("", this.values, w, 0d);
    }

    private void doTestDotProductRegularValues(final boolean mixed) {
        final double x = getPreferredEntryValue();
        final double[] data1 = {
            x, 1d, x, x, 2d, x, x, x, 3d, x, x, x, x
        };
        final double[] data2 = {
            5d, -6d, 7d, x, x, -8d, -9d, 10d, 11d, x, 12d, 13d, -15d
        };
        double expected = 0d;
        for (int i = 0; i < data1.length; i++) {
            expected += data1[i] * data2[i];
        }
        final RealVector v1 = create(data1);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(data2);
        } else {
            v2 = create(data2);
        }
        final double actual = v1.dotProduct(v2);
        Assert.assertEquals("", expected, actual, 0d);
    }

    private void doTestDotProductSpecialValues(final boolean mixed) {
        for (final double value : this.values) {
            final double[] data1 = {
                value
            };
            final RealVector v1 = create(data1);
            for (final double value2 : this.values) {
                final double[] data2 = {
                    value2
                };
                final RealVector v2;
                if (mixed) {
                    v2 = createAlien(data2);
                } else {
                    v2 = create(data2);
                }
                final double expected = data1[0] * data2[0];
                final double actual = v1.dotProduct(v2);
                Assert.assertEquals(data1[0] + " * " + data2[0], expected,
                    actual, 0d);
            }
        }
    }

    private void doTestDotProductDimensionMismatch(final boolean mixed) {
        final double[] data1 = new double[10];
        final double[] data2 = new double[data1.length + 1];
        final RealVector v1 = create(data1);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(data2);
        } else {
            v2 = create(data2);
        }
        v1.dotProduct(v2);
    }

    @Test
    public void testDotProductSameType() {
        doTestDotProductRegularValues(false);
        doTestDotProductSpecialValues(false);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testDotProductDimensionMismatchSameType() {
        doTestDotProductDimensionMismatch(false);
    }

    @Test
    public void testDotProductMixedTypes() {
        doTestDotProductRegularValues(true);
        doTestDotProductSpecialValues(true);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testDotProductDimensionMismatchMixedTypes() {
        doTestDotProductDimensionMismatch(true);
    }

    private void doTestCosine(final boolean mixed) {
        final double x = getPreferredEntryValue();
        final double[] data1 = {
            x, 1d, x, x, 2d, x, x, x, 3d, x, x, x, x
        };
        final double[] data2 = {
            5d, -6d, 7d, x, x, -8d, -9d, 10d, 11d, x, 12d, 13d, -15d
        };
        double norm1 = 0d;
        double norm2 = 0d;
        double dotProduct = 0d;
        for (int i = 0; i < data1.length; i++) {
            norm1 += data1[i] * data1[i];
            norm2 += data2[i] * data2[i];
            dotProduct += data1[i] * data2[i];
        }
        norm1 = MathLib.sqrt(norm1);
        norm2 = MathLib.sqrt(norm2);
        final double expected = dotProduct / (norm1 * norm2);
        final RealVector v1 = create(data1);
        final RealVector v2;
        if (mixed) {
            v2 = createAlien(data2);
        } else {
            v2 = create(data2);
        }
        final double actual = v1.cosine(v2);
        Assert.assertEquals("", expected, actual, 0d);

    }

    @Test
    public void testCosineSameType() {
        doTestCosine(false);
    }

    @Test
    public void testCosineMixedTypes() {
        doTestCosine(true);
    }

    @Test(expected = MathArithmeticException.class)
    public void testCosineLeftNullVector() {
        final RealVector v = create(new double[] { 0, 0, 0 });
        final RealVector w = create(new double[] { 1, 0, 0 });
        v.cosine(w);
    }

    @Test(expected = MathArithmeticException.class)
    public void testCosineRightNullVector() {
        final RealVector v = create(new double[] { 0, 0, 0 });
        final RealVector w = create(new double[] { 1, 0, 0 });
        w.cosine(v);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testCosineDimensionMismatch() {
        final RealVector v = create(new double[] { 1, 2, 3 });
        final RealVector w = create(new double[] { 1, 2, 3, 4 });
        v.cosine(w);
    }

    @Test
    public void testEquals() {
        final RealVector v = create(new double[] { 0, 1, 2 });

        Assert.assertTrue(v.equals(v));
        Assert.assertTrue(v.equals(v.copy()));
        Assert.assertFalse(v.equals(null));
        Assert.assertFalse(v.equals(v.getSubVector(0, v.getDimension() - 1)));
        Assert.assertTrue(v.equals(v.getSubVector(0, v.getDimension())));
    }

    @Test
    public void testSerial() {
        final RealVector v = create(new double[] { 0, 1, 2 });
        Assert.assertEquals(v, TestUtils.serializeAndRecover(v));
    }

    @Test
    public void testMinMax() {
        final RealVector v1 = create(new double[] { 0, -6, 4, 12, 7 });
        Assert.assertEquals(1, v1.getMinIndex());
        Assert.assertEquals(-6, v1.getMinValue(), 1.0e-12);
        Assert.assertEquals(3, v1.getMaxIndex());
        Assert.assertEquals(12, v1.getMaxValue(), 1.0e-12);
        final RealVector v2 = create(new double[] { Double.NaN, 3, Double.NaN, -2 });
        Assert.assertEquals(3, v2.getMinIndex());
        Assert.assertEquals(-2, v2.getMinValue(), 1.0e-12);
        Assert.assertEquals(1, v2.getMaxIndex());
        Assert.assertEquals(3, v2.getMaxValue(), 1.0e-12);
        final RealVector v3 = create(new double[] { Double.NaN, Double.NaN });
        Assert.assertEquals(-1, v3.getMinIndex());
        Assert.assertTrue(Double.isNaN(v3.getMinValue()));
        Assert.assertEquals(-1, v3.getMaxIndex());
        Assert.assertTrue(Double.isNaN(v3.getMaxValue()));
        final RealVector v4 = create(new double[0]);
        Assert.assertEquals(-1, v4.getMinIndex());
        Assert.assertTrue(Double.isNaN(v4.getMinValue()));
        Assert.assertEquals(-1, v4.getMaxIndex());
        Assert.assertTrue(Double.isNaN(v4.getMaxValue()));
    }

    /*
     * TESTS OF THE VISITOR PATTERN
     */

    /** The whole vector is visited. */
    @Test
    public void testWalkInDefaultOrderPreservingVisitor1() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final RealVector v = create(data);
        final RealVectorPreservingVisitor visitor;
        visitor = new RealVectorPreservingVisitor(){

            private int expectedIndex;

            @Override
            public void visit(final int actualIndex, final double actualValue) {
                Assert.assertEquals(this.expectedIndex, actualIndex);
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
                ++this.expectedIndex;
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(0, actualStart);
                Assert.assertEquals(data.length - 1, actualEnd);
                this.expectedIndex = 0;
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        v.walkInDefaultOrder(visitor);
    }

    /** Visiting an invalid subvector. */
    @Test
    public void testWalkInDefaultOrderPreservingVisitor2() {
        final RealVector v = create(new double[5]);
        final RealVectorPreservingVisitor visitor;
        visitor = new RealVectorPreservingVisitor(){

            @Override
            public void visit(final int index, final double value) {
                // Do nothing
            }

            @Override
            public void start(final int dimension, final int start, final int end) {
                // Do nothing
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        try {
            v.walkInDefaultOrder(visitor, -1, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 5, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 0, 5);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 4, 0);
            Assert.fail();
        } catch (final NumberIsTooSmallException e) {
            // Expected behavior
        }
    }

    /** Visiting a valid subvector. */
    @Test
    public void testWalkInDefaultOrderPreservingVisitor3() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final int expectedStart = 2;
        final int expectedEnd = 7;
        final RealVector v = create(data);
        final RealVectorPreservingVisitor visitor;
        visitor = new RealVectorPreservingVisitor(){

            private int expectedIndex;

            @Override
            public void visit(final int actualIndex, final double actualValue) {
                Assert.assertEquals(this.expectedIndex, actualIndex);
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
                ++this.expectedIndex;
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(expectedStart, actualStart);
                Assert.assertEquals(expectedEnd, actualEnd);
                this.expectedIndex = expectedStart;
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        v.walkInDefaultOrder(visitor, expectedStart, expectedEnd);
    }

    /** The whole vector is visited. */
    @Test
    public void testWalkInOptimizedOrderPreservingVisitor1() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final RealVector v = create(data);
        final RealVectorPreservingVisitor visitor;
        visitor = new RealVectorPreservingVisitor(){
            private final boolean[] visited = new boolean[data.length];

            @Override
            public void visit(final int actualIndex, final double actualValue) {
                this.visited[actualIndex] = true;
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(0, actualStart);
                Assert.assertEquals(data.length - 1, actualEnd);
                Arrays.fill(this.visited, false);
            }

            @Override
            public double end() {
                for (int i = 0; i < data.length; i++) {
                    Assert.assertTrue("entry " + i + "has not been visited",
                        this.visited[i]);
                }
                return 0.0;
            }
        };
        v.walkInOptimizedOrder(visitor);
    }

    /** Visiting an invalid subvector. */
    @Test
    public void testWalkInOptimizedOrderPreservingVisitor2() {
        final RealVector v = create(new double[5]);
        final RealVectorPreservingVisitor visitor;
        visitor = new RealVectorPreservingVisitor(){

            @Override
            public void visit(final int index, final double value) {
                // Do nothing
            }

            @Override
            public void start(final int dimension, final int start, final int end) {
                // Do nothing
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        try {
            v.walkInOptimizedOrder(visitor, -1, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 5, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 0, 5);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 4, 0);
            Assert.fail();
        } catch (final NumberIsTooSmallException e) {
            // Expected behavior
        }
    }

    /** Visiting a valid subvector. */
    @Test
    public void testWalkInOptimizedOrderPreservingVisitor3() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final int expectedStart = 2;
        final int expectedEnd = 7;
        final RealVector v = create(data);
        final RealVectorPreservingVisitor visitor;
        visitor = new RealVectorPreservingVisitor(){
            private final boolean[] visited = new boolean[data.length];

            @Override
            public void visit(final int actualIndex, final double actualValue) {
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
                this.visited[actualIndex] = true;
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(expectedStart, actualStart);
                Assert.assertEquals(expectedEnd, actualEnd);
                Arrays.fill(this.visited, true);
            }

            @Override
            public double end() {
                for (int i = expectedStart; i <= expectedEnd; i++) {
                    Assert.assertTrue("entry " + i + "has not been visited",
                        this.visited[i]);
                }
                return 0.0;
            }
        };
        v.walkInOptimizedOrder(visitor, expectedStart, expectedEnd);
    }

    /** The whole vector is visited. */
    @Test
    public void testWalkInDefaultOrderChangingVisitor1() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final RealVector v = create(data);
        final RealVectorChangingVisitor visitor;
        visitor = new RealVectorChangingVisitor(){

            private int expectedIndex;

            @Override
            public double visit(final int actualIndex, final double actualValue) {
                Assert.assertEquals(this.expectedIndex, actualIndex);
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
                ++this.expectedIndex;
                return actualIndex + actualValue;
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(0, actualStart);
                Assert.assertEquals(data.length - 1, actualEnd);
                this.expectedIndex = 0;
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        v.walkInDefaultOrder(visitor);
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals("entry " + i, i + data[i], v.getEntry(i), 0.0);
        }
    }

    /** Visiting an invalid subvector. */
    @Test
    public void testWalkInDefaultOrderChangingVisitor2() {
        final RealVector v = create(new double[5]);
        final RealVectorChangingVisitor visitor;
        visitor = new RealVectorChangingVisitor(){

            @Override
            public double visit(final int index, final double value) {
                return 0.0;
            }

            @Override
            public void start(final int dimension, final int start, final int end) {
                // Do nothing
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        try {
            v.walkInDefaultOrder(visitor, -1, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 5, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 0, 5);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInDefaultOrder(visitor, 4, 0);
            Assert.fail();
        } catch (final NumberIsTooSmallException e) {
            // Expected behavior
        }
    }

    /** Visiting a valid subvector. */
    @Test
    public void testWalkInDefaultOrderChangingVisitor3() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final int expectedStart = 2;
        final int expectedEnd = 7;
        final RealVector v = create(data);
        final RealVectorChangingVisitor visitor;
        visitor = new RealVectorChangingVisitor(){

            private int expectedIndex;

            @Override
            public double visit(final int actualIndex, final double actualValue) {
                Assert.assertEquals(this.expectedIndex, actualIndex);
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
                ++this.expectedIndex;
                return actualIndex + actualValue;
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(expectedStart, actualStart);
                Assert.assertEquals(expectedEnd, actualEnd);
                this.expectedIndex = expectedStart;
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        v.walkInDefaultOrder(visitor, expectedStart, expectedEnd);
        for (int i = expectedStart; i <= expectedEnd; i++) {
            Assert.assertEquals("entry " + i, i + data[i], v.getEntry(i), 0.0);
        }
    }

    /** The whole vector is visited. */
    @Test
    public void testWalkInOptimizedOrderChangingVisitor1() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final RealVector v = create(data);
        final RealVectorChangingVisitor visitor;
        visitor = new RealVectorChangingVisitor(){
            private final boolean[] visited = new boolean[data.length];

            @Override
            public double visit(final int actualIndex, final double actualValue) {
                this.visited[actualIndex] = true;
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
                return actualIndex + actualValue;
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(0, actualStart);
                Assert.assertEquals(data.length - 1, actualEnd);
                Arrays.fill(this.visited, false);
            }

            @Override
            public double end() {
                for (int i = 0; i < data.length; i++) {
                    Assert.assertTrue("entry " + i + "has not been visited",
                        this.visited[i]);
                }
                return 0.0;
            }
        };
        v.walkInOptimizedOrder(visitor);
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals("entry " + i, i + data[i], v.getEntry(i), 0.0);
        }
    }

    /** Visiting an invalid subvector. */
    @Test
    public void testWalkInOptimizedOrderChangingVisitor2() {
        final RealVector v = create(new double[5]);
        final RealVectorChangingVisitor visitor;
        visitor = new RealVectorChangingVisitor(){

            @Override
            public double visit(final int index, final double value) {
                return 0.0;
            }

            @Override
            public void start(final int dimension, final int start, final int end) {
                // Do nothing
            }

            @Override
            public double end() {
                return 0.0;
            }
        };
        try {
            v.walkInOptimizedOrder(visitor, -1, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 5, 4);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 0, -1);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 0, 5);
            Assert.fail();
        } catch (final OutOfRangeException e) {
            // Expected behavior
        }
        try {
            v.walkInOptimizedOrder(visitor, 4, 0);
            Assert.fail();
        } catch (final NumberIsTooSmallException e) {
            // Expected behavior
        }
    }

    /** Visiting a valid subvector. */
    @Test
    public void testWalkInOptimizedOrderChangingVisitor3() {
        final double[] data = new double[] {
            0d, 1d, 0d, 0d, 2d, 0d, 0d, 0d, 3d
        };
        final int expectedStart = 2;
        final int expectedEnd = 7;
        final RealVector v = create(data);
        final RealVectorChangingVisitor visitor;
        visitor = new RealVectorChangingVisitor(){
            private final boolean[] visited = new boolean[data.length];

            @Override
            public double visit(final int actualIndex, final double actualValue) {
                Assert.assertEquals(Integer.toString(actualIndex),
                    data[actualIndex], actualValue, 0d);
                this.visited[actualIndex] = true;
                return actualIndex + actualValue;
            }

            @Override
            public void start(final int actualSize, final int actualStart,
                              final int actualEnd) {
                Assert.assertEquals(data.length, actualSize);
                Assert.assertEquals(expectedStart, actualStart);
                Assert.assertEquals(expectedEnd, actualEnd);
                Arrays.fill(this.visited, true);
            }

            @Override
            public double end() {
                for (int i = expectedStart; i <= expectedEnd; i++) {
                    Assert.assertTrue("entry " + i + "has not been visited",
                        this.visited[i]);
                }
                return 0.0;
            }
        };
        v.walkInOptimizedOrder(visitor, expectedStart, expectedEnd);
        for (int i = expectedStart; i <= expectedEnd; i++) {
            Assert.assertEquals("entry " + i, i + data[i], v.getEntry(i), 0.0);
        }
    }

    /**
     * Minimal implementation of the {@link RealVector} abstract class, for
     * mixed types unit tests.
     */
    public static class RealVectorTestImpl extends RealVector
        implements Serializable {

         /** Serializable UID. */
        private static final long serialVersionUID = 20120706L;

        /** Entries of the vector. */
        protected double data[];

        public RealVectorTestImpl(final double[] d) {
            this.data = d.clone();
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not supported, unneeded for test purposes");
        }

        @Override
        public RealVector copy() {
            return new RealVectorTestImpl(this.data);
        }

        @Override
        public double getEntry(final int index) {
            checkIndex(index);
            return this.data[index];
        }

        @Override
        public int getDimension() {
            return this.data.length;
        }

        @Override
        public RealVector append(final RealVector v) {
            throw unsupported();
        }

        @Override
        public RealVector append(final double d) {
            throw unsupported();
        }

        @Override
        public RealVector getSubVector(final int index, final int n) {
            throw unsupported();
        }

        @Override
        public void setEntry(final int index, final double value) {
            checkIndex(index);
            this.data[index] = value;
        }

        @Override
        public void setSubVector(final int index, final RealVector v) {
            throw unsupported();
        }

        @Override
        public boolean isNaN() {
            throw unsupported();
        }

        @Override
        public boolean isInfinite() {
            throw unsupported();
        }

        @Override
        public RealVector ebeDivide(final RealVector v) throws DimensionMismatchException {
            throw unsupported();
        }

        @Override
        public RealVector ebeMultiply(final RealVector v) throws DimensionMismatchException {
            throw unsupported();
        }
    }
}
