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
 * @history
 * version 1.0 - Sylvain VRESK - code creation
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.utils;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * @description Patrius base library localized Java exception class test.
 * 
 * @author Sylvain VRESK
 * 
 * @version $Id: PatriusMessagesTest.java 18021 2017-09-29 15:56:26Z bignon $
 * 
 * @since 1.0
 * 
 */

public class PatriusMessagesTest {

    /** Features description */
    public enum features {
        /**
         * @featureTitle Patrius messages translation behavior.
         * 
         * @featureDescription Internationalization availability.
         * 
         * @coveredRequirements NA
         */
        INTERNATIONALIZATION;
    }

    /**
     * @testType UT
     * 
     * @description Checks that all described keys in PatriusMessages class are present in each foreign language
     *              translation files (PatriusMessages_xx.properties).
     * 
     * @testedFeature {@link features#INTERNATIONALIZATION}
     * 
     * @testedMethod none
     * 
     * @input Enum PatriusMessages = - : Patrius base formatted messages
     * @input File PatriusMessages_fr.properties = - : Patrius French translated messages
     * @input File PatriusMessages_en.properties = - : Patrius English translated messages
     * 
     * @output none
     * 
     * @testPassCriteria No exception is thrown
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     */
    @Test
    public void testAllKeysPresentInPropertiesFiles() {
        for (final String language : new String[] { "en", "fr" }) {
            final ResourceBundle bundle = ResourceBundle.getBundle("META-INF/localization/PatriusMessages", new Locale(
                language));
            for (final PatriusMessages message : PatriusMessages.values()) {
                final String messageKey = message.toString();
                boolean keyPresent = false;
                for (final Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
                    keyPresent |= messageKey.equals(keys.nextElement());
                }
                Assert.assertTrue("missing key \"" + message.name() + "\" for language " + language, keyPresent);
            }
            int nbKeys = 0;
            for (final Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements(); keys.nextElement()) {
                ++nbKeys;
            }
            Assert.assertEquals(PatriusMessages.values().length, nbKeys);
            Assert.assertEquals(language, bundle.getLocale().getLanguage());
        }
    }

    /**
     * @testType UT
     * 
     * @description Checks that all messages have a translation in French translation messages file
     *              (PatriusMessages_fr.properties).
     * 
     * @testedFeature {@link features#INTERNATIONALIZATION}
     * 
     * @testedMethod {@link PatriusMessages#getLocalizedString}
     * 
     * @input Enum PatriusMessages = - : Patrius base formatted messages
     * @input File PatriusMessages_fr.properties = - : Patrius French translated messages
     * 
     * @output none
     * 
     * @testPassCriteria No exception is thrown
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     */
    @Test
    public void testNoMissingFrenchTranslation() {
        for (final PatriusMessages message : PatriusMessages.values()) {
            final String translated = message.getLocalizedString(Locale.FRENCH);
            Assert.assertFalse(message.name(), translated.toLowerCase().contains("missing translation"));
        }
    }

    /**
     * @testType UT
     * 
     * @description Checks that all messages have a translation in English translation messages file
     *              (PatriusMessages_en.properties).
     * 
     * @testedFeature {@link features#INTERNATIONALIZATION}
     * 
     * @testedMethod {@link PatriusMessages#getLocalizedString}
     * 
     * @input Enum PatriusMessages = - : Patrius base formatted messages
     * @input File PatriusMessages_en.properties = - : Patrius English translated messages
     * 
     * @output none
     * 
     * @testPassCriteria No exception is thrown
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     */
    @Test
    public void testNoMissingEnglishTranslation() {
        for (final PatriusMessages message : PatriusMessages.values()) {
            final String translated = message.getLocalizedString(Locale.ENGLISH);
            Assert.assertFalse(message.name(), translated.toLowerCase().contains("missing translation"));
        }
    }

    /**
     * @testType UT
     * 
     * @description Checks that unknown language make messages fall back to using the default format
     *              (PatriusMessages_kl.properties is missing).
     * 
     * @testedFeature {@link features#INTERNATIONALIZATION}
     * 
     * @testedMethod {@link PatriusMessages#getLocalizedString}
     * 
     * @input Enum PatriusMessages = - : Patrius base formatted messages
     * @input File PatriusMessages_kl.properties = - : Patrius Klingon translated messages
     * 
     * @output none
     * 
     * @testPassCriteria No exception arise
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     */
    @Test
    public void testMissingKlingonTranslation() {
        for (final PatriusMessages message : PatriusMessages.values()) {
            final String translated = message.getLocalizedString(new Locale("kl", "KL"));
            // if a localized messages file SiriusMessages_kl.properties is missing so messages
            // fall back to using the source format and no exception is sent to the user
            Assert.assertFalse(message.name(), translated.toLowerCase().contains("missing translation"));
        }
    }

    /**
     * @testType UT
     * 
     * @description Checks that all messages text are not empty.
     * 
     * @testedFeature {@link features#INTERNATIONALIZATION}
     * 
     * @testedMethod {@link PatriusMessages#getSourceString}
     * 
     * @input Enum PatriusMessages = - : Patrius base formatted messages
     * 
     * @output none
     * 
     * @testPassCriteria No exception is thrown
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testGetSourceString() {
        for (final PatriusMessages message : PatriusMessages.values()) {
            final String actual = message.getSourceString();
            Assert.assertFalse(actual.length() <= 0);
        }
    }

}
