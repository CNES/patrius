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
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3248:22/05/2023:[PATRIUS] Renommage de GeodeticPoint en GeodeticCoordinates
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface is intended for ephemerides valid only during a time range.
 * 
 * <p>
 * This interface provides a mean to retrieve orbital parameters at any time within a given range. It should be
 * implemented by orbit readers based on external data files and by continuous models built after numerical integration
 * has been completed and dense output data as been gathered.
 * </p>
 * 
 * @author Luc Maisonobe
 * 
 */
public interface BoundedPropagator extends Propagator {

    /**
     * Get the first date of the range.
     * 
     * @return the first date of the range
     */
    AbsoluteDate getMinDate();

    /**
     * Get the last date of the range.
     * 
     * @return the last date of the range
     */
    AbsoluteDate getMaxDate();

    /**
     * Set propagation frame.
     * 
     * @param frameIn
     *        the frame to use. This frame must be inertial or pseudo-inertial, otherwise an exception is raised.
     * @throws PatriusException
     *         if frame is not inertial or pseudo-inertial
     */
    abstract void setOrbitFrame(Frame frameIn) throws PatriusException;

}
