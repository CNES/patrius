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
 * @history creation 08/03/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3157:10/05/2022:[PATRIUS] Construction d'un AttitudeFrame a partir d'un AttitudeProvider 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.6:FA:FA-2589:27/01/2021:[PATRIUS] Bug dans AttitudeTransformProvider 
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
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Coverage tests for the attitude frames.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: AttitudeFrameTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class AttitudeFrameTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Attitude frame
         * 
         * @featureDescription Specific frame to define an attitude
         * 
         * @coveredRequirements DV-ATT_10, DV-ATT_80, DV-REPERES_60
         */
        ATTITUDE_FRAME
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
     * @testedFeature {@link features#ATTITUDE_FRAME}
     * 
     * @testedMethod {@link AttitudeFrame#AttitudeFrame(PVCoordinatesProvider, AttitudeProvider, Frame)}
     * @testedMethod {@link AttitudeFrame#getAttitudeProvider()}
     * 
     * @description creation of an attitude frame and test of its update method
     *              by asking for frames transformations.
     * 
     * @input the spacecraft PV, an attitude provider, a reference frame, a date for the computation,
     *        a frame to get the transformation.
     * 
     * @output the transformation to another frame
     * 
     * @testPassCriteria the transformation is the expected one (translation and rotation).
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

        // pv coordinates creation
        final Vector3D position = new Vector3D(8.0, 5.0, -4.0);
        final Vector3D velocity = new Vector3D(7.0, -3.0, 2.0);
        final PVCoordinates inCoordinates = new PVCoordinates(position, velocity);
        final PVCoordinatesProvider pv = new BasicPVCoordinatesProvider(inCoordinates, this.refFrame);

        try {
            // attitude frame creation
            final AttitudeFrame attitudeFrame = new AttitudeFrame(pv, attitudeLaw, this.refFrame);

            // transformation getting : opposite transformation in the reference frame
            final Transform transformation = this.refFrame.getTransformTo(attitudeFrame, this.date);

            final Rotation outputRotation = transformation.getRotation();
            final Vector3D outputPosition = transformation.getTranslation();

            // test of the transformation
            // translation
            Assert.assertEquals(position.getX(), outputPosition.getX(), this.comparisonEpsilon);
            Assert.assertEquals(position.getY(), outputPosition.getY(), this.comparisonEpsilon);
            Assert.assertEquals(position.getZ(), outputPosition.getZ(), this.comparisonEpsilon);

            // rotation
            double outputNullAngle = (outputRotation.applyInverseTo(pAttitude)).getAngle();
            Assert.assertEquals(0.0, outputNullAngle, this.comparisonEpsilon);

            // attitude law getting
            final AttitudeProvider attLaw = attitudeFrame.getAttitudeProvider();
            outputNullAngle =
                (attLaw.getAttitude(pv, this.date, this.refFrame).getRotation().applyInverseTo(pAttitude))
                    .getAngle();
            Assert.assertEquals(0.0, outputNullAngle, this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_FRAME}
     * 
     * @description check second order derivative is properly computed.
     * 
     * @testPassCriteria the second derivative is the expected one as defined in the attitude law (reference: math, relative threshold: 0).
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public void secondDerivativesTest() throws PatriusException {
        
        // Initialization
        final Frame frame = FramesFactory.getGCRF();
        // Build an attitude law with constant rotation acceleration (0, 0, 1)
        final AttitudeLaw attitudeLaw = new AttitudeLaw() {
            /** Serializable UID. */
            private static final long serialVersionUID = 1964871911597720230L;

            @Override
            public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
                // nothing to do
            }
            
            @Override
            public Attitude getAttitude(final Orbit orbit) throws PatriusException {
                return getAttitude(orbit, orbit.getDate(), orbit.getFrame());
            }
            
            @Override
            public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                    final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                final AngularCoordinates ac = new AngularCoordinates(Rotation.IDENTITY, Vector3D.PLUS_I, Vector3D.PLUS_K);
                return new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF(), ac);
            }
        };

        // Build attitude frame and compute transformation from reference frame
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, frame, AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
        final AttitudeFrame attitudeFrame = new AttitudeFrame(orbit, attitudeLaw, frame);
        final Transform transformation = frame.getTransformTo(attitudeFrame, orbit.getDate(), true);

        // Second derivative is expected to be (0, 0, 1)
        final Vector3D acc = transformation.getRotationAcceleration();
        Assert.assertEquals(0, acc.distance(Vector3D.PLUS_K), 0.);
    }
    
    /**
     * Test the attitude frame constructor with its attitude provider getter.
     * 
     * @throws PatriusException if a problem occurs during frames transformations
     * 
     * @testedMethod {@link AttitudeFrame#AttitudeFrame(PVCoordinatesProvider,
     *               AttitudeProvider, Frame)}
     * @testedMethod {@link AttitudeFrame#getAttitudeProvider()}
     */
    @Test
    public void testAttitudeProvider() throws PatriusException {
        // Define the frame
        final Frame frame = FramesFactory.getGCRF();
        // Define the orbit
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, frame,
            AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
        // Create the attitude provider
        final AttitudeProvider attitudeProvider = new AttitudeProvider(){

            /** Serializable UID. */
            private static final long serialVersionUID = 8349380468661246626L;

            /**
             * Set the spin derivatives computations.
             */
            @Override
            public void setSpinDerivativesComputation(final boolean computeSpinDeriv) {
                // Nothing needs to be done here
            }

            /**
             * Get the attitude.
             */
            @Override
            public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                final AngularCoordinates angularCoord = new AngularCoordinates(Rotation.IDENTITY, Vector3D.PLUS_I,
                    Vector3D.PLUS_K);
                return new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF(), angularCoord);
            }
        };
        // Build the attitude frame
        final AttitudeFrame attitudeFrame = new AttitudeFrame(orbit, attitudeProvider, frame);
        // Check that the attitude frame has been correctly built
        Assert.assertNotNull(attitudeFrame);
        // Check that the attitude provider can be retrieved from the attitude frame
        Assert.assertEquals(attitudeProvider, attitudeFrame.getAttitudeProvider());
    }
}
