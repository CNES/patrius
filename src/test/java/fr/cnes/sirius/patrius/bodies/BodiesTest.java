/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * limitations under the License.
 * HISTORY
* VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
* VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
* VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
* VERSION:4.11.1:DM:DM-75:30/06/2023:[PATRIUS] Degradation performance Patrius 4.11
* VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
* VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
* VERSION:4.11:FA:FA-3257:22/05/2023:[PATRIUS] Suite 3182
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:276:08/09/2014:Moon-Earth barycenter problem
 * VERSION::FA:274:24/10/2014:third body ephemeris clearing modified
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialPoint.BodyNature;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.EME2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.EclipticJ2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BodiesTest {

    @Test
    public void defaultBodies() {
        Utils.setDataRoot("regular-data");
        CelestialBodyFactory.clearCelestialBodyLoaders("SOLAR_SYSTEM_BARYCENTER");
        try {
            CelestialBodyFactory.clearCelestialBodyLoaders();
            CelestialBodyFactory.addDefaultCelestialBodyLoader(JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES);
            // expected
            CelestialBodyFactory.getEarth();
        } catch (final PatriusException ex) {
            Assert.fail("OrekitException");
        }
    }

    @Test
    public void IAUPole() throws PatriusException {
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());

        CelestialBodyFactory.addDefaultCelestialBodyLoader(JPLCelestialBodyLoader.DEFAULT_DE_SUPPORTED_NAMES);
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.SUN).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.SUN).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.MERCURY).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.MERCURY).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.VENUS).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.VENUS).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.EARTH).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.EARTH).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.MOON).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.MOON).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.MARS).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.MARS).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.JUPITER).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.JUPITER).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.SATURN).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.SATURN).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.URANUS).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.URANUS).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.NEPTUNE).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.NEPTUNE).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.PLUTO).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.PLUTO).getPrimeMeridianAngle(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.EARTH_MOON).getPole(date));
        Assert.assertNotNull(IAUPoleFactory.getIAUPole(EphemerisType.EARTH_MOON).getPrimeMeridianAngle(date));
        
        // Check IAU pole exist
        final CelestialBodyOrientation pole1 = CelestialBodyFactory.getSun().getOrientation();
        Assert.assertNotNull(pole1);
        final CelestialBodyOrientation pole2 = CelestialBodyFactory.getMoon().getOrientation();
        CelestialBodyFactory.getSun().setOrientation(pole2);
        Assert.assertNotSame(pole1, CelestialBodyFactory.getSun().getOrientation());
    }

    @Test
    public void geocentricPV() throws PatriusException {
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());
        final Frame geocentricFrame = FramesFactory.getGCRF();
        checkPV(CelestialBodyFactory.getMoon(), date, geocentricFrame,
            new Vector3D(-0.0022350411591597575, -0.0010106334699928434, -5.658291803646671E-4),
            new Vector3D(3.1279236468844985E-4, -4.526815459166321E-4, -2.428841016970333E-4));
        checkPV(CelestialBodyFactory.getEarth(), date, geocentricFrame, Vector3D.ZERO, Vector3D.ZERO);
    }

    @Test
    public void heliocentricPV() throws PatriusException {
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());
        final Frame eme2000 = FramesFactory.getGCRF();
        final Frame heliocentricFrame = new Frame(eme2000, new TransformProvider(){

            /** Serializable UID. */
            private static final long serialVersionUID = 1819904714245631265L;

            @Override
            public Transform getTransform(final AbsoluteDate date)
                                                                  throws PatriusException {
                return new Transform(date, CelestialBodyFactory.getSun().getPVCoordinates(date, eme2000));
            }

            @Override
            public Transform
                    getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
                return getTransform(date);
            }

            @Override
            public Transform
                    getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives) throws PatriusException {
                return getTransform(date);
            }

            @Override
            public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                          final boolean computeSpinDerivatives)
                                                                               throws PatriusException {
                return getTransform(date);
            }
        }, "heliocentric/aligned EME2000", true);
        checkPV(CelestialBodyFactory.getSun(), date, heliocentricFrame, Vector3D.ZERO, Vector3D.ZERO);
        checkPV(CelestialBodyFactory.getMercury(), date, heliocentricFrame,
            new Vector3D(0.3388866970713254, -0.16350851403469605, -0.12250815624343761),
            new Vector3D(0.008716751907934464, 0.02294287010530833, 0.011349219084264612));
        checkPV(CelestialBodyFactory.getVenus(), date, heliocentricFrame,
            new Vector3D(0.5733328682513444, -0.3947124128748959, -0.21383496742544283),
            new Vector3D(0.012311818929592546, 0.014756722625966128, 0.005857890214695866));
        checkPV(CelestialBodyFactory.getMars(), date, heliocentricFrame,
            new Vector3D(-0.15808000178306866, -1.3285167111540124, -0.6050478023304016),
            new Vector3D(0.014443621048367267, -1.3669889027283553E-4, -4.542404441793112E-4));
        checkPV(CelestialBodyFactory.getJupiter(), date, heliocentricFrame,
            new Vector3D(-5.387442227958154, -0.8116709870422928, -0.21662388956102652),
            new Vector3D(0.0010628473875341506, -0.006527800816267844, -0.0028242250304474767));
        checkPV(CelestialBodyFactory.getSaturn(), date, heliocentricFrame,
            new Vector3D(7.89952834654684, 4.582711147265509, 1.552649660593234),
            new Vector3D(-0.003208403682518813, 0.004335751536569781, 0.001928152129122073));
        checkPV(CelestialBodyFactory.getUranus(), date, heliocentricFrame,
            new Vector3D(-18.2705614311796, -1.151408356279009, -0.24540975062356502),
            new Vector3D(2.1887052624725852E-4, -0.0037678288699642877, -0.0016532828516810242));
        checkPV(CelestialBodyFactory.getNeptune(), date, heliocentricFrame,
            new Vector3D(-16.06747366050193, -23.938436657940095, -9.39837851302005),
            new Vector3D(0.0026425894813251684, -0.0015042632480101307, -6.815738977894145E-4));
        checkPV(CelestialBodyFactory.getPluto(), date, heliocentricFrame,
            new Vector3D(-30.488788499360652, -0.8637991387172488, 8.914537151982762),
            new Vector3D(3.21695873843002E-4, -0.0031487797507673814, -0.0010799339515148705));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * 
     * @testedMethod {@link AbstractCelestialBody#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description Test that the PV of a body expressed in its own body fixed frame is zero.
     * 
     * @input a random CelestialBody
     * 
     * @output norm of the distance between the position vectors and the velocity vectors
     * 
     * @testPassCriteria Both norms are supposed to be strictly equal to zero
     * 
     * @referenceVersion 4.11
     */
    @Test
    public void testMoonSelfTransformedPV() throws PatriusException {
        Utils.setDataRoot("regular-data");
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        final PVCoordinates pvMoonFromMoon = moon.getPVCoordinates(AbsoluteDate.J2000_EPOCH, moon.getICRF());

        final PVCoordinates reference =
            new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);

        final PVCoordinates error = new PVCoordinates(reference, pvMoonFromMoon);
        Assert.assertEquals(0, error.getPosition().getNorm(), 0.);
        Assert.assertEquals(0, error.getVelocity().getNorm(), 0.);

        Assert.assertEquals(BodyNature.PHYSICAL_BODY, moon.getBodyNature());
    }

    @Test(expected = PatriusException.class)
    public void noMercury() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMercury();
    }

    @Test(expected = PatriusException.class)
    public void noVenus() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getVenus();
    }

    @Test(expected = PatriusException.class)
    public void noEarthMoonBarycenter() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getEarthMoonBarycenter();
    }

    @Test(expected = PatriusException.class)
    public void noMars() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMars();
    }

    @Test(expected = PatriusException.class)
    public void noJupiter() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getJupiter();
    }

    @Test(expected = PatriusException.class)
    public void noSaturn() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getSaturn();
    }

    @Test(expected = PatriusException.class)
    public void noUranus() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getUranus();
    }

    @Test(expected = PatriusException.class)
    public void noNeptune() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getNeptune();
    }

    @Test(expected = PatriusException.class)
    public void noPluto() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getPluto();
    }

    @Test(expected = PatriusException.class)
    public void noMoon() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getMoon();
    }

    @Test(expected = PatriusException.class)
    public void noSun() throws PatriusException {
        Utils.setDataRoot("no-data");
        CelestialBodyFactory.getSun();
    }

    private static void checkPV(final PVCoordinatesProvider body, final AbsoluteDate date, final Frame frame,
                         final Vector3D position, final Vector3D velocity)
                                                                          throws PatriusException {

        final PVCoordinates pv = body.getPVCoordinates(date, frame);

        final double posScale = 149597870691.0;
        final double velScale = posScale / Constants.JULIAN_DAY;
        final PVCoordinates reference =
            new PVCoordinates(new Vector3D(posScale, position), new Vector3D(velScale, velocity));

        final PVCoordinates error = new PVCoordinates(reference, pv);
        Assert.assertEquals(0, error.getPosition().getNorm(), 2.0e-3);
        Assert.assertEquals(0, error.getVelocity().getNorm(), 5.0e-10);

    }

    @Test
    public void testFrameShift() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final Frame moon = CelestialBodyFactory.getMoon().getRotatingFrame(IAUPoleModelType.TRUE);
        final Frame earth = CelestialBodyFactory.getEarth().getRotatingFrame(IAUPoleModelType.TRUE);
        final AbsoluteDate date0 = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());

        for (double t = 0; t < Constants.JULIAN_DAY; t += 3600) {
            final AbsoluteDate date = date0.shiftedBy(t);
            final Transform transform = earth.getTransformTo(moon, date);
            for (double dt = -10; dt < 10; dt += 0.125) {
                final Transform shifted = transform.shiftedBy(dt);
                final Transform computed = earth.getTransformTo(moon, transform.getDate().shiftedBy(dt));
                final Transform error = new Transform(computed.getDate(), computed, shifted.getInverse(), true);
                Assert.assertEquals(0.0, error.getTranslation().getNorm(), 100.0);
                Assert.assertEquals(0.0, error.getVelocity().getNorm(), 20.0);
                Assert.assertEquals(0.0, error.getAcceleration().getNorm(), 1e-2);
                Assert.assertEquals(0.0, error.getRotation().getAngle(), 4.0e-8);
                Assert.assertEquals(0.0, error.getRotationRate().getNorm(), 8.0e-10);
                Assert.assertNull(error.getRotationAcceleration());
            }
        }
    }

    @Test
    public void testKepler() throws PatriusException {
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TimeScalesFactory.getTT());
        final double au = 149597870691.0;
        checkKepler(CelestialBodyFactory.getMoon(), CelestialBodyFactory.getEarth(), date, 3.844e8, 0.012);
        checkKepler(CelestialBodyFactory.getMercury(), CelestialBodyFactory.getSun(), date, 0.387 * au, 4.0e-9);
        checkKepler(CelestialBodyFactory.getVenus(), CelestialBodyFactory.getSun(), date, 0.723 * au, 8.0e-9);
        checkKepler(CelestialBodyFactory.getEarth(), CelestialBodyFactory.getSun(), date, 1.000 * au, 2.0e-5);
        checkKepler(CelestialBodyFactory.getMars(), CelestialBodyFactory.getSun(), date, 1.52 * au, 2.0e-7);
        checkKepler(CelestialBodyFactory.getJupiter(), CelestialBodyFactory.getSun(), date, 5.20 * au, 2.0e-6);
        checkKepler(CelestialBodyFactory.getSaturn(), CelestialBodyFactory.getSun(), date, 9.58 * au, 8.0e-7);
        checkKepler(CelestialBodyFactory.getUranus(), CelestialBodyFactory.getSun(), date, 19.20 * au, 6.0e-7);
        checkKepler(CelestialBodyFactory.getNeptune(), CelestialBodyFactory.getSun(), date, 30.05 * au, 4.0e-7);
        checkKepler(CelestialBodyFactory.getPluto(), CelestialBodyFactory.getSun(), date, 39.24 * au, 3.0e-7);
    }

    private static void checkKepler(final PVCoordinatesProvider orbiting, final CelestialBody central,
                             final AbsoluteDate start, final double a, final double epsilon)
                                                                                            throws PatriusException {

        // set up Keplerian orbit of orbiting body around central body
        final Orbit orbit = new KeplerianOrbit(orbiting.getPVCoordinates(start,
                central.getInertialFrame(IAUPoleModelType.CONSTANT)),
            central.getInertialFrame(IAUPoleModelType.CONSTANT), start, central.getGM());
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        Assert.assertEquals(a, orbit.getA(), 0.02 * a);
        final double duration = MathLib.min(50 * Constants.JULIAN_DAY, 0.01 * orbit.getKeplerianPeriod());

        double max = 0;
        for (AbsoluteDate date = start; date.durationFrom(start) < duration; date = date.shiftedBy(duration / 100)) {
            final PVCoordinates ephemPV = orbiting.getPVCoordinates(date, central.getInertialFrame(IAUPoleModelType.CONSTANT));
            final PVCoordinates keplerPV = propagator.propagate(date).getPVCoordinates();
            final Vector3D error = keplerPV.getPosition().subtract(ephemPV.getPosition());
            max = MathLib.max(max, error.getNorm());
        }
        Assert.assertTrue(max < epsilon * a);
    }

    public void testEarthMoonBarycenter() throws PatriusException {

        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody mars = CelestialBodyFactory.getMars();
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final CelestialPoint earthMoonBarycenter = CelestialBodyFactory.getEarthMoonBarycenter();
        final List<Frame> frames = Arrays.asList(FramesFactory.getEME2000(),
            FramesFactory.getGCRF(),
                sun.getInertialFrame(IAUPoleModelType.CONSTANT),
            mars.getInertialFrame(IAUPoleModelType.CONSTANT),
            earth.getInertialFrame(IAUPoleModelType.CONSTANT));

        final AbsoluteDate date = new AbsoluteDate(1969, 7, 23, TimeScalesFactory.getTT());
        final double refDistance = bodyDistance(sun, earthMoonBarycenter, date, frames.get(0));
        for (final Frame frame : frames) {
            Assert.assertEquals(frame.toString(), refDistance,
                bodyDistance(sun, earthMoonBarycenter, date, frame),
                1.0e-14 * refDistance);
        }
    }

    private static double bodyDistance(final CelestialPoint body1, final CelestialPoint body2, final AbsoluteDate date,
                                final Frame frame)
                                                  throws PatriusException {
        final Vector3D body1Position = body1.getPVCoordinates(date, frame).getPosition();
        final Vector3D body2Position = body2.getPVCoordinates(date, frame).getPosition();
        final Vector3D bodyPositionDifference = body1Position.subtract(body2Position);

        return bodyPositionDifference.getNorm();
    }

    @Test
    // FT 274 : third body data clearing problem
            public
            void ThirdBodyDataClearTest() throws PatriusException {

        // first third body data
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = new AbsoluteDate(1969, 7, 24, TimeScalesFactory.getTT());
        CelestialBodyFactory.clearCelestialBodyLoaders();
        CelestialBodyFactory.addDefaultCelestialBodyLoader("unxp0000.405");
        final CelestialPoint ssb = CelestialBodyFactory.getSolarSystemBarycenter();
        final PVCoordinates pvssb = ssb.getPVCoordinates(date, FramesFactory.getEME2000());

        // second third body data
        final AbsoluteDate date2 = new AbsoluteDate(1969, 7, 24, TimeScalesFactory.getTT());
        CelestialBodyFactory.clearCelestialBodyLoaders();
        CelestialBodyFactory.addDefaultCelestialBodyLoader("unxp1800.406");
        final CelestialPoint ssb2 = CelestialBodyFactory.getSolarSystemBarycenter();
        final PVCoordinates pvssb2 = ssb2.getPVCoordinates(date2, FramesFactory.getEME2000());

        final double EPS = 1E-12;

        // comparisons
        Assert.assertEquals(pvssb2.getPosition().getX() - pvssb.getPosition().getX(), -3.2412109375, EPS);
        Assert.assertEquals(pvssb2.getPosition().getY() - pvssb.getPosition().getY(), -0.2138824462890625, EPS);
        Assert.assertEquals(pvssb2.getPosition().getZ() - pvssb.getPosition().getZ(), -0.42032623291015625, EPS);

    }

    /**
     * @testType UT
     * 
     * @description check that Moon JPL body transformations from/to inertial and body frame properly compute spin
     * 
     * @testPassCriteria transformation spin and reference are the same (relative threshold: 1E-5, due to finite differences approximation, reference: finite differences).
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testMoonSpinDerivatives() throws PatriusException {
        // Build Jupiter
        Utils.setDataRoot("regular-dataPBASE");
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        // Check derivatives by finite differences
        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        // Body-oriented frame
        final Transform tBody = moon.getRotatingFrame(IAUPoleModelType.TRUE).getTransformTo(moon.getInertialFrame(IAUPoleModelType.CONSTANT), date);
        final Transform tBodydt = moon.getRotatingFrame(IAUPoleModelType.TRUE).getTransformTo(moon.getInertialFrame(IAUPoleModelType.CONSTANT), date.shiftedBy(10.));
        final Vector3D actualSpinBody = tBody.getRotationRate();
        final Vector3D referenceSpinBody = AngularCoordinates.estimateRate(tBody.getRotation(), tBodydt.getRotation(), 10.);
        Assert.assertEquals(0, actualSpinBody.distance(referenceSpinBody) / referenceSpinBody.getNorm(), 1E-5);

        // Inertial frame
        final Transform tInertial = moon.getInertialFrame(IAUPoleModelType.TRUE).getTransformTo(FramesFactory.getGCRF(), date);
        final Transform tInertialdt = moon.getInertialFrame(IAUPoleModelType.TRUE).getTransformTo(FramesFactory.getGCRF(), date.shiftedBy(10.));
        final Vector3D actualSpinInertial = tInertial.getRotationRate().normalize();
        final Vector3D referenceSpinInertial = AngularCoordinates.estimateRate(tInertial.getRotation(), tInertialdt.getRotation(), 10.).normalize();
        Assert.assertEquals(0, actualSpinInertial.distance(referenceSpinInertial) / referenceSpinInertial.getNorm(), 1E-5);
    }

    /**
     * Test all celestial bodies frames.
     * Checks:
     * <ul>
     * <li>Frame name</li>
     * <li>No translation wrt to ICRF centered on the body</li>
     * <li>Rotation of each transform wrt to ICRF is different</li>
     * <li>Rotation is checked (non-regression only). Reference: PATRIUS 4.9</li>
     * </ul>
     */
    @Test
    public void testCelestialBodyFrames() throws PatriusException {
        Utils.setDataRoot("regular-data");
        
        // Initialization
        final CelestialBody body = CelestialBodyFactory.getMoon();
        
        // Check frames names
        Assert.assertEquals("Moon ICRF frame", body.getICRF().toString());
        Assert.assertEquals("Moon EME2000 frame", body.getEME2000().toString());
        Assert.assertEquals("Moon Inertial frame (constant model)", body.getInertialFrame(IAUPoleModelType.CONSTANT)
                .toString());
        Assert.assertEquals("Moon Inertial frame (mean model)", body.getInertialFrame(IAUPoleModelType.MEAN).toString());
        Assert.assertEquals("Moon Inertial frame (true model)", body.getInertialFrame(IAUPoleModelType.TRUE).toString());
        Assert.assertEquals("Moon Rotating frame (constant model)", body.getRotatingFrame(IAUPoleModelType.CONSTANT).toString());
        Assert.assertEquals("Moon Rotating frame (mean model)", body.getRotatingFrame(IAUPoleModelType.MEAN).toString());
        Assert.assertEquals("Moon Rotating frame (true model)", body.getRotatingFrame(IAUPoleModelType.TRUE).toString());
        
        // Build all transformations wrt to ICRF
        final AbsoluteDate date = new AbsoluteDate(2020, 1, 1, 0, 0, 0, TimeScalesFactory.getTAI());
        final Transform t1 = body.getICRF().getTransformTo(body.getInertialFrame(IAUPoleModelType.CONSTANT), date);
        final Transform t2 = body.getICRF().getTransformTo(body.getInertialFrame(IAUPoleModelType.MEAN), date);
        final Transform t3 = body.getICRF().getTransformTo(body.getInertialFrame(IAUPoleModelType.TRUE), date);
        final Transform t4 = body.getICRF().getTransformTo(body.getRotatingFrame(IAUPoleModelType.CONSTANT), date);
        final Transform t5 = body.getICRF().getTransformTo(body.getRotatingFrame(IAUPoleModelType.MEAN), date);
        final Transform t6 = body.getICRF().getTransformTo(body.getRotatingFrame(IAUPoleModelType.TRUE), date);
        final Transform t7 = body.getICRF().getTransformTo(body.getEME2000(), date);
        final Transform t8 = body.getICRF().getTransformTo(body.getEclipticJ2000(), date);
        
        // Check there is no translation wrt to ICRF
        Assert.assertEquals(t1.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertEquals(t2.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertEquals(t3.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertEquals(t4.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertEquals(t5.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertEquals(t6.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertEquals(t7.getCartesian().getPosition().getNorm(), 0., 0.);
        Assert.assertEquals(t8.getCartesian().getPosition().getNorm(), 0., 0.);

        // Check that rotations are different
        Assert.assertFalse(t1.getAngular().getRotation().isEqualTo(t2.getAngular().getRotation()));
        Assert.assertFalse(t1.getAngular().getRotation().isEqualTo(t3.getAngular().getRotation()));
        Assert.assertFalse(t4.getAngular().getRotation().isEqualTo(t5.getAngular().getRotation()));
        Assert.assertFalse(t4.getAngular().getRotation().isEqualTo(t6.getAngular().getRotation()));
        
        // Non-regression only: reference 4.9
        Assert.assertTrue(t1.getAngular().getRotation().isEqualTo(new Rotation(false, new Quaternion(0.9791150769220948, 0.20330682271646888, -9.048352298852238E-6, -4.357639374190694E-5))));
        Assert.assertTrue(t2.getAngular().getRotation().isEqualTo(new Rotation(false, new Quaternion(0.9791196894615126, 0.20328460881314586, -7.947563293417777E-6, -3.827941401594056E-5))));
        Assert.assertTrue(t3.getAngular().getRotation().isEqualTo(new Rotation(false, new Quaternion(0.978140743597777, 0.2051429642516145, -0.006982067979128592, -0.033291149856733725))));
        Assert.assertTrue(t4.getAngular().getRotation().isEqualTo(new Rotation(false, new Quaternion(0.9248883711192153, 0.19204106332043216, -0.06673750252815996, 0.321322014066118))));
        Assert.assertTrue(t5.getAngular().getRotation().isEqualTo(new Rotation(false, new Quaternion(-0.1275486527673613, -0.026465862722669854, 0.20155443521248123, -0.970776343414771))));
        Assert.assertTrue(t6.getAngular().getRotation().isEqualTo(new Rotation(false, new Quaternion(-0.13024941882559138, -0.01342175034516523, 0.204822463789684, -0.9700013937311592))));
        
        // Test EME2000/Ecliptic J2000 case (known reference)
        Assert.assertTrue(t7.getAngular().getRotation().isEqualTo(new EME2000Provider().getTransform(AbsoluteDate.J2000_EPOCH).getRotation()));
        Assert.assertTrue(t8.getAngular().getRotation().isEqualTo(new EclipticJ2000Provider().getTransform(AbsoluteDate.J2000_EPOCH).getRotation()));

    }
}
