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
 * @history 03/10/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3155:10/05/2022:[PATRIUS] Ajout d'une methode public contains a la classe AbsoluteDateInterval
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2650:18/05/2021:constructeur d intervalle de temps avec date de debut et duree
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.5:DM:DM-2471:27/05/2020:Ajout d'une methode toString(TimeScale) a la classe AbsoluteDateInterval
 * VERSION:4.4:FA:FA-2137:04/10/2019:FA mineure Patrius V4.3
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.interval.ComparableInterval;
import fr.cnes.sirius.patrius.math.interval.GenericInterval;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class implements an interval based on the AbsoluteDate class,<br>
 * using the ComparableInterval class.
 * </p>
 * <p>
 * This implementation enforces that <code>AbsoluteDate.PAST_INFINITE</code> and
 * <code>AbsoluteDate.FUTURE_INFINITE</code> cannot be closed boundaries.
 * </p>
 * 
 * @useSample <p>
 *            <code>
 * final AbsoluteDate lowEnd = new AbsoluteDate();
 * final AbsoluteDate upEnd = new AbsoluteDate(lowEnd, 4578.14);
 * final IntervalEndpointType lowInt = IntervalEndpointType.CLOSED;
 * final IntervalEndpointType upInt = IntervalEndpointType.OPEN;
 * final AbsoluteDateInterval dateInterval = new AbsoluteDateInterval(lowInt, lowEnd, upEnd, upInt);
 * </code>
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @author cardosop
 * 
 * @version $Id: AbsoluteDateInterval.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 3.0
 */
public class AbsoluteDateInterval extends ComparableInterval<AbsoluteDate> {

    /** Interval ] -inf ; +inf [. */
    public static final AbsoluteDateInterval INFINITY = new AbsoluteDateInterval(IntervalEndpointType.OPEN,
            AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY, IntervalEndpointType.OPEN);

     /** Serializable UID. */
    private static final long serialVersionUID = -2265247850750610628L;

    /**
     * Builds a new {@link AbsoluteDateInterval} with closed endpoints (or open if infinite dates are provided).
     *
     * @param lowerData
     *        the lower end AbsoluteDate
     * @param upperData
     *        the upper end AbsoluteDate
     *
     * @throws MathIllegalArgumentException
     *         when the interval is invalid (unchecked exception).<br>
     *         In addition to <code>GenericInterval</code> and <code>ComparableInterval</code> checks, the infinite date
     *         boundaries also have to be opened.
     *
     * @see ComparableInterval
     * @see GenericInterval
     * @see AbsoluteDate
     */
    public AbsoluteDateInterval(final AbsoluteDate lowerData, final AbsoluteDate upperData) {
        this(decideIntervalEndPoint(lowerData), lowerData, upperData, 
                decideIntervalEndPoint(upperData));
        
    }

    /**
     * Builds a new {@link AbsoluteDateInterval} with closed endpoints.
     *
     * @param lowerData
     *        the lower end AbsoluteDate
     * @param duration
     *        the interval duration
     *
     * @throws MathIllegalArgumentException
     *         when the interval is invalid (unchecked exception).<br>
     *         In addition to <code>GenericInterval</code> and <code>ComparableInterval</code> checks, the infinite date
     *         boundaries also have to be opened.
     *
     * @see ComparableInterval
     * @see GenericInterval
     * @see AbsoluteDate
     */
    public AbsoluteDateInterval(final AbsoluteDate lowerData, final double duration) {
        this(IntervalEndpointType.CLOSED, lowerData, lowerData.shiftedBy(duration), IntervalEndpointType.CLOSED);  
    }
    
