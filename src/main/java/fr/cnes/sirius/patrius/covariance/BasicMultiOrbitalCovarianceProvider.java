/**
 *
 * Copyright 2011-2022 CNES
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
 * @history Created 20/02/2025
 *
 * HISTORY
 * VERSION:4.16:OPENFD-379:25/04/2025:[PATRIUS] Ajout d'une implementation basique de OrbitalCovarianceProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.SpacecraftStateProvider;
import fr.cnes.sirius.patrius.propagation.numerical.JacobiansMapper;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements {@link MultiOrbitalCovarianceProvider} by transforming an initial covariance with the partial
 * derivatives of a spacecraft state providers.
 */
public class BasicMultiOrbitalCovarianceProvider implements MultiOrbitalCovarianceProvider {

    /** Serial UID */
    private static final long serialVersionUID = 7098854149392819590L;

    /** Index of the orbit rows */
    private static final int[] ORBIT_ROWS = IntStream.range(0, SpacecraftState.ORBIT_DIMENSION).toArray();

    /** The initial covariance */
    private final Covariance initialCov;

    /** The list of spacecraft state provider associated to their mapper */
    private final List<Pair<SpacecraftStateProvider, JacobiansMapper>> scProviderAndMapper;

    /** Index of the covariance parameters in the mapper */
    private final int[][] mapperIndex;

    /** The sizes of additional parameters for each spacecraft */
    private final int[] additionalParamsSize;

    /** The common positionAngle */
    private final PositionAngle positionAngle;

    /** The common orbit type */
    private final OrbitType orbitType;

    /** The common frame */
    private final Frame frame;

    /**
     * Constructor
     *
     * @param initialCovariance
     *        The initial covariance to propagate
     * @param scProviderMap
     *        The map of spacecraft state providers and their mapper. The spacecraft state providers are used to
     *        propagate the initial covariance. The mappers are used to extract partial derivatives from the spacecraft
     *        states<br>
     *        <b> Conditions</b>:
     *        <ul>
     *        <li>Entries of this map <b>must</b> be ordered in the same order of the provided covariance</li>
     *        <li>The partial derivatives of the providers <b>must</b> be consistent with the initial covariance, i.e.
     *        must have the same date, frame, orbit type and position angle.</li>
     */
    public BasicMultiOrbitalCovarianceProvider(final Covariance initialCovariance,
                                               final Map<SpacecraftStateProvider, JacobiansMapper> scProviderMap) {

        final Collection<JacobiansMapper> mappers = scProviderMap.values();

        this.initialCov = initialCovariance;
        this.mapperIndex = extractMapperIndex(initialCovariance, mappers);
        this.additionalParamsSize = new int[this.mapperIndex.length];
        for (int i = 0; i < this.mapperIndex.length; i++) {
            this.additionalParamsSize[i] = this.mapperIndex[i].length;
        }

        // Initialize scProviderAndMapper
        this.scProviderAndMapper = new ArrayList<>(scProviderMap.size());
        for (final Entry<SpacecraftStateProvider, JacobiansMapper> entry : scProviderMap.entrySet()) {
            this.scProviderAndMapper.add(new Pair<>(entry.getKey(), entry.getValue()));
        }

        this.frame = checkAndGetUnique(mappers.stream().map(JacobiansMapper::getPropagationFrame), "frames");
        this.orbitType = checkAndGetUnique(mappers.stream().map(JacobiansMapper::getOrbitType), "orbit types");
        this.positionAngle = checkAndGetUnique(mappers.stream().map(JacobiansMapper::getAngleType), "position angles");
    }

    /** {@inheritDoc} */
    @Override
    public MultiOrbitalCovariance getMultiOrbitalCovariance(final AbsoluteDate date) throws PatriusException {
        // Create the global state transition matrix
        final int covSize = this.initialCov.getSize();
        final Array2DRowRealMatrix stm = new Array2DRowRealMatrix(covSize, covSize);

        int scIndex = 0;
        int stmIndex = 0;

        // Precise the size of the list
        final List<Orbit> orbits = new ArrayList<>(this.scProviderAndMapper.size());
        for (final Pair<SpacecraftStateProvider, JacobiansMapper> entry : this.scProviderAndMapper) {
            final SpacecraftState state = entry.getKey().getSpacecraftState(date);
            orbits.add(state.getOrbit());

            // Copy state jacobian in the STM
            stm.setSubMatrix(entry.getValue().getStateJacobian(state), stmIndex, stmIndex);

            // Handle parameters jacobian if present
            final int[] scMapperIndex = this.mapperIndex[scIndex];
            if (scMapperIndex.length > 0) {
                final int stmIndexPlusOrbitDim = stmIndex + SpacecraftState.ORBIT_DIMENSION;
                final RealMatrix paramJacobian =
                    new Array2DRowRealMatrix(entry.getValue().getParametersJacobian(state), false);
                paramJacobian.copySubMatrix(ORBIT_ROWS, scMapperIndex, stm.getDataRef(), stmIndex,
                    stmIndexPlusOrbitDim);
                for (int i = stmIndexPlusOrbitDim; i < stmIndexPlusOrbitDim + scMapperIndex.length; i++) {
                    stm.setEntry(i, i, 1);
                }
                stmIndex = stmIndexPlusOrbitDim + scMapperIndex.length;
            } else {
                stmIndex += SpacecraftState.ORBIT_DIMENSION;
            }
            scIndex++;
        }

        final Covariance propagCovariance =
            this.initialCov.quadraticMultiplication(stm, this.initialCov.getParameterDescriptors());
        return new MultiOrbitalCovariance(propagCovariance, orbits, this.additionalParamsSize, this.frame,
            this.orbitType, this.positionAngle);
    }

