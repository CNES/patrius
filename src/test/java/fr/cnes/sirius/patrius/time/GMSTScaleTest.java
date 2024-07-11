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

public class GMSTScaleTest {

    @Test
    // reference: http://www.astro.umd.edu/~jph/GST_eqn.pdf
            public
            void testReference() throws PatriusException {
        Assert.assertEquals("GMST", this.gmst.toString());
        final AbsoluteDate date = new AbsoluteDate(2001, 10, 3, 6, 30, 0.0, TimeScalesFactory.getUT1());
        final DateTimeComponents gmstComponents = date.getComponents(this.gmst);
        Assert.assertEquals(2001, gmstComponents.getDate().getYear());
        Assert.assertEquals(10, gmstComponents.getDate().getMonth());
        Assert.assertEquals(3, gmstComponents.getDate().getDay());
        Assert.assertEquals(7, gmstComponents.getTime().getHour());
        Assert.assertEquals(18, gmstComponents.getTime().getMinute());
        Assert.assertEquals(8.329, gmstComponents.getTime().getSecond(), 4.0e-4);
    }

    @Test
    public void testSymmetry() {
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            final double dt1 = this.gmst.offsetFromTAI(date);
            final DateTimeComponents components = date.getComponents(this.gmst);
            final double dt2 = this.gmst.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals(0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void secondCallTest() throws PatriusException {
        final TimeScale gmst1 = TimeScalesFactory.getGMST();
        final AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        Assert.assertEquals(gmst1.offsetFromTAI(t1), this.gmst.offsetFromTAI(t1), 1.0e-15);
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        this.gmst = TimeScalesFactory.getGMST();
    }

    @After
    public void tearDown() {
        this.gmst = null;
    }

    private TimeScale gmst;

}
