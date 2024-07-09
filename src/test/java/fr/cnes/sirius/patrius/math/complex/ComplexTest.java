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
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.complex;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * @version $Id: ComplexTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ComplexTest {

    private final double inf = Double.POSITIVE_INFINITY;
    private final double neginf = Double.NEGATIVE_INFINITY;
    private final double nan = Double.NaN;
    private final double pi = FastMath.PI;
    private final Complex oneInf = new Complex(1, this.inf);
    private final Complex oneNegInf = new Complex(1, this.neginf);
    private final Complex infOne = new Complex(this.inf, 1);
    private final Complex infZero = new Complex(this.inf, 0);
    private final Complex infNaN = new Complex(this.inf, this.nan);
    private final Complex infNegInf = new Complex(this.inf, this.neginf);
    private final Complex infInf = new Complex(this.inf, this.inf);
    private final Complex negInfInf = new Complex(this.neginf, this.inf);
    private final Complex negInfZero = new Complex(this.neginf, 0);
    private final Complex negInfOne = new Complex(this.neginf, 1);
    private final Complex negInfNaN = new Complex(this.neginf, this.nan);
    private final Complex negInfNegInf = new Complex(this.neginf, this.neginf);
    private final Complex oneNaN = new Complex(1, this.nan);
    private final Complex zeroInf = new Complex(0, this.inf);
    private final Complex zeroNaN = new Complex(0, this.nan);
    private final Complex nanInf = new Complex(this.nan, this.inf);
    private final Complex nanNegInf = new Complex(this.nan, this.neginf);
    private final Complex nanZero = new Complex(this.nan, 0);

    @Test
    public void testConstructor() {
        final Complex z = new Complex(3.0, 4.0);
        Assert.assertEquals(3.0, z.getReal(), 1.0e-5);
        Assert.assertEquals(4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testConstructorNaN() {
        Complex z = new Complex(3.0, Double.NaN);
        Assert.assertTrue(z.isNaN());

        z = new Complex(this.nan, 4.0);
        Assert.assertTrue(z.isNaN());

        z = new Complex(3.0, 4.0);
        Assert.assertFalse(z.isNaN());
    }

    @Test
    public void testAbs() {
        final Complex z = new Complex(3.0, 4.0);
        Assert.assertEquals(5.0, z.abs(), 1.0e-5);
    }

    @Test
    public void testAbsNaN() {
        Assert.assertTrue(Double.isNaN(Complex.NaN.abs()));
        final Complex z = new Complex(this.inf, this.nan);
        Assert.assertTrue(Double.isNaN(z.abs()));
    }

    @Test
    public void testAbsInfinite() {
        Complex z = new Complex(this.inf, 0);
        Assert.assertEquals(this.inf, z.abs(), 0);
        z = new Complex(0, this.neginf);
        Assert.assertEquals(this.inf, z.abs(), 0);
        z = new Complex(this.inf, this.neginf);
        Assert.assertEquals(this.inf, z.abs(), 0);
    }

    @Test
    public void testAdd() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex y = new Complex(5.0, 6.0);
        final Complex z = x.add(y);
        Assert.assertEquals(8.0, z.getReal(), 1.0e-5);
        Assert.assertEquals(10.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testAddNaN() {
        final Complex x = new Complex(3.0, 4.0);
        Complex z = x.add(Complex.NaN);
        Assert.assertSame(Complex.NaN, z);
        z = new Complex(1, this.nan);
        final Complex w = x.add(z);
        Assert.assertSame(Complex.NaN, w);
    }

    @Test
    public void testAddInf() {
        Complex x = new Complex(1, 1);
        final Complex z = new Complex(this.inf, 0);
        final Complex w = x.add(z);
        Assert.assertEquals(w.getImaginary(), 1, 0);
        Assert.assertEquals(this.inf, w.getReal(), 0);

        x = new Complex(this.neginf, 0);
        Assert.assertTrue(Double.isNaN(x.add(z).getReal()));
    }

    @Test
    public void testScalarAdd() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testScalarAddNaN() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testScalarAddInf() {
        Complex x = new Complex(1, 1);
        final double yDouble = Double.POSITIVE_INFINITY;

        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.add(yComplex), x.add(yDouble));

        x = new Complex(this.neginf, 0);
        Assert.assertEquals(x.add(yComplex), x.add(yDouble));
    }

    @Test
    public void testConjugate() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex z = x.conjugate();
        Assert.assertEquals(3.0, z.getReal(), 1.0e-5);
        Assert.assertEquals(-4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testConjugateNaN() {
        final Complex z = Complex.NaN.conjugate();
        Assert.assertTrue(z.isNaN());
    }

    @Test
    public void testConjugateInfiinite() {
        Complex z = new Complex(0, this.inf);
        Assert.assertEquals(this.neginf, z.conjugate().getImaginary(), 0);
        z = new Complex(0, this.neginf);
        Assert.assertEquals(this.inf, z.conjugate().getImaginary(), 0);
    }

    @Test
    public void testDivide() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex y = new Complex(5.0, 6.0);
        final Complex z = x.divide(y);
        Assert.assertEquals(39.0 / 61.0, z.getReal(), 1.0e-5);
        Assert.assertEquals(2.0 / 61.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testDivideReal() {
        final Complex x = new Complex(2d, 3d);
        final Complex y = new Complex(2d, 0d);
        Assert.assertEquals(new Complex(1d, 1.5), x.divide(y));

    }

    @Test
    public void testDivideImaginary() {
        final Complex x = new Complex(2d, 3d);
        final Complex y = new Complex(0d, 2d);
        Assert.assertEquals(new Complex(1.5d, -1d), x.divide(y));
    }

    @Test
    public void testDivideInf() {
        final Complex x = new Complex(3, 4);
        Complex w = new Complex(this.neginf, this.inf);
        Assert.assertTrue(x.divide(w).equals(Complex.ZERO));

        Complex z = w.divide(x);
        Assert.assertTrue(Double.isNaN(z.getReal()));
        Assert.assertEquals(this.inf, z.getImaginary(), 0);

        w = new Complex(this.inf, this.inf);
        z = w.divide(x);
        Assert.assertTrue(Double.isNaN(z.getImaginary()));
        Assert.assertEquals(this.inf, z.getReal(), 0);

        w = new Complex(1, this.inf);
        z = w.divide(w);
        Assert.assertTrue(Double.isNaN(z.getReal()));
        Assert.assertTrue(Double.isNaN(z.getImaginary()));
    }

    @Test
    public void testDivideZero() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex z = x.divide(Complex.ZERO);
        // Assert.assertEquals(z, Complex.INF); // See MATH-657
        Assert.assertEquals(z, Complex.NaN);
    }

    @Test
    public void testDivideZeroZero() {
        final Complex x = new Complex(0.0, 0.0);
        final Complex z = x.divide(Complex.ZERO);
        Assert.assertEquals(z, Complex.NaN);
    }

    @Test
    public void testDivideNaN() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex z = x.divide(Complex.NaN);
        Assert.assertTrue(z.isNaN());
    }

    @Test
    public void testDivideNaNInf() {
        Complex z = this.oneInf.divide(Complex.ONE);
        Assert.assertTrue(Double.isNaN(z.getReal()));
        Assert.assertEquals(this.inf, z.getImaginary(), 0);

        z = this.negInfNegInf.divide(this.oneNaN);
        Assert.assertTrue(Double.isNaN(z.getReal()));
        Assert.assertTrue(Double.isNaN(z.getImaginary()));

        z = this.negInfInf.divide(Complex.ONE);
        Assert.assertTrue(Double.isNaN(z.getReal()));
        Assert.assertTrue(Double.isNaN(z.getImaginary()));
    }

    @Test
    public void testScalarDivide() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.divide(yComplex), x.divide(yDouble));
    }

    @Test
    public void testScalarDivideNaN() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.divide(yComplex), x.divide(yDouble));
    }

    @Test
    public void testScalarDivideInf() {
        Complex x = new Complex(1, 1);
        double yDouble = Double.POSITIVE_INFINITY;
        Complex yComplex = new Complex(yDouble);
        TestUtils.assertEquals(x.divide(yComplex), x.divide(yDouble), 0);

        yDouble = Double.NEGATIVE_INFINITY;
        yComplex = new Complex(yDouble);
        TestUtils.assertEquals(x.divide(yComplex), x.divide(yDouble), 0);

        x = new Complex(1, Double.NEGATIVE_INFINITY);
        TestUtils.assertEquals(x.divide(yComplex), x.divide(yDouble), 0);
    }

    @Test
    public void testScalarDivideZero() {
        final Complex x = new Complex(1, 1);
        TestUtils.assertEquals(x.divide(Complex.ZERO), x.divide(0), 0);
    }

    @Test
    public void testReciprocal() {
        final Complex z = new Complex(5.0, 6.0);
        final Complex act = z.reciprocal();
        final double expRe = 5.0 / 61.0;
        final double expIm = -6.0 / 61.0;
        Assert.assertEquals(expRe, act.getReal(), MathLib.ulp(expRe));
        Assert.assertEquals(expIm, act.getImaginary(), MathLib.ulp(expIm));
    }

    @Test
    public void testReciprocalReal() {
        final Complex z = new Complex(-2.0, 0.0);
        Assert.assertEquals(new Complex(-0.5, 0.0), z.reciprocal());
    }

    @Test
    public void testReciprocalImaginary() {
        final Complex z = new Complex(0.0, -2.0);
        Assert.assertEquals(new Complex(0.0, 0.5), z.reciprocal());
    }

    @Test
    public void testReciprocalInf() {
        Complex z = new Complex(this.neginf, this.inf);
        Assert.assertTrue(z.reciprocal().equals(Complex.ZERO));

        z = new Complex(1, this.inf).reciprocal();
        Assert.assertEquals(z, Complex.ZERO);
    }

    @Test
    public void testReciprocalZero() {
        Assert.assertEquals(Complex.ZERO.reciprocal(), Complex.NaN);
    }

    @Test
    public void testReciprocalNaN() {
        Assert.assertTrue(Complex.NaN.reciprocal().isNaN());
    }

    @Test
    public void testMultiply() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex y = new Complex(5.0, 6.0);
        final Complex z = x.multiply(y);
        Assert.assertEquals(-9.0, z.getReal(), 1.0e-5);
        Assert.assertEquals(38.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testMultiplyNaN() {
        final Complex x = new Complex(3.0, 4.0);
        Complex z = x.multiply(Complex.NaN);
        Assert.assertSame(Complex.NaN, z);
        z = Complex.NaN.multiply(5);
        Assert.assertSame(Complex.NaN, z);
    }

    @Test
    public void testMultiplyInfInf() {
        // Assert.assertTrue(infInf.multiply(infInf).isNaN()); // MATH-620
        Assert.assertTrue(this.infInf.multiply(this.infInf).isInfinite());
    }

    @Test
    public void testMultiplyNaNInf() {
        Complex z = new Complex(1, 1);
        Complex w = z.multiply(this.infOne);
        Assert.assertEquals(w.getReal(), this.inf, 0);
        Assert.assertEquals(w.getImaginary(), this.inf, 0);

        // [MATH-164]
        Assert.assertTrue(new Complex(1, 0).multiply(this.infInf).equals(Complex.INF));
        Assert.assertTrue(new Complex(-1, 0).multiply(this.infInf).equals(Complex.INF));
        Assert.assertTrue(new Complex(1, 0).multiply(this.negInfZero).equals(Complex.INF));

        w = this.oneInf.multiply(this.oneNegInf);
        Assert.assertEquals(w.getReal(), this.inf, 0);
        Assert.assertEquals(w.getImaginary(), this.inf, 0);

        w = this.negInfNegInf.multiply(this.oneNaN);
        Assert.assertTrue(Double.isNaN(w.getReal()));
        Assert.assertTrue(Double.isNaN(w.getImaginary()));

        z = new Complex(1, this.neginf);
        Assert.assertSame(Complex.INF, z.multiply(z));
    }

    @Test
    public void testScalarMultiply() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
        final int zInt = -5;
        final Complex zComplex = new Complex(zInt);
        Assert.assertEquals(x.multiply(zComplex), x.multiply(zInt));
    }

    @Test
    public void testScalarMultiplyNaN() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
    }

    @Test
    public void testScalarMultiplyInf() {
        final Complex x = new Complex(1, 1);
        double yDouble = Double.POSITIVE_INFINITY;
        Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.multiply(yComplex), x.multiply(yDouble));

        yDouble = Double.NEGATIVE_INFINITY;
        yComplex = new Complex(yDouble);
        Assert.assertEquals(x.multiply(yComplex), x.multiply(yDouble));
    }

    @Test
    public void testNegate() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex z = x.negate();
        Assert.assertEquals(-3.0, z.getReal(), 1.0e-5);
        Assert.assertEquals(-4.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testNegateNaN() {
        final Complex z = Complex.NaN.negate();
        Assert.assertTrue(z.isNaN());
    }

    @Test
    public void testSubtract() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex y = new Complex(5.0, 6.0);
        final Complex z = x.subtract(y);
        Assert.assertEquals(-2.0, z.getReal(), 1.0e-5);
        Assert.assertEquals(-2.0, z.getImaginary(), 1.0e-5);
    }

    @Test
    public void testSubtractNaN() {
        final Complex x = new Complex(3.0, 4.0);
        Complex z = x.subtract(Complex.NaN);
        Assert.assertSame(Complex.NaN, z);
        z = new Complex(1, this.nan);
        final Complex w = x.subtract(z);
        Assert.assertSame(Complex.NaN, w);
    }

    @Test
    public void testSubtractInf() {
        Complex x = new Complex(1, 1);
        final Complex z = new Complex(this.neginf, 0);
        final Complex w = x.subtract(z);
        Assert.assertEquals(w.getImaginary(), 1, 0);
        Assert.assertEquals(this.inf, w.getReal(), 0);

        x = new Complex(this.neginf, 0);
        Assert.assertTrue(Double.isNaN(x.subtract(z).getReal()));
    }

    @Test
    public void testScalarSubtract() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = 2.0;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testScalarSubtractNaN() {
        final Complex x = new Complex(3.0, 4.0);
        final double yDouble = Double.NaN;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testScalarSubtractInf() {
        Complex x = new Complex(1, 1);
        final double yDouble = Double.POSITIVE_INFINITY;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.subtract(yComplex), x.subtract(yDouble));

        x = new Complex(this.neginf, 0);
        Assert.assertEquals(x.subtract(yComplex), x.subtract(yDouble));
    }

    @Test
    public void testEqualsNull() {
        final Complex x = new Complex(3.0, 4.0);
        Assert.assertFalse(x.equals(null));
    }

    @Test
    public void testEqualsClass() {
        final Complex x = new Complex(3.0, 4.0);
        Assert.assertFalse(x.equals(this));
    }

    @Test
    public void testEqualsSame() {
        final Complex x = new Complex(3.0, 4.0);
        Assert.assertTrue(x.equals(x));
    }

    @Test
    public void testEqualsTrue() {
        final Complex x = new Complex(3.0, 4.0);
        final Complex y = new Complex(3.0, 4.0);
        Assert.assertTrue(x.equals(y));
    }

    @Test
    public void testEqualsRealDifference() {
        final Complex x = new Complex(0.0, 0.0);
        final Complex y = new Complex(0.0 + Double.MIN_VALUE, 0.0);
        Assert.assertFalse(x.equals(y));
    }

    @Test
    public void testEqualsImaginaryDifference() {
        final Complex x = new Complex(0.0, 0.0);
        final Complex y = new Complex(0.0, 0.0 + Double.MIN_VALUE);
        Assert.assertFalse(x.equals(y));
    }

    @Test
    public void testEqualsNaN() {
        final Complex realNaN = new Complex(Double.NaN, 0.0);
        final Complex imaginaryNaN = new Complex(0.0, Double.NaN);
        final Complex complexNaN = Complex.NaN;
        Assert.assertTrue(realNaN.equals(imaginaryNaN));
        Assert.assertTrue(imaginaryNaN.equals(complexNaN));
        Assert.assertTrue(realNaN.equals(complexNaN));
    }

    @Test
    public void testHashCode() {
        final Complex x = new Complex(0.0, 0.0);
        Complex y = new Complex(0.0, 0.0 + Double.MIN_VALUE);
        Assert.assertFalse(x.hashCode() == y.hashCode());
        y = new Complex(0.0 + Double.MIN_VALUE, 0.0);
        Assert.assertFalse(x.hashCode() == y.hashCode());
        final Complex realNaN = new Complex(Double.NaN, 0.0);
        final Complex imaginaryNaN = new Complex(0.0, Double.NaN);
        Assert.assertEquals(realNaN.hashCode(), imaginaryNaN.hashCode());
        Assert.assertEquals(imaginaryNaN.hashCode(), Complex.NaN.hashCode());
    }

    @Test
    public void testAcos() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(0.936812, -2.30551);
        TestUtils.assertEquals(expected, z.acos(), 1.0e-5);
        TestUtils.assertEquals(new Complex(MathLib.acos(0), 0),
            Complex.ZERO.acos(), 1.0e-12);
    }

    @Test
    public void testAcosInf() {
        TestUtils.assertSame(Complex.NaN, this.oneInf.acos());
        TestUtils.assertSame(Complex.NaN, this.oneNegInf.acos());
        TestUtils.assertSame(Complex.NaN, this.infOne.acos());
        TestUtils.assertSame(Complex.NaN, this.negInfOne.acos());
        TestUtils.assertSame(Complex.NaN, this.infInf.acos());
        TestUtils.assertSame(Complex.NaN, this.infNegInf.acos());
        TestUtils.assertSame(Complex.NaN, this.negInfInf.acos());
        TestUtils.assertSame(Complex.NaN, this.negInfNegInf.acos());
    }

    @Test
    public void testAcosNaN() {
        Assert.assertTrue(Complex.NaN.acos().isNaN());
    }

    @Test
    public void testAsin() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(0.633984, 2.30551);
        TestUtils.assertEquals(expected, z.asin(), 1.0e-5);
    }

    @Test
    public void testAsinNaN() {
        Assert.assertTrue(Complex.NaN.asin().isNaN());
    }

    @Test
    public void testAsinInf() {
        TestUtils.assertSame(Complex.NaN, this.oneInf.asin());
        TestUtils.assertSame(Complex.NaN, this.oneNegInf.asin());
        TestUtils.assertSame(Complex.NaN, this.infOne.asin());
        TestUtils.assertSame(Complex.NaN, this.negInfOne.asin());
        TestUtils.assertSame(Complex.NaN, this.infInf.asin());
        TestUtils.assertSame(Complex.NaN, this.infNegInf.asin());
        TestUtils.assertSame(Complex.NaN, this.negInfInf.asin());
        TestUtils.assertSame(Complex.NaN, this.negInfNegInf.asin());
    }

    @Test
    public void testAtan() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(1.44831, 0.158997);
        TestUtils.assertEquals(expected, z.atan(), 1.0e-5);
    }

    @Test
    public void testAtanInf() {
        TestUtils.assertSame(Complex.NaN, this.oneInf.atan());
        TestUtils.assertSame(Complex.NaN, this.oneNegInf.atan());
        TestUtils.assertSame(Complex.NaN, this.infOne.atan());
        TestUtils.assertSame(Complex.NaN, this.negInfOne.atan());
        TestUtils.assertSame(Complex.NaN, this.infInf.atan());
        TestUtils.assertSame(Complex.NaN, this.infNegInf.atan());
        TestUtils.assertSame(Complex.NaN, this.negInfInf.atan());
        TestUtils.assertSame(Complex.NaN, this.negInfNegInf.atan());
    }

    @Test
    public void testAtanI() {
        Assert.assertTrue(Complex.I.atan().isNaN());
    }

    @Test
    public void testAtanNaN() {
        Assert.assertTrue(Complex.NaN.atan().isNaN());
    }

    @Test
    public void testCos() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(-27.03495, -3.851153);
        TestUtils.assertEquals(expected, z.cos(), 1.0e-5);
    }

    @Test
    public void testCosNaN() {
        Assert.assertTrue(Complex.NaN.cos().isNaN());
    }

    @Test
    public void testCosInf() {
        TestUtils.assertSame(this.infNegInf, this.oneInf.cos());
        TestUtils.assertSame(this.infInf, this.oneNegInf.cos());
    }

    @Test
    public void testCosh() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(-6.58066, -7.58155);
        TestUtils.assertEquals(expected, z.cosh(), 1.0e-5);
    }

    @Test
    public void testCoshNaN() {
        Assert.assertTrue(Complex.NaN.cosh().isNaN());
    }

    @Test
    public void testCoshInf() {
        TestUtils.assertSame(this.infInf, this.infOne.cosh());
        TestUtils.assertSame(this.infNegInf, this.negInfOne.cosh());
    }

    @Test
    public void testExp() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(-13.12878, -15.20078);
        TestUtils.assertEquals(expected, z.exp(), 1.0e-5);
        TestUtils.assertEquals(Complex.ONE,
            Complex.ZERO.exp(), 10e-12);
        final Complex iPi = Complex.I.multiply(new Complex(this.pi, 0));
        TestUtils.assertEquals(Complex.ONE.negate(),
            iPi.exp(), 10e-12);
    }

    @Test
    public void testExpNaN() {
        Assert.assertTrue(Complex.NaN.exp().isNaN());
    }

    @Test
    public void testExpInf() {
        TestUtils.assertSame(this.infInf, this.infOne.exp());
        TestUtils.assertSame(Complex.ZERO, this.negInfOne.exp());
    }

    @Test
    public void testLog() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(1.60944, 0.927295);
        TestUtils.assertEquals(expected, z.log(), 1.0e-5);
    }

    @Test
    public void testLogNaN() {
        Assert.assertTrue(Complex.NaN.log().isNaN());
    }

    @Test
    public void testLogInf() {
        TestUtils.assertEquals(new Complex(this.inf, this.pi / 2),
            this.oneInf.log(), 10e-12);
        TestUtils.assertEquals(new Complex(this.inf, -this.pi / 2),
            this.oneNegInf.log(), 10e-12);
        TestUtils.assertEquals(this.infZero, this.infOne.log(), 10e-12);
        TestUtils.assertEquals(new Complex(this.inf, this.pi),
            this.negInfOne.log(), 10e-12);
        TestUtils.assertEquals(new Complex(this.inf, this.pi / 4),
            this.infInf.log(), 10e-12);
        TestUtils.assertEquals(new Complex(this.inf, -this.pi / 4),
            this.infNegInf.log(), 10e-12);
        TestUtils.assertEquals(new Complex(this.inf, 3d * this.pi / 4),
            this.negInfInf.log(), 10e-12);
        TestUtils.assertEquals(new Complex(this.inf, -3d * this.pi / 4),
            this.negInfNegInf.log(), 10e-12);
    }

    @Test
    public void testLogZero() {
        TestUtils.assertSame(this.negInfZero, Complex.ZERO.log());
    }

    @Test
    public void testPow() {
        final Complex x = new Complex(3, 4);
        final Complex y = new Complex(5, 6);
        final Complex expected = new Complex(-1.860893, 11.83677);
        TestUtils.assertEquals(expected, x.pow(y), 1.0e-5);
    }

    @Test
    public void testPowNaNBase() {
        final Complex x = new Complex(3, 4);
        Assert.assertTrue(Complex.NaN.pow(x).isNaN());
    }

    @Test
    public void testPowNaNExponent() {
        final Complex x = new Complex(3, 4);
        Assert.assertTrue(x.pow(Complex.NaN).isNaN());
    }

    @Test
    public void testPowZero() {
        TestUtils.assertEquals(Complex.ONE,
            Complex.ONE.pow(Complex.ZERO), 10e-12);
        TestUtils.assertEquals(Complex.ONE,
            Complex.I.pow(Complex.ZERO), 10e-12);
        TestUtils.assertEquals(Complex.ONE,
            new Complex(-1, 3).pow(Complex.ZERO), 10e-12);
    }

    @Test
    public void testScalarPow() {
        final Complex x = new Complex(3, 4);
        final double yDouble = 5.0;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testScalarPowNaNBase() {
        final Complex x = Complex.NaN;
        final double yDouble = 5.0;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testScalarPowNaNExponent() {
        final Complex x = new Complex(3, 4);
        final double yDouble = Double.NaN;
        final Complex yComplex = new Complex(yDouble);
        Assert.assertEquals(x.pow(yComplex), x.pow(yDouble));
    }

    @Test
    public void testScalarPowZero() {
        TestUtils.assertEquals(Complex.ONE, Complex.ONE.pow(0.0), 10e-12);
        TestUtils.assertEquals(Complex.ONE, Complex.I.pow(0.0), 10e-12);
        TestUtils.assertEquals(Complex.ONE, new Complex(-1, 3).pow(0.0), 10e-12);
    }

    @Test(expected = NullArgumentException.class)
    public void testpowNull() {
        Complex.ONE.pow(null);
    }

    @Test
    public void testSin() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(3.853738, -27.01681);
        TestUtils.assertEquals(expected, z.sin(), 1.0e-5);
    }

    @Test
    public void testSinInf() {
        TestUtils.assertSame(this.infInf, this.oneInf.sin());
        TestUtils.assertSame(this.infNegInf, this.oneNegInf.sin());
    }

    @Test
    public void testSinNaN() {
        Assert.assertTrue(Complex.NaN.sin().isNaN());
    }

    @Test
    public void testSinh() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(-6.54812, -7.61923);
        TestUtils.assertEquals(expected, z.sinh(), 1.0e-5);
    }

    @Test
    public void testSinhNaN() {
        Assert.assertTrue(Complex.NaN.sinh().isNaN());
    }

    @Test
    public void testSinhInf() {
        TestUtils.assertSame(this.infInf, this.infOne.sinh());
        TestUtils.assertSame(this.negInfInf, this.negInfOne.sinh());
    }

    @Test
    public void testSqrtRealPositive() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(2, 1);
        TestUtils.assertEquals(expected, z.sqrt(), 1.0e-5);
    }

    @Test
    public void testSqrtRealZero() {
        final Complex z = new Complex(0.0, 4);
        final Complex expected = new Complex(1.41421, 1.41421);
        TestUtils.assertEquals(expected, z.sqrt(), 1.0e-5);
    }

    @Test
    public void testSqrtRealNegative() {
        final Complex z = new Complex(-3.0, 4);
        final Complex expected = new Complex(1, 2);
        TestUtils.assertEquals(expected, z.sqrt(), 1.0e-5);
    }

    @Test
    public void testSqrtImaginaryZero() {
        final Complex z = new Complex(-3.0, 0.0);
        final Complex expected = new Complex(0.0, 1.73205);
        TestUtils.assertEquals(expected, z.sqrt(), 1.0e-5);
    }

    @Test
    public void testSqrtImaginaryNegative() {
        final Complex z = new Complex(-3.0, -4.0);
        final Complex expected = new Complex(1.0, -2.0);
        TestUtils.assertEquals(expected, z.sqrt(), 1.0e-5);
    }

    @Test
    public void testSqrtPolar() {
        double r = 1;
        for (int i = 0; i < 5; i++) {
            r += i;
            double theta = 0;
            for (int j = 0; j < 11; j++) {
                theta += this.pi / 12;
                final Complex z = ComplexUtils.polar2Complex(r, theta);
                final Complex sqrtz = ComplexUtils.polar2Complex(MathLib.sqrt(r), theta / 2);
                TestUtils.assertEquals(sqrtz, z.sqrt(), 10e-12);
            }
        }
    }

    @Test
    public void testSqrtNaN() {
        Assert.assertTrue(Complex.NaN.sqrt().isNaN());
    }

    @Test
    public void testSqrtInf() {
        TestUtils.assertSame(this.infNaN, this.oneInf.sqrt());
        TestUtils.assertSame(this.infNaN, this.oneNegInf.sqrt());
        TestUtils.assertSame(this.infZero, this.infOne.sqrt());
        TestUtils.assertSame(this.zeroInf, this.negInfOne.sqrt());
        TestUtils.assertSame(this.infNaN, this.infInf.sqrt());
        TestUtils.assertSame(this.infNaN, this.infNegInf.sqrt());
        TestUtils.assertSame(this.nanInf, this.negInfInf.sqrt());
        TestUtils.assertSame(this.nanNegInf, this.negInfNegInf.sqrt());
    }

    @Test
    public void testSqrt1z() {
        final Complex z = new Complex(3, 4);
        final Complex expected = new Complex(4.08033, -2.94094);
        TestUtils.assertEquals(expected, z.sqrt1z(), 1.0e-5);
    }

    @Test
    public void testSqrt1zNaN() {
        Assert.assertTrue(Complex.NaN.sqrt1z().isNaN());
    }

    @Test
    public void testTan() {
        final Complex z = new Complex(3, 4);
        Complex expected = new Complex(-0.000187346, 0.999356);
        TestUtils.assertEquals(expected, z.tan(), 1.0e-5);
        /* Check that no overflow occurs (MATH-722) */
        Complex actual = new Complex(3.0, 1E10).tan();
        expected = new Complex(0, 1);
        TestUtils.assertEquals(expected, actual, 1.0e-5);
        actual = new Complex(3.0, -1E10).tan();
        expected = new Complex(0, -1);
        TestUtils.assertEquals(expected, actual, 1.0e-5);
    }

    @Test
    public void testTanNaN() {
        Assert.assertTrue(Complex.NaN.tan().isNaN());
    }

    @Test
    public void testTanInf() {
        TestUtils.assertSame(Complex.valueOf(0.0, 1.0), this.oneInf.tan());
        TestUtils.assertSame(Complex.valueOf(0.0, -1.0), this.oneNegInf.tan());
        TestUtils.assertSame(Complex.NaN, this.infOne.tan());
        TestUtils.assertSame(Complex.NaN, this.negInfOne.tan());
        TestUtils.assertSame(Complex.NaN, this.infInf.tan());
        TestUtils.assertSame(Complex.NaN, this.infNegInf.tan());
        TestUtils.assertSame(Complex.NaN, this.negInfInf.tan());
        TestUtils.assertSame(Complex.NaN, this.negInfNegInf.tan());
    }

    @Test
    public void testTanCritical() {
        TestUtils.assertSame(this.infNaN, new Complex(this.pi / 2, 0).tan());
        TestUtils.assertSame(this.negInfNaN, new Complex(-this.pi / 2, 0).tan());
    }

    @Test
    public void testTanh() {
        final Complex z = new Complex(3, 4);
        Complex expected = new Complex(1.00071, 0.00490826);
        TestUtils.assertEquals(expected, z.tanh(), 1.0e-5);
        /* Check that no overflow occurs (MATH-722) */
        Complex actual = new Complex(1E10, 3.0).tanh();
        expected = new Complex(1, 0);
        TestUtils.assertEquals(expected, actual, 1.0e-5);
        actual = new Complex(-1E10, 3.0).tanh();
        expected = new Complex(-1, 0);
        TestUtils.assertEquals(expected, actual, 1.0e-5);
    }

    @Test
    public void testTanhNaN() {
        Assert.assertTrue(Complex.NaN.tanh().isNaN());
    }

    @Test
    public void testTanhInf() {
        TestUtils.assertSame(Complex.NaN, this.oneInf.tanh());
        TestUtils.assertSame(Complex.NaN, this.oneNegInf.tanh());
        TestUtils.assertSame(Complex.valueOf(1.0, 0.0), this.infOne.tanh());
        TestUtils.assertSame(Complex.valueOf(-1.0, 0.0), this.negInfOne.tanh());
        TestUtils.assertSame(Complex.NaN, this.infInf.tanh());
        TestUtils.assertSame(Complex.NaN, this.infNegInf.tanh());
        TestUtils.assertSame(Complex.NaN, this.negInfInf.tanh());
        TestUtils.assertSame(Complex.NaN, this.negInfNegInf.tanh());
    }

    @Test
    public void testTanhCritical() {
        TestUtils.assertSame(this.nanInf, new Complex(0, this.pi / 2).tanh());
    }

    /** test issue MATH-221 */
    @Test
    public void testMath221() {
        Assert.assertEquals(new Complex(0, -1), new Complex(0, 1).multiply(new Complex(-1, 0)));
    }

    /**
     * Test: computing <b>third roots</b> of z.
     * 
     * <pre>
     * <code>
     * <b>z = -2 + 2 * i</b>
     *   => z_0 =  1      +          i
     *   => z_1 = -1.3660 + 0.3660 * i
     *   => z_2 =  0.3660 - 1.3660 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRoot_normal_thirdRoot() {
        // The complex number we want to compute all third-roots for.
        final Complex z = new Complex(-2, 2);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assert.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assert.assertEquals(1.0, thirdRootsOfZ[0].getReal(), 1.0e-5);
        Assert.assertEquals(1.0, thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assert.assertEquals(-1.3660254037844386, thirdRootsOfZ[1].getReal(), 1.0e-5);
        Assert.assertEquals(0.36602540378443843, thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assert.assertEquals(0.366025403784439, thirdRootsOfZ[2].getReal(), 1.0e-5);
        Assert.assertEquals(-1.3660254037844384, thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>fourth roots</b> of z.
     * 
     * <pre>
     * <code>
     * <b>z = 5 - 2 * i</b>
     *   => z_0 =  1.5164 - 0.1446 * i
     *   => z_1 =  0.1446 + 1.5164 * i
     *   => z_2 = -1.5164 + 0.1446 * i
     *   => z_3 = -1.5164 - 0.1446 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRoot_normal_fourthRoot() {
        // The complex number we want to compute all third-roots for.
        final Complex z = new Complex(5, -2);
        // The List holding all fourth roots
        final Complex[] fourthRootsOfZ = z.nthRoot(4).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assert.assertEquals(4, fourthRootsOfZ.length);
        // test z_0
        Assert.assertEquals(1.5164629308487783, fourthRootsOfZ[0].getReal(), 1.0e-5);
        Assert.assertEquals(-0.14469266210702247, fourthRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assert.assertEquals(0.14469266210702256, fourthRootsOfZ[1].getReal(), 1.0e-5);
        Assert.assertEquals(1.5164629308487783, fourthRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assert.assertEquals(-1.5164629308487783, fourthRootsOfZ[2].getReal(), 1.0e-5);
        Assert.assertEquals(0.14469266210702267, fourthRootsOfZ[2].getImaginary(), 1.0e-5);
        // test z_3
        Assert.assertEquals(-0.14469266210702275, fourthRootsOfZ[3].getReal(), 1.0e-5);
        Assert.assertEquals(-1.5164629308487783, fourthRootsOfZ[3].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>third roots</b> of z.
     * 
     * <pre>
     * <code>
     * <b>z = 8</b>
     *   => z_0 =  2
     *   => z_1 = -1 + 1.73205 * i
     *   => z_2 = -1 - 1.73205 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRoot_cornercase_thirdRoot_imaginaryPartEmpty() {
        // The number 8 has three third roots. One we all already know is the number 2.
        // But there are two more complex roots.
        final Complex z = new Complex(8, 0);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assert.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assert.assertEquals(2.0, thirdRootsOfZ[0].getReal(), 1.0e-5);
        Assert.assertEquals(0.0, thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assert.assertEquals(-1.0, thirdRootsOfZ[1].getReal(), 1.0e-5);
        Assert.assertEquals(1.7320508075688774, thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assert.assertEquals(-1.0, thirdRootsOfZ[2].getReal(), 1.0e-5);
        Assert.assertEquals(-1.732050807568877, thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test: computing <b>third roots</b> of z with real part 0.
     * 
     * <pre>
     * <code>
     * <b>z = 2 * i</b>
     *   => z_0 =  1.0911 + 0.6299 * i
     *   => z_1 = -1.0911 + 0.6299 * i
     *   => z_2 = -2.3144 - 1.2599 * i
     * </code>
     * </pre>
     */
    @Test
    public void testNthRoot_cornercase_thirdRoot_realPartZero() {
        // complex number with only imaginary part
        final Complex z = new Complex(0, 2);
        // The List holding all third roots
        final Complex[] thirdRootsOfZ = z.nthRoot(3).toArray(new Complex[0]);
        // Returned Collection must not be empty!
        Assert.assertEquals(3, thirdRootsOfZ.length);
        // test z_0
        Assert.assertEquals(1.0911236359717216, thirdRootsOfZ[0].getReal(), 1.0e-5);
        Assert.assertEquals(0.6299605249474365, thirdRootsOfZ[0].getImaginary(), 1.0e-5);
        // test z_1
        Assert.assertEquals(-1.0911236359717216, thirdRootsOfZ[1].getReal(), 1.0e-5);
        Assert.assertEquals(0.6299605249474365, thirdRootsOfZ[1].getImaginary(), 1.0e-5);
        // test z_2
        Assert.assertEquals(-2.3144374213981936E-16, thirdRootsOfZ[2].getReal(), 1.0e-5);
        Assert.assertEquals(-1.2599210498948732, thirdRootsOfZ[2].getImaginary(), 1.0e-5);
    }

    /**
     * Test cornercases with NaN and Infinity.
     */
    @Test
    public void testNthRoot_cornercase_NAN_Inf() {
        // NaN + finite -> NaN
        List<Complex> roots = this.oneNaN.nthRoot(3);
        Assert.assertEquals(1, roots.size());
        Assert.assertEquals(Complex.NaN, roots.get(0));

        roots = this.nanZero.nthRoot(3);
        Assert.assertEquals(1, roots.size());
        Assert.assertEquals(Complex.NaN, roots.get(0));

        // NaN + infinite -> NaN
        roots = this.nanInf.nthRoot(3);
        Assert.assertEquals(1, roots.size());
        Assert.assertEquals(Complex.NaN, roots.get(0));

        // finite + infinite -> Inf
        roots = this.oneInf.nthRoot(3);
        Assert.assertEquals(1, roots.size());
        Assert.assertEquals(Complex.INF, roots.get(0));

        // infinite + infinite -> Inf
        roots = this.negInfInf.nthRoot(3);
        Assert.assertEquals(1, roots.size());
        Assert.assertEquals(Complex.INF, roots.get(0));
    }

    /**
     * Test standard values
     */
    @Test
    public void testGetArgument() {
        Complex z = new Complex(1, 0);
        Assert.assertEquals(0.0, z.getArgument(), 1.0e-12);

        z = new Complex(1, 1);
        Assert.assertEquals(FastMath.PI / 4, z.getArgument(), 1.0e-12);

        z = new Complex(0, 1);
        Assert.assertEquals(FastMath.PI / 2, z.getArgument(), 1.0e-12);

        z = new Complex(-1, 1);
        Assert.assertEquals(3 * FastMath.PI / 4, z.getArgument(), 1.0e-12);

        z = new Complex(-1, 0);
        Assert.assertEquals(FastMath.PI, z.getArgument(), 1.0e-12);

        z = new Complex(-1, -1);
        Assert.assertEquals(-3 * FastMath.PI / 4, z.getArgument(), 1.0e-12);

        z = new Complex(0, -1);
        Assert.assertEquals(-FastMath.PI / 2, z.getArgument(), 1.0e-12);

        z = new Complex(1, -1);
        Assert.assertEquals(-FastMath.PI / 4, z.getArgument(), 1.0e-12);

    }

    /**
     * Verify atan2-style handling of infinite parts
     */
    @Test
    public void testGetArgumentInf() {
        Assert.assertEquals(FastMath.PI / 4, this.infInf.getArgument(), 1.0e-12);
        Assert.assertEquals(FastMath.PI / 2, this.oneInf.getArgument(), 1.0e-12);
        Assert.assertEquals(0.0, this.infOne.getArgument(), 1.0e-12);
        Assert.assertEquals(FastMath.PI / 2, this.zeroInf.getArgument(), 1.0e-12);
        Assert.assertEquals(0.0, this.infZero.getArgument(), 1.0e-12);
        Assert.assertEquals(FastMath.PI, this.negInfOne.getArgument(), 1.0e-12);
        Assert.assertEquals(-3.0 * FastMath.PI / 4, this.negInfNegInf.getArgument(), 1.0e-12);
        Assert.assertEquals(-FastMath.PI / 2, this.oneNegInf.getArgument(), 1.0e-12);
    }

    @Test
    public void testSerial() {
        final Complex z = new Complex(3.0, 4.0);
        Assert.assertEquals(z, TestUtils.serializeAndRecover(z));
        final Complex ncmplx = (Complex) TestUtils.serializeAndRecover(this.oneNaN);
        Assert.assertEquals(this.nanZero, ncmplx);
        Assert.assertTrue(ncmplx.isNaN());
        final Complex infcmplx = (Complex) TestUtils.serializeAndRecover(this.infInf);
        Assert.assertEquals(this.infInf, infcmplx);
        Assert.assertTrue(infcmplx.isInfinite());
        final TestComplex tz = new TestComplex(3.0, 4.0);
        Assert.assertEquals(tz, TestUtils.serializeAndRecover(tz));
        final TestComplex ntcmplx = (TestComplex) TestUtils.serializeAndRecover(new TestComplex(this.oneNaN));
        Assert.assertEquals(this.nanZero, ntcmplx);
        Assert.assertTrue(ntcmplx.isNaN());
        final TestComplex inftcmplx = (TestComplex) TestUtils.serializeAndRecover(new TestComplex(this.infInf));
        Assert.assertEquals(this.infInf, inftcmplx);
        Assert.assertTrue(inftcmplx.isInfinite());
    }

    /**
     * Class to test extending Complex
     */
    public static class TestComplex extends Complex {

        /**
         * Serialization identifier.
         */
        private static final long serialVersionUID = 3268726724160389237L;

        public TestComplex(final double real, final double imaginary) {
            super(real, imaginary);
        }

        public TestComplex(final Complex other) {
            this(other.getReal(), other.getImaginary());
        }

        @Override
        protected TestComplex createComplex(final double real, final double imaginary) {
            return new TestComplex(real, imaginary);
        }

    }

    /**
     * Tests the method value of. Coverage purpose.
     * 
     * @since 2.3
     */
    @Test
    public void testValueOf() {

        final Complex NaN = new Complex(Double.NaN, Double.NaN);
        final Complex z = new Complex(1, 0);
        Assert.assertEquals(NaN, Complex.valueOf(Double.NaN));

        final Complex z2 = new Complex(-FastMath.PI / 4, 0);
        Assert.assertEquals(z2, Complex.valueOf(-FastMath.PI / 4));

    }

    /**
     * Coverage purposes only.
     * 
     * @since 2.3
     */
    @Test
    public void coverageIfs() {

        // tests the if (Double.isInfinite(real) || Double.isInfinite(imaginary)) of multiply()
        final Double INF_REAL = Double.POSITIVE_INFINITY;
        final Complex INF = new Complex(INF_REAL, INF_REAL);
        final Complex z = INF;
        Assert.assertEquals(INF, z.multiply(2));
        // tests the if (n <= 0) of nthRoot(int n)
        try {
            z.nthRoot(-1);
        } catch (final Exception e) {
            // expected = NotPositiveException
        }
        // tests the if (Double.isNaN(realPart) || Double.isNaN(imaginaryPart)) of valueOf(double, double)
        final Complex NaN = new Complex(Double.NaN, Double.NaN);
        Assert.assertEquals(NaN, Complex.valueOf(Double.NaN, Double.NaN));
    }
}
