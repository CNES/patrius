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
 * @history created 15/02/2016
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for mean/osculating elements converter.
 * <p>
 * This interface provides methods to convert from mean elements to osculating elements and in return.
 * </p>
 * 
 * @author Emmanuel Bignon
 * @since 3.2
 * @version $Id: MeanOsculatingElementsProvider.java 18092 2017-10-02 17:12:58Z bignon $
 */

public interface MeanOsculatingElementsProvider {

    /**
     * Convert provided osculating orbit into mean elements.
     * <p>
     * <b>Warning: </b>Used algorithm often consists in an iterative algorithm with a convergence criterion. As a result
     * convergence is not always ensured, depending on the underlying theory.
     * </p>
     * 
     * @param orbit
     *        an orbit (osculating elements)
     * @return mean elements of provided orbit
     * @throws PatriusException
     *         if conversion fails
     */
    Orbit osc2mean(final Orbit orbit) throws PatriusException;

    /**
     * Convert provided mean orbit into osculating elements.
     * 
     * @param orbit
     *        an orbit (mean elements)
     * @return osculating elements of provided orbit
     * @throws PatriusException
     *         if conversion fails
     */
    Orbit mean2osc(final Orbit orbit) throws PatriusException;

    /**
     * Propagate mean orbit until provided date.
     * 
     * @param date
     *        a date
     * @return mean orbit at provided date
     * @throws PatriusException
     *         thrown if computation failed
     */
    Orbit propagateMeanOrbit(final AbsoluteDate date) throws PatriusException;
}
