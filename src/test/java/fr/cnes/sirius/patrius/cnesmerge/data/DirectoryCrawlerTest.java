/**
 * 
 * Copyright 2011-2022 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.data;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.junit.Test;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DirectoryCrawler;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * -this class adds tests in order to raise the cover rate of DirectoryCrawler
 * </p>
 * IMPORTANT NOTE : this class uses regular-dataCNES,
 * that shall be safely replaced by regular-data
 * when this test is merged inside Orekit
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: DirectoryCrawlerTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class DirectoryCrawlerTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Exception throwing tests
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the class by raising the
         *                     exceptions
         * 
         * @coveredRequirements NA
         */
        EXCEPTIONTHROWING
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EXCEPTIONTHROWING}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader, java.io.File)}
     * 
     * @description The purpose of this test is to check if the provider throws an OrekitException when the loader
     *              raises an OrekitException. We use a loader that always throws this exception when fed.
     * 
     * @testPassCriteria the exception OrekitException is raised
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     * 
     */
    @Test(expected = PatriusException.class)
    public final void testParseException() throws PatriusException, URISyntaxException {
        final URL url = DirectoryCrawlerTest.class.getClassLoader().getResource("regular-dataCNES-2003");
        new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new PatriusExceptionLoader());
    }

    // Mock for a loader that throws a PatriusException
    private static class PatriusExceptionLoader implements DataLoader {
        @Override
        public final boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name) throws ParseException, PatriusException {
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER);
        }
    }
}
