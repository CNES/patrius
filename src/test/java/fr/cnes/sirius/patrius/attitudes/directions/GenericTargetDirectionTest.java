/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 30/11/2011
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitudeTest;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the directions described by its target point, represented by a PVCoordinatesProvider.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: GenericTargetDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class GenericTargetDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Geneneric target direction
         * 
         * @featureDescription direction described by its target point,
         *                     represented by a PVCoordinatesProvider.
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170, DV-GEOMETRIE_190
         */
        GENERIC_ORIGIN_TARGET_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TabulatedAttitudeTest.class.getSimpleName(), "Generic target direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_ORIGIN_TARGET_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by its
     *              target point, and getting of the vector expressed
     *              in a frame, at a date, giving the origin point.
     * 
     * @input the origin and target created as basic PVCoordinatesProvider
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the returned vector is the correct one between the origin and the target
     *                   The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
            ComparisonType.ABSOLUTE);

        // origin and target creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
        final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
        final Vector3D targetPos = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D targetVel = new Vector3D(-8.657, 657.5764, 567.1596);

        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        final PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);

        final BasicPVCoordinatesProvider originIn = new BasicPVCoordinatesProvider(originPV, frame);
        final BasicPVCoordinatesProvider targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        // Direction creation
        final GenericTargetDirection direction = new GenericTargetDirection(targetIn);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Vector3D expected = targetPos.subtract(originPos);

        try {
            final Vector3D result = direction.getVector(originIn, date, frame);

            Assert.assertEquals(expected.getX(), result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getY(), result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(expected.getZ(), result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", expected, result);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_ORIGIN_TARGET_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getTargetPVCoordinates(AbsoluteDate, Frame)}
     * 
     * 
     * @description Instantiation of a direction described by its
     *              target point, and getting of the target
     *              PV coordinates expressed in a frame, at a date.
     * 
     * @input the target created as basic PVCoordinatesProvider
     * 
     * @output PVCoordinates
     * 
     * @testPassCriteria the returned coordinates are identical to the ones used to
     *                   create the direction. The 1.0e-14 epsilon is the simple double comparison epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetTarget() {
        // origin and target creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D targetPos = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D targetVel = new Vector3D(-8.657, 657.5764, 567.1596);

        final PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);

        final BasicPVCoordinatesProvider targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        // Direction creation
        final GenericTargetDirection direction = new GenericTargetDirection(targetIn);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        try {
            final PVCoordinates outTarget = direction.getTargetPVCoordinates(date, frame);
            final Vector3D resultPos = outTarget.getPosition();
            final Vector3D resultVel = outTarget.getVelocity();

            Assert.assertEquals(targetPos.getX(), resultPos.getX(), this.comparisonEpsilon);
            Assert.assertEquals(targetPos.getY(), resultPos.getY(), this.comparisonEpsilon);
            Assert.assertEquals(targetPos.getZ(), resultPos.getZ(), this.comparisonEpsilon);

            Assert.assertEquals(targetVel.getX(), resultVel.getX(), this.comparisonEpsilon);
            Assert.assertEquals(targetVel.getY(), resultVel.getY(), this.comparisonEpsilon);
            Assert.assertEquals(targetVel.getZ(), resultVel.getZ(), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GENERIC_ORIGIN_TARGET_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by its
     *              target point, and getting of the line containing a given origin and
     *              target point.
     * 
     * @input the origin and target created as basic PVCoordinatesProvider
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains both points. An exception is returned if
     *                   the points are identical. The 1.0e-14 epsilon is the simple double comparison epsilon, used
     *                   because
     *                   the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testGetLine() {
        // origin and target creation
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D originPos = new Vector3D(1.635732, -8.654534, 5.6721);
        final Vector3D originVel = new Vector3D(7.6874231, 654.687534, -17.721);
        Vector3D targetPos = new Vector3D(721.54457534, 8785.65721, -687424.654);
        final Vector3D targetVel = new Vector3D(-8.657, 657.5764, 567.1596);

        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        PVCoordinates targetPV = new PVCoordinates(targetPos, targetVel);

        final BasicPVCoordinatesProvider originIn = new BasicPVCoordinatesProvider(originPV, frame);
        BasicPVCoordinatesProvider targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        // Direction creation
        GenericTargetDirection direction = new GenericTargetDirection(targetIn);

        // test
        // the frame and date have no meaning here
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        try {
            // line creation
            final Line line = direction.getLine(originIn, date, frame);
            // test of the points
            Assert.assertTrue(line.contains(originPos));
            Assert.assertTrue(line.contains(targetPos));

        } catch (final PatriusException e) {
            Assert.fail();
        }

        // test with identical origin and target points
        targetPos = originPos;
        targetPV = new PVCoordinates(targetPos, targetVel);
        targetIn = new BasicPVCoordinatesProvider(targetPV, frame);

        direction = new GenericTargetDirection(targetIn);

        try {
            direction.getLine(originIn, date, frame);

            // an exception must be thrown
            Assert.fail();

        } catch (final PatriusException e) {
            // expected !
        }
    }
}
