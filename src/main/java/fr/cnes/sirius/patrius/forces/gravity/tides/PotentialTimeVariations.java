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
 * @history created 19/04/12
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for perturbating forces that moficate the C and S coefficients over the time.
 * 
 * @author Julie Anton
 * 
 * @version $Id: PotentialTimeVariations.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 */
public interface PotentialTimeVariations {

    /**
     * Update the C and the S coefficients for acceleration computation.
     * 
     * @param date
     *        : date
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    void updateCoefficientsCandS(final AbsoluteDate date) throws PatriusException;

    /**
     * Update the C and the S coefficients for partial derivatives computation.
     * 
     * @param date
     *        : date
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    void updateCoefficientsCandSPD(final AbsoluteDate date) throws PatriusException;
}
