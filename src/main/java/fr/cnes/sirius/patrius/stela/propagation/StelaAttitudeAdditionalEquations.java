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
 * @history 30/10/2014
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:30/11/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:300:18/03/2015:Renamed AbstractAttitudeEquation into AttitudeEquation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;

/**
 * <p>
 * This abstract class allows users to add their own attitude differential equations to a Stela GTO propagator.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @see StelaAdditionalEquations
 * 
 * @author Charlotte Maggiorani
 * 
 * @version $Id$
 * 
 * @since 2.3
 * 
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class StelaAttitudeAdditionalEquations implements StelaAdditionalEquations {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serial UID. */
    private static final long serialVersionUID = -7827108207856933702L;

    /** Equation name. */
    private final String name;

    /** Equation type. */
    private final AttitudeType type;

    /**
     * Create a new attitude equation. The name of the created equation is set by default in the following form
     * "ATTITUDE_FORCES" or "ATTITUDE_EVENTS" or "ATTITUDE"
     * 
     * @param attitudeType
     *        the attitude type
     */
    public StelaAttitudeAdditionalEquations(final AttitudeType attitudeType) {
        this.name = attitudeType.toString();
        this.type = attitudeType;
    }

    /**
     * Get the name of the additional equation. The name is in the following form : "ATTITUDE_FORCES" or
     * "ATTITUDE_EVENTS" or "ATTITUDE".
     * 
     * @return name of the additional equation
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the attitude type.
     * 
     * @return the attitude type : ATTITUDE_FORCES or ATTITUDE_EVENTS or ATTITUDE
     */
    public AttitudeType getAttitudeType() {
        return this.type;
    }
}
