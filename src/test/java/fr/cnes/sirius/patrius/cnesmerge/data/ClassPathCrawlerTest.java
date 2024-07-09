/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
package fr.cnes.sirius.patrius.cnesmerge.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.data.ClasspathCrawler;
import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * -this class adds tests in order to raise the cover rate of ClassPathCrawler
 * </p>
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: ClassPathCrawlerTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class ClassPathCrawlerTest {

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
        EXCEPTIONTHROWING,

        /**
         * @featureTitle Increase the code coverage
         * 
         * @featureDescription Increase the code coverage
         * 
         * @coveredRequirements NA
         */
        COVERAGE
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EXCEPTIONTHROWING}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.ClasspathCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description the purpose of this test is to cover the throwing of the exception OrekitException
     * 
     * @testPassCriteria the expected output is an OrekitException raised by the special DataLoader
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test(expected = PatriusException.class)
    public final void testFeedOrekitException() throws PatriusException {
        final PatriusExceptionLoader loader = new PatriusExceptionLoader();
        new ClasspathCrawler("regular-data/UTC-TAI.history", "regular-data/de405-ephemerides/unxp0000.405",
            "regular-data/de405-ephemerides/unxp0001.405", "regular-data/de406-ephemerides/unxp0000.406",
            "regular-data/Earth-orientation-parameters/monthly/bulletinb_IAU2000-216.txt", "").feed(
            Pattern.compile(".*"), loader);
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#EXCEPTIONTHROWING}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.ClasspathCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description the purpose of this test is to check if the loader detects a wrong URL
     * 
     * @testPassCriteria the expected output is an URISyntaxException
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws MalformedURLException
     * @throws URISyntaxException
     * @throws PatriusException
     * 
     */
    @Test(expected = PatriusException.class)
    public final void feedFailsOnURI() throws MalformedURLException, URISyntaxException, PatriusException {
        // Lousy URL that cannot be converted to URI
        final URL lousyUrl = new URL("http://--- --");
        try {
            final ClasspathCrawler crawler = new ClasspathCrawler(lousyUrl.toString());
            crawler.feed(Pattern.compile(".*"), new LazyLoader());
        } catch (final PatriusException e) {
            final PatriusException tempE = new PatriusException(PatriusMessages.UNABLE_TO_FIND_RESOURCE,
                lousyUrl.toString());
            if (e.getLocalizedMessage().equals(tempE.getLocalizedMessage())) {
                throw e;
            }
        }
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description is for the case where the visitor accepts no data from the beginning
     * 
     * @input none
     * 
     * @output boolean
     * 
     * @testPassCriteria feed returns False and RefuseLoader did refuse data
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     */
    @Test
    public final void testRefuseLoader() throws PatriusException {
        final ClasspathCrawler zipJarCrawler = new ClasspathCrawler("regular-data/UTC-TAI.history");
        final RefuseLoader refuseLoader = new RefuseLoader();
        final boolean hasFed = zipJarCrawler.feed(Pattern.compile(".*"), refuseLoader);
        assertFalse(hasFed);
        assertTrue(refuseLoader.didRefuseOnce());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link ClasspathCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description is for the case where the visitor stops accepting data after 3 files loaded. The purpose
     *              of this test is to increase the conditional coverage of the feed method.
     * 
     * @input none
     * 
     * @output integer
     * 
     * @testPassCriteria the crawler has loaded 3 files
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     * 
     */
    @Test
    public final void testStoppingLoader() throws PatriusException, URISyntaxException {
        final StoppingLoader crawler = new StoppingLoader();
        new ClasspathCrawler("regular-data/UTC-TAI.history", "regular-data/de405-ephemerides/unxp0000.405",
            "regular-data/de405-ephemerides/unxp0001.405", "regular-data/de406-ephemerides/unxp0000.406").feed(
            Pattern.compile(".*"), crawler);
        Assert.assertEquals(3, crawler.getCount());
    }

    // Mock loader that stops accepting data files after 3 files loaded
    private static class StoppingLoader implements DataLoader {
        private int count = 0;

        @Override
        public boolean stillAcceptsData() {
            if (this.count > 2) {
                return false;
            }
            else {
                return true;
            }
        }

        @Override
        public void loadData(final InputStream input, final String name) {
            ++this.count;
        }

        public int getCount() {
            return this.count;
        }
    }

    // Mock loader that always refuses data
    private static class RefuseLoader implements DataLoader {

        /**
         * True when stillAcceptsData was called at least once.
         */
        private boolean didRefuseOnce = false;

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            this.didRefuseOnce = true;
            // always false!
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                        PatriusException {
            // does nothing
        }

        /**
         * True when, indeed, refused data at least once.
         * 
         * @return true when, indeed, refused data at least once.
         */
        public boolean didRefuseOnce() {
            return this.didRefuseOnce;
        }
    }

    // Mock for a loader that throws a PatriusException
    private static class PatriusExceptionLoader implements DataLoader {
        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name) throws ParseException, PatriusException {
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER);
        }
    }

    // Mock loader that does nothing
    private static class LazyLoader implements DataLoader {

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            // always true
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                        PatriusException {
            // does nothing
        }
    }
}
