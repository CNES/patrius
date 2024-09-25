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
 * @history creation 21/06/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:185:11/04/2014:the getLine method of GroundVelocityDirection has been modified
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:489:12/01/2016:Physical Validation
 * VERSION::DM:557:15/02/2016: class rename
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.PolarMotion;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModel;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.transformations.TIRFProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the ground velocity direction.
 *              </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id: GroundVelocityDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.2
 */
public class GroundVelocityDirectionTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Position direction
         * 
         * @featureDescription ground velocity direction.
         * 
         * @coveredRequirements DV-ATT_390
         */
        GROUND_VELOCITY_DIRECTION,

        /**
         * @featureTitle Physical case
         * 
         * @featureDescription Different cases to physically validate the direction
         * 
         * @coveredRequirements DV-ATT_390
         */
        PHYSICAL_CASES
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = 1e-10;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(GroundVelocityDirectionTest.class.getSimpleName(), "Ground velocity direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PHYSICAL_CASES}
     * 
     * @testedMethod {@link GroundVelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Equatorial circular orbit with a fixed spherical earth.
     *              Expected velocity Vt = w Requa.
     * 
     * @input EquinoctialOrbit in GCRF
     * @input OneAxisEllipsoid with GCRF body frame
     * 
     * @output Vector3D Normalized velocity on ground
     * 
     * @testPassCriteria Normalized velocity can be expressed as a cos and sin of angle between GCRF X axis and Position
     *                   at t.
     *                   Distance from expected to result vector lower than comparisonEpsilon
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testCircEquaSpherFixedEarth() {
        try {
            // gcrf
            final CelestialBodyFrame gcrf = FramesFactory.getGCRF();

            // orbit creation
            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new EquinoctialOrbit(7500000, 0, 0, 0, 0, 0, PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = 0;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, gcrf);

            // ground velocity direction
            final IDirection earthCenterDir = new EarthCenterDirection();
            final IDirection groundVeloDir = new GroundVelocityDirection(earth, earthCenterDir);

            double teta = 0;
            final double w = orbit.getKeplerianMeanMotion();
            for (int t = 0; t < 10; t++) {
                teta = w * t;

                // expected direction vector
                final Vector3D expected = new Vector3D(-MathLib.sin(teta), MathLib.cos(teta), 0);

                // computed direction vector
                final Vector3D result = groundVeloDir.getVector(orbit, date.shiftedBy(t), gcrf);

                // check direction vector
                Assert.assertEquals(0., result.distance(expected), this.comparisonEpsilon);
            }
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PHYSICAL_CASES}
     * 
     * @testedMethod {@link GroundVelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Equatorial circular orbit with a spherical earth with natural rotation rate. Vt = w Requa - wearth.
     * 
     * @input EquinoctialOrbit in GCRF
     * @input OneAxisEllipsoid with ITRF body frame
     * @input FramesConfiguration with nothing to keep Z-axis from GCRF and ITRF aligned
     * 
     * @output Vector3D Normalized velocity on ground
     * 
     * @testPassCriteria Normalized velocity can be expressed as a cos and sin of angle between GCRF X axis and Position
     *                   at t.
     *                   Distance from expected to result vector lower than comparisonEpsilon
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testCircEquaSpherEarth() {
        try {

            // Configurations builder
            final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

            // Tides and libration
            final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
            final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;
            // Polar Motion
            final PolarMotion defaultPolarMotion = new PolarMotion(false, tides, lib, SPrimeModelFactory.NO_SP);
            // Diurnal rotation
            final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(tides, lib);

            // Precession Nutation
            final PrecessionNutation precNut = new PrecessionNutation(false,
                PrecessionNutationModelFactory.NO_PN);

            builder.setDiurnalRotation(defaultDiurnalRotation);
            builder.setPolarMotion(defaultPolarMotion);
            builder.setCIRFPrecessionNutation(precNut);
            builder.setEOPHistory(new NoEOP2000History());

            FramesFactory.setConfiguration(builder.getConfiguration());

            // gcrf - itrf
            final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
            final CelestialBodyFrame itrf = FramesFactory.getITRF();

            // orbit creation
            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new EquinoctialOrbit(7500000, 0, 0, 0, 0, 0, PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = 0;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

            // ground velocity direction
            final IDirection earthCenterDir = new EarthCenterDirection();
            final IDirection groundVeloDir = new GroundVelocityDirection(earth, earthCenterDir);

            double teta = 0;
            final double w = orbit.getKeplerianMeanMotion();
            for (int t = 0; t < 10; t++) {
                teta = w * t;

                // expected direction vector
                final Vector3D expected = new Vector3D(-MathLib.sin(teta), MathLib.cos(teta), 0).normalize();

                // computed direction vector
                final Vector3D result = groundVeloDir.getVector(orbit, date.shiftedBy(t), gcrf);

                // check direction vector
                Assert.assertEquals(0., result.distance(expected), this.comparisonEpsilon);
            }
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PHYSICAL_CASES}
     * 
     * @testedMethod {@link GroundVelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Circular orbit with a spherical fixed earth at apogee. Vt = w Requa
     * 
     * @input CircularOrbit in GCRF with i = 30 deg and alpha = 90 deg.
     * @input OneAxisEllipsoid with GCRF body frame
     * 
     * @output Vector3D Normalized velocity on ground
     * 
     * @testPassCriteria Normalized velocity should be (-1, 0, 0) in GCRF at comparisonEpsilon
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testCircIncFixedSpherEarth() {
        try {

            // gcrf
            final CelestialBodyFrame gcrf = FramesFactory.getGCRF();

            // orbit creation
            final double i = MathLib.toRadians(30);
            final double R = 7500000;

            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new CircularOrbit(R, 0, 0, i, 0, MathLib.toRadians(90),
                PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = 0;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, gcrf);

            // ground velocity direction
            final IDirection earthCenterDir = new EarthCenterDirection();
            final IDirection groundVeloDir = new GroundVelocityDirection(earth, earthCenterDir);

            orbit.getKeplerianMeanMotion();

            final Vector3D expected = new Vector3D(-1, 0, 0);

            // computed direction vector
            final Vector3D result = groundVeloDir.getVector(orbit, date, gcrf);

            // check direction vector
            Assert.assertEquals(0., result.distance(expected), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PHYSICAL_CASES}
     * 
     * @testedMethod {@link GroundVelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Circular orbit with a spherical fixed earth at ascending node. Vt = w Requa
     * 
     * @input CircularOrbit in GCRF with i = 30 deg and alpha = 0 deg.
     * @input OneAxisEllipsoid with GCRF body frame
     * 
     * @output Vector3D Normalized velocity on ground
     * 
     * @testPassCriteria Normalized velocity should be (0, cos(i), sin(i)) in GCRF at comparisonEpsilon
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testCircIncFixedSpherEarthAlpha0() {
        try {
            // gcrf
            final CelestialBodyFrame gcrf = FramesFactory.getGCRF();

            // orbit creation
            final double i = MathLib.toRadians(30);
            final double R = 7500000;

            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new CircularOrbit(R, 0, 0, i, 0, 0,
                PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = 0;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, gcrf);

            // ground velocity direction
            final IDirection earthCenterDir = new EarthCenterDirection();
            final IDirection groundVeloDir = new GroundVelocityDirection(earth, earthCenterDir);

            orbit.getKeplerianMeanMotion();

            final Vector3D expected = new Vector3D(0, MathLib.cos(i), MathLib.sin(i));

            // computed direction vector
            final Vector3D result = groundVeloDir.getVector(orbit, date, gcrf);

            // check direction vector
            Assert.assertEquals(0., result.distance(expected), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PHYSICAL_CASES}
     * 
     * @testedMethod {@link GroundVelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Circular orbit at ascending node with a spherical earth with rotation rate of ITRF .
     * 
     * @input CircularOrbit in GCRF with i = 30 deg and alpha = 0 deg.
     * @input OneAxisEllipsoid with ITRF body frame
     * 
     * @output Vector3D Normalized velocity on ground
     * 
     * @testPassCriteria Expected velocity should be SatVelocity on ground in GCRF minus EarthVelocity on ground in
     *                   GCRF.
     *                   This velocity normalized should be equal to result at comparisonEpsilon
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testCircIncSpherEarthAlpha0() {
        try {
            // Configurations builder
            final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();

            // Tides and libration
            final TidalCorrectionModel tides = TidalCorrectionModelFactory.NO_TIDE;
            final LibrationCorrectionModel lib = LibrationCorrectionModelFactory.NO_LIBRATION;
            // Polar Motion
            final PolarMotion defaultPolarMotion = new PolarMotion(false, tides, lib, SPrimeModelFactory.NO_SP);
            // Diurnal rotation
            final DiurnalRotation defaultDiurnalRotation = new DiurnalRotation(tides, lib);

            // Precession Nutation
            final PrecessionNutation precNut = new PrecessionNutation(false,
                PrecessionNutationModelFactory.NO_PN);

            builder.setDiurnalRotation(defaultDiurnalRotation);
            builder.setPolarMotion(defaultPolarMotion);
            builder.setCIRFPrecessionNutation(precNut);
            builder.setEOPHistory(new NoEOP2000History());

            FramesFactory.setConfiguration(builder.getConfiguration());

            // gcrf - itrf
            final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
            final CelestialBodyFrame itrf = FramesFactory.getITRF();

            // orbit creation
            final double i = MathLib.toRadians(30);
            final double raan = 0;
            final double a = 7500000;

            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new CircularOrbit(a, 0, 0, i, raan, 0,
                PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = 0;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

            // ground velocity direction
            final IDirection earthCenterDir = new EarthCenterDirection();
            final IDirection groundVeloDir = new GroundVelocityDirection(earth, earthCenterDir);

            // sat velocity on earth surface in GCRF
            final Vector3D satVelocity = orbit.getPVCoordinates().getVelocity().scalarMultiply(ae / a);

            // earth velocity on earth surface in GCRF
            final Vector3D earthVelocity = new Vector3D(0, 1, 0).scalarMultiply(ae
                    * TIRFProvider.getEarthRotationRate());

            final Vector3D expected = satVelocity.subtract(earthVelocity).normalize();

            // computed direction vector
            final Vector3D result = groundVeloDir.getVector(orbit, date, gcrf);

            // check direction vector
            Assert.assertEquals(0., result.distance(expected), 1E-15);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GROUND_VELOCITY_DIRECTION}
     * 
     * @testedMethod {@link GroundVelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of the ground velocity direction and computation of the direction vector for a given
     *              orbit at given date and frame.
     * 
     * @input orbit
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the direction vector is the expected one. The angle between both vectors should be equal to 0.
     *                   with an allowed error of 1e-10 due to computation errors.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        try {
            // gcrf
            final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
            final CelestialBodyFrame itrf = FramesFactory.getITRF();
            // orbit creation
            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new KeplerianOrbit(7500000, 0.01, 0.2, 0, 0, 0, PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = Constants.GRIM5C1_EARTH_FLATTENING;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

            // ground velocity direction
            final IDirection nadirDir = new NadirDirection(earth);
            final IDirection groundVeloDir = new GroundVelocityDirection(earth, nadirDir);

            // nadir point
            final EllipsoidPoint point = earth.buildPoint(orbit.getPVCoordinates().getPosition(), gcrf, date, "");
            final EllipsoidPoint nadir = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), point
                .getLLHCoordinates().getLatitude(), point.getLLHCoordinates().getLongitude(), 0., "");

            // ground velocity
            final Vector3D surfacePointLocation = itrf.getTransformTo(gcrf, date).transformPosition(
                nadir.getPosition());
            final Vector3D bodySpin = itrf.getTransformTo(gcrf, date).getRotationRate().negate();
            final Vector3D surfacePointVelocity = Vector3D.crossProduct(bodySpin, surfacePointLocation);
            final double r = surfacePointLocation.getNorm() / orbit.getPVCoordinates().getPosition().getNorm();
            final Vector3D satVelocity = orbit.getPVCoordinates().getVelocity().scalarMultiply(r);
            final Vector3D relativeVelocity = satVelocity.subtract(surfacePointVelocity);

            // expected direction vector
            final Vector3D expected = relativeVelocity.normalize();

            // computed direction vector
            final Vector3D result = groundVeloDir.getVector(orbit, date, gcrf);

            // check direction vector
            Assert.assertEquals(0., result.distance(expected), this.comparisonEpsilon);

            Report.printToReport("Direction", expected, result);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GROUND_VELOCITY_DIRECTION}
     * 
     * @testedMethod {@link GroundVelocityDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of the ground velocity direction and computation of the direction vector for a given
     *              orbit at given date and frame.
     * 
     * @input orbit
     * 
     * @output Line
     * 
     * @testPassCriteria the line is the expected one. The angle between their directions should be lower than 1e-10 and
     *                   the distance between these lines should be below 1e-8 m (we perform the same tests than the
     *                   method {@link Line#isSimilarTo(Line)}, for the first test - angle between the directions - we
     *                   take the same epsilon than the method i.e. 1e-10, however for the second test - distance
     *                   between the lines - we take an epsilon of 1e-8 m instead of 1e-10 m because of errors which are
     *                   generated by the line transformations from a frame to another one. These transformations occur
     *                   when the line computation of the ground velocity direction is performed.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testGetLine() {

        try {
            // gcrf
            final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
            final CelestialBodyFrame itrf = FramesFactory.getITRF();
            // orbit creation
            final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
            final double mu = Constants.GRIM5C1_EARTH_MU;
            final Orbit orbit = new KeplerianOrbit(7500000, 0.01, 0.2, 0, 0, 0, PositionAngle.MEAN, gcrf, date, mu);

            // body shape
            final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
            final double f = Constants.GRIM5C1_EARTH_FLATTENING;
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

            // ground velocity direction
            final IDirection nadirDir = new NadirDirection(earth);
            final IDirection groundVeloDir = new GroundVelocityDirection(earth, nadirDir);

            // nadir point
            final EllipsoidPoint point = earth.buildPoint(orbit.getPVCoordinates().getPosition(), gcrf, date, "");
            final EllipsoidPoint nadir = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), point
                .getLLHCoordinates().getLatitude(), point.getLLHCoordinates().getLongitude(), 0., "");

            // ground velocity
            final Vector3D surfacePointLocation = itrf.getTransformTo(gcrf, date).transformPosition(
                nadir.getPosition());
            final Vector3D bodySpin = itrf.getTransformTo(gcrf, date).getRotationRate().negate();
            final Vector3D surfacePointVelocity = Vector3D.crossProduct(bodySpin, surfacePointLocation);
            final double r = surfacePointLocation.getNorm() / orbit.getPVCoordinates().getPosition().getNorm();
            final Vector3D satVelocity = orbit.getPVCoordinates().getVelocity().scalarMultiply(r);
            final Vector3D relativeVelocity = satVelocity.subtract(surfacePointVelocity);

            // expected line
            final Line expected = new Line(surfacePointLocation, surfacePointLocation.add(relativeVelocity),
                surfacePointLocation);

            // computed line
            final Line result = groundVeloDir.getLine(orbit, date, gcrf);

            // check the lines
            final double angle = Vector3D.angle(expected.getDirection(), result.getDirection());
            Assert.assertEquals(0., angle, this.comparisonEpsilon);

            final double distance = result.distance(expected.getOrigin());
            Assert.assertEquals(0., distance, 100 * this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException
     *         OrekitException if the precession-nutation model data embedded in the library cannot be read.
     * @testType UT
     * 
     * @testedFeature {@link features#GROUND_VELOCITY_DIRECTION}
     * 
     * @testedMethod {@link GroundVelocityDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of the ground velocity direction and computation of the direction vector for a given
     *              orbit at given date and frame.
     * 
     * @input orbit and a pointing direction that does not cross the earth surface
     * 
     * @output OrekitException
     * 
     * @testPassCriteria the nadir point cannot be computed because the pointing direction does not intersect the earth,
     *                   therefore an exception should be raised.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testException() throws PatriusException {
        // gcrf
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        // orbit creation
        final AbsoluteDate date = new AbsoluteDate(2012, 6, 20, TimeScalesFactory.getTAI());
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final Orbit orbit = new KeplerianOrbit(7500000, 0.01, 0.2, 0, 0, 0, PositionAngle.MEAN, gcrf, date, mu);

        // body shape
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double f = Constants.GRIM5C1_EARTH_FLATTENING;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, itrf);

        // ground velocity direction
        final IDirection nadirDir = new VelocityDirection(gcrf);
        final IDirection groundVeloDir = new GroundVelocityDirection(earth, nadirDir);
        try {
            groundVeloDir.getVector(orbit, date, gcrf);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
        }
    }

    /** Set up */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
