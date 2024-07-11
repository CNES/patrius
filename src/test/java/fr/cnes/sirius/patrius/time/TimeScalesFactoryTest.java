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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2649:18/05/2021: ajout d un getter parametrable TimeScalesFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link TimeScalesFactory} class.
 */
public class TimeScalesFactoryTest {

    /**
     * Check the get method for all time scales.
     */
    @Test
    public void testGet() throws PatriusException {
        // Data initialization
        Utils.setDataRoot("regular-dataPBASE");
        final FramesConfiguration svgConfig = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getSimpleConfiguration(true));
        
        // Checks
        Assert.assertEquals(TimeScalesFactory.get("TAI"), TimeScalesFactory.getTAI());
        Assert.assertEquals(TimeScalesFactory.get("UTC"), TimeScalesFactory.getUTC());
        Assert.assertEquals(TimeScalesFactory.get("UT1"), TimeScalesFactory.getUT1());
        Assert.assertEquals(TimeScalesFactory.get("TT"), TimeScalesFactory.getTT());
        Assert.assertEquals(TimeScalesFactory.get("GST"), TimeScalesFactory.getGST());
        Assert.assertEquals(TimeScalesFactory.get("GPS"), TimeScalesFactory.getGPS());
        Assert.assertEquals(TimeScalesFactory.get("TCG"), TimeScalesFactory.getTCG());
        Assert.assertEquals(TimeScalesFactory.get("TDB"), TimeScalesFactory.getTDB());
        Assert.assertEquals(TimeScalesFactory.get("TCB"), TimeScalesFactory.getTCB());
        Assert.assertEquals(TimeScalesFactory.get("GMST"), TimeScalesFactory.getGMST());
        
        try {
            TimeScalesFactory.get("WrongTimeScale");
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        
        // Set back original frame configuration
        FramesFactory.setConfiguration(svgConfig);
    }
}
