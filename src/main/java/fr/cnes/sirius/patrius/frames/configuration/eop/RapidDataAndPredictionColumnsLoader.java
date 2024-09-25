/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.11:DM:DM-3260:22/05/2023:[PATRIUS] Harmonisation des EOP2000HistoryLoader
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for IERS rapid data and prediction files in columns format (finals file).
 * <p>
 * Rapid data and prediction files contain {@link EOPEntry
 * Earth Orientation Parameters} for several years periods, in one file only that is updated regularly.
 * </p>
 * <p>
 * These files contain both the data from IERS Bulletin A and IERS bulletin B. This class parses only the part from
 * Bulletin A.
 * </p>
 * <p>
 * The rapid data and prediction file is recognized thanks to its base name, which must match one of the the patterns
 * <code>finals.*</code> or <code>finals2000A.*</code> (or the same ending with <code>.gz</code> for gzip-compressed
 * files) where * stands for a word like "all", "daily", or "data". The file with 2000A in their name correspond to the
 * IAU-2000 precession-nutation model whereas the files without any identifier correspond to the IAU-1980
 * precession-nutation model. The files with the all suffix start from 1973-01-01, the file with the data suffix start
 * from 1992-01-01 and the files with the daily suffix.
 * </p>
 * 
 * @author Romain Di Costanzo
 * @see <a href="http://maia.usno.navy.mil/ser7/readme.finals2000A">file format description at USNO</a>
 */
@SuppressWarnings("PMD.NullAssignment")
public class RapidDataAndPredictionColumnsLoader implements EOP1980HistoryLoader, EOP2000HistoryLoader {

    /** 1296000. */
    private static final double COEF = 1296000.;

    /** 1000. */
    private static final double ONE_THOUSAND = 1000.;

    /** Conversion factor. */
    private static final double ARC_SECONDS_TO_RADIANS = 2 * FastMath.PI / COEF;

    /** Conversion factor. */
    private static final double MILLI_ARC_SECONDS_TO_RADIANS = ARC_SECONDS_TO_RADIANS / ONE_THOUSAND;

    /** Conversion factor. */
    private static final double MILLI_SECONDS_TO_SECONDS = 1.0e-3;

    /** Field for year, month and day parsing. */
    private static final String INTEGER2_FIELD = "((?:\\p{Blank}|\\p{Digit})\\p{Digit})";

    /** Field for modified Julian day parsing. */
    private static final String MJD_FIELD = "\\p{Blank}+(\\p{Digit}+)(?:\\.00*)";

    /** Field for separator parsing. */
    private static final String SEPARATOR = "\\p{Blank}*[IP]";

    /** Field for real parsing. */
    private static final String REAL_FIELD = "\\p{Blank}*(-?\\p{Digit}*\\.\\p{Digit}*)";

    /** Start index of the date part of the line. */
    private static final int DATE_START = 0;

    /** end index of the date part of the line. */
    private static final int DATE_END = 15;

    /** Pattern to match the date part of the line (always present). */
    private static final Pattern DATE_PATTERN = Pattern.compile(INTEGER2_FIELD + INTEGER2_FIELD + INTEGER2_FIELD
            + MJD_FIELD);

    /** Start index of the pole part of the line. */
    private static final int POLE_START = 16;

    /** end index of the pole part of the line. */
    private static final int POLE_END = 55;

