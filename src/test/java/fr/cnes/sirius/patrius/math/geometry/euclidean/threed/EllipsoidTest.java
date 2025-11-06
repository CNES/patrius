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
 * @history Created on 06/10/2011
 *
 * HISTORY
 * VERSION:4.16:OPENFD-407:25/04/2025:[PATRIUS] Methode toString de Vector3D pas assez precise
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-576:25/04/2025:[PATRIUS] Probleme de convergence dans Ellipsoid.runNewtonAlgorithmLine
 * VERSION:4.15:OPENFD-384:21/11/2024:[PATRIUS] Non convergence de l'algo d'intersection avec un ellipsoïde
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.15:OPENFD-399:21/11/2024:problème de convergence dans EllipsoidPoint.closestPointTo
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:650:22/07/2016: ellipsoid corrections
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinates;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for {@link Ellipsoid}
 * </p>
 *
 * @see Ellipsoid
 *
 * @author Rami Houdroge
 *
 * @version $Id: EllipsoidTest.java 17909 2017-09-11 11:57:36Z bignon $
 *
 * @since 1.2
 *
 */
public class EllipsoidTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Ellipsoid shape
         *
         * @featureDescription Creation of a ellipsoid, computation of distances and intersections with lines and
         *                     points.
         *
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        ELLIPSOID_SHAPE,

        /**
         * @featureTitle Ellipsoid getters
         *
         * @featureDescription Test Ellipsoid getters
         *
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        ELLIPSOID_PROPS,

        /**
         * @featureTitle Ellipsoid basis transformations
         *
         * @featureDescription Test Ellipsoid basis transformations
         *
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        ELLIPSOID_BASISTRANSFORMATIONS,

        /**
         * @featureTitle Ellipsoid intersections
         *
         * @featureDescription Test Ellipsoid intersection algorithms
         *
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130,
         *                      DV-GEOMETRIE_140
         */
        ELLIPSOID_INTERSECTIONS,

        /**
         * @featureTitle Ellipsoid distance
         *
         * @featureDescription Test Ellipsoid distance computation algorithms
         *
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        ELLIPSOID_DISTANCES
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** A list of 5000 random points very close to the center of a spherical Earth */
    private ArrayList<Vector3D> points;

    /** The points nearest neighbors on the Earth's surface before OPENFD-399 */
    private ArrayList<Vector3D> coords;

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ELLIPSOID_SHAPE}
     *
     * @testedMethod {@link Ellipsoid#Ellipsoid(Vector3D, Vector3D, Vector3D, double, double, double)}
     *
     * @description Test Ellipsoid Constructor. Here we
     *              check the correctness of the Ellipsoid class constructor. Nominal case as well as degraded cases are
     *              checked. Once the test is passed, the method is considered correct and used afterwards.
     *
     * @input data
     *
     * @output Spheroid
     *
     * @testPassCriteria No exception is raised for nominal cases, an IllegalArgumentException is raised for degraded
     *                   cases. We check the returned elements with the ones given at the construction with an epsilon
     *                   of 1e-14 which takes into account the machine error only.
     *
     * @referenceVersion 2.0
     *
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testConstructor() {

        // Spheroid parameters
        final Vector3D position = Vector3D.ZERO;
        final Vector3D revAxis = Vector3D.PLUS_K;
        final Vector3D xAxis = Vector3D.PLUS_I;
        final double a = 1.0;
        double b = -1.0;
        final double c = 2;
        try {
            // create spheroid object
            new Ellipsoid(position, revAxis, xAxis, a, b, c);
            Assert.fail();
        } catch (final IllegalArgumentException e) {

        }
        b = 1.5;

        try {
            // new test
            new Ellipsoid(position, Vector3D.ZERO, xAxis, a, b, c);
            // new test // test getters
            Assert.fail();
        } catch (final MathArithmeticException e) {

        }
        try {
            new Ellipsoid(position, revAxis, Vector3D.ZERO, a, b, c);
            // test getters
            Assert.fail();
        } catch (final MathArithmeticException e) {

        }

        // create spheroid object
        Ellipsoid myEllipsoid = new Ellipsoid(position, revAxis, xAxis, 1, 1.5, .5);
        Assert.assertEquals(1, myEllipsoid.getSemiA(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, myEllipsoid.getSemiB(), this.comparisonEpsilon);
        Assert.assertEquals(.5, myEllipsoid.getSemiC(), this.comparisonEpsilon);

        // create spheroid object
        myEllipsoid = new Ellipsoid(position, revAxis, xAxis, 1, 1.5, 2);
        Assert.assertEquals(1, myEllipsoid.getSemiA(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, myEllipsoid.getSemiB(), this.comparisonEpsilon);
        Assert.assertEquals(2, myEllipsoid.getSemiC(), this.comparisonEpsilon);

        // create spheroid object
        myEllipsoid = new Ellipsoid(position, revAxis, xAxis, 1, .75, 2);
        Assert.assertEquals(1, myEllipsoid.getSemiA(), this.comparisonEpsilon);
        Assert.assertEquals(.75, myEllipsoid.getSemiB(), this.comparisonEpsilon);
        Assert.assertEquals(2, myEllipsoid.getSemiC(), this.comparisonEpsilon);

        // create spheroid object
        myEllipsoid = new Ellipsoid(position, revAxis, xAxis, 1, 1.5, 2);
        Assert.assertEquals(1, myEllipsoid.getSemiA(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, myEllipsoid.getSemiB(), this.comparisonEpsilon);
        Assert.assertEquals(2, myEllipsoid.getSemiC(), this.comparisonEpsilon);

        Assert.assertEquals(1, myEllipsoid.getSemiA(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, myEllipsoid.getSemiB(), this.comparisonEpsilon);
        Assert.assertEquals(2, myEllipsoid.getSemiC(), this.comparisonEpsilon);
    }

    /**
     * Testing distance and closestPointTo(Vector3D)
     *
     * @testType UT
     *
     * @testedFeature {@link features#ELLIPSOID_DISTANCES}
     *
     * @testedMethod {@link Ellipsoid#closestPointTo(Vector3D)}
     * @testedMethod {@link Ellipsoid#distanceTo(Vector3D)}
     *
     * @description Test Ellipsoid distance computation algorithms for distance to Vector3D.
     *
     * @input Vector3D
     *
     * @output Vector3D containing the closest computed point
     *
     * @testPassCriteria The expected result is the same as the predicted one with an epsilon of 1e-14 due to the
     *                   computation errors.
     *
     * @referenceVersion 2.0
     *
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testClosestPointToPart1() {

        // Params
        Vector3D center;
        Vector3D axis;
        Vector3D xaxis;
        double a;
        double b;
        double c;
        Ellipsoid ellipsoid;
        Vector3D aFarAwayPoint;
        Vector3D theClosestPoint;
        Vector3D expectedPoint;
        //
        /** test avec un ellipsoid sur l'axe X et un point en dehors */
        // definition d'un ellipsoide de revolution
        center = new Vector3D(0, 0, 0);
        axis = new Vector3D(0, 0, 1);
        xaxis = new Vector3D(1, 0, 0);
        a = 2;
        b = 1.5;
        c = 1;
        ellipsoid = new Ellipsoid(center, axis, xaxis, a, b, c);

        // getNormal test
        final Vector3D point = new Vector3D(5., 0., 0.);
        final Vector3D normal = ellipsoid.getNormal(point);
        Assert.assertEquals(1., normal.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0., normal.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0., normal.getZ(), this.comparisonEpsilon);

        // un point de l'espace (en dehors de l'ellipsoide
        // et son pt le plus proche de l'ellipsoide
        aFarAwayPoint = ellipsoid.getAffineStandardExpression(new Vector3D(5, 0, 0));
        expectedPoint = ellipsoid.getAffineStandardExpression(new Vector3D(a, 0, 0));
        // le point de l'ellipsoide le plus proche de ce point de l'espace
        theClosestPoint = ellipsoid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), ellipsoid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // un point de l'espace (en dehors de l'ellipsoide
        // et son pt le plus proche de l'ellipsoide
        aFarAwayPoint = ellipsoid.getAffineStandardExpression(new Vector3D(0, 5, 0));
        expectedPoint = ellipsoid.getAffineStandardExpression(new Vector3D(0, b, 0));
        // le point de l'ellipsoide le plus proche de ce point de l'espace
        theClosestPoint = ellipsoid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), ellipsoid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        // un point de l'espace (en dehors de l'ellipsoide
        // et son pt le plus proche de l'ellipsoide
        aFarAwayPoint = ellipsoid.getAffineStandardExpression(new Vector3D(0, 0, 5));
        expectedPoint = ellipsoid.getAffineStandardExpression(new Vector3D(0, 0, c));
        // le point de l'ellipsoide le plus proche de ce point de l'espace
        theClosestPoint = ellipsoid.closestPointTo(aFarAwayPoint);
        Assert.assertEquals(expectedPoint.getX(), theClosestPoint.getX(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getY(), theClosestPoint.getY(), this.comparisonEpsilon);
        Assert.assertEquals(expectedPoint.getZ(), theClosestPoint.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(aFarAwayPoint.subtract(expectedPoint).getNorm(), ellipsoid.distanceTo(aFarAwayPoint),
            this.comparisonEpsilon);

        Assert.assertEquals(ellipsoid.getSemiPrincipalX().getX(), 1, this.comparisonEpsilon);
        Assert.assertEquals(ellipsoid.getSemiPrincipalX().getY(), 0, this.comparisonEpsilon);
        Assert.assertEquals(ellipsoid.getSemiPrincipalX().getZ(), 0, this.comparisonEpsilon);

        Assert.assertEquals(ellipsoid.getSemiPrincipalY().getX(), 0, this.comparisonEpsilon);
        Assert.assertEquals(ellipsoid.getSemiPrincipalY().getY(), 1, this.comparisonEpsilon);
        Assert.assertEquals(ellipsoid.getSemiPrincipalY().getZ(), 0, this.comparisonEpsilon);

        Assert.assertEquals(ellipsoid.getSemiPrincipalZ().getX(), 0, this.comparisonEpsilon);
        Assert.assertEquals(ellipsoid.getSemiPrincipalZ().getY(), 0, this.comparisonEpsilon);
        Assert.assertEquals(ellipsoid.getSemiPrincipalZ().getZ(), 1, this.comparisonEpsilon);

        final String expected =
            "Ellipsoid{Center{0.0; 0.0; 0.0},Revolution axis{0.0; 0.0; 1.0},Axis a{1.0; 0.0; 0.0},Semi axis a{2.0},Semi axis b{1.5},Semi axis c{1.0}}";
        Assert.assertEquals(expected, ellipsoid.toString());

        ellipsoid = new Ellipsoid(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.PLUS_I, 2, 1.5, 1);
        theClosestPoint = ellipsoid.closestPointTo(Vector3D.ZERO);
        Assert.assertEquals(theClosestPoint.getX(), 0, this.comparisonEpsilon);
        Assert.assertEquals(theClosestPoint.getY(), 0, this.comparisonEpsilon);
        Assert.assertEquals(theClosestPoint.getZ(), 1, this.comparisonEpsilon);

        ellipsoid = new Ellipsoid(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.PLUS_I, 2, 1, 1.5);
        theClosestPoint = ellipsoid.closestPointTo(Vector3D.ZERO);
        Assert.assertEquals(theClosestPoint.getX(), 0, this.comparisonEpsilon);
        Assert.assertEquals(theClosestPoint.getY(), 1, this.comparisonEpsilon);
        Assert.assertEquals(theClosestPoint.getZ(), 0, this.comparisonEpsilon);

        ellipsoid = new Ellipsoid(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.PLUS_I, 1, 2, 1.5);
        theClosestPoint = ellipsoid.closestPointTo(Vector3D.ZERO);
        Assert.assertEquals(theClosestPoint.getX(), 1, this.comparisonEpsilon);
        Assert.assertEquals(theClosestPoint.getY(), 0, this.comparisonEpsilon);
        Assert.assertEquals(theClosestPoint.getZ(), 0, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ELLIPSOID_DISTANCES}
     *
     * @testedMethod {@link Spheroid#closestPointTo(Vector3D)}
     *
     * @description Make sure the vector (user point - computed closest point) is normal to the surface of the ellipsoid
     *
     * @input none.
     *
     * @output dot product of surface tangents and user point / closest point vector
     *
     * @testPassCriteria The computed dot products must be within machine espilon range
     *
     * @referenceVersion 2.0
     *
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testOrthogonality() {

        // definition d'un ellipsoide de revolution
        final Vector3D center = new Vector3D(0, 0, 0);
        final Vector3D axis = new Vector3D(0, 0, 1);
        final Vector3D xaxis = new Vector3D(1, 0, 0);
        final double a = 2;
        final double b = 1.5;
        final double c = 1;
        final Ellipsoid ellipsoid = new Ellipsoid(center, axis, xaxis, a, b, c);

        final Random ran = new Random();
        Vector3D p;
        Vector3D s;
        Vector3D dir;
        Vector3D v1;
        Vector3D v2;
        double ct;
        double st;
        double cp;
        double sp;
        double[] cc;
        for (int i = 0; i < 100; i++) {

            // random point and its closest point
            p = new Vector3D(ran.nextDouble() * 10, ran.nextDouble() * 10, ran.nextDouble() * 10);
            s = ellipsoid.closestPointTo(p);

            // vector from closest point to user point
            dir = p.subtract(s);

            // ellipsoidic coordinates of closest point
            cc = ellipsoid.getEllipsoidicCoordinates(s);
            ct = MathLib.cos(cc[0]);
            st = MathLib.sin(cc[0]);
            cp = MathLib.cos(cc[1]);
            sp = MathLib.sin(cc[1]);

            // tangents to ellipsoid surface
            v1 = new Vector3D(-a * st * cp, b * ct * cp, 0);
            v2 = new Vector3D(-a * ct * sp, -b * st * sp, c * cp);

            // make sure the dir vector is normal to the surface
            Assert.assertEquals(0, Vector3D.dotProduct(v1, dir), this.comparisonEpsilon);
            Assert.assertEquals(0, Vector3D.dotProduct(v2, dir), this.comparisonEpsilon);

        }

    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ELLIPSOID_DISTANCES}
     *
     * @testedMethod {@link Spheroid#closestPointTo(Line)}
     *
     * @description Make sure the vector (computed user line point - computed ellipsoid point) is normal to the surface
     *              of the ellipsoid and othogonal to the line direction
     *
     * @input none.
     *
     * @output dot product of surface tangents / line and computed points
     *
     * @testPassCriteria The computed dot products must be within machine espilon range. Points calculated are the
     *                   expected ones, whether the line intersects the ellipsoid or not.
     *
     * @referenceVersion 2.0
     *
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testLineDistancesAtSingularities() {

        final Ellipsoid ellipsoid = new Ellipsoid(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.PLUS_I, 2, 1.5, 1.3);

        /*
         * No intersection case
         */
        // On top of case with infinite line
        Vector3D pt1 = new Vector3D(-5, 0, 5);
        Vector3D pt2 = new Vector3D(5, 0, 5);
        Line line = new Line(pt1, pt2);
        Vector3D[] pts = ellipsoid.closestPointTo(line);

        Assert.assertFalse(ellipsoid.intersects(line));
        assertEq(new Vector3D(0, 0, 5), pts[0]);
        assertEq(new Vector3D(0, 0, 1.3), pts[1]);

        // Same test with semi-finite line
        final Vector3D minAbsP = new Vector3D(4, 0, 5);
        line = new Line(pt1, pt2, minAbsP);
        pts = ellipsoid.closestPointTo(line);

        Assert.assertFalse(ellipsoid.intersects(line));
        assertEq(minAbsP, pts[0]);
        assertEq(ellipsoid.closestPointTo(minAbsP), pts[1]);

        // Same test with semi-finite line and with line's closest point with abscissa > min abscissa
        line = new Line(pt1, pt2, pt1);
        pts = ellipsoid.closestPointTo(line);

        Assert.assertFalse(ellipsoid.intersects(line));
        assertEq(new Vector3D(0, 0, 5), pts[0]);
        assertEq(new Vector3D(0, 0, 1.3), pts[1]);

        // Underneath case with infinite line
        pt1 = new Vector3D(-5, 0, -5);
        pt2 = new Vector3D(5, 0, -5);
        line = new Line(pt1, pt2);
        pts = ellipsoid.closestPointTo(line);

        Assert.assertFalse(ellipsoid.intersects(line));
        assertEq(new Vector3D(0, 0, -5), pts[0]);
        assertEq(new Vector3D(0, 0, -1.3), pts[1]);

        // Same test with semi-finite line
        line = new Line(pt1, pt2, minAbsP);
        pts = ellipsoid.closestPointTo(line);

        Assert.assertFalse(ellipsoid.intersects(line));
        Vector3D projectedMinAbsP = line.toSpace(line.toSubSpace(minAbsP));
        assertEq(projectedMinAbsP, pts[0]);
        assertEq(ellipsoid.closestPointTo(projectedMinAbsP), pts[1]);

        /*
         * Intersection case
         */
        // Intersection with infinite line
        pt1 = new Vector3D(-5, 0, 0);
        pt2 = new Vector3D(5, 0, 0);
        line = new Line(pt1, pt2);
        pts = ellipsoid.closestPointTo(line);

        Assert.assertTrue(ellipsoid.intersects(line));
        assertEq(new Vector3D(-2, 0, 0), pts[0]);
        assertEq(new Vector3D(-2, 0, 0), pts[1]);

        // Same test with semi-finite line completely outside the ellispoid: no intersection
        pt1 = new Vector3D(-5, 0, 0);
        pt2 = new Vector3D(5, 0, 0);
        line = new Line(pt1, pt2, minAbsP);
        pts = ellipsoid.closestPointTo(line);

        Assert.assertFalse(ellipsoid.intersects(line));
        projectedMinAbsP = line.toSpace(line.toSubSpace(minAbsP));
        assertEq(projectedMinAbsP, pts[0]);
        assertEq(new Vector3D(2, 0, 0), pts[1]);

        // Same test with semi-finite line whose min abscissa point is within the ellispoid: 1 intersection
        pt1 = new Vector3D(-5, 0, 0);
        pt2 = new Vector3D(5, 0, 0);
        line = new Line(pt1, pt2, new Vector3D(0, 0, 5));
        pts = ellipsoid.closestPointTo(line);

        Assert.assertTrue(ellipsoid.intersects(line));
        assertEq(new Vector3D(2, 0, 0), pts[0]);
        assertEq(new Vector3D(2, 0, 0), pts[1]);

    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ELLIPSOID_DISTANCES}
     *
     * @testedMethod {@link Ellipsoid#setNewtonThreshold(double)}
     *
     * @description Test Ellipsoid setter for Newton algorithm.
     *
     * @input data
     *
     * @output Spheroid
     *
     * @testPassCriteria algorithm does not converge if threshold is too small, algorithm converge with default
     *                   threshold (1E-11).
     *
     * @referenceVersion 3.3
     *
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testNewtonThreshold() {

        final Ellipsoid ellipsoid = new Spheroid(Vector3D.ZERO, Vector3D.PLUS_K, 6378E3, 6378E3 * (1. - 1. / 100.));
        final Vector3D p1 = new Vector3D(7000E3, 1000E3, 2000E3);
        final Vector3D dir = new Vector3D(-0.4336, 0.9011, 0.00000).normalize();
        ellipsoid.distanceTo(new Line(p1, p1.add(dir)));
        ellipsoid.setNewtonThreshold(1E-17);
        try {
            ellipsoid.distanceTo(new Line(p1, p1.add(dir)));
            Assert.fail();
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Test for the closest point on the ellipsoid to a line where the line is defined with a point
     * that is very far away from the ellipsoid.
     * This is a case that can happen in interplanetary settings.
     *
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @Test
    public void testLineDistanceInterplanetary()
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        // Create ellipsoid with the dimensions of Phobos
        final Ellipsoid ellipsoid =
            new Ellipsoid(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.PLUS_I, 26.8e3 / 2, 22.4e3 / 2, 18.4e3 / 2);

        // Create a line
        final Line line = new Line(Vector3D.PLUS_I, Vector3D.PLUS_J);

        // Set the origin and direction directly through introspection to use the exact values
        // encountered in the FDS where the problem was first seen
        // Direction
        java.lang.reflect.Field field = Line.class.getDeclaredField("direction");
        field.setAccessible(true);
        field.set(line, new Vector3D(0.9413931066584893, -0.2680739184724201, -0.2047324912433796));

        // Origin
        field = Line.class.getDeclaredField("zero");
        field.setAccessible(true);
        field.set(line, new Vector3D(4.975192403933805E10, 5.882337081855984E10, 1.517448775808159E11));

        // Set an appropriate threshold given the large values involved in the line definition
        ellipsoid.setNewtonThreshold(1e-9);

        // Compute the closest point to line
        ellipsoid.distanceTo(line);

        // The code is expected to complete successfully without throwing any exception, especially
        // the MaxCountExceededException.
        // If the exception is thrown, the test will fail automatically, no need to add try/catch
        // blocks here.
    }

    /**
     * This test aims to check proper Newton's algorithm convergence for computing the closest point
     * on the ellipsoid's surface when the point of interest is very close to it's center. It was
     * implemented due to OPENFD-399.
     */
    @Test
    public void testClosestPointInsideConvergence() {
        // This point cause issues before OPENFD-399 implementation
        final Vector3D point = new Vector3D(33148.50802297387, -25919.51638901363, 3128.4907145896764);
        // This point did not cause issues before OPENFD-399 implementation
        final Vector3D point2 = point.add(new Vector3D(0, 0, 100));

        try {
            // the method should work with both points after OPENFD-399 implementation
            computeClosestPoint(point);
            computeClosestPoint(point2);
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * This test aims to check proper Newton's algorithm convergence for computing the point on the line that is the
     * closest to the ellipsoid. The primary algorithm failed to converge before OPENFD-576 and after this, a new backup
     * algorithm should resolve this specific case.<br>
     * It was implemented due to OPENFD-576.
     */
    @Test
    public void testClosestPointLineInsideConvergence() {
        final Spheroid earthShape = new Spheroid(Vector3D.ZERO, Vector3D.PLUS_K, 6378137.0, 6356752.314140356);
        earthShape.setNewtonThreshold(1.0E-11);
        final Vector3D direction = new Vector3D(-0.9895496521188716, 0.001513449610784831, 0.14418458815593221);

        // This point caused issues before OPENFD-576 implementation
        final Vector3D point1 = new Vector3D(887619.7170645071 - 0.000001, -1528279.2119383009, 6107842.502980271); //
        // This point did not cause issues before OPENFD-576 implementation
        final Vector3D point2 = new Vector3D(887619.7170645071, -1528279.2119383009, 6107842.502980271);

        final Line line1 = Line.createLine(point1, direction);
        final Line line2 = Line.createLine(point2, direction);

        try {
            // The method should work with both points after OPENFD-576 implementation
            earthShape.distanceTo(line1);
            earthShape.distanceTo(line2);
        } catch (final MaxCountExceededException e) {
            Assert.fail();
        }
    }

    /**
     * This test aims to check for proper Newton's algorithm precision when it comes to compute a
     * point's nearest neighbor on a sphere surface. It was implemented for OPENFD-399.
     * We compute 5000 points' nearest neighbors and we compare the results with before the
     * modifications.
     * Both points and results lists are stored in .txt files in ./ressources.
     *
     * @throws PatriusException
     * @throws IOException
     */
    @Test
    public void testClosestPointInsidePrecision() throws PatriusException, IOException {
        // Loading old results (before OPENFD-399)
        setUpSources();
        Assert.assertEquals(this.points.size(), this.coords.size());
        for (int i = 0; i < Math.min(this.points.size(), this.coords.size()); i++) {
            final LLHCoordinates newCoords = computeClosestPointSphere(this.points.get(i));
            assertEq(this.coords.get(i), new Vector3D(newCoords.getLatitude(),
                newCoords.getLongitude(), newCoords.getHeight()));
        }
    }

    /**
     * Compute one point's the closest neighbor on an ellipsoid's surface.
     *
     * @param point
     *        : Point of interest
     * @throws PatriusException
     */
    public LLHCoordinates computeClosestPoint(final Vector3D point) throws PatriusException {
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final EllipsoidPoint ellipsoidPoint = new EllipsoidPoint(earth, point, null);
        final LLHCoordinates lhhc = ellipsoidPoint.getLLHCoordinates();
        return lhhc;
    }

    /**
     * Compute one point's the closest neighbor on an sphere surface.
     * It should be the intersection of the line center - point and the sphere's surface
     *
     * @param point
     *        : Point of interest
     * @throws PatriusException
     */
    public LLHCoordinates computeClosestPointSphere(final Vector3D point) throws PatriusException {
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            0, FramesFactory.getITRF(), "Earth");
        final EllipsoidPoint ellipsoidPoint = new EllipsoidPoint(earth, point, null);
        final LLHCoordinates lhhc = ellipsoidPoint.getLLHCoordinates();
        return lhhc;
    }

    /**
     * Test equality of vectors
     *
     * @param v1
     *        expected
     * @param v2
     *        actual
     */
    private void assertEq(final Vector3D v1, final Vector3D v2) {

        Assert.assertEquals(v1.getX(), v2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(v1.getY(), v2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(v1.getZ(), v2.getZ(), this.comparisonEpsilon);

    }

    public void setUpSources() throws NumberFormatException, IOException {
        final String pointsFileName = "./points.txt";
        final String coordsFileName = "./coords.txt";
        final InputStream pointsInputStream =
            EllipsoidTest.class.getClassLoader().getResourceAsStream(pointsFileName);
        final BufferedReader pointReader = new BufferedReader(new InputStreamReader(pointsInputStream));

        final InputStream coordsInputStream =
            EllipsoidTest.class.getClassLoader().getResourceAsStream(coordsFileName);
        final BufferedReader coordsReader = new BufferedReader(new InputStreamReader(coordsInputStream));

        String pointsLine;
        this.points = new ArrayList<Vector3D>();
        this.coords = new ArrayList<Vector3D>();

        while ((pointsLine = pointReader.readLine()) != null) {
            final String[] parts = pointsLine.split(" ");
            final double x = Double.parseDouble(parts[0].replace(",", "."));
            final double y = Double.parseDouble(parts[1].replace(",", "."));
            final double z = Double.parseDouble(parts[2].replace(",", "."));
            this.points.add(new Vector3D(x, y, z));
        }
        String coordsLine;
        while ((coordsLine = coordsReader.readLine()) != null) {
            final String[] parts = coordsLine.split(" ");
            final double lat = Double.parseDouble(parts[0].replace(",", "."));
            final double lon = Double.parseDouble(parts[1].replace(",", "."));
            final double height = Double.parseDouble(parts[2].replace(",", "."));
            this.coords.add(new Vector3D(lat, lon, height));
        }
    }
}
