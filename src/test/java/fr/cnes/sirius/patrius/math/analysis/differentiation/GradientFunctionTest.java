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
package fr.cnes.sirius.patrius.math.analysis.differentiation;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Test for class {@link GradientFunction}.
 */
public class GradientFunctionTest {

    @Test
    public void test2DDistance() {
        final EuclideanDistance f = new EuclideanDistance();
        final GradientFunction g = new GradientFunction(f);
        for (double x = -10; x < 10; x += 0.5) {
            for (double y = -10; y < 10; y += 0.5) {
                final double[] point = new double[] { x, y };
                TestUtils.assertEquals(f.gradient(point), g.value(point), 1.0e-15);
            }
        }
    }

    @Test
    public void test3DDistance() {
        final EuclideanDistance f = new EuclideanDistance();
        final GradientFunction g = new GradientFunction(f);
        for (double x = -10; x < 10; x += 0.5) {
            for (double y = -10; y < 10; y += 0.5) {
                for (double z = -10; z < 10; z += 0.5) {
                    final double[] point = new double[] { x, y, z };
                    TestUtils.assertEquals(f.gradient(point), g.value(point), 1.0e-15);
                }
            }
        }
    }

    private static class EuclideanDistance implements MultivariateDifferentiableFunction {

        @Override
        public double value(final double[] point) {
            double d2 = 0;
            for (final double x : point) {
                d2 += x * x;
            }
            return MathLib.sqrt(d2);
        }

        @Override
        public DerivativeStructure value(final DerivativeStructure[] point)
                                                                           throws DimensionMismatchException,
                                                                           MathIllegalArgumentException {
            DerivativeStructure d2 = point[0].getField().getZero();
            for (final DerivativeStructure x : point) {
                d2 = d2.add(x.multiply(x));
            }
            return d2.sqrt();
        }

        public double[] gradient(final double[] point) {
            final double[] gradient = new double[point.length];
            final double d = this.value(point);
            for (int i = 0; i < point.length; ++i) {
                gradient[i] = point[i] / d;
            }
            return gradient;
        }

    }

}