    /** Pattern to match the pole part of the line. */
    private static final Pattern POLE_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD + REAL_FIELD
            + REAL_FIELD);

    /** Start index of the UT1-UTC part of the line. */
    private static final int UT1_UTC_START = 57;

    /** end index of the UT1-UTC part of the line. */
    private static final int UT1_UTC_END = 78;

    /** Pattern to match the UT1-UTC part of the line. */
    private static final Pattern UT1_UTC_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD);

    /** Start index of the LOD part of the line. */
    private static final int LOD_START = 79;

    /** end index of the LOD part of the line. */
    private static final int LOD_END = 93;

    /** Pattern to match the LOD part of the line. */
    private static final Pattern LOD_PATTERN = Pattern.compile(REAL_FIELD + REAL_FIELD);

    /** Start index of the nutation part of the line. */
    private static final int NUTATION_START = 95;

    /** end index of the nutation part of the line. */
    private static final int NUTATION_END = 134;

    /** Pattern to match the nutation part of the line. */
    private static final Pattern NUTATION_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD + REAL_FIELD
            + REAL_FIELD);

    /** 70. */
    private static final int SEVENTY = 70;

    /** Nineteenth century. */
    private static final int NINETEENTH = 1900;

    /** Twentieth century. */
    private static final int TWENTIETH = 2000;

    /** History entries for IAU1980. */
    private EOP1980History history1980;

    /** History entries for IAU2000. */
    private EOP2000History history2000;

    /** File supported name. */
    private final String supportedNames;

    /**
     * Build a loader for IERS bulletins B files.
     */
    public RapidDataAndPredictionColumnsLoader() {
        this.supportedNames = "";
    }

    /**
     * Build a loader for IERS bulletins B files.
     * 
     * @param supportedNamesIn
     *        regular expression for supported files names
     */
    public RapidDataAndPredictionColumnsLoader(final String supportedNamesIn) {
        this.supportedNames = supportedNamesIn;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    public void loadData(final InputStream input, final String name) throws PatriusException, IOException {
        // CHECKSTYLE: resume CyclomaticComplexity check
        loadDataIn(input, name);
    }

    /**
     * Load data from a stream.
     * 
     * @param input
     *        data input stream
     * @param name
     *        name of the file (or zip entry)
     * @exception IOException
     *            if data can't be read
     * @exception PatriusException
     *            if some data is missing
     *            or if some loader specific error occurs
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    private void loadDataIn(final InputStream input, final String name) throws PatriusException, IOException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // set up a reader for line-oriented bulletin B files
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        // Init x
        double x = 0;
        // Init y
        double y = 0;
        // Init dtu1
        double dtu1 = 0;
        // Init lod
        double lod = 0;
        // Init nutationDX
        double nutationDX = 0;
        // Init nutationDY
        double nutationDY = 0;
        int date = 0;
        // loop on the lines
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            // split the lines in its various columns (some of them can be blank)
            final String datePart = (line.length() >= DATE_END) ? line.substring(DATE_START, DATE_END) : "";
            final String polePart = (line.length() >= POLE_END) ? line.substring(POLE_START, POLE_END) : "";
            final String ut1utcPart = (line.length() >= UT1_UTC_END) ? line.substring(UT1_UTC_START, UT1_UTC_END) : "";
            final String lodPart = (line.length() >= LOD_END) ? line.substring(LOD_START, LOD_END) : "";
            final String nutationPart = (line.length() >= NUTATION_END) ? line.substring(NUTATION_START, NUTATION_END)
                : "";

            // parse the date part
            final Matcher dateMatcher = DATE_PATTERN.matcher(datePart);
            if (dateMatcher.matches()) {
                final int yy = Integer.parseInt(dateMatcher.group(1).trim());
                final int mm = Integer.parseInt(dateMatcher.group(2).trim());
                final int dd = Integer.parseInt(dateMatcher.group(3).trim());
                final int yyyy = (yy > SEVENTY) ? yy + NINETEENTH : yy + TWENTIETH;
                date = Integer.parseInt(dateMatcher.group(4).trim());
                final int reconstructedDate = new DateComponents(yyyy, mm, dd).getMJD();
                if (reconstructedDate != date) {
                    this.notifyUnexpectedErrorEncountered(name);
                }
            } else {
                this.notifyUnexpectedErrorEncountered(name);
            }

            // parse the pole part
            if (polePart.trim().isEmpty()) {
                // pole part is blank
                x = 0;
                y = 0;
            } else {
                final Matcher poleMatcher = POLE_PATTERN.matcher(polePart);
                if (poleMatcher.matches()) {
                    x = ARC_SECONDS_TO_RADIANS * Double.parseDouble(poleMatcher.group(1));
                    y = ARC_SECONDS_TO_RADIANS * Double.parseDouble(poleMatcher.group(3));
                } else {
                    this.notifyUnexpectedErrorEncountered(name);
                }
            }

            // parse the UT1-UTC part
            dtu1 = parseDoublePart(ut1utcPart, name, UT1_UTC_PATTERN, 1.0);

            // parse the lod part
            lod = parseDoublePart(lodPart, name, LOD_PATTERN, MILLI_SECONDS_TO_SECONDS);

            // parse the nutation part
            if (nutationPart.trim().isEmpty()) {
                // nutation part is blank
                nutationDX = 0;
                nutationDY = 0;
            } else {
                final Matcher nutationMatcher = NUTATION_PATTERN.matcher(nutationPart);
                if (nutationMatcher.matches()) {
                    nutationDX = MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(1));
                    nutationDY = MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(3));
                } else {
                    this.notifyUnexpectedErrorEncountered(name);
                }
            }

            synchronized (this) {
                if (this.history1980 != null) {
                    this.history1980.addEntry(new EOP1980Entry(date, dtu1, lod, x, y, nutationDX, nutationDY));
                }

                if (this.history2000 != null) {
                    this.history2000.addEntry(new EOP2000Entry(date, dtu1, lod, x, y, nutationDX, nutationDY));
                }
            }

        }
    }

    /**
     * internal method to reduce length method - parse double from String
     * 
     * @param paramString to parse
     * @param name name of the file (or zip entry)
     * @param pattern to match the string part of the line
     * @param factor used to multiply with double parsed
     * @return result
     * @throws PatriusException if some data is missing or if some loader specific error occurs
     */
    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    private double parseDoublePart(final String paramString, final String name, final Pattern pattern,
                                   final double factor) throws PatriusException {

        double result = 0;
        // parse the param part
        if (paramString.trim().isEmpty()) {
            // param part is blank
            result = 0;
        } else {
            // regex Matcher
            final Matcher partMatcher = pattern.matcher(paramString);
            if (partMatcher.matches()) {
                result = factor * Double.parseDouble(partMatcher.group(1));
            } else {
                this.notifyUnexpectedErrorEncountered(name);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void fillHistory(final EOP1980History history) throws PatriusException {
        synchronized (this) {
            this.history1980 = history;
            this.history2000 = null;
            DataProvidersManager.getInstance().feed(this.supportedNames, this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fillHistory(final EOP2000History history) throws PatriusException {
        synchronized (this) {
            this.history1980 = null;
            this.history2000 = history;
            DataProvidersManager.getInstance().feed(this.supportedNames, this);
        }
    }

    /**
     * Load celestial body.
     * 
     * @param history
     *        history to fill up
     * @param istream
     *        input data stream
     * @throws PatriusException
     *         if the history cannot be loaded
     * @throws IOException
     *         if data can't be read
     */
    public void fillHistory(final EOP2000History history,
                            final InputStream istream) throws PatriusException, IOException {
        synchronized (history) {
            this.history1980 = null;
            this.history2000 = history;
            loadDataIn(istream, "");
        }
    }

    /**
     * Throw an exception for an unexpected format error.
     * 
     * @param name
     *        name of the file (or zip entry)
     * @exception PatriusException
     *            always thrown to notify an unexpected error has been
     *            encountered by the caller
     */
    private void notifyUnexpectedErrorEncountered(final String name) throws PatriusException {
        String loaderName = this.getClass().getName();
        loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
        throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
            name, loaderName);
    }

}
