/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2608:27/01/2021:Mauvaise date de reference pour le Galileo System Time
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GalileoScaleTest {

    @Test
    public void testT0() {
        final TimeScale scale = TimeScalesFactory.getGST();
        Assert.assertEquals("GST", scale.toString());
        final AbsoluteDate t0 =
            new AbsoluteDate(new DateComponents(1999, 8, 22), new TimeComponents(0, 0, 0), scale);
        Assert.assertEquals(AbsoluteDate.GALILEO_EPOCH, t0);
    }

    @Test
    public void test2006() throws PatriusException {
        final AbsoluteDate tGalileo =
            new AbsoluteDate(new DateComponents(2006, 1, 2), TimeComponents.H00, TimeScalesFactory.getGST());
        final AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(2006, 1, 1), new TimeComponents(23, 59, 46),
                TimeScalesFactory.getUTC());
        Assert.assertEquals(tUTC, tGalileo);
    }

    @Test
    public void testConstant() {
        final TimeScale scale = TimeScalesFactory.getGST();
        final double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH);
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assert.assertEquals(reference, scale.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    public void testSameAsGPS() {
        final TimeScale gst = TimeScalesFactory.getGST();
        final TimeScale gps = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assert.assertEquals(gps.offsetFromTAI(date), gst.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    public void testSymmetry() {
        final TimeScale scale = TimeScalesFactory.getGST();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            final double dt1 = scale.offsetFromTAI(date);
            final DateTimeComponents components = date.getComponents(scale);
            final double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals(0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void secondCallTest() {
        final TimeScale scale = TimeScalesFactory.getGST();
        final TimeScale scale1 = TimeScalesFactory.getGST();
        final AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        Assert.assertEquals(scale.offsetFromTAI(t1), scale1.offsetFromTAI(t1), 1.0e-15);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
