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
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link SolarActivityDataFactory}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: SolarActivityDataFactoryTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class SolarActivityDataFactoryTest {

    /**
     * Features description.
     */
    public enum features {
        /**
         * @featureTitle SolarActivityDataFactory tests
         * 
         * @featureDescription make sure mechanisms are valid
         * 
         * @coveredRequirements DV-MOD_261
         */
        SOLAR_ACTIVITY_FACTORY_TESTS
    }

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_FACTORY_TESTS}
     * 
     * @testedMethod {@link SolarActivityDataFactory#getSolarActivityDataProvider()}.
     * 
     * @description make sure the mechanisms of loading readers is working
     * 
     * @input none
     * 
     * @output provider
     * 
     * @testPassCriteria factory behaves as expected
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testDefaultReaders() throws PatriusException, InstantiationException, IllegalAccessException,
                                    IllegalArgumentException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("atmosphere");
        SolarActivityDataFactory.getSolarActivityDataProvider();

        try {
            CNESUtils.clearNewFactoriesAndCallSetDataRoot("bad-end-data");
            SolarActivityDataFactory.getSolarActivityDataProvider();
            Assert.fail();
        } catch (final PatriusException e) {
            // expected!
        }
    }

    /**
     * @throws PatriusException
     *         if no solar activity at date
     * @testType UT
     * 
     * @testedFeature {@link features#SOLAR_ACTIVITY_FACTORY_TESTS}
     * 
     * @testedMethod {@link SolarActivityDataFactory#addSolarActivityDataReader(SolarActivityDataReader)}.
     * 
     * @description make sure the mechanisms of loading readers is working
     * 
     * @input none
     * 
     * @output provider
     * 
     * @testPassCriteria factory behaves as expected
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testSpecificReader() throws PatriusException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("atmosphere");
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader(
            SolarActivityDataFactory.ACSOL_FILENAME));
        SolarActivityDataFactory.getSolarActivityDataProvider();

    }

}
