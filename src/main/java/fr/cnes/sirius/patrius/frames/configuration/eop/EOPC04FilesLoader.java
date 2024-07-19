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
 * VERSION:4.5:FA:FA-2244:27/05/2020:Evolution de la prise en compte des fichiers EOP IERS
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
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for EOP 05 C04 files.
 * <p>
 * EOP 05 C04 files contain {@link EOPEntry
 * Earth Orientation Parameters} consistent with ITRF2005 for one year periods.
 * </p>
 * <p>
 * The EOP 05 C04 files are recognized thanks to their base names, which must match one of the the patterns
 * <code>eopc04_IAU2000.##</code> or <code>eopc04.##</code> (or the same ending with <code>.gz</code> for
 * gzip-compressed files) where # stands for a digit character.
 * </p>
 * <p>
 * Between 2002 and 2007, another series of Earth Orientation Parameters was in use: EOPC04 (without the 05). These
 * parameters were consistent with the previous ITRS realization: ITRF2000. These files are no longer provided by IERS
 * and only 6 files covering the range 2002 to 2007 were generated. The content of these files is not the same as the
 * content of the new files supported by this class, however IERS uses the same file naming convention for both. If a
 * file from the older series is found by this class, a parse error will be triggered. Users must remove such files to
 * avoid being lured in believing they do have EOP data.
 * </p>
 * <p>
 * Files containing old data (back to 1962) have been regenerated in the new file format and are available at IERS web
 * site: <a href="http://hpiers.obspm.fr/iers/eop/eopc04_05/">Index of /iers/eop/eopc04_05</a>.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class EOPC04FilesLoader implements EOP1980HistoryLoader, EOP2000HistoryLoader {

    /** Year field. */
    private static final int YEAR_FIELD = 0;

    /** Month field. */
    private static final int MONTH_FIELD = 1;

    /** Day field. */
    private static final int DAY_FIELD = 2;

    /** MJD field. */
    private static final int MJD_FIELD = 3;

    /** X component of pole motion field. */
    private static final int POLE_X_FIELD = 4;

    /** Y component of pole motion field. */
    private static final int POLE_Y_FIELD = 5;

    /** UT1-UTC field. */
    private static final int UT1_UTC_FIELD = 6;

    /** LoD field. */
    private static final int LOD_FIELD = 7;

    /** Correction for nutation in obliquity field. */
    private static final int DDEPS_FIELD = 8;

    /** Correction for nutation in longitude field. */
    private static final int DDPSI_FIELD = 9;

    /** Correction for nutation dx. */
    private static final int DX_FIELD = 8;

    /** Correction for nutation dy. */
    private static final int DY_FIELD = 9;

    /**
     * Pattern for data lines.<br>
     * The data lines in the EOP 05 C04 yearly data files have the following fixed form:<br>
     * year month day MJD ...12 floating values fields in decimal format...<br>
     * 2000 1 1 51544 0.043157 0.377872 0.3555456 ...<br>
     * 2000 1 2 51545 0.043475 0.377738 0.3547352 ...<br>
     * 2000 1 3 51546 0.043627 0.377507 0.3538988 ...<br>
     * the corresponding fortran format is:<br>
     * 3(I4),I7,2(F11.6),2(F12.7),2(F12.6),2(F11.6),2(F12.7),2F12.6
     */
    private static final Pattern LINEPATTERN = Pattern.compile("^\\d+ +\\d+ +\\d+ +\\d+(?: +-?\\d+\\.\\d+){12}$");

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** History entries for IAU1980. */
    private EOP1980History history1980;

    /** History entries for IAU2000. */
    private EOP2000History history2000;

    /**
     * Build a loader for IERS EOP 05 C04 files.
     * 
     * @param supportedNamesIn
     *        regular expression for supported files names
     */
    public EOPC04FilesLoader(final String supportedNamesIn) {

        this.supportedNames = supportedNamesIn;

    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, PatriusException {

        // read all file, ignoring header
        synchronized (this) {
            loadDataIn(input, name, this.history1980, this.history2000);
        }

    }

    /**
     * Loads the data in a static manner.
     * 
     * @param input
     *        input stream
     * @param name
     *        name of the Orekit data loader resource
     * @param history1980
     *        output as a EOP1980History object, may be null if output not needed
     * @param history2000
     *        output as a EOP2000History object, may be null if output not needed
     * @throws IOException
     *         if data can't be read
     * @throws PatriusException
     *         if some data is missing
     */
    private static void loadDataIn(final InputStream input, final String name, final EOP1980History history1980,
                                   final EOP2000History history2000) throws IOException, PatriusException {

        // set up a reader for line-oriented bulletin B files
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));

        // read all file, ignoring header
        int lineNumber = 0;
        boolean inHeader = true;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            ++lineNumber;
            boolean parsed = false;

            if (LINEPATTERN.matcher(line).matches()) {
                // This is not header
                inHeader = false;
                // this is a data line, build an entry from the extracted fields
                final String[] fields = line.split(" +");
                final int year = Integer.parseInt(fields[YEAR_FIELD]);
                final int month = Integer.parseInt(fields[MONTH_FIELD]);
                final int day = Integer.parseInt(fields[DAY_FIELD]);
                final int mjd = Integer.parseInt(fields[MJD_FIELD]);
                if (new DateComponents(year, month, day).getMJD() != mjd) {
                    throw new PatriusException(PatriusMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                        name, year, month, day, mjd);
                }

                // the first six fields are consistent with the expected format
                final double x = Double.parseDouble(fields[POLE_X_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                final double y = Double.parseDouble(fields[POLE_Y_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                final double dtu1 = Double.parseDouble(fields[UT1_UTC_FIELD]);
                final double lod = Double.parseDouble(fields[LOD_FIELD]);
                if (history1980 != null) {
                    // EOP 1980
                    final double dpsi = Double.parseDouble(fields[DDPSI_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double deps = Double.parseDouble(fields[DDEPS_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    history1980.addEntry(new EOP1980Entry(mjd, dtu1, lod, x, y, dpsi, deps));
                }
                if (history2000 != null) {
                    // EOP 2000
                    final double dx = Double.parseDouble(fields[DX_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double dy = Double.parseDouble(fields[DY_FIELD]) * Constants.ARC_SECONDS_TO_RADIANS;
                    history2000.addEntry(new EOP2000Entry(mjd, dtu1, lod, x, y, dx, dy));
                }
                parsed = true;

            }
            if (!(inHeader || parsed)) {
                // Parse exception
                throw new PatriusException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                    lineNumber, name, line);
            }
        }

        // check if we have read something
        if (inHeader) {
            throw new PatriusException(PatriusMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
        }

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
     * Fills the history object directy with data from the {@link InputStream}, bypassing the Orekit data loaders.
     * 
     * @param history
     *        history object
     * @param istream
     *        input stream
     * @throws PatriusException
     *         if some data is missing
     * @throws IOException
     *         if data can't be read
     */
    public static void fillHistory(final EOP1980History history,
                                   final InputStream istream) throws PatriusException, IOException {
        synchronized (history) {
            // Loads directy from the input stream
            loadDataIn(istream, "", history, null);
        }
    }

    /**
     * Fills the history object directy with data from the {@link InputStream}, bypassing the Orekit data loaders.
     * 
     * @param history
     *        history object
     * @param istream
     *        input stream
     * @throws PatriusException
     *         if some data is missing
     * @throws IOException
     *         if data can't be read
     */
    public static void fillHistory(final EOP2000History history,
                                   final InputStream istream) throws PatriusException, IOException {
        synchronized (history) {
            // Loads directy from the input stream
            loadDataIn(istream, "", null, history);
        }
    }

}
