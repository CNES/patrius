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
 * @history creation 17/05/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:92:17/05/2013:Creation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.util.Collection;

/**
 * Interface for geomagnetic data provider. <br>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: GeoMagneticDataProvider.java 18070 2017-10-02 16:46:00Z bignon $
 * 
 * @since 2.1
 * 
 */
public interface GeoMagneticDataProvider {

    /**
     * Returns a {@link Collection} of {@link GeoMagneticField} models.
     * 
     * @return a {@link Collection} of {@link GeoMagneticField} models
     */
    Collection<GeoMagneticField> getModels();
}
