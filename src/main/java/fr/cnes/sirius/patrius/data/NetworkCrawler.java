/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Provider for data files directly fetched from network.
 * 
 * <p>
 * This class handles a list of URLs pointing to data files or zip/jar on the net. Since the net is not a tree structure
 * the list elements cannot be top elements recursively browsed as in {@link DirectoryCrawler}, they must be data files
 * or zip/jar archives.
 * </p>
 * <p>
 * The files fetched from network can be locally cached on disk. This prevents too frequent network access if the URLs
 * are remote ones (for example original internet URLs).
 * </p>
 * <p>
 * If the URL points to a remote server (typically on the web) on the other side of a proxy server, you need to
 * configure the networking layer of your application to use the proxy. For a typical authenticating proxy as used in
 * many corporate environments, this can be done as follows using for example the AuthenticatorDialog graphical
 * authenticator class that can be found in the tests directories:
 * 
 * <pre>
 * System.setProperty(&quot;http.proxyHost&quot;, &quot;proxy.your.domain.com&quot;);
 * System.setProperty(&quot;http.proxyPort&quot;, &quot;8080&quot;);
 * System.setProperty(&quot;http.nonProxyHosts&quot;, &quot;localhost|*.your.domain.com&quot;);
 * Authenticator.setDefault(new AuthenticatorDialog());
 * </pre>
 * 
 * </p>
 * <p>
 * Gzip-compressed files are supported.
 * </p>
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for list browsing.
 * </p>
 * 
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
public class NetworkCrawler implements DataProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -6846661159486123245L;

    /** Default time-out. */
    private static final int DEFAULT_TIMEOUT = 10000;

    /** URLs list. */
    private final List<URL> urls;

    /** Connection timeout (milliseconds). */
    private int timeout;

    /**
     * Build a data classpath crawler.
     * <p>
     * The default timeout is set to 10 seconds.
     * </p>
     * 
     * @param urlsIn
     *        list of data file URLs
     */
    public NetworkCrawler(final URL... urlsIn) {

        this.urls = new ArrayList<URL>();
        for (final URL url : urlsIn) {
            this.urls.add(url);
        }

        this.timeout = DEFAULT_TIMEOUT;

    }

    /**
     * Set the timeout for connection.
     * 
     * @param timeoutIn
     *        connection timeout in milliseconds
     */
    public void setTimeout(final int timeoutIn) {
        this.timeout = timeoutIn;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    public boolean feed(final Pattern supported, final DataLoader visitor) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        try {
            PatriusException delayedException = null;
            boolean loaded = false;
            for (final URL url : this.urls) {
                try {

                    if (visitor.stillAcceptsData()) {
                        final String name = url.toURI().toString();
                        final String fileName = new File(url.getPath()).getName();
                        if (ZIP_ARCHIVE_PATTERN.matcher(fileName).matches()) {

                            // browse inside the zip/jar file
                            new ZipJarCrawler(url).feed(supported, visitor);
                            loaded = true;

                        } else {

                            // remove suffix from gzip files
                            final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(fileName);
                            final String baseName = gzipMatcher.matches() ? gzipMatcher.group(1) : fileName;

                            if (supported.matcher(baseName).matches()) {

                                final InputStream stream = this.getStream(url);

                                // visit the current file
                                if (gzipMatcher.matches()) {
                                    visitor.loadData(new GZIPInputStream(stream), name);
                                } else {
                                    visitor.loadData(stream, name);
                                }

                                stream.close();
                                loaded = true;

                            }

                        }
                    }

                } catch (final PatriusException oe) {
                    // maybe the next path component will be able to provide data
                    // wait until all components have been tried
                    delayedException = oe;
                }
            }

            if (!loaded && delayedException != null) {
                // Exception
                throw delayedException;
            }

            // Return results
            return loaded;

        } catch (final URISyntaxException use) {
            // Exception
            throw new PatriusException(use, new DummyLocalizable(use.getMessage()));
        } catch (final IOException ioe) {
            // Exception
            throw new PatriusException(ioe, new DummyLocalizable(ioe.getMessage()));
        } catch (final ParseException pe) {
            // Exception
            throw new PatriusException(pe, new DummyLocalizable(pe.getMessage()));
        }

    }

    /**
     * Get the stream to read from the remote URL.
     * 
     * @param url
     *        url to read from
     * @return stream to read the content of the URL
     * @throws IOException
     *         if the URL cannot be opened for reading
     */
    private InputStream getStream(final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(this.timeout);
        return connection.getInputStream();
    }

}
