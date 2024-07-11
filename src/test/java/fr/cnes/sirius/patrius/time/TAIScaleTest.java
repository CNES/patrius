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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.Constants;

public class TAIScaleTest {

    @Test
    public void testZero() {
        final TimeScale scale = TimeScalesFactory.getTAI();
        Assert.assertEquals("TAI", scale.toString());
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assert.assertEquals(0, scale.offsetFromTAI(date), 0);
            final DateTimeComponents components = date.getComponents(scale);
            Assert.assertEquals(0, scale.offsetToTAI(components.getDate(), components.getTime()), 0);
        }
    }

}
