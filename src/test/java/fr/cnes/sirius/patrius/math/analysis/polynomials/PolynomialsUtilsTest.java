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
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation de fonctions 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.polynomials;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.IterativeLegendreGaussIntegrator;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Tests the PolynomialsUtils class.
 * 
 * @version $Id: PolynomialsUtilsTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class PolynomialsUtilsTest {

    @Test
    public void testFirstChebyshevPolynomials() {
        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(3), "-3 x + 4 x^3");
        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(2), "-1 + 2 x^2");
        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(1), "x");
        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(0), "1");

        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(7), "-7 x + 56 x^3 - 112 x^5 + 64 x^7");
        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(6), "-1 + 18 x^2 - 48 x^4 + 32 x^6");
        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(5), "5 x - 20 x^3 + 16 x^5");
        this.checkPolynomial(PolynomialsUtils.createChebyshevPolynomial(4), "1 - 8 x^2 + 8 x^4");

    }

    @Test
    public void testChebyshevBounds() {
        for (int k = 0; k < 12; ++k) {
            final PolynomialFunction Tk = PolynomialsUtils.createChebyshevPolynomial(k);
            for (double x = -1; x <= 1; x += 0.02) {
                Assert.assertTrue(k + " " + Tk.value(x), MathLib.abs(Tk.value(x)) < (1 + 1e-12));
            }
        }
    }

    @Test
    public void testChebyshevDifferentials() {
        for (int k = 0; k < 12; ++k) {

            final PolynomialFunction Tk0 = PolynomialsUtils.createChebyshevPolynomial(k);
            final PolynomialFunction Tk1 = Tk0.polynomialDerivative();
            final PolynomialFunction Tk2 = Tk1.polynomialDerivative();

            final PolynomialFunction g0 = new PolynomialFunction(new double[] { k * k });
            final PolynomialFunction g1 = new PolynomialFunction(new double[] { 0, -1 });
            final PolynomialFunction g2 = new PolynomialFunction(new double[] { 1, 0, -1 });

            final PolynomialFunction Tk0g0 = Tk0.multiply(g0);
            final PolynomialFunction Tk1g1 = Tk1.multiply(g1);
            final PolynomialFunction Tk2g2 = Tk2.multiply(g2);

            this.checkNullPolynomial(Tk0g0.add(Tk1g1.add(Tk2g2)));

        }
    }

    @Test
    public void testChebyshevOrthogonality() {
        final UnivariateFunction weight = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return 1 / MathLib.sqrt(1 - x * x);
            }
        };
        for (int i = 0; i < 10; ++i) {
            final PolynomialFunction pi = PolynomialsUtils.createChebyshevPolynomial(i);
            for (int j = 0; j <= i; ++j) {
                final PolynomialFunction pj = PolynomialsUtils.createChebyshevPolynomial(j);
                this.checkOrthogonality(pi, pj, weight, -0.9999, 0.9999, 1.5, 0.03);
            }
        }
    }

    @Test
    public void testFirstHermitePolynomials() {
        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(3), "-12 x + 8 x^3");
        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(2), "-2 + 4 x^2");
        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(1), "2 x");
        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(0), "1");

        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(7), "-1680 x + 3360 x^3 - 1344 x^5 + 128 x^7");
        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(6), "-120 + 720 x^2 - 480 x^4 + 64 x^6");
        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(5), "120 x - 160 x^3 + 32 x^5");
        this.checkPolynomial(PolynomialsUtils.createHermitePolynomial(4), "12 - 48 x^2 + 16 x^4");

    }

    @Test
    public void testHermiteDifferentials() {
        for (int k = 0; k < 12; ++k) {

            final PolynomialFunction Hk0 = PolynomialsUtils.createHermitePolynomial(k);
            final PolynomialFunction Hk1 = Hk0.polynomialDerivative();
            final PolynomialFunction Hk2 = Hk1.polynomialDerivative();

            final PolynomialFunction g0 = new PolynomialFunction(new double[] { 2 * k });
            final PolynomialFunction g1 = new PolynomialFunction(new double[] { 0, -2 });
            final PolynomialFunction g2 = new PolynomialFunction(new double[] { 1 });

            final PolynomialFunction Hk0g0 = Hk0.multiply(g0);
            final PolynomialFunction Hk1g1 = Hk1.multiply(g1);
            final PolynomialFunction Hk2g2 = Hk2.multiply(g2);

            this.checkNullPolynomial(Hk0g0.add(Hk1g1.add(Hk2g2)));

        }
    }

    @Test
    public void testHermiteOrthogonality() {
        final UnivariateFunction weight = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return MathLib.exp(-x * x);
            }
        };
        for (int i = 0; i < 10; ++i) {
            final PolynomialFunction pi = PolynomialsUtils.createHermitePolynomial(i);
            for (int j = 0; j <= i; ++j) {
                final PolynomialFunction pj = PolynomialsUtils.createHermitePolynomial(j);
                this.checkOrthogonality(pi, pj, weight, -50, 50, 1.5, 1.0e-8);
            }
        }
    }

    @Test
    public void testFirstLaguerrePolynomials() {
        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(3), 6l, "6 - 18 x + 9 x^2 - x^3");
        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(2), 2l, "2 - 4 x + x^2");
        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(1), 1l, "1 - x");
        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(0), 1l, "1");

        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(7), 5040l,
            "5040 - 35280 x + 52920 x^2 - 29400 x^3"
                    + " + 7350 x^4 - 882 x^5 + 49 x^6 - x^7");
        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(6), 720l,
            "720 - 4320 x + 5400 x^2 - 2400 x^3 + 450 x^4"
                    + " - 36 x^5 + x^6");
        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(5), 120l,
            "120 - 600 x + 600 x^2 - 200 x^3 + 25 x^4 - x^5");
        this.checkPolynomial(PolynomialsUtils.createLaguerrePolynomial(4), 24l,
            "24 - 96 x + 72 x^2 - 16 x^3 + x^4");

    }

    @Test
    public void testLaguerreDifferentials() {
        for (int k = 0; k < 12; ++k) {

            final PolynomialFunction Lk0 = PolynomialsUtils.createLaguerrePolynomial(k);
            final PolynomialFunction Lk1 = Lk0.polynomialDerivative();
            final PolynomialFunction Lk2 = Lk1.polynomialDerivative();

            final PolynomialFunction g0 = new PolynomialFunction(new double[] { k });
            final PolynomialFunction g1 = new PolynomialFunction(new double[] { 1, -1 });
            final PolynomialFunction g2 = new PolynomialFunction(new double[] { 0, 1 });

            final PolynomialFunction Lk0g0 = Lk0.multiply(g0);
            final PolynomialFunction Lk1g1 = Lk1.multiply(g1);
            final PolynomialFunction Lk2g2 = Lk2.multiply(g2);

            this.checkNullPolynomial(Lk0g0.add(Lk1g1.add(Lk2g2)));

        }
    }

    @Test
    public void testLaguerreOrthogonality() {
        final UnivariateFunction weight = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return MathLib.exp(-x);
            }
        };
        for (int i = 0; i < 10; ++i) {
            final PolynomialFunction pi = PolynomialsUtils.createLaguerrePolynomial(i);
            for (int j = 0; j <= i; ++j) {
                final PolynomialFunction pj = PolynomialsUtils.createLaguerrePolynomial(j);
                this.checkOrthogonality(pi, pj, weight, 0.0, 100.0, 0.99999, 1.0e-13);
            }
        }
    }

    @Test
    public void testFirstLegendrePolynomials() {
        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(3), 2l, "-3 x + 5 x^3");
        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(2), 2l, "-1 + 3 x^2");
        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(1), 1l, "x");
        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(0), 1l, "1");

        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(7), 16l, "-35 x + 315 x^3 - 693 x^5 + 429 x^7");
        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(6), 16l, "-5 + 105 x^2 - 315 x^4 + 231 x^6");
        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(5), 8l, "15 x - 70 x^3 + 63 x^5");
        this.checkPolynomial(PolynomialsUtils.createLegendrePolynomial(4), 8l, "3 - 30 x^2 + 35 x^4");

    }

    @Test
    public void testLegendreDifferentials() {
        for (int k = 0; k < 12; ++k) {

            final PolynomialFunction Pk0 = PolynomialsUtils.createLegendrePolynomial(k);
            final PolynomialFunction Pk1 = Pk0.polynomialDerivative();
            final PolynomialFunction Pk2 = Pk1.polynomialDerivative();

            final PolynomialFunction g0 = new PolynomialFunction(new double[] { k * (k + 1) });
            final PolynomialFunction g1 = new PolynomialFunction(new double[] { 0, -2 });
            final PolynomialFunction g2 = new PolynomialFunction(new double[] { 1, 0, -1 });

            final PolynomialFunction Pk0g0 = Pk0.multiply(g0);
            final PolynomialFunction Pk1g1 = Pk1.multiply(g1);
            final PolynomialFunction Pk2g2 = Pk2.multiply(g2);

            this.checkNullPolynomial(Pk0g0.add(Pk1g1.add(Pk2g2)));

        }
    }

    @Test
    public void testLegendreOrthogonality() {
        final UnivariateFunction weight = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return 1;
            }
        };
        for (int i = 0; i < 10; ++i) {
            final PolynomialFunction pi = PolynomialsUtils.createLegendrePolynomial(i);
            for (int j = 0; j <= i; ++j) {
                final PolynomialFunction pj = PolynomialsUtils.createLegendrePolynomial(j);
                this.checkOrthogonality(pi, pj, weight, -1, 1, 0.1, 1.0e-13);
            }
        }
    }

    @Test
    public void testHighDegreeLegendre() {
        PolynomialsUtils.createLegendrePolynomial(40);
        final double[] l40 = PolynomialsUtils.createLegendrePolynomial(40).getCoefficients();
        final double denominator = 274877906944d;
        final double[] numerators = new double[] {
            +34461632205d, -28258538408100d, +3847870979902950d, -207785032914759300d,
            +5929294332103310025d, -103301483474866556880d, +1197358103913226000200d, -9763073770369381232400d,
            +58171647881784229843050d, -260061484647976556945400d, +888315281771246239250340d,
            -2345767627188139419665400d,
            +4819022625419112503443050d, -7710436200670580005508880d, +9566652323054238154983240d,
            -9104813935044723209570256d,
            +6516550296251767619752905d, -3391858621221953912598660d, +1211378079007840683070950d,
            -265365894974690562152100d,
            +26876802183334044115405d
        };
        for (int i = 0; i < l40.length; ++i) {
            if (i % 2 == 0) {
                final double ci = numerators[i / 2] / denominator;
                Assert.assertEquals(ci, l40[i], MathLib.abs(ci) * 1e-15);
            } else {
                Assert.assertEquals(0, l40[i], 0);
            }
        }
    }

    @Test
    public void testJacobiLegendre() {
        for (int i = 0; i < 10; ++i) {
            final PolynomialFunction legendre = PolynomialsUtils.createLegendrePolynomial(i);
            final PolynomialFunction jacobi = PolynomialsUtils.createJacobiPolynomial(i, 0, 0);
            this.checkNullPolynomial(legendre.subtract(jacobi));
        }
    }

    @Test
    public void testJacobiEvaluationAt1() {
        for (int v = 0; v < 10; ++v) {
            for (int w = 0; w < 10; ++w) {
                for (int i = 0; i < 10; ++i) {
                    final PolynomialFunction jacobi = PolynomialsUtils.createJacobiPolynomial(i, v, w);
                    final double binomial = ArithmeticUtils.binomialCoefficient(v + i, i);
                    Assert.assertTrue(Precision.equals(binomial, jacobi.value(1.0), 1));
                }
            }
        }
    }

    @Test
    public void testJacobiOrthogonality() {
        for (int v = 0; v < 5; ++v) {
            for (int w = v; w < 5; ++w) {
                final int vv = v;
                final int ww = w;
                final UnivariateFunction weight = new UnivariateFunction(){
                    @Override
                    public double value(final double x) {
                        return MathLib.pow(1 - x, vv) * MathLib.pow(1 + x, ww);
                    }
                };
                for (int i = 0; i < 10; ++i) {
                    final PolynomialFunction pi = PolynomialsUtils.createJacobiPolynomial(i, v, w);
                    for (int j = 0; j <= i; ++j) {
                        final PolynomialFunction pj = PolynomialsUtils.createJacobiPolynomial(j, v, w);
                        this.checkOrthogonality(pi, pj, weight, -1, 1, 0.1, 1.0e-12);
                    }
                }
            }
        }
    }

    @Test
    public void testShift() {
        // f1(x) = 1 + x + 2 x^2
        final PolynomialFunction f1x = new PolynomialFunction(new double[] { 1, 1, 2 });

        final PolynomialFunction f1x1 = new PolynomialFunction(PolynomialsUtils.shift(f1x.getCoefficients(), 1));
        this.checkPolynomial(f1x1, "4 + 5 x + 2 x^2");

        final PolynomialFunction f1xM1 = new PolynomialFunction(PolynomialsUtils.shift(f1x.getCoefficients(), -1));
        this.checkPolynomial(f1xM1, "2 - 3 x + 2 x^2");

        final PolynomialFunction f1x3 = new PolynomialFunction(PolynomialsUtils.shift(f1x.getCoefficients(), 3));
        this.checkPolynomial(f1x3, "22 + 13 x + 2 x^2");

        // f2(x) = 2 + 3 x^2 + 8 x^3 + 121 x^5
        final PolynomialFunction f2x = new PolynomialFunction(new double[] { 2, 0, 3, 8, 0, 121 });

        final PolynomialFunction f2x1 = new PolynomialFunction(PolynomialsUtils.shift(f2x.getCoefficients(), 1));
        this.checkPolynomial(f2x1, "134 + 635 x + 1237 x^2 + 1218 x^3 + 605 x^4 + 121 x^5");

        final PolynomialFunction f2x3 = new PolynomialFunction(PolynomialsUtils.shift(f2x.getCoefficients(), 3));
        this.checkPolynomial(f2x3, "29648 + 49239 x + 32745 x^2 + 10898 x^3 + 1815 x^4 + 121 x^5");
    }

    /**
     * @description Evaluate the Chebyshev abscissas determination feature on a specified range with a specified degree.
     *              Also evaluate the exceptions cases.
     *
     * @testedMethod {@link PolynomialsUtils#getChebyshevAbscissas(double, double, int)}
     *
     * @testPassCriteria The Chebyshev abscissas values are the ones expected (bases on external contexts) and the
     *                   exceptions are returned as expected with the errors cases.
     */
    @Test
    public void testChebyshevAbscissas() {

        double[] abscissas = PolynomialsUtils.getChebyshevAbscissas(-1., 1., 1);
        Assert.assertEquals(1, abscissas.length);
        Assert.assertEquals(0., abscissas[0], 1e-14);

        // Context #1 extracted from an external example (degree 10)
        abscissas = PolynomialsUtils.getChebyshevAbscissas(-1., 1., 10);

        Assert.assertEquals(10, abscissas.length);
        Assert.assertEquals(0.987688341, abscissas[0], 1e-9);
        Assert.assertEquals(0.891006524, abscissas[1], 1e-9);
        Assert.assertEquals(0.707106781, abscissas[2], 1e-9);
        Assert.assertEquals(0.453990499, abscissas[3], 1e-9);
        Assert.assertEquals(0.156434465, abscissas[4], 1e-9);
        Assert.assertEquals(-0.156434465, abscissas[5], 1e-9);
        Assert.assertEquals(-0.453990499, abscissas[6], 1e-9);
        Assert.assertEquals(-0.707106781, abscissas[7], 1e-9);
        Assert.assertEquals(-0.891006524, abscissas[8], 1e-9);
        Assert.assertEquals(-0.987688341, abscissas[9], 1e-9);

        // Context #2 extracted from an external example (degree 5)
        abscissas = PolynomialsUtils.getChebyshevAbscissas(-1., 3., 5);

        Assert.assertEquals(5, abscissas.length);
        Assert.assertEquals(2.902113, abscissas[0], 1e-6);
        Assert.assertEquals(2.175571, abscissas[1], 1e-6);
        Assert.assertEquals(1., abscissas[2], 1e-6);
        Assert.assertEquals(-0.175571, abscissas[3], 1e-6);
        Assert.assertEquals(-0.902113, abscissas[4], 1e-6);

        // Try to use call a 0 degree Chebyshev polynomial (should fail)
        try {
            PolynomialsUtils.getChebyshevAbscissas(-1., 1., 0);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Try a use an incorrect range
        try {
            PolynomialsUtils.getChebyshevAbscissas(1., 1., 5);
            Assert.fail();
        } catch (final MathIllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    private void checkPolynomial(final PolynomialFunction p, final long denominator, final String reference) {
        final PolynomialFunction q = new PolynomialFunction(new double[] { denominator });
        Assert.assertEquals(reference, p.multiply(q).toString());
    }

    private void checkPolynomial(final PolynomialFunction p, final String reference) {
        Assert.assertEquals(reference, p.toString());
    }

    private void checkNullPolynomial(final PolynomialFunction p) {
        for (final double coefficient : p.getCoefficients()) {
            Assert.assertEquals(0, coefficient, 1e-13);
        }
    }

    private void checkOrthogonality(final PolynomialFunction p1,
            final PolynomialFunction p2,
            final UnivariateFunction weight,
            final double a, final double b,
            final double nonZeroThreshold,
            final double zeroThreshold) {
        final UnivariateFunction f = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                return weight.value(x) * p1.value(x) * p2.value(x);
            }
        };
        final double dotProduct =
            new IterativeLegendreGaussIntegrator(5, 1.0e-9, 1.0e-8, 2, 15).integrate(1000000, f, a, b);
        if (p1.degree() == p2.degree()) {
            // integral should be non-zero
            Assert.assertTrue("I(" + p1.degree() + ", " + p2.degree() + ") = " + dotProduct,
                MathLib.abs(dotProduct) > nonZeroThreshold);
        } else {
            // integral should be zero
            Assert.assertEquals("I(" + p1.degree() + ", " + p2.degree() + ") = " + dotProduct,
                0.0, MathLib.abs(dotProduct), zeroThreshold);
        }
    }
}
