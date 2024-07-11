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
 * @history creation 19/06/2012
 *
 * HISTORY
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3133:10/05/2022:[PATRIUS] Ajout de plusieurs fonctionnalites a la classe EclipseDetector 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection a altitude
 * VERSION:4.6:DM:DM-2586:27/01/2021:[PATRIUS] intersection entre un objet de type «ExtendedOneAxisEllipsoid» et une droite. 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:457:09/11/2015: Move extendedOneAxisEllipsoid from patrius to orekit addons
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape.MarginType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Ellipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for the ExtendedOneAxisEllipsoid
 * </p>
 * 
 * @see ExtendedOneAxisEllipsoid
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class ExtendedOneAxisEllipsoidTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Celestial body shape
         * 
         * @featureDescription New PATRIUS class for the celestial body shape,
         *                     with more functionalities than the OneAxisEllipsoid
         * 
         * @coveredRequirements DV-EVT_160, DV-VISI_20, DV-VISI_40
         */
        SPHEROID_BODY_SHAPE
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(ExtendedOneAxisEllipsoidTest.class.getSimpleName(), "Extended one axis ellipsoid");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_BODY_SHAPE}
     * 
     * @testedMethod {@link ExtendedOneAxisEllipsoid#transform(GeodeticPoint)}
     * @testedMethod {@link ExtendedOneAxisEllipsoid#transform(Vector3D, Frame, AbsoluteDate)}
     * 
     * @description Test of the cartesian / ellipsoidic transformations.
     * 
     * @input a spheroid celestial body shape, some points of space
     * 
     * @output transformed points : OREKIT's oneAxisEllipsoid tests
     * 
     * @testPassCriteria the output points have the expected coordinates
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public final void transformTest() throws PatriusException {

        Report.printMethodHeader("transformTest", "Geodetic coordinates computation", "Orekit", 1E-10,
            ComparisonType.RELATIVE);

        // Cartesian to ellipsoidic
        this.checkCartesianToEllipsoidic(6378137, 1.0 / 298.257222101,
            4637885.347, 121344.608, 4362452.869,
            0.0261578115331310, 0.757987116290729, 260.455572965371);

        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
            5722966.0, -3304156.0, -24621187.0,
            5.75958652642615, -1.3089969725151, 19134410.3342696, true);

        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            0.0, 0.0, 7000000.0,
            0.0, 1.57079632679490, 643247.685859644);

        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            -6379999.0, 0, 6379000.0,
            3.14159265358979, 0.787690146758403, 2654544.7767725);

        Report.printMethodHeader("transformTest", "Position computation", "Orekit", 1E-6, ComparisonType.ABSOLUTE);

        // Ellipsoidic to cartesian
        final ExtendedOneAxisEllipsoid model =
            new ExtendedOneAxisEllipsoid(6378137.0, 1.0 / 298.257222101,
                FramesFactory.getITRF(), "spheroid2");

        Assert.assertTrue(model.getBodyFrame().getTransformTo(FramesFactory.getITRF(),
            AbsoluteDate.J2000_EPOCH).getRotation().isEqualTo(Rotation.IDENTITY));

        final GeodeticPoint nsp =
            new GeodeticPoint(0.852479154923577, 0.0423149994747243, 111.6);
        final Vector3D p = model.transform(nsp);
        Assert.assertEquals(4201866.69291890, p.getX(), 1.0e-6);
        Assert.assertEquals(177908.184625686, p.getY(), 1.0e-6);
        Assert.assertEquals(4779203.64408617, p.getZ(), 1.0e-6);

        Report.printToReport("Position", new Vector3D(4201866.69291890, 177908.184625686, 4779203.64408617), p);

        Assert.assertTrue(Precision.equalsWithRelativeTolerance(4201866.69291890, p.getX()));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(177908.184625686, p.getY()));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(4779203.64408617, p.getZ()));

        // Test getNativeFrame
        Assert.assertEquals(FramesFactory.getITRF(), model.getNativeFrame(null, null));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_BODY_SHAPE}
     * 
     * @testedMethod {@link ExtendedOneAxisEllipsoid#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)}
     * 
     * @description Test of the spheroid line intersection.
     * 
     * @input a spheroid celestial body shape, some lines of space
     * 
     * @output intersection points : OREKIT's oneAxisEllipsoid tests
     * 
     * @testPassCriteria the output points have the expected coordinates
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testLineIntersection() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();

        ExtendedOneAxisEllipsoid model = new ExtendedOneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
        Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction = new Vector3D(0.0, 1.0, 1.0);
        Line line = new Line(point, point.add(direction));
        GeodeticPoint gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertEquals(gp.getAltitude(), 0.0, 1.0e-12);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new ExtendedOneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
        point = new Vector3D(0.0, -93.7139699, -3.5930796);
        direction = new Vector3D(0.0, -1.0, -1.0);
        line = new Line(point, point.add(direction)).revert();
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new ExtendedOneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
        point = new Vector3D(0.0, -93.7139699, 3.5930796);
        direction = new Vector3D(0.0, -1.0, 1.0);
        line = new Line(point, point.add(direction));
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new ExtendedOneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
        point = new Vector3D(-93.7139699, 0.0, 3.5930796);
        direction = new Vector3D(-1.0, 0.0, 1.0);
        line = new Line(point, point.add(direction));
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));
        Assert.assertFalse(line.contains(new Vector3D(0, 0, 7000000)));

        point = new Vector3D(0.0, 0.0, 110);
        direction = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(point, point.add(direction));
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertEquals(gp.getLatitude(), FastMath.PI / 2, 1.0e-12);

        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(point, point.add(direction));
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertEquals(gp.getLatitude(), 0, 1.0e-12);

        point = new Vector3D(0.0, -110, 0);
        direction = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(point, point.add(direction));
        final GeodeticPoint gp2 = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(gp2 == null);
    }

    @Test
    public void testGetIntersectionPoint() throws PatriusException {
        // Set data common to all cases
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final ExtendedOneAxisEllipsoid model = new ExtendedOneAxisEllipsoid(100.0, 0.9, frame, "spheroid");

        // Case with no intersection points at all
        Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction = new Vector3D(0.0, 9.0, -2.0);
        Vector3D pointMinAbscissa = new Vector3D(0.0, 90, 0);
        Line line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with 2 points, where the abscissas of both points > abscissa min and the first point is the closest one
        // one
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, -1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, 110, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNotNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with 2 points, where the abscissas of both points > abscissa min and the second point is the closest one
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, 1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, 90, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNotNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with 2 points, where the abscissas of both points <= abscissa min
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, -1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, -200, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNull(model.getIntersectionPoint(line, point, frame, date));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ELLIPSOID_DISTANCES}
     * 
     * @testedMethod {@link Ellipsoid#distanceTo(Line)}
     * 
     * @description Test distance to Ellipsoid (with or without normalisation) on 70 various cases.
     * 
     * @input data
     * 
     * @output closest point and distance
     * 
     * @testPassCriteria Convergence is OK. Distance with and without normalisation is the same (at 1E-10).
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testDistanceToEllipsoid() throws PatriusException {
        // Distance to ellipsoid with Ellipsoid = Earth
        final List<Double> listRes = this.recordDistanceToEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        // Distance to ellipsoid with normalized Ellipsoid
        final List<Double> listResNorm = this.recordDistanceToEllipsoid(1.);

        // Check distance is similar
        for (int i = 0; i < listRes.size(); i++) {
            final double d1 = listRes.get(i);
            final double d2 = listResNorm.get(i) * Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
            Assert.assertEquals(0., (d1 - d2) / Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 3E-10);
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ExtendedOneAxisEllipsoid#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}
     * 
     * @description Checks intersection between a line and an ellipsoid at a given altitude on two cases:
     *              <ul>
     *              <li>Intersection at altitude = 100m: altitude of computed points should be 100m (accuracy: 1E-3m)</li>
     *              <li>Intersection at altitude = 0m: altitude of computed points should be 0m (accuracy: 0m)</li>
     *              <li>Intersection at altitude = 1E-4m: altitude of computed points should be 0m (accuracy: 0m)</li>
     *              </ul>
     * 
     * @testPassCriteria altitude of computed points are as expected. Points are on the initial line.
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void intersectionPointsAltitudeTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final ExtendedOneAxisEllipsoid model = new ExtendedOneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, frame, "");
        
        // Test with a random point and altitude = 100m
        final Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction = new Vector3D(0.0, 1.0, 1.0);
        final Line line = new Line(point, point.add(direction));
        final GeodeticPoint gp = model.getIntersectionPoint(line, point, frame, date, 100);
        Assert.assertEquals(gp.getAltitude(), 100.0, 1.0e-3);
        Assert.assertTrue(line.distance(model.transform(gp)) < 1E-8);

        // Test with a random point and altitude = 0m. Exact result is expected
        final Vector3D point2 = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction2 = new Vector3D(0.0, 1.0, 1.0);
        final Line line2 = new Line(point2, point2.add(direction2));
        final GeodeticPoint gp2 = model.getIntersectionPoint(line2, point2, frame, date, 0);
        Assert.assertEquals(gp2.getAltitude(), 0.0, 0);
        Assert.assertTrue(line2.distance(model.transform(gp2)) < 1E-8);

        // No intersection test
        final Vector3D point3 = new Vector3D(1E9, 5E9, 10E9);
        final Vector3D direction3 = new Vector3D(1.0, 1.0, 1.0);
        final Line line3 = new Line(point3, point3.add(direction3));
        final GeodeticPoint gp3 = model.getIntersectionPoint(line3, point3, frame, date, 0.1);
        Assert.assertNull(gp3);

        // Test with a random point and altitude = < eps. Exact result is expected (altitude should be 0)
        final Vector3D point4 = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction4 = new Vector3D(0.0, 1.0, 1.0);
        final Line line4 = new Line(point4, point4.add(direction4));
        final GeodeticPoint gp4 = model.getIntersectionPoint(line4, point4, frame, date, 1E-15);
        Assert.assertEquals(gp4.getAltitude(), 0.0, 1E-9);
        Assert.assertTrue(line4.distance(model.transform(gp4)) < 1E-8);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_BODY_SHAPE}
     * 
     * @testedMethod {@link ExtendedOneAxisEllipsoid#getLocalRadius(PVCoordinatesProvider, Frame, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
     * 
     * @description Check computation does not throw any exception with an acos(x > 1)
     * 
     * @input data leading to acos(x > 1)
     * 
     * @output apparent radius
     * 
     * @testPassCriteria No {@link ArithmeticException} thrown.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testOutOfBoundAcosApparentRadius() throws PatriusException {
        final ExtendedOneAxisEllipsoid ellipsoidInertial = new ExtendedOneAxisEllipsoid(6378137.0,
            (6378137.0 - 6356752.314245179) / 6378137.0, new Frame(FramesFactory.getITRF(), Transform.IDENTITY,
                "inertialFrame", true), "");
        final Vector3D pos = new Vector3D(-264144.8224132271, 1472993.560163555, -6179291.330687755);
        final AbsoluteDate date = new AbsoluteDate(366803769, 0.5980079787259456);
        final PVCoordinatesProvider occultedBody = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate otherDate, final Frame otherFrame)
                throws PatriusException {
                return new PVCoordinates(new Vector3D(1, 2, 3), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(AbsoluteDate date, Frame frame) throws PatriusException {
                return null;
            }
        };
        try {
            ellipsoidInertial.getLocalRadius(pos, FramesFactory.getGCRF(), date, occultedBody);
        } catch (final ArithmeticException e) {
            Assert.fail();
        }
        Assert.assertTrue(true);
    }

    /**
     * Test needed to validate the resize of an ExtendedOneAxisEllipsoid.
     * 
     * @testedMethod {@link ExtendedOneAxisEllipsoid#resize(MarginType, double)}
     * 
     * @throws PatriusException if the precession-nutation model data embedded in the library cannot be read or if the
     *         margin type is invalid
     */
    @Test
    public final void testResize() throws PatriusException {
        // Initialization
        final double tolerance = 1E-11;
        final ExtendedOneAxisEllipsoid model = new ExtendedOneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "");
        // Case with a zero margin distance
        final double marginValue2 = 0.;
        final ExtendedOneAxisEllipsoid model2 = model.resize(MarginType.DISTANCE, marginValue2);
        Assert.assertEquals(
            1. - (model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue2)
                    / model2.getEquatorialRadius(), model2.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() + marginValue2, model2.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue2,
            model2.getEquatorialRadius() * (1. - model2.getFlattening()), tolerance);
        // Case with a positive margin distance
        final double marginValue3 = 1E3;
        final ExtendedOneAxisEllipsoid model3 = model.resize(MarginType.DISTANCE, marginValue3);
        Assert.assertEquals(
            1. - (model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue3)
                    / model3.getEquatorialRadius(), model3.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() + marginValue3, model3.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue3,
            model3.getEquatorialRadius() * (1. - model3.getFlattening()), tolerance);
        // Case with a negative margin distance smaller than the opposite of the polar (smallest) radius
        final double marginValue4 = -model.getEquatorialRadius() * (1 - model.getFlattening()) + 1E-3;
        final ExtendedOneAxisEllipsoid model4 = model.resize(MarginType.DISTANCE, marginValue4);
        Assert.assertEquals(
            1. - (model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue4)
                    / model4.getEquatorialRadius(), model4.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() + marginValue4, model4.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue4,
            model4.getEquatorialRadius() * (1. - model4.getFlattening()), tolerance);
        // Case with a negative margin distance equal to the opposite of the polar (smallest) radius
        final double marginValue5 = -model.getEquatorialRadius() * (1 - model.getFlattening());
        try {
            model.resize(MarginType.DISTANCE, marginValue5);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Case with a negative margin distance larger than the opposite of the polar (smallest) radius
        final double marginValue6 = -model.getEquatorialRadius() * (1 - model.getFlattening()) - 1E-3;
        try {
            model.resize(MarginType.DISTANCE, marginValue6);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Case with a positive margin scale factor larger than 1
        final double marginValue7 = 2.;
        final ExtendedOneAxisEllipsoid model7 = model.resize(MarginType.SCALE_FACTOR, marginValue7);
        Assert.assertEquals(model.getFlattening(), model7.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * marginValue7, model7.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) * marginValue7,
            model7.getEquatorialRadius() * (1. - model7.getFlattening()), tolerance);
        // Case with a positive margin scale factor equal to 1
        final double marginValue8 = 1.;
        final ExtendedOneAxisEllipsoid model8 = model.resize(MarginType.SCALE_FACTOR, marginValue8);
        Assert.assertEquals(model.getFlattening(), model8.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * marginValue8, model8.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) * marginValue8,
            model8.getEquatorialRadius() * (1. - model8.getFlattening()), tolerance);
        // Case with a positive margin scale factor smaller than 1
        final double marginValue9 = 0.5;
        final ExtendedOneAxisEllipsoid model9 = model.resize(MarginType.SCALE_FACTOR, marginValue9);
        Assert.assertEquals(model.getFlattening(), model9.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * marginValue9, model9.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) * marginValue9,
            model9.getEquatorialRadius() * (1. - model9.getFlattening()), tolerance);
        // Case with a margin scale factor equal to 0
        final double marginValue10 = 0.;
        try {
            model.resize(MarginType.SCALE_FACTOR, marginValue10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        // Case with a negative margin scale factor
        final double marginValue11 = -0.5;
        try {
            model.resize(MarginType.SCALE_FACTOR, marginValue11);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Compute distance to ellipsoid.
     * 
     * @param requa
     *        ellipsoid equatorial radius
     * @return list of distance to ellipsoid
     * @throws PatriusException
     */
    private List<Double> recordDistanceToEllipsoid(final double requa) throws PatriusException {

        // Reference Frame
        final Frame gcrf = FramesFactory.getGCRF();

        final List<Double> listRes = new ArrayList<Double>();

        // Ellipsoid
        final double rpole = requa * (1 - Constants.WGS84_EARTH_FLATTENING);
        final ExtendedOneAxisEllipsoid elli =
            new ExtendedOneAxisEllipsoid(requa, (requa - rpole) / requa, gcrf, "Terre");

        // Position "observateur" : rayon r, et latitude geocentrique teta
        double teta;
        double r;
        Vector3D pos; // Position cartesienne
        Vector3D dir1; // Direction visee 1 (Direction de visée "rasante" AU DESSUS l'horizon, vers l'Ouest)
        Vector3D dir2; // Direction visee 2 (visée "rasante" vers Nord-ouest)
        Vector3D dir3; // Direction visee 3 (visée "rasante" vers Nord)
        final Vector3D west = Vector3D.MINUS_J;
        final Vector3D est = Vector3D.PLUS_J;
        final Vector3D polaris = Vector3D.PLUS_K;
        r = requa + 700.e3 * requa / Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

        teta = MathLib.toRadians(0);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4336, -0.9011, 0.00000).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(1);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4335, -0.9011, -0.0076).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(5);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4319, -0.9011, -0.0378).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(10);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4271, -0.9010, -0.0753).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(15);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4191, -0.9009, -0.1123).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(30);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.3766, -0.9005, -0.2174).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(45);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.3084, -0.8999, -0.3084).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(60);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.2187, -0.8993, -0.3787).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(80);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0761, -0.8988, -0.4318).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(85);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0382, -0.8987, -0.4369).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(89);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0077, -0.8987, -0.4385).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(90);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0000, -0.8987, -0.4386).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(91);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(0.0077, -0.8987, -0.4383).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(120);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(0.2187, -0.8993, -0.3760).normalize();
        dir2 = this.rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = this.rotateAzim(pos, dir1, MathLib.toRadians(90));
        this.testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);

        return listRes;
    }

    /**
     * Rotate vector.
     */
    private Vector3D rotateAzim(final Vector3D pos, final Vector3D dir0, final double angle) {
        final Rotation rot = new Rotation(pos.negate(), angle);
        return rot.applyTo(dir0).normalize();
    }

    /**
     * Compute distance to ellipsoid.
     * 
     * @throws PatriusException
     */
    private void testsDistLineEllipsoid(final Frame frame, final ExtendedOneAxisEllipsoid elli, final Vector3D pos,
                                        final Vector3D dir1, final Vector3D dir2, final Vector3D dir3,
                                        final Vector3D dir4, final Vector3D dir5, final Vector3D dir6,
                                        final List<Double> listRes) throws PatriusException {
        listRes.add(elli.distanceTo(Line.createLine(pos, dir1), frame, AbsoluteDate.J2000_EPOCH));
        listRes.add(elli.distanceTo(Line.createLine(pos, dir2), frame, AbsoluteDate.J2000_EPOCH));
        listRes.add(elli.distanceTo(Line.createLine(pos, dir3), frame, AbsoluteDate.J2000_EPOCH));
        listRes.add(elli.distanceTo(Line.createLine(pos, dir4), frame, AbsoluteDate.J2000_EPOCH));
        listRes.add(elli.distanceTo(Line.createLine(pos, dir5), frame, AbsoluteDate.J2000_EPOCH));
        listRes.add(elli.distanceTo(Line.createLine(pos, dir6), frame, AbsoluteDate.J2000_EPOCH));
    }

    private void checkCartesianToEllipsoidic(final double ae, final double f,
                                             final double x, final double y, final double z,
                                             final double longitude, final double latitude,
                                             final double altitude)
                                                                   throws PatriusException {
        this.checkCartesianToEllipsoidic(ae, f, x, y, z, longitude, latitude, altitude, false);
    }

    /**
     * Tests the transformation from cartesian to ellipsoidic coordinates.
     * 
     * @param ae
     *        equatorial radius of the ellipsoid
     * @param f
     *        the flattening (f = (a-b)/a)
     * @param x
     *        coordinate
     * @param y
     *        coordinate
     * @param z
     *        coordinate
     * @param longitude
     *        coordinate
     * @param latitude
     *        coordinate
     * @param altitude
     *        coordinate
     * @throws PatriusException
     *         in case of frames computation problem
     */
    private void checkCartesianToEllipsoidic(final double ae, final double f,
                                             final double x, final double y, final double z,
                                             final double longitude, final double latitude,
                                             final double altitude, final boolean writeToReport)
                                                                                                throws PatriusException {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final ExtendedOneAxisEllipsoid model = new ExtendedOneAxisEllipsoid(ae, f, frame, "spheroid");
        final GeodeticPoint gp = model.transform(new Vector3D(x, y, z), frame, date);
        Assert.assertEquals(longitude, MathUtils.normalizeAngle(gp.getLongitude(), longitude), 1.0e-10);
        Assert.assertEquals(latitude, gp.getLatitude(), 1.0e-10);
        Assert.assertEquals(altitude, gp.getAltitude(), 1.0e-10 * MathLib.abs(altitude));
        final Vector3D rebuiltNadir = Vector3D.crossProduct(gp.getSouth(), gp.getWest());
        Assert.assertEquals(0, rebuiltNadir.subtract(gp.getNadir()).getNorm(), 1.0e-15);

        if (writeToReport) {
            Report.printToReport("Longitude", longitude, MathUtils.normalizeAngle(gp.getLongitude(), longitude));
            Report.printToReport("Latitude", latitude, gp.getLatitude());
            Report.printToReport("Altitude", altitude, gp.getAltitude());
        }
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

    }
}
