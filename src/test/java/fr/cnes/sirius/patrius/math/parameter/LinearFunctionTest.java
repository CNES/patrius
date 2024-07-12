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
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
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
 * Unit test class for the {@link LinearFunction} class.
 *
 * @author bonitt
 */
public class LinearFunctionTest {

    /** Initial state */
    private SpacecraftState state;

    /**
     * @description Builds a new instance and tests the basic getters.
     *
     * @testedMethod {@link LinearFunction#LinearFunction(AbsoluteDate, double, double)}
     * @testedMethod {@link LinearFunction#LinearFunction(AbsoluteDate, Parameter, Parameter)}
     * @testedMethod {@link LinearFunction#getParameters()}
     *
     * @testPassCriteria The instance is build without error and the basic getters return the
     *                   expected data.
     */
    @Test
    public void testLinearFunction() {
        LinearFunction linFct = new LinearFunction(this.state.getDate(), 0.1, 1.2);
        Assert.assertEquals(2, linFct.getParameters().size());
        Assert.assertTrue(linFct.getParameters().get(0).getName().equals("A0"));
        Assert.assertEquals(0.1, linFct.getParameters().get(0).getValue(), 0.);
        Assert.assertTrue(linFct.getParameters().get(1).getName().equals("A1"));
        Assert.assertEquals(1.2, linFct.getParameters().get(1).getValue(), 0.);

        final Parameter paramSlope = new Parameter("slope", 2);
        final Parameter paramZeroValue = new Parameter("zeroValue", 0.5);

        linFct = new LinearFunction(this.state.getDate(), paramZeroValue, paramSlope);

        Assert.assertEquals(2, linFct.getParameters().size());
        Assert.assertTrue(linFct.getParameters().get(0).equals(paramZeroValue));
        Assert.assertTrue(linFct.getParameters().get(1).equals(paramSlope));

    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link LinearFunction} values computation feature.
     * 
     * @testedMethod {@link LinearFunction#value(SpacecraftState)}
     * 
     * @passCriteria The values are computed as expected.
     */
    @Test
    public void testLinearFunctionValues() throws PatriusException {
        // Linear functions initialization
        final int nbPieces = 3;
        final double zeroValue = 0.5;
        final double slope = 2.0;
        final LinearFunction[] linFctTab = new LinearFunction[nbPieces];

        for (int i = 0; i < nbPieces; i++) {
            linFctTab[i] = new LinearFunction(this.state.getDate(), zeroValue, slope * i);
        }

        // Evaluate the values
        Assert.assertEquals(zeroValue, linFctTab[0].value(this.state), 0.);
        Assert.assertEquals(zeroValue, linFctTab[0].value(this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(zeroValue, linFctTab[1].value(this.state), 0.);
        Assert.assertEquals(zeroValue + (slope * 10.),
                linFctTab[1].value(this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(zeroValue, linFctTab[2].value(this.state), 0.);
        Assert.assertEquals(zeroValue + (slope * 2. * 10.),
                linFctTab[2].value(this.state.shiftedBy(10.)), 0.);
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link LinearFunction} derivatives computation feature.
     * 
     * @testedMethod {@link LinearFunction#derivativeValue(Parameter, SpacecraftState)}
     * 
     * @passCriteria The derivatives are computed as expected.
     */
    @Test
    public void testLinearFunctionDerivatives() throws PatriusException {
        // Linear function initialization
        final Parameter paramSlope = new Parameter("slope", 2);
        final Parameter paramZeroValue = new Parameter("zeroValue", 0.5);
        final LinearFunction linFct = new LinearFunction(this.state.getDate(), paramZeroValue,
                paramSlope);

        // Evaluate the derivatives
        Assert.assertEquals(1.0, linFct.derivativeValue(paramZeroValue, this.state.shiftedBy(10.)),
                0.);
        Assert.assertEquals(10.0, linFct.derivativeValue(paramSlope, this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(0.,
                linFct.derivativeValue(new Parameter("random", 1.), this.state.shiftedBy(10.)), 0.);
    }

    /**
     * @description Evaluate the {@link LinearFunction} error cases.
     * 
     * @testedMethod {@link LinearFunction#LinearFunction(AbsoluteDate, Parameter, Parameter)}
     * @testedMethod {@link LinearFunction#getParameters()}
     * 
     * @passCriteria The expected exceptions are returned.
     */
    @Test
    public void testLinearFunctionExceptions() {
        // Linear function initialization
        final Parameter paramSlope = new Parameter("slope", 2);
        final Parameter paramZeroValue = new Parameter("zeroValue", 0.5);
        final LinearFunction linFct = new LinearFunction(this.state.getDate(), paramZeroValue,
                paramSlope);

        // Test constructor with a null attribute
        try {
            new LinearFunction(null, paramZeroValue, paramSlope);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new LinearFunction(this.state.getDate(), null, paramSlope);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            new LinearFunction(this.state.getDate(), paramZeroValue, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Try to access a non-defined parameter (linear function has only two parameters)
        try {
            linFct.getParameters().get(2);
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

        final LinearFunction linFct = new LinearFunction(this.state.getDate(), 1.1, 0.1);
        final LinearFunction deserializedFct = TestUtils.serializeAndRecover(linFct);

        Assert.assertEquals(linFct.value(this.state), deserializedFct.value(this.state), 0.);
        Assert.assertEquals(linFct.value(this.state.shiftedBy(10.)),
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
