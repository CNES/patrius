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
 * @history created 29/06/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.ephemeris;

import fr.cnes.sirius.patrius.bodies.IAUPole;

/**
 * Interface representing a celestial body, with a name, an attraction coefficient
 * and a definition for the IAU pole.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: IEphemerisBody.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.2
 * 
 */
public interface IEphemerisBody {

    /**
     * @return the IAU pole
     */
    IAUPole getIAUPole();

    /**
     * @return the attraction coefficient
     */
    double getGM();

    /**
     * @return the celestial body name
     */
    String name();

}
