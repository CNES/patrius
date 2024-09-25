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
 * @history creation 27/09/2011
 *
 * HISTORY
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.13:FA:FA-159:08/12/2023:[PATRIUS] Probleme de normalisation dans TopocentricFrame
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.frames;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.CardanMountPV;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TopocentricPV;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * additional tests for the topocentric frame.
 * 
 * @author Julie Anton
 * 
 */
public class TopocentricFrameTest {

    /** Computation date. */
    private AbsoluteDate date;

    /** Reference frame = ITRF 2005. */
    private CelestialBodyFrame frameITRF2005;

    /** Earth shape. */
    OneAxisEllipsoid earthSpheric;

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon for distance comparison. */
    private final double distanceEpsilon = Utils.epsilonTest;

    /** Epsilon for angle comparison. */
    private final double angleEpsilon = Utils.epsilonAngle;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle transformation
         * 
         * @featureDescription transformation validation
         * 
         * @coveredRequirements DV-COORD_140, DV-COORD_150
         */
        TRANSFORMATION,
        /**
         * @featureTitle topocentric frame definition
         * 
         * @featureDescription implementation of a topocentric frame
         * 
         * @coveredRequirements DV-REPERES_130
         */
        DEFINITION,
        /**
         * @featureTitle topocentric frame orientation
         * 
         * @featureDescription implementation of different topocentric frames
         * 
         * @coveredRequirements DV-REPERES_140
         */
        ORIENTATION,
        /**
         * @featureTitle azimuth
         * 
         * @featureDescription azimuth computation
         * 
         * @coveredRequirements DV-COORD_140
         */
        AZIMUTH
    }

    @SuppressWarnings("unused")
    public void tutorial() throws PatriusException {
        // Reference frame = ITRF 2005
        this.frameITRF2005 = FramesFactory.getITRF();

        // Elliptic earth shape
        this.earthSpheric = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

        // Geodetic point at which to attach the frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(30.), 0., "");

        // Build the east frame
        final TopocentricFrame topoEast = new TopocentricFrame(point, "lon 30 lat 0");

        // Build a frame rotated by 30° from the North frame
        final TopocentricFrame topo30 = new TopocentricFrame(point, MathLib.toRadians(30.), "lon 30 lat 0");

        topoEast.getZenith();

        // compute the azimuth of a position
        final Vector3D position = new Vector3D(1.0, 2.0, 3.0);
        topo30.getAzimuth(position, this.earthSpheric.getBodyFrame(), this.date);

        // get the transformation from the topocentric East to the ITRF2005
        final Transform eastTOitrf2005 = topoEast.getTransformTo(this.frameITRF2005, AbsoluteDate.FIFTIES_EPOCH_TT);

        // apply the transformation
        final PVCoordinates pointPVeast = new PVCoordinates(new Vector3D(0., -1., 1), Vector3D.ZERO);
        eastTOitrf2005.transformPVCoordinates(pointPVeast);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DEFINITION}
     * 
     * @testedMethod {@link TopocentricFrame#TopocentricFrame(EllipsoidPoint, String)}
     * 
     * @description two topocentric frames are generated, the origins of these frames are two geodetic points with
     *              opposite latitude. We check here if these topocentric frames are coherent ie that their East axis
     *              are collinear, their North axis are perpendicular and their Zenith axis are perdendicular.
     * 
     * @input GeodeticPoint point1 = (45°, 30°, 0 m)
     *        <p>
     *        GeodeticPoint point2 = (-45°, 30°, 0 m)
     *        </p>
     * 
     * @output TopocentricFrame
     * 
     * @testPassCriteria the obtained topocentric frames should have their East axis collinear, their North axis
     *                   perpendicular and their Zenith axis perpendicular with an epsilon of 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
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

        Assert.assertEquals(1., xDiff, this.comparisonEpsilon);
        Assert.assertEquals(0., yDiff, this.comparisonEpsilon);
        Assert.assertEquals(0., zDiff, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DEFINITION}
     * 
     * @testedMethod {@link TopocentricFrame#TopocentricFrame(EllipsoidPoint, String)}
     * 
     * @description two topocentric frames are generated, the origins of these frames are two geodetic points with
     *              opposite latitude wrt the equator and opposite longitude wrt the center of the Earth. We check here
     *              if these topocentric frames are consistent with the theory ie that their East axis are collinear
     *              with the same direction, their North axis are collinear with the same direction and their Zenith
     *              axis are also collinear but with opposite directions.
     * 
     * @input GeodeticPoint point1 = (45°,30°,0 m)
     *        <p>
     *        GeodeticPoint point2 = (-45°, 210°, 0 m)
     *        </p>
     * 
     * @output TopocentricFrame
     * 
     * @testPassCriteria the obtained topocentric frames should have their East axis are collinear with the same
     *                   direction, their North axis are collinear with the same direction and their Zenith axis are
     *                   also collinear but with opposite directions. The south direction of one frame should be
     *                   collinear but with opposite direction to the north direction of the other frame. The nadir
     *                   direction of one frame should be collinear and with the same direction to the zenith direction
     *                   of the other frame. The epsilon used is equal to 1e-14, it takes into account the computation
     *                   errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
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
        double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assert.assertEquals(1., xDiff, this.comparisonEpsilon);
        Assert.assertEquals(1., yDiff, this.comparisonEpsilon);
        Assert.assertEquals(-1., zDiff, this.comparisonEpsilon);

        // test the South direction
        yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getSouth());

        Assert.assertEquals(-1., yDiff, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DEFINITION}
     * 
     * @testedMethod {@link TopocentricFrame#TopocentricFrame(EllipsoidPoint, String)}
     * 
     * @description two topocentric frames are generated, the origins of these frames are the
     *              singular north and south poles. We check here
     *              if these topocentric frames are well defined.
     * 
     * @input GeodeticPoint point1 = (90°,30°,0 m)
     *        <p>
     *        GeodeticPoint point2 = (-90°, 30°, 0 m)
     *        </p>
     * 
     * @output TopocentricFrame
     * 
     * @testPassCriteria The obtained topocentric frames should have their Zenith, East and North
     *                   axis be unit vector forming a right-handed orthonormal basis. The epsilon
     *                   used is equal to 1e-14, it takes into account the computation errors.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testSingularPoles() {
        // First point at north pole
        final EllipsoidPoint point1 =
                new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
                        MathLib.toRadians(90.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame1 = new TopocentricFrame(point1, "lat 90");

        // Second point at south pole
        final EllipsoidPoint point2 =
                new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
                        MathLib.toRadians(-90.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoFrame2 = new TopocentricFrame(point2, "lat -90");

        final Vector3D zenith1 = topoFrame1.getZenith();
        final Vector3D zenith2 = topoFrame2.getZenith();
        final Vector3D east1 = topoFrame1.getEast();
        final Vector3D east2 = topoFrame2.getEast();
        final Vector3D north1 = topoFrame1.getNorth();
        final Vector3D north2 = topoFrame2.getNorth();

        // Check that East and North are unit vectors
        Assert.assertEquals(1., zenith1.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1., zenith2.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1., east1.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1., east2.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1., north1.getNorm(), this.comparisonEpsilon);
        Assert.assertEquals(1., north2.getNorm(), this.comparisonEpsilon);

        // Check that Zenith, East and North define a right-handed orthonormal basis
        final double dotProduct1 = Vector3D.dotProduct(east1, north1);
        final Vector3D crossProduct1 = Vector3D.crossProduct(east1, north1);
        final double zDiff1 = Vector3D.dotProduct(crossProduct1, zenith1);
        Assert.assertEquals(0., dotProduct1, this.comparisonEpsilon);
        Assert.assertEquals(1., zDiff1, this.comparisonEpsilon);

        final double dotProduct2 = Vector3D.dotProduct(east2, north2);
        final Vector3D crossProduct2 = Vector3D.crossProduct(east2, north2);
        final double zDiff2 = Vector3D.dotProduct(crossProduct2, zenith2);
        Assert.assertEquals(0., dotProduct2, this.comparisonEpsilon);
        Assert.assertEquals(1., zDiff2, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZIMUTH}
     * 
     * @testedMethod {@link TopocentricFrame#getAzimuth(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description the azimuth computation is tested on different cases
     * 
     * @input the first topocentric frame is an East topocentric frame with the origin at the North pole, the second
     *        topocentric frame is a topocentric frame shifted by an angle larger than 2PI wrt the North topocentric
     *        frame
     *        <p>
     *        GeodeticPoint satPoint = (28°,30°,800000 m) : satellite position
     *        </p>
     *        <p>
     *        GeodeticPoint satPoint = (28°,-30°,800000 m) : satellite position
     *        </p>
     * 
     * @output azimuth of the satellite (double)
     * 
     * @testPassCriteria the azimuth of the satellites is always equal to PI minus their longitude with an allowed error
     *                   of 1e-7 on the angles.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    @Test
    public void testAzimuthPole() throws PatriusException {

        // Surface point at longitude 0
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(89.9999999), MathLib.toRadians(0.), 0., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "lon 0 lat 90");

        // Point at 30 deg longitude
        // **************************
        EllipsoidPoint satPoint = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(28.), MathLib.toRadians(30.), 800000., "");

        // Azimuth =
        double azi = topoFrame.getAzimuth(satPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI - satPoint.getLLHCoordinates().getLongitude(), azi, this.angleEpsilon);

        // Point at -30 deg longitude
        // ***************************
        satPoint = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(28.), MathLib.toRadians(-30.), 800000., "");

        // Azimuth =
        azi = topoFrame.getAzimuth(satPoint.getPosition(), this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI - satPoint.getLLHCoordinates().getLongitude(), azi, this.angleEpsilon);

        // Added for a complete coverage : when the orientation angle is above 2PI (absolute value)
        final TopocentricFrame topoFrameCoverage = new TopocentricFrame(point, -(MathUtils.HALF_PI + MathUtils.TWO_PI),
            "lon 0 lat 90");

        // Azimuth =
        final double aziCoverage = topoFrameCoverage.getAzimuth(satPoint.getPosition(),
            this.earthSpheric.getBodyFrame(), this.date);
        Assert.assertEquals(FastMath.PI - satPoint.getLLHCoordinates().getLongitude(), aziCoverage, this.angleEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ORIENTATION}
     * 
     * @testedMethod {@link TopocentricFrame#TopocentricFrame(Frame, EllipsoidPoint, double, String)}
     * 
     * @description the construction of a generic topocentric frame oriented by any angle from the local North is
     *              tested. For that purpose, we create a East topocentric frame, a North topocentric frame and a
     *              topocentric frame whose x axis is shifted by 30° wrt the local North. The three topocentric frames
     *              have the same origin.
     * 
     * @input PVCoordinates testPV = (sqrt(3) / 2, 0.5, 1, 0, 0, 0) : Cartesian position in the East topocentric frame.
     * 
     * @output the position velocity coordinates of the testPV point expressed in the North topocentric frame and in the
     *         generic topocentric frame, shifted by an angle of 30° wrt the North topocentric frame (the type of the
     *         output is PVCoordinates) with an epsilon of 1e-12 because we compare distances.
     * 
     * @testPassCriteria the position velocity obtained in the North topocentric frame are : (0.5, -sqrt(3) / 2, 1, 0,
     *                   0, 0) the position velocity obtained in the generic topocentric frame are : (0, -1, 1, 0, 0, 0)
     *                   with an epsilon of 1e-12 because the comparison is done on distances.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    @Test
    public void testConstructorWithAngle() throws PatriusException {
        // get the east topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(30.), 0., "");
        final TopocentricFrame topoEast = new TopocentricFrame(point, "lon 30 lat 0");

        Assert.assertEquals(-MathUtils.HALF_PI, topoEast.getOrientation(), 0.);

        /*
         * test the North topocentric frame first
         */
        // get the north topocentric frame
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., "lon 30 lat 0");

        Assert.assertEquals(0., topoNorth.getOrientation(), 0.);

        // get the transformation east to north
        final Transform eastTOnorth = topoEast.getTransformTo(topoNorth, AbsoluteDate.FIFTIES_EPOCH_TT);

        // test the transformation
        final PVCoordinates testPV = new PVCoordinates(new Vector3D(MathLib.sqrt(3.) / 2., 0.5, 1.), Vector3D.ZERO);
        final PVCoordinates expectedResultNorth = new PVCoordinates(new Vector3D(0.5, -MathLib.sqrt(3) / 2, 1),
            Vector3D.ZERO);
        final PVCoordinates resultNorth = eastTOnorth.transformPVCoordinates(testPV);

        Assert.assertEquals(expectedResultNorth.getPosition().getX(), resultNorth.getPosition().getX(),
            this.distanceEpsilon);
        Assert.assertEquals(expectedResultNorth.getPosition().getY(), resultNorth.getPosition().getY(),
            this.distanceEpsilon);
        Assert.assertEquals(expectedResultNorth.getPosition().getZ(), resultNorth.getPosition().getZ(),
            this.distanceEpsilon);

        /*
         * test one rotated frame
         */
        // get a topocentric frame rotated by 30 degrees around the zenith from the north frame
        final TopocentricFrame topo30 = new TopocentricFrame(point, MathLib.toRadians(30.),
            "lon 30 lat 0");

        Assert.assertEquals(MathLib.toRadians(30.), topo30.getOrientation(), 0.);

        // get the transformation east to north
        final Transform eastTO30 = topoEast.getTransformTo(topo30, AbsoluteDate.FIFTIES_EPOCH_TT);

        // test the transformation
        final PVCoordinates expectedResult30 = new PVCoordinates(new Vector3D(0., -1., 1), Vector3D.ZERO);
        final PVCoordinates result30 = eastTO30.transformPVCoordinates(testPV);

        Assert.assertEquals(expectedResult30.getPosition().getX(), result30.getPosition().getX(), this.distanceEpsilon);
        Assert.assertEquals(expectedResult30.getPosition().getY(), result30.getPosition().getY(), this.distanceEpsilon);
        Assert.assertEquals(expectedResult30.getPosition().getZ(), result30.getPosition().getZ(), this.distanceEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#getAzimuth(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description we test analytically the results given by the function getAzimuth() with several local topocentric
     *              frames
     * 
     * @input The inputs are the following
     *        <p>
     *        In the local topocentric frame with an x axis shifted by 45° wrt the local North :
     *        </p>
     *        <p>
     *        Vector3D position = (1,1,0) [m]
     *        </p>
     *        <p>
     *        Vector3D position = (-1,1,0) [m]
     *        </p>
     *        <p>
     *        Vector3D position = (-1,-1,0) [m]
     *        </p>
     *        <p>
     *        Vector3D position = (1,-1,0) [m]
     *        </p>
     *        <p>
     *        In the local topocentric frame with an x axis shifted by -45° wrt the local North :
     *        </p>
     *        <p>
     *        Vector3D position = (1,1,0) [m]
     *        </p>
     *        <p>
     *        Vector3D position = (-1,1,0) [m]
     *        </p>
     *        <p>
     *        Vector3D position = (-1,-1,0) [m]
     *        </p>
     *        <p>
     *        Vector3D position = (1,-1,0) [m]
     *        </p>
     * 
     * @output azimuth values
     * 
     * @testPassCriteria the expected outputs of the method for this test case are the following
     *                   <p>
     *                   In the local topocentric frame with an x axis shifted by 45° wrt the local North :
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code 3 PI / 2} [rad]
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code PI} [rad]
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code PI / 2} [rad]
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code 0} [rad]
     *                   </p>
     *                   <p>
     *                   In the local topocentric frame with an x axis shifted by -45° wrt the local North :
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code 0} [rad]
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code 3 PI / 2} [rad]
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code PI} [rad]
     *                   </p>
     *                   <p>
     *                   double azimuth = {@code PI / 2} [rad]
     *                   </p>
     *                   all with an epsilon of 1e-7 because the comparison is done on angles.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    @Test
    public void testGetAzimuth() throws PatriusException {

        // North topocentric frame
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth = new TopocentricFrame(point, 0., "north topocentric frame");

        // topocentric frame shifted by 45° wrt the North topocentric frame
        final TopocentricFrame topoNorth45 = new TopocentricFrame(point, MathLib.toRadians(45),
            "north topocentric frame");

        // transformation of
        Assert.assertEquals(3 * MathUtils.HALF_PI, topoNorth.getAzimuth(new Vector3D(1, 1, 0), topoNorth45, this.date),
            this.angleEpsilon);
        Assert.assertEquals(FastMath.PI, topoNorth.getAzimuth(new Vector3D(-1, 1, 0), topoNorth45, this.date),
            this.angleEpsilon);
        Assert.assertEquals(MathUtils.HALF_PI, topoNorth.getAzimuth(new Vector3D(-1, -1, 0), topoNorth45, this.date),
            this.angleEpsilon);
        Assert.assertEquals(0., topoNorth.getAzimuth(new Vector3D(1, -1, 0), topoNorth45, this.date),
            this.angleEpsilon);

        // topocentric frame shifted by -45° wrt the North topocentric frame
        final TopocentricFrame topoNorthMinus45 = new TopocentricFrame(point, MathLib.toRadians(-45),
            "north topocentric frame");

        Assert.assertEquals(0., topoNorth.getAzimuth(new Vector3D(1, 1, 0), topoNorthMinus45, this.date),
            Utils.epsilonAngle);
        Assert.assertEquals(3 * MathUtils.HALF_PI,
            topoNorth.getAzimuth(new Vector3D(-1, 1, 0), topoNorthMinus45, this.date), this.angleEpsilon);
        Assert.assertEquals(FastMath.PI, topoNorth.getAzimuth(new Vector3D(-1, -1, 0), topoNorthMinus45, this.date),
            this.angleEpsilon);
        Assert.assertEquals(MathUtils.HALF_PI,
            topoNorth.getAzimuth(new Vector3D(1, -1, 0), topoNorthMinus45, this.date), this.angleEpsilon);
    }

    /**
     * @testtype UT
     * 
     * @testedFeature {@link features#TRANSFORMATION}
     * 
     * @testedMethod {@link TopocentricFrame#transformFromCardanToPV(CardanMountPV)}
     * @testedMethod {@link TopocentricFrame#transformFromPVToCardan(PVCoordinates, Frame, AbsoluteDate)}
     * @testedMethod {@link TopocentricFrame#transformFromTopocentricToPV(TopocentricPV)}
     * @testedMethod {@link TopocentricFrame#transformFromPVToTopocentric(PVCoordinates, Frame, AbsoluteDate)}
     * 
     * @description we test that the composition of the transformation from pv coordinates to topocentric coordinates
     *              (respectively cardan mounting) with the transformation from topocentric coordinates (respectively
     *              cardan mounting) to pv coordinates is equivalent to the identity transformation.
     * 
     * @input The input values are the following :
     *        <p>
     *        PVCoordinates pv = (100,-65,35,-23,-86,12)
     *        </p>
     * 
     * 
     * @output position velocity coordinates
     * 
     * @testPassCriteria the obtained pv coordinates should be equal to the input with an epsilon of 1e-12 because the
     *                   comparison is done on distances.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if frames transformations cannot be computed
     */
    @Test
    public void testTransformComposition() throws PatriusException {

        // **************************
        // North topocentric frame
        // **************************

        // Construction of the North topocentric frame
        // Origin of the local topocentric frame : Toulouse
        final EllipsoidPoint point = new EllipsoidPoint(this.earthSpheric, this.earthSpheric.getLLHCoordinatesSystem(),
            MathLib.toRadians(43.604482), MathLib.toRadians(1.443962), 0., "");
        final TopocentricFrame topoNorth =
            new TopocentricFrame(point, 0., "north topocentric frame");

        // Position and velocity of the satellite expressed in the previous topocentric frame
        final Vector3D position = new Vector3D(100, -65, 35);
        final Vector3D velocity = new Vector3D(-23, -86, 12);
        final PVCoordinates pv = new PVCoordinates(position, velocity);

        PVCoordinates pvResult;

        // Transformation pv coordinates --> topocentric coordinates --> pv coordinates

        TopocentricPV topoCoord = topoNorth.transformFromPVToTopocentric(pv, topoNorth, this.date);
        pvResult = topoNorth.transformFromTopocentricToPV(topoCoord);
        // The obtained pv coordinates should be the same as the satellite position and velocity given
        // initially
        Assert.assertEquals(pv.getPosition().getX(), pvResult.getPosition().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getY(), pvResult.getPosition().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getZ(), pvResult.getPosition().getZ(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getX(), pvResult.getVelocity().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getY(), pvResult.getVelocity().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getZ(), pvResult.getVelocity().getZ(), this.distanceEpsilon);

        // Transformation from pv coordinates --> cardan mounting --> pv coordinates

        CardanMountPV cardanCoord = topoNorth.transformFromPVToCardan(pv, topoNorth, this.date);
        pvResult = topoNorth.transformFromCardanToPV(cardanCoord);
        // The obtained pv coordinates should be the same as the satellite position and velocity given
        // initially
        Assert.assertEquals(pv.getPosition().getX(), pvResult.getPosition().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getY(), pvResult.getPosition().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getZ(), pvResult.getPosition().getZ(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getX(), pvResult.getVelocity().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getY(), pvResult.getVelocity().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getZ(), pvResult.getVelocity().getZ(), this.distanceEpsilon);

        // **************************
        // East topocentric frame
        // **************************

        // Construction of the East topocentric frame (with Toulouse as the origin)
        final TopocentricFrame topoEast = new TopocentricFrame(point, MathLib.toRadians(-90),
            "north topocentric frame");

        // Transformation pv coordinates --> topocentric coordinates --> pv coordinates

        topoCoord = topoEast.transformFromPVToTopocentric(pv, topoEast, this.date);
        pvResult = topoEast.transformFromTopocentricToPV(topoCoord);
        // The obtained pv coordinates should be the same as the satellite position and velocity given
        // initially
        Assert.assertEquals(pv.getPosition().getX(), pvResult.getPosition().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getY(), pvResult.getPosition().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getZ(), pvResult.getPosition().getZ(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getX(), pvResult.getVelocity().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getY(), pvResult.getVelocity().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getZ(), pvResult.getVelocity().getZ(), this.distanceEpsilon);

        // Transformation pv coordinates --> cardan mounting --> pv coordinates

        cardanCoord = topoEast.transformFromPVToCardan(pv, topoEast, this.date);
        pvResult = topoEast.transformFromCardanToPV(cardanCoord);
        // The obtained pv coordinates should be the same as the satellite position and velocity given
        // initially
        Assert.assertEquals(pv.getPosition().getX(), pvResult.getPosition().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getY(), pvResult.getPosition().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getPosition().getZ(), pvResult.getPosition().getZ(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getX(), pvResult.getVelocity().getX(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getY(), pvResult.getVelocity().getY(), this.distanceEpsilon);
        Assert.assertEquals(pv.getVelocity().getZ(), pvResult.getVelocity().getZ(), this.distanceEpsilon);

    }

    /**
     * before the tests
     */
    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-dataPBASE");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

            // Reference frame = ITRF 2005
            this.frameITRF2005 = FramesFactory.getITRF();

            // Elliptic earth shape
            this.earthSpheric = new OneAxisEllipsoid(6378136.460, 0., this.frameITRF2005);

            // Reference date
            this.date =
                new AbsoluteDate(new DateComponents(2008, 04, 07), TimeComponents.H00, TimeScalesFactory.getUTC());

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    /**
     * after the tests
     */
    @After
    public void tearDown() {
        this.date = null;
        this.frameITRF2005 = null;
        this.earthSpheric = null;
    }
}
