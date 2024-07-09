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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;

/**
 * This utility class defines static methods to manage {@link Parameter parameters} and
 * {@link ParameterDescriptor parameter descriptors}.
 *
 * @author Hugo Veuillez (CNES)
 * @author Thibaut Bonit (Thales Services)
 * @author Pierre Seimandi (GMV)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* END-HISTORY
 */
public final class ParameterUtils {

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor.
     * This private constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private ParameterUtils() {
    }

    /**
     * Adds a given field descriptor to the parameters of a parameterizable object and maps it to
     * the specified value (existing values are overwritten when the field descriptor is already
     * associated with a parameter; parameter descriptors which are not currently mutable are
     * ignored).
     * <p>
     * Note that this method will not have any effect if
     * {@linkplain IParameterizable#getParameters() getParameters()} provides a copy of the
     * parameters stored by the class implementing the {@linkplain IParameterizable} interface
     * (instead of a direct access to their reference).
     * </p>
     *
     * @param parameterizable
     *        the parameterizable object whose parameters are to be updated
     * @param fieldDescriptor
     *        the field descriptor to add
     * @param fieldValue
     *        the value to be mapped to the field descriptor
     * @param <T>
     *        the type associated with the field descriptor
     */
    public static <T> void addFieldToParameters(final IParameterizable parameterizable,
            final FieldDescriptor<T> fieldDescriptor, final T fieldValue) {
        if (parameterizable != null) {
            addFieldToParameters(parameterizable.getParameters(), fieldDescriptor, fieldValue);
        }
    }

    /**
     * Adds a given field descriptor to multiple parameters and maps it to the specified value
     * (existing values are overwritten when the field descriptor is already associated with a
     * parameter; parameter descriptors which are not currently mutable are ignored).
     *
     * @param parameters
     *        the parameters whose parameter descriptor is to be updated
     * @param fieldDescriptor
     *        the field descriptor to add
     * @param fieldValue
     *        the value to be mapped to the field descriptor
     * @param <T>
     *        the type associated with the field descriptor
     */
    public static <T> void addFieldToParameters(final Collection<Parameter> parameters,
            final FieldDescriptor<T> fieldDescriptor, final T fieldValue) {
        if (parameters != null) {
            for (final Parameter parameter : parameters) {
                if (parameter != null) {
                    final ParameterDescriptor descriptor = parameter.getDescriptor();
                    if ((descriptor != null) && descriptor.isMutable()) {
                        descriptor.addField(fieldDescriptor, fieldValue);
                    }
                }
            }
        }
    }

    /**
     * Adds a given field descriptor to multiple parameter descriptors and maps it to the specified
     * value (existing values are overwritten when the field descriptor is already associated with a
     * parameter descriptor; parameter descriptors which are not currently mutable are ignored).
     * 
     * @param descriptors
     *        the parameters descriptors to be updated
     * @param fieldDescriptor
     *        the field descriptor to add
     * @param fieldValue
     *        the value to be mapped to the field descriptor
     * @param <T>
     *        the type associated with the field descriptor
     */
    public static <T> void addFieldToParameterDescriptors(
            final Collection<ParameterDescriptor> descriptors,
            final FieldDescriptor<T> fieldDescriptor, final T fieldValue) {
        if (descriptors != null) {
            for (final ParameterDescriptor descriptor : descriptors) {
                if ((descriptor != null) && descriptor.isMutable()) {
                    descriptor.addField(fieldDescriptor, fieldValue);
                }
            }
        }
    }

    /**
     * Build a list of N parameter descriptors, each one associated with a single
     * {@link StandardFieldDescriptors#PARAMETER_NAME PARAMETER_NAME} descriptor mapped to the
     * string "p" + i, where i is the i<sup>th</sup> element (starting from 0).
     *
     * @param n
     *        the number N of parameter descriptors to build
     * @return the parameter descriptors build
     */
    public static List<ParameterDescriptor> buildDefaultParameterDescriptors(final int n) {
        return buildDefaultParameterDescriptors(n, 0);
    }

    /**
     * Build a list of N parameter descriptors, each one associated with a single
     * {@link StandardFieldDescriptors#PARAMETER_NAME PARAMETER_NAME} descriptor mapped to the
     * string "p" + k, where k is the index of the i<sup>th</sup> element (starting from 0) shifted
     * by the specified start index. For example, if the start index is 3, then k is 3 for the first
     * element, 4, for the second element, etc.
     *
     * @param n
     *        the number N of parameter descriptors to build
     * @param startIndex
     *        the start index
     * @return the parameter descriptors build
     */
    public static List<ParameterDescriptor> buildDefaultParameterDescriptors(final int n,
            final int startIndex) {
        final List<ParameterDescriptor> list = new ArrayList<>(n);
        for (int i = startIndex; i < (startIndex + n); i++) {
            list.add(new ParameterDescriptor("p" + i));
        }
        return list;
    }

