/**
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
 * 
 * @history created 18/03/2015
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.propagation.numerical.ModeHandler} and adapted to multi
 * propagation.
 * </p>
 * <p>
 * Common interface for all propagator mode handlers initialization.
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public interface MultiModeHandler {

    /**
     * Initialize the mode handler.
     * 
     * @param orbit
     *        orbit type
     * @param angle
     *        position angle type
     * @param attitudeProvidersForces
     *        attitude providers for forces computation for each state
     * @param attitudeProvidersEvents
     *        attitude providers for events computation for each state
     * @param stateVectorInfo
     *        the state vector informations
     * @param activateHandlers
     *        if handlers shall be active
     * @param reference
     *        reference date
     * @param frame
     *        the map of reference frame for each state
     * @param mu
     *        the map of central body attraction coefficient for each state
     */
    void initialize(OrbitType orbit, PositionAngle angle,
                    Map<String, MultiAttitudeProvider> attitudeProvidersForces,
                    Map<String, MultiAttitudeProvider> attitudeProvidersEvents,
                    final MultiStateVectorInfo stateVectorInfo,
                    boolean activateHandlers, AbsoluteDate reference, final Map<String, Frame> frame,
                    final Map<String, Double> mu);

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
