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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.function;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Commons-Math code

/**
 * <a href="http://en.wikipedia.org/wiki/Sinc_function">Sinc</a> function,
 * defined by
 * 
 * <pre>
 * <code>
 *   sinc(x) = 1            if x = 0,
 *             sin(x) / x   otherwise.
 * </code>
 * </pre>
 * 
 * @since 3.0
 * @version $Id: Sinc.java 18108 2017-10-04 06:45:27Z bignon $
 */
@SuppressWarnings("PMD.ShortClassName")
public class Sinc implements UnivariateDifferentiableFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = -3755126425479754072L;

    /**
     * Value below which the computations are done using Taylor series.
     * <p>
     * The Taylor series for sinc even order derivatives are:
     * 
     * <pre>
     * d^(2n)sinc/dx^(2n)     = Sum_(k>=0) (-1)^(n+k) / ((2k)!(2n+2k+1)) x^(2k)
     *                        = (-1)^n     [ 1/(2n+1) - x^2/(4n+6) + x^4/(48n+120) - x^6/(1440n+5040) + O(x^8) ]
     * </pre>
     * 
     * </p>
     * <p>
     * The Taylor series for sinc odd order derivatives are:
     * 
     * <pre>
     * d^(2n+1)sinc/dx^(2n+1) = Sum_(k>=0) (-1)^(n+k+1) / ((2k+1)!(2n+2k+3)) x^(2k+1)
     *                        = (-1)^(n+1) [ x/(2n+3) - x^3/(12n+30) + x^5/(240n+840) - x^7/(10080n+45360) + O(x^9) ]
     * </pre>
     * 
     * </p>
     * <p>
     * So the ratio of the fourth term with respect to the first term is always smaller than x^6/720, for all derivative
     * orders. This implies that neglecting this term and using only the first three terms induces a relative error
     * bounded by x^6/720. The SHORTCUT value is chosen such that this relative error is below double precision accuracy
     * when |x| <= SHORTCUT.
     * </p>
     */
    private static final double SHORTCUT = 6.0e-3;

    /** For normalized sinc function. */
    private final boolean normalized;

    /**
     * The sinc function, {@code sin(x) / x}.
     */
    public Sinc() {
        this(false);
    }

    /**
     * Instantiates the sinc function.
     * 
     * @param normalizedIn
     *        If {@code true}, the function is <code> sin(&pi;x) / &pi;x</code>, otherwise {@code sin(x) / x}.
     */
    public Sinc(final boolean normalizedIn) {
        this.normalized = normalizedIn;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x) {
        final double scaledX = this.normalized ? FastMath.PI * x : x;
        if (MathLib.abs(scaledX) <= SHORTCUT) {
            // use Taylor series
            final double scaledX2 = scaledX * scaledX;
            return ((scaledX2 - 20) * scaledX2 + 120) / 120;
        } else {
            // use definition expression
            return MathLib.sin(scaledX) / scaledX;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @since 3.1
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        final double scaledX = (this.normalized ? FastMath.PI : 1) * t.getValue();
        final double scaledX2 = scaledX * scaledX;

        final double[] f = new double[t.getOrder() + 1];

        if (MathLib.abs(scaledX) <= SHORTCUT) {

            for (int i = 0; i < f.length; ++i) {
                final int k = i / 2;
                if ((i & 0x1) == 0) {
                    // even derivation order
                    f[i] = (((k & 0x1) == 0) ? 1 : -1) *
                        (1.0 / (i + 1) - scaledX2 * (1.0 / (2 * i + 6) - scaledX2 / (24 * i + 120)));
                } else {
                    // odd derivation order
                    f[i] = (((k & 0x1) == 0) ? -scaledX : scaledX) *
                        (1.0 / (i + 2) - scaledX2 * (1.0 / (6 * i + 24) - scaledX2 / (120 * i + 720)));
                }
            }

        } else {

            final double inv = 1 / scaledX;
            final double[] sincos = MathLib.sinAndCos(scaledX);
            final double sin = sincos[0];
            final double cos = sincos[1];

            f[0] = inv * sin;

            // the nth order derivative of sinc has the form:
            // dn(sinc(x)/dxn = [S_n(x) sin(x) + C_n(x) cos(x)] / x^(n+1)
            // where S_n(x) is an even polynomial with degree n-1 or n (depending on parity)
            // and C_n(x) is an odd polynomial with degree n-1 or n (depending on parity)
            // S_0(x) = 1, S_1(x) = -1, S_2(x) = -x^2 + 2, S_3(x) = 3x^2 - 6...
            // C_0(x) = 0, C_1(x) = x, C_2(x) = -2x, C_3(x) = -x^3 + 6x...
            // the general recurrence relations for S_n and C_n are:
            // S_n(x) = x S_(n-1)'(x) - n S_(n-1)(x) - x C_(n-1)(x)
            // C_n(x) = x C_(n-1)'(x) - n C_(n-1)(x) + x S_(n-1)(x)
            // as per polynomials parity, we can store both S_n and C_n in the same array
            final double[] sc = new double[f.length];
            sc[0] = 1;

            double coeff = inv;
            for (int n = 1; n < f.length; ++n) {

                double s = 0;
                double c = 0;

                // update and evaluate polynomials S_n(x) and C_n(x)
                final int kStart;
                if ((n & 0x1) == 0) {
                    // even derivation order, S_n is degree n and C_n is degree n-1
                    sc[n] = 0;
                    kStart = n;
                } else {
                    // odd derivation order, S_n is degree n-1 and C_n is degree n
                    sc[n] = sc[n - 1];
                    c = sc[n];
                    kStart = n - 1;
                }

                // in this loop, k is always even
                for (int k = kStart; k > 1; k -= 2) {

                    // sine part
                    sc[k] = (k - n) * sc[k] - sc[k - 1];
                    s = s * scaledX2 + sc[k];

                    // cosine part
                    sc[k - 1] = (k - 1 - n) * sc[k - 1] + sc[k - 2];
                    c = c * scaledX2 + sc[k - 1];

                }
                sc[0] *= -n;
                s = s * scaledX2 + sc[0];

                coeff *= inv;
                f[n] = coeff * (s * sin + c * scaledX * cos);

            }

        }

        if (this.normalized) {
            double scale = FastMath.PI;
            for (int i = 1; i < f.length; ++i) {
                f[i] *= scale;
                scale *= FastMath.PI;
            }
        }

        return t.compose(f);

    }

    // CHECKSTYLE: resume MagicNumber check
}
