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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link LinearCombinationFunction} class.
 *
 * @author bonitt
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* END-HISTORY
 */
public class LinearCombinationFunctionsTest {

    /** Initial state */
    private SpacecraftState state;

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link LinearCombinationFunction#LinearCombinationFunction(Map)}
     * @testedMethod {@link LinearCombinationFunction#LinearCombinationFunction(Collection)}
     * @testedMethod {@link LinearCombinationFunction#value(SpacecraftState)}
     * @testedMethod {@link LinearCombinationFunction#derivativeValue(Parameter, SpacecraftState)}
     * @testedMethod {@link LinearCombinationFunction#getParameters()}
     * @testedMethod {@link LinearCombinationFunction#supportsParameter(Parameter)}
     * @testedMethod {@link LinearCombinationFunction#isDifferentiableBy(Parameter)}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the
     *                   expected data.
     */
    @Test
    public void testLinearCombinationFunction() throws PatriusException {

        // Check the LinearCombinationFunction(map) constructor
        final Parameter zeroValueParam = new Parameter("zeroValue", 0.5);
        final Parameter slopeParam = new Parameter("slope", 2.0);

        final Map<Parameter, Function<SpacecraftState, Double>> map = new LinkedHashMap<>(2);
        map.put(zeroValueParam, state -> 1.0);
        map.put(slopeParam, state -> state.getDate().durationFrom(this.state.getDate()));

        LinearCombinationFunction linearCombinationFunction = new LinearCombinationFunction(map);

        // Check the value, derivativeValue, getParameters, supportsParameter and isDifferentiableBy
        // methods
        Assert.assertEquals(zeroValueParam.getValue() + (10. * slopeParam.getValue()),
                linearCombinationFunction.value(this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(10.,
                linearCombinationFunction.derivativeValue(slopeParam, this.state.shiftedBy(10.)),
                0.);

        Assert.assertEquals(2, linearCombinationFunction.getParameters().size());
        Assert.assertTrue(linearCombinationFunction.getParameters().get(0).equals(zeroValueParam));
        Assert.assertTrue(linearCombinationFunction.getParameters().get(1).equals(slopeParam));

        Assert.assertTrue(linearCombinationFunction.supportsParameter(slopeParam));
        Assert.assertTrue(linearCombinationFunction.isDifferentiableBy(slopeParam));
        Assert.assertFalse(linearCombinationFunction.supportsParameter(new Parameter("param", 1.0)));
        Assert.assertFalse(linearCombinationFunction
                .isDifferentiableBy(new Parameter("param", 1.0)));

        // Check the LinearCombinationFunction(monomialFunctionList) constructor
        final Collection<Function<SpacecraftState, Double>> monomialFunctionList = new ArrayList<>();
        monomialFunctionList.add(state -> 1.0);
        monomialFunctionList.add(state -> state.getDate().durationFrom(this.state.getDate()));

        linearCombinationFunction = new LinearCombinationFunction(monomialFunctionList);

        // Check the value, derivativeValue and getParameters methods
        Assert.assertEquals(2, linearCombinationFunction.getParameters().size());
        final Parameter a0 = linearCombinationFunction.getParameters().get(0);
        final Parameter a1 = linearCombinationFunction.getParameters().get(1);
        Assert.assertTrue(a0.getName().contains("0"));
        Assert.assertTrue(a1.getName().contains("1"));

        Assert.assertEquals(0., linearCombinationFunction.value(this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(10.,
                linearCombinationFunction.derivativeValue(a1, this.state.shiftedBy(10.)), 0.);

        a0.setValue(0.01);
        a1.setValue(1.);

        Assert.assertEquals(0.01 + (10. * 1.),
                linearCombinationFunction.value(this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(10.,
                linearCombinationFunction.derivativeValue(a1, this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(
                0.,
                linearCombinationFunction.derivativeValue(new Parameter("param", 1.0),
                        this.state.shiftedBy(10.)), 0.);
    }

    /**
     * @description Evaluate the {@link LinearCombinationFunction} error cases.
     * 
     * @testedMethod {@link LinearCombinationFunction#LinearCombinationFunction(Map)}
     * @testedMethod {@link LinearCombinationFunction#LinearCombinationFunction(Collection)}
     * 
     * @passCriteria The expected exceptions are returned.
     */
    @Test
    public void testLinearCombinationFunctionExceptions() throws PatriusException {

        final Parameter zeroValueParam = new Parameter("zeroValue", 0.5);
        final Parameter slopeParam = new Parameter("slope", 2.0);
        Map<Parameter, Function<SpacecraftState, Double>> map;

        // Test the LinearCombinationFunction(map) constructor with a null value (should fail)
        try {
            map = new LinkedHashMap<>(2);
            map.put(zeroValueParam, state -> 1.0);
            map.put(slopeParam, null);
            new LinearCombinationFunction(map);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Test the LinearCombinationFunction(map) constructor with a null key (should fail)
        try {
            map = new LinkedHashMap<>(2);
            map.put(zeroValueParam, state -> 1.0);
            map.put(null, state -> state.getDate().durationFrom(this.state.getDate()));
            new LinearCombinationFunction(map);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Test the LinearCombinationFunction(monomialFunctionList) constructor with a null element
        // (should fail)
        try {
            final List<Function<SpacecraftState, Double>> monomialFunctionList = new ArrayList<>();
            monomialFunctionList.add(state -> 1.0);
            monomialFunctionList.add(null);
            new LinearCombinationFunction(monomialFunctionList);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Initial state initialization.
     */
    @Before
    public void setUp() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), date, Constants.GRIM5C1_EARTH_MU);
        this.state = new SpacecraftState(orbit);
    }
}
