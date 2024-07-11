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
 * @history creation 06/10/11
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Unit tests for {@link SphericalCap}.
 * 
 * @author cardosop
 * 
 * @version $Id: SphericalCapTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class SphericalCapTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Spherical cap shape
         * 
         * @featureDescription Creation of a spherical cap shape, computation of distances and intersections with lines
         *                     and points.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_130, DV-GEOMETRIE_140
         */
        SPHERICAL_CAP_SHAPE
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double zeroEpsilon = 0.0;

    /** Epsilon for the geometry. */
    private final double geometryEpsilon = 1e-10;

    /**
     * X unit vector.
     */
    private static final Vector3D XUV = Vector3D.PLUS_I;
    /**
     * Y unit vector.
     */
    private static final Vector3D YUV = Vector3D.PLUS_J;
    /**
     * Z unit vector.
     */
    private static final Vector3D ZUV = Vector3D.PLUS_K;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERICAL_CAP_SHAPE}
     * 
     * @testedMethod {@link SphericalCap#closestPointTo(Line)}
     * @testedMethod {@link SphericalCap#distanceTo(Line)}
     * @testedMethod {@link SphericalCap#getIntersectionPoints(Line)}
     * @testedMethod {@link SphericalCap#intersects(Line)}
     * 
     * @description Test case 1 : hemisphere
     * 
     * @input Misc
     * 
     * @output Misc
     * 
     * @testPassCriteria Everything as expected. When the result of the method {@link SphericalCap#distanceTo(Line)} is
     *                   tested, if the line intersects the spherical cap the result returned by the method is exactly 0
     *                   so in that case the epsilon is 0 ; the epsilon equals 1e-14 if there is no intersection, this
     *                   last epsilon takes into account the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testHemisphere01() {
        // Useful vectors
        final Vector3D minusZUnitVector = Vector3D.MINUS_K;

        // A hemisphere :
        // - sphere at the origin, radius 1.
        // - plane (x,y), the normal is the z unit vector
        final Sphere originSphere = new Sphere(Vector3D.ZERO, 1.);
        final Plane xyPlane = new Plane(ZUV);
        final SphericalCap hemisphere01 = new SphericalCap(originSphere, xyPlane);

        // tests for the intersects method
        // - intersection with the Line oriented by the z unit vector
        final Line zLine = new Line(Vector3D.ZERO, ZUV);
        boolean doesIntersect = hemisphere01.intersects(zLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the x unit vector
        final Line xLine = new Line(Vector3D.ZERO, XUV);
        doesIntersect = hemisphere01.intersects(xLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the y unit vector
        final Line yLine = new Line(Vector3D.ZERO, YUV);
        doesIntersect = hemisphere01.intersects(yLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the x unit vector
        // intersecting the sphere at the lower point (the sphere, not the
        // spherical cap...)
        final Line xBotLine = new Line(Vector3D.MINUS_K, Vector3D.MINUS_K.add(XUV));
        doesIntersect = hemisphere01.intersects(xBotLine);
        Assert.assertTrue(!doesIntersect);
        // - intersection with a Line parallel to the xy plane and at the z unit vector
        // (one point intersection only)
        final double someXValue = -0.32;
        final double someYValue = 16.3424;
        final Line anXyParallelLine = new Line(ZUV, ZUV.add(new Vector3D(someXValue, someYValue, 0.)));
        doesIntersect = hemisphere01.intersects(anXyParallelLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with a Line parallel to the xy plane and slightly above the z unit vector
        // (does not intersect)
        final double someOtherXValue = 5.67;
        final double someOtherYValue = 9.99;
        final Vector3D moreThanZUnit = new Vector3D(0., 0., 1.0000001);
        final Line anOtherXyParallelLine = new Line(moreThanZUnit, moreThanZUnit.add(new Vector3D(someOtherXValue,
            someOtherYValue, 0.)));
        doesIntersect = hemisphere01.intersects(anOtherXyParallelLine);
        Assert.assertTrue(!doesIntersect);
        // - intersection with a Line oriented by (z unit + y unit) and passing through (- z unit)
        // (one point intersection only)
        final Vector3D yPlusZ = YUV.add(ZUV);
        final Line yPlusZLine = new Line(minusZUnitVector, minusZUnitVector.add(yPlusZ));
        doesIntersect = hemisphere01.intersects(yPlusZLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with a Line oriented by (z unit + more than y unit) and passing through (- z unit)
        // (does not intersect)
        final Vector3D moreThanYUnit = new Vector3D(0., 1.0000001, 0.);
        final Vector3D yMorePlusZ = moreThanYUnit.add(ZUV);
        final Line yMorePlusZLine = new Line(minusZUnitVector, minusZUnitVector.add(yMorePlusZ));
        doesIntersect = hemisphere01.intersects(yMorePlusZLine);
        Assert.assertTrue(!doesIntersect);

        // tests for the distance method
        // - if a line intersects, its distance is EXACTLY zero
        double distance = hemisphere01.distanceTo(zLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        distance = hemisphere01.distanceTo(zLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        distance = hemisphere01.distanceTo(xLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        distance = hemisphere01.distanceTo(yLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        distance = hemisphere01.distanceTo(anXyParallelLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        distance = hemisphere01.distanceTo(yPlusZLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - distance to a line with xunit+yunit direction passing through (0.,0.,2.)
        // (should be 1.)
        final Vector3D twoZUnit = ZUV.add(ZUV);
        final Vector3D xPlusYUnit = XUV.add(YUV);
        final Line thruTwo = new Line(twoZUnit, twoZUnit.add(xPlusYUnit));
        distance = hemisphere01.distanceTo(thruTwo);
        Assert.assertEquals(1., distance, this.comparisonEpsilon);
        // - distance to a line with xunit+yunit direction passing through (0.,0.,3.)
        // (should be 2.)
        final Vector3D threeZUnit = twoZUnit.add(ZUV);
        final Line thruThree = new Line(threeZUnit, threeZUnit.add(xPlusYUnit));
        distance = hemisphere01.distanceTo(thruThree);
        Assert.assertEquals(2., distance, this.comparisonEpsilon);
        // - distance to a line with xunit-zunit direction passing through (0.,0.,2)
        // distance should be sqrt(2) - 1
        final Vector3D xMinusZUnit = new Vector3D(1., 0., -1);
        final Line xMinZLine = new Line(twoZUnit, twoZUnit.add(xMinusZUnit));
        distance = hemisphere01.distanceTo(xMinZLine);
        Assert.assertEquals(MathLib.sqrt(2.) - 1., distance, this.comparisonEpsilon);
        // - distance to a line with 4xunit+zunit direction passing through -zunit
        // distance should be sqrt (9/17)
        final Vector3D fourEtc = new Vector3D(4., 0., 1);
        final Line fourEtcLine = new Line(minusZUnitVector, minusZUnitVector.add(fourEtc));
        distance = hemisphere01.distanceTo(fourEtcLine);
        Assert.assertEquals(MathLib.sqrt(9. / 17.), distance, this.comparisonEpsilon);
        // - Line oriented by the y unit + 0.5 z unit vector
        // and passing thru 0,0,-1
        // distance : 1 / sqrt(5)
        final Line y12zLine = new Line(ZUV.negate(), ZUV.negate().add(YUV.add(0.5, ZUV)));
        distance = hemisphere01.distanceTo(y12zLine);
        Assert.assertEquals(1. / MathLib.sqrt(5), distance, this.comparisonEpsilon);

        // tests for the intersections method
        // - intersection with the Line oriented by the z unit vector
        // the intersection points are the center of the bottom circle - the origin
        // and the top of the sphere - (0,0,1)
        final Vector3D[] zInterVec = hemisphere01.getIntersectionPoints(zLine);
        Assert.assertEquals(2, zInterVec.length);
        // Which one is the bottom one? Don't know yet...
        Assert.assertTrue(this.closeVectors(zInterVec[0], Vector3D.ZERO)
            || this.closeVectors(zInterVec[1], Vector3D.ZERO));
        Assert.assertTrue(this.closeVectors(zInterVec[0], ZUV) || this.closeVectors(zInterVec[1], ZUV));
        // - intersection with the Line oriented by the x unit vector
        // the intersection points are (1,0,0) and (-1,0,0)
        final Vector3D[] xInterVec = hemisphere01.getIntersectionPoints(xLine);
        Assert.assertEquals(2, xInterVec.length);
        Assert.assertTrue(this.closeVectors(xInterVec[0], XUV) || this.closeVectors(xInterVec[1], XUV));
        Assert.assertTrue(this.closeVectors(xInterVec[0], XUV.negate())
            || this.closeVectors(xInterVec[1], XUV.negate()));
        // - intersection with the Line oriented by the y unit vector
        // the intersection points are (0,1,0) and (0,-1,0)
        final Vector3D[] yInterVec = hemisphere01.getIntersectionPoints(yLine);
        Assert.assertEquals(2, yInterVec.length);
        Assert.assertTrue(this.closeVectors(yInterVec[0], YUV) || this.closeVectors(yInterVec[1], YUV));
        Assert.assertTrue(this.closeVectors(yInterVec[0], YUV.negate())
            || this.closeVectors(yInterVec[1], YUV.negate()));
        // - intersection with a Line parallel to the xy plane and at the z unit vector
        // one point intersection only at the z unit vector
        final Vector3D[] anxyInterVec = hemisphere01.getIntersectionPoints(anXyParallelLine);
        Assert.assertEquals(1, anxyInterVec.length);
        Assert.assertTrue(this.closeVectors(anxyInterVec[0], ZUV));
        // - intersection with a Line parallel to the xy plane and slightly above the z unit vector
        // (does not intersect)
        final Vector3D[] anOtherXyInterVec = hemisphere01.getIntersectionPoints(anOtherXyParallelLine);
        Assert.assertEquals(0, anOtherXyInterVec.length);
        // - intersection with a Line oriented by (z unit + y unit) and passing through (- z unit)
        // (one point intersection only : (0,1,0)
        final Vector3D[] ypzInterVec = hemisphere01.getIntersectionPoints(yPlusZLine);
        Assert.assertEquals(1, ypzInterVec.length);
        Assert.assertTrue(this.closeVectors(ypzInterVec[0], YUV));
        // - intersection with a Line oriented by (z unit + more than y unit) and passing through (- z unit)
        // (does not intersect)
        final Vector3D[] ympInterVec = hemisphere01.getIntersectionPoints(yMorePlusZLine);
        Assert.assertEquals(0, ympInterVec.length);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERICAL_CAP_SHAPE}
     * 
     * @testedMethod {@link SphericalCap#closestPointTo(Line)}
     * @testedMethod {@link SphericalCap#distanceTo(Line)}
     * @testedMethod {@link SphericalCap#getIntersectionPoints(Line)}
     * @testedMethod {@link SphericalCap#intersects(Line)}
     * 
     * @description Test case 1 : small spherical cap<br>
     *              Test method for a small spherical cap. The sphere is centered at the origin, radius 1.5. The plane
     *              is parallel to zy, goes through xUnit.
     * 
     * @input Misc
     * 
     * @output Misc
     * 
     * @testPassCriteria everything as expected. When the result of the method {@link SphericalCap#distanceTo(Line)} is
     *                   tested, if the line intersects the spherical cap the result returned by the method is exactly 0
     *                   so in that case the epsilon is 0 ; the epsilon equals 1e-14 if there is no intersection, this
     *                   last epsilon takes into account the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testSmallCap01() {

        // A small cap :
        // - the sphere is centered at the origin, radius 1.5.
        // - the plane is parallel to zy, goes thru xUnit.
        final Sphere originSphere = new Sphere(Vector3D.ZERO, 1.5);
        final Plane zyPlane = new Plane(XUV, XUV);
        final SphericalCap smallCap01 = new SphericalCap(originSphere, zyPlane);

        // tests for the intersects method
        // - intersection with the Line oriented by the z unit vector
        // does not intersect
        final Line zLine = new Line(Vector3D.ZERO, ZUV);
        boolean doesIntersect = smallCap01.intersects(zLine);
        Assert.assertTrue(!doesIntersect);
        // - intersection with the Line oriented by the x unit vector
        final Line xLine = new Line(Vector3D.ZERO, XUV);
        doesIntersect = smallCap01.intersects(xLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the y unit vector
        // does not intersect
        final Line yLine = new Line(Vector3D.ZERO, YUV);
        doesIntersect = smallCap01.intersects(yLine);
        Assert.assertTrue(!doesIntersect);
        // - intersection with the Line oriented by the y unit vector passing thru xUnit
        // does intersect
        final Line yDirXThruLine = new Line(XUV, XUV.add(YUV));
        doesIntersect = smallCap01.intersects(yDirXThruLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the y unit vector passing thru xUnit * 1.5
        // does intersect by a single point
        final Line yDir15XThruLine = new Line(XUV.scalarMultiply(1.5), XUV.scalarMultiply(1.5).add(YUV));
        doesIntersect = smallCap01.intersects(yDir15XThruLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the y unit vector passing thru ( xUnit * 1.25
        // + zUnit * sqrt( 1.5^2 - (1.25)^2 ) )
        // does intersect by a single point!
        final double lensHeight = MathLib.sqrt(1.5 * 1.5 - 1.25 * 1.25);
        final Vector3D passEtcVec = new Vector3D(1.25, 0., lensHeight);
        final Line yDirThruEtcLine = new Line(passEtcVec, passEtcVec.add(YUV));
        doesIntersect = smallCap01.intersects(yDirThruEtcLine);
        Assert.assertTrue(doesIntersect);
        // - the same, but slightly farther on x axis
        // does not intersect
        final Vector3D passEtcVec2 = new Vector3D(1.25 + 0.000001, 0., lensHeight);
        final Line yDirThruEtcLine2 = new Line(passEtcVec2, passEtcVec2.add(YUV));
        doesIntersect = smallCap01.intersects(yDirThruEtcLine2);
        Assert.assertTrue(!doesIntersect);
        // - the same, but slightly farther on z axis
        // does not intersect
        final Vector3D passEtcVec3 = new Vector3D(1.25, 0., lensHeight + 0.000001);
        final Line yDirThruEtcLine3 = new Line(passEtcVec3, passEtcVec3.add(YUV));
        doesIntersect = smallCap01.intersects(yDirThruEtcLine3);
        Assert.assertTrue(!doesIntersect);
        // - intersection with the Line oriented by the y unit vector passing thru origin
        // does not intersect
        final Line yOrigLine = new Line(Vector3D.ZERO, YUV);
        doesIntersect = smallCap01.intersects(yOrigLine);
        Assert.assertTrue(!doesIntersect);

        // tests for the distance method
        // - intersection with the Line oriented by the x unit vector
        double distance = smallCap01.distanceTo(xLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - intersection with the Line oriented by the y unit vector passing thru xUnit
        distance = smallCap01.distanceTo(yDirXThruLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - intersection with the Line oriented by the y unit vector passing thru xUnit * 1.5
        // does intersect by a single point
        distance = smallCap01.distanceTo(yDir15XThruLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - intersection with the Line oriented by the y unit vector passing thru ( xUnit * 1.25
        // + zUnit * sqrt( 1.5^2 - (1.25)^2 ) )
        // does intersect by a single point!
        distance = smallCap01.distanceTo(yDirThruEtcLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - Line oriented by the z unit vector
        // distance is 1 (determined by the plane)
        distance = smallCap01.distanceTo(zLine);
        Assert.assertEquals(1., distance, this.comparisonEpsilon);
        // - intersection with the Line oriented by the y unit vector
        // distance is 1 (determined by the plane)
        distance = smallCap01.distanceTo(yLine);
        Assert.assertEquals(1., distance, this.comparisonEpsilon);
        // - Line oriented by the y unit vector passing thru ( xUnit * 1.25
        // + zUnit * sqrt( 1.5^2 - (1.25)^2 ) ), but slightly farther on x axis
        // does not intersect
        // distance : see line below, it's a tad complicated.
        final double eeDist = MathLib.sqrt(1.250001 * 1.250001 + MathLib.pow(MathLib.sqrt(1.5 * 1.5 - 1.25 * 1.25),
            2)) - 1.5;
        distance = smallCap01.distanceTo(yDirThruEtcLine2);
        Assert.assertEquals(eeDist, distance, this.comparisonEpsilon);
        // - the same, but slightly farther on z axis this time
        // does not intersect
        // distance : see line below, it's a tad complicated.
        final double eeeDist = MathLib.sqrt(1.25 * 1.25 + MathLib.pow(
            MathLib.sqrt(1.5 * 1.5 - 1.25 * 1.25) + 0.000001, 2)) - 1.5;
        distance = smallCap01.distanceTo(yDirThruEtcLine3);
        Assert.assertEquals(eeeDist, distance, this.comparisonEpsilon);
        // - Line oriented by the y unit vector passing thru origin
        // does not intersect
        distance = smallCap01.distanceTo(yOrigLine);
        Assert.assertEquals(1., distance, this.comparisonEpsilon);
        // - Line oriented by z unit vector + 0.5 x unit vector, passing thru origin
        // distance : 1.5 * sin ( acos(1/sqrt(5)) - acos(2/3) )
        // which seems to be 0.39442719099991613
        final Line z05xline = new Line(Vector3D.ZERO, ZUV.add(0.5, XUV));
        distance = smallCap01.distanceTo(z05xline);
        Assert.assertEquals(0.39442719099991613, distance, this.comparisonEpsilon);

        // tests for the intersections method
        // - intersection with the Line oriented by the x unit vector
        // 2 points : center of base circle (xUnit) and farther sphere point on x axis (1.5xUnit)
        Vector3D[] interVecs = smallCap01.getIntersectionPoints(xLine);
        Assert.assertEquals(2, interVecs.length);
        boolean b1;
        boolean b2;
        boolean b3;
        boolean b4;
        b1 = this.closeVectors(interVecs[0], XUV);
        b2 = this.closeVectors(interVecs[0], XUV.scalarMultiply(1.5));
        b3 = this.closeVectors(interVecs[1], XUV);
        b4 = this.closeVectors(interVecs[1], XUV.scalarMultiply(1.5));
        // note : ^ is XOR
        Assert.assertTrue(((b1 && b4) ^ (b2 && b3)));
        // - intersection with the Line oriented by the y unit vector passing thru xUnit
        // 2 points : (1 , +-sqrt(5)/2 , 0)
        interVecs = smallCap01.getIntersectionPoints(yDirXThruLine);
        Assert.assertEquals(2, interVecs.length);
        final Vector3D vecsmin = new Vector3D(1., -0.5 * MathLib.sqrt(5), 0.);
        final Vector3D vecsplu = new Vector3D(1., 0.5 * MathLib.sqrt(5), 0.);
        b1 = this.closeVectors(interVecs[0], vecsmin);
        b2 = this.closeVectors(interVecs[0], vecsplu);
        b3 = this.closeVectors(interVecs[1], vecsmin);
        b4 = this.closeVectors(interVecs[1], vecsplu);
        Assert.assertTrue(((b1 && b4) ^ (b2 && b3)));
        // - intersection with the Line oriented by the y unit vector passing thru xUnit * 1.5
        // does intersect by a single point (xUnit * 1.5)
        interVecs = smallCap01.getIntersectionPoints(yDir15XThruLine);
        Assert.assertEquals(1, interVecs.length);
        Assert.assertTrue(this.closeVectors(interVecs[0], XUV.scalarMultiply(1.5)));
        // - intersection with the Line oriented by the y unit vector passing thru ( xUnit * 1.25
        // + zUnit * sqrt( 1.5 - (1.25)^2 ) )
        // does intersect by a single point! : (1.25, 0 , sqrt(1.5 - 1.25^2) )
        interVecs = smallCap01.getIntersectionPoints(yDirThruEtcLine);
        Assert.assertEquals(1, interVecs.length);
        Assert.assertTrue(this.closeVectors(interVecs[0], passEtcVec));
        // - Line oriented by the z unit vector
        // no intersection
        interVecs = smallCap01.getIntersectionPoints(zLine);
        Assert.assertEquals(0, interVecs.length);
        // - intersection with the Line oriented by the y unit vector
        // distance is 1 (determined by the plane)
        interVecs = smallCap01.getIntersectionPoints(yLine);
        Assert.assertEquals(0, interVecs.length);
        // - Line oriented by the y unit vector passing thru ( xUnit * 1.25
        // + zUnit * sqrt( 1.5 - (1.25)^2 ) ), but slightly farther on x axis
        // does not intersect
        interVecs = smallCap01.getIntersectionPoints(yDirThruEtcLine2);
        Assert.assertEquals(0, interVecs.length);
        // - the same, but slightly farther on z axis this time
        // does not intersect
        interVecs = smallCap01.getIntersectionPoints(yDirThruEtcLine3);
        Assert.assertEquals(0, interVecs.length);
        // - Line oriented by the y unit vector passing thru origin
        // does not intersect
        interVecs = smallCap01.getIntersectionPoints(yOrigLine);
        Assert.assertEquals(0, interVecs.length);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERICAL_CAP_SHAPE}
     * 
     * @testedMethod {@link SphericalCap#closestPointTo(Line)}
     * @testedMethod {@link SphericalCap#distanceTo(Line)}
     * @testedMethod {@link SphericalCap#getIntersectionPoints(Line)}
     * @testedMethod {@link SphericalCap#intersects(Line)}
     * 
     * @description Test case 1 : huge spherical cap<br>
     *              Test method for a huge spherical cap. The sphere is centered at (-2,-1,-1), radius 2. The plane
     *              contains (0,-1,-1) , normal (-zunit - x unit).
     * 
     * @input Misc
     * 
     * @output Misc
     * 
     * @testPassCriteria everything as expected. When the result of the method {@link SphericalCap#distanceTo(Line)} is
     *                   tested, if the line intersects the spherical cap the result returned by the method is exactly 0
     *                   so in that case the epsilon is 0 ; the epsilon equals 1e-14 if there is no intersection, this
     *                   last epsilon takes into account the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testHugeCap01() {

        // A huge cap :
        // -The sphere is centered at (-2,-1,-1), radius 2.
        // -The plane contains (0,-1,-1) , normal (-zunit - x unit)
        final Vector3D hugeCenter = new Vector3D(-2, -1, -1);
        final Sphere originSphere = new Sphere(hugeCenter, 2.);
        final Vector3D planePoint = new Vector3D(0., -1., -1.);
        final Vector3D mzmxUnit = ZUV.negate().add(XUV.negate());
        final Plane zyPlane = new Plane(planePoint, mzmxUnit);
        final SphericalCap hugeCap01 = new SphericalCap(originSphere, zyPlane);

        // tests for the intersects method
        // - intersection with the Line oriented by the z unit vector
        final Line zLine = new Line(Vector3D.ZERO, ZUV);
        boolean doesIntersect = hugeCap01.intersects(zLine);
        Assert.assertTrue(!doesIntersect);
        // - intersection with the Line oriented by the x unit vector
        final Line xLine = new Line(Vector3D.ZERO, XUV);
        doesIntersect = hugeCap01.intersects(xLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the y unit vector
        // does not intersect
        final Line yLine = new Line(Vector3D.ZERO, YUV);
        doesIntersect = hugeCap01.intersects(yLine);
        Assert.assertTrue(!doesIntersect);
        // - intersection with the Line oriented by the (x unit - z unit) vector
        // going through (0,-1,-1)
        final Vector3D zommVec = new Vector3D(0., -1., -1.);
        final Line xmzLine = new Line(zommVec, zommVec.add(XUV.add(ZUV.negate())));
        doesIntersect = hugeCap01.intersects(xmzLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the (x unit + z unit) vector
        // going through (-1,-1,0)
        final Vector3D mmoVec = new Vector3D(-1., -1., 0.);
        final Line xzmmoLine = new Line(mmoVec, mmoVec.add(XUV.add(ZUV)));
        doesIntersect = hugeCap01.intersects(xzmmoLine);
        Assert.assertTrue(doesIntersect);
        // - intersection with the Line oriented by the (x unit - z unit) vector
        // going through (0,-1,0)
        // does not intersect
        final Vector3D omoVec = new Vector3D(0., -1., 0.);
        final Line xmzomoLine = new Line(omoVec, omoVec.add(XUV.add(ZUV.negate())));
        doesIntersect = hugeCap01.intersects(xmzomoLine);
        Assert.assertTrue(!doesIntersect);

        // tests for the distanceTo method
        // - intersection with the Line oriented by the z unit vector
        // distance : sqrt(5) -2
        double distance = hugeCap01.distanceTo(zLine);
        Assert.assertEquals(MathLib.sqrt(5) - 2., distance, this.zeroEpsilon);
        // - intersection with the Line oriented by the x unit vector
        distance = hugeCap01.distanceTo(xLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - intersection with the Line oriented by the y unit vector
        // does not intersect
        // distance : sqrt(2)/2
        distance = hugeCap01.distanceTo(yLine);
        Assert.assertEquals(MathLib.sqrt(2.) / 2., distance, this.comparisonEpsilon);
        // - intersection with the Line oriented by the (x unit - z unit) vector
        // going through (0,-1,-1)
        distance = hugeCap01.distanceTo(xmzLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - intersection with the Line oriented by the (x unit + z unit) vector
        // going through (-1,-1,0)
        distance = hugeCap01.distanceTo(xzmmoLine);
        Assert.assertEquals(0., distance, this.zeroEpsilon);
        // - intersection with the Line oriented by the (x unit - z unit) vector
        // going through (0,-1,0)
        // does not intersect; distance = sqrt(2)/2
        distance = hugeCap01.distanceTo(xmzomoLine);
        Assert.assertEquals(MathLib.sqrt(2.) / 2., distance, this.comparisonEpsilon);
        // - intersection with the Line oriented by the (x unit - 2 * z unit) vector
        // going through (0,-1,0)
        // does not intersect; distance = 1 / sqrt(5)
        final Line xm2zomoLine = new Line(omoVec, omoVec.add(XUV.add(2., ZUV.negate())));
        distance = hugeCap01.distanceTo(xm2zomoLine);
        Assert.assertEquals(1. / MathLib.sqrt(5.), distance, this.comparisonEpsilon);

        // tests for the intersections method
        boolean b1;
        boolean b2;
        boolean b3;
        boolean b4;
        // - intersection with the Line oriented by the x unit vector
        Vector3D[] interVecs = hugeCap01.getIntersectionPoints(xLine);
        Assert.assertEquals(2, interVecs.length);
        b1 = this.closeVectors(interVecs[0], XUV.scalarMultiply(-1.));
        b2 = this.closeVectors(interVecs[0], XUV.scalarMultiply(-(2. + MathLib.sqrt(2.))));
        b3 = this.closeVectors(interVecs[1], XUV.scalarMultiply(-1.));
        b4 = this.closeVectors(interVecs[1], XUV.scalarMultiply(-(2. + MathLib.sqrt(2.))));
        Assert.assertTrue(((b1 && b4) ^ (b2 && b3)));
        // - intersection with the Line oriented by the (x unit - z unit) vector
        // going through (0,-1,-1)
        interVecs = hugeCap01.getIntersectionPoints(xmzLine);
        Assert.assertEquals(2, interVecs.length);
        final Vector3D otv = new Vector3D(-2., -1., 1.);
        b1 = this.closeVectors(interVecs[0], zommVec);
        b2 = this.closeVectors(interVecs[0], otv);
        b3 = this.closeVectors(interVecs[1], zommVec);
        b4 = this.closeVectors(interVecs[1], otv);
        Assert.assertTrue(((b1 && b4) ^ (b2 && b3)));
        // - intersection with the Line oriented by the (x unit + z unit) vector
        // going through (-1,-1,0)
        interVecs = hugeCap01.getIntersectionPoints(xzmmoLine);
        Assert.assertEquals(2, interVecs.length);
        final Vector3D otv2 = new Vector3D(-(2. + MathLib.sqrt(2)), -1., -(1. + MathLib.sqrt(2)));
        b1 = this.closeVectors(interVecs[0], mmoVec);
        b2 = this.closeVectors(interVecs[0], otv2);
        b3 = this.closeVectors(interVecs[1], mmoVec);
        b4 = this.closeVectors(interVecs[1], otv2);
        Assert.assertTrue(((b1 && b4) ^ (b2 && b3)));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERICAL_CAP_SHAPE}
     * 
     * @testedMethod {@link SphericalCap#SphericalCap(Sphere, Plane)}
     * 
     * @description Error case : invalid spherical cap.
     * 
     * @input misc
     * 
     * @output IllegalArgumentException
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testError1() {
        // The plane and the sphere are too far apart
        final Sphere someSp = new Sphere(Vector3D.PLUS_J, 3.);
        final Plane someP = new Plane(Vector3D.PLUS_J.scalarMultiply(10.), Vector3D.PLUS_J);

        new SphericalCap(someSp, someP);
    }

    /**
     * Returns true when vectors are "quite close"
     * 
     * @param v1
     *        first vector
     * @param v2
     *        second vector
     * @return true when vectors are "quite close" i.e. under the geometry epsilon
     */
    private boolean closeVectors(final Vector3D v1, final Vector3D v2) {
        return (v1.distance(v2) < this.geometryEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERICAL_CAP_SHAPE}
     * 
     * @testedMethod {@link SphericalCap#closestPointTo(Line)}
     * 
     * @description Compute the point of the spherical cap the shortest distance to a line of space, and the associated
     *              point of the line.
     * 
     * @input Points of space (Vector3D)
     * 
     * @output Vector3D[]
     * 
     * @testPassCriteria The output vector must be the one of the shape and the one of the line realizing the shortest
     *                   distance with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testClosestPointToLine() {

        // The cap :
        final Vector3D hugeCenter = new Vector3D(0.0, 0.0, 0.0);
        final Sphere originSphere = new Sphere(hugeCenter, 2.0);
        final Vector3D planePoint = new Vector3D(0.0, 0.0, 1.0);
        final Vector3D normal = new Vector3D(0.0, 0.0, -1.0);
        final Plane plane = new Plane(planePoint, normal);
        final SphericalCap cap = new SphericalCap(originSphere, plane);

        // test with an intersecting line
        Vector3D lineOrig = new Vector3D(0.0, 0.0, 0.0);
        Vector3D lineDir = new Vector3D(0.0, 0.0, 1.0);
        Line line = new Line(lineOrig, lineOrig.add(lineDir));

        Vector3D[] closestPoints = cap.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(0.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-2.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(-2.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the sphere
        lineOrig = new Vector3D(3.0, 0.0, 0.0);
        lineDir = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));

        closestPoints = cap.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(3.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the disk, out of the sphere
        lineOrig = new Vector3D(0.0, 0.0, 4.0);
        lineDir = new Vector3D(-1.0, 0.0, 1.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));

        closestPoints = cap.closestPointTo(line);
        point2 = closestPoints[1];

        Assert.assertEquals(2.0 * MathLib.cos(FastMath.PI / 6.0), point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getY(), 1.0e-8);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line closest to the disk, inside the complete sphere
        lineOrig = new Vector3D(0.0, 0.0, 1.5);
        lineDir = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(lineOrig, lineOrig.add(lineDir));

        closestPoints = cap.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(-2.0 * MathLib.cos(FastMath.PI / 6.0), point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.5, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(-2.0 * MathLib.cos(FastMath.PI / 6.0), point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(0.0, point2.getY(), 1.0e-8);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SPHERICAL_CAP_SHAPE}
     * 
     * @testedMethod {@link SphericalCap#toString()}
     * 
     * @description Creates a string describing the shape, the order of the informations
     *              in this output being the same as the one of the constructor
     * 
     * @input none.
     * 
     * @output String
     * 
     * @testPassCriteria The output string must contain the right information.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testToString() {

        // The cap :
        final Vector3D hugeCenter = new Vector3D(0.0, 0.0, 0.0);
        final Sphere originSphere = new Sphere(hugeCenter, 2.0);
        final Vector3D planePoint = new Vector3D(0.0, 0.0, 1.0);
        final Vector3D normal = new Vector3D(0.0, 0.0, -1.0);
        final Plane plane = new Plane(planePoint, normal);
        final SphericalCap cap = new SphericalCap(originSphere, plane);

        // string creation
        final String result = cap.toString();

        final String expected =
            "SphericalCap{Sphere center{0; 0; 0},Sphere radius{2.0},Plane origin{-0; -0; 1},Plane normal{0; 0; -1}}";
        Assert.assertEquals(expected, result);
    }

}
