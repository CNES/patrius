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
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.filter;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.filter.FIRFilter.DataType;
import fr.cnes.sirius.patrius.math.filter.FIRFilter.FilterType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Non-regression tests for {@link FIRFilter}.
 *
 * @author Florian Teilhard
 * @version 4.11
 */
public class FIRFilterTest {

    /**
     * Time spin function used for test purposes.
     */
    private class SpinTestFunction extends AbstractVector3DFunction {

        /**
         * Default constructor.
         *
         * @param zeroDate
         *        zero
         */
        public SpinTestFunction(final AbsoluteDate zeroDate) {
            super(zeroDate);
        }

        @Override
        public Vector3D getVector3D(final AbsoluteDate date) {
            final AbsoluteDate zero = this.getZeroDate();
            final double x = date.durationFrom(zero);
            return new Vector3D(2.0 * x, x - 1.0, x * x);
        }
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link FIRFilter#FIRFilter(FilterType, DataType[], List, double)}
     * @testedMethod {@link FIRFilter#compute(fr.cnes.sirius.patrius.math.analysis.UnivariateVectorFunction, double)}
     *
     * @description Here we test the FIRFilter via non-regression for the two different filter
     *              types: CAUSAL and LINEAR. The data type is "OTHER"
     *
     * @input a vector function, a computation point x0, and a coefficients list, a data type array,
     *        a sampling step as the FIRFilter parameters
     *
     * @output the filtered values of the vector function computed at x0.
     *
     * @testPassCriteria the output values are close to the reference results (with a tolerance of
     *                   Precision.DOUBLE_COMPARISON_EPSILON
     * @throws PatriusException if a problem occurs in the FIRFilter
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testComputationFilterTypes() throws PatriusException {
        // Creating parameters of the FIR filter
        final List<Double> coeffs = Arrays.asList(2.0, 8.0, 0.4);
        final DataType[] dataTypeArray = { DataType.OTHER, DataType.OTHER, DataType.OTHER };
        final double samplingStep = 2.0;
        // reference date of the spin function
        final AbsoluteDate zeroDate = AbsoluteDate.J2000_EPOCH;
        // computation point
        final double x0 = 500.5;
        // creation of the spin test function
        final SpinTestFunction fct = new SpinTestFunction(zeroDate);

        // Causal type filter
        final FIRFilter firFilterCausal = new FIRFilter(FilterType.CAUSAL, dataTypeArray, coeffs, samplingStep);
        // filtering the spin function
        final double[] filteredArray = firFilterCausal.compute(fct, x0);
        // Reference filtered values for non-regession test
        final double[] filteredArrayRef = { 10375.2, 5177.2, 2587623.4 };
        // non-regression comparison
        Assert.assertArrayEquals(filteredArrayRef, filteredArray, Precision.DOUBLE_COMPARISON_EPSILON);

        // Linear type filter
        final FIRFilter firFilterLinear = new FIRFilter(FilterType.LINEAR, dataTypeArray, coeffs, samplingStep);
        // filtering the spin function
        final double[] filteredArrayLinear = firFilterLinear.compute(fct, x0);
        // Reference filtered values for non-regession test
        final double[] filteredArrayLinearRef = { 20420.4, 10189.8, 5110333.1 };
        // non-regression comparison
        Assert.assertArrayEquals(filteredArrayLinearRef, filteredArrayLinear, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link FIRFilter#FIRFilter(FilterType, DataType[], List, double)}
     * @testedMethod {@link FIRFilter#compute(fr.cnes.sirius.patrius.math.analysis.UnivariateVectorFunction, double)}
     *
     * @description Here we test the FIRFilter via non-regression for the ANGULAR data type.
     *
     * @input a vector function, a computation point x0, and a coefficients list, a data type array,
     *        a sampling step as the FIRFilter parameters
     *
     * @output the filtered values of the vector function computed at x0.
     *
     * @testPassCriteria the output values are close to the reference results with the PI modulo
     *                   applied (with a tolerance of Precision.DOUBLE_COMPARISON_EPSILON
     * @throws PatriusException if a problem occurs in the FIRFilter
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testComputationFilterAngularData() throws PatriusException {
        // Creating parameters of the FIR filter
        final List<Double> coeffs = Arrays.asList(2.0, 8.0, 0.4);
        final DataType[] dataTypeArray = { DataType.ANGULAR, DataType.ANGULAR, DataType.ANGULAR };
        final double samplingStep = 2.0;
        // reference date of the spin function
        final AbsoluteDate zeroDate = AbsoluteDate.J2000_EPOCH;
        // computation point
        final double x0 = 500.5;
        // creation of the spin test function
        final SpinTestFunction fct = new SpinTestFunction(zeroDate);

        // Causal type filter
        final FIRFilter firFilterCausal = new FIRFilter(FilterType.CAUSAL, dataTypeArray, coeffs, samplingStep);
        // filtering the spin function
        final double[] filteredArray = firFilterCausal.compute(fct, x0);
        // Reference filtered values for non-regession test
        final double[] filteredArrayRef = { 1.6610578465024446, -0.14469311597895285, 0.34538830909878016 };
        // non-regression comparison
        Assert.assertArrayEquals(filteredArrayRef, filteredArray, Precision.DOUBLE_COMPARISON_EPSILON);

    }
}
