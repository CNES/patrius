/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history creation 18/09/2012
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * R12 value provider for the Bent model.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public interface R12Provider {

    /**
     * Provides the R12 value for the Bent model.
     * 
     * @param date
     *        the date
     * @return r12 R12 value for the date
     * @throws PatriusException
     *         in case of data loading issues
     */
    double getR12(final AbsoluteDate date) throws PatriusException;

}