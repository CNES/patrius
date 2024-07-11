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
 */
/*
 * HISTORY
* VERSION:4.9:FA:FA-3112:10/05/2022:[PATRIUS] Reliquats mineurs de reversement de la branche patrius-for-lotus 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.io.Serializable;

/**
 * This class links a value and a parameter descriptor which can contain any number of information
 * of any type (like a name, a date, ...).
 * This class is for example used when computing finite differences and derivatives of analytical
 * functions.
 *
 * <p>
 * Note that while its reference cannot be changed once set (the attribute is {@code final}), the
 * parameter descriptor itself is possibly mutable (it can be set as immutable, but it is not a
 * definitive property and it's not the case by default). Also note that it is possible for the
 * parameter descriptor to be {@code null} or empty (that is, not associated with any field).
 * However, using parameters in such a state is strongly discouraged since it can potentially lead
 * to errors if higher-level methods do not handle these cases properly.
 * </p>
 *
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 *
 * @author auguief
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
 * @author Pierre Seimandi (GMV)
 *
 * @since 2.3
 *
 * @version $Id: Parameter.java 18069 2017-10-02 16:45:28Z bignon $
 */
public class Parameter implements Serializable {

    /** Serial version UID. */
    private static final long serialVersionUID = -4043807902539452466L;

    /** Default value separator for the {@link #toString()} methods. */
    private static final String DEFAULT_VALUE_SEPARATOR = ": ";

    /** Default name separator for the {@link #toString()} and {@link #getName()} methods. */
    private static final String DEFAULT_NAME_SEPARATOR = "_";

    /** Parameter descriptor. */
    private final ParameterDescriptor descriptor;

    /** Parameter value. */
    private double value;

    /**
     * Creates a new instance using the provided name and value.
     *
     * @param parameterName
     *        the parameter name
     * @param parameterValue
     *        the parameter value
     *
     * @since 2.3
     */
    public Parameter(final String parameterName, final double parameterValue) {
        this(new ParameterDescriptor(parameterName), parameterValue);
    }

    /**
     * Creates a new instance using the provided parameter descriptor and value.
     *
     * @param parameterDescriptor
     *        the parameter descriptor
     * @param parameterValue
     *        the parameter value
     */
    public Parameter(final ParameterDescriptor parameterDescriptor, final double parameterValue) {
        this.descriptor = parameterDescriptor;
        this.value = parameterValue;
    }

    /**
     * Gets the parameter descriptor.
     *
     * @return the parameter descriptor
     */
    public ParameterDescriptor getDescriptor() {
        return this.descriptor;
    }

    /**
     * Gets the parameter name, which is a concatenation of field values currently associated with
     * the parameter descriptor (printed in reverse order by default).
     *
     * @return the parameter name
     */
    public String getName() {
        return getName(true);
    }

    /**
     * Gets the parameter name, which is a concatenation of field values currently associated with
     * the parameter descriptor.
     *
     * @param reverseOrder
     *        whether or not the field values should be printed in reverse order
     * @return the parameter name, or {@code null} if the parameter descriptor associated with this
     *         parameter is {@code null}
     */
    public String getName(final boolean reverseOrder) {
        return getName(DEFAULT_NAME_SEPARATOR, reverseOrder);
    }

    /**
     * Gets the parameter name, which is a concatenation of field values currently associated with
     * the parameter descriptor (printed in reverse order by default).
     *
     * @param separator
     *        the string used as separator between two field values
     * @return the parameter name, or {@code null} if the parameter descriptor associated with this
     *         parameter is {@code null}
     */
    public String getName(final String separator) {
        return getName(separator, true);
    }

    /**
     * Gets the parameter name, which is a concatenation of field values currently associated with
     * the parameter descriptor.
     *
     * @param separator
     *        the string used as separator between two field values
     * @param reverseOrder
     *        whether or not the field values should be printed in reverse order
     * @return the parameter name, or {@code null} if the parameter descriptor associated with this
     *         parameter is {@code null}
     */
    public String getName(final String separator, final boolean reverseOrder) {
        String name = null;
        if (this.descriptor != null) {
            name = this.descriptor.getName(separator, reverseOrder);
        }
        return name;
    }

    /**
     * Gets the parameter value.
     *
     * @return the parameter value
     */
    public double getValue() {
        return this.value;
    }

    /**
     * Sets the parameter value.
     *
     * @param parameterValue
     *        the new parameter value
     */
    public void setValue(final double parameterValue) {
        this.value = parameterValue;
    }

    /**
     * Gets a string representation of this parameter, which includes the name of this class, the
     * name of the parameter and the parameter value.
     *
     * @return a string representation of this parameter
     */
    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Gets a string representation of this parameter, which includes the name of this class, the
     * name of the parameter and the parameter value.
     *
     * @param reverseOrder
     *        whether or not the associated field values should be printed in reverse order
     * @return a string representation of this parameter
     */
    public String toString(final boolean reverseOrder) {
        return toString(DEFAULT_NAME_SEPARATOR, DEFAULT_VALUE_SEPARATOR, true, reverseOrder);
    }

    /**
     * Gets a string representation of this parameter, which includes the name of this class (if
     * requested), the name of the parameter and the parameter value.
     *
     * @param nameSeparator
     *        the string to be used as separator when retrieving the name of the parameter
     * @param valueSeparator
     *        the string to be used as separator between the name of the parameter and its value
     * @param printClassName
     *        whether or not the name of this class should be printed
     * @param reverseOrder
     *        whether or not the associated field values should be printed in reverse order
     * @return a string representation of this parameter
     * @see Parameter#getName(String, boolean)
     */
    public String toString(final String nameSeparator, final String valueSeparator,
            final boolean printClassName, final boolean reverseOrder) {
        final StringBuilder builder = new StringBuilder();

        if (printClassName) {
            builder.append(Parameter.class.getSimpleName());
            builder.append("[");
        }

        builder.append(this.getName(nameSeparator, reverseOrder));
        builder.append(valueSeparator);
        builder.append(this.value);

        if (printClassName) {
            builder.append("]");
        }

        return builder.toString();
    }

    /**
     * Performs a shallow copy of this parameter (the references to the field descriptors and the
     * mapped values are preserved).
     *
     * @return a shallow copy of this parameter
     */
    public Parameter copy() {
        return new Parameter(this.descriptor.copy(), this.value);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * <p>
     * This methods simply redirects to the {@linkplain Object#equals(Object)} method, which considers that two objects
     * are equals if and only if they are the same instance. This default behavior is preserved on purpose for
     * historical reasons, as other classes sometimes use the {@linkplain Parameter} class as key in their internal maps
     * and rely on the fact that two separate {@linkplain Parameter} instances can never be equal.
     * </p>
     * 
     * @param object
     *        the reference object with which to compare
     * 
     * @return {@code true} if this object is the same as the provided object, {@code false} otherwise
     */
    @Override
    @SuppressWarnings("PMD.UselessOverridingMethod")
    // Reason: kept on purpose to force calling Object method
    public boolean equals(final Object object) {
        return super.equals(object);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.UselessOverridingMethod")
    // Reason: kept on purpose to force calling Object method
    public int hashCode() {
        return super.hashCode();
    }
}
