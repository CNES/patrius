/**
 * 
 * Copyright 2021-2021 CNES
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
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitude;
import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation.OrientationType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the class {@link CelestialBodyTabulatedOrientation}.
 * 
 * @author Thibaut BONIT
 * 
 * @since 4.13
 */
public class CelestialBodyTabulatedOrientationTest {

    /** Epsilon for double comparison. */
    private static final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Initialized attitude leg. */
    private TabulatedAttitude attLeg;

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @description Builds a new instance and tests the basic getters.
     * 
     * @testPassCriteria The instance is build without error and the basic getters return the expected data.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testConstructor() throws PatriusException {

        final CelestialBodyTabulatedOrientation orientation = new CelestialBodyTabulatedOrientation(this.attLeg);

        Assert.assertEquals(this.attLeg, orientation.getTabulatedAttitude());
        Assert.assertEquals(0.5, orientation.getDH(), 0.); // Non regression on the default value
        orientation.setDH(1.);
        Assert.assertEquals(1., orientation.getDH(), 0.);

        // Test to set a non-strictly positive dH (should fail)
        try {
            orientation.setDH(0.);
            Assert.fail();
        } catch (final NotStrictlyPositiveException e) {
            Assert.assertTrue(true);
        }

        // Note: TimeStampedAngularCoordinates doesn't implement equals() so compare only the rotation with isEqualTo()
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date0 = this.attLeg.getAttitudes().get(0).getDate();
        final AbsoluteDate date1 = this.attLeg.getAttitudes().get(1).getDate();
        final AbsoluteDate date3 = this.attLeg.getAttitudes().get(3).getDate();

        Assert.assertTrue(this.attLeg.getAttitude(date0).getOrientation().getRotation()
            .isEqualTo(orientation.getOrientation(date0).getRotation()));
        Assert.assertTrue(this.attLeg.getAttitude(date0.shiftedBy(5)).getOrientation().getRotation()
            .isEqualTo(orientation.getOrientation(date0.shiftedBy(5)).getRotation()));

        Assert.assertTrue(this.attLeg.getAttitude(date1.shiftedBy(5), gcrf).getOrientation().getRotation()
            .isEqualTo(orientation.getOrientation(date1.shiftedBy(5), gcrf).getRotation()));
        Assert.assertTrue(this.attLeg.getAttitude(date3.shiftedBy(7), gcrf).getOrientation().getRotation()
            .isEqualTo(orientation.getOrientation(date3.shiftedBy(7), gcrf).getRotation()));
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @description Builds a new instance and tests the orientation features.
     * 
     * @testPassCriteria The orientation features return the expected data.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testOrientationFeatures() throws PatriusException {

        final AbsoluteDate date0 = this.attLeg.getAttitudes().get(0).getDate();
        final AbsoluteDate date1 = date0.shiftedBy(10.);
        final AbsoluteDate date2 = date0.shiftedBy(35.);
        final Frame icrf = FramesFactory.getICRF();

        final CelestialBodyTabulatedOrientation orientation = new CelestialBodyTabulatedOrientation(this.attLeg);

        // Evaluate the getPole method
        final Vector3D expectedPole1 = AngularCoordinates.estimateRate(
            this.attLeg.getAttitude(date1.shiftedBy(-0.5)).getRotation(), // dH = 0.5 (default value)
            this.attLeg.getAttitude(date1.shiftedBy(+0.5)).getRotation(), 2. * 0.5);
        Assert.assertTrue(orientation.getPole(date0.shiftedBy(10.)).distance(expectedPole1) < eps);

        orientation.setDH(1.2); // Change the value of dH
        final Vector3D expectedPole2 = AngularCoordinates.estimateRate(
            this.attLeg.getAttitude(date2.shiftedBy(-1.2)).getRotation(),
            this.attLeg.getAttitude(date2.shiftedBy(+1.2)).getRotation(), 2. * 1.2);
        Assert.assertTrue(orientation.getPole(date2).distance(expectedPole2) < eps);
        orientation.setDH(0.5); // Reset the default value of dH

        // Test to get a pole at a date which isn't compatible with the interval
        // (outside [startDate + dH ; endDate - dH]) (should fail)
        final AbsoluteDateInterval interval = this.attLeg.getTimeInterval();
        try {
            orientation.getPole(interval.getLowerData().shiftedBy(orientation.getDH() - 1e-7));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            orientation.getPole(interval.getUpperData().shiftedBy(-orientation.getDH() + 1e-7));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Evaluate the getPoleDerivative method
        final Vector3D temp1PoleDerivative1 = AngularCoordinates.estimateRate(
            this.attLeg.getAttitude(date1.shiftedBy(-2 * 0.5)).getRotation(),
            this.attLeg.getAttitude(date1).getRotation(), 2. * 0.5);
        final Vector3D temp2PoleDerivative1 = AngularCoordinates.estimateRate(
            this.attLeg.getAttitude(date1).getRotation(),
            this.attLeg.getAttitude(date1.shiftedBy(+2 * 0.5)).getRotation(), 2. * 0.5);
        final Vector3D expectedPoleDerivative1 = temp2PoleDerivative1.subtract(temp1PoleDerivative1).scalarMultiply(
            1 / (2. * 0.5));
        Assert
            .assertTrue(orientation.getPoleDerivative(date0.shiftedBy(10.)).distance(expectedPoleDerivative1) < eps);

        orientation.setDH(1.2); // Change the value of dH
        final Vector3D temp1PoleDerivative2 = AngularCoordinates.estimateRate(
            this.attLeg.getAttitude(date2.shiftedBy(-2 * 1.2)).getRotation(),
            this.attLeg.getAttitude(date2).getRotation(), 2. * 1.2);
        final Vector3D temp2PoleDerivative2 = AngularCoordinates.estimateRate(
            this.attLeg.getAttitude(date2).getRotation(),
            this.attLeg.getAttitude(date2.shiftedBy(+2 * 1.2)).getRotation(), 2. * 1.2);
        final Vector3D expectedPoleDerivative2 = temp2PoleDerivative2.subtract(temp1PoleDerivative2).scalarMultiply(
            1 / (2. * 1.2));
        Assert
            .assertTrue(orientation.getPoleDerivative(date2).distance(expectedPoleDerivative2) < eps);
        orientation.setDH(0.5); // Reset the default value of dH

        // Test to get a pole derivative at a date which isn't compatible with the interval
        // (outside [startDate + 2 * dH ; endDate- 2 * dH]) (should fail)
        try {
            orientation.getPoleDerivative(interval.getLowerData().shiftedBy(2. * orientation.getDH() - 1e-7));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            orientation.getPoleDerivative(interval.getUpperData().shiftedBy(-2. * orientation.getDH() + 1e-7));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Evaluate the getAngularCoordinates method (based on getPrimeMeridianAngle method which is already validated)
        // ICRF_TO_ROTATING
        Assert.assertEquals(this.attLeg.getAttitude(date0).getOrientation(),
            orientation.getAngularCoordinates(date0, OrientationType.ICRF_TO_ROTATING));
        Assert.assertEquals(this.attLeg.getAttitude(date2).getOrientation(),
            orientation.getAngularCoordinates(date2, OrientationType.ICRF_TO_ROTATING));

        // getAngularCoordinates(AbsoluteDate) method should call the ICRF_TO_ROTATING mode
        Assert.assertEquals(orientation.getAngularCoordinates(date0),
            orientation.getAngularCoordinates(date0, OrientationType.ICRF_TO_ROTATING));
        Assert.assertEquals(orientation.getAngularCoordinates(date2),
            orientation.getAngularCoordinates(date2, OrientationType.ICRF_TO_ROTATING));

        // ICRF_TO_INERTIAL
        // Already validated through the original AbstractCelestialBody validation

        // INERTIAL_TO_ROTATING
        // Temp1 & temp2 are already validated, we use them to build the frames composition
        final AbsoluteDate date0Bis = date0.shiftedBy(10.);
        AngularCoordinates temp1 = orientation.getAngularCoordinates(date0Bis, OrientationType.ICRF_TO_INERTIAL);
        AngularCoordinates temp2 = orientation.getAngularCoordinates(date0Bis, OrientationType.ICRF_TO_ROTATING);
        Frame f1 = new Frame(icrf, new Transform(date0Bis, temp1).getInverse(), "f1");
        Frame f2 = new Frame(f1, new Transform(date0Bis, temp2), "f2");
        Transform t = icrf.getTransformTo(f2, date0Bis);
        Assert.assertEquals(t.getAngular(),
            orientation.getAngularCoordinates(date0Bis, OrientationType.INERTIAL_TO_ROTATING));

        temp1 = orientation.getAngularCoordinates(date2, OrientationType.ICRF_TO_INERTIAL);
        temp2 = orientation.getAngularCoordinates(date2, OrientationType.ICRF_TO_ROTATING);
        f1 = new Frame(icrf, new Transform(date2, temp1).getInverse(), "f1");
        f2 = new Frame(f1, new Transform(date2, temp2), "f2");
        t = icrf.getTransformTo(f2, date2);
        Assert.assertEquals(t.getAngular(),
            orientation.getAngularCoordinates(date2, OrientationType.INERTIAL_TO_ROTATING));

        // Evaluate the getPrimeMeridianAngle method (based on getAngularCoordinates method which is already validated)
        Assert.assertEquals(
            orientation.getAngularCoordinates(date1, OrientationType.INERTIAL_TO_ROTATING).getRotation()
                .getAngle(), orientation.getPrimeMeridianAngle(date1), 0.);
        Assert.assertEquals(orientation.getAngularCoordinates(date2, OrientationType.INERTIAL_TO_ROTATING)
            .getRotation().getAngle(), orientation.getPrimeMeridianAngle(date2), 0.);

        // Evaluate the getPrimeMeridianAngleDerivative method
        Assert.assertEquals(
            (orientation.getPrimeMeridianAngle(date1.shiftedBy(0.5)) - orientation.getPrimeMeridianAngle(date1
                .shiftedBy(-0.5))) / (2 * 0.5), orientation.getPrimeMeridianAngleDerivative(date1), 0.);

        Assert.assertEquals(
            (orientation.getPrimeMeridianAngle(date2.shiftedBy(0.5)) - orientation.getPrimeMeridianAngle(date2
                .shiftedBy(-0.5))) / (2 * 0.5), orientation.getPrimeMeridianAngleDerivative(date2), 0.);
    }

    /**
     * @testType UT
     * 
     * @description test toString method of {@link CelestialBodyTabulatedOrientation}
     * 
     * @testPassCriteria String is as expected
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testToString() {
        final CelestialBodyTabulatedOrientation orientation = new CelestialBodyTabulatedOrientation(null);
        Assert.assertEquals("Tabulated celestial body orientation", orientation.toString());
    }

    /**
     * SetUp.
     * 
     * @throws PatriusException
     *         f the number of points used for interpolation is < 1 and != -1
     *         if there is not enough data for Hermite interpolation
     */
    @Before
    public void setUp() throws PatriusException {
        // Attitudes creation
        final Rotation rot1 = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Rotation rot2 = new Rotation(false, 0.64, 0.48, 0.48, 0.36);
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = date0.shiftedBy(10.0);
        final AbsoluteDate date2 = date0.shiftedBy(20.0);
        final AbsoluteDate date3 = date0.shiftedBy(30.0);
        final AbsoluteDate date4 = date0.shiftedBy(40.0);

        final Frame eme2000 = FramesFactory.getEME2000();
        final Attitude att0 = new Attitude(date0, eme2000, rot1, Vector3D.ZERO);
        final Attitude att1 = new Attitude(date1, eme2000, rot2, Vector3D.ZERO);
        final Attitude att2 = new Attitude(date2, eme2000, rot1, Vector3D.ZERO);
        final Attitude att3 = new Attitude(date3, eme2000, rot2, Vector3D.ZERO);
        final Attitude att4 = new Attitude(date4, eme2000, rot1, Vector3D.ZERO);
        final List<Attitude> attList = Arrays.asList(att0, att1, att2, att3, att4);

        // Attitude leg creation
        this.attLeg = new TabulatedAttitude(attList, 4);
    }
}