    /**
     * Builds the parameter descriptors associated with a given orbit type and position angle type,
     * each representing one of the orbital coordinates.
     * <p>
     * The method always returns six parameter descriptors, each one associated with a single
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate descriptor} mapped
     * to the described coordinate (for example, {@linkplain CartesianCoordinate#X X},
     * {@linkplain CartesianCoordinate#Y Y}, {@linkplain CartesianCoordinate#Z Z},
     * {@linkplain CartesianCoordinate#VX VX}, {@linkplain CartesianCoordinate#VY VY},
     * {@linkplain CartesianCoordinate#VZ VZ} if the orbit type is {@linkplain OrbitType#CARTESIAN
     * CARTESIAN}).
     * </p>
     *
     * @param orbitType
     *        the orbit type
     * @param positionAngle
     *        the position angle type
     * @return a list of six parameter descriptors, each one associated with a single orbital
     *         coordinate descriptor mapped to the described coordinate
     */
    public static List<ParameterDescriptor> buildOrbitalParameterDescriptors(
            final OrbitType orbitType, final PositionAngle positionAngle) {
        final int size = 6;
        final List<ParameterDescriptor> parameterDescriptors = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final OrbitalCoordinate orbitalCoordinateType = orbitType.getCoordinateType(i,
                    positionAngle);
            parameterDescriptors.add(new ParameterDescriptor(
                    StandardFieldDescriptors.ORBITAL_COORDINATE, orbitalCoordinateType));
        }

