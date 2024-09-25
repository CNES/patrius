/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * @history creation 01/09/2014
 */
/* 
 * HISTORY
* VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/09/2014:create a class to define parameterizable piecewize function.
 * VERSION::FA:411:10/02/2015:javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is used to define parameterizable piecewize function.
 * <p>
 * It is defined by a collection of {@link IParamDiffFunction functions}, each sub function applying to a certain
 * interval defined by the first date of the interval.
 * </p>
 * <p>
 * First interval from PAST_INFINITY to the first date.<br>
 * Last interval from the last date to FUTURE_INFINITY.<br>
 * The interval structure is defined as [k, k+1[ (first bracket closed, second opened).
 * </p>
 * <p>
 * Each function parameters descriptor is enriched with a {@link StandardFieldDescriptors#DATE_INTERVAL} corresponding
 * to the interval where the parameter is defined.
 * </p>
 *
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 *
 * @author auguief
 * @since 2.3
 * @version $Id: PiecewiseFunction.java 18069 2017-10-02 16:45:28Z bignon $
 */
public class PiecewiseFunction extends IntervalsFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = -4651028856848403142L;

    /**
     * Simple constructor with a collection of {@link IParamDiffFunction functions} and a collection of
     * {@link AbsoluteDate dates} where the dates represents the connection points between functions,
     * with an interval form [closed; opened[.
     * <p>
     * So, the collection of dates size must be equal to the collection of functions size - 1, else an exception is
     * thrown.
     * </p>
     * <p>
     * The function is defined between {@link AbsoluteDate#PAST_INFINITY} and {@link AbsoluteDate#FUTURE_INFINITY}, such
     * as the <i>N</i> intervals <i>[PAST_INFINITY; date1[, [date1, date2[, [...[, [dateN, FUTURE_INFINITY[</i> are
     * defined from the N-1 dates for the N piecewise functions.
     * </p>
     * <p>
     * Each function parameters descriptor is enriched with a {@link StandardFieldDescriptors#DATE_INTERVAL}
     * corresponding to the interval where the parameter is defined.
     * </p>
     *
     * @param functionsCollection
     *        Collection of functions
     * @param datesCollection
     *        Collection of dates, chronologically ordered
     * @throws PatriusException
     *         if the collection of dates contains {@link AbsoluteDate#PAST_INFINITY} or
     *         {@link AbsoluteDate#FUTURE_INFINITY}<br>
     *         if the collection of dates isn't chronologically ordered<br>
     *         if the collection of dates contains the same date twice
     * @throws DimensionMismatchException
     *         if {@code datesCollection.size() != functionsCollection.size() - 1}
     */
    public PiecewiseFunction(final Collection<IParamDiffFunction> functionsCollection,
                             final Collection<AbsoluteDate> datesCollection)
        throws PatriusException {
        super(functionsCollection, buildIntervals(functionsCollection, datesCollection));
    }

    /**
     * Private service to check the input data consistency and to build the intervals from the collection of dates.
     *
     * @param functionsCollection
     *        Collection of functions
     * @param datesCollection
     *        Collection of dates, chronologically ordered
     * @return the intervals defining the piecewise function (size N, for N functions)
     * @throws PatriusException
     *         if the collection of dates contains {@link AbsoluteDate#PAST_INFINITY} or
     *         {@link AbsoluteDate#FUTURE_INFINITY}<br>
     *         if the collection of dates isn't chronologically ordered<br>
     *         if the collection of dates contains the same date twice
     * @throws DimensionMismatchException
     *         if {@code datesCollection.size() != functionsCollection.size() - 1}
     * 
     */
    private static List<AbsoluteDateInterval> buildIntervals(final Collection<IParamDiffFunction> functionsCollection,
                                                             final Collection<AbsoluteDate> datesCollection)
        throws PatriusException {

        // Check AbsoluteDate.PAST_INFINITY & AbsoluteDate.FUTURE_INFINITY aren't in the collection of dates
        if (datesCollection.contains(AbsoluteDate.PAST_INFINITY)) {
            throw new PatriusException(PatriusMessages.PAST_INFINITY_DATE_NOT_ALLOWED);
        }
        if (datesCollection.contains(AbsoluteDate.FUTURE_INFINITY)) {
            throw new PatriusException(PatriusMessages.FUTURE_INFINITY_DATE_NOT_ALLOWED);
        }

        // Check for size consistency
        if (datesCollection.size() != functionsCollection.size() - 1) {
            throw new DimensionMismatchException(datesCollection.size(), functionsCollection.size() - 1);
        }

        // Check the collection of dates is chronologically ordered or if it doesn't contain the same date twice
        Iterator<AbsoluteDate> iterator = datesCollection.iterator();
        AbsoluteDate previousDate = iterator.next(); // First date
        while (iterator.hasNext()) {
            final AbsoluteDate currentDate = iterator.next();
            // Compare the current date to the previous date
            final int res = currentDate.compareTo(previousDate);
            if (res == 0) {
                // The dates are the same
                throw new PatriusException(PatriusMessages.DUPLICATED_ELEMENT, previousDate);
            }
            if (res < 0) {
                // The dates aren't chronologically ordered
                throw new PatriusException(PatriusMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES, previousDate,
                    currentDate);
            }
            previousDate = currentDate;
        }

        // Build the intervals list
        final IntervalEndpointType open = IntervalEndpointType.OPEN;
        final IntervalEndpointType closed = IntervalEndpointType.CLOSED;

        final int intervalsSize = datesCollection.size() - 1; // End-point of the "for" loop
        // Take into account the first & last intervals
        final List<AbsoluteDateInterval> intervals = new ArrayList<>(intervalsSize + 2);

        iterator = datesCollection.iterator();
        previousDate = iterator.next(); // First date
        intervals.add(new AbsoluteDateInterval(open, AbsoluteDate.PAST_INFINITY, previousDate, open)); // First interval
        while (iterator.hasNext()) {
            final AbsoluteDate currentDate = iterator.next();
            intervals.add(new AbsoluteDateInterval(closed, previousDate, currentDate, open));
            previousDate = currentDate;
        }
        // Last interval
        intervals.add(new AbsoluteDateInterval(closed, previousDate, AbsoluteDate.FUTURE_INFINITY, open));

        return intervals;
    }
}
