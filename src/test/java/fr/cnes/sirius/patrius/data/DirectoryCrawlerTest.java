/**
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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DirectoryCrawlerTest {

    @Test(expected = PatriusException.class)
    public void testNoDirectory() throws PatriusException, URISyntaxException {
        final File existing = new File(this.getClass().getClassLoader().getResource("regular-data").toURI().getPath());
        final File inexistent = new File(existing.getParent(), "inexistant-directory");
        new DirectoryCrawler(inexistent).feed(Pattern.compile(".*"), new CountingLoader());
    }

    @Test(expected = PatriusException.class)
    public void testNotADirectory() throws PatriusException, URISyntaxException {
        final URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data/UTC-TAI.history");
        new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new CountingLoader());
    }

    @Test
    public void testNominal() throws PatriusException, URISyntaxException {
        final URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        final CountingLoader crawler = new CountingLoader();
        new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), crawler);
        Assert.assertTrue(crawler.getCount() > 0);
    }

    @Test
    public void testCompressed() throws PatriusException, URISyntaxException {
        final URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("compressed-data");
        final CountingLoader crawler = new CountingLoader();
        new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), crawler);
        Assert.assertTrue(crawler.getCount() > 0);
    }

    @Test
    public void testMultiZipClasspath() throws PatriusException, URISyntaxException {
        final URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("zipped-data/multizip.zip");
        final File parent = new File(url.toURI().getPath()).getParentFile();
        final CountingLoader crawler = new CountingLoader();
        new DirectoryCrawler(parent).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test(expected = PatriusException.class)
    public void testIOException() throws PatriusException, URISyntaxException {
        final URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        try {
            new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"), new IOExceptionLoader());
        } catch (final PatriusException oe) {
            // expected behavior
            Assert.assertNotNull(oe.getCause());
            Assert.assertEquals(IOException.class, oe.getCause().getClass());
            Assert.assertEquals("dummy error", oe.getMessage());
            throw oe;
        }
    }

    @Test(expected = PatriusException.class)
    public void testParseException() throws PatriusException, URISyntaxException {
        final URL url =
            DirectoryCrawlerTest.class.getClassLoader().getResource("regular-data");
        try {
            new DirectoryCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*"),
                new ParseExceptionLoader());
        } catch (final PatriusException oe) {
            // expected behavior
            Assert.assertNotNull(oe.getCause());
            Assert.assertEquals(ParseException.class, oe.getCause().getClass());
            Assert.assertEquals("dummy error", oe.getMessage());
            throw oe;
        }
    }

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

    private static class IOExceptionLoader implements DataLoader {
        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name) throws IOException {
            if (name.endsWith("UTC-TAI.history")) {
                throw new IOException("dummy error");
            }
        }
    }

    private static class ParseExceptionLoader implements DataLoader {
        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name) throws ParseException {
            if (name.endsWith("UTC-TAI.history")) {
                throw new ParseException("dummy error", 0);
            }
        }
    }

}
