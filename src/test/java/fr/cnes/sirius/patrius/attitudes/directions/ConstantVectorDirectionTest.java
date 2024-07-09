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
 * HISTORY
* VERSION:4.6:FA:FA-2588:27/01/2021:[PATRIUS] erreur de calcul dans ConstantVectorDirection.getLine(…) 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
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
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the directions described by a vector constant in its frame.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: ConstantVectorDirectionTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class ConstantVectorDirectionTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Constant vector direction
         * 
         * @featureDescription direction described by a vector
         *                     constant in its frame.
         * 
         * @coveredRequirements DV-GEOMETRIE_160, DV-GEOMETRIE_170, DV-GEOMETRIE_190
         */
        CONSTANT_VECTOR_DIRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TabulatedAttitudeTest.class.getSimpleName(), "Constant vector direction");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_VECTOR_DIRECTION}
     * 
     * @testedMethod {@link ConstantVectorDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by a vector
     *              constant in its frame, and getting of the vector expressed
     *              in another frame.
     * 
     * @input a vector and its expression frame
     * 
     * @output Vector3D
     * 
     * @testPassCriteria the vector is identical when asked in the same frame, and correctly
     *                   transformed when asked in a different frame.
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

        // Direction creation
        final Vector3D inputVector = new Vector3D(1.0, 2.0, 5.0);
        final Frame parentFrame = FramesFactory.getGCRF();
        final Transform idTransform = new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.ZERO);
        final Frame inputFrame = new Frame(parentFrame, idTransform, "inputFrame");

        // test with a null - normed vector
        try {
            new ConstantVectorDirection(Vector3D.ZERO, inputFrame);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        final ConstantVectorDirection direction = new ConstantVectorDirection(inputVector, inputFrame);

        // frame getting
        final Frame inputFrameCopy = direction.getFrame();

        // output frame creation
        final Vector3D rotationVect = new Vector3D(1.0, 0.0, 0.0);
        final double rotationAngle = -MathUtils.HALF_PI;
        final Rotation rotationOut = new Rotation(rotationVect, rotationAngle);
        final Transform outTransform = new Transform(AbsoluteDate.J2000_EPOCH, rotationOut);
        final Frame outputFrame = new Frame(inputFrameCopy, outTransform, "outputFrame");

        // vector getting
        // the date has no meaning, the frame has no relative movement
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        try {
            // in the same frame
            Vector3D result = direction.getVector(
                new BasicPVCoordinatesProvider(PVCoordinates.ZERO, outputFrame), date, inputFrame);
            Assert.assertEquals(1.0, result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(2.0, result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(5.0, result.getZ(), this.comparisonEpsilon);

            // in another frame
            result = direction.getVector(
                new BasicPVCoordinatesProvider(PVCoordinates.ZERO, outputFrame), date, outputFrame);
            Assert.assertEquals(1.0, result.getX(), this.comparisonEpsilon);
            Assert.assertEquals(-5.0, result.getY(), this.comparisonEpsilon);
            Assert.assertEquals(2.0, result.getZ(), this.comparisonEpsilon);

            Report.printToReport("Direction", new Vector3D(1., -5., 2.), result);
        } catch (final PatriusException e) {
            Assert.fail();
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTANT_VECTOR_DIRECTION}
     * 
     * @testedMethod {@link GenericTargetDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of a direction described by a vector
     *              constant in its frame, and getting of the line containing a given origin and
     *              this vector.
     * 
     * @input the origin created as basic PVCoordinatesProvider and a constant vector direction
     * 
     * @output Line
     * 
     * @testPassCriteria the returned Line contains the origin and is directed by the direction. An exception
     *                   is returned if the points are identical. The 1.0e-14 epsilon is the simple double comparison
     *                   epsilon,
     *                   used because the computations involve here no mechanics algorithms.
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testGetLine() throws PatriusException {

        // Direction creation
        final Vector3D inputVectorInInputFrame = new Vector3D(1.0, 2.0, 5.0);
        final Frame parentFrame = FramesFactory.getGCRF();
        final Transform idTransform = new Transform(AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I);
        final Frame inputFrame = new Frame(parentFrame, idTransform, "inputFrame");
        final Vector3D inputVectorInParentFrame = inputFrame.getTransformTo(parentFrame, AbsoluteDate.J2000_EPOCH).transformVector(inputVectorInInputFrame);

        final ConstantVectorDirection direction = new ConstantVectorDirection(inputVectorInInputFrame, inputFrame);

        // line getting
        // the date has no meaning, the frame has no relative movement
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final PVCoordinatesProvider origin = new BasicPVCoordinatesProvider(PVCoordinates.ZERO, inputFrame);

        final Line line = direction.getLine(origin, date, parentFrame);

        // test of the points
        Assert.assertTrue(line.contains(origin.getPVCoordinates(date, parentFrame).getPosition()));
        Assert.assertTrue(line.contains(origin.getPVCoordinates(date, parentFrame).getPosition()
                .add(inputVectorInParentFrame)));
    }

}