    /** {@inheritDoc} */
    @Override
    public OrbitalCovarianceProvider getOrbitalCovarianceProvider(final int index) {

        int startIndex = 0;
        for (int i = 0; i < index; i++) {
            startIndex += SpacecraftState.ORBIT_DIMENSION + this.additionalParamsSize[i];
        }

        final int endIndex = startIndex + SpacecraftState.ORBIT_DIMENSION + this.additionalParamsSize[index];
        final Covariance extractedCov =
            this.initialCov.getSubCovariance(IntStream.range(startIndex, endIndex).toArray());

        final Pair<SpacecraftStateProvider, JacobiansMapper> entry = this.scProviderAndMapper.get(index);
        return new BasicOrbitalCovarianceProvider(extractedCov, entry.getKey(), entry.getValue());
    }

    /**
     * Checks the consistency of the covariance and extracts the parameter index in the mapper.
     *
     * @param covariance
     *        The covariance
     * @param mappers
     *        The mappers
     * @return For each spacecraft, the index of the covariance parameters in the mapper. Second dimension array can be
     *         length 0 if no other parameters except orbital parameters.
     */
    private static int[][] extractMapperIndex(final Covariance covariance, final Collection<JacobiansMapper> mappers) {
        final List<ParameterDescriptor> descriptors = covariance.getParameterDescriptors();

        // Precise size of List
        final List<List<ParameterDescriptor>> mapperParamDescriptors = new ArrayList<>(mappers.size());
        for (final JacobiansMapper mapper : mappers) {
            final List<ParameterDescriptor> mapperDescriptors =
                mapper.getParametersList().stream().map(Parameter::getDescriptor).collect(Collectors.toList());
            mapperParamDescriptors.add(mapperDescriptors);
        }

        final List<List<Integer>> descriptorIndexList = new ArrayList<>();
        descriptorIndexList.add(new ArrayList<>());
        int scIndex = 0;
        int i = 0;
        for (final ParameterDescriptor descriptor : descriptors) {
            if (i < SpacecraftState.ORBIT_DIMENSION) {
                checkOrbCoordinates(descriptor, i, scIndex);
            } else {
                final int descriptorIndex = mapperParamDescriptors.get(scIndex).indexOf(descriptor);
                if (descriptorIndex < 0) {
                    // The descriptor is not supported by this mapper
                    // Switch to the next spacecraft
                    descriptorIndexList.add(new ArrayList<>());
                    scIndex++;
                    i = 0;
                    checkOrbCoordinates(descriptor, i, scIndex);
                } else {
                    descriptorIndexList.get(scIndex).add(descriptorIndex);
                }
            }
            i++;
        }

        return descriptorIndexList.stream()
            .map(l -> l.stream().mapToInt(Integer::intValue).toArray())
            .toArray(int[][]::new);
    }

    /**
     * Check the provided descriptor represents the ith orbital parameter of the covariance
     *
     * @param descriptor
     *        The descriptor to check
     * @param paramPositionInSpacecraftParams
     *        The position it should represent according to its position in the covariance
     * @param spacecraftIndex
     *        The spacecraft index for exception management
     */
    private static void checkOrbCoordinates(final ParameterDescriptor descriptor,
                                            final int paramPositionInSpacecraftParams,
                                            final int spacecraftIndex) {
        // Should be orbital coordinates
        final OrbitalCoordinate orbCoordinates = descriptor.getFieldValue(StandardFieldDescriptors.ORBITAL_COORDINATE);
        if (orbCoordinates == null) {
            throw new IllegalArgumentException(
                String.format(
                    "The descriptor (%d) of spacecraft (%d) of the covariance (%s) is not an orbital parameter",
                    paramPositionInSpacecraftParams, spacecraftIndex, descriptor.toString()));
        }
        if (orbCoordinates.getStateVectorIndex() != paramPositionInSpacecraftParams) {
            throw new IllegalArgumentException(String
                .format("The orbital parameters of the spacecraft (%d) of the covariance are not in the correct order",
                    spacecraftIndex));
        }
    }

    /**
     * Check the uniqueness of the provided elements and return the unique element. Throw an exception otherwise.
     *
     * @param <T>
     *        The type of the element
     * @param values
     *        The values to check
     * @param nameForException
     *        The name of the elements for the exception management
     * @return the unique element
     */
    private static <T> T checkAndGetUnique(final Stream<T> values, final String nameForException) {
        final Set<T> valuesSet = values.collect(Collectors.toSet());
        if (valuesSet.size() > 1) {
            throw new IllegalStateException("The mappers " + nameForException + " are not all identical.");
        }
        return valuesSet.iterator().next();
    }

}