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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.function;

import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * <a href="http://en.wikipedia.org/wiki/Harmonic_oscillator">
 * simple harmonic oscillator</a> function.
 * 
 * @since 3.0
 * @version $Id: HarmonicOscillator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class HarmonicOscillator implements UnivariateDifferentiableFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = 7956465468142703801L;
    /** Amplitude. */
    private final double amplitude;
    /** Angular frequency. */
    private final double omega;
    /** Phase. */
    private final double phase;

    /**
     * Harmonic oscillator function.
     * 
     * @param amplitudeIn
     *        Amplitude.
     * @param omegaIn
     *        Angular frequency.
     * @param phaseIn
     *        Phase.
     */
    public HarmonicOscillator(final double amplitudeIn,
        final double omegaIn,
        final double phaseIn) {
        this.amplitude = amplitudeIn;
        this.omega = omegaIn;
        this.phase = phaseIn;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x) {
        return value(this.omega * x + this.phase, this.amplitude);
    }

    /**
     * @param xTimesOmegaPlusPhase
     *        {@code omega * x + phase}.
     * @param amplitude
     *        Amplitude.
     * @return the value of the harmonic oscillator function at {@code x}.
     */
    private static double value(final double xTimesOmegaPlusPhase,
                                final double amplitude) {
        return amplitude * MathLib.cos(xTimesOmegaPlusPhase);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        // Init
        final double x = t.getValue();
        final double[] f = new double[t.getOrder() + 1];

        final double alpha = this.omega * x + this.phase;
        f[0] = this.amplitude * MathLib.cos(alpha);
        if (f.length > 1) {
            f[1] = -this.amplitude * this.omega * MathLib.sin(alpha);
            final double mo2 = -this.omega * this.omega;
            // Compute factor for if the order is >= 2
            for (int i = 2; i < f.length; ++i) {
                f[i] = mo2 * f[i - 2];
            }
        }

        // return derivative structure
        return t.compose(f);

    }

    /**
     * Parametric function where the input array contains the parameters of
     * the harmonic oscillator function, ordered as follows:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
     */
    public static class Parametric implements ParametricUnivariateFunction {
        /**
         * Computes the value of the harmonic oscillator at {@code x}.
         * 
         * @param x
         *        Value for which the function must be computed.
         * @param param
         *        Values of norm, mean and standard deviation.
         * @return the value of the function.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 3.
         */
        @Override
        public double value(final double x, final double... param) {
            validateParameters(param);
            return HarmonicOscillator.value(x * param[1] + param[2], param[0]);
        }

        /**
         * Computes the value of the gradient at {@code x}.
         * The components of the gradient vector are the partial
         * derivatives of the function with respect to each of the <em>parameters</em> (amplitude, angular frequency and
         * phase).
         * 
         * @param x
         *        Value at which the gradient must be computed.
         * @param param
         *        Values of amplitude, angular frequency and phase.
         * @return the gradient vector at {@code x}.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 3.
         */
        @Override
        public double[] gradient(final double x, final double... param) {
            validateParameters(param);

            final double amplitude = param[0];
            final double omega = param[1];
            final double phase = param[2];

            final double xTimesOmegaPlusPhase = omega * x + phase;
            final double a = HarmonicOscillator.value(xTimesOmegaPlusPhase, 1);
            final double p = -amplitude * MathLib.sin(xTimesOmegaPlusPhase);
            final double w = p * x;

            return new double[] { a, w, p };
        }

        /**
         * Validates parameters to ensure they are appropriate for the evaluation of
         * the {@link #value(double,double[])} and {@link #gradient(double,double[])} methods.
         * 
         * @param param
         *        Values of norm, mean and standard deviation.
         * @throws NullArgumentException
         *         if {@code param} is {@code null}.
         * @throws DimensionMismatchException
         *         if the size of {@code param} is
         *         not 3.
         */
        private static void validateParameters(final double[] param) {
            if (param == null) {
                throw new NullArgumentException();
            }
            if (param.length != 3) {
                throw new DimensionMismatchException(param.length, 3);
            }
        }
    }
}
