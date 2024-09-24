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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.tle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.ChronologicalComparator;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class reads and handles series of TLEs for one space object.
 * <p>
 * TLE data is read using the standard Orekit mechanism based on a configured {@link DataProvidersManager
 * DataProvidersManager}. This means TLE data may be retrieved from many different storage media (local disk files,
 * remote servers, database ...).
 * </p>
 * <p>
 * This class provides bounded ephemerides by finding the best initial TLE to propagate and then handling the
 * propagation.
 * </p>
 * 
 * @see TLE
 * @see DataProvidersManager
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class TLESeries implements DataLoader, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -1120722267423537022L;

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = ".*\\.tle$";

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Available satellite numbers. */
    private final Set<Integer> availableSatNums;

    /** Set containing all TLE entries. */
    private final SortedSet<TimeStamped> tles;

    /** Satellite number used for filtering. */
    private int filterSatelliteNumber;

    /** Launch year used for filtering (all digits). */
    private int filterLaunchYear;

    /** Launch number used for filtering. */
    private int filterLaunchNumber;

    /** Launch piece used for filtering. */
    private String filterLaunchPiece;

    /** Previous TLE in the cached selection. */
    private TLE previous;

    /** Next TLE in the cached selection. */
    private TLE next;

    /** Last used TLE. */
    private TLE lastTLE;

    /** Associated propagator. */
    private TLEPropagator lastPropagator;

    /** Date of the first TLE. */
    private AbsoluteDate firstDate;

    /** Date of the last TLE. */
    private AbsoluteDate lastDate;

    /** Indicator for non-TLE extra lines. */
    private final boolean ignoreNonTLELines;

    /**
     * Simple constructor with a TLE file.
     * <p>
     * This constructor does not load any data by itself. Data must be loaded later on by calling one of the
     * {@link #loadTLEData()
     * loadTLEData()} method, the {@link #loadTLEData(int)
     * loadTLEData(filterSatelliteNumber)} method or the {@link #loadTLEData(int, int, String)
     * loadTLEData(filterLaunchYear, filterLaunchNumber, filterLaunchPiece)} method.
     * <p>
     * 
     * @param supportedNamesIn
     *        regular expression for supported files names
     *        (if null, a default pattern matching files with a ".tle" extension will be used)
     * @param ignoreNonTLELinesIn
     *        if true, extra non-TLE lines are silently ignored,
     *        if false an exception will be generated when such lines are encountered
     * @see #loadTLEData()
     * @see #loadTLEData(int)
     * @see #loadTLEData(int, int, String)
     */
    public TLESeries(final String supportedNamesIn, final boolean ignoreNonTLELinesIn) {

        this.supportedNames = (supportedNamesIn == null) ? DEFAULT_SUPPORTED_NAMES : supportedNamesIn;
        this.availableSatNums = new TreeSet<>();
        this.ignoreNonTLELines = ignoreNonTLELinesIn;
        this.filterSatelliteNumber = -1;
        this.filterLaunchYear = -1;
        this.filterLaunchNumber = -1;
        this.filterLaunchPiece = null;

        this.tles = new TreeSet<>(new ChronologicalComparator());
        this.previous = null;
        this.next = null;

    }

    /**
     * Load TLE data for a specified object.
     * <p>
     * The TLE data already loaded in the instance will be discarded and replaced by the newly loaded data.
     * </p>
     * <p>
     * The filtering values will be automatically set to the first loaded satellite. This feature is useful when the
     * satellite selection is already set up by either the instance configuration (supported file names) or by the
     * {@link DataProvidersManager data providers manager} configuration and the local filtering feature provided here
     * can be ignored.
     * </p>
     * 
     * @exception PatriusException
     *            if some data can't be read, some
     *            file content is corrupted or no TLE data is available
     * @see #loadTLEData(int)
     * @see #loadTLEData(int, int, String)
     */
    public void loadTLEData() throws PatriusException {

        this.availableSatNums.clear();

        // set the filtering parameters
        this.filterSatelliteNumber = -1;
        this.filterLaunchYear = -1;
        this.filterLaunchNumber = -1;
        this.filterLaunchPiece = null;

        // load the data from the configured data providers
        this.tles.clear();
        DataProvidersManager.getInstance().feed(this.supportedNames, this);
        if (this.tles.isEmpty()) {
            throw new PatriusException(PatriusMessages.NO_TLE_DATA_AVAILABLE);
        }

    }

    /**
     * Get the available satellite numbers.
     * 
     * @return available satellite numbers
     * @throws PatriusException
     *         if some data can't be read, some
     *         file content is corrupted or no TLE data is available
     */
    public Set<Integer> getAvailableSatelliteNumbers() throws PatriusException {
        if (this.availableSatNums.isEmpty()) {
            this.loadTLEData();
        }
        return this.availableSatNums;
    }

    /**
     * Load TLE data for a specified object.
     * <p>
     * The TLE data already loaded in the instance will be discarded and replaced by the newly loaded data.
     * </p>
     * <p>
     * Calling this method with the satellite number set to a negative value, is equivalent to call
     * {@link #loadTLEData()}.
     * </p>
     * 
     * @param satelliteNumber
     *        satellite number
     * @exception PatriusException
     *            if some data can't be read, some
     *            file content is corrupted or no TLE data is available for the selected object
     * @see #loadTLEData()
     * @see #loadTLEData(int, int, String)
     */
    public void loadTLEData(final int satelliteNumber) throws PatriusException {

        if (satelliteNumber < 0) {
            // no filtering at all
            this.loadTLEData();
        } else {
            // set the filtering parameters
            this.filterSatelliteNumber = satelliteNumber;
            this.filterLaunchYear = -1;
            this.filterLaunchNumber = -1;
            this.filterLaunchPiece = null;

            // load the data from the configured data providers
            this.tles.clear();
            DataProvidersManager.getInstance().feed(this.supportedNames, this);
            if (this.tles.isEmpty()) {
                throw new PatriusException(PatriusMessages.NO_TLE_FOR_OBJECT, satelliteNumber);
            }
        }

    }

    /**
     * Load TLE data for a specified object.
     * <p>
     * The TLE data already loaded in the instance will be discarded and replaced by the newly loaded data.
     * </p>
     * <p>
     * Calling this method with either the launch year or the launch number set to a negative value, or the launch piece
     * set to null or an empty string are all equivalent to call {@link #loadTLEData()}.
     * </p>
     * 
     * @param launchYear
     *        launch year (all digits)
     * @param launchNumber
     *        launch number
     * @param launchPiece
     *        launch piece
     * @exception PatriusException
     *            if some data can't be read, some
     *            file content is corrupted or no TLE data is available for the selected object
     * @see #loadTLEData()
     * @see #loadTLEData(int)
     */
    public void loadTLEData(final int launchYear, final int launchNumber,
                            final String launchPiece) throws PatriusException {

        if ((launchYear < 0) || (launchNumber < 0) ||
            (launchPiece == null) || (launchPiece.length() == 0)) {
            // no filtering at all
            this.loadTLEData();
        } else {
            // set the filtering parameters
            this.filterSatelliteNumber = -1;
            this.filterLaunchYear = launchYear;
            this.filterLaunchNumber = launchNumber;
            this.filterLaunchPiece = launchPiece;

            // load the data from the configured data providers
            this.tles.clear();
            DataProvidersManager.getInstance().feed(this.supportedNames, this);
            if (this.tles.isEmpty()) {
                throw new PatriusException(PatriusMessages.NO_TLE_FOR_LAUNCH_YEAR_NUMBER_PIECE,
                    launchYear, launchNumber, launchPiece);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return this.tles.isEmpty();
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        try {

            int lineNumber = 0;
            String pendingLine = null;
            for (String line = r.readLine(); line != null; line = r.readLine()) {

                ++lineNumber;

                if (pendingLine == null) {

                    // we must wait for the second line
                    pendingLine = line;

                } else {

                    // safety checks
                    if (!TLE.isFormatOK(pendingLine, line)) {
                        if (this.ignoreNonTLELines) {
                            // just shift one line
                            pendingLine = line;
                            continue;
                        }
                        throw new PatriusException(PatriusMessages.NOT_TLE_LINES,
                            lineNumber - 1, lineNumber, pendingLine, line);
                    }

                    final TLE tle = new TLE(pendingLine, line);

                    if (this.filterSatelliteNumber < 0) {
                        if ((this.filterLaunchYear < 0) ||
                            ((tle.getLaunchYear() == this.filterLaunchYear) &&
                                (tle.getLaunchNumber() == this.filterLaunchNumber) &&
                            tle.getLaunchPiece().equals(this.filterLaunchPiece))) {
                            // we now know the number of the object to load
                            this.filterSatelliteNumber = tle.getSatelliteNumber();
                        }
                    }

                    this.availableSatNums.add(tle.getSatelliteNumber());

                    if (tle.getSatelliteNumber() == this.filterSatelliteNumber) {
                        // accept this TLE
                        this.tles.add(tle);
                    }

                    // we need to wait for two new lines
                    pendingLine = null;

                }

            }

            if ((pendingLine != null) && !this.ignoreNonTLELines) {
                // there is an unexpected last line
                throw new PatriusException(PatriusMessages.MISSING_SECOND_TLE_LINE,
                    lineNumber, pendingLine);
            }

        } finally {
            r.close();
        }

    }

    /**
     * Get the extrapolated position and velocity from an initial date.
     * For a good precision, this date should not be too far from the range :
     * [{@link #getFirstDate() first date} ; {@link #getLastDate() last date}].
     * 
     * @param date
     *        the final date
     * @return the final PVCoordinates
     * @exception PatriusException
     *            if the underlying propagator cannot be initialized
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date) throws PatriusException {
        final TLE toExtrapolate = this.getClosestTLE(date);
        if (toExtrapolate != this.lastTLE) {
            this.lastTLE = toExtrapolate;
            this.lastPropagator = TLEPropagator.selectExtrapolator(this.lastTLE);
        }
        return this.lastPropagator.getPVCoordinates(date);
    }

    /**
     * Get the closest TLE to the selected date.
     * 
     * @param date
     *        the date
     * @return the TLE that will suit the most for propagation.
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Orekit code kept as such
    public TLE getClosestTLE(final AbsoluteDate date) {
        // CHECKSTYLE: resume ReturnCount check

        // don't search if the cached selection is fine
        if ((this.previous != null) && (date.durationFrom(this.previous.getDate()) >= 0) &&
            (this.next != null) && (date.durationFrom(this.next.getDate()) <= 0)) {
            // the current selection is already good
            if (this.next.getDate().durationFrom(date) > date.durationFrom(this.previous.getDate())) {
                return this.previous;
            }
            return this.next;
        }

        // reset the selection before the search phase
        this.previous = null;
        this.next = null;
        final SortedSet<TimeStamped> headSet = this.tles.headSet(date);
        final SortedSet<TimeStamped> tailSet = this.tles.tailSet(date);

        if (headSet.isEmpty()) {
            return (TLE) tailSet.first();
        }
        if (tailSet.isEmpty()) {
            return (TLE) headSet.last();
        }
        this.previous = (TLE) headSet.last();
        this.next = (TLE) tailSet.first();

        if (this.next.getDate().durationFrom(date) > date.durationFrom(this.previous.getDate())) {
            return this.previous;
        }
        return this.next;
    }

    /**
     * Get the start date of the series.
     * 
     * @return the first date
     */
    public AbsoluteDate getFirstDate() {
        if (this.firstDate == null) {
            this.firstDate = this.tles.first().getDate();
        }
        return this.firstDate;
    }

    /**
     * Get the last date of the series.
     * 
     * @return the end date
     */
    public AbsoluteDate getLastDate() {
        if (this.lastDate == null) {
            this.lastDate = this.tles.last().getDate();
        }
        return this.lastDate;
    }

    /**
     * Get the first TLE.
     * 
     * @return first TLE
     */
    public TLE getFirst() {
        return (TLE) this.tles.first();
    }

    /**
     * Get the last TLE.
     * 
     * @return last TLE
     */
    public TLE getLast() {
        return (TLE) this.tles.last();
    }

}
