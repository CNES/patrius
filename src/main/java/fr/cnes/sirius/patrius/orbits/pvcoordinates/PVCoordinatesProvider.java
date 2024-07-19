/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 ** Interface for PV coordinates providers.
 * 
 * @author Veronique Pommier
 *         <p>
 *         The PV coordinates provider interface can be used by any class used for position/velocity computation, for
 *         example celestial bodies or spacecraft position/velocity propagators, and many others...
 *         </p>
 */
public interface PVCoordinatesProvider extends Serializable {

    /**
     * Get the {@link PVCoordinates} of the body in the selected frame.
     * 
     * @param date
     *        current date
     * @param frame
     *        the frame where to define the position
     * @return position/velocity of the body (m and m/s)
     * @exception PatriusException
     *            if position cannot be computed in given frame
     */
    PVCoordinates getPVCoordinates(final AbsoluteDate date,
            final Frame frame) throws PatriusException;

    /**
     * Get the native frame, i.e. the raw frame in which PVCoordinates are expressed before transformation to user
     * output frame.
     * This native frame can be dependent on date and frame.
     * 
     * @param date
     *        a date
     * @param frame
     *        a frame
     * @return the native frame
     * @exception PatriusException
     *            thrown if native frame is undefined or unknown
     */
    Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException;

}
