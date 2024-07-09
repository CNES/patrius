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
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1872:10/10/2016:add Multi-attitude provider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.multi;

import java.io.Serializable;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This interface represents an attitude provider for multi-satellites models.
 * <p>
 * An attitude provider provides a way to compute an {@link Attitude Attitude} from an date and several
 * position-velocity provider.
 * </p>
 * <p>
 * It is particularly useful if attitude of one satellite depends on PV of other satellites which themselves depend on
 * other satellites PV.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.2
 */
public interface MultiAttitudeProvider extends Serializable {

    /**
     * Computes the attitude corresponding to several orbital states.
     * 
     * @param pvProvs PV providers around current date
     * @param date current date
     * @param frame reference frame in which attitude is computed
     * @return attitude attitude at the specified date in the specified frame and depending on
     *         the specified PV states
     * @throws PatriusException thrown if attitude cannot be computed
     */
    Attitude getAttitude(final Map<String, PVCoordinatesProvider> pvProvs, final AbsoluteDate date,
                         final Frame frame) throws PatriusException;

    /**
     * Computes the attitude corresponding to several orbital states.
     * 
     * @param orbits orbits of satellite. They should be at the same date
     * @return attitude attitude at the orbit date, in orbit frame and depending on the specified
     *         PV states
     * @throws PatriusException thrown if attitude cannot be computed or orbits are not at the same
     *         date
     */
    Attitude getAttitude(final Map<String, Orbit> orbits) throws PatriusException;
}
