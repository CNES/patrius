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
 * VERSION:4.8:DM:DM-2997:15/11/2021:[PATRIUS] Disposer de fonctionnalites d'evaluation de l'erreur d'approximation d'une fonction 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.analysis.function.StepFunction;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;

/**
 * Tests the {@link ErrorEvaluationFunctionUtils} class static features.
 */
public class ErrorEvaluationFunctionUtilsTest {

    /**
     * @description Evaluate the standard deviations computation process with several {@link UnivariateFunction}
     *              functions.
     *
     * @testedMethod {@link ErrorEvaluationFunctionUtils#getStandardDeviation(double[], double[], UnivariateFunction)}
     * @testedMethod {@link ErrorEvaluationFunctionUtils#getStandardDeviation(UnivariateFunction, UnivariateFunction, double[])}
     *
     * @testPassCriteria The standard deviations are computed as expected.
     */
    @Test
    public void testStandardDeviation() {

        // ---- Nominal case #1 (with function) ----
        final UnivariateFunction fct = new StepFunction(new double[] { 0., 1., 2. }, new double[] { 1., 2., 3. });
        final UnivariateFunction approxFct = new StepFunction(new double[] { 0., 1., 2. },
            new double[] { 1., 2.2, 2.6 });

        final double[] abscissas = new double[] { 0.5, 1., 2.1, 3. };
        // x = -1 ---> fct(x) = 1 | approxFct(x) = 1
        // x = 0.5 --> fct(x) = 2 | approxFct(x) = 2.2
        // x = 2.1 --> fct(x) = 3 | approxFct(x) = 2.6
        // x = 3 ----> fct(x) = 3 | approxFct(x) = 2.6

        final double std1 = ErrorEvaluationFunctionUtils.getStandardDeviation(fct, approxFct, abscissas);
        final double expectedStd1 = 0.259807621135; // Computed with an external tool
        Assert.assertEquals(expectedStd1, std1, 1e-12);

        // ---- Nominal case #2 (with function values) ----
        final double[] fctValues = new double[] { 0.8, 2., 3., 2.9 };

        // x = -1 ---> fctValues[0] = 0.8 | approxFct(x) = 1
        // x = 0.5 --> fctValues[1] = 2 | approxFct(x) = 2.2
        // x = 2.1 --> fctValues[2] = 3 | approxFct(x) = 2.6
        // x = 3 ----> fctValues[3] = 2.9 | approxFct(x) = 2.6

        final double std2 = ErrorEvaluationFunctionUtils.getStandardDeviation(abscissas, fctValues, approxFct);
        final double expectedStd2 = 0.277263412660; // Computed with an external tool
        Assert.assertEquals(expectedStd2, std2, 1e-12);

        // ---- Double.NaN management ----
        Assert.assertTrue(Double.isNaN(ErrorEvaluationFunctionUtils.getStandardDeviation(abscissas,
            new double[] { 0.8, 2., 3., Double.NaN }, approxFct)));

        // ---- Try to use incompatible arrays lengths ----
        try {
            ErrorEvaluationFunctionUtils.getStandardDeviation(new double[3], new double[4], approxFct);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Evaluate the L<sub>&infin;</sub> norms (worst values) computation process with several
     *              {@link UnivariateFunction} functions.
     *
     * @testedMethod {@link ErrorEvaluationFunctionUtils#getNormInf(double[], double[], UnivariateFunction)}
     * @testedMethod {@link ErrorEvaluationFunctionUtils#getNormInf(UnivariateFunction, UnivariateFunction, double[])}
     *
     * @testPassCriteria The L<sub>&infin;</sub> norms are computed as expected.
     */
    @Test
    public void testNormInf() {

        // ---- Nominal case #1 (with function) ----
        final UnivariateFunction fct = new StepFunction(new double[] { 0., 1., 2. }, new double[] { 1., 2., 3. });
        final UnivariateFunction approxFct = new StepFunction(new double[] { 0., 1., 2. },
            new double[] { 1., 2.2, 2.6 });

        final double[] abscissas = new double[] { 0.5, 1., 2.1, 3. };
        // x = -1 ---> fct(x) = 1 | approxFct(x) = 1
        // x = 0.5 --> fct(x) = 2 | approxFct(x) = 2.2
        // x = 2.1 --> fct(x) = 3 | approxFct(x) = 2.6
        // x = 3 ----> fct(x) = 3 | approxFct(x) = 2.6

        final double lInf1 = ErrorEvaluationFunctionUtils.getNormInf(fct, approxFct, abscissas);
        final double expectedLInf1 = 0.4;
        Assert.assertEquals(expectedLInf1, lInf1, 1e-12);

        // ---- Nominal case #2 (with function values) ----
        final double[] fctValues = new double[] { 0.2, 2., 3., 2.9 };

        // x = -1 ---> fctValues[0] = 0.2 | approxFct(x) = 1
        // x = 0.5 --> fctValues[1] = 2 | approxFct(x) = 2.2
        // x = 2.1 --> fctValues[2] = 3 | approxFct(x) = 2.6
        // x = 3 ----> fctValues[3] = 2.9 | approxFct(x) = 2.6

        final double lInf2 = ErrorEvaluationFunctionUtils.getNormInf(abscissas, fctValues, approxFct);
        final double expectedLInf2 = 0.8;
        Assert.assertEquals(expectedLInf2, lInf2, 1e-12);

        // ---- Double.NaN management ----
        Assert.assertTrue(Double.isNaN(ErrorEvaluationFunctionUtils.getNormInf(abscissas,
            new double[] { 0.8, 2., Double.NaN, 3. }, approxFct)));

        // ---- Try to use incompatible arrays lengths ----
        try {
            ErrorEvaluationFunctionUtils.getNormInf(new double[3], new double[4], approxFct);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }
    }
}
