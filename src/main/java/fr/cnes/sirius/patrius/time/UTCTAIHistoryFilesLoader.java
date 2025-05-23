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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1448:20/04/2018:PATRIUS 4.0 minor corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for UTC versus TAI history files.
 * <p>
 * UTC versus TAI history files contain {@link UTCTAIOffset
 * leap seconds} data since.
 * </p>
 * <p>
 * The UTC versus TAI history files are recognized thanks to their base names, which must match the pattern
 * <code>UTC-TAI.history</code> (or <code>UTC-TAI.history.gz</code> for gzip-compressed files)
 * </p>
 * <p>
 * Only one history file must be present in the IERS directories hierarchy.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class UTCTAIHistoryFilesLoader implements UTCTAILoader {

    /** Supported files name pattern. */
    private static final String SUPPORTED_NAMES = "^UTC-TAI\\.history$";

    /** Regular data lines pattern. */
    private final Pattern regularPattern;

    /** Last line pattern pattern. */
    private final Pattern lastPattern;

    /** Time scales offsets. */
    private final SortedMap<DateComponents, Integer> entries;

    /** Build a loader for UTC-TAI history file. */
    public UTCTAIHistoryFilesLoader() {

        // the data lines in the UTC time steps data files have the following form:
        // 1966 Jan. 1 - 1968 Feb. 1 4.313 170 0s + (MJD - 39 126) x 0.002 592s
        // 1968 Feb. 1 - 1972 Jan. 1 4.213 170 0s + ""
        // 1972 Jan. 1 - Jul. 1 10s
        // Jul. 1 - 1973 Jan. 1 11s
        // 1973 Jan. 1 - 1974 Jan. 1 12s
        // ...
        // 2006 Jan. 1.- 2009 Jan. 1 33s
        // 2009 Jan. 1.- 2012 Jul 1 34s
        // 2012 Jul 1 - 35s

        // we ignore the non-constant and non integer offsets before 1972-01-01
        final String start = "^";

        // year group
        final String yearField = "\\p{Blank}*((?:\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})|(?:    ))";

        // second group: month as a three letters capitalized abbreviation
        final StringBuilder builder = new StringBuilder("\\p{Blank}+(");
        for (final Month month : Month.values()) {
            builder.append(month.getCapitalizedAbbreviation());
            builder.append('|');
        }
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")\\.?");
        final String monthField = builder.toString();

        // day group
        final String dayField = "\\p{Blank}+([ 0-9]+)\\.?";

        // offset group
        final String offsetField = "\\p{Blank}+(\\p{Digit}+)s";

        final String separator = "\\p{Blank}*-\\p{Blank}+";
        final String finalBlanks = "\\p{Blank}*$";
        this.regularPattern = Pattern.compile(start + yearField + monthField + dayField +
            separator + yearField + monthField + dayField +
            offsetField + finalBlanks);
        this.lastPattern = Pattern.compile(start + yearField + monthField + dayField +
            separator + offsetField + finalBlanks);

        this.entries = new TreeMap<>();
    }

    /**
     * Get the regular expression for supported files names.
     * 
     * @return regular expression for supported files names
     */
    @Override
    public String getSupportedNames() {
        return SUPPORTED_NAMES;
    }

    /**
     * Load stored UTC-TAI offsets entries.
     * 
     * @return sorted UTC-TAI offsets entries (may be empty if no data file is available)
     */
    @Override
    public SortedMap<DateComponents, Integer> loadTimeSteps() {
        return this.entries;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return (this.entries == null) || this.entries.isEmpty();
    }

    /**
     * Load UTC-TAI offsets entries read from some file.
     * <p>
     * The time steps are extracted from some <code>UTC-TAI.history[.gz]</code> file. Since entries are stored in a
     * {@link java.util.SortedMap SortedMap}, they are chronologically sorted and only one entry remains for a given
     * date.
     * </p>
     * 
     * @param input
     *        data input stream
     * @param name
     *        name of the file (or zip entry)
     * @exception IOException
     *            if data can't be read
     * @exception ParseException
     *            if data can't be parsed
     * @exception PatriusException
     *            if some data is missing
     *            or if some loader specific error occurs
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    public void loadData(final InputStream input,
                         final String name) throws PatriusException, IOException, ParseException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        this.entries.clear();

        // set up a reader for line-oriented file
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        // read all file, ignoring not recognized lines
        boolean foundEntries = false;
        final String emptyYear = "    ";
        int lineNumber = 0;
        DateComponents lastDate = null;
        int lastLine = 0;
        String previousYear = emptyYear;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            // Loop on lines
            ++lineNumber;

            // check matching for regular lines and last line
            Matcher matcher = this.regularPattern.matcher(line);
            if (matcher.matches()) {
                if (lastLine > 0) {
                    // Exception
                    throw new PatriusException(PatriusMessages.UNEXPECTED_DATA_AFTER_LINE_IN_FILE,
                        lastLine, name, line);
                }
            } else {
                matcher = this.lastPattern.matcher(line);
                if (matcher.matches()) {
                    // this is the last line (there is a start date but no end date)
                    lastLine = lineNumber;
                }
            }

            if (matcher.matches()) {
                try {
                    // build an entry from the extracted fields

                    String year = matcher.group(1);
                    if (emptyYear.equals(year)) {
                        year = previousYear;
                    }
                    if (lineNumber != lastLine) {
                        if (emptyYear.equals(matcher.group(4))) {
                            previousYear = year;
                        } else {
                            previousYear = matcher.group(4);
                        }
                    }
                    final DateComponents leapDay = new DateComponents(Integer.parseInt(year.trim()),
                        Month.parseMonth(matcher.group(2)),
                        Integer.parseInt(matcher.group(3).trim()));

                    if ((lastDate != null) && leapDay.compareTo(lastDate) <= 0) {
                        throw new PatriusException(PatriusMessages.NON_CHRONOLOGICAL_DATES_IN_FILE,
                            name, lineNumber);
                    }
                    final Integer offset = Integer.valueOf(matcher.group(matcher.groupCount()));
                    lastDate = leapDay;
                    foundEntries = true;
                    this.entries.put(leapDay, offset);

                } catch (final NumberFormatException nfe) {
                    // Exception
                    throw new PatriusException(nfe, PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                        lineNumber, name, line);
                }
            }
        }

        if (!foundEntries) {
            // Exception
            throw new PatriusException(PatriusMessages.NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE, name);
        }
    }
}
