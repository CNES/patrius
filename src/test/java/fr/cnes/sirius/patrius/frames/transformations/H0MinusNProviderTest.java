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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:272:09/10/2014:added H0 - n frame
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1 + Step2)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:590:05/04/2016:correction to freeze H0 - n frame
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for H0MinusNProvider class.
 * 
 * @author Tiziana Sabatini
 * @version $Id: H0MinusNProviderTest.java 18088 2017-10-02 17:01:51Z bignon $
 * @since 2.3
 */
public class H0MinusNProviderTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(H0MinusNProviderTest.class.getSimpleName(), "H0 - n frame");
    }

    /**
     * Test the correct creation of the "H0-n" frame using the FramesFactory and the H0MinusNProvider classes.
     */
    @Test
    public void testH0MinusNProvider() throws PatriusException {

        Report.printMethodHeader("testH0MinusNProvider", "Frame conversion", "CNES",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        final String FRAME = "H0 minus n";
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2014, 10, 01),
            new TimeComponents(12, 0, 0.0), TimeScalesFactory.getUTC());

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        Frame h0MinusN = FramesFactory.getH0MinusN(FRAME, date, MathLib.toRadians(30.0));
        // line for coverage of getTransform method of FixedTransformProvider class:
        Assert.assertEquals(h0MinusN.getTransformProvider().getTransform(date), h0MinusN.getTransformProvider()
            .getTransform(date, null));
        Assert.assertEquals(h0MinusN.getTransformProvider().getTransform(date), h0MinusN.getTransformProvider()
            .getTransform(date, false));
        // Check the two getH0MinusN methods of the FramesFactory are equivalent:
        final Frame h0MinusN2 = FramesFactory.getH0MinusN(FRAME, date.shiftedBy(546.5), 546.5, MathLib.toRadians(30.0));
        Transform transform1 = h0MinusN.getTransformTo(itrf, date.shiftedBy(477893.2));
        Transform transform2 = h0MinusN2.getTransformTo(itrf, date.shiftedBy(477893.2));
        Quaternion quat1 = transform1.getRotation().getQuaternion();
        Quaternion quat2 = transform2.getRotation().getQuaternion();
        Vector3D spin1 = transform1.getRotationRate();
        Vector3D spin2 = transform2.getRotationRate();
        Assert.assertEquals(quat1.getQ0(), quat2.getQ0(), 0.0);
        Assert.assertEquals(quat1.getQ1(), quat2.getQ1(), 0.0);
        Assert.assertEquals(quat1.getQ2(), quat2.getQ2(), 0.0);
        Assert.assertEquals(quat1.getQ3(), quat2.getQ3(), 0.0);
        Assert.assertEquals(spin1.getX(), spin2.getX(), 0.0);
        Assert.assertEquals(spin1.getY(), spin2.getY(), 0.0);
        Assert.assertEquals(spin1.getZ(), spin2.getZ(), 0.0);

        transform1 = h0MinusN.getTransformTo(gcrf, date.shiftedBy(3600.0));
        transform2 = h0MinusN.getTransformTo(gcrf, date.shiftedBy(-Constants.JULIAN_YEAR));
        quat1 = transform1.getRotation().getQuaternion();
        quat2 = transform2.getRotation().getQuaternion();
        spin1 = transform1.getRotationRate();
        spin2 = transform2.getRotationRate();
        // Check the two transformations wrt the GCRF frame are identical (pseudo-inertial frame):
        Assert.assertEquals(quat1.getQ0(), quat2.getQ0(), 0.0);
        Assert.assertEquals(quat1.getQ1(), quat2.getQ1(), 0.0);
        Assert.assertEquals(quat1.getQ2(), quat2.getQ2(), 0.0);
        Assert.assertEquals(quat1.getQ3(), quat2.getQ3(), 0.0);
        Assert.assertEquals(spin1.getX(), spin2.getX(), 0.0);
        Assert.assertEquals(spin1.getY(), spin2.getY(), 0.0);
        Assert.assertEquals(spin1.getZ(), spin2.getZ(), 0.0);

        // Check the H0 - n frame is identical to the ITRF frame at the date H0 - n when the longitude is zero:
        h0MinusN = FramesFactory.getH0MinusN(FRAME, date, 0.0);
        Transform transform = h0MinusN.getTransformTo(itrf, date);
        Quaternion quat = transform.getRotation().getQuaternion();
        Assert.assertEquals(1.0, MathLib.abs(quat.getQ0()), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.0, quat.getQ1(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.0, quat.getQ2(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.0, quat.getQ3(), Precision.DOUBLE_COMPARISON_EPSILON);

        // Check the itrf / h0MinusN transformation, at the date H0 - n, is the rotation of the longitude angle:
        h0MinusN = FramesFactory.getH0MinusN(FRAME, date, MathLib.toRadians(30.0));
        transform = h0MinusN.getTransformTo(itrf, date);
        quat = transform.getRotation().getQuaternion();
        final Vector3D transformedVect = transform.transformVector(Vector3D.PLUS_I);
        Assert.assertEquals(0.866025403784439, transformedVect.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.5, transformedVect.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.0, transformedVect.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

        // Compare the H0 - n frame to the CNES reference:
        final AbsoluteDate testDate = date.shiftedBy(210.0);
        final double longitude = 0.2481;
        final Frame ref = this.getH0_CNES(longitude, testDate);
        final Frame res = FramesFactory.getH0MinusN(FRAME, testDate, longitude);
        date = testDate.shiftedBy(10500.0);
        final Transform resTransform = res.getTransformProvider().getTransform(date);
        final Rotation refRot = ref.getTransformProvider().getTransform(date).getRotation();
        final Rotation resRot = resTransform.getRotation();
        Assert.assertEquals(0., Rotation.distance(refRot, resRot), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.0, resTransform.getRotationRate().getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0.0, resTransform.getRotationAcceleration().getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Rotation distance", 0, Rotation.distance(refRot, resRot));
        Report.printToReport("Spin", Vector3D.ZERO, resTransform.getRotationAcceleration());
        Report.printToReport("Rotation acceleration", Vector3D.ZERO, resTransform.getRotationAcceleration());
    }

    /**
     * Coverage test for default constructor {@link H0MinusNProvider#H0MinusNProvider(AbsoluteDate, double)}.
     */
    @Test
    public void testConstructorH0MinusNProvider() throws PatriusException {

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2014, 10, 01),
            new TimeComponents(12, 0, 0.0), TimeScalesFactory.getUTC());
        final Frame frame1 = new Frame(gcrf, new H0MinusNProvider(date, MathLib.toRadians(30)), "FRAME", true);

        frame1.getTransformProvider().getTransform(date);
        // Check the two getH0MinusN methods of the FramesFactory are equivalent:
        final Frame frame2 = FramesFactory.getH0MinusN("FRAME", date.shiftedBy(546.5), 546.5, MathLib.toRadians(30.0));
        final Transform transform1 = frame1.getTransformTo(itrf, date.shiftedBy(477893.2));
        final Transform transform2 = frame2.getTransformTo(itrf, date.shiftedBy(477893.2));
        final Quaternion quat1 = transform1.getRotation().getQuaternion();
        final Quaternion quat2 = transform2.getRotation().getQuaternion();
        final Vector3D spin1 = transform1.getRotationRate();
        final Vector3D spin2 = transform2.getRotationRate();
        Assert.assertEquals(quat1.getQ0(), quat2.getQ0(), 0.0);
        Assert.assertEquals(quat1.getQ1(), quat2.getQ1(), 0.0);
        Assert.assertEquals(quat1.getQ2(), quat2.getQ2(), 0.0);
        Assert.assertEquals(quat1.getQ3(), quat2.getQ3(), 0.0);
        Assert.assertEquals(spin1.getX(), spin2.getX(), 0.0);
        Assert.assertEquals(spin1.getY(), spin2.getY(), 0.0);
        Assert.assertEquals(spin1.getZ(), spin2.getZ(), 0.0);

    }

    /**
     * This private function returns the CNES reference for the H0 minus N frame.
     * 
     * @param longitude
     *        the longitude
     * @param date
     *        the reference date
     * @return
     * @throws PatriusException
     */
    private Frame getH0_CNES(final double longitude, final AbsoluteDate date) throws PatriusException {

        // H0 frame:
        final Vector3D Z_H0 = Vector3D.PLUS_K;
        final Vector3D X_H0 = Vector3D.PLUS_I;

        // Same vectors in the GCRF frame:
        Vector3D Z_GCRF = null;
        Vector3D X_GCRF = null;

        final Frame inertialFrame = FramesFactory.getGCRF();

        // Terrestrial to inertial GCRF frame transformation:
        final Transform tTerToInertial = FramesFactory.getITRF().getTransformTo(
            inertialFrame, date);

        // The Z-axis of the H0 frame is the Z-axis of the terrestrial frame at H0 date:
        Z_GCRF = tTerToInertial.transformPosition(Z_H0);

        // H0 --> terrestrial frame transformation, rotation around Z:
        final Rotation rotH0ToTerrestreForPosition = new Rotation(Vector3D.PLUS_K, longitude);
        final Vector3D X_Terrestre = rotH0ToTerrestreForPosition.applyTo(X_H0);
        X_GCRF = tTerToInertial.transformPosition(X_Terrestre);

        final Rotation inertialToH0 = new Rotation(Z_H0, X_H0, Z_GCRF, X_GCRF);
        final Transform inertialToH0Transformation = new Transform(date, inertialToH0);

        final Frame h0Frame = new Frame(inertialFrame, inertialToH0Transformation, "CNES_H0minusN", true);
        return h0Frame;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

}
