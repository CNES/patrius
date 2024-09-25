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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class UT1ScaleTest {

    @Test
    public void testLeap2006() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        this.ut1 = TimeScalesFactory.getUT1();
        TimeScalesFactory.addDefaultUTCTAILoader();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate dateA = new AbsoluteDate(2005, 12, 30, 23, 59, 0.0, utc);
        final AbsoluteDate dateB = new AbsoluteDate(2006, 1, 2, 0, 1, 0.0, utc);
        final double deltaAUT1 = this.ut1.offsetFromTAI(dateA);
        final double deltaAUTC = utc.offsetFromTAI(dateA);
        final double deltaBUT1 = this.ut1.offsetFromTAI(dateB);
        final double deltaBUTC = utc.offsetFromTAI(dateB);

        // there is a leap second between the two dates
        Assert.assertEquals(deltaAUTC - 1.0, deltaBUTC, 1.0e-15);

        // the leap second induces UTC goes from above UT1 to below UT1
        Assert.assertTrue(deltaAUTC > deltaAUT1);
        Assert.assertTrue(deltaBUTC < deltaBUT1);

        // UT1 is continuous, so change should be very small in two days
        Assert.assertEquals(deltaAUT1, deltaBUT1, 3.0e-4);
        Assert.assertNotNull(this.ut1.getHistory());
    }

    @Test
    public void testSymmetry() {
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            final double dt1 = this.ut1.offsetFromTAI(date);
            final DateTimeComponents components = date.getComponents(this.ut1);
            final double dt2 = this.ut1.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals(0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testString() {
        Assert.assertEquals(this.ut1.toString(), "UT1");
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        this.ut1 = TimeScalesFactory.getUT1();
    }

    @After
    public void tearDown() throws PatriusException {
        this.ut1 = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    private UT1Scale ut1;

}
