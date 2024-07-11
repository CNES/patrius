/**
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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2415:27/05/2020:Gestion des PartialderivativesEquations avec MultiPropagateur 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::FA:905:04/09/2017: use of LinkedHashMap instead of HashMap
 * VERSION::DM:1872:10/10/2016:add Multi-attitude provider
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalStateInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Utility class that describes in a minimal fashion the structure of a state.
 * An instance contains the size of an additional state and its index in the state vector.
 * The instance <code>AdditionalStateInfo</code> is guaranteed to be immutable.
 * 
 * @concurrency immutable
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public final class MultiStateVectorInfo implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -7988745506943818839L;

    /** State vector size. */
    private final int vectorSize;

    /** Informations about the state vector. */
    private final Map<String, OneSatVectorInfo> satInfos;

    /** Satellites ID lists (redundant with satInfos keyset in order to speed-up process). */
    private final List<String> satIDList;

    /** Map of central body gravitational constant for each spacecraft. */
    private final Map<String, Double> muMap;

    /** Map of frame for each spacecraft. */
    private final Map<String, Frame> propagationFrameMap;

    /**
     * Build a MultiSatStateVectorInfo instance using the spacecraft states' map.
     * 
     * @param s the spacecraft states' map.
     * @param mus the mu map
     * @param frames the frame map
     */
    public MultiStateVectorInfo(final Map<String, SpacecraftState> s,
        final Map<String, Double> mus, final Map<String, Frame> frames) {
        this.satInfos = new HashMap<String, OneSatVectorInfo>();
        this.satIDList = new ArrayList<String>();
        this.muMap = new HashMap<String, Double>();
        this.propagationFrameMap = new HashMap<String, Frame>();
        int globalSize = 0;
        int satNb = 0;
        for (final Map.Entry<String, SpacecraftState> entry : s.entrySet()) {
            final int oneSatSize = entry.getValue().getStateVectorSize();
            final OneSatVectorInfo oneSatInfo = new OneSatVectorInfo(globalSize, satNb, oneSatSize,
                entry.getValue().getAdditionalStatesInfos());
            this.satInfos.put(entry.getKey(), oneSatInfo);
            this.satIDList.add(entry.getKey());
            final double mu = s.get(entry.getKey()).getMu();
            this.muMap.put(entry.getKey(), mu);
            final Frame frame = frames.get(entry.getKey());
            this.propagationFrameMap.put(entry.getKey(), frame);
            globalSize += oneSatSize;
            satNb++;
        }
        this.vectorSize = globalSize;
    }

    /**
     * Get global state vector size.
     * 
     * @return the global state vector size.
     */
    public int getStateVectorSize() {
        return this.vectorSize;
    }

    /**
     * Get the list of spacecraft ID.
     * 
     * @return the list of spacecraft ID.
     */
    public List<String> getIdList() {
        return this.satIDList;
    }

    /**
     * Get the state vector index of the given spacecraft in the global state vector.
     * 
     * @param satId
     *        the spacecraft ID
     * @return the state vector index
     */
    public int getSatRank(final String satId) {
        return this.satInfos.get(satId).getSatRank();
    }

    /**
     * Get the state vector size of the given spacecraft .
     * 
     * @param satId
     *        the spacecraft ID
     * @return the state vector size
     */
    private int getSatSize(final String satId) {
        return this.satInfos.get(satId).getSatStateVectSize();
    }

    /**
     * Get the additional states size of the given spacecraft .
     * 
     * @param satId
     *        the spacecraft ID
     * @return the additional states size
     */
    public int getSatAddStatesSize(final String satId) {
        return this.satInfos.get(satId).getSatAddStatesSize();
    }

    /**
     * Get the additional states informations associated with the given spacecraft ID.
     * 
     * @param satId
     *        the spacecraft ID
     * @return the state vector index
     */
    public Map<String, AdditionalStateInfo> getAddStatesInfos(final String satId) {
        return this.satInfos.get(satId).getAddStatesInfos();
    }

    /**
     * Convert state vector into a Map of SpacecraftState
     * 
     * @param y
     *        the state vector
     * @param currentDate
     *        the current date
     * @param orbitType
     *        the {@link Orbit orbital} parameters types
     * @param angleType
     *        the position angles type
     * @param attProvidersForces
     *        the map of attitude providers for forces computation
     * @param attProvidersEvents
     *        the map of attitude providers for events computation
     * @param mu
     *        the map of central attraction coefficient used for propagation
     *        (m<sup>3</sup>/s<sup>2</sup>) for each
     *        SpacecraftState
     * @param integrationFrame
     *        the map for frame in which integration is performed for each SpacecraftState
     * @return the map of SpacecraftState
     * @throws PatriusException
     *         if attitude cannot be computed
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Map<String, SpacecraftState> mapArrayToStates(final double[] y, final AbsoluteDate currentDate,
            final OrbitType orbitType, final PositionAngle angleType,
            final Map<String, MultiAttitudeProvider> attProvidersForces,
            final Map<String, MultiAttitudeProvider> attProvidersEvents, final Map<String, Double> mu,
            final Map<String, Frame> integrationFrame) throws PatriusException {
        // init map
        final Map<String, SpacecraftState> states = new LinkedHashMap<String, SpacecraftState>();
        final Map<String, Orbit> orbits = new LinkedHashMap<String, Orbit>();

        // Loop on all spacecraft ID
        final List<String> list = this.getIdList();
        final int sizeList = list.size();

        // Build orbits
        for (int i = 0; i < sizeList; i++) {
            final String satId = list.get(i);
            final int satRank = this.getSatRank(satId);
            final double[] orbitY = new double[SpacecraftState.ORBIT_DIMENSION];
            System.arraycopy(y, satRank, orbitY, 0, SpacecraftState.ORBIT_DIMENSION);
            // Build SpacecraftState instance from state vector
            final Frame frame = integrationFrame.get(satId);
            final double muI = mu.get(satId);
            final Orbit orbit = orbitType.mapArrayToOrbit(orbitY, angleType, currentDate, muI,
                frame);
            orbits.put(satId, orbit);
        }

        for (int i = 0; i < sizeList; i++) {
            final String satId = list.get(i);
            final OneSatVectorInfo sat = this.satInfos.get(satId);
            final int satRank = sat.getSatRank();
            final int satSize = sat.getSatStateVectSize();
            final double[] localY = new double[satSize];
            // Copy part of state vector representing the current state
            System.arraycopy(y, satRank, localY, 0, satSize);
            // Build SpacecraftState instance from state vector
            Attitude attForces = null;
            Attitude attEvents = null;
            if (attProvidersForces != null && attProvidersForces.get(satId) != null
                && (attProvidersForces.containsKey(satId))) {
                final MultiAttitudeProvider attProvForces = attProvidersForces.get(satId);
                attForces = attProvForces.getAttitude(orbits);
            }
            if (attProvidersEvents != null && attProvidersEvents.get(satId) != null
                && (attProvidersEvents.containsKey(satId))) {
                final MultiAttitudeProvider attProvEvents = attProvidersEvents.get(satId);
                attEvents = attProvEvents.getAttitude(orbits);
            }
            final SpacecraftState state = new SpacecraftState(localY, orbitType, angleType,
                currentDate, mu.get(satId), integrationFrame.get(satId),
                sat.getAddStatesInfos(), attForces, attEvents);
            // Add state to map
            states.put(satId, state);
        }

        // Returns built map
        return states;
    }

    /**
     * Extract a given SpacecraftState from the state vector.
     * 
     * @param y
     *        the state vector
     * @param currentDate
     *        the current date
     * @param orbitType
     *        the {@link Orbit orbital} parameters types
     * @param angleType
     *        the position angles type
     * @param attProviderForces
     *        the attitude provider for forces computation of the given SpacecraftState
     * @param attProviderEvents
     *        the attitude provider for events computation of the given SpacecraftState
     * @param id
     *        the spacecraft id
     * @return the SpacecraftState
     * @throws PatriusException
     *         if attitude cannot be computed
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public SpacecraftState mapArrayToState(final double[] y, final AbsoluteDate currentDate,
                                           final OrbitType orbitType, final PositionAngle angleType,
                                           final MultiAttitudeProvider attProviderForces,
                                           final MultiAttitudeProvider attProviderEvents,
                                           final String id) throws PatriusException {

        // Initialization
        final Map<String, Orbit> orbits = new LinkedHashMap<String, Orbit>();
        // Loop on all spacecraft ID
        final List<String> list = this.getIdList();
        final int sizeList = list.size();

        for (int i = 0; i < sizeList; i++) {
            final String satId = list.get(i);
            final int satRank = this.getSatRank(satId);
            final int satSize = this.getSatSize(satId);
            // final double[] orbitY = new double[SpacecraftState.ORBIT_DIMENSION];
            final double[] orbitY = new double[satSize];
            System.arraycopy(y, satRank, orbitY, 0, satSize);
            // Build SpacecraftState instance from state vector
            final Frame frame = this.propagationFrameMap.get(satId);
            final double muI = this.muMap.get(satId);
            final Orbit orbit = orbitType.mapArrayToOrbit(orbitY, angleType, currentDate, muI,
                frame);
            orbits.put(satId, orbit);
        }

        // array values selection
        final OneSatVectorInfo sat = this.satInfos.get(id);
        final int spacecraftRank = this.getSatRank(id);
        final int spacecraftSize = this.getSatSize(id);
        final double[] localY = new double[spacecraftSize];
        System.arraycopy(y, spacecraftRank, localY, 0, spacecraftSize);

        // SpacecraftState building
        Attitude attForces = null;
        Attitude attEvents = null;
        if (attProviderForces != null) {
            attForces = attProviderForces.getAttitude(orbits);
        }
        if (attProviderEvents != null) {
            attEvents = attProviderEvents.getAttitude(orbits);
        }

        // Build state
        return new SpacecraftState(localY, orbitType, angleType,
            currentDate, this.muMap.get(id), this.propagationFrameMap.get(id), sat.getAddStatesInfos(),
            attForces, attEvents);
    }

    /**
     * Convert a map of SpacecraftState into a state vector.
     * 
     * @param s
     *        the map of SpacecraftState
     * @param orbitType
     *        the {@link Orbit orbital} parameters types
     * @param angleType
     *        the position angles type
     * @param y
     *        state vector
     */
    public void mapStatesToArray(final Map<String, SpacecraftState> s, final OrbitType orbitType,
                                 final PositionAngle angleType, final double[] y) {
        // Get list of spacecraft ID
        final List<String> list = this.getIdList();
        final int sizeList = list.size();
        // Loop on all spacecraft ID
        for (int i = 0; i < sizeList; i++) {
            final String satId = list.get(i);
            final int satRank = this.getSatRank(satId);
            final int satSize = this.getSatSize(satId);
            final double[] stateVector = new double[satSize];
            // Convert map to array
            s.get(satId).mapStateToArray(orbitType, angleType, stateVector);
            System.arraycopy(stateVector, 0, y, satRank, satSize);
        }
    }

    /**
     * <p>
     * Utility class that describes in a minimal fashion the structure of a state. An instance contains informations
     * about the state in the state vector : its size, its index and the structure of the additional states.
     * </p>
     * <p>
     * The instance <code>OneSatVectorInfo</code> is guaranteed to be immutable.
     * </p>
     * 
     * @concurrency immutable
     * 
     * @author maggioranic
     * 
     * @version $Id$
     * 
     */
    private static final class OneSatVectorInfo implements Serializable {

        /** Serial UID. */
        private static final long serialVersionUID = 2930848178518048692L;

        /**
         * Index of the state vector representing the current spacecraft in the global state vector.
         */
        private final int satRank;

        /**
         * Size of the state vector representing the current spacecraft.
         */
        private final int satStateVectSize;

        /**
         * Additional states informations of the current spacecraft.
         */
        private final Map<String, AdditionalStateInfo> addStates;

        /**
         * Simple constructor.
         * 
         * @param satIndex
         *        index of the state vector representing the current spacecraft in the global state
         *        vector.
         * @param satNumber
         *        Spacecraft number in the global state vector.
         * @param satStateVectorSize
         *        size of the state vector representing the current spacecraft.
         * @param additionalStates
         *        additional states informations.
         */
        private OneSatVectorInfo(final int satIndex, final int satNumber,
            final int satStateVectorSize,
            final Map<String, AdditionalStateInfo> additionalStates) {
            this.satRank = satIndex;
            this.satStateVectSize = satStateVectorSize;
            this.addStates = additionalStates;
        }

        /**
         * Get the index of the state vector representing the current spacecraft in the global state
         * vector.
         * 
         * @return the index.
         */
        private int getSatRank() {
            return this.satRank;
        }

        /**
         * Get the size of the state vector representing the current spacecraft.
         * 
         * @return the size of the state vector.
         */
        private int getSatStateVectSize() {
            return this.satStateVectSize;
        }

        /**
         * Get the size of the additional states associated with the current spacecraft.
         * 
         * @return the size of the additional states associated with the current spacecraft.
         */
        private int getSatAddStatesSize() {
            return this.satStateVectSize - SpacecraftState.ORBIT_DIMENSION;
        }

        /**
         * Get the additional states informations
         * 
         * @return the additional states informations.
         */
        private Map<String, AdditionalStateInfo> getAddStatesInfos() {
            return this.addStates;
        }
    }
}
