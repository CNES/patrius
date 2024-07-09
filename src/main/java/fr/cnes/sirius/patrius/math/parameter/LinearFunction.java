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
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/09/2014:create a class to define parameterizable linear function.
 * VERSION::FA:411:10/02/2015:javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class is used to define parameterizable linear function: <i>f = a0 + a1 * (t - t0)</i>.
 *
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 *
 * @author auguief, bonitt, veuillezh
 * @since 2.3
 * @version $Id: LinearFunction.java 18069 2017-10-02 16:45:28Z bignon $
 */
public class LinearFunction extends LinearCombinationFunction {

    /** SerialVersionUID. */
    private static final long serialVersionUID = -1536194879895016776L;

    /**
     * Constructor of a linear function <i>f = a0 + a1 * (t - t0)</i> using the input a1 (slope) and
     * a0 (zero value) values and the initial date.
     * <p>
     * Note: the parameters are stored in the following order: [a0, a1].
     * </p>
     *
     * @param t0
     *        initial date
     * @param a0
     *        the a0 parameter value (function's value when t=0)
     * @param a1
     *        the a1 parameter value (slope)
     * @throws NullArgumentException if {@code t0} is {@code null}
     */
    public LinearFunction(final AbsoluteDate t0, final double a0, final double a1) {
        this(t0, new Parameter(PARAMETER_PREFIX_NAME + "0", a0), new Parameter(
                PARAMETER_PREFIX_NAME + "1", a1));
    }

    /**
     * Constructor of a linear function: <i>f = a0 + a1 * (t - t0)</i> using the input a1 (slope)
     * and a0 (zeroValue) parameters and initial date.
     * <p>
     * Note: the parameters are stored in the following order: [a0, a1] (also called [zeroValue,
     * slope]).
     * </p>
     *
     * @param t0
     *        initial date
     * @param a0
     *        the a0 parameter (function's value when t=0)
     * @param a1
     *        the a1 parameter (slope)
     * @throws NullArgumentException if {@code t0}, {@code a0} or {@code a1} is {@code null}
     */
    public LinearFunction(final AbsoluteDate t0, final Parameter a0, final Parameter a1) {
        super(constructSuperClassMap(t0, a0, a1));
    }

    /**
     * Initialize the linear function with the form <i>f = a0 + a1 * (t - t0)</i>.
     * <p>
     * Note: the parameters are stored in the following order: [a0, a1].
     * </p>
     *
     * @param t0
     *        initial date
     * @param a0
     *        the a0 parameter (function's value when t=0)
     * @param a1
     *        the a1 parameter (slope)
     * @return the linear (first order polynomial) function
     * @throws NullArgumentException if {@code t0}, {@code a0} or {@code a1} is {@code null}
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: preserve insertion order
    private static final Map<Parameter, Function<SpacecraftState, Double>> constructSuperClassMap(
            final AbsoluteDate t0, final Parameter a0, final Parameter a1) {
        // Check for null attribute
        if ((t0 == null) || (a0 == null) || (a1 == null)) {
            throw new NullArgumentException();
        }

        // Build the function map (LinkedHashMap to guarantee order)
        final Map<Parameter, Function<SpacecraftState, Double>> map = new LinkedHashMap<>(2);
        map.put(a0, state -> 1.0);
        map.put(a1, state -> state.getDate().durationFrom(t0));
        return map;
    }

    /**
     * Return the function parameters in this following order: [a0, a1].
     * <p>
     * The list is returned in a shallow copy.
     * </p>
     *
     * @return the function parameters list
     */
    @Override
    @SuppressWarnings("PMD.LooseCoupling")
    public ArrayList<Parameter> getParameters() {
        // Implementation note: override in order to have a specific javadoc
        return new ArrayList<>(this.functions.keySet());
    }
}
