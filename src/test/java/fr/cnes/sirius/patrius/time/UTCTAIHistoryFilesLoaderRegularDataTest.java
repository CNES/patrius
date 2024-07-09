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
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class UTCTAIHistoryFilesLoaderRegularDataTest {

    @Test
    public void testRegular() throws PatriusException {
        Assert.assertEquals(-32.0, TimeScalesFactory.getUTC().offsetFromTAI(AbsoluteDate.J2000_EPOCH), 10e-8);
    }

    @Test
    public void testFirstLeap() throws PatriusException {
        final UTCScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate afterLeap = new AbsoluteDate(1961, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.4228180,
            afterLeap.durationFrom(utc.getFirstKnownLeapSecond()),
            1.0e-12);
    }

    @Test
    public void testLaststLeap() throws PatriusException {
        final UTCScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate afterLeap = new AbsoluteDate(2012, 7, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.0,
            afterLeap.durationFrom(utc.getLastKnownLeapSecond()),
            1.0e-12);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
