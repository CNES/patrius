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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Parameter descriptor.
 *
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
 * @author Pierre Seimandi (GMV)
 */
public class ParameterDescriptor implements Serializable {

    /** Serial version UID. */
    private static final long serialVersionUID = 7157587435779077691L;

    /** Argument description string for field descriptors. */
    private static final String FIELD_DESCRIPTOR = "field descriptor";

    /** Argument description string for collections of field descriptors. */
    private static final String FIELD_DESCRIPTORS = "field descriptors";

    /** Argument description string for field values. */
    private static final String FIELD_VALUE = "field value";

    /** Default key/value separator for the toString() methods. */
    private static final String DEFAULT_KEY_SEPARATOR = ": ";

    /** Default entry separator for the toString() methods. */
    private static final String DEFAULT_ENTRY_SEPARATOR = "; ";

    /** Default entry separator for the getName() methods. */
    private static final String DEFAULT_NAME_SEPARATOR = "_";

    /** Field descriptors associated with this parameter descriptor and their mapped value. */
    private final Map<FieldDescriptor<?>, Object> fields;

    /** Mutability state. */
    private boolean mutable;

    /**
     * Creates a new instance that is not associated with any field.
     */
    public ParameterDescriptor() {
        this.mutable = true;
        this.fields = new LinkedHashMap<>();
    }

    /**
     * Creates a new instance that is associated with a single field representing the name of the
     * parameter.
     *
     * @param name
     *        the name of the parameter
     */
    public ParameterDescriptor(final String name) {
        this(StandardFieldDescriptors.PARAMETER_NAME, name);
    }

    /**
     * Creates a new instance that is associated with a single field.
     * 
     * @param fieldDescriptor
     *        the field descriptor
     * @param fieldValue
     *        the value mapped to the provided field descriptor
     * @param <T>
     *        the type of the field value
     * @throws IllegalArgumentException
     *         if the provided field descriptor or field value is {@code null}
     */
    public <T> ParameterDescriptor(final FieldDescriptor<T> fieldDescriptor, final T fieldValue) {
        this.mutable = true;
        this.fields = new LinkedHashMap<>();
        this.addField(fieldDescriptor, fieldValue);
    }

    /**
     * Creates a new instance that is associated with the provided fields.
     * <p>
     * <b>Important:</b> each of the provided values must be assignable to the the class specified
     * by the corresponding field descriptor. An exception will be automatically thrown if that is
     * not the case.
     * </p>
     *
     * @param fieldsMap
     *        the map between the initial field descriptors and the values mapped to them
     * @throws ClassCastException
     *         if one of the provided values cannot be cast to the class specified by its field
     *         descriptor
     */
    public ParameterDescriptor(final Map<FieldDescriptor<?>, Object> fieldsMap) {
        this.mutable = true;

        if (fieldsMap == null) {
            this.fields = new LinkedHashMap<>();
        } else {
            this.fields = new LinkedHashMap<>(fieldsMap.size());
            this.addUntypedFields(fieldsMap);
        }
    }

    /**
     * Gets the field descriptors currently associated with this parameter descriptor and the values
     * mapped to them.
     *
     * @return the associated field descriptors and the values mapped to them
     */
    public Map<FieldDescriptor<?>, Object> getAssociatedFields() {
        return new LinkedHashMap<>(this.fields);
    }

    /**
     * Gets the field descriptors currently associated with this parameter descriptor.
     *
     * @return the associated field descriptors
     */
    public Set<FieldDescriptor<?>> getAssociatedFieldDescriptors() {
        return this.fields.keySet();
    }

    /**
     * Checks if this parameter descriptor is currently mutable or not.
     * <p>
     * The mutability of this parameter descriptor can be enabled or disabled at will through the
     * {@link #setMutability(boolean)} method. Methods updating a parameter descriptor should always
     * check its mutability beforehand.
     * </p>
     *
     * @return {@code true} if this parameter descriptor is currently mutable, {@code false}
     *         otherwise
     */
    public boolean isMutable() {
        return this.mutable;
    }

