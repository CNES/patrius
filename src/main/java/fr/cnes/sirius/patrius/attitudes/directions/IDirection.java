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
 * @history creation 30/11/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3190:01/06/2022:[PATRIUS] Preciser la javadoc des methodes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This is the main interface for directions.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: IDirection.java 18065 2017-10-02 16:42:02Z bignon $
 * 
 * @since 1.1
 * 
 */
public interface IDirection extends Serializable {

    /**
     * Provides the direction vector at a given date in a given frame.
     * 
     * @param pvCoord
     *        the current coordinates of the origin point of the direction (may be null, in that specific case, the
     *        origin of the direction is the frame origin).
     * @param date
     *        the date
     * @param frame
     *        the frame to project the vector's coordinates
     * @return the direction vector (from origin to target) at the given date in the given frame
     * @exception PatriusException
     *            if some frame specific errors occur
     */
    Vector3D getVector(PVCoordinatesProvider pvCoord, AbsoluteDate date, Frame frame) throws PatriusException;

    /**
     * Provides the line containing the origin (given PV coordinates) and directed by the direction vector.
     * 
     * @param pvCoord
     *        the origin of the direction
     * @param date
     *        the current date
     * @param frame
     *        the expression frame of the line
     * @return the half Line of space from the origin position at given date and containing the direction vector
     * @throws PatriusException
     *         if some frame specific errors occur
     */
    Line getLine(PVCoordinatesProvider pvCoord, AbsoluteDate date, Frame frame) throws PatriusException;
}
