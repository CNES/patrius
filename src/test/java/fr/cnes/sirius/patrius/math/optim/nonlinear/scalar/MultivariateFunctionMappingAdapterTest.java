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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv.AbstractSimplex;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

public class MultivariateFunctionMappingAdapterTest {
    @Test
    public void testStartSimplexInsideRange() {
        final BiQuadratic biQuadratic = new BiQuadratic(2.0, 2.5, 1.0, 3.0, 2.0, 3.0);
        final MultivariateFunctionMappingAdapter wrapped = new MultivariateFunctionMappingAdapter(biQuadratic,
            biQuadratic.getLower(),
            biQuadratic.getUpper());

        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        final AbstractSimplex simplex = new NelderMeadSimplex(new double[][] {
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.75 }),
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.95 }),
            wrapped.boundedToUnbounded(new double[] { 1.7, 2.90 })
        });

        final PointValuePair optimum = optimizer.optimize(new MaxEval(300),
            new ObjectiveFunction(wrapped),
            simplex,
            GoalType.MINIMIZE,
            new InitialGuess(wrapped.boundedToUnbounded(new double[] { 1.5, 2.25 })));
        final double[] bounded = wrapped.unboundedToBounded(optimum.getPoint());

        Assert.assertEquals(biQuadratic.getBoundedXOptimum(), bounded[0], 2e-7);
        Assert.assertEquals(biQuadratic.getBoundedYOptimum(), bounded[1], 2e-7);
    }

    @Test
    public void testOptimumOutsideRange() {
        final BiQuadratic biQuadratic = new BiQuadratic(4.0, 0.0, 1.0, 3.0, 2.0, 3.0);
        final MultivariateFunctionMappingAdapter wrapped = new MultivariateFunctionMappingAdapter(biQuadratic,
            biQuadratic.getLower(),
            biQuadratic.getUpper());

        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        final AbstractSimplex simplex = new NelderMeadSimplex(new double[][] {
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.75 }),
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.95 }),
            wrapped.boundedToUnbounded(new double[] { 1.7, 2.90 })
        });

        final PointValuePair optimum = optimizer.optimize(new MaxEval(100),
            new ObjectiveFunction(wrapped),
            simplex,
            GoalType.MINIMIZE,
            new InitialGuess(wrapped.boundedToUnbounded(new double[] { 1.5, 2.25 })));
        final double[] bounded = wrapped.unboundedToBounded(optimum.getPoint());

        Assert.assertEquals(biQuadratic.getBoundedXOptimum(), bounded[0], 2e-7);
        Assert.assertEquals(biQuadratic.getBoundedYOptimum(), bounded[1], 2e-7);
    }

    @Test
    public void testUnbounded() {
        final BiQuadratic biQuadratic = new BiQuadratic(4.0, 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        final MultivariateFunctionMappingAdapter wrapped = new MultivariateFunctionMappingAdapter(biQuadratic,
            biQuadratic.getLower(),
            biQuadratic.getUpper());

        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        final AbstractSimplex simplex = new NelderMeadSimplex(new double[][] {
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.75 }),
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.95 }),
            wrapped.boundedToUnbounded(new double[] { 1.7, 2.90 })
        });

        final PointValuePair optimum = optimizer.optimize(new MaxEval(300),
            new ObjectiveFunction(wrapped),
            simplex,
            GoalType.MINIMIZE,
            new InitialGuess(wrapped.boundedToUnbounded(new double[] { 1.5, 2.25 })));
        final double[] bounded = wrapped.unboundedToBounded(optimum.getPoint());

        Assert.assertEquals(biQuadratic.getBoundedXOptimum(), bounded[0], 2e-7);
        Assert.assertEquals(biQuadratic.getBoundedYOptimum(), bounded[1], 2e-7);
    }

    @Test
    public void testHalfBounded() {
        final BiQuadratic biQuadratic = new BiQuadratic(4.0, 4.0,
            1.0, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, 3.0);
        final MultivariateFunctionMappingAdapter wrapped = new MultivariateFunctionMappingAdapter(biQuadratic,
            biQuadratic.getLower(),
            biQuadratic.getUpper());

        final SimplexOptimizer optimizer = new SimplexOptimizer(1e-13, 1e-30);
        final AbstractSimplex simplex = new NelderMeadSimplex(new double[][] {
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.75 }),
            wrapped.boundedToUnbounded(new double[] { 1.5, 2.95 }),
            wrapped.boundedToUnbounded(new double[] { 1.7, 2.90 })
        });

        final PointValuePair optimum = optimizer.optimize(new MaxEval(200),
            new ObjectiveFunction(wrapped),
            simplex,
            GoalType.MINIMIZE,
            new InitialGuess(wrapped.boundedToUnbounded(new double[] { 1.5, 2.25 })));
        final double[] bounded = wrapped.unboundedToBounded(optimum.getPoint());

        Assert.assertEquals(biQuadratic.getBoundedXOptimum(), bounded[0], 1e-7);
        Assert.assertEquals(biQuadratic.getBoundedYOptimum(), bounded[1], 1e-7);
    }

    private static class BiQuadratic implements MultivariateFunction {

        private final double xOptimum;
        private final double yOptimum;

        private final double xMin;
        private final double xMax;
        private final double yMin;
        private final double yMax;

        public BiQuadratic(final double xOptimum, final double yOptimum,
            final double xMin, final double xMax,
            final double yMin, final double yMax) {
            this.xOptimum = xOptimum;
            this.yOptimum = yOptimum;
            this.xMin = xMin;
            this.xMax = xMax;
            this.yMin = yMin;
            this.yMax = yMax;
        }

        @Override
        public double value(final double[] point) {
            // the function should never be called with out of range points
            Assert.assertTrue(point[0] >= this.xMin);
            Assert.assertTrue(point[0] <= this.xMax);
            Assert.assertTrue(point[1] >= this.yMin);
            Assert.assertTrue(point[1] <= this.yMax);

            final double dx = point[0] - this.xOptimum;
            final double dy = point[1] - this.yOptimum;
            return dx * dx + dy * dy;

        }

        public double[] getLower() {
            return new double[] { this.xMin, this.yMin };
        }

        public double[] getUpper() {
            return new double[] { this.xMax, this.yMax };
        }

        public double getBoundedXOptimum() {
            return (this.xOptimum < this.xMin) ? this.xMin : ((this.xOptimum > this.xMax) ? this.xMax : this.xOptimum);
        }

        public double getBoundedYOptimum() {
            return (this.yOptimum < this.yMin) ? this.yMin : ((this.yOptimum > this.yMax) ? this.yMax : this.yOptimum);
        }
    }
}
