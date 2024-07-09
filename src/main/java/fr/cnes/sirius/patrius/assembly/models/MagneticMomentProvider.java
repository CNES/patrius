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
 * @history creation 23/07/2012
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:04/09/2013:Electromagntic sensitive spacecraft
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Interface for electromagnetic sensitive spacecraft
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public interface MagneticMomentProvider {

    /**
     * Get the magnetic moment at given date, in the main frame of the spacecraft
     * 
     * @param date
     *        date for computation of magnetic moment
     * @return the computed magnetic moment
     */
    Vector3D getMagneticMoment(final AbsoluteDate date);

}
