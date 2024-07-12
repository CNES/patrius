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
* VERSION:4.11.1:FA:FA-60:30/06/2023:[PATRIUS] Erreur dans les méthodes getNeighbors de FacetBodyShape
* VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
* VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3250:03/11/2022:[PATRIUS] Generalisation de TopocentricFrame
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END_HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.GeodeticPointTest;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape.EllipsoidType;
import fr.cnes.sirius.patrius.bodies.mesh.ObjMeshLoader;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class TopocentricFrameTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF 2005
    private Frame frameITRF2005;

    // Earth spheric ellipsoidal shape
    private OneAxisEllipsoid earthSpheric;

    // Facet body radius
    private double facetBodyRadius;

    // Earth spheric facet-defined shape
    private BodyShape earthFacetShape;

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
    public void testAntipodes() {

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

    @Test
    public void testCartesianConstructors() throws PatriusException {

        final double oneOverSqrt2 = 1 / MathLib.sqrt(2);
        final double oneOverSqrt3 = 1 / MathLib.sqrt(3);

        /*
         * Test case 1: test equality between geodetic and cartesian versions with ellipsoid and without zenith
         */
        // Check that construction using cartesian point with ellipsoid gives same result as with geodetic point
        // Cartesian topocentric frame is ENU by default and no zenith provided yet
        GeodeticPoint geodetic = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(30.), 0.);
        Vector3D cartesian = this.earthSpheric.transform(geodetic);
        TopocentricFrame topoFrameGeodetic = new TopocentricFrame(this.earthSpheric, geodetic, "geodetic_frame");
        TopocentricFrame topoFrameCartesian = new TopocentricFrame(this.earthSpheric, cartesian, -MathUtils.HALF_PI,
            "cartesian_frame");

        // Check that components are all the same
        double xDiff = Vector3D.dotProduct(topoFrameGeodetic.getEast(), topoFrameCartesian.getEast());
        double yDiff = Vector3D.dotProduct(topoFrameGeodetic.getNorth(), topoFrameCartesian.getNorth());
        double zDiff = Vector3D.dotProduct(topoFrameGeodetic.getZenith(), topoFrameCartesian.getZenith());

        // Assertions
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);

        /*
         * Test case 2: test equality between geodetic and cartesian versions with facet body and without zenith
         */
        // Check that construction using cartesian point with facet body gives same result as with geodetic point
        // Cartesian topocentric frame is ENU by default and no zenith provided yet
        geodetic = new GeodeticPoint(MathLib.toRadians(45.), MathLib.toRadians(30.), 0.);
        cartesian = this.earthFacetShape.transform(geodetic);
        topoFrameGeodetic = new TopocentricFrame(this.earthFacetShape, geodetic, "geodetic_frame");
        topoFrameCartesian = new TopocentricFrame(this.earthFacetShape, cartesian, "cartesian_frame");

        // Check that components are all the same
        xDiff = Vector3D.dotProduct(topoFrameGeodetic.getEast(), topoFrameCartesian.getEast());
        yDiff = Vector3D.dotProduct(topoFrameGeodetic.getNorth(), topoFrameCartesian.getNorth());
        zDiff = Vector3D.dotProduct(topoFrameGeodetic.getZenith(), topoFrameCartesian.getZenith());

        // Assertions with low precision due to facets orientation that is different from true sphere
        Assert.assertEquals(1., xDiff, 1.4E-3);
        Assert.assertEquals(1., yDiff, 7E-4);
        Assert.assertEquals(1., zDiff, 7E-4);

        /*
         * Test case 3: test topocentric frame computation with ellipsoid and zenith = (0, -1, 0) at north pole
         */
        // Create cartesian zenith
        Vector3D zenith = new Vector3D(0., -1., 0.);

        // Compute associated topocentric frame at north pole
        cartesian = new Vector3D(this.earthSpheric.getEquatorialRadius(), Vector3D.PLUS_K);
        topoFrameCartesian = new TopocentricFrame(this.earthSpheric, cartesian, zenith, "cartesian_frame");

        // Assertions
        Assert.assertEquals(-MathUtils.HALF_PI, topoFrameCartesian.getPoint().getLongitude(), Utils.epsilonTest);
        Assert.assertEquals(0., topoFrameCartesian.getPoint().getLatitude(), Utils.epsilonTest);

        // Check that components are those expected
        xDiff = Vector3D.dotProduct(Vector3D.PLUS_I, topoFrameCartesian.getEast());
        yDiff = Vector3D.dotProduct(Vector3D.PLUS_K, topoFrameCartesian.getNorth());
        zDiff = Vector3D.dotProduct(Vector3D.MINUS_J, topoFrameCartesian.getZenith());

        // Assertions
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);

        /*
         * Test case 4: test topocentric frame computation with ellipsoid and zenith = normalized (1, 1, 1)
         */
        // Create cartesian zenith
        zenith = new Vector3D(1., 1., 1.).normalize();

        // Compute associated topocentric frame
        cartesian = new Vector3D(this.earthSpheric.getEquatorialRadius(), zenith);
        topoFrameCartesian = new TopocentricFrame(this.earthSpheric, cartesian, zenith, "cartesian_frame");

        // Assertions
        double delta = MathLib.asin(oneOverSqrt3);
        double alpha = MathLib.acos(oneOverSqrt3 / MathLib.cos(delta));
        Assert.assertEquals(alpha, topoFrameCartesian.getPoint().getLongitude(), Utils.epsilonTest);
        Assert.assertEquals(delta, topoFrameCartesian.getPoint().getLatitude(), Utils.epsilonTest);

        // Check that components are those expected
        Vector3D east = new Vector3D(oneOverSqrt2, Vector3D.MINUS_I, oneOverSqrt2, Vector3D.PLUS_J);
        double deltaNorth = MathUtils.HALF_PI - delta;
        double alphaNorth = MathLib.PI + alpha;
        double northX = MathLib.cos(deltaNorth) * MathLib.cos(alphaNorth);
        double northY = MathLib.cos(deltaNorth) * MathLib.sin(alphaNorth);
        double northZ = MathLib.sin(deltaNorth);
        Vector3D north = new Vector3D(northX, northY, northZ);
        xDiff = Vector3D.dotProduct(east, topoFrameCartesian.getEast());
        yDiff = Vector3D.dotProduct(north, topoFrameCartesian.getNorth());
        zDiff = Vector3D.dotProduct(zenith, topoFrameCartesian.getZenith());
        double northCheck = Vector3D.dotProduct(north, zenith.crossProduct(east));

        // Assertions
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);
        Assert.assertEquals(1., northCheck, Utils.epsilonTest);

        /*
         * Test case 5: test topocentric frame computation with facet body and zenith = (0, -1, 0) at north pole
         */
        // Create cartesian zenith
        zenith = new Vector3D(0., -1., 0.);

        // Compute associated topocentric frame at north pole
        Line line = new Line(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.ZERO);
        cartesian = this.earthFacetShape.getIntersectionPoints(line, earthFacetShape.getBodyFrame(),
            AbsoluteDate.J2000_EPOCH)[0];
        topoFrameCartesian = new TopocentricFrame(this.earthFacetShape, cartesian, zenith, "cartesian_frame");

        // Assertions
        Assert.assertEquals(0., cartesian.distance(new Vector3D(this.facetBodyRadius, Vector3D.PLUS_K)), 1E-11);
        Assert.assertEquals(-MathUtils.HALF_PI, topoFrameCartesian.getPoint().getLongitude(), Utils.epsilonTest);
        Assert.assertEquals(0., topoFrameCartesian.getPoint().getLatitude(), Utils.epsilonTest);

        // Check that components are those expected
        xDiff = Vector3D.dotProduct(Vector3D.PLUS_I, topoFrameCartesian.getEast());
        yDiff = Vector3D.dotProduct(Vector3D.PLUS_K, topoFrameCartesian.getNorth());
        zDiff = Vector3D.dotProduct(Vector3D.MINUS_J, topoFrameCartesian.getZenith());

        // Assertions
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);

        /*
         * Test case 6: test topocentric frame computation with facet body and zenith = normalized (1, 1, 1)
         */
        // Create cartesian zenith
        zenith = new Vector3D(1., 1., 1.).normalize();

        // Compute associated topocentric frame
        line = new Line(Vector3D.ZERO, zenith, Vector3D.ZERO);
        cartesian = this.earthFacetShape.getIntersectionPoints(line, earthFacetShape.getBodyFrame(),
            AbsoluteDate.J2000_EPOCH)[0];
        topoFrameCartesian = new TopocentricFrame(this.earthFacetShape, cartesian, zenith, "cartesian_frame");

        // Assertions
        delta = MathLib.asin(oneOverSqrt3);
        alpha = MathLib.acos(oneOverSqrt3 / MathLib.cos(delta));
        Assert.assertEquals(alpha, topoFrameCartesian.getPoint().getLongitude(), Utils.epsilonTest);
        Assert.assertEquals(delta, topoFrameCartesian.getPoint().getLatitude(), Utils.epsilonTest);

        // Check that components are those expected
        east = new Vector3D(oneOverSqrt2, Vector3D.MINUS_I, oneOverSqrt2, Vector3D.PLUS_J);
        deltaNorth = MathUtils.HALF_PI - delta;
        alphaNorth = MathLib.PI + alpha;
        northX = MathLib.cos(deltaNorth) * MathLib.cos(alphaNorth);
        northY = MathLib.cos(deltaNorth) * MathLib.sin(alphaNorth);
        northZ = MathLib.sin(deltaNorth);
        north = new Vector3D(northX, northY, northZ);
        xDiff = Vector3D.dotProduct(east, topoFrameCartesian.getEast());
        yDiff = Vector3D.dotProduct(north, topoFrameCartesian.getNorth());
        zDiff = Vector3D.dotProduct(zenith, topoFrameCartesian.getZenith());
        northCheck = Vector3D.dotProduct(north, zenith.crossProduct(east));

        // Altitude
        double altitude = topoFrameCartesian.getPoint().getAltitude();

        // Assertions
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);
        Assert.assertEquals(1., northCheck, Utils.epsilonTest);
        Assert.assertEquals(0., altitude, 1E-10);

        /*
         * Test case 7: same as test 5 with altitude equal to shape's radius
         */
        // Create cartesian zenith
        zenith = new Vector3D(1., 1., 1.).normalize();

        // Compute associated topocentric frame, with altitude greater than 0
        line = new Line(Vector3D.ZERO, zenith, Vector3D.ZERO);
        cartesian = this.earthFacetShape.getIntersectionPoints(line, earthFacetShape.getBodyFrame(),
            AbsoluteDate.J2000_EPOCH)[0];
        final double expectedAltitude = 1;

        cartesian = cartesian.add(expectedAltitude, ((FacetBodyShape) this.earthFacetShape)
            .getNeighbors(cartesian, 0).get(0).getNormal());
        topoFrameCartesian = new TopocentricFrame(this.earthFacetShape, cartesian, zenith, "cartesian_frame");

        // Assertions
        delta = MathLib.asin(oneOverSqrt3);
        alpha = MathLib.acos(oneOverSqrt3 / MathLib.cos(delta));
        Assert.assertEquals(alpha, topoFrameCartesian.getPoint().getLongitude(), Utils.epsilonTest);
        Assert.assertEquals(delta, topoFrameCartesian.getPoint().getLatitude(), Utils.epsilonTest);

        // Check that components are those expected
        east = new Vector3D(oneOverSqrt2, Vector3D.MINUS_I, oneOverSqrt2, Vector3D.PLUS_J);
        deltaNorth = MathUtils.HALF_PI - delta;
        alphaNorth = MathLib.PI + alpha;
        northX = MathLib.cos(deltaNorth) * MathLib.cos(alphaNorth);
        northY = MathLib.cos(deltaNorth) * MathLib.sin(alphaNorth);
        northZ = MathLib.sin(deltaNorth);
        north = new Vector3D(northX, northY, northZ);
        xDiff = Vector3D.dotProduct(east, topoFrameCartesian.getEast());
        yDiff = Vector3D.dotProduct(north, topoFrameCartesian.getNorth());
        zDiff = Vector3D.dotProduct(zenith, topoFrameCartesian.getZenith());
        northCheck = Vector3D.dotProduct(north, zenith.crossProduct(east));

        // Altitude
        altitude = topoFrameCartesian.getPoint().getAltitude();

        // Assertions
        Assert.assertEquals(1., xDiff, Utils.epsilonTest);
        Assert.assertEquals(1., yDiff, Utils.epsilonTest);
        Assert.assertEquals(1., zDiff, Utils.epsilonTest);
        Assert.assertEquals(1., northCheck, Utils.epsilonTest);
        Assert.assertEquals(0., (expectedAltitude - altitude) / expectedAltitude, 1.0E-12);

    }

    @Before
    public void setUp() throws IOException {
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

            // Build body file
            this.facetBodyRadius = 10E3;
            final String spherBodyObjPath = "src" + File.separator + "test" + File.separator
                    + "resources" + File.separator + "mnt" + File.separator + "SphericalBody.obj";
            final String modelFile = System.getProperty("user.dir") + File.separator + spherBodyObjPath;
            writeBodyFile(modelFile, 51, 100, this.facetBodyRadius / 1E3, 0.);

            // Retrieve body
            final PVCoordinatesProvider pvCoordinates = new PVCoordinatesProvider(){
                /** Serializable UID. */
                private static final long serialVersionUID = 5434704774161087476L;

                @Override
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                    throws PatriusException {
                    return PVCoordinates.ZERO;
                }

                /** {@inheritDoc} */
                @Override
                public Frame getNativeFrame(final AbsoluteDate date, final Frame frame)
                    throws PatriusException {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }
            };

            final CelestialBody celestialBody = new UserCelestialBody("My body", pvCoordinates, 0,
                IAUPoleFactory.getIAUPole(null), FramesFactory.getGCRF(), null);
            this.earthFacetShape = new FacetBodyShape("My body", celestialBody.getRotatingFrame(IAUPoleModelType.TRUE),
                EllipsoidType.INNER_SPHERE, new ObjMeshLoader(modelFile));

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @Test
    public void testVisibilityCircle() throws PatriusException {

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
    public void serialTest() throws PatriusException {

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
            frameEq(f, frameRecover);
        }
    }

    private static void frameEq(final TopocentricFrame f1, final TopocentricFrame f2) {
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
        this.earthFacetShape = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * Build spherical model and write it in file
     * 
     * @param modelFile output model file name
     * @param latitudeNumber number of latitude points (should be odd)
     * @param longitudeNumber number longitude points
     * @param radius body radius (km)
     * @param flattening the flattening of the sphere
     */
    private static void writeBodyFile(final String modelFile, final int latitudeNumber,
                                      final int longitudeNumber, final double radius, final double flattening)
        throws IOException {
        // Initialization, open resources
        final FileOutputStream fileOutputStream = new FileOutputStream(modelFile);
        final OutputStreamWriter fileWriter = new OutputStreamWriter(fileOutputStream, Charset.forName("UTF-8")
            .newEncoder());
        final PrintWriter printWriter = new PrintWriter(fileWriter);

        // Build body
        final int latitudeNumber2 = (latitudeNumber - 1) / 2 - 1;
        final int numberPoints = (2 * latitudeNumber2 + 1) * longitudeNumber + 2;

        // Points

        // South pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., -radius * (1 - flattening)));

        // Regular points excluding poles
        for (int i = -latitudeNumber2; i <= latitudeNumber2; i++) {
            final double latitude = (double) i / (latitudeNumber2 + 1) * MathLib.PI / 2.;
            for (int j = 0; j < longitudeNumber; j++) {
                final double longitude = (double) j / longitudeNumber * 2. * MathLib.PI;
                final double coslat = MathLib.cos(latitude);
                final double sinlat = MathLib.sin(latitude);
                final double coslon = MathLib.cos(longitude);
                final double sinlon = MathLib.sin(longitude);
                final Vector3D pv = new Vector3D(coslat * coslon, coslat * sinlon, sinlat * (1 - flattening))
                    .scalarMultiply(radius);
                printWriter
                    .println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", pv.getX(), pv.getY(), pv.getZ()));
            }
            }

        // North pole
        printWriter.println(String.format(Locale.US, "v %20.15f%20.15f%20.15f", 0., 0., radius * (1 - flattening)));

        // Triangles

        // South pole
            for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, j + 3, j + 2));
            }
        printWriter.println(String.format(Locale.US, "f %10d%10d%10d", 1, 2, longitudeNumber + 1));

        // Regular points excluding poles
        for (int i = 0; i < latitudeNumber - 3; i++) {
            for (int j = 0; j < longitudeNumber - 1; j++) {
                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + j, i
                        * longitudeNumber + 2 + j + 1, (i + 1) * longitudeNumber + 2 + j));

                printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + j + 1, (i + 1)
                        * longitudeNumber + 2 + j + 1, (i + 1)
                        * longitudeNumber + 2 + j));
            }
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2 + longitudeNumber
                    - 1, i * longitudeNumber + 2, (i + 1) * longitudeNumber + 2 + longitudeNumber - 1));

            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", i * longitudeNumber + 2, (i + 1)
                    * longitudeNumber + 2, (i + 1)
                    * longitudeNumber + 2 + longitudeNumber - 1));
        }

        // North pole
        for (int j = 0; j < longitudeNumber - 1; j++) {
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints, numberPoints - j - 2,
                numberPoints - j - 1));
        }
            printWriter.println(String.format(Locale.US, "f %10d%10d%10d", numberPoints,
            numberPoints - 1, numberPoints - longitudeNumber));

        // Close resources
        printWriter.close();
        fileWriter.close();
        fileOutputStream.close();
    }
}
