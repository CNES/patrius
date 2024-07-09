/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * 
 * @history created 20/11/12
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Data loader for the R12 values.
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment the instance is mutable, but no longer changes after the first call to getR12() (the first call
 *                      triggers the DataProvidersManager on the instance).
 *                      This means : an instance can be shared and used in several threads, if getR12() is called once
 *                      in a single thread context first.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public final class R12Loader implements R12Provider, DataLoader {

    /** Months in a year. */
    private static final int Q12 = 12;

    /** J2000 days for {@link AbsoluteDate}.FIFTIES_EPOCH */
    private final int fiftiesDay;

    /** Supported file name. */
    private final String suppFileName;

    /** Still accepts data. */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private volatile boolean stillAcceptsDataFlag = true;

    /** File data map. */
    @SuppressWarnings("PMD.LooseCoupling")
    private final TreeMap<Integer, Double> dataMap = new TreeMap<Integer, Double>();

    /**
     * Constructor.
     * 
     * @param supportedFileName
     *        file name the {@link DataProvidersManager} will send to the instance.
     * @throws PatriusException
     *         should not happen
     */
    public R12Loader(final String supportedFileName) throws PatriusException {
        this.suppFileName = supportedFileName;
        this.fiftiesDay = (new AbsoluteDate("1950-01-01", TimeScalesFactory.getTT()))
            .getComponents(TimeScalesFactory.getTT()).getDate().getJ2000Day();
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return this.stillAcceptsDataFlag;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                    PatriusException {
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));

        // reading the first line (unused)
        r.readLine();
        // Separator "\\s+"
        final String readSt = "\\s+";

        String line = r.readLine();
        boolean first = true;
        while (line != null) {
            final String[] tab = line.trim().split(readSt);
            final int month = Integer.parseInt(tab[1]);
            final int year = Integer.parseInt(tab[0]);
            // final int key = year * q12 + month;
            // r12Map.put(key, Double.parseDouble(tab[3]));
            // imimoisMap.put(key, Integer.parseInt(tab[5]));*
            final int midday = Integer.parseInt(tab[5]);
            final double midval = Double.parseDouble(tab[3]);
            this.dataMap.put(midday, midval);
            if (first) {
                // Extra key for the first day of the first month;
                // same data as the first mid-month.
                // This extra key makes the first half of the first month valid.

                // generate key
                final DateComponents dc = new DateComponents(year, month, 1);
                final AbsoluteDate ad = new AbsoluteDate(dc, TimeScalesFactory.getTT());
                final int key = this.day1950(ad);
                this.dataMap.put(key, midval);
                first = false;
            }
            line = r.readLine();
            if (line == null) {
                // Extra key for the first day of the month following the last;
                // same data as the last mid-month.
                // This extra key makes the last half of the last month valid.

                // generate key
                // if month = 12, nextMonth is 1
                final int nextMonth = (month % Q12) + 1;
                // if month = 12, nextYear is year + 1
                final int nextYear = year + (month / Q12);

                final DateComponents dc = new DateComponents(nextYear, nextMonth, 1);
                final AbsoluteDate ad = new AbsoluteDate(dc, TimeScalesFactory.getTT());
                final int key = this.day1950(ad);
                this.dataMap.put(key, midval);
            }
        }
        // The whole file has been read.
        this.stillAcceptsDataFlag = false;

    }

    /** {@inheritDoc} */
    @Override
    public double getR12(final AbsoluteDate date) throws PatriusException {

        if (this.stillAcceptsDataFlag) {
            // Auto-loads the data file
            DataProvidersManager.getInstance().feed(this.suppFileName, this);
        }

        // Days from 1/1/1950 (FIFTIES_EPOCH) as used in the file.
        final int daysFrom1950 = this.day1950(date);

        // Data for the days surrounding the current day.
        final Integer floorDay = this.dataMap.floorKey(daysFrom1950);
        final Integer ceilingDay = this.dataMap.ceilingKey(daysFrom1950);
        if (floorDay == null || ceilingDay == null) {
            // The date is outside of the range covered by the file
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_IONO_DATE_OUT_OF_FILE);
        }
        final double floorData = this.dataMap.get(floorDay);
        final double ceilingData = this.dataMap.get(ceilingDay);

        final double rez;
        if (floorDay.equals(ceilingDay)) {
            // Date is exactly on a record
            rez = floorData;
        } else {
            // Simple linear interpolation
            final int distFloor = daysFrom1950 - floorDay;
            final int distCeil = ceilingDay - daysFrom1950;
            rez = (distCeil * floorData + distFloor * ceilingData) / (distFloor + distCeil);
        }
        return rez;
    }

    /**
     * Computes the number of full days since 1/1/1950.
     * 
     * @param date
     *        input date
     * @return the number of days since 1/1/1950 for the input date.
     * @throws PatriusException
     *         should not happen
     */
    private int day1950(final AbsoluteDate date) throws PatriusException {
        final DateTimeComponents dtc = date.getComponents(TimeScalesFactory.getTT());
        return dtc.getDate().getJ2000Day() - this.fiftiesDay;
    }
}
