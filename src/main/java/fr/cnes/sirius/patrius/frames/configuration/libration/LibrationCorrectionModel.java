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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.libration;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface provides the pole corrections as well as the ut1-utc corrections due to libration.
 * 
 * @author Julie Anton
 * 
 * @version $Id: LibrationCorrectionModel.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public interface LibrationCorrectionModel extends Serializable {

    /**
     * Compute the pole corrections at a given date.
     * 
     * @param t
     *        date
     * @throws PatriusException
     *         when an Orekit error occurs
     * @return pole correction
     */
    PoleCorrection getPoleCorrection(final AbsoluteDate t) throws PatriusException;

    /**
     * Compute the UT1-TAI corrections at a given date.
     * 
     * @param t
     *        date
     * @return ut1-tai corrections
     */
    double getUT1Correction(final AbsoluteDate t);

    /**
     * Get IERS model origin.
     * 
     * @return IERS model origin
     */
    FrameConvention getOrigin();
}
