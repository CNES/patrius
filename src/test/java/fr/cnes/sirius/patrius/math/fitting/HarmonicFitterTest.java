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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.fitting;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.function.HarmonicOscillator;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

public class HarmonicFitterTest {
    @Test(expected = NumberIsTooSmallException.class)
    public void testPreconditions1() {
        final HarmonicFitter fitter =
            new HarmonicFitter(new LevenbergMarquardtOptimizer());

        fitter.fit();
    }

    @Test
    public void testNoError() {
        final double a = 0.2;
        final double w = 3.4;
        final double p = 4.1;
        final HarmonicOscillator f = new HarmonicOscillator(a, w, p);

        final HarmonicFitter fitter =
            new HarmonicFitter(new LevenbergMarquardtOptimizer());
        for (double x = 0.0; x < 1.3; x += 0.01) {
            fitter.addObservedPoint(1, x, f.value(x));
        }

        final double[] fitted = fitter.fit();
        Assert.assertEquals(a, fitted[0], 1.0e-13);
        Assert.assertEquals(w, fitted[1], 1.0e-13);
        Assert.assertEquals(p, MathUtils.normalizeAngle(fitted[2], p), 1e-13);

        final HarmonicOscillator ff = new HarmonicOscillator(fitted[0], fitted[1], fitted[2]);

        for (double x = -1.0; x < 1.0; x += 0.01) {
            Assert.assertTrue(MathLib.abs(f.value(x) - ff.value(x)) < 1e-13);
        }
    }

    @Test
    public void test1PercentError() {
        final Random randomizer = new Random(64925784252l);
        final double a = 0.2;
        final double w = 3.4;
        final double p = 4.1;
        final HarmonicOscillator f = new HarmonicOscillator(a, w, p);

        final HarmonicFitter fitter =
            new HarmonicFitter(new LevenbergMarquardtOptimizer());
        for (double x = 0.0; x < 10.0; x += 0.1) {
            fitter.addObservedPoint(1, x,
                f.value(x) + 0.01 * randomizer.nextGaussian());
        }

        final double[] fitted = fitter.fit();
        Assert.assertEquals(a, fitted[0], 7.6e-4);
        Assert.assertEquals(w, fitted[1], 2.7e-3);
        Assert.assertEquals(p, MathUtils.normalizeAngle(fitted[2], p), 1.3e-2);
    }

    @Test
    public void testTinyVariationsData() {
        final Random randomizer = new Random(64925784252l);

        final HarmonicFitter fitter =
            new HarmonicFitter(new LevenbergMarquardtOptimizer());
        for (double x = 0.0; x < 10.0; x += 0.1) {
            fitter.addObservedPoint(1, x, 1e-7 * randomizer.nextGaussian());
        }

        fitter.fit();
        // This test serves to cover the part of the code of "guessAOmega"
        // when the algorithm using integrals fails.
    }

    @Test
    public void testInitialGuess() {
        final Random randomizer = new Random(45314242l);
        final double a = 0.2;
        final double w = 3.4;
        final double p = 4.1;
        final HarmonicOscillator f = new HarmonicOscillator(a, w, p);

        final HarmonicFitter fitter =
            new HarmonicFitter(new LevenbergMarquardtOptimizer());
        for (double x = 0.0; x < 10.0; x += 0.1) {
            fitter.addObservedPoint(1, x,
                f.value(x) + 0.01 * randomizer.nextGaussian());
        }

        final double[] fitted = fitter.fit(new double[] { 0.15, 3.6, 4.5 });
        Assert.assertEquals(a, fitted[0], 1.2e-3);
        Assert.assertEquals(w, fitted[1], 3.3e-3);
        Assert.assertEquals(p, MathUtils.normalizeAngle(fitted[2], p), 1.7e-2);
    }

    @Test
    public void testUnsorted() {
        final Random randomizer = new Random(64925784252l);
        final double a = 0.2;
        final double w = 3.4;
        final double p = 4.1;
        final HarmonicOscillator f = new HarmonicOscillator(a, w, p);

        final HarmonicFitter fitter =
            new HarmonicFitter(new LevenbergMarquardtOptimizer());

        // build a regularly spaced array of measurements
        final int size = 100;
        final double[] xTab = new double[size];
        final double[] yTab = new double[size];
        for (int i = 0; i < size; ++i) {
            xTab[i] = 0.1 * i;
            yTab[i] = f.value(xTab[i]) + 0.01 * randomizer.nextGaussian();
        }

        // shake it
        for (int i = 0; i < size; ++i) {
            final int i1 = randomizer.nextInt(size);
            final int i2 = randomizer.nextInt(size);
            final double xTmp = xTab[i1];
            final double yTmp = yTab[i1];
            xTab[i1] = xTab[i2];
            yTab[i1] = yTab[i2];
            xTab[i2] = xTmp;
            yTab[i2] = yTmp;
        }

        // pass it to the fitter
        for (int i = 0; i < size; ++i) {
            fitter.addObservedPoint(1, xTab[i], yTab[i]);
        }

        final double[] fitted = fitter.fit();
        Assert.assertEquals(a, fitted[0], 7.6e-4);
        Assert.assertEquals(w, fitted[1], 3.5e-3);
        Assert.assertEquals(p, MathUtils.normalizeAngle(fitted[2], p), 1.5e-2);
    }

    @Test(expected = MathIllegalStateException.class)
    public void testMath844() {
        final double[] y = { 0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1, i, y[i]);
        }

        new HarmonicFitter.ParameterGuesser(points);
    }
}
