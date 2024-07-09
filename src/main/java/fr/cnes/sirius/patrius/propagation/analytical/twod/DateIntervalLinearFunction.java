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
 * This class represents a piecewise linear function of date.<br/>
 * The function is defined by an initial condition x0 at a date t[0] and all first derivatives xDot[i] on
 * intervals [t[i],t[i+1]]. For date out of bounds, 1st derivative is considered as zero.
 * 
 * @concurrency thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DateIntervalLinearFunction.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.0
 */
public class DateIntervalLinearFunction extends AbstractDateIntervalFunction {

    /** Serial UID. */
    private static final long serialVersionUID = -3526376846656376344L;

    /** Value at first date. */
    private final double x0;

    /** 1st order derivatives on date intervals. */
    private final double[] xDotIntervals;

    /**
     * Constructor.
     * 
     * @param ax0
     *        value at first date t[0]
     * @param timeIntervals
     *        date intervals (must be chronologically sorted)
     * @param axDotIntervals
     *        1st order derivatives on date intervals (size of vector must be of timeIntervals size - 1)
     */
    public DateIntervalLinearFunction(final double ax0, final AbsoluteDate[] timeIntervals,
        final double[] axDotIntervals) {
        super(timeIntervals);
        this.x0 = ax0;
        this.xDotIntervals = axDotIntervals.clone();

        this.checkCoherence();
    }

    /**
     * Clone constructor.
     * 
     * @param function
     *        function to copy
     */
    public DateIntervalLinearFunction(final DateIntervalLinearFunction function) {
        super(function);
        this.x0 = function.getX0();
        this.xDotIntervals = function.getxDotIntervals();
    }

    /**
     * Check validity of user inputs.
     */
    private void checkCoherence() {
        // Check array sizes
        if (this.getDateIntervals().length - 1 != this.xDotIntervals.length) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns value at first date.
     * 
     * @return x0 value at first date
     */
    public double getX0() {
        return this.x0;
    }

    /**
     * Returns a copy of 1st order derivatives on date intervals.
     * 
     * @return 1st order derivatives on date intervals
     */
    public double[] getxDotIntervals() {
        return this.xDotIntervals.clone();
    }

    /** {@inheritDoc} */
    @Override
    public double value(final AbsoluteDate t) {

        double res;

        // Get index of interval date belongs to
        final int k = this.getIndexInterval(t);

        if (k == -1) {
            // Case date is before first date: 1st derivative is considered as 0 and x0 is returned
            res = this.x0;

        } else if (k == this.getDateIntervals().length - 1) {
            // Case date after last date: 1st derivative is considered as 0 and last value is returned
            res = this.value(this.getDateIntervals()[this.getDateIntervals().length - 1]);

        } else {
            // Generic case
            res = this.x0;
            for (int i = 1; i <= k; i++) {
                // Value at i-th date
                res +=
                    this.xDotIntervals[i - 1] * this.getDateIntervals()[i].durationFrom(this.getDateIntervals()[i - 1]);
            }

            // Value at final date
            res += this.xDotIntervals[k] * (t.durationFrom(this.getDateIntervals()[k]));
        }

        return res;
    }
}
