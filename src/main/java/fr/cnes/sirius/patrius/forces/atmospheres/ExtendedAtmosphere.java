/**
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
 * @history created 05/08/2016
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.forces.atmospheres;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for extended atmosphere.
 * This interface provides more detailed atmospheric data such as partial densities.
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id: ExtendedAtmosphere.java 18079 2017-10-02 16:52:15Z bignon $
 */
public interface ExtendedAtmosphere extends Atmosphere {

    /**
     * Get detailed atmospheric data.
     * 
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return detailed atmospheric data
     * @exception PatriusException
     *            if some atmospheric data cannot be retrieved (because date is out of
     *            range of solar activity model or if some frame conversion cannot be performed)
     */
    AtmosphereData getData(final AbsoluteDate date, final Vector3D position,
                           final Frame frame) throws PatriusException;
}
