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
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.MultivariateVectorFunction;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.LeastSquaresConverter;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class SimplexOptimizerNelderMeadTest {
    @Test
    public void testMinimize1() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            new ObjectiveFunction(fourExtrema),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { -3, 0 }),
            new NelderMeadSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xM, optimum.getPoint()[0], 2e-7);
        Assert.assertEquals(fourExtrema.yP, optimum.getPoint()[1], 2e-5);
        Assert.assertEquals(fourExtrema.valueXmYp, optimum.getValue(), 6e-12);
        Assert.assertTrue(optimizer.getEvaluations() > 60);
        Assert.assertTrue(optimizer.getEvaluations() < 90);
    }

    @Test
    public void testMinimize2() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            new ObjectiveFunction(fourExtrema),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 1, 0 }),
            new NelderMeadSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xP, optimum.getPoint()[0], 5e-6);
        Assert.assertEquals(fourExtrema.yM, optimum.getPoint()[1], 6e-6);
        Assert.assertEquals(fourExtrema.valueXpYm, optimum.getValue(), 1e-11);
        Assert.assertTrue(optimizer.getEvaluations() > 60);
        Assert.assertTrue(optimizer.getEvaluations() < 90);
    }

    @Test
    public void testMaximize1() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            new ObjectiveFunction(fourExtrema),
            GoalType.MAXIMIZE,
            new InitialGuess(new double[] { -3, 0 }),
            new NelderMeadSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xM, optimum.getPoint()[0], 1e-5);
        Assert.assertEquals(fourExtrema.yM, optimum.getPoint()[1], 3e-6);
        Assert.assertEquals(fourExtrema.valueXmYm, optimum.getValue(), 3e-12);
        Assert.assertTrue(optimizer.getEvaluations() > 60);
        Assert.assertTrue(optimizer.getEvaluations() < 90);
    }

    @Test
    public void testMaximize2() {
        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        final FourExtrema fourExtrema = new FourExtrema();

        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            new ObjectiveFunction(fourExtrema),
            GoalType.MAXIMIZE,
            new InitialGuess(new double[] { 1, 0 }),
            new NelderMeadSimplex(new double[] { 0.2, 0.2 }));
        Assert.assertEquals(fourExtrema.xP, optimum.getPoint()[0], 4e-6);
        Assert.assertEquals(fourExtrema.yP, optimum.getPoint()[1], 5e-6);
        Assert.assertEquals(fourExtrema.valueXpYp, optimum.getValue(), 7e-12);
        Assert.assertTrue(optimizer.getEvaluations() > 60);
        Assert.assertTrue(optimizer.getEvaluations() < 90);
    }

    @Test
    public void testRosenbrock() {

        final Rosenbrock rosenbrock = new Rosenbrock();
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-3);
        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            new ObjectiveFunction(rosenbrock),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { -1.2, 1 }),
            new NelderMeadSimplex(new double[][] {
                { -1.2, 1 },
                { 0.9, 1.2 },
                { 3.5, -2.3 } }));

        Assert.assertEquals(rosenbrock.getCount(), optimizer.getEvaluations());
        Assert.assertTrue(optimizer.getEvaluations() > 40);
        Assert.assertTrue(optimizer.getEvaluations() < 50);
        Assert.assertTrue(optimum.getValue() < 8e-4);
    }

    @Test
    public void testPowell() {
        final Powell powell = new Powell();
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-3);
        final PointValuePair optimum =
            optimizer.optimize(new MaxEval(200),
                new ObjectiveFunction(powell),
                GoalType.MINIMIZE,
                new InitialGuess(new double[] { 3, -1, 0, 1 }),
                new NelderMeadSimplex(4));
        Assert.assertEquals(powell.getCount(), optimizer.getEvaluations());
        Assert.assertTrue(optimizer.getEvaluations() > 110);
        Assert.assertTrue(optimizer.getEvaluations() < 130);
        Assert.assertTrue(optimum.getValue() < 2e-3);
    }

    @Test
    public void testLeastSquares1() {
        final RealMatrix factors = new Array2DRowRealMatrix(new double[][] {
            { 1, 0 },
            { 0, 1 }
        }, false);
        final LeastSquaresConverter ls = new LeastSquaresConverter(new MultivariateVectorFunction(){
            @Override
            public double[] value(final double[] variables) {
                return factors.operate(variables);
            }
        }, new double[] { 2.0, -3.0 });
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-6);
        final PointValuePair optimum =
            optimizer.optimize(new MaxEval(200),
                new ObjectiveFunction(ls),
                GoalType.MINIMIZE,
                new InitialGuess(new double[] { 10, 10 }),
                new NelderMeadSimplex(2));
        Assert.assertEquals(2, optimum.getPointRef()[0], 3e-5);
        Assert.assertEquals(-3, optimum.getPointRef()[1], 4e-4);
        Assert.assertTrue(optimizer.getEvaluations() > 60);
        Assert.assertTrue(optimizer.getEvaluations() < 80);
        Assert.assertTrue(optimum.getValue() < 1.0e-6);
    }

    @Test
    public void testLeastSquares2() {
        final RealMatrix factors = new Array2DRowRealMatrix(new double[][] {
            { 1, 0 },
            { 0, 1 }
        }, false);
        final LeastSquaresConverter ls = new LeastSquaresConverter(new MultivariateVectorFunction(){
            @Override
            public double[] value(final double[] variables) {
                return factors.operate(variables);
            }
        }, new double[] { 2, -3 }, new double[] { 10, 0.1 });
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-6);
        final PointValuePair optimum =
            optimizer.optimize(new MaxEval(200),
                new ObjectiveFunction(ls),
                GoalType.MINIMIZE,
                new InitialGuess(new double[] { 10, 10 }),
                new NelderMeadSimplex(2));
        Assert.assertEquals(2, optimum.getPointRef()[0], 5e-5);
        Assert.assertEquals(-3, optimum.getPointRef()[1], 8e-4);
        Assert.assertTrue(optimizer.getEvaluations() > 60);
        Assert.assertTrue(optimizer.getEvaluations() < 80);
        Assert.assertTrue(optimum.getValue() < 1e-6);
    }

    @Test
    public void testLeastSquares3() {
        final RealMatrix factors =
            new Array2DRowRealMatrix(new double[][] {
                { 1, 0 },
                { 0, 1 }
            }, false);
        final LeastSquaresConverter ls = new LeastSquaresConverter(new MultivariateVectorFunction(){
            @Override
            public double[] value(final double[] variables) {
                return factors.operate(variables);
            }
        }, new double[] { 2, -3 }, new Array2DRowRealMatrix(new double[][] {
            { 1, 1.2 }, { 1.2, 2 }
        }));
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-6);
        final PointValuePair optimum = optimizer.optimize(new MaxEval(200),
            new ObjectiveFunction(ls),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 10, 10 }),
            new NelderMeadSimplex(2));
        Assert.assertEquals(2, optimum.getPointRef()[0], 2e-3);
        Assert.assertEquals(-3, optimum.getPointRef()[1], 8e-4);
        Assert.assertTrue(optimizer.getEvaluations() > 60);
        Assert.assertTrue(optimizer.getEvaluations() < 80);
        Assert.assertTrue(optimum.getValue() < 1e-6);
    }

    @Test(expected = TooManyEvaluationsException.class)
    public void testMaxIterations() {
        final Powell powell = new Powell();
        final SimplexOptimizer optimizer = new SimplexOptimizer(-1, 1e-3);
        optimizer.optimize(new MaxEval(20),
            new ObjectiveFunction(powell),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 3, -1, 0, 1 }),
            new NelderMeadSimplex(4));
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

    private static class Rosenbrock implements MultivariateFunction {
        private int count;

        public Rosenbrock() {
            this.count = 0;
        }

        @Override
        public double value(final double[] x) {
            ++this.count;
            final double a = x[1] - x[0] * x[0];
            final double b = 1.0 - x[0];
            return 100 * a * a + b * b;
        }

        public int getCount() {
            return this.count;
        }
    }

    private static class Powell implements MultivariateFunction {
        private int count;

        public Powell() {
            this.count = 0;
        }

        @Override
        public double value(final double[] x) {
            ++this.count;
            final double a = x[0] + 10 * x[1];
            final double b = x[2] - x[3];
            final double c = x[1] - 2 * x[2];
            final double d = x[0] - x[3];
            return a * a + 5 * b * b + c * c * c * c + 10 * d * d * d * d;
        }

        public int getCount() {
            return this.count;
        }
    }
}
