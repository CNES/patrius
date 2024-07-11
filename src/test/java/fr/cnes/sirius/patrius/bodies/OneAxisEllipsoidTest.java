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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3133:10/05/2022:[PATRIUS] Ajout de plusieurs fonctionnalites a la classe EclipseDetector 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
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
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape.MarginType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Ellipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Test class for {@link OneAxisEllipsoid}.
 * </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class OneAxisEllipsoidTest {

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
        Report.printClassHeader(OneAxisEllipsoidTest.class.getSimpleName(), "Extended one axis ellipsoid");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_BODY_SHAPE}
     * 
     * @testedMethod {@link OneAxisEllipsoid#transform(GeodeticPoint)}
     * @testedMethod {@link OneAxisEllipsoid#transform(Vector3D, Frame, AbsoluteDate)}
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
    public void transformTest() throws PatriusException {

        Report.printMethodHeader("transformTest", "Geodetic coordinates computation", "Orekit", 1E-10,
            ComparisonType.RELATIVE);

        // Cartesian to ellipsoidic
        checkCartesianToEllipsoidic(6378137, 1.0 / 298.257222101,
            4637885.347, 121344.608, 4362452.869,
            0.0261578115331310, 0.757987116290729, 260.455572965371);

        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
            5722966.0, -3304156.0, -24621187.0,
            5.75958652642615, -1.3089969725151, 19134410.3342696, true);

        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            0.0, 0.0, 7000000.0,
            0.0, 1.57079632679490, 643247.685859644);

        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            -6379999.0, 0, 6379000.0,
            3.14159265358979, 0.787690146758403, 2654544.7767725);

        Report.printMethodHeader("transformTest", "Position computation", "Orekit", 1E-6, ComparisonType.ABSOLUTE);

        // Ellipsoidic to cartesian
        final OneAxisEllipsoid model =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101,
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
     * @testedMethod {@link OneAxisEllipsoid#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)}
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

        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
        Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction = new Vector3D(0.0, 1.0, 1.0);
        Line line = new Line(point, point.add(direction));
        GeodeticPoint gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertEquals(gp.getAltitude(), 0.0, 1.0e-12);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(0.0, -93.7139699, -3.5930796);
        direction = new Vector3D(0.0, -1.0, -1.0);
        line = new Line(point, point.add(direction)).revert();
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
        point = new Vector3D(0.0, -93.7139699, 3.5930796);
        direction = new Vector3D(0.0, -1.0, 1.0);
        line = new Line(point, point.add(direction));
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame, "spheroid");
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
        Assert.assertNull(gp2);
    }

    @Test
    public void testGetIntersectionPoint() throws PatriusException {
        // Set data common to all cases
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame, "spheroid");

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
    public void testDistanceToEllipsoid() throws PatriusException {
        // Distance to ellipsoid with Ellipsoid = Earth
        final List<Double> listRes = recordDistanceToEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        // Distance to ellipsoid with normalized Ellipsoid
        final List<Double> listResNorm = recordDistanceToEllipsoid(1.);

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
     * @testedMethod {@link OneAxisEllipsoid#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}
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
    public void intersectionPointsAltitudeTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, frame, "");

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
     * @testedMethod {@link OneAxisEllipsoid#getApparentRadius(PVCoordinatesProvider, Frame, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
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
    public void testOutOfBoundAcosApparentRadius() throws PatriusException {
        final OneAxisEllipsoid ellipsoidInertial = new OneAxisEllipsoid(6378137.0,
            (6378137.0 - 6356752.314245179) / 6378137.0, new Frame(FramesFactory.getITRF(), Transform.IDENTITY,
                "inertialFrame", true), "");
        final Vector3D pos = new Vector3D(-264144.8224132271, 1472993.560163555, -6179291.330687755);
        final AbsoluteDate date = new AbsoluteDate(366803769, 0.5980079787259456);
        final PVCoordinatesProvider occultedBody = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 4829209871480022501L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate otherDate, final Frame otherFrame)
                throws PatriusException {
                return new PVCoordinates(new Vector3D(1, 2, 3), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return FramesFactory.getGCRF();
            }
        };
        try {
            ellipsoidInertial.getApparentRadius(new ConstantPVCoordinatesProvider(pos, FramesFactory.getGCRF()),
                date, occultedBody, PropagationDelayType.INSTANTANEOUS);
        } catch (final ArithmeticException e) {
            Assert.fail();
        }
        Assert.assertTrue(true);
    }

    /**
     * Test needed to validate the resize of an OneAxisEllipsoid.
     * 
     * @testedMethod {@link OneAxisEllipsoid#resize(MarginType, double)}
     * 
     * @throws PatriusException if the precession-nutation model data embedded in the library cannot be read or if the
     *         margin type is invalid
     */
    @Test
    public void testResize() throws PatriusException {
        // Initialization
        final double tolerance = 1E-11;
        final OneAxisEllipsoid model = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "");
        // Case with a zero margin distance
        final double marginValue2 = 0.;
        final OneAxisEllipsoid model2 = model.resize(MarginType.DISTANCE, marginValue2);
        Assert.assertEquals(
            1. - (model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue2)
                    / model2.getEquatorialRadius(), model2.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() + marginValue2, model2.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue2,
            model2.getEquatorialRadius() * (1. - model2.getFlattening()), tolerance);
        // Case with a positive margin distance
        final double marginValue3 = 1E3;
        final OneAxisEllipsoid model3 = model.resize(MarginType.DISTANCE, marginValue3);
        Assert.assertEquals(
            1. - (model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue3)
                    / model3.getEquatorialRadius(), model3.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() + marginValue3, model3.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) + marginValue3,
            model3.getEquatorialRadius() * (1. - model3.getFlattening()), tolerance);
        // Case with a negative margin distance smaller than the opposite of the polar (smallest) radius
        final double marginValue4 = -model.getEquatorialRadius() * (1 - model.getFlattening()) + 1E-3;
        final OneAxisEllipsoid model4 = model.resize(MarginType.DISTANCE, marginValue4);
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
        final OneAxisEllipsoid model7 = model.resize(MarginType.SCALE_FACTOR, marginValue7);
        Assert.assertEquals(model.getFlattening(), model7.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * marginValue7, model7.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) * marginValue7,
            model7.getEquatorialRadius() * (1. - model7.getFlattening()), tolerance);
        // Case with a positive margin scale factor equal to 1
        final double marginValue8 = 1.;
        final OneAxisEllipsoid model8 = model.resize(MarginType.SCALE_FACTOR, marginValue8);
        Assert.assertEquals(model.getFlattening(), model8.getFlattening(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * marginValue8, model8.getEquatorialRadius(), tolerance);
        Assert.assertEquals(model.getEquatorialRadius() * (1. - model.getFlattening()) * marginValue8,
            model8.getEquatorialRadius() * (1. - model8.getFlattening()), tolerance);
        // Case with a positive margin scale factor smaller than 1
        final double marginValue9 = 0.5;
        final OneAxisEllipsoid model9 = model.resize(MarginType.SCALE_FACTOR, marginValue9);
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
     * @testType UT
     * 
     * @testedFeature {@link features#SPHEROID_BODY_SHAPE}
     * 
     * @testedMethod {@link OneAxisEllipsoid#getApparentRadius(PVCoordinatesProvider, Frame, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
     * 
     * @description Check computation of apparent radius is correct depending on signal propagation. This test
     *              considers a nearly flat bodyshape with far observer and close occulted body. Occulted body is behind
     *              occulting body. Observer body moves from equatorial plane to
     *              polar plane in a time equal to signal propagation duration. For simplicity, case in in ICRF frame
     * 
     * @testPassCriteria apparent radius is as expected (reference: math, absolute threshold: 0)
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void testGetApparentRadius() throws PatriusException {
        // Initialization
        final Frame frame = FramesFactory.getICRF();
        // Reception date
        final AbsoluteDate origin = AbsoluteDate.J2000_EPOCH;
        final double equatorialRadius = 1000;
        final double polarRadius = 500;
        final double f = (equatorialRadius - polarRadius) / equatorialRadius;
        final double transfertDuration = 10.;
        final double deltaxyz = 100.;
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(1000, f, frame, "");
        // Occulted body is behind (along J) occulting body
        final PVCoordinatesProvider occultedBody = new ConstantPVCoordinatesProvider(
                Vector3D.PLUS_J.scalarMultiply(deltaxyz), frame);
        final PVCoordinatesProvider occultedBody2 = new ConstantPVCoordinatesProvider(
                Vector3D.PLUS_J.scalarMultiply(transfertDuration * Constants.SPEED_OF_LIGHT), frame);

        // Observer is moving from equatorial to polar plane such that transfert time (10s) between this two planes is
        // equal to signal propagation duration between source and observer
        final PVCoordinatesProvider observer = new PVCoordinatesProvider() {
            /** Serializable UID. */
            private static final long serialVersionUID = 3675470733301070434L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                // Duration since emission
                final double duration = date.durationFrom(origin.shiftedBy(-transfertDuration));
                return new PVCoordinates(new Vector3D(deltaxyz * (1 - duration / 10.), -transfertDuration
                        * Constants.SPEED_OF_LIGHT, deltaxyz * duration / 10.), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return FramesFactory.getGCRF();
            }
        };

        final PVCoordinatesProvider observer2 = new PVCoordinatesProvider() {
            /** Serializable UID. */
            private static final long serialVersionUID = 2653776250918773014L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                // Duration since emission
                final double duration = date.durationFrom(origin.shiftedBy(-transfertDuration));
                return new PVCoordinates(new Vector3D(deltaxyz * (1 - duration / 10.), -deltaxyz, deltaxyz * duration
                        / 10.), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return FramesFactory.getGCRF();
            }
        };

        // No light speed taken into account (PropagationDelayType.INSTANTANEOUS)
        // At t = 0s, observer is along X axis of body, at t = 10s, observer is along Z axis of body
        final double radiusEquatorialInst = ellipsoid.getApparentRadius(observer, origin.shiftedBy(-transfertDuration),
                occultedBody, PropagationDelayType.INSTANTANEOUS);
        final double radiusPolarInst = ellipsoid.getApparentRadius(observer, origin, occultedBody,
                PropagationDelayType.INSTANTANEOUS);
        Assert.assertEquals(0., (equatorialRadius - radiusEquatorialInst) / equatorialRadius, 1E-10);
        Assert.assertEquals(0., (polarRadius - radiusPolarInst) / polarRadius, 1E-10);

        // Light speed is taken into account (PropagationDelayType.LIGHT_SPEED)
        // Observer at 10s-light from occulting body
        // Occulted body very close to occulting body
        // Received light passes over polar radius of occulting body
        // At signal reception, observer is along Z axis of body (polar radius = 500m)
        final double radiusLS1 = ellipsoid.getApparentRadius(observer, origin, occultedBody,
                PropagationDelayType.LIGHT_SPEED);
        Assert.assertEquals(0., (polarRadius - radiusLS1) / polarRadius, 1E-10);

        // Light speed is taken into account (PropagationDelayType.LIGHT_SPEED)
        // Observer very close to occulting body
        // Occulted body at 10s-light from occulting body
        // Received light passes over polar radius of occulting body
        // At signal reception, observer is along Z axis of body (polar radius = 500m)
        final double radiusLS2 = ellipsoid.getApparentRadius(observer2, origin, occultedBody2,
                PropagationDelayType.LIGHT_SPEED);
        Assert.assertEquals(0., (polarRadius - radiusLS2) / polarRadius, 1E-10);
    }

    /**
     * Compute distance to ellipsoid.
     * 
     * @param requa
     *        ellipsoid equatorial radius
     * @return list of distance to ellipsoid
     * @throws PatriusException
     */
    private static List<Double> recordDistanceToEllipsoid(final double requa) throws PatriusException {

        // Reference Frame
        final Frame gcrf = FramesFactory.getGCRF();

        final List<Double> listRes = new ArrayList<>();

        // Ellipsoid
        final double rpole = requa * (1 - Constants.WGS84_EARTH_FLATTENING);
        final OneAxisEllipsoid elli =
            new OneAxisEllipsoid(requa, (requa - rpole) / requa, gcrf, "Terre");

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
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(1);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4335, -0.9011, -0.0076).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(5);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4319, -0.9011, -0.0378).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(10);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4271, -0.9010, -0.0753).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(15);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.4191, -0.9009, -0.1123).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(30);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.3766, -0.9005, -0.2174).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(45);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.3084, -0.8999, -0.3084).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(60);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.2187, -0.8993, -0.3787).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(80);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0761, -0.8988, -0.4318).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(85);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0382, -0.8987, -0.4369).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(89);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0077, -0.8987, -0.4385).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(90);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(-0.0000, -0.8987, -0.4386).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(91);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(0.0077, -0.8987, -0.4383).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);
        //
        teta = MathLib.toRadians(120);
        pos = new Vector3D(r * MathLib.cos(teta), 0, r * MathLib.sin(teta));
        dir1 = new Vector3D(0.2187, -0.8993, -0.3760).normalize();
        dir2 = rotateAzim(pos, dir1, MathLib.toRadians(30));
        dir3 = rotateAzim(pos, dir1, MathLib.toRadians(90));
        testsDistLineEllipsoid(gcrf, elli, pos, dir1, dir2, dir3, west, est, polaris, listRes);

        return listRes;
    }

    /**
     * Rotate vector.
     */
    private static Vector3D rotateAzim(final Vector3D pos, final Vector3D dir0, final double angle) {
        final Rotation rot = new Rotation(pos.negate(), angle);
        return rot.applyTo(dir0).normalize();
    }

    /**
     * Compute distance to ellipsoid.
     * 
     * @throws PatriusException
     */
    private static void testsDistLineEllipsoid(final Frame frame, final OneAxisEllipsoid elli, final Vector3D pos,
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

    private static void checkCartesianToEllipsoidic(final double ae, final double f,
                                                    final double x, final double y, final double z,
                                                    final double longitude, final double latitude,
                                                    final double altitude)
        throws PatriusException {
        checkCartesianToEllipsoidic(ae, f, x, y, z, longitude, latitude, altitude, false);
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
    private static void checkCartesianToEllipsoidic(final double ae, final double f,
                                                    final double x, final double y, final double z,
                                                    final double longitude, final double latitude,
                                                    final double altitude, final boolean writeToReport)
        throws PatriusException {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(ae, f, frame, "spheroid");
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

    /**
     * tests the jacobian matrix obtained with
     * {@link OneAxisEllipsoid#transformAndJacobian(Vector3D, Frame, AbsoluteDate, double[][])}
     * 
     * References for the resutls : MSLIB
     * 
     * @throws PatriusException
     */
    @Test
    public void testJacobianCartesianToGeodeticMSLIB() throws PatriusException {

        final double[][] jacobian =
        { { -0.107954488167401 * 1E-06, -0.282449738801487 * 1E-08, 0.114080171824722 * 1E-06 },
            { -0.563745742240173 * 1E-08, 0.215468009700879 * 1E-06, 0.000000000000000 * 1E+00 },
            { 0.725972822728309 * 1E+00, 0.189941926118555 * 1E-01, 0.687461039846560 * 1E+00 }
        };

        // nominal case
        // checks if the result is the expected one
        checkCartesianToEllipsoidicJacobian(6378137, 1.0 / 298.257222101,
            4637885.347, 121344.608, 4362452.869, jacobian);

        // case : point on the pole (an error should be thrown)
        try {
            checkCartesianToEllipsoidicJacobian(6378137.0, 1.0 / 298.257222101,
                0.0, 0.0, 7000000.0, new double[3][3]);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException ex) {
            // excpeted
        }
    }

    /**
     * Checks if the jacobian matrix obtained with
     * {@link OneAxisEllipsoid#transformAndJacobian(Vector3D, Frame, AbsoluteDate, double[][])} if the same than
     * the
     * reference
     * 
     * @param ae
     *        : equatorial radius of the body
     * @param f
     *        : flatness of the body
     * @param x
     *        : first Cartesian coordinate of the point that has to be transformed
     * @param y
     *        : second Cartesian coordinate of the point that has to be transformed
     * @param z
     *        : third Cartesian coordinate of the point that has to be transformed
     * @param jacobian
     *        : reference jacobian matrix
     * 
     * @throws PatriusException
     */
    private static void checkCartesianToEllipsoidicJacobian(final double ae, final double f,
                                                            final double x, final double y, final double z,
                                                            final double[][] jacobian)
        throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(ae, f, frame);
        final double[][] computedJacobian = new double[3][3];
        model.transformAndComputeJacobian(new Vector3D(x, y, z), frame, date, computedJacobian);
        checkMatrix(computedJacobian, jacobian);
    }

    /**
     * Compares component to component two matrices. The comparison is relative, the epsilon used is the epsilon
     * for the tests defined in the class {@link Utils}
     * 
     * @param a
     *        : first matrix
     * @param b
     *        : second matrix
     */
    private static void checkMatrix(final double[][] a, final double[][] b) {
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[0][0], b[0][0], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[0][0], b[0][0], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[0][1], b[0][1], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[0][2], b[0][2], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[1][0], b[1][0], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[1][1], b[1][1], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[1][2], b[1][2], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[2][0], b[2][0], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[2][1], b[2][1], Utils.epsilonTest));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(a[2][2], b[2][2], Utils.epsilonTest));
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the ellipsoid serialization / deserialization process.
     *
     * @testPassCriteria The ellipsoid can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final Frame frame = FramesFactory.getITRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D point = new Vector3D(0.0, -93.7139699, -3.5930796);
        final Vector3D direction = new Vector3D(0.0, -1.0, -1.0);
        final Line line = new Line(point, point.add(direction)).revert();

        final OneAxisEllipsoid elipsoid = new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101,
                frame, "spheroid");
        final OneAxisEllipsoid deserializedElipsoid = TestUtils.serializeAndRecover(elipsoid);

        final GeodeticPoint gp1 = elipsoid.getIntersectionPoint(line, point, frame, date);
        final GeodeticPoint gp2 = deserializedElipsoid.getIntersectionPoint(line, point, frame,
                date);
        Assert.assertEquals(gp1.getLatitude(), gp2.getLatitude(), 0.);
        Assert.assertEquals(gp1.getLongitude(), gp2.getLongitude(), 0.);
        Assert.assertEquals(gp1.getAltitude(), gp2.getAltitude(), 0.);

        Assert.assertEquals(elipsoid.getPVCoordinates(date, frame),
                deserializedElipsoid.getPVCoordinates(date, frame));

        Assert.assertEquals(elipsoid.getName(), deserializedElipsoid.getName());
        Assert.assertEquals(elipsoid.getEquatorialRadius(),
                deserializedElipsoid.getEquatorialRadius(), 0.);
        Assert.assertEquals(elipsoid.getFlattening(), deserializedElipsoid.getFlattening(), 0.);
        Assert.assertEquals(elipsoid.getName(), deserializedElipsoid.getName());
        Assert.assertEquals(elipsoid.getName(), deserializedElipsoid.getName());
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * tests the jacobian matrix obtained with {@link OneAxisEllipsoid#transformAndJacobian(GeodeticPoint, double[][])}
     * 
     * References for the resutls : MSLIB
     * 
     * @throws PatriusException
     */
    @Test
    public void testJacobianGeodeticToCartesianMSLIB() throws PatriusException {
        // nominal case
        final double[][] jacobian = {
            { -0.479311467789823 * 1E+07, -0.177908184625686 * 1E+06, 0.657529466860734 },
            { -0.202941785964951 * 1E+06, 0.420186669291890 * 1E+07, 0.278399774043822 * 1E-01 },
            { .419339100580230 * 1E+07, 0.000000000000000, 0.752914295167758 }
        };

        final double[][] computedJacobian = new double[3][3];

        final OneAxisEllipsoid model =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF());

        final GeodeticPoint nsp = new GeodeticPoint(0.852479154923577, 0.0423149994747243, 111.6);
        model.transformAndComputeJacobian(nsp, computedJacobian);

        // checks if the computed matrix and the expected one are the same
        checkMatrix(computedJacobian, jacobian);

        // case : the flatness is above 1 (an error should be thrown)
        try {
            new OneAxisEllipsoid(6378137.0, 1.1, FramesFactory.getITRF());
            // model2.transformAndComputeJacobian(nsp, computedJacobian);
            Assert.fail("an exception should have been thrown");
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertEquals(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN.getLocalizedString(Locale.getDefault()),
                e.getLocalizedMessage());
        }
    }
    
    /**
     * @testType UT
     * 
     * @description check that method closestPointTo returns the two expected points
     * 
     * @testPassCriteria the two points (one from the shape and one from the line) are those expected (reference math),
     *                   with or without intersection of infinite and semi-finite lines
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     * 
     * @throws PatriusException if an error occurs with line transformation
     */
    @Test
    public void closestPointToTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final double radius = 10E3;
        final OneAxisEllipsoid body = new OneAxisEllipsoid(radius, 0., frame);

        // Case 1: infinite line with intersection (basic case: body center pointing)
        final Vector3D position1 = new Vector3D(10E3, 20E3, 30E3);
        final Line lineOfSight1 = new Line(position1, Vector3D.ZERO);
        final Vector3D[] actual1 = body.closestPointTo(lineOfSight1, frame, date);
        final Vector3D reference1 = position1.normalize().scalarMultiply(body.getEquatorialRadius());
        Assert.assertEquals(0., Vector3D.distance(reference1, actual1[0]) / reference1.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual1[1]) / reference1.getNorm(), 1E-3);
        
        // Case 2: same as case 1 with semi-finite line, min abscissa before intersection (points shall be the same)
        final Line lineOfSight2 = new Line(position1, Vector3D.ZERO, position1);
        final Vector3D[] actual2 = body.closestPointTo(lineOfSight2, frame, date);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual2[0]) / reference1.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual2[1]) / reference1.getNorm(), 1E-3);

        // Case 3: same as case 1 with inverted semi-finite line, no intersection due to min abscissa
        final Line lineOfSight3 = new Line(Vector3D.ZERO, position1, position1);
        final Vector3D[] actual3 = body.closestPointTo(lineOfSight3, frame, date);
        Assert.assertEquals(0., Vector3D.distance(reference1, actual3[1]) / reference1.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(position1, actual3[0]) / position1.getNorm(), 1E-3);

        // Case 4: infinite line with tangent intersection
        final Vector3D position2 = new Vector3D(10E3, -20E3, 15E3);
        final Line lineOfSight4 = new Line(position2, new Vector3D(10E3, 20E3, -15E3));
        final Vector3D[] actual4 = body.closestPointTo(lineOfSight4, frame, date);
        final Vector3D reference2 = new Vector3D(10E3, 0, 0);
        Assert.assertEquals(0., Vector3D.distance(reference2, actual4[1]) / reference2.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference2, actual4[0]) / reference2.getNorm(), 1E-3);

        // Case 5: same as case 4 with semi-finite line, min abscissa before intersection (points shall be the same)
        final Line lineOfSight5 = new Line(position2, new Vector3D(10E3, 20E3, -15E3), position2);
        final Vector3D[] actual5 = body.closestPointTo(lineOfSight5, frame, date);
        Assert.assertEquals(0., Vector3D.distance(reference2, actual5[1]) / reference2.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference2, actual5[0]) / reference2.getNorm(), 1E-3);

        // Case 6: same as case 4 with inverted semi-finite line, no intersection due to min abscissa
        final Line lineOfSight6 = new Line(new Vector3D(10E3, 20E3, -15E3), position2, position2);
        final Vector3D[] actual6 = body.closestPointTo(lineOfSight6, frame, date);
        Vector3D projOfMinAbs = new Vector3D(radius, position2.normalize());
        Assert.assertEquals(0., Vector3D.distance(projOfMinAbs, actual6[1]) / projOfMinAbs.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(position2, actual6[0]) / position2.getNorm(), 1E-3);

        // Case 7: infinite line without intersection
        final Vector3D position3 = new Vector3D(15E3, -20E3, 15E3);
        final Line lineOfSight7 = new Line(position3, new Vector3D(15E3, 20E3, -15E3));
        final Vector3D[] actual7 = body.closestPointTo(lineOfSight7, frame, date);
        final Vector3D reference3body = new Vector3D(10E3, 0, 0);
        final Vector3D reference3line = new Vector3D(15E3, 0, 0);
        Assert.assertEquals(0., Vector3D.distance(reference3body, actual7[1]) / reference3body.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference3line, actual7[0]) / reference3line.getNorm(), 1E-3);

        // Case 8: same as case 7 with semi-finite line, min abscissa before closest point (points shall be the same)
        final Line lineOfSight8 = new Line(position3, new Vector3D(15E3, 20E3, -15E3), position3);
        final Vector3D[] actual8 = body.closestPointTo(lineOfSight8, frame, date);
        Assert.assertEquals(0., Vector3D.distance(reference3body, actual8[1]) / reference3body.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(reference3line, actual8[0]) / reference3line.getNorm(), 1E-3);

        // Case 9: same as case 7 with inverted semi-finite line, min abscissa after supposed closest point
        final Line lineOfSight9 = new Line(new Vector3D(15E3, 20E3, -15E3), position3, position3);
        final Vector3D[] actual9 = body.closestPointTo(lineOfSight9, frame, date);
        projOfMinAbs = new Vector3D(radius, position3.normalize());
        Assert.assertEquals(0., Vector3D.distance(projOfMinAbs, actual9[1]) / projOfMinAbs.getNorm(), 1E-3);
        Assert.assertEquals(0., Vector3D.distance(position3, actual9[0]) / position3.getNorm(), 1E-3);

    }
    
}
