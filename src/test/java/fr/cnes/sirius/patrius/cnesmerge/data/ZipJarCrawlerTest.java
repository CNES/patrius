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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.ZipJarCrawler;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * -this class adds tests in order to raise the cover rate of ZipJarCrawler
 * </p>
 * IMPORTANT NOTE : this class uses zipped-dataCNES,
 * that shall be safely replaced by zipped-data
 * when this test is merged inside Orekit
 * 
 * @author Philippe Pavero
 * 
 * @version $Id: ZipJarCrawlerTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.0
 * 
 */
public class ZipJarCrawlerTest {
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
         * @featureTitle Object construction tests
         * 
         * @featureDescription the purpose of this feature is to increase the cover on the constructor
         * 
         * @coveredRequirements NA
         */
        CONSTRUCTION,

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
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTION}
     * 
     * @testedMethod {@link ZipJarCrawler#ZipJarCrawler(String)}
     * 
     * @description the crawler searches for resources in a zip file. The construction is based on the resource name of
     *              the zip file.
     * 
     * @testPassCriteria there should be 6 resources found
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     */
    @Test
    public final void testConstructorWithResource() throws PatriusException, URISyntaxException {
        final CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler("zipped-dataCNES/multizip.zip").feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#CONSTRUCTION}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.ZipJarCrawler#ZipJarCrawler(URL)}
     * 
     * @description the crawler search for resources in a zip file.
     *              The construction is based on the URL of the zip file.
     * 
     * @testPassCriteria there should be 6 resources found
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     */
    @Test
    public final void testConstructorWithUrl() throws PatriusException, URISyntaxException {
        final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-dataCNES/multizip.zip");
        final CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler(url).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#EXCEPTIONTHROWING}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.ZipJarCrawler#ZipJarCrawler(URL)}
     * 
     * @description the purpose of this test is to check if the provider detects a wrong URL
     * 
     * @testPassCriteria the exception URISyntaxException is raised
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    @Test(expected = URISyntaxException.class)
    public final void testConstructorWithUrlURIExp() throws PatriusException, URISyntaxException, MalformedURLException {
        try {
            final URL url = new URL("http://--- --");
            final CountingLoader crawler = new CountingLoader();
            new ZipJarCrawler(url).feed(Pattern.compile(".*\\.txt$"), crawler);
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
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#EXCEPTIONTHROWING}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description the purpose of this test is to check if the provider throws an OrekitException when
     *              the loader raises an OrekitException. We use a loader that always throws this exception when fed.
     * 
     * @testPassCriteria the exception OrekitException is raised
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     */
    @Test(expected = PatriusException.class)
    public final void testFeedOrekitException() throws PatriusException, URISyntaxException {
        final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-dataCNES/multizip.zip");
        new ZipJarCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new PatriusExceptionLoader());
    }

    /**
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#EXCEPTIONTHROWING}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description the purpose of this test is to check if the provider throws an OrekitException when
     *              the loader raises an OrekitException. We use a loader that always throws this exception when fed.
     * 
     * @testPassCriteria The purpose of this test is to check if the provider throws an OrekitException when
     *                   the loader raises an ParseException. We use a loader that always throws this exception when
     *                   fed.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     * @throws ParseException
     */
    @Test(expected = ParseException.class)
    public final void testFeedParseException() throws PatriusException, URISyntaxException, ParseException {
        try {
            final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-dataCNES/multizip.zip");
            new ZipJarCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new ParseExceptionLoader());
        } catch (final PatriusException e) {
            // We expect an OrekitException embedding an URISyntaxException
            // so we throw the embedded exception : JUnit will check if it's
            // the expected one
            final Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof ParseException) {
                    throw (ParseException) cause;
                }
            }
        }
    }

    /**
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
        final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-dataCNES/multizip.zip");
        final ZipJarCrawler zipJarCrawler = new ZipJarCrawler(url);
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
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
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
     */
    @Test
    public final void testStoppingLoader() throws PatriusException, URISyntaxException {
        final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-dataCNES/multizip.zip");
        final StoppingLoader crawler = new StoppingLoader();
        new ZipJarCrawler(url).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(3, crawler.getCount());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description this test is for the case where the data files are located directly under the the zip directory,
     *              meaning that there should not be a "/" in their name. The purpose
     *              of this test is to increase the conditional coverage of the feed method.
     * 
     * @input none
     * 
     * @output integer
     * 
     * @testPassCriteria the crawler has loaded 5 files
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     */
    @Test
    public final void testFilesDirectlyUnderZip() throws PatriusException, URISyntaxException {
        final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource(
            "zipped-data_directlyInZip/zipped-text-files.zip");
        final CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler(url).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(5, crawler.getCount());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description this test is for the case where one of the dataFiles does not match the pattern. The purpose
     *              of this test is to increase the conditional coverage of the feed method.
     * 
     * @input none
     * 
     * @output integer
     * 
     * @testPassCriteria the crawler has loaded 4 files
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     */
    @Test
    public final void testFilesUnrecognized() throws PatriusException, URISyntaxException {
        final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource(
            "zipped-data_directlyInZip/zipped-text-files-bis.zip");
        final CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler(url).feed(Pattern.compile("doc(\\p{Digit})\\.txt$"), crawler);
        Assert.assertEquals(4, crawler.getCount());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.data.DirectoryCrawler#feed(java.util.regex.Pattern, fr.cnes.sirius.patrius.data.DataLoader)}
     * 
     * @description this test is for the case where no data is loaded and an OrekitException is thrown. The purpose
     *              of this test is to increase the conditional coverage of the feed method.
     * 
     * @input none
     * 
     * @output OrekitException
     * 
     * @testPassCriteria an OrekitException is thrown
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     * @throws URISyntaxException
     */
    @Test(expected = PatriusException.class)
    public final void testNoLoadingAndException() throws PatriusException, URISyntaxException {
        final URL url = ZipJarCrawlerTest.class.getClassLoader().getResource(
            "zipped-data_directlyInZip/zipped-text-files-bis.zip");
        final PatriusExceptionLoader crawler = new PatriusExceptionLoader();
        new ZipJarCrawler(url).feed(Pattern.compile("doc(\\p{Digit})\\.txt$"), crawler);
    }

    // Mock for a loader that throws a Patrius Exception
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

    // Mock for a loader that throws a ParseException
    private static class ParseExceptionLoader implements DataLoader {
        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name) throws ParseException {
            throw new ParseException("dummy error", 0);
        }
    }

    // Mock loader that counts the number of loaded data files
    private static class CountingLoader implements DataLoader {
        private int count = 0;

        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name) {
            ++this.count;
        }

        public int getCount() {
            return this.count;
        }
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
}
