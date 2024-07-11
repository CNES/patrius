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
 * @history 29/04/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class represents a piecewise 2nd order polynomial function of date.<br/>
 * The function is defined by an initial condition (x0, xDot0) at a date t[0] and all second derivatives xDotDot[i] on
 * intervals [t[i],t[i+1]]. For date out of bounds, 2nd derivative is considered as zero.
 * 
 * @concurrency thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: DateIntervalParabolicFunction.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.0
 */
public class DateIntervalParabolicFunction extends AbstractDateIntervalFunction {

    /** Serial UID. */
    private static final long serialVersionUID = 7312268828212459079L;

    /** Value at first date. */
    private final double x0;

    /** 1st derivative at first date. */
    private final double xDot0;

    /** 2nd order derivatives on date intervals. */
    private final double[] xDotDotIntervals;

    /**
     * Constructor.
     * 
     * @param ax0
     *        value at first date t[0]
     * @param axDot0
     *        1st derivative at first date t[0]
     * @param timeIntervals
     *        date intervals (must be chronologically sorted)
     * @param axDotDotIntervals
     *        2nd order derivatives on date intervals (size of vector must be of
     *        timeIntervals size - 1)
     */
    public DateIntervalParabolicFunction(final double ax0, final double axDot0, final AbsoluteDate[] timeIntervals,
        final double[] axDotDotIntervals) {
        super(timeIntervals);
        this.x0 = ax0;
        this.xDot0 = axDot0;
        this.xDotDotIntervals = axDotDotIntervals.clone();

        this.checkCoherence();
    }

    /**
     * Clone constructor.
     * 
     * @param function
     *        function to copy
     */
    public DateIntervalParabolicFunction(final DateIntervalParabolicFunction function) {
        super(function);
        this.x0 = function.getX0();
        this.xDot0 = function.getxDot0();
        this.xDotDotIntervals = function.getxDotDotIntervals();
    }

    /**
     * Check validity of user inputs.
     */
    private void checkCoherence() {
        // Check array sizes
        if (this.getDateIntervals().length - 1 != this.xDotDotIntervals.length) {
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
     * Returns first derivative value at first date.
     * 
     * @return first derivative value at first date
     */
    public double getxDot0() {
        return this.xDot0;
    }

    /**
     * Returns a copy of 1st order derivatives on date intervals.
     * 
     * @return 1st order derivatives on date intervals
     */
    public double[] getxDotDotIntervals() {
        return this.xDotDotIntervals.clone();
    }

    /** {@inheritDoc} */
    @Override
    public double value(final AbsoluteDate t) {

        double res;

        // Get index of interval date belongs to
        final int k = this.getIndexInterval(t);

        if (k == -1) {
            // Case date is before first date: 2nd order derivative is considered as 0 and 1st order derivative is used
            final double deltaT = t.durationFrom(this.getDateIntervals()[0]);
            res = this.x0 + this.xDot0 * deltaT;
        } else {
            // Generic case
            res = this.x0;
            double xDot = this.xDot0;

            for (int i = 1; i <= k; i++) {
                final double deltaT = this.getDateIntervals()[i].durationFrom(this.getDateIntervals()[i - 1]);
                // Value and its 1st order derivative at i-th date
                res += xDot * deltaT + this.xDotDotIntervals[i - 1] * deltaT * deltaT / 2.0;
                xDot = xDot + this.xDotDotIntervals[i - 1] * deltaT;
            }

            if (k == this.getDateIntervals().length - 1) {
                // Case date is after first date: 2nd order derivative is considered as 0 and last 1st order derivative
                // is used
                final double deltaT = t.durationFrom(this.getDateIntervals()[k]);
                res += xDot * deltaT;
            } else {
                // Generic case
                final double deltaT = t.durationFrom(this.getDateIntervals()[k]);
                res += xDot * deltaT + this.xDotDotIntervals[k] * deltaT * deltaT / 2.0;
            }
        }

        return res;
    }
}
