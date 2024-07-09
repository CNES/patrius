/**
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
 * HISTORY
 * VERSION:4.7:DM:DM-2800:18/05/2021:Compatibilite ephemerides planetaires
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class JPLEphemeridesLoaderTest {

    /**
     * Test reading of all available JPL file formats.
     * Only readable files are tested: DE200, 202, 403, 405, 410, 413, 414, 418, 421, 422, 423 and 430.
     * Position over 10 years ephemeris is checked vs standard ephemeris DE405.
     * Threshold are arbitrary, but small enough to ensure read data is consistent.
     */
    @Test
    public void testJPLEphemerisFormat() throws PatriusException {
        Utils.setDataRoot("jplEphemeris");
        
        // Load data
        // Accepted file formats are DE200, 202, 403, 405, 410, 413, 414, 418, 421, 422, 423 and 430
        final Pair<Double, List<PVCoordinates>> de200 = loadBody("lnxm1600p2170.200");
        final Pair<Double, List<PVCoordinates>> de202 = loadBody("lnxp1900p2050.202");
        final Pair<Double, List<PVCoordinates>> de403 = loadBody("lnxp2000.403");
        final Pair<Double, List<PVCoordinates>> de405 = loadBody("lnx1900.405");
        final Pair<Double, List<PVCoordinates>> de410 = loadBody("lnxp1960p2020.410");
        final Pair<Double, List<PVCoordinates>> de413 = loadBody("lnxp1900p2050.413");
        final Pair<Double, List<PVCoordinates>> de414 = loadBody("lnxp1600p2200.414");
        final Pair<Double, List<PVCoordinates>> de418 = loadBody("lnxp1900p2050.418");
        final Pair<Double, List<PVCoordinates>> de421 = loadBody("lnxp1900p2053.421");
        // 422 not tested in PATRIUS tests because file is too large (> 500Mo)
        final Pair<Double, List<PVCoordinates>> de423 = loadBody("lnxp1900.423");
        // 430 not tested in PATRIUS tests because file is too large (> 100Mo)

        // Compare data vs DE405 ephemeris (position threshold: 200m over 10 years ephemeris)
        for (int i = 0; i < de405.getValue().size(); i++) {
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de200.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de202.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de403.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de410.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de413.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de414.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de418.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de421.getValue().get(i).getPosition()), 200.);
            Assert.assertEquals(0., de405.getValue().get(i).getPosition().distance(de423.getValue().get(i).getPosition()), 200.);
        }
        // Check GM (relative threshold: 1E-6)
        Assert.assertEquals(0., (de405.getKey() - de200.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de202.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de403.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de410.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de413.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de414.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de418.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de421.getKey()) / de405.getKey(), 1E-6);
        Assert.assertEquals(0., (de405.getKey() - de423.getKey()) / de405.getKey(), 1E-6);
    }

    /**
     * Test reading of all available JPL file formats.
     * Only readable files are tested: INPOP 06b/06c/08a/10a/10b/10e/13c/17a/19a.
     * Position over 10 years ephemeris is checked vs standard ephemeris DE405
     * Threshold are arbitrary, but small enough to ensure read data is consistent.
     */
    @Test
    public void testIMCCEEphemerisFormat() throws PatriusException {
        Utils.setDataRoot("imcceEphemeris");
        
        // Load data
        // Accepted file formats are INPOP 06b/06c/08a/10a/10b/10e/13c/17a/19a
        final Pair<Double, List<PVCoordinates>> inpop06b = loadBody("inpop06b_2_m100_p100_littleendian.dat");
        final Pair<Double, List<PVCoordinates>> inpop06c = loadBody("inpop06c_m100_p100_littleendian.dat");
        final Pair<Double, List<PVCoordinates>> inpop08a = loadBody("inpop08a_m100_p100_tt.dat");
        final Pair<Double, List<PVCoordinates>> inpop10a = loadBody("inpop10a_m100_p100_tt.dat");
        final Pair<Double, List<PVCoordinates>> inpop10b = loadBody("inpop10b_TDB_m100_p100_tt.dat");
        final Pair<Double, List<PVCoordinates>> inpop10e = loadBody("inpop10e_TDB_m100_p100_tt.dat");
        final Pair<Double, List<PVCoordinates>> inpop13c = loadBody("inpop13c_TDB_m100_p100_tt.dat");
        final Pair<Double, List<PVCoordinates>> inpop17a = loadBody("inpop17a_TDB_m100_p100_tt.dat");
        final Pair<Double, List<PVCoordinates>> inpop19a = loadBody("inpop19a_TDB_m100_p100_tt.dat");
        
        // Compare data vs Inpop 19a ephemeris (position threshold: 20m over 10 years ephemeris)
        for (int i = 0; i < inpop19a.getValue().size(); i++) {
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop06b.getValue().get(i).getPosition()), 20.);
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop06c.getValue().get(i).getPosition()), 20.);
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop08a.getValue().get(i).getPosition()), 20.);
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop10a.getValue().get(i).getPosition()), 20.);
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop10b.getValue().get(i).getPosition()), 20.);
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop10e.getValue().get(i).getPosition()), 20.);
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop13c.getValue().get(i).getPosition()), 20.);
            Assert.assertEquals(0., inpop19a.getValue().get(i).getPosition().distance(inpop17a.getValue().get(i).getPosition()), 20.);
        }
        // Check GM (relative threshold: 1E-7)
        Assert.assertEquals(0., (inpop19a.getKey() - inpop06b.getKey()) / inpop19a.getKey(), 1E-7);
        Assert.assertEquals(0., (inpop19a.getKey() - inpop06c.getKey()) / inpop19a.getKey(), 1E-7);
        Assert.assertEquals(0., (inpop19a.getKey() - inpop08a.getKey()) / inpop19a.getKey(), 1E-7);
        Assert.assertEquals(0., (inpop19a.getKey() - inpop10a.getKey()) / inpop19a.getKey(), 1E-7);
        Assert.assertEquals(0., (inpop19a.getKey() - inpop10b.getKey()) / inpop19a.getKey(), 1E-7);
        Assert.assertEquals(0., (inpop19a.getKey() - inpop10e.getKey()) / inpop19a.getKey(), 1E-7);
        Assert.assertEquals(0., (inpop19a.getKey() - inpop13c.getKey()) / inpop19a.getKey(), 1E-7);
        Assert.assertEquals(0., (inpop19a.getKey() - inpop17a.getKey()) / inpop19a.getKey(), 1E-7);
    }

    /**
     * Load Moon GM and 10 years ephemeris from a JPL file.
     * @param file a JPL file
     * @return Moon GM and 10 years ephemeris
     */
    private Pair<Double, List<PVCoordinates>> loadBody(final String file) throws PatriusException {
        CelestialBodyFactory.clearCelestialBodyLoaders();
        CelestialBodyFactory.addDefaultCelestialBodyLoader("Moon", file);
        final CelestialBody body = CelestialBodyFactory.getMoon();
        final Double gm = body.getGM();
        final List<PVCoordinates> list = new ArrayList<PVCoordinates>();
        for (int i = 0; i < 10 * 365; i++) {
            list.add(body.getPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(i * Constants.JULIAN_DAY), FramesFactory.getGCRF()));
        }
        return new Pair<Double, List<PVCoordinates>>(gm, list);
    }

    /**
     * FA-1777.
     * 
     * @testType UT
     * 
     * @testedFeature NONE
     * 
     * @description check that solar system barycenter and Earth-Moon barycenter parent frame is GCRF and not EME2000
     * 
     * @input None
     * 
     * @output transformation between
     * 
     * @testPassCriteria solar system barycenter and Earth-Moon barycenter parent frame is GCRF
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testFA1777() throws PatriusException {
        Utils.setDataRoot("regular-data");

        // Retrieve frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame icrf = CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame();
        final Frame frameSSB = CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame();
        final Frame frameEMB = CelestialBodyFactory.getEarthMoonBarycenter().getInertiallyOrientedFrame();

        // Compute transforms
        final Transform tGCRF_ICRF = gcrf.getTransformTo(icrf, AbsoluteDate.J2000_EPOCH);
        final Transform tGCRF_SSB = gcrf.getTransformTo(frameSSB, AbsoluteDate.J2000_EPOCH);
        final Transform tGCRF_EMB = gcrf.getTransformTo(frameEMB, AbsoluteDate.J2000_EPOCH);

        // Check Earth - Moon barycenter inertial frame is aligned with GCRF (no pole correction)
        Assert.assertTrue(tGCRF_EMB.getRotation().isEqualTo(Transform.IDENTITY.getRotation()));
        // Check Solar system barycenter frame is aligned with GCRF (no pole correction)
        Assert.assertTrue(tGCRF_SSB.getRotation().isEqualTo(Transform.IDENTITY.getRotation()));
        // Check ICRF frame is aligned with GCRF (no pole correction)
        Assert.assertTrue(tGCRF_ICRF.getRotation().isEqualTo(Transform.IDENTITY.getRotation()));
    }

    @Test
    public void testConstantsJPL() throws PatriusException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        final JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30056, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    @Test
    public void testConstantsInpop() throws PatriusException {
        Utils.setDataRoot("inpop");
        final JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_INPOP_SUPPORTED_NAMES,
                JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, loader.getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30057, loader.getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    @Test
    public void testGMJPL() throws PatriusException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        final JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(22032.080e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY),
            1.0e6);
        Assert.assertEquals(324858.599e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS),
            1.0e6);
        Assert.assertEquals(42828.314e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS),
            1.0e6);
        Assert.assertEquals(126712767.863e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER),
            6.0e7);
        Assert.assertEquals(37940626.063e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN),
            2.0e6);
        Assert.assertEquals(5794549.007e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS),
            1.0e6);
        Assert.assertEquals(6836534.064e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE),
            1.0e6);
        Assert.assertEquals(981.601e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO),
            1.0e6);
        Assert.assertEquals(132712440017.987e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN),
            1.0e6);
        Assert.assertEquals(4902.801e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON),
            1.0e6);
        Assert.assertEquals(403503.233e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON),
            1.0e6);
    }

    @Test
    public void testGMInpop() throws PatriusException {

        Utils.setDataRoot("inpop");

        final JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_INPOP_SUPPORTED_NAMES,
                JPLEphemeridesLoader.EphemerisType.SUN);
        Assert.assertEquals(22032.081e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MERCURY),
            1.0e6);
        Assert.assertEquals(324858.597e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.VENUS),
            1.0e6);
        Assert.assertEquals(42828.376e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MARS),
            1.0e6);
        Assert.assertEquals(126712764.535e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.JUPITER),
            6.0e7);
        Assert.assertEquals(37940585.443e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SATURN),
            2.0e6);
        Assert.assertEquals(5794549.099e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.URANUS),
            1.0e6);
        Assert.assertEquals(6836527.128e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.NEPTUNE),
            1.0e6);
        Assert.assertEquals(971.114e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.PLUTO),
            1.0e6);
        Assert.assertEquals(132712442110.032e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.SUN),
            1.0e6);
        Assert.assertEquals(4902.800e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.MOON),
            1.0e6);
        Assert.assertEquals(403503.250e9,
            loader.getLoadedGravitationalCoefficient(JPLEphemeridesLoader.EphemerisType.EARTH_MOON),
            1.0e6);
    }

    @Test
    public void testDerivative405() throws PatriusException, ParseException {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        this.checkDerivative(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
            new AbsoluteDate(1969, 6, 25, TimeScalesFactory.getTT()));
    }

    @Test
    public void testDerivative406() throws PatriusException, ParseException {
        Utils.setDataRoot("regular-data:regular-data/de406-ephemerides");
        this.checkDerivative(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
            new AbsoluteDate(2964, 9, 26, TimeScalesFactory.getTT()));
    }

    @Test
    @Ignore
    public void testDerivative414() throws PatriusException, ParseException {
        Utils.setDataRoot("regular-data/de414-ephemerides");
        this.checkDerivative(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
            new AbsoluteDate(1950, 1, 12, TimeScalesFactory.getTT()));
    }

    @Test
    public void testEndianness() throws PatriusException, ParseException {
        Utils.setDataRoot("inpop");
        final JPLEphemeridesLoader.EphemerisType type = JPLEphemeridesLoader.EphemerisType.MARS;
        final JPLEphemeridesLoader loaderInpopTCBBig =
            new JPLEphemeridesLoader("^inpop.*_TCB_.*_bigendian\\.dat$", type);
        final CelestialBody bodysInpopTCBBig = loaderInpopTCBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        final JPLEphemeridesLoader loaderInpopTCBLittle =
            new JPLEphemeridesLoader("^inpop.*_TCB_.*_littleendian\\.dat$", type);
        final CelestialBody bodysInpopTCBLittle = loaderInpopTCBLittle.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBLittle.getLoadedConstant("TIMESC"), 1.0e-10);
        final AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4, TimeScalesFactory.getTT());
        final Frame eme2000 = FramesFactory.getEME2000();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            final AbsoluteDate date = t0.shiftedBy(dt);
            final Vector3D pInpopTCBBig = bodysInpopTCBBig.getPVCoordinates(date, eme2000).getPosition();
            final Vector3D pInpopTCBLittle = bodysInpopTCBLittle.getPVCoordinates(date, eme2000).getPosition();
            Assert.assertEquals(0.0, pInpopTCBBig.distance(pInpopTCBLittle), 1.0e-10);
        }
        for (final String name : DataProvidersManager.getInstance().getLoadedDataNames()) {
            Assert.assertTrue(name.contains("inpop"));
        }
    }

    @Test
    public void testInpopvsJPL() throws PatriusException, ParseException {
        Utils.setDataRoot("regular-data:inpop");
        final JPLEphemeridesLoader.EphemerisType type = JPLEphemeridesLoader.EphemerisType.MARS;
        final JPLEphemeridesLoader loaderDE405 =
            new JPLEphemeridesLoader("^unxp(\\d\\d\\d\\d)\\.405$", type);
        final CelestialBody bodysDE405 = loaderDE405.loadCelestialBody(CelestialBodyFactory.MARS);
        final JPLEphemeridesLoader loaderInpopTDBBig =
            new JPLEphemeridesLoader("^inpop.*_TDB_.*_bigendian\\.dat$", type);
        final CelestialBody bodysInpopTDBBig = loaderInpopTDBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(0.0, loaderInpopTDBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        final JPLEphemeridesLoader loaderInpopTCBBig =
            new JPLEphemeridesLoader("^inpop.*_TCB_.*_bigendian\\.dat$", type);
        final CelestialBody bodysInpopTCBBig = loaderInpopTCBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, loaderInpopTCBBig.getLoadedConstant("TIMESC"), 1.0e-10);
        final AbsoluteDate t0 = new AbsoluteDate(1969, 7, 17, 10, 43, 23.4, TimeScalesFactory.getTT());
        final Frame gcrf = FramesFactory.getGCRF();
        for (double dt = 0; dt < 30 * Constants.JULIAN_DAY; dt += 3600) {
            final AbsoluteDate date = t0.shiftedBy(dt);
            final Vector3D pDE405 = bodysDE405.getPVCoordinates(date, gcrf).getPosition();
            final Vector3D pInpopTDBBig = bodysInpopTDBBig.getPVCoordinates(date, gcrf).getPosition();
            final Vector3D pInpopTCBBig = bodysInpopTCBBig.getPVCoordinates(date, gcrf).getPosition();
            Assert.assertTrue(pDE405.distance(pInpopTDBBig) > 650.0);
            Assert.assertTrue(pDE405.distance(pInpopTDBBig) < 1050.0 * 3);
            Assert.assertTrue(pDE405.distance(pInpopTCBBig) > 1350.0);
            Assert.assertTrue(pDE405.distance(pInpopTCBBig) < 1900.0 * 3);
        }

    }

    // Tests for coverage purposes.
    @Test
    public void testCoverage() throws PatriusException, ParseException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        final JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(JPLEphemeridesLoader.DEFAULT_DE_SUPPORTED_NAMES,
                JPLEphemeridesLoader.EphemerisType.EARTH);
        Assert.assertEquals(FramesFactory.getGCRF(), loader.loadCelestialBody("Earth").getInertiallyOrientedFrame());
        Assert.assertEquals("Earth", loader.loadCelestialBody("Earth").getName());

        Assert.assertEquals(Double.NaN, loader.getLoadedConstant("Mock"), 0.0);

        Assert.assertEquals(Double.NaN, loader.getMaxChunksDuration(), 0.0);

        // Chebyshev coefficients:
        final double[] xCoeffs = new double[5];
        final double[] yCoeffs = new double[5];
        final double[] zCoeffs = new double[5];
        final PosVelChebyshev posVel1 = new PosVelChebyshev(new AbsoluteDate(), 10., xCoeffs, yCoeffs, zCoeffs);
        Assert.assertEquals(10., posVel1.getValidityDuration(), 0.0);
        final PosVelChebyshev posVel2 = new PosVelChebyshev(new AbsoluteDate().shiftedBy(5.), 10., xCoeffs, yCoeffs,
            zCoeffs);
        Assert.assertFalse(posVel2.isSuccessorOf(posVel1));

    }

    private void checkDerivative(final String supportedNames, final AbsoluteDate date)
                                                                                      throws PatriusException,
                                                                                      ParseException {
        final JPLEphemeridesLoader loader =
            new JPLEphemeridesLoader(supportedNames, JPLEphemeridesLoader.EphemerisType.MERCURY);
        final CelestialBody body = loader.loadCelestialBody(CelestialBodyFactory.MERCURY);
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

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
