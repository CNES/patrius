/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GPSScaleTest {

    @Test
    public void testT0() {
        final TimeScale scale = TimeScalesFactory.getGPS();
        Assert.assertEquals("GPS", scale.toString());
        final AbsoluteDate t0 =
            new AbsoluteDate(new DateComponents(1980, 1, 6), TimeComponents.H00, scale);
        Assert.assertEquals(AbsoluteDate.GPS_EPOCH, t0);
    }

    @Test
    public void testArbitrary() throws PatriusException {
        final AbsoluteDate tGPS =
            new AbsoluteDate(new DateComponents(1999, 3, 4), TimeComponents.H00, TimeScalesFactory.getGPS());
        final AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(1999, 3, 3), new TimeComponents(23, 59, 47),
                TimeScalesFactory.getUTC());
        Assert.assertEquals(tUTC, tGPS);
    }

    @Test
    public void testConstant() {
        final TimeScale scale = TimeScalesFactory.getGPS();
        final double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH);
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assert.assertEquals(reference, scale.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    public void testSymmetry() {
        final TimeScale scale = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            final double dt1 = scale.offsetFromTAI(date);
            final DateTimeComponents components = date.getComponents(scale);
            final double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals(0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void secondCallTest() throws PatriusException {
        final TimeScale scale = TimeScalesFactory.getGPS();
        final TimeScale scale1 = TimeScalesFactory.getGPS();
        final AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        Assert.assertEquals(scale.offsetFromTAI(t1), scale1.offsetFromTAI(t1), 1.0e-15);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
