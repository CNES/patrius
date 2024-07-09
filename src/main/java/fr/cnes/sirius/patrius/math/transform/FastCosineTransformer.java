/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.transform;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.FunctionUtils;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.complex.Complex;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implements the Fast Cosine Transform for transformation of one-dimensional
 * real data sets. For reference, see James S. Walker, <em>Fast Fourier
 * Transforms</em>, chapter 3 (ISBN 0849371635).
 * <p>
 * There are several variants of the discrete cosine transform. The present implementation corresponds to DCT-I, with
 * various normalization conventions, which are specified by the parameter {@link DctNormalization}.
 * <p>
 * DCT-I is equivalent to DFT of an <em>even extension</em> of the data series. More precisely, if x<sub>0</sub>,
 * &hellip;, x<sub>N-1</sub> is the data set to be cosine transformed, the extended data set
 * x<sub>0</sub><sup>&#35;</sup>, &hellip;, x<sub>2N-3</sub><sup>&#35;</sup> is defined as follows
 * <ul>
 * <li>x<sub>k</sub><sup>&#35;</sup> = x<sub>k</sub> if 0 &le; k &lt; N,</li>
 * <li>x<sub>k</sub><sup>&#35;</sup> = x<sub>2N-2-k</sub> if N &le; k &lt; 2N - 2.</li>
 * </ul>
 * <p>
 * Then, the standard DCT-I y<sub>0</sub>, &hellip;, y<sub>N-1</sub> of the real data set x<sub>0</sub>, &hellip;,
 * x<sub>N-1</sub> is equal to <em>half</em> of the N first elements of the DFT of the extended data set
 * x<sub>0</sub><sup>&#35;</sup>, &hellip;, x<sub>2N-3</sub><sup>&#35;</sup> <br/>
 * y<sub>n</sub> = (1 / 2) &sum;<sub>k=0</sub><sup>2N-3</sup> x<sub>k</sub><sup>&#35;</sup> exp[-2&pi;i nk / (2N - 2)]
 * &nbsp;&nbsp;&nbsp;&nbsp;k = 0, &hellip;, N-1.
 * <p>
 * The present implementation of the discrete cosine transform as a fast cosine transform requires the length of the
 * data set to be a power of two plus one (N&nbsp;=&nbsp;2<sup>n</sup>&nbsp;+&nbsp;1). Besides, it implicitly assumes
 * that the sampled function is even.
 * 
 * @version $Id: FastCosineTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class FastCosineTransformer implements RealTransformer, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20120212L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** The type of DCT to be performed. */
    private final DctNormalization normalization;

    /**
     * Creates a new instance of this class, with various normalization
     * conventions.
     * 
     * @param normalizationIn
     *        the type of normalization to be applied to the
     *        transformed data
     */
    public FastCosineTransformer(final DctNormalization normalizationIn) {
        this.normalization = normalizationIn;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws MathIllegalArgumentException
     *         if the length of the data array is
     *         not a power of two plus one
     */
    @Override
    public double[] transform(final double[] f, final TransformType type) {
        if (type == TransformType.FORWARD) {
            // Forward
            final double[] res;
            if (this.normalization == DctNormalization.ORTHOGONAL_DCT_I) {
                final double s = MathLib.sqrt(2.0 / (f.length - 1));
                res = TransformUtils.scaleArray(this.fct(f), s);
            } else {
                res = this.fct(f);
            }
            // Return result
            return res;
        }
        // Inverse case
        final double s2 = 2.0 / (f.length - 1);
        final double s1;
        if (this.normalization == DctNormalization.ORTHOGONAL_DCT_I) {
            s1 = MathLib.sqrt(s2);
        } else {
            s1 = s2;
        }
        // Return result
        return TransformUtils.scaleArray(this.fct(f), s1);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if the lower bound is greater than, or equal to the upper bound
     * @throws fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException
     *         if the number of sample points is negative
     * @throws MathIllegalArgumentException
     *         if the number of sample points is
     *         not a power of two plus one
     */
    @Override
    public double[] transform(final UnivariateFunction f,
                              final double min, final double max, final int n,
                              final TransformType type) {

        final double[] data = FunctionUtils.sample(f, min, max, n);
        return this.transform(data, type);
    }

    /**
     * Perform the FCT algorithm (including inverse).
     * 
     * @param f
     *        the real data array to be transformed
     * @return the real transformed array
     * @throws MathIllegalArgumentException
     *         if the length of the data array is
     *         not a power of two plus one
     */
    protected double[] fct(final double[] f) {

        // Sanity check
        final int n = f.length - 1;
        if (!ArithmeticUtils.isPowerOfTwo(n)) {
            // Exception
            throw new MathIllegalArgumentException(
                PatriusMessages.NOT_POWER_OF_TWO_PLUS_ONE,
                Integer.valueOf(f.length));
        }

        // Initialization
        final double[] transformed = new double[f.length];

        if (n == 1) {
            // trivial case
            transformed[0] = HALF * (f[0] + f[1]);
            transformed[1] = HALF * (f[0] - f[1]);
            return transformed;
        }

        // construct a new array and perform FFT on it
        final double[] x = new double[n];
        x[0] = HALF * (f[0] + f[n]);
        x[n >> 1] = f[n >> 1];
        // temporary variable for transformed[1]
        double t1 = HALF * (f[0] - f[n]);
        for (int i = 1; i < (n >> 1); i++) {
            final double[] sincos = MathLib.sinAndCos(i * FastMath.PI / n);
            final double sin = sincos[0];
            final double cos = sincos[1];
            final double a = 0.5 * (f[i] + f[n - i]);
            final double b = sin * (f[i] - f[n - i]);
            final double c = cos * (f[i] - f[n - i]);
            x[i] = a - b;
            x[n - i] = a + b;
            t1 += c;
        }
        final FastFourierTransformer transformer;
        transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        final Complex[] y = transformer.transform(x, TransformType.FORWARD);

        // reconstruct the FCT result for the original array
        transformed[0] = y[0].getReal();
        transformed[1] = t1;
        for (int i = 1; i < (n >> 1); i++) {
            transformed[2 * i] = y[i].getReal();
            transformed[2 * i + 1] = transformed[2 * i - 1] - y[i].getImaginary();
        }
        transformed[n] = y[n >> 1].getReal();

        // Return result
        return transformed;
    }
}
