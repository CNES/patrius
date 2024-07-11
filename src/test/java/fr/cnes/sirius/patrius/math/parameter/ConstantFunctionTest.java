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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class for the {@link ConstantFunction} class.
 *
 * @author bonitt
 */
public class ConstantFunctionTest {

    /** Initial state */
    private SpacecraftState state;

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link ConstantFunction#ConstantFunction(double)}
     * @testedMethod {@link ConstantFunction#ConstantFunction(String, double)}
     * @testedMethod {@link ConstantFunction#ConstantFunction(Parameter)}
     * @testedMethod {@link ConstantFunction#getParameters()}
     * @testedMethod {@link ConstantFunction#value()}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the
     *                   expected data.
     */
    @Test
    public void testConstantFunction() {

        ConstantFunction cstFct = new ConstantFunction(1.2);
        Assert.assertEquals(1, cstFct.getParameters().size());
        Assert.assertTrue(cstFct.getParameters().get(0).getName().equals("A0"));
        Assert.assertEquals(1.2, cstFct.getParameters().get(0).getValue(), 0.);
        Assert.assertEquals(1.2, cstFct.value(), 0.);

        cstFct = new ConstantFunction("cstA0", 1.1);
        Assert.assertEquals(1, cstFct.getParameters().size());
        Assert.assertTrue(cstFct.getParameters().get(0).getName().equals("cstA0"));
        Assert.assertEquals(1.1, cstFct.getParameters().get(0).getValue(), 0.);
        Assert.assertEquals(1.1, cstFct.value(), 0.);

        final Parameter paramA0 = new Parameter("a0", 0.5);
        cstFct = new ConstantFunction(paramA0);
        Assert.assertEquals(1, cstFct.getParameters().size());
        Assert.assertTrue(cstFct.getParameters().get(0).equals(paramA0));
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link ConstantFunction} values computation feature.
     * 
     * @testedMethod {@link ConstantFunction#value(SpacecraftState)}
     * 
     * @passCriteria The values are computed as expected.
     */
    @Test
    public void testConstantFunctionValues() throws PatriusException {
        // Constant functions initialization
        final int nbPieces = 3;
        final double a0 = 0.5;
        final ConstantFunction[] cstFctTab = new ConstantFunction[nbPieces];

        for (int i = 0; i < nbPieces; i++) {
            cstFctTab[i] = new ConstantFunction(a0 * i);
        }

        // Evaluate the values
        Assert.assertEquals(a0 * 0., cstFctTab[0].value(this.state), 0.);
        Assert.assertEquals(a0 * 0., cstFctTab[0].value(this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(a0 * 1., cstFctTab[1].value(this.state), 0.);
        Assert.assertEquals(a0 * 1., cstFctTab[1].value(this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(a0 * 2., cstFctTab[2].value(this.state), 0.);
        Assert.assertEquals(a0 * 2., cstFctTab[2].value(this.state.shiftedBy(10.)), 0.);
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link ConstantFunction} derivatives computation feature.
     * 
     * @testedMethod {@link ConstantFunction#derivativeValue(Parameter, SpacecraftState)}
     * 
     * @passCriteria The derivatives are computed as expected.
     */
    @Test
    public void testConstantFunctionDerivatives() throws PatriusException {
        // Constant function initialization
        final Parameter paramA0 = new Parameter("a0", 0.5);
        final ConstantFunction cstFct = new ConstantFunction(paramA0);

        // Evaluate the derivatives
        Assert.assertEquals(1.0, cstFct.derivativeValue(paramA0, this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(0.,
                cstFct.derivativeValue(new Parameter("random", 1.), this.state.shiftedBy(10.)), 0.);
    }

    /**
     * @description Evaluate the {@link ConstantFunction} error cases.
     * 
     * @testedMethod {@link ConstantFunction#ConstantFunction(Parameter)}
     * @testedMethod {@link ConstantFunction#getParameters()}
     * 
     * @passCriteria The expected exceptions are returned.
     */
    @Test
    public void testConstantFunctionExceptions() {
        // Constant function initialization
        final ConstantFunction cstFct = new ConstantFunction("cstA0", 1.1);

        // Test constructor with a null attribute
        try {
            new ConstantFunction(null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to access a non-defined parameter (constant function has only one parameter)
        try {
            cstFct.getParameters().get(1);
            Assert.fail();
        } catch (final IndexOutOfBoundsException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @description Evaluate the function serialization / deserialization process.
     *
     * @testPassCriteria The function can be serialized and deserialized.
     */
    @Test
    public void testSerialization() {

        final ConstantFunction cstFct = new ConstantFunction("cstA0", 1.1);
        final ConstantFunction deserializedFct = TestUtils.serializeAndRecover(cstFct);

        Assert.assertEquals(1.1, deserializedFct.value(), 0.);
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
