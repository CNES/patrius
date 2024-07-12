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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:265:19/09/2014:bug in ClasspathCrawler with regard to data file path names
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.data;

import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test the {@link ClasspathCrawler} class is able to stock and then provide data
 * even when the data file is not at the root of the classpath (Orekit bug).
 * 
 * @author sabatinit
 * 
 * @version $Id: BugClasspathCrawlerTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 * 
 */
public class BugClasspathCrawlerTest {

    /**
     * @testType UT
     * 
     * @testedMethod {@link ClasspathCrawler#feed(Pattern, DataLoader)}
     * 
     * @description tests the ephemeris and UTC-TAI data loading when a ClasspathCrawler data provider is
     *              involved, and the data files are not at the root of the class path.
     * 
     * @input UTC-TAI and ephemeris data files
     * 
     * @output TimeScalesFactory and CelestialBodyFactory output
     * 
     * @testPassCriteria TimeScalesFactory and CelestialBodyFactory methods should be called without errors
     *                   raising
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void test() {

        final String[] list = { "classpathCrawler-bug-test-data/time-scales/UTC-TAI.history",
            "classpathCrawler-bug-test-data/ephemeris/de406-ephemeris/unxp1962.406" };

        DataProvidersManager.getInstance().clearProviders();

        DataProvider provider = null;

        try {
            provider = new ClasspathCrawler(list);
        } catch (final PatriusException e) {
            Assert.fail("The ClasspathCrawler cannot be created");
        }
        DataProvidersManager.getInstance().addProvider(provider);

        // Get the UTC date from UTC-TAI data:
        final AbsoluteDate date = new AbsoluteDate();
        boolean testOk = false;
        try {
            TimeScalesFactory.getUTC();
            testOk = true;
        } catch (final PatriusException e) {
            Assert.fail("The UTC-TAI have not been loaded");
        }
        Assert.assertTrue(testOk);

        // Get the ephemeris date from ephemeris data:
        testOk = false;
        try {
            CelestialBodyFactory.getSun().getPVCoordinates(date, FramesFactory.getGCRF());
            testOk = true;
        } catch (final PatriusException e) {
            Assert.fail("The ephemeris have not been loaded");
        }
        Assert.assertTrue(testOk);
    }
}
