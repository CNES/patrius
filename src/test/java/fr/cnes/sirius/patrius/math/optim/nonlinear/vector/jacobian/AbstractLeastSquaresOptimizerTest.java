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
package fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointVectorValuePair;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Target;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.Weight;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class AbstractLeastSquaresOptimizerTest {

    public static AbstractLeastSquaresOptimizer createOptimizer() {
        return new AbstractLeastSquaresOptimizer(null){

            @Override
            protected PointVectorValuePair doOptimize() {
                final double[] params = this.getStartPoint();
                final double[] res = this.computeResiduals(this.computeObjectiveValue(params));
                this.setCost(this.computeCost(res));
                return new PointVectorValuePair(params, null);
            }
        };
    }

    @Test
    public void testGetChiSquare() throws IOException {
        final StatisticalReferenceDataset dataset = StatisticalReferenceDatasetFactory.createKirby2();
        final AbstractLeastSquaresOptimizer optimizer = createOptimizer();
        final double[] a = dataset.getParameters();
        final double[] y = dataset.getData()[1];
        final double[] w = new double[y.length];
        Arrays.fill(w, 1.0);

        final StatisticalReferenceDataset.LeastSquaresProblem problem = dataset.getLeastSquaresProblem();

        optimizer.optimize(new MaxEval(1),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            new Target(y),
            new Weight(w),
            new InitialGuess(a));
        final double expected = dataset.getResidualSumOfSquares();
        final double actual = optimizer.getChiSquare();
        Assert.assertEquals(dataset.getName(), expected, actual,
            1E-11 * expected);
    }

    @Test
    public void testGetRMS() throws IOException {
        final StatisticalReferenceDataset dataset = StatisticalReferenceDatasetFactory.createKirby2();
        final AbstractLeastSquaresOptimizer optimizer = createOptimizer();
        final double[] a = dataset.getParameters();
        final double[] y = dataset.getData()[1];
        final double[] w = new double[y.length];
        Arrays.fill(w, 1);

        final StatisticalReferenceDataset.LeastSquaresProblem problem = dataset.getLeastSquaresProblem();

        optimizer.optimize(new MaxEval(1),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            new Target(y),
            new Weight(w),
            new InitialGuess(a));

        final double expected = MathLib
            .sqrt(dataset.getResidualSumOfSquares() /
                dataset.getNumObservations());
        final double actual = optimizer.getRMS();
        Assert.assertEquals(dataset.getName(), expected, actual,
            1E-11 * expected);
    }

    @Test
    public void testComputeSigma() throws IOException {
        final StatisticalReferenceDataset dataset = StatisticalReferenceDatasetFactory.createKirby2();
        final AbstractLeastSquaresOptimizer optimizer = createOptimizer();
        final double[] a = dataset.getParameters();
        final double[] y = dataset.getData()[1];
        final double[] w = new double[y.length];
        Arrays.fill(w, 1);

        final StatisticalReferenceDataset.LeastSquaresProblem problem = dataset.getLeastSquaresProblem();

        final PointVectorValuePair optimum = optimizer.optimize(new MaxEval(1),
            problem.getModelFunction(),
            problem.getModelFunctionJacobian(),
            new Target(y),
            new Weight(w),
            new InitialGuess(a));

        final double[] sig = optimizer.computeSigma(optimum.getPoint(), 1e-14);

        final int dof = y.length - a.length;
        final double[] expected = dataset.getParametersStandardDeviations();
        for (int i = 0; i < sig.length; i++) {
            final double actual = MathLib.sqrt(optimizer.getChiSquare() / dof) * sig[i];
            Assert.assertEquals(dataset.getName() + ", parameter #" + i,
                expected[i], actual, 1e-6 * expected[i]);
        }
    }
}