    /**
     * Enables or disables the mutability of this parameter descriptor.
     *
     * @param enabled
     *        whether or not to allow this parameter descriptor to be mutable
     * @return this parameter descriptor (for chaining)
     */
    public ParameterDescriptor setMutability(final boolean enabled) {
        this.mutable = enabled;
        return this;
    }

    /**
     * Checks if a field descriptor is currently associated with this parameter descriptor.
     *
     * @param fieldDescriptor
     *        the field descriptor to check
     * @return {@code true} if the provided field descriptor is associated with this parameter
     *         descriptor, {@code false} otherwise
     */
    public boolean contains(final FieldDescriptor<?> fieldDescriptor) {
        return this.fields.containsKey(fieldDescriptor);
    }

    /**
     * Checks if a field descriptor is currently associated with this parameter descriptor and
     * mapped to a given value.
     *
     * @param fieldDescriptor
     *        the field descriptor to check
     * @param fieldValue
     *        the value expected to be mapped to the provided field descriptor
     * @return {@code true} if the provided field descriptor is associated with this parameter
     *         descriptor and mapped to the specified value, {@code false} otherwise
     */
    public boolean contains(final FieldDescriptor<?> fieldDescriptor, final Object fieldValue) {
        final Object mappedValue = this.fields.get(fieldDescriptor);
        return (mappedValue != null) && mappedValue.equals(fieldValue);
    }

    /**
     * Adds a single field descriptor with this parameter descriptor and maps it to the specified
     * value (stored values are overwritten when a field descriptor is already associated with this
     * instance).
     *
     * @param fieldDescriptor
     *        the field descriptor to add
     * @param fieldValue
     *        the value to be mapped to the specified field descriptor
     * @param <T>
     *        the type of the field value
     * @return the value which was previously mapped to the provided field descriptor, or
     *         {@code null} if this field descriptor was not
     *         already associated with this parameter descriptor
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     * @throws NullArgumentException
     *         if the provided field descriptor or field value is {@code null}
     */
    public final <T> T addField(final FieldDescriptor<T> fieldDescriptor, final T fieldValue) {
        checkMutability();
        checkNotNull(fieldDescriptor, FIELD_DESCRIPTOR);
        checkNotNull(fieldValue, FIELD_VALUE);

        final Class<T> fieldClass = fieldDescriptor.getFieldClass();
        return fieldClass.cast(this.fields.put(fieldDescriptor, fieldValue));
    }

    /**
     * Adds a single field descriptor with this parameter descriptor and maps it to the specified
     * value (stored values are overwritten when a field descriptor is already associated with this
     * instance).
     * <p>
     * <b>Important:</b> the provided value must be assignable to the class specified by the field
     * descriptor. An exception will be automatically thrown if that is not the case.
     * </p>
     *
     * @param fieldDescriptor
     *        the field descriptor to add
     * @param fieldValue
     *        the value to be associated with the specified field descriptor
     * @return the value which was previously mapped to the provided field descriptor, or
     *         {@code null} if this field descriptor was not
     *         already associated with this parameter descriptor
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     * @throws NullArgumentException
     *         if the provided field descriptor or field value is {@code null}
     * @throws ClassCastException
     *         if the provided field value cannot be cast to the class specified by the field
     *         descriptor
     */
    public final Object
            addUntypedField(final FieldDescriptor<?> fieldDescriptor, final Object fieldValue) {
        checkMutability();
        checkNotNull(fieldDescriptor, FIELD_DESCRIPTOR);
        checkNotNull(fieldValue, FIELD_VALUE);

        final Class<?> fieldClass = fieldDescriptor.getFieldClass();
        return this.fields.put(fieldDescriptor, fieldClass.cast(fieldValue));
    }

