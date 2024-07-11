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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class SimplexOptimizerMultiDirectionalTest {
    @Test
    public void testMinimize1() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-11, 1e-30);
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(200),
            new ObjectiveFunction(fourExtrema),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { -3, 0 }),
            new MultiDirectionalSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xM, optimum.getPoint()[0], 4e-6);
        Assert.assertEquals(fourExtrema.yP, optimum.getPoint()[1], 3e-6);
        Assert.assertEquals(fourExtrema.valueXmYp, optimum.getValue(), 8e-13);
        Assert.assertTrue(optimizer.getEvaluations() > 120);
        Assert.assertTrue(optimizer.getEvaluations() < 150);
    }

    @Test
    public void testMinimize2() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-11, 1e-30);
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(200),
            new ObjectiveFunction(fourExtrema),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 1, 0 }),
            new MultiDirectionalSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xP, optimum.getPoint()[0], 2e-8);
        Assert.assertEquals(fourExtrema.yM, optimum.getPoint()[1], 3e-6);
        Assert.assertEquals(fourExtrema.valueXpYm, optimum.getValue(), 2e-12);
        Assert.assertTrue(optimizer.getEvaluations() > 120);
        Assert.assertTrue(optimizer.getEvaluations() < 150);
    }

    @Test
    public void testMaximize1() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-11, 1e-30);
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(200),
            new ObjectiveFunction(fourExtrema),
            GoalType.MAXIMIZE,
            new InitialGuess(new double[] { -3.0, 0.0 }),
            new MultiDirectionalSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xM, optimum.getPoint()[0], 7e-7);
        Assert.assertEquals(fourExtrema.yM, optimum.getPoint()[1], 3e-7);
        Assert.assertEquals(fourExtrema.valueXmYm, optimum.getValue(), 2e-14);
        Assert.assertTrue(optimizer.getEvaluations() > 120);
        Assert.assertTrue(optimizer.getEvaluations() < 150);
    }

    @Test
    public void testMaximize2() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(new SimpleValueChecker(1e-15, 1e-30));
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(200),
            new ObjectiveFunction(fourExtrema),
            GoalType.MAXIMIZE,
            new InitialGuess(new double[] { 1, 0 }),
            new MultiDirectionalSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xP, optimum.getPoint()[0], 2e-8);
        Assert.assertEquals(fourExtrema.yP, optimum.getPoint()[1], 3e-6);
        Assert.assertEquals(fourExtrema.valueXpYp, optimum.getValue(), 2e-12);
        Assert.assertTrue(optimizer.getEvaluations() > 180);
        Assert.assertTrue(optimizer.getEvaluations() < 220);
    }

    @Test
    public void testRosenbrock() {
        final MultivariateFunction rosenbrock = new MultivariateFunction(){
            @Override
            public double value(final double[] x) {
                ++SimplexOptimizerMultiDirectionalTest.this.count;
                final double a = x[1] - x[0] * x[0];
                final double b = 1.0 - x[0];
                return 100 * a * a + b * b;
            }
        };

        this.count = 0;
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-3);
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            new ObjectiveFunction(rosenbrock),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { -1.2, 1 }),
            new MultiDirectionalSimplex(new double[][] {
                { -1.2, 1.0 },
                { 0.9, 1.2 },
                { 3.5, -2.3 } }));

        Assert.assertEquals(this.count, optimizer.getEvaluations());
        Assert.assertTrue(optimizer.getEvaluations() > 50);
        Assert.assertTrue(optimizer.getEvaluations() < 100);
        Assert.assertTrue(optimum.getValue() > 1e-2);
    }

    @Test
    public void testPowell() {
        final MultivariateFunction powell = new MultivariateFunction(){
            @Override
            public double value(final double[] x) {
                ++SimplexOptimizerMultiDirectionalTest.this.count;
                final double a = x[0] + 10 * x[1];
                final double b = x[2] - x[3];
                final double c = x[1] - 2 * x[2];
                final double d = x[0] - x[3];
                return a * a + 5 * b * b + c * c * c * c + 10 * d * d * d * d;
            }
        };

        this.count = 0;
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-3);
        final PointValuePair optimum = optimizer.optimize(new MaxEval(1000),
            new ObjectiveFunction(powell),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 3, -1, 0, 1 }),
            new MultiDirectionalSimplex(4));
        Assert.assertEquals(this.count, optimizer.getEvaluations());
        Assert.assertTrue(optimizer.getEvaluations() > 800);
        Assert.assertTrue(optimizer.getEvaluations() < 900);
        Assert.assertTrue(optimum.getValue() > 1e-2);
    }

    @Test
    public void testMath283() {
        // fails because MultiDirectional.iterateSimplex is looping forever
        // the while(true) should be replaced with a convergence check
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-14, 1e-14);
        final Gaussian2D function = new Gaussian2D(0, 0, 1);
        final PointValuePair estimate = optimizer.optimize(new MaxEval(1000),
            new ObjectiveFunction(function),
            GoalType.MAXIMIZE,
            new InitialGuess(function.getMaximumPosition()),
            new MultiDirectionalSimplex(2));
        final double EPSILON = 1e-5;
        final double expectedMaximum = function.getMaximum();
        final double actualMaximum = estimate.getValue();
        Assert.assertEquals(expectedMaximum, actualMaximum, EPSILON);

        final double[] expectedPosition = function.getMaximumPosition();
        final double[] actualPosition = estimate.getPoint();
        Assert.assertEquals(expectedPosition[0], actualPosition[0], EPSILON);
        Assert.assertEquals(expectedPosition[1], actualPosition[1], EPSILON);
    }

    private static class FourExtrema implements MultivariateFunction {
        // The following function has 4 local extrema.
        final double xM = -3.841947088256863675365;
        final double yM = -1.391745200270734924416;
        final double xP = 0.2286682237349059125691;
        final double yP = -this.yM;
        final double valueXmYm = 0.2373295333134216789769; // Local maximum.
        final double valueXmYp = -this.valueXmYm; // Local minimum.
        final double valueXpYm = -0.7290400707055187115322; // Global minimum.
        final double valueXpYp = -this.valueXpYm; // Global maximum.

        @Override
        public double value(final double[] variables) {
            final double x = variables[0];
            final double y = variables[1];
            return (x == 0 || y == 0) ? 0 :
                MathLib.atan(x) * MathLib.atan(x + 2) * MathLib.atan(y) * MathLib.atan(y) / (x * y);
        }
    }

    private static class Gaussian2D implements MultivariateFunction {
        private final double[] maximumPosition;
        private final double std;

        public Gaussian2D(final double xOpt, final double yOpt, final double std) {
            this.maximumPosition = new double[] { xOpt, yOpt };
            this.std = std;
        }

        public double getMaximum() {
            return this.value(this.maximumPosition);
        }

        public double[] getMaximumPosition() {
            return this.maximumPosition.clone();
        }

        @Override
        public double value(final double[] point) {
            final double x = point[0], y = point[1];
            final double twoS2 = 2.0 * this.std * this.std;
            return 1.0 / (twoS2 * FastMath.PI) * MathLib.exp(-(x * x + y * y) / twoS2);
        }
    }

    private int count;
}
