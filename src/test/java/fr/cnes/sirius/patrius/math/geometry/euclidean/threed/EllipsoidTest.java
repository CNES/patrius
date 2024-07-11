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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:650:22/07/2016: ellipsoid corrections
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

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
    public final void testConstructor() {

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
    public final void testClosestPointToPart1() {

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
            "Ellipsoid{Center{0; 0; 0},Revolution axis{0; 0; 1},Axis a{1; 0; 0},Semi axis a{2.0},Semi axis b{1.5},Semi axis c{1.0}}";
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
     * @testPassCriteria The computed dot products must be within machine espilon range
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testLineDistancesAtSingularities() {

        final Ellipsoid ellipsoid = new Ellipsoid(Vector3D.ZERO, Vector3D.PLUS_K, Vector3D.PLUS_I, 2, 1.5, 1.3);

        // on top of
        Vector3D pt1 = new Vector3D(-5, 0, 5);
        Vector3D pt2 = new Vector3D(5, 0, 5);
        Line line1 = new Line(pt1, pt2);
        Vector3D[] pts1 = ellipsoid.closestPointTo(line1);

        this.assertEq(new Vector3D(0, 0, 1.3), pts1[0]);
        this.assertEq(new Vector3D(0, 0, 5), pts1[1]);

        // underneath
        pt1 = new Vector3D(-5, 0, -5);
        pt2 = new Vector3D(5, 0, -5);
        line1 = new Line(pt1, pt2);
        pts1 = ellipsoid.closestPointTo(line1);

        this.assertEq(new Vector3D(0, 0, -1.3), pts1[0]);
        this.assertEq(new Vector3D(0, 0, -5), pts1[1]);

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
    public final void testNewtonThreshold() {

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

}
