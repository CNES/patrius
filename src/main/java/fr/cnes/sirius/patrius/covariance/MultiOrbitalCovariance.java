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
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3317:22/05/2023:[PATRIUS] Parametres additionels non rattaches a orbite
 * VERSION:4.11:FA:FA-3316:22/05/2023:[PATRIUS] Anomalie lors de l'evaluation d'un potentiel variable
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.SymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Orbital covariance associated with multiple orbits.
 * <p>
 * This class associates a {@linkplain Covariance} instance with multiple orbits, their common date being the date of
 * definition of the orbital covariance. The frame, orbit type (Cartesian, Keplerian, etc) and position angle type
 * (mean, true, eccentric) in which the {@linkplain Covariance} is expressed are specified at construction.
 * </p>
 * <p>
 * The covariance matrix must be at least N by N, where N is the number of orbits multiplied by the number of orbital
 * parameters ({@link SpacecraftState#ORBIT_DIMENSION}).</br> Nevertheless, it can have a bigger size to take into
 * account additional parameters. The latter can be of two different nature:
 * <ul>
 * <li>attached to a specific orbit (drag coefficient for example)</li>
 * <li>without associated orbit (environment parameter for example)</li>
 * </ul>
 * The parameters must be ordered according to the following manner: for each orbit, first the orbital parameters
 * (ordered acording to their index) then the additional parameters specific to the orbit. The additional parameters
 * that are not attached to any orbit must be placed at the end.
 * </p>
 * <p>
 * The parameter descriptors of orbital parameters must be associated to an
 * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE
 * orbital coordinate descriptor}, and these descriptors must be mapped to a valid {@linkplain OrbitalCoordinate} (one
 * with the expected orbit type and state vector index).
 * </p>
 *
 * @author Hugo Veuillez (CNES)
 * @author Pierre Seimandi (GMV)
 */
public class MultiOrbitalCovariance extends AbstractOrbitalCovariance<MultiOrbitalCovariance> {

    /** Serializable UID. */
    private static final long serialVersionUID = 961203772245088823L;

    /** Short argument description for the orbits. */
    private static final String ORBIT = "orbit";

    /** Short argument description for the list of orbits. */
    private static final String ORBITS_LIST = "orbits list";

    /**
     * Short argument description for the array storing the number of additional parameters for each
     * orbit.
     */
    private static final String ADDITIONAL_PARAMETERS_ARRAY = "additional parameters array";

    /** Date of the covariance. */
    private final AbsoluteDate date;

    /** List of associated orbits. */
    private final List<Orbit> associatedOrbits;

    /** Array storing the number of additional parameters for each orbit. */
    private final int[] nbAdditionalParameters;

    /**
     * Array storing the start index for each orbit (first row/column in the covariance matrix
     * associated the orbit).
     */
    private final int[] startIndices;

    /**
     * Creates a new instance that associates a covariance matrix with multiple orbits, the
     * covariance being defined in the specified frame, orbit type and position angle type.
     *
     * <p>
     * The covariance matrix must be at least N by N, where N is the number of orbits multiplied by the number of
     * orbital parameters. Each orbit is associated with K consecutive rows/columns, where the first six represent the
     * uncertainty on the orbital parameters and the remaining ones represent the uncertainty on the additional
     * parameters. The parameter descriptors of these first six rows/columns are automatically associated with an
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate descriptor}, mapped to a valid
     * {@linkplain OrbitalCoordinate}.
     * </p>
     * <p>
     * Note that additional parameters that are not related to any orbit can be present at the end of the covariance
     * matrix
     * </p>
     *
     * @param covarianceMatrix
     *        the covariance matrix
     * @param orbits
     *        the orbits associated with the covariance
     * @param nbAdditionalParametersIn
     *        the number of additional parameters for each orbit
     * @param frame
     *        the frame of the covariance
     * @param orbitType
     *        the orbit type of the covariance
     * @param positionAngle
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the orbits collection is null
     *         or empty or if all the orbits aren't defined at the same date
     */
    public MultiOrbitalCovariance(final SymmetricPositiveMatrix covarianceMatrix,
            final Collection<Orbit> orbits, final int[] nbAdditionalParametersIn,
            final Frame frame, final OrbitType orbitType, final PositionAngle positionAngle) {
        super(covarianceMatrix, frame, orbitType, positionAngle);

        // Place the provided orbits into a new list and ensure no orbit is null.
        // In addition, check that the orbits are all defined at the same date.
        this.associatedOrbits = buildOrbitsList(orbits);
        this.date = this.associatedOrbits.get(0).getDate();

        // Check the dimensions of the covariance matrix
        final int nbOrbits = this.associatedOrbits.size();
        this.checkCovarianceMatrixDimension(nbOrbits);

        // Check the array storing the number of additional parameters for each orbit
        checkAdditionalParametersArray(nbAdditionalParametersIn, nbOrbits, this.getCovariance()
            .getSize());
        this.nbAdditionalParameters = Arrays.copyOf(nbAdditionalParametersIn, nbOrbits);

        // Build the start index array and check the associated parameter descriptors
        this.startIndices = new int[nbOrbits];
        int startIndex = 0;
        for (int i = 0; i < nbOrbits; i++) {
            this.initParameterDescriptors(startIndex, false);
            this.startIndices[i] = startIndex;
            startIndex += ORBIT_DIMENSION + nbAdditionalParametersIn[i];
        }
    }

    /**
     * Creates a new instance that associates a covariance matrix with multiple orbits, the
     * covariance being defined in the specified frame, orbit type and position angle type.
     *
     * <p>
     * The covariance matrix must be at least N by N, where N is the number of orbits multiplied by the number of
     * orbital parameters. Each orbit is associated with K consecutive rows/columns, where the first six represent the
     * uncertainty on the orbital parameters and the remaining ones represent the uncertainty on the additional
     * parameters. The parameter descriptors of these first six rows/columns must be associated to an
     * {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE orbital coordinate descriptor}, and these descriptors
     * must be mapped to a valid {@linkplain OrbitalCoordinate} (one with the expected orbit type and state vector
     * index).
     * </p>
     * <p>
     * Note that additional parameters that are not related to any orbit can be present at the end of the covariance
     * matrix
     * </p>
     *
     * @param covariance
     *        the covariance matrix
     * @param orbits
     *        the orbits associated with the covariance
     * @param nbAdditionalParametersIn
     *        the number of additional parameters for each orbit
     * @param frame
     *        the frame of the covariance
     * @param orbitType
     *        the orbit type of the covariance
     * @param positionAngle
     *        the position angle type of the covariance
     * @throws IllegalArgumentException
     *         if any of the provided arguments is {@code null} or if the orbits collection is null
     *         or empty or if all the orbits aren't defined at the same date
     */
    public MultiOrbitalCovariance(final Covariance covariance, final Collection<Orbit> orbits,
            final int[] nbAdditionalParametersIn, final Frame frame, final OrbitType orbitType,
            final PositionAngle positionAngle) {
        super(covariance, frame, orbitType, positionAngle);

        // Place the provided orbits into a new list and ensure no orbit is null.
        // In addition, check that the orbits are all defined at the same date.
        this.associatedOrbits = buildOrbitsList(orbits);
        this.date = this.associatedOrbits.get(0).getDate();

        // Check the dimensions of the covariance matrix
        final int nbOrbits = this.associatedOrbits.size();
        this.checkCovarianceMatrixDimension(nbOrbits);

        // Check the array storing the number of additional parameters for each orbit
        checkAdditionalParametersArray(nbAdditionalParametersIn, nbOrbits, this.getCovariance()
            .getSize());
        this.nbAdditionalParameters = Arrays.copyOf(nbAdditionalParametersIn, nbOrbits);

        // Build the start index array and check the associated parameter descriptors
        this.startIndices = new int[nbOrbits];
        int startIndex = 0;
        for (int i = 0; i < nbOrbits; i++) {
            this.checkParameterDescriptors(startIndex, this.getOrbitType());
            this.startIndices[i] = startIndex;
            startIndex += ORBIT_DIMENSION + nbAdditionalParametersIn[i];
        }
    }

    /**
     * Builds the list of associated orbits.
     * <p>
     * The provided collection must not be empty and must not contain any {@code null} orbit.<br>
     * In addition, all the orbits must be defined at the same date.
     * </p>
     *
     * @param orbits
     *        the associated orbits
     * @return the list of associated orbits
     * @throws IllegalArgumentException
     *         if the orbits collection is null or empty or if all the orbits aren't defined at the
     *         same date
     */
    private static List<Orbit> buildOrbitsList(final Collection<Orbit> orbits) {
        // Ensure the list of orbits is not null or empty.
        requireNonEmpty(orbits, ORBITS_LIST);

        final List<Orbit> out = new ArrayList<>(orbits.size());
        AbsoluteDate referenceDate = null;
        // Loop on each orbit
        for (final Orbit orbit : orbits) {
            // Ensure the orbit isn't null and extract its date
            final AbsoluteDate date = requireNonNull(orbit, ORBIT).getDate();
            // Check each orbit has the same date
            if (referenceDate == null) {
                referenceDate = date;
            } else if (!date.equals(referenceDate)) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.INVALID_ORBITS_DATE_COVARIANCE_MATRIX, date, referenceDate);
            }
            out.add(orbit);
        }

        return out;
    }

    /**
     * Checks the array containing the number of additional parameters for each orbit and throws an
     * exception if it is invalid.
     * <p>
     * The tested array is considered invalid if it is {@code null}, if its length does not match the specified number
     * of orbits, if it contains a negative value, or if the reconstructed covariance size (that is, the total number of
     * additional parameters plus six times the number of orbits) does not match the actual covariance size.
     * </p>
     *
     * @param nbAdditionalParameters
     *        the array containing the number of additional parameters for each orbit
     * @param nbOrbits
     *        the number of orbits
     * @param covarianceSize
     *        the size of the associated covariance matrix
     * @throws IllegalArgumentException
     *         if the provided array is {@code null}, if the length of the provided array is not
     *         equal to the specified number of orbit, if the array contains a negative value, or if
     *         the reconstructed covariance size is greater than the actual covariance size
     */
    private static void checkAdditionalParametersArray(final int[] nbAdditionalParameters,
                                                       final int nbOrbits, final int covarianceSize) {
        // Ensure the tested array is not null
        requireNonNull(nbAdditionalParameters, ADDITIONAL_PARAMETERS_ARRAY);

        // Ensure the length of the array matches the number orbits
        final int length = nbAdditionalParameters.length;
        if (length != nbOrbits) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_PARAMETERS_COUNT_NOT_MATCH_ORBITS_NUMBER, length,
                nbOrbits);
        }

        // Ensure the number of additional parameters is positive for each orbit
        int totalSize = 0;
        for (int i = 0; i < length; i++) {
            if (nbAdditionalParameters[i] < 0) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.NEGATIVE_PARAMETERS_COUNT, nbAdditionalParameters[i]);
            }
            totalSize += ORBIT_DIMENSION + nbAdditionalParameters[i];
        }

        // Ensure the provided numbers are consistent with the size of the covariance matrix
        if (totalSize > covarianceSize) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_PARAMETERS_COUNT_NOT_MATCH_COVARIANCE_SIZE, totalSize,
                covarianceSize);
        }
    }

    /**
     * Checks the provided index and throws an exception if it not a valid index for the list of the
     * associated orbits.
     *
     * @param index
     *        the orbit index to be checked
     * @throws IllegalArgumentException
     *         if the provided index is not a valid index for the list of associated orbits
     */
    private void checkOrbitIndex(final int index) {
        if (index < 0 || index > this.associatedOrbits.size() - 1) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.INVALID_ORBIT_INDEX, index, this.associatedOrbits.size() - 1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Gets the list of associated orbits.
     *
     * @return the list of associated orbits
     */
    public List<Orbit> getOrbits() {
        return new ArrayList<>(this.associatedOrbits);
    }

    /**
     * Gets a specific orbit from the list of associated orbits.
     *
     * @param index
     *        the index of the orbit to be retrieved
     * @return the selected associated orbit
     * @throws IllegalArgumentException
     *         if the provided index is invalid
     */
    public Orbit getOrbit(final int index) {
        checkOrbitIndex(index);
        return this.associatedOrbits.get(index);
    }

    /**
     * Extracts the orbital covariance of the selected orbit.</br>
     * By default, does not include the additional parameters that are not attached to any orbit.
     *
     * @param index
     *        the index of the selected orbit in the orbits list
     * @return the orbital covariance of the selected orbit
     * @throws IllegalArgumentException
     *         if the provided index is invalid
     */
    public OrbitalCovariance getOrbitalCovariance(final int index) {
        return getOrbitalCovariance(index, false);
    }

    /**
     * Extracts the orbital covariance of the selected orbit.
     *
     * @param index
     *        the index of the selected orbit in the orbits list
     * @param includeNonOrbitalAdditionalParameters
     *        include the additional parameters that are not attached to any orbit (if true)
     * @return the orbital covariance of the selected orbit
     * @throws IllegalArgumentException
     *         if the provided index is invalid
     */
    public OrbitalCovariance getOrbitalCovariance(final int index,
                                                  final boolean includeNonOrbitalAdditionalParameters) {
        // Selected orbit
        final Orbit orbit = this.getOrbit(index);

        // Index of the orbital element and attached additional parameters
        final int startIndex = this.startIndices[index];
        final int nbIndexedOrbitalElements = ORBIT_DIMENSION + this.nbAdditionalParameters[index];
        final int[] indexedOrbitIndices = IntStream.range(startIndex, startIndex + nbIndexedOrbitalElements).toArray();

        // Index of all the elements
        final int[] indices;
        if (includeNonOrbitalAdditionalParameters) {
            // If include non orbital additional parameters
            // Add the non orbital additional parameter index to indices
            final int nbOrbits = this.startIndices.length;
            final int firstNonOrbitalAdditionalIndex =
                this.startIndices[nbOrbits - 1] + this.nbAdditionalParameters[nbOrbits - 1] + ORBIT_DIMENSION;
            final int nbNonOrbitalAdditionalParam = this.getCovariance().getSize() - firstNonOrbitalAdditionalIndex;

            final int[] nonOrbitalIndices =
                IntStream.range(firstNonOrbitalAdditionalIndex,
                    firstNonOrbitalAdditionalIndex + nbNonOrbitalAdditionalParam).toArray();

            indices = new int[nbIndexedOrbitalElements + nbNonOrbitalAdditionalParam];
            System.arraycopy(indexedOrbitIndices, 0, indices, 0, indexedOrbitIndices.length);
            System.arraycopy(nonOrbitalIndices, 0, indices, indexedOrbitIndices.length, nonOrbitalIndices.length);
        } else {
            // If not: keep the indexedOrbitIndices
            indices = indexedOrbitIndices;
        }

        // Extract the covariance associated with the selected orbit
        final Covariance covariance = this.getCovariance().getSubCovariance(indices);

        // Build a new orbital covariance using the extracted covariance and the selected orbit
        return new OrbitalCovariance(covariance, orbit);
    }

    /**
     * Computes the relative covariance between two orbits.
     * <p>
     * The relative covariance matrix is defined by:<br>
     * C<sub>rel</sub> = C<sub>i</sub> + C<sub>j</sub> -C<sub>i,j</sub> -C<sub>j,i</sub><br>
     * where C<sub>i</sub> and C<sub>j</sub> are the covariance submatrices related representing the
     * uncertainty on the orbital parameters of the i<sup>th</sup> and j<sup>th</sup> orbits, and
     * where C<sub>i,j</sub> and C<sub>j,i</sub> are the submatrices representing the correlations
     * between the orbital parameters of the two orbits.
     * </p>
     * <p>
     * As an example, for three satellites the complete covariance matrix would be:<br>
     * [C<sub>1</sub>, C<sub>1,2</sub>, C<sub>1,3</sub>]<br>
     * [C<sub>2,1</sub>, C<sub>2</sub>, C<sub>2,3</sub>]<br>
     * [C<sub>3,1</sub>, C<sub>3,2</sub>, C<sub>3</sub>]<br>
     * </p>
     * <p>
     * The parameter descriptors associated with the relative covariance matrix are built by
     * intersecting the parameter descriptors describing the same orbital coordinate. This
     * intersection only keeps the identical field descriptors and may result in an empty parameter
     * descriptor if the orbital coordinate descriptors are mapped to different kind of orbital
     * coordinates. Since {@linkplain Covariance} instances do not allow empty parameter
     * descriptors, an exception is thrown if that occurs.
     * </p>
     *
     * @param index1
     *        the index of the first selected orbit in the list of associated orbits
     * @param index2
     *        the index of the second selected orbit in the list of associated orbits
     * @return the relative covariance between the two selected orbits
     */
    public Covariance getRelativeCovariance(final int index1, final int index2) {
        // Start indices
        final int startIndex1 = this.startIndices[index1];
        final int startIndex2 = this.startIndices[index2];

        // Parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = this.getParameterDescriptors();
        final List<ParameterDescriptor> newParameterDescriptors = new ArrayList<>(ORBIT_DIMENSION);
        for (int i = 0; i < ORBIT_DIMENSION; i++) {
            final ParameterDescriptor parameterDescriptor1 = parameterDescriptors.get(startIndex1
                    + i);
            final ParameterDescriptor parameterDescriptor2 = parameterDescriptors.get(startIndex2
                    + i);
            newParameterDescriptors
                .add(parameterDescriptor1.intersectionWith(parameterDescriptor2));
        }

        // Return the relative covariance
        final SymmetricPositiveMatrix covarianceMatrix = getRelativeCovarianceMatrix(index1, index2);
        return new Covariance(covarianceMatrix, newParameterDescriptors);
    }

    /**
     * Computes the relative covariance matrix between two orbits.
     * <p>
     * The relative covariance matrix is defined by:<br>
     * C<sub>rel</sub> = C<sub>i</sub> + C<sub>j</sub> -C<sub>i,j</sub> -C<sub>j,i</sub><br>
     * where C<sub>i</sub> and C<sub>j</sub> are the covariance submatrices related representing the
     * uncertainty on the orbital parameters of the i<sup>th</sup> and j<sup>th</sup> orbits, and
     * where C<sub>i,j</sub> and C<sub>j,i</sub> are the submatrices representing the correlations
     * between the orbital parameters of the two orbits.
     * </p>
     * <p>
     * As an example, for three satellites the complete covariance matrix would be:<br>
     * [C<sub>1</sub>, C<sub>1,2</sub>, C<sub>1,3</sub>]<br>
     * [C<sub>2,1</sub>, C<sub>2</sub>, C<sub>2,3</sub>]<br>
     * [C<sub>3,1</sub>, C<sub>3,2</sub>, C<sub>3</sub>]<br>
     * </p>
     *
     * @param index1
     *        the index of the first selected orbit in the list of associated orbits
     * @param index2
     *        the index of the second selected orbit in the list of associated orbits
     * @return the relative covariance between the two selected orbits
     */
    public ArrayRowSymmetricPositiveMatrix getRelativeCovarianceMatrix(final int index1,
                                                                       final int index2) {
        // Start indices
        final int startIndex1 = this.startIndices[index1];
        final int startIndex2 = this.startIndices[index2];
        final int finalIndex1 = startIndex1 + ORBIT_DIMENSION - 1;
        final int finalIndex2 = startIndex2 + ORBIT_DIMENSION - 1;

        // Initial covariance matrix
        final SymmetricPositiveMatrix covarianceMatrix = this.getCovarianceMatrix();

        // Submatrices related to the orbital parameters of each orbit
        final RealMatrix subMatrix1 = covarianceMatrix.getSubMatrix(startIndex1, finalIndex1);
        final RealMatrix subMatrix2 = covarianceMatrix.getSubMatrix(startIndex2, finalIndex2);

        // Submatrices related to the correlation between
        final RealMatrix subMatrix12 = covarianceMatrix.getSubMatrix(startIndex1, finalIndex1,
            startIndex2, finalIndex2);
        final RealMatrix subMatrix21 = covarianceMatrix.getSubMatrix(startIndex2, finalIndex2,
            startIndex1, finalIndex1);

        // Relative covariance matrix
        final RealMatrix matrix = subMatrix1.add(subMatrix2).subtract(subMatrix12)
            .subtract(subMatrix21);
        return new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, matrix);
    }

    /**
     * Shifts the orbital covariance in time by a given duration.
     * <p>
     * The shift of the orbital covariance is based on a simple Keplerian model. It is not intended
     * as a replacement for proper covariance propagation, but it should be sufficient for small
     * time shifts or coarse accuracy.
     * </p>
     *
     * @param dt
     *        the time shift
     * @return the time-shifted orbital covariance
     * @throws PatriusException
     *         if the Keplerian transition matrix cannot be computed
     */
    public MultiOrbitalCovariance shiftedBy(final double dt) throws PatriusException {
        // Associated frame, orbit type, position angle type and covariance
        final Frame frame = this.getFrame();
        final OrbitType orbitType = this.getOrbitType();
        final PositionAngle positionAngle = this.getPositionAngle();
        final Covariance covariance = this.getCovariance();

        // Compute the Jacobians matrix
        final int n = covariance.getSize();
        final int nbOrbits = this.associatedOrbits.size();
        final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(n);

        for (int i = 0; i < nbOrbits; i++) {
            final Orbit orbit = this.associatedOrbits.get(i);
            final RealMatrix subJacobians = orbit.getJacobian(frame, frame, orbitType, orbitType,
                positionAngle, positionAngle);
            final int startIndex = this.startIndices[i];
            jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
        }

        // Shifted orbits
        final List<Orbit> shiftedOrbits = new ArrayList<>(this.associatedOrbits.size());
        for (final Orbit orbit : this.associatedOrbits) {
            shiftedOrbits.add(orbit.shiftedBy(dt));
        }

        // Return the shifted orbital covariance
        // (the parameter descriptors remain unchanged)
        final Covariance shiftedCovariance = this.getCovariance().quadraticMultiplication(
            jacobians, this.getParameterDescriptors());
        return new MultiOrbitalCovariance(shiftedCovariance, shiftedOrbits,
            this.nbAdditionalParameters, frame, orbitType, positionAngle);
    }

    /** {@inheritDoc} */
    @Override
    public MultiOrbitalCovariance transformTo(final Frame destFrame, final OrbitType destOrbitType,
                                              final PositionAngle destPositionAngle)
        throws PatriusException {
        final MultiOrbitalCovariance out;
        if (this.getFrame().equals(destFrame) && this.getOrbitType().equals(destOrbitType)
                && this.getPositionAngle().equals(destPositionAngle)) {
            out = this.copy();
        } else {
            final Covariance covariance = this.getCovariance(destFrame, destOrbitType,
                destPositionAngle);
            out = new MultiOrbitalCovariance(covariance, this.associatedOrbits,
                this.nbAdditionalParameters, destFrame, destOrbitType, destPositionAngle);
        }
        return out;
    }

    /**
     * Transforms this orbital covariance to a local orbital frame centered on a given orbit.
     * <p>
     * <b>Important:</b><br>
     * The returned covariance is defined in {@linkplain OrbitType#CARTESIAN Cartesian coordinates},
     * in the specified {@linkplain LOFType}. Note that the local orbital frame uses the reference
     * orbit as its {@linkplain PVCoordinatesProvider}, which relies on a simple Keplerian model for
     * the propagation. The LOF built is therefore only valid locally, at the date of the reference
     * orbit, unless it is frozen at this date (the LOF then becomes an inertial frame which can be
     * used at other dates).
     * </p>
     *
     * @param index
     *        the index of the selected orbit in the list of associated orbits, which will be used
     *        to build the local orbital frame
     * @param lofType
     *        the type of the local orbital frame
     * @param frozenLof
     *        whether or not the local orbital frame built should be frozen at the date of the
     *        reference orbit
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified local orbital frame
     */
    public MultiOrbitalCovariance transformTo(final int index, final LOFType lofType,
                                              final boolean frozenLof)
        throws PatriusException {
        final Orbit referenceOrbit = this.getOrbit(index);
        return transformTo(referenceOrbit, lofType, frozenLof);
    }

    /**
     * Transforms this orbital covariance to a local orbital frame centered on a given orbit.
     * <p>
     * <em>The reference orbit must be defined at the same date as the covariance.</em>
     * </p>
     * <p>
     * <b>Important:</b><br>
     * The returned covariance is defined in {@linkplain OrbitType#CARTESIAN Cartesian coordinates},
     * in the specified {@linkplain LOFType}. Note that the local orbital frame uses the reference
     * orbit as its {@linkplain PVCoordinatesProvider}, which relies on a simple Keplerian model for
     * the propagation. The LOF built is therefore only valid locally, at the date of the reference
     * orbit, unless it is frozen at this date (the LOF then becomes an inertial frame which can be
     * used at other dates).
     * </p>
     *
     * @param referenceOrbit
     *        the orbit used to build the local orbital frame
     * @param lofType
     *        the type of the local orbital frame
     * @param frozenLof
     *        whether or not the local orbital frame built should be frozen at the date of the
     *        reference orbit
     * @return the transformed orbital covariance
     * @throws PatriusException
     *         if the orbital covariance cannot be transformed to the specified local orbital frame
     */
    public MultiOrbitalCovariance transformTo(final Orbit referenceOrbit, final LOFType lofType,
                                              final boolean frozenLof)
        throws PatriusException {
        // Ensure the reference orbit is not null and defined at the same date as the covariance
        checkOrbit(referenceOrbit);

        // Initialize the reference pseudo inertial frame
        final Frame pseudoInertialFrame;
        if (this.getFrame().isPseudoInertial()) {
            pseudoInertialFrame = this.getFrame();
        } else if (referenceOrbit.getFrame().isPseudoInertial()) {
            pseudoInertialFrame = referenceOrbit.getFrame();
        } else {
            pseudoInertialFrame = FramesFactory.getGCRF();
        }

        // Check if the reference orbit frame is pseudo-inertial, otherwise convert the orbit in the
        // pseudo-inertial frame
        final Orbit orbit;
        if (referenceOrbit.getFrame().isPseudoInertial()) {
            orbit = referenceOrbit;
        } else {
            orbit = referenceOrbit.getType().convertOrbit(referenceOrbit, pseudoInertialFrame);
        }

        // Build the LOF frame and freeze it if requested
        Frame lofFrame = new LocalOrbitalFrame(pseudoInertialFrame, lofType, orbit, lofType.name());
        if (frozenLof) {
            lofFrame = lofFrame.getFrozenFrame(pseudoInertialFrame, orbit.getDate(), "Frozen_"
                    + lofType.name());
        }

        // Return the orbital covariance transformed in the expected frame
        return this.transformTo(lofFrame, OrbitType.CARTESIAN, this.getPositionAngle());
    }

    /**
     * Gets the covariance transformed to the specified frame, orbit type and position angle type.
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
     * @return the covariance expressed in the specified frame, orbit type and position angle type
     * @throws PatriusException
     *         if the covariance cannot be transformed to the specified frame, orbit type and
     *         position angle type
     */
    private Covariance getCovariance(final Frame destFrame, final OrbitType destOrbitType,
                                     final PositionAngle destPositionAngle)
        throws PatriusException {
        // Associated frame, orbit type, position angle type and covariance
        final Frame frame = this.getFrame();
        final OrbitType orbitType = this.getOrbitType();
        final PositionAngle positionAngle = this.getPositionAngle();
        final Covariance covariance = this.getCovariance();

        // Compute the Jacobians matrix
        final int n = covariance.getSize();
        final int nbOrbits = this.associatedOrbits.size();
        final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(n);

        // Loop on each orbit
        for (int i = 0; i < nbOrbits; i++) {
            final Orbit orbit = this.associatedOrbits.get(i);

            // Compute the jacobian of the current orbit
            final RealMatrix subJacobians = orbit.getJacobian(frame, destFrame,
                orbitType, destOrbitType, positionAngle, destPositionAngle);
            final int startIndex = this.startIndices[i];
            jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
        }

        // Update the parameter descriptors
        final List<ParameterDescriptor> updatedParameterDescriptors = convertParameterDescriptors(
            this.getParameterDescriptors(), destOrbitType, destPositionAngle);
        return covariance.quadraticMultiplication(jacobians, updatedParameterDescriptors);
    }

    /**
     * Gets a copy of this orbital covariance.
     * <p>
     * This method performs a shallow copy of the associated covariance (shallow copy of the
     * parameter descriptors list, deep copy of the covariance matrix) and a shallow copy of the
     * list of orbits (which are immutable). The frame, orbit type and position angle type are all
     * passed by reference since they are immutable.
     * </p>
     *
     * @return a copy of this orbital covariance
     * @see Covariance#copy()
     */
    public MultiOrbitalCovariance copy() {
        return new MultiOrbitalCovariance(this.getCovariance().copy(), this.associatedOrbits,
            this.nbAdditionalParameters, this.getFrame(), this.getOrbitType(),
            this.getPositionAngle());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if (object == this) {
            // Same instance
            isEqual = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final MultiOrbitalCovariance other = (MultiOrbitalCovariance) object;

            isEqual = super.equals(other);
            isEqual &= Objects.equals(this.associatedOrbits, other.associatedOrbits);
            isEqual &= Arrays.equals(this.nbAdditionalParameters, other.nbAdditionalParameters);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.associatedOrbits,
            Arrays.hashCode(this.nbAdditionalParameters));
    }
}
