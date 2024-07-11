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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Helper class for loading data files from a zip/jar archive.
 * 
 * <p>
 * This class browses all entries in a zip/jar archive in filesystem or in classpath.
 * </p>
 * <p>
 * The organization of entries within the archive is unspecified. All entries are checked in turn. If several entries of
 * the archive are supported by the data loader, all of them will be loaded.
 * </p>
 * <p>
 * Gzip-compressed files are supported.
 * </p>
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for zip entries browsing.
 * </p>
 * 
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class ZipJarCrawler implements DataProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -7180396021919528046L;

    /** Zip archive on the filesystem. */
    private final File file;

    /** Zip archive in the classpath. */
    private final String resource;

    /** Class loader to use. */
    private final ClassLoader classLoader;

    /** Zip archive on network. */
    private final URL url;

    /** Prefix name of the zip. */
    private final String name;

    /**
     * Build a zip crawler for an archive file on filesystem.
     * 
     * @param fileIn
     *        zip file to browse
     */
    public ZipJarCrawler(final File fileIn) {
        this.file = fileIn;
        this.resource = null;
        this.classLoader = null;
        this.url = null;
        this.name = fileIn.getAbsolutePath();
    }

    /**
     * Build a zip crawler for an archive file in classpath.
     * <p>
     * Calling this constructor has the same effect as calling {@link #ZipJarCrawler(ClassLoader, String)} with
     * {@code ZipJarCrawler.class.getClassLoader()} as first argument.
     * </p>
     * 
     * @param resourceIn
     *        name of the zip file to browse
     * @exception PatriusException
     *            if resource name is malformed
     */
    public ZipJarCrawler(final String resourceIn) throws PatriusException {
        this(ZipJarCrawler.class.getClassLoader(), resourceIn);
    }

    /**
     * Build a zip crawler for an archive file in classpath.
     * 
     * @param classLoaderIn
     *        class loader to use to retrieve the resources
     * @param resourceIn
     *        name of the zip file to browse
     * @exception PatriusException
     *            if resource name is malformed
     */
    public ZipJarCrawler(final ClassLoader classLoaderIn, final String resourceIn) throws PatriusException {
        try {
            this.file = null;
            this.resource = resourceIn;
            this.classLoader = classLoaderIn;
            this.url = null;
            this.name = classLoaderIn.getResource(resourceIn).toURI().toString();
        } catch (final URISyntaxException use) {
            throw new PatriusException(use, PatriusMessages.SIMPLE_MESSAGE, use.getMessage());
        }
    }

    /**
     * Build a zip crawler for an archive file on network.
     * 
     * @param urlIn
     *        URL of the zip file on network
     * @exception PatriusException
     *            if url syntax is malformed
     */
    public ZipJarCrawler(final URL urlIn) throws PatriusException {
        try {
            this.file = null;
            this.resource = null;
            this.classLoader = null;
            this.url = urlIn;
            this.name = urlIn.toURI().toString();
        } catch (final URISyntaxException use) {
            throw new PatriusException(use, PatriusMessages.SIMPLE_MESSAGE, use.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean feed(final Pattern supported, final DataLoader visitor) throws PatriusException {

        try {

            // open the raw data stream
            final InputStream rawStream;
            if (this.file != null) {
                rawStream = new FileInputStream(this.file);
            } else if (this.resource != null) {
                rawStream = this.classLoader.getResourceAsStream(this.resource);
            } else {
                rawStream = this.url.openConnection().getInputStream();
            }

            // add the zip format analysis layer and browse the archive
            final ZipInputStream zip = new ZipInputStream(rawStream);
            final boolean loaded = this.feed(this.name, supported, visitor, zip);
            zip.close();

            // Return result
            return loaded;

        } catch (final IOException ioe) {
            // Exception
            throw new PatriusException(ioe, new DummyLocalizable(ioe.getMessage()));
        } catch (final ParseException pe) {
            // Exception
            throw new PatriusException(pe, new DummyLocalizable(pe.getMessage()));
        }

    }

    /**
     * Feed a data file loader by browsing the entries in a zip/jar.
     * 
     * @param prefix
     *        prefix to use for name
     * @param supported
     *        pattern for file names supported by the visitor
     * @param visitor
     *        data file visitor to use
     * @param zip
     *        zip/jar input stream
     * @exception PatriusException
     *            if some data is missing, duplicated
     *            or can't be read
     * @return true if something has been loaded
     * @exception IOException
     *            if data cannot be read
     * @exception ParseException
     *            if data cannot be read
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private boolean feed(final String prefix, final Pattern supported,
                         final DataLoader visitor, final ZipInputStream zip)
                                                                            throws PatriusException, IOException,
                                                                            ParseException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        PatriusException delayedException = null;
        boolean loaded = false;

        // loop over all entries
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {

            try {

                if (visitor.stillAcceptsData() && !entry.isDirectory()) {

                    final String fullName = prefix + "!" + entry.getName();

                    if (ZIP_ARCHIVE_PATTERN.matcher(entry.getName()).matches()) {

                        // recurse inside the archive entry
                        loaded = this.feed(fullName, supported, visitor, new ZipInputStream(zip)) || loaded;

                    } else {

                        // remove leading directories
                        String entryName = entry.getName();
                        final int lastSlash = entryName.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            entryName = entryName.substring(lastSlash + 1);
                        }

                        // remove suffix from gzip entries
                        final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(entryName);
                        final String baseName = gzipMatcher.matches() ? gzipMatcher.group(1) : entryName;

                        if (supported.matcher(baseName).matches()) {

                            // visit the current entry
                            final InputStream stream =
                                gzipMatcher.matches() ? new GZIPInputStream(zip) : zip;
                            visitor.loadData(stream, fullName);
                            loaded = true;

                        }

                    }

                }

            } catch (final PatriusException oe) {
                delayedException = oe;
            }

            // prepare next entry processing
            zip.closeEntry();
            entry = zip.getNextEntry();

        }

        if (!loaded && delayedException != null) {
            throw delayedException;
        }
        return loaded;

    }

}
