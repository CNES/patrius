/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:265:19/09/2014:bug in ClasspathCrawler with regard to data file path names
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Provider for data files stored as resources in the classpath.
 * 
 * <p>
 * This class handles a list of data files or zip/jar archives located in the classpath. Since the classpath is not a
 * tree structure the list elements cannot be whole directories recursively browsed as in {@link DirectoryCrawler}, they
 * must be data files or zip/jar archives.
 * </p>
 * <p>
 * A typical use case is to put all data files in a single zip or jar archive and to build an instance of this class
 * with the single name of this zip/jar archive. Two different instances may be used one for user or project specific
 * data and another one for system-wide or general data.
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
public class ClasspathCrawler implements DataProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -1867610154167960050L;

    /** List elements. */
    private final List<String> listElements;

    /** Class loader to use. */
    private final ClassLoader classLoader;

    /**
     * Build a data classpath crawler.
     * <p>
     * Calling this constructor has the same effect as calling {@link #ClasspathCrawler(ClassLoader, String...)} with
     * {@code ClasspathCrawler.class.getClassLoader()} as first argument.
     * </p>
     * 
     * @param list
     *        list of data file names within the classpath
     * @exception PatriusException
     *            if a list elements is not an existing resource
     */
    public ClasspathCrawler(final String... list) throws PatriusException {
        this(ClasspathCrawler.class.getClassLoader(), list);
    }

    /**
     * Build a data classpath crawler.
     * 
     * @param classLoaderIn
     *        class loader to use to retrieve the resources
     * @param list
     *        list of data file names within the classpath
     * @exception PatriusException
     *            if a list elements is not an existing resource
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public ClasspathCrawler(final ClassLoader classLoaderIn, final String... list) throws PatriusException {

        this.listElements = new ArrayList<String>();
        this.classLoader = classLoaderIn;

        // check the resources
        for (final String name : list) {
            if (!"".equals(name)) {

                final String convertedName = name.replace('\\', '/');
                final InputStream stream = classLoaderIn.getResourceAsStream(convertedName);
                if (stream == null) {
                    throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_RESOURCE, name);
                }

                this.listElements.add(convertedName);
                try {
                    stream.close();
                } catch (final IOException exc) {
                    // ignore this error
                }
            }
        }

    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Commons-Math code kept as such
    @Override
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public boolean feed(final Pattern supported, final DataLoader visitor) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        try {
            PatriusException delayedException = null;
            boolean loaded = false;
            for (final String name : this.listElements) {
                try {

                    if (visitor.stillAcceptsData()) {

                        if (ZIP_ARCHIVE_PATTERN.matcher(name).matches()) {

                            // browse inside the zip/jar file
                            final DataProvider zipProvider = new ZipJarCrawler(name);
                            loaded = zipProvider.feed(supported, visitor) || loaded;

                        } else {

                            // remove prefix from file name
                            final int pos = name.lastIndexOf('/');
                            String baseName = name.substring((pos == -1) ? 0 : (pos + 1));
                            // remove suffix from gzip files
                            final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(baseName);
                            baseName = gzipMatcher.matches() ? gzipMatcher.group(1) : baseName;

                            if (supported.matcher(baseName).matches()) {

                                final InputStream stream = this.classLoader.getResourceAsStream(name);
                                final URI uri = this.classLoader.getResource(name).toURI();

                                // visit the current file
                                if (gzipMatcher.matches()) {
                                    visitor.loadData(new GZIPInputStream(stream), uri.toString());
                                } else {
                                    visitor.loadData(stream, uri.toString());
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
                } catch (final URISyntaxException use) {
                    // this should bever happen
                    throw new PatriusException(use, PatriusMessages.SIMPLE_MESSAGE, use.getMessage());
                }
            }

            if (!loaded && delayedException != null) {
                throw delayedException;
            }

            return loaded;

        } catch (final IOException ioe) {
            throw new PatriusException(ioe, new DummyLocalizable(ioe.getMessage()));
        } catch (final ParseException pe) {
            throw new PatriusException(pe, new DummyLocalizable(pe.getMessage()));
        }

    }

}
