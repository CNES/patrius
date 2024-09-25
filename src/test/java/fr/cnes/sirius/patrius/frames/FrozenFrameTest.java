/**
 * Copyright 2022-2022 CNES
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
 * HISTORY
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] L'attitude des spacecraft state devrait etre initialisee de maniere lazy
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for class {@link CelestialBodyFrame}.
 */
public class FrozenFrameTest {

    /**
     * Test constructors.
     */
    @Test
    public void testConstructor() throws PatriusException {
        final Frame coordinateFrame = FramesFactory.getICRF();
        final Frame referenceFrame = FramesFactory.getEME2000();
        final String frozenFrameName = "Frozen Frame";

        final FrozenFrame frozenFrame = new FrozenFrame(coordinateFrame, referenceFrame, frozenFrameName);
        Assert.assertEquals(referenceFrame, frozenFrame.getParent());
    }

    /**
     * Test getter for transforms.
     * @throws PatriusException
     */
    @Test
    public void testGetTransform() throws PatriusException {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame coordinateFrame = FramesFactory.getICRF();
        final Frame referenceFrame = FramesFactory.getEME2000();
        final String frozenFrameName = "Frozen Frame";

        final FrozenFrame frozenFrame = new FrozenFrame(coordinateFrame, referenceFrame, frozenFrameName);

        // Reference values for non-regression test
        final Quaternion quaternionRef = new Rotation(true, 0.9999999999999984, -1.653020584550675E-8,
                4.028108631990782E-8, 3.539139805514139E-8).getQuaternion();
        final Vector3D translationRef = new Vector3D(27566638023.97404, -132361428631.15027, -57418643983.93181);
        // Check the rotation via non-regression
        Assert.assertEquals(quaternionRef, frozenFrame.getTransformProvider().getTransform(date)
                .getRotation().getQuaternion());
        Assert.assertEquals(quaternionRef, frozenFrame.getTransformProvider().getTransform(date, false).getRotation()
                .getQuaternion());
        Assert.assertEquals(quaternionRef,
                frozenFrame.getTransformProvider().getTransform(date, FramesFactory.getConfiguration()).getRotation()
                        .getQuaternion());
        // Check the translation via non-regression
        Assert.assertEquals(translationRef, frozenFrame
                .getTransformProvider().getTransform(date).getTranslation());
        // Check that the velocity and the rotation rate are frozen
        Assert.assertEquals(Vector3D.ZERO, frozenFrame.getTransformProvider().getTransform(date).getRotationRate());
        Assert.assertEquals(Vector3D.ZERO, frozenFrame.getTransformProvider().getTransform(date).getVelocity());
        // Check that the acceleration and the rotation acceleration are null
        Assert.assertNull(frozenFrame.getTransformProvider().getTransform(date).getRotationAcceleration());
        Assert.assertNull(frozenFrame.getTransformProvider().getTransform(date).getAcceleration());


    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
