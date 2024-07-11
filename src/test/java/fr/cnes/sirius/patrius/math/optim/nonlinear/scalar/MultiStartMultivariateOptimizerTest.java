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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleValueChecker;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.gradient.CircleScalar;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import fr.cnes.sirius.patrius.math.random.GaussianRandomGenerator;
import fr.cnes.sirius.patrius.math.random.JDKRandomGenerator;
import fr.cnes.sirius.patrius.math.random.RandomVectorGenerator;
import fr.cnes.sirius.patrius.math.random.UncorrelatedRandomVectorGenerator;

public class MultiStartMultivariateOptimizerTest {
    @Test
    public void testCircleFitting() {
        final CircleScalar circle = new CircleScalar();
        circle.addPoint(30.0, 68.0);
        circle.addPoint(50.0, -6.0);
        circle.addPoint(110.0, -20.0);
        circle.addPoint(35.0, 15.0);
        circle.addPoint(45.0, 97.0);
        final GradientMultivariateOptimizer underlying = new NonLinearConjugateGradientOptimizer(
            NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
            new SimpleValueChecker(1e-10, 1e-10));
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(753289573253l);
        final RandomVectorGenerator generator = new UncorrelatedRandomVectorGenerator(new double[] { 50, 50 },
            new double[] { 10, 10 },
            new GaussianRandomGenerator(g));
        final MultiStartMultivariateOptimizer optimizer =
            new MultiStartMultivariateOptimizer(underlying, 10, generator);
        final PointValuePair optimum = optimizer.optimize(new MaxEval(200),
            circle.getObjectiveFunction(),
            circle.getObjectiveFunctionGradient(),
            GoalType.MINIMIZE,
            new InitialGuess(new double[] { 98.680, 47.345 }));
        Assert.assertEquals(200, optimizer.getMaxEvaluations());
        final PointValuePair[] optima = optimizer.getOptima();
        for (final PointValuePair o : optima) {
            final Vector2D center = new Vector2D(o.getPointRef()[0], o.getPointRef()[1]);
            Assert.assertEquals(69.960161753, circle.getRadius(center), 1e-8);
            Assert.assertEquals(96.075902096, center.getX(), 1e-8);
            Assert.assertEquals(48.135167894, center.getY(), 1e-8);
        }
        Assert.assertTrue(optimizer.getEvaluations() > 70);
        Assert.assertTrue(optimizer.getEvaluations() < 90);
        Assert.assertEquals(3.1267527, optimum.getValue(), 1e-8);
    }

    @Test
    public void testRosenbrock() {
        final Rosenbrock rosenbrock = new Rosenbrock();
        final SimplexOptimizer underlying = new SimplexOptimizer(new SimpleValueChecker(-1, 1e-3));
        final NelderMeadSimplex simplex = new NelderMeadSimplex(new double[][] {
            { -1.2, 1.0 },
            { 0.9, 1.2 },
            { 3.5, -2.3 }
        });
        final JDKRandomGenerator g = new JDKRandomGenerator();
        g.setSeed(16069223052l);
        final RandomVectorGenerator generator =
            new UncorrelatedRandomVectorGenerator(2, new GaussianRandomGenerator(g));
        final MultiStartMultivariateOptimizer optimizer =
            new MultiStartMultivariateOptimizer(underlying, 10, generator);
        final PointValuePair optimum = optimizer.optimize(new MaxEval(1100),
            new ObjectiveFunction(rosenbrock),
            GoalType.MINIMIZE,
            simplex,
            new InitialGuess(new double[] { -1.2, 1.0 }));

        Assert.assertEquals(rosenbrock.getCount(), optimizer.getEvaluations());
        Assert.assertTrue(optimizer.getEvaluations() > 900);
        Assert.assertTrue(optimizer.getEvaluations() < 1200);
        Assert.assertTrue(optimum.getValue() < 8e-4);
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
            final double b = 1 - x[0];
            return 100 * a * a + b * b;
        }

        public int getCount() {
            return this.count;
        }
    }
}
