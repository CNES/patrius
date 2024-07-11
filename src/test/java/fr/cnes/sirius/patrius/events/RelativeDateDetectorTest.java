/**
 * 
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
 * @history created 28/05/18
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.RelativeDateDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;

public class RelativeDateDetectorTest {

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {

        final RelativeDateDetector dateDetector = new RelativeDateDetector(86400,
            AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTAI());
        final RelativeDateDetector detector = (RelativeDateDetector) dateDetector.copy();
        Assert.assertEquals(86400, detector.getRelativeDate(), 0);
        Assert.assertEquals(AbsoluteDate.J2000_EPOCH,
            detector.getReferenceDate());
        Assert.assertEquals(TimeScalesFactory.getTAI().getName(), detector
            .getTimeScale().getName());
        final RelativeDateDetector dateDetector2 = new RelativeDateDetector(85000,
            AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTAI(), 1e-10,
            1e-5, Action.CONTINUE);
        Assert.assertEquals(85000, dateDetector2.getRelativeDate(), 0);
        Assert.assertEquals(1e-10, dateDetector2.getMaxCheckInterval(), 0);
    }
}
