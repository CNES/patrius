/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history Created 10/02/2016
 * 
 * HISTORY
* VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:555:10/02/2016:new solar activity data provider
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is a solar activity data provider fed with:
 * <ul>
 * <li>a user-defined {@link SolarActivityDataProvider}</li>
 * <li>A averaged duration <i>d</i></li>
 * </ul>
 * It is built with the following convention:
 * <ul>
 * <li>It returns solar activity from user-provided solar activity data provider if date is within timespan of the
 * user-provided solar activity data provider.</li>
 * <li>It returns an average of first available solar data over user-defined period <i>d</i> if date is before lower
 * boundary of the user-provided solar activity data provider.</li>
 * <li>It returns an average of last available solar data over user-defined period <i>d</i> if date is after upper
 * boundary of the user-provided solar activity data provider.</li>
 * </ul>
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * @version $Id: ExtendedSolarActivityWrapper.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 3.2
 * 
 */
public class ExtendedSolarActivityWrapper implements SolarActivityDataProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -103029685775257948L;

    /** Underlying solar activity provider. */
    private final SolarActivityDataProvider provider;

    /** Mean duration (s) for extension computation. */
    private final double d;

    /** Mean flux F10.7 on first data. */
    private final double meanF107A1;

    /** Mean flux F10.7 on last data. */
    private final double meanF107A2;

    /** Mean geomagnetic activity Ap on first data. */
    private final double meanAp1;

    /** Mean geomagnetic activity Ap on last data. */
    private final double meanAp2;

    /** Mean geomagnetic activity Kp on first data. */
    private final double meanKp1;

    /** Mean geomagnetic activity Kp on last data. */
    private final double meanKp2;

    /** Average F10.7 sampling period of underlying solar activity provider. */
    private final double stepF107;

    /** Average Ap/Kp sampling period of underlying solar activity provider. */
    private final double stepApKp;

    /**
     * Constructor.
     * 
     * @param innerProvider
     *        underlying solar activity provider
     * @param duration
     *        mean duration (s) for extension computation. If this duration is larger than available data
     *        timespan, d will be equal to timespan length
     */
    public ExtendedSolarActivityWrapper(final SolarActivityDataProvider innerProvider, final double duration) {
        this.provider = innerProvider;
        final double dataTimeSpan = this.provider.getMaxDate().durationFrom(this.provider.getMinDate());
        final boolean tooSmallData = (dataTimeSpan <= duration);
        this.d = MathLib.min(duration, dataTimeSpan);

        try {
            if (this.d > 0) {
                // Two case: boundary is infinity (mean cannot be computed, hence direct value is returned) or not
                this.meanF107A1 = this.computeMeanF107A1(tooSmallData);
                this.meanF107A2 = this.computeMeanF107A2(tooSmallData);
                this.meanAp1 = this.computeMeanAp1(tooSmallData);
                this.meanAp2 = this.computeMeanAp2(tooSmallData);
            } else {
                // Particular case: d < 0: boundaries are used
                this.meanF107A1 = this.provider.getInstantFluxValue(this.provider.getFluxMinDate());
                this.meanF107A2 = this.provider.getInstantFluxValue(this.provider.getFluxMaxDate());
                this.meanAp1 = this.provider.getAp(this.provider.getApKpMinDate());
                this.meanAp2 = this.provider.getAp(this.provider.getApKpMaxDate());
            }

            this.meanKp1 = SolarActivityToolbox.apToKp(this.meanAp1);
            this.meanKp2 = SolarActivityToolbox.apToKp(this.meanAp2);

            this.stepF107 = this.provider.getStepF107();
            this.stepApKp = this.provider.getStepApKp();

        } catch (final PatriusException e) {
            // It cannot happen since mean data computation is consistent with available data timespan
            throw PatriusException.createInternalError(e);
        }
    }

    /**
     * Compute lower mean F10.7.
     * 
     * @param tooSmallData
     *        true if there is not enough data to compute value
     * @return lower mean F10.7
     * @throws PatriusException
     *         thrown if mean computation failed
     */
    private double computeMeanF107A1(final boolean tooSmallData) throws PatriusException {
        final double result;
        if (this.provider.getFluxMinDate().equals(AbsoluteDate.PAST_INFINITY)) {
            result = this.provider.getInstantFluxValue(this.provider.getFluxMinDate());
        } else {
            result = SolarActivityToolbox.getMeanFlux(this.provider.getFluxMinDate(),
                tooSmallData ? this.provider.getFluxMaxDate() : this.provider.getFluxMinDate().shiftedBy(this.d),
                this.provider);
        }

        return result;
    }

    /**
     * Compute upper mean F10.7.
     * 
     * @param tooSmallData
     *        true if there is not enough data to compute value
     * @return upper mean F10.7
     * @throws PatriusException
     *         thrown if mean computation failed
     */
    private double computeMeanF107A2(final boolean tooSmallData) throws PatriusException {
        final double result;

        if (this.provider.getFluxMaxDate().equals(AbsoluteDate.FUTURE_INFINITY)) {
            result = this.provider.getInstantFluxValue(this.provider.getFluxMaxDate());
        } else {
            result = SolarActivityToolbox.getMeanFlux(
                tooSmallData ? this.provider.getFluxMinDate() : this.provider.getFluxMaxDate().shiftedBy(-this.d),
                this.provider.getFluxMaxDate(), this.provider);
        }

        return result;
    }

    /**
     * Compute lower mean Ap/Kp.
     * 
     * @param tooSmallData
     *        true if there is not enough data to compute value
     * @return lower mean Ap/Kp
     * @throws PatriusException
     *         thrown if mean computation failed
     */
    private double computeMeanAp1(final boolean tooSmallData) throws PatriusException {
        final double result;

        if (this.provider.getApKpMinDate().equals(AbsoluteDate.PAST_INFINITY)) {
            result = this.provider.getAp(this.provider.getApKpMinDate());
        } else {
            result = SolarActivityToolbox.getMeanAp(
                this.provider.getApKpMinDate(),
                tooSmallData ? this.provider.getApKpMaxDate() : this.provider.getApKpMinDate().shiftedBy(this.d),
                this.provider);
        }

        return result;
    }

    /**
     * Compute upper mean Ap/Kp.
     * 
     * @param tooSmallData
     *        true if there is not enough data to compute value
     * @return upper mean Ap/Kp
     * @throws PatriusException
     *         thrown if mean computation failed
     */
    private double computeMeanAp2(final boolean tooSmallData) throws PatriusException {
        final double result;

        if (this.provider.getApKpMaxDate().equals(AbsoluteDate.FUTURE_INFINITY)) {
            result = this.provider.getAp(this.provider.getApKpMaxDate());
        } else {
            result = SolarActivityToolbox.getMeanAp(
                tooSmallData ? this.provider.getApKpMinDate() : this.provider.getApKpMaxDate().shiftedBy(-this.d),
                this.provider.getApKpMaxDate(), this.provider);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getFluxMinDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getFluxMaxDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getApKpMinDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getApKpMaxDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public double getInstantFluxValue(final AbsoluteDate date) throws PatriusException {
        final double res;
        if (date.durationFrom(this.provider.getFluxMinDate()) < 0) {
            res = this.meanF107A1;
        } else if (date.durationFrom(this.provider.getFluxMaxDate()) > 0) {
            res = this.meanF107A2;
        } else {
            res = this.provider.getInstantFluxValue(date);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double getAp(final AbsoluteDate date) throws PatriusException {
        final double res;
        if (date.durationFrom(this.provider.getApKpMinDate()) < 0) {
            res = this.meanAp1;
        } else if (date.durationFrom(this.provider.getApKpMaxDate()) > 0) {
            res = this.meanAp2;
        } else {
            res = this.provider.getAp(date);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double getKp(final AbsoluteDate date) throws PatriusException {
        final double res;
        if (date.durationFrom(this.provider.getApKpMinDate()) < 0) {
            res = this.meanKp1;
        } else if (date.durationFrom(this.provider.getApKpMaxDate()) > 0) {
            res = this.meanKp2;
        } else {
            res = this.provider.getKp(date);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public SortedMap<AbsoluteDate, Double> getInstantFluxValues(final AbsoluteDate date1,
                                                                final AbsoluteDate date2) throws PatriusException {

        SortedMap<AbsoluteDate, Double> result = new TreeMap<AbsoluteDate, Double>();

        if (date1.durationFrom(this.provider.getFluxMinDate()) < 0
            && date2.durationFrom(this.provider.getFluxMinDate()) < 0) {
            // [date1; date2] < min date
            result.put(date1, this.meanF107A1);
            result.put(date2, this.meanF107A1);
        } else if (date1.durationFrom(this.provider.getFluxMaxDate()) > 0
            && date2.durationFrom(this.provider.getFluxMaxDate()) > 0) {
            // max date < [date1; date2]
            result.put(date1, this.meanF107A2);
            result.put(date2, this.meanF107A2);
        } else {
            // Intersection of [date1; date2] and [min date; max date] is not empty
            result = this.getInstantFluxValuesGeneral(date1, date2);
        }

        return result;
    }

    /**
     * Get raw instant flux values between the given dates (general case: intersection with underlying provider
     * data is not empty).
     * 
     * @param date1
     *        first date
     * @param date2
     *        second date
     * @return submap of instant flux values sorted according to date
     * @throws PatriusException
     *         if no solar activity at date
     */
    private SortedMap<AbsoluteDate, Double>
            getInstantFluxValuesGeneral(final AbsoluteDate date1,
                                        final AbsoluteDate date2) throws PatriusException {

        // Initialization
        final SortedMap<AbsoluteDate, Double> result = new TreeMap<AbsoluteDate, Double>();

        // Get provider min/max date
        AbsoluteDate minDate = date1;
        AbsoluteDate maxDate = date2;
        if (date1.durationFrom(this.provider.getFluxMinDate()) < 0) {
            minDate = this.provider.getFluxMinDate();
        }
        if (date2.durationFrom(this.provider.getFluxMaxDate()) > 0) {
            maxDate = this.provider.getFluxMaxDate();
        }

        // Data from underlying provider
        result.putAll(this.provider.getInstantFluxValues(minDate, maxDate));

        // Extrapolated data
        if (date1.durationFrom(this.provider.getFluxMinDate()) < 0) {
            AbsoluteDate date = this.provider.getFluxMinDate().shiftedBy(-this.stepF107);
            while (date.durationFrom(date1) >= 0) {
                result.put(date, this.meanF107A1);
                date = date.shiftedBy(-this.stepF107);
            }
        }

        if (date2.durationFrom(this.provider.getFluxMaxDate()) > 0) {
            AbsoluteDate date = this.provider.getFluxMaxDate().shiftedBy(this.stepF107);
            while (date.durationFrom(date2) <= 0) {
                result.put(date, this.meanF107A2);
                date = date.shiftedBy(this.stepF107);
            }
        }

        // Return result
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public SortedMap<AbsoluteDate, Double[]> getApKpValues(final AbsoluteDate date1,
                                                           final AbsoluteDate date2) throws PatriusException {

        SortedMap<AbsoluteDate, Double[]> result = new TreeMap<AbsoluteDate, Double[]>();

        if (date1.durationFrom(this.provider.getApKpMinDate()) < 0
            && date2.durationFrom(this.provider.getApKpMinDate()) < 0) {
            // [date1; date2] < min date
            result.put(date1, new Double[] { this.meanAp1, this.meanKp1 });
            result.put(date2, new Double[] { this.meanAp1, this.meanKp1 });
        } else if (date1.durationFrom(this.provider.getApKpMaxDate()) > 0
            && date2.durationFrom(this.provider.getApKpMaxDate()) > 0) {
            // max date < [date1; date2]
            result.put(date1, new Double[] { this.meanAp2, this.meanKp2 });
            result.put(date2, new Double[] { this.meanAp2, this.meanKp2 });
        } else {
            // Intersection of [date1; date2] and [min date; max date] is not empty
            result = this.getApKpValuesGeneral(date1, date2);
        }

        return result;
    }

    /**
     * Get ap / kp values between the given dates (general case: intersection with underlying provider
     * data is not empty).
     * 
     * @param date1
     *        first date
     * @param date2
     *        second date
     * @return submap of instant flux values sorted according to date
     * @throws PatriusException
     *         if no solar activity at date
     */
    private SortedMap<AbsoluteDate, Double[]> getApKpValuesGeneral(final AbsoluteDate date1,
                                                                   final AbsoluteDate date2) throws PatriusException {

        // Initialization
        final SortedMap<AbsoluteDate, Double[]> result = new TreeMap<AbsoluteDate, Double[]>();

        // Get provider min/max date
        AbsoluteDate minDate = date1;
        AbsoluteDate maxDate = date2;
        if (date1.durationFrom(this.provider.getApKpMinDate()) < 0) {
            minDate = this.provider.getApKpMinDate();
        }
        if (date2.durationFrom(this.provider.getApKpMaxDate()) > 0) {
            maxDate = this.provider.getApKpMaxDate();
        }

        // Data from underlying provider
        result.putAll(this.provider.getApKpValues(minDate, maxDate));

        // Extrapolated data
        if (date1.durationFrom(this.provider.getApKpMinDate()) < 0) {
            AbsoluteDate date = this.provider.getApKpMinDate().shiftedBy(-this.stepApKp);
            while (date.durationFrom(date1) >= 0) {
                result.put(date, new Double[] { this.meanAp1, this.meanKp1 });
                date = date.shiftedBy(-this.stepApKp);
            }
        }

        if (date2.durationFrom(this.provider.getApKpMaxDate()) > 0) {
            AbsoluteDate date = this.provider.getApKpMaxDate().shiftedBy(this.stepApKp);
            while (date.durationFrom(date2) <= 0) {
                result.put(date, new Double[] { this.meanAp2, this.meanKp2 });
                date = date.shiftedBy(this.stepApKp);
            }
        }

        // Return result
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public double getStepApKp() throws PatriusException {
        return stepApKp;
    }

    /** {@inheritDoc} */
    @Override
    public double getStepF107() throws PatriusException {
        return stepF107;
    }
}
