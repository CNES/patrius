/**
 * 
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
 * 
 * @history Created 20/08/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:New solar data management system
 * VERSION::FA:183:17/03/2014:Completed javadoc
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Solar activity toolbox. Has methods to compute mean flux values, to convert from ap to kp.
 * 
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * @version $Id: SolarActivityToolbox.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.2
 * 
 */
public final class SolarActivityToolbox {

    /**
     * Constant 0.01
     */
    private static final double C = 0.01;
    /**
     * Constant 1 / 3
     */
    private static final double OT = 1. / 3.;
    /**
     * Ap to Kp table
     */
    private static final double[][] TAB = new double[][] { { 0., 0. }, { OT, 2. }, { 1 - OT, 3. }, { 1., 4. },
        { 1 + OT, 5 }, { 2 - OT, 6 }, { 2, 7. }, { 2 + OT, 9. }, { 3 - OT, 12. }, { 3., 15. }, { 3 + OT, 18 },
        { 4 - OT, 22 }, { 4, 27. }, { 4 + OT, 32. }, { 5 - OT, 39. }, { 5., 48. }, { 5 + OT, 56 }, { 6 - OT, 67 },
        { 6, 80. }, { 6 + OT, 94. }, { 7 - OT, 111. }, { 7., 132. }, { 7 + OT, 154 }, { 8 - OT, 179 }, { 8, 207. },
        { 8 + OT, 236. }, { 9 - OT, 300. }, { 9., 400. } };

    /**
     * This is a utility class. Private constructor.
     */
    private SolarActivityToolbox() {
    }

    /**
     * Convert a single ap coefficient to a kp coefficient
     * 
     * @param ap
     *        coefficient to convert
     * @return corresponding kp coefficient, linear interpolation
     */
    public static double apToKp(final double ap) {

        // Check that ap is within bounds
        checkApSanity(ap);

        // look for closest inferior value
        int lower = 0;
        boolean flag = false;
        for (int i = 0; i < TAB.length && !flag; i++) {
            if (TAB[i][1] <= ap) {
                lower = i;
            }
            flag = MathLib.abs(TAB[i][1] - ap) < Precision.DOUBLE_COMPARISON_EPSILON;
        }

        // Check flag
        if (flag) {
            // if ap is in the array, return corresponding kp
            return TAB[lower][0];
        } else {
            // interpolate otherwise
            final double x1 = TAB[lower][0];
            final double x2 = TAB[lower + 1][0];
            final double y1 = MathLib.log(TAB[lower][1] + C);
            final double y2 = MathLib.log(TAB[lower + 1][1] + C);

            return (MathLib.log(ap + C) - y1) / (y2 - y1) * (x2 - x1) + x1;

        }
    }

    /**
     * Convert a single kp coefficient to a ap coefficient
     * 
     * @param kp
     *        coefficient to convert
     * @return corresponding ap coefficient, linear interpolation
     */
    public static double kpToAp(final double kp) {

        checkKpSanity(kp);

        /*
         * since successive kp indices have a difference of 1/3 and the first one is 0, the index of flooring entry of
         * kp is as follows
         */
        final int index = (int) MathLib.floor(MathLib.divide(kp, OT));

        if (MathLib.abs(kp % OT) < Precision.DOUBLE_COMPARISON_EPSILON) {
            // if kp is in the array, return corresponding ap
            return TAB[index][1];
        } else {
            // interpolate otherwise
            final double x1 = TAB[index][0];
            final double x2 = TAB[index + 1][0];
            final double y1 = MathLib.log(TAB[index][1] + C);
            final double y2 = MathLib.log(TAB[index + 1][1] + C);

            return MathLib.exp((kp - x1) / (x2 - x1) * (y2 - y1) + y1) - C;
        }
    }

    /**
     * Convert an array
     * 
     * @param ap
     *        array to convert
     * @return corresponding kp array
     */
    public static double[] apToKp(final double[] ap) {

        final double[] kp = new double[ap.length];
        for (int i = 0; i < ap.length; i++) {
            kp[i] = apToKp(ap[i]);
        }
        return kp;

    }

    /**
     * Convert an array
     * 
     * @param kp
     *        array to convert
     * @return corresponding ap array
     */
    public static double[] kpToAp(final double[] kp) {

        final double[] ap = new double[kp.length];
        for (int i = 0; i < kp.length; i++) {
            ap[i] = kpToAp(kp[i]);
        }
        return ap;

    }

