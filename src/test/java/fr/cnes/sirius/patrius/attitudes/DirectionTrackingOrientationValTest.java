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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:BodyCenterPointing law replaced by BodyCenterGroundPointing
 * VERSION::FA:185:14/04/2014:Deactivated obsolete test
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.LinkedList;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.GenericTargetDirection;
import fr.cnes.sirius.patrius.attitudes.directions.GroundVelocityDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.attitudes.directions.NadirDirection;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
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
 *              This class aims at testing the DirectionTrackingOrientation.
 *              </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id: DirectionTrackingOrientationValTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 */
public class DirectionTrackingOrientationValTest {

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
         * @coveredRequirements DV-ATT_240
         */
        ALIGNEMENT,
        /**
         * @featureTitle Comparison test
         * 
         * @featureDescription compare the result with those of the YawSterring Orekit law
         * 
         * @coveredRequirements DV-ATT_240
         */
        YAW_STEERING,
        /**
         * @featureTitle Comparison test
         * 
         * @featureDescription compare the result with those of the YawCompensation Orekit law
         * 
         * @coveredRequirements DV-ATT_240
         */
        YAW_COMPENSATION
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#YAW_STEERING}
     * 
     * @testedMethod {@link DirectionTrackingOrientation#getOrientation(AbsoluteDate, Frame)}
     * 
     * @description compare the attitudes given by DirectionTrackingOrientation with those given by YawSteering. The two
     *              directions given at the construction of DirectionTrackingOrientation are the sun-satellite direction
     *              and the earth-satellite direction. The earth-satellite direction should be aligned with the
     *              satelltie K axis and the sun-satellite direction should be aligned at best with the satellite I
     *              axis.
     * 
     * @input an elliptic orbit
     * 
     * @output the attitudes are computed with both ComposedAttitude and YawSteering laws on 100 points equally
     *         distributed over 1 orbit period
     * 
     * @testPassCriteria the angle of the rotation between the two attitudes (one obtained with the ComposedAttitude law
     *                   and the other one obtained with the YawSteering law) is below 1e-14 rad. (NB : the spins are
     *                   different between YawSteering and ComposedAttitude, they are computed differently).
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     */
    @Test
    public final void testYawSteering() throws PatriusException {
        // ellipstic orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit ellipticOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        // orbit period
        final double period = MathUtils.TWO_PI * MathLib.sqrt(MathLib.pow(a, 3) / mu);

        // yaw steering law with Orekit
        final BodyShape earthSphere = new OneAxisEllipsoid(6378136.460, 0., FramesFactory.getITRF());
        final AbstractGroundPointing geocentricLaw = new BodyCenterGroundPointing(earthSphere);
        final AttitudeProvider orekitYawSteering = new YawSteering(geocentricLaw, sun, Vector3D.PLUS_I);

        // yaw steering law with Sirius
        final IDirection d2 = new GenericTargetDirection(sun);
        final IOrientationLaw oneDirOrientation =
            new DirectionTrackingOrientation(d2, Vector3D.PLUS_I, Vector3D.PLUS_K);
        final LinkedList<IOrientationLaw> modifiers = new LinkedList<IOrientationLaw>();
        modifiers.add(oneDirOrientation);
        final AttitudeProvider siriusYawSteering = new ComposedAttitudeLaw(geocentricLaw, modifiers);

        // comparison between Orekit and Sirius
        Attitude attOrekit;
        Attitude attSirius;
        Rotation rotOrekit;
        Rotation rotSirius;

        // comparison done on 100 points equally distributed over one orbit
        final double h = period / 100.;

        // loop over 1 period
        AbsoluteDate currentDate = date;
        while (currentDate.durationFrom(date) < period) {

            currentDate = currentDate.shiftedBy(h);

            // orekit attitude
            attOrekit = orekitYawSteering.getAttitude(ellipticOrbit, currentDate, FramesFactory.getGCRF());

            // sirius attitude
            attSirius = siriusYawSteering.getAttitude(ellipticOrbit, currentDate, FramesFactory.getGCRF());

            // comparison of the rotations
            rotOrekit = attOrekit.getRotation();
            rotSirius = attSirius.getRotation();

            Assert.assertEquals(0, Rotation.distance(rotOrekit, rotSirius), this.epsilon);
        }
        /*
         * NB : the spin is different between YawSteering and TwoDirectionsLaw
         */
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#YAW_COMPENSATION}
     * 
     * @testedMethod {@link DirectionTrackingOrientation#getOrientation(AbsoluteDate, Frame)}
     * 
     * @description compare the attitudes given by DirectionTrackingOrientation with those given by YawCompensation. The
     *              two directions given at the construction of DirectionTrackingOrientation are the ground velocity
     *              direction and the nadir direction. The nadir direction should be aligned with the satelltie K axis
     *              and the ground velocity direction should be aligned at best with the satellite I axis.
     * 
     * @input a quasi circular orbit
     * 
     * @output the attitudes are computed with both ComposedAttitude and YawCompensation laws on 100 points equally
     *         distributed over 1 orbit period
     * 
     * @testPassCriteria the angle of the rotation between the two attitudes (one obtained with the ComposedAttitude law
     *                   and the other one obtained with the YawCompensation law) is below 1e-12 rad. (NB : the spins
     *                   are different between YawCompensation and ComposedAttitude, they are computed differently). The
     *                   epsilon 1e-12 is 100 times the comparison epsilon, it takes into account the errors due to the
     *                   line transformations from a frame to another one (those transformations are computed by the
     *                   method
     *                   {@link Transform#transformLine(fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line)}
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     * 
     * @throws PatriusException
     * 
     *         FIXME test deactivated during FT 185
     */
    @Test
    @Ignore
    public final void testYawCompensation() throws PatriusException {
        // ellipstic orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 8000000;
        final Orbit ellipticOrbit = new KeplerianOrbit(a, 0.01, MathLib.toRadians(40), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);
        // orbit period
        final double period = MathUtils.TWO_PI * MathLib.sqrt(MathLib.pow(a, 3) / mu);

        final OneAxisEllipsoid body = new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
            Constants.GRIM5C1_EARTH_FLATTENING, earth.getBodyOrientedFrame());

        // yaw compensation law with Orekit
        final AbstractGroundPointing nadirLaw = new NadirPointing(body);
        final AttitudeProvider orekitYawCompensation = new YawCompensation(nadirLaw);

        // yaw compesnation law with Sirius
        final IDirection nadirDir = new NadirDirection(body);
        final IDirection d2 = new GroundVelocityDirection(body, nadirDir);
        final IOrientationLaw oneDirOrientation = new DirectionTrackingOrientation(d2, Vector3D.MINUS_I,
            Vector3D.PLUS_K);
        final LinkedList<IOrientationLaw> modifiers = new LinkedList<IOrientationLaw>();
        modifiers.add(oneDirOrientation);
        final AttitudeProvider siriusYawCompensation = new ComposedAttitudeLaw(nadirLaw, modifiers);

        // comparison between Orekit and Sirius
        Attitude attOrekit;
        Attitude attSirius;
        Rotation rotOrekit;
        Rotation rotSirius;

        // comparison done on 100 points equally distributed over one orbit
        final double h = period / 100.;

        // loop over 1 period
        AbsoluteDate currentDate = date;
        while (currentDate.durationFrom(date) < period) {

            currentDate = currentDate.shiftedBy(h);

            // orekit attitude
            attOrekit = orekitYawCompensation.getAttitude(ellipticOrbit, currentDate, FramesFactory.getGCRF());

            // sirius attitude
            attSirius = siriusYawCompensation.getAttitude(ellipticOrbit, currentDate, FramesFactory.getGCRF());

            // comparison of the rotations
            rotOrekit = attOrekit.getRotation();
            rotSirius = attSirius.getRotation();

            Assert.assertEquals(0, Rotation.distance(rotOrekit, rotSirius), 100 * this.epsilon);
        }
        /*
         * NB : the spin is different between YawSteering and TwoDirectionsLaw
         */
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#ALIGNEMENT}
     * 
     * @testedMethod {@link TwoDirectionsAttitude#getAttitude(org.orekit.utils.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description for several points on an orbit, we test that the first given satellite axis is perfectly aligned
     *              with the first direction and that the second satellite axis is at best aligned with the second
     *              direction.
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
    public final void testBasic() throws PatriusException {
        // ellipstic orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit ellipticOrbit = new KeplerianOrbit(a, 0.93, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // yaw steering law : the K satellite axis should be aligned with the earth direction and the I satellite axis
        // should be aligned at best with the sun direction
        final IDirection d1 = new GenericTargetDirection(earth);
        final IDirection d2 = new GenericTargetDirection(sun);
        final IOrientationLaw oneDirOrientation =
            new DirectionTrackingOrientation(d2, Vector3D.PLUS_I, Vector3D.PLUS_K);
        final LinkedList<IOrientationLaw> modifiers = new LinkedList<IOrientationLaw>();
        modifiers.add(oneDirOrientation);
        final BodyCenterPointing geocentricLaw = new BodyCenterPointing(earth.getBodyOrientedFrame());
        final AttitudeLaw siriusYawSteering = new ComposedAttitudeLaw(geocentricLaw, modifiers);

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
     * Before the test
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SUN, loaderSun);
        final JPLEphemeridesLoader loaderEarth = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH);

        earth = loaderEarth.loadCelestialBody(CelestialBodyFactory.EARTH);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH, loaderEarth);

    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
