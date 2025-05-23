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
* VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite 
 *          de convertir les sorties de VacuumSignalPropagation 
* VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait 
 *          retourner un CelestialBodyFrame 
* VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
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
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.EllipsoidPointTest;
import fr.cnes.sirius.patrius.bodies.IAUPoleFactory;
import fr.cnes.sirius.patrius.bodies.IAUPoleModelType;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.UserCelestialBody;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape;
import fr.cnes.sirius.patrius.bodies.mesh.ObjMeshLoader;
import fr.cnes.sirius.patrius.cnesmerge.frames.TopocentricFrameTest.features;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPV;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPosition;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPV;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPosition;
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
    private CelestialBodyFrame frameITRF2005;

    // Earth spheric ellipsoidal shape
    private OneAxisEllipsoid earthSpheric;

    // Facet body radius
    private double facetBodyRadius;

    // Earth spheric facet-defined shape
    private FacetBodyShape earthFacetShape;

    // Body mu
    private double mu;

    /** Epsilon for distance comparison. */
    private final double distanceEpsilon = Utils.epsilonTest;

    /** Epsilon for angle comparison. */
    private final double angleEpsilon = Utils.epsilonAngle;

    @Test
    public void testZero() {

        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            0., 0., 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "zero");

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

        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            FastMath.PI / 2., 0., 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "north pole");

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
        final EllipsoidPoint point1 = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(45.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame1 = new TopocentricFrame(point1, "lat 45");

        // Second point at latitude -45° and same longitude
        final EllipsoidPoint point2 = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(-45.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame2 = new TopocentricFrame(point2, "lat -45");

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
        final EllipsoidPoint point1 = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(45.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame1 = new TopocentricFrame(point1, "lon 30");
        final BodyPoint p1 = topoFrame1.getFrameOrigin();
        Assert.assertEquals(point1.getLLHCoordinates().getLatitude(), p1.getLLHCoordinates().getLatitude(), 1.0e-15);
        Assert.assertEquals(point1.getLLHCoordinates().getLongitude(), p1.getLLHCoordinates().getLongitude(), 1.0e-15);
        Assert.assertEquals(point1.getLLHCoordinates().getHeight(), p1.getLLHCoordinates().getHeight(), 1.0e-15);

        // Second point at latitude -45° and same longitude
        final EllipsoidPoint point2 = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(45.), MathLib.toRadians(210.), 0., "");
        final TopocentricFrame topoFrame2 = new TopocentricFrame(point2, "lon 210");

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
        final EllipsoidPoint point1 = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(45.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame1 = new TopocentricFrame(point1, "lon 30");

        // Second point at latitude -45° and longitude 210
        final EllipsoidPoint point2 = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(-45.), MathLib.toRadians(210.), 0., "");
        final TopocentricFrame topoFrame2 = new TopocentricFrame(point2, "lon 210");

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
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(45.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "lon 30 lat 45");

        // Point at 800 km over zenith
        final EllipsoidPoint satPoint = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(45.), MathLib.toRadians(30.), 800000., "");

        // Zenith point elevation = 90 deg
        final double site = topoFrame.getElevation(satPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2., site, Utils.epsilonAngle);

        // Zenith point range = defined altitude
        final double range = topoFrame.getRange(satPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(800000., range, 1e-8);
    }

    @Test
    public void testAzimuthEquatorial()
        throws PatriusException {

        // Surface point at latitude 0
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "lon 30 lat 0");

        // Point at infinite, separated by +20 deg in longitude
        // *****************************************************
        EllipsoidPoint infPoint = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(50.), 1000000000., "");

        // Azimuth = pi/2
        double azi = topoFrame.getAzimuth(infPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        double site = topoFrame.getElevation(infPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(
            FastMath.PI
                    / 2.
                    - MathLib.abs(point.getLLHCoordinates().getLongitude()
                            - infPoint.getLLHCoordinates().getLongitude()), site, 1.e-2);

        // Point at infinite, separated by -20 deg in longitude
        // *****************************************************
        infPoint = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(10.), 1000000000., "");

        // Azimuth = pi/2
        azi = topoFrame.getAzimuth(infPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(3 * FastMath.PI / 2., azi, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        site =
            topoFrame.getElevation(infPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(
            FastMath.PI
                    / 2.
                    - MathLib.abs(point.getLLHCoordinates().getLongitude()
                            - infPoint.getLLHCoordinates().getLongitude()), site, 1.e-2);
    }

    @Test
    public void testAzimuthPole()
        throws PatriusException {

        // Surface point at latitude 0
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(89.999), MathLib.toRadians(0.), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "lon 0 lat 90");

        // Point at 30 deg longitude
        // **************************
        EllipsoidPoint satPoint = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(28.), MathLib.toRadians(30.), 800000., "");

        // Azimuth =
        double azi = topoFrame.getAzimuth(satPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI - satPoint.getLLHCoordinates().getLongitude(), azi, 1.e-5);

        // Point at -30 deg longitude
        // ***************************
        satPoint = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(28.), MathLib.toRadians(-30.), 800000., "");

        // Azimuth =
        azi = topoFrame.getAzimuth(satPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI - satPoint.getLLHCoordinates().getLongitude(), azi, 1.e-5);
    }

    @Test
    public void testDoppler()
        throws PatriusException {

        // Surface point at latitude 45, longitude 5
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(45.), MathLib.toRadians(5.), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "lon 5 lat 45");

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
        final EllipsoidPoint satPointGeo = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(30.), MathLib.toRadians(15.), 800000., "");
        final Vector3D satPoint = earthElliptic.buildPoint(satPointGeo.getPosition(), earthElliptic.getBodyFrame(),
            null, "").getPosition();

        // ****************************
        // Test at equatorial position
        // ****************************
        EllipsoidPoint pointElliptic = new EllipsoidPoint(earthElliptic, earthElliptic.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(5.), 0., "");
        TopocentricFrame topoElliptic = new TopocentricFrame(pointElliptic, "elliptic, equatorial lon 5");

        EllipsoidPoint pointSpheric = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(0.), MathLib.toRadians(5.), 0., "");
        TopocentricFrame topoSpheric = new TopocentricFrame(pointSpheric, "spheric, equatorial lon 5");

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
        EllipsoidPoint infPointGeo = new EllipsoidPoint(earthElliptic, earthElliptic.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(-15.), 1000000000., "");
        Vector3D infPoint = infPointGeo.getPosition();

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(3 * FastMath.PI / 2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(
            FastMath.PI
                    / 2.
                    - MathLib.abs(pointSpheric.getLLHCoordinates().getLongitude()
                            - infPointGeo.getLLHCoordinates().getLongitude()), eleElli, 1.e-2);

        // Infinite point separated by +20 deg in longitude
        // *************************************************
        infPointGeo = new EllipsoidPoint(earthElliptic, earthElliptic.getLLHCoordinatesSystem(), MathLib.toRadians(0.),
            MathLib.toRadians(25.), 1000000000., "");
        infPoint = infPointGeo.getPosition();

        // Azimuth = pi/2
        aziElli = topoElliptic.getAzimuth(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI / 2., aziElli, Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        eleElli = topoElliptic.getElevation(infPoint, earthElliptic.getBodyFrame(), this.date);
        Assert.assertEquals(
            FastMath.PI
                    / 2.
                    - MathLib.abs(pointSpheric.getLLHCoordinates().getLongitude()
                            - infPointGeo.getLLHCoordinates().getLongitude()), eleElli, 1.e-2);

        // ************************
        // Test at polar position
        // ************************
        pointSpheric = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(89.999), MathLib.toRadians(0.), 0., "");
        topoSpheric = new TopocentricFrame(pointSpheric, "lon 0 lat 90");

        pointElliptic = new EllipsoidPoint(earthElliptic, earthElliptic.getLLHCoordinatesSystem(),
            MathLib.toRadians(89.999), MathLib.toRadians(0.), 0., "");
        topoElliptic = new TopocentricFrame(pointElliptic, "lon 0 lat 90");

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
        pointSpheric = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(60), MathLib.toRadians(30.), 0., "");
        topoSpheric = new TopocentricFrame(pointSpheric, "lon 30 lat 60");

        pointElliptic = new EllipsoidPoint(earthElliptic, earthElliptic.getLLHCoordinatesSystem(),
            MathLib.toRadians(60), MathLib.toRadians(30.), 0., "");
        topoElliptic = new TopocentricFrame(pointElliptic, "lon 30 lat 60");

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
    public void testTopoFrameAxis() throws PatriusException {

        // #1: Test the axis without specifying a zenith
        final EllipsoidPoint ellipsoidPoint = new EllipsoidPoint(this.earthSpheric,
            this.earthSpheric.getLLHCoordinatesSystem(), MathLib.toRadians(45.), MathLib.toRadians(30.), 0., "");
        TopocentricFrame topoFrame = new TopocentricFrame(ellipsoidPoint, "topoFrame");

        Assert.assertEquals(-MathUtils.HALF_PI, topoFrame.getOrientation(), 0.);

        Assert.assertNotNull(topoFrame.getTransformProvider().getTransform(AbsoluteDate.J2000_EPOCH).getAngular()
            .getRotationAcceleration()); // Quick test to cover FA-124

        // Assertions by non regression
        Assert.assertEquals(new Vector3D(-0.5, 0.8660254037844387, 0.), topoFrame.getEast());
        Assert.assertEquals(new Vector3D(-0.6123724356957945, -0.35355339059327373, 0.7071067811865478),
            topoFrame.getNorth());
        Assert.assertEquals(new Vector3D(0.6123724356957947, 0.35355339059327384, 0.7071067811865475),
            topoFrame.getZenith());
        Assert.assertEquals(topoFrame.getZenith().negate(), topoFrame.getNadir());

        // #: Test the axis by specifying a zenith
        Vector3D zenith = new Vector3D(0., -1., 0.);
        topoFrame = new TopocentricFrame(ellipsoidPoint, zenith, "topoFrame");

        Assert.assertEquals(-MathUtils.HALF_PI, topoFrame.getOrientation(), 0.);

        // Assertions by non regression
        Assert.assertEquals(new Vector3D(1., 0., 0.), topoFrame.getEast());
        Assert.assertEquals(new Vector3D(0., 0., 1.), topoFrame.getNorth());
        Assert.assertEquals(zenith, topoFrame.getZenith());
        Assert.assertEquals(zenith.negate(), topoFrame.getNadir());

        // #3: Test the axis by specifying an other zenith
        zenith = new Vector3D(1., 1., 1.).normalize();
        topoFrame = new TopocentricFrame(ellipsoidPoint, zenith, "topoFrame");

        // Assertions by non regression
        Assert.assertEquals(new Vector3D(-0.7071067811865476, 0.7071067811865476, 0.), topoFrame.getEast());
        Assert.assertEquals(new Vector3D(-0.4082482904638631, -0.4082482904638631, 0.8164965809277261),
            topoFrame.getNorth());
        Assert.assertEquals(zenith, topoFrame.getZenith());
        Assert.assertEquals(zenith.negate(), topoFrame.getNadir());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromPositionToTopocentric(Vector3D, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#transformFromTopocentricToPosition(TopocentricPosition)}
     * @testedMethod {@link TopocentricFrame#transformFromPositionToCardan(Vector3D, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#transformFromCardanToPosition(CardanMountPosition)}
     * 
     * @description we test the transformations analytically
     * 
     * @input The inputs are the following :
     *        <p>
     *        CardanMounting cardanCoord = (PI/4, acos(sqrt(2/3)), sqrt(3))
     *        </p>
     *        <p>
     *        TopocentricCoordinates topoCoord = (acos(sqrt(2/3)), PI/4, sqrt(3))
     *        </p>
     * 
     * @output set of pv coordinates, Cardan mount, set of pv coordinates, Topocentric coordinates
     * 
     * @testPassCriteria In both cases, the obtained set of pv coordinates should be :
     *                   <p>
     *                   Vector3D position = (1,1,1)
     *                   </p>
     *                   <p>
     *                   and the inputs when the opposite transformation is permformed.
     *                   </p>
     *                   <p>
     *                   with 1e-7 as epsilon on angles and 1e-12 as epsilon on distances.
     *                   </p>
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    @Test
    public void testTransform() throws PatriusException {
        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., "north topocentric frame");

        Vector3D position;
        PVCoordinates pv;

        /*
         * Cardan mounting
         */

        // POSITION

        // from Cardan mounting set to cartesian coordinates set (position only)
        final double cardanX = MathLib.toRadians(45.);
        final double cardanY = MathLib.acos(MathLib.sqrt(2. / 3.));
        final double cardanR = MathLib.sqrt(3.);
        final CardanMountPosition cardanCoordPosition = new CardanMountPosition(cardanX, cardanY, cardanR);
        position = topoNorth.transformFromCardanToPosition(cardanCoordPosition);

        Assert.assertEquals(1., position.getX(), this.distanceEpsilon);
        Assert.assertEquals(-1., position.getY(), this.distanceEpsilon);
        Assert.assertEquals(1., position.getZ(), this.distanceEpsilon);

        // transformation done component to component
        Assert.assertEquals(cardanX, topoNorth.getXangleCardan(position, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(cardanY, topoNorth.getYangleCardan(position, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(cardanR, topoNorth.getRange(position, topoNorth, this.date), this.distanceEpsilon);

        // from position coordinates set (position only) to Cardan mounting set
        final CardanMountPosition cardanCoordResult =
            topoNorth.transformFromPositionToCardan(position, topoNorth, this.date);

        Assert.assertEquals(cardanX, cardanCoordResult.getXangle(), this.angleEpsilon);
        Assert.assertEquals(cardanY, cardanCoordResult.getYangle(), this.angleEpsilon);
        Assert.assertEquals(cardanR, cardanCoordResult.getRange(), this.distanceEpsilon);

        // Try to transform a Cartesian position coordinates into Cardan mounting with a low satellite passage
        // (= X axis) (should fail)
        try {
            topoNorth.transformFromPositionToCardan(Vector3D.PLUS_I, topoNorth, this.date);
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // PV

        // from Cardan mounting set to cartesian coordinates set (PV)
        final double cardanXRate = MathLib.toRadians(5.);
        final double cardanYRate = MathLib.acos(MathLib.sqrt(1. / 3.));
        final double cardanRRate = MathLib.sqrt(1.5);
        final CardanMountPV cardanCoordPV =
            new CardanMountPV(cardanX, cardanY, cardanR, cardanXRate, cardanYRate, cardanRRate);
        pv = topoNorth.transformFromCardanToPV(cardanCoordPV);

        // transformation done component to component
        Assert.assertEquals(cardanX, topoNorth.getXangleCardan(pv.getPosition(), topoNorth, this.date),
            this.angleEpsilon);
        Assert.assertEquals(cardanY, topoNorth.getYangleCardan(pv.getPosition(), topoNorth, this.date),
            this.angleEpsilon);
        Assert.assertEquals(cardanR, topoNorth.getRange(pv.getPosition(), topoNorth, this.date), this.distanceEpsilon);
        Assert.assertEquals(cardanXRate, topoNorth.getXangleCardanRate(pv, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(cardanYRate, topoNorth.getYangleCardanRate(pv, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(cardanRRate, topoNorth.getRangeRate(pv, topoNorth, this.date), this.distanceEpsilon);

        // from position coordinates set (position only) to Cardan mounting set
        final CardanMountPV cardanCoordPVResult = topoNorth.transformFromPVToCardan(pv, topoNorth, this.date);

        Assert.assertEquals(cardanX, cardanCoordPVResult.getXangle(), this.angleEpsilon);
        Assert.assertEquals(cardanY, cardanCoordPVResult.getYangle(), this.angleEpsilon);
        Assert.assertEquals(cardanR, cardanCoordPVResult.getRange(), this.distanceEpsilon);
        Assert.assertEquals(cardanXRate, cardanCoordPVResult.getXangleRate(), this.angleEpsilon);
        Assert.assertEquals(cardanYRate, cardanCoordPVResult.getYangleRate(), this.angleEpsilon);
        Assert.assertEquals(cardanRRate, cardanCoordPVResult.getRangeRate(), this.distanceEpsilon);

        // Try to transform a Cartesian position coordinates into Cardan mounting with a low satellite passage
        // (= X axis) (should fail)
        try {
            topoNorth.transformFromPVToCardan(new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO), topoNorth, this.date);
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }

        /*
         * Topocentric coordinates
         */

        // POSITION

        // from topocentric coordinates to cartesian coordinates (position only)
        final double elevation = MathLib.acos(MathLib.sqrt(2. / 3.));
        final double azimuth = MathLib.toRadians(315.);
        final double range = MathLib.sqrt(3.);
        final TopocentricPosition topoCoordPosition = new TopocentricPosition(elevation, azimuth, range);
        position = topoNorth.transformFromTopocentricToPosition(topoCoordPosition);

        Assert.assertEquals(1., position.getX(), this.distanceEpsilon);
        Assert.assertEquals(1., position.getY(), this.distanceEpsilon);
        Assert.assertEquals(1., position.getZ(), this.distanceEpsilon);

        // transformation done component to component
        Assert.assertEquals(elevation, topoNorth.getElevation(position, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(azimuth, topoNorth.getAzimuth(position, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(range, topoNorth.getRange(position, topoNorth, this.date), this.distanceEpsilon);

        // from Cartesian position coordinates (position only) to topocentric coordinates
        final TopocentricPosition topoCoordResult =
            topoNorth.transformFromPositionToTopocentric(position, topoNorth, this.date);

        Assert.assertEquals(elevation, topoCoordResult.getElevation(), this.angleEpsilon);
        Assert.assertEquals(azimuth, topoCoordResult.getAzimuth(), this.angleEpsilon);
        Assert.assertEquals(range, topoCoordResult.getRange(), this.distanceEpsilon);

        // Try to transform a Cartesian position coordinates into topocentric coordinates with a satellite passage near
        // to the zenith (= Z axis) (should fail)
        try {
            topoNorth.transformFromPositionToTopocentric(Vector3D.PLUS_K, topoNorth, this.date);
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // PV

        // from topocentric coordinates to cartesian coordinates (PV)
        final double elevationRate = MathLib.toRadians(4.);
        final double azimuthRate = MathLib.acos(MathLib.sqrt(1. / 4.));
        final double rangeRate = MathLib.sqrt(1.4);
        final TopocentricPV topoCoordPV = new TopocentricPV(elevation, azimuth, range, elevationRate, azimuthRate,
            rangeRate);
        pv = topoNorth.transformFromTopocentricToPV(topoCoordPV);

        // transformation done component to component
        Assert.assertEquals(elevation, topoNorth.getElevation(pv.getPosition(), topoNorth, this.date),
            this.angleEpsilon);
        Assert.assertEquals(azimuth, topoNorth.getAzimuth(pv.getPosition(), topoNorth, this.date),
            this.angleEpsilon);
        Assert.assertEquals(range, topoNorth.getRange(pv.getPosition(), topoNorth, this.date), this.distanceEpsilon);
        Assert.assertEquals(elevationRate, topoNorth.getElevationRate(pv, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(azimuthRate, topoNorth.getAzimuthRate(pv, topoNorth, this.date), this.angleEpsilon);
        Assert.assertEquals(rangeRate, topoNorth.getRangeRate(pv, topoNorth, this.date), this.distanceEpsilon);

        // from Cartesian position coordinates (PV) to topocentric coordinates
        final TopocentricPV topoCoordPVResult = topoNorth.transformFromPVToTopocentric(pv, topoNorth, this.date);

        Assert.assertEquals(elevation, topoCoordPVResult.getElevation(), this.angleEpsilon);
        Assert.assertEquals(azimuth, topoCoordPVResult.getAzimuth(), this.angleEpsilon);
        Assert.assertEquals(range, topoCoordPVResult.getRange(), this.distanceEpsilon);
        Assert.assertEquals(elevationRate, topoCoordPVResult.getElevationRate(), this.angleEpsilon);
        Assert.assertEquals(azimuthRate, topoCoordPVResult.getAzimuthRate(), this.angleEpsilon);
        Assert.assertEquals(rangeRate, topoCoordPVResult.getRangeRate(), this.distanceEpsilon);

        // Try to transform a Cartesian position coordinates into topocentric coordinates with a satellite passage near
        // to the zenith (= Z axis) (should fail)
        try {
            topoNorth.transformFromPVToTopocentric(new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO), topoNorth,
                this.date);
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCardanDerivatives() throws PatriusException {

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., "north topocentric frame");

        /*
         * Cardan mounting
         */

        // from Cardan mounting set to cartesian coordinates set (position only)
        final double cardanX = MathLib.toRadians(45.);
        final double cardanY = MathLib.acos(MathLib.sqrt(2. / 3.));
        final double cardanR = MathLib.sqrt(3.);
        final CardanMountPosition cardanCoordPosition = new CardanMountPosition(cardanX, cardanY, cardanR);
        final Vector3D position = topoNorth.transformFromCardanToPosition(cardanCoordPosition);

        final Frame gcrf = FramesFactory.getGCRF();
        final Transform t = topoNorth.getTransformTo(gcrf, this.date);
        final Vector3D positionGCRF = t.transformPosition(position);

        final double dH = 0.1;
        final double dH2 = dH * 2.;

        // IN TOPO FRAME
        Vector3D dXangle = topoNorth.getDXangleCardan(position, topoNorth, this.date);

        double xAngleMinusdX = topoNorth.getXangleCardan(
            new Vector3D(position.getX() - dH, position.getY(), position.getZ()), topoNorth, this.date);
        double xAnglePlusdX = topoNorth.getXangleCardan(
            new Vector3D(position.getX() + dH, position.getY(), position.getZ()), topoNorth, this.date);
        double numValX = (xAnglePlusdX - xAngleMinusdX) / dH2;

        double xAngleMinusdY = topoNorth.getXangleCardan(
            new Vector3D(position.getX(), position.getY() - dH, position.getZ()), topoNorth, this.date);
        double xAnglePlusdY = topoNorth.getXangleCardan(
            new Vector3D(position.getX(), position.getY() + dH, position.getZ()), topoNorth, this.date);
        double numValY = (xAnglePlusdY - xAngleMinusdY) / dH2;

        double xAngleMinusdZ = topoNorth.getXangleCardan(new Vector3D(position.getX(), position.getY(),
            position.getZ() - dH), topoNorth, this.date);
        double xAnglePlusdZ = topoNorth.getXangleCardan(new Vector3D(position.getX(), position.getY(),
            position.getZ() + dH), topoNorth, this.date);
        double numValZ = (xAnglePlusdZ - xAngleMinusdZ) / dH2;

        Assert.assertEquals(dXangle.getX(), numValX, 0.); // Special case : expect 0 (do not compute rel diff)
        Assert.assertEquals(0., MathLib.abs(dXangle.getY() - numValY) / numValY, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dXangle.getZ() - numValZ) / numValZ, 2e-3);

        Vector3D dYangle = topoNorth.getDYangleCardan(position, topoNorth, this.date);

        double yAngleMinusdX = topoNorth.getYangleCardan(
            new Vector3D(position.getX() - dH, position.getY(), position.getZ()), topoNorth, this.date);
        double yAnglePlusdX = topoNorth.getYangleCardan(
            new Vector3D(position.getX() + dH, position.getY(), position.getZ()), topoNorth, this.date);
        numValX = (yAnglePlusdX - yAngleMinusdX) / dH2;

        double yAngleMinusdY = topoNorth.getYangleCardan(
            new Vector3D(position.getX(), position.getY() - dH, position.getZ()), topoNorth, this.date);
        double yAnglePlusdY = topoNorth.getYangleCardan(
            new Vector3D(position.getX(), position.getY() + dH, position.getZ()), topoNorth, this.date);
        numValY = (yAnglePlusdY - yAngleMinusdY) / dH2;

        double yAngleMinusdZ = topoNorth.getYangleCardan(new Vector3D(position.getX(), position.getY(),
            position.getZ() - dH), topoNorth, this.date);
        double yAnglePlusdZ = topoNorth.getYangleCardan(new Vector3D(position.getX(), position.getY(),
            position.getZ() + dH), topoNorth, this.date);
        numValZ = (yAnglePlusdZ - yAngleMinusdZ) / dH2;

        Assert.assertEquals(0., MathLib.abs(dYangle.getX() - numValX) / numValX, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dYangle.getY() - numValY) / numValY, 2.1e-3);
        Assert.assertEquals(0., MathLib.abs(dYangle.getZ() - numValZ) / numValZ, 2.1e-3);

        // IN OTHER FRAME (GCRF)
        dXangle = topoNorth.getDXangleCardan(positionGCRF, gcrf, this.date);

        xAngleMinusdX = topoNorth.getXangleCardan(new Vector3D(positionGCRF.getX() - dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        xAnglePlusdX = topoNorth.getXangleCardan(new Vector3D(positionGCRF.getX() + dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        numValX = (xAnglePlusdX - xAngleMinusdX) / dH2;

        xAngleMinusdY = topoNorth.getXangleCardan(new Vector3D(positionGCRF.getX(), positionGCRF.getY() - dH,
            positionGCRF.getZ()), gcrf, this.date);
        xAnglePlusdY = topoNorth.getXangleCardan(new Vector3D(positionGCRF.getX(), positionGCRF.getY() + dH,
            positionGCRF.getZ()), gcrf, this.date);
        numValY = (xAnglePlusdY - xAngleMinusdY) / dH2;

        xAngleMinusdZ = topoNorth.getXangleCardan(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() - dH), gcrf, this.date);
        xAnglePlusdZ = topoNorth.getXangleCardan(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() + dH), gcrf, this.date);
        numValZ = (xAnglePlusdZ - xAngleMinusdZ) / dH2;

        Assert.assertEquals(0., MathLib.abs(dXangle.getX() - numValX) / numValX, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dXangle.getY() - numValY) / numValY, 3e-3);
        Assert.assertEquals(0., MathLib.abs(dXangle.getZ() - numValZ) / numValZ, 2e-3);

        dYangle = topoNorth.getDYangleCardan(positionGCRF, gcrf, this.date);

        yAngleMinusdX = topoNorth.getYangleCardan(new Vector3D(positionGCRF.getX() - dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        yAnglePlusdX = topoNorth.getYangleCardan(new Vector3D(positionGCRF.getX() + dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        numValX = (yAnglePlusdX - yAngleMinusdX) / dH2;

        yAngleMinusdY = topoNorth.getYangleCardan(new Vector3D(positionGCRF.getX(), positionGCRF.getY() - dH,
            positionGCRF.getZ()), gcrf, this.date);
        yAnglePlusdY = topoNorth.getYangleCardan(new Vector3D(positionGCRF.getX(), positionGCRF.getY() + dH,
            positionGCRF.getZ()), gcrf, this.date);
        numValY = (yAnglePlusdY - yAngleMinusdY) / dH2;

        yAngleMinusdZ = topoNorth.getYangleCardan(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() - dH), gcrf, this.date);
        yAnglePlusdZ = topoNorth.getYangleCardan(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() + dH), gcrf, this.date);
        numValZ = (yAnglePlusdZ - yAngleMinusdZ) / dH2;

        Assert.assertEquals(0., MathLib.abs(dYangle.getX() - numValX) / numValX, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dYangle.getY() - numValY) / numValY, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dYangle.getZ() - numValZ) / numValZ, 3e-3);
    }

    @Test
    public void testElevAzimDerivatives() throws PatriusException {

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., "north topocentric frame");

        /*
         * Topocentric coordinates
         */

        // from topocentric coordinates to cartesian coordinates (position only)
        final double elevation = MathLib.acos(MathLib.sqrt(2. / 3.));
        final double azimuth = MathLib.toRadians(315.);
        final double range = MathLib.sqrt(3.);
        final TopocentricPosition topoCoordPosition = new TopocentricPosition(elevation, azimuth, range);
        final Vector3D position = topoNorth.transformFromTopocentricToPosition(topoCoordPosition);

        final Frame gcrf = FramesFactory.getGCRF();
        final Transform t = topoNorth.getTransformTo(gcrf, this.date);
        final Vector3D positionGCRF = t.transformPosition(position);

        final double dH = 0.1;
        final double dH2 = dH * 2.;

        // IN TOPO FRAME
        Vector3D dElevation = topoNorth.getDElevation(position, topoNorth, this.date);

        double elevMinusdX = topoNorth.getElevation(
            new Vector3D(position.getX() - dH, position.getY(), position.getZ()), topoNorth, this.date);
        double elevPlusdX = topoNorth.getElevation(
            new Vector3D(position.getX() + dH, position.getY(), position.getZ()), topoNorth, this.date);
        double numValX = (elevPlusdX - elevMinusdX) / dH2;

        double elevMinusdY = topoNorth.getElevation(
            new Vector3D(position.getX(), position.getY() - dH, position.getZ()), topoNorth, this.date);
        double elevPlusdY = topoNorth.getElevation(
            new Vector3D(position.getX(), position.getY() + dH, position.getZ()), topoNorth, this.date);
        double numValY = (elevPlusdY - elevMinusdY) / dH2;

        double elevMinusdZ = topoNorth.getElevation(new Vector3D(position.getX(), position.getY(),
            position.getZ() - dH), topoNorth, this.date);
        double elevPlusdZ = topoNorth.getElevation(new Vector3D(position.getX(), position.getY(),
            position.getZ() + dH), topoNorth, this.date);
        double numValZ = (elevPlusdZ - elevMinusdZ) / dH2;

        Assert.assertEquals(0., MathLib.abs(dElevation.getX() - numValX) / numValX, 2.1e-3);
        Assert.assertEquals(0., MathLib.abs(dElevation.getY() - numValY) / numValY, 2.1e-3);
        Assert.assertEquals(0., MathLib.abs(dElevation.getZ() - numValZ) / numValZ, 2e-3);

        Vector3D dAzimuth = topoNorth.getDAzimuth(position, topoNorth, this.date);

        double azimMinusdX = topoNorth.getAzimuth(
            new Vector3D(position.getX() - dH, position.getY(), position.getZ()), topoNorth, this.date);
        double azimPlusdX = topoNorth.getAzimuth(
            new Vector3D(position.getX() + dH, position.getY(), position.getZ()), topoNorth, this.date);
        numValX = (azimPlusdX - azimMinusdX) / dH2;

        double azimMinusdY = topoNorth.getAzimuth(
            new Vector3D(position.getX(), position.getY() - dH, position.getZ()), topoNorth, this.date);
        double azimPlusdY = topoNorth.getAzimuth(
            new Vector3D(position.getX(), position.getY() + dH, position.getZ()), topoNorth, this.date);
        numValY = (azimPlusdY - azimMinusdY) / dH2;

        double azimMinusdZ = topoNorth.getAzimuth(new Vector3D(position.getX(), position.getY(),
            position.getZ() - dH), topoNorth, this.date);
        double azimPlusdZ = topoNorth.getAzimuth(new Vector3D(position.getX(), position.getY(),
            position.getZ() + dH), topoNorth, this.date);
        numValZ = (azimPlusdZ - azimMinusdZ) / dH2;

        Assert.assertEquals(0., MathLib.abs(dAzimuth.getX() - numValX) / numValX, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dAzimuth.getY() - numValY) / numValY, 2e-3);
        Assert.assertEquals(dAzimuth.getZ(), numValZ, 0.); // Special case : expect 0 (do not compute rel diff)

        // IN OTHER FRAME (GCRF)
        dElevation = topoNorth.getDElevation(positionGCRF, gcrf, this.date);

        elevMinusdX = topoNorth.getElevation(new Vector3D(positionGCRF.getX() - dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        elevPlusdX = topoNorth.getElevation(new Vector3D(positionGCRF.getX() + dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        numValX = (elevPlusdX - elevMinusdX) / dH2;

        elevMinusdY = topoNorth.getElevation(new Vector3D(positionGCRF.getX(), positionGCRF.getY() - dH,
            positionGCRF.getZ()), gcrf, this.date);
        elevPlusdY = topoNorth.getElevation(new Vector3D(positionGCRF.getX(), positionGCRF.getY() + dH,
            positionGCRF.getZ()), gcrf, this.date);
        numValY = (elevPlusdY - elevMinusdY) / dH2;

        elevMinusdZ = topoNorth.getElevation(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() - dH), gcrf, this.date);
        elevPlusdZ = topoNorth.getElevation(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() + dH), gcrf, this.date);
        numValZ = (elevPlusdZ - elevMinusdZ) / dH2;

        Assert.assertEquals(0., MathLib.abs(dElevation.getX() - numValX) / numValX, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dElevation.getY() - numValY) / numValY, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dElevation.getZ() - numValZ) / numValZ, 3.4e-3);

        dAzimuth = topoNorth.getDAzimuth(positionGCRF, gcrf, this.date);

        azimMinusdX = topoNorth.getAzimuth(new Vector3D(positionGCRF.getX() - dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        azimPlusdX = topoNorth.getAzimuth(new Vector3D(positionGCRF.getX() + dH, positionGCRF.getY(),
            positionGCRF.getZ()), gcrf, this.date);
        numValX = (azimPlusdX - azimMinusdX) / dH2;

        azimMinusdY = topoNorth.getAzimuth(new Vector3D(positionGCRF.getX(), positionGCRF.getY() - dH,
            positionGCRF.getZ()), gcrf, this.date);
        azimPlusdY = topoNorth.getAzimuth(new Vector3D(positionGCRF.getX(), positionGCRF.getY() + dH,
            positionGCRF.getZ()), gcrf, this.date);
        numValY = (azimPlusdY - azimMinusdY) / dH2;

        azimMinusdZ = topoNorth.getAzimuth(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() - dH), gcrf, this.date);
        azimPlusdZ = topoNorth.getAzimuth(
            new Vector3D(positionGCRF.getX(), positionGCRF.getY(), positionGCRF.getZ() + dH), gcrf, this.date);
        numValZ = (azimPlusdZ - azimMinusdZ) / dH2;

        Assert.assertEquals(0., MathLib.abs(dAzimuth.getX() - numValX) / numValX, 2e-3);
        Assert.assertEquals(0., MathLib.abs(dAzimuth.getY() - numValY) / numValY, 3e-3);
        Assert.assertEquals(0., MathLib.abs(dAzimuth.getZ() - numValZ) / numValZ, 2e-3);
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
                public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                    return PVCoordinates.ZERO;
                }

                /** {@inheritDoc} */
                @Override
                public Frame getNativeFrame(final AbsoluteDate date)
                    throws PatriusException {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }
            };

            final CelestialBody celestialBody = new UserCelestialBody("My body", pvCoordinates, 0,
                IAUPoleFactory.getIAUPole(null), FramesFactory.getGCRF(), null);
            this.earthFacetShape = new FacetBodyShape("My body", celestialBody.getRotatingFrame(IAUPoleModelType.TRUE),
                new ObjMeshLoader(modelFile));

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @Test
    public void testVisibilityCircle() throws PatriusException {

        // a few random from International Laser Ranging Service
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());

        final TopocentricFrame[] ilrs = {
            new TopocentricFrame(new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(52.3800),
                MathLib.toRadians(3.0649), 133.745, ""), "Potsdam"),
            new TopocentricFrame(new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
                MathLib.toRadians(36.46273), MathLib.toRadians(-6.20619), 64.0, ""), "San Fernando"),
            new TopocentricFrame(new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), MathLib.toRadians(35.5331),
                MathLib.toRadians(24.0705), 157.0, ""), "Chania")
        };

        final PolynomialFunction distanceModel =
            new PolynomialFunction(new double[] { 7.0892e+05, 3.1913, -8.2181e-07, 1.4033e-13 });
        for (final TopocentricFrame station : ilrs) {
            for (double altitude = 500000; altitude < 2000000; altitude += 100000) {
                for (double azimuth = 0; azimuth < 2 * FastMath.PI; azimuth += 0.05) {
                    final BodyPoint p = station.computeLimitVisibilityPoint(
                        Constants.WGS84_EARTH_EQUATORIAL_RADIUS + altitude, azimuth, MathLib.toRadians(5.0));
                    final double d = station.getRange(p.getPosition(), earth.getBodyFrame(), AbsoluteDate.J2000_EPOCH);
                    Assert.assertEquals(distanceModel.value(altitude), d, 40000.0);
                }
            }
        }
    }

    @Test
    public void serialTest() throws PatriusException {

        // a few random from International Laser Ranging Service
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());

        final TopocentricFrame[] frames = {
            new TopocentricFrame(new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
                MathLib.toRadians(52.3800), MathLib.toRadians(3.0649), 133.745, ""), "Potsdam"),
            new TopocentricFrame(new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
                MathLib.toRadians(36.46273), MathLib.toRadians(-6.20619), 64.0, ""), "San Fernando"),
            new TopocentricFrame(new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
                MathLib.toRadians(35.5331), MathLib.toRadians(24.0705), 157.0, ""), "Chania") };

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
        EllipsoidPointTest.assertEqualsBodyPoint(f1.getFrameOrigin(), f2.getFrameOrigin());
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