    /**
     * Adds multiple field descriptors with this parameter descriptor and maps them to the specified
     * values (existing values are overwritten when a field descriptor is already associated with
     * this instance).
     * <p>
     * <b>Important:</b> each of the provided values must be assignable to the the class specified
     * by the corresponding field descriptor. An exception will be automatically thrown if that is
     * not the case.
     * </p>
     *
     * @param fieldsMap
     *        the field descriptors to be added and the values mapped to them
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     * @throws IllegalArgumentException
     *         if one of the field descriptors or field values is {@code null}
     * @throws ClassCastException
     *         if a field value cannot be cast to the class specified by the associated field
     *         descriptor
     */
    public final void addUntypedFields(final Map<FieldDescriptor<?>, Object> fieldsMap) {
        checkMutability();
        if (fieldsMap != null) {
            for (final Map.Entry<FieldDescriptor<?>, Object> entry : fieldsMap.entrySet()) {
                this.addUntypedField(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Replaces the value mapped to a given field descriptor if it is currently associated with this
     * parameter descriptor.
     *
     * @param fieldDescriptor
     *        the field descriptor whose mapped value is to be replaced
     * @param fieldValue
     *        the new value to be mapped to the specified field descriptor
     * @param <T>
     *        the type of the field value
     * @return the value which was previously mapped to the provided field descriptor, or
     *         {@code null} if this field descriptor is not
     *         associated with this parameter descriptor
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     * @throws NullArgumentException
     *         if the new field value is {@code null}
     */
    public <T> T replaceField(final FieldDescriptor<T> fieldDescriptor, final T fieldValue) {
        checkMutability();
        T replaced = null;

        if (fieldDescriptor != null) {
            checkNotNull(fieldValue, FIELD_VALUE);
            final Class<T> fieldClass = fieldDescriptor.getFieldClass();
            replaced = fieldClass.cast(this.fields.replace(fieldDescriptor, fieldValue));
        }

        return replaced;
    }

    /**
     * Replaces the value mapped to a given field descriptor if it is currently associated with this
     * parameter descriptor and mapped to the specified value.
     * 
     * @param fieldDescriptor
     *        the field descriptor whose mapped value is to be replaced
     * @param oldFieldValue
     *        the value expected to be mapped to the specified field descriptor
     * @param newFieldValue
     *        the new value to be mapped to the specified field descriptor
     * @param <T>
     *        the type of the field values
     * @return {@code true} if the value mapped to the specified field descriptor was replaced,
     *         {@code false} otherwise
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     * @throws NullArgumentException
     *         if the new field value is {@code null}
     */
    public <T> boolean replaceField(final FieldDescriptor<T> fieldDescriptor,
            final T oldFieldValue, final T newFieldValue) {
        checkMutability();
        boolean replaced = false;

        if (fieldDescriptor != null) {
            checkNotNull(newFieldValue, FIELD_VALUE);
            replaced = this.fields.replace(fieldDescriptor, oldFieldValue, newFieldValue);
        }

        return replaced;
    }

    /**
     * Replaces the value mapped to a given field descriptor if it is currently associated with this
     * parameter descriptor.
     * <p>
     * <b>Important:</b> the provided value must be assignable to the class specified by the field
     * descriptor. An exception will be automatically thrown if that is not the case.
     * </p>
     *
     * @param fieldDescriptor
     *        the field descriptor whose mapped value is to be replaced
     * @param fieldValue
     *        the new value to be mapped to the specified field descriptor
     * @return the value which was previously mapped to the provided field descriptor, or
     *         {@code null} if this field descriptor is not
     *         associated with this parameter descriptor
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     * @throws NullArgumentException
     *         if the new field value is {@code null}
     * @throws ClassCastException
     *         if the new field value cannot be cast to the class specified by the field descriptor
     */
    public Object replaceUntypedField(final FieldDescriptor<?> fieldDescriptor,
            final Object fieldValue) {
        checkMutability();
        Object replaced = null;

        if (fieldDescriptor != null) {
            checkNotNull(fieldValue, FIELD_VALUE);
            final Class<?> fieldClass = fieldDescriptor.getFieldClass();
            replaced = this.fields.replace(fieldDescriptor, fieldClass.cast(fieldValue));
        }

        return replaced;
    }

    /**
     * Replaces the value mapped to a given field descriptor if it is currently associated with this
     * parameter descriptor and mapped to the specified value.
     * <p>
     * <b>Important:</b> the provided values must be assignable to the class specified by the field
     * descriptor. An exception will be automatically thrown if that is not the case.
     * </p>
     *
     * @param fieldDescriptor
     *        the field descriptor whose mapped value is to be replaced
     * @param oldFieldValue
     *        the value expected to be mapped to the specified field descriptor
     * @param newFieldValue
     *        the new value to be mapped to the specified field descriptor
     * @return {@code true} if the value mapped to the specified field descriptor was replaced,
     *         {@code false} otherwise
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     * @throws NullArgumentException
     *         if the new field value is {@code null}
     * @throws ClassCastException
     *         if the new field value cannot be cast to the class specified by the field descriptor
     */
    public boolean replaceUntypedField(final FieldDescriptor<?> fieldDescriptor,
            final Object oldFieldValue, final Object newFieldValue) {
        checkMutability();
        boolean replaced = false;

        if (fieldDescriptor != null) {
            checkNotNull(newFieldValue, FIELD_VALUE);
            final Class<?> fieldClass = fieldDescriptor.getFieldClass();
            replaced = this.fields.replace(fieldDescriptor, oldFieldValue,
                    fieldClass.cast(newFieldValue));
        }

        return replaced;
    }

    /**
     * Removes a given field descriptor from this parameter descriptor.
     * 
     * @param fieldDescriptor
     *        the field descriptor to be removed
     * @param <T>
     *        the type of the associated with the field descriptor
     * @return the value which was previously mapped to the provided field descriptor, or
     *         {@code null} if this field descriptor was not associated with this parameter
     *         descriptor
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     */
    public <T> T removeField(final FieldDescriptor<T> fieldDescriptor) {
        checkMutability();
        T removed = null;

        if (fieldDescriptor != null) {
            final Class<T> fieldClass = fieldDescriptor.getFieldClass();
            removed = fieldClass.cast(this.fields.remove(fieldDescriptor));
        }

        return removed;
    }

    /**
     * Removes a given field descriptor from this parameter descriptor if it is currently mapped to
     * the specified value.
     *
     * @param fieldDescriptor
     *        the field descriptor to be removed
     * @param fieldValue
     *        the value expected to be mapped with the specified field descriptor
     * @param <T>
     *        the type of the field value
     * @return {@code true} if the specified field descriptor and its mapped value were removed,
     *         {@code false} otherwise
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     */
    public <T> boolean removeField(final FieldDescriptor<T> fieldDescriptor, final T fieldValue) {
        checkMutability();
        return this.fields.remove(fieldDescriptor, fieldValue);
    }

    /**
     * Removes a given field descriptor from this parameter descriptor if it is currently mapped to
     * the specified value.
     *
     * @param fieldDescriptor
     *        the field descriptor to be removed
     * @param fieldValue
     *        the value expected to be mapped with the specified field descriptor
     * @return {@code true} if the specified field descriptor and its mapped value were removed,
     *         {@code false} otherwise
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     */
    public boolean removeUntypedField(final FieldDescriptor<?> fieldDescriptor,
            final Object fieldValue) {
        checkMutability();
        return this.fields.remove(fieldDescriptor, fieldValue);
    }

    /**
     * Removes multiple field descriptors and the associated values from this parameter descriptor.
     *
     * @param fieldDescriptors
     *        the field descriptors to be removed
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     */
    public void removeUntypedFields(final FieldDescriptor<?>... fieldDescriptors) {
        checkMutability();
        if (fieldDescriptors != null) {
            removeUntypedFields(Arrays.asList(fieldDescriptors));
        }
    }

    /**
     * Removes multiple field descriptors and the associated values from this parameter descriptor.
     *
     * @param fieldDescriptors
     *        the field descriptors to be removed
     * @throws IllegalStateException
     *         if this parameter descriptor is currently immutable
     */
    public void removeUntypedFields(final Collection<FieldDescriptor<?>> fieldDescriptors) {
        checkMutability();
        if (fieldDescriptors != null) {
            for (final FieldDescriptor<?> fieldDescriptor : fieldDescriptors) {
                this.fields.remove(fieldDescriptor);
            }
        }
    }

    /**
     * Removes all the fields currently associated with this parameter descriptor.
     */
    public void clear() {
        checkMutability();
        this.fields.clear();
    }

    /**
     * Checks whether this parameter descriptor is currently associated with anything or not.
     *
     * @return {@code true} if this parameter descriptor is not associated with anything,
     *         {@code false} otherwise
     */
    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    /**
     * Gets the value currently mapped to a given field descriptor.
     *
     * @param fieldDescriptor
     *        the field descriptor
     * @param <T>
     *        the type of the field value
     * @return the value mapped to the specified field descriptor, or {@code null} if the field
     *         descriptor is not associated with this parameter descriptor
     * @throws NullArgumentException
     *         if the provided field descriptor is {@code null}
     */
    public <T> T getFieldValue(final FieldDescriptor<T> fieldDescriptor) {
        checkNotNull(fieldDescriptor, FIELD_DESCRIPTOR);
        final Class<T> fieldClass = fieldDescriptor.getFieldClass();
        return fieldClass.cast(this.fields.get(fieldDescriptor));
    }

    /**
     * Returns a new {@linkplain ParameterDescriptor} instance which is only associated with the
     * selected subset of field descriptors (the ones which are not currently associated with this
     * parameter descriptor are simply ignored; the field values remain unchanged).
     *
     * @param fieldDescriptors
     *        the selected field descriptors
     * @return the new {@linkplain ParameterDescriptor} instance build
     */
    public ParameterDescriptor extractSubset(final FieldDescriptor<?>... fieldDescriptors) {
        return extractSubset(Arrays.asList(fieldDescriptors));
    }

    /**
     * Returns a new {@linkplain ParameterDescriptor} instance which is only associated with the
     * selected subset of field descriptors (the ones which are not currently associated with this
     * parameter descriptor are simply ignored; the field values remain unchanged).
     *
     * @param fieldDescriptors
     *        the selected field descriptors
     * @return the new {@linkplain ParameterDescriptor} instance build
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: preserve insertion order
    public ParameterDescriptor extractSubset(final Collection<FieldDescriptor<?>> fieldDescriptors) {
        final Map<FieldDescriptor<?>, Object> subset = new LinkedHashMap<>();
        for (final FieldDescriptor<?> fieldDescriptor : fieldDescriptors) {
            final Object fieldValue = this.getFieldValue(fieldDescriptor);
            if (fieldValue != null) {
                subset.put(fieldDescriptor, fieldValue);
            }
        }

        return new ParameterDescriptor(subset).setMutability(this.mutable);
    }

    /**
     * Merges this parameter descriptor with another one.
     * <p>
     * The field descriptors of both this instance and the one provided are kept. When a field
     * descriptor is associated with both instances, the provided parameter descriptor takes
     * precedence and replaces the currently mapped value.
     * </p>
     *
     * @param descriptor
     *        the other parameter descriptor
     * @return the {@linkplain ParameterDescriptor} instance resulting from the merging process
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: preserve insertion order
    public ParameterDescriptor mergeWith(final ParameterDescriptor descriptor) {
        final Map<FieldDescriptor<?>, Object> subset = new LinkedHashMap<>(this.fields);

        if (descriptor != null) {
            for (final Map.Entry<FieldDescriptor<?>, Object> entry : descriptor.fields.entrySet()) {
                subset.put(entry.getKey(), entry.getValue());
            }
        }

        return new ParameterDescriptor(subset).setMutability(this.mutable);
    }

    /**
     * Extracts the field descriptors associated with both this parameter descriptor and the one
     * provided, and which are mapped to the same value.
     *
     * @param descriptor
     *        the other parameter descriptor
     * @return the {@linkplain ParameterDescriptor} instance resulting from the intersection
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Reason: preserve insertion order
    public ParameterDescriptor intersectionWith(final ParameterDescriptor descriptor) {
        final Map<FieldDescriptor<?>, Object> subset = new LinkedHashMap<>();

        if (descriptor != null) {
            for (final Map.Entry<FieldDescriptor<?>, Object> entry : this.fields.entrySet()) {
                final FieldDescriptor<?> fieldDescriptor = entry.getKey();
                final Object fieldValue = entry.getValue();
                if (descriptor.contains(fieldDescriptor, fieldValue)) {
                    subset.put(fieldDescriptor, fieldValue);
                }
            }
        }

        return new ParameterDescriptor(subset).setMutability(this.mutable);
    }

    /**
     * Gets the name of this parameter descriptor, which is comprised of the associated field values
     * separated by an underscore (printed in reverse order by default).
     *
     * @return the name of this parameter descriptor
     */
    public String getName() {
        return getName(DEFAULT_NAME_SEPARATOR, true);
    }

    /**
     * Gets the name of this parameter descriptor, which is comprised of the associated field values
     * separated by an underscore.
     *
     * @param reverseOrder
     *        whether or not the field values should be printed in reverse order
     * @return the name of this parameter descriptor
     */
    public String getName(final boolean reverseOrder) {
        return getName(DEFAULT_NAME_SEPARATOR, reverseOrder);
    }

    /**
     * Gets the name of this parameter descriptor, which is comprised of the associated field values
     * separated by the specified string (printed in reverse order by default).
     *
     * @param separator
     *        the string used as separator between two field values
     * @return the name of this parameter descriptor
     */
    public String getName(final String separator) {
        return getName(separator, true);
    }

    /**
     * Gets the name of this parameter descriptor, which is comprised of the associated field values
     * separated by the specified string.
     *
     * @param separator
     *        the string used as separator between two field values
     * @param reverseOrder
     *        whether or not the field values should be printed in reverse order
     * @return the name of this parameter descriptor
     */
    public String getName(final String separator, final boolean reverseOrder) {
        return toString(DEFAULT_KEY_SEPARATOR, separator, false, false, true, reverseOrder);
    }

    /**
     * Gets a string representation of this parameter descriptor which includes the name of this
     * class, the name of associated the field descriptors and their mapped values (printed in 
     * reverse order by default).
     *
     * @return a string representation of this parameter descriptor
     */
    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Gets a string representation of this parameter descriptor which includes the name of this
     * class, the name of associated the field descriptors and their mapped values.
     *
     * @param reverseOrder
     *        whether or not the field descriptors and their mapped values should be printed in
     *        reverse order
     * @return a string representation of this parameter descriptor
     */
    public String toString(final boolean reverseOrder) {
        return toString(DEFAULT_KEY_SEPARATOR, DEFAULT_ENTRY_SEPARATOR, true, true, true,
                reverseOrder);
    }

    /**
     * Gets a string representation of this parameter descriptor.
     *
     * @param keySeparator
     *        the string used as separator between a key and its mapped value
     * @param entrySeparator
     *        the string used as separator between two entries
     * @param printClassName
     *        whether or not the name of this class should be printed
     * @param printFieldName
     *        whether or not the name associated with the field descriptors should be printed
     * @param printFieldValue
     *        whether or not the values mapped to the field descriptors should be printed
     * @param reverseOrder
     *        whether or not the field descriptors and their mapped values should be printed in
     *        reverse order
     * @return a string representation of this parameter descriptor
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: method toString kept in one block
    public String toString(final String keySeparator, final String entrySeparator,
            final boolean printClassName, final boolean printFieldName,
            final boolean printFieldValue, final boolean reverseOrder) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialize the builder
        final StringBuilder builder = new StringBuilder();

        // Class name
        if (printClassName) {
            builder.append(ParameterDescriptor.class.getSimpleName());
            builder.append("[");
        }

        // Print field name and/or value
        if (printFieldName || printFieldValue) {
            final List<String> fieldStrings = new ArrayList<>(this.fields.size());

            // Loop on each field
            for (final Map.Entry<FieldDescriptor<?>, Object> entry : this.fields.entrySet()) {
                final FieldDescriptor<?> fieldDescriptor = entry.getKey();
                final Object fieldValue = entry.getValue();

                final String name = fieldDescriptor.getName().trim();
                final String value = fieldDescriptor.printField(fieldValue).trim();

                if (printFieldName && printFieldValue
                        && ((name.length() > 0) || (value.length() > 0))) {
                    // Print field name and value
                    fieldStrings.add(name + keySeparator + value);
                } else {
                    if (printFieldName && (name.length() > 0)) {
                        // Print field name
                        fieldStrings.add(fieldDescriptor.getName());
                    }
                    if (printFieldValue && (value.length() > 0)) {
                        // Print field value
                        fieldStrings.add(fieldDescriptor.printField(fieldValue));
                    }
                }
            }

            // Reverse field order if needed
            if (reverseOrder) {
                Collections.reverse(fieldStrings);
            }
            builder.append(String.join(entrySeparator, fieldStrings));
        }

        if (printClassName) {
            builder.append("]");
        }

        // Extract the final string from the builder
        return builder.toString();
    }

    /**
     * Performs a shallow copy of the parameter descriptor (the references to the field descriptors
     * and the mapped values are preserved).
     *
     * @return a shallow copy of this parameter descriptor
     */
    public ParameterDescriptor copy() {
        return new ParameterDescriptor(this.fields).setMutability(this.mutable);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * This method only compares the associated field descriptors and the values mapped to them when
     * checking if two {@linkplain ParameterDescriptor} instances are equal or not. The order in
     * which the field descriptors are stored and their mutability are not taken into account.
     * </p>
     *
     * @param object
     *        the reference object with which to compare
     * @return {@code true} if this object is the same as the provided object, {@code false}
     *         otherwise
     */
    @Override
    public boolean equals(final Object object) {
        boolean out = false;

        if (object == this) {
            out = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            final ParameterDescriptor other = (ParameterDescriptor) object;
            out = Objects.equals(this.fields, other.fields);
        }

        return out;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.fields);
    }

    /**
     * Checks if this parameter descriptor is equal to another one with respect to a given field
     * descriptor.
     * <p>
     * This method considers that two {@linkplain ParameterDescriptor} instances are equal if the
     * specified field descriptor is mapped to the same value in both instances, or if it is not
     * associated with either of the instances.
     * </p>
     *
     * @param parameterDescriptor
     *        the parameter descriptor to compare this instance with
     * @param fieldDescriptor
     *        the field descriptor to check
     * @return {@code true} if the specified field descriptor is mapped to the same value in both
     *         parameter descriptors, {@code false} otherwise
     */
    public boolean equals(final ParameterDescriptor parameterDescriptor,
            final FieldDescriptor<?> fieldDescriptor) {
        return areEqual(this, parameterDescriptor, fieldDescriptor);
    }

    /**
     * Checks if this parameter descriptor is equal to another one with respect to multiple field
     * descriptors.
     * <p>
     * This method considers that two {@linkplain ParameterDescriptor} instances are equal if the
     * specified field descriptors are mapped to the same values in both instances, or if they are
     * not associated with either of the instances.
     * </p>
     *
     * @param parameterDescriptor
     *        the parameter descriptor to compare this instance with
     * @param fieldDescriptors
     *        the field descriptors to check
     * @return {@code true} if the specified field descriptors are mapped to the same values in both
     *         parameter descriptors, {@code false} otherwise
     */
    public boolean equals(final ParameterDescriptor parameterDescriptor,
            final FieldDescriptor<?>... fieldDescriptors) {
        return areEqual(this, parameterDescriptor, fieldDescriptors);
    }

    /**
     * Checks if this parameter descriptor is equal to another one with respect to multiple field
     * descriptors.
     * <p>
     * This method considers that two {@linkplain ParameterDescriptor} instances are equal if the
     * specified field descriptors are mapped to the same values in both instances, or if they are
     * not associated with either of the instances.
     * </p>
     *
     * @param parameterDescriptor
     *        the parameter descriptor to compare this instance with
     * @param fieldDescriptors
     *        the field descriptors to check
     * @return {@code true} if the specified field descriptors are mapped to the same values in both
     *         parameter descriptors, {@code false} otherwise
     */
    public boolean equals(final ParameterDescriptor parameterDescriptor,
            final Collection<FieldDescriptor<?>> fieldDescriptors) {
        return areEqual(this, parameterDescriptor, fieldDescriptors);
    }

    /**
     * Checks if two parameter descriptors are equal with respect to a given field descriptor.
     * <p>
     * This method considers that two {@linkplain ParameterDescriptor} instances are equal if the
     * specified field descriptor is mapped to the same value in both instances, or if it is not
     * associated with either of the instances.
     * </p>
     *
     * @param parameterDescriptor1
     *        the first parameter descriptor
     * @param parameterDescriptor2
     *        the second parameter descriptor
     * @param fieldDescriptor
     *        the field descriptor to check
     * @return {@code true} if the specified field descriptor is mapped to the same value in both
     *         parameter descriptors, {@code false} otherwise
     */
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    // Reason: object equality is required
    public static boolean areEqual(final ParameterDescriptor parameterDescriptor1,
            final ParameterDescriptor parameterDescriptor2,
            final FieldDescriptor<?> fieldDescriptor) {
        boolean out = false;

        if (parameterDescriptor1 == parameterDescriptor2) {
            out = true;
        } else if ((parameterDescriptor1 != null) && (parameterDescriptor2 != null)) {
            final Object object1 = parameterDescriptor1.getFieldValue(fieldDescriptor);
            final Object object2 = parameterDescriptor2.getFieldValue(fieldDescriptor);
            out = Objects.deepEquals(object1, object2);
        }

        return out;
    }

    /**
     * Checks if two parameter descriptors are equal with respect to multiple field descriptors.
     * <p>
     * This method considers that two {@linkplain ParameterDescriptor} instances are equal if the
     * specified field descriptors are mapped to the same values in both instances, or if they are
     * not associated with either of the instances.
     * </p>
     *
     * @param parameterDescriptor1
     *        the first parameter descriptor
     * @param parameterDescriptor2
     *        the second parameter descriptor
     * @param fieldDescriptors
     *        the field descriptors to check
     * @return {@code true} if the specified field descriptors are mapped to the same values in both
     *         parameter descriptors, {@code false} otherwise
     * @throws NullArgumentException
     *         if the provided field descriptor array is {@code null}
     */
    public static boolean areEqual(final ParameterDescriptor parameterDescriptor1,
            final ParameterDescriptor parameterDescriptor2,
            final FieldDescriptor<?>... fieldDescriptors) {
        checkNotNull(fieldDescriptors, FIELD_DESCRIPTORS);
        return areEqual(parameterDescriptor1, parameterDescriptor2, Arrays.asList(fieldDescriptors));
    }

    /**
     * Checks if two parameter descriptors are equal with respect to multiple field descriptors.
     * <p>
     * This method considers that two {@linkplain ParameterDescriptor} instances are equal if the
     * specified field descriptors are mapped to the same values in both instances, or if they are
     * not associated with either of the instances.
     * </p>
     *
     * @param parameterDescriptor1
     *        the first parameter descriptor
     * @param parameterDescriptor2
     *        the second parameter descriptor
     * @param fieldDescriptors
     *        the field descriptors to check
     * @return {@code true} if the specified field descriptors are mapped to the same values in both
     *         parameter descriptors, {@code false} otherwise
     * @throws NullArgumentException
     *         if the provided field descriptor collection is {@code null}
     */
    public static boolean areEqual(final ParameterDescriptor parameterDescriptor1,
            final ParameterDescriptor parameterDescriptor2,
            final Collection<FieldDescriptor<?>> fieldDescriptors) {
        checkNotNull(fieldDescriptors, FIELD_DESCRIPTORS);
        boolean out = true;
        for (final FieldDescriptor<?> fieldDescriptor : fieldDescriptors) {
            out &= areEqual(parameterDescriptor1, parameterDescriptor2, fieldDescriptor);
        }
        return out;
    }

    /**
     * Ensures this parameter descriptor is currently mutable and throws an exception if that's not
     * the case.
     * 
     * @throws IllegalStateException
     *         if the parameter descriptor is currently immutable
     */
    private void checkMutability() {
        if (!this.mutable) {
            throw PatriusException
                    .createIllegalStateException(PatriusMessages.IMMUTABLE_PARAM_DESCRIPTOR);
        }
    }

    /**
     * Ensures an argument is not {@code null} and throws an exception if that's not the case.
     *
     * @param object
     *        the object to check
     * @param description
     *        a short description of the object checked
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
