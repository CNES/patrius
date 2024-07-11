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

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.ParametricUnivariateFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class CurveFitterTest {
    @Test
    public void testMath303() {
        final LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        final CurveFitter<ParametricUnivariateFunction> fitter =
            new CurveFitter<ParametricUnivariateFunction>(optimizer);
        fitter.addObservedPoint(2.805d, 0.6934785852953367d);
        fitter.addObservedPoint(2.74333333333333d, 0.6306772025518496d);
        fitter.addObservedPoint(1.655d, 0.9474675497289684);
        fitter.addObservedPoint(1.725d, 0.9013594835804194d);

        final ParametricUnivariateFunction sif = new SimpleInverseFunction();

        final double[] initialguess1 = new double[1];
        initialguess1[0] = 1.0d;
        Assert.assertEquals(1, fitter.fit(sif, initialguess1).length);

        final double[] initialguess2 = new double[2];
        initialguess2[0] = 1.0d;
        initialguess2[1] = .5d;
        Assert.assertEquals(2, fitter.fit(sif, initialguess2).length);
    }

    @Test
    public void testMath304() {
        final LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        final CurveFitter<ParametricUnivariateFunction> fitter =
            new CurveFitter<ParametricUnivariateFunction>(optimizer);
        fitter.addObservedPoint(2.805d, 0.6934785852953367d);
        fitter.addObservedPoint(2.74333333333333d, 0.6306772025518496d);
        fitter.addObservedPoint(1.655d, 0.9474675497289684);
        fitter.addObservedPoint(1.725d, 0.9013594835804194d);

        final ParametricUnivariateFunction sif = new SimpleInverseFunction();

        final double[] initialguess1 = new double[1];
        initialguess1[0] = 1.0d;
        Assert.assertEquals(1.6357215104109237, fitter.fit(sif, initialguess1)[0], 1.0e-14);

        final double[] initialguess2 = new double[1];
        initialguess2[0] = 10.0d;
        Assert.assertEquals(1.6357215104109237, fitter.fit(sif, initialguess1)[0], 1.0e-14);
    }

    @Test
    public void testMath372() {
        final LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        final CurveFitter<ParametricUnivariateFunction> curveFitter =
            new CurveFitter<ParametricUnivariateFunction>(optimizer);

        curveFitter.addObservedPoint(15, 4443);
        curveFitter.addObservedPoint(31, 8493);
        curveFitter.addObservedPoint(62, 17586);
        curveFitter.addObservedPoint(125, 30582);
        curveFitter.addObservedPoint(250, 45087);
        curveFitter.addObservedPoint(500, 50683);

        final ParametricUnivariateFunction f = new ParametricUnivariateFunction(){
            @Override
            public double value(final double x, final double... parameters) {
                final double a = parameters[0];
                final double b = parameters[1];
                final double c = parameters[2];
                final double d = parameters[3];

                return d + ((a - d) / (1 + MathLib.pow(x / c, b)));
            }

            @Override
            public double[] gradient(final double x, final double... parameters) {
                final double a = parameters[0];
                final double b = parameters[1];
                final double c = parameters[2];
                final double d = parameters[3];

                final double[] gradients = new double[4];
                final double den = 1 + MathLib.pow(x / c, b);

                // derivative with respect to a
                gradients[0] = 1 / den;

                // derivative with respect to b
                // in the reported (invalid) issue, there was a sign error here
                gradients[1] = -((a - d) * MathLib.pow(x / c, b) * MathLib.log(x / c)) / (den * den);

                // derivative with respect to c
                gradients[2] = (b * MathLib.pow(x / c, b - 1) * (x / (c * c)) * (a - d)) / (den * den);

                // derivative with respect to d
                gradients[3] = 1 - (1 / den);

                return gradients;

            }
        };

        final double[] initialGuess = new double[] { 1500, 0.95, 65, 35000 };
        final double[] estimatedParameters = curveFitter.fit(f, initialGuess);

        Assert.assertEquals(2411.00, estimatedParameters[0], 500.00);
        Assert.assertEquals(1.62, estimatedParameters[1], 0.04);
        Assert.assertEquals(111.22, estimatedParameters[2], 0.30);
        Assert.assertEquals(55347.47, estimatedParameters[3], 300.00);
        Assert.assertTrue(optimizer.getRMS() < 600.0);
    }

    private static class SimpleInverseFunction implements ParametricUnivariateFunction {

        @Override
        public double value(final double x, final double... parameters) {
            return parameters[0] / x + (parameters.length < 2 ? 0 : parameters[1]);
        }

        @Override
        public double[] gradient(final double x, final double... doubles) {
            final double[] gradientVector = new double[doubles.length];
            gradientVector[0] = 1 / x;
            if (doubles.length >= 2) {
                gradientVector[1] = 1;
            }
            return gradientVector;
        }
    }
}
