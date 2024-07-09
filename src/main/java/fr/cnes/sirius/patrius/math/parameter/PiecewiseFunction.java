/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * @history creation 01/09/2014
 */
/* 
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/09/2014:create a class to define parameterizable piecewize function.
 * VERSION::FA:411:10/02/2015:javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is used to define parameterizable piecewize function. It is defined by a list of
 * {@link IParamDiffFunction}, each sub function applying to a certain interval defined by the first
 * date of the interval.
 * <p>
 * First interval from –infinity to the first date. Last interval from the last date to + infinity.
 * The interval structure is defined as [k, k+1[ (first bracket close, second open)
 * </p>
 * <p>
 * Each function parameters descriptor is enriched with a
 * {@link StandardFieldDescriptors#DATE_INTERVAL} corresponding to the interval where the parameter
 * is defined.
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

    /** SerialVersionUID. */
    private static final long serialVersionUID = -4651028856848403142L;

    /**
     * Simple constructor with 2 lists ({@link IParamDiffFunction} and {@link AbsoluteDate}) where
     * the dates list represents the connection points between functions, with an interval form
     * [closed; opened[.
     * <p>
     * So dates list size must equals to functions list size - 1 else exception is thrown.
     * </p>
     * <p>
     * The function is defined between {@link AbsoluteDate#PAST_INFINITY} and
     * {@link AbsoluteDate#FUTURE_INFINITY}, such as the <i>N</i> intervals <i>[PAST_INFINITY;
     * date1[, [date1, date2[, [...[, [dateN, FUTURE_INFINITY[</i> are defined from the N-1 dates
     * for the N piecewise functions.
     * </p>
     * <p>
     * Each function parameters descriptor is enriched with a
     * {@link StandardFieldDescriptors#DATE_INTERVAL} corresponding to the interval where the
     * parameter is defined.
     * </p>
     *
     * @param flist
     *        list of functions
     * @param xlist
     *        list of dates ordered chronologically
     * @exception PatriusException
     *            if the dates list contains {@link AbsoluteDate#PAST_INFINITY} or
     *            {@link AbsoluteDate#FUTURE_INFINITY}</br>
     *            if the dates aren't ordered chronologically in the list</br>
     *            if the list contains the same date twice
     * @throws DimensionMismatchException
     *         if {@code xlist.size() != flist.size() - 1}
     */
    public PiecewiseFunction(final List<IParamDiffFunction> flist, final List<AbsoluteDate> xlist)
            throws PatriusException {
        super(flist, buildIntervals(flist, xlist));
    }

    /**
     * Private service to check the input data consistency and to build the intervals from the dates
     * list.
     *
     * @param flist
     *        list of IParamDiffFunction
     * @param xlist
     *        list of AbsoluteDate ordered chronologically
     * @return the intervals defining the piecewise function (size N, for N functions)
     * @throws PatriusException
     *         if the dates list contains {@link AbsoluteDate#PAST_INFINITY} or
     *         {@link AbsoluteDate#FUTURE_INFINITY}</br>
     *         if the dates aren't ordered chronologically in the list</br>
     *         if the list contains the same date twice
     * @throws DimensionMismatchException
     *         if {@code xlist.size() != flist.size() - 1}
     * 
     */
    private static List<AbsoluteDateInterval> buildIntervals(final List<IParamDiffFunction> flist,
            final List<AbsoluteDate> xlist) throws PatriusException {

        // Check AbsoluteDate.PAST_INFINITY & AbsoluteDate.FUTURE_INFINITY aren't in the dates list
        if (xlist.contains(AbsoluteDate.PAST_INFINITY)) {
            throw new PatriusException(PatriusMessages.PAST_INFINITY_DATE_NOT_ALLOWED);
        }
        if (xlist.contains(AbsoluteDate.FUTURE_INFINITY)) {
            throw new PatriusException(PatriusMessages.FUTURE_INFINITY_DATE_NOT_ALLOWED);
        }

        // Check for size consistency
        if (xlist.size() != flist.size() - 1) {
            throw new DimensionMismatchException(xlist.size(), flist.size() - 1);
        }

        // Check the order of xlist or if it doesn't contain the same date twice
        for (int i = 0; i < (xlist.size() - 1); i++) {
            final int result = xlist.get(i + 1).compareTo(xlist.get(i));
            if (result == 0.) {
                throw new PatriusException(PatriusMessages.DUPLICATED_ELEMENT, xlist.get(i));
            }
            if (result < 0.) {
                throw new PatriusException(PatriusMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES,
                        xlist.get(i), xlist.get(i + 1));
            }
        }

        // Build the intervals list
        final IntervalEndpointType open = IntervalEndpointType.OPEN;
        final IntervalEndpointType closed = IntervalEndpointType.CLOSED;

        final int intervalsSize = xlist.size() - 1; // End-point of the "for" loop
        // Take into account the first & last intervals
        final List<AbsoluteDateInterval> intervals = new ArrayList<>(intervalsSize + 2);

        intervals
                .add(new AbsoluteDateInterval(open, AbsoluteDate.PAST_INFINITY, xlist.get(0), open));
        for (int i = 0; i < intervalsSize; i++) {
            intervals.add(new AbsoluteDateInterval(closed, xlist.get(i), xlist.get(i + 1), open));
        }
        intervals.add(new AbsoluteDateInterval(closed, xlist.get(xlist.size() - 1),
                AbsoluteDate.FUTURE_INFINITY, open));

        return intervals;
    }
}
