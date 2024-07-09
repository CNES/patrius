/**
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
 * 
 * @history creation 05/04/2017
 *
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the {@link PropulsiveProperty} class.
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class PropulsivePropertyTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Propulsive property
         * 
         * @featureDescription Functional tests for the propulsive property
         */
        PROPULSIVE_PROPERTY
    }

    /** Thrust param. */
    private static final String THRUST = "thrust";

    /** ISP param. */
    private static final String ISP = "Isp";

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#PROPULSIVE_PROPERTY}
     * 
     * @testedMethod {@link PropulsiveProperty#PropulsiveProperty(double, double)}
     * @testedMethod {@link PropulsiveProperty#PropulsiveProperty(fr.cnes.sirius.patrius.math.parameter.Parameter, fr.cnes.sirius.patrius.math.parameter.Parameter)}
     * @testedMethod {@link PropulsiveProperty#PropulsiveProperty(fr.cnes.sirius.patrius.math.analysis.IDependentVariable, fr.cnes.sirius.patrius.math.analysis.IDependentVariable)}
     * @testedMethod {@link PropulsiveProperty#getIsp()}
     * @testedMethod {@link PropulsiveProperty#getThrust()}
     * @testedMethod {@link PropulsiveProperty#getIsp(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * @testedMethod {@link PropulsiveProperty#getThrust(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * @testedMethod {@link PropulsiveProperty#getIspParam()}
     * @testedMethod {@link PropulsiveProperty#getThrustParam()}
     * @testedMethod {@link PropulsiveProperty#getType()}
     * 
     * @description Test for all class methods.
     * 
     * @input A name for the property, thrust and isp as constant, param or functions
     * 
     * @output A PropulsiveProperty instance.
     * 
     * @testPassCriteria The instance exists and returns the expected values.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testPropulsiveProperty() {

        // ISP, thrust constants values
        final double isp = 200;
        final double thrust = 1000;

        // Dummy spacecraft
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit testOrbit = new KeplerianOrbit(10000000., 0.93, MathLib.toRadians(75), 0, 0, 0,
            PositionAngle.MEAN,
            FramesFactory.getGCRF(), date.shiftedBy(100.), Constants.EGM96_EARTH_MU);
        final SpacecraftState dummyState = new SpacecraftState(testOrbit);

        // ISP and thrust as functions
        final IDependentVariable<SpacecraftState> ispFunc = new IDependentVariable<SpacecraftState>(){

            @Override
            public double value(final SpacecraftState s) {
                return s.getDate().durationFrom(date);
            }
        };

        final IDependentVariable<SpacecraftState> thrustFunc = new IDependentVariable<SpacecraftState>(){

            @Override
            public double value(final SpacecraftState s) {
                return s.getDate().durationFrom(date);
            }
        };

        // Create the property with available constructors
        final PropulsiveProperty prop1 = new PropulsiveProperty(thrust, isp);
        final PropulsiveProperty prop2 = new PropulsiveProperty(new Parameter(THRUST, thrust), new Parameter(
            ISP, isp));

        // Test getters on name and parameters
        Assert.assertSame(prop1.getPartName(), "");
        prop1.setPartName("Test");
        Assert.assertEquals(prop1.getPartName(), "Test");
        Assert.assertEquals(prop1.getIspParam().getName(), ISP);
        Assert.assertEquals(prop2.getIspParam().getName(), ISP);
        Assert.assertEquals(prop1.getThrustParam().getName(), THRUST);
        Assert.assertEquals(prop2.getThrustParam().getName(), THRUST);
        Assert.assertEquals(prop1.getIspParam().getValue(), prop2.getIspParam().getValue(), 0.);
        Assert.assertEquals(prop1.getThrustParam().getValue(), prop2.getThrustParam().getValue(), 0.);

        // ISP and thrust values are the same retrieved from direct value method or by getted the functions first
        Assert.assertEquals(prop1.getIsp().value(dummyState), prop1.getIsp(dummyState), 0.);
        Assert.assertEquals(prop1.getThrust().value(dummyState), prop1.getThrust(dummyState), 0.);

        // Same values for the two instances
        Assert.assertEquals(prop1.getIsp(dummyState), prop2.getIsp(dummyState), 0.);
        Assert.assertEquals(prop1.getThrust(dummyState), prop2.getThrust(dummyState), 0.);

        // Define the property with functions
        final PropulsiveProperty prop3 = new PropulsiveProperty(thrustFunc, ispFunc);

        // ISP and thrust parameters values are NaN by default since we cannot guess their values a priori
        Assert.assertEquals(true, Double.isNaN(prop3.getIspParam().getValue()));
        Assert.assertEquals(true, Double.isNaN(prop3.getThrustParam().getValue()));

        // Expected functions values wrt their definition
        Assert.assertEquals(prop3.getIsp(dummyState), 100., 0.);
        Assert.assertEquals(prop3.getThrust(dummyState), 100., 0.);

        // Test copy constructor
        final PropulsiveProperty prop4 = new PropulsiveProperty(prop2);
        Assert.assertEquals(prop2.getIspParam().getValue(), prop4.getIspParam().getValue(), 0.);
        Assert.assertEquals(prop2.getThrustParam().getValue(), prop4.getThrustParam().getValue(), 0.);

        // Test serialization
        final PropulsiveProperty prop5 = (PropulsiveProperty) TestUtils.serializeAndRecover(prop2);
        Assert.assertEquals(prop2.getIspParam().getValue(), prop5.getIspParam().getValue(), 0.);
        Assert.assertEquals(prop2.getThrustParam().getValue(), prop5.getThrustParam().getValue(), 0.);

        Assert.assertNotNull(prop1.toString());
        Assert.assertNotNull(prop2.toString());
        Assert.assertNotNull(new PropulsiveProperty(new Parameter("", 0), null).toString());
    }
}
