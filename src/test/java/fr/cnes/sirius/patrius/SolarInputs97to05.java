/**
 * Copyright 2011-2022 CNES
 * Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:DM:DM-2967:15/11/2021:[PATRIUS] corriger les utilisations de java.util.Date 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.forces.atmospheres.DTMInputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.JB2006InputParameters;
import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.ChronologicalComparator;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class reads and provides solar activity data needed by the
 * two atmospheric models. The data are furnished at the <a
 * href="http://sol.spacenvironment.net/~JB2006/">
 * official JB2006 website.</a>
 * 
 * @author Fabien Maussion
 */
public class SolarInputs97to05 implements JB2006InputParameters, DTMInputParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = -3687601846334870069L;

    private static final double third = 1.0 / 3.0;

    private static final double[] kpTab = new double[] {
        0, 0 + third, 1 - third, 1, 1 + third, 2 - third, 2, 2 + third,
        3 - third, 3, 3 + third, 4 - third, 4, 4 + third, 5 - third, 5,
        5 + third, 6 - third, 6, 6 + third, 7 - third, 7, 7 + third,
        8 - third, 8, 8 + third, 9 - third, 9
    };

    private static final double[] apTab = new double[] {
        0, 2, 3, 4, 5, 6, 7, 9, 12, 15, 18, 22, 27, 32,
        39, 48, 56, 67, 80, 94, 111, 132, 154, 179, 207, 236, 300, 400
    };

    /** All entries. */
    private final SortedSet<TimeStamped> data;

    private LineParameters currentParam;
    private AbsoluteDate firstDate;
    private AbsoluteDate lastDate;

    /**
     * Simple constructor.
     * Data file address is set internally, nothing to be done here.
     * 
     * @exception PatriusException
     */
    private SolarInputs97to05() throws PatriusException {

        this.data = new TreeSet<>(new ChronologicalComparator());
        InputStream in = SolarInputs97to05.class.getResourceAsStream("/atmosphereOrekit/JB_All_97-05.txt");
        final BufferedReader rFlux = new BufferedReader(new InputStreamReader(in));

        in = SolarInputs97to05.class.getResourceAsStream("/atmosphereOrekit/NOAA_ap_97-05.dat.txt");
        final BufferedReader rAp = new BufferedReader(new InputStreamReader(in));

        try {
            this.read(rFlux, rAp);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Singleton getter.
     * 
     * @return the unique instance of this class.
     * @exception PatriusException
     */
    public static SolarInputs97to05 getInstance() throws PatriusException {
        if (LazyHolder.instance == null) {
            throw LazyHolder.orekitException;
        }
        return LazyHolder.instance;
    }

    private void read(final BufferedReader rFlux, final BufferedReader rAp) throws IOException, PatriusException {

        rFlux.readLine();
        rFlux.readLine();
        rFlux.readLine();
        rFlux.readLine();
        rAp.readLine();
        String lineAp;
        String[] flux;
        String[] ap;
        final Calendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(0, 0, 0, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        AbsoluteDate date = null;
        boolean first = true;

        for (String lineFlux = rFlux.readLine(); lineFlux != null; lineFlux = rFlux.readLine()) {

            flux = lineFlux.trim().split("\\s+");

            lineAp = rAp.readLine();
            if (lineAp == null) {
                throw new PatriusException(new DummyLocalizable("inconsistent JB2006 and geomagnetic indices files"));
            }
            ap = lineAp.trim().split("\\s+");

            final int fluxYear = Integer.parseInt(flux[0]);
            final int fluxDay = Integer.parseInt(flux[1]);
            final int apYear = Integer.parseInt(ap[11]);

            if (fluxDay != Integer.parseInt(ap[0])) {
                throw new PatriusException(new DummyLocalizable("inconsistent JB2006 and geomagnetic indices files"));
            }
            if (((fluxYear < 2000) && ((fluxYear - 1900) != apYear)) ||
                ((fluxYear >= 2000) && ((fluxYear - 2000) != apYear))) {
                throw new PatriusException(new DummyLocalizable("inconsistent JB2006 and geomagnetic indices files"));
            }

            cal.set(Calendar.YEAR, fluxYear);
            cal.set(Calendar.DAY_OF_YEAR, fluxDay);

            date = new AbsoluteDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(cal.getTimeInMillis()), ZoneOffset.UTC), TimeScalesFactory.getUTC());

            if (first) {
                first = false;
                this.firstDate = date;
            }

            this.data.add(new LineParameters(date,
                new double[] {
                    Double.parseDouble(ap[3]),
                    Double.parseDouble(ap[4]),
                    Double.parseDouble(ap[5]),
                    Double.parseDouble(ap[6]),
                    Double.parseDouble(ap[7]),
                    Double.parseDouble(ap[8]),
                    Double.parseDouble(ap[9]),
                    Double.parseDouble(ap[10]),

                },
                Double.parseDouble(flux[3]),
                Double.parseDouble(flux[4]),
                Double.parseDouble(flux[5]),
                Double.parseDouble(flux[6]),
                Double.parseDouble(flux[7]),
                Double.parseDouble(flux[8])));

        }
        this.lastDate = date;

    }

    private void findClosestLine(final AbsoluteDate date) throws PatriusException {

        if ((date.durationFrom(this.firstDate) < 0) || (date.durationFrom(this.lastDate) > Constants.JULIAN_DAY)) {
            throw new PatriusException(PatriusMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, date, this.firstDate,
                this.lastDate);
        }

        // don't search if the cached selection is fine
        if ((this.currentParam != null) && (date.durationFrom(this.currentParam.date) >= 0) &&
            (date.durationFrom(this.currentParam.date) < Constants.JULIAN_DAY)) {
            return;
        }
        final LineParameters before = new LineParameters(date.shiftedBy(-Constants.JULIAN_DAY), null, 0, 0, 0, 0, 0, 0);

        // search starting from entries a few steps before the target date
        final SortedSet<TimeStamped> tailSet = this.data.tailSet(before);
        if (tailSet != null) {
            this.currentParam = (LineParameters) tailSet.first();
            if (this.currentParam.date.durationFrom(date) == -Constants.JULIAN_DAY) {
                this.currentParam = (LineParameters) this.data.tailSet(date).first();
            }
        } else {
            throw new PatriusException(new DummyLocalizable("unable to find data for date {0}"), date);
        }
    }

    /** Container class for Solar activity indexes. */
    private static class LineParameters implements TimeStamped, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -1127762834954768272L;

        /** Entries */
        private final AbsoluteDate date;
        private final double[] ap;
        private final double f10;
        private final double f10B;
        private final double s10;
        private final double s10B;
        private final double xm10;
        private final double xm10B;

        /** Simple constructor. */
        private LineParameters(final AbsoluteDate date, final double[] ap, final double f10,
            final double f10B, final double s10, final double s10B,
            final double xm10, final double xm10B) {
            this.date = date;
            this.ap = ap;
            this.f10 = f10;
            this.f10B = f10B;
            this.s10 = s10;
            this.s10B = s10B;
            this.xm10 = xm10;
            this.xm10B = xm10B;

        }

        /** Get the current date */
        @Override
        public AbsoluteDate getDate() {
            return this.date;
        }

    }

    @Override
    public double getAp(final AbsoluteDate date) {
        double result = Double.NaN;
        try {
            this.findClosestLine(date);
            final LocalDateTime localDateTime = date.toLocalDateTime(TimeScalesFactory.getUTC());
            final int hour = localDateTime.get(ChronoField.HOUR_OF_DAY);
            for (int i = 0; i < 8; i++) {
                if ((hour >= (i * 3)) && (hour < ((i + 1) * 3))) {
                    result = this.currentParam.ap[i];
                }
            }
        } catch (final PatriusException e) {
            // nothing
        }
        return result;
    }

    @Override
    public double getF10(final AbsoluteDate date) {
        double result = Double.NaN;
        try {
            this.findClosestLine(date);
            result = this.currentParam.f10;
        } catch (final PatriusException e) {
            // nothing
        }
        return result;
    }

    @Override
    public double getF10B(final AbsoluteDate date) {
        double result = Double.NaN;
        try {
            this.findClosestLine(date);
            result = this.currentParam.f10B;
        } catch (final PatriusException e) {
            // nothing
        }
        return result;
    }

    @Override
    public AbsoluteDate getMaxDate() {
        return this.lastDate.shiftedBy(Constants.JULIAN_DAY);
    }

    @Override
    public AbsoluteDate getMinDate() {
        return this.firstDate;
    }

    @Override
    public double getS10(final AbsoluteDate date) {
        double result = Double.NaN;
        try {
            this.findClosestLine(date);
            result = this.currentParam.s10;
        } catch (final PatriusException e) {
            // nothing
        }
        return result;
    }

    @Override
    public double getS10B(final AbsoluteDate date) {
        double result = Double.NaN;
        try {
            this.findClosestLine(date);
            result = this.currentParam.s10B;
        } catch (final PatriusException e) {
            // nothing
        }
        return result;
    }

    @Override
    public double getXM10(final AbsoluteDate date) {
        double result = Double.NaN;
        try {
            this.findClosestLine(date);
            result = this.currentParam.xm10;
        } catch (final PatriusException e) {
            // nothing
        }
        return result;
    }

    @Override
    public double getXM10B(final AbsoluteDate date) {
        double result = Double.NaN;
        try {
            this.findClosestLine(date);
            result = this.currentParam.xm10B;
        } catch (final PatriusException e) {
            // nothing
        }
        return result;
    }

    @Override
    public double get24HoursKp(final AbsoluteDate date) {
        double result = 0;
        AbsoluteDate myDate = date;

        for (int i = 0; i < 8; i++) {
            result += this.getThreeHourlyKP(date);
            myDate = myDate.shiftedBy(3 * 3600);
        }

        return result / 8;
    }

    @Override
    public double getInstantFlux(final AbsoluteDate date) {
        return this.getF10(date);
    }

    @Override
    public double getMeanFlux(final AbsoluteDate date) {
        return this.getF10B(date);
    }

    /**
     * The 3-H Kp is derived from the Ap index.
     * The used method is explained on <a
     * href="http://www.ngdc.noaa.gov/stp/GEOMAG/kp_ap.shtml">
     * NOAA website.</a>. Here is the corresponding tab :
     * 
     * <pre>
     * The scale is O to 9 expressed in thirds of a unit, e.g. 5- is 4 2/3,
     * 5 is 5 and 5+ is 5 1/3.
     * 
     * The 3-hourly ap (equivalent range) index is derived from the Kp index as follows:
     * 
     * Kp = 0o   0+   1-   1o   1+   2-   2o   2+   3-   3o   3+   4-   4o   4+
     * ap =  0    2    3    4    5    6    7    9   12   15   18   22   27   32
     * Kp = 5-   5o   5+   6-   6o   6+   7-   7o   7+   8-   8o   8+   9-   9o
     * ap = 39   48   56   67   80   94  111  132  154  179  207  236  300  400
     * 
     * </pre>
     */
    @Override
    public double getThreeHourlyKP(final AbsoluteDate date) {

        final double ap = this.getAp(date);
        int i = 0;
        for (i = 0; ap >= apTab[i]; i++) {
            if (i == apTab.length - 1) {
                i++;
                break;
            }
        }
        return kpTab[i - 1];
    }

    /**
     * Holder for the singleton.
     * <p>
     * We use the Initialization on demand holder idiom to store the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.
     * </p>
     */
    private static class LazyHolder {
        private static final SolarInputs97to05 instance;
        private static final PatriusException orekitException;
        static {
            SolarInputs97to05 tmpInstance = null;
            PatriusException tmpException = null;
            try {
                tmpInstance = new SolarInputs97to05();
            } catch (final PatriusException oe) {
                tmpException = oe;
            }
            instance = tmpInstance;
            orekitException = tmpException;
        }
    }
    
    /**
     * This methods throws an exception if the user did not provide solar activity on the provided interval [start,
     * end].
     * All models should implement their own method since the required data interval depends on the model.
     * @param start range start date
     * @param end range end date
     * @throws PatriusException thrown if some solar activity data is missing
     */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do (test)
    }

}
