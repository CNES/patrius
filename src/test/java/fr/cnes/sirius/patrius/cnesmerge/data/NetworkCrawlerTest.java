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

import org.junit.Test;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.NetworkCrawler;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * @description Complementary tests for the NetworkCrawler class
 * 
 * @author cardosop
 * 
 * @version $Id: NetworkCrawlerTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class NetworkCrawlerTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Increase the code coverage
         * 
         * @featureDescription Increase the code coverage
         * 
         * @coveredRequirements NA
         */
        COVERAGE
    }

    /** A data name pattern */
    private static final String STARPAT = ".*";

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.NetworkCrawler#setTimeout}
     * 
     * @description Unit test for setTimeout code coverage
     * 
     * @input int timeout = 357 : value likely to not be the default
     * 
     * @output none
     * 
     * @testPassCriteria none, coverage only
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     */
    @Test
    public final void setTimeout() {
        final NetworkCrawler useless = new NetworkCrawler();
        useless.setTimeout(357);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.NetworkCrawler#feed}
     * 
     * @description is for the case where the visitor accepts no data from the beginning.<br>
     *              This is mainly for code coverage.<br>
     *              Test method for
     *              {@link fr.cnes.sirius.patrius.data.NetworkCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     *              .
     * 
     * @input none
     * 
     * @output boolean
     * 
     * @testPassCriteria feed returns False and RefuznikLoader did refuse data.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws MalformedURLException
     *         if wrong URL
     * @throws PatriusException
     *         if problem
     */
    @Test
    public final void refuznikVisitor() throws MalformedURLException, PatriusException {
        final NetworkCrawler ncraw = new NetworkCrawler(new URL("http://bogussss"));
        final RefusingLoader rf = new RefusingLoader();
        final boolean hasFed = ncraw.feed(Pattern.compile(STARPAT), rf);
        assertFalse(hasFed);
        assertTrue(rf.didRefuseOnce());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.NetworkCrawler#feed}
     * 
     * @description is for the case where the feed fails because the loader rejects the data.<br>
     *              This is mainly for code coverage.<br>
     *              Test method for
     *              {@link fr.cnes.sirius.patrius.data.NetworkCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     *              .
     * 
     * @input none
     * 
     * @output boolean
     * 
     * @testPassCriteria OrekitException when ChokingLoader rejects data.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws MalformedURLException
     *         if wrong URL
     * @throws PatriusException
     *         if problem
     */
    @Test(expected = PatriusException.class)
    public final void feedFailsOnReject() throws MalformedURLException, PatriusException {
        final NetworkCrawler ncraw = new NetworkCrawler(this.url("regular-data/UTC-TAI.history"));
        final RejectingLoader cl = new RejectingLoader();
        ncraw.feed(Pattern.compile(STARPAT), cl);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.NetworkCrawler#feed}
     * 
     * @description is for the case where the feed fails because of an URISyntaxException.<br>
     *              This is mainly for code coverage.<br>
     *              Test method for
     *              {@link fr.cnes.sirius.patrius.data.NetworkCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     *              .
     * 
     * @input none
     * 
     * @output boolean
     * 
     * @testPassCriteria URISyntaxException during feed.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws MalformedURLException
     *         if wrong URL
     * @throws URISyntaxException
     *         if malformed URI
     */
    @Test(expected = URISyntaxException.class)
    public final void feedFailsOnURI() throws MalformedURLException, URISyntaxException {
        try {
            // Lousy URL that cannot be converted to URI
            final URL lousyUrl = new URL("http://--- --");
            final NetworkCrawler ncraw = new NetworkCrawler(lousyUrl);
            ncraw.feed(Pattern.compile(STARPAT), new NiceLoader());
        } catch (final PatriusException e) {
            // We expect an OrekitException embedding an URISyntaxException
            // so we throw the embedded exception : JUnit will check if it's
            // the expected one
            final Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof URISyntaxException) {
                    throw (URISyntaxException) cause;
                }
            }
        }
    }

    /**
     * @description Dummy loader that accepts no data.
     * @author cardosop
     */
    private static class RefusingLoader implements DataLoader {

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

    /**
     * @description Dummy loader that always rejects data.
     * @author cardosop
     */
    private static class RejectingLoader implements DataLoader {

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
            // Simulates the loader rejecting the data
            throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
        }

    }

    /**
     * @description Dummy loader that plays nice.
     * @author cardosop
     */
    private static class NiceLoader implements DataLoader {

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

    /**
     * @description URL loader.
     * @param resource
     *        the resource to be URLified
     * @return an URL
     */
    private URL url(final String resource) {
        return NetworkCrawlerTest.class.getClassLoader().getResource(resource);
    }

}
