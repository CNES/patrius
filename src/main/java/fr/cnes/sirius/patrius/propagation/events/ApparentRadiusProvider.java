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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:611:02/08/2016:Creation of the class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import java.io.Serializable;

import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface to represent apparent radius providers.
 */
public interface ApparentRadiusProvider extends Serializable {

    /**
     * Compute the apparent radius (in meters) of the occulting body from the spacecraft (observer) position. Given a
     * plane containing the spacecraft (observer) position, the center of the occulting body and the center of the
     * occulted body, and given a line contained within this plane, passing by the spacecraft (observer) position and
     * tangent to the mesh of the occulting body, the apparent radius corresponds to the length of the line starting
     * from the center of the occulting body, perpendicular to the first given line and ending at the intersection of
     * the two lines.
     * <p>
     * Please notice that this method will for the moment be used only with an instantaneous propagation delay type.
     * <p>
     * 
     * @param pvObserver the spacecraft (observer) position-velocity
     * @param date the date at which the signal is received by the observer (reception date)
     * @param occultedBody the body which is occulted to the spacecraft (observer) by the occulting body
     * @param propagationDelayType propagation delay type
     * @return the apparent radius (in meters) of the occulting body from the spacecraft (observer) position
     * @throws PatriusException if the {@link PVCoordinatesProvider} computation fails
     */
    double getApparentRadius(final PVCoordinatesProvider pvObserver,
            final AbsoluteDate date,
            final PVCoordinatesProvider occultedBody,
            final PropagationDelayType propagationDelayType);

}
