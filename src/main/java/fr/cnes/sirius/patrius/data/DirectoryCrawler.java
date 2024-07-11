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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
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
import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Provider for data files stored in a directories tree on filesystem.
 * 
 * <p>
 * This class handles data files recursively starting from a root directories tree. The organization of files in the
 * directories is free. There may be sub-directories to any level. All sub-directories are browsed and all terminal
 * files are checked for loading.
 * </p>
 * <p>
 * Gzip-compressed files are supported.
 * </p>
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for directory hierarchy crawling.
 * </p>
 * 
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
public class DirectoryCrawler implements DataProvider {

     /** Serializable UID. */
    private static final long serialVersionUID = -1472871512868530337L;

    /** Root directory. */
    private final File root;

    /**
     * Build a data files crawler.
     * 
     * @param rootIn
     *        root of the directories tree (must be a directory)
     * @exception PatriusException
     *            if root is not a directory
     */
    public DirectoryCrawler(final File rootIn) throws PatriusException {
        if (!rootIn.isDirectory()) {
            throw new PatriusException(PatriusMessages.NOT_A_DIRECTORY, rootIn.getAbsolutePath());
        }
        this.root = rootIn;
    }

    /** {@inheritDoc} */
    @Override
    public boolean feed(final Pattern supported, final DataLoader visitor) throws PatriusException {
        try {
            return this.feed(supported, visitor, this.root);
        } catch (final IOException ioe) {
            throw new PatriusException(ioe, new DummyLocalizable(ioe.getMessage()));
        } catch (final ParseException pe) {
            throw new PatriusException(pe, new DummyLocalizable(pe.getMessage()));
        }
    }

    /**
     * Feed a data file loader by browsing a directory hierarchy.
     * 
     * @param supported
     *        pattern for file names supported by the visitor
     * @param visitor
     *        data file visitor to feed
     * @param directory
     *        current directory
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
    // Reason: Commons-Math code kept as such
    private boolean feed(final Pattern supported, final DataLoader visitor,
                         final File directory) throws PatriusException, IOException, ParseException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // search in current directory
        final File[] list = directory.listFiles();
        // we sort the list (so it's consistent on any OS)
        Arrays.sort(list);

        PatriusException delayedException = null;
        boolean loaded = false;
        for (final File element : list) {
            try {
                if (visitor.stillAcceptsData()) {
                    if (element.isDirectory()) {

                        // recurse in the sub-directory
                        loaded = this.feed(supported, visitor, element) || loaded;

                    } else if (ZIP_ARCHIVE_PATTERN.matcher(element.getName()).matches()) {

                        // browse inside the zip/jar file
                        final DataProvider zipProvider = new ZipJarCrawler(element);
                        loaded = zipProvider.feed(supported, visitor) || loaded;

                    } else {

                        // remove suffix from gzip files
                        final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(element.getName());
                        final String baseName =
                            gzipMatcher.matches() ? gzipMatcher.group(1) : element.getName();

                        if (supported.matcher(baseName).matches()) {

                            // visit the current file
                            InputStream input = new FileInputStream(element);
                            if (gzipMatcher.matches()) {
                                input = new GZIPInputStream(input);
                            }
                            visitor.loadData(input, element.getPath());
                            input.close();
                            loaded = true;

                        }

                    }
                }
            } catch (final PatriusException oe) {
                delayedException = oe;
            }

        }

        if (!loaded && delayedException != null) {
            throw delayedException;
        }

        return loaded;

    }

}
