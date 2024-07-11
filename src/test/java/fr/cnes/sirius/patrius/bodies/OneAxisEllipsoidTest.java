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
* VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection a altitude
* VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:531:10/02/2016:Robustification of convergence of transform() method
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ReentryParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class OneAxisEllipsoidTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(OneAxisEllipsoidTest.class.getSimpleName(), "One axis ellipsoid");
    }

    @Test
    public void testOrigin() throws PatriusException {
        final double ae = 6378137.0;
        this.checkCartesianToEllipsoidic(ae, 1.0 / 298.257222101,
            ae, 0, 0,
            0, 0, 0);
    }

    @Test
    public void testStandard() throws PatriusException {

        Report.printMethodHeader("testStandard", "Geodetic coordinates computation", "Orekit", 1E-10,
            ComparisonType.ABSOLUTE);

        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            4637885.347, 121344.608, 4362452.869,
            0.026157811533131, 0.757987116290729, 260.455572965555, true);
    }

    /**
     * Comparison between the results given by MSLIB and those obtained with Orekit concerning the transformation of
     * coordinates
     * from cartesian to geodesic.
     * 
     * Case : standard
     * 
     * the differences on the references : on the altitude (difference of 1.8 1E-10)
     * 
     * References for the results : MSLIB
     * 
     * @throws PatriusException
     */
    @Test
    public void testStandardMSLIB() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137, 1.0 / 298.257222101,
            4637885.347, 121344.608, 4362452.869,
            0.0261578115331310, 0.757987116290729, 260.455572965371);
    }

    @Test
    public void testLongitudeZero() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            6378400.0, 0, 6379000.0,
            0.0, 0.787815771252351, 2653416.77864152);
    }

    @Test
    public void testLongitudePi() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            -6379999.0, 0, 6379000.0,
            3.14159265358979, 0.787690146758403, 2654544.7767725);
    }

    @Test
    public void testNorthPole() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            0.0, 0.0, 7000000.0,
            0.0, 1.57079632679490, 643247.685859644);
    }

    @Test
    public void testEquator() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
            6379888.0, 6377000.0, 0.0,
            0.785171775899913, 0.0, 2642345.24279301);
    }

    @Test
    public void testInside3Roots() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
            9219.0, -5322.0, 6056743.0,
            5.75963470503781, 1.56905114598949, -300000.009586231);
    }

    @Test
    public void testInsideLessThan3Roots() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
            1366863.0, -789159.0, -5848.988,
            -0.523598928689, -0.00380885831963, -4799808.27951);
    }

    /**
     * Comparison between the results given by MSLIB and those obtained with Orekit concerning the transformation of
     * coordinates
     * from cartesian to geodesic.
     * 
     * Case : point inside with less than 3 roots
     * 
     * the differences on the references : on z (difference of 1.8 1E3)
     * 
     * References for the results : MSLIB
     * 
     * @throws PatriusException
     */

    @Test
    public void testInsideLessThan3RootsMSLIB() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137, 1.0 / 298.257,
            1366863, -789159, -5848988,
            5.75958637849098, -1.30899688879521, -299999.833065513);
    }

    @Test
    public void testOutside() throws PatriusException {
        this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
            5722966.0, -3304156.0, -24621187.0,
            5.75958652642615, -1.3089969725151, 19134410.3342696);
    }

    /**
     * Added test for the transformation from Cartesian coordinates to geodetic coordinates,
     * case when the point is at the center.
     * 
     * References for the results : MSLIB
     * 
     * @throws PatriusException
     */
    @Test
    public void testCenter() throws PatriusException {
        final double ae = 6378137.0;
        final double f = 1.0 / 298.257222101;
        this.checkCartesianToEllipsoidic(ae, f,
            0, 0, 0,
            0, 1.57079632679490, -6356752.314140356);
    }

    /**
     * Added test to cover the case when the algorithm that transforms Cartesian coordinates
     * into geodetic coordinates does not converge
     * 
     * @throws PatriusException
     */
    @Test
    public void testNonConvergence() throws PatriusException {
        // case : the algorithm does not converge
        try {
            this.checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                100, 100, 100, 0, 0, 0);
            Assert.fail("an exception should have been thrown");
        } catch (final RuntimeException ex) {
            // Excepted
        }
    }

    /**
     * Added test to cover the case where we have 3 real roots.
     * 
     * References for the results : MSLIB
     * 
     * @see <a href="http://www.spaceroots.org/documents/distance/distance-to-ellipse.pdf">Quick computation of the
     *      distance between a point and an ellipse</a>
     * @throws PatriusException
     */
    @Test
    public void testInside3RootsFlatBodyMSLIB() throws PatriusException {
        this.checkCartesianToEllipsoidic(100, 0.9,
            0, 10, 4,
            1.5707963267948965, 1.5606852181535817, -5.949572069364308);
        this.checkCartesianToEllipsoidic(100, 0.9,
            0, 10, -4,
            1.5707963267948965, -1.5606852181535817, -5.949572069364308);
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
        this.checkCartesianToEllipsoidicJacobian(6378137, 1.0 / 298.257222101,
            4637885.347, 121344.608, 4362452.869, jacobian);

        // case : point on the pole (an error should be thrown)
        try {
            this.checkCartesianToEllipsoidicJacobian(6378137.0, 1.0 / 298.257222101,
                0.0, 0.0, 7000000.0, new double[3][3]);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException ex) {
            // excpeted
        }
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
        this.checkMatrix(computedJacobian, jacobian);

        // case : the flatness is above 1 (an error should be thrown)
        final OneAxisEllipsoid model2 =
            new OneAxisEllipsoid(6378137.0, 1.1, FramesFactory.getITRF());

        try {
            model2.transformAndComputeJacobian(nsp, computedJacobian);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException ex) {
            // expected
        }
    }

    @Test
    public void testGeoCar() throws PatriusException {
        final OneAxisEllipsoid model =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101,
                FramesFactory.getITRF());
        final GeodeticPoint nsp =
            new GeodeticPoint(0.852479154923577, 0.0423149994747243, 111.6);
        final Vector3D p = model.transform(nsp);
        Assert.assertEquals(4201866.69291890, p.getX(), 1.0e-6);
        Assert.assertEquals(177908.184625686, p.getY(), 1.0e-6);
        Assert.assertEquals(4779203.64408617, p.getZ(), 1.0e-6);

        Assert.assertTrue(Precision.equalsWithRelativeTolerance(4201866.69291890, p.getX()));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(177908.184625686, p.getY()));
        Assert.assertTrue(Precision.equalsWithRelativeTolerance(4779203.64408617, p.getZ()));

    }

    @Test
    public void testLineIntersection() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();

        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame);
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

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(0.0, -93.7139699, 3.5930796);
        direction = new Vector3D(0.0, -1.0, 1.0);
        line = new Line(point, point.add(direction));
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
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

    }

    @Test
    public void testGetIntersectionPoint() throws PatriusException {
        // Set data common to all cases
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame);

        // Case with b2 < ac
        Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction = new Vector3D(0.0, 9.0, -2.0);
        Vector3D pointMinAbscissa = new Vector3D(0.0, 90, 0);
        Line line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with b2 >= ac, k = k1 and abscissa(k1) > abscissa min
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, -1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, 110, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNotNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with b2 >= ac, k = k1, abscissa(k1) <= abscissa min and abscissa(k2) > abscissa min
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, -1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, 90, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNotNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with b2 >= ac, k = k1, abscissa(k1) <= abscissa min and abscissa(k2) <= abscissa min
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, -1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, -110, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with b2 >= ac, k = k2 and abscissa(k2) > abscissa min
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, 1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, 90, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNotNull(model.getIntersectionPoint(line, point, frame, date));

        // Case with b2 >= ac, k = k2, abscissa(k2) <= abscissa min and abscissa(k1) <= abscissa min
        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, 1.0, 0.0);
        pointMinAbscissa = new Vector3D(0.0, 110, 0);
        line = new Line(point, point.add(direction), pointMinAbscissa);
        Assert.assertNull(model.getIntersectionPoint(line, point, frame, date));
    }

    @Test
    public void testNoLineIntersection() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame);
        final Vector3D point = new Vector3D(0.0, 93.7139699, 3.5930796);
        final Vector3D direction = new Vector3D(0.0, 9.0, -2.0);
        final Line line = new Line(point, point.add(direction));
        Assert.assertNull(model.getIntersectionPoint(line, point, frame, date));
    }

    @Test
    public void testIntersectionFromPoints() throws PatriusException {

        Report.printMethodHeader("testIntersectionFromPoints", "Intersection point computation", "Orekit",
            Utils.epsilonAngle, ComparisonType.ABSOLUTE);

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 03, 21),
            TimeComponents.H12,
            TimeScalesFactory.getUTC());

        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frame);

        // Satellite on polar position
        // ***************************
        final double mu = 3.9860047e14;
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., MathLib.toRadians(90.), MathLib.toRadians(60.),
                MathLib.toRadians(90.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();
        PVCoordinates pvSatItrf = frame.getTransformTo(FramesFactory.getEME2000(), date).transformPVCoordinates(
            pvSatEME2000);
        Vector3D pSatItrf = pvSatItrf.getPosition();

        // Test first visible surface points
        GeodeticPoint geoPoint = new GeodeticPoint(MathLib.toRadians(70.), MathLib.toRadians(60.), 0.);
        Vector3D pointItrf = earth.transform(geoPoint);
        Line line = new Line(pSatItrf, pointItrf);
        GeodeticPoint geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test second visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(65.), MathLib.toRadians(-120.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(30.), MathLib.toRadians(60.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);

        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);

        // For polar satellite position, intersection point is at the same longitude but different latitude
        Assert.assertEquals(1.04437199, geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(1.36198012, geoInter.getLatitude(), Utils.epsilonAngle);

        // Satellite on equatorial position
        // ********************************
        circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., MathLib.toRadians(1.e-4), MathLib.toRadians(0.),
                MathLib.toRadians(0.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        pvSatEME2000 = circ.getPVCoordinates();
        pvSatItrf = frame.getTransformTo(FramesFactory.getEME2000(), date).transformPVCoordinates(pvSatEME2000);
        pSatItrf = pvSatItrf.getPosition();

        // Test first visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(5.), MathLib.toRadians(0.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);
        Assert.assertTrue(line.toSubSpace(pSatItrf).getX() < 0);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // With the point opposite to satellite point along the line
        final GeodeticPoint geoInter2 = earth.getIntersectionPoint(line,
            line.toSpace(new Vector1D(-line.toSubSpace(pSatItrf).getX())), frame, date);
        Assert.assertTrue(MathLib.abs(geoInter.getLongitude() - geoInter2.getLongitude()) > MathLib.toRadians(0.1));
        Assert.assertTrue(MathLib.abs(geoInter.getLatitude() - geoInter2.getLatitude()) > MathLib.toRadians(0.1));

        // Test second visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(-5.), MathLib.toRadians(0.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(40.), MathLib.toRadians(0.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(-0.00768481, geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(0.32180410, geoInter.getLatitude(), Utils.epsilonAngle);

        // Satellite on any position
        // *************************
        circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., MathLib.toRadians(50.), MathLib.toRadians(0.),
                MathLib.toRadians(90.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        pvSatEME2000 = circ.getPVCoordinates();
        pvSatItrf = frame.getTransformTo(FramesFactory.getEME2000(), date).transformPVCoordinates(pvSatEME2000);
        pSatItrf = pvSatItrf.getPosition();

        // Test first visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(40.), MathLib.toRadians(90.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test second visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(60.), MathLib.toRadians(90.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(90.), 0.);
        pointItrf = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(MathLib.toRadians(89.5364061088196), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(MathLib.toRadians(35.555543683351125), geoInter.getLatitude(), Utils.epsilonAngle);

        Report.printToReport("Longitude", MathLib.toRadians(89.5364061088196), geoInter.getLongitude());
        Report.printToReport("Latitude", MathLib.toRadians(35.555543683351125), geoInter.getLatitude());
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link OneAxisEllipsoid#getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}
     * 
     * @description Checks intersection between a line and an ellipsoid at a given altitude on two cases:
     *              <ul>
     *              <li>Intersection at altitude = 100m: altitude of computed points should be 100m (accuracy: 1E-3m)</li>
     *              <li>Intersection at altitude = 0m: altitude of computed points should be 0m (accuracy: 1E-9m)</li>
     *              <li>Intersection at altitude = 1E-4m: altitude of computed points should be 0m (accuracy: 0m)</li>
     *              </ul>
     * 
     * @testPassCriteria altitude of computed points are as expected. Points are on the initial line.
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void intersectionPointsAltitudeTest() throws PatriusException {
        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        OneAxisEllipsoid model = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, frame);
        
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
        Assert.assertEquals(gp2.getAltitude(), 0.0, 1E-9);
        Assert.assertTrue(line2.distance(model.transform(gp2)) < 1E-8);

        // No intersection test
        final Vector3D point3 = new Vector3D(1E9, 5E9, 10E9);
        final Vector3D direction3 = new Vector3D(1.0, 1.0, 1.0);
        final Line line3 = new Line(point3, point3.add(direction3));
        final GeodeticPoint gp3 = model.getIntersectionPoint(line3, point3, frame, date, 0);
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
     * FA-531: test robust convergence.
     */
    @Test
    public final void testTransformRobustConvergence() {

        // Constants
        final double eqRad = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double flat = Constants.GRIM5C1_EARTH_FLATTENING;
        final double mu = Constants.EGM96_EARTH_MU;

        // Cartesian parameters: these parameters lead to a non convergence of transform() algorithm
        final Vector3D position = new Vector3D(-8.582052568218349E7, 3.8804626313961256E8, 1.08706297081165E7);
        final Vector3D velocity = new Vector3D(-132.5724594357039, -887.1215915899561, -38.754386754805886);
        final PVCoordinates pv = new PVCoordinates(position, velocity);
        final CartesianParameters carP = new CartesianParameters(pv, mu);

        // Reentry parameters
        final ReentryParameters reenParam = carP.getReentryParameters(eqRad, flat);
        final GeodeticPoint point = new GeodeticPoint(reenParam.getLatitude(), reenParam.getLongitude(),
            reenParam.getAltitude());

        // Reverse transformation
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(eqRad, flat, null);
        final Vector3D actual = ellipsoid.transform(point);

        // Check position
        Assert.assertEquals(0., MathLib.abs((position.getX() - actual.getX()) / position.getNorm()), 1E-14);
        Assert.assertEquals(0., MathLib.abs((position.getY() - actual.getY()) / position.getNorm()), 1E-14);
        Assert.assertEquals(0., MathLib.abs((position.getZ() - actual.getZ()) / position.getNorm()), 1E-14);
    }

    private void checkCartesianToEllipsoidic(final double ae, final double f,
                                             final double x, final double y, final double z,
                                             final double longitude, final double latitude,
                                             final double altitude)
                                                                   throws PatriusException {
        this.checkCartesianToEllipsoidic(ae, f, x, y, z, longitude, latitude, altitude, false);
    }

    private void checkCartesianToEllipsoidic(final double ae, final double f,
                                             final double x, final double y, final double z,
                                             final double longitude, final double latitude,
                                             final double altitude, final boolean writeToReport)
                                                                                                throws PatriusException {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(ae, f, frame);
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
     * Checks if the jacobian matrix obtained with
     * {@link OneAxisEllipsoid#transformAndJacobian(Vector3D, Frame, AbsoluteDate, double[][])} if the same than the
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
    private void checkCartesianToEllipsoidicJacobian(final double ae, final double f,
                                                     final double x, final double y, final double z,
                                                     final double[][] jacobian)
                                                                               throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getITRF();
        final OneAxisEllipsoid model = new OneAxisEllipsoid(ae, f, frame);
        final double[][] computedJacobian = new double[3][3];
        model.transformAndComputeJacobian(new Vector3D(x, y, z), frame, date, computedJacobian);
        this.checkMatrix(computedJacobian, jacobian);
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
    private void checkMatrix(final double[][] a, final double[][] b) {
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

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
