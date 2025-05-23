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
package fr.cnes.sirius.patrius.math.optim.univariate;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.FunctionUtils;
import fr.cnes.sirius.patrius.math.analysis.QuinticFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.function.Sin;
import fr.cnes.sirius.patrius.math.analysis.function.StepFunction;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooLargeException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.exception.TooManyEvaluationsException;
import fr.cnes.sirius.patrius.math.optim.ConvergenceChecker;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.stat.descriptive.DescriptiveStatistics;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * @version $Id: BrentOptimizerTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public final class BrentOptimizerTest {

    @Test
    public void testSinMin() {
        final UnivariateFunction f = new Sin();
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        Assert.assertEquals(3 * Math.PI / 2, optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(4, 5)).getPoint(), 1e-8);
        Assert.assertTrue(optimizer.getEvaluations() <= 50);
        Assert.assertEquals(200, optimizer.getMaxEvaluations());
        Assert.assertEquals(3 * Math.PI / 2, optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(1, 5)).getPoint(), 1e-8);
        Assert.assertTrue(optimizer.getEvaluations() <= 100);
        Assert.assertTrue(optimizer.getEvaluations() >= 15);
        try {
            optimizer.optimize(new MaxEval(10),
                new UnivariateObjectiveFunction(f),
                GoalType.MINIMIZE,
                new SearchInterval(4, 5));
            Assert.fail("an exception should have been thrown");
        } catch (final TooManyEvaluationsException fee) {
            // expected
        }
    }

    @Test
    public void testSinMinWithValueChecker() {
        final UnivariateFunction f = new Sin();
        final ConvergenceChecker<UnivariatePointValuePair> checker = new SimpleUnivariateValueChecker(1e-5, 1e-14);
        // The default stopping criterion of Brent's algorithm should not
        // pass, but the search will stop at the given relative tolerance
        // for the function value.
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14, checker);
        final UnivariatePointValuePair result = optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(4, 5));
        Assert.assertEquals(3 * Math.PI / 2, result.getPoint(), 1e-3);
    }

    @Test
    public void testBoundaries() {
        final double lower = -1.0;
        final double upper = +1.0;
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1573869102691180623L;

            @Override
            public double value(final double x) {
                if (x < lower) {
                    throw new NumberIsTooSmallException(x, lower, true);
                } else if (x > upper) {
                    throw new NumberIsTooLargeException(x, upper, true);
                } else {
                    return x;
                }
            }
        };
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        Assert.assertEquals(lower,
            optimizer.optimize(new MaxEval(100),
                new UnivariateObjectiveFunction(f),
                GoalType.MINIMIZE,
                new SearchInterval(lower, upper)).getPoint(),
            1.0e-8);
        Assert.assertEquals(upper,
            optimizer.optimize(new MaxEval(100),
                new UnivariateObjectiveFunction(f),
                GoalType.MAXIMIZE,
                new SearchInterval(lower, upper)).getPoint(),
            1.0e-8);
    }

    @Test
    public void testQuinticMin() {
        // The function has local minima at -0.27195613 and 0.82221643.
        final UnivariateFunction f = new QuinticFunction();
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        Assert.assertEquals(-0.27195613, optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(-0.3, -0.2)).getPoint(), 1.0e-8);
        Assert.assertEquals(0.82221643, optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(0.3, 0.9)).getPoint(), 1.0e-8);
        Assert.assertTrue(optimizer.getEvaluations() <= 50);

        // search in a large interval
        Assert.assertEquals(-0.27195613, optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(-1.0, 0.2)).getPoint(), 1.0e-8);
        Assert.assertTrue(optimizer.getEvaluations() <= 50);
    }

    @Test
    public void testQuinticMinStatistics() {
        // The function has local minima at -0.27195613 and 0.82221643.
        final UnivariateFunction f = new QuinticFunction();
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-11, 1e-14);

        final DescriptiveStatistics[] stat = new DescriptiveStatistics[2];
        for (int i = 0; i < stat.length; i++) {
            stat[i] = new DescriptiveStatistics();
        }

        final double min = -0.75;
        final double max = 0.25;
        final int nSamples = 200;
        final double delta = (max - min) / nSamples;
        for (int i = 0; i < nSamples; i++) {
            final double start = min + i * delta;
            stat[0].addValue(optimizer.optimize(new MaxEval(40),
                new UnivariateObjectiveFunction(f),
                GoalType.MINIMIZE,
                new SearchInterval(min, max, start)).getPoint());
            stat[1].addValue(optimizer.getEvaluations());
        }

        final double meanOptValue = stat[0].getMean();
        final double medianEval = stat[1].getPercentile(50);
        Assert.assertTrue(meanOptValue > -0.2719561281);
        Assert.assertTrue(meanOptValue < -0.2719561280);
        Assert.assertEquals(23, (int) medianEval);
    }

    @Test
    public void testQuinticMax() {
        // The quintic function has zeros at 0, +-0.5 and +-1.
        // The function has a local maximum at 0.27195613.
        final UnivariateFunction f = new QuinticFunction();
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-12, 1e-14);
        Assert.assertEquals(0.27195613, optimizer.optimize(new MaxEval(100),
            new UnivariateObjectiveFunction(f),
            GoalType.MAXIMIZE,
            new SearchInterval(0.2, 0.3)).getPoint(), 1e-8);
        try {
            optimizer.optimize(new MaxEval(5),
                new UnivariateObjectiveFunction(f),
                GoalType.MAXIMIZE,
                new SearchInterval(0.2, 0.3));
            Assert.fail("an exception should have been thrown");
        } catch (final TooManyEvaluationsException miee) {
            // expected
        }
    }

    @Test
    public void testMinEndpoints() {
        final UnivariateFunction f = new Sin();
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-8, 1e-14);

        // endpoint is minimum
        double result = optimizer.optimize(new MaxEval(50),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(3 * Math.PI / 2, 5)).getPoint();
        Assert.assertEquals(3 * Math.PI / 2, result, 1e-6);

        result = optimizer.optimize(new MaxEval(50),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(4, 3 * Math.PI / 2)).getPoint();
        Assert.assertEquals(3 * Math.PI / 2, result, 1e-6);
    }

    @Test
    public void testMath832() {
        final UnivariateFunction f = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4428455985959641906L;

            @Override
            public double value(final double x) {
                final double sqrtX = MathLib.sqrt(x);
                final double a = 1e2 * sqrtX;
                final double b = 1e6 / x;
                final double c = 1e4 / sqrtX;

                return a + b + c;
            }
        };

        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-8);
        final double result = optimizer.optimize(new MaxEval(1483),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(Double.MIN_VALUE,
                Double.MAX_VALUE)).getPoint();

        Assert.assertEquals(804.9355825, result, 1e-6);
    }

    /**
     * Contrived example showing that prior to the resolution of MATH-855
     * (second revision), the algorithm would not return the best point if
     * it happened to be the initial guess.
     */
    @Test
    public void testKeepInitIfBest() {
        final double minSin = 3 * Math.PI / 2;
        final double offset = 1e-8;
        final double delta = 1e-7;
        final UnivariateFunction f1 = new Sin();
        final UnivariateFunction f2 = new StepFunction(new double[] { minSin, minSin + offset, minSin + 2 * offset },
            new double[] { 0, -1, 0 });
        final UnivariateFunction f = FunctionUtils.add(f1, f2);
        // A slightly less stringent tolerance would make the test pass
        // even with the previous implementation.
        final double relTol = 1e-8;
        final UnivariateOptimizer optimizer = new BrentOptimizer(relTol, 1e-100);
        final double init = minSin + 1.5 * offset;
        final UnivariatePointValuePair result = optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(minSin - 6.789 * delta,
                minSin + 9.876 * delta,
                init));
        optimizer.getEvaluations();

        final double sol = result.getPoint();
        final double expected = init;

        // System.out.println("numEval=" + numEval);
        // System.out.println("min=" + init + " f=" + f.value(init));
        // System.out.println("sol=" + sol + " f=" + f.value(sol));
        // System.out.println("exp=" + expected + " f=" + f.value(expected));

        Assert.assertTrue("Best point not reported", f.value(sol) <= f.value(expected));
    }

    /**
     * Contrived example showing that prior to the resolution of MATH-855,
     * the algorithm, by always returning the last evaluated point, would
     * sometimes not report the best point it had found.
     */
    @Test
    public void testMath855() {
        final double minSin = 3 * Math.PI / 2;
        final double offset = 1e-8;
        final double delta = 1e-7;
        final UnivariateFunction f1 = new Sin();
        final UnivariateFunction f2 = new StepFunction(new double[] { minSin, minSin + offset, minSin + 5 * offset },
            new double[] { 0, -1, 0 });
        final UnivariateFunction f = FunctionUtils.add(f1, f2);
        final UnivariateOptimizer optimizer = new BrentOptimizer(1e-8, 1e-100);
        final UnivariatePointValuePair result = optimizer.optimize(new MaxEval(200),
            new UnivariateObjectiveFunction(f),
            GoalType.MINIMIZE,
            new SearchInterval(minSin - 6.789 * delta,
                minSin + 9.876 * delta));
        optimizer.getEvaluations();

        final double sol = result.getPoint();
        final double expected = 4.712389027602411;

        // System.out.println("min=" + (minSin + offset) + " f=" + f.value(minSin + offset));
        // System.out.println("sol=" + sol + " f=" + f.value(sol));
        // System.out.println("exp=" + expected + " f=" + f.value(expected));

        Assert.assertTrue("Best point not reported", f.value(sol) <= f.value(expected));
    }
}
