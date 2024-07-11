/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * @description <p>
 *              Validation tests for the object LineSegment.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: LineSegmentTest.java 17909 2017-09-11 11:57:36Z bignon $
 * 
 * @since 1.0
 * 
 */
public class LineSegmentTest {
    /** Features description. */
    public enum features {

        /**
         * @featureTitle Line segment
         * 
         * @featureDescription Creation of a line segment, computation of distance to lines.
         * 
         * @coveredRequirements DV-GEOMETRIE_50, DV-GEOMETRIE_60, DV-GEOMETRIE_90, DV-GEOMETRIE_120, DV-GEOMETRIE_130
         */
        LINE_SEGMENT
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon taking into account the machine error. */
    private final double machineEpsilon = Precision.EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LINE_SEGMENT}
     * 
     * @testedMethod {@link LineSegment#LineSegment(Vector3D, Vector3D, double)}
     * 
     * @description Instantiation of a line segment from its origin, direction and length.
     * 
     * @input A vector origin, direction vectors (OK and with null norm) and lengths (positive, null, negative).
     * 
     * @output LineSegment
     * 
     * @testPassCriteria The segment can be created only if the direction's norm and length are strictly positive, an
     *                   exception is thrown otherwise. We check the returned elements (origin, direction) with the ones
     *                   given at the construction with an epsilon of 1e-16 which takes into account the machine error
     *                   only.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testConstructor() {

        // Creation of a segment, nominal case
        final Vector3D origin = new Vector3D(1.0, 1.0, 1.0);
        Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        double length = 2.0;

        LineSegment segment = new LineSegment(origin, direction, length);

        final Vector3D outOrigin = segment.getOrigin();
        final Vector3D outDirection = segment.getDirection();
        final double outLength = segment.getLength();
        final Vector3D outEnd = segment.getEnd();

        Assert.assertEquals(origin.getX(), outOrigin.getX(), this.machineEpsilon);
        Assert.assertEquals(origin.getY(), outOrigin.getY(), this.machineEpsilon);
        Assert.assertEquals(origin.getZ(), outOrigin.getZ(), this.machineEpsilon);

        Assert.assertEquals(origin.getX() + length, outEnd.getX(), this.machineEpsilon);
        Assert.assertEquals(origin.getY(), outEnd.getY(), this.machineEpsilon);
        Assert.assertEquals(origin.getZ(), outEnd.getZ(), this.machineEpsilon);

        Assert.assertEquals(1.0, outDirection.getX(), this.machineEpsilon);
        Assert.assertEquals(0.0, outDirection.getY(), this.machineEpsilon);
        Assert.assertEquals(0.0, outDirection.getZ(), this.machineEpsilon);

        Assert.assertEquals(length, outLength, this.machineEpsilon);

        // test with wrong direction
        direction = new Vector3D(0.0, 0.0, 0.0);
        try {
            segment = new LineSegment(origin, direction, length);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        // tests with wrong length
        direction = new Vector3D(0.0, 1.0, 1.0);
        length = 0.0;
        try {
            segment = new LineSegment(origin, direction, length);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
        length = -1.0;
        try {
            segment = new LineSegment(origin, direction, length);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LINE_SEGMENT}
     * 
     * @testedMethod {@link LineSegment#distanceTo(Line)}
     * 
     * @description Compute the shortest distance between the segment and a line of space.
     * 
     * @input Lines of space
     * 
     * @output doubles : the distances
     * 
     * @testPassCriteria The output doubles must be the right distance, zero if the line touches the segment with an
     *                   epsilon of 1e-14 due to the computation errors.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testDistanceToLine() {

        // Creation of a segment, nominal case
        final Vector3D origin = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double length = 2.0;

        final LineSegment segment = new LineSegment(origin, direction, length);

        // test with a line closest to the origin
        Vector3D originLine = new Vector3D(0.0, 0.0, 0.0);
        Vector3D directionLine = new Vector3D(0.0, 1.0, 1.0);
        Line line = new Line(originLine, directionLine);

        double distance = segment.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line closest to the origin
        originLine = new Vector3D(1.0, 0.0, 1.0);
        directionLine = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        distance = segment.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line closest to the end
        originLine = new Vector3D(5.0, 0.0, 1.0);
        directionLine = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        distance = segment.distanceTo(line);
        Assert.assertEquals(MathLib.sqrt(2.0) / 2.0, distance, this.comparisonEpsilon);

        // test with a line closest to the middle
        originLine = new Vector3D(3.0, 0.0, 2.0);
        directionLine = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        distance = segment.distanceTo(line);
        Assert.assertEquals(1.0, distance, this.comparisonEpsilon);

        // test with a line touching the segment
        originLine = new Vector3D(3.0, 0.0, 1.0);
        directionLine = new Vector3D(-1.0, 1.0, 0.0);
        line = new Line(originLine, originLine.add(directionLine));

        distance = segment.distanceTo(line);
        Assert.assertEquals(0.0, distance, this.comparisonEpsilon);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LINE_SEGMENT}
     * 
     * @testedMethod {@link LineSegment#toString()}
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

        // Creation of a segment, nominal case
        final Vector3D origin = new Vector3D(1.0, 1.0, 1.0);
        final Vector3D direction = new Vector3D(2.0, 0.0, 0.0);
        final double length = 2.0;

        final LineSegment segment = new LineSegment(origin, direction, length);

        // string creation
        final String result = segment.toString();

        final String expected = "LineSegment{Origin{1; 1; 1},Direction{1; 0; 0},Length{2.0}}";
        Assert.assertEquals(expected, result);
    }
}