    /**
     * Builds a new {@link AbsoluteDateInterval}.
     * 
     * @param lowerDataIn
     *        lower end AbsoluteDate
     * @param upperDataIn
     *        upper end AbsoluteDate
     * @param lowerEndpointIn
     *        lower end boundary type
     * @param upperEndpointIn
     *        upper end boundary type
     * @throws MathIllegalArgumentException
     *         when the interval is invalid (unchecked exception).<br>
     *         In addition to <code>GenericInterval</code> and <code>ComparableInterval</code> checks, the infinite date
     *         boundaries also have to be opened.
     * @see ComparableInterval
     * @see GenericInterval
     * @see AbsoluteDate
     */
    public AbsoluteDateInterval(final IntervalEndpointType lowerEndpointIn,
        final AbsoluteDate lowerDataIn,
        final AbsoluteDate upperDataIn, final IntervalEndpointType upperEndpointIn) {
        // The parent constructor performs some validation
        super(lowerEndpointIn, lowerDataIn, upperDataIn, upperEndpointIn);
        
        // calls the AbsoluteDate specific validation
        if (!adIntervalIsOK(lowerEndpointIn, lowerDataIn, upperDataIn, upperEndpointIn)) {
            throw new MathIllegalArgumentException(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN);
        }
    }

    /**
     * Builds a new {@link AbsoluteDateInterval} from a {@link ComparableInterval}<{@link AbsoluteDate}>.
     *
     * @param interval
     *        the interval
     *
     * @throws MathIllegalArgumentException
     *         when the interval is invalid (unchecked exception).<br>
     *         In addition to <code>GenericInterval</code> and <code>ComparableInterval</code> checks, the infinite date
     *         boundaries also have to be opened.
     *
     * @see ComparableInterval
     * @see GenericInterval
     * @see AbsoluteDate
     */
    public AbsoluteDateInterval(final ComparableInterval<AbsoluteDate> interval) {
        this(interval.getLowerEndpoint(), interval.getLowerData(), interval.getUpperData(),
            interval.getUpperEndpoint());
    }

    /**
     * Builds a new {@link AbsoluteDateInterval} from a {@link ComparableInterval}<{@link Double}>
     * and a reference {@link AbsoluteDate}.
     *
     * <p>
     * The bounds of the interval returned are computed by shifting the reference date by the lower and upper data of
     * the interval provided.
     * </p>
     *
     * @param interval
     *        the interval
     * @param referenceDate
     *        the reference date
     *
     * @throws MathIllegalArgumentException
     *         when the interval is invalid (unchecked exception).<br>
     *         In addition to <code>GenericInterval</code> and <code>ComparableInterval</code> checks, the infinite date
     *         boundaries also have to be opened.
     *
     * @see ComparableInterval
     * @see GenericInterval
     * @see AbsoluteDate
     */
    public AbsoluteDateInterval(final ComparableInterval<Double> interval,
        final AbsoluteDate referenceDate) {
        this(interval.getLowerEndpoint(), referenceDate.shiftedBy(interval.getLowerData()),
            referenceDate.shiftedBy(interval.getUpperData()), interval.getUpperEndpoint());
    }

    /**
     * Check the validity of the <code>AbsoluteDate</code>-based interval.<br>
     * 
     * <p>
     * Returns <code>false</code> if:
     * <ul>
     * <li>the the lower and the upper bounds are the same and any endpoints is open;
     * <li>if a bound is infinite with a closed endpoint.
     * </ul>
     * </p>
     *
     * @param lowerDataIn
     *        lower end data value
     * @param upperDataIn
     *        upper end data value
     * @param lowerEndpointIn
     *        lower end boundary type
     * @param upperEndpointIn
     *        upper end boundary type
     * @return true if the dates are valid
     */
    private static boolean adIntervalIsOK(final IntervalEndpointType lowerEndpointIn,
                                   final AbsoluteDate lowerDataIn, final AbsoluteDate upperDataIn,
                                   final IntervalEndpointType upperEndpointIn) {
        boolean validFlag = true;
        final AbsoluteDate ld = lowerDataIn;
        final IntervalEndpointType le = lowerEndpointIn;
        final AbsoluteDate ud = upperDataIn;
        final IntervalEndpointType ue = upperEndpointIn;
        final AbsoluteDate past8 = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate future8 = AbsoluteDate.FUTURE_INFINITY;
        // Empty intervals are forbidden.
        // An empty interval has one open endpoint
        // and equal endpoint values
        if ((ld.compareTo(ud) == 0) &&
            (le == IntervalEndpointType.OPEN || ue == IntervalEndpointType.OPEN)) {
            validFlag = false;
        } else if ((ld.equals(past8) || ld.equals(future8)) &&
                // A closed bracket does not make sense with an infinity endpoint,
                // so we forbid it
            le == IntervalEndpointType.CLOSED) {
            validFlag = false;
        } else if ((ud.equals(past8) || ud.equals(future8)) &&
            ue == IntervalEndpointType.CLOSED) {
            validFlag = false;
        }
        // return
        return validFlag;
    }

