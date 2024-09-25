/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 *
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
 * @history created 21/08/2014
 *
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:285:21/08/2014: (creation) FFT adapted to all orders
 * VERSION::FA:375:27/11/2014: copy of input data in method transform
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.transform;

import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * This class, with PACKAGE visibility, implements an algorithm for the Fast Fourier Transform for odd order, based on
 * the Bluestein algorithm.
 * 
 * @version $Id: FFToddOrder.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 * @since 2.3
 */
class FFToddOrder extends AbstractFastFourierTransformer {

    /** Error message in case the real part and imaginary part of a vector do not have the same length */
    private static final String ERROR_MISMATCHED_LENGTHS = "Mismatched lengths";

    /**
     * Constructor of the class FFToddOrder, inherited from the one of the abstract class AbstractFastFourierTransformer
     * 
     * @param dftNormalization
     *        an enum with two possible values : STANDARD or UNITARY
     */
    public FFToddOrder(final DftNormalization dftNormalization) {
        super(dftNormalization);
    }

    // inherited methods

    /**
     * Returns the (forward, inverse) transform of the specified real data set.
     * 
     * @param f
     *        the real data array to be transformed
     * @param type
     *        the type of transform (forward, inverse) to be performed
     * @return the complex transformed array
     */
    @Override
    public Complex[] transform(final double[] f, final TransformType type) {

        // creating the real and imaginary part
        final double[] realPart = f.clone();
        final double[] imaginaryPart = new double[f.length];

        switch (type) {
            case FORWARD:
                dft(realPart, imaginaryPart);
                break;
            case INVERSE:
                inverseDFT(realPart, imaginaryPart);
                break;
            default:
                /*
                 * This should never occur in normal conditions. However this
                 * clause has been added for coding quality reasons.
                 */
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        final double[][] dataRI = new double[][] { realPart, imaginaryPart };
        normalizeTransformedData(dataRI, this.getNormalization(), type);
        return TransformUtils.createComplexArray(dataRI);
    }

    /**
     * Returns the (forward, inverse) transform of the specified complex data set.
     * 
     * @param f
     *        the complex data array to be transformed
     * @param type
     *        the type of transform (forward, inverse) to be performed
     * @return the complex transformed array
     */
    @Override
    public Complex[] transform(final Complex[] f, final TransformType type) {

        // transformation of the complex f into a double array of reals
        final double[][] data = TransformUtils.createRealImaginaryArray(f);
        final double[] realPart = data[0];
        final double[] imaginaryPart = data[1];

        switch (type) {
            case FORWARD:
                dft(realPart, imaginaryPart);
                break;
            case INVERSE:
                inverseDFT(realPart, imaginaryPart);
                break;
            default:
                /*
                 * This should never occur in normal conditions. However this
                 * clause has been added for coding quality reasons.
                 */
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }

        final double[][] dataRI = new double[][] { realPart, imaginaryPart };
        normalizeTransformedData(dataRI, this.getNormalization(), type);
        return TransformUtils.createComplexArray(dataRI);
    }

    // methods from the original algorithm
    /**
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the
     * vector.
     * The vector can have any length. This is a wrapper function.
     * 
     * @param real
     *        real part of the given complex vector (input) and real part of the DFT computed (output)
     * @param imag
     *        imaginary part of the given complex vector (input) and imaginary part of the DFT computed (output)
     * 
     */
    public static void dft(final double[] real, final double[] imag) {

        final int n = real.length;
        if (n == 0) {
            return;
        } else if ((n & (n - 1)) == 0) {
            // Is power of 2
            transformRadix2(real, imag);
        } else {
            // More complicated algorithm for arbitrary sizes
            transformBluestein(real, imag);
        }
    }

    /**
     * Computes the inverse discrete Fourier transform (IDFT) of the given complex vector, storing the result back into
     * the vector.
     * The vector can have any length. This is a wrapper function. This transform does not perform scaling, so the
     * inverse is not a true inverse.
     * 
     * @param real
     *        real part of the given complex vector (input) and real part of the inverse DFT computed (output)
     * @param imag
     *        imaginary part of the given complex vector (input) and imaginary part of the inverse DFT computed
     *        (output)
     */
    public static void inverseDFT(final double[] real, final double[] imag) {
        dft(imag, real);
    }

    /**
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the
     * vector.
     * The vector can have any length. This requires the convolution function, which in turn requires the radix-2 FFT
     * function.
     * Uses Bluestein's chirp z-transform algorithm.
     * 
     * @param real
     *        vector of the real components
     * @param imag
     *        vector of the imaginary parts
     * 
     */
    public static void transformBluestein(final double[] real, final double[] imag) {
        // Find a power-of-2 convolution length m such that m >= n * 2 + 1

        final int n = real.length;
        /* This condition is from the original algorithm from the source in the head */
        if (n >= 0x20000000) {
            throw new IllegalArgumentException("Array too large");
        }
        final int m = Integer.highestOneBit(n * 2 + 1) << 1;

        // Trignometric tables
        final double[] cosTable = new double[n];
        final double[] sinTable = new double[n];
        for (int i = 0; i < n; i++) {
            final int j = (int) ((long) i * i % (n * 2));
            // This is more accurate than j = i * i
            final double[] sincos = MathLib.sinAndCos(FastMath.PI * j / n);
            cosTable[i] = sincos[1];
            sinTable[i] = sincos[0];
        }

        // Temporary vectors and preprocessing
        final double[] areal = new double[m];
        final double[] aimag = new double[m];
        for (int i = 0; i < n; i++) {
            areal[i] = real[i] * cosTable[i] + imag[i] * sinTable[i];
            aimag[i] = -real[i] * sinTable[i] + imag[i] * cosTable[i];
        }
        final double[] breal = new double[m];
        final double[] bimag = new double[m];
        breal[0] = cosTable[0];
        bimag[0] = sinTable[0];
        for (int i = 1; i < n; i++) {
            // breal[i] = breal[m - i] = cosTable[i];
            // bimag[i] = bimag[m - i] = sinTable[i];
            breal[m - i] = cosTable[i];
            bimag[m - i] = sinTable[i];
            breal[i] = cosTable[i];
            bimag[i] = sinTable[i];
        }

        // Convolution
        final double[] creal = new double[m];
        final double[] cimag = new double[m];
        convolveComplex(areal, aimag, breal, bimag, creal, cimag);

        // Postprocessing
        for (int i = 0; i < n; i++) {
            real[i] = creal[i] * cosTable[i] + cimag[i] * sinTable[i];
            imag[i] = -creal[i] * sinTable[i] + cimag[i] * cosTable[i];
        }
    }

    /**
     * Computes the circular convolution of the given COMPLEX vectors. Each vector's length must be the same.
     * 
     * @param xrealIn
     *        the real part of the first COMPLEX vector
     * @param ximagIn
     *        the imaginary part of the first COMPLEX vector
     * @param yrealIn
     *        the real part of the second COMPLEX vector, with the same length as x
     * @param yimagIn
     *        the imaginary part of the second COMPLEX vector, with the same length as x
     * @param outreal
     *        the real part of the circular convolution of x and y
     * @param outimag
     *        the imaginary part of the circular convolution of x and y
     */
    public static void convolveComplex(final double[] xrealIn, final double[] ximagIn, final double[] yrealIn,
                                       final double[] yimagIn, final double[] outreal, final double[] outimag) {
        // check vectors dimensions
        boolean cond = xrealIn.length != ximagIn.length || xrealIn.length != yrealIn.length
            || yrealIn.length != yimagIn.length;
        cond |= xrealIn.length != outreal.length || outreal.length != outimag.length;
        if (cond) {
            // Dimension mismatch
            throw new IllegalArgumentException(ERROR_MISMATCHED_LENGTHS);
        }

        // Clone data
        final double[] xreal = xrealIn.clone();
        final double[] ximag = ximagIn.clone();
        final double[] yreal = yrealIn.clone();
        final double[] yimag = yimagIn.clone();

        // vector dimension
        final int n = xreal.length;

        // Computation
        dft(xreal, ximag);
        dft(yreal, yimag);
        for (int i = 0; i < n; i++) {
            final double temp = xreal[i] * yreal[i] - ximag[i] * yimag[i];
            ximag[i] = ximag[i] * yreal[i] + xreal[i] * yimag[i];
            xreal[i] = temp;
        }
        inverseDFT(xreal, ximag);
        for (int i = 0; i < n; i++) {
            // Scaling (because this FFT implementation omits it)
            outreal[i] = xreal[i] / n;
            outimag[i] = ximag[i] / n;
        }
    }

    /**
     * Computes the discrete Fourier transform (DFT) of the given complex vector, storing the result back into the
     * vector.
     * The vector's length must be a power of 2. Uses the Cooley-Tukey decimation-in-time radix-2 algorithm.
     * 
     * @param real
     *        real part of the given complex vector (input) and real part of the DFT computed (output)
     * @param imag
     *        imaginary part of the given complex vector (input) and imaginary part of the DFT computed (output)
     * 
     */
    @SuppressWarnings("PMD.OneDeclarationPerLine")
    public static void transformRadix2(final double[] real, final double[] imag) {
        // Initialization
        if (real.length != imag.length) {
            // Exception
            throw new IllegalArgumentException(ERROR_MISMATCHED_LENGTHS);
        }
        // get vector length
        final int n = real.length;
        // Equal to floor(log2(n))
        final int levels = 31 - Integer.numberOfLeadingZeros(n);
        if (1 << levels != n) {
            // Exception
            throw new IllegalArgumentException("Length is not a power of 2");
        }

        // Pre-compute cos/sin
        // For optimisation
        final double[] cosTable = new double[n / 2];
        final double[] sinTable = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            final double[] sincos = MathLib.sinAndCos(2 * FastMath.PI * i / n);
            cosTable[i] = sincos[1];
            sinTable[i] = sincos[0];
        }

        // Bit-reversed addressing permutation
        for (int i = 0; i < n; i++) {
            final int j = Integer.reverse(i) >>> (32 - levels);
            if (j > i) {
                double temp = real[i];
                real[i] = real[j];
                real[j] = temp;
                temp = imag[i];
                imag[i] = imag[j];
                imag[j] = temp;
            }
        }

        // Cooley-Tukey decimation-in-time radix-2 FFT
        for (int size = 2; size <= n; size *= 2) {
            // local variables halfsize and tablestep to use in computation
            final int halfsize = size / 2;
            final int tablestep = n / size;
            for (int i = 0; i < n; i += size) {
                for (int j = i, k = 0; j < i + halfsize; j++, k += tablestep) {
                    final double tpre = real[j + halfsize] * cosTable[k] + imag[j + halfsize] * sinTable[k];
                    final double tpim = -real[j + halfsize] * sinTable[k] + imag[j + halfsize] * cosTable[k];
                    real[j + halfsize] = real[j] - tpre;
                    imag[j + halfsize] = imag[j] - tpim;
                    real[j] += tpre;
                    imag[j] += tpim;
                }
            }
            // Prevent overflow in 'size *= 2'
            if (size == n) {
                break;
            }
        }
    }

    // CHECKSTYLE: resume MagicNumber check
}
