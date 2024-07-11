/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:661:01/02/2017:add H0MinusNFrame class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class H0MinusNFrameTest {

    @Test
    public void testGetters() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI());
        final H0MinusNFrame frame = new H0MinusNFrame("MyFrame", date, 12.3, 14.5);
        Assert.assertEquals("MyFrame", frame.getName());
        Assert.assertEquals(0., date.durationFrom(frame.getH0()), 0.);
        Assert.assertEquals(12.3, frame.getN(), 0.);
        Assert.assertEquals(14.5, frame.getLongitude(), 0.);
    }

    @Test
    public void testFrameConversion() throws PatriusException {
        // Build in different ways same instance of H0 - n frame
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI());
        final H0MinusNFrame frame1 = new H0MinusNFrame("MyFrame", date, 12.3, 14.5);
        final Frame frame2 = FramesFactory.getH0MinusN("", date.shiftedBy(-12.3), 14.5);
        final Frame frame3 = FramesFactory.getH0MinusN("", date, 12.3, 14.5);

        // Compute transformation from these frames to GCRF
        final Transform t1 = frame1.getTransformTo(FramesFactory.getGCRF(), date.shiftedBy(86400));
        final Transform t2 = frame2.getTransformTo(FramesFactory.getGCRF(), date.shiftedBy(86400));
        final Transform t3 = frame3.getTransformTo(FramesFactory.getGCRF(), date.shiftedBy(86400));

        // Check
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ0(), t2.getRotation().getQuaternion().getQ0(), 0.);
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ1(), t2.getRotation().getQuaternion().getQ1(), 0.);
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ2(), t2.getRotation().getQuaternion().getQ2(), 0.);
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ3(), t2.getRotation().getQuaternion().getQ3(), 0.);
        Assert.assertEquals(0., t1.getRotationRate().subtract(t2.getRotationRate()).getNorm(), 0.);
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ0(), t3.getRotation().getQuaternion().getQ0(), 0.);
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ1(), t3.getRotation().getQuaternion().getQ1(), 0.);
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ2(), t3.getRotation().getQuaternion().getQ2(), 0.);
        Assert.assertEquals(t1.getRotation().getQuaternion().getQ3(), t3.getRotation().getQuaternion().getQ3(), 0.);
        Assert.assertEquals(0., t1.getRotationRate().subtract(t3.getRotationRate()).getNorm(), 0.);

        // Verify frame2 and frame3 are instances of H0MinusNFrame
        Assert.assertTrue(frame2 instanceof H0MinusNFrame && frame3 instanceof H0MinusNFrame);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
