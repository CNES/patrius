/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
* HISTORY
* VERSION:4.8:DM:DM-2997:15/11/2021:[PATRIUS] Disposer de fonctionnalites d'evaluation de l'erreur d'approximation d'une fonction 
* VERSION:4.8:FA:FA-2998:15/11/2021:[PATRIUS] Discontinuite methode computeBearing de la classe ProjectionEllipsoidUtils
* VERSION:4.8:FA:FA-2982:15/11/2021:[PATRIUS] Orienter correctement les facettes de la methode toObjFile de GeodeticMeshLoader
* END-HISTORY
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
 */
package fr.cnes.sirius.patrius.math.analysis;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * A collection of static methods that evaluate {@link UnivariateFunction} functions errors.
 */
public final class ErrorEvaluationFunctionUtils {

    /**
     * Private constructor.
     */
    private ErrorEvaluationFunctionUtils() {
        super();
    }

    /**
     * Compute the standard deviation &sigma; between the function to evaluate and the approximated function at the
     * considered abscissas.
     * <p>
     * Note: the standard deviation isn't computed (return {@link Double#NaN}) if the function or the approximated
     * function have {@link Double#NaN} values.
     * </p>
     * 
     * @param function
     *        Function to evaluate
     * @param approximatedFunction
     *        Approximated function
     * @param abscissas
     *        Abscissas to considered
     * @return the standard deviation &sigma;
     */
    public static double getStandardDeviation(final UnivariateFunction function,
            final UnivariateFunction approximatedFunction,
            final double[] abscissas) {
        final double[] functionValues = new double[abscissas.length];
        for (int i = 0; i < functionValues.length; i++) {
            functionValues[i] = function.value(abscissas[i]);
        }
        return getStandardDeviation(abscissas, functionValues, approximatedFunction);
    }

    /**
     * Compute the standard deviation &sigma; between the function to evaluate and the approximated function at the
     * considered abscissas.
     * <p>
     * Note: the standard deviation isn't computed (return {@link Double#NaN}) if the function or the approximated
     * function have {@link Double#NaN} values.
     * </p>
     *
     * @param abscissas
     *        Abscissas to considered
     * @param functionValues
     *        Function values at the specified abscissas
     * @param approximatedFunction
     *        Approximated function
     * @return the standard deviation &sigma;
     * @throws DimensionMismatchException if {@code abscissas} and {@code functionValues} do not have the same length
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    // Reason: false positive
    public static double getStandardDeviation(final double[] abscissas,
            final double[] functionValues,
            final UnivariateFunction approximatedFunction) {
        // Check the input arrays dimension consistency
        final int size = abscissas.length;
        if (size != functionValues.length) {
            throw new DimensionMismatchException(size, functionValues.length);
        }

        // Fill the values tab and compute the mean value
        final double[] values = new double[size];
        double mean = 0.;
        for (int i = 0; i < size; i++) {
            values[i] = functionValues[i] - approximatedFunction.value(abscissas[i]);
            mean += values[i];
        }
        mean /= size;

        final double deviation;
        // Double.NaN safety
        if (Double.isNaN(mean)) {
            deviation = Double.NaN;
        } else {
            // Compute the variance
            double variance = 0.;
            for (int i = 0; i < size; i++) {
                final double temp = values[i] - mean;
                variance += temp * temp;
            }
            variance /= size;

            // Compute the deviation
            deviation = MathLib.sqrt(variance);
        }

        return deviation;
    }

    /**
     * Compute the L<sub>&infin;</sub> norm (worst value) between the function to evaluate and the approximated function
     * at the considered abscissas.
     * <p>
     * Note: the L<sub>&infin;</sub> norm isn't computed (return {@link Double#NaN}) if the function or the approximated
     * function have {@link Double#NaN} values.
     * </p>
     * 
     * @param function
     *        Function to evaluate
     * @param approximatedFunction
     *        Approximated function
     * @param abscissas
     *        Abscissas to considered
     * @return L<sub>&infin;</sub> norm
     */
    public static double getNormInf(final UnivariateFunction function,
            final UnivariateFunction approximatedFunction,
            final double[] abscissas) {
        final double[] functionValues = new double[abscissas.length];
        for (int i = 0; i < functionValues.length; i++) {
            functionValues[i] = function.value(abscissas[i]);
        }
        return getNormInf(abscissas, functionValues, approximatedFunction);
    }

    /**
     * Compute the L<sub>&infin;</sub> norm (worst value) between the function to evaluate and the approximated function
     * at the considered abscissas.
     * <p>
     * Note: the L<sub>&infin;</sub> norm isn't computed (return {@link Double#NaN}) if the function or the approximated
     * function have {@link Double#NaN} values.
     * </p>
     * 
     * @param abscissas
     *        Abscissas to considered
     * @param functionValues
     *        Function values at the specified abscissas
     * @param approximatedFunction
     *        Approximated function
     * @return L<sub>&infin;</sub> norm
     * @throws DimensionMismatchException if {@code abscissas} and {@code functionValues} do not have the same length
     */
    public static double getNormInf(final double[] abscissas,
            final double[] functionValues,
            final UnivariateFunction approximatedFunction) {
        // Check the input arrays dimension consistency
        final int size = abscissas.length;
        if (size != functionValues.length) {
            throw new DimensionMismatchException(size, functionValues.length);
        }

        // Fill the values tab and check for Double#NaN values
        final double[] values = new double[size];
        boolean hasNaN = false;
        for (int i = 0; i < size && !hasNaN; i++) {
            values[i] = functionValues[i] - approximatedFunction.value(abscissas[i]);
            if (Double.isNaN(values[i])) {
                // NaN
                hasNaN = true;
            }
        }

        final double lInf;
        // Double.NaN safety
        if (hasNaN) {
            lInf = Double.NaN;
        } else {
            // Compute the Linf norm
            double lInfTemp = 0.;
            for (int i = 0; i < abscissas.length; i++) {
                lInfTemp = MathLib.max(lInfTemp,
                    MathLib.abs(functionValues[i] - approximatedFunction.value(abscissas[i])));
            }
            lInf = lInfTemp;
        }
        // Return result
        return lInf;
    }
}
