/**
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
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:300:18/03/2015:Creation multi propagator (Replaced AdditionalStateData by AdditionalStateInfo)
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Common interface for all propagator mode handlers initialization.
 * 
 * @author Luc Maisonobe
 */
public interface ModeHandler {

    /**
     * Initialize the mode handler.
     * 
     * @param orbit
     *        orbit type
     * @param angle
     *        position angle type
     * @param attitudeProviderForces
     *        attitude provider for forces computation
     * @param attitudeProviderEvents
     *        attitude provider for events computation
     * @param additionalStateInfos
     *        additional states informations
     * @param activateHandlers
     *        if handlers shall be active
     * @param reference
     *        reference date
     * @param frame
     *        reference frame
     * @param mu
     *        central body attraction coefficient
     */
    void initialize(OrbitType orbit, PositionAngle angle,
                    AttitudeProvider attitudeProviderForces,
                    AttitudeProvider attitudeProviderEvents,
                    Map<String, AdditionalStateInfo> additionalStateInfos,
                    boolean activateHandlers, AbsoluteDate reference, Frame frame, double mu);

    /**
     * Define new reference date.
     * <p>
     * To be called by {@link NumericalPropagator} only.
     * </p>
     * 
     * @param newReference
     *        new reference date
     */
    void setReference(final AbsoluteDate newReference);
}
