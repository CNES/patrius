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
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.interpolation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Expm1;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.exception.NonMonotonicSequenceException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test case for Neville interpolator.
 * <p>
 * The error of polynomial interpolation is f(z) - p(z) = f^(n)(zeta) * (z-x[0])(z-x[1])...(z-x[n-1]) / n! where f^(n)
 * is the n-th derivative of the approximated function and zeta is some point in the interval determined by x[] and z.
 * <p>
 * Since zeta is unknown, f^(n)(zeta) cannot be calculated. But we can bound it and use the absolute value upper bound
 * for estimates. For reference, see <b>Introduction to Numerical Analysis</b>, ISBN 038795452X, chapter 2.
 * 
 * @version $Id: NevilleInterpolatorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class NevilleInterpolatorTest {

    /**
     * Test of interpolator for the sine function.
     * <p>
     * |sin^(n)(zeta)| <= 1.0, zeta in [0, 2*PI]
     */
    @Test
    public void testSinFunction() {
        final UnivariateFunction f = new Sin();
        final UnivariateInterpolator interpolator = new NevilleInterpolator();
        double x[], y[], z, expected, result, tolerance;

        // 6 interpolating points on interval [0, 2*PI]
        final int n = 6;
        final double min = 0.0, max = 2 * FastMath.PI;
        x = new double[n];
        y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = min + i * (max - min) / n;
            y[i] = f.value(x[i]);
        }
        final double derivativebound = 1.0;
        final UnivariateFunction p = interpolator.interpolate(x, y);

        z = FastMath.PI / 4;
        expected = f.value(z);
        result = p.value(z);
        tolerance = MathLib.abs(derivativebound * this.partialerror(x, z));
        Assert.assertEquals(expected, result, tolerance);

        z = FastMath.PI * 1.5;
        expected = f.value(z);
        result = p.value(z);
        tolerance = MathLib.abs(derivativebound * this.partialerror(x, z));
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of interpolator for the exponential function.
     * <p>
     * |expm1^(n)(zeta)| <= e, zeta in [-1, 1]
     */
    @Test
    public void testExpm1Function() {
        final UnivariateFunction f = new Expm1();
        final UnivariateInterpolator interpolator = new NevilleInterpolator();
        double x[], y[], z, expected, result, tolerance;

        // 5 interpolating points on interval [-1, 1]
        final int n = 5;
        final double min = -1.0, max = 1.0;
        x = new double[n];
        y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = min + i * (max - min) / n;
            y[i] = f.value(x[i]);
        }
        final double derivativebound = FastMath.E;
        final UnivariateFunction p = interpolator.interpolate(x, y);

        z = 0.0;
        expected = f.value(z);
        result = p.value(z);
        tolerance = MathLib.abs(derivativebound * this.partialerror(x, z));
        Assert.assertEquals(expected, result, tolerance);

        z = 0.5;
        expected = f.value(z);
        result = p.value(z);
        tolerance = MathLib.abs(derivativebound * this.partialerror(x, z));
        Assert.assertEquals(expected, result, tolerance);

        z = -0.5;
        expected = f.value(z);
        result = p.value(z);
        tolerance = MathLib.abs(derivativebound * this.partialerror(x, z));
        Assert.assertEquals(expected, result, tolerance);
    }

    /**
     * Test of parameters for the interpolator.
     */
    @Test
    public void testParameters() {
        final UnivariateInterpolator interpolator = new NevilleInterpolator();

        try {
            // bad abscissas array
            final double x[] = { 1.0, 2.0, 2.0, 4.0 };
            final double y[] = { 0.0, 4.0, 4.0, 2.5 };
            final UnivariateFunction p = interpolator.interpolate(x, y);
            p.value(0.0);
            Assert.fail("Expecting NonMonotonicSequenceException - bad abscissas array");
        } catch (final NonMonotonicSequenceException ex) {
            // expected
        }
    }

    /**
     * Returns the partial error term (z-x[0])(z-x[1])...(z-x[n-1])/n!
     */
    protected double partialerror(final double x[], final double z) throws
                                                                   IllegalArgumentException {

        if (x.length < 1) {
            throw new IllegalArgumentException("Interpolation array cannot be empty.");
        }
        double out = 1;
        for (int i = 0; i < x.length; i++) {
            out *= (z - x[i]) / (i + 1);
        }
        return out;
    }
}