    /**
     * Compute mean flux between given dates (rectangular rule)
     * 
     * @param minDate
     *        first date for mean flux computation
     * @param maxDate
     *        last date for mean flux computation
     * @param data
     *        solar data
     * @return the averaged solar flux over interval [date1, date2]
     * @throws PatriusException
     *         if not enough data to cover timespan or if date2 is set before date1
     */
    public static double getMeanAp(final AbsoluteDate minDate, final AbsoluteDate maxDate,
                                   final SolarActivityDataProvider data) throws PatriusException {

        // get all ap values
        final SortedMap<AbsoluteDate, Double[]> map = data.getApKpValues(minDate, maxDate);

        // not enough elements
        if (map.size() <= 1) {
            throw new IllegalArgumentException();
        }

        // total area container
        double result = 0;

        // iterate over all elements
        final Iterator<Entry<AbsoluteDate, Double[]>> sets = map.entrySet().iterator();

        // rectangle method : requires knowledge of previous step
        Entry<AbsoluteDate, Double[]> previous = sets.next();
        Entry<AbsoluteDate, Double[]> current;

        while (sets.hasNext()) {

            current = sets.next();

            result += previous.getValue()[0] * (current.getKey().durationFrom(previous.getKey()));
            previous = current;
        }

        return MathLib.divide(result, maxDate.durationFrom(minDate));
    }

    /**
     * Compute mean flux between given dates using trapezoidal rule
     * 
     * @param date1
     *        first date for mean flux computation
     * @param date2
     *        last date for mean flux computation
     * @param data
     *        solar data
     * @return the averaged (trapezoidal rule) solar flux over interval [date1, date2]
     * @throws PatriusException
     *         if not enough data to cover timespan or if date2 is set before date1
     */
    public static double getMeanFlux(final AbsoluteDate date1, final AbsoluteDate date2,
                                     final SolarActivityDataProvider data) throws PatriusException {

        // check dates order
        if (date1.durationFrom(date2) > 0) {
            throw new PatriusException(PatriusMessages.NON_EXISTENT_TIME);
        }

        // check range
        if (date1.durationFrom(data.getFluxMinDate()) < 0 || data.getFluxMaxDate().durationFrom(date2) < 0) {

            if (date1.durationFrom(data.getFluxMinDate()) < 0) {
                throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, date1, data.getFluxMinDate(),
                    data.getFluxMaxDate());
            } else {
                throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, date2, data.getFluxMinDate(),
                    data.getFluxMaxDate());
            }
        }

        // get all values for integration, including borders (overwritten if exist already)
        final SortedMap<AbsoluteDate, Double> map = new TreeMap<AbsoluteDate, Double>();
        map.putAll(data.getInstantFluxValues(date1, date2));
        map.put(date1, data.getInstantFluxValue(date1));
        map.put(date2, data.getInstantFluxValue(date2));

        if (map.size() <= 1) {
            throw new IllegalArgumentException();
        }

        // Iterator on map entry set
        final Iterator<Entry<AbsoluteDate, Double>> entries = map.entrySet().iterator();

        // intermediate variable
        Entry<AbsoluteDate, Double> current;
        Entry<AbsoluteDate, Double> previous = entries.next();

        double area = 0;

        // Loop on all entries
        while (entries.hasNext()) {
            current = entries.next();
            area += (current.getKey().durationFrom(previous.getKey())) * (current.getValue() + previous.getValue()) / 2;
            previous = current;
        }

        return MathLib.divide(area, date2.durationFrom(date1));
    }

    /**
     * Compute mean flux between given dates.
     * 
     * @param date1
     *        first date for mean flux computation
     * @param date2
     *        last date for mean flux computation
     * @param data
     *        solar data
     * @return the averaged (arithmetic mean) solar flux over interval [date1, date2]
     * @throws PatriusException
     *         if not enough data to cover timespan or if date2 is set before date1
     */
    public static double getAverageFlux(final AbsoluteDate date1, final AbsoluteDate date2,
                                        final SolarActivityDataProvider data) throws PatriusException {

        // check dates order
        if (date1.durationFrom(date2) > 0) {
            throw new PatriusException(PatriusMessages.NON_EXISTENT_TIME);
        }

        // check range
        if (date1.durationFrom(data.getFluxMinDate()) < 0 || data.getFluxMaxDate().durationFrom(date2) < 0) {
            if (date1.durationFrom(data.getFluxMinDate()) < 0) {
                // date1 is before FluxMinDate
                throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, date1, data.getFluxMinDate(),
                    data.getFluxMaxDate());
            } else {
                // date2 is before FluxMinDate
                throw new PatriusException(PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, date2, data.getFluxMinDate(),
                    data.getFluxMaxDate());
            }
        }

        final SortedMap<AbsoluteDate, Double> map = data.getInstantFluxValues(date1, date2);

        double sum = 0;
        for (final Double vals : map.values()) {
            sum += vals;
        }

        return MathLib.divide(sum, map.size());
    }

    /**
     * Check that the specified ap coefficient is within bounds
     * 
     * @param ap
     *        ap coefficient
     */
    public static void checkApSanity(final double ap) {

        // check range
        if (ap > TAB[TAB.length - 1][1] || ap < 0) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.ILLEGAL_VALUE_FOR_GEOMAG_COEFFICIENT,
                ap);
        }

    }

    /**
     * Check that the specified kp coefficient is within bounds
     * 
     * @param kp
     *        kp coefficient
     */
    public static void checkKpSanity(final double kp) {

        // check range
        if (kp > TAB[TAB.length - 1][0] || kp < 0) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.ILLEGAL_VALUE_FOR_GEOMAG_COEFFICIENT,
                kp);
        }

    }
}
