/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
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
 * @history 15/05/2012
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This is the interface for all emissivity models (albedo and infrared).
 * 
 * @author ClaudeD
 * 
 * @version $Id: IEmissivityModel.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public interface IEmissivityModel extends Serializable {

    /**
     * Get the albedo and infrared emissivities.
     * 
     * @param cdate
     *        current date
     * @param latitude
     *        (rad) geocentric latitude
     * @param longitude
     *        (rad) geocentric longitude
     * 
     * @return albedo emissivity ([0]) and infrared emissivity ([1])
     * 
     */
    double[] getEmissivity(final AbsoluteDate cdate, final double latitude, final double longitude);
}
