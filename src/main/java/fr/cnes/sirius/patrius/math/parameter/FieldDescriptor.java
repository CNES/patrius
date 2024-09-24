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
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Field descriptor.
 * <p>
 * A field descriptor associates a name with a given class, and provides the means to generate
 * custom string representations of any instance of this class.
 * </p>
 *
 * @param <T>
 *        the type of the described field
 *
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
 * @author Pierre Seimandi (GMV)
 */
public class FieldDescriptor<T> implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 2464360492514865635L;

    /** Name of the descriptor. */
    private final String name;

    /** Class of the described fields. */
    private final Class<T> fieldClass;

    /** Function to use when converting field values to strings. */
    private transient Function<T, String> printFunction;

    /**
     * Creates a new field descriptor.
     *
     * @param nameIn
     *        the name of the descriptor
     * @param fieldClassIn
     *        the class of the described fields
     * @throws IllegalArgumentException
     *         if {@code nameIn} or {@code fieldClassIn} is {@code null}
     */
    public FieldDescriptor(final String nameIn, final Class<T> fieldClassIn) {
        this(nameIn, fieldClassIn, null);
    }

    /**
     * Creates a new field descriptor which uses the specified function to convert field values to
     * strings.
     *
     * @param nameIn
     *        the name of the descriptor
     * @param fieldClassIn
     *        the class of the described fields
     * @param printFunctionIn
     *        the function to use when converting field values to strings
     * @throws NullArgumentException
     *         if {@code nameIn} or {@code fieldClassIn} is {@code null}
     */
    public FieldDescriptor(final String nameIn, final Class<T> fieldClassIn,
            final Function<T, String> printFunctionIn) {
        checkNotNull(nameIn, "name");
        checkNotNull(fieldClassIn, "field class");

        this.name = nameIn;
        this.fieldClass = fieldClassIn;
        this.printFunction = printFunctionIn;
    }

    /**
     * Gets the name of the descriptor.
     *
     * @return the name of the descriptor
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the class of the described fields.
     *
     * @return the class of the described fields
     */
    public Class<T> getFieldClass() {
        return this.fieldClass;
    }

    /**
     * Gets the function to use when converting field values to strings.
     *
     * @return the function to use when converting field values to strings
     */
    public Function<T, String> getPrintFunction() {
        return this.printFunction;
    }

    /**
     * Sets the function to use when converting field values to strings.
     *
     * @param newPrintFunction
     *        the new function to use when converting field values to strings
     */
    public void setPrintFunction(final Function<T, String> newPrintFunction) {
        this.printFunction = newPrintFunction;
    }

    /**
     * Gets a string representation of a given field value.
     *
     * <p>
     * The string representation of the field value is generated using the printer function
     * specified at construction. The standard {@code toString} method is used instead if no
     * function was specified, or if the class of the provided object does not match the class
     * associated with this field descriptor.
     * </p>
     *
     * @param value
     *        the field value
     *
     * @return a string representation of the specified field value
     */
    public String printField(final Object value) {
        final String str;
        if ((this.printFunction != null) && this.fieldClass.isInstance(value)) {
            str = this.printFunction.apply(this.fieldClass.cast(value));
        } else {
            str = value.toString();
        }

        return str;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append(FieldDescriptor.class.getSimpleName());
        builder.append("[");
        builder.append("name: ");
        builder.append(this.getName());
        builder.append("; ");
        builder.append("class: ");
        builder.append(this.fieldClass.getSimpleName());
        builder.append("]");

        return builder.toString();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * <p>
     * This method only compares the name of the descriptor and the class of the described fields
     * when checking if two {@linkplain FieldDescriptor} instances are equal or not. The function
     * used to convert field values to strings is not taken into account.
     * </p>
     *
     * @param object
     *        the reference object with which to compare
     *
     * @return {@code true} if this object is the same as the provided object, {@code false}
     *         otherwise
     */
    @Override
    public boolean equals(final Object object) {
        boolean out = false;

        if (object == this) {
            out = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            final FieldDescriptor<?> other = (FieldDescriptor<?>) object;
            out = true;
            out &= Objects.equals(this.name, other.name);
            out &= Objects.equals(this.fieldClass, other.fieldClass);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.fieldClass);
    }

    /**
     * Ensures an argument is not {@code null} and throws an exception if that's not the case.
     *
     * @param object
     *        the object to check
     * @param description
     *        a short description of the object checked
     *
     * @throws NullArgumentException
     *         if the object is {@code null}
     */
    private static void checkNotNull(final Object object, final String description) {
        if (object == null) {
            throw new NullArgumentException(PatriusMessages.NULL_NOT_ALLOWED_DESCRIPTION,
                    description);
        }
    }
}
