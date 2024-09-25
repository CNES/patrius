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
 * @history creation 03/04/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-99:08/12/2023:[PATRIUS] Ajout du repere de calcul dans MomentumDirection
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:667:24/08/2016:add constructors
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.CelestialBodyPolesAxisDirection;
import fr.cnes.sirius.patrius.attitudes.directions.GenericTargetDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.attitudes.directions.MomentumDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              This class aims at testing the SunPointing.
 *              </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id: SunPointingTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 */
public class SunPointingTest {
    /** Sun. */
    private static CelestialBody sun;
    /** Earth. */
    private static CelestialBody earth;
    /** Epsilon. */
    private final double epsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Basic test
         * 
         * @featureDescription test the behavior of the attitude law
         * 
         * @coveredRequirements DV-ATT_340
         */
        ALIGNEMENT
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ALIGNEMENT}
     * 
     * @testedMethod {@link SunPointing#getAttitude(org.orekit.utils.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description for several points on an orbit, we test that the first given satellite axis is perfectly aligned
     *              with the first direction (sun-satellite) and that the second satellite axis is at best aligned with
     *              the second direction (the sun poles axis).
     * 
     * @input an elliptic orbit
     * 
     * @output angle between the first axis and the first direction, angle between the second axis and the second
     *         direction.
     * 
     * @testPassCriteria the angle between the first axis and the first direction should be equal to 0 (with an error of
     *                   1e-14), the angle between the second axis and the ssecond direction should be minimal.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     */
    @Test
    public void testBasicPolesAxis() throws PatriusException {
        // ellipstic orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit ellipticOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // yaw steering law : the K satellite axis should be aligned with the earth direction and the I satellite axis
        // should be aligned at best with the sun direction
        final IDirection d1 = new GenericTargetDirection(sun);
        final IDirection d2 = new CelestialBodyPolesAxisDirection(sun);
        final AttitudeLaw siriusYawSteering = new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I);

        final double period = MathUtils.TWO_PI * MathLib.sqrt(MathLib.pow(a, 3) / mu);

        // for 100 points equally distributed on an orbit, we check if the first axis is perfectly aligned with the
        // first direction and that the second axis is aligned with the second direction
        AbsoluteDate currentDate = date;
        final double h = period / 100.;

        final AttitudeFrame frame = new AttitudeFrame(ellipticOrbit, siriusYawSteering, FramesFactory.getGCRF());
        double angle;
        Frame f;
        while (currentDate.durationFrom(date) < period) {
            currentDate = currentDate.shiftedBy(h);

            // in the satellite frame, the first direction should be aligned with the K axis
            Assert.assertEquals(0,
                Vector3D.angle(d1.getVector(ellipticOrbit, currentDate, frame).normalize(), Vector3D.PLUS_K),
                this.epsilon);

            // angle between the I satellite axis and the second direction : it has to be minimal
            angle = Vector3D.angle(d2.getVector(ellipticOrbit, currentDate, frame), Vector3D.PLUS_I);

            for (int j = 0; j < 360; j++) {
                f = new Frame(frame, new Transform(currentDate, new Rotation(Vector3D.PLUS_K, MathLib.toRadians(j))),
                    "");
                if (angle > Vector3D.angle(d2.getVector(ellipticOrbit, currentDate, f), Vector3D.PLUS_I)) {
                    // if the second axis is not best aligned with the second direction
                    Assert.fail();
                }
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ALIGNEMENT}
     * 
     * @testedMethod {@link SunPointing#getAttitude(org.orekit.utils.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description for several points on an orbit, we test that the first given satellite axis is perfectly aligned
     *              with the first direction (sun-satellite) and that the second satellite axis is at best aligned with
     *              the second direction (the normal to the orbit plane).
     * 
     * @input an elliptic orbit
     * 
     * @output angle between the first axis and the first direction, angle between the second axis and the second
     *         direction.
     * 
     * @testPassCriteria the angle between the first axis and the first direction should be equal to 0 (with an error of
     *                   1e-14), the angle between the second axis and the ssecond direction should be minimal.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     */
    @Test
    public void testBasicNormal() throws PatriusException {
        // ellipstic orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Frame gcrf = FramesFactory.getGCRF();
        final Orbit ellipticOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            gcrf, date, mu);

        // yaw steering law : the K satellite axis should be aligned with the sun-satellite direction and the I
        // satellite axis
        // should be aligned at best with the normal to the satellite orbit
        final IDirection d1 = new GenericTargetDirection(sun);
        final IDirection d2 = new MomentumDirection(earth.getInertialFrame(IAUPoleModelType.CONSTANT));
        final AttitudeLaw siriusYawSteering = new SunPointing(earth.getInertialFrame(IAUPoleModelType.CONSTANT), Vector3D.PLUS_K, Vector3D.PLUS_I);

        final double period = MathUtils.TWO_PI * MathLib.sqrt(MathLib.pow(a, 3) / mu);

        // for 100 points equally distributed on an orbit, we check if the first axis is perfectly aligned with the
        // first direction and that the second axis is aligned with the second direction
        AbsoluteDate currentDate = date;
        final double h = period / 100.;

        final AttitudeFrame satFrame = new AttitudeFrame(ellipticOrbit, siriusYawSteering, FramesFactory.getGCRF());
        double angle;
        Frame f;
        Transform t;
        while (currentDate.durationFrom(date) < period) {
            currentDate = currentDate.shiftedBy(h);

            // in the satellite frame, the first direction should be aligned with the K axis
            Assert.assertEquals(0,
                Vector3D.angle(d1.getVector(ellipticOrbit, currentDate, satFrame).normalize(), Vector3D.PLUS_K),
                this.epsilon);

            // angle between the I satellite axis and the second direction : it has to be minimal
            t = gcrf.getTransformTo(satFrame, currentDate);

            angle = Vector3D.angle(t.transformVector(d2.getVector(ellipticOrbit, currentDate, gcrf)), Vector3D.PLUS_I);

            for (int j = 0; j < 360; j++) {
                f = new Frame(satFrame,
                    new Transform(currentDate, new Rotation(Vector3D.PLUS_K, MathLib.toRadians(j))), "");
                t = gcrf.getTransformTo(f, currentDate);
                if (angle > Vector3D.angle(t.transformVector(d2.getVector(ellipticOrbit, currentDate, gcrf)),
                    Vector3D.PLUS_I)) {
                    // if the second axis is not best aligned with the second direction
                    Assert.fail();
                }
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link SunPointing#getAttitude(org.orekit.utils.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description check the 4 constructors returns same results.
     * 
     * @input an orbit
     * 
     * @output attitude.
     * 
     * @testPassCriteria attitude is the exactly the same with all constructors.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     * 
     * @throws PatriusException
     */
    @Test
    public void testConstructors() throws PatriusException {
        // elliptic orbit
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final Orbit orbit = new KeplerianOrbit(10000000, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);

        // Check results are exactly the same with default constructor 1 and detailed constructor 1
        final AttitudeLaw law1 = new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I);
        final AttitudeLaw law2 = new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I, CelestialBodyFactory.getSun());
        final Attitude attitude1 = law1.getAttitude(orbit);
        final Attitude attitude2 = law2.getAttitude(orbit);
        Assert.assertEquals(0., Rotation.distance(attitude1.getRotation(), attitude2.getRotation()));

        // Check results are exactly the same with default constructor 2 and detailed constructor 2
        final AttitudeLaw law3 = new SunPointing(earth.getInertialFrame(IAUPoleModelType.CONSTANT), Vector3D.PLUS_K, Vector3D.PLUS_I);
        final AttitudeLaw law4 =
            new SunPointing(earth.getInertialFrame(IAUPoleModelType.CONSTANT), Vector3D.PLUS_K, Vector3D.PLUS_I, CelestialBodyFactory.getSun());
        final Attitude attitude3 = law3.getAttitude(orbit);
        final Attitude attitude4 = law4.getAttitude(orbit);
        Assert.assertEquals(0., Rotation.distance(attitude3.getRotation(), attitude4.getRotation()));
    }

    /**
     * @throws PatriusException
     *         get class parameters
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {
        final Vector3D firstAxis = new Vector3D(15000, 1121, 3000);
        final Vector3D secondAxis = new Vector3D(1620, 2100, 1300);
        final SunPointing sunPointing = new SunPointing(firstAxis, secondAxis);
        Assert.assertEquals(firstAxis.getY(), sunPointing.getFirstAxis().getY(), 0);
        Assert.assertEquals(secondAxis.getZ(), sunPointing.getSecondAxis().getZ());
        Assert.assertNotNull(sunPointing.toString());
    }

    /**
     * Before the test
     * 
     * @throws PatriusException
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        final JPLCelestialBodyLoader loaderSun = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SUN);

        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        sun = (CelestialBody) loaderSun.loadCelestialPoint(CelestialBodyFactory.SUN);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SUN, loaderSun);
        final JPLCelestialBodyLoader loaderEarth = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH);

        earth = (CelestialBody) loaderEarth.loadCelestialPoint(CelestialBodyFactory.EARTH);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH, loaderEarth);
    }
}
