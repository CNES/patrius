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
package fr.cnes.sirius.patrius.math.analysis.function;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Test for class {@link HarmonicOscillator}.
 */
public class HarmonicOscillatorTest {
    private final double EPS = Math.ulp(1d);

    @Test
    public void testSomeValues() {
        final double a = -1.2;
        final double w = 0.34;
        final double p = 5.6;
        final UnivariateFunction f = new HarmonicOscillator(a, w, p);

        final double d = 0.12345;
        for (int i = 0; i < 10; i++) {
            final double v = i * d;
            Assert.assertEquals(a * MathLib.cos(w * v + p), f.value(v), 0);
        }
    }

    @Test
    public void testDerivative() {
        final double a = -1.2;
        final double w = 0.34;
        final double p = 5.6;
        final HarmonicOscillator f = new HarmonicOscillator(a, w, p);

        for (int maxOrder = 0; maxOrder < 6; ++maxOrder) {
            final double d = 0.12345;
            for (int i = 0; i < 10; i++) {
                final double v = i * d;
                final DerivativeStructure h = f.value(new DerivativeStructure(1, maxOrder, 0, v));
                for (int k = 0; k <= maxOrder; ++k) {
                    final double trigo;
                    switch (k % 4) {
                        case 0:
                            trigo = +MathLib.cos(w * v + p);
                            break;
                        case 1:
                            trigo = -MathLib.sin(w * v + p);
                            break;
                        case 2:
                            trigo = -MathLib.cos(w * v + p);
                            break;
                        default:
                            trigo = +MathLib.sin(w * v + p);
                            break;
                    }
                    Assert.assertEquals(a * MathLib.pow(w, k) * trigo,
                        h.getPartialDerivative(k),
                        Precision.EPSILON);
                }
            }
        }
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage1() {
        final HarmonicOscillator.Parametric g = new HarmonicOscillator.Parametric();
        g.value(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage2() {
        final HarmonicOscillator.Parametric g = new HarmonicOscillator.Parametric();
        g.value(0, new double[] { 0 });
    }

    @Test(expected = NullArgumentException.class)
    public void testParametricUsage3() {
        final HarmonicOscillator.Parametric g = new HarmonicOscillator.Parametric();
        g.gradient(0, null);
    }

    @Test(expected = DimensionMismatchException.class)
    public void testParametricUsage4() {
        final HarmonicOscillator.Parametric g = new HarmonicOscillator.Parametric();
        g.gradient(0, new double[] { 0 });
    }

    @Test
    public void testParametricValue() {
        final double amplitude = 2;
        final double omega = 3;
        final double phase = 4;
        final HarmonicOscillator f = new HarmonicOscillator(amplitude, omega, phase);

        final HarmonicOscillator.Parametric g = new HarmonicOscillator.Parametric();
        Assert.assertEquals(f.value(-1), g.value(-1, new double[] { amplitude, omega, phase }), 0);
        Assert.assertEquals(f.value(0), g.value(0, new double[] { amplitude, omega, phase }), 0);
        Assert.assertEquals(f.value(2), g.value(2, new double[] { amplitude, omega, phase }), 0);
    }

    @Test
    public void testParametricGradient() {
        final double amplitude = 2;
        final double omega = 3;
        final double phase = 4;
        final HarmonicOscillator.Parametric f = new HarmonicOscillator.Parametric();

        final double x = 1;
        final double[] grad = f.gradient(1, new double[] { amplitude, omega, phase });
        final double xTimesOmegaPlusPhase = omega * x + phase;
        final double a = MathLib.cos(xTimesOmegaPlusPhase);
        Assert.assertEquals(a, grad[0], this.EPS);
        final double w = -amplitude * x * MathLib.sin(xTimesOmegaPlusPhase);
        Assert.assertEquals(w, grad[1], this.EPS);
        final double p = -amplitude * MathLib.sin(xTimesOmegaPlusPhase);
        Assert.assertEquals(p, grad[2], this.EPS);
    }
}
