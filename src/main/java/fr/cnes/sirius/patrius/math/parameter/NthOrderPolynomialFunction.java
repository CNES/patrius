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
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.serializablefunction.SerializableFunction;

/**
 * This class is used to define parameterizable Nth order polynomial function: <i>f = a0 + a1 * (t -
 * t0) + a2 * (t - t0)^2 + ...</i>.
 * <p>
 * This function is serializable.
 * </p>
 *
 * @author veuillezh, bonitt
 */
public class NthOrderPolynomialFunction extends LinearCombinationFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = -1120199180006993206L;

    /**
     * Constructor of a linear polynomial function of order N, defined such as:</br>
     * <i>f = a0 + a1 * (t - t0) + a2 * (t - t0)^2 + ...</i> with all its coefficients/parameters
     * initialized at 0.
     * <p>
     * Note: For instance, a linear polynomial function of order 3 (N = 3) will have 4
     * coefficients/parameters: <i>a0, a1, a2, a3</i>.
     * </p>
     *
     * @param t0
     *        Initial date
     * @param n
     *        Expected polynomial function order
     */
    public NthOrderPolynomialFunction(final AbsoluteDate t0, final int n) {
        super(constructSuperClassMap(t0, n));
    }

    /**
     * Constructor of a linear polynomial function of order N, defined such as:</br>
     * <i>f = a0 + a1 * (t - t0) + a2 * (t - t0)^2 + ...</i>, with the first specified value
     * represents <i>a0</i>, the second represents <i>a1</i>, etc.
     * <p>
     * The linear polynomial function order depends of how many coefficients are given to the
     * constructor (N coefficients = polynomial of order N).
     * </p>
     *
     * @param t0
     *        Initial date
     * @param values
     *        N order coefficients values
     * @throws NullArgumentException if {@code t0} is {@code null}
     */
    public NthOrderPolynomialFunction(final AbsoluteDate t0, final double... values) {
        super(constructSuperClassMap(t0, values));
    }

    /**
     * Constructor of a linear polynomial function of order N, defined such as:</br>
     * <i>f = a0 + a1 * (t - t0) + a2 * (t - t0)^2 + ...</i>, with the first specified parameter
     * represents <i>a0</i>, the second represents <i>a1</i>, etc.
     * <p>
     * The linear polynomial function order depends of how many parameters are given to the constructor (N parameters =
     * polynomial of order N).
     * </p>
     *
     * @param t0
     *        Initial date
     * @param params
     *        N order coefficients parameters
     * @throws NullArgumentException if {@code t0} or any {@code params} is {@code null}
     */
    public NthOrderPolynomialFunction(final AbsoluteDate t0, final Parameter... params) {
        super(constructSuperClassMap(t0, params));
    }

    /**
     * Initialize a linear polynomial function of order N, defined such as:</br>
     * <i>f = a0 + a1 * (t - t0) + a2 * (t - t0)^2 + ...</i>, with all its coefficients/parameters
     * initialized at 0.
     * <p>
     * Note: For instance, a linear polynomial function of order 3 (N = 3) will have 4
     * coefficients/parameters: <i>a0, a1, a2, a3</i>.
     * </p>
     *
     * @param t0
     *        Initial date
     * @param n
     *        Polynomial function order
     * @return the Nth order polynomial function
     * @throws NullArgumentException if {@code t0} is {@code null}
     */
    private static final Map<Parameter, Function<SpacecraftState, Double>> constructSuperClassMap(
                                   final AbsoluteDate t0, final int n) {
        return constructSuperClassMap(t0, new double[n + 1]);
    }

    /**
     * Initialize a linear polynomial function of order N, defined such as:</br>
     * <i>f = a0 + a1 * (t - t0) + a2 * (t - t0)^2 + ...</i>, with the first specified value
     * represents <i>a0</i>, the second represents <i>a1</i>, etc.
     *
     * @param t0
     *        Initial date
     * @param values
     *        N order coefficients values
     * @return the Nth order polynomial function
     * @throws NullArgumentException if {@code t0} is {@code null}
     */
    private static final
        Map<Parameter, Function<SpacecraftState, Double>>
            constructSuperClassMap(final AbsoluteDate t0, final double... values) {
        final int size = values.length;
        final Parameter[] coefsAParam = new Parameter[size];
        for (int i = 0; i < size; i++) {
            coefsAParam[i] = new Parameter(PARAMETER_PREFIX_NAME + i, values[i]);
        }

        return constructSuperClassMap(t0, coefsAParam);
    }

    /**
     * Initialize a linear polynomial function of order N, defined such as:</br>
     * <i>f = a0 + a1 * (t - t0) + a2 * (t - t0)^2 + ...</i>, with the first specified parameter
     * represents <i>a0</i>, the second represents <i>a1</i>, etc.
     *
     * @param t0
     *        Initial date
     * @param params
     *        N order coefficients parameters
     * @return the Nth order polynomial function
     * @throws NullArgumentException if {@code t0} or any {@code params} is {@code null}
     */
    // Reason: preserve insertion order
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static final
        Map<Parameter, Function<SpacecraftState, Double>>
            constructSuperClassMap(final AbsoluteDate t0,
                                   final Parameter... params) {
        // Check for null attribute
        if (t0 == null) {
            throw new NullArgumentException();
        }
        final int size = params.length;
        // Loop on each parameter to check for null element
        for (int i = 0; i < size; i++) {
            if (params[i] == null) {
                throw new NullArgumentException();
            }
        }

        // Build the function map (LinkedHashMap to guarantee order)
        final Map<Parameter, Function<SpacecraftState, Double>> map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            final int pow = i;
            map.put(params[i], (SerializableFunction<SpacecraftState, Double>) state -> MathLib
                .pow(state.getDate().durationFrom(t0), pow));
        }
        return map;
    }

    /**
     * Return the function N parameters in this following order : <i>a0</i>, <i>a1</i>, <i>a2</i>,
     * ...
     * <p>
     * The list is returned in a shallow copy.
     * </p>
     *
     * @return the function N parameters list
     */
    @Override
    @SuppressWarnings("PMD.LooseCoupling")
    public ArrayList<Parameter> getParameters() {
        // Implementation note: override in order to have a specific javadoc for this function
        return super.getParameters();
    }

    /**
     * Getter for a String representation of this function.
     * 
     * @return a String representation of this function
     */
    @Override
    public String toString() {

        final StringBuilder myStringBuilder = new StringBuilder();

        // Initialize the string joiners for the values and for the parameter names
        final StringJoiner strJoinerValues = new StringJoiner(" + ", "Value     : f = ", "\n");
        final StringJoiner strJoinerNames = new StringJoiner("; ", "Parameters: [", "]\n");

        // Build the format for the values: f = a0 + a1 * (t - t0) + a2 * (t - t0)^2 + ...
        int indice = 0;
        final String factor = " * (t - t0)";
        for (final Parameter key : this.functions.keySet()) {
            final String paramValue = Double.toString(key.getValue());
            final String paramName = key.getName();

            if (indice == 0) {
                // Degree 0: "paramValue"
                strJoinerValues.add(paramValue);
            } else if (indice == 1) {
                // Degree 1: "paramValue * (t - t0)"
                strJoinerValues.add(paramValue + factor);
            } else {
                // Degree > 1: "paramValue * (t - t0)^N"
                strJoinerValues.add(paramValue + factor + "^" + indice);
            }
            strJoinerNames.add(paramName);
            indice++;
        }
        myStringBuilder.append(strJoinerValues);
        myStringBuilder.append(strJoinerNames);

        return myStringBuilder.toString();
    }
}
