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
 * @history creation 18/09/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: refactoring of the interface
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import java.io.IOException;
import java.text.ParseException;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for all the signal propagation corrections due to the ionosphere : computation
 * of the electronic content.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public interface IonosphericCorrection {

    /**
     * Calculates the ionospheric signal delay for the signal path from the position
     * of the transmitter and the receiver and the current date.
     * 
     * @param date
     *        the current date
     * @param satellite
     *        the satellite position
     * @param satFrame
     *        the satellite position frame
     * @return the ionospheric signal delay
     * @throws PatriusException thrown if computation failed
     * @throws ParseException thrown if computation failed
     * @throws IOException thrown if computation failed
     */
    double computeSignalDelay(final AbsoluteDate date, final Vector3D satellite,
                              final Frame satFrame) throws PatriusException, IOException, ParseException;

}
