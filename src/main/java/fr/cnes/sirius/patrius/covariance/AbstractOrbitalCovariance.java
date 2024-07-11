/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2021 CNES
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
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrixFormat;
import fr.cnes.sirius.patrius.math.linear.SymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.parameter.FieldDescriptor;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Abstract representation of an orbital covariance.
 *
 * <p>
 * An orbital covariance associates a {@linkplain Covariance} instance with a given date and the
 * frame, orbit type (Cartesian, Keplerian, etc) and position angle type (mean, true, eccentric) in
 * which it is expressed.
 * </p>
 *
 * @param <T>
 *        the type of orbital covariance represented by this class
 *
 * @author Hugo Veuillez (CNES)
 * @author Pierre Seimandi (GMV)
 */
public abstract class AbstractOrbitalCovariance<T extends AbstractOrbitalCovariance<T>> implements
        TimeStamped, Serializable {

    /** Number of orbital parameters. */
    protected static final int ORBIT_DIMENSION = SpacecraftState.ORBIT_DIMENSION;

    /** Field descriptor used to describe the orbital coordinates. */
    protected static final FieldDescriptor<OrbitalCoordinate> ORBITAL_COORDINATE_DESCRIPTOR = 
            StandardFieldDescriptors.ORBITAL_COORDINATE;

    /** Serial version UID. */
    private static final long serialVersionUID = 8671810074857107460L;

    /** Short argument description for the covariance. */
    private static final String COVARIANCE = "covariance";

    /** Short argument description for the covariance matrices. */
    private static final String COVARIANCE_MATRIX = "covariance matrix";

    /** Short argument description for the frames. */
    private static final String FRAME = "frame";

    /** Short argument description for the orbit types. */
    private static final String ORBIT_TYPE = "orbit type";

    /** Short argument description for the position angle types. */
    private static final String POSITION_ANGLE = "position angle type";

    /** Short argument description for the orbits. */
    private static final String ORBIT = "orbit";

    /** Covariance. */
    private final Covariance covariance;

    /** Frame of the covariance. */
    private final Frame frame;

    /** Orbit type of covariance. */
    private final OrbitType orbitType;

    /** Position angle type of the covariance. */
    private final PositionAngle positionAngle;

    /**
     * Creates a new instance from the supplied covariance matrix, frame, orbit type and position
     * angle type.
     *
     * @param covarianceIn
     *        the covariance matrix
     * @param frameIn
     *        the frame of the covariance
     * @param orbitTypeIn
     *        the orbit type of the covariance
     * @param positionAngleIn
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided argument is {@code null}
     */
    public AbstractOrbitalCovariance(final SymmetricPositiveMatrix covarianceIn,
            final Frame frameIn, final OrbitType orbitTypeIn, final PositionAngle positionAngleIn) {
        this.covariance = new Covariance(requireNonNull(covarianceIn, COVARIANCE_MATRIX));
        this.frame = requireNonNull(frameIn, FRAME);
        this.orbitType = requireNonNull(orbitTypeIn, ORBIT_TYPE);
        this.positionAngle = requireNonNull(positionAngleIn, POSITION_ANGLE);
    }

    /**
     * Creates a new instance from the supplied covariance, frame, orbit type and position angle
     * type.
     *
     * @param covarianceIn
     *        the orbital covariance
     * @param frameIn
     *        the frame of the covariance
     * @param orbitTypeIn
     *        the orbit type of the covariance
     * @param positionAngleIn
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided argument is {@code null}
     */
    public AbstractOrbitalCovariance(final Covariance covarianceIn, final Frame frameIn,
            final OrbitType orbitTypeIn, final PositionAngle positionAngleIn) {
        this.covariance = requireNonNull(covarianceIn, COVARIANCE);
        this.frame = requireNonNull(frameIn, FRAME);
        this.orbitType = requireNonNull(orbitTypeIn, ORBIT_TYPE);
        this.positionAngle = requireNonNull(positionAngleIn, POSITION_ANGLE);
    }

    /**
     * Gets the covariance.
     *
     * @return the covariance
     */
    public Covariance getCovariance() {
        return this.covariance;
    }

    /**
     * Gets the covariance matrix.
     *
     * @return the covariance matrix
     */
    public SymmetricPositiveMatrix getCovarianceMatrix() {
        return this.covariance.getCovarianceMatrix();
    }

    /**
     * Gets the parameter descriptors associated with the covariance matrix.
     *
     * @return the parameter descriptors associated with the covariance matrix
     */
    public List<ParameterDescriptor> getParameterDescriptors() {
        return this.covariance.getParameterDescriptors();
    }

    /**
     * Gets the frame of the covariance.
     *
     * @return the frame of the covariance
     */
    public Frame getFrame() {
        return this.frame;
    }

    /**
     * Gets the orbit type of covariance.
     *
     * @return the orbit type of covariance
     */
    public OrbitType getOrbitType() {
        return this.orbitType;
    }

    /**
     * Gets the position angle type of the covariance.
     *
     * @return the position angle type of the covariance
     */
    public PositionAngle getPositionAngle() {
        return this.positionAngle;
    }

    /**
     * Transforms this orbital covariance to the specified frame, orbit type and position angle
     * type.
     * <p>
     * <b>Important:</b><br>
     * The use of non-Cartesian coordinates types may be incompatible with some frames (e.g. local
     * orbital frames).
     * </p>
     *
     * @param destFrame
     *        the destination frame
     * @param destOrbitType
     *        the destination orbit type
     * @param destPositionAngle
     *        the destination position angle type
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified frame, orbit type
     *         and position angle type
     */
    public abstract T transformTo(final Frame destFrame, final OrbitType destOrbitType,
            final PositionAngle destPositionAngle) throws PatriusException;

    /**
     * Transforms this orbital covariance to the specified frame, orbit type.
     * <p>
     * <b>Important:</b><br>
     * The use of non-Cartesian coordinates types may be incompatible with some frames (e.g. local
     * orbital frames).
     * </p>
     *
     * @param destFrame
     *        the destination frame
     * @param destOrbitType
     *        the destination orbit type
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified frame and orbit type
     */
    public T transformTo(final Frame destFrame, final OrbitType destOrbitType)
            throws PatriusException {
        return this.transformTo(destFrame, destOrbitType, this.positionAngle);
    }

    /**
     * Transforms this orbital covariance to the specified orbit type and position angle type.
     * <p>
     * <b>Important:</b><br>
     * The use of non-Cartesian coordinates types may be incompatible with some frames (e.g. local
     * orbital frames).
     * </p>
     *
     * @param destOrbitType
     *        the destination orbit type
     * @param destPositionAngle
     *        the destination position angle type
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified orbit type and
     *         position angle type
     */
    public T transformTo(final OrbitType destOrbitType, final PositionAngle destPositionAngle)
            throws PatriusException {
        return this.transformTo(this.frame, destOrbitType, destPositionAngle);
    }

    /**
     * Transforms this orbital covariance to the specified frame.
     * <p>
     * <b>Important:</b><br>
     * The use of non-Cartesian coordinates types may be incompatible with some frames (e.g. local
     * orbital frames).
     * </p>
     *
     * @param destFrame
     *        the destination frame
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified frame
     */
    public T transformTo(final Frame destFrame) throws PatriusException {
        return this.transformTo(destFrame, this.orbitType, this.positionAngle);
    }

    /**
     * Transforms this orbital covariance to the specified orbit type.
     * <p>
     * <b>Important:</b><br>
     * The use of non-Cartesian coordinates types may be incompatible with some frames (e.g. local
     * orbital frames).
     * </p>
     *
     * @param destOrbitType
     *        the destination orbit type
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified orbit type
     */
    public T transformTo(final OrbitType destOrbitType) throws PatriusException {
        return this.transformTo(this.frame, destOrbitType, this.positionAngle);
    }

    /**
     * Transforms this orbital covariance to the specified position angle type.
     * <p>
     * <b>Important:</b><br>
     * The use of non-Cartesian coordinates types may be incompatible with some frames (e.g. local
     * orbital frames).
     * </p>
     *
     * @param destPositionAngle
     *        the destination position angle type
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified position angle type
     */
    public T transformTo(final PositionAngle destPositionAngle) throws PatriusException {
        return this.transformTo(this.frame, this.orbitType, destPositionAngle);
    }

    /**
     * Returns a string representation of this orbital covariance which includes its date, frame,
     * orbit type and position angle type, the associated parameter descriptors, but not the
     * covariance matrix.
     * <p>
     * The date is represented in the TAI time scale.
     * </p>
     *
     * @return a string representation of the orbital covariance
     */
    @Override
    public String toString() {
        return this.toString(null);
    }

    /**
     * Returns a string representation of this orbital covariance which includes its date, frame,
     * orbit type and position angle type, the associated parameter descriptors, and the covariance
     * matrix if the provided format is not {@code null}.
     * <p>
     * The date is represented in the TAI time scale.
     * </p>
     *
     * @param realMatrixFormat
     *        the format used to represent the covariance matrix
     * @return a string representation of the orbital covariance
     */
    public String toString(final RealMatrixFormat realMatrixFormat) {
        return this.toString(realMatrixFormat, TimeScalesFactory.getTAI());
    }

    /**
     * Returns a string representation of this orbital covariance which includes its date, frame,
     * orbit type and position angle type, the associated parameter descriptors, and the covariance
     * matrix if the provided format is not {@code null}.
     *
     * @param realMatrixFormat
     *        the format used to represent the covariance matrix
     * @param timeScale
     *        the time scale used to represent the date
     *
     * @return a string representation of the orbital covariance
     */
    public String toString(final RealMatrixFormat realMatrixFormat, final TimeScale timeScale) {
        return this.toString(realMatrixFormat, timeScale, Covariance.DEFAULT_NAME_SEPARATOR,
                Covariance.DEFAULT_FIELD_SEPARATOR, true, false);
    }

    /**
     * Returns a string representation of this instance which includes the name of the class (if
     * requested), the names of the associated parameter descriptors and the the covariance matrix
     * (if the specified matrix format is not {@code null}).
     *
     * @param realMatrixFormat
     *        the format used to represent the covariance matrix
     * @param timeScale
     *        the time scale used to represent the date
     * @param nameSeparator
     *        the string to use as a separator between the names of the parameter descriptors
     * @param fieldSeparator
     *        the string to use as a separator between the field values of a parameter descriptor
     * @param printClassName
     *        whether or not the name of this class should be printed
     * @param reverseOrder
     *        whether or not the field values of each parameter descriptor should be printed in
     *        reverse order
     * @return string representation of this instance
     */
    public String toString(final RealMatrixFormat realMatrixFormat, final TimeScale timeScale,
            final String nameSeparator, final String fieldSeparator, final boolean printClassName,
            final boolean reverseOrder) {
        // Separators
        final String semicolon = ";";
        final String whitespace = " ";
        final String separator = semicolon + whitespace;
        final String lineSeparator = System.lineSeparator();

        // Build the string
        final StringBuilder builder = new StringBuilder();

        // Class name
        final String className = this.getClass().getSimpleName();
        if (printClassName) {
            builder.append(className);
            builder.append("[");
        }

        // Frame, orbit type and position angle type
        builder.append(this.getDate().toString(timeScale));
        builder.append(whitespace);
        builder.append(timeScale);
        builder.append(separator);
        builder.append(this.frame);
        builder.append(separator);
        builder.append(this.orbitType);
        builder.append(separator);
        builder.append(this.positionAngle);
        builder.append(separator);

        // Parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = this.covariance
                .getParameterDescriptors();
        builder.append("Parameters: ");
        builder.append(ParameterUtils.concatenateParameterDescriptorNames(parameterDescriptors,
                nameSeparator, fieldSeparator, reverseOrder));

        // Covariance matrix
        if (realMatrixFormat != null) {
            final String regex = "(?:\\s*[\\n|\\r]+)";
            // Extract the covariance
            final SymmetricPositiveMatrix covarianceMatrix = this.covariance.getCovarianceMatrix();
            final String initialString = realMatrixFormat.format(covarianceMatrix);

            // New line
            builder.append(semicolon);
            builder.append(lineSeparator);

            // Add the covariance matrix
            if (!printClassName) {
                builder.append(initialString.replaceAll(regex, lineSeparator));
            } else {
                // Indent space
                final int nbSpaces = className.length();
                final String indentation = String.format("%" + nbSpaces + "c", ' ');
                final String[] split = initialString.split(regex);
                final String indentedString = String.join(lineSeparator + indentation, split);
                builder.append(indentation);
                builder.append(indentedString);
            }
        }

        // Closing bracket
        if (printClassName) {
            builder.append("]");
        }

        return builder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if ((object != null) && (object.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final AbstractOrbitalCovariance<?> other = (AbstractOrbitalCovariance<?>) object;

            isEqual = true;
            isEqual &= Objects.equals(this.getDate(), other.getDate());
            isEqual &= Objects.equals(this.covariance, other.covariance);
            isEqual &= Objects.equals(this.frame, other.frame);
            isEqual &= Objects.equals(this.orbitType, other.orbitType);
            isEqual &= Objects.equals(this.positionAngle, other.positionAngle);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.getDate(), this.covariance, this.frame, this.orbitType,
                this.positionAngle);
    }

    /**
     * Checks an object and throws an exception if it is {@code null}.
     *
     * @param object
     *        the object to be checked
     * @param description
     *        a short description of the object
     * @param <T>
     *        the type of the object
     * @return the specified object, if it is not {@code null}
     * @throws IllegalArgumentException
     *         if the object is {@code null}
     */
    protected static <T> T requireNonNull(final T object, final String description) {
        if (object == null) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.NULL_NOT_ALLOWED_DESCRIPTION, description);
        }
        return object;
    }

    /**
     * Checks a collection and throws an exception if it is {@code null} or empty.
     *
     * @param collection
     *        the collection to be checked
     * @param description
     *        a short description of the collection
     * @param <T>
     *        the type of the collection
     * @return the specified collection, if it is not {@code null} nor empty
     * @throws IllegalArgumentException
     *         if the collection is {@code null} or if the collection is empty
     */
    protected static <T extends Collection<?>> T requireNonEmpty(final T collection,
            final String description) {
        if (collection == null) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.NULL_COLLECTION_NOT_ALLOWED, description);
        }
        if (collection.isEmpty()) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.EMPTY_COLLECTION_NOT_ALLOWED, description);
        }
        return collection;
    }

    /**
     * Checks an orbit and throws an exception if it is {@code null}, or if it is not defined at the
     * same date as this orbital covariance.
     *
     * @param orbit
     *        the orbit to be checked
     * @throws IllegalArgumentException
     *         if the orbit is {@code null} or if the orbit is not defined at the same date as the
     *         orbital covariance
     */
    protected void checkOrbit(final Orbit orbit) {
        final AbsoluteDate orbitDate = requireNonNull(orbit, ORBIT).getDate();
        if (!this.getDate().equals(orbitDate)) {
            throw PatriusException
                    .createIllegalArgumentException(
                            PatriusMessages.INVALID_ORBIT_DATE_COVARIANCE_MATRIX, orbitDate,
                            this.getDate());
        }
    }

    /**
     * Checks the covariance matrix and throws an exception if its dimensions are not large enough
     * to store the orbital parameters of a single orbit.
     */
    protected void checkCovarianceMatrixDimension() {
        checkCovarianceMatrixDimension(1);
    }

    /**
     * Checks the covariance matrix and throws an exception if its dimensions are not large enough
     * to store the orbital parameters of a given number of orbits.
     *
     * @param nbOrbits
     *        the number of orbits
     * @throws IllegalArgumentException
     *         if the covariance matrix is not large enough to store the orbital parameters of the
     *         specified number of orbits
     */
    protected void checkCovarianceMatrixDimension(final int nbOrbits) {
        final int size = this.covariance.getSize();
        final int minSize = nbOrbits * ORBIT_DIMENSION;
        if (size < minSize) {
            throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.INVALID_COVARIANCE_MATRIX, nbOrbits, minSize, size);
        }
    }

    /**
     * Checks that the first six parameter descriptors associated with the covariance have an
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate descriptor} which
     * is mapped to an {@linkplain OrbitalCoordinate} instance with the expected state vector index.
     *
     * @throws IllegalStateException
     *         if one of the checked parameter descriptors does not have an orbital coordinate
     *         descriptor, or if it does, but it is mapped to an orbital coordinate with a wrong
     *         state vector index
     */
    protected void checkParameterDescriptors() {
        checkParameterDescriptors(0, this.orbitType);
    }

    /**
     * Starting from the specified index, checks that the first six parameter descriptors associated
     * with the covariance have an {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital
     * coordinate descriptor} which is mapped to an {@linkplain OrbitalCoordinate} instance with the
     * expected state vector index.
     *
     * @param startIndex
     *        the start index
     * @param expectedOrbitType
     *        the orbital coordinate descriptor should be compatible with the expected orbit type
     * @throws IllegalStateException
     *         if one of the checked parameter descriptors does not have an orbital coordinate
     *         descriptor, or if it does, but it is mapped to an orbital coordinate with a wrong
     *         state vector index
     */
    protected void
            checkParameterDescriptors(final int startIndex, final OrbitType expectedOrbitType) {
        // Parameter descriptors and orbital coordinate descriptor
        final List<ParameterDescriptor> parameterDescriptors = this.covariance
                .getParameterDescriptors();
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = ORBITAL_COORDINATE_DESCRIPTOR;

        // Check the parameter descriptors related to the orbital parameters
        for (int i = 0; i < ORBIT_DIMENSION; i++) {
            final int index = startIndex + i;
            MatrixUtils.checkRowIndex(this.getCovarianceMatrix(), index);
            final ParameterDescriptor parameterDescriptor = parameterDescriptors.get(index);
            final OrbitalCoordinate orbitalCoordinate = parameterDescriptor
                    .getFieldValue(fieldDescriptor);

            if (orbitalCoordinate == null) {
                // Throw an exception if the parameter descriptor is
                // not associated with an orbital coordinate descriptor
                throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.NO_ORBITAL_COORDINATE_DESCRIPTOR, index);
            } else if (!orbitalCoordinate.getOrbitType().equals(expectedOrbitType)) {
                // Throw an exception if the mapped value is not for the expected orbit type
                throw PatriusException.createIllegalArgumentException(
                        PatriusMessages.INVALID_ORBITAL_COORDINATE_DESCRIPTOR_WRONG_ORBIT_TYPE,
                        index, orbitalCoordinate, orbitalCoordinate.getOrbitType(),
                        expectedOrbitType);
            } else if (orbitalCoordinate.getStateVectorIndex() != i) {
                // Throw an exception if the mapped value is not for the correct state vector index
                throw PatriusException
                        .createIllegalArgumentException(
                                PatriusMessages.INVALID_ORBITAL_COORDINATE_DESCRIPTOR_WRONG_STATE_VECTOR_INDEX,
                                index, orbitalCoordinate, orbitalCoordinate.getStateVectorIndex(),
                                i);
            }
        }
    }

    /**
     * Starting from the specified index, adds the proper
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate
     * descriptor} to the first six parameter descriptors associated with the covariance matrix.
     * <p>
     * This method considers that the targeted parameter descriptors are the ones related to the
     * orbital parameters, in the order defined by the {@linkplain OrbitalCoordinate} classes (
     * {@linkplain CartesianCoordinate}, {@linkplain KeplerianCoordinate}, ...). If one of the
     * parameter descriptors checked is already associated with an
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital
     * coordinate descriptor}, the behavior of the method depends on the {@code replaceDescriptors}
     * argument. If it is set to {@code true}, the previously mapped value is simply ignored and
     * replaced by the expected orbital coordinate. Otherwise, an exception is thrown.
     * </p>
     *
     * @param startIndex
     *        the start index
     * @param replaceDescriptors
     *        whether or not to replace the values mapped to the existing orbital coordinate
     *        descriptors
     * @throws IllegalStateException
     *         if {@code replaceDescriptors} is {@code true} and one of the parameter descriptors to
     *         initialize already have an orbital coordinate descriptor and it is mapped to an
     *         orbital coordinate that is not the one expected
     */
    protected void initParameterDescriptors(final int startIndex, final boolean replaceDescriptors) {
        // Parameter descriptors and orbital coordinate descriptor
        final List<ParameterDescriptor> parameterDescriptors = this.covariance
                .getParameterDescriptors();
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = ORBITAL_COORDINATE_DESCRIPTOR;

        // Check the parameter descriptors related to the orbital parameters
        for (int i = 0; i < ORBIT_DIMENSION; i++) {
            final int index = startIndex + i;
            MatrixUtils.checkRowIndex(this.getCovarianceMatrix(), index);
            final ParameterDescriptor parameterDescriptor = parameterDescriptors.get(index);
            final OrbitalCoordinate orbitalCoordinate = parameterDescriptor
                    .getFieldValue(fieldDescriptor);
            final OrbitalCoordinate expectedCoordinate = this.orbitType.getCoordinateType(i,
                    this.positionAngle);

            if (orbitalCoordinate == null) {
                // If the parameter descriptor is not already associated with an
                // orbital coordinate descriptor, add the proper descriptor.
                parameterDescriptor.addField(fieldDescriptor, expectedCoordinate);
            } else if (!orbitalCoordinate.equals(expectedCoordinate)) {
                if (replaceDescriptors) {
                    // Replace the value mapped to the orbital coordinate descriptor
                    parameterDescriptor.replaceField(fieldDescriptor, expectedCoordinate);
                } else {
                    // Throw an exception if a value is already mapped to the orbital
                    // coordinate descriptor, but it's not the expected coordinate
                    throw PatriusException
                            .createIllegalArgumentException(
                                    PatriusMessages.INVALID_ORBITAL_COORDINATE_DESCRIPTOR_ROW_ALREADY_MAPPED,
                                    index, orbitalCoordinate);
                }
            }
        }
    }

    /**
     * Returns an updated list of parameter descriptors where the orbital coordinates mapped to the
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate descriptors} were
     * all converted to the specified orbit type and position angle type.
     *
     * @param parameterDescriptors
     *        the parameter descriptors to be updated
     * @param destOrbitType
     *        the destination orbit type
     * @param destPositionAngle
     *        the destination position angle type
     * @return the updated list of parameter descriptors
     */
    protected static List<ParameterDescriptor> convertParameterDescriptors(
            final Collection<ParameterDescriptor> parameterDescriptors,
            final OrbitType destOrbitType, final PositionAngle destPositionAngle) {
        // Orbital coordinate descriptor
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = ORBITAL_COORDINATE_DESCRIPTOR;

        // Build the list of updated parameter descriptors
        final List<ParameterDescriptor> newParameterDescriptors = new ArrayList<>(
                parameterDescriptors.size());

        for (final ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            final OrbitalCoordinate orbitalCoordinate = parameterDescriptor
                    .getFieldValue(fieldDescriptor);

            if (orbitalCoordinate == null) {
                newParameterDescriptors.add(parameterDescriptor);
            } else {
                final ParameterDescriptor newParameterDescriptor = parameterDescriptor.copy();
                newParameterDescriptor.replaceField(fieldDescriptor,
                        orbitalCoordinate.convertTo(destOrbitType, destPositionAngle));
                newParameterDescriptors.add(newParameterDescriptor);
            }
        }

        // Return result
        return newParameterDescriptors;
    }
}
