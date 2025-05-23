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
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:01/09/2014:create a class to define parameterizable constant function.
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.Collections;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.serializablefunction.SerializableFunction;

/**
 * This class is used to define a constant parameterizable function.
 * <p>
 * This function is serializable.
 * </p>
 *
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 *
 * @author auguief
 * @since 2.3
 * @version $Id: ConstantFunction.java 18069 2017-10-02 16:45:28Z bignon $
 */
public class ConstantFunction extends LinearCombinationFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = -4651028856848403142L;

    /** The parameter associates to the function. */
    private final transient Parameter param;

    /**
     * Constructor of a constant function <i>f = a0</i> using the input value.
     *
     * @param value
     *        the a0 parameter value
     */
    public ConstantFunction(final double value) {
        this(new Parameter(PARAMETER_PREFIX_NAME + "0", value));
    }

    /**
     * Constructor of a constant function <i>f = a0</i> using the input value.
     *
     * @param name
     *        the a0 parameter name
     * @param value
     *        the a0 parameter value
     */
    public ConstantFunction(final String name, final double value) {
        this(new Parameter(name, value));
    }

    /**
     * Constructor of a constant function <i>f = a0</i> using the input parameter.
     *
     * @param param
     *        the a0 parameter
     * @throws NullArgumentException
     *         if {@code param} is {@code null}
     */
    public ConstantFunction(final Parameter param) {
        super(Collections.singletonMap(param, (SerializableFunction<SpacecraftState, Double>) state -> 1.0));
        this.param = param;
    }

    /**
     * Value of the parameter.
     *
     * @return the value of the parameter
     *         parameter as double
     */
    public double value() {
        return this.param.getValue();
    }

    /**
     * Getter for the parameter associated to this constant function.
     *
     * @return the parameter
     */
    public Parameter getParameter() {
        return this.param;
    }

    /**
     * Return the function parameter [a0] stored in a list.
     * <p>
     * The list is returned in a shallow copy.
     * </p>
     *
     * @return the function parameter stored in a list
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
        myStringBuilder.append("Value    : f = " + this.param.getValue() + "\n");
        myStringBuilder.append("Parameter: [" + this.param.getName() + "]\n");

        return myStringBuilder.toString();
    }

    /**
     * Internal method to recover the final transient attribute after deserialization.
     *
     * @return the deserialized object
     */
    private Object readResolve() {
        return new ConstantFunction(this.getParameters().get(0));
    }
}
