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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.forces.atmospheres.DTMInputParameters;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.ChronologicalComparator;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.Month;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class reads and provides solar activity data needed by
 * atmospheric models: F107 solar flux and Kp indexes.
 * <p>
 * The data are retrieved through the NASA Marshall Solar Activity Future Estimation (MSAFE) as estimates of monthly
 * F10.7 Mean solar flux and Ap geomagnetic parameter. The data can be retrieved at the NASA <a
 * href="http://sail.msfc.nasa.gov/archive_index.htm"> Marshall Solar Activity website</a>. Here Kp indices are deduced
 * from Ap indexes, which in turn are tabulated equivalent of retrieved Ap values.
 * </p>
 * <p>
 * If several MSAFE files are available, some dates may appear in several files (for example August 2007 is in all files
 * from the first one published in March 1999 to the February 2008 file). In this case, the data from the most recent
 * file is used and the older ones are discarded. The date of the file is assumed to be 6 months after its first entry
 * (which explains why the file having August 2007 as its first entry is the February 2008 file). This implies that
 * MSAFE files must <em>not</em> be edited to change their time span, otherwise this would break the old entries
 * overriding mechanism.
 * </p>
 * <p>
 * With these data, the {@link #getInstantFlux(AbsoluteDate)} and {@link #getMeanFlux(AbsoluteDate)} methods return the
 * same values and the {@link #get24HoursKp(AbsoluteDate)} and {@link #getThreeHourlyKP(AbsoluteDate)} methods return
 * the same values.
 * </p>
 * 
 * @author Bruno Revelin
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class MarshallSolarActivityFutureEstimation implements DTMInputParameters, DataLoader {

    /** Strength level of activity. */
    public static enum StrengthLevel {

        /** Strong level of activity. */
        STRONG,

        /** Average level of activity. */
        AVERAGE,

        /** Weak level of activity. */
        WEAK

    }

    /** Serializable UID. */
    private static final long serialVersionUID = -5212198874900835369L;

    /** 1/3. */
    private static final double ONE_THIRD = 1.0 / 3.0;

    /** 3 hours geomagnetic indices array. */
    private static final double[] KP_ARRAY = new double[] {
        0, 0 + ONE_THIRD, 1 - ONE_THIRD, 1, 1 + ONE_THIRD, 2 - ONE_THIRD,
        2, 2 + ONE_THIRD, 3 - ONE_THIRD, 3, 3 + ONE_THIRD, 4 - ONE_THIRD,
        4, 4 + ONE_THIRD, 5 - ONE_THIRD, 5, 5 + ONE_THIRD, 6 - ONE_THIRD,
        6, 6 + ONE_THIRD, 7 - ONE_THIRD, 7, 7 + ONE_THIRD, 8 - ONE_THIRD,
        8, 8 + ONE_THIRD, 9 - ONE_THIRD, 9
    };

    /** Mean geomagnetic indices array. */
    private static final double[] AP_ARRAY = new double[] {
        0, 2, 3, 4, 5, 6, 7, 9, 12, 15, 18, 22, 27, 32,
        39, 48, 56, 67, 80, 94, 111, 132, 154, 179, 207, 236, 300, 400
    };

    /** 7 */
    private static final int C_7 = 7;

    /** 8 */
    private static final int C_8 = 8;

    /** Pattern for the data fields of MSAFE data. */
    private final Pattern dataPattern;

    /** Data set. */
    private final SortedSet<TimeStamped> data;

    /** Selected strength level of activity. */
    private final StrengthLevel strengthLevel;

    /** First available date. */
    private AbsoluteDate firstDate;

    /** Last available date. */
    private AbsoluteDate lastDate;

    /** Previous set of solar activity parameters. */
    private LineParameters previousParam;

    /** Current set of solar activity parameters. */
    private LineParameters currentParam;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /**
     * Simple constructor.
     * <p>
     * The original file names used by NASA Marshall space center are of the form: Dec2010F10.txt or Oct1999F10.TXT. So
     * a recommended regular expression for the supported name that work with all published files is:
     * "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)"
     * </p>
     * 
     * @param supportedNamesIn
     *        regular expression for supported files names
     * @param strengthLevelIn
     *        selected strength level of activity
     */
    public MarshallSolarActivityFutureEstimation(final String supportedNamesIn,
                                                 final StrengthLevel strengthLevelIn) {

        this.firstDate = null;
        this.lastDate = null;
        this.data = new TreeSet<>(new ChronologicalComparator());
        this.supportedNames = supportedNamesIn;
        this.strengthLevel = strengthLevelIn;

        // the data lines have the following form:
        // 2010.5003 JUL 83.4 81.3 78.7 6.4 5.9 5.2
        // 2010.5837 AUG 87.3 83.4 78.5 7.0 6.1 4.9
        // 2010.6670 SEP 90.8 85.5 79.4 7.8 6.2 4.7
        // 2010.7503 OCT 94.2 87.6 80.4 9.1 6.4 4.9
        final StringBuilder builder = new StringBuilder("^");

        // first group: year
        builder.append("\\p{Blank}*(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})");

        // month as fraction of year, not stored in a group
        builder.append("\\.\\p{Digit}+");

        // second group: month as a three upper case letters abbreviation
        builder.append("\\p{Blank}+(");
        for (final Month month : Month.values()) {
            builder.append(month.getUpperCaseAbbreviation());
            builder.append('|');
        }
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")");

        // third to eighth group: data fields
        for (int i = 0; i < 6; ++i) {
            builder.append("\\p{Blank}+([-+]?[0-9]+\\.[0-9]+)");
        }

        // end of line
        builder.append("\\p{Blank}*$");

        // compile the pattern
        this.dataPattern = Pattern.compile(builder.toString());

    }

    /**
     * Get the strength level for activity.
     * 
     * @return strength level to set
     */
    public StrengthLevel getStrengthLevel() {
        return this.strengthLevel;
    }

    /**
     * Find the data bracketing a specified date.
     * 
     * @param date
     *        date to bracket
     * @throws PatriusException
     *         if specified date is out of range
     */
    private void bracketDate(final AbsoluteDate date) throws PatriusException {

        // raise an exception if the date is outside of the interval
        if ((date.durationFrom(this.firstDate) < 0) || (date.durationFrom(this.lastDate) > 0)) {
            throw new PatriusException(PatriusMessages.OUT_OF_RANGE_EPHEMERIDES_DATE,
                date, this.firstDate, this.lastDate);
        }

        // don't search if the cached selection is fine
        if ((this.previousParam != null) &&
                (date.durationFrom(this.previousParam.getDate()) > 0) &&
                (date.durationFrom(this.currentParam.getDate()) <= 0)) {
            return;
        }
        // update params if the date is equal to the firstDate
        if (date.equals(this.firstDate)) {
            this.currentParam = (LineParameters) this.data.tailSet(date.shiftedBy(1)).first();
            this.previousParam = (LineParameters) this.data.first();
        } else if (date.equals(this.lastDate)) {
            // update params if the date is equal to the LastDate
            this.currentParam = (LineParameters) this.data.last();
            this.previousParam = (LineParameters) this.data.headSet(date.shiftedBy(-1)).last();
        } else {
            this.currentParam = (LineParameters) this.data.tailSet(date).first();
            this.previousParam = (LineParameters) this.data.headSet(date).last();
        }

    }

    /**
     * Get the supported names for data files.
     * 
     * @return regular expression for the supported names for data files
     */
    public String getSupportedNames() {
        return this.supportedNames;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return this.firstDate;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return this.lastDate;
    }

    /** {@inheritDoc} */
    @Override
    public double getInstantFlux(final AbsoluteDate date) throws PatriusException {
        return this.getMeanFlux(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getMeanFlux(final AbsoluteDate date) throws PatriusException {

        // get the neighboring dates
        this.bracketDate(date);

        // perform a linear interpolation
        final AbsoluteDate previousDate = this.previousParam.getDate();
        final AbsoluteDate currentDate = this.currentParam.getDate();
        final double dt = currentDate.durationFrom(previousDate);
        final double previousF107 = this.previousParam.getF107();
        final double currentF107 = this.currentParam.getF107();
        final double previousWeight = currentDate.durationFrom(date) / dt;
        final double currentWeight = date.durationFrom(previousDate) / dt;

        return previousF107 * previousWeight + currentF107 * currentWeight;

    }

    /** {@inheritDoc} */
    @Override
    public double getThreeHourlyKP(final AbsoluteDate date) throws PatriusException {
        return this.get24HoursKp(date);
    }

    /**
     * Get the date of the file from which data at the specified date comes from.
     * <p>
     * If several MSAFE files are available, some dates may appear in several files (for example August 2007 is in all
     * files from the first one published in March 1999 to the February 2008 file). In this case, the data from the most
     * recent file is used and the older ones are discarded. The date of the file is assumed to be 6 months after its
     * first entry (which explains why the file having August 2007 as its first entry is the February 2008 file). This
     * implies that MSAFE files must <em>not</em> be edited to change their time span, otherwise this would break the
     * old entries overriding mechanism.
     * </p>
     * 
     * @param date
     *        date of the solar activity data
     * @return date of the file
     * @exception PatriusException
     *            if specified date is out of range
     */
    public DateComponents getFileDate(final AbsoluteDate date) throws PatriusException {
        this.bracketDate(date);
        final double dtP = date.durationFrom(this.previousParam.getDate());
        final double dtC = this.currentParam.getDate().durationFrom(date);
        return (dtP < dtC) ? this.previousParam.getFileDate() : this.currentParam.getFileDate();
    }

    /**
     * The Kp index is derived from the Ap index.
     * <p>
     * The method used is explained on <a href="http://www.ngdc.noaa.gov/stp/GEOMAG/kp_ap.html"> NOAA website.</a> as
     * follows:
     * </p>
     * <p>
     * The scale is 0 to 9 expressed in thirds of a unit, e.g. 5- is 4 2/3, 5 is 5 and 5+ is 5 1/3. The ap (equivalent
     * range) index is derived from the Kp index as follows:
     * </p>
     * <table border="1">
     * <tbody>
     * <tr>
     * <td>Kp</td>
     * <td>0o</td>
     * <td>0+</td>
     * <td>1-</td>
     * <td>1o</td>
     * <td>1+</td>
     * <td>2-</td>
     * <td>2o</td>
     * <td>2+</td>
     * <td>3-</td>
     * <td>3o</td>
     * <td>3+</td>
     * <td>4-</td>
     * <td>4o</td>
     * <td>4+</td>
     * </tr>
     * <tr>
     * <td>ap</td>
     * <td>0</td>
     * <td>2</td>
     * <td>3</td>
     * <td>4</td>
     * <td>5</td>
     * <td>6</td>
     * <td>7</td>
     * <td>9</td>
     * <td>12</td>
     * <td>15</td>
     * <td>18</td>
     * <td>22</td>
     * <td>27</td>
     * <td>32</td>
     * </tr>
     * <tr>
     * <td>Kp</td>
     * <td>5-</td>
     * <td>5o</td>
     * <td>5+</td>
     * <td>6-</td>
     * <td>6o</td>
     * <td>6+</td>
     * <td>7-</td>
     * <td>7o</td>
     * <td>7+</td>
     * <td>8-</td>
     * <td>8o</td>
     * <td>8+</td>
     * <td>9-</td>
     * <td>9o</td>
     * </tr>
     * <tr>
     * <td>ap</td>
     * <td>39</td>
     * <td>48</td>
     * <td>56</td>
     * <td>67</td>
     * <td>80</td>
     * <td>94</td>
     * <td>111</td>
     * <td>132</td>
     * <td>154</td>
     * <td>179</td>
     * <td>207</td>
     * <td>236</td>
     * <td>300</td>
     * <td>400</td>
     * </tr>
     * </tbody>
     * </table>
     * 
     * @param date
     *        date of the Kp data
     * @return the 24H geomagnetic index
     * @exception PatriusException
     *            if the date is out of range of available data
     */
    @Override
    public double get24HoursKp(final AbsoluteDate date) throws PatriusException {

        // get the neighboring dates
        this.bracketDate(date);

        // perform a linear interpolation
        final AbsoluteDate previousDate = this.previousParam.getDate();
        final AbsoluteDate currentDate = this.currentParam.getDate();
        final double dt = currentDate.durationFrom(previousDate);
        final double previousAp = this.previousParam.getAp();
        final double currentAp = this.currentParam.getAp();
        final double previousWeight = currentDate.durationFrom(date) / dt;
        final double currentWeight = date.durationFrom(previousDate) / dt;
        final double ap = previousAp * previousWeight + currentAp * currentWeight;

        // calculating Ap index, then corresponding Kp index
        final int i = Arrays.binarySearch(AP_ARRAY, ap);
        final double res;
        if (i >= 0) {
            // the exact value for ap has been found, return the corresponding Kp
            res = KP_ARRAY[i];
        } else {
            // the exact value has not been found, we have an insertion point
            final int jSup = -(i + 1);
            final int jInf = jSup - 1;
            if ((ap - AP_ARRAY[jInf]) < (AP_ARRAY[jSup] - ap)) {
                res = KP_ARRAY[jInf];
            } else {
                res = KP_ARRAY[jSup];
            }
        }

        // Return result
        return res;
    }

    /** {@inheritDoc} */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @Override
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // select the groups we want to store
        final int f107Group;
        final int apGroup;
        switch (this.strengthLevel) {
            case STRONG:
                f107Group = 3;
                apGroup = 6;
                break;
            case AVERAGE:
                f107Group = 4;
                apGroup = C_7;
                break;
            default:
                f107Group = 5;
                apGroup = C_8;
                break;
        }

        // read the data
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        boolean inData = false;
        final TimeScale utc = TimeScalesFactory.getUTC();
        DateComponents fileDate = null;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            final String line2 = line.trim();
            if (line2.length() > 0) {
                final Matcher matcher = this.dataPattern.matcher(line2);
                if (matcher.matches()) {

                    // we are in the data section
                    inData = true;

                    // extract the data from the line
                    final int year = Integer.parseInt(matcher.group(1));
                    final Month month = Month.parseMonth(matcher.group(2));
                    final AbsoluteDate date = new AbsoluteDate(year, month, 1, utc);
                    if (fileDate == null) {
                        // the first entry of each file correspond exactly to 6 months before file publication
                        // so we compute the file date by adding 6 months to its first entry
                        if (month.getNumber() > 6) {
                            fileDate = new DateComponents(year + 1, month.getNumber() - 6, 1);
                        } else {
                            fileDate = new DateComponents(year, month.getNumber() + 6, 1);
                        }
                    }

                    // check if there is already an entry for this date or not
                    boolean addEntry = false;
                    final Iterator<TimeStamped> iterator = this.data.tailSet(date).iterator();
                    if (iterator.hasNext()) {
                        final LineParameters existingEntry = (LineParameters) iterator.next();
                        if (existingEntry.getDate().equals(date)) {
                            // there is an entry for this date
                            if (existingEntry.getFileDate().compareTo(fileDate) < 0) {
                                // the entry was read from an earlier file
                                // we replace it with the new entry as it is fresher
                                iterator.remove();
                                addEntry = true;
                            }
                        } else {
                            // it is the first entry we get for this date
                            addEntry = true;
                        }
                    } else {
                        // it is the first entry we get for this date
                        addEntry = true;
                    }
                    if (addEntry) {
                        // we must add the new entry
                        this.data.add(new LineParameters(fileDate, date,
                            Double.parseDouble(matcher.group(f107Group)),
                            Double.parseDouble(matcher.group(apGroup))));
                    }

                } else {
                    if (inData) {
                        // we have already read some data, so we are not in the header anymore
                        // however, we don't recognize this non-empty line,
                        // we consider the file is corrupted
                        throw new PatriusException(
                            PatriusMessages.NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE,
                            name);
                    }
                }
            }
        }

        if (this.data.isEmpty()) {
            throw new PatriusException(PatriusMessages.NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE,
                name);
        }
        this.firstDate = this.data.first().getDate();
        this.lastDate = this.data.last().getDate();

    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        if (start.compareTo(getMinDate()) < 0) {
            throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, start, getMinDate(), getMaxDate());
        }
        if (end.compareTo(getMaxDate()) > 0) {
            throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, end, getMinDate(), getMaxDate());
        }
    }

    /** Container class for Solar activity indexes. */
    private static final class LineParameters implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 6607862001953526475L;

        /** File date. */
        private final DateComponents fileDate;

        /** Entry date. */
        private final AbsoluteDate date;

        /** F10.7 flux at date. */
        private final double f107;

        /** Ap index at date. */
        private final double ap;

        /**
         * Simple constructor.
         * 
         * @param fileDateIn
         *        file date
         * @param dateIn
         *        entry date
         * @param f107In
         *        F10.7 flux at date
         * @param apIn
         *        Ap index at date
         */
        private LineParameters(final DateComponents fileDateIn, final AbsoluteDate dateIn, final double f107In,
                               final double apIn) {
            this.fileDate = fileDateIn;
            this.date = dateIn;
            this.f107 = f107In;
            this.ap = apIn;
        }

        /**
         * Get the file date.
         * 
         * @return file date
         */
        public DateComponents getFileDate() {
            return this.fileDate;
        }

        /**
         * Get the current date.
         * 
         * @return current date
         */
        @Override
        public AbsoluteDate getDate() {
            return this.date;
        }

        /**
         * Get the F10.0 flux.
         * 
         * @return f10.7 flux
         */
        public double getF107() {
            return this.f107;
        }

        /**
         * Get the Ap index.
         * 
         * @return Ap index
         */
        public double getAp() {
            return this.ap;
        }

    }

}
