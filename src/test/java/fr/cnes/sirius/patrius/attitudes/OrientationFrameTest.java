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
 * @history creation 09/03/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.directions.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Coverage tests for the orientation frames.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: OrientationFrameTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class OrientationFrameTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Orientation frame
         * 
         * @featureDescription Specific frame to define an orientation
         * 
         * @coveredRequirements DV-ATT_80, DV-REPERES_60
         */
        ORIENTATION_FRAME
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Reference frame. */
    private final Frame refFrame = FramesFactory.getGCRF();

    /** reference date */
    private final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ORIENTATION_FRAME}
     * 
     * @testedMethod {@link OrientationFrame#OrientationFrame(IOrientationLaw, OrientationFrame)}
     * 
     * @description creation of an orientation frame and test of its update method
     *              by asking for frames transformations.
     * 
     * @input an orientation law, a reference frame, a date for the computation,
     *        a frame to get the transformation.
     * 
     * @output the transformation to another frame
     * 
     * @testPassCriteria the transformation is the expected one (translation null and rotation OK).
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void updateFrameTest() {

        // basic attitude law creation
        final Rotation pAttitude = new Rotation(Vector3D.PLUS_K, FastMath.PI / 4.0);
        final Attitude inAttitude = new Attitude(this.date, this.refFrame, pAttitude, Vector3D.PLUS_K);
        final AttitudeLaw attitudeLaw = new BasicAttitudeProvider(inAttitude);

        // basic orientation law creation
        final Rotation orientationRotation = new Rotation(Vector3D.PLUS_K, FastMath.PI / 8.0);
        final IOrientationLaw orientationLaw = new BasicOrientationLaw(orientationRotation, this.refFrame);

        // pv coordinates creation
        final Vector3D position = new Vector3D(8.0, 5.0, -4.0);
        final Vector3D velocity = new Vector3D(7.0, -3.0, 2.0);
        final PVCoordinates inCoordinates = new PVCoordinates(position, velocity);
        final PVCoordinatesProvider pv = new BasicPVCoordinatesProvider(inCoordinates, this.refFrame);

        // creation of an orientation frame from an attitude frame
        // ========================================================
        try {
            // attitude frame creation
            final AttitudeFrame attitudeFrame = new AttitudeFrame(pv, attitudeLaw, this.refFrame);

            // orientation fram ecreation
            final OrientationFrame orientationFrame = new OrientationFrame(orientationLaw, attitudeFrame);

            // transformation getting : opposite transformation in the reference frame
            final Transform transformation = this.refFrame.getTransformTo(orientationFrame, this.date);

            Rotation outputRotation = transformation.getRotation();
            Vector3D outputPosition = transformation.getTranslation();

            // test of the transformation
            // translation
            Assert.assertEquals(8.0, outputPosition.getX(), this.comparisonEpsilon);
            Assert.assertEquals(5.0, outputPosition.getY(), this.comparisonEpsilon);
            Assert.assertEquals(-4.0, outputPosition.getZ(), this.comparisonEpsilon);

            // rotation
            double outputAngle = (outputRotation.applyInverseTo(pAttitude)).getAngle();

            Assert.assertEquals(FastMath.PI / 8.0, outputAngle, this.comparisonEpsilon);

            // creation of an orientation frame from a previous orientation frame
            // ===================================================================
            // orientation fram ecreation
            final OrientationFrame orientationFrame2 = new OrientationFrame(orientationLaw, orientationFrame);

            // transformation getting : opposite transformation in the reference frame
            final Transform transformation2 = this.refFrame.getTransformTo(orientationFrame2, this.date);

            outputRotation = transformation2.getRotation();
            outputPosition = transformation2.getTranslation();

            // test of the transformation
            // translation
            Assert.assertEquals(8.0, outputPosition.getX(), this.comparisonEpsilon);
            Assert.assertEquals(5.0, outputPosition.getY(), this.comparisonEpsilon);
            Assert.assertEquals(-4.0, outputPosition.getZ(), this.comparisonEpsilon);

            // rotation
            outputAngle = (outputRotation.applyInverseTo(pAttitude)).getAngle();

            Assert.assertEquals(FastMath.PI / 4.0, outputAngle, this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

}
