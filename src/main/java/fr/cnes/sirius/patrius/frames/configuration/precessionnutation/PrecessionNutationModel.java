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
 *
 * @history creation 28/06/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This interface provides the Celestial Intermediate Pole motion (CIP) in the GCRS, those coordinates are used for
 * the GCRF to CIRF transformation.
 *
 * @author Julie Anton
 *
 * @version $Id: PrecessionNutationModel.java 18073 2017-10-02 16:48:07Z bignon $
 *
 */
public interface PrecessionNutationModel extends Serializable {

    /**
     * Getter for the CIP coordinates at the provided date.
     *
     * @param date
     *        Date for the CIP coordinates
     * @return the CIP coordinates
     */
    CIPCoordinates getCIPCoordinates(AbsoluteDate date);

    /**
     * Return computation type : direct or interpolated.
     *
     * @return true if direct computation, false if interpolated
     */
    boolean isDirect();

    /**
     * Get IERS model origin.
     *
     * @return IERS model origin
     */
    FrameConvention getOrigin();
}
