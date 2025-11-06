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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.SpacecraftStateProvider;
import fr.cnes.sirius.patrius.propagation.numerical.JacobiansMapper;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements OrbitalCovarianceProvider by transforming an initial covariance with the partial derivatives of
 * a spacecraft state provider.
 */
public class BasicOrbitalCovarianceProvider implements OrbitalCovarianceProvider {

    /** Serial UID */
    private static final long serialVersionUID = 7098854149392819590L;

    /** Index of the orbit rows */
    private static final int[] ORBIT_ROWS = IntStream.range(0, SpacecraftState.ORBIT_DIMENSION).toArray();

    /** The initial covariance */
    private final Covariance initialCov;

    /** The spacecraft state provider */
    private final SpacecraftStateProvider scProvider;

    /** The mapper used to extract partial derivatives from the spacecraft state */
    private final JacobiansMapper mapper;

    /** Index of the covariance parameters in the mapper */
    private final int[] mapperIndex;

    /**
     * Constructor
     *
     * @param initialCovariance
     *        The initial covariance to propagate
     * @param scProvider
     *        The spacecraft state provider used to propagate the initial covariance. The partial derivatives of the
     *        provider must be consistent with the initial covariance, i.e. must have the same date, frame, orbit type
     *        and position angle.
     * @param mapper
     *        The mapper used to extract partial derivatives from the spacecraft state
     */
    public BasicOrbitalCovarianceProvider(final Covariance initialCovariance, final SpacecraftStateProvider scProvider,
                                          final JacobiansMapper mapper) {

        this.initialCov = initialCovariance;
        this.scProvider = scProvider;
        this.mapper = mapper;
        this.mapperIndex = extractMapperIndex(initialCovariance, mapper);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return this.scProvider.getNativeFrame(date);
    }

    /** {@inheritDoc} */
    @Override
    public OrbitalCovariance getOrbitalCovariance(final AbsoluteDate date) throws PatriusException {

        final SpacecraftState state = this.scProvider.getSpacecraftState(date);

        final int covSize = this.initialCov.getSize();
        final Array2DRowRealMatrix stm = new Array2DRowRealMatrix(covSize, covSize);
        stm.setSubMatrix(this.mapper.getStateJacobian(state), 0, 0);

        if (this.mapperIndex.length != 0) {
            final RealMatrix paramJacobian = new Array2DRowRealMatrix(this.mapper.getParametersJacobian(state), false);
            paramJacobian.copySubMatrix(ORBIT_ROWS, this.mapperIndex, stm.getDataRef(), 0,
                SpacecraftState.ORBIT_DIMENSION);
            for (int i = SpacecraftState.ORBIT_DIMENSION; i < SpacecraftState.ORBIT_DIMENSION
                    + this.mapperIndex.length; i++) {
                stm.setEntry(i, i, 1);
            }
        }

        final Covariance propagCovariance =
            this.initialCov.quadraticMultiplication(stm, this.initialCov.getParameterDescriptors());
        return new OrbitalCovariance(propagCovariance, state.getOrbit());
    }

    /**
     * Checks the consistency of the covariance and extracts the parameter index in the mapper.
     *
     * @param covariance
     *        The covariance
     * @param mapper
     *        The mapper
     * @return the index of the covariance parameters in the mapper. Can be length 0 if no other parameters except
     *         orbital parameters.
     */
    private static int[] extractMapperIndex(final Covariance covariance, final JacobiansMapper mapper) {

        final List<ParameterDescriptor> descriptors = covariance.getParameterDescriptors();
        final List<ParameterDescriptor> mapperParamDescriptors =
            mapper.getParametersList().stream().map(Parameter::getDescriptor).collect(Collectors.toList());
        final List<Integer> descriptorIndexList = new ArrayList<>();
        int i = 0;
        for (final ParameterDescriptor descriptor : descriptors) {
            if (i < SpacecraftState.ORBIT_DIMENSION) {
                // Should be orbital coordinates
                final OrbitalCoordinate orbCoordinates =
                    descriptor.getFieldValue(StandardFieldDescriptors.ORBITAL_COORDINATE);
                if (orbCoordinates == null) {
                    throw new IllegalArgumentException(
                        String.format("The %dth descriptor (%s) of the covariance is not an orbital parameter", i,
                            descriptor.toString()));
                }
                if (orbCoordinates.getStateVectorIndex() != i) {
                    throw new IllegalArgumentException(
                        "The orbital parameters of the covariance are not in the correct order");
                }
            } else {
                final int descriptorIndex = mapperParamDescriptors.indexOf(descriptor);
                if (descriptorIndex < 0) {
                    throw new IllegalArgumentException(
                        String.format("The descriptor %s appears in the covariance but not in the mapper",
                            descriptor.toString()));
                }
                descriptorIndexList.add(descriptorIndex);
            }
            i++;
        }

        int[] mapperIndex = new int[0];
        if (!descriptorIndexList.isEmpty()) {
            mapperIndex = descriptorIndexList.stream().mapToInt(Integer::intValue).toArray();
        }

        return mapperIndex;
    }

}