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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TCGScaleTest {

    @Test
    public void testRatio() {
        final TimeScale scale = TimeScalesFactory.getTCG();
        Assert.assertEquals("TCG", scale.toString());
        final double dtTT = 1e6;
        final AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate t2 = t1.shiftedBy(dtTT);
        final double dtTCG = dtTT + scale.offsetFromTAI(t2) - scale.offsetFromTAI(t1);
        Assert.assertEquals(1 - 6.969290134e-10, dtTT / dtTCG, 1.0e-15);
    }

    @Test
    public void testSymmetry() {
        final TimeScale scale = TimeScalesFactory.getTCG();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            final double dt1 = scale.offsetFromTAI(date);
            final DateTimeComponents components = date.getComponents(scale);
            final double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals(0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testReference() throws PatriusException {
        final DateComponents referenceDate = new DateComponents(1977, 01, 01);
        final TimeComponents thirtyTwo = new TimeComponents(0, 0, 32.184);
        final AbsoluteDate ttRef = new AbsoluteDate(referenceDate, thirtyTwo, TimeScalesFactory.getTT());
        final AbsoluteDate tcgRef = new AbsoluteDate(referenceDate, thirtyTwo, TimeScalesFactory.getTCG());
        final AbsoluteDate taiRef = new AbsoluteDate(referenceDate, TimeComponents.H00, TimeScalesFactory.getTAI());
        final AbsoluteDate utcRef = new AbsoluteDate(new DateComponents(1976, 12, 31),
            new TimeComponents(23, 59, 45),
            TimeScalesFactory.getUTC());
        Assert.assertEquals(0, ttRef.durationFrom(tcgRef), 1.0e-15);
        Assert.assertEquals(0, ttRef.durationFrom(taiRef), 1.0e-15);
        Assert.assertEquals(0, ttRef.durationFrom(utcRef), 1.0e-15);
    }

    @Test
    public void secondCallTest() {
        final TimeScale scale = TimeScalesFactory.getTCG();
        final TimeScale scale1 = TimeScalesFactory.getTCG();
        final AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        Assert.assertEquals(scale.offsetFromTAI(t1), scale1.offsetFromTAI(t1), 1.0e-15);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
