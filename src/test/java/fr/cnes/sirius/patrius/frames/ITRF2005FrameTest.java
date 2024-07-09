/**
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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ITRF2005FrameTest {

    @Test
    public void testTidalEffects() throws PatriusException {

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();// TODO : FramesFactory.getITRF2005(false)
        final AbsoluteDate date0 = new AbsoluteDate(2007, 10, 20, TimeScalesFactory.getUTC());

        double minCorrection = Double.POSITIVE_INFINITY;
        double maxCorrection = Double.NEGATIVE_INFINITY;

        // Build and save the two alternating configurations
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(false));
        final FramesConfiguration iers_false = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final FramesConfiguration iers_true = FramesFactory.getConfiguration();

        for (double dt = 0; dt < 3 * Constants.JULIAN_DAY; dt += 60) {
            final AbsoluteDate date = date0.shiftedBy(dt);

            FramesFactory.setConfiguration(iers_false);

            final Transform t_with = itrf.getTransformTo(gcrf, date);

            FramesFactory.setConfiguration(iers_true);

            final Transform t_without = gcrf.getTransformTo(itrf, date);

            final Transform t = new Transform(date, t_with, t_without);

            Assert.assertEquals(0, t.getTranslation().getNorm(), 1.0e-15);
            final double milliarcSeconds = MathLib.toDegrees(t.getRotation().getAngle()) * 3600000.0;
            minCorrection = MathLib.min(minCorrection, milliarcSeconds);
            maxCorrection = MathLib.max(maxCorrection, milliarcSeconds);
        }

        Assert.assertEquals(0.613, maxCorrection, 0.001);

    }

    @Test
    public void testShift() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(false));

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();// TODO FramesFactory.getITRF2005(false);
        final AbsoluteDate date0 = new AbsoluteDate(2007, 10, 20, TimeScalesFactory.getUTC());

        for (double t = 0; t < Constants.JULIAN_DAY; t += 3600) {
            final AbsoluteDate date = date0.shiftedBy(t);
            final Transform transform = gcrf.getTransformTo(itrf, date);
            for (double dt = -10; dt < 10; dt += 0.125) {
                final Transform shifted = transform.shiftedBy(dt);
                final Transform computed = gcrf.getTransformTo(itrf, transform.getDate().shiftedBy(dt));
                final Transform error = new Transform(computed.getDate(), computed, shifted.getInverse());
                Assert.assertEquals(0.0, error.getTranslation().getNorm(), 1.0e-10);
                Assert.assertEquals(0.0, error.getVelocity().getNorm(), 1.0e-10);
                Assert.assertEquals(0.0, error.getRotation().getAngle(), 1.0e-10);
                Assert.assertEquals(0.0, error.getRotationRate().getNorm(), 5.0e-15);
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