        return parameterDescriptors;
    }

    /**
     * Builds the parameters associated with a given orbit type and position angle type, each one
     * representing one of the orbital coordinates.
     * <p>
     * The method always returns six parameters, each one with associated with a single
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate descriptor} mapped
     * to the described coordinate (for example, {@linkplain CartesianCoordinate#X X},
     * {@linkplain CartesianCoordinate#Y Y}, {@linkplain CartesianCoordinate#Z Z},
     * {@linkplain CartesianCoordinate#VX VX}, {@linkplain CartesianCoordinate#VY VY},
     * {@linkplain CartesianCoordinate#VZ VZ} if the orbit type is {@linkplain OrbitType#CARTESIAN
     * CARTESIAN}). The parameter value is set to zero by default.
     * </p>
     *
     * @param orbitType
     *        the orbit type
     * @param positionAngle
     *        the position angle type
     * @return a list of six parameters, each one associated with a single orbital coordinate
     *         descriptor mapped to the described coordinate
     */
    public static List<Parameter> buildOrbitalParameters(final OrbitType orbitType,
            final PositionAngle positionAngle) {
        final List<ParameterDescriptor> descriptors = ParameterUtils
                .buildOrbitalParameterDescriptors(orbitType, positionAngle);

        final List<Parameter> parameters = new ArrayList<>(descriptors.size());
        for (final ParameterDescriptor descriptor : descriptors) {
            parameters.add(new Parameter(descriptor, 0.));
        }
        return parameters;
    }

    /**
     * Concatenates the names of multiple parameters.
     *
     * @param parameters
     *        the parameters whose names are to be concatenated
     * @param nameSeparator
     *        the string used as a separator between names
     * @param fieldSeparator
     *        the string used as a separator between the field values of a parameter descriptor
     * @param reverseOrder
     *        whether or not the field values of each parameter descriptor should be printed in
     *        reverse order
     * @return the concatenated name
     */
    public static String concatenateParameterNames(final Collection<Parameter> parameters,
            final String nameSeparator, final String fieldSeparator, final boolean reverseOrder) {
        final List<String> names = new ArrayList<>(parameters.size());
        for (final Parameter parameter : parameters) {
            if (parameter != null) {
                final String name = parameter.getName(fieldSeparator, reverseOrder);
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return String.join(nameSeparator, names);
    }

    /**
     * Concatenates the names of multiple parameter descriptors.
     *
     * @param descriptors
     *        the parameter descriptors whose names are to be concatenated
     * @param nameSeparator
     *        the string used as a separator between names
     * @param fieldSeparator
     *        the string used as a separator between the field values of a parameter descriptor
     * @param reverseOrder
     *        whether or not the field values of each parameter descriptor should be printed in
     *        reverse order
     * @return the concatenated name
     */
    public static String concatenateParameterDescriptorNames(
            final Collection<ParameterDescriptor> descriptors, final String nameSeparator,
            final String fieldSeparator, final boolean reverseOrder) {
        final List<String> names = new ArrayList<>(descriptors.size());
        for (final ParameterDescriptor descriptor : descriptors) {
            if (descriptor != null) {
                final String name = descriptor.getName(fieldSeparator, reverseOrder);
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return String.join(nameSeparator, names);
    }

    /**
     * Extracts the parameters associated with a given field descriptor.
     *
     * @param parameters
     *        the parameters to check
     * @param fieldDescriptor
     *        the field descriptor which must be associated to the parameters
     *
     * @return the parameters which are associated to the specified field descriptor containing the
     *         field and which matches the field filter
     */
    public static List<Parameter> extractParameters(final Collection<Parameter> parameters,
            final FieldDescriptor<?> fieldDescriptor) {
        return extractParameters(parameters, fieldDescriptor, (fieldValue) -> true);
    }

    /**
     * Extracts the parameters associated with a given field descriptor if the mapped value matches
     * the specified predicate.
     *
     * @param parameters
     *        the parameters to check
     * @param fieldDescriptor
     *        the field descriptor which must be associated to the parameters
     * @param fieldFilter
     *        the predicate on the field value mapped to the specified field descriptor
     * @param <T>
     *        the type of fields associated with the provided field descriptor
     * @return the parameters matching the specified criteria
     */
    public static <T> List<Parameter> extractParameters(final Collection<Parameter> parameters,
            final FieldDescriptor<T> fieldDescriptor, final Predicate<T> fieldFilter) {
        final List<Parameter> out = new ArrayList<>();

        if (parameters != null) {
            for (final Parameter parameter : parameters) {
                if (parameter != null) {
                    final ParameterDescriptor descriptor = parameter.getDescriptor();
                    if (descriptor != null) {
                        final T fieldValue = descriptor.getFieldValue(fieldDescriptor);
                        if ((fieldValue != null)
                                && ((fieldFilter == null) || fieldFilter.test(fieldValue))) {
                            out.add(parameter);
                        }
                    }
                }
            }
        }

        return out;
    }

    /**
     * Extracts the parameter descriptors from all the provided parameters and returns them into a
     * new list.
     *
     * @param parameters
     *        the parameters from which to extract the parameter descriptor
     * @return the list of parameter descriptors
     */
    public static List<ParameterDescriptor> extractParameterDescriptors(
            final Collection<Parameter> parameters) {
        final List<ParameterDescriptor> out;

        if (parameters == null) {
            out = new ArrayList<>(0);
        } else {
            out = new ArrayList<>(parameters.size());
            for (final Parameter parameter : parameters) {
                if (parameter != null) {
                    out.add(parameter.getDescriptor());
                }
            }
        }

        return out;
    }

    /**
     * Extracts the parameter descriptors associated with a given field descriptor.
     *
     * @param parameterDescriptors
     *        the parameter descriptors to check
     * @param fieldDescriptor
     *        the field descriptor which must be associated to the parameters
     * @return the parameter descriptors associated with the specified field descriptor
     */
    public static List<ParameterDescriptor> extractParameterDescriptors(
            final Collection<ParameterDescriptor> parameterDescriptors,
            final FieldDescriptor<?> fieldDescriptor) {
        return extractParameterDescriptors(parameterDescriptors, fieldDescriptor, (fieldValue) -> true);
    }

    /**
     * Extracts the parameter descriptors associated with a given field descriptor if the mapped
     * value matches the specified predicate.
     *
     * @param descriptors
     *        the parameter descriptors to check
     * @param fieldDescriptor
     *        the field descriptor which must be associated to the parameters
     * @param fieldFilter
     *        the predicate on the field value mapped to the specified field descriptor
     * @param <T>
     *        the type of fields associated with the provided field descriptor
     * @return @return the parameter descriptors matching the specified criteria
     */
    public static <T> List<ParameterDescriptor> extractParameterDescriptors(
            final Collection<ParameterDescriptor> descriptors,
            final FieldDescriptor<T> fieldDescriptor, final Predicate<T> fieldFilter) {
        final List<ParameterDescriptor> out = new ArrayList<>();

        if (descriptors != null) {
            for (final ParameterDescriptor descriptor : descriptors) {
                if (descriptor != null) {
                    final T fieldValue = descriptor.getFieldValue(fieldDescriptor);
                    if ((fieldValue != null)
                            && ((fieldFilter == null) || fieldFilter.test(fieldValue))) {
                        out.add(descriptor);
                    }
                }
            }
        }

        return out;
    }
}
