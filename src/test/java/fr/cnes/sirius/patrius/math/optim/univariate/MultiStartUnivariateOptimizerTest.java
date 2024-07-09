/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.univariate;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.random.JDKRandomGenerator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class MultiStartUnivariateOptimizerTest {
    @Test(expected = MathIllegalStateException.class)
    public void testMissingMaxEval() {
        final UnivariateOptimizer underlying = new BrentOptimizer(1e-10, 1e-14);
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(44428400075l);
        final MultiStartUnivariateOptimizer optimizer = new MultiStartUnivariateOptimizer(underlying, 10, g);
        optimizer.optimize(new UnivariateObjectiveFunction(new Sin()),
            GoalType.MINIMIZE,
            new SearchInterval(-1, 1));
    }

    @Test(expected = MathIllegalStateException.class)
    public void testMissingSearchInterval() {
        final UnivariateOptimizer underlying = new BrentOptimizer(1e-10, 1e-14);
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(44428400075l);
        final MultiStartUnivariateOptimizer optimizer = new MultiStartUnivariateOptimizer(underlying, 10, g);
        optimizer.optimize(new MaxEval(300),
            new UnivariateObjectiveFunction(new Sin()),
            GoalType.MINIMIZE);
    }

    @Test
    public void testSinMin() {
        final UnivariateFunction f = new Sin();
        final UnivariateOptimizer underlying = new BrentOptimizer(1e-10, 1e-14);
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(44428400075l);
        final MultiStartUnivariateOptimizer optimizer = new MultiStartUnivariateOptimizer(underlying, 10, g);
        optimizer.optimize(new MaxEval(300),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(-100.0, 100.0));
        final UnivariatePointValuePair[] optima = optimizer.getOptima();
        for (int i = 1; i < optima.length; ++i) {
            final double d = (optima[i].getPoint() - optima[i - 1].getPoint()) / (2 * FastMath.PI);
            Assert.assertTrue(MathLib.abs(d - MathLib.rint(d)) < 1.0e-8);
            Assert.assertEquals(-1.0, f.value(optima[i].getPoint()), 1.0e-10);
            Assert.assertEquals(f.value(optima[i].getPoint()), optima[i].getValue(), 1.0e-10);
        }
        Assert.assertTrue(optimizer.getEvaluations() > 200);
        Assert.assertTrue(optimizer.getEvaluations() < 300);
    }

    @Test
    public void testQuinticMin() {
        // The quintic function has zeros at 0, +-0.5 and +-1.
        // The function has extrema (first derivative is zero) at 0.27195613 and 0.82221643,
        final UnivariateFunction f = new QuinticFunction();
        final UnivariateOptimizer underlying = new BrentOptimizer(1e-9, 1e-14);
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(4312000053L);
        final MultiStartUnivariateOptimizer optimizer = new MultiStartUnivariateOptimizer(underlying, 5, g);

        final UnivariatePointValuePair optimum = optimizer.optimize(new MaxEval(300),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(-0.3, -0.2));
        Assert.assertEquals(-0.27195613, optimum.getPoint(), 1e-9);
        Assert.assertEquals(-0.0443342695, optimum.getValue(), 1e-9);

        final UnivariatePointValuePair[] optima = optimizer.getOptima();
        for (final UnivariatePointValuePair element : optima) {
            Assert.assertEquals(f.value(element.getPoint()), element.getValue(), 1e-9);
        }
        Assert.assertTrue(optimizer.getEvaluations() >= 50);
        Assert.assertTrue(optimizer.getEvaluations() <= 100);
    }

    @Test
    public void testBadFunction() {
        final UnivariateFunction f = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                if (x < 0) {
                    throw new LocalException();
                }
                return 0;
            }
        };
        final UnivariateOptimizer underlying = new BrentOptimizer(1e-9, 1e-14);
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(4312000053L);
        final MultiStartUnivariateOptimizer optimizer = new MultiStartUnivariateOptimizer(underlying, 5, g);

        try {
            optimizer.optimize(new MaxEval(300),
                new UnivariateObjectiveFunction(f),
                GoalType.MINIMIZE,
                new SearchInterval(-0.3, -0.2));
            Assert.fail();
        } catch (final LocalException e) {
            // Expected.
        }

        // Ensure that the exception was thrown because no optimum was found.
        Assert.assertTrue(optimizer.getOptima()[0] == null);
    }

    private static class LocalException extends RuntimeException {
        private static final long serialVersionUID = 1194682757034350629L;
    }

}
