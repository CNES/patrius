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
 * Unit test class for the {@link NthOrderPolynomialFunction} class.
 *
 * @author bonitt
 */
public class NthOrderPolynomialFunctionTest {

    /** Initial state */
    private SpacecraftState state;

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link NthOrderPolynomialFunction#NthOrderPolynomialFunction(AbsoluteDate, int)}
     * @testedMethod {@link NthOrderPolynomialFunction#NthOrderPolynomialFunction(AbsoluteDate, double...)}
     * @testedMethod {@link NthOrderPolynomialFunction#NthOrderPolynomialFunction(AbsoluteDate, Parameter...)}
     * @testedMethod {@link NthOrderPolynomialFunction#getParameters()}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the
     *                   expected data.
     */
    @Test
    public void testNthOrderPolynomialFunction() throws PatriusException {

        // Evaluate the first constructor: Build a 3rd order polynomial function
        NthOrderPolynomialFunction nOrderFct = new NthOrderPolynomialFunction(this.state.getDate(),
                3);

        // Evaluate the parameters consistency
        Assert.assertEquals(4, nOrderFct.getParameters().size());
        Assert.assertEquals(0., nOrderFct.getParameters().get(0).getValue(), 0.);
        Assert.assertEquals(0., nOrderFct.getParameters().get(1).getValue(), 0.);
        Assert.assertEquals(0., nOrderFct.getParameters().get(2).getValue(), 0.);
        Assert.assertEquals(0., nOrderFct.getParameters().get(3).getValue(), 0.);

        Assert.assertTrue(nOrderFct.getParameters().get(0).getName().contains("0"));
        Assert.assertTrue(nOrderFct.getParameters().get(1).getName().contains("1"));
        Assert.assertTrue(nOrderFct.getParameters().get(2).getName().contains("2"));
        Assert.assertTrue(nOrderFct.getParameters().get(3).getName().contains("3"));

        Assert.assertEquals(0., nOrderFct.value(this.state.shiftedBy(10.)), 0.);

        // Change the parameters values and check the impact
        nOrderFct.getParameters().get(0).setValue(0.01);
        nOrderFct.getParameters().get(1).setValue(1.1);
        nOrderFct.getParameters().get(2).setValue(1.2);
        nOrderFct.getParameters().get(3).setValue(1.3);

        Assert.assertEquals(0.01 + (10. * 1.1) + (100. * 1.2) + (1000. * 1.3),
                nOrderFct.value(this.state.shiftedBy(10.)), 0.);

        // Check the behavior with others constructors
        nOrderFct = new NthOrderPolynomialFunction(this.state.getDate(), 1.1, 1.2, 1.3, 1.4);

        Assert.assertEquals(4, nOrderFct.getParameters().size());
        Assert.assertTrue(nOrderFct.getParameters().get(0).getName().equals("A0"));
        Assert.assertEquals(1.1, nOrderFct.getParameters().get(0).getValue(), 0.);
        Assert.assertTrue(nOrderFct.getParameters().get(1).getName().equals("A1"));
        Assert.assertEquals(1.2, nOrderFct.getParameters().get(1).getValue(), 0.);
        Assert.assertTrue(nOrderFct.getParameters().get(2).getName().equals("A2"));
        Assert.assertEquals(1.3, nOrderFct.getParameters().get(2).getValue(), 0.);
        Assert.assertTrue(nOrderFct.getParameters().get(3).getName().equals("A3"));
        Assert.assertEquals(1.4, nOrderFct.getParameters().get(3).getValue(), 0.);

        final Parameter paramA2 = new Parameter("a2", 3);
        final Parameter paramA1 = new Parameter("a1", 2);
        final Parameter paramA0 = new Parameter("a0", 0.5);
        nOrderFct = new NthOrderPolynomialFunction(this.state.getDate(), paramA0, paramA1, paramA2);

        Assert.assertTrue(nOrderFct.getParameters().get(0).equals(paramA0));
        Assert.assertTrue(nOrderFct.getParameters().get(1).equals(paramA1));
        Assert.assertTrue(nOrderFct.getParameters().get(2).equals(paramA2));
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link NthOrderPolynomialFunction} values computation feature.
     * 
     * @testedMethod {@link NthOrderPolynomialFunction#value(SpacecraftState)}
     * 
     * @passCriteria The values are computed as expected.
     */
    @Test
    public void testNthOrderPolynomialFunctionValues() throws PatriusException {
        // Nth order polynomial functions initialization
        final int nbPieces = 3;
        final double a0 = 0.5;
        final double a1 = 2.0;
        final double a2 = 1.0;
        final NthOrderPolynomialFunction[] nOrderFctTab = new NthOrderPolynomialFunction[nbPieces];

        for (int i = 0; i < nbPieces; i++) {
            nOrderFctTab[i] = new NthOrderPolynomialFunction(this.state.getDate(), a0, a1 * i, a2
                    * i);
        }

        // Evaluate the values
        Assert.assertEquals(a0, nOrderFctTab[0].value(this.state), 0.);
        Assert.assertEquals(a0, nOrderFctTab[0].value(this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(a0, nOrderFctTab[1].value(this.state), 0.);
        Assert.assertEquals(a0 + (a1 * 10.) + (a2 * 10. * 10.),
                nOrderFctTab[1].value(this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(a0, nOrderFctTab[2].value(this.state), 0.);
        Assert.assertEquals(a0 + (a1 * 2. * 10.) + (a2 * 2. * 10. * 10.),
                nOrderFctTab[2].value(this.state.shiftedBy(10.)), 0.);
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link NthOrderPolynomialFunction} derivatives computation feature.
     * 
     * @testedMethod {@link NthOrderPolynomialFunction#derivativeValue(Parameter, SpacecraftState)}
     * 
     * @passCriteria The derivatives are computed as expected.
     */
    @Test
    public void testNthOrderPolynomialFunctionDerivatives() throws PatriusException {
        // Nth order polynomial function initialization
        final Parameter paramA2 = new Parameter("a2", 3);
        final Parameter paramA1 = new Parameter("a1", 2);
        final Parameter paramA0 = new Parameter("a0", 0.5);
        final NthOrderPolynomialFunction nOrderFct = new NthOrderPolynomialFunction(
                this.state.getDate(), paramA0, paramA1, paramA2);

        Assert.assertEquals(1.0, nOrderFct.derivativeValue(paramA0, this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(10.0, nOrderFct.derivativeValue(paramA1, this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(10.0 * 10.0,
                nOrderFct.derivativeValue(paramA2, this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(0.,
                nOrderFct.derivativeValue(new Parameter("random", 1.), this.state.shiftedBy(10.)),
                0.);
    }

    /**
     * @description Evaluate the {@link NthOrderPolynomialFunction} error cases.
     * 
     * @testedMethod {@link NthOrderPolynomialFunction#NthOrderPolynomialFunction(AbsoluteDate, int)}
     * @testedMethod {@link NthOrderPolynomialFunction#NthOrderPolynomialFunction(AbsoluteDate, double...)}
     * @testedMethod {@link NthOrderPolynomialFunction#NthOrderPolynomialFunction(AbsoluteDate, Parameter...)}
     * @testedMethod {@link NthOrderPolynomialFunction#getParameters()}
     * 
     * @passCriteria The expected exceptions are returned.
     */
    @Test
    public void testNthOrderPolynomialFunctionExceptions() {
        // Nth order polynomial function initialization
        final Parameter paramA2 = new Parameter("a2", 3);
        final Parameter paramA1 = new Parameter("a1", 2);
        final Parameter paramA0 = new Parameter("a0", 0.5);
        final NthOrderPolynomialFunction nOrderFct = new NthOrderPolynomialFunction(
                this.state.getDate(), paramA0, paramA1, paramA2);

        // Test constructor with a null attribute
        try {
            new NthOrderPolynomialFunction(null, 2);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new NthOrderPolynomialFunction(null, 2.1);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new NthOrderPolynomialFunction(null, paramA0, paramA1);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new NthOrderPolynomialFunction(this.state.getDate(), paramA0, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to access a non-defined parameter (n+1, with n=2 in this example)
        try {
            nOrderFct.getParameters().get(3);
            Assert.fail();
        } catch (final IndexOutOfBoundsException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the function serialization / deserialization process.
     *
     * @testPassCriteria The function can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final NthOrderPolynomialFunction nOrderFct = new NthOrderPolynomialFunction(
                this.state.getDate(), 1.1, 1.2, 0.1);
        final NthOrderPolynomialFunction deserializedFct = TestUtils.serializeAndRecover(nOrderFct);

        Assert.assertEquals(nOrderFct.value(this.state), deserializedFct.value(this.state), 0.);
        Assert.assertEquals(nOrderFct.value(this.state.shiftedBy(10.)),
                deserializedFct.value(this.state.shiftedBy(10.)), 0.);
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
