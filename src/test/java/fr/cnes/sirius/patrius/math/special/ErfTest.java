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
 * VERSION:4.5:DM:DM-2422:27/05/2020:Ajout des methodes erfInv et erfcInv dans Erf 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.special;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * @version $Id: ErfTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ErfTest {
    @Test
    public void testErf0() {
        final double actual = Erf.erf(0.0);
        final double expected = 0.0;
        Assert.assertEquals(expected, actual, 1.0e-15);
        Assert.assertEquals(1 - expected, Erf.erfc(0.0), 1.0e-15);
    }

    @Test
    public void testErf1960() {
        final double x = 1.960 / MathLib.sqrt(2.0);
        double actual = Erf.erf(x);
        double expected = 0.95;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - actual, Erf.erfc(x), 1.0e-15);

        actual = Erf.erf(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - actual, Erf.erfc(-x), 1.0e-15);
    }

    @Test
    public void testErf2576() {
        final double x = 2.576 / MathLib.sqrt(2.0);
        double actual = Erf.erf(x);
        double expected = 0.99;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - actual, Erf.erfc(x), 1e-15);

        actual = Erf.erf(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - actual, Erf.erfc(-x), 1.0e-15);
    }

    @Test
    public void testErf2807() {
        final double x = 2.807 / MathLib.sqrt(2.0);
        double actual = Erf.erf(x);
        double expected = 0.995;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - actual, Erf.erfc(x), 1.0e-15);

        actual = Erf.erf(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - actual, Erf.erfc(-x), 1.0e-15);
    }

    @Test
    public void testErf3291() {
        final double x = 3.291 / MathLib.sqrt(2.0);
        double actual = Erf.erf(x);
        double expected = 0.999;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - expected, Erf.erfc(x), 1.0e-5);

        actual = Erf.erf(-x);
        expected = -expected;
        Assert.assertEquals(expected, actual, 1.0e-5);
        Assert.assertEquals(1 - expected, Erf.erfc(-x), 1.0e-5);
    }

    /**
     * MATH-301, MATH-456
     */
    @Test
    public void testLargeValues() {
        for (int i = 1; i < 200; i *= 10) {
            double result = Erf.erf(i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result > 0 && result <= 1);
            result = Erf.erf(-i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result >= -1 && result < 0);
            result = Erf.erfc(i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result >= 0 && result < 1);
            result = Erf.erfc(-i);
            Assert.assertFalse(Double.isNaN(result));
            Assert.assertTrue(result >= 1 && result <= 2);
        }
        Assert.assertEquals(-1, Erf.erf(Double.NEGATIVE_INFINITY), 0);
        Assert.assertEquals(1, Erf.erf(Double.POSITIVE_INFINITY), 0);
        Assert.assertEquals(2, Erf.erfc(Double.NEGATIVE_INFINITY), 0);
        Assert.assertEquals(0, Erf.erfc(Double.POSITIVE_INFINITY), 0);
    }

    /**
     * Compare Erf.erf against reference values computed using GCC 4.2.1 (Apple OSX packaged version)
     * erfl (extended precision erf).
     */
    @Test
    public void testErfGnu() {
        final double tol = 1E-15;
        final double[] gnuValues = new double[] { -1, -1, -1, -1, -1,
            -1, -1, -1, -0.99999999999999997848,
            -0.99999999999999264217, -0.99999999999846254017, -0.99999999980338395581, -0.99999998458274209971,
            -0.9999992569016276586, -0.99997790950300141459, -0.99959304798255504108, -0.99532226501895273415,
            -0.96610514647531072711, -0.84270079294971486948, -0.52049987781304653809, 0,
            0.52049987781304653809, 0.84270079294971486948, 0.96610514647531072711, 0.99532226501895273415,
            0.99959304798255504108, 0.99997790950300141459, 0.9999992569016276586, 0.99999998458274209971,
            0.99999999980338395581, 0.99999999999846254017, 0.99999999999999264217, 0.99999999999999997848,
            1, 1, 1, 1,
            1, 1, 1, 1 };
        double x = -10d;
        for (int i = 0; i < 41; i++) {
            Assert.assertEquals(gnuValues[i], Erf.erf(x), tol);
            x += 0.5d;
        }
    }

    /**
     * Compare Erf.erfc against reference values computed using GCC 4.2.1 (Apple OSX packaged version)
     * erfcl (extended precision erfc).
     */
    @Test
    public void testErfcGnu() {
        final double tol = 1E-15;
        final double[] gnuValues = new double[] { 2, 2, 2, 2, 2,
            2, 2, 2, 1.9999999999999999785,
            1.9999999999999926422, 1.9999999999984625402, 1.9999999998033839558, 1.9999999845827420998,
            1.9999992569016276586, 1.9999779095030014146, 1.9995930479825550411, 1.9953222650189527342,
            1.9661051464753107271, 1.8427007929497148695, 1.5204998778130465381, 1,
            0.47950012218695346194, 0.15729920705028513051, 0.033894853524689272893, 0.0046777349810472658333,
            0.00040695201744495893941, 2.2090496998585441366E-05, 7.4309837234141274516E-07,
            1.5417257900280018858E-08,
            1.966160441542887477E-10, 1.5374597944280348501E-12, 7.3578479179743980661E-15,
            2.1519736712498913103E-17,
            3.8421483271206474691E-20, 4.1838256077794144006E-23, 2.7766493860305691016E-26,
            1.1224297172982927079E-29,
            2.7623240713337714448E-33, 4.1370317465138102353E-37, 3.7692144856548799402E-41,
            2.0884875837625447567E-45 };
        double x = -10d;
        for (int i = 0; i < 41; i++) {
            Assert.assertEquals(gnuValues[i], Erf.erfc(x), tol);
            x += 0.5d;
        }
    }

    /**
     * Tests erfc against reference data computed using Maple reported in Marsaglia, G,,
     * "Evaluating the Normal Distribution," Journal of Statistical Software, July, 2004.
     * http//www.jstatsoft.org/v11/a05/paper
     */
    @Test
    public void testErfcMaple() {
        final double[][] ref = new double[][]
        { { 0.1, 4.60172162722971e-01 },
            { 1.2, 1.15069670221708e-01 },
            { 2.3, 1.07241100216758e-02 },
            { 3.4, 3.36929265676881e-04 },
            { 4.5, 3.39767312473006e-06 },
            { 5.6, 1.07175902583109e-08 },
            { 6.7, 1.04209769879652e-11 },
            { 7.8, 3.09535877195870e-15 },
            { 8.9, 2.79233437493966e-19 },
            { 10.0, 7.61985302416053e-24 },
            { 11.1, 6.27219439321703e-29 },
            { 12.2, 1.55411978638959e-34 },
            { 13.3, 1.15734162836904e-40 },
            { 14.4, 2.58717592540226e-47 },
            { 15.5, 1.73446079179387e-54 },
            { 16.6, 3.48454651995041e-62 }
        };
        for (int i = 0; i < 15; i++) {
            final double result = 0.5 * Erf.erfc(ref[i][0] / Math.sqrt(2));
            Assert.assertEquals(ref[i][1], result, 1E-15);
            TestUtils.assertRelativelyEquals(ref[i][1], result, 1E-13);
        }
    }

    /**
     * Test the implementation of Erf.erf(double, double) for consistency with results
     * obtained from Erf.erf(double) and Erf.erfc(double).
     */
    @Test
    public void testTwoArgumentErf() {
        final double[] xi = new double[] { -2.0, -1.0, -0.9, -0.1, 0.0, 0.1, 0.9, 1.0, 2.0 };
        for (final double x1 : xi) {
            for (final double x2 : xi) {
                final double a = Erf.erf(x1, x2);
                final double b = Erf.erf(x2) - Erf.erf(x1);
                final double c = Erf.erfc(x1) - Erf.erfc(x2);
                Assert.assertEquals(a, b, 1E-15);
                Assert.assertEquals(a, c, 1E-15);
            }
        }
    }

    /**
     * Test the inverse erf function.
     */
    @Test
    public void testErfInv() {
        // First case
        final double x1 = 0.5;
        Assert.assertEquals(Erf.erf(Erf.erfInv(x1)), x1, 1E-15);
        // Second case
        final double x2 = 0.9999;
        Assert.assertEquals(Erf.erf(Erf.erfInv(x2)), x2, 1E-15);
        // Third case
        final double x3 = 0.99999999;
        Assert.assertEquals(Erf.erf(Erf.erfInv(x3)), x3, 1E-15);
        // Fourth case
        final double x4 = 1.0;
        Assert.assertEquals(Erf.erf(Erf.erfInv(x4)), x4, 1E-15);
    }

    /**
     * Test the inverse erfc function.
     */
    @Test
    public void testErfcInv() {
        double x = 1.0;
        Assert.assertEquals(Erf.erfc(Erf.erfcInv(x)), x, 1E-15);
    }
}
