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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2682:18/05/2021: Echelle de temps TDB (diff. PATRIUS - SPICE) 
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Test;

public class TDBScaleTest {

    @Test
    public void testReference() {
        final TimeScale scale = TimeScalesFactory.getTDB();
        Assert.assertEquals("TDB", scale.toString());
        Assert.assertEquals(32.183927340791372839, scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH), 1.0e-15);
    }

    @Test
    public void testDate5000000() {
        final TimeScale scale = TimeScalesFactory.getTDB();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5000000);
        Assert.assertEquals(32.185364155950634549, scale.offsetFromTAI(date), 1.0e-13);
    }

    @Test
    public void testToTAI5000000() {
        final TimeScale scale = TimeScalesFactory.getTDB();
        final AbsoluteDate date = new AbsoluteDate(2000, 2, 28, 8, 53, 20.001364155950634549, scale);
        final double dt = AbsoluteDate.J2000_EPOCH.shiftedBy(5000000).durationFrom(date);
        Assert.assertEquals(0.0, dt, 1.0e-13);
    }

    @Test
    public void testToTAI() {
        final TimeScale scale = TimeScalesFactory.getTDB();
        final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 11, 59, 59.999927340791372839, scale);
        final double dt = AbsoluteDate.J2000_EPOCH.durationFrom(date);
        Assert.assertEquals(0.0, dt, 1.0e-13);
    }

    /**
     * Test user-defined model.
     */
    @Test
    public void testUserModel() {
        // Set user-defined model with fixed offset
        TDBScale.setModel(new TDBModel() {
            
            @Override
            public double offsetFromTAI(final AbsoluteDate date) {
                return 10;
            }
        });
        final TimeScale scale = TimeScalesFactory.getTDB();
        
        // Checks
        Assert.assertEquals(10, scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH), 1.0e-15);
        Assert.assertEquals(-10, scale.offsetToTAI(AbsoluteDate.J2000_EPOCH.getComponents(scale).getDate(), AbsoluteDate.J2000_EPOCH.getComponents(scale).getTime()), 1.0e-15);
        
        // Set back default model
        TDBScale.setModel(new TDBDefaultModel());
    }

}
