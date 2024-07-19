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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;

/**
 * This class is used to define a linear combination of functions where the linear coefficients are
 * parameters:</br>
 * <i>g(sc) = p1*f1(sc) + p2*f2(sc) + ...</i>
 * <p>
 * Note: The child class have to cast their functions to {@link Serializable} if it should be serializable (not
 * required).
 * </p>
 * 
 * @author veuillezh, bonitt
 */
public class LinearCombinationFunction implements IParamDiffFunction {

    /** Standard parameter prefix name: "A". */
    protected static final String PARAMETER_PREFIX_NAME = "A";

     /** Serializable UID. */
    private static final long serialVersionUID = 2138447268552406913L;

    /** Map describing the functions. */
    protected final Map<Parameter, Function<SpacecraftState, Double>> functions;

    /**
     * Constructor to initialize the function.
     * <p>
     * <b>WARNING: The given map is directly stored in the function. Please be careful to not modify
     * the map, otherwise the function would change to, which can lead to unexpected behaviors.</b>
     * </p>
     *
     * @param functions
     *        Map describing the functions
     * @throws NullArgumentException if a parameter (key) or a monomial function (value) is
     *         described by <i>null</i> because a null parameter/function can't be applied
     */
    public LinearCombinationFunction(
            final Map<Parameter, Function<SpacecraftState, Double>> functions) {
        // Check for null key/value
        final Set<Entry<Parameter, Function<SpacecraftState, Double>>> entries = functions
                .entrySet();
        for (final Entry<Parameter, Function<SpacecraftState, Double>> entry : entries) {
            if ((entry.getKey() == null) || (entry.getValue() == null)) {
                throw new NullArgumentException();
            }
        }
        this.functions = functions;
    }

    /**
     * Constructor to initialize the function with the monomial function description. The parameters
     * are automatically initialized with a generic name and a 0.0 value.
     *
     * @param monomialFunctionCollection
     *        Collection describing the monomial functions
     * @throws NullArgumentException if a monomial function is described by <i>null</i> because a
     *         null function can't be applied
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: preserve insertion order
    public LinearCombinationFunction(
            final Collection<Function<SpacecraftState, Double>> monomialFunctionCollection) {

        final Map<Parameter, Function<SpacecraftState, Double>> map = new LinkedHashMap<>(
                monomialFunctionCollection.size());
        int i = 0;

        for (final Function<SpacecraftState, Double> monomialFunction : monomialFunctionCollection) {
            if (monomialFunction == null) {
                throw new NullArgumentException();
            }
            map.put(new Parameter(PARAMETER_PREFIX_NAME + i, 0.), monomialFunction);
            i++;
        }
        this.functions = map;
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValue(final Parameter p, final SpacecraftState s) {
        double value = 0;

        // Check if the function is differentiable by p
        final Function<SpacecraftState, Double> function = this.functions.get(p);
        if (function != null) {
            value = function.apply(s);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final SpacecraftState state) {
        double value = 0;
        final Set<Entry<Parameter, Function<SpacecraftState, Double>>> entries = this.functions
                .entrySet();
        for (final Entry<Parameter, Function<SpacecraftState, Double>> entry : entries) {
            value += entry.getKey().getValue() * entry.getValue().apply(state);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return this.functions.containsKey(param);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return this.supportsParameter(p);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The list is returned in a shallow copy.
     * </p>
     */
    @Override
    @SuppressWarnings("PMD.LooseCoupling")
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(this.functions.keySet());
    }
}
