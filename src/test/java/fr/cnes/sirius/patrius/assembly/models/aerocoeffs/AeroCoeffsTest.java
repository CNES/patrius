/**
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
 * 
 * @history creation 23/05/2018
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.aerocoeffs;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.US76;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class AeroCoeffsTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Aerodynamic coefficient
         * 
         * @featureDescription Aerodynamic coefficient computation
         * 
         * @coveredRequirements DV-VEHICULE_480, DV-VEHICULE_490
         */
        AERO_COEFFICIENT,
    }

    private static FramesConfiguration config_svg;

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-dataPBASE");
        config_svg = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FramesFactory.setConfiguration(config_svg);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_COEFFICIENT}
     * 
     * @testedMethod {@link AeroCoeffByAltitude#value(SpacecraftState)}
     * 
     * @description check that the function aerodynamic coefficient = f(altitude) is correct
     * 
     * @input altitude
     * 
     * @output aerodynamic coefficient
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void aeroCoeffByAltitudeTest() {

        final EllipsoidBodyShape earthShape = new OneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "Earth");

        // Initialization
        final double[] xVariables = { 100E3, 200E3, 300E3, 400E3, 500E3, 600E3 };
        final double[] yVariables = { 2., 2.1, 2.2, 2.3, 2.4, 2.5 };
        final AeroCoeffByAltitude aeroCoeff = new AeroCoeffByAltitude(xVariables, yVariables, earthShape);

        // State
        final Orbit orbit =
            new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH, Constants.GRIM5C1_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Check (altitude is 551863.54)
        final double actual = aeroCoeff.value(state);
        final double expectedAltitude = 7000000 * (1. - 0.01) - Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double expected = 2.4 + (expectedAltitude - 500E3) / (600E3 - 500E3) * (2.5 - 2.4);
        Assert.assertEquals(expected, actual, 1E-14);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_COEFFICIENT}
     * 
     * @testedMethod {@link AeroCoeffByAoA#value(SpacecraftState)}
     * 
     * @description check that the function aerodynamic coefficient = f(angle of attack) is correct
     * 
     * @input angle of attack
     * 
     * @output aerodynamic coefficient
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void aeroCoeffByAoATest() throws PatriusException {

        final EllipsoidBodyShape earthShape = new OneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "Earth");
        final AttitudeLaw attitudeLaw =
            new ConstantAttitudeLaw(FramesFactory.getGCRF(), new Rotation(Vector3D.PLUS_I, FastMath.PI / 2.));

        // Initialization
        final double[] xVariables = { 0., 0.1, 0.2, 0.3, 0.4, 0.5 };
        final double[] yVariables = { 2., 2.1, 2.2, 2.3, 2.4, 2.5 };
        final AeroCoeffByAoA aeroCoeff = new AeroCoeffByAoA(xVariables, yVariables, earthShape);

        // State
        final AbsoluteDate date = new AbsoluteDate(2005, 01, 01, TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date,
                Constants.GRIM5C1_EARTH_MU);
        final Attitude attitude = attitudeLaw.getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Check (angle of attack is 0)
        final double actual = aeroCoeff.value(state);
        final double expectedAngleOfAttack = 0;
        final double expected = 2. + (expectedAngleOfAttack - 0.) / (0.1 - 0.) * (2.1 - 2.);
        Assert.assertEquals(expected, actual, 1E-14);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_COEFFICIENT}
     * 
     * @testedMethod {@link AeroCoeffByMach#value(SpacecraftState)}
     * 
     * @description check that the function aerodynamic coefficient = f(Mach number) is correct
     * 
     * @input Mach number
     * 
     * @output aerodynamic coefficient
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void aeroCoeffByMachTest() throws PatriusException {

        final EllipsoidBodyShape earthShape = new OneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final Atmosphere atmosphere = new US76(earthShape);
        final AttitudeLaw attitudeLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);

        // Initialization
        final double[] xVariables = { 7., 7.2, 7.4, 7.6, 7.8, 8. };
        final double[] yVariables = { 2., 2.1, 2.2, 2.3, 2.4, 2.5 };
        final AeroCoeffByMach aeroCoeff = new AeroCoeffByMach(xVariables, yVariables, atmosphere);

        // State
        final AbsoluteDate date = new AbsoluteDate(2005, 01, 01, TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date,
                Constants.GRIM5C1_EARTH_MU);
        final Attitude attitude = attitudeLaw.getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Check (Mach is 7.5336096244782444)
        final double actual = aeroCoeff.value(state);
        final double expectedMach = 7.5336096244782444;
        final double expected = 2.2 + (expectedMach - 7.4) / (7.6 - 7.4) * (2.3 - 2.2);
        Assert.assertEquals(expected, actual, 1E-14);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_COEFFICIENT}
     * 
     * @testedMethod {@link AeroCoeffByAoAAndMach#value(SpacecraftState)}
     * 
     * @description check that the function aerodynamic coefficient = f(angle of attack, Mach number) is correct
     * 
     * @input angle of attack, Mach number
     * 
     * @output aerodynamic coefficient
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void aeroCoeffByAoAAndMachTest() throws PatriusException {

        final EllipsoidBodyShape earthShape = new OneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final Atmosphere atmosphere = new US76(earthShape);
        final AttitudeLaw attitudeLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);

        // Initialization
        final double[] xVariables = { 0., 0.1, 0.2, 0.3, 0.4, 0.5 };
        final double[] yVariables = { 7., 7.2, 7.4, 7.6, 7.8, 8. };
        final double[][] zVariables = {
            { 2., 2.1, 2.2, 2.3, 2.4, 2.5 },
            { 2., 2.1, 2.2, 2.3, 2.4, 2.5 },
            { 2., 2.1, 2.2, 2.3, 2.4, 2.5 },
            { 2., 2.1, 2.2, 2.3, 2.4, 2.5 },
            { 2., 2.1, 2.2, 2.3, 2.4, 2.5 },
            { 2., 2.1, 2.2, 2.3, 2.4, 2.5 },
        };
        final AeroCoeffByAoAAndMach aeroCoeff =
            new AeroCoeffByAoAAndMach(xVariables, yVariables, zVariables, atmosphere, earthShape);

        // State
        final AbsoluteDate date = new AbsoluteDate(2005, 01, 01, TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), date,
                Constants.GRIM5C1_EARTH_MU);
        final Attitude attitude = attitudeLaw.getAttitude(orbit);
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Check (Mach is 7.5336096244782444)
        final double actual = aeroCoeff.value(state);
        final double expectedMach = 7.5336096244782444;
        final double expected = 2.2 + (expectedMach - 7.4) / (7.6 - 7.4) * (2.3 - 2.2);
        Assert.assertEquals(expected, actual, 1E-14);

        // Checks
        Assert.assertEquals(0, aeroCoeff.getParameters().size(), 0);
        Assert.assertFalse(aeroCoeff.supportsParameter(null));
        Assert.assertEquals(0, aeroCoeff.derivativeValue(null, null), 0);
        Assert.assertFalse(aeroCoeff.isDifferentiableBy(null));
        Assert.assertEquals(xVariables, aeroCoeff.getAoAArray());
        Assert.assertEquals(yVariables, aeroCoeff.getMachArray());
        Assert.assertEquals(zVariables, aeroCoeff.getAerodynamicCoefficientsArray());
        Assert.assertEquals(xVariables[2], aeroCoeff.getFunction().getxtab()[2]);
        Assert.assertEquals(yVariables[2], aeroCoeff.getFunction().getytab()[2]);
        Assert.assertEquals(zVariables[2][3], aeroCoeff.getFunction().getValues()[2][3]);
        Assert.assertNotNull(aeroCoeff.toString());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_COEFFICIENT}
     * 
     * @testedMethod {@link AeroCoeffByAoAAndMach#value(SpacecraftState)}
     * 
     * @description check that the function aerodynamic coefficient = f(angle of attack, Mach number) is correct
     * 
     * @input angle of attack, Mach number
     * 
     * @output aerodynamic coefficient
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void aeroCoeffConstantTest() {

        // Initialization
        final Parameter p = new Parameter("Parameter", 2.8);
        final AeroCoeffConstant aeroCoeff = new AeroCoeffConstant(p);

        // Check
        final double actual = aeroCoeff.value(null);
        final double expected = 2.8;
        Assert.assertEquals(expected, actual, 1E-14);

        // Checks
        Assert.assertEquals(0, aeroCoeff.getParameters().size(), 1);
        Assert.assertEquals(0, aeroCoeff.derivativeValue(p, null), 1);
        Assert.assertEquals(2.8, aeroCoeff.getAerodynamicCoefficient());
        Assert.assertTrue(aeroCoeff.supportsParameter(p));
        Assert.assertTrue(aeroCoeff.isDifferentiableBy(p));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_COEFFICIENT}
     * 
     * @testedMethod {@link AbstractAeroCoeff1D#getParameters()}
     * @testedMethod {@link AbstractAeroCoeff1D#supportsParameter(fr.cnes.sirius.patrius.math.parameter.Parameter)}
     * @testedMethod {@link AbstractAeroCoeff1D#derivativeValue(fr.cnes.sirius.patrius.math.parameter.Parameter, SpacecraftState)}
     * @testedMethod {@link AbstractAeroCoeff1D#isDifferentiableBy(fr.cnes.sirius.patrius.math.parameter.Parameter)}
     * @testedMethod {@link AbstractAeroCoeff1D#getXArray()}
     * @testedMethod {@link AbstractAeroCoeff1D#getYArray()}
     * @testedMethod {@link AbstractAeroCoeff1D#getFunction()}
     * @testedMethod {@link AbstractAeroCoeff1D#toString()}
     * 
     * @description check that minors methods returns output as expected (functional test)
     * 
     * @input {@link AeroCoeffByAltitude}
     * 
     * @output methods output
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void aeroCoeffOneVarFunctionalTest() {

        final EllipsoidBodyShape earthShape = new OneAxisEllipsoid(
            Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "Earth");

        // Initialization
        final double[] xVariables = { 100E3, 200E3, 300E3, 400E3, 500E3, 600E3 };
        final double[] yVariables = { 2., 2.1, 2.2, 2.3, 2.4, 2.5 };
        final AeroCoeffByAltitude aeroCoeff = new AeroCoeffByAltitude(xVariables, yVariables, earthShape);

        // Checks
        Assert.assertEquals(0, aeroCoeff.getParameters().size(), 0);
        Assert.assertFalse(aeroCoeff.supportsParameter(null));
        Assert.assertEquals(0, aeroCoeff.derivativeValue(null, null), 0);
        Assert.assertFalse(aeroCoeff.isDifferentiableBy(null));
        Assert.assertEquals(xVariables, aeroCoeff.getXArray());
        Assert.assertEquals(yVariables, aeroCoeff.getYArray());
        Assert.assertEquals(xVariables[2], aeroCoeff.getFunction().getxtab()[3]);
        Assert.assertEquals(yVariables[2], aeroCoeff.getFunction().getValues()[3]);
        Assert.assertNotNull(aeroCoeff.toString());
    }
}
