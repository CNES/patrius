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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.interval.ComparableInterval;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is used to define parameterizable interval functions. It is defined by a map of
 * {@link AbsoluteDateInterval} and {@link IParamDiffFunction}.
 * <p>
 * Note that this function is not defined on dates outside of its {@link AbsoluteDateInterval
 * intervals}.
 * </p>
 * <p>
 * Each function parameters descriptor is enriched with a
 * {@link StandardFieldDescriptors#DATE_INTERVAL} corresponding to the interval where the parameter
 * is defined.
 * </p>
 *
 * @author bonitt
 */
public class IntervalsFunction extends Parameterizable implements IParamDiffFunction {

     /** Serializable UID. */
    private static final long serialVersionUID = 4240076365289388871L;

    /** Map of intervals and functions. */
    @SuppressWarnings("PMD.LooseCoupling")
    private final TreeMap<ComparableInterval<AbsoluteDate>, IParamDiffFunction> mapOfFunctions;

    /*
     * Implementation note: the map uses ComparableInterval(AbsoluteDate) keys instead of
     * AbsoluteDateInterval because it's less computational intensive to instantiate this type in
     * the method IntervalsFunction#searchFunctionToUse(AbsoluteDate) which is called very often.
     * This technical choice has shown valuable gains on performances.
     */

    /**
     * Simple constructor with a list of {@link IParamDiffFunction functions} and a list of
     * {@link AbsoluteDateInterval intervals}. Both lists should be the same size. They define each
     * sub-function to apply on each interval. The intervals must not overlap with each other.
     * </p>
     * <p>
     * Each function parameters descriptor is enriched with a
     * {@link StandardFieldDescriptors#DATE_INTERVAL} corresponding to the interval where the
     * parameter is defined.
     * </p>
     *
     * @param functionsList
     *        List of functions
     * @param intervalsList
     *        List of intervals
     * @throws DimensionMismatchException if {@code functionsList.size() != intervalsList.size()}
     * @throws IllegalArgumentException if some intervals from the specified list overlap with each
     *         other
     */
    public IntervalsFunction(final List<IParamDiffFunction> functionsList,
            final List<AbsoluteDateInterval> intervalsList) {
        super();

        final int functionsListSize = functionsList.size();

        // Check for size consistency
        if (functionsListSize != intervalsList.size()) {
            throw new DimensionMismatchException(functionsListSize, intervalsList.size());
        }

        // Build the map
        this.mapOfFunctions = new TreeMap<>();

        for (int i = 0; i < functionsListSize; i++) {
            this.mapOfFunctions.put(intervalsList.get(i), functionsList.get(i));
        }

        // Check none interval overlaps another one
        checkOverlap(this.mapOfFunctions);

        // Update the parameters descriptors with the intervals
        updateParametersDescriptors(this.mapOfFunctions);

        // Add the parameters into the parameters list
        final Collection<IParamDiffFunction> functions = this.mapOfFunctions.values();
        for (final IParamDiffFunction function : functions) {
            this.addAllParameters(function.getParameters());
        }
    }

    /**
     * Simple constructor to initialize directly the map of intervals and functions.
     * <p>
     * Each function parameters descriptor is enriched with a
     * {@link StandardFieldDescriptors#DATE_INTERVAL} corresponding to the interval where the
     * parameter is defined.
     * </p>
     * <p>
     * Note: the given map is stored with a shallow copy.
     * </p>
     *
     * @param mapOfFunctions
     *        Map of intervals and functions. Note that the map is not duplicated internally.
     * @throws IllegalArgumentException if some intervals overlap with each other
     */
    public IntervalsFunction(final Map<AbsoluteDateInterval, IParamDiffFunction> mapOfFunctions) {
        super();

        // Set the map
        this.mapOfFunctions = new TreeMap<>();
        this.mapOfFunctions.putAll(mapOfFunctions);

        // Check none interval overlaps an other one
        checkOverlap(this.mapOfFunctions);

        // Update the parameters descriptors with the intervals
        updateParametersDescriptors(this.mapOfFunctions);

        // Add the parameters into the parameters list
        final Collection<IParamDiffFunction> functions = this.mapOfFunctions.values();
        for (final IParamDiffFunction function : functions) {
            this.addAllParameters(function.getParameters());
        }
    }

    /**
     * Evaluate each intervals couples to determine if they overlap (overlapping isn't allowed).
     * <p>
     * Note: The given intervals stored in a TreeMap are already ordered chronologically.
     * </p>
     *
     * @param map
     *        Map of intervals and functions
     * @throws IllegalArgumentException if some intervals overlap with each other
     */
    @SuppressWarnings("PMD.LooseCoupling")
    private static void checkOverlap(
            final TreeMap<ComparableInterval<AbsoluteDate>, IParamDiffFunction> map) {

        final Set<ComparableInterval<AbsoluteDate>> intervalsSet = map.keySet();

        final Iterator<ComparableInterval<AbsoluteDate>> it = intervalsSet.iterator();
        ComparableInterval<AbsoluteDate> currentInterval = it.next();
        while (it.hasNext()) {
            final ComparableInterval<AbsoluteDate> nextInterval = it.next();
            if (currentInterval.overlaps(nextInterval)) {
                throw PatriusException
                        .createIllegalArgumentException(PatriusMessages.INTERVALS_OVERLAPPING_NOT_ALLOWED);
            }
            currentInterval = nextInterval;
        }
    }

    /**
     * Enrich the parameters descriptors with the interval.
     * <p>
     * For each interval key, the function's parameters get a new parameter descriptor with the
     * current interval.
     * </p>
     *
     * @param map
     *        parameters map to update
     */
    @SuppressWarnings("PMD.LooseCoupling")
    private static void updateParametersDescriptors(
            final TreeMap<ComparableInterval<AbsoluteDate>, IParamDiffFunction> map) {

        // Loop on each map entry to extract all the parameters for each interval
        for (final Entry<ComparableInterval<AbsoluteDate>, IParamDiffFunction> entry : map
                .entrySet()) {
            final AbsoluteDateInterval currentInterval = new AbsoluteDateInterval(entry.getKey());
            final IParamDiffFunction currentFunction = entry.getValue();
            final List<Parameter> parameters = currentFunction.getParameters();

            // Loop on all parameters and update their descriptor with the current interval, then
            // add them in the Parameterizable list
            for (final Parameter parameter : parameters) {
                final ParameterDescriptor descriptor = parameter.getDescriptor();
                if (descriptor.isMutable()) {
                    descriptor.addField(StandardFieldDescriptors.DATE_INTERVAL, currentInterval);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the given date isn't contained by the intervals
     */
    @Override
    public double value(final SpacecraftState s) {
        // Find the interval and the function to use, then compute the value
        return this.searchFunctionToUse(s.getDate()).value(s);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the given date isn't contained by the intervals
     */
    @Override
    public double derivativeValue(final Parameter p, final SpacecraftState s) {
        // Find the interval and the function to use, then compute the derivative
        return this.searchFunctionToUse(s.getDate()).derivativeValue(p, s);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return this.supportsParameter(p);
    }

    /**
     * Private service to find the function to use.
     *
     * @param date
     *        current date
     * @return function to use in the map of functions
     * @throws IllegalStateException if the given date isn't contained by the intervals
     */
    private IParamDiffFunction searchFunctionToUse(final AbsoluteDate date) {

        /*
         * Implementation note: this method uses the TreeMap#floorEntry(Object) intervals searching
         * method as its complexity is O(log(n)) instead of a classic search with a linear approach
         * of
         * O(n).
         * Some benchmarks have been made and have shown good performance improvements, especially
         * when dealing with many intervals.
         */

        // Build the interval ]date ; +Infinity[ for searching in the map
        final ComparableInterval<AbsoluteDate> dateInt = new ComparableInterval<>(
                IntervalEndpointType.CLOSED, date, AbsoluteDate.FUTURE_INFINITY,
                IntervalEndpointType.OPEN);

        // Search for the floor entry
        // In other words: the greatest key <= ]date ; +Infinity[ (if doesn't exist -> null)
        final Entry<ComparableInterval<AbsoluteDate>, IParamDiffFunction> entryOut = this.mapOfFunctions
                .floorEntry(dateInt);

        // Check if an entry has been found and that the given date is actually contained in the
        // found interval (could be beyond)
        if ((entryOut == null) || !entryOut.getKey().contains(date)) {
            // The function is not defined at this date
            throw PatriusException
                    .createIllegalStateException(PatriusMessages.DATE_OUTSIDE_INTERVAL);
        }
        return entryOut.getValue();
    }
}
