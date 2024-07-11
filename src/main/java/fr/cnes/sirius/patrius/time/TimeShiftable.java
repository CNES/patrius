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
 * VERSION:4.3:DM:DM-2091:15/05/2019:[PATRIUS] optimisation du SpacecraftState
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents objects that can be shifted in time.
 * 
 * @param <T>
 *        Type of the object.
 * @author Luc Maisonobe
 */
public interface TimeShiftable<T extends TimeShiftable<T>> {

    /**
     * Get a time-shifted instance.
     * 
     * @param dt
     *        time shift in seconds
     * @return a new instance, shifted with respect to instance (which is not changed)
     * @throws PatriusException if attitude cannot be computed
     *         if attitude events cannot be computed
     */
    T shiftedBy(double dt) throws PatriusException;

}
