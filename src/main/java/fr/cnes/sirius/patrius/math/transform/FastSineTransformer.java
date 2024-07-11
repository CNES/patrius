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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * Implements the Fast Sine Transform for transformation of one-dimensional real
 * data sets. For reference, see James S. Walker, <em>Fast Fourier
 * Transforms</em>, chapter 3 (ISBN 0849371635).
 * <p>
 * There are several variants of the discrete sine transform. The present implementation corresponds to DST-I, with
 * various normalization conventions, which are specified by the parameter {@link DstNormalization}. <strong>It should
 * be noted that regardless to the convention, the first element of the dataset to be transformed must be zero.</strong>
 * <p>
 * DST-I is equivalent to DFT of an <em>odd extension</em> of the data series. More precisely, if x<sub>0</sub>,
 * &hellip;, x<sub>N-1</sub> is the data set to be sine transformed, the extended data set
 * x<sub>0</sub><sup>&#35;</sup>, &hellip;, x<sub>2N-1</sub><sup>&#35;</sup> is defined as follows
 * <ul>
 * <li>x<sub>0</sub><sup>&#35;</sup> = x<sub>0</sub> = 0,</li>
 * <li>x<sub>k</sub><sup>&#35;</sup> = x<sub>k</sub> if 1 &le; k &lt; N,</li>
 * <li>x<sub>N</sub><sup>&#35;</sup> = 0,</li>
 * <li>x<sub>k</sub><sup>&#35;</sup> = -x<sub>2N-k</sub> if N + 1 &le; k &lt; 2N.</li>
 * </ul>
 * <p>
 * Then, the standard DST-I y<sub>0</sub>, &hellip;, y<sub>N-1</sub> of the real data set x<sub>0</sub>, &hellip;,
 * x<sub>N-1</sub> is equal to <em>half</em> of i (the pure imaginary number) times the N first elements of the DFT of
 * the extended data set x<sub>0</sub><sup>&#35;</sup>, &hellip;, x<sub>2N-1</sub><sup>&#35;</sup> <br />
 * y<sub>n</sub> = (i / 2) &sum;<sub>k=0</sub><sup>2N-1</sup> x<sub>k</sub><sup>&#35;</sup> exp[-2&pi;i nk / (2N)]
 * &nbsp;&nbsp;&nbsp;&nbsp;k = 0, &hellip;, N-1.
 * <p>
 * The present implementation of the discrete sine transform as a fast sine transform requires the length of the data to
 * be a power of two. Besides, it implicitly assumes that the sampled function is odd. In particular, the first element
 * of the data set must be 0, which is enforced in
 * {@link #transform(UnivariateFunction, double, double, int, TransformType)}, after sampling.
 * 
 * @version $Id: FastSineTransformer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 1.2
 */
public class FastSineTransformer implements RealTransformer, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 20120211L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** The type of DST to be performed. */
    private final DstNormalization normalization;

    /**
     * Creates a new instance of this class, with various normalization conventions.
     * 
     * @param normalizationIn
     *        the type of normalization to be applied to the transformed data
     */
    public FastSineTransformer(final DstNormalization normalizationIn) {
        this.normalization = normalizationIn;
    }

    /**
     * {@inheritDoc}
     * 
     * The first element of the specified data set is required to be {@code 0}.
     * 
     * @throws MathIllegalArgumentException
     *         if the length of the data array is
     *         not a power of two, or the first element of the data array is not zero
     */
    @Override
    public double[] transform(final double[] f, final TransformType type) {
        final double[] res;
        if (this.normalization == DstNormalization.ORTHOGONAL_DST_I) {
            final double s = MathLib.sqrt(2.0 / f.length);
            res = TransformUtils.scaleArray(this.fst(f), s);
        } else if (type == TransformType.FORWARD) {
            res = this.fst(f);
        } else {
            final double s = 2.0 / f.length;
            res = TransformUtils.scaleArray(this.fst(f), s);
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation enforces {@code f(x) = 0.0} at {@code x = 0.0}.
     * 
     * @throws fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException
     *         if the lower bound is greater than, or equal to the upper bound
     * @throws fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException
     *         if the number of sample points is negative
     * @throws MathIllegalArgumentException
     *         if the number of sample points is not a power of two
     */
    @Override
    public double[] transform(final UnivariateFunction f,
                              final double min, final double max, final int n,
                              final TransformType type) {

        final double[] data = FunctionUtils.sample(f, min, max, n);
        data[0] = 0.0;
        return this.transform(data, type);
    }

    /**
     * Perform the FST algorithm (including inverse). The first element of the
     * data set is required to be {@code 0}.
     * 
     * @param f
     *        the real data array to be transformed
     * @return the real transformed array
     * @throws MathIllegalArgumentException
     *         if the length of the data array is
     *         not a power of two, or the first element of the data array is not zero
     */
    protected double[] fst(final double[] f) {

        if (!ArithmeticUtils.isPowerOfTwo(f.length)) {
            // Exception
            throw new MathIllegalArgumentException(
                PatriusMessages.NOT_POWER_OF_TWO_CONSIDER_PADDING,
                Integer.valueOf(f.length));
        }
        if (f[0] != 0.0) {
            // Exception
            throw new MathIllegalArgumentException(
                PatriusMessages.FIRST_ELEMENT_NOT_ZERO,
                Double.valueOf(f[0]));
        }

        // Initialization
        final double[] transformed = new double[f.length];

        final int n = f.length;
        if (n == 1) {
            // trivial case
            transformed[0] = 0.0;
            return transformed;
        }

        // construct a new array and perform FFT on it
        final double[] x = new double[n];
        x[0] = 0.0;
        x[n >> 1] = 2.0 * f[n >> 1];
        for (int i = 1; i < (n >> 1); i++) {
            final double a = MathLib.sin(i * FastMath.PI / n) * (f[i] + f[n - i]);
            final double b = 0.5 * (f[i] - f[n - i]);
            x[i] = a + b;
            x[n - i] = a - b;
        }
        final FastFourierTransformer transformer;
        transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        final Complex[] y = transformer.transform(x, TransformType.FORWARD);

        // reconstruct the FST result for the original array
        transformed[0] = 0.0;
        transformed[1] = HALF * y[0].getReal();
        for (int i = 1; i < (n >> 1); i++) {
            transformed[2 * i] = -y[i].getImaginary();
            transformed[2 * i + 1] = y[i].getReal() + transformed[2 * i - 1];
        }

        // Return result
        return transformed;
    }
}
