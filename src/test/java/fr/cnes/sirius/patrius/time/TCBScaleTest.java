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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.Constants;

public class TCBScaleTest {

    @Test
    public void testReference() {
        final TimeScale tcb = TimeScalesFactory.getTCB();
        final TimeScale tdb = TimeScalesFactory.getTDB();
        Assert.assertEquals("TCB", tcb.toString());
        final AbsoluteDate refTCB = new AbsoluteDate("1977-01-01T00:00:32.184", tcb);
        final AbsoluteDate refTDB = new AbsoluteDate("1977-01-01T00:00:32.184", tdb);
        Assert.assertEquals(0.0, refTCB.durationFrom(refTDB), 1.0e-12);
    }

    @Test
    public void testRate() {
        final TimeScale tcb = TimeScalesFactory.getTCB();
        final TimeScale tdb = TimeScalesFactory.getTDB();
        final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;
        for (double deltaT = 1.0; deltaT < 10.0; deltaT += 0.3) {
            final AbsoluteDate t1 = t0.shiftedBy(deltaT);
            final double tdbRate = t1.offsetFrom(t0, tdb) / deltaT;
            final double tcbRate = t1.offsetFrom(t0, tcb) / deltaT;
            Assert.assertEquals(tdbRate + 1.550505e-8, tcbRate, 1.0e-14);
        }
    }

    @Test
    public void testSymmetry() {
        final TimeScale scale = TimeScalesFactory.getTCB();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            final double dt1 = scale.offsetFromTAI(date);
            final DateTimeComponents components = date.getComponents(scale);
            final double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals(0.0, dt1 + dt2, 1.0e-10);
        }
    }

}
