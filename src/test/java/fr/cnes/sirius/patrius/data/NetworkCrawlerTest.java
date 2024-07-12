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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class NetworkCrawlerTest {

    @Test(expected = PatriusException.class)
    public void noElement() throws PatriusException, MalformedURLException {
        final File existing = new File(this.url("regular-data").getPath());
        final File inexistent = new File(existing.getParent(), "inexistant-directory");
        new NetworkCrawler(inexistent.toURI().toURL()).feed(Pattern.compile(".*"), new CountingLoader());
    }

    // WARNING!
    // the following test is commented out by default, as it does connect to the web
    // if you want to enable it, you will have uncomment it and to either set the proxy
    // settings according to your local network or remove the proxy authentication
    // settings if you have a transparent connection to internet
    // @Test
    // public void remote() throws java.net.MalformedURLException, OrekitException, URISyntaxException {
    //
    // System.setProperty("http.proxyHost", "proxy.your.domain.com");
    // System.setProperty("http.proxyPort", "8080");
    // System.setProperty("http.nonProxyHosts", "localhost|*.your.domain.com");
    // java.net.Authenticator.setDefault(new AuthenticatorDialog());
    // CountingLoader loader = new CountingLoader();
    // NetworkCrawler crawler =
    // new NetworkCrawler(new URL("http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history"));
    // crawler.setTimeout(1000);
    // crawler.feed(Pattern.compile(".*\\.history"), loader);
    // Assert.assertEquals(1, loader.getCount());
    //
    // }

    @Test
    public void local() throws PatriusException {
        final CountingLoader crawler = new CountingLoader();
        new NetworkCrawler(this.url("regular-data/UTC-TAI.history"),
            this.url("regular-data/de405-ephemerides/unxp0000.405"),
            this.url("regular-data/de405-ephemerides/unxp0001.405"),
            this.url("regular-data/de406-ephemerides/unxp0000.406"),
            this.url("regular-data/Earth-orientation-parameters/monthly/bulletinb_IAU2000-216.txt"),
            this.url("no-data")).feed(Pattern.compile(".*"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test
    public void compressed() throws PatriusException {
        final CountingLoader crawler = new CountingLoader();
        new NetworkCrawler(this.url("compressed-data/UTC-TAI.history.gz"),
            this.url("compressed-data/eopc04_IAU2000.00.gz"),
            this.url("compressed-data/eopc04_IAU2000.02.gz")).feed(Pattern.compile("^eopc04.*"), crawler);
        Assert.assertEquals(2, crawler.getCount());
    }

    @Test
    public void multiZip() throws PatriusException {
        final CountingLoader crawler = new CountingLoader();
        new NetworkCrawler(this.url("zipped-data/multizip.zip")).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test(expected = PatriusException.class)
    public void ioException() throws PatriusException {
        try {
            new NetworkCrawler(this.url("regular-data/UTC-TAI.history"))
                .feed(Pattern.compile(".*"), new IOExceptionLoader());
        } catch (final PatriusException oe) {
            // expected behavior
            Assert.assertNotNull(oe.getCause());
            Assert.assertEquals(IOException.class, oe.getCause().getClass());
            Assert.assertEquals("dummy error", oe.getMessage());
            throw oe;
        }
    }

    @Test(expected = PatriusException.class)
    public void parseException() throws PatriusException {
        try {
            new NetworkCrawler(this.url("regular-data/UTC-TAI.history")).feed(Pattern.compile(".*"),
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

    private URL url(final String resource) {
        return DirectoryCrawlerTest.class.getClassLoader().getResource(resource);
    }

}
