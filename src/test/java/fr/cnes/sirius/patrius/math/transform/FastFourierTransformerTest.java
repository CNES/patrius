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
 */
/*
 * Odd powers FFT :
 * Copyright (c) 2014 Nayuki Minase
 * http://nayuki.eigenstate.org/page/free-small-fft-in-multiple-languages
 *
 * (MIT License)
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:285:21/08/2014: FFT adapted to all orders
 * VERSION::FA:375:27/11/2014: copy of input data in method transform
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.transform;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.function.Sinc;
import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test case for fast Fourier transformer.
 * <p>
 * FFT algorithm is exact, the small tolerance number is used only to account for round-off errors.
 * 
 * @version $Id: FastFourierTransformerTest.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 * @since 2.3
 */

public final class FastFourierTransformerTest {
    /** The common seed of all random number generators used in this test. */
    private static final long SEED = 20110111L;
    /** Error message */
    private static final String LENGTH_ERROR = "Mismatched lengths";

    /*
     * Precondition checks.
     */

    /**
     * Tests if the powerOfTwo algorithm throws an exception if the order is not a power of two
     */
    @Test(expected = MathIllegalArgumentException.class)
    public void testTransformComplexSizeNotAPowerOfTwo() {
        final int n = 127;
        final Complex[] x = createComplexData(n);
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FFTpowerOfTwoOrder fft;
                fft = new FFTpowerOfTwoOrder(element);
                fft.transform(x, element2); // Expected behaviour : throws exception
            }
        }
    }

    /**
     * Tests if the odd algorithm is ok with power of two orders.
     */
    @Test
    public void testTransformComplexSizeNotAPowerOfTwoForBluesteinAlgo() {
        final int n = 127;
        final Complex[] x = createComplexData(n);

        // tests that the initial tab is not modified during algorithm
        final Complex[] xInitial = x.clone();

        // algorithm
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FFToddOrder fft;
                fft = new FFToddOrder(element);
                fft.transform(x, element2); // Expected behaviour : runs correctly
            }
        }
        // x should not be modified by algorithm
        Assert.assertArrayEquals(xInitial, x);
    }

    /**
     * Tests if the FastFourierTransformer algorithm deals correctly with the algorithms depending on the order
     */
    @Test
    public void testTransformComplexSizeNotAPowerOfTwoForFastFourierTransformer() {
        final int n = 127;
        final Complex[] x = createComplexData(n);

        // tests that the initial tab is not modified during algorithm
        final Complex[] xInitial = x.clone();

        // algorithm
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FastFourierTransformer fft;
                fft = new FastFourierTransformer(element);
                fft.transform(x, element2); // Expected behaviour : runs correctly
            }
        }
        // x should not be modified by algorithm
        Assert.assertArrayEquals(xInitial, x);
    }

    /**
     * 3 tests in the same way as the previous 3-uplets
     * Tests if the odd algorithm is ok with power of two orders
     */
    @Test(expected = MathIllegalArgumentException.class)
    public void testTransformRealSizeNotAPowerOfTwo() {
        final int n = 127;
        final double[] x = createRealData(n);
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FFTpowerOfTwoOrder fft;
                fft = new FFTpowerOfTwoOrder(element);
                fft.transform(x, element2); // Expected behaviour : throws exception
            }
        }
    }

    /**
     * Tests if the odd algorithm is ok with power of two orders
     */
    @Test
    public void testTransformRealSizeNotAPowerOfTwoForBluesteinAlgo() {
        final int n = 127;
        final double[] x = createRealData(n);

        // tests that the initial tab is not modified during algorithm
        final double[] xInitial = x.clone();

        // algorithm
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FFToddOrder fft;
                fft = new FFToddOrder(element);
                fft.transform(x, element2); // Expected behaviour : runs correctly
            }
        }
        // x should not be modified by algorithm
        Assert.assertArrayEquals(xInitial, x, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Tests if the FastFourierTransformer algorithm deals correctly with the algorithms depending on the order
     */
    public void testTransformRealSizeNotAPowerOfTwoForFastFourierTransformer() {
        final int n = 127;
        final double[] x = createRealData(n);

        // tests that the initial tab is not modified during algorithm
        final double[] xInitial = x.clone();

        // algorithm
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FastFourierTransformer fft;
                fft = new FastFourierTransformer(element);
                fft.transform(x, element2); // Expected behaviour : runs correctly
            }
        }
        // x should not be modified by algorithm
        Assert.assertArrayEquals(xInitial, x, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * 3 tests in the same way as the previous 3-uplets
     * Tests if the odd algorithm is ok with power of two orders
     */
    @Test(expected = MathIllegalArgumentException.class)
    public void testTransformFunctionSizeNotAPowerOfTwo() {
        final int n = 127;
        final UnivariateFunction f = new Sin();
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FFTpowerOfTwoOrder fft;
                fft = new FFTpowerOfTwoOrder(element);
                fft.transform(f, 0.0, Math.PI, n, element2); // Expected behaviour : throws exception
            }
        }
    }

    /**
     * Tests if the odd algorithm is ok with power of two orders
     */
    @Test
    public void testTransformFunctionSizeNotAPowerOfTwoForBluesteinAlgo() {
        final int n = 127;
        final UnivariateFunction f = new Sin();
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FFToddOrder fft;
                fft = new FFToddOrder(element);
                fft.transform(f, 0.0, Math.PI, n, element2); // Expected behaviour : runs correctly
            }
        }
    }

    /**
     * Tests if the FastFourierTransformer algorithm deals correctly with the algorithms depending on the order
     */
    @Test
    public void testTransformFunctionSizeNotAPowerOfTwoForFastFourierTransformer() {
        final int n = 127;
        final UnivariateFunction f = new Sin();
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FastFourierTransformer fft;
                fft = new FastFourierTransformer(element);
                fft.transform(f, 0.0, Math.PI, n, element2); // Expected behaviour: runs correctly
            }
        }
    }

    /**
     * Tests if the number of sample (ie the size of the real and imaginary part double[].
     */
    @Test
    public void testTransformFunctionNotStrictlyPositiveNumberOfSamples() {
        final int n = -128;
        final UnivariateFunction f = new Sin();
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FastFourierTransformer fft;
                fft = new FastFourierTransformer(element);
                try {
                    fft.transform(f, 0.0, Math.PI, n, element2);
                    fft.transform(f, 0.0, Math.PI, n, element2);
                    Assert.fail(element + ",  " + element2 +
                        ": NotStrictlyPositiveException was expected");
                } catch (final NotStrictlyPositiveException e) {
                    // Expected behaviour
                }
            }
        }
    }

    /**
     * Tests if the min and the max parameters are in the wrong order (ie min > max)
     */
    @Test
    public void testTransformFunctionInvalidBounds() {
        final int n = 128;
        final UnivariateFunction f = new Sin();
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                final FastFourierTransformer fft;
                fft = new FastFourierTransformer(element);
                try {
                    fft.transform(f, Math.PI, 0.0, n, element2);
                    Assert.fail(element + ", " + element2 +
                        ": NumberIsTooLargeException was expected");
                } catch (final NumberIsTooLargeException e) {
                    // Expected behaviour
                }
            }
        }
    }

    /**
     * Naive implementation of DFT, for reference.
     * 
     * @param x
     *        Complex[]
     * @param sgn
     *        int
     * @return y
     *         Complex[]
     */
    private static Complex[] dft(final Complex[] x, final int sgn) {
        final int n = x.length;
        final double[] cos = new double[n];
        final double[] sin = new double[n];
        final Complex[] y = new Complex[n];
        for (int i = 0; i < n; i++) {
            final double arg = 2.0 * FastMath.PI * i / n;
            cos[i] = MathLib.cos(arg);
            sin[i] = MathLib.sin(arg);
        }
        for (int i = 0; i < n; i++) {
            double yr = 0.0;
            double yi = 0.0;
            for (int j = 0; j < n; j++) {
                final int index = (i * j) % n;
                final double c = cos[index];
                final double s = sin[index];
                final double xr = x[j].getReal();
                final double xi = x[j].getImaginary();
                yr += c * xr - sgn * s * xi;
                yi += sgn * s * xr + c * xi;
            }
            y[i] = new Complex(yr, yi);
        }
        return y;
    }

    /**
     * Tests the complex transformation
     * 
     * @param n
     *        size
     * @param tol
     *        tolerance
     * @param normalization
     *        DftNormalization.STANDARD or UNIFORM
     * @param type
     *        TransformType.FORWARD or INVERSE
     */
    private static void doTestTransformComplex(final int n, final double tol, final DftNormalization normalization,
                                               final TransformType type) {
        final FastFourierTransformer fft;
        fft = new FastFourierTransformer(normalization);
        final Complex[] x = createComplexData(n);
        final Complex[] expected;
        final double s;
        if (type == TransformType.FORWARD) {
            expected = dft(x, -1);
            if (normalization == DftNormalization.STANDARD) {
                s = 1.0;
            } else {
                s = 1.0 / MathLib.sqrt(n);
            }
        } else {
            expected = dft(x, 1);
            if (normalization == DftNormalization.STANDARD) {
                s = 1.0 / n;
            } else {
                s = 1.0 / MathLib.sqrt(n);
            }
        }
        final Complex[] actual = fft.transform(x, type);
        for (int i = 0; i < n; i++) {
            final String msg;
            msg = String.format(" %s, %s, %d, %d", normalization, type, n, i);
            final double re = s * expected[i].getReal();
            Assert.assertEquals(msg, re, actual[i].getReal(),
                tol * MathLib.abs(re));
            final double im = s * expected[i].getImaginary();
            Assert.assertEquals(msg, im, actual[i].getImaginary(), tol *
                MathLib.abs(re));
        }
    }

    /**
     * @param n
     *        size
     * @param tol
     *        tolerance
     * @param normalization
     *        DftNormalization.STANDARD or UNIFORM
     * @param type
     *        TransformType.FORWARD or INVERSE
     */
    private static void doTestTransformReal(final int n, final double tol, final DftNormalization normalization,
                                            final TransformType type) {
        final FastFourierTransformer fft;
        fft = new FastFourierTransformer(normalization);
        final double[] x = createRealData(n);
        final Complex[] xc = new Complex[n];
        for (int i = 0; i < n; i++) {
            xc[i] = new Complex(x[i], 0.0);
        }
        final Complex[] expected;
        final double s;
        if (type == TransformType.FORWARD) {
            expected = dft(xc, -1);
            if (normalization == DftNormalization.STANDARD) {
                s = 1.0;
            } else {
                s = 1.0 / MathLib.sqrt(n);
            }
        } else {
            expected = dft(xc, 1);
            if (normalization == DftNormalization.STANDARD) {
                s = 1.0 / n;
            } else {
                s = 1.0 / MathLib.sqrt(n);
            }
        }
        final Complex[] actual = fft.transform(x, type);
        for (int i = 0; i < n; i++) {
            final String msg;
            msg = String.format("%s, %s, %d, %d", normalization, type, n, i);
            final double re = s * expected[i].getReal();
            Assert.assertEquals(msg, re, actual[i].getReal(),
                tol * MathLib.abs(re));
            final double im = s * expected[i].getImaginary();
            Assert.assertEquals(msg, im, actual[i].getImaginary(), tol *
                MathLib.abs(re));
        }
    }

    /**
     * 
     * @param f
     *        univariate function
     * @param min
     *        double
     * @param max
     *        double
     * @param n
     *        size
     * @param tol
     *        tolerance
     * @param normalization
     *        DftNormalization.STANDARD or UNIFORM
     * @param type
     *        TransformType.FORWARD or INVERSE
     */
    private static void doTestTransformFunction(final UnivariateFunction f,
                                                final double min, final double max, final int n, final double tol,
                                                final DftNormalization normalization,
                                                final TransformType type) {
        final FastFourierTransformer fft;
        fft = new FastFourierTransformer(normalization);
        final Complex[] x = new Complex[n];
        for (int i = 0; i < n; i++) {
            final double t = min + i * (max - min) / n;
            x[i] = new Complex(f.value(t));
        }
        final Complex[] expected;
        final double s;
        if (type == TransformType.FORWARD) {
            expected = dft(x, -1);
            if (normalization == DftNormalization.STANDARD) {
                s = 1.0;
            } else {
                s = 1.0 / MathLib.sqrt(n);
            }
        } else {
            expected = dft(x, 1);
            if (normalization == DftNormalization.STANDARD) {
                s = 1.0 / n;
            } else {
                s = 1.0 / MathLib.sqrt(n);
            }
        }
        final Complex[] actual = fft.transform(f, min, max, n, type);
        for (int i = 0; i < n; i++) {
            final String msg = String.format("%d, %d", n, i);
            final double re = s * expected[i].getReal();
            Assert.assertEquals(msg, re, actual[i].getReal(),
                tol * MathLib.abs(re));
            final double im = s * expected[i].getImaginary();
            Assert.assertEquals(msg, im, actual[i].getImaginary(), tol *
                MathLib.abs(re));
        }
    }

    /**
     * Tests of standard transform (when data is valid).
     */
    @Test
    public void testTransformComplex() {
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                doTestTransformComplex(2, 1.0E-15, element, element2);
                doTestTransformComplex(4, 1.0E-14, element, element2);
                doTestTransformComplex(8, 1.0E-14, element, element2);
                doTestTransformComplex(16, 1.0E-13, element, element2);
                doTestTransformComplex(32, 1.0E-13, element, element2);
                doTestTransformComplex(64, 1.0E-12, element, element2);
                doTestTransformComplex(128, 1.0E-12, element, element2);
            }
        }
    }

    /**
     * Tests of standard transform (when data is valid).
     */
    @Test
    public void testStandardTransformReal() {
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                doTestTransformReal(2, 1.0E-15, element, element2);
                doTestTransformReal(4, 1.0E-14, element, element2);
                doTestTransformReal(8, 1.0E-14, element, element2);
                doTestTransformReal(16, 1.0E-13, element, element2);
                doTestTransformReal(32, 1.0E-13, element, element2);
                doTestTransformReal(64, 1.0E-13, element, element2);
                doTestTransformReal(128, 1.0E-11, element, element2);
            }
        }
    }

    /**
     * Tests of standard transform (when data is valid).
     */
    @Test
    public void testStandardTransformFunction() {
        final UnivariateFunction f = new Sinc();
        final double min = -FastMath.PI;
        final double max = FastMath.PI;
        final DftNormalization[] norm;
        norm = DftNormalization.values();
        final TransformType[] type;
        type = TransformType.values();
        for (final DftNormalization element : norm) {
            for (final TransformType element2 : type) {
                doTestTransformFunction(f, min, max, 2, 1.0E-15, element, element2);
                doTestTransformFunction(f, min, max, 4, 1.0E-14, element, element2);
                doTestTransformFunction(f, min, max, 8, 1.0E-14, element, element2);
                doTestTransformFunction(f, min, max, 16, 1.0E-13, element, element2);
                doTestTransformFunction(f, min, max, 32, 1.0E-13, element, element2);
                doTestTransformFunction(f, min, max, 64, 1.0E-12, element, element2);
                doTestTransformFunction(f, min, max, 128, 1.0E-11, element, element2);
            }
        }
    }

    /*
     * Additional tests for 1D data.
     */

    /**
     * Test of transformer for the ad hoc data taken from Mathematica.
     */
    @Test
    public void testAdHocData() {
        FastFourierTransformer transformer;
        transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex result[];
        final double tolerance = 1E-12;

        final double x[] = { 1.3, 2.4, 1.7, 4.1, 2.9, 1.7, 5.1, 2.7 };
        final Complex y[] = {
            new Complex(21.9, 0.0),
            new Complex(-2.09497474683058, 1.91507575950825),
            new Complex(-2.6, 2.7),
            new Complex(-1.10502525316942, -4.88492424049175),
            new Complex(0.1, 0.0),
            new Complex(-1.10502525316942, 4.88492424049175),
            new Complex(-2.6, -2.7),
            new Complex(-2.09497474683058, -1.91507575950825) };

        result = transformer.transform(x, TransformType.FORWARD);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(y[i].getReal(), result[i].getReal(), tolerance);
            Assert.assertEquals(y[i].getImaginary(), result[i].getImaginary(), tolerance);
        }

        result = transformer.transform(y, TransformType.INVERSE);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(x[i], result[i].getReal(), tolerance);
            Assert.assertEquals(0.0, result[i].getImaginary(), tolerance);
        }

        final double x2[] = { 10.4, 21.6, 40.8, 13.6, 23.2, 32.8, 13.6, 19.2 };
        TransformUtils.scaleArray(x2, 1.0 / MathLib.sqrt(x2.length));
        final Complex y2[] = y;

        transformer = new FastFourierTransformer(DftNormalization.UNITARY);
        result = transformer.transform(y2, TransformType.FORWARD);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(x2[i], result[i].getReal(), tolerance);
            Assert.assertEquals(0.0, result[i].getImaginary(), tolerance);
        }

        result = transformer.transform(x2, TransformType.INVERSE);
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(y2[i].getReal(), result[i].getReal(), tolerance);
            Assert.assertEquals(y2[i].getImaginary(), result[i].getImaginary(), tolerance);
        }
    }

    /**
     * Test of transformer for the sine function.
     */
    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        FastFourierTransformer transformer;
        transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex result[];
        final int N = 1 << 8;
        double min;
        double max;
        final double tolerance = 1E-12;

        min = 0.0;
        max = 2.0 * FastMath.PI;
        result = transformer.transform(f, min, max, N, TransformType.FORWARD);
        Assert.assertEquals(0.0, result[1].getReal(), tolerance);
        Assert.assertEquals(-(N >> 1), result[1].getImaginary(), tolerance);
        Assert.assertEquals(0.0, result[N - 1].getReal(), tolerance);
        Assert.assertEquals(N >> 1, result[N - 1].getImaginary(), tolerance);
        for (int i = 0; i < N - 1; i += i == 0 ? 2 : 1) {
            Assert.assertEquals(0.0, result[i].getReal(), tolerance);
            Assert.assertEquals(0.0, result[i].getImaginary(), tolerance);
        }

        min = -FastMath.PI;
        max = FastMath.PI;
        result = transformer.transform(f, min, max, N, TransformType.INVERSE);
        Assert.assertEquals(0.0, result[1].getReal(), tolerance);
        Assert.assertEquals(-0.5, result[1].getImaginary(), tolerance);
        Assert.assertEquals(0.0, result[N - 1].getReal(), tolerance);
        Assert.assertEquals(0.5, result[N - 1].getImaginary(), tolerance);
        for (int i = 0; i < N - 1; i += i == 0 ? 2 : 1) {
            Assert.assertEquals(0.0, result[i].getReal(), tolerance);
            Assert.assertEquals(0.0, result[i].getImaginary(), tolerance);
        }
    }

    /**
     * Utility methods for checking (successful) transforms.
     */

    /**
     * Creates a complex vector of samples.
     * 
     * @param n
     *        the size of the sample
     * @return data a Complex[] randomly created
     */
    private static Complex[] createComplexData(final int n) {
        final Random randomObject = new Random(SEED);
        final Complex[] data = new Complex[n];
        for (int i = 0; i < n; i++) {
            final double re = 2.0 * randomObject.nextDouble() - 1.0;
            final double im = 2.0 * randomObject.nextDouble() - 1.0;
            data[i] = new Complex(re, im);
        }
        return data;
    }

    /**
     * Creates a real vector of samples.
     * 
     * @param n
     *        the size of the sample
     * @return data a double[] randomly created
     */
    private static double[] createRealData(final int n) {
        final Random randomObject = new Random(SEED);
        final double[] data = new double[n];
        for (int i = 0; i < n; i++) {
            data[i] = 2.0 * randomObject.nextDouble() - 1.0;
        }
        return data;
    }

    /**
     * silly test for coverage : creating a double[][] that is supposed to be converted into complex, and therefore
     * should be of dimension 2 by n.
     * Expected behaviour : DimensionMismatchException
     */
    @Test(expected = Exception.class)
    public void testDataRI3() {
        final DftNormalization normalization = DftNormalization.UNITARY;
        final TransformType type = TransformType.FORWARD;
        final FFTpowerOfTwoOrder fft = new FFTpowerOfTwoOrder(normalization);
        final double[][] dataRI = new double[3][5];
        // Expected behaviour : DimensionMismatchException
        fft.transformInPlace(dataRI, normalization, type);
    }

    /**
     * silly test for coverage : creating a double[][] which is not a matrix, meaning all lines do not have the same
     * length.
     * // Expected behaviour : DimensionMismatchException
     */
    @Test(expected = Exception.class)
    public void testDataRIwithDifferentSizes() {
        final DftNormalization normalization = DftNormalization.STANDARD;
        final TransformType type = TransformType.FORWARD;
        final FFTpowerOfTwoOrder fft = new FFTpowerOfTwoOrder(normalization);
        final double[][] dataRI = new double[2][];
        dataRI[0] = new double[5];
        dataRI[1] = new double[7];
        // Expected behaviour : DimensionMismatchException
        fft.transformInPlace(dataRI, normalization, type);
    }

    /**
     * silly test for coverage : creating a double[][]that is supposed to be converted into complex, and therefore
     * should be of dimension 2 by n.
     * // Expected behaviour : DimensionMismatchException
     */
    @Test(expected = Exception.class)
    public void testConvolveComplexWithDifferentSizes() {
        final double[] xreal = new double[5];
        final double[] ximag = new double[7];

        final double[] outreal = new double[5];
        final double[] outimag = new double[5];
        // Expected behaviour : DimensionMismatchException
        FFToddOrder.convolveComplex(xreal, ximag, xreal, ximag, outreal, outimag);
    }

    /**
     * tests the Bluestein algorithm
     * 
     */
    @Test
    public void testBluesteinAlgo() {
        // Test power-of-2 size FFTs
        for (int i = 0; i <= 12; i++) {
            testFft(1 << i);
        }

        // Test small size FFTs
        for (int i = 0; i < 30; i++) {
            testFft(i);
        }

        // Test diverse size FFTs
        int prev = 0;
        for (int i = 0; i <= 100; i++) {
            final int n = (int) Math.round(Math.pow(1500, i / 100.0));
            if (n > prev) {
                testFft(n);
                prev = n;
            }
        }

        // Test power-of-2 size convolutions
        for (int i = 0; i <= 12; i++) {
            testConvolution(1 << i);
        }

        // Test diverse size convolutions
        prev = 0;
        for (int i = 0; i <= 100; i++) {
            final int n = (int) Math.round(Math.pow(1500, i / 100.0));
            if (n > prev) {
                testConvolution(n);
                prev = n;
            }
        }

    }

    /**
     * auxiliary function for the test of the Bluestein algorithm for FORWARD FT (DFT) in class FFToddOrder
     * 
     * @param size
     *        the size of the input vector to test
     */
    public static void testFft(final int size) {
        final double[] inputreal = randomReals(size);
        final double[] inputimag = randomReals(size);

        final double[] refoutreal = new double[size];
        final double[] refoutimag = new double[size];
        naiveDft(inputreal, inputimag, refoutreal, refoutimag, false);

        final double[] actualoutreal = inputreal.clone();
        final double[] actualoutimag = inputimag.clone();

        FFToddOrder.dft(actualoutreal, actualoutimag);

        final double tolerance = 1E-12;
        for (int i = 0; i < actualoutimag.length; i++) {
            Assert.assertEquals(refoutreal[i], actualoutreal[i], tolerance);
            Assert.assertEquals(refoutimag[i], actualoutimag[i], tolerance);
        }

    }

    /**
     * auxiliary function for the test of the convolution in class FFToddOrder
     * 
     * @param size
     *        the size of the input vector to test
     */
    public static void testConvolution(final int size) {
        final double[] input0real = randomReals(size);
        final double[] input0imag = randomReals(size);

        final double[] input1real = randomReals(size);
        final double[] input1imag = randomReals(size);

        final double[] refoutreal = new double[size];
        final double[] refoutimag = new double[size];
        naiveConvolve(input0real, input0imag, input1real, input1imag, refoutreal, refoutimag);

        final double[] actualoutreal = new double[size];
        final double[] actualoutimag = new double[size];
        FFToddOrder.convolveComplex(input0real, input0imag, input1real, input1imag, actualoutreal, actualoutimag);
        final double tolerance = 1E-12;
        for (int i = 0; i < actualoutimag.length; i++) {
            Assert.assertEquals(refoutreal[i], actualoutreal[i], tolerance);
            Assert.assertEquals(refoutimag[i], actualoutimag[i], tolerance);
        }
    }

    /**
     * auxiliary function for the test of the Bluestein algo : Naive reference computation functions.
     * 
     * @param inreal
     *        the real part of the input vector
     * @param inimag
     *        the imaginary part of the input vector
     * @param outreal
     *        the real part of the output vector
     * @param outimag
     *        the imaginary part of the output vector
     * @param inverse
     *        the naive DFT in output
     */
    public static void naiveDft(final double[] inreal, final double[] inimag, final double[] outreal,
                                final double[] outimag, final boolean inverse) {
        if (inreal.length != inimag.length || inreal.length != outreal.length || outreal.length != outimag.length) {
            throw new IllegalArgumentException(LENGTH_ERROR);
        }

        final int n = inreal.length;
        final double coef = (inverse ? 2 : -2) * Math.PI;
        for (int k = 0; k < n; k++) { // For each output element
            double sumreal = 0;
            double sumimag = 0;
            for (int t = 0; t < n; t++) { // For each input element
                final double angle = coef * (int) ((long) t * k % n) / n; // This is more accurate than t * k
                sumreal += inreal[t] * Math.cos(angle) - inimag[t] * Math.sin(angle);
                sumimag += inreal[t] * Math.sin(angle) + inimag[t] * Math.cos(angle);
            }
            outreal[k] = sumreal;
            outimag[k] = sumimag;
        }
    }

    /**
     * auxiliary function for the test of the Bluestein algo : Naive reference computation functions.
     * 
     * @param xreal
     *        the real part of the first vector
     * @param ximag
     *        the imaginary part of the first vector
     * @param yreal
     *        the real part of the second vector
     * @param yimag
     *        the imaginary part of the second vector
     * @param outreal
     *        the real part of the output vector
     * @param outimag
     *        the imaginary part of the output vector
     */
    public static void naiveConvolve(final double[] xreal, final double[] ximag, final double[] yreal,
                                     final double[] yimag, final double[] outreal,
                                     final double[] outimag) {
        if (xreal.length != ximag.length || xreal.length != yreal.length || yreal.length != yimag.length
            || xreal.length != outreal.length || outreal.length != outimag.length) {
            throw new IllegalArgumentException(LENGTH_ERROR);
        }

        final int n = xreal.length;
        for (int i = 0; i < n; i++) {
            double sumreal = 0;
            double sumimag = 0;
            for (int j = 0; j < n; j++) {
                final int k = (i - j + n) % n;
                sumreal += xreal[k] * yreal[j] - ximag[k] * yimag[j];
                sumimag += xreal[k] * yimag[j] + ximag[k] * yreal[j];
            }
            outreal[i] = sumreal;
            outimag[i] = sumimag;
        }
    }

    /**
     * 
     * Creates random vector
     * 
     * @param size
     *        requested size for the random vector to create
     * @return
     *         a randomly created real vector
     */
    private static double[] randomReals(final int size) {
        final double[] result = new double[size];
        final Random random = new Random();
        for (int i = 0; i < result.length; i++) {
            result[i] = random.nextDouble() * 2 - 1;
        }
        return result;
    }

}
