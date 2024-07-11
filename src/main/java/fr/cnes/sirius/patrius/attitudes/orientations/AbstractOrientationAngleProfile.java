/**
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * This abstract class aims at defining all common features to classes representing the angular
 * velocities profile of an {@link OrientationAngleLeg}.
 * 
 * @author delaygni
 *
 * @since 4.2
 */
public abstract class AbstractOrientationAngleProfile extends AbstractOrientationAngleLeg
    implements OrientationAngleProfile {

    /** Serial ID. */
    private static final long serialVersionUID = 7123669438390991739L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ATTITUDE_ORIENTATION_ANGLE_PROFILE";

    /**
     * Constructor
     * 
     * @param timeInterval time interval of the profile
     */
    public AbstractOrientationAngleProfile(final AbsoluteDateInterval timeInterval) {
        this(timeInterval, DEFAULT_NATURE);
    }

    /**
     * Constructor
     * 
     * @param timeInterval time interval of the profile
     * @param natureIn leg nature
     */
    public AbstractOrientationAngleProfile(final AbsoluteDateInterval timeInterval,
        final String natureIn) {
        super(timeInterval, natureIn);
    }
}
