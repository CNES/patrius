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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-4:08/12/2023:[PATRIUS] Lien entre un repere predefini et un CelestialBody
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.frames.transformations.FixedTransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for class {@link CelestialBodyFrame}.
 */
public class CelestialBodyFrameTest {

    /**
     * Test constructors.
     */
    @Test
    public void testConstructors() throws PatriusException {
        // Build Celestial body Frame
        final CelestialPoint earth = CelestialBodyFactory.getEarth();
        // Constructor 1
        final CelestialBodyFrame frame1 = new CelestialBodyFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame1", earth);
        Assert.assertEquals(FramesFactory.getGCRF(), frame1.getParent());
        Assert.assertTrue(frame1.getTransformProvider().getTransform(AbsoluteDate.J2000_EPOCH).getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertEquals("Frame1", frame1.getName());
        Assert.assertEquals(false, frame1.isPseudoInertial());
        Assert.assertEquals(earth, frame1.getCelestialPoint());

        // Constructor 2
        final CelestialBodyFrame frame2 = new CelestialBodyFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame2", true, earth);
        Assert.assertEquals(FramesFactory.getGCRF(), frame2.getParent());
        Assert.assertTrue(frame2.getTransformProvider().getTransform(AbsoluteDate.J2000_EPOCH).getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertEquals("Frame2", frame2.getName());
        Assert.assertEquals(true, frame2.isPseudoInertial());
        Assert.assertEquals(earth, frame2.getCelestialPoint());

        // Constructor 3
        final CelestialBodyFrame frame3 = new CelestialBodyFrame(FramesFactory.getGCRF(), new FixedTransformProvider(Transform.IDENTITY), "Frame3", earth);
        Assert.assertEquals(FramesFactory.getGCRF(), frame3.getParent());
        Assert.assertTrue(frame3.getTransformProvider().getTransform(AbsoluteDate.J2000_EPOCH).getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertEquals("Frame3", frame3.getName());
        Assert.assertEquals(false, frame3.isPseudoInertial());
        Assert.assertEquals(earth, frame3.getCelestialPoint());

        // Constructor 4
        final CelestialBodyFrame frame4 = new CelestialBodyFrame(FramesFactory.getGCRF(), new FixedTransformProvider(Transform.IDENTITY), "Frame4", true, earth);
        Assert.assertEquals(FramesFactory.getGCRF(), frame4.getParent());
        Assert.assertTrue(frame4.getTransformProvider().getTransform(AbsoluteDate.J2000_EPOCH).getAngular().getRotation().isEqualTo(Rotation.IDENTITY));
        Assert.assertEquals("Frame4", frame4.getName());
        Assert.assertEquals(true, frame4.isPseudoInertial());
        Assert.assertEquals(earth, frame4.getCelestialPoint());
    }

    /**
     * Test getter.
     */
    @Test
    public void testGetter() throws PatriusException {
        // Build Celestial body Frame
        final CelestialPoint earth = CelestialBodyFactory.getEarth();
        final CelestialBodyFrame frame = new CelestialBodyFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame", earth);
        // Checks get
        Assert.assertEquals(frame.getCelestialPoint(), earth);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
