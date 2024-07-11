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
 * @history creation 17/10/11
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Unit tests for the Disk class.
 * 
 * @author cardosop
 * 
 * @version $Id: DiskTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class DiskTest {

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
        DISK_SHAPE
    }

    /**
     * sqrt(2).
     */
    private static final double SQRT_2 = MathLib.sqrt(2);
    /**
     * The tested disk.
     */
    private Disk diskOne;
    /**
     * The tested lines.
     */
    private final Line[] linesOne = new Line[5];

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double zeroEpsilon = 0.0;

    /**
     * Setup.
     */
    @Before
    public final void setUp() {
        // First disk : center (1.5,0,0) radius 2, normal (1,0,0)
        this.diskOne = new Disk(Vector3D.PLUS_I.scalarMultiply(1.5), Vector3D.PLUS_I, 2.);
        // Lines
        // This line is far from the disk
        Vector3D tmpVec = new Vector3D(0., 0., 3.);
        this.linesOne[0] = new Line(tmpVec, tmpVec.add(Vector3D.PLUS_J.add(Vector3D.MINUS_K)));
        tmpVec = new Vector3D(1.5, 2., 0.);
        // This is a tangent to the disk, but inside the disk's plane
        this.linesOne[1] = new Line(tmpVec, tmpVec.add(Vector3D.PLUS_K));
        // This line goes through the disk, but inside the disk's plane
        tmpVec = new Vector3D(1.5, 0., 0.);
        this.linesOne[2] = new Line(tmpVec, tmpVec.add(Vector3D.PLUS_K.add(Vector3D.PLUS_J)));
        // This line goes through the disk, is not inside the disk's plane
        tmpVec = new Vector3D(0., 1., 1.);
        this.linesOne[3] = new Line(tmpVec, tmpVec.add(Vector3D.PLUS_I));
        // This line goes through the border of the disk, is not inside the disk's plane
        tmpVec = new Vector3D(0., 2., 0.);
        this.linesOne[4] = new Line(tmpVec, tmpVec.add(Vector3D.PLUS_I));

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DISK_SHAPE}
     * 
     * @testedMethod {@link Disk#intersects(Line)}
     * 
     * @description Test the function intersects(Line) {@link Disk#intersects(Line)} with several lines.
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria Everything as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testIntersects() {
        // Tests for diskOne
        Assert.assertTrue(!this.diskOne.intersects(this.linesOne[0]));
        Assert.assertTrue(!this.diskOne.intersects(this.linesOne[1]));
        Assert.assertTrue(!this.diskOne.intersects(this.linesOne[2]));
        Assert.assertTrue(this.diskOne.intersects(this.linesOne[3]));
        Assert.assertTrue(this.diskOne.intersects(this.linesOne[4]));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DISK_SHAPE}
     * 
     * @testedMethod {@link Disk#getIntersectionPoints(Line)}
     * 
     * @description Test the function getIntersectionPoints(Line) {@link Disk#getIntersectionPoints(Line)} with several
     *              lines.
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria Everything as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGetIntersectionPoints() {
        final Vector3D[] ipZer = this.diskOne.getIntersectionPoints(this.linesOne[0]);
        Assert.assertEquals(0, ipZer.length);
        final Vector3D[] ipOne = this.diskOne.getIntersectionPoints(this.linesOne[1]);
        Assert.assertEquals(0, ipOne.length);
        final Vector3D[] ipTwo = this.diskOne.getIntersectionPoints(this.linesOne[2]);
        Assert.assertEquals(0, ipTwo.length);
        final Vector3D[] ipThr = this.diskOne.getIntersectionPoints(this.linesOne[3]);
        Assert.assertEquals(1, ipThr.length);
        final Vector3D cdtOneA = new Vector3D(1.5, 1., 1.);
        Assert.assertTrue(ipThr[0].equals(cdtOneA));
        final Vector3D[] ipFou = this.diskOne.getIntersectionPoints(this.linesOne[4]);
        Assert.assertEquals(1, ipFou.length);
        final Vector3D cdtOneB = new Vector3D(1.5, 2., 0.);
        Assert.assertTrue(ipFou[0].equals(cdtOneB));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DISK_SHAPE}
     * 
     * @testedMethod {@link Disk#distanceTo(Line)}
     * 
     * @description Test the function distanceTo(Line) {@link Disk#distanceTo(Line)} with several lines.
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria Everything as expected. If the line intersects the disk the distance should be exactly 0, if it
     *                   does not intersect the disk, the allowed error on the distance computation is 1e-14 due to the
     *                   computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testDistanceTo() {
        // Distance to linesOne[0] is sqrt( 1.5^2 + (3/sqrt(2) -2)^2 )
        final double p1 = (3. / SQRT_2) - 2;
        final double expDst = MathLib.sqrt(1.5 * 1.5 + p1 * p1);
        Assert.assertEquals(expDst, this.diskOne.distanceTo(this.linesOne[0]), this.comparisonEpsilon);
        Assert.assertEquals(0., this.diskOne.distanceTo(this.linesOne[1]), this.comparisonEpsilon);
        Assert.assertEquals(0., this.diskOne.distanceTo(this.linesOne[2]), this.comparisonEpsilon);
        Assert.assertEquals(0., this.diskOne.distanceTo(this.linesOne[3]), this.zeroEpsilon);
        Assert.assertEquals(0., this.diskOne.distanceTo(this.linesOne[4]), this.zeroEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DISK_SHAPE}
     * 
     * @testedMethod {@link Disk#distanceTo(Line)}
     * 
     * @description Special case : distance to a giant disk.<br>
     *              The idea is to have the distance finder algorithm converges slowly.<br>
     *              This unit test also illustrates the relatively poor precision when the circle goes larger , due to
     *              numeric errors in small rotations, and also the precision loss when comparing large and small
     *              doubles.
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria Everything as expected with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testGiantDistanceTo() {
        // line
        final Line zLine = new Line(Vector3D.ZERO, Vector3D.PLUS_K);
        final double hugeDouble = 10000000000.;
        final double smallDouble = 1;
        final Vector3D farCenter = new Vector3D(hugeDouble + smallDouble, 0., 0.);
        final Disk giantDisk = new Disk(farCenter, Vector3D.PLUS_J, hugeDouble);
        // The distance to the line shall therefore be smallDouble...
        final double dist = giantDisk.distanceTo(zLine);
        Assert.assertEquals(smallDouble, dist, this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DISK_SHAPE}
     * 
     * @testedMethod {@link Disk#Disk(Vector3D, Vector3D, double)}
     * 
     * @description Error case : illegal disk.
     * 
     * @input Disk constructor with illegal parameters
     * 
     * @output IllegalArgumentException {@link IllegalArgumentException}
     * 
     * @testPassCriteria IllegalArgumentException {@link IllegalArgumentException} as expected
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testIllegalDisk() {
        new Disk(Vector3D.PLUS_I, Vector3D.MINUS_K, -1.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DISK_SHAPE}
     * 
     * @testedMethod {@link Disk#closestPointTo(Line)}
     * 
     * @description Compute the point of the disk realizing the shortest distance to a line of space, and the associated
     *              point of the line. The output vector must be the one of the shape and the one of the line realizing
     *              the shortest distance.
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria Everything as expected with an epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testClosestPointToLine() {

        // Creation of the disk
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final double radius = 4.0;
        final Disk disk = new Disk(center, normal, radius);

        // test with an intersecting line
        Vector3D origLine = new Vector3D(0.0, 3.0, 2.0);
        Vector3D dirLine = new Vector3D(1.0, -1.0, -1.0);
        Line line = new Line(origLine, origLine.add(dirLine));

        Vector3D[] closestPoints = disk.closestPointTo(line);

        Vector3D point1 = closestPoints[0];
        Vector3D point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, point2.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getZ(), this.comparisonEpsilon);

        // test with a line parallel to the ellipse
        origLine = new Vector3D(2.0, 1.0, 1.0);
        dirLine = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = disk.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(2.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-3.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(-3.0, point2.getY(), this.comparisonEpsilon);

        // test with a line not parallel to the ellipse
        origLine = new Vector3D(1.0, 6.0, 1.0);
        dirLine = new Vector3D(1.0, 0.0, 0.0);
        line = new Line(origLine, origLine.add(dirLine));

        closestPoints = disk.closestPointTo(line);

        point1 = closestPoints[0];
        point2 = closestPoints[1];

        Assert.assertEquals(1.0, point1.getX(), this.comparisonEpsilon);
        Assert.assertEquals(6.0, point1.getY(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point1.getZ(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, point2.getX(), this.comparisonEpsilon);
        Assert.assertEquals(5.0, point2.getY(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DISK_SHAPE}
     * 
     * @testedMethod {@link Disk#toString()}
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

        // Creation of the disk
        final Vector3D center = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D normal = new Vector3D(2.0, 0.0, 0.0);
        final double radius = 4.0;
        final Disk disk = new Disk(center, normal, radius);

        // string creation
        final String result = disk.toString();

        final String expected = "Disk{center{1; 1; 1},normal{1; 0; 0},radius{4.0}}";
        Assert.assertEquals(expected, result);
    }

}
