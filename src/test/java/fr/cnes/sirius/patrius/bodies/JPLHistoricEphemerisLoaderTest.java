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
 * HISTORY
 * VERSION:4.11:FA:FA-3279:22/05/2023:[PATRIUS] Absence de TU pour JPLHistoricEphemerisLoader
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the validation of the {@link JPLHistoricEphemerisLoader} class.
 */
public class JPLHistoricEphemerisLoaderTest {

    /**
     * Test for the validation of the JPL constants.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the JPL constants.
     * @testedMethod {@link JPLHistoricEphemerisLoader#getLoadedAstronomicalUnit()}
     * @testedMethod {@link JPLHistoricEphemerisLoader#getLoadedEarthMoonMassRatio()}
     * @testPassCriteria The JPL constants are as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testConstantsJPL() throws PatriusException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        final JPLHistoricEphemerisLoader loader = new JPLHistoricEphemerisLoader(
            JPLHistoricEphemerisLoader.DEFAULT_DE_SUPPORTED_NAMES,
                EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30056, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    /**
     * Test for the validation of the inpop constants.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the inpop constants.
     * @testedMethod {@link JPLHistoricEphemerisLoader#getLoadedAstronomicalUnit()}
     * @testedMethod {@link JPLHistoricEphemerisLoader#getLoadedEarthMoonMassRatio()}
     * @testPassCriteria The inpop constants are as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testConstantsInpop() throws PatriusException {
        Utils.setDataRoot("inpop");
        final JPLHistoricEphemerisLoader loader = new JPLHistoricEphemerisLoader(
            JPLHistoricEphemerisLoader.DEFAULT_INPOP_SUPPORTED_NAMES,
            EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30057, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    /**
     * Test for the validation of the JPL gravitational coefficients.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the JPL gravitational coefficients.
     * @testedMethod {@link JPLHistoricEphemerisLoader#getLoadedGravitationalCoefficient(String)}
     * @testPassCriteria The JPL gravitational coefficients are as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGMJPL() throws PatriusException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        final JPLHistoricEphemerisLoader loader = new JPLHistoricEphemerisLoader(
            JPLHistoricEphemerisLoader.DEFAULT_DE_SUPPORTED_NAMES,
                EphemerisType.SUN);
        Assert.assertEquals(22032.080e9, loader
                .getLoadedGravitationalCoefficient(EphemerisType.MERCURY),
                1.0e6);
        Assert.assertEquals(324858.599e9,
                loader.getLoadedGravitationalCoefficient(EphemerisType.VENUS),
                1.0e6);
        Assert.assertEquals(42828.314e9,
                loader.getLoadedGravitationalCoefficient(EphemerisType.MARS),
                1.0e6);
        Assert.assertEquals(126712767.863e9, loader
                .getLoadedGravitationalCoefficient(EphemerisType.JUPITER),
                6.0e7);
        Assert.assertEquals(
                37940626.063e9,
                loader.getLoadedGravitationalCoefficient(EphemerisType.SATURN),
                2.0e6);
        Assert.assertEquals(
                5794549.007e9,
                loader.getLoadedGravitationalCoefficient(EphemerisType.URANUS),
                1.0e6);
        Assert.assertEquals(6836534.064e9, loader
                .getLoadedGravitationalCoefficient(EphemerisType.NEPTUNE),
                1.0e6);
        Assert.assertEquals(981.601e9,
                loader.getLoadedGravitationalCoefficient(EphemerisType.PLUTO),
                1.0e6);
        Assert.assertEquals(132712440017.987e9,
                loader.getLoadedGravitationalCoefficient(EphemerisType.SUN),
                1.0e6);
        Assert.assertEquals(4902.801e9,
                loader.getLoadedGravitationalCoefficient(EphemerisType.MOON),
                1.0e6);
        Assert.assertEquals(403503.233e9, loader
                .getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON),
                1.0e6);
    }

    /**
     * Test for the validation of the inpop gravitational coefficients.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the inpop gravitational coefficients.
     * @testedMethod {@link JPLHistoricEphemerisLoader#getLoadedGravitationalCoefficient(String)}
     * @testPassCriteria The inpop gravitational coefficients are as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGMInpop() throws PatriusException {

        Utils.setDataRoot("inpop");

        final JPLHistoricEphemerisLoader loader = new JPLHistoricEphemerisLoader(
            JPLHistoricEphemerisLoader.DEFAULT_INPOP_SUPPORTED_NAMES,
            EphemerisType.SUN);
        Assert.assertEquals(22032.081e9, loader
            .getLoadedGravitationalCoefficient(EphemerisType.MERCURY),
            1.0e6);
        Assert.assertEquals(324858.597e9,
            loader.getLoadedGravitationalCoefficient(EphemerisType.VENUS),
            1.0e6);
        Assert.assertEquals(42828.376e9,
            loader.getLoadedGravitationalCoefficient(EphemerisType.MARS),
            1.0e6);
        Assert.assertEquals(126712764.535e9, loader
            .getLoadedGravitationalCoefficient(EphemerisType.JUPITER),
            6.0e7);
        Assert.assertEquals(
            37940585.443e9,
            loader.getLoadedGravitationalCoefficient(EphemerisType.SATURN),
            2.0e6);
        Assert.assertEquals(
            5794549.099e9,
            loader.getLoadedGravitationalCoefficient(EphemerisType.URANUS),
            1.0e6);
        Assert.assertEquals(6836527.128e9, loader
            .getLoadedGravitationalCoefficient(EphemerisType.NEPTUNE),
            1.0e6);
        Assert.assertEquals(971.114e9,
            loader.getLoadedGravitationalCoefficient(EphemerisType.PLUTO),
            1.0e6);
        Assert.assertEquals(132712442110.032e9,
            loader.getLoadedGravitationalCoefficient(EphemerisType.SUN),
            1.0e6);
        Assert.assertEquals(4902.800e9,
            loader.getLoadedGravitationalCoefficient(EphemerisType.MOON),
            1.0e6);
        Assert.assertEquals(403503.250e9, loader
            .getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON),
            1.0e6);
    }

    /**
     * Test for the validation of the derivative for the de405-ephemerides file.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the derivative for the de405-ephemerides file.
     * @testedMethod {@link JPLHistoricEphemerisLoader#JPLHistoricEphemerisLoader(String, EphemerisType)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#loadCelestialBodyEphemeris(String)}
     * @testPassCriteria The derivative for the de405-ephemerides file is as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDerivative405() throws PatriusException {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        checkDerivative(JPLHistoricEphemerisLoader.DEFAULT_DE_SUPPORTED_NAMES, new AbsoluteDate(1969, 6,
                25, TimeScalesFactory.getTT()));
    }

    /**
     * Test for the validation of the derivative for the de406-ephemerides file.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the derivative for the de406-ephemerides file.
     * @testedMethod {@link JPLHistoricEphemerisLoader#JPLHistoricEphemerisLoader(String, EphemerisType)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#loadCelestialBodyEphemeris(String)}
     * @testPassCriteria The derivative for the de406-ephemerides file is as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testDerivative406() throws PatriusException {
        Utils.setDataRoot("regular-data:regular-data/de406-ephemerides");
        checkDerivative(JPLHistoricEphemerisLoader.DEFAULT_DE_SUPPORTED_NAMES, new AbsoluteDate(2964, 9,
                26, TimeScalesFactory.getTT()));
    }

    /**
     * Test for the validation of the inpop big and little endians.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the inpop big and little endians.
     * @testedMethod {@link JPLHistoricEphemerisLoader#JPLHistoricEphemerisLoader(String, EphemerisType)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#loadCelestialBodyEphemeris(String)}
     * @testPassCriteria The inpop big and little endians are as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testEndianness() throws PatriusException {
        Utils.setDataRoot("inpop");
        final EphemerisType type = EphemerisType.MARS;
        final JPLHistoricEphemerisLoader loaderInpopTCBBig = new JPLHistoricEphemerisLoader(
            "^inpop.*_TCB_.*_bigendian\\.dat$", type);
        final CelestialBodyEphemeris bodysInpopTCBBig = loaderInpopTCBBig
            .loadCelestialBodyEphemeris(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        final JPLHistoricEphemerisLoader loaderInpopTCBLittle = new JPLHistoricEphemerisLoader(
            "^inpop.*_TCB_.*_littleendian\\.dat$", type);
        final CelestialBodyEphemeris bodysInpopTCBLittle = loaderInpopTCBLittle
            .loadCelestialBodyEphemeris(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBLittle.getLoadedConstant("TIMESC"), 1.0e-10);
        final AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4,
            TimeScalesFactory.getTT());
        final Frame eme2000 = FramesFactory.getEME2000();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            final AbsoluteDate date = t0.shiftedBy(dt);
            final Vector3D pInpopTCBBig = bodysInpopTCBBig.getPVCoordinates(date, eme2000)
                .getPosition();
            final Vector3D pInpopTCBLittle = bodysInpopTCBLittle.getPVCoordinates(date, eme2000)
                .getPosition();
            Assert.assertEquals(0.0, pInpopTCBBig.distance(pInpopTCBLittle), 1.0e-10);
        }
        for (final String name : DataProvidersManager.getInstance().getLoadedDataNames()) {
            Assert.assertTrue(name.contains("inpop"));
        }
    }

    /**
     * Test for the validation of the inpop vs JPL big and little endians.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test for the validation of the inpop vs JPL big and little endians.
     * @testedMethod {@link JPLHistoricEphemerisLoader#JPLHistoricEphemerisLoader(String, EphemerisType)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#loadCelestialBodyEphemeris(String)}
     * @testPassCriteria The inpop vs JPL big and little endians are as expected.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testInpopvsJPL() throws PatriusException {
        Utils.setDataRoot("regular-data:inpop");
        final EphemerisType type = EphemerisType.MARS;
        final JPLHistoricEphemerisLoader loaderDE405 = new JPLHistoricEphemerisLoader(
            "^unxp(\\d\\d\\d\\d)\\.405$", type);
        final CelestialBodyEphemeris bodysDE405 = loaderDE405.loadCelestialBodyEphemeris(CelestialBodyFactory.MARS);
        final JPLHistoricEphemerisLoader loaderInpopTDBBig = new JPLHistoricEphemerisLoader(
            "^inpop.*_TDB_.*_bigendian\\.dat$", type);
        final CelestialBodyEphemeris bodysInpopTDBBig = loaderInpopTDBBig
            .loadCelestialBodyEphemeris(CelestialBodyFactory.MARS);
        Assert.assertEquals(0.0, loaderInpopTDBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        final JPLHistoricEphemerisLoader loaderInpopTCBBig = new JPLHistoricEphemerisLoader(
            "^inpop.*_TCB_.*_bigendian\\.dat$", type);
        final CelestialBodyEphemeris bodysInpopTCBBig = loaderInpopTCBBig
            .loadCelestialBodyEphemeris(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        final AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4,
            TimeScalesFactory.getTT());
        final Frame gcrf = FramesFactory.getGCRF();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            final AbsoluteDate date = t0.shiftedBy(dt);
            final Vector3D pDE405 = bodysDE405.getPVCoordinates(date, gcrf).getPosition();
            final Vector3D pInpopTDBBig = bodysInpopTDBBig.getPVCoordinates(date, gcrf)
                .getPosition();
            final Vector3D pInpopTCBBig = bodysInpopTCBBig.getPVCoordinates(date, gcrf)
                .getPosition();
            Assert.assertTrue(pDE405.distance(pInpopTDBBig) > 650.0);
            Assert.assertTrue(pDE405.distance(pInpopTDBBig) < 1050.0 * 3);
            Assert.assertTrue(pDE405.distance(pInpopTCBBig) > 1350.0);
            Assert.assertTrue(pDE405.distance(pInpopTCBBig) < 1900.0 * 3);
        }
    }

    /**
     * Test for coverage purposes.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Test which covers the methods mentioned here below.
     * @testedMethod {@link JPLHistoricEphemerisLoader#JPLHistoricEphemerisLoader(String, EphemerisType)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#setCacheSize(int)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#getLoadedConstant(String)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#getMaxChunksDuration()}
     * @testPassCriteria This test covers the methods mentioned here above without any exceptions.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testCoverage() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        final JPLHistoricEphemerisLoader loader = new JPLHistoricEphemerisLoader(
                JPLHistoricEphemerisLoader.DEFAULT_DE_SUPPORTED_NAMES,
                EphemerisType.EARTH);
        loader.setCacheSize(40);

        Assert.assertEquals(Double.NaN, loader.getLoadedConstant("Mock"), 0.0);

        Assert.assertEquals(Double.NaN, loader.getMaxChunksDuration(), 0.0);

        // Chebyshev coefficients:
        final double[] xCoeffs = new double[5];
        final double[] yCoeffs = new double[5];
        final double[] zCoeffs = new double[5];
        final PosVelChebyshev posVel1 = new PosVelChebyshev(new AbsoluteDate(), 10., xCoeffs,
                yCoeffs, zCoeffs);
        Assert.assertEquals(10., posVel1.getValidityDuration(), 0.0);
        final PosVelChebyshev posVel2 = new PosVelChebyshev(new AbsoluteDate().shiftedBy(5.), 10.,
                xCoeffs, yCoeffs, zCoeffs);
        Assert.assertFalse(posVel2.isSuccessorOf(posVel1));

        // Check ICRF of a body (Moon) is aligned with GCRF for all possible transformations
        final Frame icrfMoon = CelestialBodyFactory.getMoon().getICRF();
        final Transform tICrF_GCRF = icrfMoon.getTransformTo(FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH);
        final Transform tICrF_GCRF2 = icrfMoon.getTransformTo(FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH, FramesConfigurationFactory.getIERS2010Configuration(),
                true);
        final Transform tICrF_GCRF3 = icrfMoon.getTransformTo(FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH, true);
        final Transform tICrF_GCRF4 = icrfMoon.getTransformTo(FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH, FramesConfigurationFactory.getIERS2010Configuration());
        Assert.assertTrue(tICrF_GCRF.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertTrue(tICrF_GCRF2.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertTrue(tICrF_GCRF3.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertTrue(tICrF_GCRF4.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));

        // Test GetNativeFrame
        Assert.assertEquals(icrfMoon, CelestialBodyFactory.getMoon().getNativeFrame(null, null));
        
        // Set back former size
        loader.setCacheSize(50);
    }

    /**
     * Method for the validation of the derivatives.
     * 
     * @param supportedNames the supported names
     * @param date the date
     * @throws PatriusException if an error occurs
     */
    private static void checkDerivative(final String supportedNames, final AbsoluteDate date)
            throws PatriusException {
        final JPLHistoricEphemerisLoader loader = new JPLHistoricEphemerisLoader(supportedNames,
                EphemerisType.MERCURY);
        final CelestialBodyEphemeris body = loader.loadCelestialBodyEphemeris(CelestialBodyFactory.MERCURY);
        final double h = 20;

        // eight points finite differences estimation of the velocity
        final Frame eme2000 = FramesFactory.getEME2000();
        final Vector3D pm4h = body.getPVCoordinates(date.shiftedBy(-4 * h), eme2000).getPosition();
        final Vector3D pm3h = body.getPVCoordinates(date.shiftedBy(-3 * h), eme2000).getPosition();
        final Vector3D pm2h = body.getPVCoordinates(date.shiftedBy(-2 * h), eme2000).getPosition();
        final Vector3D pm1h = body.getPVCoordinates(date.shiftedBy(-h), eme2000).getPosition();
        final Vector3D pp1h = body.getPVCoordinates(date.shiftedBy(h), eme2000).getPosition();
        final Vector3D pp2h = body.getPVCoordinates(date.shiftedBy(2 * h), eme2000).getPosition();
        final Vector3D pp3h = body.getPVCoordinates(date.shiftedBy(3 * h), eme2000).getPosition();
        final Vector3D pp4h = body.getPVCoordinates(date.shiftedBy(4 * h), eme2000).getPosition();
        final Vector3D d4 = pp4h.subtract(pm4h);
        final Vector3D d3 = pp3h.subtract(pm3h);
        final Vector3D d2 = pp2h.subtract(pm2h);
        final Vector3D d1 = pp1h.subtract(pm1h);
        final double c = 1.0 / (840 * h);
        final Vector3D estimatedV = new Vector3D(-3 * c, d4, 32 * c, d3, -168 * c, d2, 672 * c, d1);

        final Vector3D loadedV = body.getPVCoordinates(date, eme2000).getVelocity();
        Assert.assertEquals(0, loadedV.subtract(estimatedV).getNorm(), 5.0e-11 * loadedV.getNorm());
    }

