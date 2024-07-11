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
import fr.cnes.sirius.patrius.math.analysis.SumSincFunction;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for {@link PowellOptimizer}.
 */
public class PowellOptimizerTest {

    @Test
    public void testSumSinc() {
        final MultivariateFunction func = new SumSincFunction(-1);

        final int dim = 2;
        final double[] minPoint = new double[dim];
        for (int i = 0; i < dim; i++) {
            minPoint[i] = 0;
        }

        final double[] init = new double[dim];

        // Initial is minimum.
        for (int i = 0; i < dim; i++) {
            init[i] = minPoint[i];
        }
        this.doTest(func, minPoint, init, GoalType.MINIMIZE, 1e-9, 1e-9);

        // Initial is far from minimum.
        for (int i = 0; i < dim; i++) {
            init[i] = minPoint[i] + 3;
        }
        this.doTest(func, minPoint, init, GoalType.MINIMIZE, 1e-9, 1e-5);
        // More stringent line search tolerance enhances the precision
        // of the result.
        this.doTest(func, minPoint, init, GoalType.MINIMIZE, 1e-9, 1e-9, 1e-7);
    }

    @Test
    public void testQuadratic() {
        final MultivariateFunction func = new MultivariateFunction(){
            @Override
            public double value(final double[] x) {
                final double a = x[0] - 1;
                final double b = x[1] - 1;
                return a * a + b * b + 1;
            }
        };

        final int dim = 2;
        final double[] minPoint = new double[dim];
        for (int i = 0; i < dim; i++) {
            minPoint[i] = 1;
        }

        final double[] init = new double[dim];

        // Initial is minimum.
        for (int i = 0; i < dim; i++) {
            init[i] = minPoint[i];
        }
        this.doTest(func, minPoint, init, GoalType.MINIMIZE, 1e-9, 1e-8);

        // Initial is far from minimum.
        for (int i = 0; i < dim; i++) {
            init[i] = minPoint[i] - 20;
        }
        this.doTest(func, minPoint, init, GoalType.MINIMIZE, 1e-9, 1e-8);
    }

    @Test
    public void testMaximizeQuadratic() {
        final MultivariateFunction func = new MultivariateFunction(){
            @Override
            public double value(final double[] x) {
                final double a = x[0] - 1;
                final double b = x[1] - 1;
                return -a * a - b * b + 1;
            }
        };

        final int dim = 2;
        final double[] maxPoint = new double[dim];
        for (int i = 0; i < dim; i++) {
            maxPoint[i] = 1;
        }

        final double[] init = new double[dim];

        // Initial is minimum.
        for (int i = 0; i < dim; i++) {
            init[i] = maxPoint[i];
        }
        this.doTest(func, maxPoint, init, GoalType.MAXIMIZE, 1e-9, 1e-8);

        // Initial is far from minimum.
        for (int i = 0; i < dim; i++) {
            init[i] = maxPoint[i] - 20;
        }
        this.doTest(func, maxPoint, init, GoalType.MAXIMIZE, 1e-9, 1e-8);
    }

    /**
     * Ensure that we do not increase the number of function evaluations when
     * the function values are scaled up.
     * Note that the tolerances parameters passed to the constructor must
     * still hold sensible values because they are used to set the line search
     * tolerances.
     */
    @Test
    public void testRelativeToleranceOnScaledValues() {
        final MultivariateFunction func = new MultivariateFunction(){
            @Override
            public double value(final double[] x) {
                final double a = x[0] - 1;
                final double b = x[1] - 1;
                return a * a * MathLib.sqrt(MathLib.abs(a)) + b * b + 1;
            }
        };

        final int dim = 2;
        final double[] minPoint = new double[dim];
        for (int i = 0; i < dim; i++) {
            minPoint[i] = 1;
        }

        final double[] init = new double[dim];
        // Initial is far from minimum.
        for (int i = 0; i < dim; i++) {
            init[i] = minPoint[i] - 20;
        }

        final double relTol = 1e-10;

        final int maxEval = 1000;
        // Very small absolute tolerance to rely solely on the relative
        // tolerance as a stopping criterion
        final PowellOptimizer optim = new PowellOptimizer(relTol, 1e-100);

        final PointValuePair funcResult = optim.optimize(new MaxEval(maxEval),
            new ObjectiveFunction(func),
            GoalType.MINIMIZE,
            new InitialGuess(init));
        final double funcValue = func.value(funcResult.getPoint());
        final int funcEvaluations = optim.getEvaluations();

        final double scale = 1e10;
        final MultivariateFunction funcScaled = new MultivariateFunction(){
            @Override
            public double value(final double[] x) {
                return scale * func.value(x);
            }
        };

        final PointValuePair funcScaledResult = optim.optimize(new MaxEval(maxEval),
            new ObjectiveFunction(funcScaled),
            GoalType.MINIMIZE,
            new InitialGuess(init));
        final double funcScaledValue = funcScaled.value(funcScaledResult.getPoint());
        final int funcScaledEvaluations = optim.getEvaluations();

        // Check that both minima provide the same objective funciton values,
        // within the relative function tolerance.
        Assert.assertEquals(1, funcScaledValue / (scale * funcValue), relTol);

        // Check that the numbers of evaluations are the same.
        Assert.assertEquals(funcEvaluations, funcScaledEvaluations);
    }

    /**
     * @param func
     *        Function to optimize.
     * @param optimum
     *        Expected optimum.
     * @param init
     *        Starting point.
     * @param goal
     *        Minimization or maximization.
     * @param fTol
     *        Tolerance (relative error on the objective function) for
     *        "Powell" algorithm.
     * @param pointTol
     *        Tolerance for checking that the optimum is correct.
     */
    private void doTest(final MultivariateFunction func,
                        final double[] optimum,
                        final double[] init,
                        final GoalType goal,
                        final double fTol,
                        final double pointTol) {
        final PowellOptimizer optim = new PowellOptimizer(fTol, Math.ulp(1d));

        final PointValuePair result = optim.optimize(new MaxEval(1000),
            new ObjectiveFunction(func),
            goal,
            new InitialGuess(init));
        final double[] point = result.getPoint();

        for (int i = 0, dim = optimum.length; i < dim; i++) {
            Assert.assertEquals("found[" + i + "]=" + point[i] + " value=" + result.getValue(),
                optimum[i], point[i], pointTol);
        }
    }

    /**
     * @param func
     *        Function to optimize.
     * @param optimum
     *        Expected optimum.
     * @param init
     *        Starting point.
     * @param goal
     *        Minimization or maximization.
     * @param fTol
     *        Tolerance (relative error on the objective function) for
     *        "Powell" algorithm.
     * @param fLineTol
     *        Tolerance (relative error on the objective function)
     *        for the internal line search algorithm.
     * @param pointTol
     *        Tolerance for checking that the optimum is correct.
     */
    private void doTest(final MultivariateFunction func,
                        final double[] optimum,
                        final double[] init,
                        final GoalType goal,
                        final double fTol,
                        final double fLineTol,
                        final double pointTol) {
        final PowellOptimizer optim = new PowellOptimizer(fTol, Math.ulp(1d),
            fLineTol, Math.ulp(1d));

        final PointValuePair result = optim.optimize(new MaxEval(1000),
            new ObjectiveFunction(func),
            goal,
            new InitialGuess(init));
        final double[] point = result.getPoint();

        for (int i = 0, dim = optimum.length; i < dim; i++) {
            Assert.assertEquals("found[" + i + "]=" + point[i] + " value=" + result.getValue(),
                optimum[i], point[i], pointTol);
        }
    }
}
