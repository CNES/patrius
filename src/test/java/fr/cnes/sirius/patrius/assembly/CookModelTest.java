/**
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:600:16/06/2016:add Cook (Cn, Ct) models
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.models.cook.AlphaCookModel;
import fr.cnes.sirius.patrius.assembly.models.cook.AlphaProvider;
import fr.cnes.sirius.patrius.assembly.models.cook.CnCookModel;
import fr.cnes.sirius.patrius.assembly.models.cook.CookWallGasTemperature;
import fr.cnes.sirius.patrius.assembly.models.cook.CtCookModel;
import fr.cnes.sirius.patrius.assembly.models.cook.GinsWallGasTemperature;
import fr.cnes.sirius.patrius.assembly.models.cook.WallGasTemperatureProvider;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the Cook models (Cn and Ct).
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: CookModelTest.java 18100 2017-10-03 10:04:21Z bignon $
 * 
 * @since 3.3
 */
public class CookModelTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test Cook model
         * 
         * @featureDescription test Cook model (Cn and Ct)
         * 
         * @coveredRequirements DM-600
         */
        COOK_MODEL
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(CookModelTest.class.getSimpleName(), "Cook model");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COOK_MODEL}
     * 
     * @testedMethod {@link CnCookModel#value(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * @testedMethod {@link CtCookModel#value(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * 
     * @description check the value returned by Cn and Ct Cook model
     * 
     * @input data
     * 
     * @output Cn and Ct
     * 
     * @testPassCriteria Results is the same as reference (Scilab 5.5.2, relative threshold: 1E-12)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCookModel() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testCookModel", "Cook model", "Scilab 5.5.2", 1E-12, ComparisonType.RELATIVE);

        // Bird, Cook and Gins versions with positive and negative incidence
        this.test("Bird - i > 0", 6.7423645550951194e-01, 7.9939745433382958e-01, 8000., 900., 12E-3, 0.5, 0.05, 300.,
            4.,
            16E-3, false, false);
        this.test("Cook - i > 0", 1.0454702771950726e+00, 9.9749498660407776e-01, 8300., 1050., 13.4E-3, 0.75, 0.,
            360., 4.,
            0., false, false);
        this.test("Gins - i > 0", 1.3948260071496854e-01, 3.5984815676023429e-01, 8000., 1000., 13E-3, 0.2, 0.08, 400.,
            4.,
            16E-3, true, false);

        this.test("Bird - i < 0", 2.5517072346053971e-09, 1.8766327702943358e-08, 8000., 900., 12E-3, -0.5, 0.05, 300.,
            4.,
            16E-3, false, false);
        this.test("Cook - i < 0", 3.1101487187326426e-15, 2.3433388273331572e-14, 8300., 1050., 13.4E-3, -0.75, 0.,
            360.,
            4., 0., false, false);
        this.test("Gins - i < 0", 2.6494281949804310e-04, 1.5832818362758476e-03, 8000., 1000., 13E-3, -0.2, 0.08,
            400., 4.,
            16E-3, true, false);

        // Constant wall temperature case
        this.test("Constant", 6.7423645550951194e-01, 7.9939745433382958e-01, 8000., 900., 12E-3, 0.5, 0.05, 300., 4.,
            16E-3, false, true);
    }

    /**
     * Generic test.
     */
    private void test(final String name, final double cnExpected, final double ctExpected, final double vrel,
                      final double temp, final double molarMass,
                      final double incidence, final double epsilon, final double surfaceTemp, final double kValue,
                      final double wallGasMolarMassValue,
                      final boolean isGins, final boolean isConstant) {

        // Initial state
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7000000, 0, 0), new Vector3D(0, 0, 0)),
            FramesFactory.getEME2000(), date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Initialization
        final double eps = 5E-12;
        final MyAtmosphere atmosphere = new MyAtmosphere(-vrel, temp, molarMass);
        final Facet facet = new Facet(new Vector3D(MathLib.cos(FastMath.PI / 2. - incidence), MathLib.sin(FastMath.PI
            / 2. - incidence), 0.), 1.);
        final AlphaProvider alpha = new AlphaCookModel(atmosphere, kValue, wallGasMolarMassValue);
        final WallGasTemperatureProvider wallGasTemperatureProvider = isGins ? new GinsWallGasTemperature(atmosphere,
            alpha, surfaceTemp) : new CookWallGasTemperature(atmosphere, alpha, surfaceTemp);
        final IParamDiffFunction cnCook = isConstant ? new CnCookModel(atmosphere, facet, FramesFactory.getEME2000(),
            epsilon, 2074.5301772792236) :
            new CnCookModel(atmosphere, facet, FramesFactory.getEME2000(), epsilon, wallGasTemperatureProvider);
        final IParamDiffFunction ctCook = new CtCookModel(atmosphere, facet, FramesFactory.getEME2000(), epsilon);

        // Computation
        final double cnActual = cnCook.value(state);
        final double ctActual = ctCook.value(state);

        // Check results
        Assert.assertEquals(0., (cnExpected - cnActual) / cnExpected, eps);
        Assert.assertEquals(0., (ctExpected - ctActual) / ctExpected, eps);

        Report.printToReport("Cn Cook (" + name + ")", cnExpected, cnActual);
        Report.printToReport("Ct Cook (" + name + ")", ctExpected, ctActual);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COOK_MODEL}
     * 
     * @testedMethod {@link CnCookModel#derivativeValue()}
     * @testedMethod {@link CtCookModel#derivativeValue()}
     * @testedMethod {@link CnCookModel#isDifferentiableBy()}
     * @testedMethod {@link CtCookModel#isDifferentiableBy()}
     * @testedMethod {@link CnCookModel#supportsParameter()}
     * @testedMethod {@link CtCookModel#supportsParameter()}
     * @testedMethod {@link CnCookModel#getParameters()}
     * @testedMethod {@link CtCookModel#getParameters()}
     * 
     * @description check Cn and Ct Cook model do not have derivable parameters
     * 
     * @input data
     * 
     * @output no derivable parameters
     * 
     * @testPassCriteria Cn and Ct Cook model do not have derivable parameters
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testDerivableParameters() throws PatriusException {

        // Initialization
        final MyAtmosphere atmosphere = new MyAtmosphere(1, 2, 3);
        final Facet facet = new Facet(new Vector3D(MathLib.cos(FastMath.PI / 2. - 4),
            MathLib.sin(FastMath.PI / 2. - 4), 0.), 1.);
        final AlphaProvider alpha = new AlphaCookModel(atmosphere, 1, 2);
        final WallGasTemperatureProvider wallGasTemperatureProvider =
            new CookWallGasTemperature(atmosphere, alpha, 10.);
        final IParamDiffFunction cnCook = new CnCookModel(atmosphere, facet, FramesFactory.getEME2000(), 0.5,
            wallGasTemperatureProvider);
        final IParamDiffFunction ctCook = new CtCookModel(atmosphere, facet, FramesFactory.getEME2000(), 0.5);

        // Check
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        final Parameter parameter = new Parameter("param", 0);

        Assert.assertEquals(0., cnCook.derivativeValue(parameter, state), 0.);
        Assert.assertEquals(0., ctCook.derivativeValue(parameter, state), 0.);
        Assert.assertFalse(cnCook.isDifferentiableBy(parameter));
        Assert.assertFalse(ctCook.isDifferentiableBy(parameter));
        Assert.assertFalse(cnCook.supportsParameter(parameter));
        Assert.assertFalse(ctCook.supportsParameter(parameter));
        Assert.assertEquals(0, cnCook.getParameters().size());
        Assert.assertEquals(0, ctCook.getParameters().size());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COOK_MODEL}
     * 
     * @testedMethod {@link CnCookModel#value(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * @testedMethod {@link CtCookModel#value(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * 
     * @description check exception are thrown as expected if incorrect input data is provided
     * 
     * @input data
     * 
     * @output exception
     * 
     * @testPassCriteria Results is the same as reference (Scilab 5.5.2, relative threshold: 1E-12)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCookModelException() throws PatriusException {

        Utils.setDataRoot("almanac");

        // Initialization
        final CelestialBody sun = new MeeusSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());
        final ExtendedAtmosphere atmosphere = new MSISE2000(new ContinuousMSISE2000SolarData(new ConstantSolarActivity(
            140., 15.)), earth, sun);
        final Facet facet = new Facet(new Vector3D(MathLib.cos(FastMath.PI / 2. - 0.1),
            MathLib.sin(FastMath.PI / 2. - 0.1), 0.), 1.);

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Alpha computation exception
        final AlphaProvider alpha = new AlphaCookModel(atmosphere, 1., 1.);
        try {
            alpha.getAlpha(state);
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertTrue(true);
        }

        // Cn computation exception
        final CnCookModel cn = new CnCookModel(atmosphere, facet, FramesFactory.getEME2000(), 1., 1.);
        try {
            cn.value(state);
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertTrue(true);
        }

        // Ct computation exception
        final CtCookModel ct = new CtCookModel(atmosphere, facet, FramesFactory.getEME2000(), 1.);
        try {
            ct.value(state);
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertTrue(true);
        }

        // Cook wall gaz temperature computation exception
        final CookWallGasTemperature cook = new CookWallGasTemperature(atmosphere, alpha, 1.);
        try {
            cook.getWallGasTemperature(state, Vector3D.PLUS_I, 1.);
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertTrue(true);
        }

        // Gins wall gaz temperature computation exception
        final GinsWallGasTemperature gins = new GinsWallGasTemperature(atmosphere, alpha, 1.);
        try {
            gins.getWallGasTemperature(state, Vector3D.PLUS_I, 1.);
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertTrue(true);
        }

    }

    /**
     * Local atmosphere for tests.
     */
    private class MyAtmosphere implements ExtendedAtmosphere {

        /** Relative velocity. */
        private final double vrel;

        /** Temperature. */
        private final double temp;

        /** Molar mass. */
        private final double molarMass;

        public MyAtmosphere(final double vrel, final double temp, final double molarMass) {
            this.vrel = vrel;
            this.temp = temp;
            this.molarMass = molarMass;
        }

        @Override
        public double
                getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // Unused
            return 0;
        }

        @Override
        public Vector3D
                getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                throws PatriusException {
            return new Vector3D(this.vrel, 0, 0);
        }

        @Override
        public
                double
                getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                    throws PatriusException {
            // Unused
            return 0;
        }

        @Override
        public AtmosphereData
                getData(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // 0 set for unused data
            // Values have been chosen to result in a molar mass of molarMass
            final double c = Constants.AVOGADRO_CONSTANT * AtmosphereData.HYDROGEN_MASS;
            return new AtmosphereData(1., this.temp, 0, 0, 0, 0, 0, 0, 1, 0, (1. - this.molarMass / c)
                / (this.molarMass / c - 16.));
        }

        @Override
        public Atmosphere copy() {
            return null;
        }
        
        /** {@inheritDoc} */
        @Override
        public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
            // Nothing to do
        }
    }
}