    /**
     * Decider weather the interval bracket should be closed or open when the user did not specify it.
     * If the date is -inf or +inf the bracket is OPEN, and CLOSED otherwise.
     * @param data
     *        AbsolutData to decide for the bracket
     * @return IntervalEndpointType.CLOSED or IntervalEndpointType.OPEN
     */
    private static IntervalEndpointType decideIntervalEndPoint(final AbsoluteDate data) {
        if (data ==  AbsoluteDate.PAST_INFINITY || data ==  AbsoluteDate.FUTURE_INFINITY) {
            return IntervalEndpointType.OPEN;
        }
        return IntervalEndpointType.CLOSED;
    }

    /**
     * Computes the interval duration (its length) in seconds.
     * <p>
     * The duration is the number of seconds physically elapsed between the lower and the upper endpoint, as computed by
     * <code>AbsoluteDate.durationFrom()</code>.<br>
     * This means the duration is measured in a linear time scale (TAI time scale).<br>
     * </p>
     * 
     * @return the interval duration in seconds
     * 
     * @see AbsoluteDate#durationFrom(AbsoluteDate)
     */
    public double getDuration() {
        return this.getUpperData().durationFrom(this.getLowerData());
    }

    /**
     * Compute the middle date.
     * <p>
     * Note: if the {@link #getLowerData() lower date} is {@link AbsoluteDate#PAST_INFINITY
     * PAST_INFINITY}, then {@link AbsoluteDate#PAST_INFINITY PAST_INFINITY} is returned.
     * </p>
     *
     * @return the middle date
     */
    public AbsoluteDate getMiddleDate() {
        final AbsoluteDate middleDate;
        if (this.getLowerData().equals(AbsoluteDate.PAST_INFINITY)) {
            middleDate = AbsoluteDate.PAST_INFINITY;
        } else {
            middleDate = this.getLowerData().shiftedBy(this.getDuration() / 2.);
        }
        return middleDate;
    }

    /**
     * Computes the duration in seconds between the two intervals.
     * <p>
     * The duration between two intervals is the duration between the end of the earlier interval and the beginning of
     * the later. If the intervals overlap, this duration is 0.<br>
     * The sign of the result is positive if the given interval comes earlier.
     * </p>
     * 
     * @param interval
     *        the given interval
     * @return the duration in seconds
     * 
     * @see AbsoluteDate#durationFrom(AbsoluteDate)
     * 
     */
    public double durationFrom(final AbsoluteDateInterval interval) {
        double rez = 0.;
        // If the intervals overlap, the separation duration is 0.
        if (this.overlaps(interval)) {
            rez = 0.;
        } else {
            if (this.getLowerData().compareTo(interval.getLowerData()) > 0) {
                // the given interval is before,
                // the separating duration is positive
                rez = this.getLowerData().durationFrom(interval.getUpperData());
            } else {
                // the given interval is after,
                // the separating duration is negative
                rez = this.getUpperData().durationFrom(interval.getLowerData());
            }
        }
        return rez;
    }

