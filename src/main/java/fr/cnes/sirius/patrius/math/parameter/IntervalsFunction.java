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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.AbsoluteDateIntervalsList;
import fr.cnes.sirius.patrius.time.IntervalMapSearcher;
import fr.cnes.sirius.patrius.tools.cache.CacheEntry;

/**
 * This class is used to define parameterizable interval functions. It is defined by a map of
 * {@link AbsoluteDateInterval} and {@link IParamDiffFunction}.
 * <p>
 * Note that this function is not defined on dates outside of its {@link AbsoluteDateInterval
 * intervals}.
 * </p>
 * <p>
 * Each function parameters descriptor is enriched with a {@link StandardFieldDescriptors#DATE_INTERVAL} corresponding
 * to the interval where the parameter is defined.
 * </p>
 *
 * @author bonitt
 */
public class IntervalsFunction extends Parameterizable implements IParamDiffFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = 4240076365289388871L;

    /** Efficient functions searcher. */
    private final IntervalMapSearcher<IParamDiffFunction> functionsSearcher;

    /**
     * Simple constructor with a collection of {@link IParamDiffFunction functions} and a collection of
     * {@link AbsoluteDateInterval intervals}. Both collections should be the same size. They define each
     * sub-function to apply on each interval. The intervals must not overlap with each other.
     * </p>
     * <p>
     * Each function parameters descriptor is updated with a {@link StandardFieldDescriptors#DATE_INTERVAL}
     * corresponding to the interval where the parameter is defined.
     * </p>
     *
     * @param functionsCollection
     *        Collection of functions
     * @param intervalsCollection
     *        Collection of intervals
     * @throws DimensionMismatchException
     *         if {@code intervalsCollection.size() != functionsCollection.size()}
     * @throws IllegalArgumentException
     *         if some intervals from the specified collection overlap with each other<br>
     *         if one of the functions of the functions collection is {@code null}
     */
    public IntervalsFunction(final Collection<IParamDiffFunction> functionsCollection,
                             final Collection<AbsoluteDateInterval> intervalsCollection) {
        this(functionsCollection, intervalsCollection, true);
    }

    /**
     * Simple constructor with a collection of {@link IParamDiffFunction functions} and a collection of
     * {@link AbsoluteDateInterval intervals}. Both collections should be the same size. They define each
     * sub-function to apply on each interval. The intervals must not overlap with each other.
     *
     * @param functionsCollection
     *        Collection of functions
     * @param intervalsCollection
     *        Collection of intervals
     * @param updateParamDescriptors
     *        Indicate if parameters descriptor should be updated with a {@link StandardFieldDescriptors#DATE_INTERVAL}
     *        corresponding to the interval where the parameter is defined.
     * @throws DimensionMismatchException
     *         if {@code intervalsCollection.size() != functionsCollection.size()}
     * @throws IllegalArgumentException
     *         if some intervals from the specified collection overlap with each other<br>
     *         if one of the functions of the functions collection is {@code null}
     */
    public IntervalsFunction(final Collection<IParamDiffFunction> functionsCollection,
                             final Collection<AbsoluteDateInterval> intervalsCollection,
                             final boolean updateParamDescriptors) {
        super();

        this.functionsSearcher = new IntervalMapSearcher<>(intervalsCollection, functionsCollection);
        updateParametersDescriptorsAndParameterizableList(updateParamDescriptors);
    }

    /**
     * Simple constructor to initialize directly the map of intervals and functions.
     * <p>
     * Each function parameters descriptor is enriched with a {@link StandardFieldDescriptors#DATE_INTERVAL}
     * corresponding to the interval where the parameter is defined.
     * </p>
     * <p>
     * Note: the given map is stored with a shallow copy.
     * </p>
     *
     * @param mapOfFunctions
     *        Map of intervals and functions. Note that the map is not duplicated internally.
     * @throws IllegalArgumentException
     *         if some intervals from the specified map overlap with each other<br>
     *         if one of the objects of the map is {@code null}
     * @throws NullArgumentException
     *         if {@code mapOfFunctions} is {@code null}
     */
    public IntervalsFunction(final Map<AbsoluteDateInterval, IParamDiffFunction> mapOfFunctions) {
        this(mapOfFunctions, true);
    }

    /**
     * Simple constructor to initialize directly the map of intervals and functions.
     * <p>
     * Note: the given map is stored with a shallow copy.
     * </p>
     *
     * @param mapOfFunctions
     *        Map of intervals and functions. Note that the map is not duplicated internally.
     * @param updateParamDescriptors
     *        Indicate if parameters descriptor should be updated with a {@link StandardFieldDescriptors#DATE_INTERVAL}
     *        corresponding to the interval where the parameter is defined.
     * @throws IllegalArgumentException
     *         if some intervals from the specified map overlap with each other<br>
     *         if one of the objects of the map is {@code null}
     * @throws NullArgumentException
     *         if {@code mapOfFunctions} is {@code null}
     */
    public IntervalsFunction(final Map<AbsoluteDateInterval, IParamDiffFunction> mapOfFunctions,
                             final boolean updateParamDescriptors) {
        super();

        this.functionsSearcher = new IntervalMapSearcher<>(mapOfFunctions);
        updateParametersDescriptorsAndParameterizableList(updateParamDescriptors);
    }

    /**
     * Enrich the parameters descriptors with the interval and add all the parameters to the {@link Parameterizable}
     * super implementation.
     * <p>
     * For each interval, the function's parameters get a new parameter descriptor with the current interval.
     * </p>
     *
     * @param updateParameterDescriptors
     *        Update the parameter descriptors with the intervals
     */
    private void updateParametersDescriptorsAndParameterizableList(final boolean updateParameterDescriptors) {

        // Loop on each cache entry to extract all the parameters for each interval
        for (final CacheEntry<AbsoluteDateInterval, IParamDiffFunction> entry : this.functionsSearcher) {
            final AbsoluteDateInterval currentInterval = new AbsoluteDateInterval(entry.getKey());
            final IParamDiffFunction currentFunction = entry.getValue();
            final List<Parameter> parameters = currentFunction.getParameters();

            // Add the parameters of the function to the list of parameters
            this.addAllParameters(parameters);

            if (updateParameterDescriptors) {
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
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *         if the provided date does not belong to any of the intervals
     */
    @Override
    public double value(final SpacecraftState s) {
        // Find the interval and the function to use, then compute the value
        return this.functionsSearcher.getData(s.getDate()).value(s);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *         if the provided date does not belong to any of the intervals
     */
    @Override
    public double derivativeValue(final Parameter p, final SpacecraftState s) {
        // Find the interval and the function to use, then compute the derivative
        return this.functionsSearcher.getData(s.getDate()).derivativeValue(p, s);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return this.supportsParameter(p);
    }

    /**
     * Getter for the available intervals.
     *
     * @return the available intervals
     */
    public AbsoluteDateIntervalsList getIntervals() {
        return this.functionsSearcher.getIntervals();
    }

    /**
     * Getter for the functions associated to the {@link AbsoluteDateInterval intervals}.
     *
     * @return the functions
     */
    public List<IParamDiffFunction> getFunctions() {
        return this.functionsSearcher.getData();
    }

    /**
     * Getter for the association between {@link AbsoluteDateInterval intervals} and functions.
     *
     * @return the interval/functions association
     */
    public Map<AbsoluteDateInterval, IParamDiffFunction> getIntervalFunctionAssociation() {
        return this.functionsSearcher.getIntervalDataAssociation();
    }

    /**
     * Getter for a String representation of this function.
     * 
     * @return a String representation of this function
     */
    @Override
    public String toString() {

        // Extract the information
        final Map<AbsoluteDateInterval, IParamDiffFunction> intervalFunctionMap = getIntervalFunctionAssociation();

        // Initialize the StringBuilder and the class information
        final StringBuilder strBuilder = new StringBuilder();
        final String className = this.getClass().getSimpleName() + ":";

        strBuilder.append(className);
        strBuilder.append('\n');

        // Loop on each Map value to store the result
        for (final Entry<AbsoluteDateInterval, IParamDiffFunction> intervalFunction : intervalFunctionMap.entrySet()) {
            final AbsoluteDateInterval interval = intervalFunction.getKey();
            final IParamDiffFunction function = intervalFunction.getValue();

            // Add the info in the StringBuilder
            strBuilder.append(interval);
            strBuilder.append(": ");
            strBuilder.append(function);
        }

        // Return the resulting String
        return strBuilder.toString();
    }
}
