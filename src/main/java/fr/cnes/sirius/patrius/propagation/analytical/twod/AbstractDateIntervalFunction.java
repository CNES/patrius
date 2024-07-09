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
 * @history 29/04/2015
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Abstract class for piecewise function of date.<br/>
 * 
 * @concurrency thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: AbstractDateIntervalFunction.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.0
 */
public abstract class AbstractDateIntervalFunction implements UnivariateDateFunction {

    /** Serial UID. */
    private static final long serialVersionUID = 2566814408153820802L;

    /** Date intervals. */
    private final AbsoluteDate[] dateIntervals;

    /**
     * Constructor.
     * 
     * @param timeIntervals
     *        date intervals (must be chronologically sorted)
     */
    public AbstractDateIntervalFunction(final AbsoluteDate[] timeIntervals) {
        this.dateIntervals = timeIntervals.clone();

        this.checkCoherence();
    }

    /**
     * Clone constructor.
     * 
     * @param function
     *        function to copy
     */
    public AbstractDateIntervalFunction(final AbstractDateIntervalFunction function) {
        this.dateIntervals = function.getDateIntervals();
    }

    /**
     * Check validity of user inputs.
     */
    private void checkCoherence() {

        // At least one date is necessary
        if (this.dateIntervals.length < 1) {
            throw new IllegalArgumentException();
        }

        // Check that date intervals are sorted by increasing date
        for (int i = 1; i < this.dateIntervals.length; i++) {
            if (this.dateIntervals[i].durationFrom(this.dateIntervals[i - 1]) <= 0) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Returns a copy of date intervals.
     * 
     * @return date intervals
     */
    public AbsoluteDate[] getDateIntervals() {
        return this.dateIntervals.clone();
    }

    /**
     * Returns index such as dateIntervals[k] <= date <= dateIntervals[k+1].
     * 
     * @param date
     *        a date
     * @return index such as dateIntervals[k] <= date <= dateIntervals[k+1], -1 if date is before dateIntervals[0],
     *         dateIntervals.length - 1 if date is after dateIntervals[dateIntervals.length - 1]
     */
    protected int getIndexInterval(final AbsoluteDate date) {

        int res = -1;

        if (date.durationFrom(this.dateIntervals[0]) < 0) {
            // Case date is before first date: -1 is returned
            res = -1;

        } else if (date.durationFrom(this.dateIntervals[this.dateIntervals.length - 1]) > 0) {
            // Case date is after last date: dateIntervals.length - 1 is returned
            res = this.dateIntervals.length - 1;

        } else {
            // Generic case: find interval such as dateIntervals[k] <= t <= dateIntervals[k+1]
            for (int i = 0; i < this.dateIntervals.length - 1; i++) {
                if (date.durationFrom(this.dateIntervals[i]) >= 0 
                        && date.durationFrom(this.dateIntervals[i + 1]) <= 0) {
                    res = i;
                    break;
                }
            }
        }

        return res;
    }
}