    /**
     * Checks if the duration of the interval is longer, shorter or equal to the duration of
     * another interval.
     * <p>
     * It returns a positive integer if the duration is longer, a negative integer if the duration is shorter and zero
     * if the durations of the two intervals are identical.<br>
     * The method checks also the lower and upper end points of the intervals, considering that an interval with an open
     * end point is shorter than an interval with the same duration and a closed end point.
     * </p>
     * 
     * @param interval
     *        other interval
     * @return a negative integer, zero, or a positive integer
     */
    public int compareDurationTo(final AbsoluteDateInterval interval) {
        int rez;
        final double duration1 = this.getDuration();
        final double duration2 = interval.getDuration();
        if (duration1 > duration2) {
            // the interval is longer than the input interval:
            rez = +1;
        } else if (duration1 < duration2) {
            // the interval is shorter than the input interval:
            rez = -1;
        } else {
            // the two intervals have the same duration, check the lower end point:
            final int lowerPointCompare = this.getLowerEndpoint().compareTo(
                interval.getLowerEndpoint());
            rez = lowerPointCompare;
            if (lowerPointCompare == 0) {
                // the two intervals have the same duration and the same lower end points, check the
                // upper end point:
                final int upperPointCompare = this.getUpperEndpoint().compareTo(
                    interval.getUpperEndpoint());
                rez = upperPointCompare;
            }
        }
        return rez;
    }

    /**
     * Returns this interval merged with another interval (if they can be merged).
     * <p>
     * To be merged, the two intervals must overlap or be connected. If that is not the case, the method returns
     * <code>null</code>.
     * </p>
     * 
     * @param interval
     *        the interval to be merged
     * @return the merged interval
     */
    public AbsoluteDateInterval mergeTo(final AbsoluteDateInterval interval) {
        final ComparableInterval<AbsoluteDate> merged = super.mergeTo(interval);
        AbsoluteDateInterval result = null;
        if (merged != null) {
            result = new AbsoluteDateInterval(merged);
        }
        return result;
    }

    /**
     * Returns the intersection with another interval.
     * <p>
     * If the intervals do not overlap, the method returns null.
     * </p>
     * 
     * @param interval
     *        the interval to be intersected
     * @return the interval representing the intersection between the two intervals.
     */
    public AbsoluteDateInterval getIntersectionWith(final AbsoluteDateInterval interval) {
        final ComparableInterval<AbsoluteDate> intersection = super.getIntersectionWith(interval);
        AbsoluteDateInterval result = null;
        if (intersection != null) {
            result = new AbsoluteDateInterval(intersection);
        }
        return result;
    }

    /**
     * Returns the interval with its lower an upper bounds shifted by the specified value.
     *
     * @param shift
     *        the shift applied to the lower and the upper bounds (in seconds)
     *
     * @return the shifted interval
     */

    public AbsoluteDateInterval shift(final double shift) {
        return this.shift(shift, shift);
    }

    /**
     * Returns the interval with its lower an upper bounds shifted by the specified values.
     *
     * @param lowerShift
     *        the shift applied to the lower bound (in seconds)
     * @param upperShift
     *        the shift applied to the upper bound (in seconds)
     *
     * @return the shifted interval
     */
    public AbsoluteDateInterval shift(final double lowerShift, final double upperShift) {
        return new AbsoluteDateInterval(this.getLowerEndpoint(), this.getLowerData().shiftedBy(lowerShift),
            this.getUpperData().shiftedBy(upperShift), this.getUpperEndpoint());
    }

    /**
     * Returns the interval scaled by a given factor with respect to its midpoint.
     *
     * @param scalingFactor
     *        the scaling factor
     *
     * @return the scaled interval
     */
    public AbsoluteDateInterval scale(final double scalingFactor) {
        // Interval midpoint
        final AbsoluteDate midpoint = this.getLowerData().shiftedBy(0.5 * this.getDuration());

        // Return the scaled interval
        return this.scale(scalingFactor, midpoint);
    }

