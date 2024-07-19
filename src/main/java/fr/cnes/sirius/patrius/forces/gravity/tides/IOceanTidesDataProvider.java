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
 * @history creation 08/12/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::DM:241:08/12/2014:improved tides conception
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsProvider;

/**
 * <p>
 * Interface that provides ocean tides inputs.
 * </p>
 * 
 * @author Charlotte Maggiorani
 * 
 * @version $Id: IOceanTidesDataProvider.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 2.3.1
 * 
 */
public interface IOceanTidesDataProvider extends OceanTidesCoefficientsProvider {

    /**
     * Get love numbers.
     * 
     * @return the love numbers.
     * 
     * @since 2.3.1
     */
    double[] getLoveNumbers();

    /**
     * Get the ocean tides standard
     * 
     * @return the ocean tides standard
     * 
     * @since 2.3.1
     */
    TidesStandard getStandard();
}
