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
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END_HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.GeodeticPointTest;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TopocentricFrameTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF 2005
    private Frame frameITRF2005;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    // Body mu
    private double mu;

    @Test
    public void testZero() {

        final GeodeticPoint point = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(this.earthSpheric, point, "zero");

        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.PLUS_J);
        final double yDiff = Vector3D.dotProduct(topoFrame.getNorth(), Vector3D.PLUS_K);
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.PLUS_I);
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);
    }

    @Test
    public void testPole() {

        final GeodeticPoint point = new GeodeticPoint(FastMath.PI / 2., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(this.earthSpheric, point, "north pole");

        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.PLUS_J);
        final double yDiff = Vector3D.dotProduct(topoFrame.getNorth(), Vector3D.PLUS_I.negate());
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.PLUS_K);
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);
    }

    @Test
    public void testNormalLatitudes() {

        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(this.earthSpheric, point1, "lat 45");

        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(MathLib.toRadians(-45.), MathLib.toRadians(30.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(this.earthSpheric, point2, "lat -45");

        // Check that frame North and Zenith directions are all normal to each other, and East are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getEast());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(0., yDiff, Utils.epsilonTest);
        Assert.assertEquals(0., zDiff, Utils.epsilonTest);
    }

    @Test
    public void testOppositeLongitudes() {

        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(this.earthSpheric, point1, "lon 30");
        final GeodeticPoint p1 = topoFrame1.getPoint();
        Assert.assertEquals(point1.getLatitude(), p1.getLatitude(), 1.0e-15);
        Assert.assertEquals(point1.getLongitude(), p1.getLongitude(), 1.0e-15);
        Assert.assertEquals(point1.getAltitude(), p1.getAltitude(), 1.0e-15);

        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(210.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(this.earthSpheric, point2, "lon 210");

        // Check that frame North and Zenith directions are all normal to each other,
        // and East of the one is West of the other
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(0., yDiff, Utils.epsilonTest);
        Assert.assertEquals(0., zDiff, Utils.epsilonTest);
    }

    @Test
    public void testAntipodes()
                               throws PatriusException {

        // First point at latitude 45° and longitude 30
        final GeodeticPoint point1 = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(this.earthSpheric, point1, "lon 30");

        // Second point at latitude -45° and longitude 210
        final GeodeticPoint point2 = new GeodeticPoint(MathLib.toRadians(-45.), MathLib.toRadians(210.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(this.earthSpheric, point2, "lon 210");

        // Check that frame Zenith directions are opposite to each other,
        // and East and North are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(-1., zDiff, Utils.epsilonTest);
    }

    @Test
    public void testSiteAtZenith()
                                  throws PatriusException {

        // Surface point at latitude 45°
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(this.earthSpheric, point, "lon 30 lat 45");

        // Point at 800 km over zenith
        final GeodeticPoint satPoint = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(30.), 800000.);

        // Zenith point elevation = 90 deg
        final double site =
            topoFrame.getElevation(this.earthSpheric.transform(satPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2., site, Utils.epsilonAngle);

        // Zenith point range = defined altitude
        final double range =
            topoFrame.getRange(this.earthSpheric.transform(satPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(800000., range, 1e-8);
    }

    @Test
    public void testAzimuthEquatorial()
                                       throws PatriusException {

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(this.earthSpheric, point, "lon 30 lat 0");

        // Point at infinite, separated by +20 deg in longitude
        // *****************************************************
        GeodeticPoint infPoint = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(50.), 1000000000.);

        // Azimuth = pi/2
        double azi =
            topoFrame.getAzimuth(this.earthSpheric.transform(infPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        double site =
            topoFrame.getElevation(this.earthSpheric.transform(infPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2. - MathLib.abs(point.getLongitude() - infPoint.getLongitude()), site,
            1.e-2);

        // Point at infinite, separated by -20 deg in longitude
        // *****************************************************
        infPoint = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(10.), 1000000000.);

        // Azimuth = pi/2
        azi = topoFrame.getAzimuth(this.earthSpheric.transform(infPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(3 * FastMath.PI / 2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        site =
            topoFrame.getElevation(this.earthSpheric.transform(infPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2. - MathLib.abs(point.getLongitude() - infPoint.getLongitude()), site,
            1.e-2);

    }

    @Test
    public void testAzimuthPole()
                                 throws PatriusException {

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(89.999), MathLib.toRadians(0.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(this.earthSpheric, point, "lon 0 lat 90");

        // Point at 30 deg longitude
        // **************************
        GeodeticPoint satPoint = new GeodeticPoint(MathLib.toRadians(28.), MathLib.toRadians(30.), 800000.);

        // Azimuth =
        double azi =
            topoFrame.getAzimuth(this.earthSpheric.transform(satPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI - satPoint.getLongitude(), azi, 1.e-5);

        // Point at -30 deg longitude
        // ***************************
        satPoint = new GeodeticPoint(MathLib.toRadians(28.), MathLib.toRadians(-30.), 800000.);

        // Azimuth =
        azi = topoFrame.getAzimuth(this.earthSpheric.transform(satPoint), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI - satPoint.getLongitude(), azi, 1.e-5);

    }

    @Test
    public void testDoppler()
                             throws PatriusException {

        // Surface point at latitude 45, longitude 5
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(5.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(this.earthSpheric, point, "lon 5 lat 45");

        // Point at 30 deg longitude
        // ***************************
        final CircularOrbit orbit =
            new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, MathLib.toRadians(50.), MathLib.toRadians(120.),
                MathLib.toRadians(90.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Transform satellite position to position/velocity parameters in body frame
        final Transform eme2000ToItrf =
            FramesFactory.getEME2000().getTransformTo(this.earthSpheric.getBodyFrame(), this.date);
        final PVCoordinates pvSatItrf = eme2000ToItrf.transformPVCoordinates(orbit.getPVCoordinates());

        // Compute range rate directly
        // ********************************************
        final double dop = topoFrame.getRangeRate(pvSatItrf, this.earthSpheric.getBodyFrame(), this.date);

        // Compare to finite difference computation (2 points)
        // *****************************************************
        final double dt = 0.1;
        final KeplerianPropagator extrapolator = new KeplerianPropagator(orbit);

        // Extrapolate satellite position a short while after reference date
        final AbsoluteDate dateP = this.date.shiftedBy(dt);
        final Transform j2000ToItrfP =
            FramesFactory.getEME2000().getTransformTo(this.earthSpheric.getBodyFrame(), dateP);
        final SpacecraftState orbitP = extrapolator.propagate(dateP);
        final Vector3D satPointGeoP = j2000ToItrfP.transformPVCoordinates(orbitP.getPVCoordinates()).getPosition();

        // Retropolate satellite position a short while before reference date
        final AbsoluteDate dateM = this.date.shiftedBy(-dt);
        final Transform j2000ToItrfM =
            FramesFactory.getEME2000().getTransformTo(this.earthSpheric.getBodyFrame(), dateM);
        final SpacecraftState orbitM = extrapolator.propagate(dateM);
        final Vector3D satPointGeoM = j2000ToItrfM.transformPVCoordinates(orbitM.getPVCoordinates()).getPosition();

        // Compute ranges at both instants
        final double rangeP = topoFrame.getRange(satPointGeoP, this.earthSpheric.getBodyFrame(), dateP);
        final double rangeM = topoFrame.getRange(satPointGeoM, this.earthSpheric.getBodyFrame(), dateM);
        final double dopRef2 = (rangeP - rangeM) / (2. * dt);
        Assert.assertEquals(dopRef2, dop, 1.e-3);

    }

    @Test
    public void testEllipticEarth() throws PatriusException {

        // Elliptic earth shape
        final OneAxisEllipsoid earthElliptic =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, this.frameITRF2005);

        // Satellite point
        // Caution !!! Sat point target shall be the same whatever earth shape chosen !!
        final GeodeticPoint satPointGeo = new GeodeticPoint(MathLib.toRadians(30.), MathLib.toRadians(15.), 800000.);
        final Vector3D satPoint = earthElliptic.transform(satPointGeo);

        // ****************************
        // Test at equatorial position
        // ****************************
        GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(5.), 0.);
        TopocentricFrame topoElliptic = new TopocentricFrame(earthElliptic, point, "elliptic, equatorial lon 5");
        TopocentricFrame topoSpheric = new TopocentricFrame(this.earthSpheric, point, "spheric, equatorial lon 5");

        // Compare azimuth/elevation/range of satellite point : shall be strictly identical
        // ***************************************************
        double aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), this.date);
        double aziSphe = topoSpheric.getAzimuth(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(aziElli, aziSphe, Utils.epsilonAngle);

        double eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), this.date);
        double eleSphe = topoSpheric.getElevation(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(eleElli, eleSphe, Utils.epsilonAngle);

        double disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), this.date);
        double disSphe = topoSpheric.getRange(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(disElli, disSphe, Utils.epsilonTest);

        // Infinite point separated by -20 deg in longitude
        // *************************************************
        GeodeticPoint infPointGeo = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(-15.), 1000000000.);
        Vector3D infPoint = earthElliptic.transform(infPointGeo);

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(3 * FastMath.PI / 2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2. - MathLib.abs(point.getLongitude() - infPointGeo.getLongitude()),
            eleElli, 1.e-2);

        // Infinite point separated by +20 deg in longitude
        // *************************************************
        infPointGeo = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(25.), 1000000000.);
        infPoint = earthElliptic.transform(infPointGeo);

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2. - MathLib.abs(point.getLongitude() - infPointGeo.getLongitude()),
            eleElli, 1.e-2);

        // ************************
        // Test at polar position
        // ************************
        point = new GeodeticPoint(MathLib.toRadians(89.999), MathLib.toRadians(0.), 0.);
        topoSpheric = new TopocentricFrame(this.earthSpheric, point, "lon 0 lat 90");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 0 lat 90");

        // Compare azimuth/elevation/range of satellite point : slight difference due to earth flatness
        // ***************************************************
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), this.date);
        aziSphe = topoSpheric.getAzimuth(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(aziElli, aziSphe, 1.e-7);

        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), this.date);
        eleSphe = topoSpheric.getElevation(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(eleElli, eleSphe, 1.e-2);

        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), this.date);
        disSphe = topoSpheric.getRange(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(disElli, disSphe, 20.e+3);

        // *********************
        // Test at any position
        // *********************
        point = new GeodeticPoint(MathLib.toRadians(60), MathLib.toRadians(30.), 0.);
        topoSpheric = new TopocentricFrame(this.earthSpheric, point, "lon 10 lat 45");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 10 lat 45");

        // Compare azimuth/elevation/range of satellite point : slight difference
        // ***************************************************
        aziElli = topoElliptic.getAzimuth(satPoint, earthElliptic.getBodyFrame(), this.date);
        aziSphe = topoSpheric.getAzimuth(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(aziElli, aziSphe, 1.e-2);

        eleElli = topoElliptic.getElevation(satPoint, earthElliptic.getBodyFrame(), this.date);
        eleSphe = topoSpheric.getElevation(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(eleElli, eleSphe, 1.e-2);

        disElli = topoElliptic.getRange(satPoint, earthElliptic.getBodyFrame(), this.date);
        disSphe = topoSpheric.getRange(satPoint, this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(disElli, disSphe, 20.e+3);

    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

            // Elliptic earth shape
            this.earthSpheric = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

            // Reference date
            this.date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            // Body mu
            this.mu = 3.9860047e14;

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @Test
    public void testVisibilityCircle() throws PatriusException, IOException {

        // a few random from International Laser Ranging Service
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF());
        final TopocentricFrame[] ilrs = {
            new TopocentricFrame(earth,
                new GeodeticPoint(MathLib.toRadians(52.3800), MathLib.toRadians(3.0649), 133.745),
                "Potsdam"),
            new TopocentricFrame(earth,
                new GeodeticPoint(MathLib.toRadians(36.46273), MathLib.toRadians(-6.20619), 64.0),
                "San Fernando"),
            new TopocentricFrame(earth,
                new GeodeticPoint(MathLib.toRadians(35.5331), MathLib.toRadians(24.0705), 157.0),
                "Chania")
        };

        final PolynomialFunction distanceModel =
            new PolynomialFunction(new double[] { 7.0892e+05, 3.1913, -8.2181e-07, 1.4033e-13 });
        for (final TopocentricFrame station : ilrs) {
            for (double altitude = 500000; altitude < 2000000; altitude += 100000) {
                for (double azimuth = 0; azimuth < 2 * FastMath.PI; azimuth += 0.05) {
                    final GeodeticPoint p = station.computeLimitVisibilityPoint(Constants.WGS84_EARTH_EQUATORIAL_RADIUS
                        + altitude,
                        azimuth, MathLib.toRadians(5.0));
                    final double d =
                        station.getRange(earth.transform(p), earth.getBodyFrame(), AbsoluteDate.J2000_EPOCH);
                    Assert.assertEquals(distanceModel.value(altitude), d, 40000.0);
                }
            }
        }

    }

    @Test
    public final void seriaTest() throws PatriusException {

        // a few random from International Laser Ranging Service
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());

        final TopocentricFrame[] frames = {
            new TopocentricFrame(earth, new GeodeticPoint(MathLib.toRadians(52.3800), MathLib.toRadians(3.0649),
                133.745), "Potsdam"),
            new TopocentricFrame(earth, new GeodeticPoint(MathLib.toRadians(36.46273),
                MathLib.toRadians(-6.20619), 64.0), "San Fernando"),
            new TopocentricFrame(earth, new GeodeticPoint(MathLib.toRadians(35.5331), MathLib.toRadians(24.0705),
                157.0), "Chania") };

        for (final TopocentricFrame f : frames) {
            final TopocentricFrame frameRecover = TestUtils.serializeAndRecover(f);
            this.frameEq(f, frameRecover);
        }
    }

    private void frameEq(final TopocentricFrame f1, final TopocentricFrame f2) {
        FrameTest.frameEq(f1, f2);
        Assert.assertTrue(f1.getEast().equals(f2.getEast()));
        Assert.assertTrue(f1.getNadir().equals(f2.getNadir()));
        Assert.assertTrue(f1.getNorth().equals(f2.getNorth()));
        Assert.assertTrue(f1.getSouth().equals(f2.getSouth()));
        Assert.assertTrue(f1.getWest().equals(f2.getWest()));
        Assert.assertTrue(f1.getZenith().equals(f2.getZenith()));
        GeodeticPointTest.assertEqualsGeodeticPoint(f1.getPoint(), f2.getPoint());
    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.frameITRF2005 = null;
        this.earthSpheric = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
