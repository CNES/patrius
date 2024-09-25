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
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitude;
import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation.OrientationType;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the celestial body with a tabulated orientation.
 * 
 * @author Thibaut BONIT
 * 
 * @since 4.13
 * 
 */
public class UserTabulatedCelestialBodyTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-dataCNES-2003");
        Report.printClassHeader(UserTabulatedCelestialBodyTest.class.getSimpleName(), "User-defined celestial body");
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @description Builds a celestial body with a tabulated orientation and evaluated the body inertial & rotating
     *              frames.
     * 
     * @testPassCriteria The frames are built as expected.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testBodyTabulatedFrames() throws PatriusException {

        // Attitudes creation
        final Rotation rot1 = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Rotation rot2 = new Rotation(false, 0.64, 0.48, 0.48, 0.36);
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = date0.shiftedBy(10.0);
        final AbsoluteDate date2 = date0.shiftedBy(20.0);
        final AbsoluteDate date3 = date0.shiftedBy(30.0);
        final AbsoluteDate date4 = date0.shiftedBy(40.0);

        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame icrf = FramesFactory.getICRF();

        Attitude att0 = new Attitude(date0, eme2000, rot1, Vector3D.ZERO);
        Attitude att1 = new Attitude(date1, eme2000, rot2, Vector3D.ZERO);
        Attitude att2 = new Attitude(date2, eme2000, rot1, Vector3D.ZERO);
        Attitude att3 = new Attitude(date3, eme2000, rot2, Vector3D.ZERO);
        Attitude att4 = new Attitude(date4, eme2000, rot1, Vector3D.ZERO);
        List<Attitude> attList = Arrays.asList(att0, att1, att2, att3, att4);

        // Attitude leg creation
        TabulatedAttitude attLeg = new TabulatedAttitude(attList, 4);

        CelestialBodyTabulatedOrientation orientation = new CelestialBodyTabulatedOrientation(attLeg);

        // Build a celestial body with a tabulated orientation
        AbstractCelestialBody body = new UserTabulatedCelestialBody("testName", Constants.EGM96_EARTH_MU, orientation,
            icrf);

        // Extract the 6 inertial/rotating frames from the evaluated model
        CelestialBodyFrame constantInertialFrame = body.getInertialFrame(IAUPoleModelType.CONSTANT);
        final CelestialBodyFrame meanInertialFrame = body.getInertialFrame(IAUPoleModelType.MEAN);
        final CelestialBodyFrame trueInertialFrame = body.getInertialFrame(IAUPoleModelType.TRUE);

        CelestialBodyFrame constantRotatingFrame = body.getRotatingFrame(IAUPoleModelType.CONSTANT);
        final CelestialBodyFrame meanRotatingFrame = body.getRotatingFrame(IAUPoleModelType.MEAN);
        final CelestialBodyFrame trueRotatingFrame = body.getRotatingFrame(IAUPoleModelType.TRUE);

        // The 3 inertial frames should be the same instance
        Assert.assertEquals(constantInertialFrame, meanInertialFrame);
        Assert.assertEquals(constantInertialFrame, trueInertialFrame);

        // The 3 rotating frames should be the same instance
        Assert.assertEquals(constantRotatingFrame, meanRotatingFrame);
        Assert.assertEquals(constantRotatingFrame, trueRotatingFrame);

        // Evaluate the inertial frames
        Assert.assertEquals("testName Inertial frame", constantInertialFrame.getName());
        Assert.assertEquals(body.getICRF(), constantInertialFrame.getParent());
        Assert.assertTrue(constantInertialFrame.isPseudoInertial());
        Assert.assertEquals(body, constantInertialFrame.getCelestialPoint());

        TransformProvider inertialFrameTransformProvider = constantInertialFrame.getTransformProvider();
        Transform transform = inertialFrameTransformProvider.getTransform(date1);

        AngularCoordinates angularCoord = orientation.getAngularCoordinates(date1, OrientationType.ICRF_TO_INERTIAL);
        Transform expectedTransform =
            new Transform(date1, angularCoord.getRotation(), angularCoord.getRotationRate(), null);

        evaluateTransform(transform, expectedTransform);

        // Evaluate the rotating frames
        Assert.assertEquals("testName Rotating frame", constantRotatingFrame.getName());
        Assert.assertEquals(body.getICRF(), constantRotatingFrame.getParent());
        Assert.assertFalse(constantRotatingFrame.isPseudoInertial());
        Assert.assertEquals(body, constantRotatingFrame.getCelestialPoint());

        final TransformProvider rotatingFrameTransformProvider = constantRotatingFrame.getTransformProvider();
        transform = rotatingFrameTransformProvider.getTransform(date1);

        angularCoord = orientation.getAngularCoordinates(date1, OrientationType.ICRF_TO_ROTATING);
        expectedTransform = new Transform(date1, angularCoord.getRotation(), angularCoord.getRotationRate(), null);

        evaluateTransform(transform, expectedTransform);

        // Special case: build a non-rotating celestial body and evaluate ICRF_TO_INERTIAL mode (through the inertial)
        att0 = new Attitude(date0, eme2000, rot1, Vector3D.ZERO);
        att1 = new Attitude(date1, eme2000, rot1, Vector3D.ZERO);
        att2 = new Attitude(date2, eme2000, rot1, Vector3D.ZERO);
        att3 = new Attitude(date3, eme2000, rot1, Vector3D.ZERO);
        att4 = new Attitude(date4, eme2000, rot1, Vector3D.ZERO);
        attList = Arrays.asList(att0, att1, att2, att3, att4);
        attLeg = new TabulatedAttitude(attList, 4);

        orientation = new CelestialBodyTabulatedOrientation(attLeg);

        body = new UserTabulatedCelestialBody("testName", Constants.EGM96_EARTH_MU, orientation, icrf);

        constantInertialFrame = body.getInertialFrame(IAUPoleModelType.CONSTANT);
        constantRotatingFrame = body.getRotatingFrame(IAUPoleModelType.CONSTANT);

        inertialFrameTransformProvider = constantInertialFrame.getTransformProvider();
        transform = inertialFrameTransformProvider.getTransform(date1);

        angularCoord = attLeg.getAttitude(date1).getOrientation();
        expectedTransform = new Transform(date1, angularCoord.getRotation(), angularCoord.getRotationRate(), null);
        evaluateTransform(transform, expectedTransform);

        // Special case: build a celestial body rotating at constant speed and evaluate ICRF_TO_INERTIAL mode (through
        // the inertial)
        final Rotation rot1Bis = new Rotation(false, 1., 0., 0., 0.);
        final Rotation rot2Bis = new Rotation(true, 1., 0., 0., 0.5); // Rotation along K axis
        att0 = new Attitude(date0, icrf, rot1Bis, Vector3D.ZERO);
        att4 = new Attitude(date4, icrf, rot2Bis, Vector3D.ZERO);
        attList = Arrays.asList(att0, att4);
        attLeg = new TabulatedAttitude(attList, 2);

        orientation = new CelestialBodyTabulatedOrientation(attLeg);

        body = new UserTabulatedCelestialBody("testName", Constants.EGM96_EARTH_MU, orientation, icrf);

        constantInertialFrame = body.getInertialFrame(IAUPoleModelType.CONSTANT);
        constantRotatingFrame = body.getRotatingFrame(IAUPoleModelType.CONSTANT);

        inertialFrameTransformProvider = constantInertialFrame.getTransformProvider();
        transform = inertialFrameTransformProvider.getTransform(date1);

        angularCoord = attLeg.getAttitude(date1).getOrientation();
        expectedTransform = new Transform(date1, Rotation.IDENTITY, Vector3D.ZERO, null);
        evaluateTransform(transform, expectedTransform);
    }

    /**
     * Compare the first {@link Transform} with the second (fail if they are not the same).
     * 
     * @param t1
     *        First transform
     * @param t2
     *        Second transform
     */
    private static void evaluateTransform(final Transform t1, final Transform t2) {
        Assert.assertEquals(t1.getDate(), t2.getDate());
        Assert.assertEquals(t1.getCartesian(), t2.getCartesian());
        Assert.assertEquals(t1.getVelocity(), t2.getVelocity());
        Assert.assertEquals(t1.getAcceleration(), t2.getAcceleration());
        Assert.assertEquals(t1.getRotation(), t2.getRotation());
        Assert.assertEquals(t1.getRotationRate(), t2.getRotationRate());
        Assert.assertEquals(t1.getRotationAcceleration(), t2.getRotationAcceleration());
    }

    /**
     * @description Try to implement a AbstractCelestialBody with a celestial body orientation which is nor a
     *              CelestialBodyIAUOrientation or a CelestialBodyTabulatedOrientation implementation (should fail).
     *
     * @testPassCriteria The exception is returned as expected.
     */
    @Test
    public void testUnsupportedOrientationType() {

        final CelestialBodyOrientation genericOrientation = new CelestialBodyOrientationForTest();

        // Test to implement a AbstractCelestialBody with a celestial body orientation which is nor a
        // CelestialBodyIAUOrientation or a CelestialBodyTabulatedOrientation implementation (should fail)
        try {
            new UserTabulatedCelestialBody("testName", Constants.EGM96_EARTH_MU, genericOrientation,
                FramesFactory.getICRF());
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Basic {@link AbstractCelestialBody} implementation.
     * <p>
     * The purpose of this class is to evaluate the body frames initialization with a tabulated orientation or a generic
     * one (error case).
     * </p>
     */
    protected static class UserTabulatedCelestialBody extends AbstractCelestialBody {

        /** Serializable UID. */
        private static final long serialVersionUID = -570571254560161403L;

        /**
         * Protected constructor designed only for the tests.
         * 
         * @param name
         *        name of the body
         * @param gm
         *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
         * @param celestialBodyOrientation
         *        tabulated celestial body orientation
         * @param parentFrame
         *        parent frame (usually it should be the ICRF centered on the parent body)
         */
        protected UserTabulatedCelestialBody(final String name, final double gm,
                                             final CelestialBodyOrientation celestialBodyOrientation,
                                             final Frame parentFrame) {
            super(name, gm, celestialBodyOrientation, parentFrame);
        }
    }

    /**
     * Basic direct {@link CelestialBodyOrientation} implementation.
     * <p>
     * The purpose of this class is only to put in evidence AbstractCelestialBody body cannot be build with
     * {@link CelestialBodyOrientation} direct implementations. It requires either {@link CelestialBodyIAUOrientation}
     * or a {@link CelestialBodyTabulatedOrientation} implementation to describe the celestial body orientation.
     * </p>
     */
    protected static class CelestialBodyOrientationForTest implements CelestialBodyOrientation {

        /** Serializable UID. */
        private static final long serialVersionUID = -2667982263661708813L;

        @Override
        public Vector3D getPole(final AbsoluteDate date) {
            return null;
        }

        @Override
        public Vector3D getPoleDerivative(final AbsoluteDate date) {
            return null;
        }

        @Override
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return 0;
        }

        @Override
        public double getPrimeMeridianAngleDerivative(final AbsoluteDate date) {
            return 0;
        }

        @Override
        public AngularCoordinates getAngularCoordinates(final AbsoluteDate date, final OrientationType orientationType) {
            return null;
        }
    }
}
