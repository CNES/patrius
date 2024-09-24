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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un ForceModel lorsque le SpacecraftState est en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3168:10/05/2022:[PATRIUS] Ajout de la classe FixedPVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2800:18/05/2021:Compatibilite ephemerides planetaires
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.forces.gravity.AbstractGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class JPLCelestialBodyLoaderTest {

    /**
     * FA-1777.
     * 
     * @testType UT
     * 
     * @testedFeature NONE
     * 
     * @description check that solar system barycenter and Earth-Moon barycenter parent frame is
     *              GCRF and not EME2000
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
        final Frame icrf = CelestialBodyFactory.getSolarSystemBarycenter().getInertialFrame(IAUPoleModelType.CONSTANT);
        final Frame frameSSB = CelestialBodyFactory.getSolarSystemBarycenter().getInertialFrame(
            IAUPoleModelType.CONSTANT);
        final Frame frameEMB = CelestialBodyFactory.getEarthMoonBarycenter()
            .getInertialFrame(IAUPoleModelType.CONSTANT);

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

        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
            JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, ((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30056, ((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    @Test
    public void testConstantsInpop() throws PatriusException {
        Utils.setDataRoot("inpop");
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
            JPLCelestialBodyLoader.DEFAULT_INPOP_SUPPORTED_NAMES, EphemerisType.SUN);
        Assert.assertEquals(149597870691.0, ((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedAstronomicalUnit(), 0.1);
        Assert.assertEquals(81.30057, ((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedEarthMoonMassRatio(), 1.0e-8);
    }

    @Test
    public void testGMJPL() throws PatriusException {
        Utils.setDataRoot("regular-data/de405-ephemerides");

        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
            JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, EphemerisType.SUN);
        Assert.assertEquals(22032.080e9, loader.getLoadedGravitationalCoefficient(EphemerisType.MERCURY), 1.0e6);
        Assert.assertEquals(324858.599e9, loader.getLoadedGravitationalCoefficient(EphemerisType.VENUS), 1.0e6);
        Assert.assertEquals(42828.314e9, loader.getLoadedGravitationalCoefficient(EphemerisType.MARS), 1.0e6);
        Assert.assertEquals(126712767.863e9, loader.getLoadedGravitationalCoefficient(EphemerisType.JUPITER), 6.0e7);
        Assert.assertEquals(37940626.063e9, loader.getLoadedGravitationalCoefficient(EphemerisType.SATURN), 2.0e6);
        Assert.assertEquals(5794549.007e9, loader.getLoadedGravitationalCoefficient(EphemerisType.URANUS), 1.0e6);
        Assert.assertEquals(6836534.064e9, loader.getLoadedGravitationalCoefficient(EphemerisType.NEPTUNE), 1.0e6);
        Assert.assertEquals(981.601e9, loader.getLoadedGravitationalCoefficient(EphemerisType.PLUTO), 1.0e6);
        Assert.assertEquals(132712440017.987e9, loader.getLoadedGravitationalCoefficient(EphemerisType.SUN), 1.0e6);
        Assert.assertEquals(4902.801e9, loader.getLoadedGravitationalCoefficient(EphemerisType.MOON), 1.0e6);
        Assert.assertEquals(403503.233e9, loader.getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON), 1.0e6);
    }

    @Test
    public void testGMInpop() throws PatriusException {

        Utils.setDataRoot("inpop");

        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
            JPLCelestialBodyLoader.DEFAULT_INPOP_SUPPORTED_NAMES, EphemerisType.SUN);
        Assert.assertEquals(22032.081e9, loader.getLoadedGravitationalCoefficient(EphemerisType.MERCURY), 1.0e6);
        Assert.assertEquals(324858.597e9, loader.getLoadedGravitationalCoefficient(EphemerisType.VENUS), 1.0e6);
        Assert.assertEquals(42828.376e9, loader.getLoadedGravitationalCoefficient(EphemerisType.MARS), 1.0e6);
        Assert.assertEquals(126712764.535e9, loader.getLoadedGravitationalCoefficient(EphemerisType.JUPITER), 6.0e7);
        Assert.assertEquals(37940585.443e9, loader.getLoadedGravitationalCoefficient(EphemerisType.SATURN), 2.0e6);
        Assert.assertEquals(5794549.099e9, loader.getLoadedGravitationalCoefficient(EphemerisType.URANUS), 1.0e6);
        Assert.assertEquals(6836527.128e9, loader.getLoadedGravitationalCoefficient(EphemerisType.NEPTUNE), 1.0e6);
        Assert.assertEquals(971.114e9, loader.getLoadedGravitationalCoefficient(EphemerisType.PLUTO), 1.0e6);
        Assert.assertEquals(132712442110.032e9, loader.getLoadedGravitationalCoefficient(EphemerisType.SUN), 1.0e6);
        Assert.assertEquals(4902.800e9, loader.getLoadedGravitationalCoefficient(EphemerisType.MOON), 1.0e6);
        Assert.assertEquals(403503.250e9, loader.getLoadedGravitationalCoefficient(EphemerisType.EARTH_MOON), 1.0e6);
    }

    @Test
    public void testDerivative405() throws PatriusException {
        Utils.setDataRoot("regular-data/de405-ephemerides");
        checkDerivative(JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, new AbsoluteDate(1969, 6,
            25, TimeScalesFactory.getTT()));
    }

    @Test
    public void testDerivative406() throws PatriusException {
        Utils.setDataRoot("regular-data:regular-data/de406-ephemerides");
        checkDerivative(JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, new AbsoluteDate(2964, 9,
            26, TimeScalesFactory.getTT()));
    }

    @Test
    public void testEndianness() throws PatriusException {
        Utils.setDataRoot("inpop");
        final EphemerisType type = EphemerisType.MARS;
        final JPLCelestialBodyLoader loaderInpopTCBBig = new JPLCelestialBodyLoader("^inpop.*_TCB_.*_bigendian\\.dat$",
            type);
        final CelestialBody bodysInpopTCBBig = loaderInpopTCBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, ((JPLHistoricEphemerisLoader) loaderInpopTCBBig.getEphemerisLoader()).getLoadedConstant("TIMESC"), 1.0e-10);
        final JPLCelestialBodyLoader loaderInpopTCBLittle = new JPLCelestialBodyLoader(
            "^inpop.*_TCB_.*_littleendian\\.dat$", type);
        final CelestialBody bodysInpopTCBLittle = loaderInpopTCBLittle.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, ((JPLHistoricEphemerisLoader) loaderInpopTCBLittle.getEphemerisLoader()).getLoadedConstant("TIMESC"), 1.0e-10);
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
    public void testInpopvsJPL() throws PatriusException {
        Utils.setDataRoot("regular-data:inpop");
        final EphemerisType type = EphemerisType.MARS;
        final JPLCelestialBodyLoader loaderDE405 = new JPLCelestialBodyLoader("^unxp(\\d\\d\\d\\d)\\.405$", type);
        final CelestialBody bodysDE405 = loaderDE405.loadCelestialBody(CelestialBodyFactory.MARS);
        final JPLCelestialBodyLoader loaderInpopTDBBig = new JPLCelestialBodyLoader("^inpop.*_TDB_.*_bigendian\\.dat$",
            type);
        final CelestialBody bodysInpopTDBBig = loaderInpopTDBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(0.0, ((JPLHistoricEphemerisLoader) loaderInpopTDBBig.getEphemerisLoader()).getLoadedConstant("TIMESC"), 1.0e-10);
        final JPLCelestialBodyLoader loaderInpopTCBBig = new JPLCelestialBodyLoader("^inpop.*_TCB_.*_bigendian\\.dat$",
            type);
        final CelestialBody bodysInpopTCBBig = loaderInpopTCBBig.loadCelestialBody(CelestialBodyFactory.MARS);
        Assert.assertEquals(1.0, ((JPLHistoricEphemerisLoader) loaderInpopTCBBig.getEphemerisLoader()).getLoadedConstant("TIMESC"), 1.0e-10);
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
    public void testCoverage() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
            JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, EphemerisType.EARTH);
        Assert.assertEquals("Earth", loader.loadCelestialBody("Earth").getName());

        Assert.assertEquals(Double.NaN, ((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedConstant("Mock"), 0.0);

        Assert.assertEquals(Double.NaN, ((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getMaxChunksDuration(), 0.0);

        // Chebyshev coefficients:
        final double[] xCoeffs = new double[5];
        final double[] yCoeffs = new double[5];
        final double[] zCoeffs = new double[5];
        final PosVelChebyshev posVel1 = new PosVelChebyshev(new AbsoluteDate(), 10., xCoeffs, yCoeffs, zCoeffs);
        Assert.assertEquals(10., posVel1.getValidityDuration(), 0.0);
        final PosVelChebyshev posVel2 = new PosVelChebyshev(new AbsoluteDate().shiftedBy(5.), 10., xCoeffs, yCoeffs,
            zCoeffs);
        Assert.assertFalse(posVel2.isSuccessorOf(posVel1));

        // Check ICRF of a body (Moon) is aligned with GCRF for all possible transformations
        final Frame icrfMoon = CelestialBodyFactory.getMoon().getICRF();
        final Transform tICrF_GCRF = icrfMoon.getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH);
        final Transform tICrF_GCRF2 = icrfMoon.getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH,
            FramesConfigurationFactory.getIERS2010Configuration(), true);
        final Transform tICrF_GCRF3 = icrfMoon.getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, true);
        final Transform tICrF_GCRF4 = icrfMoon.getTransformTo(FramesFactory.getGCRF(),
            AbsoluteDate.J2000_EPOCH, FramesConfigurationFactory.getIERS2010Configuration());
        Assert.assertTrue(tICrF_GCRF.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertTrue(tICrF_GCRF2.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertTrue(tICrF_GCRF3.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertTrue(tICrF_GCRF4.getAngular().getRotation().isEqualTo(Rotation.IDENTITY));

        // Test GetNativeFrame
        Assert.assertEquals(icrfMoon, CelestialBodyFactory.getMoon().getNativeFrame(null, null));
    }

    /**
     * Check that Earth frames from Earth body are properly linked to PATRIUS frame tree.
     */
    @Test
    public void testEarth() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        Assert.assertEquals(FramesFactory.getGCRF(), earth.getICRF());
        Assert.assertEquals(FramesFactory.getGCRF(), earth.getInertialFrame(IAUPoleModelType.CONSTANT));
        Assert.assertEquals(FramesFactory.getMOD(true), earth.getInertialFrame(IAUPoleModelType.MEAN));
        Assert.assertEquals(FramesFactory.getTOD(true), earth.getInertialFrame(IAUPoleModelType.TRUE));
        Assert.assertNull(earth.getIAUPole());
        // Nothing to do
        earth.setIAUPole(null);
        try {
            earth.getRotatingFrame(IAUPoleModelType.CONSTANT);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            earth.getRotatingFrame(IAUPoleModelType.MEAN);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        Assert.assertEquals(FramesFactory.getITRF(), earth.getRotatingFrame(IAUPoleModelType.TRUE));
        Assert.assertEquals(earth.getInertialFrame(IAUPoleModelType.CONSTANT), earth.getNativeFrame(null, null));
    }

    /**
     * Enumeration of various bodies with their equatorial radius and
     * polar radius.
     */
    private enum Body {

        SUN(EphemerisType.SUN, 696000000., 696000000.), MERCURY(EphemerisType.MERCURY, 2439700.,
                2439700.), VENUS(EphemerisType.VENUS, 6051800., 6051800.), EARTH(
                EphemerisType.EARTH, 6378136.6, 6356751.9), MOON(EphemerisType.MOON, 1737400.,
                1737400.), MARS(EphemerisType.MARS, 3396190., 3376200.), JUPITER(
                EphemerisType.JUPITER, 71492000., 66854000.), SATURN(EphemerisType.SATURN,
                60268000., 54364000.), URANUS(EphemerisType.URANUS, 25559000., 24973000.), NEPTUNE(
                EphemerisType.NEPTUNE, 24764000., 24341000.), PLUTO(EphemerisType.PLUTO, 1195000.,
                1195000.), EARTH_MOON(EphemerisType.EARTH_MOON, 1, 1), SOLAR_SYSTEM_BARYCENTER(
                EphemerisType.SOLAR_SYSTEM_BARYCENTER, 1, 1);

        private final EphemerisType type;
        private final double ae;
        private final double f;

        /**
         * Simple constructor
         * 
         * @param type
         *        the ephemeris type
         * @param ae
         *        the equatorial radius
         * @param ap
         *        the polar radius
         */
        private Body(final EphemerisType type, final double ae, final double ap) {
            this.type = type;
            this.ae = ae;
            this.f = MathLib.divide(ae - ap, ae);
        }

        public EphemerisType getEphemerisType() {
            return this.type;
        }

        public double getEquatorialRadius() {
            return this.ae;
        }

        public double getFlatnessCoeff() {
            return this.f;
        }
    }

    /**
     * Check the default instance {@link BodyShape} for
     * each celestial bodies.
     * 
     * @throws PatriusException
     */
    @Test
    public void testShapeMethods() throws PatriusException {
        // data to load
        Utils.setDataRoot("regular-dataPBASE");

        // arbitrary date
        final AbsoluteDate date = new AbsoluteDate(2005, 1, 1, 12, 00, 00);

        for (final Body body : Body.values()) {
            final EphemerisType type = body.getEphemerisType();

            // celestial body loader
            final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
                JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, type);
            final CelestialBody celestialBody = loader.loadCelestialBody("body");

            if (type.equals(EphemerisType.EARTH_MOON)
                    || type.equals(EphemerisType.SOLAR_SYSTEM_BARYCENTER)) {
                // the shape is not defined for barycenters
                Assert.assertEquals(null, celestialBody.getShape());
            } else {
                // geometric parameters to build the default shape
                final double radius = body.getEquatorialRadius();
                final double flatness = body.getFlatnessCoeff();

                // test the various shape methods
                final BodyShape defaultShape = celestialBody.getShape();
                final BodyShape expectedShape = new OneAxisEllipsoid(radius, flatness,
                    celestialBody.getRotatingFrame(IAUPoleModelType.TRUE), "body");

                // arbitrary line expressed in the body frame
                final Line line1 = new Line(new Vector3D(10 * radius, 0, 0), new Vector3D(0, 10 * radius, 0));

                double expected;
                double actual;

                // test distance method
                expected = expectedShape.distanceTo(line1, celestialBody.getInertialFrame(IAUPoleModelType.CONSTANT),
                    date);
                actual = defaultShape
                    .distanceTo(line1, celestialBody.getInertialFrame(IAUPoleModelType.CONSTANT), date);
                Assert.assertEquals(expected, actual, 0.);

                // test getApparentRadius method
                final Vector3D posInInertialFrame = new Vector3D(2 * radius, 0, 0);
                final PVCoordinates pv = new PVCoordinates(posInInertialFrame, Vector3D.ZERO);
                final PVCoordinatesProvider provider = new ConstantPVCoordinatesProvider(pv,
                    celestialBody.getInertialFrame(IAUPoleModelType.TRUE));
                expected = expectedShape.getApparentRadius(
                    new ConstantPVCoordinatesProvider(posInInertialFrame.scalarMultiply(2.5), celestialBody
                        .getInertialFrame(IAUPoleModelType.CONSTANT)),
                    date, provider, PropagationDelayType.INSTANTANEOUS);
                actual = defaultShape.getApparentRadius(
                    new ConstantPVCoordinatesProvider(posInInertialFrame.scalarMultiply(2.5), celestialBody
                        .getInertialFrame(IAUPoleModelType.CONSTANT)),
                    date, provider, PropagationDelayType.INSTANTANEOUS);

                // test getIntersections method
                final Line line2 = new Line(new Vector3D(0.8 * radius, 0, 0), new Vector3D(0,
                    0.8 * radius, 0));
                final BodyPoint[] actualInter = defaultShape.getIntersectionPoints(line2,
                    celestialBody.getInertialFrame(IAUPoleModelType.CONSTANT), date);
                final BodyPoint[] expectedInter = expectedShape.getIntersectionPoints(line2,
                    celestialBody.getInertialFrame(IAUPoleModelType.CONSTANT), date);
                Assert.assertEquals(expectedInter.length, actualInter.length);
                for (int i = 0; i < expectedInter.length; i++) {
                    Assert.assertEquals(expectedInter[0].getLLHCoordinates().getLatitude(), actualInter[0]
                        .getLLHCoordinates().getLatitude(), 1e-12);
                    Assert.assertEquals(expectedInter[0].getLLHCoordinates().getLongitude(), actualInter[0]
                        .getLLHCoordinates().getLongitude(), 1e-12);
                    Assert.assertEquals(expectedInter[0].getLLHCoordinates().getHeight(), actualInter[0]
                        .getLLHCoordinates().getHeight(), 1e-12);
                }

                // test the setters with slightly modified parameters
                celestialBody.setShape(new OneAxisEllipsoid(radius * 1.05, flatness * 0.9,
                    celestialBody.getRotatingFrame(IAUPoleModelType.TRUE), "body modified"));
                Assert.assertEquals("body modified", celestialBody.getShape().getName());
                // the distance to the line is lower with the new body shape
                actual = celestialBody.getShape().distanceTo(line1,
                    celestialBody.getInertialFrame(IAUPoleModelType.CONSTANT), date);
                expected = defaultShape.distanceTo(line1,
                    celestialBody.getInertialFrame(IAUPoleModelType.CONSTANT), date);
                Assert.assertTrue(actual < expected);
            }
        }
    }

    /**
     * Check the default instance {@link AbstractGravityModel} for
     * each celestial bodies.
     * 
     * @throws PatriusException
     */
    @Test
    public void testAttractionModel() throws PatriusException {
        // data to load
        Utils.setDataRoot("regular-dataPBASE");

        for (final EphemerisType type : EphemerisType.values()) {

            // celestial body loader
            final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
                JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, type);
            final CelestialBody celestialBody = loader.loadCelestialBody("body");

            // central attraction coefficient
            final double mu = celestialBody.getGM();
            final GravityModel actual = celestialBody.getGravityModel();

            Assert.assertTrue(actual.getClass().equals(NewtonianGravityModel.class));
            Assert.assertEquals(mu, actual.getMu(), 0.);

            // set a new AttractionModel
            final AbstractGravityModel newModel = new NewtonianGravityModel(2 * mu);
            celestialBody.setGravityModel(newModel);
            Assert.assertEquals(2 * mu, celestialBody.getGM(), 0.);

            // set a new central attraction coefficient, the attraction model must me updated
            celestialBody.setGM(1.5 * mu);
            Assert.assertEquals(1.5 * mu, celestialBody.getGravityModel().getMu(), 0.);
        }
    }

    /**
     * Check the constructor with an {@link AbstractGravityModel} in input.
     * 
     * @throws PatriusException
     */
    @Test
    public void testAttractionModelConstructor() throws PatriusException {
        // data to load
        Utils.setDataRoot("regular-dataPBASE");
        final AbstractGravityModel attractionModel = new NewtonianGravityModel(2 * Constants.GRS80_EARTH_MU);

        for (final EphemerisType type : EphemerisType.values()) {

            // celestial body loader
            final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
                JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, type, attractionModel);
            final CelestialBody celestialBody = loader.loadCelestialBody("body");

            // La Terre est une exception car elle utilise toujours un modèle d'attraction de type PointAttraction
            if (!type.equals(EphemerisType.EARTH)) {
                // central attraction coefficient
                final double mu = celestialBody.getGM();
                final GravityModel actual = celestialBody.getGravityModel();

                Assert.assertTrue(actual.getClass().equals(NewtonianGravityModel.class));
                Assert.assertEquals(mu, actual.getMu(), 0.);

                // set a new AttractionModel
                final AbstractGravityModel newModel = new NewtonianGravityModel(2 * mu);
                celestialBody.setGravityModel(newModel);
                Assert.assertEquals(2 * mu, celestialBody.getGM(), 0.);

                // set a new central attraction coefficient, the attraction model must me updated
                celestialBody.setGM(1.5 * mu);
                Assert.assertEquals(1.5 * mu, celestialBody.getGravityModel().getMu(), 0.);
            }
        }
    }

    /**
     * Check the constructor with no {@link AbstractGravityModel} as input.
     * 
     * @throws PatriusException
     */
    @Test
    public void testNoAttractionModelConstructor() throws PatriusException {
        // data to load
        Utils.setDataRoot("regular-dataPBASE");

        for (final EphemerisType ephemerisType : EphemerisType.values()) {
            // celestial body loader
            final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
                JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, ephemerisType);
            final CelestialBody celestialBody = loader.loadCelestialBody("body");

            // La Terre est une exception car elle utilise toujours un modèle d'attraction de type PointAttraction
            if (!ephemerisType.equals(EphemerisType.EARTH)) {
                // central attraction coefficient
                final double mu = celestialBody.getGM();
                final GravityModel actual = celestialBody.getGravityModel();

                Assert.assertTrue(actual.getClass().equals(NewtonianGravityModel.class));
                Assert.assertEquals(mu, actual.getMu(), 0.);

                // set a new AttractionModel
                final AbstractGravityModel newModel = new NewtonianGravityModel(2 * mu);
                celestialBody.setGravityModel(newModel);
                Assert.assertEquals(2 * mu, celestialBody.getGM(), 0.);

                // set a new central attraction coefficient, the attraction model must me updated
                celestialBody.setGM(1.5 * mu);
                Assert.assertEquals(1.5 * mu, celestialBody.getGravityModel().getMu(), 0.);
            }
        }
    }

    private static void checkDerivative(final String supportedNames, final AbsoluteDate date)
        throws PatriusException {
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(supportedNames, EphemerisType.MERCURY);
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

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the loader serialization / deserialization process.
     *
     * @testPassCriteria The loader can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final AbstractGravityModel attractionModel = new NewtonianGravityModel(
            2 * Constants.GRS80_EARTH_MU);

        // Loop over each ephemeris types
        for (final EphemerisType type : EphemerisType.values()) {
            final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(
                JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES, type, attractionModel);
            final JPLCelestialBodyLoader deserializedLoader = TestUtils.serializeAndRecover(loader);

            Assert.assertEquals(((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedAstronomicalUnit(),
                    ((JPLHistoricEphemerisLoader) deserializedLoader.getEphemerisLoader()).getLoadedAstronomicalUnit(), 0.);
            Assert.assertEquals(((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedEarthMoonMassRatio(),
                    ((JPLHistoricEphemerisLoader) deserializedLoader.getEphemerisLoader()).getLoadedEarthMoonMassRatio(), 0.);
            Assert.assertEquals(loader.getLoadedGravitationalCoefficient(EphemerisType.SUN),
                    deserializedLoader.getLoadedGravitationalCoefficient(EphemerisType.SUN), 0.);
            Assert.assertEquals(((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedConstant("GMS", "GM_Sun"),
                    ((JPLHistoricEphemerisLoader) deserializedLoader.getEphemerisLoader()).getLoadedConstant("GMS", "GM_Sun"), 0.);
            Assert.assertEquals(((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getMaxChunksDuration(),
                    ((JPLHistoricEphemerisLoader) deserializedLoader.getEphemerisLoader()).getMaxChunksDuration(), 0.);
            Assert.assertEquals(((JPLHistoricEphemerisLoader) loader.getEphemerisLoader()).getLoadedAstronomicalUnit(),
                    ((JPLHistoricEphemerisLoader) deserializedLoader.getEphemerisLoader()).getLoadedAstronomicalUnit(), 0.);

            final CelestialBody mars1 = loader.loadCelestialBody(CelestialBodyFactory.MARS);
            final CelestialBody mars2 = deserializedLoader
                .loadCelestialBody(CelestialBodyFactory.MARS);

            for (int i = 0; i < 10; i++) {
                final AbsoluteDate currentDate = date.shiftedBy(i);
                Assert.assertEquals(mars1.getPVCoordinates(currentDate, frame),
                    mars2.getPVCoordinates(currentDate, frame));
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