    /**
     * Returns the interval scaled by a given factor with respect to the specified epoch.
     *
     * @param scalingFactor
     *        the scaling factor
     * @param epoch
     *        the epoch of reference
     *
     * @return the scaled interval
     */
    public AbsoluteDateInterval scale(final double scalingFactor, final AbsoluteDate epoch) {
        if (scalingFactor <= 0) {
            throw new IllegalArgumentException(PatriusMessages.NOT_POSITIVE_SCALING_FACTOR
                .getSourceString());
        }

        // Interval lower and upper endpoints
        AbsoluteDate lower = this.getLowerData();
        AbsoluteDate upper = this.getUpperData();

        // Lower and upper durations w.r.t the specified epoch
        final double dtm = lower.durationFrom(epoch);
        final double dtp = upper.durationFrom(epoch);

        // Interval scaling w.r.t the midpoint
        lower = epoch.shiftedBy(dtm * scalingFactor);
        upper = epoch.shiftedBy(dtp * scalingFactor);

        // Return the scaled interval
        return new AbsoluteDateInterval(this.getLowerEndpoint(), lower, upper, this.getUpperEndpoint());
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDateInterval extendTo(final AbsoluteDate epoch) {
        return new AbsoluteDateInterval(super.extendTo(epoch));
    }

    /**
     * Returns a String representing the durations between the interval's bounds and a specified
     * date.
     *
     * @param date
     *        the reference date
     *
     * @return a String with boundary brackets and values.
     */
    public String toString(final AbsoluteDate date) {
        final double lower = this.getLowerData().durationFrom(date);
        final double upper = this.getUpperData().durationFrom(date);

        final ComparableInterval<Double> interval = new ComparableInterval<>(
            this.getLowerEndpoint(), lower, upper, this.getUpperEndpoint());

        return interval.toString();
    }

    /**
     * Returns a String representing the interval using provided Timescale.
     * @param timeScale timescale in which the dates should be represented
     * @return a String representation of date interval in provided timescale
     */
    public String toString(final TimeScale timeScale) {
        final String leftB = "[";
        final String rightB = "]";
        final String spc = " ";
        // Starting and ending brackets determined here
        final String startB = (this.getLowerEndpoint().equals(IntervalEndpointType.CLOSED)) ? leftB : rightB;
        final String endB = (this.getUpperEndpoint().equals(IntervalEndpointType.CLOSED)) ? rightB : leftB;
        // Values added here
        return startB + spc + this.getLowerData().toString(timeScale) + " ; " + this.getUpperData().toString(timeScale)
                + spc + endB;
    }
    
    /**
     * Returns a list of dates constructed from the interval, a date every step (in seconds).
     * It takes into account if the interval is open or closed.
     * Example: For a step = 0.5
     *          [0, 1]   => 0, 0.5, 1
     *          [0, 1[   => 0, 0.5 
     *          ]0, 1[   => 0.5 
     *          [0, 0.9] => 0, 0.5
     * 
     * @param step
     *        step in seconds between dates
     * @return list of dates 
     * @throws MathIllegalArgumentException
     *         when n is smaller than 2
     *         when lower or upper data is infinity
     */
    public List<AbsoluteDate> getDateList(final double step) {

        // step must be positive and different from 0
        if (step <= 0) {
            throw new MathIllegalArgumentException(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN);
        }
        // data should not be infinity
        final AbsoluteDate lData = this.getLowerData();
        final AbsoluteDate upData = this.getUpperData();
        if (lData == AbsoluteDate.PAST_INFINITY || upData == AbsoluteDate.FUTURE_INFINITY) {
            throw new MathIllegalArgumentException(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN);
        }
        // New list of dates
        final List<AbsoluteDate> dateList = new ArrayList<>((int) (this.getDuration() / step));
        // Check if the interval is closed
        final IntervalEndpointType lowInt = this.getLowerEndpoint();
        if (lowInt == IntervalEndpointType.CLOSED) {
            dateList.add(lData);
        }
        AbsoluteDate next = lData;
        double timeLeft;
        final IntervalEndpointType upInt = this.getUpperEndpoint();
        // Fill the list with the dates
        while (true) {
            next = next.shiftedBy(step);
            timeLeft = upData.durationFrom(next);
            if (timeLeft > 0) {
                dateList.add(next);
            } else if (timeLeft == 0 && upInt == IntervalEndpointType.CLOSED) {
                dateList.add(next);
                break;
            } else {
                break;
            }

        }
        return dateList;
    }
    
    /**
     * Returns a list of dates constructed from the interval evenly distributed in the interval.
     * It takes into account if the interval is open or closed.
     * Example: For n = 3
     *          [0, 1]   => 0, 0.5, 1
     *          [0, 1[   => 0, 0.33, 0.66
     *          ]0, 1[   => 0.25, 0.5, 0.75
     * 
     * @param n
     *        number of dates in the list to return
     * @return list of dates 
     * @throws MathIllegalArgumentException
     *         when n is smaller than 2
     *         when lower or upper data is infinity
     */
    public List<AbsoluteDate> getDateList(final int n) {
        // n must be minimum 2
        if (n < 2) {
            throw new MathIllegalArgumentException(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN);
        }
        // data should not be infinity
        final AbsoluteDate lData = this.getLowerData();
        final AbsoluteDate upData = this.getUpperData();
        if (lData == AbsoluteDate.PAST_INFINITY || upData == AbsoluteDate.FUTURE_INFINITY) {
            throw new MathIllegalArgumentException(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN);
        }

        // Number of intervals
        int nIntervals = n - 1;
        final IntervalEndpointType lowInt = this.getLowerEndpoint();
        final List<AbsoluteDate> dateList = new ArrayList<>(n); // New list of dates
        // Check if the interval is closed
        if (lowInt == IntervalEndpointType.OPEN) {
            nIntervals += 1;
        } else {
            dateList.add(lData);
        }
        final IntervalEndpointType upInt = this.getUpperEndpoint();
        if (upInt == IntervalEndpointType.OPEN) {
            nIntervals += 1;
        }
        final double totalDuration = upData.durationFrom(lData);
        // Compute the step from the number of values in the interval
        final double step = totalDuration / nIntervals;

        AbsoluteDate next = lData;
        // Fill the list with the dates
        while (dateList.size() < n) {
            next = next.shiftedBy(step);
            dateList.add(next);
        }
        return dateList;
    }
    
    /**
     * Check whether the given date is contained in the current interval.
     * 
     * @param dateIn date to check
     * @return boolean: if true, the given date is contained in the interval; if false, it is not
     */
    public boolean contains(final AbsoluteDate dateIn) {
        // Define boolean to return
        final boolean containsDateIn;
        // Compare given date with respect to lower data
        final int dateInWrtLowerData = dateIn.compareTo(this.getLowerData());
        // Compare given date with respect to upper data
        final int dateInWrtUpperData = dateIn.compareTo(this.getUpperData());
        // Check if given date is contained in the interval
        if (dateInWrtLowerData > 0 && dateInWrtUpperData < 0) {
            containsDateIn = true;
            // Check if lower end-point is contained in the interval and if given date coincides with it
        } else if (this.getLowerEndpoint() == IntervalEndpointType.CLOSED && dateInWrtLowerData == 0) {
            containsDateIn = true;
            // Check if upper end-point is contained in the interval and if given date coincides with it
        } else if (this.getUpperEndpoint() == IntervalEndpointType.CLOSED && dateInWrtUpperData == 0) {
            containsDateIn = true;
            // Given date is not contained in the interval
        } else {
            containsDateIn = false;
        }

        return containsDateIn;
    }
}
