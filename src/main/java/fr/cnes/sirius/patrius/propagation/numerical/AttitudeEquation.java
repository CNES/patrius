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
 * @history creation 27/08/2014
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:300:18/03/2015:Renamed AbstractAttitudeEquation into AttitudeEquation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

/**
 * <p>
 * This interface allows users to add their own attitude differential equations to a numerical propagator.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @see AdditionalEquations
 * 
 * @author Charlotte Maggiorani
 * 
 * @version $Id: AttitudeEquation.java 18084 2017-10-02 16:55:24Z bignon $
 * 
 * @since 2.3
 * 
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class AttitudeEquation implements AdditionalEquations {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 4597478286897057093L;

    /**
     * Attitude type.
     */
    public enum AttitudeType {
        /** Attitude for forces computation. */
        ATTITUDE_FORCES("ATTITUDE_FORCES"),

        /** Attitude for events computation. */
        ATTITUDE_EVENTS("ATTITUDE_EVENTS"),

        /** Default attitude. */
        ATTITUDE("ATTITUDE");

        /** Name of attitude type. */
        private String name = "";

        /**
         * Private constructor of attitude type.
         * 
         * @param nameIn
         *        the attitude type
         */
        private AttitudeType(final String nameIn) {
            this.name = nameIn;
        }

        /** @return the name of the attitude type. */
        @Override
        public String toString() {
            return this.name;
        }
    }

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
    public AttitudeEquation(final AttitudeType attitudeType) {
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