    /**
     * Test for the validation of the serialization.
     * 
     * @throws PatriusException if an error occurs
     * 
     * @testType UT
     * @description Evaluate the loader serialization and deserialization processes.
     * @testedMethod {@link JPLHistoricEphemerisLoader#JPLHistoricEphemerisLoader(String, EphemerisType)}
     * @testedMethod {@link JPLHistoricEphemerisLoader#loadCelestialBodyEphemeris(String)}
     * @testPassCriteria The loader can be serialized and deserialized.
     * @referenceVersion 4.11
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSerialization() throws PatriusException {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getGCRF();

        // Loop over each ephemeris types
        for (final EphemerisType type : EphemerisType.values()) {
            final JPLHistoricEphemerisLoader loader = new JPLHistoricEphemerisLoader(
                    JPLHistoricEphemerisLoader.DEFAULT_DE_SUPPORTED_NAMES, type);
            final JPLHistoricEphemerisLoader deserializedLoader = TestUtils.serializeAndRecover(loader);

            Assert.assertEquals(loader.getLoadedAstronomicalUnit(),
                    deserializedLoader.getLoadedAstronomicalUnit(), 0.);
            Assert.assertEquals(loader.getLoadedEarthMoonMassRatio(),
                    deserializedLoader.getLoadedEarthMoonMassRatio(), 0.);
            Assert.assertEquals(loader.getLoadedGravitationalCoefficient(EphemerisType.SUN),
                    deserializedLoader.getLoadedGravitationalCoefficient(EphemerisType.SUN), 0.);
            Assert.assertEquals(loader.getLoadedConstant("GMS", "GM_Sun"),
                    deserializedLoader.getLoadedConstant("GMS", "GM_Sun"), 0.);
            Assert.assertEquals(loader.getMaxChunksDuration(),
                    deserializedLoader.getMaxChunksDuration(), 0.);
            Assert.assertEquals(loader.getLoadedAstronomicalUnit(),
                    deserializedLoader.getLoadedAstronomicalUnit(), 0.);

            final CelestialBodyEphemeris mars1 = loader.loadCelestialBodyEphemeris(CelestialBodyFactory.MARS);
            final CelestialBodyEphemeris mars2 = deserializedLoader
                    .loadCelestialBodyEphemeris(CelestialBodyFactory.MARS);

            for (int i = 0; i < 10; i++) {
                final AbsoluteDate currentDate = date.shiftedBy(i);
                Assert.assertEquals(mars1.getPVCoordinates(currentDate, frame),
                        mars2.getPVCoordinates(currentDate, frame));
            }
        }
    }

    /**
     * Set up.
     */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
